package com.v2tech.vo;

public class ConferenceGroup extends Group {

	private long chairManUId;
	private boolean isSyn;
	private boolean isCanInvitation = true;

	public ConferenceGroup(long mGId, String mName) {
		super(mGId, GroupType.CONFERENCE, mName);
	}

	public ConferenceGroup(long mGId, GroupType mGroupType, String mName,
			String mOwner, String createDate, long chairMan) {
		super(mGId, GroupType.CONFERENCE, mName, mOwner, createDate);
		this.chairManUId = chairMan;
	}

	public ConferenceGroup(long mGId, GroupType mGroupType, String mName) {
		super(mGId, GroupType.CONFERENCE, mName);
	}

	public long getChairManUId() {
		return chairManUId;
	}

	public void setChairManUId(long chairManUId) {
		this.chairManUId = chairManUId;
	}

	public boolean isSyn() {
		return isSyn;
	}

	public void setSyn(boolean isSyn) {
		this.isSyn = isSyn;
	}

	public boolean isCanInvitation() {
		return isCanInvitation;
	}

	public void setCanInvitation(boolean isCanInvitation) {
		this.isCanInvitation = isCanInvitation;
	}

	/**
	 * <conf createuserid='1138' id='513956640327' starttime='2012' subject='
	 * 啊'/> <user id='1138' uetype='2'/>
	 * 
	 * <conf createuserid='1121' id='513968489010' starttime='1396848297'
	 * subject='444'/>
	 * 
	 * @param type
	 * @param xml
	 * @return
	 */
	public static Group parseConferenceGroupFromXML(String xml) {
		String strId = null;
		String name = null;
		String owner = null;
		String startTimeStr = null;
		int start, end = -1;
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

		start = xml.indexOf("subject='");
		if (start != -1) {
			end = xml.indexOf("'", start + 9);
			if (end != -1) {
				name = xml.substring(start + 9, end);
			}
		}

		start = xml.indexOf("starttime='");
		if (start != -1) {
			end = xml.indexOf("'", start + 11);
			if (end != -1) {
				startTimeStr = xml.substring(start + 11, end);
			}
		}

		Group g = new ConferenceGroup(Long.parseLong(strId),
				GroupType.CONFERENCE, name, owner, startTimeStr, 0);
		return g;
	}

	@Override
	public int compareTo(Group g) {
		if (g.getCreateDate() == null) {
			return 1;
		}
		if (this.getCreateDate() == null) {
			return -1;
		}
		if (this.getCreateDate().before(g.getCreateDate())) {
			return -1;
		}

		return 1;
	}
	
	
	
	public static final  int EXTRA_FLAG_INVITATION = 0X0001;
	
	public static final  int EXTRA_FLAG_SYNC = 0X0002;
	/**
	 * 
	 * @param g
	 * @param xml
	 * @return
	 */
	public static int extraAttrFromXml(ConferenceGroup g, String xml) {
		int flag = 0;
		String val = extraValue(xml, "inviteuser='");
		if (val != null) {
			if (val.equals("1")) {
				g.setCanInvitation(true);
			} else {
				g.setCanInvitation(false);
			}
			flag |= EXTRA_FLAG_INVITATION;
		}

		String syncVal = extraValue(xml, "syncdesktop='");
		if (syncVal != null) {
			if (syncVal.equals("1")) {
				g.setSyn(true);
			} else {
				g.setSyn(false);
			}
			flag |= EXTRA_FLAG_SYNC;
		}
		return flag;
	}

	private static String extraValue(String xml, String str) {
		int start = xml.indexOf(str);
		if (start != -1) {
			int end = xml.indexOf("'", start + str.length());
			if (end != -1) {
				return xml.substring(start + str.length(), end);
			}
		}
		return null;
	}

}
