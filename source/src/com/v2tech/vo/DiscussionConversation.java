package com.v2tech.vo;

import com.v2tech.vo.Group.GroupType;

public class DiscussionConversation extends Conversation {

	private Group discussionGroup;
	public DiscussionConversation(Group discussion) {
		if (discussion == null)
			throw new NullPointerException("Given Discussion Group is null ... please check!");

		if (discussion.getGroupType() != GroupType.DISCUSSION) {
			throw new IllegalArgumentException(
					" Given The type of Group Object is not GroupType.DISCUSSION ... please check!");
		}
		
		this.discussionGroup = discussion;
		super.mExtId = discussionGroup.getmGId();
		super.mType = V2GlobalConstants.GROUP_TYPE_DISCUSSION;
	}

	@Override
	public String getName() {
		return discussionGroup.getName();
	}

	public Group getDiscussionGroup() {
		return discussionGroup;
	}

	public void setDiscussionGroup(Group discussionGroup) {
		this.discussionGroup = discussionGroup;
	}

}
