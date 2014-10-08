package com.v2tech.vo;

public class ConversationFirendAuthenticationData extends Conversation{
	
	private CharSequence msg;
	private User user;
	public ConversationFirendAuthenticationData(int mType, long mExtId){
		super(mType,mExtId);
	}


	
	public CharSequence getMsg() {
		return msg;
	}


	public void setMsg(CharSequence msg) {
		this.msg=msg;
	}
	
	
	public User getUser() {
		return user;
	}


	public void setUser(User user) {
		this.user = user;
	}
}
