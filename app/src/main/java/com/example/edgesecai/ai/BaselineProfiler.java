package com.example.edgesecai.ai;

public class BaselineProfiler {
    private static final double ALPHA = 0.05; // Smoothing factor for EMA
    private double meanPacketRate = 5.0;
    private double varPacketRate = 4.0;
    private double meanByteRate = 2048.0;
    private double varByteRate = 409600.0;
    private long totalSamples = 0;

    public synchronized void updateBaseline(double packetRate, double byteRate) {
        if (totalSamples == 0) {
            meanPacketRate = packetRate;
            meanByteRate = byteRate;
            totalSamples++;
            return;
        }

        double deltaP = packetRate - meanPacketRate;
        meanPacketRate += ALPHA * deltaP;
        varPacketRate = (1 - ALPHA) * varPacketRate + ALPHA * (deltaP * deltaP);

        double deltaB = byteRate - meanByteRate;
        meanByteRate += ALPHA * deltaB;
        varByteRate = (1 - ALPHA) * varByteRate + ALPHA * (deltaB * deltaB);

        totalSamples++;
    }

    public synchronized double computePacketRateZScore(double packetRate) {
        double stdDev = Math.sqrt(Math.max(1.0, varPacketRate));
        return Math.max(0, (packetRate - meanPacketRate) / stdDev);
    }

    public synchronized double computeByteRateZScore(double byteRate) {
        double stdDev = Math.sqrt(Math.max(1.0, varByteRate));
        return Math.max(0, (byteRate - meanByteRate) / stdDev);
    }

    public synchronized double getMeanPacketRate() { return meanPacketRate; }
    public synchronized double getMeanByteRate() { return meanByteRate; }
}
