package com.example.drawabledemo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

public class DrawableView extends View {

public DrawableView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public DrawableView(Context context) {
		super(context);
	}
	
	/*
	 * 每个View背后都有一个隐含的Bitmap对象，参数Canvas
	 * 就是跟该对象关联起来的。自定义View时一定要搞清楚
	 * 三个概念：
	 * 		Content of View, View object, Container of View.
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		// clear canvas
		canvas.drawColor(Color.RED);
		
		// get Drawable object from resource file
		Drawable drawable = getResources().getDrawable(R.drawable.bg);
		// set bound for drawable object, this =MUST= be called
		drawable.setBounds(0, 0, getWidth(), getHeight());
		
		/*
		 * settings by canvas
		 */
		// rotate
		canvas.save();
		canvas.rotate(180f, getWidth()/2.0f, getHeight()/2.0f);
		// draw the Drawable object to screen
		drawable.draw(canvas);
	}
}
