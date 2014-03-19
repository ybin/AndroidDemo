package com.example.notificationdemo;

import android.os.Bundle;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		sendNotification();
//		finish();
	}

	private void sendNotification() {
		NotificationManager notiManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		long when = System.currentTimeMillis() + 7000;
		PendingIntent pIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, MainActivity.class),
				PendingIntent.FLAG_CANCEL_CURRENT);
		Notification noti = new Notification.Builder(this)
				.setContentTitle("New mail!")
				.setContentText("There is a new mail!")
				.setSmallIcon(android.R.drawable.stat_notify_chat)
				.setLargeIcon(BitmapFactory.decodeResource(getResources(),
						android.R.drawable.stat_notify_chat))
				.setContentIntent(pIntent)
				.setWhen(when)
				.setShowWhen(true)
				.setDefaults(Notification.DEFAULT_SOUND)
				.setAutoCancel(true)
				.build();

		notiManager.notify(0, noti);
	}
}
