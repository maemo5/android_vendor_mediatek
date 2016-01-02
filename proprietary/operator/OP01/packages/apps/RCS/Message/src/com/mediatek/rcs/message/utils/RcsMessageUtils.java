package com.mediatek.rcs.message.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.os.ServiceManager;
import android.provider.Settings;
import android.provider.Telephony;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Mms;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import com.mediatek.rcs.message.R;
import com.mediatek.storage.StorageManagerEx;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;

import com.mediatek.rcs.message.data.RcsProfile;

import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.IpMessage;
import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.common.IpMessageConsts.IpMessageStatus;
import com.mediatek.rcs.common.IpMessageConsts.IpMessageType;
import com.mediatek.rcs.common.IpAttachMessage;
import com.mediatek.rcs.common.IpImageMessage;
import com.mediatek.rcs.common.IpTextMessage;
import com.mediatek.rcs.common.IpVideoMessage;
import com.mediatek.rcs.common.IpVoiceMessage;
import com.mediatek.rcs.common.IpVCardMessage;
import com.mediatek.rcs.common.provider.ThreadMapCache;
import com.mediatek.rcs.common.provider.ThreadMapCache.MapInfo;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.common.utils.Logger;
import com.mediatek.rcs.common.utils.RCSUtils;

import android.graphics.Rect;
import android.media.ExifInterface;

import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.mediatek.telephony.TelephonyManagerEx;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import java.lang.reflect.Method;

import android.os.StatFs;
import android.os.SystemProperties;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;

/**
 * RcsMessageUtils
 *
 */
public class RcsMessageUtils {

    public static final String TAG = "RcsMessageUtils";
    public static final Uri SMS_CONTENT_URI = Uri.parse("content://sms");
    private static final String[] PROJECTION_WITH_THREAD = { Sms._ID, Sms.THREAD_ID,
            Sms.ADDRESS, Sms.BODY };
    private static final String SELECTION_IP_MSG_ID = Sms.IPMSG_ID + "=?";
    public static final int UNCONSTRAINED = -1;
    public static final String IP_MESSAGE_FILE_PATH = File.separator + ".Rcse" + File.separator;
    public static final String CACHE_PATH = File.separator + ".Rcse" + "/Cache/";


    static long getIdInMmsDb(int ipMsgId) {
        Log.d(TAG, "getIdInMmsDb() entry, ipMsgId: " + ipMsgId);
        ContentResolver contentResolver = ContextCacher.getHostContext()
                .getApplicationContext().getContentResolver();

        Cursor cursor = null;
        try {
            final String[] args = { Integer.toString(ipMsgId) };
            cursor = contentResolver.query(SMS_CONTENT_URI, PROJECTION_WITH_THREAD,
                    SELECTION_IP_MSG_ID, args, null);
            if (cursor.moveToFirst()) {
                long mmsDbId = cursor.getLong(cursor.getColumnIndex(Sms._ID));
                long threadId = cursor.getLong(cursor.getColumnIndex(Sms.THREAD_ID));
                String contact = cursor.getString(cursor.getColumnIndex(Sms.ADDRESS));
                Log.d(TAG, "getIdInMmsDb() contact is " + contact + " threadId is " + threadId);
                Log.d(TAG, "getIdInMmsDb() mmsDbId: " + mmsDbId);
                return mmsDbId;
            } else {
                Log.d(TAG, "getIdInMmsDb() empty cursor");
                return -1l;
            }
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
    }

    public static int getVideoCaptureDurationLimit() {
        return 500;
    }

    public static boolean isGroupchat(long threadId) {
        MapInfo info = ThreadMapCache.getInstance().getInfoByThreadId(threadId);
        if (info != null && info.getChatId() != null) {
            return true;
        }
        return false;
    }

    public static String getGroupChatIdByThread(long threadId) {
        MapInfo info = ThreadMapCache.getInstance().getInfoByThreadId(threadId);
        if (info != null) {
            return info.getChatId();
        }
        return null;
    }

    public static Drawable getGroupDrawable(long threadId) {
        return ContextCacher.getPluginContext().getResources()
                .getDrawable(R.drawable.group_example);
    }

    public static boolean getSDCardStatus() {
        boolean ret = false;
        String sdStatus = Environment.getExternalStorageState();
        Log.d(TAG, "getSDCardStatus(): sdStatus = " + sdStatus);
        if (sdStatus.equals(Environment.MEDIA_MOUNTED)) {
            ret = true;
        }
        return ret;
    }

    public static String getSDCardPath(Context c) {
        File sdDir = null;
        String sdStatus = Environment.getExternalStorageState();

        if (TextUtils.isEmpty(sdStatus)) {
            return c.getFilesDir().getAbsolutePath();
        }

        boolean sdCardExist = sdStatus.equals(android.os.Environment.MEDIA_MOUNTED);

        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();
            return sdDir.toString();
        }
        return c.getFilesDir().getAbsolutePath();
    }

    public static String getCachePath(Context c) {
        String path = null;
        String sdCardPath = getSDCardPath(c);
        if (!TextUtils.isEmpty(sdCardPath)) {
            path = sdCardPath + CACHE_PATH;
        }
        return path;
    }

    public static File getStorageFile(String filename) {
        String dir = "";
        String path = StorageManagerEx.getDefaultPath();
        if (path == null) {
            Log.e(TAG, "default path is null");
            return null;
        }
        dir = path + "/" + Environment.DIRECTORY_DOWNLOADS + "/";
        Log.i(TAG, "copyfile,  file full path is " + dir + filename);
        File file = getUniqueDestination(dir + filename);

        // make sure the path is valid and directories created for this file.
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            Log.i(TAG, "[RCS] copyFile: mkdirs for " + parentFile.getPath());
            parentFile.mkdirs();
        }
        return file;
    }

    public static File getUniqueDestination(String fileName) {
        File file;
        final int index = fileName.indexOf(".");
        if (index > 0) {
            final String extension = fileName.substring(index + 1, fileName.length());
            final String base = fileName.substring(0, index);
            file = new File(base + "." + extension);
            for (int i = 2; file.exists(); i++) {
                file = new File(base + "_" + i + "." + extension);
            }
        } else {
            file = new File(fileName);
            for (int i = 2; file.exists(); i++) {
                file = new File(fileName + "_" + i);
            }
        }
        return file;
    }

    public static String getUniqueFileName(String fileName) {
        String mName = fileName;
        File file;
        final int index = fileName.lastIndexOf(".");
        if (index > 0) {
            final String extension = fileName.substring(index + 1, fileName.length());
            final String base = fileName.substring(0, index);
            file = new File(base + "." + extension);
            for (int i = 1; file.exists(); i++) {
                mName = base + "(" + i + ")." + extension;
                file = new File(mName);
            }
        } else {
            file = new File(fileName);
            for (int i = 1; file.exists(); i++) {
                mName = fileName + "(" + i + ")";
                file = new File(mName);
            }
        }
        return mName;
    }

    public static void copy(File src, File dest) {
        InputStream is = null;
        OutputStream os = null;

        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }
        try {
            is = new FileInputStream(src);
            os = new FileOutputStream(dest);
            byte[] b = new byte[256];
            int len = 0;
            try {
                while ((len = is.read(b)) != -1) {
                    os.write(b, 0, len);

                }
                os.flush();
            } catch (IOException e) {
                Log.e(TAG, "", e);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        Log.e(TAG, "", e);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "", e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    Log.e(TAG, "", e);
                }
            }
        }
    }

    public static void copy(String src, String dest) {
        InputStream is = null;
        OutputStream os = null;

        File out = new File(dest);
        if (!out.getParentFile().exists()) {
            out.getParentFile().mkdirs();
        }

        try {
            is = new BufferedInputStream(new FileInputStream(src));
            os = new BufferedOutputStream(new FileOutputStream(dest));

            byte[] b = new byte[256];
            int len = 0;
            try {
                while ((len = is.read(b)) != -1) {
                    os.write(b, 0, len);

                }
                os.flush();
            } catch (IOException e) {
                Log.e(TAG, "", e);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        Log.e(TAG, "", e);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "", e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    Log.e(TAG, "", e);
                }
            }
        }
    }

    public static byte[] resizeImg(String path, float maxLength) {
        // int d = getExifOrientation(path);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        options.inJustDecodeBounds = false;

        int l = Math.max(options.outHeight, options.outWidth);
        int be = (int) (l / maxLength);
        if (be <= 0) {
            be = 1;
        }
        options.inSampleSize = be;

        bitmap = BitmapFactory.decodeFile(path, options);
        if (null == bitmap) {
            return null;
        }
        /*
        if (d != 0) {
            bitmap = rotate(bitmap, d);
        }
        */

        String[] tempStrArry = path.split("\\.");
        String filePostfix = tempStrArry[tempStrArry.length - 1];
        CompressFormat formatType = null;
        if (filePostfix.equalsIgnoreCase("PNG")) {
            formatType = Bitmap.CompressFormat.PNG;
        } else if (filePostfix.equalsIgnoreCase("JPG") || filePostfix.equalsIgnoreCase("JPEG")) {
            formatType = Bitmap.CompressFormat.JPEG;
            // } else if (filePostfix.equalsIgnoreCase("GIF")) {
            // formatType = Bitmap.CompressFormat.PNG;
        } else if (filePostfix.equalsIgnoreCase("BMP")) {
            formatType = Bitmap.CompressFormat.PNG;
        } else {
            Log.d(TAG, "resizeImg(): Can't compress the image,because can't support the format:"
                            + filePostfix);
            return null;
        }

        int quality = 100;
        if (be == 1) {
            if (getFileSize(path) > 50 * 1024) {
                quality = 30;
            }
        } else {
            quality = 30;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(formatType, quality, baos);
        final byte[] tempArry = baos.toByteArray();
        if (baos != null) {
            try {
                baos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            baos = null;
        }

        return tempArry;
    }

    /**
     *
     *
     * @param stream
     * @return
     */
    public static void nmsStream2File(byte[] stream, String filepath) throws Exception {
        FileOutputStream outStream = null;
        try {
            File f = new File(filepath);
            if (!f.getParentFile().exists()) {
                f.getParentFile().mkdirs();
            }
            if (f.exists()) {
                f.delete();
            }
            f.createNewFile();
            outStream = new FileOutputStream(f);
            outStream.write(stream);
            outStream.flush();
        } catch (IOException e) {
            Log.e(TAG, "nmsStream2File():", e);
            throw new RuntimeException(e.getMessage());
        } finally {
            if (outStream != null) {
                try {
                    outStream.close();
                    outStream = null;
                } catch (IOException e) {
                    Log.e(TAG, "nmsStream2File():", e);
                    throw new RuntimeException(e.getMessage());
                }
            }
        }
    }

    public static boolean isPic(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        String path = name.toLowerCase();
        if (path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg")
                || path.endsWith(".bmp") || path.endsWith(".gif")) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isValidAttach(String path, boolean inspectSize) {
        if (!isExistsFile(path) || getFileSize(path) == 0) {
            Log.e(TAG, "isValidAttach: file is not exist, or size is 0");
            return false;
        }
        return true;
    }

    public static boolean isExistsFile(String filepath) {
        try {
            if (TextUtils.isEmpty(filepath)) {
                return false;
            }
            File file = new File(filepath);
            return file.exists();
        } catch (Exception e) {
            Log.e(TAG, "isExistsFile():", e);
            return false;
        }
    }

    public static int getFileSize(String filepath) {
        try {
            if (TextUtils.isEmpty(filepath)) {
                return -1;
            }
            File file = new File(filepath);
            return (int) file.length();
        } catch (Exception e) {
            Log.e(TAG, "getFileSize():", e);
            return -1;
        }
    }

    public static long getCompressLimit() {
        return 1024 * 300; // 300K
    }

    public static long getPhotoSizeLimit() {
        return 10 * 1024 * 1024;
    }

    public static String getPhotoResolutionLimit() {
        return "4000x4000";
    }

    public static String getVideoResolutionLimit() {
        return "1920x1080";
    }

    public static long getAudioDurationLimit() {
        return 180000L;
    }

    public static boolean isVideo(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        String path = name.toLowerCase();
        if (path.endsWith(".mp4") || path.endsWith(".3gp")) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isAudio(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        // /M: add 3gpp audio file{@
        String extArrayString[] = { ".amr", ".ogg", ".mp3", ".aac", ".ape", ".flac", ".wma",
                ".wav", ".mp2", ".mid", ".3gpp" };
        // /@}
        String path = name.toLowerCase();
        for (String ext : extArrayString) {
            if (path.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFileStatusOk(Context context, String path) {
        if (TextUtils.isEmpty(path)) {
            Toast.makeText(context, R.string.ipmsg_no_such_file, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!isExistsFile(path)) {
            Toast.makeText(context, R.string.ipmsg_no_such_file, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public static String formatFileSize(int size) {
        String result = "";
        int oneMb = 1024 * 1024;
        int oneKb = 1024;
        if (size > oneMb) {
            int s = size % oneMb / 100;
            if (s == 0) {
                result = size / oneMb + "MB";
            } else {
                result = size / oneMb + "." + s + "MB";
            }
        } else if (size > oneKb) {
            int s = size % oneKb / 100;
            if (s == 0) {
                result = size / oneKb + "KB";
            } else {
                result = size / oneKb + "." + s + "KB";
            }
        } else if (size > 0) {
            result = size + "B";
        } else {
            result = "invalid size";
        }
        return result;
    }

    public static String formatAudioTime(int duration) {
        String result = "";
        if (duration > 60) {
            if (duration % 60 == 0) {
                result = duration / 60 + "'";
            } else {
                result = duration / 60 + "'" + duration % 60 + "\"";
            }
        } else if (duration > 0) {
            result = duration + "\"";
        } else {
            // TODO IP message replace this string with resource
            result = "no duration";
        }
        return result;
    }

    /**
     * Get bitmap.
     *
     * @param path
     * @param options
     * @return
     */
    public static Bitmap getBitmapByPath(String path, Options options, int width, int height) {
        if (TextUtils.isEmpty(path) || width <= 0 || height <= 0) {
            Log.w(TAG, "parm is error.");
            return null;
        }

        File file = new File(path);
        if (!file.exists()) {
            Log.w(TAG, "file not exist!");
            return null;
        }
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException:" + e.toString());
        }
        if (options != null) {
            Rect r = getScreenRegion(width, height);
            int w = r.width();
            int h = r.height();
            int maxSize = w > h ? w : h;
            int inSimpleSize = computeSampleSize(options, maxSize, w * h);
            options.inSampleSize = inSimpleSize;
            options.inJustDecodeBounds = false;
        }
        Bitmap bm = null;
        try {
            bm = BitmapFactory.decodeStream(in, null, options);
        } catch (java.lang.OutOfMemoryError e) {
            Log.e(TAG, "bitmap decode failed, catch outmemery error");
        }
        try {
            in.close();
        } catch (IOException e) {
            Log.e(TAG, "IOException:" + e.toString());
        }
        return bm;
    }

    private static Rect getScreenRegion(int width, int height) {
        return new Rect(0, 0, width, height);
    }

    /**
     * Compute sample size.
     * @param options BitmapFactory.Options
     * @param minSideLength Min length
     * @param maxNumOfPixels Max number by pixels
     * @return int sample size.
     */
    public static int computeSampleSize(BitmapFactory.Options options, int minSideLength,
            int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels);

        int roundedSize;
        if (initialSize <= 8) {
            roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }

        return roundedSize;
    }

    private static int computeInitialSampleSize(BitmapFactory.Options options,
            int minSideLength, int maxNumOfPixels) {
        double w = options.outWidth;
        double h = options.outHeight;

        int lowerBound = (maxNumOfPixels == UNCONSTRAINED) ? 1 : (int) Math.ceil(Math.sqrt(w * h
                / maxNumOfPixels));
        int upperBound = (minSideLength == UNCONSTRAINED) ? 128 : (int) Math.min(
                Math.floor(w / minSideLength), Math.floor(h / minSideLength));

        if (upperBound < lowerBound) {
            return lowerBound;
        }

        if ((maxNumOfPixels == UNCONSTRAINED) && (minSideLength == UNCONSTRAINED)) {
            return 1;
        } else if (minSideLength == UNCONSTRAINED) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }

    public static int getExifOrientation(String filepath) {
        int degree = 0;
        ExifInterface exif = null;

        try {
            exif = new ExifInterface(filepath);
        } catch (IOException ex) {
            Log.e(TAG, "getExifOrientation():", ex);
        }

        if (exif != null) {
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
            if (orientation != -1) {
                // We only recognize a subset of orientation tag values.
                switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
                default:
                    break;
                }
            }
        }

        return degree;
    }

    public static Options getOptions(String path) {
        Options options = new Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        return options;
    }

    public static Bitmap rotate(Bitmap b, int degrees) {
        if (degrees != 0 && b != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees, (float) b.getWidth() / 2, (float) b.getHeight() / 2);
            try {
                Bitmap b2 = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
                if (b != b2) {
                    b.recycle();
                    b = b2;
                }
            } catch (OutOfMemoryError ex) {
                // We have no memory to rotate. Return the original bitmap.
                Log.w(TAG, "OutOfMemoryError.");
            }
        }
        return b;
    }

    /**
     * This method is from message app.
     * 
     * @param context It used to get string from res.
     * @param when time.
     * @return string.
     */
    public static String formatTimeStampStringExtend(Context context, long when) {
        Time then = new Time();
        then.set(when);
        Time now = new Time();
        now.setToNow();

        // Basic settings for formatDateTime() we want for all cases.
        int formatFags = DateUtils.FORMAT_NO_NOON_MIDNIGHT
                | DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_CAP_AMPM;

        // If the message is from a different year, show the date and year.
        if (then.year != now.year) {
            formatFags |= DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE;
        } else if (then.yearDay != now.yearDay) {
            // If it is from a different day than today, show only the date.
            if ((now.yearDay - then.yearDay) == 1) {
                return context.getString(R.string.str_ipmsg_yesterday);
            } else {
                formatFags |= DateUtils.FORMAT_SHOW_DATE;
            }
        } else if ((now.toMillis(false) - then.toMillis(false)) < 60000) {
            return context.getString(R.string.time_now);
        } else {
            // Otherwise, if the message is from today, show the time.
            formatFags |= DateUtils.FORMAT_SHOW_TIME;
        }
        return DateUtils.formatDateTime(context, when, formatFags);
    }

    /**
     * this method is from message app.
     * @param context It used to get string from res.
     * @param when time.
     * @param fullFormat flag.
     * @return time string.
     */
    public static String formatTimeStampString(Context context, long when, boolean fullFormat) {
        Time then = new Time();
        then.set(when);
        Time now = new Time();
        now.setToNow();

        // Basic settings for formatDateTime() we want for all cases.
        int formatFags = DateUtils.FORMAT_NO_NOON_MIDNIGHT |
        // / M: Fix ALPS00419488 to show 12:00, so mark
        // DateUtils.FORMAT_ABBREV_ALL
        // DateUtils.FORMAT_ABBREV_ALL |
                DateUtils.FORMAT_CAP_AMPM;

        // If the message is from a different year, show the date and year.
        if (then.year != now.year) {
            formatFags |= DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE;
        } else if (then.yearDay != now.yearDay) {
            // If it is from a different day than today, show only the date.
            formatFags |= DateUtils.FORMAT_SHOW_DATE;
        } else {
            // Otherwise, if the message is from today, show the time.
            formatFags |= DateUtils.FORMAT_SHOW_TIME;
        }

        // If the caller has asked for full details, make sure to show the date
        // and time no matter what we've determined above (but still make
        // showing
        // the year only happen if it is a different year from today).
        if (fullFormat) {
            formatFags |= (DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);
        }

        return DateUtils.formatDateTime(context, when, formatFags);
    }

    /**
     * na.
     * @param bytes file size.
     * @param context caller of this method.
     * @return file size string.
     */
    public static String getDisplaySize(long bytes, Context context) {
        String displaySize = context.getString(R.string.unknown);
        long iKb = bytes / 1024;
        if (iKb == 0 && bytes >= 0) {
            // display "less than 1KB"
            displaySize = context.getString(R.string.less_1K);
        } else if (iKb >= 1024) {
            // diplay MB
            double iMb = ((double) iKb) / 1024;
            iMb = round(iMb, 2, BigDecimal.ROUND_UP);
            StringBuilder builder = new StringBuilder(new Double(iMb).toString());
            builder.append("MB");
            displaySize = builder.toString();
        } else {
            // display KB
            StringBuilder builder = new StringBuilder(new Long(iKb).toString());
            builder.append("KB");
            displaySize = builder.toString();
        }
        return displaySize;
    }

    private static double round(double value, int scale, int roundingMode) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(scale, roundingMode);
        double d = bd.doubleValue();
        bd = null;
        return d;
    }

    /**
     * get the 3G/4G Capability subId.
     * @return the 3G/4G Capability subId
     */
    public static long get34GCapabilitySubId() {
        ITelephony iTelephony = ITelephony.Stub
                .asInterface(ServiceManager.getService("phone"));
        TelephonyManager telephonyManager = TelephonyManager.getDefault();
        long subId = -1;
        /*
         * if (iTelephony != null) { for (int i = 0; i <
         * telephonyManager.getPhoneCount(); i++) { try { Log.d(TAG,
         * "get34GCapabilitySubId, iTelephony.getPhoneRat(" + i + "): " +
         * iTelephony.getPhoneRat(i)); if (((iTelephony.getPhoneRat(i) &
         * (PhoneRatFamily.PHONE_RAT_FAMILY_3G |
         * PhoneRatFamily.PHONE_RAT_FAMILY_4G)) > 0)) { subId =
         * PhoneFactory.getPhone(i).getSubId(); Log.d(TAG,
         * "get34GCapabilitySubId success, subId: " + subId); return subId; } }
         * catch (RemoteException e) { Log.d(TAG,
         * "get34GCapabilitySubId FAIL to getPhoneRat i" + i + " error:" +
         * e.getMessage()); } } }
         */
        return subId;
    }

    /**
     * Get current send sub id. Used for forward
     * @param context Context
     * @return sub id.
     */
    public static int getSendSubid(Context context) {
        List<SubscriptionInfo> mSubInfoList = SubscriptionManager.from(context)
                .getActiveSubscriptionInfoList();
        int mSubCount = mSubInfoList.isEmpty() ? 0 : mSubInfoList.size();
        Log.v(TAG, "getSimInfoList(): mSubCount = " + mSubCount);
        if (mSubCount > 1) {
            int subIdinSetting = SubscriptionManager.getDefaultSmsSubId();
            if (subIdinSetting == Settings.System.SMS_SIM_SETTING_AUTO) {
                // getZhuKa
                int mainCardSubId = SubscriptionManager.getDefaultDataSubId();
                if (!SubscriptionManager.isValidSubscriptionId(mainCardSubId)) { // data unset
                    return mSubInfoList.get(0).getSubscriptionId(); // SIM1
                }
                return mainCardSubId;
            } else { // SIM1/SIM2
                return subIdinSetting;
            }
        } else if (mSubCount == 1) {
            return mSubInfoList.get(0).getSubscriptionId();
        } else {
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }
    }

    // add for forward
    public static int getForwardSubid(Context context) {
        List<SubscriptionInfo> mSubInfoList = SubscriptionManager.from(context)
                .getActiveSubscriptionInfoList();
        int mSubCount = mSubInfoList.isEmpty() ? 0 : mSubInfoList.size();
        Log.v(TAG, "getSimInfoList(): mSubCount = " + mSubCount);
        if (mSubCount > 1) {
            int subIdinSetting = SubscriptionManager.getDefaultSmsSubId();
            if (subIdinSetting == Settings.System.SMS_SIM_SETTING_AUTO) {
                // getZhuKa
                int mainCardSubId = SubscriptionManager.getDefaultDataSubId();
                if (!SubscriptionManager.isValidSubscriptionId(mainCardSubId)) { // data unset
                    return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
                }
                return mainCardSubId;
            } else { // SIM1/SIM2
                return subIdinSetting;
            }
        } else if (mSubCount == 1) {
            return mSubInfoList.get(0).getSubscriptionId();
        } else {
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }
    }

    public static boolean isSupportRcsForward(Context context) {
        if (!getConfigStatus()) {
            Log.d(TAG, "isSupprotRcsForward() result = false");
            return false;
        }
        boolean result;
        int subid = RcsMessageUtils.getForwardSubid(context);
        Log.d(TAG, "subid = " + subid);
        int mainCardSubId = SubscriptionManager.getDefaultDataSubId();
        if (SubscriptionManager.isValidSubscriptionId(subid)
                && SubscriptionManager.isValidSubscriptionId(mainCardSubId)
                && subid == mainCardSubId) {
            result = true;
        } else {
            result = false;
        }
        Log.d(TAG, "isSupprotRcsForward = " + result);
        return result;
    }

    public static Intent createForwardIntentFromMms(Context context, Uri uri) {
        if (isSupportRcsForward(context)) {
            Intent sendIntent = new Intent();
            sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
            sendIntent.setAction("android.intent.action.ACTION_RCS_MESSAGING_SEND");
            sendIntent.setType("mms/pdu");
            return sendIntent;
        }
        return null;
    }

    public static Intent createForwordIntentFromSms(Context context, String mBody) {
        if (isSupportRcsForward(context)) {
            Intent sendIntent = new Intent();
            sendIntent.setAction("android.intent.action.ACTION_RCS_MESSAGING_SEND");
            sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_STREAM, mBody);
            return sendIntent;
        }
        return null;
    }

    public static Intent createForwordIntentFromIpmessage(Context context, IpMessage ipMessage) {
        if (isSupportRcsForward(context)) {
            Intent sendIntent = new Intent();
            sendIntent.setAction("android.intent.action.ACTION_RCS_MESSAGING_SEND");
            sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (ipMessage.getType() == IpMessageType.TEXT) {
                IpTextMessage textMessage = (IpTextMessage) ipMessage;
                sendIntent.setType("text/plain");
                sendIntent.putExtra(Intent.EXTRA_STREAM, textMessage.getBody());
                return sendIntent;
            } else {
                if (RCSServiceManager.getInstance().serviceIsReady()) {
                    IpAttachMessage attachMessage = (IpAttachMessage) ipMessage;
                    if (ipMessage.getType() == IpMessageType.PICTURE) {
                        sendIntent.putExtra(Intent.EXTRA_STREAM, attachMessage.getPath());
                        sendIntent.setType("image/jpeg");
                    } else if (ipMessage.getType() == IpMessageType.VIDEO) {
                        sendIntent.putExtra(Intent.EXTRA_STREAM, attachMessage.getPath());
                        sendIntent.setType("video/mp4");
                    } else if (ipMessage.getType() == IpMessageType.VCARD) {
                        sendIntent.putExtra(Intent.EXTRA_STREAM, attachMessage.getPath());
                        sendIntent.setType("text/x-vcard");
                    } else if (ipMessage.getType() == IpMessageType.GEOLOC) {
                        sendIntent.putExtra(Intent.EXTRA_STREAM, attachMessage.getPath());
                        sendIntent.setType("geo/*");
                    } else if (ipMessage.getType() == IpMessageType.VOICE) {
                        sendIntent.putExtra(Intent.EXTRA_STREAM, attachMessage.getPath());
                        sendIntent.setType("audio/*");
                    }
                    return sendIntent;
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    public static int getMainCardSendCapability() {
        if (getConfigStatus()) {
            if (RCSServiceManager.getInstance().serviceIsReady()) {
                Log.v(TAG, "getMainCardSendCapability(): mSendSubid SubCapatibily=7");
                return 7; // send all
            } else {
                Log.v(TAG, "getMainCardSendCapability(): mSendSubid SubCapatibily=1");
                return 1; // send Sms
            }
        } else {
            Log.v(TAG, "getMainCardSendCapability(): mSendSubid SubCapatibily=3 ");
            return 3; // send Mms\Sms
        }
    }

    public static boolean getConfigStatus() {
        boolean isActive = RCSServiceManager.getInstance().isServiceEnabled();
        Log.v(TAG, "getConfigStatus: isActive" + isActive);
        return isActive;
    }

    public static int getUserSelectedId(Context context) {
        int subIdinSetting = (int) SubscriptionManager.getDefaultSmsSubId();
        return subIdinSetting;
    }

    public static int getRcsSubId(Context context) {
        int rcsSubId = SubscriptionManager.getDefaultDataSubId();
        if (!SubscriptionManager.isValidSubscriptionId(rcsSubId)) {////data unset
            rcsSubId = -1;
        }
        return rcsSubId;
    }

    /**
     * Get Photo Destinator path. Used for choose a picture.
     * @param filePath String
     * @param context Context
     * @return String file path
     */
    public static String getPhotoDstFilePath(String filePath, Context context) {
        // for choose a picture
        int index = filePath.lastIndexOf("/");
        String fileName = filePath.substring(index + 1);
        return RcsMessageConfig.getPicTempPath(context) + File.separator + fileName;
    }

    public static String getPhotoDstFilePath(Context context) {
        // only for take photo
        String fileName = System.currentTimeMillis() + ".jpg";
        return RcsMessageConfig.getPicTempPath(context) + File.separator + fileName;
    }

    public static String getVideoDstFilePath(String filePath, Context context) {
        // for choose a video
        int index = filePath.lastIndexOf("/");
        String fileName = filePath.substring(index + 1);
        return RcsMessageConfig.getVideoTempPath(context) + File.separator + fileName;
    }

    public static String getVideoDstFilePath(Context context) {
        // only for record a video
        String fileName = System.currentTimeMillis() + ".3gp";
        return RcsMessageConfig.getVideoTempPath(context) + File.separator + fileName;
    }

    public static String getAudioDstPath(String filePath, Context context) {
        int index = filePath.lastIndexOf("/");
        String fileName = filePath.substring(index + 1);
        return RcsMessageConfig.getAudioTempPath(context) + File.separator + fileName;
    }

    public static String getGeolocPath(Context context) {
        return RcsMessageConfig.getGeolocTempPath(context) + File.separator;
    }

    public static void copyFileToDst(String src, String dst) {
        if (src == null || dst == null) {
            return;
        }
        RcsMessageUtils.copy(src, dst);
    }

    public static boolean isGif(String filePath) {
        if (filePath != null && filePath.contains(".gif")) {
            return true;
        }
        return false;
    }
}
