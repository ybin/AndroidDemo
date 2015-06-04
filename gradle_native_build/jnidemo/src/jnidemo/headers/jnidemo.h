#ifndef _JNI_DEMO_H_
#define _JNI_DEMO_H_

#define JNIEXPORT __declspec(dllexport)
#define JNIIMPORT __declspec(dllimport)
#define JNICALL __stdcall

#ifdef _JNI_IMPLEMENTATION_
#define _JNI_IMPORT_OR_EXPORT_ JNIEXPORT
#else
#define _JNI_IMPORT_OR_EXPORT_ JNIIMPORT
#endif

#ifdef __cplusplus
extern "C" {
#endif

struct _jnidemo_interface;
struct _jnidemo;
#ifdef __cplusplus
typedef _jnidemo jnidemoEnv;
#else
typedef const struct _jnidemo_interface *jnidemoEnv;
#endif

// global getter
_JNI_IMPORT_OR_EXPORT_ jnidemoEnv* JNICALL getEnv();

// for c
struct _jnidemo_interface {
    int (JNICALL *f1)(jnidemoEnv* env, int i);
    int (JNICALL *f2)(jnidemoEnv* env, int i);
    int (JNICALL *f3)(jnidemoEnv* env, int i);
};

// for cpp
struct _jnidemo {
    struct _jnidemo_interface *functions;
    
#ifdef __cplusplus
    int f1(int i) {
        return functions->f1(this, i);
    }
    
    int f2(int i) {
        return functions->f2(this, i);
    }
    
    int f3(int i) {
        return functions->f2(this, i);
    }
#endif
};

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif