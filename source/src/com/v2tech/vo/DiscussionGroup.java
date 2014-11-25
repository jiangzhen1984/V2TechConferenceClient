package com.v2tech.vo;

import java.util.Date;

public class DiscussionGroup extends Group {

	
	
	
	public DiscussionGroup(long gId, String name,
			User owner, Date createDate) {
		super(gId, GroupType.DISCUSSION, name, owner, createDate);
	}

	public DiscussionGroup(long gId, String name,
			User owner) {
		super(gId, GroupType.DISCUSSION, name, owner);
	}

	@Override
	public String toXml() {
		StringBuilder xml = new StringBuilder();
		xml.append("<discussion creatoruserid='"+this.getOwnerUser().getmUserId()+"' name='"+this.mName+"'/>");
		return xml.toString();
	}

}
