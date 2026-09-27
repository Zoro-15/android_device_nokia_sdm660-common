package org.lineageos.settings.device;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;

public class DeviceSettingsFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener {

    public static final String NODE_VIBRATOR = "/sys/class/leds/vibrator/vmax_mv";
    public static final String NODE_KCAL_RGB =
            "/sys/devices/platform/soc/c900000.qcom,mdss_mdp/drm/card0/sde-crtc-0/kcal_rgb";
    public static final String NODE_KCAL_SAT =
            "/sys/devices/platform/soc/c900000.qcom,mdss_mdp/drm/card0/sde-crtc-0/kcal_sat";
    public static final String NODE_GLOVE_MODE =
            "/sys/devices/platform/soc/c1b5000.i2c/i2c-5/5-0038/fts_glove_mode";
    public static final String NODE_GLOVE_MODE_ALT =
            "/proc/touchscreen/glove_mode";

    public static final String KEY_KCAL_PRESET = "kcal_preset";
    public static final String KEY_KCAL_RED = "kcal_red";
    public static final String KEY_KCAL_GREEN = "kcal_green";
    public static final String KEY_KCAL_BLUE = "kcal_blue";
    public static final String KEY_KCAL_SAT = "kcal_sat";
    public static final String KEY_VIBRATOR = "vibrator_intensity";
    public static final String KEY_GLOVE_MODE = "glove_mode_enabled";
    public static final String KEY_PERF_PROFILE = "perf_mode_profile";
    public static final String KEY_AUTO_IEM = "auto_iem_profile";
    public static final String KEY_HEADPHONE_IMPEDANCE = "headphone_impedance_status";

    public static final String NODE_HEADSET_STATE = "/sys/class/switch/h2w/state";
    public static final String NODE_IMPEDANCE_1 = "/sys/bus/i2c/drivers/wcd9335/impedance";
    public static final String NODE_IMPEDANCE_2 = "/sys/class/switch/h2w/impedance";
    public static final String NODE_IMPEDANCE_3 = "/sys/devices/platform/soc/soc:qcom,msm-audio-pinctrl/impedance";

    private ListPreference mPresetPref;
    private SeekBarPreference mRedPref;
    private SeekBarPreference mGreenPref;
    private SeekBarPreference mBluePref;
    private SeekBarPreference mSatPref;
    private SeekBarPreference mVibPref;
    private SwitchPreference mGlovePref;
    private ListPreference mPerfPref;
    private Preference mImpedancePref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.device_settings_preferences, rootKey);

        mPresetPref = findPreference(KEY_KCAL_PRESET);
        if (mPresetPref != null) mPresetPref.setOnPreferenceChangeListener(this);

        mRedPref = findPreference(KEY_KCAL_RED);
        if (mRedPref != null) mRedPref.setOnPreferenceChangeListener(this);

        mGreenPref = findPreference(KEY_KCAL_GREEN);
        if (mGreenPref != null) mGreenPref.setOnPreferenceChangeListener(this);

        mBluePref = findPreference(KEY_KCAL_BLUE);
        if (mBluePref != null) mBluePref.setOnPreferenceChangeListener(this);

        mSatPref = findPreference(KEY_KCAL_SAT);
        if (mSatPref != null) mSatPref.setOnPreferenceChangeListener(this);

        mVibPref = findPreference(KEY_VIBRATOR);
        if (mVibPref != null) mVibPref.setOnPreferenceChangeListener(this);

        mGlovePref = findPreference(KEY_GLOVE_MODE);
        if (mGlovePref != null) mGlovePref.setOnPreferenceChangeListener(this);

        mPerfPref = findPreference(KEY_PERF_PROFILE);
        if (mPerfPref != null) mPerfPref.setOnPreferenceChangeListener(this);

        mImpedancePref = findPreference(KEY_HEADPHONE_IMPEDANCE);
        updateImpedanceStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateImpedanceStatus();
    }

    private void updateImpedanceStatus() {
        if (mImpedancePref == null) return;

        int ohms = FileUtils.readInt(NODE_IMPEDANCE_1, -1);
        if (ohms <= 0) ohms = FileUtils.readInt(NODE_IMPEDANCE_2, -1);
        if (ohms <= 0) ohms = FileUtils.readInt(NODE_IMPEDANCE_3, -1);

        if (ohms > 0) {
            if (ohms < 32) {
                mImpedancePref.setSummary(ohms + " \u03a9 (Sensitive IEM Detected \u2022 Low Gain / Zero-Hiss Active)");
            } else if (ohms <= 64) {
                mImpedancePref.setSummary(ohms + " \u03a9 (Standard Headphone Load Detected \u2022 Optimal Gain)");
            } else {
                mImpedancePref.setSummary(ohms + " \u03a9 (High-Impedance Headphone Detected \u2022 Boosted Gain)");
            }
            return;
        }

        int state = FileUtils.readInt(NODE_HEADSET_STATE, 0);
        if (state == 1) {
            mImpedancePref.setSummary("Headset with Mic Connected (~16\u03a9 - 32\u03a9 IEM Detected \u2022 Zero-Hiss Active)");
        } else if (state == 2) {
            mImpedancePref.setSummary("Headphones (TRS) Connected (~16\u03a9 - 32\u03a9 IEM Detected \u2022 Zero-Hiss Active)");
        } else {
            mImpedancePref.setSummary("3.5mm Port Empty (Unplugged)");
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        SharedPreferences prefs = preference.getSharedPreferences();

        if (KEY_KCAL_PRESET.equals(key)) {
            String preset = (String) newValue;
            int r = 256, g = 256, b = 256, sat = 256;

            if ("default".equals(preset)) {
                r = 256; g = 256; b = 256; sat = 256;
            } else if ("vibrant".equals(preset)) {
                r = 256; g = 256; b = 256; sat = 285;
            } else if ("warm".equals(preset)) {
                r = 256; g = 235; b = 210; sat = 245;
            } else if ("cool".equals(preset)) {
                r = 248; g = 252; b = 256; sat = 260;
            }

            if (!"custom".equals(preset)) {
                if (mRedPref != null) mRedPref.setValue(r);
                if (mGreenPref != null) mGreenPref.setValue(g);
                if (mBluePref != null) mBluePref.setValue(b);
                if (mSatPref != null) mSatPref.setValue(sat);

                prefs.edit()
                        .putInt(KEY_KCAL_RED, r)
                        .putInt(KEY_KCAL_GREEN, g)
                        .putInt(KEY_KCAL_BLUE, b)
                        .putInt(KEY_KCAL_SAT, sat)
                        .apply();

                FileUtils.writeLine(NODE_KCAL_RGB, r + " " + g + " " + b);
                FileUtils.writeLine(NODE_KCAL_SAT, sat);
            }
            return true;
        } else if (KEY_VIBRATOR.equals(key)) {
            int val = (Integer) newValue;
            FileUtils.writeLine(NODE_VIBRATOR, val);
            return true;
        } else if (KEY_KCAL_SAT.equals(key)) {
            int val = (Integer) newValue;
            FileUtils.writeLine(NODE_KCAL_SAT, val);
            if (mPresetPref != null) mPresetPref.setValue("custom");
            return true;
        } else if (KEY_KCAL_RED.equals(key) || KEY_KCAL_GREEN.equals(key) || KEY_KCAL_BLUE.equals(key)) {
            int r = KEY_KCAL_RED.equals(key) ? (Integer) newValue : prefs.getInt(KEY_KCAL_RED, 256);
            int g = KEY_KCAL_GREEN.equals(key) ? (Integer) newValue : prefs.getInt(KEY_KCAL_GREEN, 256);
            int b = KEY_KCAL_BLUE.equals(key) ? (Integer) newValue : prefs.getInt(KEY_KCAL_BLUE, 256);
            FileUtils.writeLine(NODE_KCAL_RGB, r + " " + g + " " + b);
            if (mPresetPref != null) mPresetPref.setValue("custom");
            return true;
        } else if (KEY_GLOVE_MODE.equals(key)) {
            boolean enabled = (Boolean) newValue;
            String val = enabled ? "1" : "0";
            FileUtils.writeLine(NODE_GLOVE_MODE, val);
            FileUtils.writeLine(NODE_GLOVE_MODE_ALT, val);
            return true;
        } else if (KEY_PERF_PROFILE.equals(key)) {
            int mode = Integer.parseInt((String) newValue);
            PerformanceTileService.applyProfile(mode);
            return true;
        }
        return true;
    }
}
