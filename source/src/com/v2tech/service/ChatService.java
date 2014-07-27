package com.v2tech.service;

import java.util.List;

import android.os.Handler;
import android.os.HandlerThread;

import com.V2.jni.AudioRequest;
import com.V2.jni.AudioRequestCallback;
import com.V2.jni.ChatRequest;
import com.V2.jni.FileRequest;
import com.V2.jni.FileRequestCallback;
import com.V2.jni.V2GlobalEnum;
import com.V2.jni.VideoRequest;
import com.V2.jni.VideoRequestCallback;
import com.V2.jni.VideoRequestCallbackAdapter;
import com.V2.jni.ind.AudioJNIObjectInd;
import com.V2.jni.ind.VideoJNIObjectInd;
import com.v2tech.service.jni.FileTransStatusIndication.FileTransErrorIndication;
import com.v2tech.service.jni.FileTransStatusIndication.FileTransProgressStatusIndication;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.service.jni.RequestChatServiceResponse;
import com.v2tech.service.jni.RequestSendMessageResponse;
import com.v2tech.util.V2Log;
import com.v2tech.vo.UserChattingObject;
import com.v2tech.vo.UserDeviceConfig;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.VMessageAudioItem;
import com.v2tech.vo.VMessageFileItem;
import com.v2tech.vo.VMessageImageItem;

/**
 * <ul>
 * Internet Message Service.
 * </ul>
 * <ul>
 * Send message {@link #sendVMessage(VMessage, Registrant)}
 * </ul>
 * <ul>
 * Invite User video or audio call
 * {@link #inviteUserChat(UserChattingObject, Registrant)}
 * </ul>
 * 
 * @author 28851274
 * 
 */
public class ChatService extends AbstractHandler {

	/**
	 * Pause sending file to others
	 */
	public static final int OPERATION_PAUSE_SENDING = 1;

	/**
	 * Resume send file to others
	 */
	public static final int OPERATION_RESUME_SEND = 2;

	/**
	 * Pause downloading file
	 */
	public static final int OPERATION_PAUSE_DOWNLOADING = 3;

	/**
	 * Resume download file
	 */
	public static final int OPERATION_RESUME_DOWNLOAD = 4;

	/**
	 * Cancel sending file
	 */
	public static final int OPERATION_CANCEL_SENDING = 5;

	/**
	 * Cancel download file
	 */
	public static final int OPERATION_CANCEL_DOWNLOADING = 6;

	private AudioRequestCallback callback;

	private VideoRequestCallback videoCallback;

	private FileRequestCB fileCallback;

	private Registrant mCaller;

	private Handler thread;

	private static final int KEY_CANCELLED_LISTNER = 1;
	private static final int KEY_FILE_TRANS_STATUS_NOTIFICATION_LISTNER = 2;

	public ChatService() {
		super();
		init();
	}

	private void init() {
		callback = new AudioRequestCallbackImpl();
		AudioRequest.getInstance().addCallback(callback);

		videoCallback = new VideoRequestCallbackImpl();
		VideoRequest.getInstance().addCallback(videoCallback);

		fileCallback = new FileRequestCB();
		FileRequest.getInstance().addCallback(fileCallback);

		HandlerThread backEnd = new HandlerThread("back-end");
		backEnd.start();
		thread = new Handler(backEnd.getLooper());
	}

	/**
	 * Register listener for out conference by kick.
	 * 
	 * @param msg
	 */
	public void registerCancelledListener(Handler h, int what, Object obj) {
		registerListener(KEY_CANCELLED_LISTNER, h, what, obj);
	}

	public void removeRegisterCancelledListener(Handler h, int what, Object obj) {
		unRegisterListener(KEY_CANCELLED_LISTNER, h, what, obj);

	}

	/**
	 * Register listener for out conference by kick.
	 * 
	 * @param msg
	 */
	public void registerFileTransStatusListener(Handler h, int what, Object obj) {
		registerListener(KEY_FILE_TRANS_STATUS_NOTIFICATION_LISTNER, h, what,
				obj);
	}

	public void removeRegisterFileTransStatusListener(Handler h, int what,
			Object obj) {
		unRegisterListener(KEY_FILE_TRANS_STATUS_NOTIFICATION_LISTNER, h, what,
				obj);

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

				// Send file
				if (msg.getFileItems().size() > 0) {
					sendFileMessage(msg, null);
					return;
				}
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
							audioList.get(i).getUuid(),
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

	public void sendFileMessage(VMessage vm, Registrant caller) {
		List<VMessageFileItem> items = vm.getFileItems();
		if (items == null || items.size() <= 0) {
			if (caller != null) {
				JNIResponse resp = new RequestChatServiceResponse(
						RequestChatServiceResponse.Result.INCORRECT_PAR);
				sendResult(caller, resp);
			} else {
				V2Log.e("Incorrect parameters");
			}
			return;
		}

		StringBuilder sb = new StringBuilder();
		for (VMessageFileItem item : items) {
			sb.append(
					"<file id=\"" + item.getUuid() + "\" name=\""
							+ item.getFilePath() + "\" encrypttype=\"0\"  />")
					.append("\n");
		}

		FileRequest.getInstance().inviteFileTrans(vm.getToUser().getmUserId(),
				sb.toString(), 2);

		JNIResponse resp = new RequestChatServiceResponse(
				RequestChatServiceResponse.Result.SUCCESS);
		sendResult(caller, resp);
	}

	/**
	 * Update file operation, like pause transport; resume transport; cancel
	 * transport
	 * 
	 * @param vfi
	 *            file item Object
	 * @param opt
	 *            operation of file
	 * @param caller
	 * 
	 * @see {@link #OPERATION_PAUSE_SENDING}
	 * @see {@link #OPERATION_RESUME_SEND}
	 * @see {@link #OPERATION_PAUSE_DOWNLOADING}
	 * @see {@link #OPERATION_RESUME_DOWNLOAD}
	 * @see {@link #OPERATION_CANCEL_SENDING}
	 * @see {@link #OPERATION_CANCEL_DOWNLOADING}
	 */
	public void updateFileOperation(VMessageFileItem vfi, int opt,
			Registrant caller) {
		if (vfi == null
				|| (opt < OPERATION_PAUSE_SENDING || opt > OPERATION_CANCEL_DOWNLOADING)) {
			JNIResponse resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.Result.INCORRECT_PAR);
			sendResult(caller, resp);
			return;
		}

		switch (opt) {
		case OPERATION_PAUSE_SENDING:
			FileRequest.getInstance().pauseSendFile(vfi.getUuid(),
					vfi.getTransType());
			break;
		case OPERATION_RESUME_SEND:
			FileRequest.getInstance().resumeSendFile(vfi.getUuid(),
					vfi.getTransType());
			break;
		case OPERATION_PAUSE_DOWNLOADING:
			FileRequest.getInstance().pauseHttpRecvFile(vfi.getUuid(),
					vfi.getTransType());
			break;
		case OPERATION_RESUME_DOWNLOAD:
			FileRequest.getInstance().resumeHttpRecvFile(vfi.getUuid(),
					vfi.getTransType());
			break;
		case OPERATION_CANCEL_SENDING:
			FileRequest.getInstance().cancelSendFile(vfi.getUuid(),
					vfi.getTransType());
			break;
		case OPERATION_CANCEL_DOWNLOADING:
			FileRequest.getInstance().cancelRecvFile(vfi.getUuid(),
					vfi.getTransType());
			break;

		}

		JNIResponse resp = new RequestChatServiceResponse(
				RequestChatServiceResponse.Result.SUCCESS);
		sendResult(caller, resp);

	}

	/**
	 * Invite contact for chat
	 * 
	 * @param ud
	 * @param caller
	 */
	public void inviteUserChat(UserChattingObject ud, Registrant caller) {
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
					ud.getUser().getmUserId(), V2GlobalEnum.REQUEST_TYPE_IM);
		} else if (ud.isVideoType()) {
			VideoRequest.getInstance().inviteVideoChat(ud.getGroupdId(),
					ud.getUser().getmUserId(), ud.getDeviceId(),
					V2GlobalEnum.REQUEST_TYPE_IM);
		}
	}

	/**
	 * Cancel current video or audio conversation.
	 * 
	 * @param ud
	 * @param caller
	 */
	public void cancelChattingCall(UserChattingObject ud, Registrant caller) {
		if (ud == null) {
			JNIResponse resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.Result.INCORRECT_PAR);
			sendResult(caller, resp);
			return;
		}

		if (ud.isAudioType()) {
			if (ud.isConnected() || ! ud.isIncoming()) {
				AudioRequest.getInstance()
						.CloseAudioChat(ud.getGroupdId(),
								ud.getUser().getmUserId(),
								V2GlobalEnum.REQUEST_TYPE_IM);
			} else {
				AudioRequest.getInstance()
						.RefuseAudioChat(ud.getGroupdId(),
								ud.getUser().getmUserId(),
								V2GlobalEnum.REQUEST_TYPE_IM);
			}

		} else if (ud.isVideoType()) {
			if (ud.isConnected() || ! ud.isIncoming()) {
				VideoRequest.getInstance().closeVideoChat(ud.getGroupdId(),
						ud.getUser().getmUserId(), ud.getDeviceId(),
						V2GlobalEnum.REQUEST_TYPE_IM);
			} else {
				VideoRequest.getInstance().refuseVideoChat(ud.getGroupdId(),
						ud.getUser().getmUserId(), ud.getDeviceId(),
						V2GlobalEnum.REQUEST_TYPE_IM);
			}
		}

		if (caller != null) {
			JNIResponse resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.Result.SUCCESS);
			sendResult(caller, resp);
		}

		this.mCaller = null;
	}

	/**
	 * accept incoming call
	 * 
	 * @param ud
	 * @param caller
	 */
	public void acceptChatting(UserChattingObject ud, Registrant caller) {
		if (ud == null) {
			JNIResponse resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.Result.INCORRECT_PAR);
			sendResult(caller, resp);
			return;
		}

		if (ud.isAudioType()) {
			AudioRequest.getInstance().AcceptAudioChat(ud.getGroupdId(),
					ud.getUser().getmUserId(), V2GlobalEnum.REQUEST_TYPE_IM);
		} else if (ud.isVideoType()) {
			VideoRequest.getInstance().acceptVideoChat(ud.getGroupdId(),
					ud.getUser().getmUserId(), ud.getDeviceId(),
					V2GlobalEnum.REQUEST_TYPE_IM);
		}
		if (caller != null) {
			JNIResponse resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.Result.SUCCESS);
			sendResult(caller, resp);
		}
	}

	/**
	 * Reject incoming call
	 * 
	 * @param ud
	 * @param caller
	 */
	public void refuseChatting(UserChattingObject ud, Registrant caller) {
		if (ud == null) {
			JNIResponse resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.Result.INCORRECT_PAR);
			sendResult(caller, resp);
			return;
		}

		if (ud.isAudioType()) {
			AudioRequest.getInstance().RefuseAudioChat(ud.getGroupdId(),
					ud.getUser().getmUserId(), V2GlobalEnum.REQUEST_TYPE_IM);
		} else if (ud.isVideoType()) {
			VideoRequest.getInstance().refuseVideoChat(ud.getGroupdId(),
					ud.getUser().getmUserId(), ud.getDeviceId(),
					V2GlobalEnum.REQUEST_TYPE_IM);
		}
		if (caller != null) {
			JNIResponse resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.Result.SUCCESS);
			sendResult(caller, resp);
		}
	}

	public void muteChatting(UserChattingObject ud, Registrant caller) {
		if (ud == null) {
			JNIResponse resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.Result.INCORRECT_PAR);
			sendResult(caller, resp);
			return;
		}
		AudioRequest.getInstance().MuteMic(ud.getGroupdId(),
				ud.getUser().getmUserId(), ud.isMute(),
				V2GlobalEnum.REQUEST_TYPE_IM);

		if (caller != null) {
			JNIResponse resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.Result.SUCCESS);
			sendResult(caller, resp);
		}

	}

	/**
	 * Open remote video device of conversation
	 * 
	 * @param ud
	 * @param caller
	 */
	public void openVideoDevice(UserChattingObject ud, Registrant caller) {
		if (ud == null) {
			JNIResponse resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.Result.INCORRECT_PAR);
			sendResult(caller, resp);
			return;
		}

		VideoRequest
				.getInstance()
				.openVideoDevice(
						UserDeviceConfig.UserDeviceConfigType.EVIDEODEVTYPE_CAMERA
								.ordinal(), ud.getUser().getmUserId(),
						ud.getDeviceId(), ud.getVp(),
						V2GlobalEnum.REQUEST_TYPE_IM);

		if (caller != null) {
			JNIResponse resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.Result.SUCCESS);
			sendResult(caller, resp);
		}
	}

	public void closeVideoDevice(UserChattingObject ud, Registrant caller) {
		if (ud == null) {
			JNIResponse resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.Result.INCORRECT_PAR);
			sendResult(caller, resp);
			return;
		}

		VideoRequest
				.getInstance()
				.closeVideoDevice(
						UserDeviceConfig.UserDeviceConfigType.EVIDEODEVTYPE_CAMERA
								.ordinal(), ud.getUser().getmUserId(),
						ud.getDeviceId(), ud.getVp(),
						V2GlobalEnum.REQUEST_TYPE_IM);

		if (caller != null) {
			JNIResponse resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.Result.SUCCESS);
			sendResult(caller, resp);
		}
	}

	class VideoRequestCallbackImpl extends VideoRequestCallbackAdapter {

		public void OnVideoChatAccepted(long nGroupID, int nBusinessType,
				long nFromuserID, String szDeviceID) {
			if (mCaller != null) {
				JNIResponse resp = new RequestChatServiceResponse(
						RequestChatServiceResponse.ACCEPTED,
						RequestChatServiceResponse.Result.SUCCESS);
				sendResult(mCaller, resp);
				mCaller = null;
			}
		}

		public void OnVideoChatRefused(long nGroupID, int nBusinessType,
				long nFromUserID, String szDeviceID) {
			if (mCaller != null) {
				JNIResponse resp = new RequestChatServiceResponse(
						RequestChatServiceResponse.REJCTED,
						RequestChatServiceResponse.Result.SUCCESS);
				sendResult(mCaller, resp);
				mCaller = null;
			}
		}

		@Override
		public void OnVideoChatClosed(VideoJNIObjectInd ind) {
			notifyListener(KEY_CANCELLED_LISTNER, 0, 0, null);
			// Clean cache
			mCaller = null;
		}

	}

	class AudioRequestCallbackImpl implements AudioRequestCallback {

		@Override
		public void OnAudioChatAccepted(AudioJNIObjectInd ind) {
			if (mCaller != null) {
				JNIResponse resp = new RequestChatServiceResponse(
						RequestChatServiceResponse.ACCEPTED,
						RequestChatServiceResponse.Result.SUCCESS);
				sendResult(mCaller, resp);
				mCaller = null;
			}
		}

		@Override
		public void OnAudioChatRefused(AudioJNIObjectInd ind) {
			if (mCaller != null) {
				JNIResponse resp = new RequestChatServiceResponse(
						RequestChatServiceResponse.REJCTED,
						RequestChatServiceResponse.Result.SUCCESS);
				sendResult(mCaller, resp);
				mCaller = null;
			//Else means remote side is out and then cancel calling
			} else {
				notifyListener(KEY_CANCELLED_LISTNER, 0, 0, null);
			}

		}

		@Override
		public void OnAudioChatInvite(AudioJNIObjectInd ind) {

		}

		@Override
		public void OnAudioChatClosed(AudioJNIObjectInd ind) {
			notifyListener(KEY_CANCELLED_LISTNER, 0, 0, null);
			// Clean cache
			mCaller = null;
		}

	}

	class FileRequestCB implements FileRequestCallback {

		@Override
		public void OnFileTransInvite(long nGroupID, int nBusinessType,
				long userid, String szFileID, String szFileName,
				long nFileBytes, int linetype) {

		}

		@Override
		public void OnFileTransProgress(String szFileID, long nBytesTransed,
				int nTransType) {
			notifyListener(KEY_FILE_TRANS_STATUS_NOTIFICATION_LISTNER, 0, 0,
					new FileTransProgressStatusIndication(nTransType, szFileID,
							nBytesTransed));

		}

		@Override
		public void OnFileTransEnd(String szFileID, String szFileName,
				long nFileSize, int nTransType) {
			notifyListener(KEY_FILE_TRANS_STATUS_NOTIFICATION_LISTNER, 0, 0,
					new FileTransProgressStatusIndication(nTransType, szFileID,
							nFileSize));
		}

		@Override
		public void OnFileDownloadError(String sFileID) {
			notifyListener(KEY_FILE_TRANS_STATUS_NOTIFICATION_LISTNER, 0, 0,
					new FileTransErrorIndication(sFileID));

		}

	}

}
