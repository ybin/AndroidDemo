package com.example.provider;

import android.net.Uri;

public class PersonProviderConstants {
	public static final String AUTHORITY = "com.example.provider.PersonProvider";
	public static final Uri PERSON_ALL_URI =
			Uri.parse("content://" + AUTHORITY + "/persons");
	public static final Uri NOTIFY_URI = Uri.parse("content://" + AUTHORITY + "/persons");
	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.scott.person";
	public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.scott.person";
}
