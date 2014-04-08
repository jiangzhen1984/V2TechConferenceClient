package com.v2tech.service;

import java.util.HashMap;
import java.util.Map;

import android.os.Handler;
import android.os.Message;

import com.v2tech.logic.AsynResult;
import com.v2tech.logic.jni.JNIResponse;
import com.v2tech.util.V2Log;

public abstract class AbstractHandler extends Handler {

	protected static final int REQUEST_TIME_OUT = 0;

	protected static final int MONITOR_TYPE_CONFERENCE = 0X01000000;

	protected static final int MONITOR_TYPE_DEVICE = 0X02000000;

	protected static final int MONITOR_TYPE_CONTACT = 0X03000000;

	protected static final int DEFAULT_TIME_OUT_SECS = 10;

	private Map<Integer, Meta> metaHolder = new HashMap<Integer, Meta>();

	protected Message initTimeoutMessage(int mointorMessageID, Object obj,
			long timeOutSec, Message caller) {
		Message msg = Message.obtain(this, REQUEST_TIME_OUT, mointorMessageID,
				0, obj);
		metaHolder.put(Integer.valueOf(mointorMessageID), new Meta(
				mointorMessageID, caller, msg));
		this.sendMessageDelayed(msg, timeOutSec * 1000);
		return msg;
	}

	protected Message removeTimeoutMessage(int mointorMessageID) {
		Meta meta = metaHolder.get(Integer.valueOf(mointorMessageID));
		metaHolder.remove(Integer.valueOf(mointorMessageID));
		if (meta != null) {
			this.removeMessages(REQUEST_TIME_OUT, meta.timeoutMessage);
			return meta.caller;
		} else {
			return null;
		}
	}

	class Meta {
		int mointorMessageID;
		Message caller;
		Message timeoutMessage;

		public Meta(int mointorMessageID, Message caller, Message timeoutMessage) {
			super();
			this.mointorMessageID = mointorMessageID;
			this.caller = caller;
			this.timeoutMessage = timeoutMessage;
		}

	}

	@Override
	public void handleMessage(Message msg) {
		Message caller = null;
		V2Log.d(this.getClass().getName()+"   "+ msg.what);
		switch (msg.what) {
		case REQUEST_TIME_OUT:
			Meta meta = metaHolder.get(Integer.valueOf(msg.arg1));
			if (meta != null && meta.caller != null) {
				Object origObject = meta.caller.obj;
				meta.caller.obj = new AsynResult(AsynResult.AsynState.TIME_OUT,
						null);
				if (msg.obj != null) {
					V2Log.d(msg.obj.getClass().getName()+"   "+ msg.what);
				}
				JNIResponse jniRes = (JNIResponse) msg.obj;
				if (jniRes == null) {
					jniRes = new JNIResponse(JNIResponse.Result.FAILED);
				}
				jniRes.callerObject = origObject;
				caller = meta.caller;
			} else {
				V2Log.w("Doesn't find time out message in the queue :" + msg.arg1);
			}
			break;
			//Handle normal message 
		default:
			caller = removeTimeoutMessage(msg.what);
			if (caller == null) {
				V2Log.w(this.getClass().getName()+ " Igore message client don't expect callback :"+msg.what);
				return;
			}
			Object origObject = caller.obj;
			caller.obj = new AsynResult(AsynResult.AsynState.SUCCESS, msg.obj);
			JNIResponse jniRes = (JNIResponse) msg.obj;
			jniRes.callerObject = origObject;
			break;
		}
		
		if (caller == null) {
			V2Log.w(" can not send message:" + msg.what +" to target caller is null");
			return;
		} else {
			if (caller.getTarget() == null) {
				V2Log.w(" can not send message:" + msg.what +" to target caller target("+caller.what+") is null");
				return;
			}
			caller.sendToTarget();
		}
	}

}
