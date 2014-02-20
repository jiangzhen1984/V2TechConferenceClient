package com.V2.jni;


public interface AudioRequestCallback {
	
	
	/**
	 * 
	 * @param nGroupID
	 * @param nBusinessType
	 * @param nFromUserID
	 */
	public void OnAudioChatAccepted(long nGroupID, long nBusinessType,
			long nFromUserID);

	/**
	 * 
	 * @param nGroupID
	 * @param nBusinessType
	 * @param nFromUserID
	 */
	public void OnAudioChatRefused(long nGroupID, long nBusinessType,
			long nFromUserID);

}
