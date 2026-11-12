package com.example.edgesecai.engine;

import com.example.edgesecai.model.NetworkPacket;

import java.nio.ByteBuffer;
import java.util.Locale;

public class PacketParser {

    public static NetworkPacket parse(byte[] buffer, int length, boolean isOutbound, int uid, String appName) {
        if (buffer == null || length < 20) return null;

        int version = (buffer[0] >> 4) & 0x0F;
        if (version != 4) {
            // IPv6 or non-IPv4 - simplify for on-device prototype
            return null;
        }

        int ihl = (buffer[0] & 0x0F) * 4;
        if (ihl < 20 || length < ihl) return null;

        int protocolNum = buffer[9] & 0xFF;
        String protocol;
        switch (protocolNum) {
            case 6: protocol = "TCP"; break;
            case 17: protocol = "UDP"; break;
            case 1: protocol = "ICMP"; break;
            default: protocol = "IP-" + protocolNum; break;
        }

        String srcIp = formatIp(buffer, 12);
        String dstIp = formatIp(buffer, 16);

        int srcPort = 0;
        int dstPort = 0;
        String tcpFlags = "";
        String dnsQuery = "";
        String payloadPreview = "";

        if (protocolNum == 6 && length >= ihl + 20) { // TCP
            srcPort = ((buffer[ihl] & 0xFF) << 8) | (buffer[ihl + 1] & 0xFF);
            dstPort = ((buffer[ihl + 2] & 0xFF) << 8) | (buffer[ihl + 3] & 0xFF);

            int dataOffset = ((buffer[ihl + 12] >> 4) & 0x0F) * 4;
            int flagsByte = buffer[ihl + 13] & 0xFF;
            tcpFlags = parseTcpFlags(flagsByte);

            int payloadOffset = ihl + dataOffset;
            int payloadLength = length - payloadOffset;
            if (payloadLength > 0 && payloadOffset < length) {
                payloadPreview = extractPayloadPreview(buffer, payloadOffset, payloadLength);
            }
        } else if (protocolNum == 17 && length >= ihl + 8) { // UDP
            srcPort = ((buffer[ihl] & 0xFF) << 8) | (buffer[ihl + 1] & 0xFF);
            dstPort = ((buffer[ihl + 2] & 0xFF) << 8) | (buffer[ihl + 3] & 0xFF);

            int payloadOffset = ihl + 8;
            int payloadLength = length - payloadOffset;
            if (dstPort == 53 || srcPort == 53) {
                dnsQuery = parseDnsQuery(buffer, payloadOffset, payloadLength);
            }
            if (payloadLength > 0 && payloadOffset < length) {
                payloadPreview = extractPayloadPreview(buffer, payloadOffset, payloadLength);
            }
        }

        return new NetworkPacket(
                System.currentTimeMillis(),
                srcIp, srcPort, dstIp, dstPort, protocol, length,
                tcpFlags, dnsQuery, payloadPreview, uid, appName, isOutbound
        );
    }

    private static String formatIp(byte[] buffer, int offset) {
        return (buffer[offset] & 0xFF) + "." +
                (buffer[offset + 1] & 0xFF) + "." +
                (buffer[offset + 2] & 0xFF) + "." +
                (buffer[offset + 3] & 0xFF);
    }

    private static String parseTcpFlags(int flags) {
        StringBuilder sb = new StringBuilder();
        if ((flags & 0x02) != 0) sb.append("SYN ");
        if ((flags & 0x10) != 0) sb.append("ACK ");
        if ((flags & 0x01) != 0) sb.append("FIN ");
        if ((flags & 0x04) != 0) sb.append("RST ");
        if ((flags & 0x08) != 0) sb.append("PSH ");
        if ((flags & 0x20) != 0) sb.append("URG ");
        return sb.toString().trim();
    }

    private static String parseDnsQuery(byte[] buffer, int offset, int length) {
        if (length < 12 || offset + length > buffer.length) return "";
        try {
            // DNS header is 12 bytes; Question section follows
            int qOffset = offset + 12;
            StringBuilder domain = new StringBuilder();
            while (qOffset < offset + length) {
                int labelLen = buffer[qOffset] & 0xFF;
                if (labelLen == 0) break;
                if ((labelLen & 0xC0) != 0) break; // Pointer compression

                qOffset++;
                if (qOffset + labelLen > buffer.length) break;

                if (domain.length() > 0) domain.append(".");
                for (int i = 0; i < labelLen; i++) {
                    char c = (char) buffer[qOffset + i];
                    domain.append(c);
                }
                qOffset += labelLen;
            }
            return domain.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static String extractPayloadPreview(byte[] buffer, int offset, int length) {
        int sampleLen = Math.min(length, 64);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sampleLen; i++) {
            byte b = buffer[offset + i];
            if (b >= 32 && b <= 126) {
                sb.append((char) b);
            } else {
                sb.append(".");
            }
        }
        return sb.toString();
    }
}
