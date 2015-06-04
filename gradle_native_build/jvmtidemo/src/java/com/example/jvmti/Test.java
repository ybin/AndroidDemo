package com.example.jvmti;

public class Test {
    public static void main(String[] args) {
        System.out.println("hello, java");
        Class klass = Class.class;
        System.out.println("There are " + countInstances(klass) + " instances of " + klass);
        
        printSysteProperties();
    }
    
    private static native int countInstances(Class klass);
    private static native void printSysteProperties();
}