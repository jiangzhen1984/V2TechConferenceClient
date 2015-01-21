package com.bizcom.request.jni;

import com.bizcom.vo.User;

public class RequestLogInResponse extends JNIResponse {
	
	private String serverID;
	private User u;
	
	public RequestLogInResponse(User u, Result res) {
		super(res);
		this.u = u;
		this.serverID = null;
		this.res = res;
	}
	
	public RequestLogInResponse(User u, String serverID , Result res) {
		super(res);
		this.u = u;
		this.serverID = serverID;
		this.res = res;
	}
	
	public RequestLogInResponse(User u , Result res, Object originObject) {
		super(res);
		this.u = u;
		this.serverID = null;
		this.res = res;
		this.callerObject = originObject;
	}
	
	public String getServerID() {
		return serverID;
	}

	public void setServerID(String serverID) {
		this.serverID = serverID;
	}

	public User getUser() {
		return u;
	}

	public void setUser(User u) {
		this.u = u;
	}
}
