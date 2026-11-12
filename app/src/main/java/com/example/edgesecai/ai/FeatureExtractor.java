package com.example.edgesecai.ai;

import com.example.edgesecai.model.NetworkFlow;
import com.example.edgesecai.model.NetworkPacket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeatureExtractor {

    public static FeatureVector extract(NetworkFlow flow, List<NetworkPacket> recentPackets) {
        long duration = Math.max(1, flow.getDurationMs());
        double durationSec = duration / 1000.0;
        int totalPackets = flow.getTotalPackets();
        long totalBytes = flow.getTotalBytes();

        double packetRate = durationSec > 0 ? (totalPackets / durationSec) : totalPackets;
        double byteRate = durationSec > 0 ? (totalBytes / durationSec) : totalBytes;
        double bytesPerPacket = totalPackets > 0 ? ((double) totalBytes / totalPackets) : 0;
        double outboundInboundRatio = (double) (flow.getBytesOut() + 1) / (double) (flow.getBytesIn() + 1);

        // Inter-arrival time stats
        List<Long> iatList = flow.getInterArrivalTimes();
        double iatMean = 0;
        double iatVariance = 0;
        double iatCV = 1.0; // default for sporadic

        if (iatList.size() >= 2) {
            double sum = 0;
            for (Long t : iatList) sum += t;
            iatMean = sum / iatList.size();

            double varSum = 0;
            for (Long t : iatList) {
                varSum += Math.pow(t - iatMean, 2);
            }
            iatVariance = varSum / iatList.size();
            double stdDev = Math.sqrt(iatVariance);
            iatCV = iatMean > 0 ? (stdDev / iatMean) : 1.0;
        }

        // Payload entropy
        double payloadEntropy = 0;
        if (recentPackets != null && !recentPackets.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (NetworkPacket p : recentPackets) {
                if (!p.getPayloadPreview().isEmpty()) {
                    sb.append(p.getPayloadPreview());
                }
            }
            if (sb.length() > 0) {
                payloadEntropy = calculateShannonEntropy(sb.toString());
            }
        }

        // DNS domain entropy
        double dnsEntropy = 0;
        List<String> domains = flow.getDnsQueries();
        if (!domains.isEmpty()) {
            double maxEntropy = 0;
            for (String domain : domains) {
                double e = calculateShannonEntropy(domain);
                if (e > maxEntropy) maxEntropy = e;
            }
            dnsEntropy = maxEntropy;
        }

        // SYN/ACK ratio
        double synAckRatio = (double) (flow.getSynCount() + 1) / (double) (flow.getAckCount() + 1);

        // Port entropy across recent packets
        double portEntropy = 0;
        if (recentPackets != null && recentPackets.size() > 1) {
            Map<Integer, Integer> portCounts = new HashMap<>();
            for (NetworkPacket p : recentPackets) {
                portCounts.put(p.getDstPort(), portCounts.getOrDefault(p.getDstPort(), 0) + 1);
            }
            int total = recentPackets.size();
            for (int count : portCounts.values()) {
                double p = (double) count / total;
                portEntropy -= p * (Math.log(p) / Math.log(2));
            }
        }

        return new FeatureVector(
                duration, packetRate, byteRate, bytesPerPacket,
                outboundInboundRatio, iatMean, iatVariance, iatCV,
                payloadEntropy, dnsEntropy, synAckRatio, portEntropy
        );
    }

    public static double calculateShannonEntropy(String input) {
        if (input == null || input.isEmpty()) return 0.0;
        Map<Character, Integer> freqMap = new HashMap<>();
        for (char c : input.toCharArray()) {
            freqMap.put(c, freqMap.getOrDefault(c, 0) + 1);
        }
        double length = input.length();
        double entropy = 0.0;
        double log2 = Math.log(2);
        for (int count : freqMap.values()) {
            double p = count / length;
            entropy -= p * (Math.log(p) / log2);
        }
        return entropy;
    }
}
