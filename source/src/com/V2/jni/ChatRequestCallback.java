package com.V2.jni;


public interface ChatRequestCallback {

	
	/**
	 * 
	 * @param nGroupID
	 * @param nBusinessType
	 * @param nFromUserID
	 * @param nTime
	 * @param szXmlText
	 */
	public void OnRecvChatTextCallback(long nGroupID, int nBusinessType,
			long nFromUserID, long nTime, String szXmlText);

	
	/**
	 * 
	 * @param nGroupID
	 * @param nBusinessType
	 * @param nFromUserID
	 * @param nTime
	 * @param pPicData
	 */
	public void OnRecvChatPictureCallback(long nGroupID, int nBusinessType,
			long nFromUserID, long nTime, byte[] pPicData);

}
