package com.v2tech.service;

import android.os.Handler;
import android.os.Message;

import com.V2.jni.GroupRequest;
import com.V2.jni.ImRequest;
import com.V2.jni.ImRequestCallback;
import com.v2tech.service.jni.CreateCrowdResponse;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.util.V2Log;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.Group;

public class CrowdGroupService extends AbstractHandler {

	private static final int CREATE_GROUP_MESSAGE = 0x0001;

	private ImRequestCB imCB;

	public CrowdGroupService() {
		imCB = new ImRequestCB(this);
		ImRequest.getInstance().addCallback(imCB);
	}

	/**
	 * 
	 * @param group
	 * @param caller
	 */
	public void createGroup(CrowdGroup group, Registrant caller) {
		this.initTimeoutMessage(CREATE_GROUP_MESSAGE, DEFAULT_TIME_OUT_SECS,
				caller);
		GroupRequest.getInstance().createGroup(
				Group.GroupType.CHATING.intValue(), group.toGroupXml(),
				group.toGroupUserListXml());
		// TODO add callback handle

	}

	class ImRequestCB implements ImRequestCallback {

		private Handler mCallbackHandler;

		public ImRequestCB(Handler mCallbackHandler) {
			this.mCallbackHandler = mCallbackHandler;
		}

		@Override
		public void OnLoginCallback(long nUserID, int nStatus, int nResult) {

		}

		@Override
		public void OnLogoutCallback(int nType) {

		}

		@Override
		public void OnConnectResponseCallback(int nResult) {

		}

		@Override
		public void OnUpdateBaseInfoCallback(long nUserID, String updatexml) {

		}

		@Override
		public void OnUserStatusUpdatedCallback(long nUserID, int nType,
				int nStatus, String szStatusDesc) {

		}

		@Override
		public void OnChangeAvatarCallback(int nAvatarType, long nUserID,
				String AvatarName) {

		}

		@Override
		public void OnModifyCommentNameCallback(long nUserId,
				String sCommmentName) {

		}

		@Override
		public void OnCreateCrowdCallback(String sCrowdXml, int nResult) {
			if (sCrowdXml == null || sCrowdXml.isEmpty()) {
				return;
			}
			int start = sCrowdXml.indexOf("id='");
			int end = sCrowdXml.indexOf("'", start + 4);
			long id = 0;
			if (start != -1 && end != -1) {
				id = Long.parseLong(sCrowdXml.substring(start + 4, end));
			} else {
				V2Log.e("unmalformed crow response " + sCrowdXml);
				return;
			}
			JNIResponse jniRes = new CreateCrowdResponse(id,
					CreateCrowdResponse.Result.SUCCESS);
			Message.obtain(mCallbackHandler, CREATE_GROUP_MESSAGE, jniRes)
					.sendToTarget();
		}

	}
}
