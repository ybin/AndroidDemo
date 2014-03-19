package com.example.defineview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class CustomView extends View {

	private String mText;
	private int mFontColor;
	private int mFontSize;

	private Paint mPaint;

	public CustomView(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.CustomView);

		try {
			mText = a.getString(R.styleable.CustomView_text);
			mFontColor = a.getColor(R.styleable.CustomView_fontColor,
					android.R.color.black);
			mFontSize = a.getDimensionPixelOffset(R.styleable.CustomView_fontSize, 20);
		} finally {
			a.recycle();
		}

		mPaint = new Paint();
		//mPaint.setTextSize(16 * getResources().getDisplayMetrics().density);
		mPaint.setTextSize(mFontSize);
		System.out.println("text, ascent, descent: " + mPaint.descent() + ", " + mPaint.ascent());
		System.out.println("text: " + mText + ", color: " + mFontColor + ", size: " + mFontSize);
		System.out.println("xx: " + mPaint.measureText("xxxxxxxxxxxxxxxxxxxx"));
		System.out.println("xx: " + mPaint.measureText("MMMMMMMMMM"));

	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(measureWidth(widthMeasureSpec),
				measureHeight(heightMeasureSpec));
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
			result = (int)(mPaint.measureText(mText) + getPaddingLeft() + getPaddingRight());
			if(mode == MeasureSpec.AT_MOST) {
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
			result = (int)(mFontSize + getPaddingTop() + getPaddingBottom());
			if(mode == MeasureSpec.AT_MOST) {
				result = Math.min(result, size);
			}
			break;
		}
		}
		return result;
	}

}
