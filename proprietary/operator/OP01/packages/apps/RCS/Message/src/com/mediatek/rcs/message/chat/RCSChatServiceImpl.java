/*
 * Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are protected under
 * relevant copyright laws. The information contained herein is confidential and proprietary to
 * MediaTek Inc. and/or its licensors. Without the prior written permission of MediaTek inc. and/or
 * its licensors, any reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES THAT THE
 * SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE") RECEIVED FROM MEDIATEK AND/OR ITS
 * REPRESENTATIVES ARE PROVIDED TO RECEIVER ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS
 * ANY AND ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT. NEITHER DOES MEDIATEK
 * PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED
 * BY, INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO
 * SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT
 * IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN
 * MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE
 * TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM. RECEIVER'S SOLE
 * AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK
 * SOFTWARE RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK
 * SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software") have been
 * modified by MediaTek Inc. All revisions are subject to any receiver's applicable license
 * agreements with MediaTek Inc.
 */
package com.mediatek.rcs.message.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.capability.Capabilities;
import org.gsma.joyn.capability.CapabilityService;
import org.gsma.joyn.chat.ChatIntent;
import org.gsma.joyn.chat.ChatMessage;
import org.gsma.joyn.chat.ChatService;
import org.gsma.joyn.chat.GroupChatIntent;
import org.gsma.joyn.ft.FileTransferIntent;
import org.gsma.joyn.ft.FileTransferService;
import org.gsma.joyn.ft.FileTransferServiceConfiguration;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Telephony.Sms;
import android.util.Log;

import com.mediatek.rcs.common.provider.RCSDataBaseUtils;
import com.mediatek.rcs.common.service.IRCSChatService;
import com.mediatek.rcs.common.service.IRCSChatServiceListener;
import com.mediatek.rcs.common.service.Participant;
import com.mediatek.rcs.common.utils.Logger;
import com.mediatek.rcs.message.ft.FileTransferManager;

public class RCSChatServiceImpl extends IRCSChatService.Stub {
    public final String TAG = "RCSChatServiceImpl";
    // public final String TAG = this.toString().substring(this.toString().lastIndexOf('.') + 1);

    // The map retains Object&IChatBase
    private final Map<String, One2OneChat> mO2OChatMap = new ConcurrentHashMap<String, One2OneChat>();
    private final Map<String, SimpleGroupChat> mGroupChatMap = new ConcurrentHashMap<String, SimpleGroupChat>();

    private RCSChatManagerReceiver mReceiver = null;
    private RCSChatServiceListenerWrapper mListener = null;
    private Service mService = null;

    private FileTransferManager mFTManager = null; //for filetransfer

    private PendingMessageManager mPendingMsgManager = null;

    private boolean mGroupInitComplete = true;

    private Handler mWorkHandler = null;
    private Handler mNotifyHandler = null;

    public RCSChatServiceImpl(Service service) {
        Logger.d(TAG, "RCSChatServiceImpl #Constructor");
        mService = service;
        GsmaManager.initialize(this);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ChatIntent.ACTION_NEW_CHAT);
        intentFilter.addAction(ChatIntent.ACTION_DELIVERY_STATUS);
        intentFilter.addAction(GroupChatIntent.ACTION_NEW_INVITATION);
        intentFilter.addAction(FileTransferIntent.ACTION_NEW_INVITATION);
        intentFilter.addAction(FileTransferIntent.ACTION_DELIVERY_STATUS);

        mReceiver = new RCSChatManagerReceiver();
        mService.registerReceiver(mReceiver, intentFilter);

        mPendingMsgManager = new PendingMessageManager(this);
        mFTManager = FileTransferManager.getInstance(this);

        HandlerThread thread = new HandlerThread("RCSChatServiceImplWorker");
        thread.start();
        mWorkHandler = new WorkHandler(thread.getLooper());

        thread = new HandlerThread("RCSChatServiceImplNotifyer");
        thread.start();
        mNotifyHandler = new Handler(thread.getLooper());
        mListener = new RCSChatServiceListenerWrapper(service, mNotifyHandler);
    }

    public void onDestroy() {
        GsmaManager.unInitialize();
        mService.unregisterReceiver(mReceiver);
    }

    public Handler getWorkHandler() {
        return mWorkHandler;
    }

    public Handler getNotifyHandler() {
        return mNotifyHandler;
    }

    public PendingMessageManager getPendingMessageManager() {
        return mPendingMsgManager;
    }

    public Context getContext() {
        return mService;
    }

    public RCSChatServiceListenerWrapper getListener() {
        return mListener;
    }

    public One2OneChat getOne2OneChat(String contact) {
        One2OneChat chat = mO2OChatMap.get(contact);
        if (chat == null) {
            chat = new One2OneChat(this, contact, contact);
            mO2OChatMap.put(contact, chat);
        }
        return chat;
    }

    public SimpleGroupChat getGroupChat(String chatId) {
        SimpleGroupChat groupChat = mGroupChatMap.get(chatId);
        if (groupChat == null) {
            Logger.v(TAG, "getGroupChat NULL Warningg!!!");
        }
        return groupChat;
    }

    public SimpleGroupChat getOrCreateGroupChat(String chatId) {
        SimpleGroupChat groupChat = mGroupChatMap.get(chatId);
        if (groupChat == null) {
            Logger.v(TAG, "getOrCreateGroupChat Create One!!!");
            groupChat = new SimpleGroupChat(this, chatId);
            mGroupChatMap.put(chatId, groupChat);
        }
        return groupChat;
    }

    public void removeGroupChat(String chatId) {
        mGroupChatMap.remove(chatId);
    }

    private void handleO2OInvitation(Intent intent) {
        Logger.v(TAG, "handleOne2OneChatReceivedMessage() entry");
        String contactNumber = intent.getStringExtra(ChatIntent.EXTRA_CONTACT);
        String displayName = intent.getStringExtra(ChatIntent.EXTRA_DISPLAY_NAME);
        ChatMessage chatMessage = intent.getParcelableExtra(ChatIntent.EXTRA_MESSAGE);
        Logger.v(TAG, "handleOne2OneChatReceivedMessage contact:" + contactNumber);

        if (chatMessage == null) {
            Logger.v(TAG, "Just a invitation for larger mode.");
            return;
        } else if (chatMessage.isPublicMessage()) {
            return;
        }
        Logger.v(TAG, "handleOne2OneChatReceivedMessage MessageId: " + chatMessage.getId());
        One2OneChat chat = getOne2OneChat(contactNumber);
        chat.onReceiveChatMessage(chatMessage);
    }

    private void handleMessageDeliveryStatus(Intent intent) {
        // ChatIntent.ACTION_DELIVERY_STATUS
        String remoteContact = intent.getStringExtra(ChatIntent.EXTRA_CONTACT);
        String msgId = intent.getStringExtra("msgId");
        String status = intent.getStringExtra("status");
        Logger.v(TAG, "handleMessageDeliveryStatus() from broadcast, msgId: " + msgId
                + ", status: " + status);
        One2OneChat chat = getOne2OneChat(remoteContact);
        chat.onReceiveMessageDeliveryStatus(msgId, status);
        return;
    }

    private void handleGroupInvitation(Intent intent) {
        Logger.v(TAG, "handleNewGroupInvitation() entry");
        String contact = intent.getStringExtra(GroupChatIntent.EXTRA_CONTACT);
        String displayName = intent.getStringExtra(GroupChatIntent.EXTRA_DISPLAY_NAME);
        String chatId = intent.getStringExtra(GroupChatIntent.EXTRA_CHAT_ID);
        String subject = intent.getStringExtra(GroupChatIntent.EXTRA_SUBJECT);
        // boolean isGroupExist = intent.getBooleanExtra("isGroupChatExist", false);
        String rejoinId = intent.getStringExtra(GroupChatIntent.EXTRA_SESSION_IDENTITY);
        Logger.v(TAG, "handleNewGroupInvitation contact:" + contact + " displayName: "
                + displayName + " chatId: " + chatId + " subject: " + subject + " rejoinId: "
                + rejoinId);
        List<Participant> participantList = new ArrayList<Participant>();
        participantList.add(new Participant(contact, displayName));
        SimpleGroupChat groupChat = mGroupChatMap.get(chatId);
        if (groupChat == null) {
            groupChat = new SimpleGroupChat(this, subject, participantList);
            groupChat.setChatId(chatId);            
            mGroupChatMap.put(chatId, groupChat);
        }
        groupChat.handleInvitation(rejoinId);
    }

    private void handleGroupFTInvitation(Intent intent) {
        Logger.v(TAG, "handleGroupFTInvitation() entry");
        String chatId = intent.getStringExtra(GroupChatIntent.EXTRA_CHAT_ID);
        SimpleGroupChat groupChat = mGroupChatMap.get(chatId);
        if (groupChat == null) {
            Logger.v(TAG, "handleGroupFTInvitation() groupChat == null");
            return;
        }
        groupChat.handleFTInvitation(intent);
    }

    public void handleRegistrationStatusChanged(boolean status) {
        for (SimpleGroupChat groupChat : mGroupChatMap.values()) {
            groupChat.onStatusChanged(status);
        }
        for (One2OneChat o2oChat : mO2OChatMap.values()) {
            o2oChat.onStatusChanged(status);
        }
        mFTManager.onStatusChanged(status);
        mPendingMsgManager.handleRegistrationChanged(status);
    }

    public void handleCoreServiceDown() {
        for (SimpleGroupChat groupChat : mGroupChatMap.values()) {
            groupChat.onCoreServiceDown();
        }
    }

    public void handleCapabilityChanged(String contact, Capabilities capability) {
        Logger.d(TAG, "handleCapabilityChanged #contact: " + contact);
        One2OneChat chat = getOne2OneChat(contact);
        chat.handleCapabilityChanged(capability);
    }

    @Override
    public boolean getRCSStatus() throws RemoteException {
        return GsmaManager.getInstance().getRCSStatus();
    }

    @Override
    public boolean getConfigurationStatus() throws RemoteException {
        return GsmaManager.getInstance().getConfigurationStatus();
    }

    @Override
    public boolean getRegistrationStatus() throws RemoteException {
        return GsmaManager.getInstance().getRegistrationStatus();
    }

    @Override
    public void getBurnMessageCapability(String contact) throws RemoteException {
        try {
            CapabilityService capabilityService = GsmaManager.getInstance().getCapabilityApi();
            Capabilities capability = capabilityService.getContactCapabilities(contact);
            if (capability != null) {
                One2OneChat chat = getOne2OneChat(contact);
                chat.handleCapabilityChanged(capability);
            }
            capabilityService.requestContactCapabilities(contact);
        } catch (JoynServiceException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getMSISDN() throws RemoteException {
        return GsmaManager.getInstance().getMSISDN();
    }

    @Override
    public void sendOne2OneMessage(String contact, String content) throws RemoteException {
        Logger.d(TAG, "sendOne2OneMessage #contact: " + contact + ", content: " + content);
        One2OneChat chat = getOne2OneChat(contact);
        long msgIdInSMS = RCSDataBaseUtils.saveMsgToSmsDB(contact, content, false);
        chat.sendChatMessage(msgIdInSMS, content, 0);
    }

    @Override
    public void sendOne2MultiMessage(List<String> contacts, String content) throws RemoteException {
        Logger.d(TAG, "sendOne2MultiMessage #contacts: " + contacts + ", content: " + content);
        long smsId = RCSDataBaseUtils.saveMsgToSmsDBForOne2Multi(contacts, content);
        new One2MultiChat(this, null, contacts).sendChatMessage(smsId, content);
    }

    @Override
    public void sendBurnMessage(String contact, String content) throws RemoteException {
        Logger.d(TAG, "sendOne2OneMessage #contact: " + contact + ", content: " + content);
        One2OneChat chat = getOne2OneChat(contact);
        long msgIdInSMS = RCSDataBaseUtils.saveMsgToSmsDB(contact, content, true);
        chat.sendChatMessage(msgIdInSMS, content, 1);
    }

    @Override
    public void sendBurnDeliveryReport(String contact, String msgId) throws RemoteException {
        Logger.d(TAG, "sendBurnDeliveryReport #contact: " + contact + ", msgId: " + msgId);
        One2OneChat chat = getOne2OneChat(contact);
        //String msgId = RCSDataBaseUtils.findMsgIdInRcsDb(ipmsgId);
        chat.sendBurnDeliveryReport(msgId);
    }

    @Override
    public void blockMessages(String chatId, boolean block) throws RemoteException {
        Logger.d(TAG, "blockMessages #chatId: " + chatId + ", block=" + block);
        try {
            ChatService chatService = GsmaManager.getInstance().getChatApi();
            if (chatService != null) {
                chatService.blockGroupMessages(chatId, block);
            }
        } catch (JoynServiceException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendOne2OneFileTransfer(String contact, String filePath) throws RemoteException {
        Log.d(TAG, "sendOne2OneFileTransfer() enter, contact = " + contact + "filePath = "
                + filePath);
        boolean isBurn = false;
        mFTManager.handleSendFileTransferInvitation(contact, filePath, isBurn);
    }

    @Override
    public void sendOne2OneBurnFileTransfer(String contact, String filePath) throws RemoteException {
        Log.d(TAG, "sendOne2OneBurnFileTransfer() enter, contact = " + contact + "filePath = "
                + filePath);
        boolean isBurn = true;
        mFTManager.handleSendFileTransferInvitation(contact, filePath, isBurn);
    }

    @Override
    public void sendOne2MultiFileTransfer(List<String> contacts, String filePath)
            throws RemoteException {
        Log.d(TAG, "sendOne2MultiFileTransfer() enter, contacts = " + contacts + "filePath = "
                + filePath);
        mFTManager.handleSendFileTransferInvitation(contacts, filePath);
    }

    @Override
    public void sendGroupFileTransfer(String chatId, String filePath) throws RemoteException {
        Log.d(TAG, "sendGroupChatFileTransfer() enter, filePath = " + filePath);
        // save db;
        Random RANDOM = new Random();
        int messageTag = RANDOM.nextInt(1000) + 1;
        messageTag = Integer.MAX_VALUE - messageTag;
        int dummyId = messageTag;
        long dummIpMsgId = Long.valueOf(dummyId);

        boolean isBurn = false;
        //ThreadMapCache.createInstance();
        //MapInfo info = ThreadMapCache.getInstance().getInfoByChatId(chatId);
        //long threadId = info.getThreadId();

        long msgIdInSMS = RCSDataBaseUtils.saveSentFileTransferToSmsDBInGroup(chatId, isBurn,
                dummIpMsgId, filePath);
        
        SimpleGroupChat groupChat = getOrCreateGroupChat(chatId);
        groupChat.sendFile(-dummIpMsgId, filePath);
    }

    @Override
    public void resendGroupFileTransfer(String chatId, long msgId) throws RemoteException {
        Log.d(TAG, "sendGroupChatFileTransfer() enter, msgId = " + msgId);
        SimpleGroupChat groupChat = getOrCreateGroupChat(chatId);
        groupChat.resendFile(msgId);
    }

    @Override
    public void resendFileTransfer(long ipMsgId) throws RemoteException {
        // Log.d(TAG, "resendFileTransfer() enter, contact = " + contact + "filePath = " +
        // filePath);
       //Log.d(TAG, "resendFileTransfer() enter, fid = " + fid + "isBurn = " + isBurn);
       Log.d(TAG, "resendFileTransfer() enter,ipMsgId = " +ipMsgId);
       mFTManager.handleResendFileTransfer(ipMsgId);
    }

    @Override
    public void resendOne2MultiFileTransfer(long index) throws RemoteException {
        // TODO Auto-generated method stub
    }

    @Override
    public void acceptFileTransfer(String fileTransferTag) throws RemoteException {
        Log.d(TAG, "acceptFileTransfer() enter,  fileTransferTag = " + fileTransferTag);
        mFTManager.handleAcceptFileTransfer(fileTransferTag);
    }

    @Override
    public void acceptGroupFileTransfer(String chatId, String fileTransferTag)
            throws RemoteException {
        Log.d(TAG, "acceptGroupFileTransfer() enter,  #chatId = " + chatId + ", #fileTransferTag"
                + fileTransferTag);
        getOrCreateGroupChat(chatId).downloadFile(fileTransferTag);
    }

    public void reAcceptFileTransfer(String fileTransferTag, long ipMessageId)
            throws RemoteException {
        Log.d(TAG, "reacceptFileTransfer() enter,  fileTransferTag = " + fileTransferTag
                + "ipMessageId = " + ipMessageId);
        mFTManager.handleReAcceptFileTransfer(fileTransferTag, ipMessageId);
    }

    @Override
    public void reAcceptGroupFileTransfer(String chatId, String fileTransferTag, long ipMessageId)
            throws RemoteException {
        Log.d(TAG, "reacceptGroupFileTransfer() enter,  #chatId = " + chatId + ", #fileTransferTag"
                + fileTransferTag + "#ipMessageId = " + ipMessageId);
        getOrCreateGroupChat(chatId).redownloadFile(fileTransferTag,ipMessageId);
    }

    @Override
    public void resumeFileTransfer() throws RemoteException {
        // TODO Auto-generated method stub
    }

    public void pauseFileTransfer(String fileTransferTag)
        throws RemoteException {
        Log.d(TAG, "pauseFileTransfer() enter,  fileTransferTag = " + fileTransferTag);
        mFTManager.handlePauseFileTransfer(fileTransferTag);
    }

    // add by Feng.
    @Override
    public long getRcsFileTransferMaxSize() throws RemoteException {
        FileTransferService fileTransferService = null;
        FileTransferServiceConfiguration fileTransferConfig = null;
        GsmaManager instance = GsmaManager.getInstance();
        long maxSize = 0;

        if (instance != null) {
            try {
                fileTransferService = instance.getFileTransferApi();
            } catch (JoynServiceException e) {
                e.printStackTrace();
            }
        } else {
            return 0;
        }

        if (fileTransferService != null) {
            try{
                fileTransferConfig = fileTransferService.getConfiguration();
            } catch(JoynServiceException e) {
                Logger.e(TAG,"getfileTransferConfig error ");
            }
             
        }
       
        if (fileTransferConfig != null) {
            maxSize = fileTransferConfig.getMaxSize();
        }
        return maxSize;
    }

    @Override
    public void resendMessage(String contact, long index) throws RemoteException {
        Logger.d(TAG, "resendMessage #contact: " + contact + ", #index: " + index);
        One2OneChat chat = getOne2OneChat(contact);
        long ipMsgId = RCSDataBaseUtils.findIpMsgIdInSmsDb(index);
        if (ipMsgId != Integer.MAX_VALUE) {
            String msgId = RCSDataBaseUtils.findMsgIdInRcsDb(ipMsgId);
            chat.resendChatMessage(msgId);
        } else {
            String body = RCSDataBaseUtils.findIpMsgTextInSmsDb(index);
            boolean burn = RCSDataBaseUtils.findIpMsgBurnFlagInSmsDb(index);
            if (burn)
                chat.sendChatMessage(index, body, 1);
            else
                chat.sendChatMessage(index, body, 0);
        }

        Uri uri = ContentUris.withAppendedId(Sms.CONTENT_URI, index);
        RCSDataBaseUtils.updateSmsBoxType(uri, Sms.MESSAGE_TYPE_OUTBOX);
    }

    @Override
    public void resendMultiMessage(long index) throws RemoteException {
        // TODO Auto-generated method stub
    }

    @Override
    public void deleteMessage(long index) throws RemoteException {
        Logger.i(TAG, "removeMessage msgId: " + index);
        // delete message from database //need check, post to a thread;
        RCSDataBaseUtils.deleteMessage(index);
    }

    @Override
    public void deleteMessages(long[] indexs) throws RemoteException {
        for (long index : indexs) {
            deleteMessage(index);
        }
    }

    @Override
    public void deleteO2OMessages(String contact) throws RemoteException {
        Logger.i(TAG, "deleteO2OMessages contact: " + contact);
        RCSDataBaseUtils.deleteMessageByChat(contact);
    }

    @Override
    public void deleteGroupMessages(String chatId) throws RemoteException {
        Logger.i(TAG, "deleteGroupMessages chatId: " + chatId);
        RCSDataBaseUtils.deleteMessageByChat(chatId);
    }

    @Override
    public synchronized void startGroups(List<String> chatIds) throws RemoteException {
        if (mGroupInitComplete) {
            Logger.d(TAG, "startGroups #mGroupInitComplete true");
            return;
        }
        Logger.d(TAG, "startGroups #group count: " + chatIds.size());
        for (String chatId : chatIds) {
            Logger.d(TAG, "startGroups #chatId: " + chatId);
            if (mGroupChatMap.containsKey(chatId)) {
                Logger.d(TAG, "startGroups #chatId: " + chatId + " exist~");
                continue;
            }
            SimpleGroupChat groupChat = new SimpleGroupChat(this, chatId);
            mGroupChatMap.put(chatId, groupChat);
            groupChat.updateGroupStatus();
        }
        mGroupInitComplete = true;
    }

    @Override
    public String initGroupChat(String subject, List<String> contacts) throws RemoteException {
        Logger.d(TAG, "initGroupChat #contacts: " + contacts + ", subject: " + subject);
        List<Participant> participants = new ArrayList<Participant>();
        for (String contact : contacts) {
            participants.add(new Participant(contact, null));
        }
        SimpleGroupChat groupChat = new SimpleGroupChat(this, subject, participants);
        groupChat.startGroup();
        Logger.d(TAG, "initGroupChat #groupChatId: " + groupChat.getChatId());
        if (groupChat.getChatId() == null)
            return null;
        mGroupChatMap.put(groupChat.getChatId(), groupChat);
        return groupChat.getChatId();
    }

    @Override
    public void acceptGroupChat(String chatId) throws RemoteException {
        Logger.d(TAG, "acceptGroupChat #chatId: " + chatId);
        SimpleGroupChat groupChat = getOrCreateGroupChat(chatId);
        groupChat.invitationAccepted();
    }

    @Override
    public void rejectGroupChat(String chatId) throws RemoteException {
        Logger.d(TAG, "rejectGroupChat #chatId: " + chatId);
        SimpleGroupChat groupChat = getGroupChat(chatId);
        if (groupChat == null) {
            getListener().onRejectInvitationResult(chatId, false);
            return;
        }
        groupChat.invitationRejected();
    }

    @Override
    public List<String> getGroupParticipants(String chatId) throws RemoteException {
        Logger.d(TAG, "getGroupParticipants #chatId: " + chatId);
        SimpleGroupChat groupChat = getOrCreateGroupChat(chatId);
        return groupChat.getParticipants();
    }

    @Override
    public void sendGroupMessage(String chatId, String content) throws RemoteException {
        // if (!getRegistrationStatus())
        // return false;
        Logger.d(TAG, "sendGroupMessage #chatId: " + chatId + ", #content: " + content);
        SimpleGroupChat groupChat = getOrCreateGroupChat(chatId);
        long msgIdInSMS = RCSDataBaseUtils.saveMsgToSmsDB(chatId, content, false);
        groupChat.sendChatMessage(msgIdInSMS, content);
    }

    @Override
    public void resendGroupMessage(String chatId, long msgIndex) throws RemoteException {
        Logger.d(TAG, "resendGroupMessage #chatId: " + chatId + ", #msgIndex: " + msgIndex);
        SimpleGroupChat groupChat = getOrCreateGroupChat(chatId);
        long ipMsgId = RCSDataBaseUtils.findIpMsgIdInSmsDb(msgIndex);
        if (ipMsgId == Integer.MAX_VALUE) {
            String body = RCSDataBaseUtils.findIpMsgTextInSmsDb(msgIndex);
            groupChat.sendChatMessage(msgIndex, body);
        } else {
            String msgId = RCSDataBaseUtils.findMsgIdInRcsDb(ipMsgId);
            groupChat.resendChatMessage(msgId);
        }

        Uri uri = ContentUris.withAppendedId(Sms.CONTENT_URI, msgIndex);
        RCSDataBaseUtils.updateSmsBoxType(uri, Sms.MESSAGE_TYPE_OUTBOX);
    }

    @Override
    public void addParticipants(String chatId, List<Participant> participants)
            throws RemoteException {
        Logger.d(TAG, "addParticipants #chatId: " + chatId + ", #participants: " + participants);
        SimpleGroupChat groupChat = getOrCreateGroupChat(chatId);
        groupChat.getGroupConfigHandler()
                .obtainMessage(ISimpleGroupChat.OP_ADD_PARTICIPANT, participants).sendToTarget();
    }

    @Override
    public void removeParticipants(String chatId, List<Participant> participants)
            throws RemoteException {
        Logger.d(TAG, "removeParticipants #chatId: " + chatId + ", #participants: " + participants);
        SimpleGroupChat groupChat = getOrCreateGroupChat(chatId);
        groupChat.getGroupConfigHandler()
                .obtainMessage(ISimpleGroupChat.OP_REMOVE_PARTICIPANT, participants).sendToTarget();
    }

    @Override
    public void modifySubject(String chatId, String subject) throws RemoteException {
        Logger.d(TAG, "modifySubject #chatId: " + chatId + ", #subject: " + subject);
        SimpleGroupChat groupChat = getOrCreateGroupChat(chatId);
        groupChat.getGroupConfigHandler()
                .obtainMessage(ISimpleGroupChat.OP_MODIFY_SUBJECT, subject).sendToTarget();
    }

    @Override
    public void modifyNickName(String chatId, String nickName) throws RemoteException {
        Logger.d(TAG, "modifyNickName #chatId: " + chatId + ", #nickName: " + nickName);
        // need change to local nick name, just modify the database.
        RCSDataBaseUtils.modifyGroupNickName(chatId, nickName);
    }

    @Override
    public void modifyRemoteAlias(String chatId, String alias) throws RemoteException {
        Logger.d(TAG, "modifyRemoteAlias #chatId: " + chatId + ", #alias: " + alias);
        SimpleGroupChat groupChat = getOrCreateGroupChat(chatId);
        groupChat.getGroupConfigHandler()
                .obtainMessage(ISimpleGroupChat.OP_MODIFY_MYNICKNAME, alias).sendToTarget();
    }

    @Override
    public void transferChairman(String chatId, String contact) throws RemoteException {
        Logger.d(TAG, "transferChairman #chatId: " + chatId + ", #contact: " + contact);
        SimpleGroupChat groupChat = getOrCreateGroupChat(chatId);
        Participant participant = new Participant(contact, null);
        groupChat.getGroupConfigHandler()
                .obtainMessage(ISimpleGroupChat.OP_TRANSFER_CHAIRMAN, participant).sendToTarget();
    }

    @Override
    public void quit(String chatId) throws RemoteException {
        Logger.d(TAG, "quit #chatId: " + chatId);
        SimpleGroupChat groupChat = getOrCreateGroupChat(chatId);
        groupChat.getGroupConfigHandler().obtainMessage(ISimpleGroupChat.OP_QUIT_GROUP)
                .sendToTarget();
    }

    @Override
    public void abort(String chatId) throws RemoteException {
        Logger.d(TAG, "abort #chatId: " + chatId);
        SimpleGroupChat groupChat = getOrCreateGroupChat(chatId);
        groupChat.getGroupConfigHandler().obtainMessage(ISimpleGroupChat.OP_ABORT_GROUP)
                .sendToTarget();
    }

    @Override
    public void addRCSChatServiceListener(IRCSChatServiceListener listener) throws RemoteException {
        mListener.addListener(listener);
        IBinder binder = listener.asBinder();
        binder.linkToDeath(mListener, 0);
    }

    @Override
    public void removeRCSChatServiceListener(IRCSChatServiceListener listener)
            throws RemoteException {
        mListener.removeListener(listener);
    }

    private class RCSChatManagerReceiver extends BroadcastReceiver {
        public final String TAG = this.toString().substring(this.toString().lastIndexOf('.') + 1);

        @Override
        public void onReceive(final Context context, final Intent intent) {
            // Logger.v(TAG, "onReceive() action: " + intent.getAction());
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    asyncOnReceive(context, intent);
                    return null;
                }
            }.execute();
        }

        private void asyncOnReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Logger.v(TAG, "asyncOnReceive() entry, the action is " + action);
            if (ChatIntent.ACTION_NEW_CHAT.equalsIgnoreCase(action)) {
                handleO2OInvitation(intent);
            } else if (GroupChatIntent.ACTION_NEW_INVITATION.equalsIgnoreCase(action)) {
                handleGroupInvitation(intent);
            } else if (ChatIntent.ACTION_DELIVERY_STATUS.equalsIgnoreCase(action)) {
                handleMessageDeliveryStatus(intent);
            } else if (FileTransferIntent.ACTION_NEW_INVITATION.equalsIgnoreCase(action)) {
                // add by feng
                boolean isGroupChat = intent.getBooleanExtra("isGroupTransfer", false);
                Log.v(TAG, "asyncOnReceive() entry, is Group" + isGroupChat);
                if (isGroupChat) {
                    handleGroupFTInvitation(intent);
                } else {
                    mFTManager.handleRecevieFileTransferInvitation(intent);
                }
            } else if (FileTransferIntent.ACTION_DELIVERY_STATUS.equalsIgnoreCase(action)){
                mFTManager.handleFileTransferDeliveryStatus(intent);
            }
        }
    }

    private class WorkHandler extends Handler {
        static final String TAG = "WorkHandler";

        public WorkHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            Logger.d(TAG, "handleMessage():" + msg.what);
            switch (msg.what) {
                case BaseChatImpl.BASE_OP_RESEND_MESSAGE:
                case BaseChatImpl.BASE_OP_SEND_MESSAGE_RST:
                    mPendingMsgManager.handlePendingMessage(msg);
                    break;
            }
        }
    }

    @Override
    public void sendEmoticonShopMessage(String contact, String content) throws RemoteException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void sendO2MEmoticonShopMessage(List<String> contacts, String content)
            throws RemoteException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void sendGroupEmotionShopMessage(String chatId, String content) throws RemoteException {
        // TODO Auto-generated method stub
        
    }

}
