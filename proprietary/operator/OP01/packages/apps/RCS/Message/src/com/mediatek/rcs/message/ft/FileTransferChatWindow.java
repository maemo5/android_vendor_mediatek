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

package com.mediatek.rcs.message.ft;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaFile;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.util.Log;

import org.gsma.joyn.chat.ChatLog;
import org.gsma.joyn.chat.ChatMessage;
import org.gsma.joyn.ft.FileTransferLog;
import org.gsma.joyn.ft.MultiFileTransferLog;

import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.MessageStatusUtils.IFileTransfer.Status;
import com.mediatek.rcs.common.INotifyListener;
import com.mediatek.rcs.common.IpMessage;
import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.common.IpMessageConsts.IpMessageStatus;
import com.mediatek.rcs.common.IpAttachMessage;
import com.mediatek.rcs.common.provider.RCSDataBaseUtils;
import com.mediatek.rcs.common.provider.SpamMsgUtils;
import com.mediatek.rcs.common.provider.ThreadMapCache.MapInfo;
import com.mediatek.rcs.common.service.FileStruct;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.common.utils.RCSUtils;

import com.mediatek.rcs.message.chat.GsmaManager;
import com.mediatek.rcs.message.chat.RCSChatServiceImpl;
import android.os.RemoteException;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
//import android.provider.Telephony.Sms;



/**
 * Plugin ChatWindow file transfer
 */
public class FileTransferChatWindow {

    private static final String TAG = "FileTransferChatWindow";

    public static final int STARTING = 0;
    public static final int DOWNLOADING = 1;
    public static final int DONE = 2;

    private FileStruct mFileTransfer;
    private int mMessageBox;
    private String mRemote;
    private Set<String> mRemotes;
    private IpMessage mIpMessage = null;

    public static final int ONE2ONE = 0;
    public static final int ONE2MULTI = 1;
    public static final int GROUP = 2;

    private boolean mBurn = false;
    private int mChatType;

    private long mIpMsgId;  //it is +
    private String mFileTransferTag;

    private String mChatId;
    private long mSmsId = -1;

    private RCSChatServiceImpl mService = null;

    public FileTransferChatWindow(FileStruct fileTransfer, int chatType, String chatId, RCSChatServiceImpl service) {
        mService = service;
        mChatId = chatId;
        mFileTransfer = fileTransfer;
        mChatType = chatType;
        mBurn = fileTransfer.mSessionType;
        mFileTransferTag = fileTransfer.mFileTransferTag;
    }

    public FileTransferChatWindow(FileStruct fileTransfer, int chatType, RCSChatServiceImpl service) {
        mService = service;
        mFileTransfer = fileTransfer;
        mChatType = chatType;
        mBurn = fileTransfer.mSessionType;
        mFileTransferTag = fileTransfer.mFileTransferTag;
    }

    public long saveSendFileTransferToSmsDB() {
        String body = RCSUtils.getSaveBody(mBurn, mFileTransfer.mFilePath);
        Log.d(TAG,"saveSendFileTransferToSmsDB, body = " + body);
        if (mChatType == ONE2ONE) {
            mSmsId = RCSDataBaseUtils.saveSentFileTransferToSmsDB(mFileTransfer.mRemote,mBurn,
                    Long.valueOf(mFileTransfer.mFileTransferTag),body);
            RCSDataBaseUtils.setFileTransferOutBox(mSmsId);
        } else if (mChatType == ONE2MULTI) {
            mSmsId = RCSDataBaseUtils.saveSentFileTransferToSmsDB(mFileTransfer.mRemotes,mBurn,
                Long.valueOf(mFileTransfer.mFileTransferTag),body);
        } /*else if (mChatType == GROUP) {
            mSmsId = RCSDataBaseUtils.saveFileTransferToSmsDB(mFileTransfer.mRemotes,mBurn,
                (mFileTransfer.mFileTransferTag).toLong(),mChatId,,mFileTransfer.mFilePath);
        } */
        
        return mSmsId;
    }

    public long saveReceiveFileTransferToSmsDB() {
        String body = RCSUtils.getSaveBody(mBurn, mFileTransfer.mFilePath);
        Log.d(TAG,"saveReceiveFileTransferToSmsDB, body = " + body);
        mIpMsgId = RCSUtils.findFTIdInRcseDb(ContextCacher.getHostContext().getContentResolver(),mFileTransferTag); 

        if (mChatType == ONE2ONE) {
            if (RCSDataBaseUtils.isIpSpamMessage(mService.getContext(), mFileTransfer.mRemote)) {
                Log.d(TAG, "onReceiveChatMessage, spam msg, contact=" + mFileTransfer.mRemote);
                SpamMsgUtils.getInstance(mService.getContext()).insertSpamFtIpMsg(
                        body, mFileTransfer.mRemote, RCSUtils.getRCSSubId(), mIpMsgId);
                return -1;
            }
        }
      
         
        if (mChatType == ONE2ONE) {
            mSmsId = RCSDataBaseUtils.saveReceivedFileTransferToSmsDB(mFileTransfer.mRemote,mBurn, mIpMsgId, body); 
        } else if (mChatType == GROUP) {
            mSmsId = RCSDataBaseUtils.saveReceivedFileTransferToSmsDBInGroup(mFileTransfer.mRemote, mIpMsgId, mChatId, body);
        }
        
        mService.getListener().onNewMessage(mSmsId);
        

//        Set<String> recipient = new HashSet<String>();
//        recipient.add(mFileTransfer.mRemote);
//        long threadId = Threads.getOrCreateThreadId(ContextCacher.getHostContext(), recipient);
//        notifyNewFileTransfer(threadId,mFileTransfer.mRemote, mSmsId);
        return mSmsId;
    }

//    private void notifyNewFileTransfer(long threadId, String number, long smsId) {
//        Log.d(TAG, "notifyNewMessage, entry: threadId=" + threadId);
//        Intent intent = new Intent();
//        intent.setAction(IpMessageConsts.MessageAction.ACTION_NEW_MESSAGE);
//        intent.putExtra(IpMessageConsts.MessageAction.KEY_THREAD_ID, threadId);
//        intent.putExtra(IpMessageConsts.MessageAction.KEY_IPMSG_ID, mIpMsgId);
//        intent.putExtra(IpMessageConsts.MessageAction.KEY_NUMBER, number);
//        intent.putExtra(IpMessageConsts.MessageAction.KEY_SMS_ID, smsId);
//        List<INotifyListener> listeners = RCSServiceManager.getInstance().getNotifyListeners();
//        Log.d(TAG, "notifyNewMessage, listeners =  " + listeners);
//        for (INotifyListener l : listeners) {
//            l.notificationsReceived(intent);
//            Log.d(TAG, "notifyNewMessage, l= " + l);
//        }
//        mRcsNotificationsManager.notify(intent);
//    }

    public void updateInfo(String newFileTransferTag, String oldFileTransferTag){ 
        Log.d(TAG, "updateInfo enter, newFileTransferTag = " + newFileTransferTag + 
            "oldFileTransferTag" + oldFileTransferTag);
        mFileTransfer.mFileTransferTag = newFileTransferTag;
        mFileTransferTag = newFileTransferTag;

        mSmsId = getSmsId(oldFileTransferTag);
        mIpMsgId = RCSDataBaseUtils.updateFTMsgId(mSmsId,newFileTransferTag);
    }

    public void setSendFail(String fileTransferTag) {
        if (mSmsId != -1) {
            RCSDataBaseUtils.setFileTransferFail(mSmsId);
        } else {
            mSmsId = getSmsId(fileTransferTag);
            RCSDataBaseUtils.setFileTransferFail(mSmsId);
        }
        
    }

    private long getSmsId(String fileTransferTag) {
        return RCSDataBaseUtils.getSmsIdfromFid(fileTransferTag);     
    }

    public void updateInfo(String newFileTransferTag) {
        mFileTransferTag = newFileTransferTag;
    }

    public void updateFTStatus(Status status) {
        //Notify app
        Log.d(TAG, "updateFTStatus enter, Status = " + status);
        updateStatus(-mIpMsgId,status);
    }

    private void updateStatus(long ipMsgId, Status status) {
       
        Uri uri;
        long smsId = -1;
        int type = -1;

        Log.d(TAG, "updateStatus enter, Status = " + status + " ipMsgId = " + ipMsgId);

        ContentResolver cv = ContextCacher.getHostContext().getContentResolver();
        if (cv != null) {           
            smsId = RCSUtils.getIdInMmsDb(cv, ipMsgId);   
            type = RCSUtils.getIdAndTypeInMmsDb(cv, ipMsgId); 
            Log.d(TAG, "smsId = " + smsId + " type " + type);
        }
        if (smsId > 0) {
            uri = ContentUris.withAppendedId(Sms.CONTENT_URI, smsId);
                switch(status) {
                case FINISHED:
                    Log.d(TAG, " drawDeleteBARMsgIndicator FT smsId = " + smsId + ",type = " + type);
                    if (type == Sms.MESSAGE_TYPE_OUTBOX || type == Sms.MESSAGE_TYPE_FAILED) {
                        
                        int stat = RCSUtils.getIntStatus(status);
                        mService.getListener().onUpdateFileTransferStatus(mIpMsgId, stat,
                                Sms.MESSAGE_TYPE_SENT);

                        RCSDataBaseUtils.updateFileTransferSent(mFileTransferTag);
                        deleteBurnedMsg(ipMsgId);
                    }  else {
                        int stat = RCSUtils.getIntStatus(status);
                        mService.getListener().onUpdateFileTransferStatus(mIpMsgId, stat,
                                Sms.MESSAGE_TYPE_INBOX);
                    } 
                    break;
                case TRANSFERING:
                    if (type == Sms.MESSAGE_TYPE_FAILED) {
                        RCSDataBaseUtils.updateFileTransferOutBox(mFileTransferTag);
                    } 
                    break;
                case WAITING:
                    break;
                case CANCEL:
                    break;
                case FAILED:
                    if (type == Sms.MESSAGE_TYPE_OUTBOX) {

                        int stat = RCSUtils.getIntStatus(status);
                        mService.getListener().onUpdateFileTransferStatus(mIpMsgId, stat,
                                Sms.MESSAGE_TYPE_FAILED);
                        
                        RCSDataBaseUtils.updateFileTransferFail(mFileTransferTag);
                    } else if (type == Sms.MESSAGE_TYPE_INBOX){
                        int stat = RCSUtils.getIntStatus(status);
                        mService.getListener().onUpdateFileTransferStatus(mIpMsgId, stat,
                                Sms.MESSAGE_TYPE_INBOX);
                        Log.d(TAG, "download file fail !");
                    }
                    break;
                case TIMEOUT:
                    break;
                case REJECTED:
                    if (type == Sms.MESSAGE_TYPE_OUTBOX) {

                        int stat = RCSUtils.getIntStatus(status);
                        mService.getListener().onUpdateFileTransferStatus(mIpMsgId, stat,
                                Sms.MESSAGE_TYPE_FAILED);
                        RCSDataBaseUtils.updateFileTransferFail(mFileTransferTag);
                    } else {
                        Log.d(TAG, "download file fail !");
                        int stat = RCSUtils.getIntStatus(status);
                        mService.getListener().onUpdateFileTransferStatus(mIpMsgId, stat,
                                Sms.MESSAGE_TYPE_INBOX);
                    }
                    break;
                case PENDING:
                    break;
                default:
                    break;
                }
        }
        
    }

    private void deleteBurnedMsg(final long ipMsgId) {
    	Log.d(TAG, " drawDeleteBARMsgIndicator deleteBurnedMsg()");
    	//RCSUtils.updateSmsDateSent(uri, System.currentTimeMillis());
        ContentResolver resolver = ContextCacher.getHostContext().getContentResolver();
        String[] projection = { FileTransferLog.SESSION_TYPE };
        final Cursor cursor = resolver.query(RCSUtils.RCS_URI_FT, projection, "_id=" + -ipMsgId, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                	boolean sessionType  = (cursor.getInt(cursor.getColumnIndex(FileTransferLog.SESSION_TYPE)) == 1) ? true:false;
                	Log.d(TAG, " drawDeleteBARMsgIndicator deleteBurnedMsg, sessionType=" + sessionType+ " ipMsgId = "+ipMsgId);
                	if(sessionType){
                		new Thread(new Runnable() {
                            public void run() {
                            	try {
                                    Thread.sleep(5500);
                                    ContextCacher.getHostContext().getContentResolver().delete(Sms.CONTENT_URI, Sms.IPMSG_ID + " = " + ipMsgId, null);
                	                RCSDataBaseUtils.deleteMessage(ipMsgId);
                	                }
                            	 catch (Exception e) {
                            		    e.printStackTrace();
                            	 	}
                            	}
                        	}).start();
                		}
                	}
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
        return ;
    }
    
/*
    private void notifyReceiveFT() {
         // notify app
        Intent intent = new Intent();
        intent.setAction(IpMessageConsts.MessageAction.ACTION_NEW_MESSAGE);
        intent.putExtra(IpMessageConsts.MessageAction.KEY_THREAD_ID, idInMms);
        ContextCacher.getHostContext().sendBroadcast(intent);  
    }
*/
    public void setFilePath(String filePath) {
        Log.d(TAG, "setFilePath() entry! filePath = " + filePath);
        RCSDataBaseUtils.updateFTMsgFilePath(filePath,mIpMsgId);
        mService.getListener().setFilePath(mIpMsgId,filePath);
    }

    public static  void onReceiveMessageDeliveryStatus(String fid, String status) {
        Log.d(TAG, "onReceiveMessageDeliveryStatus() entry! fid = " + fid + "status = " + status);
        if (status.equalsIgnoreCase("delivered")) {
            Log.d(TAG, "send filetransfer delivered !!");
            
            RCSDataBaseUtils.updateFileTransferDelivered(fid);
        }
    }

    
    













    
/*

    
    public void setFilePath(String filePath) {
        Log.d(TAG, "setFilePath() entry! filePath = " + filePath);
        if (mIpMessage == null) {
            long ipMsgId = mChatWindow.getFTIpMsgId(mFileTransfer.mFileTransferTag);
            IpMessage ipMessage = mChatWindow.getChatMessage(ipMsgId);
            mIpMessage = ipMessage;
        }
 
        if (mIpMessage != null && mIpMessage instanceof IpAttachMessage) {
            ((IpAttachMessage) mIpMessage).setPath(filePath);
        }   
    }

    public void setProgress(long progress) {
        Log.d(TAG, "setProgress() entry! progress = " + progress);

    }

    public void setStatus(Status status) {
        Log.d(TAG, "setStatus() entry! status = " + status);
        //int statusInMms = convertStatus(status);
        
        long ipMsgId = mChatWindow.getFTIpMsgId(mFileTransfer.mFileTransferTag);
        IpMessage ipMessage = mChatWindow.getChatMessage(ipMsgId);
        mIpMessage = ipMessage;

        if (ipMessage != null) {
            //get this ipMessage from chatwindow cache
            int messageType = ipMessage.getType();
                switch (messageType) {
                    case IpMessageConsts.IpMessageType.PICTURE:
                         //((RCSImageMessage) ipMessage).setStatus((int)status);
                         ((IpImageMessage) ipMessage).setRcsStatus(status);
                        break;
                    case IpMessageConsts.IpMessageType.VIDEO:
                        //((RCSVideoMessage) ipMessage).setStatus((int)status);
                        ((IpVideoMessage) ipMessage).setRcsStatus(status);
                        break;
                    case IpMessageConsts.IpMessageType.VOICE:
                        //((RCSVoiceMessage) ipMessage).setStatus((int)status);
                        ((IpVoiceMessage) ipMessage).setRcsStatus(status);
                        break;
                    case IpMessageConsts.IpMessageType.VCARD:
                        //((RCSVcardMessage) ipMessage).setStatus((int)status);
                        ((IpVCardMessage) ipMessage).setRcsStatus(status);
                        break;
                     case IpMessageConsts.IpMessageType.GEOLOC:
                        //((RCSVcardMessage) ipMessage).setStatus((int)status);
                        ((IpGeolocMessage) ipMessage).setRcsStatus(status);
                        break;
                    default:
                        Log.e(TAG,"setStatus, Nonsupport message type");
                }

                //Notify app
                Intent it = new Intent();
                it.setAction(IpMessageConsts.IpMessageStatus.ACTION_MESSAGE_STATUS);
                it.putExtra(IpMessageConsts.STATUS, status);
                it.putExtra(IpMessageConsts.IpMessageStatus.IP_MESSAGE_ID, ipMsgId);
                mRcsNotificationsManager.notify(it);

               updateStatus(ipMsgId,status,ipMessage);
        }
    }




    public void updateTag(String transferTag, long transferSize, boolean isMulti) {
        String oldTag = mFileTransfer.mFileTransferTag;  //dummyid in common cache
        mFileTransfer.mFileTransferTag = transferTag;    //real stack ftId
        mFileTransfer.mSize = transferSize;          //file size

        String remote = null; //contact
        long ipMsgId = -1;
        int duration = 0;
        int sessionType = 0; //is burn
        IpMessage ipMessage;

        if (isMulti) {
            ContentResolver resolver = ContextCacher.getHostContext().getContentResolver();
            Cursor cursor = resolver.query(RCSUtils.RCS_URI_FT_MULTI, RCSUtils.PROJECTION_FILE_TRANSFER_IN_MULTI, "ft_id=" + transferTag, null, null);
            try {
                    if (cursor != null && cursor.moveToFirst()) {
                        remote = cursor.getString(cursor.getColumnIndex(MultiFileTransferLog.CONTACT_NUMBER));
                        duration = cursor.getInt(cursor.getColumnIndex(MultiFileTransferLog.DURATION));
                        ipMsgId = cursor.getLong(cursor.getColumnIndex(MultiFileTransferLog.ID));
                    }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else {
        ContentResolver resolver = ContextCacher.getHostContext().getContentResolver();
        Cursor cursor = resolver.query(RCSUtils.RCS_URI_FT, RCSUtils.PROJECTION_FILE_TRANSFER, "ft_id=" + transferTag, null, null);
        try {
                if (cursor != null && cursor.moveToFirst()) {
            remote = cursor.getString(cursor.getColumnIndex(FileTransferLog.CONTACT_NUMBER));
            //String thumbNail = null; //cursor.getString(cursor.getColumnIndex(FileTransferLog.CONTACT_NUMBER));
            //Date date = null;
                    sessionType = cursor.getInt(cursor.getColumnIndex(FileTransferLog.SESSION_TYPE));
            duration = cursor.getInt(cursor.getColumnIndex(FileTransferLog.DURATION));
            ipMsgId = cursor.getLong(cursor.getColumnIndex(FileTransferLog.ID));
        }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        }


        //generate ipMessage from fileStruct
        //by chuangjie
        ipMessage = RCSUtils.analysisFileType(remote,mFileTransfer);

        Log.e(TAG, "yangfeng test --- ipMsgId = "+ipMsgId);
        //set ipMsgid
        if (ipMsgId > 0 && isMulti) {
            ipMsgId = -(ipMsgId + RCSUtils.MULTI_FT_BASE_NUMBER);
        } else if (ipMsgId > 0){
            ipMsgId = -ipMsgId;
        } else {
            Log.e(TAG, "RcsChatWindowFileTransfer.updateTag error");
            return;
        }
        ipMessage.setIpDbId(ipMsgId);

        //set messageId or ftId
        ipMessage.setMessageId(transferTag);

        //set is burn
        if (sessionType == 1)
            ipMessage.setBurnedMessage(true);
        else
            ipMessage.setBurnedMessage(false);

        //set duration if it is voice or video
        if (ipMessage.getType() == IpMessageConsts.IpMessageType.VIDEO) {
            ((IpVideoMessage)ipMessage).setDuration(duration);
        } else if(ipMessage.getType() == IpMessageConsts.IpMessageType.VOICE) {
            ((IpVoiceMessage)ipMessage).setDuration(duration);
        }

        mChatWindow.cacheMessage(ipMessage);

        //update mms db
        updateMessageIdInMmsDb(oldTag,ipMsgId);

        //remove ipmessage in common cache
        Log.d(TAG, "RCSMessageManager.sCachedSendMessage.remove old ftId = " + (-Long.valueOf(oldTag)));
        RCSMessageManager.sCachedSendMessage.remove(-Long.valueOf(oldTag));
    }
 
    static void updateMessageIdInMmsDb(String oldId, Long newId) {
        Log.d(TAG, "updateMessageIdInMmsDb() entry");
        long smsId = -1;

        ContentResolver cv = ContextCacher.getHostContext().getContentResolver();
        if (cv != null) {           
            smsId = RCSUtils.getIdInMmsDb(cv, -(Long.parseLong(oldId)));
            ContentValues contentValues = new ContentValues();
            contentValues.put(Sms.IPMSG_ID, newId);
            cv.update(Sms.CONTENT_URI, contentValues, Sms._ID + "=" + smsId, null);
        } else {
            Log.e(TAG, "getIdInMmsDb(), cr is null!");
        }
    }

    */
}

