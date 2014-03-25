package com.V2.jni;

public interface ConfRequestCallback {

	/**
	 * <ul>Indicate result what current request to enter conference. <br/>
	 * This call back is called after user request to enter conference.</ul>
	 * @param nConfID  conference ID
	 * @param nTime  entered time
	 * @param szConfData
	 * @param nJoinResult 0 means successfully
	 * 
	 * @see ConfRequest#enterConf(long)
	 */
	public void OnEnterConfCallback(long nConfID, long nTime, String szConfData, int nJoinResult);
	
	/**
	 *  <ul>Indicate new attendee entered current conference.<br>
	 *  This callback is called many times and same with current conference's attendee count except self.</ul>
	 *  
	 * @param nConfID conference ID what user entered
	 * @param nTime
	 * @param szUserInfos  < user id='146' uetype='1'/>
	 */
	public void OnConfMemberEnterCallback(long nConfID, long  nTime, String szUserInfos);
	
	/**
	 * <ul><Indicate attendee exited current conference. </ul>
	 * @param nConfID  conference ID what user entered
	 * @param nTime
	 * @param nUserID exited user's ID
	 */
	public void OnConfMemberExitCallback(long nConfID, long nTime, long nUserID);
	
	
	/**
	 * <ul>Indicate user has been requested to get out of conference.</ul>
	 * <ul>Two reason will affect this function will be called :<br>
	 *    1. Chair man of conference requested.<br>
	 *    2. Chair man removed conference.</ul>
	 * @param nReason TODO add comments of reason
	 */
	public void OnKickConfCallback(int nReason);
	
}
