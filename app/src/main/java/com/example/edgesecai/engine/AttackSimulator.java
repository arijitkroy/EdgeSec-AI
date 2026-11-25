package com.example.edgesecai.engine;

import com.example.edgesecai.model.NetworkPacket;
import com.example.edgesecai.storage.SecurityRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class AttackSimulator {
    private final SecurityRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final AtomicInteger runCounter = new AtomicInteger(1);

    public AttackSimulator(SecurityRepository repository) {
        this.repository = repository;
    }

    public void simulatePortScan(SimulatorCallback callback) {
        executor.execute(() -> {
            int run = runCounter.getAndIncrement();
            String attackerIp = "192.168.1." + (100 + (run % 50));
            String victimIp = "192.168.1.200";
            repository.unblockEndpoint(attackerIp, 0);
            repository.clearReportedAlertKey("PORT_SCAN:");

            int[] targetPorts = {21, 22, 23, 80, 443, 8080, 4444, 3389, 5900, 8443};
            for (int port : targetPorts) {
                NetworkPacket packet = new NetworkPacket(
                        System.currentTimeMillis(),
                        attackerIp, 49152 + port,
                        victimIp, port,
                        "TCP", 44, "SYN",
                        "", "", 10200, "PortScannerTool", true
                );
                repository.processPacket(packet);
                try {
                    Thread.sleep(60);
                } catch (InterruptedException ignored) {}
            }
            if (callback != null) callback.onSimulationComplete("Port scan attack simulated (10 ports probed)");
        });
    }

    public void simulateC2Beaconing(SimulatorCallback callback) {
        executor.execute(() -> {
            int run = runCounter.getAndIncrement();
            int localPort = 50000 + (run % 10000);
            String victimIp = "192.168.1.200";
            String c2Ip = "198.51.100." + (40 + (run % 40));
            int c2Port = 8443;
            repository.unblockEndpoint(c2Ip, 0);
            repository.unblockEndpoint(c2Ip, c2Port);
            repository.clearReportedAlertKey("MALWARE_C2_BEACONING:");

            for (int i = 0; i < 6; i++) {
                NetworkPacket packet = new NetworkPacket(
                        System.currentTimeMillis(),
                        victimIp, localPort,
                        c2Ip, c2Port,
                        "TCP", 64, "ACK PSH",
                        "", "PING_HEARTBEAT_STATUS=OK", 10255, "TrojanSpywareService", true
                );
                repository.processPacket(packet);
                try {
                    Thread.sleep(850 + (long) (Math.random() * 20));
                } catch (InterruptedException ignored) {}
            }
            if (callback != null) callback.onSimulationComplete("Malware C2 beaconing simulated (6 regular heartbeats)");
        });
    }

    public void simulateDnsTunneling(SimulatorCallback callback) {
        executor.execute(() -> {
            int run = runCounter.getAndIncrement();
            int localPort = 53000 + (run % 10000);
            String victimIp = "192.168.1.200";
            String dnsServerIp = "8.8.8.8";
            repository.unblockEndpoint(dnsServerIp, 0);
            repository.unblockEndpoint(null, 53);
            repository.clearReportedAlertKey("DNS_TUNNELING:");

            String prefix = Long.toHexString(Double.doubleToLongBits(Math.random()));
            String encodedQuery = prefix + "a8f3c7b9e1d2c4b5a6f7e8d9.exfil.darkops-c2.net";

            NetworkPacket packet = new NetworkPacket(
                    System.currentTimeMillis(),
                    victimIp, localPort,
                    dnsServerIp, 53,
                    "UDP", 128, "",
                    encodedQuery, "EXFILTRATED_PAYLOAD_BASE64", 10188, "CovertDataExfiltrator", true
            );
            repository.processPacket(packet);

            if (callback != null) callback.onSimulationComplete("DNS Tunneling query simulated with high Shannon entropy");
        });
    }

    public void simulateSynFlood(SimulatorCallback callback) {
        executor.execute(() -> {
            int run = runCounter.getAndIncrement();
            String attackerIp = "203.0.113." + (10 + (run % 50));
            String victimIp = "192.168.1.200";
            int targetPort = 80;
            int srcPort = 45000 + (run % 10000);
            repository.unblockEndpoint(attackerIp, 0);
            repository.unblockEndpoint(victimIp, targetPort);
            repository.clearReportedAlertKey("SYN_FLOOD_DOS:");

            for (int i = 0; i < 35; i++) {
                NetworkPacket packet = new NetworkPacket(
                        System.currentTimeMillis(),
                        attackerIp, srcPort,
                        victimIp, targetPort,
                        "TCP", 40, "SYN",
                        "", "", 0, "ExternalAttacker", false
                );
                repository.processPacket(packet);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {}
            }
            if (callback != null) callback.onSimulationComplete("SYN flood DoS packet storm simulated (35 SYNs)");
        });
    }

    public void simulateCryptominer(SimulatorCallback callback) {
        executor.execute(() -> {
            int run = runCounter.getAndIncrement();
            int localPort = 48000 + (run % 10000);
            String victimIp = "192.168.1.200";
            String minerPoolIp = "198.51.100." + (80 + (run % 20));
            int poolPort = 3333;
            repository.unblockEndpoint(minerPoolIp, 0);
            repository.unblockEndpoint(minerPoolIp, poolPort);
            repository.clearReportedAlertKey("SUSPICIOUS_PORT_USAGE:");

            NetworkPacket packet = new NetworkPacket(
                    System.currentTimeMillis(),
                    victimIp, localPort,
                    minerPoolIp, poolPort,
                    "TCP", 180, "ACK PSH",
                    "", "{\"id\":1,\"method\":\"mining.subscribe\",\"params\":[\"cpuminer/2.5.1\"]}",
                    10199, "StealthCryptoMiner", true
            );
            repository.processPacket(packet);

            if (callback != null) callback.onSimulationComplete("Cryptomining Stratum RPC connection simulated on port 3333");
        });
    }

    public void simulateNormalTraffic(SimulatorCallback callback) {
        executor.execute(() -> {
            int run = runCounter.getAndIncrement();
            int localPort = 55000 + (run % 10000);
            String localIp = "192.168.1.200";
            String webServerIp = "142.250.190.46";
            int webPort = 443;

            // 1. Normal DNS query
            NetworkPacket dnsPacket = new NetworkPacket(
                    System.currentTimeMillis(),
                    localIp, 51234 + (run % 1000),
                    "8.8.8.8", 53,
                    "UDP", 75, "",
                    "wikipedia.org", "", 10100, "Chrome", true
            );
            repository.processPacket(dnsPacket);

            // 2. Normal HTTPS handshake & payload
            for (int i = 0; i < 4; i++) {
                NetworkPacket packet = new NetworkPacket(
                        System.currentTimeMillis(),
                        localIp, localPort,
                        webServerIp, webPort,
                        "TCP", 512, "ACK",
                        "", "TLSv1.3 Encrypted Application Data", 10100, "Chrome", true
                );
                repository.processPacket(packet);
                try {
                    Thread.sleep(120);
                } catch (InterruptedException ignored) {}
            }

            if (callback != null) callback.onSimulationComplete("Benign browsing traffic simulated (Clean Baseline)");
        });
    }

    public interface SimulatorCallback {
        void onSimulationComplete(String message);
    }
}

