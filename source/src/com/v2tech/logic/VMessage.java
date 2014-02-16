package com.v2tech.logic;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class VMessage {

	public enum MessageType {
		TEXT(1), IMAGE(2);

		private int code;

		private MessageType(int code) {
			this.code = code;
		}

		public int getIntValue() {
			return code;
		}
	}

	private static DateFormat sfL = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	private static DateFormat sfT = new SimpleDateFormat("HH:mm");

	private User mUser;

	private User mToUser;

	MessageType mType;

	private String mText;

	private Date mDate;

	private boolean isLocal;

	private String mStrDateTime;

	public VMessage(User u, User toUser) {
		this(u, toUser, null, false);
	}

	public VMessage(User u, User toUser, String text) {
		this(u, toUser, text, false);
	}

	public VMessage(User u, User toUser, boolean isRemote) {
		this(u, toUser, null, isRemote);
	}

	public VMessage(User u, User toUser, String text, boolean isRemote) {
		this.mUser = u;
		this.mToUser = toUser;
		this.mText = text;
		this.mDate = new Date();
		this.mType = MessageType.TEXT;
		this.isLocal = !isRemote;

		if (System.currentTimeMillis() / (24 * 36000) == this.mDate.getTime()
				/ (24 * 36000)) {
			mStrDateTime = sfL.format(this.mDate);
		} else {
			mStrDateTime = sfT.format(this.mDate);
		}

	}

	public User getUser() {
		return mUser;
	}

	public void setUser(User mUser) {
		this.mUser = mUser;
	}

	public MessageType getType() {
		return mType;
	}

	public void setType(MessageType mType) {
		this.mType = mType;
	}

	public String getText() {
		return mText;
	}

	public void setText(String mText) {
		this.mText = mText;
	}

	public Date getDate() {
		return mDate;
	}

	public void setDate(Date mDate) {
		if (mDate == null) {
			return;
		}
		this.mDate = mDate;
		if (System.currentTimeMillis() / (24 * 36000) == this.mDate.getTime()
				/ (24 * 36000)) {
			mStrDateTime = sfL.format(this.mDate);
		} else {
			mStrDateTime = sfT.format(this.mDate);
		}
	}


	public boolean isLocal() {
		return isLocal;
	}

	public void setLocal(boolean isLocal) {
		this.isLocal = isLocal;
	}

	public String getDateTimeStr() {
		return this.mStrDateTime;
	}

	public User getToUser() {
		return this.mToUser;
	}

	public String toXml() {
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
				.append("<TChatData IsAutoReply=\"False\">\n")
				.append("<FontList>\n")
				.append("<TChatFont Color=\"255\" Name=\"Segoe UI\" Size=\"18\" Style=\"\"/>")
				.append("</FontList>\n")
				.append("<ItemList>\n")
				.append("<TTextChatItem NewLine=\"True\" FontIndex=\"0\" Text=\""
						+ this.mText + "\"/>").append("    </ItemList>")
				.append("</TChatData>");
		return sb.toString();
	}
}
