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

package com.mediatek.rcs.incallui.ext;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telecom.Call;
import android.telecom.Call.Details;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;

import com.mediatek.common.PluginImpl;
import com.mediatek.incallui.ext.DefaultRCSeCallButtonExt;
import com.mediatek.incallui.ext.IRCSeCallButtonExt;
import com.mediatek.rcs.incallui.service.CallStatusServicePresenter;
import com.mediatek.rcs.phone.R;
import com.mediatek.xlog.Xlog;

import java.util.HashMap;

//import org.gsma.joyn.JoynServiceListener;
//import org.gsma.joyn.JoynServiceException;
//import org.gsma.joyn.chat.ChatService;

@PluginImpl(interfaceName="com.mediatek.incallui.ext.IRCSeCallButtonExt")
public class RCSCallButtonExtension extends DefaultRCSeCallButtonExt {

    private static final String TAG = "RCSCallButtonExtension";
    CallStatusServicePresenter mCallStatusServicePresenter;
    private Context mContext;

    private Context fContext;
    //private ChatService mChatApi;
    //private boolean mRegistrationStatus;
    private int mMenuId;
    private Menu mMenu;

    public RCSCallButtonExtension (Context context) {
        super();

        //mChatApi = new ChatService(context, new ChatServiceListener());
        //mChatApi.connect();

        mContext = context;
        //mRegistrationStatus = true;
        mMenuId = 0x100;
    }

    /**
      * Interface to add menu item to overflow menu in order to send message
      *
      * @param context the incallactivity context
      * @param menu the incallactivity menu
      */
    @Override
    public void configureOverflowMenu(Context context, Menu menu){
        Xlog.i(TAG, "configureOverflowMenu.");
        mMenu = menu;
        fContext = context;
        mCallStatusServicePresenter = CallStatusServicePresenter.getInstance(mContext);
        boolean showMenu = mCallStatusServicePresenter.isNeedShowMenuItem();

        //try {
            //mRegistrationStatus = mChatApi.isServiceRegistered();
            //Xlog.i(TAG, "mRegistrationStatus init state: " + mRegistrationStatus);
        //} catch (JoynServiceException e) {
            //e.printStackTrace();
        //}

        if (showMenu /* && mRegistrationStatus*/) {  
            addOverflowMenu(menu);
        } else {
            removeOverflowMenu(menu);
        }
    }

    /**
      * Interface to updated CallMap to service presenter to control the state of calls
      *
      * @param call the incallactivity call
      * @param callMap the incallactivity callMap
      */
    @Override
    public void onStateChange(Call call, HashMap<String, Call> callMap) {
        Xlog.i(TAG, "onStateChange.");
        mCallStatusServicePresenter = CallStatusServicePresenter.getInstance(mContext);
        mCallStatusServicePresenter.onCallStatusChange(callMap);
    }

    private void addOverflowMenu(Menu menu) {
        Xlog.i(TAG, "addOverflowMenu.");
        MenuItem item = menu.findItem(mMenuId);
        if (item == null) {
            MenuItem speedDialMenu = menu.add(Menu.NONE, mMenuId, 0, mContext.getString(R.string.call_send_message));
            speedDialMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    Call call = mCallStatusServicePresenter.onMenuItemSelected();
                    if (call != null) {
                        Details details = call.getDetails();
                        String scheme = details.getHandle().getScheme();
                        String uriString = details.getHandle().getSchemeSpecificPart();
                        launchMessage(uriString);
                        //launchTestApp(uriString);
                    }
                    return true;
                }
            });
        }
    }

    private void removeOverflowMenu(Menu menu) {
        Xlog.i(TAG, "removeOverflowMenu.");
        menu.removeItem(mMenuId);
    }

    private void launchMessage(String number) {
        Xlog.i(TAG, "launchMessage, number = " + number);
        //Intent intent = new Intent();
        //intent.setAction(Intent.ACTION_SENDTO);
        Uri uri = Uri.parse("smsto:" + number);
        //intent.putExtra("address", number);
        Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
        intent.setClassName("com.android.mms", "com.android.mms.ui.ComposeMessageActivity");
        intent.putExtra("rcsmode", 1);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    private void launchTestApp(String number) {
        Xlog.i(TAG, "launchTestApp, number = " + number);
        final Intent intent = new Intent();
        intent.setClassName("com.mediatek.rcs.phone", "com.mediatek.rcs.incallui.test.TestActivity");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    /*
    private class ChatServiceListener implements JoynServiceListener {
        @Override
        public void onServiceConnected() {
            Xlog.d(TAG, "ChatService onServiceConnected entry#" + this);
            mCallStatusServicePresenter = CallStatusServicePresenter.getInstance(mContext);
            boolean showMenu = mCallStatusServicePresenter.isNeedShowMenuItem();
            try {
                mRegistrationStatus = mChatApi.isServiceRegistered();
                Xlog.i(TAG, "mRegistrationStatus init state: " + mRegistrationStatus);
                if (mMenu != null && mRegistrationStatus && showMenu) {
                    addOverflowMenu(mMenu);
                }
            } catch (JoynServiceException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(int error) {
            Xlog.d(TAG, "ChatService onServiceDisconnected");
            if (mMenu != null) {
                removeOverflowMenu(mMenu);
            }
        }
    }*/
}