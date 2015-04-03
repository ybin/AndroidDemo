LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := refbase
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := src/RefBase.cpp \
                   src/TextOutput.cpp \

#LOCAL_C_INCLUDES += frameworks/base/include system/core/include
#LOCAL_SHARED_LIBRARIES := libutils libbinder liblog

LOCAL_C_INCLUDES += jni/include
LOCAL_LDLIBS += -Ljni/libs_ -lcutils -llog

#include $(BUILD_EXECUTABLE)
include $(BUILD_SHARED_LIBRARY)


#########################
include $(CLEAR_VARS)

LOCAL_MODULE := testsp
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := src/testsp.cpp \

LOCAL_C_INCLUDES += jni/include
LOCAL_LDLIBS += -Llibs/armeabi -lrefbase -llog

include $(BUILD_EXECUTABLE)