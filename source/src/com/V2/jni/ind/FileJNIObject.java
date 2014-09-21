package com.V2.jni.ind;

public class FileJNIObject extends JNIObjectInd {

	public V2User user;
	public String fileId;
	public String fileName;
	public long fileSize;
	public int fileType;
	
	//For crowd file type
	public String url;
	

	/**
	 * 
	 * @param userid
	 * @param szFileID
	 * @param szFileName
	 * @param nFileBytes
	 * @param linetype 2: offline file  1: online file
	 */
	public FileJNIObject(V2User user, String szFileID, String szFileName,
			long nFileBytes, int linetype) {
		this.user = user;
		this.fileId = szFileID;
		this.fileName = szFileName;
		this.fileSize = nFileBytes;
		this.fileType = linetype;
		this.mType = JNIIndType.FILE;
	}
	
}
