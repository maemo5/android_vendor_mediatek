package com.mediatek.rcs.message.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.mediatek.rcs.common.provider.ThreadMapCache;
import com.mediatek.rcs.common.provider.ThreadMapCache.MapInfo;


public class ProxyActivity extends Activity {
    private static final String TAG = "ProxyActivity";
    private final String ACTION_START_GROUP = "com.mediatek.rcs.groupchat.START";
    private final String KEY_CHAT_ID = "chat_id";
    private Handler mHandler;

    @Override
    public void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        initialize(savedInstanceState, getIntent());
    }
    
    private void initialize(Bundle saveBundle, Intent intent) {
        String action = intent.getAction();
        Log.e(TAG, "initialize action: " +action);
        if (action != null) {
            if (action.equals(ACTION_START_GROUP)) {
                String chatId = intent.getStringExtra(KEY_CHAT_ID);
                Log.e(TAG, "initialize chatId: " +chatId);
                if (chatId != null) {
                    startGroupChat(chatId);
                } else {
                    finish();
                }
            }
        } else {
            Log.e(TAG, "unkown action: " +action);
            finish();
        }
    }
    
    private void startGroupChat(final String chatId) {
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                // TODO Auto-generated method stub
                ThreadMapCache mapCache = null;
                while(mapCache == null) {
                    mapCache = ThreadMapCache.getInstance();
                    if (mapCache != null) {
                        final MapInfo info = mapCache.getInfoByChatId(chatId);
                        if (info != null) {
                            mHandler.post(new Runnable() {
                                
                                @Override
                                public void run() {
                                    // TODO Auto-generated method stub
                                    openCreatedGroup(info.getThreadId(), chatId);
                                }
                            });
                            
                        }
                    }
                }
            }
        }, "startGroupChat").start();
    }

    private void openCreatedGroup(long threadId, String chatId) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setType("vnd.android-dir/mms-sms");
//        if (threadId < 0) {
//            threadId = -threadId;
//        }
        intent.putExtra("thread_id", threadId);
        intent.putExtra("chat_id", chatId);
        intent.setPackage("com.android.mms");
        startActivity(intent);
        finish();
    }
}
