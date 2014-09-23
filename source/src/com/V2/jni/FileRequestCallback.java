package com.V2.jni;

import android.content.Context;

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
	 * @param nTransType
	 *            2: offline file 1: online file
	 */
	public void OnFileTransProgress(String szFileID, long nBytesTransed,
			int nTransType);

	/**
	 * 
	 * @param szFileID
	 * @param errorCode
	 * @param nTransType
	 */
	public void OnFileTransError(String szFileID, int errorCode, int nTransType);

	/**
	 * 
	 * @param szFileID
	 * @param szFileName
	 * @param nFileSize
	 * @param nTransType
	 */
	public void OnFileTransEnd(String szFileID, String szFileName,
			long nFileSize, int nTransType , Context context);

	/**
	 * 
	 * @param szFileID
	 */
	public void OnFileTransCancel(String szFileID);

	/**
	 * 
	 * @param sFileID
	 * @param t1
	 *            错误代码
	 */
	public void OnFileDownloadError(String sFileID,int errorCode, int nTransType , Context context);
}
