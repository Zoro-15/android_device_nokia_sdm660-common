package org.lineageos.settings.device;

import android.content.Intent;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class NetworkModeTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();
        int currentMode = NetworkModeUtils.getPreferredNetworkMode(this);
        int nextMode;

        // Cycle: 4G Only (11) -> 3G Only (2) -> 2G Only (1) -> 4G Only (11)
        if (currentMode == NetworkModeUtils.MODE_4G_ONLY) {
            nextMode = NetworkModeUtils.MODE_3G_ONLY;
        } else if (currentMode == NetworkModeUtils.MODE_3G_ONLY) {
            nextMode = NetworkModeUtils.MODE_2G_ONLY;
        } else if (currentMode == NetworkModeUtils.MODE_2G_ONLY) {
            nextMode = NetworkModeUtils.MODE_4G_ONLY;
        } else {
            // If currently in Auto (9) or other mode, switch to 4G Only first
            nextMode = NetworkModeUtils.MODE_4G_ONLY;
        }

        NetworkModeUtils.setPreferredNetworkMode(this, nextMode);
        updateTileState();
    }

    private void updateTileState() {
        Tile tile = getQsTile();
        if (tile == null) return;

        int mode = NetworkModeUtils.getPreferredNetworkMode(this);
        String label;
        String subtitle = "";
        int state;

        String band = NetworkModeUtils.getActiveLteBand(this);

        switch (mode) {
            case NetworkModeUtils.MODE_4G_ONLY:
                label = !band.isEmpty() ? "4G Only [" + band + "]" : "4G Only";
                subtitle = !band.isEmpty() ? "Band " + band : "LTE Forced";
                state = Tile.STATE_ACTIVE;
                break;
            case NetworkModeUtils.MODE_3G_ONLY:
                label = "3G Only";
                subtitle = "HSPA/UMTS";
                state = Tile.STATE_ACTIVE;
                break;
            case NetworkModeUtils.MODE_2G_ONLY:
                label = "2G Only";
                subtitle = "GSM/EDGE";
                state = Tile.STATE_ACTIVE;
                break;
            case NetworkModeUtils.MODE_AUTO:
            default:
                label = !band.isEmpty() ? "4G Auto [" + band + "]" : "4G Auto";
                subtitle = !band.isEmpty() ? "Band " + band : "Auto Switch";
                state = Tile.STATE_INACTIVE;
                break;
        }

        tile.setLabel(label);
        try {
            tile.setSubtitle(subtitle);
        } catch (Throwable ignored) {
        }
        tile.setState(state);
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_network_mode));
        tile.updateTile();
    }
}
