package com.bizcom.bo;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

public class UserAvatarObject implements Parcelable {

	private long mUId;
	
	private String mAvatarPath;
	
	/**
	 * Never will be parcel
	 */
	private Bitmap bm;
	
	public UserAvatarObject(Parcel in) {
		if (in != null) {
			mUId = in.readLong();
			mAvatarPath = in.readString();
		}
	}
	
	
	
	public UserAvatarObject(long uId, String avatarPath) {
		super();
		this.mUId = uId;
		this.mAvatarPath = avatarPath;
	}



	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel  out, int flag) {
		out.writeLong(mUId);
		out.writeString(mAvatarPath);
	}
	
	public static final Parcelable.Creator<UserAvatarObject> CREATOR = new Parcelable.Creator<UserAvatarObject>() {
	    public UserAvatarObject createFromParcel(Parcel in) {
	        return new UserAvatarObject(in);
	    }

	    public UserAvatarObject[] newArray(int size) {
	        return new UserAvatarObject[size];
	    }
	};

	public long getUId() {
		return mUId;
	}



	public void setUId(long mUId) {
		this.mUId = mUId;
	}



	public String getAvatarPath() {
		return mAvatarPath;
	}



	public void setAvatarPath(String mAvatarPath) {
		this.mAvatarPath = mAvatarPath;
	}



	public Bitmap getBm() {
		return bm;
	}



	public void setBm(Bitmap bm) {
		this.bm = bm;
	}

	

	
}
