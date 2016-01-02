package com.mediatek.rcs.common.provider;

import java.util.Set;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.mediatek.rcs.common.utils.Logger;
import com.mediatek.rcs.common.utils.RCSUtils;

public class ThreadMapUtils {

    private static ThreadMapUtils sInstance;
    private ContentResolver mCr;
    private static final String TAG = "ThreadMapUtils";
    
    private ThreadMapUtils(Context context) {
        mCr = context.getContentResolver();
    }
    
    public synchronized static ThreadMapUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ThreadMapUtils(context);
        }
        return sInstance;
    }
    
    public static final String[] MAP_PROJECTION = {
        ThreadMapData.KEY_ID,
        ThreadMapData.KEY_THREAD_ID,
        ThreadMapData.KEY_CHAT_ID,
        ThreadMapData.KEY_SUBJECT,
        ThreadMapData.KEY_NICKNAME,
        ThreadMapData.KEY_STATUS,
        ThreadMapData.KEY_SUB_ID,
        ThreadMapData.KEY_ISCHAIRMEN
    };
    
    public Uri insertMapData(long threadId, String chatId, String subject, long status, int isMeChairmen) {
        Logger.d(TAG, "insertMapData(), threadId=" + threadId + ", chatId=" + chatId
                + ", subject=" + subject + ", status=" + status);
        ContentValues cv = new ContentValues(3);
        cv.put(ThreadMapData.KEY_THREAD_ID, threadId);
        cv.put(ThreadMapData.KEY_CHAT_ID, chatId);
        cv.put(ThreadMapData.KEY_SUBJECT, subject);
        cv.put(ThreadMapData.KEY_STATUS, status);
        cv.put(ThreadMapData.KEY_ISCHAIRMEN, isMeChairmen);
        cv.put(ThreadMapData.KEY_SUB_ID, RCSUtils.getRCSSubId());
        return mCr.insert(ThreadMapData.CONTENT_URI, cv);
    }
    
    public int deleteMapData(long threadId) {
        Logger.d(TAG, "deleteMapData(), threadId=" + threadId);
        String selection = ThreadMapData.KEY_THREAD_ID + "=" + threadId;
        return mCr.delete(ThreadMapData.CONTENT_URI, selection, null);
    }
    
    public int deleteMapDatas(Set<Long> threadIds) {
        String where = ThreadMapData.KEY_THREAD_ID + formatIdInClause(threadIds);
        Logger.d(TAG, "deleteMapData(), threadIds=" + where);
        return mCr.delete(ThreadMapData.CONTENT_URI, where, null);
    }

    public int deleteMapData(String chatId) {
        Logger.d(TAG, "deleteMapData(), chatId=" + chatId);
        String selection = ThreadMapData.KEY_CHAT_ID + "='" + chatId + "'";
        return mCr.delete(ThreadMapData.CONTENT_URI, selection, null);
    }

    public boolean updateThreadId(String chatId, long threadId) {
        Logger.d(TAG, "updateThreadId(), threadId=" + threadId + ", chatId=" + chatId);
        String selection = ThreadMapData.KEY_CHAT_ID + "='" + chatId + "'";
        ContentValues cv = new ContentValues(1);
        cv.put(ThreadMapData.KEY_THREAD_ID, threadId);
        int count = mCr.update(ThreadMapData.CONTENT_URI, cv, selection, null);
        return count > 0 ? true : false;
    }

    public boolean updateSubId(String chatId, int subId) {
        Logger.d(TAG, "updateThreadId(), subId=" + subId + ", chatId=" + chatId);
        String selection = ThreadMapData.KEY_CHAT_ID + "='" + chatId + "'";
        ContentValues cv = new ContentValues(1);
        cv.put(ThreadMapData.KEY_SUB_ID, subId);
        int count = mCr.update(ThreadMapData.CONTENT_URI, cv, selection, null);
        return count > 0 ? true : false;
    }

    public boolean updateSubject(long threadId, String subject) {
        Logger.d(TAG, "updateSubject(), threadId=" + threadId + ", subject=" + subject);
        String selection = ThreadMapData.KEY_THREAD_ID + "=" + threadId;
        ContentValues cv = new ContentValues(1);
        cv.put(ThreadMapData.KEY_SUBJECT, subject);
        int count = mCr.update(ThreadMapData.CONTENT_URI, cv, selection, null);
        return count > 0 ? true : false;
    }

    public boolean updateChairmen(String chatId, boolean isChairmen) {
        Logger.d(TAG, "updateChairmen(), chatId=" + chatId + ", isChairmen=" + isChairmen);
        String selection = ThreadMapData.KEY_CHAT_ID + "='" + chatId + "'";
        ContentValues cv = new ContentValues(1);
        int isMeChairmen = isChairmen ? 1 : 0;
        cv.put(ThreadMapData.KEY_ISCHAIRMEN, isMeChairmen);
        int count = mCr.update(ThreadMapData.CONTENT_URI, cv, selection, null);
        return count > 0 ? true : false;
    }

    public boolean updateStatus(String chatId, long status) {
        Logger.d(TAG, "updateStatus(), chatId=" + chatId + ", status=" + status);
        String selection = ThreadMapData.KEY_CHAT_ID + "='" + chatId + "'";
        ContentValues cv = new ContentValues(1);
        cv.put(ThreadMapData.KEY_STATUS, status);
        int count = mCr.update(ThreadMapData.CONTENT_URI, cv, selection, null);
        return count > 0 ? true : false;
    }

    public boolean updateSubject(String chatId, String subject) {
        Logger.d(TAG, "updateSubject(), chatId=" + chatId + ", subject=" + subject);
        String selection = ThreadMapData.KEY_CHAT_ID + "='" + chatId + "'";
        ContentValues cv = new ContentValues(1);
        cv.put(ThreadMapData.KEY_SUBJECT, subject);
        int count = mCr.update(ThreadMapData.CONTENT_URI, cv, selection, null);
        return count > 0 ? true : false;
    }

    public boolean updateNickName(long threadId, String nickName) {
        Logger.d(TAG, "updateNickName(), threadId=" + threadId + ", nickName=" + nickName);
        String selection = ThreadMapData.KEY_THREAD_ID + "=" + threadId;
        ContentValues cv = new ContentValues(1);
        cv.put(ThreadMapData.KEY_NICKNAME, nickName);
        int count = mCr.update(ThreadMapData.CONTENT_URI, cv, selection, null);
        return count > 0 ? true : false;
    }

    public boolean updateNickName(String chatId, String nickName) {
        Logger.d(TAG, "updateNickName(), chatId=" + chatId + ", nickName=" + nickName);
        String selection = ThreadMapData.KEY_CHAT_ID + "='" + chatId + "'";
        ContentValues cv = new ContentValues(1);
        cv.put(ThreadMapData.KEY_NICKNAME, nickName);
        int count = mCr.update(ThreadMapData.CONTENT_URI, cv, selection, null);
        return count > 0 ? true : false;
    }
    
    public Cursor getAllMapData() {
        Logger.d(TAG, "getAllMapData()");
        return mCr.query(ThreadMapData.CONTENT_URI, MAP_PROJECTION, null, null, null);
    }

    public Cursor getMapDataByThreadId(long threadId) {
        Logger.d(TAG, "getMapDataByThreadId(), threadId=" + threadId);
        return mCr.query(ThreadMapData.CONTENT_URI, MAP_PROJECTION,
                ThreadMapData.KEY_THREAD_ID + "=" + threadId, null, null);
    }

    public Cursor getMapDataByChatId(String chatId) {
        Logger.d(TAG, "getMapDataByChatId(), chatId=" + chatId);
        return mCr.query(ThreadMapData.CONTENT_URI, MAP_PROJECTION,
                ThreadMapData.KEY_CHAT_ID + "='" + chatId + "'", null, null);
    }

    private String formatIdInClause(Set<Long> ids) {
        /* to IN sql */
        if (ids == null || ids.size() == 0) {
            return " IN ()";
        }
        String in = " IN ";
        in += ids.toString();
        in = in.replace('[', '(');
        in = in.replace(']', ')');
        return in;
    }
}
