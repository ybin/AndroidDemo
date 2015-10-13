package com.ybin.camera2.camera;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Surface;

import java.util.List;

public class CameraModelImpl implements CameraModel {
    private static final String TAG = CameraModelImpl.class.getSimpleName();

    public static final int STATE_CODE = 0;
    public static final int STATE_CAMERA_DEVICE_OPENED = STATE_CODE + 1;
    public static final int STATE_CAMERA_DEVICE_CLOSED = STATE_CODE + 2;
    public static final int STATE_SESSION_CONFIGURED = STATE_CODE + 3;
    public static final int STATE_SESSION_CONFIGURE_FAILED = STATE_CODE + 4;

    private String mCameraID;
    private int mState = STATE_CODE;
    private Handler mHandler = null;
    private List<Surface> mSurfaceList;
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private PreviewDataAvailableCallback mPreviewAvailableCallback;

    /**
     * callback object for camera availability.
     */
    private CameraManager.AvailabilityCallback mAvailabilityCallback
            = new CameraManager.AvailabilityCallback() {
        @Override
        public void onCameraAvailable(String cameraId) {
            Log.d(TAG, "onCameraAvailable " + cameraId + ", " + System.currentTimeMillis());
        }

        @Override
        public void onCameraUnavailable(String cameraId) {
            Log.d(TAG, "onCameraUnavailable " + System.currentTimeMillis());
        }
    };

    /**
     * callback object for CameraDevice state.
     */
    private CameraDevice.StateCallback mCameraDeviceStateCallback
            = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mState = STATE_CAMERA_DEVICE_OPENED;
            Log.d(TAG, "onOpened " + System.currentTimeMillis());
            mCameraDevice = camera;
            createCaptureSession(mCameraDevice);
        }

        @Override
        public void onClosed(CameraDevice camera) {
            mState = STATE_CAMERA_DEVICE_CLOSED;
            Log.d(TAG, "onClosed: CameraDevice closed." + System.currentTimeMillis());
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            mState = STATE_CAMERA_DEVICE_CLOSED;
            Log.d(TAG, "onDisconnected" + System.currentTimeMillis());
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            mState = STATE_CAMERA_DEVICE_CLOSED;
            Log.d(TAG, "onError" + System.currentTimeMillis());
        }
    };

    /**
     * callback object for CameraCaptureSession state.
     */
    private CameraCaptureSession.StateCallback mSessionStateCallback
            = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            mState = STATE_SESSION_CONFIGURED;
            Log.d(TAG, "onConfigured: session confiured." + System.currentTimeMillis());
            mCaptureSession = session;
            startPreview();
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            mState = STATE_SESSION_CONFIGURE_FAILED;
            Log.e(TAG, "onConfigureFailed." + System.currentTimeMillis());
            session.close();
        }
    };

    /**
     * callback object for capture request.
     */
    private CameraCaptureSession.CaptureCallback mCaptureRequestCallback
            = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session,
                                       CaptureRequest request, TotalCaptureResult result) {
            Log.d(TAG, "onCaptureCompleted" + System.currentTimeMillis());
        }
    };

    /**
     * callback object for preview request.
     */
    private CameraCaptureSession.CaptureCallback mPreviewRequestCallback
            = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(CameraCaptureSession session,
                                     CaptureRequest request, long timestamp, long frameNumber) {
            if (frameNumber == 10 && mPreviewAvailableCallback != null) {
                mPreviewAvailableCallback.onPreviewDataAvailable(frameNumber);
            }
        }
    };

    private void createCaptureSession(CameraDevice device) {
        try {
            device.createCaptureSession(mSurfaceList, mSessionStateCallback, mHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "createCaptureSession failed.");
        }
    }

    @Nullable
    private CaptureRequest.Builder getRequestBuilder(int type) {
        Log.d(TAG, "getRequestBuilder() called with " + "type = [" + type + "]");

        CaptureRequest.Builder requestBuilder = null;
        try {
            requestBuilder = mCameraDevice.createCaptureRequest(type);
            if (requestBuilder == null) {
                Log.e(TAG, "get null request build with type: " + type);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "getRequestBuilder failed with type: " + type);
        }
        return requestBuilder;
    }


    /**
     * @param context context for obtaining CameraManager reference, do not
     *                hold context reference.
     */
    public CameraModelImpl(Context context, String cameraId) {
        mCameraID = cameraId;
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        mCameraManager.registerAvailabilityCallback(mAvailabilityCallback, mHandler);
    }

    public void setSurfaceList(List<Surface> list) {
        mSurfaceList = list;
    }

    public String[] getCameraIdList() {
        try {
            return mCameraManager.getCameraIdList();
        } catch (CameraAccessException e) {
            Log.e(TAG, "getCameraIdList failed.");
        }
        return null;
    }

    /**
     * open camera include:
     *  1. open camera via CameraManager
     *  2. create capture session
     */
    public void open() {
        try {
            mCameraManager.openCamera(mCameraID, mCameraDeviceStateCallback, mHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "openCamera failed.");
        }
    }

    public void close() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
        }
    }

    /**
     * Call this when release CameraModel object.
     * CameraManager reference will be held in the whole lifetime of CameraModel.
     */
    public void release() {
        mCameraManager.unregisterAvailabilityCallback(mAvailabilityCallback);
        mCameraManager = null;
    }

    public void startPreview() {
        if (mState != STATE_SESSION_CONFIGURED) {
            Log.e(TAG, "startPreview, not configured yet.");
            return;
        }

        CaptureRequest.Builder requestBuilder = getRequestBuilder(CameraDevice.TEMPLATE_PREVIEW);
        if (requestBuilder == null) {
            Log.e(TAG, "startPreview failed with request builder being null.");
            return;
        }

        // request parameters....................
        requestBuilder.addTarget(mSurfaceList.get(0));

        try {
            mCaptureSession.setRepeatingRequest(requestBuilder.build(), mPreviewRequestCallback, mHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "startPreview failed.");
        }
    }

    public void stopPreview() {
        try {
            mCaptureSession.stopRepeating();
        } catch (CameraAccessException e) {
            Log.e(TAG, "stopPreview failed.");
        }
    }

    public void capture() {
        if (mState != STATE_SESSION_CONFIGURED) {
            Log.e(TAG, "capture, not configured yet.");
            return;
        }

        CaptureRequest.Builder requestBuilder = getRequestBuilder(CameraDevice.TEMPLATE_STILL_CAPTURE);
        if (requestBuilder == null) {
            Log.e(TAG, "capture failed with request builder being null.");
            return;
        }

        // request parameters....................
        requestBuilder.addTarget(mSurfaceList.get(1));
        requestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        requestBuilder.set(CaptureRequest.CONTROL_CAPTURE_INTENT, CaptureRequest.CONTROL_CAPTURE_INTENT_ZERO_SHUTTER_LAG);

        try {
            mCaptureSession.capture(requestBuilder.build(), mCaptureRequestCallback, mHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "capture failed.");
        }
    }

    ///////////////
    public void setPreviewAvailableCallback(PreviewDataAvailableCallback callback) {
        mPreviewAvailableCallback = callback;
    }

    public interface PreviewDataAvailableCallback {
        void onPreviewDataAvailable(long frameNumber);
    }
}
