package com.mediatek.rcs.message.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.net.Uri;

import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Telephony.Sms;
import android.database.sqlite.SqliteWrapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;


import com.mediatek.rcs.common.INotifyListener;
import com.mediatek.rcs.common.IpAttachMessage;
import com.mediatek.rcs.common.IpTextMessage;
import com.mediatek.rcs.common.IpImageMessage;
import com.mediatek.rcs.common.IpVideoMessage;
import com.mediatek.rcs.common.MessageStatusUtils.IFileTransfer.Status;
import com.mediatek.rcs.common.MessageStatusUtils.IFileTransfer;
import com.mediatek.rcs.common.RCSMessageManager;
import com.mediatek.rcs.common.IpMessage;
import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.common.IpMessageConsts.IpMessageType;

import com.mediatek.rcs.message.plugin.EmojiImpl;
import com.mediatek.rcs.message.R;

public class RcsIpMsgContentShowActivity extends Activity implements INotifyListener {
    /** Called when the activity is first created. */
	private static final String TAG = "RcsIpMsgContentShowActivity";


	 private int recLen = 5;
	 private long position;

	 TextView ipMsgTextContentShowText;
	 ImageView ipMsgVideoContentShowText;
	 ImageView ipMsgImageContentShowText;
	 ImageView ipMsgVideoBtn;
	 Timer timer = null;
	 ProgressBar progressLarge;
	 // private Handler progressHandler ;
	 Context mContext;
	 ProgressThread pt;
	 private boolean isStopThreadRunning = false;
	 private boolean isBurnedMsg = false;
	 private static Bitmap sThumbDefaultVideo;
	 SharedPreferences sp;

	 private Handler mDownloadMsgHandler = null;
	 
	 private long mIpMsgId;
	 private long mThreadId;
	 private Status mIpStatus = Status.WAITING;
	 private int mIpMsgType = -1;
	 IpMessage mIpMessage = null;
	 private boolean mIsFavoritaSpamCall;
	 
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ipmsg_content_show);
        Log.d(TAG, " [BurnedMsg] onCreate() ");
        mContext = this;

        sp = ContextCacher.getPluginContext().getSharedPreferences(IpMessageConsts.BurnedMsgStoreSP.PREFS_NAME, Context.MODE_WORLD_READABLE);
        ipMsgTextContentShowText = (TextView) findViewById(R.id.ip_msg_text_content);
        ipMsgImageContentShowText = (ImageView) findViewById(R.id.ip_msg_pic_content);
        ipMsgVideoContentShowText = (ImageView) findViewById(R.id.ip_msg_video_content);
        ipMsgVideoBtn = (ImageView) findViewById(R.id.video_media_paly);
        ipMsgVideoContentShowText.setOnClickListener(mShowListener);
        progressLarge = (ProgressBar) findViewById(R.id.progress_large);

        mIpMsgId = getIntent().getLongExtra("ipmsg_id", 0);
        mThreadId = getIntent().getLongExtra("thread_id", 0);
        position = getIntent().getLongExtra("position_id", 0);
        // favorite or spam msg show
        mIsFavoritaSpamCall = getIntent().getBooleanExtra("fav_spam", false);
        if (mIsFavoritaSpamCall) {
            Log.d(TAG, "mIsFavoritaSpamCall ture");
            showFavoritaSpamMsg(getIntent().getStringExtra("path"),
                    getIntent().getIntExtra("type", -1));
            return;
        } else {
        	setIpMsgInfo();
            // / M: add for ip message, notification listener
            RCSServiceManager.getInstance().registNotifyListener(this);
        }
    }

    private void setIpMsgInfo() {
    	Log.d(TAG, " [BurnedMsg]:  setIpMsgInfo() ");
    	mIpMessage = RCSMessageManager.getInstance(mContext).getIpMsgInfo(mThreadId,mIpMsgId);
    	if (mIpMessage != null) {
    		mIpMsgType = mIpMessage.getType();
    		isBurnedMsg = mIpMessage.getBurnedMessage();
//    		if(mIpMsgType >0){
//    			mIpStatus = ((IpAttachMessage) mIpMessage).getRcsStatus();
//    		} 
    		Log.d(TAG, " [BurnedMsg]: mIpMsgId = "+ mIpMsgId + "  ipMsgType = " + mIpMsgType + 
    				   "  isBurnedMsg = " + isBurnedMsg +"  mIpMessage = "+mIpMessage );
    	}

    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, " [BurnedMsg]:  onResume() ");
        
        //Skiping if the activity is called by favorite or spam.
        if(mIsFavoritaSpamCall) {
            Log.d(TAG, "onResume mIsFavoritaSpamCall true, return");
            return;
        }
        
        if(mIpMessage != null && mIpMessage.getBurnedMessage() ) {
	        Window win = getWindow();
	        win.addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        //(new DownloadFTAsyncTask(this)).execute();
        
        this.runOnUiThread(mRunnable);
        (new DownloadFTAsyncTask(this)).execute();
    }
    
    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
        	Log.d(TAG, " [BurnedMsg]:  run() ");
            if(mIpMessage == null ) {
            	Log.d(TAG, " [BurnedMsg]: mIpMessage is null");
            	return;
            }
            
            if (mIpMsgType == IpMessageType.TEXT) {
    	    	isStopThreadRunning = true;
    	    	hideprogressBar();
    	    	setMessageContent();
    	    	if(isBurnedMsg) {
        	    	saveMsgId(mIpMsgId);
    	    	}

            } else {
            	Log.d(TAG, " [BurnedMsg]: show msg content:  getRcsStatus() = " + ((IpAttachMessage)mIpMessage).getRcsStatus()
            			+ "  getStatus() = " + mIpMessage.getStatus());
            	if ((mIpMessage.getStatus() == Sms.MESSAGE_TYPE_OUTBOX || mIpMessage.getStatus() == Sms.MESSAGE_TYPE_SENT) 
            			|| (mIpMessage.getStatus() == Sms.MESSAGE_TYPE_INBOX &&
                                ((IpAttachMessage)mIpMessage).getRcsStatus() == Status.FINISHED)) {
        	    	isStopThreadRunning = true;
        	    	hideprogressBar();
        	    	
        	    	setMessageContent();
            	} else if(mIpMessage.getStatus() == Sms.MESSAGE_TYPE_INBOX &&
                        ((IpAttachMessage)mIpMessage).getRcsStatus() != Status.FINISHED) {
//                    if (mIpMsgType == IpMessageType.PICTURE) {
//                        IpImageMessage imageMessage = (IpImageMessage) mIpMessage;
//                        showPictureThumbnail(imageMessage.getThumbPath());
//                    } else if (mIpMsgType == IpMessageType.VIDEO) {
//                        IpVideoMessage videoMessage = (IpVideoMessage) mIpMessage;
//                        showVideoThumbnail(videoMessage.getThumbPath());
//                        ipMsgVideoBtn.setVisibility(View.VISIBLE);
//                    }
            	}
            	    
            	
            }
        	
            /*
        	if ((mIpMessage.getStatus() == Sms.MESSAGE_TYPE_OUTBOX ||
        			mIpMessage.getStatus() == Sms.MESSAGE_TYPE_SENT)&& (mIpMessage.getStatus() == Sms.MESSAGE_TYPE_INBOX &&
                    ((IpAttachMessage)mIpMessage).getRcsStatus() == Status.FINISHED)){
        	// if (true || status == IFileTransfer.Status.FINISHED  || ipMessage.getStatus() == IpMessageConsts.IpMessageStatus.MO_SENDING) {
    	    	isStopThreadRunning = true;
    	    	hideprogressBar();
    	    	
    	    	setMessageContent();
    		} */
        }
    };
        @Override
        public void onBackPressed() {
            Log.d(TAG, " [BurnedMsg]:  onBackPressed() ");
            if ((mIpMsgType != IpMessageType.TEXT)&& mIpMessage != null 
                && ((IpAttachMessage)mIpMessage).getRcsStatus() != Status.FINISHED
                && mIpMessage.getStatus() == Sms.MESSAGE_TYPE_INBOX) {
                RCSMessageManager.getInstance(ContextCacher.getHostContext())
                    .pauseDownloadAttach(mIpMsgId,mThreadId);
            }
            super.onBackPressed();
        }
    
    @Override
    protected void onPause() {
        super.onPause();
        //Skiping if the activity is called by favorite or spam.
        if(mIsFavoritaSpamCall) {
            Log.d(TAG, "onPause mIsFavoritaSpamCall true, return");
            return;
        }

        Log.d(TAG, " [BurnedMsg]:  onPause() ");
        // ClearMsgId();
        isStopThreadRunning = false;
        Log.d(TAG, "[BurnedMsg]: isBurnedMsg = "+ isBurnedMsg + "  mIpMsgId = " + mIpMsgId );
        if( isBurnedMsg) {
            deleteBurnedMsg();
        }

        RCSServiceManager.getInstance().unregistNotifyListener(this);
        finish();
    }

    private void deleteBurnedMsg() {
        if (mIpMsgType == IpMessageType.TEXT) {

            // RCSMessageManager.getInstance(ContextCacher.getHostContext()).
            // sendDisplayedDeliveryReport(null,String.valueOf(ipMsgId));
            Log.d(TAG, "[BurnedMsg]: mIpMessage.getFrom() = "
                    + mIpMessage.getFrom() + " mIpMessage.getMessageId() = "
                    + mIpMessage.getMessageId());
            RCSMessageManager.getInstance(ContextCacher.getHostContext())
                    .sendBurnDeliveryReport(mIpMessage.getFrom(),
                            mIpMessage.getMessageId());
            ContextCacher.getHostContext().getContentResolver().delete(
                    Sms.CONTENT_URI, Sms.IPMSG_ID + " = " + mIpMsgId, null);
            removeMsgId(mIpMsgId);
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Log.d(TAG, " [BurnedMsg]:  onPause(), text run");
                        Thread.sleep(1000);
                        RCSMessageManager.getInstance(
                                ContextCacher.getHostContext())
                                .deleteStackMessage(mIpMsgId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } else {
            Log.d(TAG, "[BurnedMsg]: mIpMessage.getStatus() = "
                    + mIpMessage.getStatus() + "  getRcsStatus = "
                    + ((IpAttachMessage) mIpMessage).getRcsStatus());
            if (mIpMessage.getStatus() == Sms.MESSAGE_TYPE_INBOX
                    && ((IpAttachMessage) mIpMessage).getRcsStatus() == Status.FINISHED) {
                Log.d(TAG, "[BurnedMsg]: mIpMessage.getFrom() = "
                        + mIpMessage.getFrom() + " mIpMessage.getIpDbId() = "
                        + mIpMessage.getIpDbId());
                RCSMessageManager.getInstance(ContextCacher.getHostContext())
                        .sendBurnDeliveryReport(mIpMessage.getFrom(),
                                mIpMessage.getMessageId());
                ContextCacher.getHostContext().getContentResolver().delete(
                        Sms.CONTENT_URI, Sms.IPMSG_ID + " = " + mIpMsgId, null);
                removeMsgId(mIpMsgId);
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            Log.d(TAG, " [BurnedMsg]:  onPause(), ft run");
                            Thread.sleep(1000);
                            RCSMessageManager.getInstance(
                                    ContextCacher.getHostContext())
                                    .deleteStackMessage(mIpMsgId);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        }
    }
    
    private class DownloadFTAsyncTask extends AsyncTask<Void,Void,Void> {
        private Context mTaskContext;
        public DownloadFTAsyncTask(Context context) {
        	mTaskContext = context;
        }

        @Override
        protected Void doInBackground(Void... params) {
        	        	
//        	if (true || status == IFileTransfer.Status.FINISHED  || ipMessage.getStatus() == IpMessageConsts.IpMessageStatus.MO_SENDING) {
//    	    	isStopThreadRunning = true;
//    	    	// hideprogressBar();
//    	    	// progressLarge .setVisibility(ProgressBar.GONE);
//    	    	
//    	    	setMessageContent();
//    		} else 
        	Log.d(TAG, " [BurnedMsg]: doInBackground:  getStatus() = " + mIpMessage.getStatus());
    		if( (mIpMessage.getStatus() == Sms.MESSAGE_TYPE_INBOX &&
                    ((IpAttachMessage)mIpMessage).getRcsStatus() == IFileTransfer.Status.FAILED)){
    			Log.d(TAG, " [BurnedMsg]: ,Redownload");
    			
    			showprogressBar();
    			RCSMessageManager.getInstance(ContextCacher.getHostContext()).reDownloadAttach(mIpMsgId,mThreadId);
    		} else if( (mIpMessage.getStatus() == Sms.MESSAGE_TYPE_INBOX &&
                    ((IpAttachMessage)mIpMessage).getRcsStatus() == IFileTransfer.Status.WAITING)){
    			Log.d(TAG, " [BurnedMsg]: ,the first download");
    			
    			showprogressBar();
    			RCSMessageManager.getInstance(ContextCacher.getHostContext()).downloadAttach(mIpMsgId,mThreadId);
        	}
        	
        	return null;
        }
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
//        Toast.makeText(this,intent.getStringExtra("ipmsg_id"), Toast.LENGTH_SHORT);
//        //ipMsgTextContentShowText.setText(intent.getExtras().toString());
//        ipMsgTextContentShowText.setText("0000");

    }
    
    private void setMessageContent() {
        View showContent;
        View hideContent1;
        View hideContent2;
        switch (mIpMsgType) {
	        case IpMessageType.TEXT:
	        	IpTextMessage textMessage = (IpTextMessage) mIpMessage;
	        	ipMsgTextContentShowText.setText(textMessage.getBody());
	        	showContent = ipMsgTextContentShowText;
	        	hideContent1 = ipMsgImageContentShowText;
	        	hideContent2 = ipMsgVideoContentShowText;
	        	break;
	        case IpMessageType.PICTURE:
	            IpImageMessage imageMessage = (IpImageMessage) mIpMessage;
                try {
                    Log.d(TAG, " Test filePath 4" + imageMessage.getPath());
                    showPictureThumbnail(imageMessage.getPath());

                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
	        	showContent = ipMsgImageContentShowText;
	        	hideContent1 = ipMsgTextContentShowText;
	        	hideContent2 = ipMsgVideoContentShowText;
	        	break;
	        case IpMessageType.VIDEO:
	        	IpVideoMessage videoMessage = (IpVideoMessage) mIpMessage;
	        	showVideoThumbnail(videoMessage.getPath());
	        	ipMsgVideoBtn.setVisibility(View.VISIBLE);
	        	showContent = ipMsgVideoContentShowText;
	        	hideContent1 = ipMsgTextContentShowText;
	        	hideContent2 = ipMsgImageContentShowText;
	        	break;
	        default:
	            return;
        }
        if (showContent != null) {
        	showContent.setVisibility(View.VISIBLE);
        }
        if (hideContent1 != null) {
        	hideContent1.setVisibility(View.GONE);
        }
        if (hideContent2 != null) {
        	hideContent2.setVisibility(View.GONE);
        }
    }

    /*
     * Na(mtk81368) 2014/12/28
     * this method is added to open favorite or spam message when click the list item;
     * param path: ip message file path
     */
    private void showFavoritaSpamMsg(String path, int type) {
        Log.d(TAG, " setFavoritaSpamCallMsg path = " + path + " type = " + type);
        if (type == -1) {
            Log.d(TAG, "showFavoritaSpamMsg type is -1 error, return");
            return;
        }
        progressLarge.setVisibility(View.GONE);
        ipMsgTextContentShowText.setVisibility(View.GONE);
        ipMsgImageContentShowText.setVisibility(View.GONE);
        ipMsgVideoContentShowText.setVisibility(View.GONE);

        switch (type) {
        case IpMessageType.TEXT:
            ipMsgTextContentShowText.setVisibility(View.VISIBLE);
            EmojiImpl emojiImpl = EmojiImpl.getInstance(mContext);
            CharSequence body = emojiImpl.getEmojiExpression(path, true);
            ipMsgTextContentShowText.setText(body);
            break;

        case IpMessageType.PICTURE:
            ipMsgImageContentShowText.setVisibility(View.VISIBLE);
            try {
                File file = new File(path);
                InputStream in = new FileInputStream(file);
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                ipMsgImageContentShowText.setImageBitmap(bitmap);
            } catch (NullPointerException e) {
                Log.d(TAG, "showFavoritaSpamMsg show PICTURE exception");
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "showFavoritaSpamMsg show PICTURE exception");
                e.printStackTrace();
            }
            break;

        case IpMessageType.VIDEO:
            ipMsgVideoContentShowText.setVisibility(View.VISIBLE);
            showVideoThumbnail(path);
            break;

        default:
            return;
        }
    }

    private OnClickListener mShowListener = new OnClickListener() {
        public void onClick(View v) {
        	// File file = new File("/storage/sdcard0/VID_20141120_163315.3gp");
        	Uri mVideoUri = Uri.parse(((IpAttachMessage)mIpMessage).getPath());
        	Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra("SingleItemOnly", true);
            intent.putExtra("CanShare", false);
        	try {
	        	if (mIpMsgType == IpMessageType.VIDEO) {
		            //intent.putExtra("title", "VID_20141120_163315");
		            
		            // intent.putExtra(EXTRA_FULLSCREEN_NOTIFICATION, true);
		            // mType = MessageUtils.getContentType(mType, mTitle);
		            intent.setDataAndType(mVideoUri, "video/*");
		            mContext.startActivity(intent);
	        	}
            } catch (ActivityNotFoundException e) {
                Intent mchooserIntent = Intent.createChooser(intent, null);
                mContext.startActivity(mchooserIntent);
            }
        }
    };

    
    @Override
    public void notificationsReceived(Intent intent) {

    	// ipMsgVideoContentShowText.
    	
    	if(intent.getAction() == IpMessageConsts.IpMessageStatus.ACTION_MESSAGE_STATUS){
    		mIpStatus = (Status)intent.getExtras().get(IpMessageConsts.STATUS);
    		Log.d(TAG,"  [BurnedMsg]: notificationsReceived() mIpStatus = "+mIpStatus);
    		//((IpAttachMessage)ipMessage).getRcsStatus() == Status.FINISHED)
    		this.runOnUiThread( new Runnable() {
    	        @Override
    	        public void run() {
		    		if (mIpStatus == Status.FINISHED) {
		    	    	isStopThreadRunning = true;
	    	        	hideprogressBar();
	        	    	mIpMessage = RCSMessageManager.getInstance(mContext).getIpMsgInfo(mThreadId,mIpMsgId);
	        	    	Log.d(TAG,"  [BurnedMsg]: mIpMessage = "+mIpMessage);
                        Log.d(TAG, "Test filepath 3  = " + ((IpAttachMessage)mIpMessage).getPath());
	        	    	setMessageContent();
	        	    	if(isBurnedMsg) {
	        	    		saveMsgId(mIpMsgId);
	        	    	}
	        	    	
		    		} else if (mIpStatus == Status.FAILED) {
		    	    	isStopThreadRunning = true;
		    	    	hideprogressBar();
		    	        Toast.makeText(mContext, R.string.download_file_fail,
		    	                Toast.LENGTH_SHORT).show();
		    	        finishActivity();
		    		}
    	        }
	    	});
    		
//    		if (mIpStatus == Status.FAILED) {
//    			Log.d(TAG,"  [BurnedMsg]: notificationsReceived() Toast  ");
//    	        Toast.makeText(mContext, R.string.download_file_fail,
//    	                Toast.LENGTH_SHORT).show();
//    	    	this.finish();
//    		}
    	}
    }

    private void saveMsgId( long msgId) {
    	Log.d(TAG, "[BurnedMsg]: saveMsgId()");
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
        prefs.commit();
        Log.d(TAG, "[BurnedMsg]: save success burnedMsgList = "+burnedMsgList);
    }
    
    private void removeMsgId( long msgId) {
    	Log.d(TAG, "[BurnedMsg]: removeMsgId()");
    	SharedPreferences sp = ContextCacher.getPluginContext().getSharedPreferences(IpMessageConsts.BurnedMsgStoreSP.PREFS_NAME, Context.MODE_WORLD_READABLE);
    	Set<String> burnedMsgList = sp.getStringSet(IpMessageConsts.BurnedMsgStoreSP.PREF_PREFIX_KEY, null);
    	if (burnedMsgList == null) {
    		Log.d(TAG, "[BurnedMsg]: burnedMsgList is null");
    		return;
    	}
    	burnedMsgList = new HashSet<String>(burnedMsgList);
    	Log.d(TAG, "[BurnedMsg]: removeMsgId burnedMsgList = "+burnedMsgList);
    	for (String id : burnedMsgList) {
    		if ( Long.valueOf(id) == msgId) {
    			burnedMsgList.remove(String.valueOf(msgId));
    			break;
    		}
        }
    	burnedMsgList = new HashSet<String>(burnedMsgList);
    	// Set<String> burnedMsgList = new HashSet<String>();
    	SharedPreferences.Editor prefs = sp.edit();
        prefs.putStringSet(IpMessageConsts.BurnedMsgStoreSP.PREF_PREFIX_KEY , burnedMsgList);
        prefs.apply();
        Log.d(TAG, "[BurnedMsg]: remove success burnedMsgList = "+burnedMsgList);
    }
    
    private void ClearMsgId() {
    	Log.d(TAG, "[BurnedMsg]: ClearMsgId()");
    	SharedPreferences.Editor prefs = sp.edit();
        prefs.clear();
        prefs.commit();
    }

    private void showPictureThumbnail(String path) {
        Log.d(TAG, "[BurnedMsg]: showPictureThumbnail() path = "+path);
        try {
            File file = new File(path);
            InputStream in = new FileInputStream(file);
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            Log.d(TAG, " Test filePath 5 , bitmap = " + bitmap);
            ipMsgImageContentShowText.setImageBitmap(bitmap);
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    private void showVideoThumbnail(String path) {
    	Log.d(TAG, "[BurnedMsg]: showVideoThumbnail() path = "+path);
    	// Uri mVideoUri = Uri.fromFile(new File("/storage/sdcard0/VID_20141120_163315.3gp"));
    	// Uri mVideoUri = Uri.parse("file:///storage/sdcard0/VID_20141120_163315.3gp");
    	Uri mVideoUri = Uri.parse(path);
        Bitmap t = getThumbnailFromVideoUri(mVideoUri, mContext,
        		500, 500);

        ipMsgVideoContentShowText.setImageBitmap(t);
        //ipMsgVideoContentShowText.setImageResource(R.drawable.ipmsg_message_box_mint);
        ipMsgVideoContentShowText.setVisibility(View.VISIBLE);
    }

    private Bitmap getThumbnailFromVideoUri(Uri VideoUri, Context context, int width, int height) {
    	Log.d(TAG, "[BurnedMsg]: getThumbnailFromVideoUri()");
        if (VideoUri == null) {
            return null;
        }
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        Bitmap raw = null;
        try {
            try {
                retriever.setDataSource(context, VideoUri);
                raw = retriever.getFrameAtTime(-1);
            } finally {
                retriever.release();
            }
        } catch (IllegalArgumentException e) {
            // corrupted video
        } catch (RuntimeException e) {
            // corrupted video
        }
        Bitmap thumb;
        if (raw != null) {
            thumb = Bitmap.createScaledBitmap(raw, width, height, true);
            if (thumb != raw) {
                raw.recycle();
            }
        } else {
        	
            if (sThumbDefaultVideo == null) {
                sThumbDefaultVideo = BitmapFactory.decodeResource(context.getResources(), R.drawable.ipmsg_service);
            }
            thumb = sThumbDefaultVideo;
        }
        return thumb;
    }
    
    private void showprogressBar() {
    	Log.d(TAG," [BurnedMsg]: showprogressBar");
	    //setProgress(progressLarge.getProgress() * 100);	    
//	    progressHandler = new DownloadMsgHandler() {
//           @Override
//           public void handleMessage(Message msg) {
//        	   progressLarge.setProgress(msg.what * 100);
//        	   Log.d("progressbarVaule","receive i = "+msg.what);
//                if (msg.what == 100) {
//                	isStopThreadRunning = true;
//                	hideprogressBar();
//                }
//           }
//        };
        if (null != progressLarge) {
        	progressLarge.setVisibility(View.VISIBLE);
        }
    	
//    	isStopThreadRunning = false;
//        pt = new ProgressThread();
//        pt.start();
        
    }

    private class ProgressThread extends Thread {

        public ProgressThread() {
        }
        
        @Override
        public void run() {
        	Log.d(TAG," [BurnedMsg]: ProgressThread run()");
        	 Looper.prepare();
             if (null != Looper.myLooper()) {
            	 mDownloadMsgHandler = new DownloadMsgHandler(Looper.myLooper());
             }
           int i = 0;
           while (i <= 100 && !isStopThreadRunning) {
        	   mDownloadMsgHandler.sendEmptyMessage(i);
                Log.d(TAG,"send i = "+i);
                ++i;
                try {
                	sleep(1000);
                } catch (InterruptedException e) {
                	e.printStackTrace();
                }
             }
           Looper.loop();
        }
    }
    
    private class DownloadMsgHandler extends Handler {
        public DownloadMsgHandler(Looper looper) {
            super(looper);
        }
        
        @Override
        public void handleMessage(Message msg) {
        	Log.d(TAG," [BurnedMsg]: handleMessage()");
     	   progressLarge.setProgress(msg.what * 100);
    	   Log.d(TAG," [BurnedMsg]: receive i = "+msg.what);
            if (msg.what == 100) {
            	isStopThreadRunning = true;
            	hideprogressBar();
            }
        }
    }
    
    private void hideprogressBar() {
    	Log.d(TAG," [BurnedMsg]: hideprogressBar");
    	if (progressLarge != null) {
    		progressLarge.setVisibility(ProgressBar.GONE);
    	}
    	
    }
    private void finishActivity() {
    	Log.d(TAG," [BurnedMsg]: finishActivity()");
    	this.finish();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG," [BurnedMsg]: onStop()");
        isStopThreadRunning = false;
    }

    
    
    private void setIpMsgTextContent() {
        final Uri contentUri = ContentUris.withAppendedId(
        		Uri.parse("content://org.gsma.joyn.provider.chat/message"),
                Long.valueOf(position));
        /*
        //String[] projection = SMS_PROJECTION;
        Cursor cursor = null;
        Cursor cursor = SqliteWrapper.query(mContext, mContext.getContentResolver(),
        		searchUri, null, null, null, null);
        if (cursor == null) {
            Log.w(TAG, "null Cursor ");
            return;
        }
        String text = cursor.getString(cursor.getColumnIndexOrThrow(message.body));
        ipMsgTextContentShowText.setText(text);
        */
        
        Cursor cursor = null;
        final String column = "body";
        final String[] projection = { column };

        try {
            cursor = mContext.getContentResolver().query(contentUri, projection,
            		null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int columnIndex = cursor.getColumnIndexOrThrow(column);
                String text = cursor.getString(columnIndex);
                ipMsgTextContentShowText.setText(text);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return ;
    }
    
    private void setIpMsgMediaContent() {
        final Uri contentUri = ContentUris.withAppendedId(
        		Uri.parse("content://org.gsma.joyn.provider.ft/ft"),Long.valueOf(position));
       
        Cursor cursor = null;
        final String column = "filename";
        final String[] projection = { column };
        try {
            cursor = mContext.getContentResolver().query(contentUri, projection,
            		null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int columnIndex = cursor.getColumnIndexOrThrow(column);
                String filename = cursor.getString(columnIndex);
//                ipMsgVideoContentShowText.setImageBitmap(filename);
                
                try {
                    File file = new File(filename);  
                    InputStream in = new FileInputStream(file);
                    Bitmap bitmap = BitmapFactory.decodeStream(in);
                    ipMsgVideoContentShowText.setImageBitmap(bitmap);
                } catch (NullPointerException e) {
                    return;
                } catch (FileNotFoundException e) {
                    return;
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return;
    }
    
    
    private void saveMsgIdTemp( long msgId) {
    	Log.d(TAG, "[BurnedMsg]: saveIpMsgId()");
    	SharedPreferences.Editor prefs = sp.edit();
        prefs.putLong(IpMessageConsts.BurnedMsgStoreSP.PREF_PREFIX_KEY , msgId);
        prefs.apply();
    }
    
    private void updateMsgId() {
    	Log.d(TAG, "[BurnedMsg]: updateMsgId()");
    	long msgIdValue = sp.getLong(IpMessageConsts.BurnedMsgStoreSP.PREF_PREFIX_KEY, 0);
		Log.d(TAG, " [BurnedMsg]: mIpMsgId = "+ mIpMsgId + "  msgIdValue = " + msgIdValue );
    	if (msgIdValue != 0){
    		ClearMsgId();
    	}
    	if (mIpMsgId != msgIdValue){
    		saveMsgId(mIpMsgId);
    	}
    }
    
    
}
