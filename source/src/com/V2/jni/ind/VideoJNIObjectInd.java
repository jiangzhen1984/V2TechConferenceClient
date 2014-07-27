package com.V2.jni.ind;


public class VideoJNIObjectInd extends JNIObjectInd {
	
	private long mGroupId;
	private long mFromUserId;
	private String mDeviceId;
	
	public VideoJNIObjectInd(long groupId, long fromUserId, String deviceId, int requestType) {
		this.mGroupId = groupId;
		this.mFromUserId = fromUserId;
		this.mRequestType = requestType;
		this.mType = JNIIndType.VIDEO;
		this.mDeviceId = deviceId;
	}

	public long getGroupId() {
		return mGroupId;
	}

	public void setGroupId(long groupId) {
		this.mGroupId = groupId;
	}

	public long getFromUserId() {
		return mFromUserId;
	}

	public void setFromUserId(long fromUserId) {
		this.mFromUserId = fromUserId;
	}

	public String getDeviceId() {
		return mDeviceId;
	}

	public void setDeviceId(String deviceId) {
		this.mDeviceId = deviceId;
	}
	
	
	

}
