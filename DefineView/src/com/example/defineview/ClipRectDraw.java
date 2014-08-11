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
 * ---------------------------------------------------矩形区域----------------------
 * --------------------------- canvas.clipRect(左上角x轴坐标, 左上角y轴坐标, 右下角x轴坐标,
 * 右下角y轴坐标, Region.Op.XOR); 最后一个参数有多个选择分别是： //DIFFERENCE是第一次不同于第二次的部分显示出来
 * //REPLACE是显示第二次的 //REVERSE_DIFFERENCE 是第二次不同于第一次的部分显示 //INTERSECT：交集显示
 * //UNION：全部显示 //XOR补集，就是全集的减去交集剩余部分显示
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
		
		// 左上图
		canvas.save();
		canvas.translate(10, 10);
		drawScene(canvas);
		canvas.restore();
		// 右上图
		canvas.save();
		canvas.translate(160, 10);
		canvas.clipRect(10, 10, 90, 90);
		canvas.clipRect(30, 30, 70, 70, Region.Op.XOR);
		drawScene(canvas);
		canvas.restore();
		// 左中图
		canvas.save();
		canvas.translate(10, 130);
		path.reset();
		/* 抛物曲线 */
		path.cubicTo(0, 0, 100, 0, 100, 100);
		path.cubicTo(100, 100, 0, 100, 0, 0);
		canvas.clipPath(path, Region.Op.REPLACE);
		drawScene(canvas);
		canvas.restore();
		// 右中图
		canvas.save();
		canvas.translate(160, 130);
		canvas.clipRect(0, 0, 60, 60);
		canvas.clipRect(40, 40, 100, 100, Region.Op.UNION);
		drawScene(canvas);
		canvas.restore();
		// 左下图
		canvas.save();
		canvas.translate(10, 250);
		canvas.clipRect(0, 0, 60, 60);
		canvas.clipRect(40, 40, 100, 100, Region.Op.XOR);
		drawScene(canvas);
		canvas.restore();
		// 右下图
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
