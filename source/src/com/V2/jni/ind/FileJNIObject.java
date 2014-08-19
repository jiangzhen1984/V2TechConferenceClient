package com.V2.jni.ind;

public class FileJNIObject extends JNIObjectInd {

//	public long groupId;
	public String fileId;
	public long fromUserId;
	public String fileName;
	public long fileSize;
	public int fileType;
	
	
	/**
	 * 
	 * @param mRequestType  1: conference   2: IM 
	 * @param groupId
	 * @param fileId
	 * @param fromUserId
	 * @param fileName
	 * @param fileSize
	 * @param fileType   2: offline file  1: online file
	 */
	public FileJNIObject(int mRequestType, long groupId, String fileId, long fromUserId,
			String fileName, long fileSize, int fileType) {
		super();
//		this.groupId = groupId;
		this.fileId = fileId;
		this.fromUserId = fromUserId;
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.mType = JNIIndType.FILE;
		this.fileType = fileType;

	}
	
	
	
}
