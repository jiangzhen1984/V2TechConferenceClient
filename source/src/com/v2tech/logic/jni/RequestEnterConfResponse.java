package com.v2tech.logic.jni;

/**
 * Used to wrap response data from JNI when receive call from JNI
 * @author 28851274
 *
 */
public class RequestEnterConfResponse extends JNIResponse {

	
	
	
	long nConfID;
	long nTime;
	String szConfData;

	/**
	 * This class is wrapper that wrap{@link com.V2.jni.ConfRequestCallback#OnEnterConfCallback(long, long,
	 *      String, int)} return data
	 * @param nConfID
	 * @param nTime
	 * @param szConfData
	 * @param nJoinResult
	 * @see  com.V2.jni.ConfRequestCallback#OnEnterConfCallback(long, long,
	 *      String, int)
	 */
	public RequestEnterConfResponse(long nConfID, long nTime,
			String szConfData, Result res) {
		super(res);
		this.nConfID = nConfID;
		this.nTime = nTime;
		this.szConfData = szConfData;
	}
	
	
	public long getConferenceID() {
		return nConfID;
	}
}
