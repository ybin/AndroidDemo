LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := calc
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := src/CalcClient.cpp \
                   src/CalcService.cpp \
                   src/Calc.cpp \
                   src/Util.cpp \
                   test/calc_test.cpp \

#LOCAL_C_INCLUDES += frameworks/base/include system/core/include
#LOCAL_SHARED_LIBRARIES := libutils libbinder liblog

LOCAL_C_INCLUDES += jni/deps/include
LOCAL_C_INCLUDES += jni/include
LOCAL_LDLIBS += -Ljni/deps/libs -llog -lutils -lbinder

include $(BUILD_EXECUTABLE)