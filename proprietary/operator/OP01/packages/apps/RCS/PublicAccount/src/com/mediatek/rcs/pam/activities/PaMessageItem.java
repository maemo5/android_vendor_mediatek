/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.rcs.pam.activities;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.vcard.VCardEntry;
import com.android.vcard.VCardEntryHandler;
import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.activities.PaMessageListAdapter.ColumnsMap;
import com.mediatek.rcs.pam.model.MediaArticle;
import com.mediatek.rcs.pam.model.MediaBasic;
import com.mediatek.rcs.pam.provider.PAContract.MediaArticleColumns;
import com.mediatek.rcs.pam.provider.PAContract.MediaBasicColumns;
import com.mediatek.rcs.pam.provider.PAContract.MediaColumns;
import com.mediatek.rcs.pam.util.PaVcardParserResult;
import com.mediatek.rcs.pam.util.PaVcardUtils;


import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.Telephony.MmsSms;
import android.util.Log;

/**
 * Mostly immutable model for an SMS/MMS message.
 *
 * <p>The only mutable field is the cached formatted message member,
 * the formatting of which is done outside this model in MessageListItem.
 */
public class PaMessageItem {
    private static String TAG = "PA/PaMessageItem";

    public enum DeliveryStatus  { NONE, INFO, FAILED, PENDING, RECEIVED }

    private static final String[] MEDIA_BASIC_PROJECTION = new String[] { 
        MediaBasicColumns._ID,
        MediaBasicColumns.TITLE,
        MediaBasicColumns.FILE_SIZE,
        MediaBasicColumns.DURATION,
        MediaBasicColumns.FILE_TYPE,
        MediaBasicColumns.ACCOUNT_ID,
        MediaBasicColumns.CREATE_TIME,
        MediaBasicColumns.MEDIA_UUID,            
        MediaBasicColumns.THUMBNAIL_ID, 
        MediaBasicColumns.ORIGINAL_ID            
    }; 
    
   
    private static final String[] MEDIA_PROJECTION = new String[] {
        MediaColumns._ID,
        MediaColumns.TYPE,
        MediaColumns.TIMESTAMP,
        MediaColumns.URL,
        MediaColumns.PATH,
    };    
    
    private static final String[] MEDIA_ARTICLE_PROJECTION = new String[] {
        MediaArticleColumns._ID,
        MediaArticleColumns.TITLE,
        MediaArticleColumns.AUTHOR,
        MediaArticleColumns.TEXT,
        MediaArticleColumns.THUMBNAIL_ID, 
        MediaArticleColumns.ORIGINAL_ID,
        MediaArticleColumns.SOURCE_URL, 
        MediaArticleColumns.BODY_URL,
        MediaArticleColumns.TEXT,
        MediaArticleColumns.FILE_TYPE,
        MediaArticleColumns.MEDIA_UUID
    };
    
    final Context mContext;
    
    final int mMsgId;
    String mUuid;
    int mSrcId;
    int mSrcTable;
    String mChatId;
    long mAccountId;
    final int mType;
    long mTimeStamp;
    long mCreateTimeStamp;
    String mTimeStampStr;
    String mSmsDigest;
    String mText;
    int mDirection;
    int mForwardable;
    int mStatus;
    
    //add for vCard
    int mVcardCount;
    String mVcardName;
    Bitmap mVcardBitmap;
    PaVcardEntryhandler mVcardEntryHandler;
    
    
    ArrayList<Integer> mDatas = new ArrayList<Integer>();
    public MediaBasic mMediaBasic; //for single msg
    public ArrayList<MediaArticle> mMediaArticles = new ArrayList<MediaArticle>(); //for mix msg
    
    private DownloadCallback mDownloadCallback;
    
    private long[] mDownloadIds = new long[5];
    //private long mDownloadObjectId;

    DeliveryStatus mDeliveryStatus;
    boolean mReadReport;
    boolean mLocked;            // locked to prevent auto-deletion

    String mTimestamp;
    String mAddress;
    String mContact;
    String mBody; // Body of SMS, first text of MMS.
    String mTextContentType; // ContentType of text of MMS.
    Pattern mHighlight; // portion of message to highlight (from search)

    // The only non-immutable field.  Not synchronized, as access will
    // only be from the main GUI thread.  Worst case if accessed from
    // another thread is it'll return null and be set again from that
    // thread.
    CharSequence mCachedFormattedMessage;

    // The last message is cached above in mCachedFormattedMessage. In the latest design, we
    // show "Sending..." in place of the timestamp when a message is being sent. mLastSendingState
    // is used to keep track of the last sending state so that if the current sending state is
    // different, we can clear the message cache so it will get rebuilt and recached.
    boolean mLastSendingState;

    // Fields for MMS only.
    Uri mMessageUri;
    int mMessageType;
    int mAttachmentType;
    String mSubject;
    //SlideshowModel mSlideshow;
    int mMessageSize;
    int mErrorType;
    int mErrorCode;
    Cursor mCursor;
    ColumnsMap mColumnsMap;
    
    //lry add
    //private SoftReference<Bitmap> mBitmapCache = new SoftReference<Bitmap>(null);
    private ArrayList<SoftReference<Bitmap>> mBitmapCaches = new ArrayList<SoftReference<Bitmap>>();
    private int[] mCacheBitmapWidths = new int[5];
    private int[] mCacheBitmapHeights = new int[5];
    
    public Bitmap getMessageBitmap(int index) {
        return mBitmapCaches.get(index).get();
    }
    
    public void setMessageBitmapCache(Bitmap bitmap, int index) {
        if (null != bitmap) {
            SoftReference<Bitmap> bitmapCache = new SoftReference<Bitmap>(bitmap);
            mBitmapCaches.add(index, bitmapCache);
        }
    }
    
    public void setMessageBitmapSize(int width, int height, int index) {
        mCacheBitmapWidths[index] = width;
        mCacheBitmapHeights[index] = height;
    }
    
    public int getMessageBitmapWidth(int index) {
        return mCacheBitmapWidths[index];
    }
    
    public int getMessageBitmapHeight(int index) {
        return mCacheBitmapHeights[index];
    }
    
    PaMessageItem(Context context, final Cursor cursor,
            final ColumnsMap columnsMap, Pattern highlight) throws Exception {
        Log.d(TAG, "PaMessageItem().");

        mContext = context;
        
        for (int i = 0; i < 5 ; i++) {
            SoftReference<Bitmap> bitmapCache = new SoftReference<Bitmap>(null);
            mBitmapCaches.add(bitmapCache);
        }
        
        mCursor = cursor;
        mHighlight = highlight;
        mColumnsMap = columnsMap;
        
        mMsgId = cursor.getInt(columnsMap.mColumnMsgId);
        mUuid = cursor.getString(columnsMap.mColumnUuid);
        mSrcId = cursor.getInt(columnsMap.mColumnSrcId);
        mSrcTable = cursor.getInt(columnsMap.mColumnSrcTable);
        mChatId = cursor.getString(columnsMap.mColumnChatId);
        mAccountId = cursor.getLong(columnsMap.mColumnAccountId);
        mType = cursor.getInt(columnsMap.mColumnMsgType);//type;
        mTimeStamp = cursor.getLong(columnsMap.mColumnTimeStamp);
        mTimeStampStr = RcsMessageUtils.formatTimeStampString(mContext, mTimeStamp, false);
        //Utils.covertTimestampToString(mTimeStamp);
        mCreateTimeStamp = cursor.getLong(columnsMap.mColumnCreateTimeStamp);
        mSmsDigest = cursor.getString(columnsMap.mColumnSmsDigest);
        mText = cursor.getString(columnsMap.mColumnText);
        mDirection = cursor.getInt(columnsMap.mColumnDirection);
        mForwardable = cursor.getInt(columnsMap.mColumnFowardable);
        mStatus = cursor.getInt(columnsMap.mColumnStatus);
        for (int i = 0 ; i < 5 ; i++) {
            if (!cursor.isNull(columnsMap.mColumnData1 + i)) {
                Integer data = cursor.getInt(columnsMap.mColumnData1 + i);
                if (data > -1) {
                    mDatas.add(data);
                }                
            } else {
                break;
            }
        }
        loadAllMediaInfo();
        dumpItemInfo();
    }
    
    public int loadAllMediaInfo() {
        Log.d(TAG, "loadAllMediaInfo() data size:" + mDatas.size());
        if (0 == mDatas.size()) {
            return 0;
        }

        switch (mType) {
        case Constants.MEDIA_TYPE_TEXT:
        case Constants.MEDIA_TYPE_PICTURE:
        case Constants.MEDIA_TYPE_VIDEO:
        case Constants.MEDIA_TYPE_AUDIO:
        case Constants.MEDIA_TYPE_SMS:
            if (mDatas.size() > 1) {
                Log.e(TAG, "Single media data wrong !!!!!");
            }
            mMediaBasic = loadMediaBasic(mDatas.get(0));
            break;
        case Constants.MEDIA_TYPE_VCARD:
        case Constants.MEDIA_TYPE_GEOLOC:
            mMediaBasic = loadMediaOnly(mDatas.get(0));
            break;
            
        case Constants.MEDIA_TYPE_SINGLE_ARTICLE:
            if (mDatas.size() > 1) {
                Log.e(TAG, "Single article data wrong !!!!!");
            }
            mMediaArticles.add(loadMediaArticle(mDatas.get(0)));
            break;

        case Constants.MEDIA_TYPE_MULTIPLE_ARTICLE:
            for (int i = 0 ; i < mDatas.size() ; i ++) {
                mMediaArticles.add(loadMediaArticle(mDatas.get(i)));
            }
            Log.d(TAG, "loadAllMediaInfo() articleSize=" + mMediaArticles.size());
            break;
        default:
            break;
        }
        return mDatas.size();
    }
    
    public MediaBasic loadMediaOnly(int key) {
        Log.i(TAG, "loadMediaOnly key=" + key);
        
        MediaBasic mediaBasic = new MediaBasic();
        MediaInfo mediaInfo = loadMediaInfo(key);
        if (mediaInfo.path != null) {
            mediaBasic.originalPath = new String(mediaInfo.path);
            if (mType == Constants.MEDIA_TYPE_VCARD) {
                mVcardCount = PaVcardUtils.getVcardEntryCount(mediaBasic.originalPath);
                if (mVcardCount == 1) {
                    mVcardEntryHandler = new PaVcardEntryhandler();
                    PaVcardUtils.parseVcard(mediaBasic.originalPath, mVcardEntryHandler);
                }
            } else if (mType == Constants.MEDIA_TYPE_GEOLOC) {
                //Do nothing currently
            }
        }
        
        Log.i(TAG, "loadMediaOnly path=" + mediaBasic.originalPath);
        return mediaBasic;
    }
    
    private class PaVcardEntryhandler implements VCardEntryHandler {

        @Override
        public void onEnd() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onEntryCreated(VCardEntry entry) {
            PaVcardParserResult result = PaVcardUtils.ParserRcsVcardEntry(entry, mContext);
            
            mVcardName = result.getName();
            byte[] pic = result.getPhoto();
            if (pic != null) {
                mVcardBitmap = BitmapFactory.decodeByteArray(pic, 0, pic.length);
            }
        }

        @Override
        public void onStart() {
            // TODO Auto-generated method stub
            
        }
    }
    
    
    public MediaBasic loadMediaBasic(int key) {
        Log.i(TAG, "loadMediaBasic key=" + key);
        Cursor cursor = null; 
        MediaBasic mediaBasic = new MediaBasic();

        cursor = mContext.getContentResolver().query(
                MediaBasicColumns.CONTENT_URI,
                MEDIA_BASIC_PROJECTION,
                MediaBasicColumns._ID + "=" + key,
                null,
                null);
        if (null == cursor) {
            return mediaBasic;
        }
        Log.i(TAG, "loadMediaBasic count=" + cursor.getCount());
        
        if (cursor.moveToFirst()) {
            mediaBasic.id = key;
            mediaBasic.fileSize = cursor.getString(cursor.getColumnIndex(MediaBasicColumns.FILE_SIZE));
            mediaBasic.fileType = cursor.getString(cursor.getColumnIndex(MediaBasicColumns.FILE_TYPE));
            mediaBasic.duration = cursor.getString(cursor.getColumnIndex(MediaBasicColumns.DURATION));
            mediaBasic.thumbnailId = cursor.getInt(cursor.getColumnIndex(MediaBasicColumns.THUMBNAIL_ID));
            mediaBasic.originalId = cursor.getInt(cursor.getColumnIndex(MediaBasicColumns.ORIGINAL_ID));
        }
        cursor.close();
        
        //format fileSize string
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher matcher;
        if (null != mediaBasic.fileSize) {            
            matcher = pattern.matcher(mediaBasic.fileSize);
            if (!matcher.matches()) {
                mediaBasic.fileSize = mediaBasic.fileSize.substring(0, mediaBasic.fileSize.length() - 1);
            }
        }
        if (null != mediaBasic.duration) {
            matcher = pattern.matcher(mediaBasic.duration);
            if (!matcher.matches()) {
                mediaBasic.duration = mediaBasic.fileSize.substring(0, mediaBasic.duration.length() - 1);
            }
        }        
        
        MediaInfo mediaInfo = loadMediaInfo(mediaBasic.thumbnailId);
        if (mediaInfo.id == mediaBasic.thumbnailId) {
            if (null != mediaInfo.path) {
                mediaBasic.thumbnailPath = new String(mediaInfo.path);
            }
            if (null != mediaInfo.url) {
                mediaBasic.thumbnailUrl = new String(mediaInfo.url);
            }            
        }
        mediaInfo = loadMediaInfo(mediaBasic.originalId);
        if (mediaInfo.id == mediaBasic.originalId) {
            if (mediaInfo.path != null) {
                mediaBasic.originalPath = new String(mediaInfo.path);
            } else {
                mediaBasic.originalPath = null;
            }
            if (null != mediaInfo.url) {
                mediaBasic.originalUrl = new String(mediaInfo.url);
            }
        }
   
        Log.i(TAG, "loadMediaBasic:" + mediaBasic.toString());
        return mediaBasic;
    }
    
    public MediaArticle loadMediaArticle(int key) {
        Log.i(TAG, "loadMediaArticle key=" + key);
        Cursor cursor = null;
        MediaArticle mediaArticle = new MediaArticle();

        cursor = mContext.getContentResolver().query(
                MediaArticleColumns.CONTENT_URI, 
                MEDIA_ARTICLE_PROJECTION, 
                MediaArticleColumns._ID + "=" + key, 
                null,
                null);
        
        if (null == cursor) {
            return mediaArticle;
        }
        Log.i(TAG, "loadMediaArticle count=" + cursor.getCount());

        if (cursor.moveToFirst()) {
            mediaArticle.id = key;
            mediaArticle.title = cursor.getString(cursor.getColumnIndex(MediaArticleColumns.TITLE));
            mediaArticle.author = cursor.getString(cursor.getColumnIndex(MediaArticleColumns.AUTHOR));
            mediaArticle.mainText = cursor.getString(cursor.getColumnIndex(MediaArticleColumns.TEXT));
            mediaArticle.fileType = cursor.getString(cursor.getColumnIndex(MediaArticleColumns.FILE_TYPE));
            mediaArticle.thumbnailId = cursor.getInt(cursor.getColumnIndex(MediaArticleColumns.THUMBNAIL_ID));
            //mediaArticle.originalId = cursor.getInt(cursor.getColumnIndex(MediaArticleColumns.ORIGINAL_ID));
            mediaArticle.sourceUrl = cursor.getString(cursor.getColumnIndex(MediaArticleColumns.SOURCE_URL));
            //mediaArticle.bodyUrl = cursor.getString(cursor.getColumnIndex(MediaArticleColumns.BODY_URL));
            //mediaArticle.mediaUuid = cursor.getString(cursor.getColumnIndex(MediaArticleColumns.MEDIA_UUID));
        }
        cursor.close();
        
        MediaInfo mediaInfo = loadMediaInfo(mediaArticle.thumbnailId);
        if (mediaInfo.id == mediaArticle.thumbnailId) {
            //mediaArticle.thumbnailType = mediaInfo.type;
            if (mediaInfo.url != null) {
                mediaArticle.thumbnailUrl = new String(mediaInfo.url);
            }
            if (mediaInfo.path != null) {
                mediaArticle.thumbnailPath = new String(mediaInfo.path);
            }
        }
        //mediaInfo = loadMediaInfo(mediaArticle.originalId);
        //if (mediaInfo.id == mediaArticle.originalId) {
        //  mediaArticle.originalType = mediaInfo.type;
        //  mediaArticle.originalUrl = new String(mediaInfo.url);
        //}
        Log.i(TAG, "loadMediaArticle:" + mediaArticle.toString());
        return mediaArticle;
    }
    
    class MediaInfo {
        public long id;
        public int type;
        public String timeStamp;
        public String path;
        public String url;
 
        public void clear() {
            id = 0;
            type = 0;
            timeStamp = null;
            path = null;
            url = null;
        }
    }
    
    private MediaInfo loadMediaInfo(long id) {
        Log.i(TAG, "loadMediaInfo key=" + id);
        MediaInfo mediaInfo = new MediaInfo();
        Cursor cursor = null;

        cursor = mContext.getContentResolver().query(
                MediaColumns.CONTENT_URI, 
                MEDIA_PROJECTION, 
                MediaColumns._ID + "=" + id,
                null, 
                null);
        
        if (cursor == null) {
            return mediaInfo;
        }
        Log.i(TAG, "loadMediaInfo count=" + cursor.getCount());
        if (cursor.moveToFirst()) {
            mediaInfo.id = id;
            mediaInfo.type = cursor.getInt(cursor.getColumnIndex(MediaColumns.TYPE));
            mediaInfo.path = cursor.getString(cursor.getColumnIndex(MediaColumns.PATH));
            mediaInfo.url = cursor.getString(cursor.getColumnIndex(MediaColumns.URL));
        }
        cursor.close();
        return mediaInfo;
    }
    
    public void dumpItemInfo() {
        Log.d(TAG, "dumpItemInfo. mMsgId=" +  mMsgId +
                ", mSrcTable=" + mSrcTable +
                ", mChatId=" + mChatId + 
                ", mAccountId=" + mAccountId + 
                ", mType=" + mType + 
                ", mTimeStamp=" + mTimeStamp + 
                ", mSmsDigest=" + mSmsDigest +
                ", mText=" + mText + 
                ", mDirection=" + mDirection +
                ", mForwardable" + mForwardable +
                ", mStatus" + mStatus +
                ", mData=" + mDatas +
                ", mHighlight=" + mHighlight +
                ", mMediaArticles size=" + mMediaArticles.size() + 
                ", mMediaArticles=" + mMediaArticles.toString());
    }

    public boolean isDownloaded() {
        return false;
    }

    public boolean isMe() {
        // Logic matches MessageListAdapter.getItemViewType which is used to decide which
        // type of MessageListItem to create: a left or right justified item depending on whether
        // the message is incoming or outgoing.
        boolean isMe = false;
        if (mDirection == Constants.MESSAGE_DIRECTION_OUTGOING) {
            isMe = true;
        }
        Log.d(TAG, "isMe=" + isMe);
        return isMe;
    }

    public boolean isOutgoingMessage() {
        boolean isOut = false;
        if (mDirection == Constants.MESSAGE_DIRECTION_OUTGOING) {
            isOut = true;
        }
        Log.d(TAG, "isOut=" + isOut);
        return isOut;
    }

    public boolean isSending() {
        return !isFailedMessage() && isOutgoingMessage();
    }

    public boolean isFailedMessage() {
        boolean isFailedMms = (mErrorType >= MmsSms.ERR_TYPE_GENERIC_PERMANENT);
        //boolean isFailedSms = (mBoxId == Sms.MESSAGE_TYPE_FAILED);
        return isFailedMms ;//|| isFailedSms;
    }

    // Note: This is the only mutable field in this class.  Think of
    // mCachedFormattedMessage as a C++ 'mutable' field on a const
    // object, with this being a lazy accessor whose logic to set it
    // is outside the class for model/view separation reasons.  In any
    // case, please keep this class conceptually immutable.
    public void setCachedFormattedMessage(CharSequence formattedMessage) {
        mCachedFormattedMessage = formattedMessage;
    }

    public CharSequence getCachedFormattedMessage() {
        return mCachedFormattedMessage;
    }

    //public int getBoxId() {
    //    return mBoxId;
    //}

    public int getMessageId() {
        return mMsgId;
    }
    
    public interface DownloadCallback {
        void onDownloadFinished(PaMessageItem messageItem, int index);
    }
    
    public void setDownloadCallback(DownloadCallback callback) {
        mDownloadCallback = callback;
    }
    
    public void setDownloadReqId(int index, long reqId) {
        Log.d(TAG, "setDownloadReqId. index=" + index + ". reqId=" + reqId);
        mDownloadIds[index] = reqId;
    }
    
    public void setDownloadResult(long reqId, boolean result, String path) {
        Log.d(TAG, "setDownloadResult(). reqId=" + reqId + ". result=" + result);
        if (reqId <= 0) {
            return;
        }
        for (int i = 0 ; i < 5 ; i++) {
            if (reqId == mDownloadIds[i]) {
                mDownloadIds[i] = 0;
                updateAfterDownload(i, path);
                if (mDownloadCallback != null) {
                    mDownloadCallback.onDownloadFinished(this, i);
                }
                Log.d(TAG, "setDownloadResult(): mDownloadIds:" + mDownloadIds);   
            }
        }

        Log.w(TAG, "setDownloadResult() fail to find this reqId");
    }
    
    private void updateAfterDownload(int index, String path) {
        Log.d(TAG, "updatePath() type=" + mType + ". index=" + index);
        switch(mType) {
        case Constants.MEDIA_TYPE_AUDIO:
            mMediaBasic.originalPath = path;
            break;
        case Constants.MEDIA_TYPE_VIDEO:
        case Constants.MEDIA_TYPE_PICTURE:
            mMediaBasic.thumbnailPath = path;
            break;
        case Constants.MEDIA_TYPE_SINGLE_ARTICLE:
        case Constants.MEDIA_TYPE_MULTIPLE_ARTICLE:
            mMediaArticles.get(index).thumbnailPath = path;
            break;
        default:
            break;
        }
    }
    
    public int getType() {
        return mType;
    }

    public String getText() {
        return mText;
    }
    
    public String getTimeStamp() {
        return mTimeStampStr;
    }

    @Override
    public String toString() {
        return null;
    }

}
