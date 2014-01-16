package com.v2tech.view;

import java.util.Hashtable;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import com.V2.jni.GroupRequestCallback;
import com.V2.jni.ImRequest;
import com.V2.jni.ImRequestCallback;
import com.v2tech.R;
import com.v2tech.logic.NetworkStateCode;
import com.v2tech.logic.User;
import com.v2tech.util.V2Log;

class MetaData {

	static long searil = 1;
	static Object lock = new Object();
	Long id;
	Message caller;

	private MetaData() {
		synchronized (lock) {
			id = searil++;
		}
	}

	static MetaData obtain(Message message) {
		MetaData m = new MetaData();
		m.caller = message;
		return m;
	}
}

/**
 * This service is used to wrap jni call.<br>
 * JNI calls are asynchronization, we don't expect activity involve JNI.
 * 
 * @author 28851274
 * 
 */
public class JNIService extends Service {

	private static Hashtable<Integer, MetaData> map = new Hashtable<Integer, MetaData>();

	private final LocalBinder mBinder = new LocalBinder();

	private Integer mBinderRef = 0;

	private LocalHander mHander;

	private JNICallbackHandler mCallbackHandler;

	private ImRequestCallback mImCB;

	private Context mContext;

	@Override
	public void onCreate() {
		super.onCreate();
		mContext = this;
		// start handler thread
		HandlerThread hd = new HandlerThread("queue");
		hd.start();
		mHander = new LocalHander(hd.getLooper());

		HandlerThread callback = new HandlerThread("callback");
		callback.start();
		mCallbackHandler = new JNICallbackHandler(callback.getLooper());

		mImCB = new ImRequestCB(mCallbackHandler);
		ImRequest.getInstance().setCallback(mImCB);
	}
	
	
	
	

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		synchronized (mBinderRef) {
			mBinderRef++;
		}
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		synchronized (mBinderRef) {
			mBinderRef--;
		}
		// if mBinderRef equals 0 means no activity
		if (mBinderRef == 0) {
			// TODO Do Logout
		}
		return super.onUnbind(intent);
	}

	/**
	 * Used to local binder
	 * 
	 * @author 28851274
	 * 
	 */
	class LocalBinder extends Binder {
		JNIService getService() {
			return JNIService.this;
		}
	}

	/**
	 * 
	 * @param msgId
	 * @param m
	 * @return
	 */
	private MetaData getAndQueued(Integer msgId, Message m) {
		if (map.containsKey(msgId)) {
			V2Log.e(" MSG ID:" + msgId + " in the queque!");
			return null;
		} else {
			MetaData meta = MetaData.obtain(m);
			map.put(msgId, meta);
			return meta;
		}
	}

	/**
	 * 
	 * @param msgId
	 * @return
	 */
	private MetaData getMeta(Integer msgId) {
		MetaData m = map.get(msgId);
		map.remove(msgId);
		return m;
	}

	/**
	 * Asynchronous login function.
	 * After login, will call message.sendToTarget to caller
	 * @param mail  user mail
	 * @param passwd  password
	 * @param message  callback message Message.obj is {@link User} object
	 */
	public void login(String mail, String passwd, Message message) {
		MetaData m = getAndQueued(JNI_LOG_IN, message);
		if (m != null) {
			Message.obtain(mHander, JNI_LOG_IN, new InnerUser(m, mail, passwd))
					.sendToTarget();
		} else {
			V2Log.e(" can't get metadata for login");
		}
	}

	class InnerUser {
		// call parameter
		MetaData m;
		String mail;
		String passwd;

		// call back parameter
		long idCallback;
		int status;
		int nResult;

		InnerUser(MetaData m, String mail, String passwd) {
			this.m = m;
			this.mail = mail;
			this.passwd = passwd;
		}

		InnerUser(MetaData m, long idCallback, int status, int nResult) {
			this.m = m;
			this.idCallback = idCallback;
			this.status = status;
			this.nResult = nResult;
		}
	}

	private static final int JNI_LOG_IN = 1;
	private static final int JNI_LOG_OUT = 2;
	private static final int JNI_CONNECT_RESPONSE = 3;
	private static final int JNI_GROUP_NOTIFY = 25;

	static class LocalHander extends Handler {

		public LocalHander(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case JNI_LOG_IN:
				InnerUser iu = (InnerUser) msg.obj;
				ImRequest.getInstance().login(iu.mail, iu.mail, 1, 2);
				break;
			case JNI_LOG_OUT:

				break;
			}
		}

	}

	class JNICallbackHandler extends Handler {

		public JNICallbackHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case JNI_LOG_IN:
				InnerUser iu = ((InnerUser) msg.obj);
				User u = new User(iu.idCallback, "",
						NetworkStateCode.fromInt(iu.nResult));
				if (iu.m == null || iu.m.caller == null) {
					V2Log.w("No available caller, ignore this call back");
					return;
				}
				iu.m.caller.obj = u;
				iu.m.caller.sendToTarget();
				break;
			case JNI_LOG_OUT:
				break;
			case JNI_CONNECT_RESPONSE:
				if (msg.arg1 == NetworkStateCode.CONNECTED_ERROR
						.intValue()) {
					Toast.makeText(mContext, R.string.error_connect_to_server,
							Toast.LENGTH_SHORT).show();
				}
				break;
			case JNI_GROUP_NOTIFY:
				break;
			}
		}

	}
	
	
	/////////////////////////////////////////////////
	// TODO Need to be optimize code structure    //
	////////////////////////////////////////////////

	class ImRequestCB implements ImRequestCallback {

		private JNICallbackHandler mCallbackHandler;

		public ImRequestCB(JNICallbackHandler mCallbackHandler) {
			this.mCallbackHandler = mCallbackHandler;
		}

		@Override
		public void OnLoginCallback(long nUserID, int nStatus, int nResult) {
			Message.obtain(
					mCallbackHandler,
					JNI_LOG_IN,
					new InnerUser(getMeta(JNI_LOG_IN), nUserID, nStatus,
							nResult)).sendToTarget();
		}

		@Override
		public void OnLogoutCallback(int nUserID) {

		}

		@Override
		public void OnConnectResponseCallback(int nResult) {
			Message.obtain(mCallbackHandler, JNI_CONNECT_RESPONSE, nResult, 0)
					.sendToTarget();
		}

	}

	class GroupRequestCB implements GroupRequestCallback {
		private JNICallbackHandler mCallbackHandler;

		public GroupRequestCB(JNICallbackHandler mCallbackHandler) {
			this.mCallbackHandler = mCallbackHandler;
		}

		@Override
		public void OnGetGroupInfoCallback(int groupType, String sXml) {
			Message.obtain(mCallbackHandler, JNI_GROUP_NOTIFY, sXml)
			.sendToTarget();
		}
		
	}

}
