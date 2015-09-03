package com.ybin.opengles.fragments;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ybin.opengles.renderers.SimpleRenderer;

public class GLSurfaceViewFragment extends Fragment {
    private static final String TAG = GLSurfaceViewFragment.class.getSimpleName();

    private GLSurfaceView mGLSurfaceView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mGLSurfaceView == null) {
            mGLSurfaceView = new GLSurfaceView(getActivity());
            mGLSurfaceView.setRenderer(new SimpleRenderer());
        }
        return mGLSurfaceView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mGLSurfaceView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mGLSurfaceView.onPause();
    }
}
