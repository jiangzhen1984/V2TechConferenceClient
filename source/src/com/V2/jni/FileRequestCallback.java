package com.V2.jni;

public interface FileRequestCallback {

	/**
	 * 
	 * @param nGroupID
	 * @param nBusinessType  1: conference   2: IM 
	 * @param userid
	 * @param szFileID
	 * @param szFileName
	 * @param nFileBytes
	 * @param linetype  2: offline file  1: online file
	 */
	public void OnFileTransInvite(long nGroupID, int nBusinessType,
			long userid, String szFileID, String szFileName, long nFileBytes,
			int linetype);
}
