package com.bizcom.request;

import android.os.Message;

import com.V2.jni.ImRequest;
import com.V2.jni.callbacAdapter.ImRequestCallbackAdapter;
import com.V2.jni.ind.V2ClientType;
import com.V2.jni.ind.BoUserInfoBase;
import com.V2.jni.util.EscapedcharactersProcessing;
import com.V2.jni.util.V2Log;
import com.bizcom.request.jni.JNIResponse;
import com.bizcom.request.jni.RequestLogInResponse;
import com.bizcom.request.jni.RequestUserUpdateResponse;
import com.bizcom.request.util.HandlerWrap;
import com.bizcom.request.util.V2AbstractHandler;
import com.bizcom.vc.application.GlobalConfig;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.application.V2GlobalEnum;
import com.bizcom.vo.User;

import java.text.SimpleDateFormat;
import java.util.Date;

public class V2ImRequest extends V2AbstractHandler {

	// 此处的消息类型只与AbstractHandler的REQUEST_TIME_OUT消息并列。
	// 与参数caller中的消息类型what完全没有关系，上层传什么消息what回调就是什么消息what
	private static final int JNI_REQUEST_LOG_IN = 1;
	private static final int JNI_REQUEST_UPDAE_USER = 2;

	private ImRequestCB imCB = null;

	public V2ImRequest() {
		super();
		imCB = new ImRequestCB();
		ImRequest.getInstance().addCallback(imCB);
	}

	public void login(String mail, String passwd, HandlerWrap caller) {
		initTimeoutMessage(JNI_REQUEST_LOG_IN, DEFAULT_TIME_OUT_SECS, caller);
		ImRequest.getInstance().proxy.login(mail, passwd,
				V2GlobalEnum.USER_STATUS_ONLINE, V2ClientType.ANDROID, false);
	}

	public void updateUserInfo(User user, HandlerWrap caller) {
		if (user == null) {
			if (caller != null && caller.getHandler() != null) {
				callerSendMessage(caller,
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
					EscapedcharactersProcessing.convert(user.getCommentName()));
		}
	}

	@Override
	public void clearCalledBack() {
		ImRequest.getInstance().removeCallback(imCB);
	}

	private class ImRequestCB extends ImRequestCallbackAdapter {

		// private Handler handler;

		// public ImRequestCB(Handler handler) {
		// this.handler = handler;
		// }

		@Override
		public void OnLoginCallback(long nUserID, int nStatus, int nResult,
				long serverTime, String sDBID) {
			// 获取系统时间
			GlobalConfig.recordLoginTime(serverTime);
			SimpleDateFormat fromat = new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss");
			String date = fromat.format(new Date(
					GlobalConfig.LONGIN_SERVER_TIME * 1000));
			V2Log.d("get server time ：" + date);
			RequestLogInResponse.Result res = RequestLogInResponse.Result
					.fromInt(nResult);
			Message m = Message.obtain(V2ImRequest.this, JNI_REQUEST_LOG_IN,
					new RequestLogInResponse(new User(nUserID), sDBID, res));
			dispatchMessage(m);
		}

		@Override
		public void OnConnectResponseCallback(int nResult) {
			RequestLogInResponse.Result res = RequestLogInResponse.Result
					.fromInt(nResult);
			if (res != RequestLogInResponse.Result.SUCCESS) {
				Message m = Message
						.obtain(V2ImRequest.this, JNI_REQUEST_LOG_IN,
								new RequestLogInResponse(null, res));
				dispatchMessage(m);
			}
		}

		@Override
		public void OnModifyCommentNameCallback(long nUserId,
				String sCommmentName) {
			User u = GlobalHolder.getInstance().getUser(nUserId);
			Message m = Message.obtain(V2ImRequest.this,
					JNI_REQUEST_UPDAE_USER, new RequestUserUpdateResponse(u,
							JNIResponse.Result.SUCCESS));
			dispatchMessage(m);
		}

		@Override
		public void OnUpdateBaseInfoCallback(BoUserInfoBase user) {
			if (user.mId != GlobalHolder.getInstance().getCurrentUserId()) {
				return;
			}
			Message m = Message.obtain(V2ImRequest.this,
					JNI_REQUEST_UPDAE_USER, new JNIResponse(
							JNIResponse.Result.SUCCESS));
			dispatchMessage(m);
		}
	}
}
