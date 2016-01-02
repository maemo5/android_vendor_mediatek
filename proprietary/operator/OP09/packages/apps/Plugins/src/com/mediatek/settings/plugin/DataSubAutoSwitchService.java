/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.settings.plugin;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;

import java.util.List;

/**
 * To meet CT spec: if current data sub is radio off, should switch to
 * next active sub automatically.
 */
public class DataSubAutoSwitchService extends Service {
    private static final String TAG = "DataSubAutoSwitchService";

    private TelephonyManager mTelephonyManager;
    private ITelephony mITelephony;
    private PhoneStateListener mPhoneStateListener;

    private static final int MODE_PHONE1_ONLY = 1;
    private static final int MODE_PHONE2_ONLY = 2;

    private static final boolean RADIO_POWER_OFF = false;
    private static final boolean RADIO_POWER_ON = true;

    @Override
    public void onCreate() {
        super.onCreate();
        mPhoneStateListener = createPhoneStateListener();
        registerPhoneStateListener(mPhoneStateListener);
        Log.d(TAG, "onCreate");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        mITelephony = null;
        unregisterPhoneStateListener(mPhoneStateListener);
    }

    private void registerPhoneStateListener(PhoneStateListener phoneStateListener) {
        Log.d(TAG, "registerPhoneStateListener enter");
        getTelephonyManager().listen(phoneStateListener,
                PhoneStateListener.LISTEN_SERVICE_STATE);
    }

    private void unregisterPhoneStateListener(PhoneStateListener phoneStateListener) {
        Log.d(TAG, "unregisterPhoneStateListener enter");
        getTelephonyManager().listen(phoneStateListener,
                PhoneStateListener.LISTEN_NONE);
    }

    private void handleDataSubSwitch() {
        if (!isAirPlaneModeOn(this)
                && isMobileDataOn()
                && shouldSwitchDataSub()) {
            int preferredDataSub = getPreferreDataSub();
            if (preferredDataSub > -1) {
                SubscriptionManager.from(this).setDefaultDataSubId(preferredDataSub);
                Log.d(TAG, "onServiceStateChanged change data sub to " + preferredDataSub
                        + ", slotId=" + SubscriptionManager.getSlotId(preferredDataSub));
            }
        }
    }

    private PhoneStateListener createPhoneStateListener() {
        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            @Override
            public void onServiceStateChanged(ServiceState serviceState) {
                Log.d(TAG, "onServiceStateChanged serviceState=" + serviceState.getState());
                handleDataSubSwitch();
            }
        };
        return phoneStateListener;
    }

    private boolean shouldSwitchDataSub() {
        int subId = SubscriptionManager.getDefaultDataSubId();
        Log.d(TAG, "isCurrentDataSubRadioOff current dataSubId=" + subId);
        if (subId < 0) {
            return false;
        } else {
            boolean radioOn = isTargetSubRadioOn(subId);
            return (!radioOn && shouldSwitchWhenRadioOff(subId));
        }
    }

    private boolean shouldSwitchWhenRadioOff(int subId) {
        int dualSimMode = Settings.System.getInt(this
                .getContentResolver(), Settings.System.MSIM_MODE_SETTING, -1);
        int slotId = SubscriptionManager.getSlotId(subId);
        if (slotId >= 0) {
            if ((dualSimMode & (MODE_PHONE1_ONLY << slotId)) == 1
                    ? RADIO_POWER_ON : RADIO_POWER_OFF) {
                Log.d(TAG, "shouldSwitchWhenRadioOff return false msimMode=" + dualSimMode);
                return false;
            } else {
                Log.d(TAG, "shouldSwitchWhenRadioOff return true msimMode=" + dualSimMode);
                return true;
            }
        }
        Log.d(TAG, "shouldSwitchWhenRadioOff return false slotId=" + slotId + ", subId=" + subId);
        return false;
    }

    private int getPreferreDataSub() {
        int curDataSubId = SubscriptionManager.getDefaultDataSubId();
        int newDataSubId = -1;
        List<SubscriptionInfo> subInfoRecordList =
                SubscriptionManager.from(this).getActiveSubscriptionInfoList();
        for (SubscriptionInfo subInfoRecord : subInfoRecordList) {
            if (subInfoRecord.getSubscriptionId() != curDataSubId &&
                    isTargetSubRadioOn(subInfoRecord.getSubscriptionId())) {
                newDataSubId = subInfoRecord.getSubscriptionId();
            }
        }
        Log.d(TAG, "switchDataSub curDataSubId=" + curDataSubId +
                ", newDataSubId=" + newDataSubId);
        return newDataSubId;
    }

    private boolean isAirPlaneModeOn(Context context) {
        int airplaneMode = Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0);
        Log.d(TAG, "isAirPlaneModeOn airplaneMode=" + airplaneMode);
        return airplaneMode == 1;
    }

    private boolean isMobileDataOn() {
        boolean dataEnabled = getTelephonyManager().getDataEnabled();
        Log.d(TAG, "isMobileDataOn dataEnabled=" + dataEnabled);
        return dataEnabled;
    }

    private boolean isTargetSubRadioOn(int subId) {
        boolean isRadioOn = false;
        try {
            isRadioOn = getITelephony().isRadioOnForSubscriber(subId);
        } catch (RemoteException e) {
            isRadioOn = false;
            Log.e(TAG, "ITelephony exception");
        }
        Log.d(TAG, "isTargetSubRadioOn subId=" + subId + ", isRadioOn=" + isRadioOn);
        return isRadioOn;
    }

    private ITelephony getITelephony() {
        if (mITelephony == null) {
            mITelephony = ITelephony.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE));
        }
        return mITelephony;
    }

    private TelephonyManager getTelephonyManager() {
        if (mTelephonyManager == null) {
            mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        }
        return mTelephonyManager;
    }
}
