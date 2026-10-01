package org.lineageos.settings.device;

import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;

public class DeviceSettingsFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener {

    public static final String NODE_VIBRATOR = "/sys/class/leds/vibrator/vmax_mv";
    public static final String KEY_VIBRATOR = "vibrator_intensity";

    private SeekBarPreference mVibPref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.device_settings_preferences, rootKey);

        mVibPref = findPreference(KEY_VIBRATOR);
        if (mVibPref != null) {
            mVibPref.setMin(1500);
            mVibPref.setMax(3100);
            mVibPref.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();

        if (KEY_VIBRATOR.equals(key)) {
            int val = (Integer) newValue;
            FileUtils.writeLine(NODE_VIBRATOR, val);
            return true;
        }
        return true;
    }
}
