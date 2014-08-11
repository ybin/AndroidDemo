package com.example.defineview;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RelativeLayout;

public class MainActivity extends Activity {

	private static final int VIEW_ID_BASE = 1;
//	private RelativeLayout mContainer;
	private Button[] mChileViews;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// ʹ��RelativeLayout�����Զ����еĲ���
		autowrapLayout();
	}
	
	private void autowrapLayout() {/*
		mChileViews = new Button[10];
		final RelativeLayout container =
				(RelativeLayout) findViewById(R.id.main_relative_layout);
		
		for (int i = 0; i < mChileViews.length; i++) {
			mChileViews[i] = new Button(this);
			mChileViews[i].setId(VIEW_ID_BASE + i);
			mChileViews[i].setText("" + i);
			container.addView(mChileViews[i]);
		}
		
		container.post(new Runnable() {
			@Override
			public void run() {
				// �����ڲ����ǲ���ʹ��local variable�ģ�
				// ����final��local variable����
				reLayout(container);
			}
		});
	*/}

	private void reLayout(RelativeLayout container) {
		int curLineTotalWidth = 0;
		int currentViewWidth;
		
		int containerWidth = container.getMeasuredWidth()
				- container.getPaddingLeft() - container.getPaddingRight();

		Button lineHeadButton = mChileViews[0];

		for (int i = 0; i < mChileViews.length; i++) {
			RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT);

			currentViewWidth = mChileViews[i].getMeasuredWidth();
			
			if (i == 0) {
				// ��һ��view��ֻ����Ȼ���þͺ���
				curLineTotalWidth += currentViewWidth;
				
			} else {
				if (curLineTotalWidth + currentViewWidth > containerWidth) {
					// ��ʼ����
					lp.addRule(RelativeLayout.BELOW, lineHeadButton.getId());
					lineHeadButton = mChileViews[i];
					curLineTotalWidth = currentViewWidth;
				} else {
					// ���軻��
					lp.addRule(RelativeLayout.RIGHT_OF, mChileViews[i-1].getId());
					// ��������baseline
					lp.addRule(RelativeLayout.ALIGN_BASELINE, mChileViews[i-1].getId());
					curLineTotalWidth += currentViewWidth;
				}
			}
			
			mChileViews[i].setLayoutParams(lp);
		}
	}
}
