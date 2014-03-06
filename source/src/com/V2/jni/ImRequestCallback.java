package com.V2.jni;


/**
 * 
 * @author 28851274
 *
 */
public interface ImRequestCallback {

	
	/**
	 * Login call back function. <br>
	 * This function is called after you call {@link ImRequest#login(String, String, int, int)}.
	 * @param nUserID if succeed means user ID, otherwise 0
	 * @param nStatus
	 * @param nResult  0: succeed,  1: failed
	 */
	public void OnLoginCallback(long nUserID, int nStatus, int nResult);
	
	
	public void OnLogoutCallback(int nUserID);
	
	/**
	 * When network connection state changed, this function will be called.<br>
	 * <p>
	 *    When call {@link ImRequest#login(String, String, int, int)},  this call back will before than {@link #OnLoginCallback(long, int, int)}
	 * </p>
	 * @param nResult 301 can't not connect server;  0: succeed
	 */
	public void OnConnectResponseCallback(int nResult);
	
	
	/**
	 * Update user information call back. <br>
	 * <p> When you log in successfully, this function will be called.<br>
	 *    another case is when you call {@link ImRequest#getUserBaseInfo(long)}, this function will be called by JNI.
	 * </p>
	 * @param nUserID  user id
	 * @param updatexml TODO add XML example
	 */
	public void OnUpdateBaseInfoCallback(long nUserID, String updatexml);
	
	
	
	/**
	 * <ul>User status update API.</ul>
	 * 
	 * @param nUserID
	 * @param eUEType
	 * @param nStatus  1 is online, 0 is offline
	 * @param szStatusDesc
	 * 
	 * @see com.v2tech.logic.User.Status
	 */
	public void OnUserStatusUpdatedCallback(long nUserID, int eUEType,  int nStatus, String szStatusDesc);
	
	
	
	/**
	 *  <ul>Indicate user avatar changed.</ul>
	 * @param nAvatarType
	 * @param nUserID  User ID which user's changed avatar
	 * @param AvatarName  patch of avatar
	 */
	public void OnChangeAvatarCallback(int nAvatarType, long nUserID, String AvatarName);
}
