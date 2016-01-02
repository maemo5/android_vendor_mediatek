/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.rcs.pam.activities;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CursorAdapter;
import android.widget.ListView;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.PAService;
import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.provider.PAContract.MessageColumns;


/**
 * The back-end data adapter of a message list.
 */
public class PaMessageListAdapter extends CursorAdapter {
    private static final String TAG = "PA/PaMessageListAdapter";
    private static final boolean LOCAL_LOGV = false;   
    

    static final String[] PROJECTION = new String[] {
        MessageColumns._ID,
        MessageColumns.UUID,
        MessageColumns.SOURCE_ID,
        MessageColumns.SOURCE_TABLE,
        MessageColumns.CHAT_ID,
        MessageColumns.ACCOUNT_ID,
        MessageColumns.TYPE,
        MessageColumns.TIMESTAMP,
        MessageColumns.CREATE_TIME,
        MessageColumns.SMS_DIGEST, 
        MessageColumns.TEXT, 
        MessageColumns.DIRECTION,
        MessageColumns.FORWARDABLE,
        MessageColumns.STATUS,
        MessageColumns.DATA1, 
        MessageColumns.DATA2, 
        MessageColumns.DATA3,
        MessageColumns.DATA4, 
        MessageColumns.DATA5
    };

    // The indexes of the default columns which must be consistent
    // with above PROJECTION.

    static final int COLUMN_ID                  = 0;
    static final int COLUMN_UUID                = 1;
    static final int COLUMN_SOURCE_ID           = 2;
    static final int COLUMN_SOURCE_TABLE        = 3;
    static final int COLUMN_CHAT_ID             = 4;
    static final int COLUMN_ACCOUNT_ID          = 5;
    static final int COLUMN_MSG_TYPE            = 6;
    static final int COLUMN_TIMESTAMP           = 7;
    static final int COLUMN_CREATE_TIMESTAMP    = 8;
    static final int COLUMN_SMS_DIGEST          = 9;
    static final int COLUMN_TEXT                = 10;
    static final int COLUMN_DIRECTION           = 11;
    static final int COLUMN_FORWARDABLE         = 12;
    static final int COLUMN_STATUS              = 13;
    static final int COLUMN_DATA1               = 14;
    static final int COLUMN_DATA2               = 15;
    static final int COLUMN_DATA3               = 16;
    static final int COLUMN_DATA4               = 17;
    static final int COLUMN_DATA5               = 18;

    private static final int CACHE_SIZE         = 50;

    protected LayoutInflater mInflater;
    private final MessageItemCache mMessageItemCache;
    private final ColumnsMap mColumnsMap;
    private OnDataSetChangedListener mOnDataSetChangedListener;
    private Handler mMsgListItemHandler;
    private Pattern mHighlight;
    private Context mContext;
    private PAService mPAService;
    
    private float mTextSize = 0;
    
    //<ReqId, msgID>
    private HashMap<Long, DownloadInfo> mDownloadMap = new HashMap<Long, DownloadInfo>();

    public class DownloadInfo {
        int mMsgId;
        String mUrl;
        public DownloadInfo(int msgId, String url) {
            this.mMsgId = msgId;
            this.mUrl = url;
        }
        
        public boolean isExist(int msgId, String url) {
            if (url == null  || url.isEmpty()) {
                return false;
            }
            
            if (url.equals(mUrl) && mMsgId == msgId) {
                return true;
            }            
            return false;
        }
        
        public int getId() {
            return mMsgId;
        }
        
    }
    
    
    public PaMessageListAdapter(
            Context context, Cursor c, ListView listView,
            boolean useDefaultColumnsMap, Pattern highlight) {
        super(context, c, FLAG_REGISTER_CONTENT_OBSERVER);
        
        mContext = context;
        mHighlight = highlight;
        
        mInflater = (LayoutInflater)context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        mMessageItemCache = new MessageItemCache(CACHE_SIZE);
        
        mColumnsMap = new ColumnsMap();
        
        listView.setRecyclerListener(new AbsListView.RecyclerListener() {

            @Override
            public void onMovedToScrapHeap(View view) {
                if (view instanceof PaMessageListItem) {
                    PaMessageListItem pmli = (PaMessageListItem)view;
                    pmli.unbind();
                }
            }
       });
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        Log.d(TAG, "bindView() start. pos=" + cursor.getPosition());
        if (view instanceof PaMessageListItem) {
            int msgId = cursor.getInt(COLUMN_ID);
            PaMessageItem msgItem = getCachedMessageItem(msgId, cursor);
            Log.d(TAG,"bindView getItem done. msgId=" + msgId);
            if (msgItem != null) {
                PaMessageListItem pmli = (PaMessageListItem) view;
                int position = cursor.getPosition();
                //must set callback before bind ,due to it user in bind process
                pmli.setMessageListItemAdapter(this);
                pmli.setMsgListItemHandler(mMsgListItemHandler);
                pmli.bind(msgItem, position);
            } else {
                Log.e(TAG,"bindView getItem but result is NULL !!!");
                view.setVisibility(View.GONE);
            }
            Log.d(TAG,"bindView getItem end.");
        }
        if (mTextSize != 0) {
            PaMessageListItem mli = (PaMessageListItem) view;
            mli.setBodyTextSize(mTextSize);
        }
    }

    public interface OnDataSetChangedListener {
        void onDataSetChanged(PaMessageListAdapter adapter);
        void onContentChanged(PaMessageListAdapter adapter);
    }
    
    public void setService(PAService service) {
        mPAService = service;
    }

    public void setOnDataSetChangedListener(OnDataSetChangedListener l) {
        mOnDataSetChangedListener = l;
    }

    public void setMsgListItemHandler(Handler handler) {
        mMsgListItemHandler = handler;
    }
    
    public long sendDownloadRequest(String url, int type, int msgId) {
        if (mPAService == null) {
            Log.d(TAG, "sendDownloadRequest(). service not ready !!!");
            return 0;
        }
        
        //Filter duplicate download request         
        Iterator iter = mDownloadMap.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry entry = (Map.Entry) iter.next();
            DownloadInfo val = (DownloadInfo)entry.getValue();
            if (val.isExist(msgId, url)) {
                Log.d(TAG, "duplicate download request. msgId=" + msgId);
                return 0;
            }
        }
        
        long reqId = mPAService.downloadObject(url, type);
        Log.d(TAG, "sendDownloadRequest(). url=" + url + 
                ". type=" + type + ". msgId=" + msgId + ". reqId=" + reqId);
        if (reqId > 0) {
            mDownloadMap.put(reqId, new DownloadInfo(msgId, url));
        }
        Log.d(TAG, "sendDownloadRequest() mDownloadMap=" + mDownloadMap.toString());
        return reqId;
    }
    
    public void reportDownloadResult(long reqId, boolean result, String path) {
        int msgId = mDownloadMap.get(reqId).getId();
        PaMessageItem item = mMessageItemCache.get(msgId);
        Log.d(TAG, mMessageItemCache.snapshot().toString());
        Log.d(TAG, "reportDownloadResult() msgId=" + msgId + ". item=" + item);

        mDownloadMap.remove(reqId);

        if (item != null) {
            item.setDownloadResult(reqId, result, path);
        } else {
            Log.d(TAG, "reportDownloadResult() fail to find item");
        }

        Log.d(TAG, "reportDownloadResult() mDownloadMap=" + mDownloadMap.toString());
    }

    public void cancelBackgroundLoading() {
        mMessageItemCache.evictAll();
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        Log.v(TAG, "MessageListAdapter.notifyDataSetChanged().");

        mMessageItemCache.evictAll();
        //mDownloadMap.clear();

        if (mOnDataSetChangedListener != null) {
            mOnDataSetChangedListener.onDataSetChanged(this);
        }
    }

    @Override
    protected void onContentChanged() {
        Log.i(TAG, "onContentChanged()");
        if (getCursor() != null && !getCursor().isClosed()) {
            if (mOnDataSetChangedListener != null) {
                mOnDataSetChangedListener.onContentChanged(this);
            }
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        Log.d(TAG, "newView pos=" + cursor.getPosition());
        int type = getItemViewType(cursor);
        View view;
        switch (type) {
        case Constants.MESSAGE_DIRECTION_INCOMING:
            view = mInflater.inflate(R.layout.message_list_item_recv_ipmsg, parent, false);
            break;
        case Constants.MESSAGE_DIRECTION_OUTGOING:
        default:
            view = mInflater.inflate(R.layout.message_list_item_send_ipmsg, parent, false);
            break;
        }
        return view;// new PaMessageListItem(context, view);
    }

    public PaMessageItem getCachedMessageItem(int msgId, Cursor c) {
        PaMessageItem item = mMessageItemCache.get(msgId);
        if (item == null && c != null && isCursorValid(c)) {
            try {
                Log.d(TAG, "getCachedMessageItem() but create new: " + mColumnsMap.mColumnMsgId);
                item = new PaMessageItem(mContext, c, mColumnsMap, mHighlight);
                mMessageItemCache.put(item.mMsgId, item);
            } catch (Exception e) {
                //TODO should be MMSException
                e.printStackTrace();
                Log.e(TAG, "getCachedMessageItem(): " + msgId);
            }
        }
        return item;
    }

    private boolean isCursorValid(Cursor cursor) {
        // Check whether the cursor is valid or not.
        if (cursor == null || cursor.isClosed() || cursor.isBeforeFirst() || cursor.isAfterLast()) {
            return false;
        }
        return true;
    }

    private static long getKey(int type, long id) {
        return id;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }
    
    public void setTextSize(float size) {
        mTextSize = size;
    }

    /* MessageListAdapter says that it contains four types of views. Really, it just contains
     * a single type, a MessageListItem. Depending upon whether the message is an incoming or
     * outgoing message, the avatar and text and other items are laid out either left or right
     * justified. That works fine for everything but the message text. When views are recycled,
     * there's a greater than zero chance that the right-justified text on outgoing messages
     * will remain left-justified. The best solution at this point is to tell the adapter we've
     * got two different types of views. That way we won't recycle views between the two types.
     * @see android.widget.BaseAdapter#getViewTypeCount()
     */
    @Override
    public int getViewTypeCount() {
        return 2;   // Incoming and outgoing messages
    }

    @Override
    public int getItemViewType(int position) {
        Cursor cursor = (Cursor)getItem(position);
        return getItemViewType(cursor);
    }

    private int getItemViewType(Cursor cursor) {
        int type = cursor.getInt(COLUMN_DIRECTION);
        Log.d(TAG, "getItemViewType=" + type);
        return type;
    }

    public Cursor getCursorForItem(PaMessageItem item) {
        return null;
    }

    public static class ColumnsMap {
        public int mColumnMsgId;
        public int mColumnUuid;
        public int mColumnSrcId;
        public int mColumnSrcTable;
        public int mColumnChatId;
        public int mColumnAccountId;
        public int mColumnMsgType;
        public int mColumnTimeStamp;
        public int mColumnCreateTimeStamp;
        public int mColumnSmsDigest;
        public int mColumnText;
        public int mColumnDirection;
        public int mColumnFowardable;
        public int mColumnStatus;
        public int mColumnData1;
        public int mColumnData2;
        public int mColumnData3;
        public int mColumnData4;
        public int mColumnData5;

        public ColumnsMap() {            
            mColumnMsgId            = COLUMN_ID;
            mColumnUuid             = COLUMN_UUID;
            mColumnSrcId            = COLUMN_SOURCE_ID;
            mColumnSrcTable         = COLUMN_SOURCE_TABLE;
            mColumnChatId           = COLUMN_CHAT_ID;
            mColumnAccountId        = COLUMN_ACCOUNT_ID;  
            mColumnMsgType          = COLUMN_MSG_TYPE;
            mColumnTimeStamp        = COLUMN_TIMESTAMP;
            mColumnCreateTimeStamp  = COLUMN_CREATE_TIMESTAMP;
            mColumnSmsDigest        = COLUMN_SMS_DIGEST;
            mColumnText             = COLUMN_TEXT;
            mColumnDirection        = COLUMN_DIRECTION;
            mColumnFowardable       = COLUMN_FORWARDABLE;
            mColumnStatus           = COLUMN_STATUS;
            mColumnData1            = COLUMN_DATA1;
            mColumnData2            = COLUMN_DATA2;
            mColumnData3            = COLUMN_DATA3;
            mColumnData4            = COLUMN_DATA4;
            mColumnData5            = COLUMN_DATA5;
        }

        public ColumnsMap(Cursor cursor) {

        }
    }

    private static class MessageItemCache extends LruCache<Integer, PaMessageItem> {
        public MessageItemCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected void entryRemoved(boolean evicted, Integer key,
                PaMessageItem oldValue, PaMessageItem newValue) {
            //oldValue.cancelPduLoading();
        }
    }
}
