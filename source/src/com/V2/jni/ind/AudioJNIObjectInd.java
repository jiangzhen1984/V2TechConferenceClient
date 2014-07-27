package com.V2.jni.ind;


/**
 * This class wrapped audio JNI request returned result
 * @see com.V2.jni.AudioRequest
 * @author jiangzhen
 *
 */
public class AudioJNIObjectInd extends JNIObjectInd {

	public AudioJNIObjectInd() {
		this(0, 0, -1);
	}

	
	private long mGroupId;
	private long mFromUserId;
	
	public AudioJNIObjectInd(long groupId, long fromUserId, int requestType) {
		this.mGroupId = groupId;
		this.mFromUserId = fromUserId;
		this.mRequestType = requestType;
		this.mType = JNIIndType.AUDIO;
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
	
	
}
