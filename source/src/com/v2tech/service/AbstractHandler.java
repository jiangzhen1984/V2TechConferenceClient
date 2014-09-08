package com.v2tech.service;

import java.util.ArrayList;
import java.util.List;

import android.os.Handler;
import android.os.Message;
import android.util.SparseArray;

import com.V2.jni.util.V2Log;
import com.v2tech.service.jni.JNIResponse;

/**
 * Abstract handler.
 * <ul>
 * It used to handle message and handle time out message
 * </ul>
 * <ul>
 * Notice: If you want to override {@link #handleMessage(Message)}, please
 * handle time out message
 * </ul>
 * 
 * @author jiangzhen
 * 
 */
public abstract class AbstractHandler extends Handler {

	protected static final int REQUEST_TIME_OUT = 0;

	protected static final int MONITOR_TYPE_CONFERENCE = 0X01000000;

	protected static final int MONITOR_TYPE_DEVICE = 0X02000000;

	protected static final int MONITOR_TYPE_CONTACT = 0X03000000;

	protected static final int DEFAULT_TIME_OUT_SECS = 10;

	private SparseArray<Meta> metaHolder = new SparseArray<Meta>();

	private SparseArray<List<Registrant>> registrantHolder = new SparseArray<List<Registrant>>();

	private SparseArray<List<PendingObject>> pendingObjectHolder = new SparseArray<List<PendingObject>>();

	protected Message initTimeoutMessage(int mointorMessageID, long timeOutSec,
			Registrant caller) {
		// Create unique message object
		Message msg = Message.obtain(this, REQUEST_TIME_OUT, mointorMessageID,
				0, new Object());
		metaHolder.put(Integer.valueOf(mointorMessageID), new Meta(
				mointorMessageID, caller, msg));
		this.sendMessageDelayed(msg, timeOutSec * 1000);
		return msg;
	}

	protected Registrant removeTimeoutMessage(int mointorMessageID) {
		Meta meta = metaHolder.get(Integer.valueOf(mointorMessageID));
		metaHolder.remove(Integer.valueOf(mointorMessageID));
		if (meta != null) {
			this.removeMessages(REQUEST_TIME_OUT, meta.timeoutMessage.obj);
			return meta.caller;
		} else {
			return null;
		}
	}

	protected void sendResult(Registrant caller, Object obj) {
		if (caller != null) {
			Message result = Message.obtain();
			result.what = caller.getWhat();
			result.obj = obj;
			caller.getHandler().sendMessage(result);
		}
	}

	
	protected boolean checkParamNull(Registrant caller, Object... objs) {
		boolean flag = false;
		for (Object obj : objs) {
			if (obj == null) {
				flag = true;
				break;
			}
		}
		if (flag && caller != null) {
			sendResult(caller, 	new JNIResponse(
					JNIResponse.Result.INCORRECT_PAR));
			return false;
		}
		return true;
	}
	
	
	/**
	 * 
	 * @param key
	 * @param arg1
	 * @param arg2
	 * @param obj
	 */
	protected void notifyListener(int key, int arg1, int arg2, Object obj) {
		List<Registrant> list = registrantHolder.get(key);
		if (list == null || list.size() <= 0) {
			V2Log.i(this.getClass().getName() + "  : No listener: " + key
					+ " " + arg1 + "  " + arg2 + "  " + obj);
			return;
		} else {
			V2Log.i(this.getClass().getName() + "  : Notify listener: " + key
					+ " " + arg1 + "  " + arg2 + "  " + obj);
		}
		for (Registrant re : list) {
			Handler h = re.getHandler();
			if (h != null) {
				Message.obtain(h, re.getWhat(), arg1, arg2,
						new AsyncResult(re.getObject(), obj)).sendToTarget();
			}
		}
	}
	
	
	
	/**
	 * 
	 * @param key
	 * @param arg1
	 * @param arg2
	 * @param obj
	 */
	protected void notifyListenerWithPending(int key, int arg1, int arg2, Object obj) {
		List<Registrant> list = registrantHolder.get(key);
		if (list == null || list.size() <= 0) {
			List<PendingObject> pendingList = pendingObjectHolder.get(key);
			if (pendingList == null) {
				pendingList = new ArrayList<PendingObject>();
				pendingObjectHolder.put(key, pendingList);
			}
			pendingList.add(new PendingObject(key, arg1, arg2, obj));
			V2Log.i(this.getClass().getName() + "  : pend obj for " + key
					+ "  " + pendingList.size() + "   " + pendingObjectHolder);
			return;
		} else {
			V2Log.i(this.getClass().getName() + "  : Notify listener: " + key
					+ " " + arg1 + "  " + arg2 + "  " + obj);
			for (int i = 0; i < list.size(); i++) {
				Registrant re = list.get(i);
				Handler h = re.getHandler();
				if (h != null) {
					Message.obtain(h, re.getWhat(), arg1, arg2,
							new AsyncResult(re.getObject(), obj)).sendToTarget();
				}
			}
		}
		
	}
	

	protected void registerListener(int key, Handler h, int what, Object obj) {
		synchronized (pendingObjectHolder) {
			List<Registrant> list = registrantHolder.get(key);
			if (list == null) {
				list = new ArrayList<Registrant>();
				registrantHolder.append(key, list);
			}
			Registrant re = new Registrant(h, what, obj);
			list.add(re);

			List<PendingObject> pendingList = pendingObjectHolder.get(key);
			if (pendingList == null || pendingList.size() <= 0) {

				return;
			}

			for (int i = 0; i < pendingList.size(); i++) {
				PendingObject po = pendingList.get(i);
				Message.obtain(h, re.getWhat(), po.arg1, po.arg2,
						new AsyncResult(re.getObject(), po.obj)).sendToTarget();
			}
			
			pendingList.clear();
			pendingObjectHolder.remove(key);
		}
	}

	protected void unRegisterListener(int key, Handler h, int what, Object obj) {
		List<Registrant> list = registrantHolder.get(key);
		if (list != null) {
			for (int i = 0; i < list.size(); i++) {
				Registrant re = list.get(i);
				if (re.getHandler() == h && what == re.getWhat()) {
					list.remove(re);
					i--;
				}
			}
		}
	}
	
	/**
	 * Clear all callbacks from JNI interface
	 */
	public abstract void clear();

	class Meta {
		int mointorMessageID;
		Registrant caller;
		Message timeoutMessage;

		public Meta(int mointorMessageID, Registrant caller,
				Message timeoutMessage) {
			super();
			this.mointorMessageID = mointorMessageID;
			this.caller = caller;
			this.timeoutMessage = timeoutMessage;
		}

	}

	class PendingObject {
		int key;
		int arg1;
		int arg2;
		Object obj;

		public PendingObject(int key, int arg1, int arg2, Object obj) {
			super();
			this.key = key;
			this.arg1 = arg1;
			this.arg2 = arg2;
			this.obj = obj;
		}

	}

	@Override
	public void handleMessage(Message msg) {
		Message caller = null;
		V2Log.d(this.getClass().getName() + "   " + msg.what);
		switch (msg.what) {
		case REQUEST_TIME_OUT:
			Integer key = Integer.valueOf(msg.arg1);
			Meta meta = metaHolder.get(key);
			if (meta != null && meta.caller != null) {

				JNIResponse jniRes = new JNIResponse(
						JNIResponse.Result.TIME_OUT);

				jniRes.callerObject = meta.caller.getObject();
				if (meta.caller.getHandler() != null) {
					caller = Message.obtain(meta.caller.getHandler(),
							meta.caller.getWhat(), jniRes);
				} else {
					V2Log.w(" message no target:" + meta.caller);
				}
			} else {
				V2Log.w("Doesn't find time out message in the queue :"
						+ msg.arg1);
			}
			// remove cache
			metaHolder.remove(key);
			break;
		// Handle normal message
		default:
			Registrant resgister = removeTimeoutMessage(msg.what);
			if (resgister == null) {
				V2Log.w(this.getClass().getName()
						+ " Igore message client don't expect callback :"
						+ msg.what);
				return;
			}
			Object origObject = resgister.getObject();
			if (resgister.getHandler() != null) {
				caller = Message.obtain(resgister.getHandler(),
						resgister.getWhat());
				JNIResponse jniRes = (JNIResponse) msg.obj;
				jniRes.callerObject = origObject;
				caller.obj = jniRes;
			} else {
				V2Log.w("Doesn't find  message in the queue :" + msg.arg1);
			}
			break;
		}

		if (caller == null) {
			V2Log.w(" can not send message:" + msg.what
					+ " to target caller is null");
			return;
		} else {
			if (caller.getTarget() == null) {
				V2Log.w(" can not send message:" + msg.what
						+ " to target caller target(" + caller.what
						+ ") is null");
				return;
			}
			caller.sendToTarget();
		}
	}

}
