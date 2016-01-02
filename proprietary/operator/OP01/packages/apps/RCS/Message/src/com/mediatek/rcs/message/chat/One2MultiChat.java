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

package com.mediatek.rcs.message.chat;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.chat.Chat;
import org.gsma.joyn.chat.ChatListener;
import org.gsma.joyn.chat.ChatLog;
import org.gsma.joyn.chat.ChatMessage;
import org.gsma.joyn.chat.ChatService;
import org.gsma.joyn.chat.ConferenceUser;
import org.gsma.joyn.chat.GeolocMessage;
import org.gsma.joyn.chat.GroupChat;
import org.gsma.joyn.chat.GroupChatListener;

import com.mediatek.rcs.common.provider.RCSDataBaseUtils;
import com.mediatek.rcs.common.utils.Logger;

/**
 * This class is the implementation of a 1-2-1 chat model
 */

public class One2MultiChat extends BaseChatImpl {
    public final String TAG = this.toString().substring(this.toString().lastIndexOf('.') + 1);;

    private List<String> mContacts;
    private ChatListener mPagerModeListener = null;
    private GroupChatListener mLargerModeListener = null;

    public One2MultiChat(RCSChatServiceImpl service, Object tag, List<String> contacts) {
        super(service, tag);
        mContacts = contacts;
    }

    public void sendChatMessage(final long smsId, final String content) {
        Runnable worker = new Runnable() {
            @Override
            public void run() {
                // save sms db;
//                long smsId = RCSDataBaseUtils.saveMsgToSmsDBForOne2Multi(mContacts, content);
                if (!GsmaManager.isServiceAvailable()) {
                    notifyMessageSendFail(smsId);
                    return;
                }
                final ChatMessage message = sendMessage(content);
                // combine sms db with stack db
                RCSDataBaseUtils.combineMsgId(smsId, message.getId());
            }
        };
        Logger.w(TAG, "sendChatMessage() post to worker thread");
        mWorkHandler.post(worker);
    }

    public ChatMessage sendMessage(String content) {
        Chat o2oChatImpl = null;
        String messageId = null;
        ChatService chatService = null;
        if (GsmaManager.isServiceAvailable()) {
            try {
                Logger.d(TAG, "sendMessage() to stack. remotes: " + mContacts);
                chatService = mGsmaManager.getChatApi();
                Set<String> contacts = new HashSet<String>();
                for (String contact : mContacts) {
                    contacts.add(contact);
                }
                int byteLength = content.getBytes("UTF-8").length;
                if (byteLength > MAX_PAGER_MODE_MSG_LENGTH) {
                    messageId = chatService.sendOne2MultiMessageLargeMode(content, contacts,
                            new One2MultiChatListenerLargerMode());
                } else {
                    o2oChatImpl = chatService.openSingleChat(mContacts.get(0),
                            new One2MultiChatListenerPagerMode());
                    messageId = o2oChatImpl.sendOnetoMultiMessageByPagerMode(content,
                            (HashSet<String>) contacts);
                }

                Logger.d(TAG, "sendMessage() messageId: " + messageId);
                ChatMessage chatMessage = new ChatMessage(messageId, null, content, new Date(),
                        true, null);
                return chatMessage;
            } catch (JoynServiceException e1) {
                e1.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return new ChatMessage(null, null, content, new Date(), true, null);
    }

    public void onReceiveMessageDeliveryStatus(final String msgId, final String status) {
        // status: ImdnDocument.java "sent" "delivered" "displayed" "failed" "error"
        // "display_burned"
        Logger.d(TAG, "onReceiveMessageDeliveryStatus() msgId: " + msgId + ", status: " + status);
        if (status.equalsIgnoreCase("sent")) {
            RCSDataBaseUtils.updateMessageSent(msgId);
        } else if (status.equalsIgnoreCase("delivered")) {
            RCSDataBaseUtils.updateMessageDelivered(msgId);
        } else if (status.equalsIgnoreCase("failed")) {
            // need check;
            long ipMsgId = RCSDataBaseUtils.findTextIdInRcseDb(mService.getContext()
                    .getContentResolver(), msgId, ChatLog.Message.Direction.OUTGOING);
            long smsId = RCSDataBaseUtils.getIdInMmsDb(mService.getContext().getContentResolver(),
                    ipMsgId);
            notifyMessageSendFail(smsId);
        } else {
            // do nothing.
        }
    }

    public void notifyMessageSendFail(final long smsId) {
        mService.getListener().onSendO2MMessageFailed(smsId);
    }

    private class One2MultiChatListenerPagerMode extends ChatListener {
        private final String TAG = this.toString()
                .substring(this.toString().indexOf("One2MultiChatListenerPagerMode"),
                        this.toString().length());

        public One2MultiChatListenerPagerMode() {
        }

        /**
         * Callback called when a new message has been received
         * 
         * @param message
         *            Chat message
         * @see ChatMessage
         */
        public void onNewMessage(final ChatMessage message) {
            Logger.d(TAG, "onNewMessage()   message id:" + message.getId() + " ,message text:"
                    + message.getMessage());
        }

        /**
         * Callback called when a new geoloc has been received
         * 
         * @param message
         *            Geoloc message
         * @see GeolocMessage
         */
        public void onNewGeoloc(GeolocMessage message) {
            // not use in CMCC RCS, the geoloc message for CMCC is just file transfer
            // encode&decode file at app.
        }

        /**
         * Callback called when a message has been delivered to the remote
         * 
         * @param msgId
         *            Message ID
         */
        public void onReportMessageDelivered(final String msgId) {
            Logger.d(TAG, "onReportMessageDelivered()  msgId:" + msgId);
        }

        /**
         * Callback called when a message has been displayed by the remote
         * 
         * @param msgId
         *            Message ID
         */
        public void onReportMessageDisplayed(final String msgId) {
            Logger.d(TAG, "onReportMessageDisplayed()  msgId:" + msgId);
        }

        /**
         * Callback called when a message has failed to be delivered to the remote
         * 
         * @param msgId
         *            Message ID
         */
        public void onReportMessageFailed(final String msgId) {
            Logger.d(TAG, "onReportMessageFailed()  msgId:" + msgId);
            onReceiveMessageDeliveryStatus(msgId, "failed");            
        }

        /**
         * Callback called when a message has been sent to the server
         * 
         * @CMCC RCS spec
         * 
         * @param msgId
         *            Message ID
         */

        public void onReportSentMessage(final String msgId) {
            Logger.d(TAG, "onReportSentMessage()  msgId:" + msgId);
            onReceiveMessageDeliveryStatus(msgId, "sent");
        }

        /**
         * Callback called when a message has been delivered to the remote contact.
         * 
         * @CMCC RCS spec
         * 
         * @param msgId
         *            Message ID
         */

        public void onReportDeliveredMessage(final String msgId) {
            Logger.d(TAG, "onReportDeliveredMessage()  msgId:" + msgId);
            onReceiveMessageDeliveryStatus(msgId, "delivered");
        }

        /**
         * Callback called when a new burn message arrived.
         * 
         * @CMCC RCS spec
         * 
         * @param msgId
         *            Message ID
         */
        public void onNewBurnMessageArrived(final ChatMessage msg) {
            Logger.d(TAG, "onNewBurnMessageArrived()   message id:" + msg.getId()
                    + " message text:" + msg.getMessage());
        }

        /**
         * Callback called when a message send unsuccessfully.
         * 
         * @CMCC RCS spec
         * 
         * @param msgId
         *            Message ID
         */

        public void onReportFailedMessage(final String msgId, int errtype, String statusCode) {
            Logger.d(TAG, "onReportFailedMessage()  msgId:" + msgId + " ,errorType: " + errtype
                    + " ,statusCode: " + statusCode);
            //onReceiveMessageDeliveryStatus(msgId, "failed");
        }

        /**
         * Callback called when an Is-composing event has been received. If the remote is typing a
         * message the status is set to true, else it is false.
         * 
         * @param status
         *            Is-composing status
         */
        public void onComposingEvent(final boolean status) {
            Logger.d(TAG, "onComposingEvent()  session: the status is " + status);
        }
    }

    private class One2MultiChatListenerLargerMode extends GroupChatListener {
        public final String TAG = this.toString().substring(
                this.toString().indexOf("One2MultiChatListenerLargerMode"),
                this.toString().length());

        public One2MultiChatListenerLargerMode() {
            Logger.d(TAG, "Constructor() entry");
            Logger.d(TAG, "Constructor() exit");
        }

        /**
         * Callback called when the session is well established and messages may be exchanged with
         * the group of participants
         */
        public void onSessionStarted() {
            Logger.d(TAG, "onSessionStarted() entry");
        }

        /**
         * Callback called when the session has been aborted or terminated
         */
        public void onSessionAborted() {
            Logger.d(TAG, "onSessionAborted() entry");
        }

        /**
         * Callback called when the session has failed
         * 
         * @param error
         *            Error
         * @see GroupChat.Error
         */
        public void onSessionError(int error) {
            Logger.d(TAG, "onSessionError() entry, error: " + error);
        }

        /**
         * Callback called when a new message has been received
         * 
         * @param message
         *            New chat message
         * @see ChatMessage
         */
        public void onNewMessage(ChatMessage message) {
            Logger.d(TAG, "onNewMessage() entry DISPLAY: " + message.getContact() + " content: "
                    + message.getMessage());

        }

        /**
         * Callback called when a new geoloc has been received
         * 
         * @param message
         *            Geoloc message
         * @see GeolocMessage
         */
        public void onNewGeoloc(GeolocMessage message) {
            Logger.d(TAG, "onNewGeoloc() entry");
        }

        /**
         * Callback called when a message has been delivered to the remote
         * 
         * @param msgId
         *            Message ID
         */
        public void onReportMessageDeliveredContact(String msgId, String contact) {
            Logger.d(TAG, "onReportMessageDelivered() entry, msgId: " + msgId + "contact: "
                    + contact);
        }

        /**
         * Callback called when a message has been displayed by the remote
         * 
         * @param msgId
         *            Message ID
         */
        public void onReportMessageDisplayedContact(String msgId, String contact) {
            Logger.d(TAG, "onReportMessageDisplayedContact() entry, msgId: " + msgId + "contact: "
                    + contact);
        }

        /**
         * Callback called when a message has failed to be delivered to the remote
         * 
         * @param msgId
         *            Message ID
         */
        public void onReportMessageFailedContact(String msgId, String contact) {
            Logger.d(TAG, "onReportMessageFailedContact() entry, msgId: " + msgId + "contact: "
                    + contact);
            onReceiveMessageDeliveryStatus(msgId, "failed");            
        }

        /**
         * Callback called when a message has been delivered to the remote
         * 
         * @param msgId
         *            Message ID
         */
        public void onReportMessageDelivered(String msgId) {
            Logger.d(TAG, "onReportMessageDelivered() entry, msgId: " + msgId);
        }

        /**
         * Callback called when a message has been displayed by the remote
         * 
         * @param msgId
         *            Message ID
         */
        public void onReportMessageDisplayed(String msgId) {
            Logger.d(TAG, "onReportMessageDisplayed() entry, msgId: " + msgId);
        }

        /**
         * Callback called when a message has failed to be delivered to the remote
         * 
         * @param msgId
         *            Message ID
         */
        public void onReportMessageFailed(String msgId) {
            Logger.d(TAG, "onReportMessageFailed() entry, msgId: " + msgId);
        }

        /**
         * Callback called when a message has failed to be delivered to the remote
         * 
         * @param msgId
         *            Message ID
         */
        public void onReportFailedMessage(final String msgId, int errtype, String statusCode) {
            Logger.d(TAG, "onReportFailedMessage() entry, msgId: " + msgId + " errtype: " + errtype
                    + " statusCode: " + statusCode);
            onReceiveMessageDeliveryStatus(msgId, "failed");
            return;
        }

        /**
         * Callback called when a message has been sent to remote
         * 
         * @param msgId
         *            Message ID
         */
        public void onReportSentMessage(final String msgId) {
            Logger.d(TAG, "onReportSentMessage() entry, msgId: " + msgId);
            onReceiveMessageDeliveryStatus(msgId, "sent");
        }

        /**
         * Callback called when a group chat is dissolved
         * 
         */
        public void onGroupChatDissolved() {
            // base on the spec SIP Bye reason 410
            // means participants count is less than the MIN number
            // temp solution as KICKED OUT.
            Logger.d(TAG, "onGroupChatDissolved() entry!");
        }

        /**
         * Callback called to inform the result of invite participants
         * 
         */
        public void onInviteParticipantsResult(int errType, String statusCode) {
            Logger.d(TAG, "onInviteParticipantsResult() entry, errType: " + errType
                    + " statusCode: " + statusCode);
        }

        /**
         * Callback called when an Is-composing event has been received. If the remote is typing a
         * message the status is set to true, else it is false.
         * 
         * @param contact
         *            Contact
         * @param status
         *            Is-composing status
         */
        public void onComposingEvent(String contact, boolean status) {
            Logger.d(TAG, "onComposingEvent() contact: " + contact + " status: " + status);
        }

        /**
         * Callback called when a new participant has joined the group chat
         * 
         * @param contact
         *            Contact
         * @param contactDisplayname
         *            Contact displayname
         */
        public void onParticipantJoined(String contact, String contactDisplayname) {
            Logger.d(TAG, "onParticipantJoined() contact:" + contact + " contactDisplayname: "
                    + contactDisplayname);
        }

        /**
         * Callback called when a participant has left voluntary the group chat
         * 
         * @param contact
         *            Contact
         */
        public void onParticipantLeft(String contact) {
            Logger.d(TAG, "onParticipantLeft() contact: " + contact);
        }

        /**
         * Callback called when a participant is disconnected from the group chat
         * 
         * @param contact
         *            Contact
         */
        public void onParticipantDisconnected(String contact) {
            Logger.d(TAG, "onParticipantDisconnected() contact: " + contact);
        }

        /**
         * Callback called when new chairman is successfully changed by current chairman (Callback
         * only received by chairman)
         * 
         * @param errType
         *            errorType
         */
        public void onSetChairmanResult(int errType, int statusCode) {
            Logger.d(TAG, "onSetChairmanResult() errType:" + errType + " statusCode: " + statusCode);
        }

        /**
         * Callback called when chairman is changed by current chairman (Callback received by every
         * user of group)
         * 
         * @param newChairman
         *            new chairman
         */
        public void onChairmanChanged(String newChairman) {
            Logger.d(TAG, "onChairmanChanged() newSubject:" + newChairman);
        }

        /**
         * Callback called when subject is modified (Callback only received by chairman)
         * 
         * @param errType
         *            errorType
         */
        public void onModifySubjectResult(int errType, int statusCode) {
            Logger.d(TAG, "onModifySubjectResult() errType:" + errType + " statusCode: "
                    + statusCode);
        }

        public void onModifyMyNickNameResult(int errType, int statusCode) {
            Logger.d(TAG, "onModifyMyNickNameResult() errType:" + errType + " statusCode: "
                    + statusCode);
        }

        /**
         * Callback called when subject is changed (Callback received by every user of group)
         * 
         * @param newSubject
         *            new subject
         */
        public void onSubjectChanged(String newSubject) {
            Logger.d(TAG, "onSubjectChanged() newSubject:" + newSubject);
        }

        /**
         * Callback called when participants are removed (Callback only received by chairman)
         * 
         * @param errType
         *            errorType
         * @param statusCode
         *            status Code
         */
        public void onRemoveParticipantResult(int errType, int statusCode, String participant) {
            Logger.d(TAG, "onInviteParticipantsResult() entry, errType: " + errType
                    + " statusCode: " + statusCode + " participant: " + participant);
        }

        /**
         * Callback called participant is kicked out(removed) by chairman (Callback received by
         * removed participant)
         * 
         * @param from
         *            who kicked out
         */
        public void onReportMeKickedOut(String from) {
            Logger.d(TAG, "onReportMeKickedOut() from:" + from);
        }

        /**
         * Callback called chairman has successfully aborted the group (Callback only received by
         * chairman)
         * 
         * @param errType
         *            errorType
         */
        public void onAbortConversationResult(int errType, int statusCode) {
            Logger.d(TAG, "onAbortConversationResult() errType:" + errType + " statusCode: "
                    + statusCode);
        }

        /**
         * Callback called user has left the group successfully (Callback received by user who left
         * the group)
         * 
         * @param errType
         *            errorType
         */
        public void onQuitConversationResult(int errType, int statusCode) {
            Logger.d(TAG, "onQuitConversationResult() errType:" + errType + " statusCode: "
                    + statusCode);
        }

        @Override
        public void onReportParticipantKickedOut(String contact) {
        }

        @Override
        public void onModifyNickNameResult(int arg0, int arg1) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onNickNameChanged(String arg0, String arg1) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onSessionAbortedbyChairman() {
            // TODO Auto-generated method stub

        }

        @Override
        public void onConferenceNotify(String arg0, List<ConferenceUser> arg1) {
            // TODO Auto-generated method stub
            
        }
    }
}
