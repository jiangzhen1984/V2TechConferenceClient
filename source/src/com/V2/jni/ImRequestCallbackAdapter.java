package com.V2.jni;

import com.V2.jni.ind.V2Group;

public abstract class ImRequestCallbackAdapter implements ImRequestCallback {

	@Override
	public void OnLoginCallback(long nUserID, int nStatus, int nResult , long serverTime) {

	}

	@Override
	public void OnLogoutCallback(int nType) {

	}

	@Override
	public void OnConnectResponseCallback(int nResult) {

	}

	@Override
	public void OnUpdateBaseInfoCallback(long nUserID, String updatexml) {

	}

	@Override
	public void OnUserStatusUpdatedCallback(long nUserID, int nType,
			int nStatus, String szStatusDesc) {

	}

	@Override
	public void OnChangeAvatarCallback(int nAvatarType, long nUserID,
			String AvatarName) {

	}

	@Override
	public void OnModifyCommentNameCallback(long nUserId, String sCommmentName) {

	}

	@Override
	public void OnCreateCrowdCallback(V2Group crowd, int nResult) {

	}

}
