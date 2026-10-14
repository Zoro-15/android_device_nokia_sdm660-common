package org.lineageos.settings.device;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

public class BatteryCareReceiver extends BroadcastReceiver {
    private static final String TAG = "BatteryCareReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            Log.i(TAG, "Boot completed: Initializing BatteryCareService");
            BatteryCareService.start(context);
            // Re-apply bypass if user had it enabled before reboot
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (prefs.getBoolean(ThermalChargingService.PREF_BYPASS_CHARGING, false)) {
                ThermalChargingService.setBypassMode(context, true);
            }
        } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
            Log.i(TAG, "Power disconnected: Resetting bypass charging state to normal");
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putBoolean(ThermalChargingService.PREF_BYPASS_CHARGING, false)
                    .apply();
            ThermalChargingService.setBypassMode(context, false);
        } else if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
            Log.i(TAG, "Power connected: Ensuring BatteryCareService is active");
            BatteryCareService.start(context);
        }
    }
}
