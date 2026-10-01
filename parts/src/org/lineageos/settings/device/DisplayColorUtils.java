package org.lineageos.settings.device;

import android.content.Context;
import android.hardware.display.ColorDisplayManager;
import android.provider.Settings;
import android.util.Log;
import java.lang.reflect.Method;

public final class DisplayColorUtils {
    private static final String TAG = "DisplayColorUtils";

    public static final String KEY_COLOR_PRESET = "color_preset_mode";
    public static final String KEY_COLOR_TEMP = "color_temperature_kelvin";
    public static final String KEY_COLOR_RED = "color_channel_red";
    public static final String KEY_COLOR_GREEN = "color_channel_green";
    public static final String KEY_COLOR_BLUE = "color_channel_blue";

    private DisplayColorUtils() {}

    /**
     * Set Color Temperature (Kelvin, typically 2500K - 7500K, default 6500K)
     */
    public static void setColorTemperature(Context context, int kelvin) {
        try {
            ColorDisplayManager cdm = context.getSystemService(ColorDisplayManager.class);
            if (cdm != null) {
                cdm.setNightDisplayActivated(true);
                Method setTemp = ColorDisplayManager.class.getMethod("setNightDisplayColorTemperature", int.class);
                setTemp.invoke(cdm, kelvin);
                return;
            }
        } catch (Throwable t) {
            Log.d(TAG, "ColorDisplayManager reflection fallback: " + t.getMessage());
        }

        try {
            Settings.Secure.putInt(context.getContentResolver(), "night_display_activated", 1);
            Settings.Secure.putInt(context.getContentResolver(), "night_display_color_temperature", kelvin);
        } catch (Throwable t) {
            Log.e(TAG, "Failed to set color temperature via Settings.Secure", t);
        }
    }

    /**
     * Set Display Color Mode (0: Natural, 1: Boosted, 2: Saturated, 3: Automatic)
     */
    public static void setColorMode(Context context, int mode) {
        try {
            ColorDisplayManager cdm = context.getSystemService(ColorDisplayManager.class);
            if (cdm != null) {
                cdm.setColorMode(mode);
                return;
            }
        } catch (Throwable t) {
            Log.d(TAG, "ColorDisplayManager setColorMode fallback: " + t.getMessage());
        }

        try {
            Settings.System.putInt(context.getContentResolver(), "display_color_mode", mode);
        } catch (Throwable t) {
            Log.e(TAG, "Failed to set display_color_mode", t);
        }
    }

    /**
     * Apply RGB channel color balance (LineageOS LiveDisplay or ColorDisplayManager fallback)
     */
    public static void applyColorCalibration(Context context, int r, int g, int b) {
        // Try LineageOS LiveDisplayManager first
        try {
            Class<?> ldClass = Class.forName("lineageos.hardware.LiveDisplayManager");
            Method getInst = ldClass.getMethod("getInstance", Context.class);
            Object ldObj = getInst.invoke(null, context);
            if (ldObj != null) {
                float[] rgbFloat = new float[]{r / 256.0f, g / 256.0f, b / 256.0f};
                Method setColorBalance = ldClass.getMethod("setColorBalance", float[].class);
                setColorBalance.invoke(ldObj, (Object) rgbFloat);
                return;
            }
        } catch (Throwable ignored) {
        }

        // Native AOSP fallback via ColorDisplayManager temperature matrix
        // Warmer when Red dominates, cooler/daylight when Blue dominates
        int kelvin = 6500;
        if (r > b) {
            float ratio = (float) b / (float) Math.max(r, 1);
            kelvin = Math.round(2700 + ratio * 3800);
        } else if (b > r) {
            float ratio = (float) r / (float) Math.max(b, 1);
            kelvin = Math.round(6500 + (1.0f - ratio) * 1500);
        }
        setColorTemperature(context, kelvin);
    }
}
