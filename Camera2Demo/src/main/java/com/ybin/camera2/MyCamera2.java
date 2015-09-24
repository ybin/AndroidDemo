package com.ybin.camera2;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
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
    private ImageReader mImageReader;
    private List<Surface> mSurfaceList = new ArrayList<>(2);
    private TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.d(TAG, "onSurfaceTextureAvailable() called with " + "surface = [" + surface
                    + "], width = [" + width + "], height = [" + height + "]");
            surface.setDefaultBufferSize(width, height);
            mSurfaceList.add(0, new Surface(surface));

            mImageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            mImageReader.setOnImageAvailableListener(mReaderListener, null);
            mSurfaceList.add(1, mImageReader.getSurface());

            mCameraModel.setSurfaceList(mSurfaceList);
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
//            mPreviewCover.setTranslationX(0);
            mPreviewCover.animate().setDuration(300).translationX(1080).start();
        }
    };

    ImageReader.OnImageAvailableListener mReaderListener = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            try {
                image = reader.acquireLatestImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);

                File file = new File(Environment.getExternalStorageDirectory() + "/DCIM/Camera",
                        "pic_" + System.currentTimeMillis() + ".jpg");
                save(bytes, file);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if(image != null) {
                    image.close();
                }
            }
        }

        private void save(byte[] bytes, File file) throws IOException {
            Log.d(TAG, "save() called with " + "bytes = [" + bytes + "], file = [" + file + "]");
            OutputStream output = null;
            try {
                output = new FileOutputStream(file);
                output.write(bytes);
            } catch (Exception e) {
                Log.e(TAG, "save error.");
                e.printStackTrace();
            } finally {
                if (output != null) {
                    output.close();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_camera_layout);

        mCameraModel = new CameraModelImpl(this, "0");
        mTextView = (TextureView) findViewById(R.id.preview);
        mTextView.setSurfaceTextureListener(mSurfaceTextureListener);
        mPreviewCover = (ImageView) findViewById(R.id.privew_cover);
        mCameraModel.setPreviewAvailableCallback(mPreviewAvailableCallback);

        Button capture = (Button) findViewById(R.id.capture_button);
        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "do capture.");
                mCameraModel.capture();
            }
        });
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        if (mTextView.isAvailable()) {
            mCameraModel.open();
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        mCameraModel.close();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        mCameraModel.release();
        mImageReader.close();
    }
}
