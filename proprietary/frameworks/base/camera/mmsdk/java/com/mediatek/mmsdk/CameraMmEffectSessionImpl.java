package com.mediatek.mmsdk;

import static com.android.internal.util.Preconditions.checkNotNull;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CameraCaptureSession.StateCallback;
import android.hardware.camera2.dispatch.ArgumentReplacingDispatcher;
import android.hardware.camera2.dispatch.BroadcastDispatcher;
import android.hardware.camera2.dispatch.DuckTypingDispatcher;
import android.hardware.camera2.dispatch.HandlerDispatcher;
import android.hardware.camera2.dispatch.InvokeDispatcher;
import android.hardware.camera2.impl.CameraCaptureSessionImpl;
import android.hardware.camera2.utils.TaskDrainer;
import android.hardware.camera2.utils.TaskSingleDrainer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import com.mediatek.mmsdk.BaseParameters;
import com.mediatek.mmsdk.CameraMmEffect;
import com.mediatek.mmsdk.CameraMmEffectImpl;

import java.util.List;

public class CameraMmEffectSessionImpl extends CameraMmEffectSession {
    private static final String TAG = "CameraMmEffectSessionImpl";

    private CameraMmEffectImpl mCameraMmEffectImpl;
    private static final boolean VERBOSE = true;

    /** This session is closed; all further calls will throw ISE */
    private boolean mClosed = false;

    /** User-specified set of surfaces used as the configuration outputs */
    private final List<Surface> mOutputs;
    // User-specified state handler used for outgoing state callback events
    // this handler is from AP calling
    private final Handler mStateHandler;
    // this callback is from CameraMmEffect's
    // createEffectSession(xxx,xxx,callback),but have been
    // wrapped by proxy by MmSdkCallbackProxies.SessionStateCallbackProxy
    // used for notify AP current state
    private final CameraMmEffectSession.StateCallback mStateCallback;
    /** Internal handler; used for all incoming events to preserve total order */
    private final Handler mDeviceHandler;

    /**
     * Drain Sequence IDs which have been queued but not yet finished with
     * aborted/completed
     */
    private final TaskDrainer<Integer> mSequenceDrainer;
    /** Drain state transitions from ACTIVE -> IDLE */
    private final TaskSingleDrainer mIdleDrainer;
    /** Drain state transitions from BUSY -> IDLE */
    private final TaskSingleDrainer mAbortDrainer;
    /** Drain the UNCONFIGURED state transition */
    private final TaskSingleDrainer mUnconfigureDrainer;
    /** This session failed to be configured successfully */
    private final boolean mConfigureSuccess;

    // Is the session in the process of aborting? Pay attention to BUSY->IDLE
    // transitions.
    private volatile boolean mAborting;

    /**
     * Do not unconfigure if this is set; another session will overwrite
     * configuration
     */
    private boolean mSkipUnconfigure = false;

    public CameraMmEffectSessionImpl(
            List<Surface> outputs, CameraMmEffectSession.StateCallback callback,
            Handler effectStateHandler, CameraMmEffectImpl effectImpl, Handler deviceStateHandler,
            boolean configureSuccess) {
        if (outputs == null || outputs.isEmpty()) {
            throw new IllegalArgumentException("outputs must be a non-null, non-empty list");
        } else if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }

        mOutputs = outputs;
        mStateHandler = checkHandler(effectStateHandler);
        mStateCallback = createUserStateCallbackProxy(mStateHandler, callback);
        mDeviceHandler = checkNotNull(deviceStateHandler, "deviceStateHandler must not be null");
        mCameraMmEffectImpl = checkNotNull(effectImpl, "deviceImpl must not be null");

        /*
         * Use the same handler as the device's StateCallback for all the
         * internal coming events
         * 
         * This ensures total ordering between CameraDevice.StateCallback and
         * CameraDeviceImpl.CaptureCallback events.
         */
        mSequenceDrainer = new TaskDrainer<>(mDeviceHandler, new SequenceDrainListener(),
        /* name */"seq");
        mIdleDrainer = new TaskSingleDrainer(mDeviceHandler, new IdleDrainListener(),
        /* name */"idle");
        mAbortDrainer = new TaskSingleDrainer(mDeviceHandler, new AbortDrainListener(),
        /* name */"abort");
        mUnconfigureDrainer = new TaskSingleDrainer(mDeviceHandler, new UnconfigureDrainListener(),
        /* name */"unconf");

        // CameraDevice should call configureOutputs and have it finish before
        // constructing us
        if (configureSuccess) {
            mStateCallback.onConfigured(this);
            if (VERBOSE)
                Log.v(TAG, "Created session successfully");
            mConfigureSuccess = true;
        } else {
            mStateCallback.onConfigureFailed(this);
            mClosed = true;
            Log.e(TAG, "Failed to create capture session; configuration failed");
            mConfigureSuccess = false;
        }

    }

    @Override
    public CameraMmEffectImpl getEffect() {
        return mCameraMmEffectImpl;
    }

    @Override
    public void close() {
        if (mClosed) {
            if (VERBOSE) {
                Log.i(TAG, "[close],current session is closed,so return");
            }
            return;
        }

        if (VERBOSE) {
            Log.i(TAG, "[close] on going");
        }
        mClosed = true;
        try {
            mCameraMmEffectImpl.stopRepeating();
        } catch (CameraEffectHalException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // If no sequences are pending, fire #onClosed immediately
        mSequenceDrainer.beginDrain();
    }

    @Override
    public void start(int index, BaseParameters ps, CaptureCallback callback, Handler handler,
            long timeLapse, boolean repeating) {
        checkNotClosed();
        handler = checkHandler(handler, callback);
        if (VERBOSE) {
            Log.v(TAG, "capture - callback " + callback + " handler " + handler);
        }
        CameraMmEffectImpl.CaptureCallback cb = createCaptureCallbackProxy(handler, callback);
        mCameraMmEffectImpl.start(index, ps, timeLapse, mDeviceHandler, cb, repeating);
    }

    @Override
    public void addInputFrameParameter(int index, BaseParameters baseParameters, long timestamp,
            boolean repeating) {
        if (baseParameters == null) {
            throw new IllegalArgumentException("[addInputFrameParameter] parameters is null");
        }
        mCameraMmEffectImpl.addInputFrameParameter(index, baseParameters, timestamp, repeating);
    }

    @Override
    public void abort(BaseParameters baseParameters) {
        // baseParameters may be null
        if (VERBOSE) {
            Log.v(TAG, "[abort]baseParameters " + baseParameters);
        }
        mCameraMmEffectImpl.abortCapture(baseParameters);
    }

    @Override
    public void closeSession() {
        mCameraMmEffectImpl.closeSession();
    }

    // Replace this session with another session.
    // <p>After this call completes, the session will not call any further
    // methods on the camera
    // device.</p>
    void replaceSessionClose() {
        if (VERBOSE) {
            Log.i(TAG, "[replaceSessionClose]");
        }
        mSkipUnconfigure = true;

        close();
    }

    private void checkNotClosed() {
        if (mClosed) {
            throw new IllegalStateException("Session has been closed; further changes are illegal.");
        }
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
     * Post calls into a CameraMmEffectSession.StateCallback to the
     * user-specified {@code handler}.TODO
     */
    private StateCallback createUserStateCallbackProxy(Handler handler, StateCallback callback) {
        InvokeDispatcher<StateCallback> userCallbackSink = new InvokeDispatcher<>(callback);
        HandlerDispatcher<StateCallback> handlerDispatcher = new HandlerDispatcher<>(
                userCallbackSink, handler);
        return new MmSdkCallbackProxies.SessionStateCallbackProxy(handlerDispatcher);

    }

    private class SequenceDrainListener implements TaskDrainer.DrainListener {
        @Override
        public void onDrained() {
            /*
             * No repeating request is set; and the capture queue has fully
             * drained.
             * 
             * If no captures were queued to begin with, and an abort was
             * queued, it's still possible to get another BUSY before the last
             * IDLE.
             * 
             * If the camera is already "IDLE" and no aborts are pending, then
             * the drain immediately finishes.
             */
            if (VERBOSE)
                Log.v(TAG, "onSequenceDrained");
            mAbortDrainer.beginDrain();
        }
    }

    private class AbortDrainListener implements TaskDrainer.DrainListener {
        @Override
        public void onDrained() {
            if (VERBOSE)
                Log.v(TAG, "onAbortDrained");
            synchronized (CameraMmEffectSessionImpl.this) {
                /*
                 * Any queued aborts have now completed.
                 * 
                 * It's now safe to wait to receive the final "IDLE" event, as
                 * the camera device will no longer again transition to "ACTIVE"
                 * by itself.
                 * 
                 * If the camera is already "IDLE", then the drain immediately
                 * finishes.
                 */
                mIdleDrainer.beginDrain();
            }
        }
    }

    private class IdleDrainListener implements TaskDrainer.DrainListener {

        @Override
        public void onDrained() {

            if (VERBOSE) {
                Log.i(TAG, "[onDrained]");
            }

            // Take device lock before session lock so that we can call back
            // into device
            // without causing a deadlock
            synchronized (mCameraMmEffectImpl.mInterfaceLock) {
                synchronized (CameraMmEffectSessionImpl.this) {

                    // Fast path: A new capture session has replaced this one;
                    // don't unconfigure.
                    if (mSkipUnconfigure) {
                        mStateCallback.onClosed(CameraMmEffectSessionImpl.this);
                        return;
                    }

                    // Slow path: #close was called explicitly on this session;
                    // unconfigure first
                    mUnconfigureDrainer.taskStarted();
                    try {
                        mCameraMmEffectImpl.configureOutputsChecked(null, null, null);
                    } catch (CameraEffectHalException e) {
                        // TODO Auto-generated catch block
                        Log.e(TAG, "Exception while configuring outputs: ", e);
                    } catch (IllegalArgumentException e) {
                        mUnconfigureDrainer.taskFinished();
                    }
                    mUnconfigureDrainer.beginDrain();

                }
            }

        }

    }

    private class UnconfigureDrainListener implements TaskDrainer.DrainListener {

        @Override
        public void onDrained() {
            if (VERBOSE) Log.v(TAG, "[onUnconfigureDrained]");
            synchronized (CameraMmEffectSessionImpl.this) {
                mStateCallback.onClosed(CameraMmEffectSessionImpl.this);
            }
        }

    }

    // Create an internal state callback, to be invoked on the mDeviceHandler
    CameraMmEffectImpl.DeviceStateCallback getDeviceStateCallback() {
        final CameraMmEffectSession session = this;
        return new CameraMmEffectImpl.DeviceStateCallback() {
            private boolean mBusy = false;
            private boolean mActive = false;

            @Override
            public void onOpened(CameraMmEffect effect) {
                throw new AssertionError("Camera must already be open before creating a session");
            }

            @Override
            public void onDisconnected(CameraMmEffect effect) {
                if (VERBOSE)
                    Log.v(TAG, "onDisconnected");
                close();
            }

            @Override
            public void onUnconfigured(CameraMmEffect effect) {
                if (VERBOSE)
                    Log.v(TAG, "onUnconfigured");
                synchronized (session) {
                    // Ignore #onUnconfigured before #close is called.
                    //
                    // Normally, this is reached when this session is closed and
                    // no immediate other
                    // activity happens for the camera, in which case the camera
                    // is configured to
                    // null streams by this session and the UnconfigureDrainer
                    // task is started.
                    // However, we can also end up here if
                    //
                    // 1) Session is closed
                    // 2) New session is created before this session finishes
                    // closing, setting
                    // mSkipUnconfigure and therefore this session does not
                    // configure null or
                    // start the UnconfigureDrainer task.
                    // 3) And then the new session fails to be created, so
                    // onUnconfigured fires
                    // _anyway_.
                    // In this second case, need to not finish a task that was
                    // never started, so
                    // guard with mSkipUnconfigure
                    if (mClosed && mConfigureSuccess && !mSkipUnconfigure) {
                        mUnconfigureDrainer.taskFinished();
                    }
                }
            }

            @Override
            public void onActive(CameraMmEffect effect) {
                mIdleDrainer.taskStarted();
                mActive = true;

                if (VERBOSE)
                    Log.v(TAG, "onActive");
                mStateCallback.onActive(session);
            }

            @Override
            public void onBusy(CameraMmEffect effect) {
                mBusy = true;

                // TODO: Queue captures during abort instead of failing them
                // since the app won't be able to distinguish the two actives
                // Don't signal the application since there's no clean mapping
                // here
                if (VERBOSE)
                    Log.v(TAG, "onBusy");
            }

            @Override
            public void onIdle(CameraMmEffect effect) {
                boolean isAborting;
                if (VERBOSE)
                    Log.v(TAG, "onIdle");
                synchronized (session) {
                    isAborting = mAborting;
                }

                /*
                 * Check which states we transitioned through:
                 * 
                 * (ACTIVE -> IDLE) (BUSY -> IDLE)
                 * 
                 * Note that this is also legal: (ACTIVE -> BUSY -> IDLE)
                 * 
                 * and mark those tasks as finished
                 */
                if (mBusy && isAborting) {
                    mAbortDrainer.taskFinished();

                    synchronized (session) {
                        mAborting = false;
                    }
                }

                if (mActive) {
                    mIdleDrainer.taskFinished();
                }

                mBusy = false;
                mActive = false;

                mStateCallback.onReady(session);
            }

            @Override
            public void onError(CameraMmEffect effect, int error) {
                Log.wtf(TAG, "Got device error " + error);
            }

            @Override
            public void onPrepared(IEffectHal effect, BaseParameters result) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onStated(IEffectHal effect, BaseParameters result) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onCompleted(IEffectHal effect, BaseParameters partialResult, long uid) {
                // TODO Auto-generated method stub

            }

        };
    }

    /**
     * convert CameraMmEffectSessionImpl.CaptureCallback to
     * CameraMmEffectImpl.CaptureCallback, so the callback will be : Forward
     * callback from CameraMmEffectImpl.CaptureCallback to the
     * CameraMmEffectSessionImpl.CaptureCallback.
     * 
     * @param handler
     * @param callback
     * @return
     */
    CameraMmEffectImpl.CaptureCallback createCaptureCallbackProxy(Handler handler,
            CaptureCallback callback) {
        CameraMmEffectImpl.CaptureCallback localCallback = new CameraMmEffectImpl.CaptureCallback() {

            @Override
            public void onCaptureSequenceCompleted(CameraMmEffect camera, int sequenceId,
                    long frameNumber) {
                // TODO will notify current session is completed
            }

            @Override
            public void onCaptureSequenceAborted(CameraMmEffect camera, int sequenceId) {
                // TODO will notify current session is completed
            }

        };

        if (callback == null) {
            return localCallback;
        }

        // <Step1> new localCallback for dispatcher
        InvokeDispatcher<CameraMmEffectImpl.CaptureCallback> localSink = new InvokeDispatcher<>(
                localCallback);
        // <Step2> get user's captureCallback dispatcher
        InvokeDispatcher<CaptureCallback> userCallbackSink = new InvokeDispatcher<>(callback);
        // <Step3> create the user's callback handerDispatcher
        HandlerDispatcher<CaptureCallback> handlerPassthrough = new HandlerDispatcher<>(
                userCallbackSink, handler);
        // <Step4> will convert user's handlerDispatcher to captureCallback
        DuckTypingDispatcher<CameraMmEffectImpl.CaptureCallback, CaptureCallback> duckToSession = new DuckTypingDispatcher<>(
                handlerPassthrough, CaptureCallback.class);

        // <Step5> will change the argument by effectSession
        ArgumentReplacingDispatcher<CameraMmEffectImpl.CaptureCallback, CameraMmEffectSessionImpl> replaceDeviceWithSession = new ArgumentReplacingDispatcher<CameraMmEffectImpl.CaptureCallback, CameraMmEffectSessionImpl>(
                duckToSession, 0, this);

        // <Step6> will notify user callback and localSink callback
        BroadcastDispatcher<CameraMmEffectImpl.CaptureCallback> broadcaster = new BroadcastDispatcher<CameraMmEffectImpl.CaptureCallback>(
                replaceDeviceWithSession, localSink);

        return new MmSdkCallbackProxies.DeviceCaptureCallback(broadcaster);
    }
}
