package com.batterymonitor;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private TextView tvBattery;
    private BroadcastReceiver batteryReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvBattery = findViewById(R.id.tvBattery);
        Button btnStart = findViewById(R.id.btnStart);
        Button btnStop = findViewById(R.id.btnStop);

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        // OnePlus Nord / Android 14 App Battery Optimizations Exception
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent optIntent = new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                optIntent.setData(android.net.Uri.parse("package:" + getPackageName()));
                startActivity(optIntent);
            }
        }

        // Start service button
        btnStart.setOnClickListener(v -> {
            Intent intent = new Intent(this, BatteryService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        });

        // Stop service button
        btnStop.setOnClickListener(v -> {
            Intent intent = new Intent(this, BatteryService.class);
            stopService(intent);
        });

        // Live battery reading in UI
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                boolean charging = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                        == BatteryManager.BATTERY_STATUS_CHARGING
                        || intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                        == BatteryManager.BATTERY_STATUS_FULL;

                if (level >= 0 && scale > 0) {
                    // Use BatteryManager for more precise reading
                    BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
                    int intProperty = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                    // Combine both for 2 decimal places approximation
                    float precise = (level * 100.0f / scale);
                    String pct = String.format("%.2f%%", precise);
                    String state = charging ? "⚡ Charging" : "🔋 Discharging";
                    tvBattery.setText(pct + "\n" + state);
                }
            }
        };

        // Auto-start service on open
        Intent serviceIntent = new Intent(this, BatteryService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(batteryReceiver);
    }
}
