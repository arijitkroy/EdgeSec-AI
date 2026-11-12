package com.example.edgesecai.model;

public class SecurityStats {
    private long totalPackets;
    private long totalBytes;
    private int activeFlows;
    private int threatsDetected;
    private int threatsBlocked;
    private double currentAnomalyLevel;
    private boolean protectionActive;

    public SecurityStats() {
        this.totalPackets = 0;
        this.totalBytes = 0;
        this.activeFlows = 0;
        this.threatsDetected = 0;
        this.threatsBlocked = 0;
        this.currentAnomalyLevel = 0.0;
        this.protectionActive = false;
    }

    public synchronized void recordPacket(int bytes) {
        totalPackets++;
        totalBytes += bytes;
    }

    public synchronized void incrementThreats(boolean blocked) {
        threatsDetected++;
        if (blocked) threatsBlocked++;
    }

    public synchronized void reset() {
        totalPackets = 0;
        totalBytes = 0;
        activeFlows = 0;
        threatsDetected = 0;
        threatsBlocked = 0;
        currentAnomalyLevel = 0.0;
    }

    public long getTotalPackets() { return totalPackets; }
    public void setTotalPackets(long totalPackets) { this.totalPackets = totalPackets; }

    public long getTotalBytes() { return totalBytes; }
    public void setTotalBytes(long totalBytes) { this.totalBytes = totalBytes; }

    public int getActiveFlows() { return activeFlows; }
    public void setActiveFlows(int activeFlows) { this.activeFlows = activeFlows; }

    public int getThreatsDetected() { return threatsDetected; }
    public void setThreatsDetected(int threatsDetected) { this.threatsDetected = threatsDetected; }

    public int getThreatsBlocked() { return threatsBlocked; }
    public void setThreatsBlocked(int threatsBlocked) { this.threatsBlocked = threatsBlocked; }

    public double getCurrentAnomalyLevel() { return currentAnomalyLevel; }
    public void setCurrentAnomalyLevel(double currentAnomalyLevel) { this.currentAnomalyLevel = currentAnomalyLevel; }

    public boolean isProtectionActive() { return protectionActive; }
    public void setProtectionActive(boolean protectionActive) { this.protectionActive = protectionActive; }
}
