package com.mediatek.rcs.pam.model;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.PAMException;
import com.mediatek.rcs.pam.Utils;
import com.mediatek.rcs.pam.provider.PAContract.MediaBasicColumns;
import com.mediatek.rcs.pam.provider.PAContract.MediaColumns;

public class MediaBasic implements SanityCheck {
    public String thumbnailUrl;
    public String originalUrl;
    public String title;
    public String fileSize;
    public String duration;
    public String fileType;
    public String publicAccountUuid;
    public long createTime;
    public String mediaUuid;
    public String description;
    
    // Android Specific
    public long id = Constants.INVALID;
    public long thumbnailId = Constants.INVALID;
    public long originalId = Constants.INVALID;
    public String originalPath;
    public String thumbnailPath;
    public long accountId = Constants.INVALID;
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{_class:\"MediaBasic\", thumbnailUrl:\"")
        .append(thumbnailUrl)
        .append("\", originalUrl:\"")
        .append(originalUrl)
        .append("\", title:\"")
        .append(title)
        .append("\", fileSize:\"")
        .append(fileSize)
        .append("\", duration:\"")
        .append(duration)
        .append("\", fileType:\"")
        .append(fileType)
        .append("\", publicAccountUuid:\"")
        .append(publicAccountUuid)
        .append("\", createTime:\"")
        .append(createTime)
        .append("\", mediaUuid:\"")
        .append(mediaUuid)
        .append("\"}");
        return sb.toString();
    }

    @Override
    public void checkSanity() throws PAMException {
        Utils.throwIf(ResultCode.PARAM_ERROR_MANDATORY_MISSING,
                (TextUtils.isEmpty(thumbnailUrl) ||
                 TextUtils.isEmpty(originalUrl) ||
                 TextUtils.isEmpty(mediaUuid)));
    }
    
    public void storeToContentValues(ContentValues cv) {
        if (id != Constants.INVALID) {
            cv.put(MediaBasicColumns._ID, id);
        }
        cv.put(MediaBasicColumns.TITLE, title);
        cv.put(MediaBasicColumns.FILE_SIZE, fileSize);
        cv.put(MediaBasicColumns.DURATION, duration);
        cv.put(MediaBasicColumns.FILE_TYPE, fileType);
        cv.put(MediaBasicColumns.ACCOUNT_ID, accountId);
        cv.put(MediaBasicColumns.CREATE_TIME, createTime);
        cv.put(MediaBasicColumns.MEDIA_UUID, mediaUuid);
        cv.put(MediaBasicColumns.THUMBNAIL_ID, thumbnailId);
        cv.put(MediaBasicColumns.ORIGINAL_ID, originalId);
        cv.put(MediaBasicColumns.DESCRIPTION, description);
    }
    
    /**
     * Store original media information to ContentValues.
     * @param cv
     * @param type type code from MessageContent
     */
    public void storeOriginalToContentValues(ContentValues cv, int type) {
        if (originalId != Constants.INVALID) {
            cv.put(MediaColumns._ID, id);
        }
        cv.put(MediaColumns.TYPE, type);
        cv.put(MediaColumns.TIMESTAMP, createTime);
        cv.put(MediaColumns.PATH, originalPath);
        cv.put(MediaColumns.URL, originalUrl);
    }
    
    /**
     * Store thumbnail information to ContentValues.
     * @param cv
     * @param type type code from MessageContent
     */
    public void storeThumbnailToContentValues(ContentValues cv, int type) {
        if (thumbnailId != Constants.INVALID) {
            cv.put(MediaColumns._ID, id);
        }
        cv.put(MediaColumns.TYPE, type);
        cv.put(MediaColumns.TIMESTAMP, createTime);
        cv.put(MediaColumns.PATH, thumbnailPath);
        cv.put(MediaColumns.URL, thumbnailUrl);
    }

    public static String[] sFullProjection = {
        MediaBasicColumns._ID,
        MediaBasicColumns.TITLE,
        MediaBasicColumns.FILE_SIZE,
        MediaBasicColumns.DURATION,
        MediaBasicColumns.FILE_TYPE,
        MediaBasicColumns.ACCOUNT_ID,
        MediaBasicColumns.CREATE_TIME,
        MediaBasicColumns.MEDIA_UUID,
        MediaBasicColumns.THUMBNAIL_ID,
        MediaBasicColumns.ORIGINAL_ID,
        MediaBasicColumns.DESCRIPTION,
    };

    public static MediaBasic loadFromProvider(long mediaId, ContentResolver cr) {
        MediaBasic result = null;
        Cursor c = null;
        try {
            c = cr.query(
                    MediaBasicColumns.CONTENT_URI,
                    sFullProjection,
                    MediaColumns._ID + "=?",
                    new String[]{Long.toString(mediaId)},
                    null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                result = new MediaBasic();
                result.id = mediaId;
                result.accountId = c.getLong(c.getColumnIndexOrThrow(MediaBasicColumns.ACCOUNT_ID));
                result.title = c.getString(c.getColumnIndexOrThrow(MediaBasicColumns.TITLE));
                result.fileSize = c.getString(c.getColumnIndexOrThrow(MediaBasicColumns.FILE_SIZE));
                result.duration = c.getString(c.getColumnIndexOrThrow(MediaBasicColumns.FILE_TYPE));
                result.createTime = Utils.convertStringToTimestamp(
                        c.getString(c.getColumnIndexOrThrow(MediaBasicColumns.CREATE_TIME)));
                result.mediaUuid = c.getString(c.getColumnIndexOrThrow(MediaBasicColumns.MEDIA_UUID));
                result.description = c.getString(c.getColumnIndexOrThrow(MediaBasicColumns.DESCRIPTION));
                result.thumbnailId = c.getLong(c.getColumnIndexOrThrow(MediaBasicColumns.THUMBNAIL_ID));
                if (result.originalId != Constants.INVALID) {
                    MediaEntry me = MediaEntry.loadFromProvider(result.originalId, cr);
                    result.thumbnailUrl = me.url;
                    result.thumbnailPath = me.path;
                }
                result.originalId = c.getLong(c.getColumnIndexOrThrow(MediaBasicColumns.ORIGINAL_ID));
                if (result.originalId != Constants.INVALID) {
                    MediaEntry me = MediaEntry.loadFromProvider(result.originalId, cr);
                    result.originalUrl = me.url;
                    result.originalPath = me.path;
                }
                // TODO load public account UUID
                return result;
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return result;
    }
}
