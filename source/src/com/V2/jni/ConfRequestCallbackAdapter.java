package com.V2.jni;

public abstract class ConfRequestCallbackAdapter implements ConfRequestCallback {

	@Override
	public void OnEnterConfCallback(long nConfID, long nTime,
			String szConfData, int nJoinResult) {

	}

	@Override
	public void OnConfMemberEnterCallback(long nConfID, long nTime,
			String szUserInfos) {

	}

	@Override
	public void OnConfMemberExitCallback(long nConfID, long nTime, long nUserID) {

	}

	@Override
	public void OnKickConfCallback(int nReason) {

	}

	@Override
	public void OnGrantPermissionCallback(long userid, int type, int status) {

	}

	@Override
	public void OnConfNotify(String confXml, String creatorXml) {

	}

}
