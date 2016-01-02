/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
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

package com.mediatek.rcs.message.plugin;

import android.content.Context;
import android.util.Log;
import android.text.SpannableString;

import com.mediatek.common.PluginImpl;
import com.mediatek.mms.ipmessage.IIpEmptyService;
import com.mediatek.mms.ipmessage.IpEmptyReceiver;
import com.mediatek.mms.ipmessage.IpMessagePluginImpl;
import com.mediatek.mms.ipmessage.IpComposeActivity;
import com.mediatek.mms.ipmessage.IpScrollListener;
import com.mediatek.mms.ipmessage.IpSettingListActivity;
import com.mediatek.mms.ipmessage.IpSharePanel;
import com.mediatek.mms.ipmessage.IpMessageListItem;
import com.mediatek.mms.ipmessage.IpMessageItem;
import com.mediatek.mms.ipmessage.IpMessageListAdapter;
import com.mediatek.mms.ipmessage.IpContact;
import com.mediatek.mms.ipmessage.IpDialogModeActivity;
import com.mediatek.mms.ipmessage.IpMessagingNotification;
import com.mediatek.mms.ipmessage.IpMultiDeleteActivity;
import com.mediatek.mms.ipmessage.IpConfig;
import com.mediatek.mms.ipmessage.IpContact;
import com.mediatek.mms.ipmessage.IpConversation;
import com.mediatek.mms.ipmessage.IpConversationList;
import com.mediatek.mms.ipmessage.IpConversationListItem;
import com.mediatek.mms.ipmessage.IpSearchActivity;
import com.mediatek.mms.ipmessage.IpStatusBarSelector;
import com.mediatek.mms.ipmessage.IpSpamMsgReceiver;
import com.mediatek.mms.ipmessage.IpUtils;

import com.mediatek.rcs.common.RCSMessageManager;
import com.mediatek.rcs.common.provider.ThreadMapCache;
import com.mediatek.rcs.message.proxy.RcsProxyReceivers;
import com.mediatek.rcs.message.proxy.RcsProxyServices;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.common.utils.RCSUtils;
import com.mediatek.rcs.message.utils.RcsMessageConfig;
import com.mediatek.rcs.message.utils.RcsMessageUtils;

/**
 * Used to access RCS plugin
 */
 @PluginImpl(interfaceName="com.mediatek.mms.ipmessage.IIpMessagePlugin")
public class RCSMessagePluginExt extends IpMessagePluginImpl {

    private static final String TAG = "RCSMessagePluginExt";

    private Context mPluginContext;
    private static RCSMessagePluginExt sInstance;
    private RcsComposeActivity mComposeActivity;

    public RCSMessagePluginExt(Context context) {
        super(context);
        ContextCacher.setPluginContext(context);
        mPluginContext = context;
        sInstance = this;
    }
    
    public static RCSMessagePluginExt getInstance() {
        return sInstance;
    }

    @Override
    public boolean isActualPlugin() {
        Log.d(TAG, "isActualPlugin entry ");
        return true;
    }
    
    @Override
    public IpMessageListItem getIpMessageListItem() {
        Log.d(TAG, "getIpMessageListItem");
        return new RcsMessageListItem(mPluginContext);
    }
    
    @Override
    public IpMessageItem getIpMessageItem() {
        Log.d(TAG, "getIpMessageItem");
        return new RcsMessageItem();
    }
    
    @Override
    public IpComposeActivity getIpComposeActivity() {
        Log.d(TAG, "getIpComposeActivity");
        mComposeActivity = new RcsComposeActivity(mPluginContext);
        return mComposeActivity;
    }
    
    public IpDialogModeActivity getIpDialogModeActivity() {
        return new RcsDialogModeActivity(mPluginContext);
    }
    @Override
    public IpMessageListAdapter getIpMessageListAdapter() {
        Log.d(TAG, "getIpMessageListAdapter");
        return new RcsMessageListAdapter(mPluginContext);
    }
    
    public IpMessagingNotification getIpMessagingNotification() {
        return new RcsMessagingNotification();
    }
    
    public IpMultiDeleteActivity getIpMultiDeleteActivity() {
        return new RcsMultiDeleteActivity();
    }
    
    public IpSearchActivity getIpSearchActivity() {
        return new RcsSearchActivity();
    }

    public IpStatusBarSelector getIpStatusBarSelector() {
        return new RcsStatusBarSelector();
    }

    public IpConfig getIpConfig() {
        return new RcsMessageConfig(mPluginContext);
    }

//    private static IIpMessagePluginCallback sPluginCallback;
//    public void setIpMessagePluginCallback(IIpMessagePluginCallback callback) {
//        //
//        sPluginCallback = callback;
//    }
//    
//    public static IIpMessagePluginCallback getIpMsgCallback() {
//        return sPluginCallback;
//    }

    RcsMessageSharePanel mSharePanelExt;
    @Override
    public IpSharePanel getIpSharePanel( ) {
        if (mSharePanelExt == null) {
            mSharePanelExt= new RcsMessageSharePanel(mPluginContext);
        }
        mSharePanelExt.setComposer(mComposeActivity);
        return mSharePanelExt;
    }
    
    @Override
    public IpContact getIpContact() {
        return new RcsContact();
    }

    @Override
    public IpConversation getIpConversation() {
        return new RcsConversation();
    }

    @Override
    public IpConversationList getIpConversationList() {
        return new RcsConversationList(mPluginContext);
    }

    @Override
    public IpConversationListItem getIpConversationListItem() {
        return new RcsConversationListItem(mPluginContext);
    }
    
    @Override
    public IpUtils getIpUtils() {
        return new RcsUtilsPlugin(mPluginContext);
    }
    
    @Override
    public IpSettingListActivity getIpSettingListActivity() {
        return new RcsSettingListActivity(mPluginContext);
    }

    public IIpEmptyService getIPEmptyService() {
        Log.d(TAG, "getIPEmptyReceiver");
        return  new RcsProxyServices();
    }

    @Override
    public IpSpamMsgReceiver getIpSpamMsgReceiver() {
        return new RcsSpamMsgReceiver();
    }
    
    @Override
    public IpEmptyReceiver getIpEmptyReceiver() {
        Log.d(TAG, "getIPEmptyReceiver");
        return  new RcsProxyReceivers();
    }

    @Override
    public IpScrollListener getIpScrollListener() {
        Log.d(TAG, "getIpScrollListener");
        return new RcsScrollListener();
    }
}
