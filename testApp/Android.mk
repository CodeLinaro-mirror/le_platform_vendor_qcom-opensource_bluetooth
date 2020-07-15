LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

#LOCAL_MODULE_TAGS := optional
LOCAL_CFLAGS := -Werror

LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/res
LOCAL_RESOURCE_DIR += frameworks/support/v7/appcompat/res
LOCAL_RESOURCE_DIR += frameworks/support/v7/recyclerview/res


LOCAL_AAPT_FLAGS := --auto-add-overlay
src_dirs:= src/org/codeaurora/bluetooth/testApp \

LOCAL_SRC_FILES := \
        $(call all-java-files-under, $(src_dirs))

LOCAL_PACKAGE_NAME := wearos_ble_testapp
LOCAL_CERTIFICATE := platform

LOCAL_PRIVATE_PLATFORM_APIS := true

LOCAL_PROGUARD_ENABLED := disabled

include $(BUILD_PACKAGE)

include $(call all-makefiles-under,$(LOCAL_PATH)) 



