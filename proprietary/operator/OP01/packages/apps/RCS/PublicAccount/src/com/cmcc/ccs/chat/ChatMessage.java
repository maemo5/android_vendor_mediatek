package com.cmcc.ccs.chat;

import java.sql.Date;
import java.util.Set;

public class ChatMessage {
    public static final int TIMEOUT = -1;
    public static final int UNKNOW = -2;
    public static final int INTERNAL = -3;
    public static final int OUTOFSIZE = -4;
    public static final int INCOMING = -5;
    public static final int OUTCOMING = -6;

    public static final int UNREAD = 1;
    public static final int READ = 2;
    public static final int SENDING = 3;
    public static final int SENT = 4;
    public static final int FAILED = 5;
    public static final int TO_SEND = 6;
    
    public static final int OTO = 1;
    public static final int OTM = 2;
    public static final int MTM = 3;
    public static final int PUBLIC = 4;
    
    public static final String MESSAGE_ID = "CHATMESSAGE_MESSAGE_ID";
    public static final String CONTACT_NUMBER = "CHATMESSAGE_CONTACT_NUMBER";
    public static final String BODY = "CHATMESSAGE_BODY";
    public static final String TIMESTAMP = "CHATMESSAGE_TIMESTAMP";
    public static final String MIME_TYPE = "CHATMESSAGE_MIME_TYPE";
    public static final String MESSAGE_STATUS = "CHATMESSAGE_MESSAGE_STATUS";
    public static final String DIRECTION = "CHATMESSAGE_DIRECTION";
    public static final String FAVORITE = "CHATMESSAGE_FAVORITE";
    public static final String TYPE = "CHATMESSAGE_TYPE";
    public static final String FLAG = "CHATMESSAGE_FLAG";


    public String getContact() {
        return null;
    }

    public Set<String> getContacts() {
        return null;
    }

    public String getId() {
        return null;
    }

    public Date getReceiptDate() {
        return null;
    }

    public String getMessage() {
        return null;
    }
}
