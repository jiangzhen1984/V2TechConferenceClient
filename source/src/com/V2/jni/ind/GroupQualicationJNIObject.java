package com.V2.jni.ind;

import com.v2tech.vo.VMessageQualification.QualificationState;
import com.v2tech.vo.VMessageQualification.Type;

public class GroupQualicationJNIObject {

	public Type qualicationType;
	public QualificationState state;
	public String applyReason;
	public String refuseReason;

	public GroupQualicationJNIObject(Type qualicationType,
			QualificationState state, String refuseReason) {
		super();
		this.qualicationType = qualicationType;
		this.state = state;
		this.refuseReason = refuseReason;
		applyReason = null;
	}
}
