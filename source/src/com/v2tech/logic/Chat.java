package com.v2tech.logic;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.V2.jni.AudioRequest;
import com.V2.jni.AudioRequestCallback;
import com.V2.jni.ChatRequest;
import com.v2tech.util.V2Log;

public class Chat extends Handler {

	private AudioRequestCallback callback;

	private Message mCaller;

	public Chat() {
		super();
		init();
	}

	public Chat(Callback callback) {
		super(callback);
		init();
	}

	public Chat(Looper looper, Callback callback) {
		super(looper, callback);
		init();
	}

	public Chat(Looper looper) {
		super(looper);
		init();
	}

	private void init() {
		callback = new AudioRequestCallbackImpl();
		AudioRequest.getInstance().registerCallback(callback);
	}

	/**
	 * send message 
	 * @param msg
	 * @param caller
	 */
	public void sendVMessage(VMessage msg, Message caller) {
		ChatRequest.getInstance().sendChatText(0, msg.getToUser().getmUserId(),
				msg.toXml(), ChatRequest.BT_IM);
		if (msg.mType == VMessage.MessageType.IMAGE
				|| msg.mType == VMessage.MessageType.IMAGE_AND_TEXT) {
			// TODO send image
		} 
	}

	public void inviteUserAudioChat(UserAudioDevice ud, Message caller) {
		if (mCaller != null) {
			V2Log.e(" audio call is on going");
			return;
		}
		this.mCaller = caller;

		AudioRequest.getInstance().InviteAudioChat(0,
				ud.getUser().getmUserId(), AudioRequest.BT_IM);
	}

	public void cancelAudioCall(UserAudioDevice ud, Message caller) {
		this.mCaller = null;
		AudioRequest.getInstance().CloseAudioChat(0, ud.getUser().getmUserId(),
				AudioRequest.BT_IM);
		caller.sendToTarget();
	}

	public void acceptAudioChat(UserAudioDevice ud, Message caller) {
		AudioRequest.getInstance().AcceptAudioChat(0,
				ud.getUser().getmUserId(), AudioRequest.BT_IM);
	}

	public void refuseAudioChat(UserAudioDevice ud, Message caller) {

	}

	class AudioRequestCallbackImpl implements AudioRequestCallback {

		@Override
		public void OnAudioChatAccepted(long nGroupID, long nBusinessType,
				long nFromUserID) {
			mCaller.arg1 = 1;
			mCaller.arg2 = 1;
			mCaller.sendToTarget();
		}

		@Override
		public void OnAudioChatRefused(long nGroupID, long nBusinessType,
				long nFromUserID) {

		}

	}

	@Override
	public void handleMessage(Message msg) {
		super.handleMessage(msg);
	}

}
