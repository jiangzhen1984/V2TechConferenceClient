package com.V2.jni;

public abstract class VideoRequestCallbackAdapter implements
		VideoRequestCallback {

	@Override
	public void OnRemoteUserVideoDevice(long uid, String szXmlData) {

	}

	@Override
	public void OnVideoChatInviteCallback(long nGroupID, int nBusinessType,
			long nFromUserID, String szDeviceID) {

	}

	@Override
	public void OnSetCapParamDone(String szDevID, int nSizeIndex,
			int nFrameRate, int nBitRate) {

	}

	@Override
	public void OnVideoChatAccepted(long nGroupID, int nBusinessType,
			long nFromuserID, String szDeviceID) {

	}

	@Override
	public void OnVideoChatRefused(long nGroupID, int nBusinessType,
			long nFromUserID, String szDeviceID) {

	}

	@Override
	public void OnVideoChatClosed(long nGroupID, int nBusinessType,
			long nFromUserID, String szDeviceID) {

	}

}
