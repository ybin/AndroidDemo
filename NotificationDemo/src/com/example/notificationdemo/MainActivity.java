package com.example.notificationdemo;

import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;

public class MainActivity extends Activity {

	public static final String TAG = "NotificationDemo";
	
	private static final int NOTIFICATION_NORMAL = 0;
	private static final int NOTIFICATION_WITH_REMOTEVIEW = 1;
	
	private NotificationManager mNotificationManager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Log.v(TAG, "onCreate::");

		mNotificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		sendNormalNotification();
		sendRemoteViewNotification();
		finish();
	}

	private void sendNormalNotification() {
		/*
		 * PendingIntent里的intent是如何处理的呢？
		 * 	如果PendingIntent是通过getService获取的，就startService(intent)
		 * 	如果PendingIntent是通过getActivity获取的，就startActivity(intent)
		 * 	如果PendingIntent是通过getBroadcast获取的，就sendBroadcast(intent)
		 */
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
				.setWhen(System.currentTimeMillis())
				.setShowWhen(true)
				.setDefaults(Notification.DEFAULT_SOUND)
				.setAutoCancel(true)
				.build();

		mNotificationManager.notify(NOTIFICATION_NORMAL, noti);
	}
	
	private void sendRemoteViewNotification() {
		RemoteViews remote = new RemoteViews(getPackageName(), R.layout.notification_view);
		PendingIntent pIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, MainActivity.class),
				PendingIntent.FLAG_CANCEL_CURRENT);
		remote.setOnClickPendingIntent(R.id.center_button, pIntent);
		
		Notification noti = new Notification.Builder(this)
				.setContent(remote)
				.setSmallIcon(android.R.drawable.stat_notify_chat)
				.setShowWhen(false)
				.build();
		mNotificationManager.notify(NOTIFICATION_WITH_REMOTEVIEW, noti);
	}
}
