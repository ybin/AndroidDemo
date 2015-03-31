
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := _calc
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := binder.cpp

#LOCAL_C_INCLUDES += frameworks/base/include system/core/include
LOCAL_C_INCLUDES += include

LOCAL_SHARED_LIBRARIES := libutils libbinder liblog
LOCAL_LDLIBS += -Lshared_libraries -llog -lutils -lbinder
#LOCAL_SHARED_LIBRARIES := libutils libcutils libbinder liblog

include $(BUILD_EXECUTABLE)

