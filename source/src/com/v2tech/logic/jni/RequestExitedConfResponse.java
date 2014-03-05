package com.v2tech.logic.jni;


/**
 * Used to wrap response data from JNI when receive call from JNI
 * @author 28851274
 *
 */
public class RequestExitedConfResponse extends JNIResponse {

	
	public enum ExitedResult {
		SUCCESS(0), FAILED(1);

		private int val;
		private ExitedResult(int i) {
			this.val = i;
		}
		
		public int value() {
			return val;
		}
		
	}
	
	
	
	long nConfID;
	long nTime;
	ExitedResult er;

	/**
	 * This class is wrapper that wrap response of exited conference
	 * @param nConfID
	 * @param nTime
	 * @param nJoinResult {@link ExitedResult}
	 */
	public RequestExitedConfResponse(long nConfID, long nTime,
			ExitedResult result) {
		super();
		this.nConfID = nConfID;
		this.nTime = nTime;
		er = result;
	}

}
