package com.example.imageviewscaletypedemo;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

public class MyArrayAdapter extends ArrayAdapter<Integer> {
	
	private Integer[] mScaleTypes;
	private int mResId;
	private LayoutInflater mInflater;

	public MyArrayAdapter(Context context, int textViewResourceId,
			Integer[] objects) {
		super(context, textViewResourceId, objects);
		
		mResId = textViewResourceId;
		mScaleTypes = objects;
		mInflater = (LayoutInflater)context.getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ScaleType scaleType = null;
		int scaleTypeName = 0;
		Log.v("xxx", "pos: " + position);
		
		switch (mScaleTypes[position].intValue()) {
		case Util.TYPE_CENTER:
			scaleTypeName = R.string.scale_type_center;
			scaleType = ImageView.ScaleType.CENTER;
			break;
		case Util.TYPE_CENTER_CROP:
			scaleTypeName = R.string.scale_type_center_crop;
			scaleType = ImageView.ScaleType.CENTER_CROP;
			break;
		case Util.TYPE_CENTER_INSIDE:
			scaleTypeName = R.string.scale_type_center_inside;
			scaleType = ImageView.ScaleType.CENTER_INSIDE;
			break;
		case Util.TYPE_FIT_CENTER:
			scaleTypeName = R.string.scale_type_fit_center;
			scaleType = ImageView.ScaleType.FIT_CENTER;
			break;
		case Util.TYPE_FIT_END:
			scaleTypeName = R.string.scale_type_fit_end;
			scaleType = ImageView.ScaleType.FIT_END;
			break;
		case Util.TYPE_FIT_START:
			scaleTypeName = R.string.scale_type_fit_start;
			scaleType = ImageView.ScaleType.FIT_START;
			break;
		case Util.TYPE_FIT_XY:
			scaleTypeName = R.string.scale_type_fit_xy;
			scaleType = ImageView.ScaleType.FIT_XY;
			break;
		case Util.TYPE_MATRIX:
			scaleTypeName = R.string.scale_type_matrix;
			scaleType = ImageView.ScaleType.MATRIX;
			break;
		}
		
		if(convertView == null) {
			convertView = mInflater.inflate(mResId, null);
		}
		
		((TextView)convertView.findViewById(R.id.scale_type)).setText(scaleTypeName);
		((ImageView)convertView.findViewById(R.id.big_img)).setScaleType(scaleType);
		((ImageView)convertView.findViewById(R.id.small_img)).setScaleType(scaleType);
		
		return convertView;
	}
}
