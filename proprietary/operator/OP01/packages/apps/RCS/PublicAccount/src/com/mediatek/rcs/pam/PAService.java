package com.mediatek.rcs.pam;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import org.gsma.joyn.JoynService;

/**
 * This is the wrapper of public account service for client.
 */
public class PAService {
    private static final String TAG = Constants.TAG_PREFIX + "PAService";
    private final Context mContext;
    private IPAService mServiceImpl = null;
    private IPAServiceCallback mServiceCallback;
    private ServiceConnection mServiceConnection;
    private long mToken = Constants.INVALID;

    public PAService(Context context, IPAServiceCallback callback) {
        mContext = context;
        mServiceCallback = callback;

        mServiceConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "onServiceConnected");
                mServiceImpl = IPAService.Stub.asInterface(service);
                try {
                    mToken = mServiceImpl.registerCallback(mServiceCallback);
                    Log.d(TAG, "PAServiceImpl connected with token: " + mToken + ", waiting for RCS connection.");
                    // mServiceCallback.onServiceConnected();
                } catch (RemoteException e) {
                    throw new Error(e);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "onServiceDisconnected");
                mServiceImpl = null;
                try {
                    mServiceCallback.onServiceDisconnected(JoynService.Error.INTERNAL_ERROR);
                } catch (RemoteException e) {
                    throw new Error(e);
                }
            }
        };
    }

    public void connect() {
        Log.d(TAG, "connect");
        Intent intent = new Intent();
        intent.setClass(mContext, PAServiceImpl.class);
        mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void disconnect() {
        Log.d(TAG, "disconnect");
        try {
            mServiceImpl.unregisterCallback(mToken);
        } catch (RemoteException e) {
            throw new Error(e);
        }
        mContext.unbindService(mServiceConnection);
        mServiceImpl = null;
    }

    // TODO use gsma termial api exception instead
    public boolean isServiceConnected() {
        try {
            return mServiceImpl != null && mServiceImpl.isServiceConnected();
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    private void checkConnectStatus() {
        if (mToken == Constants.INVALID) {
            throw new Error("Invalid token");
        }
        try {
            if (mServiceImpl == null || !mServiceImpl.isServiceConnected()) {
                throw new Error("Service not connected");
            }
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    // TODO use GSMA terminal api exception instead
    public boolean isServiceRegistered() {
        if (!isServiceConnected()) {
            throw new Error("Service is not connected.");
        }
        try {
            return mServiceImpl.isServiceRegistered();
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    /**
     * Send message to public account specified by accountId.
     * 
     * @param accountId
     *            Account ID in provider. Cannot be Constants.INVALID.
     * @param message
     *            Message to sent. Cannot be null.
     * @param system
     *            Whether this message is system message. System messages cannot
     *            be queried from provider unless you add a special query
     *            parameter.
     * @return The message being sent.
     */
    public long sendMessage(long accountId, String message, boolean system) {
        checkConnectStatus();
        if (accountId == Constants.INVALID || message == null) {
            throw new Error("Invalid parameter");
        }
        try {
            return mServiceImpl.sendMessage(mToken, accountId, message, system);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    /**
     * Resend a failed message.
     * 
     * @param messageId
     *            the message ID to resend
     */
    public void resendMessage(long messageId) {
        checkConnectStatus();
        try {
            mServiceImpl.resendMessage(mToken, messageId);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public long sendImage(long accountId, String path, String thumbnailPath) {
        checkConnectStatus();
        try {
            return mServiceImpl.sendImage(mToken, accountId, path, thumbnailPath);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public long sendAudio(long accountId, String path, int duration) {
        checkConnectStatus();
        try {
            return mServiceImpl.sendAudio(mToken, accountId, path, duration);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public long sendVideo(long accountId, String path, String thumbnailPath, int duration) {
        checkConnectStatus();
        try {
            return mServiceImpl.sendVideo(mToken, accountId, path, thumbnailPath, duration);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public long sendGeoLoc(long accountId, String data) {
        checkConnectStatus();
        try {
            return mServiceImpl.sendGeoLoc(mToken, accountId, data);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }
    
    public long sendVcard(long accountId, String data) {
        checkConnectStatus();
        try {
            return mServiceImpl.sendVcard(mToken, accountId, data);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public void complainSpamMessage(long messageId) {
        checkConnectStatus();
        try {
            mServiceImpl.complainSpamMessage(mToken, messageId);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }
    
    public boolean deleteMessage(long messageId) {
        checkConnectStatus();
        try {
            return mServiceImpl.deleteMessage(mToken, messageId);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }        
    }
    
    public long deleteMessageByAccount(long accountId) {
        checkConnectStatus();
        try {
            return mServiceImpl.deleteMessageByAccount(mToken, accountId);
        } catch (RemoteException e) {
            throw new Error(e);
        }        
    }

    public long subscribe(String id) {
        checkConnectStatus();
        try {
            return mServiceImpl.subscribe(mToken, id);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public long unsubscribe(String id) {
        checkConnectStatus();
        try {
            return mServiceImpl.unsubscribe(mToken, id);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public long getSubscribedList(int order, int pageSize, int pageNumber) {
        checkConnectStatus();
        try {
            return mServiceImpl.getSubscribedList(mToken, order, pageSize, pageNumber);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public long getDetails(String uuid, String timestamp) {
        checkConnectStatus();
        try {
            return mServiceImpl.getDetails(mToken, uuid, timestamp);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public long getMenu(String uuid, String timestamp) {
        checkConnectStatus();
        try {
            return mServiceImpl.getMenu(mToken, uuid, timestamp);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public long downloadObject(String url, int type) {
        checkConnectStatus();
        Log.d(TAG, "downloadObject(" + url + ", " + type + ")");
        try {
            return mServiceImpl.downloadObject(mToken, url, type);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public void cancelDownload(long requestId) {
        checkConnectStatus();
        try {
            mServiceImpl.cancelDownload(requestId);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public long setAcceptStatus(String uuid, int acceptStatus) {
        checkConnectStatus();
        try {
            return mServiceImpl.setAcceptStatus(mToken, uuid, acceptStatus);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }
}
