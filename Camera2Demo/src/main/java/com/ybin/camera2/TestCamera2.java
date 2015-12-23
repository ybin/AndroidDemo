package com.ybin.camera2;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;

import com.ybin.camera2.camerasvc.Camera2Impl;

import java.util.ArrayList;
import java.util.List;

public class TestCamera2 extends Activity {
    private static final String TAG = "TestCamera2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<Surface> surfaces = new ArrayList<>(2);
        surfaces.add(new Surface(new SurfaceTexture(0)));
        surfaces.add(new Surface(new SurfaceTexture(1)));

        Camera2Impl imple = new Camera2Impl(this, "0", null);
        imple.setSurfaces(surfaces);

        CameraManager mgr = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        mgr.registerAvailabilityCallback(new CameraManager.AvailabilityCallback() {
            @Override
            public void onCameraAvailable(String cameraId) {
                Log.d(TAG, "onCameraAvailable: " + cameraId);
            }
        }, null);
    }
}
