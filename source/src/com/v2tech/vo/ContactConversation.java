package com.v2tech.vo;


import android.graphics.Bitmap;

public class ContactConversation extends Conversation {
	
	private User u;
	private CharSequence msg;
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
	public CharSequence getMsg() {
		return msg;
	}

	@Override
	public String getDate() {
		return date;
	}
	
	public void setMsg(CharSequence msg) {
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
