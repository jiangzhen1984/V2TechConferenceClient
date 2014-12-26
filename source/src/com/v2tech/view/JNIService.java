package com.v2tech.view;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
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
import android.text.TextUtils;
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
import com.V2.jni.ind.GroupJoinErrorJNIObject;
import com.V2.jni.ind.GroupQualicationJNIObject;
import com.V2.jni.ind.SendingResultJNIObjectInd;
import com.V2.jni.ind.V2Conference;
import com.V2.jni.ind.V2Group;
import com.V2.jni.ind.V2User;
import com.V2.jni.ind.VideoJNIObjectInd;
import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.db.ContentDescriptor;
import com.v2tech.db.provider.VerificationProvider;
import com.v2tech.service.BitmapManager;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.jni.FileDownLoadErrorIndication;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.GlobalState;
import com.v2tech.util.Notificator;
import com.v2tech.util.XmlParser;
import com.v2tech.view.bo.GroupUserObject;
import com.v2tech.view.bo.UserAvatarObject;
import com.v2tech.view.bo.UserStatusObject;
import com.v2tech.view.contacts.add.AddFriendHistroysHandler;
import com.v2tech.view.conversation.CommonCallBack;
import com.v2tech.view.conversation.CommonCallBack.CommonUpdateConversationStateInterface;
import com.v2tech.view.conversation.ConversationP2PAVActivity;
import com.v2tech.view.conversation.MessageBuilder;
import com.v2tech.view.conversation.MessageLoader;
import com.v2tech.vo.AddFriendHistorieNode;
import com.v2tech.vo.AudioVideoMessageBean;
import com.v2tech.vo.ConferenceGroup;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.DiscussionGroup;
import com.v2tech.vo.FileDownLoadBean;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.GroupQualicationState;
import com.v2tech.vo.NetworkStateCode;
import com.v2tech.vo.User;
import com.v2tech.vo.UserDeviceConfig;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.VMessageAbstractItem;
import com.v2tech.vo.VMessageAudioItem;
import com.v2tech.vo.VMessageFileItem;
import com.v2tech.vo.VMessageFileItem.FileType;
import com.v2tech.vo.VMessageImageItem;
import com.v2tech.vo.VMessageQualification;
import com.v2tech.vo.VMessageQualification.QualificationState;
import com.v2tech.vo.VMessageQualification.ReadState;
import com.v2tech.vo.VMessageQualification.Type;
import com.v2tech.vo.VMessageQualificationApplicationCrowd;
import com.v2tech.vo.VMessageQualificationInvitationCrowd;
import com.v2tech.vo.VideoBean;

/**
 * This service is used to wrap JNI call.<br>
 * JNI calls are asynchronous, we don't expect activity involve JNI.<br>
 * 
 * @author 28851274
 * 
 */
public class JNIService extends Service implements
		CommonUpdateConversationStateInterface {
	private static final String TAG_FILE = "JNIService";
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
	 * to listener bitmap change if you are UI.<br>
	 * key avatar : #UserAvatarObject
	 */
	public static final String JNI_BROADCAST_USER_AVATAR_CHANGED_NOTIFICATION = "com.v2tech.jni.broadcast.user_avatar_notification";
	public static final String JNI_BROADCAST_USER_UPDATE_NAME_OR_SIGNATURE = "com.v2tech.jni.broadcast.user_update_sigature";
	public static final String JNI_BROADCAST_GROUP_NOTIFICATION = "com.v2tech.jni.broadcast.group_geted";
	public static final String JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION = "com.v2tech.jni.broadcast.group_user_updated";
	public static final String JNI_BROADCAST_GROUP_UPDATED = "com.v2tech.jni.broadcast.group_updated";
	public static final String JNI_BROADCAST_GROUP_JOIN_FAILED = "com.v2tech.jni.broadcast.group_join_failed";
	public static final String JNI_BROADCAST_NEW_MESSAGE = "com.v2tech.jni.broadcast.new.message";
	public static final String JNI_BROADCAST_MESSAGE_SENT_RESULT = "com.v2tech.jni.broadcast.message_sent_result";
	public static final String JNI_BROADCAST_NEW_CONF_MESSAGE = "com.v2tech.jni.broadcast.new.conf.message";
	public static final String JNI_BROADCAST_CONFERENCE_INVATITION = "com.v2tech.jni.broadcast.conference_invatition_new";
	public static final String JNI_BROADCAST_CONFERENCE_REMOVED = "com.v2tech.jni.broadcast.conference_removed";
	public static final String JNI_BROADCAST_CONFERENCE_CONF_SYNC_OPEN_VIDEO = "com.v2tech.jni.broadcast.conference_confSyncOpenVideo";
	public static final String JNI_BROADCAST_CONFERENCE_CONF_SYNC_CLOSE_VIDEO = "com.v2tech.jni.broadcast.conference_confSyncCloseVideo";
	public static final String JNI_BROADCAST_GROUP_USER_REMOVED = "com.v2tech.jni.broadcast.group_user_removed";
	public static final String JNI_BROADCAST_GROUP_USER_ADDED = "com.v2tech.jni.broadcast.group_user_added";
	public static final String JNI_BROADCAST_VIDEO_CALL_CLOSED = "com.v2tech.jni.broadcast.video_call_closed";
	public static final String JNI_BROADCAST_CONTACTS_AUTHENTICATION = "com.v2tech.jni.broadcast.friend_authentication";
	public static final String JNI_BROADCAST_NEW_QUALIFICATION_MESSAGE = "com.v2tech.jni.broadcast.new.qualification_message";
	public static final String BROADCAST_CROWD_NEW_UPLOAD_FILE_NOTIFICATION = "com.v2tech.jni.broadcast.new.upload_crowd_file_message";
	public static final String JNI_BROADCAST_CONFERENCE_CONF_SYNC_CLOSE_VIDEO_TO_MOBILE="com.v2tech.jni.broadcast.conference_OnConfSyncCloseVideoToMobile";
	public static final String JNI_BROADCAST_CONFERENCE_CONF_SYNC_OPEN_VIDEO_TO_MOBILE="com.v2tech.jni.broadcast.conference_OnConfSyncOpenVideoToMobile";
	
	
	public static final String JNI_BROADCAST_GROUPS_LOADED = "com.v2tech.jni.broadcast.groups_loaded";
	public static final String JNI_BROADCAST_OFFLINE_MESSAGE_END = "com.v2tech.jni.broadcast.offline_message_end";
	/**
	 * Current user kicked by crowd master key crowd : crowdId
	 */
	public static final String JNI_BROADCAST_KICED_CROWD = "com.v2tech.jni.broadcast.kick_crowd";

	/**
	 * Crowd invitation with key crowd
	 */
	public static final String JNI_BROADCAST_CROWD_INVATITION = "com.v2tech.jni.broadcast.crowd_invatition";

	/**
	 * Broadcast for joined new discussion key gid
	 */
	public static final String JNI_BROADCAST_NEW_DISCUSSION_NOTIFICATION = "com.v2tech.jni.broadcast.new_discussion_notification";

	private boolean isDebug = true;

	private final LocalBinder mBinder = new LocalBinder();

	private Integer mBinderRef = 0;

	/**
	 * @see V2GlobalEnum
	 */
	private List<Integer> delayBroadcast = new ArrayList<Integer>();
	private List<GroupUserInfoOrig> delayUserBroadcast = new ArrayList<GroupUserInfoOrig>();
	private boolean noNeedBroadcast;
	private boolean isAcceptApply;

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
		GroupRequest.getInstance().addCallback(mGRCB);

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

		CommonCallBack.getInstance().setConversationStateInterface(this);
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

	private User convertUser(V2User user) {
		if (user == null) {
			return null;
		}

		User u = new User(user.uid, user.name);
		u.setSignature(user.mSignature);
		u.setJob(user.mJob);
		u.setTelephone(user.mTelephone);
		u.setMobile(user.mMobile);
		u.setAddress(user.mAddress);
		u.setSex(user.mSex);
		u.setEmail(user.mEmail);
		u.setFax(user.mFax);
		if (user.mCommentname != null && !user.mCommentname.isEmpty()) {
			u.setNickName(user.mCommentname);
		}
		u.setmCommentname(user.mCommentname);
		u.setAccount(user.mAccount);
		u.setAuthtype(user.mAuthtype);
		u.setBirthday(user.mBirthday);
		return u;
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
	private static final int JNI_GROUP_LOADED = 63;
	private static final int JNI_OFFLINE_LOADED = 64;
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
				GlobalHolder
						.getInstance()
						.setServerConnection(
								NetworkStateCode.fromInt(msg.arg1) == NetworkStateCode.CONNECTED);
				broadcastNetworkState(NetworkStateCode.fromInt(msg.arg1));
				break;
			case JNI_UPDATE_USER_INFO:
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
					if (((msg.arg1 == V2GlobalEnum.GROUP_TYPE_CROWD) ||
						(msg.arg1 == V2GlobalEnum.GROUP_TYPE_DEPARTMENT))
							&& !noNeedBroadcast) {
						V2Log.d(TAG,
								"ConversationTabFragment no builed successfully! Need to delay sending , type is ："
										+ msg.arg1);
						delayBroadcast.add(msg.arg1);
					} else {
						Intent gi = new Intent(JNI_BROADCAST_GROUP_NOTIFICATION);
						gi.putExtra("gtype", msg.arg1);
						gi.addCategory(JNI_BROADCAST_CATEGROY);
						mContext.sendBroadcast(gi);
					}
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
						if (existU.isDirty()
								&& !GlobalHolder.getInstance().getGlobalState()
										.isGroupLoaded()) {
							V2Log.e(TAG,
									"The User that id is : "
											+ existU.getmUserId() + " dirty!"
											+ " Need to get user base infos");
							ImRequest.getInstance().getUserBaseInfo(
									existU.getmUserId());
						}
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

					if (!noNeedBroadcast) {
						V2Log.d(TAG,
								"ConversationTabFragment no builed successfully! Need to delay sending , type is ："
										+ msg.arg1);
						delayUserBroadcast.add(go);
					} else {
						Intent i = new Intent(
								JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION);
						i.addCategory(JNI_BROADCAST_CATEGROY);
						i.putExtra("gid",go.gId);
						i.putExtra("gtype", go.gType);
						mContext.sendBroadcast(i);
					}
				} else {
					V2Log.e("Invalid group user data");
				}
				break;
			case JNI_GROUP_LOADED:
				V2Log.e(TAG, "The All Group Infos Loaded !");
				if (!noNeedBroadcast) {
					V2Log.d(TAG,
							"ConversationTabFragment no builed successfully! Need to delay sending , type is ：JNI_GROUP_LOADED");
					delayBroadcast.add(JNI_GROUP_LOADED);
				} else {
					Intent loaded = new Intent();
					loaded.addCategory(JNI_BROADCAST_CATEGROY);
					loaded.setAction(JNI_BROADCAST_GROUPS_LOADED);
					sendBroadcast(loaded);
				}
				break;
			case JNI_OFFLINE_LOADED:
				boolean isOfflineEnd = (Boolean) msg.obj;
				V2Log.e(TAG, "OFFLINE MESSAGE LOAD : " + isOfflineEnd);
				if (!noNeedBroadcast) {
					V2Log.d(TAG,
							"ConversationTabFragment no builed successfully! Need to delay sending , type is ：JNI_OFFLINE_LOADED");
					delayBroadcast.add(JNI_OFFLINE_LOADED);
				} else {
					Intent i = new Intent();
					i.addCategory(JNI_BROADCAST_CATEGROY);
					i.setAction(JNI_BROADCAST_OFFLINE_MESSAGE_END);
					sendBroadcast(i);
				}
				break;
			case JNI_CONFERENCE_INVITATION:
				Group g = (Group) msg.obj;
				Group cache = GlobalHolder.getInstance().getGroupById(
						GroupType.CONFERENCE.intValue(), g.getmGId());
				// conference already in cache list
				if (cache != null && g.getmGId() != 0) {
					V2Log.d("Current user conference in group:"
							+ cache.getName() + "  " + cache.getmGId()
							+ " only send Broadcast!");
					Intent i = new Intent();
					i.setAction(JNIService.JNI_BROADCAST_CONFERENCE_INVATITION);
					i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
					i.putExtra("gid", g.getmGId());
					sendBroadcast(i);
					return;
				}
				else{
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
				}
				break;
			case JNI_RECEIVED_MESSAGE:
				VMessage vm = (VMessage) msg.obj;
				if (vm != null) {
					String action = null;
					vm.setReadState(VMessageAbstractItem.STATE_UNREAD);
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
				long serverTime , String sDBID) {
			if (JNIResponse.Result.fromInt(nResult) == JNIResponse.Result.SUCCESS) {
				// Just request current logged in user information
				ImRequest.getInstance().getUserBaseInfo(nUserID);
			}
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
		public void OnUpdateBaseInfoCallback(V2User user) {
			if (user == null || user.uid <= 0) {
				return;
			}
			User u = convertUser(user);
			GlobalHolder.getInstance().putUser(u.getmUserId(), u);
			// Message.obtain(mCallbackHandler, JNI_UPDATE_USER_INFO, u)
			// .sendToTarget();
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
				V2Log.e(TAG,
						"OnChangeAvatarCallback --> Change the user avatar failed...file path is : "
								+ f.getAbsolutePath());
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
		public void OnGroupsLoaded() {
			Message.obtain(mCallbackHandler, JNI_GROUP_LOADED)
				.sendToTarget();
		}

		@Override
		public void OnOfflineStart() {
			super.OnOfflineStart();
			GlobalHolder.getInstance().setOfflineLoaded(false);
		}
		
		@Override
		public void OnOfflineEnd() {
			super.OnOfflineEnd();
			Message.obtain(mCallbackHandler, JNI_OFFLINE_LOADED , true)
				.sendToTarget();
		}
	}

	class GroupRequestCB extends GroupRequestCallbackAdapter {
		private JNICallbackHandler mCallbackHandler;
		private ContactsGroupRequestCBHandler contactsGroupHandler;

		public GroupRequestCB(JNICallbackHandler mCallbackHandler) {
			this.mCallbackHandler = mCallbackHandler;
			contactsGroupHandler = new ContactsGroupRequestCBHandler();
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
				V2Log.e(TAG,
						"OnModifyGroupInfoCallback --> update Group Infos failed...get V2Group is null!");
				return;
			}

			if (group.type == GroupType.CONFERENCE.intValue()) {
				// 此处不处理，处理在ConferenceService
			} else if (group.type == GroupType.CHATING.intValue()) {
				CrowdGroup cg = (CrowdGroup) GlobalHolder.getInstance()
						.getGroupById(group.id);
				cg.setAnnouncement(group.announce);
				cg.setBrief(group.brief);
				cg.setAuthType(CrowdGroup.AuthType.fromInt(group.authType));
				cg.setName(group.name);
			} else if (group.type == GroupType.DISCUSSION.intValue()) {
				DiscussionGroup cg = (DiscussionGroup) GlobalHolder
						.getInstance().getGroupById(group.id);
				cg.setName(group.name);
			}

			// Send broadcast
			Intent i = new Intent(JNI_BROADCAST_GROUP_UPDATED);
			i.addCategory(JNI_BROADCAST_CATEGROY);
			i.putExtra("gid", group.id);
			mContext.sendBroadcast(i);

		}

		@Override
		public void OnApplyJoinGroup(V2Group group, V2User user, String reason) {
			if (group == null || user == null) {
				V2Log.d(TAG, "OnApplyJoinGroup --> Receive failed! Because V2Group or V2User is null");
				return;
			}
			
			VMessageQualification qualication = 
					VerificationProvider.queryCrowdQualMessageByCrowdId(user.uid , group.id);
			if(qualication != null){
				V2Log.d(TAG, "OnApplyJoinGroup --> qualication : " + qualication.getReadState().name() + 
						" offline state : " + GlobalHolder.getInstance().isOfflineLoaded());
				if(qualication.getReadState() == ReadState.READ && !GlobalHolder.getInstance().isOfflineLoaded()){
					return ;
				}
			}
			else{
				V2Log.d(TAG, "OnApplyJoinGroup --> group id : " + group.id + " group name : " + group.name +
						" group user id : " + user.uid);
			}
			
			CrowdGroup crowd = (CrowdGroup) GlobalHolder.getInstance()
					.getGroupById(group.id);
			if (crowd == null) {
				V2Log.d(TAG, "OnApplyJoinGroup --> Parse failed! Because get CrowdGroup is null from GlobleHolder!"
						+ "ID is : " + group.id);
				return;
			}
			
			User remoteUser = GlobalHolder.getInstance().getUser(user.uid);
			if(remoteUser.isDirty()){
				remoteUser = convertUser(user);
				GlobalHolder.getInstance().putUser(user.uid, remoteUser);
			}
			
			checkMessageAndSendBroadcast(
					VMessageQualification.Type.CROWD_APPLICATION, crowd, remoteUser, reason);

		}

		@Override
		public void OnJoinGroupError(int eGroupType, long nGroupID, int nErrorNo) {
			// Send broadcast
			Intent i = new Intent(JNI_BROADCAST_GROUP_JOIN_FAILED);
			i.addCategory(JNI_BROADCAST_CATEGROY);
			i.putExtra("joinCode", new GroupJoinErrorJNIObject(eGroupType,
					nGroupID, nErrorNo));
			mContext.sendBroadcast(i);
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
				
				VMessageQualification qualication = 
						VerificationProvider.queryCrowdQualMessageByCrowdId(group.creator.uid , group.id);
				if(qualication != null){
					V2Log.d(TAG, "OnInviteJoinGroupCallback --> qualication : " + qualication.getReadState().name() + 
							" offline state : " + GlobalHolder.getInstance().isOfflineLoaded());
					if(qualication.getReadState() == ReadState.READ &&
						!GlobalHolder.getInstance().isOfflineLoaded() && 
						qualication.getQualState() == QualificationState.WAITING){
						return ;
					}
				}
				else{
					V2Log.d(TAG, "OnInviteJoinGroupCallback --> group id : " + group.id + " group name : " + group.name +
							" group user id : " + group.creator.uid);
				}
				User owner = GlobalHolder.getInstance().getUser(
						group.creator.uid);
				if (owner.isDirty()) {
					if(!TextUtils.isEmpty(owner.getName()) && TextUtils.isEmpty(group.creator.name)){
						V2Log.e(TAG, "OnInviteJoinGroupCallback --> Get Create User Name is empty and older user"
								+ "name not empty , dirty is mistake");
					}
					else
						owner.setName(group.creator.name);
				}

				CrowdGroup cg = new CrowdGroup(group.id, group.name, owner);
				cg.setAuthType(CrowdGroup.AuthType.fromInt(group.authType));

				checkMessageAndSendBroadcast(
						VMessageQualification.Type.CROWD_INVITATION, cg, owner,
						null);

			} else if (gType == GroupType.DISCUSSION) {
				Group cache = GlobalHolder.getInstance().getGroupById(group.id);
				if (cache != null) {
					V2Log.w("Discussion exists  id " + cache.getmGId()
							+ "  name: " + cache.getName());
					return;
				}

				User owner = GlobalHolder.getInstance().getUser(
						group.creator.uid);
				DiscussionGroup cg = new DiscussionGroup(group.id, group.name,
						owner);
				GlobalHolder.getInstance().addGroupToList(
						GroupType.DISCUSSION.intValue(), cg);
				Intent i = new Intent();
				i.setAction(JNIService.JNI_BROADCAST_NEW_DISCUSSION_NOTIFICATION);
				i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
				i.putExtra("gid", cg.getmGId());
				sendBroadcast(i);
			}
		}

		/**
		 * 
		 * @param type
		 * @param g
		 * @param user
		 * @param reason
		 * @return
		 */
		private VMessageQualification checkMessageAndSendBroadcast(
				VMessageQualification.Type type, CrowdGroup g, User user,
				String reason) {
			// boolean sendBroadcast = true;
			VMessageQualification crowdMsg = null;
			if (type == Type.CROWD_APPLICATION) {
				crowdMsg = VerificationProvider.queryCrowdApplyQualMessageByUserId(user
						.getmUserId());
				if (crowdMsg != null
						&& !(crowdMsg instanceof VMessageQualificationApplicationCrowd)) {
					VerificationProvider
							.deleteCrowdQualMessage(crowdMsg.getId());
					crowdMsg = null;
				}
			} else {
				crowdMsg = VerificationProvider.queryCrowdQualMessageByCrowdId(user, g);
			}

			if (crowdMsg != null) {
				if (crowdMsg.getQualState() != VMessageQualification.QualificationState.WAITING) {
					crowdMsg.setQualState(VMessageQualification.QualificationState.WAITING);
				}
				
				CrowdGroup olderGroup = crowdMsg.getmCrowdGroup();
				crowdMsg.setReadState(VMessageQualification.ReadState.UNREAD);
				crowdMsg.setmCrowdGroup(g);
				if (type == VMessageQualification.Type.CROWD_APPLICATION) {
					((VMessageQualificationApplicationCrowd) crowdMsg).setApplyReason(reason);
				} else if (type == VMessageQualification.Type.CROWD_INVITATION) {
					crowdMsg.setRejectReason(reason);
				} else {
					throw new RuntimeException("checkMessageAndSendBroadcast --> Unkown type");
				}
				
				if(olderGroup.getmGId() == g.getmGId())
					VerificationProvider.updateCrowdQualicationMessage(crowdMsg);
				else
					VerificationProvider.updateCrowdQualicationMessage(olderGroup, crowdMsg);
			} else {
				// Save message to database
				if (type == VMessageQualification.Type.CROWD_APPLICATION) {
					crowdMsg = new VMessageQualificationApplicationCrowd(g,
							user);
					((VMessageQualificationApplicationCrowd) crowdMsg)
							.setApplyReason(reason);
				} else if (type == VMessageQualification.Type.CROWD_INVITATION) {
					crowdMsg = new VMessageQualificationInvitationCrowd(g,
							GlobalHolder.getInstance().getCurrentUser());
				} else {
					throw new RuntimeException("Unkown type");
				}

				crowdMsg.setmTimestamp(new Date(GlobalConfig
						.getGlobalServerTime()));
				crowdMsg.setReadState(VMessageQualification.ReadState.UNREAD);
				Uri uri = VerificationProvider.saveQualicationMessage(crowdMsg);
				if (uri != null) {
					crowdMsg.setId(Long.parseLong(uri.getLastPathSegment()));
				}
			}

			if (crowdMsg != null && crowdMsg.getId() > 0) {
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
			if (user == null){
				V2Log.e(TAG,
						"OnRequestCreateRelationCallback ---> Create relation failed...get user is null");
				return ;
			}
			
			AddFriendHistorieNode node = VerificationProvider.queryFriendQualMessageByUserId(user.uid);
			if(node != null && node.readState == ReadState.READ.intValue()){
				V2Log.d(TAG, "OnRequestCreateRelationCallback --> Node readState : " + node.readState + " offlineLoad"
						+ ": " + GlobalHolder.getInstance().isOfflineLoaded());
				if(node.readState == ReadState.READ.intValue() && !GlobalHolder.getInstance().isOfflineLoaded()){
					return ;
				}
			}
			else{
				V2Log.d(TAG, "OnRequestCreateRelationCallback --> user id is : " + user.uid);
			}

			boolean isOutORG = false;
			User vUser = GlobalHolder.getInstance().getUser(user.uid);
			if (!vUser.isDirty()) {
				AddFriendHistroysHandler.addMeNeedAuthentication(
						getApplicationContext(), vUser, additInfo);
			} else { // 组织外的用户
				isOutORG = true;
				User newUser = convertUser(user);
				GlobalHolder.getInstance().putUser(newUser.getmUserId(),
						newUser);
				AddFriendHistroysHandler.addMeNeedAuthentication(
						getApplicationContext(), newUser, additInfo);
			}
			Intent intent = new Intent();
			intent.putExtra("isOutORG", isOutORG);
			intent.putExtra("v2User", user);
			intent.putExtra("uid", user.uid);
			intent.setAction(JNI_BROADCAST_CONTACTS_AUTHENTICATION);
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
					sendStickyBroadcast(i);

					if (GlobalConfig.isApplicationBackground(mContext)
							|| GlobalHolder.getInstance().isInMeeting()
							|| GlobalHolder.getInstance().isInAudioCall()
							|| GlobalHolder.getInstance().isInVideoCall()) {
						Intent intent = new Intent(mContext, MainActivity.class);
						// intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						intent.addCategory(PublicIntent.DEFAULT_CATEGORY);
						intent.putExtra("initFragment", 3);
						Notificator
						.updateSystemNotification(
								mContext,
								mContext.getText(
										R.string.requesting_delete_conference)
										.toString(),
								gName
										+ mContext
												.getText(R.string.confs_is_deleted_notification),
								1, intent,
								PublicIntent.VIDEO_NOTIFICATION_ID);
					}
				}
			} else if (groupType == GroupType.CHATING.intValue()) {
				GlobalHolder.getInstance().removeGroup(
						GroupType.fromInt(V2GlobalEnum.GROUP_TYPE_CROWD),
						nGroupID);
				Intent i = new Intent();
				i.setAction(PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				i.putExtra("group", new GroupUserObject(
						V2GlobalEnum.GROUP_TYPE_CROWD, nGroupID, -1));
				sendBroadcast(i, null);

			} else if (groupType == GroupType.DISCUSSION.intValue()) {
				GlobalHolder.getInstance().removeGroup(
						GroupType.fromInt(V2GlobalEnum.GROUP_TYPE_DISCUSSION),
						nGroupID);
				Intent i = new Intent();
				i.setAction(PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				i.putExtra("group", new GroupUserObject(
						V2GlobalEnum.GROUP_TYPE_DISCUSSION, nGroupID, -1));
				sendBroadcast(i);

			}
		}

		// 20141123 1
		@Override
		public void OnDelGroupUserCallback(int groupType, long nGroupID,
				long nUserID) {

			if (groupType == GroupType.CONTACT.intValue()) {
				// 如果是好友组，好友关系被别人移除，传来的groupId为0
				Set<Group> groupSet = GlobalHolder.getInstance()
						.getUser(nUserID).getBelongsGroup();
				for (Group gg : groupSet) {
					if (gg.getGroupType() == GroupType.CONTACT) {
						nGroupID = gg.getmGId();
					}
				}
			} else if (groupType == GroupType.CHATING.intValue()
					&& GlobalHolder.getInstance().getCurrentUserId() == nUserID) {
				GlobalHolder.getInstance().removeGroup(
						GroupType.fromInt(V2GlobalEnum.GROUP_TYPE_CROWD),
						nGroupID);
				Intent i = new Intent();
				i.setAction(PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				i.putExtra("group", new GroupUserObject(
						V2GlobalEnum.GROUP_TYPE_CROWD, nGroupID, -1));
				sendBroadcast(i);
			} else if (groupType == GroupType.DISCUSSION.intValue()
					&& GlobalHolder.getInstance().getCurrentUserId() == nUserID) {
				GlobalHolder.getInstance().removeGroup(
						GroupType.fromInt(V2GlobalEnum.GROUP_TYPE_DISCUSSION),
						nGroupID);
				Intent i = new Intent();
				i.setAction(PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				i.putExtra("group", new GroupUserObject(
						V2GlobalEnum.GROUP_TYPE_DISCUSSION, nGroupID, -1));
				sendBroadcast(i);
			}

			if(groupType == GroupType.DISCUSSION.intValue()){
				DiscussionGroup dis = (DiscussionGroup) GlobalHolder.getInstance().
						getGroupById(groupType, nGroupID);
				if(dis != null){
					if(dis.getOwnerUser().getmUserId() == nUserID){
						dis.setCreatorExist(false);
					}
				}
			}
			
			GlobalHolder.getInstance().removeGroupUser(nGroupID, nUserID);
			GroupUserObject obj = new GroupUserObject(groupType, nGroupID,
					nUserID);

			// if (groupType == GroupType.CONTACT.intValue()) {
			// List<Group> groupList = GlobalHolder.getInstance().getGroup(
			// GroupType.CONTACT.intValue());
			// for (int i = 0; i < groupList.size(); i++) {
			// Group group = groupList.get(i);
			//
			// Log.i("20141201 2",
			// group.getName() + "(人数):"
			// + String.valueOf(group.getUserCount()));
			// }
			// }

			Intent i = new Intent();
			i.setAction(JNIService.JNI_BROADCAST_GROUP_USER_REMOVED);
			i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
			i.putExtra("obj", obj);
			sendBroadcast(i);
		}

		@Override
		public void OnAddGroupUserInfoCallback(int groupType, long nGroupID,
				V2User user) {
			if (user == null) {
				V2Log.e("JNIServie OnAddGroupUserInfoCallback --> : get null GroupAddUserJNIObject Object!");
				return;
			}

			User newUser = convertUser(user);
			GroupType gType = GroupType.fromInt(groupType);
			newUser = GlobalHolder.getInstance().putUser(newUser.getmUserId(),
					newUser);
			GlobalHolder.getInstance().addUserToGroup(newUser, nGroupID);
			// if (gType == GroupType.CONTACT) {
			// Set<Group> groupSet = newUser.getBelongsGroup();
			// for (Group gg : groupSet) {
			// Log.i("20141201 2", gg.getName());
			// }
			// }

			if (gType == GroupType.CONTACT) {
				contactsGroupHandler.OnAddContactsGroupUserInfoCallback(
						nGroupID, newUser);
			} else if (gType == GroupType.CHATING) {

				long msgID = -1;
				if (user.uid != GlobalHolder.getInstance().getCurrentUserId()) {
					long waitMessageExist = VerificationProvider
							.queryCrowdInviteWaitingQualMessageById(user.uid);
					if (waitMessageExist != -1) {
						V2Log.e("CrowdCreateActivity  -->Delete  VMessageQualification Cache Object Successfully!");
						// if (CrowdGroupService.isLocalInvite) {
						boolean isTrue = VerificationProvider.deleteCrowdInviteWattingQualMessage(waitMessageExist);
						if(!isTrue){
							V2Log.e(TAG, "delete local invite waitting qualication message failed... cols id is :" + waitMessageExist);
						}
						msgID = VerificationProvider.updateCrowdQualicationMessageState(
								nGroupID, user.uid, new GroupQualicationState(
										Type.CROWD_APPLICATION,
										QualificationState.BE_ACCEPTED, null,
										ReadState.UNREAD, true));
					} else {
						V2Log.e("CrowdCreateActivity  -->Not found  VMessageQualification Cache Object ! user id is : "
								+ user.uid);
						Group group = GlobalHolder.getInstance().getGroupById(
								groupType, nGroupID);
						if (group == null) {
							V2Log.e(TAG,
									"OnAddGroupUserInfoCallback --> update crowd qualication message failed..group is null");
							return;
						}

						if (group.getOwnerUser().getmUserId() == GlobalHolder
								.getInstance().getCurrentUserId()) {
							msgID = VerificationProvider.updateCrowdQualicationMessageState(
									nGroupID, user.uid,
									new GroupQualicationState(
											Type.CROWD_APPLICATION,
											QualificationState.ACCEPTED, null,
											ReadState.UNREAD, false));
						}
					}
				}

				if (msgID == -1) {
					V2Log.e(TAG,
							"OnAddGroupUserInfoCallback --> update crowd qualication message failed..");
					return;
				}

				Intent intent = new Intent();
				intent.setAction(JNIService.JNI_BROADCAST_NEW_QUALIFICATION_MESSAGE);
				intent.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
				intent.putExtra("msgId", msgID);
				sendOrderedBroadcast(intent, null);
			} else if(gType == GroupType.DISCUSSION){
				DiscussionGroup dis = (DiscussionGroup) GlobalHolder.getInstance().
						getGroupById(groupType, nGroupID);
				if(dis != null){
					if(dis.getOwnerUser().getmUserId() == user.uid){
						if(!dis.isCreatorExist()){
							dis.setCreatorExist(true);
						}
					}
				}
			}

			GroupUserObject object = new GroupUserObject(groupType, nGroupID,
					newUser.getmUserId());
			Intent i = new Intent();
			i.setAction(JNIService.JNI_BROADCAST_GROUP_USER_ADDED);
			i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
			i.putExtra("obj", object);
			sendBroadcast(i);
		}

		@Override
		public void OnAcceptApplyJoinGroup(V2Group group) {
			if (group == null || group.creator == null) {
				V2Log.e(TAG,
						"OnAcceptApplyJoinGroup : May receive accept apply join message failed.. get null V2Group Object");
				return;
			}

			CrowdGroup g = (CrowdGroup) GlobalHolder.getInstance()
					.getGroupById(V2GlobalEnum.GROUP_TYPE_CROWD, group.id);
			if (g == null) {
				User user = GlobalHolder.getInstance().getUser(
						group.creator.uid);
				g = new CrowdGroup(group.id, group.name, user, null);
				g.setBrief(group.brief);
				g.setAnnouncement(group.announce);
				GlobalHolder.getInstance().addGroupToList(
						GroupType.CHATING.intValue(), g);
			}

			long msgID = VerificationProvider.updateCrowdQualicationMessageState(group,
					new GroupQualicationState(Type.CROWD_INVITATION,
							QualificationState.BE_ACCEPTED, null,
							ReadState.UNREAD, false));
			if (msgID == -1) {
				V2Log.e(TAG,
						"OnAcceptApplyJoinGroup : Update Qualication Message to Database failed.. return -1 , group id is : "
								+ group.id
								+ " user id"
								+ ": "
								+ group.creator.uid);
				return;
			}
			isAcceptApply = true;
			
			Intent i = new Intent();
			i.setAction(PublicIntent.BROADCAST_NEW_CROWD_NOTIFICATION);
			i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
			i.putExtra("group", new GroupUserObject(
					V2GlobalEnum.GROUP_TYPE_CROWD, group.id, -1));
			sendBroadcast(i);

			Intent intent = new Intent();
			intent.setAction(JNIService.JNI_BROADCAST_NEW_QUALIFICATION_MESSAGE);
			intent.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
			intent.putExtra("msgId", msgID);
			sendOrderedBroadcast(intent, null);
		}

		@Override
		public void OnRefuseApplyJoinGroup(V2Group parseSingleCrowd,
				String reason) {
			if (parseSingleCrowd == null) {
				V2Log.e(TAG,
						"OnRefuseApplyJoinGroup : May receive refuse apply join message failed.. get null V2Group Object");
				return;
			}

			long msgID = VerificationProvider.updateCrowdQualicationMessageState(
					parseSingleCrowd, new GroupQualicationState(
							Type.CROWD_INVITATION,
							QualificationState.BE_REJECT, reason,
							ReadState.UNREAD, false));
			if (msgID == -1) {
				V2Log.e(TAG,
						"OnRefuseApplyJoinGroup : Update Qualication Message to Database failed.. return -1 , group id is : "
								+ parseSingleCrowd.id
								+ " user id"
								+ ": "
								+ parseSingleCrowd.creator);
				return;
			}

			Intent intent = new Intent();
			intent.setAction(JNIService.JNI_BROADCAST_NEW_QUALIFICATION_MESSAGE);
			intent.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
			intent.putExtra("msgId", msgID);
			sendOrderedBroadcast(intent, null);
		}

		@Override
		public void OnRefuseInviteJoinGroup(GroupQualicationJNIObject obj) {

			if (obj == null) {
				V2Log.e("OnRefuseInviteJoinGroup : May receive refuse invite message failed.. get null GroupQualicationJNIObject");
				return;
			}

			GroupType gType = GroupType.fromInt(obj.groupType);
			if (gType == GroupType.CONTACT) {
				AddFriendHistroysHandler.addOtherRefused(
						getApplicationContext(), obj.userID, obj.reason);
				// temptag 20140917
				Intent intent = new Intent();
				intent.putExtra("uid", obj.userID);
				intent.setAction(JNI_BROADCAST_CONTACTS_AUTHENTICATION);
				intent.addCategory(JNI_BROADCAST_CATEGROY);
				sendOrderedBroadcast(intent, null);
			} else if (gType == GroupType.CHATING) {
				long waitMessageExist = VerificationProvider
						.queryCrowdInviteWaitingQualMessageById(obj.userID);
				if (waitMessageExist != -1) {
					boolean isTrue = VerificationProvider.deleteCrowdInviteWattingQualMessage(waitMessageExist);
					if(!isTrue){
						V2Log.e(TAG, "delete local invite waitting qualication message failed... cols id is :" + waitMessageExist);
					}
				}
				long msgID = VerificationProvider
						.updateCrowdQualicationMessageState(
								obj.groupID,
								obj.userID,
								new GroupQualicationState(
										com.v2tech.vo.VMessageQualification.Type
												.fromInt(obj.qualicationType),
										com.v2tech.vo.VMessageQualification.QualificationState
												.fromInt(obj.state),
										obj.reason, ReadState.UNREAD, false));
				if (msgID == -1) {
					V2Log.e(TAG,
							"OnRefuseInviteJoinGroup --> update refuse Invite join group failed... !");
					return;
				}
				Intent intent = new Intent();
				intent.setAction(JNIService.JNI_BROADCAST_NEW_QUALIFICATION_MESSAGE);
				intent.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
				intent.putExtra("msgId", msgID);
				sendOrderedBroadcast(intent, null);
			}
		}

		@Override
		public void onAddGroupInfo(V2Group crowd) {
			if (crowd == null) {
				V2Log.e(TAG,
						"onAddGroupInfo--> Given V2Group Object is null ... please check!");
				return;
			}

			if (crowd.creator == null) {
				V2Log.e(TAG,
						"onAddGroupInfo--> The Creator of V2Group Object is null ... please check!");
				return;
			}

			if (isAcceptApply) {
				isAcceptApply = false;
				return;
			}

			if (crowd.type == V2Group.TYPE_CROWD
					&& GlobalHolder.getInstance().getCurrentUserId() != crowd.owner.uid) {
				V2Log.e(TAG, "onAddGroupInfo--> add a new group , id is : "
						+ crowd.id);
				User user = GlobalHolder.getInstance().getUser(
						crowd.creator.uid);
				if (user == null) {
					V2Log.e(TAG,
							"onAddGroupInfo--> add a new group failed , get user is null , id is : "
									+ crowd.creator.uid);
					return;
				}
				CrowdGroup g = new CrowdGroup(crowd.id, crowd.name, user, null);
				g.setBrief(crowd.brief);
				g.setAnnouncement(crowd.announce);
				g.setAuthType(CrowdGroup.AuthType.fromInt(crowd.authType));
				GlobalHolder.getInstance().addGroupToList(
						GroupType.CHATING.intValue(), g);

				VerificationProvider.updateCrowdQualicationMessageState(crowd,
						new GroupQualicationState(Type.CROWD_INVITATION,
								QualificationState.ACCEPTED, null,
								ReadState.UNREAD, false));
				Intent i = new Intent();
				i.setAction(PublicIntent.BROADCAST_NEW_CROWD_NOTIFICATION);
				i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
				i.putExtra("group", new GroupUserObject(
						V2GlobalEnum.GROUP_TYPE_CROWD, crowd.id, -1));
				sendBroadcast(i);
			} else if (crowd.type == V2Group.TYPE_DISCUSSION_BOARD) {
				if(GlobalHolder.getInstance().getCurrentUserId()== crowd.creator.uid){
					Group existGroup = GlobalHolder.getInstance().
							getGroupById(V2GlobalEnum.GROUP_TYPE_DISCUSSION, crowd.id);
					if(existGroup != null)
						return ;
				}
				
				V2Log.e(TAG,
						"onAddGroupInfo--> add a new discussion group , id is : "
								+ crowd.id);
				User user = GlobalHolder.getInstance().getUser(
						crowd.creator.uid);
				if (user == null) {
					V2Log.e(TAG,
							"onAddGroupInfo--> add a new group failed , get user is null , id is : "
									+ crowd.creator.uid);
					return;
				}
				DiscussionGroup g = new DiscussionGroup(crowd.id, crowd.name,
						user, null);
				GlobalHolder.getInstance().addGroupToList(
						GroupType.DISCUSSION.intValue(), g);
				Intent i = new Intent();
				i.setAction(PublicIntent.BROADCAST_NEW_CROWD_NOTIFICATION);
				i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
				i.putExtra("group", new GroupUserObject(
						V2GlobalEnum.GROUP_TYPE_DISCUSSION, crowd.id, -1));
				sendBroadcast(i);
			}
		}

		@Override
		public void OnAddGroupFile(V2Group group, List<FileJNIObject> list) {
			if (list == null || list.size() <= 0 || group == null) {
				V2Log.e("OnAddGroupFile : May receive new group files failed.. get empty collection");
				return;
			}
			if (group.type == V2GlobalEnum.GROUP_TYPE_CROWD) {
				CrowdGroup cg = (CrowdGroup) GlobalHolder.getInstance()
						.getGroupById(group.id);
				if (cg != null) {
					cg.addNewFileNum(list.size());
				}
			}

			for (FileJNIObject fileJNIObject : list) {
				long uploadUserID = fileJNIObject.user.uid;
				// 自己上传不提示
				if (GlobalHolder.getInstance().getCurrentUserId() == uploadUserID)
					continue;
				User user = GlobalHolder.getInstance().getUser(uploadUserID);
				VMessage vm = new VMessage(V2GlobalEnum.GROUP_TYPE_CROWD,
						group.id, user, null, new Date(
								GlobalConfig.getGlobalServerTime()));
				VMessageFileItem item = new VMessageFileItem(vm,
						fileJNIObject.fileName,
						VMessageFileItem.STATE_FILE_SENT, fileJNIObject.fileId);
				item.setFileSize(fileJNIObject.fileSize);
				item.setFileType(FileType.fromInt(fileJNIObject.fileType));
				// save to database
				vm.setmXmlDatas(vm.toXml());
				MessageBuilder.saveMessage(mContext, vm);
				MessageBuilder.saveFileVMessage(mContext, vm);
				fileJNIObject.vMessageID = vm.getUUID();
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
			GlobalHolder.getInstance().removeGroup(
					GroupType.fromInt(groupType), groupId);
			Intent kick = new Intent();
			kick.setAction(JNI_BROADCAST_KICED_CROWD);
			kick.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
			kick.putExtra("group", new GroupUserObject(groupType, groupId,
					nUserId));
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
					V2Log.d(TAG_FILE, "自动接受了对方音频邀请因为在视频通话中并且是同一个人");
				} else {
					V2Log.i("Ignore audio call for others: "
							+ ind.getFromUserId());
					updateAudioRecord(ind);
					AudioRequest.getInstance().RefuseAudioChat(
							ind.getSzSessionID(), ind.getFromUserId());
				}
				return;
			}

			if (GlobalHolder.getInstance().isInMeeting()
					|| GlobalHolder.getInstance().isInAudioCall()
					|| GlobalHolder.getInstance().isInVideoCall()) {
				V2Log.i("OnAudioChatInvite --> The audio chat invite coming ! Ignore audio call ");
				updateAudioRecord(ind);
				AudioRequest.getInstance().RefuseAudioChat(
						ind.getSzSessionID(), ind.getFromUserId());
				return;
			}

			// 如果在p2p界面则拒绝
			ActivityManager activityManager = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
			List<RunningTaskInfo> listRunningTaskInfo = activityManager
					.getRunningTasks(1);
			if ((listRunningTaskInfo != null) && listRunningTaskInfo.size() > 0) {
				if (listRunningTaskInfo.get(0).topActivity.getClassName()
						.equals(ConversationP2PAVActivity.class.getName())) {
					V2Log.i("OnAudioChatInvite --> The audio chat invite coming ! Ignore audio call ");
					updateAudioRecord(ind);
					AudioRequest.getInstance().RefuseAudioChat(
							ind.getSzSessionID(), ind.getFromUserId());
					return;
				}
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

		private void updateAudioRecord(AudioJNIObjectInd ind) {
			// record in database
			VideoBean currentVideoBean = new VideoBean();
			currentVideoBean.readSatate = AudioVideoMessageBean.STATE_UNREAD;
			currentVideoBean.formUserID = ind.getFromUserId();
			currentVideoBean.remoteUserID = ind.getFromUserId();
			currentVideoBean.toUserID = GlobalHolder.getInstance()
					.getCurrentUserId();
			currentVideoBean.mediaChatID = ind.getSzSessionID();
			currentVideoBean.mediaType = AudioVideoMessageBean.TYPE_AUDIO;
			currentVideoBean.startDate = GlobalConfig.getGlobalServerTime();
			currentVideoBean.mediaState = AudioVideoMessageBean.STATE_NO_ANSWER_CALL;
			MessageBuilder.saveMediaChatHistories(mContext, currentVideoBean);

			Intent intent = new Intent();
			intent.setAction(ConversationP2PAVActivity.P2P_BROADCAST_MEDIA_UPDATE);
			intent.addCategory(PublicIntent.DEFAULT_CATEGORY);
			intent.putExtra("hasUnread", true);
			intent.putExtra("remoteID", currentVideoBean.remoteUserID);
			sendBroadcast(intent);
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
				V2Log.i("OnVideoChatInvite --> The video chat invite coming ! Ignore video call ");
				updateVideoRecord(ind);
				VideoRequest.getInstance().refuseVideoChat(
						ind.getSzSessionID(), ind.getFromUserId(),
						ind.getDeviceId());
				return;
			}

			// 如果在p2p界面则拒绝
			ActivityManager activityManager = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
			List<RunningTaskInfo> listRunningTaskInfo = activityManager
					.getRunningTasks(1);
			if ((listRunningTaskInfo != null) && listRunningTaskInfo.size() > 0) {
				if (listRunningTaskInfo.get(0).topActivity.getClassName()
						.equals(ConversationP2PAVActivity.class.getName())) {
					V2Log.i("OnVideoChatInvite --> The video chat invite coming ! Ignore video call ");
					updateVideoRecord(ind);
					VideoRequest.getInstance().refuseVideoChat(
							ind.getSzSessionID(), ind.getFromUserId(),
							ind.getDeviceId());
					return;
				}

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

		private void updateVideoRecord(VideoJNIObjectInd ind) {
			// record in database
			VideoBean currentVideoBean = new VideoBean();
			currentVideoBean.readSatate = AudioVideoMessageBean.STATE_UNREAD;
			currentVideoBean.formUserID = ind.getFromUserId();
			currentVideoBean.remoteUserID = ind.getFromUserId();
			currentVideoBean.toUserID = GlobalHolder.getInstance()
					.getCurrentUserId();
			currentVideoBean.mediaChatID = ind.getSzSessionID();
			currentVideoBean.mediaType = AudioVideoMessageBean.TYPE_VIDEO;
			currentVideoBean.startDate = GlobalConfig.getGlobalServerTime();
			currentVideoBean.mediaState = AudioVideoMessageBean.STATE_NO_ANSWER_CALL;
			MessageBuilder.saveMediaChatHistories(mContext, currentVideoBean);

			Intent intent = new Intent();
			intent.setAction(ConversationP2PAVActivity.P2P_BROADCAST_MEDIA_UPDATE);
			intent.addCategory(PublicIntent.DEFAULT_CATEGORY);
			intent.putExtra("hasUnread", true);
			intent.putExtra("remoteID", currentVideoBean.remoteUserID);
			sendBroadcast(intent);
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

		@Override
		public void OnConfSyncOpenVideo(String str) {
			V2Log.d(V2Log.JNISERVICE_CALLBACK,
					"CLASS = JNIService.ConfRequestCB METHOD = OnConfSyncOpenVideo()"
							+ " str = " + str);

			// 广播给界面

			Intent i = new Intent();
			i.setAction(JNIService.JNI_BROADCAST_CONFERENCE_CONF_SYNC_OPEN_VIDEO);
			i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
			i.putExtra("xml", str);
			sendBroadcast(i);

		}

		@Override
		public void OnConfSyncCloseVideoToMobile(long nDstUserID,
				String sDstMediaID) {
			V2Log.d(V2Log.JNISERVICE_CALLBACK,
					"CLASS = JNIService.ConfRequestCB METHOD = OnConfSyncCloseVideoToMobile()"
							+ " nDstUserID = " + nDstUserID + " sDstMediaID = "
							+ sDstMediaID);
			
			V2Log.d("20141211 2",
					"CLASS = JNIService.ConfRequestCB METHOD = OnConfSyncCloseVideoToMobile()"
							+ " nDstUserID = " + nDstUserID + " sDstMediaID = "
							+ sDstMediaID);

			// 广播给界面
			Intent i = new Intent();
			i.setAction(JNIService.JNI_BROADCAST_CONFERENCE_CONF_SYNC_CLOSE_VIDEO_TO_MOBILE);
			i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
			i.putExtra("nDstUserID", nDstUserID);
			i.putExtra("sDstMediaID", sDstMediaID);
			sendBroadcast(i);

		}

		@Override
		public void OnConfSyncOpenVideoToMobile(String sSyncVideoMsgXML) {
			V2Log.d(V2Log.JNISERVICE_CALLBACK,
					"CLASS = JNIService.ConfRequestCB METHOD = OnConfSyncOpenVideoToMobile()"
							+ " sSyncVideoMsgXML = " + sSyncVideoMsgXML);
			
			V2Log.d("20141211 2",
					"CLASS = JNIService.ConfRequestCB METHOD = OnConfSyncOpenVideoToMobile()"
							+ " sSyncVideoMsgXML = " + sSyncVideoMsgXML);
			
			// 广播给界面
			Intent i = new Intent();
			i.setAction(JNIService.JNI_BROADCAST_CONFERENCE_CONF_SYNC_OPEN_VIDEO_TO_MOBILE);
			i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
			i.putExtra("sSyncVideoMsgXML", sSyncVideoMsgXML);
			sendBroadcast(i);
		}

		@Override
		public void OnConfSyncCloseVideo(long gid, String str) {
			V2Log.d(V2Log.JNISERVICE_CALLBACK,
					"CLASS = JNIService.ConfRequestCB METHOD = OnConfSyncCloseVideo()"
							+ " gid = " + gid + " str = " + str);

			// 广播给界面
			Intent i = new Intent();
			i.setAction(JNIService.JNI_BROADCAST_CONFERENCE_CONF_SYNC_CLOSE_VIDEO);
			i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
			i.putExtra("gid", gid);
			i.putExtra("dstDeviceID", str);
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

			int state;
			int fileState;
			if (ind.getRet() == SendingResultJNIObjectInd.Result.FAILED) {
				state = VMessageAbstractItem.STATE_SENT_FALIED;
				fileState = VMessageAbstractItem.STATE_FILE_SENT_FALIED;
			} else {
				state = VMessageAbstractItem.STATE_SENT_SUCCESS;
				fileState = VMessageAbstractItem.STATE_FILE_SENT;
			}

			List<String> cacheNames = GlobalHolder.getInstance()
					.getDataBaseTableCacheName();
			if (!cacheNames.contains(ContentDescriptor.HistoriesMessage.NAME)) {
				V2Log.d(TAG,
						"OnSendChatResult --> update database failed...beacuse table isn't exist! name is : "
								+ ContentDescriptor.HistoriesMessage.NAME);
				return;
			}

			List<VMessage> messages = MessageLoader.queryMessage(mContext,
					ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_ID
							+ "= ? ", new String[] { ind.getUuid() }, null);
			if (messages != null && messages.size() > 0) {
				VMessage vm = messages.get(0);
				vm.setState(state);
				List<VMessageAbstractItem> items = vm.getItems();
				for (VMessageAbstractItem item : items) {
					switch (item.getType()) {
					case VMessageAbstractItem.ITEM_TYPE_IMAGE:
					case VMessageAbstractItem.ITEM_TYPE_AUDIO:
						item.setState(state);
						break;
					case VMessageAbstractItem.ITEM_TYPE_FILE:
						item.setState(fileState);
						break;
					}
				}
				MessageLoader.updateChatMessageState(mContext, messages.get(0));
			} else {
				V2Log.e(TAG,
						"Resend message failed...update to database failed...uuid is : "
								+ ind.getUuid());
			}

			Intent i = new Intent();
			i.setAction(JNIService.JNI_BROADCAST_MESSAGE_SENT_RESULT);
			i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
			i.putExtra("uuid", ind.getUuid());
			i.putExtra("errorCode", ind.getErrorCode());
			sendBroadcast(i);
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
		private Map<String , FileDownLoadBean> mark = new HashMap<String, FileDownLoadBean>();
		public FileRequestCB(JNICallbackHandler mCallbackHandler) {
			this.mCallbackHandler = mCallbackHandler;
		}

		@Override
		public void OnFileTransBegin(String szFileID, int nTransType,
				long nFileSize) {
			updateTransFileState(szFileID, true);
		}
		
		@Override
		public void OnFileTransProgress(String szFileID, long nBytesTransed,
				int nTransType) {
			super.OnFileTransProgress(szFileID, nBytesTransed, nTransType);
			FileDownLoadBean lastBean = mark.get(szFileID);
			if(lastBean == null){
				lastBean = new FileDownLoadBean();
				lastBean.lastLoadTime = System.currentTimeMillis();
				lastBean.lastLoadSize = 0;
				mark.put(szFileID, lastBean);
			} else {
				FileDownLoadBean bean = GlobalHolder.getInstance().globleFileProgress.get(szFileID);
				if(bean == null)
					bean = new FileDownLoadBean();
				
				bean.lastLoadTime = lastBean.lastLoadTime;
				bean.lastLoadSize = lastBean.lastLoadSize;
				long time = System.currentTimeMillis();
				bean.currentLoadTime = time;
				bean.currentLoadSize = nBytesTransed;
				GlobalHolder.getInstance().globleFileProgress.put(szFileID, bean);
			}
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
					file.fileSize, VMessageFileItem.STATE_FILE_UNDOWNLOAD,
					file.fileName, FileType.fromInt(file.fileType));
			vm.setmXmlDatas(vm.toXml());
			Message.obtain(mCallbackHandler, JNI_RECEIVED_MESSAGE, vm)
					.sendToTarget();
		}

		@Override
		public void OnFileTransEnd(String szFileID, String szFileName,
				long nFileSize, int nTransType) {
			mark.remove(szFileID);
			GlobalHolder.getInstance().globleFileProgress.remove(szFileID);
			VMessage vm = new VMessage(0, 0, null, null);
			VMessageFileItem item = new VMessageFileItem(vm, null, 0);
			item.setUuid(szFileID);
			if (nTransType == FileDownLoadErrorIndication.TYPE_SEND)
				item.setState(VMessageAbstractItem.STATE_FILE_SENT);
			else
				item.setState(VMessageAbstractItem.STATE_FILE_DOWNLOADED);
			int updates = MessageBuilder.updateVMessageItem(mContext, item);
			V2Log.e(TAG, "OnFileTransEnd updates success : " + updates);
			vm = null;
			item = null;

			updateTransFileState(szFileID, false);
		}

		@Override
		public void OnFileTransError(String szFileID, int errorCode,
				int nTransType) {
			mark.remove(szFileID);
			GlobalHolder.getInstance().globleFileProgress.remove(szFileID);
			VMessageFileItem fileItem = MessageLoader.queryFileItemByID(V2GlobalEnum.GROUP_TYPE_USER, szFileID);
			if(fileItem != null){
				if(fileItem.getState() == VMessageAbstractItem.STATE_FILE_SENDING)
					fileItem.setState(VMessageAbstractItem.STATE_FILE_SENT_FALIED);
				else if(fileItem.getState() == VMessageAbstractItem.STATE_FILE_DOWNLOADING)
					fileItem.setState(VMessageAbstractItem.STATE_FILE_DOWNLOADED_FALIED);
				int updates = MessageBuilder.updateVMessageItem(mContext, fileItem);
				V2Log.e(TAG, "OnFileTransEnd updates success : " + updates);
			}
			else{
				V2Log.e(TAG, "OnFileTransEnd updates failed , id : " + szFileID);
			}
//			VMessage vm = new VMessage(0, 0, null, null);
//			VMessageFileItem item = new VMessageFileItem(vm, null, 0);
//			item.setUuid(szFileID);
//			if (nTransType == FileDownLoadErrorIndication.TYPE_SEND)
//				item.setState(VMessageAbstractItem.STATE_FILE_SENT_FALIED);
//			else
//				item.setState(VMessageAbstractItem.STATE_FILE_DOWNLOADED_FALIED);
			fileItem = null;
			updateTransFileState(szFileID, false);
		}

		@Override
		public void OnFileTransCancel(String szFileID) {
			mark.remove(szFileID);
			GlobalHolder.getInstance().globleFileProgress.remove(szFileID);
			VMessage vm = new VMessage(0, 0, null, null);
			VMessageFileItem item = new VMessageFileItem(vm, null, 0);
			item.setUuid(szFileID);
			item.setState(VMessageAbstractItem.STATE_FILE_SENT_FALIED);
			int updates = MessageBuilder.updateVMessageItem(mContext, item);
			V2Log.e(TAG, "OnFileTransEnd updates success : " + updates);
			vm = null;
			item = null;
			updateTransFileState(szFileID, false);
		}
	}

	@Override
	public void updateConversationState() {
		V2Log.d(TAG,
				"ConversationTabFragment already builed successfully , send broadcast now!");
		if (delayBroadcast.size() <= 0) {
			V2Log.d(TAG,
					"There is no broadcast in delayBroadcast collections , mean no callback!");
		} else {
			synchronized (JNIService.class) {
				for (int i = 0; i < delayBroadcast.size(); i++) {
					int type = delayBroadcast.get(i);
					V2Log.d(TAG,
							"The delay broadcast was sending now , type is : "
									+ type);
					if(type == JNI_GROUP_LOADED){
						Intent loaded = new Intent();
						loaded.addCategory(JNI_BROADCAST_CATEGROY);
						loaded.setAction(JNI_BROADCAST_GROUPS_LOADED);
						sendBroadcast(loaded);
					} else if(type == JNI_OFFLINE_LOADED){
						Intent offline = new Intent();
						offline.addCategory(JNI_BROADCAST_CATEGROY);
						offline.setAction(JNI_BROADCAST_OFFLINE_MESSAGE_END);
						sendBroadcast(offline);
					} else {
						Intent gi = new Intent(JNI_BROADCAST_GROUP_NOTIFICATION);
						gi.putExtra("gtype", type);
						gi.addCategory(JNI_BROADCAST_CATEGROY);
						mContext.sendBroadcast(gi);
					}
				}

				for (int i = 0; i < delayUserBroadcast.size(); i++) {
					GroupUserInfoOrig go = delayUserBroadcast.get(i);
					V2Log.d(TAG,
							"The delay user broadcast was sending now , type is : "
									+ go.gType + "-------");
					Intent intent = new Intent(
							JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION);
					intent.addCategory(JNI_BROADCAST_CATEGROY);
					intent.putExtra("gid", go.gId);
					intent.putExtra("gtype", go.gType);
					mContext.sendBroadcast(intent);
				}
			}
			delayBroadcast.clear();
		}
		noNeedBroadcast = true;
	}

	private void updateTransFileState(String szFileID, boolean isAdd) {
		VMessageFileItem fileItem = MessageLoader.queryFileItemByID(
				V2GlobalEnum.GROUP_TYPE_USER, szFileID);
		if (fileItem == null || fileItem.getVm().getToUser() == null) {
			V2Log.d(TAG, "update transing file size failed...file id is : "
					+ szFileID);
			return;
		}

		long uid = fileItem.getVm().getToUser().getmUserId();
		Integer trans = GlobalConfig.mTransingFiles.get(uid);
		if(trans == null){
			trans = 0;
			GlobalConfig.mTransingFiles.put(uid, trans);
		}
		else{
			if (isAdd) {
				trans = trans + 1;
				V2Log.d("TRANSING_File_SIZE" , "JNIService updateTransFileState --> 用户" + uid
						+ "增加了一个文件传输，当前正在传输个数是：" + trans);
			} else {
				trans = trans - 1;
				V2Log.e("TRANSING_File_SIZE" , "JNIService updateTransFileState --> 用户" + uid
						+ "的一个文件传输完毕，当前正在传输个数是：" + trans);
			}
			GlobalConfig.mTransingFiles.put(uid, trans);
		}
	}

	public class ContactsGroupRequestCBHandler {
		private void OnAddContactsGroupUserInfoCallback(long nGroupID,
				User newUser) {
			AddFriendHistroysHandler.becomeFriendHanler(
					getApplicationContext(), newUser);
			Intent intent = new Intent();
			intent.setAction(JNI_BROADCAST_CONTACTS_AUTHENTICATION);
			intent.addCategory(JNI_BROADCAST_CATEGROY);
			intent.putExtra("uid", newUser.getmUserId());
			intent.putExtra("gid", nGroupID);
			sendBroadcast(intent);
		}
	}

}
