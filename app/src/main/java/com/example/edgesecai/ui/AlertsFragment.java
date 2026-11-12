package com.example.edgesecai.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edgesecai.R;
import com.example.edgesecai.model.NetworkFlow;
import com.example.edgesecai.model.SecurityStats;
import com.example.edgesecai.model.ThreatAlert;
import com.example.edgesecai.storage.SecurityRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class AlertsFragment extends Fragment implements SecurityRepository.SecurityUpdateListener {

    private RecyclerView recyclerAlerts;
    private TextView textNoAlerts;
    private MaterialButton btnClearAlerts;
    private ThreatAlertAdapter adapter;
    private SecurityRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alerts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerAlerts = view.findViewById(R.id.recyclerAlerts);
        textNoAlerts = view.findViewById(R.id.textNoAlerts);
        btnClearAlerts = view.findViewById(R.id.btnClearAlerts);
        btnClearAlerts.setEnabled(false);

        adapter = new ThreatAlertAdapter();
        adapter.setOnBlockClickListener(alert -> {
            repository.blockEndpoint(
                    alert.getDestination().split(":")[0],
                    0
            );
            Snackbar.make(view, "Endpoint blocked: " + alert.getDestination(), Snackbar.LENGTH_SHORT).show();
        });

        recyclerAlerts.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerAlerts.setAdapter(adapter);

        repository = SecurityRepository.getInstance(requireContext());
        btnClearAlerts.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.clear_alerts_title)
                .setMessage(R.string.clear_alerts_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.clear_entries, (dialog, which) -> repository.clearAlerts())
                .show());
    }

    @Override
    public void onResume() {
        super.onResume();
        repository.addListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        repository.removeListener(this);
    }

    @Override
    public void onAlertDetected(ThreatAlert alert) {
        if (!isAdded()) return;
        adapter.addAlert(alert);
        recyclerAlerts.setVisibility(View.VISIBLE);
        textNoAlerts.setVisibility(View.GONE);
        btnClearAlerts.setEnabled(true);
        recyclerAlerts.smoothScrollToPosition(0);
    }

    @Override
    public void onAlertsUpdated(List<ThreatAlert> alerts) {
        if (!isAdded()) return;
        adapter.setAlerts(alerts);
        boolean hasAlerts = alerts != null && !alerts.isEmpty();
        recyclerAlerts.setVisibility(hasAlerts ? View.VISIBLE : View.GONE);
        textNoAlerts.setVisibility(hasAlerts ? View.GONE : View.VISIBLE);
        btnClearAlerts.setEnabled(hasAlerts);
    }

    @Override
    public void onStatsChanged(SecurityStats stats) {
        // Not needed here
    }

    @Override
    public void onFlowsUpdated(List<NetworkFlow> flows) {
        // Not needed here
    }
}
