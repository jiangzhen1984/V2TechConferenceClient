package com.V2.jni;


/**
 * 
 * @author 28851274
 *
 */
public interface ImRequestCallback {

	
	public void OnLoginCallback(long nUserID, int nStatus, int nResult);
	
	
	public void OnLogoutCallback(int nUserID);
	
	
	public void OnConnectResponseCallback(int nResult);
	
	
	public void OnUpdateBaseInfoCallback(long nUserID, String updatexml);
}
