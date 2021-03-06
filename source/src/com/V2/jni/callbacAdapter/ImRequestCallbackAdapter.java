package com.V2.jni.callbacAdapter;

import java.util.List;

import com.V2.jni.callbackInterface.ImRequestCallback;
import com.V2.jni.ind.BoUserInfoBase;

public abstract class ImRequestCallbackAdapter implements ImRequestCallback {

	@Override
	public void OnLoginCallback(long nUserID, int nStatus, int nResult , long serverTime , String sDBID) {

	}

	@Override
	public void OnLogoutCallback(int nType) {

	}

	@Override
	public void OnConnectResponseCallback(int nResult) {

	}

	@Override
	public void OnUpdateBaseInfoCallback(BoUserInfoBase user) {

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
	public void OnSearchUserCallback(List<BoUserInfoBase> list) {
		
	}

	@Override
	public void OnGroupsLoaded() {
		
	}
	
	@Override
	public void OnOfflineStart() {
		
	}
	
	@Override
	public void OnOfflineEnd() {
		
	}
}
