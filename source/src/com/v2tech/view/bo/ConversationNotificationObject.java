package com.v2tech.view.bo;

import android.os.Parcel;
import android.os.Parcelable;

import com.v2tech.vo.Conversation;

public class ConversationNotificationObject implements Parcelable {

	
	private int cid;
	private int type;
	private long extId; 
	
	public ConversationNotificationObject(Parcel in) {
		cid = in.readInt();
		type = in.readInt();
		extId = in.readLong();
	}
	
	public ConversationNotificationObject(int type, long extId) {
		this.type = type;
		this.extId = extId;
	}
	
	
	public ConversationNotificationObject(Conversation cov) {
		if (cov != null) {
			this.cid = cov.getId();
			this.type= cov.getType();
			this.extId = cov.getExtId();
		}
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel par, int flag) {
		par.writeInt(cid);
		par.writeInt(type);
		par.writeLong(extId);
	}

	
	
	public static final Parcelable.Creator<ConversationNotificationObject> CREATOR = new Parcelable.Creator<ConversationNotificationObject>() {
	    public ConversationNotificationObject createFromParcel(Parcel in) {
	        return new ConversationNotificationObject(in);
	    }

	    public ConversationNotificationObject[] newArray(int size) {
	        return new ConversationNotificationObject[size];
	    }
	};

	public int getCid() {
		return cid;
	}

	public void setCid(int cid) {
		this.cid = cid;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public long getExtId() {
		return extId;
	}

	public void setExtId(long extId) {
		this.extId = extId;
	}
	
	
	
	
}
