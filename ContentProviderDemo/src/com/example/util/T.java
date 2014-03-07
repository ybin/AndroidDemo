package com.example.util;

public class T {
	public static int __LINE__() {
		return Thread.currentThread().getStackTrace()[2].getLineNumber();
	}
	
	public static String __FILE__() {
		return Thread.currentThread().getStackTrace()[2].getFileName();
	}
}
