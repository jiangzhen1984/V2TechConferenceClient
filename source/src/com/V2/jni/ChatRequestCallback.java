package com.V2.jni;


public interface ChatRequestCallback {

	
	/**
	 * <ul>Use to receive message from server side.</ul>
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
	 * <ul>Receive image data from server side.</ul>
	 * @param nGroupID
	 * @param nBusinessType
	 * @param nFromUserID
	 * @param nTime
	 * @param pPicData
	 */
	public void OnRecvChatPictureCallback(long nGroupID, int nBusinessType,
			long nFromUserID, long nTime, String szSeqID, byte[] pPicData);

}
