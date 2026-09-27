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
        int state;

        switch (mode) {
            case NetworkModeUtils.MODE_4G_ONLY:
                label = "4G Only";
                state = Tile.STATE_ACTIVE;
                break;
            case NetworkModeUtils.MODE_3G_ONLY:
                label = "3G Only";
                state = Tile.STATE_ACTIVE;
                break;
            case NetworkModeUtils.MODE_2G_ONLY:
                label = "2G Only";
                state = Tile.STATE_ACTIVE;
                break;
            case NetworkModeUtils.MODE_AUTO:
            default:
                label = "4G Auto";
                state = Tile.STATE_INACTIVE;
                break;
        }

        tile.setLabel(label);
        tile.setState(state);
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_network_mode));
        tile.updateTile();
    }
}
