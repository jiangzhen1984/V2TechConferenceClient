package com.v2tech.vo;

import java.util.Date;


public abstract class VMessageQualification {
	
	
	public enum Type {
		CROWD_INVITATION(0), CROWD_APPLICATION(1), CONTACT(2);
		
		private int type;
		private Type(int type){
			this.type = type;
		}
		public static Type fromInt(int code) {
			switch (code) {
				case 0:
					return CROWD_INVITATION;
				case 1:
					return CROWD_APPLICATION;
				case 2:
					return CONTACT;
			}
			return null;
		}
		
		public int intValue() {
			return type;
		}
	}
	
	public enum ReadState {
		UNREAD(0), READ(1);
		
		private int type;
		private ReadState(int type){
			this.type = type;
		}
		public static ReadState fromInt(int code) {
			switch (code) {
				case 0:
					return UNREAD;
				case 1:
					return READ;
			}
			return null;
		}
		
		public int intValue() {
			return type;
		}
	}
	
	public enum QualificationState {
		WAITING(0),ACCEPTED(1),REJECT(2);
		
		private int type;
		private QualificationState(int type){
			this.type = type;
		}
		public static QualificationState fromInt(int code) {
			switch (code) {
				case 0:
					return WAITING;
				case 1:
					return ACCEPTED;
				case 2:
					return REJECT;
			}
			return null;
		}
		
		public int intValue() {
			return type;
		}
	}
	
	protected long mId;
	protected Type mType;
	protected String mRejectReason;
	protected Date mTimestamp;
	protected ReadState mReadState;
	protected QualificationState mQualState;
	
	
	/**
	 * 
	 * @param crowd
	 * @param invitationUser
	 * @param beInvitatonUser
	 */
	protected VMessageQualification(Type type) {
		this.mType = type;
		this.mReadState = ReadState.UNREAD;
		this.mQualState = QualificationState.WAITING;
		
	}

	
	

	public long getId() {
		return mId;
	}




	public void setId(long id) {
		this.mId = id;
	}


	public String getRejectReason() {
		return mRejectReason;
	}


	public void setRejectReason(String rejectReason) {
		this.mRejectReason = rejectReason;
	}



	public Type getType() {
		return mType;
	}




	public ReadState getReadState() {
		return mReadState;
	}



	public void setReadState(ReadState readState) {
		this.mReadState = readState;
	}



	public QualificationState getQualState() {
		return mQualState;
	}



	public void setQualState(QualificationState qualState) {
		this.mQualState = qualState;
	}
	
	
	
	
}
