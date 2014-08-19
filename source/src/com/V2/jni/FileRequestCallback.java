package com.V2.jni;

import com.V2.jni.ind.FileJNIObject;

public interface FileRequestCallback {

	/**
	 * 
	 * @param file 
	 */
	public void OnFileTransInvite(FileJNIObject file);
	
	
	/**
	 * 
	 * @param szFileID
	 * @param nBytesTransed
	 * @param nTransType  2: offline file  1: online file
	 */
	public void OnFileTransProgress(String szFileID, long nBytesTransed,
			int nTransType);
	
	
	public void OnFileTransEnd(String szFileID, String szFileName,
			long nFileSize, int nTransType);
	
	
	/**
	 * 
	 * @param sFileID
	 * @param t1 错误代码
	 */
	public void OnFileDownloadError(String sFileID , int t1);
}
