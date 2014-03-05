package com.v2tech.logic.jni;

/**
 * Used to wrap response data from JNI when receive call from JNI
 * @author 28851274
 *
 */
public class RequestCloseUserVideoDeviceResponse extends JNIResponse {

	
	public enum Result {
		SUCCESS(0), FAILED(1);

		private int val;
		private Result(int i) {
			this.val = i;
		}
		
		public int value() {
			return val;
		}
		
	}
	
	
	
	long nConfID;
	long nTime;
	Result er;

	/**
	 * This class is wrapper that wrap response of request to close user video device
	 * @param nConfID
	 * @param nTime
	 * @param result {@link Result}
	 */
	public RequestCloseUserVideoDeviceResponse(long nConfID, long nTime,
			Result result) {
		super();
		this.nConfID = nConfID;
		this.nTime = nTime;
		er = result;
	}

}
