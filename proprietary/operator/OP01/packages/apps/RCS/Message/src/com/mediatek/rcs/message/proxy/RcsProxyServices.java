package com.mediatek.rcs.message.proxy;

import com.mediatek.mms.ipmessage.IpEmptyService;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class RcsProxyServices extends IpEmptyService {
    private final static String TAG = "mtk80999";
    private static final String SERVICE_ACTION = "com.mediatek.rcs.EmptyService";
    private static final String SERVICE_PACKAGE_NAME = "com.android.mms";
    private static final String TAG_SERVICE = "service";
    private static final String SERVICE_MESSAGE_SENDER = "servcie_IMessageSender";
    
    private Service mService;
    private RcsMessageSender mRcsMessageSender;


    public void onCreate(Service service){
        mService = service;
        Log.d(TAG, "onCreate: " + service);
//        mService = service;
        mRcsMessageSender = RcsMessageSender.getInstance(service);
    }
    
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.d(TAG, "onStartCommand");
        return 0;
    }

    public void onDestroy() {
        Log.d(TAG, "onDestroy");
    }

    public IBinder onBind(Intent intent){
        String service = intent.getStringExtra(TAG_SERVICE);
        Log.d(TAG, "onBind: intent service = " + service);
        if (service == null) {
            return null;
        }
        if (service.equals(SERVICE_MESSAGE_SENDER)) {
            return mRcsMessageSender;
        } else {
            return null;
        }
    }

    public void onLowMemory() {
        Log.d(TAG, "onLowMemory");
    }

    public void onRebind(Intent intent) {
        Log.d(TAG, "onRebind: intent = " + intent);
    }

    public boolean onUnbind(Intent intent){
        Log.d(TAG, "onUnbind, intent = " + intent);
        return false;
    }

}
