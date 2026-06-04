package com.batterymonitor;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
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
    private ProgressBar pbBattery;
    private TextView tvStatus;
    private TextView tvTime;
    private TextView tvCurrent;
    private TextView tvHealth;
    private TextView tvVoltage;
    private TextView tvTemp;
    private BroadcastReceiver batteryReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvBattery = findViewById(R.id.tvBattery);
        pbBattery = findViewById(R.id.pbBattery);
        tvStatus = findViewById(R.id.tvStatus);
        tvTime = findViewById(R.id.tvTime);
        tvCurrent = findViewById(R.id.tvCurrent);
        tvHealth = findViewById(R.id.tvHealth);
        tvVoltage = findViewById(R.id.tvVoltage);
        tvTemp = findViewById(R.id.tvTemp);
        
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
                try {
                    Intent optIntent = new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    optIntent.setData(android.net.Uri.parse("package:" + getPackageName()));
                    startActivity(optIntent);
                } catch (Exception ignored) {}
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
                updateUI(intent);
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

    private void updateUI(Intent intent) {
        float precise = BatteryUtils.getPreciseBatteryLevel(this, intent);
        String pct = String.format("%.2f%%", precise);
        tvBattery.setText(pct);
        pbBattery.setProgress((int) (precise * 100));

        tvStatus.setText(BatteryUtils.getBatteryStatusText(intent));
        tvTime.setText(BatteryUtils.getRemainingTime(this, intent, precise));
        
        int currentMa = BatteryUtils.getBatteryCurrentNow(this);
        tvCurrent.setText(currentMa + "mA");
        // Color current green if charging, white if discharging
        int status = intent != null ? intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) : -1;
        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            tvCurrent.setTextColor(0xFF78F8B3);
        } else {
            tvCurrent.setTextColor(0xFFF3F7FA);
        }

        tvHealth.setText(BatteryUtils.getBatteryHealthText(intent));
        tvVoltage.setText(String.format("%.2fV", BatteryUtils.getBatteryVoltage(intent)));
        tvTemp.setText(String.format("%.1f°C", BatteryUtils.getBatteryTemperature(intent)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent initial = registerReceiver(batteryReceiver, filter);
        if (initial != null) {
            updateUI(initial);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(batteryReceiver);
    }
}
