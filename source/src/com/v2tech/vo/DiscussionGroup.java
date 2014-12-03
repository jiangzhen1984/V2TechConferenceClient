package com.v2tech.vo;

import java.util.Date;
import java.util.List;

public class DiscussionGroup extends Group {

	public DiscussionGroup(long gId, String name, User owner, Date createDate) {
		super(gId, GroupType.DISCUSSION, name, owner, createDate);
	}

	public DiscussionGroup(long gId, String name, User owner) {
		super(gId, GroupType.DISCUSSION, name, owner);
	}

	@Override
	public String getName() {
		if (this.mName != null && !this.mName.isEmpty()) {
			return mName;
		}
		StringBuilder sb = new StringBuilder();
		List<User> users = getUsers();
		sb.append(this.mOwnerUser.getName());
		if (users != null) {
			for (User user : users) {
				if (user.getmUserId() == this.mOwnerUser.getmUserId())
					continue;

				sb.append(" ").append(user.getName());
				if (sb.toString().length() >= 30)
					break;
			}
		}
		return sb.toString();

	}

	@Override
	public String toXml() {
		StringBuilder xml = new StringBuilder();
		xml.append("<discussion creatoruserid='"
				+ this.getOwnerUser().getmUserId() + "' name='" + this.mName
				+ "'/>");
		return xml.toString();
	}

}
