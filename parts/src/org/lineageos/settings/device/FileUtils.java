package org.lineageos.settings.device;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

public final class FileUtils {
    private static final String TAG = "DeviceParts-FileUtils";

    public static final String BATT_CHARGING_ENABLED_NODE = "/sys/class/power_supply/battery/battery_charging_enabled";
    public static final String CHARGING_ENABLED_NODE = "/sys/class/power_supply/battery/charging_enabled";
    public static final String BATT_FCC_NODE = "/sys/class/power_supply/battery/constant_charge_current_max";
    public static final String MAIN_FCC_NODE = "/sys/class/power_supply/main/constant_charge_current_max";

    private FileUtils() {}

    public static boolean fileExists(String path) {
        return new File(path).exists();
    }

    public static boolean writeLine(String path, String value) {
        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(value.getBytes());
            fos.flush();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to write to " + path + ": " + e.getMessage());
            return false;
        }
    }

    public static boolean writeLine(String path, int value) {
        return writeLine(path, String.valueOf(value));
    }

    public static String readOneLine(String path) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path)))) {
            return br.readLine();
        } catch (Exception e) {
            Log.e(TAG, "Failed to read from " + path + ": " + e.getMessage());
            return null;
        }
    }

    public static int readInt(String path, int defaultValue) {
        String line = readOneLine(path);
        if (line != null) {
            try {
                return Integer.parseInt(line.trim());
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    private static final String[] CHARGING_CONTROL_NODES = {
        BATT_CHARGING_ENABLED_NODE,
        CHARGING_ENABLED_NODE,
    };

    public static String getChargingControlNode() {
        for (String node : CHARGING_CONTROL_NODES) {
            if (fileExists(node)) {
                return node;
            }
        }
        return BATT_CHARGING_ENABLED_NODE;
    }

    /**
     * Controls true hardware bypass mode on Qualcomm PM660:
     * - Keeps the buck switching converter ACTIVE (1) so USB power supplies VPH_PWR (motherboard/CPU/display).
     * - Clamps Fast Charge Current (FCC) to 0uA so 0mA flows into the battery cell.
     * - If load spikes above charger capacity, PMIC BATFET automatically supplements from battery.
     */
    public static boolean setBypassMode(Context context, boolean bypass) {
        // Step 1: Ensure buck switching converter stays enabled (powers motherboard from charger)
        String chgNode = getChargingControlNode();
        if (fileExists(chgNode)) {
            writeLine(chgNode, "1");
        }

        if (bypass) {
            // Step 2: In bypass mode, clamp Fast Charge Current (FCC) to 0
            if (fileExists(BATT_FCC_NODE)) {
                writeLine(BATT_FCC_NODE, 0);
            }
            if (fileExists(MAIN_FCC_NODE)) {
                writeLine(MAIN_FCC_NODE, 0);
            }
            Log.i(TAG, "Bypass charging ACTIVE: Buck switcher ON (VPH_PWR), FCC clamped to 0uA");
            return true;
        } else {
            // Step 3: Resuming normal charging - re-evaluate and apply user's charging profile
            Log.i(TAG, "Bypass charging INACTIVE: Resuming user charging profile");
            if (context != null) {
                ThermalChargingService.evaluate(context);
            } else {
                if (fileExists(BATT_FCC_NODE)) writeLine(BATT_FCC_NODE, 2000000);
                if (fileExists(MAIN_FCC_NODE)) writeLine(MAIN_FCC_NODE, 2000000);
            }
            return true;
        }
    }

    public static boolean isBypassActive(Context context) {
        if (context != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (prefs.getBoolean("bypass_charging_enabled", false)) {
                return true;
            }
        }
        int fcc = readInt(BATT_FCC_NODE, -1);
        return fcc == 0;
    }

    public static boolean setChargingEnabled(Context context, boolean enable) {
        return setBypassMode(context, !enable);
    }

    public static boolean setChargingEnabled(boolean enable) {
        return setBypassMode(null, !enable);
    }

    public static boolean isChargingEnabled() {
        int fcc = readInt(BATT_FCC_NODE, -1);
        if (fcc == 0) return false;
        String val = readOneLine(getChargingControlNode());
        if (val == null) return true;
        return !"0".equals(val.trim());
    }
}
