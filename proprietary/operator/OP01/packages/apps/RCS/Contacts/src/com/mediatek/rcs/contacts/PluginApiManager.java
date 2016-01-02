/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
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

package com.mediatek.rcs.contacts;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;

import com.mediatek.rcs.contacts.ext.ContactExtention.OnPresenceChangedListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.gsma.joyn.capability.Capabilities;
import org.gsma.joyn.capability.CapabilitiesListener;
import org.gsma.joyn.capability.CapabilityService;
import org.gsma.joyn.Intents;
import org.gsma.joyn.JoynServiceConfiguration;
import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.JoynServiceListener;

/**
 * This class manages the APIs which are used by plug-in, providing a convenient
 * way for API invocations.
 */
public class PluginApiManager {
    public static final String TAG = "PluginApiManager";

    private static PluginApiManager sInstance = null;
    private boolean mNeedReconnectManagedAPi = false;
    private MyCapabilitiesListener mMyCapabilitiesListener = null;
    private CapabilityService mCapabilitiesApi = null;
    private boolean mRcsEnabled = false;
    private Context mContext = null;
    private static final int MAX_CACHE_SIZE = 2048;
    private final LruCache<String, ContactInformation> mContactsCache =
            new LruCache<String, ContactInformation>(MAX_CACHE_SIZE);
    private final LruCache<Long, List<String>> mCache =
            new LruCache<Long, List<String>>(MAX_CACHE_SIZE);
    private static final String CONTACT_CAPABILITIES =
            "com.orangelabs.rcs.capability.CONTACT_CAPABILITIES";
    private static final String INTENT_RCS_ON =
            "com.mediatek.intent.rcs.stack.LaunchService";
    private static final String INTENT_RCS_OFF =
            "com.mediatek.intent.rcs.stack.StopService";
    private final List<CapabilitiesChangeListener> mCapabilitiesChangeListenerList =
            new ArrayList<CapabilitiesChangeListener>();
    private final List<RegistrationListener> mRegistrationListeners =
            new CopyOnWriteArrayList<RegistrationListener>();
    private Cursor mCursor = null;
    private List<Long> mQueryOngoingList = new ArrayList<Long>();
    private final ConcurrentHashMap<Long, OnPresenceChangedListener> mPresenceListeners =
            new ConcurrentHashMap<Long, OnPresenceChangedListener>();
    /**
     * MIME type for RCS registration state.
     */
    private static final String MIMETYPE_REGISTRATION_STATE =
            "vnd.android.cursor.item/com.orangelabs.rcs.registration-state";
    /**
     * MIME type for RCSE capabilities.
     */
    private static final String MIMETYPE_RCSE_CAPABILITIES =
            "vnd.android.cursor.item/com.orangelabs.rcse.capabilities";
    private static final int RCS_CONTACT = 1;
    private static final int RCS_CAPABLE_CONTACT = 2;
    private static final int REGISTRATION_STATUS_ONLINE = 1;

    /**
     * The CapabilitiesChangeListener defined as a listener to notify the
     * specify observer that the capabilities has been changed.
     *
     * @see CapabilitiesChangeEvent
     */
    public interface CapabilitiesChangeListener {

        /**
         * On capabilities changed.
         *
         * @param contact the contact
         * @param contactInformation the contact information
         */
        void onCapabilitiesChanged(String contact,
                ContactInformation contactInformation);

        /**
         * Called when CapabilityApi connected status is changed.
         *
         * @param isConnected
         *            True if CapabilityApi is connected.
         */
        void onApiConnectedStatusChanged(boolean isConnected);
    }

    /**
     * Add presence changed listener.
     *
     * @param listener
     *            The presence changed listener.
     * @param contactId
     *            The contact id.
     */
    public void addOnPresenceChangedListener(
            OnPresenceChangedListener listener, long contactId) {
        mPresenceListeners.put(contactId, listener);
    }

    /**
     * Register the CapabilitiesChangeListener.
     *
     * @param listener            The CapabilitiesChangeListener used to register
     */
    public void addCapabilitiesChangeListener(
            CapabilitiesChangeListener listener) {
        Log.v(TAG, "addCapabilitiesChangeListener(), listener = " + listener);
        mCapabilitiesChangeListenerList.add(listener);
    }

    /**
     * Unregister the CapabilitiesChangeListener.
     *
     * @param listener            The CapabilitiesChangeListener used to unregister
     */
    public void removeCapabilitiesChangeListener(
            CapabilitiesChangeListener listener) {
        Log.v(TAG, "removeCapabilitiesChangeListener(), listener = "
                + listener);
        mCapabilitiesChangeListenerList.remove(listener);
    }

    /**
     * The RegistrationListener defined as a listener to notify the specify
     * observer that the registration status has been changed and
     * RegistrationAPi connected status.
     *
     * @see RegistrationEvent
     */
    public interface RegistrationListener {
        /**
         * Called when RegistrationApi connected status is changed.
         *
         * @param isConnected
         *            True if RegistrationApi is connected
         */
        void onApiConnectedStatusChanged(boolean isConnected);

        /**
         * Called when the status of RCS-e account is registered.
         *
         * @param status
         *            Current status of RCS-e account.
         */
        void onStatusChanged(boolean status);

        /**
         * Called when the rcse core service status has been changed.
         *
         * @param status
         *            Current status of rcse core service.
         */
        void onRcsCoreServiceStatusChanged(int status);

    }

    /**
     * Register the RegistrationListener.
     * @param listener            The RegistrationListener used to register
     */
    public void addRegistrationListener(RegistrationListener listener) {
        Log.v(TAG, "addRegistrationListener(), listener = " + listener);
        mRegistrationListeners.add(listener);
    }

    /**
     * Unregister the RegistrationListener.
     * @param listener            The RegistrationListener used to unregister
     */
    public void removeRegistrationListener(RegistrationListener listener) {
        Log.v(TAG, "removeRegistrationListener(), listener = "
                + listener);
        mRegistrationListeners.remove(listener);
    }

    /**
     * The class including some informations of contact: whether it is an Rcse
     * contact, the capabilities of IM, file transfer,CS call,image and video
     * share.
     */
    public static class ContactInformation {
        public int isRcsContact = 0; // 0 indicate not Rcs, 1 indicate Rcs
        public boolean isReadBurnSupported = false;
    }

    /**
     * Get the presence of number.
     *
     * @param snumber
     *            The number whose presence to be queried.
     * @return The presence of the number.
     */
    public int getContactPresence(String snumber) {
        Log.d(TAG, "getContactPresence(num), number:" + snumber);
        final String number = getAvailableNumber(snumber);
        ContactInformation info = null;
        synchronized (mContactsCache) {
            info = mContactsCache.get(number);
        }
        if (info != null) {
            Log.d(TAG, "getContactPresence(num), number:" + number
                    + " getisRcs:" + info.isRcsContact);
            return info.isRcsContact;
        } else {
            ContactInformation defaultInfo = new ContactInformation();
            synchronized (mContactsCache) {
                Log.d(TAG, "getContactPresence(num), setisRcs:0" + number);
                mContactsCache.put(number, defaultInfo);
            }
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    queryContactPresence(number);
                }
            });
        }
        return 0;
    }

    /**
     * Core service API connection.
     */
    private ServiceConnection mApiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
        }

        public void onServiceDisconnected(ComponentName className) {
        }
    };

    /**
     * Inits the data.
     */
    private void initData() {
        Log.d(TAG, "initData() entry");
        Thread thread = new Thread() {
            public void run() {
                Looper.prepare();
                registerObserver();
                queryContactsPresence();
                IntentFilter filter = new IntentFilter();
                filter.addAction(CONTACT_CAPABILITIES);
                filter.addAction(Intents.Client.SERVICE_UP);
                filter.addAction(INTENT_RCS_ON);
                filter.addAction(INTENT_RCS_OFF);
                mContext.registerReceiver(mBroadcastReceiver, filter);
            }
        };
        thread.start();
        Log.d(TAG, "initData() exit");
    }

    /**
     * Get presence of contact id.
     *
     * @param contactId
     *            The contact id whose presence to be queried.
     * @return The presence of the contact.
     */
    public int getContactPresence(final long contactId) {
        final List<String> numbers = mCache.get(contactId);
        Log.d(TAG, "getContactPresence(id), contactId: " + contactId
                + " numbers: " + numbers);
        if (numbers != null) {
            synchronized (mPresenceListeners) {
                mPresenceListeners.remove(contactId);
            }
            ContactInformation info = null;
            for (String number : numbers) {
                number = getAvailableNumber(number);
                info = mContactsCache.get(number);
                if (info != null) {
                    Log.d(TAG, "getisRCS:" + info.isRcsContact);
                    if (info.isRcsContact == 1) {
                        return 1;
                    }
                }
            }
            if (mQueryOngoingList.contains(contactId)) {
                Log.d(TAG, "getContactPresence(id) contact id " + contactId
                        + " query presence operation is ongoing");
                return 0;
            }
            mQueryOngoingList.add(contactId);
            if (info == null) {
                Log.d(TAG,
                        "getContactPresence(id) info is null, so retry to query");
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        queryPresence(contactId, numbers);
                        mQueryOngoingList.remove(contactId);
                    }
                });
            }
        } else {
            if (mQueryOngoingList.contains(contactId)) {
                Log.d(TAG, "getContactPresence(id) contact id " + contactId
                        + " query presence operation is ongoing");
                return 0;
            }
            mQueryOngoingList.add(contactId);
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    final List<String> list = getNumbersByContactId(contactId);
                    queryPresence(contactId, list);
                    mQueryOngoingList.remove(contactId);
                }
            });
        }
        return 0;
    }

    /**
     * Register observer.
     */
    private void registerObserver() {
        Log.d(TAG, "registerObserver entry");
        if (mCursor != null && mCursor.isClosed()) {
            Log.d(TAG, "registerObserver close cursor");
            mCursor.close();
        }
        // Query contactContracts phone database
        mCursor = mContext.getContentResolver().query(Phone.CONTENT_URI, null,
                null, null, null);
        if (mCursor != null) {
            // Register content observer
            Log.d(TAG, "registerObserver begin to registerContentObserver");
            mCursor.registerContentObserver(new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    Log.d(TAG, "onChange entry" + selfChange);
                    if (mCache == null) {
                        Log.d(TAG, "onChange mCache is null");
                        return;
                    }
                    Map<Long, List<String>> map = mCache.snapshot();
                    if (map != null) {
                        Set<Long> keys = map.keySet();
                        for (Long key : keys) {
                            getNumbersByContactId(key);
                        }
                    } else {
                        Log.d(TAG, "onChange map is null");
                    }
                }
            });
        } else {
            Log.d(TAG, "registerObserver mCursor is null");
        }
        Log.d(TAG, "registerObserver exit");
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            String action = intent.getAction();
            if (CONTACT_CAPABILITIES.equals(action)) {

                String number = intent.getStringExtra("contact");
                Capabilities capabilities = intent
                        .getParcelableExtra("capabilities");
                ContactInformation info = new ContactInformation();
                if (capabilities == null) {
                    Log.d(TAG, "onReceive(),capabilities is null");
                    return;
                }
                info.isRcsContact = capabilities.isSupportedRcseContact() ? 1
                        : 0;
                info.isReadBurnSupported = capabilities.isBurnAfterRead();
                number = getAvailableNumber(number);
                synchronized (mContactsCache) {
                    Log.d(TAG, "onReceive(), setisRcs:" + info.isRcsContact + number);
                    mContactsCache.put(number, info);
                }
                for (CapabilitiesChangeListener listener : mCapabilitiesChangeListenerList) {
                    if (listener != null) {
                        listener.onCapabilitiesChanged(number, info);
                        return;
                    }
                }
            } else if (Intents.Client.SERVICE_UP.equals(action)) {
                Log.d(TAG, "onreceive(), rcscoreservice action");
                mCapabilitiesApi = new CapabilityService(mContext,
                        new MyJoynServiceListener());
                mCapabilitiesApi.connect();
            } else if (INTENT_RCS_ON.equals(action)) {
                mRcsEnabled = true;
                Log.d(TAG, "onreceive(), rcs on");
            } else if (INTENT_RCS_OFF.equals(action)) {
                mRcsEnabled = false;
                Log.d(TAG, "onreceive(), rcs off");
            }
        }
    };

    /**
     * Query contacts presence.
     */
    private void queryContactsPresence() {
        Log.d(TAG, "queryContactsPresence() entry");
        List<String> availableNumbers = new ArrayList<String>();
        String[] projection = {Data.DATA1, Data.DATA2, Data.MIMETYPE};
        String selection = Data.MIMETYPE + "=?";

        // Get all registration state entries
        String[] selectionArgs = { MIMETYPE_REGISTRATION_STATE };
        Cursor c = mContext.getContentResolver().query(Data.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null);
        if (c != null) {
            while (c.moveToNext()) {
                String number = c.getString(0);
                int registrationState = c.getInt(1);
                if (registrationState == REGISTRATION_STATUS_ONLINE) {
                    // If the registration status is online, add the number to the list
                    Log.d(TAG, "queryContactsPresence(): " + number);
                    ContactInformation info = new ContactInformation();
                    info.isRcsContact = 1;
                    info.isReadBurnSupported = false;
                    number = getAvailableNumber(number);
                    synchronized (mContactsCache) {
                        Log.d(TAG, "queryContactsPresence(), setisRcs:1" + number);
                        mContactsCache.put(number, info);
                        info = mContactsCache.get(number);
                        Log.d(TAG, "queryContactsPresence(), getisRcs:" + info.isRcsContact);
                    }
                }
            }
            c.close();
        }
    }

    /**
     * Query contacts presence.
     */
    private String getAvailableNumber(String number) {
        int size = number.length();
        Log.d(TAG, "getAvailableNumber(), " + size);
        //if (size < 11) {
        //    return null;
        //}
        if (size > 11) {
            number = number.substring(number.length() - 11);
        }

        return number;
    }

    /**
     * Query contact presence.
     *
     * @param snumber the number
     */
    private void queryContactPresence(String snumber) {
        Log.d(TAG, "queryContactPresence(num), number:" + snumber);
        final String number = getAvailableNumber(snumber);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "queryContactPresence(num), in async task");
                if (mCapabilitiesApi == null) {
                    Log.d(TAG,
                            "queryContactPresence(num), mCapabilitiesApi is null, number:"
                                    + number);
                } else {
                    try {
                        boolean gotValue = true;
                        Capabilities capabilities = null;
                        capabilities = mCapabilitiesApi
                                .getContactCapabilities(number);
                        mCapabilitiesApi.requestContactCapabilities(number);
                        ContactInformation info = new ContactInformation();
                        if (capabilities == null) {
                            capabilities = new Capabilities(false, false,
                                    false, false, false, false, false, null,
                                    false, false, false, false, false, false);
                            gotValue = false;
                            Log.d(TAG, "queryContactPresence(num), get null");
                        }
                        info.isRcsContact = capabilities
                                .isSupportedRcseContact() ? 1 : 0;
                        info.isReadBurnSupported = capabilities
                                .isBurnAfterRead();
                        synchronized (mContactsCache) {
                            if (gotValue || mContactsCache.get(number) == null) {
                                Log.d(TAG, "queryContactPresence(num), setisRcs:"
                                    + info.isRcsContact + number);
                                mContactsCache.put(number, info);
                            }
                        }
                    } catch (JoynServiceException e) {
                        e.printStackTrace();
                    }
                }
                Log.d(TAG, "queryContactPresence(num) leave async task");
            }
        });
        Log.d(TAG, "queryContactPresence(num) exit");
    }

    /**
     * Query a series of phone number.
     *
     * @param numbers            The phone numbers list need to query
     */
    public void queryNumbersPresence(List<String> numbers) {
        Log.d(TAG, "queryNumbersPresence entry, numbers: " + numbers
                + ", mCapabilitiesApi= " + mCapabilitiesApi);
        if (mCapabilitiesApi != null) {
            for (String number : numbers) {
                Log.d(TAG, "queryNumbersPresence number: " + number);
                number = getAvailableNumber(number);
                ContactInformation info = new ContactInformation();
                try {
                    boolean gotValue = true;
                    Capabilities capabilities = null;
                    capabilities = mCapabilitiesApi
                            .getContactCapabilities(number);
                    mCapabilitiesApi.requestContactCapabilities(number);
                    if (capabilities == null) {
                        capabilities = new Capabilities(false, false, false,
                                false, false, false, false, null, false, false,
                                false, false, false, false);
                        gotValue = false;
                        Log.d(TAG, "queryNumbersPresence(), get null");
                    }
                    info.isRcsContact = capabilities.isSupportedRcseContact() ? 1
                            : 0;
                    info.isReadBurnSupported = capabilities.isBurnAfterRead();
                    synchronized (mContactsCache) {
                        if (gotValue || mContactsCache.get(number) == null) {
                            Log.d(TAG, "queryNumbersPresence(), setisRcs:"
                                    + info.isRcsContact + number);
                            mContactsCache.put(number, info);
                        }
                    }
                } catch (JoynServiceException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Query a series of phone number.
     *
     * @param contactId            The contact id
     * @param numbers            The phone numbers list need to query
     */
    private void queryPresence(long contactId, List<String> numbers) {
        Log.d(TAG, "queryPresence() entry, contactId: " + contactId
                + " numbers: " + numbers + " mCapabilitiesApi: "
                + mCapabilitiesApi);
        if (mCapabilitiesApi != null) {
            boolean needNotify = false;
            for (String number : numbers) {
                number = getAvailableNumber(number);
                boolean gotValue = true;
                ContactInformation info = new ContactInformation();
                Capabilities capabilities = null;
                ContactInformation cachedInfo = mContactsCache.get(number);
                if (cachedInfo == null) {
                    try {
                        capabilities = mCapabilitiesApi
                                .getContactCapabilities(number);
                    } catch (JoynServiceException e) {
                        e.printStackTrace();
                    }
                } else {
                    if (cachedInfo.isRcsContact == 1) {
                        needNotify = true;
                    }
                    continue;
                }
                if (capabilities == null) {
                    capabilities = new Capabilities(false, false, false, false,
                            false, false, false, null, false, false, false,
                            false, false, false);
                    gotValue = false;
                    Log.d(TAG, "queryPresence(), get null");
                }
                info.isRcsContact = capabilities.isSupportedRcseContact() ? 1
                        : 0;
                info.isReadBurnSupported = capabilities.isBurnAfterRead();
                synchronized (mContactsCache) {
                    Log.d(TAG, "queryPresence(), info:"
                                + mContactsCache.get(number));
                    if (mContactsCache.get(number) != null) {
                        Log.d(TAG, "queryPresence(), getisRcs:"
                                + mContactsCache.get(number).isRcsContact);
                    }
                    if (gotValue) {
                        Log.d(TAG, "queryPresence(), setisRcs:"
                            + info.isRcsContact + number);
                        mContactsCache.put(number, info);
                    }
                }
                if (info.isRcsContact == 1) {
                    needNotify = true;
                }
            }
            synchronized (mPresenceListeners) {
                if (needNotify) {
                    OnPresenceChangedListener listener = mPresenceListeners
                            .get(contactId);
                    if (listener != null) {
                        listener.onPresenceChanged(contactId, 1);
                    }
                }
                mPresenceListeners.remove(contactId);
            }
            Log.d(TAG, "queryPresence() contactId: " + contactId
                    + " needNotify: " + needNotify);
        }
    }

    /**
     * Obtain the phone numbers from a specific contact id.
     *
     * @param contactId            The contact id
     * @return The phone numbers of the contact id
     */
    public List<String> getNumbersByContactId(long contactId) {
        Log.d(TAG, "getNumbersByContactId() entry, contact id is: "
                + contactId);
        List<String> list = new ArrayList<String>();
        String[] projection = { Phone.NUMBER };
        String selection = Phone.CONTACT_ID + "=? ";
        String[] selectionArgs = { Long.toString(contactId) };
        Cursor cur = mContext.getContentResolver().query(Phone.CONTENT_URI,
                projection, selection, selectionArgs, null);
        try {
            if (cur != null) {
                while (cur.moveToNext()) {
                    String number = cur.getString(0);
                    if (!TextUtils.isEmpty(number)) {
                        list.add(number.replace(" ", ""));
                    } else {
                        Log.w(TAG,
                                "getNumbersByContactId() invalid number: "
                                        + number);
                    }
                }
            }
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
        mCache.put(contactId, list);
        Log.d(TAG, "getNumbersByContactId() exit, list: " + list);
        return list;
    }

    /**
     * Return whether burn after read is supported.
     *
     * @param number
     *            The number whose capability is to be queried.
     * @return True if burn after read is supported, else false.
     */
    public boolean isReadBurnSupported(String number) {
        Log.d(TAG, "isReadBurnSupported(num) entry, with number: " + number);
        if (number == null) {
            Log.w(TAG, "number is null");
            return false;
        }
        number = getAvailableNumber(number);
        ContactInformation info = mContactsCache.get(number);
        if (info != null) {
            Log.d(TAG, "isReadBurnSupported(num), isReadburn" + info.isReadBurnSupported);
            return info.isReadBurnSupported;
        } else {
            Log.d(TAG, "isReadBurnSupported(num) info is null");
            queryContactPresence(number);
            return false;
        }
    }

    /**
     * Return whether burn after read is supported.
     *
     * @param contactId
     *            The contactId whose capability is to be queried.
     * @return True if burn after read is supported, else false.
     */
    public boolean isReadBurnSupported(final long contactId) {
        Log.d(TAG, "isReadBurnSupported(id) entry, with contact id: " + contactId);
        final List<String> numbers = mCache.get(contactId);
        if (numbers != null) {
            for (String number : numbers) {
                boolean isReadBurnSupported = isReadBurnSupported(number);
                if (isReadBurnSupported) {
                    Log.d(TAG, "isReadBurnSupported(id) exit with true");
                    return true;
                }
            }
            Log.d(TAG, "isReadBurnSupported(id) exit with false");
            return false;
        } else {
            Log.d(TAG, "isReadBurnSupported(id) numbers is null, exit with false");
            return false;
        }
    }

    /**
     * Check whether a number is a rcse account.
     *
     * @param number            The number to query
     * @return True if number is a rcse account, otherwise return false.
     */
    public boolean isRcseContact(String number) {
        Log.d(TAG, "sharing isRcseContact() entry, with number: " + number);
        if (number == null) {
            Log.w(TAG, "sharing number is null");
            return false;
        }
        number = getAvailableNumber(number);
        ContactInformation info = mContactsCache.get(number);
        if (info != null) {
            Log.d(TAG, "isRcseContact(), getisRcs:" + info.isRcsContact);
            return info.isRcsContact == 1;
        } else {
            Log.d(TAG, "sharing ContactInformation info is null");
            queryContactPresence(number);
            return false;
        }
    }

    /**
     * This method should only be called from ApiService, for APIs
     * initialization.
     *
     * @param context
     *            The Context of this application.
     * @return true If initialize successfully, otherwise false.
     */
    public static synchronized boolean initialize(Context context) {
        Log.v(TAG, "initialize() entry");
        if (null != sInstance) {
            Log.w(
                    TAG,
                    "initialize() sInstance has existed, " +
                    "is it really the first time you call this method?");
            return true;
        } else {
            if (null != context) {
                PluginApiManager apiManager = new PluginApiManager(context);
                sInstance = apiManager;
                return true;
            } else {
                Log.e(TAG, "initialize() the context is null");
                return false;
            }
        }
    }

    /**
     * Get the context.
     *
     * @return Context
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * Get the instance of PluginApiManager.
     *
     * @return The instance of ApiManager, or null if the instance has not been
     *         initialized.
     */
    public static PluginApiManager getInstance() {
        if (null == sInstance) {
            throw new RuntimeException(
                    "Please call initialize() before calling this method");
        }
        return sInstance;
    }

    /**
     * Instantiates a new plugin api manager.
     *
     * @param context the context
     */
    private PluginApiManager(Context context) {
        Log.v(TAG, "PluginApiManager(), context = " + context);
        mContext = context;
        mMyCapabilitiesListener = new MyCapabilitiesListener();
        MyJoynServiceListener myJoynServiceListener = new MyJoynServiceListener();
        mCapabilitiesApi = new CapabilityService(context, myJoynServiceListener);
        mCapabilitiesApi.connect();
        mRcsEnabled = JoynServiceConfiguration.isServiceActivated(mContext);
        initData();
    }

    /**
     * The listener interface for receiving myJoynService events.
     * The class that is interested in processing a myJoynService
     * event implements this interface, and the object created
     * with that class is registered with a component using the
     * component's addMyJoynServiceListener method. When
     * the myJoynService event occurs, that object's appropriate
     * method is invoked.
     *
     * @see MyJoynServiceEvent
     */
    public class MyJoynServiceListener implements JoynServiceListener {

        /**
         * On service connected.
         */
        @Override
        public void onServiceConnected() {
            try {
                Log.d(TAG, "onServiceConnected");
                PluginApiManager.this.mCapabilitiesApi
                        .addCapabilitiesListener(mMyCapabilitiesListener);
            } catch (JoynServiceException e) {
                e.printStackTrace();
            }

        }

        /**
         * On service disconnected.
         *
         * @param error the error
         */
        @Override
        public void onServiceDisconnected(int error) {
            try {
                Log.d(TAG, "onServiceDisConnected");
                if (PluginApiManager.this.mCapabilitiesApi != null) {
                    PluginApiManager.this.mCapabilitiesApi
                            .removeCapabilitiesListener(mMyCapabilitiesListener);
                    PluginApiManager.this.mCapabilitiesApi = null;
                }
            } catch (JoynServiceException e) {
                e.printStackTrace();
            }

        }

    }

    /**
     * The listener interface for receiving myCapabilities events.
     * The class that is interested in processing a myCapabilities
     * event implements this interface, and the object created
     * with that class is registered with a component using the
     * component's addMyCapabilitiesListener method. When
     * the myCapabilities event occurs, that object's appropriate
     * method is invoked.
     *
     * @see MyCapabilitiesEvent
     */
    public class MyCapabilitiesListener extends CapabilitiesListener {

        /**
         * On capabilities received.
         *
         * @param contact the contact
         * @param capabilities the capabilities
         */
        @Override
        public void onCapabilitiesReceived(final String contact,
                Capabilities capabilities) {

            Log.w(TAG, "onCapabilitiesReceived(), contact = " + contact
                    + ", capabilities = " + capabilities + ", mContactsCache= "
                    + mContactsCache);
            if (null != contact && capabilities != null) {
                Log.v(TAG, "Remove from cache");
                ContactInformation info = mContactsCache.remove(contact);
                Log.v(TAG, "after remove from cache");
                if (info == null) {
                    Log.v(TAG, "cache does not exist, so create a object.");
                    info = new ContactInformation();
                }
                info.isRcsContact = capabilities.isSupportedRcseContact() ? 1
                        : 0;
                Log.v(TAG, "Options  is RCS Contact:" + info.isRcsContact);
                if (capabilities.isSupportedRcseContact()) {
                    Log.w(TAG, "Options It is RCS Contact");
                }
                info.isReadBurnSupported = capabilities.isBurnAfterRead();
                Log.d(TAG, "onCapabilitiesReceived() setisRcs:" + info.isRcsContact);
                String number = contact;
                number = getAvailableNumber(number);
                mContactsCache.put(number, info);
                Log.w(TAG, "put capability into cache");
                for (CapabilitiesChangeListener listener : mCapabilitiesChangeListenerList) {
                    if (listener != null) {
                        Log.w(TAG, "Notify the listener");
                        listener.onCapabilitiesChanged(contact, info);
                    }
                }
            } else {
                Log.d(TAG,
                        "onCapabilitiesReceived()-invalid contact or capabilities");
            }
        }
    }

    /**
     * Get the registration status.
     *
     * @return Registration status
    */
    public boolean getRegistrationStatus() {
        return mRcsEnabled;
    }

    /**
     * This constructor is just used for test case.
     */
    public PluginApiManager() {

    }

    /**
     * Clear all the information in the mContactsCache.
     */
    public void cleanContactCache() {
        Log.d(TAG, "cleanContactCache() entry");
        mContactsCache.evictAll();
    }

    /**
     * Sets the managed api status.
     *
     * @param needReconnect            Indicate whether need to reconnect API
     */
    public void setManagedApiStatus(boolean needReconnect) {
        mNeedReconnectManagedAPi = true;
    }

    /**
     * Gets the managed api status.
     *
     * @return True if need to reconnect API, otherwise return false
     */
    public boolean getManagedApiStatus() {
        return mNeedReconnectManagedAPi;
    }

    /**
     * Reconnect ManagedCapabilityApi.
     */
    public void reConnectManagedApi() {
        Log.d(TAG, "reConnectManagedApi():");
        mNeedReconnectManagedAPi = false;
        mCapabilitiesApi.connect();
    }

}
