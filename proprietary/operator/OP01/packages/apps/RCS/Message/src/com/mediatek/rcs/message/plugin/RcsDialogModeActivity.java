package com.mediatek.rcs.message.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.R.mipmap;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Message;
import android.provider.Settings;
import android.provider.MediaStore.Video.Thumbnails;
import android.provider.Telephony.Mms.Draft;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mediatek.mms.ipmessage.IpDialogModeActivity;
import com.mediatek.mms.ipmessage.IpDialogModeActivityCallback;
import com.mediatek.rcs.common.IpImageMessage;
import com.mediatek.rcs.common.IpMessage;
import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.common.IpMessageConsts.IpMessageStatus;
import com.mediatek.rcs.common.IpMessageConsts.IpMessageType;
import com.mediatek.rcs.common.IpGeolocMessage;
import com.mediatek.rcs.common.IpTextMessage;
import com.mediatek.rcs.common.IpVCalendarMessage;
import com.mediatek.rcs.common.IpVCardMessage;
import com.mediatek.rcs.common.IpVideoMessage;
import com.mediatek.rcs.common.IpVoiceMessage;
import com.mediatek.rcs.common.RCSMessageManager;
import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.provider.ThreadMapCache;
import com.mediatek.rcs.common.provider.ThreadMapCache.MapInfo;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.message.R;
import com.mediatek.rcs.message.utils.RcsMessageUtils;


public class RcsDialogModeActivity extends IpDialogModeActivity {
    private static String TAG = "RcseDialogModeActivity";
    
    /// M: add for ipmessage
    private TextView mGroupSender;
//    private final ArrayList<Uri> mIpMessageUris;
    private final Map<Uri, IpMessage> mIpMessages;
    private final Map<String, Bitmap> mBitmaps;
    private List<Uri> mUris;
    private View mMmsView;
    private TextView mSmsContentText;
    private EditText mEditor;

    private ViewGroup mIpViewHost;
    private View mIpView;
    private ImageView mBurnMessageImage;
    
    private View mIpImageView; // ip_image | ip_video
    private ImageView mImageContent; // image_content
    private ImageView mMediaPlayView; //video play

    private View mIpAudioView; // ip_audio
    private ImageView mAudioIcon; // ip_audio_icon
    private TextView mAudioInfo; // audio_info

    private View mIpVCardView; //vcard
    private ImageView mVCardIcon;
    private TextView mVCardInfo;
    
    private View mIpGeolocView;

    private Activity mActivity;
    private Context mRcsContext;
    private Cursor mCursor;
    private IpDialogModeActivityCallback mCallback;
    
    private long mCurThreadId;

    private static final int SMS_ADDR = 2;
    private static final int PREVIEW_IMAGE_SIZE = 90;

    public RcsDialogModeActivity(Context context) {
//        mIpMessageUris = new ArrayList<Uri>();
        mIpMessages = new HashMap<Uri, IpMessage>();
        mBitmaps = new HashMap<String, Bitmap>();
        mRcsContext = context;
    }

    @Override
    public boolean onIpInitDialogView(Activity context, List<Uri> uris, View mmsView,
            Cursor cursor, IpDialogModeActivityCallback callback, EditText replyEditor,
            TextView smsContentText, View ipView, TextView groupSender) {
//        return super.onIpInitDialogView(context, uris, mmsView, cursor, callback, replyEditor, smsContentText, ipView, groupSender);
        mActivity = context;
        mUris = uris;
        mMmsView = mmsView;
        mEditor = replyEditor;
        mSmsContentText = smsContentText;
        mIpViewHost = (ViewGroup)ipView;
        mCallback = callback;
        return true;
    }

    private void initIpView() {
        if (mIpView == null) {
            LayoutInflater inflater = LayoutInflater.from(mRcsContext);
            mIpView = inflater.inflate(R.layout.dialog_mode_ipmsg, mIpViewHost, true);
            mBurnMessageImage = (ImageView) mIpView.findViewById(R.id.rcs_burn_message);
            mIpImageView = mIpView.findViewById(R.id.ip_image);
            mImageContent = (ImageView)mIpView.findViewById(R.id.image_content);
            mMediaPlayView = (ImageView)mIpView.findViewById(R.id.video_media_play);
            mIpAudioView = mIpView.findViewById(R.id.ip_audio);
            mAudioIcon = (ImageView)mIpView.findViewById(R.id.ip_audio_icon);
            mAudioInfo = (TextView)mIpView.findViewById(R.id.audio_info);

            mIpVCardView = mIpView.findViewById(R.id.ip_vcard);
            mVCardIcon = (ImageView)mIpView.findViewById(R.id.ip_vcard_icon);
            mVCardInfo = (TextView)mIpView.findViewById(R.id.vcard_info);
            mIpGeolocView = mIpView.findViewById(R.id.ip_geoloc);
        }
    }
    
    @Override
    public boolean onIpAddNewUri(Intent intent, Uri newUri) {
        if (intent.getBooleanExtra("ipmessage", false)) {
            long ipMessageId = intent.getLongExtra(IpMessageConsts.MessageAction.KEY_IPMSG_ID, 0);
            Log.d(TAG, "receiver a ipmessage,uri:" + newUri.toString() + ", ipMessageId = " + ipMessageId);
            if (ipMessageId == 0) {
                String[] projection = new String[] {"_id", "ipmsg_id"};
                Cursor cursor = mRcsContext.getContentResolver().query(newUri, projection, null, null, null);
                try {
                    if (cursor != null && cursor.moveToFirst()) {
                        ipMessageId = cursor.getLong(1);
                    }
                } catch (Exception e) {
                    // TODO: handle exception
                    Log.e(TAG, "[onIpAddNewUri]e : " +e);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
            if (ipMessageId != 0) {
                IpMessage ipMsg  = RCSMessageManager.getInstance(mRcsContext).getIpMsgInfo(0, ipMessageId);
                mIpMessages.put(newUri, ipMsg);
            }
        }
        return super.onIpAddNewUri(intent, newUri);
    }

    @Override
    public boolean onIpDestroy() {
//        IpMessageUtils.removeIpMsgNotificationListeners(mContext, this);
//        return true;
        mIpMessages.clear();
        return super.onIpDestroy();
    }

    /**
     * Implements onIpSetDialogView.
     * @return CharSequence
     */
    public String onIpSetDialogView() {
        IpMessage ipMessage = getCurIpMessage();
        if (ipMessage != null) {
            if (mMmsView != null) {
                Log.d(TAG, "Hide MMS views");
                mMmsView.setVisibility(View.GONE);
            }
            initIpView();
            mIpViewHost.setVisibility(View.VISIBLE);
            Log.d(TAG, "id:" + ipMessage.getIpDbId() + ",type:" + ipMessage.getType());
            if (ipMessage.getBurnedMessage()) {
//            if (true) {
                setBurnMessageItem(ipMessage);
            } else {
                mBurnMessageImage.setVisibility(View.GONE);
                switch (ipMessage.getType()) {
                    case IpMessageType.TEXT:
                        setIpTextItem((IpTextMessage) ipMessage);
                        break;
                    case IpMessageType.PICTURE:
                        setIpImageItem((IpImageMessage) ipMessage);
                        break;
                    case IpMessageType.VOICE:
                        setIpVoiceItem((IpVoiceMessage) ipMessage);
                        break;
                    case IpMessageType.VCARD:
                        setIpVCardItem((IpVCardMessage) ipMessage);
                        break;
                    case IpMessageType.VIDEO:
                        setIpVideoItem((IpVideoMessage) ipMessage);
                        break;
                    case IpMessageType.GEOLOC:
                        setIpGeolocItem((IpGeolocMessage)ipMessage);
                        break;
                    case IpMessageType.UNKNOWN_FILE:
                    case IpMessageType.COUNT:
                        Log.w(TAG, "Unknown IP message type. type = " + ipMessage.getType());
                        break;
                    default:
                        Log.w(TAG, "Error IP message type. type = " + ipMessage.getType());
                        break;
                }
            }
        } else {
            mIpViewHost.setVisibility(View.GONE);
        }
        return null;
    }

    @Override
    public String onIpGetSenderString() {
        long threadId = mCallback.getIpThreadId();
        MapInfo info = ThreadMapCache.getInstance().getInfoByThreadId(threadId);
        if (info != null) {
            String groupName = info.getNickName();
            if (TextUtils.isEmpty(groupName)) {
                groupName = info.getSubject();
            }
            return groupName;
        }
        return super.onIpGetSenderNumber();
    }
    
    @Override
    public String onIpGetSenderNumber() {
//        if (isCurGroupIpMessage()) {
//            return getCurGroupIpMessageNumber();
//        }
//        return null;
        return super.onIpGetSenderNumber();
    }
    
    @Override
    public boolean onIpClick(long threadId) {
        return super.onIpClick(threadId);
    }
    
    @Override
    public boolean onIpSendReplySms(String body, String to) {
        long threadId = mCallback.getIpThreadId();
        Log.d(TAG, "onIpSendReplySms: threadId=" + threadId);
        MapInfo info = ThreadMapCache.getInstance().getInfoByThreadId(threadId);
        if (info != null) {
            //group chat
            IpTextMessage msg = new IpTextMessage();
            msg.setBody(body);
            sendIpTextMessage(msg);
            return true;
        }
        return super.onIpSendReplySms(body, to);
    }

    @Override
    public boolean onIpSendMessage(String body, String to) {
        if (RCSServiceManager.getInstance().isServiceEnabled()) {
            int selected = mCallback.getIpSelectedId();
            int rcsSubId = RcsMessageUtils.getRcsSubId(mActivity);
            if (selected == rcsSubId) {
                IpTextMessage msg = new IpTextMessage();
                msg.setBody(body);
                msg.setTo(to);
                sendIpTextMessage(msg);
                return true;
            }
        }
        return super.onIpSendMessage(body, to);
    }

    @Override
    public boolean onIpUpdateSendButtonState(ImageButton sendButton) {
//        if (isCurIpMessage()) {
//            sendButton.setImageResource(R.drawable.ic_send_ipmsg);
//            return true;
//        }
//        return false;
        return super.onIpUpdateSendButtonState(sendButton);
    }


  
    // / M: add for ipmessage
    private IpMessage getCurIpMessage() {
        IpMessage ipmsg = null;
        Uri curUri;
        if (mUris.size() <= 0) {
            mCallback.setCurUriIdx(0, null);
            return null;
        }
        curUri = (Uri) mUris.get(mCallback.getCurUriIdx());
        if (curUri != null) {
            ipmsg = mIpMessages.get(curUri);
            Log.d(TAG, "check ipMsg " + ipmsg);
        }
        return ipmsg;
    }

    private void setBurnMessageItem(IpMessage message) {
        mBurnMessageImage.setVisibility(View.VISIBLE);
        mSmsContentText.setVisibility(View.GONE);
        mIpImageView.setVisibility(View.GONE);
        mIpAudioView.setVisibility(View.GONE);
        mIpGeolocView.setVisibility(View.GONE);
        mIpVCardView.setVisibility(View.GONE);
    }

    private void setIpTextItem(IpTextMessage textMessage) {
        Log.d(TAG, "setIpTextItem()");
        if (TextUtils.isEmpty(textMessage.getBody())) {
            Log.w(TAG, "setIpTextItem(): No message content!");
            return;
        }
        String body = textMessage.getBody();
        if (TextUtils.isEmpty(body)) {
            body = mSmsContentText.getText().toString();
        }
        EmojiImpl emoji = EmojiImpl.getInstance(mActivity);
        CharSequence formattedMessage = emoji.getEmojiExpression(body, true);
        mSmsContentText.setVisibility(View.VISIBLE);
        mSmsContentText.setText(formattedMessage);
        mIpImageView.setVisibility(View.GONE);
        mIpAudioView.setVisibility(View.GONE);
        mIpVCardView.setVisibility(View.GONE);
        mIpImageView.setVisibility(View.GONE);
        mIpGeolocView.setVisibility(View.GONE);
    }

    private void setIpImageItem(IpImageMessage imageMessage) {
        Log.d(TAG, "setIpImageItem()");
        mIpImageView.setVisibility(View.VISIBLE);
        mMediaPlayView.setVisibility(View.INVISIBLE);
        String path = imageMessage.getThumbPath();
        Bitmap bitmap = getImageBitmap(path);
        if (bitmap != null) {
            mImageContent.setImageBitmap(bitmap);
        } else {
            Drawable defaultDrawable = mRcsContext.getResources().getDrawable(R.drawable.ic_missing_thumbnail_picture);
            mImageContent.setImageDrawable(defaultDrawable);
        }

        // / M: add for ipmessage, hide text view
        mMediaPlayView.setVisibility(View.GONE);
        mIpAudioView.setVisibility(View.GONE);
        mIpVCardView.setVisibility(View.GONE);
        mIpGeolocView.setVisibility(View.GONE);
        mSmsContentText.setVisibility(View.GONE);
    }

    private void setIpGeolocItem(IpGeolocMessage imageMessage) {
        Log.d(TAG, "setIpImageItem()");
        mIpGeolocView.setVisibility(View.VISIBLE);

        // / M: add for ipmessage, hide text view
        mIpImageView.setVisibility(View.GONE);
        mMediaPlayView.setVisibility(View.GONE);
        mIpAudioView.setVisibility(View.GONE);
        mIpVCardView.setVisibility(View.GONE);
        mSmsContentText.setVisibility(View.GONE);
    }
    
    private Bitmap getImageBitmap(String filePath) {
        Log.d(TAG, "decodeImageBitmap(): filePath = " + filePath);
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }
        Bitmap bitmap = mBitmaps.get(filePath);
        if (bitmap == null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            bitmap = RcsMessageUtils.getBitmapByPath(filePath, options, PREVIEW_IMAGE_SIZE, PREVIEW_IMAGE_SIZE);
            if (bitmap != null) {
                mBitmaps.put(filePath, bitmap);
            }
        }
        return bitmap;
    }

    private void setIpVideoItem(IpVideoMessage videoMessage) {
        String path = videoMessage.getThumbPath();
        Log.d(TAG, "setIpVideoItem(): path = " + path);
        Bitmap bitmap = getVideoThumbnail(path);
        if (bitmap != null) {
            mImageContent.setImageBitmap(bitmap);
        } else {
            Drawable defaultDrawable = mRcsContext.getResources().getDrawable(R.drawable.ic_missing_thumbnail_picture);
            mImageContent.setImageDrawable(defaultDrawable);
        }
        mMediaPlayView.setVisibility(View.VISIBLE);
        mIpImageView.setVisibility(View.VISIBLE);
        mMediaPlayView.setVisibility(View.VISIBLE);
        mSmsContentText.setVisibility(View.GONE);
        mIpAudioView.setVisibility(View.GONE);
        mIpVCardView.setVisibility(View.GONE);
        mIpGeolocView.setVisibility(View.GONE);
    }
    
    private Bitmap getVideoThumbnail(String filePath) {
        Log.d(TAG, "setVideoView(): filePath = " + filePath);
        if (TextUtils.isEmpty(filePath)) {
            return null;
        } 
        Bitmap bitmap = mBitmaps.get(filePath);
        if (bitmap == null) {
            bitmap = ThumbnailUtils.createVideoThumbnail(filePath, Thumbnails.MICRO_KIND);
            if (bitmap != null) {
                mBitmaps.put(filePath, bitmap);
            }
        }
        return bitmap;
    }
    
    private void setIpVoiceItem(IpVoiceMessage voiceMessage) {
        Log.d(TAG, "setIpVoiceItem(): message Id = " + voiceMessage.getIpDbId());

        mAudioInfo.setText(RcsMessageUtils.formatFileSize(voiceMessage.getSize()));
//        mAudioDur.setText(RcsMessageUtils.formatAudioTime(voiceMessage.getDuration()));
        mIpAudioView.setVisibility(View.VISIBLE);
        mIpImageView.setVisibility(View.GONE);
        mIpVCardView.setVisibility(View.GONE);
        mSmsContentText.setVisibility(View.GONE);
    }

    private void setIpVCardItem(IpVCardMessage vCardMessage) {
        Log.d(TAG, "setIpVCardItem(): vCardMessage = " + vCardMessage);
        String name = vCardMessage.getName();
        if (name != null && name.lastIndexOf(".") != -1) {
            name = name.substring(0, name.lastIndexOf("."));
        }
        mVCardInfo.setText(name);
        mIpVCardView.setVisibility(View.VISIBLE);
        mIpImageView.setVisibility(View.GONE);
        mIpAudioView.setVisibility(View.GONE);
        mIpGeolocView.setVisibility(View.GONE);
        mSmsContentText.setVisibility(View.GONE);
        // / M: add for ipmessage, hide image view or video view
    }

    private void sendIpTextMessage(final IpTextMessage ipMessage) {
        final long threadId = mCallback.getIpThreadId();
        Uri uri = (Uri) mUris.get(mCallback.getCurUriIdx());
        mCallback.markIpAsRead(uri);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "sendMessageForIpMsg(): calling API: saveIpMsg().");
                int ret = -1;
                ipMessage.setStatus(IpMessageStatus.OUTBOX);
                mCallback.onPreMessageSent();
                ret = RCSMessageManager.getInstance(mActivity).saveRCSMsg(ipMessage, 0, threadId);
                mCallback.onMessageSent();
            }
        }).start();
    }
}
