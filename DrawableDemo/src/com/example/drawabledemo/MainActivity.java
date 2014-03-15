package com.example.drawabledemo;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
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

	/*
	 * 利用Canvas把一个Drawable对象写入Bitmap对象中，
	 * 然后把Bitmap保存到文件中。
	 * 概念解释：
	 * Drawable: Android中对'可绘制'对象的统称，其内容可能
	 * 			 是一个png, jpg图片，也可能是其他内容(见API)
	 * Bitmap: '可绘制'对象在内存中的表示
	 * Canvas: 操作'可绘制'对象的工具，如draw line, draw circle，
	 * 		   	甚至是draw bitmap等，每个Canvas对象背后都有一个
	 * 		 	Bitmap对象作为数据的最终存储容器。每个View都有一个
	 * 		 	隐含的Bitmap对象，onDraw(Canvas)函数中的Canvas
	 * 			就是跟这个隐含的Bitmap对象关联的。
	 */
	public void inverseImage(Drawable d, String savedFile) {
		Log.v(TAG, "save file: " + savedFile);
		
		Bitmap bm = Bitmap.createBitmap(
				d.getIntrinsicWidth(),
				d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bm);
		// rotate canvas
		canvas.rotate(180f,
				d.getIntrinsicWidth()/2f, d.getIntrinsicHeight()/2f);
		d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
		d.draw(canvas);
		// save bitmap to file
		convertBitmapToFile(bm, savedFile);
	}
	
	// Drawable -> Bitmap
	private Bitmap convertDrawableToBitmap(Drawable d) {
		Bitmap bm = Bitmap.createBitmap(d.getIntrinsicWidth(),
				d.getIntrinsicHeight(),
				Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bm);
		d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
		d.draw(canvas);
		return bm;
	}
	
	// Bitmap -> Drawable
	private Drawable convertBitmapToDrawable(Resources res, Bitmap bm) {
		return new BitmapDrawable(res, bm);
	}
	
	// Bitmap -> File
	private void convertBitmapToFile(Bitmap bm, String fpath) {
		try {
			OutputStream out = new FileOutputStream(fpath);
			bm.compress(CompressFormat.PNG, 100, out);
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// File -> Bitmap
	private Bitmap convertFileToBitmap(String fpath) {
		return BitmapFactory.decodeFile(fpath);
	}
	
	// Resource -> Bitmap
	private Bitmap vonvertResourceToBitmap(Resources res, int resId) {
		return BitmapFactory.decodeResource(res, resId);
	}
	
	// Bitmap -> byte[]
	private byte[] convertBitmapToByteArray(Bitmap bm) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		bm.compress(CompressFormat.PNG, 100, out);
		return out.toByteArray();
	}
	
	// byte[] -> Bitmap
	private Bitmap convertByteArrayToBitmap(byte[] bytes) {
		return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
	}
}
