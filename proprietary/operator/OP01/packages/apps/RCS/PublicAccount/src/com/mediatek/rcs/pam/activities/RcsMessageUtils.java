package com.mediatek.rcs.pam.activities;

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
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Telephony.Sms;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import com.mediatek.rcs.pam.MediaFolder;
import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.util.ContextCacher;
import com.mediatek.rcs.pam.util.EmojiImpl;

public class RcsMessageUtils {
    
    public static final String TAG = "PA/RcsMessageUtils";
    
    public static final int UNCONSTRAINED = -1;
    public static final String PA_MESSAGE_CACHE_PATH = 
            File.separator + MediaFolder.ROOT_DIR + File.separator + ".cache";
    public static final String PA_FILE_PREFIX_IMG = "IMG";
    public static final String PA_FILE_PREFIX_VDO = "VDO";
    public static final String PA_FILE_SUFFIX_JPG = ".jpg";
    public static final String PA_FILE_SUFFIX_3GP = ".3gp";

    public static CharSequence formatTextMessage(CharSequence inputChars, boolean showImg, CharSequence inputBuf) {
        Log.d(TAG, "formatTextMessage(): inputChars = " + inputChars);
        if (inputChars == null) {
            return "";
        }
        
        EmojiImpl emoji = EmojiImpl.getInstance(ContextCacher.getPluginContext());
        CharSequence outChars = emoji.getEmojiExpression(inputChars, showImg);
        if (inputBuf == null) {
            return outChars;
        } else {
            String bufStr = inputBuf.toString();
            String inputStr = inputChars.toString();
            int start = bufStr.indexOf(inputStr);
            if (start == -1) {
                return inputBuf;
            }
            CharSequence bufChars = emoji.getEmojiExpression(inputBuf,  showImg);
            return bufChars;
        }        
    }
    
    public static int getVideoCaptureDurationLimit() {
        return 500;
    }
    
    public static Drawable getMeDrawADrawable() {
        return null;
    }

    public static String getTempFilePath(String prefix, String suffix) {
        
        MediaFolder.getRootDir();
        
        String fileName = prefix + System.currentTimeMillis() + suffix;
        String pathName = Environment.getExternalStorageDirectory() + PA_MESSAGE_CACHE_PATH;
        File path = new File(pathName);
        if (!path.exists()) {
            path.mkdirs();
        }
        String fullPath = pathName + File.separator + fileName;
        Log.d(TAG, "getTempFilePath=" + fullPath);
        return fullPath;
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
            path = sdCardPath + PA_MESSAGE_CACHE_PATH + File.separator;
        }
        return path;
    }
    

    public static File getStorageFile(String filename) {
        String dir = "";
        String path = "/storage/sdcard0";//StorageManagerEx.getDefaultPath();
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
        File file ;
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


    public static Bitmap resizeImage(Bitmap bitmap, int w, int h, boolean needRecycle) {
        if (null == bitmap) {
            return null;
        }

        Bitmap bitmapOrg = bitmap;
        int width = bitmapOrg.getWidth();
        int height = bitmapOrg.getHeight();
        int newWidth = w;
        int newHeight = h;

        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);

        Bitmap resizedBitmap = Bitmap.createBitmap(bitmapOrg, 0, 0, width, height, matrix, true);
        if (needRecycle && !bitmapOrg.isRecycled() && bitmapOrg != resizedBitmap) {
            bitmapOrg.recycle();
        }
        return resizedBitmap;
    }

    public static byte[] resizeImg(String path, float maxLength) {
        //int d = getExifOrientation(path);
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
        Log.d(TAG, "resizeImg() after decodeFile. w=" + bitmap.getWidth() +
                ". h=" + bitmap.getHeight());
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
        //} else if (filePostfix.equalsIgnoreCase("GIF")) {
        //    formatType = Bitmap.CompressFormat.PNG;
        } else if (filePostfix.equalsIgnoreCase("BMP")) {
            formatType = Bitmap.CompressFormat.PNG;
        } else {
            Log.d(TAG, "resizeImg(): Can't compress the image,because can't support the format:" + filePostfix);
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

/*
    public static int getExifOrientation(String filepath) {
        int degree = 0;
        ExifInterface exif = null;

        try {
            exif = new ExifInterface(filepath);
        } catch (IOException ex) {
            MmsLog.e(TAG, "getExifOrientation():", ex);
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

    */

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
            if (filepath == null) {
                return false;
            }
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
    

     /**
     * Return the file transfer IpMessage
     * 
     * @param remote remote user
     * @param FileStructForBinder file strut
     * @return The file transfer IpMessage
     */
     /*
    public static IpMessage analysisFileType(String remote, FileStruct fileTransfer) {

        String fileName = fileTransfer.mName;
        if (fileName != null) {
            String mimeType = MediaFile.getMimeTypeForFile(fileName);
            if (mimeType == null) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        getFileExtension(fileName));
            }
            if (mimeType != null) {
                if (mimeType.contains(FILE_TYPE_IMAGE)) {
                    return new IpImageMessage(fileTransfer, remote);
                } else if (mimeType.contains(FILE_TYPE_AUDIO)
                        || mimeType.contains("application/ogg")) {
                    return new IpVoiceMessage(fileTransfer, remote);
                } else if (mimeType.contains(FILE_TYPE_VIDEO)) {
                    return new IpVideoMessage(fileTransfer, remote);
                } else if (fileName.toLowerCase().endsWith(".vcf")) {
                    return new IpVCardMessage(fileTransfer, remote);
                } else {
                    // Todo
                    Log.d(TAG, "analysisFileType() other type add here!");
                }
            }
        } else {
            Log.w(TAG, "analysisFileType(), file name is null!");
        }
        return null;
    }
    */
        

     public static String getFileExtension(String fileName) {
        Log.d(TAG, "getFileExtension() entry, the fileName is " + fileName);
        String extension = null;
        if (TextUtils.isEmpty(fileName)) {
            Log.d(TAG, "getFileExtension() entry, the fileName is null");
            return null;
        }
        int lastDot = fileName.lastIndexOf(".");
        extension = fileName.substring(lastDot + 1).toLowerCase();
        Log.d(TAG, "getFileExtension() entry, the extension is " + extension);
        return extension;
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
        ///M: add 3gpp audio file{@
        String extArrayString[] = {".amr", ".ogg", ".mp3", ".aac", ".ape", ".flac", ".wma", ".wav", ".mp2", ".mid",".3gpp"};
        ///@}
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
        //if (getFileSize(path) > (2 * 1024 * 1024)) {
        //    Toast.makeText(context, R.string.ipmsg_over_file_limit, Toast.LENGTH_SHORT).show();
        //    return false;
        //}
        return true;
    }

    public static String formatFileSize(int size, int scale) {
        Log.d(TAG, "formatFileSize:" + size);
        String result = "";
        float oneMb = 1024f * 1024f;
        float oneKb = 1024f;
        if (size > oneMb) {
            float s = size / oneMb;
            float ss = new BigDecimal(s).setScale(scale, BigDecimal.ROUND_HALF_UP).floatValue();
            result = ss + "MB";            
        } else if (size > oneKb) {
            double s = size / oneKb;
            Double ss = new BigDecimal(s).setScale(scale, BigDecimal.ROUND_HALF_UP).doubleValue();
            result = ss + "KB";
        } else if (size > 0) {
            result = size + "B";
        } else {
            result = String.valueOf(size);
        }
        return result;
    }

    public static String formatAudioTime(int duration) {
        Log.d(TAG, "formatAudioTime:" + duration);
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
            result = String.valueOf(duration);
        }
        return result;
    }

        /**
     * Get bitmap
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

    private static int computeInitialSampleSize(BitmapFactory.Options options, int minSideLength,
            int maxNumOfPixels) {
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
     * Get the current available storage size in byte;
     * 
     * @return available storage size in byte; -1 for no external storage
     *         detected
     */
    public static long getFreeStorageSize() {
        boolean isExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (isExist) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            int availableBlocks = stat.getAvailableBlocks();
            int blockSize = stat.getBlockSize();
            long result = (long) availableBlocks * blockSize;
            Log.d(TAG, "getFreeStorageSize() blockSize: " + blockSize + " availableBlocks: "
                    + availableBlocks + " result: " + result);
            return result;
        }
        return -1;
    }

    public static String formatTimeStampString(Context context, long when, boolean fullFormat) {
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
        
        if (fullFormat) {
            formatFlags |= (DateUtils.FORMAT_SHOW_DATE);
        }
        return DateUtils.formatDateTime(context, when, formatFlags);
    }
    
    public static String formatSimpleTimeStampString(long when) {
        DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd");
        
        DateTime dt = new DateTime(when, DateTimeZone.UTC);
        dt = dt.toDateTime(DateTimeZone.forID("Asia/Shanghai"));
        return FORMATTER.print(dt);
    }
}
