package com.example.provider;

import java.util.ArrayList;

import com.example.util.T;

import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class MainActivity extends Activity implements LoaderCallbacks<Cursor> {
	
	public static final String TAG = "ContentProviderDemo";

	private ContentResolver mResolver;

	private static final int LOADER_ID_0 = 0;
	private SimpleCursorAdapter mCursorAdapter;
	private int mPopupWindowWidth;
	private PopupWindow mPopup;
	private Handler mHandler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "onCreate() in " + T.__FILE__());
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mResolver = getContentResolver();

		mHandler = new Handler() {
			public void handleMessage(Message msg) {
				Log.v(TAG, "content changed.");
				getLoaderManager().restartLoader(LOADER_ID_0, null, MainActivity.this);
				requery();
			};
		};
		
		// 为PERSON_ALL_URI注册变化通知，数据有变动会收到通知，然后我们更新cursor
		getContentResolver().registerContentObserver(
				PersonProviderConstants.PERSON_ALL_URI,
				true,
				new PersonObserver(mHandler));
		
		// loader managing
		getLoaderManager().initLoader(LOADER_ID_0, null, this);
		
		mPopupWindowWidth =
				getResources().getDisplayMetrics().widthPixels / 2;
		
		LayoutInflater inflater =
				(LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout contentView = (LinearLayout) inflater.inflate(
				R.layout.popup_content, null);
		mPopup = new PopupWindow(contentView, mPopupWindowWidth,
				LayoutParams.WRAP_CONTENT, true);
		mPopup.setTouchable(true);
		mPopup.setOutsideTouchable(true);
		mPopup.setBackgroundDrawable(new BitmapDrawable());
		
		// cursor is null, that's OK!
		mCursorAdapter = new SimpleCursorAdapter(
				this,
				android.R.layout.simple_list_item_2,
				null,
				new String[] { "name", "info" }, // column name
				new int[] {	android.R.id.text1, android.R.id.text2 }, // view id
				0);
		// 参数跟SimpleCursorAdapter一样，新的adapter只是在bindView()时添加了一个
		// 长按监听器弹出popup window而已。
//		mCursorAdapter = new PopupCursorAdapter(
//				this,
//				android.R.layout.simple_list_item_2,
//				null,
//				new String[] { "name", "info" }, // column name
//				new int[] {	android.R.id.text1, android.R.id.text2 }, // view id
//				0);
		ListView lv = (ListView) findViewById(R.id.listView);
		lv.setAdapter(mCursorAdapter);
		lv.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View v,
					int position, long id) {
				Log.v(TAG, "onItemLongClick()");
				mPopup.showAsDropDown(v, (v.getWidth() - mPopupWindowWidth) / 2, 5);
				return true;
			}
		});
		
		// register context menu for ListView
//		registerForContextMenu(lv);
	}
	
	private boolean clearDB() {
		// delete all items
		if(mResolver.delete(PersonProviderConstants.PERSON_ALL_URI, null, null) != -1) {
			return true;
		}
		return false;
	}
	
	public void init(View view) {
		// clear old data
		boolean ret = clearDB();
		Log.v(TAG, "clear database " + ret);
		
		ArrayList<Person> persons = new ArrayList<Person>();

		Person person1 = new Person("Ella", 22, "lively girl");
		Person person2 = new Person("Jenny", 22, "beautiful girl");
		Person person3 = new Person("Jessica", 23, "sexy girl");
		Person person4 = new Person("Kelly", 23, "hot baby");
		Person person5 = new Person("Jane", 25, "pretty woman");

		persons.add(person1);
		persons.add(person2);
		persons.add(person3);
		persons.add(person4);
		persons.add(person5);

		for (Person person : persons) {
			ContentValues values = new ContentValues();
			values.put("name", person.name);
			values.put("age", person.age);
			values.put("info", person.info);
			mResolver.insert(PersonProviderConstants.PERSON_ALL_URI, values);
		}
	}

	public void query(View view) {
		Log.v(TAG, "query()");
		
		mCursorAdapter.notifyDataSetChanged();
	}

	public void insert(View view) {
		Log.v(TAG, "insert()");
		
		Person person = new Person("Alina", 26, "attractive lady");
		ContentValues values = new ContentValues();
		values.put("name", person.name);
		values.put("age", person.age);
		values.put("info", person.info);
		mResolver.insert(PersonProviderConstants.PERSON_ALL_URI, values);
	}

	public void update(View view) {
		Log.v(TAG, "update()");
		
		Person person = new Person();
		person.name = "Jane";
		person.age = 30;
		// 将指定name的记录age字段更新为30
		ContentValues values = new ContentValues();
		values.put("age", person.age);
		mResolver.update(
				PersonProviderConstants.PERSON_ALL_URI,
				values,
				"name = ?",
				new String[] { person.name });
	}

	public void delete(View view) {
		Log.v(TAG, "delete()");
		
		// delete #1 item
		Uri delUri = ContentUris.withAppendedId(PersonProviderConstants.PERSON_ALL_URI, 1);
		mResolver.delete(delUri, null, null);
		
		// delete Alina
		mResolver.delete(PersonProviderConstants.PERSON_ALL_URI,
				"name = ?", new String[] { "Alina" });
	}

	private void requery() {
		query(null);
	}
	
	
	// -------- LoaderManager.LoaderCallbacks -------- //
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		if(id == LOADER_ID_0) {
			// create a CursorLoader for LoaderManager to manage
			CursorLoader cl = new CursorLoader(this,
					PersonProviderConstants.PERSON_ALL_URI, null, null, null, null);
			return cl;
		} else {
			// create other CursorLoader.
		}
		
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		Log.v(TAG, "onLoadFinished()");
		
		if (LOADER_ID_0 == loader.getId()) {
			// now data(cursor) has been prepared, old cursor will be closed by
			// system, don't close it by ourselves.
			mCursorAdapter.swapCursor(new MyCursorWrapper(data));
		} else {
			// deal with other loaders
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (LOADER_ID_0 == loader.getId()) {
			// data reset
			mCursorAdapter.swapCursor(null);
		} else {
			// deal with other loaders
		}
	}
	// -------- LoaderManager.LoaderCallbacks -------- //
	
	// -------- wrap the cursor ---------//
	class MyCursorWrapper extends CursorWrapper {
		public MyCursorWrapper(Cursor cursor) {
			super(cursor);
		}

		@Override
		public String getString(int columnIndex) {
			// 将简介前加上年龄
			if (getColumnName(columnIndex).equals("info")) {
				int age = getInt(getColumnIndex("age"));
				return age + " years old, " + super.getString(columnIndex);
			}
			return super.getString(columnIndex);
		}
	}
	// -------- wrap the cursor ---------//
	
	//--------- Context Menu -----------//
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		// create our menu, the result will be
		// saved in 'menu'
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.context_menu, menu);
		
		menu.setHeaderTitle("This is ContextMenu...");
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		// how can I get the target view? e.g. a list in ListView
		AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
		// android.R.layout.simple_list_item_2
		View target = info.targetView;
		TextView text1 = (TextView) target.findViewById(android.R.id.text1);
		TextView text2 = (TextView) target.findViewById(android.R.id.text2);
		Log.v(TAG, "onContextItemSelected: item id = " + item.getItemId()
				+ ", text1 = " + text1.getText() + ", text2 = " + text2.getText());
		
		// deal with each item
		switch (item.getItemId()) {
			case R.id.option_0:
				return true;
			case R.id.option_1:
				return true;
			case R.id.option_2:
				return true;
			case R.id.option_3:
				return true;
			default:
				return super.onContextItemSelected(item);
		}
	}
	
	@Override
	public void onContextMenuClosed(Menu menu) {
		super.onContextMenuClosed(menu);
	}
	//--------- Context Menu -----------//
	
	//--------- Options Menu -----------//
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// deal with each item
		switch (item.getItemId()) {
			case R.id.option_0:
				return true;
			case R.id.option_1:
				return true;
			case R.id.option_2:
				return true;
			case R.id.option_3:
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onOptionsMenuClosed(Menu menu) {
		super.onOptionsMenuClosed(menu);
	}
	//--------- Options Menu -----------//
	
}