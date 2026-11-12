package com.example.edgesecai.engine;

import android.net.VpnService;

import com.example.edgesecai.model.NetworkPacket;
import com.example.edgesecai.storage.SecurityRepository;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * In-process SOCKS5 endpoint for the local tun2socks bridge. Outbound sockets are
 * protected from the VPN, so the bridge connects directly to the requested host.
 */
public final class DirectSocks5Server implements AutoCloseable {
    public static final int PORT = 10808;
    private static final int MAX_UDP_PAYLOAD = 65507;

    private final VpnService vpnService;
    private final SecurityRepository repository;
    private final ExecutorService clients = Executors.newCachedThreadPool();
    private volatile boolean running;
    private ServerSocket serverSocket;
    private Thread acceptThread;

    public DirectSocks5Server(VpnService vpnService, SecurityRepository repository) {
        this.vpnService = vpnService;
        this.repository = repository;
    }

    public synchronized void start() throws IOException {
        if (running) return;
        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT));
        running = true;
        acceptThread = new Thread(this::acceptLoop, "EdgeSec-Socks-Accept");
        acceptThread.start();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket client = serverSocket.accept();
                android.util.Log.d("EdgeSec-Socks", "Accepted local SOCKS client");
                clients.execute(() -> handleClient(client));
            } catch (IOException e) {
                if (running) android.util.Log.e("EdgeSec-Socks", "Accept failed", e);
            }
        }
    }

    private void handleClient(Socket client) {
        boolean handedOff = false;
        DataInputStream input = null;
        DataOutputStream output = null;
        try {
            client.setSoTimeout(15000);
            input = new DataInputStream(new BufferedInputStream(client.getInputStream()));
            output = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
            int version = input.readUnsignedByte();
            int methods = input.readUnsignedByte();
            if (version != 5 || methods < 1) return;
            boolean noAuth = false;
            for (int i = 0; i < methods; i++) {
                if (input.readUnsignedByte() == 0) noAuth = true;
            }
            output.write(new byte[]{5, (byte) (noAuth ? 0 : 0xff)});
            output.flush();
            if (!noAuth) return;

            int requestVersion = input.readUnsignedByte();
            int command = input.readUnsignedByte();
            input.readUnsignedByte(); // reserved
            int addressType = input.readUnsignedByte();
            if (requestVersion != 5) return;
            InetAddress address = readAddress(input, addressType);
            int port = input.readUnsignedShort();

            if (command == 1) {
                handedOff = handleConnect(client, output, address, port);
            } else if (command == 3) {
                handleUdpAssociate(client, input, output);
            } else {
                writeReply(output, 7);
            }
        } catch (Exception e) {
            android.util.Log.w("EdgeSec-Socks", "SOCKS handshake failed", e);
        } finally {
            if (!handedOff) closeQuietly(client);
        }
    }

    private InetAddress readAddress(DataInputStream input, int type) throws IOException {
        if (type == 1) {
            byte[] bytes = new byte[4];
            input.readFully(bytes);
            return InetAddress.getByAddress(bytes);
        }
        if (type == 4) {
            byte[] bytes = new byte[16];
            input.readFully(bytes);
            return InetAddress.getByAddress(bytes);
        }
        if (type == 3) {
            int length = input.readUnsignedByte();
            byte[] name = new byte[length];
            input.readFully(name);
            return InetAddress.getByName(new String(name, StandardCharsets.UTF_8));
        }
        throw new IOException("Unsupported SOCKS address type");
    }

    private boolean handleConnect(Socket local, DataOutputStream output,
                                  InetAddress address, int port) throws IOException {
        if (repository.isTrafficBlocked(address.getHostAddress(), port)) {
            writeReply(output, 2);
            return false;
        }

        Socket remote = new Socket();
        if (!vpnService.protect(remote)) {
            remote.close();
            writeReply(output, 1);
            return false;
        }
        try {
            remote.connect(new InetSocketAddress(address, port), 10000);
            android.util.Log.d("EdgeSec-Socks", "TCP destination connected on port " + port);
            remote.setSoTimeout(0);
            local.setSoTimeout(0);
            writeReply(output, 0);
            // This proxy observes socket data, not raw TCP SYN packets.
            report(address, port, "TCP", 0, "CONNECT", true, "", "");
            clients.execute(() -> pipe(local, remote, true, address, port));
            clients.execute(() -> pipe(remote, local, false, address, port));
            return true;
        } catch (IOException e) {
            android.util.Log.w("EdgeSec-Socks", "TCP destination connect failed on port " + port, e);
            remote.close();
            writeReply(output, 5);
            return false;
        }
    }

    private void pipe(Socket source, Socket target, boolean outbound, InetAddress address, int port) {
        byte[] buffer = new byte[8192];
        try {
            int count;
            while (running && (count = source.getInputStream().read(buffer)) >= 0) {
                if (repository.isTrafficBlocked(address.getHostAddress(), port)) break;
                target.getOutputStream().write(buffer, 0, count);
                target.getOutputStream().flush();
                report(address, port, "TCP", count, outbound ? "ACK PSH" : "ACK",
                        outbound, "", "");
            }
        } catch (IOException ignored) {
            // Closing either half tears down this proxied connection.
        } finally {
            closeQuietly(source);
            closeQuietly(target);
        }
    }

    private void handleUdpAssociate(Socket control, DataInputStream input, DataOutputStream output)
            throws IOException {
        // The loopback socket receives SOCKS-framed datagrams from the native TUN engine.
        DatagramSocket localRelay = new DatagramSocket(new InetSocketAddress(
                InetAddress.getByName("127.0.0.1"), 0));
        // Internet datagrams must use a wildcard-bound socket; a loopback-bound socket
        // cannot send packets with a routable source address.
        DatagramSocket upstream = new DatagramSocket();
        if (!vpnService.protect(upstream)) {
            localRelay.close();
            upstream.close();
            writeReply(output, 1);
            return;
        }

        writeReply(output, 0, InetAddress.getByName("127.0.0.1"), localRelay.getLocalPort());
        control.setSoTimeout(0);
        AtomicReference<InetSocketAddress> clientAddress = new AtomicReference<>();
        clients.execute(() -> {
            byte[] buffer = new byte[MAX_UDP_PAYLOAD];
            while (running && !upstream.isClosed()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    upstream.receive(packet);
                    InetSocketAddress expected = clientAddress.get();
                    if (expected != null) {
                        sendUdpReply(localRelay, expected, packet);
                    }
                } catch (IOException e) {
                    if (running) android.util.Log.w("EdgeSec-Socks", "UDP receive stopped", e);
                    break;
                }
            }
            localRelay.close();
            upstream.close();
        });

        clients.execute(() -> {
            byte[] buffer = new byte[MAX_UDP_PAYLOAD];
            while (running && !localRelay.isClosed()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    localRelay.receive(packet);
                    clientAddress.set((InetSocketAddress) packet.getSocketAddress());
                    forwardUdpRequest(upstream, packet);
                } catch (IOException e) {
                    if (running) android.util.Log.w("EdgeSec-Socks", "Local UDP relay stopped", e);
                    break;
                }
            }
            localRelay.close();
            upstream.close();
        });

        try {
            byte[] discard = new byte[128];
            while (running && control.getInputStream().read(discard) != -1) {
                // RFC 1928 keeps the control connection open for the UDP association lifetime.
            }
        } finally {
            localRelay.close();
            upstream.close();
        }
    }

    private void forwardUdpRequest(DatagramSocket upstream, DatagramPacket packet) {
        try {
            DataInputStream framed = new DataInputStream(new java.io.ByteArrayInputStream(
                    packet.getData(), packet.getOffset(), packet.getLength()));
            if (framed.readUnsignedShort() != 0 || framed.readUnsignedByte() != 0) return;
            int type = framed.readUnsignedByte();
            InetAddress destination = readAddress(framed, type);
            int port = framed.readUnsignedShort();
            int headerLength = packet.getLength() - framed.available();
            int payloadLength = packet.getLength() - headerLength;
            if (payloadLength < 0 || repository.isTrafficBlocked(destination.getHostAddress(), port)) return;

            byte[] data = new byte[payloadLength];
            framed.readFully(data);
            DatagramPacket outbound = new DatagramPacket(data, data.length, destination, port);
            upstream.send(outbound);
            String dnsQuery = port == 53 ? parseDnsQuestion(data) : "";
            report(destination, port, "UDP", data.length, "", true, dnsQuery, "");
        } catch (Exception e) {
            android.util.Log.w("EdgeSec-Socks", "Dropping malformed or blocked UDP frame", e);
        }
    }

    private void sendUdpReply(DatagramSocket relay, InetSocketAddress client, DatagramPacket response)
            throws IOException {
        byte[] sourceAddress = response.getAddress().getAddress();
        int type = sourceAddress.length == 16 ? 4 : 1;
        int headerLength = 4 + sourceAddress.length + 2;
        byte[] framed = new byte[headerLength + response.getLength()];
        framed[0] = 0;
        framed[1] = 0;
        framed[2] = 0;
        framed[3] = (byte) type;
        System.arraycopy(sourceAddress, 0, framed, 4, sourceAddress.length);
        int portOffset = 4 + sourceAddress.length;
        framed[portOffset] = (byte) (response.getPort() >>> 8);
        framed[portOffset + 1] = (byte) response.getPort();
        System.arraycopy(response.getData(), response.getOffset(), framed, headerLength, response.getLength());
        report(response.getAddress(), response.getPort(), "UDP", response.getLength(), "", false, "", "");
        relay.send(new DatagramPacket(framed, framed.length, client));
    }

    private void report(InetAddress destination, int port, String protocol, int length, String flags,
                        boolean outbound, String dnsQuery, String payloadPreview) {
        long now = System.currentTimeMillis();
        // Keep one stable client-to-server flow key; isOutbound carries the byte direction.
        NetworkPacket packet = new NetworkPacket(now, "10.1.1.2", 0,
                destination.getHostAddress(), port, protocol, length, flags,
                dnsQuery, payloadPreview, 0, "Device traffic", outbound);
        repository.processPacket(packet);
        android.util.Log.d("EdgeSec-Socks", "Observed " + (outbound ? "outbound " : "inbound ")
                + protocol + " flow on destination port " + port + " (" + length + " payload bytes)");
    }

    private static String parseDnsQuestion(byte[] message) {
        if (message == null || message.length < 17) return "";
        int questionCount = ((message[4] & 0xff) << 8) | (message[5] & 0xff);
        if (questionCount < 1) return "";
        int offset = 12;
        StringBuilder domain = new StringBuilder();
        while (offset < message.length) {
            int labelLength = message[offset++] & 0xff;
            if (labelLength == 0) return domain.toString();
            if ((labelLength & 0xc0) != 0 || labelLength > 63 || offset + labelLength > message.length) {
                return "";
            }
            if (domain.length() > 0) domain.append('.');
            for (int i = 0; i < labelLength; i++) {
                int value = message[offset++] & 0xff;
                if (value < 33 || value > 126) return "";
                domain.append((char) value);
            }
        }
        return "";
    }

    private static void writeReply(DataOutputStream output, int code) throws IOException {
        writeReply(output, code, InetAddress.getByName("0.0.0.0"), 0);
    }

    private static void writeReply(DataOutputStream output, int code, InetAddress address, int port)
            throws IOException {
        byte[] ip = address.getAddress();
        output.writeByte(5);
        output.writeByte(code);
        output.writeByte(0);
        output.writeByte(ip.length == 16 ? 4 : 1);
        output.write(ip);
        output.writeShort(port);
        output.flush();
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public synchronized void close() {
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
            serverSocket = null;
        }
        clients.shutdownNow();
    }
}
