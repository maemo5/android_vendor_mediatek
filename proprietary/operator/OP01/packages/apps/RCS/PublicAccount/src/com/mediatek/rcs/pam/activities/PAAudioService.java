package com.mediatek.rcs.pam.activities;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnTimedTextListener;
import android.media.TimedText;
import android.util.Log;

public class PAAudioService {
	
	private static final String TAG = "PA/PAAudioPlayer";
	
	public static final int PLAYER_STATE_ERROR 	 = -1;
	//Media player not create
	public static final int PLAYER_STATE_UNKOWN  = 0;
	//Media player create() or reset().
	public static final int PLAYER_STATE_IDLE 	 = 1;
	//Media player setDataSource()&prepare() or play complete 
	public static final int PLAYER_STATE_READY	 = 2;
	//Media player start()
	public static final int PLAYER_STATE_PLAYING = 3;
	//Media player pause()
	public static final int PLAYER_STATE_PAUSE	 = 4;
	//Media Player onError()
	
	private static final int INVALID_ID = -1;
	
	private static PAAudioService mAudioService = null;
	private static MediaPlayer mMediaPlayer;
	private static int mPlayerState = PLAYER_STATE_UNKOWN;
	
	//Passed by caller
	private static int mId = INVALID_ID;
	private static String mAudioPath;	
	private static IAudioServiceCallBack mCallback;
	
	ReentrantReadWriteLock lock;
	
	
	interface IAudioServiceCallBack {
		
		void onError(int what, int extra);
		
		void onCompletion();

	}
	
	class ErrorLisener implements OnErrorListener {
		
		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			stateChange(PLAYER_STATE_ERROR);
			
			if (mCallback != null) {
				mCallback.onError(what, extra);
			}
			//cause media player send onCompletion callback.
			return false;
		}		
	}

	class CompletionListener implements OnCompletionListener {

		@Override
		public void onCompletion(MediaPlayer mp) {
				stateChange(PLAYER_STATE_READY);
			
				if (mCallback != null) {
					mCallback.onCompletion();
				}
		}
	}
	

	private PAAudioService() {
		
		mMediaPlayer = new MediaPlayer();
		
		mMediaPlayer.setOnErrorListener(new ErrorLisener());
		
		mMediaPlayer.setOnCompletionListener(new CompletionListener());
		
	}
	
	public static PAAudioService getService() {		
		Log.e(TAG, "getService()");
		if (mAudioService == null) {
			stateChange(PLAYER_STATE_IDLE);
			mAudioService = new PAAudioService();
		}
		return mAudioService;		
	}
	
	public void resetService() {
		Log.e(TAG, "reset()");
		mMediaPlayer.reset();
		stateChange(PLAYER_STATE_IDLE);
		mId = INVALID_ID;
		mAudioPath = null;
	}
	
	public void releaseService() {
		Log.e(TAG, "release()");
		mMediaPlayer.release();
		mMediaPlayer = null;
		mId = INVALID_ID;
		mAudioPath = null;
		mAudioService = null;
		stateChange(PLAYER_STATE_UNKOWN);
	}
	
	public boolean bindAudio(int id, String path, IAudioServiceCallBack callback) {
		Log.d(TAG, "bindAudioFile(). id=" + id + ". path=" + path + ". state=" + mPlayerState);
		
		if (path == null || path.isEmpty()) {
			Log.d(TAG, "bindAudioFile() failed for path is null.");
			return false;
		}
		
		if (mId != INVALID_ID && mPlayerState > PLAYER_STATE_IDLE) {
			if (mId != id || !path.equals(mAudioPath)) {
				stopAudio();				
				resetService();
			}
		}		
		
		File file = new File(path);
		if (!file.exists()) {
			Log.d(TAG, "bindAudioFile() fail for path not exist.");
			return false;
		}		
	
		switch (mPlayerState) {
		case PLAYER_STATE_IDLE:			
			try {
				mMediaPlayer.setDataSource(path);
				mMediaPlayer.prepare();
				stateChange(PLAYER_STATE_READY);
		
				mId = id;
				mAudioPath = path;
				mCallback = callback;				
			} catch (IllegalArgumentException | SecurityException | 
					IllegalStateException | IOException e) {
				e.printStackTrace();
				//prepare audio fail, reset Service
				resetService();
				return false;
			}
			break;
		
		case PLAYER_STATE_READY:
		case PLAYER_STATE_PLAYING:
		case PLAYER_STATE_PAUSE:
			mCallback = callback;
			Log.d(TAG, "bindAudioFile() already bind, return directly");
			return true;
		
		
		case PLAYER_STATE_ERROR:
		case PLAYER_STATE_UNKOWN:
		default:
			Log.d(TAG, "bindAudioFile() in error state.");
			return false;
		}
		return true;
	}
	
	public boolean unBindAudio(int id) {
		Log.d(TAG, "unBindAudio(). mId=" + id);
		if (mId == id) {
			mCallback = null;
			return true;
		}
		Log.d(TAG, "unBindAudio() fail. current mId =" + mId);
		return false;
		
	}
	
	public boolean playAudio() {
		Log.d(TAG, "PlayAudio()  current state = " + mPlayerState);
		
		switch (mPlayerState) {

		case PLAYER_STATE_READY:
		case PLAYER_STATE_PAUSE:
			try {
				mMediaPlayer.start();
				stateChange(PLAYER_STATE_PLAYING);
			} catch (IllegalStateException  e) {
				e.printStackTrace();
				return false;
			}			
			break;
		
		case PLAYER_STATE_IDLE:
		case PLAYER_STATE_UNKOWN:
		case PLAYER_STATE_PLAYING:
		case PLAYER_STATE_ERROR:
		default:
			Log.e(TAG, "PlayAudio() but in error state");
			return false;
		}		
		return true;
	}
	
	public boolean pauseAudio() {
		Log.d(TAG, "pauseAudio()  current state = " + mPlayerState);
		
		switch (mPlayerState) {

		case PLAYER_STATE_PLAYING:
			try {
				mMediaPlayer.pause();
				stateChange(PLAYER_STATE_PAUSE);
			} catch (IllegalStateException  e) {
				e.printStackTrace();
				return false;
			}			
			break;
			
		case PLAYER_STATE_UNKOWN:		
		case PLAYER_STATE_IDLE:
		case PLAYER_STATE_READY:
		case PLAYER_STATE_PAUSE:
		case PLAYER_STATE_ERROR:
		default:
			Log.e(TAG, "PlayAudio() but in error state");
			return false;
		}		
		return true;		
	}
	
	public boolean stopAudio() {
		Log.d(TAG, "pauseAudio()  current state = " + mPlayerState);
		
		switch (mPlayerState) {

		case PLAYER_STATE_READY:
		case PLAYER_STATE_PLAYING:
		case PLAYER_STATE_PAUSE:
			try {
				mMediaPlayer.stop();
				if (mCallback != null) {
					mCallback.onCompletion();
				}
				resetService();
			} catch (IllegalStateException  e) {
				e.printStackTrace();
				return false;
			}			
			break;
			
		case PLAYER_STATE_UNKOWN:		
		case PLAYER_STATE_IDLE:
		case PLAYER_STATE_ERROR:
		default:
			Log.e(TAG, "stopAudio() but in error state");
			return false;
		}		
		return true;	
	}
	
	public int getServiceStatus() {
		return mPlayerState;
	}
	
	public int getDuration() {
		
		if (mPlayerState > PLAYER_STATE_READY) {
			return mMediaPlayer.getDuration();
		}		
		return -1;
	}
	
	public int getCurrentTime() {
		if (mPlayerState > PLAYER_STATE_READY) {
			
			return mMediaPlayer.getCurrentPosition();
		}
		return 0;
	}

	public static int getCurrentId() {
		return mId;
	}
	
	public static String getCurrentAudioPath() {
		return mAudioPath;
	}
	
	public static int getCurrentState() {
		return mPlayerState;
	}
	
	private static boolean stateChange(int newState) {
		
		if (mPlayerState != newState) {
			Log.d(TAG, "stateChange from " + mPlayerState + "->" + newState);
			mPlayerState = newState;
			return true;
		} else {
			Log.d(TAG, "stateChange no change:" + mPlayerState);
			return false;
		}
	}
}
