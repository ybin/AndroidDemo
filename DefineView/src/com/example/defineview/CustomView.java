package com.example.defineview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

public class CustomView extends View {

	private String mText;
	private int mFontColor;
	private int mFontSize;
	private Drawable mBackground;

	private Paint mPaint;

	public CustomView(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomView);

		try {
			mText = a.getString(R.styleable.CustomView_text);
			mFontColor = a.getColor(R.styleable.CustomView_fontColor, android.R.color.black);
			
			int bgResId = a.getResourceId(R.styleable.CustomView_background, 0);
			System.out.println("bg id: " + bgResId);
			if(bgResId != 0) {
				mBackground = context.getResources().getDrawable(bgResId);
				System.out.println("mBackground: " + mBackground);
			}
			// dimension in Pixels
			mFontSize = a.getDimensionPixelOffset(R.styleable.CustomView_fontSize, 60);
		} finally {
			a.recycle();
		}
		mPaint = new Paint();
		// setTextSize()函数需要以Pixels为单位的参数
		// mPaint.setTextSize(16 * getResources().getDisplayMetrics().density);
		mPaint.setTextSize(mFontSize);
		mPaint.setColor(mFontColor);

		System.out.println("text ascent and descent: " + mPaint.descent() + ", " + mPaint.ascent());
		System.out.println("text: " + mText + ", color: " + mFontColor + ", size: " + mFontSize);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if(mBackground != null) {
			mBackground.setBounds(0, 0, getWidth(), getHeight());
			mBackground.draw(canvas);
		}
		// 字体的纵向原点是其baseline，而不是左上角
		canvas.drawText(mText, 0, -mPaint.ascent(), mPaint);
	}

	private int measureWidth(int spec) {
		int result = 0;
		int mode = MeasureSpec.getMode(spec);
		int size = MeasureSpec.getSize(spec);

		switch (mode) {
		case MeasureSpec.EXACTLY: {
			result = size;
			break;
		}
		case MeasureSpec.UNSPECIFIED:
		case MeasureSpec.AT_MOST: {
			result = (int) (mPaint.measureText(mText) + getPaddingLeft() + getPaddingRight());
			if (mode == MeasureSpec.AT_MOST) {
				result = Math.min(result, size);
			}
			break;
		}
		}
		return result;
	}

	private int measureHeight(int spec) {
		int result = 0;
		int mode = MeasureSpec.getMode(spec);
		int size = MeasureSpec.getSize(spec);

		switch (mode) {
		case MeasureSpec.EXACTLY: {
			result = size;
			break;
		}
		case MeasureSpec.UNSPECIFIED:
		case MeasureSpec.AT_MOST: {
			result = (int) ((mPaint.descent() - mPaint.ascent()) + getPaddingTop() + getPaddingBottom());
			if (mode == MeasureSpec.AT_MOST) {
				result = Math.min(result, size);
			}
			break;
		}
		}
		return result;
	}
}
