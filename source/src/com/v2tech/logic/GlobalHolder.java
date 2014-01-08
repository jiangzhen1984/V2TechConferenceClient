package com.v2tech.logic;

public class GlobalHolder {

	private static GlobalHolder gh = new GlobalHolder();
	
	private User u;
	
	private GlobalHolder() {
		
	}
	
	public static GlobalHolder getInstance() {
		if (gh == null) {
			gh = new GlobalHolder();
		}
		return gh;
	}
	
	public static long getLoggedUserId() {
		return getInstance().getUser().getmUserId();
	}
	
	
	public User getUser() {
		return u;
	}

	public void setUser(User u) {
		this.u = u;
	}
}
