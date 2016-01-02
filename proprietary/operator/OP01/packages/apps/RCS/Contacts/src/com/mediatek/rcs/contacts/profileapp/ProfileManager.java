/*
* This software/firmware and related documentation ("MediaTek Software") are
* protected under relevant copyright laws. The information contained herein
* is confidential and proprietary to MediaTek Inc. and/or its licensors.
* Without the prior written permission of MediaTek inc. and/or its licensors,
* any reproduction, modification, use or disclosure of MediaTek Software,
* and information contained herein, in whole or in part, shall be strictly prohibited.
*/
/* MediaTek Inc. (C) 2014. All rights reserved.
*
* BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
* THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
* RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
* AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
* NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
* SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
* SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
* THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
* THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
* CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
* SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
* STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
* CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
* AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
* OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
* MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
*
* The following software/firmware and/or related documentation ("MediaTek Software")
* have been modified by MediaTek Inc. All revisions are subject to any receiver's
* applicable license agreements with MediaTek Inc.
*/

package com.mediatek.rcs.contacts.profileapp;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Profile;
import android.util.Base64;
import android.util.Log;

import com.cmcc.ccs.profile.ProfileListener;
import com.cmcc.ccs.profile.ProfileService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.gsma.joyn.JoynServiceListener;

/* ProfileManager. */
public class ProfileManager extends ProfileListener implements JoynServiceListener{

    private static ProfileManager sInstance;
    private static ProfileInfo mProfile = null;
    private ArrayList<ProfileManagerListener> mListenerList 
            = new ArrayList<ProfileManagerListener>();
    private static final Uri PROFILE_CONTENT_URI 
            = Uri.parse("content://com.cmcc.ccs.profile");
    private static final String TAG = ProfileManager.class.getName();
    private Context mContext;
    private ProfileService mProfileSevs;
    private HashMap<String, String> mTempProfileInfoList = new HashMap<String, String>();
    private ArrayList<String> mTempNumberList = new ArrayList<String>();

    private ProfileManagerHandler mHandler;
    private static final int MSG_GET_PROFILE_FROM_DB = 1001;
    private static final int MSG_UPDATE_PROFILE_TO_SERVER = 1002;
    private static final int MSG_UPDATE_PROFILE_TO_CONTACT_DB = 1003;

    private int mServerState;
    private static final int SERVER_INIT = 0;
    private static final int SERVER_CONNECTING = 1;
    private static final int SERVER_CONNECTED = 2;
    private static final int SERVER_GET_PROFILE = 4;
    private static final int SERVER_SET_PROFILE = 8;
    private static final int SERVER_GET_CONTACT_PORTRAIT = 16;

    public static final int SERVER_RESULT_NONE = 0;
    public static final int SERVER_RESULT_GET_PROFILE = 1;
    public static final int SERVER_RESULT_SET_PROFILE = 2;

    private ProfileManager(Context context) {
        Log.i(TAG, "ProfileManager: Constructure");
        mProfileSevs = new ProfileService(context, this);
        mProfileSevs.addProfileListener(this);
        mProfileSevs.connect();
        mServerState = SERVER_CONNECTING;
        mContext = context;
        
        HandlerThread thread = new HandlerThread("ProfileManagerHandler");
        thread.start();
        mHandler = new ProfileManagerHandler(thread.getLooper());
        
        getMyProfileFromLocal();
    }

    /* single instance class */
    public static ProfileManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ProfileManager(context);
        }
        return sInstance;
    }

    public void onServiceConnected() {
        Log.i(TAG, "onServiceConnected: serverState = " + mServerState);
        if ((mServerState & SERVER_GET_PROFILE) > 0) {
            mProfileSevs.getProfileInfo();
        } 
        if ((mServerState & SERVER_SET_PROFILE) > 0) {
            mProfileSevs.setProfileInfo(mTempProfileInfoList);
            mTempProfileInfoList.clear();
        }
        if ((mServerState & SERVER_GET_CONTACT_PORTRAIT) > 0) {
            Log.i(TAG, "onServiceConnected: number size = " + mTempNumberList.size());
            for (String number : mTempNumberList) {
                mProfileSevs.getContactPortrait(number);
            }
            mTempNumberList.clear();
        }

        mServerState = SERVER_CONNECTED;
    }
    
    public void onServiceDisconnected(int code) {
        Log.i(TAG, "onServiceDisconnected");
        mServerState = SERVER_INIT;
    }

    /**
     * register listener.
     * @param listener :the listener you what to registered.
     */
    public void registerProfileManagerListener(ProfileManagerListener listener) {
        if (!mListenerList.contains(listener)) {
            mListenerList.add(listener);
            /* notify listener after register */
            listener.onProfileInfoUpdated(0, SERVER_RESULT_NONE, mProfile);
        }
    }

    /**
     * unregister listener.
     * @param listener
     */
    public void unregisterProfileManagerListener(ProfileManagerListener listener) {
        if (mListenerList.contains(listener)) {
            mListenerList.remove(listener);
        }
    }

    /**
     * Get my profile from server
     */
    public void getMyProfileFromServer() {
        Log.i(TAG, "getMyProfileFromServer: serverState = " + mServerState);
        if ((mServerState & SERVER_CONNECTED) > 0) {
            mProfileSevs.getProfileInfo();
        } else if ((mServerState & SERVER_CONNECTING) > 0) {
            mServerState |= SERVER_GET_PROFILE;
        } else {
            mServerState |= SERVER_GET_PROFILE;
            mProfileSevs.connect();
            mServerState |= SERVER_CONNECTING;
        }
    }

    /**
     * Get my profile from local variable.
     * @return ProfileInfo :personal profile information.
     */
    public ProfileInfo getMyProfileFromLocal() {
        Log.i(TAG, "getMyProfileFromLocal:");
        if (mProfile == null) {
            mProfile = new ProfileInfo();      
            Message msg = mHandler.obtainMessage(MSG_GET_PROFILE_FROM_DB);
            msg.arg1 = 0;
            msg.sendToTarget();
        }
        return mProfile;
    }

    /**
     * Get my profile from data base.
     * @return void
     */
    public void getMyProfileFromDB() {
        Log.i(TAG, "getMyProfileFromDB:");
        removeMessageIfExist(MSG_GET_PROFILE_FROM_DB);
        Message msg = mHandler.obtainMessage(MSG_GET_PROFILE_FROM_DB);
        msg.arg1 = 0;
        msg.sendToTarget();
    }

    /**
     * Get contact icon by number.
     * @param number contact number
     */
    public void getContactPortraitByNumber(final String number) {
   
        Log.i(TAG, "getContactPortraitByNumber: serverState = " + mServerState 
              + "; number = " + number);
        if ((mServerState & SERVER_CONNECTED) > 0) {
            mProfileSevs.getContactPortrait(number);
        } else if ((mServerState & SERVER_CONNECTING) > 0) {
            mServerState |= SERVER_GET_CONTACT_PORTRAIT;
            mTempNumberList.add(number);
        } else {
            mServerState |= SERVER_GET_CONTACT_PORTRAIT;
            mProfileSevs.connect();
            mServerState |= SERVER_CONNECTING;
            mTempNumberList.add(number);
        }

    }

    /**
     * API entry for update Profile other number.
     * @param list : other number list.
     * @return void
     */
    public void updateProfileOtherNumber(ArrayList<ProfileInfo.OtherNumberInfo> list) {

        mProfile.setAllOtherNumber(list);
        
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(ProfileInfo.PHONE_NUMBER_SECOND, mProfile.getOtherNumberToString());

        updateProfileByType(map);
    }

    /**
     * API entry for update Profile to Server and database by data type.
     * @param type : data type.
     * @param content : data content.
     */
    public void updateProfileByType(HashMap map) {
    
        Iterator iter = map.entrySet().iterator();
        String type = null;
        String content = null;
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            type = (String)entry.getKey();
            content = (String)entry.getValue();
            Log.d(TAG, "updateProfileByType: type = " + type + " content = " + content);
            if (type == ProfileInfo.PORTRAIT) {
                if (content != null && !content.equals("")) {
                    byte[] photo = Base64.decode(content, Base64.DEFAULT);
                    mProfile.setPhoto(photo);
                } else {
                    mProfile.setPhoto(null);
                }
            } else if (type != ProfileInfo.PHONE_NUMBER_SECOND) {
                mProfile.setContentByKey(type, content);
            }
        }
        removeMessageIfExist(MSG_UPDATE_PROFILE_TO_CONTACT_DB);
        Message msg = mHandler.obtainMessage(MSG_UPDATE_PROFILE_TO_CONTACT_DB);
        msg.sendToTarget();
        
        updateProfileToServer(map);
        notifyProfileInfoUpdate(0, SERVER_RESULT_NONE);
        
    }

    /**
     * Update Profile to Server by data type.
     * @param type : data type.
     * @param content : data content.
     * @return void
     */
    private void updateProfileToServer(HashMap map) {
        Log.d(TAG, "updateProfileToServer: serverState = " + mServerState);

        if ((mServerState & SERVER_CONNECTED) > 0) {
            mProfileSevs.setProfileInfo(map);
        } else if ((mServerState & SERVER_CONNECTING) > 0) {
            mServerState |= SERVER_SET_PROFILE;
        } else {
            mServerState |= SERVER_SET_PROFILE;
            mProfileSevs.connect();
            mServerState |= SERVER_CONNECTING;
        }
        mTempProfileInfoList.clear();
        mTempProfileInfoList.putAll(map);

    }

    /**
     * Update Profile to Database by data type.
     * @param type : data type.
     * @param content : data content.
     * @return void
     */
    private void updateProfileDbByType(String type, String content) {
        Log.d(TAG, "updateProfileDbByType: type = " + type + " content = " + content);

        ContentValues values = new ContentValues();
        values.put(type, content);

        String[] projections = {"_id"};
        ContentResolver resolver = mContext.getContentResolver();
        Cursor c = resolver.query(PROFILE_CONTENT_URI, projections, null, null, null);

        if (c != null && c.getCount() >= 1) {
            c.moveToFirst();
            long id = c.getLong(c.getColumnIndex("_id"));
            String selection = "_id = " + String.valueOf(id);
            resolver.update(PROFILE_CONTENT_URI, values, selection, null);
        } else {
            resolver.insert(PROFILE_CONTENT_URI, values);
        }
        c.close();
    }

    /**
     * Update profile info from data base.
     * @param context
     */
    private void getProfileFromDB(Context context) {
        Log.d(TAG, "getProfileFromDB: ");
        ContentResolver resolver = context.getContentResolver();
        Cursor c = resolver.query(PROFILE_CONTENT_URI,
                ProfileInfo.mAllProfileKeySet, null, null, null);
        if (c != null) {
            c.moveToFirst();
            if (c.getCount() >= 1) {
                for (String key : ProfileInfo.mAllProfileKeySet) {

                    if (key.equals(ProfileInfo.PORTRAIT)) {
                        
                        String photoStr = c.getString(c.getColumnIndex(ProfileInfo.PORTRAIT));
                        if (photoStr != null && !photoStr.equals("")) {
                            byte[] photo = Base64.decode(photoStr, Base64.DEFAULT);
                            mProfile.setPhoto(photo);
                        } else {
                            mProfile.setPhoto(null);
                        }
                    } else if (key.equals(ProfileInfo.PHONE_NUMBER_SECOND)) {
                    
                        String otherNumber = c.getString(c.getColumnIndex(ProfileInfo.PHONE_NUMBER_SECOND));
                        mProfile.parseOtherNumberStringToMap(otherNumber);
                    } else {
                        mProfile.setContentByKey(key, c.getString(c.getColumnIndex(key)));
                    }
                }
            } else {
                mProfile.clearAll();
            }
        }
        c.close();
        
        removeMessageIfExist(MSG_UPDATE_PROFILE_TO_CONTACT_DB);
        Message msg = mHandler.obtainMessage(MSG_UPDATE_PROFILE_TO_CONTACT_DB);
        msg.sendToTarget();
    }

    /**
     * Notify listener to update profile information.
     */
    private void notifyProfileInfoUpdate(int flag, int operation) {
        for (ProfileManagerListener listener: mListenerList) {
            listener.onProfileInfoUpdated(flag, operation, mProfile);
        }
    }

    /**
     * Notify all listeners that contact info changed.
     * @param flag
     * @param number
     * @param photo
     */
    private void notifyContactInfoUpdate(int flag, String number, byte[] photo) {
        for (ProfileManagerListener listener: mListenerList) {
            listener.onContactIconGotten(flag, number, photo);
        }
    }

    /**
     * Update profile info to Contact Profile DB.
     * @param type: data type.
     * @param content: data content.
     */
    public void updateProfileInfoToContactDB() {
    
        Log.d(TAG, "updateProfileInfoToContactDB: ");
        long rawContactId;
        ContentResolver resolver = mContext.getContentResolver();

        String[] projections = {Profile._ID, Profile.NAME_RAW_CONTACT_ID};

        Cursor c = resolver.query(Profile.CONTENT_URI, projections, null, null, null);
        Uri dataUri = Profile.CONTENT_URI.buildUpon().appendPath("data").build();

        if (c.getCount() >= 1 && c.moveToFirst()) {
            rawContactId = c.getLong(c.getColumnIndex(Profile.NAME_RAW_CONTACT_ID));
        } else {
            ContentValues values = new ContentValues();
            values.put(Profile.DISPLAY_NAME, ProfileInfo.getContentByKey(ProfileInfo.NAME));
            Uri newUri = resolver.insert(Profile.CONTENT_RAW_CONTACTS_URI, values);
            rawContactId = ContentUris.parseId(newUri);
        }
        c.close();

        updateProfileNameToContactDB(rawContactId, mContext, dataUri);
        updateProfilePhotoToContactDB(rawContactId, mContext, dataUri);
        updateProfileNumberToContactDB(rawContactId, mContext, dataUri);
    }

    /**
     * Update profile info' name to Contact Profile DB.
     * @param rawId:  raw contact ID.
     * @param context: context. 
     * @param dataUri:  Data table Uri.
     */
    private void updateProfileNameToContactDB(long rawId, Context context, Uri dataUri) {
        Log.d(TAG, "updateProfileNameToContactDB: name = " + mProfile.getName());
        ContentResolver resolver = context.getContentResolver();
        ContentValues values =  new ContentValues();

        String[] projections = {Data._ID};
        String selection = Data.MIMETYPE + " = ?" + " AND (" + Data.RAW_CONTACT_ID + " = ?)";
        String[] selectionArgs = new String[] { 
                StructuredName.CONTENT_ITEM_TYPE, 
                String.valueOf(rawId) };

        Cursor c = resolver.query(dataUri, projections, selection, selectionArgs, null, null);
        if (c != null && c.getCount() >= 1) {
            values.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
            values.put(StructuredName.DISPLAY_NAME, mProfile.getName());
            values.put(StructuredName.GIVEN_NAME, ProfileInfo.getContentByKey(ProfileInfo.FIRST_NAME));
            values.put(StructuredName.FAMILY_NAME, ProfileInfo.getContentByKey(ProfileInfo.LAST_NAME));
            values.put(StructuredName.RAW_CONTACT_ID, rawId);
            resolver.update(dataUri, values, selection, selectionArgs);
        } else {
            values.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
            values.put(StructuredName.DISPLAY_NAME, mProfile.getName());
            values.put(StructuredName.GIVEN_NAME, ProfileInfo.getContentByKey(ProfileInfo.FIRST_NAME));
            values.put(StructuredName.FAMILY_NAME, ProfileInfo.getContentByKey(ProfileInfo.LAST_NAME));
            values.put(StructuredName.RAW_CONTACT_ID, rawId);
            resolver.insert(dataUri, values);
        }
        c.close();


    }

    /**
     * Update profile info' photo to Contact Profile DB.
     * @param rawId:  raw contact ID.
     * @param context
     * @param dataUri:  Data table Uri.
     */
    private void updateProfilePhotoToContactDB(long rawId, Context context, Uri dataUri) {

        ContentResolver resolver = context.getContentResolver();
        ContentValues values =  new ContentValues();

        String[] projections = {Data._ID};
        String selection = Data.MIMETYPE + " = ?" + " AND (" + Data.RAW_CONTACT_ID + " = ?)";
        String[] selectionArgs = new String[] { Photo.CONTENT_ITEM_TYPE, String.valueOf(rawId) };

        Cursor c = resolver.query(dataUri, projections, selection, selectionArgs, null, null);
        if (c != null && c.getCount() >= 1) {
            values.put(Photo.PHOTO, mProfile.photo);
            values.put(Photo.RAW_CONTACT_ID, rawId);
            resolver.update(dataUri, values, selection, selectionArgs);
        } else {
            values.put(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
            values.put(Photo.PHOTO, mProfile.photo);
            values.put(Photo.RAW_CONTACT_ID, rawId);
            resolver.insert(dataUri, values);
        }
        c.close();

    }

    /**
     * Update profile info' Number to Contact Profile DB.
     * @param rawId:  raw contact ID.
     * @param context
     * @param dataUri:  Data table Uri.
     */
    private void updateProfileNumberToContactDB(long rawId, Context context, Uri dataUri) {

        ContentResolver resolver = context.getContentResolver();
        ContentValues values =  new ContentValues();

        String[] projections = {Data._ID};
        String selection = Data.MIMETYPE + " = ?" + " AND (" + Data.RAW_CONTACT_ID + " = ?)";
        String[] selectionArgs = new String[] { Phone.CONTENT_ITEM_TYPE, String.valueOf(rawId) };

        Cursor c = resolver.query(dataUri, projections, selection, selectionArgs, null, null);
        String number = mProfile.getContentByKey(ProfileInfo.PHONE_NUMBER);
        Log.d(TAG, "updateProfileNumberToContactDB: number = " + number);
        if (c != null && c.getCount() >= 1) {
            if (number == null || number.equals("")) {
                resolver.delete(dataUri, selection, selectionArgs);
            } else {
                values.put(Phone.NUMBER, number);
                values.put(Phone.RAW_CONTACT_ID, rawId);
                resolver.update(dataUri, values, selection, selectionArgs);
            }
        } else if (number != null && !number.equals("")) {
            values.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
            values.put(Phone.NUMBER, number);
            values.put(Phone.RAW_CONTACT_ID, rawId);
            resolver.insert(dataUri, values);
        }

        c.close();

    }

    /**
     * remove unhandled message.
     * @param what:  msg id
     */
    private void removeMessageIfExist(int what) {
        if (mHandler.hasMessages(what)) {
            Log.d(TAG, "onGetProfile: remove messages " + what);
            mHandler.removeMessages(what);
        }
    }

    private final class ProfileManagerHandler extends Handler {
    
            public ProfileManagerHandler(Looper looper) {
                super(looper);
            }
            
            @Override
            public void handleMessage (Message msg) {
                Log.d(TAG, "handleMessage: msg = " + msg.what);
                if(null == msg)
                {
                    return;
                }
                switch (msg.what) {
                    case MSG_GET_PROFILE_FROM_DB:
                        getProfileFromDB(mContext);
                        notifyProfileInfoUpdate(msg.arg1, msg.arg2);
                        break;
                                            
                    case MSG_UPDATE_PROFILE_TO_CONTACT_DB:
                        updateProfileInfoToContactDB();
                        break;
                        
                    default:
                        break;
                }
            }
    }
    

    /* Profile manager listener communicate with UI */
    interface ProfileManagerListener {

        /* Notify profile updating information */
        void onProfileInfoUpdated(int flag, int operation, ProfileInfo profile);

        /* Notify Contact icon update information */
        void onContactIconGotten(int flag, String number, byte[]icon);
    }

    /**
     * ProfileListener:
     * listener call back when profile update done.
     * @param result:
     */
    public void onUpdateProfile(int result) {
        Log.i(TAG, "onUpdateProfile: result = " + result);
        if (result == ProfileService.OK || result == ProfileService.NOUPDATE) {
            notifyProfileInfoUpdate(result, SERVER_RESULT_SET_PROFILE);
        } else {
            removeMessageIfExist(MSG_GET_PROFILE_FROM_DB);
            Message msg = mHandler.obtainMessage(MSG_GET_PROFILE_FROM_DB);
            msg.arg1 = result;
            msg.arg2 = SERVER_RESULT_SET_PROFILE;
            msg.sendToTarget();
        }
    }

    /**
     * ProfileListener:
     * listener when get profile result back.
     * @param result:
     */
    public void onGetProfile(int result) {
        Log.d(TAG, "onGetProfile: result = " + result);
        removeMessageIfExist(MSG_GET_PROFILE_FROM_DB);
        Message msg = mHandler.obtainMessage(MSG_GET_PROFILE_FROM_DB);
        msg.arg1 = result;
        msg.arg2 = SERVER_RESULT_GET_PROFILE;
        msg.sendToTarget();
    }

   /**
     * ProfileListener:
     * listener when get Contact portrait get back.
     * @param result:
     * @param number:
     * @param portrait:
     */
   public void onGetContactPortrait (int result, String portrait, String number, String mimeType) {
       Log.i(TAG, "onGetContactPortrait: result = " + result + " number = " + number);
       byte[] photo = null;
       if (portrait != null) {
           photo = Base64.decode(portrait, Base64.DEFAULT);
       }
       notifyContactInfoUpdate(result, number, photo);
   }


}
