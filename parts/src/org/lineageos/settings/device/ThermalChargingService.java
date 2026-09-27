package org.lineageos.settings.device;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

public class ThermalChargingService {
    private static final String TAG = "ThermalChargingService";
    public static final String CURRENT_NODE = "/sys/class/power_supply/battery/constant_charge_current_max";
    public static final String TEMP_NODE = "/sys/class/power_supply/battery/temp";

    public static final String PREF_BLAZE_ENABLED = "blaze_thermal_engine_enabled";
    public static final String PREF_CHARGING_PROFILE = "charging_current_profile";
    public static final String PREF_THERMAL_THRESHOLD = "thermal_threshold_celsius";

    public static void evaluate(Context context) {
        if (!FileUtils.fileExists(CURRENT_NODE)) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isBlazeEnabled = prefs.getBoolean(PREF_BLAZE_ENABLED, true);

        int userCurrent = 2000000;
        try {
            userCurrent = Integer.parseInt(prefs.getString(PREF_CHARGING_PROFILE, "2000000"));
        } catch (Exception e) {
            userCurrent = 2000000;
        }

        // Rule 5: If Master Toggle is OFF, strictly enforce user profile without dynamic stepping
        if (!isBlazeEnabled) {
            FileUtils.writeLine(CURRENT_NODE, userCurrent);
            Log.d(TAG, "Blaze engine OFF: Statically enforcing charging current profile " + userCurrent + " uA");
            return;
        }

        // Master Toggle is ON: Apply dynamic thermal regulation
        // Battery temp in sysfs is reported in tenths of a degree Celsius (e.g. 420 = 42.0 C)
        int maxTemp = prefs.getInt(PREF_THERMAL_THRESHOLD, 42) * 10;
        int currentTemp = FileUtils.readInt(TEMP_NODE, 250);

        if (currentTemp >= maxTemp) {
            // Step down to 1A cooldown (or maintain user trickle if already below 1000mA)
            int cooldownCurrent = Math.min(userCurrent, 1000000);
            FileUtils.writeLine(CURRENT_NODE, cooldownCurrent);
            Log.w(TAG, "Battery temp (" + (currentTemp / 10.0f) + "C) >= cutoff (" + (maxTemp / 10.0f) + "C). Cooldown throttled to " + cooldownCurrent + " uA.");
        } else {
            // Battery temp is within safe operating range: Apply target profile current
            FileUtils.writeLine(CURRENT_NODE, userCurrent);
            Log.d(TAG, "Battery temp safe (" + (currentTemp / 10.0f) + "C). Applying profile current " + userCurrent + " uA.");
        }
    }
}
