package com.mediatek.rcs.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.gsma.joyn.chat.ChatLog;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.Telephony.Threads;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import com.mediatek.rcs.common.INotifyListener;
import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.provider.GroupMemberData;
import com.mediatek.rcs.common.provider.ThreadMapCache;
import com.mediatek.rcs.common.provider.ThreadMapCache.MapInfo;
import com.mediatek.rcs.common.service.IRCSChatService;
import com.mediatek.rcs.common.service.Participant;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.common.utils.Logger;
import com.mediatek.rcs.common.utils.RCSUtils;

public class GroupManager {

    private static final String TAG = "GroupManager";
    private static GroupManager sInstance;
    private Context mContext;
    private Context mPluginContext;

    private Map<String, RCSGroup> mGroupList = new ConcurrentHashMap<String, RCSGroup>();
    private List<IInitGroupListener> mListener = new CopyOnWriteArrayList<IInitGroupListener>();

    private GroupManager(Context context) {
        Logger.d(TAG, "GroupManager constructor");
        mContext = context;
        mPluginContext = ContextCacher.getPluginContext();
//        RCSServiceManager.getInstance().registNotifyListener(this);
    }

    public static GroupManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new GroupManager(context);
        }
        return sInstance;
    }
    
    public static void createInstance(Context context) {
        Logger.d(TAG, "createInstance");
        if (sInstance == null) {
            sInstance = new GroupManager(context);
        }
    }

    public void addGroupListener(IInitGroupListener listener) {
        mListener.add(listener);
    }

    public void removeGroupListener(IInitGroupListener listener) {
        mListener.remove(listener);
    }

    public List<IInitGroupListener> getAllListeners() {
        return mListener;
    }

    /**
     * Get Group Info, should not call this function in main thread
     * @param chatId
     * @return
     */
    public RCSGroup getRCSGroup(String chatId) {
        Logger.d(TAG, "getRCSGroup, chatId=" + chatId);
//        if (mGroupList.containsKey(chatId)) {
//            return mGroupList.get(chatId);
//        }
        RCSGroup groupInfo = null;
        String chatSelection = ChatLog.GroupChat.CHAT_ID + "='" + chatId + "'";
        Cursor chatCursor = null;
        Cursor participantCursor = null;
        try {
            chatCursor = mContext.getContentResolver().query(
                    RCSUtils.RCS_URI_GROUP_CHAT, RCSUtils.PROJECTION_GROUP_INFO, chatSelection, null, null);
            if (chatCursor.moveToFirst()) {
                List<Participant> participants = new ArrayList<Participant>();
                String memberSelection = GroupMemberData.COLUMN_CHAT_ID + "='" + chatId + "'";
//                memberSelection  = memberSelection + " AND " + GroupMemberData.COLUMN_STATE + "<>" + GroupMemberData.STATE.STATE_PENDING;
                participantCursor = mContext.getContentResolver().query(
                        RCSUtils.RCS_URI_GROUP_MEMBER, RCSUtils.PROJECTION_GROUP_MEMBER, memberSelection, null, null);
                String myNickName = null;
                String myNumber = RCSServiceManager.getInstance().getMyNumber();
                while (participantCursor.moveToNext()) {
                    String number = participantCursor.getString(participantCursor.getColumnIndex(GroupMemberData.COLUMN_CONTACT_NUMBER));
                    String name = participantCursor.getString(participantCursor.getColumnIndex(GroupMemberData.COLUMN_CONTACT_NAME));
                    Participant participant = new Participant(number, name);
                    participant.setState(participantCursor.getInt(participantCursor.getColumnIndex(GroupMemberData.COLUMN_STATE)));
                    participants.add(participant);
                    if (PhoneNumberUtils.compare(number, myNumber)) {
                        myNickName = name;
                    }
                }
                groupInfo = new RCSGroup(mContext
                                        , chatCursor.getString(chatCursor.getColumnIndex(ChatLog.GroupChat.CHAT_ID))
                                        , chatCursor.getString(chatCursor.getColumnIndex(ChatLog.GroupChat.SUBJECT))
                                        , chatCursor.getString(chatCursor.getColumnIndex(ChatLog.GroupChat.CHAIRMAN))
                                        , chatCursor.getString(chatCursor.getColumnIndex(ChatLog.GroupChat.NICKNAME))
                                        , participants
                                        , myNickName
                                        );
            }
        } finally {
            if (chatCursor != null) {
                chatCursor.close();
            }
            if (participantCursor != null) {
                participantCursor.close();
            }
        }
//        mGroupList.put(chatId, groupInfo);
        return groupInfo;
    }
    
    public void removeRCSGroup(String chatId) {
        mGroupList.remove(chatId);
    }

    public String initGroupChat(HashSet<String> participants, String subject) {
        Logger.d(TAG, "init GroupChat, subject = " + subject);
        List<String> contacts = new ArrayList<String>(participants);
        IRCSChatService service = RCSServiceManager.getInstance().getChatService();
        String chatId = null;
        try {
            chatId = service.initGroupChat(subject, contacts);
            if (!TextUtils.isEmpty(chatId)) {
                RCSServiceManager.getInstance().recordInvitingGroup(chatId, subject, contacts);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return chatId;
    }

    public boolean acceptGroupInvitation(String chatId) {
        Logger.d(TAG, "acceptGroupInvitation, chatId=" + chatId);
        IRCSChatService service = RCSServiceManager.getInstance().getChatService();
        try {
            service.acceptGroupChat(chatId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean rejectGroupInvitation(String chatId) {
        Logger.d(TAG, "rejectGroupInvitation, chatId=" + chatId);
        IRCSChatService service = RCSServiceManager.getInstance().getChatService();
        try {
            service.rejectGroupChat(chatId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return true;
    }
    
    public List<String> getGroupParticipants(String chatId) {
        IRCSChatService service = RCSServiceManager.getInstance().getChatService();
        List<String> participants = null;
        try {
            participants = service.getGroupParticipants(chatId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return participants;
    }
/*
    public void notificationsReceived(Intent intent) {
        String action = intent.getAction();
        Logger.d(TAG, "notificationsReceived, action=" + action);
        if (action.equals(IpMessageConsts.GroupActionList.ACTION_GROUP_OPERATION_RESULT)) {
            int actionType = intent.getIntExtra(IpMessageConsts.GroupActionList.KEY_ACTION_TYPE, 0);
            int result = intent.getIntExtra(IpMessageConsts.GroupActionList.KEY_ACTION_RESULT, IpMessageConsts.GroupActionList.VALUE_FAIL);
            Logger.d(TAG, "notificationsReceived, actionType=" + actionType + ", result=" + result);
            switch (actionType) {
                case IpMessageConsts.GroupActionList.VALUE_INIT_GROUP:
                    long threadId = intent.getLongExtra(IpMessageConsts.GroupActionList.KEY_THREAD_ID, 0);
                    String chatId = intent.getStringExtra(IpMessageConsts.GroupActionList.KEY_CHAT_ID);
                    if (result == IpMessageConsts.GroupActionList.VALUE_SUCCESS) {
                        addGroupSysMessage("invite someone to this group", threadId);
                    }
                    for (IInitGroupListener listener : mListener) {
                        listener.onInitGroupResult(result, threadId, chatId);
                    }
                    break;
                case IpMessageConsts.GroupActionList.VALUE_ACCEPT_GROUP_INVITE:
                    //TODO
                    break;
                case IpMessageConsts.GroupActionList.VALUE_REJECT_GROUP_INVITE:
                    //TODO
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void finalize() {
        Logger.d(TAG, "finalize() called");
        try {
            RCSServiceManager.getInstance().unregistNotifyListener(this);
            super.finalize();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void addGroupSysMessage(String body, long threadId) {
        Logger.d(TAG, "addGroupSysMessage, body=" + body + ", threadId=" + threadId);
        ContentValues values = new ContentValues();
        values.put("thread_id", threadId);
        values.put("date", System.currentTimeMillis());
        values.put("sub_id", RCSUtils.getRCSSubId());
        values.put("body", body);
        Uri uri = RCSUtils.URI_GROUP_SYS_MESSAGE;
        Uri resultUri = ContextCacher.getHostContext().getContentResolver().insert(uri, values);
        Logger.d(TAG, "addGroupSysMessage end, resultUri=" + resultUri);
    }*/
}
