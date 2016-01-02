package com.mediatek.mmsdk;

import android.content.Context;
import android.hardware.camera2.utils.BinderHolder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import com.mediatek.mmsdk.CameraMmEffect;
import com.mediatek.mmsdk.CameraMmEffectImpl;
import com.mediatek.mmsdk.CameraEffectHalRuntimeException;
import com.mediatek.mmsdk.EffectHalVersion;
import com.mediatek.mmsdk.CameraEffectHalException;
import com.mediatek.mmsdk.IEffectFactory;
import com.mediatek.mmsdk.IEffectHal;
import com.mediatek.mmsdk.IFeatureManager;
import com.mediatek.mmsdk.IMMSdkService;
import com.mediatek.mmsdk.IEffectListener;
import com.mediatek.mmsdk.IEffectHalClient;

import java.util.List;

//TODO the exception need complete
public class CameraMmManager {

    private static final String TAG = "CameraMmManager";
    private static final String CAMERA_MM_SERVICE_BINDER_NAME = "media.mmsdk";
    private static final boolean isDebug = true;

    private final Context mContext;

    private IMMSdkService mIMMSdkService;
    private IFeatureManager mIFeatureManager;
    private IEffectFactory mIEffectFactory;
    private IEffectHal mIEffectHal;

    public CameraMmManager(Context context) {
        mContext = context;
    }

    public CameraMmEffect openCameraEffect(EffectHalVersion version,
            CameraMmEffect.StateCallback callback, Handler handler) throws CameraEffectHalException {
        // <1>check not null
        if (version == null) {
            throw new IllegalArgumentException("effect version is null");
        } else if (handler == null) {
            if (Looper.myLooper() != null) {
                handler = new Handler();
            } else {
                throw new IllegalArgumentException("Looper doesn't exist in the calling thread");
            }
        }
        // <2>open camera effect
        return openEffect(version, callback, handler);
    }

    // get current EffecHal supported effects,D1 not have finished this,maybe
    // ready in D3
    // the returns value will be put into the versions
    public void getAllSupportedEffectHal(List<String> versions) throws CameraEffectHalException {
        getEffectFactory();
        try {
            mIEffectFactory.getAllSupportedEffectHal(versions);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void getEffectHalVersions(String effectName, List<EffectHalVersion> versions)
            throws CameraEffectHalException {
        getEffectFactory();
        try {
            mIEffectFactory.getSupportedVersion(effectName, versions);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private CameraMmEffect openEffect(EffectHalVersion version,
            CameraMmEffect.StateCallback callback, Handler handler) throws CameraEffectHalException {

        CameraMmEffect cameraEffect = null;

        // <Step1> first get the camera MM service
        IMMSdkService cameraMmService = getCameraService();
        if (cameraMmService == null) {
            throw new RuntimeException("Camera effect service is currently unavailable");
        }
        // <Step2> get the Feature Manager
        getFeatureManager();

        // <Step3> get the Effect Factory
        getEffectFactory();

        // <Step4> get the effect HAL Client
        IEffectHalClient effectHalClient = getEffectHalClient(version);

        // <Step5> create the Camera effect
        CameraMmEffectImpl cameraMmEffectImpl = new CameraMmEffectImpl(callback, handler);

        // <Step6> init the EffectHalClient
        int initValue = -1;
        try {
            // now native status: uninit -> init
            initValue = effectHalClient.init();
        } catch (RemoteException e1) {
            throw new CameraEffectHalException(CameraEffectHalException.EFFECT_INITIAL_ERROR); 
        }

        // <Step7> set effect listener
        IEffectListener effectListener = cameraMmEffectImpl.getEffectHalListener();
        int setListenerValue = -1;
        try {
            setListenerValue = effectHalClient.setEffectListener(effectListener);
        } catch (RemoteException e) {
            CameraEffectHalRuntimeException exception = new CameraEffectHalRuntimeException(
                    CameraEffectHalException.EFFECT_HAL_LISTENER_ERROR);
            cameraMmEffectImpl.setRemoteCamerEffectFail(exception);
        }

        // <Step8> set remote effect camera
        cameraMmEffectImpl.setRemoteCameraEffect(effectHalClient);

        cameraEffect = cameraMmEffectImpl;
        if (isDebug) {
            Log.i(TAG, "[openEffect],initValue = " + initValue +",setListenerValue = " + setListenerValue);
        }
        
        return cameraEffect;
    }

    private IMMSdkService getCameraService() throws CameraEffectHalException {
        if (mIMMSdkService == null) {
            if (isDebug) {
                Log.i(TAG, "getCameraMmService: Reconnecting to camera effect service");
            }
            getMmSdkService();
            if (mIMMSdkService == null) {
                throw new CameraEffectHalException(
                        CameraEffectHalException.EFFECT_HAL_SERVICE_ERROR);
            }
        }
        if (isDebug) {
            Log.i(TAG, "mIMMSdkService = " + mIMMSdkService);
        }
        return mIMMSdkService;
    }

    private IFeatureManager getFeatureManager() throws CameraEffectHalException {
        getCameraService();
        if (mIFeatureManager == null) {
            BinderHolder featureManagerHolder = new BinderHolder();
            try {
                mIMMSdkService.connectFeatureManager(featureManagerHolder);
            } catch (RemoteException e) {
                throw new CameraEffectHalException(
                        CameraEffectHalException.EFFECT_HAL_FEATUREMANAGER_ERROR);
            }
            mIFeatureManager = IFeatureManager.Stub.asInterface(featureManagerHolder.getBinder());
        }
        return mIFeatureManager;
    }

    private IEffectFactory getEffectFactory() throws CameraEffectHalException {
        getFeatureManager();
        if (mIEffectFactory == null) {
            BinderHolder effectFactoryHolder = new BinderHolder();
            try {
                mIFeatureManager.getEffectFactory(effectFactoryHolder);
            } catch (RemoteException e) {
                throw new CameraEffectHalException(
                        CameraEffectHalException.EFFECT_HAL_FACTORY_ERROR);
            }
            mIEffectFactory = IEffectFactory.Stub.asInterface(effectFactoryHolder.getBinder());
        }
        return mIEffectFactory;
    }

    // TODO this whether need or not ? [Need Check]
    private IEffectHal getEffectHal(EffectHalVersion version) throws CameraEffectHalException {
        BinderHolder effectHalHolder = new BinderHolder();
        try {
            mIEffectFactory.createEffectHal(version, effectHalHolder);
        } catch (RemoteException e) {
            throw new CameraEffectHalException(CameraEffectHalException.EFFECT_HAL_ERROR);
        }
        mIEffectHal = IEffectHal.Stub.asInterface(effectHalHolder.getBinder());
        return mIEffectHal;
    }

    private IEffectHalClient getEffectHalClient(EffectHalVersion version)
            throws CameraEffectHalException {
        getEffectFactory();
        BinderHolder effectHalClientHolder = new BinderHolder();
        try {
            mIEffectFactory.createEffectHalClient(version, effectHalClientHolder);
        } catch (RemoteException e) {
            throw new CameraEffectHalException(CameraEffectHalException.EFFECT_HAL_CLIENT_ERROR);
        }
        return IEffectHalClient.Stub.asInterface(effectHalClientHolder.getBinder());

    }

    private void getMmSdkService() {

        mIMMSdkService = null;
        // <1>get the CAMERA_MM_SERVICE_BINDER_NAME service ,will return the
        // IBinder object
        IBinder cameraMmManagerService = ServiceManager.getService(CAMERA_MM_SERVICE_BINDER_NAME);
        if (cameraMmManagerService == null) {
            return;
        }
        // <2>set the binder death listener for service
        try {
            cameraMmManagerService.linkToDeath(new CameraMmServiceBinderListener(), 0);
        } catch (RemoteException e) {

        }

        // <3>get the IMMSdkService
        /**
         * Cast an IBinder object into an IMMSdkService object
         * */
        mIMMSdkService = IMMSdkService.Stub.asInterface(cameraMmManagerService);
    }

    /**
     * Listener for camera MM service death.
     * 
     */
    private class CameraMmServiceBinderListener implements IBinder.DeathRecipient {

        @Override
        public void binderDied() {
            // TODO Auto-generated method stub Need Check
        }
    }
}
