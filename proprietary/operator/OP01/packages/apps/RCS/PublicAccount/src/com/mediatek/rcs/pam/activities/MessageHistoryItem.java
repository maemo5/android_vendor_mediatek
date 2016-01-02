package com.mediatek.rcs.pam.activities;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.activities.MessageHistoryActivity.DownloadListener;
import com.mediatek.rcs.pam.activities.MessageHistoryActivity.Downloader;
import com.mediatek.rcs.pam.model.ResultCode;
import com.mediatek.rcs.pam.provider.PAContract.MessageHistorySummaryColumns;
import com.mediatek.rcs.pam.provider.PAContract.SearchColumns;

import java.io.File;

public class MessageHistoryItem {
    private final Context mContext;
    public long id = Constants.INVALID;
    public String accountUuid;
    public String accountName;
    public long logoId = Constants.INVALID;
    public String logoPath;
    public String logoUrl;
    public Bitmap logoBitmap = null;
    public long lastMessageId = Constants.INVALID;
    public String lastMessageSummary;
    public long lastMessageTimestamp;

    public MessageHistoryItemView view;

    public MessageHistoryItem(Context context, Downloader downloader, Cursor cursor, boolean search) {
        mContext = context;
        loadFromCursor(downloader, cursor, search);
    }

    public void loadFromCursor(Downloader downloader, Cursor cursor, boolean search) {
        if (search) {
            id = cursor.getLong(cursor.getColumnIndexOrThrow(SearchColumns.ACCOUNT_ID));
            accountName = cursor.getString(cursor.getColumnIndexOrThrow(SearchColumns.ACCOUNT_NAME));
            logoId = cursor.getLong(cursor.getColumnIndexOrThrow(SearchColumns.ACCOUNT_LOGO_ID));
            logoPath = cursor.getString(cursor.getColumnIndexOrThrow(SearchColumns.ACCOUNT_LOGO_PATH));
            logoUrl = cursor.getString(cursor.getColumnIndexOrThrow(SearchColumns.ACCOUNT_LOGO_URL));
            lastMessageId = cursor.getLong(cursor.getColumnIndexOrThrow(SearchColumns.MESSAGE_ID));
            lastMessageSummary = cursor.getString(cursor.getColumnIndexOrThrow(SearchColumns.MESSAGE_SUMMARY));
            lastMessageTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(SearchColumns.MESSAGE_TIMESTAMP));
            // load image
            if (logoPath != null) {
                File file = new File(logoPath);
                if (file.exists()) {
                    logoBitmap = (new BitmapDrawable(mContext.getResources(), logoPath)).getBitmap();
                } else {
                    downloadLogo(downloader);
                }
            } else {
                downloadLogo(downloader);
            }
        } else {
            id = cursor.getLong(cursor.getColumnIndexOrThrow(MessageHistorySummaryColumns._ID));
            accountUuid = cursor.getString(cursor.getColumnIndexOrThrow(MessageHistorySummaryColumns.UUID));
            accountName = cursor.getString(cursor.getColumnIndexOrThrow(MessageHistorySummaryColumns.NAME));
            logoId = cursor.getLong(cursor.getColumnIndexOrThrow(MessageHistorySummaryColumns.LOGO_ID));
            logoPath = cursor.getString(cursor.getColumnIndexOrThrow(MessageHistorySummaryColumns.LOGO_PATH));
            logoUrl = cursor.getString(cursor.getColumnIndexOrThrow(MessageHistorySummaryColumns.LOGO_URL));
            lastMessageId = cursor.getLong(cursor.getColumnIndexOrThrow(MessageHistorySummaryColumns.LAST_MESSAGE_ID));
            lastMessageSummary = cursor.getString(cursor
                    .getColumnIndexOrThrow(MessageHistorySummaryColumns.LAST_MESSAGE_SUMMARY));
            lastMessageTimestamp = cursor.getLong(cursor
                    .getColumnIndexOrThrow(MessageHistorySummaryColumns.LAST_MESSAGE_TIMESTAMP));
            // load image
            if (logoPath != null) {
                File file = new File(logoPath);
                if (file.exists()) {
                    logoBitmap = BitmapFactory.decodeFile(logoPath);
                } else {
                    downloadLogo(downloader);
                }
            } else {
                downloadLogo(downloader);
            }
        }
    }

    private void downloadLogo(Downloader downloader) {
        downloader.downloadObject(logoUrl, Constants.MEDIA_TYPE_PICTURE, new DownloadListener() {
            @Override
            public void reportDownloadResult(int resultCode, String path, long mediaId) {
                if (resultCode == ResultCode.SUCCESS) {
                    logoPath = path;
                    logoBitmap = BitmapFactory.decodeFile(logoPath);
                    if (view != null) {
                        view.setLogo(logoBitmap);
                    }
                }
            }
        });
    }
}
