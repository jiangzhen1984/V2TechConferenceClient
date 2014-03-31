package com.v2tech.logic;

public class FriendGroup extends Group {
	
	
	public FriendGroup(long mGId, String mName) {
		super(mGId, Group.GroupType.FRIEND, mName);
	}

	public FriendGroup(long mGId, GroupType mGroupType, String mName) {
		super(mGId, mGroupType, mName);
	}

	public FriendGroup(long mGId, GroupType mGroupType, String mName,
			String mOwner, String createDate) {
		super(mGId, mGroupType, mName, mOwner, createDate);
	}

}
