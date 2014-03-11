package com.v2tech.logic.jni;

public class RequestOpenUserVideoDeviceResponse extends JNIResponse {

	long nConfID;
	long nTime;

	/**
	 * This class is wrapper that wrap response of request open user video
	 * device
	 * 
	 * @param nConfID
	 * @param nTime
	 * @param nJoinResult
	 *            {@link Result}
	 */
	public RequestOpenUserVideoDeviceResponse(long nConfID, long nTime,
			Result result) {
		super(result);
		this.nConfID = nConfID;
		this.nTime = nTime;
	}

}
