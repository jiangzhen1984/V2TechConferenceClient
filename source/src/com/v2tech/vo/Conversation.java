package com.v2tech.vo;

public class Conversation {

	public static final String TYPE_CONFERNECE = "1";

	public static final String TYPE_CONTACT = "2";

	public static final String TYPE_GROUP = "3";

	public static final int READ_FLAG_READ = 1;
	public static final int READ_FLAG_UNREAD = 0;

	private int mId;

	protected String mType;

	protected long mExtId;

	protected int readFlag;

	protected Conversation() {

	}
	
	
	public Conversation(int mId, String mType, long mExtId, int readFlag) {
		super();
		this.mId = mId;
		this.mType = mType;
		this.mExtId = mExtId;
		this.readFlag = readFlag;
	}

	public Conversation(int mId, String mType, long mExtId) {
		this(0, mType, mExtId, READ_FLAG_UNREAD);
	}

	public Conversation(String mType, long mExtId) {
		this(0, mType, mExtId);
	}

	public String getName() {
		return null;
	}

	public CharSequence getMsg() {
		return null;
	}

	public String getDate() {
		return null;
	}

	public void setMsg(CharSequence msg) {
	}

	public void setDate(String date) {
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
		result = prime * result + ((mType == null) ? 0 : mType.hashCode());
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
		if (mType == null) {
			if (other.mType != null)
				return false;
		} else if (!mType.equals(other.mType))
			return false;
		return true;
	}

	public int getId() {
		return mId;
	}

	public void setId(int id) {
		this.mId = id;
	}

	public String getType() {
		return mType;
	}

	public void setType(String type) {
		this.mType = type;
	}

	public long getExtId() {
		return mExtId;
	}

	public void setExtId(long extId) {
		this.mExtId = extId;
	}

}
