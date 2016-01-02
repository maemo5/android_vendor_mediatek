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

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.android.vcard.VCardBuilder;
import com.android.vcard.VCardConfig;
import com.google.android.collect.Lists;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

/**
 * ProfileVCardBuilder: build profile uri to vcard file.
 */
public class ProfileVCardBuilder {

    private static ProfileVCardBuilder mInstance;
    private Context mContext;
    private static final Uri PROFILE_CONTENT_URI = Uri.parse("content://com.cmcc.ccs.profile");

    private static final int MSG_BUILD_PROFILE_VCARD = 1001;
    private static final String PROFILE_AUTHORITY = "com.mediatek.profileapp";
    private static final String TAG = "ProfileVCardBuilder";
    private ProfileInfo mProfile;
    
    private ProfileVCardBuilder() {
        
    }

    public static ProfileVCardBuilder getInstance() {

        
        if (mInstance == null) {
            mInstance = new ProfileVCardBuilder();
        } 
        return mInstance;
    }


    public void buildProfileVCard(VcardBuilderCallback callback, Context context) {
        
        mContext = context;
        HandlerThread thread = new HandlerThread("VCardBuilderHandler");
        thread.start();
        VCardBuilderHandler handler = new VCardBuilderHandler(thread.getLooper());
        
        Message msg = handler.obtainMessage(MSG_BUILD_PROFILE_VCARD);
        msg.obj = callback;
        msg.sendToTarget();
    }

    private void handleBuildVCard(VcardBuilderCallback callback) {
        
        mProfile = ProfileManager
                .getInstance(mContext).getMyProfileFromLocal();
        
        VCardBuilder builder = new VCardBuilder(VCardConfig.VERSION_40 
                | VCardConfig.VERSION_30);

        appendPhoto(builder);
        appendName(builder);
        appendPostal(builder);
        appendEvents(builder);
        appendPhones(builder);
        appendEmails(builder);
        appendCompany(builder);
        
        Uri uri = writeToVcfFile(builder);
        callback.onVcardBuildDone(uri);
        
    }

    private void appendPhoto(VCardBuilder builder) {
        
        if (mProfile.photo!= null) {
            final ArrayList<ContentValues> list = Lists.newArrayList();
            final ContentValues values = new ContentValues();
            values.put(Photo.PHOTO, mProfile.photo);
            list.add(values);
            builder.appendPhotos(list);
        }

    }

    private void appendName(VCardBuilder builder) {
        final ArrayList<ContentValues> list = Lists.newArrayList();

        final ContentValues values = new ContentValues();
        
        String name = mProfile.getContentByKey(ProfileInfo.NAME);
        if (name != null) {
            values.put(StructuredName.DISPLAY_NAME, name);
            list.add(values);
            builder.appendNameProperties(list);
        }

    }

    private void appendPostal(VCardBuilder builder) {
        final ArrayList<ContentValues> list = Lists.newArrayList();

        final ContentValues values = new ContentValues();

        String address = mProfile.getContentByKey(ProfileInfo.ADDRESS);
        if (address != null) {
            values.put(StructuredPostal.FORMATTED_ADDRESS, address);
            values.put(StructuredPostal.TYPE, StructuredPostal.TYPE_HOME);
            values.put(StructuredPostal.LABEL, 
                    StructuredPostal.getTypeLabelResource(StructuredPostal.TYPE_HOME));
            list.add(values);
            builder.appendPostals(list);
        }
    }

    private void appendEvents(VCardBuilder builder) {

        final ArrayList<ContentValues> list = Lists.newArrayList();

        final ContentValues values = new ContentValues();
        String birth = ProfileInfo.getContentByKey(ProfileInfo.BIRTHDAY);
        if (birth != null) {
            values.put(Event.START_DATE, birth);
            values.put(Event.TYPE, Event.TYPE_BIRTHDAY);
            values.put(Event.LABEL, 
                    Event.getTypeResource(Event.TYPE_BIRTHDAY));
            list.add(values);
            builder.appendEvents(list);
        }
        
    }
    
    private void appendPhones(VCardBuilder builder) {
        
        ArrayList<ContentValues> list = new ArrayList<ContentValues>();
        String phoneNumber = mProfile.getContentByKey(ProfileInfo.PHONE_NUMBER);
        if (phoneNumber != null) {
            ContentValues value = new ContentValues();
            value.put(Phone.NUMBER, phoneNumber);
            value.put(Phone.TYPE, Phone.TYPE_MOBILE);
            value.put(Phone.LABEL, Phone.getTypeLabelResource(Phone.TYPE_MOBILE));
            list.add(value);
        }
        
        for(ProfileInfo.OtherNumberInfo info : mProfile.mOtherNumberArrayList) {
            ContentValues value = new ContentValues();
            int type = getVcardNumberType(info.type);
            value.put(Phone.NUMBER, info.number);
            value.put(Phone.TYPE, type);
            value.put(Phone.LABEL, Phone.getTypeLabelResource(type));
            list.add(value);
        }
        
        String companyTel = mProfile.getContentByKey(ProfileInfo.COMPANY_TEL);
        if (companyTel != null) {

            ContentValues value = new ContentValues();
            value.put(Phone.NUMBER, companyTel);
            value.put(Phone.TYPE, Phone.TYPE_COMPANY_MAIN);
            value.put(Phone.LABEL, Phone.getTypeLabelResource(Phone.TYPE_COMPANY_MAIN));
            list.add(value);
        }

        String companyFax = mProfile.getContentByKey(ProfileInfo.COMPANY_FAX);
        if (companyFax != null) {

            ContentValues value = new ContentValues();
            value.put(Phone.NUMBER, companyFax);
            value.put(Phone.TYPE, Phone.TYPE_FAX_WORK);
            value.put(Phone.LABEL, Phone.getTypeLabelResource(Phone.TYPE_FAX_WORK));
            list.add(value);
        }
        
        if (list.size() > 0) {
            builder.appendPhones(list, null);
        }
        
    }

    private void appendEmails(VCardBuilder builder) {
        
        ArrayList<ContentValues> list = new ArrayList<ContentValues>();
        ContentValues value = new ContentValues();
        
        String email = mProfile.getContentByKey(ProfileInfo.EMAIL);
        if (email != null) {
            value.put(Email.ADDRESS, email);
            value.put(Email.TYPE, Email.TYPE_WORK);
            list.add(value);
            builder.appendEmails(list);
        }
    }

    private void appendCompany(VCardBuilder builder) {
        
        ArrayList<ContentValues> list = new ArrayList<ContentValues>();

        // Append company name
        String company = mProfile.getContentByKey(ProfileInfo.COMPANY);
        if (company != null) {
            ContentValues value = new ContentValues();
            value.put(Organization.COMPANY, company);
            list.add(value);
        }

        // Append job title
        String title = mProfile.getContentByKey(ProfileInfo.TITLE);
        if (title != null) {
            ContentValues value = new ContentValues();
            value.put(Organization.TITLE, title);
            list.add(value);
        }

        // Append office address.
        String address = mProfile.getContentByKey(ProfileInfo.COMPANY_ADDR);
        if (address != null) {
            ContentValues value = new ContentValues();
            value.put(Organization.OFFICE_LOCATION, address);
            list.add(value);
        }
        
        if (list.size() > 0) {
            builder.appendOrganizations(list);
        }
    }
    
    private Uri writeToVcfFile(VCardBuilder builder) {
        Uri vCardUri = generateTempFileUri("profile");
        final OutputStream outputStream;
        Writer writer = null;
        try {
            outputStream = mContext.getContentResolver().openOutputStream(vCardUri);
        } catch (FileNotFoundException e) {
            Log.w(TAG, "FileNotFoundException thrown", e);
            return null;
        }
        try {
            writer = new BufferedWriter(new OutputStreamWriter(outputStream));
            writer.write(builder.toString());
            writer.flush();
        } catch (IOException e) {
            Log.w(TAG, "IOException thrown", e);
            return null;
        }
        return vCardUri;
    }
    
    private Uri generateTempFileUri(String prefix) {
        
        final File dir = mContext.getCacheDir();
        dir.mkdir();
        
        File tmpFile = new File(dir, prefix + "_vcard.vcf");
        Uri outputUri;
        outputUri = FileProvider.getUriForFile(mContext,
                PROFILE_AUTHORITY, new File(tmpFile.getAbsolutePath()));
        return outputUri;
    }

    private int getVcardNumberType(int type) {
        switch (type) {
            case ProfileInfo.VALUE_NUMBER_TYPE_HOME: 
                return Phone.TYPE_HOME;
            case ProfileInfo.VALUE_NUMBER_TYPE_WORK: 
                return Phone.TYPE_WORK;
            case ProfileInfo.VALUE_NUMBER_TYPE_OTHER: 
                return Phone.TYPE_OTHER;
            default: 
                return Phone.TYPE_OTHER;
        }
    }
    
    private final class VCardBuilderHandler extends Handler {
    
            public VCardBuilderHandler(Looper looper) {
                super(looper);
            }
            
            @Override
            public void handleMessage (Message msg) {
                if(null == msg)
                {
                    return;
                }
                switch (msg.what) {
                    case MSG_BUILD_PROFILE_VCARD:
                        VcardBuilderCallback callback = (VcardBuilderCallback)msg.obj;
                        handleBuildVCard(callback);
                        break;
                        
                     default:
                        break;
                }
            }
        }



    interface VcardBuilderCallback {
        
        void onVcardBuildDone(Uri uri);
        
    }
}
