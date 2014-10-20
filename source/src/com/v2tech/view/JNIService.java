package com.v2tech.view;

import java.io.File;
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
import android.support.v4.util.LongSparseArray;
import android.util.Log;
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
import com.V2.jni.GroupRequestCallbackAdapter;
import com.V2.jni.ImRequest;
import com.V2.jni.ImRequestCallback;
import com.V2.jni.ImRequestCallbackAdapter;
import com.V2.jni.V2GlobalEnum;
import com.V2.jni.VideoRequest;
import com.V2.jni.VideoRequestCallbackAdapter;
import com.V2.jni.ind.AudioJNIObjectInd;
import com.V2.jni.ind.FileJNIObject;
import com.V2.jni.ind.SendingResultJNIObjectInd;
import com.V2.jni.ind.V2Conference;
import com.V2.jni.ind.V2Group;
import com.V2.jni.ind.V2User;
import com.V2.jni.ind.VideoJNIObjectInd;
import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.service.BitmapManager;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.jni.FileDownLoadErrorIndication;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.GlobalState;
import com.v2tech.util.Notificator;
import com.v2tech.util.XmlParser;
import com.v2tech.view.bo.GroupUserObject;
import com.v2tech.view.bo.UserAvatarObject;
import com.v2tech.view.bo.UserStatusObject;
import com.v2tech.view.contacts.add.AddFriendHistroysHandler;
import com.v2tech.view.conversation.MessageBuilder;
import com.v2tech.view.conversation.MessageLoader;
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
import com.v2tech.vo.VMessageQualification;
import com.v2tech.vo.VMessageQualificationApplicationCrowd;
import com.v2tech.vo.VMessageQualificationInvitationCrowd;

/**
 * This service is used to wrap JNI call.<br>
 * JNI calls are asynchronous, we don't expect activity involve JNI.<br>
 * 
 * @author 28851274
 * 
 */
public class JNIService extends Service {
	private static final String TAG = "JNIService";
	public static final int BINARY_TYPE_AUDIO = 3;
	public static final int BINARY_TYPE_IMAGE = 2;

	public static final String JNI_BROADCAST_CATEGROY = "com.v2tech.jni.broadcast";
	public static final String JNI_ACTIVITY_CATEGROY = "com.v2tech";
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
	public static final String JNI_BROADCAST_GROUP_UPDATED = "com.v2tech.jni.broadcast.group_updated";
	public static final String JNI_BROADCAST_NEW_MESSAGE = "com.v2tech.jni.broadcast.new.message";
	public static final String JNI_BROADCAST_MESSAGE_SENT_FAILED = "com.v2tech.jni.broadcast.message_sent_failed";
	public static final String JNI_BROADCAST_NEW_CONF_MESSAGE = "com.v2tech.jni.broadcast.new.conf.message";
	public static final String JNI_BROADCAST_CONFERENCE_INVATITION = "com.v2tech.jni.broadcast.conference_invatition_new";
	public static final String JNI_BROADCAST_CONFERENCE_REMOVED = "com.v2tech.jni.broadcast.conference_removed";
	public static final String JNI_BROADCAST_GROUP_USER_REMOVED = "com.v2tech.jni.broadcast.group_user_removed";
	/**
	 * key crowd : crowdId
	 */
	public static final String JNI_BROADCAST_NEW_CROWD = "com.v2tech.jni.broadcast.new_crowd";
	public static final String JNI_BROADCAST_GROUP_USER_ADDED = "com.v2tech.jni.broadcast.group_user_added";
	public static final String JNI_BROADCAST_VIDEO_CALL_CLOSED = "com.v2tech.jni.broadcast.video_call_closed";
	public static final String JNI_BROADCAST_FRIEND_AUTHENTICATION = "com.v2tech.jni.broadcast.friend_authentication";
	public static final String JNI_BROADCAST_NEW_QUALIFICATION_MESSAGE = "com.v2tech.jni.broadcast.new.qualification_message";
	public static final String BROADCAST_CROWD_NEW_UPLOAD_FILE_NOTIFICATION = "com.v2tech.jni.broadcast.new.upload_crowd_file_message";
	/**
	 * Current user kicked by crowd master key crowd : crowdId
	 */
	public static final String JNI_BROADCAST_KICED_CROWD = "com.v2tech.jni.broadcast.kick_crowd";

	/**
	 * Crowd invitation with key crowd
	 */
	public static final String JNI_BROADCAST_CROWD_INVATITION = "com.v2tech.jni.broadcast.crowd_invatition";

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

		HandlerThread callback = new HandlerThread("JNI-Callbck");
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

		if (code != NetworkStateCode.CONNECTED) {
			((MainApplication) getApplication()).netWordIsConnected = false;
		} else {
			((MainApplication) getApplication()).netWordIsConnected = true;
		}

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
				List<V2Group> gl = (List<V2Group>) msg.obj;

				if (gl != null && gl.size() > 0) {
					GlobalHolder.getInstance().updateGroupList(msg.arg1, gl);
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
					Group group = GlobalHolder.getInstance().findGroupById(
							go.gId);
					for (User tu : lu) {
						User existU = GlobalHolder.getInstance().putUser(
								tu.getmUserId(), tu);
						if (existU.getmUserId() == GlobalHolder.getInstance()
								.getCurrentUserId()) {
							// Update logged user object.
							GlobalHolder.getInstance().setCurrentUser(existU);
						}

						UserStatusObject userStatusObject = onLineUsers.get(tu
								.getmUserId());
						if (userStatusObject != null) {
							existU.updateStatus(User.Status
									.fromInt(userStatusObject.getStatus()));
							existU.setDeviceType(User.DeviceType
									.fromInt(userStatusObject.getDeviceType()));
						}

						if (group == null) {
							V2Log.e(" didn't find group information  " + go.gId);
						} else {
							group.addUserToGroup(existU);
						}
					}
					V2Log.w("  group:" + go.gId + "  user size:" + lu.size()
							+ "  " + group);
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
						GroupType.CONFERENCE.intValue(), g.getmGId());
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
							GroupType.CONFERENCE.intValue(), g);
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
					vm.setReadState(VMessage.STATE_UNREAD);
					MessageBuilder.saveBinaryVMessage(mContext, vm);
					MessageBuilder.saveFileVMessage(mContext, vm);
					MessageBuilder.saveMessage(mContext, vm);
					Long id = MessageLoader.queryVMessageID(mContext, vm);
					if (id == null) {
						V2Log.e("the message :" + vm.getUUID()
								+ " save in databases is failed ....");
						return;
					}
					vm.setId(id);
					if (vm.getMsgCode() == V2GlobalEnum.GROUP_TYPE_CONFERENCE) {
						action = JNI_BROADCAST_NEW_CONF_MESSAGE;
					} else {
						action = JNI_BROADCAST_NEW_MESSAGE;
						sendNotification();
					}

					Intent ii = new Intent(action);
					ii.addCategory(JNI_BROADCAST_CATEGROY);
					ii.putExtra("mid", vm.getId());
					ii.putExtra("groupID", vm.getGroupId());
					ii.putExtra("groupType", vm.getMsgCode());
					ii.putExtra("remoteUserID", vm.getFromUser().getmUserId());
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
				iv.putExtra("sessionID", vjoi.getSzSessionID());
				iv.putExtra("is_coming_call", true);
				iv.putExtra("voice", false);
				iv.putExtra("device", vjoi.getDeviceId());
				mContext.startActivity(iv);
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

	private LongSparseArray<UserStatusObject> onLineUsers = new LongSparseArray<UserStatusObject>();

	class ImRequestCB extends ImRequestCallbackAdapter {

		private JNICallbackHandler mCallbackHandler;

		public ImRequestCB(JNICallbackHandler mCallbackHandler) {
			this.mCallbackHandler = mCallbackHandler;
		}

		@Override
		public void OnLoginCallback(long nUserID, int nStatus, int nResult,
				long serverTime) {
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
			User u = GlobalHolder.getInstance().getUser(nUserID);
			if (u == null) {
				V2Log.e("Can't update user status, user " + nUserID
						+ "  isn't exist");
				onLineUsers.put(nUserID, uso);
			} else {
				V2Log.e(TAG, "the " + u.getArra()
						+ " user is updating state...." + nStatus);
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

	}

	class GroupRequestCB extends GroupRequestCallbackAdapter {
		private static final String TAG = "GroupRequestCB";
		private JNICallbackHandler mCallbackHandler;

		public GroupRequestCB(JNICallbackHandler mCallbackHandler) {
			this.mCallbackHandler = mCallbackHandler;
		}

		@Override
		public void OnGetGroupInfoCallback(int groupType, List<V2Group> list) {
			Message.obtain(mCallbackHandler, JNI_GROUP_NOTIFY, groupType, 0,
					list).sendToTarget();
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
		public void OnModifyGroupInfoCallback(V2Group group) {
			if (group == null) {
				return;
			}
			if (group.type == GroupType.CONFERENCE.intValue()) {

			} else if (group.type == GroupType.CHATING.intValue()) {
				CrowdGroup cg = (CrowdGroup) GlobalHolder.getInstance()
						.getGroupById(group.id);
				cg.setAnnouncement(group.announce);
				cg.setBrief(group.brief);
				cg.setAuthType(CrowdGroup.AuthType.fromInt(group.authType));
			}

			// Send broadcast
			Intent i = new Intent(JNI_BROADCAST_GROUP_UPDATED);
			i.addCategory(JNI_BROADCAST_CATEGROY);
			i.putExtra("gid", group.id);
			mContext.sendBroadcast(i);

		}

		@Override
		public void onAddGroupInfo(V2Group group) {
			// if (group == null || group.creator == null) {
			// return;
			// }
			// GroupType gType = GroupType.fromInt(group.type);
			// if (gType == GroupType.CHATING) {
			// CrowdGroup cg = convertCrowd(group);
			// GlobalHolder.getInstance().addGroupToList(gType.intValue(), cg);
			//
			// // Send broadcast
			// Intent i = new Intent(JNI_BROADCAST_NEW_CROWD);
			// i.addCategory(JNI_BROADCAST_CATEGROY);
			// i.putExtra("crowd", cg.getmGId());
			// mContext.sendBroadcast(i);
			// }

		}



		@Override
		public void OnApplyJoinGroup(V2Group group, V2User user, String reason) {
			if (group == null || user == null) {
				return;
			}
			CrowdGroup cg = (CrowdGroup) GlobalHolder.getInstance()
					.getGroupById(group.id);
			User u = GlobalHolder.getInstance().getUser(user.uid);
			if (cg == null || u == null) {
				return;
			}
			checkMessageAndSendBroadcast(
					VMessageQualification.Type.CROWD_APPLICATION, cg, u, reason);

		}

		@Override
		public void OnInviteJoinGroupCallback(V2Group group) {
			if (group == null) {
				V2Log.e(" invitation group is null");
				return;
			}

			GroupType gType = GroupType.fromInt(group.type);

			if (gType == GroupType.CONFERENCE) {
				User owner = GlobalHolder.getInstance()
						.getUser(group.owner.uid);
				if (owner == null)
					V2Log.e("get create conference man is null , owner id is :"
							+ group.owner.uid);
				User chairMan = GlobalHolder.getInstance().getUser(
						group.chairMan.uid);
				if (owner == null)
					V2Log.e("get create conference man is null , chairMan id is :"
							+ group.owner.uid);

				ConferenceGroup g = new ConferenceGroup(group.id, group.name,
						owner, group.createTime, chairMan);
				Message.obtain(mCallbackHandler, JNI_CONFERENCE_INVITATION, g)
						.sendToTarget();
			} else if (gType == GroupType.CHATING) {
				User owner = GlobalHolder.getInstance().getUser(
						group.creator.uid);
				if (owner.isDirty()) {
					owner.setName(group.creator.name);
				}

				CrowdGroup cg = new CrowdGroup(group.id, group.name, owner);
				cg.setAuthType(CrowdGroup.AuthType.fromInt(group.authType));

				checkMessageAndSendBroadcast(
						VMessageQualification.Type.CROWD_INVITATION, cg,
						GlobalHolder.getInstance().getCurrentUser(), null);

			} else if (gType == GroupType.CONTACT) {
			}
		}

		private VMessageQualification checkMessageAndSendBroadcast(
				VMessageQualification.Type type, CrowdGroup g, User user, String reason) {
			boolean sendBroadcast = true;
			VMessageQualification crowdMsg = MessageBuilder
					.queryQualMessageByCrowdId(mContext, user, g);
			if (crowdMsg != null) {
				if (crowdMsg.getQualState() != VMessageQualification.QualificationState.WAITING) {
					crowdMsg.setReadState(VMessageQualification.ReadState.UNREAD);
					crowdMsg.setQualState(VMessageQualification.QualificationState.WAITING);
					MessageBuilder.updateQualicationMessage(mContext, crowdMsg);
				} else {
					sendBroadcast = false;
				}
			} else {
				// Save message to database
				if (type == VMessageQualification.Type.CROWD_APPLICATION) {
					crowdMsg = new VMessageQualificationApplicationCrowd(g,
							user);
					((VMessageQualificationApplicationCrowd)crowdMsg).setApplyReason(reason);
				} else if (type == VMessageQualification.Type.CROWD_INVITATION) {
					crowdMsg = new VMessageQualificationInvitationCrowd(g,
							GlobalHolder.getInstance().getCurrentUser());
				} else {
					throw new RuntimeException("Unkown type");
				}
				crowdMsg.setReadState(VMessageQualification.ReadState.UNREAD);
				Uri uri = MessageBuilder.saveQualicationMessage(mContext,
						crowdMsg);
				if (uri != null) {
					crowdMsg.setId(Long.parseLong(uri.getLastPathSegment()));
				}
			}

			if (sendBroadcast && crowdMsg != null && crowdMsg.getId() > 0) {
				// Send broadcast
				Intent i = new Intent(JNI_BROADCAST_NEW_QUALIFICATION_MESSAGE);
				i.addCategory(JNI_BROADCAST_CATEGROY);
				i.putExtra("msgId", crowdMsg.getId());
				mContext.sendOrderedBroadcast(i, null);
			}

			return crowdMsg;
		}

		@Override
		public void OnRequestCreateRelationCallback(V2User user,
				String additInfo) {
			User vUser = GlobalHolder.getInstance().getUser(user.uid);
			AddFriendHistroysHandler.addMeNeedAuthentication(
					getApplicationContext(), vUser, additInfo);
			Intent intent = new Intent();
			intent.setAction(JNI_BROADCAST_FRIEND_AUTHENTICATION);
			intent.addCategory(JNI_BROADCAST_CATEGROY);
			sendBroadcast(intent);

		}

		@Override
		public void OnDelGroupCallback(int groupType, long nGroupID,
				boolean bMovetoRoot) {
			// TODO just support conference
			if (groupType == Group.GroupType.CONFERENCE.intValue()) {
				String gName = "";
				Group rG = GlobalHolder.getInstance().getGroupById(
						Group.GroupType.CONFERENCE.intValue(), nGroupID);
				if (rG != null) {
					gName = rG.getName();
				} else {
					gName = nGroupID + "";
				}
				boolean flag = GlobalHolder.getInstance().removeGroup(
						GroupType.CONFERENCE, nGroupID);
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
			} else if (groupType == GroupType.CHATING.intValue()) {
				Intent i = new Intent();
				i.setAction(PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				i.putExtra("crowd", nGroupID);
				sendBroadcast(i);

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

		// 增加好友成功时的回调
		@Override
		public void OnAddGroupUserInfoCallback(int groupType, long nGroupID,
				String sXml) {
			if (sXml == null || sXml.isEmpty()) {
				V2Log.e("Incorrect user xml ");
				return;
			}

			User remoteUser = User.fromXmlToUser(sXml);

			GlobalHolder.getInstance().putUser(remoteUser.getmUserId(),
					remoteUser);

			long uid = remoteUser.getmUserId();
			GroupType gType = GroupType.fromInt(groupType);
			if (gType == GroupType.CONTACT) {
				AddFriendHistroysHandler.becomeFriendHanler(
						getApplicationContext(), sXml);
				User user = GlobalHolder.getInstance().getUser(uid);

				if (remoteUser.getmCommentname() != null) {
					user.setNickName(remoteUser.getmCommentname());
				}

				Intent intent = new Intent();
				intent.setAction(JNI_BROADCAST_FRIEND_AUTHENTICATION);
				intent.addCategory(JNI_BROADCAST_CATEGROY);
				intent.putExtra("uid", uid);
				intent.putExtra("gid", nGroupID);
				sendBroadcast(intent);

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

		@Override
		public void OnRefuseInviteJoinGroup(int groupType, long nGroupID,
				long nUserID, String sxml) {

			GroupType gType = GroupType.fromInt(groupType);
			if (gType == GroupType.CONTACT) {
				AddFriendHistroysHandler.addOtherRefused(
						getApplicationContext(), nUserID, sxml);
				// temptag 20140917
				Intent intent = new Intent();
				intent.setAction(JNI_BROADCAST_FRIEND_AUTHENTICATION);
				intent.addCategory(JNI_BROADCAST_CATEGROY);
				sendBroadcast(intent);
			}

		}

		@Override
		public void OnAddGroupFile(V2Group group, List<FileJNIObject> list) {
			if (list == null || list.size() <= 0 || group == null) {
				V2Log.e("OnAddGroupFile : May receive new group files failed.. get empty collection");
				return;
			}
			
			for (FileJNIObject fileJNIObject : list) {
				User user = GlobalHolder.getInstance().getUser(
						list.get(0).user.uid);
				VMessage vm = new VMessage(V2GlobalEnum.GROUP_TYPE_CROWD, group.id, user,
						null, new Date(GlobalConfig.getGlobalServerTime()));
				VMessageFileItem item = new VMessageFileItem(vm,
						fileJNIObject.fileName , fileJNIObject.fileType);
				item.setFileSize(fileJNIObject.fileSize);
				item.setUuid(fileJNIObject.fileId);
				item.setState(VMessageFileItem.STATE_FILE_SENT);
				//save to database
				vm.setmXmlDatas(vm.toXml());
				MessageBuilder.saveMessage(mContext, vm);
				MessageBuilder.saveFileVMessage(mContext, vm);
			}
			
			Intent intent = new Intent();
			intent.setAction(BROADCAST_CROWD_NEW_UPLOAD_FILE_NOTIFICATION);
			intent.addCategory(JNI_BROADCAST_CATEGROY);
			intent.putExtra("groupID", group.id);
			intent.putParcelableArrayListExtra("fileJniObjects",
					new ArrayList<FileJNIObject>(list));
			sendBroadcast(intent);
		}

		@Override
		public void OnKickGroupUser(int groupType, long groupId, long nUserId) {
			Intent kick = new Intent();
			kick.setAction(JNI_BROADCAST_KICED_CROWD);
			kick.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
			kick.putExtra("crowd", groupId);
			kick.putExtra("userId", nUserId);
			sendBroadcast(kick);
		}
	}

	class AudioRequestCB extends AudioRequestCallbackAdapter {

		@Override
		public void OnAudioChatInvite(AudioJNIObjectInd ind) {

			if (GlobalHolder.getInstance().isInVideoCall()) {
				GlobalState state = GlobalHolder.getInstance().getGlobalState();
				// if in video automatically accept audio and user never accept
				// audio call.
				// because audio and video use different message
				if (state.getUid() == ind.getFromUserId()
						&& !state.isVoiceConnected()) {
					AudioRequest.getInstance().AcceptAudioChat(
							ind.getSzSessionID(), ind.getFromUserId());
					// mark voice state to connected
					GlobalHolder.getInstance().setVoiceConnectedState(true);
				} else {
					V2Log.i("Ignore audio call for others: "
							+ ind.getFromUserId());
					AudioRequest.getInstance().RefuseAudioChat(
							ind.getSzSessionID(), ind.getFromUserId());
				}
				return;
			}

			if (GlobalHolder.getInstance().isInMeeting()
					|| GlobalHolder.getInstance().isInAudioCall()
					|| GlobalHolder.getInstance().isInVideoCall()) {
				V2Log.i("Ignore audio call ");
				AudioRequest.getInstance().RefuseAudioChat(
						ind.getSzSessionID(), ind.getFromUserId());
				return;
			}

			Intent iv = new Intent();
			iv.addCategory(PublicIntent.DEFAULT_CATEGORY);
			iv.setAction(PublicIntent.START_P2P_CONVERSACTION_ACTIVITY);
			iv.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			iv.putExtra("uid", ind.getFromUserId());
			iv.putExtra("is_coming_call", true);
			iv.putExtra("voice", true);
			iv.putExtra("sessionID", ind.getSzSessionID());
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
				VideoRequest.getInstance().refuseVideoChat(
						ind.getSzSessionID(), ind.getFromUserId(),
						ind.getDeviceId());
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

		@Override
		/*
		 * Use to user quickly pressed video call button more than one time
		 * Because chat close event doesn't notify to activity. P2PConversation
		 * doesn't start up yet.
		 * 
		 * @see
		 * com.V2.jni.VideoRequestCallbackAdapter#OnVideoChatClosed(com.V2.jni
		 * .ind.VideoJNIObjectInd)
		 */
		public void OnVideoChatClosed(VideoJNIObjectInd ind) {
			super.OnVideoChatClosed(ind);
			if (GlobalHolder.getInstance().isInMeeting()
					|| GlobalHolder.getInstance().isInAudioCall()
					|| GlobalHolder.getInstance().isInVideoCall()) {
				return;
			}
			Intent i = new Intent(JNI_BROADCAST_VIDEO_CALL_CLOSED);
			i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
			i.putExtra("fromUserId", ind.getFromUserId());
			i.putExtra("groupId", ind.getGroupId());
			// Send sticky broadcast, make sure activity receive
			mContext.sendStickyBroadcast(i);

		}

	}

	class ConfRequestCB extends ConfRequestCallbackAdapter {

		public ConfRequestCB(JNICallbackHandler mCallbackHandler) {
		}

		@Override
		public void OnConfNotify(V2Conference v2conf, V2User user) {
			if (v2conf == null || user == null) {
				V2Log.e(" v2conf is " + v2conf + " or user is null" + user);
				return;
			}

			User owner = GlobalHolder.getInstance().getUser(user.uid);
			Group g = new ConferenceGroup(v2conf.cid, v2conf.name, owner,
					v2conf.startTime, owner);
			User u = GlobalHolder.getInstance().getUser(user.uid);
			g.setOwnerUser(u);
			GlobalHolder.getInstance().addGroupToList(
					Group.GroupType.CONFERENCE.intValue(), g);

			Intent i = new Intent();
			i.setAction(JNIService.JNI_BROADCAST_CONFERENCE_INVATITION);
			i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
			i.putExtra("gid", g.getmGId());
			sendBroadcast(i);
		}

	}

	class ChatRequestCB extends ChatRequestCallbackAdapter {

		public ChatRequestCB(JNICallbackHandler mCallbackHandler) {
		}

		@Override
		public void OnRecvChatTextCallback(int eGroupType, long nGroupID,
				long nFromUserID, long nToUserID, long nTime, String szSeqID,
				String szXmlText) {
			User toUser = GlobalHolder.getInstance().getUser(nToUserID);
			User fromUser = GlobalHolder.getInstance().getUser(nFromUserID);
			if (toUser == null) {
				V2Log.w("No valid user object for receive message " + toUser
						+ "  " + fromUser);
				toUser = new User(nToUserID);
			}
			if (fromUser == null) {
				V2Log.w("No valid user object for receive message " + toUser
						+ "  " + fromUser);
				fromUser = new User(nFromUserID);
			}

			String uuid = XmlParser.parseForMessageUUID(szXmlText);

			// Record image data meta
			VMessage cache = new VMessage(eGroupType, nGroupID, fromUser,
					toUser, uuid, new Date(nTime * 1000));
			cache.setmXmlDatas(szXmlText);
			XmlParser.extraImageMetaFrom(cache, szXmlText);
			if (cache.getItems().size() > 0) {
				synchronized (cacheImageMeta) {
					cacheImageMeta.add(cache);
					return;
				}
			}

			// Record audio data meta
			VMessage cacheAudio = new VMessage(eGroupType, nGroupID, fromUser,
					toUser, uuid, new Date(nTime * 1000));
			cacheAudio.setmXmlDatas(szXmlText);
			XmlParser.extraAudioMetaFrom(cacheAudio, szXmlText);
			if (cacheAudio.getItems().size() > 0) {
				synchronized (cacheAudioMeta) {
					cacheAudioMeta.add(cacheAudio);
					return;
				}
			}

			VMessage vm = new VMessage(eGroupType, nGroupID, fromUser, toUser,
					uuid, new Date(nTime * 1000));
			vm.setmXmlDatas(szXmlText);

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
		public void OnRecvChatBinary(int eGroupType, long nGroupID,
				long nFromUserID, long nToUserID, long nTime, int binaryType,
				String messageId, String binaryPath) {

			switch (binaryType) {
			case BINARY_TYPE_IMAGE:
				handlerChatPictureCallback(eGroupType, nGroupID, nFromUserID,
						nToUserID, nTime, messageId, binaryPath);
				break;
			case BINARY_TYPE_AUDIO:
				handlerChatAudioCallback(eGroupType, nGroupID, nFromUserID,
						nToUserID, nTime, messageId, binaryPath);
				break;
			default:
				break;
			}
		}

		@Override
		public void OnSendChatResult(SendingResultJNIObjectInd ind) {
			super.OnSendChatResult(ind);
			if (ind.getRet() == SendingResultJNIObjectInd.Result.FAILED) {
				Intent i = new Intent();
				i.setAction(JNIService.JNI_BROADCAST_MESSAGE_SENT_FAILED);
				i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
				i.putExtra("uuid", ind.getUuid());
				i.putExtra("errorCode", ind.getErrorCode());
				sendBroadcast(i);
			}
		}

		private void handlerChatPictureCallback(int eGroupType, long nGroupID,
				long nFromUserID, long nToUserID, long nTime, String messageId,
				String binaryPath) {

			boolean isCache = false;
			VMessage vm = null;
			String uuid = messageId;
			synchronized (cacheImageMeta) {
				for (VMessage v : cacheImageMeta) {
					List<VMessageImageItem> items = v.getImageItems();
					int receivedCount = 0;
					for (int i = 0; i < items.size(); i++) {

						VMessageImageItem vait = (VMessageImageItem) items
								.get(i);
						if (vait.isReceived()) {
							receivedCount++;
							continue;
						}

						if (vait.getUuid().equals(uuid)) {
							receivedCount++;
							vm = v;
							vait.setFilePath(binaryPath);
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
				V2Log.e(" Didn't receive image meta data: " + messageId);
				return;
			}

			Message.obtain(mCallbackHandler, JNI_RECEIVED_MESSAGE, vm)
					.sendToTarget();

		}

		private void handlerChatAudioCallback(int eGroupType, long nGroupID,
				long nFromUserID, long nToUserID, long nTime, String messageId,
				String binaryPath) {
			VMessage vm = null;
			synchronized (cacheAudioMeta) {
				for (VMessage v : cacheAudioMeta) {
					List<VMessageAudioItem> list = v.getAudioItems();
					for (int i = 0; i < list.size(); i++) {
						VMessageAudioItem item = list.get(i);
						if (item.getUuid().equals(messageId)) {
							item.setAudioFilePath(binaryPath);
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
				Message.obtain(mCallbackHandler, JNI_RECEIVED_MESSAGE, vm)
						.sendToTarget();
			} else {
				V2Log.e(" Didn't find audio item : " + messageId);
			}
		}

	}

	class FileRequestCB extends FileRequestCallbackAdapter {

		private JNICallbackHandler mCallbackHandler;

		public FileRequestCB(JNICallbackHandler mCallbackHandler) {
			this.mCallbackHandler = mCallbackHandler;
		}

		@Override
		public void OnFileTransInvite(FileJNIObject file) {
			User fromUser = GlobalHolder.getInstance().getUser(file.user.uid);
			// If doesn't receive user information from server side,
			// construct new user object
			if (fromUser == null) {
				fromUser = new User(file.user.uid);
			}
			// FIXME input date as null
			VMessage vm = new VMessage(0, 0, fromUser, GlobalHolder
					.getInstance().getCurrentUser(), new Date(
					GlobalConfig.getGlobalServerTime()));
			VMessageFileItem vfi = new VMessageFileItem(vm, file.fileId,
					file.fileSize, file.fileName, file.fileType);
			vfi.setState(VMessageFileItem.STATE_FILE_UNDOWNLOAD);
			vm.setmXmlDatas(vm.toXml());
			Message.obtain(mCallbackHandler, JNI_RECEIVED_MESSAGE, vm)
					.sendToTarget();
		}

		@Override
		public void OnFileTransEnd(String szFileID, String szFileName,
				long nFileSize, int nTransType) {
			VMessage vm = new VMessage(0, 0, null, null);
			VMessageFileItem item = new VMessageFileItem(vm, null);
			item.setUuid(szFileID);
			if (nTransType == FileDownLoadErrorIndication.TYPE_SEND)
				item.setState(VMessageAbstractItem.STATE_FILE_SENT);
			else
				item.setState(VMessageAbstractItem.STATE_FILE_DOWNLOADED);
			int updates = MessageBuilder.updateVMessageItem(mContext, item);
			Log.e(TAG, "OnFileTransEnd updates success : " + updates);
			vm = null;
			item = null;

		}

		@Override
		public void OnFileDownloadError(String szFileID, int errorCode,
				int nTransType) {
			VMessage vm = new VMessage(0, 0, null, null);
			VMessageFileItem item = new VMessageFileItem(vm, null);
			item.setUuid(szFileID);
			if (nTransType == FileDownLoadErrorIndication.TYPE_SEND)
				item.setState(VMessageAbstractItem.STATE_FILE_SENT_FALIED);
			else
				item.setState(VMessageAbstractItem.STATE_FILE_DOWNLOADED_FALIED);
			int updates = MessageBuilder.updateVMessageItem(mContext, item);
			Log.e(TAG, "OnFileTransEnd updates success : " + updates);
			vm = null;
			item = null;
		}

	}

}
