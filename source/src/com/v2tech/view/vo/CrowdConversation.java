package com.v2tech.view.vo;

import com.v2tech.logic.Group;
import com.v2tech.logic.User;
import com.v2tech.logic.Group.GroupType;

public class CrowdConversation extends Conversation {
	
	private Group g;

	public CrowdConversation(Group g) {
		if (g == null) {
			throw new NullPointerException(" group is null");
		}
		if (g.getGroupType() != GroupType.CHATING) {
			throw new IllegalArgumentException(" group type is not GroupType.CHATING");
		}
		this.g = g;
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
		if (g != null) {
			User u = g.getOwnerUser();
			return u == null ? g.getOwner() + "" : u.getName();
		}
		return super.getMsg();
	}

	@Override
	public String getDate() {
		if (g != null) {
			return g.getStrCreateDate();
		}
		return super.getDate();
	}
	
	
}
