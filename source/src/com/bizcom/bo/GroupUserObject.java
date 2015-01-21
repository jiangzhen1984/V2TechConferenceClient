package com.bizcom.bo;

import android.os.Parcel;
import android.os.Parcelable;

public class GroupUserObject implements Parcelable {

	private int mType;

	private long mGroupId;

	private long mUserId;

	public GroupUserObject(Parcel in) {
		if (in != null) {
			mType = in.readInt();
			mGroupId = in.readLong();
			mUserId = in.readLong();
		}
	}

	public GroupUserObject(int mType, long mGroupId, long mUserId) {
		super();
		this.mType = mType;
		this.mGroupId = mGroupId;
		this.mUserId = mUserId;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flag) {
		out.writeInt(mType);
		out.writeLong(mGroupId);
		out.writeLong(mUserId);
	}

	public static final Parcelable.Creator<GroupUserObject> CREATOR = new Parcelable.Creator<GroupUserObject>() {
		public GroupUserObject createFromParcel(Parcel in) {
			return new GroupUserObject(in);
		}

		public GroupUserObject[] newArray(int size) {
			return new GroupUserObject[size];
		}
	};

	public int getmType() {
		return mType;
	}

	public void setmType(int mType) {
		this.mType = mType;
	}

	public long getmGroupId() {
		return mGroupId;
	}

	public void setmGroupId(long mGroupId) {
		this.mGroupId = mGroupId;
	}

	public long getmUserId() {
		return mUserId;
	}

	public void setmUserId(long mUserId) {
		this.mUserId = mUserId;
	}

}
