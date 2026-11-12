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
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class AlertsFragment extends Fragment implements SecurityRepository.SecurityUpdateListener {

    private RecyclerView recyclerAlerts;
    private TextView textNoAlerts;
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

        // Load existing alerts
        List<ThreatAlert> existing = repository.getAlerts();
        if (!existing.isEmpty()) {
            adapter.setAlerts(existing);
            recyclerAlerts.setVisibility(View.VISIBLE);
            textNoAlerts.setVisibility(View.GONE);
        }
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
        recyclerAlerts.smoothScrollToPosition(0);
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
