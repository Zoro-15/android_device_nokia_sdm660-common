package org.lineageos.settings.device;

import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;

public class BatteryProtectionActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.battery_protection_title);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new BatteryProtectionFragment())
                    .commit();
        }
    }
}
