package org.lineageos.settings.device;

import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import androidx.preference.PreferenceManager;

public class BypassTileService extends TileService {
    private static final String TAG = "BypassTileService";

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean currentlyBypassed = prefs.getBoolean(ThermalChargingService.PREF_BYPASS_CHARGING, false);
        boolean newBypassState = !currentlyBypassed;

        prefs.edit().putBoolean(ThermalChargingService.PREF_BYPASS_CHARGING, newBypassState).apply();
        boolean success = ThermalChargingService.setBypassMode(this, newBypassState);
        if (success) {
            Log.i(TAG, "Bypass charging toggled: " + (newBypassState ? "ACTIVE (0mA AC Direct)" : "INACTIVE (charging resumed)"));
        } else {
            Log.e(TAG, "Failed to apply bypass state");
        }
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;

        boolean bypassed = ThermalChargingService.isBypassActive(this);

        tile.setState(bypassed ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_bypass_charging));
        tile.setLabel(getString(R.string.bypass_charging_tile_label));
        tile.setSubtitle(bypassed ? getString(R.string.bypass_charging_active) : getString(R.string.bypass_charging_inactive));
        tile.updateTile();
    }
}
