package com.v2tech.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

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

import com.V2.jni.ConfRequest;
import com.V2.jni.ConfRequestCallback;
import com.V2.jni.GroupRequest;
import com.V2.jni.GroupRequestCallback;
import com.V2.jni.ImRequest;
import com.V2.jni.ImRequestCallback;
import com.V2.jni.VideoRequest;
import com.V2.jni.VideoRequestCallback;
import com.v2tech.R;
import com.v2tech.logic.AsynResult;
import com.v2tech.logic.AsynResult.AsynState;
import com.v2tech.logic.Group;
import com.v2tech.logic.NetworkStateCode;
import com.v2tech.logic.User;
import com.v2tech.logic.UserDeviceConfig;
import com.v2tech.util.V2Log;

class MetaData {

	static long searil = 1;
	static Object lock = new Object();
	Long id;
	Message caller;
	Message timeOutMessage;
	Runnable timeoutCallback;

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
 * This service will hold all data which from server.
 * </p>
 * TODO add permission check to make sure don't let others stop this service.
 * 
 * @author 28851274
 * 
 */
public class JNIService extends Service {
	
	public static final String JNI_BROADCAST_CATEGROY = "com.v2tech.jni.broadcast";
	public static final String JNI_BROADCAST_GROUP_NOTIFICATION = "com.v2tech.jni.broadcast.group_geted";
	public static final String JNI_BROADCAST_ATTENDEE_ENTERED_NOTIFICATION = "com.v2tech.jni.broadcast.attendee.entered.notification";
	public static final String JNI_BROADCAST_ATTENDEE_EXITED_NOTIFICATION = "com.v2tech.jni.broadcast.attendee.exited.notification";

	private static Hashtable<Integer, MetaData> map = new Hashtable<Integer, MetaData>();

	private final LocalBinder mBinder = new LocalBinder();

	private Integer mBinderRef = 0;

	private LocalHander mHandler;

	private JNICallbackHandler mCallbackHandler;

	private ImRequestCallback mImCB;

	private ConfRequestCallback mCRCB;

	private GroupRequestCB mGRCB;

	private VideoRequestCB mVRCB;

	private Context mContext;

	// ///////////////////////////////////////
	// only refer group data which group is GroupType.CONFERENCE
	private List<Group> mConfGroup = null;

	private Map<Long, User> mUserHolder = new HashMap<Long, User>();
	
	private List<UserDeviceConfig> mUserDeviceList = new ArrayList<UserDeviceConfig>();
	
	private long mloggedInUserId;
	
	private int mLoadGroupOwnerCount = 0;

	// ////////////////////////////////////////

	@Override
	public void onCreate() {
		super.onCreate();
		mContext = this;
		// start handler thread
		HandlerThread hd = new HandlerThread("queue");
		hd.start();
		mHandler = new LocalHander(hd.getLooper());

		HandlerThread callback = new HandlerThread("callback");
		callback.start();
		mCallbackHandler = new JNICallbackHandler(callback.getLooper());

		mImCB = new ImRequestCB(mCallbackHandler);
		ImRequest.getInstance().setCallback(mImCB);

		mCRCB = new ConfRequestCB(mCallbackHandler);
		ConfRequest.getInstance().setCallback(mCRCB);

		mGRCB = new GroupRequestCB(mCallbackHandler);
		GroupRequest.getInstance().setCallback(mGRCB);

		mVRCB = new VideoRequestCB(mCallbackHandler);
		VideoRequest.getInstance().setCallback(mVRCB);
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
	private MetaData getAndQueued(Integer msgId, Message caller) {
		Integer key = Integer.valueOf(msgId);
		synchronized (map) {
			if (map.containsKey(key)) {
				V2Log.e(" MSG ID:" + msgId + " in the queque!");
				return null;
			} else {
				MetaData meta = MetaData.obtain(caller);
				map.put(key, meta);
				return meta;
			}
		}
	}

	/**
	 * 
	 * @param msgId
	 * @param m
	 * @return
	 */
	private MetaData getAndQueued(Integer msgId, Message timeoutMsg,
			Message caller) {
		synchronized (map) {
			if (map.containsKey(msgId)) {
				V2Log.e(" MSG ID:" + msgId + " in the queque!");
				return null;
			} else {
				MetaData meta = MetaData.obtain(caller);
				meta.timeOutMessage = timeoutMsg;
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
		// To regenerate key for make sure hash code is same with integer value.
		// otherwise maybe we query incorrect answer
		Integer key = Integer.valueOf(msgId);
		synchronized (map) {
			MetaData m = map.get(key);
			map.remove(key);
			return m;
		}
	}

	/**
	 * 
	 * @param msgId
	 * @return
	 */
	private MetaData getMetaWithoutRemvoe(Integer msgId) {
		// To regenerate key for make sure hash code is same with integer value.
		// otherwise maybe we query incorrect answer
		Integer key = Integer.valueOf(msgId);
		synchronized (map) {
			MetaData m = map.get(key);
			return m;
		}
	}

	private synchronized boolean existMessage(Integer msgId) {
		// To regenerate key for make sure hash code is same with integer value.
		// otherwise maybe we query incorrect answer
		Integer key = Integer.valueOf(msgId);
		return map.containsKey(key);
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
			Message.obtain(mHandler, JNI_LOG_IN, new InnerUser(m, mail, passwd))
					.sendToTarget();
		} else {
			V2Log.e(" can't get metadata for login");
		}
	}
	
	
	public long getLoggedUserId() {
		return this.mloggedInUserId;
	}
	
	public User getloggedUser() {
		return this.mUserHolder.get(Long.valueOf(this.mloggedInUserId));
	}


	/**
	 * get user object according to user id.
	 * 
	 * @param nUserID
	 * @return
	 */
	public User getUserBaseInfo(long nUserID) {
		return this.mUserHolder.get(nUserID);
	}

	/**
	 * Group information is server active call, we can't get from server
	 * directly.<br>
	 * Only way to get group information is waiting for server call.<br>
	 * So if this function return null, means service doesn't receive any call
	 * from server. otherwise server already sent group information to service.<br>
	 * If you want to know indication, please use {@link getGroupAsyn}
	 * 
	 * @param gType
	 * @return return null means server doesn't send group information to
	 *         service. AsynResult.object {@link User}
	 */
	public List<Group> getGroup(Group.GroupType gType) {
		if (mLoadGroupOwnerCount > 0) {
			return null;
		}
		if (gType == Group.GroupType.CONFERENCE) {
			return this.mConfGroup;
		} else {
			throw new RuntimeException(" Unknown group type :" + gType);
		}
	}

	/**
	 * User request to enter conference.<br>
	 * 
	 * @param confID
	 * @param msg
	 *            if input is null, ignore response Message.object is
	 *            {@link AsynResult} AsynResult.obj is Integer 0: success 1:
	 *            failed
	 */
	public void requestEnterConference(long confID, Message msg) {
		MetaData m = getAndQueued(JNI_REQUEST_ENTER_CONF, msg);
		if (m != null) {
			Message.obtain(mHandler, JNI_REQUEST_ENTER_CONF,
					Long.valueOf(confID)).sendToTarget();
		} else {
			if (msg != null) {
				V2Log.e(" Enter conf request already in queue");
				msg.obj = new AsynResult(AsynState.FAIL, new Exception(
						"Request already in the queue"));
				msg.sendToTarget();
			}
		}
	}

	/**
	 * User request exit conference
	 * 
	 * @param confID
	 * @param msg
	 *            if input is null, ignore response Message.object is
	 *            {@link AsynResult} AsynResult.obj is Object[] Object[0]
	 *            Integer 0: success 1: failed Object[1] Long conf id
	 */
	public void requestExitConference(long confID, Message msg) {
		MetaData m = getAndQueued(JNI_REQUEST_EXIT_CONF, msg);
		if (m != null) {
			Message.obtain(mHandler, JNI_REQUEST_EXIT_CONF,
					Long.valueOf(confID)).sendToTarget();
		} else {
			if (msg != null) {
				V2Log.e(" exit conf request already in queue");
				msg.obj = new AsynResult(AsynState.FAIL, new Exception(
						"Request already in the queue"));
				msg.sendToTarget();
			}
		}
	}

	
	/**
	 * Request open video device.
	 * @param nGroupID
	 * @param userDevice 
	 * @param caller
	 */
	public void requestOpenVideoDevice(long nGroupID,
			UserDeviceConfig userDevice, Message caller) {
		if (nGroupID < 0 || userDevice == null || userDevice.getUserID() <= 0) {
			V2Log.e("Invalid device parameters");
			if (caller != null) {
				caller.obj = new AsynResult(AsynState.FAIL, new Exception(
						"Invalid device parameters"));
				caller.sendToTarget();
				return;
			}
		}
		//put to queue
		getAndQueued(JNI_REQUEST_OPEN_VIDEO, caller);
		Message.obtain(mHandler, JNI_REQUEST_OPEN_VIDEO,
				new OpenVideoRequest(nGroupID, userDevice)).sendToTarget();
	}
	
	/**
	 * Request close video device.
	 * @param nGroupID
	 * @param userDevice 
	 * @param caller
	 */
	public void requestCloseVideoDevice(long nGroupID,
			UserDeviceConfig userDevice, Message caller) {
		if (nGroupID < 0 || userDevice == null || userDevice.getUserID() <= 0) {
			V2Log.e("Invalid device parameters");
			if (caller != null) {
				caller.obj = new AsynResult(AsynState.FAIL, new Exception(
						"Invalid device parameters"));
				caller.sendToTarget();
				return;
			}
		}
		//put to queue
		getAndQueued(JNI_REQUEST_CLOSE_VIDEO, caller);
		Message.obtain(mHandler, JNI_REQUEST_CLOSE_VIDEO,
				new OpenVideoRequest(nGroupID, userDevice)).sendToTarget();
	}
	
	
	
	public void applyForControlPermission(int type) {
		Message.obtain(mHandler, JNI_REQUEST_SPEAK, type, 0).sendToTarget();
	}
	
	public void applyForReleasePermission(int type) {
		Message.obtain(mHandler, JNI_REQUEST_RELEASE_SPEAK, type, 0).sendToTarget();
	}
	
	
	/**
	 * 
	 * @param uid
	 * @return
	 */
	public List<UserDeviceConfig> getAttendeeDevice(long uid) {
		List<UserDeviceConfig> l = new ArrayList<UserDeviceConfig>();
		for(UserDeviceConfig udl : mUserDeviceList) {
			if(udl.getUserID() == uid) {
				l.add(udl);
			}
		}
		return l;
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
	
	
	

	class OpenVideoRequest {
		long group;
		UserDeviceConfig userDevice;

		public OpenVideoRequest(long group, UserDeviceConfig userDevice) {
			super();
			this.group = group;
			this.userDevice = userDevice;
		}

	}

	class RequestEnterConfResponse {
		long nConfID;
		long nTime;
		String szConfData;
		int nJoinResult;

		public RequestEnterConfResponse(long nConfID, long nTime,
				String szConfData, int nJoinResult) {
			super();
			this.nConfID = nConfID;
			this.nTime = nTime;
			this.szConfData = szConfData;
			this.nJoinResult = nJoinResult;
		}

	}

	// //////////////////////////////////////////////////////////
	// Internal Handler definition //
	// //////////////////////////////////////////////////////////

	private static final int CANCEL_REQUEST = 0x1000;
	private static final int REQUEST_TIME_OUT = 0x2000;
	private static final int JNI_LOG_IN = 21;
	private static final int JNI_LOG_OUT = 22;
	private static final int JNI_CONNECT_RESPONSE = 23;
	private static final int JNI_UPDATE_USER_INFO = 24;
	private static final int JNI_GROUP_NOTIFY = 35;
	private static final int JNI_LOAD_GROUP_OWNER_INFO = 36;
	private static final int JNI_REQUEST_ENTER_CONF = 55;
	private static final int JNI_REQUEST_EXIT_CONF = 56;
	private static final int JNI_ATTENDEE_ENTERED_NOTIFICATION = 57;
	private static final int JNI_ATTENDEE_EXITED_NOTIFICATION = 58;
	private static final int JNI_GET_ATTENDEE_INFO_DONE = 59;
	private static final int JNI_REQUEST_OPEN_VIDEO = 70;
	private static final int JNI_REQUEST_CLOSE_VIDEO = 71;
	private static final int JNI_REQUEST_SPEAK = 72;
	private static final int JNI_REQUEST_RELEASE_SPEAK = 73;
	private static final int JNI_REMOTE_USER_DEVICE_INFO_NOTIFICATION = 80;
	

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
				// Message m = Message.obtain(mCallbackHandler,
				// REQUEST_TIME_OUT,
				// JNI_LOG_IN, 0);
				// iu.m.timeOutMessage = m;
				// mCallbackHandler.sendMessageDelayed(m, 10000);

				iu.m.timeoutCallback = new TimeoutCallback(JNI_LOG_IN, 0, null);
				mCallbackHandler.postDelayed(iu.m.timeoutCallback, 10000);
				break;

			case JNI_LOG_OUT:
				break;
			case JNI_UPDATE_USER_INFO:
				ImRequest.getInstance().getUserBaseInfo((Long) msg.obj);
				break;
			case JNI_GROUP_NOTIFY:
				break;
			case JNI_LOAD_GROUP_OWNER_INFO:
				break;
			case JNI_REQUEST_ENTER_CONF:
				ConfRequest.getInstance().enterConf((Long) msg.obj);
				break;
			case JNI_REQUEST_EXIT_CONF:
				ConfRequest.getInstance().exitConf((Long) msg.obj);
				// As now exit conference request no call back.
				// So send successful message to caller
				MetaData exitD = getMeta(JNI_REQUEST_EXIT_CONF);
				if (exitD != null && exitD.caller != null) {
					exitD.caller.obj = new AsynResult(AsynState.SUCCESS, null);
					exitD.caller.sendToTarget();
				}
				break;
			case JNI_REQUEST_OPEN_VIDEO:
				OpenVideoRequest requestObj = (OpenVideoRequest) msg.obj;
				VideoRequest.getInstance().openVideoDevice(requestObj.group,
						requestObj.userDevice.getUserID(),
						requestObj.userDevice.getDeviceID(),
						requestObj.userDevice.getVp(),
						requestObj.userDevice.getBusinessType());
				// As now open video device no call back.
				// So send successful message to caller
				MetaData d = getMeta(JNI_REQUEST_OPEN_VIDEO);
				if (d != null && d.caller != null) {
					d.caller.obj = new AsynResult(AsynState.SUCCESS, null);
					d.caller.sendToTarget();
				}
				break;
			case JNI_REQUEST_CLOSE_VIDEO:
				OpenVideoRequest requestCloseObj = (OpenVideoRequest) msg.obj;
				VideoRequest.getInstance().closeVideoDevice(requestCloseObj.group,
						requestCloseObj.userDevice.getUserID(),
						requestCloseObj.userDevice.getDeviceID(),
						requestCloseObj.userDevice.getVp(),
						requestCloseObj.userDevice.getBusinessType());
				// As now open video device no call back.
				// So send successful message to caller
				MetaData response = getMeta(JNI_REQUEST_CLOSE_VIDEO);
				if (response != null && response.caller != null) {
					response.caller.obj = new AsynResult(AsynState.SUCCESS, null);
					response.caller.sendToTarget();
				}
				break;
			case JNI_REQUEST_SPEAK:
				// 3 means apply speak
				ConfRequest.getInstance().applyForControlPermission(msg.arg1);
				break;
			case JNI_REQUEST_RELEASE_SPEAK:
				ConfRequest.getInstance().releaseControlPermission(msg.arg1);
				break;
			}
		}

	}

	class JNICallbackHandler extends Handler {

		public JNICallbackHandler(Looper looper) {
			super(looper);
		}

		@Override
		public synchronized void handleMessage(Message msg) {
			MetaData d = getMeta(msg.what);
			AsynResult ar = null;
			int arg1 = 0;
			int arg2 = 0;
			switch (msg.what) {
			case REQUEST_TIME_OUT:

				MetaData origin = getMeta(msg.arg1);
				if (origin == null) {
					V2Log.w(msg.arg1 + " REQUEST_TIME_OUT : empty ");
				} else {
					origin.caller.obj = new AsynResult(
							AsynResult.AsynState.TIME_OUT, new Exception(
									msg.arg1 + " time out"));
					origin.caller.sendToTarget();
				}
				break;

			case CANCEL_REQUEST:
				break;
			case JNI_LOG_IN:
				InnerUser iu = ((InnerUser) msg.obj);
				ar = new AsynResult(AsynResult.AsynState.SUCCESS,
						new User(iu.idCallback, "",
								NetworkStateCode.fromInt(iu.nResult)));
				mloggedInUserId = iu.idCallback;
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
						m.caller.obj = new AsynResult(
								AsynResult.AsynState.SUCCESS, new User(0, "",
										NetworkStateCode.CONNECTED_ERROR));
						m.caller.sendToTarget();
					}
					// after login or before log in send broadcast
					Toast.makeText(mContext, R.string.error_connect_to_server,
							Toast.LENGTH_SHORT).show();

				}
				break;
			case JNI_UPDATE_USER_INFO:
				User u = User.fromXml(msg.arg1, (String) msg.obj);
				mUserHolder.put(Long.valueOf(u.getmUserId()), u);
				// If someone waiting for this event
				ar = new AsynResult(AsynResult.AsynState.SUCCESS, u);
				break;
			case JNI_GROUP_NOTIFY:
				if (msg.arg1 == Group.GroupType.CONFERENCE.intValue()) {
					mConfGroup = Group
							.parserFromXml(msg.arg1, (String) msg.obj);
					mLoadGroupOwnerCount = mConfGroup.size();
					for (Group g : mConfGroup) {
						// put caller message to queue
						getAndQueued(JNI_UPDATE_USER_INFO,
								Message.obtain(this, JNI_LOAD_GROUP_OWNER_INFO));
						Message.obtain(mHandler, JNI_UPDATE_USER_INFO, g.getOwner())
								.sendToTarget();
					}
				}

				ar = new AsynResult(AsynResult.AsynState.SUCCESS, mConfGroup);

				break;
			case JNI_LOAD_GROUP_OWNER_INFO:
				mLoadGroupOwnerCount--;
				User gu = (User)((AsynResult) msg.obj).getObject();
				for (Group g : mConfGroup) {
					if (g.getOwner() == gu.getmUserId()) {
						g.setOwnerUser(gu);
					}
				}
				if (mLoadGroupOwnerCount == 0) {
					Intent ei = new Intent(JNI_BROADCAST_GROUP_NOTIFICATION);
					ei.addCategory(JNI_BROADCAST_CATEGROY);
					ei.putExtra("gtype", Group.GroupType.CONFERENCE.intValue());
					mContext.sendBroadcast(ei);
				}
				break;
			case JNI_REQUEST_ENTER_CONF:
				RequestEnterConfResponse recr = (RequestEnterConfResponse) msg.obj;
				ar = new AsynResult(AsynResult.AsynState.SUCCESS, new Object[] {
						recr.nJoinResult, recr.nConfID });
				break;
			case JNI_ATTENDEE_ENTERED_NOTIFICATION:
				Long uid = Long.valueOf(Long.parseLong(msg.obj.toString()));
				User attendeeUser = mUserHolder.get(uid);
				// check cache, if exist doesn't query again, send successful
				// message directly.
				if (attendeeUser != null && attendeeUser.getName() != null
						&& !attendeeUser.getName().equals("")) {
					Message.obtain(
							this,
							JNI_GET_ATTENDEE_INFO_DONE,
							new AsynResult(AsynResult.AsynState.SUCCESS,
									attendeeUser)).sendToTarget();
				} else {
					// put caller message to queue
					getAndQueued(JNI_UPDATE_USER_INFO,
							Message.obtain(this, JNI_GET_ATTENDEE_INFO_DONE));
					Message.obtain(mHandler, JNI_UPDATE_USER_INFO, uid)
							.sendToTarget();
				}
				break;
			case JNI_ATTENDEE_EXITED_NOTIFICATION:
				Intent ei = new Intent(JNI_BROADCAST_ATTENDEE_EXITED_NOTIFICATION);
				ei.addCategory(JNI_BROADCAST_CATEGROY);
				ei.putExtra("uid", (Long)msg.obj);
				ei.putExtra("name", mUserHolder.get(Long.valueOf((Long)msg.obj)).getName());
				mContext.sendBroadcast(ei);
				break;
			case JNI_GET_ATTENDEE_INFO_DONE:
				User attendee = (User)((AsynResult) msg.obj).getObject();
				Intent i = new Intent(JNI_BROADCAST_ATTENDEE_ENTERED_NOTIFICATION);
				i.addCategory(JNI_BROADCAST_CATEGROY);
				i.putExtra("uid", attendee.getmUserId());
				i.putExtra("name", attendee.getName());
				mContext.sendBroadcast(i);
				break;
			case JNI_REMOTE_USER_DEVICE_INFO_NOTIFICATION:
				mUserDeviceList.addAll(UserDeviceConfig.parseFromXml((String)msg.obj));
				break;
			}

			if (d != null && d.caller != null) {
				d.caller.obj = ar;
				d.caller.arg1 = arg1;
				d.caller.arg2 = arg2;
				d.caller.sendToTarget();
				if (d.timeoutCallback != null) {
					// FIXME need to remove when finish requesting normally
					// but this solution doesn't work
					this.removeMessages(REQUEST_TIME_OUT, d.timeoutCallback);
				}
			} else {
				V2Log.w("MSG: " + msg.what
						+ " Metadata object or call is null ");
			}
		}
	}

	class TimeoutCallback implements Runnable {
		int arg1;
		int arg2;
		Object obj;

		TimeoutCallback(int arg1, int arg2, Object obj) {
			this.arg1 = arg1;
			this.arg2 = arg2;
			this.obj = obj;
		}

		@Override
		public void run() {
			Message.obtain(mCallbackHandler, REQUEST_TIME_OUT, arg1, arg2)
					.sendToTarget();
		}

	}

	// ///////////////////////////////////////////////
	// JNI call back implements //
	// TODO Need to be optimize code structure //
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
					new InnerUser(getMetaWithoutRemvoe(JNI_LOG_IN), nUserID,
							nStatus, nResult)).sendToTarget();
		}

		@Override
		public void OnLogoutCallback(int nUserID) {

		}

		@Override
		public void OnConnectResponseCallback(int nResult) {
			Message.obtain(mCallbackHandler, JNI_CONNECT_RESPONSE, nResult, 0)
					.sendToTarget();
		}

		@Override
		public void OnUpdateBaseInfoCallback(long nUserID, String updatexml) {
			Message.obtain(mCallbackHandler, JNI_UPDATE_USER_INFO,
					(int) nUserID, 0, updatexml).sendToTarget();
		}

	}

	class GroupRequestCB implements GroupRequestCallback {
		private JNICallbackHandler mCallbackHandler;

		public GroupRequestCB(JNICallbackHandler mCallbackHandler) {
			this.mCallbackHandler = mCallbackHandler;
		}

		@Override
		public void OnGetGroupInfoCallback(int groupType, String sXml) {
			Message.obtain(mCallbackHandler, JNI_GROUP_NOTIFY, groupType, 0,
					sXml).sendToTarget();
		}

	}

	class ConfRequestCB implements ConfRequestCallback {

		private JNICallbackHandler mCallbackHandler;

		public ConfRequestCB(JNICallbackHandler mCallbackHandler) {
			this.mCallbackHandler = mCallbackHandler;
		}

		@Override
		public void OnEnterConfCallback(long nConfID, long nTime,
				String szConfData, int nJoinResult) {
			Message.obtain(
					mCallbackHandler,
					JNI_REQUEST_ENTER_CONF,
					new RequestEnterConfResponse(nConfID, nTime, szConfData,
							nJoinResult)).sendToTarget();
		}

		@Override
		public void OnConfMemberEnterCallback(long nConfID, long nTime,
				String szUserInfos) {
			int start = szUserInfos.indexOf("id='");
			if (start != -1) {
				int end = szUserInfos.indexOf("'", start + 4);
				if (end != -1) {
					String id = szUserInfos.substring(start + 4, end);
					Message.obtain(mCallbackHandler,
							JNI_ATTENDEE_ENTERED_NOTIFICATION, id)
							.sendToTarget();
				} else {
					V2Log.e("Invalid attendee user id ignore callback message");
				}
			} else {
				V2Log.e("Invalid attendee user id ignore callback message");
			}
		}

		@Override
		public void OnConfMemberExitCallback(long nConfID, long nTime,
				long nUserID) {
			Message.obtain(mCallbackHandler, JNI_ATTENDEE_EXITED_NOTIFICATION, 0, 0, nUserID).sendToTarget();
		}
		
		
		

	}

	class VideoRequestCB implements VideoRequestCallback {

		private JNICallbackHandler mCallbackHandler;

		public VideoRequestCB(JNICallbackHandler mCallbackHandler) {
			this.mCallbackHandler = mCallbackHandler;
		}

		@Override
		public void OnRemoteUserVideoDevice(String szXmlData) {
			if (szXmlData == null) {
				V2Log.e(" No avaiable user device configuration");
				return;
			}
			Message.obtain(mCallbackHandler, JNI_REMOTE_USER_DEVICE_INFO_NOTIFICATION, szXmlData).sendToTarget();
		}

	}

}
