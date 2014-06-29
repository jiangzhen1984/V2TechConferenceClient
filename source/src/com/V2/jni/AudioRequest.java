package com.V2.jni;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import com.v2tech.util.V2Log;

import android.content.Context;
import android.util.Log;

public class AudioRequest {

	public static final int BT_CONF = 1;
	public static final int BT_IM = 2;

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

	// ����Է���ʼ��Ƶͨ��
	public native void InviteAudioChat(long nGroupID, long nToUserID,
			int businesstype);

	// ���ܶԷ�����Ƶͨ������
	public native void AcceptAudioChat(long nGroupID, long nToUserID,
			int businesstype);

	// �ܾ�Է�����Ƶͨ������
	public native void RefuseAudioChat(long nGroupID, long nToUserID,
			int businesstype);

	// ȡ����Ƶͨ��
	public native void CloseAudioChat(long nGroupID, long nToUserID,
			int businesstype);

	public native void MuteMic(long nGroupID, long nUserID, boolean bMute,
			int businesstype);

	public native void CancelAudioChat(long nGroupID, long nToUserID,
			int businessType);

	// �յ���Ƶͨ��������Ļص�
	private void OnAudioChatInvite(long nGroupID, long nBusinessType,
			long nFromUserID) {
		V2Log.d("OnAudioChaInvite " + nGroupID + ":" + nBusinessType + ":"
				+ nFromUserID);
		for (WeakReference<AudioRequestCallback> wr : callbacks) {
			Object obj = wr.get();
			if (obj != null) {
				((AudioRequestCallback) obj).OnAudioChatInvite(nGroupID,
						nBusinessType, nFromUserID);
			}
		}
	}

	private void OnAudioChatAccepted(long nGroupID, long nBusinessType,
			long nFromUserID) {
		V2Log.d("OnAudioChatAccepted " + nGroupID + ":" + nBusinessType + ":"
				+ nFromUserID);

		for (WeakReference<AudioRequestCallback> wr : callbacks) {
			Object obj = wr.get();
			if (obj != null) {
				((AudioRequestCallback) obj).OnAudioChatAccepted(nGroupID,
						nBusinessType, nFromUserID);
			}
		}
	}

	// ��Ƶͨ�����뱻�Է��ܾ�Ļص�
	private void OnAudioChatRefused(long nGroupID, long nBusinessType,
			long nFromUserID) {
		V2Log.d("OnAudioChatRefused " + nGroupID + ":" + nBusinessType + ":"
				+ nFromUserID);
		for (WeakReference<AudioRequestCallback> wr : callbacks) {
			Object obj = wr.get();
			if (obj != null) {
				((AudioRequestCallback) obj).OnAudioChatRefused(nGroupID,
						nBusinessType, nFromUserID);
			}
		}
	}

	// ��Ƶͨ�����ر���ص�
	private void OnAudioChatClosed(long nGroupID, long nBusinessType,
			long nFromUserID) {
		V2Log.d("OnAudioChatClosed " + nGroupID + ":" + nBusinessType + ":"
				+ nFromUserID);
		for (WeakReference<AudioRequestCallback> wr : callbacks) {
			Object obj = wr.get();
			if (obj != null) {
				((AudioRequestCallback) obj).OnAudioChatClosed(nGroupID,
						nBusinessType, nFromUserID);
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
