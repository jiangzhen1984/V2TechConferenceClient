package com.v2tech.view;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import com.V2.jni.ChatRequest;
import com.V2.jni.ChatRequestCallback;
import com.V2.jni.ConfRequest;
import com.V2.jni.ConfRequestCallback;
import com.V2.jni.GroupRequest;
import com.V2.jni.GroupRequestCallback;
import com.V2.jni.ImRequest;
import com.V2.jni.ImRequestCallback;
import com.V2.jni.VideoRequest;
import com.V2.jni.VideoRequestCallback;
import com.v2tech.R;
import com.v2tech.db.ContentDescriptor;
import com.v2tech.logic.AsynResult;
import com.v2tech.logic.AsynResult.AsynState;
import com.v2tech.logic.CameraConfiguration;
import com.v2tech.logic.ConferencePermission;
import com.v2tech.logic.GlobalHolder;
import com.v2tech.logic.Group;
import com.v2tech.logic.NetworkStateCode;
import com.v2tech.logic.User;
import com.v2tech.logic.UserDeviceConfig;
import com.v2tech.logic.VImageMessage;
import com.v2tech.logic.VMessage;
import com.v2tech.logic.VMessage.MessageType;
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
	public static final String JNI_BROADCAST_USER_STATUS_NOTIFICATION = "com.v2tech.jni.broadcast.user_stauts_notification";
	public static final String JNI_BROADCAST_GROUP_NOTIFICATION = "com.v2tech.jni.broadcast.group_geted";
	public static final String JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION = "com.v2tech.jni.broadcast.group_user_updated";
	public static final String JNI_BROADCAST_ATTENDEE_ENTERED_NOTIFICATION = "com.v2tech.jni.broadcast.attendee.entered.notification";
	public static final String JNI_BROADCAST_ATTENDEE_EXITED_NOTIFICATION = "com.v2tech.jni.broadcast.attendee.exited.notification";
	public static final String JNI_BROADCAST_NEW_MESSAGE = "com.v2tech.jni.broadcast.new.message";
	
	private boolean isDebug = true;

	private static Hashtable<Integer, MetaData> map = new Hashtable<Integer, MetaData>();

	private final LocalBinder mBinder = new LocalBinder();

	private Integer mBinderRef = 0;

	private LocalHander mHandler;

	private JNICallbackHandler mCallbackHandler;

	private Handler thread;

	// ////////////////////////////////////////
	// JNI call back definitions
	private ImRequestCallback mImCB;

	private ConfRequestCallback mCRCB;

	private GroupRequestCB mGRCB;

	private VideoRequestCB mVRCB;
	
	private ChatRequestCB mChRCB;

	// ////////////////////////////////////////

	private Context mContext;

	// ///////////////////////////////////////
	// only refer group data which group is GroupType.CONFERENCE
	private List<Group> mConfGroup = null;

	private List<Group> mContactsGroup = null;

	private Set<UserDeviceConfig> mUserDeviceList = new HashSet<UserDeviceConfig>();

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

		HandlerThread backEnd = new HandlerThread("back-end");
		backEnd.start();
		thread = new Handler(backEnd.getLooper());

		mImCB = new ImRequestCB(mCallbackHandler);
		ImRequest.getInstance().setCallback(mImCB);

		mCRCB = new ConfRequestCB(mCallbackHandler);
		ConfRequest.getInstance().setCallback(mCRCB);

		mGRCB = new GroupRequestCB(mCallbackHandler);
		GroupRequest.getInstance().setCallback(mGRCB);

		mVRCB = new VideoRequestCB(mCallbackHandler);
		VideoRequest.getInstance().setCallback(mVRCB);
		
		mChRCB = new ChatRequestCB(mCallbackHandler);
		ChatRequest.getInstance().setChatRequestCallback(mChRCB);
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
		}
		return super.onUnbind(intent);
	}

	/**
	 * Used to local binder
	 * 
	 * @author 28851274
	 * 
	 */
	public class LocalBinder extends Binder {
		public JNIService getService() {
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
	 *            callback message Message.obj is {@link AsynResult}
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

	/**
	 * Get current logged user id
	 * 
	 * @return
	 */
	public long getLoggedUserId() {
		return this.mloggedInUserId;
	}

	/**
	 * Get current logged user's object
	 * 
	 * @return {@link com.v2tech.logic.User}
	 * 
	 * @see com.v2tech.logic.User
	 */
	public User getloggedUser() {
		return GlobalHolder.getInstance().getUser(Long.valueOf(this.mloggedInUserId));
	}

	/**
	 * Get user data according to user id
	 * 
	 * @param id
	 * @return
	 */
	public User getUser(long id) {
		return GlobalHolder.getInstance().getUser(Long.valueOf(id));
	}

	/**
	 * Update user data according to user object
	 * 
	 * @param u
	 *            user data
	 * @param caller
	 *            After update, send message to caller
	 */
	public void updateUserData(User u, Message caller) {
		// TODO need to implement
	}

	/**
	 * get user object according to user id.
	 * 
	 * @param nUserID
	 * @return {@link com.v2tech.logic.User}
	 * 
	 * @see com.v2tech.logic.User
	 */
	public User getUserBaseInfo(long nUserID) {
		return GlobalHolder.getInstance().getUser(nUserID);
	}

	/**
	 * Group information is server active call, we can't request from server
	 * directly.<br>
	 * Only way to get group information is waiting for server call.<br>
	 * So if this function return null, means service doesn't receive any call
	 * from server. otherwise server already sent group information to service.<br>
	 * If you want to know indication, please register receiver:<br>
	 * category: {@link #JNI_BROADCAST_CATEGROY} <br>
	 * action : {@link #JNI_BROADCAST_GROUP_NOTIFICATION}<br>
	 * Notice: maybe you didn't receive broadcast forever, because this
	 * broadcast is sent before you register
	 * 
	 * @param gType
	 * @return return null means server didn't send group information to
	 *         service.
	 */
	public List<Group> getGroup(Group.GroupType gType) {
		if (gType == Group.GroupType.CONFERENCE) {
			if (mLoadGroupOwnerCount > 0) {
				return null;
			}
			return this.mConfGroup;
		} else if (gType == Group.GroupType.CONTACT) {
			return this.mContactsGroup;
		} else {
			throw new RuntimeException(" Unknown group type :" + gType);
		}
	}

	/**
	 * If return null, means service doesn't receive group data or user data.<br>
	 * 
	 * @param gId
	 * @return
	 */
	public List<User> getGroupUser(long gId) {
		for (Group g : this.mContactsGroup) {
			if (g.getmGId() == gId) {
				return g.getUsers();
			}
		}
		return null;
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
			m.timeOutMessage = Message.obtain(mCallbackHandler,
					REQUEST_TIME_OUT, JNI_REQUEST_ENTER_CONF, 0);
			Message.obtain(mHandler, JNI_REQUEST_ENTER_CONF,
					Long.valueOf(confID)).sendToTarget();
			mCallbackHandler.sendMessageDelayed(m.timeOutMessage, 10000);
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
	 * User request quit conference
	 * 
	 * @param confID
	 * @param msg
	 *            if input is null, ignore response Message.object is
	 *            {@link AsynResult} AsynResult.obj is Object[] Object[0]
	 *            Integer 0: success 1: failed Object[1] Long conference id
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
	 * 
	 * @param nGroupID
	 *            conference id
	 * @param userDevice
	 *            {@link UserDeviceConfig} if want to open local video,
	 *            {@link UserDeviceConfig#getVp()} should be null and
	 *            {@link UserDeviceConfig#getDeviceID()} should be ""
	 * @param caller
	 *            message object for response
	 * 
	 * @see UserDeviceConfig
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
		// put to queue
		getAndQueued(JNI_REQUEST_OPEN_VIDEO, caller);
		Message.obtain(mHandler, JNI_REQUEST_OPEN_VIDEO,
				new OpenVideoRequest(nGroupID, userDevice)).sendToTarget();
	}

	/**
	 * Request close video device.
	 * 
	 * @param nGroupID
	 * @param userDevice
	 *            {@link UserDeviceConfig} if want to open local video,
	 *            {@link UserDeviceConfig#getVp()} should be null and
	 *            {@link UserDeviceConfig#getDeviceID()} should be ""
	 * @param caller
	 *            message object for response
	 * 
	 * @see UserDeviceConfig
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
		// put to queue
		getAndQueued(JNI_REQUEST_CLOSE_VIDEO, caller);
		Message.obtain(mHandler, JNI_REQUEST_CLOSE_VIDEO,
				new OpenVideoRequest(nGroupID, userDevice)).sendToTarget();
	}

	/**
	 * Request speak permission on the conference.
	 * 
	 * @param type
	 *            speak type should be {@link ConferencePermission#SPEAKING}
	 * @param caller
	 *            message for response, as now no response to send
	 * 
	 * @see ConferencePermission
	 */
	public void applyForControlPermission(ConferencePermission type,
			Message caller) {
		Message.obtain(mHandler, JNI_REQUEST_SPEAK, type.intValue(), 0)
				.sendToTarget();
	}

	/**
	 * Request release permission on the conference.
	 * 
	 * @param type
	 *            speak type should be {@link ConferencePermission#SPEAKING}
	 * @param caller
	 *            message for response, as now no response to send
	 * 
	 * @see ConferencePermission
	 */
	public void applyForReleasePermission(ConferencePermission type,
			Message caller) {
		Message.obtain(mHandler, JNI_REQUEST_RELEASE_SPEAK, type.intValue(), 0)
				.sendToTarget();
	}

	/**
	 * 
	 * @param cc
	 * @param caller
	 */
	public void updateCameraParameters(CameraConfiguration cc, Message caller) {
		if (caller != null) {
			// put caller message to the queue
			getAndQueued(JNI_UPDATE_CAMERA_PAR, caller);
		}
		Message.obtain(mHandler, JNI_UPDATE_CAMERA_PAR, cc).sendToTarget();
	}

	public void sendMessage(VMessage msg, Message caller) {
		if (caller != null) {
			getAndQueued(JNI_SEND_MESSAGE, caller);
		}
		Message.obtain(mHandler, JNI_SEND_MESSAGE, msg).sendToTarget();
	}

	/**
	 * Get user's video device according to user id.<br>
	 * This function never return null, even through we don't receive video
	 * device data from server.
	 * 
	 * @param uid
	 *            user's id
	 * @return list of user device
	 */
	public List<UserDeviceConfig> getAttendeeDevice(long uid) {
		List<UserDeviceConfig> l = new ArrayList<UserDeviceConfig>();
		for (UserDeviceConfig udl : mUserDeviceList) {
			if (udl.getUserID() == uid) {
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

	class GroupUserInfoOrig {
		int gType;
		long gId;
		String xml;

		public GroupUserInfoOrig(int gType, long gId, String xml) {
			super();
			this.gType = gType;
			this.gId = gId;
			this.xml = xml;
		}

	}

	// //////////////////////////////////////////////////////////
	// Internal message definition //
	// //////////////////////////////////////////////////////////

	private static final int CANCEL_REQUEST = 0x1000;
	private static final int REQUEST_TIME_OUT = 0x2000;
	private static final int JNI_LOG_IN = 21;
	private static final int JNI_LOG_OUT = 22;
	private static final int JNI_CONNECT_RESPONSE = 23;
	private static final int JNI_UPDATE_USER_INFO = 24;
	private static final int JNI_UPDATE_USER_STATUS = 25;
	private static final int JNI_GROUP_NOTIFY = 35;
	private static final int JNI_LOAD_GROUP_OWNER_INFO = 36;
	private static final int JNI_REQUEST_ENTER_CONF = 55;
	private static final int JNI_REQUEST_EXIT_CONF = 56;
	private static final int JNI_ATTENDEE_ENTERED_NOTIFICATION = 57;
	private static final int JNI_ATTENDEE_EXITED_NOTIFICATION = 58;
	private static final int JNI_GET_ATTENDEE_INFO_DONE = 59;
	private static final int JNI_GROUP_USER_INFO_NOTIFICATION = 60;
	private static final int JNI_REQUEST_OPEN_VIDEO = 70;
	private static final int JNI_REQUEST_CLOSE_VIDEO = 71;
	private static final int JNI_REQUEST_SPEAK = 72;
	private static final int JNI_REQUEST_RELEASE_SPEAK = 73;
	private static final int JNI_UPDATE_CAMERA_PAR = 75;
	private static final int JNI_REMOTE_USER_DEVICE_INFO_NOTIFICATION = 80;
	private static final int JNI_SEND_MESSAGE = 90;
	private static final int JNI_RECEIVED_MESSAGE = 91;

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
				iu.m.timeOutMessage = m;
				mCallbackHandler.sendMessageDelayed(m, 10000);
				break;

			case JNI_LOG_OUT:
				break;
			case JNI_UPDATE_USER_INFO:
				ImRequest.getInstance().getUserBaseInfo((Long) msg.obj);
				break;
			case JNI_GROUP_NOTIFY:
				break;
			case JNI_LOAD_GROUP_OWNER_INFO:
				MetaData gmd = getAndQueued(JNI_UPDATE_USER_INFO,
						Message.obtain(mCallbackHandler,
								JNI_LOAD_GROUP_OWNER_INFO, msg.arg1, 0));
				if (gmd == null) {
					// TODO
				} else {
					Message.obtain(mHandler, JNI_UPDATE_USER_INFO,
							mConfGroup.get(msg.arg1).getOwner()).sendToTarget();
				}

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
				VideoRequest.getInstance().closeVideoDevice(
						requestCloseObj.group,
						requestCloseObj.userDevice.getUserID(),
						requestCloseObj.userDevice.getDeviceID(),
						requestCloseObj.userDevice.getVp(),
						requestCloseObj.userDevice.getBusinessType());
				// As now open video device no call back.
				// So send successful message to caller
				MetaData response = getMeta(JNI_REQUEST_CLOSE_VIDEO);
				if (response != null && response.caller != null) {
					response.caller.obj = new AsynResult(AsynState.SUCCESS,
							null);
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
			case JNI_UPDATE_CAMERA_PAR:
				if (msg.obj == null) {
					// FIXME send error result
				} else {
					CameraConfiguration cc = (CameraConfiguration) msg.obj;
					VideoRequest.getInstance().setCapParam(cc.getDeviceId(),
							cc.getCameraIndex(), cc.getFrameRate(),
							cc.getBitRate());
				}
				break;
			case JNI_SEND_MESSAGE:
				final VMessage vm = (VMessage) msg.obj;
				ChatRequest.getInstance().sendChatText(0,
						vm.getToUser().getmUserId(), vm.toXml(),
						ChatRequest.BT_IM);
				if (vm.getType() == VMessage.MessageType.IMAGE) {
					thread.post(new Runnable() {
						@Override
						public void run() {
							byte[] data= ((VImageMessage)vm).getWrapperData();
							ChatRequest.getInstance().sendChatPicture(0,
									vm.getToUser().getmUserId(), data, data.length,
									ChatRequest.BT_IM);
						}

					});
				}
				//TODO as now send message no callback, just send response to message caller
				MetaData responseSendMsg = getMeta(JNI_SEND_MESSAGE);
				if (responseSendMsg != null && responseSendMsg.caller != null) {
					responseSendMsg.caller.obj = new AsynResult(AsynState.SUCCESS,
							null);
					responseSendMsg.caller.sendToTarget();
				}
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
				// just remove message from queue
				getMeta(msg.arg1);
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
						if (m.timeOutMessage != null) {
							// You can't use this because mCallbackHandler is
							// different object.
							// we use handler thread
							mCallbackHandler.removeMessages(REQUEST_TIME_OUT);
						}
					}
					// after login or before log in send broadcast
					Toast.makeText(mContext, R.string.error_connect_to_server,
							Toast.LENGTH_SHORT).show();

				}
				break;
			case JNI_UPDATE_USER_INFO:
				User u = User.fromXml(msg.arg1, (String) msg.obj);
				if (u.getmUserId() == mloggedInUserId) {
					u.setCurrentLoggedInUser(true);
				}
				GlobalHolder.getInstance().putUser(u.getmUserId(), u);
				// If someone waiting for this event
				ar = new AsynResult(AsynResult.AsynState.SUCCESS, u);
				if (d != null && d.caller != null
						&& d.caller.what == JNI_LOAD_GROUP_OWNER_INFO) {
					arg1 = d.caller.arg1;
				}
				break;
			case JNI_UPDATE_USER_STATUS:
				Intent iun = new Intent(
						JNI_BROADCAST_USER_STATUS_NOTIFICATION);
				iun.addCategory(JNI_BROADCAST_CATEGROY);
				iun.putExtra("uid", Long.valueOf(msg.arg1));
				iun.putExtra("status", msg.arg2);
				mContext.sendBroadcast(iun);
				
				break;
			case JNI_GROUP_NOTIFY:
				if (msg.arg1 == Group.GroupType.CONFERENCE.intValue()) {
					mConfGroup = Group
							.parserFromXml(msg.arg1, (String) msg.obj);
					mLoadGroupOwnerCount = mConfGroup.size();

					if (mConfGroup != null && mConfGroup.size() > 0) {
						Message.obtain(mHandler, JNI_LOAD_GROUP_OWNER_INFO, 0,
								0).sendToTarget();
					}
				} else if (msg.arg1 == Group.GroupType.CONTACT.intValue()) {
					mContactsGroup = Group.parserFromXml(msg.arg1,
							(String) msg.obj);
					// TODO send broadcast to tell UI contacts group update
				}

				ar = new AsynResult(AsynResult.AsynState.SUCCESS, mConfGroup);

				break;
			case JNI_LOAD_GROUP_OWNER_INFO:
				mLoadGroupOwnerCount--;
				User gu = (User) ((AsynResult) msg.obj).getObject();

				Group g = mConfGroup.get(msg.arg1);
				if (g != null && g.getOwner() == gu.getmUserId()) {
					g.setOwnerUser(gu);
				}

				if (mLoadGroupOwnerCount == 0) {
					Intent ei = new Intent(JNI_BROADCAST_GROUP_NOTIFICATION);
					ei.addCategory(JNI_BROADCAST_CATEGROY);
					ei.putExtra("gtype", Group.GroupType.CONFERENCE.intValue());
					mContext.sendBroadcast(ei);
				} else {
					arg1 = msg.arg1 + 1;
					Message.obtain(mHandler, JNI_LOAD_GROUP_OWNER_INFO,
							msg.arg1 + 1, 0).sendToTarget();
				}
				break;
			case JNI_GROUP_USER_INFO_NOTIFICATION:
				GroupUserInfoOrig go = (GroupUserInfoOrig) msg.obj;
				if (go != null && go.xml != null) {
					List<User> lu = User.fromXml(go.xml);
					addUserToGroup(mContactsGroup, lu, go.gId);
					for (User tu : lu) {
						User.Status us = GlobalHolder.getInstance().getOnlineUserStatus(tu.getmUserId());
						if (us != null) {
							tu.updateStatus(us);
						}
						GlobalHolder.getInstance().putUser(tu.getmUserId(), tu);
					}
					Intent i = new Intent(
							JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION);
					i.addCategory(JNI_BROADCAST_CATEGROY);
					i.putExtra("gid", go.gId);
					i.putExtra("gtype", go.gType);
					mContext.sendStickyBroadcast(i);

				} else {
					V2Log.e("Invalid group user data");
				}
				break;
			case JNI_REQUEST_ENTER_CONF:
				RequestEnterConfResponse recr = (RequestEnterConfResponse) msg.obj;
				ar = new AsynResult(AsynResult.AsynState.SUCCESS, new Object[] {
						recr.nJoinResult, recr.nConfID });
				break;
			case JNI_ATTENDEE_ENTERED_NOTIFICATION:
				Long uid = Long.valueOf(Long.parseLong(msg.obj.toString()));
				User attendeeUser = GlobalHolder.getInstance().getUser(uid);
				// check cache, if exist, send successful event
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
				Intent ei = new Intent(
						JNI_BROADCAST_ATTENDEE_EXITED_NOTIFICATION);
				ei.addCategory(JNI_BROADCAST_CATEGROY);
				ei.putExtra("uid", (Long) msg.obj);
				ei.putExtra("name",
						GlobalHolder.getInstance().getUser(Long.valueOf((Long) msg.obj)).getName());
				mContext.sendBroadcast(ei);
				break;
			case JNI_GET_ATTENDEE_INFO_DONE:

				User attendee = (User) ((AsynResult) msg.obj).getObject();
				Intent i = new Intent(
						JNI_BROADCAST_ATTENDEE_ENTERED_NOTIFICATION);
				i.addCategory(JNI_BROADCAST_CATEGROY);
				i.putExtra("uid", attendee.getmUserId());
				i.putExtra("name", attendee.getName());
				mContext.sendStickyBroadcast(i);
				V2Log.i("send broad cast for attendee enter :"
						+ attendee.getName());
				break;
			case JNI_UPDATE_CAMERA_PAR:
				break;
			case JNI_REMOTE_USER_DEVICE_INFO_NOTIFICATION:
				mUserDeviceList.addAll(UserDeviceConfig
						.parseFromXml((String) msg.obj));
				break;
				
			case JNI_RECEIVED_MESSAGE:
				VMessage vm = (VMessage)msg.obj;
				if (vm != null) {
					Uri uri = saveMessageToDB(vm);
					Intent ii = new Intent(
							JNI_BROADCAST_NEW_MESSAGE);
					ii.addCategory(JNI_BROADCAST_CATEGROY);
					ii.putExtra("mid", uri.getLastPathSegment());
					mContext.sendBroadcast(ii);
					sendNotification();
				}
				break;

			}

			if (d != null && d.caller != null) {
				d.caller.obj = ar;
				d.caller.arg1 = arg1;
				d.caller.arg2 = arg2;
				d.caller.sendToTarget();
				if (d.timeOutMessage != null) {
					// You can't use this because mCallbackHandler is different
					// object.
					// we use handler thread
					mCallbackHandler.removeMessages(REQUEST_TIME_OUT);
				}
			} else {
				V2Log.w("MSG: " + msg.what
						+ " Metadata object or call is null ");
			}
		}
		
		
		/**
		 * Add user list to Group
		 * @param gList
		 * @param uList
		 * @param belongGID
		 */
		private void addUserToGroup(List<Group> gList, List<User> uList, long belongGID) {
			for (Group g : gList) {
				if(belongGID == g.getmGId()) {
					g.addUserToGroup(uList);
					return;
				}
				addUserToGroup(g.getChildGroup(), uList, belongGID);
			}
		}
		
		
		
		private Uri saveMessageToDB(VMessage vm) {
			ContentValues cv = new ContentValues();
			cv.put(ContentDescriptor.Messages.Cols.FROM_USER_ID, vm.getUser().getmUserId());
			cv.put(ContentDescriptor.Messages.Cols.TO_USER_ID, vm.getToUser().getmUserId());
			cv.put(ContentDescriptor.Messages.Cols.MSG_CONTENT, vm.getText());
			cv.put(ContentDescriptor.Messages.Cols.MSG_TYPE,vm.getType().getIntValue());
			cv.put(ContentDescriptor.Messages.Cols.SEND_TIME, vm.getNormalDateStr());
			return getContentResolver().insert(ContentDescriptor.Messages.CONTENT_URI,
					cv);
		}
		
		private void sendNotification() {
			Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
		}
	}

	// ///////////////////////////////////////////////
	// JNI call back implements //
	// FIXME Need to be optimize code structure //
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

		@Override
		public void OnUserStatusUpdatedCallback(long nUserID, int eUEType,
				int nStatus, String szStatusDesc) {
			GlobalHolder.getInstance().updateUserStatus(nUserID, User.Status.fromInt(nStatus));
			User u = GlobalHolder.getInstance().getUser(nUserID);
			if (u == null) {
				V2Log.e("Can't update user status, user "+ nUserID+"  isn't exist");
			} else {
				u.updateStatus(User.Status.fromInt(nStatus));
			}
			Message.obtain(mCallbackHandler, JNI_UPDATE_USER_STATUS, (int)nUserID, nStatus).sendToTarget();
		}
		
		

	}

	class GroupRequestCB implements GroupRequestCallback {
		private JNICallbackHandler mCallbackHandler;

		public GroupRequestCB(JNICallbackHandler mCallbackHandler) {
			this.mCallbackHandler = mCallbackHandler;
		}

		@Override
		public void OnGetGroupInfoCallback(int groupType, String sXml) {
			if (isDebug) {
				V2Log.d("group type:"+groupType +" " + sXml);
			}
			Message.obtain(mCallbackHandler, JNI_GROUP_NOTIFY, groupType, 0,
					sXml).sendToTarget();
		}

		@Override
		public void OnGetGroupUserInfoCallback(int groupType, long nGroupID,
				String sXml) {
			if (isDebug) {
				V2Log.d("group type:"+groupType +" "+ nGroupID +" " + sXml);
			}
			Message.obtain(mCallbackHandler, JNI_GROUP_USER_INFO_NOTIFICATION,
					new GroupUserInfoOrig(groupType, nGroupID, sXml))
					.sendToTarget();
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
			Message.obtain(mCallbackHandler, JNI_ATTENDEE_EXITED_NOTIFICATION,
					0, 0, nUserID).sendToTarget();
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
			Message.obtain(mCallbackHandler,
					JNI_REMOTE_USER_DEVICE_INFO_NOTIFICATION, szXmlData)
					.sendToTarget();
		}

		@Override
		public void OnSetCapParamDone(String szDevID, int nSizeIndex,
				int nFrameRate, int nBitRate) {

		}

	}
	
	
	class ChatRequestCB implements ChatRequestCallback {
		
		private JNICallbackHandler mCallbackHandler;

		public ChatRequestCB(JNICallbackHandler mCallbackHandler) {
			this.mCallbackHandler = mCallbackHandler;
		}
		
		@Override
		public void OnRecvChatTextCallback(long nGroupID, int nBusinessType,
				long nFromUserID, long nTime, String szXmlText) {
			User toUser = GlobalHolder.getInstance().getCurrentUser();
			User fromUser =  GlobalHolder.getInstance().getUser(nFromUserID);
			if (toUser == null || fromUser == null) {
				V2Log.e("No valid user object "+ toUser  +"  "+ fromUser);
				return;
			}
			VMessage vm = VMessage.fromXml(szXmlText);
			if (vm == null) {
				V2Log.e(" xml parsed failed : "+ szXmlText);
				return;
			}
			vm.setToUser(toUser);
			vm.setUser(fromUser);
			vm.setLocal(false);
			vm.setDate(new Date(nTime * 1000));
			vm.setType(MessageType.TEXT);
			Message.obtain(mCallbackHandler,
					JNI_RECEIVED_MESSAGE, vm)
					.sendToTarget();
		}

		@Override
		public void OnRecvChatPictureCallback(long nGroupID, int nBusinessType,
				long nFromUserID, long nTime, byte[] pPicData) {
			User toUser = GlobalHolder.getInstance().getCurrentUser();
			User fromUser =  GlobalHolder.getInstance().getUser(nFromUserID);
			VMessage vm = new VImageMessage(fromUser, toUser, pPicData);
			vm.setLocal(false);
			vm.setDate(new Date(nTime * 1000));
			Message.obtain(mCallbackHandler,
					JNI_RECEIVED_MESSAGE, vm)
					.sendToTarget();
		}
		
	}

}
