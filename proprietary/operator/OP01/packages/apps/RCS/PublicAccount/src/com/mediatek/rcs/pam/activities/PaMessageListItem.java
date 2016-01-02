/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.rcs.pam.activities;

import java.io.File;
import java.text.SimpleDateFormat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.LineHeightSpan;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.QuickContactBadge;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.rcs.message.location.GeoLocUtils;
import com.mediatek.rcs.message.location.GeoLocXmlParser;
import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.activities.PAAudioService.IAudioServiceCallBack;
import com.mediatek.rcs.pam.activities.PaMessageItem.DownloadCallback;
import com.mediatek.rcs.pam.model.MediaArticle;
import com.mediatek.rcs.pam.util.PaVcardUtils;

/**
 * This class provides view of a message in the messages list.
 */
public class PaMessageListItem extends LinearLayout implements
                            IAudioServiceCallBack {
    public static final String EXTRA_URLS = "com.android.mms.ExtraUrls";

    private static final String TAG = "PA/PaMessageListItem";
    
    public static final int DOWNLOAD_TYPE_THUMBNAIL = 1;
    public static final int DOWNLOAD_TYPE_IMAGE = 2;
    public static final int DOWNLOAD_TYPE_AUDIO = 3;
    public static final int DOWNLOAD_TYPE_VIDEO = 4;
    
    private static final float MAX_SCALE = 0.4f;
    private static final float MIN_SCALE = 0.3f;
    private static final float COMP_NUMBER = 0.5f;
    
    private static final int AUDIO_MODE_PREVIEW = 0;
    private static final int AUDIO_MODE_PLAY = 1;
    
    private static final int DRAW_TYPE_NORMAL = 0;
    private static final int DRAW_TYPE_SMALL = 1;
    private static final int DRAW_TYPE_LARGE = 2;
    
    private int mAudioMode;
    
    //static final int MSG_LIST_EDIT    = 1;
    //static final int MSG_LIST_PLAY    = 2;
    //static final int MSG_LIST_DETAILS = 3;

    private ImageView mImageView;
    private ImageView mLockedIndicator;
    private ImageView mDeliveredIndicator;
    private ImageView mDetailsIndicator;
    private TextView mBodyTextView;
    private Button mDownloadButton;
    private TextView mDownloadingLabel;
    private Handler mHandler;
    private PaMessageItem mMessageItem;
    private String mDefaultCountryIso;
    private TextView mDateView;
    public View mMessageBlock;
    //private QuickContactDivot mAvatar;
    static private Drawable sDefaultContactImage;
    //private Presenter mPresenter;
    private int mPosition;      // for debugging
    //private ImageLoadedCallback mImageLoadedCallback;
    
    private PaMessageListAdapter mMessageListAdapter;
    
    private CheckBox mSelectedBox;
    private View mTimeDivider;
    private TextView mTimeDividerStr;
    
    private LinearLayout mSendStatusBar;
    private ImageView mSendingIcon;
    private TextView  mSendingText;
    private ImageView mSendingSuccessIcon;
    private TextView  mSendingSuccessText;
    private ImageView mSendingFailIcon;
    private TextView  mSendingFailText;
    
    private View mIpAudioView;
    private View mIpAudioPreview;
    private ImageView mAudioIcon;
    private TextView mAudioInfo;
    private TextView mAudioDur;
    private ProgressBar mAudioDownloadBar;
    private View mIpAudioPlay;
    private ProgressBar mAudioPlayBar;
    private TextView mAudioTimePlay;
    private TextView mAudioTimeDuration;
    
    private View mIpImageView;
    private ImageView mImageContent;
    private View mIpImageSizeBg;
    private TextView mContentSize;
    //private ProgressBar mIpImageProgress;
 
    //private TextView mCaption;
    private ImageView mMediaPlayView;
    
    private View mGeolocView;
    private View mVcardView;
    private TextView mVCardInfo;
    private ImageView mVCardPortraint;
    
    private View mSingleMixView;
    private TextView mSingleMixTitle;
    private TextView mSingleMixCreateDate;
    private ImageView mSingleMixLogo;
    private TextView mSingleMixText;
    
    private LinearLayout mMultiMixView;
    private RelativeLayout mMultiMixHeaderView;
    private ImageView mMultiMixHeaderThumb;
    private TextView mMultiMixHeaderText;
    private LinearLayout mMultiAricleLayout[] = new LinearLayout[4];
    private LinearLayout mMutliMixBodyView[] = new LinearLayout[4];
    private ImageView mMultiMixBodyThumb[] = new ImageView[4];
    private TextView mMultiMixBodyText[] = new TextView[4];
    
    private TextView mSenderName;
    private QuickContactBadge mSenderPhoto;
    
    private boolean mVisible;

    PAAudioService mAudioService;
    
    //public PaMessageListItem(Context context) {
    //    super(context);
    //    //mContext = context;
    //    Log.d(TAG, "constructure PaMessageListItem(Context context) ");
    //}

    public PaMessageListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "PaMessageListItem(Context context, AttributeSet attrs)");
        //mContext = context;
        //onFinishInflateView();
    }
    
    //public PaMessageListItem (Context context, View view) {
    //super(context);
    //addView(view);
    //Log.d(TAG, "PaMessageListItem (Context context, View view)");
    //onFinishInflate();
    //}
    
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        
        mBodyTextView = (TextView) findViewById(R.id.text_view);
        mSelectedBox = (CheckBox)findViewById(R.id.select_check_box);
        
        mTimeDivider = (View)findViewById(R.id.time_divider);
        mTimeDividerStr = (TextView)mTimeDivider.findViewById(R.id.time_divider_str);   

        mDateView = (TextView)findViewById(R.id.date_view);
        mSendStatusBar = (LinearLayout)findViewById(R.id.status_panel);
        mSendingIcon = (ImageView)findViewById(R.id.delivered_sending);
        mSendingText = (TextView)findViewById(R.id.delivered_sending_txt);
        mSendingSuccessIcon = (ImageView)findViewById(R.id.delivered_success);
        mSendingSuccessText = (TextView)findViewById(R.id.delivered_success_txt);
        mSendingFailIcon = (ImageView)findViewById(R.id.delivered_failed);
        mSendingFailText = (TextView)findViewById(R.id.delivered_failed_txt);

        //for audio & video
        mIpAudioView = (View)findViewById(R.id.ip_audio);
        mIpAudioPreview = (View)findViewById(R.id.ip_audio_preview);
        mAudioIcon = (ImageView)findViewById(R.id.ip_audio_icon);
        mAudioInfo = (TextView)findViewById(R.id.ip_audio_info);
        mAudioDur = (TextView)findViewById(R.id.ip_audio_dur);
        mAudioDownloadBar = (ProgressBar)findViewById(R.id.ip_audio_downLoad_bar);        
        mIpAudioPlay = (View)findViewById(R.id.ip_audio_play);
        mAudioPlayBar = (ProgressBar)findViewById(R.id.ip_audio_play_bar);
        if (mAudioPlayBar != null) {
            mAudioPlayBar.setMax(100);
        }        
        mAudioTimePlay = (TextView)findViewById(R.id.ip_audio_time_play);
        mAudioTimeDuration = (TextView)findViewById(R.id.ip_audio_time_duration);

        //for image and view
        mIpImageView = (View)findViewById(R.id.ip_image);
        mImageContent = (ImageView)findViewById(R.id.image_content);
        mIpImageSizeBg = (View)findViewById(R.id.image_size_bg);
        mContentSize = (TextView)findViewById(R.id.content_size);
        //mIpImageProgress = (ProgressBar)findViewById(R.id.image_downLoad_progress);
        mMediaPlayView = (ImageView)findViewById(R.id.video_media_paly);
        
        mGeolocView = (View)findViewById(R.id.ip_geoloc);
        mVcardView = (View)findViewById(R.id.ip_vcard);
        mVCardInfo = (TextView)findViewById(R.id.vcard_info);
        mVCardPortraint = (ImageView)findViewById(R.id.ip_vcard_icon);
        
        //Single mixture
        mSingleMixView = (View)findViewById(R.id.ip_single_mix);
        mSingleMixTitle = (TextView)findViewById(R.id.single_mix_title);
        mSingleMixCreateDate = (TextView)findViewById(R.id.single_create_date);
        mSingleMixLogo = (ImageView)findViewById(R.id.single_mix_logo);
        mSingleMixText = (TextView)findViewById(R.id.single_mix_text);
        
        //multi mixture
        mMultiMixView = (LinearLayout)findViewById(R.id.ip_multi_mix);
        mMultiMixHeaderView = (RelativeLayout)findViewById(R.id.multi_mix_header);
        mMultiMixHeaderThumb = (ImageView)findViewById(R.id.multi_mix_header_thumb);
        mMultiMixHeaderText = (TextView)findViewById(R.id.multi_mix_header_title);

    }

    public void bind(PaMessageItem msgItem, int position) {
        if (null == msgItem) {
            Log.e(TAG, "bind: msgItem is null, position=" + position);
            return;
        }

        Log.e(TAG, "bind: msgItem position=" + position + ". type=" + msgItem.mType);

        mMessageItem = msgItem;
        mPosition = position;
        mBodyTextView.setText("");
        mTimeDividerStr.setText("");

        //if (!mMessageItem.isMe()) {
            //mSenderName = (TextView)findViewById(R.id.sender_name);
            mSenderPhoto = (QuickContactBadge)findViewById(R.id.sender_photo);
        //}
        //TODO: add for delete mode

        setLongClickable(false);
        setFocusable(false);
        setClickable(false);

        bindMessage();
    }

    public void unbind() {
        if (null == mMessageItem) {
            Log.e(TAG, "unbind() but mMessageItem is null.");
            return;
        }
        Log.e(TAG, "unbind: msgItemId=" + mMessageItem.mMsgId);

        for (int i = 0; i < 4; i ++) {
            if (mMultiAricleLayout[i] != null) {
                mMultiMixView.removeView(mMultiAricleLayout[i]);
                mMultiAricleLayout[i] = null;
                mMutliMixBodyView[i] = null;
                mMultiMixBodyThumb[i] = null;
                mMultiMixBodyText[i] = null;
                }
        }
        
        if (mMessageItem.mType == Constants.MEDIA_TYPE_AUDIO) {
            if (mAudioService != null) {
                mAudioService.unBindAudio(mMessageItem.mMsgId);
            }
        }
        mMessageItem = null;
    }

    public PaMessageItem getMessageItem() {
        return mMessageItem;
    }

    public void setMsgListItemHandler(Handler handler) {
        mHandler = handler;
    }

    private String buildTimestampLine(String timestamp) {
        return null;
    }

    private void bindMessage() {
        mVisible = true;

        new Thread(new Runnable() {

            @Override
            public void run() {
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        if (mMessageItem == null) {
                            return;
                        }
                        // TODO Auto-generated method stub
                        if (mVisible && !mMessageItem.isOutgoingMessage()) {
                            synchronized (PaComposeActivity.sLockObj) {
                                if (PaComposeActivity.mLogoBitmap !=  null) {
                                    mSenderPhoto.setImageBitmap(PaComposeActivity.mLogoBitmap);
                                }
                            }
                        } else {
                            synchronized (PaComposeActivity.sLockObj) {
                                if (PaComposeActivity.mPortraitBitmap != null) {
                                    mSenderPhoto.setImageBitmap(PaComposeActivity.mPortraitBitmap);
                                }
                            }
                        }
                    }
                });
            }
        }).start();

        switch (mMessageItem.getType()) {
        case Constants.MEDIA_TYPE_TEXT:
        case Constants.MEDIA_TYPE_SMS:
            setTextItem();
            break;
        case Constants.MEDIA_TYPE_PICTURE:
            setImageItem();
            break;
        case Constants.MEDIA_TYPE_VIDEO:
            setVideoItem();
            break;
        case Constants.MEDIA_TYPE_AUDIO:
            mAudioService = PAAudioService.getService();
            setAudioItem();
            break;
        case Constants.MEDIA_TYPE_SINGLE_ARTICLE:
            setSingleMixItem();
            break;
        case Constants.MEDIA_TYPE_MULTIPLE_ARTICLE:
            setMultiMixItem();
            break;
        case Constants.MEDIA_TYPE_VCARD:
            setVCardItem();
            break;
        case Constants.MEDIA_TYPE_GEOLOC:
            setGeolocItem();
            break;
        default:
            break;
        }
    }
    
    private void initImageVideoItem() {
        FrameLayout.LayoutParams para = new FrameLayout.LayoutParams(352, 288);
        mImageContent.setLayoutParams(para);
        mImageContent.setImageDrawable(new ColorDrawable(android.R.color.transparent));
        adjustTextWidth(352);
    }
    
    private void initSingleArticleItem() {
        LinearLayout.LayoutParams para = new LinearLayout.LayoutParams(352, 288);
        mSingleMixLogo.setLayoutParams(para);
        mSingleMixLogo.setImageDrawable(new ColorDrawable(android.R.color.transparent));
    }
    
    private void prepareForVisibility(int type) {
        Log.i(TAG, "prepareForVisibility(). type=" + type);

        mTimeDividerStr.setText(mMessageItem.getTimeStamp());
        
        mBodyTextView.setVisibility(View.GONE);
        mIpAudioView.setVisibility(View.GONE);
        mIpImageView.setVisibility(View.GONE);
        mGeolocView.setVisibility(View.GONE);
        mVcardView.setVisibility(View.GONE);

        if (mSingleMixView != null) {
            mSingleMixView.setVisibility(View.GONE);
        }
        if (mMultiMixView != null) {
            mMultiMixView.setVisibility(View.GONE);
        }
        switch (type) {
        case Constants.MEDIA_TYPE_TEXT:
        case Constants.MEDIA_TYPE_SMS:
            mBodyTextView.setVisibility(View.VISIBLE);
            break;

        case Constants.MEDIA_TYPE_PICTURE:
            mIpImageView.setVisibility(View.VISIBLE);
            mImageContent.setVisibility(View.VISIBLE);
            mMediaPlayView.setVisibility(View.GONE);
            mIpImageSizeBg.setVisibility(View.VISIBLE);
            initImageVideoItem();
            break;

        case Constants.MEDIA_TYPE_VIDEO:
            mIpImageView.setVisibility(View.VISIBLE);
            mMediaPlayView.setVisibility(View.VISIBLE);
            mImageContent.setVisibility(View.VISIBLE); 
            mIpImageSizeBg.setVisibility(View.VISIBLE);
            initImageVideoItem();
            break;
    
        case Constants.MEDIA_TYPE_AUDIO:
            mIpAudioView.setVisibility(View.VISIBLE);
            mIpAudioPreview.setVisibility(View.VISIBLE);
            mIpAudioPlay.setVisibility(View.GONE);
            break;
        case Constants.MEDIA_TYPE_GEOLOC:
            mGeolocView.setVisibility(View.VISIBLE);
            break;

        case Constants.MEDIA_TYPE_VCARD:
            mVcardView.setVisibility(View.VISIBLE);
            break;

        case Constants.MEDIA_TYPE_SINGLE_ARTICLE:
            if (mSingleMixView != null) {
                mSingleMixView.setVisibility(View.VISIBLE);
                initSingleArticleItem();
            } else {
                Log.i(TAG, "prepareForVisibility(). Error for no single mix view");
            }
            break;

        case Constants.MEDIA_TYPE_MULTIPLE_ARTICLE:
            if (mMultiMixView != null) {
                mMultiMixView.setVisibility(View.VISIBLE);
            } else {
                Log.i(TAG, "prepareForVisibility(). Error for no mulit mix view");
            }
            break;
        default:
            Log.i(TAG, "prepareForVisibility(). No valid view");
            break;
        }
    }
    
    private void updateStatusView(int type) {
        if (mMessageItem.mDirection == Constants.MESSAGE_DIRECTION_INCOMING) {
            return;
        }
        mSendStatusBar.setVisibility(View.VISIBLE);
        mSendingIcon.setVisibility(View.GONE);
        mSendingText.setVisibility(View.GONE);
        mSendingSuccessIcon.setVisibility(View.GONE);
        mSendingSuccessText.setVisibility(View.GONE);
        mSendingFailIcon.setVisibility(View.GONE);
        mSendingFailText.setVisibility(View.GONE);

        int status = mMessageItem.mStatus;
        Log.d(TAG, "updateStatusView(). status=" + status);
        switch (status) {
        case Constants.MESSAGE_STATUS_TO_SEND:
            mDateView.setText(R.string.to_send_text);
            mSendingIcon.setVisibility(View.VISIBLE);
            break;
            
        case Constants.MESSAGE_STATUS_SENDING:
            mDateView.setText(R.string.sending_text);
            mSendingIcon.setVisibility(View.VISIBLE);
            break;
            
        case Constants.MESSAGE_STATUS_SENT:
            //mSendingSuccessIcon.setVisibility(View.VISIBLE);
            mSendStatusBar.setVisibility(View.GONE);
            break;
            
        case Constants.MESSAGE_STATUS_FAILED:
            mDateView.setText(R.string.send_fail_text);
            mSendingFailIcon.setVisibility(View.VISIBLE);
            break;
            
        default:
            Log.d(TAG, "updateStatusView(). unrecoganized state");
            break;
        }

    }
    
    private void setTextItem() {
        prepareForVisibility(mMessageItem.getType());

        SpannableStringBuilder buf = new SpannableStringBuilder();
        buf.append(mMessageItem.getText());
        buf = new SpannableStringBuilder(RcsMessageUtils.
                formatTextMessage(mMessageItem.getText(), true, buf));
        
        mBodyTextView.setText(buf);

        updateStatusView(mMessageItem.getType());
    }
    
    private void adjustTextWidth(int width) {
        if ((mMessageItem.getType() != Constants.MEDIA_TYPE_PICTURE) &&
                (mMessageItem.getType() != Constants.MEDIA_TYPE_VIDEO)) {
            return;
        }
        
        if (mIpImageSizeBg != null && 
                (mIpImageSizeBg.getVisibility() == View.VISIBLE)) {
            ViewGroup.LayoutParams lp = mContentSize.getLayoutParams();
            lp.width = width;
            mIpImageSizeBg.setLayoutParams(lp);
            Log.d(TAG, "adjustTextWidth=" + lp.width);
        }       
    }
    
    private void setImageItem() {
        Log.d(TAG, "setImageItem id=" + mMessageItem.mMsgId);

        prepareForVisibility(mMessageItem.getType());
        
        if (mMessageItem.mMediaBasic == null) {
            Log.e(TAG, "mMediaBasic is null !");
            return;
        }
        
        try {
            int size = Integer.parseInt(mMessageItem.mMediaBasic.fileSize);
            mContentSize.setText(RcsMessageUtils.formatFileSize(size, 2));
        } catch (NumberFormatException e) {
            Log.e(TAG, "Parse image size failed:" + mMessageItem.mMediaBasic.fileSize);
            mContentSize.setText("");
        }
        
        if (mMessageItem.mDirection == Constants.MESSAGE_DIRECTION_OUTGOING) {
            if (RcsMessageUtils.isExistsFile(mMessageItem.mMediaBasic.originalPath)) {
                drawThumbnail(mImageContent, 0, mMessageItem.mMediaBasic.originalPath);
            }            
        } else {
            // Download thumbnail image firstly.
            String path = mMessageItem.mMediaBasic.thumbnailPath;
            if (RcsMessageUtils.isExistsFile(path)) {
                drawThumbnail(mImageContent, 0, path);
            } else {
                String url = mMessageItem.mMediaBasic.thumbnailUrl;
                int type = Constants.THUMBNAIL_TYPE;
                sendDownloadReq(url, type);
            }
        }       
        updateStatusView(mMessageItem.getType());
    }

    
    private void setVideoItem() {
        Log.d(TAG, "setVideoItem id=" + mMessageItem.mMsgId);

        prepareForVisibility(mMessageItem.getType());
        
        if (mMessageItem.mMediaBasic == null) {
            Log.e(TAG, "mMediaBasic is null !");
            return;
        }
        
        try {
            int size = Integer.parseInt(mMessageItem.mMediaBasic.fileSize);
            String strSize = RcsMessageUtils.formatFileSize(size, 2);
            //int duration = Integer.parseInt(mMessageItem.mMediaBasic.duration);
            //String strDur = RcsMessageUtils.formatAudioTime(duration);
            mContentSize.setText(strSize);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Parse image size failed:" + mMessageItem.mMediaBasic.fileSize);
            mContentSize.setText("");
        }

        String path = mMessageItem.mMediaBasic.thumbnailPath;
        if (RcsMessageUtils.isExistsFile(path)) {
            drawThumbnail(mImageContent, 0, path);
        } else if (mMessageItem.mDirection == Constants.MESSAGE_DIRECTION_INCOMING){
            String url = mMessageItem.mMediaBasic.thumbnailUrl;
            int type = Constants.THUMBNAIL_TYPE;
            sendDownloadReq(url, type);
        }
        updateStatusView(mMessageItem.getType());
    }
    
    
    private void setAudioItem() {
        Log.d(TAG, "setAudioItem id=" + mMessageItem.mMsgId);

        prepareForVisibility(mMessageItem.getType());

        if (mMessageItem.mMediaBasic == null) {
            Log.e(TAG, "mMediaBasic is null !");
            return;
        }
        String audioUrl = mMessageItem.mMediaBasic.originalUrl;
        String audioPath = mMessageItem.mMediaBasic.originalPath;
        int type = Constants.MEDIA_TYPE_AUDIO;
        if (audioPath == null || audioPath.isEmpty()) {
            //audio haven't download,start download
            sendDownloadReq(audioUrl, type);
            showAudioPreview(true);
        } else {
            int id = PAAudioService.getCurrentId();
            int state = PAAudioService.getCurrentState();
            if (id == mMessageItem.mMsgId &&
                ((state == PAAudioService.PLAYER_STATE_PLAYING ||
                  state == PAAudioService.PLAYER_STATE_PAUSE))) {
                //audio has been playing.
                boolean ret = mAudioService.bindAudio(mMessageItem.mMsgId, 
                        mMessageItem.mMediaBasic.originalPath, this);
                showAudioPlay(false);
            } else {
                //audio is ready to play.
                showAudioPreview(false);
            }
        }

        updateStatusView(mMessageItem.getType());
    }
    
    private void setSingleMixItem() {
        Log.d(TAG, "setSingleMixItem id=" + mMessageItem.mMsgId);

        if (mMessageItem.mDirection != Constants.MESSAGE_DIRECTION_INCOMING) {
            Log.e(TAG, "setSingleMixItem() but not incoming message: " + mMessageItem.mDirection);
            return;
        }

        prepareForVisibility(mMessageItem.getType());
        
        if (mMessageItem.mMediaArticles == null || 
                mMessageItem.mMediaArticles.size() == 0) {
            Log.e(TAG, "setSingleMixItem. mediaArticle is null !!!");
            return;
        }

        mSingleMixTitle.setText(mMessageItem.mMediaArticles.get(0).title);
        mSingleMixText.setText(mMessageItem.mMediaArticles.get(0).mainText);
        if (mMessageItem.mCreateTimeStamp > 0) {
            mSingleMixCreateDate.setText(RcsMessageUtils.
                    formatSimpleTimeStampString(mMessageItem.mCreateTimeStamp));
        }       
        
        String path = mMessageItem.mMediaArticles.get(0).thumbnailPath;
        Log.d(TAG, "Single mix item thumbnail path:" + path + ".");
        if (RcsMessageUtils.isExistsFile(path)) {
            drawThumbnail(mSingleMixLogo, 0, path);
        } else {
            String url = mMessageItem.mMediaArticles.get(0).thumbnailUrl;
            int type = Constants.THUMBNAIL_TYPE;
            sendDownloadReq(url, type);
            Log.d(TAG, "setSingleMixItem() download req for:");
        }
    }
    
    private void sendDownloadReq(String url, int type) {
        Log.d(TAG, "sendDownloadReq():" + mMessageItem.mMsgId + "|" + mMessageListAdapter);
        long reqId = mMessageListAdapter.sendDownloadRequest(url, type, mMessageItem.mMsgId);
        if (reqId > 0) {
            mMessageItem.setDownloadReqId(0, reqId);
            mMessageItem.setDownloadCallback(new DownloadCallback() {
                @Override
                public void onDownloadFinished(PaMessageItem messageItem, int index) {
                    if (isMessageItemMatched(messageItem.mMsgId)) {
                        messageItem.dumpItemInfo();
                        updateAfterDownload(index);
                    }
                }
            });
            Log.d(TAG, "sendDownloadReq() reqId=" + reqId);
        } else {
            Log.d(TAG, "sendDownloadReq() fail. reqId=" + reqId);
        }
    }
    
     void updateAfterDownload(int index) {
         Log.d(TAG, "updateAfterDownload(). index=" + index);
         
         String path = null;
         
         switch (mMessageItem.mType) {
         case Constants.MEDIA_TYPE_PICTURE:
         case Constants.MEDIA_TYPE_VIDEO:
             path = mMessageItem.mMediaBasic.thumbnailPath;
             drawThumbnail(mImageContent, 0, path);
             break;
             
         case Constants.MEDIA_TYPE_AUDIO:
             path = mMessageItem.mMediaBasic.originalPath;
             showAudioPreview(false);
             break;
             
         case Constants.MEDIA_TYPE_SINGLE_ARTICLE:
             path = mMessageItem.mMediaArticles.get(0).thumbnailPath;
             drawThumbnail(mSingleMixLogo, 0, path);
             break;
         
         case Constants.MEDIA_TYPE_MULTIPLE_ARTICLE:
             path = mMessageItem.mMediaArticles.get(index).thumbnailPath;
             if (index == 0) {
                 drawThumbnail(mMultiMixHeaderThumb, 0, path);
             } else {
                 drawThumbnail(mMultiMixBodyThumb[index - 1], index, path, DRAW_TYPE_SMALL);
             }
             
         default:
             break;
         }
    }
    
    private void showAudioPreview(boolean isDownloading) {
        mAudioMode = AUDIO_MODE_PREVIEW;

        mIpAudioPreview.setVisibility(View.VISIBLE);
        mIpAudioPlay.setVisibility(View.GONE);

        String strFileSize = null;
        String strDuration = null;
        int fileSize = 0;
        int duration = 0;

        try {
            fileSize = Integer.parseInt(mMessageItem.mMediaBasic.fileSize);
            strFileSize = RcsMessageUtils.formatFileSize(fileSize, 2);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            Log.d(TAG, "Audio size parse error:" + mMessageItem.mMediaBasic.fileSize);
        }

        try {
            duration = Integer.parseInt(mMessageItem.mMediaBasic.duration);
            strDuration = RcsMessageUtils.formatAudioTime(duration);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            Log.d(TAG, "Audio duration parse error:" + mMessageItem.mMediaBasic.duration);
        }
       
        if (null == strFileSize) {
            strFileSize = mMessageItem.mMediaBasic.fileSize;
        }        
        if (null == strDuration) {
            strDuration = mMessageItem.mMediaBasic.duration;
        }
        if (strFileSize != null) {
            mAudioInfo.setText(strFileSize);
        }
        if (strDuration != null) {
            mAudioDur.setText(strDuration);
        }
        if (isDownloading) {
            mAudioDownloadBar.setVisibility(View.VISIBLE);
        } else {
            mAudioDownloadBar.setVisibility(View.INVISIBLE);
        }
    }
    
    private void showAudioPlay(boolean reset) {
        mAudioMode = AUDIO_MODE_PLAY;

        mIpAudioPreview.setVisibility(View.GONE);
        mIpAudioPlay.setVisibility(View.VISIBLE);
    
        mAudioTimeDuration.setText(getTimeFromInt(mAudioService.getDuration()));
        if (reset) {
            mAudioTimePlay.setText(getTimeFromInt(0));
            mAudioPlayBar.setProgress(0);
        }

        if (null != mHandler) {
            mHandler.post(mAudioUpdateRunnable);
        }
    }

    Runnable mAudioUpdateRunnable = new Runnable() {
        @Override  
        public void run() {
            int progress;
            if (mAudioService.getDuration() > 0) {
                progress = mAudioService.getCurrentTime() * 100 / mAudioService.getDuration();
            } else {
                progress = 100;
            }

            mAudioTimePlay.setText(getTimeFromInt(mAudioService.getCurrentTime()));
            mAudioPlayBar.setProgress(progress);
            Log.d(TAG, "Timer task update: " + mAudioService.getCurrentTime());

            if (progress < 100 && null != mHandler && 
               mAudioService.getServiceStatus() == mAudioService.PLAYER_STATE_PLAYING) {  
                mHandler.postDelayed(mAudioUpdateRunnable, 500);  
            }
        }  
    };
    
    private String getTimeFromInt(int time) {

        if (time <= 0) {
            return "0:00";
        }
        SimpleDateFormat formatter = new SimpleDateFormat("mm:ss");
        return formatter.format(time);
    }

    private void setMultiMixItem() {
        Log.d(TAG, "setMultiMixItem id=" + mMessageItem.mMsgId);

        if (mMessageItem.mDirection != Constants.MESSAGE_DIRECTION_INCOMING) {
            Log.e(TAG, "setMultiMixItem() but not incoming message: " + mMessageItem.mDirection);
            return;
        }

        prepareForVisibility(mMessageItem.getType());

        int size = mMessageItem.mMediaArticles.size();
        Log.d(TAG, "setMultiMixItem size=" + size);
        MediaArticle ma;
        String path;
        String title;
        String text;
        for (int i = 0 ; i < size ; i ++) {
            ma = mMessageItem.mMediaArticles.get(i);
            path = ma.thumbnailPath;//mMessageItem.getThumbnailPath(i);
            title = ma.title;
            text = ma.mainText;
            if (0 == i) {
                mMultiMixHeaderText.setText(title);
                if (RcsMessageUtils.isExistsFile(path)) {                    
                    drawThumbnail(mMultiMixHeaderThumb, i, path);
                } else {
                    sendMixDownloadReq(0);
                }
                setMixListeners(mMultiMixHeaderView, ma.sourceUrl);
            } else {
                int pos = i - 1;
                mMultiAricleLayout[pos] = (LinearLayout)LayoutInflater.from(getContext()).
                        inflate(R.layout.multi_article_item, mMultiMixView, false);
                Log.d(TAG, "mMultiAricleLayout[" + pos  + "]=" + mMultiAricleLayout[pos]);
                mMutliMixBodyView[pos] = (LinearLayout)mMultiAricleLayout[pos].findViewById(R.id.multi_mix_body);
                mMultiMixBodyText[pos] = (TextView) mMultiAricleLayout[pos].findViewById(R.id.multi_mix_body_title);
                mMultiMixBodyText[pos].setText(title);
                mMultiMixBodyThumb[pos] = (ImageView)mMultiAricleLayout[pos].findViewById(R.id.multi_mix_body_thumb);
                mMultiMixBodyThumb[pos].setVisibility(View.VISIBLE);
                if (RcsMessageUtils.isExistsFile(path)) {
                    drawThumbnail(mMultiMixBodyThumb[pos], i, path, DRAW_TYPE_SMALL);                    
                } else {
                    sendMixDownloadReq(i);
                }
                setMixListeners(mMultiAricleLayout[pos], ma.sourceUrl);
                mMultiMixView.addView(mMultiAricleLayout[pos], i);
            }
        }
    }
    
    void sendMixDownloadReq(final int index) {
        String url = mMessageItem.mMediaArticles.get(index).thumbnailUrl;
        int type = Constants.THUMBNAIL_TYPE;
        Log.d(TAG, "sendMixDownloadReq:" + mMessageItem.mMsgId + "|" + mMessageListAdapter);
        long reqId = mMessageListAdapter.sendDownloadRequest(url, type, mMessageItem.mMsgId);
        if (reqId > 0) {
            mMessageItem.setDownloadReqId(index, reqId);
            mMessageItem.setDownloadCallback(new DownloadCallback() {
                @Override
                public void onDownloadFinished(PaMessageItem messageItem, int index) {
                    if (isMessageItemMatched(messageItem.mMsgId)) {
                        messageItem.dumpItemInfo();
                        updateAfterDownload(index);
                    }
                }
            });
        }
    }
    
    void setMixListeners(View view, final String url) {

        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "setMultiMixItem onClick");
                PaWebViewActivity.openHyperLink(getContext(), url, 
                        (mMessageItem.mForwardable == 1 ? true : false));
            }
        });

        view.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                v.showContextMenu();
                return true;
            }
        });
    }
    
    private boolean drawThumbnail(ImageView iv, int index, String path) {
        return drawThumbnail(iv, index, path, DRAW_TYPE_NORMAL);
    }
    
    private boolean drawThumbnail(ImageView iv, int index, String path, int type) {
        Log.d(TAG, "drawThumbnail path=" + path);

        Bitmap bitmap = mMessageItem.getMessageBitmap(index);
        if (path == null || path.isEmpty()) {
            return false;
        }
        
        File file = new File(path);
        if (!file.exists()) {
            Log.d(TAG, "drawThumbnail() but file not exist");
            return false;
        }
        
        if (null == bitmap) {
            Log.d(TAG, "drawThumbnail() bitmap cache is null, do redraw");
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            bitmap = BitmapFactory.decodeFile(path, options);
            int width = options.outWidth;
            int height = options.outHeight;
            int w = options.outHeight;
            int h = 0;            
            
            if (DRAW_TYPE_NORMAL == type) {
                int screenWidth = 0;
                DisplayMetrics dm = new DisplayMetrics();
                WindowManager wmg = (WindowManager)getContext().
                                getSystemService(Context.WINDOW_SERVICE);
                wmg.getDefaultDisplay().getMetrics(dm);
                if (dm.heightPixels > dm.widthPixels) {
                    screenWidth = dm.widthPixels;
                } else {
                    screenWidth = dm.heightPixels;
                }
                if (width > screenWidth * MAX_SCALE) {
                    w = (int)(screenWidth * MAX_SCALE);
                    h = height * w / width;
                } else if (width > screenWidth * MIN_SCALE) {
                    w = (int)(screenWidth  * MIN_SCALE);
                    h = height * w / width;
                } else {
                    w = width;
                    h = height;
                }
            } else if (type == DRAW_TYPE_SMALL) {
                w = 200;
                h = 200;                
            }
            
            bitmap = PaUtils.getBitmapByPath(path, options, w, h);
            mMessageItem.setMessageBitmapSize(w, h, index);
            mMessageItem.setMessageBitmapCache(bitmap, index);
        }

        if (null != bitmap) {
            ViewGroup.LayoutParams params = (ViewGroup.LayoutParams)iv.getLayoutParams();
            params.height = mMessageItem.getMessageBitmapHeight(index);
            params.width = mMessageItem.getMessageBitmapWidth(index);
            iv.setLayoutParams(params);
            iv.setImageBitmap(bitmap);
            Log.d(TAG, "drawThumbnail done. size=" + params.width + "," + params.height);
            adjustTextWidth(params.width);
            return true;
        }
        return false;
    }
    
    private void setGeolocItem() {
        Log.d(TAG, "setGeolocItem id=" + mMessageItem.mMsgId);

        prepareForVisibility(mMessageItem.getType());

        updateStatusView(mMessageItem.getType());
    }
    
    private void setVCardItem() {
        Log.d(TAG, "setVCardItem id=" + mMessageItem.mMsgId);

        prepareForVisibility(mMessageItem.getType());

        String cvfPath = mMessageItem.mMediaBasic.originalPath;
        if (mMessageItem.mVcardCount == 1) {
            if (mMessageItem.mVcardBitmap != null) {
                mVCardPortraint.setImageBitmap(mMessageItem.mVcardBitmap);
            }
            if (mMessageItem.mVcardName != null && !mMessageItem.mVcardName.isEmpty()) {
                mVCardInfo.setText(mMessageItem.mVcardName);
            }            
        } else {
            mVCardPortraint.setImageResource(R.drawable.ipmsg_chat_contact_vcard);
            mVCardInfo.setText(R.string.multi_contacts_name);
        }

        updateStatusView(mMessageItem.getType());
    }

    /*static private class ImageLoadedCallback implements ItemLoadedCallback<ImageLoaded> {
        private long mMessageId;
        private final PaMessageListItem mListItem;

        public ImageLoadedCallback(PaMessageListItem listItem) {
            mListItem = null;
        }

        public void reset(PaMessageListItem listItem) {
            mMessageId = listItem.getMessageItem().getMessageId();
        }

        public void onItemLoaded(ImageLoaded imageLoaded, Throwable exception) {

        }
    }*/

    private LineHeightSpan mSpan = new LineHeightSpan() {
        @Override
        public void chooseHeight(CharSequence text, int start,
                int end, int spanstartv, int v, FontMetricsInt fm) {
            fm.ascent -= 10;
        }
    };

    //TextAppearanceSpan mTextSmallSpan =
    //    new TextAppearanceSpan(mContext, android.R.style.TextAppearance_Small);

    ForegroundColorSpan mColorSpan = null;  // set in ctor


    private void sendMessage(PaMessageItem messageItem, int message) {
        if (mHandler != null) {
            Message msg = Message.obtain(mHandler, message);
            msg.obj = messageItem;
            msg.sendToTarget();
        }
    }

    public void onMessageListItemClick() {
        if (mMessageItem == null) {
            Log.d(TAG, "onMessageListItemClick():Message item is null !");
            return;
        }
        Log.d(TAG, "onMessageListItemClick onClick");

        //re-send for sent fail message. 
        if (Constants.MESSAGE_STATUS_FAILED == mMessageItem.mStatus &&
            Constants.MESSAGE_DIRECTION_OUTGOING == mMessageItem.mDirection) {

            Log.d(TAG, "onMessageListItemClick on sent fail item, do resent");
            sendMessage(mMessageItem, PaComposeActivity.ACTION_RESEND);
            return;
        }

        switch(mMessageItem.mType) {

        case Constants.MEDIA_TYPE_PICTURE:
            if (mMessageItem.mMediaBasic == null) {
                Toast.makeText(getContext(), "mediaBasic is null", Toast.LENGTH_LONG).show();
                break;
            }
            Intent picIntent = new Intent(getContext(), PAIpMsgContentShowActivity.class);
            picIntent.putExtra("type", Constants.MEDIA_TYPE_PICTURE);
            picIntent.putExtra("thumbnail_url", mMessageItem.mMediaBasic.thumbnailUrl);
            picIntent.putExtra("thumbnail_path", mMessageItem.mMediaBasic.thumbnailPath);
            picIntent.putExtra("original_url", mMessageItem.mMediaBasic.originalUrl);
            picIntent.putExtra("original_path", mMessageItem.mMediaBasic.originalPath);
            picIntent.putExtra("forwardable", (mMessageItem.mForwardable == 1 ? true : false));
            getContext().startActivity(picIntent);
            return;

        case Constants.MEDIA_TYPE_VIDEO:
            if (mMessageItem.mMediaBasic == null) {
                Toast.makeText(getContext(), "mediaBasic is null", Toast.LENGTH_LONG).show();
                break;
            }
            Intent vidoIntent = new Intent(getContext(), PAIpMsgContentShowActivity.class);
            vidoIntent.putExtra("type", Constants.MEDIA_TYPE_VIDEO);
            vidoIntent.putExtra("thumbnail_url", mMessageItem.mMediaBasic.thumbnailUrl);
            vidoIntent.putExtra("thumbnail_path", mMessageItem.mMediaBasic.thumbnailPath);
            vidoIntent.putExtra("original_url", mMessageItem.mMediaBasic.originalUrl);
            vidoIntent.putExtra("original_path", mMessageItem.mMediaBasic.originalPath);
            vidoIntent.putExtra("forwardable", (mMessageItem.mForwardable == 1 ? true : false));
            getContext().startActivity(vidoIntent);
            return;

        case Constants.MEDIA_TYPE_AUDIO:
            if (mMessageItem.mMediaBasic == null) {
                Toast.makeText(getContext(), "mediaBasic is null", Toast.LENGTH_LONG).show();
                break;
            }
            if (mMessageItem.mMediaBasic.originalPath != null) {
                if (mAudioMode == AUDIO_MODE_PREVIEW) {
                    //begin play a audio
                    boolean ret = mAudioService.bindAudio(mMessageItem.mMsgId, 
                            mMessageItem.mMediaBasic.originalPath, this);
                    if (ret) {
                        mAudioService.playAudio();
                        showAudioPlay(true);
                    }
                } else {
                    if (mAudioService.getServiceStatus() == mAudioService.PLAYER_STATE_PLAYING) {
                        mAudioService.pauseAudio();
                    } else {
                        mAudioService.playAudio();
                        mHandler.post(mAudioUpdateRunnable);
                    }
                }
            } else {
                Log.w(TAG, "onMessageListItemClick() Audio is not ready.");
            }
            break;

        case Constants.MEDIA_TYPE_SINGLE_ARTICLE:
            if (mMessageItem.mMediaArticles == null) {
                Toast.makeText(getContext(), "mediaArtical is null", Toast.LENGTH_LONG).show();
                break;
            }
            String url = mMessageItem.mMediaArticles.get(0).sourceUrl;
            PaWebViewActivity.openHyperLink(getContext(), url, 
                    (mMessageItem.mForwardable == 1 ? true : false));
            break;

        case Constants.MEDIA_TYPE_GEOLOC:
            if (mMessageItem.mMediaBasic == null) {
                Toast.makeText(getContext(), "mediaBasic is null", Toast.LENGTH_LONG).show();
                break;
            }
            GeoLocXmlParser parser = GeoLocUtils.parseGeoLocXml(mMessageItem.mMediaBasic.originalPath);
            double latitude = parser.getLatitude();
            double longitude = parser.getLongitude();
            Log.d(TAG, "parseGeoLocXml: latitude=" + latitude + ", longtitude=" + longitude);

            if (latitude != 0.0 || longitude != 0.0) {
                Uri uri = Uri.parse("geo:" + latitude + "," + longitude);
                Intent locIntent = new Intent(Intent.ACTION_VIEW,uri);
                getContext().startActivity(locIntent);
            } else {
                Toast.makeText(getContext(), "parse geoloc info fail", Toast.LENGTH_LONG).show();
            }
            break;

        case Constants.MEDIA_TYPE_VCARD:
            if (mMessageItem.mMediaBasic == null) {
                Toast.makeText(getContext(), "mediaBasic is null", Toast.LENGTH_LONG).show();
                break;
            }
            
            int entryCount = PaVcardUtils.getVcardEntryCount(mMessageItem.mMediaBasic.originalPath);
            Log.d(TAG, "vCard entryCount=" + entryCount);
            if (entryCount <= 0) {
                Toast.makeText(getContext(), "parse vCard error", Toast.LENGTH_LONG).show();
            } else if (entryCount == 1) {
                Uri uri = Uri.parse("file://" + mMessageItem.mMediaBasic.originalPath);
                Intent vcardIntent = new Intent("android.intent.action.rcs.contacts.VCardViewActivity");
                vcardIntent.setDataAndType(uri, "text/x-vcard".toLowerCase());
                vcardIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                mContext.startActivity(vcardIntent);
            } else {
                Resources res = getContext().getResources();
                AlertDialog.Builder b = new AlertDialog.Builder(getContext());
                b.setTitle(R.string.multi_contacts_name)
                 .setMessage(res.getString(R.string.multi_contacts_notification))
                 .setCancelable(true)
                 .setPositiveButton(android.R.string.ok, 
                         new DialogInterface.OnClickListener() {
                             @Override
                             public void onClick(DialogInterface dialog, int which) {
                                 // TODO Auto-generated method stub
                                 importVcard();
                             }
                         })
                 .setNegativeButton(android.R.string.cancel,
                         new DialogInterface.OnClickListener() {
                                
                             @Override
                             public void onClick(DialogInterface dialog, int which) {
                                 // TODO Auto-generated method stub
                                 dialog.dismiss();
                             }
                         })
                 .create().show();
            }
            break;

        default:
            break;
        }
    }
    
    private void importVcard() {
        Uri uri = Uri.parse("file://" + mMessageItem.mMediaBasic.originalPath);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "text/x-vcard");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        getContext().startActivity(intent);
    }
    
    private boolean isMessageItemMatched(int msgId) {
        Log.d(TAG, "isMessageItemMatched, msgId=" + msgId);

        boolean ret = false;
        if (mMessageItem == null) {
            Log.d(TAG, "isMessageItemMatched, mMessageItem is null");
        } else if (mMessageItem.mMsgId != msgId) {
            Log.d(TAG, "isMessageItemMatched, mMessageItem has chagne to" + mMessageItem.mMsgId);
        } else {
            ret = true;
        }
        return ret;
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mVisible = false;
    }
    
    // MTK Add
    public void setMessageListItemAdapter(PaMessageListAdapter adapter) {
        mMessageListAdapter = adapter;
    }
    
    public void setBodyTextSize(float size) {
        if (mBodyTextView != null && mBodyTextView.getVisibility() == View.VISIBLE) {
            mBodyTextView.setTextSize(size);
        }
    }

    @Override
    public void onError(int what, int extra) {
        if (null != mHandler) {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    showAudioPreview(false);
                }
            });
        }
    }

    @Override
    public void onCompletion() {
        if (null != mHandler) {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    showAudioPreview(false);
                }
            });
        }
    }

    
}
