#include <stdio.h>
#include "jnidemo.h"

int main()
{
    jnidemoEnv *env = getEnv();
    (*env)->f1(env, 0);
    (*env)->f2(env, 0);
    (*env)->f3(env, 0);
    return 0;
}