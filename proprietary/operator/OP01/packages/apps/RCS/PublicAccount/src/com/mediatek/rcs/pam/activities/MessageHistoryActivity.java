package com.mediatek.rcs.pam.activities;

import android.app.ActionBar;
import android.content.AsyncQueryHandler;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.IPAServiceCallback.Stub;
import com.mediatek.rcs.pam.IPAServiceCallbackWrapper;
import com.mediatek.rcs.pam.MainActivity;
import com.mediatek.rcs.pam.PAService;
import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.SimpleServiceCallback;
import com.mediatek.rcs.pam.provider.PAContract;
import com.mediatek.rcs.pam.provider.PAContract.AccountColumns;
import com.mediatek.rcs.pam.provider.PAContract.MessageColumns;
import com.mediatek.rcs.pam.provider.PAContract.MessageHistorySummaryColumns;
import com.mediatek.rcs.pam.provider.PAContract.SearchColumns;

public class MessageHistoryActivity extends LoadingMaskActivity {
    private static final String TAG = Constants.TAG_PREFIX + "MessageHistoryActivity";
    
    public static final String ACTION = "com.mediatek.pam.MessageHistoryActivity";
    
    public static final String KEY_UUID = "com.mediatek.pam.MessageHistoryActivity.KEY_UUID";
    
    public static final int MESSAGE_HISTORY_QUERY_TOKEN  = 0;
    public static final int MESSAGE_SEARCH_QUERY_TOKEN  = 1;
    
    private boolean mDestroyed = false;
    private Handler mHandler;
    private Runnable mWatchDogCallback;

    public interface DownloadListener {
        void reportDownloadResult(int resultCode, String path, long mediaId);
    }

    /*
     * Mode Switching:
     *     MESSAGE_HISTORY_LIST => SEARCH_DESCRIPTION => SEARCH_RESULT_LIST
     *     MESSAGE_HISTORY_LIST <= SEARCH_DESCRIPTION <= SEARCH_RESULT_LIST
     *     MESSAGE_HISTORY_LIST <= SEARCH_RESULT_LIST
     */
    public static enum Mode {
        MESSAGE_HISTORY_LIST,
        SEARCH_DESCRIPTION,
        SEARCH_RESULT_LIST,
    }
    
    private boolean mIsHistoryEmpty = true;
    private boolean mIsSearchResultEmpty = true;
    
    private Mode mMode = Mode.MESSAGE_HISTORY_LIST;
    
    private SearchView mSearchView;
    private String mKeywords;
    private View mSearchDescriptionView;
    private View mEmptyHistoryDescriptionView;
    private ListView mSearchResultView;
    private View mEmptySearchDescriptionView;
    private ListView mMessageHistoryView;
    
    private ActionMode mActionMode;
    
    private PAService mService;

    private AsyncQueryHandler mProviderActionHandler;
    private MessageHistoryAdaptor mHistoryAdaptor;
    private SearchResultAdaptor mSearchResultAdaptor;
    private LongSparseArray<DownloadListener> mDownloadListeners;

    private Stub mCallback;

    private Downloader mDownloader;
    
    private ActionModeCallback mActionModeCallback;

    private boolean mIsQueryingMessageHistory = false;
    private boolean mIsQueryingSearchResult = false;
    
    private class ActionModeCallback implements ActionMode.Callback {
        private long mAccountId = Constants.INVALID;

        // Called when the action mode is created; startActionMode() was called
        @Override
        public synchronized boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.message_history_context_menu, menu);
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public synchronized boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public synchronized boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.message_history_context_delete_item:
                    startDeleteMessagesByAccountId(mAccountId);
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public synchronized void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            mAccountId = Constants.INVALID;
        }
        
        public void setAccountId(long id) {
            mAccountId = id;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        
        mDownloadListeners = new LongSparseArray<DownloadListener>();
        
        mSearchDescriptionView = (View) findViewById(R.id.search_description);
        mEmptyHistoryDescriptionView = (View) findViewById(R.id.empty_history_description);
        mEmptySearchDescriptionView = (View) findViewById(R.id.empty_search_description);
        
        mSearchResultView = (ListView) findViewById(R.id.search_result_list);
        mSearchResultView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MessageHistoryItem item = ((MessageHistoryItemView) view).getDataItem();
                Intent intent = new Intent(MessageHistoryActivity.this, PaComposeActivity.class);
                intent.putExtra(PaComposeActivity.ACCOUNT_ID, item.id);
                intent.putExtra(PaComposeActivity.UUID, item.accountUuid);
                intent.putExtra(PaComposeActivity.NAME, item.accountName);
                if (!TextUtils.isEmpty(item.logoPath)) {
                    intent.putExtra(PaComposeActivity.IMAGE_PATH, item.logoPath);
                }
                intent.putExtra(PaComposeActivity.SELECT_ID, item.lastMessageId);
                startActivity(intent);
            }
        });
        mMessageHistoryView = (ListView) findViewById(R.id.message_history_list);
        mMessageHistoryView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MessageHistoryItem item = ((MessageHistoryItemView) view).getDataItem();
                Intent intent = new Intent(MessageHistoryActivity.this, PaComposeActivity.class);
                intent.putExtra(PaComposeActivity.ACCOUNT_ID, item.id);
                intent.putExtra(PaComposeActivity.UUID, item.accountUuid);
                intent.putExtra(PaComposeActivity.NAME, item.accountName);
                if (!TextUtils.isEmpty(item.logoPath)) {
                    intent.putExtra(PaComposeActivity.IMAGE_PATH, item.logoPath);
                }
                intent.putExtra(PaComposeActivity.SELECT_ID, item.lastMessageId);
                startActivity(intent);
            }
        });
        
        switchToMode(Mode.MESSAGE_HISTORY_LIST);
        
        mDownloader = new Downloader();
        
        mCallback = new SimpleServiceCallback() {
            
            @Override
            public void reportDownloadResult(
                    final long requestId,
                    final int resultCode,
                    final String path,
                    final long mediaId) throws RemoteException {
                DownloadListener listener = mDownloadListeners.get(Long.valueOf(requestId));
                if (listener != null) {
                    listener.reportDownloadResult(resultCode, path, mediaId);
                    mDownloadListeners.remove(Long.valueOf(requestId));
                }
            }

            @Override
            public void onServiceConnected() throws RemoteException {
                Log.d(TAG, "onServiceConnected");
                cancelConnectingWatchDog();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startMessageHistoryQuery();
                    }
                });
            }

            @Override
            public void onServiceDisconnected(int reason) throws RemoteException {
                Log.d(TAG, "onServiceDisconnected");
                finish();
            }
            
            @Override
            public void reportDeleteMessageResult(long requestId, final int resultCode) throws RemoteException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "reportDeleteMessageResult(" + resultCode + ")");
                        startMessageHistoryQuery();
                    }
                });
            }
        };
        
        mService = new PAService(this, new IPAServiceCallbackWrapper(mCallback, this));
        
        // initialize with a null cursor
        mHistoryAdaptor = new MessageHistoryAdaptor(this, null, mDownloader, this);
        
        mSearchResultAdaptor = new SearchResultAdaptor(this, null, mDownloader, this);

        mProviderActionHandler = new AsyncQueryHandler(getContentResolver()) {
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                if (token == MESSAGE_HISTORY_QUERY_TOKEN) {
                    mIsQueryingMessageHistory = false;
                }
                if (token == MESSAGE_SEARCH_QUERY_TOKEN) {
                    mIsQueryingSearchResult = false;
                }
                if (mDestroyed || mService == null || !mService.isServiceConnected()) {
                    Log.w(TAG, "onQueryComplete " + token + " invoked when no service connection. Do nothing");
                    if (cursor != null) {
                        cursor.close();
                    }
                    return;
                }
                if (token == MESSAGE_HISTORY_QUERY_TOKEN) {
                    if (mMode == Mode.MESSAGE_HISTORY_LIST) {
                        bindMessageHistoryData(cursor);
                        if (mIsHistoryEmpty) {
                            mMessageHistoryView.setVisibility(View.GONE);
                            mEmptyHistoryDescriptionView.setVisibility(View.VISIBLE);
                        } else {
                            mMessageHistoryView.setVisibility(View.VISIBLE);
                            mEmptyHistoryDescriptionView.setVisibility(View.GONE);
                        }
                        switchToNormalView();
                    }
                } else if (token == MESSAGE_SEARCH_QUERY_TOKEN) {
                    if (mMode == Mode.SEARCH_DESCRIPTION) {
                        switchToMode(Mode.SEARCH_RESULT_LIST);
                    }
                    bindMessageSearchData(cursor);
                    if (mIsSearchResultEmpty) {
                        mSearchResultView.setVisibility(View.GONE);
                        mEmptySearchDescriptionView.setVisibility(View.VISIBLE);
                    } else {
                        mSearchResultView.setVisibility(View.VISIBLE);
                        mEmptySearchDescriptionView.setVisibility(View.GONE);
                    }
                    switchToNormalView();
                } else {
                    throw new Error("Invalid query token " + token);
                }
            }
        };
        switchToLoadingView();
        
        mMessageHistoryView.setAdapter(mHistoryAdaptor);
        mSearchResultView.setAdapter(mSearchResultAdaptor);
        
        mActionModeCallback = new ActionModeCallback();
        
        mMessageHistoryView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (mActionMode != null) {
                    return false;
                }

                mActionModeCallback.setAccountId(id);
                mActionMode = startActionMode(mActionModeCallback);
                return true;
            }
        });

        ActionBar actionBar = getActionBar();
        actionBar.setLogo(R.drawable.ic_account_detail);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        getContentResolver().registerContentObserver(
                AccountColumns.CONTENT_URI,
                true,
                new ContentObserver(new Handler()) {
                    @Override
                    public void onChange(boolean selfChange) {
                        if (mMode == Mode.MESSAGE_HISTORY_LIST) {
                            startMessageHistoryQuery();
                        }
                        super.onChange(selfChange);
                    }
                });
        getContentResolver().registerContentObserver(
                MessageColumns.CONTENT_URI,
                true,
                new ContentObserver(new Handler()) {
                    @Override
                    public void onChange(boolean selfChange) {
                        if (mMode == Mode.MESSAGE_HISTORY_LIST) {
                            startMessageHistoryQuery();
                        }
                        super.onChange(selfChange);
                    }
                });

        
        startConnectingWatchDog();
        mService.connect();
    }
    
    private static final int CONNECTING_TIMEOUT = 10; // seconds
    
    private void startConnectingWatchDog() {
        Log.w(TAG, "startConnectingWatchDog");
        mWatchDogCallback = new Runnable() {
            
            @Override
            public void run() {
                Log.w(TAG, "Service connecting time out.");
                Toast.makeText(MessageHistoryActivity.this, R.string.message_history_cannot_connect_to_rcs, Toast.LENGTH_LONG).show();
                finish();
            }
        };
        mHandler.postDelayed(mWatchDogCallback, CONNECTING_TIMEOUT * 1000);
    }
    
    private void cancelConnectingWatchDog() {
        Log.w(TAG, "cancelConnectingWatchDog");
        if (mWatchDogCallback != null) {
            mHandler.removeCallbacks(mWatchDogCallback);
        }
    }
    
    @Override
    public void onDestroy() {
        mDestroyed = true;
        if (mService != null) {
            mService.disconnect();
        }
        super.onDestroy();
    }
    
    public class Downloader {
        // TODO check for concurrency issues of this method and the callback method
        public void downloadObject(String url, int type, DownloadListener listener) {
            long requestId = mService.downloadObject(url, type);
            mDownloadListeners.put(Long.valueOf(requestId), listener);
        }
    }
    
    public void startMessageHistoryQuery() {
        if (mIsQueryingMessageHistory) {
            return;
        }
        mIsQueryingMessageHistory = true;
        switchToLoadingView();
        mProviderActionHandler.startQuery(
                MESSAGE_HISTORY_QUERY_TOKEN,
                null,
                PAContract.MessageHistorySummaryColumns.CONTENT_URI,
                null, /* all columns */
                (MessageHistorySummaryColumns.LAST_MESSAGE_ID + "!=-1 AND " 
                 + MessageHistorySummaryColumns.LAST_MESSAGE_ID + " IS NOT NULL"),
                null,
                null);
    }
    
    public void startMessageSearchQuery() {
        if (mIsQueryingSearchResult) {
            return;
        }
        mIsQueryingSearchResult = true;
        switchToLoadingView();
        mProviderActionHandler.startQuery(
                MESSAGE_SEARCH_QUERY_TOKEN,
                null,
                SearchColumns.CONTENT_URI.buildUpon().appendQueryParameter(PAContract.SEARCH_PARAM_KEYWORD, mKeywords).build(),
                null, /* all columns */
                null,
                null,
                null);
    }
    
    public void startDeleteMessagesByAccountId(long accountId) {
        switchToLoadingView();
        if (mService != null && mService.isServiceConnected()) {
            mService.deleteMessageByAccount(accountId);
        }
    }
    
    private void bindMessageHistoryData(Cursor cursor) {
        Log.d(TAG, "bindMessageHistoryData with " + (cursor == null ? 0 : cursor.getCount()));
        if (cursor != null && cursor.getCount() > 0) {
            mIsHistoryEmpty = false;
            int position = cursor.getPosition();
            Log.d(TAG, "Cursor position is " + position);
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); ++i) {
                Log.d(TAG, "UUID: " + cursor.getString(cursor.getColumnIndex(MessageHistorySummaryColumns.UUID)));
                Log.d(TAG, "NAME: " + cursor.getString(cursor.getColumnIndex(MessageHistorySummaryColumns.NAME)));
                Log.d(TAG, "SUMMARY: " + cursor.getString(
                        cursor.getColumnIndex(MessageHistorySummaryColumns.LAST_MESSAGE_SUMMARY)));
                cursor.moveToNext();
            }
            cursor.moveToPosition(position);
        } else {
            mIsHistoryEmpty = true;
        }
        mHistoryAdaptor.changeCursor(cursor);
    }
    
    private void bindMessageSearchData(Cursor cursor) {
        if (cursor != null && cursor.getCount() > 0) {
            mIsSearchResultEmpty = false;
        } else {
            mIsSearchResultEmpty = true;
        }
        mSearchResultAdaptor.changeCursor(cursor);
    }
    
    private void switchToMode(Mode mode) {
        if (mode == mMode) {
            return;
        }
        Mode lastMode = mMode;
        mMode = mode;
        if (mode == Mode.SEARCH_DESCRIPTION) {
            mSearchDescriptionView.setVisibility(View.VISIBLE);
            mSearchResultView.setVisibility(View.GONE);
            mMessageHistoryView.setVisibility(View.GONE);
            mEmptyHistoryDescriptionView.setVisibility(View.GONE);
            mEmptySearchDescriptionView.setVisibility(View.GONE);
        } else if (lastMode == Mode.SEARCH_DESCRIPTION && mode == Mode.SEARCH_RESULT_LIST) {
            mMessageHistoryView.setVisibility(View.GONE);
            mEmptyHistoryDescriptionView.setVisibility(View.GONE);
            mSearchDescriptionView.setVisibility(View.GONE);
            if (mIsSearchResultEmpty) {
                mSearchResultView.setVisibility(View.GONE);
                mEmptySearchDescriptionView.setVisibility(View.VISIBLE);
            } else {
                mSearchResultView.setVisibility(View.VISIBLE);
                mEmptySearchDescriptionView.setVisibility(View.GONE);
            }
        } else if (mode == Mode.MESSAGE_HISTORY_LIST) {
            mSearchDescriptionView.setVisibility(View.GONE);
            mEmptySearchDescriptionView.setVisibility(View.GONE);
            mSearchResultView.setVisibility(View.GONE);
            if (mIsHistoryEmpty) {
                mMessageHistoryView.setVisibility(View.GONE);
                mEmptyHistoryDescriptionView.setVisibility(View.VISIBLE);
            } else {
                mMessageHistoryView.setVisibility(View.VISIBLE);
                mEmptyHistoryDescriptionView.setVisibility(View.GONE);
            }
        } else {
            throw new Error("Invalid mode switching: " + lastMode + " to " + mode);
        }
    }
    
    
    @Override
    public void onResume() {
        super.onResume();
        if (mMode == Mode.MESSAGE_HISTORY_LIST) {
            startMessageHistoryQuery();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.message_history, menu);
        MenuItem menuItem = menu.findItem(R.id.search_button_item);
        mSearchView = (SearchView) menuItem.getActionView();
        
        mSearchView.setOnSearchClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                mSearchView.setSubmitButtonEnabled(false);
                switchToMode(Mode.SEARCH_DESCRIPTION);
            }
        });

        mSearchView.setOnCloseListener(new OnCloseListener() {
            
            @Override
            public boolean onClose() {
                switchToMode(Mode.MESSAGE_HISTORY_LIST);
                return false;
            }
        });

        mSearchView.setOnQueryTextListener(new OnQueryTextListener() {
            
            @Override
            public boolean onQueryTextSubmit(String query) {
                // we use "mKeywords" instead of "query"
                startMessageSearchQuery();
                return false;
            }
            
            @Override
            public boolean onQueryTextChange(String newText) {
                mKeywords = newText;
                if (newText.length() > 0) {
                    mSearchView.setSubmitButtonEnabled(true);
                } else {
                    mSearchView.setSubmitButtonEnabled(false);
                }
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected int getContentLayoutId() {
        return R.layout.activity_message_history;
    }
    
    public Mode getMode() {
        return mMode;
    }
}
