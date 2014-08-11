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
		 * ���飺
		 * 		�����漰��'����'����Ĳ���(��ת�����š���б)����Ҫ������ɸò�����
		 * 		Ȼ����ƽ�ơ�
		 * ����ת��ƽ����ϣ����š�ƽ����ϣ���б��ƽ����ϣ���Ϊ��ת�����š���б
		 * ����������(0,0)Ϊ'������'������Ƚ���ƽ�ƣ�����Щ�����Ĳ����ͻ�Ӱ��ƽ��
		 * ��Ч����
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
		// �Ŵ�����
		matrix.setScale(2, 2);
		// ƽ�ƣ��������
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
	 * Canvas�е�rotate, scale, skew, translate����pre-XXXX�ģ��������ҳˣ�
	 * ������ѭ�Ȳ������ƶ���ԭ��ʱ��Ҫ�ȵ���Canvas��rotate(), scale(), skew()��
	 * �ٵ���translate()��˳�����Ҫ��
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
		// ����Ϊscale�ı���
		canvas.scale(2, 2);
		image.draw(canvas);
		canvas.restore();
		
		// skew
		canvas.save();
		canvas.translate(0, 2*intrinsicHeight);
		// ����Ϊ��б�Ƕȵ�tangentֵ
		canvas.skew((float)Math.tan(10*Math.PI/180), 0);
		image.draw(canvas);
		canvas.restore();
	}
}
