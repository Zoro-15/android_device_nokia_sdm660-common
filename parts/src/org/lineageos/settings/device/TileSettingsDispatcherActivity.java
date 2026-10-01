package org.lineageos.settings.device;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

public class TileSettingsDispatcherActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ComponentName component = getIntent().getParcelableExtra(Intent.EXTRA_COMPONENT_NAME);
        if (component == null) {
            component = getIntent().getParcelableExtra("android.service.quicksettings.extra.COMPONENT_NAME");
        }
        if (component == null) {
            component = getIntent().getParcelableExtra("android.intent.extra.COMPONENT_NAME");
        }

        Intent targetIntent;
        if (component != null) {
            String cls = component.getClassName();
            if (cls.contains("NetworkModeTileService")) {
                targetIntent = new Intent(this, NetworkSettingsActivity.class);
            } else if (cls.contains("BypassTileService") || cls.contains("BlazeTileService")) {
                targetIntent = new Intent(this, BatteryProtectionActivity.class);
            } else {
                targetIntent = new Intent(this, DeviceSettingsActivity.class);
            }
        } else {
            targetIntent = new Intent(this, DeviceSettingsActivity.class);
        }

        try {
            sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        } catch (Exception ignored) {}

        targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(targetIntent);
        finish();
    }
}
