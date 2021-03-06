package com.V2.jni.callbackInterface;

import com.V2.jni.ind.AudioJNIObjectInd;

public interface AudioRequestCallback {

	/**
	 * Other invite current to join voice call.
	 * 
	 * @param ind
	 *            indication object
	 * @see AudioJNIObjectInd
	 */
	public void OnAudioChatInvite(AudioJNIObjectInd ind);

	/**
	 * Other user accept current voice invitation
	 * 
	 * @param ind
	 *            indication object
	 * @see AudioJNIObjectInd
	 */
	public void OnAudioChatAccepted(AudioJNIObjectInd ind);

	/**
	 * Other user refuse current conversation of voice
	 * 
	 * @param ind
	 *            indication object
	 * @see AudioJNIObjectInd
	 */
	public void OnAudioChatRefused(AudioJNIObjectInd ind);

	/**
	 * Other user close current conversation of voice
	 * 
	 * @param ind
	 *            indication object
	 * @see AudioJNIObjectInd
	 */
	public void OnAudioChatClosed(AudioJNIObjectInd ind);

	/**
	 * This callBack is called when layer start recording.
	 * 
	 * @param ind
	 *            indication object
	 * @see AudioJNIObjectInd
	 */
	public void OnRecordStart(AudioJNIObjectInd ind);
	
	/**
	 * This callBack is called when layer stop recording.
	 * 
	 * @param ind
	 *            indication object
	 * @see AudioJNIObjectInd
	 */
	public void OnRecordStop(AudioJNIObjectInd ind);
}
