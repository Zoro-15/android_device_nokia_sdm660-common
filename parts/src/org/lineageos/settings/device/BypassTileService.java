package org.lineageos.settings.device;

import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import androidx.preference.PreferenceManager;

public class BypassTileService extends TileService {
    private static final String TAG = "BypassTileService";
    public static final String PREF_BYPASS_CHARGING = "bypass_charging_enabled";

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean currentlyBypassed = prefs.getBoolean(PREF_BYPASS_CHARGING, false);
        boolean newBypassState = !currentlyBypassed;

        // Bypass ON means charging is DISABLED (setChargingEnabled(false))
        // Bypass OFF means charging is ENABLED (setChargingEnabled(true))
        boolean success = FileUtils.setChargingEnabled(!newBypassState);
        if (success) {
            prefs.edit().putBoolean(PREF_BYPASS_CHARGING, newBypassState).apply();
            Log.i(TAG, "Bypass charging toggled: " + (newBypassState ? "ACTIVE (0mA)" : "INACTIVE (charging resumed)"));
        } else {
            Log.e(TAG, "Failed to write bypass state for " + FileUtils.getChargingControlNode());
        }
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean bypassed = prefs.getBoolean(PREF_BYPASS_CHARGING, false);

        // Hardware verification fallback
        bypassed = !FileUtils.isChargingEnabled();

        tile.setState(bypassed ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_bypass_charging));
        tile.setLabel(getString(R.string.bypass_charging_tile_label));
        tile.setSubtitle(bypassed ? getString(R.string.bypass_charging_active) : getString(R.string.bypass_charging_inactive));
        tile.updateTile();
    }
}
