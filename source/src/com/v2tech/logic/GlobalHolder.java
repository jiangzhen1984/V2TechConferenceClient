package com.v2tech.logic;

import java.util.HashMap;
import java.util.Map;

public class GlobalHolder {

	private static GlobalHolder holder;
	
	private User mCurrentUser;
	
	private Map<Long, User.Status> onlineUsers = new HashMap<Long, User.Status>();
	
	

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
		this.mCurrentUser.updateStatus(User.Status.ONLINE);
		User mU = getUser(u.getmUserId());
		if (mU != null) {
			mU.updateStatus(User.Status.ONLINE);
		} else {
			putUser(u.getmUserId(), u);
		}
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
	
	public void updateUserStatus(User u) {
		Long key = Long.valueOf(u.getmUserId());
		if (u.getmStatus() == User.Status.OFFLINE) {
			onlineUsers.remove(key);
		} else {
			onlineUsers.put(key, u.getmStatus());
		}
		
	}
	
	public void updateUserStatus(long uid, User.Status us) {
		Long key = Long.valueOf(uid);
		if (us == User.Status.OFFLINE) {
			onlineUsers.remove(key);
		} else {
			onlineUsers.put(key, us);
		}
	}
	
	public User.Status getOnlineUserStatus(long uid) {
		Long key = Long.valueOf(uid);
		return  onlineUsers.get(key);
	}
}
