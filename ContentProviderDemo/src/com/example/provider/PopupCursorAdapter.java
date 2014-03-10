package com.example.provider;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SimpleCursorAdapter;

public class PopupCursorAdapter extends SimpleCursorAdapter {
	public static final String TAG = "PopupCursorAdapter";
	
	private int mPopupWindowWidth;
	private PopupWindow mPopup;

	public PopupCursorAdapter(Context context, int layout, Cursor c,
			String[] from, int[] to, int flags) {
		super(context, layout, c, from, to, flags);
		Log.v(TAG, "PopupCursorAdapter::constructor");
		
		mPopupWindowWidth =
				context.getResources().getDisplayMetrics().widthPixels / 2;
		
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout contentView = (LinearLayout) inflater.inflate(
				R.layout.popup_content, null);
//		contentView.setRotation(180);
		mPopup = new PopupWindow(contentView, mPopupWindowWidth,
				LayoutParams.WRAP_CONTENT, true);
		mPopup.setTouchable(true);
		mPopup.setOutsideTouchable(true);
		mPopup.setBackgroundDrawable(new BitmapDrawable());
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		super.bindView(view, context, cursor);

		view.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				Log.v(TAG, "onLongClick()");
				showPopupContent(v);
				return true;
			}
		});
	}

	private void showPopupContent(View v) {
		Log.v(TAG, "showPopupContent()");

		mPopup.showAsDropDown(v, (v.getWidth() - mPopupWindowWidth) / 2, 5);
	}
}
