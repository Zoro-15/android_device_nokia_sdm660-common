package org.lineageos.settings.device;

import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import androidx.preference.PreferenceManager;

public class PerformanceTileService extends TileService {

    public static final String PREF_PERF_MODE = "perf_mode_profile";
    public static final int MODE_BALANCED = 0;
    public static final int MODE_GAMING = 1;
    public static final int MODE_BATTERY_SAVER = 2;

    public static final String NODE_GPU_MIN_FREQ =
            "/sys/class/kgsl/kgsl-3d0/devfreq/min_freq";
    public static final String NODE_STUNE_BOOST =
            "/dev/stune/top-app/schedtune.boost";
    public static final String NODE_CPU_BIG_MAX =
            "/sys/devices/system/cpu/cpufreq/policy4/scaling_max_freq";

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int current = prefs.getInt(PREF_PERF_MODE, MODE_BALANCED);
        int next;

        // Cycle: Balanced (0) -> Gaming (1) -> Battery Saver (2) -> Balanced (0)
        if (current == MODE_BALANCED) {
            next = MODE_GAMING;
        } else if (current == MODE_GAMING) {
            next = MODE_BATTERY_SAVER;
        } else {
            next = MODE_BALANCED;
        }

        applyProfile(next);
        prefs.edit().putInt(PREF_PERF_MODE, next).apply();
        updateTileState();
    }

    public static void applyProfile(int mode) {
        switch (mode) {
            case MODE_GAMING:
                // GPU max clock 588MHz + high top-app responsiveness boost
                FileUtils.writeLine(NODE_GPU_MIN_FREQ, "588000000");
                FileUtils.writeLine(NODE_STUNE_BOOST, "30");
                FileUtils.writeLine(NODE_CPU_BIG_MAX, "2208000");
                break;
            case MODE_BATTERY_SAVER:
                // GPU idle floor 160MHz + 0 boost + underclock Gold cluster to 1.4GHz
                FileUtils.writeLine(NODE_GPU_MIN_FREQ, "160000000");
                FileUtils.writeLine(NODE_STUNE_BOOST, "0");
                FileUtils.writeLine(NODE_CPU_BIG_MAX, "1401600");
                break;
            case MODE_BALANCED:
            default:
                // Standard balanced operation
                FileUtils.writeLine(NODE_GPU_MIN_FREQ, "160000000");
                FileUtils.writeLine(NODE_STUNE_BOOST, "5");
                FileUtils.writeLine(NODE_CPU_BIG_MAX, "2208000");
                break;
        }
    }

    private void updateTileState() {
        Tile tile = getQsTile();
        if (tile == null) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int mode = prefs.getInt(PREF_PERF_MODE, MODE_BALANCED);
        String label;
        int state;

        switch (mode) {
            case MODE_GAMING:
                label = "Gaming Turbo";
                state = Tile.STATE_ACTIVE;
                break;
            case MODE_BATTERY_SAVER:
                label = "Battery Saver";
                state = Tile.STATE_ACTIVE;
                break;
            case MODE_BALANCED:
            default:
                label = "Balanced Mode";
                state = Tile.STATE_INACTIVE;
                break;
        }

        tile.setLabel(label);
        tile.setState(state);
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_performance_mode));
        tile.updateTile();
    }
}
