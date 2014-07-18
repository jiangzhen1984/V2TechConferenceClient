package com.v2tech.vo;

public class CrowdGroup extends Group {

	public CrowdGroup(long mGId, String mName) {
		super(mGId, GroupType.CHATING, mName);
	}

	public CrowdGroup(long mGId, String mName, String mOwner, String createDate) {
		super(mGId, GroupType.CHATING, mName, mOwner, createDate);
	}

	public CrowdGroup(long mGId, String mName, long mOwner) {
		super(mGId, GroupType.CHATING, mName, mOwner);
	}

	public String toGroupXml() {
		StringBuffer sb = new StringBuffer();
		sb.append("<crowd name=\"" + this.mName + "\" size=\"100\"  />");
		return sb.toString();
	}

	public String toGroupUserListXml() {
		StringBuffer sb = new StringBuffer();
		sb.append("<userlist>");
		for (User u : this.users) {
			sb.append(" <user id=\"" + u.getmUserId() + "\" />");
		}
		sb.append("</userlist>");
		return sb.toString();
	}

}
