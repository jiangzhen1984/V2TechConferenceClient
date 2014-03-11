package com.v2tech.logic.jni;

/**
 * JNI call back data wrapper
 * 
 * @author 28851274
 * 
 */
public class JNIResponse {

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
