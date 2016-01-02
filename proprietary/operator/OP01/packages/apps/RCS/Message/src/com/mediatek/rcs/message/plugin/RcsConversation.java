package com.mediatek.rcs.message.plugin;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.provider.Telephony.Threads;
import android.provider.Telephony.ThreadSettings;
import android.util.Log;

import com.mediatek.mms.ipmessage.IpContact;
import com.mediatek.mms.ipmessage.IpConversation;
import com.mediatek.mms.ipmessage.IpConversationCallback;

import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.common.provider.RCSDataBaseUtils;
import com.mediatek.rcs.common.provider.ThreadMapCache;
import com.mediatek.rcs.common.provider.ThreadMapCache.MapInfo;
import com.mediatek.rcs.message.utils.RcsMessageUtils;
import com.mediatek.rcs.message.utils.ThreadNumberCache;

public class RcsConversation extends IpConversation {
    private final String TAG = "RcsConversation";
    
    public static final String[] PROJECTION_NEW_GROUP_INVITATION = {
        Threads._ID, 
        Threads.DATE, 
        Threads.READ,
        Telephony.Threads.STATUS
    };
    public static final Uri URI_CONVERSATION =
            Uri.parse("content://mms-sms/conversations").buildUpon()
                    .appendQueryParameter("simple", "true").build();
    public static final String SELECTION_NEW_GROUP_INVITATION_BY_STATUS =
            "(" + Telephony.Threads.STATUS +
            " = " + IpMessageConsts.GroupActionList.GROUP_STATUS_INVITING
            + " OR " + Telephony.Threads.STATUS +
            " = " + IpMessageConsts.GroupActionList.GROUP_STATUS_INVITING_AGAIN
            + " OR " + Telephony.Threads.STATUS +
            " = " + IpMessageConsts.GroupActionList.GROUP_STATUS_INVITE_EXPAIRED
            + ")";
    public static final String SELECTION_NEW_GROUP_INVITATION_BY_THREAD = "threads._id = ?";
    
        /// M: use this instead of the google default to query more columns in thread_settings
    public static final String[] ALL_THREADS_PROJECTION_EXTEND = {
        Threads._ID, Threads.DATE, Threads.MESSAGE_COUNT, Threads.RECIPIENT_IDS,
        Threads.SNIPPET, Threads.SNIPPET_CHARSET, Threads.READ, Threads.ERROR,
        Threads.HAS_ATTACHMENT
        /// M:
        , Threads.TYPE , Telephony.Threads.READ_COUNT , Telephony.Threads.STATUS,
        Telephony.ThreadSettings._ID, /// M: add for common
        Telephony.ThreadSettings.NOTIFICATION_ENABLE,
        Telephony.ThreadSettings.SPAM, Telephony.ThreadSettings.MUTE,
        Telephony.ThreadSettings.MUTE_START,
        Telephony.Threads.DATE_SENT
    };

    /** M: this type is used by mType, a convenience for identify a group conversation.
     *  so currently mType value maybe 0 sms 1 mms 2 wappush 3 cellbroadcast 10 guide 110 group
     *  the matched number maybe not right, but the type is as listed.
     */
    public static final int TYPE_GROUP = 110;
    public static final int STICKY     = 18;

//  public static final int SPAM           = 14;
    private long mThreadId = 0;
    private String mNumber;
    private boolean mIsSticky;
    private boolean mIsGroup;
    private String mGroupChatId;
    IpConversationCallback mConversationCallback;

    /**
     *  Override IpConversation's onIpFillFromCursor. Host class is ConversationList.
     * @param context Context
     * @param c cursor
     * @param recipientSize recipientsize
     * @param number number
     * @param type type
     * @param date date
     * @return int
     */
    public int onIpFillFromCursor(Context context, Cursor c, int recipientSize, String number,
            int type, long date) {
        mThreadId = c.getLong(0);
        Log.d(TAG, "onIpFillFromCursor, threadId = " + mThreadId);
        Log.d(TAG, "onIpFillFromCursor, stick Time = " + c.getLong(STICKY));
        mIsSticky = (c.getLong(STICKY) != 0);
        if(RcsConversationList.mStickyThreadsSet != null) {
            if (mIsSticky) {
                RcsConversationList.mStickyThreadsSet.add(mThreadId);
            } else {
                RcsConversationList.mStickyThreadsSet.remove(mThreadId);
            }
        }

        MapInfo info = ThreadMapCache.getInstance().getInfoByThreadId(mThreadId);
        if (info != null) {
            mIsGroup = true;
            mGroupChatId = info.getChatId();
            return TYPE_GROUP;
        }
        return type;
    }

    public long getThreadId() {
        return mThreadId;
    }

    public boolean isSticky() {
        return mIsSticky;
    }
    
    /**
     * Override IpConversation's guaranteeIpThreadId.
     * @param threadId
     * @return real threadId, if threadId is group chat, return real threadid; else return 0.
     */
    public long guaranteeIpThreadId(long threadId) {
        if (!mIsGroup) {
            return 0;
        }
        if (mThreadId < 0) {
            mThreadId = RCSDataBaseUtils.createGroupThreadByChatId(mGroupChatId);
        }
        Log.d(TAG, "[guaranteeIpThreadId] mThreadId = " + mThreadId);
        return mThreadId;
    }

    /**
     * Override IpConversation's onIpInit.
     * @param callback use this to callback to Conversation in mms host.
     */
    public void onIpInit(IpConversationCallback callback) {
        mConversationCallback = callback;
    }

    /**
     * Get this conversation's contact list from mms host.
     * @return list of IpContact.
     */
    public List<IpContact> getIpContactList() {
        return mConversationCallback.getIpContactList();
    }

    /**
     * Set Group chat ID. Use this it can update threadid in guaranteeIpThreadId.
     * @param chatId Group chat's chat id.
     */
    public void setGroupChatId(String chatId) {
        if (chatId == null) {
            return;
        }
        mIsGroup = true;
        mGroupChatId = chatId;
        MapInfo info = ThreadMapCache.getInstance().getInfoByChatId(chatId);
        mThreadId = info.getThreadId();
    }
}
