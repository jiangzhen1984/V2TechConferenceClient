package com.v2tech.vo;


public class FriendGroup extends Group {
	public FriendGroup(long mGId, String mName) {
		super(mGId, Group.GroupType.CONTACT, mName, null, null);
	}

	public FriendGroup(long mGId, GroupType mGroupType, String mName) {
		super(mGId, Group.GroupType.CONTACT, mName, null, null);
	}

	public FriendGroup(long mGId, GroupType mGroupType, String mName,
			String mOwner, String createDate) {
		super(mGId, Group.GroupType.CONTACT, mName, null, null);
	}

	@Override
	public String toXml() {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
	

}
