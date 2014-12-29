package com.v2tech.vo;



public class ConferenceConversation extends Conversation {

	private Group g;

	public ConferenceConversation(Group g) {
		super();
		if (g == null) {
			throw new NullPointerException(" group is null");
		}
		this.g = g;
		super.mExtId = g.getmGId();
		super.mType = TYPE_CONFERNECE;
	}

	@Override
	public String getName() {
		if (g != null) {
			return g.getName();
		}
		return super.getName();
	}

	@Override
	public CharSequence getMsg() {
		if (g != null) {
			User u = g.getOwnerUser();
			// TODO need use localization
			return u == null ? "" : "创建人:" + u.getName();
		}
		return super.getMsg();
	}

	@Override
	public String getDate() {
		if (g != null) {
			return g.getStrCreateDate();
		}
		return super.getDate();
	}

	public Group getGroup() {
		return g;
	}

	public void setG(Group g) {
		this.g = g;
	}
}
