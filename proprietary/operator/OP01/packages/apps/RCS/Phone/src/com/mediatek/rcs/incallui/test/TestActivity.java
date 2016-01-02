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

package com.mediatek.rcs.incallui.test;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.view.View;
import android.widget.TextView;

import com.mediatek.rcs.phone.R;
import com.mediatek.services.rcs.phone.ICallStatusService;
import com.mediatek.services.rcs.phone.IServiceMessageCallback;
import com.mediatek.xlog.Xlog;

import java.util.HashMap;
import java.util.Iterator;
public class TestActivity extends Activity {
    public static final String TAG = "TestActivity";

    private ICallStatusService mCallStatusService;
    private ServiceConnection mConnection = null;

    private static final int STATUS_ID_NAME = 1;
    private static final int STATUS_ID_STATUS = 2;
    private static final int STATUS_ID_TIME = 3;

    private final Object mServiceAndQueueLock = new Object();
    private static final int MSG_UPDATE_STATUS = 1;

    private TextView mName;
    private TextView mStatus;
    private TextView mTime;

    private MainHandler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Xlog.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mName = (TextView) findViewById(R.id.nameTextView);
        mStatus = (TextView) findViewById(R.id.statusTextView);
        mTime = (TextView) findViewById(R.id.timeTextView);

        if (mHandler == null) {
            mHandler = new MainHandler();
        }
    }

    @Override
    protected void onStart() {
        Xlog.i(TAG, "onStart");
        super.onStart();
    }

    @Override
    protected void onResume() {
        Xlog.i(TAG, "onResume");
        super.onResume();
        //registerCallback();
        setupCallStatusServiceConnection();
    }

    @Override
    protected void onPause() {
        Xlog.i(TAG, "onPause");
        super.onPause();
        unregisterCallback();
    }

    @Override
    public void onStop() {
        Xlog.i(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Xlog.i(TAG, "onDestroy");
        super.onDestroy();
        unbind();
    }

    private final IServiceMessageCallback.Stub mServiceMessageCallback = new IServiceMessageCallback.Stub() {
        @Override
        public void updateMsgStatus(String name, String status, String time) {
            try {
                updatedMessageInfo(name, status, time);
                Xlog.i(TAG, "updateMsgStatus");
            } catch (Exception e) {
                Xlog.e(TAG, "Error updateMsgStatus", e);
            }
        }

        @Override
        public void stopfromClient() {
            try {
                unbind();
                Xlog.i(TAG, "stopfromClient");
            } catch (Exception e) {
                Xlog.e(TAG, "Error stopfromClient", e);
            }
        }
    };

    private void updatedMessageInfo(String name, String status, String time) {
        Xlog.i(TAG, "updatedMessageInfo.");

        HashMap<Integer, String> map = new HashMap<Integer, String>();
        map.put(new Integer(1), name);
        map.put(new Integer(2), status);
        map.put(new Integer(3), time);
        mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_STATUS, map));
    }

    private void unbind() {
        Xlog.i(TAG, "unbind.");
        if (mCallStatusService != null) {
            unregisterCallback();
            unbindService(mConnection);
            mCallStatusService = null;
        }
        mConnection = null;
    }

    private class CallStatusServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName name, IBinder service) {
            Xlog.i(TAG, "onServiceConnected.");
            if (mCallStatusService != null) {
                Xlog.d(TAG, "Service alreay connected, service = " + mCallStatusService);
                return;
            }
            onCallStatusServiceConnected(ICallStatusService.Stub.asInterface(service));
        }

        public void onServiceDisconnected(ComponentName name) {
            Xlog.i(TAG, "onServiceDisconnected.");
            if (mCallStatusService != null) {
                unregisterCallback();
                unbindService(mConnection);
                mCallStatusService = null;
            }
            mConnection = null;
        }
    }

    private void onCallStatusServiceConnected(ICallStatusService callstatusService) {
        mCallStatusService = callstatusService;
        registerCallback();
    }

    private Intent getCallStatusServiceIntent() {
        Xlog.i(TAG, "getCallStatusServiceIntent.");
        final Intent intent = new Intent(ICallStatusService.class.getName());
        final ComponentName component = new ComponentName("com.mediatek.rcs.phone",
                                                    "com.mediatek.rcs.incallui.service.CallStatusService");
        intent.setComponent(component);
        return intent;
    }

    private void setupCallStatusServiceConnection() {
        Xlog.i(TAG, "setupCallStatusServiceConnection.");
        synchronized (mServiceAndQueueLock) {
            if (mCallStatusService == null || mConnection == null) {
                mConnection = new CallStatusServiceConnection();
                boolean failedConnection = false;

                Intent intent = getCallStatusServiceIntent();
                if (!bindService(intent, mConnection,
                        Context.BIND_AUTO_CREATE)) {
                    Xlog.d(TAG, "Bind service failed!");
                    mConnection = null;
                    failedConnection = true;
                } else {
                    Xlog.d(TAG, "Bind service successfully!");
                }
            } else {
                Xlog.d(TAG, "Alreay bind service!");
            }
        }
    }

    private void registerCallback() {
      Xlog.d(TAG, "registerCallback.");
        try {
            synchronized (mServiceAndQueueLock) {
                if (mCallStatusService !=  null) {
                    mCallStatusService.registerMessageCallback(mServiceMessageCallback);
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();            
        }
    }

    private void unregisterCallback() {
        Xlog.d(TAG, "unregisterCallback.");
        try {
            synchronized (mServiceAndQueueLock) {
                if (mCallStatusService != null) {
                    mCallStatusService.unregisterMessageCallback(mServiceMessageCallback);
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private class MainHandler extends Handler{
        MainHandler() {
            super(getApplicationContext().getMainLooper(), null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            Xlog.i(TAG, "handleMessage:" + msg);
            executeMessage(msg);
        }
    }

    private void executeMessage(Message msg) {
        switch(msg.what) {
            case MSG_UPDATE_STATUS:
                handleMessageUpdated((HashMap<Integer, String>)msg.obj);
                break;
            default:
                break;
        }
    }

    private void handleMessageUpdated(HashMap<Integer, String> map) {
        String name = "";
        String status = "";
        String time = "";

        Iterator iterator = map.keySet().iterator();
        while(iterator.hasNext()) {
            Integer integer = (Integer) iterator.next();
            String string = (String) map.get(integer);
            int value = integer.intValue();
            Xlog.i(TAG, "value = " + value);
            if (value == STATUS_ID_NAME) {
                name = string;
            } else if (value == STATUS_ID_STATUS) {
                status = string;
            } else if (value == STATUS_ID_TIME){
                time = string;
            } else {
                Xlog.d(TAG, "Not any valid view id");
            }
        }

        mName.setText(name);
        mStatus.setText(status);
        mTime.setText(time);
    }

}
