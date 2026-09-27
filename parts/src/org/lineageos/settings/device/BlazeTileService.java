package org.lineageos.settings.device;

import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import androidx.preference.PreferenceManager;

public class BlazeTileService extends TileService {
    private static final String TAG = "BlazeTileService";
    public static final String KEY_CURRENT_PROFILE = "charging_current_profile";
    public static final String VAL_BALANCED = "2000000";
    public static final String VAL_TURBO = "3000000";

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String currentProfile = prefs.getString(KEY_CURRENT_PROFILE, VAL_BALANCED);
        boolean isCurrentlyTurbo = VAL_TURBO.equals(currentProfile);
        String newProfile = isCurrentlyTurbo ? VAL_BALANCED : VAL_TURBO;

        prefs.edit().putString(KEY_CURRENT_PROFILE, newProfile).apply();
        Log.i(TAG, "Blaze Turbo QS tile clicked: " + (newProfile.equals(VAL_TURBO) ? "TURBO (3000mA)" : "BALANCED (2000mA)"));

        // Evaluate immediately to apply charging current to sysfs
        ThermalChargingService.evaluate(this);

        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String currentProfile = prefs.getString(KEY_CURRENT_PROFILE, VAL_BALANCED);
        boolean isTurbo = VAL_TURBO.equals(currentProfile);

        tile.setState(isTurbo ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_blaze_turbo));
        tile.setLabel(getString(R.string.blaze_turbo_tile_label));
        tile.setSubtitle(isTurbo ? getString(R.string.blaze_turbo_tile_active) : getString(R.string.blaze_turbo_tile_inactive));
        tile.updateTile();
    }
}
