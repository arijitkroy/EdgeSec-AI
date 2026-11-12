package com.example.edgesecai.model;

import java.util.UUID;

public class ThreatAlert {
    private final String id;
    private final long timestamp;
    private final ThreatType threatType;
    private final ThreatSeverity severity;
    private final String source;
    private final String destination;
    private final String appName;
    private final double anomalyScore;
    private final String mitreTechnique;
    private final String description;
    private final String aiDiagnosis;
    private final String recommendedActions;
    private boolean isBlocked;
    private final String packetSample;

    public ThreatAlert(String id, long timestamp, ThreatType threatType, ThreatSeverity severity,
                       String source, String destination, String appName, double anomalyScore,
                       String mitreTechnique, String description, String aiDiagnosis,
                       String recommendedActions, boolean isBlocked, String packetSample) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.timestamp = timestamp;
        this.threatType = threatType;
        this.severity = severity;
        this.source = source != null ? source : "N/A";
        this.destination = destination != null ? destination : "N/A";
        this.appName = appName != null ? appName : "System";
        this.anomalyScore = anomalyScore;
        this.mitreTechnique = mitreTechnique != null ? mitreTechnique : threatType.getMitreTechnique();
        this.description = description;
        this.aiDiagnosis = aiDiagnosis;
        this.recommendedActions = recommendedActions;
        this.isBlocked = isBlocked;
        this.packetSample = packetSample != null ? packetSample : "";
    }

    public String getId() { return id; }
    public long getTimestamp() { return timestamp; }
    public ThreatType getThreatType() { return threatType; }
    public ThreatSeverity getSeverity() { return severity; }
    public String getSource() { return source; }
    public String getDestination() { return destination; }
    public String getAppName() { return appName; }
    public double getAnomalyScore() { return anomalyScore; }
    public String getMitreTechnique() { return mitreTechnique; }
    public String getDescription() { return description; }
    public String getAiDiagnosis() { return aiDiagnosis; }
    public String getRecommendedActions() { return recommendedActions; }
    public boolean isBlocked() { return isBlocked; }
    public void setBlocked(boolean blocked) { isBlocked = blocked; }
    public String getPacketSample() { return packetSample; }
}
