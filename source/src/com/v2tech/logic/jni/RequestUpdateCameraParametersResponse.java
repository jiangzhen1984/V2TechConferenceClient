package com.v2tech.logic.jni;

import com.v2tech.logic.CameraConfiguration;


public class RequestUpdateCameraParametersResponse extends JNIResponse {

	
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
	
	
	CameraConfiguration config;
	Result er;

	/**
	 * This class is wrapper that wrap response of request to update camera
	 * @param config
	 * @param result {@link Result}
	 */
	public RequestUpdateCameraParametersResponse(CameraConfiguration config,
			Result result) {
		super();
		this.config = config;
		er = result;
	}

}
