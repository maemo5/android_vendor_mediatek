ifeq ($(MTK_FM_SUPPORT),yes)
ifeq ($(MTK_FM_RX_SUPPORT),yes)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_CERTIFICATE := platform

LOCAL_JAVA_LIBRARIES := android.test.runner
LOCAL_STATIC_JAVA_LIBRARIES := librobotium4

# Include all test java files.
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := FMRadioTest

LOCAL_INSTRUMENTATION_FOR := FMRadio

LOCAL_PROGUARD_ENABLED := disabled

# Vanzo:yucheng on: Tue, 12 May 2015 15:25:42 +0800
# Del FMRadioTest to fix compile error
#include $(BUILD_PACKAGE)
# End of Vanzo: yucheng
endif
endif
