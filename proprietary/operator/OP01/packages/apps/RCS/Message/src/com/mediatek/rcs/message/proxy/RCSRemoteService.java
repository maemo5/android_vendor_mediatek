package com.mediatek.rcs.message.proxy;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.mediatek.rcs.common.utils.Logger;
import com.mediatek.rcs.message.chat.RCSChatServiceImpl;
import com.mediatek.common.PluginImpl;

import com.mediatek.mms.ipmessage.IpRemoteService;

@PluginImpl(interfaceName="com.mediatek.mms.ipmessage.IIpRemoteService")
public class RCSRemoteService extends IpRemoteService {
    public static final String TAG = "RCSRemoteService";

    private static Context sPlugInContext;
    private static Service sService;
    private static RCSChatServiceImpl sServiceImpl = null;

    public RCSRemoteService(Context plugInContext) {
        super(plugInContext);
        sPlugInContext = plugInContext;
        Logger.d(TAG, "Constructor");
    }

    public static Intent getServiceIntent() {
        Intent intent = new Intent("com.mediatek.rcs.RemoteService");
        intent.setPackage("com.android.mms");
        return intent;
    }

    public static Context getPlugInConext() {
        return sPlugInContext;
    }

    public void onCreate(Service service) {
        Logger.d(TAG, "onCreate");
        sService = service;
        if (sServiceImpl == null) {
            sServiceImpl = new RCSChatServiceImpl(sService);
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.d(TAG, "onStartCommand: " + intent.getAction());
        return 0;
    }

    public void onDestroy() {
        Logger.d(TAG, "onDestroy");
        sServiceImpl.onDestroy();
        sServiceImpl = null;
    }


    public IBinder onBind(Intent intent) {
        Logger.d(TAG, "onBind: " + intent.getAction());
        return sServiceImpl;
    }

    public boolean onUnbind(Intent intent) {
        Logger.d(TAG, "onUnbind: " + intent.getAction());
        return false;
    }
}
