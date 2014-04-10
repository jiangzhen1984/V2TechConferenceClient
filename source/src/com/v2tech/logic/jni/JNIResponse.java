package com.v2tech.logic.jni;

/**
 * JNI call back data wrapper
 * 
 * @author 28851274
 * 
 */
public class JNIResponse {

	public enum Result {
		SUCCESS(0), FAILED(1), CONNECT_ERROR(301), SERVER_REJECT(300), UNKNOWN(-1);

		private int val;
		private Result(int i) {
			this.val = i;
		}
		
		public int value() {
			return val;
		}
		
		public static Result fromInt(int code) {
			switch (code) {
			case 0:
				return SUCCESS;
			case 1:
				return FAILED;
			case 301:
				return CONNECT_ERROR;
			case 300:
				return SERVER_REJECT;
			default:
				return UNKNOWN;
			}
		}
		
		
	}
	
	protected Result res;

	public Object callerObject;
	
	
	
	
	public JNIResponse(Result res) {
		super();
		this.res = res;
	}





	public Result getResult() {
		return res;
	}
}
