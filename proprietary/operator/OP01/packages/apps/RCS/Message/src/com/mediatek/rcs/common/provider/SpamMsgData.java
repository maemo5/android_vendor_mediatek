package com.mediatek.rcs.common.provider;

import android.net.Uri;

public class SpamMsgData {

    public static final Uri CONTENT_URI = Uri.parse("content://com.mediatek.message.spam");

    public static final String AUTHORITY = "com.mediatek.message.spam";
    
    public static final String COLUMN_ID            = "_id";
    public static final String COLUMN_BODY          = "body";
    public static final String COLUMN_DATE          = "date";
    public static final String COLUMN_ADDRESS       = "address";
    public static final String COLUMN_TYPE          = "type";
    public static final String COLUMN_SUB_ID        = "sub_id";
    public static final String COLUMN_IPMSG_ID      = "ipmsg_id";

    public static class Type {
        /**
         * 
         */
        public static final int TYPE_SMS            = 0;
        
        /**
         * 
         */
        public static final int TYPE_MMS_PUSH       = 1;
        
        /**
         * 
         */
        public static final int TYPE_IP_TEXT_MSG    = 2;
        
        /**
         * 
         */
        public static final int TYPE_IP_FT_MSG      = 3;
    }
}
