package com.v2tech.service;

import android.os.Handler;
import android.os.Message;

import com.V2.jni.GroupRequest;
import com.V2.jni.ImRequest;
import com.V2.jni.ImRequestCallbackAdapter;
import com.v2tech.service.jni.CreateCrowdResponse;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.util.V2Log;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.Group;

/**
 * Crowd group service, used to create crowd and remove crowd
 * @author 28851274
 *
 */
public class CrowdGroupService extends AbstractHandler {

	private static final int CREATE_GROUP_MESSAGE = 0x0001;

	private ImRequestCB imCB;

	public CrowdGroupService() {
		imCB = new ImRequestCB(this);
		ImRequest.getInstance().addCallback(imCB);
	}

	/**
	 * Create crowd function, it's asynchronization request. response will be send by caller.
	 * @param group
	 * @param caller  if input is null, ignore response Message. Response Message
	 *            object is
	 *            {@link com.v2tech.service.jni.CreateCrowdResponse}
	 */
	public void createGroup(CrowdGroup group, Registrant caller) {
		this.initTimeoutMessage(CREATE_GROUP_MESSAGE, DEFAULT_TIME_OUT_SECS,
				caller);
		GroupRequest.getInstance().createGroup(
				Group.GroupType.CHATING.intValue(), group.toGroupXml(),
				group.toGroupUserListXml());

	}

	class ImRequestCB extends ImRequestCallbackAdapter {

		private Handler mCallbackHandler;

		public ImRequestCB(Handler mCallbackHandler) {
			this.mCallbackHandler = mCallbackHandler;
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
