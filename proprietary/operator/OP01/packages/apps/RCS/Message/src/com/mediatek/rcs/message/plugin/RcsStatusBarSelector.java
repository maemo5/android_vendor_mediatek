package com.mediatek.rcs.message.plugin;

import com.mediatek.mms.ipmessage.IpStatusBarSelector;
import com.mediatek.widget.CustomAccountRemoteViews.AccountInfo;
import java.util.ArrayList;

public class RcsStatusBarSelector extends IpStatusBarSelector {

	public boolean onIpRefreshData(ArrayList<AccountInfo> data) {
		data.remove(0);
        return true;
    }
}
