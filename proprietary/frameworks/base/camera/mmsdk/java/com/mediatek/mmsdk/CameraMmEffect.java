package com.mediatek.mmsdk;

import android.hardware.camera2.CameraAccessException;
import android.os.Handler;
import android.view.Surface;

import com.mediatek.mmsdk.CameraEffectHalException;
import java.util.List;

public abstract class CameraMmEffect implements AutoCloseable {

    /**
     * create the user wanted session 
     * @param outputs where the output buffer to place
     * @param callback about the effect hal client status callback
     * @param handler which handler user want 
     * @param key which parameters need to change
     * @param value which the parameter's value need to set to the effect hal
     * @throws CameraEffectHalException
     */
    public abstract void createEffectSession(List<Surface> outputs,
            CameraMmEffectSession.StateCallback callback, Handler handler, String key, String value)
            throws CameraEffectHalException;

    /**
     * this function used for get Effect HAL which parameters need
     * 
     * @param outParameters
     * @param outSurface
     *            this is the Camera Client will put the surface, Effect HAL
     *            will get the surface in there,AP can get this for when start
     *            capture to set capture parameters.
     */
    public abstract void getCaputreRequirement(BaseParameters outParameters, List<Surface> outSurface);
    
    /**
     * set the camera client and camera Effect HAL whether need sync the surface and parameters 
     * @param index which surface need 
     * @param sync true means need ;otherwise is false
     * @return if the value is not negative,means success.
     */
    public abstract int setInputsyncMode(int index, boolean sync);
    
    /**
     * set the Framework and camera Effect HAL whether need sync the surface and parameters 
     * @param index which surface need 
     * @param sync true means need ;otherwise is false
     * @return if the value is not negative,means success.
     */
    public abstract int setOutputsyncMode(int index, boolean sync);
    
    /**
     * get current camera client between effect hal have sync mode
     * @param index which surface need to know
     * @return true means is in sync mode, false means Async mode
     */
    public abstract boolean getInputsyncMode(int index);
    
    /**
     * get current camera Framework between effect hal have sync mode
     * @param index which surface need to know
     * @return true means is in sync mode, false means Async mode
     */
    public abstract boolean getOutputsyncMode(int index);

    /**
     * close current effect HAL client
     */
    public abstract void closeEffectHal();

    // A callback objects for receiving updates about the state of a camera
    // Effect Hal.so the session state not need in here
    // AP must extends this StateCallback,so follow event will forward to AP
    // TODO Need improve this
    public static abstract class StateCallback {

        /**
         * An error code that can be reported by {@link #onError} indicating
         * that the camera device is in use already.
         * 
         * <p>
         * This error can be produced when opening the camera fails.
         * </p>
         * 
         * @see #onError
         */
        public static final int ERROR_EFFECT_HAL_IN_USE = 1;

        /**
         * An error code that can be reported by {@link #onError} indicating
         * that the camera device could not be opened because there are too many
         * other open camera devices.
         * 
         * <p>
         * The system-wide limit for number of open cameras has been reached,
         * and more camera devices cannot be opened until previous instances are
         * closed.
         * </p>
         * 
         * <p>
         * This error can be produced when opening the camera fails.
         * </p>
         * 
         * @see #onError
         */
        public static final int ERROR_MAX_EFFECT_IN_USE = 2;

        /**
         * An error code that can be reported by {@link #onError} indicating
         * that the camera device could not be opened due to a device policy.
         * 
         * @see android.app.admin.DevicePolicyManager#setCameraDisabled(android.content.ComponentName,
         *      boolean)
         * @see #onError
         */
        public static final int ERROR_EFFECT_DISABLED = 3;

        /**
         * An error code that can be reported by {@link #onError} indicating
         * that the camera device has encountered a fatal error.
         * 
         * <p>
         * The camera device needs to be re-opened to be used again.
         * </p>
         * 
         * @see #onError
         */
        public static final int ERROR_EFFECT_DEVICE = 4;

        /**
         * An error code that can be reported by {@link #onError} indicating
         * that the camera service has encountered a fatal error.
         * 
         * <p>
         * The Android device may need to be shut down and restarted to restore
         * camera function, or there may be a persistent hardware problem.
         * </p>
         * 
         * <p>
         * An attempt at recovery <i>may</i> be possible by closing the
         * CameraDevice and the CameraManager, and trying to acquire all
         * resources again from scratch.
         * </p>
         * 
         * @see #onError
         */
        public static final int ERROR_EFFECT_SERVICE = 5;

        public static final int ERROR_EFFECT_LISTENER = 6;

        /**
         * The method called when a camera effect has finished opening.
         * 
         * <p>
         * At this point, the camera device is ready to use, and
         * {@link CameraDevice#createCaptureSession} can be called to set up the
         * first capture session.
         * </p>
         * 
         * @param effect
         *            the effect that has become opened
         */
        public abstract void onOpened(CameraMmEffect effect); // Must implement

        public abstract void onPrepared(IEffectHal effect, BaseParameters result);
        
        public abstract void onStated(IEffectHal effect, BaseParameters result);
        
        /**
         * when the session is completed,will notify by effect HAL listener
         * @param effect current effect hal
         * @param partialResult about current session's parameters result 
         * @param uid which session is finished,this is match with started session
         */
        public abstract void onCompleted(IEffectHal effect, BaseParameters partialResult, long uid);
        /**
         * The method called when a camera device has been closed with
         * {@link CameraDevice#close}.
         * 
         * <p>
         * Any attempt to call methods on this CameraDevice in the future will
         * throw a {@link IllegalStateException}.
         * </p>
         * 
         * <p>
         * The default implementation of this method does nothing.
         * </p>
         * 
         * @param effect
         *            the camera device that has become closed
         */
        public void onClosed(CameraMmEffect effect) {
            // Default empty implementation
        }

        /**
         * The method called when a camera device is no longer available for
         * use.
         * 
         * <p>
         * This callback may be called instead of {@link #onOpened} if opening
         * the camera fails.
         * </p>
         * 
         * <p>
         * Any attempt to call methods on this CameraDevice will throw a
         * {@link CameraAccessException}. The disconnection could be due to a
         * change in security policy or permissions; the physical disconnection
         * of a removable camera device; or the camera being needed for a
         * higher-priority use case.
         * </p>
         * 
         * <p>
         * There may still be capture callbacks that are invoked after this
         * method is called, or new image buffers that are delivered to active
         * outputs.
         * </p>
         * 
         * <p>
         * The default implementation logs a notice to the system log about the
         * disconnection.
         * </p>
         * 
         * <p>
         * You should clean up the camera with {@link CameraDevice#close} after
         * this happens, as it is not recoverable until opening the camera again
         * after it becomes
         * {@link CameraManager.AvailabilityCallback#onCameraAvailable
         * available}.
         * </p>
         * 
         * @param effect
         *            the device that has been disconnected
         */
        public abstract void onDisconnected(CameraMmEffect effect); // Must
                                                                    // implement

        /**
         * The method called when a camera device has encountered a serious
         * error.
         * 
         * <p>
         * This callback may be called instead of {@link #onOpened} if opening
         * the camera fails.
         * </p>
         * 
         * <p>
         * This indicates a failure of the camera device or camera service in
         * some way. Any attempt to call methods on this CameraDevice in the
         * future will throw a {@link CameraAccessException} with the
         * {@link CameraAccessException#CAMERA_ERROR CAMERA_ERROR} reason.
         * </p>
         * 
         * <p>
         * There may still be capture completion or camera stream callbacks that
         * will be called after this error is received.
         * </p>
         * 
         * <p>
         * You should clean up the camera with {@link CameraDevice#close} after
         * this happens. Further attempts at recovery are error-code specific.
         * </p>
         * 
         * @param effect
         *            The device reporting the error
         * @param error
         *            The error code, one of the {@code StateCallback.ERROR_*}
         *            values.
         * 
         * @see #ERROR_CAMERA_DEVICE
         * @see #ERROR_CAMERA_SERVICE
         * @see #ERROR_CAMERA_DISABLED
         * @see #ERROR_CAMERA_IN_USE
         */
        public abstract void onError(CameraMmEffect effect, int error); // Must
                                                                        // implement
    }

    @Override
    public abstract void close();
}
