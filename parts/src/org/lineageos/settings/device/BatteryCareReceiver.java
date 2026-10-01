package org.lineageos.settings.device;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BatteryCareReceiver extends BroadcastReceiver {
    private static final String TAG = "BatteryCareReceiver";
    public static final String NODE_CHARGING_ENABLED =
            "/sys/class/power_supply/battery/battery_charging_enabled";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            Log.i(TAG, "Boot completed: Initializing BatteryCareService");
            BatteryCareService.start(context);
            String savedBand = NetworkModeUtils.getLteBandLock(context);
            if (savedBand != null && !"0".equals(savedBand)) {
                NetworkModeUtils.setLteBandLock(context, savedBand);
            }
        } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
            Log.i(TAG, "Power disconnected: Resetting charging enable state to normal");
            // Re-enable charging so next plug-in is clean
            FileUtils.setChargingEnabled(true);
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putBoolean(BatteryCareService.PREF_BYPASS_CHARGING, false)
                    .apply();
        } else if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
            Log.i(TAG, "Power connected: Ensuring BatteryCareService is active");
            BatteryCareService.start(context);
        } else if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
            int state = intent.getIntExtra("state", -1);
            boolean autoIem = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean(DeviceSettingsFragment.KEY_AUTO_IEM, true);
            if (autoIem) {
                if (state == 1) {
                    Log.i(TAG, "3.5mm Headset/IEM Connected: Applying Hi-Fi Low-Gain Audio Profile");
                    try {
                        android.os.SystemProperties.set("persist.vendor.audio.hifi", "true");
                    } catch (Exception ignored) {}
                } else if (state == 0) {
                    Log.i(TAG, "3.5mm Headset Disconnected: Restoring Standard Audio Profile");
                    try {
                        android.os.SystemProperties.set("persist.vendor.audio.hifi", "false");
                    } catch (Exception ignored) {}
                }
            }
        }
    }
}
