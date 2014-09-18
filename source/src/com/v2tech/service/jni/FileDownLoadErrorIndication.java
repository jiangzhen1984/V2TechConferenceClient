package com.v2tech.service.jni;

public class FileDownLoadErrorIndication extends FileTransStatusIndication {

	public FileDownLoadErrorIndication(String uuid,int errorCode, int nTransType) {
		super(IND_TYPE_DOWNLOAD_ERR, nTransType, uuid , errorCode);
	}
}
