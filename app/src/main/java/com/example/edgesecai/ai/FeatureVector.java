package com.example.edgesecai.ai;

public class FeatureVector {
    public final double flowDurationMs;
    public final double packetRate;
    public final double byteRate;
    public final double bytesPerPacket;
    public final double outboundInboundRatio;
    public final double interArrivalTimeMean;
    public final double interArrivalTimeVariance;
    public final double interArrivalTimeCV; // Coefficient of Variation (stdDev / mean)
    public final double payloadEntropy;
    public final double dnsEntropy;
    public final double synAckRatio;
    public final double portEntropy;

    public FeatureVector(double flowDurationMs, double packetRate, double byteRate,
                         double bytesPerPacket, double outboundInboundRatio,
                         double interArrivalTimeMean, double interArrivalTimeVariance,
                         double interArrivalTimeCV, double payloadEntropy,
                         double dnsEntropy, double synAckRatio, double portEntropy) {
        this.flowDurationMs = flowDurationMs;
        this.packetRate = packetRate;
        this.byteRate = byteRate;
        this.bytesPerPacket = bytesPerPacket;
        this.outboundInboundRatio = outboundInboundRatio;
        this.interArrivalTimeMean = interArrivalTimeMean;
        this.interArrivalTimeVariance = interArrivalTimeVariance;
        this.interArrivalTimeCV = interArrivalTimeCV;
        this.payloadEntropy = payloadEntropy;
        this.dnsEntropy = dnsEntropy;
        this.synAckRatio = synAckRatio;
        this.portEntropy = portEntropy;
    }

    public double[] toArray() {
        return new double[]{
                normalize(flowDurationMs, 0, 60000),
                normalize(packetRate, 0, 100),
                normalize(byteRate, 0, 100000),
                normalize(bytesPerPacket, 0, 1500),
                normalize(outboundInboundRatio, 0, 50),
                normalize(interArrivalTimeMean, 0, 10000),
                normalize(interArrivalTimeCV, 0, 2),
                normalize(payloadEntropy, 0, 8),
                normalize(dnsEntropy, 0, 8),
                normalize(synAckRatio, 0, 20),
                normalize(portEntropy, 0, 5)
        };
    }

    private double normalize(double val, double min, double max) {
        if (Double.isNaN(val) || Double.isInfinite(val)) return 0.0;
        if (val <= min) return 0.0;
        if (val >= max) return 1.0;
        return (val - min) / (max - min);
    }
}
