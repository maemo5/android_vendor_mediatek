package com.mediatek.rcs.pam.activities;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.model.MessageContent;

import java.util.ArrayList;

public class AccountHistoryMsgItemView extends LinearLayout {

    private static String TAG = Constants.TAG_PREFIX + "AccountHistoryMsgItemView";

    private AccountHistoryMsgUtils mAccountHistoryMsgUtils;
    private int mMediaType;
    private int mPosition;

    private TextView mTimeView;
    private TextView mTextMsgView;
    private LinearLayout mImageVideoLayout;
    private ImageView mGelocView;
    private LinearLayout mVcardLayout;
    private LinearLayout mSingleMix;
    private LinearLayout mMultipleMix;
    private AccountAudioView mAudioView;
    private ArrayList<View> mMultiBodyItemList;

    public AccountHistoryMsgItemView(Context context) {
        super(context);
    }

    public AccountHistoryMsgItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.i(TAG, "AccountHistoryMsgItemView(" + context + ", " + attrs + ")");
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mTimeView = (TextView) findViewById(R.id.tv_text_time);
        mTextMsgView = (TextView) findViewById(R.id.tv_text_message);
        mImageVideoLayout = (LinearLayout) findViewById(R.id.ip_image);
        mGelocView = (ImageView) findViewById(R.id.ip_geoloc);
        mVcardLayout = (LinearLayout) findViewById(R.id.ip_vcard);
        mSingleMix = (LinearLayout) findViewById(R.id.ip_single_mix);
        mMultipleMix = (LinearLayout) findViewById(R.id.ip_multi_mix);
        mAudioView = (AccountAudioView) findViewById(R.id.ip_audio_view);
    }

    public void bind(MessageContent message, int position,
            AccountHistoryMsgUtils accountHistoryMsgUtils) {
        Log.i(TAG, "bind " + position);

        mAccountHistoryMsgUtils = accountHistoryMsgUtils;

        hideAllViews();

        // update time view for all types
        mTimeView.setVisibility(View.VISIBLE);
        mTimeView.setText(AccountHistoryMsgUtils.getFormatterTime(message.createTime));

        mPosition = position;
        mMediaType = message.mediaType;
        switch (mMediaType) {
            case Constants.MEDIA_TYPE_TEXT:
                mAccountHistoryMsgUtils.updateText(mTextMsgView, message);
                break;

            case Constants.MEDIA_TYPE_PICTURE:
                mAccountHistoryMsgUtils.updateImageOrVideoView(mImageVideoLayout, message);
                break;

            case Constants.MEDIA_TYPE_VIDEO:
                mAccountHistoryMsgUtils.updateImageOrVideoView(mImageVideoLayout, message);
                break;

            case Constants.MEDIA_TYPE_AUDIO:
                mAudioView.setVisibility(View.VISIBLE);
                mAccountHistoryMsgUtils.getAudioDownloader().addListener(mAudioView);
                mAudioView.bind(message, position, mAccountHistoryMsgUtils.getAudioDownloader());
                break;

            case Constants.MEDIA_TYPE_GEOLOC:
                mAccountHistoryMsgUtils.updateGeoView(mGelocView, message);
                break;

            case Constants.MEDIA_TYPE_VCARD:
                mAccountHistoryMsgUtils.updateVcard(mVcardLayout, message);
                break;

            case Constants.MEDIA_TYPE_SINGLE_ARTICLE:
                mAccountHistoryMsgUtils.updateSingleArticleView(mSingleMix, message);
                break;

            case Constants.MEDIA_TYPE_MULTIPLE_ARTICLE:
                mMultiBodyItemList = mAccountHistoryMsgUtils.updateMultiArticleView(mMultipleMix,
                        message);
                break;

            default:
                break;
        }
    }

    public void unbind() {
        Log.i(TAG, "unbind " + mPosition + ", type " + mMediaType);
        if (mMediaType == Constants.MEDIA_TYPE_MULTIPLE_ARTICLE && mMultipleMix != null) {
            for (int i = 0; i < mMultiBodyItemList.size(); i ++) {
                mMultipleMix.removeView(mMultiBodyItemList.get(i));
            }
        } else if (mMediaType == Constants.MEDIA_TYPE_AUDIO && mAudioView != null) {
            Log.i(TAG, "ubind the audio view");
            mAccountHistoryMsgUtils.getAudioDownloader().removeListener(mAudioView);
            mAudioView.unbind();
        }
    }

    public int getPosition() {
        return mPosition;
    }

    private void hideAllViews() {
        mTimeView.setVisibility(View.GONE);
        mTextMsgView.setVisibility(View.GONE);
        mImageVideoLayout.setVisibility(View.GONE);
        mGelocView.setVisibility(View.GONE);
        mVcardLayout.setVisibility(View.GONE);
        mSingleMix.setVisibility(View.GONE);
        mMultipleMix.setVisibility(View.GONE);
        mAudioView.setVisibility(View.GONE);
    }

}
