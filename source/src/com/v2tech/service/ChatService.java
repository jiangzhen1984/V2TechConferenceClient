package com.v2tech.service;

import java.util.ArrayList;
import java.util.List;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.SparseArray;

import com.V2.jni.AudioRequest;
import com.V2.jni.AudioRequestCallback;
import com.V2.jni.ChatRequest;
import com.V2.jni.VideoRequest;
import com.V2.jni.VideoRequestCallback;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.service.jni.RequestChatServiceResponse;
import com.v2tech.service.jni.RequestSendMessageResponse;
import com.v2tech.util.V2Log;
import com.v2tech.vo.UserAudioDevice;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.VMessageAudioItem;
import com.v2tech.vo.VMessageImageItem;

public class ChatService extends AbstractHandler {

	private AudioRequestCallback callback;
	
	private VideoRequestCallback videoCallback;

	private Registrant mCaller;

	private Handler thread;
	
	private static final int KEY_CANCELLED_LISTNER = 1;
	
	private SparseArray<List<Registrant>> registrantHolder = new SparseArray<List<Registrant>>();

	public ChatService() {
		super();
		init();
	}

	private void init() {
		callback = new AudioRequestCallbackImpl();
		AudioRequest.getInstance().addCallback(callback);

		videoCallback = new VideoRequestCallbackImpl();
		VideoRequest.getInstance().addCallback(videoCallback);
		
		HandlerThread backEnd = new HandlerThread("back-end");
		backEnd.start();
		thread = new Handler(backEnd.getLooper());
	}
	
	
	private void notifyListener(int key, int arg1, int arg2, Object obj) {
		List<Registrant> list = registrantHolder.get(key);
		if (list == null) {
			V2Log.e(" No listener for " + key);
			return;
		} else {
			V2Log.i(" Notify listener: " + arg1 + "  " + arg2 + "  " + obj);
		}
		for (Registrant re : list) {
			Handler h = re.getHandler();
			if (h != null) {
				Message.obtain(h, re.getWhat(), arg1, arg2, obj).sendToTarget();
			}
		}
	}
	
	
	private void registerListener(int key, Handler h, int what, Object obj) {
		List<Registrant> list = registrantHolder.get(key);
		if (list == null) {
			list = new ArrayList<Registrant>();
			registrantHolder.append(key, list);
		}
		list.add(new Registrant(h, what, obj));
	}

	private void unRegisterListener(int key, Handler h, int what, Object obj) {
		List<Registrant> list = registrantHolder.get(key);
		if (list != null) {
			for (Registrant re : list) {
				if (re.getHandler() == h && what == re.getWhat()) {
					list.remove(re);
				}
			}
		}
	}
	
	/**
	 * Register listener for out conference by kick.
	 * 
	 * @param msg
	 */
	public void registerCancelledListener(Handler h, int what, Object obj) {
		registerListener(KEY_CANCELLED_LISTNER, h, what, obj);
	}

	public void removeRegisterCancelledListener(Handler h, int what,
			Object obj) {
		unRegisterListener(KEY_CANCELLED_LISTNER, h, what, obj);

	}

	/**
	 * send message
	 * 
	 * @param msg
	 * @param caller
	 */
	public void sendVMessage(final VMessage msg, final Registrant caller) {
		if (msg == null) {
			V2Log.w(" ToUser is null can not send message");
			return;
		}
		thread.post(new Runnable() {
			@Override
			public void run() {

				// If message items do not only contain audio message item
				// then send text or image message
				ChatRequest.getInstance().sendChatText(
						msg.getGroupId(),
						msg.getToUser() == null ? 0 : msg.getToUser()
								.getmUserId(), msg.getUUID(), msg.toXml(),
						msg.getMsgCode());

				List<VMessageImageItem> imageItems = msg.getImageItems();
				for (VMessageImageItem item : imageItems) {
					byte[] data = item.loadImageData();
					ChatRequest.getInstance().sendChatPicture(
							msg.getGroupId(),
							msg.getToUser() == null ? 0 : msg.getToUser()
									.getmUserId(), item.getUUID(), data,
							data.length, msg.getMsgCode());
				}

				List<VMessageAudioItem> audioList = msg.getAudioItems();
				for (int i = 0; audioList != null && i < audioList.size(); i++) {
					ChatRequest.getInstance().sendChatAudio(msg.getGroupId(),
							msg.getToUser().getmUserId(),
							audioList.get(i).getUUID(),
							audioList.get(i).getAudioFilePath(),
							msg.getMsgCode());
				}

				if (caller != null && caller.getHandler() != null) {
					JNIResponse jniRes = new RequestSendMessageResponse(
							JNIResponse.Result.SUCCESS);
					sendResult(caller, jniRes);
				} else {
					V2Log.w(" requester don't expect response");
				}
			}

		});
	}

	/**
	 * Invite contact for chat
	 * 
	 * @param ud
	 * @param caller
	 */
	public void inviteUserChat(UserAudioDevice ud, Registrant caller) {
		JNIResponse resp = null;
		if (mCaller != null) {
			V2Log.e(" audio call is on going");
			resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.Result.INCORRECT_PAR);
		}
		if (ud == null) {
			resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.Result.INCORRECT_PAR);
		}

		if (resp != null) {
			sendResult(caller, resp);
			return;
		}

		this.mCaller = caller;

		if (ud.isAudioType()) {
			AudioRequest.getInstance().InviteAudioChat(ud.getGroupdId(),
					ud.getUser().getmUserId(), AudioRequest.BT_IM);
		} else if (ud.isVideoType()) {
			VideoRequest.getInstance().inviteVideoChat(ud.getGroupdId(),
					ud.getUser().getmUserId(), ud.getDeviceId(),
					VideoRequest.BT_IM);
		}
	}

	/**
	 * 
	 * @param ud
	 * @param caller
	 */
	public void cancelChattingCall(UserAudioDevice ud, Registrant caller) {
		if (ud == null) {
			JNIResponse resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.Result.INCORRECT_PAR);
			sendResult(caller, resp);
			return;
		}

		if (ud.isAudioType()) {
			AudioRequest.getInstance().CloseAudioChat(ud.getGroupdId(),
					ud.getUser().getmUserId(), AudioRequest.BT_IM);
		} else if (ud.isVideoType()) {
			VideoRequest.getInstance().closeVideoChat(ud.getGroupdId(),
					ud.getUser().getmUserId(), ud.getDeviceId(),
					VideoRequest.BT_IM);
		}

		if (caller != null) {
			JNIResponse resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.Result.SUCCESS);
			sendResult(caller, resp);
		}

		this.mCaller = null;
	}

	public void acceptChatting(UserAudioDevice ud, Registrant caller) {
		if (ud == null) {
			JNIResponse resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.Result.INCORRECT_PAR);
			sendResult(caller, resp);
			return;
		}

		if (ud.isAudioType()) {
			AudioRequest.getInstance().AcceptAudioChat(ud.getGroupdId(),
					ud.getUser().getmUserId(), AudioRequest.BT_IM);
		} else if (ud.isVideoType()) {
			VideoRequest.getInstance().acceptVideoChat(ud.getGroupdId(),
					ud.getUser().getmUserId(), ud.getDeviceId(),
					VideoRequest.BT_IM);
		}
		if (caller != null) {
			JNIResponse resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.Result.SUCCESS);
			sendResult(caller, resp);
		}
	}

	public void refuseChatting(UserAudioDevice ud, Registrant caller) {
		if (mCaller != null) {
			JNIResponse resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.Result.INCORRECT_PAR);
			sendResult(caller, resp);
			return;
		}

		if (ud.isAudioType()) {
			AudioRequest.getInstance().RefuseAudioChat(ud.getGroupdId(),
					ud.getUser().getmUserId(), AudioRequest.BT_IM);
		} else if (ud.isVideoType()) {
			VideoRequest.getInstance().refuseVideoChat(ud.getGroupdId(),
					ud.getUser().getmUserId(), ud.getDeviceId(),
					VideoRequest.BT_IM);
		}
		if (caller != null) {
			JNIResponse resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.Result.SUCCESS);
			sendResult(caller, resp);
		}
	}
	
	
	class VideoRequestCallbackImpl implements VideoRequestCallback {

		@Override
		public void OnRemoteUserVideoDevice(String szXmlData) {
			
		}

		@Override
		public void OnVideoChatInviteCallback(long nGroupID, int nBusinessType,
				long nFromUserID, String szDeviceID) {
			
		}

		@Override
		public void OnSetCapParamDone(String szDevID, int nSizeIndex,
				int nFrameRate, int nBitRate) {
			
		}
		
		public void OnVideoChatAccepted(long nGroupID, int nBusinessType,
				long nFromuserID, String szDeviceID) {
			if (mCaller != null) {
				JNIResponse resp = new RequestChatServiceResponse(RequestChatServiceResponse.ACCEPTED,
						RequestChatServiceResponse.Result.SUCCESS);
				sendResult(mCaller, resp);
				mCaller = null;
			}
		}

		public void OnVideoChatRefused(long nGroupID, int nBusinessType,
				long nFromUserID, String szDeviceID) {
			if (mCaller != null) {
				JNIResponse resp = new RequestChatServiceResponse(RequestChatServiceResponse.REJCTED,
						RequestChatServiceResponse.Result.SUCCESS);
				sendResult(mCaller, resp);
				mCaller = null;
			}
		}

		@Override
		public void OnVideoChatClosed(long nGroupID, int nBusinessType,
				long nFromUserID, String szDeviceID) {
			notifyListener(KEY_CANCELLED_LISTNER, 0, 0, null);
			//Clean cache
			mCaller = null;
		}
		
		
		
	}

	class AudioRequestCallbackImpl implements AudioRequestCallback {

		@Override
		public void OnAudioChatAccepted(long nGroupID, long nBusinessType,
				long nFromUserID) {
			if (mCaller != null) {
				JNIResponse resp = new RequestChatServiceResponse(RequestChatServiceResponse.ACCEPTED,
						RequestChatServiceResponse.Result.SUCCESS);
				sendResult(mCaller, resp);
				mCaller = null;
			}
		}

		@Override
		public void OnAudioChatRefused(long nGroupID, long nBusinessType,
				long nFromUserID) {
			if (mCaller != null) {
				JNIResponse resp = new RequestChatServiceResponse(RequestChatServiceResponse.REJCTED,
						RequestChatServiceResponse.Result.SUCCESS);
				sendResult(mCaller, resp);
				mCaller = null;
			}

		}

		@Override
		public void OnAudioChatInvite(long nGroupID, long nBusinessType,
				long nFromUserID) {

		}

		@Override
		public void OnAudioChatClosed(long nGroupID, long nBusinessType,
				long nFromUserID) {
			notifyListener(KEY_CANCELLED_LISTNER, 0, 0, null);
			//Clean cache
			mCaller = null;
		}
		
		

	}

}
