package com.V2.jni;

public interface WBRequestCallback {
	
	public void OnWBoardChatInvite(long nGroupID, int nBusinessType, long  nFromUserID, String szWBoardID, 
			int nWhiteIndex,String szFileName, int type);

	public void OnWBoardPageList(String szWBoardID, String szPageData,
			int nPageID);

	public void OnWBoardActivePage(long nUserID, String szWBoardID, int nPageID);

	public void OnWBoardDocDisplay(String szWBoardID, int nPageID,
			String szFileName, int result);

}
