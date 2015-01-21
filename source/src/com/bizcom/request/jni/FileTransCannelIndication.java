package com.bizcom.request.jni;

public class FileTransCannelIndication extends FileTransStatusIndication {

	public FileTransCannelIndication(String uuid) {
		super(IND_TYPE_TRANS_CANNEL, 1, uuid);
	}
}
