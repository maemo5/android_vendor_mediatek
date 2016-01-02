package com.mediatek.rcs.common.provider;

import android.net.Uri;

public class GroupMemberData {

    public static final Uri CONTENT_URI = Uri.parse("content://com.mediatek.message.groupmember");

    public static final String AUTHORITY = "com.mediatek.message.groupmember";
    
    public static final String COLUMN_ID   		           = "_id";
    public static final String COLUMN_CHAT_ID   	       = "chat_id";
    public static final String COLUMN_CONTACT_NUMBER       = "number";
    public static final String COLUMN_CONTACT_NAME         = "name";
    public static final String COLUMN_TYPE                 = "type";
    public static final String COLUMN_PORTRAIT             = "portrait";
    public static final String COLUMN_STATE                = "state";

    public static final class STATE {
        /**
         * 
         */
        public static final int STATE_PENDING         = 0;
        
        /**
         * 
         */
        public static final int STATE_CONNECTED       = 1;
        
        /**
         * 
         */
        public static final int STATE_DISCONNECTED    = 2;
        
    }
}
