package com.v2tech.service;

import java.util.List;

import android.os.Handler;
import android.os.HandlerThread;

import com.V2.jni.AudioRequest;
import com.V2.jni.AudioRequestCallback;
import com.V2.jni.ChatRequest;
import com.V2.jni.FileRequest;
import com.V2.jni.FileRequestCallbackAdapter;
import com.V2.jni.GroupRequest;
import com.V2.jni.V2GlobalEnum;
import com.V2.jni.VideoRequest;
import com.V2.jni.VideoRequestCallback;
import com.V2.jni.VideoRequestCallbackAdapter;
import com.V2.jni.ind.AudioJNIObjectInd;
import com.V2.jni.ind.VideoJNIObjectInd;
import com.V2.jni.util.V2Log;
import com.v2tech.service.jni.FileDownLoadErrorIndication;
import com.v2tech.service.jni.FileTransCannelIndication;
import com.v2tech.service.jni.FileTransStatusIndication;
import com.v2tech.service.jni.FileTransStatusIndication.FileTransErrorIndication;
import com.v2tech.service.jni.FileTransStatusIndication.FileTransProgressStatusIndication;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.service.jni.RequestChatServiceResponse;
import com.v2tech.service.jni.RequestSendMessageResponse;
import com.v2tech.util.GlobalConfig;
import com.v2tech.vo.UserChattingObject;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.VMessageAudioItem;
import com.v2tech.vo.VMessageFileItem;
import com.v2tech.vo.VMessageImageItem;

/**
 * <ul>
 * Internet Message Service.
 * </ul>
 * <p>
 * <li>
 * Send message {@link #sendVMessage(VMessage, Registrant)}</li>
 * <li>
 * Invite User video or audio call
 * {@link #inviteUserChat(UserChattingObject, Registrant)}</li>
 * <li>
 * Accept video or audio call
 * {@link #acceptChatting(UserChattingObject, Registrant)}</li>
 * 
 * <li>
 * decline video or audio call
 * {@link #refuseChatting(UserChattingObject, Registrant)}</li>
 * 
 * <li>
 * mute microphone when in conversation
 * {@link #muteChatting(UserChattingObject, Registrant)}</li>
 * <li>
 * Operation file when transport file<br>
 * Notice: this operation doesn't contain send re-send operation. Send file use
 * {@link #sendVMessage(VMessage, Registrant)}<br>
 * But when send file in progress, resume or cancel or suspend use this
 * function.
 * {@link #updateFileOperation(VMessageFileItem, FileOperationEnum, Registrant)}
 * <br>
 * </li>
 * 
 * </p>
 * 
 * @author 28851274
 * 
 */
public class ChatService extends DeviceService {

	private AudioRequestCallback callback;

	private VideoRequestCallback videoCallback;

	private FileRequestCB fileCallback;

	private Registrant mCaller;

	private Handler thread;
	
	private HandlerThread backEndThread;

	private static final int KEY_CANCELLED_LISTNER = 1;
	private static final int KEY_FILE_TRANS_STATUS_NOTIFICATION_LISTNER = 2;
	private static final int KEY_VIDEO_CONNECTED = 3;
	private static final int KEY_P2P_CALL_RESPONSE = 4;

	private static final String TAG = "ChatService";

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

		backEndThread = new HandlerThread("back-end");
		backEndThread.start();
		thread = new Handler(backEndThread.getLooper());
	}
	
	
	@Override
	public void clearCalledBack() {
		AudioRequest.getInstance().removeCallback(callback);
		VideoRequest.getInstance().removeCallback(videoCallback);
		FileRequest.getInstance().removeCallback(fileCallback);
		backEndThread.quit();
	}


    /**
     * Register listener for out conference by kick.
     * @param h
     * @param what
     * @param obj
     */
	public void registerCancelledListener(Handler h, int what, Object obj) {
		registerListener(KEY_CANCELLED_LISTNER, h, what, obj);
	}

	public void removeRegisterCancelledListener(Handler h, int what, Object obj) {
		unRegisterListener(KEY_CANCELLED_LISTNER, h, what, obj);

	}

    /**
     * Register listener for video connected, after this notification. can open
     * remote video device
     * @param h
     * @param what
     * @param obj
     */
	public void registerVideoChatConnectedListener(Handler h, int what,
			Object obj) {
		registerListener(KEY_VIDEO_CONNECTED, h, what, obj);
	}

	public void removeVideoChatConnectedistener(Handler h, int what, Object obj) {
		unRegisterListener(KEY_VIDEO_CONNECTED, h, what, obj);

	}

    /**
     * Register listener for out conference by kick.
     * @param h
     * @param what
     * @param obj
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
	
	
	public void registerP2PCallResponseListener(Handler h, int what,
			Object obj) {
		registerListener(KEY_P2P_CALL_RESPONSE, h, what, obj);
	}

	public void removeP2PCallResponseListener(Handler h, int what, Object obj) {
		unRegisterListener(KEY_P2P_CALL_RESPONSE, h, what, obj);

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
				// then send text message
				ChatRequest.getInstance().sendTextMessage(
						msg.getMsgCode(),
						msg.getGroupId(),
						msg.getToUser() == null ? 0 : msg.getToUser()
								.getmUserId(), msg.getUUID(), msg.toXml(),
						msg.getTextContent().length());

				// send image message
				List<VMessageImageItem> imageItems = msg.getImageItems();
				for (VMessageImageItem item : imageItems) {
					// byte[] data = item.loadImageData();
					ChatRequest.getInstance().sendBinaryMessage(
							msg.getMsgCode(),
							msg.getGroupId(),
							msg.getToUser() == null ? 0 : msg.getToUser()
									.getmUserId(), 2, item.getUuid(),
							item.getFilePath(), 0);
				}

				// send aduio message
				List<VMessageAudioItem> audioList = msg.getAudioItems();
				for (int i = 0; audioList != null && i < audioList.size(); i++) {
					ChatRequest.getInstance().sendBinaryMessage(
							msg.getMsgCode(),
							msg.getGroupId(),
							msg.getToUser() == null ? 0 : msg.getToUser()
									.getmUserId(), 3,
							audioList.get(i).getUuid(),
							audioList.get(i).getAudioFilePath(), 0);
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

		for (VMessageFileItem item : items) {
			if (vm.getToUser() == null) {
				GroupRequest.getInstance().groupUploadFile(vm.getMsgCode(),
						vm.getGroupId(), item.toXmlItem());
			} else {
				FileRequest.getInstance().inviteFileTrans(
						vm.getToUser().getmUserId(), item.toXmlItem(),
						V2GlobalEnum.FILE_TYPE_OFFLINE);
			}

		}
		RequestChatServiceResponse resp = new RequestChatServiceResponse(
				RequestChatServiceResponse.Result.SUCCESS);
		resp.setUuid(vm.getUUID());
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
	 * @see FileOperationEnum
	 */
	public void updateFileOperation(VMessageFileItem vfi,
			FileOperationEnum opt, Registrant caller) {
		if (vfi == null || (opt == null)) {
			JNIResponse resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.Result.INCORRECT_PAR);
			sendResult(caller, resp);
			return;
		}

		switch (opt) {
		case OPERATION_PAUSE_SENDING:
			FileRequest.getInstance().pauseSendFile(vfi.getUuid());
			break;
		case OPERATION_RESUME_SEND:
			FileRequest.getInstance().resumeSendFile(vfi.getUuid());
			break;
		case OPERATION_PAUSE_DOWNLOADING:
			FileRequest.getInstance().pauseHttpRecvFile(vfi.getUuid());
			break;
		case OPERATION_RESUME_DOWNLOAD:
			FileRequest.getInstance().resumeHttpRecvFile(vfi.getUuid());
			break;
		case OPERATION_CANCEL_SENDING:
			FileRequest.getInstance().cancelSendFile(vfi.getUuid());
			break;
		case OPERATION_CANCEL_DOWNLOADING:
			FileRequest.getInstance().cancelRecvFile(vfi.getUuid());
			break;
		case OPERATION_START_DOWNLOAD:
			FileRequest.getInstance().acceptFileTrans(
					vfi.getUuid(),
					GlobalConfig.getGlobalFilePath() + "/" + vfi.getFileName());
		default:
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

			AudioRequest.getInstance().InviteAudioChat(ud.getSzSessionID(),
					ud.getUser().getmUserId());

		} else if (ud.isVideoType()) {

			// If connected, send audio message
			if (ud.isConnected()) {
				AudioRequest.getInstance().InviteAudioChat(ud.getSzSessionID(),
						ud.getUser().getmUserId());
			} else {
				VideoRequest.getInstance().inviteVideoChat(ud.getSzSessionID(),
						ud.getUser().getmUserId(), ud.getDeviceId());
			}

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
			if (ud.isConnected() || !ud.isIncoming()) {
				AudioRequest.getInstance().CloseAudioChat(ud.getSzSessionID(),
						ud.getUser().getmUserId());
			} else {
				AudioRequest.getInstance().RefuseAudioChat(ud.getSzSessionID(),
						ud.getUser().getmUserId());
			}

		} else if (ud.isVideoType()) {
			if (ud.isConnected() || !ud.isIncoming()) {
				VideoRequest.getInstance().closeVideoChat(ud.getSzSessionID(),
						ud.getUser().getmUserId(), ud.getDeviceId());

				AudioRequest.getInstance().CloseAudioChat(ud.getSzSessionID(),
						ud.getUser().getmUserId());
			} else {
				VideoRequest.getInstance().refuseVideoChat(ud.getSzSessionID(),
						ud.getUser().getmUserId(), ud.getDeviceId());
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
			AudioRequest.getInstance().AcceptAudioChat(ud.getSzSessionID(),
					ud.getUser().getmUserId());
		} else if (ud.isVideoType()) {
			VideoRequest.getInstance().acceptVideoChat(ud.getSzSessionID(),
					ud.getUser().getmUserId(), ud.getDeviceId());
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
			AudioRequest.getInstance().RefuseAudioChat(ud.getSzSessionID(),
					ud.getUser().getmUserId());
		} else if (ud.isVideoType()) {
			VideoRequest.getInstance().refuseVideoChat(ud.getSzSessionID(),
					ud.getUser().getmUserId(), ud.getDeviceId());
		}
		if (caller != null) {
			JNIResponse resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.Result.SUCCESS);
			sendResult(caller, resp);
		}
	}

	/**
	 * Mute microphone when in conversation
	 * 
	 * @param ud
	 * @param caller
	 */
	public void muteChatting(UserChattingObject ud, Registrant caller) {
		if (ud == null) {
			JNIResponse resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.Result.INCORRECT_PAR);
			sendResult(caller, resp);
			return;
		}
		AudioRequest.getInstance().MuteMic(ud.getGroupdId(),
				ud.getUser().getmUserId(), ud.isMute());

		if (caller != null) {
			JNIResponse resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.Result.SUCCESS);
			sendResult(caller, resp);
		}

	}

	public void suspendOrResumeAudio(boolean flag) {
		if (flag) {
			AudioRequest.getInstance().ResumePlayout();
		} else {
			AudioRequest.getInstance().PausePlayout();
		}
	}

	class VideoRequestCallbackImpl extends VideoRequestCallbackAdapter {

		@Override
		public void OnVideoChatAccepted(VideoJNIObjectInd ind) {
			RequestChatServiceResponse resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.ACCEPTED,
					ind.getFromUserId(), ind.getGroupId(),
					ind.getDeviceId(),
					RequestChatServiceResponse.Result.SUCCESS);
			resp.setUuid(ind.getSzSessionID());
			resp.setDeviceID(ind.getDeviceId());
			if (mCaller != null) {
				sendResult(mCaller, resp);
				mCaller = null;
			}
			else
				notifyListener(KEY_P2P_CALL_RESPONSE, 0, 0, resp);
		}

		@Override
		public void OnVideoChatRefused(VideoJNIObjectInd ind) {
			JNIResponse resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.REJCTED,
					RequestChatServiceResponse.Result.SUCCESS);
			if (mCaller != null) {
				sendResult(mCaller, resp);
				mCaller = null;
			}
			else
				notifyListener(KEY_P2P_CALL_RESPONSE, 0, 0, resp);
		}

		@Override
		public void OnVideoChatClosed(VideoJNIObjectInd ind) {
			notifyListener(KEY_CANCELLED_LISTNER, 0, 0, null);
			// Clean cache
			mCaller = null;
		}

		@Override
		public void OnVideoChating(VideoJNIObjectInd ind) {
			notifyListener(KEY_VIDEO_CONNECTED, 0, 0, ind);
		}

	}

	class AudioRequestCallbackImpl implements AudioRequestCallback {

		@Override
		public void OnAudioChatAccepted(AudioJNIObjectInd ind) {
			RequestChatServiceResponse resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.ACCEPTED,
					RequestChatServiceResponse.Result.SUCCESS);
			resp.setUuid(ind.getSzSessionID());
			if (mCaller != null) {
				sendResult(mCaller, resp);
				mCaller = null;
			}
			else
				notifyListener(KEY_P2P_CALL_RESPONSE, 0, 0, resp);
		}

		@Override
		public void OnAudioChatRefused(AudioJNIObjectInd ind) {
			JNIResponse resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.REJCTED,
					RequestChatServiceResponse.Result.SUCCESS,
					ind.getFromUserId());
			if (mCaller != null) {
				sendResult(mCaller, resp);
				mCaller = null;
				// Else means remote side is out and then cancel calling
			} else
				notifyListener(KEY_P2P_CALL_RESPONSE, 0, 0, resp);
			

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

	class FileRequestCB extends FileRequestCallbackAdapter {

		@Override
		public void OnFileTransProgress(String szFileID, long nBytesTransed,
				int nTransType) {
			notifyListener(
					KEY_FILE_TRANS_STATUS_NOTIFICATION_LISTNER,
					0,
					0,
					new FileTransProgressStatusIndication(
							nTransType,
							szFileID,
							nBytesTransed,
							FileTransStatusIndication.IND_TYPE_PROGRESS_TRANSING));

		}

		@Override
		public void OnFileTransEnd(String szFileID, String szFileName,
				long nFileSize, int nTransType ) {
			notifyListener(KEY_FILE_TRANS_STATUS_NOTIFICATION_LISTNER, 0, 0,
					new FileTransProgressStatusIndication(nTransType, szFileID,
							nFileSize,
							FileTransStatusIndication.IND_TYPE_PROGRESS_END));
		}

		@Override
		public void OnFileDownloadError(String szFileID, int errorCode,
				int nTransType) {
			notifyListener(KEY_FILE_TRANS_STATUS_NOTIFICATION_LISTNER, 0, 0,
					new FileDownLoadErrorIndication(szFileID, errorCode,
							nTransType));

		}

		@Override
		public void OnFileTransError(String szFileID, int errorCode,
				int nTransType) {
			notifyListener(KEY_FILE_TRANS_STATUS_NOTIFICATION_LISTNER, 0, 0,
					new FileTransErrorIndication(szFileID, errorCode,
							nTransType));
		}

		@Override
		public void OnFileTransCancel(String szFileID) {
			notifyListener(KEY_FILE_TRANS_STATUS_NOTIFICATION_LISTNER, 0, 0,
					new FileTransCannelIndication(szFileID));
		}
		
		
	}

}
