package com.V2.jni;

public abstract class AudioRequestCallbackAdapter implements
		AudioRequestCallback {

	@Override
	public void OnAudioChatInvite(long nGroupID, long nBusinessType,
			long nFromUserID) {

	}

	@Override
	public void OnAudioChatAccepted(long nGroupID, long nBusinessType,
			long nFromUserID) {

	}

	@Override
	public void OnAudioChatRefused(long nGroupID, long nBusinessType,
			long nFromUserID) {

	}

	@Override
	public void OnAudioChatClosed(long nGroupID, long nBusinessType,
			long nFromUserID) {

	}

}
