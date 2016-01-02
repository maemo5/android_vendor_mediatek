package com.mediatek.rcs.pam.model;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.PAMException;
import com.mediatek.rcs.pam.Utils;
import com.mediatek.rcs.pam.provider.PAContract.AccountColumns;
import com.mediatek.rcs.pam.provider.PAContract.MediaColumns;


public class PublicAccount implements SanityCheck {
    private static final String TAG = "PAM/PublicAccount";
    
    public static final String[] BASIC_PROJECTION = {
        AccountColumns._ID,
        AccountColumns.UUID,
        AccountColumns.NAME,
        AccountColumns.ID_TYPE,
        AccountColumns.INTRODUCTION,
        AccountColumns.RECOMMEND_LEVEL,
        AccountColumns.LOGO_ID,
        AccountColumns.LOGO_URL,
        AccountColumns.LOGO_PATH,
        AccountColumns.SUBSCRIPTION_STATUS,
    };
    
    public static String[] sDetailProjection = {
        AccountColumns._ID,
        AccountColumns.UUID,
        AccountColumns.NAME,
        AccountColumns.ID_TYPE,
        AccountColumns.INTRODUCTION,
        AccountColumns.RECOMMEND_LEVEL,
        AccountColumns.LOGO_ID,
        AccountColumns.LOGO_URL,
        AccountColumns.LOGO_PATH,
        AccountColumns.SUBSCRIPTION_STATUS,

        AccountColumns.COMPANY,
        AccountColumns.TYPE,
        AccountColumns.UPDATE_TIME,
        AccountColumns.MENU_TYPE,
        AccountColumns.MENU_TIMESTAMP,
        AccountColumns.ACTIVE_STATUS,
        AccountColumns.ACCEPT_STATUS,
        AccountColumns.TELEPHONE,
        AccountColumns.EMAIL,
        AccountColumns.ZIPCODE,
        AccountColumns.ADDRESS,
        AccountColumns.FIELD,
        AccountColumns.QRCODE_URL,
        
        AccountColumns.MENU,
        
        AccountColumns.LAST_MESSAGE,
    };

    
    // basic
    public String uuid;
    public String name;
    public int idtype;
    public String introduction;
    public int recommendLevel;
    public String logoUrl;
    public int subscribeStatus;

    // detail
    public String company;
    public String type;
    public long updateTime;
    public int menuType;
    public long menuTimestamp;
    public String qrcode;
    public int activeStatus;
    public int acceptStatus;
    public String telephone;
    public String email;
    public String zipcode;
    public String address;
    public String field;
    
    // Android Specific
    public long id = Constants.INVALID;
    public String logoPath = null;
    public long logoId = Constants.INVALID;
    public Bitmap logoImage = null;
    public String menu = null;
    private MenuInfo mMenuInfo = null;
    public long lastMessageId = Constants.INVALID;


    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{_class:\"PublicAccountDetail\", uuid:\"")
        .append(uuid)
        .append("\", name:\"")
        .append(name)
        .append("\", recommendLevel:")
        .append(recommendLevel)
        .append(", logoUrl:\"")
        .append(logoUrl)
        .append("\", company:\"")
        .append(company)
        .append("\", introduction:\"")
        .append(introduction)
        .append("\", type:")
        .append(type)
        .append("\", idtype:")
        .append(idtype)
        .append(", updateTime:\"")
        .append(updateTime)
        .append("\", menuType:")
        .append(menuType)
        .append(", menuTimestamp:\"")
        .append(menuTimestamp)
        .append("\", subscribeStatus:")
        .append(subscribeStatus)
        .append("\", activeStatus:")
        .append(activeStatus)
        .append("\", acceptStatus:")
        .append(acceptStatus)
        .append("\", telephone:")
        .append(telephone)
        .append("\", email:")
        .append(email)
        .append("\", zipcode:")
        .append(zipcode)
        .append("\", address:")
        .append(address)
        .append("\", field:")
        .append(field)
        .append(", qrcode:\"")
        .append(qrcode)
        .append("\"}");
        return sb.toString();
    }
    
    public void checkBasicSanity() throws PAMException {
           Utils.throwIf(ResultCode.PARAM_ERROR_MANDATORY_MISSING,
                (TextUtils.isEmpty(uuid) ||
                 TextUtils.isEmpty(name) ||
                 TextUtils.isEmpty(logoUrl) ||
//                 TextUtils.isEmpty(introduction) ||
                 /* Work around for CMCC server issue */
//                 idtype == Constants.INVALID ||
                 subscribeStatus == Constants.INVALID));
   }
    
    @Override
    public void checkSanity() throws PAMException {
        checkBasicSanity();
        
         Utils.throwIf(ResultCode.PARAM_ERROR_MANDATORY_MISSING,
                (/* Work around for CMCC server issue *//* company == null || */
                 TextUtils.isEmpty(type) ||
                 updateTime < 0));
        Utils.throwIf(ResultCode.PARAM_ERROR_INVALID_FORMAT,
                (menuType != Constants.MENU_CONF_YES && menuType != Constants.MENU_CONF_NO));
        Utils.throwIf(ResultCode.PARAM_ERROR_MANDATORY_MISSING,
                (menuType == Constants.MENU_CONF_YES && menuTimestamp < 0));
        Utils.throwIf(ResultCode.PARAM_ERROR_INVALID_FORMAT,
                (acceptStatus != Constants.ACCEPT_STATUS_YES && acceptStatus != Constants.ACCEPT_STATUS_NO));
        Utils.throwIf(ResultCode.PARAM_ERROR_INVALID_FORMAT,
                (activeStatus != Constants.ACTIVE_STATUS_CLOSED &&
                 activeStatus != Constants.ACTIVE_STATUS_NORMAL &&
                 activeStatus != Constants.ACTIVE_STATUS_SUSPENDED));
    }
    
    public MenuInfo menuInfo() {
        if (mMenuInfo == null) {
            mMenuInfo = new MenuInfo();
            mMenuInfo.parseMenuInfoString(menu);
        }
        return mMenuInfo;
    }

    public void loadFullInfoFromCursor(Cursor c) {
        loadBasicInfoFromCursor(c);
        company = c.getString(c.getColumnIndexOrThrow(AccountColumns.COMPANY));
        type = c.getString(c.getColumnIndexOrThrow(AccountColumns.TYPE));
        updateTime = c.getLong(c.getColumnIndexOrThrow(AccountColumns.UPDATE_TIME));
        menuType = c.getInt(c.getColumnIndexOrThrow(AccountColumns.MENU_TYPE));
        menuTimestamp = c.getLong(c.getColumnIndexOrThrow(AccountColumns.MENU_TIMESTAMP));
        activeStatus = c.getInt(c.getColumnIndexOrThrow(AccountColumns.ACTIVE_STATUS));
        acceptStatus = c.getInt(c.getColumnIndexOrThrow(AccountColumns.ACCEPT_STATUS));
        telephone = c.getString(c.getColumnIndexOrThrow(AccountColumns.TELEPHONE));
        email = c.getString(c.getColumnIndexOrThrow(AccountColumns.EMAIL));
        zipcode = c.getString(c.getColumnIndexOrThrow(AccountColumns.ZIPCODE));
        address = c.getString(c.getColumnIndexOrThrow(AccountColumns.ADDRESS));
        field = c.getString(c.getColumnIndexOrThrow(AccountColumns.FIELD));
        qrcode = c.getString(c.getColumnIndexOrThrow(AccountColumns.QRCODE_URL));
        menu = c.getString(c.getColumnIndexOrThrow(AccountColumns.MENU));
        lastMessageId = c.getLong(c.getColumnIndexOrThrow(AccountColumns.LAST_MESSAGE));
    }

    
    public void loadBasicInfoFromCursor(Cursor c) {
        id = c.getLong(c.getColumnIndexOrThrow(AccountColumns._ID));
        uuid = c.getString(c.getColumnIndexOrThrow(AccountColumns.UUID));
        name = c.getString(c.getColumnIndexOrThrow(AccountColumns.NAME));
        idtype = c.getInt(c.getColumnIndexOrThrow(AccountColumns.ID_TYPE));
        introduction = c.getString(c.getColumnIndexOrThrow(AccountColumns.INTRODUCTION));
        recommendLevel = c.getInt(c.getColumnIndexOrThrow(AccountColumns.RECOMMEND_LEVEL));
        logoId = c.getLong(c.getColumnIndexOrThrow(AccountColumns.LOGO_ID));
        logoUrl = c.getString(c.getColumnIndexOrThrow(AccountColumns.LOGO_URL));
        logoPath = c.getString(c.getColumnIndexOrThrow(AccountColumns.LOGO_PATH));
        subscribeStatus = c.getInt(c.getColumnIndexOrThrow(AccountColumns.SUBSCRIPTION_STATUS));
    }
    
    public void storeBasicInfoToContentValues(ContentValues cv) {
        if (id != Constants.INVALID) {
            cv.put(AccountColumns._ID, id);
        }
        cv.put(AccountColumns.UUID, uuid);
        cv.put(AccountColumns.NAME, name);
        cv.put(AccountColumns.ID_TYPE, idtype);
        cv.put(AccountColumns.INTRODUCTION, introduction);
        cv.put(AccountColumns.RECOMMEND_LEVEL, recommendLevel);
        cv.put(AccountColumns.LOGO_ID, logoId);
        cv.put(AccountColumns.LOGO_URL, logoUrl);
        cv.put(AccountColumns.LOGO_PATH, logoPath);
        cv.put(AccountColumns.SUBSCRIPTION_STATUS, subscribeStatus);
    }
    
    public void storeFullInfoToContentValues(ContentValues cv) {
        storeBasicInfoToContentValues(cv);
        cv.put(AccountColumns.COMPANY, company);
        cv.put(AccountColumns.TYPE, type);
        cv.put(AccountColumns.UPDATE_TIME, updateTime);
        cv.put(AccountColumns.MENU_TYPE, menuType);
        cv.put(AccountColumns.MENU_TIMESTAMP, menuTimestamp);
        cv.put(AccountColumns.ACTIVE_STATUS, activeStatus);
        cv.put(AccountColumns.ACCEPT_STATUS, acceptStatus);
        cv.put(AccountColumns.TELEPHONE, telephone);
        cv.put(AccountColumns.EMAIL, email);
        cv.put(AccountColumns.ZIPCODE, zipcode);
        cv.put(AccountColumns.ADDRESS, address);
        cv.put(AccountColumns.FIELD, field);
        cv.put(AccountColumns.QRCODE_URL, qrcode);
        cv.put(AccountColumns.MENU, menu);
        cv.put(AccountColumns.LAST_MESSAGE, lastMessageId);
    }
    
    public static ContentValues storeBasicInfoToContentValues(PublicAccount account) {
        ContentValues cv = new ContentValues();
        account.storeBasicInfoToContentValues(cv);
        return cv;
    }
    
    public static ContentValues storeFullInfoToContentValues(PublicAccount account) {
        ContentValues cv = storeBasicInfoToContentValues(account);
        account.storeFullInfoToContentValues(cv);
        return cv;
    }
    
    public static long queryAccountId(Context context, String uuid, boolean subscribedOnly) {
        ContentResolver cr = context.getContentResolver();
        Cursor c = null;
        long result = Constants.INVALID;
        try {
            if (subscribedOnly) {
                c = cr.query(
                        AccountColumns.CONTENT_URI,
                        new String[]{AccountColumns._ID},
                        AccountColumns.UUID + "=? AND " + AccountColumns.SUBSCRIPTION_STATUS + "=?",
                        new String[]{uuid, Integer.toString(Constants.SUBSCRIPTION_STATUS_YES)},
                        null);
            } else {
                c = cr.query(
                        AccountColumns.CONTENT_URI,
                        new String[]{AccountColumns._ID},
                        AccountColumns.UUID + "=?",
                        new String[]{uuid},
                        null);
            }
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                result = c.getLong(c.getColumnIndexOrThrow(AccountColumns._ID));
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return result;
    }
    
    public static String queryAccountUuid(Context context, long accountId) {
        String result = null;
        Cursor c = null;
        try {
            c = context.getContentResolver().query(
                    AccountColumns.CONTENT_URI,
                    new String[] {AccountColumns.UUID},
                    AccountColumns._ID + "=?",
                    new String[]{Long.toString(accountId)},
                    null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                result = c.getString(c.getColumnIndexOrThrow(AccountColumns.UUID));
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return result;
    }

    /**
     * Insert a row into media table if the URL does not exist.
     * @param cr
     * @param logoUrl
     * @return ID of this row
     */
    public static long insertLogoUrl(ContentResolver cr, String logoUrl) {
        Cursor c = null;
        try {
            c = cr.query(
                    MediaColumns.CONTENT_URI,
                    new String[]{MediaColumns._ID, MediaColumns.URL},
                    MediaColumns.URL + "='" + logoUrl + "'",
                    null,
                    null);
            if (c == null || c.getCount() == 0) {
                ContentValues mediaValues = new ContentValues();
                mediaValues.put(MediaColumns.TYPE, Constants.MEDIA_TYPE_PICTURE);
                mediaValues.put(MediaColumns.TIMESTAMP, Utils.currentTimestamp());
                mediaValues.put(MediaColumns.URL, logoUrl);
                Uri uri = cr.insert(MediaColumns.CONTENT_URI, mediaValues);
                return Long.parseLong(uri.getLastPathSegment());
            } else {
                c.moveToFirst();
                return c.getLong(c.getColumnIndexOrThrow(MediaColumns._ID));
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error(e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

}
