package com.V2.jni;

public abstract class FileRequestCallbackAdapter implements FileRequestCallback {

	@Override
	public void OnFileTransInvite(long nGroupID, int nBusinessType,
			long userid, String szFileID, String szFileName, long nFileBytes,
			int linetype) {

	}

	@Override
	public void OnFileTransProgress(String szFileID, long nBytesTransed,
			int nTransType) {

	}

	@Override
	public void OnFileTransEnd(String szFileID, String szFileName,
			long nFileSize, int nTransType) {

	}

	@Override
	public void OnFileDownloadError(String sFileID , int t1) {

	}

}
