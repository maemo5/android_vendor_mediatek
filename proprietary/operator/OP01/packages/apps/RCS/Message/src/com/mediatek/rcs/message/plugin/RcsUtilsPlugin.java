package com.mediatek.rcs.message.plugin;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.mediatek.mms.ipmessage.IpContactInfo;
import com.mediatek.mms.ipmessage.IpMessageItem;
import com.mediatek.mms.ipmessage.IpUtils;
import com.mediatek.mms.ipmessage.IpUtilsCallback;

import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.common.provider.ThreadMapCache;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.common.utils.Logger;
import com.mediatek.rcs.common.GroupManager;
import com.mediatek.rcs.common.RCSMessageManager;

import com.mediatek.rcs.message.data.RcsProfile;
import com.mediatek.rcs.message.group.PortraitManager;
import com.mediatek.rcs.message.utils.NewMessageReceiver;
import com.mediatek.rcs.message.R;

import java.util.Collection;
import java.util.List;

/**
 * RcsUtilsPlugin. extends IpUtils.
 *
 */
public class RcsUtilsPlugin extends IpUtils {

    public static final String TAG = "RcsUtilsPlugin";


    private static RcsUtilsPlugin sInstance;
    private Context mPluginContext;

    /**
     * RcsUtilsPlugin Construction.
     * @param context Context
     */
    public RcsUtilsPlugin(Context context) {
        mPluginContext = context;
    }

    private static IpUtilsCallback sCallback;

    @Override
    public boolean initIpUtils(IpUtilsCallback ipUtilsCallback) {
        sCallback = ipUtilsCallback;
        return true;
    }

    @Override
    public boolean onIpBootCompleted(Context context) {
        Log.d(TAG, "onIpBootCompleted()");
        return false;
    }

    @Override
    public boolean onIpMmsCreate(final Context context) {
        ContextCacher.setHostContext(context);

        Logger.d(TAG, "onIpMmsCreate. start");
        String processName = getCurProcessName(context);
        Logger.d(TAG, "createManager, processName=" + processName);
        if (processName != null && processName.equalsIgnoreCase("com.android.mms")) {
            GroupManager.createInstance(ContextCacher.getHostContext());
            RCSServiceManager.createManager(context);
            NewMessageReceiver.init(context);
            RcsProfile.init(context);
            new Thread(new Runnable() {

                public void run() {
                    Logger.d(TAG, "RCSCreateChatsThread start run");
                    ThreadMapCache.createInstance(ContextCacher.getHostContext());
                }
            }, "RCSCreateChatsThread").start();
            PortraitManager.init(mPluginContext);
        }
        return false;
    }

    @Override
    public CharSequence formatIpMessage(CharSequence inputChars, boolean showImg,
            CharSequence inputBuf) {
        Log.d(TAG, "formatIpMessage(): inputChars = " + inputChars);
        if (inputChars == null) {
            return "";
        }

        EmojiImpl emoji = EmojiImpl.getInstance(ContextCacher.getPluginContext());
        CharSequence outChars = emoji.getEmojiExpression(inputChars, showImg);
        if (inputBuf == null) {
            return outChars;
        } else {
            String bufStr = inputBuf.toString();
            String inputStr = inputChars.toString();
            int start = bufStr.indexOf(inputStr);
            if (start == -1) {
                return inputBuf;
            }

            CharSequence bufChars = emoji.getEmojiExpression(inputBuf, showImg);
            return bufChars;
        }
    }

    @Override
    public void onIpDeleteMessage(Context context, Collection<Long> threadIds, int maxSmsId,
            boolean deleteLockedMessages) {
        Log.d(TAG, "onIpDeleteMessage, threads = " + threadIds);
        RCSMessageManager.getInstance(context).deleteRCSThreads(threadIds, maxSmsId,
                deleteLockedMessages);
    }

    @Override
    public String getIpTextMessageType(IpMessageItem item) {
        if (item == null) {
            return null;
        }
        RcsMessageItem rcsItem = (RcsMessageItem) item;
        if (rcsItem.getIpMessageId() != 0) {
            return mPluginContext.getString(R.string.rcs_message_type);
        }
        return null;
    }

    @Override
    public long getKey(String type, long msgId) {
        if (type.equals("rcs")) {
            return msgId + 10000000;
        }
        return 0;
    }

    /**
     * Get Notification Resource Id from host.
     * @return Notification resource id.
     */
    public static int getNotificationResourceId() {
        if (sCallback != null) {
            return sCallback.getNotificationResourceId();
        } else {
            return 0;
        }
    }

    /**
     * Get Contact Name by Number.This function will callback to host to get.
     * @param number Contact's number
     * @return Contact's Name in contact db. The number if the contact doesn't exist in db.
     */
    public static String getContactNameByNumber(String number) {
        String[] numbers = new String[] { number };
        List<IpContactInfo> infoList = sCallback.getContactInfoListByNumbers(numbers, true);
        if (infoList != null && infoList.size() > 0) {
            return infoList.get(0).getName();
        }
        return null;
    }

    /**
     * Unblocking notify new message. Called when receive one ipmessage.
     * @param context Context
     * @param newMsgThreadId Thread id belong to.
     * @param newSmsId The id in sms table.
     * @param ipMessageId the id in stack table.
     */
    public static void unblockingNotifyNewIpMessage(Context context, long newMsgThreadId,
            long newSmsId, long ipMessageId) {
        Log.d(TAG, "unblockingNotifyNewIpMessage: id = " + newSmsId);
        unblockingIpUpdateNewMessageIndicator(context, newMsgThreadId, false, null);
        if (isIpPopupNotificationEnable()) {
            notifyNewIpMessageDialog(context, newSmsId, ipMessageId);
        }
        notifyIpWidgetDatasetChanged(context);
    }

    private static void unblockingIpUpdateNewMessageIndicator(final Context context,
            final long newMsgThreadId, final boolean isStatusMessage,
            final Uri statusMessageUri) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                sCallback.blockingIpUpdateNewMessageIndicator(context, newMsgThreadId,
                        isStatusMessage, statusMessageUri);
            }
        }, "MessagingNotification.nonBlockingUpdateNewMessageIndicator").start();
    }

    private static void blockingIpUpdateNewMessageIndicator(Context context,
            long newMsgThreadId, boolean isStatusMessage, Uri statusMessageUri) {
        sCallback.blockingIpUpdateNewMessageIndicator(context, newMsgThreadId,
                isStatusMessage, statusMessageUri);
    }

    private static boolean isIpPopupNotificationEnable() {
        return sCallback.isIpPopupNotificationEnable();
    }

    private static void notifyIpWidgetDatasetChanged(Context context) {
        sCallback.notifyIpWidgetDatasetChanged(context);
    }

    private static void notifyNewIpMessageDialog(Context context, long smsId, long ipmessageId) {
        Log.d(TAG, "notifyNewIpMessageDialog,id:" + smsId);
        if (smsId == 0) {
            return;
        }
        if (sCallback.isIpHome(context)) {
            Log.d(TAG, "at launcher");
            Intent smsIntent = sCallback.getDialogModeIntent(context);
            Uri smsUri = Uri.parse("content://sms/" + smsId);
            smsIntent.putExtra("com.android.mms.transaction.new_msg_uri", smsUri.toString());
            smsIntent.putExtra("ipmessage", true);
            smsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            smsIntent.putExtra(IpMessageConsts.MessageAction.KEY_IPMSG_ID, ipmessageId);
            context.startActivity(smsIntent);
        } else {
            Log.d(TAG, "not at launcher");
        }
    }

    /**
     * Send mms. Will callback to host to send mms.
     * @param uri mms's uri
     * @param messageSize pdu's size
     * @param subId   which sub id to send
     * @param threadId the mms belong to
     * @return true if sent out, else return false.
     */
    public static boolean sendMms(Uri uri, long messageSize, int subId, long threadId) {
        if (sCallback != null) {
            return sCallback.sendMms(ContextCacher.getHostContext(), uri, messageSize, subId,
                    threadId);
        }
        return false;
    }

    /**
     * Get timeStamp format string. will callback to mms host.
     * @param when  long
     * @param fullFormat . if true, will have year, month, day and time
     * @return format string
     */
    public static String formatIpTimeStampString(long when, boolean fullFormat) {
        if (sCallback != null) {
            return sCallback.formatIpTimeStampString(ContextCacher.getHostContext(), when,
                    fullFormat);
        }
        return null;
    }

    private static String getCurProcessName(Context context) {
        int pid = android.os.Process.myPid();
        ActivityManager am = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo appProcess : am.getRunningAppProcesses()) {
            if (appProcess.pid == pid) {
                return appProcess.processName;
            }
        }
        return null;
    }
}
