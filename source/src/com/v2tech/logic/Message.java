package com.v2tech.logic;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Message {

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

	MessageType mType;

	private String mText;

	private Date mDate;

	private boolean isLocal;

	private String mStrDateTime;

	public Message(User u) {
		this(u, null, false);
	}

	public Message(User u, String text) {
		this(u, text, false);
	}

	public Message(User u, boolean isRemote) {
		this(u, null, isRemote);
	}

	public Message(User u, String text, boolean isRemote) {
		this.mUser = u;
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

}
