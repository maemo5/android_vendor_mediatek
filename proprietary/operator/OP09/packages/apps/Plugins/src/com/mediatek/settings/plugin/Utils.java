package com.mediatek.settings.plugin;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;

/**
 * replace MTK_FEATION_OPTION.
 */
public class Utils {
    private static final String TAG = "Utils";

    // Check if mtk gemini feature is enabled
    public static boolean isGeminiSupport() {
        return SystemProperties.getInt("ro.mtk_gemini_support", 0) == 1;
    }

    public static boolean isMtkSharedSdcardSupport() {
        return SystemProperties.get("ro.mtk_shared_sdcard").equals("1");
    }

    /**
     * judge if sim state is ready.
     * sim state:SIM_STATE_UNKNOWN = 0;SIM_STATE_ABSENT = 1
     * SIM_STATE_PIN_REQUIRED = 2;SIM_STATE_PUK_REQUIRED = 3;
     * SIM_STATE_NETWORK_LOCKED = 4;SIM_STATE_READY = 5;
     * SIM_STATE_CARD_IO_ERROR = 6;
     * @param context Context
     * @param simId sim id
     * @return true if is SIM_STATE_READY
     */
    public static boolean isSimStateReady(Context context, int simId) {
        TelephonyManager mTelephonyManager = TelephonyManager.from(context);
        Log.i(TAG, "isSimStateReady = " + mTelephonyManager.getSimState(simId));
        return mTelephonyManager.getSimState(simId) == TelephonyManager.SIM_STATE_READY;
    }

    /**
     * judge if sim radio on or not.
     * @param simId simid
     * @return true if sim radio on
     */
    public static boolean isTargetSimRadioOn(int simId) {
        int[] targetSubId = SubscriptionManager.getSubId(simId);
        if (targetSubId != null && targetSubId.length > 0) {
            for (int i = 0; i < targetSubId.length; i++) {
               if (isTargetSlotRadioOn(i)) {
                   Log.i(TAG, "isTargetSimRadioOn true simId = " + simId);
                   return true;
               }
            }
            Log.i(TAG, "isTargetSimRadioOn false simId = " + simId);
            return false;
        } else {
            Log.i(TAG, "isTargetSimRadioOn false because " +
                    "targetSubId[] = null or targetSubId[].length is 0  simId =" + simId);

            return false;
        }
    }

    /**
     * judge subid is radio on or not.
     * @param subId subId
     * @return true if this subId is radio on
     */
    public static boolean isTargetSlotRadioOn(int subId) {
        boolean radioOn = true;
        try {
            ITelephony iTel = ITelephony.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE));
            if (null == iTel) {
                Log.i(TAG, "isTargetSlotRadioOn = false because iTel = null");
                return false;
            }
            Log.i(TAG, "isTargetSlotRadioOn = " + iTel.isRadioOnForSubscriber(subId));
            radioOn = iTel.isRadioOnForSubscriber(subId);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
        Log.i(TAG, "isTargetSlotRadioOn radioOn = " + radioOn);
        return radioOn;
    }

    /**
     * Get the slotId insert or not.
     * @param slotId Slot id
     * @return true if slot card is insert.
     */
    public static boolean isSIMInserted(int slotId) {
        try {
            ITelephony tmex = ITelephony.Stub.asInterface(android.os.ServiceManager
                    .getService(Context.TELEPHONY_SERVICE));
            Log.i(TAG, "isSIMInserted slotId = " + slotId + "return" +
                    (tmex != null && tmex.hasIccCardUsingSlotId(slotId)));
            return (tmex != null && tmex.hasIccCardUsingSlotId(slotId));
        } catch (RemoteException e) {
            e.printStackTrace();
            Log.i(TAG, "isSIMInserted return false because catch RemoteException");
            return false;
        }
    }
}