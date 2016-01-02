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
package com.mediatek.rcs.message.plugin;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.mms.ipmessage.IpMessagingNotification;
import com.mediatek.rcs.message.R;
import com.mediatek.rcs.message.utils.RcsMessageUtils;

import com.mediatek.rcs.common.IpMessage;
import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.common.IpImageMessage;
import com.mediatek.rcs.common.RCSMessageManager;
import com.mediatek.rcs.common.provider.ThreadMapCache;
import com.mediatek.rcs.common.provider.ThreadMapCache.MapInfo;
import com.mediatek.rcs.common.service.Participant;
import com.mediatek.rcs.common.utils.ContextCacher;

/**
 * Plugin implements. Response file is MessagingNotification in MMS host.
 *
 */
public class RcsMessagingNotification extends IpMessagingNotification {
    private static final String TAG = "RcseMessagingNotification";
    private static final String NEW_MESSAGE_ACTION = "com.mediatek.mms.ipmessage.newMessage";
    private static int NEW_GROUP_INVITATION_NOTIFY_ID = 321;
    private static final String NOTIFICATION_RINGTONE = "pref_key_ringtone";
    private static final String NOTIFICATION_MUTE = "pref_key_mute";
    private static final String MUTE_START = "mute_start";
    private static final String DEFAULT_RINGTONE = "content://settings/system/notification_sound";

    private Context mContext;
    private IntentFilter mIntentFilter;

    @Override
    public boolean IpMessagingNotificationInit(Context context) {
        mContext = context;
        return super.IpMessagingNotificationInit(context);
    }

    @Override
    public String onIpFormatBigMessage(String number, String sender) {
        Log.d(TAG, "onIpFormatBigMessage: " + number + ", " + sender);
        return super.onIpFormatBigMessage(number, sender);
    }

    @Override
    public boolean isIpAttachMessage(long msgId, Cursor cursor) {
        long ipmsgId = cursor.getLong(cursor.getColumnIndex(Sms.IPMSG_ID));
        Log.d(TAG, "isIpAttachMessage: ipmsgId = " + ipmsgId);
        if (ipmsgId > 0) {
            return false;
        } else if (ipmsgId < 0) {
            return true;
        }
        return super.isIpAttachMessage(msgId, cursor);
    }

    @Override
    public String onNewIpMsgContent(long msgId, Cursor cursor) {
        Context pluginContext = ContextCacher.getPluginContext();
        long ipMsgId = cursor.getLong(6);
        if (ipMsgId == 0) {
            return null;
        }
        IpMessage ipMessage = RCSMessageManager.getInstance(pluginContext).getIpMsgInfo(ipMsgId);
        if (null != ipMessage && ipMessage.getBurnedMessage()) {
            return pluginContext.getString(R.string.menu_burned_msg);
        } else if ( null != ipMessage){
            int ipMessageType = ipMessage.getType();
            if (ipMessageType == IpMessageConsts.IpMessageType.PICTURE) {
                return "[Picture]";
            } else if(ipMessageType == IpMessageConsts.IpMessageType.VIDEO) {
                return "[Video]";
            } else if (ipMessageType == IpMessageConsts.IpMessageType.VOICE) {
                return "[Voice]";
            } else if(ipMessageType == IpMessageConsts.IpMessageType.VCARD) {
                return "[Vcard]";
            } else if (ipMessageType == IpMessageConsts.IpMessageType.GEOLOC) {
                return "[Geolocation]";
            }
        }
        return null;
    }

    @Override
    public Bitmap getIpBitmap(long msgId, Cursor cursor) {
        IpMessage ipMessage = RCSMessageManager.getInstance(mContext).getIpMsgInfo(msgId);
        Log.d(TAG, "getIpBitmap: ipMessage = " + ipMessage);
        if (null != ipMessage) {
            int ipMessageType = ipMessage.getType();
            if (ipMessageType == IpMessageConsts.IpMessageType.PICTURE) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                String filePath = ((IpImageMessage) ipMessage).getPath();
                return BitmapFactory.decodeFile(filePath);
            }
        }
        return null;
    }

    @Override
    public boolean onIpgetNewMessageNotificationInfo(String number, long threadId) {
        return super.onIpgetNewMessageNotificationInfo(number, threadId);
    }

    @Override
    public String getIpNotificationTitle(String number, long threadId, String title) {
        boolean isGroupChat = RcsMessageUtils.isGroupchat(threadId);
        Log.d(TAG, "getIpNotificationTitle: isGroupChat = " + isGroupChat);
        if (isGroupChat) {
            MapInfo info = ThreadMapCache.getInstance().getInfoByThreadId(threadId);
            if (info != null) {
                String groupName = info.getNickName();
                if (TextUtils.isEmpty(groupName)) {
                    groupName = info.getSubject();
                }
                if (TextUtils.isEmpty(groupName)) {
                    groupName = title;
                }
                return groupName;
            }
        }
        return title;
    }

    @Override
    public boolean setIpSmallIcon(Notification.Builder noti, String number) {
        return super.setIpSmallIcon(noti, number);
    }

    @Override
    public String ipBuildTickerMessage(String address, String displayAddress) {
        return displayAddress;
    }

    /**
     * Called when want to cancel group invitation notifications.
     * @param context Context
     */
    public static void cancelNewGroupInviations(Context context) {
        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NEW_GROUP_INVITATION_NOTIFY_ID);
    }

    /**
     * Called when a group invitation arrived.
     * @param p inviter
     * @param subject  group's subject
     * @param threadId  thread id
     */
    public static void updateNewGroupInvitation(Participant p, String subject, long threadId) {
        asynUpdateNewGroupInvitation(p, subject, threadId);
    }

    private static void asynUpdateNewGroupInvitation(final Participant p, final String subject,
                                            final long threadId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                Context hostContext = ContextCacher.getHostContext();
                Context pluginContext = ContextCacher.getPluginContext();
                int count = 0;
                boolean isCurrentThreadExist = false;
                if (hostContext != null) {
                    Cursor cursor = hostContext.getContentResolver().query(
                            RcsConversation.URI_CONVERSATION,
                            RcsConversation.PROJECTION_NEW_GROUP_INVITATION,
                            RcsConversation.SELECTION_NEW_GROUP_INVITATION_BY_STATUS,
                            null, null);
                    if (cursor != null) {
                        try {
                            count = cursor.getCount();
                            cursor.moveToFirst();
                            do {
                                long thread_id = cursor.getLong(0);
                                if (thread_id == threadId) {
                                    isCurrentThreadExist = true;
                                    break;
                                }
                            } while(cursor.moveToNext());
                        } catch (Exception e) {
                            // TODO: handle exception
                        } finally {
                            cursor.close();
                        }
                    }
                } else {
                    throw new RuntimeException("asynUpdateNewGroupInvitation, context is null");
                }
                if (!isCurrentThreadExist) {
                    count ++;
                }
                notifyNewGroupInvitation(p, subject, threadId, count);
            }
        }, "asynUpdateNewGroupInvitation").start();
    }

    private static void notifyNewGroupInvitation(final Participant p, final String subject,
            final long threadId, int count) {
        long timeMillis = System.currentTimeMillis();
        Context pluginContext = ContextCacher.getPluginContext();
        Context hostContext = ContextCacher.getHostContext();
        int smallIcon = RcsUtilsPlugin.getNotificationResourceId();
        if (smallIcon == 0) {
            return;
        }
        String contentTitle = null;
        String contentText = null;
        Bitmap largeIcon = BitmapFactory.decodeResource(pluginContext.getResources(),
                R.drawable.group_example);
        String tiker = subject + pluginContext.getString(R.string.group_invite_tiker);
        if (count == 1) {
            contentTitle = subject;
            if (TextUtils.isEmpty(contentTitle)) {
                contentTitle = pluginContext.getString(R.string.group_chat);
            }
            String invitee = p.getDisplayName();
            if (TextUtils.isEmpty(invitee)) {
                String number = p.getContact();
                if (!TextUtils.isEmpty(number)) {
                    Log.e(TAG, "[notifyNewGroupInvitation]: number is null");
                    invitee = RcsUtilsPlugin.getContactNameByNumber(number);
                }
            }
            if (TextUtils.isEmpty(invitee)) {
                invitee = "Unknown";
            }
            contentText = pluginContext.getString(R.string.one_group_invite_content, invitee);
        } else {
            contentTitle = pluginContext.getString(R.string.group_chat);
            contentText = pluginContext.getString(R.string.more_group_invite_content, count);
        }
        int defaults = getNotificationDefaults(hostContext);
        //content intent
        Intent clickIntent = null;
        clickIntent = new Intent();
        clickIntent.setClassName(hostContext,
                          "com.android.mms.transaction.MessagingNotificationProxyReceiver");
        clickIntent.putExtra("thread_count", count);
        clickIntent.putExtra("thread_id", threadId);
        PendingIntent pIntent = PendingIntent.getBroadcast(hostContext, 0,
                                                clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // notify sound
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(hostContext);
        String muteStr = sp.getString(NOTIFICATION_MUTE, Integer.toString(0));
        long appMute = Integer.parseInt(muteStr);
        long appMuteStart = sp.getLong(MUTE_START, 0);
        if (appMuteStart > 0 && appMute > 0) {
            long currentTime = (System.currentTimeMillis() / 1000);
            if ((appMute * 3600 + appMuteStart / 1000) <= currentTime) {
                appMute = 0;
                appMuteStart = 0;
                SharedPreferences.Editor editor =
                                PreferenceManager.getDefaultSharedPreferences(hostContext).edit();
                editor.putLong(MUTE_START, 0);
                editor.putString(NOTIFICATION_MUTE, String.valueOf(appMute));
                editor.apply();
            }
        }
        Uri ringtone = null;
        if (appMute == 0) {
            String ringtoneStr = sp.getString(NOTIFICATION_RINGTONE, null);
            ringtoneStr = checkRingtone(hostContext, ringtoneStr);
            ringtone = TextUtils.isEmpty(ringtoneStr) ? null : Uri.parse(ringtoneStr);
        }
        final Notification.Builder notifBuilder = new Notification.Builder(hostContext)
        .setWhen(timeMillis)
        .setContentTitle(contentTitle)
        .setContentText(contentText)
        .setLargeIcon(largeIcon)
        .setDefaults(defaults)
        .setContentIntent(pIntent)
        .setSmallIcon(smallIcon)
        .setSound(ringtone);
        
        Notification notification = notifBuilder.build();
        NotificationManager nm = (NotificationManager)
                hostContext.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NEW_GROUP_INVITATION_NOTIFY_ID, notification);
    }

    private static final String checkRingtone(Context context, String ringtoneUri) {
        if (!TextUtils.isEmpty(ringtoneUri)) {
            InputStream inputStream = null;
            boolean invalidRingtone = true;
            try {
                inputStream = context.getContentResolver().openInputStream(Uri.parse(ringtoneUri));
            } catch (FileNotFoundException ex) {
            } finally {
                if (inputStream != null) {
                    invalidRingtone = false;
                    try {
                        inputStream.close();
                    } catch (IOException ex) {
                    }
                }
            }
            if (invalidRingtone) {
                ringtoneUri = DEFAULT_RINGTONE;
            }
        }
        return ringtoneUri;
    }

    protected static void processNotificationSound(Context context, Notification notification,
                                                        Uri ringtone) {
        int state = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE))
                                                        .getCallState();
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.shouldVibrate(AudioManager.VIBRATE_TYPE_NOTIFICATION)) {
            /* vibrate on */
            notification.defaults |= Notification.DEFAULT_VIBRATE;
        }
        notification.sound = ringtone;
    }

    protected static int getNotificationDefaults(Context context) {
        int defaults = 0;
        int state = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE))
                                                                                   .getCallState();
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.shouldVibrate(AudioManager.VIBRATE_TYPE_NOTIFICATION)) {
            /* vibrate on */
            defaults |= Notification.DEFAULT_VIBRATE;
        }
        defaults |= Notification.DEFAULT_LIGHTS;
        return defaults;
    }
}