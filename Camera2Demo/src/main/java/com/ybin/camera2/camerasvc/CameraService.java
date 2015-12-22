package com.ybin.camera2.camerasvc;

import android.os.Handler;
import android.view.Surface;

import java.util.List;

interface CameraService {
    void init(String id, List<Surface> surfaces, Handler handler);
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
    void setCameraId(String id);
    String[] getCameraIdList();
    void setSurfaces(List<Surface> surfaces);
}
