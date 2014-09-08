package com.v2tech.vo;


public class ContactGroup extends Group {
	
	
	public ContactGroup(long mGId, String mName) {
		super(mGId, Group.GroupType.CONTACT, mName, null, null);
	}

	
	public String toXml() {
		return "<friendgroup "+(this.mGId == 0? "" : "id=\""+mGId+"\"")+"  name=\""+this.mName+"\" />";
	}

}
