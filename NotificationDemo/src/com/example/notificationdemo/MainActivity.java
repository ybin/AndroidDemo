package com.example.notificationdemo;

import android.os.Bundle;
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

		mNotificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		sendNormalNotification();
		sendRemoteViewNotification();
	}

	private void sendNormalNotification() {
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
