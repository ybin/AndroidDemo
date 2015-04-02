#include "Util.h"

void assert_fail(const char *file, int line, const char *func, const char *expr) {
    INFO("assertion failed at file %s, line %d, function %s:", file, line, func);
    INFO("%s", expr);
    abort();
}