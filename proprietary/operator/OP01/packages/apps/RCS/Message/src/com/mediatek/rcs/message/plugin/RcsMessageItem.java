package com.mediatek.rcs.message.plugin;

import java.lang.ref.SoftReference;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Message;
import android.util.Log;

import android.provider.Telephony.Sms;

import com.mediatek.mms.ipmessage.IpMessageItem;
import com.mediatek.rcs.common.IpAttachMessage;
import com.mediatek.rcs.common.IpMessage;
import com.mediatek.rcs.common.RCSMessageManager;
import com.mediatek.rcs.common.IpMessageConsts.IpMessageType;
import com.mediatek.rcs.message.utils.BackgroundLoader;
import com.mediatek.rcs.message.utils.RcsMessageUtils;

public class RcsMessageItem extends IpMessageItem {
    private static String TAG = "RcseMessageItem";

    IpMessage mIpMessage = null;
    Context mContext;
    long mThreadId;
    boolean mIsGroupItem;
    long mIpMessageId;
    long mMsgId;
    int mSubId;
    String mType;
    String mAddress;
    int mBoxId;
    String mSubject;
    int mMessageType;
    int mBurnedMsgTimerNum = 5;
    String mBody;
    boolean mIsSystemEvent = false;
    long mSentDate;
    long mDate;
//    Handler mHandler;

    private SoftReference<Bitmap> mBitmapCache = new SoftReference<Bitmap>(null);
    private int mCacheBitmapWidth;
    private int mCacheBitmapHeight;

    public RcsMessageItem() {
    }

    public Bitmap getIpMessageBitmap() {
        return mBitmapCache.get();
    }

    public void setIpMessageBitmapCache(Bitmap bitmap) {
        if (null != bitmap) {
            mBitmapCache = new SoftReference<Bitmap>(bitmap);
        }
    }

    // / M: these 3 methods is matched with setIpMessageBitmapCache.
    public void setIpMessageBitmapSize(int width, int height) {
        mCacheBitmapWidth = width;
        mCacheBitmapHeight = height;
    }

    public int getIpMessageBitmapWidth() {
        return mCacheBitmapWidth;
    }

    public int getIpMessageBitmapHeight() {
        return mCacheBitmapHeight;
    }
    
    public boolean isReceivedBurnedMessage() {
        Log.d(TAG, " [BurnedMsg]: isReceivedBurnedMessage()");
        IpMessage ipMsg = getIpMessage();
        if (ipMsg == null) {
            Log.d(TAG, " [BurnedMsg]: BurnedMessage: IpMessage is null");
            return false;
        }
        Log.d(TAG, " [BurnedMsg]: BurnedMessage: isReceivedBurnedMessage  getStatus = " + ipMsg.getStatus());
        if (ipMsg.getStatus() == Sms.MESSAGE_TYPE_OUTBOX || ipMsg.getStatus() == Sms.MESSAGE_TYPE_SENT ) {
            return false;
        }
        boolean isBurned = false;
        if (ipMsg.getType() == IpMessageType.TEXT) {
            isBurned =  ipMsg.getBurnedMessage();
        } else if (ipMsg != null && (ipMsg.getType() == IpMessageType.VIDEO ||
                ipMsg.getType() == IpMessageType.PICTURE||
                ipMsg.getType() == IpMessageType.VOICE)){
            isBurned = ((IpAttachMessage) ipMsg).getBurnedMessage();
        }
        Log.d(TAG, " [BurnedMsg]: BurnedMessage: isReceivedBurnedMessage  ipMsg.getType() = " + ipMsg.getType()+ " isBurned = "+isBurned);
        return isBurned;
    }

    @Override
    public boolean onIpCreateMessageItem(Context context, Cursor cursor, long msgId, String type,
            int subId) {
        /// M: add for ipmessage
        mContext = context;
        int columnCount = cursor.getColumnCount();
        if (columnCount <= RcsMessageListAdapter.COLUMN_SIM_MESSAGE_COLUMN_MAX) {
            Log.d(TAG, "onIpCreateMessageItem: columnCount = " + columnCount);
            mIpMessageId = -1;
            return false;
        }

        mIpMessageId = cursor.getLong(RcsMessageListAdapter.COLUMN_SMS_IP_MESSAGE_ID);
        mThreadId = cursor.getLong(RcsMessageListAdapter.COLUMN_THREAD_ID);
        String chatId = RcsMessageUtils.getGroupChatIdByThread(mThreadId);
        if (chatId != null) {
            mIsGroupItem = true;
        }
        mMsgId = msgId;
        mType = type;
        mSubId = subId;
        mAddress = cursor.getString(RcsMessageListAdapter.COLUMN_SMS_ADDRESS);
        mBody = cursor.getString(RcsMessageListAdapter.COLUMN_SMS_BODY);
        mSentDate = cursor.getLong(RcsMessageListAdapter.COLUMN_SMS_DATE_SENT);
        mDate = cursor.getLong(RcsMessageListAdapter.COLUMN_SMS_DATE);
        mBoxId = cursor.getInt(RcsMessageListAdapter.COLUMN_SMS_TYPE);
        Log.d(TAG, "address is: " + mAddress);
//        mSubject = cursor.getString(RcsMessageListAdapter.COLUMN_SMS_BODY);
        Log.d(TAG, "mBody is: " + mBody);
        if (mIpMessageId != 0) {
            //TODO: get ipMessage from Rcs            
//            mIpMessage = IpMessageManager.getInstance(context).getIpMsgInfo(mMsgId);
            loadIpMessage(mThreadId, mIpMessageId);
            if (mIpMessage == null) {
                Log.d(TAG, "MessageItem.init(): ip message is null!");
            }
            return true;
        }
        return false;
    }

    /**
     * add for mms
     */
    public boolean onIpCreateMessageItem(Context context, long msgId, String type, int subId, int boxId, String subject, 
                        int messageType, String address) {
        mContext = context;
        mIpMessageId = 0;
        mMsgId = msgId;
        mType = type;
        mSubId = subId;
        //for mms
        mBoxId = boxId;
        mSubject = subject;
        mMessageType = messageType;
        mAddress = address;
        return false;
    }

    /**
     * add for group system event
     * @param context
     * @param cursor
     * @return
     */
    public boolean onIpCreateMessageItem(Context context, Cursor cursor) {
        mContext = context;
        String type = cursor.getString(RcsMessageListAdapter.COLUMN_MSG_TYPE);
        if (type.equals(RcsMessageListAdapter.RCS_TYPE)) {
            //system event
            mIsSystemEvent = true;
            mBody = cursor.getString(RcsMessageListAdapter.COLUMN_SMS_BODY);
        }
        return true;
    }

    public synchronized IpMessage getIpMessage() {
        Log.e(TAG, "[getIpMessage]: ipMessage is " + mIpMessage);
//        Log.e(TAG, "getIpMessage: this is " + this);
        return mIpMessage;
    }

    private synchronized void updateIpMessage(IpMessage msg) {
        mIpMessage = msg;
        Log.e(TAG, "[updateIpMessage]: msg is " + mLoadIpMsgFinishedMessage);
//        Log.e(TAG, "updateIpMessage: this is " + this);
        if (mLoadIpMsgFinishedMessage != null) {
            mLoadIpMsgFinishedMessage.sendToTarget();
        }
    }

    private Message mLoadIpMsgFinishedMessage = null;
    public synchronized void setGetIpMessageFinishMessage(Message msg) {
        Log.d(TAG, "[setGetIpMessageFinishMessage]:  msg = " + msg + ", ipMessage = " + mIpMessage);
        mLoadIpMsgFinishedMessage = msg;
        if (mIpMessage != null && msg != null) {
            msg.sendToTarget();
        } 
    }

    public void onItemDetachedFromWindow() {
    }
    Runnable mLoadMsgRunnable;

    private void loadIpMessage(final long thread_id, final long ipMsgId) {
        final Object object = new Object();

        mLoadMsgRunnable = new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
//                Log.d(TAG, "async load ipmessage: thread_id = " + thread_id + ", ipMsgId = " + ipMsgId);
                IpMessage ipMsg  = RCSMessageManager.getInstance(mContext).getIpMsgInfo(thread_id, ipMsgId);
//                try {
//                    //TODO test ,must delete when IT
//                    int waitTime = 4000;
                    /// @}
//                    Thread.sleep(waitTime);
//                } catch (InterruptedException ex) {
//                    Log.e(TAG, "wait has been intrrupted", ex);
 //               }

//                Log.d(TAG, "async load ipmessage loaded, ipMsg = " + ipMsg);
                updateIpMessage(ipMsg);
                synchronized (object) {
                    object.notifyAll();
                }
            }
        };
        getBackgroundMessageLoader().pushTask(mLoadMsgRunnable);
        synchronized (object) {
            try {
                /// M: Fix ALPS00391886, avoid waiting too long time when many uncached messages
                int waitTime = 400;
                /// @}
                object.wait(waitTime);
            } catch (InterruptedException ex) {
                Log.e(TAG, "wait has been intrrupted", ex);
            }
        }
    }

    public void detached() {
        if (getIpMessage() == null && mLoadMsgRunnable != null) {
            getBackgroundMessageLoader().cacelTask(mLoadMsgRunnable);
        }
    }

    private static BackgroundLoader sBackgroundLoader;
    private static BackgroundLoader getBackgroundMessageLoader() {
        if(sBackgroundLoader == null) {
            sBackgroundLoader = new BackgroundLoader();
        }
        return sBackgroundLoader;
    }

    public long getIpMessageId() {
        return mIpMessageId;
    }
}
