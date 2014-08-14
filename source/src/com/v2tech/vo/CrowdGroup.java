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

	/**
	 * 08-10 15:23:15.101: D/V2TECH(8144): OnInviteJoinGroup::==>3:<crowd
	 * announcement='' authtype='0' creatoruserid='121' id='45' name='ll'
	 * size='500' summary=''/>:<user account='guozm' bconf='1' bemail='1'
	 * bfilebox='1' birthday='2001-01-01' blive='1' bmix='1' bsip='1' bsms='1'
	 * bsystemavatar='1' bvod='1' id='121' nickname='guozm' orgboxid='1' sex='0'
	 * userboxid='22'/>:
	 * 
	 * @param xml
	 * @return
	 */
	public static Group parseXml(String xml , String userInfo) {
		String strId = null;
		String name = null;
		String owner = null;
		int start, end = -1;
		
		User mOwnerUser = null;
		String nickname = null;
		long userId = 0;
		start = xml.indexOf("createuserid='");
		if (start != -1) {
			end = xml.indexOf("'", start + 14);
			if (end != -1) {
				owner = xml.substring(start + 14, end);
			}
		}

		start = xml.indexOf(" id='");
		if (start != -1) {
			end = xml.indexOf("'", start + 5);
			if (end != -1) {
				strId = xml.substring(start + 5, end);
			}
		}

		start = xml.indexOf("name='");
		if (start != -1) {
			end = xml.indexOf("'", start + 6);
			if (end != -1) {
				name = xml.substring(start + 6, end);
			}
		}
		
		start = userInfo.indexOf(" nickname='");
		if (start != -1) {
			end = userInfo.indexOf("'", start + 11);
			if (end != -1) {
				nickname = userInfo.substring(start + 11, end);
			}
		}
		
		start = userInfo.indexOf(" id='");
		if (start != -1) {
			end = userInfo.indexOf("'", start + 5);
			if (end != -1) {
				userId = Long.parseLong(userInfo.substring(start + 5, end));
			}
		}

		mOwnerUser = new User(userId , nickname);
		Group g = new CrowdGroup(Long.parseLong(strId),name, owner, null);
		g.setOwnerUser(mOwnerUser);
		return g;
	}
}
