/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.rcs.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.gsma.joyn.chat.ChatLog;
import org.gsma.joyn.chat.ChatLog.Message.Status.Content;
import org.gsma.joyn.ft.FileTransferLog;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.RemoteException;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.util.Log;

import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.provider.ThreadMapCache;
import com.mediatek.rcs.common.provider.ThreadMapCache.MapInfo;
import com.mediatek.rcs.common.service.FileStruct;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.common.utils.Logger;
import com.mediatek.rcs.common.utils.RCSUtils;
import com.mediatek.rcs.common.service.IRCSChatService;
import android.media.MediaFile;
import android.text.TextUtils;
import java.io.File;
/**
* Provide message management related interface
*/
public class RCSMessageManager{
    private static final String TAG = "RCSMessageManager";

    public static final int SAVE_SUCCESS = 1;
    public static final int SAVE_DRAFT = 0;
    public static final int SAVE_FAIL = -1;
    public static final int ERROR_CODE_UNSUPPORT_TYPE = 100;
    public static final int ERROR_CODE_INVALID_PATH = 101;
    public static final int ERROR_CODE_EXCEED_MAXSIZE = 102;
    public static final int ERROR_CODE_UNKNOWN = 103;
    private static RCSMessageManager sInstance;
    private Context mContext;

    /**
     * The Constant RANDOM.
     */
    private static final Random RANDOM = new Random();
    /**
     * The Constant MESSAGE_TAG_RANGE.
     */
    public static final int MESSAGE_TAG_RANGE = 1000;
    /**
     * The Constant COMMA.
     */
    public static final String COMMA = ",";

//    private IRCSChatService mService = null;

    public static final char[] FILE_TRANSFER_TYPE = {0x01, 0x02, 0x03, 0x04};
    public static final String FILE_TRANSFER_TYPE_IMAGE = String.valueOf(FILE_TRANSFER_TYPE[0]);
    public static final String FILE_TRANSFER_TYPE_VIDEO = String.valueOf(FILE_TRANSFER_TYPE[1]);
    public static final String FILE_TRANSFER_TYPE_AUDIO = String.valueOf(FILE_TRANSFER_TYPE[2]);
    public static final String FILE_TRANSFER_TYPE_VCARD = String.valueOf(FILE_TRANSFER_TYPE[3]);

    public static Map<Long, IpMessage> sCachedSendMessage = new ConcurrentHashMap<Long, IpMessage>();

    private RCSMessageManager(Context context) {
        mContext = ContextCacher.getHostContext();
//        mService = RCSServiceManager.getInstance().getChatService();
    }

    public static RCSMessageManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new RCSMessageManager(context);
        }
        return sInstance;
    }

    /**
     * 
     * @param ipmsgId
     * @return
     */
    public IpMessage getIpMsgInfo(long ipmsgId) {
        Logger.d(TAG, "getIpMsgInfo() msgId = " + ipmsgId);
        IpMessage ipMessage = null;
        if (ipmsgId > 0) {
            ipMessage = getIpTextMsgInfo(ipmsgId);
        } else if (ipmsgId < 0) {
            ipMessage = getIpFTMsgInfo(ipmsgId);
        } else {
            Logger.d(TAG, "this is sms, no need to return ipmsg Info");
        }
        return ipMessage;
    }

    public IpMessage getIpMsgInfo(long threadId, long ipmsgId) {
        Logger.d(TAG, "getIpMsgInfo() ipmsgId = " + ipmsgId + ", threadId = " + threadId);
        IpMessage ipMessage = null;

        if (ipmsgId > 0) {
            ipMessage = getIpTextMsgInfo(ipmsgId);
        } else
        if (ipmsgId < 0) {
            ipMessage = getIpFTMsgInfo(ipmsgId);
        }
        return ipMessage;
    }

    private IpMessage getIpTextMsgInfo(long ipmsgId) {
        Logger.d(TAG, "getIpTextMsgInfo() ipmsgId = " + ipmsgId);
        IpTextMessage ipMessage = null;
        ContentResolver resolver = ContextCacher.getHostContext().getContentResolver();
        if (ipmsgId > 0) {
            Cursor cursor = resolver.query(RCSUtils.RCS_URI_MESSAGE, RCSUtils.PROJECTION_TEXT_IP_MESSAGE, "_id=" + ipmsgId, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    ipMessage = new IpTextMessage(ipmsgId,
                            cursor.getString(cursor.getColumnIndex(ChatLog.Message.MESSAGE_ID)),
                            cursor.getInt(cursor.getColumnIndex(ChatLog.Message.DIRECTION)));
                    int status = transferStatusToMms(cursor.getInt(cursor.getColumnIndex(ChatLog.Message.MESSAGE_STATUS))
                            ,cursor.getLong(cursor.getColumnIndex(ChatLog.Message.TIMESTAMP_DELIVERED)));
                    ipMessage.setStatus(status);
                    
                    ipMessage.setDate(cursor.getLong(cursor.getColumnIndex(ChatLog.Message.TIMESTAMP)));
                    // TODO: should modify for geoloc message
                    byte[] blob = cursor.getBlob(cursor.getColumnIndex(ChatLog.Message.BODY));
                    if (blob != null) {
                        ipMessage.setBody(new String(blob));
                    }
                    boolean isBurned = cursor.getInt(cursor.getColumnIndex(ChatLog.Message.MESSAGE_TYPE)) == ChatLog.Message.Type.BURN ? true : false;
                    ipMessage.setBurnedMessage(isBurned);
                    String contactNumber = cursor.getString(cursor.getColumnIndex(ChatLog.Message.CONTACT_NUMBER));
                    if (cursor.getInt(cursor.getColumnIndex(ChatLog.Message.DIRECTION)) == ChatLog.Message.Direction.INCOMING) {
                        ipMessage.setFrom(contactNumber);
                    } else if (cursor.getInt(cursor.getColumnIndex(ChatLog.Message.DIRECTION)) == ChatLog.Message.Direction.OUTGOING) {
                        ipMessage.setTo(contactNumber);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return ipMessage;
    }

    private IpMessage getIpFTMsgInfo(long ipmsgId) {
        
        Log.d(TAG, "getIpFTMsgInfo() ipmsgId = " + ipmsgId);
        IpMessage ipMessage = null;
        ContentResolver resolver = ContextCacher.getHostContext().getContentResolver();         
        Cursor cursor;
        
        if (isDummyId(-ipmsgId)) {
                // it is only in sms db
                // get info(filePath) from sms db and generate ipMsg
                // not save in cache!!
                Log.d(TAG, "getIpFTMsgInfo()it is only in sms db ");
                String selection = Sms.IPMSG_ID + "=" + ipmsgId;
                cursor = resolver.query(RCSUtils.SMS_CONTENT_URI, RCSUtils.PROJECTION_FOR_DUMY_FILETRANSFER,
                    selection, null, null);
                String body = null;
                String remote = null;
                int Type = Sms.MESSAGE_TYPE_OUTBOX;

            try {
                if (cursor != null && cursor.moveToFirst()) {
                        body = cursor.getString(cursor.getColumnIndex(Sms.BODY));
                        remote = cursor.getString(cursor.getColumnIndex(Sms.ADDRESS));
                        Type = cursor.getInt(cursor.getColumnIndex(Sms.TYPE));
                    }
                 } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                 
                if (body != null) {
                    String filePath = getFliePathfromBody(body);
                    String fileName = getFileName(filePath);
                    String fileTransferTag = (Long.valueOf(-ipmsgId)).toString();
                    String thumbNail = null;
                    boolean sessionType = isBrunfromBody(body);
                    int duration = RCSUtils.getDuration(ContextCacher.getHostContext(), filePath);
                    long size = RCSUtils.getFileSize(filePath);
                    Date date =  new Date();
                    
                    FileStruct filestruct = new FileStruct(filePath,
                                                           fileName,
                                                           size,
                                                           fileTransferTag, 
                                                           date, 
                                                           remote, 
                                                           thumbNail,
                                                           sessionType,
                                                           duration);
                    ipMessage = RCSUtils.analysisFileType(remote,filestruct);
                    ipMessage.setIpDbId(ipmsgId);
                    ipMessage.setMessageId(fileTransferTag);
                    //((IpAttachMessage)ipMessage).setRcsStatus(Status.FAILED);
                    ipMessage.setStatus(Type);  
                 }
                return ipMessage;
                    }
        if (RCSCacheManager.getIpMessage(ipmsgId) != null) {
            Log.d(TAG, "getIpFTMsgInfo, get ipMessage from cache! ");
            String filepath = ((IpAttachMessage)(RCSCacheManager.getIpMessage(ipmsgId))).getPath();
            Log.d(TAG, "Test filepath 1  = " + filepath);
            return RCSCacheManager.getIpMessage(ipmsgId) ;
        }
        
        
            //one2one or group
            long ipid = -ipmsgId;
            cursor = resolver.query(RCSUtils.RCS_URI_FT, RCSUtils.PROJECTION_FILE_TRANSFER, "_id=" + ipid, null, null);

        try {
            if (cursor != null && cursor.moveToFirst()) {
                String filePath = cursor.getString(cursor.getColumnIndex(FileTransferLog.FILENAME));
                int index = filePath.lastIndexOf("/");
                String fileName = filePath.substring(index + 1);
                long size = cursor.getLong(cursor.getColumnIndex(FileTransferLog.FILESIZE));
                String fileTransferTag = cursor.getString(cursor.getColumnIndex(FileTransferLog.FT_ID));
                String remote = cursor.getString(cursor.getColumnIndex(FileTransferLog.CONTACT_NUMBER));
                String thumbNail = cursor.getString(cursor.getColumnIndex(FileTransferLog.FILEICON));
                        Date date =  new Date();
                        boolean sessionType = false;
                        sessionType = (cursor.getInt(cursor.getColumnIndex(FileTransferLog.SESSION_TYPE)) == 1) ? true:false; 
                int duration = cursor.getInt(cursor.getColumnIndex(FileTransferLog.DURATION));
                int state = cursor.getInt(cursor.getColumnIndex(FileTransferLog.STATE));
                        Log.d(TAG, "Test filepath 2  = " + filePath);
                FileStruct filestruct = new FileStruct(filePath,
                                                       fileName,
                                                       size,
                                                       fileTransferTag, 
                                                       date, 
                                                       remote, 
                                                       thumbNail,
                                                       sessionType,
                                                       duration);
                Log.d(TAG, "yangfeng test stack db state  = " + state);
                Log.d(TAG, "yangfeng test new FileStruct: filePath = " + filePath + "fileName = " + fileName +" thumbNail = " + thumbNail);
                ipMessage = RCSUtils.analysisFileType(remote,filestruct);
                if (ipMessage != null) {
                    ipMessage.setIpDbId(ipmsgId);
                    ipMessage.setMessageId(fileTransferTag);
                    ((IpAttachMessage)ipMessage).setRcsStatus(RCSUtils.getRcsStatusformStack(state));
                    ipMessage.setStatus(RCSUtils.getIdAndTypeInMmsDb(resolver,ipmsgId));
                }
            }  
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }   

        RCSCacheManager.setIpMessage(ipMessage,ipmsgId);
       
        Log.d(TAG, "yangfeng test getIpFTMsgInfo() ipMessage = " + ipMessage);
        return ipMessage;
    }

    private boolean isBrunfromBody(String body) {
        if (body.contains(RCSUtils.BURN_FT_TYPE)) {
            return true;
        }
       return false;
    }

    private String getFliePathfromBody(String body) {
        String filePath;
        filePath = body.substring(6);
        return filePath;
    }

    private String getFileName(String filePath) {
        int index = filePath.lastIndexOf("/");
        String fileName = filePath.substring(index + 1); 
        return fileName;
    }

    private boolean isDummyId(long ipmsgId) {
        if (ipmsgId > Integer.MAX_VALUE - 1001 && ipmsgId < Integer.MAX_VALUE ) {
            return true;
        } else {
            return false;
        }
    }

    public void sendEmoticonShopMessage(String contact, String content) {
        // TODO
    }

    public void sendGroupEmoticonShopMessage(String chatId, String content) {
        // TODO
    }

    /**
     * Send RCSMessage before save it, used for forward activity
     * @return
     */
    public int sendRCSMessage(long threadId, IpMessage msg) {
        return saveRCSMsg(msg, IpMessageConsts.IpMessageSendMode.AUTO, threadId);
    }

    public int saveRCSMsg(IpMessage msg, int sendMsgMode, long threadId) {
        Logger.w(TAG, "saveIpMsg() entry");
        int result = SAVE_SUCCESS;
        if(msg.getStatus() == IpMessageConsts.IpMessageStatus.DRAFT) {
            Logger.w(TAG, "saveRCSMsg() Draft message status " + msg.getStatus());
            return SAVE_DRAFT;
        }
        if (msg instanceof IpTextMessage) {
            IpTextMessage message = (IpTextMessage) msg;
            sendTextRCSMessage(message, threadId);
        } else {
            IpAttachMessage message = (IpAttachMessage) msg; 
            result = sendFileTransfer(message, threadId);
        }
        return result;
    }

    private int sendFileTransfer(IpAttachMessage msg, long threadId) {
        Log.d(TAG, "sendFileTransfer enter, msg = " + msg +
            " threadId = " + threadId);
        if (msg == null) {
            return ERROR_CODE_UNKNOWN ;
        }

        /* check if the ipmessage is sendable */
        if (msg.getSize() > RCSUtils.getFileTransferMaxSize()*1024) { 
            Log.d(TAG, "msg.getSize() = " + msg.getSize() +
            " maxSize = " + RCSUtils.getFileTransferMaxSize()*1024);
            return ERROR_CODE_EXCEED_MAXSIZE;
        }
         
        String filePath = msg.getPath();
        int index = filePath.lastIndexOf("/");
        String fileName = filePath.substring(index + 1);
        String mimeType = MediaFile.getMimeTypeForFile(fileName);
       
        if(mimeType == null && !(fileName.toLowerCase().endsWith(".vcf"))
            && !(fileName.toLowerCase().endsWith(".xml")))
        {
            Log.d(TAG, "saveFileTransferMsg() mimeType null");
            return ERROR_CODE_UNSUPPORT_TYPE;      
        }

        if (TextUtils.isEmpty(filePath)) {
            Log.e(TAG, "saveFileTransferMsg() invalid filePath: " + filePath);
            return ERROR_CODE_INVALID_PATH;
        }
        
        File file = new File(filePath);
        if (!file.exists()) {
            Log.e(TAG, "saveFileTransferMsg() file does not exist: " + filePath);
            return ERROR_CODE_INVALID_PATH;
        }
        IRCSChatService service = RCSServiceManager.getInstance().getChatService();
        MapInfo info = ThreadMapCache.getInstance().getInfoByThreadId(threadId);
        try {
            String contact = msg.getTo();
            if (info != null) {
                Log.d(TAG, "sendGroupFilrTransfer, content=" + msg.getPath());
                service.sendGroupFileTransfer(info.getChatId(), msg.getPath());
            } else if (contact != null && contact.contains(COMMA)){
                Log.d(TAG, "sendOne2MultiFT, filePath= " + msg.getPath() + ", contact=" + contact);
                List<String> recipients = collectMultiContact(contact);
                service.sendOne2MultiFileTransfer(recipients,msg.getPath());
            } else if (contact != null && !contact.contains(COMMA)) {
                if (msg.getBurnedMessage()) {
                    Log.d(TAG, "sendBurnFT, filePath= " + msg.getPath());
                    service.sendOne2OneBurnFileTransfer(contact, msg.getPath());
                } else {
                    Logger.d(TAG, "sendOne2OneFT, filePath= " + msg.getPath());
                    service.sendOne2OneFileTransfer(contact, msg.getPath());
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return SAVE_SUCCESS;
    }
    
    private void sendTextRCSMessage(IpTextMessage msg, long threadId) {
        Logger.d(TAG, "sendTextRCSMessage enter, threadId=" + threadId);
        if (msg == null) {
            return;
        }
        IRCSChatService service = RCSServiceManager.getInstance().getChatService();
        MapInfo info = ThreadMapCache.getInstance().getInfoByThreadId(threadId);
        try {
            String contact = msg.getTo();
            if (info != null) {
                Logger.d(TAG, "sendGroupMessage, content=" + msg.getBody());
                service.sendGroupMessage(info.getChatId(), msg.getBody());
            } else if (contact != null && contact.contains(COMMA)){
                Logger.d(TAG, "sendOne2MultiMessage, content=" + msg.getBody() + ", contact=" + contact);
                List<String> recipients = collectMultiContact(contact);
                service.sendOne2MultiMessage(recipients, msg.getBody());
            } else if (contact != null && !contact.contains(COMMA)) {
//                contact = RCSUtils.formatNumberToInternational(contact);
                if (msg.getBurnedMessage()) {
                    Logger.d(TAG, "sendBurnMessage, content=" + msg.getBody());
                    service.sendBurnMessage(contact, msg.getBody());
                } else {
                    Logger.d(TAG, "sendOne2OneMessage, content=" + msg.getBody());
                    service.sendOne2OneMessage(contact, msg.getBody());
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void resendMessage(long smsId, long threadId) {
        Logger.d(TAG, "resendMessage, smsId=" + smsId + ", threadId=" + threadId);
        MapInfo info = ThreadMapCache.getInstance().getInfoByThreadId(threadId);
        IRCSChatService service = RCSServiceManager.getInstance().getChatService();
        try {
            if (info != null) {
                service.resendGroupMessage(info.getChatId(), smsId);
            } else {
//                service.resendMessage(null, smsId);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public int reSendFileTransfer(long ipmsgId, long threadId) {
        //get chatTag
        Log.d(TAG, "sendFileTransfer enter, ipmsgId = " + ipmsgId +
            " threadId = " + threadId);

        IpAttachMessage ipAttachMessage = ((IpAttachMessage) getIpMsgInfo(threadId,ipmsgId));
        
        IRCSChatService service = RCSServiceManager.getInstance().getChatService();
        MapInfo info = ThreadMapCache.getInstance().getInfoByThreadId(threadId);
        
        ///get contacts array
        String[] contacts = null;
        String contact = null;
        if (ipAttachMessage.getTo().contains(COMMA)) {
            contacts = ipAttachMessage.getTo().split(COMMA);
        } else {
            contact = ipAttachMessage.getTo();
        }
        
        try {
        if (info != null) {
                Log.d(TAG, "resendFileTransfer, in groupchat");
                service.resendGroupFileTransfer(info.getChatId(),ipmsgId);
            } else if (contacts != null){
                Log.d(TAG, "resendFileTransfer, in one2multi");
                //List<String> recipients = collectMultiContact(contact);
                 service.resendFileTransfer(ipmsgId);
            } else if (contact != null) {
                Log.d(TAG, "resendFileTransfer, in one2one");
                /*
                service.resendFileTransfer(contact, 
                    ipAttachMessage.getPath(), 
                    ipAttachMessage.getTag(), 
                    ipAttachMessage.getBurnedMessage());  */

                service.resendFileTransfer(ipmsgId);
        }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return SAVE_SUCCESS;
    }

    public void deleteIpMsg(long ipmsgId) {
        long threadId = getThreadId(ipmsgId);
        if (threadId > 0) {
            deleteRCSMsg(threadId, ipmsgId);
        }
    }
    public void deleteFTIpMsg(long ipmsgId) {
        long threadId = getThreadId(ipmsgId);
        if (threadId > 0) {
            deleteRCSMsg(threadId, ipmsgId);
            RCSCacheManager.removeIpMessage(ipmsgId);
        }
    }
    public long getThreadId(long ipmsgId) {
        ContentResolver resolver = mContext.getContentResolver();
        String selection = Sms.IPMSG_ID + "=" + ipmsgId + " AND " + RCSUtils.SELECTION_NON_MULTI_MSG;
        String[] projection = {Sms.THREAD_ID};
        Cursor cursor = resolver.query(Sms.CONTENT_URI, projection, selection, null, null);
        long threadId = 0;
        try {
            if (cursor != null && cursor.moveToFirst()) {
                threadId = cursor.getLong(cursor.getColumnIndex(Sms.THREAD_ID));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return threadId;
    }
    
    public long getIpMsgSentTime(long ipmsgId) {
        ContentResolver resolver = mContext.getContentResolver();
        String selection = Sms.IPMSG_ID + "=" + ipmsgId + " AND " + RCSUtils.SELECTION_NON_MULTI_MSG;
        String[] projection = {Sms.DATE_SENT};
        Cursor cursor = resolver.query(Sms.CONTENT_URI, projection, selection, null, null);
        long time = 0;
        try {
            if (cursor != null && cursor.moveToFirst()) {
                time = cursor.getLong(cursor.getColumnIndex(Sms.DATE_SENT));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return time;
    }
    
    public long getIpMsgId(long threadId) {
        ContentResolver resolver = mContext.getContentResolver();
        String selection = Sms.THREAD_ID + "=" + threadId;
        String[] projection = {Sms.IPMSG_ID};
        Cursor cursor = resolver.query(Sms.CONTENT_URI, projection, selection, null, "_id DESC LIMIT 1");
        long ipmsgId = 0;
        try {
            if (cursor != null && cursor.moveToFirst()) {
                ipmsgId = cursor.getLong(cursor.getColumnIndex(Sms.IPMSG_ID));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return ipmsgId;
    }

    public void deleteRCSMsg(long threadId, long ipmsgId) {
        Log.d(TAG, "[BurnedMsg]: deleteRCSMsg() threadId = "+threadId+ " ipmsgId = "+ipmsgId);
        mContext.getContentResolver().delete(Sms.CONTENT_URI, Sms.IPMSG_ID + " = " + ipmsgId, null);
        Collection<Long> ipmsgIds = new ArrayList<Long>();
        ipmsgIds.add(ipmsgId);
        deleteStackMessages(threadId, ipmsgIds);
    }
    
    public void deleteMultiRCSMsg(long threadId, Collection<Long> ipmsgIds) {
        deleteMsgsInMmsDb(threadId, ipmsgIds);
        deleteStackMessages(threadId, ipmsgIds);
    }

    /**
     * this function is only used for MultiDeleteActivity,
     * And no need to case locked info
     * And no need to remove Group Chat Window
     * And no need to remove Group system message
     * @param threadId
     * @param maxSmsId
     */
    public void deleteThreadFromMulti(long threadId, long maxSmsId) {
        Logger.d(TAG, "deleteThreadFromMulti, threadId = " + threadId + ", maxSmsId = " + maxSmsId);
//        MapInfo info = ThreadMapCache.getInstance().getInfoByThreadId(threadId);
        String selection = Sms.THREAD_ID + "=" + threadId +
                " AND " + 
                Sms._ID + "<=" + maxSmsId +
                " AND " +
                Sms.IPMSG_ID + "<>0";
        String[] projection = {Sms.IPMSG_ID};
        Cursor cursor = mContext.getContentResolver().query(Sms.CONTENT_URI, projection, selection, null, null);

        Collection<Long> ipmsgIds = new HashSet<Long>();
        try {
            while (cursor.moveToNext()) {
                ipmsgIds.add(cursor.getLong(cursor.getColumnIndex(Sms.IPMSG_ID)));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        mContext.getContentResolver().delete(Sms.CONTENT_URI, selection, null);
        deleteStackMessages(threadId, ipmsgIds);
    }

    public void deleteRCSThreads(Collection<Long> threadIds, long maxSmsId, boolean deleteLock) {
        Logger.d(TAG, "deleteRCSThreads, threadIds=" + threadIds);
        String where = null;
        if (threadIds == null) {
            where = null;
        } else {
            where = "_id " + RCSUtils.formatIdInClause(threadIds);
        }
        Logger.d(TAG, "deleteRCSThreads, where = " + where);
        Uri.Builder builder = Threads.CONTENT_URI.buildUpon();
        builder.appendQueryParameter("simple", "true");
        Cursor cursor = mContext.getContentResolver().query(
                builder.build(), RCSUtils.PROJECTION_CREATE_RCS_CHATS, where, null, null);
        Map<Long, Integer> map = new HashMap<Long, Integer>();
        try {
            while (cursor.moveToNext()) {
                map.put(cursor.getLong(cursor.getColumnIndex(Threads._ID)), cursor.getInt(cursor.getColumnIndex(Threads.STATUS)));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Logger.d(TAG, "deleteRCSThreads, threadIds = null, after get all, threads = " + threadIds);
        Iterator<Entry<Long, Integer>> iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<Long, Integer> entry = iter.next();
            deleteRCSThread(entry.getKey(), maxSmsId, deleteLock, entry.getValue());
        }
    }

    private void deleteRCSThread(long threadId, long maxSmsId, boolean deleteLock, int status) {
        Logger.d(TAG, "deleteRCSThread, threadId = " + threadId + ", maxSmsId = " + maxSmsId + ", deleteLock = " + deleteLock);
        boolean isRemoveWindow = true;
        Collection<Long> ipmsgIds = new HashSet<Long>();
//        if (deleteLock) {
//            deleteRCSThreadWithoutLock(threadId, maxSmsId, status);
//        } else {
            String selection = Sms.THREAD_ID + "=" + threadId +
                    " AND " + 
                    Sms._ID + "<=" + maxSmsId +
                    " AND " +
                    Sms.IPMSG_ID + "<>0";
            String[] projection = {Sms.ADDRESS, Sms.PROTOCOL, Sms.IPMSG_ID, Sms.LOCKED};
            String address = null;
            Cursor cursor = mContext.getContentResolver().query(Sms.CONTENT_URI, projection, selection, null, null);
            try {
                while (cursor.moveToNext()) {
                    int locked = cursor.getInt(cursor.getColumnIndex(Sms.LOCKED));
                    if (!deleteLock && locked > 0) {
                        isRemoveWindow = false;
                    } else {
                        ipmsgIds.add(cursor.getLong(cursor.getColumnIndex(Sms.IPMSG_ID)));
                    }
                    if (address == null && cursor.getString(cursor.getColumnIndex(Sms.ADDRESS)) != null) {
                        address = cursor.getString(cursor.getColumnIndex(Sms.ADDRESS));
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
//        }
        deleteMsgsInMmsDb(threadId, ipmsgIds);
//        if (isRemoveWindow) {
            removeChatAndWindow(threadId, status, isRemoveWindow, ipmsgIds, address);
//        } else {
//            deleteStackMessages(threadId, ipmsgIds);
//        }
    }
/**
    private void deleteRCSThreadWithoutLock(long threadId, long maxSmsId, int status) {
        Logger.d(TAG, "deleteRCSThread, threadId = " + threadId + ", maxSmsId = " + maxSmsId);
        String selection = Sms.THREAD_ID + "=" + threadId +
                           " AND " + 
                           Sms._ID + "<=" + maxSmsId +
                           " AND " +
                           Sms.IPMSG_ID + "<>0";
        mContext.getContentResolver().delete(Sms.CONTENT_URI, selection, null);
        removeChatAndWindow(threadId, status);
    }*/

    private void removeChatAndWindow(long threadId, int status, boolean removeAll, Collection<Long> ipmsgIds, String address) {
        Logger.d(TAG, "removeChatAndWindow, threadId=" + threadId);
        MapInfo info = ThreadMapCache.getInstance().getInfoByThreadId(threadId);
        String chatId = null;
        int i = 0;
        long[] ipMsgIds = new long[ipmsgIds.size()];
        for (long ipmsgId : ipmsgIds) {
            ipMsgIds[i++] = ipmsgId;
        }
        IRCSChatService service = RCSServiceManager.getInstance().getChatService();
        if (info != null) {
            chatId = info.getChatId();
            Logger.d(TAG, "delete group thread, threadId=" + threadId + ", chatId=" + chatId);
            int count = RCSUtils.deleteGroupThread(mContext, threadId);
            if (count > 0 && status > 0) {
                ThreadMapCache.getInstance().updateThreadId(chatId, RCSUtils.DELETED_THREAD_ID);
            } else if (count > 0 && status < 0) {
                ThreadMapCache.getInstance().removeByChatId(chatId);
            }
            try {
                if (removeAll) {
                    service.deleteGroupMessages(chatId);
                } else {
                    service.deleteMessages(ipMsgIds);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            Logger.d(TAG, "deleteMessages, for one2one or one2multi");
            try {
                if (removeAll && address != null && !address.contains(COMMA)) {
                    service.deleteO2OMessages(address);
                } else {
                    service.deleteMessages(ipMsgIds);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteMsgsInMmsDb(long threadId, Collection<Long> ipmsgIds) {
        Logger.d(TAG, "deleteMsgsInMmsDb, ipmsgIds = " + ipmsgIds);
        String where = Sms.IPMSG_ID + RCSUtils.formatIdInClause(ipmsgIds);
        // delete rcs message in mms provider
        mContext.getContentResolver().delete(Sms.CONTENT_URI, where, null);
    }

    private void deleteStackMessages(long threadId, Collection<Long> ipmsgIds) {
        Logger.d(TAG, "deleteStackMessages, threadId=" + threadId);
        long[] ipmessageIds = new long[ipmsgIds.size()];
        int i = 0;
        for (long ipmsgId : ipmsgIds) {
            ipmessageIds[i++] = ipmsgId;
        }
        IRCSChatService service = RCSServiceManager.getInstance().getChatService();
        try {
            service.deleteMessages(ipmessageIds);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    
    public void deleteStackMessage(long ipmsgId) {
        IRCSChatService service = RCSServiceManager.getInstance().getChatService();
        try {
            service.deleteMessage(ipmsgId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    
    public void deleteLastBurnedMessage() {
        // delete ip burned message
        SharedPreferences sp = ContextCacher.getPluginContext().getSharedPreferences(IpMessageConsts.BurnedMsgStoreSP.PREFS_NAME, Context.MODE_WORLD_READABLE);
        Set<String> burnedMsgList = sp.getStringSet(IpMessageConsts.BurnedMsgStoreSP.PREF_PREFIX_KEY, null);
        if (burnedMsgList == null) {
            Log.d(TAG, "[BurnedMsg]: onIpMmsCreate() burnedMsgList is null");
            return;
        }
        Log.d(TAG, "[BurnedMsg]: onIpMmsCreate() burnedMsgList = "+burnedMsgList);
        for (String id : burnedMsgList) {
            if (Long.valueOf(id)>0){
                deleteIpMsg(Long.valueOf(id));
            } else {
                deleteFTIpMsg(Long.valueOf(id));
            }
        }
        
		SharedPreferences spDelete = ContextCacher.getPluginContext().getSharedPreferences(IpMessageConsts.BurnedMsgStoreSP.PREFS_NAME, Context.MODE_WORLD_READABLE);
    	SharedPreferences.Editor prefsDelete = spDelete.edit();
    	prefsDelete.clear();
    	prefsDelete.apply();
    	
//    	SharedPreferences spTemp = ContextCacher.getPluginContext().getSharedPreferences(IpMessageConsts.BurnedMsgStoreSP.PREFS_NAME, Context.MODE_WORLD_READABLE);
//    	Set<String> burnedMsgListTemp = spTemp.getStringSet(IpMessageConsts.BurnedMsgStoreSP.PREF_PREFIX_KEY, null);
//    	if (burnedMsgListTemp == null) {
//    		Log.d(TAG, "[BurnedMsg]: onIpMmsCreate() burnedMsgListTemp is null ");
//    	} else {
//    		Log.d(TAG, "[BurnedMsg]: onIpMmsCreate() burnedMsgListTemp = "+burnedMsgListTemp);
//    	}
    }
/*
    private void deleteStackFTMessages(long threadId, Collection<Long> ipmsgIds) {
        Logger.d(TAG, "deleteStackMessages, threadId=" + threadId);
        // delete rcs message in related chat window
        IChatWindow chatWindow = ChatWindowManager.getInstance().getChatWindowByWindowTag(Long.valueOf(threadId));
        if (chatWindow != null) {
            Iterator<Long> iter = ipmsgIds.iterator();
            while (iter.hasNext()) {
                long ipmsgId = iter.next();
                ((BaseChatWindow)chatWindow).removeChatMessage(ipmsgId);
            }
        }
        MessageContainer container = new MessageContainer(ChatWindowManager.getInstance().getChatTagByWindowTag(Long.valueOf(threadId)), threadId);
        container.setChatWindowType(((BaseChatWindow)chatWindow).getWindowType());
        container.setLongArray(ipmsgIds);
        ChatControllerImpl.getInstance().obtainMessage(ChatController.EVENT_DELETE_FT_MESSAGE_LIST, container).sendToTarget();
    }*/
    public int downloadAttach(long IpMsgId, long threadId) {
        Log.d(TAG, "downloadAttach enter, IpMsgId = " + IpMsgId +
            " threadId = " + threadId);

        IpMessage ipMsg = getIpFTMsgInfo(IpMsgId);

        IRCSChatService service = RCSServiceManager.getInstance().getChatService();
        MapInfo info = ThreadMapCache.getInstance().getInfoByThreadId(threadId);
        try {
            if (info != null) {
                // for group chat
                service.acceptGroupFileTransfer(info.getChatId(),((IpAttachMessage)ipMsg).getTag());
            } else {
                // for one2one
            service.acceptFileTransfer(((IpAttachMessage)ipMsg).getTag());
            }         
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return SAVE_SUCCESS;
    }


    public int reDownloadAttach(long IpMsgId, long threadId) {
        // TODO
        Log.d(TAG, "redownloadAttach enter, IpMsgId = " + IpMsgId +
            " threadId = " + threadId);

        IpMessage ipMsg = getIpFTMsgInfo(IpMsgId);

        IRCSChatService service = RCSServiceManager.getInstance().getChatService();
        MapInfo info = ThreadMapCache.getInstance().getInfoByThreadId(threadId);
        try {
            if (info != null) {
                // for group chat
                service.reAcceptGroupFileTransfer(info.getChatId(),((IpAttachMessage)ipMsg).getTag(),IpMsgId);
            } else {
                // for one2one
                service.reAcceptFileTransfer(((IpAttachMessage)ipMsg).getTag(),IpMsgId);
            }         
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return SAVE_SUCCESS;
    }

    public int pauseDownloadAttach(long IpMsgId, long threadId) {
        Log.d(TAG, "pauseDownloadAttach enter, IpMsgId = " + IpMsgId +
            " threadId = " + threadId);

        IpMessage ipMsg = getIpFTMsgInfo(IpMsgId);

        IRCSChatService service = RCSServiceManager.getInstance().getChatService();
        //MapInfo info = ThreadMapCache.getInstance().getInfoByThreadId(threadId);
        try {
            service.pauseFileTransfer(((IpAttachMessage)ipMsg).getTag());        
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return SAVE_SUCCESS;
    }

    public boolean isDownloading(long msgId) {
        return false;
    }

    /**
     * Max length of message a user can send
     */
    
    public int getMaxTextLimit() {
        return 3000;
    }

    private List<String> collectMultiContact(String contact) {
        String[] contacts = contact.split(COMMA);
        List<String> contactSet = new ArrayList<String>();
        for (String singleContact : contacts) {
            // TODO: format number 
            contactSet.add(singleContact);
        }
        return contactSet;
    }

    /**
     * Generate message tag.
     *
     * @return the int
     */
    private int generateMessageTag() {
        int messageTag = RANDOM.nextInt(MESSAGE_TAG_RANGE) + 1;
        messageTag = Integer.MAX_VALUE - messageTag;
        Logger.d(TAG, "generateMessageTag() messageTag: " + messageTag);
        return messageTag;
    }

    private int transferStatusToMms(int status, long timeStampDelivery) {
        int mmsStatus = -1;
        if (timeStampDelivery > 0) {
            return IpMessageConsts.IpMessageStatus.DELIVERED;
        }
        switch (status) {
            case Content.SENDING:
                mmsStatus = IpMessageConsts.IpMessageStatus.OUTBOX;
                break;
            case Content.SENT:
                mmsStatus = IpMessageConsts.IpMessageStatus.SENT;
                break;
            case Content.FAILED:
                mmsStatus = IpMessageConsts.IpMessageStatus.FAILED;
                break;
            default:
                break;
        }
        return mmsStatus;
    }

    public void sendBurnDeliveryReport(String contact,String msgId) {
        Logger.d(TAG,
        "sendBurnDeliveryReport drawDeleteBARMsgIndicator contact = "+contact+" msgId = "+ msgId );
        IRCSChatService service = RCSServiceManager.getInstance().getChatService();
        try {
            service.sendBurnDeliveryReport(contact, msgId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void sendDisplayedDeliveryReport(String contact,String msgId) {
/*        try {
            ChatService chatService = gsmaManager.getChatApi();
            Chat o2oChatImpl = chatService.getChat(contact);
            if (o2oChatImpl != null) {
                o2oChatImpl.sendDisplayedDeliveryReport(msgId);
            }
            
        } catch (JoynServiceException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }*/
    }
    
    public void initSpamReport(String contact,long threadId,long ipMessageId) {
/*        IpMessage ipMessage = getIpMsgInfo(threadId,ipMessageId);
        if (ipMessage == null) {
            Log.d(TAG, "spam-report:  ipMessage is null ");
            return;
        }
        FileTransferService fileTransferService = null;
        ChatService chatService = null;
         try{
             if(ipMessage.getType() == IpMessageType.TEXT) {
                 chatService = GsmaManager.getInstance().getChatApi();
                 if (chatService != null) {
                     Log.d(TAG, "spam-report:  ipMessage.getMessageId() = " + ipMessage.getMessageId() + "  contact = "+contact);
                     chatService.initiateSpamReport(contact,ipMessage.getMessageId());
                 }
             } else {
                 fileTransferService = GsmaManager.getInstance().getFileTransferApi();
                 if (fileTransferService != null) {
                     Log.d(TAG, "spam-report:  ipMessage.getMessageId() = " + ipMessage.getMessageId() + "  contact = "+contact);
                     fileTransferService.initiateFileSpamReport(contact,ipMessage.getMessageId());
                 }
             }
         } catch(JoynServiceException e) {
             e.printStackTrace();
         }*/
    }

    public String getMyNumber() {
        String myNumber = null;
        IRCSChatService service = RCSServiceManager.getInstance().getChatService();
        try {
            myNumber = service.getMSISDN();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return myNumber;
    }
}