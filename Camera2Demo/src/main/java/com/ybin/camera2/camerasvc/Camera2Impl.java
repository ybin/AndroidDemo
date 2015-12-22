package com.ybin.camera2.camerasvc;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Surface;

import java.util.List;

public class Camera2Impl implements CameraService {
    private static final String TAG = "Camera2Impl";

    public static final int SERVICE_UNAVAILABLE = -1;
    public static final int SERVICE_AVAILABLE = 0;

    private String mCameraId;
    private Handler mHandler = null;
    private List<Surface> mSurfaceList;

    private int mServiceState = SERVICE_UNAVAILABLE;
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;

    private CameraManager.AvailabilityCallback mAvailabilityCallback
            = new CameraManager.AvailabilityCallback() {
        @Override
        public void onCameraAvailable(@NonNull String cameraId) {
            Log.d(TAG, "onCameraAvailable " + cameraId);
            if (mCameraId != null && mCameraId.equals(cameraId)) {
                openCamera();
            }
        }

        @Override
        public void onCameraUnavailable(@NonNull String cameraId) {
            Log.d(TAG, "onCameraUnavailable " + cameraId);
            mServiceState = SERVICE_UNAVAILABLE;
            closeCamera();
        }
    };

    private CameraDevice.StateCallback mCameraDeviceStateCallback
            = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG, "onOpened: " + mCameraId);
            mCameraDevice = camera;
            createCaptureSession(mCameraDevice);
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            mServiceState = SERVICE_UNAVAILABLE;
            Log.d(TAG, "onClosed: CameraDevice closed: " + mCameraId);
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mServiceState = SERVICE_UNAVAILABLE;
            Log.d(TAG, "onDisconnected: " + mCameraId);
            camera.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            mServiceState = SERVICE_UNAVAILABLE;
            Log.d(TAG, "onError: " + mCameraId + ", error: " + error);
            camera.close();
        }
    };

    private CameraCaptureSession.StateCallback mSessionStateCallback
            = new CameraCaptureSession.StateCallback() {
        @Override
        public void onReady(@NonNull CameraCaptureSession session) {}

        @Override
        public void onActive(@NonNull CameraCaptureSession session) {}

        @Override
        public void onClosed(@NonNull CameraCaptureSession session) {
            Log.d(TAG, "onClosed: session closed.");
            mServiceState = SERVICE_AVAILABLE;
        }

        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            mServiceState = SERVICE_AVAILABLE;
            Log.d(TAG, "onConfigured: session confiured.");
            mCaptureSession = session;
            startPreview();
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            mServiceState = SERVICE_UNAVAILABLE;
            Log.e(TAG, "onConfigureFailed.");
            session.close();
        }
    };

    private CameraCaptureSession.CaptureCallback mCaptureRequestCallback
            = new CameraCaptureSession.CaptureCallback() {};

    private CameraCaptureSession.CaptureCallback mPreviewRequestCallback
            = new CameraCaptureSession.CaptureCallback() {};

    //@@@@@@@@@@@@@@@@@@@   @@@@@@@@@@@@@@@@@//
    private void openCamera() {
        try {
            mCameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "openCamera failed: id = " + mCameraId);
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void createCaptureSession(CameraDevice device) {
        try {
            device.createCaptureSession(mSurfaceList, mSessionStateCallback, mHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "createCaptureSession failed.");
            e.printStackTrace();
        }
    }

    @Nullable
    private CaptureRequest.Builder getRequestBuilder(int type) {
        Log.d(TAG, "getRequestBuilder: " + type);
        CaptureRequest.Builder requestBuilder = null;
        try {
            requestBuilder = mCameraDevice.createCaptureRequest(type);
        } catch (CameraAccessException e) {
            Log.e(TAG, "getRequestBuilder failed with type: " + type);
        }
        return requestBuilder;
    }

    public Camera2Impl(Context context) {
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }









    //@@@@@@@@@@@@@@@@@@@   @@@@@@@@@@@@@@@@@//

    @Override
    public void setSurfaces(List<Surface> list) {
        mSurfaceList = list;
    }

    @Override
    public String[] getCameraIdList() {
        try {
            return mCameraManager.getCameraIdList();
        } catch (CameraAccessException e) {
            Log.e(TAG, "getCameraIdList failed.");
        }
        return null;
    }

    @Override
    public void setCameraId(String id) {
        mCameraId = id;
    }

    @Override
    public void init(String id, List<Surface> surfaces, Handler handler) {
        Log.d(TAG, "init: id=" + id);
        mCameraId = id;
        mSurfaceList = surfaces;
        mHandler = handler;

        mCameraManager.registerAvailabilityCallback(mAvailabilityCallback, mHandler);
    }

    @Override
    public void release() {
        closeCamera();
        mCameraManager.unregisterAvailabilityCallback(mAvailabilityCallback);
        mCameraManager = null;
    }

    @Override
    public void startPreview() {
        if (mServiceState != SERVICE_AVAILABLE) {
            Log.e(TAG, "startPreview, not configured yet.");
            return;
        }

        CaptureRequest.Builder requestBuilder = getRequestBuilder(CameraDevice.TEMPLATE_PREVIEW);
        if (requestBuilder == null) {
            Log.d(TAG, "startPreview: getRequestBuilder failed.");
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

    @Override
    public void stopPreview() {
        if (mServiceState != SERVICE_AVAILABLE) {
            Log.e(TAG, "stopPreview, not configured yet.");
            return;
        }
        try {
            mCaptureSession.stopRepeating();
        } catch (CameraAccessException e) {
            Log.e(TAG, "stopPreview failed.");
        }
    }

    @Override
    public void capture() {
        if (mServiceState != SERVICE_AVAILABLE) {
            Log.e(TAG, "capture, not configured yet.");
            return;
        }
        CaptureRequest.Builder requestBuilder = getRequestBuilder(CameraDevice.TEMPLATE_STILL_CAPTURE);
        if (requestBuilder == null) {
            Log.d(TAG, "capture: getRequestBuilder failed.");
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

    @Override
    public void startBurst() {

    }

    @Override
    public void stopBurst() {

    }

    @Override
    public void startRecording() {

    }

    @Override
    public void stopRecording() {

    }

    @Override
    public boolean setParameterAsync() {
        return false;
    }

    @Override
    public void getSupportedParameterAsync() {

    }
}
