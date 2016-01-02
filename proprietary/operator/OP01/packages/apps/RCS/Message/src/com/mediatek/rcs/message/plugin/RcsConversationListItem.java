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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.QuickContactBadge;
import android.widget.RelativeLayout;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.provider.Telephony.Sms;

import com.mediatek.mms.ipmessage.IpContact;
import com.mediatek.mms.ipmessage.IpConversation;
import com.mediatek.mms.ipmessage.IpConversationListItem;
import com.mediatek.rcs.message.group.PortraitManager;
import com.mediatek.rcs.message.group.PortraitManager.GroupThumbnail;
import com.mediatek.rcs.message.group.PortraitManager.onGroupPortraitChangedListener;
import com.mediatek.rcs.message.plugin.EmojiImpl;
import com.mediatek.rcs.message.utils.RcsMessageConfig;
import com.mediatek.rcs.message.utils.RcsMessageUtils;
import com.mediatek.rcs.message.R;
import com.mediatek.rcs.common.IpMessage;
import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.common.RCSMessageManager;
import com.mediatek.rcs.common.provider.ThreadMapCache;
import com.mediatek.rcs.common.provider.ThreadMapCache.MapInfo;
import com.mediatek.rcs.common.utils.ContextCacher;

import com.mediatek.rcs.common.utils.RCSUtils;

/**
 * Plugin implements. response ConversationListItem in MMS host.
 *
 */
public class RcsConversationListItem extends IpConversationListItem
                                     implements onGroupPortraitChangedListener {
    private static String TAG = "RcsConversationListItem";
    /// M: New feature for rcse, adding IntegrationMode.
    private ImageView mFullIntegrationModeView;
    private Context mContext;
    private QuickContactBadge mAvatarView;
    private long mThreadId;
    private Context mPluginContext;
    private boolean mVisible; //true between onbind and ondetached
    private Handler mHandler;
    private GroupThumbnail mThumbnail;

    /**
     * Construction.
     * @param pluginContext Context
     */
    public RcsConversationListItem(Context pluginContext) {
        mPluginContext = pluginContext;
    }

    @Override
    public void onIpSyncView(Context context, ImageView fullIntegrationModeView,
                             QuickContactBadge avatarView) {
        Log.d(TAG, "onIpSyncView");
        mContext = context;
        mFullIntegrationModeView = fullIntegrationModeView;
        mFullIntegrationModeView.setVisibility(View.VISIBLE);
        mAvatarView = avatarView;
    }

    @Override
    public String onIpFormatMessage(IpContact ipContact, long threadId,
                                    String number, String name) {
        Log.d("avatar", "ConvListItem.formatMessage(): number = " + number
                + ", name = " + name);
            MapInfo info = ThreadMapCache.getInstance().getInfoByThreadId(threadId);
            if (info != null) {
                name = info.getNickName();
                Log.d(TAG, "group's nickName: " + name);
                if (TextUtils.isEmpty(name)) {
                    name = info.getSubject(); 
                }
                Log.d(TAG, "onIpFormatMessage: number = " + number + ", group name = " + name);
            }
        return name;
    }

    @Override
    public boolean updateIpAvatarView(final IpContact ipContact, String number,
                                      QuickContactBadge avatarView, final Uri uri) {
        String chatId = RcsMessageUtils.getGroupChatIdByThread(mThreadId);
        Log.d(TAG, "updateIpAvatarView: chatId = " + chatId );

        if (chatId != null) {
            updateGroupAvataView(chatId);
            return true;
        }
        return false;
    }

    @Override
    public boolean updateIpAvatarView(IpConversation ipConv, QuickContactBadge avatarView) {
        String chatId = RcsMessageUtils.getGroupChatIdByThread(mThreadId);
        Log.d(TAG, "updateIpAvatarView: chatId = " + chatId );
        if (chatId != null) {
            updateGroupAvataView(chatId);
            return true;
        }
        return false;
    }

    @Override
    public boolean onIpBind(IpConversation ipConv, boolean isActionMode, boolean isChecked,
                            int convType, RelativeLayout conversationItem, TextView fromView,
                            TextView subjectView, TextView dateView) {
        Log.d(TAG, "onIpBind: ipConv = " + ipConv );
        Log.d(TAG, "onIpBind: isActionMode = " + isActionMode + ", isChecked = "
                    + isChecked);
        mVisible = true;
        mHandler = new Handler();
        if (ipConv instanceof RcsConversation) {
            RcsConversation conv = (RcsConversation) ipConv;
            mThreadId = conv.getThreadId();

            // show sticky conversations as a different background color
            boolean isSticky = conv.isSticky();
            if (isActionMode) {
                if (!isChecked) {
                    if (isSticky) {
                        conversationItem.setBackgroundColor(Color.parseColor("#F5FFF1"));
                    } else {
                        conversationItem.setBackgroundColor(0);
                    }
                }
            } else {
                if (isSticky) {
                    conversationItem.setBackgroundColor(Color.parseColor("#F5FFF1"));
                } else {
                    conversationItem.setBackgroundColor(0);
                }
            }

            // show Group chat invite status string
            String chatId = RcsMessageUtils.getGroupChatIdByThread(mThreadId);
            Log.d(TAG, "onIpBind: group chatId = " + chatId );
            if (chatId != null) {
                MapInfo info = ThreadMapCache.getInstance().getInfoByThreadId(mThreadId);
                long status = info.getStatus();
                Log.d(TAG, "onIpBind: group status = " + status );
                String statusText = null;
                if (status == IpMessageConsts.GroupActionList.GROUP_STATUS_INVITING ||
                        status == IpMessageConsts.GroupActionList.GROUP_STATUS_INVITING_AGAIN) {
                    statusText = mPluginContext.getString(R.string.group_invite);
                } else if (status == IpMessageConsts.GroupActionList.GROUP_STATUS_INVITE_EXPAIRED) {
                    statusText = mPluginContext.getString(R.string.group_invite_expired);
                }
                if (statusText != null) {
                    subjectView.setText(statusText);
                }
            }

            String subject = subjectView.getText().toString();
            Log.d(TAG, "onIpBindHide: subject = " + subject);
            if (!TextUtils.isEmpty(subject)) {
                String body = null;
                Context pluginContext = ContextCacher.getPluginContext();
                RCSMessageManager msgManager = RCSMessageManager.getInstance(pluginContext);
                long ipMsgId = msgManager.getIpMsgId(mThreadId);
                IpMessage ipMessage = msgManager.getIpMsgInfo(ipMsgId);
                if (null != ipMessage ) {
                     Log.d(TAG, "onIpBindHide: subject = " + subject+ " mThreadId = "
                             +mThreadId+ " ipMsgId= "+ipMsgId);
                     Log.d(TAG, "onIpBindHide: ipMessage.getBurnedMessage() = " +
                             ipMessage.getBurnedMessage() + " ipMessage.getStatus() = "
                             +ipMessage.getStatus());
                     if ( ipMessage.getBurnedMessage() && 
                             !(ipMessage.getStatus() == Sms.MESSAGE_TYPE_SENT ||
                              ipMessage.getStatus() == Sms.MESSAGE_TYPE_DRAFT ||
                              ipMessage.getStatus() == Sms.MESSAGE_TYPE_OUTBOX )) {
                         subjectView.setText(pluginContext.getString(R.string.menu_burned_msg));
                         return false;
                     }
                }
                //format format conversation list item subject if has filetransfer
                if (subject.contains(RCSUtils.IMAGE_FT_TYPE)) {
                    body = "[Picture]";
                } else if (subject.contains(RCSUtils.VIDEO_FT_TYPE)) {
                    body = "[Video]";
                } else if (subject.contains(RCSUtils.AUDIO_FT_TYPE)) {
                    body = "[Audio]";
                } else if (subject.contains(RCSUtils.VCARD_FT_TYPE)) {
                    body = "[Vcard]";
                } else if (subject.contains(RCSUtils.GEOLOC_FT_TYPE)) {
                    body = "[Geolocation]";
                } else if (subject.contains(RCSUtils.FILE_TYPE)) {
                    body = "[File]";
                } 
                if (body != null) {
                    subjectView.setText(body);
                    return false;
                }

                // format conversation list item subject if has emoji image
                EmojiImpl emoji = EmojiImpl.getInstance(mPluginContext);
                CharSequence cs = emoji.getEmojiExpression(subject, true);
                subjectView.setText(cs);
            }
        }
        return false;
    }

    private void updateGroupAvatarView(Drawable drawable, Uri uri) {
        mAvatarView.setImageDrawable(drawable);
        mAvatarView.setVisibility(View.VISIBLE);
        mAvatarView.assignContactUri(uri);
    }

    @Override
    public void onIpDetachedFromWindow() {
//        move to onIpUnbind
    }

    @Override
    public void onIpUnbind() {
        mVisible = false;
        if (mThumbnail != null) {
            mThumbnail.removeChangedListener(this);
            mThumbnail = null;
        }
    }
    
    private void updateGroupAvataView(String chatId) {
        if (mThumbnail != null) {
            mThumbnail.removeChangedListener(this);
            mThumbnail = null;
        }
        try {
            mThumbnail = PortraitManager.getInstance().getGroupPortrait(chatId);
        } catch (Exception e) {
            // TODO: handle exception
            Log.e(TAG, "updateGroupAvataView: e: " +e);
            
        }
        if (mThumbnail != null) {
            mThumbnail.addChangedListener(this);
            mAvatarView.setImageBitmap(mThumbnail.mBitmap);
        } else {
            Drawable drawable = RcsMessageUtils.getGroupDrawable(mThreadId);
            mAvatarView.setImageDrawable(drawable);
        }
        mAvatarView.setVisibility(View.VISIBLE);
        mAvatarView.setOnClickListener(mAvataOnClickListener);
        mAvatarView.assignContactUri(null);
    }
    
    private OnClickListener mAvataOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            // TODO  process avatar click event

        }
    };

    @Override
    public void onChanged(final Bitmap newBitmap) {
        // TODO Auto-generated method stub
        if (mHandler != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    if (mVisible) {
                        mAvatarView.setImageBitmap(newBitmap);
                    }
                }
            });
        }
    }
}
