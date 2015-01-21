package com.bizcom.request.jni;

/**
 * Used to wrap response data from JNI when receive call from JNI
 * @author 28851274
 *
 */
public class CreateDiscussionBoardResponse extends JNIResponse {

	
	
	long nGroupId;

	/**
	 * This class is wrapper that wrap response of create crowd
	 * @param nGroupId returned crowd id
	 * @param result {@link Result}
	 */
	public CreateDiscussionBoardResponse(long nGroupId,
			Result result) {
		super(result);
		this.nGroupId = nGroupId;
	}
	
	
	public long getGroupId() {
		return nGroupId;
	}

}
