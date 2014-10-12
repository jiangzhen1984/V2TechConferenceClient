package com.V2.jni.ind;

import android.os.Parcel;
import android.os.Parcelable;

public class V2User implements Parcelable{

	
	public long uid;
	public String name;
	//2 means non-registered user
	public int type;
	
	
	public V2User() {
		
	}
	
	public V2User(long uid) {
		super();
		this.uid = uid;
	}
	public V2User(long uid, String name) {
		super();
		this.uid = uid;
		this.name = name;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(uid);
		dest.writeString(name);
	}
	
	public static final Parcelable.Creator<V2User> CREATOR = new Creator<V2User>() {

		@Override
		public V2User[] newArray(int i) {
			return new V2User[i];
		}

		@Override
		public V2User createFromParcel(Parcel parcel) {
			return new V2User(parcel.readLong(), parcel.readString());
		}
	};
}
