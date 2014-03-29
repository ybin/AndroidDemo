package com.example.defineview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class CanvasOperations extends View {

	public static final String TAG = "CanvasOperations";
	
	public CanvasOperations(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		Log.v(TAG, "canvas size: " + canvas.getWidth() + "," + canvas.getHeight());
		
		Drawable image = getResources().getDrawable(R.drawable.ic_launcher);
		int intrinsicWidth = image.getIntrinsicWidth();
		int intrinsicHeight = image.getIntrinsicHeight();
		
		Log.v(TAG, "imgage size: " + intrinsicWidth + "," + intrinsicHeight);
		
		image.setBounds(0, 0, intrinsicWidth, intrinsicHeight);
		// background
		canvas.drawColor(Color.DKGRAY);
		// normal image
		image.draw(canvas);
		// rotation
		canvas.save();
		Paint pRed = new Paint();
		Paint pBlue = new Paint();
		pRed.setColor(Color.RED);
		pBlue.setColor(Color.BLUE);
		canvas.drawCircle(10, 10, 10, pRed);
		canvas.rotate(90, getWidth()/2, getHeight()/2);
		image.draw(canvas);
		canvas.drawCircle(10, 10, 10, pBlue);
		canvas.restore();
		// translation
		canvas.save();
		canvas.translate(0, intrinsicHeight);
		image.draw(canvas);
		canvas.restore();
		// scale
		canvas.save();
		canvas.translate(2*intrinsicWidth, 2*intrinsicHeight);
		// 参数为scale的倍数
		canvas.scale(2, 2, intrinsicWidth, intrinsicHeight);
		image.draw(canvas);
		canvas.restore();
		// skew
		canvas.save();
		canvas.translate(0, 2*intrinsicHeight);
		// 参数为倾斜角度的tangent值
		canvas.skew(1, 0);
		image.draw(canvas);
		canvas.restore();
	}
}
