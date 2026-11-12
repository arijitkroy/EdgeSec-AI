package com.example.edgesecai.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.TrafficStats;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.edgesecai.R;
import com.example.edgesecai.engine.VpnInspectorService;
import com.example.edgesecai.model.NetworkFlow;
import com.example.edgesecai.model.SecurityStats;
import com.example.edgesecai.model.ThreatAlert;
import com.example.edgesecai.storage.SecurityRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment implements SecurityRepository.SecurityUpdateListener {

    private TextView textShieldStatus, textShieldSubtitle;
    private MaterialButton btnToggleShield;
    private TextView textPacketCount, textActiveFlows, textThreatsDetected, textThreatsBlocked;
    private LinearProgressIndicator progressAnomaly;
    private TextView textAnomalyPercent, textDataVolume;

    private SecurityRepository repository;
    private boolean isShieldActive = false;
    private final Handler trafficHandler = new Handler(Looper.getMainLooper());
    private long previousRxBytes = TrafficStats.getTotalRxBytes();
    private long previousTxBytes = TrafficStats.getTotalTxBytes();
    private long previousTrafficSampleTime = System.currentTimeMillis();

    private final Runnable trafficSampler = new Runnable() {
        @Override
        public void run() {
            if (!isAdded() || textDataVolume == null) return;

            long rxBytes = TrafficStats.getTotalRxBytes();
            long txBytes = TrafficStats.getTotalTxBytes();
            long now = System.currentTimeMillis();
            long elapsedMs = Math.max(1L, now - previousTrafficSampleTime);

            if (rxBytes < 0 || txBytes < 0) {
                textDataVolume.setText("Unavailable on this device");
            } else if (previousRxBytes >= 0 && previousTxBytes >= 0) {
                double rxPerSecond = Math.max(0L, rxBytes - previousRxBytes) * 1000.0 / elapsedMs;
                double txPerSecond = Math.max(0L, txBytes - previousTxBytes) * 1000.0 / elapsedMs;
                textDataVolume.setText(String.format(Locale.US, "↓ %s/s   ↑ %s/s",
                        formatRate(rxPerSecond), formatRate(txPerSecond)));
            }

            previousRxBytes = rxBytes;
            previousTxBytes = txBytes;
            previousTrafficSampleTime = now;
            trafficHandler.postDelayed(this, 1000L);
        }
    };

    private static String formatRate(double bytesPerSecond) {
        if (bytesPerSecond < 1024.0) return String.format(Locale.US, "%.0f B", bytesPerSecond);
        if (bytesPerSecond < 1024.0 * 1024.0) {
            return String.format(Locale.US, "%.1f KB", bytesPerSecond / 1024.0);
        }
        return String.format(Locale.US, "%.2f MB", bytesPerSecond / (1024.0 * 1024.0));
    }

    private final ActivityResultLauncher<Intent> vpnLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    startVpnService();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textShieldStatus = view.findViewById(R.id.textShieldStatus);
        textShieldSubtitle = view.findViewById(R.id.textShieldSubtitle);
        btnToggleShield = view.findViewById(R.id.btnToggleShield);
        textPacketCount = view.findViewById(R.id.textPacketCount);
        textActiveFlows = view.findViewById(R.id.textActiveFlows);
        textThreatsDetected = view.findViewById(R.id.textThreatsDetected);
        textThreatsBlocked = view.findViewById(R.id.textThreatsBlocked);
        progressAnomaly = view.findViewById(R.id.progressAnomaly);
        textAnomalyPercent = view.findViewById(R.id.textAnomalyPercent);
        textDataVolume = view.findViewById(R.id.textDataVolume);

        repository = SecurityRepository.getInstance(requireContext());

        btnToggleShield.setOnClickListener(v -> {
            if (isShieldActive) {
                stopVpnService();
            } else {
                requestVpn();
            }
        });

        updateShieldUI();
    }

    @Override
    public void onResume() {
        super.onResume();
        repository.addListener(this);
        previousRxBytes = TrafficStats.getTotalRxBytes();
        previousTxBytes = TrafficStats.getTotalTxBytes();
        previousTrafficSampleTime = System.currentTimeMillis();
        trafficHandler.post(trafficSampler);
    }

    @Override
    public void onPause() {
        super.onPause();
        trafficHandler.removeCallbacks(trafficSampler);
        repository.removeListener(this);
    }

    private void requestVpn() {
        Intent intent = VpnService.prepare(requireContext());
        if (intent != null) {
            vpnLauncher.launch(intent);
        } else {
            startVpnService();
        }
    }

    private void startVpnService() {
        Intent serviceIntent = new Intent(requireContext(), VpnInspectorService.class);
        serviceIntent.setAction(VpnInspectorService.ACTION_START);
        requireContext().startForegroundService(serviceIntent);
        // The service publishes active state only after both the TUN and forwarder start.
    }

    private void stopVpnService() {
        Intent serviceIntent = new Intent(requireContext(), VpnInspectorService.class);
        serviceIntent.setAction(VpnInspectorService.ACTION_STOP);
        requireContext().startService(serviceIntent);
        isShieldActive = false;
        updateShieldUI();
    }

    private void updateShieldUI() {
        if (isShieldActive) {
            textShieldStatus.setText(R.string.shield_active);
            textShieldStatus.setTextColor(getResources().getColor(R.color.shield_active_glow, null));
            textShieldSubtitle.setText(R.string.shield_subtitle_active);
            btnToggleShield.setText(R.string.deactivate_shield);
        } else {
            textShieldStatus.setText(R.string.shield_inactive);
            textShieldStatus.setTextColor(getResources().getColor(R.color.shield_inactive, null));
            textShieldSubtitle.setText(R.string.shield_subtitle_inactive);
            btnToggleShield.setText(R.string.activate_shield);
        }
    }

    @Override
    public void onAlertDetected(ThreatAlert alert) {
        // Stats will update via onStatsChanged
    }

    @Override
    public void onStatsChanged(SecurityStats stats) {
        if (!isAdded()) return;
        textPacketCount.setText(String.valueOf(stats.getTotalPackets()));
        textActiveFlows.setText(String.valueOf(stats.getActiveFlows()));
        textThreatsDetected.setText(String.valueOf(stats.getThreatsDetected()));
        textThreatsBlocked.setText(String.valueOf(stats.getThreatsBlocked()));

        int anomalyPercent = (int) (stats.getCurrentAnomalyLevel() * 100);
        progressAnomaly.setProgress(anomalyPercent);

        if (anomalyPercent >= 75) {
            progressAnomaly.setIndicatorColor(Color.parseColor("#E53935"));
            textAnomalyPercent.setTextColor(Color.parseColor("#E53935"));
        } else if (anomalyPercent >= 50) {
            progressAnomaly.setIndicatorColor(Color.parseColor("#FB8C00"));
            textAnomalyPercent.setTextColor(Color.parseColor("#FB8C00"));
        } else {
            progressAnomaly.setIndicatorColor(Color.parseColor("#43A047"));
            textAnomalyPercent.setTextColor(getResources().getColor(R.color.text_secondary, null));
        }
        textAnomalyPercent.setText(String.format(Locale.US, "%d%% %s", anomalyPercent,
                anomalyPercent < 25 ? "— Clean Baseline" : anomalyPercent < 60 ? "— Elevated" : "— CRITICAL"));

        // Live device-wide throughput is sampled independently of VPN packet inspection.

        isShieldActive = stats.isProtectionActive();
        updateShieldUI();
    }

    @Override
    public void onFlowsUpdated(List<NetworkFlow> flows) {
        // Handled in FlowsFragment
    }
}
