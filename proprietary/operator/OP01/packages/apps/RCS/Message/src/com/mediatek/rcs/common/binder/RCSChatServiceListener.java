package com.mediatek.rcs.common.binder;

import java.util.List;

import org.gsma.joyn.chat.ChatLog;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;

import com.mediatek.rcs.common.GroupManager;
import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.common.RCSMessageManager;
import com.mediatek.rcs.common.binder.RCSServiceManager.GroupInfo;
import com.mediatek.rcs.common.provider.GroupMemberData;
import com.mediatek.rcs.common.provider.RCSDataBaseUtils;
import com.mediatek.rcs.common.provider.ThreadMapCache;
import com.mediatek.rcs.common.provider.ThreadMapCache.MapInfo;
import com.mediatek.rcs.common.service.IRCSChatService;
import com.mediatek.rcs.common.service.IRCSChatServiceListener;
import com.mediatek.rcs.common.service.Participant;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.common.utils.Logger;
import com.mediatek.rcs.common.utils.RCSUtils;
import com.mediatek.rcs.common.MessageStatusUtils.IFileTransfer.Status;

import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import com.mediatek.rcs.common.RCSCacheManager;

/**
 * RCSChatServiceListener. Register this listener to service, listen message delivery.
 *
 */
public class RCSChatServiceListener extends IRCSChatServiceListener.Stub {

    private final String INVITATION_PREFERENCE = "invitation_preference";

    /**
     * RCSChatServiceListener Construction.
     */
    public RCSChatServiceListener() {
    }

    private static final String TAG = "RCSChatServiceListener";

    @Override
    public void onNewMessage(long msgId) throws RemoteException {
        Logger.d(TAG, "onNewMessage, msgId=" + msgId);
        long threadId = 0;
        long ipmsgId = 0;
        String number = null;
        Cursor cursor = getMessageInfo(msgId);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                threadId = cursor.getLong(cursor.getColumnIndex(Sms.THREAD_ID));
                ipmsgId = cursor.getLong(cursor.getColumnIndex(Sms.IPMSG_ID));
                number = cursor.getString(cursor.getColumnIndex(Sms.ADDRESS));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Logger.d(TAG, "notifyNewMessage, entry: threadId=" + threadId + ", ipmsgId=" + ipmsgId);
        notifyNewMessage(threadId, ipmsgId, number, msgId);
    }

    @Override
    public void onNewGroupMessage(String chatId, long msgId, String number) throws RemoteException {
        Logger.d(TAG, "onNewGroupMessage, chatId=" + chatId);
        
        MapInfo info = ThreadMapCache.getInstance().getInfoByChatId(chatId);
        long threadId = ensureGroupThreadId(chatId);
        if (info != null) {
            int subId = RCSUtils.getRCSSubId();
            if (info.getSubId() != subId) {
                ThreadMapCache.getInstance().updateSubId(chatId, subId);
                ThreadMapCache.getInstance().updateStatusByChatId(chatId, subId);
            }
        }
        long ipmsgId = 0;
        Cursor cursor = getMessageInfo(msgId);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                ipmsgId = cursor.getLong(cursor.getColumnIndex(Sms.IPMSG_ID));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (threadId <= 0) {
            Logger.e(TAG, "onNewGroupMessage, no such thread, delete it, msgId=" + msgId);
            RCSMessageManager.getInstance(ContextCacher.getHostContext()).deleteIpMsg(ipmsgId);
            return;
        }
        ContentResolver resolver = ContextCacher.getHostContext().getContentResolver();
        ContentValues values = new ContentValues();
        values.put(Sms.THREAD_ID, threadId);
        Uri uri = ContentUris.withAppendedId(Sms.CONTENT_URI, msgId);
        resolver.update(uri, values, null, null);
        Logger.d(TAG, "notifyNewMessage, entry: threadId=" + threadId + ", ipmsgId=" + ipmsgId);
        notifyNewMessage(threadId, ipmsgId, number, msgId);
    }

    private void notifyNewMessage(long threadId, long ipmsgId, String number, long smsId) {
        Intent intent = new Intent();
        intent.setAction(IpMessageConsts.MessageAction.ACTION_NEW_MESSAGE);
        intent.putExtra(IpMessageConsts.MessageAction.KEY_THREAD_ID, threadId);
        intent.putExtra(IpMessageConsts.MessageAction.KEY_IPMSG_ID, ipmsgId);
        intent.putExtra(IpMessageConsts.MessageAction.KEY_NUMBER, number);
        intent.putExtra(IpMessageConsts.MessageAction.KEY_SMS_ID, smsId);
        notifyAllListeners(intent);

    }

    private Cursor getMessageInfo(long smsId) {
        Uri uri = ContentUris.withAppendedId(Sms.CONTENT_URI, smsId);
        ContentResolver resolver = ContextCacher.getHostContext().getContentResolver();
        String[] projection = { Sms.THREAD_ID, Sms.ADDRESS, Sms.BODY, Sms.IPMSG_ID, Sms.PROTOCOL };
        return resolver.query(uri, projection, null, null, null);
    }

    private boolean getRequestDeliveryReport(long subId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ContextCacher
                .getHostContext());
        return prefs.getBoolean(subId + "_" + RCSUtils.SMS_DELIVERY_REPORT_MODE,
                RCSUtils.DEFAULT_DELIVERY_REPORT_MODE);
    }

    @Override
    public void onSendO2OMessageFailed(long msgId) throws RemoteException {
        // TODO:
        boolean sendBySms = true;
        Uri uri = ContentUris.withAppendedId(Sms.CONTENT_URI, msgId);
        if (!sendBySms) {
            RCSUtils.updateSmsBoxType(uri, Sms.MESSAGE_TYPE_FAILED);
            return;
        }
        boolean requestDeliveryReport = getRequestDeliveryReport(RCSUtils.getRCSSubId());
        Logger.d(TAG, "SMS DR request=" + requestDeliveryReport);
        long ipmsgId = 0;
        String body = null;
        int protocol = 0;
        Cursor cursor = getMessageInfo(msgId);
        try {
            if (cursor == null || cursor.getCount() == 0) {
                Logger.d(TAG, "[onSendO2OMessageFailed]: don't find the message msgId = " + msgId);
                return;
            }
            if (cursor != null && cursor.moveToFirst()) {
                ipmsgId = cursor.getLong(cursor.getColumnIndex(Sms.IPMSG_ID));
                body = cursor.getString(cursor.getColumnIndex(Sms.BODY));
                protocol = cursor.getInt(cursor.getColumnIndex(Sms.PROTOCOL));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (body.length() > RCSUtils.MAX_LENGTH_OF_TEXT_MESSAGE
                || protocol == IpMessageConsts.MessageProtocolType.RCS_BURN_PROTO) {
            RCSUtils.updateSmsBoxType(uri, Sms.MESSAGE_TYPE_FAILED);
            return;
        }
        ContentValues cv = new ContentValues();
        cv.put(Sms.TYPE, Sms.MESSAGE_TYPE_QUEUED);
        cv.put(Sms.IPMSG_ID, 0);
        if (requestDeliveryReport) {
            cv.put(Sms.STATUS, Sms.STATUS_PENDING);
        }
        ContentResolver resolver = ContextCacher.getHostContext().getContentResolver();
        resolver.update(uri, cv, null, null);

        Intent intent = new Intent(RCSUtils.ACTION_SEND_MESSAGE);
        intent.setClassName("com.android.mms", "com.android.mms.transaction.SmsReceiver");
        ContextCacher.getPluginContext().sendBroadcast(intent);
        RCSMessageManager.getInstance(ContextCacher.getPluginContext()).deleteStackMessage(ipmsgId);
    }

    @Override
    public void onSendO2MMessageFailed(long msgId) throws RemoteException {
        // TODO:
        boolean sendBySms = true;
        Uri uri = ContentUris.withAppendedId(Sms.CONTENT_URI, msgId);
        if (!sendBySms) {
            RCSUtils.updateSmsBoxType(uri, Sms.MESSAGE_TYPE_FAILED);
            return;
        }

        boolean requestDeliveryReport = getRequestDeliveryReport(RCSUtils.getRCSSubId());
        Logger.d(TAG, "SMS DR request=" + requestDeliveryReport);
        String messageContent = null;
        long threadId = 0;
        String recipients = null;
        long ipmsgId = 0;
        Cursor cursor = getMessageInfo(msgId);
        try {
            if (cursor == null || cursor.getCount() == 0) {
                Logger.d(TAG, "[onSendO2MMessageFailed]: don't find the message msgId = " + msgId);
                return;
            }
            if (cursor != null && cursor.moveToFirst()) {
                messageContent = cursor.getString(cursor.getColumnIndex(Sms.BODY));
                threadId = cursor.getLong(cursor.getColumnIndex(Sms.THREAD_ID));
                recipients = cursor.getString(cursor.getColumnIndex(Sms.ADDRESS));
                ipmsgId = cursor.getLong(cursor.getColumnIndex(Sms.IPMSG_ID));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (messageContent.length() > RCSUtils.MAX_LENGTH_OF_TEXT_MESSAGE) {
            RCSUtils.updateSmsBoxType(uri, Sms.MESSAGE_TYPE_FAILED);
            return;
        }
        ContentResolver resolver = ContextCacher.getHostContext().getContentResolver();
        String[] contacts = recipients.split(RCSMessageManager.COMMA);
        for (int i = 0; i < contacts.length; i++) {
            Sms.addMessageToUri(RCSUtils.getRCSSubId(),
                    resolver,
                    Uri.parse("content://sms/queued"), contacts[i],
                    messageContent, null, System.currentTimeMillis(),
                    true /* read */,
                    requestDeliveryReport,
                    threadId);
        }
        // delete message in mms db
        resolver.delete(uri, null, null);
        // send broadcast to notify SmsReceiverService
        Intent intent = new Intent();
        intent.setAction(RCSUtils.ACTION_SEND_MESSAGE);
        intent.setClassName("com.android.mms", "com.android.mms.transaction.SmsReceiver");
        ContextCacher.getPluginContext().sendBroadcast(intent);
        RCSMessageManager.getInstance(ContextCacher.getPluginContext()).deleteStackMessage(ipmsgId);
    }

    @Override
    public void onSendGroupMessageFailed(long msgId) throws RemoteException {
        Logger.d(TAG, "sendGroupMessageFailed, msgId=" + msgId);
        if (msgId < 0) {
            return;
        }
        Uri uri = ContentUris.withAppendedId(Sms.CONTENT_URI, msgId);
        RCSUtils.updateSmsBoxType(uri, Sms.MESSAGE_TYPE_FAILED);
    }

    @Override
    public void onParticipantJoined(String chatId, Participant participant) throws RemoteException {
        Logger.d(TAG, "onParticipantJoined, chatId=" + chatId + ", " + participant.getContact());
        updateGroupAddress(chatId);
        notifyParticipantOperation(chatId, IpMessageConsts.GroupActionList.VALUE_PARTICIPANT_JOIN,
                participant);
    }

    @Override
    public void onParticipantLeft(String chatId, Participant participant) throws RemoteException {
        Logger.d(TAG, "onParticipantLeft, chatId=" + chatId + ", " + participant.getContact());
        updateGroupAddress(chatId);
        notifyParticipantOperation(chatId, IpMessageConsts.GroupActionList.VALUE_PARTICIPANT_LEFT,
                participant);
    }

    @Override
    public void onParticipantRemoved(String chatId, Participant participant)
                                                                throws RemoteException {
        Logger.d(TAG, "onParticipantRemoved, chatId=" + chatId + ", " + participant.getContact());
        updateGroupAddress(chatId);
        notifyParticipantOperation(chatId,
                IpMessageConsts.GroupActionList.VALUE_PARTICIPANT_REMOVED, participant);
    }

    @Override
    public void onChairmenChanged(String chatId, Participant participant, boolean isMe)
            throws RemoteException {
        Logger.d(TAG, "onChairmenChanged, chatId=" + chatId + ", " + participant.getContact());
        ensureGroupThreadId(chatId);
        ThreadMapCache.getInstance().updateIsMeChairmenByChatId(chatId, isMe);
        notifyParticipantOperation(chatId, IpMessageConsts.GroupActionList.VALUE_CHAIRMEN_CHANGED,
                participant);
    }

    @Override
    public void onSubjectModified(String chatId, String subject) throws RemoteException {
        Logger.d(TAG, "onSubjectModified, chatId=" + chatId + ", subject=" + subject);
        ThreadMapCache.getInstance().updateSubjectByChatId(chatId, subject);
        ensureGroupThreadId(chatId);
        notifyStringOperation(chatId, IpMessageConsts.GroupActionList.VALUE_SUBJECT_MODIFIED,
                IpMessageConsts.GroupActionList.KEY_SUBJECT, subject);
    }

    @Override
    public void onParticipantNickNameModified(String chatId, String contact, String nickName)
            throws RemoteException {
        Logger.d(TAG, "onParticipantNickNameChanged,contact=" + contact + ", nickName=" + nickName);
        ensureGroupThreadId(chatId);
        Intent intent = getNotifyIntent(chatId,
                IpMessageConsts.GroupActionList.ACTION_GROUP_NOTIFY,
                IpMessageConsts.GroupActionList.VALUE_PARTICIPANT_NICKNAME_MODIFIED);
        intent.putExtra(IpMessageConsts.GroupActionList.KEY_CONTACT_NUMBER, contact);
        intent.putExtra(IpMessageConsts.GroupActionList.KEY_NICK_NAME, nickName);
        notifyAllListeners(intent);
    }

    @Override
    public void onMeRemoved(String chatId, String contact) throws RemoteException {
        Logger.d(TAG, "onMeRemoved, contact = " + contact);
        ensureGroupThreadId(chatId);
        markGroupUnavailable(chatId);
        notifyStringOperation(chatId, IpMessageConsts.GroupActionList.VALUE_ME_REMOVED,
                IpMessageConsts.GroupActionList.KEY_CONTACT_NUMBER, contact);
    }

    @Override
    public void onAbort(String chatId) throws RemoteException {
        Logger.d(TAG, "onAbort, chatId=" + chatId);
        ensureGroupThreadId(chatId);
        markGroupUnavailable(chatId);

        notifyNullParamOperation(chatId, IpMessageConsts.GroupActionList.VALUE_GROUP_ABORTED);
    }

    @Override
    public void onNewInvite(Participant participant, String subject, String chatId)
            throws RemoteException {
        // TODO
        Context context = ContextCacher.getHostContext();
        boolean rejected = getInvitationRejectedState(context, chatId);
        Logger.d(TAG, "onNewInvite, chatId =" + chatId + ", rejected = " + rejected);
        if (rejected) {
            GroupManager.getInstance(context).rejectGroupInvitation(chatId);
            return;
        }

        MapInfo info = ThreadMapCache.getInstance().getInfoByChatId(chatId);
        long threadId = 0;
        long status = 0;
        boolean addSysMsg = true;
        if (info != null) {
            threadId = info.getThreadId();
            status = info.getStatus();
            if (threadId == RCSUtils.DELETED_THREAD_ID) {
                threadId = createGroupThread(participant, true);
                ThreadMapCache.getInstance().updateThreadId(chatId, threadId);
                updateGroupStatus(chatId, IpMessageConsts.GroupActionList.GROUP_STATUS_INVITING,
                        false);
            } else {
                if (status < 0 && status != IpMessageConsts.GroupActionList.GROUP_STATUS_INVALID) {
                    addSysMsg = false;
                }
                updateGroupStatus(chatId,
                        IpMessageConsts.GroupActionList.GROUP_STATUS_INVITING_AGAIN, true);
            }
            if (info.getSubId() != RCSUtils.getRCSSubId()) {
                ThreadMapCache.getInstance().updateSubId(chatId, RCSUtils.getRCSSubId());
            }
            ThreadMapCache.getInstance().updateSubjectByChatId(chatId, subject);
        } else {
            threadId = createGroupThread(participant, true);
            ThreadMapCache.getInstance().addMapInfo(threadId, chatId, subject,
                    IpMessageConsts.GroupActionList.GROUP_STATUS_INVITING, false);
        }
        Logger.d(TAG, "onNewInvite, receive new invite, chatId=" + chatId + ", threadId="
                + threadId);

        String name = RCSDataBaseUtils.getContactDisplayName(ContextCacher.getPluginContext(),
                participant.getContact());
        Participant contact = new Participant(participant.getContact(), name);
        Intent intent = getNotifyIntent(chatId,
                IpMessageConsts.GroupActionList.ACTION_GROUP_NOTIFY,
                IpMessageConsts.GroupActionList.VALUE_NEW_INVITE_RECEIVED);
        intent.putExtra(IpMessageConsts.GroupActionList.KEY_PARTICIPANT, contact);
        intent.putExtra(IpMessageConsts.GroupActionList.KEY_SUBJECT, subject);
        intent.putExtra(IpMessageConsts.GroupActionList.KEY_ADD_SYS_EVENT, addSysMsg);
        notifyAllListeners(intent);
    }

    @Override
    public void onInvitationTimeout(String chatId) throws RemoteException {
        Logger.d(TAG, "onInvitationTimeout, chatId=" + chatId);
        // No need to update timeout status, UI should not care this, always can
        // reject/accept
        // markGroupInvitationTimeOut(chatId);
    }

    @Override
    public void onAddParticipantFail(Participant participant, String chatId)
                                                        throws RemoteException {
        Logger.d(TAG, "onAddParticipantFail, number=" + participant.getContact() + ", chatId="
                + chatId);
        notifyParticipantOperation(chatId,
                IpMessageConsts.GroupActionList.VALUE_ADD_PARTICIPANT_FAIL, participant);
    }

    @Override
    public void onAddParticipantsResult(String chatId, boolean result) throws RemoteException {
        Logger.d(TAG, "onAddParticipantsResult, result=" + result + ", chatId=" + chatId);
        notifyResultOperation(chatId, IpMessageConsts.GroupActionList.VALUE_ADD_PARTICIPANTS,
                result);
    }

    @Override
    public void onRemoveParticipantsResult(String chatId, boolean result) throws RemoteException {
        Logger.d(TAG, "onRemoveParticipantsResult, result=" + result + ", chatId=" + chatId);
        notifyResultOperation(chatId, IpMessageConsts.GroupActionList.VALUE_REMOVE_PARTICIPANTS,
                result);
    }

    @Override
    public void onTransferChairmenResult(String chatId, boolean result) throws RemoteException {
        Logger.d(TAG, "onTransferChairmenResult, result = " + result + ", chatId=" + chatId);
        ensureGroupThreadId(chatId);
        if (result) {
            ThreadMapCache.getInstance().updateIsMeChairmenByChatId(chatId, false);
        }
        notifyResultOperation(chatId, IpMessageConsts.GroupActionList.VALUE_TRANSFER_CHAIRMEN,
                result);
    }

    @Override
    public void onMyNickNameModifiedResult(String chatId, boolean result) throws RemoteException {
        Logger.d(TAG, "onMyNickNameModifiedResult, result = " + result + ", chatId=" + chatId);
        String myNickName = getMyNickName(chatId);

        notifyResultOperationWithStringArg(chatId,
                IpMessageConsts.GroupActionList.VALUE_MODIFY_SELF_NICK_NAME, result,
                IpMessageConsts.GroupActionList.KEY_SELF_NICK_NAME, myNickName);
    }

    @Override
    public void onSubjectModifiedResult(String chatId, boolean result) throws RemoteException {
        Logger.d(TAG, "onSubjectModifiedResult, result=" + result + ", chatId=" + chatId);
        ensureGroupThreadId(chatId);
        String subject = getSubject(chatId);
        ThreadMapCache.getInstance().updateSubjectByChatId(chatId, subject);
        notifyResultOperationWithStringArg(chatId,
                IpMessageConsts.GroupActionList.VALUE_MODIFY_SUBJECT, result,
                IpMessageConsts.GroupActionList.KEY_SUBJECT, subject);
    }

    @Override
    public void onQuitConversationResult(String chatId, boolean result) throws RemoteException {
        Logger.d(TAG, "onQuitConversationResult, result=" + result + ", chatId=" + chatId);
        if (result) {
            markGroupUnavailable(chatId);
        }
        notifyResultOperation(chatId, IpMessageConsts.GroupActionList.VALUE_EXIT_GROUP, result);
    }

    @Override
    public void onAbortResult(String chatId, boolean result) throws RemoteException {
        Logger.d(TAG, "onAbortResult, result=" + result + ", chatId=" + chatId);
        if (result) {
            markGroupUnavailable(chatId);
        }
        notifyResultOperation(chatId, IpMessageConsts.GroupActionList.VALUE_DESTROY_GROUP, result);
    }

    @Override
    public void onInitGroupResult(boolean result, String chatId) throws RemoteException {
        Logger.d(TAG, "onInitGroupResult, result=" + result + ", chatId=" + chatId);
        String subject = null;
        List<String> participants = null;
        GroupInfo groupInfo = RCSServiceManager.getInstance().getInvitingGroup(chatId);
        if (groupInfo != null) {
            subject = groupInfo.getSubject();
            participants = groupInfo.getParticipants();
        }
        if (result) {
            long threadId = createGroupThread(null, false);
            Logger.d(TAG, "onInitGroupResult, threadId=" + threadId + ", chatId=" + chatId);
            if (threadId > 0) {
                ThreadMapCache.getInstance().addMapInfo(threadId, chatId, subject,
                        RCSUtils.getRCSSubId(), true);
            }
        }
        if (!TextUtils.isEmpty(chatId)) {
            RCSServiceManager.getInstance().removeInvitingGroup(chatId);
        }
        notifyInitGroupResult(chatId, IpMessageConsts.GroupActionList.VALUE_INIT_GROUP, result,
                participants);
    }

    @Override
    public void onRequestBurnMessageCapabilityResult(String contact, boolean result)
            throws RemoteException {
        Log.d(TAG, "onRequestBurnMessageCapabilityResult contact = " + contact + " result = "
                + result);
        RCSServiceManager.getInstance().callBurnCapListener(contact, result);
    }

    @Override
    public void onAcceptInvitationResult(String chatId, boolean result) throws RemoteException {
        if (result) {
            updateGroupStatus(chatId, RCSUtils.getRCSSubId(), false);
        }
        notifyResultOperation(chatId, IpMessageConsts.GroupActionList.VALUE_ACCEPT_GROUP_INVITE,
                result);
    }

    @Override
    public void onRejectInvitationResult(String chatId, boolean result) throws RemoteException {
        Log.d(TAG, "onRejectInvitationResult. chatId = " + chatId + ", result = " + result);
        Context context = ContextCacher.getHostContext();
        boolean autoRejected = getInvitationRejectedState(context, chatId);

        if (result) {
            if (autoRejected) {
                removeInvitationRejectRecord(context, chatId);
            }
        } else {
            if (!autoRejected && RCSServiceManager.getInstance().serviceIsReady()) {
                Log.d(TAG, "reject fail. need record it");
                addInvitationRejectRecord(ContextCacher.getHostContext(), chatId);
                result = true;
            }
        }
        if (result) {
            updateGroupStatus(chatId, IpMessageConsts.GroupActionList.GROUP_STATUS_INVALID, false);
        }
        if (!autoRejected) {
            notifyResultOperation(chatId,
                    IpMessageConsts.GroupActionList.VALUE_REJECT_GROUP_INVITE, result);
        }
    }

    @Override
    public void onUpdateFileTransferStatus(long ipMsgId, int rcsStatus, int smsStatus) {
        Log.d(TAG, "onUpdateFileTransferStatus , ipMsgId = " + -ipMsgId + " rcsStatus = "
                + rcsStatus + " smsStatus " + smsStatus);
        Status stat = RCSUtils.getRcsStatus(rcsStatus);
        Intent it = new Intent();
        it.setAction(IpMessageConsts.IpMessageStatus.ACTION_MESSAGE_STATUS);
        it.putExtra(IpMessageConsts.STATUS, stat);
        it.putExtra(IpMessageConsts.IpMessageStatus.IP_MESSAGE_ID, -ipMsgId);
        RCSServiceManager.getInstance().callNotifyListeners(it);
        RCSCacheManager.updateStatus(-ipMsgId, stat, smsStatus);
    }

    @Override
    public void setFilePath(long ipMsgId, String filePath) {
        Log.d(TAG, "setFilePath , ipMsgId = " + -ipMsgId + " filePath = " + filePath);
        RCSCacheManager.setFilePath(-ipMsgId, filePath);
    }

    private int updateGroupAddress(String chatId) {
        Logger.d(TAG, "updateGroupAddress, entry");
        List<String> participants = GroupManager.getInstance(ContextCacher.getPluginContext())
                .getGroupParticipants(chatId);
        long threadId = ensureGroupThreadId(chatId);
        Uri.Builder uriBuilder = RCSUtils.MMS_SMS_URI_GROUP_ADDRESS.buildUpon();
        String recipient = null;
        for (String participant : participants) {
            if (Mms.isEmailAddress(participant)) {
                recipient = Mms.extractAddrSpec(recipient);
            }
            Logger.d(TAG, "recipient = " + recipient);
            uriBuilder.appendQueryParameter("recipient", recipient);
        }
        return ContextCacher
                .getHostContext()
                .getContentResolver()
                .update(ContentUris.withAppendedId(uriBuilder.build(), threadId),
                        new ContentValues(1), null, null);
    }

    private void notifyInitGroupResult(String chatId, int actionType, boolean result,
            List<String> participants) {
        Logger.d(TAG, "notifyInitGroupResult, entry: actionType = " + actionType + ", result="
                + result);
        int success = (result == true) ? IpMessageConsts.GroupActionList.VALUE_SUCCESS
                : IpMessageConsts.GroupActionList.VALUE_FAIL;
        Intent intent = getNotifyIntent(chatId,
                IpMessageConsts.GroupActionList.ACTION_GROUP_OPERATION_RESULT, actionType);
        intent.putExtra(IpMessageConsts.GroupActionList.KEY_ACTION_RESULT, success);
        String[] parts = new String[participants.size()];
        int i = 0;
        for (String participant : participants) {
            parts[i++] = RCSDataBaseUtils.getContactDisplayName(ContextCacher.getPluginContext(),
                    participant);
        }
        intent.putExtra(IpMessageConsts.GroupActionList.KEY_PARTICIPANT_LIST, parts);
        notifyAllListeners(intent);
    }

    private void notifyParticipantOperation(String chatId, int actionType, Participant participant) {
        Logger.d(TAG, "notifyParticipantOperation, entry: actionType = " + actionType);
        Intent intent = getNotifyIntent(chatId,
                IpMessageConsts.GroupActionList.ACTION_GROUP_NOTIFY, actionType);
        String name = RCSDataBaseUtils.getContactDisplayName(ContextCacher.getPluginContext(),
                participant.getContact());
        Participant contact = new Participant(participant.getContact(), name);
        intent.putExtra(IpMessageConsts.GroupActionList.KEY_PARTICIPANT, contact);
        notifyAllListeners(intent);
    }

    private Intent getNotifyIntent(String chatId, String action, int actionType) {
        long threadId = 0;
        MapInfo info = ThreadMapCache.getInstance().getInfoByChatId(chatId);
        if (info != null) {
            threadId = info.getThreadId();
        }
        Intent intent = new Intent();
        intent.setAction(action);
        intent.putExtra(IpMessageConsts.GroupActionList.KEY_ACTION_TYPE, actionType);
        intent.putExtra(IpMessageConsts.GroupActionList.KEY_CHAT_ID, chatId);
        intent.putExtra(IpMessageConsts.GroupActionList.KEY_THREAD_ID, threadId);
        return intent;
    }

    private void notifyStringOperation(String chatId, int actionType, String key, String value) {
        Logger.d(TAG, "notifyStringOperation, entry: actionType = " + actionType + ", key=" + key
                + ", value=" + value);
        Intent intent = getNotifyIntent(chatId,
                IpMessageConsts.GroupActionList.ACTION_GROUP_NOTIFY, actionType);
        intent.putExtra(key, value);
        notifyAllListeners(intent);
    }

    private void markGroupUnavailable(String chatId) {
        Logger.d(TAG, "markGroupUnavailable, chatId=" + chatId);
        updateGroupStatus(chatId, IpMessageConsts.GroupActionList.GROUP_STATUS_INVALID, false);
        Intent intent = new Intent("com.mediatek.rcs.groupchat.STATE_CHANGED");
        intent.putExtra("chat_id", chatId);
        ContextCacher.getPluginContext().sendBroadcast(intent);
    }

    private void updateGroupStatus(String chatId, long status, boolean addDate) {
        Logger.d(TAG, "updateGroupStatus, chatId=" + chatId + ", status=" + status);
        ContentValues values = new ContentValues();
        values.put(Threads.STATUS, status);
        if (addDate) {
            values.put(Threads.DATE, System.currentTimeMillis());
        }
        MapInfo info = ThreadMapCache.getInstance().getInfoByChatId(chatId);
        if (info != null) {
            long threadId = info.getThreadId();
            if (threadId > 0) {
                Uri uri = ContentUris.withAppendedId(RCSUtils.URI_THREADS_UPDATE_STATUS, threadId);
                ContextCacher.getHostContext().getContentResolver().update(uri, values, null, null);
                ThreadMapCache.getInstance().updateStatusByChatId(chatId, status);
            } else {
                if (status == IpMessageConsts.GroupActionList.GROUP_STATUS_INVALID) {
                    ThreadMapCache.getInstance().removeByChatId(chatId);
                } else {
                    ThreadMapCache.getInstance().updateStatusByChatId(chatId, status);
                }
            }
        } else {
            Logger.e(TAG, "error: can not find threadId, maybe no invite come to AP");
        }
    }

    private void notifyNullParamOperation(String chatId, int actionType) {
        Logger.d(TAG, "notifyNullParamOperation, entry: actionType = " + actionType);
        Intent intent = getNotifyIntent(chatId,
                IpMessageConsts.GroupActionList.ACTION_GROUP_NOTIFY, actionType);
        notifyAllListeners(intent);
    }

    private long createGroupThread(Participant participant, boolean fromInvite) {
        Logger.d(TAG, "createGroupThread, participant = " + participant + ", fromInvite= "
                + fromInvite);
        String contact = null;
        Uri.Builder builder = RCSUtils.MMS_SMS_URI_ADD_THREAD.buildUpon();
        if (participant != null) {
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
     * Group is deleted in message, When receive a group message, create group again.
     *
     * @param participants
     * @param fromInvite
     * @return thread id
     */
    private long createGroup(List<Participant> participants, boolean fromInvite) {
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

    private void markGroupInvitationTimeOut(String chatId) {
        Logger.d(TAG, "markGroupInvitationTimeOut, chatId= " + chatId);
        updateGroupStatus(chatId, IpMessageConsts.GroupActionList.GROUP_STATUS_INVITE_EXPAIRED,
                false);
    }

    private void notifyResultOperation(String chatId, int actionType, boolean result) {
        Logger.d(TAG, "notifyResultOperation, entry: actionType = " + actionType + ", result="
                + result);
        int success = (result == true) ? IpMessageConsts.GroupActionList.VALUE_SUCCESS
                : IpMessageConsts.GroupActionList.VALUE_FAIL;
        Intent intent = getNotifyIntent(chatId,
                IpMessageConsts.GroupActionList.ACTION_GROUP_OPERATION_RESULT, actionType);
        intent.putExtra(IpMessageConsts.GroupActionList.KEY_ACTION_RESULT, success);
        notifyAllListeners(intent);
    }

    private void notifyResultOperationWithStringArg(String chatId, int actionType, boolean result,
            String key, String value) {
        Logger.d(TAG, "notifyResultOperationWithStringArg, entry: actionType = " + actionType
                + ", result=" + result);
        int success = (result == true) ? IpMessageConsts.GroupActionList.VALUE_SUCCESS
                : IpMessageConsts.GroupActionList.VALUE_FAIL;
        Intent intent = getNotifyIntent(chatId,
                IpMessageConsts.GroupActionList.ACTION_GROUP_OPERATION_RESULT, actionType);
        intent.putExtra(key, value);
        intent.putExtra(IpMessageConsts.GroupActionList.KEY_ACTION_RESULT, success);
        notifyAllListeners(intent);
    }

    private void notifyAllListeners(Intent intent) {
        long threadId = intent.getLongExtra(IpMessageConsts.GroupActionList.KEY_THREAD_ID, 0);
        if (threadId > 0) {
            RCSServiceManager.getInstance().callNotifyListeners(intent);
        }
    }

    private String getSubject(String chatId) {
        Logger.d(TAG, "getSubject, chatId=" + chatId);
        String chatSelection = ChatLog.GroupChat.CHAT_ID + "='" + chatId + "'";
        Cursor chatCursor = null;
        try {
            chatCursor = ContextCacher.getPluginContext().getContentResolver()
                    .query(RCSUtils.RCS_URI_GROUP_CHAT, RCSUtils.PROJECTION_GROUP_INFO,
                            chatSelection, null, null);
            if (chatCursor.moveToFirst()) {
                return chatCursor.getString(chatCursor.getColumnIndex(ChatLog.GroupChat.SUBJECT));
            }
        } finally {
            if (chatCursor != null) {
                chatCursor.close();
            }
        }
        return null;
    }

    private String getMyNickName(String chatId) {
        String memberSelection = GroupMemberData.COLUMN_CHAT_ID + "='" + chatId + "'";
        Cursor cursor = ContextCacher
                .getPluginContext()
                .getContentResolver()
                .query(RCSUtils.RCS_URI_GROUP_MEMBER, RCSUtils.PROJECTION_GROUP_MEMBER,
                        memberSelection, null, null);
        String myNickName = null;
        IRCSChatService service = RCSServiceManager.getInstance().getChatService();
        String myNumber = null;
        try {
            myNumber = service.getMSISDN();

        } catch (RemoteException e) {
            e.printStackTrace();
        }
        try {
            while (cursor.moveToNext()) {
                String number = cursor.getString(cursor
                        .getColumnIndex(GroupMemberData.COLUMN_CONTACT_NUMBER));
                String name = cursor.getString(cursor
                        .getColumnIndex(GroupMemberData.COLUMN_CONTACT_NAME));
                if (PhoneNumberUtils.compare(number, myNumber)) {
                    myNickName = name;
                    break;
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return myNickName;
    }

    private boolean addInvitationRejectRecord(Context context, String chatId) {
        SharedPreferences sp = context.getSharedPreferences(INVITATION_PREFERENCE,
                Context.MODE_WORLD_WRITEABLE);
        Editor editor = sp.edit();
        editor.putBoolean(chatId, true);
        Logger.d(TAG, "[addInvitationRejectRecord], chatId=" + chatId);
        return editor.commit();
    }

    private boolean removeInvitationRejectRecord(Context context, String chatId) {
        SharedPreferences sp = context.getSharedPreferences(INVITATION_PREFERENCE,
                Context.MODE_WORLD_WRITEABLE);
        Editor editor = sp.edit();
        editor.remove(chatId);
        Logger.d(TAG, "[removeInvitationRejectRecord], chatId=" + chatId);
        return editor.commit();
    }

    private boolean getInvitationRejectedState(Context context, String chatId) {
        SharedPreferences sp = context.getSharedPreferences(INVITATION_PREFERENCE,
                Context.MODE_WORLD_READABLE);
        boolean value = sp.getBoolean(chatId, false);
        return value;
    }

    /**
     * Ensure group thread id is right. When receive group event exception invitation.
     * Must ensure the thread id is exit.
     *
     * @param chatId group chat id
     * @return real thread id
     */
    private long ensureGroupThreadId(String chatId) {
        MapInfo info = ThreadMapCache.getInstance().getInfoByChatId(chatId);
        long threadId = 0;
        if (info != null) {
            threadId = info.getThreadId();
            if (threadId == RCSUtils.DELETED_THREAD_ID) {
                Logger.d(TAG, "[ensureGroupThreadId], chatId=" + chatId + "threadid = " + threadId);
                long threadIdInDB = RCSDataBaseUtils.getThreadIdByChatId(chatId);
                if (threadIdInDB != 0 && threadIdInDB != threadId) {
                    threadId = threadIdInDB;
                } else {
                    List<Participant> participants = RCSDataBaseUtils
                            .getGroupAvailableParticipants(chatId);
                    threadId = createGroup(participants, false);
                }
                ThreadMapCache.getInstance().updateThreadId(chatId, threadId);
            }
        } else {
            throw new RuntimeException("[ensureGroupThreadId] don't find mapinfo");
        }
        return threadId;
    }
}
