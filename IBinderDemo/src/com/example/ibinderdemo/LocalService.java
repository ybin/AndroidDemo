package com.example.ibinderdemo;

import java.util.Random;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class LocalService extends Service {
	private final IBinder mBinder = new LocalBinder();
	private final Random mGenerator = new Random();
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	public int getRandomNumber() {
		return mGenerator.nextInt(100);
	}

	public class LocalBinder extends Binder {
		LocalService getService() {
			return LocalService.this;
		}
	}
}
