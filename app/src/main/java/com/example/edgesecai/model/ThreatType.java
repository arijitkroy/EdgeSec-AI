package com.example.edgesecai.model;

public enum ThreatType {
    PORT_SCAN("Network Reconnaissance / Port Scan", "T1046"),
    SYN_FLOOD_DOS("SYN Flood Denial of Service", "T1498"),
    MALWARE_C2_BEACONING("Command & Control (C2) Beaconing", "T1071"),
    DNS_TUNNELING("DNS Tunneling Exfiltration", "T1048.003"),
    DATA_EXFILTRATION("Anomalous Data Exfiltration", "T1041"),
    SUSPICIOUS_PORT_USAGE("Known Malware / Exploit Port Connection", "T1571"),
    ANOMALOUS_TRAFFIC_VOLUME("Statistical Anomaly: Traffic Spike", "T1499"),
    BACKGROUND_ANOMALY("Suspicious Background App Network Activity", "T1020");

    private final String displayName;
    private final String mitreTechnique;

    ThreatType(String displayName, String mitreTechnique) {
        this.displayName = displayName;
        this.mitreTechnique = mitreTechnique;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMitreTechnique() {
        return mitreTechnique;
    }
}
