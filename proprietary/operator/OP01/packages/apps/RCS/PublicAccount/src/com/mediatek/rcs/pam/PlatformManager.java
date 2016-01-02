package com.mediatek.rcs.pam;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.rcs.pam.client.PAMClient.Transmitter;
import com.mediatek.rcs.pam.connectivity.HttpDigestTransmitter;

import org.gsma.joyn.JoynServiceConfiguration;

import java.net.URI;
import java.net.URISyntaxException;

public class PlatformManager {
    public static PlatformManager sInstance;

    private static final String SERVER_URL = "http://122.70.137.46:8088/interface/index.php";
    private static final String SERVER2_URL = "http://122.70.137.46:8188";
    private static final String NAF_URL = "http://122.70.137.46:8088/interface/index.php";
    // FIXME for test only
    private static final String TEST_SIP_DOMAIN = "@bj.ims.mnc460.mcc000.3gppnetwork.org";

    private static final String TAG = Constants.TAG_PREFIX + "PlatformManager";

    protected Transmitter mTransmitter;

    private final JoynServiceConfiguration mJoynConfig;

    protected PlatformManager() {
        mJoynConfig = new JoynServiceConfiguration();
    }

    public static synchronized PlatformManager getInstance() {
        if (sInstance == null) {
            sInstance = new PlatformManager();
        }
        return sInstance;
    }

    public synchronized Transmitter getTransmitter(Context context) {
        if (mTransmitter == null) {
            mTransmitter = new HttpDigestTransmitter(context, getServerUrl(context), "12345678", null);
            // mTransmitter = new GbaTransmitter(context, getServerUrl(context), NAF_URL);
            // mTransmitter = new HttpTransmitter(context, getServerUrl(context));
        }
        return mTransmitter;
    }

    public String getUserId(Context context) {
        String publicUri = mJoynConfig.getPublicUri(context);
        Log.d(TAG, "getUserId: publicUri = " + publicUri);
        String uuid = Utils.extractUuidFromSipUri(mJoynConfig.getPublicUri(context));
        if (uuid == null) {
            return null;
        }
        return Constants.SIP_PREFIX + Utils.extractNumberFromUuid(uuid) + TEST_SIP_DOMAIN;
    }

    public String getIdentity(Context context) {
        String phoneNumber = Utils.extractNumberFromUuid(Utils.extractUuidFromSipUri(mJoynConfig.getPublicUri(context)));
//        return mJoynConfig.getPublicUri(context);
        if (phoneNumber == null) {
            return null;
        }
        return Constants.TEL_PREFIX + phoneNumber;
    }
    
    public String getServerUrl(Context context) {
        return SERVER2_URL;
//        URI uri = URI.create(getPublicAccountServerAddress(context));
//        String port = getPublicAccountServerAddressPort(context);
//        if (!TextUtils.isEmpty(port)) {
//            try {
//                uri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), Integer.parseInt(port), uri.getPath(), uri.getQuery(), uri.getFragment());
//            } catch (NumberFormatException e) {
//                e.printStackTrace();
//                Log.e(TAG, "Invalid port config from server: '" + port + "', use address directly");
//            } catch (URISyntaxException e) {
//                e.printStackTrace();
//                Log.e(TAG, "Invalid address config from server: '" + uri + "', use default address");
//                return SERVER_URL;
//            }
//            return uri.toString();
//        } else {
//            return SERVER_URL;
//        }
    }
    
    public String getNafUrl(Context context) {
        return getServerUrl(context);
    }
    
    public String getPublicAccountServerAddress(Context context) {
        String result = mJoynConfig.getPublicAccountAddress(context);
        Log.d(TAG, "PA Address from Joyn Config: " + result);
        return result;
    }
    
    public String getPublicAccountServerAddressPort(Context context) {
        String result = mJoynConfig.getPublicAccountAddressPort(context);
        Log.d(TAG, "PA Port from Joyn Config: " + result);
        return result;
    }
    
    public String getPublicAccountServerAddressType(Context context) {
        String result = mJoynConfig.getPublicAccountAddressType(context);
        Log.d(TAG, "PA Type from Joyn Config: " + result);
        return result;
    }
    
    public boolean getPublicAccountServerAUTH(Context context) {
        return mJoynConfig.getPublicAccountAUTH(context);
    }
    
    public boolean supportCcs() {
        return false;
    }
    
    public boolean isRcsServiceActivated(Context context) {
        return JoynServiceConfiguration.isServiceActivated(context);
    }
}
