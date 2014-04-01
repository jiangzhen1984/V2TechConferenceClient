package com.v2tech.logic;

public class OrgGroup extends Group {

	public OrgGroup(long mGId, GroupType mGroupType, String mName) {
		super(mGId, mGroupType, mName);
	}

	
	public OrgGroup(long mGId, String mName) {
		super(mGId, GroupType.ORG, mName);
	}
}
