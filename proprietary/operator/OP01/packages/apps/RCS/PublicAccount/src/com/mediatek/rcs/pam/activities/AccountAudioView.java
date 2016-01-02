package com.mediatek.rcs.pam.activities;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.activities.PAAudioService.IAudioServiceCallBack;
import com.mediatek.rcs.pam.model.MessageContent;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class AccountAudioView extends LinearLayout implements IAudioServiceCallBack,
        IAudioDownloadListener {

    private static final String TAG = Constants.TAG_PREFIX + "AccountAudioView";

    private final static int AUDIO_MODE_PREVIEW = 0;
    private final static int AUDIO_MODE_PLAY = 1;

    private int mPosition = 1;
    private MessageContent mMessage;

    private int mAudioMode;
    private Handler mHandler;
    private PAAudioService mAudioService;

    private LinearLayout mIpAudioView;
    private LinearLayout mIpAudioPreview;
    private LinearLayout mIpAudioPlay;

    private TextView mAudioInfo;
    private TextView mAudioDur;
    private ProgressBar mAudioDownloadBar;

    private ProgressBar mAudioPlayBar;
    private TextView mAudioTimePlay;
    private TextView mAudioTimeDuration;

    public AccountAudioView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.account_history_item_audio, this);

        Log.d(TAG, "PaMessageListItem(Context context, AttributeSet attrs)");
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // inflate layout views
        mIpAudioView = (LinearLayout) findViewById(R.id.ip_audio);
        mIpAudioPreview = (LinearLayout) findViewById(R.id.ip_audio_preview);
        mIpAudioPlay = (LinearLayout) findViewById(R.id.ip_audio_play);

        // inflate views of preview
        mAudioInfo = (TextView) findViewById(R.id.ip_audio_info);
        mAudioDur = (TextView) findViewById(R.id.ip_audio_dur);
        mAudioDownloadBar = (ProgressBar) findViewById(R.id.ip_audio_downLoad_bar);

        // inflate views of play
        mAudioPlayBar = (ProgressBar) findViewById(R.id.ip_audio_play_bar);
        if (mAudioPlayBar != null) {
            mAudioPlayBar.setMax(100);
        }
        mAudioTimePlay = (TextView) findViewById(R.id.ip_audio_time_play);
        mAudioTimeDuration = (TextView) findViewById(R.id.ip_audio_time_duration);

        mIpAudioPreview.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                openAudio();
            }
        });
        mIpAudioPlay.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                openAudio();
            }
        });
    }

    /*
     * Bind view to info when adapter.getView called
     */
    public void bind(MessageContent message, int position, IAudioDownloader audioDownloader) {
        Log.i(TAG, "bind position " + position);

        mAudioService = PAAudioService.getService();
        mMessage = message;
        mPosition = position;
        mHandler = new Handler();

        String audioUrl = mMessage.basicMedia.originalUrl;
        String audioPath = mMessage.basicMedia.originalPath;

        if (audioPath == null || audioPath.isEmpty()) {
            showAudioPreview(true);
            audioDownloader.downloadAudio(this, audioUrl);

        } else {
            int id = PAAudioService.getCurrentId();
            int state = PAAudioService.getCurrentState();
            Log.i(TAG, "id is " + id + ", state is " + state);

            if (id == mPosition && ((state == PAAudioService.PLAYER_STATE_PLAYING ||
                    state == PAAudioService.PLAYER_STATE_PAUSE))) {
                // audio has been playing.
                mAudioService.bindAudio(mPosition, audioPath, this);
                showAudioPlay(false);
            } else {
                // audio is ready to play.
                showAudioPreview(false);
            }
        }
    }

    /*
     * unbind view when it's recycled
     */
    public void unbind() {
        Log.i(TAG, "unbind position " + mPosition);

        if (mAudioService != null) {
            mAudioService.unBindAudio(mPosition);
        }
    }

    /*
     * If audio file ready, open it and switch play/pause state.
     */
    private void openAudio() {
        Log.i(TAG, "onClick to open audio");

        String originalPath = mMessage.basicMedia.originalPath;
        Log.i(TAG, "originalPath is " + originalPath);

        if (originalPath != null) {

            Log.i(TAG, "mAudioMode is " + mAudioMode);

            if (mAudioMode == AUDIO_MODE_PREVIEW) {
                // begin play a audio
                boolean ret = mAudioService.bindAudio(mPosition, originalPath, this);
                Log.i(TAG, "ret is " + ret);

                if (ret) {
                    mAudioService.playAudio();
                    showAudioPlay(true);
                }
            } else {
                if (mAudioService.getServiceStatus() == PAAudioService.PLAYER_STATE_PLAYING) {
                    Log.i(TAG, "pauseAudio");
                    mAudioService.pauseAudio();

                } else {
                    Log.i(TAG, "playAudio");
                    mAudioService.playAudio();
                    mHandler.post(mAudioPlayRunnable);
                }
            }
        } else {
            Log.i(TAG, "onClick Audio is not ready.");
        }
    }

    /*
     * Show preview view: audio size and duration
     */
    private void showAudioPreview(final boolean isDownloading) {
        Log.i(TAG, "showAudioPreview " + isDownloading);
        mAudioMode = AUDIO_MODE_PREVIEW;

        mIpAudioView.post(new Runnable() {

            @Override
            public void run() {
                mIpAudioPreview.setVisibility(View.VISIBLE);
                mIpAudioPlay.setVisibility(View.GONE);

                mAudioInfo.setText(mMessage.basicMedia.fileSize);
                int duration = Integer.parseInt(mMessage.basicMedia.duration);
                mAudioDur.setText(RcsMessageUtils.formatAudioTime(duration));

                if (isDownloading) {
                    mAudioDownloadBar.setVisibility(View.VISIBLE);
                } else {
                    mAudioDownloadBar.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    /*
     * Show play view: current progress & time, duration
     */
    private void showAudioPlay(final boolean reset) {
        Log.i(TAG, "showAudioPlay " + reset);

        mAudioMode = AUDIO_MODE_PLAY;

        mIpAudioView.post(new Runnable() {

            @Override
            public void run() {
                mIpAudioPreview.setVisibility(View.GONE);
                mIpAudioPlay.setVisibility(View.VISIBLE);

                mAudioTimeDuration.setText(getTimeFromInt(mAudioService.getDuration()));
                if (reset) {
                    mAudioTimePlay.setText(getTimeFromInt(0));
                    mAudioPlayBar.setProgress(0);
                }

                if (null != mHandler) {
                    mHandler.post(mAudioPlayRunnable);
                }
            }
        });
    }

    /*
     * Refresh the play view every 0.5 second
     */
    private Runnable mAudioPlayRunnable = new Runnable() {
        @Override
        public void run() {
            int progress;
            if (mAudioService.getDuration() > 0) {
                progress = mAudioService.getCurrentTime() * 100 / mAudioService.getDuration();
            } else {
                progress = 100;
            }

            String time = getTimeFromInt(mAudioService.getCurrentTime());
            mAudioTimePlay.setText(time);
            mAudioPlayBar.setProgress(progress);

            if (progress % 5 == 0) {
                Log.i(TAG, "[AudioPlayRunnable] progress " + progress + ", " + time);
            }

            if (progress < 100 && null != mHandler
                    && mAudioService.getServiceStatus() == PAAudioService.PLAYER_STATE_PLAYING) {
                mHandler.postDelayed(mAudioPlayRunnable, 500);
            }
        }
    };

    /*
     * Convert int time to "mm:ss" format
     */
    private String getTimeFromInt(int time) {

        if (time <= 0) {
            return "0:00";
        }
        SimpleDateFormat formatter = new SimpleDateFormat("mm:ss", Locale.getDefault());
        return formatter.format(time);
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

    /*
     * Update audio path and the view, after download finishes
     */
    @Override
    public void downloadComplete(String link, String filePath) {
        Log.i(TAG, "downloadComplete " + filePath);

        if (mMessage != null && mMessage.basicMedia.originalUrl.equals(link)) {
            mMessage.basicMedia.originalPath = filePath;
        }

         showAudioPreview(false);
    }

}
