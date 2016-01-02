package com.orangelabs.rcs.service;

import com.orangelabs.rcs.utils.logger.Logger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Device boot event receiver: automatically starts the RCS service
 * 
 * @author jexa7410
 */
public class DataSimChanged extends BroadcastReceiver {
    private static Logger logger = Logger.getLogger(DeviceBoot.class.getSimpleName());
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (logger.isActivated())
            logger.debug("Data Sim Changed");
        LauncherUtils.stopRcsService(context);
        LauncherUtils.launchRcsService(context, true, false);
    }
}
