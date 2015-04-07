LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := testsp
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := refbase/RefBase.cpp \
                   refbase/testsp.cpp \
                   refbase/fake_atomic.cpp \

LOCAL_C_INCLUDES += jni/refbase

include $(BUILD_EXECUTABLE)