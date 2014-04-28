package com.v2tech.vo;



public class ConferenceGroup extends Group {
	
	
	private boolean isSyn;

	public ConferenceGroup(long mGId, String mName) {
		super(mGId, GroupType.CONFERENCE, mName);
	}

	public ConferenceGroup(long mGId, GroupType mGroupType, String mName,
			String mOwner, String createDate) {
		super(mGId, GroupType.CONFERENCE, mName, mOwner, createDate);
	}

	public ConferenceGroup(long mGId, GroupType mGroupType, String mName) {
		super(mGId, GroupType.CONFERENCE, mName);
	}

	
	
	
	public boolean isSyn() {
		return isSyn;
	}

	public void setSyn(boolean isSyn) {
		this.isSyn = isSyn;
	}

	/**
	 * <conf createuserid='1138' id='513956640327' starttime='2012' subject='
	 * 啊'/> <user id='1138' uetype='2'/>
	 * 
	 * <conf createuserid='1121' id='513968489010' starttime='1396848297' subject='444'/>
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
			
			Group g = new ConferenceGroup(Long.parseLong(strId), GroupType.CONFERENCE,
					name, owner, startTimeStr);
			return g;
	}
		
	
}
