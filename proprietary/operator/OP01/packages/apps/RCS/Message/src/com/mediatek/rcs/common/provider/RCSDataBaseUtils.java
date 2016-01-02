package com.mediatek.rcs.common.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.gsma.joyn.chat.ChatLog;
import org.gsma.joyn.ft.FileTransfer;
import org.gsma.joyn.ft.FileTransferLog;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;

import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.common.provider.ThreadMapCache.MapInfo;
import com.mediatek.rcs.common.service.Participant;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.common.utils.Logger;
import com.mediatek.rcs.common.utils.RCSUtils;

public class RCSDataBaseUtils {
    private static final String TAG = "RCSUtils";

    private static Context sHostContext = ContextCacher.getHostContext();
    private static ContentResolver sContentResolver = ContextCacher.getHostContext()
            .getContentResolver();

    /**
     * URI for mark a conversation as top
     */
    public static final Uri MMS_SMS_URI_TOP = Uri.parse("content://mms-sms/rcs/top");
    public static final Uri MMS_SMS_URI_ADD_THREAD = Uri.parse("content://mms-sms/rcs/thread");
    public static final Uri MMS_SMS_URI_DELETE_GROUP_THREADS = Uri.parse("content://mms-sms/rcs/group_conversations");
    public static final Uri URI_GROUP_SYS_MESSAGE = Uri.parse("content://system-ipmsg");
    public static final Uri MMS_SMS_URI_GROUP_ADDRESS = Uri.parse("content://mms-sms/rcs/thread_addr");

    /**
     * URI for get all messages by threadId, including group chat system msgs
     */
    public static final Uri MMS_SMS_URI_CONVERSATION_MESSAGES = Uri
            .parse("content://mms-sms/rcs/conversations");

    public static final Uri RCS_URI_MESSAGE = ChatLog.Message.CONTENT_URI;
    public static final Uri RCS_URI_GROUP_CHAT = ChatLog.GroupChat.CONTENT_URI;
    public static final Uri RCS_URI_MULTI_MESSAGE = ChatLog.MultiMessage.CONTENT_URI;
    public static final Uri RCS_URI_FT = FileTransferLog.CONTENT_URI;
    public static final Uri RCS_URI_GROUP_MEMBER = ChatLog.GroupChatMember.CONTENT_URI;

    public static final Uri MMS_SMS_URI_THREAD_SETTINGS = Uri.parse("content://mms-sms/thread_settings/");
    public static final Uri SMS_CONTENT_URI = Sms.CONTENT_URI;
    public static final Uri MMS_SMS_CREATE_CHATS = Uri.parse("content://mms-sms/conversations");
    public static final Uri MMS_SMS_ADDRESSES = Uri.parse("content://mms-sms/canonical-addresses");
    public static final Uri URI_THREADS_UPDATE_STATUS = Uri.parse("content://mms-sms/conversations/status");

    private static final String RCS_BLACK_LIST_URI = "content://com.cmcc.ccs.black_list/black_list";
    private static final String[] PROJECTION_BLACK_LIST = { "PHONE_NUMBER" };

    private static final String TYPE = "type";
    public static final String KEY_EVENT_ROW_ID = "_id";
    public static final String KEY_EVENT_MESSAGE_ID = "msg_id";
    public static final String KEY_EVENT_FT_ID = "ft_id";

    public static final int MESSAGE_TYPE_TEXT = 1;
    public static final int MESSAGE_TYPE_FT = 2;

    private static final int ERROR_IPMSG_ID = 0;

    // add by feng
    /** Image */
    public static final String FILE_TYPE_IMAGE = "image";
    /** Audio */
    public static final String FILE_TYPE_AUDIO = "audio";
    /** Video */
    public static final String FILE_TYPE_VIDEO = "video";
    /** Text */
    public static final String FILE_TYPE_TEXT = "text";
    /** Application */
    public static final String FILE_TYPE_APP = "application";
    private static String COUNTRY_CODE = "+34";
    /**
     * M: Added to avoid the magic number problem @{T-Mobile
     */
    private static final String INTERNATIONAL_PREFIX = "00";
    /** T-Mobile@} */
    /** M: add for format UUSD and star codes @{T-Mobile */
    private static final String Tel_URI_PREFIX = "tel:";
    private static final String SIP_URI_PREFIX = "sip:";
    private static final String AT_SIGN = "@";
    private static final String POUND_SIGN = "#";
    private static final String POUND_SIGN_HEX_VALUE = "23%";
    /** T-Mobile@} */
    /**
     * Country area code
     */
    private static String COUNTRY_AREA_CODE = "0";
    private static String COUNTRY_CODE_PLUS = "+";
    private static final String METHOD_GET_METADATA = "getMetadataForRegion";
    private static final String REGION_TW = "TW";
    private static final String INTERNATIONAL_PREFIX_TW = "0(?:0[25679] | 16 | 17 | 19)";

    public static final String[] GET_IP_MSG_ID_PROJECTION = { KEY_EVENT_ROW_ID,
            Sms.IPMSG_ID };
    public static final String[] PROJECTION_WITH_THREAD = { KEY_EVENT_ROW_ID,
            Sms.THREAD_ID, Sms.ADDRESS, Sms.BODY };
    public static final String[] PROJECTION_ONLY_ID = { KEY_EVENT_ROW_ID };
    public static final String[] PROJECTION_MESSAGE_ID = { KEY_EVENT_MESSAGE_ID };
    public static final String[] PROJECTION_FILETRANSFER_ID = { KEY_EVENT_FT_ID };
    public static final String[] PROJECTION_CREATE_RCS_CHATS = { Threads._ID,
            Threads.RECIPIENT_IDS, Threads.STATUS };

    public static final String[] PROJECTION_GROUP_INFO = {
            ChatLog.GroupChat.ID, ChatLog.GroupChat.CHAT_ID,
            ChatLog.GroupChat.CHAIRMAN, ChatLog.GroupChat.NICKNAME,
            ChatLog.GroupChat.SUBJECT, ChatLog.GroupChat.PARTICIPANTS_LIST };

    public static final String[] PROJECTION_GROUP_MEMBER = {
		 GroupMemberData.COLUMN_CHAT_ID
		,GroupMemberData.COLUMN_CONTACT_NUMBER
		,GroupMemberData.COLUMN_CONTACT_NAME
		,GroupMemberData.COLUMN_STATE
		,GroupMemberData.COLUMN_PORTRAIT
		,GroupMemberData.COLUMN_TYPE};


    public static final String[] PROJECTION_TEXT_IP_MESSAGE = {
            ChatLog.Message.ID, ChatLog.Message.DIRECTION,
            ChatLog.Message.MESSAGE_ID, ChatLog.Message.MESSAGE_STATUS,
            ChatLog.Message.BODY, ChatLog.Message.TIMESTAMP,
            ChatLog.Message.CONTACT_NUMBER, ChatLog.Message.MIME_TYPE,
            ChatLog.Message.TIMESTAMP_DELIVERED, ChatLog.Message.MESSAGE_TYPE };
    // add by feng
    public static final String[] PROJECTION_FILE_TRANSFER = {
            FileTransferLog.ID, FileTransferLog.FT_ID,
            FileTransferLog.CONTACT_NUMBER, FileTransferLog.FILENAME,
            FileTransferLog.FILESIZE, FileTransferLog.MIME_TYPE,
            FileTransferLog.DIRECTION, FileTransferLog.TRANSFERRED,
            FileTransferLog.TIMESTAMP, FileTransferLog.TIMESTAMP_SENT,
            FileTransferLog.TIMESTAMP_DELIVERED,
            FileTransferLog.TIMESTAMP_DISPLAYED, FileTransferLog.STATE,
            FileTransferLog.FILEICON, FileTransferLog.CHAT_ID,
            FileTransferLog.MSG_ID, FileTransferLog.DURATION,
            FileTransferLog.SESSION_TYPE };
    public static final String[] PROJECTION_WITH_TYPE = { KEY_EVENT_ROW_ID,
            Sms.THREAD_ID, Sms.ADDRESS, Sms.BODY, Sms.TYPE };


    public static void setContext(Context context) {
        sHostContext = context;
        sContentResolver = context.getContentResolver();
    }

    public static long getThreadIdByChatId(String chatId) {
        Cursor cursor = sContentResolver.query(ThreadMapData.CONTENT_URI,
                ThreadMapUtils.MAP_PROJECTION, ThreadMapData.KEY_CHAT_ID + "='" + chatId + "'",
                null, null);
        long threadId = 0;
        try {
            if (cursor != null && cursor.moveToFirst()) {
                threadId = cursor.getLong(cursor.getColumnIndex(ThreadMapData.KEY_THREAD_ID));
                if (threadId == RCSUtils.DELETED_THREAD_ID) {
                    //unused chatId. group is deleted before.
                    Logger.d(TAG, "[getThreadIdByChatId], chatId=" + chatId +
                                                                    "threadid = " + threadId);
                    List<Participant> participants = getGroupAvailableParticipants(chatId);
                    threadId = createGroup(participants, false);
                    String selection = ThreadMapData.KEY_CHAT_ID + "='" + chatId + "'";
                    ContentValues cv = new ContentValues(1);
                    cv.put(ThreadMapData.KEY_THREAD_ID, threadId);
                    sContentResolver.update(ThreadMapData.CONTENT_URI, cv, selection, null);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return threadId;
    }

    public static String getContactDisplayName(Context context, String number) {
        Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number));
        Cursor cursor = context.getContentResolver().query(uri,
                new String[]{PhoneLookup._ID, PhoneLookup.DISPLAY_NAME},
                null, null, null);
        String name = number;
        try {
            if (cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Logger.d(TAG, "contact name=" + name + " for number " + number);
        return name;
    }

    /**
     * If is spam message, return true, else return false.
     */
    public static boolean isIpSpamMessage(Context context, String number) {
        Cursor cursor = context.getContentResolver().query(
                Uri.parse(RCS_BLACK_LIST_URI), PROJECTION_BLACK_LIST, null,
                null, null);
        if (cursor == null) {
            Log.d(TAG, "isIpSpamMessage, cursor is null...");
            return false;
        }
        String blockNumber;
        boolean result = false;
        try {
            while (cursor.moveToNext()) {
                blockNumber = cursor.getString(0);
                if (PhoneNumberUtils.compare(number, blockNumber)) {
                    result = true;
                    break;
                }
            }
        } finally {
            cursor.close();
        }
        Log.d(TAG, "isIpSpamMessage, number=" + number + ", result=" + result);
        return result;
    }

    /**
     * 
     * @param context
     * @param threadId
     * @param isTop
     * @return
     */
    public static boolean markConversationTop(Context context, long threadId,
            boolean isTop) {
        long time = isTop ? System.currentTimeMillis() : 0;
        Uri markTopUri = ContentUris.withAppendedId(MMS_SMS_URI_TOP, threadId);
        ContentValues cv = new ContentValues(1);
        cv.put("top", time);
        int count = context.getContentResolver().update(markTopUri, cv, null,
                null);
        return count > 0 ? true : false;
    }

    public static Uri addTextMultiMessage(ContentResolver resolver,
            long ipMsgId, String body, String contact, int boxType,
            long targetThreadId, long subId) {
        ContentValues cv = new ContentValues();
        cv.put(Sms.ADDRESS, contact);
        cv.put(Sms.BODY, body);
        cv.put(Sms.IPMSG_ID, ipMsgId);
        /*** fix me ****/
        cv.put(Sms.SUBSCRIPTION_ID, subId);
        // cv.put(Sms.PROTOCOL,
        // IpMessageConsts.MessageProtocolType.RCS_MULTI_PROTO);
        cv.put(Sms.REPLY_PATH_PRESENT, 0);
        if (targetThreadId <= 0) {
            Logger.d(TAG, "addTextMultiMessage fail, error threadId = 0");
            return null;
        }
        cv.put(Sms.THREAD_ID, targetThreadId);
        cv.put(TYPE, boxType);
        if (boxType == Sms.MESSAGE_TYPE_INBOX) {
            cv.put(Sms.READ, 0);
        }
        return resolver.insert(SMS_CONTENT_URI, cv);
    }

    public static Uri addFTMultiMessage(ContentResolver resolver, long ipMsgId,
            String body, String contact, int boxType, long targetThreadId,
            long subId) {
        return addTextMultiMessage(resolver, -ipMsgId, body, contact, boxType,
                targetThreadId, subId);
    }

    public static long findTextIdInRcseDb(ContentResolver resolver, String msgId, int direction) {
        if (TextUtils.isEmpty(msgId)) {
            Logger.e(TAG, "findTextIdInRcseDb(), invalid msgId: " + msgId);
            return ERROR_IPMSG_ID;
        }
        Uri uri = null;
        String where = null;
        uri = RCS_URI_MESSAGE;
        where = ChatLog.Message.MESSAGE_ID + "='" + msgId + "'" + " AND " +
                ChatLog.Message.DIRECTION + "=" + direction;
        Cursor cursor = resolver.query(uri, PROJECTION_ONLY_ID, where, null,
                null);
        try {
            if (null != cursor && cursor.moveToFirst()) {
                return cursor.getLong(cursor
                        .getColumnIndex(KEY_EVENT_ROW_ID));
            }
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
        return ERROR_IPMSG_ID;
    }

    public static long findFTIdInRcseDb(ContentResolver resolver, String ftId) {
        Logger.d(TAG, "findFTIdInRcseDb() entry, msgId: " + ftId);
        return findIdInRcsDb(resolver, MESSAGE_TYPE_FT, ftId);
    }

    private static long findIdInRcsDb(ContentResolver resolver, int msgType,
            String id) {
        Logger.d(TAG, "findIdInRcseDb() entry, msgId: " + id + ",  msgType="
                + msgType);

        if (TextUtils.isEmpty(id)) {
            Logger.e(TAG, "findIdInRcseDb(), invalid msgId: " + id);
            return ERROR_IPMSG_ID;
        }
        Uri uri = null;
        String where = null;
        if (msgType == MESSAGE_TYPE_TEXT) {
            uri = RCS_URI_MESSAGE;
            where = KEY_EVENT_MESSAGE_ID + "='" + id + "'";
        } else if (msgType == MESSAGE_TYPE_FT) { // add by feng
            uri = RCS_URI_FT;
            where = KEY_EVENT_FT_ID + "='" + id + "'";
        } else {
            Logger.e(TAG, "findIdInRcseDb(), invalid msgType: " + msgType);
            return ERROR_IPMSG_ID;
        }
        Cursor cursor = resolver.query(uri, PROJECTION_ONLY_ID, where, null,
                null);
        try {
            if (null != cursor && cursor.moveToFirst()) {
                long rowId = cursor.getLong(cursor
                        .getColumnIndex(KEY_EVENT_ROW_ID));
                Logger.d(TAG, "findIdInRcseDb() row id for " + id + ", is "
                        + rowId);
                return rowId;
            } else {
                Logger.w(TAG, "findIdInRcseDb() invalid cursor: " + cursor);
            }
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
        return ERROR_IPMSG_ID;
    }

    public static Uri storeFTMessageInDatabase(ContentResolver resolver,
            String ftId, String text, String contact, int boxType,
            long threadId, long subId, boolean isBurnMessage) {
        Logger.d(TAG, "storeMessageInDatabase() with messageId = " + ftId
                + " ,text = " + text + " ,contact = " + contact
                + " ,and boxType = " + boxType + " ,and isBurnMessage = "
                + isBurnMessage);
        long ipMsgId = ERROR_IPMSG_ID;
        ipMsgId = findFTIdInRcseDb(resolver, ftId);
        if (ERROR_IPMSG_ID < ipMsgId) {
            return storeMessageInMmsDb(resolver, -ipMsgId, text, contact,
                    boxType, threadId, subId, isBurnMessage);
        } else {
            Logger.w(TAG,
                    "storeMessageInDatabase() message not found in Rcse DB ,");
            return null;
        }
    }

    public static Uri addTextIpMessage(ContentResolver resolver, long ipMsgId,
            String body, String contact, int boxType, long threadId,
            long subId, boolean isBurnMessage) {
        Logger.d(TAG, "addIpTextMessage(), body=" + body + ", contact="
                + contact + ", boxType=" + boxType + ", threadId=" + threadId
                + ", isBurnMessage=" + isBurnMessage);
        return storeMessageInMmsDb(resolver, ipMsgId, body, contact, boxType,
                threadId, subId, isBurnMessage);
    }

    public static Uri addFTIpMessage(ContentResolver resolver, long ipMsgId,
            String body, String contact, int boxType, long threadId,
            long subId, boolean isBurnMessage) {
        Logger.d(TAG, "addFTIpMessage(), body=" + body + ", contact=" + contact
                + ", boxType=" + boxType + ", threadId=" + threadId
                + ", isBurnMessage=" + isBurnMessage);
        return storeMessageInMmsDb(resolver, -ipMsgId, body, contact, boxType,
                threadId, subId, isBurnMessage);
    }

    public static Uri storeMessageInMmsDb(ContentResolver resolver,
            long ipMsgId, String text, String contact, int boxType,
            long threadId, long subId, boolean isBurnMessage) {
//        long idInMmsDb = getIdInMmsDb(resolver, ipMsgId);
//        if (0 < idInMmsDb) {
//            Logger.d(TAG,
//                    "storeMessageInDatabase() message found in Mms DB, id: "
//                            + idInMmsDb);
//            return ContentUris.withAppendedId(SMS_CONTENT_URI, idInMmsDb);
//        } else {
//            Logger.d(TAG,
//                    "storeMessageInDatabase() message not found in Mms DB");
            // TODO discuss with mengjie, what to save in contact field for
            // group
            // if (TextUtils.isEmpty(contact)) {
            // Logger.w(TAG, "storeMessageInDatabase() invalid remote: " +
            // contact);
            // return null;
            // }
        return insertDatabase(resolver, text, contact, ipMsgId, boxType,
                threadId, subId, isBurnMessage);
//        }
    }

    public static long getIdInMmsDb(ContentResolver contentResolver,
            long ipMsgId) {
        Logger.d(TAG, "getIdInMmsDb() entry, ipMsgId: " + ipMsgId);

        Cursor cursor = contentResolver.query(SMS_CONTENT_URI,
                PROJECTION_WITH_THREAD, Sms.IPMSG_ID + "=" + ipMsgId, null,
                null);
        try {
            if (cursor.moveToFirst()) {
                long mmsDbId = cursor.getLong(cursor.getColumnIndex(Sms._ID));
                long threadId = cursor.getLong(cursor
                        .getColumnIndex(Sms.THREAD_ID));
                String contact = cursor.getString(cursor
                        .getColumnIndex(Sms.ADDRESS));
                Logger.d(TAG, "getIdInMmsDb() contact is " + contact
                        + " threadId is " + threadId);
                Logger.d(TAG, "getIdInMmsDb() mmsDbId: " + mmsDbId);
                return mmsDbId;
            } else {
                Logger.d(TAG, "getIdInMmsDb() empty cursor");
                return 0;
            }
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
    }

    // add by feng
    public static int getIdAndTypeInMmsDb(ContentResolver contentResolver,
            long ipMsgId) {
        Log.d(TAG, "getIdInMmsDb() entry, ipMsgId: " + ipMsgId);

        Cursor cursor = contentResolver.query(SMS_CONTENT_URI,
                PROJECTION_WITH_TYPE, Sms.IPMSG_ID + "=" + ipMsgId, null, null);
        try {
            if (cursor.moveToFirst()) {
                long mmsDbId = cursor.getLong(cursor.getColumnIndex(Sms._ID));
                long threadId = cursor.getLong(cursor
                        .getColumnIndex(Sms.THREAD_ID));
                String contact = cursor.getString(cursor
                        .getColumnIndex(Sms.ADDRESS));
                int boxType = cursor.getInt(cursor.getColumnIndex(Sms.TYPE));
                Log.d(TAG, "getIdInMmsDb() contact is " + contact
                        + " threadId is " + threadId);
                Log.d(TAG, "yangfeng test getIdInMmsDb() mmsDbId: " + mmsDbId);
                Log.d(TAG, "yangfeng test getIdInMmsDb() boxType: " + boxType);
                return boxType;
            } else {
                Log.d(TAG, "getIdInMmsDb() empty cursor");
                return -1;
            }
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
    }

    public static Cursor getInfoInMmsDbByIpMsgId(ContentResolver resolver,
            long ipMsgId) {
        Logger.d(TAG, "getIdInMmsDb() entry, ipMsgId: " + ipMsgId);

        Cursor cursor = resolver.query(SMS_CONTENT_URI, PROJECTION_WITH_THREAD,
                Sms.IPMSG_ID + "=" + ipMsgId, null, null);
        return cursor;
    }

//    private static Uri insertDatabase(ContentResolver resolver, String body,
//            String contact, long ipMsgId, int boxType, long subId) {
//        return insertDatabase(resolver, body, contact, ipMsgId, boxType, 0,
//                subId, false);
//    }

    private static Uri insertDatabase(ContentResolver resolver, String body,
            String contact, long ipMsgId, int boxType, long targetThreadId,
            long subId, boolean isBurnMessage) {
        Logger.d(TAG, "InsertDatabase(), body = " + body + "contact is "
                + contact + " , boxType: " + boxType + " , threadId: "
                + targetThreadId + " , isBurnMessage: " + isBurnMessage);
        ContentValues cv = new ContentValues();
        cv.put(Sms.ADDRESS, contact);
        cv.put(Sms.BODY, body);
        cv.put(Sms.IPMSG_ID, ipMsgId);
        cv.put(Sms.SUBSCRIPTION_ID, subId);
        if (isBurnMessage) {
            cv.put(Sms.PROTOCOL, IpMessageConsts.MessageProtocolType.RCS_BURN_PROTO);
        } else {
            cv.put(Sms.PROTOCOL, 0);
        }
        cv.put(Sms.REPLY_PATH_PRESENT, 0);
        if (targetThreadId > 0) {
            cv.put(Sms.THREAD_ID, targetThreadId);
        }
        cv.put(TYPE, boxType);
        if (boxType == Sms.MESSAGE_TYPE_INBOX) {
            cv.put(Sms.READ, 0);
        }
        Uri smsUri = SMS_CONTENT_URI;
        if (isBurnMessage) {
            smsUri = smsUri.buildUpon()
                    .appendQueryParameter("readBurnMessage", "true").build();
        }
        return resolver.insert(smsUri, cv);
    }

    public static int updateTextIpMsgId(ContentResolver resolver, long smsId,
            long ipMsgId) {
        Logger.d(TAG, "updateTextIpMsgId(), smsId=" + smsId + ", ipMsgId="
                + ipMsgId);
        ContentValues values = new ContentValues();
        values.put("ipmsg_id", ipMsgId);
        return resolver.update(SMS_CONTENT_URI, values, "_id=" + smsId, null);

    }

    public static int updateFTIpMsgId(ContentResolver resolver, long smsId,
            long ipMsgId) {
        Logger.d(TAG, "updateFTIpMsgId(), smsId=" + smsId + ", ipMsgId="
                + ipMsgId);
        return updateTextIpMsgId(resolver, smsId, -ipMsgId);
    }

    public static int deleteGroupThread(Context context, long threadId) {
        Uri uri = ContentUris.withAppendedId(MMS_SMS_URI_DELETE_GROUP_THREADS, threadId);
        return context.getContentResolver().delete(uri, "deleteGroupThreads", null);
    }

    public static String formatIdInClause(Collection<Long> ids) {
        /* to IN sql */
        if (ids == null || ids.size() == 0) {
            return " IN ()";
        }
        String in = " IN (";
        Iterator<Long> iter = ids.iterator();
        if (iter.hasNext()) {
            in += iter.next();
        }
        while (iter.hasNext()) {
            in += ", " + iter.next();
        }
        in += ")";
        return in;
    }

    /************************** stack message related start ****************/
    public static void getRCSMessageInfo(ContentResolver resolver, long threadId) {

    }

    public static void deleteStackTextMessage(ContentResolver resolver,
            String msgId) {
        Logger.d(TAG, "deleteMessage with msgID = " + msgId);
        // Delete entries
        resolver.delete(RCS_URI_MESSAGE, ChatLog.Message.MESSAGE_ID + " = '"
                + msgId + "'", null);
    }

    /************************** stack message related send ****************/

    public static long saveMsgToSmsDB(String contact, String content, boolean burn) {
        // save sms db first.
        Set<String> recipient = new HashSet<String>();
        recipient.add(contact);
        long threadId = getThreadIdByChatId(contact);
        //
        Uri uri = addTextIpMessage(sContentResolver, Integer.MAX_VALUE, content, contact,
                Sms.MESSAGE_TYPE_OUTBOX, threadId, RCSUtils.getRCSSubId(), burn);

        return Long.valueOf(uri.getLastPathSegment()).longValue();
    }

    public static long receiveMessage(String chatId, String contact, String msgId, String content,
            boolean burn) {
        long ipmsgId = findTextIdInRcseDb(sContentResolver, msgId, ChatLog.Message.Direction.INCOMING);
        long threadId = getThreadIdByChatId(chatId);
        Uri uri = storeMessageInMmsDb(sContentResolver, ipmsgId, content, contact,
                Sms.MESSAGE_TYPE_INBOX, threadId, RCSUtils.getRCSSubId(), burn);

        return Long.valueOf(uri.getLastPathSegment()).longValue();
    }

    public static long saveMsgToSmsDBForOne2Multi(List<String> contacts, String content) {
        Set<String> recipients = new HashSet<String>();
        StringBuilder contactList = new StringBuilder("");
        for (String recipient : contacts) {
            recipients.add(recipient);
            contactList.append(recipient);
            contactList.append(',');
        }
        long threadId = Threads.getOrCreateThreadId(sHostContext, recipients);
        Uri uri = addTextMultiMessage(sContentResolver, 0, content, contactList.toString(),
                Sms.MESSAGE_TYPE_OUTBOX, threadId, RCSUtils.getRCSSubId());
        return Long.valueOf(uri.getLastPathSegment()).longValue();
    }

    public static void combineMsgId(long msgId, String stackMsgId) {
        long ipMsgId = findTextIdInRcseDb(sContentResolver, stackMsgId, ChatLog.Message.Direction.OUTGOING);
        ContentValues cv = new ContentValues();
        cv.put("ipmsg_id", ipMsgId);
        sContentResolver.update(Sms.CONTENT_URI, cv, Sms._ID + "=" + msgId, null);
    }

    public static void updateMessageSent(String msgId) {
        long ipMsgId = findTextIdInRcseDb(sContentResolver, msgId, ChatLog.Message.Direction.OUTGOING);
//        long smsId = getIdInMmsDb(sContentResolver, ipMsgId);
//        Uri uri = ContentUris.withAppendedId(Sms.CONTENT_URI, smsId);
//        Sms.moveMessageToFolder(sHostContext, uri, Sms.MESSAGE_TYPE_SENT, 0);
        ContentValues values = new ContentValues();
        values.put(Sms.TYPE, Sms.MESSAGE_TYPE_SENT);
        values.put(Sms.DATE_SENT, System.currentTimeMillis());
        String selection = Sms.IPMSG_ID + "=" + ipMsgId;
        sContentResolver.update(Sms.CONTENT_URI, values, selection, null);
    }

    public static void updateMessageDelivered(String msgId) {
        long ipMsgId = findTextIdInRcseDb(sContentResolver, msgId, ChatLog.Message.Direction.OUTGOING);
        long smsId = getIdInMmsDb(sContentResolver, ipMsgId);
        Uri uri = ContentUris.withAppendedId(Sms.CONTENT_URI, smsId);

        ContentValues values = new ContentValues();
        values.put(Sms.STATUS, Sms.STATUS_COMPLETE);

        sContentResolver.update(uri, values, null, null);
    }

    public static long findIpMsgIdInSmsDb(long smsMsgId) {
        Logger.d(TAG, "findIpMsgIdInSmsDb() entry, smsMsgId: " + smsMsgId);

        String[] projection = { Sms.IPMSG_ID, };
        Cursor cursor = sContentResolver.query(SMS_CONTENT_URI, projection, Sms._ID + "="
                + smsMsgId, null, null);
        try {
            if (cursor.moveToFirst()) {
                long ipMsgId = cursor.getLong(cursor.getColumnIndex(Sms.IPMSG_ID));
                Logger.d(TAG, "findIpMsgIdInSmsDb() IPMSG_ID: " + ipMsgId);
                return ipMsgId;
            } else {
                Logger.d(TAG, "getIdInMmsDb() empty cursor");
                return 0;
            }
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
    }

	public static String findIpMsgTextInSmsDb(long smsMsgId) {
		Logger.d(TAG, "findIpMsgTextInSmsDb() entry, smsMsgId: " + smsMsgId);

		String[] projection = { Sms.BODY, };
		Cursor cursor = sContentResolver.query(SMS_CONTENT_URI, projection,
				Sms._ID + "=" + smsMsgId, null, null);
		try {
			if (cursor.moveToFirst()) {
				String body = cursor.getString(cursor.getColumnIndex(Sms.BODY));
				Logger.d(TAG, "findIpMsgTextInSmsDb() Body: " + body);
				return body;
			} else {
				Logger.d(TAG, "findIpMsgTextInSmsDb() empty cursor");
				return null;
			}
		} finally {
			if (null != cursor) {
				cursor.close();
			}
		}
	}
	
	   public static boolean findIpMsgBurnFlagInSmsDb(long smsMsgId) {
	        Logger.d(TAG, "findIpMsgBurnFlagInSmsDb() entry, smsMsgId: " + smsMsgId);

	        String[] projection = { Sms.PROTOCOL, };
	        Cursor cursor = sContentResolver.query(SMS_CONTENT_URI, projection,
	                Sms._ID + "=" + smsMsgId, null, null);
	        try {
	            if (cursor.moveToFirst()) {
	                int isBurnMessage = cursor.getInt(cursor.getColumnIndex(Sms.PROTOCOL));
	                Logger.d(TAG, "findIpMsgBurnFlagInSmsDb() isBurnMessage: " + isBurnMessage);
	                if (isBurnMessage == IpMessageConsts.MessageProtocolType.RCS_BURN_PROTO)
	                    return true;
	                return false;
	            } else {
	                Logger.d(TAG, "findIpMsgBurnFlagInSmsDb() empty cursor");
	                return false;
	            }
	        } finally {
	            if (null != cursor) {
	                cursor.close();
	            }
	        }
	    }

    public static String findMsgIdInRcsDb(long ipMsgId) {
        Logger.d(TAG, "findMsgIdInRcsDb() entry, ipMsgId: " + ipMsgId);

        if (ipMsgId > 0) {
            Cursor cursor = sContentResolver.query(RCS_URI_MESSAGE, PROJECTION_MESSAGE_ID,
                    KEY_EVENT_ROW_ID + "=" + ipMsgId, null, null);
            try {
                if (null != cursor && cursor.moveToFirst()) {
                    String msgId = cursor.getString(cursor.getColumnIndex(KEY_EVENT_MESSAGE_ID));
                    Logger.d(TAG, "findMsgIdInRcsDb()msgId: " + msgId);
                    return msgId;
                } else {
                    Logger.w(TAG, "findIdInRcseDb() invalid cursor: " + cursor);
                }
            } finally {
                if (null != cursor) {
                    cursor.close();
                }
            }
        } else if (ipMsgId < 0) {
            long ftIpMsgId = -ipMsgId;
            Cursor cursor = sContentResolver.query(RCS_URI_FT, PROJECTION_FILE_TRANSFER, 
                    KEY_EVENT_ROW_ID + "=" + ftIpMsgId, null, null);
            try {
                if (null != cursor && cursor.moveToFirst()) {
                    String msgId = cursor.getString(cursor.getColumnIndex(KEY_EVENT_FT_ID));
                    Logger.d(TAG, "findMsgIdInRcsDb()msgId: " + msgId);
                    return msgId;
                } else {
                    Logger.w(TAG, "findIdInRcseDb() invalid cursor: " + cursor);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return null;
    }

    public static void updateSmsBoxType(Uri uri, int folder) {
        Logger.d(TAG, "updateSmsBoxType, uri=" + uri + ", folder=" + folder);
        ContentValues values = new ContentValues();
        values.put(Sms.TYPE, folder);
        sContentResolver.update(uri, values, null, null);
    }

    public static List<String> getAvailableGroupChatIds() {
        List<String> chatIds = new ArrayList<String>();
        String selection = ThreadMapData.KEY_STATUS + "=" + RCSUtils.getRCSSubId();
        Logger.d(TAG, "selection=" + selection);
        Cursor cursor = sContentResolver.query(ThreadMapData.CONTENT_URI,
                ThreadMapUtils.MAP_PROJECTION, selection, null, null);
        try {
            while (cursor.moveToNext()) {
                int chatIdIndex = cursor.getColumnIndex(ThreadMapData.KEY_CHAT_ID);
                Logger.d(TAG, "chatIdIndex = " + chatIdIndex + ", cursor=" + cursor.getPosition());
                chatIds.add(cursor.getString(chatIdIndex));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return chatIds;
    }

    public static void addGroupSysMessage(String body, String chatId) {
        long threadId = getThreadIdByChatId(chatId);
        Logger.d(TAG, "addGroupSysMessage, body=" + body + ", threadId=" + threadId);
        if (threadId <= 0) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("thread_id", threadId);
        values.put("date", System.currentTimeMillis());
        values.put("sub_id", RCSUtils.getRCSSubId());
        values.put("body", body);
        Uri uri = RCSUtils.URI_GROUP_SYS_MESSAGE;
        Uri resultUri = ContextCacher.getHostContext().getContentResolver().insert(uri, values);
        Logger.d(TAG, "addGroupSysMessage end, resultUri=" + resultUri);
    }

    public static List<Participant> getGroupParticipants(String chatId) {
        Logger.i(TAG, "getGroupParticipants(): chatId = " + chatId);
        List<Participant> participants = new ArrayList<Participant>();
        String memberSelection = GroupMemberData.COLUMN_CHAT_ID + "='" + chatId + "'";
        Cursor cursor = sContentResolver.query(RCSUtils.RCS_URI_GROUP_MEMBER,
                RCSUtils.PROJECTION_GROUP_MEMBER, memberSelection, null, null);
        try {
            while (cursor.moveToNext()) {
                Participant participant = new Participant(cursor.getString(cursor
                        .getColumnIndex(GroupMemberData.COLUMN_CONTACT_NUMBER)),
                        cursor.getString(cursor
                                .getColumnIndex(GroupMemberData.COLUMN_CONTACT_NAME)));
                participant.setState(cursor.getInt(cursor.getColumnIndex(GroupMemberData.COLUMN_STATE)));
                participants.add(participant);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Logger.i(TAG, "getGroupParticipants(): recipients size = " + participants.size());
        return participants;
    }

    public static List<Participant> getGroupAvailableParticipants(String chatId) {
        Logger.i(TAG, "getGroupParticipants(): chatId = " + chatId);
        List<Participant> participants = new ArrayList<Participant>();
        String memberSelection = GroupMemberData.COLUMN_CHAT_ID + "='" + chatId + "'" + 
                " AND " + GroupMemberData.COLUMN_STATE + "<>" + GroupMemberData.STATE.STATE_PENDING;
        Cursor cursor = sContentResolver.query(RCSUtils.RCS_URI_GROUP_MEMBER,
                RCSUtils.PROJECTION_GROUP_MEMBER, memberSelection, null, null);
        try {
            while (cursor.moveToNext()) {
                participants.add(new Participant(cursor.getString(cursor
                        .getColumnIndex(GroupMemberData.COLUMN_CONTACT_NUMBER)),
                        cursor.getString(cursor
                                .getColumnIndex(GroupMemberData.COLUMN_CONTACT_NAME))));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Logger.i(TAG, "getGroupParticipants(): recipients size = " + participants.size());
        return participants;
    }

    public static String getGroupSubject(String chatId) {
        Logger.i(TAG, "getGroupSubject(): chatId = " + chatId);
        String selection = ChatLog.GroupChat.CHAT_ID + "='" + chatId + "'";
        String subject = null;
        Cursor cursor = sContentResolver.query(RCSUtils.RCS_URI_GROUP_CHAT,
                RCSUtils.PROJECTION_GROUP_INFO, selection, null, null);
        try {
            if (cursor.moveToFirst()) {
                subject = cursor.getString(cursor.getColumnIndex(ChatLog.GroupChat.SUBJECT));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Logger.i(TAG, "getGroupSubject(): subject = " + subject);
        return subject;

    }

    public static int modifyGroupNickName(String chatId, String nickName) {
        Logger.d(TAG, "modifyGroupNickName, nickName=" + nickName);
        ContentValues values = new ContentValues();
        values.put(ChatLog.GroupChat.NICKNAME, nickName);
        String where = ChatLog.GroupChat.CHAT_ID + "='" + chatId + "'";
        int count = sContentResolver.update(RCS_URI_GROUP_CHAT, values, where, null);
        Logger.d(TAG, "modifyGroupNickName, affectRow=" + count);
        return count;
    }

    /* delete the group chat + messages + ft */
    public static void deleteGroupChat(String chatID) {
        Log.d(TAG, "deleteGroupChat() : for chat_id = " + chatID);
        deleteMessageByChat(chatID);
        deleteFtByChat(chatID);
        // delete the chat info
        sContentResolver.delete(RCS_URI_GROUP_CHAT, ChatLog.GroupChat.CHAT_ID + " = '" + chatID
                + "'", null);
        Log.d(TAG, "chat : " + chatID + " information deleted");
    }

    public static void deleteMessageByChat(String chatID) {
        Log.d(TAG, "deleteMessage() : for chat_id = " + chatID);
        // delete the chatmessage in messages table
        int deletedRows = sContentResolver.delete(RCS_URI_MESSAGE, ChatLog.Message.CHAT_ID + " = '"
                + chatID + "'", null);
        Log.d(TAG, "Messages deleted for chat : " + chatID + " , msg count : " + deletedRows);
    }

    public static void deleteFtByChat(String chatID) {
        Log.d(TAG, "deleteFt() : for chat_id = " + chatID);
        // Delete ft in ft table.
        int deletedRows = sContentResolver.delete(RCS_URI_FT, FileTransferLog.CHAT_ID + " = '"
                + chatID + "'", null);
        Log.d(TAG, "FT deleted for chat : " + chatID + " , msg count : " + deletedRows);
    }

    public static boolean isRCSTextMessage(long ipmsgId) {
        return ipmsgId > 0 ? true : false;
    }

    public static int deleteMessage(long ipmsgId) {
        if (ipmsgId > 0) {
            return deleteTextMessage(ipmsgId);
        } else if (ipmsgId < 0) {
            return deleteFileTransfer(-ipmsgId);
        } else {
            Log.d(TAG, "deleteMessage, ipmsg=0, no need to delete");
            return 0;
        }
    }

    private static int deleteTextMessage(long ipmsgId) {
        Log.d(TAG, "deleteTextMessage with ipmsgId = " + ipmsgId);
        return sContentResolver.delete(RCS_URI_MESSAGE, ChatLog.Message.ID + " = " + ipmsgId, null);
    }

    private static int deleteFileTransfer(long ipmsgId) {
        Log.d(TAG, "deleteFileTransfer with ipmsgId = " + ipmsgId);
        return sContentResolver.delete(RCS_URI_FT, FileTransferLog.ID + " = " + ipmsgId, null);
    }

    public static int deleteMessages(Collection<Long> ipmsgIds) {
        Log.d(TAG, "deleteMessage with ipmsgIds = " + ipmsgIds);
        if (ipmsgIds == null || ipmsgIds.size() == 0) {
            return 0;
        }
        Collection<Long> textMsgIds = new HashSet<Long>();
        Collection<Long> ftMsgIds = new HashSet<Long>();
        for (long ipmsgId : ipmsgIds) {
            if (ipmsgId > 0) {
                textMsgIds.add(ipmsgId);
            } else if (ipmsgId < 0) {
                ftMsgIds.add(-ipmsgId);
            }
        }
        return sContentResolver
                .delete(RCS_URI_MESSAGE, "_id " + formatIdInClause(textMsgIds), null)
                + sContentResolver.delete(RCS_URI_FT, "_id " + formatIdInClause(ftMsgIds), null);
    }
    ////////////////////////////////////////////////////////////////

    public static long saveSentFileTransferToSmsDB(String contact, boolean burn, long ipMsgId, String filePath) {
         // save sms db first. it is for sending.
        Set<String> recipient = new HashSet<String>();
        recipient.add(contact);
        long threadId = Threads.getOrCreateThreadId(sHostContext, recipient);

        Uri uri = RCSUtils.addFTIpMessage(ContextCacher.getHostContext().getContentResolver(), ipMsgId, 
                            filePath, contact, Sms.MESSAGE_TYPE_OUTBOX, threadId, RCSUtils.getRCSSubId(),
                            burn);     
        return Long.valueOf(uri.getLastPathSegment()).longValue();
    }

    public static long saveSentFileTransferToSmsDB(Set<String> contacts, boolean burn, long ipMsgId, String filePath) {
         // save sms db first. it is for sending.
        //Set<String> recipient = new HashSet<String>();
        //recipient.add(contacts);
        long threadId = Threads.getOrCreateThreadId(sHostContext, contacts);

        Iterator it = contacts.iterator();
        StringBuffer strbuf = new StringBuffer();

        while (it.hasNext()) {
            strbuf.append(it.next()).append(",");
        }

        String recipients = strbuf.toString();

        Uri uri = RCSUtils.addFTIpMessage(ContextCacher.getHostContext().getContentResolver(), ipMsgId, 
                            filePath, recipients, Sms.MESSAGE_TYPE_OUTBOX, threadId, RCSUtils.getRCSSubId(),
                            burn);     
        return Long.valueOf(uri.getLastPathSegment()).longValue();
    }

    public static long saveSentFileTransferToSmsDBInGroup(String chatId, boolean burn, long ipMsgId, String filePath) {
         // save sms db first. it is for sending.
        //Set<String> recipient = new HashSet<String>();
        //recipient.add(contact);
        //long threadId = Threads.getOrCreateThreadId(sHostContext, recipient);
        String body = RCSUtils.getSaveBody(burn, filePath);

        long threadId = getThreadIdByChatId(chatId);

        Uri uri = RCSUtils.addFTIpMessage(ContextCacher.getHostContext().getContentResolver(), ipMsgId, 
                            body, "", Sms.MESSAGE_TYPE_OUTBOX, threadId, RCSUtils.getRCSSubId(),
                            burn);     
        return Long.valueOf(uri.getLastPathSegment()).longValue();
    }
    public static long saveReceivedFileTransferToSmsDB(String contact, boolean burn, long ipMsgId, String filePath) {
         // save sms db for receiving a ft in one2one
        Set<String> recipient = new HashSet<String>();
        recipient.add(contact);
        long threadId = Threads.getOrCreateThreadId(sHostContext, recipient);

        Uri uri = RCSUtils.addFTIpMessage(ContextCacher.getHostContext().getContentResolver(), ipMsgId, 
                            filePath, contact, Sms.MESSAGE_TYPE_INBOX, threadId, RCSUtils.getRCSSubId(),
                            burn);
        
        return Long.valueOf(uri.getLastPathSegment()).longValue();
    }

    public static long saveReceivedFileTransferToSmsDBInGroup(String contact, long IpMsgId, String chatId, String filePath) {
        // save sms db for receiving a ft in group
        long threadId = getThreadIdByChatId(chatId);
        boolean burn = false;
        String body = RCSUtils.getSaveBody(burn, filePath);

        Uri uri = RCSUtils.addFTIpMessage(ContextCacher.getHostContext().getContentResolver(), IpMsgId, 
                            body, contact, Sms.MESSAGE_TYPE_INBOX, threadId, RCSUtils.getRCSSubId(),
                            burn);
        
        return Long.valueOf(uri.getLastPathSegment()).longValue();
    }

    public static void setFileTransferOutBox(long smsId) {
        Uri uri = ContentUris.withAppendedId(Sms.CONTENT_URI, smsId);
        Sms.moveMessageToFolder(sHostContext, uri, Sms.MESSAGE_TYPE_OUTBOX, 0);
    }

    public static long updateFTMsgId(long msgId, String stackMsgId) {
        ContentResolver resolver = sHostContext.getContentResolver();

        long ipMsgId = RCSUtils.findFTIdInRcseDb(resolver, stackMsgId);

        Log.d(TAG, "updateFTMsgId, ipMsgId =  " +  ipMsgId + "msgId = " + msgId);
        
        ContentValues cv = new ContentValues();
        cv.put("ipmsg_id", -ipMsgId);
        resolver.update(Sms.CONTENT_URI, cv, Sms._ID + "=" + msgId, null);

        return ipMsgId;
    }

    public static void updateFTMsgFilePath(String filePath, long ipMsgId) {
        long smsId = RCSUtils.getIdInMmsDb(sHostContext.getContentResolver(), ipMsgId);      
        ContentResolver resolver = sHostContext.getContentResolver();
        ContentValues cv = new ContentValues();
        cv.put("body", filePath);
        resolver.update(Sms.CONTENT_URI, cv, Sms._ID + "=" + smsId, null); 
    }

    public static void updateFileTransferSent(String msgId) {
        Log.d(TAG, "updateFileTransferSent, msgId = " + msgId );
        long ipMsgId = RCSUtils.findFTIdInRcseDb(sHostContext.getContentResolver(), msgId);
        long smsId = RCSUtils.getIdInMmsDb(sHostContext.getContentResolver(), -ipMsgId);
        Log.d(TAG, "updateFileTransferSent, ipMsgId = " + -ipMsgId );
        Log.d(TAG, "updateFileTransferSent, smsId = " + smsId );
        Uri uri = ContentUris.withAppendedId(Sms.CONTENT_URI, smsId);
        ContentValues values = new ContentValues();
        values.put(Sms.TYPE, Sms.MESSAGE_TYPE_SENT);
        values.put(Sms.DATE_SENT, System.currentTimeMillis());
        sHostContext.getContentResolver().update(uri, values, null, null);
//        Sms.moveMessageToFolder(sHostContext, uri, Sms.MESSAGE_TYPE_SENT, 0);
    }

    public static void updateFileTransferOutBox(String msgId){
        long ipMsgId = RCSUtils.findFTIdInRcseDb(sHostContext.getContentResolver(), msgId);
        long smsId = RCSUtils.getIdInMmsDb(sHostContext.getContentResolver(), -ipMsgId);
        Uri uri = ContentUris.withAppendedId(Sms.CONTENT_URI, smsId);
        Sms.moveMessageToFolder(sHostContext, uri, Sms.MESSAGE_TYPE_OUTBOX, 0);
    }

    public static void updateFileTransferFail(String msgId) {
        long ipMsgId = RCSUtils.findFTIdInRcseDb(sHostContext.getContentResolver(), msgId);
        long smsId = RCSUtils.getIdInMmsDb(sHostContext.getContentResolver(), -ipMsgId);
        Uri uri = ContentUris.withAppendedId(Sms.CONTENT_URI, smsId);
        Sms.moveMessageToFolder(sHostContext, uri, Sms.MESSAGE_TYPE_FAILED, 0);
    }

    public static void updateFileTransferDelivered(String msgId) {
        long ipMsgId = RCSUtils.findFTIdInRcseDb(sHostContext.getContentResolver(), msgId);
        long smsId = RCSUtils.getIdInMmsDb(sHostContext.getContentResolver(), -ipMsgId);
        Uri uri = ContentUris.withAppendedId(Sms.CONTENT_URI, smsId);
        ContentValues values = new ContentValues();
        values.put(Sms.STATUS, Sms.STATUS_COMPLETE);
        sContentResolver.update(uri, values, null, null);
    }

     public static void setFileTransferFail(long smsId) {
        Uri uri = ContentUris.withAppendedId(Sms.CONTENT_URI, smsId);
        Sms.moveMessageToFolder(sHostContext, uri, Sms.MESSAGE_TYPE_FAILED, 0);
    }

     public static long getSmsIdfromFid(String fid) {
        long smsId;
        smsId = RCSUtils.getIdInMmsDb(sHostContext.getContentResolver(),-Long.valueOf(fid));
        Log.d(TAG, "getSmsIdfromFid, smsId = " + smsId );
        return smsId;
     }

     private static long createGroup(List<Participant> participants, boolean fromInvite) {
         Logger.d(TAG, "createGroupThread, fromInvite= " + fromInvite);
         String contact = null;
         Uri.Builder builder = RCSUtils.MMS_SMS_URI_ADD_THREAD.buildUpon();
         for (Participant participant : participants) {
             contact = participant.getContact();
             Logger.d(TAG, "createGroupThread, participant number = " + contact);
             builder.appendQueryParameter("recipient", contact);
         }
         ContentValues values = new ContentValues();
         if (fromInvite) {
             values.put(Threads.STATUS, IpMessageConsts.GroupActionList.GROUP_STATUS_INVITING);
         } else {
             values.put(Threads.STATUS, RCSUtils.getRCSSubId());
         }
         Uri result = ContextCacher.getHostContext().getContentResolver()
                 .insert(builder.build(), values);
         Logger.d(TAG, "createGroupThread, uri=" + result);
         return Long.parseLong(result.getLastPathSegment());
     }
     
     /**
      * Create group thread id by chatId. Must ensure the thread id is exist.
      *
      * @param chatId group chat id
      * @return real thread id
      */
     public static long createGroupThreadByChatId(String chatId) {
         MapInfo info = ThreadMapCache.getInstance().getInfoByChatId(chatId);
         long threadId = 0;
         if (info != null) {
             threadId = info.getThreadId();
             if (threadId == RCSUtils.DELETED_THREAD_ID) {
                 Logger.d(TAG, "[ensureGroupThreadId],chatId=" + chatId + "threadid = " + threadId);
                 List<Participant> participants = getGroupAvailableParticipants(chatId);
                 threadId = createGroup(participants, false);
                 ThreadMapCache.getInstance().updateThreadId(chatId, threadId);
             }
         } else {
             throw new RuntimeException("[ensureGroupThreadId] don't find mapinfo");
         }
         return threadId;
     }
}
