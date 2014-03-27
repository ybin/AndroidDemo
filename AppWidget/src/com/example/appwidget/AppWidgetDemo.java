package com.example.appwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.sax.StartElementListener;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * 一个widget可以被放置到过个桌面上，甚至一个桌面上也可以 放置多个同一类型的widget，
 * 如可以在每个桌面上都放置一个音乐播放器的widget，
 * 所以我们需要用一个id来表示一个widget.
 * 
 * widget支持定时功能，以自动更新其内容，当然可以关闭定时， 或者使用别的方式进行定时。
 */
public class AppWidgetDemo extends AppWidgetProvider {

	public static final String TAG = "AppWidgetDemo";
	
	private static final String MEDIA_PLAYBACK_SERVICE =
			"cn.zte.music.musicservicecommand.startmusic";
	private static final String PLAY_INTENT = "com.example.appwidget.PLAY";
	private static final String NEXT_INTENT = "com.example.appwidget.NEXT";
	private static final String PREV_INTENT = "com.example.appwidget.PREV";
	private Context mContext;

	// -------------- AppWidgetProvider -------------//

	/* 启动第一个widget时会调用 */
	@Override
	public void onEnabled(Context context) {
		Log.v(TAG, "onEnabled()");
		// start music service
		Intent i = new Intent(MEDIA_PLAYBACK_SERVICE);
		context.startService(i);
	}

	/* 删除最后一个widget时会调用 */
	@Override
	public void onDisabled(Context context) {
		Log.v(TAG, "onDisabled()");
		super.onDisabled(context);
	}

	/* 定时更新widget时调用 */
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		Log.v(TAG, "onUpdate(), widget number: " + appWidgetIds.length);

		int length = appWidgetIds.length;
		for (int i = 0; i < length; i++) {
			int id = appWidgetIds[i];
			RemoteViews views = new RemoteViews(context.getPackageName(),
					R.layout.widget_main_layout);
			views.setOnClickPendingIntent(R.id.control_play,
					PendingIntent.getBroadcast(
							context, 0, new Intent(PLAY_INTENT), 0));
			views.setOnClickPendingIntent(R.id.control_next,
					PendingIntent.getBroadcast(
							context, 0, new Intent(NEXT_INTENT), 0));
			views.setOnClickPendingIntent(R.id.control_prev,
					PendingIntent.getBroadcast(
							context, 0, new Intent(PREV_INTENT), 0));
			
			appWidgetManager.updateAppWidget(id, views);
		}
	}

	/* widget大小改变时调用 */
	@Override
	public void onAppWidgetOptionsChanged(Context context,
			AppWidgetManager appWidgetManager, int appWidgetId,
			Bundle newOptions) {
		Log.v(TAG, "onAppWidgetOptionsChanged()");
		super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId,
				newOptions);
	}

	/*
	 * widget中所有的动作都是通过接收intent来执行的，上面的回调函数
	 * 也是一样的，所以onReceive()函数中一定要调用super.onReceive()，
	 * 它会处理对应的intent，从而调用上面的那么函数。
	 * 不妨看一下 AppWidgetProvider类，及其简单。
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.v(TAG, "onReceive(" + intent.getAction() + ")");
		super.onReceive(context, intent);
		mContext = context;
		
		String action = intent.getAction();
		if(PLAY_INTENT.equals(action)) {
			play();
		} else if(NEXT_INTENT.equals(action)) {
			next();
		} else if(PREV_INTENT.equals(action)) {
			prev();
		}
	}

	// -------------- AppWidgetProvider -------------//

	
	// -------------- Operations -------------//
	private void play() {
		Intent i = new Intent(MEDIA_PLAYBACK_SERVICE);
		i.putExtra("command", "play");
		mContext.sendBroadcast(i);
	}
	
	private void next() {
		Intent i = new Intent(MEDIA_PLAYBACK_SERVICE);
		i.putExtra("command", "next");
		mContext.sendBroadcast(i);
	}
	
	private void prev() {
		Intent i = new Intent(MEDIA_PLAYBACK_SERVICE);
		i.putExtra("command", "prev");
		mContext.sendBroadcast(i);
	}
	// -------------- Operations -------------//
	
}
