LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := v2vi-prebuilt
LOCAL_SRC_FILES := ../libs/libv2vi.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/..
include $(PREBUILT_SHARED_LIBRARY)



include $(CLEAR_VARS)
LOCAL_MODULE := event-prebuilt
LOCAL_SRC_FILES := ../libs/libevent.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/..
include $(PREBUILT_SHARED_LIBRARY)



include $(CLEAR_VARS)
LOCAL_MODULE := v2ve-prebuilt
LOCAL_SRC_FILES := ../libs/libv2ve.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/..
include $(PREBUILT_SHARED_LIBRARY)



include $(CLEAR_VARS)
LOCAL_MODULE := v2client-prebuilt
LOCAL_SRC_FILES := ../libs/libv2client.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/..
include $(PREBUILT_SHARED_LIBRARY)


include $(CLEAR_VARS)
LOCAL_MODULE := udt-prebuilt
LOCAL_SRC_FILES := ../libs/libudt.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/..
include $(PREBUILT_SHARED_LIBRARY)
 
 
 
include $(CLEAR_VARS)
 
LOCAL_MODULE    := v2techclient
LOCAL_SHARED_LIBRARIES += event
LOCAL_SHARED_LIBRARIES += udt
LOCAL_SHARED_LIBRARIES += v2client
LOCAL_SHARED_LIBRARIES += v2ve
LOCAL_SHARED_LIBRARIES += v2vi

include $(BUILD_SHARED_LIBRARY)
