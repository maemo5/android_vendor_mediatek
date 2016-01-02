ifeq ($(MTK_SMSREG_APP),yes)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

#LOCAL_SRC_FILES := $(call all-subdir-java-files)
LOCAL_SRC_FILES := $(call all-java-files-under,src)

LOCAL_PACKAGE_NAME := SmsReg

#LOCAL_PROGUARD_ENABLED := full

LOCAL_JAVA_LIBRARIES += telephony-common
LOCAL_JAVA_LIBRARIES += mediatek-framework mediatek-telephony-common

# Vanzo:yucheng on: Tue, 13 Jan 2015 17:10:23 +0800
# implement Feature_app_5003, vanzo SmsRegister porting
#include $(BUILD_PACKAGE)
# End of Vanzo: yucheng

include $(CLEAR_VARS)

include $(call all-makefiles-under,$(LOCAL_PATH))

endif
