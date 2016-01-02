package com.mediatek.rcs.message.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.rcs.common.provider.ThreadMapData;

public class ThreadMapProvider extends ContentProvider {

    /**
     * Database tables
     */
    public static final String TABLE_MAP = "threadmap";
    // Create the constants used to differentiate between the different URI requests
    private static final int THREAD_MAP         = 1;
    private static final int THREAD_MAP_ID      = 2;
    
    private static final String TAG = "RCS/Provider/ThreadMapProvider";

    // Allocate the UriMatcher object
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI(ThreadMapData.AUTHORITY, null, THREAD_MAP);
        uriMatcher.addURI(ThreadMapData.AUTHORITY, "#", THREAD_MAP_ID);
    }

    /**
     * Database helper class
     */
    private SQLiteOpenHelper mOpenHelper;

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        Log.d(TAG, "delete uri=" + uri.toString() + ", where=" + where + ", whereArgs=" + whereArgs);
        int count = 0;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int match = uriMatcher.match(uri);
        switch(match) {
            case THREAD_MAP:
                count = db.delete(TABLE_MAP, where, whereArgs);
                break;
            case THREAD_MAP_ID:
                String finalSelection = concatSelections(where, "_id=" + uri.getLastPathSegment());
                count = db.delete(TABLE_MAP, finalSelection, null);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        int match = uriMatcher.match(uri);
        switch(match) {
            case THREAD_MAP:
                return "vnd.android.cursor.dir/threadmap";
            case THREAD_MAP_ID:
                return "vnd.android.cursor.item/threadmap";
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        Log.d(TAG, "insert uri=" + uri.toString() + ", values=" + values);
        long id = 0;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int match = uriMatcher.match(uri);
        switch(match) {
            case THREAD_MAP:
            case THREAD_MAP_ID:
                id = db.insert(TABLE_MAP, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        return ContentUris.withAppendedId(ThreadMapData.CONTENT_URI, id);
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = RCSMessageDatabaseHelper.getInstance(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        // TODO Auto-generated method stub
        Log.d(TAG, "query uri=" + uri.toString() + ", projection=" + projection + ", selection=" + 
                    selection + ", selectionArgs=" + selectionArgs + ", sortOrder=" + sortOrder);
        Cursor cursor = null;
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        int match = uriMatcher.match(uri);
        switch(match) {
            case THREAD_MAP:
                cursor = db.query(TABLE_MAP, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case THREAD_MAP_ID:
                String rowId = uri.getLastPathSegment();
                selection = concatSelections(selection, "_id=" + rowId);
                cursor = db.query(TABLE_MAP, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where,
            String[] whereArgs) {
        // TODO Auto-generated method stub
        Log.d(TAG, "update uri=" + uri.toString() + ", values=" + values + ", where=" +
                    where + ", whereArgs=" + whereArgs);
        int count = 0;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int match = uriMatcher.match(uri);
        switch(match) {
            case THREAD_MAP:
                count = db.update(TABLE_MAP, values, where, whereArgs);
                break;
            case THREAD_MAP_ID:
                where = concatSelections(where, ThreadMapData.KEY_ID + "=" + uri.getLastPathSegment());
                count = db.update(TABLE_MAP, values, where, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        return count;
    }

    private String concatSelections(String selection1, String selection2) {
        if (TextUtils.isEmpty(selection1)) {
            return selection2;
        } else if (TextUtils.isEmpty(selection2)) {
            return selection1;
        } else {
            return selection1 + " AND " + selection2;
        }
    }
}
