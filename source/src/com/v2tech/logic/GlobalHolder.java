package com.v2tech.logic;

public class GlobalHolder {

	private static GlobalHolder holder;
	
	private User mCurrentUser;
	
	
	public static synchronized GlobalHolder getInstance() {
		if (holder == null) {
			holder = new GlobalHolder();
		}
		return holder;
	}
	private GlobalHolder() {
		
	}
	
	
	public User getCurrentUser() {
		return mCurrentUser;
	}
	
	public long getCurrentUserId() {
		if (mCurrentUser == null) {
			return 0;
		} else {
			return mCurrentUser.getmUserId();
		}
	}
	
	public void setCurrentUser(User u) {
		this.mCurrentUser = u;
	}
}
