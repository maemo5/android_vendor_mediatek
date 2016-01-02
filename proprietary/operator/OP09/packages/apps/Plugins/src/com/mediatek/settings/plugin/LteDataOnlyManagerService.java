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

import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.view.WindowManager;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.op09.plugin.R;
import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.xlog.Xlog;

/**
 * Service that check Lte data only mode and whether show dialog.
 */
public class LteDataOnlyManagerService extends Service {
    private static final String TAG = "LteDataOnlyManagerService";
    private AlertDialog mDialog;
    private int mStartId = -1;
    private static final boolean RADIO_POWER_OFF = false;
    private static final boolean RADIO_POWER_ON = true;
    private static final int MODE_PHONE1_ONLY = 1;

    @Override
    public void onCreate() {
        Xlog.d(TAG, "onCreate");
        super.onCreate();
        createPermissionDialog();
        getContentResolver().registerContentObserver(
           Settings.Global.getUriFor(Settings.Global.LTE_ON_CDMA_RAT_MODE),
           true, mContentObserver);

        getContentResolver().registerContentObserver(
           Settings.System.getUriFor(Settings.System.MSIM_MODE_SETTING),
           true, mObserverForRadioState);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
        registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // start a new query
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Xlog.d(TAG, "onStartCommand, startId = " + startId);
        mStartId = startId;
        showPermissionDialog();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Xlog.d(TAG, "onDestroy");
        getContentResolver().unregisterContentObserver(mContentObserver);
        getContentResolver().unregisterContentObserver(mObserverForRadioState);
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        unregisterReceiver(mReceiver);
    }

    private void createPermissionDialog() {
         final AlertDialog.Builder builder = new AlertDialog.Builder(this);
         builder.setTitle(getString(R.string.lte_only_dialog_title_prompt))
                .setMessage(R.string.lte_data_only_prompt)
                .setNegativeButton(R.string.lte_only_dialog_button_no,
                    new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        stopSelf(mStartId);
                    }
                })
                .setPositiveButton(R.string.lte_only_dialog_button_yes,
                      new DialogInterface.OnClickListener() {
                      @Override
                      public void onClick(DialogInterface dialog, int which) {
                          if (!checkServiceCondition()) {
                              Xlog.d(TAG,
                                "PositiveButton onClick :checkServiceCondition failed, stop");
                              stopSelf(mStartId);
                              return;
                          }
                          try {
                               ITelephonyEx telephonyEx = ITelephonyEx.Stub.asInterface(
                                   ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
                               if (telephonyEx != null) {
                                   Settings.Global.putInt(getContentResolver(),
                                                         Settings.Global.LTE_ON_CDMA_RAT_MODE,
                                                         TelephonyManagerEx.SVLTE_RAT_MODE_4G);
                                   telephonyEx.switchSvlteRatMode(
                                       TelephonyManagerEx.SVLTE_RAT_MODE_4G);
                               }
                          } catch (RemoteException e) {
                               e.printStackTrace();
                         } finally {
                               stopSelf(mStartId);
                         }
                      }
                  })
                  .setOnDismissListener(
                            new DialogInterface.OnDismissListener() {
                                public void onDismiss(DialogInterface dialog) {
                                    Xlog.d(TAG,
                                "OnDismissListener :stopSelf(), mStartId = " + mStartId);
                                    stopSelf(mStartId);
                                }
                             });
          mDialog = builder.create();
          mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
          mDialog.setCanceledOnTouchOutside(false);
     }

    private void showPermissionDialog() {
        if (!checkServiceCondition()) {
            stopSelf(mStartId);
            return;
        }
        if (!mDialog.isShowing()) {
            mDialog.show();
        }
    }

    private boolean checkServiceCondition() {
        return is4GDataOnly() && !isAirPlaneMode() && isSlot1Inserted()
            && isSlot1RadioOn();
    }

    private boolean is4GDataOnly() {
        int patternLteDataOnly = Settings.Global.getInt(getContentResolver(),
                                   Settings.Global.LTE_ON_CDMA_RAT_MODE,
                                   TelephonyManagerEx.SVLTE_RAT_MODE_4G);
       return (patternLteDataOnly == TelephonyManagerEx.SVLTE_RAT_MODE_4G_DATA_ONLY);
    }

    private boolean isAirPlaneMode() {
        Xlog.i(TAG, "isAirPlaneMode = " + (Settings.System.getInt(getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, -1) == 1));
        return Settings.System.getInt(getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, -1) == 1;
    }

    private boolean isSlot1Inserted() {
        boolean isSlot1Inserted = false;
        TelephonyManagerEx telephonyManagerEx = TelephonyManagerEx.getDefault();
        if (telephonyManagerEx != null) {
            isSlot1Inserted = telephonyManagerEx.hasIccCard(PhoneConstants.SIM_ID_1);
        }
        Xlog.i(TAG, "isSIMInserted = " + isSlot1Inserted);
        return isSlot1Inserted;
    }

    private boolean isSlot1RadioOn() {
        return (getRadioStateForSlotId(PhoneConstants.SIM_ID_1) == RADIO_POWER_ON);
    }

    private boolean getRadioStateForSlotId(final int slotId) {
        int currentSimMode = Settings.System.getInt(getContentResolver(),
                Settings.System.MSIM_MODE_SETTING, -1);
        boolean radiosState = ((currentSimMode & (MODE_PHONE1_ONLY << slotId)) == 0) ?
                RADIO_POWER_OFF : RADIO_POWER_ON;
        Xlog.d(TAG, "soltId: " + slotId + ", radiosState : " + radiosState);
        return radiosState;
    }

    private ContentObserver mContentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (mDialog != null && mDialog.isShowing() && !is4GDataOnly()) {
                mDialog.dismiss();
            }
        }
    };

    private ContentObserver mObserverForRadioState = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (mDialog != null && mDialog.isShowing() && !isSlot1RadioOn()) {
                mDialog.dismiss();
            }
        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Xlog.d(TAG, "onReceive action = " + action);
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                if (intent.getBooleanExtra("state", false)) {
                    Xlog.d(TAG, "Action stop service");
                    stopSelf(mStartId);
                }
            } else if (action.equals(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED)) {
                if (!isSlot1Inserted()) {
                    Xlog.d(TAG, "Action stop service");
                    stopSelf(mStartId);
                }
            }
        }
    };

}
