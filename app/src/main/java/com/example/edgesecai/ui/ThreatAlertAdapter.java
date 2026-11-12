package com.example.edgesecai.ui;

import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edgesecai.R;
import com.example.edgesecai.model.ThreatAlert;
import com.example.edgesecai.model.ThreatSeverity;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ThreatAlertAdapter extends RecyclerView.Adapter<ThreatAlertAdapter.ViewHolder> {

    public interface OnBlockClickListener {
        void onBlockClicked(ThreatAlert alert);
    }

    private final List<ThreatAlert> alerts = new ArrayList<>();
    private OnBlockClickListener blockListener;

    public void setOnBlockClickListener(OnBlockClickListener listener) {
        this.blockListener = listener;
    }

    public void setAlerts(List<ThreatAlert> newAlerts) {
        alerts.clear();
        if (newAlerts != null) alerts.addAll(newAlerts);
        notifyDataSetChanged();
    }

    public void addAlert(ThreatAlert alert) {
        alerts.add(0, alert);
        notifyItemInserted(0);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_threat_alert, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ThreatAlert alert = alerts.get(position);

        // Severity badge
        holder.textSeverityBadge.setText(alert.getSeverity().getLabel().toUpperCase(Locale.US));
        holder.textSeverityBadge.setBackgroundColor(Color.parseColor(alert.getSeverity().getColorHex()));

        // MITRE technique
        holder.textMitreTechnique.setText(alert.getMitreTechnique());

        // Anomaly score
        holder.textAnomalyScore.setText(String.format(Locale.US, "%.1f%% AI Confidence", alert.getAnomalyScore() * 100));
        if (alert.getAnomalyScore() >= 0.90) {
            holder.textAnomalyScore.setTextColor(Color.parseColor(ThreatSeverity.CRITICAL.getColorHex()));
        } else if (alert.getAnomalyScore() >= 0.75) {
            holder.textAnomalyScore.setTextColor(Color.parseColor(ThreatSeverity.HIGH.getColorHex()));
        } else {
            holder.textAnomalyScore.setTextColor(Color.parseColor(ThreatSeverity.MEDIUM.getColorHex()));
        }

        // Threat title
        holder.textThreatTitle.setText(alert.getDescription());

        // Endpoints
        holder.textEndpoints.setText(alert.getSource() + " → " + alert.getDestination());
        holder.textAppName.setText("App: " + alert.getAppName());

        // Timestamp
        CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                alert.getTimestamp(), System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS);
        holder.textTimestamp.setText(relativeTime);

        // Blocked badge
        holder.textBlockedBadge.setVisibility(alert.isBlocked() ? View.VISIBLE : View.GONE);

        // Expanded details
        holder.textAiDiagnosis.setText(alert.getAiDiagnosis());
        holder.textRecommendedActions.setText(alert.getRecommendedActions());

        if (alert.getPacketSample() != null && !alert.getPacketSample().isEmpty()) {
            holder.textForensicsHeader.setVisibility(View.VISIBLE);
            holder.textForensics.setVisibility(View.VISIBLE);
            holder.textForensics.setText(alert.getPacketSample());
        } else {
            holder.textForensicsHeader.setVisibility(View.GONE);
            holder.textForensics.setVisibility(View.GONE);
        }

        // Block button
        if (alert.isBlocked()) {
            holder.btnBlockEndpoint.setText("Already Blocked");
            holder.btnBlockEndpoint.setEnabled(false);
        } else {
            holder.btnBlockEndpoint.setText("Block IP");
            holder.btnBlockEndpoint.setEnabled(true);
            holder.btnBlockEndpoint.setOnClickListener(v -> {
                if (blockListener != null) {
                    blockListener.onBlockClicked(alert);
                    alert.setBlocked(true);
                    notifyItemChanged(position);
                }
            });
        }

        // Toggle expand/collapse on card click
        holder.itemView.setOnClickListener(v -> {
            boolean isVisible = holder.layoutExpandedDetails.getVisibility() == View.VISIBLE;
            holder.layoutExpandedDetails.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        });
    }

    @Override
    public int getItemCount() {
        return alerts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textSeverityBadge, textMitreTechnique, textAnomalyScore;
        TextView textThreatTitle, textEndpoints, textAppName, textTimestamp, textBlockedBadge;
        TextView textAiDiagnosis, textRecommendedActions, textForensicsHeader, textForensics;
        View layoutExpandedDetails;
        MaterialButton btnBlockEndpoint;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textSeverityBadge = itemView.findViewById(R.id.textSeverityBadge);
            textMitreTechnique = itemView.findViewById(R.id.textMitreTechnique);
            textAnomalyScore = itemView.findViewById(R.id.textAnomalyScore);
            textThreatTitle = itemView.findViewById(R.id.textThreatTitle);
            textEndpoints = itemView.findViewById(R.id.textEndpoints);
            textAppName = itemView.findViewById(R.id.textAppName);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
            textBlockedBadge = itemView.findViewById(R.id.textBlockedBadge);
            textAiDiagnosis = itemView.findViewById(R.id.textAiDiagnosis);
            textRecommendedActions = itemView.findViewById(R.id.textRecommendedActions);
            textForensicsHeader = itemView.findViewById(R.id.textForensicsHeader);
            textForensics = itemView.findViewById(R.id.textForensics);
            layoutExpandedDetails = itemView.findViewById(R.id.layoutExpandedDetails);
            btnBlockEndpoint = itemView.findViewById(R.id.btnBlockEndpoint);
        }
    }
}
