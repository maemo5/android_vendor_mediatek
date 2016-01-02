package com.mediatek.rcs.pam;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.rcs.pam.client.PAMClient;
import com.mediatek.rcs.pam.model.MediaArticle;
import com.mediatek.rcs.pam.model.MediaBasic;
import com.mediatek.rcs.pam.model.MenuInfo;
import com.mediatek.rcs.pam.model.MessageContent;
import com.mediatek.rcs.pam.model.PublicAccount;
import com.mediatek.rcs.pam.model.ResultCode;
import com.mediatek.rcs.pam.provider.PAContract;
import com.mediatek.rcs.pam.provider.PAContract.AccountColumns;
import com.mediatek.rcs.pam.provider.PAContract.MediaArticleColumns;
import com.mediatek.rcs.pam.provider.PAContract.MediaBasicColumns;
import com.mediatek.rcs.pam.provider.PAContract.MediaColumns;
import com.mediatek.rcs.pam.provider.PAContract.MessageColumns;
import com.mediatek.rcs.pam.provider.PAContract.StateColumns;
import com.mediatek.rcs.pam.provider.PAProvider;
import com.mediatek.rcs.pam.provider.RcseProviderContract;
import com.mediatek.rcs.pam.util.Pair;

import org.gsma.joyn.JoynContactFormatException;
import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.JoynServiceListener;
import org.gsma.joyn.JoynServiceRegistrationListener;
import org.gsma.joyn.chat.ChatListener;
import org.gsma.joyn.chat.ChatMessage;
import org.gsma.joyn.chat.ChatService;
import org.gsma.joyn.chat.GeolocMessage;
import org.gsma.joyn.chat.NewChatListener;
import org.gsma.joyn.chat.PublicAccountChat;
import org.gsma.joyn.chat.SpamReportListener;
import org.gsma.joyn.ft.FileSpamReportListener;
import org.gsma.joyn.ft.FileTransfer;
import org.gsma.joyn.ft.FileTransferListener;
import org.gsma.joyn.ft.FileTransferService;
import org.gsma.joyn.ft.NewFileTransferListener;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PAServiceImpl extends Service {
    public static final String ACTION_RESTART = "com.mediatek.pam.RESTART";
    
    public static final String ACTION_KEY_ACCOUNT = "contact";
    public static final String ACTION_KEY_CHAT_MESSAGE = "firstMessage";
    public static final String ACTION_KEY_FT_ID = "transferId";
    
    private static final String TAG = "PAM/PAServiceImpl";
    private static long sUuidCounter = 0;
    private static long sTokenCounter = 0;
    private boolean mIsGettingSubscribedList = false;
    
    private static final long SERVICE_TOKEN = -1000;
    
    // FIXME use value from CMCC's response
    private static final String SPAM_MESSAGE_REPORT_RECEIVER = "";
    
    private PAMClient mClient;
    private ExecutorService mExecutorService;
    private ConcurrentHashMap<Long, CancellableTask> mWorkingTasks;
    private Handler mHandler;
    
    private ChatService mRcseChatService;
    private FileTransferService mRcseFTService;
    
    private ConcurrentHashMap<Long, IPAServiceCallback> mClientListeners;
    private Map<String, PublicAccountChat> mChats;
    private Set<String> mPendingSystemMessages = new HashSet<String>();
    private Map<String, MessageRecoverRecord> mMessageRecoverRecords;

    private static class MessageRecoverRecord {
        public static final long RECOVER_TIMEOUT = 45 * 1000; // 45s
        public static final long RECOVER_INTERVAL = 5 * 1000; // 5s
        
        public long messageId;
        public long startTimestamp;
        public long clientToken;
        
        public MessageRecoverRecord(long id, long token) {
            messageId = id;
            startTimestamp = Constants.INVALID;
            clientToken = token;
        }
    }
    
    private static synchronized long generateUuid() {
        sUuidCounter += 1;
        return sUuidCounter;
    }
    
    private static synchronized long generateToken() {
        sTokenCounter += 1;
        return sTokenCounter;
    }
   
    private class FTListener extends FileTransferListener {
        
        public String transferId;
        private final long mMessageId;
        private final long mToken;
        
        public FTListener(long messageId, long token) {
            super();
            mMessageId = messageId;
            mToken = token;
        }
       
        @Override
        public void onTransferStarted() {
            Log.d(TAG, "FTListener.onTransferStarted()");
            try {
                updateMessageStatusAndSourceId(mMessageId, Constants.MESSAGE_STATUS_SENDING, transferId);
                IPAServiceCallback callback = mClientListeners.get(mToken);
                if (callback != null) {
                    callback.onTransferProgress(mMessageId, 0, 0);
                }
            } catch (RemoteException e) {
                throw new Error(e);
            }
        }
        
        @Override
        public void onTransferProgress(long currentSize, long totalSize) {
            Log.d(TAG, "FTListener.onTransferProgress(" + currentSize + ", " + totalSize + ")");
            updateMessageStatusAndSourceId(mMessageId, Constants.MESSAGE_STATUS_SENDING, transferId);
            try {
                IPAServiceCallback callback = mClientListeners.get(mToken);
                if (callback != null) {
                    callback.onTransferProgress(mMessageId, currentSize, totalSize);
                }
            } catch (RemoteException e) {
                throw new Error(e);
            }
        }
        
        @Override
        public void onTransferError(int error) {
            Log.d(TAG, "FTListener.onTransferError(" + error + ")");
            try {
                final String sourceId = transferId;
                final MessageRecoverRecord record = mMessageRecoverRecords.get(sourceId);
                if (record != null) {
                    boolean shouldRetry = false;
                    if (record.startTimestamp == Constants.INVALID) {
                        record.startTimestamp = System.currentTimeMillis();
                        shouldRetry = true;
                    } else {
                        final long currentTimestamp = System.currentTimeMillis();
                        long timespan = currentTimestamp - record.startTimestamp;
                        if (timespan > 0 && timespan < MessageRecoverRecord.RECOVER_TIMEOUT) {
                            shouldRetry = true;
                        }
                    }
                    if (shouldRetry) {
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Log.d(TAG, "Auto recover FT: " + sourceId);
                                    resendMessageInternal(record.clientToken, record.messageId);
                                } catch (JoynServiceException e) {
                                    e.printStackTrace();
                                    updateMessageStatusAndSourceId(
                                            mMessageId,
                                            Constants.MESSAGE_STATUS_FAILED,
                                            sourceId);
                                    try {
                                        IPAServiceCallback callback = mClientListeners.get(mToken);
                                        if (callback != null) {
                                            callback.onReportMessageFailed(mMessageId);
                                        }
                                    } catch (RemoteException e1) {
                                        throw new Error(e1);
                                    }
                                }
                            }
                        },
                        MessageRecoverRecord.RECOVER_INTERVAL);
                    } else {
                        updateMessageStatusAndSourceId(mMessageId, Constants.MESSAGE_STATUS_FAILED, sourceId);
                        IPAServiceCallback callback = mClientListeners.get(mToken);
                        if (callback != null) {
                            callback.onReportMessageFailed(mMessageId);
                        }
                    }
                } else {
                    updateMessageStatusAndSourceId(mMessageId, Constants.MESSAGE_STATUS_FAILED, sourceId);
                    IPAServiceCallback callback = mClientListeners.get(mToken);
                    if (callback != null) {
                        callback.onReportMessageFailed(mMessageId);
                    }
                }
            } catch (RemoteException e) {
                throw new Error(e);
            }
        }
        
        @Override
        public void onTransferAborted() {
            Log.d(TAG, "FTListener.onTransferAborted");
            try {
                mMessageRecoverRecords.remove(transferId);
                updateMessageStatusAndSourceId(mMessageId, Constants.MESSAGE_STATUS_FAILED, transferId);
                IPAServiceCallback callback = mClientListeners.get(mToken);
                if (callback != null) {
                    callback.onReportMessageFailed(mMessageId);
                }
            } catch (RemoteException e) {
                throw new Error(e);
            }
        }
        
        @Override
        public void onFileTransferred(String filename) {
            Log.d(TAG, "FTListener.onFileTransferred(" + filename + ")");
            try {
                mMessageRecoverRecords.remove(transferId);
                updateMessageStatusAndSourceId(mMessageId, Constants.MESSAGE_STATUS_SENT, transferId);
                IPAServiceCallback callback = mClientListeners.get(mToken);
                if (callback != null) {
                    callback.onReportMessageDelivered(mMessageId);
                }
            } catch (RemoteException e) {
                throw new Error(e);
            }
        }

        @Override
        public void onTransferPaused() {
            // do nothing
            Log.d(TAG, "FTListener.onTransferPaused()");
        }

        @Override
        public void onTransferResumed(String oldTransferId, String newTransferId) {
            // do nothing
            Log.d(TAG, "FTListener.onTransferResumed(" + oldTransferId + ", " + newTransferId + ")");
            updateMessageStatusAndSourceId(mMessageId, Constants.MESSAGE_STATUS_SENDING, newTransferId);
        }
    }

    public class PASBinder extends IPAService.Stub {
        private static final String TAG = Constants.TAG_PREFIX + "PASBinder";
        
        // Callback Management
        @Override
        public synchronized long registerCallback(final IPAServiceCallback callback) throws RemoteException {
            if (callback == null) {
                throw new RemoteException("Illegal parameter: callback is null");
            }
            long token = generateToken();
            Log.d(TAG, "New Token: " + token + " for Callback: " + callback);
            mClientListeners.put(token, callback);
            if (mRcseChatService.isServiceConnected() && mRcseFTService.isServiceConnected()) {
                Log.d(TAG, "Instant connected");
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            callback.onServiceConnected();
                        } catch (RemoteException e) {
                            throw new Error(e);
                        }
                    }
                });
                try {
                    if (mRcseChatService.isServiceRegistered() && mRcseFTService.isServiceRegistered()) {
                        Log.d(TAG, "Instant registered");
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    callback.onServiceRegistered();
                                } catch (RemoteException e) {
                                    throw new Error(e);
                                }
                            }
                        });
                    }
                } catch (JoynServiceException e) {
                    e.printStackTrace();
                    throw new RemoteException(e.getLocalizedMessage());
                }
            }
            return token;
        }
        
        @Override
        public synchronized void unregisterCallback(long token) throws RemoteException {
            mClientListeners.remove(token);
        }

        // Service States
        @Override
        public boolean isServiceConnected() throws RemoteException {
            return mRcseChatService.isServiceConnected() && mRcseFTService.isServiceConnected();
        }
        
        @Override
        public boolean isServiceRegistered() throws RemoteException {
            try {
                return mRcseChatService.isServiceRegistered() && mRcseFTService.isServiceRegistered();
            } catch (JoynServiceException e) {
                throw new RemoteException(e.getLocalizedMessage());
            }
        }

        // Account Management
        @Override
        public synchronized long subscribe(long token, String id) throws RemoteException {
            long requestId = generateUuid();
            final class SubscribeTask extends CancellableTask {
                private final String mUuid;
                public SubscribeTask(long requestId, String id, IPAServiceCallback callback) {
                    super(requestId, callback);
                    mUuid = id;
                }

                @Override
                public void doRun() {
                    Log.d(TAG, "subscribe(" + mId + ", " + mUuid + ")");
                    int result = ResultCode.SUCCESS;
                    try {
                        mClient.subscribe(mUuid);
                        ContentResolver cr = getContentResolver();
                        Cursor c = null;
                        try {
                            c = cr.query(
                                    AccountColumns.CONTENT_URI,
                                    new String[] {
                                        AccountColumns.NAME,
                                        AccountColumns.RECOMMEND_LEVEL,
                                        AccountColumns.LOGO_URL
                                    },
                                    AccountColumns.UUID + "=?",
                                    new String[]{mUuid},
                                    null);
                            if (c == null || c.getCount() == 0) {
                                // insert
                                throw new Error("No account in content provider");
                            } else {
                                // update
                                c.moveToFirst();
                                ContentValues accountValues = new ContentValues();
                                accountValues.put(AccountColumns.SUBSCRIPTION_STATUS, Constants.SUBSCRIPTION_STATUS_YES);
                                cr.update(
                                        AccountColumns.CONTENT_URI,
                                        accountValues,
                                        AccountColumns.UUID + "=?",
                                        new String[]{mUuid});
                            }
                        } finally {
                            if (c != null) {
                                c.close();
                            }
                        }
                    } catch (PAMException e) {
                        result = e.resultCode;
                    }
                    if (mCallback != null) {
                        if (isCancelled()) {
                            result = getCancelReason();
                        }
                        if (isCancelled()) {
                            result = getCancelReason();
                        }
                        try {
                            mCallback.reportSubscribeResult(mId, result);
                        } catch (RemoteException e) {
                            throw new Error(e);
                        }
                    }
                    mWorkingTasks.remove(mId);
                }
            }
            CancellableTask task = new SubscribeTask(requestId, id, mClientListeners.get(token));
            mWorkingTasks.put(requestId, task);
            mExecutorService.execute(task);
            return requestId;
        }

        @Override
        public synchronized long unsubscribe(long token, String id) throws RemoteException {
            long requestId = generateUuid();
            final class UnsubscribeTask extends CancellableTask {
                private final String mUuid;
                public UnsubscribeTask(long requestId, String id, IPAServiceCallback callback) {
                    super(requestId, callback);
                    mUuid = id;
                }

                @Override
                public void doRun() {
                    Log.d(TAG, "unsubscribe(" + mId + ", " + mUuid + ")");
                    int result = ResultCode.SUCCESS;
                    try {
                        mClient.unsubscribe(mUuid);
                        ContentResolver cr = getContentResolver();
                        Cursor c = null;
                        try {
                            c = cr.query(
                                    AccountColumns.CONTENT_URI,
                                    new String[] {
                                        AccountColumns.NAME,
                                        AccountColumns.RECOMMEND_LEVEL,
                                        AccountColumns.LOGO_URL
                                    },
                                    AccountColumns.UUID + "=?",
                                    new String[]{mUuid},
                                    null);
                            if (c == null || c.getCount() == 0) {
                                // insert
                                throw new Error("No account in content provider");
                            } else {
                                // update
                                c.moveToFirst();
                                ContentValues accountValues = new ContentValues();
                                accountValues.put(AccountColumns.SUBSCRIPTION_STATUS, Constants.SUBSCRIPTION_STATUS_NO);
                                cr.update(
                                        AccountColumns.CONTENT_URI,
                                        accountValues,
                                        AccountColumns.UUID + "=?",
                                        new String[]{mUuid});
                            }
                        } finally {
                            if (c != null) {
                                c.close();
                            }
                        }
                    } catch (PAMException e) {
                        result = e.resultCode;
                    }
                    if (mCallback != null) {
                        if (isCancelled()) {
                            result = getCancelReason();
                        }
                        try {
                            mCallback.reportUnsubscribeResult(mId, result);
                        } catch (RemoteException e) {
                            throw new Error(e);
                        }
                    }
                    mWorkingTasks.remove(mId);
                }
            }
            CancellableTask task = new UnsubscribeTask(requestId, id, mClientListeners.get(token));
            mWorkingTasks.put(requestId, task);
            mExecutorService.execute(task);
            return requestId;
        }

        @Override
        public synchronized long getSubscribedList(
                long token,
                int order,
                int pageSize,
                int pageNumber) throws RemoteException {
            long requestId = generateUuid();
            final class GetSubscribedListTask extends CancellableTask {
                private final int mOrder;
                private final int mPageSize;
                private final int mPageNumber;
                public GetSubscribedListTask(
                        long requestId,
                        int order,
                        int pageSize,
                        int pageNumber,
                        IPAServiceCallback callback) {
                    super(requestId, callback);
                    mOrder = order;
                    mPageSize = pageSize;
                    mPageNumber = pageNumber;
                }

                @Override
                public void doRun() {
                    Log.d(TAG, "getSubscribedList(" + mId + ", " + mOrder + ", " + mPageSize + ", " + mPageNumber + ")");
                    int result = ResultCode.SUCCESS;
                    long[] accountIds = null;
                    ContentProviderResult[] r = null;
                    try {
                        List<PublicAccount> accounts = mClient.getSubscribedList(mOrder, mPageSize, mPageNumber);
                        accountIds = new long[accounts.size()];
                        ContentResolver cr = getContentResolver();
                        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
                        for (int i = 0; i < accountIds.length; ++i) {
                            PublicAccount p = accounts.get(i);
                            long logoId = PublicAccount.insertLogoUrl(cr, p.logoUrl);
                            ContentProviderOperation.Builder builder =
                                    ContentProviderOperation.newInsert(AccountColumns.CONTENT_URI);
                            builder.withValue(AccountColumns.UUID, p.uuid)
                                   .withValue(AccountColumns.NAME, p.name)
                                   .withValue(AccountColumns.ID_TYPE, p.idtype)
                                   .withValue(AccountColumns.INTRODUCTION, p.introduction)
                                   .withValue(AccountColumns.LOGO_ID, logoId)
                                   .withValue(AccountColumns.SUBSCRIPTION_STATUS, Constants.SUBSCRIPTION_STATUS_YES);
                            if (p.recommendLevel != Constants.INVALID) {
                                builder.withValue(AccountColumns.RECOMMEND_LEVEL, p.recommendLevel);
                            }
                            operations.add(builder.build());
                        }
                        r = cr.applyBatch(PAContract.AUTHORITY, operations);
                    } catch (PAMException e) {
                        result = e.resultCode;
                    } catch (RemoteException e) {
                        throw new Error(e);
                    } catch (OperationApplicationException e) {
                        throw new Error(e);
                    }
                    if (mCallback != null) {
                        if (isCancelled()) {
                            result = getCancelReason();
                        }
                        try {
                            if (r != null) { 
                                accountIds = new long[r.length];
                                for (int i = 0; i < r.length ; ++i) {
                                    if (r[i] != null && r[i].uri != null) {
                                        accountIds[i] = Long.parseLong(r[i].uri.getLastPathSegment());
                                    }
                                }
                            }
                            mCallback.reportGetSubscribedResult(mId, result, accountIds);
                        } catch (RemoteException e) {
                            throw new Error(e);
                        }
                    }
                    mWorkingTasks.remove(mId);
                }
            }
            CancellableTask task = new GetSubscribedListTask(
                    requestId,
                    order,
                    pageSize,
                    pageNumber,
                    mClientListeners.get(token));
            mWorkingTasks.put(requestId, task);
            mExecutorService.submit(task);
            return requestId;
        }

        @Override
        public synchronized long getDetails(long token, String uuid, String timestamp) throws RemoteException {
            long requestId = generateUuid();
            final class GetDetailsTask extends CancellableTask {
                private final String mUuid;
                private final String mTimestamp;
                public GetDetailsTask(long requestId, String uuid, String timestamp, IPAServiceCallback callback) {
                    super(requestId, callback);
                    mUuid = uuid;
                    mTimestamp = timestamp;
                }

                @Override
                public void doRun() {
                    Log.d(TAG, "getDetails(" + mId + ", " + mUuid + ", " + mTimestamp + ")");
                    int result = ResultCode.SUCCESS;
                    long accountId = -1;
                    try {
                        PublicAccount details = mClient.getDetails(mUuid, mTimestamp);
                        Cursor c = null;
                        ContentResolver cr = getContentResolver();
                        try {
                            c = cr.query(
                                    AccountColumns.CONTENT_URI,
                                    new String[]{AccountColumns._ID},
                                    AccountColumns.UUID + "=?",
                                    new String[]{details.uuid},
                                    null);
                            if (c == null || c.getCount() == 0) {
                                // insert
                                long logoId = PublicAccount.insertLogoUrl(cr, details.logoUrl);
                                ContentValues cv = PublicAccount.storeFullInfoToContentValues(details);
                                cv.remove(AccountColumns.LOGO_URL);
                                cv.remove(AccountColumns.LOGO_PATH);
                                cv.put(AccountColumns.LOGO_ID, logoId);
                                Uri uri = cr.insert(AccountColumns.CONTENT_URI, cv);
                                accountId = Long.parseLong(uri.getLastPathSegment());
                            } else {
                                // update
                                c.moveToFirst();
                                accountId = c.getLong(c.getColumnIndexOrThrow(AccountColumns._ID));
                                long logoId = getLogoId(details.logoUrl);
                                if (logoId == Constants.INVALID) {
                                    logoId = PublicAccount.insertLogoUrl(cr, details.logoUrl);
                                }
                                ContentValues cv = PublicAccount.storeFullInfoToContentValues(details);
                                cv.put(AccountColumns.LOGO_ID, logoId);
                                cv.remove(AccountColumns.LOGO_URL);
                                cv.remove(AccountColumns.LOGO_PATH);
                                cv.remove(AccountColumns.LAST_MESSAGE);
                                cr.update(
                                        AccountColumns.CONTENT_URI,
                                        cv,
                                        AccountColumns._ID + "=?",
                                        new String[]{Long.toString(accountId)});
                            }
                        } finally {
                            if (c != null) {
                                c.close();
                            }
                        }
                    } catch (PAMException e) {
                        result = e.resultCode;
                    }
                    if (mCallback != null) {
                        if (isCancelled()) {
                            result = getCancelReason();
                        }
                        try {
                            mCallback.reportGetDetailsResult(mId, result, accountId);
                        } catch (RemoteException e) {
                            throw new Error(e);
                        }
                    }
                    mWorkingTasks.remove(mId);
                }
            }
            CancellableTask task = new GetDetailsTask(requestId, uuid, timestamp, mClientListeners.get(token));
            mWorkingTasks.put(requestId, task);
            mExecutorService.submit(task);
            return requestId;
        }

        @Override
        public synchronized long getMenu(long token, String uuid, String timestamp) throws RemoteException {
            long requestId = generateUuid();
            final class GetMenuTask extends CancellableTask {
                private final String mUuid;
                private final String mTimestamp;
                public GetMenuTask(long requestId, String uuid, String timestamp, IPAServiceCallback callback) {
                    super(requestId, callback);
                    mUuid = uuid;
                    mTimestamp = timestamp;
                }

                @Override
                public void doRun() {
                    Log.d(TAG, "getMenu(" + mId + ", " + mUuid + ", " + mTimestamp + ")");
                    int result = ResultCode.SUCCESS;
                    try {
                        MenuInfo info = mClient.getMenu(mUuid, mTimestamp);
                        ContentResolver cr = getContentResolver();
                        cr.update(
                                AccountColumns.CONTENT_URI,
                                MenuInfo.storeToContentValues(info),
                                AccountColumns.UUID + "=?",
                                new String[]{info.uuid});
                    } catch (PAMException e) {
                        result = e.resultCode;
                    }
                    if (mCallback != null) {
                        if (isCancelled()) {
                            result = getCancelReason();
                        }
                        if (isCancelled()) {
                            result = getCancelReason();
                        }
                        try {
                            mCallback.reportGetMenuResult(mId, result);
                        } catch (RemoteException e) {
                            throw new Error(e);
                        }
                    }
                    mWorkingTasks.remove(mId);
                }
            }
            CancellableTask task = new GetMenuTask(requestId, uuid, timestamp, mClientListeners.get(token));
            mWorkingTasks.put(requestId, task);
            mExecutorService.submit(task);
            return requestId;
        }

        // Media Downloading
        // TODO check for duplicate downloading
        @Override
        public synchronized long downloadObject(final long token, String url, int type) throws RemoteException {
            final long requestId = generateUuid();
            final class DownloadTask extends CancellableTask {
                private static final int BUFFER_SIZE = 32 * 1024;
                
                private final int mType;
                private final String mUrlString;
                
                public DownloadTask(long requestId, String url, int type, IPAServiceCallback callback) {
                    super(requestId, callback);
                    mUrlString = url;
                    mType = type;
                }
                
                @Override
                public void doRun() {
                    Log.d(TAG,
                          "ThreadID: " + Thread.currentThread().getId()
                          + " downloadObject(" + mId + ", " + mUrlString + ", " + mType + ")");
                    int resultCode = ResultCode.SUCCESS;
                    IPAServiceCallback callback = mClientListeners.get(token);
                    Log.d(TAG, "Task ID: " + mId + ", Thread ID: " + Thread.currentThread().getId() + ", callback ready");
                    File file = null;
                    FileOutputStream fos = null;
                    BufferedInputStream bis = null;
                    long mediaId = Constants.INVALID;
                    try {
                        URL url = new URL(mUrlString);
                        Log.d(TAG, "Task ID: " + mId + ", Thread ID: " + Thread.currentThread().getId() + ", URL built");
                        URLConnection conn = url.openConnection();
                        Log.d(TAG,
                              "Task ID: " + mId + ", Thread ID: " + Thread.currentThread().getId() + ", connnection opened");
                        if (mId == 1) {
                            Log.d(TAG, "Request 1");
                        }
                        final int fileSize = conn.getContentLength();
                        final String contentType = conn.getContentType();
                        Log.d(TAG,
                              "Task ID: " + mId + ", Thread ID: " + Thread.currentThread().getId()
                              + ", Content Length: " + fileSize);
                        String filename = MediaFolder.generateMediaFileName(
                                Constants.INVALID,
                                mType,
                                MediaFolder.getExtensionFromMimeType(contentType));
                        if (filename == null) {
                            resultCode = ResultCode.SYSEM_ERROR_UNKNOWN;
                        } else {
                            file = new File(filename);
                            Log.d(TAG,
                                  "Task ID: " + mId + ", Thread ID: " + Thread.currentThread().getId()
                                  + ", Temp File: " + file.getAbsolutePath());
                            byte[] buffer = new byte[BUFFER_SIZE];
                            fos = new FileOutputStream(file);
                            bis = new BufferedInputStream(conn.getInputStream());
                            Log.d(TAG,
                                  "Task ID: " + mId + ", Thread ID: " + Thread.currentThread().getId()
                                  + ", input stream opened");
                            int currentPosition = 0;
                            int currentPercentage = 0;
                            while (currentPosition < fileSize) {
                                Log.d(TAG,
                                      "Task ID: " + mId + ", Thread ID: " + Thread.currentThread().getId()
                                      + ", isCancelled: " + isCancelled());
                                if (isCancelled()) {
                                    Log.d(TAG,
                                          "Task ID: " + mId + ", Thread ID: " + Thread.currentThread().getId()
                                          + ", cancelled");
                                    resultCode = getCancelReason();
                                    break;
                                }
                                int length = bis.read(buffer);
                                fos.write(buffer, 0, length);  
                                currentPosition += length;
                                int percentage = currentPosition * 100 / fileSize;
                                if (percentage > currentPercentage) {
                                    callback.updateDownloadProgress(requestId, percentage);
                                    currentPercentage = percentage;
                                }
                            }
                            fos.close();
                            mediaId = storeMediaFile(file.getAbsolutePath(), mType, false, mUrlString);
                        }
                    } catch (MalformedURLException e) {
                        Log.e(TAG, "Invalid URL format: " + mUrlString);
                        resultCode = ResultCode.PARAM_ERROR_INVALID_FORMAT;
                    } catch (IOException e) {
                        resultCode = ResultCode.SYSTEM_ERROR_NETWORK;
                    } catch (RemoteException e) {
                        throw new Error(e);
                    } finally {
                        try {
                            if (bis != null) {
                                bis.close();
                            }
                            if (fos != null) {
                                fos.close();
                            }
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        if (mCallback != null) {
                            if (isCancelled()) {
                                resultCode = getCancelReason();
                            }
                            try {
                                Log.d(TAG,
                                      "Task ID: " + mId + ", Thread ID: " + Thread.currentThread().getId()
                                      + ", report result");
                                mCallback.reportDownloadResult(
                                        mId,
                                        resultCode,
                                        file == null ? null : file.getAbsolutePath(), mediaId);
                            } catch (RemoteException e) {
                                throw new Error(e);
                            }
                        }
                    }
                    mWorkingTasks.remove(mId);
                }
            }
            DownloadTask task = new DownloadTask(requestId, url, type, mClientListeners.get(token));
            mWorkingTasks.put(requestId, task);
            mExecutorService.submit(task);
            return requestId;
        }

        @Override
        public synchronized void cancelDownload(long requestId) throws RemoteException {
            Log.d(TAG, "cancelDownload(" + requestId + ")");
            mWorkingTasks.get(requestId).cancel();
        }

        // Messaging
        @Override
        public long sendMessage(long token, long accountId, String message, boolean system) throws RemoteException {
            Log.d(TAG, "sendMessage(" + accountId + ", " + message + ", " + system + ")");
            PublicAccountChat chat = getOrInitChat(accountId, token);
            String sourceId = null;
            try {
                if (message.length() <= Constants.LARGE_MESSAGE_THRESHOLD) {
                    Log.d(TAG, "Send in Pager Mode");
                    sourceId = chat.sendPublicAccountMessageByPagerMode(message);
                } else {
                    Log.d(TAG, "Send in Large Mode");
                    sourceId = chat.sendPublicAccountMessageByLargeMode(message);
                }
            } catch (JoynServiceException e) {
                throw new Error(e);
            }

            if (system) {
                mPendingSystemMessages.add(sourceId);
            }
            
            long messageId = copyMessageFromChatProvider(sourceId, system, accountId);
            if (messageId != Constants.INVALID) {
                MessageRecoverRecord mrr = new MessageRecoverRecord(messageId, token);
                mMessageRecoverRecords.put(sourceId, mrr);
                Log.d(TAG, "Add MRR for " + sourceId);
            } else {
                Log.w(TAG, "Failed to initiate sending of message: " + message);
            }
            return messageId;
        }

        @Override
        public long sendImage(final long token, long accountId, String path, String thumbnailPath) throws RemoteException {
            long requestId = generateUuid();
            final class SendImageTask extends CancellableTask {
                private final long mAccountId;
                private final String mPath;
                private final String mThumbnailPath;

                public SendImageTask(
                        long requestId,
                        long accountId,
                        String path,
                        String thumbnailPath,
                        IPAServiceCallback callback) {
                    super(requestId, callback);
                    mAccountId = accountId;
                    mPath = path;
                    mThumbnailPath = thumbnailPath;
                }
                
                @Override
                public void doRun() {
                    Log.d(TAG, "sendImage(" + mId + ", " + mAccountId + ", " + mPath + ", " + mThumbnailPath + ")");
                    final long messageId = storeOutgoingMediaMessage(
                            mAccountId,
                            mPath,
                            null,
                            Constants.MEDIA_TYPE_PICTURE,
                            null);
                    FTListener listener = new FTListener(messageId, token);
                    final IPAServiceCallback callback = mClientListeners.get(token);
                    FileTransfer ft = null;
                    try {
                        ft = mRcseFTService.transferPublicChatFile(
                                Constants.SIP_PREFIX + PublicAccount.queryAccountUuid(PAServiceImpl.this, mAccountId),
                                mPath,
                                null,
                                listener,
                                0);
                        if (ft != null) {
                            String sourceId = ft.getTransferId();
                            Log.d(TAG, "FT sourceId is " + sourceId);
                            listener.transferId = sourceId;
                            if (messageId != Constants.INVALID) {
                                mMessageRecoverRecords.put(
                                        sourceId,
                                        new MessageRecoverRecord(
                                                messageId,
                                                token));
                            }
                        }
                    } catch (JoynServiceException e) {
                        throw new Error(e);
                    }
                    
                    mWorkingTasks.remove(mId);
                    
                    if (ft == null) {
                        try {
                            if (callback != null) {
                                callback.onReportMessageFailed(messageId);
                            }
                        } catch (RemoteException e) {
                            throw new Error(e);
                        }
                    }
                }
            }
            SendImageTask task = new SendImageTask(requestId, accountId, path, thumbnailPath, mClientListeners.get(token));
            mWorkingTasks.put(requestId, task);
            mExecutorService.submit(task);
            return requestId;
        }

        @Override
        public long sendAudio(final long token, long accountId, String path, int duration) throws RemoteException {
            long requestId = generateUuid();
            final class SendAudioTask extends CancellableTask {
                private final long mAccountId;
                private final String mPath;
                private int mDuration;

                public SendAudioTask(
                        long requestId,
                        long accountId,
                        String path,
                        int duration,
                        IPAServiceCallback callback) {
                    super(requestId, callback);
                    mAccountId = accountId;
                    mPath = path;
                    mDuration = duration;
                }
                
                @Override
                public void doRun() {
                    Log.d(TAG, "sendAudio(" + mId + ", " + mAccountId + ", " + mPath + ", " + mDuration + ")");
                    final long messageId = storeOutgoingMediaMessage(
                            mAccountId,
                            mPath,
                            null,
                            Constants.MEDIA_TYPE_AUDIO,
                            Integer.toString(mDuration));
                    FTListener listener = new FTListener(messageId, token);
                    final IPAServiceCallback callback = mClientListeners.get(token);
                    FileTransfer ft = null;
                    try {
                        ft = mRcseFTService.transferPublicChatFile(
                                Constants.SIP_PREFIX + PublicAccount.queryAccountUuid(PAServiceImpl.this, mAccountId),
                                mPath,
                                null,
                                listener,
                                mDuration
                            );
                        if (ft != null) {
                            String sourceId = ft.getTransferId();
                            Log.d(TAG, "FT sourceId is " + sourceId);
                            listener.transferId = sourceId;
                            if (messageId != Constants.INVALID) {
                                mMessageRecoverRecords.put(
                                        sourceId,
                                        new MessageRecoverRecord(
                                                messageId,
                                                token));
                            }
                        }
                    } catch (JoynServiceException e) {
                        throw new Error(e);
                    }
                    if (ft == null) {
                        try {
                            if (callback != null) {
                                callback.onReportMessageFailed(messageId);
                            }
                        } catch (RemoteException e) {
                            throw new Error(e);
                        }
                    }
                    mWorkingTasks.remove(mId);
                }
            }
            SendAudioTask task = new SendAudioTask(requestId, accountId, path, duration, mClientListeners.get(token));
            mWorkingTasks.put(requestId, task);
            mExecutorService.submit(task);
            return requestId;
        }

        @Override
        public long sendVideo(
                final long token,
                long accountId,
                String path,
                String thumbnailPath,
                int duration) throws RemoteException {
            long requestId = generateUuid();
            final class SendVideoTask extends CancellableTask {
                private final long mAccountId;
                private final String mPath;
                private String mThumbnailPath;
                private int mDuration;

                public SendVideoTask(
                        long requestId,
                        long accountId,
                        String path,
                        String thumbnailPath,
                        int duration,
                        IPAServiceCallback callback) {
                    super(requestId, callback);
                    mAccountId = accountId;
                    mPath = path;
                    mThumbnailPath = thumbnailPath;
                    mDuration = duration;
                }
                
                @Override
                public void doRun() {
                    Log.d(TAG,
                          "sendVideo(" + mId + ", " + mAccountId + ", " + mPath + ", "
                    + mThumbnailPath + ", " + mDuration + ")");
                    final long messageId = storeOutgoingMediaMessage(
                            mAccountId,
                            mPath,
                            mThumbnailPath,
                            Constants.MEDIA_TYPE_VIDEO,
                            Integer.toString(mDuration));
                    FTListener listener = new FTListener(messageId, token);
                    final IPAServiceCallback callback = mClientListeners.get(token);
                    FileTransfer ft = null;
                    try {
                        ft = mRcseFTService.transferPublicChatFile(
                                Constants.SIP_PREFIX + PublicAccount.queryAccountUuid(PAServiceImpl.this, mAccountId),
                                mPath,
                                mThumbnailPath,
                                listener,
                                mDuration
                                );
                        if (ft != null) {
                            String sourceId = ft.getTransferId();
                            Log.d(TAG, "FT sourceId is " + sourceId);
                            listener.transferId = sourceId;
                            if (messageId != Constants.INVALID) {
                                mMessageRecoverRecords.put(
                                        sourceId,
                                        new MessageRecoverRecord(
                                                messageId,
                                                token));
                            }
                        }
                    } catch (JoynServiceException e) {
                        throw new Error(e);
                    }
                    
                    if (ft == null) {
                        try {
                            if (callback != null) {
                                callback.onReportMessageFailed(messageId);
                            }
                        } catch (RemoteException e) {
                            throw new Error(e);
                        }
                    }
                    mWorkingTasks.remove(mId);
                }
            }
            SendVideoTask task = new SendVideoTask(
                    requestId,
                    accountId,
                    path,
                    thumbnailPath,
                    duration,
                    mClientListeners.get(token));
            mWorkingTasks.put(requestId, task);
            mExecutorService.submit(task);
            return requestId;
        }

        @Override
        public long setAcceptStatus(long token, String uuid, int acceptStatus) throws RemoteException {
            long requestId = generateUuid();
            final class SetAcceptStatusTask extends CancellableTask {
                private final String mUuid;
                private final int mAcceptStatus;

                public SetAcceptStatusTask(long requestId, String uuid, int acceptStatus, IPAServiceCallback callback) {
                    super(requestId, callback);
                    mUuid = uuid;
                    mAcceptStatus = acceptStatus;
                }
                
                @Override
                public void doRun() {
                    Log.d(TAG, "setAcceptStatus(" + mId + ", " + mUuid + ", " + mAcceptStatus + ")");
                    int result = ResultCode.SUCCESS;
                    try {
                        result = mClient.setAcceptStatus(mUuid, mAcceptStatus);
                        if (result == ResultCode.SUCCESS) {
                            ContentResolver cr = getContentResolver();
                            ContentValues cv = new ContentValues();
                            cv.put(AccountColumns.ACCEPT_STATUS, mAcceptStatus);
                            cr.update(
                                    AccountColumns.CONTENT_URI,
                                    cv,
                                    AccountColumns.UUID + "=?",
                                    new String[]{mUuid});
                        }
                    } catch (PAMException e) {
                        result = e.resultCode;
                    }
                    if (mCallback != null) {
                        if (isCancelled()) {
                            result = getCancelReason();
                        }
                        try {
                            mCallback.reportSetAcceptStatusResult(mId, result);
                        } catch (RemoteException e) {
                            throw new Error(e);
                        }
                    }
                    mWorkingTasks.remove(mId);
                }
            }
            SetAcceptStatusTask task = new SetAcceptStatusTask(requestId, uuid, acceptStatus, mClientListeners.get(token));
            mWorkingTasks.put(requestId, task);
            mExecutorService.submit(task);
            return requestId;
        }

        @Override
        public void complainSpamMessage(final long token, final long messageId) throws RemoteException {
            try {
                String sourceId;
                int sourceTable;
                Cursor c = null;
                try {
                    c = getContentResolver().query(
                            MessageColumns.CONTENT_URI,
                            new String[] {
                                MessageColumns.SOURCE_ID,
                                MessageColumns.SOURCE_TABLE,
                            },
                            MessageColumns._ID + "=?",
                            new String[] {Long.toString(messageId)},
                            null);
                    if (c != null && c.getCount() > 0) {
                        c.moveToFirst();
                        sourceTable = c.getInt(c.getColumnIndexOrThrow(MessageColumns.SOURCE_TABLE));
                        sourceId = c.getString(c.getColumnIndexOrThrow(MessageColumns.SOURCE_ID));
                    } else {
                        throw new RemoteException("Invalid Message ID");
                    }
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }

                if (sourceTable == Constants.TABLE_MESSAGE) {
                    final String id = sourceId;
                    SpamReportListener listener = new SpamReportListener() {
                        @Override
                        public void onSpamReportSuccess(String contact, String msgId) {
                            try {
                                if (id.equals(msgId)) {
                                    mClientListeners.get(token).reportComplainSpamSuccess(messageId);
                                    mRcseChatService.removeSpamReportListener(this);
                                }
                            } catch (RemoteException e) {
                                throw new Error(e);
                            } catch (JoynServiceException e) {
                                throw new Error(e);
                            }
                        }

                        @Override
                        public void onSpamReportFailed(String contact, String msgId, int errorCode) {
                            try {
                                if (id.equals(msgId)) {
                                    mClientListeners.get(token).reportComplainSpamFailed(messageId, errorCode);
                                    mRcseChatService.removeSpamReportListener(this);
                                }
                            } catch (RemoteException e) {
                                throw new Error(e);
                            } catch (JoynServiceException e) {
                                throw new Error(e);
                            }
                        }
                    };
                    mRcseChatService.addSpamReportListener(listener);
                    mRcseChatService.initiateSpamReport(SPAM_MESSAGE_REPORT_RECEIVER, sourceId);
                } else if (sourceTable == Constants.TABLE_FT) {
                    final String id = sourceId;
                    final FileSpamReportListener listener = new FileSpamReportListener() {
                        
                        @Override
                        public void onFileSpamReportSuccess(String contact, String msgId) {
                            try {
                                if (id.equals(msgId)) {
                                    mClientListeners.get(token).reportComplainSpamSuccess(messageId);
                                    mRcseFTService.removeFileSpamReportListener(this);
                                }
                            } catch (RemoteException e) {
                                throw new Error(e);
                            } catch (JoynServiceException e) {
                                throw new Error(e);
                            }
                        }
                        
                        @Override
                        public void onFileSpamReportFailed(String contact, String msgId, int errorCode) {
                            try {
                                if (id.equals(msgId)) {
                                    mClientListeners.get(token).reportComplainSpamFailed(messageId, errorCode);
                                    mRcseFTService.removeFileSpamReportListener(this);
                                }
                            } catch (RemoteException e) {
                                throw new Error(e);
                            } catch (JoynServiceException e) {
                                throw new Error(e);
                            }
                        }
                    };
                    mRcseFTService.addFileSpamReportListener(listener);
                    mRcseFTService.initiateFileSpamReport(SPAM_MESSAGE_REPORT_RECEIVER, sourceId);
                } else {
                    throw new RemoteException("Invalid Source Table");
                }
            } catch (JoynServiceException e) {
                // FIXME check for errors
                e.printStackTrace();
                throw new RemoteException(e.getLocalizedMessage());
            }
        }

        @Override
        public void resendMessage(long token, long messageId) throws RemoteException {
            Log.d(TAG, "resendMessage(" + messageId + ")");
            try {
                resendMessageInternal(token, messageId);
            } catch (JoynServiceException e) {
                e.printStackTrace();
                throw new RemoteException(e.getLocalizedMessage());
            }
        }

        @Override
        public long sendGeoLoc(final long token, long accountId, String data) throws RemoteException {
            long requestId = generateUuid();
            final String xmlHeaderString = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>";
            if (data != null && !data.startsWith(xmlHeaderString)) {
                data = xmlHeaderString + data;
            }
            
            final class SendGeoLocTask extends CancellableTask {
                private final long mAccountId;
                private String mPath;
                private final String mData;

                public SendGeoLocTask(
                        long requestId,
                        long accountId,
                        String data,
                        IPAServiceCallback callback) {
                    super(requestId, callback);
                    mAccountId = accountId;
                    mData = data;
                }
                
                @Override
                public void doRun() {
                    Log.d(TAG, "sendGeoLoc(" + mId + ", " + mAccountId + ", " + mData + ")");
                    Pair<Long, String> resultPair = storeOutgoingRawMediaMessage(
                            mAccountId,
                            mData,
                            Constants.MEDIA_TYPE_GEOLOC);
                    mPath = resultPair.value2;
                    final long messageId = resultPair.value1;
                    final IPAServiceCallback callback = mClientListeners.get(token);
                    FTListener listener = new FTListener(messageId, token);
                    FileTransfer ft = null;
                    try {
                        ft = mRcseFTService.transferPublicChatFile(
                                Constants.SIP_PREFIX + PublicAccount.queryAccountUuid(PAServiceImpl.this, mAccountId),
                                mPath,
                                null,
                                listener,
                                0);
                        if (ft != null) {
                            String sourceId = ft.getTransferId();
                            Log.d(TAG, "FT sourceId is " + sourceId);
                            listener.transferId = sourceId;
                            if (messageId != Constants.INVALID) {
                                mMessageRecoverRecords.put(
                                        sourceId,
                                        new MessageRecoverRecord(
                                                messageId,
                                                token));
                            }
                        }
                    } catch (JoynServiceException e) {
                        throw new Error(e);
                    }
                    
                    mWorkingTasks.remove(mId);
                    
                    if (ft == null) {
                        try {
                            if (callback != null) {
                                callback.onReportMessageFailed(messageId);
                            }
                        } catch (RemoteException e) {
                            throw new Error(e);
                        }
                    }
                }
            }
            SendGeoLocTask task = new SendGeoLocTask(requestId, accountId, data, mClientListeners.get(token));
            mWorkingTasks.put(requestId, task);
            mExecutorService.submit(task);
            return requestId;
        }

        @Override
        public long sendVcard(final long token, long accountId, String data) throws RemoteException {
            long requestId = generateUuid();
            final class SendVcardTask extends CancellableTask {
                private final long mAccountId;
                private String mPath;
                private final String mData;

                public SendVcardTask(
                        long requestId,
                        long accountId,
                        String data,
                        IPAServiceCallback callback) {
                    super(requestId, callback);
                    mAccountId = accountId;
                    mData = data;
                }
                
                @Override
                public void doRun() {
                    Log.d(TAG, "sendVcard(" + mId + ", " + mAccountId + ", " + mData + ")");
                    Pair<Long, String> resultPair = storeOutgoingRawMediaMessage(
                            mAccountId,
                            mData,
                            Constants.MEDIA_TYPE_VCARD);
                    mPath = resultPair.value2;
                    final long messageId = resultPair.value1;
                    FTListener listener = new FTListener(messageId, token);
                    final IPAServiceCallback callback = mClientListeners.get(token);
                    FileTransfer ft = null;
                    try {
                        ft = mRcseFTService.transferPublicChatFile(
                                Constants.SIP_PREFIX + PublicAccount.queryAccountUuid(PAServiceImpl.this, mAccountId),
                                mPath,
                                null,
                                listener,
                                0);
                        if (ft != null) {
                            String sourceId = ft.getTransferId();
                            Log.d(TAG, "FT sourceId is " + sourceId);
                            listener.transferId = sourceId;
                            if (messageId != Constants.INVALID) {
                                mMessageRecoverRecords.put(
                                        sourceId,
                                        new MessageRecoverRecord(
                                                messageId,
                                                token));
                            }
                        }
                    } catch (JoynServiceException e) {
                        throw new Error(e);
                    }
                    
                    mWorkingTasks.remove(mId);
                    
                    if (ft == null) {
                        try {
                            if (callback != null) {
                                callback.onReportMessageFailed(messageId);
                            }
                        } catch (RemoteException e) {
                            throw new Error(e);
                        }
                    }
                }
            }
            SendVcardTask task = new SendVcardTask(requestId, accountId, data, mClientListeners.get(token));
            mWorkingTasks.put(requestId, task);
            mExecutorService.submit(task);
            return requestId;
        }

        @Override
        public boolean deleteMessage(long token, long messageId) throws RemoteException {
            Log.d(TAG, "deleteMessage(" + messageId + ")");
            // Mark deleted
            ContentValues cv = new ContentValues();
            cv.put(MessageColumns.DELETED, Constants.DELETED_YES);
            int updateCount = getContentResolver().update(
                    MessageColumns.CONTENT_URI,
                    cv,
                    MessageColumns._ID + "=?",
                    new String[]{Long.toString(messageId)});
            deleteMessagesInBackground(new long[] {messageId});
            return (updateCount == 1);
        }

        @Override
        public long deleteMessageByAccount(long token, final long accountId) throws RemoteException {
            Log.d(TAG, "deleteMessageByAccount(" + accountId + ")");
            long requestId = generateUuid();
            final class BatchDeleteTask extends CancellableTask {
                public BatchDeleteTask(long requestId, IPAServiceCallback callback) {
                    super(requestId, callback);
                }

                @Override
                public void doRun() {
                    Log.d(TAG, "deleteMessageByAccount(" + mId + ", " + accountId + ")");
                    int result = ResultCode.SUCCESS;
                    Cursor c = null;
                    try {
                        c = getContentResolver().query(
                                MessageColumns.CONTENT_URI,
                                new String[]{MessageColumns._ID},
                                MessageColumns.ACCOUNT_ID + "=?",
                                new String[]{Long.toString(accountId)},
                                null);
                        if (c != null && c.getCount() > 0) {
                            // Collect message IDs
                            long[] messageIds = new long[c.getCount()];
                            c.moveToFirst();
                            int index = c.getColumnIndexOrThrow(MessageColumns._ID);
                            for (int i = 0; i < c.getCount(); ++i) {
                                messageIds[i] = c.getLong(index);
                                c.moveToNext();
                            }
                            // Mark deleted
                            ContentValues cv = new ContentValues();
                            cv.put(MessageColumns.DELETED, Constants.DELETED_YES);
                            int updateCount = getContentResolver().update(
                                    MessageColumns.CONTENT_URI,
                                    cv,
                                    MessageColumns.ACCOUNT_ID + "=? AND " + MessageColumns.DELETED + "!=" + Constants.DELETED_YES,
                                    new String[]{Long.toString(accountId)});
                            // Sanity check
                            if (updateCount != messageIds.length) {
                                throw new Error("Failed to mark deleted flags: " + messageIds.length + " to mark, " + updateCount + " done");
                            }
                            // Delete in background
                            deleteMessagesInBackground(messageIds);
                        }
                    } finally {
                        if (c != null) {
                            c.close();
                        }
                    }

                    if (mCallback != null) {
                        if (isCancelled()) {
                            result = getCancelReason();
                        }
                        try {
                            mCallback.reportDeleteMessageResult(mId, result);
                        } catch (RemoteException e) {
                            throw new Error(e);
                        }
                    }
                    mWorkingTasks.remove(mId);
                }
            }
            CancellableTask task = new BatchDeleteTask(requestId, mClientListeners.get(token));
            mWorkingTasks.put(requestId, task);
            mExecutorService.execute(task);
            return requestId;
        }
    }
    
    private void deleteMessagesInBackground(final long[] messageIds) {
       class DeleteMessageTask extends CancellableTask {
            private static final int BATCH_SIZE = 5;

            public DeleteMessageTask(long requestId, IPAServiceCallback callback) {
                super(requestId, callback, true);
            }

            @Override
            protected void doRun() {
                StringBuilder sb = new StringBuilder("deleteMessagesInBackground(");
                for (long id : messageIds) {
                    sb.append(id).append(", ");
                }
                sb.append(")");
                Log.d(TAG, sb.toString());
                for (int i = 0; i < messageIds.length; ++i) {
                    deleteFullMessageContent(messageIds[i]);
                    if ((i % BATCH_SIZE) == 0) {
                        Thread.yield();
                    }
                }
                mWorkingTasks.remove(mId);
            }
        }
        long requestId = generateUuid();
        DeleteMessageTask task = new DeleteMessageTask(requestId, null);
        mWorkingTasks.put(requestId, task);
        mExecutorService.submit(task);
    }
    
    private void markSendingMessageAsFailed() {
        Log.d(TAG, "markSendingMessageAsFailed");
        ContentValues cv = new ContentValues();
        cv.put(MessageColumns.STATUS, Constants.MESSAGE_STATUS_FAILED);
        int count = getContentResolver().update(
                MessageColumns.CONTENT_URI,
                cv,
                MessageColumns.STATUS + "=? OR " + MessageColumns.STATUS + "=?",
                new String[] {
                        Integer.toString(Constants.MESSAGE_STATUS_TO_SEND),
                        Integer.toString(Constants.MESSAGE_STATUS_SENDING)});
        Log.d(TAG, "Total " + count + " messages are marked as failed.");
        if (PlatformManager.getInstance().supportCcs()) {
            markCcsSendingMessageAsFailed();
        }
    }
    
    private void markCcsSendingMessageAsFailed() {
        ContentValues cv = new ContentValues();
        cv.put(com.cmcc.ccs.chat.ChatMessage.MESSAGE_STATUS, com.cmcc.ccs.chat.ChatMessage.FAILED);
        int count = getContentResolver().update(
                CCS_MESSAGE_CONTENT_URI,
                cv,
                com.cmcc.ccs.chat.ChatMessage.FLAG + "=? AND (" + com.cmcc.ccs.chat.ChatMessage.MESSAGE_STATUS + "=? OR " + com.cmcc.ccs.chat.ChatMessage.MESSAGE_STATUS + "=?)",
                new String[] {
                        Integer.toString(com.cmcc.ccs.chat.ChatMessage.PUBLIC),
                        Integer.toString(com.cmcc.ccs.chat.ChatMessage.TO_SEND),
                        Integer.toString(com.cmcc.ccs.chat.ChatMessage.SENDING)});
    }
    
    private void deleteFullMessageContent(long messageId) {
        Log.d(TAG, "deleteFullMessageContent(" + messageId + ")");

        ContentResolver cr = getContentResolver();
        MessageContent messageContent = MessageContent.loadFromProvider(messageId, cr, true);

        if (PlatformManager.getInstance().supportCcs()) {
            deleteFromCcsMessageProvider(messageContent);
        }

        cr.delete(MessageColumns.CONTENT_URI, MessageColumns._ID + "=?", new String[]{Long.toString(messageId)});
        if (messageContent.mediaId != Constants.INVALID) {
            cr.delete(
                    MediaColumns.CONTENT_URI,
                    MediaColumns._ID + "=?",
                    new String[]{Long.toString(messageContent.mediaId)});
            if (!TextUtils.isEmpty(messageContent.mediaPath)) {
            Utils.deleteFile(messageContent.mediaPath);
        }
        }
        if (messageContent.basicMedia != null) {
           if (messageContent.basicMedia.originalId != Constants.INVALID) {
               cr.delete(
                       MediaColumns.CONTENT_URI,
                       MediaColumns._ID + "=?",
                       new String[]{Long.toString(messageContent.basicMedia.originalId)});
               if (!TextUtils.isEmpty(messageContent.basicMedia.originalPath)) {
               Utils.deleteFile(messageContent.basicMedia.originalPath);
               } else {
                   Log.w(TAG, "Original path of media is empty: " + messageContent.basicMedia.originalId);
               }
           }
           if (messageContent.basicMedia.thumbnailId != Constants.INVALID) {
               cr.delete(
                       MediaColumns.CONTENT_URI,
                       MediaColumns._ID + "=?",
                       new String[]{Long.toString(messageContent.basicMedia.thumbnailId)});
               if (!TextUtils.isEmpty(messageContent.basicMedia.thumbnailPath)) {
               Utils.deleteFile(messageContent.basicMedia.thumbnailPath);
               } else {
                   Log.w(TAG, "Thumbnail path of media is empty: " + messageContent.basicMedia.thumbnailId);
               }
           }
           cr.delete(
                   MediaBasicColumns.CONTENT_URI,
                   MediaBasicColumns._ID + "=?",
                   new String[]{Long.toString(messageContent.basicMedia.id)});
        }
        if (messageContent.article != null && messageContent.article.size() > 0) {
            for (MediaArticle ma : messageContent.article) {
                if (ma != null) {
                    if (ma.originalId != Constants.INVALID) {
                        cr.delete(
                                MediaColumns.CONTENT_URI,
                                MediaColumns._ID + "=?",
                                new String[]{Long.toString(ma.originalId)});
                        if (!TextUtils.isEmpty(ma.originalPath)) {
                        Utils.deleteFile(ma.originalPath);
                    }
                    }
                    if (ma.thumbnailId != Constants.INVALID) {
                        cr.delete(
                                MediaColumns.CONTENT_URI,
                                MediaColumns._ID + "=?",
                                new String[]{Long.toString(ma.thumbnailId)});
                        if (!TextUtils.isEmpty(ma.thumbnailPath)) {
                        Utils.deleteFile(ma.thumbnailPath);
                    }
                    }
                    cr.delete(
                            MediaArticleColumns.CONTENT_URI,
                            MediaArticleColumns._ID + "=?",
                            new String[]{Long.toString(ma.id)});
                } else {
                    Log.w(TAG, "MediaArticle is null");
                }
            }
        }
    }
    
    private void updateMessageStatusAndSourceId(long messageId, int status, String sourceId) {
        ContentResolver cr = getContentResolver();
        ContentValues cv = new ContentValues();
        cv.put(PAContract.MessageColumns.STATUS, status);
        if (sourceId != null) {
            cv.put(PAContract.MessageColumns.SOURCE_ID, sourceId);
        }
        cr.update(
                PAContract.MessageColumns.CONTENT_URI,
                cv,
                PAContract.MessageColumns._ID + "=?",
                new String[]{Long.toString(messageId)});
        if (PlatformManager.getInstance().supportCcs()) {
            updateCcsMessageStatus(messageId, status);
        }
    }
    
    private long storeMediaFile(String path, int type, boolean copy, String url) {
        File file = null;
        if (copy) { 
            try {
                File f = new File(path);
                String filename = MediaFolder.generateMediaFileName(
                        Constants.INVALID,
                        type,
                        f.getName());
                if (filename == null) {
                    return Constants.INVALID;
                }
                file = new File(filename);
                Utils.copyFile(path, filename);
            } catch (IOException e) {
                throw new Error(e);
            }
        } else {
            file = new File(path);
        }
        
        ContentValues cv = new ContentValues();
        cv.put(MediaColumns.TYPE, type);
        cv.put(MediaColumns.TIMESTAMP, file.lastModified());
        cv.put(MediaColumns.PATH, file.getAbsolutePath());

        Cursor c = null;
        long mediaId = Constants.INVALID;
        ContentResolver cr = getContentResolver();
        if (TextUtils.isEmpty(url)) {
            // insert to media table
            Uri uri = cr.insert(MediaColumns.CONTENT_URI, cv);
            mediaId = Long.parseLong(uri.getLastPathSegment());
        } else {
            try {
                c = getContentResolver().query(
                        MediaColumns.CONTENT_URI,
                        new String[]{MediaColumns._ID},
                        MediaColumns.URL + "=?",
                        new String[]{url},
                        null);
                if (c != null && c.getCount() > 0) {
                    c.moveToFirst();
                    mediaId = c.getLong(c.getColumnIndexOrThrow(MediaColumns._ID));
                    cr.update(
                            MediaColumns.CONTENT_URI,
                            cv,
                            MediaColumns._ID + "=?",
                            new String[]{Long.toString(mediaId)});
                } else {
                    // insert to media table
                    Uri uri = cr.insert(MediaColumns.CONTENT_URI, cv);
                    mediaId = Long.parseLong(uri.getLastPathSegment());
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }
        
        if (copy) {
            final String newPathString = MediaFolder.generateMediaFileName(
                    mediaId,
                    type,
                    MediaFolder.extractExtension(file.getName()));
            cv = new ContentValues();
            cv.put(MediaColumns.PATH, newPathString);
            cr.update(
                    MediaColumns.CONTENT_URI,
                    cv,
                    MediaColumns._ID + "=?",
                    new String[]{Long.toString(mediaId)});
            file.renameTo(new File(newPathString));
        }
        Log.d(TAG, "storeMediaFile: " + path + " as " + mediaId);
        return mediaId;
    }

    /*
     * This method only applies to GeoLoc and vCard.
     */
    private Pair<Long, String> storeContentToMediaFile(String content, int type) {
        String extension = null;
        if (type == Constants.MEDIA_TYPE_GEOLOC) {
            extension = ".xml";
        } else if (type == Constants.MEDIA_TYPE_VCARD) {
            extension = ".vcf";
        } else {
            throw new Error("Invalid media type: " + type);
        }
        String filename = MediaFolder.generateMediaFileName(Constants.INVALID, type, extension);
        if (filename == null) {
            return new Pair<Long, String>((long) Constants.INVALID, null);
        }
        
        try {
            File file = new File(filename);
            Utils.storeToFile(content, filename);
            
            // insert to media table
            ContentValues cv = new ContentValues();
            cv.put(MediaColumns.TYPE, type);
            cv.put(MediaColumns.TIMESTAMP, file.lastModified());
            cv.put(MediaColumns.PATH, file.getAbsolutePath());
            Uri uri = getContentResolver().insert(MediaColumns.CONTENT_URI, cv);
            final long mediaId = Long.parseLong(uri.getLastPathSegment());
            
            final String newPathString = MediaFolder.generateMediaFileName(
                    mediaId,
                    type,
                    MediaFolder.extractExtension(file.getName()));
            cv = new ContentValues();
            cv.put(MediaColumns.PATH, newPathString);
            getContentResolver().update(uri, cv, null, null);
            file.renameTo(new File(newPathString));
            Log.d(TAG, "storeMediaFile: " + file.getAbsolutePath() + " as " + mediaId);
            return new Pair<Long, String>(mediaId, newPathString);
        } catch (IOException e) {
            throw new Error(e);
        }
    }
  
    private long storeMediaBasic(MediaBasic mediaBasic) {
        ContentValues cv = new ContentValues();
        mediaBasic.storeToContentValues(cv);
        Uri uri = getContentResolver().insert(MediaBasicColumns.CONTENT_URI, cv);
        mediaBasic.id = Long.parseLong(uri.getLastPathSegment());
        return mediaBasic.id;
    }
    
    /*
     * Raw media message contains GeoLoc and vCard messages.
     */
    private Pair<Long, String> storeOutgoingRawMediaMessage(long accountId, String data, int type) {
        Pair<Long, String> resultPair = storeContentToMediaFile(data, type);
        long messageId = Constants.INVALID;

        MessageContent messageContent = new MessageContent();
        messageContent.accountId = accountId;
        messageContent.mediaType = type;
        messageContent.timestamp = Utils.currentTimestamp();
        messageContent.createTime = messageContent.timestamp;
        messageContent.publicAccountUuid = PublicAccount.queryAccountUuid(PAServiceImpl.this, accountId);
        messageContent.direction = Constants.MESSAGE_DIRECTION_OUTGOING;
        messageContent.status = Constants.MESSAGE_STATUS_TO_SEND;
        messageContent.forwardable = Constants.MESSAGE_FORWARDABLE_YES;
        messageContent.text = data;
        messageContent.mediaId = resultPair.value1;
        messageContent.mediaPath = resultPair.value2;
        
        messageContent.generateSmsDigest(this);

        ContentValues cv = new ContentValues();
        cv.put(MessageColumns.SOURCE_TABLE, Constants.TABLE_FT);
        messageContent.storeToContentValues(cv);
        
        Uri uri = getContentResolver().insert(PAContract.MessageColumns.CONTENT_URI, cv);
        messageId = Long.parseLong(uri.getLastPathSegment());

        return new Pair<Long, String>(messageId, messageContent.mediaPath);
    }
    
    private void resendMessageInternal(long token, final long messageId) throws JoynServiceException {
        Cursor c = null;
        try {
            Uri uri = MessageColumns.CONTENT_URI
                    .buildUpon()
                    .appendQueryParameter(
                            PAContract.MESSAGES_PARAM_INCLUDING_SYSTEM,
                            Integer.toString(Constants.IS_SYSTEM_YES))
                    .build();
            
            c = getContentResolver().query(
                    uri,
                    new String[] {
                        MessageColumns.SOURCE_ID,
                        MessageColumns.SOURCE_TABLE,
                        MessageColumns.ACCOUNT_ID,
                    },
                    MessageColumns._ID + "=?",
                    new String[] {Long.toString(messageId)},
                    null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                String sourceId = c.getString(c.getColumnIndexOrThrow(MessageColumns.SOURCE_ID));
                int sourceTable = c.getInt(c.getColumnIndexOrThrow(MessageColumns.SOURCE_TABLE));
                long accountId = c.getLong(c.getColumnIndexOrThrow(MessageColumns.ACCOUNT_ID));
                ContentValues cv = new ContentValues();
                cv.put(MessageColumns.STATUS, Constants.MESSAGE_STATUS_TO_SEND);
                getContentResolver().update(
                        MessageColumns.CONTENT_URI,
                        cv,
                        MessageColumns._ID + "=?",
                        new String[]{Long.toString(messageId)});
                if (sourceTable == Constants.TABLE_MESSAGE) {
                    if (mRcseChatService != null && 
                        mRcseChatService.isServiceConnected() &&
                        mRcseChatService.isServiceRegistered()) {
                        PublicAccountChat chat = getOrInitChat(accountId, token);
                        if (chat != null) {
                            Log.d(TAG, "Resend message: " + messageId);
                            chat.resendMessage(sourceId);
                        } else {
                            Log.e(TAG, "Failed to resend message: " + messageId);
                        }
                    } else {
                        Log.d(TAG, "Resend message when service is not ready. Ignore.");
                    }
                } else if (sourceTable == Constants.TABLE_FT) {
                    if (mRcseFTService != null && 
                        mRcseFTService.isServiceConnected() &&
                        mRcseFTService.isServiceRegistered()) {
                        FTListener listener = new FTListener(messageId, token);
                        final FileTransfer newFT = mRcseFTService.resumeFileTransfer(sourceId, listener);
                        String newTransferId = newFT.getTransferId();
                        Log.d(TAG, "New transfer ID is " + newTransferId);
                        listener.transferId = newTransferId;
                        final MessageRecoverRecord record = mMessageRecoverRecords.get(sourceId);
                        if (record != null) {
                            record.startTimestamp = Constants.INVALID;
                            if (!sourceId.equals(newTransferId)) {
                                mMessageRecoverRecords.remove(sourceId);
                                mMessageRecoverRecords.put(newTransferId, record);

                                // update provider
                                ContentValues values = new ContentValues();
                                values.put(MessageColumns.SOURCE_ID, newTransferId);
                                getContentResolver().update(
                                        MessageColumns.CONTENT_URI,
                                        values,
                                        MessageColumns.SOURCE_ID + "=? AND " + MessageColumns.SOURCE_TABLE + "=?",
                                        new String[] {
                                            sourceId,
                                            Integer.toString(Constants.TABLE_FT),
                                        });
                            }
                        }

                    } else {
                        Log.d(TAG, "Resend FT when service is not ready. Ignore.");
                    }
                } else {
                    Log.e(TAG, "Invalid source table: " + sourceTable);
                }
            } else {
                Log.e(TAG, "Invalid messageId: " + messageId);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }

    }
    
    /**
     * Store media message into message table as non-system message.
     * @param accountId receiver's account id
     * @param path media file path
     * @param type media type
     * @return message ID
     */
    private long storeOutgoingMediaMessage(long accountId, String path, String thumbPath, int type, String duration) {
        long messageId = Constants.INVALID;

        // copy file
        long mediaId = storeMediaFile(path, type, true, null);
        long thumbId = TextUtils.isEmpty(thumbPath) ?
                       Constants.INVALID :
                       storeMediaFile(thumbPath, Constants.MEDIA_TYPE_PICTURE, true, null);
        File file = new File(path);
        
        MessageContent messageContent = new MessageContent();
        messageContent.accountId = accountId;
        messageContent.basicMedia = new MediaBasic();
        MediaBasic mb = messageContent.basicMedia;
        mb.accountId = accountId;
        String extensionString = MediaFolder.extractExtension(file.getName());

        switch(type) {
        case Constants.MEDIA_TYPE_PICTURE:
            mb.fileType = "image";
            if (extensionString.equalsIgnoreCase(".jpeg") ||
                extensionString.equalsIgnoreCase(".jpg")) {
                mb.fileType += "/jpeg";
            } else if (extensionString.equalsIgnoreCase(".png")) {
                mb.fileType += "/png";
            } else if (extensionString.equalsIgnoreCase(".bmp")) {
                mb.fileType += "/bmp";
            } else if (extensionString.equalsIgnoreCase(".gif")) {
                mb.fileType += "/gif";
            }
            break;
        case Constants.MEDIA_TYPE_AUDIO:
            mb.fileType = "audio";
            if (extensionString.equalsIgnoreCase(".mp3")) {
                mb.fileType += "/mp3";
            } else if (extensionString.equalsIgnoreCase(".wav")) {
                mb.fileType += "/wav";
            } else if (extensionString.equalsIgnoreCase(".amr")) {
                mb.fileType += "/amr";
            }
            mb.duration = duration;
            break;
        case Constants.MEDIA_TYPE_VIDEO:
            mb.fileType = "video";
            if (extensionString.equalsIgnoreCase(".3gp")) {
                mb.fileType += "/3gpp";
            } else if (extensionString.equalsIgnoreCase(".mp4") ||
                       extensionString.equalsIgnoreCase(".mp4a") ||
                       extensionString.equalsIgnoreCase(".mpeg4")) {
                mb.fileType += "/mp4";
            } else if (extensionString.equalsIgnoreCase(".mpg") ||
                       extensionString.equalsIgnoreCase(".mpeg")) {
                mb.fileType += "/mpeg";
            }
            mb.duration = duration;
            break;
        default:
            throw new Error("Invalid type");
        }

        mb.title = file.getName();
        mb.fileSize = Long.toString(file.length());
        mb.createTime = file.lastModified() / 1000 * 1000;
        mb.publicAccountUuid = PublicAccount.queryAccountUuid(PAServiceImpl.this, accountId);
        mb.originalId = mediaId;
        mb.thumbnailId = thumbId;
        
        messageContent.mediaType = type;
        messageContent.createTime = Utils.currentTimestamp();
        messageContent.timestamp = messageContent.createTime;
        messageContent.publicAccountUuid = mb.publicAccountUuid;
        messageContent.direction = Constants.MESSAGE_DIRECTION_OUTGOING;
        messageContent.status = Constants.MESSAGE_STATUS_TO_SEND;
        messageContent.forwardable = Constants.MESSAGE_FORWARDABLE_YES;
        
        storeMediaBasic(mb);
        
        messageContent.generateSmsDigest(this);
        ContentValues cv = new ContentValues();
        messageContent.sourceTable = Constants.TABLE_FT;
        messageContent.storeToContentValues(cv);
        
        Uri uri = getContentResolver().insert(PAContract.MessageColumns.CONTENT_URI, cv);
        messageId = Long.parseLong(uri.getLastPathSegment());
        
        if (PlatformManager.getInstance().supportCcs()) {
            updateCcsMessageProvider(messageContent);
        }
        return messageId;
    }
    
    private PublicAccountChat getOrInitChat(final long accountId, final long token) {
        final String uuid = PublicAccount.queryAccountUuid(this, accountId);
        if (uuid == null) {
            // FIXME should we be more robust?
            throw new Error("Invalid account ID " + accountId);
        }
        PublicAccountChat chat = mChats.get(Constants.SIP_PREFIX + uuid);
        if (chat == null) {
            try {
                chat = mRcseChatService.initPublicAccountChat(Constants.SIP_PREFIX + uuid, new PAChatListener(token, uuid));
            } catch (JoynContactFormatException e) {
                e.printStackTrace();
                return null;
            } catch (JoynServiceException e) {
                e.printStackTrace();
                return null;
            }
            if (chat == null) {
                throw new Error("Failed to create chat");
            }
            mChats.put(uuid, chat);
        }
        return chat;
    }
    
    private FileTransfer getFT(final long messageId, final long token) throws JoynServiceException {
        FileTransfer result = null;
        Cursor c = null;
        try {
            c = getContentResolver().query(
                    MessageColumns.CONTENT_URI,
                    new String[]{
                        MessageColumns.SOURCE_ID,
                        MessageColumns.SOURCE_TABLE,
                    },
                    MessageColumns._ID + "=?",
                    new String[]{Long.toString(messageId)},
                    null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                int sourceTable = c.getInt(c.getColumnIndexOrThrow(MessageColumns.SOURCE_TABLE));
                if (sourceTable == Constants.TABLE_FT) {
                    String transferId = c.getString(c.getColumnIndexOrThrow(MessageColumns.SOURCE_ID));;
                    return mRcseFTService.getFileTransfer(transferId);
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return result;
    }
    
    /**
     * Update the state field in PAProvider if the message with messageId
     * exists. If the messageId in not stored in PAProvider, then nothing
     * will happen. This behavior is crucial to system message (message 
     * which will not be stored in PAProvider). 
     * 
     * @param messageId message id from RCSe
     */
    private void updateMessageStateFromRCSe(String messageId) {
        ContentResolver cr = getContentResolver();
        Cursor c = null;
        try {
            c = cr.query(
                    RcseProviderContract.MessageColumns.CONTENT_URI,
                    new String[] {RcseProviderContract.MessageColumns.MESSAGE_STATUS},
                    RcseProviderContract.MessageColumns.MESSAGE_ID + "=?",
                    new String[]{messageId},
                    null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                final int state = c.getInt(c.getColumnIndexOrThrow(RcseProviderContract.MessageColumns.MESSAGE_STATUS));
                ContentValues cv = new ContentValues();
                cv.put(PAContract.MessageColumns.STATUS, state);
                cr.update(
                        PAContract.MessageColumns.CONTENT_URI,
                        cv,
                        PAContract.MessageColumns.SOURCE_ID + "=? AND " + PAContract.MessageColumns.SOURCE_TABLE + "=?",
                        new String[]{messageId, Integer.toString(Constants.TABLE_MESSAGE)});
                if (PlatformManager.getInstance().supportCcs()) {
                    updateCcsMessageStatus(messageId, Constants.TABLE_MESSAGE, state);
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }
    
    private void deleteMessage(long messageId) {
        ContentResolver cr = getContentResolver();
        cr.delete(
                PAContract.MessageColumns.CONTENT_URI,
                PAContract.MessageColumns._ID + "=?",
                new String[]{Long.toString(messageId)});
        // TODO remove all related resources
    }
    
    private void deleteMessageInRCSe(String messageId) {
        ContentResolver cr = getContentResolver();
        cr.delete(
                RcseProviderContract.MessageColumns.CONTENT_URI,
                RcseProviderContract.MessageColumns.MESSAGE_ID + "=?",
                new String[]{messageId});
    }
    
    private class PAChatListener extends ChatListener {
        private static final String TAG = Constants.TAG_PREFIX + "PAChatListener";
        private long mToken;
        private String mAccountUuid;
        private long mAccountId = Constants.INVALID;
        
        public PAChatListener(long token, String uuid) {
            mToken = token;
            mAccountUuid = uuid;
        }
        
        private long getAccountId() {
            if (mAccountId == Constants.INVALID) {
                mAccountId = PublicAccount.queryAccountId(PAServiceImpl.this, mAccountUuid, false);
            }
            return mAccountId;
        }
        
        @Override
        public void onReportMessageFailed(String msgId) {
            Log.d(TAG, "onReportMessageFailed(" + msgId + ")");
            reportMessageFailed(msgId);
        }

        @Override
        public void onReportFailedMessage(String msgId, int errtype, String statusCode) {
            Log.d(TAG, "onReportFailedMessage(" + msgId + "," + errtype + ","  + statusCode + ")");
            reportMessageFailed(msgId);
        }

        private void reportMessageFailed(final String msgId) {
            Log.d(TAG, "reportMessageFailed:" + msgId);
            final MessageRecoverRecord record = mMessageRecoverRecords.get(msgId);
            Log.d(TAG, "Get MRR for " + msgId);
            if (record != null) {
                boolean shouldRetry = false;
                if (record.startTimestamp == Constants.INVALID) {
                    record.startTimestamp = System.currentTimeMillis();
                    shouldRetry = true;
                } else {
                    final long currentTimestamp = System.currentTimeMillis();
                    long timespan = currentTimestamp - record.startTimestamp;
                    if (timespan > 0 && timespan < MessageRecoverRecord.RECOVER_TIMEOUT) {
                        shouldRetry = true;
                    }
                }
                if (shouldRetry) {
                    // try to recover in 5s
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Log.d(TAG, "Auto recover message: " + msgId);
                                resendMessageInternal(record.clientToken, record.messageId);
                            } catch (JoynServiceException e) {
                                e.printStackTrace();
                                reportMessageFailureInternal(msgId);
                            }
                        }
                    },
                    MessageRecoverRecord.RECOVER_INTERVAL);
                } else {
                    reportMessageFailureInternal(msgId);
                }
            } else {
                Log.w(TAG, "This should not happen. Report failure directly anyway.");
                reportMessageFailureInternal(msgId);
            }
        }
        
        private void reportMessageFailureInternal(String msgId) {
            mMessageRecoverRecords.remove(msgId);
            try {
                updateMessageStateFromRCSe(msgId);
                long messageId = getMessageIdFromSourceId(msgId);
                if (mToken == SERVICE_TOKEN) {
                    for (IPAServiceCallback listener : mClientListeners.values()) {
                        listener.onReportMessageFailed(messageId);
                    }
                } else {
                    mClientListeners.get(mToken).onReportMessageFailed(messageId);
                }
            } catch (RemoteException e) {
                throw new Error(e);
            }
            
        }

        @Override
        public void onReportMessageDisplayed(String msgId) {
            Log.d(TAG, "onReportMessageDisplayed(" + msgId + ")");
            try {
                updateMessageStateFromRCSe(msgId);
                long messageId = getMessageIdFromSourceId(msgId);
                if (mToken == SERVICE_TOKEN) {
                    for (IPAServiceCallback listener : mClientListeners.values()) {
                        listener.onReportMessageDisplayed(messageId);
                    }
                } else {
                    mClientListeners.get(mToken).onReportMessageDisplayed(messageId);
                }
            } catch (RemoteException e) {
                throw new Error(e);
            }
        }
        
        @Override
        public void onReportMessageDelivered(String msgId) {
            Log.d(TAG, "onReportMessageDelivered(" + msgId + ")");
            reportMessageDelivered(msgId);
        }

        @Override
        public void onReportDeliveredMessage(String msgId) {
            Log.d(TAG, "onReportDeliveredMessage(" + msgId + ")");
            reportMessageDelivered(msgId);
        }

        @Override
        public void onReportSentMessage(String msgId) {
            Log.d(TAG, "onReportSentMessage(" + msgId + ")");
            reportMessageDelivered(msgId);
        }
        
        private void reportMessageDelivered(String msgId) {
            try {
                mMessageRecoverRecords.remove(msgId);
                long messageId = getMessageIdFromSourceId(msgId);
                if (mPendingSystemMessages.contains(msgId)) {
                    deleteMessageInRCSe(msgId);
                    deleteMessage(messageId);
                    mPendingSystemMessages.remove(msgId);
                } else {
                    updateMessageStateFromRCSe(msgId);
                }
                if (mToken == SERVICE_TOKEN) {
                    for (IPAServiceCallback listener : mClientListeners.values()) {
                        listener.onReportMessageDelivered(messageId);
                    }
                } else {
                    mClientListeners.get(mToken).onReportMessageDelivered(messageId);
                }
            } catch (RemoteException e) {
                throw new Error(e);
            }
        }
        
        @Override
        public void onNewMessage(ChatMessage message) {
            Log.d(TAG, "onNewMessage(" + message.getId() + ")");
            try {
                if (mAccountUuid.equals(message.getContact())) {
                    long messageId = copyMessageFromChatProvider(message.getId(), false, getAccountId());
                    if (mToken == SERVICE_TOKEN) {
                        for (IPAServiceCallback listener : mClientListeners.values()) {
                            listener.onNewMessage(getAccountId(), messageId);
                        }
                    } else {
                        mClientListeners.get(mToken).onNewMessage(getAccountId(), messageId);
                    }
                }
            } catch (RemoteException e) {
                throw new Error(e);
            }
        }
        
        @Override
        public void onComposingEvent(boolean status) {
            Log.d(TAG, "onComposingEvent(" + status + ")");
            try {
                if (mToken == SERVICE_TOKEN) {
                    for (IPAServiceCallback listener : mClientListeners.values()) {
                        listener.onComposingEvent(getAccountId(), status);
                    }
                } else {
                    mClientListeners.get(mToken).onComposingEvent(getAccountId(), status);
                }
            } catch (RemoteException e) {
                throw new Error(e);
            }
        }

        @Override
        public void onNewBurnMessageArrived(ChatMessage message) {
            // do nothing
            Log.d(TAG, "onNewBurnMessageArrived(" + message.getId() + ")");
            throw new Error("Not Supported");
        }

        @Override
        public void onNewGeoloc(GeolocMessage message) {
            Log.d(TAG, "onNewGeoloc(" + message.getId() + ")");
            // GeoLoc messages are pushed from public account server as XML message.
            throw new Error("Not Supported");
        }
    }
    
    private long copyMessageFromChatProvider(String messageId, boolean system, long accountId) {
        ContentResolver cr = getContentResolver();
        Cursor c = null;
        try {
            c = cr.query(
                    RcseProviderContract.MessageColumns.CONTENT_URI,
                    RcseProviderContract.MESSAGE_FULL_PROJECTION,
                    RcseProviderContract.MessageColumns.MESSAGE_ID + "=?",
                    new String[]{messageId},
                    null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                MessageContent message = MessageContent.buildFromRcseMessageProviderCursor(this, c);
                if (TextUtils.isEmpty(message.smsDigest)) {
                    message.generateSmsDigest(PAServiceImpl.this);
                }
                ContentValues cv = new ContentValues();
                message.storeToContentValues(cv);
                // store extra resources
                switch (message.mediaType) {
                case Constants.MEDIA_TYPE_PICTURE:
                case Constants.MEDIA_TYPE_AUDIO:
                case Constants.MEDIA_TYPE_VIDEO:
                    message.basicMedia.id = storeMediaBasicToProvider(cr, message.mediaType, message.basicMedia);
                    cv.put(MessageColumns.DATA1, message.basicMedia.id);
                    break;
                case Constants.MEDIA_TYPE_GEOLOC:
                case Constants.MEDIA_TYPE_VCARD:
                    message.mediaId = storeContentToMediaFile(message.text, message.mediaType).value1;
                    cv.put(MessageColumns.DATA1, message.mediaId);
                    break;
                case Constants.MEDIA_TYPE_SINGLE_ARTICLE:
                case Constants.MEDIA_TYPE_MULTIPLE_ARTICLE:
                    for (int i = 0; i < message.article.size(); ++i) {
                        MediaArticle ma = message.article.get(i);
                        ma.id = storeMediaArticlesToProvider(cr, ma);
                        cv.put(PAContract.MESSAGE_DATA_COLUMN_LIST[i], ma.id);
                    }
                    break;
                default:
                    // do nothing
                    break;
                }
                if (system) {
                    cv.put(MessageColumns.SYSTEM, Constants.IS_SYSTEM_YES);
                }
                if (accountId != Constants.INVALID) {
                    cv.put(MessageColumns.ACCOUNT_ID, accountId);
                }
                Uri uri = cr.insert(MessageColumns.CONTENT_URI, cv);
                message.id = Long.parseLong(uri.getLastPathSegment());
                // sync to ccs provider
                if (PlatformManager.getInstance().supportCcs()) {
                    updateCcsMessageProvider(message);
                }
                return message.id;
            } else {
                throw new Error("No message found: " + messageId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return Constants.INVALID;
    }
    
    private long copyMessageFromFTProvider(String ftId /* accountId */) {
        ContentResolver cr = getContentResolver();
        Cursor c = null;
        try {
            c = cr.query(
                    RcseProviderContract.FileTransferColumns.CONTENT_URI,
                    RcseProviderContract.FILE_TRANSFER_FULL_PROJECTION,
                    RcseProviderContract.FileTransferColumns.FT_ID + "=?",
                    new String[]{ftId},
                    null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                MessageContent message = MessageContent.buildFromRcseFTProviderCursor(this, c);
                ContentValues cv = new ContentValues();
                message.storeToContentValues(cv);
                // store extra resources
                long mediaId = Constants.INVALID;
                switch (message.mediaType) {
                case Constants.MEDIA_TYPE_PICTURE:
                case Constants.MEDIA_TYPE_AUDIO:
                case Constants.MEDIA_TYPE_VIDEO:
                    message.basicMedia.id = storeMediaBasicToProvider(cr, message.mediaType, message.basicMedia);
                    cv.put(MessageColumns.DATA1, mediaId);
                    break;
                case Constants.MEDIA_TYPE_SINGLE_ARTICLE:
                case Constants.MEDIA_TYPE_MULTIPLE_ARTICLE:
                    for (int i = 0; i < message.article.size(); ++i) {
                        MediaArticle ma = message.article.get(i);
                        ma.id = storeMediaArticlesToProvider(cr, ma);
                        cv.put(PAContract.MESSAGE_DATA_COLUMN_LIST[i], ma.id);
                    }
                    break;
                default:
                    // do nothing
                    break;
                }
                Uri uri = cr.insert(MessageColumns.CONTENT_URI, cv);
                return Long.parseLong(uri.getLastPathSegment());
            } else {
                throw new Error("No file transfer found: " + ftId);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }
    
    private long storeMediaBasicToProvider(ContentResolver cr, int mediaType, MediaBasic media) {
        ContentValues cv = null;
        if (media.originalUrl != null) {
            Cursor c = null;
            cv = new ContentValues();
            media.storeOriginalToContentValues(cv, mediaType);
            try {
                c = getContentResolver().query(
                        MediaColumns.CONTENT_URI,
                        new String[]{MediaColumns._ID},
                        MediaColumns.URL + "=?",
                        new String[] {media.originalUrl},
                        null);
                if (c != null && c.getCount() > 0) {
                    c.moveToFirst();
                    media.originalId = c.getLong(c.getColumnIndexOrThrow(MediaColumns._ID));
                    int result = getContentResolver().update(
                        MediaColumns.CONTENT_URI,
                        cv,
                        MediaColumns.URL + "=?",
                        new String[] {media.originalUrl});
                    if (result != 1) {
                        Log.e(TAG, "Failed to update media item with URL: " + media.originalId);
                    }
                } else {
                    Uri uri = cr.insert(MediaColumns.CONTENT_URI, cv);
                    media.originalId = Long.parseLong(uri.getLastPathSegment());
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new Error(e);
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }

        if (media.thumbnailUrl != null) {
            Cursor c = null;
            cv = new ContentValues();
            media.storeThumbnailToContentValues(cv, Constants.MEDIA_TYPE_PICTURE);
            try {
                c = getContentResolver().query(
                        MediaColumns.CONTENT_URI,
                        new String[]{MediaColumns._ID},
                        MediaColumns.URL + "=?",
                        new String[] {media.thumbnailUrl},
                        null);
                if (c != null && c.getCount() > 0) {
                    c.moveToFirst();
                    media.thumbnailId = c.getLong(c.getColumnIndexOrThrow(MediaColumns._ID));
                    int result = getContentResolver().update(
                        MediaColumns.CONTENT_URI,
                        cv,
                        MediaColumns.URL + "=?",
                        new String[] {media.thumbnailUrl});
                    if (result != 1) {
                        Log.e(TAG, "Failed to update media item with URL: " + media.thumbnailUrl);
                    }
                } else {
                    Uri uri = cr.insert(MediaColumns.CONTENT_URI, cv);
                    media.thumbnailId = Long.parseLong(uri.getLastPathSegment());
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }
        
        cv = new ContentValues();
        media.storeToContentValues(cv);
        Uri uri = cr.insert(MediaBasicColumns.CONTENT_URI, cv);
        media.id = Long.parseLong(uri.getLastPathSegment());
        return media.id;
    }
    
    private long storeMediaArticlesToProvider(ContentResolver cr, MediaArticle article) {
        ContentValues cv = null;
        if (article.originalUrl != null) {
            Cursor c = null;
            cv = new ContentValues();
            cv.put(MediaColumns.TYPE, Constants.MEDIA_TYPE_PICTURE);
            cv.put(MediaColumns.URL, article.originalUrl);
            try {
                c = getContentResolver().query(
                        MediaColumns.CONTENT_URI,
                        new String[]{MediaColumns._ID},
                        MediaColumns.URL + "=?",
                        new String[] {article.originalUrl},
                        null);
                if (c != null && c.getCount() > 0) {
                    c.moveToFirst();
                    article.originalId = c.getLong(c.getColumnIndexOrThrow(MediaColumns._ID));
                    /* no need to update */
                } else {
                    cv.put(MediaColumns.TIMESTAMP, Utils.currentTimestamp());
                    Uri uri = cr.insert(MediaColumns.CONTENT_URI, cv);
                    article.originalId = Long.parseLong(uri.getLastPathSegment());
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new Error(e);
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }
        
        if (article.thumbnailUrl != null) {
            Cursor c = null;
            cv = new ContentValues();
            cv.put(MediaColumns.TYPE, Constants.MEDIA_TYPE_PICTURE);
            cv.put(MediaColumns.URL, article.thumbnailUrl);
            try {
                c = getContentResolver().query(
                        MediaColumns.CONTENT_URI,
                        new String[]{MediaColumns._ID},
                        MediaColumns.URL + "=?",
                        new String[] {article.thumbnailUrl},
                        null);
                if (c != null && c.getCount() > 0) {
                    c.moveToFirst();
                    article.thumbnailId = c.getLong(c.getColumnIndexOrThrow(MediaColumns._ID));
                    /* no need to update */
                } else {
                    cv.put(MediaColumns.TIMESTAMP, Utils.currentTimestamp());
                    Uri uri = cr.insert(MediaColumns.CONTENT_URI, cv);
                    article.thumbnailId = Long.parseLong(uri.getLastPathSegment());
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new Error(e);
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }
        
        cv = new ContentValues();
        article.storeToContentValues(cv);
        
        Cursor c = null;
        try {
            c = getContentResolver().query(
                    MediaArticleColumns.CONTENT_URI,
                    new String[]{MediaArticleColumns._ID},
                    MediaArticleColumns.MEDIA_UUID + "=?",
                    new String[] {article.mediaUuid},
                    null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                article.id = c.getLong(c.getColumnIndexOrThrow(MediaArticleColumns._ID));
                /* no need to update */
            } else {
                Uri uri = cr.insert(MediaArticleColumns.CONTENT_URI, cv);
                article.id = Long.parseLong(uri.getLastPathSegment());
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return article.id;
    }
    
    private long getLogoId(String logoUrl) {
        long logoId = Constants.INVALID;
        ContentResolver cr = getContentResolver();
        Cursor c = null;
        try {
            c = cr.query(
                    MediaColumns.CONTENT_URI,
                    new String[]{ MediaColumns._ID },
                    MediaColumns.URL + "=? AND " + MediaColumns.TYPE + "=?",
                    new String[]{
                        logoUrl,
                        Integer.toString(Constants.MEDIA_TYPE_PICTURE)
                    },
                    null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                logoId = c.getLong(c.getColumnIndexOrThrow(MediaColumns._ID));
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        
        return logoId;
    }
    
    @Override
    public void onCreate() {
        mHandler = new Handler();
        mMessageRecoverRecords = new ConcurrentHashMap<String, PAServiceImpl.MessageRecoverRecord>();
        mClientListeners = new ConcurrentHashMap<Long, IPAServiceCallback>();
        mChats = new ConcurrentHashMap<String, PublicAccountChat>();
//        mFTs = new ConcurrentHashMap<String, FileTransfer>();
        mRcseChatService = new ChatService(this, new JoynServiceListener() {
            
            @Override
            public void onServiceDisconnected(int reason) {
                Log.d(TAG, "mRcseChatService is disconnected");
                notifyListenersOfServiceDisconnectedEvent(reason);
            }
            
            @Override
            public void onServiceConnected() {
                Log.d(TAG, "mRcseChatService is connected");
                try {
                    mRcseChatService.addServiceRegistrationListener(new JoynServiceRegistrationListener() {
                        
                        @Override
                        public void onServiceUnregistered() {
                            Log.d(TAG, "mRcseChatService is unregistered");
                            notifyListenersOfServiceUnregisteredEvent();
                        }
                        
                        @Override
                        public void onServiceRegistered() {
                            try {
                                Log.d(TAG, "mRcseChatService is registered");
                                if (mRcseFTService.isServiceConnected() && mRcseFTService.isServiceRegistered()) {
                                    Log.d(TAG, "mRcseFTService is also registered");
                                    notifyListenersOfServiceRegisteredEvent();
                                }
                            } catch (JoynServiceException e) {
                                throw new Error(e);
                            }
                        }
                    });
                } catch (JoynServiceException e) {
                    throw new Error(e);
                }

                // We only care about public account chat
                NewChatListener listener = new NewChatListener() {
                    
                    @Override
                    public void onNewSingleChat(String chatId, ChatMessage message) {
                        // do nothing
                    }
                    
                    @Override
                    public void onNewPublicAccountChat(String chatId, ChatMessage message) {
                        try {
                            PublicAccountChat chat = (PublicAccountChat) mRcseChatService.getPublicAccountChat(chatId);
                            String accountUuid = chat.getRemoteContact();
                            long accountId = PublicAccount.queryAccountId(PAServiceImpl.this, accountUuid, true);
                            if (accountId != Constants.INVALID) {
                                mChats.put(Constants.SIP_PREFIX + accountUuid, chat);
                                chat.addEventListener(new PAChatListener(SERVICE_TOKEN, null));
                                try {
                                    long messageId = copyMessageFromChatProvider(message.getId(), false, accountId);
                                    for (IPAServiceCallback listener : mClientListeners.values()) {
                                        listener.onNewMessage(accountId, messageId);
                                    }
                                } catch (RemoteException e) {
                                    throw new Error(e);
                                }
                            }
                        } catch (JoynServiceException e) {
                            throw new Error(e);
                        }
                    }
                    
                    @Override
                    public void onNewGroupChat(String chatId) {
                        // do nothing
                    }
                };
                
                
                try {
                    mRcseChatService.addEventListener(listener);
                } catch (JoynServiceException e) {
                    throw new Error(e);
                }
                
                if (mRcseFTService.isServiceConnected()) {
                    Log.d(TAG, "mRcseFTService is also connected");
                    notifyListenersOfServiceConnectedEvent();
                }
            }
        });
        
        mRcseFTService = new FileTransferService(this, new JoynServiceListener() {
            
            @Override
            public void onServiceDisconnected(int reason) {
                Log.d(TAG, "mRcseFTService is disconnected");
                notifyListenersOfServiceDisconnectedEvent(reason);
            }
            
            @Override
            public void onServiceConnected() {
                Log.d(TAG, "mRcseFTService is connected");
                try {
                    mRcseFTService.addServiceRegistrationListener(new JoynServiceRegistrationListener() {
                        
                        @Override
                        public void onServiceUnregistered() {
                            Log.d(TAG, "mRcseFTService is unregistered");
                            notifyListenersOfServiceUnregisteredEvent();
                        }
                        
                        @Override
                        public void onServiceRegistered() {
                            Log.d(TAG, "mRcseFTService is registered");
                            try {
                                if (mRcseChatService.isServiceConnected() && mRcseChatService.isServiceRegistered()) {
                                    Log.d(TAG, "mRcseChatService is also registered");
                                    notifyListenersOfServiceRegisteredEvent();
                                }
                            } catch (JoynServiceException e) {
                                throw new Error(e);
                            }
                        }
                    });
                } catch (JoynServiceException e) {
                    throw new Error(e);
                }

                NewFileTransferListener listener = new NewFileTransferListener() {
                    
                    @Override
                    public void onReportFileDisplayed(String transferId) {
                        // do nothing
                    }
                    
                    @Override
                    public void onReportFileDelivered(String transferId) {
                        // do nothing
                    }
                    
                    @Override
                    public void onNewFileTransfer(final String transferId) {
                        Log.d(TAG, "Incoming FT: " + transferId);
                        try {
                            final FileTransfer ft = mRcseFTService.getFileTransfer(transferId);
                            String accountUuid = ft.getRemoteContact();
                            final long accountId = PublicAccount.queryAccountId(PAServiceImpl.this, accountUuid, true);
                            String type = ft.getFileType();
                            final int mediaType;
                            if (type.equalsIgnoreCase("application/xml")) {
                                mediaType = Constants.MEDIA_TYPE_PICTURE;
                            } else {
                                mediaType = Constants.INVALID;
                            }
                            
                            if (accountId != Constants.INVALID && mediaType != Constants.INVALID) {
                                ft.addEventListener(new FileTransferListener() {
                                    
                                    @Override
                                    public void onTransferStarted() {
                                        // do nothing
                                    }
                                    
                                    @Override
                                    public void onTransferProgress(long currentSize, long totalSize) {
                                        // do nothing
                                    }
                                    
                                    @Override
                                    public void onTransferError(int error) {
                                        // do nothing
                                    }
                                    
                                    @Override
                                    public void onTransferAborted() {
                                        // do nothing
                                    }
                                    
                                    @Override
                                    public void onFileTransferred(String filename) {
                                        long messageId = copyMessageFromFTProvider(transferId);
                                        // TODO put a notification on the notification bar
    //                                    postNewMessageNotification(messageId);
                                        
                                    }

                                    @Override
                                    public void onTransferPaused() {
                                        // do nothing
                                    }

                                    @Override
                                    public void onTransferResumed(String arg0, String arg1) {
                                        // do nothing
                                    }
                                });
                                ft.acceptInvitation();
                            }
                        } catch (JoynServiceException e) {
                            throw new Error(e);
                        }
                    }

                    @Override
                    public void onNewBurnFileTransfer(String arg0, boolean arg1, String arg2, String arg3)
                            throws RemoteException {
                        // TODO Auto-generated method stub
                        
                    }

                    @Override
                    public void onFileDeliveredReport(String arg0, String arg1) {
                        // TODO Auto-generated method stub
                        
                    }

                    @Override
                    public void onFileDisplayedReport(String arg0, String arg1) {
                        // TODO Auto-generated method stub
                        
                    }

                    @Override
                    public void onNewBurnFileTransfer(String arg0, Boolean arg1, String arg2, String arg3) {
                        // do nothing
                    }

                    @Override
                    public void onNewFileTransferReceived(String arg0, boolean arg1, boolean arg2, String arg3,
                            String arg4, int arg5) {
                        // TODO Auto-generated method stub
                        
                    }

                    @Override
                    public void onNewPublicAccountChatFile(String arg0, boolean arg1, boolean arg2, String arg3,
                            String arg4) {
                        // TODO Auto-generated method stub
                        
                    }
                };
                // register listener
                try {
                    mRcseFTService.addNewFileTransferListener(listener);
                } catch (JoynServiceException e) {
                    throw new Error(e);
                }
                if (mRcseChatService.isServiceConnected()) {
                    Log.d(TAG, "mRcseChatService is also connected");
                    notifyListenersOfServiceConnectedEvent();
                }
            }
        });

        PlatformManager pm = PlatformManager.getInstance();
        mClient = new PAMClient(
                pm.getTransmitter(this),
                this);
        mExecutorService = Executors.newFixedThreadPool(15);
        mWorkingTasks = new ConcurrentHashMap<Long, CancellableTask>();
    }
    
    private void notifyListenersOfServiceConnectedEvent() {
        Log.d(TAG, "notifyListenersOfServiceConnectedEvent");
        for (IPAServiceCallback listener : mClientListeners.values()) {
            try {
                listener.onServiceConnected();
            } catch (RemoteException e) {
                // ignore
                e.printStackTrace();
            }
        }
        try {
            if (mRcseChatService.isServiceRegistered() && mRcseFTService.isServiceRegistered()) {
                Log.d(TAG, "RCSe Chat and FT are both already registered. Send instant notification.");
                notifyListenersOfServiceRegisteredEvent();
            }
        } catch (JoynServiceException e) {
            throw new Error(e);
        }
    }
    
    private void notifyListenersOfServiceDisconnectedEvent(int reason) {
        for (IPAServiceCallback listener : mClientListeners.values()) {
            try {
                listener.onServiceDisconnected(reason);
            } catch (RemoteException e) {
                // ignore
                e.printStackTrace();
            }
        }
        if (!PlatformManager.getInstance().isRcsServiceActivated(this)) {
            ActivityManager ams = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.AppTask> tasks = ams.getAppTasks();
            for (ActivityManager.AppTask task : tasks) {
                task.finishAndRemoveTask();
            }
        }
    }
    
    private void notifyListenersOfServiceRegisteredEvent() {
        Log.d(TAG, "notifyListenersOfServiceRegisteredEvent");
        for (IPAServiceCallback listener : mClientListeners.values()) {
            try {
                listener.onServiceRegistered();
            } catch (RemoteException e) {
                // ignore
                e.printStackTrace();
            }
        }
        if (!isInitialized()) {
            getSubscribedInBackground();
        }
    }
    
    private void notifyListenersOfServiceUnregisteredEvent() {
        for (IPAServiceCallback listener : mClientListeners.values()) {
            try {
                listener.onServiceUnregistered();
            } catch (RemoteException e) {
                // ignore
                e.printStackTrace();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Check the intents and permissions.
        if (!mRcseChatService.isServiceConnected()) {
            mRcseChatService.connect();
        }
        if (!mRcseFTService.isServiceConnected()) {
            mRcseFTService.connect();
        }
        return new PASBinder();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_STICKY;
        }
        
        final String action = intent.getAction();
        
        if (PAMReceiver.ACTION_NEW_MESSAGE.equals(action)) {
            Bundle extras = intent.getExtras();
            Set<String> keys = extras.keySet();
            for (String key:keys) {
                Log.i(TAG, "Key: " + key);
            }
            String accountUuid = intent.getStringExtra(ACTION_KEY_ACCOUNT);
            accountUuid = Utils.extractUuidFromSipUri(accountUuid);
            ChatMessage message = (ChatMessage) intent.getParcelableExtra(ACTION_KEY_CHAT_MESSAGE);
            if (message == null) {
                Log.d(TAG, "Message is null. Ignore.");
                return START_STICKY;
            }
            if (!message.isPublicMessage()) {
                Log.d(TAG, "Not a public message. Ignore.");
                return START_STICKY;
            }
            Log.d(TAG, "Message content: " + message.getMessage());
            long accountId = PublicAccount.queryAccountId(PAServiceImpl.this, accountUuid, true);
            if (accountId != Constants.INVALID) {
                // Do NOT use chat and chat listener because RCS stack uses broadcast only.
                 
                try {
                    long messageId = copyMessageFromChatProvider(message.getId(), false, accountId);
                    for (IPAServiceCallback listener : mClientListeners.values()) {
                        listener.onNewMessage(accountId, messageId);
                    }
                } catch (RemoteException e) {
                    throw new Error(e);
                }
            } else {
                Log.d(TAG, "Cannot find UUID " + accountUuid + " in database. Ignore.");
                }
        } else if (PAMReceiver.ACTION_RCS_ACCOUNT_CHANGED.equals(action)) {
            // FIXME
            // 0. Cancel all tasks and tell clients the reason (this part should be done by each task)
            for (CancellableTask task : mWorkingTasks.values()) {
                task.cancel();
            }
            // 1. Ask clients to quit
            for (IPAServiceCallback listener : mClientListeners.values()) {
                try {
                    listener.onAccountChanged(PlatformManager.getInstance().getIdentity(this));
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException happened. Ignore and carry on.");
                    e.printStackTrace();
                }
            }
            // 2. Clean state
            // Reset PA
            Log.d(TAG, "Account Changed. Delete all data and exit.");
            deleteAllData();
            // relaunch in 5s
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            Intent i = new Intent(ACTION_RESTART);
            i.setClass(this, PAServiceImpl.class);
            PendingIntent pi = PendingIntent.getService(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5000, pi);

            Process.killProcessQuiet(Process.myPid());
        } else if (PAMReceiver.ACTION_JOYN_UP.equals(action)) {
            Log.d(TAG, "Joyn Service is up. We get up!");
            if (mRcseChatService != null && !mRcseChatService.isServiceConnected()) {
                mRcseChatService.connect();
            }
            if (mRcseFTService != null && !mRcseFTService.isServiceConnected()) {
                mRcseFTService.connect();
            }
            markSendingMessageAsFailed();

            return START_STICKY;
        } else if (ACTION_RESTART.equals(action)) {
            Log.d(TAG, "PAService is relaunched.");
            mRcseChatService.connect();
            mRcseFTService.connect();
            markSendingMessageAsFailed();

            return START_STICKY;
        }
        
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        if (mRcseChatService != null) {
            mRcseChatService.disconnect();
        }
        if (mRcseFTService != null) {
            mRcseFTService.disconnect();
        }
    }
    
    private void deleteAllData() {
        File dbFile = getDatabasePath(PAProvider.DATABASE_NAME);
        File dbJournalFile = getDatabasePath(PAProvider.DATABASE_NAME + "-journal");
        dbFile.delete();
        dbJournalFile.delete();
    }
    
    private long getMessageIdFromSourceId(String sourcdId) {
        Cursor c = null;
        try {
            c = getContentResolver().query(
                    Uri.parse(
                            MessageColumns.CONTENT_URI_STRING + "?" +
                            PAContract.MESSAGES_PARAM_INCLUDING_SYSTEM + "=" + Constants.IS_SYSTEM_YES),
                    new String[] {MessageColumns._ID},
                    MessageColumns.SOURCE_ID + "=?",
                    new String[]{sourcdId},
                    null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                return c.getLong(c.getColumnIndexOrThrow(MessageColumns._ID));
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return Constants.INVALID;
    }
    
    private boolean isInitialized() {
        boolean result = false;
        Cursor c = null;
        try {
            c = getContentResolver().query(StateColumns.CONTENT_URI, null, null, null, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                int value = c.getInt(c.getColumnIndexOrThrow(StateColumns.INITIALIZED));
                result = (value == Constants.INIT_YES);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return result;
    }
    
    private void setInitialized(boolean flag) {
        Log.d(TAG, "setInitialized(" + flag + ")");
        ContentValues cv = new ContentValues();
        cv.put(StateColumns.INITIALIZED, flag ? Constants.INIT_YES : Constants.INIT_NO);
        int result = getContentResolver().update(StateColumns.CONTENT_URI, cv, null, null);
        Log.d(TAG, "update result is " + result);
    }
    
    private void getSubscribedInBackground() {
        if (mIsGettingSubscribedList) {
            Log.d(TAG, "Duplicate getSubscribed. Ignore.");
            return;
        }
        mIsGettingSubscribedList = true;
        class GetSubscribedTask extends CancellableTask {
            private static final int BATCH_SIZE = 50;

            public GetSubscribedTask(long requestId, IPAServiceCallback callback) {
                super(requestId, callback, true);
            }

            @Override
            protected void doRun() {
                Log.d(TAG, "getSubscribedInBackground");
                int pageNumber = 1;
                List<PublicAccount> accounts = null;
                do {
                    try {
                        accounts = mClient.getSubscribedList(Constants.ORDER_BY_NAME, BATCH_SIZE, pageNumber);
                        // TODO test edge cases
                        for (PublicAccount account : accounts) {
                            Log.d(TAG, "Store account: " + account.uuid);
                            Log.d(TAG, "Subscription status: " + account.subscribeStatus);
                            Cursor c = null;
                            ContentResolver cr = getContentResolver();
                            long accountId;
                            try {
                                c = cr.query(
                                        AccountColumns.CONTENT_URI,
                                        new String[]{AccountColumns._ID},
                                        AccountColumns.UUID + "=?",
                                        new String[]{account.uuid},
                                        null);
                                if (c == null || c.getCount() == 0) {
                                    // insert
                                    long logoId = PublicAccount.insertLogoUrl(cr, account.logoUrl);
                                    Log.d(TAG, "Insert account logo url: " + account.logoId);
                                    ContentValues cv = PublicAccount.storeBasicInfoToContentValues(account);
                                    cv.remove(AccountColumns.LOGO_URL);
                                    cv.remove(AccountColumns.LOGO_PATH);
                                    cv.put(AccountColumns.LOGO_ID, logoId);
                                    Uri uri = cr.insert(AccountColumns.CONTENT_URI, cv);
                                    accountId = Long.parseLong(uri.getLastPathSegment());
                                    Log.d(TAG, "Insert account " + accountId + " in background");
                                } else {
                                    // update
                                    c.moveToFirst();
                                    accountId = c.getLong(c.getColumnIndexOrThrow(AccountColumns._ID));
                                    long logoId = getLogoId(account.logoUrl);
                                    if (logoId == Constants.INVALID) {
                                        Log.d(TAG, "Insert account logo url: " + account.logoId);
                                        logoId = PublicAccount.insertLogoUrl(cr, account.logoUrl);
                                    }
                                    ContentValues cv = PublicAccount.storeBasicInfoToContentValues(account);
                                    cv.put(AccountColumns.LOGO_ID, logoId);
                                    cv.remove(AccountColumns.LOGO_URL);
                                    cv.remove(AccountColumns.LOGO_PATH);
                                    cr.update(
                                            AccountColumns.CONTENT_URI,
                                            cv,
                                            AccountColumns._ID + "=?",
                                            new String[]{Long.toString(accountId)});
                                    Log.d(TAG, "Update account " + accountId + " in background");
                                }
                            } finally {
                                if (c != null) {
                                    c.close();
                                }
                            }
                        }
                    } catch (PAMException e) {
                        e.printStackTrace();
                        setInitialized(false);
                        mWorkingTasks.remove(mId);
                        mIsGettingSubscribedList = false;
                        return;
                    }
                    pageNumber += 1;
                } while (accounts.size() == BATCH_SIZE);
                setInitialized(true);
                mWorkingTasks.remove(mId);
                mIsGettingSubscribedList = false;
            }
        }
        long requestId = generateUuid();
        GetSubscribedTask task = new GetSubscribedTask(requestId, null);
        mWorkingTasks.put(requestId, task);
        mExecutorService.submit(task);
    }
    
    private String getUuidFromAccountId(long accountId) {
        String uuid = null;
        Cursor c = null;
        try {
            c = getContentResolver().query(
                    AccountColumns.CONTENT_URI,
                    new String[]{AccountColumns.UUID},
                    AccountColumns._ID + "=?",
                    new String[]{Long.toString(accountId)},
                    null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                uuid = c.getString(c.getColumnIndexOrThrow(AccountColumns.UUID));
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return uuid;
    }
    
    private static final Uri CCS_MESSAGE_CONTENT_URI = Uri.parse("content://com.cmcc.ccs.message");

    private void updateCcsMessageProvider(MessageContent messageContent) {
        ContentValues cv = new ContentValues();
        cv.put(com.cmcc.ccs.chat.ChatMessage.MESSAGE_ID, messageContent.sourceId);
        cv.put(com.cmcc.ccs.chat.ChatMessage.FLAG, com.cmcc.ccs.chat.ChatMessage.PUBLIC);
        cv.put(com.cmcc.ccs.chat.ChatMessage.CONTACT_NUMBER, getUuidFromAccountId(messageContent.accountId));
        cv.put(com.cmcc.ccs.chat.ChatMessage.TIMESTAMP, messageContent.timestamp);
        cv.put(com.cmcc.ccs.chat.ChatMessage.MESSAGE_STATUS, messageContent.status);
        cv.put(com.cmcc.ccs.chat.ChatMessage.DIRECTION,
               (messageContent.direction == Constants.MESSAGE_DIRECTION_INCOMING ?
                        com.cmcc.ccs.chat.ChatMessage.INCOMING :
                        com.cmcc.ccs.chat.ChatMessage.OUTCOMING));

        // Set type, mime_type and body according to mediaType 
        if (messageContent.mediaType == Constants.MEDIA_TYPE_SINGLE_ARTICLE ||
            messageContent.mediaType == Constants.MEDIA_TYPE_MULTIPLE_ARTICLE) {
            cv.put(com.cmcc.ccs.chat.ChatMessage.TYPE, com.cmcc.ccs.chat.ChatService.XML);
            cv.put(com.cmcc.ccs.chat.ChatMessage.BODY, messageContent.buildMediaArticleString());
            cv.put(com.cmcc.ccs.chat.ChatMessage.MIME_TYPE, "application/xml");
        } else if (messageContent.mediaType == Constants.MEDIA_TYPE_TEXT) {
            cv.put(com.cmcc.ccs.chat.ChatMessage.TYPE, com.cmcc.ccs.chat.ChatService.IM);
            cv.put(com.cmcc.ccs.chat.ChatMessage.BODY, messageContent.text);
            cv.put(com.cmcc.ccs.chat.ChatMessage.MIME_TYPE, "text/plain");
        } else if (messageContent.mediaType == Constants.MEDIA_TYPE_AUDIO ||
                   messageContent.mediaType == Constants.MEDIA_TYPE_PICTURE ||
                   messageContent.mediaType == Constants.MEDIA_TYPE_VIDEO) {
            if (messageContent.direction == Constants.MESSAGE_DIRECTION_OUTGOING) {
                cv.put(com.cmcc.ccs.chat.ChatMessage.TYPE, com.cmcc.ccs.chat.ChatService.FT);
                cv.put(com.cmcc.ccs.chat.ChatMessage.BODY, messageContent.sourceId);
                cv.put(com.cmcc.ccs.chat.ChatMessage.MIME_TYPE, messageContent.basicMedia.fileType);
            } else {
                cv.put(com.cmcc.ccs.chat.ChatMessage.TYPE, com.cmcc.ccs.chat.ChatService.XML);
                cv.put(com.cmcc.ccs.chat.ChatMessage.BODY, messageContent.basicMedia.originalUrl);
                cv.put(com.cmcc.ccs.chat.ChatMessage.MIME_TYPE, messageContent.basicMedia.fileType);
            }
        } else if (messageContent.mediaType == Constants.MEDIA_TYPE_GEOLOC ||
                   messageContent.mediaType == Constants.MEDIA_TYPE_VCARD) {
            if (messageContent.direction == Constants.MESSAGE_DIRECTION_OUTGOING) {
                cv.put(com.cmcc.ccs.chat.ChatMessage.TYPE, com.cmcc.ccs.chat.ChatService.FT);
                cv.put(com.cmcc.ccs.chat.ChatMessage.BODY, messageContent.sourceId);
            } else {
                cv.put(com.cmcc.ccs.chat.ChatMessage.TYPE, com.cmcc.ccs.chat.ChatService.XML);
                cv.put(com.cmcc.ccs.chat.ChatMessage.BODY, messageContent.text);
            }
            cv.put(com.cmcc.ccs.chat.ChatMessage.MIME_TYPE,
                   (messageContent.mediaType == Constants.MEDIA_TYPE_GEOLOC ? "application/geoloc" : "text/vcard"));
        }
        Cursor c = null;
        try {
            c = getContentResolver().query(
                    CCS_MESSAGE_CONTENT_URI,
                    new String[] {
                            com.cmcc.ccs.chat.ChatMessage.MESSAGE_ID,
                            com.cmcc.ccs.chat.ChatMessage.TYPE},
                    com.cmcc.ccs.chat.ChatMessage.MESSAGE_ID + "=? AND " + com.cmcc.ccs.chat.ChatMessage.TYPE + "=?",
                    new String[] {
                        cv.getAsString(com.cmcc.ccs.chat.ChatMessage.MESSAGE_ID),
                        Integer.toString(cv.getAsInteger(com.cmcc.ccs.chat.ChatMessage.TYPE))},
                    null);
            if (c != null && c.getCount() > 0) {
                // update
                getContentResolver().update(
                        CCS_MESSAGE_CONTENT_URI,
                        cv,
                        com.cmcc.ccs.chat.ChatMessage.MESSAGE_ID + "=? AND " + com.cmcc.ccs.chat.ChatMessage.TYPE + "=?",
                        new String[] {
                                cv.getAsString(com.cmcc.ccs.chat.ChatMessage.MESSAGE_ID),
                                Integer.toString(cv.getAsInteger(com.cmcc.ccs.chat.ChatMessage.TYPE))});
            } else {
                // insert
                getContentResolver().insert(CCS_MESSAGE_CONTENT_URI, cv);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }
    
    private void updateCcsMessageStatus(String sourceId, int sourceTable, int status) {
        Pair<String, Integer> pair = getCombinedKeyForCcsMessage(sourceId, sourceTable);
        if (pair != null) {
            ContentValues cv = new ContentValues();
            cv.put(com.cmcc.ccs.chat.ChatMessage.MESSAGE_STATUS, status);
            getContentResolver().update(
                    CCS_MESSAGE_CONTENT_URI,
                    cv,
                    com.cmcc.ccs.chat.ChatMessage.MESSAGE_ID + "=? AND " + com.cmcc.ccs.chat.ChatMessage.TYPE + "=?",
                    new String[] { pair.value1, Integer.toString(pair.value2)});
        }
    }
    
    private void updateCcsMessageStatus(long messageId, int status) {
        Pair<String, Integer> pair = getCombinedKeyForCcsMessage(messageId);
        if (pair != null) {
            ContentValues cv = new ContentValues();
            cv.put(com.cmcc.ccs.chat.ChatMessage.MESSAGE_STATUS, status);
            getContentResolver().update(
                    CCS_MESSAGE_CONTENT_URI,
                    cv,
                    com.cmcc.ccs.chat.ChatMessage.MESSAGE_ID + "=? AND " + com.cmcc.ccs.chat.ChatMessage.TYPE + "=?",
                    new String[] { pair.value1, Integer.toString(pair.value2)});
        }
    }
    
    private void deleteFromCcsMessageProvider(MessageContent messageContent) {
        Pair<String, Integer> pair = getCombinedKeyForCcsMessage(messageContent);
        getContentResolver().delete(
                CCS_MESSAGE_CONTENT_URI,
                com.cmcc.ccs.chat.ChatMessage.MESSAGE_ID + "=? AND " + com.cmcc.ccs.chat.ChatMessage.TYPE + "=?",
                new String[] { pair.value1, Integer.toString(pair.value2) });
    }
    
    private Pair<String, Integer> getCombinedKeyForCcsMessage(MessageContent messageContent) {
        int type = -1;
        if (messageContent.mediaType == Constants.MEDIA_TYPE_SINGLE_ARTICLE
                || messageContent.mediaType == Constants.MEDIA_TYPE_MULTIPLE_ARTICLE) {
            type = com.cmcc.ccs.chat.ChatService.XML;
        } else if (messageContent.mediaType == Constants.MEDIA_TYPE_TEXT) {
            type = com.cmcc.ccs.chat.ChatService.IM;
        } else if (messageContent.mediaType == Constants.MEDIA_TYPE_AUDIO
                || messageContent.mediaType == Constants.MEDIA_TYPE_PICTURE
                || messageContent.mediaType == Constants.MEDIA_TYPE_VIDEO) {
            if (messageContent.direction == Constants.MESSAGE_DIRECTION_OUTGOING) {
                type = com.cmcc.ccs.chat.ChatService.FT;
            } else {
                type = com.cmcc.ccs.chat.ChatService.XML;
            }
        } else if (messageContent.mediaType == Constants.MEDIA_TYPE_GEOLOC
                || messageContent.mediaType == Constants.MEDIA_TYPE_VCARD) {
            if (messageContent.direction == Constants.MESSAGE_DIRECTION_OUTGOING) {
                type = com.cmcc.ccs.chat.ChatService.FT;
            } else {
                type = com.cmcc.ccs.chat.ChatService.XML;
            }
        }
        return new Pair<String, Integer>(messageContent.sourceId, type);
    }

    private Pair<String, Integer> getCombinedKeyForCcsMessage(String sourceId, int sourceTable) {
        Cursor c = null;
        try {
            c = getContentResolver().query(
                    MessageColumns.CONTENT_URI,
                    new String[] {
                        MessageColumns.SOURCE_ID,
                        MessageColumns.TYPE,
                        MessageColumns.DIRECTION,
                    },
                    PAContract.MessageColumns.SOURCE_ID + "=? AND " + PAContract.MessageColumns.SOURCE_TABLE + "=?",
                    new String[]{sourceId, Integer.toString(sourceTable)},
                    null);
            if (c != null && c.getCount() > 0) {
                return getCombinedKeyForCcsMessage(c);
            } else {
                return null;
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }
    
    private Pair<String, Integer> getCombinedKeyForCcsMessage(long messageId) {
        Cursor c = null;
        try {
            c = getContentResolver().query(
                    MessageColumns.CONTENT_URI,
                    new String[] {
                        MessageColumns.SOURCE_ID,
                        MessageColumns.TYPE,
                        MessageColumns.DIRECTION,
                    },
                    MessageColumns._ID + "=?",
                    new String[] {Long.toString(messageId)},
                    null);
            if (c != null && c.getCount() > 0) {
                return getCombinedKeyForCcsMessage(c);
            } else {
                return null;
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private Pair<String, Integer> getCombinedKeyForCcsMessage(Cursor c) {
        String sourceId = c.getString(c.getColumnIndexOrThrow(MessageColumns.SOURCE_ID));
        int mediaType = c.getInt(c.getColumnIndexOrThrow(MessageColumns.TYPE));
        int direction = c.getInt(c.getColumnIndexOrThrow(MessageColumns.DIRECTION));
        int type = -1;
        if (mediaType == Constants.MEDIA_TYPE_SINGLE_ARTICLE
                || mediaType == Constants.MEDIA_TYPE_MULTIPLE_ARTICLE) {
            type = com.cmcc.ccs.chat.ChatService.XML;
        } else if (mediaType == Constants.MEDIA_TYPE_TEXT) {
            type = com.cmcc.ccs.chat.ChatService.IM;
        } else if (mediaType == Constants.MEDIA_TYPE_AUDIO
                || mediaType == Constants.MEDIA_TYPE_PICTURE
                || mediaType == Constants.MEDIA_TYPE_VIDEO) {
            if (direction == Constants.MESSAGE_DIRECTION_OUTGOING) {
                type = com.cmcc.ccs.chat.ChatService.FT;
            } else {
                type = com.cmcc.ccs.chat.ChatService.XML;
            }
        } else if (mediaType == Constants.MEDIA_TYPE_GEOLOC
                || mediaType == Constants.MEDIA_TYPE_VCARD) {
            if (direction == Constants.MESSAGE_DIRECTION_OUTGOING) {
                type = com.cmcc.ccs.chat.ChatService.FT;
            } else {
                type = com.cmcc.ccs.chat.ChatService.XML;
            }
        }
        return new Pair<String, Integer>(sourceId, type);
    }
}
