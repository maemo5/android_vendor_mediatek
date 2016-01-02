package com.mediatek.mmsdk;

import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.utils.LongParcelable;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;

import com.mediatek.mmsdk.IEffectHal;
import com.mediatek.mmsdk.IEffectHalClient;
import com.mediatek.mmsdk.IEffectListener;

import com.mediatek.mmsdk.BaseParameters;
import com.mediatek.mmsdk.CameraEffectHalException;
import com.mediatek.mmsdk.CameraMmEffect;
import com.mediatek.mmsdk.CameraMmEffectImpl;
import com.mediatek.mmsdk.CameraMmEffectSession;
import com.mediatek.mmsdk.CameraMmEffectSessionImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.AbstractMap.SimpleEntry;

public class CameraMmEffectImpl extends CameraMmEffect {

    private static final String TAG = "CameraMmEffectImpl";
    private static final int REQUEST_ID_NONE = -1;
    private static final int SUCCESS_VALUE = 1;

    private boolean DEBUG = true;
    private boolean mIsCloseSession = false;
    private boolean mIsCloseEffectHal = false;
    private boolean mInError = false;
    private volatile boolean mClosing = false;

    private CameraMmEffectSessionImpl mCurrentSession;
    //access from this class and Session only!
    final Object mInterfaceLock = new Object();
    
    // this callback is used to notify AP about current camera effect HAL Client status
    private StateCallback mEffectHalCallback;
    
    //according to current Effect HAL state,notify EffectHalSession to change session's state
    private DeviceStateCallback mSessionStateCallback; 
    
    //this handler is from AP call openCameraMmEffect(xxx,xxx,handler)'s parameters
    private Handler mEffectHandler;
    private IEffectHalClient mIEffectHalClient;
    private EffectHalListener mEffectHalListener = new EffectHalListener();

    // Map stream IDs to Surfaces
    private final SparseArray<Surface> mConfiguredOutputs = new SparseArray<Surface>();

    private int mRepeatingRequestId = REQUEST_ID_NONE;
    /** map request IDs to callback/request data */
    private final SparseArray<CaptureCallbackHolder> mCaptureCallbackMap = new SparseArray<CaptureCallbackHolder>();

    private final ArrayList<Integer> mRepeatingRequestIdDeletedList = new ArrayList<Integer>();

    /**
     * A list tracking request and its expected last frame. Updated when calling
     * ICameraDeviceUser methods.
     */
    private final List<SimpleEntry</* frameNumber */Long, /* requestId */Integer>> mFrameNumberRequestPairs = new ArrayList<SimpleEntry<Long, Integer>>();


    public CameraMmEffectImpl(StateCallback callback, Handler handler) {
        mEffectHalCallback = callback;
        mEffectHandler = handler;
    }

    public EffectHalListener getEffectHalListener() {
        return mEffectHalListener;
    }

    // Used for get the IEffectHalClient when get the proxy success
    public void setRemoteCameraEffect(IEffectHalClient client) {
        // TODO Need Check
        synchronized (mInterfaceLock) {
            if (mInError) {
                return;
            }
            mIEffectHalClient = client;
            mEffectHandler.post(mCallOnOpened);
            mEffectHandler.post(mCallOnUnconfigured);
        }
    }

    // Call to indicate failed connection to a remote camera effect service
    // will use the android errors.h tag [Need Check]
    public void setRemoteCamerEffectFail(final CameraEffectHalRuntimeException exception) {
        int failureCode = StateCallback.ERROR_EFFECT_DEVICE;
        boolean failureIsError = true;
        switch (exception.getReason()) {
        case CameraEffectHalException.EFFECT_HAL_IN_USE:
            failureCode = StateCallback.ERROR_EFFECT_HAL_IN_USE;
            break;

        case CameraEffectHalException.EFFECT_HAL_CLIENT_ERROR:
            failureCode = StateCallback.ERROR_MAX_EFFECT_IN_USE;
            break;

        case CameraEffectHalException.EFFECT_HAL_SERVICE_ERROR:
            failureCode = StateCallback.ERROR_EFFECT_DISABLED;
            break;

        case CameraEffectHalException.EFFECT_HAL_FEATUREMANAGER_ERROR:
            failureIsError = false;
            break;

        case CameraEffectHalException.EFFECT_HAL_FACTORY_ERROR:
            failureCode = StateCallback.ERROR_EFFECT_DEVICE;
            break;

        case CameraEffectHalException.EFFECT_HAL_LISTENER_ERROR:
            failureCode = StateCallback.ERROR_EFFECT_LISTENER;
            break;

        default:
            Log.wtf(TAG, "Unknown failure in opening camera device: " + exception.getReason());
            break;
        }

        final int code = failureCode;
        final boolean isError = failureIsError;
        synchronized (mInterfaceLock) {
            mInError = true;
            mEffectHandler.post(new Runnable() {

                @Override
                public void run() {
                    if (isError) {
                        mEffectHalCallback.onError(CameraMmEffectImpl.this, code);
                    } else {
                        mEffectHalCallback.onDisconnected(CameraMmEffectImpl.this);
                    }
                }
            });
        }

    }

    // this interface used for create the effect session,such as VFB/Lomo
    // effect...
    // TODO need check the key /value,if some feature need to change more than
    // one parameters,how to do ? is Need A List ???
    @Override
    public void createEffectSession(List<Surface> outputs,
            CameraMmEffectSession.StateCallback callback, Handler handler, String key, String value)
            throws CameraEffectHalException {

        if (DEBUG) {
            Log.i(TAG, "[createEffectSession]");
        }
        checkIfCameraClosedOrInError();

        // check surface
        if (outputs == null) {
            throw new IllegalArgumentException(
                    "createEffectSession: the outputSurface must not be null");
        }

        // check handler
        handler = checkHandler(handler);

        // Notify current session that it's going away, before starting camera
        // operations After this call completes, the session is not allowed to
        // call into CameraDeviceImpl
        if (mCurrentSession != null) {
            mCurrentSession.replaceSessionClose();
        }

        boolean configureSuccess = true;
        CameraEffectHalException pendingException = null;
        try {
            configureSuccess = configureOutputsChecked(outputs, key, value);
        } catch (CameraEffectHalException e) {
            configureSuccess = false;
            pendingException = e;
            if (DEBUG) {
                Log.v(TAG, "createEffectSession- failed with exception ", e);
            }
        }

        CameraMmEffectSessionImpl newSessionImpl = new CameraMmEffectSessionImpl(outputs, callback,
                handler, this, mEffectHandler, configureSuccess);

        // TODO: wait until current session closes, then create the new session
        mCurrentSession = newSessionImpl;

        if (pendingException != null) {
            throw pendingException;
        }

        mSessionStateCallback = mCurrentSession.getDeviceStateCallback();

    }

    public int start(int index, BaseParameters ps, long timeLapse, Handler handler,CaptureCallback callback,
            boolean repeating) {
        if (DEBUG) {
            Log.i(TAG, "calling start begin");
        }
        return submitCaptureRequest(index, ps, timeLapse, handler, callback,repeating);
    }

    @Override
    public void getCaputreRequirement(BaseParameters outParameters, List<Surface> outSurface) {
        // @1 TODO need improve the status_t when fail,maybe need add the
        // exception
        // to notify AP
        // this function will return the status_t
        // @2 call prepare need at the state of configured,so will add this
        // state checked
        int getRequirementValue = -1;
        try {
            getRequirementValue = mIEffectHalClient.getCaptureRequirement(outParameters);
        } catch (RemoteException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
        if (SUCCESS_VALUE != getRequirementValue) {
            if (DEBUG) {
                Log.i(TAG, "[createCaputreRequest] getCaptureRequirement fail");
            }
        }
        // getInputSurfaces(out List<Surface> input);
        // this function will return the status_t
        int getInputSurfaceValue = -1;
        try {
            getInputSurfaceValue = mIEffectHalClient.getInputSurfaces(outSurface);
        } catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        if (SUCCESS_VALUE != getInputSurfaceValue) {
            if (DEBUG) {
                Log.i(TAG, "[createCaputreRequest] getInputSurfaceValue fail");
            }
        }

        int prepareSuccess = -1;
        try {
            prepareSuccess = mIEffectHalClient.prepare();
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // when called the prepare, IEffectListener will callback the onPrepared
        if (SUCCESS_VALUE != prepareSuccess) {
            if (DEBUG) {
                Log.i(TAG, "[createCaputreRequest] prepareSuccess fail");
            }
        }
    }

    @Override
    public int setInputsyncMode(int index, boolean sync) {
        int status_t = -1;
        try {
            status_t = mIEffectHalClient.setInputsyncMode(index, sync);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return status_t;
    }

    @Override
    public int setOutputsyncMode(int index, boolean sync) {
        int status_t = -1;
        try {
            status_t = mIEffectHalClient.setOutputsyncMode(index, sync);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return status_t;
    }

    @Override
    public boolean getInputsyncMode(int index) {
        boolean value = false;
        try {
            value = mIEffectHalClient.getInputsyncMode(index);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return value;
    }

    @Override
    public boolean getOutputsyncMode(int index) {
        boolean value = false;
        try {
            value = mIEffectHalClient.getOutputsyncMode(index);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return value;
    }

    /**
     * Attempt to configure the outputs; the device goes to idle and then
     * configures the new outputs if possible.
     * 
     * <p>
     * The configuration may gracefully fail, if there are too many outputs, if
     * the formats are not supported, or if the sizes for that format is not
     * supported. In this case this function will return {@code false} and the
     * unconfigured callback will be fired.
     * </p>
     * 
     * <p>
     * If the configuration succeeds (with 1 or more outputs), then the idle
     * callback is fired. Unconfiguring the device always fires the idle
     * callback.
     * </p>
     * 
     * @param outputs
     *            a list of one or more surfaces, or {@code null} to unconfigure
     * @return whether or not the configuration was successful
     * @throws CameraEffectHalException
     *             if there were any unexpected problems during configuration
     */
    public boolean configureOutputsChecked(List<Surface> outputs, String key, String value)
            throws CameraEffectHalException {
        // Treat a null input the same an empty list
        if (outputs == null) {
            outputs = new ArrayList<Surface>();
        }
        boolean success = false;
        synchronized (mInterfaceLock) {
            checkIfCameraClosedOrInError();
            // Streams to create
            HashSet<Surface> addSet = new HashSet<Surface>(outputs);
            // Streams to delete
            List<Integer> deleteList = new ArrayList<Integer>(); 
            
            for (int i = 0; i < mConfiguredOutputs.size(); i++) {
                int streamId = mConfiguredOutputs.keyAt(i);
                Surface s = mConfiguredOutputs.valueAt(i);

                if (!outputs.contains(s)) {
                    deleteList.add(streamId);
                } else {
                    addSet.remove(s); // Don't create a stream previously
                                      // created
                }
            }

            mEffectHandler.post(mCallOnBusy);
            stopRepeating();

            waitUntilIdle();

            if (key != null && value != null) {
                try {
                    mIEffectHalClient.setParameter(key, value);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            // set outputSurface,now EffectHal have established a link with
            // frameWrok
            // the output from EffectHal will be put into outputs
            try {
                //Notice:the outputs maybe null
                mIEffectHalClient.setOutputSurface(outputs);
            } catch (RemoteException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            // configure effect HAL;now effect HAL is in configured state
            try {
                success = SUCCESS_VALUE == mIEffectHalClient.configure();
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Delete all streams first (to free up HW resources)
            for (Integer streamId : deleteList) {
                // mRemoteDevice.deleteStream(streamId);
                mConfiguredOutputs.delete(streamId);
            }

            // Add all new streams
            for (Surface s : addSet) {
                // TODO: remove width,height,format since we are ignoring
                // it.
                int streamId = 0 /* mRemoteDevice.createStream(0, 0, 0, s) */;
                streamId++; // Jian add This just for the stream id is different
                mConfiguredOutputs.put(streamId, s);
            }

            if (success && outputs.size() > 0) {
                mEffectHandler.post(mCallOnIdle);
            } else {
                // Always return to the 'unconfigured' state if we didn't hit a
                // fatal error
                mEffectHandler.post(mCallOnUnconfigured);
            }

        }

        return success;
    }

    public void addInputFrameParameter(int index, BaseParameters baseParameters, long timestamp,
            boolean repeating) {
        // addInputParameter(int index, in BaseParameters parameter, long
        // timestamp, boolean repeat)
        // index is the which surface need,
        // this time the inputFrame if is processed,will notify the
        // IEffectListener.onInputFrameProcessed()
        try {
            mIEffectHalClient.addInputParameter(index, baseParameters, timestamp, repeating);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void abortCapture(BaseParameters baseParameters) {
        // when effect HAL have dealed this ,will notify the
        // :IEffectListener.onAborted();
        try {
            mIEffectHalClient.abort(baseParameters);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        mBaseParameters = baseParameters;
    }

    private BaseParameters mBaseParameters;

    public void closeSession() {
        // TODO the baseParameters will are you from ???????
        try {
            mIEffectHalClient.abort(mBaseParameters);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        mIsCloseSession = true;
    }

    // TODO Need Check the repeating whether need it
    private int submitCaptureRequest(int index, BaseParameters ps, long timeLapse, Handler handler,CaptureCallback callback,
            boolean repeating) {
        // Need a valid handler, or current thread needs to have a looper, if
        // callback is valid
        handler = checkHandler(handler, callback);
        
        if (repeating) {
            try {
                stopRepeating();
            } catch (CameraEffectHalException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        LongParcelable lastFrameNumberRef = new LongParcelable();
        // addOutputParameter(int index, in BaseParameters parameter, long
        // timestamp, boolean repeat);

        // Need Check whether need this or not? //TODO
        int requestId = -1;
        try {
            mIEffectHalClient.addInputParameter(index, ps, timeLapse, repeating);
            requestId = mIEffectHalClient.addOutputParameter(index, ps, timeLapse, repeating);
            mIEffectHalClient.start(); // call this for start capture
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        mCaptureCallbackMap.put(requestId, new CaptureCallbackHolder(callback, null, handler, repeating));
        mEffectHandler.post(mCallOnActive);
        // when called start,the onStarted() will be notify TODO Need Check,maybe not used
        
        return requestId;
    }

    // TODO Need finished the IEffectListener
    // this same as the CameraDeviceImpl's camera device callback
    public class EffectHalListener extends IEffectListener.Stub {

        // when call effect HAL client prepare() interface, effect hal will notify AP when prepare done.
        @Override
        public void onPrepared(IEffectHal effect, BaseParameters result) throws RemoteException {
            mEffectHalCallback.onPrepared(effect, result);

        }

        // TODO Need check this callback is defined in the effect listener
        // @Override
        // public void onStarted(IEffectHal effect, BaseParameters
        // partialResult) {
        // mEffectHalCallback.onStated(effect, partialResult);
        // }

        // when the buffer is dispatched to the Effect HAL,this is
        // notified,means current inputFrame is in processing
        @Override
        public void onInputFrameProcessed(IEffectHal effect, BaseParameters parameter,
                BaseParameters partialResult) throws RemoteException {
            // mEffectHalCallback.onInputFrameProcessed(effect, parameter,
            // partialResult);

        }

        // when the frame is success processed by effect HAL,will notify this
        @Override
        public void onOutputFrameProcessed(IEffectHal effect, BaseParameters parameter,
                BaseParameters partialResult) throws RemoteException {
            // mEffectHalCallback.onOutputFrameProcessed(effect, parameter,
            // partialResult);

        }

        // when this session is processed done by the effect HAL,will notify
        // this
        @Override
        public void onCompleted(IEffectHal effect, BaseParameters partialResult, long uid)
                throws RemoteException {
            mIEffectHalClient.release();
            mEffectHalCallback.onCompleted(effect, partialResult, uid);
        }

        // if abort this capture,will be notified when Effect HAL have aborted
        @Override
        public void onAborted(IEffectHal effect, BaseParameters result) throws RemoteException {

            mIEffectHalClient.release();
            if (mIsCloseSession) {
                mIEffectHalClient.unconfigure();

            } else if (mIsCloseEffectHal) {
                mIEffectHalClient.unconfigure();
                mIEffectHalClient.uninit();
                // mEffectHalCallback.onClosed();

            } else {
                // mEffectHalCallback.onAborted(effect, result);
            }

        }

        // Effect HAL is failed in this session
        @Override
        public void onFailed(IEffectHal effect, BaseParameters result) throws RemoteException {
            // mEffectHalCallback.onFailed(effect, result);
        }

    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

    @Override
    public void closeEffectHal() {
        // TODO Auto-generated method stub

    }

    // *************************************************************************
    // ***********************************begin CaptureCallback*****************
    // *************************************************************************
    /**
     * <p>
     * A callback for tracking the progress of a {@link CaptureRequest}
     * submitted to the camera device.
     * </p>
     * 
     */
    public static abstract class CaptureCallback {

        /**
         * This constant is used to indicate that no images were captured for
         * the request.
         * 
         * @hide
         */
        public static final int NO_FRAMES_CAPTURED = -1;

        /**
         * This method is called when the camera device has started capturing
         * the output image for the request, at the beginning of image exposure.
         * 
         * @see android.media.MediaActionSound
         */
        public void onCaptureStarted(CameraMmEffect camera, CaptureRequest request, long timestamp,
                long frameNumber) {
            // default empty implementation
        }

        /**
         * This method is called when some results from an image capture are
         * available.
         * 
         * @hide
         */
        public void onCapturePartial(CameraMmEffect camera, CaptureRequest request,
                CaptureResult result) {
            // default empty implementation
        }

        /**
         * This method is called when an image capture makes partial forward
         * progress; some (but not all) results from an image capture are
         * available.
         * 
         */
        public void onCaptureProgressed(CameraMmEffect camera, CaptureRequest request,
                CaptureResult partialResult) {
            // default empty implementation
        }

        /**
         * This method is called when an image capture has fully completed and
         * all the result metadata is available.
         */
        public void onCaptureCompleted(CameraMmEffect camera, CaptureRequest request,
                TotalCaptureResult result) {
            // default empty implementation
        }

        /**
         * This method is called instead of {@link #onCaptureCompleted} when the
         * camera device failed to produce a {@link CaptureResult} for the
         * request.
         */
        public void onCaptureFailed(CameraMmEffect camera, CaptureRequest request,
                CaptureFailure failure) {
            // default empty implementation
        }

        /**
         * This method is called independently of the others in CaptureCallback,
         * when a capture sequence finishes and all {@link CaptureResult} or
         * {@link CaptureFailure} for it have been returned via this callback.
         */
        public void onCaptureSequenceCompleted(CameraMmEffect camera, int sequenceId,
                long frameNumber) {
            // default empty implementation
        }

        /**
         * This method is called independently of the others in CaptureCallback,
         * when a capture sequence aborts before any {@link CaptureResult} or
         * {@link CaptureFailure} for it have been returned via this callback.
         */
        public void onCaptureSequenceAborted(CameraMmEffect camera, int sequenceId) {
            // default empty implementation
        }
    }

    // **************************************************************************
    // ***********************************begin DeviceStateCallback**************
    // **************************************************************************
    public static abstract class DeviceStateCallback extends StateCallback {

        /**
         * The method called when a camera device has no outputs configured.
         * 
         */
        public void onUnconfigured(CameraMmEffect effect) {
            // Default empty implementation
        }

        /**
         * The method called when a camera device begins processing
         * {@link CaptureRequest capture requests}.
         * 
         */
        public void onActive(CameraMmEffect effect) {
            // Default empty implementation
        }

        /**
         * The method called when a camera device is busy.
         * 
         */
        public void onBusy(CameraMmEffect effect) {
            // Default empty implementation
        }

        /**
         * The method called when a camera device has finished processing all
         * submitted capture requests and has reached an idle state.
         * 
         */
        public void onIdle(CameraMmEffect effect) {
            // Default empty implementation
        }

    }

    // *******************************************************************
    // *******************************************************************
    // Runnables for all state transitions, except error, which needs the
    // error code argument
    private final Runnable mCallOnOpened = new Runnable() {

        @Override
        public void run() {
            DeviceStateCallback stateCallback2 = null;
            synchronized (mInterfaceLock) {
                if (mIEffectHalClient == null) {
                    // current camera effect hal has closed
                    return;
                }
                stateCallback2 = mSessionStateCallback;
            }
            if (stateCallback2 != null) {
                stateCallback2.onOpened(CameraMmEffectImpl.this);
            }
            mEffectHalCallback.onOpened(CameraMmEffectImpl.this);
        }
    };

    private final Runnable mCallOnUnconfigured = new Runnable() {

        @Override
        public void run() {
            DeviceStateCallback stateCallback2 = null;
            synchronized (mInterfaceLock) {
                if (mIEffectHalClient == null) {
                    return;
                }
                stateCallback2 = mSessionStateCallback;
            }
            if (stateCallback2 != null) {
                stateCallback2.onUnconfigured(CameraMmEffectImpl.this);
            }
        }
    };

    private final Runnable mCallOnActive = new Runnable() {

        @Override
        public void run() {
            DeviceStateCallback stateCallback2 = null;
            synchronized (mInterfaceLock) {
                if (mIEffectHalClient == null) {
                    return;
                }
                stateCallback2 = mSessionStateCallback;
            }
            if (stateCallback2 != null) {
                stateCallback2.onActive(CameraMmEffectImpl.this);
            }
        }
    };

    private final Runnable mCallOnBusy = new Runnable() {

        @Override
        public void run() {

            DeviceStateCallback sessionCallback = null;
            synchronized (mInterfaceLock) {
                if (mIEffectHalClient == null)
                    return; // Camera already closed

                sessionCallback = mSessionStateCallback;
            }
            if (sessionCallback != null) {
                sessionCallback.onBusy(CameraMmEffectImpl.this);
            }
        }
    };

    private final Runnable mCallOnClosed = new Runnable() {
        private boolean isClosedOnce = false;

        @Override
        public void run() {
            if (isClosedOnce) {
                throw new AssertionError("Don't post #onClosed more than once");
            }
            DeviceStateCallback sessionCallback = null;
            synchronized (mInterfaceLock) {
                sessionCallback = mSessionStateCallback;
            }
            if (sessionCallback != null) {
                sessionCallback.onClosed(CameraMmEffectImpl.this);
            }
            mEffectHalCallback.onClosed(CameraMmEffectImpl.this);
            isClosedOnce = true;
        }
    };

    private final Runnable mCallOnIdle = new Runnable() {

        @Override
        public void run() {
            DeviceStateCallback sessionCallback = null;
            synchronized (mInterfaceLock) {
                if (mIEffectHalClient == null)
                    return;

                sessionCallback = mSessionStateCallback;
            }
            if (sessionCallback != null) {
                sessionCallback.onIdle(CameraMmEffectImpl.this);
            }
        }
    };

    private final Runnable mCallOnDisconnected = new Runnable() {

        @Override
        public void run() {
            DeviceStateCallback sessionCallback = null;
            synchronized (mInterfaceLock) {
                if (mIEffectHalClient == null)
                    return; // Camera already closed

                sessionCallback = mSessionStateCallback;
            }
            if (sessionCallback != null) {
                sessionCallback.onDisconnected(CameraMmEffectImpl.this);
            }
            mEffectHalCallback.onDisconnected(CameraMmEffectImpl.this);
        }
    };

    private void checkIfCameraClosedOrInError() throws CameraEffectHalException {
        if (mInError) {
            throw new CameraEffectHalRuntimeException(CameraEffectHalException.EFFECT_HAL_FACTORY_ERROR,
                    "The camera device has encountered a serious error");
        }
        if (mIEffectHalClient == null) {
            throw new IllegalStateException("effect hal client have closed");
        }
    }

    /**
     * Default handler management.
     * 
     * <p>
     * If handler is null, get the current thread's Looper to create a Handler
     * with. If no looper exists, throw {@code IllegalArgumentException}.
     * </p>
     */
    static Handler checkHandler(Handler handler) {
        if (handler == null) {
            Looper looper = Looper.myLooper();
            if (looper == null) {
                throw new IllegalArgumentException(
                        "No handler given, and current thread has no looper!");
            }
            handler = new Handler(looper);
        }
        return handler;
    }

    /**
     * Default handler management, conditional on there being a callback.
     * 
     * <p>
     * If the callback isn't null, check the handler, otherwise pass it through.
     * </p>
     */
    static <T> Handler checkHandler(Handler handler, T callback) {
        if (callback != null) {
            return checkHandler(handler);
        }
        return handler;
    }

    public void stopRepeating() throws CameraEffectHalException {

        synchronized (mInterfaceLock) {
            if (REQUEST_ID_NONE != mRepeatingRequestId) {

                int requestId = mRepeatingRequestId;
                mRepeatingRequestId = REQUEST_ID_NONE;

                if (mCaptureCallbackMap.get(requestId) != null) {
                    mRepeatingRequestIdDeletedList.add(requestId);
                }

                LongParcelable lastFrameNumberRef = new LongParcelable();
                // mIEffectHalClient.abort(); TODO need cancel the request
                long lastFrameNumer = lastFrameNumberRef.getNumber();
                checkEarlyTriggerSequenceComplete(requestId, lastFrameNumer);
            }
        }
    }

    private void waitUntilIdle() throws CameraEffectHalException {
        synchronized (mInterfaceLock) {
            checkIfCameraClosedOrInError();
            if (mRepeatingRequestId != REQUEST_ID_NONE) {
                throw new IllegalStateException("Active repeating request ongoing");
            }

            // waitUntilIdle() TODO whether need this ?
        }
    }

    /**
     * This method checks lastFrameNumber returned from ICameraDeviceUser
     * methods for starting and stopping repeating request and flushing.
     * 
     * @param requestId
     * @param lastFrameNumber
     */
    private void checkEarlyTriggerSequenceComplete(final int requestId, final long lastFrameNumber) {

        if (CaptureCallback.NO_FRAMES_CAPTURED == lastFrameNumber) {
            final CaptureCallbackHolder holder;
            int index = mCaptureCallbackMap.indexOfKey(requestId);
            holder = index >= 0 ? mCaptureCallbackMap.valueAt(index) : null;
            if (holder != null) {
                mCaptureCallbackMap.remove(index);
                if (DEBUG) {
                    Log.i(TAG, String.format("remove holder for requestId %d "
                            + ",because lastFrame is %d.", requestId, lastFrameNumber));
                }
            }

            if (holder != null) {
                if (DEBUG) {
                    Log.v(TAG, "immediately trigger onCaptureSequenceAborted because"
                            + " request did not reach HAL");
                }

                Runnable resultDispatch = new Runnable() {

                    @Override
                    public void run() {
                        if (!CameraMmEffectImpl.this.isClosed()) {
                            if (DEBUG) {
                                Log.d(TAG, String
                                        .format("early trigger sequence complete for request %d",
                                                requestId));
                            }

                            if (lastFrameNumber > Integer.MAX_VALUE
                                    || lastFrameNumber < Integer.MIN_VALUE) {
                                throw new AssertionError(lastFrameNumber + " cannot be cast to int");
                            }

                            holder.getCallback().onCaptureSequenceAborted(CameraMmEffectImpl.this,
                                    requestId);

                        }
                    }
                };
                holder.getHandler().post(resultDispatch);
            } else {
                Log.w(TAG, String.format("did not register callback to request %d", requestId));
            }

        } else {
            mFrameNumberRequestPairs
                    .add(new SimpleEntry<Long, Integer>(lastFrameNumber, requestId));
            checkAndFireSequenceComplete();
        }
    }

    static class CaptureCallbackHolder {

        // because effectHal not use request,so the request maybe not need, such
        // as mRequestList, getRequest() TODO

        private final boolean mRepeating;
        private final CaptureCallback mCallback;
        // this requestlist maybe not need
        private final List<CaptureRequest> mRequestList;
        private final Handler mHandler;

        CaptureCallbackHolder(CaptureCallback callback, List<CaptureRequest> requestList,
                Handler handler, boolean repeating) {
            if (callback == null || handler == null) {
                throw new UnsupportedOperationException(
                        "Must have a valid handler and a valid callback");
            }
            mRepeating = repeating;
            mHandler = handler;
            mRequestList = new ArrayList<CaptureRequest>(requestList);
            mCallback = callback;
        }

        public boolean isRepeating() {
            return mRepeating;
        }

        public CaptureCallback getCallback() {
            return mCallback;
        }

        public CaptureRequest getRequest() {
            return getRequest(0);
        }

        public CaptureRequest getRequest(int subsequenceId) {
            if (subsequenceId >= mRequestList.size()) {
                throw new IllegalArgumentException(String.format(
                        "Requested subsequenceId %d is larger than request list size %d.",
                        subsequenceId, mRequestList.size()));
            } else {
                if (subsequenceId < 0) {
                    throw new IllegalArgumentException(String.format(
                            "Requested subsequenceId %d is negative", subsequenceId));
                } else {
                    return mRequestList.get(subsequenceId);
                }
            }
        }

        public Handler getHandler() {
            return mHandler;
        }

    } // CaptureCallbackHolder

    // **************************************************************************************
    // **************************************************************************************
    // ***************** This class tracks the last frame number for submitted
    // requests******
    // **************************************************************************************
    // **************************************************************************************
    public class FrameNumberTracker {
        private long mCompletedFrameNumber = -1;
        private final TreeSet<Long> mFutureErrorSet = new TreeSet<Long>();
        /** Map frame numbers to list of partial results */
        private final HashMap<Long, List<CaptureResult>> mPartialResults = new HashMap<>();

        private void update() {
            // do-nothing
        }

    }

    /**
     * Whether the camera device has started to close (may not yet have
     * finished)
     */
    private boolean isClosed() {
        return mClosing;
    }

    // TODO
    private void checkAndFireSequenceComplete() {
        // do-nothing
    }
}
