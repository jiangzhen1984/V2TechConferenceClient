package com.v2tech.view.vo;

import com.v2tech.logic.Group;
import com.v2tech.logic.Group.GroupType;
import com.v2tech.logic.User;

public class CrowdConversation extends Conversation {
	
	private Group g;
	private User lastSendUser;
	private String msg;
	private String date;

	public CrowdConversation(Group g) {
		if (g == null) {
			throw new NullPointerException(" group is null");
		}
		if (g.getGroupType() != GroupType.CHATING) {
			throw new IllegalArgumentException(" group type is not GroupType.CHATING");
		}
		this.g = g;
		super.mExtId = g.getmGId();
		super.mType = TYPE_GROUP;
		super.mNotiFlag = NONE;
	}

	

	@Override
	public String getName() {
		if (g != null) {
			return g.getName();
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
	
	
}
