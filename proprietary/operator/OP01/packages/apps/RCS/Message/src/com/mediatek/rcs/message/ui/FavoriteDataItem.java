package com.mediatek.rcs.message.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaFile;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore.Video.Thumbnails;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.mediatek.rcs.common.service.PortraitService;
import com.mediatek.rcs.common.service.PortraitService.Portrait;
import com.mediatek.rcs.common.utils.RCSUtils;
import com.mediatek.rcs.message.R;
import com.mediatek.rcs.message.data.ForwardSendData;
import com.mediatek.rcs.message.location.GeoLocUtils;
import com.mediatek.rcs.message.location.GeoLocXmlParser;
import com.mediatek.rcs.message.ui.SpamDataItem.Constants;
import com.mediatek.rcs.message.utils.RcsMessageUtils;

import java.io.File;

/**
 * The favorite and spam data is wraped a FavoriteSpamItem type to
 * load by adaptor.
 * @author mtk81368
 */
public class FavoriteDataItem {
    private static final String TAG = "com.mediatek.rcsmessage.favspam/FavoriteItem";

    protected Context mContext;
    private int mType;
    private String mTypeName;
    private IFavoriteData mTypeData;

    // common property
    protected Bitmap mImage;
    private long mDate;
    private String mFrom;
    private String mAddress;
    protected int mMsgId;
    private String mPath;
    private String mSize;
    private String mCt;

    /**
     * construct method.
     * @param context the activity that display data.
     * @param msgId message id.
     */
    public FavoriteDataItem(Context context, Cursor cursor) {
        mMsgId = cursor.getInt(cursor.getColumnIndex(Constants.COLUMN_NAME_ID));
        mContext = context;
        mImage = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.ic_contact_picture);
        mFrom = mContext.getString(R.string.unknown);
        mSize = mContext.getString(R.string.unknown);
        mDate = System.currentTimeMillis();
    }

    /**
     * Get data from database and wrapping it to a FavoriteSpamItem.
     * @param cursor The data item cursor.
     * @param portraitService It's used to get contact infomation.
     * @return methord excute result.
     */
    public boolean initData(Cursor cursor, PortraitService portraitService) {
        if (cursor == null) {
            Log.e(TAG, "setData error cursor is null");
            return false;
        }
        this.setFrom(cursor.getString(cursor.getColumnIndex(Constants.COLUMN_NAME_ADDRESS)));
        this.setDate(cursor.getLong(cursor.getColumnIndex(Constants.COLUMN_NAME_DATE)));
        this.setPath(cursor.getString(cursor.getColumnIndex(Constants.COLUMN_NAME_PATH)));
//        mCt = cursor.getString(cursor.getColumnIndex(Constants.COLUMN_NAME_CT));
        mSize = composerFileSize(mPath);

        /**
         * one to one chat message, it is need get group name from db if a muti.
         * chat msg. so group msg need add other branch to do with.
         * if the msg is from me, the mAddress will be null.
         **/
        mAddress = cursor.getString(cursor.getColumnIndex(Constants.COLUMN_NAME_ADDRESS));
        Log.e(TAG, "mAddress = " + mAddress);
        if (mAddress != null && portraitService != null) {
            Portrait protrait = portraitService.requestPortrait(mAddress);
            mImage = PortraitService.decodeString(protrait.mImage);
            mFrom = protrait.mName;
        }
        if (mAddress == null) {
            mFrom = mContext.getString(R.string.me);
            mAddress = mFrom;
        }
        mType = cursor.getInt(cursor.getColumnIndex(Constants.COLUMN_NAME_TYPE));
        mTypeData = createTypeData(mType);
        if (mTypeData == null) {
            Log.e(TAG, "initData fail");
            return false;
        }
        mTypeData.initTypeData(cursor);
        return true;
    }

    private IFavoriteData createTypeData(int mType2) {
        Log.d(TAG, "createTypeData mType2 = " + mType2);
        switch (mType2) {
        case Constants.MSG_TYPE_SMS:
            return new TextData(Constants.MSG_TYPE_SMS);

        case Constants.MSG_TYPE_MMS:
            return new MmsData();

        case Constants.MSG_TYPE_IPMSG:
            if(mPath == null) {
                mCt = Constants.CT_TYPE_IPTEXT;
                Log.d(TAG, "createTypeData new TextData(Constants.MSG_TYPE_IPTEXT)");
                return new TextData(Constants.MSG_TYPE_IPTEXT);
            }
            analysisFileType(mPath);
            if (mCt != null) {
                if (mCt.equals(Constants.CT_TYPE_VEDIO)) {
                    Log.d(TAG, "createTypeData new VideoData()");
                    return new VideoData();
                } else if (mCt.equals(Constants.CT_TYPE_AUDIO)) {
                    Log.d(TAG, "createTypeData new MusicData()");
                    return new MusicData();
                } else if (mCt.equals(Constants.CT_TYPE_IMAGE)) {
                    Log.d(TAG, "createTypeData new PictureData()");
                    return new PictureData();
                } else if (mCt.equals(Constants.CT_TYPE_VCARD)) {
                    Log.d(TAG, "createTypeData new VcardData()");
                    return new VcardData();
                } else if (mCt.equals(Constants.CT_TYPE_GEOLOCATION)) {
                    Log.d(TAG, "createTypeData new GeolocationData()");
                    return new GeolocationData();
                }
            }

        default:
            Log.e(TAG, "unknown type = " + mType2);
            return null;
        }
    }

    private void analysisFileType(String filePath) {
      if (filePath != null) {
          String mimeType = MediaFile.getMimeTypeForFile(filePath);
          if (mimeType == null) {
              mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                      RCSUtils.getFileExtension(filePath));
          }
          if (mimeType != null) {
              if (mimeType.contains(Constants.FILE_TYPE_IMAGE)) {
                 // mTypeName = mContext.getString(R.string.pic_type_name);
                  mCt = Constants.CT_TYPE_IMAGE;
              } else if (mimeType.contains(Constants.FILE_TYPE_AUDIO)
                      || mimeType.contains("application/ogg")) {
                 // mTypeName = mContext.getString(R.string.music_type_name);
                  mCt = Constants.CT_TYPE_AUDIO;
              } else if (mimeType.contains(Constants.FILE_TYPE_VIDEO)) {
                 // mTypeName = mContext.getString(R.string.video_type_name);
                  mCt = Constants.CT_TYPE_VEDIO;
              } else if (filePath.toLowerCase().endsWith(".vcf")) {
                 // mTypeName = mContext.getString(R.string.vcard_type_name);
                  mCt = Constants.CT_TYPE_VCARD;
              } else if (filePath.toLowerCase().endsWith(".xml")) {
                 // mTypeName = mContext.getString(R.string.map_type_name);
                  mCt = Constants.CT_TYPE_GEOLOCATION;
              } else {
                  // Todo
                  Log.d(TAG, "analysisFileType() other type add here!");
              }
          }
      } else {
          Log.w(TAG, "analysisFileType(), file name is null!");
      }
      return;
  }

    /**
     * 
     * @return type info
     */
   public IFavoriteData getTypeData() {
        return mTypeData;
    }

    /**
     * @return contact image.
     */
    public Bitmap getImage() {
        return mImage;
    }

    /**
     * @return data.
     */
    public long getDate() {
        return mDate;
    }

    /**
     * @return get contact name.
     */
    public String getFrom() {
        return mFrom;
    }

    /**
     * @param address phone number.
     */
    public void setAddress(String address) {
        mAddress = address;
    }

    private void setDate(long date) {
        Log.d(TAG, "setDate = " + date);
        mDate = date;
    }

    /**
     * @param from name of the pone number.
     */
    public void setFrom(String from) {
        mFrom = from;
    }

    private void setPath(String path) {
        Log.d(TAG, "path = " + path);
        mPath = path;
    }

    /**
     * @return mMsgId This data msgid in the database.
     */
    public int getMsgId() {
        return mMsgId;
    }

    /**
     * @return This data phone number
     */
    public String getAddress() {
        return mAddress;
    }

    /**
     * @return return the ip message file size.
     */
    public String getSize() {
        return mSize;
    }

    /**
     * @return ip message file path.
     */
    public String getPath() {
        return mPath;
    }

    /**
     * @param bit contact avator.
     */
    public void setImage(Bitmap bit) {
        mImage = bit;
    }

    /**
     * Must Interface of all the data.
     * And each data is must one of them.
     * @author mtk81368
     */
    public interface IFavoriteData {
        /**
         * Init data foward intent.
         * @return foward intent.
         */
        public abstract ForwardSendData initFowardIntent();
        /**
         * It used to get descripte data string.
         * The String will be only show in the spam ui.
         * @return the content string.
         */
        public abstract String getTypeName();
        /**
         * Init data content.
         * @param cursor used to query data from databse.
         * @return init result.
         */
        public abstract boolean initTypeData(Cursor cursor);
        /**
         * create open the item intent.
         * @return open this item intent.
         */
        public abstract Intent initIntent();
        /**
         * get content type, eg: music,picture, vedio, vcard, etc.
         * @return this item content type.
         */
        public abstract int getType();
    }

    private String composerFileSize(String path) {
        if (path == null) {
            Log.e(TAG, "file path == null");
            return null;
        } else {
            Log.e(TAG, "file path = " + path);
        }

        String retSize = null;
        File file = new File(path);
        if (file != null && file.exists()) {
            long size = file.length();
            Log.d(TAG, "file size = " + size);
            retSize = RcsMessageUtils.getDisplaySize(size, mContext);
            Log.d(TAG, "mSize = " + retSize);
        }
        return retSize;
    }

    /**
     * If a ip message is a mms, it will be wrap as a mmsData.
     * @author mtk81368
     */
    public class MmsData implements IFavoriteData {
        private int mType = Constants.MSG_TYPE_MMS;
        private String mSubject = mContext.getString(R.string.no_subject);
        private String mTypeName = mContext.getString(R.string.multimedia_message);

        @Override
        public int getType() {
            return mType;
        }

        private void setSubject(String subject) {
            Resources res = mContext.getResources();
            String str = res.getString(R.string.subject_label);
            str = str + subject;
            mSubject = str;
        }

        public String getSubject() {
            return mSubject;
        }

        @Override
        public ForwardSendData initFowardIntent() {
            return new ForwardSendData("favorite/pdu", mPath);
        }

        @Override
        public String getTypeName() {
            return mTypeName;
        }

        @Override
        public boolean initTypeData(Cursor cursor) {
            this.setSubject(cursor.getString(cursor.getColumnIndex(Constants.COLUMN_NAME_BODY)));
            return true;
        }

        @Override
        public Intent initIntent() {
            Intent intent = new Intent();
//            intent.putExtra(Constants.MSGID, mMsgId);
            return intent;
        }
    }

    /**
     * If a ip message is a audio, it will be wrap as a music data and.
     * display music view in the ui.
     * @author mtk81368
     */
    public class MusicData implements IFavoriteData {
        private int mType = Constants.MSG_TYPE_MUSIC;
        private String mMusicName = mContext.getString(R.string.unknown);
        private String mTypeName = mContext.getString(R.string.music_type_name);

        @Override
        public int getType() {
            return mType;
        }

        public String getMusicName() {
            return mMusicName;
        }

        @Override
        public ForwardSendData initFowardIntent() {
            return new ForwardSendData("video/mp4", mPath);
        }

        @Override
        public String getTypeName() {
            return mTypeName;
        }

        @Override
        public boolean initTypeData(Cursor cursor) {
            mMusicName = mPath.subSequence(mPath.lastIndexOf(File.separator) + 1,
                    mPath.length()).toString();
            return true;
        }

        @Override
        public Intent initIntent() {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            File file = new File(mPath);
            Uri uri = Uri.fromFile(file);
            intent.setDataAndType(uri, "audio/amr");
            return intent;
        }
    }

    /**
     * If a ip message is a Geolocation, it will be wrap as a map data.
     * and it will be diaplay a map picture on the ui.
     * @author mtk81368
     */
    public class GeolocationData implements IFavoriteData {
        private int mType = Constants.MSG_TYPE_GELOCATION;
        private String mTypeName = mContext.getString(R.string.map_type_name);
        private Bitmap mBody;

        @Override
        public int getType() {
            return mType;
        }

        /**
         * @return thumb nail.
         */
        public Bitmap getContentImage() {
            return mBody;
        }


        @Override
        public ForwardSendData initFowardIntent() {
            return new ForwardSendData("geo/*", mPath);
        }

        @Override
        public String getTypeName() {
            return mTypeName;
        }

        @Override
        public boolean initTypeData(Cursor cursor) {
            if (mPath != null) {
                mBody = BitmapFactory
                        .decodeResource(mContext.getResources(), R.drawable.ipmsg_geolocation);
            }
            return true;
        }

        @Override
        public Intent initIntent() {
            Intent intent = new Intent();
            GeoLocXmlParser parser = GeoLocUtils.parseGeoLocXml(mPath);
            double latitude = parser.getLatitude();
            double longitude = parser.getLongitude();
            Log.d(TAG, "parseGeoLocXml:latitude=" + latitude + ",longitude=" + longitude);
            if (latitude != 0.0 || longitude != 0.0) {
                Uri uri = Uri.parse("geo:" + latitude + "," + longitude);  
                intent = new Intent(Intent.ACTION_VIEW, uri);
          } else {
              Toast.makeText(mContext, mContext.getString(R.string.geoloc_map_failed), Toast.LENGTH_SHORT).show();
          }
            return intent;
        }
    }

    /**
     * If a ip message is a picture, it will be wrap as a picture data.
     * and it will be diaplay a thumb nail on the ui.
     * @author mtk81368
     */
    public class PictureData implements IFavoriteData {
        private int mType = Constants.MSG_TYPE_PICTURE;
        private String mTypeName = mContext.getString(R.string.pic_type_name);
        private Bitmap mBody;

        @Override
        public int getType() {
            return mType;
        }

        /**
         * @return thumb nail.
         */
        public Bitmap getContentImage() {
            return mBody;
        }

        private void setBody(String path) {
            File file = new File(path);
            if (file.exists()) {
                Log.d(TAG, "PictureData file exists");
            } else {
                Log.d(TAG, "PictureData file  no exists");
                return;
            }
            Bitmap bitmap = null;
            try {
                bitmap = BitmapFactory.decodeFile(path);
            } catch(java.lang.OutOfMemoryError e) {
                Log.d(TAG, "java.lang.OutOfMemoryError");
                bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                        R.drawable.ipmsg_choose_a_photo);
            }
            mBody = bitmap;
        }

        @Override
        public ForwardSendData initFowardIntent() {
            return new ForwardSendData("image/jpeg", mPath);
        }

        @Override
        public String getTypeName() {
            return mTypeName;
        }

        @Override
        public boolean initTypeData(Cursor cursor) {
            if (mPath != null) {
                setBody(mPath);
            }
            return true;
        }

        // import com.mediatek.rcs.message.plugin.IpMessageConsts.IpMessageType
        // IpMessageType.TEXT=0 PICTURE = 4; VOICE = 5; VCARD = 6; VIDEO = 9
        @Override
        public Intent initIntent() {
            Intent intent = new Intent();
            intent.putExtra("path", mPath);
            intent.putExtra("fav_spam", true);
            intent.putExtra("type", 4);
            intent.setAction("com.mediatek.rcs.message.ui.RcsIpMsgContentShowActivity");
            // intent.putExtra("favorite_spam_type", IpMessageType.PICTURE);
            return intent;
        }
    }

    /**
     * If a ip message is a text or sms, it will be diaplay.
     * content on the ui.
     * @author mtk81368
     */
    public class TextData implements IFavoriteData {
        private int mType;
        private String mTypeName = mContext.getString(R.string.imtext_type_name);
        private String mBody;

        public TextData(int type) {
            Log.d(TAG, "TextData type = " + type);
            mType = type;
            if (mType == Constants.MSG_TYPE_SMS) {
                mTypeName = mContext.getString(R.string.text_message);
            }
        }

        @Override
        public int getType() {
            return mType;
        }

        public String getContent() {
            return mBody;
        }

        @Override
        public String getTypeName() {
            return mTypeName;
        }

        private void setBody(String body) {
            Log.d(TAG, "setBody = " + body);
            mBody = body;
        }

        @Override
        public Intent initIntent() {
            Intent intent = new Intent();
            intent.putExtra("path", mBody);
            intent.putExtra("fav_spam", true);
            intent.putExtra("type", 0);
            intent.setAction("com.mediatek.rcs.message.ui.RcsIpMsgContentShowActivity");
            return intent;
        }

        @Override
        public ForwardSendData initFowardIntent() {
            return new ForwardSendData("text/plain", mBody);
        }

        @Override
        public boolean initTypeData(Cursor cursor) {
            mBody = cursor.getString(cursor.getColumnIndex(Constants.COLUMN_NAME_BODY));
            Log.d(TAG, "initTypeData mBody = " + mBody);
            return true;
        }
    }

    /**
     * If a ip message is a text or vcard, it will be wrap as
     * a vcard data, and dispaly a vcard icon and file name on
     * the ui.
     * @author mtk81368
     */
    public class VcardData implements IFavoriteData {
        private int mType = Constants.MSG_TYPE_VCARD;
        private String mTypeName = mContext.getString(R.string.vcard_type_name);
        private String mVcardName = mContext.getString(R.string.unknown);

        @Override
        public int getType() {
            return mType;
        }

        @Override
        public boolean initTypeData(Cursor cursor) {
            mVcardName = mPath.subSequence(mPath.lastIndexOf(File.separator) + 1,
                    mPath.length()).toString();
            return true;
        }

        /**
         * getView get the vcard name.
         * @return vcard file name.
         */
        public String getName() {
            return mVcardName;
        }

        @Override
        public Intent initIntent() {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            File file = new File(mPath);
            Uri uri = Uri.fromFile(file);
            intent.setDataAndType(uri, "text/x-vcard");
            return intent;
        }

        @Override
        public ForwardSendData initFowardIntent() {
            return new ForwardSendData("text/x-vcard", mPath);
        }

        @Override
        public String getTypeName() {
            return mTypeName;
        }
    }

    /**
     * If a ip message is a text or video, it will be wrap as
     * a videoData. And display Thumbnail on the ui.
     * @author mtk81368
     */
    public class VideoData implements IFavoriteData {
        private int mType = Constants.MSG_TYPE_VIDEO;
        private String mTypeName = mContext.getString(R.string.video_type_name);
        private Bitmap mBody;

        @Override
        public int getType() {
            return mType;
        }

        /**
         * getView get the video firt frame.
         * @return video firt frame thumb nail.
         */
        public Bitmap getContentImage() {
            return mBody;
        }

        private Bitmap createVideoThumbnail(String filePath) {

            File file = new File(filePath);
            if (file.exists()) {
                Log.d(TAG, "VideoData file exists");
            } else {
                Log.d(TAG, "VideoData file  no exists");
            }

            Bitmap bitmap = null;
            try {
                bitmap = ThumbnailUtils.createVideoThumbnail(filePath, Thumbnails.MINI_KIND);
                bitmap = ThumbnailUtils.extractThumbnail(bitmap, 500, 300);
            } catch(java.lang.OutOfMemoryError e) {
                Log.d(TAG, "java.lang.OutOfMemoryError");
                bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                        R.drawable.ipmsg_choose_a_video);
            }
            if (bitmap == null) {
                Log.d(TAG, "VideoData bitmap = null");
            } else {
                Log.d(TAG, "VideoData bitmap !=  null");
            }

            return bitmap;
        }

        @Override
        public ForwardSendData initFowardIntent() {
            return new ForwardSendData("video/mp4", mPath);
        }

        @Override
        public String getTypeName() {
            return mTypeName;
        }

        @Override
        public boolean initTypeData(Cursor cursor) {
            mBody = createVideoThumbnail(mPath);
            return true;
        }

        @Override
        public Intent initIntent() {
            Intent intent = new Intent();
            intent.putExtra("path", mPath);
            intent.putExtra("fav_spam", true);
            intent.putExtra("type", 9);
            intent.setAction("com.mediatek.rcs.message.ui.RcsIpMsgContentShowActivity");
            return intent;
        }
    }

    /**
     * Constants of the favorite and spam part.
     * @author mtk81368
     */
    public static class Constants {
        // message type
        public static final int MSG_TYPE_SMS = 1;
        public static final int MSG_TYPE_MMS = 2;
        public static final int MSG_TYPE_MUSIC = 0x4;
        public static final int MSG_TYPE_IPMSG = 3;
        public static final int MSG_TYPE_PUBLICACCOUNT = 0x7;
        public static final int MSG_TYPE_VIDEO = 0x5;
        public static final int MSG_TYPE_VCARD = 0x6;
        public static final int MSG_TYPE_PICTURE = 0x8;
        public static final int MSG_TYPE_IPTEXT = 0x11;
        public static final int MSG_TYPE_GELOCATION = 0x9;
        public static final int MSG_TYPE_UNKOWN = 0x100;

        public static final String COLUMN_NAME_ID = "_id";
        public static final String COLUMN_NAME_ADDRESS = "address";
        public static final String COLUMN_NAME_DATE = "date";
        public static final String COLUMN_NAME_TYPE = "type";
        public static final String COLUMN_NAME_CT = "ct";
        public static final String COLUMN_NAME_BODY = "body";
        public static final String COLUMN_NAME_PATH = "path";

        public static final String CT_TYPE_VEDIO = "video/mp4";
        public static final String CT_TYPE_AUDIO = "audio/mp3";
        public static final String CT_TYPE_IMAGE = "image/jpeg";
        public static final String CT_TYPE_VCARD = "text/x-vcard";
        public static final String CT_TYPE_GEOLOCATION = "xml/*";
        public static final String CT_TYPE_IPTEXT = "text/plain";

        /** Image. */
        public static final String FILE_TYPE_IMAGE = "image";
        /** Audio. */
        public static final String FILE_TYPE_AUDIO = "audio";
        /** Video. */
        public static final String FILE_TYPE_VIDEO = "video";
        /** Text. */
        public static final String FILE_TYPE_TEXT = "text";
        /** Application. */
        public static final String FILE_TYPE_APP = "application";
        /** forward sms flag when ue havent config rcs situation */
        public static final int FORWARD_TYPE_SMS = 1;
        /** forward mms flag when ue havent config rcs situation */
        public static final int FORWARD_TYPE_MMS = 2;
    }
}
