package com.bizcom.request;

import java.util.List;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.V2.jni.AudioRequest;
import com.V2.jni.AudioRequestCallback;
import com.V2.jni.ChatRequest;
import com.V2.jni.FileRequest;
import com.V2.jni.FileRequestCallbackAdapter;
import com.V2.jni.GroupRequest;
import com.V2.jni.VideoRequest;
import com.V2.jni.VideoRequestCallback;
import com.V2.jni.VideoRequestCallbackAdapter;
import com.V2.jni.ind.AudioJNIObjectInd;
import com.V2.jni.ind.VideoJNIObjectInd;
import com.V2.jni.util.EscapedcharactersProcessing;
import com.V2.jni.util.V2Log;
import com.bizcom.request.jni.FileTransStatusIndication;
import com.bizcom.request.jni.JNIResponse;
import com.bizcom.request.jni.RequestChatServiceResponse;
import com.bizcom.request.jni.FileTransStatusIndication.FileTransProgressStatusIndication;
import com.bizcom.vc.application.GlobalConfig;
import com.bizcom.vc.application.V2GlobalConstants;
import com.bizcom.vc.application.V2GlobalEnum;
import com.bizcom.vo.UserChattingObject;
import com.bizcom.vo.VMessage;
import com.bizcom.vo.VMessageAudioItem;
import com.bizcom.vo.VMessageFileItem;
import com.bizcom.vo.VMessageImageItem;

/**
 * <ul>
 * Internet Message Service.
 * </ul>
 * <p>
 * <li>
 * Send message {@link #sendVMessage(VMessage, MessageListener)}</li>
 * <li>
 * Invite User video or audio call
 * {@link #inviteUserChat(UserChattingObject, MessageListener)}</li>
 * <li>
 * Accept video or audio call
 * {@link #acceptChatting(UserChattingObject, MessageListener)}</li>
 * 
 * <li>
 * decline video or audio call
 * {@link #refuseChatting(UserChattingObject, MessageListener)}</li>
 * 
 * <li>
 * mute microphone when in conversation
 * {@link #muteChatting(UserChattingObject, MessageListener)}</li>
 * <li>
 * Operation file when transport file<br>
 * Notice: this operation doesn't contain send re-send operation. Send file use
 * {@link #sendVMessage(VMessage, MessageListener)}<br>
 * But when send file in progress, resume or cancel or suspend use this
 * function.
 * {@link #updateFileOperation(VMessageFileItem, FileOperationEnum, MessageListener)}
 * <br>
 * </li>
 * 
 * </p>
 * 
 * @author 28851274
 * 
 */
public class ChatService extends DeviceService {

	private static final String TAG = "ChatService";

	private AudioRequestCallback callback;

	private VideoRequestCallback videoCallback;

	private FileRequestCB fileCallback;

	private MessageListener mCaller;

	private Handler thread;

	private HandlerThread backEndThread;

	private static final int KEY_CANCELLED_LISTNER = 1;
	private static final int KEY_FILE_TRANS_STATUS_NOTIFICATION_LISTNER = 2;
	private static final int KEY_VIDEO_CONNECTED = 3;
	private static final int KEY_P2P_CALL_RESPONSE = 4;
	private static final int KEY_P2P_RECORD_CALL_RESPONSE = 5;

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
	 * 
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
	 * 
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
	 * 
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

	public void registerP2PCallResponseListener(Handler h, int what, Object obj) {
		registerListener(KEY_P2P_CALL_RESPONSE, h, what, obj);
	}

	public void removeP2PCallResponseListener(Handler h, int what, Object obj) {
		unRegisterListener(KEY_P2P_CALL_RESPONSE, h, what, obj);

	}

	public void registerP2PRecordResponseListener(Handler h, int what,
			Object obj) {
		registerListener(KEY_P2P_RECORD_CALL_RESPONSE, h, what, obj);
	}

	public void removeP2PRecordResponseListener(Handler h, int what, Object obj) {
		unRegisterListener(KEY_P2P_RECORD_CALL_RESPONSE, h, what, obj);

	}

	/**
	 * send message
	 * 
	 * @param msg
	 * @param caller
	 */
	public void sendVMessage(final VMessage msg, final MessageListener caller) {
		if (msg == null) {
			V2Log.e(TAG,
					"Send Message fail ! Because VMessage Object is null ! please check!");
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
				V2Log.d(TAG,
						"OnSendChat ChatService---> eGroupType :"
								+ msg.getMsgCode() + " | nGroupID: "
								+ msg.getGroupId() + " | sSeqID: "
								+ msg.getUUID() + " | sendContent: "
								+ msg.getTextContent());
				// send image message
				List<VMessageImageItem> imageItems = msg.getImageItems();
				for (int i = 0; imageItems != null && i < imageItems.size(); i++) {
					VMessageImageItem item = imageItems.get(i);
					// byte[] data = item.loadImageData();
					ChatRequest.getInstance().sendBinaryMessage(
							msg.getMsgCode(),
							msg.getGroupId(),
							msg.getToUser() == null ? 0 : msg.getToUser()
									.getmUserId(), 2, item.getUuid(),
							item.getFilePath(), 0);
					V2Log.d(TAG,
							"OnSendBinary ChatService---> eGroupType :"
									+ msg.getMsgCode() + " | nGroupID: "
									+ msg.getGroupId() + " | MessageID: "
									+ msg.getUUID() + " | sSeqID: "
									+ item.getUuid());
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
					V2Log.d(TAG,
							"OnSendBinary ChatService---> eGroupType :"
									+ msg.getMsgCode() + " | nGroupID: "
									+ msg.getGroupId() + " | MessageID: "
									+ msg.getUUID() + " | sSeqID: "
									+ audioList.get(i).getUuid());
				}
			}
		});
	}

	public void sendFileMessage(VMessage vm, MessageListener caller) {
		List<VMessageFileItem> items = vm.getFileItems();
		if (items == null || items.size() <= 0) {
			if (caller != null) {
				JNIResponse resp = new RequestChatServiceResponse(
						RequestChatServiceResponse.Result.INCORRECT_PAR);
				sendResult(caller, resp);
			} else {
				V2Log.e(TAG, "Incorrect parameters");
			}
			return;
		}

		for (VMessageFileItem item : items) {
			String xml = EscapedcharactersProcessing.convertAmp(item
					.toXmlItem());
			if (vm.getToUser() == null) {
				GroupRequest.getInstance().groupUploadFile(vm.getMsgCode(),
						vm.getGroupId(), xml);
			} else {
				FileRequest.getInstance().inviteFileTrans(
						vm.getToUser().getmUserId(), xml,
						V2GlobalEnum.FILE_TYPE_OFFLINE);
			}

		}
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
			FileOperationEnum opt, MessageListener caller) {
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
			// if(vfi.getState() ==
			// VMessageAbstractItem.STATE_FILE_DOWNLOADED_FALIED)
			// FileRequest.getInstance().httpDownloadFile(url, sfileid,
			// filePath, encrypttype);
			String path = GlobalConfig.getGlobalFilePath() + "/" + vfi.getFileName();
			V2Log.d(TAG, "start download file! id is : " + vfi.getUuid() + " and path is : " + path);
			FileRequest.getInstance().acceptFileTrans(vfi.getUuid(),path);
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
	public void inviteUserChat(UserChattingObject ud, MessageListener caller) {
		JNIResponse resp = null;
		// if (mCaller != null) {
		// V2Log.e(" audio call is on going");
		// resp = new RequestChatServiceResponse(
		// RequestChatServiceResponse.Result.INCORRECT_PAR);
		// }

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
				String szSessionID = ud.getSzSessionID();
				szSessionID = "ByVideo" + szSessionID;
				AudioRequest.getInstance().InviteAudioChat(szSessionID,
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
	public void closeChat(UserChattingObject ud, MessageListener caller) {
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

				Log.i("temptag20141030 1",
						"ud.getSzSessionID():" + ud.getSzSessionID()
								+ " ud.getUser().getmUserId():"
								+ ud.getUser().getmUserId()
								+ " ud.getDeviceId():" + ud.getDeviceId());

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
	public void acceptChatting(UserChattingObject ud, MessageListener caller) {
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
	public void refuseChatting(UserChattingObject ud, MessageListener caller) {
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
	public void muteChatting(UserChattingObject ud, MessageListener caller) {
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

	public void startAudioRecord(String fileID) {
		AudioRequest.getInstance().RecordFile(fileID);
	}

	public void stopAudioRecord(String fileID) {
		AudioRequest.getInstance().StopRecord(fileID);
	}

	class VideoRequestCallbackImpl extends VideoRequestCallbackAdapter {

		@Override
		public void OnVideoChatAccepted(VideoJNIObjectInd ind) {
			RequestChatServiceResponse resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.ACCEPTED, ind.getFromUserId(),
					ind.getGroupId(), ind.getDeviceId(),
					RequestChatServiceResponse.Result.SUCCESS);
			resp.setUuid(ind.getSzSessionID());

			resp.setDeviceID(ind.getDeviceId());
			if (mCaller != null) {
				Log.i("temptag20141030 1",
						"VideoRequestCallbackImpl 1 ind.getDeviceId()"
								+ ind.getDeviceId());
				sendResult(mCaller, resp);
				mCaller = null;
			} else {
				Log.i("temptag20141030 1",
						"VideoRequestCallbackImpl 2 ind.getDeviceId()"
								+ ind.getDeviceId());
				notifyListener(KEY_P2P_CALL_RESPONSE, 0, 0, resp);
			}
		}

		@Override
		public void OnVideoChatRefused(VideoJNIObjectInd ind) {
			JNIResponse resp = new RequestChatServiceResponse(
					RequestChatServiceResponse.REJCTED,
					RequestChatServiceResponse.Result.SUCCESS);
			if (mCaller != null) {
				sendResult(mCaller, resp);
				mCaller = null;
			} else {
				Log.i("temptag20141030 1", "OnVideoChatRefused()");
				notifyListener(KEY_P2P_CALL_RESPONSE, 0, 0, resp);
			}
		}

		@Override
		public void OnVideoChatClosed(VideoJNIObjectInd ind) {
//			Log.i("temptag20141030 1", "OnVideoChatClosed()");
//			notifyListener(KEY_CANCELLED_LISTNER, 0, 0, null);
//			mCaller = null;
		}

		@Override
		public void OnVideoChating(VideoJNIObjectInd ind) {
			Log.i("temptag20141030 1", "OnVideoChating()");
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
			} else {
				Log.i("temptag20141030 1", "OnAudioChatAccepted");
				notifyListener(KEY_P2P_CALL_RESPONSE, 0, 0, resp);
			}
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
				Log.i("temptag20141030 1", "OnAudioChatRefused");
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

		@Override
		public void OnRecordStart(AudioJNIObjectInd ind) {
			notifyListener(KEY_P2P_RECORD_CALL_RESPONSE, ind.getResult(),
					V2GlobalConstants.RECORD_TYPE_START, ind.getSzSessionID());
		}

		@Override
		public void OnRecordStop(AudioJNIObjectInd ind) {
			notifyListener(KEY_P2P_RECORD_CALL_RESPONSE, ind.getResult(),
					V2GlobalConstants.RECORD_TYPE_STOP, ind.getSzSessionID());
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
				long nFileSize, int nTransType) {
			notifyListener(KEY_FILE_TRANS_STATUS_NOTIFICATION_LISTNER, 0, 0,
					new FileTransProgressStatusIndication(nTransType, szFileID,
							nFileSize,
							FileTransStatusIndication.IND_TYPE_PROGRESS_END));
		}

		// @Override
		// public void OnFileDownloadError(String szFileID, int errorCode,
		// int nTransType) {
		// notifyListener(KEY_FILE_TRANS_STATUS_NOTIFICATION_LISTNER, 0, 0,
		// new FileDownLoadErrorIndication(szFileID, errorCode,
		// nTransType));
		// }

		// @Override
		// public void OnFileTransError(String szFileID, int errorCode,
		// int nTransType) {
		// notifyListener(KEY_FILE_TRANS_STATUS_NOTIFICATION_LISTNER, 0, 0,
		// new FileTransErrorIndication(szFileID, errorCode,
		// nTransType));
		// }
		//
		// @Override
		// public void OnFileTransCancel(String szFileID) {
		// notifyListener(KEY_FILE_TRANS_STATUS_NOTIFICATION_LISTNER, 0, 0,
		// new FileTransCannelIndication(szFileID));
		// }
	}

}
