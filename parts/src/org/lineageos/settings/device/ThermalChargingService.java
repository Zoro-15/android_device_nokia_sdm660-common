package org.lineageos.settings.device;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

public class ThermalChargingService {
    private static final String TAG = "ThermalChargingService";

    // Battery profile fast charge current (votes BATT_PROFILE_VOTER on fcc_votable)
    public static final String BATT_FCC_NODE = "/sys/class/power_supply/battery/constant_charge_current_max";
    // Input current limit node (sets smblib_set_icl_current AND clears THERMAL_DAEMON_VOTER)
    public static final String BATT_ICL_NODE = "/sys/class/power_supply/battery/input_current_max";
    // Direct hardware FCC node in PM660
    public static final String MAIN_FCC_NODE = "/sys/class/power_supply/main/constant_charge_current_max";
    // Direct hardware ICL node in PM660
    public static final String MAIN_ICL_NODE = "/sys/class/power_supply/main/current_max";
    // Step charging enable node (controls step-chg-jeita clamp)
    public static final String STEP_CHG_NODE = "/sys/class/power_supply/battery/step_charging_enabled";
    // Battery temperature node (in tenths of a degree Celsius, e.g. 420 = 42.0 C)
    public static final String TEMP_NODE = "/sys/class/power_supply/battery/temp";

    public static final String PREF_BLAZE_ENABLED = "blaze_thermal_engine_enabled";
    public static final String PREF_CHARGING_PROFILE = "charging_current_profile";
    public static final String PREF_THERMAL_THRESHOLD = "thermal_threshold_celsius";

    public static void evaluate(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isBlazeEnabled = prefs.getBoolean(PREF_BLAZE_ENABLED, true);

        int userCurrent = 2000000;
        try {
            userCurrent = Integer.parseInt(prefs.getString(PREF_CHARGING_PROFILE, "2000000"));
        } catch (Exception e) {
            userCurrent = 2000000;
        }

        // If Master Toggle is OFF, strictly enforce user profile without dynamic stepping
        if (!isBlazeEnabled) {
            applyChargingCurrent(userCurrent);
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
            applyChargingCurrent(cooldownCurrent);
            Log.w(TAG, "Battery temp (" + (currentTemp / 10.0f) + "C) >= cutoff (" + (maxTemp / 10.0f) + "C). Cooldown throttled to " + cooldownCurrent + " uA.");
        } else {
            // Battery temp is within safe operating range: Apply target profile current
            applyChargingCurrent(userCurrent);
            Log.d(TAG, "Battery temp safe (" + (currentTemp / 10.0f) + "C). Applying profile current " + userCurrent + " uA.");
        }
    }

    private static void applyChargingCurrent(int currentUa) {
        // 1. Set battery profile fast charge current
        if (FileUtils.fileExists(BATT_FCC_NODE)) {
            FileUtils.writeLine(BATT_FCC_NODE, currentUa);
        }

        // 2. Set charger input current limit (also clears THERMAL_DAEMON_VOTER in kernel)
        if (FileUtils.fileExists(BATT_ICL_NODE)) {
            FileUtils.writeLine(BATT_ICL_NODE, currentUa);
        }

        // 3. Set main charger direct hardware FCC
        if (FileUtils.fileExists(MAIN_FCC_NODE)) {
            FileUtils.writeLine(MAIN_FCC_NODE, currentUa);
        }

        // 4. Set main charger direct hardware ICL
        if (FileUtils.fileExists(MAIN_ICL_NODE)) {
            FileUtils.writeLine(MAIN_ICL_NODE, currentUa);
        }

        // 5. If Turbo (>= 3000mA), disable step-charging voltage clamp so current isn't throttled to 1.5A
        // If lower profile or cooldown, keep step-charging active
        if (FileUtils.fileExists(STEP_CHG_NODE)) {
            FileUtils.writeLine(STEP_CHG_NODE, currentUa >= 3000000 ? 0 : 1);
        }
    }
}
