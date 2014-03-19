package com.example.playbackground;

import cn.zte.music.service.IMediaPlaybackService;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.View;

public class MainActivity extends Activity implements ServiceConnection {

	public static final String TAG = "PlayMusicBackground";
	
	public static final String MEDIA_PLAYBACK_SERVICE = "cn.zte.music.musicservicecommand.startmusic";
	private IMediaPlaybackService mService;
	private boolean mServiceStarted;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		// start music service
		Intent i = new Intent(MEDIA_PLAYBACK_SERVICE);
		bindService(i, this, Service.BIND_AUTO_CREATE);
	}

	@Override
	protected void onStop() {
		// stop music service
		unbindService(this);
		super.onStop();
	}

	// onClick listener of 'play' button
	public void play(View v) {
		if(!mServiceStarted) {
			return;
		}
		
		try {
			mService.play();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	// onClick listener of 'pause' button
	public void pause(View v) {
		if(!mServiceStarted) {
			return;
		}
		
		try {
			mService.pause();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	//------------------- ServiceConnection -------------------//
	@Override
	public void onServiceConnected(ComponentName name, IBinder binder) {
		Log.v(TAG, "onServiceConnected()");
		mService = IMediaPlaybackService.Stub.asInterface(binder);
		mServiceStarted = true;
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		Log.v(TAG, "onServiceDisconnected()");
		mService = null;
		mServiceStarted = false;
	}
	//------------------- ServiceConnection -------------------//
	
}
