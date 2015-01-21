package com.bizcom.bo;

import android.os.Parcel;
import android.os.Parcelable;

public class ConversationNotificationObject implements Parcelable {

	private int conversationType;
	private long extId;
	private long msgID; // P2PText notificateConversationUpdate
	private boolean isDeleteConversation;

	public ConversationNotificationObject(Parcel in) {
		conversationType = in.readInt();
		extId = in.readLong();
		msgID = in.readLong();
		int deleteInt = in.readInt();
		if (deleteInt == 1)
			isDeleteConversation = true;
		else
			isDeleteConversation = false;
	}

	public ConversationNotificationObject(int conversationType, long extId) {
		this(conversationType, extId, false, -1);
	}

	public ConversationNotificationObject(int conversationType, long extId,
			boolean isDeleteConversation) {
		this(conversationType, extId, isDeleteConversation, -1);
	}

	public ConversationNotificationObject(int conversationType, long extId,
			boolean isDeleteConversation, long msgID) {
		super();
		this.conversationType = conversationType;
		this.extId = extId;
		this.isDeleteConversation = isDeleteConversation;
		this.msgID = msgID;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel par, int flag) {
		par.writeInt(conversationType);
		par.writeLong(extId);
		par.writeLong(msgID);
		par.writeInt(isDeleteConversation ? 1 : 0);
	}

	public static final Parcelable.Creator<ConversationNotificationObject> CREATOR = new Parcelable.Creator<ConversationNotificationObject>() {
		public ConversationNotificationObject createFromParcel(Parcel in) {
			return new ConversationNotificationObject(in);
		}

		public ConversationNotificationObject[] newArray(int size) {
			return new ConversationNotificationObject[size];
		}
	};

	public int getConversationType() {
		return conversationType;
	}

	public void setConversationType(int conversationType) {
		this.conversationType = conversationType;
	}

	public long getExtId() {
		return extId;
	}

	public void setExtId(long extId) {
		this.extId = extId;
	}

	public long getMsgID() {
		return msgID;
	}

	public void setMsgID(long msgID) {
		this.msgID = msgID;
	}

	public boolean isDeleteConversation() {
		return isDeleteConversation;
	}

	public void setDeleteConversation(boolean isDeleteConversation) {
		this.isDeleteConversation = isDeleteConversation;
	}
}
