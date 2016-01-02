package com.mediatek.rcs.pam.model;

import android.content.ContentResolver;
import android.database.Cursor;

import com.mediatek.rcs.pam.provider.PAContract.MediaColumns;

public class MediaEntry {
    public long id;
    public int type;
    public String url;
    public String path;
    public String timestamp;
    
    public static String[] sFullProjection = {
        MediaColumns._ID,
        MediaColumns.TYPE,
        MediaColumns.TIMESTAMP,
        MediaColumns.PATH,
        MediaColumns.URL,
    };
    
    public static MediaEntry loadFromProvider(long mediaId, ContentResolver cr) {
        MediaEntry result = null;
        Cursor c = null;
        try {
            c = cr.query(
                    MediaColumns.CONTENT_URI,
                    sFullProjection,
                    MediaColumns._ID + "=?",
                    new String[]{Long.toString(mediaId)},
                    null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                result = new MediaEntry();
                result.id = mediaId;
                result.type = c.getInt(c.getColumnIndexOrThrow(MediaColumns.TYPE));
                result.url = c.getString(c.getColumnIndexOrThrow(MediaColumns.URL));
                result.path = c.getString(c.getColumnIndexOrThrow(MediaColumns.PATH));
                result.timestamp = c.getString(c.getColumnIndexOrThrow(MediaColumns.TIMESTAMP));
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
