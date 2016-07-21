LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)


# Additional Logging
# Uncomment for INFO level messages
LOCAL_CFLAGS += -DLOG_NIDEBUG=0
LOCAL_CFLAGS += -Wno-psabi -Wno-write-strings

LOCAL_DEFAULT_CPP_EXTENSION := cc
LOCAL_SRC_FILES:= \
    bt_fwd_service.c

LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_C_INCLUDES += $(TARGET_OUT_HEADERS)/common/inc


LOCAL_C_INCLUDES += \
        $(LOCAL_PATH)/include \
        system/bt/hci/include

LOCAL_SHARED_LIBRARIES := liblog libcutils

LOCAL_MODULE:= bt-fwd-service
LOCAL_MODULE_TAGS := optional

include $(BUILD_EXECUTABLE)
