package com.mediatek.rcs.message.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

//import com.mediatek.rcs.message.provider.ThreadMapData;
import com.mediatek.rcs.common.provider.GroupMemberData;
import com.mediatek.rcs.common.provider.RejoinData;
import com.mediatek.rcs.common.provider.SpamMsgData;
import com.mediatek.rcs.common.provider.ThreadMapData;
import com.mediatek.rcs.message.provider.ThreadMapProvider;

public class RCSMessageDatabaseHelper extends SQLiteOpenHelper {

    /**
     * Database name
     */
    public static final String DATABASE_NAME = "rcsmessage.db";
    public static final int    DATABASE_VERSION = 4;

    private static RCSMessageDatabaseHelper sInstance = null;

    private RCSMessageDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized RCSMessageDatabaseHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new RCSMessageDatabaseHelper(context);
        }
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createSpamTable(db);
        createMappingTable(db);
        createFavoriteTable(db);
        createGroupMemberTable(db);
        createRejoinTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch(oldVersion) {
            case 1:
                if (newVersion <= 1) {
                    return;
                }
                updateToVersion2(db);
            case 2:
                if (newVersion <= 2) {
                    return;
                }
                updateToVersion3(db);
            case 3:
                if (newVersion <=3) {
                    return;
                }
                updateToVersion4(db);
            default:
                break;
        }
    }

    private void createSpamTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + SpamMsgProvider.TABLE_SPAM + " (" +
                SpamMsgData.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                SpamMsgData.COLUMN_BODY + " TEXT," +
                SpamMsgData.COLUMN_DATE + " INTEGER DEFAULT 0," +
                SpamMsgData.COLUMN_ADDRESS + " TEXT," +
                SpamMsgData.COLUMN_TYPE + " INTEGER DEFAULT 0," +
                SpamMsgData.COLUMN_SUB_ID + " LONG DEFAULT -1," +
                SpamMsgData.COLUMN_IPMSG_ID + " INTEGER DEFAULT 0" +
                ");");

    }

    private void createGroupMemberTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + GroupMemberProvider.TABLE_GROUPMEMBER + " ("
                + GroupMemberData.COLUMN_ID + " INTEGER primary key autoincrement,"
                + GroupMemberData.COLUMN_CHAT_ID + " TEXT,"
                + GroupMemberData.COLUMN_CONTACT_NUMBER + " TEXT,"
                + GroupMemberData.COLUMN_CONTACT_NAME + " TEXT,"
                + GroupMemberData.COLUMN_STATE + " INTEGER DEFAULT 0,"
                + GroupMemberData.COLUMN_TYPE + " INTEGER DEFAULT 0,"
                + GroupMemberData.COLUMN_PORTRAIT + " TEXT);");
    }

    private void createMappingTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + ThreadMapProvider.TABLE_MAP + " ("
                + ThreadMapData.KEY_ID + " INTEGER primary key autoincrement,"
                + ThreadMapData.KEY_THREAD_ID + " long,"
                + ThreadMapData.KEY_SUBJECT + " TEXT,"
                + ThreadMapData.KEY_NICKNAME + " TEXT,"
                + ThreadMapData.KEY_STATUS + " INTEGER DEFAULT 0,"
                + ThreadMapData.KEY_ISCHAIRMEN + " INTEGER DEFAULT 0,"
                + ThreadMapData.KEY_SUB_ID + " INTEGER DEFAULT 0,"
                + ThreadMapData.KEY_CHAT_ID + " TEXT);");
    }

    private void createRejoinTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + RejoinProvider.TABLE_REJOIN + " ("
                + RejoinData.COLUMN_ID + " INTEGER primary key autoincrement,"
                + RejoinData.COLUMN_CHAT_ID + " TEXT,"
                + RejoinData.COLUMN_REJOIN_ID + " TEXT" +
                ");");
    }

    private void createFavoriteTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE favorite (" +
                   "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                   "favoriteid TEXT," +
                   "address TEXT," +
                   "date INTEGER DEFAULT 0," +
                   "type INTEGER DEFAULT 0," +
                   "path TEXT," +
                   "ct TEXT," +
                   "body TEXT," +
                   "chatid TEXT," +
                   "size INTEGER DEFAULT 0" +
                   ");");
    }
    
    private void updateToVersion2(SQLiteDatabase db) {
        createGroupMemberTable(db);
    }

    private void updateToVersion3(SQLiteDatabase db) {
        createRejoinTable(db);
    }

    private void updateToVersion4(SQLiteDatabase db) {
        String sql = "ALTER TABLE " +
                ThreadMapProvider.TABLE_MAP + " ADD COLUMN " +
                ThreadMapData.KEY_SUB_ID + " INTEGER DEFAULT 0";
        db.execSQL(sql);
    }

}
