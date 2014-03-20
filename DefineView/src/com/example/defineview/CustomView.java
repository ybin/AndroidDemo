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
		// setTextSize()������Ҫ��PixelsΪ��λ�Ĳ���
		// mPaint.setTextSize(16 * getResources().getDisplayMetrics().density);
		mPaint.setTextSize(mFontSize);
		mPaint.setColor(mFontColor);

		System.out.println("text ascent and descent: " + mPaint.descent() + ", " + mPaint.ascent());
		System.out.println("text: " + mText + ", color: " + mFontColor + ", size: " + mFontSize);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = measureWidth(widthMeasureSpec);
		int height = measureHeight(heightMeasureSpec, width - getPaddingLeft() - getPaddingRight());
		
		setMeasuredDimension(width, height);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if(mBackground != null) {
			mBackground.setBounds(0, 0, getWidth(), getHeight());
			mBackground.draw(canvas);
		}
		// ���������ԭ������baseline�����������Ͻ�
		// �������Զ����е����
		//canvas.drawText(mText, 0, -mPaint.ascent(), mPaint);
		
		// �����Զ����е����
		drawText(canvas);
	}
	
	// һ��һ�еĻ����ı���ע�⣺û�п����ı��д��ڻ��з������
	private void drawText(Canvas canvas) {
		int start = 0;
		int end;
		float lineHeight = mPaint.descent() - mPaint.ascent();
		int lineWidth = getWidth() - getPaddingLeft() - getPaddingRight();
		float y = getPaddingTop() - mPaint.ascent();
		
		while((end = findEndIndex(start, lineWidth)) != -1) {
			canvas.drawText(mText, start, end, 0, y, mPaint);
			start = end;
			y += lineHeight;
		}
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

	private int measureHeight(int spec, int lineWidth) {
		int fontHeight = (int)(mPaint.descent() - mPaint.ascent());
		int lineCount = countLines((int)mPaint.measureText(mText), lineWidth);
		
		int contentHeight = 0;
		int mode = MeasureSpec.getMode(spec);
		int size = MeasureSpec.getSize(spec);

		switch (mode) {
		case MeasureSpec.EXACTLY: {
			contentHeight = size;
			break;
		}
		case MeasureSpec.UNSPECIFIED:
		case MeasureSpec.AT_MOST: {
			contentHeight = fontHeight * lineCount + getPaddingTop() + getPaddingBottom();
			if (mode == MeasureSpec.AT_MOST) {
				contentHeight = Math.min(contentHeight, size);
			}
			break;
		}
		}
		return contentHeight;
	}
	
	private int countLines(int contentWidth, int lineWidth) {
		int count = 0;
		
		// ���ּ��㷽�����������Ϊû�п�����β��ʣ��ռ��޷�����һ���ַ������
		//count = (contentWidth + lineWidth - 1) / lineWidth;
		
		// ���ǵ���β��ʣ��ռ��޷�����һ���ַ������
		int start = 0;
		int end;
		while((end = findEndIndex(start, lineWidth)) != -1) {
			count += 1;
			start = end;
		}
		return count == 0 ? 1 : count;
	}
	
	/*
	 *  ���ַ���Ч�ʱȽϵͣ���Ҫ�Ż���������Դ�����һ�е��ַ�����
	 *  ��Ϊ�ο�ֵ��Ȼ��end��ֵ����ʵ��������ӻ��߼��١�
	 */
	private int findEndIndex(int start, int lineWidth) {
		int end = start;
		int textLength = mText.length();
		
		while(end < textLength 
				&& (int)mPaint.measureText(mText, start, end + 1) <= lineWidth) {
			end += 1;
		}
		
		if(end == start) {
			return -1;
		}
		
		return end;
	}
}
