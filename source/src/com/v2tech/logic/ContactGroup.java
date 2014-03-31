package com.v2tech.logic;

public class ContactGroup extends Group {

	public ContactGroup(long mGId, GroupType mGroupType, String mName) {
		super(mGId, mGroupType, mName);
	}

	
	public ContactGroup(long mGId, String mName) {
		super(mGId, GroupType.CONTACT, mName);
	}
}
