package com.cmcc.ccs.publicaccount;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.IPAServiceCallback;
import com.mediatek.rcs.pam.IPAServiceCallbackWrapper;
import com.mediatek.rcs.pam.PAMException;
import com.mediatek.rcs.pam.PAService;
import com.mediatek.rcs.pam.PlatformManager;
import com.mediatek.rcs.pam.client.PAMClient;
import com.mediatek.rcs.pam.model.ResultCode;
import com.mediatek.rcs.pam.provider.PAContract.AccountColumns;
import com.mediatek.rcs.pam.provider.PAContract.CcsSearchColumns;
import com.mediatek.rcs.pam.provider.PAContract.MessageColumns;

import org.gsma.joyn.JoynService;
import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.JoynServiceListener;
import org.gsma.joyn.JoynServiceNotAvailableException;
import org.gsma.joyn.JoynServiceRegistrationListener;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class PublicAccountService extends JoynService {
    private static final String TAG = "PublicAccountService";

    private static final int TIMEOUT = 5; // 30s
    
    private Context mContext;
    private PAMClient mClient;
    private PAService mPAService;
    private IPAServiceCallback mCallback;
    private JoynServiceListener mListener;
    private List<PublicAccountChatListener> mEventListeners;
    private List<JoynServiceRegistrationListener> mRegistrationListeners;
    private Map<Long, BlockingQueue<Long>> mPendingRequests;
    private Map<Long, String> mPendingPARequests;

    private long mUuidCounter = 0;
    private ExecutorService mExecutorService;
    private ConcurrentHashMap<Long, RequestedTask> mWorkingTasks;

    private abstract class RequestedTask implements Runnable {
        protected final long mId;
        
        public RequestedTask(long requestId) {
            mId = requestId;
        }
    }

    public static final String READ_PERMISSION = "com.cmcc.ccs.READ_PUBLICACCOUNT";
    public static final String WRITE_PERMISSION = "com.cmcc.ccs.WRITE_PUBLICACCOUNT";
    
    public PublicAccountService(Context context, JoynServiceListener listener) {
        super(context, listener);
        mContext = context;
        mListener = listener;
        mEventListeners = new LinkedList<PublicAccountChatListener>();
        mRegistrationListeners = new LinkedList<JoynServiceRegistrationListener>();
        mPendingRequests = new HashMap<Long, BlockingQueue<Long>>();
        mPendingPARequests = new HashMap<Long, String>();
        mCallback = new IPAServiceCallback.Stub() {
            @Override
            public void onServiceConnected() throws RemoteException {
                mListener.onServiceConnected();
            }

            @Override
            public void onServiceDisconnected(int reason) throws RemoteException {
                mListener.onServiceDisconnected(reason);
            }

            @Override
            public void onServiceRegistered() throws RemoteException {
                for (JoynServiceRegistrationListener listener : mRegistrationListeners) {
                    listener.onServiceRegistered();
                }
            }

            @Override
            public void onServiceUnregistered() throws RemoteException {
                for (JoynServiceRegistrationListener listener : mRegistrationListeners) {
                    listener.onServiceUnregistered();
                }
            }

            @Override
            public void onNewMessage(long accountId, long messageId) throws RemoteException {
                // do nothing
            }

            @Override
            public void onReportMessageFailed(long messageId) throws RemoteException {
                // do nothing
            }

            @Override
            public void onReportMessageDisplayed(long messageId) throws RemoteException {
                // do nothing
            }

            @Override
            public void onReportMessageDelivered(long messageId) throws RemoteException {
                // do nothing
            }

            @Override
            public void onComposingEvent(long accountId, boolean status) throws RemoteException {
                // do nothing
            }

            @Override
            public void reportSubscribeResult(long requestId, int resultCode) throws RemoteException {
                String account = mPendingPARequests.get(requestId);
                if (account != null) {
                    for (PublicAccountChatListener listener : mEventListeners) {
                        listener.onFollowPublicAccount(
                                account,
                                resultCode == ResultCode.SUCCESS ? PublicService.OK : PublicService.INTERNEL_ERROR,
                                null);
                    }
                }
            }

            @Override
            public void reportUnsubscribeResult(long requestId, int resultCode) throws RemoteException {
                String account = mPendingPARequests.get(requestId);
                if (account != null) {
                    for (PublicAccountChatListener listener : mEventListeners) {
                        listener.onUnfollowPublicAccount(
                                account,
                                resultCode == ResultCode.SUCCESS ? PublicService.OK : PublicService.INTERNEL_ERROR,
                                null);
                    }
                }
            }

            @Override
            public void reportGetSubscribedResult(long requestId, int resultCode, long[] accountIds)
                    throws RemoteException {
                // do nothing
            }

            @Override
            public void reportGetDetailsResult(long requestId, int resultCode, long accountId) throws RemoteException {
                String account = mPendingPARequests.get(requestId);
                if (account != null) {
                    // request from get public account info
                    for (PublicAccountChatListener listener : mEventListeners) {
                        listener.onGetInfo(
                                account,
                                resultCode == ResultCode.SUCCESS ? PublicService.OK : PublicService.INTERNEL_ERROR,
                                null);
                    }
                } else {
                    // request from send message
                    BlockingQueue<Long> queue = mPendingRequests.get(requestId);
                    if (resultCode == ResultCode.SUCCESS) {
                        try {
                            queue.put((long) accountId);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            queue.put((long) Constants.INVALID);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void reportGetMenuResult(long requestId, int resultCode) throws RemoteException {
                String account = mPendingPARequests.get(requestId);
                if (account != null) {
                    Cursor c = null;
                    String menu = null;
                    try {
                        c = mContext.getContentResolver().query(
                                AccountColumns.CONTENT_URI,
                                new String[]{AccountColumns.MENU},
                                AccountColumns.UUID + "=?",
                                new String[]{account},
                                null);
                        if (c != null && c.getCount() > 0) {
                            c.moveToFirst();
                            menu = c.getString(c.getColumnIndexOrThrow(AccountColumns.MENU));
                        } else {
                            resultCode = ResultCode.SYSEM_ERROR_UNKNOWN;
                        }
                    } finally {
                        if (c != null) {
                            c.close();
                        }
                    }
                    for (PublicAccountChatListener listener : mEventListeners) {
                        listener.onMenuConfigUpdated(
                                account,
                                menu,
                                resultCode == ResultCode.SUCCESS ? PublicService.OK : PublicService.INTERNEL_ERROR,
                                null);
                    }
                }
            }

            @Override
            public void reportDownloadResult(long requestId, int resultCode, String path, long mediaId)
                    throws RemoteException {
                // do nothing
            }

            @Override
            public void updateDownloadProgress(long requestId, int percentage) throws RemoteException {
                // do nothing
            }

            @Override
            public void onTransferProgress(long messageId, long currentSize, long totalSize) throws RemoteException {
                // do nothing
            }

            @Override
            public void reportSetAcceptStatusResult(long requestId, int resultCode) throws RemoteException {
                // do nothing
            }

            @Override
            public void reportComplainSpamSuccess(long messageId) throws RemoteException {
                // do nothing
            }

            @Override
            public void reportComplainSpamFailed(long messageId, int errorCode) throws RemoteException {
                // do nothing
            }

            @Override
            public void onAccountChanged(String newAccount) throws RemoteException {
                // do nothing
            }

            @Override
            public void reportDeleteMessageResult(long requestId, int resultCode) throws RemoteException {
                // do nothing
            }

        };
        mPAService = new PAService(context, new IPAServiceCallbackWrapper(mCallback, context));
        mClient = new PAMClient(PlatformManager.getInstance().getTransmitter(mContext), mContext);
    }
    
    @Override
    public void addServiceRegistrationListener(JoynServiceRegistrationListener listener) throws JoynServiceException {
        if (mPAService.isServiceConnected()) {
            mRegistrationListeners.add(listener);
        } else {
            throw new JoynServiceNotAvailableException();
        }
    }
    
    @Override
    public void removeServiceRegistrationListener(JoynServiceRegistrationListener listener) throws JoynServiceException {
        if (mPAService.isServiceConnected()) {
            mRegistrationListeners.remove(listener);
        } else {
            throw new JoynServiceNotAvailableException();
        }
    }

    public void addEventListener(PublicAccountChatListener listener) {
        mEventListeners.add(listener);
    }

    public void removeEventListener(PublicAccountChatListener listener) {
        mEventListeners.remove(listener);
    }

    /**
     * Synchronous API.
     * @param account UUID of the public account
     * @param message Body of message
     * @return
     */
    public String sendMessage(String account, String message) {
        Cursor c = null;
        try {
            c = mContext.getContentResolver().query(
                    AccountColumns.CONTENT_URI,
                    new String[] {AccountColumns._ID},
                    AccountColumns.UUID + "=?",
                    new String[] {account},
                    null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                final long accountId = c.getLong(c.getColumnIndexOrThrow(AccountColumns._ID));
                return sendMessageToAccountId(accountId, message);
            } else {
                long requestId = mPAService.getDetails(account, null);
                BlockingQueue<Long> queue = new LinkedBlockingQueue<Long>();
                mPendingRequests.put(requestId, queue);
                long accountId = Constants.INVALID;
                try {
                    accountId = queue.poll(TIMEOUT, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    // time out
                    Log.d(TAG, "Get details time out when sending message");
                    return null;
                } finally {
                    mPendingRequests.remove(queue);
                }
                if (accountId != Constants.INVALID) {
                    String result = sendMessageToAccountId(accountId, message);
                    return result;
                } else {
                    Log.d(TAG, "No public account: " + account);
                    return null;
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }
    
    private String sendMessageToAccountId(long accountId, String message) {
        final long messageId = mPAService.sendMessage(accountId, message, false);
        if (messageId == Constants.INVALID) {
            return null;
        } else {
            return Long.toString(messageId);
        }
    }

    public boolean deleteMessage(String msgId) {
        return mPAService.deleteMessage(Long.parseLong(msgId));
    }

    public boolean setMessageRead(String msgId) {
        ContentValues cv = new ContentValues();
        cv.put(MessageColumns.STATUS, Constants.MESSAGE_STATUS_READ);
        int updateCount = mContext.getContentResolver().update(
                MessageColumns.CONTENT_URI,
                cv,
                MessageColumns._ID + "=?",
                new String[]{msgId});
        // TODO Should we send read report to the server? Does server care?
        return (updateCount == 1);
    }

    public void getPublicAccountInfo(String account) {
        long requestId = mPAService.getDetails(account, null);
        mPendingPARequests.put(requestId, account);
    }

    public void followPublicAccount(String account) {
        long requestId = mPAService.subscribe(account);
        mPendingPARequests.put(requestId, account);
    }

    public void searchPublicAccount(final String keyword, final int pageNum, final int order, final int pageSize) {
        final long requestId = generateUuid();
        final class SearchTask extends RequestedTask {
            public SearchTask(long requestId) {
                super(requestId);
            }
            
            @Override
            public void run() {
                try {
                    List<com.mediatek.rcs.pam.model.PublicAccount> accounts = mClient.search(keyword, order, pageSize, pageNum);
                    ContentResolver cr = mContext.getContentResolver();
                    cr.delete(CcsSearchColumns.CONTENT_URI, null, null);
                    boolean errorHappened = false;
                    for (com.mediatek.rcs.pam.model.PublicAccount account : accounts) {
                        long accountId = storeSearchedAccount(account);
                        if (accountId == Constants.INVALID) {
                            Log.e(TAG, "Insert searched account failed: " + account.uuid);
                            errorHappened = true;
                        }
                    }
                    if (errorHappened) {
                        for (PublicAccountChatListener listener : mEventListeners) {
                            listener.onSearch(PublicService.INTERNEL_ERROR, Integer.toString(ResultCode.PARAM_ERROR_UNKNOWN));
                        }
                    } else {
                        for (PublicAccountChatListener listener : mEventListeners) {
                            listener.onSearch(PublicService.OK, null);
                        }
                    }
                } catch (PAMException e) {
                    e.printStackTrace();
                    for (PublicAccountChatListener listener : mEventListeners) {
                        listener.onSearch(PublicService.INTERNEL_ERROR, Integer.toString(ResultCode.PARAM_ERROR_UNKNOWN));
                    }
                }
                mWorkingTasks.remove(mId);
            }
        }
        SearchTask task = new SearchTask(requestId);
        mWorkingTasks.put(requestId, task);
        mExecutorService.execute(task);
    }
    
    private long storeSearchedAccount(com.mediatek.rcs.pam.model.PublicAccount account) {
        ContentValues cv = new ContentValues();
        cv.put(CcsSearchColumns.ACCOUNT, account.uuid);
        cv.put(CcsSearchColumns.NAME, account.name);
        cv.put(CcsSearchColumns.PORTRAIT, account.logoUrl);
        String portraitType = null;
        String url = account.logoUrl.toLowerCase();
        if (url.endsWith(".jpg") || url.endsWith(".jpeg")) {
            portraitType = "JPG";
        } else if (url.endsWith(".png")) {
            portraitType = "PNG";
        } else if (url.endsWith(".bmp")) {
            portraitType = "BMP";
        } else if (url.endsWith(".gif")) {
            portraitType = "GIF";
        }
        cv.put(CcsSearchColumns.PORTRAIT_TYPE, portraitType);
        cv.put(CcsSearchColumns.BREIF_INTRODUCTION, account.introduction);
        Uri uri = mContext.getContentResolver().insert(CcsSearchColumns.CONTENT_URI, cv);
        Log.d(TAG, "Insert searched account: " + uri);
        long accountId = Constants.INVALID;
        try {
            accountId = Long.parseLong(uri.getLastPathSegment());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return accountId;
    }

    public void unfollowPublicAccount(String account) {
        long requestId = mPAService.unsubscribe(account);
        mPendingPARequests.put(requestId, account);
    }

    public boolean getPublicAccountStatus(String account) {
        int subscriptionStatus = getSubscriptionStatus(account);
        if (subscriptionStatus != Constants.INVALID) {
            return (subscriptionStatus == Constants.SUBSCRIPTION_STATUS_YES);
        } else {
            long requestId = mPAService.getDetails(account, null);
            BlockingQueue<Long> queue = new LinkedBlockingQueue<Long>();
            mPendingRequests.put(requestId, queue);
            long accountId = Constants.INVALID;
            try {
                accountId = queue.poll(TIMEOUT, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                // time out
                Log.d(TAG, "Get details time out when sending message");
                return false;
            } finally {
                mPendingRequests.remove(queue);
            }
            if (accountId != Constants.INVALID) {
                return (Constants.SUBSCRIPTION_STATUS_YES == getSubscriptionStatus(account));
            } else {
                Log.d(TAG, "No public account: " + account);
                return false;
            }
        }
    }
    
    private int getSubscriptionStatus(String account) {
        Cursor c = null;
        try {
            c = mContext.getContentResolver().query(
                    AccountColumns.CONTENT_URI,
                    new String[]{AccountColumns.SUBSCRIPTION_STATUS},
                    AccountColumns.UUID,
                    new String[]{account},
                    null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                return c.getInt(c.getColumnIndexOrThrow(AccountColumns.SUBSCRIPTION_STATUS));
            } else {
                return Constants.INVALID;
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public void reportPublicAccount(final String account, final String reason, final String description, final int type, final String data) {
        final long requestId = generateUuid();
        final class ReportTask extends RequestedTask {
            public ReportTask(long requestId) {
                super(requestId);
            }
            
            @Override
            public void run() {
                try {
                    int result = mClient.complain(account, type, reason, data, description);
                    for (PublicAccountChatListener listener : mEventListeners) {
                        listener.onReportPublicAccount(
                                account,
                                // TODO check this error code
                                result == ResultCode.SUCCESS ? PublicService.OK : PublicService.INTERNEL_ERROR,
                                Integer.toString(result));
                    }
                } catch (PAMException e) {
                    e.printStackTrace();
                    for (PublicAccountChatListener listener : mEventListeners) {
                        listener.onReportPublicAccount(
                                account,
                                // TODO check this error code
                                PublicService.INTERNEL_ERROR,
                                null);
                    }
                }
                mWorkingTasks.remove(mId);
            }
        }
        ReportTask task = new ReportTask(requestId);
        mWorkingTasks.put(requestId, task);
        mExecutorService.execute(task);
    }

    public void updateMenuConfig(String account) {
        long requestId = mPAService.getMenu(account, null);
        mPendingPARequests.put(requestId, account);
    }

    @Override
    public void connect() {
        mPAService.connect();
    }

    @Override
    public void disconnect() {
        mPAService.disconnect();
    }
    
    private synchronized long generateUuid() {
        mUuidCounter += 1;
        return mUuidCounter;
    }
}
