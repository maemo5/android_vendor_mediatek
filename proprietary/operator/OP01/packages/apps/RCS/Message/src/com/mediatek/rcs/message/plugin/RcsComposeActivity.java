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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnClickListener;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.NetworkInfo.State;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.MediaStore.Audio;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.PhoneConstants;
import com.cmcc.ccs.blacklist.CCSblacklist;
import com.mediatek.mms.ipmessage.IpComposeActivity;
import com.mediatek.mms.ipmessage.IpComposeActivityCallback;
import com.mediatek.mms.ipmessage.IpContact;
import com.mediatek.mms.ipmessage.IpConversation;
import com.mediatek.mms.ipmessage.IpWorkingMessageCallback;
import com.mediatek.mms.ipmessage.IIpMessageItem;
import com.mediatek.mms.ipmessage.IpMessageItem;
import com.mediatek.mms.ipmessage.IpMessageListAdapter;
import com.mediatek.rcs.common.GroupManager;
import com.mediatek.rcs.common.IInitGroupListener;
import com.mediatek.rcs.common.IpAttachMessage;
import com.mediatek.rcs.common.IpImageMessage;
import com.mediatek.rcs.common.IpMessage;
import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.common.IpMessageConsts.GroupActionList;
import com.mediatek.rcs.common.IpMessageConsts.IpMessageStatus;
import com.mediatek.rcs.common.IpMessageConsts.IpMessageType;
import com.mediatek.rcs.common.IpTextMessage;
import com.mediatek.rcs.common.IpVCardMessage;
import com.mediatek.rcs.common.IpVideoMessage;
import com.mediatek.rcs.common.IpVoiceMessage;
import com.mediatek.rcs.common.IpGeolocMessage;
import com.mediatek.rcs.common.MessageStatusUtils.IFileTransfer.Status;
import com.mediatek.rcs.common.RCSGroup;
import com.mediatek.rcs.common.RCSMessageManager;
import com.mediatek.rcs.message.data.RcsProfile;
import com.mediatek.rcs.message.group.PortraitManager;
import com.mediatek.rcs.message.group.PortraitManager.MemberInfo;
import com.mediatek.rcs.message.location.GeoLocService;
import com.mediatek.rcs.message.location.GeoLocUtils;
import com.mediatek.rcs.message.location.GeoLocXmlParser;
import com.mediatek.rcs.message.plugin.RCSEmoji;
import com.mediatek.rcs.message.provider.FavoriteMsgProvider;
import com.mediatek.rcs.message.ui.CreateGroupActivity;
import com.mediatek.rcs.message.ui.RcsSettingsActivity;
import com.mediatek.rcs.message.utils.RcsMessageConfig;
import com.mediatek.rcs.message.utils.RcsMessageUtils;
import com.mediatek.rcs.message.utils.ThreadNumberCache;
import com.mediatek.rcs.message.R;
import com.mediatek.services.rcs.phone.ICallStatusService;
import com.mediatek.services.rcs.phone.IServiceMessageCallback;
import com.mediatek.storage.StorageManagerEx;
import com.mediatek.telecom.TelecomManagerEx;

import com.google.android.mms.ContentType;
import com.google.android.mms.pdu.PduComposer;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.RetrieveConf;
import com.google.android.mms.pdu.SendReq;
import static com.google.android.mms.pdu.PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND;
import static com.google.android.mms.pdu.PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF;

import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.binder.RCSServiceManager.OnServiceChangedListener;
import com.mediatek.rcs.common.provider.ThreadMapCache;
import com.mediatek.rcs.common.provider.ThreadMapCache.MapInfo;
import com.mediatek.rcs.common.RCSCacheManager;
import com.mediatek.rcs.common.service.Participant;
import com.mediatek.rcs.common.IpMessageConsts.IpMessageStatus;

import com.mediatek.rcs.message.plugin.RCSVCardAttachment;
import com.mediatek.rcs.message.plugin.RcsMessageListAdapter.OnMessageListChangedListener;
import com.mediatek.rcs.common.utils.RCSUtils;
import com.mediatek.rcs.common.IBurnMessageCapabilityListener;
import android.provider.Telephony.Sms;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.location.Location;

import com.mediatek.rcs.message.R;

/**
 * class RcsComposeActivity, plugin implements response ComposeMessageActivity.
 *
 */
public class RcsComposeActivity extends IpComposeActivity implements
        IBurnMessageCapabilityListener, OnServiceChangedListener {

    private static final String TAG = "RcsComposeActivity";

    // / M: host's option menu and context menu
    private static final int MENU_ADD_SUBJECT           = 0;
    private static final int MENU_CALL_RECIPIENT        = 5;
    private static final int MENU_GROUP_PARTICIPANTS    = 32;
    private static final int MENU_SELECT_MESSAGE        = 101;
    private static final int MENU_SHOW_CONTACT          = 121;
    private static final int MENU_CREATE_CONTACT        = 122;
    private static final int MENU_CHAT_SETTING          = 137;
    //rcs message new option menu item from 5000;
    private static final int MENU_GROUP_CHAT_INFO = 5000;
    private static final int MENU_EDIT_BURNED_MSG = 5001;

    // / M: add for ip message, context menu
    private static final int MENU_DELIVERY_REPORT = 20;
    private static final int MENU_FORWARD_MESSAGE = 21;
    private static final int MENU_SAVE_MESSAGE_TO_SUB = 32;
    private static final int MENU_IPMSG_DELIVERY_REPORT   = 33;
    private static final int MENU_SELECT_TEXT = 36;
    private static final int MENU_INVITE_FRIENDS_TO_CHAT = 100;
    private static final int MENU_RETRY = 200;
//    private static final int MENU_SHARE = 202;
//    private static final int MENU_VIEW_IP_MESSAGE = 206;
    private static final int MENU_COPY = 207;
//    private static final int MENU_SEND_VIA_TEXT_MSG = 208;
    private static final int MENU_EXPORT_SD_CARD = 209;
    private static final int MENU_FORWARD_IPMESSAGE = 210;
    private static final int MENU_FAVORITE = 211;
    private static final int MENU_BLACK_LIST = 214;
    private static final int MENU_REPORT = 1000;

    private static final int MENU_ADD_TO_BOOKMARK = 35;
    private static final int MENU_ADD_ADDRESS_TO_CONTACTS = 27;

    public static final int OK = 0;
    public static final int UPDATE_SENDBUTTON = 2;

    private static final String PREFERENCE_SEND_WAY_CHANGED_NAME = "sendway";
    private static final String PREFERENCE_KEY_SEND_WAY = "sendway";
    private static final int PREFERENCE_VALUE_SEND_WAY_UNKNOWN = 0;
    private static final int PREFERENCE_VALUE_SEND_WAY_IM = 1;
    private static final int PREFERENCE_VALUE_SEND_WAY_SMS = 2;
    private static final String PREFERENCE_KEY_SEND_WAY_IM = "sendway_im";
    private static final String PREFERENCE_KEY_SEND_WAY_SMS = "sendway_sms";

    private boolean mIsIpServiceEnabled = false;

    private ImageButton mSendButtonIpMessage; // press to send ipmessage
    private TextView mTypingStatus;
    // private TextView mRemoteStrangerText;
    private TextView mShowCallTimeText;
    private boolean mIsDestroyTypingThread = false;
    private long mLastSendTypingStatusTimestamp = 0L;

    // /M: for indentify that just send common message.
    private boolean mJustSendMsgViaCommonMsgThisTime = false;
    // /M: whether or not show invite friends to use ipmessage interface.
    private boolean mShowInviteMsg = false;

    // /M: working IP message
    private AlertDialog mReplaceDialog = null;

    // chat mode number
    private String mChatModeNumber = "";
    private boolean mIsSmsEnabled;

    // ipmessage status
    private boolean mIsIpMessageRecipients = false;

    private String mChatSenderName = "";

    private static final int REQUEST_CODE_IPMSG_TAKE_PHOTO = 200;
    private static final int REQUEST_CODE_IPMSG_RECORD_VIDEO = 201;
    private static final int REQUEST_CODE_IPMSG_SHARE_CONTACT = 203;
    private static final int REQUEST_CODE_IPMSG_CHOOSE_PHOTO = 204;
    private static final int REQUEST_CODE_IPMSG_CHOOSE_VIDEO = 205;
    private static final int REQUEST_CODE_IPMSG_RECORD_AUDIO = 206;
    private static final int REQUEST_CODE_IPMSG_CHOOSE_AUDIO = 208;
    private static final int REQUEST_CODE_IPMSG_SHARE_VCALENDAR = 209;

    // / M: IP message
    public static final int IPMSG_TAKE_PHOTO = 100;
    public static final int IPMSG_RECORD_VIDEO = 101;
    public static final int IPMSG_RECORD_AUDIO = 102;
    public static final int IPMSG_CHOOSE_PHOTO = 104;
    public static final int IPMSG_CHOOSE_VIDEO = 105;
    public static final int IPMSG_CHOOSE_AUDIO = 106;
    public static final int IPMSG_SHARE_CONTACT = 108;
    public static final int IPMSG_SHARE_CALENDAR = 109;
    public static final int IPMSG_SHARE_POSITION = 110;

    public static final int REQUEST_CODE_IPMSG_PICK_CONTACT = 210;
    public static final int REQUEST_CODE_INVITE_FRIENDS_TO_CHAT = 211;

    public static final int REQUEST_CODE_IPMSG_SHARE_FILE = 214;
    public static final int REQUEST_CODE_IP_MSG_PICK_CONTACTS = 215;

    public static final int REQUEST_CODE_DELETE_PARTICIPENT_FROM_CHAT = 500;
    public static final int REQUEST_CODE_IP_MSG_BURNED_MSG_AUDIO = 220;
    private static final int SMS_CONVERT = 0;
    private static final int MMS_CONVERT = 1;
    private static final int SERVICE_IS_NOT_ENABLED = 0;
    private static final int RECIPIENTS_ARE_NOT_IP_MESSAGE_USER = 1;
    // M: add logic for the current ipmessage user can not send ip message
    private static final int RECIPIENTS_IP_MESSAGE_NOT_SENDABLE = 2;

    // send or received ipMsg
    public static final int DIRECTION_INCOMING = 0;
    public static final int DIRECTION_OUTGOING = 1;

    // /M: for forward ipMsg
    public static final String FORWARD_IPMESSAGE = "forwarded_ip_message";
    public static final String IP_MESSAGE_ID = "ip_msg_id";
    public static final String CHOICE_FILEMANAGER_ACTION = "com.mediatek.filemanager.ADD_FILE";
    public static final String FILE_SCHEMA = "file://";
    public static final String SMS_BODY = "sms_body";

    private static final int RECIPIENTS_LIMIT_FOR_SMS = 50;

    // /M: for check whether or not can convert IpMessage to Common message.
    // /M: can not support translating media through common message , -1;

    public static final int ACTION_SHARE = 0;
    public static final String SHARE_ACTION = "shareAction";
    public static final String FORWARD_MESSAGE = "forwarded_message";
    public static final int RESULT_OK = -1;
    public static final int RESULT_CANCELLED = 0;
    private String mIpMessageVcardName = "";
    private ImageView mFullIntegratedView;
    private String mPhotoFilePath = "";

    private String mVideoFilePath = "";
    private String mAudioPath = "";

    private String mDstPath = "";
    private int mDuration = 0;
    private String mCalendarSummary = "";
    private EditText mTextEditor;
    private TextWatcher mEditorWatcher;
    private TextView mTextCounter;
    private Handler mUiHandler;

    private Activity mContext;
    private Context mPluginContext;
    private IpComposeActivityCallback mCallback;
    private IpWorkingMessageCallback mWorkingMessage;
    private boolean mDisplayBurned = false;
    private boolean mBurnedCapbility = false;
    private long mThreadId = 0;
    private String mOldcontact = null;
    private boolean mRegistCapbility = false;
    private boolean mIsGroupChat;
    private String mGroupChatId;
    private boolean mIsChatActive = true;
    private String[] mNumbers;
    private View mBottomPanel;
    private View mEditZone;
    private RCSEmoji mEmoji;
    private ListView mListView;
    private RcsMessageListAdapter mRcsMessageListAdapter;
    private final static int DIALOG_ID_GETLOC_PROGRESS = 101;
    private final static int DIALOG_ID_GEOLOC_SEND_CONFRIM = 102;

    private static Location mLocation = null;
    private GeoLocService mGeoLocSrv = null;
    private RCSGroup mGroup;
    private int mGroupStatus;
    private Dialog mInvitationDialog;
    private Dialog mInvitationExpiredDialog;
    private ProgressDialog mProgressDialog;
    private boolean mIsNeedShowCallTime;
    private ICallStatusService mCallStatusService;
    private static final String RCS_MODE_FLAG = "rcsmode";
    private static final int RCS_MODE_VALUE = 1;
    private ServiceConnection mCallConnection = null;

    final private int[] mUnusedOptionMenuForGroup = { MENU_ADD_SUBJECT, MENU_CHAT_SETTING,
            MENU_GROUP_PARTICIPANTS, MENU_SHOW_CONTACT, MENU_CREATE_CONTACT };

    private static RcsComposeActivity sComposer;
    // private boolean mIsRcsMode = true;
    private RCSServiceManager mServiceManager;
    private boolean mServiceEnabled = false;

    /**
     * Construction.
     * @param context Context
     */
    public RcsComposeActivity(Context context) {
        mPluginContext = context;
    }

    @Override
    public void onRequestBurnMessageCapabilityResult(String contact, boolean result) {

        String number = getNumber();
        Log.d(TAG, "onRequestBurnMessageCapabilityResult contact = " + contact + " result = "
                + result + " number = " + number);
        if (PhoneNumberUtils.compare(contact, number)) {
            Log.d(TAG,"onRequestBurnMessageCapabilityResult contact = number ");
            mBurnedCapbility = result;
            if (mDisplayBurned && !mBurnedCapbility) {
                checkBurnedMsgCapbility();
            }
            // mDisplayBurned = result;
        } else {
            mBurnedCapbility = false;
        }
        Log.d(TAG, "onRequestBurnMessageCapabilityResult mBurnedCapbility = "
                + mBurnedCapbility);
        return;
    }
    @Override
    public boolean onIpComposeActivityCreate(Activity context,
            IpComposeActivityCallback callback, Handler handler, Handler uiHandler,
            ImageButton sendButton, TextView typingTextView, TextView strangerTextView,
            View bottomPanel) {
        Log.d(TAG, "onIpComposeActivityCreate enter. sendButton = " + sendButton);
        mContext = context;
        mCallback = callback;
        mUiHandler = handler;
        mSendButtonIpMessage = sendButton;
        mTypingStatus = typingTextView;
        mShowCallTimeText = strangerTextView;
        mBottomPanel = bottomPanel;
        mEditZone = (View) mBottomPanel.getParent();
        mServiceManager = RCSServiceManager.getInstance();
        mServiceEnabled = mServiceManager.isServiceEnabled();
        mServiceManager.addOnServiceChangedListener(this);

        // / M: add for ip emoji
        if (mServiceManager.isServiceEnabled()) {
            mEmoji = new RCSEmoji(context, (ViewParent) mBottomPanel, mCallback);
        }

        Log.d(TAG, "mIsIpServiceEnabled =" + mIsIpServiceEnabled);
        mContext.registerReceiver(mChangeSubReceiver, new IntentFilter(
                TelephonyIntents.ACTION_DEFAULT_SMS_SUBSCRIPTION_CHANGED));
        sComposer = this;
        RCSCacheManager.clearCache();
        return false;
    }

    @Override
    public boolean onIpComposeActivityResume(boolean isSmsEnabled, EditText textEditor,
            TextWatcher watcher, TextView textCounter) {
        String[] numbers = mCallback.getConversationInfo();
        mIsSmsEnabled = isSmsEnabled;
        mTextEditor = textEditor;
        mEditorWatcher = watcher;
        mTextCounter = textCounter;

        if (!mRegistCapbility) {
            Log.d(TAG, "onRequestBurnMessageCapabilityResult onIpComposeActivityResume " +
            		"mRegistCapbility = " + mRegistCapbility);
            mServiceManager.registBurnMsgCapListener(this);
            mRegistCapbility = true;
        }
        if (numbers != null && numbers.length > 0 && numbers[0].length() > 0 || mIsGroupChat) {
            mCallback.enableShareButton(true);
        } else {
            mCallback.enableShareButton(false);
        }
        if (mServiceEnabled && mEmoji != null) {
            mEmoji.setEmojiEditor(textEditor);
        }

        if (mIsGroupChat) {
            mCallback.hideIpRecipientEditor();
            MapInfo info = ThreadMapCache.getInstance().getInfoByChatId(mGroupChatId);
            if (info != null
                    && info.getStatus() == GroupActionList.GROUP_STATUS_INVALID) {
                setChatActive(false);
            }
        } else if (mIsNeedShowCallTime) {
            mCallback.hideIpRecipientEditor();
            mCallback.updateIpTitle();
        }

        initDualSimState();
        return true;
    }

    @Override
    public boolean onIpComposeActivityPause() {
        Log.d(TAG, "onIpComposeActivityPause enter. mIsIpServiceEnabled = "
                + mIsIpServiceEnabled);
        // / M: add for ip message, notification listener
        if (!mIsGroupChat) {
            List<String> numbers = getRecipientsList();
            if (numbers != null && numbers.size() == 1) {
                PortraitManager.getInstance().invalidatePortrait(numbers.get(0));
            }
        } else {
            PortraitManager.getInstance().invalidateGroupPortrait(mGroupChatId);
        }
        return true;
    }

    @Override
    public boolean onIpComposeActivityDestroy() {
        Log.d(TAG, "onIpComposeActivityDestroy ");
        if (mIsGroupChat) {
            GroupManager.getInstance(mContext).removeGroupListener(mInitGroupListener);
            mGroup.removeActionListener(mGroupListener);
            mGroup.releaseGroup();
        }
        mContext.unregisterReceiver(mChangeSubReceiver);
        mRcsMessageListAdapter.removeOnMessageListChangedListener(mMsgListChangedListener);
        mServiceManager.removeOnServiceChangedListener(this);
        mServiceManager.unregistBurnMsgCapListener(this);
        mRegistCapbility = false;
        sComposer = null;
        return false;
    }

    @Override
    public boolean onIpMessageListItemHandler(int msg, long currentMsgId, long threadId,
            long subId) {
        Log.d(TAG, "onIpMessageListItemHandler msg =" + msg + " currentMsgId =" + currentMsgId);
        return super.onIpMessageListItemHandler(msg, currentMsgId, threadId, subId);
    }

    @Override
    public boolean onIpUpdateCounter(CharSequence text, int start, int before, int count) {
        Log.d(TAG, "onIpUpdateCounter text =" + text + " count =" + count);
        if (isRcsMode()) {
            return true;
        }
        return super.onIpUpdateCounter(text, start, before, count);
    }

    @Override
    public boolean onIpDeleteMessageListenerClick(long ipMessageId) {
        Log.d(TAG, "onIpDeleteMessageListenerClick ipMessageId =" + ipMessageId);
        return super.onIpDeleteMessageListenerClick(ipMessageId);
    }

    @Override
    public boolean onIpDiscardDraftListenerClick() {
        Log.d(TAG, "onIpDiscardDraftListenerClick");
        return super.onIpDiscardDraftListenerClick();
    }

    @Override
    public boolean onIpCreateContextMenu(ContextMenu menu, boolean isSmsEnabled,
            boolean isForwardEnabled, IpMessageItem ipMsgItem) {
        Log.d(TAG, "onIpCreateContextMenu ipMsgItem =" + ipMsgItem);
        Context pluginContext = mPluginContext;
        if (ipMsgItem == null) {
            return false;
        }
        RcsMessageItem rcseMsgItem = (RcsMessageItem) ipMsgItem;
        long ipMessageId = rcseMsgItem.mIpMessageId;
        long msgId = rcseMsgItem.mMsgId;
        RCSMessageManager msgManager = RCSMessageManager.getInstance(mContext);
        IpMessage ipMessage = msgManager.getIpMsgInfo(mThreadId, ipMessageId);
        RcsMsgListMenuClickListener l = new RcsMsgListMenuClickListener(ipMsgItem);
        if (isSmsEnabled && ipMessageId != 0) {
            if (ipMessage != null) {
                MenuItem item = null;
                if (mServiceEnabled) {
                    int ipStatus = ipMessage.getStatus();
                    if (ipStatus == IpMessageStatus.FAILED
                            || ipStatus == IpMessageStatus.NOT_DELIVERED) {
                        if (mIsChatActive) {
                            menu.add(0, MENU_RETRY, 0,
                                    pluginContext.getString(R.string.ipmsg_resend))
                                    .setOnMenuItemClickListener(l);
                        }
                    }
                    if (ipMessage.getDirection() == DIRECTION_OUTGOING) {
                        item = menu.findItem(MENU_DELIVERY_REPORT);
                        if (item != null) {
                            menu.removeItem(MENU_DELIVERY_REPORT);
                        }
                        menu.add(0, MENU_IPMSG_DELIVERY_REPORT, 0,
                                pluginContext.getString(R.string.ipmsg_delivery_report))
                                .setOnMenuItemClickListener(l);
                    }
                }
                item = menu.findItem(MENU_SAVE_MESSAGE_TO_SUB);
                if (item != null) {
                    menu.removeItem(MENU_SAVE_MESSAGE_TO_SUB);
                }

                if (ipMessage.getType() == IpMessageType.TEXT) {
                    item = menu.findItem(MENU_COPY);
                    if (item == null) {
                        menu.add(0, MENU_COPY, 0, pluginContext.getString(R.string.ipmsg_copy))
                                .setOnMenuItemClickListener(l);
                    }
                    if (!ipMessage.getBurnedMessage()) {
                        menu.add(0, MENU_FAVORITE, 0,
                                pluginContext.getString(R.string.menu_favorite))
                                .setOnMenuItemClickListener(l);
                    }
                } else if (ipMessage.getType() == IpMessageType.PICTURE
                        || ipMessage.getType() == IpMessageType.VIDEO
                        || ipMessage.getType() == IpMessageType.VOICE
                        || ipMessage.getType() == IpMessageType.VCARD
                        || ipMessage.getType() == IpMessageType.GEOLOC) {
                    if ((ipMessage.getStatus() == Sms.MESSAGE_TYPE_INBOX &&
                            ((IpAttachMessage) ipMessage).getRcsStatus() != Status.FINISHED)) {
                        Log.d(TAG, "this is a undownload FT");
                    } else {
                        if (!ipMessage.getBurnedMessage()) {
                            menu.add(0, MENU_EXPORT_SD_CARD, 0,
                                    pluginContext.getString(R.string.copy_to_sdcard))
                                    .setOnMenuItemClickListener(l);
                            menu.add(0, MENU_FAVORITE, 0,
                                    pluginContext.getString(R.string.menu_favorite))
                                    .setOnMenuItemClickListener(l);
                        }
                    }

                    item = menu.findItem(MENU_COPY);
                    if (item != null) {
                        item.setVisible(false);
                    }

                    item = menu.findItem(MENU_SELECT_TEXT);
                    if (item != null) {
                        item.setVisible(false);
                    }

                    item = menu.findItem(MENU_SAVE_MESSAGE_TO_SUB);
                    if (item != null) {
                        menu.removeItem(MENU_SAVE_MESSAGE_TO_SUB);
                    }

                    item = menu.findItem(MENU_ADD_TO_BOOKMARK);
                    if (item != null) {
                        menu.removeItem(MENU_ADD_TO_BOOKMARK);
                    }

                    item = menu.findItem(MENU_ADD_ADDRESS_TO_CONTACTS);
                    while (item != null) {
                        menu.removeItem(MENU_ADD_ADDRESS_TO_CONTACTS);
                        item = menu.findItem(MENU_ADD_ADDRESS_TO_CONTACTS);
                    }

                } else {
                    // TODO: not text
                    item = menu.findItem(MENU_COPY);
                    if (item != null) {
                        item.setVisible(false);
                    }

                    item = menu.findItem(MENU_SELECT_TEXT);
                    if (item != null) {
                        item.setVisible(false);
                    }
                }
                // hide the report menu ,because cmcc requirement is not clear.
                // menu.add(0, MENU_REPORT, 0,
                // pluginContext.getString(R.string.ipmsg_report))
                // .setOnMenuItemClickListener(l);

            }
        }

        if ("mms".equals(rcseMsgItem.mType)) {
            if (rcseMsgItem.mBoxId == Mms.MESSAGE_BOX_INBOX
                    && rcseMsgItem.mMessageType == MESSAGE_TYPE_NOTIFICATION_IND) {
                Log.d(TAG, "this is a mms,rcseMsgItem.mBoxId =" + rcseMsgItem.mBoxId);
            } else {
                menu.add(0, MENU_FAVORITE, 0, pluginContext.getString(R.string.menu_favorite))
                        .setOnMenuItemClickListener(l);
            }
        } else {
            if (ipMessageId == 0) {
                menu.add(0, MENU_FAVORITE, 0, pluginContext.getString(R.string.menu_favorite))
                        .setOnMenuItemClickListener(l);
            }
        }

        if ("mms".equals(rcseMsgItem.mType)) {
            Log.d(TAG, "rcseMsgItem.mBoxId =" + rcseMsgItem.mBoxId);
        }
        int forwardMenuId = forwardMsgHandler(rcseMsgItem);
        if (MENU_FORWARD_IPMESSAGE == forwardMenuId) {
            MenuItem item = menu.findItem(MENU_FORWARD_MESSAGE);
            if (item != null) {
                menu.removeItem(MENU_FORWARD_MESSAGE);
            }
            menu.add(0, MENU_FORWARD_IPMESSAGE, 0,
                    pluginContext.getString(R.string.ipmsg_forward))
                    .setOnMenuItemClickListener(l);
        } else if (0 == forwardMenuId) {
            MenuItem item = null;
            item = menu.findItem(MENU_FORWARD_MESSAGE);
            if (item != null) {
                menu.removeItem(MENU_FORWARD_MESSAGE);
            }

            item = menu.findItem(MENU_FORWARD_IPMESSAGE);
            if (item != null) {
                menu.removeItem(MENU_FORWARD_IPMESSAGE);
            }
        }
        if (ipMessage != null && ipMessage.getBurnedMessage()) {
            MenuItem itemBurned = menu.findItem(MENU_COPY);
            if (itemBurned != null) {
                menu.removeItem(MENU_COPY);
            }

            itemBurned = menu.findItem(MENU_SELECT_TEXT);
            if (itemBurned != null) {
                menu.removeItem(MENU_SELECT_TEXT);
            }

            itemBurned = menu.findItem(MENU_FORWARD_IPMESSAGE);
            if (itemBurned != null) {
                menu.removeItem(MENU_FORWARD_IPMESSAGE);
            }
            itemBurned = menu.findItem(MENU_FORWARD_MESSAGE);
            if (itemBurned != null) {
                menu.removeItem(MENU_FORWARD_MESSAGE);
            }
            itemBurned = menu.findItem(MENU_IPMSG_DELIVERY_REPORT);
            if (itemBurned != null) {
                menu.removeItem(MENU_IPMSG_DELIVERY_REPORT);
            }
            itemBurned = menu.findItem(MENU_REPORT);
            if (itemBurned != null) {
                menu.removeItem(MENU_REPORT);
            }
        }
        return true;
    }

    private int forwardMsgHandler(RcsMessageItem rcseMsgItem) {
        Intent sendIntent;
        long ipMessageId = rcseMsgItem.mIpMessageId;
        IpMessage ipMessage = RCSMessageManager.getInstance(mContext).getIpMsgInfo(mThreadId,
                ipMessageId);
        Log.d(TAG, "forwardMsgHandler()  mType =" + rcseMsgItem.mType + " mIpMessageId = "
                + rcseMsgItem.mIpMessageId);
        if ("sms".equals(rcseMsgItem.mType)) {
            if (rcseMsgItem.mIpMessageId == 0) {
                // sms forward
                sendIntent = RcsMessageUtils.createForwordIntentFromSms(mContext,
                        rcseMsgItem.mBody);
                if (sendIntent != null) {
                    return MENU_FORWARD_IPMESSAGE;
                } else {
                    return MENU_FORWARD_MESSAGE;
                }
            } else {
                // ip message forward
                if (ipMessage == null) {
                    return 0;
                }
                sendIntent = RcsMessageUtils.createForwordIntentFromIpmessage(mContext, ipMessage);
                Log.d(TAG,
                        "forwardMsgHandler()  sendIntent is null   getType = "
                                + ipMessage.getType());
                if ((ipMessage.getType() == IpMessageType.TEXT)) {
                    if (sendIntent != null) {
                        Log.d(TAG, "forwardMsgHandler()  sendIntent != null");
                        return MENU_FORWARD_IPMESSAGE;
                    } else {
                        return MENU_FORWARD_MESSAGE;
                    }
                }

                if ((ipMessage.getType() == IpMessageType.PICTURE
                        || ipMessage.getType() == IpMessageType.VIDEO
                        || ipMessage.getType() == IpMessageType.VOICE
                        || ipMessage.getType() == IpMessageType.VCARD
                        || ipMessage.getType() == IpMessageType.GEOLOC)) {
                    Log.d(TAG, "forwardMsgHandler()  sendIntent is null   getStatus = "
                            + ipMessage.getStatus() + " getRcsStatus() = "
                            + ((IpAttachMessage) ipMessage).getRcsStatus());

                    if ((ipMessage.getStatus() == Sms.MESSAGE_TYPE_INBOX &&
                            ((IpAttachMessage) ipMessage).getRcsStatus() != Status.FINISHED)) {
                        return 0;
                    } else {
                        if (!ipMessage.getBurnedMessage()) {
                            return MENU_FORWARD_IPMESSAGE;
                        }
                        return 0;
                    }
                }
            }
        } else if ("mms".equals(rcseMsgItem.mType)) {
            // mms forward
            Uri realUri = ContentUris.withAppendedId(Mms.CONTENT_URI, rcseMsgItem.mMsgId);
            sendIntent = RcsMessageUtils.createForwardIntentFromMms(mContext, realUri);
            if (sendIntent != null) {
                return MENU_FORWARD_IPMESSAGE;
            } else {
                return MENU_FORWARD_MESSAGE;
            }
        }
        return MENU_FORWARD_IPMESSAGE;
    }

    /**
     * private class RcsMsgListMenuClickListener.
     *
     */
    private final class RcsMsgListMenuClickListener implements MenuItem.OnMenuItemClickListener {
        private long mIpMessageId;
        private long mMsgId;
        private IpMessageItem mIpMsgItem;

        public RcsMsgListMenuClickListener(IpMessageItem ipMsgItem) {
            mIpMsgItem = ipMsgItem;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            return onIpMenuItemClick(item, mIpMsgItem);
        }
    }

    @Override
    public boolean onIpMenuItemClick(MenuItem item, IpMessageItem ipMsgItem) {
        Log.d(TAG, "onIpMenuItemClick ipMsgItem =" + ipMsgItem);
        if (ipMsgItem != null && onIpMessageMenuItemClick(item, (RcsMessageItem) ipMsgItem)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onIpUpdateTitle(String number, String titleOriginal,
            ImageView ipCustomView, ArrayList<String> titles) {
        Log.d(TAG, "onIpUpdateTitle number =" + number);
        /*TODO should use RcsGroup better.
        if (mGroup != null) {
            String groupName = mGroup.getGroupNickName();
            if (TextUtils.isEmpty(groupName)) {
                groupName = mGroup.getSubject();
            }
            titles.add(groupName);
            titles.add("");
            return true;
        }
        */
        if (mIsGroupChat) {
            MapInfo info = ThreadMapCache.getInstance().getInfoByChatId(mGroupChatId);
            if (info != null) {
                String groupName = info.getNickName();
                if (TextUtils.isEmpty(groupName)) {
                    groupName = info.getSubject();
                }
                titles.add(groupName);
                titles.add("");
                return true;
            }
        }
        return super.onIpUpdateTitle(number, titleOriginal, ipCustomView, titles);
    }

    @Override
    public boolean onIpTextChanged(CharSequence s, int start, int before, int count) {
        Log.d(TAG, "onIpTextChanged s=" + s + ", start=" + start + ", before=" + before
                + ", count=" + count);
        // delete
        if (before > 0) {
            return true;
        }
        if (!TextUtils.isEmpty(s.toString())) {
            if (mServiceEnabled && mEmoji != null && mTextEditor != null) {
                float textSize = mTextEditor.getTextSize();
                EmojiImpl emojiImpl = EmojiImpl.getInstance(mPluginContext);
                CharSequence str = emojiImpl.getEmojiExpression(s, true, start, count,
                        (int) textSize);
                mTextEditor.removeTextChangedListener(mEditorWatcher);
                mTextEditor.setTextKeepState(str);
                mTextEditor.addTextChangedListener(mEditorWatcher);
            }
        }
        return true;
    }

    private void setEmojiActive(final boolean active) {
        if (mEmoji == null) {
            return;
        }
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!active) {
                    // hide emoticon panel, hide emoticon button
                    if (mEmoji.isEmojiPanelShow()) {
                        mEmoji.showEmojiPanel(false);
                    }
                    mEmoji.setEmojiButtonVisible(false);
                    mEmoji.setEmojiEditor(null);
                } else {
                    // show emoticon button
                    mEmoji.setEmojiButtonVisible(true);
                    mEmoji.setEmojiEditor(mTextEditor);
                }
            }
        });
    }

    @Override
    public boolean onIpRecipientsEditorFocusChange(boolean hasFocus, List<String> numbers) {
        Log.d(TAG, "onIpRecipientsEditorFocusChange hasFocus =" + hasFocus);
        return super.onIpRecipientsEditorFocusChange(hasFocus, numbers);
    }

    @Override
    public boolean onIpInitialize(Intent intent, IpWorkingMessageCallback workingMessageCallback) {
        mWorkingMessage = workingMessageCallback;
        long threadId = intent.getLongExtra("thread_id", 0);
        mIsNeedShowCallTime = false;
        if (threadId != 0) {
            mThreadId = threadId;
        } else {
            Uri uri = intent.getData();
            if (uri != null && uri.toString().startsWith("content://mms-sms/conversations/")) {
                String threadIdStr = uri.getPathSegments().get(1);
                threadIdStr = threadIdStr.replaceAll("-", "");
                mThreadId = Long.parseLong(threadIdStr);
            }
        }
        if (intent.getIntExtra(RCS_MODE_FLAG, 0) == RCS_MODE_VALUE) {
            mIsNeedShowCallTime = true;
        }
        if (mIsGroupChat) {
            GroupManager.getInstance(mContext).removeGroupListener(mInitGroupListener);
            dismissInvitationDialog();
            dismissInvitationTimeOutDialog();
            mGroup.removeActionListener(mGroupListener);
        }

        if (mIsNeedShowCallTime) {
            setupCallStatusServiceConnection();
        }
        mIsGroupChat = false;
        String chatId = intent.getStringExtra("chat_id");
        if (!TextUtils.isEmpty(chatId)) {
            mIsGroupChat = true;
            mGroupChatId = chatId;
        } else {
            MapInfo info = ThreadMapCache.getInstance().getInfoByThreadId(mThreadId);
            if (info != null) {
                mIsGroupChat = true;
                mGroupChatId = info.getChatId();
            }
        }
        if (mIsGroupChat) {
            mCallback.hideIpRecipientEditor();
            final GroupManager manager = GroupManager.getInstance(mContext);
            manager.addGroupListener(mInitGroupListener);
            mGroup = manager.getRCSGroup(mGroupChatId);
            mGroup.addActionListener(mGroupListener);
            MapInfo info = ThreadMapCache.getInstance().getInfoByChatId(mGroupChatId);
            int status = (int)info.getStatus();
            if (status == GroupActionList.GROUP_STATUS_INVITING
                    || status == GroupActionList.GROUP_STATUS_INVITING_AGAIN) {
                 RcsMessagingNotification.cancelNewGroupInviations(mContext);
                 showInvitaionDialog(status);
                 setChatActive(false);
            }else if (status == GroupActionList.GROUP_STATUS_INVALID) {
                setChatActive(false);
            } else {
                 setChatActive(mServiceEnabled);
            }
            PortraitManager.getInstance().initGroupChatPortrait(mGroupChatId);
            RcsConversation conversation = (RcsConversation) mCallback.getIpConversation();
            conversation.setGroupChatId(chatId);
        }
        Log.d(TAG, "mThreadID = " + mThreadId + ", mGroupChatID = " + mGroupChatId);
        mCallback.resetSharePanel();
        return true;
    }

    @Override
    public boolean onIpSaveInstanceState(Bundle outState, long threadId) {
        Log.d(TAG, "onIpSaveInstanceState threadId =" + threadId);
        return super.onIpSaveInstanceState(outState, threadId);
    }

    @Override
    public boolean onIpShowSmsOrMmsSendButton(boolean isMms) {
        Log.d(TAG, "onIpShowSmsOrMmsSendButton, isMms :" + isMms + "  mDisplayBurned = "
                + mDisplayBurned);
        return mDisplayBurned;
    }

    @Override
    public boolean onIpPrepareOptionsMenu(IpConversation ipConv, Menu menu) {
        Log.d(TAG, "onIpPrepareOptionsMenu, menu :" + menu);
        // / M: whether has IPMsg APK or not. ture: has ; false: no;
        Context pluginContext = mPluginContext;

        // / M: true: the host has been activated.
        boolean hasActivatedHost = RcsMessageConfig.isActivated();
        Log.d(TAG, "onRequestBurnMessageCapabilityResult mOldcontact = "
                + mOldcontact);
        Log.d(TAG, "onRequestBurnMessageCapabilityResult getNumber() = "
                + getNumber());
        boolean isServiceAvalible = RCSServiceManager.getInstance().isServiceActivated();
        if (isServiceAvalible && getNumber() != null && 
                !(getNumber().equals(mOldcontact))) {
            // getContactCapbility();
            if (1 == getRecipientSize()) {
                Log.d(TAG,"onIpPrepareOptionsMenu onRequestBurnMessageCapabilityResult" +
                	" getNumber() = " + getNumber() + " mBurnedCapbility = "
                    + mBurnedCapbility + "  mRegistCapbility = " + mRegistCapbility);
                if (!mRegistCapbility) {
                    mServiceManager.registBurnMsgCapListener(this);
                    mRegistCapbility = true;
                }
                mBurnedCapbility = false;
                mServiceManager.getBurnMsgCap(getNumber());
                mOldcontact = getNumber();
            } else {
                mBurnedCapbility = false;
                Log.d(TAG,
                        "onIpPrepareOptionsMenu onRequestBurnMessageCapabilityResult "
                                + "mBurnedCapbility = " + mBurnedCapbility);
            }

        }

        Log.d(TAG, "[addGroupMenuOptions] group info");
        if (mIsGroupChat) {
            if (mIsChatActive) {
                MenuItem groupInfoItem = menu.add(0, MENU_GROUP_CHAT_INFO, 0,
                        pluginContext.getText(R.string.menu_group_info));
                groupInfoItem.setIcon(pluginContext.getResources().getDrawable(
                        R.drawable.ic_add_chat_holo_dark));
                groupInfoItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                // disable unused menu
                for (int id : mUnusedOptionMenuForGroup) {
                    MenuItem item = menu.findItem(id);
                    if (item != null) {
                        item.setVisible(false);
                    }
                }
            } else {
                // only can select menu
                int size = menu.size();
                for (int index = 0; index < size; index++) {
                    MenuItem item = menu.getItem(index);
                    if (item.getItemId() != MENU_SELECT_MESSAGE) {
                        item.setVisible(false);
                    }
                }
            }
        } else {

            if (1 == getRecipientSize() && !mIsGroupChat) {
                if (isRcsMode()) {
                    if (mIsSmsEnabled && !isRecipientsEditorVisible()) {
                        MenuItem item = menu.findItem(MENU_SHOW_CONTACT);
                        if (item != null) {
                            // if show cantact means the catact exist in db
                            menu.add(0, MENU_INVITE_FRIENDS_TO_CHAT, 0, pluginContext
                                    .getString(R.string.menu_invite_friends_to_chat));
                        }
                    }
                    if (mDisplayBurned) {
                        menu.add(0, MENU_EDIT_BURNED_MSG, 0,
                                pluginContext.getString(R.string.menu_cacel_burned_msg));
                    } else {
                        menu.add(0, MENU_EDIT_BURNED_MSG, 0,
                                pluginContext.getString(R.string.menu_burned_msg));
                    }
                    String number = getNumber();
                    boolean isInBlackList = RCSUtils.isIpSpamMessage(mContext, number);
                    if (isInBlackList) {
                        menu.add(0, MENU_BLACK_LIST, 0,
                                pluginContext.getString(R.string.menu_remove_black_list));
                    } else {
                        menu.add(0, MENU_BLACK_LIST, 0,
                                pluginContext.getString(R.string.menu_add_black_list));
                    }
                }
            }
        }
        // not allow to edit mms
        MenuItem item = menu.findItem(MENU_ADD_SUBJECT);
        if (item != null) {
            if (isRcsMode()) {
                item.setVisible(false);
            } else {
                item.setVisible(true);
            }
        }
        return true;
    }

    @Override
    public void onIpMsgActivityResult(Context context, int requestCode, int resultCode,
            Intent data) {
        Log.d(TAG, "[BurnedMsg] onIpMsgActivityResult(), ++++requestCode :" + requestCode);
        boolean isServiceAvalible = RCSServiceManager.getInstance().isServiceActivated();
        Log.d(TAG,"onIpMsgActivityResult, isServiceAvalible = " + isServiceAvalible);

        if (resultCode != RESULT_OK) {
            Log.d(TAG, "bail due to resultCode=" + resultCode);
            if (resultCode == RESULT_CANCELLED
                    && requestCode == REQUEST_CODE_IP_MSG_BURNED_MSG_AUDIO) {
                Log.d(TAG, " [BurnedMsg]: onIpMsgActivityResult() resultCode = " + resultCode);
            } else {
                return;
            }
        }

        boolean isSendOrgPic = RcsSettingsActivity.getSendMSGStatus(mPluginContext);

        // / M: add for ip message
        switch (requestCode) {
        case REQUEST_CODE_INVITE_FRIENDS_TO_CHAT:
            Intent intent = new Intent(CreateGroupActivity.ACTIVITY_ACTION);
            ArrayList<String> allList = new ArrayList<String>();
            String[] numbers = mCallback.getConversationInfo();
            if (numbers != null && numbers.length > 0) {
                for (String number : numbers) {
                    allList.add(number);
                    Log.v(TAG, "now chat contact number: " + number);
                }
            }
            if (allList != null && allList.size() > 0) {
                intent.putStringArrayListExtra(
                        CreateGroupActivity.TAG_CREATE_GROUP_BY_NUMBERS, allList);
            }
            long[] ids = data.getLongArrayExtra("com.mediatek.contacts.list.pickdataresult");
            final long[] contactsId = data
                    .getLongArrayExtra("com.mediatek.contacts.list.pickdataresult");
            if (contactsId != null && contactsId.length > 0) {
                intent.putExtra(CreateGroupActivity.TAG_CREATE_GROUP_BY_IDS, contactsId);
            }
            mContext.startActivity(intent);

            return;
        case REQUEST_CODE_IP_MSG_PICK_CONTACTS:
            long threadId = mCallback.genIpThreadIdFromContacts(data);
            if (threadId <= 0) {
                Log.d(TAG, "[onIpMsgActivityResult] return thread id <= 0");
                break;
            }

            Intent it = createIntent(mContext.getApplicationContext(), threadId);
            mContext.startActivity(it);
            break;
        case REQUEST_CODE_IPMSG_TAKE_PHOTO:
            if (!isServiceAvalible) {
                Toast.makeText(mPluginContext, R.string.cannot_send_filetransfer, Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            if (!RcsMessageUtils.isValidAttach(mDstPath, false)) {
                Toast.makeText(mPluginContext, R.string.ipmsg_err_file, Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            if (!RcsMessageUtils.isPic(mDstPath)) {
                Toast.makeText(mPluginContext, R.string.ipmsg_invalid_file_type,

                Toast.LENGTH_SHORT).show();
                return;
            }
            long photoSize = getFileSize(mDstPath);

            Log.d(TAG, " isSendOrgPic = " + isSendOrgPic);
            if (RcsMessageUtils.isGif(mDstPath)) {
                Log.d(TAG, " it is a gif");
                RcsMessageUtils.copyFileToDst(mPhotoFilePath, mDstPath);
                sendImage(requestCode);
            } else if (photoSize < getRcsFileMaxSize() && isSendOrgPic) {
                Log.d(TAG, " photoSize < RCSMaxSize and is org pic sent, photoSize = "
                        + photoSize);
                RcsMessageUtils.copyFileToDst(mPhotoFilePath, mDstPath);
                sendImage(requestCode);
            } else if (photoSize > getRcsFileMaxSize() && isSendOrgPic) {
                // exceed max size and don't org pic sent
                Log.d(TAG, " photoSize > RCSMaxSize and is org pic sent, photoSize = "
                        + photoSize);
                showSendPicAlertDialog();
            } else {
                // RcsMessageUtils.copyFileToDst(mPhotoFilePath,mDstPath);
                // sendImage(requestCode);
                Log.d(TAG, " It is NOT org pic sent ");
                new Thread(mResizePic, "ipmessage_resize_pic").start();
            }
            return;

        case REQUEST_CODE_IPMSG_RECORD_VIDEO:
            if (!isServiceAvalible) {
                Toast.makeText(mPluginContext, R.string.cannot_send_filetransfer, Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            if (!getMediaMsgInfo(data, requestCode)) {
                Toast.makeText(mPluginContext, R.string.ipmsg_err_file, Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            if (!RcsMessageUtils.isVideo(mDstPath)) {
                Toast.makeText(mPluginContext, R.string.ipmsg_invalid_file_type,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (!RcsMessageUtils.isFileStatusOk(mPluginContext, mDstPath)) {
                Log.e(TAG, "onIpMsgActivityResult(): record video failed, invalid file");
                return;
            }

            mIpMsgHandler.postDelayed(mSendVideo, 100);
            return;

        case REQUEST_CODE_IPMSG_SHARE_CONTACT:
            if (!isServiceAvalible) {
                Toast.makeText(mPluginContext, R.string.cannot_send_filetransfer, Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            asyncIpAttachVCardByContactsId(data);
            return;

        case REQUEST_CODE_IPMSG_CHOOSE_PHOTO:
            if (!isServiceAvalible) {
                Toast.makeText(mPluginContext, R.string.cannot_send_filetransfer, Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            if (!getMediaMsgInfo(data, requestCode)) {
                Toast.makeText(mPluginContext, R.string.ipmsg_err_file, Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            if (!RcsMessageUtils.isPic(mDstPath)) {
                Toast.makeText(mPluginContext, R.string.ipmsg_invalid_file_type,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            mPhotoFilePath = mDstPath;
            Log.e(TAG, " send image old filename = " + mDstPath);
            mDstPath = RcsMessageUtils.getPhotoDstFilePath(mDstPath, mContext);
            Log.e(TAG, " send image new filename = " + mDstPath);
            photoSize = getFileSize(mPhotoFilePath);

            // Context ctx = ContextCacher.getPluginContext();
            // String dir = ctx.getDir();
            Log.d(TAG, " isSendOrgPic = " + isSendOrgPic);
            if (RcsMessageUtils.isGif(mDstPath)) {
                Log.d(TAG, " it is a gif");
                RcsMessageUtils.copyFileToDst(mPhotoFilePath, mDstPath);
                sendImage(requestCode);
            } else if (photoSize < getRcsFileMaxSize() && isSendOrgPic) {
                Log.d(TAG, " photoSize < RCSMaxSize and is org pic sent, photoSize = "
                        + photoSize);
                RcsMessageUtils.copyFileToDst(mPhotoFilePath, mDstPath);
                sendImage(requestCode);
            } else if (photoSize > getRcsFileMaxSize() && isSendOrgPic) {
                // exceed max size and don't org pic sent
                Log.d(TAG, " photoSize > RCSMaxSize and is org pic sent, photoSize = "
                        + photoSize);
                showSendPicAlertDialog();
            } else {
                // RcsMessageUtils.copyFileToDst(mPhotoFilePath,mDstPath);
                // sendImage(requestCode);
                Log.d(TAG, " It is NOT org pic sent ");
                new Thread(mResizePic, "ipmessage_resize_pic").start();
            }
            return;

        case REQUEST_CODE_IPMSG_CHOOSE_VIDEO:
            if (!isServiceAvalible) {
                Toast.makeText(mPluginContext, R.string.cannot_send_filetransfer, Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            if (!getMediaMsgInfo(data, requestCode)) {
                if (getFileSize(mDstPath) > getRcsFileMaxSize()) {
                    Toast.makeText(mPluginContext, R.string.ipmsg_over_file_limit,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mPluginContext, R.string.ipmsg_err_file, Toast.LENGTH_SHORT)
                            .show();
                }
                return;
            }
            if (!RcsMessageUtils.isVideo(mDstPath)) {
                Toast.makeText(mPluginContext, R.string.ipmsg_invalid_file_type,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (!RcsMessageUtils.isFileStatusOk(mPluginContext, mDstPath)) {
                Log.e(TAG, "onIpMsgActivityResult(): choose video failed, invalid file");
                return;
            }
            mVideoFilePath = mDstPath;
            Log.e(TAG, " send video old filename = " + mDstPath);
            mDstPath = RcsMessageUtils.getVideoDstFilePath(mVideoFilePath, mContext);
            Log.e(TAG, " send video new filename = " + mDstPath);
            RcsMessageUtils.copyFileToDst(mVideoFilePath, mDstPath);
            mIpMsgHandler.postDelayed(mSendVideo, 100);
            return;

        case REQUEST_CODE_IPMSG_RECORD_AUDIO:
            if (!isServiceAvalible) {
                Toast.makeText(mPluginContext, R.string.cannot_send_filetransfer, Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            if (!getMediaMsgInfo(data, requestCode)) {
                return;
            }
            if (!RcsMessageUtils.isAudio(mDstPath)) {
                Toast.makeText(mPluginContext, R.string.ipmsg_invalid_file_type,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (!RcsMessageUtils.isFileStatusOk(mPluginContext, mDstPath)) {
                Log.e(TAG, "onIpMsgActivityResult(): record audio failed, invalid file");
                return;
            }
            mAudioPath = mDstPath;
            mDstPath = RcsMessageUtils.getAudioDstPath(mAudioPath, mContext);

            RcsMessageUtils.copyFileToDst(mAudioPath, mDstPath);
            mIpMsgHandler.postDelayed(mSendAudio, 100);
            return;
        case REQUEST_CODE_IPMSG_CHOOSE_AUDIO:
            if (!isServiceAvalible) {
                Toast.makeText(mPluginContext, R.string.cannot_send_filetransfer, Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            if (data != null) {
                Uri uri = (Uri) data
                        .getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                if (Settings.System.getUriFor(Settings.System.RINGTONE).equals(uri)) {
                    return;
                }
                if (getAudio(data)) {
                    mAudioPath = mDstPath;
                    mDstPath = RcsMessageUtils.getAudioDstPath(mAudioPath, mContext);
                    RcsMessageUtils.copyFileToDst(mAudioPath, mDstPath);
                    mIpMsgHandler.postDelayed(mSendAudio, 100);
                }
            }
            return;
        case REQUEST_CODE_IP_MSG_BURNED_MSG_AUDIO:
            Log.d(TAG, " [BurnedMsg]: bindIpmsg: change data ");
            mCallback.notifyIpDataSetChanged();
            return;
        default:
            break;
        }
    }

    private boolean checkBurnedMsgCapbility() {
        Log.d(TAG," onRequestBurnMessageCapabilityResult checkBurnedMsgCapbility() " +
        		"mBurnedCapbility = " + mBurnedCapbility
                + "  mDisplayBurned = "+ mDisplayBurned);
        if (mDisplayBurned && !mBurnedCapbility) {

            mDisplayBurned = false;
            mSendButtonIpMessage.setImageDrawable(mPluginContext.getResources()
                    .getDrawable(R.drawable.ic_send_ipbar));

            Toast.makeText(mPluginContext, R.string.ipmsg_burn_cap,
                    Toast.LENGTH_SHORT).show();
            RcsMessageConfig.setEditingDisplayBurnedMsg(mDisplayBurned);
            mCallback.resetSharePanel();
            return false;
        }
        return true;
    }

    private void showSendPicAlertDialog() {
        Resources res = mPluginContext.getResources();
        AlertDialog.Builder b = new AlertDialog.Builder(mContext);
        b.setTitle("Picture is exceed maxsize").setMessage("If resize the pic and send ?");
        b.setCancelable(true);
        b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public final void onClick(DialogInterface dialog, int which) {
                new Thread(mResizePic, "ipmessage_resize_pic").start();
            }
        });

        b.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public final void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        b.create().show();
    }

    @Override
    public boolean onIpHandleForwardedMessage(Intent intent) {
        Log.d(TAG, "onIpHandleForwardedMessage, intent :" + intent);
        return super.onIpHandleForwardedMessage(intent);
    }

    @Override
    public boolean onIpInitMessageList(ListView list, IpMessageListAdapter adapter) {
        Log.d(TAG, "onIpInitMessageList, mIsIpMessageRecipients :" + mIsIpMessageRecipients);
        mListView = list;
        mRcsMessageListAdapter = (RcsMessageListAdapter) adapter;
        mRcsMessageListAdapter.addOnMessageListChangedListener(mMsgListChangedListener);
        return super.onIpInitMessageList(list, adapter);
    }

    @Override
    public boolean onIpSaveDraft(long threadId) {
        Log.d(TAG, "onIpSaveDraft, threadId =" + threadId);
        if (mIsGroupChat) {
            final String body = mTextEditor.getText().toString();
            if (mThreadId <= 0 && !TextUtils.isEmpty(body)) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        mCallback.guaranteeIpThreadId();
                        mThreadId = mCallback.getCurrentThreadId();
                        ContentValues values = new ContentValues(3);
                        values.put(Sms.THREAD_ID, mThreadId);
                        values.put(Sms.BODY, body);
                        values.put(Sms.TYPE, Sms.MESSAGE_TYPE_DRAFT);
                        mContext.getContentResolver().insert(Sms.CONTENT_URI, values);
                    }
                }, "onIpSaveDraft").run();
                return true;
            }
        }
        return super.onIpSaveDraft(threadId);
    }

    @Override
    public boolean onIpResetMessage() {
        Log.d(TAG, "onIpResetMessage");
        return super.onIpResetMessage();
    }

    @Override
    public boolean onIpUpdateTextEditorHint() {
        Log.d(TAG, "onIpUpdateTextEditorHint, mIsIpServiceEnabled :" + mIsIpServiceEnabled);
        return super.onIpUpdateTextEditorHint();
    }

    public Handler mIpMsgHandler = new Handler() {
        public void handleMessage(Message msg) {
            Log.d(TAG, "mIpMsgHandler handleMessage, msg.what: " + msg.what);
            switch (msg.what) {
            case ACTION_SHARE:
                if (RcsMessageConfig.isServiceEnabled(mContext)
                        && isNetworkConnected(mContext)) {
                    doMoreAction(msg);
                }
                break;
            default:
                Log.d(TAG, "msg type: " + msg.what + "not handler");
                break;
            }
            super.handleMessage(msg);
        }
    };
    @Override
    public boolean loadIpMessagDraft(long threadId) {
        Log.d(TAG, "loadIpMessagDraft() threadId = " + threadId);
        return super.loadIpMessagDraft(threadId);
    }

    @Override
    public boolean checkIpMessageBeforeSendMessage(long subId, boolean bCheckEcmMode) {
        Log.d(TAG, "checkIpMessageBeforeSendMessage()");
        if (RcsMessageUtils.getConfigStatus()) {
            int mainCardSubId = SubscriptionManager.getDefaultDataSubId();
            if (!SubscriptionManager.isValidSubscriptionId(mainCardSubId)
                    || (int) subId != mainCardSubId) {
                if (!mIsGroupChat) {
                    return false;
                }
            }
        } else {
            if (!mIsGroupChat) {
                return false; // not Config
            } else {
                return true;
            }
        }
        Log.d(TAG, "checkIpMessageBeforeSendMessage() conitnue send");
        mWorkingMessage = mCallback.getWorkingMessage();

        // if the message is mms, need use mms send
        boolean isMms = mWorkingMessage.requiresIpMms();
        if (isMms) {
            if (mIsGroupChat) {
                throw new RuntimeException("can not send mms in group chat");
            }
            return false;
        }

        if (!mServiceEnabled) {
            // not Activated or not configuration successful
            return false;
        }

        boolean serviceReady = mServiceManager.serviceIsReady();
        if (isNeedShowSendWayChangedDialog(serviceReady, (int) subId)) {
            showSendWayChanged(serviceReady, (int) subId);
            return true;
        } else {
            int sendMode = getLastSendMode();
            if (sendMode == PREFERENCE_VALUE_SEND_WAY_SMS) {
                // TODO if last send mode is sms, always by sms infuture if not indicate?
                return false;
            }
        }
        if(!checkBurnedMsgCapbility()) {
            return true;
        }
         String body = mTextEditor.getText().toString();
         if (body != null && body.length() > 0) {
             sendIpTextMessage();
             return true;
         }
         return false;
    }

    private boolean onIpMessageMenuItemClick(MenuItem menuItem, RcsMessageItem rcseMsgItem) {
        long ipMessageId = rcseMsgItem.mIpMessageId;
        long msgId = rcseMsgItem.mMsgId;

        Log.d(TAG, "onIpMessageMenuItemClick(): ");
        switch (menuItem.getItemId()) {
        case MENU_RETRY:
            IpMessage iPMsg = RCSMessageManager.getInstance(mContext)
                    .getIpMsgInfo(ipMessageId);
            if (iPMsg.getType() == IpMessageType.TEXT) {
                RCSMessageManager.getInstance(mContext).resendMessage(msgId, mThreadId);
            } else {
                RCSMessageManager.getInstance(mContext).reSendFileTransfer(ipMessageId,
                        mThreadId);
            }
            return true;

        case MENU_FORWARD_IPMESSAGE:
            Log.d(TAG, "MENU_FORWARD_IPMESSAGE");
            hideInputMethod();
            forwardIpMsg(mContext, rcseMsgItem);
            return true;

        case MENU_EXPORT_SD_CARD:
            IpMessage ipMessageForSave = RCSMessageManager.getInstance(mContext).getIpMsgInfo(
                    mThreadId, ipMessageId);
            Log.d(TAG, "onIpMessageMenuItemClick(): Save IP message. msgId = " + ipMessageId);
            copyFile((IpAttachMessage) ipMessageForSave);
            return true;

        case MENU_FAVORITE:
            if ("sms".equals(rcseMsgItem.mType)) {
                String address = null;
                if (rcseMsgItem.mBoxId == 1) {
                    address = rcseMsgItem.mAddress;
                }
                if (ipMessageId != 0) {
                    IpMessage ipMessageForFavorite = RCSMessageManager.getInstance(mContext)
                            .getIpMsgInfo(mThreadId, ipMessageId);
                    if (ipMessageId < 0) {
                        IpAttachMessage attachMessage = (IpAttachMessage) ipMessageForFavorite;
                        setAttachIpmessageFavorite(msgId, attachMessage.getType(),
                                attachMessage.getPath(), address);
                    } else {
                        IpTextMessage textMessage = (IpTextMessage) ipMessageForFavorite;
                        setTextIpmessageFavorite(msgId, textMessage.getType(),
                                textMessage.getBody(), address);
                    }
                } else {
                    setSmsFavorite(mContext, msgId, rcseMsgItem.mBody, address);
                }
            } else {
                setMmsFavorite(mContext, msgId, rcseMsgItem.mSubject, getRecipientStr(),
                        rcseMsgItem.mBoxId, rcseMsgItem.mMessageType);
            }
            return true;

        case MENU_REPORT:

            // Context menu handlers for the spam report.
            String contact = String.valueOf("100869999");
            Log.d(TAG, "spam-report:  ipMessageId = " + ipMessageId + "  contact = " + contact
                    + "  mThreadId = " + mThreadId);
            RCSMessageManager.getInstance(mContext).initSpamReport(contact, mThreadId,
                    ipMessageId);

            return true;

        case MENU_IPMSG_DELIVERY_REPORT:
            showIpDeliveryReport(mContext, rcseMsgItem);
            return true;

        default:
            break;
        }
        return false;
    }

    private void showIpDeliveryReport(Context context, RcsMessageItem rcseMsgItem) {
        Log.d(TAG, "showIpDeliveryReport()");
        if (rcseMsgItem == null) {
            return;
        }
        if (rcseMsgItem.mIpMessageId != 0) {
            new AlertDialog.Builder(context)
                .setTitle(mPluginContext.getString(R.string.ipmsg_delivery_report))
                .setMessage(getIpDeliveryStatus(mPluginContext, rcseMsgItem))
                .setCancelable(true)
                .show();
        }
        return;
    }

    private String getIpDeliveryStatus(Context context, RcsMessageItem msgItem) {
        StringBuilder details = new StringBuilder();
        Resources res = context.getResources();
        if (!msgItem.mIsGroupItem) {
            details.append(res.getString(R.string.to_address_label));
            details.append(msgItem.mAddress);
        }

        String status = res.getString(R.string.status_none);
        int ipStatus = msgItem.getIpMessage().getStatus();
        if (ipStatus == IpMessageStatus.VIEWED) {
            status = res.getString(R.string.status_read);
        } else if (ipStatus == IpMessageStatus.DELIVERED) {
            status = res.getString(R.string.status_received);
        } else if (ipStatus == IpMessageStatus.FAILED) {
            status = res.getString(R.string.status_failed);
        } else if (ipStatus == IpMessageStatus.SENT || ipStatus == IpMessageStatus.OUTBOX) {
            status = res.getString(R.string.status_sending);
        }

        details.append('\n');
        details.append(res.getString(R.string.ipmsg_status_label));
        details.append(status);

        if (msgItem.mDate > 0L) {
            details.append('\n');
            details.append(res.getString(R.string.sent_label));
            String dateStr = RcsUtilsPlugin.formatIpTimeStampString(msgItem.mDate, true);
            details.append(dateStr);
        }
        return details.toString();
    }

    private boolean forwardIpMsg(Context context, RcsMessageItem rcseMsgItem) {

        Intent sendIntent = new Intent();
        long ipMessageId = rcseMsgItem.mIpMessageId;
        IpMessage ipMessage = RCSMessageManager.getInstance(mContext).getIpMsgInfo(mThreadId,
                ipMessageId);
        Log.d(TAG, "forwardMsgHandler()  mType =" + rcseMsgItem.mType + " mIpMessageId = "
                + ipMessageId);
        if ("sms".equals(rcseMsgItem.mType)) {
            if (rcseMsgItem.mIpMessageId == 0) {
                // sms forward
                sendIntent = RcsMessageUtils.createForwordIntentFromSms(context,
                        rcseMsgItem.mBody);
            } else {
                // ip message forward
                sendIntent = RcsMessageUtils.createForwordIntentFromIpmessage(context,
                        ipMessage);
                if (sendIntent != null) {
                } else {
                    if ((ipMessage.getType() == IpMessageType.PICTURE
                            || ipMessage.getType() == IpMessageType.VIDEO
                            || ipMessage.getType() == IpMessageType.VOICE
                            || ipMessage.getType() == IpMessageType.VCARD || ipMessage
                                .getType() == IpMessageType.GEOLOC)) {
                        Toast.makeText(context, "no service, can't forward",
                                Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }

            }
        } else if ("mms".equals(rcseMsgItem.mType)) {
            // mms forward
            Uri realUri = ContentUris.withAppendedId(Mms.CONTENT_URI, rcseMsgItem.mMsgId);
            sendIntent = RcsMessageUtils.createForwardIntentFromMms(context, realUri);
        }

        mContext.startActivity(sendIntent);
        return true;
    }

    private boolean isNetworkConnected(Context context) {
        boolean isNetworkConnected = false;
        ConnectivityManager connManager = (ConnectivityManager) mContext
                .getSystemService(mContext.CONNECTIVITY_SERVICE);
        State state = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
        if (State.CONNECTED == state) {
            isNetworkConnected = true;
        }
        if (!isNetworkConnected) {
            state = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();
            if (State.CONNECTED == state) {
                isNetworkConnected = true;
            }
        }
        return isNetworkConnected;
    }

    private long getRcsFileMaxSize() {
        long maxSize = RCSUtils.getFileTransferMaxSize() * 1024;
        Log.d(TAG, "getRcsFileMaxSize() = " + maxSize);
        return maxSize;
    }

    private long getFileSize(String filepath) {
        return RcsMessageUtils.getFileSize(filepath);
    }

    private Runnable mResizePic = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "mResizePic(): start resize pic.");
            long maxLen = RcsMessageUtils.getCompressLimit();
            byte[] img = RcsMessageUtils.resizeImg(mPhotoFilePath, (float) maxLen);
            if (null == img) {
                return;
            }
            Log.d(TAG, "mResizePic(): put stream to file.");
            try {
                RcsMessageUtils.nmsStream2File(img, mDstPath);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            Log.d(TAG, "mResizePic(): post send pic.");
            mIpMsgHandler.postDelayed(mSendPic, 100);
        }
    };

    private boolean sendMessageForIpMsg(final IpMessage ipMessage,
            boolean isSendSecondTextMessage, final boolean isDelDraft) {
        Log.d(TAG, "sendMessageForIpMsg(): start.");
        if (!mIsGroupChat) {
            mWorkingMessage.syncWorkingIpRecipients();
            mChatModeNumber = getRecipientStr();
        }

        ipMessage.setTo(mChatModeNumber);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "sendMessageForIpMsg(): calling API: saveIpMsg().");
                int ret = -1;
                ipMessage.setStatus(IpMessageStatus.OUTBOX);
                mCallback.guaranteeIpThreadId();
                mThreadId = mCallback.getCurrentThreadId();
                ret = RCSMessageManager.getInstance(mContext).saveRCSMsg(ipMessage, 0,
                        mThreadId);
                mCallback.onPreIpMessageSent();

                if (ipMessage.getType() == IpMessageConsts.IpMessageType.TEXT) {
                    mCallback.asyncDeleteDraftSmsMessage();
                } else {
                    Message msg = new Message();
                    msg.what = ret;
                    mFTNotifyHandler.sendMessage(msg);
                }
                mCallback.onIpMessageSent();
            }
        }).start();

        return true;
    }

    public Handler mFTNotifyHandler = new Handler() {
        public void handleMessage(Message msg) {
            Log.d(TAG, "mIpMsgHandler handleMessage, msg.what: " + msg.what);
            switch (msg.what) {
            case RCSMessageManager.ERROR_CODE_UNSUPPORT_TYPE:
                Toast.makeText(mContext,
                        mPluginContext.getString(R.string.ipmsg_invalid_file_type),
                        Toast.LENGTH_SHORT).show();
                break;
            case RCSMessageManager.ERROR_CODE_INVALID_PATH:
                Toast.makeText(mContext,
                        mPluginContext.getString(R.string.ipmsg_invalid_file_type),
                        Toast.LENGTH_SHORT).show();
                break;
            case RCSMessageManager.ERROR_CODE_EXCEED_MAXSIZE:
                Toast.makeText(mContext,
                        mPluginContext.getString(R.string.ipmsg_over_file_limit),
                        Toast.LENGTH_SHORT).show();
                break;
            case RCSMessageManager.ERROR_CODE_UNKNOWN:
                Toast.makeText(mContext,
                        mPluginContext.getString(R.string.ipmsg_invalid_file_type),
                        Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
            }
            super.handleMessage(msg);
        }
    };

    private void sendIpTextMessage() {
        String body = mTextEditor.getText().toString();
        if (TextUtils.isEmpty(body)) {
            Log.w(TAG, "sendIpTextMessage(): No content for sending!");
            return;
        }
        IpTextMessage msg = new IpTextMessage();
        msg.setBody(body);
        msg.setType(IpMessageType.TEXT);
        msg.setBurnedMessage(mDisplayBurned);
        sendMessageForIpMsg(msg, false, false);
    }

    private Runnable mSendAudio = new Runnable() {
        public void run() {
        	if(!checkBurnedMsgCapbility()) {
        		return;
        	}
        	
            if (RcsMessageUtils.isExistsFile(mDstPath)
                    && RcsMessageUtils.getFileSize(mDstPath) != 0) {
                IpVoiceMessage msg = new IpVoiceMessage();
                msg.setPath(mDstPath);
                msg.setDuration(mDuration);
                msg.setSize(RcsMessageUtils.getFileSize(mDstPath));
                msg.setType(IpMessageType.VOICE);
                msg.setBurnedMessage(mDisplayBurned);
                sendMessageForIpMsg(msg, false, false);
                mIpMsgHandler.removeCallbacks(mSendAudio);
            }
        }
    };

    private Runnable mSendGeoLocation = new Runnable() {
        public void run() {
            Log.d(TAG, "mSendGeoLocation(): start.");
            if (RcsMessageUtils.isExistsFile(mDstPath)
                    && RcsMessageUtils.getFileSize(mDstPath) != 0) {
                IpGeolocMessage msg = new IpGeolocMessage();
                msg.setPath(mDstPath);
                msg.setType(IpMessageType.GEOLOC);
                sendMessageForIpMsg(msg, false, false);
                mIpMsgHandler.removeCallbacks(mSendGeoLocation);
            }
        }
    };
    private Runnable mSendPic = new Runnable() {
        public void run() {
            Log.d(TAG, "mSendPic(): start.");
        	if(!checkBurnedMsgCapbility()) {
        		return;
        	}
            if (RcsMessageUtils.isExistsFile(mDstPath)
                    && RcsMessageUtils.getFileSize(mDstPath) != 0) {
                Log.d(TAG, "mSendPic(): start send image.");
                sendImage(REQUEST_CODE_IPMSG_TAKE_PHOTO);
                mIpMsgHandler.removeCallbacks(mSendPic);
            }
            Log.d(TAG, "mSendPic(): end.");
        }
    };

    private Runnable mSendVideo = new Runnable() {
        public void run() {
            Log.d(TAG, "mSendVideo(): start send video. Path = " + mDstPath);
        	if(!checkBurnedMsgCapbility()) {
        		return;
        	}
            if (RcsMessageUtils.isExistsFile(mDstPath)
                    && RcsMessageUtils.getFileSize(mDstPath) != 0) {
                IpVideoMessage msg = new IpVideoMessage();
                msg.setPath(mDstPath);
                msg.setDuration(mDuration);
                msg.setType(IpMessageType.VIDEO);
                msg.setSize(RcsMessageUtils.getFileSize(mDstPath));
                msg.setBurnedMessage(mDisplayBurned);
                sendMessageForIpMsg(msg, false, false);
                mIpMsgHandler.removeCallbacks(mSendVideo);
            }
        }
    };

    private Runnable mSendVcard = new Runnable() {
        public void run() {
            if (RcsMessageUtils.isExistsFile(mDstPath)
                    && RcsMessageUtils.getFileSize(mDstPath) != 0) {
                IpVCardMessage msg = new IpVCardMessage();
                msg.setPath(mDstPath);
                msg.setName(mIpMessageVcardName);
                msg.setType(IpMessageType.VCARD);
                sendMessageForIpMsg(msg, false, false);
                mIpMsgHandler.removeCallbacks(mSendVcard);
            }
        }
    };

    public boolean getMediaMsgInfo(Intent data, int requestCode) {
        if (null == data) {
            Log.e(TAG, "getMediaMsgInfo(): take video error, result intent is null.");
            return false;
        }

        Uri uri = data.getData();
        Cursor cursor = null;
        if (requestCode == REQUEST_CODE_IPMSG_TAKE_PHOTO
                || requestCode == REQUEST_CODE_IPMSG_CHOOSE_PHOTO) {
            final String[] selectColumn = { "_data" };
            cursor = mContext.getContentResolver().query(uri, selectColumn, null, null, null);
        } else {
            final String[] selectColumn = { "_data", "duration" };
            cursor = mContext.getContentResolver().query(uri, selectColumn, null, null, null);
        }
        if (null == cursor) {
            if (requestCode == REQUEST_CODE_IPMSG_RECORD_AUDIO) {
                mDstPath = uri.getEncodedPath();
                mDuration = data.getIntExtra("audio_duration", 0);
                mDuration = mDuration / 1000 == 0 ? 1 : mDuration / 1000;
            } else {
                mPhotoFilePath = uri.getEncodedPath();
                mDstPath = mPhotoFilePath;
            }
            return true;
        }
        if (0 == cursor.getCount()) {
            cursor.close();
            Log.e(TAG, "getMediaMsgInfo(): take video cursor getcount is 0");
            return false;
        }
        cursor.moveToFirst();
        if (requestCode == REQUEST_CODE_IPMSG_TAKE_PHOTO
                || requestCode == REQUEST_CODE_IPMSG_CHOOSE_PHOTO) {
            mPhotoFilePath = cursor.getString(cursor.getColumnIndex("_data"));
            mDstPath = mPhotoFilePath;
        } else {
            mDstPath = cursor.getString(cursor.getColumnIndex("_data"));
            mDuration = cursor.getInt(cursor.getColumnIndex("duration"));
            mDuration = mDuration / 1000 == 0 ? 1 : mDuration / 1000;
        }
        if (null != cursor && !cursor.isClosed()) {
            cursor.close();
        }
        return true;
    }

    private boolean getAudio(Intent data) {
        Uri uri = (Uri) data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        if (Settings.System.getUriFor(Settings.System.RINGTONE).equals(uri)) {
            return false;
        }
        if (null == uri) {
            uri = data.getData();
        }
        if (null == uri) {
            Log.e(TAG, "getAudio(): choose audio failed, uri is null");
            return false;
        }
        final String scheme = uri.getScheme();
        if (scheme.equals("file")) {
            mDstPath = uri.getEncodedPath();
        } else {
            ContentResolver cr = mContext.getContentResolver();
            Cursor c = cr.query(uri, null, null, null, null);
            c.moveToFirst();
            mDstPath = c.getString(c.getColumnIndexOrThrow(Audio.Media.DATA));
            c.close();
        }

        if (!RcsMessageUtils.isAudio(mDstPath)) {
            Toast.makeText(mPluginContext, R.string.ipmsg_invalid_file_type,
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!mServiceManager.isFeatureSupported(IpMessageConsts.FeatureId.FILE_TRANSACTION)
                && !RcsMessageUtils.isFileStatusOk(mPluginContext, mDstPath)) {
            Log.e(TAG, "getAudio(): choose audio failed, invalid file");
            return false;
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(mContext, uri);
            String dur = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (dur != null) {
                mDuration = Integer.parseInt(dur);
                mDuration = mDuration / 1000 == 0 ? 1 : mDuration / 1000;
            }
        } catch (Exception ex) {
            Log.e(TAG,
                    "getAudio(): MediaMetadataRetriever failed to get duration for "
                            + uri.getPath());
            return false;
        } finally {
            retriever.release();
        }
        return true;
    }

    private void sendImage(int requestCode) {
        IpImageMessage msg = new IpImageMessage();
        msg.setType(IpMessageType.PICTURE);
        msg.setPath(mDstPath);
        msg.setSize(RcsMessageUtils.getFileSize(mDstPath));
        msg.setBurnedMessage(mDisplayBurned);
        // msg.setRcsStatus(Status.TRANSFERING);
        msg.setStatus(Sms.MESSAGE_TYPE_OUTBOX);
        Log.d(TAG, "sendImage(): start send message.");
        sendMessageForIpMsg(msg, false, false);
    }

    @Override
    public boolean onIpMsgOptionsItemSelected(IpConversation ipConv, MenuItem item,
            long threadId) {
        switch (item.getItemId()) {
        case MENU_INVITE_FRIENDS_TO_CHAT:
            if (mServiceManager
                    .isFeatureSupported(IpMessageConsts.FeatureId.EXTEND_GROUP_CHAT)) {
                Intent intent = new Intent(
                        "android.intent.action.contacts.list.PICKMULTIPHONES");
                intent.setType(Phone.CONTENT_TYPE);
                intent.putExtra("Group", true);
                List<String> existNumbers = new ArrayList<String>();
                String number = getNumber();
                existNumbers.add(number);
                String me = RcsProfile.getInstance().getNumber();
                if (!TextUtils.isEmpty(me)) {
                    existNumbers.add(me);
                }
                String[] numbers = existNumbers.toArray(new String[existNumbers.size()]);
                intent.putExtra("ExistNumberArray", numbers);
                mContext.startActivityForResult(intent, REQUEST_CODE_INVITE_FRIENDS_TO_CHAT);
                return true;
            }
            break;
        case MENU_GROUP_CHAT_INFO:
            Log.i(TAG, "launch GroupChatInfo Activity");
            {
                Intent intent = new Intent(
                        "com.mediatek.rcs.message.ui.RcsGroupManagementSetting");
                intent.setPackage("com.mediatek.rcs.message");
                intent.putExtra("SCHATIDKEY", mGroupChatId);
                mContext.startActivity(intent);
                return true;
            }
        case MENU_EDIT_BURNED_MSG:
            Log.d(TAG,
                    "onRequestBurnMessageCapabilityResult MENU_EDIT_BURNED_MSG mBurnedCapbility = "
                            + mBurnedCapbility);
            if (!mBurnedCapbility) {
                Toast.makeText(mPluginContext,R.string.ipmsg_burn_cap,
                        Toast.LENGTH_SHORT).show();
                mOldcontact = null;
                return true;
            }
            mDisplayBurned = !mDisplayBurned;
            // if(
            // !RCSMessageManager.getInstance(mContext).isBurnAferReadCapbility(getNumber()))
            // {
            // return false;
            // }

            if (mDisplayBurned) {
                // mSendButtonIpMessage.setImageResource(R.drawable.ic_send_ipbar);
                mSendButtonIpMessage.setImageDrawable(mPluginContext.getResources()
                        .getDrawable(R.drawable.ic_send_ipbar));
            } else {
                if (mTextEditor != null && !mTextEditor.getText().toString().isEmpty()) {
                    mSendButtonIpMessage.setImageDrawable(mPluginContext.getResources()
                            .getDrawable(R.drawable.ic_send_ipmsg));
                } else {
                    mSendButtonIpMessage.setImageDrawable(mPluginContext.getResources()
                            .getDrawable(R.drawable.ic_send_sms_unsend));
                }
                mOldcontact = null;
            }

            RcsMessageConfig.setEditingDisplayBurnedMsg(mDisplayBurned);
            mCallback.resetSharePanel();
            return true;
        case MENU_CALL_RECIPIENT:
            if (mIsGroupChat) {
                // TODO: multi call
                List<Participant> participants = mGroup.getParticipants();
                ArrayList<Participant> toCallParticipants = new ArrayList<Participant>();
                // ArrayList<String> numbers = new ArrayList<String>();
                String myNumber = RcsProfile.getInstance().getNumber();
                PortraitManager pManager = PortraitManager.getInstance();
                for (Participant p : participants) {
                    if (!p.getContact().equals(myNumber)) {
                        MemberInfo info = pManager.getMemberInfo(mGroup.getChatId(),
                                p.getContact());
                        Participant partipant = new Participant(info.mNumber, info.mName);
                        toCallParticipants.add(partipant);
                    }
                }
                if (toCallParticipants.size() == 1) {
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"
                            + toCallParticipants.get(0).getContact()));
                    mContext.startActivity(dialIntent);
                } else if (toCallParticipants.size() > 1) {
                    showSelectContactNumberDialog(toCallParticipants);
                }
                return true;
            } else if (getRecipientSize() > 1) {
                // one2multi
                ArrayList<Participant> toCallParticipants = new ArrayList<Participant>();
                RcsConversation conversation = (RcsConversation) mCallback.getIpConversation();
                if (conversation != null) {
                    List<IpContact> list = conversation.getIpContactList();
                    for (IpContact c : list) {
                        RcsContact contact = (RcsContact) c;
                        Participant p = new Participant(contact.getNumber(), contact.getName());
                        toCallParticipants.add(p);
                    }
                    if (toCallParticipants.size() > 1) {
                        showSelectContactNumberDialog(toCallParticipants);
                    } else {
                        Log.e(TAG,
                                "init call data error: toCallParticipants is not bigger than 1");
                    }
                } else {
                    Log.e(TAG, "init call data error: conversation is null");
                }
                return true;
            }
            break;

        case MENU_BLACK_LIST:
            String number = getNumber();
            String recipient = getRecipientStr();
            boolean isInBlackList = RCSUtils.isIpSpamMessage(mContext, number);
            CCSblacklist blist = new CCSblacklist(mContext);
            if (isInBlackList) {
                blist.removeblackNumber(number);
            } else {
                blist.addblackNumber(number, recipient);
            }
            break;
        default:
            break;
        }
        return false;
    }

    private boolean doMoreAction(Message msg) {
        Bundle bundle = msg.getData();
        int action = bundle.getInt(SHARE_ACTION);
        boolean ret = true;
        boolean isNoRecipient = (getRecipientSize() == 0 && !mIsGroupChat);
        switch (action) {
        case IPMSG_TAKE_PHOTO:
            if (isNoRecipient) {
                toastNoRecipients(mContext);
            } else {
                takePhoto();
            }
            break;

        case IPMSG_RECORD_VIDEO:
            if (isNoRecipient) {
                toastNoRecipients(mContext);
            } else {
                recordVideo();
            }
            break;

        case IPMSG_SHARE_CONTACT:
            if (isNoRecipient) {
                toastNoRecipients(mContext);
            } else {
                shareContact();
            }
            break;

        case IPMSG_CHOOSE_PHOTO:
            if (isNoRecipient) {
                toastNoRecipients(mContext);
            } else {
                choosePhoto();
            }
            break;

        case IPMSG_CHOOSE_VIDEO:
            if (isNoRecipient) {
                toastNoRecipients(mContext);
            } else {
                chooseVideo();
            }
            break;

        case IPMSG_RECORD_AUDIO:
            if (isNoRecipient) {
                toastNoRecipients(mContext);
            } else {
                recordAudio();
            }
            break;

        case IPMSG_CHOOSE_AUDIO:
            if (isNoRecipient) {
                toastNoRecipients(mContext);
            } else {
                chooseAudio();
            }
            break;

        case IPMSG_SHARE_POSITION:
            if (isNoRecipient) {
                toastNoRecipients(mContext);
            } else {
                sharePosition();
            }
            break;

        default:
            ret = false;
            Log.e(TAG, "doMoreAction(): invalid share action type: " + action);
            break;
        }
        if (ret) {
            mCallback.hideIpSharePanel();
        }
        return ret;
    }

    private class GeoLocCallback implements GeoLocService.callback {

        public void queryGeoLocResult(boolean ret, final Location location) {

            mContext.removeDialog(DIALOG_ID_GETLOC_PROGRESS);
            mGeoLocSrv.removeCallback();
            // if success, store location and show send location confirm dialog
            // if fail, show fail toast
            if (ret == true) {
                mLocation = location;
                mContext.showDialog(DIALOG_ID_GEOLOC_SEND_CONFRIM);
            } else {
                Toast.makeText(mContext, mPluginContext.getString(R.string.geoloc_get_failed),
                        Toast.LENGTH_SHORT).show();
            }
        }

    }

    public void sharePosition() {
        mGeoLocSrv = new GeoLocService(mContext);
        queryGeoLocation();
        // sendGeoLocation();
    }

    private void queryGeoLocation() {
        if (mGeoLocSrv.isEnable()) {
            mContext.showDialog(DIALOG_ID_GETLOC_PROGRESS);
        } else {
            Toast.makeText(mContext, mPluginContext.getString(R.string.geoloc_check_gps),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public Dialog onIpCreateDialog(int id) {
        Log.d(TAG, "onCreateDialog, id:" + id);
        switch (id) {
        case DIALOG_ID_GETLOC_PROGRESS:
            mGeoLocSrv.queryCurrentGeoLocation(new GeoLocCallback());
            ProgressDialog dialog = new ProgressDialog(mContext);
            dialog.setMessage(mPluginContext.getString(R.string.geoloc_being_get));
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface arg0) {
                    mGeoLocSrv.removeCallback();
                }
            });
            dialog.setCanceledOnTouchOutside(false);
            return dialog;

        case DIALOG_ID_GEOLOC_SEND_CONFRIM:
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setMessage(mPluginContext.getString(R.string.geoloc_send_confrim))
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    sendGeoLocation();
                                }
                            }).setNegativeButton(android.R.string.cancel, null);
            // Create the AlertDialog object
            AlertDialog dialog2 = builder.create();
            return dialog2;
        default:
            break;
        }
        return super.onIpCreateDialog(id);
    }

    private void sendGeoLocation() {
        String path = RcsMessageUtils.getGeolocPath(mContext);
        String fileName = GeoLocUtils.buildGeoLocXml(mLocation, "Sender Number", "Message Id",
                path);
        mDstPath = fileName;

        // mDstPath = RcsMessageConfig.getGeolocTempPath(mContext) +
        // File.separator + "geoloc_1.xml";
        Log.e(TAG, "sendGeoLocation() , mDstPath = " + mDstPath);

        // mDstPath = path + "geoloc_1.xml";
        mIpMsgHandler.postDelayed(mSendGeoLocation, 100);
    }

    public void takePhoto() {
        Intent imageCaptureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        mPhotoFilePath = RcsMessageUtils.getPhotoDstFilePath(mContext);
        mDstPath = mPhotoFilePath;
        File out = new File(mPhotoFilePath);
        Uri uri = Uri.fromFile(out);

        long sizeLimit = RcsMessageUtils.getPhotoSizeLimit();
        String resolutionLimit = RcsMessageUtils.getPhotoResolutionLimit();

        imageCaptureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        imageCaptureIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);

        try {
            mContext.startActivityForResult(imageCaptureIntent, REQUEST_CODE_IPMSG_TAKE_PHOTO);
        } catch (Exception e) {
            Toast.makeText(mContext, mPluginContext.getString(R.string.ipmsg_no_app),
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, "takePhoto()");
        }
    }

    public void choosePhoto() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra("com.mediatek.gallery3d.extra.RCS_PICKER", true);
        mDstPath = mPhotoFilePath;
        try {
            mContext.startActivityForResult(intent, REQUEST_CODE_IPMSG_CHOOSE_PHOTO);
        } catch (Exception e) {
            Toast.makeText(mContext, mPluginContext.getString(R.string.ipmsg_no_app),
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, "choosePhoto()");
        }
    }

    public void recordVideo() {
        int durationLimit = RcsMessageUtils.getVideoCaptureDurationLimit();
        String resolutionLimit = RcsMessageUtils.getVideoResolutionLimit();
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        mVideoFilePath = RcsMessageUtils.getVideoDstFilePath(mContext);
        mDstPath = mVideoFilePath;
        File out = new File(mVideoFilePath);
        Uri uri = Uri.fromFile(out);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, durationLimit);
        intent.putExtra("mediatek.intent.extra.EXTRA_RESOLUTION_LIMIT", resolutionLimit);
        try {
            mContext.startActivityForResult(intent, REQUEST_CODE_IPMSG_RECORD_VIDEO);
        } catch (Exception e) {
            Toast.makeText(mContext, mPluginContext.getString(R.string.ipmsg_no_app),
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, "recordVideo()");
        }
    }

    public void chooseVideo() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("video/*");
        try {
            mContext.startActivityForResult(intent, REQUEST_CODE_IPMSG_CHOOSE_VIDEO);
        } catch (Exception e) {
            Toast.makeText(mContext, mPluginContext.getString(R.string.ipmsg_no_app),
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, "chooseVideo()");
        }
    }

    public void recordAudio() {
        Log.d(TAG, "recordAudio(), enter");
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/amr");
        intent.setClassName("com.android.soundrecorder",
                "com.android.soundrecorder.SoundRecorder");
        intent.putExtra("com.android.soundrecorder.maxduration",
                RcsMessageUtils.getAudioDurationLimit());
        mContext.startActivityForResult(intent, REQUEST_CODE_IPMSG_RECORD_AUDIO);
    }

    private void shareContact() {
        addContacts();
    }

    private void addContacts() {
        Intent intent = new Intent("android.intent.action.contacts.list.PICKMULTICONTACTS");
        intent.setType(Contacts.CONTENT_TYPE);
        mContext.startActivityForResult(intent, REQUEST_CODE_IPMSG_SHARE_CONTACT);
    }

    private void addGroups() {
        Intent intent = new Intent("android.intent.action.rcs.contacts.GroupListActivity");
        mContext.startActivityForResult(intent, REQUEST_CODE_IPMSG_SHARE_CONTACT);
    }

    private void chooseAudio() {
        // Not support ringtone
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(ContentType.AUDIO_UNSPECIFIED);
        String[] mimeTypess = new String[] { ContentType.AUDIO_UNSPECIFIED,
                ContentType.AUDIO_MP3, ContentType.AUDIO_3GPP, "audio/M4A",
                ContentType.AUDIO_AAC, ContentType.AUDIO_AMR };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypess);

        // / @}
        mContext.startActivityForResult(intent, REQUEST_CODE_IPMSG_CHOOSE_AUDIO);
    }

    private void shareCalendar() {
        Intent intent = new Intent("android.intent.action.CALENDARCHOICE");
        intent.setType("text/x-vcalendar");
        intent.putExtra("request_type", 0);
        try {
            mContext.startActivityForResult(intent, REQUEST_CODE_IPMSG_SHARE_VCALENDAR);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(mContext, mPluginContext.getString(R.string.ipmsg_no_app),
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, "shareCalendar()");
        }
    }

    private void asyncIpAttachVCardByContactsId(final Intent data) {
        if (data == null) {
            return;
        }
        long[] contactsId = data
                .getLongArrayExtra("com.mediatek.contacts.list.pickcontactsresult");
        RCSVCardAttachment va = new RCSVCardAttachment(mPluginContext);
        mIpMessageVcardName = va.getVCardFileNameByContactsId(contactsId, true);
        mDstPath = RcsMessageUtils.getCachePath(mContext) + mIpMessageVcardName;
        Log.d(TAG, "asyncIpAttachVCardByContactsId(): mIpMessageVcardName = "
                + mIpMessageVcardName + ", mDstPath = " + mDstPath);
        mIpMsgHandler.postDelayed(mSendVcard, 100);
    }

    private void toastNoRecipients(Context context) {
        Toast.makeText(context,
                mPluginContext.getString(R.string.ipmsg_need_input_recipients),
                Toast.LENGTH_SHORT).show();
    }

    private static Intent createIntent(Context context, long threadId) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.mms",
                "com.android.mms.ui.ComposeMessageActivity"));
        if (threadId > 0) {
            intent.setData(getUri(threadId));
        }
        return intent;
    }

    private static Uri getUri(long threadId) {
        return ContentUris.withAppendedId(Threads.CONTENT_URI, threadId);
    }

    private boolean isRecipientsEditorVisible() {
        return mCallback.isIpRecipientEditorVisible();
    }

    private List<String> getRecipientsList() {
        List<String> list;
        if (isRecipientsEditorVisible()) {
            list = mCallback.getRecipientsEditorInfoList();
        } else {
            String[] numbers = mCallback.getConversationInfo();
            list = new ArrayList<String>(numbers.length);
            for (String number : numbers) {
                list.add(number);
            }
        }
        return list;
    }

    private String formatRecipientsStr(List<String> list) {
        StringBuffer builder = new StringBuffer();
        for (String number : list) {
            if (!TextUtils.isEmpty(number)) {
                builder.append(number);
                builder.append(",");
            }
        }
        String recipientStr = builder.toString();
        if (recipientStr.endsWith(",")) {
            recipientStr = recipientStr.substring(0, recipientStr.lastIndexOf(","));
        }
        return recipientStr;
    }

    private String getRecipientStr() {
        String ret = "";
        List<String> list = getRecipientsList();
        if (list != null && list.size() > 0) {
            ret = formatRecipientsStr(list);
        }
        Log.d(TAG, "getRecipientStr: " + ret);
        return ret;
    }

    private String getNumber() {
        List<String> list = getRecipientsList();
        if (list != null && list.size() > 0) {
            return list.get(0);
        }
        return "";
    }

    private int getRecipientSize() {
        int ret = 0;
        List<String> list = getRecipientsList();
        if (list != null) {
            ret = list.size();
        }
        Log.d(TAG, "getRecipientSize: " + ret);
        return ret;
    }

    private void hideInputMethod() {
        Log.d(TAG, "hideInputMethod()");
        if (mContext.getWindow() != null && mContext.getWindow().getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) mContext
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(mContext.getWindow().getCurrentFocus()
                    .getWindowToken(), 0);
        }
    }

    private void getContactCapbility() {
        if (1 == getRecipientSize()) {
            mServiceManager.registBurnMsgCapListener(this);
            Log.d(TAG, "onRequestBurnMessageCapabilityResult getNumber() = " + getNumber());
            mServiceManager.getBurnMsgCap(getNumber());
        }
    }

    public void onIpRecipientsChipChanged(int number) {
        if (number > 0) {
            mCallback.enableShareButton(true);
        } else {
            mCallback.enableShareButton(false);
        }
    }

    @Override
    public boolean handleIpMessage(Message msg) {
        Log.d(TAG, "mIpMsgHandler handleMessage, msg.what: " + msg.what);
        boolean ret = false;

        switch (msg.what) {
        case ACTION_SHARE:
            if (RcsMessageConfig.isServiceEnabled(mContext)) {
                ret = doMoreAction(msg);
            }
            break;
        default:
            Log.d(TAG, "msg type: " + msg.what + "not handler");
            break;
        }
        return ret;
    }

    private void copyFile(IpAttachMessage ipAttachMessage) {
        Log.d(TAG, "copyFile type = " + ipAttachMessage.getType());
        String source = ipAttachMessage.getPath();
        if (TextUtils.isEmpty(source)) {
            Log.e(TAG, "saveMsgInSDCard(): save ipattachmessage failed, source empty!");
            Toast.makeText(mPluginContext, R.string.copy_to_sdcard_fail, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        File inFile = new File(source);
        if (!inFile.exists()) {
            Log.e(TAG,
                    "saveMsgInSDCard(): save ipattachmessage failed, source file not exist!");
            return;
        }
        String attName = source.substring(source.lastIndexOf("/") + 1);
        File dstFile = RcsMessageUtils.getStorageFile(attName);
        if (dstFile == null) {
            Log.i(TAG, "saveMsgInSDCard(): save ipattachmessage failed, dstFile not exist!");
            return;
        }
        RcsMessageUtils.copy(inFile, dstFile);
        Toast.makeText(mPluginContext, R.string.copy_to_sdcard_success, Toast.LENGTH_SHORT)
                .show();
        // Notify other applications listening to scanner events
        // that a media file has been added to the sd card
        mPluginContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri
                .fromFile(dstFile)));
    }

    private boolean setSmsFavorite(Context mContext, long id, String mBody, String mAddress) {
        Log.d(TAG, "setSmsFavorite id =" + id + ",mBody = " + mBody + ",mAddress =" + mAddress);
        ContentValues values = new ContentValues();
        values.put("favoriteid", id);
        values.put("type", 1); // sms
        values.put("address", mAddress);
        values.put("body", mBody);
        values.put("date", System.currentTimeMillis());
        mContext.getContentResolver().insert(FavoriteMsgProvider.CONTENT_URI, values);
        Toast.makeText(mPluginContext, R.string.toast_favorite_success, Toast.LENGTH_SHORT)
                .show();
        return true;
    }

    private boolean setMmsFavorite(Context mContext, long id, String mSubject,
            String mAddress, int mBoxId, int type) {
        Log.d(TAG, "setMmsFavorite id =" + id + ",mSubject = " + mSubject + ",mAddress ="
                + mAddress + ",mBoxId = " + mBoxId);
        byte[] pduMid;
        String mPduFileName = mPluginContext.getDir("favorite", 0).getPath() + "/pdu";
        File path = new File(mPduFileName);
        Log.d(TAG, "thiss mPduFileName =" + mPduFileName);
        if (!path.exists()) {
            path.mkdirs();
        }
        try {
            Log.d(TAG, "thiss cache id =" + id);
            Log.d(TAG, "thiss time =" + System.currentTimeMillis());
            Uri realUri = ContentUris.withAppendedId(Mms.CONTENT_URI, id);
            Log.d(TAG, "thiss realUri =" + realUri);
            PduPersister p = PduPersister.getPduPersister(mContext);
            Log.d(TAG, "thiss mBoxId =" + mBoxId);
            if (mBoxId == Mms.MESSAGE_BOX_INBOX) {
                if (type == MESSAGE_TYPE_NOTIFICATION_IND) {
                    pduMid = null;
                } else if (type == MESSAGE_TYPE_RETRIEVE_CONF) {
                    RetrieveConf rPdu = (RetrieveConf) p.load(realUri, false);
                    pduMid = new PduComposer(mContext, rPdu).make(false);
                } else {
                    pduMid = null;
                }
            } else {
                SendReq sPdu = (SendReq) p.load(realUri);
                pduMid = new PduComposer(mContext, sPdu).make();
                Log.d(TAG, "thiss SendReq pduMid =" + pduMid);
            }
            String mFile = mPduFileName + "/" + System.currentTimeMillis() + ".pdu";
            if (pduMid != null) {
                byte[] pduByteArray = pduMid;
                Log.d(TAG, "thiss fileName =" + mFile);
                writeToFile(mFile, pduByteArray);
            }
            if (pduMid != null) {
                ContentValues values = new ContentValues();
                values.put("favoriteid", id);
                values.put("path", mFile);
                values.put("type", 2); // mms
                if (mBoxId != Mms.MESSAGE_BOX_INBOX) {
                    mAddress = null;
                }
                values.put("address", mAddress);
                if (!TextUtils.isEmpty(mSubject)) {
                    values.put("body", mSubject);
                }
                values.put("date", System.currentTimeMillis());
                mContext.getContentResolver().insert(FavoriteMsgProvider.CONTENT_URI, values);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(mPluginContext, R.string.toast_favorite_success, Toast.LENGTH_SHORT)
                .show();
        return true;
    }

    private boolean setTextIpmessageFavorite(long id, int ct, String mBody, String mAddress) {
        Log.d(TAG, "setTextIpmessageFavorite id =" + id + ",mBody = " + mBody + ",mAddress ="
                + mAddress);
        ContentValues values = new ContentValues();
        values.put("favoriteid", id);
        values.put("type", 3); // Ipmessage
        values.put("address", mAddress);
        values.put("body", mBody);
        values.put("ct", "text/plain");
        values.put("date", System.currentTimeMillis());
        if (mGroupChatId != null) {
            values.put("chatid", mGroupChatId);
        }
        mContext.getContentResolver().insert(FavoriteMsgProvider.CONTENT_URI, values);
        Toast.makeText(mPluginContext, R.string.toast_favorite_success, Toast.LENGTH_SHORT)
                .show();
        return true;
    }

    private boolean setAttachIpmessageFavorite(long id, int ct, String mPath, String mAddress) {
        Log.d(TAG, "setAttachIpmessageFavorite id =" + id + ",mPath = " + mPath
                + ",mAddress =" + mAddress);
        ContentValues values = new ContentValues();
        values.put("favoriteid", id);
        values.put("type", 3); // Ipmessage
        values.put("address", mAddress);
        values.put("date", System.currentTimeMillis());
        if (mGroupChatId != null) {
            values.put("chatid", mGroupChatId);
        }
        if (mPath != null) {
            String mImFileName = mPluginContext.getDir("favorite", 0).getPath() + "/Ipmessage";
            File path = new File(mImFileName);
            Log.d(TAG, "thiss mImFileName =" + mImFileName);
            if (!path.exists()) {
                path.mkdirs();
            }
            String mFileName = mImFileName + "/" + getFileName(mPath);
            String mNewpath = RcsMessageUtils.getUniqueFileName(mFileName);
            Log.d(TAG, "thiss mNewpath =" + mNewpath);
            RcsMessageUtils.copy(mPath, mNewpath);
            values.put("path", mNewpath);
            String mimeType = RCSUtils.getFileType(getFileName(mPath));
            if (mimeType != null) {
                values.put("ct", mimeType);
            }
        }
        mContext.getContentResolver().insert(FavoriteMsgProvider.CONTENT_URI, values);
        Toast.makeText(mPluginContext, R.string.toast_favorite_success, Toast.LENGTH_SHORT)
                .show();
        return true;
    }

    private String getFileName(String mFile) {
        return mFile.substring(mFile.lastIndexOf("/") + 1);
    }

    private void writeToFile(String fileName, byte[] buf) {
        try {
            FileOutputStream outStream = new FileOutputStream(fileName);
            // byte[] buf = inBuf.getBytes();
            outStream.write(buf, 0, buf.length);
            outStream.flush();
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onIpKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown(): keyCode = " + keyCode);
        switch (keyCode) {
        case KeyEvent.KEYCODE_DEL:
            break;

        case KeyEvent.KEYCODE_BACK:
            if (mEmoji != null && mEmoji.isEmojiPanelShow()) {
                mEmoji.showEmojiPanel(false);
                return true;
            }
            break;
        default:
            break;
        }
        return false;
    }

    @Override
    public boolean onIPQueryMsgList(AsyncQueryHandler mQueryHandler, int token, Object cookie,
            Uri uri, String[] projection, String selection, String[] selectionArgs,
            String orderBy) {
        if (!mIsGroupChat) {
            // return false;
            mQueryHandler.startQuery(token, cookie, uri, projection, selection, selectionArgs,
                    orderBy);
        } else {
            uri = ContentUris.withAppendedId(Uri.parse("content://mms-sms/rcs/conversations"),
                    mThreadId);
            mQueryHandler.startQuery(token, cookie, uri, projection, selection,
                                          selectionArgs, null);
        }
        setChatType();
        return true;
    }

    @Override
    public void onIpConfig(Configuration newConfig) {
        Log.d(TAG, "onIpConfig()");
        if (mEmoji != null && mEmoji.isEmojiPanelShow()) {
            mEmoji.resetEmojiPanel(newConfig);
        }
    }

    private Dialog createInvitationDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);
        dialogBuilder
                .setTitle(mPluginContext.getString(R.string.group_chat))
                .setCancelable(false)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setMessage(mPluginContext.getString(R.string.group_invitation_indicater))
                .setPositiveButton(mPluginContext.getString(R.string.group_accept),
                        new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                // accept invitation
                                MapInfo info = ThreadMapCache.getInstance().getInfoByChatId(
                                        mGroupChatId);
                                if (info != null && info.getStatus() == IpMessageConsts.
                                           GroupActionList.GROUP_STATUS_INVITE_EXPAIRED) {
                                    showInvitaionTimeOutDialog();
                                } else {
                                    if (mIsSmsEnabled) {
                                        boolean ret = GroupManager.getInstance(mContext)
                                                .acceptGroupInvitation(mGroupChatId);
                                        if (ret == false) {
                                            Toast.makeText(mContext, R.string.group_aborted,
                                                    Toast.LENGTH_SHORT).show();
                                            mContext.finish();
                                        } else {
                                            // setChatActive(true);
                                            showProgressDialog(mPluginContext
                                                    .getString(R.string.pref_please_wait));
                                        }
                                    }
                                }
                            }
                        })
                .setNegativeButton(mPluginContext.getString(R.string.group_reject),
                        new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                // reject invitation
                                GroupManager.getInstance(mContext).rejectGroupInvitation(
                                        mGroupChatId);
                                showProgressDialog(mPluginContext
                                        .getString(R.string.pref_please_wait));
                            }
                        })
                .setNeutralButton(mPluginContext.getString(R.string.group_shelve),
                        new OnClickListener() {

                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                // don't process now. do noting but exit this
                                // screen
                                mContext.finish();
                            }
                        });
        Dialog dialog = dialogBuilder.create();
        return dialog;
    }

    private void showInvitaionDialog(final int status) {
        mGroupStatus = status;
        if (mInvitationDialog == null) {
            mInvitationDialog = createInvitationDialog();
        }
        if (!mInvitationDialog.isShowing()) {
            mInvitationDialog.show();
        }
    }

    private void dismissInvitationDialog() {
        if (mInvitationDialog != null && mInvitationDialog.isShowing()) {
            mInvitationDialog.dismiss();
        }
    }

    private Dialog createInvitationTimeOutDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);
        dialogBuilder.setTitle(mPluginContext.getString(R.string.group_chat))
                .setCancelable(false).setIconAttribute(android.R.attr.alertDialogIcon)
                .setMessage(mPluginContext.getString(R.string.group_invitation_expired))
                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        if (mRcsMessageListAdapter == null
                                || mRcsMessageListAdapter.isOnlyHasSystemMsg()) {
                            HashSet<Long> set = new HashSet<Long>();
                            set.add(mThreadId);
                            RCSMessageManager.getInstance(mContext).deleteRCSThreads(set, 0,
                                    true);
                            mContext.finish();
                        } else {
                            ContentValues values = new ContentValues();
                            values.put(Threads.STATUS,
                                    GroupActionList.GROUP_STATUS_INVALID);
                            Uri uri = ContentUris.withAppendedId(
                                    RCSUtils.URI_THREADS_UPDATE_STATUS, mThreadId);
                            if (mIsSmsEnabled) {
                                mContext.getContentResolver().update(uri, values, null, null);
                            }
                            setChatActive(false);
                        }
                    }
                });
        Dialog dialog = dialogBuilder.create();
        return dialog;
    }

    private void showInvitaionTimeOutDialog() {
        if (mInvitationExpiredDialog == null) {
            mInvitationExpiredDialog = createInvitationTimeOutDialog();
        }
        if (!mInvitationExpiredDialog.isShowing()) {
            mInvitationExpiredDialog.show();
        }
    }

    private void dismissInvitationTimeOutDialog() {
        if (mInvitationExpiredDialog != null && mInvitationExpiredDialog.isShowing()) {
            mInvitationExpiredDialog.dismiss();
        }
    }

    @Override
    public boolean onIpCheckRecipientsCount() {
        if (mIsGroupChat) {
            mCallback.callbackCheckConditionsAndSendMessage(true);
            return true;

        } else {
            return super.onIpCheckRecipientsCount();
        }
    }

    @Override
    public boolean isIpRecipientCallable(String[] numbers) {
        if (mIsGroupChat) {
            if (mIsChatActive && mGroup.getParticipants().size() > 0) {
                return true;
            }
        } else if (numbers.length > 0) {
            return true;
        }
        return super.isIpRecipientCallable(numbers);
    }

    /**
     * Override IpComposeActivity's showIpMessageDetails.
     * @param msgItem IIpMessageItem
     * @return true if show.
     */
    public boolean showIpMessageDetails(IIpMessageItem msgItem) {
        if (msgItem == null || !(msgItem instanceof RcsMessageItem)) {
            return false;
        }
        if (!mIsGroupChat) {
            return false;
        }
        RcsMessageItem item = (RcsMessageItem) msgItem;
        Resources res = mPluginContext.getResources();
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(res.getString(R.string.message_details_title))
        .setMessage(RcsMessageListItem.getIpTextMessageDetails(mPluginContext, item))
        .setCancelable(true)
        .show();
        return true;
    }
    private boolean isMms() {
        boolean ret = false;
        if (mWorkingMessage != null) {
            ret = mWorkingMessage.requiresIpMms();
        }
        return ret;
    }

    private void invalidateOptionMenu() {
        if (mCallback != null) {
            mCallback.invalidateIpOptionsMenu();
        }
    }

    private void setChatType() {
        if (mRcsMessageListAdapter == null) {
            return;
        }
        if (mIsGroupChat) {
            mRcsMessageListAdapter.setChatType(mRcsMessageListAdapter.CHAT_TYPE_GROUP);
        } else if (getRecipientSize() == 1) {
            mRcsMessageListAdapter.setChatType(mRcsMessageListAdapter.CHAT_TYPE_ONE2ONE);
        } else if (getRecipientSize() > 1) {
            mRcsMessageListAdapter.setChatType(mRcsMessageListAdapter.CHAT_TYPE_ONE2MULTI);
        } else {
            Log.d(TAG, "setChatType(): unknown chat type");
        }
    }

    private void setChatActive(final boolean active) {
        mIsChatActive = active;
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                showEditZone(active);
                if (mRcsMessageListAdapter != null) {
                    mRcsMessageListAdapter.setChatActive(active);
                }
                invalidateOptionMenu();
                if (!active) {
                    mContext.getWindow().setSoftInputMode(
                            WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                    mCallback.hideIpInputMethod();
                    if (mTextEditor != null) {
                        mTextEditor.clearFocus();
                    }
                }
            }
        });
    }

    private void showEditZone(boolean show) {
        int visible = show ? View.VISIBLE : View.GONE;
        if (mEditZone != null) {
            mEditZone.setVisibility(visible);
            if (mTextEditor != null) {
                mTextEditor.setVisibility(visible);
            }
        }
    }

    private int getLastSendMode() {
        SharedPreferences preferece = mContext.getSharedPreferences(
                PREFERENCE_SEND_WAY_CHANGED_NAME, Context.MODE_WORLD_READABLE);
        return preferece.getInt(PREFERENCE_KEY_SEND_WAY, PREFERENCE_VALUE_SEND_WAY_UNKNOWN);
    }

    private boolean isNeedShowSendWayChangedDialog(final boolean canUseIM, final int subId) {
        SharedPreferences preferece = mContext.getSharedPreferences(
                PREFERENCE_SEND_WAY_CHANGED_NAME, Context.MODE_WORLD_READABLE);
        boolean mode = RcsSettingsActivity.getSendMSGMode(mPluginContext);
        if (!mode) {
            return false;
        }
        int lastSendWay = preferece.getInt(PREFERENCE_KEY_SEND_WAY,
                PREFERENCE_VALUE_SEND_WAY_UNKNOWN);
        int nowSendWay = canUseIM ? PREFERENCE_VALUE_SEND_WAY_IM
                : PREFERENCE_VALUE_SEND_WAY_SMS;
        if (lastSendWay == nowSendWay) {
            return true;
        } else {
            return false;
        }
    }

    void showSendWayChanged(boolean useIM, int subId) {
        DialogFragment fragment = new DialogFragment() {
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                final boolean isIM = getArguments().getBoolean("useIM");
                View contents = View.inflate(mPluginContext,
                        R.layout.send_way_changed_dialog_view, null);
                TextView msg = (TextView) contents.findViewById(R.id.message);
                final CheckBox rememberCheckBox = (CheckBox) contents
                        .findViewById(R.id.remember_choice);
                if (isIM) {
                    msg.setText(mPluginContext.getString(R.string.send_way_use_im_indicate));
                } else {
                    msg.setText(mPluginContext.getString(R.string.send_way_use_sms_indicate));
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                return builder
                        .setTitle(mPluginContext.getString(R.string.send_way_changed))
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        boolean checked = rememberCheckBox.isChecked();
                                        SharedPreferences preferece = mContext
                                                .getSharedPreferences(
                                                        PREFERENCE_SEND_WAY_CHANGED_NAME,
                                                        Context.MODE_WORLD_WRITEABLE);
                                        Editor editor = preferece.edit();
                                        String pName = isIM ? PREFERENCE_KEY_SEND_WAY_IM
                                                : PREFERENCE_KEY_SEND_WAY_SMS;
                                        if (isIM) {
                                            editor.putInt(PREFERENCE_KEY_SEND_WAY,
                                                    PREFERENCE_VALUE_SEND_WAY_IM);
                                            editor.putBoolean(PREFERENCE_KEY_SEND_WAY_IM,
                                                    checked);
                                            // TODO use IM send
                                        } else {
                                            editor.putInt(PREFERENCE_KEY_SEND_WAY,
                                                    PREFERENCE_VALUE_SEND_WAY_SMS);
                                            editor.putBoolean(PREFERENCE_KEY_SEND_WAY_SMS,
                                                    checked);
                                            // TODO use sms send
                                        }
                                        editor.apply();
                                    }
                                })
                        .setNegativeButton(android.R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        boolean checked = rememberCheckBox.isChecked();
                                        SharedPreferences preferece = mContext
                                                .getSharedPreferences(
                                                        PREFERENCE_SEND_WAY_CHANGED_NAME,
                                                        Context.MODE_WORLD_WRITEABLE);
                                        Editor editor = preferece.edit();
                                        if (isIM) {
                                            // TODO use sms send?
                                        } else {
                                            // TODO not to send
                                        }
                                        // String pName = isIM ?
                                        // PREFERENCE_SEND_WAY_IM :
                                        // PREFERENCE_SEND_WAY_SMS;
                                        // editor.putBoolean(pName, checked);
                                    }
                                }).setView(contents).create();
            }
        };
        Bundle args = new Bundle();
        args.putBoolean("useIM", useIM);
        fragment.show(mContext.getFragmentManager(), "showSendWayChanged");
    }

    void initDualSimState() {
        if (!RcsMessageUtils.getConfigStatus()) {
            return;
        }
        List<SubscriptionInfo> mSubInfoList = SubscriptionManager.from(mContext)
                .getActiveSubscriptionInfoList();
        int mSubCount = (mSubInfoList == null || mSubInfoList.isEmpty()) ? 0 : mSubInfoList
                .size();
        int subIdinSetting = SubscriptionManager.getDefaultSmsSubId();
        int mainCardSubId = SubscriptionManager.getDefaultDataSubId();
        Log.d(TAG, "initDualSimState: mSubCount=" + mSubCount + ", subIdinSetting="
                + subIdinSetting + ", mainCardSubId=" + mainCardSubId);
        int subId = -1;
        if (mSubCount > 1) {
            if (subIdinSetting == (int) Settings.System.SMS_SIM_SETTING_AUTO) {
                subId = mRcsMessageListAdapter.getAutoSelectSubId();
                // the SIM card has been removed
                if (subId == -1) {
                    subId = getIpAutoSelectSubId();
                }
            } else {
                // SIM1 or SIM2
                subId = subIdinSetting;
            }
            int capatibily = 0;
            if (SubscriptionManager.isValidSubscriptionId(mainCardSubId)
                    && subId == mainCardSubId) {
                capatibily = RcsMessageUtils.getMainCardSendCapability();
            } else {
                // if not main card, can send sms or mms
                mCallback.enableShareButton(true);
                return;
            }

            Log.d(TAG, "initDualSimState: capatibily=" + capatibily);
            // if not support mms, disable share button
            if (capatibily == 1) {
                mCallback.enableShareButton(false);
            } else {
                mCallback.enableShareButton(true);
            }
        }
    }

    void showSelectContactNumberDialog(ArrayList<Participant> participants) {
        boolean[] checkedItems = new boolean[participants.size()];

        DialogFragment fragment = new DialogFragment() {
            int mCheckedNumber = 0;
            final int MAX_NUMBER = 5;
            AlertDialog mDialog;

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                Bundle arguments = getArguments();
                final ArrayList<Participant> participants = arguments
                        .getParcelableArrayList("participants");
                final String[] sourceArray = new String[participants.size()];
                final boolean[] checkedArray = arguments.getBooleanArray("checklist");
                int index = 0;
                for (Participant p : participants) {
                    String content = p.getDisplayName();
                    if (TextUtils.isEmpty(content)) {
                        content = p.getContact();
                    }
                    sourceArray[index] = content;
                    index++;
                }
                int totalNumber = checkedArray.length;
                for (int checkedIndex = 0; checkedIndex < totalNumber; checkedIndex++) {
                    if (checkedArray[checkedIndex]) {
                        mCheckedNumber++;
                    }
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                builder.setMultiChoiceItems(sourceArray, checkedArray,
                        new OnMultiChoiceClickListener() {

                            @Override
                            public void onClick(DialogInterface arg0, int posion,
                                    boolean checked) {
                                // TODO Auto-generated method stub
                                Log.d(TAG, "setMultiChoiceItems: " + posion + ", " + checked);
                                if (checked) {
                                    mCheckedNumber++;
                                    if (mCheckedNumber == MAX_NUMBER + 1) {
                                        // TODO
                                        Toast.makeText(mContext,
                                                "the max number is " + MAX_NUMBER,
                                                Toast.LENGTH_SHORT).show();
                                        mDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(
                                                false);
                                    }
                                } else {
                                    mCheckedNumber--;
                                    if (mCheckedNumber == MAX_NUMBER) {
                                        mDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(
                                                true);
                                    }
                                }
                            }
                        })
                        .setTitle(
                                mPluginContext.getString(R.string.multi_select_contact_title))
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        ArrayList<String> selectedNumbers = new ArrayList<String>();
                                        int totalNumber = checkedArray.length;
                                        StringBuilder contentBuilder = new StringBuilder("");
                                        for (int checkedIndex = 0; checkedIndex < totalNumber;
                                                checkedIndex++) {
                                            if (checkedArray[checkedIndex]) {
                                                selectedNumbers.add(participants.get(
                                                        checkedIndex).getContact());
                                            }
                                        }
                                        startMultiCall(selectedNumbers);
                                    }
                                });
                mDialog = builder.create();
                mDialog.setCanceledOnTouchOutside(false);
                return mDialog;
            }
        };
        Bundle args = new Bundle();
        args.putParcelableArrayList("participants", participants);
        args.putBooleanArray("checklist", checkedItems);
        fragment.setArguments(args);
        fragment.show(mContext.getFragmentManager(), "showSelectContactNumberDialog");
    }

    private void startMultiCall(ArrayList<String> numbers) {
        if (numbers == null || numbers.size() == 0) {
            Toast.makeText(mContext, mPluginContext.getString(R.string.no_contact_selected),
                                    Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri = Uri.fromParts("tel", numbers.get(0), null);
        final Intent intent = new Intent(Intent.ACTION_CALL, uri);
        intent.putExtra(TelecomManagerEx.EXTRA_VOLTE_CONF_CALL_DIAL, true);
        intent.putStringArrayListExtra(TelecomManagerEx.EXTRA_VOLTE_CONF_CALL_NUMBERS, numbers);
        mContext.startActivity(intent);
    }

    @Override
    public int getIpAutoSelectSubId() {
        if (isRecipientsEditorVisible()) {
            int userSuggestionId = mContext.getIntent().getIntExtra(
                    PhoneConstants.SUBSCRIPTION_KEY,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            if (userSuggestionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                return userSuggestionId;
            } else {
            	SubscriptionInfo subInfoRecord = SubscriptionManager.from(
            			mContext).getActiveSubscriptionInfoForSimSlotIndex(0);
            	if (subInfoRecord != null) {
            		int subId = subInfoRecord.getSubscriptionId();
                    Log.d(TAG, "getIpAutoSelectSubId subId = " + subId);
            		return subId;
                }
            }
        }
        Log.d(TAG, "getIpAutoSelectSubId isRecipientsEditorVisible false");
        return -1;
    }

    /**
     * get current composer instance
     * @return RcsComposeActivity
     */
    public static RcsComposeActivity getRcsComposer() {
        return sComposer;
    }

    /**
     * Process new Invitation when the new invitation's threadid is same as current.
     * @param threadId thread id
     * @return true processed, or false
     */
    public boolean processNewInvitation(long threadId) {
        Log.d(TAG, "processNewInvitation: threadId = " + threadId);
        if (sComposer != null && mThreadId == threadId) {
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    showInvitaionDialog(GroupActionList.GROUP_STATUS_INVITING_AGAIN);
                }
            });
            return true;
        } else {
            return false;
        }
    }

    private OnMessageListChangedListener mMsgListChangedListener =
                                                            new OnMessageListChangedListener() {

        @Override
        public void onChanged() {
            // TODO Auto-generated method stub
            mCallback.resetSharePanel();
            if (mShowInviteMsg || mIsGroupChat) {
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        mCallback.hideIpRecipientEditor();
                    }
                });
            }
        }
    };

    /**
     * Is Rcs Mode.
     * @return true if edit rcs message or false
     */
    public boolean isRcsMode() {
        if (mIsGroupChat) {
            return true;
        }
        // not configured
        if (!mServiceEnabled) {
            Log.d(TAG, "service is not configured");
            return false;
        }

        if (mCallback == null) {
            // composer not create completed.
            return true;
        }

        // have mms
        if (isMms()) {
            Log.d(TAG, "current is mms mode");
            return false;
        }

        List<SubscriptionInfo> subInfoList = SubscriptionManager.from(mContext)
                .getActiveSubscriptionInfoList();
        int subCount = (subInfoList == null || subInfoList.isEmpty()) ? 0 : subInfoList.size();
        if (subCount == 0) {
            return false;
        } else if (subCount == 1) {
            return true;
        }

        /* TODO: for it, can release the code below when QC */
        int rcsSubId = RcsMessageUtils.getRcsSubId(mContext);
        Log.d(TAG, "rcsSubId is: " + rcsSubId);
        if (rcsSubId == -1) {
            return false;
        } else {
            int userSelectedId = RcsMessageUtils.getUserSelectedId(mContext);
            Log.d(TAG, "userSelectedId is: " + userSelectedId);
            if (userSelectedId == (int) Settings.System.SMS_SIM_SETTING_AUTO) {
                // auto
                if (isRecipientsEditorVisible()) {
                    int userSuggestionId = (int) mContext.getIntent().getLongExtra(
                            PhoneConstants.SUBSCRIPTION_KEY,
                            SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                    if (userSuggestionId != (int) SubscriptionManager.INVALID_SUBSCRIPTION_ID
                            && userSuggestionId != rcsSubId) {
                        return false;
                    }
                } else if (mRcsMessageListAdapter != null) {
                    int lastMsgSubId = (int) mRcsMessageListAdapter.getAutoSelectSubId();
                    Log.d(TAG, "last message subid is: " + lastMsgSubId);
                    if (rcsSubId != lastMsgSubId && lastMsgSubId != -1) {
                        // not rcs subid
                        return false;
                    }
                }
            } else if (userSelectedId != rcsSubId) {
                return false;
            }
        }
        return true;
    }

    private BroadcastReceiver mChangeSubReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null
                    && action.equals(TelephonyIntents.ACTION_DEFAULT_SMS_SUBSCRIPTION_CHANGED)) {
                int subIdinSetting = (int) intent.getLongExtra(
                        PhoneConstants.SUBSCRIPTION_KEY,
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                mCallback.resetSharePanel();
                initDualSimState();
            }
        }
    };

    private ProgressDialog createProgressDialog() {
        ProgressDialog dialog = new ProgressDialog(mContext);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setIndeterminate(true);
        return dialog;
    }

    private void showProgressDialog(String msg) {
        if (mProgressDialog == null) {
            mProgressDialog = createProgressDialog();
        }
        if (TextUtils.isEmpty(msg)) {
            mProgressDialog.setMessage("");
        } else {
            mProgressDialog.setMessage(msg);
        }
        mProgressDialog.show();
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    IInitGroupListener mInitGroupListener = new IInitGroupListener() {

        @Override
        public void onRejectGroupInvitationResult(final int result, final long threadId,
                final String chatId) {
            // TODO Auto-generated method stub
            Log.d(TAG, "onRejectGroupInvitationResult: result : " + result);
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    if (mThreadId == threadId) {
                        dismissProgressDialog();
                        if (result != GroupActionList.VALUE_SUCCESS) {
                            // fail
                            Toast.makeText(mContext,
                                    mPluginContext.getString(R.string.group_accept_fail),
                                    Toast.LENGTH_SHORT).show();
                            showInvitaionDialog(mGroupStatus);
                        } else {
                            if (mGroupStatus ==
                                    GroupActionList.GROUP_STATUS_INVITING
                                    || mRcsMessageListAdapter == null
                                    || mRcsMessageListAdapter.isOnlyHasSystemMsg()) {
                                mContext.finish();
                                HashSet<Long> set = new HashSet<Long>();
                                set.add(mThreadId);
                                RCSMessageManager.getInstance(mContext).deleteRCSThreads(set,
                                        0, true);
                            } else if (mGroupStatus ==
                                    GroupActionList.GROUP_STATUS_INVITING_AGAIN) {
                                setChatActive(false);
                                ContentValues values = new ContentValues();
                                values.put(Threads.STATUS,
                                        GroupActionList.GROUP_STATUS_INVALID);
                                Uri uri = ContentUris.withAppendedId(
                                        RCSUtils.URI_THREADS_UPDATE_STATUS, mThreadId);
                                try {
                                    mContext.getContentResolver().update(uri, values, null,
                                            null);
                                } catch (Exception e) {
                                    // TODO: handle exception
                                    Log.e(TAG, "[showInvitaionTimeOutDialog]: e = " + e);
                                }
                            }
                        }
                    }
                }
            });
        }

        @Override
        public void onInitGroupResult(int result, long threadId, String chatId) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onAcceptGroupInvitationResult(final int result, final long threadId,
                final String chatId) {
            Log.d(TAG, "onAcceptGroupInvitationResult: result : " + result);
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mThreadId == threadId) {
                        dismissProgressDialog();
                        if (result == GroupActionList.VALUE_SUCCESS) {
                            // success
                            // Toast.makeText(mContext, "Accept result sucess",
                            // Toast.LENGTH_SHORT).show();
                            setChatActive(true);
                        } else {
                            Toast.makeText(mContext,
                                    mPluginContext.getString(R.string.group_accept_fail),
                                    Toast.LENGTH_SHORT).show();
                            showInvitaionDialog(mGroupStatus);
                        }
                    }
                }
            });
        }
    };

    private void setupCallStatusServiceConnection() {
        Log.i(TAG, "setupCallStatusServiceConnection.");
        if (mCallStatusService == null || mCallConnection == null) {
            mCallConnection = new CallStatusServiceConnection();
            boolean failedConnection = false;

            Intent intent = getCallStatusServiceIntent();
            if (!mContext.bindService(intent, mCallConnection, Context.BIND_AUTO_CREATE)) {
                Log.d(TAG, "Bind service failed!");
                mCallConnection = null;
                failedConnection = true;
            } else {
                Log.d(TAG, "Bind service successfully!");
            }
        } else {
            Log.d(TAG, "Alreay bind service!");
        }
    }

    private void unbindCallStatusService() {
        Log.i(TAG, "unbindCallStatusService.");
        if (mCallStatusService != null) {
            unregisterCallListener();
            mContext.unbindService(mCallConnection);
            mCallStatusService = null;
        }
        mCallConnection = null;
    }

    private Intent getCallStatusServiceIntent() {
        Log.i(TAG, "getCallStatusServiceIntent.");
        final Intent intent = new Intent(ICallStatusService.class.getName());
        final ComponentName component = new ComponentName("com.mediatek.rcs.phone",
                "com.mediatek.rcs.incallui.service.CallStatusService");
        intent.setComponent(component);
        return intent;
    }

    private final IServiceMessageCallback.Stub mCallServiceListener =
                                              new IServiceMessageCallback.Stub() {
        @Override
        public void updateMsgStatus(final String name, final String status, final String time) {
            try {
                // updatedMessageInfo(name, status, time);
                Log.i(TAG, "updateMsgStatus: name = " + name + ", status = " + status
                        + ", time = " + time);
                mContext.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        mShowCallTimeText.setVisibility(View.VISIBLE);
                        mShowCallTimeText.setText(status + "  " + time);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error updateMsgStatus", e);
            }
        }

        @Override
        public void stopfromClient() {
            try {
                Log.i(TAG, "[IServiceMessageCallback]stopfromClient");
                unbindCallStatusService();
                mContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        mShowCallTimeText.setVisibility(View.GONE);
                        mIsNeedShowCallTime = false;
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error stopfromClient", e);
            }
        }
    };

    private void registerCallListener() {
        Log.d(TAG, "registerCallback.");
        try {
            if (mCallStatusService != null) {
                mCallStatusService.registerMessageCallback(mCallServiceListener);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void unregisterCallListener() {
        Log.d(TAG, "unregisterCallback.");
        try {
            if (mCallStatusService != null) {
                mCallStatusService.unregisterMessageCallback(mCallServiceListener);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Used for connect CallStatus from Call service.
     *
     */
    private class CallStatusServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "onServiceConnected.");
            if (mCallStatusService != null) {
                Log.d(TAG, "Service alreay connected, service = " + mCallStatusService);
                return;
            }
            mCallStatusService = ICallStatusService.Stub.asInterface(service);
            registerCallListener();
        }

        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "onServiceDisconnected.");
            if (mCallStatusService != null) {
                unregisterCallListener();
                mContext.unbindService(mCallConnection);
                mCallStatusService = null;
                mUiHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        mShowCallTimeText.setVisibility(View.GONE);
                        mIsNeedShowCallTime = false;
                    }
                }, 1500);
            }
            mCallConnection = null;
        }
    }

    @Override
    public void onServiceStateChanged(final boolean activated, final boolean configured,
            final boolean registered) {
        mContext.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                boolean enable = activated && configured;
                if (mServiceEnabled != enable) {
                    mServiceEnabled = enable;
                    invalidateOptionMenu();
                    if (mIsGroupChat) {
                        setChatActive(mServiceEnabled);
                    }
                    
                    // reset the burned message mode
                    if (mTextEditor != null && !mTextEditor.getText().toString().isEmpty()) {
                        mSendButtonIpMessage.setImageDrawable(mPluginContext.getResources()
                                .getDrawable(R.drawable.ic_send_ipmsg));
                    } else {
                        mSendButtonIpMessage.setImageDrawable(mPluginContext.getResources()
                                .getDrawable(R.drawable.ic_send_sms_unsend));
                    }
                    mOldcontact = null;
                    mDisplayBurned = false;
                    RcsMessageConfig.setEditingDisplayBurnedMsg(mDisplayBurned);
                    
                    if (mCallback != null) {
                        mCallback.resetSharePanel();
                    }

                    mContext.closeContextMenu();
                    ListAdapter adapter = mListView.getAdapter();
                    if (adapter instanceof CursorAdapter) {
                        CursorAdapter cursorAdapter = (CursorAdapter) adapter;
                        cursorAdapter.notifyDataSetChanged();
                    }
                    setEmojiActive(enable);
                    

                }
            }
        });
    }

    RCSGroup.SimpleGroupActionListener mGroupListener = new RCSGroup.SimpleGroupActionListener() {

        @Override
        public void onGroupAborted() {
            // TODO Auto-generated method stub
            Log.d(TAG, "onGroupAborted");
            setChatActive(false);
            // Toast.makeText(mContext, "add onGroupAborted",
            // Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onSubjectModified(String newSubject) {
            // TODO Auto-generated method stub
            Log.d(TAG, "onSubjectModified: " + newSubject);
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    mCallback.updateIpTitle();
                }
            });
        }

        @Override
        public void onMeRemoved() {
            // TODO Auto-generated method stub
            Log.d(TAG, "onMeRemoved");
            mContext.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    setChatActive(false);
                }
            });
        }
    };
}
