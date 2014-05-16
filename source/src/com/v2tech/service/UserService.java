package com.v2tech.service;

import android.os.Handler;
import android.os.Message;

import com.V2.jni.ImRequest;
import com.V2.jni.ImRequestCallback;
import com.V2.jni.V2ClientType;
import com.V2.jni.V2GlobalEnum;
import com.v2tech.service.jni.RequestLogInResponse;
import com.v2tech.vo.User;

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
	 * Asynchronous login function. After login, will call post message to your handler
	 * 
	 * 
	 * @param mail
	 *            user mail
	 * @param passwd
	 *            password
	 * @param message
	 *            callback message Message.obj is {@link AsynResult}
	 */
	public void login(String mail, String passwd, Registrant caller) {
		initTimeoutMessage(JNI_REQUEST_LOG_IN, DEFAULT_TIME_OUT_SECS,
				caller);
		ImRequest.getInstance().login(mail, passwd,
				V2GlobalEnum.USER_STATUS_ONLINE, V2ClientType.ANDROID, false);
	}

	/**
	 * Update user information. If updated user is logged user, can update all
	 * information.<br>
	 * otherwise only can update nick name.
	 * 
	 * @param user
	 * @param caller
	 */
	public void updateUser(User user, Registrant caller) {
		if (user == null) {
			if (caller != null && caller.getHandler() != null) {
				sendResult(caller,
						new RequestLogInResponse(null,
								RequestLogInResponse.Result.INCORRECT_PAR,
								caller.getObject()));
			}
			return;
		}
		initTimeoutMessage(JNI_REQUEST_UPDAE_USER, DEFAULT_TIME_OUT_SECS,
				caller);
		if (user.getmUserId() == GlobalHolder.getInstance().getCurrentUserId()) {
			ImRequest.getInstance().modifyBaseInfo(user.toXml());
		} else {
			ImRequest.getInstance().modifyCommentName(user.getmUserId(),
					user.getName());
		}
	}

	class ImRequestCB implements ImRequestCallback {

		private Handler handler;

		public ImRequestCB(Handler handler) {
			this.handler = handler;
		}

		@Override
		public void OnLoginCallback(long nUserID, int nStatus, int nResult) {
			RequestLogInResponse.Result res = RequestLogInResponse.Result
					.fromInt(nResult);
			Message m = Message.obtain(handler, JNI_REQUEST_LOG_IN,
					new RequestLogInResponse(new User(nUserID), res));
			handler.dispatchMessage(m);
		}

		@Override
		public void OnLogoutCallback(int nUserID) {

		}

		@Override
		public void OnConnectResponseCallback(int nResult) {
			RequestLogInResponse.Result res = RequestLogInResponse.Result
					.fromInt(nResult);
			if (res != RequestLogInResponse.Result.SUCCESS) {
				Message m = Message.obtain(handler, JNI_REQUEST_LOG_IN,
						new RequestLogInResponse(null, res));
				handler.dispatchMessage(m);
			}
		}

		@Override
		public void OnUpdateBaseInfoCallback(long nUserID, String updatexml) {
			// TODO do not send result
			// Message.obtain(handler, JNI_REQUEST_UPDAE_USER,
			// updatexml).sendToTarget();
		}

		@Override
		public void OnUserStatusUpdatedCallback(long nUserID, int nType,int nStatus,
				String szStatusDesc) {

		}

		@Override
		public void OnChangeAvatarCallback(int nAvatarType, long nUserID,
				String AvatarName) {

		}

		@Override
		public void OnModifyCommentName(long nUserId, String sCommmentName) {

		}

	}
}
