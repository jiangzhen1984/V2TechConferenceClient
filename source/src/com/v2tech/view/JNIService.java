package com.v2tech.view;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

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
import com.v2tech.logic.AsynResult;
import com.v2tech.logic.AsynResult.AsynState;
import com.v2tech.logic.Group;
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
 * This service is used to wrap JNI call.<br>
 * JNI calls are asynchronous, we don't expect activity involve JNI.<br>
 * <p>
 *     This service will hold all data which from server.
 * </p>
 * TODO add permission check to make sure do let others stop this service.
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
	
	
	
	/////////////////////////////////////////
	// only refer group data which group is GroupType.CONFERENCE
	private List<Group> mConfGroup = null;
	
	
	//////////////////////////////////////////

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
		synchronized (map) {
			if (map.containsKey(msgId)) {
				V2Log.e(" MSG ID:" + msgId + " in the queque!");
				return null;
			} else {
				MetaData meta = MetaData.obtain(m);
				map.put(msgId, meta);
				return meta;
			}
		}
	}

	/**
	 * 
	 * @param msgId
	 * @return
	 */
	private MetaData getMeta(Integer msgId) {
		synchronized (map) {
			MetaData m = map.get(msgId);
			map.remove(msgId);
			return m;
		}
	}

	private synchronized boolean existMessage(Integer msgId) {
		return map.containsKey(msgId);
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
	 *            callback message Message.obj is {@link AsynResult} object
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
	
	
	/**
	 *  Get group information. Service will send message to target, once service get group from JNI.
	 * @param gType
	 * @param msg 
	 *  	callback message Message.obj is {@link AsynResult} object. AsynResult.object is List<Group>
	 */
	public void getGroupAsyn(Group.GroupType gType, Message msg) {
		if (gType == Group.GroupType.CONFERENCE && this.mConfGroup != null) {
			msg.obj = new AsynResult(AsynState.SUCCESS, this.mConfGroup);
		}
	}
	
	
	/**
	 * Group information is server active call, we can't get from server directly.<br>
	 * Only way to get group information is waiting for server call.<br>
	 * So if this function return null, means service doesn't receive any call from server.
	 * otherwise server already sent group information to service.<br>
	 * If you want to know indication, please use {@link getGroupAsyn}
	 * @param gType
	 * @return return null means server doesn't send group information to service.  AsynResult.object {@link User}
	 */
	public List<Group> getGroup(Group.GroupType gType) {
		if (gType == Group.GroupType.CONFERENCE) {
			return this.mConfGroup;
		}
		throw new RuntimeException(" Unknown group type :"+gType);
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
	
	
	
	
	////////////////////////////////////////////////////////////
	// Internal Handler definition                            //
	////////////////////////////////////////////////////////////

	private static final int CANCEL_REQUEST = 0x1000;
	private static final int REQUEST_TIME_OUT = 0x2000;
	private static final int JNI_LOG_IN = 21;
	private static final int JNI_LOG_OUT = 22;
	private static final int JNI_CONNECT_RESPONSE = 23;
	private static final int JNI_GROUP_NOTIFY = 35;

	class LocalHander extends Handler {

		public LocalHander(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case JNI_LOG_IN:
				InnerUser iu = (InnerUser) msg.obj;
				ImRequest.getInstance().login(iu.mail, iu.passwd, 1, 2);
				Message m = Message.obtain(mCallbackHandler, REQUEST_TIME_OUT,
						JNI_LOG_IN, 0);
				mCallbackHandler.sendMessageDelayed(m, 10000);
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
			case REQUEST_TIME_OUT: {
				// TODO need to be remove when request finish normally
				int msgId = msg.arg1;
				MetaData d = getMeta(msgId);
				if (d == null) {
					V2Log.w(" REQUEST_TIME_OUT : empty ");
				} else {
					d.caller.obj = new AsynResult(
							AsynResult.AsynState.TIME_OUT, new Exception(msgId
									+ " time out"));
					d.caller.sendToTarget();
				}
			}
				break;

			case CANCEL_REQUEST: {
				// cancel request
				int msgId = msg.arg1;
				MetaData d = getMeta(msgId);
				if (d != null) {
					V2Log.i(" MSG :" + msgId + " removed");
				}
			}
				break;
			case JNI_LOG_IN: {
				InnerUser iu = ((InnerUser) msg.obj);
				User u = new User(iu.idCallback, "",
						NetworkStateCode.fromInt(iu.nResult));
				if (iu.m == null || iu.m.caller == null) {
					V2Log.w("No available caller, ignore this call back");
					break;
				}
				iu.m.caller.obj = new AsynResult(AsynResult.AsynState.SUCCESS,
						u);
				iu.m.caller.sendToTarget();
			}
				break;
			case JNI_LOG_OUT:
				break;
			case JNI_CONNECT_RESPONSE:
				// Can't not connect to server
				if (msg.arg1 == NetworkStateCode.CONNECTED_ERROR.intValue()) {
					// Logging event in progress, need to send error message to
					// Login caller
					if (existMessage(JNI_LOG_IN)) {
						MetaData m = getMeta(JNI_LOG_IN);
						User u = new User(0, "",
								NetworkStateCode.CONNECTED_ERROR);
						m.caller.obj = u;
						m.caller.sendToTarget();
					}
					// after login or before log in send broadcast
					Toast.makeText(mContext, R.string.error_connect_to_server,
							Toast.LENGTH_SHORT).show();

				}
				break;
			case JNI_GROUP_NOTIFY:
				break;
			}
		}
	}

	
	
	// ///////////////////////////////////////////////
	//  JNI call back implements                    //
	// TODO Need to be optimize code structure      //
	// ///////////////////////////////////////////////

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
			Message.obtain(mCallbackHandler, JNI_GROUP_NOTIFY, groupType, 0, sXml)
					.sendToTarget();
		}

	}

}
