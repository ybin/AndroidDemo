#define LOG_TAG "hello"

#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include "util.h"

#ifdef __cplusplus
extern "C" {
#endif

/************** native implementation **************/
static jstring getNameImpl(JNIEnv *env, jclass clazz) {
    for (int i = 0; i < 100; ++i) {
        void * m = malloc(1024*1024);
        memset(m, 0, 1024*1024);
        LOGI("hello: %p\n", m);
    }
    return env->NewStringUTF("hello, from jni2");
}


static const char *JAVA_CLASS_NAME = "com/ybin/ndkdemo/JNIInterface";
static JNINativeMethod METHODS[] = {
        {"hello", "()Ljava/lang/String;", (void*)getNameImpl},
};


/******* register native methods ********/

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    jclass clazz;

    if(vm->GetEnv((void**)&env, JNI_VERSION_1_4) != JNI_OK) {
        // error when get env.
        return JNI_ERR;
    }
    clazz = env->FindClass(JAVA_CLASS_NAME);
    if(clazz == NULL) {
        // error when get clazz.
        return JNI_ERR;
    }
    int len = sizeof(METHODS) / sizeof(METHODS[0]);
    if(env->RegisterNatives(clazz, METHODS, len) < 0) {
        // error when register clazz.
        return JNI_ERR;
    }

    return JNI_VERSION_1_4;
}

#ifdef __cplusplus
}
#endif
