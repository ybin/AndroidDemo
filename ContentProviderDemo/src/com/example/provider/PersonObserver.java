package com.example.provider;

import android.database.ContentObserver;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class PersonObserver extends ContentObserver {

	public static final String TAG = "PersonObserver";
	private Handler handler;
	
	public PersonObserver(Handler handler) {
		super(handler);
		this.handler = handler;
	}
	
	@Override
	public void onChange(boolean selfChange) {
		super.onChange(selfChange);
		Log.i(TAG, "data changed, try to requery.");
		//��handler������Ϣ,���²�ѯ��¼
		Message msg = new Message();
		handler.sendMessage(msg);
	}
}
