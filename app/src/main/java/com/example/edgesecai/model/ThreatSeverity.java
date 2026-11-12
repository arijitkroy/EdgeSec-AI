package com.example.edgesecai.model;

public enum ThreatSeverity {
    CRITICAL("#D32F2F", "Critical"),
    HIGH("#F57C00", "High"),
    MEDIUM("#FBC02D", "Medium"),
    LOW("#388E3C", "Low"),
    INFO("#1976D2", "Info");

    private final String colorHex;
    private final String label;

    ThreatSeverity(String colorHex, String label) {
        this.colorHex = colorHex;
        this.label = label;
    }

    public String getColorHex() {
        return colorHex;
    }

    public String getLabel() {
        return label;
    }
}
