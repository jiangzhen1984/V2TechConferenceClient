package com.bizcom.bo;

import android.os.Parcel;
import android.os.Parcelable;

public class UserStatusObject implements Parcelable {

	private long uid;
	
	private int deviceType;
	
	private int status;
	
	public UserStatusObject(Parcel in) {
		if (in != null) {
			uid = in.readLong();
			deviceType = in.readInt();
			status = in.readInt();
		}
	}
	
	
	
	public UserStatusObject(long uid, int deviceType, int status) {
		super();
		this.uid = uid;
		this.deviceType = deviceType;
		this.status = status;
	}



	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel  out, int flag) {
		out.writeLong(uid);
		out.writeInt(deviceType);
		out.writeInt(status);
	}
	
	public static final Parcelable.Creator<UserStatusObject> CREATOR = new Parcelable.Creator<UserStatusObject>() {
	    public UserStatusObject createFromParcel(Parcel in) {
	        return new UserStatusObject(in);
	    }

	    public UserStatusObject[] newArray(int size) {
	        return new UserStatusObject[size];
	    }
	};

	public long getUid() {
		return uid;
	}



	public void setUid(long uid) {
		this.uid = uid;
	}



	public int getDeviceType() {
		return deviceType;
	}



	public void setDeviceType(int deviceType) {
		this.deviceType = deviceType;
	}



	public int getStatus() {
		return status;
	}



	public void setStatus(int status) {
		this.status = status;
	}

	
}
