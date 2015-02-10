package com.V2.jni.callbacAdapter;

import com.V2.jni.callbackInterface.AudioRequestCallback;
import com.V2.jni.ind.AudioJNIObjectInd;

public abstract class AudioRequestCallbackAdapter implements
		AudioRequestCallback {

	@Override
	public void OnAudioChatInvite(AudioJNIObjectInd ind) {

	}

	@Override
	public void OnAudioChatAccepted(AudioJNIObjectInd ind) {

	}

	@Override
	public void OnAudioChatRefused(AudioJNIObjectInd ind) {

	}

	@Override
	public void OnAudioChatClosed(AudioJNIObjectInd ind) {

	}

	@Override
	public void OnRecordStart(AudioJNIObjectInd ind) {

	}

	@Override
	public void OnRecordStop(AudioJNIObjectInd ind) {

	}
}
