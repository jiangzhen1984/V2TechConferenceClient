package com.v2tech.logic;

import android.graphics.Bitmap;

public class ContactConversation extends Conversation {
	
	private User u;
	private String msg;
	private String date;
	
	public ContactConversation(User u) {
		super();
		this.u = u;
		if (u != null) {
			this.mExtId =u.getmUserId();
			this.mType = TYPE_CONTACT;
			this.mNotiFlag = NONE;
		}
	}
	
	
	public ContactConversation(User u, int nofiFlag) {
		super();
		this.u = u;
		if (u != null) {
			this.mExtId =u.getmUserId();
			this.mType = TYPE_CONTACT;
			this.mNotiFlag = nofiFlag;
		}
	}
	

	@Override
	public String getName() {
		if (u != null) {
			return u.getName();
		}
		return super.getName();
	}

	@Override
	public String getMsg() {
		return msg;
	}

	@Override
	public String getDate() {
		return date;
	}
	
	public void setMsg(String msg) {
		this.msg = msg;
	}
	
	public void setDate(String date) {
		this.date = date;
	}
	
	public Bitmap getAvatar() {
		if (u != null) {
			return u.getAvatarBitmap();
		} else {
			return null;
		}
	}
	
	public void updateUser(User u) {
		this.u = u;
	}
	
	
	
}
