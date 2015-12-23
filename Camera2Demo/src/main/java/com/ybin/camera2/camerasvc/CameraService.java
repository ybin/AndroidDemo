package com.ybin.camera2.camerasvc;

import android.view.Surface;

import java.util.List;

interface CameraService {
    void release();
    void startPreview();
    void stopPreview();
    void capture();
    void startBurst();
    void stopBurst();
    void startRecording();
    void stopRecording();
    boolean setParameterAsync();
    void getSupportedParameterAsync();
    String[] getCameraIdList();
    void setSurfaces(List<Surface> surfaces);
}
