#include "atomic.h"

#ifdef __cplusplus
extern "C" {
#endif

int32_t android_atomic_inc(volatile int32_t* addr) {
    int32_t tmp = *addr;
    *addr += 1;
    return tmp;
}

int32_t android_atomic_dec(volatile int32_t* addr) {
    int32_t tmp = *addr;
    *addr -= 1;
    return tmp;
}

int32_t android_atomic_add(int32_t value, volatile int32_t* addr) {
    int32_t tmp = *addr;
    *addr += value;
    return tmp;
}

int32_t android_atomic_or(int32_t value, volatile int32_t* addr) {
    int32_t tmp = *addr;
    *addr |= value;
    return tmp;
}

int android_atomic_release_cas(int32_t oldvalue, int32_t newvalue,
        volatile int32_t* addr) {
    *addr = newvalue;
    return 0;
}

#ifdef __cplusplus
} // extern "C"
#endif