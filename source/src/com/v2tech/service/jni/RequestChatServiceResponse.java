package com.v2tech.service.jni;


/**
 * Used to wrap response data from JNI when receive call from JNI
 * @author 28851274
 *
 */
public class RequestChatServiceResponse extends JNIResponse {

	public static final int UNKNOWN = 0;
	public static final int ACCEPTED = 1;
	public static final int REJCTED = 2;
	public static final int CANCELED = 3;
	
	int code;
	
	/**
	 * This class is wrapper that wrap response of chat service
	 * @param result {@link Result}
	 */
	public RequestChatServiceResponse(
			Result result) {
		super(result);
	}
	
	public RequestChatServiceResponse(int code,
			Result result) {
		super(result);
		this.code = code;
	}
	
	public int getCode() {
		return code;
	}

}
