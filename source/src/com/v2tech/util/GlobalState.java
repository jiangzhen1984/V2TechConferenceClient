package com.v2tech.util;

public final class GlobalState {

	private int state;
	private long gid;
	private long uid;
	
	
	
	
	public GlobalState() {
		super();
	}
	
	
	public GlobalState(GlobalState gstate) {
		this.state = gstate.state;
		this.gid = gstate.gid;
		this.uid = gstate.uid;
	}
	public int getState() {
		return state;
	}
	public void setState(int state) {
		this.state = state;
	}
	public long getGid() {
		return gid;
	}
	public void setGid(long gid) {
		this.gid = gid;
	}
	public long getUid() {
		return uid;
	}
	public void setUid(long uid) {
		this.uid = uid;
	}
	
	
}
