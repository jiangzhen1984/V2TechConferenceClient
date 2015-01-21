package com.bizcom.vo;

/**
 * Crowd invitation message
 * 
 * @author jiangzhen
 * 
 */
public class VMessageQualificationInvitationCrowd extends VMessageQualification {

	// Most time invitation user same with crowd owner
	protected User mInvitationUser;
	protected User mBeInvitatonUser;

	public VMessageQualificationInvitationCrowd(CrowdGroup crowdGroup,
			User beInvitationUser) {
		super(VMessageQualification.Type.CROWD_INVITATION, crowdGroup);
		this.mBeInvitatonUser = beInvitationUser;
		this.mInvitationUser = mCrowdGroup.getOwnerUser();
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

	public CrowdGroup getCrowdGroup() {
		return mCrowdGroup;
	}
}
