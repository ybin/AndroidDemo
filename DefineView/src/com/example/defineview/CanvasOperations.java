package com.example.defineview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
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
	
		/*
		 * 建议：
		 * 		凡是涉及到'物体'本身的操作(旋转、缩放、倾斜)，都要首先完成该操作，
		 * 		然后再平移。
		 * 如旋转、平移组合，缩放、平移组合，倾斜、平移组合，因为旋转、缩放、倾斜
		 * 操作都是以(0,0)为'不动点'，如果先进行平移，那这些后续的操作就会影响平移
		 * 的效果。
		 */
		operationWithCanvs(canvas, getResources().getDrawable(R.drawable.ic_launcher));
//		operationWithMatrix(canvas);
	}
	
	private void operationWithMatrix(Canvas canvas) {
		Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
		Matrix matrix = new Matrix();
		int bmWidth = bitmap.getWidth();
		int bmHeight = bitmap.getHeight();
		Log.v(TAG, "imgage size: " + bmWidth + "," + bmHeight);
		
		// background
		canvas.drawColor(Color.LTGRAY);
		
		// normal image
		matrix.reset();
		canvas.drawBitmap(bitmap, matrix, null);
		
		// rotate
		canvas.save();
		matrix.reset();
		matrix.postRotate(90);
		matrix.postTranslate(2*bmWidth, 0);
		canvas.drawBitmap(bitmap, matrix, null);
		canvas.restore();
		
		// translate
		canvas.save();
		matrix.reset();
		matrix.setTranslate(0, bmHeight);
		canvas.drawBitmap(bitmap, matrix, null);
		canvas.restore();
		
		// scale
		canvas.save();
		matrix.reset();
		// 放大两倍
		matrix.setScale(2, 2);
		// 平移，矩阵左乘
		matrix.postTranslate(bmWidth, bmHeight);
		canvas.drawBitmap(bitmap, matrix, null);
		canvas.restore();
		
		// skew
		canvas.save();
		matrix.reset();
		matrix.postSkew((float)Math.tan(10*Math.PI/180), 0);
		matrix.postTranslate(0, 2*bmHeight);
		canvas.drawBitmap(bitmap, matrix, null);
		canvas.restore();
	}
	
	/*
	 * Canvas中的rotate, scale, skew, translate都是pre-XXXX的，即矩阵右乘，
	 * 所以遵循先操作后移动的原则时，要先调用Canvas的rotate(), scale(), skew()，
	 * 再调用translate()，顺序很重要。
	 */
	private void operationWithCanvs(Canvas canvas, Drawable image) {
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
		canvas.translate(2*intrinsicWidth, 0);
		canvas.rotate(90);
		image.draw(canvas);
		canvas.restore();
		
		// translation
		canvas.save();
		canvas.translate(0, intrinsicHeight);
		image.draw(canvas);
		canvas.restore();
		
		// scale
		canvas.save();
		canvas.translate(intrinsicWidth, intrinsicHeight);
		// 参数为scale的倍数
		canvas.scale(2, 2);
		image.draw(canvas);
		canvas.restore();
		
		// skew
		canvas.save();
		canvas.translate(0, 2*intrinsicHeight);
		// 参数为倾斜角度的tangent值
		canvas.skew((float)Math.tan(10*Math.PI/180), 0);
		image.draw(canvas);
		canvas.restore();
	}
}
