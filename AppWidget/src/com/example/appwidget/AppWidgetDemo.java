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
 * һ��widget���Ա����õ����������ϣ�����һ��������Ҳ���� ���ö��ͬһ���͵�widget��
 * �������ÿ�������϶�����һ�����ֲ�������widget��
 * ����������Ҫ��һ��id����ʾһ��widget.
 * 
 * widget֧�ֶ�ʱ���ܣ����Զ����������ݣ���Ȼ���Թرն�ʱ�� ����ʹ�ñ�ķ�ʽ���ж�ʱ��
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

	/* widget��С�ı�ʱ���� */
	@Override
	public void onAppWidgetOptionsChanged(Context context,
			AppWidgetManager appWidgetManager, int appWidgetId,
			Bundle newOptions) {
		Log.v(TAG, "onAppWidgetOptionsChanged()");
		super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId,
				newOptions);
	}

	/*
	 * widget�����еĶ�������ͨ������intent��ִ�еģ�����Ļص�����
	 * Ҳ��һ���ģ�����onReceive()������һ��Ҫ����super.onReceive()��
	 * ���ᴦ���Ӧ��intent���Ӷ������������ô������
	 * ������һ�� AppWidgetProvider�࣬����򵥡�
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
