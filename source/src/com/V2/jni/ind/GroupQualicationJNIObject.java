package com.V2.jni.ind;

import com.v2tech.vo.VMessageQualification.QualificationState;
import com.v2tech.vo.VMessageQualification.Type;

public class GroupQualicationJNIObject {

	public int groupType;
	public long groupID;
	public long userID;
	public Type qualicationType;
	public QualificationState state;
	public String reason;
	
	public GroupQualicationJNIObject(int groupType, long groupID, long userID,
			Type qualicationType, QualificationState state, String reason) {
		super();
		this.groupType = groupType;
		this.groupID = groupID;
		this.userID = userID;
		this.qualicationType = qualicationType;
		this.state = state;
		this.reason = reason;
	}

}
