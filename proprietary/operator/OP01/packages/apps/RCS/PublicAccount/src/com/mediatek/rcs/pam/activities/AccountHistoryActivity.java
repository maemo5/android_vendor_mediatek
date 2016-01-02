package com.mediatek.rcs.pam.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.PAMException;
import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.Utils;
import com.mediatek.rcs.pam.model.MessageContent;

import java.util.ArrayList;
import java.util.List;

/**
 * This activity shows the history messages sent to all followers of some certain account.
 */
@SuppressLint("InflateParams")
public class AccountHistoryActivity extends ClientQueryActivity<MessageContent> {

    // 0: forward(previous to now) 1: backward (default)
    private static final int ORDER_FORWARD = Constants.ORDER_BY_TIMESTAMP_ASCENDING;
    private static final int ORDER_BACKWARD = Constants.ORDER_BY_TIMESTAMP_DESCENDING;

    // message number per request
    private static final int DEFAULT_MESSAGE_SIZE = 8;

    private int mOrder = ORDER_BACKWARD;
    private int mPageSize = DEFAULT_MESSAGE_SIZE;
    private String mTimeStamp;
    private String mUuid;
    private PAAudioService mAudioServcie;

    private TextView mNoResultTextView;
    private AccountHistoryAdapter mSearchAdapter;

    public static final String KEY_ACCOUNT_UUID = "key_account_uuid";
    public static final String KEY_ACCOUNT_TITLE = "key_account_title";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isServiceConnected()) {
            showOuterProgressBar();
        }

        mUuid = getIntent().getStringExtra(AccountHistoryActivity.KEY_ACCOUNT_UUID);
        mTimeStamp = Utils.covertTimestampToString(Utils.currentTimestamp());
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_account_history;
    }

    @Override
    protected String getLogTag() {
        return Constants.TAG_PREFIX + "AccountHistoryActivity";
    }

    @Override
    protected void onPause() {
        Log.i(mTag, "onPause");
        if (mAudioServcie.getServiceStatus() >= PAAudioService.PLAYER_STATE_READY) {
            mAudioServcie.stopAudio();
            mAudioServcie.resetService();
        }
        super.onPause();
    }

    @Override
    protected void onStart() {
        Log.i(mTag, "onStart");
        mAudioServcie = PAAudioService.getService();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mAudioServcie.releaseService();
        super.onStop();
    }

    @Override
    protected void initBasicViews() {
        mNoResultTextView = (TextView) findViewById(R.id.tv_no_history_message);
        mNoResultTextView.setVisibility(View.GONE);

        mOuterProgressBar = (ProgressBar) findViewById(R.id.pb_account_history);
        mOuterProgressBar.setVisibility(View.GONE);

        mSearchListView = (ListView) findViewById(R.id.list_account_history);
        mSearchListView.setVisibility(View.GONE);
        mSearchListView.setDividerHeight(5);

        mSearchAdapter = new AccountHistoryAdapter(this, new ArrayList<MessageContent>(),
                mDownloader);
        mSearchListView.setAdapter(mSearchAdapter);

        mSearchListView.setRecyclerListener(new AbsListView.RecyclerListener() {

            @Override
            public void onMovedToScrapHeap(View view) {
                if (view != null && view instanceof AccountHistoryMsgItemView) {
                    AccountHistoryMsgItemView messageView = (AccountHistoryMsgItemView) view;
                    int position = messageView.getPosition();
                    Log.i(mTag, "onMovedToScrapHeap " + position + ", view " + view);
                    messageView.unbind();
                }
            }
        });

        super.initBasicViews();
    }

    /*
     * When connected to service, update title, reset page number and trigger first query.
     */
    @Override
    protected void doWhenConnected() {
        super.doWhenConnected();
        updateTitle();
        startFirstQuery();
    }

    @Override
    protected List<MessageContent> searchDataViaClient() throws PAMException {
        return mUIPAMClient.getMessageHistory(mUuid, mTimeStamp, mOrder, mPageSize, mPageNum);
    }

    @Override
    protected void updateListonUI(List<MessageContent> listSearch, int type) {
        final List<MessageContent> dataList;
        if (type == LOAD_MORE_DATA) {
            dataList = mSearchAdapter.getMessageList();
            Log.i(mTag, "[updateListonUI] old size is " + dataList.size());
            dataList.addAll(listSearch);
        } else {
            dataList = listSearch;
        }

        runOnUiThread(new Runnable() {
            public void run() {
                mSearchListView.setVisibility(View.VISIBLE);
                mSearchListView.requestLayout();
                mSearchAdapter.setMessageList(dataList);
                Log.i(mTag, "[updateListonUI] new size " + dataList.size());
            }
        });
    }

    @Override
    protected void showNoSearchResult() {
        runOnUiThread(new Runnable() {
            public void run() {
                mNoResultTextView.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void hideNoSearchResult() {
        runOnUiThread(new Runnable() {
            public void run() {
                mNoResultTextView.setVisibility(View.GONE);
            }
        });
    }

    /*
     * Update activity using the account name
     */
    private void updateTitle() {
        String title = getIntent().getStringExtra(AccountHistoryActivity.KEY_ACCOUNT_TITLE);
        getActionBar().setTitle(title);
    }
}
