package com.v2tech.logic;

import java.util.HashMap;
import java.util.Map;

public class GlobalHolder {

	private static GlobalHolder holder;
	
	private User mCurrentUser;
	
	

	private Map<Long, User> mUserHolder = new HashMap<Long, User>();
	
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
	
	
	public void putUser(long id, User u) {
		if (u == null) {
			return;
		}
		Long key = Long.valueOf(id);
		mUserHolder.put(key, u);
	}
	
	public User getUser(long id) {
		Long key = Long.valueOf(id);
		return mUserHolder.get(key);
	}
}
