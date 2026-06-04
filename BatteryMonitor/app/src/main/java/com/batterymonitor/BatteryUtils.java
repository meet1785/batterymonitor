package com.batterymonitor;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;

public class BatteryUtils {

    private static final String PREFS_NAME = "battery_prefs";
    private static final String KEY_ESTIMATED_FULL = "estimated_full_uah";
    private static float sEstimatedFull = -1f;
    private static float sLastSysPct = -1f;

    public static float getPreciseBatteryLevel(Context context, Intent intent) {
        if (intent == null) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            intent = context.registerReceiver(null, filter);
        }
        if (intent == null) return 0f;

        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float sysPct = (level >= 0 && scale > 0) ? ((float) level / scale * 100f) : -1f;

        BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        if (bm != null) {
            float counter = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
            
            if (counter > 0) {
                if (sEstimatedFull < 0) {
                    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    sEstimatedFull = prefs.getFloat(KEY_ESTIMATED_FULL, -1f);
                }

                // Recalibrate when system percentage changes to stay perfectly aligned
                if (sysPct > 0 && sysPct != sLastSysPct) {
                    sLastSysPct = sysPct;
                    sEstimatedFull = counter / (sysPct / 100f);
                    saveEstimatedFull(context, sEstimatedFull);
                }

                if (sEstimatedFull > 0) {
                    float precise = (counter / sEstimatedFull) * 100f;
                    // Ensure it doesn't exceed the current system percentage integer range
                    // e.g. if sysPct is 80, precise should be [80.0, 81.0)
                    // Actually, Android's level is usually floor(actual), so 80 means 80.x
                    if (precise < sysPct) precise = sysPct;
                    if (precise >= sysPct + 1.0f) precise = sysPct + 0.99f;
                    
                    return Math.min(Math.max(precise, 0f), 100f);
                }
            }
        }

        return sysPct > 0 ? sysPct : 0f;
    }

    private static void saveEstimatedFull(Context context, float value) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putFloat(KEY_ESTIMATED_FULL, value).apply();
    }

    public static String getBatteryStatusText(Intent intent) {
        if (intent == null) return "Unknown";
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                return "⚡ Charging";
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                return "🔋 Discharging";
            case BatteryManager.BATTERY_STATUS_FULL:
                return "✅ Full";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                return "🔌 Not Charging";
            default:
                return "❓ Unknown";
        }
    }

    public static String getBatteryHealthText(Intent intent) {
        if (intent == null) return "Unknown";
        int health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD:
                return "Good";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                return "Overheat";
            case BatteryManager.BATTERY_HEALTH_DEAD:
                return "Dead";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                return "Over Voltage";
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                return "Failure";
            case BatteryManager.BATTERY_HEALTH_COLD:
                return "Cold";
            default:
                return "Unknown";
        }
    }

    public static float getBatteryTemperature(Intent intent) {
        if (intent == null) return 0f;
        return intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f;
    }

    public static float getBatteryVoltage(Intent intent) {
        if (intent == null) return 0f;
        int volt = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
        return volt > 1000 ? volt / 1000f : volt;
    }

    public static int getBatteryCurrentNow(Context context) {
        BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        if (bm != null) {
            // BATTERY_PROPERTY_CURRENT_NOW is in microamperes (uA), convert to mA
            int current = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
            // On some devices current is negative when discharging, on others it's positive.
            // We usually want to show the absolute magnitude with a sign in the UI.
            return current / 1000;
        }
        return 0;
    }

    public static String getRemainingTime(Context context, Intent intent, float preciseLevel) {
        BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        if (bm == null || intent == null) return "Estimating...";

        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING;
        int currentMa = Math.abs(getBatteryCurrentNow(context));

        if (currentMa <= 50) return "Calculating..."; // Low current makes estimation unreliable

        if (charging) {
            if (preciseLevel >= 99.9f) return "Full";
            float remainingPct = 100f - preciseLevel;
            if (sEstimatedFull > 0) {
                float remainingUah = (remainingPct / 100f) * sEstimatedFull;
                float hours = (remainingUah / 1000f) / currentMa;
                return formatHours(hours) + " until full";
            }
        } else {
            if (preciseLevel <= 0.1f) return "Empty";
            float remainingPct = preciseLevel;
            if (sEstimatedFull > 0) {
                float remainingUah = (remainingPct / 100f) * sEstimatedFull;
                float hours = (remainingUah / 1000f) / currentMa;
                return formatHours(hours) + " remaining";
            }
        }
        return "Calculating...";
    }

    private static String formatHours(float hours) {
        int h = (int) hours;
        int m = (int) ((hours - h) * 60);
        if (h > 0) {
            return h + "h " + m + "m";
        } else {
            return m + "m";
        }
    }
}
