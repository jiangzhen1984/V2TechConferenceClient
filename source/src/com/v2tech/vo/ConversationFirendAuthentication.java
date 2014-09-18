package com.v2tech.vo;

public class ConversationFirendAuthentication extends Conversation{
	
	private CharSequence msg;
	public ConversationFirendAuthentication(int mType, long mExtId){
		super(mType,mExtId);
	}


	
	public CharSequence getMsg() {
		return msg;
	}


	public void setMsg(CharSequence msg) {
		this.msg=msg;
	}
	
}
