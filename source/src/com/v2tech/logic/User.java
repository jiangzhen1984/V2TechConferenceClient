package com.v2tech.logic;

import android.util.Log;

public class User {

	private long mUserId;

	private NetworkStateCode mResult;

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

	public User(long mUserId, String mName, NetworkStateCode mResult) {
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

	public NetworkStateCode getmResult() {
		return mResult;
	}

	public void setmResult(NetworkStateCode mResult) {
		this.mResult = mResult;
	}

	public String getName() {
		return mName;
	}

	public void setName(String mName) {
		this.mName = mName;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		User other = (User) obj;
		if (mUserId != other.mUserId)
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (mUserId ^ (mUserId >>> 32));
		return result;
	}

	public static User fromXml(int uID, String xml) {
		String nickName = null;
		int pos = xml.indexOf("nickname='");
		if (pos == -1) {
			Log.w("ImRequest UI", " no nickname");
		} else {
			int end = xml.indexOf("'", pos + 10);
			if (end != -1) {
				nickName = xml.subSequence(pos + 10, end).toString();
			}
		}
		User u = new User();
		u.setmUserId(uID);
		u.setName(nickName);
		return u;
	}

}
