package com.mediatek.rcs.message.receiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import com.mediatek.rcs.common.binder.RCSServiceManager;

/**
 * Describe class RcsConfigStatusReceiver here.
 *
 *
 * Created: Sun Mar 29 11:13:27 2015
 *
 * @author <a href="mailto:"></a>
 * @version 1.0
 */
public class RcsConfigStatusReceiver extends BroadcastReceiver {
    private static final String TAG = "Rcs/RcsConfigStatusReceiver";
    private static final String CONFIGURATION_STATUS =
        "com.orangelabs.rcs.CONFIGURATION_STATUS_TO_APP";
    private static final String CORE_CONFIGURATION_STATUS = "status";

    /**
     * Creates a new <code>RcsConfigStatusReceiver</code> instance.
     *
     */
    public RcsConfigStatusReceiver() {
    }

    /**
     * Describe <code>onReceive</code> method here.
     *
     * @param context a <code>Context</code> value
     * @param intent an <code>Intent</code> value
     */
    public final void onReceive(final Context context, final Intent intent) {
        String action = intent.getAction();
		Log.d(TAG, "onReceive() action:" + action);
        boolean isConfigured = false;
        if (RCSServiceManager.RCS_SERVICE_MANAGER_READY.equals(action)) {
            isConfigured = (boolean)intent.getExtra(RCSServiceManager.RCS_SERVICE_MANAGER_READY_CONFIGRUATION_STATE_KEY);

        } else if (CONFIGURATION_STATUS.equals(action)) {
            isConfigured = (boolean)intent.getExtra(CORE_CONFIGURATION_STATUS);
        }

        Log.d(TAG, "onReceive() isConfigured:" + isConfigured);
        final ComponentName componentName =
            new ComponentName(context, "com.mediatek.rcs.message.ui.ForwardActivity");
        context.getPackageManager()
            .setComponentEnabledSetting(componentName,
                                        isConfigured ?
                                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                        PackageManager.DONT_KILL_APP);
    }
}
