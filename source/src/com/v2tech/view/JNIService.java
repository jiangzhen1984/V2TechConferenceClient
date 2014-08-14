package com.v2tech.view;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Service;
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
import android.widget.Toast;

import com.V2.jni.AudioRequest;
import com.V2.jni.AudioRequestCallbackAdapter;
import com.V2.jni.ChatRequest;
import com.V2.jni.ChatRequestCallbackAdapter;
import com.V2.jni.ConfRequest;
import com.V2.jni.ConfRequestCallbackAdapter;
import com.V2.jni.FileRequest;
import com.V2.jni.FileRequestCallbackAdapter;
import com.V2.jni.GroupRequest;
import com.V2.jni.GroupRequestCallback;
import com.V2.jni.ImRequest;
import com.V2.jni.ImRequestCallback;
import com.V2.jni.ImRequestCallbackAdapter;
import com.V2.jni.V2GlobalEnum;
import com.V2.jni.VideoRequest;
import com.V2.jni.VideoRequestCallbackAdapter;
import com.V2.jni.ind.AudioJNIObjectInd;
import com.V2.jni.ind.SendingResultJNIObjectInd;
import com.V2.jni.ind.VideoJNIObjectInd;
import com.v2tech.R;
import com.v2tech.service.BitmapManager;
import com.v2tech.service.GlobalHolder;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.Notificator;
import com.v2tech.util.V2Log;
import com.v2tech.util.XmlParser;
import com.v2tech.view.bo.GroupUserObject;
import com.v2tech.view.bo.UserAvatarObject;
import com.v2tech.view.bo.UserStatusObject;
import com.v2tech.view.conversation.MessageBuilder;
import com.v2tech.vo.ConferenceGroup;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.NetworkStateCode;
import com.v2tech.vo.User;
import com.v2tech.vo.UserDeviceConfig;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.VMessageAbstractItem;
import com.v2tech.vo.VMessageAudioItem;
import com.v2tech.vo.VMessageFileItem;
import com.v2tech.vo.VMessageImageItem;

/**
 * This service is used to wrap JNI call.<br>
 * JNI calls are asynchronous, we don't expect activity involve JNI.<br>
 * 
 * @author 28851274
 * 
 */
public class JNIService extends Service {

	public static final String JNI_BROADCAST_CATEGROY = "com.v2tech.jni.broadcast";
	public static final String JNI_BROADCAST_CONNECT_STATE_NOTIFICATION = "com.v2tech.jni.broadcast.connect_state_notification";
	public static final String JNI_BROADCAST_USER_STATUS_NOTIFICATION = "com.v2tech.jni.broadcast.user_stauts_notification";

	/**
	 * Notify user avatar changed, notice please do not listen this broadcast if
	 * you are UI. Use
	 * {@link BitmapManager#registerBitmapChangedListener(com.v2tech.service.BitmapManager.BitmapChangedListener)}
	 * to listener bitmap change if you are UI.
	 */
	public static final String JNI_BROADCAST_USER_AVATAR_CHANGED_NOTIFICATION = "com.v2tech.jni.broadcast.user_avatar_notification";
	public static final String JNI_BROADCAST_USER_UPDATE_NAME_OR_SIGNATURE = "com.v2tech.jni.broadcast.user_update_sigature";
	public static final String JNI_BROADCAST_GROUP_NOTIFICATION = "com.v2tech.jni.broadcast.group_geted";
	public static final String JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION = "com.v2tech.jni.broadcast.group_user_updated";
	public static final String JNI_BROADCAST_NEW_MESSAGE = "com.v2tech.jni.broadcast.new.message";
	public static final String JNI_BROADCAST_MESSAGE_SENT_FAILED = "com.v2tech.jni.broadcast.message_sent_failed";
	public static final String JNI_BROADCAST_NEW_CONF_MESSAGE = "com.v2tech.jni.broadcast.new.conf.message";
	public static final String JNI_BROADCAST_CONFERENCE_INVATITION = "com.v2tech.jni.broadcast.conference_invatition_new";
	public static final String JNI_BROADCAST_CONFERENCE_REMOVED = "com.v2tech.jni.broadcast.conference_removed";
	public static final String JNI_BROADCAST_GROUP_USER_REMOVED = "com.v2tech.jni.broadcast.group_user_removed";
	public static final String JNI_BROADCAST_GROUP_USER_ADDED = "com.v2tech.jni.broadcast.group_user_added";
	public static final String JNI_BROADCAST_GROUP_INVATITION = "com.v2tech.jni.broadcast.group_invatition_new";

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

	private AudioRequestCB mARCB;

	private FileRequestCB mFRCB;

	// ////////////////////////////////////////

	private Context mContext;

	private List<VMessage> cacheImageMeta = new ArrayList<VMessage>();
	private List<VMessage> cacheAudioMeta = new ArrayList<VMessage>();

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
		ImRequest.getInstance(this.getApplicationContext()).addCallback(mImCB);

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

		mARCB = new AudioRequestCB();
		AudioRequest.getInstance().addCallback(mARCB);

		mFRCB = new FileRequestCB(mCallbackHandler);
		FileRequest.getInstance().addCallback(mFRCB);
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
	private static final int JNI_CONFERENCE_INVITATION = 61;
	private static final int JNI_RECEIVED_MESSAGE = 91;
	private static final int JNI_RECEIVED_VIDEO_INVITION = 92;
	private static final int JNI_GROUP_INVITATION = 93;

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

				if (gl != null && gl.size() > 0) {
					GlobalHolder.getInstance().updateGroupList(
							Group.GroupType.fromInt(msg.arg1), gl);
					Intent gi = new Intent(JNI_BROADCAST_GROUP_NOTIFICATION);
					gi.putExtra("gtype", msg.arg1);
					gi.addCategory(JNI_BROADCAST_CATEGROY);
					mContext.sendBroadcast(gi);
				}
				break;

			case JNI_GROUP_USER_INFO_NOTIFICATION:
				GroupUserInfoOrig go = (GroupUserInfoOrig) msg.obj;
				if (go != null && go.xml != null) {
					List<User> lu = User.fromXml(go.xml);
					Group g = GlobalHolder.getInstance().findGroupById(go.gId);
					for (User tu : lu) {
						UserStatusObject uso = GlobalHolder.getInstance()
								.getOnlineUserStatus(tu.getmUserId());
						// Update user status
						if (uso != null) {
							tu.updateStatus(User.Status.fromInt(uso.getStatus()));
							tu.setDeviceType(User.DeviceType.fromInt(uso
									.getDeviceType()));
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
			case JNI_CONFERENCE_INVITATION:
				Group g = (Group) msg.obj;
				Group cache = GlobalHolder.getInstance().getGroupById(
						GroupType.CONFERENCE, g.getmGId());
				// conference already in cache list
				if (cache != null && g.getmGId() != 0) {
					V2Log.i("Current user conference in group:"
							+ cache.getName() + "  " + cache.getmGId());
					return;
				}
				GroupRequest.getInstance().getGroupInfo(
						GroupType.CONFERENCE.intValue(), g.getmGId());
				if (g != null) {
					GlobalHolder.getInstance().addGroupToList(
							GroupType.CONFERENCE, g);
					Intent i = new Intent();
					i.setAction(JNIService.JNI_BROADCAST_CONFERENCE_INVATITION);
					i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
					i.putExtra("gid", g.getmGId());
					sendBroadcast(i);
				}
				break;
			case JNI_RECEIVED_MESSAGE:
				VMessage vm = (VMessage) msg.obj; 
				if (vm != null) {
					String action = null;
					MessageBuilder.saveMessage(mContext, vm);

					if (vm.getMsgCode() == V2GlobalEnum.REQUEST_TYPE_CONF) {
						action = JNI_BROADCAST_NEW_CONF_MESSAGE;
					} else {
						action = JNI_BROADCAST_NEW_MESSAGE;
						sendNotification();
					}

					Intent ii = new Intent(action);
					ii.addCategory(JNI_BROADCAST_CATEGROY);
					ii.putExtra("mid", vm.getId());
					ii.putExtra("gm", vm.getGroupId() != 0);
					// Send ordered broadcast, make sure conversationview
					// receive message first
					mContext.sendOrderedBroadcast(ii, null);
				}
				break;
			case JNI_RECEIVED_VIDEO_INVITION:
				VideoJNIObjectInd vjoi = (VideoJNIObjectInd) msg.obj;
				Intent iv = new Intent();
				iv.addCategory(PublicIntent.DEFAULT_CATEGORY);
				iv.setAction(PublicIntent.START_P2P_CONVERSACTION_ACTIVITY);
				iv.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				iv.putExtra("uid", vjoi.getFromUserId());
				iv.putExtra("is_coming_call", true);
				iv.putExtra("voice", false);
				iv.putExtra("device", vjoi.getDeviceId());
				mContext.startActivity(iv);
				break;
			case JNI_GROUP_INVITATION:
				long groupId = (Long) msg.obj;
				Intent i = new Intent();
				i.setAction(JNIService.JNI_BROADCAST_GROUP_INVATITION);
				i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
				i.putExtra("gid", groupId);
				sendBroadcast(i);
				break;

			}

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

	}

	class ImRequestCB extends ImRequestCallbackAdapter {

		private JNICallbackHandler mCallbackHandler;

		public ImRequestCB(JNICallbackHandler mCallbackHandler) {
			this.mCallbackHandler = mCallbackHandler;
		}

		@Override
		public void OnLoginCallback(long nUserID, int nStatus, int nResult) {
		}

		@Override
		public void OnLogoutCallback(int nUserID) {
			// FIXME optimize code
			Message.obtain(mCallbackHandler, JNI_LOG_OUT).sendToTarget();
			Notificator.cancelAllSystemNotification(mContext);
			// Send broadcast PREPARE_FINISH_APPLICATION first to let all
			// activity quit and release resource
			// Notice: if any activity doesn't release resource, android will
			// automatically restart main activity
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
		public void OnUserStatusUpdatedCallback(long nUserID, int type,
				int nStatus, String szStatusDesc) {
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
		public void OnCreateCrowdCallback(String sCrowdXml, int nResult) {

		}

	}

	class GroupRequestCB implements GroupRequestCallback {
		private static final String TAG = "GroupRequestCB";
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
				if (g != null) {
					// Send message for synchronization
					Message.obtain(mCallbackHandler, JNI_CONFERENCE_INVITATION,
							g).sendToTarget();
				}
			} else if (gType == GroupType.CHATING) {
				//TODO just accept automatically
				Group g = CrowdGroup.parseXml(groupInfo , userInfo);
				GlobalHolder.getInstance().addGroupToList(GroupType.CHATING, g);
				GroupRequest.getInstance().acceptInviteJoinGroup(groupType,
						g.getmGId(),
						GlobalHolder.getInstance().getCurrentUserId());
				Message.obtain(mCallbackHandler, JNI_GROUP_INVITATION , g.getmGId()).sendToTarget(); 
			}
		}

		@Override
		public void OnDelGroupCallback(int groupType, long nGroupID,
				boolean bMovetoRoot) {
			// TODO just support conference
			if (groupType == Group.GroupType.CONFERENCE.intValue()) {
				String gName = "";
				Group rG = GlobalHolder.getInstance().getGroupById(
						Group.GroupType.CONFERENCE, nGroupID);
				if (rG != null) {
					gName = rG.getName();
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
					Notificator
							.updateSystemNotification(
									mContext,
									"",
									gName
											+ mContext
													.getText(R.string.confs_is_deleted_notification),
									1, PublicIntent.VIDEO_NOTIFICATION_ID);
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

			V2Log.e(TAG, "get friends :" + GlobalHolder.getInstance().getUser(uid).getArra());
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

	class AudioRequestCB extends AudioRequestCallbackAdapter {

		@Override
		public void OnAudioChatInvite(AudioJNIObjectInd ind) {
			// FIXME if in video automatically accept audio.
			// because audio and video use different message
			// Need to handle other user audio call
			if (GlobalHolder.getInstance().isInVideoCall()) {
				AudioRequest.getInstance().AcceptAudioChat(ind.getGroupId(),
						ind.getFromUserId(), V2GlobalEnum.REQUEST_TYPE_IM);
				return;
			}

			if (GlobalHolder.getInstance().isInMeeting()
					|| GlobalHolder.getInstance().isInAudioCall()
					|| GlobalHolder.getInstance().isInVideoCall()) {
				V2Log.i("Ignore audio call ");
				AudioRequest.getInstance().RefuseAudioChat(ind.getGroupId(),
						ind.getFromUserId(), (int) ind.getRequestType());
				return;
			}

			Intent iv = new Intent();
			iv.addCategory(PublicIntent.DEFAULT_CATEGORY);
			iv.setAction(PublicIntent.START_P2P_CONVERSACTION_ACTIVITY);
			iv.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			iv.putExtra("uid", ind.getFromUserId());
			iv.putExtra("is_coming_call", true);
			iv.putExtra("voice", true);
			mContext.startActivity(iv);

		}

	}

	class VideoRequestCB extends VideoRequestCallbackAdapter {

		private JNICallbackHandler mCallbackHandler;

		public VideoRequestCB(JNICallbackHandler mCallbackHandler) {
			this.mCallbackHandler = mCallbackHandler;
		}

		@Override
		public void OnVideoChatInviteCallback(VideoJNIObjectInd ind) {
			if (GlobalHolder.getInstance().isInMeeting()
					|| GlobalHolder.getInstance().isInAudioCall()
					|| GlobalHolder.getInstance().isInVideoCall()) {
				V2Log.i("Ignore video call ");
				VideoRequest.getInstance().refuseVideoChat(ind.getGroupId(),
						ind.getFromUserId(), ind.getDeviceId(),
						ind.getRequestType());
				return;
			}
			Message.obtain(mCallbackHandler, JNI_RECEIVED_VIDEO_INVITION, ind)
					.sendToTarget();
		}

		@Override
		public void OnRemoteUserVideoDevice(long uid, String szXmlData) {
			if (szXmlData == null) {
				V2Log.e(" No avaiable user device configuration");
				return;
			}
			List<UserDeviceConfig> ll = UserDeviceConfig.parseFromXml(uid,
					szXmlData);
			GlobalHolder.getInstance().updateUserDevice(uid, ll);
		}

	}

	class ConfRequestCB extends ConfRequestCallbackAdapter {

		public ConfRequestCB(JNICallbackHandler mCallbackHandler) {
		}

		@Override
		public void OnConfNotify(String confXml, String creatorXml) {
			Group g = ConferenceGroup.parseConferenceGroupFromXML(confXml);
			V2Log.e("==================notification============"+creatorXml);
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

				} else {
					V2Log.e(" Incorrect uid : " + confXml);
				}
			}
		}

	}

	class ChatRequestCB extends ChatRequestCallbackAdapter {

		public ChatRequestCB(JNICallbackHandler mCallbackHandler) {
		}

		@Override
		public void OnRecvChatTextCallback(long nGroupID, int nBusinessType,
				long nFromUserID, long nTime, String szXmlText) {
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
			// VMessage cache = new VMessage(fromUser, toUser, new Date());
			// cache.setMsgCode(nBusinessType);
			// XmlParser.extraImageMetaFrom(cache, szXmlText);
			// if (cache.getItems().size() > 0) {
			// synchronized (cacheImageMeta) {
			// cacheImageMeta.add(cache);
			// }
			// }

			// Record audio data meta
			VMessage cacheAudio = new VMessage(fromUser, toUser, new Date());
			cacheAudio.setMsgCode(nBusinessType);
			XmlParser.extraAudioMetaFrom(cacheAudio, szXmlText);
			if (cacheAudio.getItems().size() > 0) {
				synchronized (cacheAudioMeta) {
					cacheAudioMeta.add(cacheAudio);
				}
			}

			VMessage vm = XmlParser.parseForMessage(fromUser, toUser,
					new Date(), szXmlText);
			vm.setGroupId(nGroupID);
			vm.setMsgCode(nBusinessType);
			if (vm == null || vm.getItems().size() == 0) {
				return;
			}

			if (vm.getImageItems().size() > 0) {
				synchronized (cacheImageMeta) {
					cacheImageMeta.add(vm);
				}
				return;
			}

			Message.obtain(mCallbackHandler, JNI_RECEIVED_MESSAGE, vm)
					.sendToTarget();
		}

		@Override
		public void OnRecvChatPictureCallback(long nGroupID, int nBusinessType,
				long nFromUserID, long nTime, String nSeqId, byte[] pPicData) {
			boolean isCache = false;
			VMessage vm = null;
			String uuid = nSeqId;
			synchronized (cacheImageMeta) {
				for (VMessage v : cacheImageMeta) {
					List<VMessageImageItem> items = v.getImageItems();
					int receivedCount = 0;
					for (int i = 0; i < items.size(); i++) {
						VMessageImageItem vai = items.get(i);

						VMessageImageItem vait = (VMessageImageItem) vai;
						if (vait.isReceived()) {
							receivedCount++;
							continue;
						}
						if (vait.getUUID().equals(uuid)) {
							receivedCount++;
							vm = v;

							String filePath = GlobalConfig.getGlobalPicsPath()
									+ "/" + vait.getUUID()
									+ vait.getExtension();
							vait.setFilePath(filePath);

							File f = new File(filePath);
							OutputStream os = null;
							try {
								os = new FileOutputStream(f);
								os.write(pPicData, 0, pPicData.length);
							} catch (FileNotFoundException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							} finally {
								if (os != null) {
									try {
										os.close();
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
							}

							vait.setReceived(true);
							continue;
						}
					}

					if (receivedCount == items.size()) {
						cacheImageMeta.remove(v);
						isCache = true;
						break;
					}
				}
			}
			if (isCache == false) {
				V2Log.e(" Didn't receive image meta data: " + nSeqId);
				return;
			}

			vm.setGroupId(nGroupID);

			Message.obtain(mCallbackHandler, JNI_RECEIVED_MESSAGE, vm)
					.sendToTarget();
		}

		@Override
		public void OnRecvChatAudio(long gid, int businessType,
				long fromUserId, long timeStamp, String messageId,
				String audioPath) {
			VMessage vm = null;
			synchronized (cacheAudioMeta) {
				for (VMessage v : cacheAudioMeta) {
					List<VMessageAudioItem> list = v.getAudioItems();
					for (int i = 0; i < list.size(); i++) {
						VMessageAudioItem item = list.get(i);
						if (item.getUuid().equals(messageId)) {
							item.setAudioFilePath(audioPath);
							item.setState(VMessageAbstractItem.STATE_UNREAD);
							cacheAudioMeta.remove(item);
							vm = v;
							break;
						}
					}
					if (vm != null) {
						break;
					}
				}

			}

			if (vm != null) {
				vm.setGroupId(gid);
				Message.obtain(mCallbackHandler, JNI_RECEIVED_MESSAGE, vm)
						.sendToTarget();
			} else {
				V2Log.e(" Didn't find audio item : " + messageId);
			}

		}

		@Override
		public void OnSendChatResult(SendingResultJNIObjectInd ind) {
			if (ind.getRet() == SendingResultJNIObjectInd.Result.FAILED) {
				MessageBuilder.updateVMessageItemToSentFalied(mContext,
						ind.getUuid());
				Intent i = new Intent();
				i.setAction(JNIService.JNI_BROADCAST_MESSAGE_SENT_FAILED);
				i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
				i.putExtra("uuid", ind.getUuid());
				sendBroadcast(i);
			}

		}

	}

	class FileRequestCB extends FileRequestCallbackAdapter {

		private JNICallbackHandler mCallbackHandler;

		public FileRequestCB(JNICallbackHandler mCallbackHandler) {
			this.mCallbackHandler = mCallbackHandler;
		}

		@Override
		public void OnFileTransInvite(long nGroupID, int nBusinessType,
				long userid, String szFileID, String szFileName,
				long nFileBytes, int linetype) {
			User fromUser = GlobalHolder.getInstance().getUser(userid);
			// If doesn't receive user information from server side,
			// construct new user object
			if (fromUser == null) {
				fromUser = new User(userid);
			}

			VMessage vm = new VMessage(nGroupID, fromUser, GlobalHolder
					.getInstance().getCurrentUser());
			int pos = szFileName.lastIndexOf("/");
			VMessageFileItem vfi = new VMessageFileItem(vm, szFileID,
					pos == -1 ? szFileName : szFileName.substring(pos + 1));
			vfi.setState(VMessageFileItem.STATE_FILE_UNDOWNLOAD);
			vfi.setFileSize(nFileBytes);
			Message.obtain(mCallbackHandler, JNI_RECEIVED_MESSAGE, vm)
					.sendToTarget();
		}

	}

}
