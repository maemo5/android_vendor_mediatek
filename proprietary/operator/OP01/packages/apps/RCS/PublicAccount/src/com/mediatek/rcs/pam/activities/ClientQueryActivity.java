package com.mediatek.rcs.pam.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.mediatek.rcs.pam.IPAServiceCallback.Stub;
import com.mediatek.rcs.pam.PAMException;
import com.mediatek.rcs.pam.PAService;
import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.SimpleServiceCallback;
import com.mediatek.rcs.pam.client.UIPAMClient;
import com.mediatek.rcs.pam.model.ResultCode;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is an abstract class, which implement a ListView to show search results, a handler to
 * enqueue search actions on a different thread and the common actions to do search.
 */
abstract class ClientQueryActivity<T> extends Activity {
    public interface DownloadListener {
        void reportDownloadResult(int resultCode, String path, long mediaId);

        void reportDownloadProgress(long requestId, int percentage);
    }

    public interface Downloader {
        void downloadObject(String url, int type, ClientQueryActivity.DownloadListener listener);
    }

    protected String mTag;
    protected int mPageNum = 0;

    protected static final int LOAD_FIRST_TIME = 0;
    protected static final int LOAD_MORE_DATA = 1;

    // delay to trigger search (set > 0 when debug)
    protected static final int DELAY_SEARCH = 0;

    // timeout to connect service: 10s
    private static final int CONNECTING_TIMEOUT = 10 * 1000;

    private Handler mSearchHandler;
    protected boolean mIsLoading = false;

    protected ProgressBar mOuterProgressBar;
    private RelativeLayout mFooterProgressView;

    protected Context mContext;
    protected ListView mSearchListView;

    private PAService mService;
    protected ClientQueryActivity.Downloader mDownloader;
    private LongSparseArray<ClientQueryActivity.DownloadListener> mDownloadListenerArray;

    protected UIPAMClient mUIPAMClient;
    protected int mExceptionCode = 0;

    // Variable to remember the round of search (only process current round's request)
    protected int mUniqueSearchRound = 0;

    protected boolean mIsActivityForeground = false;

    private Handler mWatchDogHandler;
    private Runnable mWatchDogCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());

        mContext = this;
        mUIPAMClient = new UIPAMClient(mContext);
        mTag = getLogTag();

        startWatchConnection();
        initPAService();
        initBasicViews();
        initSearchHandler();

        //disable application icon from ActionBar
        getActionBar().setDisplayShowHomeEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsActivityForeground = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsActivityForeground = false;
    }

    /*
     * Disconnect the service when quit
     */
    @Override
    protected void onDestroy() {
        Log.i(mTag, "onDestroy");
        mUniqueSearchRound = 0;
        if (mService != null) {
            mService.disconnect();
        }
        stopWatchConnection();
        super.onDestroy();
    }

    /*
     * Whether activity is running in foreground
     */
    protected boolean isActivityForeground() {
        return mIsActivityForeground;
    };

    /*
     * Get the UI layout id
     */
    protected abstract int getLayoutId();

    /*
     * Get the log tag for subclass
     */
    protected abstract String getLogTag();

    /*
     * Init service and downloader
     */
    protected void initPAService() {
        mDownloadListenerArray = new LongSparseArray<ClientQueryActivity.DownloadListener>();

        Stub callback = new SimpleServiceCallback() {

            @Override
            public void reportDownloadResult(final long requestId, final int resultCode,
                    final String path, final long mediaId) throws RemoteException {
                ClientQueryActivity.DownloadListener listener = mDownloadListenerArray
                        .get(requestId);
                Log.i(mTag, "listener " + listener);
                if (listener != null) {
                    listener.reportDownloadResult(resultCode, path, 0);
                    mDownloadListenerArray.remove(Long.valueOf(requestId));
                }
            }

            @Override
            public void updateDownloadProgress(long requestId, int percentage)
                    throws RemoteException {
                super.updateDownloadProgress(requestId, percentage);
                ClientQueryActivity.DownloadListener listener = mDownloadListenerArray
                        .get(requestId);
                if (listener != null) {
                    listener.reportDownloadProgress(requestId, percentage);
                }
            }

            @Override
            public void onServiceConnected() throws RemoteException {
                Log.i(mTag, "onServiceConnected");
                doWhenConnected();
            }

            @Override
            public void onServiceDisconnected(int reason) throws RemoteException {
                Log.i(mTag, "onServiceDisconnected");
                doWhenDisConnected(reason);
            }
        };

        mService = new PAService(mContext, callback);
        mService.connect();
        mDownloader = new ClientQueryActivity.Downloader() {

            @Override
            public void downloadObject(String url, int type,
                    ClientQueryActivity.DownloadListener listener) {
                long requestId = mService.downloadObject(url, type);
                mDownloadListenerArray.put(Long.valueOf(requestId), listener);
            }
        };
    }

    /*
     * Whether service is connected
     */
    protected boolean isServiceConnected() {
        return mService.isServiceConnected();
    }

    /*
     * Hide outer progress bar when service is connected
     */
    protected void doWhenConnected() {
        hideOuterProgressBar();
        stopWatchConnection();
    }

    /*
     * Finish activity when service is disconnected
     */
    protected void doWhenDisConnected(int reason) {
        Log.i(mTag, "doWhenDisConnected, reason is " + reason);
        Toast.makeText(mContext, R.string.text_service_not_connect, Toast.LENGTH_SHORT).show();
        finish();
    }

    /*
     * Init basic views in onCreate(Bundle)
     */
    protected void initBasicViews() {
        mSearchListView.setOnScrollListener(new OnScrollListener() {
            private Boolean mIsDividePage = false;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // if scroll to end and release, load more data
                if (mIsDividePage && scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                    startMoreQuery();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                    int totalItemCount) {
                mIsDividePage = (firstVisibleItem + visibleItemCount == totalItemCount);
            }
        });
    }

    /*
     * Init handler to send (trigger search) and process search messages (do search on background)
     */
    protected void initSearchHandler() {
        HandlerThread handlerThread = new HandlerThread("Search data");
        handlerThread.start();
        mSearchHandler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Log.i(mTag, "Receive message " + msg);

                // M: check the current round
                if (msg.arg1 != mUniqueSearchRound) {
                    Log.i(mTag, "Do nothing, round " + msg.arg1 + " switch to "
                            + mUniqueSearchRound);
                    return;
                }

                if (msg.what == LOAD_FIRST_TIME) {
                    List<T> listSearch = searchData();

                    // M: check the current round after get data from remote server
                    if (msg.arg1 == mUniqueSearchRound) {
                        hideOuterProgressBar();
                        hideSearchHintView();
                        if (listSearch == null) {
                            handleQueryException();
                        } else if (listSearch.size() < 1) {
                            showNoSearchResult();
                        } else {
                            hideNoSearchResult();
                            updateListonUI(listSearch, LOAD_FIRST_TIME);
                        }
                        mIsLoading = false;
                    } else {
                        Log.i(mTag, "Do nothing, round " + msg.arg1 + " switch to "
                                + mUniqueSearchRound);
                    }
                } else if (msg.what == LOAD_MORE_DATA) {
                    List<T> listSearch = searchData();

                    // M: check the current round after get data from remote server
                    if (msg.arg1 == mUniqueSearchRound) {
                        hideFooterProgressBar();
                        if (listSearch == null) {
                            handleQueryException();
                        } else if (listSearch.size() < 1) {
                            showToastIfForeground(R.string.text_no_more_result);
                        } else {
                            updateListonUI(listSearch, LOAD_MORE_DATA);
                        }
                        mIsLoading = false;
                    } else {
                        Log.i(mTag, "Do nothing, round " + msg.arg1 + " switch to "
                                + mUniqueSearchRound);
                    }
                }
            }
        };
    }

    /*
     * Handle exception happened in search
     */
    protected void handleQueryException() {
        Log.i(mTag, "handleQueryException, exception code is " + mExceptionCode);
        runOnUiThread(new Runnable() {
            public void run() {
                if (mExceptionCode == ResultCode.SYSTEM_ERROR_NETWORK) {
                    showToastIfForeground(R.string.text_network_error);
                } else {
                    showToastIfForeground(R.string.text_exception_happend);
                }
            }
        });
    }

    /*
     * Trigger the first query & update state (loading, progress bar)
     */
    protected void startFirstQuery() {
        Log.i(mTag, "[startFirstQuery], isLoading " + mIsLoading);
        if (!mIsLoading) {
            mPageNum = 0;
            mIsLoading = true;
            showOuterProgressBar();
            Message msg = new Message();
            msg.what = LOAD_FIRST_TIME;
            msg.arg1 = mUniqueSearchRound;
            mSearchHandler.sendMessageDelayed(msg, DELAY_SEARCH);
        }
    }

    /*
     * Trigger more query & update state (loading, progress bar)
     */
    protected void startMoreQuery() {
        Log.i(mTag, "[startMoreQuery], isLoading " + mIsLoading);
        if (!mIsLoading) {
            mIsLoading = true;
            showFooterProgressBar();
            Message msg = new Message();
            msg.what = LOAD_MORE_DATA;
            msg.arg1 = mUniqueSearchRound;
            mSearchHandler.sendMessageDelayed(msg, DELAY_SEARCH);
        }
    }

    /*
     * Update the search result list view
     */
    protected abstract void updateListonUI(List<T> listSearch, int type);

    /*
     * Search data on remote server and return result
     */
    private List<T> searchData() {
        mPageNum++;

        List<T> list = new ArrayList<T>();
        try {
            list = searchDataViaClient();
        } catch (PAMException e) {
            mPageNum--;
            mExceptionCode = e.resultCode;
            e.printStackTrace();
            return null;
        }

        if (list != null && list.size() > 0) {
            doWhenResultNormal(list);
        } else {
            mPageNum--;
        }
        return list;
    }

    /*
     * Search data via client
     */
    protected abstract List<T> searchDataViaClient() throws PAMException;

    protected void doWhenResultNormal(List<T> list) {
        Log.i(mTag, "size is " + list.size());
    }

    /*
     * Show no search result view
     */
    protected abstract void hideNoSearchResult();

    /*
     * hide no search result view
     */
    protected abstract void showNoSearchResult();

    /*
     * hide the search hint view (optional)
     */
    protected void hideSearchHintView() {
        // do nothing here
    }

    /*
     * Show the outer progress bar view
     */
    protected void showOuterProgressBar() {
        mOuterProgressBar.setVisibility(View.VISIBLE);
        mOuterProgressBar.bringToFront();
    }

    /*
     * hide the outer progress bar view
     */
    protected void hideOuterProgressBar() {
        runOnUiThread(new Runnable() {
            public void run() {
                mOuterProgressBar.setVisibility(View.GONE);
            }
        });
    }

    /*
     * Show the footer progress bar view
     */
    @SuppressLint("InflateParams")
    private void showFooterProgressBar() {
        Log.i(mTag, "showFooterProgressBar");
        // inflate() each time, or else progress display still when
        // 1. backToInit when load more -> 2. startFirstQuery -> 3.startMoreQuery
        mFooterProgressView = (RelativeLayout) getLayoutInflater().inflate(
                R.layout.item_progress_load, null);
        mSearchListView.addFooterView(mFooterProgressView);
    }

    /*
     * Hide the footer progress bar view
     */
    protected void hideFooterProgressBar() {
        runOnUiThread(new Runnable() {
            public void run() {
                mSearchListView.removeFooterView(mFooterProgressView);
            }
        });
    }

    /*
     * Show the search result list view
     */
    protected void showSearchListView() {
        runOnUiThread(new Runnable() {
            public void run() {
                mSearchListView.setVisibility(View.VISIBLE);
            }
        });
    }

    /*
     * Hide the search result list view
     */
    protected void hideSearchListView() {
        runOnUiThread(new Runnable() {
            public void run() {
                mSearchListView.setVisibility(View.GONE);
            }
        });
    }

    /*
     * If not connected to service, watch it for 10s
     */
    private void startWatchConnection() {
        mWatchDogHandler = new Handler();
        mWatchDogCallback = new Runnable() {

            @Override
            public void run() {
                Log.i(mTag, "Service connection time out.");
                Toast.makeText(mContext, R.string.text_service_not_connect, Toast.LENGTH_SHORT)
                        .show();
                finish();
            }
        };
        mWatchDogHandler.postDelayed(mWatchDogCallback, CONNECTING_TIMEOUT);
    }

    /*
     * Stop watching connection when connect to service
     */
    private void stopWatchConnection() {
        if (mWatchDogHandler != null && mWatchDogCallback != null) {
            mWatchDogHandler.removeCallbacks(mWatchDogCallback);
            mWatchDogHandler = null;
        }
    }

    protected void showToastIfForeground(int resId) {
        String text = mContext.getString(resId);
        showToastIfForeground(text);
    }

    protected void showToastIfForeground(String text) {
        if (isActivityForeground()) {
            Log.i(mTag, "Show toast " + text);
            Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
        } else {
            Log.i(mTag, "Not foreground, hide toast " + text);
        }
    }
}
