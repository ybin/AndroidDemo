package com.example.appwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * һ��widget���Ա����õ����������ϣ�����һ��������Ҳ���� ���ö��ͬһ���͵�widget�� �������ÿ�������϶�����һ�����ֲ�������widget��
 * ����������Ҫ��һ��id����ʾһ��widget.
 * 
 * widget֧�ֶ�ʱ���ܣ����Զ����������ݣ���Ȼ���Թرն�ʱ�� ����ʹ�ñ�ķ�ʽ���ж�ʱ��
 */
public class AppWidgetDemo extends AppWidgetProvider {

	public static final String TAG = "AppWidgetDemo";

	private static final String MEDIA_PLAYBACK_SERVICE = "cn.zte.music.musicservicecommand.startmusic";
	private static final String PLAY_INTENT = "com.example.appwidget.PLAY";
	private static final String NEXT_INTENT = "com.example.appwidget.NEXT";
	private static final String PREV_INTENT = "com.example.appwidget.PREV";
	private static final String COMMAND = "command";
	private static final String PLAY_CMD = "play";
	private static final String PAUSE_CMD = "pause";
	private static final String NEXT_CMD = "next";
	private static final String PREV_CMD = "previous";
	// ÿ��ִ����onReceive()֮��object�������ˣ�
	// ���Ա���Ҫ����Ϊstatic��
	private static RemoteViews mRemoteViews;
	private static boolean mPlaying;

	// -------------- AppWidgetProvider -------------//

	/* ������һ��widgetʱ����� */
	@Override
	public void onEnabled(Context context) {
		Log.v(TAG, "onEnabled()");
		// start music service
		Intent i = new Intent(MEDIA_PLAYBACK_SERVICE);
		context.startService(i);
	}

	/* ɾ�����һ��widgetʱ����� */
	@Override
	public void onDisabled(Context context) {
		Log.v(TAG, "onDisabled()");
		super.onDisabled(context);
	}

	/* ��ʱ����widgetʱ���� */
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Log.v(TAG, "onUpdate(), widget number: " + appWidgetIds.length);

		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_main_layout);
		views.setOnClickPendingIntent(R.id.control_play,
				PendingIntent.getBroadcast(context, 0, new Intent(PLAY_INTENT), 0));
		views.setOnClickPendingIntent(R.id.control_next,
				PendingIntent.getBroadcast(context, 0, new Intent(NEXT_INTENT), 0));
		views.setOnClickPendingIntent(R.id.control_prev,
				PendingIntent.getBroadcast(context, 0, new Intent(PREV_INTENT), 0));
		// save remote view, used in click listeners
		mRemoteViews = views;
		// all widgets use the same view
		appWidgetManager.updateAppWidget(appWidgetIds, views);
	}

	/* widget��С�ı�ʱ���� */
	@Override
	public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
			int appWidgetId, Bundle newOptions) {
		Log.v(TAG, "onAppWidgetOptionsChanged()");
		super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
	}

	/*
	 * widget�����еĶ�������ͨ������intent��ִ�еģ�����Ļص�����
	 * Ҳ��һ���ģ�����onReceive()������һ��Ҫ����super.onReceive()�� ���ᴦ���Ӧ��intent���Ӷ������������ô������
	 * ������һ�� AppWidgetProvider�࣬����򵥡�
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.v(TAG, "onReceive(" + intent.getAction() + ")");
		super.onReceive(context, intent);

		String action = intent.getAction();
		if (PLAY_INTENT.equals(action)) {
			play(context);
		} else if (NEXT_INTENT.equals(action)) {
			next(context);
		} else if (PREV_INTENT.equals(action)) {
			prev(context);
		}
	}

	// -------------- AppWidgetProvider -------------//

	// -------------- Operations -------------//
	private void play(Context context) {
		Log.v(TAG, "playing: " + mPlaying);
		Intent i = new Intent(MEDIA_PLAYBACK_SERVICE);
		
		if (mPlaying) {
			mPlaying = false;
			i.putExtra(COMMAND, PAUSE_CMD);
			mRemoteViews.setImageViewResource(R.id.control_play,
					R.drawable.ic_widget_music_play);
		} else {
			mPlaying = true;
			i.putExtra(COMMAND, PLAY_CMD);
			mRemoteViews.setImageViewResource(R.id.control_play,
					R.drawable.ic_widget_music_pause);
		}
		AppWidgetManager manager = AppWidgetManager.getInstance(context);
		int[] widgetIds = manager.getAppWidgetIds(
				new ComponentName(context, AppWidgetDemo.class));
		manager.updateAppWidget(widgetIds, mRemoteViews);
		context.startService(i);
	}

	private void next(Context context) {
		Intent i = new Intent(MEDIA_PLAYBACK_SERVICE);
		i.putExtra(COMMAND, NEXT_CMD);
		context.startService(i);
	}

	private void prev(Context context) {
		Intent i = new Intent(MEDIA_PLAYBACK_SERVICE);
		i.putExtra(COMMAND, PREV_CMD);
		context.startService(i);
	}
	// -------------- Operations -------------//

}
