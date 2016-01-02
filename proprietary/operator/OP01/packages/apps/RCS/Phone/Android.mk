LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(call all-java-files-under, src) \
                   $(call all-Iaidl-files-under, src)

LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/res

LOCAL_JAVA_LIBRARIES += mediatek-framework telephony-common
LOCAL_STATIC_JAVA_LIBRARIES := com.mediatek.services.rcs.phone

LOCAL_PACKAGE_NAME :=  RCSPhone
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

# link your plug-in interface .jar here 
LOCAL_JAVA_LIBRARIES += com.mediatek.incallui.ext

# Put plugin apk together to specific folder
LOCAL_MODULE_PATH := $(PRODUCT_OUT)/system/plugin

#LOCAL_PROGUARD_ENABLED := disabled
LOCAL_PROGUARD_FLAG_FILES := proguard.flags
include $(BUILD_PACKAGE)

# Include plug-in's makefile to automated generate .mpinfo
include vendor/mediatek/proprietary/frameworks/plugin/mplugin.mk

# This finds and builds the test apk as well, so a single make does both.
include $(call all-makefiles-under,$(LOCAL_PATH))
