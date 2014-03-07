package com.example.provider;

import java.util.ArrayList;

import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class MainActivity extends Activity implements LoaderCallbacks<Cursor> {
	
	public static final String TAG = "ContentProviderDemo";

	private ContentResolver mResolver;

	private static final int LOADER_ID_0 = 0;
	private SimpleCursorAdapter mCursorAdapter;

	private Handler mHandler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
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
		
		
		// cursor is null, that's OK!
		mCursorAdapter = new SimpleCursorAdapter(
				this,
				android.R.layout.simple_list_item_2,
				null,
				new String[] { "name", "info" }, // column name
				new int[] {	android.R.id.text1, android.R.id.text2 }, // view id
				0);
		((ListView) findViewById(R.id.listView)).setAdapter(mCursorAdapter);
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
	
}