package com.v2tech.vo;

import com.v2tech.util.DateUtil;
import com.v2tech.vo.Group.GroupType;

public class CrowdConversation extends Conversation {
	
	private Group g;
	private User lastSendUser;
	private String msg;

	public CrowdConversation(Group g) {
		if (g == null) {
			throw new NullPointerException(" group is null");
		}
		if (g.getGroupType() != GroupType.CHATING) {
			throw new IllegalArgumentException(" group type is not GroupType.CHATING or oupType.ORG ");
		}
		this.g = g;
		super.mExtId = g.getmGId();
		super.mType = TYPE_GROUP;
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
			// TODO need use localization
			return u == null ? "" : "创建人:" + u.getName();
		}
		return msg;
	}

	@Override
	public String getDate() {
		if(dateLong != null){
			return DateUtil.getStringDate(Long.valueOf(dateLong));
		}
		return super.getDate();
	}
	
	@Override
	public String getDateLong() {
		if (dateLong != null) {
			return dateLong;
		}
		return super.getDateLong();
	}
	
	public void setMsg(String msg) {
		this.msg = msg;
	}
	
	public void setDate(String date) {
		this.date = date;
	}



	public User getLastSendUser() {
		return lastSendUser;
	}



	public void setLastSendUser(User lastSendUser) {
		this.lastSendUser = lastSendUser;
	}
	
	public Group getGroup() {
		return g;
	}
	
	
}
