package com.example.drawabledemo;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

public class MainActivity extends Activity {
	
	public static final String TAG = "DrawableDemo";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		inverseImage(getResources().getDrawable(R.drawable.bg),
				Environment.getExternalStoragePublicDirectory(
						Environment.DIRECTORY_MOVIES) + "/inverse.png");
	}

	public void inverseImage(Drawable d, String savedFile) {
		Log.v(TAG, "save file: " + savedFile);
		
		// create result bitmap
		Bitmap bitmap = Bitmap.createBitmap(
				d.getIntrinsicWidth(),
				d.getIntrinsicHeight(),
				Bitmap.Config.ARGB_8888);
		// create canvas used to draw Drawable object to bitmap
		Canvas canvas = new Canvas(bitmap);
		// rotate canvas
		canvas.rotate(180f,
				d.getIntrinsicWidth()/2f, d.getIntrinsicHeight()/2f);
		
		// set bound, this =MUST= be called
		d.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		// draw Drawable object to bitmap
		d.draw(canvas);
		
		// save bitmap to file
		try {
			OutputStream out = new FileOutputStream(savedFile);
			bitmap.compress(CompressFormat.PNG, 100, out);
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			Log.v(TAG, "save file: error");
			e.printStackTrace();
		} catch (IOException e) {
			Log.v(TAG, "save file: error");
			e.printStackTrace();
		}
	}
}
