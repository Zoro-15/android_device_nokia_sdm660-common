package org.lineageos.settings.device;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import java.util.Locale;

public class BatteryProtectionFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener {

    public static final String KEY_BYPASS = "bypass_charging_enabled";
    public static final String KEY_STOP_LEVEL = "battery_stop_level";
    public static final String KEY_CURRENT_PROFILE = "charging_current_profile";
    public static final String KEY_BLAZE_ENABLED = "blaze_thermal_engine_enabled";
    public static final String KEY_THERMAL_THRESHOLD = "thermal_threshold_celsius";

    public static final String KEY_LIVE_CURRENT = "live_current";
    public static final String KEY_LIVE_TEMP = "live_temp";
    public static final String KEY_LIVE_CYCLES = "live_cycles";

    public static final String NODE_CURRENT_NOW = "/sys/class/power_supply/battery/current_now";
    public static final String NODE_TEMP = "/sys/class/power_supply/battery/temp";
    public static final String NODE_CYCLE_COUNT = "/sys/class/power_supply/battery/cycle_count";
    public static final String NODE_STATUS = "/sys/class/power_supply/battery/status";

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Preference mCurrentPref;
    private Preference mTempPref;
    private Preference mCyclesPref;

    private final Runnable mTelemetryRunnable = new Runnable() {
        @Override
        public void run() {
            updateTelemetry();
            mHandler.postDelayed(this, 1500);
        }
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.battery_protection_preferences, rootKey);

        mCurrentPref = findPreference(KEY_LIVE_CURRENT);
        mTempPref = findPreference(KEY_LIVE_TEMP);
        mCyclesPref = findPreference(KEY_LIVE_CYCLES);

        SwitchPreference bypassPref = findPreference(KEY_BYPASS);
        if (bypassPref != null) {
            bypassPref.setOnPreferenceChangeListener(this);
        }

        ListPreference stopLevelPref = findPreference(KEY_STOP_LEVEL);
        if (stopLevelPref != null) {
            stopLevelPref.setOnPreferenceChangeListener(this);
        }

        ListPreference currentProfilePref = findPreference(KEY_CURRENT_PROFILE);
        if (currentProfilePref != null) {
            currentProfilePref.setOnPreferenceChangeListener(this);
        }

        SwitchPreference blazePref = findPreference(KEY_BLAZE_ENABLED);
        if (blazePref != null) {
            blazePref.setOnPreferenceChangeListener(this);
        }

        Preference thermalPref = findPreference(KEY_THERMAL_THRESHOLD);
        if (thermalPref != null) {
            thermalPref.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mHandler.post(mTelemetryRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mTelemetryRunnable);
    }

    private void updateTelemetry() {
        if (mCurrentPref != null) {
            boolean chargingEnabled = FileUtils.isChargingEnabled();
            if (!chargingEnabled) {
                mCurrentPref.setSummary("0 mA (Bypass Active – AC Direct)");
            } else {
                int rawCurrent = FileUtils.readInt(NODE_CURRENT_NOW, 0);
                int mA = (Math.abs(rawCurrent) > 10000) ? (rawCurrent / 1000) : rawCurrent;
                String status = FileUtils.readOneLine(NODE_STATUS);
                if (status == null) status = "Discharging";
                if (mA > 0) {
                    mCurrentPref.setSummary("+" + mA + " mA (" + status + ")");
                } else if (mA < 0) {
                    mCurrentPref.setSummary(mA + " mA (" + status + ")");
                } else {
                    mCurrentPref.setSummary("0 mA (" + status + ")");
                }
            }
        }

        if (mTempPref != null) {
            int rawTemp = FileUtils.readInt(NODE_TEMP, 0);
            if (rawTemp > 0) {
                double celsius = rawTemp / 10.0;
                mTempPref.setSummary(String.format(Locale.getDefault(), "%.1f °C", celsius));
            } else {
                mTempPref.setSummary("N/A");
            }
        }

        if (mCyclesPref != null) {
            int cycles = FileUtils.readInt(NODE_CYCLE_COUNT, 0);
            if (cycles > 0) {
                mCyclesPref.setSummary(String.format(Locale.getDefault(), "%d cycles", cycles));
            } else {
                mCyclesPref.setSummary("Healthy (OEM Gas Gauge)");
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (KEY_BYPASS.equals(key)) {
            boolean bypassed = (Boolean) newValue;
            preference.getSharedPreferences().edit().putBoolean(KEY_BYPASS, bypassed).apply();
            FileUtils.setChargingEnabled(!bypassed);
            updateTelemetry();
            return true;
        } else if (KEY_CURRENT_PROFILE.equals(key)) {
            preference.getSharedPreferences().edit().putString(KEY_CURRENT_PROFILE, (String) newValue).apply();
            ThermalChargingService.evaluate(getContext());
            return true;
        } else if (KEY_BLAZE_ENABLED.equals(key)) {
            preference.getSharedPreferences().edit().putBoolean(KEY_BLAZE_ENABLED, (Boolean) newValue).apply();
            ThermalChargingService.evaluate(getContext());
            return true;
        } else if (KEY_THERMAL_THRESHOLD.equals(key)) {
            preference.getSharedPreferences().edit().putInt(KEY_THERMAL_THRESHOLD, (Integer) newValue).apply();
            ThermalChargingService.evaluate(getContext());
            return true;
        } else if (KEY_STOP_LEVEL.equals(key)) {
            preference.getSharedPreferences().edit().putString(KEY_STOP_LEVEL, (String) newValue).apply();
            return true;
        }
        return true;
    }
}
