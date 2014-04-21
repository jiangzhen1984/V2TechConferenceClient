package com.v2tech.service;

import android.os.Handler;
import android.os.Message;
import android.util.SparseArray;

import com.v2tech.logic.Registrant;
import com.v2tech.logic.jni.JNIResponse;
import com.v2tech.util.V2Log;

public abstract class AbstractHandler extends Handler {

	protected static final int REQUEST_TIME_OUT = 0;

	protected static final int MONITOR_TYPE_CONFERENCE = 0X01000000;

	protected static final int MONITOR_TYPE_DEVICE = 0X02000000;

	protected static final int MONITOR_TYPE_CONTACT = 0X03000000;

	protected static final int DEFAULT_TIME_OUT_SECS = 10;

	private SparseArray<Meta> metaHolder = new SparseArray<Meta>();

	protected Message initTimeoutMessage(int mointorMessageID,
			long timeOutSec, Registrant caller) {
		Message msg = Message.obtain(this, REQUEST_TIME_OUT, mointorMessageID, 0);
		metaHolder.put(Integer.valueOf(mointorMessageID), new Meta(
				mointorMessageID, caller, msg));
		this.sendMessageDelayed(msg, timeOutSec * 1000);
		return msg;
	}

	protected Registrant removeTimeoutMessage(int mointorMessageID) {
		Meta meta = metaHolder.get(Integer.valueOf(mointorMessageID));
		metaHolder.remove(Integer.valueOf(mointorMessageID));
		if (meta != null) {
			this.removeMessages(REQUEST_TIME_OUT, meta.timeoutMessage);
			return meta.caller;
		} else {
			return null;
		}
	}
	
	
	protected void sendResult(Registrant caller, Object obj) {
		Message result = Message.obtain();
		result.what = caller.getWhat();
		result.obj = obj; 
		caller.getHandler().sendMessage(result);
	}

	class Meta {
		int mointorMessageID;
		Registrant caller;
		Message timeoutMessage;

		public Meta(int mointorMessageID, Registrant caller, Message timeoutMessage) {
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
			if (meta != null && meta.caller != null ) {
				
				JNIResponse jniRes = new JNIResponse(JNIResponse.Result.TIME_OUT);
				
				jniRes.callerObject = meta.caller.getObject();
				if (meta.caller.getHandler() != null) {
					caller = Message.obtain(meta.caller.getHandler(), meta.caller.getWhat(), jniRes);
				} else {
					V2Log.w(" message no target:" + meta.caller);
				}
			} else {
				V2Log.w("Doesn't find time out message in the queue :" + msg.arg1);
			}
			break;
			//Handle normal message 
		default:
			Registrant resgister = removeTimeoutMessage(msg.what);
			if (resgister == null) {
				V2Log.w(this.getClass().getName()+ " Igore message client don't expect callback :"+msg.what);
				return;
			}
			Object origObject = resgister.getObject();
			if (resgister.getHandler() != null) {
				caller = Message.obtain(resgister.getHandler(), resgister.getWhat());
				JNIResponse jniRes = (JNIResponse) msg.obj;
				jniRes.callerObject = origObject;
				caller.obj =  jniRes;
			} else {
				V2Log.w("Doesn't find time out message in the queue :" + msg.arg1);
			}
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
