package com.v2tech.service;

import android.os.Handler;
import android.os.Message;

import com.V2.jni.ImRequest;
import com.V2.jni.ImRequestCallback;
import com.V2.jni.V2ClientType;
import com.V2.jni.V2GlobalEnum;
import com.v2tech.logic.AsynResult;
import com.v2tech.logic.GlobalHolder;
import com.v2tech.logic.User;
import com.v2tech.logic.jni.JNIResponse;
import com.v2tech.logic.jni.RequestLogInResponse;
import com.v2tech.util.V2Log;

public class UserService extends AbstractHandler {

	private static final int JNI_REQUEST_LOG_IN = 1;
	private static final int JNI_REQUEST_UPDAE_USER = 2;

	private ImRequestCB imCB = null;

	public UserService() {
		super();
		imCB = new ImRequestCB(this);
		ImRequest.getInstance().setCallback(imCB);
	}

	/**
	 * Asynchronous login function. After login, will call message.sendToTarget
	 * to caller
	 * 
	 * @param mail
	 *            user mail
	 * @param passwd
	 *            password
	 * @param message
	 *            callback message Message.obj is {@link AsynResult}
	 */
	public void login(String mail, String passwd, Message caller) {
		initTimeoutMessage(JNI_REQUEST_LOG_IN, null, DEFAULT_TIME_OUT_SECS,
				caller);
		ImRequest.getInstance().login(mail, passwd,
				V2GlobalEnum.USER_STATUS_ONLINE, V2ClientType.IM);
	}
	
	
	/**
	 * TODO update comment
	 * @param user
	 * @param caller
	 */
	public void updateUser(User user, Message caller) {
		if (user == null) {
			if (caller != null) {
				caller.obj = new AsynResult(AsynResult.AsynState.INCORRECT_PAR, null); 
				caller.sendToTarget();
			}
			return;
		}
		initTimeoutMessage(JNI_REQUEST_UPDAE_USER, null, DEFAULT_TIME_OUT_SECS,
				caller);
		if (user.getmUserId() == GlobalHolder.getInstance().getCurrentUserId()) {
			ImRequest.getInstance().modifyBaseInfo(user.toXml());
		} else {
			ImRequest.getInstance().modifyCommentName(user.getmUserId(), user.getName());
		}
	}

	@Override
	public void handleMessage(Message msg) {
		// handle time out
		super.handleMessage(msg);

		// remove time out message
		Message caller = super.removeTimeoutMessage(msg.what);
		if (caller == null) {
			V2Log.w("Igore message client don't expect callback");
			return;
		}

		switch (msg.what) {
		case JNI_REQUEST_LOG_IN:
			Object origObject = caller.obj;
			caller.obj = new AsynResult(AsynResult.AsynState.SUCCESS, msg.obj);
			JNIResponse jniRes = (JNIResponse) msg.obj;
			jniRes.callerObject = origObject;
			break;
		case JNI_REQUEST_UPDAE_USER:
			caller.obj = new AsynResult(AsynResult.AsynState.SUCCESS, msg.obj);
			break;
		}
		caller.sendToTarget();

	}

	class ImRequestCB implements ImRequestCallback {

		private Handler handler;

		public ImRequestCB(Handler handler) {
			this.handler = handler;
		}

		@Override
		public void OnLoginCallback(long nUserID, int nStatus, int nResult) {
			RequestLogInResponse.Result res = nResult == 1 ? RequestLogInResponse.Result.FAILED
					: RequestLogInResponse.Result.SUCCESS;
			Message.obtain(handler, JNI_REQUEST_LOG_IN,
					new RequestLogInResponse(new User(nUserID), res))
					.sendToTarget();
		}

		@Override
		public void OnLogoutCallback(int nUserID) {

		}

		@Override
		public void OnConnectResponseCallback(int nResult) {

		}

		@Override
		public void OnUpdateBaseInfoCallback(long nUserID, String updatexml) {
			Message.obtain(handler, JNI_REQUEST_UPDAE_USER, updatexml).sendToTarget();
		}

		@Override
		public void OnUserStatusUpdatedCallback(long nUserID, int eUEType,
				int nStatus, String szStatusDesc) {

		}

		@Override
		public void OnChangeAvatarCallback(int nAvatarType, long nUserID,
				String AvatarName) {

		}

	}
}
