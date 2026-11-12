package com.example.edgesecai.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NetworkFlow {
    private final String flowId;
    private final String srcIp;
    private final int srcPort;
    private final String dstIp;
    private final int dstPort;
    private final String protocol;
    private final long startTime;
    private long lastSeenTime;

    private int packetsOut;
    private int packetsIn;
    private long bytesOut;
    private long bytesIn;

    private int synCount;
    private int ackCount;
    private int rstCount;
    private int finCount;

    private final List<Long> interArrivalTimes = Collections.synchronizedList(new ArrayList<>());
    private final List<String> dnsQueries = Collections.synchronizedList(new ArrayList<>());
    private long previousPacketTimestamp = 0;

    private int uid;
    private String appName = "Unknown";
    private double anomalyScore = 0.0;
    private String classification = "Normal";
    private boolean isBlocked = false;

    public NetworkFlow(String flowId, String srcIp, int srcPort, String dstIp, int dstPort, String protocol, long startTime) {
        this.flowId = flowId;
        this.srcIp = srcIp;
        this.srcPort = srcPort;
        this.dstIp = dstIp;
        this.dstPort = dstPort;
        this.protocol = protocol;
        this.startTime = startTime;
        this.lastSeenTime = startTime;
    }

    public synchronized void recordPacket(NetworkPacket packet) {
        lastSeenTime = packet.getTimestamp();

        if (previousPacketTimestamp > 0) {
            long delta = packet.getTimestamp() - previousPacketTimestamp;
            if (delta >= 0 && interArrivalTimes.size() < 100) {
                interArrivalTimes.add(delta);
            }
        }
        previousPacketTimestamp = packet.getTimestamp();

        if (packet.isOutbound()) {
            packetsOut++;
            bytesOut += packet.getLength();
        } else {
            packetsIn++;
            bytesIn += packet.getLength();
        }

        String flags = packet.getTcpFlags();
        if (flags.contains("SYN")) synCount++;
        if (flags.contains("ACK")) ackCount++;
        if (flags.contains("RST")) rstCount++;
        if (flags.contains("FIN")) finCount++;

        if (packet.getDnsQuery() != null && !packet.getDnsQuery().isEmpty() && !dnsQueries.contains(packet.getDnsQuery())) {
            dnsQueries.add(packet.getDnsQuery());
        }

        if (packet.getUid() > 0) {
            this.uid = packet.getUid();
        }
        if (packet.getAppName() != null && !packet.getAppName().equals("Unknown")) {
            this.appName = packet.getAppName();
        }
    }

    public String getFlowId() { return flowId; }
    public String getSrcIp() { return srcIp; }
    public int getSrcPort() { return srcPort; }
    public String getDstIp() { return dstIp; }
    public int getDstPort() { return dstPort; }
    public String getProtocol() { return protocol; }
    public long getStartTime() { return startTime; }
    public long getLastSeenTime() { return lastSeenTime; }
    public long getDurationMs() { return Math.max(1, lastSeenTime - startTime); }

    public int getTotalPackets() { return packetsOut + packetsIn; }
    public int getPacketsOut() { return packetsOut; }
    public int getPacketsIn() { return packetsIn; }
    public long getTotalBytes() { return bytesOut + bytesIn; }
    public long getBytesOut() { return bytesOut; }
    public long getBytesIn() { return bytesIn; }

    public int getSynCount() { return synCount; }
    public int getAckCount() { return ackCount; }
    public int getRstCount() { return rstCount; }
    public int getFinCount() { return finCount; }

    public List<Long> getInterArrivalTimes() { return new ArrayList<>(interArrivalTimes); }
    public List<String> getDnsQueries() { return new ArrayList<>(dnsQueries); }

    public int getUid() { return uid; }
    public void setUid(int uid) { this.uid = uid; }

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    public double getAnomalyScore() { return anomalyScore; }
    public void setAnomalyScore(double anomalyScore) { this.anomalyScore = anomalyScore; }

    public String getClassification() { return classification; }
    public void setClassification(String classification) { this.classification = classification; }

    public boolean isBlocked() { return isBlocked; }
    public void setBlocked(boolean blocked) { isBlocked = blocked; }
}
