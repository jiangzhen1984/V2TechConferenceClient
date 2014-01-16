package com.V2.jni;

public interface ConfRequestCallback {

	
	public void OnEnterConfCallback(long nConfID, long nTime, String szConfData, int nJoinResult);
	
	public void OnConfMemberEnterCallback(long nConfID, long  nTime, String szUserInfos);
	
}
