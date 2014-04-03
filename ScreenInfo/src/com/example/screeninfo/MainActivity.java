package com.example.screeninfo;

import com.example.displayinfo.R;

import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	public static final String TAG = "ScreenInfo";
	
	private TextView mInfoAreaView;
	private DisplayMetrics mDisplayMetric;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mInfoAreaView = (TextView) findViewById(R.id.info_area);
		mDisplayMetric = getResources().getDisplayMetrics();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private void fillDisplayInfo(TextView tv) {
		String info = "screen width: " + mDisplayMetric.widthPixels + "\n"
				    + "screen height: " + mDisplayMetric.heightPixels + "\n"
				    + "screen density: " + mDisplayMetric.density + "\n"
				    + "density dpi: " + mDisplayMetric.densityDpi + "\n"
				    + "scale density: " + mDisplayMetric.scaledDensity + "\n"
				    + "xdpi: " + mDisplayMetric.xdpi + "\n"
				    + "ydpi: " + mDisplayMetric.ydpi;
		
		mInfoAreaView.setText(info);
	}

	public void buttonOnClickListener(View v) {
		Log.v(TAG, "bottom button clicked.");
		fillDisplayInfo(mInfoAreaView);
		//startComponent();
	}
	
	private void startComponent() {
		Intent intent;
		intent = new Intent("cn.zte.music.musicservicecommand.exit");
		intent.setComponent(
				new ComponentName("cn.zte.music", "cn.zte.music.service.MediaPlaybackService"));
		startService(intent);
	}
}
