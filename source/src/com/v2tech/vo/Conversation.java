package com.v2tech.vo;

import com.V2.jni.V2GlobalEnum;

public class Conversation {

	public static final int TYPE_CONFERNECE = V2GlobalEnum.GROUP_TYPE_CONFERENCE;

	public static final int TYPE_CONTACT = V2GlobalEnum.GROUP_TYPE_USER;

	public static final int TYPE_GROUP = V2GlobalEnum.GROUP_TYPE_DEPARTMENT;

	public static final int TYPE_VOICE_MESSAGE = 7;

	public static final int TYPE_VERIFICATION_MESSAGE = 8;
	
	public static final int TYPE_CROWD_VERIFICATION_MESSAGE = 9;

	public static final int READ_FLAG_READ = 1;
	public static final int READ_FLAG_UNREAD = 0;

	private int mId;

	protected int mType;

	protected long mExtId;

	protected int readFlag;

	protected String date;

	protected String dateLong;

	protected CharSequence msg;

	protected Conversation() {
	}

	public Conversation(int mId, int mType, long mExtId, int readFlag) {
		super();
		this.mId = mId;
		this.mType = mType;
		this.mExtId = mExtId;
		this.readFlag = readFlag;
	}

	public Conversation(int mId, int mType, long mExtId) {
		this(0, mType, mExtId, READ_FLAG_UNREAD);
	}

	public Conversation(int mType, long mExtId) {
		this(0, mType, mExtId);
	}

	public String getDateLong() {
		return dateLong;
	}

	public void setDateLong(String dateLong) {
		this.dateLong = dateLong;
	}

	public String getName() {
		return null;
	}

	public CharSequence getMsg() {
		return msg;
	}

	public String getDate() {
		return date;
	}

	public void setMsg(CharSequence msg) {
		this.msg = msg;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public int getReadFlag() {
		return readFlag;
	}

	public void setReadFlag(int readFlag) {
		this.readFlag = readFlag;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (mExtId ^ (mExtId >>> 32));
		// result = prime * result + ((mType == null) ? 0 : mType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Conversation other = (Conversation) obj;
		if (mExtId != other.mExtId)
			return false;
		// if (mType == null) {
		// if (other.mType != null)
		// return false;
		// }
		// else if (!mType.equals(other.mType))
		// return false;
		return true;
	}

	public int getId() {
		return mId;
	}

	public void setId(int id) {
		this.mId = id;
	}

	public int getType() {
		return mType;
	}

	public void setType(int type) {
		this.mType = type;
	}

	public long getExtId() {
		return mExtId;
	}

	public void setExtId(long extId) {
		this.mExtId = extId;
	}

}
