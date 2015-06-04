#include <iostream>

#define _JNI_IMPLEMENTATION_
#include "jnidemo.h"

int JNICALL f1(jnidemoEnv *env, int i) {
    std::cout << "call f1..." << std::endl;
    return 0;
}

int JNICALL f2(jnidemoEnv *env, int i) {
    std::cout << "call f2..." << std::endl;
    return 0;
}

int JNICALL f3(jnidemoEnv *env, int i) {
    std::cout << "call f3..." << std::endl;
    return 0;
}

struct _jnidemo_interface _interface = {
    f1,
    f2,
    f3,
};
struct _jnidemo _demo = { &_interface };

#ifdef __cplusplus
jnidemoEnv *env = &_demo;
#else
jnidemoEnv _env = &_interface;
jnidemoEnv *env = &_env;
#endif

_JNI_IMPORT_OR_EXPORT_ jnidemoEnv* JNICALL getEnv() {
    return env;
}


