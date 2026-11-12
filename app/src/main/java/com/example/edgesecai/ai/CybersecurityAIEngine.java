package com.example.edgesecai.ai;

import com.example.edgesecai.model.NetworkFlow;
import com.example.edgesecai.model.NetworkPacket;
import com.example.edgesecai.model.ThreatAlert;

import java.util.List;

public class CybersecurityAIEngine {
    private final IsolationForestModel isolationForestModel;
    private final BaselineProfiler baselineProfiler;

    public CybersecurityAIEngine() {
        this.isolationForestModel = new IsolationForestModel();
        this.baselineProfiler = new BaselineProfiler();
    }

    public ThreatAlert analyzeFlow(NetworkFlow flow, List<NetworkPacket> recentPackets) {
        if (flow == null) return null;

        // 1. Feature extraction
        FeatureVector features = FeatureExtractor.extract(flow, recentPackets);

        // 2. Unsupervised Anomaly Scoring via Isolation Forest
        double anomalyScore = isolationForestModel.calculateAnomalyScore(features.toArray());
        flow.setAnomalyScore(anomalyScore);

        // 3. Update dynamic baseline (only with non-anomalous flows)
        if (anomalyScore < 0.65) {
            baselineProfiler.updateBaseline(features.packetRate, features.byteRate);
        }

        // 4. Malware Behavior Detection (C2, Exfil, DNS Tunneling, Mining)
        ThreatAlert malwareAlert = MalwareBehaviorDetector.evaluate(flow, features, recentPackets);
        if (malwareAlert != null) {
            flow.setClassification(malwareAlert.getThreatType().getDisplayName());
            if (malwareAlert.isBlocked()) flow.setBlocked(true);
            return malwareAlert;
        }

        // 5. Network Intrusion Detection (Port Scans, SYN Floods, Traffic Outliers)
        ThreatAlert intrusionAlert = NetworkIntrusionDetector.evaluate(
                flow, features, recentPackets, baselineProfiler, anomalyScore
        );
        if (intrusionAlert != null) {
            flow.setClassification(intrusionAlert.getThreatType().getDisplayName());
            if (intrusionAlert.isBlocked()) flow.setBlocked(true);
            return intrusionAlert;
        }

        flow.setClassification("Normal");
        return null;
    }

    public IsolationForestModel getIsolationForestModel() {
        return isolationForestModel;
    }

    public BaselineProfiler getBaselineProfiler() {
        return baselineProfiler;
    }
}
