package com.v2tech.view;

import java.io.File;
import java.util.Date;
import java.util.List;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
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
import com.v2tech.logic.GlobalHolder;
import com.v2tech.logic.Group;
import com.v2tech.logic.NetworkStateCode;
import com.v2tech.logic.User;
import com.v2tech.logic.UserDeviceConfig;
import com.v2tech.logic.VImageMessage;
import com.v2tech.logic.VMessage;
import com.v2tech.util.V2Log;

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
	public static final String JNI_BROADCAST_CONNECT_STATE_NOTIFICATION = "com.v2tech.jni.broadcast.connect_state_notification";
	public static final String JNI_BROADCAST_USER_STATUS_NOTIFICATION = "com.v2tech.jni.broadcast.user_stauts_notification";
	public static final String JNI_BROADCAST_USER_AVATAR_CHANGED_NOTIFICATION = "com.v2tech.jni.broadcast.user_avatar_notification";
	public static final String JNI_BROADCAST_USER_UPDATE_NAME_OR_SIGNATURE = "com.v2tech.jni.broadcast.user_update_sigature";
	public static final String JNI_BROADCAST_GROUP_NOTIFICATION = "com.v2tech.jni.broadcast.group_geted";
	public static final String JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION = "com.v2tech.jni.broadcast.group_user_updated";
	public static final String JNI_BROADCAST_ATTENDEE_ENTERED_NOTIFICATION = "com.v2tech.jni.broadcast.attendee.entered.notification";
	public static final String JNI_BROADCAST_ATTENDEE_EXITED_NOTIFICATION = "com.v2tech.jni.broadcast.attendee.exited.notification";
	public static final String JNI_BROADCAST_NEW_MESSAGE = "com.v2tech.jni.broadcast.new.message";

	private boolean isDebug = true;

	private final LocalBinder mBinder = new LocalBinder();

	private Integer mBinderRef = 0;

	private JNICallbackHandler mCallbackHandler;

	// ////////////////////////////////////////
	// JNI call back definitions
	private ImRequestCallback mImCB;

	private ConfRequestCallback mCRCB;

	private GroupRequestCB mGRCB;

	private VideoRequestCB mVRCB;

	private ChatRequestCB mChRCB;

	// ////////////////////////////////////////

	private Context mContext;

	// ////////////////////////////////////////

	@Override
	public void onCreate() {
		super.onCreate();
		mContext = this;

		HandlerThread callback = new HandlerThread("callback");
		callback.start();
		synchronized(callback) {
			while (!callback.isAlive()) {
				try {
					Thread.currentThread().sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		mCallbackHandler = new JNICallbackHandler(callback.getLooper());

		mImCB = new ImRequestCB(mCallbackHandler);
		ImRequest.getInstance().setCallback(mImCB);

		mCRCB = new ConfRequestCB(mCallbackHandler);
		ConfRequest.getInstance().addCallback(mCRCB);

		mGRCB = new GroupRequestCB(mCallbackHandler);
		GroupRequest.getInstance().setCallback(mGRCB);

		mVRCB = new VideoRequestCB(mCallbackHandler);
		VideoRequest.getInstance().addCallback(mVRCB);

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

	class VideoInvitionWrapper {
		long nGroupID;
		int nBusinessType;
		long nFromUserID;
		String szDeviceID;

		public VideoInvitionWrapper(long nGroupID, int nBusinessType,
				long nFromUserID, String szDeviceID) {
			super();
			this.nGroupID = nGroupID;
			this.nBusinessType = nBusinessType;
			this.nFromUserID = nFromUserID;
			this.szDeviceID = szDeviceID;
		}

	}

	private void broadcastNetworkState(NetworkStateCode code) {
		Intent i = new Intent();
		i.setAction(JNI_BROADCAST_CONNECT_STATE_NOTIFICATION);
		i.addCategory(JNI_BROADCAST_CATEGROY);
		i.putExtra("state", (Parcelable) code);
		sendBroadcast(i);
	}

	// //////////////////////////////////////////////////////////
	// Internal message definition //
	// //////////////////////////////////////////////////////////

	private static final int JNI_CONNECT_RESPONSE = 23;
	private static final int JNI_UPDATE_USER_INFO = 24;
	private static final int JNI_UPDATE_USER_STATUS = 25;
	private static final int JNI_LOG_OUT = 26;
	private static final int JNI_GROUP_NOTIFY = 35;
	private static final int JNI_ATTENDEE_ENTERED_NOTIFICATION = 57;
	private static final int JNI_ATTENDEE_EXITED_NOTIFICATION = 58;
	private static final int JNI_GET_ATTENDEE_INFO_DONE = 59;
	private static final int JNI_GROUP_USER_INFO_NOTIFICATION = 60;
	private static final int JNI_REMOTE_USER_DEVICE_INFO_NOTIFICATION = 80;
	private static final int JNI_RECEIVED_MESSAGE = 91;
	private static final int JNI_RECEIVED_VIDEO_INVITION = 92;

	class JNICallbackHandler extends Handler {

		public JNICallbackHandler(Looper looper) {
			super(looper);
		}

		@Override
		public synchronized void handleMessage(Message msg) {
			switch (msg.what) {

			case JNI_CONNECT_RESPONSE:
				broadcastNetworkState(NetworkStateCode.fromInt(msg.arg1));
				break;
			case JNI_UPDATE_USER_INFO:
				User u = User.fromXml(msg.arg1, (String) msg.obj);

				GlobalHolder.getInstance().putUser(u.getmUserId(), u);

				Intent sigatureIntent = new Intent();
				sigatureIntent
						.setAction(JNI_BROADCAST_USER_UPDATE_NAME_OR_SIGNATURE);
				sigatureIntent.addCategory(JNI_BROADCAST_CATEGROY);
				sigatureIntent.putExtra("uid", u.getmUserId());
				sendBroadcast(sigatureIntent);
				break;
			case JNI_UPDATE_USER_STATUS:
				Intent iun = new Intent(JNI_BROADCAST_USER_STATUS_NOTIFICATION);
				iun.addCategory(JNI_BROADCAST_CATEGROY);
				iun.putExtra("uid", Long.valueOf(msg.arg1));
				iun.putExtra("status", msg.arg2);
				mContext.sendBroadcast(iun);

				break;
			case JNI_LOG_OUT:
				Toast.makeText(mContext,
						R.string.user_logged_with_other_device,
						Toast.LENGTH_LONG).show();
				break;
			case JNI_GROUP_NOTIFY:
				List<Group> gl = Group
						.parserFromXml(msg.arg1, (String) msg.obj);
				GlobalHolder.getInstance().updateGroupList(
						Group.GroupType.fromInt(msg.arg1), gl);
				Intent gi = new Intent(JNI_BROADCAST_GROUP_NOTIFICATION);
				gi.addCategory(JNI_BROADCAST_CATEGROY);
				mContext.sendBroadcast(gi);
				break;

			case JNI_GROUP_USER_INFO_NOTIFICATION:
				GroupUserInfoOrig go = (GroupUserInfoOrig) msg.obj;
				if (go != null && go.xml != null) {
					List<User> lu = User.fromXml(go.xml);
					for (User tu : lu) {
						User.Status us = GlobalHolder.getInstance()
								.getOnlineUserStatus(tu.getmUserId());
						if (us != null) {
							tu.updateStatus(us);
						}
						User existU = GlobalHolder.getInstance().putUser(
								tu.getmUserId(), tu);
						if (existU.getmUserId() == GlobalHolder.getInstance().getCurrentUserId()) {
							//Update logged user object.
							GlobalHolder.getInstance().setCurrentUser(existU);
						}
						GlobalHolder.getInstance().addUserToGroup(existU,
								go.gId);
					}
					Intent i = new Intent(
							JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION);
					i.addCategory(JNI_BROADCAST_CATEGROY);
					i.putExtra("gid", go.gId);
					i.putExtra("gtype", go.gType);
					mContext.sendBroadcast(i);

				} else {
					V2Log.e("Invalid group user data");
				}
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

				}
				break;
			case JNI_ATTENDEE_EXITED_NOTIFICATION:
				Intent ei = new Intent(
						JNI_BROADCAST_ATTENDEE_EXITED_NOTIFICATION);
				ei.addCategory(JNI_BROADCAST_CATEGROY);
				ei.putExtra("uid", (Long) msg.obj);
				ei.putExtra(
						"name",
						GlobalHolder.getInstance()
								.getUser(Long.valueOf((Long) msg.obj))
								.getName());
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
			case JNI_REMOTE_USER_DEVICE_INFO_NOTIFICATION:
				GlobalHolder.getInstance().addAttendeeDevice(
						UserDeviceConfig.parseFromXml((String) msg.obj));
				break;

			case JNI_RECEIVED_MESSAGE:
				VMessage vm = (VMessage) msg.obj;
				if (vm != null) {
					Uri uri = saveMessageToDB(vm);
					Intent ii = new Intent(JNI_BROADCAST_NEW_MESSAGE);
					ii.addCategory(JNI_BROADCAST_CATEGROY);
					ii.putExtra("mid", uri.getLastPathSegment());
					ii.putExtra("fromuid", vm.getUser().getmUserId());
					mContext.sendBroadcast(ii);
					sendNotification();
					updateStatusBar(vm);
				}
				break;
			case JNI_RECEIVED_VIDEO_INVITION:
				Intent iv = new Intent();
				iv.addCategory(PublicIntent.DEFAULT_CATEGORY);
				iv.setAction(PublicIntent.START_VIDEO_CONVERSACTION_ACTIVITY);
				iv.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				iv.putExtra("uid", ((VideoInvitionWrapper) msg.obj).nFromUserID);
				iv.putExtra("is_coming_call", true);
				mContext.startActivity(iv);
				break;

			}

		}

		private Uri saveMessageToDB(VMessage vm) {
			ContentValues cv = new ContentValues();
			cv.put(ContentDescriptor.Messages.Cols.FROM_USER_ID, vm.getUser()
					.getmUserId());
			cv.put(ContentDescriptor.Messages.Cols.TO_USER_ID, vm.getToUser()
					.getmUserId());
			cv.put(ContentDescriptor.Messages.Cols.MSG_CONTENT, vm.getText());
			cv.put(ContentDescriptor.Messages.Cols.MSG_TYPE, vm.getType()
					.getIntValue());
			cv.put(ContentDescriptor.Messages.Cols.SEND_TIME,
					vm.getFullDateStr());
			Uri uri = getContentResolver().insert(
					ContentDescriptor.Messages.CONTENT_URI, cv);
			return uri;
		}

		private void sendNotification() {
			Uri notification = RingtoneManager
					.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			Ringtone r = RingtoneManager.getRingtone(getApplicationContext(),
					notification);
			r.play();
		}

		private void updateStatusBar(VMessage vm) {
			if (vm.getUser().getmUserId() == GlobalHolder.getInstance().CURRENT_CONVERSATION_USER) {
				return;
			}
			NotificationCompat.Builder builder = new NotificationCompat.Builder(
					mContext).setSmallIcon(R.drawable.ic_launcher)
					.setContentTitle(vm.getUser().getName())
					.setContentText(vm.getText());

			Intent resultIntent = new Intent(
					PublicIntent.START_CONVERSACTION_ACTIVITY);
			resultIntent.putExtra("user1id", GlobalHolder.getInstance()
					.getCurrentUserId());
			resultIntent.putExtra("user2id", vm.getUser().getmUserId());
			resultIntent.putExtra("user2Name", vm.getUser().getName());
			resultIntent.addCategory(PublicIntent.DEFAULT_CATEGORY);
			
			
			Intent startupActivity = new Intent(mContext, StartupActivity.class);

			// Creates the PendingIntent
			PendingIntent notifyPendingIntent = PendingIntent.getActivities(
					mContext, 0, new Intent[] {startupActivity, resultIntent},
					PendingIntent.FLAG_ONE_SHOT);

			// Puts the PendingIntent into the notification builder
			builder.setContentIntent(notifyPendingIntent);

			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			// mId allows you to update the notification later on.
			mNotificationManager.notify(PublicIntent.MESSAGE_NOTIFICATION_ID,
					builder.build());

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
		}

		@Override
		public void OnLogoutCallback(int nUserID) {
			Message.obtain(mCallbackHandler, JNI_LOG_OUT).sendToTarget();
			mCallbackHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					SharedPreferences sf = mContext.getSharedPreferences("config", Context.MODE_PRIVATE);
					Editor ed = sf.edit();
					ed.putInt("LoggedIn", 0);
					ed.commit();
					
					Intent i = new Intent();
					i.setAction(PublicIntent.FINISH_APPLICATION);
					i.addCategory(PublicIntent.DEFAULT_CATEGORY);
					mContext.sendBroadcast(i);
					//TODO optimze code
					try {
						Thread.currentThread().sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Process.killProcess(Process.myPid());
				}

			}, 2000);
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
			GlobalHolder.getInstance().updateUserStatus(nUserID,
					User.Status.fromInt(nStatus));
			User u = GlobalHolder.getInstance().getUser(nUserID);
			if (u == null) {
				V2Log.e("Can't update user status, user " + nUserID
						+ "  isn't exist");
			} else {
				u.updateStatus(User.Status.fromInt(nStatus));
			}
			Message.obtain(mCallbackHandler, JNI_UPDATE_USER_STATUS,
					(int) nUserID, nStatus).sendToTarget();
		}

		@Override
		public void OnChangeAvatarCallback(int nAvatarType, long nUserID,
				String AvatarName) {
			File f = new File(AvatarName);
			if (f.isDirectory()) {
				// Do not notify if is not file;
				return;
			}
			//System default icon
			if (AvatarName.equals("Default.png")) {
				AvatarName = null;
			}
			
			User u = GlobalHolder.getInstance().getUser(nUserID);
			if (u != null) {
				u.setAvatarPath(AvatarName);
			} else {
				V2Log.e(" Doesn't receive user data :"+ nUserID);
			}
			GlobalHolder.getInstance().putAvatar(nUserID, AvatarName);

			Intent i = new Intent();
			i.addCategory(JNI_BROADCAST_CATEGROY);
			i.setAction(JNI_BROADCAST_USER_AVATAR_CHANGED_NOTIFICATION);
			i.putExtra("uid", nUserID);
			i.putExtra("avatar", AvatarName);
			sendBroadcast(i);
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

		@Override
		public void OnGetGroupUserInfoCallback(int groupType, long nGroupID,
				String sXml) {
			if (isDebug) {
				V2Log.d("group type:" + groupType + " " + nGroupID + " " + sXml);
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
		}

		@Override
		public void OnConfMemberEnterCallback(long nConfID, long nTime,
				String szUserInfos) {
			// FIXME should send information at here?
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
		public void OnVideoChatInviteCallback(long nGroupID, int nBusinessType,
				long nFromUserID, String szDeviceID) {

			Message.obtain(
					mCallbackHandler,
					JNI_RECEIVED_VIDEO_INVITION,
					new VideoInvitionWrapper(nGroupID, nBusinessType,
							nFromUserID, szDeviceID)).sendToTarget();
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
			User fromUser = GlobalHolder.getInstance().getUser(nFromUserID);
			if (toUser == null) {
				V2Log.w("No valid user object for receive message " + toUser + "  " + fromUser);
				toUser = new User(GlobalHolder.getInstance().getCurrentUserId());
			}
			if (fromUser == null) {
				V2Log.w("No valid user object for receive message " + toUser + "  " + fromUser);
				fromUser = new User(nFromUserID);
			}
			VMessage vm = VMessage.fromXml(fromUser, toUser, new Date(), szXmlText);
			if (vm == null) {
				V2Log.e(" xml parsed failed : " + szXmlText);
				return;
			}
			Message.obtain(mCallbackHandler, JNI_RECEIVED_MESSAGE, vm)
					.sendToTarget();
		}

		@Override
		public void OnRecvChatPictureCallback(long nGroupID, int nBusinessType,
				long nFromUserID, long nTime, byte[] pPicData) {
			User toUser = GlobalHolder.getInstance().getCurrentUser();
			User fromUser = GlobalHolder.getInstance().getUser(nFromUserID);
			if (toUser == null) {
				V2Log.w("No valid user object for receive message " + toUser + "  " + fromUser);
				toUser = new User(GlobalHolder.getInstance().getCurrentUserId());
			}
			if (fromUser == null) {
				V2Log.w("No valid user object for receive message " + toUser + "  " + fromUser);
				fromUser = new User(nFromUserID);
			}
			VMessage vm = new VImageMessage(fromUser, toUser, pPicData);
			vm.setLocal(false);
			vm.setDate(new Date());
			Message.obtain(mCallbackHandler, JNI_RECEIVED_MESSAGE, vm)
					.sendToTarget();
		}

	}

}
