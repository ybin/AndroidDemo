package com.example.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

public class MainActivity extends Activity {

	public static final String TAG = "DialogDemo";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	public void showMyDialogs(View view) {
		int id = view.getId();

		switch (id) {
		case R.id.dialog_1: {
			Dialog dialog = new AlertDialog.Builder(this)
				.setTitle("Dialog title")
				.setIcon(R.drawable.ic_launcher)
				.setMessage("Dialog message...")
				.create();
			dialog.show();
			break;
		}
		case R.id.dialog_2: {
			Dialog dialog = new AlertDialog.Builder(this)
				.setTitle("Dialog title")
				.setIcon(R.drawable.ic_launcher)
				.setMessage("Dialog message...")
				.setPositiveButton("OK", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// do something
					}
				})
				.setNegativeButton("Cancel", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// do something
					}
				})
				.setNeutralButton("Middle", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// do something
					}
				})
				.create();
			dialog.show();
			break;
		}
		case R.id.dialog_3: {
			// create a simple view as dialog content
			View v = LayoutInflater.from(this).inflate(R.layout.simple_dialog_content_view, null);
			
			Dialog dialog = new AlertDialog.Builder(this)
				.setTitle("Dialog title")
				.setIcon(R.drawable.ic_launcher)
				.setView(v)
				.setPositiveButton("OK", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// do something
					}
				})
				.setNegativeButton("Cancel", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// do something
					}
				})
				.setNeutralButton("Middle", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// do something
					}
				})
				.create();
			dialog.show();
			break;
		}
		case R.id.dialog_4: {
			String[] choiceItems = new String[] {
				"item 1",
				"item 2",
				"item 3",
				"item 4"
			};
			
			Dialog dialog = new AlertDialog.Builder(this)
				.setTitle("Dialog title")
				.setIcon(R.drawable.ic_launcher)
				.setSingleChoiceItems(choiceItems, -1, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Log.v(TAG, "single choice dialog, selected item: " + which);
						dialog.dismiss();
					}
				})
				.setPositiveButton("OK", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				})
				.setNegativeButton("Cancel", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				})
				.setNeutralButton("Middle", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// do something
					}
				})
				.create();
			
			// cancel on touch outside
			dialog.setCanceledOnTouchOutside(false);
			// cancel on hit BACK key
			dialog.setCancelable(false);
			
			dialog.show();
			break;
		}
		case R.id.dialog_5: {
			String[] choiceItems = new String[] {
				"item 1",
				"item 2",
				"item 3",
				"item 4"
			};
			final boolean[] multichoiceResult = new boolean[choiceItems.length];
			for(int i = 0; i < multichoiceResult.length; i++) {
				multichoiceResult[i] = false;
			}
			
			Dialog dialog = new AlertDialog.Builder(this)
				.setTitle("Dialog title")
				.setIcon(R.drawable.ic_launcher)
				.setMultiChoiceItems(choiceItems, null, new OnMultiChoiceClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which, boolean isChecked) {
						Log.v(TAG, "multichoice clicked: "
								+ "which:" + which + ", checked:" + isChecked);
						multichoiceResult[which] = isChecked;
					}
				})
				.setPositiveButton("OK", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// do something with the result
						StringBuilder sb = new StringBuilder();
						for(int i = 0; i < multichoiceResult.length; i++) {
							sb.append("which: " + i + ", checked: " + multichoiceResult[i] + "\n");
						}
						Log.v(TAG, sb.toString());
					}
				})
				.setNegativeButton("Cancel", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// drop the result down, do nothing
						Log.v(TAG, "drop down the result.");
					}
				})
				.create();
			dialog.show();
			break;
		}
		}
	}
}
