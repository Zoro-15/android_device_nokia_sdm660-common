package org.lineageos.settings.device;

import android.content.Context;
import android.os.Bundle;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class NetworkSettingsFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener {

    public static final String KEY_NETWORK_MODE = "pref_network_mode";
    public static final String KEY_CARRIER_NAME = "pref_carrier_name";
    public static final String KEY_CURRENT_NETWORK = "pref_current_network";

    private ListPreference mNetworkModePref;
    private Preference mCarrierPref;
    private Preference mNetworkTypePref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.network_settings_preferences, rootKey);

        mNetworkModePref = findPreference(KEY_NETWORK_MODE);
        if (mNetworkModePref != null) {
            Context ctx = getContext();
            if (ctx != null) {
                int mode = NetworkModeUtils.getPreferredNetworkMode(ctx);
                mNetworkModePref.setValue(String.valueOf(mode));
            }
            mNetworkModePref.setOnPreferenceChangeListener(this);
        }

        mCarrierPref = findPreference(KEY_CARRIER_NAME);
        mNetworkTypePref = findPreference(KEY_CURRENT_NETWORK);

        updateLiveStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateLiveStatus();
    }

    private void updateLiveStatus() {
        Context ctx = getContext();
        if (ctx == null) return;

        if (mCarrierPref != null) {
            mCarrierPref.setSummary(NetworkModeUtils.getCarrierName(ctx));
        }
        if (mNetworkTypePref != null) {
            mNetworkTypePref.setSummary(NetworkModeUtils.getNetworkTypeName(ctx));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        Context ctx = getContext();
        if (ctx == null) return true;

        if (KEY_NETWORK_MODE.equals(key)) {
            try {
                int mode = Integer.parseInt((String) newValue);
                NetworkModeUtils.setPreferredNetworkMode(ctx, mode);
            } catch (NumberFormatException ignored) {
            }
            updateLiveStatus();
            return true;
        }
        return true;
    }
}
