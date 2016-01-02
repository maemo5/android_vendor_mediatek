package com.mediatek.mmsdk;

import static com.android.internal.util.Preconditions.checkNotNull;

import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.dispatch.Dispatchable;
import android.hardware.camera2.dispatch.MethodNameInvoker;

public class MmSdkCallbackProxies {

    public static class SessionStateCallbackProxy extends CameraMmEffectSession.StateCallback {

        public final MethodNameInvoker<CameraMmEffectSession.StateCallback> mProxy;
        
        public SessionStateCallbackProxy(Dispatchable<CameraMmEffectSession.StateCallback> dispatchTarget) {
            dispatchTarget = checkNotNull(dispatchTarget, "dispatchTarget must not be null");
            mProxy = new MethodNameInvoker<>(dispatchTarget,CameraMmEffectSession.StateCallback.class);

        }
        
        @Override
        public void onConfigured(CameraMmEffectSession session) {
            mProxy.invoke("onConfigured", session);
        }

        @Override
        public void onConfigureFailed(CameraMmEffectSession session) {
            mProxy.invoke("onConfigureFailed", session);
            
        }

        @Override
        public void onReady(CameraMmEffectSession session) {
            mProxy.invoke("onReady", session);
            
        }

        @Override
        public void onActive(CameraMmEffectSession session) {
            mProxy.invoke("onActive", session);
            
        }

        @Override
        public void onClosed(CameraMmEffectSession session) {
            mProxy.invoke("onClosed", session);
            
        }
        
    }
    
    
    public static class DeviceCaptureCallback extends CameraMmEffectImpl.CaptureCallback {
        private final MethodNameInvoker<CameraMmEffectImpl.CaptureCallback> mProxy;

        public DeviceCaptureCallback(Dispatchable<CameraMmEffectImpl.CaptureCallback> dispatchTarget) {
            dispatchTarget = checkNotNull(dispatchTarget, "dispatchTarget must not be null");
            mProxy = new MethodNameInvoker<>(dispatchTarget,
                    CameraMmEffectImpl.CaptureCallback.class);
        }

        @Override
        public void onCaptureStarted(CameraMmEffect camera, CaptureRequest request, long timestamp,
                long frameNumber) {
            mProxy.invoke("onCaptureStarted", camera, request, timestamp, frameNumber);
        }

        @Override
        public void onCapturePartial(CameraMmEffect camera, CaptureRequest request,
                CaptureResult result) {
            mProxy.invoke("onCapturePartial", camera, request, result);
        }

        @Override
        public void onCaptureProgressed(CameraMmEffect camera, CaptureRequest request,
                CaptureResult partialResult) {
            mProxy.invoke("onCaptureProgressed", camera, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraMmEffect camera, CaptureRequest request,
                TotalCaptureResult result) {
            mProxy.invoke("onCaptureCompleted", camera, request, result);
        }

        @Override
        public void onCaptureFailed(CameraMmEffect camera, CaptureRequest request,
                CaptureFailure failure) {
            mProxy.invoke("onCaptureFailed", camera, request, failure);
        }

        @Override
        public void onCaptureSequenceCompleted(CameraMmEffect camera, int sequenceId,
                long frameNumber) {
            mProxy.invoke("onCaptureSequenceCompleted", camera, sequenceId, frameNumber);
        }

        @Override
        public void onCaptureSequenceAborted(CameraMmEffect camera, int sequenceId) {
            mProxy.invoke("onCaptureSequenceAborted", camera, sequenceId);
        }
    }
}
