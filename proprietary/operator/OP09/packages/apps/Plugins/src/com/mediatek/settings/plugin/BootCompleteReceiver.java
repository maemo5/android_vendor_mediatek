package com.mediatek.settings.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Start DataSubAutoSwitch Service after boot complete. To meet CT spec: if
 * current data sub is radio off, should switch to next active sub automatically.
 */
public class BootCompleteReceiver extends BroadcastReceiver {

    private static final String TAG = "BootCompleteReceiver";
    private static final String IPO_BOOT = "android.intent.action.ACTION_BOOT_IPO";
    private static final String DATA_SUB_SERVICE = "com.mediatek.OP09.DATA_SUB_AUTO_SWITCH";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.v(TAG, "onReceive action=" + action);
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)
                || action.equals(IPO_BOOT)) {
            if (TelephonyManager.getDefault().getSimCount() >= 2) {
                startDataSubService(context);
            }
        }
    }

    private void startDataSubService(Context context) {
        Intent dataSubServiceIntent = new Intent(DATA_SUB_SERVICE);
        dataSubServiceIntent.setPackage(context.getBasePackageName());
        context.startService(dataSubServiceIntent);
    }
}
