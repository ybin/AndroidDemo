package com.ybin.ndkdemo;

/**
 * Interface class of JNI.
 */
public class JNIInterface {
    static {
        System.loadLibrary("hello");
    }

    public static native String hello();
}
