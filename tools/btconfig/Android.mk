#ifeq ($(BOARD_HAVE_BLUETOOTH),true)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../hidl_client/inc \

LOCAL_SRC_FILES:= \
              btconfig.c

LOCAL_MULTILIB := 32
LOCAL_MODULE_TAGS := debug optional
LOCAL_MODULE :=btconfig
LOCAL_SHARED_LIBRARIES += libcutils   \
                          libutils    \
                          libdl       \
                          liblog      \
                          libhidlbase \
                          libhidltransport \
                          libhwbinder

LOCAL_MODULE_PATH := $(TARGET_OUT_VENDOR_EXECUTABLES)
include $(BUILD_EXECUTABLE)
#endif
