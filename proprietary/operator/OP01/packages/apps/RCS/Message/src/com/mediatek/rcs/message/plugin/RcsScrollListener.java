package com.mediatek.rcs.message.plugin;


import android.content.Context;
import android.util.Log;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.HeaderViewListAdapter;

import com.mediatek.mms.ipmessage.IpScrollListener;

public class RcsScrollListener extends IpScrollListener {
    private static String TAG = "RcseScrollListener";

    public boolean onIpScroll(Context context, AbsListView view, int firstVisibleItem,
            int visibleItemCount, int totalItemCount, long threadId) {
//        Logger.w(TAG, "onIpScroll threadId = " + threadId);
//        IpMessageChatManger.getInstance(context).onScroll(view, firstVisibleItem, visibleItemCount,
//                totalItemCount, threadId);
//        return true;
        return super.onIpScroll(context, view, firstVisibleItem, visibleItemCount, totalItemCount, threadId);
    }

    @Override
    public Adapter onIpScrollStateChanged(AbsListView view) {
        Log.d(TAG, "onIpQueryComplete ");
        if ((view.getAdapter()) instanceof HeaderViewListAdapter) {
        	Log.d(TAG, "header adapter, unwrapp.");
            HeaderViewListAdapter wrappedAdapter = (HeaderViewListAdapter) view.getAdapter();
            return wrappedAdapter.getWrappedAdapter();
        } else {
        	Log.d(TAG, "return null.");
            return null;
        }
    }
}
