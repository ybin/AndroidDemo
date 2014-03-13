package com.example.appwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

public class AppWidgetDemo extends AppWidgetProvider {

	public static final String TAG = "AppWidgetDemo";

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		Log.v(TAG, "appWidgetIds: " + appWidgetIds.length);
		
		int length = appWidgetIds.length;
		for(int i = 0; i < length; i++) {
			int id = appWidgetIds[i];
			RemoteViews views = new RemoteViews(context.getPackageName(),
					R.layout.widget_main_layout);
			appWidgetManager.updateAppWidget(id, views);
		}
	}
}
