package com.v2tech.vo;

/**
 * Crowd qualification or application message
 * @author jiangzhen
 *
 */
public class VMessageQualificationCrowd extends  VMessageQualification {

	private CrowdGroup mCrowdGroup;

	public VMessageQualificationCrowd(CrowdGroup crowdGroup, User invitationUser,
			User beInvitationUser) {
		super(VMessageQualification.Type.CROWD, invitationUser, beInvitationUser);
		this.mCrowdGroup = crowdGroup;
	}

	
	public CrowdGroup getmCrowdGroup() {
		return mCrowdGroup;
	}

	public void setmCrowdGroup(CrowdGroup mCrowdGroup) {
		this.mCrowdGroup = mCrowdGroup;
	}
	
	
	
}
