package com.mediatek.rcs.pam.activities;

import android.content.AsyncQueryHandler;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.PAMException;
import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.model.PublicAccount;
import com.mediatek.rcs.pam.provider.PAContract.AccountColumns;

import java.util.ArrayList;
import java.util.List;

public class AccountListActivity extends ClientQueryActivity<PublicAccount> {

    private static final int[] TYPE_RECOMMEND = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 99 };

    // default size per request (in [0,50])
    private static final int DEFAULT_PAGESIZE = 10;

    private int mPageSize = DEFAULT_PAGESIZE;
    private int mType = -1;

    private String mTitle;
    private String[] mRecTypes;

    private ListView mFollowedListView;
    private ListView mSearchTypeListView;
    private AccountListAdapter mFollowedAdapter;

    private AsyncQueryHandler mBackgroundQueryHandler;
    private AccountSearchAdapter mSearchAdapter;
    private ContentObserver mContentObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(mTag, "onCreate");

        initBackgroundQueryHandle();

        if (!isServiceConnected()) {
            showOuterProgressBar();
        }
    }

    @Override
    protected void doWhenConnected() {
        super.doWhenConnected();
        queryFollowedAccounts();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_account_list;
    }

    @Override
    protected String getLogTag() {
        return Constants.TAG_PREFIX + "AccountListActivity";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.account_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_search) {
            startActivity(new Intent(this, AccountSearchActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        Log.i(mTag, "onDestroy");
        mFollowedAdapter.changeCursor(null);
        getContentResolver().unregisterContentObserver(mContentObserver);

        super.onDestroy();
    }

    protected void initBasicViews() {
        mFollowedAdapter = new AccountListAdapter(this, R.layout.account_list_item, null, 0,
                mDownloader);
        mFollowedListView = (ListView) findViewById(R.id.lv_account_list);
        mFollowedListView.setVisibility(View.GONE);
        mFollowedListView.setAdapter(mFollowedAdapter);

        mOuterProgressBar = (ProgressBar) findViewById(R.id.pb_account_list);
        mOuterProgressBar.setVisibility(View.GONE);

        // the recommend type list
        mSearchTypeListView = (ListView) findViewById(R.id.lv_recommend_type);
        mSearchTypeListView.setAdapter(buildRecTypeAdapter());
        mSearchTypeListView.setVisibility(View.GONE);

        mSearchTypeListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i(mTag, "Click position " + position + ", type " + mRecTypes[position]);
                if (!mIsLoading) {
                    mType = TYPE_RECOMMEND[position];
                    mTitle = mRecTypes[position];
                    startFirstQuery();
                } else {
                    Log.i(mTag, "Already loading, do nothing");
                }
            }
        });

        // the recommend account list
        mSearchListView = (ListView) findViewById(R.id.lv_recommend_accounts);
        mSearchListView.setVisibility(View.GONE);

        mSearchAdapter = new AccountSearchAdapter(mContext, new ArrayList<PublicAccount>(),
                mDownloader);
        mSearchListView.setAdapter(mSearchAdapter);
        super.initBasicViews();
    }

    private void updateTitle(final String title) {
        runOnUiThread(new Runnable() {
            public void run() {
                getActionBar().setTitle(title);
            }
        });
    }

    private void initBackgroundQueryHandle() {
        mBackgroundQueryHandler = new AsyncQueryHandler(getContentResolver()) {

            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                super.onQueryComplete(token, cookie, cursor);
                Log.i(mTag, "[onQueryComplete] cursor " + cursor);

                hideOuterProgressBar();

                if (cursor != null && cursor.getCount() > 0) {
                    Log.i(mTag, "[onQueryComplete] followed number " + cursor.getCount());
                    mFollowedListView.setVisibility(View.VISIBLE);
                    mFollowedAdapter.changeCursor(cursor);
                    hideSearchTypeView();
                    hideSearchListView();
                } else {
                    Log.i(mTag, "[onQueryComplete] not followed.");
                    mFollowedListView.setVisibility(View.GONE);
                    if (mSearchListView != null && mSearchListView.getCount() > 0) {
                        Log.i(mTag, "Search result number " + mSearchListView.getCount());
                        showSearchListView();
                        hideSearchTypeView();
                    } else {
                        Log.i(mTag, "Show recommend type list.");
                        showSearchTypeView();
                        hideSearchListView();
                    }
                }
            }
        };

        mContentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                queryFollowedAccounts();
                super.onChange(selfChange);
            }
        };
        getContentResolver().registerContentObserver(AccountColumns.CONTENT_URI, true,
                mContentObserver);
    }

    private void queryFollowedAccounts() {
        Log.i(mTag, "queryFollowedAccounts");
        String[] projection = new String[] { AccountColumns._ID, AccountColumns.UUID,
                AccountColumns.NAME, AccountColumns.ACTIVE_STATUS, AccountColumns.LOGO_PATH,
                AccountColumns.LOGO_URL };

        String selection = AccountColumns.SUBSCRIPTION_STATUS + " = "
                + Constants.SUBSCRIPTION_STATUS_YES;

        mBackgroundQueryHandler.startQuery(0, 0, AccountColumns.CONTENT_URI, projection, selection,
                null, AccountColumns.NAME);
        showOuterProgressBar();
    }

    private ArrayAdapter<String> buildRecTypeAdapter() {
        mRecTypes = getResources().getStringArray(R.array.recommend_type);

        final ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < mRecTypes.length; ++i) {
            list.add(mRecTypes[i]);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext,
                android.R.layout.simple_list_item_1, list);
        return adapter;
    }

    @Override
    protected void updateListonUI(List<PublicAccount> listSearch, int type) {
        Log.i(mTag, "[updateListonUI]");
        final List<PublicAccount> dataList;
        if (type == LOAD_MORE_DATA) {
            dataList = mSearchAdapter.getAccountList();
            Log.i(mTag, "Old size is " + dataList.size());
            dataList.addAll(listSearch);
        } else {
            updateTitle(mTitle);
            dataList = listSearch;
        }

        runOnUiThread(new Runnable() {
            public void run() {
                mSearchListView.setVisibility(View.VISIBLE);
                mSearchListView.requestLayout();
                mSearchAdapter.setAccountList(dataList);
                Log.i(mTag, "Refresh list, new size " + dataList.size());
            }
        });
    }

    @Override
    protected List<PublicAccount> searchDataViaClient() throws PAMException {
        return mUIPAMClient.getRecommends(mType, mPageSize, mPageNum);
    }

    private void showSearchTypeView() {
        final String hint = getResources().getString(R.string.text_no_accounts_followed);
        runOnUiThread(new Runnable() {
            public void run() {
                if (isActivityForeground()) {
                    Toast.makeText(AccountListActivity.this, hint, Toast.LENGTH_SHORT).show();
                    Log.i(mTag, "Activity is foreground, show toast " + hint);
                } else {
                    Log.i(mTag, "Activity is not foreground, not show toast");
                }

                mSearchTypeListView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void hideSearchTypeView() {
        runOnUiThread(new Runnable() {
            public void run() {
                mSearchTypeListView.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void showNoSearchResult() {
        runOnUiThread(new Runnable() {
            public void run() {
                showToastIfForeground(R.string.text_no_search_result);
                mSearchTypeListView.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void hideNoSearchResult() {
        hideSearchTypeView();
    }

}
