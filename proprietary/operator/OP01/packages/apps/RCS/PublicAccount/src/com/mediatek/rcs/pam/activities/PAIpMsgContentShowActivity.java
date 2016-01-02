package com.mediatek.rcs.pam.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.net.Uri;

import android.os.Handler;
import android.os.Bundle;
import android.os.RemoteException;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import java.io.InputStream;
import java.util.Timer;


import com.mediatek.rcs.pam.activities.RcsMessageUtils;
import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.model.ResultCode;


import com.mediatek.rcs.pam.IPAServiceCallback;
import com.mediatek.rcs.pam.PAService;
import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.SimpleServiceCallback;

public class PAIpMsgContentShowActivity extends Activity {
    /** Called when the activity is first created. */
    private static final String TAG = "PA/PAIpMsgContentShowActivity";
   
    private int mType;
    private String mThumbnailPath;
    private String mOriginalUrl;
    private String mOriginalPath;
    private boolean mForwardable;

    private PAServiceCallback mPAServiceCallback;
    private PAService mPAService;
    private int mState;
    private long mReqId;
    
    private final static int STATE_INIT = 0;
    private final static int STATE_SRV_READY = 1;
    private final static int STATE_DOWNLOADING = 2;
    private final static int STATE_READY = 3;

    private ImageView mImageContent;
    private ImageButton mGifControl;
    private RelativeLayout mVideoContent;
    private ImageView mVideoPreview;
    private VideoView mVideoView;
    private ImageButton mVideoControl;
    private ProgressBar mProgressBar;
    private TextView mProgressText;
    
    private int mProgress;
    
    private boolean mIsGif = false;

    //MediaController mController;
    int mVideoState;
    
    private final static int VIDEO_STATE_NOT_READY = 0;
    private final static int VIDEO_STATE_READY = 1;
    private final static int VIDEO_STATE_PLAYING = 2;
    private final static int VIDEO_STATE_PAUSE = 3;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ipmsg_content_show);

        mProgressText = (TextView) findViewById(R.id.progress_text);
        mImageContent = (ImageView) findViewById(R.id.ip_msg_pic_content);
        mGifControl = (ImageButton) findViewById(R.id.ip_msg_gif_control);
        mGifControl.setOnClickListener(mClickListener);
        
        mVideoContent = (RelativeLayout) findViewById(R.id.ip_msg_video_content);
        //mVideoContent.setOnClickListener(mClickListener);

        mVideoPreview = (ImageView) findViewById(R.id.ip_msg_video_preview);
        mVideoView = (VideoView) findViewById(R.id.ip_msg_video_view);
        mVideoControl = (ImageButton) findViewById(R.id.ip_msg_video_control);
        mVideoControl.setOnClickListener(mClickListener);
        mVideoView.setVisibility(View.GONE);
        mVideoControl.setVisibility(View.GONE);

        mProgressBar = (ProgressBar) findViewById(R.id.progress_large);
        mProgressBar.setVisibility(View.INVISIBLE);
        
        mType = getIntent().getIntExtra("type", 0);
        mThumbnailPath = getIntent().getStringExtra("thumbnail_path");
        mOriginalUrl = getIntent().getStringExtra("original_url");
        mOriginalPath = getIntent().getStringExtra("original_path");
        mForwardable = getIntent().getBooleanExtra("forwardable", true);
        if (mType == Constants.MEDIA_TYPE_PICTURE) {
            String ext = RcsMessageUtils.getFileExtension(mOriginalPath);
            if ("gif".equalsIgnoreCase(ext)) {
                mIsGif = true;
            }
        }

        ActionBar actionBar = getActionBar();  
        actionBar.setDisplayShowTitleEnabled(false);  
        actionBar.setDisplayShowHomeEnabled(false);  

        mPAServiceCallback = new PAServiceCallback();
        mPAService = new PAService(this, mPAServiceCallback);
        mPAService.connect();
    }
    
    @Override
    public void onStart() {
        super.onStart();

        setMessageContent();
    }
    
    @Override
    public void onStop() {
        if (mState == STATE_DOWNLOADING && mReqId > 0) {
            mPAService.cancelDownload(mReqId);
            changeState(STATE_SRV_READY);
        }
        super.onStop();
    }
    
    @Override 
    public void onDestroy() {
        super.onDestroy();

        mPAService.disconnect();
    }

    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }
    
    private void drawImage(ImageView view, String path) {
        try {
            File file = new File(path);
            InputStream in = new FileInputStream(file);
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            view.setImageBitmap(bitmap);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    private void setMessageContent() {
        switch (mType) {
            case Constants.MEDIA_TYPE_PICTURE:
                mVideoContent.setVisibility(View.GONE);
                mImageContent.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.GONE);
                mGifControl.setVisibility(View.GONE);

                if (mOriginalPath != null) {
                    if (RcsMessageUtils.isExistsFile(mOriginalPath)) {
                        //Original image exist, draw it directly.
                        drawImage(mImageContent, mOriginalPath);
                        if (mIsGif) {
                            mGifControl.setVisibility(View.VISIBLE);
                        }
                        return;
                    }
                }
                //draw thumbnail firstly.
                if (mThumbnailPath != null) {
                    if (RcsMessageUtils.isExistsFile(mThumbnailPath)) {
                        drawImage(mImageContent, mThumbnailPath);
                    }
                }
                
                if (mState == STATE_SRV_READY) {
                    mReqId = mPAService.downloadObject(mOriginalUrl, mType);
                    if (mReqId > 0) {
                        changeState(STATE_DOWNLOADING);
                    }
                } else if (mState == STATE_DOWNLOADING) {
                    Log.d(TAG, "Already downloading, warting...");
                } else if (mState == STATE_INIT) {
                    Log.d(TAG, "Serivce not ready, download later...");
                } else {
                    Log.e(TAG, "Error state: " + mState);
                }
                return;

            case Constants.MEDIA_TYPE_VIDEO:

                mImageContent.setVisibility(View.GONE);
                mVideoContent.setVisibility(View.VISIBLE);
                mVideoControl.setVisibility(View.GONE);
                mGifControl.setVisibility(View.GONE);

                if (mThumbnailPath != null) {
                    if (RcsMessageUtils.isExistsFile(mThumbnailPath)) {
                        //drwaPreview thumbnail
                        drawImage(mVideoPreview, mThumbnailPath);
                    }
                }

                if (mOriginalPath != null) {
                    if (RcsMessageUtils.isExistsFile(mOriginalPath)) {
                        //video is ready, draw control button
                        mVideoControl.setVisibility(View.VISIBLE);
                        mVideoState = VIDEO_STATE_READY;
                        return;
                    }
                }

                if (mState == STATE_SRV_READY) {
                    mReqId = mPAService.downloadObject(mOriginalUrl, mType);
                    if (mReqId > 0) {
                        changeState(STATE_DOWNLOADING);
                    }
                } else if (mState == STATE_DOWNLOADING) {
                    Log.d(TAG, "Already downloading, warting...");
                } else if (mState == STATE_INIT) {
                    Log.d(TAG, "Serivce not ready, download later...");
                } else {
                    Log.e(TAG, "Error state: " + mState);
                }
                return;

            default:
                Log.d(TAG, "setMessageContent(). unknown type=" + mType);
                break;
        }

    }

    private OnClickListener mClickListener = new OnClickListener() {
        public void onClick(View v) {

            if (mIsGif) {
                File file = new File(mOriginalPath);

                /* Resolution 2: play in media player commmon acitvity*/
                Uri mGifUri = Uri.fromFile(file);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(mGifUri, "image/gif");
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                }
                
            } else if (mVideoState == VIDEO_STATE_READY) {
                File file = new File(mOriginalPath);

                /* Resolution 2: play in media player commmon acitvity*/
                Uri mVideoUri = Uri.fromFile(file);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra("SingleItemOnly", true);
                intent.putExtra("CanShare", false);
                //intent.putExtra(EXTRA_FULLSCREEN_NOTIFICATION, true);
                // mType = MessageUtils.getContentType(mType, mTitle);
                intent.setDataAndType(mVideoUri, "video/*");
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                }

            } else if (mVideoState == VIDEO_STATE_PLAYING) {
                //mVideoView.pause();
            } else if (mVideoState == VIDEO_STATE_PAUSE) {
                //mVideoView.resume();
            } else {
                Log.e(TAG, "error state:" + mVideoState);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        //finish();
    }
    
    private void onServiceReady() {
        if (mOriginalPath == null || !RcsMessageUtils.isExistsFile(mOriginalPath)) {
            mReqId = mPAService.downloadObject(mOriginalUrl, mType);
            if (mReqId > 0) {
                changeState(STATE_DOWNLOADING);
            }
        } else {
            changeState(STATE_READY);
        }
    }
    
    private void changeState(int state) {
        Log.d(TAG, "changeState() from " + mState + " to " + state);
        if (mState != state) {
            mState = state;
            if (mState == STATE_DOWNLOADING) {
                mProgressBar.setVisibility(View.VISIBLE);
                mProgressText.setVisibility(View.VISIBLE);
                mProgressText.setText(mProgress + "%");
            } else {
                mProgressBar.setVisibility(View.GONE);
                mProgressText.setVisibility(View.GONE);
            }
        }
    }
    
    private void onDownloadSuccess() {
        changeState(STATE_READY);
        if (mOriginalPath == null || !RcsMessageUtils.isExistsFile(mOriginalPath)) {
            Log.e(TAG, "Download done ! but no file exist");
            Toast.makeText(this, "No valid file found !", Toast.LENGTH_LONG).show();
            return;
        }
        if (mType == Constants.MEDIA_TYPE_PICTURE) {
            drawImage(mImageContent, mOriginalPath);
            if (mIsGif) {
                mGifControl.setVisibility(View.VISIBLE);
            }
        } else if (mType == Constants.MEDIA_TYPE_VIDEO) {
            //mVideoPreview.setVisibility(View.GONE);
            mVideoControl.setVisibility(View.VISIBLE);
            mVideoState = VIDEO_STATE_READY;

            /*resolution 2: play by self*/
            //File file = new File(mOriginalPath);
            //Uri mVideoUri = Uri.fromFile(file);

            //mController = new MediaController(this);
            mVideoView.setVisibility(View.GONE);
            //mVideoView.setVisibility(View.VISIBLE);
            //mVideoView.setVideoPath(file.getAbsolutePath());  
            //mVideoView.setMediaController(mController);

            //mController.setMediaPlayer(mVideoView);
            //mVideoView.requestFocus();
        }
        invalidateOptionsMenu();
    }
    
    private void onDownloadFail() {
        changeState(STATE_SRV_READY);
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Download fail").
        setMessage("Do you want to download again ?").
        setPositiveButton("Yes",  new DialogInterface.OnClickListener() {  
            public void onClick(DialogInterface dialog, int whichButton) {  
                mReqId = mPAService.downloadObject(mOriginalUrl, mType);
                if (mReqId > 0) {
                    changeState(STATE_DOWNLOADING);
                }
            }
        }).
        setNegativeButton("No", new DialogInterface.OnClickListener() {  
            public void onClick(DialogInterface dialog, int whichButton) {  
                finish();
            }  
        }).
        setCancelable(false);
        if (!isFinishing()) {
            b.create().show();
        } else {
            Log.d(TAG, "ContentShow Activity has finished, Dialog not popup");
        }
    }
    
    private void forwardMessage() {
        Intent intent = new Intent();
        intent.putExtra("forwarded_message", true);
        intent.putExtra("forwarded_ip_message", true);

        intent.setAction("android.intent.action.ACTION_RCS_MESSAGING_SEND");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        switch (mType) {

        case Constants.MEDIA_TYPE_PICTURE:
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_STREAM, mOriginalPath);
            Log.d(TAG, "forward picture Message:" + mOriginalPath);
            break;

        case Constants.MEDIA_TYPE_VIDEO:
            intent.setType("video/*");
            intent.putExtra(Intent.EXTRA_STREAM, mOriginalPath);
            Log.d(TAG, "forward video Message:" + mOriginalPath);
            break;

        case Constants.MEDIA_TYPE_SINGLE_ARTICLE:
        case Constants.MEDIA_TYPE_GEOLOC:
        case Constants.MEDIA_TYPE_AUDIO:
        case Constants.MEDIA_TYPE_VCARD:
        case Constants.MEDIA_TYPE_TEXT: 
        default:
            return;
        }

        String str = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (str == null || str.isEmpty()) {
            str = intent.getStringExtra(Intent.EXTRA_STREAM);
        }
        Toast.makeText(this, "forward:" + str, Toast.LENGTH_LONG).show();
        startActivity(intent);
        finish();
        return;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        menu.clear();

        switch (mType) {
        case Constants.MEDIA_TYPE_VIDEO:
        case Constants.MEDIA_TYPE_PICTURE:
            if (mOriginalPath != null && !mOriginalPath.isEmpty() && mForwardable) {
                menu.add(0, Menu.FIRST + 1, 0, R.string.menu_forward).
                setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                return true;
            }
            break;
        default:
            break;
        }

        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == Menu.FIRST + 1) {
            forwardMessage();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    class PAServiceCallback extends SimpleServiceCallback {

        @Override
        public void onServiceConnected() throws RemoteException {
            Log.i(TAG, "onServiceConnected.");       
        }

        private Context getContext() {
            return getContext();
        }

        @Override
        public void onServiceDisconnected(int reason) throws RemoteException {
            Log.i(TAG, "onServiceDisconnected For reason:" + reason);
            changeState(STATE_INIT);
        }

        @Override
        public void onServiceRegistered() throws RemoteException {
            Log.i(TAG, "onServiceRegistered.");
            changeState(STATE_SRV_READY);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onServiceReady();
                }
            });
        }

        @Override
        public void onServiceUnregistered() throws RemoteException {
            Log.i(TAG, "onServiceUnregistered");
            changeState(STATE_INIT);
        }

        @Override
        public void reportDownloadResult(final long requestId, int resultCode, final String path, final long mediaId)
                throws RemoteException {
            Log.i(TAG, "reportDownloadResult. requestId =" + requestId + 
                    ". resultCode=" + resultCode + ". path=" + path);

            if (requestId != mReqId) {
                Log.d(TAG, "Ignore for unknown reqid: " + mReqId);
                return;
            }

            //reset request id.
            mReqId = 0;
            
            if (resultCode != ResultCode.SUCCESS) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onDownloadFail();
                    }
                });
                return;
            }

            if (resultCode == ResultCode.SUCCESS) {
                mOriginalPath = path;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onDownloadSuccess();
                    }
                });
            }
        }

        @Override
        public void updateDownloadProgress(long requestId, int percentage)
                throws RemoteException {
            // TODO Auto-generated method stub
            Log.i(TAG, "updateDownloadProgress. requestId =" + requestId + 
                    ". percentage=" + percentage);
            mProgress = percentage;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressText.setText(mProgress + "%");
                }
            });
        }

    }
}
