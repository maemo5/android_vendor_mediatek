package com.mediatek.rcs.pam;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeField;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Utils {
    private static final DateTimeZone GMT8 = DateTimeZone.forID("Asia/Shanghai");
    private static final DateTimeFormatter FORMATTER = ISODateTimeFormat.dateTimeNoMillis();
    private static final DateTimeFormatter HUMAN_READABLE_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd hh:mm a");
    
    private static final String TAG = Constants.TAG_PREFIX + "Utils";

    public static long currentTimestamp() {
        return System.currentTimeMillis() / 1000 * 1000;
    }

    // Convert GMT+8 time string to UTC timestamp
    public static long convertStringToTimestamp(String s) {
        if (TextUtils.isEmpty(s)) {
            return Constants.INVALID;
        }
        try {
            DateTime dateTime = FORMATTER.parseDateTime(s);
            return dateTime.getMillis();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return Constants.INVALID;
        }
    }

    // Convert UTC timestamp to GMT+8 time string
    public static String covertTimestampToString(long timestamp) {
        DateTime dt = new DateTime(timestamp, DateTimeZone.UTC);
        dt = dt.toDateTime(GMT8);
        return FORMATTER.print(dt);
    }

    // TODO use WeChat-like date format
//    public static String covertTimestampToHumanReadableString(long timestamp) {
//        DateTime dt = new DateTime(timestamp, DateTimeZone.UTC);
//        dt = dt.toDateTime(GMT8);
//        return HUMAN_READABLE_FORMATTER.print(dt);
//    }
    
    public static String covertTimestampToHumanReadableString(Context context, long when) {
        Time then = new Time();
        then.set(when);
        Time now = new Time();
        now.setToNow();
        
        int formatFlags = DateUtils.FORMAT_NO_NOON_MIDNIGHT | DateUtils.FORMAT_CAP_AMPM;
        
        if (then.year != now.year) {
            formatFlags |= DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE;
        } else if (then.yearDay != now.yearDay) {
            formatFlags |= DateUtils.FORMAT_SHOW_DATE;
        }
        
        formatFlags |= DateUtils.FORMAT_SHOW_TIME;
        
        return DateUtils.formatDateTime(context, when, formatFlags);
    }


    public static void throwIf(int resultCode, boolean predicate) throws PAMException {
        if (predicate) {
            throw new PAMException(resultCode);
        }
    }

    public static void copyFile(String src, String dest) throws IOException {
        int byteSum = 0;
        int byteRead = 0;
        byte[] buffer = new byte[4096];
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(src);
            fos = new FileOutputStream(dest);
            while ((byteRead = fis.read(buffer)) != -1) {
                byteSum += byteRead;
                System.out.println(byteSum);
                fos.write(buffer, 0, byteRead);
            }
        } finally {
            if (fis != null) {
                fis.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
    }

    public static void storeToFile(String content, String dest) throws IOException {
        int byteSum = 0;
        int byteRead = 0;
        byte[] buffer = new byte[4096];
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            is = IOUtils.toInputStream(content, "UTF8");
            fos = new FileOutputStream(dest);
            while ((byteRead = is.read(buffer)) != -1) {
                byteSum += byteRead;
                System.out.println(byteSum);
                fos.write(buffer, 0, byteRead);
            }
        } finally {
            if (is != null) {
                is.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
    }
    
    public static String extractUuidFromSipUri(String sipUri) {
        if (sipUri == null) {
            return null;
        } else if (sipUri.startsWith(Constants.QUOTED_SIP_PREFIX)) {
            int index = sipUri.indexOf(">");
            if (index == -1) {
                Log.e(TAG, "Invalid SIP URI format: " + sipUri);
                return null;
            }
            return sipUri.substring(Constants.QUOTED_SIP_PREFIX.length(), index);
        } else if (sipUri.startsWith(Constants.SIP_PREFIX)) {
            return sipUri.substring(Constants.SIP_PREFIX.length());
        } else {
            Log.w(TAG, "Invalid SIP URI format: " + sipUri + ", use it directly.");
            return sipUri;
        }
    }
    
    public static String extractNumberFromUuid(String uuid) {
        if (uuid == null) {
            return null;
        }
        int index = uuid.indexOf("@");
        if (index != -1) {
            return uuid.substring(0, index);
        } else {
            Log.e(TAG, "Invalid UUID format: " + uuid);
            return null;
        }
    }

    public static void deleteFile(String mediaPath) {
        File f = new File(mediaPath);
        if (f.exists()) {
            f.delete();
        }
    }
}
