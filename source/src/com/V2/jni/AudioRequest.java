package com.V2.jni;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;

import com.V2.jni.ind.AudioJNIObjectInd;
import com.v2tech.util.V2Log;

public class AudioRequest {

	private Context context;
	private static AudioRequest mAudioRequest;

	private List<WeakReference<AudioRequestCallback>> callbacks;

	private AudioRequest(Context context) {
		this.context = context;
		callbacks = new ArrayList<WeakReference<AudioRequestCallback>>();
	};

	public void addCallback(AudioRequestCallback callback) {
		callbacks.add(new WeakReference<AudioRequestCallback>(callback));
	}

	public void removeCallback(AudioRequestCallback callback) {
		for (WeakReference<AudioRequestCallback> wr : callbacks) {
			Object obj = wr.get();
			if (obj == callback) {
				callbacks.remove(wr);
				return;
			}
		}
	}

	public static synchronized AudioRequest getInstance(Context context) {
		if (mAudioRequest == null) {
			mAudioRequest = new AudioRequest(context);
			if (!mAudioRequest.initialize(mAudioRequest)) {
				Log.e("AudioRequest", "can't initialize AudioRequest ");
			}
		}
		return mAudioRequest;
	}

	public static synchronized AudioRequest getInstance() {
		if (mAudioRequest == null) {
			mAudioRequest = new AudioRequest(null);
			if (!mAudioRequest.initialize(mAudioRequest)) {
				Log.e("AudioRequest", "can't initialize AudioRequest ");
			}
		}
		return mAudioRequest;
	}

	public native boolean initialize(AudioRequest request);

	public native void unInitialize();

	/**
	 * Invite user to join audio call
	 * 
	 * @param nGroupID
	 * @param nToUserID
	 * @param businesstype
	 *            type of request
	 * 
	 * @see V2GlobalEnum#REQUEST_TYPE_CONF
	 * @see V2GlobalEnum#REQUEST_TYPE_IM
	 */
	public native void InviteAudioChat(long nGroupID, long nToUserID,
			int businesstype);

	/**
	 * Accept audio conversation
	 * 
	 * @param nGroupID
	 * @param nToUserID
	 * @param businesstype
	 *            type of request
	 * 
	 * @see V2GlobalEnum#REQUEST_TYPE_CONF
	 * @see V2GlobalEnum#REQUEST_TYPE_IM
	 */
	public native void AcceptAudioChat(long nGroupID, long nToUserID,
			int businesstype);

	/**
	 * reject audio conversation
	 * 
	 * @param nGroupID
	 * @param nToUserID
	 * @param businesstype
	 *            type of request
	 * 
	 * @see V2GlobalEnum#REQUEST_TYPE_CONF
	 * @see V2GlobalEnum#REQUEST_TYPE_IM
	 */
	public native void RefuseAudioChat(long nGroupID, long nToUserID,
			int businesstype);

	/**
	 * 
	 * @param nGroupID
	 * @param nToUserID
	 * @param businesstype
	 *            type of request
	 * 
	 * @see V2GlobalEnum#REQUEST_TYPE_CONF
	 * @see V2GlobalEnum#REQUEST_TYPE_IM
	 */
	public native void CloseAudioChat(long nGroupID, long nToUserID,
			int businesstype);

	/**
	 * 
	 * @param nGroupID
	 * @param nUserID
	 * @param bMute
	 * @param businesstype
	 *            type of request
	 * 
	 * @see V2GlobalEnum#REQUEST_TYPE_CONF
	 * @see V2GlobalEnum#REQUEST_TYPE_IM
	 */
	public native void MuteMic(long nGroupID, long nUserID, boolean bMute,
			int businesstype);

	public native void CancelAudioChat(long nGroupID, long nToUserID,
			int businessType);

	
	/**
	 * 
	 */
	public native void PausePlayout();
	
	public native void ResumePlayout();
	
	// �յ���Ƶͨ��������Ļص�
	private void OnAudioChatInvite(long nGroupID, long nBusinessType,
			long nFromUserID) {
		V2Log.d("OnAudioChaInvite " + nGroupID + ":" + nBusinessType + ":"
				+ nFromUserID);
		for (int i = 0; i < callbacks.size(); i++) {
			WeakReference<AudioRequestCallback> wr = callbacks.get(i);
			Object obj = wr.get();
			if (obj != null) {
				((AudioRequestCallback) obj)
						.OnAudioChatInvite(new AudioJNIObjectInd(nGroupID,
								nFromUserID, (int) nBusinessType));
			}
		}
	}

	private void OnAudioChatAccepted(long nGroupID, long nBusinessType,
			long nFromUserID) {
		V2Log.d("OnAudioChatAccepted " + nGroupID + ":" + nBusinessType + ":"
				+ nFromUserID);

		for (int i = 0; i < callbacks.size(); i++) {
			WeakReference<AudioRequestCallback> wr = callbacks.get(i);
			Object obj = wr.get();
			if (obj != null) {
				((AudioRequestCallback) obj).OnAudioChatAccepted(new AudioJNIObjectInd(nGroupID,
						nFromUserID, (int) nBusinessType));
			}
		}
	}

	// ��Ƶͨ�����뱻�Է��ܾ�Ļص�
	private void OnAudioChatRefused(long nGroupID, long nBusinessType,
			long nFromUserID) {
		V2Log.d("OnAudioChatRefused " + nGroupID + ":" + nBusinessType + ":"
				+ nFromUserID);
		for (int i = 0; i < callbacks.size(); i++) {
			WeakReference<AudioRequestCallback> wr = callbacks.get(i);
			Object obj = wr.get();
			if (obj != null) {
				((AudioRequestCallback) obj).OnAudioChatRefused(new AudioJNIObjectInd(nGroupID,
						nFromUserID, (int) nBusinessType));
			}
		}
	}

	// ��Ƶͨ�����ر���ص�
	private void OnAudioChatClosed(long nGroupID, long nBusinessType,
			long nFromUserID) {
		V2Log.d("OnAudioChatClosed " + nGroupID + ":" + nBusinessType + ":"
				+ nFromUserID);
		for (int i = 0; i < callbacks.size(); i++) {
			WeakReference<AudioRequestCallback> wr = callbacks.get(i);
			Object obj = wr.get();
			if (obj != null) {
				((AudioRequestCallback) obj).OnAudioChatClosed(new AudioJNIObjectInd(nGroupID,
						nFromUserID, (int) nBusinessType));
			}
		}
	}

	// ��Ƶͨ��������
	private void OnAudioChating(long nGroupID, long nBusinessType,
			long nFromUserID) {
		Log.e("ImRequest UI", "OnAudioChating " + nGroupID + ":"
				+ nBusinessType + ":" + nFromUserID);
	}
}
