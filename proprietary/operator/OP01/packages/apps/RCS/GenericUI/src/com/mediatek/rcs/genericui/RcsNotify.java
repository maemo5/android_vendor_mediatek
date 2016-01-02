package com.mediatek.rcs.genericui;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RcsNotify extends BroadcastReceiver {
    
    private final static String TAG = "RcsNotify";
    private final static int NOTIFICATION_ID = 1000;
    private final static int ILLEGAL_STATE = -1;
    private final static int RCS_CORE_LOADED = 0;
    private final static int RCS_CORE_FAILED = 1;
    private final static int RCS_CORE_STARTED = 2;
    private final static int RCS_CORE_STOPPED = 3;
    private final static int RCS_CORE_IMS_CONNECTED = 4;
    private final static int RCS_CORE_IMS_TRY_CONNECTION = 5;
    private final static int RCS_CORE_IMS_CONNECTION_FAILED = 6;
    private final static int RCS_CORE_IMS_TRY_DISCONNECT = 7;
    private final static int RCS_CORE_IMS_BATTERY_DISCONNECTED = 8;
    private final static int RCS_CORE_IMS_DISCONNECTED = 9;
    
    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        Log.d(TAG, "onReceive " + intent.getAction());
        mContext = context;
        if (intent.getAction().equals("org.gsma.joyn.action.VIEW_SETTINGS")) {

            int state = intent.getIntExtra("label_enum", ILLEGAL_STATE);
            Log.d(TAG, "onReceive state:" + state);

            int strId = setStringByState(state);
            int iconId = R.drawable.rcs_core_notif_off;
            if (state == RCS_CORE_IMS_CONNECTED) {
                iconId = R.drawable.rcs_core_notif_on;
            }
            
            showNotification(strId, iconId);
            
        } else if (intent.getAction().equals("com.orangelabs.rcs.SHOW_403_NOTIFICATION")) {
            showPsDialogActivity();
        }
    }

    private void showNotification(int strId, int iconId) {
        // Intents.Client.ACTION_VIEW_SETTINGS
        Intent intent = new Intent("org.gsma.joyn.action.VIEW_SETTINGS");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Notification notif = new Notification(mContext, iconId, "", 
            System.currentTimeMillis(),
            mContext.getString(R.string.rcs_core_rcs_notification_title),
            mContext.getString(strId), intent);
        notif.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_FOREGROUND_SERVICE;
        NotificationManager notificationManager =
            (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notif);
    }

    private void showPsDialogActivity() {
        Intent activityIntent = new Intent();
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        activityIntent.setClass(mContext, RcsPsAlertDialog.class);
        mContext.startActivity(activityIntent);
    }
    
    private int setStringByState(int state) {
        int strId;
        switch (state) {
        case RCS_CORE_LOADED:
            strId = R.string.rcs_core_loaded;
            break;
        case RCS_CORE_FAILED:
            strId = R.string.rcs_core_failed;
            break;
        case RCS_CORE_STARTED:
            strId = R.string.rcs_core_started;
            break;
        case RCS_CORE_STOPPED:
            strId = R.string.rcs_core_stopped;
            break;
        case RCS_CORE_IMS_CONNECTED:
            strId = R.string.rcs_core_ims_connected;
            break;
        case RCS_CORE_IMS_TRY_CONNECTION:
            strId = R.string.rcs_core_ims_try_connection;
            break;
        case RCS_CORE_IMS_CONNECTION_FAILED:
            strId = R.string.rcs_core_ims_connection_failed;
            break;
        case RCS_CORE_IMS_TRY_DISCONNECT:
            strId = R.string.rcs_core_ims_try_disconnect;
            break;
        case RCS_CORE_IMS_BATTERY_DISCONNECTED:
            strId = R.string.rcs_core_ims_battery_disconnected;
            break;
        case RCS_CORE_IMS_DISCONNECTED:
            strId = R.string.rcs_core_ims_disconnected;
            break; 
        case ILLEGAL_STATE:
        default :
            strId = R.string.app_name; // Example
            break;
        }
        
        return strId;
    }

}
