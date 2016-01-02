package com.mediatek.rcs.message.utils;

import java.io.File;

import android.content.Context;
import android.text.TextUtils;

import com.mediatek.mms.ipmessage.IpConfig;

public class RcsMessageConfig extends IpConfig {
    private static final String TAG = "RCSConfig";
    
    // M: add for ipmessage
     private static String sPicTempPath = "";
     private static String sAudioTempPath = "";
     private static String sVideoTempPath = "";
     private static String sVcardTempPath = "";
     private static String sCalendarTempPath = "";
     private static String sGeoloTempPath = "";
     private  static long sSubId = 0;
     private Context mContext;
     
     public RcsMessageConfig(Context context) {
         mContext = context;
     }
    
    public int getMaxTextLimit(Context context) {
        //TODO; modify the max text
        return 3000;
    }
    
    public boolean onIpInit(Context context) {
        initializeIpMessageFilePath(mContext);
        return false;
    }
    
    
    public static void setSubId(long subId) {
        sSubId = subId;
    }
    
    public static long getSubId() {
        return sSubId;
    }
    
    public static boolean isActivated() {
        return true;
    }
    
    public static boolean isActivated(Context context) {
        return true;
    }
    
    public static boolean isServiceEnabled(Context mContext) {
        return true;
    }


    private static boolean sDisplayBurned;
    public static void setEditingDisplayBurnedMsg(boolean isBurned) {
        sDisplayBurned = isBurned;
    }
    public static boolean isEditingDisplayBurnedMsg() {
        return sDisplayBurned;
    }
    


    private static void initializeIpMessageFilePath(Context context) {
        if (RcsMessageUtils.getSDCardStatus()) {
              sPicTempPath = RcsMessageUtils.getSDCardPath(context)
                      + RcsMessageUtils.IP_MESSAGE_FILE_PATH + "picture";
              File picturePath = new File(sPicTempPath);
              if (!picturePath.exists()) {
                  picturePath.mkdirs();
              }

              sAudioTempPath = RcsMessageUtils.getSDCardPath(context)
                      + RcsMessageUtils.IP_MESSAGE_FILE_PATH + "audio";
              File audioPath = new File(sAudioTempPath);
              if (!audioPath.exists()) {
                  audioPath.mkdirs();
              }

              sVideoTempPath = RcsMessageUtils.getSDCardPath(context)
                      + RcsMessageUtils.IP_MESSAGE_FILE_PATH + "video";
              File videoPath = new File(sVideoTempPath);
              if (!videoPath.exists()) {
                  videoPath.mkdirs();
              }
    
              sVcardTempPath = RcsMessageUtils.getSDCardPath(context)
                      + RcsMessageUtils.IP_MESSAGE_FILE_PATH + "vcard";
              File vcardPath = new File(sVcardTempPath);
              if (!vcardPath.exists()) {
                  vcardPath.mkdirs();
              }
    
              sCalendarTempPath = RcsMessageUtils.getSDCardPath(context)
                      + RcsMessageUtils.IP_MESSAGE_FILE_PATH + "calendar";
              File calendarPath = new File(sCalendarTempPath);
              if (!calendarPath.exists()) {
                  calendarPath.mkdirs();
              }
    
              String cachePath = RcsMessageUtils.getCachePath(context);
              if (cachePath != null) {
                  File f = new File(cachePath);
                  if (!f.exists()) {
                      f.mkdirs();
                  }
              }
        }
    }

    public static String getPicTempPath(Context context) {
        if (TextUtils.isEmpty(sPicTempPath)) {
            sPicTempPath = RcsMessageUtils.getSDCardPath(context)
                    + RcsMessageUtils.IP_MESSAGE_FILE_PATH + "picture";
            File picturePath = new File(sPicTempPath);
            if (!picturePath.exists()) {
                picturePath.mkdirs();
            }
        }
        return sPicTempPath;
    }

    public static String getAudioTempPath(Context context) {
        if (TextUtils.isEmpty(sAudioTempPath)) {
            sAudioTempPath = RcsMessageUtils.getSDCardPath(context)
                    + RcsMessageUtils.IP_MESSAGE_FILE_PATH + "audio";
            File audioPath = new File(sAudioTempPath);
            if (!audioPath.exists()) {
                audioPath.mkdirs();
            }
        }
        return sAudioTempPath;
    }

    public static String getVideoTempPath(Context context) {
        if (TextUtils.isEmpty(sVideoTempPath)) {
            sVideoTempPath = RcsMessageUtils.getSDCardPath(context)
                    + RcsMessageUtils.IP_MESSAGE_FILE_PATH + "video";
            File videoPath = new File(sVideoTempPath);
            if (!videoPath.exists()) {
                videoPath.mkdirs();
            }
        }
        return sVideoTempPath;
    }

    public static String getVcardTempPath(Context context) {
        if (TextUtils.isEmpty(sVcardTempPath)) {
            sVcardTempPath = RcsMessageUtils.getSDCardPath(context)
                    + RcsMessageUtils.IP_MESSAGE_FILE_PATH + "vcard";
            File vcardPath = new File(sVcardTempPath);
            if (!vcardPath.exists()) {
                vcardPath.mkdirs();
            }
        }
        return sVcardTempPath;
    }

    public static String getVcalendarTempPath(Context context) {
        if (TextUtils.isEmpty(sCalendarTempPath)) {
            sCalendarTempPath = RcsMessageUtils.getSDCardPath(context)
                    + RcsMessageUtils.IP_MESSAGE_FILE_PATH + "calendar";
            File calendarPath = new File(sCalendarTempPath);
            if (!calendarPath.exists()) {
                calendarPath.mkdirs();
            }
        }
        return sCalendarTempPath;
    }
    public static String getGeolocTempPath(Context context) {
        if (TextUtils.isEmpty(sGeoloTempPath)) {
            sGeoloTempPath = RcsMessageUtils.getSDCardPath(context)
                    + RcsMessageUtils.IP_MESSAGE_FILE_PATH + "loc";
            File geolocPath = new File(sGeoloTempPath);
            if (!geolocPath.exists()) {
                geolocPath.mkdirs();
            }
        }
        return sGeoloTempPath;
    }
}
