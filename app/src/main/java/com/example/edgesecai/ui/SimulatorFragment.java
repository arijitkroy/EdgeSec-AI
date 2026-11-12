package com.example.edgesecai.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.edgesecai.R;
import com.example.edgesecai.engine.AttackSimulator;
import com.example.edgesecai.storage.SecurityRepository;
import com.google.android.material.button.MaterialButton;

public class SimulatorFragment extends Fragment {

    private AttackSimulator simulator;
    private TextView textResult;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_simulator, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SecurityRepository repository = SecurityRepository.getInstance(requireContext());
        simulator = new AttackSimulator(repository);
        textResult = view.findViewById(R.id.textSimulatorResult);

        MaterialButton btnPortScan = view.findViewById(R.id.btnSimPortScan);
        MaterialButton btnC2Beacon = view.findViewById(R.id.btnSimC2Beacon);
        MaterialButton btnDnsTunnel = view.findViewById(R.id.btnSimDnsTunnel);
        MaterialButton btnSynFlood = view.findViewById(R.id.btnSimSynFlood);
        MaterialButton btnCryptominer = view.findViewById(R.id.btnSimCryptominer);
        MaterialButton btnNormalTraffic = view.findViewById(R.id.btnSimNormalTraffic);

        AttackSimulator.SimulatorCallback callback = this::showResult;

        btnPortScan.setOnClickListener(v -> simulator.simulatePortScan(callback));
        btnC2Beacon.setOnClickListener(v -> simulator.simulateC2Beaconing(callback));
        btnDnsTunnel.setOnClickListener(v -> simulator.simulateDnsTunneling(callback));
        btnSynFlood.setOnClickListener(v -> simulator.simulateSynFlood(callback));
        btnCryptominer.setOnClickListener(v -> simulator.simulateCryptominer(callback));
        btnNormalTraffic.setOnClickListener(v -> simulator.simulateNormalTraffic(callback));
    }

    private void showResult(String message) {
        mainHandler.post(() -> {
            if (isAdded()) {
                textResult.setText("✅ " + message);
                textResult.setVisibility(View.VISIBLE);
            }
        });
    }
}
