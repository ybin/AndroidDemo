package com.ybin.opengles.fragments;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ybin.opengles.R;
import com.ybin.opengles.renderers.SimpleRenderer;

public class GLSurfaceViewFragment extends Fragment {
    private static final String TAG = GLSurfaceViewFragment.class.getSimpleName();

    private GLSurfaceView mGLSurfaceView;

    ////////////////// activity creating
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "onAttach ");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate ");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView ");

        if (mGLSurfaceView == null) {
            mGLSurfaceView = new GLSurfaceView(getActivity());
            mGLSurfaceView.setRenderer(new SimpleRenderer());
        }
        return mGLSurfaceView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated ");
    }

    /////////////////// activity resuming

    @Override
    public void onResume() {
        super.onResume();
        mGLSurfaceView.onResume();
        Log.d(TAG, "onResume ");
    }

    ////////////////// activity pausing
    @Override
    public void onPause() {
        super.onPause();
        mGLSurfaceView.onPause();
        Log.d(TAG, "onPause ");
    }

    /////////////////// activity stopping

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop ");
    }

    ///////////////////////// activity destroying

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView ");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy ");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "onDetach ");
    }
}
