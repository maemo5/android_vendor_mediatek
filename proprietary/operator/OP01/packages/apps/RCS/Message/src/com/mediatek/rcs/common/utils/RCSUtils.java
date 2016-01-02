package com.mediatek.rcs.common.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.gsma.joyn.chat.ChatLog;
import org.gsma.joyn.ft.FileTransferLog;
import org.gsma.joyn.ft.MultiFileTransferLog;

import com.mediatek.rcs.common.IpMessage;
import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.common.MessageStatusUtils.IFileTransfer.Status;
import com.mediatek.rcs.common.provider.GroupMemberData;
import com.mediatek.rcs.common.provider.ThreadMapCache;
import com.mediatek.rcs.common.provider.ThreadMapCache.MapInfo;
import com.mediatek.rcs.common.service.FileStruct;
import com.mediatek.rcs.common.service.Participant;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Message;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import android.provider.Telephony;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Threads;

import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;

//add by feng
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.telephony.TelephonyManager;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.i18n.phonenumbers.Phonemetadata.PhoneMetadata;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;

import java.lang.reflect.Method;
import android.os.StatFs;
import android.os.SystemProperties;
import java.lang.reflect.InvocationTargetException;
import android.media.MediaFile;
import android.webkit.MimeTypeMap;
import com.mediatek.telephony.TelephonyManagerEx;

import org.gsma.joyn.ft.FileTransferService;
import org.gsma.joyn.ft.FileTransferServiceConfiguration;
import org.gsma.joyn.JoynServiceException;

import com.mediatek.rcs.common.IpImageMessage;
import com.mediatek.rcs.common.IpVCardMessage;
import com.mediatek.rcs.common.IpVideoMessage;
import com.mediatek.rcs.common.IpVoiceMessage;
import com.mediatek.rcs.common.IpGeolocMessage;
import com.mediatek.rcs.common.service.IRCSChatService;
import com.mediatek.rcs.common.binder.RCSServiceManager;
import android.os.RemoteException;
import android.media.MediaMetadataRetriever; 
public class RCSUtils {

    private static final String TAG = "RCSUtils";

    /**
     * URI for mark a conversation as top
     */
    public static final Uri MMS_SMS_URI_TOP                   = Uri.parse("content://mms-sms/rcs/top");
    public static final Uri MMS_SMS_URI_ADD_THREAD            = Uri.parse("content://mms-sms/rcs/thread");
    public static final Uri MMS_SMS_URI_DELETE_GROUP_THREADS  = Uri.parse("content://mms-sms/rcs/group_conversations");
    public static final Uri URI_GROUP_SYS_MESSAGE             = Uri.parse("content://system-ipmsg");
    public static final Uri MMS_SMS_URI_GROUP_ADDRESS         = Uri.parse("content://mms-sms/rcs/thread_addr");
    /**
     * URI for get all messages by threadId, including group chat system msgs
     */

    public static final Uri RCS_URI_MESSAGE         = ChatLog.Message.CONTENT_URI;
    public static final Uri RCS_URI_GROUP_CHAT      = ChatLog.GroupChat.CONTENT_URI;
    public static final Uri RCS_URI_FT              = FileTransferLog.CONTENT_URI;
    public static final Uri RCS_URI_GROUP_MEMBER    = GroupMemberData.CONTENT_URI;

    public static final Uri SMS_CONTENT_URI             = Sms.CONTENT_URI;
    public static final Uri URI_THREADS_UPDATE_STATUS   = Uri.parse("content://mms-sms/conversations/status");
    
    private static final Uri THREAD_SETTINGS_URI = Uri.parse("content://mms-sms/thread_settings/");
    
    private static final String RCS_BLACK_LIST_URI = "content://com.cmcc.ccs.black_list/black_list";
    private static final String[] PROJECTION_BLACK_LIST = {
        "PHONE_NUMBER"
    };

    private static final String TYPE = "type";
    public static final String KEY_EVENT_ROW_ID = "_id";
    public static final String KEY_EVENT_MESSAGE_ID = "msg_id";
    public static final String KEY_EVENT_FT_ID = "ft_id";

    public static final int MESSAGE_TYPE_TEXT               = 1;
    public static final int MESSAGE_TYPE_FT                 = 2;
    
    private static final int ERROR_IPMSG_ID                 = 0;
    
    public static final int DELETED_THREAD_ID = -100;
    public static final long MULTI_FT_BASE_NUMBER = 100000000000l;
    
    public static final int MAX_LENGTH_OF_TEXT_MESSAGE = 900;
    /**
     * preference key for delivery report
     */
    public static final String SMS_DELIVERY_REPORT_MODE = "pref_key_sms_delivery_reports";

    /**
     * send sms action for SmsReceiver
     */
    public static final String ACTION_SEND_MESSAGE = "com.android.mms.transaction.SEND_MESSAGE";

    public static final boolean DEFAULT_DELIVERY_REPORT_MODE  = false;
    
  //add by feng
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

    public static final String[] GET_IP_MSG_ID_PROJECTION    = {KEY_EVENT_ROW_ID, Sms.IPMSG_ID};
    public static final String[] PROJECTION_WITH_THREAD      = {KEY_EVENT_ROW_ID, Sms.THREAD_ID, Sms.ADDRESS, Sms.BODY};
    public static final String[] PROJECTION_ONLY_ID          = {KEY_EVENT_ROW_ID};
    public static final String[] PROJECTION_MESSAGE_ID       = {KEY_EVENT_MESSAGE_ID};
    public static final String[] PROJECTION_FILETRANSFER_ID  = {KEY_EVENT_FT_ID};
    public static final String[] PROJECTION_CREATE_RCS_CHATS = {Threads._ID, Threads.RECIPIENT_IDS, Threads.STATUS};
    public static final String[] PROJECTION_FOR_DUMY_FILETRANSFER  = {KEY_EVENT_ROW_ID, Sms.THREAD_ID, Sms.ADDRESS, Sms.BODY,Sms.TYPE};
    
    public static final String SELECTION_NON_MULTI_MSG = "(" + Sms.PROTOCOL + "=0 OR " +
            Sms.PROTOCOL + "=" + IpMessageConsts.MessageProtocolType.RCS_BURN_PROTO  + ")";
    
    public static final String SELECTION_MULTI_MSG = 
            Sms.PROTOCOL + "=" + IpMessageConsts.MessageProtocolType.RCS_MULTI_PROTO;
    
    public static final String[] PROJECTION_GROUP_INFO = {ChatLog.GroupChat.ID
                                                         ,ChatLog.GroupChat.CHAT_ID
                                                         ,ChatLog.GroupChat.CHAIRMAN
                                                         ,ChatLog.GroupChat.NICKNAME
                                                         ,ChatLog.GroupChat.SUBJECT
                                                         ,ChatLog.GroupChat.PARTICIPANTS_LIST};
    
    public static final String[] PROJECTION_GROUP_MEMBER = {GroupMemberData.COLUMN_CHAT_ID
                                                           ,GroupMemberData.COLUMN_CONTACT_NUMBER
                                                           ,GroupMemberData.COLUMN_CONTACT_NAME
                                                           ,GroupMemberData.COLUMN_STATE
                                                           ,GroupMemberData.COLUMN_PORTRAIT
                                                           ,GroupMemberData.COLUMN_TYPE};

    public static final String[] PROJECTION_TEXT_IP_MESSAGE = {ChatLog.Message.ID
                                                              ,ChatLog.Message.DIRECTION
                                                              ,ChatLog.Message.MESSAGE_ID
                                                              ,ChatLog.Message.MESSAGE_STATUS
                                                              ,ChatLog.Message.BODY
                                                              ,ChatLog.Message.TIMESTAMP
                                                              ,ChatLog.Message.CONTACT_NUMBER
                                                              ,ChatLog.Message.MIME_TYPE
                                                              ,ChatLog.Message.TIMESTAMP_DELIVERED
                                                              ,ChatLog.Message.MESSAGE_TYPE};

    //add by feng
    public static final String[] PROJECTION_FILE_TRANSFER = {FileTransferLog.ID
                                                              ,FileTransferLog.FT_ID
                                                              ,FileTransferLog.CONTACT_NUMBER
                                                              ,FileTransferLog.FILENAME
                                                              ,FileTransferLog.FILESIZE
                                                              ,FileTransferLog.MIME_TYPE
                                                              ,FileTransferLog.DIRECTION
                                                              ,FileTransferLog.TRANSFERRED
                                                              ,FileTransferLog.TIMESTAMP
                                                              ,FileTransferLog.TIMESTAMP_SENT
                                                              ,FileTransferLog.TIMESTAMP_DELIVERED
                                                              ,FileTransferLog.TIMESTAMP_DISPLAYED
                                                              ,FileTransferLog.STATE
                                                              ,FileTransferLog.FILEICON
                                                              ,FileTransferLog.CHAT_ID
                                                              ,FileTransferLog.MSG_ID
                                                              ,FileTransferLog.DURATION
                                                              ,FileTransferLog.SESSION_TYPE
                                                             };

    public static final String[] PROJECTION_WITH_TYPE      = {KEY_EVENT_ROW_ID, Sms.THREAD_ID, Sms.ADDRESS, Sms.BODY, Sms.TYPE};

    public static String IMAGE_FT_TYPE = "ft001";
    public static String VIDEO_FT_TYPE = "ft002";
    public static String AUDIO_FT_TYPE = "ft003";
    public static String VCARD_FT_TYPE = "ft004";
    public static String GEOLOC_FT_TYPE = "ft005";
    public static String BURN_FT_TYPE = "ft006";
    public static String FILE_TYPE = "ft007";

    public static int getRCSSubId() {
        // TODO
        int subId = SubscriptionManager.getDefaultDataSubId();
        return subId;
    }

    public static void updateSmsBoxType(Uri uri, int folder) {
        Logger.d(TAG, "updateSmsBoxType, uri=" + uri + ", folder=" + folder);
//        Sms.moveMessageToFolder(ContextCacher.getHostContext(), uri, folder, 0);
        ContentResolver resolver = ContextCacher.getHostContext().getContentResolver();
        ContentValues values = new ContentValues();
        values.put(Sms.TYPE, folder);
        resolver.update(uri, values, null, null);
    }

    public static int getNotificationEnable(long threadId) {
        ContentResolver resolver = ContextCacher.getHostContext().getContentResolver();
        Uri threadSettingsUri = ContentUris.withAppendedId(THREAD_SETTINGS_URI, threadId);
        Cursor c = resolver.query(threadSettingsUri,
                new String[] {Telephony.ThreadSettings.NOTIFICATION_ENABLE},
                null, null, null);
        int threadNotificationEnabled = 1;
        try {
            if (c.getCount() == 0) {
                Logger.d(TAG, "cursor count is 0");
            } else {
                c.moveToFirst();
                threadNotificationEnabled = c.getInt(0);
                Logger.d(TAG, "before check: threadNotificationEnabled = " + threadNotificationEnabled);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return threadNotificationEnabled;
    }
    
    public static void setNotificationEnable(long threadId, int enable) {
        Logger.d(TAG, "setNotificationEnable, threadId=" + threadId + ", enable=" + enable);
        Uri uri = ContentUris.withAppendedId(THREAD_SETTINGS_URI, threadId);
        ContentValues values = new ContentValues();
        values.put(Telephony.ThreadSettings.NOTIFICATION_ENABLE, enable);
        ContentResolver resolver = ContextCacher.getHostContext().getContentResolver();
        resolver.update(uri, values, null, null);
    }
    /**
     * If is spam message, return true, else return false.
     */
    public static boolean isIpSpamMessage(Context context, String number) {
        Cursor cursor = context.getContentResolver().query(Uri.parse(RCS_BLACK_LIST_URI),
               PROJECTION_BLACK_LIST, null, null, null);
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
    public static boolean markConversationTop(Context context, long threadId, boolean isTop) {
        long time = isTop ? System.currentTimeMillis() : 0;
        Uri markTopUri = ContentUris.withAppendedId(MMS_SMS_URI_TOP, threadId);
        ContentValues cv = new ContentValues(1);
        cv.put("top", time);
        int count = context.getContentResolver().update(markTopUri, cv, null, null);
        return count > 0 ? true : false;
    }

    /****************************RCS DB related start*****************************/

    public static long findFTIdInRcseDb(ContentResolver resolver, String ftId) {
        Logger.d(TAG, "findFTIdInRcseDb() entry, msgId: " + ftId);
        return findIdInRcsDb(resolver, MESSAGE_TYPE_FT, ftId);
    }

    private static long findIdInRcsDb(ContentResolver resolver, int msgType, String id) {
        Logger.d(TAG, "findIdInRcseDb() entry, msgId: " + id + ",  msgType=" + msgType);

        if (TextUtils.isEmpty(id)) {
            Logger.e(TAG, "findIdInRcseDb(), invalid msgId: " + id);
            return ERROR_IPMSG_ID;
        }
        Uri uri = null;
        String where = null;
        if (msgType == MESSAGE_TYPE_TEXT) {
            uri = RCS_URI_MESSAGE;
            where = KEY_EVENT_MESSAGE_ID + "='" + id + "'";
        } else if (msgType == MESSAGE_TYPE_FT) {  //add by feng
            uri = RCS_URI_FT;
            where = KEY_EVENT_FT_ID + "='" + id + "'";
        } else {
            Logger.e(TAG, "findIdInRcseDb(), invalid msgType: " + msgType);
            return ERROR_IPMSG_ID;
        }
        Cursor cursor = resolver.query(uri, PROJECTION_ONLY_ID, where, null, null);
        try {
            if (null != cursor && cursor.moveToFirst()) {
                long rowId =  cursor.getLong(cursor.getColumnIndex(KEY_EVENT_ROW_ID));
                Logger.d(TAG, "findIdInRcseDb() row id for " + id + ", is " + rowId);
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
    /****************************RCS DB related end*****************************/

    /*****************insert RCS messages to message db start**********************************/

    public static Uri addFTIpMessage(ContentResolver resolver, long ipMsgId, String body,
            String contact, int boxType, long threadId, long subId, boolean isBurnMessage) {
        Logger.d(TAG, "addFTIpMessage(), body=" + body + ", contact=" + contact + ", boxType=" + boxType
                + ", threadId=" + threadId + ", isBurnMessage=" + isBurnMessage);
        return storeMessageInMmsDb(resolver, -ipMsgId, body, contact, boxType, threadId, subId, isBurnMessage);
    }

    public static Uri storeMessageInMmsDb(ContentResolver resolver, long ipMsgId, String text,
            String contact, int boxType, long threadId, long subId, boolean isBurnMessage) {
        long idInMmsDb = getIdInMmsDb(resolver, ipMsgId);
        if (0 < idInMmsDb) {
            Logger.d(TAG, "storeMessageInDatabase() message found in Mms DB, id: " + idInMmsDb);
            return ContentUris.withAppendedId(SMS_CONTENT_URI, idInMmsDb);
        } else {
            Logger.d(TAG, "storeMessageInDatabase() message not found in Mms DB");
            //TODO discuss with mengjie, what to save in contact field for group
//            if (TextUtils.isEmpty(contact)) {
//                Logger.w(TAG, "storeMessageInDatabase() invalid remote: " + contact);
//                return null;
//            }
            return insertDatabase(resolver, text, contact, ipMsgId, boxType, threadId, subId, isBurnMessage);
        }
    }
    
    public static long getIdInMmsDb(ContentResolver contentResolver, long ipMsgId) {
        Logger.d(TAG, "getIdInMmsDb() entry, ipMsgId: " + ipMsgId);

        String selection = Sms.IPMSG_ID + "=" + ipMsgId + " AND " + SELECTION_NON_MULTI_MSG;
        Cursor cursor = contentResolver.query(SMS_CONTENT_URI, PROJECTION_WITH_THREAD,
                selection, null, null);
        try {
            if (cursor.moveToFirst()) {
                long mmsDbId = cursor.getLong(cursor.getColumnIndex(Sms._ID));
                long threadId = cursor.getLong(cursor.getColumnIndex(Sms.THREAD_ID));
                String contact = cursor.getString(cursor.getColumnIndex(Sms.ADDRESS));
                Logger.d(TAG, "getIdInMmsDb() contact is " + contact + " threadId is " + threadId);
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
    //add by feng
    public static int getIdAndTypeInMmsDb(ContentResolver contentResolver, long ipMsgId) {
        Log.d(TAG, "getIdInMmsDb() entry, ipMsgId: " + ipMsgId);

        Cursor cursor = contentResolver.query(SMS_CONTENT_URI, PROJECTION_WITH_TYPE,
                Sms.IPMSG_ID + "=" + ipMsgId, null, null);
        try {
            if (cursor.moveToFirst()) {
                long mmsDbId = cursor.getLong(cursor.getColumnIndex(Sms._ID));
                long threadId = cursor.getLong(cursor.getColumnIndex(Sms.THREAD_ID));
                String contact = cursor.getString(cursor.getColumnIndex(Sms.ADDRESS));
                int boxType = cursor.getInt(cursor.getColumnIndex(Sms.TYPE));
                Log.d(TAG, "getIdInMmsDb() contact is " + contact + " threadId is " + threadId);
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
    

    private static Uri insertDatabase(ContentResolver resolver, String body, String contact,
            long ipMsgId, int boxType, long targetThreadId, long subId, boolean isBurnMessage) {
        Logger.d(TAG, "InsertDatabase(), body = " + body + "contact is " + contact + " , boxType: " + boxType
                + " , threadId: " + targetThreadId + " , isBurnMessage: " + isBurnMessage);
        ContentValues cv = new ContentValues();
        cv.put(Sms.ADDRESS, contact);
        cv.put(Sms.BODY, body);
        cv.put(Sms.IPMSG_ID, ipMsgId);
        /***fix me****/
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
            smsUri = smsUri.buildUpon().appendQueryParameter("readBurnMessage", "true").build();
        }
        return resolver.insert(smsUri, cv);
    }
    /*****************insert RCS messages to message db start**********************************/

    public static int deleteGroupThread(Context context, long threadId) {
        Uri uri = ContentUris.withAppendedId(MMS_SMS_URI_DELETE_GROUP_THREADS, threadId);
        return context.getContentResolver().delete(uri, "deleteGroupThreads", null);
    }

    /**********************private function for native use start**********************/
    public static String formatIdInClause(Collection<Long> ids) {
        /* to IN sql */
        if (ids == null || ids.size() == 0) {
            return " IN ()";
        }
        String in = " IN (";
        Iterator<Long> iter = ids.iterator();
        if (iter.hasNext()) {
            in +=  iter.next();
        }
        while(iter.hasNext()) {
            in += ", " + iter.next();
        }
        in += ")";
        return in;
    }

    /**********************private function for native use end**********************/

    /**
    * Get the current available storage size in byte;
    * 
    * @return available storage size in byte; -1 for no external storage
    *         detected
    */
   public static long getFreeStorageSize() {
       boolean isExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
       if (isExist) {
           File path = Environment.getExternalStorageDirectory();
           StatFs stat = new StatFs(path.getPath());
           int availableBlocks = stat.getAvailableBlocks();
           int blockSize = stat.getBlockSize();
           long result = (long) availableBlocks * blockSize;
           Logger.d(TAG, "getFreeStorageSize() blockSize: " + blockSize + " availableBlocks: "
                   + availableBlocks + " result: " + result);
           return result;
       }
       return -1;
   }
   
   
   public static String getFileExtension(String fileName) {
       Logger.d(TAG, "getFileExtension() entry, the fileName is " + fileName);
       String extension = null;
       if (TextUtils.isEmpty(fileName)) {
           Logger.d(TAG, "getFileExtension() entry, the fileName is null");
           return null;
       }
       int lastDot = fileName.lastIndexOf(".");
       extension = fileName.substring(lastDot + 1).toLowerCase();
       return extension;
   }
   
   /**
    * Return the file transfer IpMessage
    * 
    * @param remote remote user
    * @param FileStructForBinder file strut
    * @return The file transfer IpMessage
    */
    public static IpMessage analysisFileType(String remote, FileStruct fileTransfer) {

        String fileName = fileTransfer.mName;
        if (fileName != null) {
            String mimeType = MediaFile.getMimeTypeForFile(fileName);
            if (mimeType == null) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        getFileExtension(fileName));
            }
            if (mimeType != null) {
                if (mimeType.contains(FILE_TYPE_IMAGE)) {
                    return new IpImageMessage(fileTransfer,remote);
                } else if (mimeType.contains(FILE_TYPE_AUDIO)
                        || mimeType.contains("application/ogg")) {
                    return new IpVoiceMessage(fileTransfer,remote);
                } else if (mimeType.contains(FILE_TYPE_VIDEO)) {
                    return new IpVideoMessage(fileTransfer,remote);
                } else if (fileName.toLowerCase().endsWith(".vcf")) {
                    return new IpVCardMessage(fileTransfer,remote);
                } else if (fileName.toLowerCase().endsWith(".xml")) {
                    return new IpGeolocMessage(fileTransfer,remote);
                } else {
                    // Todo
                    Logger.d(TAG, "analysisFileType() other type add here!");
                }
            }
        } else {
            Logger.w(TAG, "analysisFileType(), file name is null!");
        }
        return null;
    }

    /**
     * Return mimeType
     * 
     * @param path
     * @return mimeType
     */
     public static String getFileType(String path) {
         if (path != null) {
             String mimeType = MediaFile.getMimeTypeForFile(path);
             if (mimeType == null) {
                 mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                         getFileExtension(path));
             }
             return mimeType;
         } else {
             Logger.w(TAG, "analysisFileType(), file name is null!");
         }
         return null;
     }

 //add by feng
    /**
     * Extract user part phone number from a SIP-URI or Tel-URI or SIP address
     * 
     * @param uri SIP or Tel URI
     * @return Number or null in case of error
     */
    public static String extractNumberFromUri(String uri) {
        if (uri == null) {
            return null;
        }

        try {
            // Extract URI from address
            int index0 = uri.indexOf("<");
            if (index0 != -1) {
                uri = uri.substring(index0+1, uri.indexOf(">", index0));
            }
           
            // Extract a Tel-URI
            int index1 = uri.indexOf("tel:");
            if (index1 != -1) {
                uri = uri.substring(index1+4);
            }
            // Extract a SIP-URI
            index1 = uri.indexOf("sip:");
            if (index1 != -1) {
                int index2 = uri.indexOf("@", index1);
                uri = uri.substring(index1+4, index2);
            }
            // Remove URI parameters
            int index2 = uri.indexOf(";"); 
            if (index2 != -1) {
                uri = uri.substring(0, index2);
            }
            // Format the extracted number (username part of the URI)
            return formatNumberToInternational(uri);
        } catch(Exception e) {
            return null;
        }
    }
    
    /**
     * Format a phone number to international format
     * 
     * @param number Phone number
     * @return International number
     */
    public static String formatNumberToInternational(String number) {
        String sInternationalPrefix = null;
        if (number == null) {
            return null;
        }
        
        // Remove spaces
        number = number.trim();

        // Strip all non digits
        String phoneNumber = PhoneNumberUtils.stripSeparators(number);
        if(phoneNumber.equals(""))
        {
            return "";
        }
        if (sInternationalPrefix == null) {
            String countryIso = null;
            try {
                countryIso = getDefaultSimCountryIso();
            } catch (ClassCastException e) {
                e.printStackTrace();
                Logger.e(TAG,
                        "formatNumberToInternational() plz check whether your load matches your code base");
            }
            if (countryIso != null) {
                sInternationalPrefix = getInternationalPrefix(countryIso.toUpperCase());
            }
            Logger.d(TAG, "formatNumberToInternational() countryIso: " + countryIso
                    + " sInternationalPrefix: " + sInternationalPrefix);
        }
        if (sInternationalPrefix != null) {
            Pattern pattern = Pattern.compile(sInternationalPrefix);
            Matcher matcher = pattern.matcher(number);
            StringBuilder formattedNumberBuilder = new StringBuilder();
            if (matcher.lookingAt()) {
                int startOfCountryCode = matcher.end();
                formattedNumberBuilder.append(COUNTRY_CODE_PLUS);
                formattedNumberBuilder.append(number.substring(startOfCountryCode));
                phoneNumber = formattedNumberBuilder.toString();
            }
        }
        Logger.d(TAG, "formatNumberToInternational() number: " + number + " phoneNumber: "
                + phoneNumber + " sInternationalPrefix: " + sInternationalPrefix);
        // Format into international
        if (phoneNumber.startsWith("00" + COUNTRY_CODE.substring(1))) {
            // International format
            phoneNumber = COUNTRY_CODE + phoneNumber.substring(4);
        } else
        if ((COUNTRY_AREA_CODE != null) && (COUNTRY_AREA_CODE.length() > 0) &&
                phoneNumber.startsWith(COUNTRY_AREA_CODE)) {
            // National number with area code
            phoneNumber = COUNTRY_CODE + phoneNumber.substring(COUNTRY_AREA_CODE.length());
        } else
        if (!phoneNumber.startsWith("+")) {
            // National number
            phoneNumber = COUNTRY_CODE + phoneNumber;
        }
        return phoneNumber;
    }
    
    private static String getDefaultSimCountryIso() {
        int simId;
        String iso = null;
        TelephonyManagerEx mTelephonyManagerEx = null;
        //read if gemini support is present (change duet to L-migration)
        boolean geminiSupport = false;
        geminiSupport = SystemProperties.get("ro.mtk_gemini_support").equals("1");
        
        //if (FeatureOption.MTK_GEMINI_SUPPORT == true) {
        if(geminiSupport){
            //simId = SystemProperties.getInt(PhoneConstants.GEMINI_DEFAULT_SIM_PROP, -1);
            simId = 0; //changes as per L-migration , defualt SIM is SIM_ID_1 value = 0
            if (simId == -1) {// No default sim setting
                simId = PhoneConstants.SIM_ID_1;
            }

                        if(mTelephonyManagerEx ==null){
                           mTelephonyManagerEx  = TelephonyManagerEx.getDefault();
                        }

            if (!mTelephonyManagerEx.getDefault().hasIccCard(simId)) {
                simId = PhoneConstants.SIM_ID_2 ^ simId;
            }

                          iso = mTelephonyManagerEx.getSimCountryIso(simId);

        } else {
            iso = TelephonyManager.getDefault().getSimCountryIso();
        }
        return iso;
    }

    private static String getInternationalPrefix(String countryIso) {
        try {
            PhoneNumberUtil util = PhoneNumberUtil.getInstance();
            Method method = PhoneNumberUtil.class.getDeclaredMethod(METHOD_GET_METADATA,
                    String.class);
            method.setAccessible(true);
            PhoneMetadata metadata = (PhoneMetadata) method.invoke(util, countryIso);
            if (metadata != null) {
                String prefix = metadata.getInternationalPrefix();
                if (countryIso.equalsIgnoreCase(REGION_TW)) {
                    prefix = INTERNATIONAL_PREFIX_TW;
                }
                return prefix;
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int getFileSize(String filepath) {
        try {
            if (TextUtils.isEmpty(filepath)) {
                return -1;
            }
            File file = new File(filepath);
            return (int) file.length();
        } catch (Exception e) {
            Logger.e(TAG, "getFileSize():" + e);
            return -1;
        }
    }

    public static int getIntStatus(Status stat) {
        if (stat == Status.PENDING) {
            return 0;
        } else if (stat == Status.WAITING) {
            return 1;
        } else if (stat == Status.TRANSFERING) {
            return 2;
        }else if (stat == Status.CANCEL) {
            return 3;
        }else if (stat == Status.CANCELED) {
            return 4;
        }else if (stat == Status.FAILED) {
            return 5;
        }else if (stat == Status.REJECTED) {
            return 6;
        }else if (stat == Status.FINISHED) {
            return 7;
        }else if (stat == Status.TIMEOUT) {
            return 8;
        }
        return -1;
        
    }
    public static Status getRcsStatus(int stat) {
        switch (stat) {
            case 0:
                return Status.PENDING;
            case 1:
                return Status.WAITING;
            case 2:
                return Status.TRANSFERING;
            case 3:
                return Status.CANCEL;
            case 4:
                return Status.CANCELED;
            case 5:
                return Status.FAILED;
            case 6:
                return Status.REJECTED;
            case 7:
                return Status.FINISHED;
            case 8:
                return Status.TIMEOUT;
        }

        return Status.FAILED;
    }
    public static Status getRcsStatusformStack(int state) {
        //map stack FT state and IpMessage status

        Status status = Status.TRANSFERING;
        
        switch (state) {
            //case 2: // INITIATED,File transfer invitation sent
            //status = Status.PENDING;
            //break;
            case 1: //INVITED
            status = Status.WAITING;
            break;
            case 2: // INITIATED,File transfer invitation sent
            case 3: //STARTED,File transfer is started
            status = Status.TRANSFERING;
            break;
            case 4: //TRANSFERRED
            status = Status.FINISHED;
            break;
            case 5: //ABORTED
            case 6: //FAILED
            status = Status.FAILED;
            break;
            case 7://DELIVERED
            case 8: //DISPLAYED
            status = Status.FINISHED;
            break;
            default:
            break;        
        }
        return status;
    }
    
    public static String getSaveBody(boolean isBurn, String filePath) {
        String body = "";
        String mimeType = MediaFile.getMimeTypeForFile(filePath);
        if (isBurn) {
            body = RCSUtils.BURN_FT_TYPE + filePath;
            return body;
        }
        if (mimeType != null) {
        if (mimeType.contains(RCSUtils.FILE_TYPE_IMAGE)) {
            body = RCSUtils.IMAGE_FT_TYPE + filePath;
        } else if (mimeType.contains(RCSUtils.FILE_TYPE_VIDEO)) {
            body = RCSUtils.VIDEO_FT_TYPE + filePath;
        } else if (mimeType.contains(RCSUtils.FILE_TYPE_AUDIO)) {
            body = RCSUtils.AUDIO_FT_TYPE + filePath;
        } else if (filePath.toLowerCase().endsWith(".vcf")) {
            body = RCSUtils.VCARD_FT_TYPE + filePath;
        } else if (filePath.toLowerCase().endsWith(".xml")) {
            body = RCSUtils.GEOLOC_FT_TYPE + filePath;
        }
        } else {
            body =  RCSUtils.FILE_TYPE + filePath;
        }
        return body;
    }

    public static long getFileTransferMaxSize() {
        long maxSize = 0;
        IRCSChatService service = RCSServiceManager.getInstance().getChatService();
        try {
            maxSize = service.getRcsFileTransferMaxSize();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return maxSize;
    }
    public static int getDuration(Context context, String filePath) {
        Uri uri = Uri.fromFile(new File(filePath));
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        int duration = 0;

        String mimeType = MediaFile.getMimeTypeForFile(filePath);
        Log.d(TAG, "getDuration, enter");

        if (mimeType != null) {
            if (mimeType.contains(RCSUtils.FILE_TYPE_VIDEO) ||
                mimeType.contains(RCSUtils.FILE_TYPE_AUDIO))  {
                 try {
                    retriever.setDataSource(context,uri);
                    String dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    Log.d(TAG, "getDuration, dur = " + dur);
                    if (dur != null) {
                        duration = Integer.parseInt(dur);
                        Log.d(TAG, "getDuration, duration = " + duration);
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "MediaMetadataRetriever failed to get duration for " + ex);

                } finally {
                    retriever.release();
                }
            }
        }
       
        Log.d(TAG, "getDuration, duration = " + duration);
        return duration/1000;
    }
}