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
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity implements ServiceConnection {

	public static final String TAG = "PlayMusicBackground";
	
	public static final String MEDIA_PLAYBACK_SERVICE =
			"cn.zte.music.musicservicecommand.startmusic";
	private IMediaPlaybackService mService;
	private boolean mServiceStarted;
	private EditText mMusicText;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mMusicText = (EditText) findViewById(R.id.music_name);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		startMusicService();
	}

	@Override
	protected void onStop() {
		// stop music service
		unbindService(this);
		super.onStop();
	}
	
	private void startMusicService() {
		// start music service
		Intent i = new Intent(MEDIA_PLAYBACK_SERVICE);
		bindService(i, this, Service.BIND_AUTO_CREATE);
	}
	

	//------------------- button listeners -------------------//
	// onClick listener of 'play' button
	public void play(View v) {
		Log.v(TAG, "service started? " + mServiceStarted);
		if(!mServiceStarted) {
			return;
		}
		
		try {
			mService.play();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	
	// onClick listener of 'play special' button
	public void playSpecial(View v) {
		if("".equals(mMusicText.getText().toString())) {
			Toast.makeText(MainActivity.this,
					"Are you kidding me?!", Toast.LENGTH_SHORT).show();
			return;
		}
		
		String track = mMusicText.getText().toString();
		Log.v(TAG, "track: " + track);
		try {
			mService.openFile(track);
			mService.play();
		} catch (RemoteException e) {
			Log.v(TAG, "open file error.");
			e.printStackTrace();
		}
	}
	// onClick listener of 'next' button
	public void next(View v) {
		if(!mServiceStarted) {
			return;
		}
		
		try {
			mService.next();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	// onClick listener of 'prev' button
	public void prev(View v) {
		if(!mServiceStarted) {
			return;
		}
		
		try {
			mService.prev();
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
	
	// onClick listener of 'stop' button
	// Attention!!!
	public void stop(View v) {
		if(!mServiceStarted) {
			return;
		}
		
		try {
			mService.stop();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	//------------------- button listeners -------------------//
	

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
