package com.mediatek.mmsdk;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Handler;

import com.mediatek.mmsdk.BaseParameters;
import com.mediatek.mmsdk.CameraMmEffect;
import com.mediatek.mmsdk.CameraMmEffectImpl;
import com.mediatek.mmsdk.CameraMmEffectSessionImpl;

import java.util.List;


public abstract class CameraMmEffectSession  implements AutoCloseable {

    /**
     * get the camera effect that the session is created
     */
    public abstract CameraMmEffectImpl getEffect();
    
    public abstract void start(int index, BaseParameters ps,
            CaptureCallback callback, Handler handler,
            long timeLapse, boolean repeating);

    /**
     * set the inputFrameParameters for which[timestamp] frame and which
     * surface[index] about the parameters to the frame.
     * 
     * @param index
     * @param baseParameters
     * @param timestamp
     * @param repeating
     */
    public abstract void addInputFrameParameter(int index, BaseParameters baseParameters,
            long timestamp, boolean repeating);

    
    /**
     * abort this time capture
     * 
     * @param baseParameters
     *            current this parameters is tell Effect HAL whether need to
     *            merge the picture,such as MAV/Panorama/MT why not use a
     *            boolean,because in future maybe have other needs
     */
    public abstract void abort(BaseParameters baseParameters);

    public abstract void closeSession();

    /**
     * A callback for receiving updates about the state of a camera effect session.
     *
     */
    public static abstract class StateCallback {
        
        public abstract void onConfigured(CameraMmEffectSession session);
        
        public abstract void onConfigureFailed(CameraMmEffectSession session);
        
        public abstract void onReady(CameraMmEffectSession session);
        
        public abstract void onActive(CameraMmEffectSession session);
        
        public abstract void onClosed(CameraMmEffectSession session);
    }
    
    /**
     * <p>A callback object for tracking the progress of a {@link CaptureRequest} submitted to the
     * camera device.</p>
     *
     * <p>This callback is invoked when a request triggers a capture to start,
     * and when the capture is complete. In case on an error capturing an image,
     * the error method is triggered instead of the completion method.</p>
     */
    public static abstract class CaptureCallback {
        
        public abstract void onCaptureStarted(CameraCaptureSession session, CaptureRequest request,
                long timestamp, long frameNumber);

        public abstract void onCaptureStarted(CameraCaptureSession session, CaptureRequest request,
                long timestamp);

        public abstract void onCapturePartial(CameraCaptureSession session, CaptureRequest request,
                CaptureResult result);

        public abstract void onCaptureProgressed(CameraCaptureSession session,
                CaptureRequest request, CaptureResult partialResult);

        public abstract void onCaptureCompleted(CameraCaptureSession session,
                CaptureRequest request, TotalCaptureResult result);

        public abstract void onCaptureFailed(CameraCaptureSession session, CaptureRequest request,
                CaptureFailure failure);

        public abstract void onCaptureSequenceCompleted(CameraCaptureSession session,
                int sequenceId, long frameNumber);

        public abstract void onCaptureSequenceAborted(CameraCaptureSession session, int sequenceId);
    }
    
    
    @Override
    public abstract void close();

}

