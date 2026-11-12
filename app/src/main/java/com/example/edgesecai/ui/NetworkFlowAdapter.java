package com.example.edgesecai.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edgesecai.R;
import com.example.edgesecai.model.NetworkFlow;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NetworkFlowAdapter extends RecyclerView.Adapter<NetworkFlowAdapter.ViewHolder> {

    private final List<NetworkFlow> flows = new ArrayList<>();

    public void setFlows(List<NetworkFlow> newFlows) {
        flows.clear();
        if (newFlows != null) flows.addAll(newFlows);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_network_flow, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NetworkFlow flow = flows.get(position);

        holder.textFlowProtocol.setText(flow.getProtocol());
        holder.textFlowAppName.setText(flow.getAppName());

        // Classification color
        String classification = flow.getClassification();
        if ("Normal".equals(classification)) {
            holder.textFlowClassification.setTextColor(Color.parseColor("#43A047"));
        } else {
            holder.textFlowClassification.setTextColor(Color.parseColor("#E53935"));
        }
        holder.textFlowClassification.setText(classification);

        // Endpoints
        holder.textFlowEndpoints.setText(
                flow.getSrcIp() + ":" + flow.getSrcPort() + " → " + flow.getDstIp() + ":" + flow.getDstPort()
        );

        // Stats
        long totalBytes = flow.getTotalBytes();
        String bytesStr;
        if (totalBytes < 1024) bytesStr = totalBytes + " B";
        else if (totalBytes < 1024 * 1024) bytesStr = String.format(Locale.US, "%.1f KB", totalBytes / 1024.0);
        else bytesStr = String.format(Locale.US, "%.2f MB", totalBytes / (1024.0 * 1024.0));

        double durationSec = flow.getDurationMs() / 1000.0;
        holder.textFlowStats.setText(String.format(Locale.US, "%d I/O events • %s payload • %.1fs",
                flow.getTotalPackets(), bytesStr, durationSec));

        // Anomaly score
        double scorePercent = flow.getAnomalyScore() * 100;
        holder.textFlowAnomalyScore.setText(String.format(Locale.US, "Model score: %.0f%%", scorePercent));
        if (scorePercent >= 85) {
            holder.textFlowAnomalyScore.setTextColor(Color.parseColor("#E53935"));
        } else if (scorePercent >= 70) {
            holder.textFlowAnomalyScore.setTextColor(Color.parseColor("#FB8C00"));
        } else {
            holder.textFlowAnomalyScore.setTextColor(Color.parseColor("#94A3B8"));
        }
    }

    @Override
    public int getItemCount() {
        return flows.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textFlowProtocol, textFlowAppName, textFlowClassification;
        TextView textFlowEndpoints, textFlowStats, textFlowAnomalyScore;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textFlowProtocol = itemView.findViewById(R.id.textFlowProtocol);
            textFlowAppName = itemView.findViewById(R.id.textFlowAppName);
            textFlowClassification = itemView.findViewById(R.id.textFlowClassification);
            textFlowEndpoints = itemView.findViewById(R.id.textFlowEndpoints);
            textFlowStats = itemView.findViewById(R.id.textFlowStats);
            textFlowAnomalyScore = itemView.findViewById(R.id.textFlowAnomalyScore);
        }
    }
}
