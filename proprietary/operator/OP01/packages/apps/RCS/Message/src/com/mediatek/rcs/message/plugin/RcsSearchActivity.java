package com.mediatek.rcs.message.plugin;


import android.widget.TextView;
import com.mediatek.mms.ipmessage.IpSearchActivity;
import com.mediatek.rcs.common.provider.ThreadMapCache;
import com.mediatek.rcs.common.provider.ThreadMapCache.MapInfo;

public class RcsSearchActivity extends IpSearchActivity {

	@Override
    public boolean onIpBindView(TextView textTitle, long mThreadId) {
	    MapInfo mapInfo = ThreadMapCache.getInstance().getInfoByThreadId(mThreadId);
	    if (mapInfo != null && mapInfo.getSubject() != null) {
	        textTitle.setText(mapInfo.getSubject());
	        return true;
	    }
        return false;
    }
}
