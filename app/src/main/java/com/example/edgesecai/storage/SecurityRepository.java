package com.example.edgesecai.storage;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.edgesecai.ai.CybersecurityAIEngine;
import com.example.edgesecai.model.NetworkFlow;
import com.example.edgesecai.model.NetworkPacket;
import com.example.edgesecai.model.SecurityStats;
import com.example.edgesecai.model.ThreatAlert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SecurityRepository {
    private static volatile SecurityRepository instance;

    private final Context context;
    private final SecurityDatabaseHelper dbHelper;
    private final CybersecurityAIEngine aiEngine;
    private final SecurityStats stats;

    private final Map<String, NetworkFlow> activeFlows = Collections.synchronizedMap(new LinkedHashMap<>());
    private final List<NetworkPacket> recentPackets = Collections.synchronizedList(new ArrayList<>());
    private final List<ThreatAlert> alerts = new CopyOnWriteArrayList<>();
    private final Set<String> blockedIps = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> reportedAlertKeys = Collections.synchronizedSet(new HashSet<>());
    private final Set<Integer> blockedPorts = Collections.synchronizedSet(new HashSet<>());

    private final List<SecurityUpdateListener> listeners = new CopyOnWriteArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    public interface SecurityUpdateListener {
        void onAlertDetected(ThreatAlert alert);
        default void onAlertsUpdated(List<ThreatAlert> alerts) { }
        void onStatsChanged(SecurityStats stats);
        void onFlowsUpdated(List<NetworkFlow> flows);
    }

    private SecurityRepository(Context context) {
        this.context = context.getApplicationContext();
        this.dbHelper = new SecurityDatabaseHelper(this.context);
        this.aiEngine = new CybersecurityAIEngine();
        this.stats = new SecurityStats();

        // Load historical alerts from database
        backgroundExecutor.execute(() -> {
            List<ThreatAlert> savedAlerts = dbHelper.getAllAlerts();
            alerts.addAll(savedAlerts);
            stats.setThreatsDetected(savedAlerts.size());
            int blockedCount = 0;
            for (ThreatAlert a : savedAlerts) {
                if (a.isBlocked()) blockedCount++;
            }
            stats.setThreatsBlocked(blockedCount);
            notifyStatsChanged();
            notifyAlertsUpdated();
        });
    }

    public static SecurityRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (SecurityRepository.class) {
                if (instance == null) {
                    instance = new SecurityRepository(context);
                }
            }
        }
        return instance;
    }

    public boolean isTrafficBlocked(String ip, int port) {
        return blockedIps.contains(ip) || blockedPorts.contains(port);
    }

    public void blockEndpoint(String ip, int port) {
        if (ip != null && !ip.isEmpty()) blockedIps.add(ip);
        if (port > 0) blockedPorts.add(port);
    }

    public void processPacket(NetworkPacket packet) {
        if (packet == null) return;

        // Check if destination is blocked
        if (isTrafficBlocked(packet.getDstIp(), packet.getDstPort())) {
            stats.incrementThreats(true);
            notifyStatsChanged();
            return;
        }

        stats.recordPacket(packet.getLength());

        // Update sliding window
        synchronized (recentPackets) {
            recentPackets.add(packet);
            if (recentPackets.size() > 150) {
                recentPackets.remove(0);
            }
        }

        // Aggregate flow
        String flowKey = packet.getFlowKey();
        NetworkFlow flow;
        synchronized (activeFlows) {
            flow = activeFlows.get(flowKey);
            if (flow == null) {
                flow = new NetworkFlow(
                        flowKey, packet.getSrcIp(), packet.getSrcPort(),
                        packet.getDstIp(), packet.getDstPort(),
                        packet.getProtocol(), packet.getTimestamp()
                );
                activeFlows.put(flowKey, flow);
                if (activeFlows.size() > 50) {
                    // Evict oldest flow
                    String oldestKey = activeFlows.keySet().iterator().next();
                    activeFlows.remove(oldestKey);
                }
            }
        }

        flow.recordPacket(packet);

        // Run On-Device AI Cybersecurity Engine asynchronously
        NetworkFlow finalFlow = flow;
        backgroundExecutor.execute(() -> {
            List<NetworkPacket> packetSnapshot;
            synchronized (recentPackets) {
                packetSnapshot = new ArrayList<>(recentPackets);
            }

            ThreatAlert alert = aiEngine.analyzeFlow(finalFlow, packetSnapshot);
            if (alert != null) {
                String alertKey = alert.getThreatType().name() + ":" + finalFlow.getFlowId();
                if (reportedAlertKeys.add(alertKey)) {
                    alerts.add(0, alert);
                    dbHelper.insertAlert(alert);
                    stats.incrementThreats(alert.isBlocked());
                    if (alert.isBlocked()) {
                        blockEndpoint(alert.getDestination().split(":")[0], finalFlow.getDstPort());
                    }
                    notifyAlert(alert);
                }
            }

            stats.setActiveFlows(activeFlows.size());
            stats.setCurrentAnomalyLevel(finalFlow.getAnomalyScore());

            dbHelper.insertOrUpdateFlow(finalFlow);

            notifyStatsChanged();
            notifyFlowsUpdated();
        });
    }

    public void addSimulatedAlert(ThreatAlert alert) {
        if (alert == null) return;
        alerts.add(0, alert);
        backgroundExecutor.execute(() -> dbHelper.insertAlert(alert));
        stats.incrementThreats(alert.isBlocked());
        if (alert.isBlocked()) {
            blockEndpoint(alert.getDestination().split(":")[0], 0);
        }
        notifyAlert(alert);
        notifyStatsChanged();
    }

    public void addListener(SecurityUpdateListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            listener.onAlertsUpdated(getAlerts());
            listener.onStatsChanged(stats);
            listener.onFlowsUpdated(getActiveFlowsList());
        }
    }

    public void removeListener(SecurityUpdateListener listener) {
        listeners.remove(listener);
    }

    private void notifyAlert(ThreatAlert alert) {
        mainHandler.post(() -> {
            for (SecurityUpdateListener l : listeners) {
                l.onAlertDetected(alert);
            }
        });
    }

    private void notifyAlertsUpdated() {
        List<ThreatAlert> copy = getAlerts();
        mainHandler.post(() -> {
            for (SecurityUpdateListener l : listeners) {
                l.onAlertsUpdated(copy);
            }
        });
    }

    private void notifyStatsChanged() {
        mainHandler.post(() -> {
            for (SecurityUpdateListener l : listeners) {
                l.onStatsChanged(stats);
            }
        });
    }

    private void notifyFlowsUpdated() {
        List<NetworkFlow> copy = getActiveFlowsList();
        mainHandler.post(() -> {
            for (SecurityUpdateListener l : listeners) {
                l.onFlowsUpdated(copy);
            }
        });
    }

    public List<ThreatAlert> getAlerts() {
        return new ArrayList<>(alerts);
    }

    public void clearAlerts() {
        backgroundExecutor.execute(() -> {
            alerts.clear();
            stats.setThreatsDetected(0);
            stats.setThreatsBlocked(0);
            dbHelper.clearAlerts();
            notifyAlertsUpdated();
            notifyStatsChanged();
        });
    }

    public void clearFlows() {
        synchronized (activeFlows) {
            activeFlows.clear();
        }
        synchronized (recentPackets) {
            recentPackets.clear();
        }
        stats.setActiveFlows(0);
        backgroundExecutor.execute(dbHelper::clearFlows);
        notifyStatsChanged();
        notifyFlowsUpdated();
    }

    public List<NetworkFlow> getActiveFlowsList() {
        synchronized (activeFlows) {
            return new ArrayList<>(activeFlows.values());
        }
    }

    public SecurityStats getStats() {
        return stats;
    }

    public void setProtectionActive(boolean active) {
        stats.setProtectionActive(active);
        notifyStatsChanged();
    }

    public CybersecurityAIEngine getAiEngine() {
        return aiEngine;
    }

    public void clearAll() {
        activeFlows.clear();
        recentPackets.clear();
        alerts.clear();
        reportedAlertKeys.clear();
        blockedIps.clear();
        blockedPorts.clear();
        stats.reset();
        backgroundExecutor.execute(dbHelper::clearAllData);
        notifyAlertsUpdated();
        notifyStatsChanged();
        notifyFlowsUpdated();
    }
}
