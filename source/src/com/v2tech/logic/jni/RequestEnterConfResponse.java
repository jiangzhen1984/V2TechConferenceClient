package com.v2tech.logic.jni;

/**
 * Used to wrap response data from JNI when receive call from JNI
 * @author 28851274
 *
 */
public class RequestEnterConfResponse extends JNIResponse {

	
	enum EnteredResult {
		SUCCESS(0), FAILED(1);
		
		private int val;
		private EnteredResult(int i) {
			this.val = i;
		}
		
		public int value() {
			return val;
		}
	}
	
	
	
	long nConfID;
	long nTime;
	String szConfData;
	EnteredResult er;

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
			String szConfData, int nJoinResult) {
		super();
		this.nConfID = nConfID;
		this.nTime = nTime;
		this.szConfData = szConfData;
		er = nJoinResult == EnteredResult.SUCCESS.value() ? EnteredResult.SUCCESS
				: EnteredResult.FAILED;
	}

}
