package com.V2.jni;

public abstract class GroupRequestCallbackAdapter implements
		GroupRequestCallback {

	@Override
	public void OnGetGroupInfoCallback(int groupType, String sXml) {

	}

	@Override
	public void OnGetGroupUserInfoCallback(int groupType, long nGroupID,
			String sXml) {

	}

	@Override
	public void OnModifyGroupInfoCallback(int groupType, long nGroupID,
			String sXml) {

	}

	@Override
	public void OnInviteJoinGroupCallback(int groupType, String groupInfo,
			String userInfo, String additInfo) {

	}

	@Override
	public void OnDelGroupCallback(int groupType, long nGroupID,
			boolean bMovetoRoot) {

	}

	@Override
	public void OnDelGroupUserCallback(int groupType, long nGroupID,
			long nUserID) {

	}

	@Override
	public void OnAddGroupUserInfoCallback(int groupType, long nGroupID,
			String sXml) {

	}

}
