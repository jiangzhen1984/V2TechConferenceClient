package com.bizcom.vo;

import com.bizcom.vo.VMessageQualification.QualificationState;
import com.bizcom.vo.VMessageQualification.ReadState;
import com.bizcom.vo.VMessageQualification.Type;


public class GroupQualicationState {

	public Type qualicationType;
	public QualificationState state;
	public String applyReason;
	public String refuseReason;
	public ReadState readState;
	public boolean isOwnerGroup;
	//手动处理了验证消息状态，不更新存储时间；比如手动点同意或拒绝
	public boolean isUpdateTime = true; 

	public GroupQualicationState(Type qualicationType,
			QualificationState state, String reason , ReadState readState , boolean isOwnerGroup) {
		super();
		this.qualicationType = qualicationType;
		this.state = state;
		this.readState = readState;
		this.isOwnerGroup = isOwnerGroup;
		switch (state) {
		case ACCEPTED:
		case BE_ACCEPTED:
			this.applyReason = reason;
			break;
		case BE_REJECT:
			if(qualicationType == Type.CROWD_APPLICATION)
				this.applyReason = reason;
			else
				this.refuseReason = reason;
			break;
		case REJECT:
			this.refuseReason = reason;
			break;
		default:
			break;
		}
	}
}
