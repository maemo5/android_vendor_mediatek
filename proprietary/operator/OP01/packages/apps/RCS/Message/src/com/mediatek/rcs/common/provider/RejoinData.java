package com.mediatek.rcs.common.provider;

import android.net.Uri;

public class RejoinData {

    public static final Uri CONTENT_URI = Uri.parse("content://com.mediatek.message.rejoin");

    public static final String AUTHORITY = "com.mediatek.message.rejoin";
    
    public static final String COLUMN_ID                   = "_id";
    public static final String COLUMN_CHAT_ID              = "chat_id";
    public static final String COLUMN_REJOIN_ID            = "rejoin_id";
}
