package com.example.defineview;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class SquareImageView extends ImageView {

	public SquareImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public SquareImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SquareImageView(Context context) {
		super(context);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = 0;
		int height = 0;
		
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
	    int widthSize = MeasureSpec.getSize(widthMeasureSpec);
	    int heightMode = MeasureSpec.getMode(heightMeasureSpec);
	    int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		
		//Measure Width
	    if (widthMode == MeasureSpec.EXACTLY
	    		|| widthMode == MeasureSpec.AT_MOST) {
	        width = widthSize;
	    } else {
	        width = Integer.MAX_VALUE;
	    }

	    //Measure Height
	    if (heightMode == MeasureSpec.EXACTLY
	    		|| heightMode == MeasureSpec.AT_MOST) {
	        height = heightSize;
	    } else {
	        height = Integer.MAX_VALUE;
	    }
	    
	    int measureSpec = MeasureSpec.makeMeasureSpec(
	    		Math.min(width, height), MeasureSpec.EXACTLY);
	    super.onMeasure(measureSpec, measureSpec);
	}
}
