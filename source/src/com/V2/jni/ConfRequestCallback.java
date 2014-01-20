package com.V2.jni;

public interface ConfRequestCallback {

	/**
	 * 
	 * @param nConfID
	 * @param nTime
	 * @param szConfData
	 * @param nJoinResult
	 */
	public void OnEnterConfCallback(long nConfID, long nTime, String szConfData, int nJoinResult);
	
	/**
	 * 
	 * @param nConfID
	 * @param nTime
	 * @param szUserInfos  <user id='146' uetype='1'/>
	 */
	public void OnConfMemberEnterCallback(long nConfID, long  nTime, String szUserInfos);
	
	/**
	 * 
	 * @param nConfID
	 * @param nTime
	 * @param nUserID
	 */
	public void OnConfMemberExitCallback(long nConfID, long nTime, long nUserID);
	
}
