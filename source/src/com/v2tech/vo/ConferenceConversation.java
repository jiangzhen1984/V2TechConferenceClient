package com.v2tech.vo;

import com.v2tech.util.GlobalConfig;
import com.v2tech.vo.Group.GroupType;


public class ConferenceConversation extends Conversation {

	private Group g;

	public ConferenceConversation(Group g) {
		super();
		this.g = g;
		if (g != null) {
			super.mExtId = g.getmGId();
			super.mType = g.getGroupType() == GroupType.CONFERENCE ? TYPE_CONFERNECE : TYPE_GROUP;
		}
	}

	@Override
	public String getName() {
		if (g != null) {
			return g.getName();
		}
		return super.getName();
	}

	@Override
	public CharSequence getMsg() {
		if (g != null) {
			User u = g.getOwnerUser();
			//TODO need use localization
			return u == null ? g.getOwner() + "" : GlobalConfig.Resource.ConversationCrator +":"+u.getName();
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
	
	
	public Group getGroup() {
		return g;
	}

}
