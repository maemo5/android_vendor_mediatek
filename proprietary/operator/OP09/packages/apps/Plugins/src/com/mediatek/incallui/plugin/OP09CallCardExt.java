package com.mediatek.incallui.plugin;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.telecom.PhoneAccount;
import android.telecom.TelecomManager;
import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.incallui.ext.DefaultCallCardExt;

/**
 * callcard extension plugin for op09.
*/
@PluginImpl(interfaceName = "com.mediatek.incallui.ext.ICallCardExt")
public class OP09CallCardExt extends DefaultCallCardExt {

    private static final String TAG = "OP09CallCardExt";

    private TelecomManager mTelecomManager;

    /**
     * Return the icon drawable to represent the call provider.
     *
     * @param context for get service.
     * @param account for get icon.
     * @return The icon.
    */
    public Drawable getCallProviderIcon(Context context, PhoneAccount account){
        log("getCallProviderIcon account:" + account);
        if (account != null && getTelecomManager(
                        context).getCallCapablePhoneAccounts().size() > 0) {
            //return account.getIcon(context);
        }
        return null;
    }

    /**
     * Return the string label to represent the call provider.
     *
     * @param context  for get service.
     * @param account for get lable.
     * @return The lable.
    */
    public String getCallProviderLabel(Context context, PhoneAccount account) {
        if (account != null && getTelecomManager(
                        context).getCallCapablePhoneAccounts().size() > 0) {
            return account.getLabel().toString();
        }
        return null;
    }

    private TelecomManager getTelecomManager(Context context) {
        if (mTelecomManager == null) {
            mTelecomManager =
                    (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
        }
        return mTelecomManager;
    }

    /**
     * simple log info.
     *
     * @param msg need print out string.
     * @return void.
     */
    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}

