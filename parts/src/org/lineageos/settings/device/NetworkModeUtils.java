package org.lineageos.settings.device;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import androidx.preference.PreferenceManager;
import java.lang.reflect.Method;

public final class NetworkModeUtils {

    public static final int MODE_4G_ONLY = 11; // NETWORK_MODE_LTE_ONLY
    public static final int MODE_3G_ONLY = 2;  // NETWORK_MODE_WCDMA_ONLY
    public static final int MODE_2G_ONLY = 1;  // NETWORK_MODE_GSM_ONLY
    public static final int MODE_AUTO = 9;     // NETWORK_MODE_LTE_GSM_WCDMA

    public static final String KEY_NETWORK_MODE = "pref_network_mode";

    private NetworkModeUtils() {}

    public static int getActiveSubId() {
        int subId = SubscriptionManager.getDefaultDataSubscriptionId();
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            subId = SubscriptionManager.getDefaultSubscriptionId();
        }
        return subId;
    }

    public static int getPreferredNetworkMode(Context context) {
        int subId = getActiveSubId();
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.PREFERRED_NETWORK_MODE + subId,
                Settings.Global.getInt(context.getContentResolver(),
                        Settings.Global.PREFERRED_NETWORK_MODE, MODE_AUTO));
    }

    public static void setPreferredNetworkMode(Context context, int mode) {
        int subId = getActiveSubId();
        Settings.Global.putInt(context.getContentResolver(),
                Settings.Global.PREFERRED_NETWORK_MODE + subId, mode);
        Settings.Global.putInt(context.getContentResolver(),
                Settings.Global.PREFERRED_NETWORK_MODE, mode);

        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm != null) {
            try {
                Method method = TelephonyManager.class.getMethod("setPreferredNetworkType", int.class, int.class);
                method.invoke(tm, subId, mode);
            } catch (Exception ignored) {
                // Fallback for Android 10
            }
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(KEY_NETWORK_MODE, String.valueOf(mode)).apply();
    }

    public static String getCarrierName(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm != null) {
            String name = tm.getNetworkOperatorName();
            if (name != null && !name.trim().isEmpty()) {
                return name;
            }
            name = tm.getSimOperatorName();
            if (name != null && !name.trim().isEmpty()) {
                return name;
            }
        }
        return "No SIM / Unknown";
    }

    public static String getActiveLteBand(Context context) {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                java.util.List<android.telephony.CellInfo> cellInfoList = tm.getAllCellInfo();
                if (cellInfoList != null) {
                    for (android.telephony.CellInfo cellInfo : cellInfoList) {
                        if (cellInfo instanceof android.telephony.CellInfoLte && cellInfo.isRegistered()) {
                            android.telephony.CellIdentityLte lte = ((android.telephony.CellInfoLte) cellInfo).getCellIdentity();
                            int earfcn = lte.getEarfcn();
                            if (earfcn >= 38650 && earfcn <= 39649) return "B40";
                            if (earfcn >= 1200 && earfcn <= 1949) return "B3";
                            if (earfcn >= 2400 && earfcn <= 2649) return "B5";
                            if (earfcn >= 0 && earfcn <= 599) return "B1";
                            if (earfcn >= 3350 && earfcn <= 3799) return "B8";
                            if (earfcn >= 2750 && earfcn <= 3449) return "B7";
                            if (earfcn >= 39650 && earfcn <= 41589) return "B41";
                            if (earfcn >= 600 && earfcn <= 1199) return "B2";
                            if (earfcn >= 1950 && earfcn <= 2399) return "B4";
                            if (earfcn >= 5010 && earfcn <= 5179) return "B12";
                            if (earfcn >= 5180 && earfcn <= 5279) return "B13";
                            if (earfcn >= 5730 && earfcn <= 5849) return "B17";
                            if (earfcn >= 66436 && earfcn <= 67335) return "B66";
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return "";
    }

    public static String getNetworkTypeName(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null) return "Unknown";

        int type = tm.getNetworkType();
        switch (type) {
            case TelephonyManager.NETWORK_TYPE_LTE: {
                String band = getActiveLteBand(context);
                if (!band.isEmpty()) {
                    return "4G LTE [" + band + "] (Connected)";
                }
                return "4G LTE (Connected)";
            }
            case TelephonyManager.NETWORK_TYPE_HSPAP:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return "3G HSPA / UMTS (Connected)";
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_GSM:
                return "2G GSM / EDGE (Connected)";
            default:
                return "Standby / Searching";
        }
    }
}
