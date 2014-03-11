package com.v2tech.logic.jni;

import com.v2tech.logic.User;

public class RequestLogInResponse extends JNIResponse {
	
	
	
	User u;
	
	public RequestLogInResponse(User u, Result res) {
		super(res);
		this.u = u;
		this.res = res;
	}
	

	
	public User getUser() {
		return u;
	}

}
