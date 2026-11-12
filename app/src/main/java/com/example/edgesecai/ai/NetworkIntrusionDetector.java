package com.example.edgesecai.ai;

import com.example.edgesecai.model.NetworkFlow;
import com.example.edgesecai.model.NetworkPacket;
import com.example.edgesecai.model.ThreatAlert;
import com.example.edgesecai.model.ThreatSeverity;
import com.example.edgesecai.model.ThreatType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class NetworkIntrusionDetector {

    public static ThreatAlert evaluate(NetworkFlow flow, FeatureVector features,
                                       List<NetworkPacket> recentPackets,
                                       BaselineProfiler baselineProfiler,
                                       double isolationForestScore) {
        long now = System.currentTimeMillis();

        // 1. Detection: Port Scanning (Vertical / Horizontal / SYN Sweep)
        // Indicators: High port entropy, contacting multiple ports (> 4) in short burst, or SYN/ACK ratio >> 3.0
        Set<Integer> uniquePorts = new HashSet<>();
        if (recentPackets != null) {
            for (NetworkPacket p : recentPackets) {
                if (p.getSrcIp().equals(flow.getSrcIp())) {
                    uniquePorts.add(p.getDstPort());
                }
            }
        }

        boolean isMultiPortScan = uniquePorts.size() >= 5;
        boolean isSynStealthScan = flow.getSynCount() >= 5 && flow.getAckCount() <= 1;

        if (isMultiPortScan || isSynStealthScan) {
            double score = Math.max(0.91, isolationForestScore);
            String scanMode = isMultiPortScan ? "Vertical Port Sweep (" + uniquePorts.size() + " distinct ports)" : "Half-Open SYN Stealth Scan";

            return new ThreatAlert(
                    UUID.randomUUID().toString(),
                    now,
                    ThreatType.PORT_SCAN,
                    ThreatSeverity.HIGH,
                    flow.getSrcIp() + ":" + flow.getSrcPort(),
                    flow.getDstIp() + " (Ports: " + uniquePorts + ")",
                    flow.getAppName(),
                    score,
                    ThreatType.PORT_SCAN.getMitreTechnique(),
                    "Network Reconnaissance / Port Scan (" + scanMode + ")",
                    "A sequence of rapid probes was identified targeting " + uniquePorts.size() +
                            " ports from " + flow.getSrcIp() + " with SYN/ACK ratio=" + String.format("%.1f", features.synAckRatio) +
                            ". This indicates automated network discovery (Nmap/masscan) scanning for vulnerable services and open sockets.",
                    "1. Enable firewall rate limiting on incoming SYN packets.\n2. Blacklist scanning source IP: " + flow.getSrcIp() + ".\n3. Close non-essential local listener sockets.",
                    true,
                    "Targeted Ports: " + uniquePorts + ", SYN count: " + flow.getSynCount() + ", ACK count: " + flow.getAckCount()
            );
        }

        // 2. Detection: SYN Flood / DoS Packet Storm
        // Indicators: Extremely high packet rate (> 40 pkt/sec) with high SYN-to-ACK ratio or small payload
        if (features.packetRate > 35.0 && features.synAckRatio > 4.0 && flow.getSynCount() >= 10) {
            return new ThreatAlert(
                    UUID.randomUUID().toString(),
                    now,
                    ThreatType.SYN_FLOOD_DOS,
                    ThreatSeverity.CRITICAL,
                    flow.getSrcIp() + ":" + flow.getSrcPort(),
                    flow.getDstIp() + ":" + flow.getDstPort(),
                    flow.getAppName(),
                    0.96,
                    ThreatType.SYN_FLOOD_DOS.getMitreTechnique(),
                    "SYN Flood Denial of Service (DoS) Detected",
                    "Abnormal surge of SYN packets (" + String.format("%.1f", features.packetRate) + " pkt/sec, " +
                            flow.getSynCount() + " SYNs without ACK completions) aimed at exhausting socket state memory tables (SYN backlog queue).",
                    "1. Activate SYN Cookies and TCP backlog mitigation.\n2. Temporarily isolate network interface.\n3. Drop all unestablished embryonic connection requests from " + flow.getSrcIp() + ".",
                    true,
                    "Packet Rate: " + String.format("%.1f", features.packetRate) + " pkt/s, SYNs: " + flow.getSynCount() + ", ACKs: " + flow.getAckCount()
            );
        }

        // 3. Detection: Statistical Traffic Anomaly (High Z-Score + High Isolation Forest Anomaly Score)
        double packetZScore = baselineProfiler != null ? baselineProfiler.computePacketRateZScore(features.packetRate) : 0.0;
        double byteZScore = baselineProfiler != null ? baselineProfiler.computeByteRateZScore(features.byteRate) : 0.0;

        if ((isolationForestScore >= 0.78 && (packetZScore > 3.0 || byteZScore > 3.0)) || isolationForestScore >= 0.88) {
            return new ThreatAlert(
                    UUID.randomUUID().toString(),
                    now,
                    ThreatType.ANOMALOUS_TRAFFIC_VOLUME,
                    isolationForestScore >= 0.85 ? ThreatSeverity.HIGH : ThreatSeverity.MEDIUM,
                    flow.getSrcIp() + ":" + flow.getSrcPort(),
                    flow.getDstIp() + ":" + flow.getDstPort(),
                    flow.getAppName(),
                    isolationForestScore,
                    ThreatType.ANOMALOUS_TRAFFIC_VOLUME.getMitreTechnique(),
                    "AI Anomaly Engine: Unsupervised Outlier Deviation",
                    "The on-device Isolation Forest scored this flow at " + String.format("%.2f", isolationForestScore) +
                            " (critical threshold 0.75). The packet rate Z-score is " + String.format("%.2f", packetZScore) +
                            "σ and byte rate Z-score is " + String.format("%.2f", byteZScore) +
                            "σ above normal device baseline, signifying uncharacteristic traffic.",
                    "1. Review recent network transfers from '" + flow.getAppName() + "'.\n2. Inspect active background tasks.\n3. Apply bandwidth capping if untrusted.",
                    false,
                    "Isolation Score: " + String.format("%.3f", isolationForestScore) + ", Packet Z: " + String.format("%.2f", packetZScore) + "σ, Byte Z: " + String.format("%.2f", byteZScore) + "σ"
            );
        }

        return null;
    }
}
