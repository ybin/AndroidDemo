package com.ybin.camera2.camerasvc;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import java.lang.reflect.Array;
import java.util.List;

public class Camera2Impl implements CameraService {
    private static final String TAG = "Camera2Impl";

    public static final int STATE_UNAVAILABLE = -1;
    public static final int STATE_AVAILABLE = 0;

    private final String mCameraId;
    private Handler mHandler = null;
    private List<Surface> mSurfaceList;

    private int mDeviceState = STATE_UNAVAILABLE;
    private int mSessionState = STATE_UNAVAILABLE;
    private boolean mNeedStartPreview = false;

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;

    private CameraManager.AvailabilityCallback mAvailabilityCallback
            = new CameraManager.AvailabilityCallback() {
        @Override
        public void onCameraAvailable(String cameraId) {
            Log.d(TAG, "onCameraAvailable " + cameraId);
//            if (mCameraId.equals(cameraId)) {
//                openCamera();
//            }
        }

        @Override
        public void onCameraUnavailable(String cameraId) {
            Log.d(TAG, "onCameraUnavailable " + cameraId);
//            if (mCameraId.equals(cameraId)) {
//                mDeviceState = STATE_UNAVAILABLE;
//                closeCamera();
//            }
        }
    };

    private CameraDevice.StateCallback mCameraDeviceStateCallback
            = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            mDeviceState = STATE_AVAILABLE;
            Log.d(TAG, "onOpened: " + mCameraId);

            createSession();
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            mDeviceState = STATE_UNAVAILABLE;
            Log.d(TAG, "onClosed: CameraDevice closed: " + mCameraId);
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mDeviceState = STATE_UNAVAILABLE;
            Log.d(TAG, "onDisconnected: " + mCameraId);
            camera.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            mDeviceState = STATE_UNAVAILABLE;
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
        public void onConfigured(@NonNull CameraCaptureSession session) {
            mSessionState = STATE_AVAILABLE;
            mCaptureSession = session;
            Log.d(TAG, "onConfigured: session confiured.");

            if (mNeedStartPreview) {
                startPreview();
                mNeedStartPreview = false;
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            mSessionState = STATE_UNAVAILABLE;
            session.close();
            Log.e(TAG, "onConfigureFailed.");
        }

        @Override
        public void onClosed(@NonNull CameraCaptureSession session) {
            mSessionState = STATE_UNAVAILABLE;
            Log.d(TAG, "onClosed: session closed.");
        }
    };

    private void openCamera() {
        try {
            mCameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "openCamera failed: id = " + mCameraId);
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (mDeviceState == STATE_AVAILABLE) {
            mCameraDevice.close();
        }
    }

    private void createSession() {
        try {
            mCameraDevice.createCaptureSession(mSurfaceList, mSessionStateCallback, mHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "createSession failed.");
            e.printStackTrace();
        }
    }

    private CaptureRequest.Builder getRequestBuilder(int type) {
        Log.d(TAG, "getRequestBuilder: " + type);

        CaptureRequest.Builder requestBuilder = null;
        if (mDeviceState != STATE_AVAILABLE) {
            Log.d(TAG, "getRequestBuilder: camera device not available.");
            return null;
        }

        try {
            requestBuilder = mCameraDevice.createCaptureRequest(type);
        } catch (CameraAccessException e) {
            Log.e(TAG, "getRequestBuilder failed with type: " + type);
        }
        return requestBuilder;
    }

    public Camera2Impl(Context context, @NonNull String id, Handler handler) {
        mCameraId = id;
        mHandler = handler;
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        mCameraManager.registerAvailabilityCallback(mAvailabilityCallback, mHandler);


        try {
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics("0");
            List<CameraCharacteristics.Key<?>> keys = characteristics.getKeys();
            for (CameraCharacteristics.Key<?> k : keys) {
                Log.d(TAG, "Camera2Impl: " + k.getName() + " = " + characteristics.get(k));
                if (characteristics.get(k) instanceof int[]) {
                    for (int i :
                            (int[]) characteristics.get(k)) {
                        Log.d(TAG, "Camera2Impl: vvv: " + i);
                    }
                }
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setSurfaces(List<Surface> list) {
        if (mSurfaceList != null && mSurfaceList.equals(list)) {
            return;
        }

        mSurfaceList = list;
        openCamera();
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
    public void release() {
        closeCamera();
        mCameraManager.unregisterAvailabilityCallback(mAvailabilityCallback);
        mCameraManager = null;
    }

    @Override
    public void startPreview() {
        if (mSessionState != STATE_AVAILABLE) {
            mNeedStartPreview = true;
            Log.e(TAG, "startPreview, not configured yet.");
            return;
        }
        CaptureRequest.Builder requestBuilder = getRequestBuilder(CameraDevice.TEMPLATE_PREVIEW);

        // request parameters....................
        requestBuilder.addTarget(mSurfaceList.get(0));

        try {
            mCaptureSession.setRepeatingRequest(requestBuilder.build(), null, mHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "startPreview failed.");
        }
    }

    @Override
    public void stopPreview() {
        if (mSessionState != STATE_AVAILABLE) {
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
        if (mSessionState != STATE_AVAILABLE) {
            Log.e(TAG, "capture, not configured yet.");
            return;
        }
        CaptureRequest.Builder requestBuilder = getRequestBuilder(CameraDevice.TEMPLATE_STILL_CAPTURE);

        // request parameters....................
        requestBuilder.addTarget(mSurfaceList.get(1));
        requestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        requestBuilder.set(CaptureRequest.CONTROL_CAPTURE_INTENT, CaptureRequest.CONTROL_CAPTURE_INTENT_ZERO_SHUTTER_LAG);

        try {
            mCaptureSession.capture(requestBuilder.build(), null, mHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "capture failed.");
        }
    }

    @Override
    public void startBurst() {
        if (mSessionState != STATE_AVAILABLE) {
            Log.e(TAG, "startBurst: not configured yet.");
            return;
        }

        CaptureRequest.Builder requestBuilder = getRequestBuilder(CameraDevice.TEMPLATE_STILL_CAPTURE);
        requestBuilder.addTarget(mSurfaceList.get(1));
        requestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        requestBuilder.set(CaptureRequest.CONTROL_CAPTURE_INTENT, CaptureRequest.CONTROL_CAPTURE_INTENT_ZERO_SHUTTER_LAG);

        try {
            mCaptureSession.setRepeatingRequest(requestBuilder.build(), null, mHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "capture failed.");
        }
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
