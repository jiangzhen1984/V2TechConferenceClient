package com.v2tech.vo;



public class NormalGroup extends Group {

	public NormalGroup(long mGId, GroupType mGroupType, String mName) {
		super(mGId, GroupType.CHATING, mName);
	}

	public NormalGroup(long mGId, GroupType mGroupType, String mName,
			String mOwner, String createDate) {
		super(mGId, GroupType.CHATING, mName, mOwner, createDate);
	}

	public NormalGroup(long mGId, GroupType mGroupType, String mName,
			long mOwner) {
		super(mGId, GroupType.CHATING, mName, mOwner);
	}

	
	
}
