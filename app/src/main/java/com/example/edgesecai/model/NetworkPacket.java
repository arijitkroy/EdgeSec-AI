package com.example.edgesecai.model;

public class NetworkPacket {
    private final long timestamp;
    private final String srcIp;
    private final int srcPort;
    private final String dstIp;
    private final int dstPort;
    private final String protocol;
    private final int length;
    private final String tcpFlags;
    private final String dnsQuery;
    private final String payloadPreview;
    private final int uid;
    private final String appName;
    private final boolean isOutbound;

    public NetworkPacket(long timestamp, String srcIp, int srcPort, String dstIp, int dstPort,
                         String protocol, int length, String tcpFlags, String dnsQuery,
                         String payloadPreview, int uid, String appName, boolean isOutbound) {
        this.timestamp = timestamp;
        this.srcIp = srcIp;
        this.srcPort = srcPort;
        this.dstIp = dstIp;
        this.dstPort = dstPort;
        this.protocol = protocol;
        this.length = length;
        this.tcpFlags = tcpFlags != null ? tcpFlags : "";
        this.dnsQuery = dnsQuery != null ? dnsQuery : "";
        this.payloadPreview = payloadPreview != null ? payloadPreview : "";
        this.uid = uid;
        this.appName = appName != null ? appName : "Unknown";
        this.isOutbound = isOutbound;
    }

    public long getTimestamp() { return timestamp; }
    public String getSrcIp() { return srcIp; }
    public int getSrcPort() { return srcPort; }
    public String getDstIp() { return dstIp; }
    public int getDstPort() { return dstPort; }
    public String getProtocol() { return protocol; }
    public int getLength() { return length; }
    public String getTcpFlags() { return tcpFlags; }
    public String getDnsQuery() { return dnsQuery; }
    public String getPayloadPreview() { return payloadPreview; }
    public int getUid() { return uid; }
    public String getAppName() { return appName; }
    public boolean isOutbound() { return isOutbound; }

    public String getFlowKey() {
        return srcIp + ":" + srcPort + "->" + dstIp + ":" + dstPort + "(" + protocol + ")";
    }
}
