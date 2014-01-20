package com.v2tech.logic;

import java.util.List;

/**
 * @deprecated
 * @author jiangzhen
 *
 */
public class GlobalHolder {

	private static GlobalHolder gh = new GlobalHolder();
	
	private User u;
	
	private List<Group> mList;
	
	private GlobalHolder() {
		
	}
	
	public static GlobalHolder getInstance() {
		if (gh == null) {
			gh = new GlobalHolder();
		}
		return gh;
	}
	
	public static long getLoggedUserId() {
		return getInstance().getUser() == null ? 0 : getInstance().getUser().getmUserId();
	}
	
	
	public User getUser() {
		return u;
	}

	public void setUser(User u) {
		this.u = u;
	}

	public List<Group> getList() {
		return mList;
	}

	public void setList(List<Group> mList) {
		this.mList = mList;
	}
	
	
}
