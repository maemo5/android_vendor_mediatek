package com.mediatek.rcs.message.ui;

import android.app.ActionBar;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import com.mediatek.rcs.message.R;

public class RcsSettingsActivity extends PreferenceActivity {

    private static final String TAG = "RcsSettingsActivity";
    private static final String SEND_ORG_PIC = "pref_key_send_org_pic";
    private static final String SEND_MSG_MODE = "pref_key_send_mode";
    private static final String WLAN_BACKUP = "pref_key_wlan_backup";
    private static final String RCS_PREFERENCE = "com.mediatek.rcs.message_preferences";
    CheckBoxPreference mSendOrgPic;
    CheckBoxPreference mSendMode;
    CheckBoxPreference mWlanBackup;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActionBar();
        actionBar.setTitle(getResources().getString(R.string.rcs_setting));
        actionBar.setDisplayHomeAsUpEnabled(true);
        setMessagePreferences();
    }

    private void setMessagePreferences() {
        addPreferencesFromResource(R.xml.rcspreferences);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        mSendOrgPic = (CheckBoxPreference) findPreference(SEND_ORG_PIC);
        if (mSendOrgPic != null) {
            mSendOrgPic.setChecked(sp.getBoolean(mSendOrgPic.getKey(), false));
        }
        mSendMode = (CheckBoxPreference) findPreference(SEND_MSG_MODE);
        if( mSendMode != null) {
            mSendMode.setChecked(sp.getBoolean(mSendMode.getKey(), true));
        }
        mWlanBackup = (CheckBoxPreference) findPreference(WLAN_BACKUP);
        if( mWlanBackup != null) {
            mWlanBackup.setChecked(sp.getBoolean(mWlanBackup.getKey(), true));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        default:
            break;
        }
        return false;
    }

    
    public static boolean getSendMSGStatus(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        boolean sendOrgPic = sp.getBoolean(SEND_ORG_PIC, true);
        Log.d(TAG, "getSendMSGStatus : sendOrgPic = "+ sendOrgPic);
        return sendOrgPic;
    }
    
    public static boolean getSendMSGMode(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        boolean sendMode = sp.getBoolean(SEND_MSG_MODE, true);
        Log.d(TAG, "getSendMSGMode : sendMode = "+ sendMode);
        return sendMode;
    }
    
    /**
     * 
     * @param c
     * @return
     */
    public static boolean getWlanBackup(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        boolean wlanBackup = sp.getBoolean(WLAN_BACKUP, true);
        Log.d(TAG, "getWlanBackup : wlanBackup = "+ wlanBackup);
        return wlanBackup;
    }
    
//    public static boolean getAutoAcceptGroupChatInvitation(Context c) {
//        SharedPreferences sp = c.getSharedPreferences(RCS_PREFERENCE, MODE_WORLD_READABLE);
//        boolean autoAccept = sp.getBoolean("pref_key_auto_accept_group_invitate", false);
//        Log.d(TAG, "getAutoAcceptGroupChatInvitation : autoAccept = "+ autoAccept);
//        return autoAccept;
//    }
}
