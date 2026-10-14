package org.lineageos.settings.device;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.IBinder;
import android.util.Log;

import androidx.preference.PreferenceManager;

public class BatteryCareService extends Service {
    private static final String TAG = "BatteryCareService";
    public static final String PREF_BATTERY_STOP_LEVEL = "battery_stop_level";
    public static final String PREF_BYPASS_CHARGING = "bypass_charging_enabled";

    private final BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) return;

            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            int percent = (level * 100) / (scale == 0 ? 100 : scale);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean isBypassActive = prefs.getBoolean(PREF_BYPASS_CHARGING, false);

            // If manual bypass charging is active, hold bypass 0mA
            if (isBypassActive) {
                FileUtils.setBypassMode(context, true);
                return;
            }

            int stopThreshold = 80;
            try {
                stopThreshold = Integer.parseInt(prefs.getString(PREF_BATTERY_STOP_LEVEL, "80"));
            } catch (Exception e) {
                stopThreshold = 80;
            }

            // 0 means disabled / off (unlimited charging to 100%)
            if (stopThreshold > 0) {
                if (percent >= stopThreshold) {
                    // Reached stop threshold: hold at limit with AC bypass (0mA) without micro-cycles
                    FileUtils.setBypassMode(context, true);
                    Log.d(TAG, "Battery level " + percent + "% >= threshold " + stopThreshold + "%, holding at bypass 0mA.");
                    return;
                } else if (percent <= (stopThreshold - 2)) {
                    // 2% hysteresis: resume charging
                    FileUtils.setBypassMode(context, false);
                }
            } else {
                // Unlimited charging
                FileUtils.setBypassMode(context, false);
            }

            // Evaluate Blaze Dynamic Thermal Charging Engine
            ThermalChargingService.evaluate(context);
        }
    };

    public static void start(Context context) {
        Intent intent = new Intent(context, BatteryCareService.class);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mBatteryReceiver, filter);
        Log.i(TAG, "BatteryCareService started and ACTION_BATTERY_CHANGED receiver registered.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(mBatteryReceiver);
        } catch (Exception ignored) {}
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
