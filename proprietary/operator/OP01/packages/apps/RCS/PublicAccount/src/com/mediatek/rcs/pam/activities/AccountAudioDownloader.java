package com.mediatek.rcs.pam.activities;

import android.util.Log;
import android.util.LruCache;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.model.ResultCode;

import java.io.File;
import java.util.ArrayList;

public class AccountAudioDownloader implements IAudioDownloader {

    private static final String TAG = Constants.TAG_PREFIX + "AccountAudioDownloader";
    private static final int SIZE_PATH_CACHE = 50;

    private ClientQueryActivity.Downloader mDownloader;
    private LruCache<String, String> mPathCache;

    private ArrayList<String> mLinkList;
    private ArrayList<IAudioDownloadListener> mListenerList;

    public AccountAudioDownloader(ClientQueryActivity.Downloader downloader) {
        mDownloader = downloader;
        mPathCache = new LruCache<String, String>(SIZE_PATH_CACHE);
        mLinkList = new ArrayList<String>();
        mListenerList = new ArrayList<IAudioDownloadListener>();
    }

    @Override
    public void downloadAudio(IAudioDownloadListener listener, final String link) {

        String filePath = mPathCache.get(link);
        if (filePath != null) {
            File file = new File(filePath);
            if (file.exists()) {
                Log.i(TAG, "[audio] in cache " + filePath);
                listener.downloadComplete(link, filePath);
            } else {
                Log.i(TAG, "[audio] error in cache, remove " + filePath);
                mPathCache.remove(link);
            }
        }

        if (mLinkList.contains(link)) {
            Log.i(TAG, "Already is downloading, do nothing");
        } else {
            mLinkList.add(link);
            mDownloader.downloadObject(link, Constants.MEDIA_TYPE_AUDIO,
                    new ClientQueryActivity.DownloadListener() {

                        @Override
                        public void reportDownloadResult(int resultCode, String path,
                                long mediaId) {
                            Log.i(TAG, "result code is " + resultCode + ", path is " + path);
                            mLinkList.remove(link);

                            if (resultCode == ResultCode.SUCCESS) {
                                mPathCache.put(link, path);
                                notifyDownloadComplete(link, path);
                            }
                        }

                        @Override
                        public void reportDownloadProgress(long requestId, int percentage) {
                            Log.i(TAG, "requestId " + requestId + " percentage is " + percentage);
                        }
            });
        }
    }

    private void notifyDownloadComplete(String link, String filePath) {
        Log.i(TAG, "Download finish, notify all listeners");
        for (IAudioDownloadListener listener: mListenerList) {
            listener.downloadComplete(link, filePath);
        }
    }

    @Override
    public void addListener(IAudioDownloadListener listener) {
        mListenerList.add(listener);
    }

    @Override
    public void removeListener(IAudioDownloadListener listener) {
        mListenerList.remove(listener);
    }
}

interface IAudioDownloadListener {
    void downloadComplete(String link, String filePath);
}

interface IAudioDownloader {
    void downloadAudio(IAudioDownloadListener listener, String link);

    void addListener(IAudioDownloadListener listener);

    void removeListener(IAudioDownloadListener listener);
}
