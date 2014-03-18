package com.example.aidldemo.client;

import com.example.aidldemo.server.IAIDLServerService;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class ClientActivity extends Activity {
	public static final String TAG = "ClientActivity";
	private IAIDLServerService mService;
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = IAIDLServerService.Stub.asInterface(service);
			try {
				Log.d(TAG, "book name: " + mService.getBook().getBookName()
						+ ", book price: " + mService.getBook().getBookPrice());
			} catch (RemoteException e) {
				Log.d(TAG, "remote exception occured.");
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		bindService(new Intent("com.example.aidldemo.action.aidlservice"),
				mConnection, BIND_AUTO_CREATE);
//		finish();
	}
}
