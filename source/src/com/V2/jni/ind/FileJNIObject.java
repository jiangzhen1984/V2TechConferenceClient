package com.V2.jni.ind;

public class FileJNIObject extends JNIObjectInd {

	public long fromUserid;
	public String fileId;
	public String fileName;
	public long fileSize;
	public int fileType;
	

	/**
	 * 
	 * @param userid
	 * @param szFileID
	 * @param szFileName
	 * @param nFileBytes
	 * @param linetype 2: offline file  1: online file
	 */
	public FileJNIObject(long userid, String szFileID, String szFileName,
			long nFileBytes, int linetype) {
		this.fromUserid = userid;
		this.fileId = szFileID;
		this.fileName = szFileName;
		this.fileSize = nFileBytes;
		this.fileType = linetype;
		this.mType = JNIIndType.FILE;
	}
	
}
