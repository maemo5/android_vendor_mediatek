package com.mediatek.rcs.message.plugin;


import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mediatek.mms.ipmessage.IpMessageListAdapter;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.Settings;
import android.provider.Telephony;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.MmsSms.PendingMessages;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Conversations;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.mediatek.mms.ipmessage.IpMessageListItem;
import com.mediatek.rcs.message.R;
import com.mediatek.rcs.message.ui.MsgListItem;

public class RcsMessageListAdapter extends IpMessageListAdapter {
    private static String TAG = "RcseMessageListAdapter";
    private static final String SMS_IP_MESSAGE_ID = Telephony.Sms.IPMSG_ID;
    private static final String SMS_SPAM = "spam";
    /// M: add for OP09 Feature, two new columns for mms cc.
    private static final String MMS_CC          = "mms_cc";
    private static final String MMS_CC_ENCODING = "mms_cc_encoding";
    static final String RCS_TYPE = "rcs";

    /// M: message type
    private static final int INCOMING_ITEM_TYPE = 0;
    private static final int OUTCOMING_ITEM_TYPE = 1;
    private static final int INCOMING_ITEM_TYPE_IPMSG = 2;
    private static final int OUTGOING_ITEM_TYPE_IPMSG = 3;
    private static final int SYSTEM_EVENT_ITEM_TYPE = 4;
    private static final int UNKNOWN_ITEM_TYPE = -1;
    
    /// M: query item culumns
    static final int COLUMN_MSG_TYPE            = 0;
    static final int COLUMN_THREAD_ID           = 2;
    static final int COLUMN_SMS_ADDRESS         = 3;
    static final int COLUMN_SMS_BODY            = 4;
    static final int COLUMN_SMS_DATE            = 5;
    static final int COLUMN_SMS_DATE_SENT       = 6;
    static final int COLUMN_SMS_TYPE            = 8;
    static final int COLUMN_SMS_STATUS          = 9;
    static final int COLUMN_MMS_MESSAGE_BOX     = 18;
    static final int COLUMN_SMS_SUBID           = 24;
    static final int COLUMN_MMS_SUBID           = 25;
    static final int COLUMN_SMS_IP_MESSAGE_ID   = 28;
    static final int COLUMN_SMS_SPAM            = 29;
    
    static final int COLUMN_SIM_MESSAGE_COLUMN_MAX = 20;

    static final int CHAT_TYPE_ONE2ONE = 1;
    static final int CHAT_TYPE_ONE2MULTI = 2;
    static final int CHAT_TYPE_GROUP = 3;

    static final int IP_VIEW_TYPE_COUNT = 5;
    private Cursor mCursor;
    private long mThreadId;
    private Context mPluginContext; 
    private boolean misChatActive = true;
    private int mChatType;
    private boolean mIsSimMessage;
    private HashSet<OnMessageListChangedListener> mListChangedListeners 
                            = new HashSet<OnMessageListChangedListener>(2);

    public RcsMessageListAdapter(Context pluginContext) {
        mPluginContext = pluginContext;
    }

    @Override
    public View onIpNewView(LayoutInflater inflater, Cursor cursor, ViewGroup parent) {
        // / M: add for ipmessage
        int columnCount = cursor.getColumnCount();
        Log.d(TAG, "onIpNewView, columnCount = " + columnCount);
        if (columnCount < COLUMN_SIM_MESSAGE_COLUMN_MAX) {
            mIsSimMessage = true;
            return null;
        }
        mThreadId = cursor.getLong(COLUMN_THREAD_ID);
        String name = cursor.getString(COLUMN_SMS_ADDRESS);
        Log.d("mtk80999", "onIpNewView, name = " + name);
        Log.d(TAG, "onIpNewView(): threadId = " + mThreadId);
        LayoutInflater pluginInflater = LayoutInflater.from(mPluginContext);
        View retView = null;
        switch (getIpItemViewType(cursor)) {
            case INCOMING_ITEM_TYPE:
                retView = pluginInflater.inflate(R.layout.message_list_item_recv, null, false);
                retView.setTag(RcsMessageListItem.TAG_ITEM_TYPE, RcsMessageListItem.TYPE_INCOMING);
                break;
            case OUTCOMING_ITEM_TYPE:
                retView = pluginInflater.inflate(R.layout.message_list_item_send, null, false);
                retView.setTag(RcsMessageListItem.TAG_ITEM_TYPE, RcsMessageListItem.TYPE_OUTGOING);
                break;
            case INCOMING_ITEM_TYPE_IPMSG:
                retView =  pluginInflater.inflate(R.layout.message_list_item_recv_ipmsg, null, false);
                retView.setTag(RcsMessageListItem.TAG_ITEM_TYPE, RcsMessageListItem.TYPE_INCOMING);
                break;
            case OUTGOING_ITEM_TYPE_IPMSG:
                retView = pluginInflater.inflate(R.layout.message_list_item_send_ipmsg, parent, false);
                retView.setTag(RcsMessageListItem.TAG_ITEM_TYPE, RcsMessageListItem.TYPE_OUTGOING);
                break;
            case SYSTEM_EVENT_ITEM_TYPE:
                retView = pluginInflater.inflate(R.layout.message_list_item_recv_ipmsg, parent, false);
                retView.setTag(RcsMessageListItem.TAG_ITEM_TYPE, RcsMessageListItem.TYPE_SYSTEM_EVENT);
                break;
            default:
                return null;
        }
        if (retView != null) {
            retView.setTag(RcsMessageListItem.TAG_THREAD_ID, mThreadId);
        }
        return retView;
    }


    @Override
    public View onIpBindView(IpMessageListItem mListItem, Context context, Cursor cursor) {
        Log.d(TAG, "onIpBindView()");
        if (mIsSimMessage) {
            return null;
        }
        mListItem.setIpMessageListItemAdapter(this);
        if (cursor.isLast()) {
            RcsMessageListItem item = (RcsMessageListItem)mListItem;
            item.setIsLastItem(true);
        }
        return null;
     //   return super.onIpBindView(mListItem, context, cursor);
    }

    @Override
    public int getIpItemViewType(Cursor cursor) {
        /// M: add for ipmessage
        if (mIsSimMessage) {
            return  -1;
        }
        String type = cursor.getString(COLUMN_MSG_TYPE);
        Log.d(TAG, "getIpItemViewType(): message type = " + type);

        int boxId;
        if ("sms".equals(type)) {
            /// M: check sim sms and set box id
            long status = cursor.getLong(COLUMN_SMS_STATUS);
            boolean isSimMsg = false;
            if (status == SmsManager.STATUS_ON_ICC_SENT
                    || status == SmsManager.STATUS_ON_ICC_UNSENT) {
                isSimMsg = true;
                boxId = Sms.MESSAGE_TYPE_SENT;
            } else if (status == SmsManager.STATUS_ON_ICC_READ
                    || status == SmsManager.STATUS_ON_ICC_UNREAD) {
                isSimMsg = true;
                boxId = Sms.MESSAGE_TYPE_INBOX;
            } else {
                boxId = cursor.getInt(COLUMN_SMS_TYPE);
            }
            long ipMessageId = cursor.getLong(COLUMN_SMS_IP_MESSAGE_ID);
            Log.d(TAG, "getItemViewType(): ipMessageId = " + ipMessageId);
            if (ipMessageId != 0 && !isSimMsg) {
                return boxId == Mms.MESSAGE_BOX_INBOX ? INCOMING_ITEM_TYPE_IPMSG : OUTGOING_ITEM_TYPE_IPMSG;
            }
        } else if("mms".equals(type)) {
            boxId = cursor.getInt(COLUMN_MMS_MESSAGE_BOX);
        } else if (RCS_TYPE.equals(type)) {
            return SYSTEM_EVENT_ITEM_TYPE;
        } else {
//            throw new RuntimeException("RcsMessageListAdapter bind unkown type");
            return UNKNOWN_ITEM_TYPE;
        }

        return boxId == Mms.MESSAGE_BOX_INBOX ? INCOMING_ITEM_TYPE : OUTCOMING_ITEM_TYPE;
//        return -1;
    }

    @Override
    public int getIpViewTypeCount() {
        Log.d(TAG, "getIpViewTypeCount(): " + IP_VIEW_TYPE_COUNT);
        if (mIsSimMessage) {
            return -1;
        } else {
            return IP_VIEW_TYPE_COUNT;
        }
    }
    
    @Override
    public boolean isEnabled(Cursor cursor, int position) {
        if (!mIsSimMessage) {
            int curPosition = cursor.getPosition();
            cursor.moveToPosition(position);
            int type = getIpItemViewType(cursor);
            if (type == SYSTEM_EVENT_ITEM_TYPE) {
                return false;
            }
        }
        return super.isEnabled(cursor, position);
    }

    @Override
    public void changeCursor(Cursor cursor) {
        mCursor = cursor;
        for (OnMessageListChangedListener l : mListChangedListeners) {
            l.onChanged();
        }
    }

    // for displayed burned message.
    public void bindIpView(View view, Context context, Cursor cursor) {
    	Log.d(TAG,"bindIpView() start.");
    	RcsMessageItem msgItem = null;
/*
        if (view instanceof RCsMessageItem) {
//            view.setVisibility(View.VISIBLE);
//            String type = cursor.getString(mColumnsMap.mColumnMsgType);
//            long msgId = cursor.getLong(mColumnsMap.mColumnMsgId);
//            msgItem = getCachedMessageItem(type, msgId, cursor);
            if(msgItem.isReceivedMessage()) 
            {
            	//View viewImage = mInflater.inflate(R.layout.message_list_item_recv_mail, null);
            	
            	LinearLayout mMsgContainer = (LinearLayout) view.findViewById(R.id.mms_layout_view_parent);
            	mMsgContainer.setVisibility(View.GONE);
            	//view.setVisibility(View.INVISIBLE);
            	ImageView viewImage = (ImageView) view.findViewById(R.id.hide_ip_bar_message);
            	viewImage.setVisibility(View.VISIBLE);
//            	ImageView viewImage = new ImageView(context);
//            	viewImage.setImageResource(R.drawable.wallpaper_launcher_gallery);
//                view = viewImage;
//                view.setBackgroundResource(R.drawable.wallpaper_launcher_gallery);

//                view.setBackgroundDrawable(null);
//                return;


//                MessageListItem mliItem = (MessageListItem) view;
//                mliItem.hideAllView();
//                view.setVisibility(View.VISIBLE);
            	MessageListItem mli = (MessageListItem) view;
                int position = cursor.getPosition();
                /// M: google JB.MR1 patch, group mms
                mli.bind(msgItem, mIsGroupConversation, position, mIsDeleteMode);
                mli.setMsgListItemHandler(mMsgListItemHandler);
                mli.setMessageListItemAdapter(this);
                
                return;
            }
        }
        */
    	return;
    }
    
    public long getThreadId() {
        return mThreadId;
    }

    public void setChatActive(boolean active) {
        misChatActive = active;
    }

    public boolean isChatActive() {
        return misChatActive;
    }

    public int getAutoSelectSubId() {
        int subId = -1;
        boolean isValid = false;
        List<SubscriptionInfo> mSubInfoList =  SubscriptionManager.from(mPluginContext).getActiveSubscriptionInfoList();
        int mSubCount = mSubInfoList.isEmpty() ? 0 : mSubInfoList.size();
        long subIdinSetting = SubscriptionManager.getDefaultSmsSubId();
        if (subIdinSetting == Settings.System.SMS_SIM_SETTING_AUTO) {
            if (mCursor != null && mCursor.moveToLast()) {
                subId = getSubIdFromCursor(mCursor);
            }
            for (int i = 0; i < mSubCount; i++) {
                if ((int) mSubInfoList.get(i).getSubscriptionId() == subId) {
                    isValid = true;
                    break;
                }
            }
            if (!isValid) {
                subId = -1;
            }
        }
        Log.d(TAG, "getAutoSelectSubId subId = " + subId);
        return subId;
    }

    private int getSubIdFromCursor(Cursor c) {
        int subId = -1;
        try {
            String type = c.getString(COLUMN_MSG_TYPE);
            if (type.equals("mms")) {
                subId = c.getInt(COLUMN_MMS_SUBID);
            } else if (type.equals("sms")) {
                subId = c.getInt(COLUMN_SMS_SUBID);
            }
        } catch (Exception e) {
            Log.d(TAG, "getSimId error happens, please check!");
        } finally {
            Log.d(TAG, "getSimId id = " + subId);
            return subId;
        }
    }

    public void setChatType(int type) {
        mChatType = type;
    }

    public int getChatType() {
        return mChatType;
    }

    public boolean isOnlyHasSystemMsg() {
        if (mCursor != null && !mCursor.isClosed()) {
            if (mCursor.moveToFirst()) {
                do {
                    String type = mCursor.getString(COLUMN_MSG_TYPE);
                    if (!type.equals(RCS_TYPE)) {
                        return false;
                    }
                } while(mCursor.moveToNext());
            }
        }
        return true;
    }

    /**
     * addOnMessageListChangedListener
     * @param l OnMessageListChangedListener
     * @return true if add success, else false
     */
    public boolean addOnMessageListChangedListener(OnMessageListChangedListener l) {
        return mListChangedListeners.add(l);
    }

    /**
     * removeOnMessageListChangedListener
     * @param l OnMessageListChangedListener
     * @return true if remove success, else false
     */
    public boolean removeOnMessageListChangedListener(OnMessageListChangedListener l) {
        return mListChangedListeners.remove(l);
    }

    /**
     * OnMessageListChangedListener. when message list is changed, will call onChanged();
     * @author mtk80999
     *
     */
    public interface OnMessageListChangedListener {
        public void onChanged();
    }
}
