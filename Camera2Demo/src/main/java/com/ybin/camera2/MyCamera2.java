package com.ybin.camera2;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageView;

import com.ybin.camera2.camera.CameraModelImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MyCamera2 extends Activity {
    private static final String TAG = MyCamera2.class.getSimpleName();

    private TextureView mTextView;
    private ImageView mPreviewCover;
    private CameraModelImpl mCameraModel;
    private TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.d(TAG, "onSurfaceTextureAvailable() called with " + "surface = [" + surface
                    + "], width = [" + width + "], height = [" + height + "]");
            surface.setDefaultBufferSize(width, height);
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            reader.setOnImageAvailableListener(mReaderListener, null);
            List<Surface> list = new ArrayList<>(2);
            list.add(new Surface(surface));
            list.add(reader.getSurface());
            mCameraModel.setSurfaceList(list);
            mCameraModel.open();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Log.d(TAG, "onSurfaceTextureSizeChanged() called with " +
                    "surface = [" + surface + "], width = [" + width + "], height = [" + height + "]");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.d(TAG, "onSurfaceTextureDestroyed() called with " + "surface = [" + surface + "]");
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    private CameraModelImpl.PreviewDataAvailableCallback mPreviewAvailableCallback
            = new CameraModelImpl.PreviewDataAvailableCallback() {
        @Override
        public void onPreviewDataAvailable(long frameNumber) {
//            mPreviewCover.setVisibility(View.GONE);
            mPreviewCover.animate().setDuration(500).translationX(1080).start();
        }
    };

    ImageReader.OnImageAvailableListener mReaderListener = new ImageReader.OnImageAvailableListener() {
        final File file = new File(Environment.getExternalStorageDirectory() + "/DCIM",
                "pic_" + System.currentTimeMillis() + ".jpg");
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            try {
                image = reader.acquireLatestImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);
                save(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if(image != null) {
                    image.close();
                }
            }
        }

        private void save(byte[] bytes) throws IOException {
            OutputStream output = null;
            try {
                output = new FileOutputStream(file);
                output.write(bytes);
            } finally {
                if (output != null) {
                    output.close();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_camera_layout);

        mCameraModel = new CameraModelImpl(this, "0");
        mTextView = (TextureView) findViewById(R.id.preview);
        mTextView.setSurfaceTextureListener(mSurfaceTextureListener);
        mPreviewCover = (ImageView) findViewById(R.id.privew_cover);
        mCameraModel.setPreviewAvailableCallback(mPreviewAvailableCallback);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                mCameraModel.capture();
                Log.d(TAG, "onKeyDown over");
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraModel.close();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        Log.d(TAG, "dispatchTouchEvent " + ev.getAction());
        return super.dispatchTouchEvent(ev);
    }
}
