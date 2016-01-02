package com.mediatek.rcs.message.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import com.mediatek.mms.ipmessage.IpMessageListItem;
import com.mediatek.mms.ipmessage.IpMessageItem;
import com.mediatek.mms.ipmessage.IpMessageListItemCallback;
import com.mediatek.mms.ipmessage.IpUtilsCallback;
import com.mediatek.mms.ipmessage.IpMessageListAdapter;

//import com.mediatek.storage.StorageManagerEx;

import android.app.AlertDialog;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SqliteWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.Profile;
import android.provider.ContactsContract.QuickContact;
import android.provider.MediaStore.Video.Thumbnails;
import android.provider.Telephony.Sms;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.UnderlineSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.rcs.common.IpMessageConsts.IpMessageType;
import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.provider.ThreadMapCache;
import com.mediatek.rcs.common.provider.ThreadMapCache.MapInfo;
import com.mediatek.rcs.common.service.PortraitService;
import com.mediatek.rcs.common.service.PortraitService.Portrait;
import com.mediatek.rcs.common.service.PortraitService.UpdateListener;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.common.IpAttachMessage;
import com.mediatek.rcs.common.IpImageMessage;
import com.mediatek.rcs.common.IpMessage;
import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.common.IpMessageConsts.IpMessageStatus;
import com.mediatek.rcs.common.IpTextMessage;
import com.mediatek.rcs.common.IpVCardMessage;
import com.mediatek.rcs.common.IpVideoMessage;
import com.mediatek.rcs.common.IpVoiceMessage;
import com.mediatek.rcs.common.MessageStatusUtils.IFileTransfer.Status;
import com.mediatek.rcs.common.RCSMessageManager;

import com.mediatek.rcs.message.R;
import com.mediatek.rcs.message.ui.MsgListItem;
import com.mediatek.rcs.message.utils.RcsMessageUtils;
import com.mediatek.rcs.message.ui.RcsIpMsgContentShowActivity;

import com.google.android.mms.ContentType;

import com.mediatek.rcs.common.IpGeolocMessage;
import com.mediatek.rcs.message.data.RcsProfile;
import com.mediatek.rcs.message.group.PortraitManager;
import com.mediatek.rcs.message.group.PortraitManager.MemberInfo;
import com.mediatek.rcs.message.group.PortraitManager.onMemberInfoChangedListener;
import com.mediatek.rcs.message.location.GeoLocService;
import com.mediatek.rcs.message.location.GeoLocUtils;
import com.mediatek.rcs.message.location.GeoLocXmlParser;
import com.mediatek.rcs.message.utils.RcsVcardUtils;
import com.android.vcard.VCardEntryHandler;
import com.android.vcard.VCardEntry;
import com.mediatek.rcs.message.utils.RcsVcardParserResult;
import com.mediatek.rcs.message.utils.RcsVcardData;

public class RcsMessageListItem extends IpMessageListItem implements onMemberInfoChangedListener, OnClickListener{
    private static String TAG = "RcsMessageListItem";
    
    // / M: add for ip message, download file, accept or reject
    private View mIpmsgFileDownloadContrller; // ipmsg_file_downloading_controller_view
    private TextView mIpmsgResendButton; // ipmsg_resend
    private Button mIpmsgAcceptButton; // ipmsg_accept
    private Button mIpmsgRejectButton; // ipmsg_reject
    private View mIpmsgFileDownloadView; // ipmsg_file_download
    private TextView mIpmsgFileSize; // ipmsg_download_file_size
    private ImageView mIpmsgCancelDownloadButton; // ipmsg_download_file_cancel
    private ImageView mIpmsgPauseResumeButton; // ipmsg_download_file_resume
    private ProgressBar mIpmsgDownloadFileProgress; // ipmsg_download_file_progress

    private View mIpImageView; // ip_image
    private ImageView mImageContent; // image_content
    private View mIpImageSizeBg; // image_size_bg
    private ImageView mActionButton; // action_btn
    private TextView mContentSize; // content_size
    private ImageView mDeleteBARMsgIndicator; // delete msg indicator
    // private View mCaptionSeparator; // caption_separator
    private TextView mCaption; // text_caption
    private ImageView mMediaPlayView;
    private TextView mVideoDur;
    // private View mVideoCaptionSeparator; // caption_separator
    // private TextView mVideoCaption; // text_caption
    // / M: add for audio
    private View mIpAudioView; // ip_audio
    private ImageView mAudioIcon; // ip_audio_icon
    private TextView mAudioInfo; // audio_info
    private TextView mAudioDur; // audio_dur
    // / M: add for vcard
    private View mIpVCardView;
    private TextView mVCardInfo;
    private ImageView mVCardPortrait;
    // / M: add for vcalendar
    private View mIpVCalendarView;
    private TextView mVCalendarInfo;

    private View mIpGeolocView;
    private TextView mBodyTextView;
    private TextView mSmsRecipient;
    private LinearLayout mSmsInfo;
    private QuickContactBadge mSenderPhoto;
    private TextView mSenderName;

    private long mIpMessageId;
    private long mMsgId;

    // ip burned message
    private int delayTimerLen;
    Timer deleteBARMSGTimer = null;
    BurnedMsgTask timerTask;
    //Handler burnedMsgHandler;
    private static final int EVENT_DELETE_BAR_MSG = 1001;
    private static final int[] ipbarmsgshareIconArr = { R.drawable.ic_ipbar_timer_1,
        R.drawable.ic_ipbar_timer_2,R.drawable.ic_ipbar_timer_3, R.drawable.ic_ipbar_timer_4, R.drawable.ic_ipbar_timer_5};
    private int REQUEST_CODE_IPMSG_RECORD_AUDIO = 220;
    
    public static final int MSG_LIST_RESEND_IPMSG = 20;
    static final int MSG_LIST_NEED_REFRASH = 100;
    public static final int MESSAGE_LIST_REFRASH_WITH_CLEAR_CACHE = 1;

    private final static float MAX_SCALE = 0.3f;
    private final static float MIN_SCALE = 0.2f;
    private final static float COMP_NUMBER = 0.5f;
    private final static float HEIGHT_SCALE = 0.25f;
    
    private final static int CHAT_TYPE_ONE2ONE = 1;
    private final static int CHAT_TYPE_ONE2MULTI = 2;
    private final static int CHAT_TYPE_GROUP = 3;
    static int mChatType = CHAT_TYPE_ONE2ONE;

    private int mTimerNum = 5;
    public IpMessageListItemCallback mIpMessageItemCallback;
    private RcsMessageListAdapter mMessageListAdapter;
    public Context mContext;
    public Context mRcsContext;
    public RcsMessageItem mRcsMessageItem;
    public LinearLayout mItemView;
    private View mMsgListItem;

    private int mDirection;
    private MemberInfo mMemberInfo;
    private long mThreadId = -1;
    private boolean mIsGroupItem;
    private String mChatId;
    private boolean mVisible; //true between onbind and ondetached
    private boolean burnedAudioMsg = false;
    
    private TextView mSystemEventText;
    private View mMessageContent;
    private boolean mIsDeleteMode = false;
    private boolean mIsLastItem = false;
    
    public static final int text_view = 1;
    public static final int date_view = 2;
    public static final int sim_status = 3;
//    public static final int account_icon = 4;
    public static final int locked_indicator = 5;
    public static final int delivered_indicator = 6;
    public static final int details_indicator = 7;
    public static final int avatar = 8;
    public static final int message_block = 9;
    public static final int select_check_box = 10;
    public static final int time_divider = 11;
    public static final int time_divider_str = 12;
    public static final int unread_divider = 13;
    public static final int unread_divider_str = 14;
    public static final int on_line_divider = 15;
    public static final int on_line_divider_str = 16;
    public static final int sim_divider = 17;
    public static final int text_expire = 18;
    public static final int sender_name = 19;
    public static final int sender_name_separator = 20;
    public static final int sender_photo = 21;
    public static final int send_time_txt = 22;
    public static final int double_time_layout = 23;
    public static final int mms_file_attachment_view_stub = 24;
    public static final int file_attachment_view = 25;
    public static final int file_attachment_thumbnail = 26;
    public static final int file_attachment_name_info = 27;
    public static final int file_attachment_name_info2 = 28;
    public static final int file_attachment_thumbnail2 = 29;
    public static final int file_attachment_size_info = 30;
    public static final int mms_view = 31;
    public static final int mms_layout_view_stub = 32;
    public static final int image_view = 33;
    public static final int play_slideshow_button = 34;
    public static final int mms_downloading_view_stub = 35;
    public static final int btn_download_msg = 36;
    public static final int label_downloading = 37;
    public static final int mms_download_controls = 38;
    public static final int status_panel = 39;
    
    public static final int TAG_THREAD_ID = R.id.msg_list_item_recv;
    public static final int TAG_ITEM_TYPE = R.id.msg_list_item_send;
    public static final int TYPE_INCOMING = 1;
    public static final int TYPE_OUTGOING = 2;
    public static final int TYPE_SYSTEM_EVENT = 3;
    
    private static final int MSG_EVENT_IPMSG_LOADED = 1;

    public RcsMessageListItem(Context context) {
        mRcsContext = context;
    }

    public boolean IpMessageListItemInit() {
        return true;
    }

    @Override
    public boolean onIpFinishInflate(Context context, TextView bodyTextView,
            IpMessageListItemCallback ipMessageItemCallback, Handler handler, LinearLayout itemView) {
        mContext = context;
        mIpMessageItemCallback = ipMessageItemCallback;
        mItemView = itemView;
        int childCount = itemView.getChildCount();
        for (int index = 0; index < childCount; index ++) {
            View child = itemView.getChildAt(index);
            String className = child.getClass().getName();
            String string = child.toString();
            if (className.equals("com.mediatek.rcs.message.ui.MsgListItem")) {
                mMsgListItem =  child;
                mSenderPhoto = (QuickContactBadge)child.findViewById(R.id.sender_photo);
                mSenderName = (TextView)child.findViewById(R.id.sender_name);
                mSystemEventText = (TextView)child.findViewById(R.id.systen_event_text);
                mMessageContent = child.findViewById(R.id.message_content);
                mBodyTextView = (TextView)child.findViewById(R.id.text_view);
                break;
            }
        }

        initIpMessageResource();
        return false;
    }
    
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
            case MSG_EVENT_IPMSG_LOADED:
                // ip message loaded;
                mRcsMessageItem.setGetIpMessageFinishMessage(null);
                if (mRcsMessageItem.getIpMessage() != null) {
                    bindIpmsg(mIsDeleteMode);
                }
                break;
            default:
                break;
            }
        }
    };

    @Override
    public View findIpView(int id, View parentView) {
        if (mMsgListItem == null) {
            return null;
        }
        if (mDirection == TYPE_SYSTEM_EVENT) {
            return null;
        }
        switch (id) {
            case text_view:
                return parentView.findViewById(R.id.text_view);
            case date_view:
                return parentView.findViewById(R.id.date_view);
            case sim_status:
                return parentView.findViewById(R.id.sim_status);
//            case account_icon:
//                return parentView.findViewById(R.id.account_icon);
            case locked_indicator:
                return parentView.findViewById(R.id.locked_indicator);
            case delivered_indicator:
                return parentView.findViewById(R.id.delivered_indicator);
            case details_indicator:
                return parentView.findViewById(R.id.details_indicator);
            case avatar:
                return null;
//                return parentView.findViewById(R.id.avatar);
            case select_check_box:
                return parentView.findViewById(R.id.select_check_box);
            case time_divider:
                return parentView.findViewById(R.id.time_divider);
            case time_divider_str:
                return parentView.findViewById(R.id.time_divider_str);
            case unread_divider:
                return parentView.findViewById(R.id.unread_divider);
            case unread_divider_str:
                return parentView.findViewById(R.id.unread_divider_str);
            case on_line_divider:
                return parentView.findViewById(R.id.on_line_divider);
            case on_line_divider_str:
                return parentView.findViewById(R.id.on_line_divider_str);
            case sim_divider:
                return parentView.findViewById(R.id.sim_divider);
            case text_expire:
                return parentView.findViewById(R.id.text_expire);
            case sender_name:
                return null;//parentView.findViewById(R.id.sender_name);
            case sender_name_separator:
                return parentView.findViewById(R.id.sender_name_separator);
            case sender_photo:
                return null;//parentView.findViewById(R.id.sender_photo);
            case send_time_txt:
                return null;
            case double_time_layout:
                return null;
            case mms_file_attachment_view_stub:
                return parentView.findViewById(R.id.mms_file_attachment_view_stub);
            case file_attachment_view:
                return parentView.findViewById(R.id.file_attachment_view);
            case file_attachment_thumbnail:
                return parentView.findViewById(R.id.file_attachment_thumbnail);
            case file_attachment_name_info:
                return parentView.findViewById(R.id.file_attachment_name_info);
            case file_attachment_name_info2:
                return parentView.findViewById(R.id.file_attachment_name_info2);
            case file_attachment_thumbnail2:
                return parentView.findViewById(R.id.file_attachment_thumbnail2);
            case file_attachment_size_info:
                return parentView.findViewById(R.id.file_attachment_size_info);
            case mms_view:
                return parentView.findViewById(R.id.mms_view);
            case mms_layout_view_stub:
                return parentView.findViewById(R.id.mms_layout_view_stub);
            case image_view:
                return parentView.findViewById(R.id.image_view);
            case play_slideshow_button:
                return parentView.findViewById(R.id.play_slideshow_button);
            case mms_downloading_view_stub:
                return parentView.findViewById(R.id.mms_downloading_view_stub);
            case btn_download_msg:
                return parentView.findViewById(R.id.btn_download_msg);
            case label_downloading:
                return parentView.findViewById(R.id.label_downloading);
            case mms_download_controls:
                return parentView.findViewById(R.id.mms_download_controls);
            case status_panel:
                return parentView.findViewById(R.id.status_panel);
            default:
        }
        return null;
    }
    
    private void initIpMessageResource() {
        if (mMsgListItem != null) {

            /// M: add for audio
            mIpAudioView = (View) mMsgListItem.findViewById(R.id.ip_audio);
            mAudioIcon = (ImageView) mMsgListItem.findViewById(R.id.ip_audio_icon);
            mAudioInfo = (TextView) mMsgListItem.findViewById(R.id.audio_info);
            mAudioDur = (TextView) mMsgListItem.findViewById(R.id.audio_dur);

            /// M: add for image and video
            mIpImageView = (View) mMsgListItem.findViewById(R.id.ip_image);
            mImageContent = (ImageView) mMsgListItem.findViewById(R.id.image_content);
            mVideoDur = (TextView) mMsgListItem.findViewById(R.id.video_dur);
            mIpImageSizeBg = (View) mMsgListItem.findViewById(R.id.image_size_bg);
            //mActionButton = (ImageView) findViewById(R.id.action_btn);
            mContentSize = (TextView) mMsgListItem.findViewById(R.id.content_size);
            mDeleteBARMsgIndicator = (ImageView) mMsgListItem.findViewById(R.id.deleteBARMsg_indicator);
            /// M: add for vCard
            mIpVCardView = (View) mMsgListItem.findViewById(R.id.ip_vcard);
            mVCardInfo = (TextView) mMsgListItem.findViewById(R.id.vcard_info);
            mVCardPortrait = (ImageView) mMsgListItem.findViewById(R.id.ip_vcard_icon);

            mIpGeolocView = (View) mMsgListItem.findViewById(R.id.ip_geoloc);
            
            mCaption = (TextView) mMsgListItem.findViewById(R.id.text_caption);
            mBodyTextView = (TextView) mMsgListItem.findViewById(R.id.text_view);
            mMediaPlayView = (ImageView) mMsgListItem.findViewById(R.id.video_media_paly);
            
            mSmsInfo = (LinearLayout) mMsgListItem.findViewById(R.id.sms_info);
            mSmsRecipient = (TextView) mMsgListItem.findViewById(R.id.sms_to);
        }
    }

    public boolean onIpBind(IpMessageItem ipMessageItem, long msgId, long ipMessageId, boolean isDeleteMode) {
        Log.d(TAG, "bindView(): IpMessageId = " + ipMessageId);
        mIsDeleteMode = isDeleteMode;
        mRcsMessageItem = (RcsMessageItem) ipMessageItem;
        mVisible = true;
        delayTimerLen = mRcsMessageItem.mBurnedMsgTimerNum;
        if (mMsgListItem != null) {
            Long id = (Long)mMsgListItem.getTag(TAG_THREAD_ID);
            mThreadId = mRcsMessageItem.mThreadId;
            mChatId = RcsMessageUtils.getGroupChatIdByThread(mThreadId);
            if (!TextUtils.isEmpty(mChatId)) {
                mIsGroupItem = true;
            }
            mDirection =(Integer)mMsgListItem.getTag(TAG_ITEM_TYPE);
        }

        
        //system event
        if (mRcsMessageItem != null && mRcsMessageItem.mIsSystemEvent) {
            bindIpSystemEvent(mRcsMessageItem, isDeleteMode);
            return true;
        }
        /*
        if (mRcsMessageItem.isReceivedBurnedMessage()) {
            LinearLayout mMsgContainer = (LinearLayout) mMsgListItem.findViewById(R.id.mms_layout_view_parent);
            mMsgContainer.setVisibility(View.GONE);
            //view.setVisibility(View.INVISIBLE);
            ImageView viewImage = (ImageView) mMsgListItem.findViewById(R.id.hide_ip_bar_message);
            if (viewImage != null) {
                viewImage.setVisibility(View.VISIBLE);
                viewImage.setOnClickListener(new OnClickListener() {
                     public void onClick(View v) {
                          Intent ipMsgIntent = new Intent(ContextCacher.getPluginContext(), RcsIpMsgContentShowActivity.class);
                          ipMsgIntent.putExtra("ipmsg_id", mIpMessageId);
                          ipMsgIntent.putExtra("thread_id", mThreadId);
                          ipMsgIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                          try {
                              ContextCacher.getPluginContext().startActivity(ipMsgIntent);
                          } catch (android.content.ActivityNotFoundException e) {
                               Log.w(TAG, "onItemClick,Cannot open file: ");
                          }
                     }
                });
            }
        }
        */
        bindPartipantInfo(mRcsMessageItem, isDeleteMode);

        if (ipMessageId != 0) {
            mIpMessageId = ipMessageId;
            mMsgId = msgId;
            bindIpmsg(isDeleteMode);
            return true;
        }
        return false;
//        return bindSmsItem(mRcsMessageItem);
    }

    public boolean onIpDetachedFromWindow() {
        //move to onIpUnbind()
        return false;
    }

    @Override
    public void onIpUnbind() {
        if (mRcsMessageItem == null) {
            return;
        }
        mRcsMessageItem.setGetIpMessageFinishMessage(null);
        if (mMemberInfo != null) {
            mMemberInfo.removeChangedListener(this);
            mMemberInfo = null;
        }
        Log.d(TAG, "drawDeleteBARMsgIndicator: onIpDetachedFromWindow()  mTimeTask = " + mTimeTask);
        if (mTimeTask != null) {
            mTimeTask.setHandler(null);
        }
        if (mDeleteBARMsgIndicator != null) {
            mDeleteBARMsgIndicator.setImageDrawable(null);
        }
        mVisible = false;
    }

    public boolean onIpMessageListItemClick() {
        if (mIsDeleteMode) {
            return false;
        }
         IpMessage ipMessage = RCSMessageManager.getInstance(mContext)
            .getIpMsgInfo(mThreadId, mIpMessageId);
         Log.w(TAG, " [BurnedMsg]: onIpMessageListItemClick(), mThreadId = " + mThreadId +
            "mIpMessageId = " + mIpMessageId + "ipMessage = " + ipMessage);
         if (mIpMessageId != 0 && mRcsMessageItem.mBoxId == Sms.MESSAGE_TYPE_FAILED) {
             if (!mMessageListAdapter.isChatActive()) {
                 showIpMessageDetail(mRcsMessageItem);
             } else {
                 AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                 builder.setMessage(mRcsContext.getString(R.string.retry_indicator))
                 .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                     
                     @Override
                     public void onClick(DialogInterface arg0, int arg1) {
                         // TODO Auto-generated method stub
                         if (mIpMessageId > 0) {
                             RCSMessageManager.getInstance(mContext).resendMessage(mMsgId, mThreadId);
                         } else {
                             RCSMessageManager.getInstance(mContext).reSendFileTransfer(mIpMessageId, mThreadId);
                         }
                     }
                 })
                 .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                     
                     @Override
                     public void onClick(DialogInterface arg0, int arg1) {
                         // TODO Auto-generated method stub
                     }
                 }).create().show();
             }
             return true;
         }
         
         if(ipMessage != null && (ipMessage.getType() == IpMessageType.VIDEO ||
            ipMessage.getType() == IpMessageType.PICTURE)) {

            /* this ipmessage's type is video or image, so we will show it or download it
            or redownload it, which action will be determined by RCSStatus*/

            Log.d(TAG, "onIpMessageListItemClick(), image or video ");
            Log.d(TAG, "onIpMessageListItemClick(), ipMessage.getStatus() " + ipMessage.getStatus());
            Log.d(TAG, "onIpMessageListItemClick(), ipMessage.getRCSStatus() " + ((IpAttachMessage)ipMessage).getRcsStatus());

            if ((ipMessage.getStatus() == Sms.MESSAGE_TYPE_OUTBOX || ipMessage.getStatus() == Sms.MESSAGE_TYPE_SENT)
                    || (ipMessage.getStatus() == Sms.MESSAGE_TYPE_INBOX )) {
                    //we call show this media
                    //Log.d(TAG, " DownloadUI: onIpMessageListItemClick(),have been downloaded, show it directly");
                    //Log.d(TAG, " DownloadUI: onIpMessageListItemClick(),send image, so can show image or video ");
	            	 if (  ipMessage instanceof IpAttachMessage && (ipMessage.getStatus() == Sms.MESSAGE_TYPE_INBOX )&&
	            			 (((IpAttachMessage) ipMessage).getRcsStatus() == Status.WAITING 
	            					 || ((IpAttachMessage) ipMessage).getRcsStatus() == Status.FAILED)){
		                boolean serviceReady = RCSServiceManager.getInstance().serviceIsReady();
		                if(!serviceReady) {
		        	        Toast.makeText(mContext, mRcsContext.getString(R.string.download_file_fail),
		        	                Toast.LENGTH_SHORT).show();
		        	        return true;
		                }
	            	 }
                    Intent ipMsgIntent = new Intent(ContextCacher.getPluginContext(), RcsIpMsgContentShowActivity.class);
                    ipMsgIntent.putExtra("ipmsg_id", mIpMessageId);
                    ipMsgIntent.putExtra("thread_id", mThreadId);
                    ipMsgIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Log.d(TAG, " [BurnedMsg]: onIpMessageListItemClick() mIpMessageId = " + mIpMessageId + " mThreadId = " + mThreadId);
                    try {
                        ContextCacher.getPluginContext().startActivity(ipMsgIntent);
                    } catch (android.content.ActivityNotFoundException e) {
                         Log.w(TAG, "onItemClick,Cannot open file: ");
                    }
            }
             return true;
         } else if (ipMessage != null && ipMessage.getType() == IpMessageType.VOICE) {
            //this is a audio, so click it and play it
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String filePath = ((IpAttachMessage)ipMessage).getPath();
            File file = new File(filePath);
            Uri audioUri = Uri.fromFile(file);
            Log.w(TAG, "audioUri = " + audioUri);
            intent.setDataAndType(audioUri, ContentType.AUDIO_AMR);
            try {
                mContext.startActivity(intent);
            } catch (android.content.ActivityNotFoundException e) {
                 Log.w(TAG, "onItemClick,Cannot open file: ");
            }
            return true;
         } else if (ipMessage != null && ipMessage.getType() == IpMessageType.GEOLOC) {
            //this is a geolocation info, so will open map
            if (((IpAttachMessage)ipMessage).getPath() != null) {
                GeoLocXmlParser parser = GeoLocUtils.parseGeoLocXml(((IpAttachMessage)ipMessage).getPath());
                double latitude = parser.getLatitude();
                double longitude = parser.getLongitude();
                Log.d(TAG, "parseGeoLocXml:latitude=" + latitude + ",longitude=" + longitude);

                Toast.makeText(mContext, "latitude = " + latitude + ",longitude=" + longitude, 
                    Toast.LENGTH_SHORT).show();
                
                if (latitude != 0.0 || longitude != 0.0) {
                      Uri uri = Uri.parse("geo:" + latitude + "," + longitude);  
                      Intent it = new Intent(Intent.ACTION_VIEW,uri);  
                      mContext.startActivity(it);
                } else {
                    Toast.makeText(mContext, mRcsContext.getString(R.string.geoloc_map_failed), Toast.LENGTH_SHORT).show();
                }
            }
            return true;
         } else if (ipMessage != null && ipMessage.getType() == IpMessageType.VCARD) {
            //this is a vcard file, import or prview it

            final IpMessage ipMsg = ipMessage;

            int entryCount = ((IpVCardMessage)ipMessage).getEntryCount();
            if (entryCount == 0) {
                // there is no entrycount in ipMessage
                entryCount = RcsVcardUtils.getVcardEntryCount(((IpAttachMessage)ipMessage).getPath());
                ((IpVCardMessage)ipMessage).setEntryCount(entryCount);
         }
            
            Log.d(TAG,"onItemClick, vcard entry count = " + entryCount);
            if (entryCount == 1) {
                // preview it
                Uri uri = Uri.fromFile(new File(((IpVCardMessage)ipMessage).getPath()));
                Intent intent = new Intent("android.intent.action.rcs.contacts.VCardViewActivity");
                intent.setDataAndType(uri,"text/x-vCard".toLowerCase());
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                mContext.startActivity(intent);
            } else {
                Resources res = mRcsContext.getResources();
                AlertDialog.Builder b = new AlertDialog.Builder(mContext);
                b.setTitle(res.getString(R.string.multi_cantacts_name))
                    .setMessage(res.getString(R.string.multi_contacts_notification));
                b.setCancelable(true);
                b.setPositiveButton(android.R.string.ok,
                   new DialogInterface.OnClickListener() {
                        public final void onClick(DialogInterface dialog, int which) {
                            importVcard(ipMsg); 
                        }   
                   }
                ); 

                b.setNegativeButton(android.R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        public final void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                b.create().show();
             }
          return true;
         }
        // Host to handle it
        return false;
    }

    private void importVcard(IpMessage ipMessage) {
        Log.d(TAG,"importVcard(), entry, = ");
        final File tempVCard = new File(((IpAttachMessage)ipMessage).getPath());
        if (!tempVCard.exists() || tempVCard.length() <= 0) {
            Log.e(TAG, "importVCard fail! because of error path");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(tempVCard), "text/x-vCard".toLowerCase());
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        mContext.startActivity(intent);
    }

    public void setIpMessageListItemAdapter(IpMessageListAdapter adapter) {
        mMessageListAdapter = (RcsMessageListAdapter) adapter;
    }

    public boolean onIpSetBodyTextSize(float size) {
        return false;
    }

    public boolean onIpBindDefault() {
        hideFileTranfserViews();
        return false;
    }

    public boolean onIpSetMmsImage() {
        return false;
    }

    private void bindSenderPhoto(int chatType) {
        switch (chatType) {
        case CHAT_TYPE_ONE2ONE:
            showSenderPhoto(true);
            showSenderName(false);
            break;
        case CHAT_TYPE_GROUP:
            showSenderPhoto(true);
            showSenderName(false);
            break;

        default:
            showSenderPhoto(false);
            showSenderName(false);
            break;
        }
    }
    
    private void showSenderPhoto(boolean show) {
        if (mSenderPhoto != null) {
            int visible = show ? View.VISIBLE : View.GONE;
            mSenderPhoto.setVisibility(visible);
            //TODO: get address image
//            mSenderPhoto.setImageBitmap(bm);
//            mSenderPhoto.assignContactFromPhone(mRcsMessageItem.mAddress, false);
            mSenderPhoto.setOnClickListener(this);
        }
    }
    private void showSenderName(boolean show) {
        if (mSenderName != null) {
            int visible = show ? View.VISIBLE : View.GONE;
            mSenderName.setVisibility(visible);
            //TODO: get address Name
            if (show) {
                mSenderName.setText(mRcsMessageItem.mAddress);
            }
        }
    }

    // / M: add for ipmessage
    public boolean bindIpmsg(boolean isDeleteMode) {
        // / M: add for ipmessage, notification listener
        Log.d(TAG, " [BurnedMsg] bindIpmsg(): msgId = " + mMsgId);
        if (mMessageContent != null) {
            mMessageContent.setVisibility(View.VISIBLE);
        }
        if (mSystemEventText != null) {
            mSystemEventText.setVisibility(View.GONE);
        }
//        IpMessage ipMessage = RCSMessageManager.getInstance(mContext).getIpMsgInfo(mMsgId);
        IpMessage ipMessage = mRcsMessageItem.getIpMessage();
        if (null == ipMessage) {
            Log.d(TAG, "bindIpmsg(): ip message is null! not loaded");
//            mIpMessageItemCallback.setTextMessage("refreshing");
            if (mBodyTextView != null) {
                mBodyTextView.setText("Refreshing");
            }
            hideFileTranfserViews();
            if (mRcsMessageItem.mIpMessageId > 0) {
                if (!TextUtils.isEmpty(mRcsMessageItem.mBody)) {
                    setIpTextItem(mRcsMessageItem.mBody, isDeleteMode);
                    mIpMessageItemCallback.setSubDateView(null);
                    return true;
                }
            }
            mRcsMessageItem.setGetIpMessageFinishMessage(mHandler.obtainMessage(MSG_EVENT_IPMSG_LOADED));
            return false;
        }

        // / M: hide file transfer view
        if (mIpmsgFileDownloadContrller != null) {
            mIpmsgFileDownloadContrller.setVisibility(View.GONE);
        }
        if (mIpmsgFileDownloadView != null) {
            mIpmsgFileDownloadView.setVisibility(View.GONE);
        }
        int ipMsgStatus = ipMessage.getStatus();

        boolean isFileTransferStatus = isFileTransferStatus(ipMsgStatus);
        boolean showContent = isIpMessageShowContent(ipMsgStatus);
        if (((mDirection == TYPE_INCOMING) && mRcsMessageItem.isReceivedBurnedMessage()) && burnedAudioMsg ) {
            burnedAudioMsg = false;
            
            if (ipMessage.getType() == IpMessageType.VOICE &&
                    ((IpAttachMessage)ipMessage).getRcsStatus() == Status.FINISHED) {
                
                RCSMessageManager.getInstance(ContextCacher.getHostContext()).sendBurnDeliveryReport(
                         ipMessage.getTo(),ipMessage.getMessageId());
                ContextCacher.getHostContext().getContentResolver().delete(Sms.CONTENT_URI, Sms.IPMSG_ID + " = " + mIpMessageId, null);
                removeIpMsgId(mIpMessageId);
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            Log.d(TAG, " [BurnedMsg]:  onPause(), ft audio run");
                            Thread.sleep(1000);
                            RCSMessageManager.getInstance(ContextCacher.getHostContext()).deleteStackMessage(mRcsMessageItem.mIpMessageId);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
            return false;
        }
        
        /*
        if (true || (mDirection == TYPE_INCOMING) && mRcsMessageItem.isReceivedBurnedMessage() && burnedAudioMsg ) {
            String[] READ_PROJECTION = new String[] {"read"};
            String selection = "read = 1";
            Cursor mSmsCursor = ContextCacher.getHostContext().getContentResolver().query(Sms.CONTENT_URI,
                    READ_PROJECTION, selection, null, null);
            if (mSmsCursor != null && mSmsCursor.getCount() > 0) {
                    if (mSmsCursor.moveToFirst()) {
                        int read = mSmsCursor.getInt(mSmsCursor.getColumnIndexOrThrow("read"));
                        Log.d(TAG, " DownloadUI: bindIpmsg:  mDirection = "+ mDirection + "  read = "+read);
                        if (read == 1){
                            if (ipMessage.getType() == IpMessageType.TEXT) {
                                RCSMessageManager.getInstance(ContextCacher.getHostContext()).deleteIpMsg(mRcsMessageItem.mIpMessageId);
                            } else {
                                RCSMessageManager.getInstance(ContextCacher.getHostContext()).deleteFTIpMsg(mRcsMessageItem.mIpMessageId);
                            }
                        }
                    }
            }
            return false;
        }
        */
        
        switch (ipMessage.getType()) {
            case IpMessageType.TEXT:
                setIpTextItem(ipMessage, isDeleteMode);
                break;
            case IpMessageType.PICTURE:
                setIpImageItem(ipMessage, isDeleteMode, isFileTransferStatus, showContent);
                break;
            case IpMessageType.VOICE:
                setIpVoiceItem(ipMessage, isDeleteMode, isFileTransferStatus, showContent);
                break;
            case IpMessageType.VIDEO:
                setIpVideoItem(ipMessage, isDeleteMode, isFileTransferStatus, showContent);
                break;
            case IpMessageType.VCARD:
                setIpVCardItem(ipMessage, isDeleteMode, isFileTransferStatus, showContent);
                break;
            case IpMessageType.GEOLOC:
                setIpGeolocItem(ipMessage, isDeleteMode, isFileTransferStatus, showContent);
                break;
            default:
                Log.e(TAG, "bindIpmsg(): Error IP message type. type = " + ipMessage.getType());
                break;
        }
        
        if(ipMessage != null ) {
            Log.d(TAG, " [BurnedMsg]: bindIpmsg: ipMessage.getBurnedMessage() = " + ipMessage.getBurnedMessage()
                    + " mDirection = "+ mDirection + "  getType = "+ipMessage.getType());
        }
        
        if (mDirection == TYPE_OUTGOING) {
            // send burned message logic
             if(ipMessage != null  && ipMessage.getBurnedMessage()) {

         		if(ipMessage.getType() == IpMessageType.TEXT &&  (IpMessageConsts.IpMessageStatus.SENT == ipMessage.getStatus()||
             				IpMessageConsts.IpMessageStatus.DELIVERED == ipMessage.getStatus())) {	

                     //Log.d(TAG, " drawDeleteBARMsgIndicator: text success = " + mIpMessageId + " initial num = "+mRcsMessageItem.mBurnedMsgTimerNum);
 
                     Log.d(TAG, " [BurnedMsg]: bindIpmsg: TYPE_OUTGOING ipMessage.getStatus() = " + ipMessage.getStatus());
        			

         			drawDeleteBARMsgIndicator(mRcsMessageItem);
                    saveIpMsgId(mIpMessageId);
                 } else if (  ipMessage instanceof IpAttachMessage && ((IpAttachMessage) ipMessage).getRcsStatus() == Status.FINISHED  ){

                     //Log.d(TAG, " drawDeleteBARMsgIndicator: FT success = " + mIpMessageId + " initial num = "+mRcsMessageItem.mBurnedMsgTimerNum);
                     
                    Log.d(TAG, " [BurnedMsg]: bindIpmsg: TYPE_OUTGOING ipMessage.getRcsStatus() = " + ((IpAttachMessage) ipMessage).getRcsStatus());

         			drawDeleteBARMsgIndicator(mRcsMessageItem);
                    saveIpMsgId(mIpMessageId);
                 }
                 
             }
             
        } else {
            // receive the burned msg,show mail icon
             if ( mRcsMessageItem.isReceivedBurnedMessage() ) {
//                 LinearLayout mMsgContainer = (LinearLayout) mMsgListItem.findViewById(R.id.mms_layout_view_parent);
//                 mMsgContainer.setVisibility(View.GONE);
                 if(mBodyTextView != null) {
                     mBodyTextView.setVisibility(View.GONE);
                 }
                 if(mIpImageView != null) {
                     mIpImageView.setVisibility(View.GONE);
                 }
                 ImageView viewImage = (ImageView) mMsgListItem.findViewById(R.id.hide_ip_bar_message);
                 if (viewImage != null) {
                     viewImage.setVisibility(View.VISIBLE);
                     LinearLayout mMsgContainer = (LinearLayout) mMsgListItem.findViewById(R.id.mms_layout_view_parent);
                     mMsgContainer.setOnClickListener(new OnClickListener() {
                          public void onClick(View v) {
                              try {
                            	  
	              	                boolean serviceReady = RCSServiceManager.getInstance().serviceIsReady();
	            	                if(!serviceReady) {
	            	        	        Toast.makeText(mContext, mRcsContext.getString(R.string.download_file_fail),
	            	        	                Toast.LENGTH_SHORT).show();
	            	        	        return;
	            	                }
            	                
                                    IpMessage ipMsg = mRcsMessageItem.getIpMessage();
                                    if(ipMsg != null && (ipMsg.getType() == IpMessageType.TEXT ||ipMsg.getType() == IpMessageType.VIDEO ||
                                         ipMsg.getType() == IpMessageType.PICTURE)) {
                                       Intent ipMsgIntent = new Intent(ContextCacher.getPluginContext(), RcsIpMsgContentShowActivity.class);
                                       ipMsgIntent.putExtra("ipmsg_id", mIpMessageId);
                                       ipMsgIntent.putExtra("thread_id", mThreadId);
                                       ipMsgIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                       ipMsgIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                       Log.d(TAG, " [BurnedMsg]: bindIpmsg: onClick ipMessage mIpMessageId = " + mIpMessageId + " mThreadId = " + mThreadId);
                                       mContext.startActivity(ipMsgIntent);
                                   } else if(ipMsg != null) {
                                      //this is a audio, so click it and play it
                                      Intent intent = new Intent(Intent.ACTION_VIEW);
                                      String filePath = ((IpAttachMessage)ipMsg).getPath();
                                      File file = new File(filePath);
                                      Uri audioUri = Uri.fromFile(file);
                                      Log.w(TAG, "audioUri = " + audioUri);
                                      burnedAudioMsg = true;
                                      Log.d(TAG, " [BurnedMsg]: bindIpmsg: onClick audio burnedAudioMsg = " + burnedAudioMsg);
                                      intent.setDataAndType(audioUri, ContentType.AUDIO_AMR);
                                      intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                      ((Activity)mContext).startActivityForResult(intent,REQUEST_CODE_IPMSG_RECORD_AUDIO);
                                      // mContext.startActivity(intent);
                                     saveIpMsgId(mIpMessageId);
                                   }
                                 
                                 /*
                                 ContentResolver resolver = ContextCacher.getHostContext().getContentResolver();
                                 String selection = "ipmsg_id = " + mIpMessageId;
                                 final ContentValues values = new ContentValues(1);
                                 values.put("read", 1);
                                 SqliteWrapper.update(ContextCacher.getHostContext(), resolver,
                                         Sms.CONTENT_URI, values,selection , null);
                                Log.d(TAG, " DownloadUI: bindIpmsg: onClick write sms db " );
                                */
                               } catch (android.content.ActivityNotFoundException e) {
                                    Log.w(TAG, "[BurnedMsg]: activity not found ");
                               }
                          }
                     });
                 }
             } else {
                 ImageView viewImage = (ImageView) mMsgListItem.findViewById(R.id.hide_ip_bar_message);
                 if (viewImage != null) {
                     viewImage.setVisibility(View.GONE);
                 }
             }
        }

        mIpMessageItemCallback.setSubDateView(null);
        return true;
    }
    
    private void saveIpMsgId( long msgId) {
        Log.d(TAG, "[BurnedMsg]: saveIpMsgId()");
        SharedPreferences sp = ContextCacher.getPluginContext().getSharedPreferences(IpMessageConsts.BurnedMsgStoreSP.PREFS_NAME, Context.MODE_WORLD_READABLE);
        Set<String> burnedMsgList = sp.getStringSet(IpMessageConsts.BurnedMsgStoreSP.PREF_PREFIX_KEY, null);
        if (burnedMsgList == null) {
            Log.d(TAG, "[BurnedMsg]: burnedMsgList is null" + "  msgId = "+msgId);
            burnedMsgList = new HashSet<String>();
            burnedMsgList.add(String.valueOf(msgId));
        } else {
            Log.d(TAG, "[BurnedMsg]: msgId = "+msgId + " burnedMsgList = "+burnedMsgList);
            boolean isInsert = true;
            burnedMsgList = new HashSet<String>(burnedMsgList);
            for (String id : burnedMsgList) {
                if ( Long.valueOf(id) == msgId) {
                    isInsert = false;
                    return;
                }
            }
            if ( isInsert)
                burnedMsgList.add(String.valueOf(msgId));
            Log.d(TAG, "[BurnedMsg]: isInsert = "+isInsert + " burnedMsgList = "+burnedMsgList);
        }


        // Set<String> burnedMsgList = new HashSet<String>();
        SharedPreferences.Editor prefs = sp.edit();
        prefs.putStringSet(IpMessageConsts.BurnedMsgStoreSP.PREF_PREFIX_KEY , burnedMsgList);
        prefs.apply();
        Log.d(TAG, "[BurnedMsg]: save success burnedMsgList = "+burnedMsgList);
    }

    private void removeIpMsgId( long msgId) {
        Log.d(TAG, "[BurnedMsg]: removeIpMsgId()");
        SharedPreferences sp = ContextCacher.getPluginContext().getSharedPreferences(IpMessageConsts.BurnedMsgStoreSP.PREFS_NAME, Context.MODE_WORLD_READABLE);
        Set<String> burnedMsgList = sp.getStringSet(IpMessageConsts.BurnedMsgStoreSP.PREF_PREFIX_KEY, null);
        if (burnedMsgList == null) {
            Log.d(TAG, "[BurnedMsg]: burnedMsgList is null");
            return;
        }
        burnedMsgList = new HashSet<String>(burnedMsgList);
        Log.d(TAG, "[BurnedMsg]: removeIpMsgId burnedMsgList = "+burnedMsgList);
        for (String id : burnedMsgList) {
            if ( Long.valueOf(id) == msgId) {
                burnedMsgList.remove(String.valueOf(msgId));
                break;
            }
        }

        // Set<String> burnedMsgList = new HashSet<String>();
        SharedPreferences.Editor prefs = sp.edit();
        prefs.putStringSet(IpMessageConsts.BurnedMsgStoreSP.PREF_PREFIX_KEY , burnedMsgList);
        prefs.apply();
        Log.d(TAG, "[BurnedMsg]: remove success burnedMsgList = "+burnedMsgList);
    }
    
    private boolean isFileTransferStatus(int ipMsgStatus) {
        switch (ipMsgStatus) {
        case IpMessageStatus.MO_INVITE:
        case IpMessageStatus.MO_SENDING:
        case IpMessageStatus.MO_REJECTED:
        case IpMessageStatus.MO_SENT:
        case IpMessageStatus.MO_CANCEL:
        case IpMessageStatus.MT_INVITED:
        case IpMessageStatus.MT_REJECT:
        case IpMessageStatus.MT_RECEIVING:
        case IpMessageStatus.MT_RECEIVED:
        case IpMessageStatus.MT_CANCEL:
        case IpMessageStatus.MO_PAUSE:
        case IpMessageStatus.MO_RESUME:
        case IpMessageStatus.MT_PAUSE:
        case IpMessageStatus.MT_RESUME:
            return true;
        default:
            return false;
        }
    }

    private boolean isIpMessageShowContent(int ipMsgStatus) {
        switch (ipMsgStatus) {
        case IpMessageStatus.MO_INVITE:
        case IpMessageStatus.MO_SENDING:
        case IpMessageStatus.MO_REJECTED:
        case IpMessageStatus.MO_SENT:
        case IpMessageStatus.MT_RECEIVED:
        case IpMessageStatus.MO_PAUSE:
        case IpMessageStatus.MO_RESUME:
            return true;
        case IpMessageStatus.MO_CANCEL:
        case IpMessageStatus.MT_INVITED:
        case IpMessageStatus.MT_REJECT:
        case IpMessageStatus.MT_RECEIVING:
        case IpMessageStatus.MT_CANCEL:
        case IpMessageStatus.MT_PAUSE:
        case IpMessageStatus.MT_RESUME:
            return false;
        default:
            return true;
        }
    }

    private void setIpTextItem(IpMessage ipMessage, boolean isDeleteMode) {
        Log.d(TAG, "setIpTextItem(): ipMessage = " + ipMessage);
        if (ipMessage == null) {
            Log.e(TAG, "setIpTextItem(): ipMessage = null");
            return;
        }
      IpTextMessage textMessage = (IpTextMessage) ipMessage;
      String body = textMessage.getBody();
      setIpTextItem(body, isDeleteMode);
    }
    private void setIpTextItem(String body, boolean isDeleteMode) {
        Log.d(TAG, "setIpTextItem(): body = " + body);
        if (TextUtils.isEmpty(body)) {
            Log.w(TAG, "setIpTextItem(): No message content!");
            return;
        }
        mIpMessageItemCallback.setTextMessage(body);
        hideFileTranfserViews();
    }

    private void hideFileTranfserViews() {
        if (mIpImageView != null) {
            mIpImageView.setVisibility(View.GONE);
        }
        if (mIpAudioView != null) {
            mIpAudioView.setVisibility(View.GONE);
        }
        if (mCaption != null) {
            mCaption.setVisibility(View.GONE);
        }
        if (mIpVCardView != null) {
            mIpVCardView.setVisibility(View.GONE);
        }
        if (mIpVCalendarView != null) {
            mIpVCalendarView.setVisibility(View.GONE);
        }
        if (mIpGeolocView != null) {
            mIpGeolocView.setVisibility(View.GONE);
        }
    }

    private void setIpImageItem(IpMessage ipMessage, boolean isDeleteMode,
            boolean isFileTransferStatus, boolean showContent) {
        //IpImageMessage imageMessage = (IpImageMessage) msgItem.mIpMessage;
        IpImageMessage imageMessage = (IpImageMessage) ipMessage;
        
        Log.d(TAG, "setIpImageItem(): message Id = " + mRcsMessageItem.mMsgId
                + " ipThumbPath:" + imageMessage.getThumbPath() + " imagePath:"
                + imageMessage.getPath());
        
        mIpImageView.setVisibility(View.VISIBLE);
        mIpImageSizeBg.setVisibility(View.VISIBLE);
        updateIpMessageVideoOrImageView(mRcsMessageItem, imageMessage);
        if (!setPicView(mRcsMessageItem, imageMessage.getPath())) {
                setPicView(mRcsMessageItem, imageMessage.getThumbPath());
            }
        /// M: add for ip message, hide text, audio, vCard, vCalendar, location view
        mBodyTextView.setVisibility(View.GONE);
        mIpAudioView.setVisibility(View.GONE);
        mIpVCardView.setVisibility(View.GONE);
        mMediaPlayView.setVisibility(View.GONE);
        mIpGeolocView.setVisibility(View.GONE); 
        mVideoDur.setVisibility(View.GONE);
    }

    private void updateIpMessageVideoOrImageView(RcsMessageItem msgItem,
            IpAttachMessage message) { 
        if (RCSMessageManager.getInstance(mContext).isDownloading(
                msgItem.mMsgId)) {              
                mContentSize.setVisibility(View.GONE);
            } else {
                mContentSize.setText(RcsMessageUtils.formatFileSize(message.getSize()));
                mContentSize.setVisibility(View.VISIBLE);
            }   
    }

    private boolean setPicView(RcsMessageItem msgItem, String filePath) {
        Log.d(TAG, "setPicView(): filePath = " + filePath + ", imageView = " + mImageContent);
        if (TextUtils.isEmpty(filePath) || null == mImageContent) {
            return false;
        }
        Bitmap bitmap = msgItem.getIpMessageBitmap();
        if (null == bitmap) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            bitmap = BitmapFactory.decodeFile(filePath, options);
            int width = options.outWidth;
            int height = options.outHeight;
            int w = options.outWidth;

            /// M: get screen width
            DisplayMetrics dm = new DisplayMetrics();
            int screenWidth = 0;
            int screenHeight = 0;
            WindowManager wmg = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            wmg.getDefaultDisplay().getMetrics(dm);
            if (dm.heightPixels > dm.widthPixels) {
                screenWidth = dm.widthPixels;
                screenHeight = dm.heightPixels;
            } else {
                screenWidth = dm.heightPixels;
                screenHeight = dm.widthPixels;
            }
            /// M: the returned bitmap's w/h is different with the input!
            if (width > screenWidth * MAX_SCALE) {
                w = (int) (screenWidth * MAX_SCALE);
                if (height * w / width < screenHeight * HEIGHT_SCALE) {
                bitmap = RcsMessageUtils.getBitmapByPath(filePath, options, w, height * w / width);
                msgItem.setIpMessageBitmapSize(w, height * w / width);
                } else {
                    bitmap =
                        RcsMessageUtils.getBitmapByPath(filePath, options, w, (int) (screenHeight * HEIGHT_SCALE));
                    msgItem.setIpMessageBitmapSize(w, (int) (screenHeight * HEIGHT_SCALE));
                }
            } else if (width > screenWidth * MIN_SCALE) {
                w = (int) (screenWidth * MAX_SCALE);
                if (height * w / width < screenHeight * HEIGHT_SCALE) {
                bitmap = RcsMessageUtils.getBitmapByPath(filePath, options, w, height * w / width);
                msgItem.setIpMessageBitmapSize(w, height * w / width);
            } else {
                    bitmap = RcsMessageUtils.getBitmapByPath(filePath, options, w, (int) (screenHeight * HEIGHT_SCALE));
                    msgItem.setIpMessageBitmapSize(w, (int) (screenHeight * HEIGHT_SCALE));
                }
            } else {
                if (height < screenHeight * HEIGHT_SCALE) {
                bitmap = RcsMessageUtils.getBitmapByPath(filePath, options, width, height);
                msgItem.setIpMessageBitmapSize(width, height);
                } else {
                    bitmap = RcsMessageUtils.getBitmapByPath(filePath, options, width, (int) (screenHeight * HEIGHT_SCALE));
                    msgItem.setIpMessageBitmapSize(width, (int) (screenHeight * HEIGHT_SCALE));
                }
            }

            msgItem.setIpMessageBitmapCache(bitmap);
        }

        if (null != bitmap) {
            ViewGroup.LayoutParams params = (ViewGroup.LayoutParams) mImageContent.getLayoutParams();
            params.height = msgItem.getIpMessageBitmapHeight();
            params.width = msgItem.getIpMessageBitmapWidth();
            mImageContent.setLayoutParams(params);
            mImageContent.setImageBitmap(bitmap);
            return true;
        } else {
            mImageContent.setImageResource(R.drawable.ic_missing_thumbnail_picture);
            return false;
        }
    }

    private void setIpVoiceItem(IpMessage ipMessage,
                                boolean isDeleteMode,
                                boolean isFileTransferStatus,
                                boolean showContent) {
        
        Log.d(TAG, "setIpVoiceItem(): message Id = " + mRcsMessageItem.mMsgId);
        //IpVoiceMessage voiceMessage = (IpVoiceMessage) msgItem.mIpMessage;

        IpVoiceMessage voiceMessage = (IpVoiceMessage) ipMessage;
        
//        mAudioOrVcardIcon.setImageResource(R.drawable.ic_soundrecorder);
        /// M: add for ip message, show audio view
        mIpAudioView.setVisibility(View.VISIBLE);
        mAudioInfo.setVisibility(View.VISIBLE);
        mAudioIcon.setVisibility(View.VISIBLE);
        mAudioDur.setVisibility(View.VISIBLE);
        mAudioInfo.setText(RcsMessageUtils.formatFileSize(voiceMessage.getSize()));
        mAudioDur.setText(RcsMessageUtils.formatAudioTime(voiceMessage.getDuration()));

        /*
        if (voiceMessage.isInboxMsgDownloalable() && !isFileTransferStatus) {
            if (RCSMessageManager.getInstance(mContext).isDownloading(mRcsMessageItem.mMsgId)) {
                //if (null != mAudioDownloadProgressBar) {
                //    mAudioDownloadProgressBar.setVisibility(View.VISIBLE);
                //}
                //int progress = IpMessageUtils.getMessageManager(mContext).getDownloadProcess(msgItem.mMsgId);
                //if (null != mAudioDownloadProgressBar) {
                //    mAudioDownloadProgressBar.setProgress(progress);
                //}
                mAudioInfo.setVisibility(View.GONE);
            } else {
                //if (null != mAudioDownloadProgressBar) {
                //    mAudioDownloadProgressBar.setVisibility(View.GONE);
                //}
                mAudioInfo.setVisibility(View.VISIBLE);
                mAudioInfo.setText(RcsMessageUtils.formatFileSize(voiceMessage.getSize()));
            }
        } else {
            //if (null != mAudioDownloadProgressBar) {
            //    mAudioDownloadProgressBar.setVisibility(View.GONE);
            //}
            if (isFileTransferStatus && !showContent) {
                mIpAudioView.setVisibility(View.GONE);
            } else {
                mAudioInfo.setVisibility(View.VISIBLE);
                mAudioInfo.setText(RcsMessageUtils.formatAudioTime(voiceMessage.getDuration()));
            }

        }
        */

       // if (TextUtils.isEmpty(voiceMessage.getCaption())) {
            //mCaptionSeparator.setVisibility(View.GONE);
            mCaption.setVisibility(View.GONE);
       // } else {
            //mCaptionSeparator.setVisibility(View.VISIBLE);
        //    mCaption.setVisibility(View.VISIBLE);
        //    CharSequence caption = "";
       //     caption = SmileyParser2.getInstance().addSmileySpans(voiceMessage.getCaption());
        //    mCaption.setText(caption);
       // }

        /// M: add for ip message, hide text, image, audio, vCard, vCalendar, location view
        mBodyTextView.setVisibility(View.GONE);
        //mIpDynamicEmoticonView.setVisibility(View.GONE);
        mIpImageView.setVisibility(View.GONE);
        mIpVCardView.setVisibility(View.GONE);
        mIpGeolocView.setVisibility(View.GONE);

        
    }

    private class myVcardEntryHandler implements VCardEntryHandler {
        public void onEntryCreated(final VCardEntry entry) {
            RcsVcardParserResult result = RcsVcardUtils.ParseRcsVcardEntry(entry, mRcsContext);

            //set name
            String name = result.getName();
            //String number = result.getNumber().get(0).toString();
            //String info = name + number;
            
            mVCardInfo.setText(name);
            mIpVCardView.setVisibility(View.VISIBLE);

            //set photo
            Bitmap bitmap = null;
            
            byte[] pic = result.getPhoto();
            if (pic != null) {
                bitmap = BitmapFactory.decodeByteArray(pic,0,pic.length);
            }
            
            
            try {
                if (null == bitmap) {
                    bitmap = BitmapFactory.decodeResource(mRcsContext.getResources(),
                            R.drawable.ipmsg_chat_contact_vcard);
                }
                mVCardPortrait.setImageBitmap(bitmap);
                // set portrait
                IpMessage ipMessage = mRcsMessageItem.getIpMessage();
                ((IpVCardMessage)ipMessage).setPortrait(bitmap);
                ((IpVCardMessage)ipMessage).setName(name);
            } catch (java.lang.OutOfMemoryError e) {
                    Log.e(TAG, "setImage: out of memory: ", e);
            }
        }

        public void onEnd() {

        }

        public void onStart() {

        } 
    }

   myVcardEntryHandler mVcardEntryHandler = null;
   private void setIpVCardItem(IpMessage ipMessage,
                                boolean isDeleteMode,
                                boolean isFileTransferStatus,
                                boolean showContent) {
        
        Log.d(TAG, "setIpVCardItem(): message Id = " + mRcsMessageItem.mMsgId);
        
        IpVCardMessage vCardMessage = (IpVCardMessage)ipMessage;
        
        int entryCount = vCardMessage.getEntryCount();
        if (entryCount == 0) {
            // there is no entrycount in ipMessage
            entryCount = RcsVcardUtils.getVcardEntryCount(vCardMessage.getPath());
            ((IpVCardMessage)ipMessage).setEntryCount(entryCount);
        }
        Bitmap bitmap = vCardMessage.getPortrait();
        if (bitmap != null) {
            mIpVCardView.setVisibility(View.VISIBLE);
            mVCardPortrait.setImageBitmap(bitmap);
            String name = vCardMessage.getName();
            if (name != null && name.lastIndexOf(".") != -1) {
                name = name.substring(0, name.lastIndexOf("."));
            }
            mVCardInfo.setText(name);
        } else {
            //int entryCount = RcsVcardUtils.getVcardEntryCount(((IpAttachMessage)ipMessage).getPath());
            Log.d(TAG,"onItemClick, vcard entry count = " + entryCount);
            if (entryCount == 1) {
                mVcardEntryHandler = new myVcardEntryHandler();
                RcsVcardUtils.parseVcard(vCardMessage.getPath(),mVcardEntryHandler);
            } else {
        String name = vCardMessage.getName();
        if (name != null && name.lastIndexOf(".") != -1) {
            name = name.substring(0, name.lastIndexOf("."));
        }
            mVCardInfo.setText(name);
            mIpVCardView.setVisibility(View.VISIBLE);
                bitmap = BitmapFactory.decodeResource(mRcsContext.getResources(),
                                R.drawable.ipmsg_chat_contact_vcard);
                if (bitmap != null) {
                    mVCardPortrait.setImageBitmap(bitmap);
                    // set portrait
                    vCardMessage.setPortrait(bitmap);
                }   
            }
        }              

        /// M: add for ip message, hide text, image, audio, vCalendar, location view
        mBodyTextView.setVisibility(View.GONE);
        //mIpDynamicEmoticonView.setVisibility(View.GONE);
        mIpImageView.setVisibility(View.GONE);
        mIpAudioView.setVisibility(View.GONE);
        //mCaptionSeparator.setVisibility(View.GONE);
        mCaption.setVisibility(View.GONE);
        mIpGeolocView.setVisibility(View.GONE); 
    }
      private void setIpGeolocItem(IpMessage ipMessage,
                                boolean isDeleteMode,
                                boolean isFileTransferStatus,
                                boolean showContent) {
        
        Log.d(TAG, "setIpGeolocItem(): message Id = " + mRcsMessageItem.mMsgId);
        IpGeolocMessage geolocMessage = (IpGeolocMessage)ipMessage;    

        mBodyTextView.setVisibility(View.GONE);
        mIpImageView.setVisibility(View.GONE);
        mIpAudioView.setVisibility(View.GONE);
        mCaption.setVisibility(View.GONE);
        mIpVCardView.setVisibility(View.GONE);
        mMediaPlayView.setVisibility(View.GONE);
        mIpGeolocView.setVisibility(View.VISIBLE);
    }

    private void setIpVideoItem(IpMessage ipMessage,
                                boolean isDeleteMode,
                                boolean isFileTransferStatus,
                                boolean showContent) {
        
        Log.d(TAG, "setIpVideoItem(): message Id = " + mRcsMessageItem.mMsgId);

        IpVideoMessage videoMessage = (IpVideoMessage) ipMessage;
        mIpImageView.setVisibility(View.VISIBLE);
        mMediaPlayView.setVisibility(View.VISIBLE);
        mIpImageSizeBg.setVisibility(View.VISIBLE);
        mVideoDur.setVisibility(View.VISIBLE);
        updateIpMessageVideoOrImageView(mRcsMessageItem, videoMessage);
        setVideoView(videoMessage.getPath(), videoMessage.getThumbPath());
        mVideoDur.setText(RcsMessageUtils.formatAudioTime(videoMessage.getDuration()));
        mCaption.setVisibility(View.GONE);
        /// M: add for ip message, hide text, audio, vCard, vCalendar, location view
        mBodyTextView.setVisibility(View.GONE);
        //mIpDynamicEmoticonView.setVisibility(View.GONE);
        mIpAudioView.setVisibility(View.GONE);
        mIpVCardView.setVisibility(View.GONE);
        mIpGeolocView.setVisibility(View.GONE);  
    }

    public void setVideoView(String path, String bakPath) {
        Bitmap bp = null;
        int degree = 0;
        mMediaPlayView.setVisibility(View.VISIBLE);

        if (!TextUtils.isEmpty(path)) {
            Log.d(TAG, "setVideoView, path is " + path);
            bp = ThumbnailUtils.createVideoThumbnail(path, Thumbnails.MICRO_KIND);
            degree = RcsMessageUtils.getExifOrientation(path);
        }

        if (null == bp) {
            if (!TextUtils.isEmpty(bakPath)) {
                Log.d(TAG, "setVideoView, bakPath = " + bakPath);
                BitmapFactory.Options options = RcsMessageUtils.getOptions(bakPath);
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(bakPath, options);
                bp = RcsMessageUtils.getBitmapByPath(bakPath, RcsMessageUtils.getOptions(bakPath),
                        options.outWidth, options.outHeight);
                degree = RcsMessageUtils.getExifOrientation(bakPath);
            }
        }
        /** M: we use the same view show image/big emoticon/video snap, but they should have different property.
         *  image layout change to a dynamic size, big emoticon/video snap is still wrap_content
         *  we change ipmessage image layout to keep uniform with group chat activity.
         */
        ViewGroup.LayoutParams params = (ViewGroup.LayoutParams) mImageContent.getLayoutParams();
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        mImageContent.setLayoutParams(params);

        if (null != bp) {
            if (degree != 0) {
                bp = RcsMessageUtils.rotate(bp, degree);
            }
            mImageContent.setImageBitmap(bp);
        } else {
            mImageContent.setImageResource(R.drawable.ic_missing_thumbnail_picture);
        }
    }


    private void drawDeleteBARMsgIndicator(RcsMessageItem msgItem) {
        Log.d(TAG, "drawDeleteBARMsgIndicator() ");
//        if ( delayTimerLen < 5 ) {
//            return;
//        }
//        if(msgItem.isReceivedBurnedMessage()) {
//            return;
//        }
        // mDeleteBARMsgIndicator.setImageResource(R.drawable.ic_ipbar_timer_5);
        //mDeleteBARMsgIndicator.setVisibility(View.VISIBLE);
        mDeleteBARMsgIndicator.setVisibility(View.VISIBLE);
        startTimer();        

    }
    
    BurnedMsgTask mTimeTask;
    private static HashMap<Long, BurnedMsgTask> sBurnedTimerMap = new HashMap<Long, BurnedMsgTask>();
    
    private void startTimer() {
        
//        if (deleteBARMSGTimer != null ) {
//            Log.d(TAG, " drawDeleteBARMsgIndicator:  startTimer() deleteBARMSGTimer = " + deleteBARMSGTimer);
//            return;
//        }
        
        mTimeTask = sBurnedTimerMap.get(Long.valueOf(mIpMessageId));
        Log.d(TAG, "drawDeleteBARMsgIndicator: startTimer()  mIpMessageId = " + mIpMessageId+ " sBurnedTimerMap = " +sBurnedTimerMap+" mTimeTask = "+mTimeTask);
        if (mTimeTask != null) {
            mTimeTask.setHandler(burnedMsgHandler);
            int count = mTimeTask.getCurrentCount();
            Log.d(TAG, "drawDeleteBARMsgIndicator: startTimer()  count = " + count);
            if (count > 0) {
                mDeleteBARMsgIndicator.setImageResource(ipbarmsgshareIconArr[count-1]);
            } else {
                //TODO: delete this msg

            }
            
        } else {
            // mDeleteBARMsgIndicator.setVisibility(View.GONE);
            //mDeleteBARMsgIndicator.setImageDrawable(null);
        	
 			long sentTime = RCSMessageManager.getInstance(mContext).getIpMsgSentTime(mRcsMessageItem.mIpMessageId);
 			int count = mTimerNum;
 			Log.d(TAG, "drawDeleteBARMsgIndicator: text sentTime = " + sentTime);
 			if (sentTime >0) {
 				int timer = (int)((System.currentTimeMillis() - sentTime)/ 1000);
     			Log.d(TAG, "drawDeleteBARMsgIndicator: timer = " + timer);
     			if (timer >= 5 && mTimerNum == 5) {
    		        deleteBurnedMsg(mIpMessageId);
    		        removeIpMsgId(mIpMessageId);
    		        return ;
     			} else if (timer >= 1 && timer < 5 ){
     				count = mTimerNum - timer;
     			}
 			}
 			Log.d(TAG, "drawDeleteBARMsgIndicator: count = " + count);
        	
            deleteBARMSGTimer= new Timer();
            mTimeTask = new BurnedMsgTask(mIpMessageId, burnedMsgHandler,count);
            deleteBARMSGTimer.schedule(mTimeTask, 0, 1000);
            sBurnedTimerMap.put(Long.valueOf(mIpMessageId), mTimeTask);
        }
    }

  private class BurnedMsgTask extends TimerTask {
      long mBurnedMsgId;
      Handler mCallBackHandler;
      int mCount = mTimerNum ;
      public BurnedMsgTask(long msgId, Handler handler,int timerNum) {
          mBurnedMsgId = msgId;
          mCallBackHandler = handler;
          mCount = timerNum;
      }
      
      public void setHandler(Handler handler) {
          mCallBackHandler = handler;
      }
      
      public int getCurrentCount() {
          synchronized (this) {
              return mCount;
          }
      }
        public void run() {
            Message msg = new Message();
            synchronized (this) {
                msg.what = mCount--;
            }
            msg.obj = Long.valueOf(mBurnedMsgId);

            Log.d(TAG, " drawDeleteBARMsgIndicator:  send  msg.what = " + msg.what + " mBurnedMsgId = "+mBurnedMsgId);
            if (mCallBackHandler != null) {
                mCallBackHandler.sendMessage(msg);
            }
            if ( mCount == -1) {
                Log.d(TAG, " drawDeleteBARMsgIndicator:  this.cancel() ");
                this.cancel();
                //TODO: delete the msg
				if (!mVisible ) {
					// deleteBurnedMsg(mBurnedMsgId);
					removeIpMsgId(mBurnedMsgId);
				}
                
            }
        }
    }
      
    Handler burnedMsgHandler = new Handler(){
        int num = -1;
        public void handleMessage(Message msg) {
            num = msg.what;
            long ipmsgId = (Long)msg.obj;
            Log.d(TAG, "drawDeleteBARMsgIndicator: burnedMsgHandler icon num = " + num + ", mVisible: " + mVisible
                    +" ipmsgId = "+ipmsgId + " mIpMessageId = "+mIpMessageId);
            if (!mVisible || ipmsgId != mIpMessageId) {
                return;
            }
            if( num > 0 ){
                mDeleteBARMsgIndicator.setImageResource(ipbarmsgshareIconArr[num-1]);
            } else if( num == 0 ) {
                mDeleteBARMsgIndicator.setImageDrawable(null);
                mDeleteBARMsgIndicator.setVisibility(View.GONE);

                synchronized (sBurnedTimerMap) {
                    sBurnedTimerMap.remove(mIpMessageId);
                }
                Log.d(TAG, "drawDeleteBARMsgIndicator: burnedMsgHandler sBurnedTimerMap = " + sBurnedTimerMap);
                // deleteBurnedMsg(mIpMessageId);
                removeIpMsgId(mIpMessageId);
                /*
                if (deleteBARMSGTimer != null) {
                    deleteBARMSGTimer.cancel();
                    deleteBARMSGTimer = null;
                    Log.d(TAG, " drawDeleteBARMsgIndicator: Handler()  deleteBARMSGTimer.cancel() ");
                }
                if (mTimeTask != null) {
                    mTimeTask.cancel();
                    mTimeTask = null;
                    Log.d(TAG, " drawDeleteBARMsgIndicator:  Handler()   mTimeTask.cancel() ");
                }

                deleteBurnedMsg();
                // sDelMsgNum.remove(mIpMessageId);
                // Log.d(TAG, "drawDeleteBARMsgIndicator: burnedMsgHandler i == 0  " + sDelMsgNum);
                mDeleteBARMsgIndicator.setImageDrawable(null);
                mDeleteBARMsgIndicator.setVisibility(View.GONE);
                */
            }
        }
    };

    /*
     * private void deleteBurnedMsg(long msgBurnedId) { IpMessage ipMessage =
     * RCSMessageManager.getInstance(mContext) .getIpMsgInfo(mThreadId,
     * msgBurnedId); if(ipMessage == null) { return; } Log.w(TAG,
     * "drawDeleteBARMsgIndicator, mThreadId = " + mThreadId +
     * "  msgBurnedId = " + msgBurnedId + "  ipMessage type = " +
     * ipMessage.getType());
     * 
     * if(ipMessage.getType() == IpMessageType.TEXT) {
     * RCSMessageManager.getInstance
     * (ContextCacher.getHostContext()).deleteRCSMsg(mThreadId, msgBurnedId); }
     * else {
     * RCSMessageManager.getInstance(ContextCacher.getHostContext()).deleteFTRCSMsg
     * (mThreadId, msgBurnedId); }
     * //RCSMessageManager.getInstance(ContextCacher.
     * getHostContext()).sendBurnDeliveryReport( //
     * ipMessage.getTo(),ipMessage.getMessageId()); }
     */

    private void deleteBurnedMsg(final long msgBurnedId) {
        IpMessage ipMessage = RCSMessageManager.getInstance(mContext)
                .getIpMsgInfo(mThreadId, msgBurnedId);
        if (ipMessage == null) {
            return;
        }
        Log.w(TAG, "drawDeleteBARMsgIndicator, mThreadId = " + mThreadId
                + "  msgBurnedId = " + msgBurnedId + "  ipMessage type = "
                + ipMessage.getType());

        if (ipMessage.getType() == IpMessageType.TEXT) {

            // RCSMessageManager.getInstance(ContextCacher.getHostContext()).
            // sendDisplayedDeliveryReport(null,String.valueOf(ipMsgId));
            Log.d(TAG, "[BurnedMsg]: ipMessage.getFrom() = "
                    + ipMessage.getFrom() + " ipMessage.getMessageId() = "
                    + ipMessage.getMessageId());
            RCSMessageManager.getInstance(ContextCacher.getHostContext())
                    .sendBurnDeliveryReport(ipMessage.getFrom(),
                            ipMessage.getMessageId());
            ContextCacher.getHostContext().getContentResolver().delete(
                    Sms.CONTENT_URI, Sms.IPMSG_ID + " = " + msgBurnedId, null);
            removeIpMsgId(msgBurnedId);
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Log.d(TAG, " [BurnedMsg]:  , text run");
                        Thread.sleep(1000);
                        RCSMessageManager.getInstance(
                                ContextCacher.getHostContext())
                                .deleteStackMessage(msgBurnedId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } else {
            Log.d(TAG, "[BurnedMsg]: mIpMessage.getStatus() = "
                    + ipMessage.getStatus() + "  getRcsStatus = "
                    + ((IpAttachMessage) ipMessage).getRcsStatus());
            if (ipMessage.getStatus() == Sms.MESSAGE_TYPE_INBOX
                    && ((IpAttachMessage) ipMessage).getRcsStatus() == Status.FINISHED) {
                Log.d(TAG, "[BurnedMsg]: mIpMessage.getFrom() = "
                        + ipMessage.getFrom() + " mIpMessage.getMessageId() = "
                        + ipMessage.getMessageId());
                RCSMessageManager.getInstance(ContextCacher.getHostContext())
                        .sendBurnDeliveryReport(ipMessage.getFrom(),
                                ipMessage.getMessageId());
                ContextCacher.getHostContext().getContentResolver().delete(
                        Sms.CONTENT_URI, Sms.IPMSG_ID + " = " + msgBurnedId,
                        null);
                removeIpMsgId(msgBurnedId);
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            Log.d(TAG, " [BurnedMsg]:  onPause(), ft run");
                            Thread.sleep(1000);
                            RCSMessageManager.getInstance(
                                    ContextCacher.getHostContext())
                                    .deleteStackMessage(msgBurnedId);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        }
    }

    public boolean bindIpSystemEvent(RcsMessageItem item, boolean isDeleteMode) {
        // / M: add for ipmessage, notification listener
        Log.d(TAG, "bindIpmsg(): msgId = " + mMsgId);
        if (item == null) {
            Log.e(TAG, "bindIpSystemEvent, item is null");
            throw new RuntimeException("bindIpSystemEvent, item is null");
        }
        if (mMessageContent != null) {
            mMessageContent.setVisibility(View.GONE);
        }
        if (mSystemEventText != null) {
            mSystemEventText.setVisibility(View.VISIBLE);
            String body = item.mBody;
            if (!TextUtils.isEmpty(body)) {
                mSystemEventText.setText(item.mBody);
            }
        }
//        mIpMessageItemCallback.setSubDateView(null);
        return true;
    }

    private void bindPartipantInfo(RcsMessageItem item, boolean isDeleteMode) {
        if (mThreadId != -1) {
            if (!mIsGroupItem) {
                /// M: for multi send items, not show senderPhoto and senderName
                int chatType = mMessageListAdapter.getChatType();
                Log.d(TAG, "bindView(): chatType = " + chatType);
                if (chatType == mMessageListAdapter.CHAT_TYPE_ONE2MULTI) {
                    mVisible = false;
                    showSenderPhoto(false);
                    showSenderName(false);
                    /// M: for sms message, show recipient name view
                    if(item.mIpMessageId == 0) {
                        if (mSmsRecipient != null && mSmsInfo != null) {
                            mSmsInfo.setVisibility(View.VISIBLE);
                            mSmsRecipient.setVisibility(View.VISIBLE);
                            mSmsRecipient.setText("TO: " + item.mAddress);
                        }
                    } else {
                        if (mSmsRecipient != null && mSmsInfo != null) {
                            mSmsRecipient.setVisibility(View.GONE);
                            mSmsInfo.setVisibility(View.GONE);
                        }
                    }
                    return;
                }
                if (!RCSServiceManager.getInstance().isServiceEnabled()) {
                    showSenderPhoto(false);
                    showSenderName(false);
                    return;
                }
            }

            showSenderPhoto(true);
            if (mDirection == TYPE_INCOMING) {
                showSenderName(mIsGroupItem);
                if(!mIsGroupItem) {
                    //only portrait
                    mMemberInfo = PortraitManager.getInstance().getMemberInfo(item.mAddress);
                } else {
                    // portrait and name
                    mMemberInfo = PortraitManager.getInstance().getMemberInfo(mChatId, item.mAddress);
                }
            } else {
                //only name
                showSenderName(false);
                mMemberInfo = PortraitManager.getInstance().getMyInfo(mRcsMessageItem.mSubId);
            }
            updateParticipantInfo(mMemberInfo.mName, mMemberInfo.mDrawable);
            if (mMemberInfo != null) {
                mMemberInfo.addChangedListener(this);
            }
        }
    }

    private void updateParticipantInfo(final String name, final Drawable portrait) {
        if (mSenderName != null && mSenderName.getVisibility() == View.VISIBLE) {
            mSenderName.setText(name);
        }
        if (mSenderPhoto != null) {
            mSenderPhoto.setImageDrawable(portrait);
        }
        
        if (mDirection == TYPE_INCOMING) {
            mSenderPhoto.assignContactFromPhone(mRcsMessageItem.mAddress, false);
        } else {
            mSenderPhoto.assignContactFromPhone(RcsProfile.getInstance().getNumber(), false);
        }
    }

    @Override
    public void onChanged(final MemberInfo info) {
        // TODO Auto-generated method stub
        if (info == null) {
            Log.e(TAG, "onChanged: MemberInfo is null");
        }

        String number = mRcsMessageItem.mAddress;
        if (number == null || !number.equals(info.mNumber)) {
            Log.e(TAG, "onChanged: MemberInfo is wrong: number = " + number +
                            ", info.mNumber = " + info.mNumber);
            return;
        }
        if (mHandler != null) {
            mHandler.post(new Runnable() {
                
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    if (mVisible) {
                        updateParticipantInfo(info.mName, info.mDrawable);
                    }
                }
            });
        }
    }
    
    private void showIpMessageDetail(RcsMessageItem msgItem) {
        Resources res = mRcsContext.getResources();
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(res.getString(R.string.message_details_title))
        .setMessage(getIpTextMessageDetails(mRcsContext, msgItem))
        .setCancelable(true)
        .show();
    }
    
    public static String getIpTextMessageDetails(Context context, RcsMessageItem msgItem) {
        StringBuilder details = new StringBuilder();
        Resources res = context.getResources();

        // Message Type: Text message.
        details.append(res.getString(R.string.message_type_label));
        details.append(res.getString(R.string.rcs_message_type));


        // Address: ***
        int smsType = msgItem.mBoxId;
        if (msgItem.mBoxId == Sms.MESSAGE_TYPE_INBOX) {
            details.append('\n');
            details.append(res.getString(R.string.from_label));
            details.append(msgItem.mAddress);
        } else {
            if (!msgItem.mIsGroupItem) {
                details.append('\n');
                details.append(res.getString(R.string.to_address_label));
                details.append(msgItem.mAddress);
            }
        }
        /*
        if (msgItem.mSentDate > 0 && smsType == Sms.MESSAGE_TYPE_INBOX) {
            details.append('\n');
            details.append(res.getString(R.string.sent_label));
            String dateStr = "";
            /// M: For OP09 @{
            if (MmsConfig.isFormatDateAndTimeStampEnable()) {
                dateStr = mmsUtils.formatDateAndTimeStampString(context, 0, msgItem.mSmsSentDate, true, dateStr);
            /// @}
            } else {
                dateStr = MessageUtils.formatTimeStampString(context, msgItem.mSmsSentDate, true);
            }

            details.append(dateStr);
        }
        */
        if (msgItem.mDate > 0L) {
            details.append('\n');
            if (smsType == Sms.MESSAGE_TYPE_INBOX) {
                details.append(res.getString(R.string.received_label));
            } else {
                details.append(res.getString(R.string.sent_label));
            }
            String dateStr = RcsUtilsPlugin.formatIpTimeStampString(msgItem.mDate, true);
            details.append(dateStr);
        }
        return details.toString();
    }

    @Override
    public void onClick(View view) {
        Log.d(TAG, "onClick: mDirection + " + mDirection + ", address = "
                                        + mRcsMessageItem.mAddress);
        QuickContactBadge contact;
        if (mDirection == TYPE_OUTGOING) {
            Intent intent = new Intent(RcsProfile.PROFILE_ACTION);
            mContext.startActivity(intent);
            return;
        }
        if (mIsGroupItem) {
            PortraitManager.getInstance().invalidatePortrait(mRcsMessageItem.mAddress);
        }
        final String[] PHONE_LOOKUP_PROJECTION = new String[] {
                PhoneLookup._ID,
                PhoneLookup.LOOKUP_KEY,
            };
        Cursor cursor = null;
        Uri uri = null;
        try {
            cursor = mContext.getContentResolver().query(Uri.withAppendedPath(
                                PhoneLookup.CONTENT_FILTER_URI, mRcsMessageItem.mAddress),
                    PHONE_LOOKUP_PROJECTION, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                long contactId = cursor.getLong(0);
                String lookupKey = cursor.getString(1);
                uri = Contacts.getLookupUri(contactId, lookupKey);
            }
        } catch (Exception e) {
            Log.e(TAG, "query error: " +e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.d(TAG, "onClick: uri = " + uri);
        if (uri != null) {
            QuickContact.showQuickContact(mContext, mSenderPhoto, uri,
                    QuickContact.MODE_MEDIUM, null);
        } else {
            Uri createUri = Uri.fromParts("tel", mRcsMessageItem.mAddress, null);
            final Intent intent = new Intent(Intents.SHOW_OR_CREATE_CONTACT, createUri);
            try {
                mContext.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.d(TAG, "Activity not exist");
            }
        }
    }

    public void setIsLastItem(boolean isLast) {
        mIsLastItem = isLast;
    }

    private boolean bindSmsItem(RcsMessageItem item) {
        if ("sms".equals(item.mType)) {
            final String filter = mRcsContext.getString(R.string.ft_type_filter_in_sms);
            String body = item.mBody;
            Log.d(TAG, "bindSmsItem, body is: " + body);
            if (body != null && body.startsWith(filter)) {
                String[] filter_types = new String[]{
                        mRcsContext.getString(R.string.ft_type_image_chinese),
                        mRcsContext.getString(R.string.ft_type_video_chinese),
                        mRcsContext.getString(R.string.ft_type_audio_chinese),
                        mRcsContext.getString(R.string.ft_type_position_chinese)
                };
//                int index = -1;
                int position = -1;
                int start = 0;
                for (int index = 0; index < filter_types.length; index ++) {
                    start = body.indexOf(filter_types[index]);
                    if (start > 0) {
                        position = index;
                        break;
                    }
                }
                Log.d(TAG, "[bindSmsItem]: position = " + position);
                if (position > -1) {
                    int[] insteadofResIds = new int[]{R.drawable.ipmsg_choose_a_photo,
                            R.drawable.ipmsg_choose_a_video,
                            R.drawable.ipmsg_choose_an_audio,
                            R.drawable.ipmsg_geolocation};
                    int end = start + filter_types[position].length();
                    SpannableStringBuilder spannable = new SpannableStringBuilder(body);
                    ImageSpan image = new ImageSpan(mRcsContext, insteadofResIds[position]);
                    spannable.setSpan(image, start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                    mBodyTextView.setText(spannable);
                    mIpMessageItemCallback.setSubDateView(null);
                    return true;
                }
            }
        }
        return false;
    }
}
