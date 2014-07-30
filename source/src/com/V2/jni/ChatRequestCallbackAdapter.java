package com.V2.jni;

import com.V2.jni.ind.SendingResultJNIObjectInd;

public abstract class ChatRequestCallbackAdapter implements ChatRequestCallback {

	@Override
	public void OnRecvChatTextCallback(long nGroupID, int nBusinessType,
			long nFromUserID, long nTime, String szXmlText) {

	}

	@Override
	public void OnRecvChatPictureCallback(long nGroupID, int nBusinessType,
			long nFromUserID, long nTime, String szSeqID, byte[] pPicData) {

	}

	@Override
	public void OnRecvChatAudio(long gid, int businessType, long fromUserId,
			long timeStamp, String messageId, String audioPath) {

	}

	@Override
	public void OnSendChatResult(SendingResultJNIObjectInd ind) {

	}

}
