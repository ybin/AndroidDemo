package com.example.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

public class PersonProvider extends ContentProvider {
	
	public static final String TAG = "PersonProvider";

	private static final UriMatcher matcher;
	private DBHelper helper;
	private SQLiteDatabase db;
	
	private static final int PERSON_ALL = 0;
	private static final int PERSON_ONE = 1;
	
	static {
		matcher = new UriMatcher(UriMatcher.NO_MATCH);
		
		matcher.addURI(PersonProviderConstants.AUTHORITY, "persons", PERSON_ALL);	//匹配记录集合
		matcher.addURI(PersonProviderConstants.AUTHORITY, "persons/#", PERSON_ONE);	//匹配单条记录
	}
	
	@Override
	public boolean onCreate() {
		helper = new DBHelper(getContext());
		return true;
	}

	@Override
	public String getType(Uri uri) {
		int match = matcher.match(uri);
		switch (match) {
		case PERSON_ALL:
			return PersonProviderConstants.CONTENT_TYPE;
		case PERSON_ONE:
			return PersonProviderConstants.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		db = helper.getReadableDatabase();
		int match = matcher.match(uri);
		switch (match) {
		case PERSON_ALL:
			//doesn't need any code in my provider.
			break;
		case PERSON_ONE:
			long _id = ContentUris.parseId(uri);
			selection = "_id = ?";
			selectionArgs = new String[]{String.valueOf(_id)};
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		return db.query("person", projection, selection, selectionArgs, null, null, sortOrder);
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		int match = matcher.match(uri);
		if (match != PERSON_ALL) {
			throw new IllegalArgumentException("Wrong URI: " + uri);
		}
		db = helper.getWritableDatabase();
		if (values == null) {
			values = new ContentValues();
			values.put("name", "no name");
			values.put("age", "1");
			values.put("info", "no info.");
		}
		long rowId = db.insert("person", null, values);
		if (rowId > 0) {
			notifyDataChanged();
			return ContentUris.withAppendedId(uri, rowId);
		}
		return null;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		db = helper.getWritableDatabase();
		int match = matcher.match(uri);
		Log.v(TAG, "match: " + match);
		switch (match) {
		case PERSON_ALL:
			//doesn't need any code in my provider.
			break;
		case PERSON_ONE:
			long _id = ContentUris.parseId(uri);
			selection = "_id = ?";
			selectionArgs = new String[]{String.valueOf(_id)};
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		int count = db.delete("person", selection, selectionArgs);
		if (count > 0) {
			notifyDataChanged();
		}
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		db = helper.getWritableDatabase();
		int match = matcher.match(uri);
		switch (match) {
		case PERSON_ALL:
			//doesn't need any code in my provider.
			break;
		case PERSON_ONE:
			long _id = ContentUris.parseId(uri);
			selection = "_id = ?";
			selectionArgs = new String[]{String.valueOf(_id)};
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		int count = db.update("person", values, selection, selectionArgs);
		if (count > 0) {
			notifyDataChanged();
		}
		return count;
	}

	//通知指定URI数据已改变
	private void notifyDataChanged() {
		getContext().getContentResolver().notifyChange(PersonProviderConstants.NOTIFY_URI, null);		
	}
}
