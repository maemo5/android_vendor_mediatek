/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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
package com.mediatek.rcs.genericui.ext;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;

import com.mediatek.common.PluginImpl;
import com.mediatek.rcs.genericui.R;
import com.mediatek.settings.ext.DefaultRCSSettings;
import com.mediatek.xlog.Xlog;

import org.gsma.joyn.JoynServiceConfiguration;

import android.telephony.SubscriptionInfo;
import java.util.List;
import android.provider.Settings;


/**
 * Rcs plugin implementation of RCS settings feature.
 */
@PluginImpl(interfaceName = "com.mediatek.settings.ext.IRCSSettings")
public class GeniricRcsSettings extends DefaultRCSSettings {
    private static final String TAG = "GeniricRcsSettings";
    private static final String KEY_RCS_SWITCH = "rcs_switch";
    private static final String INTENT_RCS_ON = "com.mediatek.intent.rcs.stack.LaunchService";
    private static final String INTENT_RCS_OFF = "com.mediatek.intent.rcs.stack.StopService";

    private Context mContext;
    private SwitchPreference mRcsSwitchPref;

    public GeniricRcsSettings(Context context) {
        super();
        mContext = context;
        mContext.setTheme(R.style.SettingsPluginBase);
        Xlog.d(TAG, "GeniricRcsSettings()");
    }

    public void addRCSPreference(Activity activity, PreferenceScreen screen) {
        mRcsSwitchPref = new SwitchPreference(mContext);
        mRcsSwitchPref.setTitle(R.string.rcs_setting_title);
        mRcsSwitchPref.setKey(KEY_RCS_SWITCH);
        mRcsSwitchPref.setOnPreferenceChangeListener(mPreferenceChangeListener);
        screen.addPreference(mRcsSwitchPref);
        boolean isEnable = getRcsState();
        Xlog.d(TAG, "GeniricRcsSettings():" + isEnable);
        mRcsSwitchPref.setChecked(isEnable);
    }

    private OnPreferenceChangeListener mPreferenceChangeListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String key = preference.getKey();
            Xlog.d(TAG, "key=" + key);
            if (KEY_RCS_SWITCH.equals(key)) {
                boolean checked = ((Boolean) newValue).booleanValue();
                if (checked) {
                    Intent intent = new Intent(INTENT_RCS_ON);
                    mContext.sendBroadcast(intent);
                } else {
                    Intent intent = new Intent(INTENT_RCS_OFF);
                    mContext.sendBroadcast(intent);
                }
            }
            return true;
        }
    };

    private boolean getRcsState() {
        return JoynServiceConfiguration.isServiceActivated(mContext);
    }

    public boolean isNeedAskFirstItemForSms() {
		Xlog.d(TAG, "isNeedAskFirstItemForSms:");
		return false;
    }

	public int getDefaultSmsClickContentExt(final List<SubscriptionInfo> subInfoList,int value,int subId){
		Xlog.d(TAG, "getDefaultSmsClickContent:");
        if (value >= 0 && value < subInfoList.size()) {
		    subId = subInfoList.get(value).getSubscriptionId();
	    } else if(value >= subInfoList.size()){
		    subId = (int) Settings.System.SMS_SIM_SETTING_AUTO;
	    } else {
	        Xlog.d(TAG, "value<0");
	    }
		return subId;		
	}

}


