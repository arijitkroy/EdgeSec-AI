package com.example.edgesecai.engine;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.edgesecai.MainActivity;
import com.example.edgesecai.R;
import com.example.edgesecai.storage.SecurityRepository;

import hev.htproxy.TProxyService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class VpnInspectorService extends VpnService {
    private static final String TAG = "VpnInspectorService";
    private static final String CHANNEL_ID = "edgesec_ai_shield_channel";
    private static final int NOTIFICATION_ID = 1001;

    public static final String ACTION_START = "com.example.edgesecai.START_VPN";
    public static final String ACTION_STOP = "com.example.edgesecai.STOP_VPN";

    private ParcelFileDescriptor vpnInterface;
    private DirectSocks5Server directProxy;
    private volatile boolean isRunning;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopVpn();
            stopSelf();
            return START_NOT_STICKY;
        }
        startVpn();
        return START_STICKY;
    }

    private synchronized void startVpn() {
        if (isRunning) return;

        createNotificationChannel();
        Notification notification = buildForegroundNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        try {
            SecurityRepository repository = SecurityRepository.getInstance(this);
            directProxy = new DirectSocks5Server(this, repository);
            directProxy.start();
            Log.i(TAG, "Local SOCKS5 endpoint listening on 127.0.0.1:" + DirectSocks5Server.PORT);

            Builder builder = new Builder();
            builder.setMtu(1500);
            builder.addAddress("10.1.1.2", 32);
            builder.addAddress("fd00:ed:5ec::2", 128);
            builder.addRoute("0.0.0.0", 0);
            builder.addRoute("::", 0);
            builder.setSession("EdgeSecAI Local Traffic Inspector");
            builder.addDisallowedApplication(getPackageName());

            vpnInterface = builder.establish();
            if (vpnInterface == null) {
                throw new IOException("Android did not establish the VPN interface");
            }

            writeTunnelConfig();
            if (!TProxyService.TProxyStartService(
                    new File(getFilesDir(), "hev-tunnel.yml").getAbsolutePath(),
                    vpnInterface.getFd())) {
                throw new IOException("The local TUN forwarding engine did not start");
            }

            isRunning = true;
            repository.setProtectionActive(true);
            Log.i(TAG, "Local VPN forwarding and inspection started.");
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (isRunning && !TProxyService.TProxyIsRunning()) {
                    Log.e(TAG, "Native forwarder exited after startup; removing VPN routes.");
                    stopVpn();
                    stopSelf();
                }
            }, 2000L);
        } catch (Exception e) {
            Log.e(TAG, "Unable to start local VPN forwarding", e);
            stopVpn();
        }
    }

    private void writeTunnelConfig() throws IOException {
        String config = "tunnel:\n"
                + "  name: tun0\n"
                + "  mtu: 1500\n"
                + "  multi-queue: false\n"
                + "  ipv4: 10.1.1.2\n"
                + "  ipv6: 'fd00:ed:5ec::2'\n"
                + "  icmp: 'off'\n"
                + "socks5:\n"
                + "  address: 127.0.0.1\n"
                + "  port: " + DirectSocks5Server.PORT + "\n"
                + "  udp: 'udp'\n"
                + "misc:\n"
                + "  connect-timeout: 10000\n"
                + "  log-file: '" + new File(getFilesDir(), "hev-tunnel.log").getAbsolutePath() + "'\n"
                + "  log-level: debug\n";
        File configFile = new File(getFilesDir(), "hev-tunnel.yml");
        try (FileOutputStream output = new FileOutputStream(configFile)) {
            output.write(config.getBytes(StandardCharsets.UTF_8));
        }
    }

    private synchronized void stopVpn() {
        isRunning = false;
        SecurityRepository.getInstance(this).setProtectionActive(false);

        try {
            if (TProxyService.TProxyIsRunning()) {
                TProxyService.TProxyStopService();
            }
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "Tunnel native library was not loaded", e);
        }

        if (directProxy != null) {
            directProxy.close();
            directProxy = null;
        }
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing VPN interface", e);
            }
            vpnInterface = null;
        }
        stopForeground(true);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "EdgeSecAI Shield Protection",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("On-device traffic inspection and threat detection");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification buildForegroundNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("EdgeSecAI: Live inspection active")
                .setContentText("Traffic is forwarded locally and inspected on device")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public void onRevoke() {
        stopVpn();
        stopSelf();
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        stopVpn();
        super.onDestroy();
    }
}
