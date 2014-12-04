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
		if(mOwnerUser != null){
			sb.append(mOwnerUser.getName());
			for (User user : users) {
				if (user.getmUserId() == this.mOwnerUser.getmUserId())
					continue;

				sb.append(" ").append(user.getName());
				if (sb.toString().length() >= 30)
					break;
			}
		}
		else{
			if (users != null) {
				for (int i = 0 ; i < users.size() ; i++) {
					if(i == 0)
						sb.append(users.get(i).getName());	
					else
						sb.append(" ").append(users.get(i).getName());
					if (sb.toString().length() >= 30)
						break;
				}
			}
		}
		return sb.toString();

	}

	@Override
	public String toXml() {
		StringBuilder xml = new StringBuilder();
		xml.append("<discussion "
				+ (this.mGId > 0 ? (" id='" + this.mGId + "' ") : "")
				+ " creatoruserid='" + this.getOwnerUser().getmUserId()
				+ "' name='" + this.mName + "'/>");
		return xml.toString();
	}

}
