package com.example.aidldemo.server;

import android.os.Parcel;
import android.os.Parcelable;

public class Book implements Parcelable {
	private String mBookName;
	private int mBookPrice;

	public Book(String name, int price) {
		mBookName = name;
		mBookPrice = 42;
	}

	public Book(Parcel parcel) {
		mBookName = parcel.readString();
		mBookPrice = parcel.readInt();
	}

	public String getBookName() {
		return mBookName;
	}

	public int getBookPrice() {
		return mBookPrice;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeString(mBookName);
		parcel.writeInt(mBookPrice);
	}

	public static final Parcelable.Creator<Book> CREATOR = new Creator<Book>() {
		public Book createFromParcel(Parcel source) {
			return new Book(source);
		}

		public Book[] newArray(int size) {
			return new Book[size];
		}
	};
}
