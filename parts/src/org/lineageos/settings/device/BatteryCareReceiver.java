package org.lineageos.settings.device;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BatteryCareReceiver extends BroadcastReceiver {
    private static final String TAG = "BatteryCareReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            Log.i(TAG, "Boot completed: Initializing BatteryCareService");
            BatteryCareService.start(context);
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
        }
    }
}
