package com.example.defineview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.View;

/**
 * ---------------------------------------------------��������----------------------
 * --------------------------- canvas.clipRect(���Ͻ�x������, ���Ͻ�y������, ���½�x������,
 * ���½�y������, Region.Op.XOR); ���һ�������ж��ѡ��ֱ��ǣ� //DIFFERENCE�ǵ�һ�β�ͬ�ڵڶ��εĲ�����ʾ����
 * //REPLACE����ʾ�ڶ��ε� //REVERSE_DIFFERENCE �ǵڶ��β�ͬ�ڵ�һ�εĲ�����ʾ //INTERSECT��������ʾ
 * //UNION��ȫ����ʾ //XOR����������ȫ���ļ�ȥ����ʣ�ಿ����ʾ
 * 
 * @author emmet1988.iteye.com
 * 
 */
public class ClipRectDraw extends View {

	Context context;
	Paint paint;
	Path path;

	public ClipRectDraw(Context context) {
		super(context);
		init();
	}

	public ClipRectDraw(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public ClipRectDraw(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setStrokeWidth(5);
		paint.setTextSize(15);
		paint.setTextAlign(Paint.Align.RIGHT);
		path = new Path();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawColor(Color.GRAY);
		
		canvas.clipRect(0, 0, 100, 100);
		canvas.drawColor(Color.RED);
		canvas.clipRect(0, 0, 50, 50, Region.Op.UNION);
		canvas.drawColor(Color.BLUE);
		
		// ����ͼ
		canvas.save();
		canvas.translate(10, 10);
		drawScene(canvas);
		canvas.restore();
		// ����ͼ
		canvas.save();
		canvas.translate(160, 10);
		canvas.clipRect(10, 10, 90, 90);
		canvas.clipRect(30, 30, 70, 70, Region.Op.XOR);
		drawScene(canvas);
		canvas.restore();
		// ����ͼ
		canvas.save();
		canvas.translate(10, 130);
		path.reset();
		/* �������� */
		path.cubicTo(0, 0, 100, 0, 100, 100);
		path.cubicTo(100, 100, 0, 100, 0, 0);
		canvas.clipPath(path, Region.Op.REPLACE);
		drawScene(canvas);
		canvas.restore();
		// ����ͼ
		canvas.save();
		canvas.translate(160, 130);
		canvas.clipRect(0, 0, 60, 60);
		canvas.clipRect(40, 40, 100, 100, Region.Op.UNION);
		drawScene(canvas);
		canvas.restore();
		// ����ͼ
		canvas.save();
		canvas.translate(10, 250);
		canvas.clipRect(0, 0, 60, 60);
		canvas.clipRect(40, 40, 100, 100, Region.Op.XOR);
		drawScene(canvas);
		canvas.restore();
		// ����ͼ
		canvas.translate(160, 250);
		canvas.clipRect(0, 0, 60, 60);
		canvas.clipRect(40, 40, 100, 100, Region.Op.REVERSE_DIFFERENCE);
		drawScene(canvas);
		canvas.restore();
	}

	private void drawScene(Canvas canvas) {
//		canvas.clipRect(0, 0, 100, 100);
//		canvas.drawColor(Color.WHITE);
//
//		paint.setColor(Color.RED);
//		canvas.drawLine(0, 0, 100, 100, paint);
//
//		paint.setColor(Color.GREEN);
//		canvas.drawCircle(30, 70, 30, paint);
//
//		paint.setColor(Color.BLUE);
//		canvas.drawText("ChenJianLi", 100, 30, paint);
	}

}
