package com.example.edgesecai.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.edgesecai.model.NetworkFlow;
import com.example.edgesecai.model.ThreatAlert;
import com.example.edgesecai.model.ThreatSeverity;
import com.example.edgesecai.model.ThreatType;

import java.util.ArrayList;
import java.util.List;

public class SecurityDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "edgesec_threats.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_ALERTS = "alerts";
    private static final String COL_ALERT_ID = "id";
    private static final String COL_TIMESTAMP = "timestamp";
    private static final String COL_THREAT_TYPE = "threat_type";
    private static final String COL_SEVERITY = "severity";
    private static final String COL_SOURCE = "source";
    private static final String COL_DESTINATION = "destination";
    private static final String COL_APP_NAME = "app_name";
    private static final String COL_ANOMALY_SCORE = "anomaly_score";
    private static final String COL_MITRE = "mitre_technique";
    private static final String COL_DESCRIPTION = "description";
    private static final String COL_DIAGNOSIS = "ai_diagnosis";
    private static final String COL_ACTIONS = "recommended_actions";
    private static final String COL_IS_BLOCKED = "is_blocked";
    private static final String COL_SAMPLE = "packet_sample";

    private static final String TABLE_FLOWS = "flows";
    private static final String COL_FLOW_ID = "flow_id";
    private static final String COL_SRC_IP = "src_ip";
    private static final String COL_SRC_PORT = "src_port";
    private static final String COL_DST_IP = "dst_ip";
    private static final String COL_DST_PORT = "dst_port";
    private static final String COL_PROTOCOL = "protocol";
    private static final String COL_START_TIME = "start_time";
    private static final String COL_LAST_TIME = "last_seen_time";
    private static final String COL_PKTS_OUT = "pkts_out";
    private static final String COL_PKTS_IN = "pkts_in";
    private static final String COL_BYTES_OUT = "bytes_out";
    private static final String COL_BYTES_IN = "bytes_in";
    private static final String COL_FLOW_ANOMALY = "flow_anomaly";
    private static final String COL_CLASSIFICATION = "classification";

    public SecurityDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createAlertsTable = "CREATE TABLE " + TABLE_ALERTS + " (" +
                COL_ALERT_ID + " TEXT PRIMARY KEY, " +
                COL_TIMESTAMP + " INTEGER, " +
                COL_THREAT_TYPE + " TEXT, " +
                COL_SEVERITY + " TEXT, " +
                COL_SOURCE + " TEXT, " +
                COL_DESTINATION + " TEXT, " +
                COL_APP_NAME + " TEXT, " +
                COL_ANOMALY_SCORE + " REAL, " +
                COL_MITRE + " TEXT, " +
                COL_DESCRIPTION + " TEXT, " +
                COL_DIAGNOSIS + " TEXT, " +
                COL_ACTIONS + " TEXT, " +
                COL_IS_BLOCKED + " INTEGER, " +
                COL_SAMPLE + " TEXT)";

        String createFlowsTable = "CREATE TABLE " + TABLE_FLOWS + " (" +
                COL_FLOW_ID + " TEXT PRIMARY KEY, " +
                COL_SRC_IP + " TEXT, " +
                COL_SRC_PORT + " INTEGER, " +
                COL_DST_IP + " TEXT, " +
                COL_DST_PORT + " INTEGER, " +
                COL_PROTOCOL + " TEXT, " +
                COL_START_TIME + " INTEGER, " +
                COL_LAST_TIME + " INTEGER, " +
                COL_PKTS_OUT + " INTEGER, " +
                COL_PKTS_IN + " INTEGER, " +
                COL_BYTES_OUT + " INTEGER, " +
                COL_BYTES_IN + " INTEGER, " +
                COL_FLOW_ANOMALY + " REAL, " +
                COL_CLASSIFICATION + " TEXT, " +
                COL_IS_BLOCKED + " INTEGER)";

        db.execSQL(createAlertsTable);
        db.execSQL(createFlowsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ALERTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FLOWS);
        onCreate(db);
    }

    public synchronized void insertAlert(ThreatAlert alert) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_ALERT_ID, alert.getId());
        cv.put(COL_TIMESTAMP, alert.getTimestamp());
        cv.put(COL_THREAT_TYPE, alert.getThreatType().name());
        cv.put(COL_SEVERITY, alert.getSeverity().name());
        cv.put(COL_SOURCE, alert.getSource());
        cv.put(COL_DESTINATION, alert.getDestination());
        cv.put(COL_APP_NAME, alert.getAppName());
        cv.put(COL_ANOMALY_SCORE, alert.getAnomalyScore());
        cv.put(COL_MITRE, alert.getMitreTechnique());
        cv.put(COL_DESCRIPTION, alert.getDescription());
        cv.put(COL_DIAGNOSIS, alert.getAiDiagnosis());
        cv.put(COL_ACTIONS, alert.getRecommendedActions());
        cv.put(COL_IS_BLOCKED, alert.isBlocked() ? 1 : 0);
        cv.put(COL_SAMPLE, alert.getPacketSample());

        db.insertWithOnConflict(TABLE_ALERTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public synchronized List<ThreatAlert> getAllAlerts() {
        List<ThreatAlert> alerts = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_ALERTS, null, null, null, null, null, COL_TIMESTAMP + " DESC");

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String id = cursor.getString(cursor.getColumnIndexOrThrow(COL_ALERT_ID));
                    long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP));
                    String typeStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_THREAT_TYPE));
                    String sevStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_SEVERITY));
                    String source = cursor.getString(cursor.getColumnIndexOrThrow(COL_SOURCE));
                    String dest = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESTINATION));
                    String app = cursor.getString(cursor.getColumnIndexOrThrow(COL_APP_NAME));
                    double score = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ANOMALY_SCORE));
                    String mitre = cursor.getString(cursor.getColumnIndexOrThrow(COL_MITRE));
                    String desc = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION));
                    String diag = cursor.getString(cursor.getColumnIndexOrThrow(COL_DIAGNOSIS));
                    String actions = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACTIONS));
                    boolean blocked = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_BLOCKED)) == 1;
                    String sample = cursor.getString(cursor.getColumnIndexOrThrow(COL_SAMPLE));

                    ThreatType threatType;
                    try {
                        threatType = ThreatType.valueOf(typeStr);
                    } catch (Exception e) {
                        threatType = ThreatType.ANOMALOUS_TRAFFIC_VOLUME;
                    }

                    ThreatSeverity severity;
                    try {
                        severity = ThreatSeverity.valueOf(sevStr);
                    } catch (Exception e) {
                        severity = ThreatSeverity.MEDIUM;
                    }

                    alerts.add(new ThreatAlert(
                            id, timestamp, threatType, severity, source, dest, app,
                            score, mitre, desc, diag, actions, blocked, sample
                    ));
                }
            } finally {
                cursor.close();
            }
        }
        return alerts;
    }

    public synchronized void insertOrUpdateFlow(NetworkFlow flow) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_FLOW_ID, flow.getFlowId());
        cv.put(COL_SRC_IP, flow.getSrcIp());
        cv.put(COL_SRC_PORT, flow.getSrcPort());
        cv.put(COL_DST_IP, flow.getDstIp());
        cv.put(COL_DST_PORT, flow.getDstPort());
        cv.put(COL_PROTOCOL, flow.getProtocol());
        cv.put(COL_START_TIME, flow.getStartTime());
        cv.put(COL_LAST_TIME, flow.getLastSeenTime());
        cv.put(COL_PKTS_OUT, flow.getPacketsOut());
        cv.put(COL_PKTS_IN, flow.getPacketsIn());
        cv.put(COL_BYTES_OUT, flow.getBytesOut());
        cv.put(COL_BYTES_IN, flow.getBytesIn());
        cv.put(COL_FLOW_ANOMALY, flow.getAnomalyScore());
        cv.put(COL_CLASSIFICATION, flow.getClassification());
        cv.put(COL_IS_BLOCKED, flow.isBlocked() ? 1 : 0);

        db.insertWithOnConflict(TABLE_FLOWS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public synchronized void clearAlerts() {
        getWritableDatabase().delete(TABLE_ALERTS, null, null);
    }

    public synchronized void clearFlows() {
        getWritableDatabase().delete(TABLE_FLOWS, null, null);
    }

    public synchronized void clearAllData() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_ALERTS, null, null);
        db.delete(TABLE_FLOWS, null, null);
    }
}
