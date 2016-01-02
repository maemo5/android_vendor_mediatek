/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2014. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.selfregister;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.telephony.CellLocation;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.util.Base64;
import android.util.Log;

import com.android.internal.telephony.IccCardConstants.CardType;
import com.android.internal.telephony.TelephonyIntents;
import com.mediatek.common.dm.DmAgent;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Responsible for compose the register message.
 */
public class RegisterMessage {
    private static final String SUB_TAG = Const.TAG + "RegisterMessage";

    /// M: Fields of register message.
    private static final String FIELD_REG_VERSION = "REGVER";
    private static final String FIELD_MEID = "MEID";
    private static final String FIELD_MODEL = "MODELSMS";
    private static final String FIELD_SW_VERSION = "SWVER";
    private static final String FIELD_IMSI_CDMA = "SIM1CDMAIMSI";
    private static final String FIELD_UE_TYPE = "UETYPE";
    private static final String FIELD_ICCID = "SIM1ICCID";
    private static final String FIELD_IMSI_LTE = "SIM1LTEIMSI";
    private static final String FIELD_SIM_TYPE = "SIM1TYPE";
    private static final String FIELD_IMSI_2 = "SIM2IMSI";
    private static final String FIELD_SID = "SID";
    private static final String FIELD_NID = "NID";
    private static final String FIELD_MACID = "MACID";
    private static final String VALUE_REG_VERSION = "1.0";
    private static final int VALUE_UE_TYPE = 1;
    private static final String VALUE_EMPTY = "";

    private static final String PROPERTY_CUSTOM_VERSION = "ro.mediatek.version.release";

    private RegisterService mRegisterService;

    /**
     * Constructor method.
     * @param service The service which invokes this class.
     */
    public RegisterMessage(RegisterService service) {
        mRegisterService = service;
    }

    /**
     * Collect the data of register message.
     * @return String The register message.
     */
    public String getRegisterMessage() {
        Log.d(SUB_TAG, "Enter getRegisterMessage()...");
        String message = generateMessageData();
        Log.d(SUB_TAG, "Generate data: [" + message + "]");
        return encodeBase64(message);
    }

    private String generateMessageData() {
        TelephonyManager telephonyManager = mRegisterService.getTelephonyManager();
        if (telephonyManager == null) {
            Log.e(SUB_TAG, "generateMessageData() error: telephonyManager is null!");
            return null;
        }

        JSONObject data = new JSONObject();
        try {
            data.put(FIELD_REG_VERSION, VALUE_REG_VERSION);
            data.put(FIELD_MEID, getMEID());
            data.put(FIELD_MODEL, getModel());
            data.put(FIELD_SW_VERSION, getSoftwareVersion());
            data.put(FIELD_UE_TYPE, VALUE_UE_TYPE);
            data.put(FIELD_MACID, getMACID());

            if (telephonyManager.hasIccCard(0)) {
                data.put(FIELD_IMSI_CDMA, getIMSI(0)[0]);
                data.put(FIELD_ICCID, mRegisterService.getIccIDFromCard()[0]);
                data.put(FIELD_IMSI_LTE, getIMSI(0)[1]);
                data.put(FIELD_SIM_TYPE, getSIMType());
                data.put(FIELD_SID, getCDMASI(telephonyManager));
                data.put(FIELD_NID, getCDMANI(telephonyManager));
            } else {
                data.put(FIELD_IMSI_CDMA, VALUE_EMPTY);
                data.put(FIELD_ICCID, VALUE_EMPTY);
                data.put(FIELD_IMSI_LTE, VALUE_EMPTY);
                data.put(FIELD_SIM_TYPE, VALUE_EMPTY);
                data.put(FIELD_SID, VALUE_EMPTY);
                data.put(FIELD_NID, VALUE_EMPTY);
            }

            if (telephonyManager.hasIccCard(1)) {
                data.put(FIELD_IMSI_2, getIMSI(1)[0]);
            } else {
                data.put(FIELD_IMSI_2, VALUE_EMPTY);
            }
        } catch (JSONException e) {
            Log.e(SUB_TAG, "generateMessageData(), JSONException!");
            e.printStackTrace();
        }

        return data.toString();
    }

    /// M: Return the MEID of CDMA.
    private String getMEID() {
        TelephonyManager telephonyManager = mRegisterService.getTelephonyManager();
        if (telephonyManager == null) {
            Log.e(SUB_TAG, "Get MEID error: telephonyManager is null!");
            return null;
        }

        String meid = telephonyManager.getDeviceId(0);
        Log.v(SUB_TAG, "getMEID(), MEID: " + meid);

        if (meid.length() != Const.MEID_LENGTH) {
            Log.e(SUB_TAG, "Get invalid device ID! Length: " + meid.length());
            return null;
        }

        return meid;
    }

    private String getModel() {
        String globleModel = Build.MODEL;
        int index = globleModel.indexOf(" ");
        if (index < 0 || index == globleModel.length()) {
            Log.w(SUB_TAG, "Model in Build.MODEL may be error!!, globleModel = " + globleModel);
            if (globleModel.length() > Const.MODEL_MAX_LENGTH) {
                globleModel = globleModel.substring(0, Const.MODEL_MAX_LENGTH);
            }
            return globleModel;
        }

        String manufacturer = globleModel.substring(0, index);
        Log.d(SUB_TAG, "manufacturer:" + manufacturer);
        if (manufacturer.length() > Const.MANUFACTURE_MAX_LENGTH) {
            Log.w(SUB_TAG, "Manufacturer length > " + Const.MANUFACTURE_MAX_LENGTH + ", cut it!");
            manufacturer = manufacturer.substring(0, Const.MANUFACTURE_MAX_LENGTH);
        }

        String model = globleModel.substring(index + 1, globleModel.length());
        Log.d(SUB_TAG, "model:" + model);
        model = model.replaceAll("-", " ");
        if (model.indexOf(manufacturer) != -1) {
            model = model.replaceFirst(manufacturer, "");
        }

        String result = manufacturer + "-" + model;
        if (result.length() > Const.MODEL_MAX_LENGTH) {
            Log.w(SUB_TAG, "Model length > " + Const.MODEL_MAX_LENGTH + ", cut it!");
            result = result.substring(0, Const.MODEL_MAX_LENGTH);
        }

        return result;
    }

    private String getSoftwareVersion() {
        String result = SystemProperties.get(PROPERTY_CUSTOM_VERSION,
                Const.SOFTWARE_VERSION_DEFAULT);
        if (result.length() > Const.SOFTWARE_VERSION_MAX_LENGTH) {
            Log.w(SUB_TAG, "Software version length > " + Const.SOFTWARE_VERSION_MAX_LENGTH
                    + ", cut it!");
            result = result.substring(0, Const.SOFTWARE_VERSION_MAX_LENGTH);
        }

        return result;
    }

    /// M: CT SIM in Slot0 may returns two IMSIs.
    ///    Index 0 returns CDMA IMSI, index 1 returns LTE IMSI.
    private String[] getIMSI(int slotId) {
        TelephonyManager telephonyManager = mRegisterService.getTelephonyManager();
        if (telephonyManager == null) {
            Log.e(SUB_TAG, "Get IMSI error: telephonyManager is null!");
            return null;
        }

        String imsiArray[] = new String[2];
        int[] subId = SubscriptionManager.getSubId(slotId);

        if (slotId == 0) {
            /// M: Special way for Slot 1.
            /// Register receiver to monitor broadcast from framework.
            /// Only for SVLTE.
            Intent imsiIntent = mRegisterService.registerReceiver(null,
                    new IntentFilter(TelephonyIntents.ACTION_CDMA_CARD_IMSI));

            if (imsiIntent != null) {
                Log.d(SUB_TAG, "getIMSI(), Get IMSI from broadcast.");
                /// M: CDMA IMSI
                imsiArray[0] = imsiIntent
                        .getStringExtra(TelephonyIntents.INTENT_KEY_CDMA_CARD_CSIM_IMSI);
                /// M: LTE IMSI
                imsiArray[1] = imsiIntent
                        .getStringExtra(TelephonyIntents.INTENT_KEY_CDMA_CARD_USIM_IMSI);
            } else {
                /// M: 3G card
                Log.d(SUB_TAG, "getIMSI(), LTE IMSI is null.");
                imsiArray[0] = telephonyManager.getSubscriberId(subId[0]);
                imsiArray[1] = VALUE_EMPTY;
            }

        } else {
            imsiArray[0] = telephonyManager.getSubscriberId(subId[0]);
            imsiArray[1] = null;
        }

        return imsiArray;
    }

    /// M: Get card type of SIM 1. 1 for ICC type, 2 for UICC type.
    private int getSIMType() {
        int type = 0;

        if (mRegisterService.mPhoneValues.containsKey(RegisterService.KEY_CDMA_CARD_TYPE)) {
            CardType cardType = (CardType) mRegisterService.mPhoneValues
                    .get(RegisterService.KEY_CDMA_CARD_TYPE);

            switch (cardType) {
            case SIM_CARD:
            case UIM_CARD:
            case UIM_SIM_CARD:
            case CT_3G_UIM_CARD:
            case CT_UIM_SIM_CARD:
                type = 1;
                break;
            case CT_4G_UICC_CARD:
                type = 2;
                break;
            default:
                break;
          }
      }

        return type;
    }

    private int getCDMASI(TelephonyManager telephonyManager) {
        if (!mRegisterService.mPhoneValues.containsKey(RegisterService.KEY_SI)) {
            if (telephonyManager == null) {
                Log.e(SUB_TAG, "Get SI error! telephonyManager is null!");
                return -1;
            }

            int si = -1;
            CellLocation location = telephonyManager.getCellLocation();
            if (location instanceof CdmaCellLocation) {
                si = ((CdmaCellLocation) location).getSystemId();
                Log.d(SUB_TAG, "Get SI from TelephonyManager. SI: " + si);
                return si;
            }

            Log.w(SUB_TAG, "Get SI error! Get SI from SharedPreference.");
            SharedPreferences preference = PreferenceManager
                    .getDefaultSharedPreferences(mRegisterService);
            si = preference.getInt(Const.PRE_KEY_SI, -1);
            return si;
        }

        return (Integer) mRegisterService.mPhoneValues.get(RegisterService.KEY_SI);
    }

    private int getCDMANI(TelephonyManager telephonyManager) {
        if (!mRegisterService.mPhoneValues.containsKey(RegisterService.KEY_NI)) {
            if (telephonyManager == null) {
                Log.e(SUB_TAG, "Get NI error! telephonyManager is null!");
                return -1;
            }

            int ni = -1;
            CellLocation location = telephonyManager.getCellLocation();
            if (location instanceof CdmaCellLocation) {
                ni = ((CdmaCellLocation) location).getNetworkId();
                Log.d(SUB_TAG, "Get NI from TelephonyManager. NI: " + ni);
                return ni;
            }

            Log.w(SUB_TAG, "Get NI error! Get from SharedPreference.");
            SharedPreferences preference = PreferenceManager
                    .getDefaultSharedPreferences(mRegisterService);
            ni = preference.getInt(Const.PRE_KEY_NI, -1);
            return ni;
        }

        return (Integer) mRegisterService.mPhoneValues.get(RegisterService.KEY_NI);
    }

    private String getMACID() {
        DmAgent dmAgent = mRegisterService.getDmAgent();
        if (dmAgent == null) {
            Log.e(SUB_TAG, "Get MACID error: dmAgent is null!");
            return null;
        }

        StringBuilder macAddress = new StringBuilder();
        try {
            byte[] macAddr = dmAgent.getMacAddr();

            /// M: Convert to Hex string and split to array.
            char[] macArray = bytesToHexString(macAddr).toCharArray();

            /// M: Add ":"
            for (int i = 0; i < macAddr.length; i++) {
                macAddress.append(macArray[i * 2]);
                macAddress.append(macArray[i * 2 + 1]);
                if (i != macAddr.length - 1) {
                    String str = new String(":");
                    macAddress.append(str);
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        Log.v(SUB_TAG, "MAC: " + macAddress.toString());
        return macAddress.toString();
    }

    private String encodeBase64(String data) {
        byte[] encodeByte = Base64.encode(data.getBytes(), Base64.DEFAULT);
        return new String(encodeByte);
    }

    private String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }
}
