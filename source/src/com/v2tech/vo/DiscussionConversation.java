package com.v2tech.vo;

import java.util.List;

import com.V2.jni.V2GlobalEnum;
import com.v2tech.vo.Group.GroupType;

public class DiscussionConversation extends Conversation {

	private Group discussionGroup;
	private boolean showContact;

	public DiscussionConversation(Group discussion) {
		if (discussion == null)
			throw new NullPointerException("Given Discussion Group is null ... please check!");

		if (discussionGroup.getGroupType() != GroupType.DISCUSSION) {
			throw new IllegalArgumentException(
					" Given The type of Group Object is not GroupType.DISCUSSION ... please check!");
		}
		
		this.discussionGroup = discussion;
		super.mExtId = discussionGroup.getmGId();
		super.mType = V2GlobalEnum.GROUP_TYPE_DISCUSSION;
	}

	@Override
	public String getName() {
		if(discussionGroup != null)
			return discussionGroup.getName();
		else
			return getDiscussionNames();
	}

	private String getDiscussionNames() {
		StringBuilder sb = new StringBuilder();
		List<User> users = discussionGroup.getUsers();
		User ownerUser = discussionGroup.getOwnerUser();
		if(ownerUser != null){
			sb.append(ownerUser.getName());
		}
		if(users != null){
			for (User user : users) {
				sb.append(" ").append(user.getName());
			}
		}
		return sb.toString();
	}

	public boolean isShowContact() {
		return showContact;
	}

	public void setShowContact(boolean showContact) {
		this.showContact = showContact;
	}
}
