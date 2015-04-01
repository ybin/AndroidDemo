#ifndef _CALC_UTIL_H__
#define _CALC_UTIL_H__

#define LOG_TAG "binder_calc"

#include <stdlib.h>
#include <utils/Log.h>
#include <binder/IBinder.h>

using namespace android;

#define INFO(...) \
    do { \
        printf(__VA_ARGS__); /* print to screen */ \
        printf("\n"); \
        ALOGD(__VA_ARGS__); /* add to logcat */ \
    } while(0)

void assert_fail(const char *file, int line, const char *func, const char *expr);

#define ASSERT(e) \
    do { \
        if (!(e)) \
            assert_fail(__FILE__, __LINE__, __func__, #e); \
    } while(0)

#endif