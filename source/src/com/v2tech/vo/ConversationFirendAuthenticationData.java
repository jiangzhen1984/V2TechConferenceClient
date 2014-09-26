package com.v2tech.vo;

public class ConversationFirendAuthenticationData extends Conversation{
	
	private CharSequence msg;
	public ConversationFirendAuthenticationData(int mType, long mExtId){
		super(mType,mExtId);
	}


	
	public CharSequence getMsg() {
		return msg;
	}


	public void setMsg(CharSequence msg) {
		this.msg=msg;
	}
	
}
