package com.mediatek.rcs.common.provider;


import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

import com.mediatek.rcs.common.utils.Logger;
import com.mediatek.rcs.common.utils.RCSUtils;

public class ThreadMapCache {

    private static final String THREAD_NAME = "ThreadMapThread";
    private static final String TAG = "ThreadMapCache";
    private static HandlerThread sMapHThread = null;
    private static MapHandler sHandler = null;
    private static Context mContext = null;
    
    private static ThreadMapCache sInstance = null;

    private static final Map<String, MapInfo> sMapInfo = new ConcurrentHashMap<String, MapInfo>();

    private static final int EVENT_LOAD_ALL_MAP_DATA             = 1;
    private static final int EVENT_UPDATE_SUBJECT_BY_CHATID      = 2;
    private static final int EVENT_UPDATE_SUBJECT_BY_THREADID    = 3;
    private static final int EVENT_REMOVE_BY_CHATID              = 4;
    private static final int EVENT_REMOVE_BY_THREADID            = 5;
    private static final int EVENT_ADD_MAP_DATA                  = 6;
    private static final int EVENT_REFRESH_BY_THREADS            = 7;
    private static final int EVENT_UPDATE_NICKNAME_BY_CHATID     = 8;
    private static final int EVENT_UPDATE_NICKNAME_BY_THREADID   = 9;
    private static final int EVENT_UPDATE_THREADID_BY_CHATID     = 10;
    private static final int EVENT_UPDATE_STATUS_BY_CHATID       = 11;
    private static final int EVENT_UPDATE_ISMECHAIRMEN_BY_CHATID = 12;
    private static final int EVENT_UPDATE_SUBID_BY_CHATID        = 13;

    private ThreadMapCache(Context context) {
        mContext = context;
        if (sMapHThread == null) {
            sMapHThread = new HandlerThread(THREAD_NAME, Process.THREAD_PRIORITY_BACKGROUND);
            sMapHThread.start();
        }
        if (sHandler == null) {
            sHandler = new MapHandler(sMapHThread.getLooper());
        }
        loadAllMapData(mContext);
    }

    public synchronized static void createInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ThreadMapCache(context);
        }
    }

    public synchronized static ThreadMapCache getInstance() {
        return sInstance;
    }

    public synchronized MapInfo getInfoByThreadId(long threadId) {
        if (sMapInfo == null) {
            return null;
        }
        Collection<MapInfo> infos = sMapInfo.values();
        String chatId = null;
        for (MapInfo info : infos) {
            if (info.getThreadId() == threadId) {
                chatId = info.getChatId();
                break;
            }
        }
        if (chatId != null && sMapInfo.containsKey(chatId)) {
            return sMapInfo.get(chatId);
        }
        return null;
    }

    public synchronized MapInfo getInfoByChatId(String chatId) {
        if (sMapInfo == null) {
            return null;
        }
        if (chatId != null && sMapInfo.containsKey(chatId)) {
            return sMapInfo.get(chatId);
        }
        logD("getInfoByChatId, cannot find, not a group chat, chatId = " + chatId);
        return null;
    }

    public synchronized void updateThreadId(String chatId, long threadId) {
        MapInfo info = sMapInfo.get(chatId);
        if (info != null) {
            String subject = info.getSubject();
            String nickName = info.getNickName();
            long status = info.getStatus();
            boolean isMeChairmen = info.isMeChairmen();
            int subId = info.getSubId();
            logD("updateSubjectByThreadId, find info by given chatId=" + chatId + ", threadId=" + threadId);
            sMapInfo.remove(chatId);
            sMapInfo.put(chatId, new MapInfo(threadId, chatId, subject, nickName, status, isMeChairmen, subId));

            Message msg = new Message();
            msg.what = EVENT_UPDATE_THREADID_BY_CHATID;
            Bundle data = new Bundle();
            data.putString(ThreadMapData.KEY_CHAT_ID, chatId);
            data.putLong(ThreadMapData.KEY_THREAD_ID, threadId);
            msg.setData(data);
            sHandler.sendMessage(msg);
        }
    }
    
    public synchronized void updateSubId(String chatId, int subId) {
        MapInfo info = sMapInfo.get(chatId);
        if (info != null) {
            String subject = info.getSubject();
            String nickName = info.getNickName();
            long status = info.getStatus();
            boolean isMeChairmen = info.isMeChairmen();
            long threadId = info.getThreadId();
            logD("updateSubjectByThreadId, find info by given chatId=" + chatId + ", threadId=" + threadId);
            sMapInfo.remove(chatId);
            sMapInfo.put(chatId, new MapInfo(threadId, chatId, subject, nickName, status, isMeChairmen, subId));

            Message msg = new Message();
            msg.what = EVENT_UPDATE_SUBID_BY_CHATID;
            Bundle data = new Bundle();
            data.putString(ThreadMapData.KEY_CHAT_ID, chatId);
            data.putInt(ThreadMapData.KEY_SUB_ID, subId);
            msg.setData(data);
            sHandler.sendMessage(msg);
        }
    }

    public synchronized void updateSubjectByChatId(String chatId, String subject) {
        logD("updateSubjectByChatId related map info chatId=" + chatId);
        MapInfo info = sMapInfo.get(chatId);
        if (info != null) {
            long threadId = info.getThreadId();
            String nickName = info.getNickName();
            long status = info.getStatus();
            boolean isMeChairmen = info.isMeChairmen();
            int subId = info.getSubId();
            logD("updateSubjectByThreadId, find info by given chatId=" + chatId + ", threadId=" + threadId);
            sMapInfo.remove(chatId);
            sMapInfo.put(chatId, new MapInfo(threadId, chatId, subject, nickName, status, isMeChairmen, subId));

            Message msg = new Message();
            msg.what = EVENT_UPDATE_SUBJECT_BY_CHATID;
            Bundle data = new Bundle();
            data.putString(ThreadMapData.KEY_CHAT_ID, chatId);
            data.putString(ThreadMapData.KEY_SUBJECT, subject);
            msg.setData(data);
            sHandler.sendMessage(msg);
        }
    }

    public synchronized void updateStatusByChatId(String chatId, long status) {
        logD("updateStatusByChatId chatId=" + chatId + ", status=" + status);
        MapInfo info = sMapInfo.get(chatId);
        if (info != null) {
            long threadId = info.getThreadId();
            String subject = info.getSubject();
            String nickName = info.getNickName();
            boolean isMeChairmen = info.isMeChairmen();
            int subId = info.getSubId();
            logD("updateSubjectByThreadId, find info by given chatId=" + chatId + ", threadId=" + threadId);
            sMapInfo.remove(chatId);
            sMapInfo.put(chatId, new MapInfo(threadId, chatId, subject, nickName, status, isMeChairmen, subId));
            
            Message msg = new Message();
            msg.what = EVENT_UPDATE_STATUS_BY_CHATID;
            Bundle data = new Bundle();
            data.putString(ThreadMapData.KEY_CHAT_ID, chatId);
            data.putLong(ThreadMapData.KEY_STATUS, status);
            msg.setData(data);
            sHandler.sendMessage(msg);
        }
    }

    public synchronized void updateNickNameByChatId(String chatId, String nickName) {
        logD("updateNickNameByChatId chatId=" + chatId + ", nickName=" + nickName);
        MapInfo info = sMapInfo.get(chatId);
        if (info != null) {
            long threadId = info.getThreadId();
            String subject = info.getSubject();
            long status = info.getStatus();
            boolean isMeChairmen = info.isMeChairmen();
            int subId = info.getSubId();
            logD("updateNickNameByChatId, find info by given chatId=" + chatId + ", threadId=" + threadId);
            sMapInfo.remove(chatId);
            sMapInfo.put(chatId, new MapInfo(threadId, chatId, subject, nickName, status, isMeChairmen, subId));

            Message msg = new Message();
            msg.what = EVENT_UPDATE_NICKNAME_BY_CHATID;
            Bundle data = new Bundle();
            data.putString(ThreadMapData.KEY_CHAT_ID, chatId);
            data.putString(ThreadMapData.KEY_NICKNAME, nickName);
            msg.setData(data);
            sHandler.sendMessage(msg);
        }
    }
    
    public synchronized void updateIsMeChairmenByChatId(String chatId, boolean isMeChairmen) {
        logD("updateIsMeChairmenByChatId chatId=" + chatId + ", isMeChairmen=" + isMeChairmen);
        MapInfo info = sMapInfo.get(chatId);
        if (info != null && info.isMeChairmen() != isMeChairmen) {
            long threadId = info.getThreadId();
            String subject = info.getSubject();
            long status = info.getStatus();
            String nickName = info.getNickName();
            int subId = info.getSubId();
            logD("updateIsMeChairmenByChatId, find info by given chatId=" + chatId + ", threadId=" + threadId);
            sMapInfo.remove(chatId);
            sMapInfo.put(chatId, new MapInfo(threadId, chatId, subject, nickName, status, isMeChairmen, subId));

            Message msg = new Message();
            msg.what = EVENT_UPDATE_ISMECHAIRMEN_BY_CHATID;
            Bundle data = new Bundle();
            data.putString(ThreadMapData.KEY_CHAT_ID, chatId);
            data.putBoolean(ThreadMapData.KEY_ISCHAIRMEN, isMeChairmen);
            msg.setData(data);
            sHandler.sendMessage(msg);
        }
    }

    public synchronized void removeByChatId(String chatId) {
        logD("removeByChatId, chatId=" + chatId);
        sMapInfo.remove(chatId);
        Message msg = new Message();
        msg.what = EVENT_REMOVE_BY_CHATID;
        Bundle data = new Bundle();
        data.putString(ThreadMapData.KEY_CHAT_ID, chatId);
        msg.setData(data);
        sHandler.sendMessage(msg);
    }

    public synchronized void removeByThreadId(long threadId) {
        logD("removeByThreadId, threadId=" + threadId);
        Collection<MapInfo> infos = sMapInfo.values();
        String chatId = null;
        for (MapInfo info : infos) {
            if (info.getThreadId() == threadId) {
                chatId = info.getChatId();
                logD("removeByChatId, find it, chatId=" + chatId + ", threadId=" + threadId);
                break;
            }
        }
        sMapInfo.remove(chatId);
        Message msg = new Message();
        msg.what = EVENT_REMOVE_BY_THREADID;
        long[] ids = new long[1];
        ids[0] = threadId;
        Bundle data = new Bundle();
        data.putLongArray(ThreadMapData.KEY_THREAD_ID, ids);
        msg.setData(data);
        sHandler.sendMessage(msg);
    }

    public synchronized void addMapInfo(long threadId, String chatId, String subject, long status, boolean isMeChairmen) {
        logD("addMapInfo, threadId=" + threadId + ", chatId=" + chatId + ", subject=" + subject);
        sMapInfo.put(chatId, new MapInfo(threadId, chatId, subject, null, status, isMeChairmen, RCSUtils.getRCSSubId()));
        Message msg = new Message();
        msg.what = EVENT_ADD_MAP_DATA;
        Bundle data = new Bundle();
        data.putLong(ThreadMapData.KEY_THREAD_ID, threadId);
        data.putString(ThreadMapData.KEY_CHAT_ID, chatId);
        data.putString(ThreadMapData.KEY_SUBJECT, subject);
        data.putLong(ThreadMapData.KEY_STATUS, status);
        data.putBoolean(ThreadMapData.KEY_ISCHAIRMEN, isMeChairmen);
        msg.setData(data);
        sHandler.sendMessage(msg);
    }
    
    public void refreshByThreadsAfterDelete() {
        sHandler.sendEmptyMessage(EVENT_REFRESH_BY_THREADS);
    }

    private final class MapHandler extends Handler {
        public MapHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            switch(msg.what) {
            case EVENT_LOAD_ALL_MAP_DATA:
                loadAllMapData(mContext);
                break;
            case EVENT_UPDATE_THREADID_BY_CHATID:
                updateThreadByChatIdInDB(mContext, msg.getData());
                break;
            case EVENT_UPDATE_SUBJECT_BY_CHATID:
                updateSubjectByChatIdInDB(mContext, msg.getData());
                break;
            case EVENT_UPDATE_STATUS_BY_CHATID:
                updateStatusByChatIdInDB(mContext, msg.getData());
                break;
            case EVENT_UPDATE_ISMECHAIRMEN_BY_CHATID:
                updateIsMeChairmenByChatIdInDB(mContext, msg.getData());
                break;
//            case EVENT_UPDATE_SUBJECT_BY_THREADID:
//                updateSubjectByThreadIdInDB(mContext, msg.getData());
//                break;
            case EVENT_UPDATE_NICKNAME_BY_CHATID:
                updateNickNameByChatIdInDB(mContext, msg.getData());
                break;
            case EVENT_UPDATE_SUBID_BY_CHATID:
                updateSubIdByChatIdInDB(mContext, msg.getData());
                break;
//            case EVENT_UPDATE_NICKNAME_BY_THREADID:
//                updateNickNameByThreadIdInDB(mContext, msg.getData());
//                break;
            case EVENT_REMOVE_BY_THREADID:
                removeByThreadIdFromDB(mContext, msg.getData());
                break;
            case EVENT_REMOVE_BY_CHATID:
                removeByChatIdFromDB(mContext, msg.getData());
                break;
            case EVENT_ADD_MAP_DATA:
                addMapInfoToDB(mContext, msg.getData());
                break;
            case EVENT_REFRESH_BY_THREADS:
                refreshByThreadsInternal(mContext);
            default:
                break;
            }
        }
    }

    private synchronized void loadAllMapData(Context context) {
        Cursor cursor = null;
        try {
            cursor = ThreadMapUtils.getInstance(context).getAllMapData();
            if (cursor != null) {
                while(cursor.moveToNext()) {
                    MapInfo info = new MapInfo(cursor);
                    logD("loadAllMapData, threadId=" + info.getThreadId() +
                            ", chatId=" + info.getChatId() + ", subject=" + info.getSubject());
                    sMapInfo.put(info.getChatId(), info);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
            Logger.e(TAG, "[loadAllMapData] exception : e = " +e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void updateThreadByChatIdInDB(Context context, Bundle data) {
        if (data == null ||
                !data.containsKey(ThreadMapData.KEY_CHAT_ID) ||
                !data.containsKey(ThreadMapData.KEY_THREAD_ID)) {
                return;
        }
        String chatId = data.getString(ThreadMapData.KEY_CHAT_ID);
        long threadId = data.getLong(ThreadMapData.KEY_THREAD_ID);
        ThreadMapUtils.getInstance(context).updateThreadId(chatId, threadId); 
    }

    private void updateSubjectByChatIdInDB(Context context, Bundle data) {
        if (data == null ||
            !data.containsKey(ThreadMapData.KEY_CHAT_ID) ||
            !data.containsKey(ThreadMapData.KEY_SUBJECT)) {
            return;
        }
        String chatId = data.getString(ThreadMapData.KEY_CHAT_ID);
        String subject = data.getString(ThreadMapData.KEY_SUBJECT);
        ThreadMapUtils.getInstance(context).updateSubject(chatId, subject);
    }

    private void updateIsMeChairmenByChatIdInDB(Context context, Bundle data) {
        if (data == null ||
                !data.containsKey(ThreadMapData.KEY_CHAT_ID) ||
                !data.containsKey(ThreadMapData.KEY_ISCHAIRMEN)) {
                return;
            }
        String chatId = data.getString(ThreadMapData.KEY_CHAT_ID);
        boolean isMeChairmen = data.getBoolean(ThreadMapData.KEY_ISCHAIRMEN);
        ThreadMapUtils.getInstance(context).updateChairmen(chatId, isMeChairmen);
    }

    private void updateStatusByChatIdInDB(Context context, Bundle data) {
        if (data == null ||
                !data.containsKey(ThreadMapData.KEY_CHAT_ID) ||
                !data.containsKey(ThreadMapData.KEY_STATUS)) {
            return;
        }
        String chatId = data.getString(ThreadMapData.KEY_CHAT_ID);
        long status = data.getLong(ThreadMapData.KEY_STATUS);
        ThreadMapUtils.getInstance(context).updateStatus(chatId, status);
    }
    
    private void updateNickNameByChatIdInDB(Context context, Bundle data) {
        if (data == null ||
                !data.containsKey(ThreadMapData.KEY_CHAT_ID) ||
                !data.containsKey(ThreadMapData.KEY_NICKNAME)) {
            return;
        }
        String chatId = data.getString(ThreadMapData.KEY_CHAT_ID);
        String nickName = data.getString(ThreadMapData.KEY_NICKNAME);
        ThreadMapUtils.getInstance(context).updateNickName(chatId, nickName);
    }
    
    private void updateSubIdByChatIdInDB(Context context, Bundle data) {
        if (data == null ||
                !data.containsKey(ThreadMapData.KEY_CHAT_ID) ||
                !data.containsKey(ThreadMapData.KEY_SUB_ID)) {
            return;
        }
        String chatId = data.getString(ThreadMapData.KEY_CHAT_ID);
        int subId = data.getInt(ThreadMapData.KEY_SUB_ID);
        ThreadMapUtils.getInstance(context).updateSubId(chatId, subId);
    }

    private void removeByChatIdFromDB(Context context, Bundle data) {
        if (data == null ||
                !data.containsKey(ThreadMapData.KEY_CHAT_ID)) {
            return;
        }
        String chatId = data.getString(ThreadMapData.KEY_CHAT_ID);
        ThreadMapUtils.getInstance(context).deleteMapData(chatId);
    }

    private void removeByThreadIdFromDB(Context context, Bundle data) {
        if (data == null ||
                !data.containsKey(ThreadMapData.KEY_THREAD_ID)) {
            return;
        }
        long[] threadIds = data.getLongArray(ThreadMapData.KEY_THREAD_ID);
        Set<Long> ids = new HashSet<Long>();
        for (int i = 0; i < threadIds.length; i++) {
            ids.add(threadIds[i]);
        }
        ThreadMapUtils.getInstance(context).deleteMapDatas(ids);
    }

    private void addMapInfoToDB(Context context, Bundle data) {
        long threadId = data.getLong(ThreadMapData.KEY_THREAD_ID);
        String chatId = data.getString(ThreadMapData.KEY_CHAT_ID);
        String subject = data.getString(ThreadMapData.KEY_SUBJECT);
        long status = data.getLong(ThreadMapData.KEY_STATUS);
        int isMeChairmen = data.getBoolean(ThreadMapData.KEY_ISCHAIRMEN) ? 1 : 0;
        ThreadMapUtils.getInstance(context).insertMapData(threadId, chatId, subject, status, isMeChairmen);
    }

    private synchronized void refreshByThreadsInternal(Context context) {
        Uri uri = Uri.parse("content://mms-sms/conversations");
        Uri.Builder builder = uri.buildUpon();
        builder.appendQueryParameter("simple", "true");
        String[] projection = {"_id"};
        Cursor cursor = context.getContentResolver().query(builder.build(), projection, "status<>0", null, null);
        Set<Long> threadIds = new HashSet<Long>();
        if (cursor != null) {
            try {
                while (cursor.moveToFirst()) {
                    threadIds.add(cursor.getLong(cursor.getColumnIndexOrThrow("_id")));
                }
            } finally {
                cursor.close();
            }
        }
        Collection<MapInfo> infos = sMapInfo.values();
        Set<Long> deleteThreadIds = new HashSet<Long>();
        for (MapInfo info : infos) {
            if (threadIds.contains(Long.valueOf(info.getThreadId()))) {
                deleteThreadIds.add(Long.valueOf(info.getThreadId()));
                sMapInfo.remove(Long.valueOf(info.getThreadId()));
            }
        }
        ThreadMapUtils.getInstance(context).deleteMapDatas(deleteThreadIds);
    }

    private static void logD(String string) {
        Logger.d(TAG, string);
    }

    public class MapInfo {

        private long mThreadId;
        private String mChatId;
        private String mSubject;
        private String mNickName;
        private long mStatus;
        private boolean mIsMeChairmen;
        private int mSubId;

        MapInfo(Cursor cursor) {
            if (cursor != null && cursor.getPosition() > -1) {
                mThreadId = cursor.getLong(cursor.getColumnIndexOrThrow(ThreadMapData.KEY_THREAD_ID));
                mChatId = cursor.getString(cursor.getColumnIndexOrThrow(ThreadMapData.KEY_CHAT_ID));
                mSubject = cursor.getString(cursor.getColumnIndexOrThrow(ThreadMapData.KEY_SUBJECT));
                mNickName = cursor.getString(cursor.getColumnIndexOrThrow(ThreadMapData.KEY_NICKNAME));
                mStatus = cursor.getLong(cursor.getColumnIndexOrThrow(ThreadMapData.KEY_STATUS));
                mIsMeChairmen = cursor.getInt(cursor.getColumnIndexOrThrow(ThreadMapData.KEY_ISCHAIRMEN)) > 0 ? true : false;
                mSubId = cursor.getInt(cursor.getColumnIndexOrThrow(ThreadMapData.KEY_SUB_ID));
            }
        }

        MapInfo(long threadId, String chatId, String subject) {
            mThreadId = threadId;
            mChatId   = chatId;
            mSubject  = subject;
            mNickName = null;
            mStatus = 0;
            mIsMeChairmen = false;
            mSubId = RCSUtils.getRCSSubId();
        }

        MapInfo(long threadId, String chatId, String subject, String nickName, long status, boolean isMeChairmen, int subId) {
            mThreadId = threadId;
            mChatId   = chatId;
            mSubject  = subject;
            mNickName = nickName;
            mStatus = status;
            mIsMeChairmen = isMeChairmen;
            mSubId = subId;
        }

        public long getThreadId() {
            return mThreadId;
        }

        public String getChatId() {
            return mChatId;
        }

        public String getSubject() {
            return mSubject;
        }

        public String getNickName() {
            return mNickName;
        }

        public long getStatus() {
            return mStatus;
        }

        public boolean isMeChairmen() {
            return mIsMeChairmen;
        }

        public int getSubId() {
            return mSubId;
        }
    }
}
