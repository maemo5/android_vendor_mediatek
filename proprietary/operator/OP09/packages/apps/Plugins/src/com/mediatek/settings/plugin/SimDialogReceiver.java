package com.mediatek.settings.plugin;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Window;
import android.view.WindowManager;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.IccCardConstants.CardType;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;

import com.mediatek.op09.plugin.R;

/**
 * When user insert 3g cdma card or incorrect card.
 * Show a dialog
 */
public class SimDialogReceiver extends BroadcastReceiver {

    public static final String TEXT = "text";

    private static final String TAG = "SimDialogReceiver";
    private static final String ACTION_CDMA_CARD_TYPE = "android.intent.action.CDMA_CARD_TYPE";
    private static final String INTENT_KEY_CAMA_CARD_TYPE = "cama_card_type";
    private static final String UIM_CHANGE_ALERT_ACTIVITY_NAME
            = "com.mediatek.OP09.UIM_CHANGE_ALERT";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "intent = " + intent.getAction());
        if (TelephonyIntents.ACTION_CDMA_CARD_TYPE.equals(intent.getAction())) {
            CardType cardType = (CardType)
                intent.getExtra(TelephonyIntents.INTENT_KEY_CDMA_CARD_TYPE);
            Log.i(TAG, "intent cardType = " + cardType.toString());
            if (cardType.equals(IccCardConstants.CardType.CARD_NOT_INSERTED)) {
                showAlert(context, context.getString(R.string.no_sim_dialog_message));
            } else if (!cardType.equals(IccCardConstants.CardType.PIN_LOCK_CARD) &&
                    !cardType.equals(IccCardConstants.CardType.CT_4G_UICC_CARD)) {
                showAlert(context, context.getString(R.string.lte_sim_dialog_message));
            }
        }
    }

    private void showAlert(Context context, String text) {
        Log.d(TAG, "showAlert text=" + text);
        Intent launchIntent = new Intent(UIM_CHANGE_ALERT_ACTIVITY_NAME);
        launchIntent.setPackage(context.getPackageName());
        launchIntent.putExtra(TEXT, text);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(launchIntent);
    }
}
