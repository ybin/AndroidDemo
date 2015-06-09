package com.example.jni;

public class Hello {
  static {
	  System.loadLibrary("hello_legacy");
	  System.loadLibrary("hello");
  }

  public static void main(String[] args) {
		System.out.println(getName());
		System.out.println(getName2());
	}

  private static native String getName();
  private static native String getName2();
}
