package com.ybin.camera2.camera;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import java.util.List;

public class CameraModelImpl implements CameraModel {
    private static final String TAG = CameraModelImpl.class.getSimpleName();

    private Context mContext;
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private CameraManager.AvailabilityCallback mAvailabilityCallback;
    private Handler mHandler = null;
    private List<Surface> mSurfaceList;
    private PreviewDataAvailableCallback mPreviewAvailableCallback;

    private CameraDevice.StateCallback mCameraDeviceStateCallback
            = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) {
            Log.d(TAG, "onOpened " + System.currentTimeMillis());
            mCameraDevice = camera;
            createCaptureSession(mCameraDevice);
        }

        @Override
        public void onClosed(CameraDevice camera) {
            Log.d(TAG, "onClosed: CameraDevice closed." + System.currentTimeMillis());
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.d(TAG, "onDisconnected" + System.currentTimeMillis());
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.d(TAG, "onError" + System.currentTimeMillis());
        }
    };

    private CameraCaptureSession.StateCallback mCaptureSessionStateCallback
            = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(CameraCaptureSession session) {
            Log.d(TAG, "onConfigured: session confiured." + System.currentTimeMillis());
            mCaptureSession = session;
            startPreview();
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            Log.e(TAG, "onConfigureFailed." + System.currentTimeMillis());
        }
    };

    private CameraCaptureSession.CaptureCallback mCaptureSessionCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            Log.d(TAG, "onCaptureCompleted" + System.currentTimeMillis());
            Toast.makeText(mContext, "capture complete.", Toast.LENGTH_LONG).show();
        }
    };

    private CameraCaptureSession.CaptureCallback mPreviewRequestCallback
            = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
            if (frameNumber == 10 && mPreviewAvailableCallback != null) {
                mPreviewAvailableCallback.onPreviewDataAvailable(frameNumber);
            }
        }
    };


    public CameraModelImpl(Context context) {
        mContext = context;
        mAvailabilityCallback = new CameraManager.AvailabilityCallback() {
            @Override
            public void onCameraAvailable(String cameraId) {
                Log.d(TAG, "onCameraAvailable " + cameraId + System.currentTimeMillis());
                super.onCameraAvailable(cameraId);
            }

            @Override
            public void onCameraUnavailable(String cameraId) {
                Log.d(TAG, "onCameraUnavailable " + System.currentTimeMillis());
                super.onCameraUnavailable(cameraId);
            }
        };
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        mCameraManager.registerAvailabilityCallback(mAvailabilityCallback, mHandler);
    }


    public void openCamera(String id) {
        try {
            mCameraManager.openCamera(id, mCameraDeviceStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void stopCamera() {
        mCameraDevice.close();
    }

    public void startPreview() {
        CaptureRequest.Builder requestBuilder;
        try {
            requestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            if (requestBuilder == null) {
                Log.e(TAG, "startPreview null pointer.");
                return;
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "startPreview exception.");
            e.printStackTrace();
            return;
        }

        requestBuilder.addTarget(mSurfaceList.get(0));

        try {
            mCaptureSession.setRepeatingRequest(requestBuilder.build(), mPreviewRequestCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void capture() {
        CaptureRequest.Builder requestBuilder;

        try {
            requestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            if (requestBuilder == null) {
                Log.d(TAG, "capture request builder null.");
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.d(TAG, "capture request builder exception.");
            return;
        }

        // request parametersss....................
        requestBuilder.addTarget(mSurfaceList.get(1));

        try {
            mCaptureSession.capture(requestBuilder.build(), mCaptureSessionCaptureCallback, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "capture capture request exception");
            e.printStackTrace();
        }
    }

    private void createCaptureSession(CameraDevice device) {
        try {
            device.createCaptureSession(mSurfaceList, mCaptureSessionStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void setSurfaceList(List<Surface> list) {
        mSurfaceList = list;
    }

    public void setPreviewAvailableCallback(PreviewDataAvailableCallback callback) {
        mPreviewAvailableCallback = callback;
    }

    public interface PreviewDataAvailableCallback {
        void onPreviewDataAvailable(long frameNumber);
    }
}
