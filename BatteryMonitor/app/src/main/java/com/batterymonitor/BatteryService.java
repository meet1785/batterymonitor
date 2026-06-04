package com.batterymonitor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;

public class BatteryService extends Service {

    private static final String CHANNEL_ID = "battery_monitor_channel";
    private static final int NOTIF_ID = 101;
    private static final long UPDATE_INTERVAL_MS = 5_000; // 5 seconds for precise decimal tracking

    private Handler handler;
    private Runnable updateRunnable;
    private NotificationManager notifManager;

    @Override
    public void onCreate() {
        super.onCreate();
        notifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createNotificationChannel();
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Show initial notification immediately to satisfy foreground requirement
        startForeground(NOTIF_ID, buildNotification("Reading...", false, 0, null));

        // Schedule periodic updates
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateBatteryNotification();
                handler.postDelayed(this, UPDATE_INTERVAL_MS);
            }
        };
        handler.post(updateRunnable);

        // Also register receiver for instant updates on battery change
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);

        return START_STICKY; // Restart if killed
    }

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                updateNotification(intent);
            }
        }
    };

    private void updateBatteryNotification() {
        Intent batteryStatus = registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus != null) {
            updateNotification(batteryStatus);
        }
    }

    private void updateNotification(Intent intent) {
        float precise = BatteryUtils.getPreciseBatteryLevel(this, intent);
        String pctText = String.format("%.2f%%", precise);
        boolean charging = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING;
        
        notifManager.notify(NOTIF_ID, buildNotification(pctText, charging, precise, intent));
    }

    private Notification buildNotification(String pctText, boolean charging, float pct, Intent intent) {
        // Tap notification → open app
        Intent openApp = new Intent(this, MainActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String statusText = BatteryUtils.getBatteryStatusText(intent);
        int currentMa = BatteryUtils.getBatteryCurrentNow(this);
        String timeText = BatteryUtils.getRemainingTime(this, intent, pct);
        
        String subText = String.format("%s (%dmA) • %s", 
                statusText, currentMa, timeText);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_battery)
                .setContentTitle("Battery: " + pctText)
                .setContentText(subText)
                .setOngoing(true)             // Can't be swiped away
                .setSilent(true)              // No sound/vibration
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
                .setContentIntent(pendingIntent)
                .setProgress(100, (int) pct, false)  // Progress bar
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Battery Monitor",
                    NotificationManager.IMPORTANCE_LOW  // Silent, persistent
            );
            channel.setDescription("Shows live battery percentage with 2 decimal places");
            channel.setSound(null, null);
            channel.enableVibration(false);
            channel.setShowBadge(false);
            notifManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
        try { unregisterReceiver(batteryReceiver); } catch (Exception ignored) {}
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
