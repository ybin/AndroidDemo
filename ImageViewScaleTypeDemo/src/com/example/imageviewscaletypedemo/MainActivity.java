package com.example.imageviewscaletypedemo;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.widget.ListView;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		((ListView) findViewById(R.id.list_view)).setAdapter(
				new MyArrayAdapter(this, R.layout.list_item, getData()));
	}

	private Integer[] getData() {
		return new Integer[] {
				Integer.valueOf(Util.TYPE_CENTER),
				Integer.valueOf(Util.TYPE_CENTER_CROP),
				Integer.valueOf(Util.TYPE_CENTER_INSIDE),
				Integer.valueOf(Util.TYPE_MATRIX),
				Integer.valueOf(Util.TYPE_FIT_START),
				Integer.valueOf(Util.TYPE_FIT_CENTER),
				Integer.valueOf(Util.TYPE_FIT_END),
				Integer.valueOf(Util.TYPE_FIT_XY),
		};
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
