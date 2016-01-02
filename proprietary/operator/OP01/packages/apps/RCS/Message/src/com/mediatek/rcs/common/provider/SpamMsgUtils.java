package com.mediatek.rcs.common.provider;

import static com.google.android.mms.pdu.PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import android.provider.Telephony.Sms;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Mms.Inbox;

import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPersister;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class SpamMsgUtils {
   
    private static final String TAG = "RCS/SpamMsgUtils";
    private static SpamMsgUtils sInstance;
    private ContentResolver mResolver;
    private Context mContext;
    
    public static final String[] PROJECTION = {
        SpamMsgData.COLUMN_ID,
        SpamMsgData.COLUMN_BODY,
        SpamMsgData.COLUMN_ADDRESS,
        SpamMsgData.COLUMN_DATE,
        SpamMsgData.COLUMN_TYPE,
        SpamMsgData.COLUMN_SUB_ID,
        SpamMsgData.COLUMN_IPMSG_ID
    };
    
    private SpamMsgUtils(Context context) {
        mContext = context;
        mResolver = context.getContentResolver();
    }
    
    public synchronized static SpamMsgUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SpamMsgUtils(context);
        }
        return sInstance;
    }

    public Uri insertSpamSms(String body, String contact, long subId) {
        Log.d(TAG, "insertSpamSms, body=" + body + "; contact=" + contact);
        return insertSpamMessage(body, contact, subId, SpamMsgData.Type.TYPE_SMS, 0);
    }
    
    public Uri insertSpamMmsPush(byte[] pduData, String contact, long subId) {
        String body = byteToIsoString(pduData);
        Log.d(TAG, "insertSpamMmsPush, body=" + body + "; contact=" + contact);
        return insertSpamMessage(body, contact, subId, SpamMsgData.Type.TYPE_MMS_PUSH, 0);
    }
    
    public Uri insertSpamTextIpMsg(String body, String contact, long subId, long ipMsgId) {
        Log.d(TAG, "insertSpamTextIpMsg, body=" + body + "; contact=" + contact + "; ipMsgId=" + ipMsgId);
        return insertSpamMessage(body, contact, subId, SpamMsgData.Type.TYPE_IP_TEXT_MSG, ipMsgId);
    }

    public Uri insertSpamFtIpMsg(String body, String contact, long subId, long ipMsgId) {
        Log.d(TAG, "insertSpamFTIpMsg, body=" + body + "; contact=" + contact + "; ipMsgId=" + ipMsgId);
        return insertSpamMessage(body, contact, subId, SpamMsgData.Type.TYPE_IP_FT_MSG, ipMsgId);
    }
    
    private Uri insertSpamMessage(String body, String address, long subId, int type, long ipMsgId) {
        Log.d(TAG, "insertSpamMessage, body=" + body + "; address=" + address);
        ContentValues cv = new ContentValues();
        cv.put(SpamMsgData.COLUMN_ADDRESS, address);
        cv.put(SpamMsgData.COLUMN_BODY, body);
        cv.put(SpamMsgData.COLUMN_SUB_ID, subId);
        cv.put(SpamMsgData.COLUMN_TYPE, type);
        cv.put(SpamMsgData.COLUMN_DATE, System.currentTimeMillis());
        cv.put(SpamMsgData.COLUMN_IPMSG_ID, ipMsgId);
        return mResolver.insert(SpamMsgData.CONTENT_URI, cv);
    }
    
    private String byteToIsoString(byte[] bytes) {
        try {
            return new String(bytes, "iso-8859-1");
        } catch (UnsupportedEncodingException e) {
            // Impossible to reach here!
            Log.e(TAG, "ISO_8859_1 must be supported!", e);
            return "";
        } catch (NullPointerException e) {
            return "";
        }
    }
    
    private byte[] isoStringToByte(String data) {
        try {
            return data.getBytes("iso-8859-1");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            return null;
        } catch (NullPointerException e) {
            return null;
        }
    }
    
    public Uri restoreMessage(int spamId) {
        Log.d(TAG, "restoreMessage spamId = " + spamId);
        Uri uri = null;
        String body = null;
        long date = 0;
        int type = 0;
        int subId = -1;
        String address = null;
        long ipmsgId = 0;
        Cursor cursor = mResolver.query(SpamMsgData.CONTENT_URI, PROJECTION, "_id=" + spamId, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                body = cursor.getString(cursor.getColumnIndexOrThrow(SpamMsgData.COLUMN_BODY));
                type = cursor.getInt(cursor.getColumnIndexOrThrow(SpamMsgData.COLUMN_TYPE));
                date = cursor.getLong(cursor.getColumnIndexOrThrow(SpamMsgData.COLUMN_DATE));
                subId = cursor.getInt(cursor.getColumnIndexOrThrow(SpamMsgData.COLUMN_SUB_ID));
                address = cursor.getString(cursor.getColumnIndexOrThrow(SpamMsgData.COLUMN_ADDRESS));
                ipmsgId = cursor.getLong(cursor.getColumnIndexOrThrow(SpamMsgData.COLUMN_IPMSG_ID));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        switch (type) {
            case SpamMsgData.Type.TYPE_SMS:
                uri = restoreSms(body, address, subId, date);
                break;
            case SpamMsgData.Type.TYPE_MMS_PUSH:
                uri = restoreMmsPush(body, address, subId, date);
                break;
            case SpamMsgData.Type.TYPE_IP_TEXT_MSG:
                uri = restoreRCSTextMessage(body, address, subId, date, ipmsgId);
                break;
            case SpamMsgData.Type.TYPE_IP_FT_MSG:
                uri = restoreRCSFTMessage(body, address, subId, date, ipmsgId);
                break;
            default:
                Log.d(TAG, "unnkow spam msg type");
                break;
        }
        mResolver.delete(SpamMsgData.CONTENT_URI, "_id=" + spamId, null);
        Log.d(TAG, "restoreMessage uri = " + uri);
        return uri;
    }

    private Uri restoreSms(String body, String address, int subId, long date) {
        Log.d(TAG, "restoreSms, body=" + body + "; address=" + address);
        Uri uri = null;
        uri = Sms.addMessageToUri(subId, mResolver, Sms.Inbox.CONTENT_URI, address, body, null, date, true, false);
        return uri;
    }

    private Uri restoreMmsPush(String pushData, String address, int subId, long date) {
        Log.d(TAG, "restoreMmsPush, pushData=" + pushData + "; address=" + address);
        byte[] pduData = isoStringToByte(pushData);
        PduParser parser = new PduParser(pduData,false);
        GenericPdu pdu = parser.parse(true);
        if (null == pdu) {
            Log.e(TAG, "Invalid PUSH data");
            return null;
        }
        PduPersister p = PduPersister.getPduPersister(mContext);
        ContentResolver cr = mContext.getContentResolver();
        int type = pdu.getMessageType();
        Uri uri = null;
        if (type == MESSAGE_TYPE_NOTIFICATION_IND) {
            HashMap<String, String> attach = new HashMap<String, String>();
            attach.put("read", "1");
            attach.put("locked", "0");
            attach.put("sub_id", String.valueOf(subId));
            try {
                uri = p.persistEx(pdu, Mms.Inbox.CONTENT_URI, true, attach);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return uri;
    }
    
    private Uri restoreRCSTextMessage(String body, String address, int subId, long date, long ipmsgId) {
        Log.d(TAG, "restoreRCSTextMessage, body=" + body + "; address=" + address);
        ContentValues cv = new ContentValues();
        cv.put(Sms.BODY, body);
        cv.put(Sms.ADDRESS, address);
        cv.put(Sms.SUBSCRIPTION_ID, subId);
        cv.put(Sms.DATE, date);
        cv.put(Sms.IPMSG_ID, ipmsgId);
        
        return mResolver.insert(Sms.Inbox.CONTENT_URI, cv);
    }

    private Uri restoreRCSFTMessage(String body, String address, int subId, long date, long ipmsgId) {
        Log.d(TAG, "restoreRCSFTMessage, body=" + body + "; address=" + address);
        ContentValues cv = new ContentValues();
        cv.put(Sms.BODY, body);
        cv.put(Sms.ADDRESS, address);
        cv.put(Sms.SUBSCRIPTION_ID, subId);
        cv.put(Sms.DATE, date);
        cv.put(Sms.IPMSG_ID, -ipmsgId);
        return mResolver.insert(Sms.Inbox.CONTENT_URI, cv);
    }
}
