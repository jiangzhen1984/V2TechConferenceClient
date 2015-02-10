package com.bizcom.bo;

import android.os.Parcel;
import android.os.Parcelable;

public class MessageObject implements Parcelable {

	public int groupType;
	public long remoteGroupID;
	public long rempteUserID;
	public long messageColsID;

	public MessageObject(Parcel in) {
		if (in != null) {
			groupType = in.readInt();
			remoteGroupID = in.readLong();
			rempteUserID = in.readLong();
			messageColsID = in.readLong();
		}
	}

	public MessageObject(int groupType, long remoteGroupID, long rempteUserID,
			long messageColsID) {
		super();
		this.groupType = groupType;
		this.remoteGroupID = remoteGroupID;
		this.rempteUserID = rempteUserID;
		this.messageColsID = messageColsID;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(groupType);
		dest.writeLong(remoteGroupID);
		dest.writeLong(rempteUserID);
		dest.writeLong(messageColsID);
	}

	public static final Parcelable.Creator<MessageObject> CREATOR = new Parcelable.Creator<MessageObject>() {
		public MessageObject createFromParcel(Parcel in) {
			return new MessageObject(in);
		}

		public MessageObject[] newArray(int size) {
			return new MessageObject[size];
		}
	};
}
