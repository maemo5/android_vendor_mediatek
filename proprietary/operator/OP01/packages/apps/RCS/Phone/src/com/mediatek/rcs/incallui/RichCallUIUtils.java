/*
* This software/firmware and related documentation ("MediaTek Software") are
* protected under relevant copyright laws. The information contained herein
* is confidential and proprietary to MediaTek Inc. and/or its licensors.
* Without the prior written permission of MediaTek inc. and/or its licensors,
* any reproduction, modification, use or disclosure of MediaTek Software,
* and information contained herein, in whole or in part, shall be strictly prohibited.
*/
/* MediaTek Inc. (C) 2014. All rights reserved.
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

package com.mediatek.rcs.incallui;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemProperties;
import android.telecom.Call;
import android.telecom.Call.Details;
import android.telecom.CallProperties;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;

import com.mediatek.telecom.TelecomManagerEx;
import com.mediatek.xlog.Xlog;

public class RichCallUIUtils {

    private static final String TAG = "RichCallUIUtils";
    private static RichCallUIUtils sRichCallUIUtils;

    //public static final boolean MTK_IMS_SUPPORT = SystemProperties.get("ro.mtk_ims_support")
            //.equals("1");
    //public static final boolean MTK_VOLTE_SUPPORT = SystemProperties.get("ro.mtk_volte_support")
            //.equals("1");
    private RichCallUIUtils() {
    }

    public static RichCallUIUtils getInstance() {
        if (sRichCallUIUtils == null) {
            sRichCallUIUtils = new RichCallUIUtils();
        }
        return sRichCallUIUtils;
    }
    
    public String parseNumber(Call call) {
        Details details = call.getDetails();
        if (details != null && details.getHandle() != null) {
            String scheme = details.getHandle().getScheme();
            String uriString = details.getHandle().getSchemeSpecificPart();
            Xlog.i(TAG, "parseNumber, number = " + uriString);

            if ("tel".equals(scheme)) {
                return uriString;
            }
        }
        return "";
    }

    public boolean isVideoCall(Call call, Context cnx) {
        if (call == null) {
            return false;
        }

        return isVideoEnabled(cnx) && 
                VideoProfile.VideoState.isBidirectional(call.getDetails().getVideoState());
    }

    private boolean isVideoEnabled(Context context) {
        TelecomManager telecommMgr = (TelecomManager)
                context.getSystemService(Context.TELECOM_SERVICE);
        if (telecommMgr == null) {
            return false;
        }
        return false;
    }

    public boolean isConferenceCall(Call call) {
        return hasProperty(CallProperties.CONFERENCE, call);
    }

    private boolean hasProperty(int property, Call call) {
        return property == (property & call.getDetails().getCallProperties());
    }

/*
    private boolean isVolteConferenceCall(Call call) {
        if (MTK_IMS_SUPPORT && MTK_VOLTE_SUPPORT) {
            if (call.getChildren() != null && call.getChildren().size() > 2) {
                return true;
            }

            int callMode = TelecomManagerEx.VolteCallMode.VALUE_NOT_SET;
            Details details = call.getDetails();
            if (details != null) {
                Bundle bundle = details.getExtras();
                if (bundle != null && bundle.containsKey(TelecomManagerEx.EXTRA_VOLTE_CALL_MODE)) {
                    Object value = bundle.get(TelecomManagerEx.EXTRA_VOLTE_CALL_MODE);
                    if (value instanceof Integer){
                        callMode = (Integer) value;
                    }
                }
            }

            if (callMode == TelecomManagerEx.VolteCallMode.IMS_VOICE_CONF) {
                return true;
            }
        }
        return false;
    }
    */
}
