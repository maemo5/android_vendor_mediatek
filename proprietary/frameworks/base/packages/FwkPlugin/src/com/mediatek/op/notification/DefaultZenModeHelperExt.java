package com.mediatek.op.notification;

import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.common.notification.IZenModeHelperExt;

/* Vanzo:yuecaili on: Tue, 21 Jul 2015 12:03:38 +0800
 * implement #116054 VANZO_FEATURE_ALARM_CAN_RING_IN_SILENT_MODE
 */
import com.android.featureoption.FeatureOption;
// End of Vanzo:yuecaili
/**
 * Customize the zen mode helper, default implementation.
 *
 */
@PluginImpl(interfaceName = "com.mediatek.common.notification.IZenModeHelperExt")
public class DefaultZenModeHelperExt implements IZenModeHelperExt {
    private static final String TAG = "DefaultZenModeHelperExt";
    @Override
    public boolean customizeMuteAlarm(boolean muteAlarm) {
        Log.d(TAG, "customizeMuteAlarm, muteAlarm = " + muteAlarm);
/* Vanzo:yuecaili on: Tue, 21 Jul 2015 12:04:33 +0800
 * implement #116054 VANZO_FEATURE_ALARM_CAN_RING_IN_SILENT_MODE
        return muteAlarm;
 */
        if (FeatureOption.VANZO_FEATURE_ALARM_CAN_RING_IN_SILENT_MODE) {
            return false;
        } else {
            return muteAlarm;
        }
// End of Vanzo:yuecaili
    }
}
