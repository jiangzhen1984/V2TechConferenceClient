package com.v2tech.logic;

public class User {
	
	private long mUserId;
	
	private int mResult;
	
	private String mName;
	
	public User() {
		
	}

	public User(long mUserId) {
		super();
		this.mUserId = mUserId;
	}

	public User(long mUserId, String mName) {
		super();
		this.mUserId = mUserId;
		this.mName = mName;
	}

	public User(long mUserId, int mResult, String mName) {
		super();
		this.mUserId = mUserId;
		this.mResult = mResult;
		this.mName = mName;
	}

	public long getmUserId() {
		return mUserId;
	}

	public void setmUserId(long mUserId) {
		this.mUserId = mUserId;
	}

	public int getmResult() {
		return mResult;
	}

	public void setmResult(int mResult) {
		this.mResult = mResult;
	}

	public String getName() {
		return mName;
	}

	public void setName(String mName) {
		this.mName = mName;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		} else {
			return this.mUserId == ((User)o).mUserId;
		}
	}

	@Override
	public int hashCode() {
		return (int)mUserId;
	}
	
	
	
}
