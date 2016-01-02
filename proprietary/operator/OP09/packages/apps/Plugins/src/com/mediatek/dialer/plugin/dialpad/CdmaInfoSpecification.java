package com.mediatek.dialer.plugin.dialpad;

import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.cdma.CDMALTEPhone;
import com.android.internal.telephony.cdma.CDMAPhone;

import com.mediatek.op09.plugin.R;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * implement display for version info plugin for op09.
 */
public class CdmaInfoSpecification extends PreferenceActivity {

    private static final String TAG = "CdmaInfoSpecification";

    private static final String FILENAME_MSV = "/sys/board_properties/soc/msv";

    private static final String KEY_PRODUCT_MODEL = "product_model";
    private static final String KEY_HARDWARE_VERSION = "hardware_version";
    private static final String KEY_SOFTWARE_VERSION = "software_version";
    public static final String KEY_CDMA_INFO = "cdma_info";
    public static final String KEY_PRL_VERSION = "prl_version";
    public static final String KEY_SID = "sid";
    public static final String KEY_NID = "nid";
    public static final String KEY_MEID = "meid";
    public static final String KEY_UIM_ID = "uim_id";
    public static final String KEY_SUB_ID = "subid";

    private Phone mCdmaPhone;
    private boolean mIsCdma = true;
    private boolean mMeidValid = false;
    private String mEsn;
    private String mMeid;
    private String mUimId;
    private String mSid;
    private String mNid;
    private String mPrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPRLVersionSetting(getIntent().getIntExtra(
                    KEY_SUB_ID, SubscriptionManager.INVALID_SUBSCRIPTION_ID));
        addPreferencesFromResource(R.xml.cdma_info_specifications);
        setPhoneValuesToPreferences();
        setCDMAValuesToPreference(getIntent().getIntExtra(
                    KEY_SUB_ID, SubscriptionManager.INVALID_SUBSCRIPTION_ID));
    }

    /**
     * set the info from phone to preference.
     *
     * @return void.
     */
    private void setPhoneValuesToPreferences() {
        log("setPhoneValuesToPreferences()");
        PreferenceScreen parent = (PreferenceScreen) getPreferenceScreen();
        Preference preference = parent.findPreference(KEY_PRODUCT_MODEL);
        if (null != preference) {
            preference.setSummary(Build.MODEL + getMsvSuffix());
        }
        preference = parent.findPreference(KEY_HARDWARE_VERSION);
        if (null != preference) {
            preference.setSummary(Build.HARDWARE);
        }
        preference = parent.findPreference(KEY_SOFTWARE_VERSION);
        if (null != preference) {
            preference.setSummary(Build.DISPLAY);
        }
    }

    /**
     * set the info from cdma phone to preference.
     *
     * @param slot indicator which slot is cdma phone or invalid.
     * @return void.
     */
    private void setCDMAValuesToPreference(int subId) {
        log("setCDMAValuesToPreference(), subId = " + subId + " mIsCdma " + mIsCdma);
        if (!SubscriptionManager.isValidSubscriptionId(subId)
                    || !mIsCdma) {
            Preference preference = findPreference(KEY_CDMA_INFO);
            if (null != preference) {
                preference.setEnabled(false);
                preference.setShouldDisableView(true);
            }
            preference = findPreference(KEY_PRL_VERSION);
            if (null != preference) {
                preference.setEnabled(false);
                preference.setShouldDisableView(true);
            }
            preference = findPreference(KEY_SID);
            if (null != preference) {
                preference.setEnabled(false);
                preference.setShouldDisableView(true);
            }
            preference = findPreference(KEY_NID);
            if (null != preference) {
                preference.setEnabled(false);
                preference.setShouldDisableView(true);
            }
            preference = findPreference(KEY_MEID);
            if (null != preference) {
                preference.setEnabled(false);
                preference.setShouldDisableView(true);
            }
            preference = findPreference(KEY_UIM_ID);
            if (null != preference) {
                preference.setEnabled(false);
                preference.setShouldDisableView(true);
            }
            return;
        }

        Preference preference = findPreference(KEY_PRL_VERSION);
        if (null != preference) {
            preference.setSummary(mPrl);
        }
        preference = findPreference(KEY_SID);
        if (null != preference) {
            preference.setSummary(mSid);
        }
        preference = findPreference(KEY_NID);
        if (null != preference) {
            preference.setSummary(mNid);
        }
        preference = findPreference(KEY_MEID);
        if (null != preference) {
            if (mMeidValid) {
                preference.setTitle(getString(R.string.current_meid));
                preference.setSummary(mMeid);
            } else {
                preference.setTitle(getString(R.string.current_esn));
                preference.setSummary(mEsn);
            }
        }
        preference = findPreference(KEY_UIM_ID);
        if (null != preference) {
            preference.setSummary(mUimId);
        }
    }

    /**
     * Returns " (ENGINEERING)" if the msv file has a zero value, else returns "".
     *
     * @return a string to append to the model number description.
     */
    private String getMsvSuffix() {
        // Production devices should have a non-zero value. If we can't read it, assume it's a
        // production device so that we don't accidentally show that it's an ENGINEERING device.
        try {
            String msv = readLine(FILENAME_MSV);
            // Parse as a hex number. If it evaluates to a zero, then it's an engineering build.
            if (Long.parseLong(msv, 16) == 0) {
                return " (ENGINEERING)";
            }
        } catch (IOException ioe) {
            // Fail quietly, as the file may not exist on some devices.
        } catch (NumberFormatException nfe) {
            // Fail quietly, returning empty string should be sufficient
        }
        return "";
    }

    /**
     * Reads a line from the specified file.
     *
     * @param filename the file to read from.
     * @return the first line, if any.
     * @throws IOException if the file couldn't be read.
     */
    private String readLine(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename), 256);
        try {
            return reader.readLine();
        } finally {
            reader.close();
        }
    }

    /**
     * show version by cdma phone provider info.
     *
     * @param context from host app.
     * @param slot indicator which slot is cdma phone.
     * @return void.
     */
    private void getPRLVersionSetting(int subId) {
        mCdmaPhone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(subId));
        log("getPRLVersionSetting mCdmaPhone" + mCdmaPhone);
        if (null == mCdmaPhone || SubscriptionManager.INVALID_SUBSCRIPTION_ID == subId) {
            log("subId = INVALID_SUBSCRIPTION_ID, just return");
            mIsCdma = false;
            return;
        }

        PhoneProxy cdmaPhoneProxy = (PhoneProxy) mCdmaPhone;
        if (!(cdmaPhoneProxy.getActivePhone() instanceof CDMAPhone)
                    && !(cdmaPhoneProxy.getActivePhone() instanceof CDMALTEPhone)) {
            log("active phone intance type is not CDMAPhone, just return");
            mIsCdma = false;
            return;
        }

        if (mIsCdma) {
            CDMAPhone cdmaPhone = (CDMAPhone) cdmaPhoneProxy.getActivePhone();
            if (cdmaPhone.isMeidValid()) {
                mMeidValid = true;
                mMeid = cdmaPhone.getMeid();
            } else {
                mEsn = cdmaPhone.getEsn();
            }
            mUimId = cdmaPhone.getImei().toUpperCase();
            if (mUimId != null) {
                 log("getUimid().toUpperCase() =" + mUimId);
             }
            mPrl = cdmaPhone.getPrl();
            mSid = cdmaPhone.getSid();
            mNid = cdmaPhone.getNid();
        }
    }

    /**
     * simple log info.
     *
     * @param msg need print out string.
     * @return void.
     */
    private static void log(String msg) {
        Log.d(TAG, msg);
    }

}
