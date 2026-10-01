package org.lineageos.settings.device;

import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.UserHandle;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.IWindowManager;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import androidx.preference.PreferenceManager;

public class ResolutionTileService extends TileService {
    private static final String TAG = "ResolutionTileService";
    public static final String PREF_IS_720P = "pref_resolution_720p_active";

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();
        boolean is720p = isCurrently720p();
        setResolutionMode(!is720p);
        updateTileState();
    }

    private boolean isCurrently720p() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.contains(PREF_IS_720P)) {
            return prefs.getBoolean(PREF_IS_720P, false);
        }
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (wm != null) {
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getRealMetrics(dm);
            return dm.widthPixels <= 720;
        }
        return false;
    }

    private void setResolutionMode(boolean enable720p) {
        try {
            IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
            if (wm != null) {
                if (enable720p) {
                    wm.setForcedDisplaySize(Display.DEFAULT_DISPLAY, 720, 1520);
                    wm.setForcedDisplayDensityForUser(Display.DEFAULT_DISPLAY, 280, UserHandle.myUserId());
                } else {
                    wm.clearForcedDisplaySize(Display.DEFAULT_DISPLAY);
                    wm.clearForcedDisplayDensityForUser(Display.DEFAULT_DISPLAY, UserHandle.myUserId());
                }
            }
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putBoolean(PREF_IS_720P, enable720p)
                    .apply();
        } catch (Throwable t) {
            Log.w(TAG, "IWindowManager call failed, falling back to wm command: " + t.getMessage());
            try {
                if (enable720p) {
                    Runtime.getRuntime().exec(new String[]{"wm", "size", "720x1520"}).waitFor();
                    Runtime.getRuntime().exec(new String[]{"wm", "density", "280"}).waitFor();
                } else {
                    Runtime.getRuntime().exec(new String[]{"wm", "size", "reset"}).waitFor();
                    Runtime.getRuntime().exec(new String[]{"wm", "density", "reset"}).waitFor();
                }
                PreferenceManager.getDefaultSharedPreferences(this)
                        .edit()
                        .putBoolean(PREF_IS_720P, enable720p)
                        .apply();
            } catch (Exception e) {
                Log.e(TAG, "Failed to switch resolution: " + e.getMessage());
            }
        }
    }

    private void updateTileState() {
        Tile tile = getQsTile();
        if (tile == null) return;

        boolean is720p = isCurrently720p();
        tile.setState(is720p ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel(is720p ? "720p HD+" : "1080p FHD+");
        try {
            tile.setSubtitle(is720p ? "Gaming Mode" : "Native Mode");
        } catch (Throwable ignored) {
        }
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_resolution_mode));
        tile.updateTile();
    }
}
