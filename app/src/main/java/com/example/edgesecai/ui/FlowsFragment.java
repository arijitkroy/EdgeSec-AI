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

import java.util.List;

public class FlowsFragment extends Fragment implements SecurityRepository.SecurityUpdateListener {

    private RecyclerView recyclerFlows;
    private TextView textNoFlows;
    private NetworkFlowAdapter adapter;
    private SecurityRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_flows, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerFlows = view.findViewById(R.id.recyclerFlows);
        textNoFlows = view.findViewById(R.id.textNoFlows);

        adapter = new NetworkFlowAdapter();
        recyclerFlows.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerFlows.setAdapter(adapter);

        repository = SecurityRepository.getInstance(requireContext());
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
        // Not needed here
    }

    @Override
    public void onStatsChanged(SecurityStats stats) {
        // Not needed here
    }

    @Override
    public void onFlowsUpdated(List<NetworkFlow> flows) {
        if (!isAdded()) return;
        if (flows != null && !flows.isEmpty()) {
            adapter.setFlows(flows);
            recyclerFlows.setVisibility(View.VISIBLE);
            textNoFlows.setVisibility(View.GONE);
        } else {
            recyclerFlows.setVisibility(View.GONE);
            textNoFlows.setVisibility(View.VISIBLE);
        }
    }
}
