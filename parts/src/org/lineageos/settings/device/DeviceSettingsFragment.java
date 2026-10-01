package org.lineageos.settings.device;

import android.os.Bundle;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;

public class DeviceSettingsFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener {

    public static final String NODE_VIBRATOR = "/sys/class/leds/vibrator/vmax_mv";
    public static final String NODE_GLOVE_MODE =
            "/sys/devices/platform/soc/c1b5000.i2c/i2c-5/5-0038/fts_glove_mode";
    public static final String NODE_GLOVE_MODE_ALT =
            "/proc/touchscreen/glove_mode";

    public static final String KEY_VIBRATOR = "vibrator_intensity";
    public static final String KEY_GLOVE_MODE = "glove_mode_enabled";
    public static final String KEY_PERF_PROFILE = "perf_mode_profile";
    public static final String KEY_AUTO_IEM = "auto_iem_profile";
    public static final String KEY_HEADPHONE_IMPEDANCE = "headphone_impedance_status";

    public static final String NODE_HEADSET_STATE = "/sys/class/switch/h2w/state";
    public static final String NODE_IMPEDANCE_1 = "/sys/bus/i2c/drivers/wcd9335/impedance";
    public static final String NODE_IMPEDANCE_2 = "/sys/class/switch/h2w/impedance";
    public static final String NODE_IMPEDANCE_3 = "/sys/devices/platform/soc/soc:qcom,msm-audio-pinctrl/impedance";

    private SeekBarPreference mVibPref;
    private SwitchPreference mGlovePref;
    private ListPreference mPerfPref;
    private Preference mImpedancePref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.device_settings_preferences, rootKey);

        mVibPref = findPreference(KEY_VIBRATOR);
        if (mVibPref != null) {
            mVibPref.setMin(1500);
            mVibPref.setMax(3100);
            mVibPref.setOnPreferenceChangeListener(this);
        }

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

        if (KEY_VIBRATOR.equals(key)) {
            int val = (Integer) newValue;
            FileUtils.writeLine(NODE_VIBRATOR, val);
            return true;
        } else if (KEY_GLOVE_MODE.equals(key)) {
            boolean enabled = (Boolean) newValue;
            String val = enabled ? "1" : "0";
            FileUtils.writeLine(NODE_GLOVE_MODE, val);
            FileUtils.writeLine(NODE_GLOVE_MODE_ALT, val);
            return true;
        } else if (KEY_PERF_PROFILE.equals(key)) {
            try {
                int mode = Integer.parseInt((String) newValue);
                PerformanceTileService.applyProfile(mode);
            } catch (Exception ignored) {}
            return true;
        }
        return true;
    }
}
