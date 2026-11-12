package com.example.edgesecai.ai;

import com.example.edgesecai.model.SecurityStats;
import com.example.edgesecai.model.ThreatAlert;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AIAssistantExplainer {

    private static final Map<String, String> KNOWLEDGE_BASE = new HashMap<>();

    static {
        KNOWLEDGE_BASE.put("c2", "Command & Control (C2) refers to centralized infrastructure operated by cyber adversaries to communicate with compromised devices. Beaconing is the automated periodic or jittered check-in sent by malware to receive commands or exfiltrate state.");
        KNOWLEDGE_BASE.put("beaconing", "Beaconing traffic exhibits mathematical periodicity with very low coefficient of variation (CV < 0.35) in inter-arrival times. On-device AI tracks packet delta variance to detect C2 heartbeats even when payloads are encrypted.");
        KNOWLEDGE_BASE.put("dns tunneling", "DNS Tunneling encodes non-DNS data into DNS query labels (e.g. hex/base64 subdomains). Because port 53 is rarely blocked by firewalls, malware exploits it for covert data exfiltration. We detect it via Shannon entropy analysis (entropy > 4.2 bits/char) and abnormal subdomain length.");
        KNOWLEDGE_BASE.put("port scan", "A port scan probes a sequence of destination ports on a target host to identify active listening services. Reconnaissance tools like Nmap use SYN/half-open scans. Our AI detects port scanning by calculating destination port entropy and rapid SYN/ACK imbalances.");
        KNOWLEDGE_BASE.put("syn flood", "A SYN flood is a Denial of Service (DoS) attack where an attacker sends a high volume of TCP SYN requests without completing the 3-way handshake with ACK. This fills the host's SYN backlog queue and exhausts connection resources.");
        KNOWLEDGE_BASE.put("cryptomining", "Cryptomining malware hijacks device compute resources to mine cryptocurrencies (such as Monero). It connects to mining pools using the Stratum protocol (often on ports 3333, 4444, 14444) with JSON-RPC RPC calls like 'mining.subscribe'.");
        KNOWLEDGE_BASE.put("isolation forest", "Isolation Forest is an unsupervised AI algorithm that isolates anomalies instead of profiling normal points. It constructs random decision trees; because anomalies require far fewer partitions to isolate, their average tree path length is noticeably shorter, yielding a high anomaly score.");
        KNOWLEDGE_BASE.put("entropy", "Shannon entropy quantifies the unpredictability or randomness of information. Plain-text domains have predictable vowel-consonant patterns and lower entropy (2.5-3.5), whereas encrypted or base64-encoded exfiltrated data has high entropy (> 4.2).");
        KNOWLEDGE_BASE.put("mitre", "The MITRE ATT&CK framework is a globally accessible knowledge base of adversary tactics and techniques based on real-world observations. EdgeSecAI maps every detected anomaly directly to corresponding MITRE ATT&CK technique IDs (e.g., T1071, T1046, T1048).");
    }

    public static String generateThreatExplanation(ThreatAlert alert) {
        StringBuilder sb = new StringBuilder();
        sb.append("### On-Device AI Threat Intelligence\n\n");
        sb.append("**Incident Type:** ").append(alert.getThreatType().getDisplayName()).append("\n");
        sb.append("**Severity Level:** ").append(alert.getSeverity().getLabel()).append("\n");
        sb.append("**MITRE ATT&CK:** ").append(alert.getMitreTechnique()).append("\n");
        sb.append("**Anomaly Confidence Score:** ").append(String.format(Locale.US, "%.1f%%", alert.getAnomalyScore() * 100)).append("\n\n");

        sb.append("#### Behavioral Analysis\n");
        sb.append(alert.getAiDiagnosis()).append("\n\n");

        sb.append("#### Immediate Remediation Actions\n");
        sb.append(alert.getRecommendedActions()).append("\n\n");

        if (alert.getPacketSample() != null && !alert.getPacketSample().isEmpty()) {
            sb.append("#### Forensic Telemetry Snapshot\n");
            sb.append("```\n").append(alert.getPacketSample()).append("\n```\n");
        }

        return sb.toString();
    }

    public static String generateSecurityPostureReport(SecurityStats stats, List<ThreatAlert> alerts) {
        StringBuilder sb = new StringBuilder();
        sb.append("### Security Posture Report (Offline Assessment)\n\n");

        if (!stats.isProtectionActive()) {
            sb.append("⚠️ **Shield is currently INACTIVE.** Turn on Real-Time Inspection to guard incoming and outgoing traffic.\n\n");
        } else {
            sb.append("🛡️ **On-Device Shield: ACTIVE & SECURED.** Continuous telemetry monitoring is running.\n\n");
        }

        sb.append("- **Packets Processed:** ").append(stats.getTotalPackets()).append("\n");
        sb.append("- **Volume Inspected:** ").append(formatBytes(stats.getTotalBytes())).append("\n");
        sb.append("- **Active Network Flows:** ").append(stats.getActiveFlows()).append("\n");
        sb.append("- **Threats Detected:** ").append(stats.getThreatsDetected()).append("\n");
        sb.append("- **Attacks Mitigated / Blocked:** ").append(stats.getThreatsBlocked()).append("\n");
        sb.append("- **Current Ambient Anomaly Score:** ").append(String.format(Locale.US, "%.1f%%", stats.getCurrentAnomalyLevel() * 100)).append("\n\n");

        if (alerts == null || alerts.isEmpty()) {
            sb.append("✅ **Clean Baseline:** No active intrusion attempts or malware communication detected in the current session.");
        } else {
            sb.append("⚠️ **Security Incidents Summary:**\n");
            for (ThreatAlert a : alerts) {
                sb.append("• [").append(a.getSeverity().getLabel()).append("] ")
                        .append(a.getThreatType().getDisplayName())
                        .append(" from ").append(a.getSource())
                        .append(" (").append(a.getMitreTechnique()).append(")\n");
            }
        }

        return sb.toString();
    }

    public static String answerUserPrompt(String query, SecurityStats stats, List<ThreatAlert> recentAlerts) {
        if (query == null || query.trim().isEmpty()) {
            return "Hello! I am your offline cybersecurity assistant. Ask me about detected anomalies, port scanning, C2 beaconing, DNS tunneling, or request a system security assessment.";
        }

        String lower = query.toLowerCase(Locale.US).trim();

        if (lower.contains("status") || lower.contains("posture") || lower.contains("overview") || lower.contains("report")) {
            return generateSecurityPostureReport(stats, recentAlerts);
        }

        if (lower.contains("help") || lower.contains("how do i") || lower.contains("commands")) {
            return "### EdgeSecAI Assistant Guidance\n\n" +
                    "I operate completely offline on this device to protect your network privacy. You can ask:\n" +
                    "• **'Status report'** - Current network telemetry and detected incidents.\n" +
                    "• **'What is C2 beaconing?'** - Explains Command & Control detection.\n" +
                    "• **'Explain DNS tunneling'** - How malware smuggles data over port 53.\n" +
                    "• **'What is a port scan?'** - Reconnaissance and scanning patterns.\n" +
                    "• **'Explain Isolation Forest'** - How our on-device AI unsupervised model scores anomalies.\n" +
                    "• **'How does entropy work?'** - Shannon entropy analysis for encrypted payloads.";
        }

        for (Map.Entry<String, String> entry : KNOWLEDGE_BASE.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return "### " + capitalize(entry.getKey()) + " Analysis\n\n" +
                        entry.getValue() + "\n\n" +
                        "> **EdgeSecAI On-Device Engine:** Evaluates these indicators in real-time using local mathematical feature extraction and Isolation Forest trees without external cloud transmission.";
            }
        }

        return "### EdgeSecAI Response\n\n" +
                "I analyzed your inquiry: *\"" + query + "\"*.\n\n" +
                "All network inspection, socket attribution, and AI anomaly inference run strictly on your device hardware with 0 external network dependencies. " +
                "You can inspect live network flows in the **Flows** tab, simulate attacks in the **Simulator** tab to test detection rules, or review recent alerts in the **Alerts** tab.";
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0));
    }

    private static String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
