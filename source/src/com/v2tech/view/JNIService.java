package com.v2tech.view;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.os.Parcelable;
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
import com.v2tech.service.BitmapManager;
import com.v2tech.service.GlobalHolder;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.Notificator;
import com.v2tech.util.V2Log;
import com.v2tech.util.XmlParser;
import com.v2tech.view.bo.GroupUserObject;
import com.v2tech.view.bo.UserAvatarObject;
import com.v2tech.view.bo.UserStatusObject;
import com.v2tech.view.conference.VideoActivityV2;
import com.v2tech.vo.ConferenceGroup;
import com.v2tech.vo.Conversation;
import com.v2tech.vo.CrowdConversation;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.NetworkStateCode;
import com.v2tech.vo.User;
import com.v2tech.vo.UserDeviceConfig;
import com.v2tech.vo.VImageMessage;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.VMessage.MessageType;

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
	
	/**
	 * Notify user avatar changed, notice please do not listen this broadcast if you are UI. 
	 * Use {@link BitmapManager#registerBitmapChangedListener(com.v2tech.service.BitmapManager.BitmapChangedListener)}
	 * to listener bitmap change if you are UI.
	 */
	public static final String JNI_BROADCAST_USER_AVATAR_CHANGED_NOTIFICATION = "com.v2tech.jni.broadcast.user_avatar_notification";
	public static final String JNI_BROADCAST_USER_UPDATE_NAME_OR_SIGNATURE = "com.v2tech.jni.broadcast.user_update_sigature";
	public static final String JNI_BROADCAST_GROUP_NOTIFICATION = "com.v2tech.jni.broadcast.group_geted";
	public static final String JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION = "com.v2tech.jni.broadcast.group_user_updated";
	public static final String JNI_BROADCAST_NEW_MESSAGE = "com.v2tech.jni.broadcast.new.message";
	public static final String JNI_BROADCAST_NEW_CONF_MESSAGE = "com.v2tech.jni.broadcast.new.conf.message";
	public static final String JNI_BROADCAST_CONFERENCE_INVATITION = "com.v2tech.jni.broadcast.conference_invatition";
	public static final String JNI_BROADCAST_CONFERENCE_REMOVED = "com.v2tech.jni.broadcast.conference_removed";
	public static final String JNI_BROADCAST_GROUP_USER_REMOVED = "com.v2tech.jni.broadcast.group_user_removed";
	public static final String JNI_BROADCAST_GROUP_USER_ADDED = "com.v2tech.jni.broadcast.group_user_added";

	private boolean isDebug = true;

	private final LocalBinder mBinder = new LocalBinder();

	private Integer mBinderRef = 0;

	private JNICallbackHandler mCallbackHandler;

	// ////////////////////////////////////////
	// JNI call back definitions
	private ImRequestCallback mImCB;

	private GroupRequestCB mGRCB;

	private VideoRequestCB mVRCB;

	private ChatRequestCB mChRCB;
	
	private ConfRequestCB mCRCB;

	// ////////////////////////////////////////

	private Context mContext;

	private List<VMessage> cacheImageMeta = new ArrayList<VMessage>();

	// ////////////////////////////////////////

	@Override
	public void onCreate() {
		super.onCreate();
		mContext = this;

		HandlerThread callback = new HandlerThread("callback");
		callback.start();
		synchronized (callback) {
			while (!callback.isAlive()) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		mCallbackHandler = new JNICallbackHandler(callback.getLooper());

		mImCB = new ImRequestCB(mCallbackHandler);
		ImRequest.getInstance(this.getApplicationContext()).setCallback(mImCB);

		mGRCB = new GroupRequestCB(mCallbackHandler);
		GroupRequest.getInstance(this.getApplicationContext()).addCallback(
				mGRCB);

		mVRCB = new VideoRequestCB(mCallbackHandler);
		VideoRequest.getInstance(this.getApplicationContext()).addCallback(
				mVRCB);

		mChRCB = new ChatRequestCB(mCallbackHandler);
		ChatRequest.getInstance(this.getApplicationContext())
				.setChatRequestCallback(mChRCB);
		
		mCRCB = new ConfRequestCB(mCallbackHandler);
		ConfRequest.getInstance().addCallback(mCRCB);
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

	static long lastNotificatorTime = 0;

	// //////////////////////////////////////////////////////////
	// Internal message definition //
	// //////////////////////////////////////////////////////////

	private static final int JNI_CONNECT_RESPONSE = 23;
	private static final int JNI_UPDATE_USER_INFO = 24;
	private static final int JNI_LOG_OUT = 26;
	private static final int JNI_GROUP_NOTIFY = 35;
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
			case JNI_LOG_OUT:
				Toast.makeText(mContext,
						R.string.user_logged_with_other_device,
						Toast.LENGTH_LONG).show();
				break;
			case JNI_GROUP_NOTIFY:
				List<Group> gl = XmlParser.parserFromXml(msg.arg1,
						(String) msg.obj);
				GlobalHolder.getInstance().updateGroupList(
						Group.GroupType.fromInt(msg.arg1), gl);

				if (Group.GroupType.fromInt(msg.arg1) == GroupType.CHATING) {
					for (Group g : gl) {
						GlobalHolder.getInstance().addConversation(
								new CrowdConversation(g));
					}
				}
				Intent gi = new Intent(JNI_BROADCAST_GROUP_NOTIFICATION);
				gi.addCategory(JNI_BROADCAST_CATEGROY);
				mContext.sendBroadcast(gi);
				break;

			case JNI_GROUP_USER_INFO_NOTIFICATION:
				GroupUserInfoOrig go = (GroupUserInfoOrig) msg.obj;
				if (go != null && go.xml != null) {
					List<User> lu = User.fromXml(go.xml);
					Group g = GlobalHolder.getInstance().findGroupById(go.gId);
					for (User tu : lu) {
						UserStatusObject uso =  GlobalHolder.getInstance()
								.getOnlineUserStatus(tu.getmUserId());
						//Update user status
						if (uso != null) {
							tu.updateStatus(User.Status.fromInt(uso.getStatus()));
							tu.setDeviceType(User.DeviceType.fromInt(uso.getDeviceType()));
						}
						User existU = GlobalHolder.getInstance().putUser(
								tu.getmUserId(), tu);
						if (existU.getmUserId() == GlobalHolder.getInstance()
								.getCurrentUserId()) {
							// Update logged user object.
							GlobalHolder.getInstance().setCurrentUser(existU);
						}
						if (g == null) {
							V2Log.e(" didn't find group information  " + go.gId);
						} else {
							g.addUserToGroup(existU);
						}
					}
					V2Log.w("  group:" + go.gId + "  user size:" + lu.size()
							+ "  " + g);
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
			case JNI_REMOTE_USER_DEVICE_INFO_NOTIFICATION:
				GlobalHolder.getInstance().addAttendeeDevice(
						UserDeviceConfig.parseFromXml((String) msg.obj));
				break;

			case JNI_RECEIVED_MESSAGE:
				VMessage vm = (VMessage) msg.obj;
				if (vm != null) {
					String action = null;
					String msgUri = null;
					// FIXME handle for image message
					if (vm.getMsgCode() == VMessage.VMESSAGE_CODE_CONF) {
						action = JNI_BROADCAST_NEW_CONF_MESSAGE;
					} else {
						action = JNI_BROADCAST_NEW_MESSAGE;
						Uri uri = saveMessageToDB(vm);
						msgUri = uri.getLastPathSegment();
						sendNotification();
						updateStatusBar(vm);
					}
					Intent ii = new Intent(action);
					ii.putExtra("content", vm.getText());
					ii.addCategory(JNI_BROADCAST_CATEGROY);
					ii.putExtra("gid", vm.mGroupId);
					ii.putExtra("mid", msgUri);
					ii.putExtra("fromuid", vm.getUser().getmUserId());
					mContext.sendBroadcast(ii);
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
			cv.put(ContentDescriptor.Messages.Cols.GROUP_ID, vm.mGroupId);
			Uri uri = getContentResolver().insert(
					ContentDescriptor.Messages.CONTENT_URI, cv);
			return uri;
		}

		private void sendNotification() {
			if ((System.currentTimeMillis() / 1000) - lastNotificatorTime > 2) {
				Uri notification = RingtoneManager
						.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
				Ringtone r = RingtoneManager.getRingtone(
						getApplicationContext(), notification);
				if (r != null) {
					r.play();
				}
				lastNotificatorTime = System.currentTimeMillis() / 1000;
			}
		}

		// FIXME update message for group message
		private void updateStatusBar(VMessage vm) {
			Conversation cov = null;
			if (vm.mGroupId != 0) {
				cov = GlobalHolder.getInstance().findConversationByType(
						Conversation.TYPE_GROUP, vm.mGroupId);
			} else {
				cov = GlobalHolder.getInstance().findConversationByType(
						Conversation.TYPE_CONTACT, vm.getUser().getmUserId());
			}
			//
			if ((GlobalHolder.getInstance().CURRENT_CONVERSATION != null && cov == GlobalHolder
					.getInstance().CURRENT_CONVERSATION)
					|| (GlobalHolder.getInstance().CURRENT_ID == vm.getUser()
							.getmUserId())) {
				return;
			}
			NotificationCompat.Builder builder = new NotificationCompat.Builder(
					mContext).setSmallIcon(R.drawable.ic_launcher)
					.setContentTitle(vm.getUser().getName());
			if (vm.getType() == MessageType.IMAGE) {
				builder.setContentText(mContext.getResources().getString(
						R.string.receive_image_notification));
			} else {
				builder.setContentText(vm.getText());

			}

			Intent resultIntent = new Intent(
					PublicIntent.START_CONVERSACTION_ACTIVITY);
			resultIntent.putExtra("user1id", GlobalHolder.getInstance()
					.getCurrentUserId());
			resultIntent.putExtra("user2id", vm.getUser().getmUserId());
			resultIntent.putExtra("user2Name", vm.getUser().getName());
			resultIntent.addCategory(PublicIntent.DEFAULT_CATEGORY);

			// Creates the PendingIntent
			PendingIntent notifyPendingIntent = PendingIntent.getActivities(
					mContext, 0, new Intent[] { resultIntent },
					PendingIntent.FLAG_UPDATE_CURRENT);

			// Puts the PendingIntent into the notification builder
			builder.setContentIntent(notifyPendingIntent);

			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			// mId allows you to update the notification later on.
			mNotificationManager.notify(PublicIntent.MESSAGE_NOTIFICATION_ID,
					builder.build());

		}

	}

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
			Notificator.cancelAllSystemNotification(mContext);
			//Send broadcast PREPARE_FINISH_APPLICATION first to let all activity quit and release resource
			//Notice: if any activity doesn't release resource, android will automatically restart main activity
			Intent i = new Intent();
			i.setAction(PublicIntent.PREPARE_FINISH_APPLICATION);
			i.addCategory(PublicIntent.DEFAULT_CATEGORY);
			mContext.sendBroadcast(i);
			
			mCallbackHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					GlobalConfig.saveLogoutFlag(mContext);

					Intent i = new Intent();
					i.setAction(PublicIntent.FINISH_APPLICATION);
					i.addCategory(PublicIntent.DEFAULT_CATEGORY);
					mContext.sendBroadcast(i);
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
		public void OnUserStatusUpdatedCallback(long nUserID, int type, int nStatus,
				String szStatusDesc) {
			UserStatusObject uso = new UserStatusObject(nUserID, type, nStatus);
			GlobalHolder.getInstance().updateUserStatus(nUserID, uso);
			User u = GlobalHolder.getInstance().getUser(nUserID);
			if (u == null) {
				V2Log.e("Can't update user status, user " + nUserID
						+ "  isn't exist");
			} else {
				u.updateStatus(User.Status.fromInt(nStatus));
				u.setDeviceType(User.DeviceType.fromInt(type));
			}
			
			
			Intent iun = new Intent(JNI_BROADCAST_USER_STATUS_NOTIFICATION);
			iun.addCategory(JNI_BROADCAST_CATEGROY);
			iun.putExtra("status", uso);
			mContext.sendBroadcast(iun);
			
		}

		@Override
		public void OnChangeAvatarCallback(int nAvatarType, long nUserID,
				String AvatarName) {
			File f = new File(AvatarName);
			if (f.isDirectory()) {
				// Do not notify if is not file;
				return;
			}
			// System default icon
			if (AvatarName.equals("Default.png")) {
				AvatarName = null;
			}

			
			GlobalHolder.getInstance().putAvatar(nUserID, AvatarName);

			Intent i = new Intent();
			i.addCategory(JNI_BROADCAST_CATEGROY);
			i.setAction(JNI_BROADCAST_USER_AVATAR_CHANGED_NOTIFICATION);
			i.putExtra("avatar", new UserAvatarObject(nUserID, AvatarName));
			sendBroadcast(i);
		}

		@Override
		public void OnModifyCommentName(long nUserId, String sCommmentName) {
			// TODO implment update user nick name
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

		@Override
		public void OnModifyGroupInfoCallback(int groupType, long nGroupID,
				String sXml) {
			if (groupType == GroupType.CONFERENCE.intValue()) {

			}

		}

		@Override
		public void OnInviteJoinGroupCallback(int groupType, String groupInfo,
				String userInfo, String additInfo) {
			GroupType gType = GroupType.fromInt(groupType);
			if (gType == GroupType.CONFERENCE) {
				Group g = ConferenceGroup
						.parseConferenceGroupFromXML(groupInfo);
				GroupRequest.getInstance().getGroupInfo(
						GroupType.CONFERENCE.intValue(), g.getmGId());
				String name = "";
				int pos = -1;
				if (userInfo != null && (pos = userInfo.indexOf("id='")) != -1) {
					int end = userInfo.indexOf("'", pos + 4);
					if (end != -1) {
						Long uid = Long.parseLong(userInfo.substring(pos + 4,
								end));
						User u = GlobalHolder.getInstance().getUser(uid);
						if (u != null) {
							name = u.getName();
						}
					}
				}
				if (g != null) {
					GlobalHolder.getInstance().addGroupToList(gType, g);
					if (gType == GroupType.CONFERENCE) {
						Intent i = new Intent();
						i.setAction(JNIService.JNI_BROADCAST_CONFERENCE_INVATITION);
						i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
						i.putExtra("gid", g.getmGId());
						sendBroadcast(i);
						Intent enterConference = new Intent(mContext,
								VideoActivityV2.class);
						enterConference.putExtra("gid", g.getmGId());
						Notificator.updateSystemNotification(mContext, name
								+ " 会议邀请:", g.getName(), 1, enterConference,
								PublicIntent.VIDEO_NOTIFICATION_ID);
					}

				}
			}
		}

		@Override
		public void OnDelGroupCallback(int groupType, long nGroupID,
				boolean bMovetoRoot) {
			// TODO just support conference
			if (groupType == Group.GroupType.CONFERENCE.intValue()) {
				String name = "";
				String gName = "";
				Group rG = GlobalHolder.getInstance().getGroupById(
						Group.GroupType.CONFERENCE, nGroupID);
				if (rG != null) {
					gName = rG.getName();
					if (rG.getOwnerUser() != null) {
						name = rG.getOwnerUser().getName();
					}
				} else {
					gName = nGroupID + "";
				}
				boolean flag = GlobalHolder.getInstance()
						.removeConferenceGroup(nGroupID);
				// If flag is true, mean current user dosn't remove this group
				// should notify
				// Otherwise this user removed this group should not notify
				if (flag) {
					Intent i = new Intent();
					i.setAction(JNIService.JNI_BROADCAST_CONFERENCE_REMOVED);
					i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
					i.putExtra("gid", nGroupID);
					sendBroadcast(i);
					Notificator.updateSystemNotification(mContext, "", gName
							+ "会议被删除", 1, PublicIntent.VIDEO_NOTIFICATION_ID);
				}
			}
		}

		@Override
		public void OnDelGroupUserCallback(int groupType, long nGroupID,
				long nUserID) {
			GlobalHolder.getInstance().removeGroupUser(nGroupID, nUserID);
			GroupUserObject obj = new GroupUserObject(groupType, nGroupID,
					nUserID);
			Intent i = new Intent();
			i.setAction(JNIService.JNI_BROADCAST_GROUP_USER_REMOVED);
			i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
			i.putExtra("obj", obj);
			sendBroadcast(i);
		}

		@Override
		public void OnAddGroupUserInfoCallback(int groupType, long nGroupID,
				String sXml) {
			if (sXml == null || sXml.isEmpty()) {
				V2Log.e("Incorrect user xml ");
				return;
			}
			int start = sXml.indexOf("id='");
			int end = sXml.indexOf("'", start + 4);
			String uidStr = sXml.substring(start + 4, end);
			long uid = 0;
			try {
				uid = Long.parseLong(uidStr);
			} catch (NumberFormatException e) {
				V2Log.e("Incorrect user id  " + sXml);
				return;
			}

			GlobalHolder.getInstance().addUserToGroup(
					GlobalHolder.getInstance().getUser(uid), nGroupID);
			GroupUserObject obj = new GroupUserObject(groupType, nGroupID, uid);
			Intent i = new Intent();
			i.setAction(JNIService.JNI_BROADCAST_GROUP_USER_ADDED);
			i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
			i.putExtra("obj", obj);
			sendBroadcast(i);
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

		}

		@Override
		public void OnConfMemberExitCallback(long nConfID, long nTime,
				long nUserID) {

		}

		@Override
		public void OnKickConfCallback(int nReason) {

		}

		@Override
		public void OnGrantPermissionCallback(long userid, int type, int status) {

		}

		@Override
		public void OnConfNotify(String confXml, String creatorXml) {
			Group g = ConferenceGroup.parseConferenceGroupFromXML(confXml);

			int start = confXml.indexOf("createuserid='");
			int end = confXml.indexOf("'", start + 14);

			if (start != -1 && end != -1) {
				long uid = 0;
				uid = Long.parseLong(confXml.substring(start + 14, end));
				if (uid > 0) {
					User u = GlobalHolder.getInstance().getUser(uid);
					g.setOwnerUser(u);
					GlobalHolder.getInstance().addGroupToList(
							Group.GroupType.CONFERENCE, g);
					Intent i = new Intent();
					i.setAction(JNIService.JNI_BROADCAST_CONFERENCE_INVATITION);
					i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
					i.putExtra("gid", g.getmGId());
					sendBroadcast(i);
					
					Intent enterConference = new Intent(mContext,
							VideoActivityV2.class);
					enterConference.putExtra("gid", g.getmGId());
					Notificator.updateSystemNotification(mContext, u.getName()
							+ " 会议邀请:", g.getName(), 1, enterConference,
							PublicIntent.VIDEO_NOTIFICATION_ID);

				} else {
					V2Log.e(" Incorrect uid : " + confXml);
				}
			}
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
			// if (nGroupID > 0) {
			// V2Log.w("igonre group message:" +nGroupID+"  "+ szXmlText);
			// return;
			// }

			User toUser = GlobalHolder.getInstance().getCurrentUser();
			User fromUser = GlobalHolder.getInstance().getUser(nFromUserID);
			if (toUser == null) {
				V2Log.w("No valid user object for receive message " + toUser
						+ "  " + fromUser);
				toUser = new User(GlobalHolder.getInstance().getCurrentUserId());
			}
			if (fromUser == null) {
				V2Log.w("No valid user object for receive message " + toUser
						+ "  " + fromUser);
				fromUser = new User(nFromUserID);
			}

			// Record image data meta
			List<VMessage> l = VImageMessage.extraMetaFrom(fromUser, toUser,
					szXmlText);
			synchronized (cacheImageMeta) {
				cacheImageMeta.addAll(l);
			}
			VMessage vm = VMessage.fromXml(fromUser, toUser, new Date(),
					szXmlText);
			if (vm == null) {
				V2Log.e(" xml parsed failed : " + szXmlText);
				return;
			}
			vm.mGroupId = nGroupID;
			vm.setMsgCode(nBusinessType);
			Message.obtain(mCallbackHandler, JNI_RECEIVED_MESSAGE, vm)
					.sendToTarget();
		}

		@Override
		public void OnRecvChatPictureCallback(long nGroupID, int nBusinessType,
				long nFromUserID, long nTime, String nSeqId, byte[] pPicData) {
			boolean isCache = false;
			VMessage vm = null;
			synchronized (cacheImageMeta) {
				for (VMessage v : cacheImageMeta) {
					if (v.getUUID().equals(
							nSeqId.subSequence(1, nSeqId.length() - 1))) {
						vm = v;
						isCache = true;
						cacheImageMeta.remove(v);
						break;
					}
				}
			}
			if (isCache == false) {
				V2Log.e(" Didn't receive image meta data: " + nSeqId);
				return;
			}
			if (nGroupID > 0) {
				V2Log.w("igonre group image message:" + nGroupID);
				return;
			}
			User toUser = GlobalHolder.getInstance().getCurrentUser();
			User fromUser = GlobalHolder.getInstance().getUser(nFromUserID);
			if (toUser == null) {
				V2Log.w("No valid user object for receive message " + toUser
						+ "  " + fromUser);
				toUser = new User(GlobalHolder.getInstance().getCurrentUserId());
			}
			if (fromUser == null) {
				V2Log.w("No valid user object for receive message " + toUser
						+ "  " + fromUser);
				fromUser = new User(nFromUserID);
			}
			vm.setLocal(false);
			vm.setDate(new Date());
			vm.mGroupId = nGroupID;
			((VImageMessage) vm).updateImageData(pPicData);
			Message.obtain(mCallbackHandler, JNI_RECEIVED_MESSAGE, vm)
					.sendToTarget();
		}

	}

}
