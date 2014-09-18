package com.v2tech.vo;

import java.util.Date;


public abstract class VMessageQualification {
	
	
	public enum Type {
		CROWD(0), CONTACT(1);
		
		private int type;
		private Type(int type){
			this.type = type;
		}
		public static Type fromInt(int code) {
			switch (code) {
				case 0:
					return CROWD;
				case 1:
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
	protected User mInvitationUser;
	protected User mBeInvitatonUser;
	protected String mApplyReason;
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
	protected VMessageQualification(Type type, User invitationUser, User beInvitationUser) {
		this.mType = type;
		this.mInvitationUser = invitationUser;
		this.mBeInvitatonUser = beInvitationUser;
	}

	
	

	public long getId() {
		return mId;
	}




	public void setId(long id) {
		this.mId = id;
	}




	public User getInvitationUser() {
		return mInvitationUser;
	}


	public void setInvitationUser(User invitationUser) {
		this.mInvitationUser = invitationUser;
	}


	public User getBeInvitatonUser() {
		return mBeInvitatonUser;
	}


	public void setBeInvitatonUser(User beInvitatonUser) {
		this.mBeInvitatonUser = beInvitatonUser;
	}


	public String getApplyReason() {
		return mApplyReason;
	}


	public void setApplyReason(String applyReason) {
		this.mApplyReason = applyReason;
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
