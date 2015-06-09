#include <com_example_jni_Hello.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jstring JNICALL Java_com_example_jni_Hello_getName(JNIEnv *env, jclass clazz) {
	return env->NewStringUTF("hello, from jni");
}

#ifdef __cplusplus
}
#endif
