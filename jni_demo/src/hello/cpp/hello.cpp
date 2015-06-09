#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

static jstring getNameImpl(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF("hello, from jni2");
}

// ...

static const char *className = "com/example/jni/Hello";
static JNINativeMethod methods[] = {
    {"getName2", "()Ljava/lang/String;", (void*)getNameImpl},
    // ...
};

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    jclass clazz;

	if(vm->GetEnv((void**)&env, JNI_VERSION_1_4) != JNI_OK) {
	  // error when get env.
	  return JNI_ERR;
	}
	clazz = env->FindClass(className);
	if(clazz == NULL) {
	  // error when get clazz.
	  return JNI_ERR;
	}
	int len = sizeof(methods) / sizeof(methods[0]);
	if(env->RegisterNatives(clazz, methods, len) < 0) {
	  // error when register clazz.
	  return JNI_ERR;
	}

    return JNI_VERSION_1_4;
}

#ifdef __cplusplus
}
#endif
