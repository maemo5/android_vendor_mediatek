package com.mediatek.rcs.pam.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.activities.MessageHistoryActivity.Downloader;
import com.mediatek.rcs.pam.activities.MessageHistoryActivity.Mode;
import com.mediatek.rcs.pam.provider.PAContract;

public class SearchResultAdaptor extends CursorAdapter {
	private static final int CACHE_SIZE = 50;
	private LruCache<Long, MessageHistoryItem> mMessageHistoryItemCache;
	private LayoutInflater mInflater;
	private final Downloader mDownloader;
	private final MessageHistoryActivity mActivity;

	public SearchResultAdaptor(Context context, Cursor c, Downloader downloader, MessageHistoryActivity activity) {
		super(context, c, FLAG_REGISTER_CONTENT_OBSERVER);
		mMessageHistoryItemCache = new LruCache<Long, MessageHistoryItem>(CACHE_SIZE);
		mInflater = LayoutInflater.from(context);
		mDownloader = downloader;
		mActivity = activity;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		if (view instanceof MessageHistoryItemView) {
			MessageHistoryItemView v = (MessageHistoryItemView) view;
			long accountId = cursor.getLong(cursor.getColumnIndexOrThrow(PAContract.MessageHistorySummaryColumns._ID));
			v.bind(getMessageItem(accountId, context, cursor), cursor.getPosition());
		}
	}
	
	private MessageHistoryItem getMessageItem(long accountId, Context context, Cursor cursor) {
		MessageHistoryItem result = mMessageHistoryItemCache.get(Long.valueOf(accountId));
		if (result == null) {
			result = new MessageHistoryItem(context, mDownloader, cursor, true);
			mMessageHistoryItemCache.put(Long.valueOf(accountId), result);
		}
		return result;
	}

    @SuppressLint("InflateParams")
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // the parent to inflate() MUST be null
        return mInflater.inflate(R.layout.message_history_item, null);
	}
	
	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
		mMessageHistoryItemCache.evictAll();
	}
	
	@Override
	public void onContentChanged() {
		super.onContentChanged();
		if (mActivity.getMode() == Mode.SEARCH_RESULT_LIST) {
			mActivity.startMessageSearchQuery();
		}
	}

}
