package com.bizcom.vc.service;

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
import android.util.Log;
import android.widget.Toast;

import com.V2.jni.AudioRequest;
import com.V2.jni.ChatRequest;
import com.V2.jni.ConfRequest;
import com.V2.jni.FileRequest;
import com.V2.jni.GroupRequest;
import com.V2.jni.ImRequest;
import com.V2.jni.VideoRequest;
import com.V2.jni.callbacAdapter.AudioRequestCallbackAdapter;
import com.V2.jni.callbacAdapter.ChatRequestCallbackAdapter;
import com.V2.jni.callbacAdapter.ConfRequestCallbackAdapter;
import com.V2.jni.callbacAdapter.FileRequestCallbackAdapter;
import com.V2.jni.callbacAdapter.GroupRequestCallbackAdapter;
import com.V2.jni.callbacAdapter.ImRequestCallbackAdapter;
import com.V2.jni.callbacAdapter.VideoRequestCallbackAdapter;
import com.V2.jni.callbackInterface.ImRequestCallback;
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
import com.bizcom.bo.GroupUserObject;
import com.bizcom.bo.MessageObject;
import com.bizcom.bo.UserAvatarObject;
import com.bizcom.bo.UserStatusObject;
import com.bizcom.db.ContentDescriptor;
import com.bizcom.db.provider.VerificationProvider;
import com.bizcom.request.jni.FileDownLoadErrorIndication;
import com.bizcom.request.jni.JNIResponse;
import com.bizcom.request.util.BitmapManager;
import com.bizcom.util.Notificator;
import com.bizcom.util.XmlParser;
import com.bizcom.vc.activity.contacts.AddFriendHistroysHandler;
import com.bizcom.vc.activity.conversation.MessageBuilder;
import com.bizcom.vc.activity.conversation.MessageLoader;
import com.bizcom.vc.activity.conversationav.ConversationP2PAVActivity;
import com.bizcom.vc.activity.main.MainActivity;
import com.bizcom.vc.application.GlobalConfig;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.application.GlobalState;
import com.bizcom.vc.application.MainApplication;
import com.bizcom.vc.application.PublicIntent;
import com.bizcom.vc.application.V2GlobalConstants;
import com.bizcom.vc.listener.CommonCallBack;
import com.bizcom.vc.listener.CommonCallBack.CommonUpdateConversationStateInterface;
import com.bizcom.vo.AddFriendHistorieNode;
import com.bizcom.vo.AudioVideoMessageBean;
import com.bizcom.vo.ConferenceGroup;
import com.bizcom.vo.CrowdGroup;
import com.bizcom.vo.CrowdGroup.AuthType;
import com.bizcom.vo.DiscussionGroup;
import com.bizcom.vo.FileDownLoadBean;
import com.bizcom.vo.Group;
import com.bizcom.vo.Group.GroupType;
import com.bizcom.vo.GroupQualicationState;
import com.bizcom.vo.NetworkStateCode;
import com.bizcom.vo.User;
import com.bizcom.vo.UserDeviceConfig;
import com.bizcom.vo.VMessage;
import com.bizcom.vo.VMessageAbstractItem;
import com.bizcom.vo.VMessageAudioItem;
import com.bizcom.vo.VMessageFileItem;
import com.bizcom.vo.VMessageFileItem.FileType;
import com.bizcom.vo.VMessageImageItem;
import com.bizcom.vo.VMessageQualification;
import com.bizcom.vo.VMessageQualification.QualificationState;
import com.bizcom.vo.VMessageQualification.ReadState;
import com.bizcom.vo.VMessageQualification.Type;
import com.bizcom.vo.VMessageQualificationApplicationCrowd;
import com.bizcom.vo.VMessageQualificationInvitationCrowd;
import com.bizcom.vo.VideoBean;
import com.v2tech.R;

/**
 * This service is used to wrap JNI call.<br>
 * JNI calls are asynchronous, we don't expect activity involve JNI.<br>
 * 
 * @author 28851274
 * 
 */
public class JNIService extends Service implements
		CommonUpdateConversationStateInterface {
	private static final String TAG = "JNIService";

	public static final int BINARY_TYPE_AUDIO = 3;
	public static final int BINARY_TYPE_IMAGE = 2;

	public static final String JNI_ACTIVITY_CATEGROY = "com.v2tech";
	public static final String JNI_BROADCAST_CATEGROY = "com.v2tech.jni.broadcast";

	public static final String JNI_BROADCAST_CONNECT_STATE_NOTIFICATION = "com.v2tech.jni.broadcast.connect_state_notification";
	public static final String JNI_BROADCAST_USER_STATUS_NOTIFICATION = "com.v2tech.jni.broadcast.user_stauts_notification";
	public static final String JNI_BROADCAST_FILE_STATUS_ERROR_NOTIFICATION = "com.v2tech.jni.broadcast.file_stauts_error_notification";
	/**
	 * Notify user avatar changed, notice please do not listen this broadcast if
	 * you are UI. Use
	 * {@link BitmapManager#registerBitmapChangedListener(com.bizcom.request.util.BitmapManager.BitmapChangedListener)}
	 * to listener bitmap change if you are UI.<br>
	 * key avatar : #UserAvatarObject
	 */
	public static final String JNI_BROADCAST_USER_AVATAR_CHANGED_NOTIFICATION = "com.v2tech.jni.broadcast.user_avatar_notification";
	public static final String JNI_BROADCAST_USER_UPDATE_BASE_INFO = "com.v2tech.jni.broadcast.user_update_base_info";
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
	public static final String JNI_BROADCAST_CONFERENCE_CONF_SYNC_CLOSE_VIDEO_TO_MOBILE = "com.v2tech.jni.broadcast.conference_OnConfSyncCloseVideoToMobile";
	public static final String JNI_BROADCAST_CONFERENCE_CONF_SYNC_OPEN_VIDEO_TO_MOBILE = "com.v2tech.jni.broadcast.conference_OnConfSyncOpenVideoToMobile";
	public static final String JNI_BROADCAST_GROUPS_LOADED = "com.v2tech.jni.broadcast.groups_loaded";
	public static final String JNI_BROADCAST_OFFLINE_MESSAGE_END = "com.v2tech.jni.broadcast.offline_message_end";
	// Current user kicked by crowd master key crowd : crowdId
	public static final String JNI_BROADCAST_KICED_CROWD = "com.v2tech.jni.broadcast.kick_crowd";
	// Crowd invitation with key crowd
	public static final String JNI_BROADCAST_CROWD_INVATITION = "com.v2tech.jni.broadcast.crowd_invatition";
	// Broadcast for joined new discussion key gid
	public static final String JNI_BROADCAST_NEW_DISCUSSION_NOTIFICATION = "com.v2tech.jni.broadcast.new_discussion_notification";
	public static final String BROADCAST_CROWD_NEW_UPLOAD_FILE_NOTIFICATION = "com.v2tech.jni.broadcast.new.upload_crowd_file_message";

	// //////////////////////////////////////////////////////////
	// Internal message definition //
	// //////////////////////////////////////////////////////////
	private static final int JNI_UPDATE_USER_INFO = 24;
	private static final int JNI_UPDATE_USER_STATE = 25;
	private static final int JNI_LOG_OUT = 26;
	private static final int JNI_GROUP_NOTIFY = 35;
	private static final int JNI_GROUP_USER_INFO_NOTIFICATION = 60;
	private static final int JNI_GROUP_LOADED = 63;
	private static final int JNI_OFFLINE_LOADED = 64;
	private static final int JNI_CONFERENCE_INVITATION = 61;
	private static final int JNI_RECEIVED_MESSAGE = 91;
	private static final int JNI_RECEIVED_MESSAGE_BINARY_DATA = 93;
	private static final int JNI_RECEIVED_VIDEO_INVITION = 92;

	// ////////////////////////////////////////
	// JNI call back definitions
	private ImRequestCallback mImRequestCB;
	private GroupRequestCB mGroupRequestCB;
	private VideoRequestCB mVideoRequestCB;
	private ChatRequestCB mChatRequestCB;
	private ConfRequestCB mConfRequestCB;
	private AudioRequestCB mAudioRequestCB;
	private FileRequestCB mFileRequestCB;

	private HandlerThread mLocalHandlerThread;
	private LocalHandlerThreadHandler mLocalHandlerThreadHandler;

	private Context mContext;
	private List<Integer> delayBroadcast = new ArrayList<Integer>();
	private List<GroupUserInfoOrig> delayUserBroadcast = new ArrayList<GroupUserInfoOrig>();
	private List<MessageObject> delayMessageBroadcast = new ArrayList<MessageObject>();

	private List<VMessage> cacheImageMeta = new ArrayList<VMessage>();
	private List<VMessage> cacheAudioMeta = new ArrayList<VMessage>();
	private LongSparseArray<UserStatusObject> onLineUsers = new LongSparseArray<UserStatusObject>();

	private final LocalBinder mBinder = new LocalBinder();

	private static long lastNotificatorTime = 0;

	private Integer mBinderRef = 0;
	private boolean noNeedBroadcast;
	private boolean isAcceptApply;
	private boolean isDebug = true;

	@Override
	public void onCreate() {
		super.onCreate();
		mContext = this;

		mLocalHandlerThread = new HandlerThread("JNI-Callbck");
		mLocalHandlerThread.start();
		synchronized (mLocalHandlerThread) {
			while (!mLocalHandlerThread.isAlive()) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		mLocalHandlerThreadHandler = new LocalHandlerThreadHandler(
				mLocalHandlerThread.getLooper());
		initJNICallback();
		CommonCallBack.getInstance().setConversationStateInterface(this);
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

	@Override
	public void updateConversationState() {
		V2Log.d(TAG,
				"TabFragmentMessage already builed successfully , send broadcast now!");
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
					if (type == JNI_GROUP_LOADED) {
						Intent loaded = new Intent();
						loaded.addCategory(JNI_BROADCAST_CATEGROY);
						loaded.setAction(JNI_BROADCAST_GROUPS_LOADED);
						sendBroadcast(loaded);
					} else if (type == JNI_OFFLINE_LOADED) {
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

				for (int i = 0; i < delayMessageBroadcast.size(); i++) {
					MessageObject msgObj = delayMessageBroadcast.get(i);
					Intent intent = new Intent(JNI_BROADCAST_NEW_MESSAGE);
					intent.addCategory(JNI_BROADCAST_CATEGROY);
					intent.putExtra("msgObj", msgObj);
					mContext.sendBroadcast(intent);
				}
			}

			delayBroadcast.clear();
			delayUserBroadcast.clear();
			delayMessageBroadcast.clear();
		}
		noNeedBroadcast = true;
	}

	private void initJNICallback() {
		mImRequestCB = new ImRequestCB();
		mGroupRequestCB = new GroupRequestCB();
		mVideoRequestCB = new VideoRequestCB();
		mConfRequestCB = new ConfRequestCB();
		mChatRequestCB = new ChatRequestCB();
		mAudioRequestCB = new AudioRequestCB();
		mFileRequestCB = new FileRequestCB();

		ImRequest.getInstance(this.getApplicationContext()).addCallback(
				mImRequestCB);
		GroupRequest.getInstance().addCallback(mGroupRequestCB);
		VideoRequest.getInstance(this.getApplicationContext()).addCallback(
				mVideoRequestCB);
		ConfRequest.getInstance().addCallback(mConfRequestCB);
		ChatRequest.getInstance(this.getApplicationContext())
				.setChatRequestCallback(mChatRequestCB);
		AudioRequest.getInstance().addCallback(mAudioRequestCB);
		FileRequest.getInstance().addCallback(mFileRequestCB);
	}

	private void updateFileState(int transType, VMessageFileItem fileItem,
			String tag, boolean isAdd) {
		long remoteID;
		VMessage vm = fileItem.getVm();
		if (vm.getMsgCode() == V2GlobalConstants.GROUP_TYPE_USER)
			remoteID = vm.getToUser().getmUserId();
		else
			remoteID = vm.getGroupId();
		GlobalHolder.getInstance().changeGlobleTransFileMember(transType,
				mContext, isAdd, remoteID, tag);
	}

	private User v2UserToUser(V2User user) {
		if (user == null) {
			return null;
		}

		User u = new User(user.uid, user.getName());
		u.setSignature(user.getmSignature());
		u.setJob(user.mJob);
		u.setTelephone(user.mTelephone);
		u.setMobile(user.mMobile);
		u.setAddress(user.mAddress);
		u.setSex(user.mSex);
		u.setEmail(user.mEmail);
		u.setFax(user.mFax);
		String commentname = user.getmCommentname();
		if (!TextUtils.isEmpty(commentname)) {
			u.setNickName(user.getmCommentname());
		}
		u.setmCommentname(user.getmCommentname());
		u.setAccount(user.mAccount);
		u.setAuthtype(user.mAuthtype);
		u.setBirthday(user.mBirthday);
		return u;
	}

	private void broadcastNetworkState(NetworkStateCode code) {

		Intent i = new Intent();
		i.setAction(JNI_BROADCAST_CONNECT_STATE_NOTIFICATION);
		i.addCategory(JNI_BROADCAST_CATEGROY);
		i.putExtra("state", (Parcelable) code);
		sendBroadcast(i);
	}

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

	private class LocalHandlerThreadHandler extends Handler {

		public LocalHandlerThreadHandler(Looper looper) {
			super(looper);
		}

		@Override
		public synchronized void handleMessage(Message msg) {
			switch (msg.what) {

			case JNI_UPDATE_USER_INFO:
				V2User v2User = (V2User) msg.obj;
				User user = v2UserToUser(v2User);
				GlobalHolder.getInstance().putUser(user.getmUserId(), user);

				Intent userInfos = new Intent(
						JNI_BROADCAST_USER_UPDATE_BASE_INFO);
				userInfos.addCategory(JNI_BROADCAST_CATEGROY);
				userInfos.putExtra("uid", v2User.uid);
				mContext.sendBroadcast(userInfos);
				break;
			case JNI_UPDATE_USER_STATE:
				UserStatusObject uso = (UserStatusObject) msg.obj;
				Intent iun = new Intent(JNI_BROADCAST_USER_STATUS_NOTIFICATION);
				iun.addCategory(JNI_BROADCAST_CATEGROY);
				iun.putExtra("status", uso);
				mContext.sendBroadcast(iun);
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
					if (msg.arg1 == V2GlobalConstants.GROUP_TYPE_DEPARTMENT
							&& !noNeedBroadcast) {
						V2Log.d(TAG,
								"TabFragmentMessage no builed successfully! Need to delay sending , type is "
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
							Log.i("20150203 1", "2");
							ImRequest.getInstance().proxy
									.getUserBaseInfo(existU.getmUserId());
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
							V2Log.e(TAG,
									"Load Group users , didn't find group information , user"
											+ " id is : " + tu.getmUserId()
											+ " group id is : " + go.gId);
						} else {
							group.addUserToGroup(existU);
						}
					}
					V2Log.w(TAG, "The Group -" + go.gId
							+ "- users info have update over! " + " type is : "
							+ go.gType + "- user size is : " + lu.size());
					if (!noNeedBroadcast) {
						V2Log.d(TAG,
								"TabFragmentMessage no builed successfully! Need to delay sending , type is "
										+ msg.arg1);
						delayUserBroadcast.add(go);
					} else {
						Intent i = new Intent(
								JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION);
						i.addCategory(JNI_BROADCAST_CATEGROY);
						i.putExtra("gid", go.gId);
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
							"TabFragmentMessage no builed successfully! Need to delay sending , type is 锛欽NI_GROUP_LOADED");
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
							"TabFragmentMessage no builed successfully! Need to delay sending , type is 锛欽NI_OFFLINE_LOADED");
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
				} else {
					GroupRequest.getInstance().proxy.getGroupInfo(
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
				vm.setReadState(VMessageAbstractItem.STATE_UNREAD);
				if (vm != null) {
					String action = null;
					vm.setReadState(VMessageAbstractItem.STATE_UNREAD);
					MessageBuilder.saveBinaryVMessage(mContext, vm);
					MessageBuilder.saveFileVMessage(mContext, vm);

					boolean isDelay = false;
					if (vm.getMsgCode() == V2GlobalConstants.GROUP_TYPE_CONFERENCE) {
						action = JNI_BROADCAST_NEW_CONF_MESSAGE;
					} else {
						action = JNI_BROADCAST_NEW_MESSAGE;
						isDelay = true;
					}

					MessageObject msgObj = new MessageObject(vm.getMsgCode(),
							vm.getGroupId(), vm.getFromUser().getmUserId(),
							vm.getId());

					if (isDelay) {
						if (!noNeedBroadcast) {
							V2Log.d(TAG,
									"TabFragmentMessage no builed successfully! Need to delay sending , type is 锛欽NI_OFFLINE_LOADED");
							delayMessageBroadcast.add(msgObj);
						} else {
							Intent msgIntent = new Intent(action);
							msgIntent
									.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
							msgIntent.putExtra("msgObj", msgObj);
							// Send ordered broadcast, make sure
							// conversationview
							// receive message first
							mContext.sendBroadcast(msgIntent, null);
						}
					} else {
						Intent msgIntent = new Intent(action);
						msgIntent
								.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
						msgIntent.putExtra("msgObj", msgObj);
						// Send ordered broadcast, make sure conversationview
						// receive message first
						mContext.sendBroadcast(msgIntent, null);
					}
				}
				break;
			case JNI_RECEIVED_MESSAGE_BINARY_DATA:
				VMessage binaryVM = (VMessage) msg.obj;
				int receiveState = msg.arg1;
				if (receiveState == 1) {
					MessageLoader.updateBinaryAudioItem(binaryVM
							.getAudioItems().get(0));
				}

				if (binaryVM.getImageItems().size() > 0) {
					List<VMessageImageItem> imageItems = binaryVM
							.getImageItems();
					for (int i = 0; i < imageItems.size(); i++) {
						MessageLoader.updateBinaryImageItem(imageItems.get(i));
					}
				}
				CommonCallBack.getInstance().executeNotifyChatInterToReplace(
						binaryVM);
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
	}

	private class ImRequestCB extends ImRequestCallbackAdapter {

		// private LocalHandlerThreadHandler mLocalHandlerThreadHandler;
		//
		// public ImRequestCB(LocalHandlerThreadHandler mCallbackHandler) {
		// this.mLocalHandlerThreadHandler = mCallbackHandler;
		// }

		@Override
		public void OnLoginCallback(long nUserID, int nStatus, int nResult,
				long serverTime, String sDBID) {
			if (JNIResponse.Result.fromInt(nResult) == JNIResponse.Result.SUCCESS) {
				// Just request current logged in user information
				ImRequest.getInstance().proxy.getUserBaseInfo(nUserID);
			}
		}

		@Override
		public void OnLogoutCallback(int nUserID) {
			// FIXME optimize code
			Message.obtain(mLocalHandlerThreadHandler, JNI_LOG_OUT)
					.sendToTarget();
			Notificator.cancelAllSystemNotification(mContext);
			// Send broadcast PREPARE_FINISH_APPLICATION first to let all
			// activity quit and release resource
			// Notice: if any activity doesn't release resource, android will
			// automatically restart main activity
			Intent i = new Intent();
			i.setAction(PublicIntent.PREPARE_FINISH_APPLICATION);
			i.addCategory(PublicIntent.DEFAULT_CATEGORY);
			mContext.sendBroadcast(i);

			mLocalHandlerThreadHandler.postDelayed(new Runnable() {
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
			V2Log.d("CONNECT",
					"--------------------------------------------------------------------");
			V2Log.d("CONNECT", "Receive Connection State is : " + nResult
					+ " -- name is : "
					+ NetworkStateCode.fromInt(nResult).name());
			GlobalHolder
					.getInstance()
					.setServerConnection(
							NetworkStateCode.fromInt(nResult) == NetworkStateCode.CONNECTED);
			V2Log.d("CONNECT", "GlobleHolder Connection State is : "
					+ GlobalHolder.getInstance().isServerConnected());
			broadcastNetworkState(NetworkStateCode.fromInt(nResult));
		}

		@Override
		public void OnUpdateBaseInfoCallback(V2User user) {
			if (user == null || user.uid <= 0) {
				return;
			}
			Message.obtain(mLocalHandlerThreadHandler, JNI_UPDATE_USER_INFO,
					user).sendToTarget();
		}

		@Override
		public void OnUserStatusUpdatedCallback(long nUserID, int type,
				int nStatus, String szStatusDesc) {
			UserStatusObject uso = new UserStatusObject(nUserID, type, nStatus);
			User u = GlobalHolder.getInstance().getUser(nUserID);
			V2Log.e(TAG,
					nUserID + " : the '" + u.getName()
							+ "' user is updating state...."
							+ User.Status.fromInt(nStatus).name());
			u.updateStatus(User.Status.fromInt(nStatus));
			u.setDeviceType(User.DeviceType.fromInt(type));
			onLineUsers.put(nUserID, uso);

			Message.obtain(mLocalHandlerThreadHandler, JNI_UPDATE_USER_STATE,
					uso).sendToTarget();
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
			Message.obtain(mLocalHandlerThreadHandler, JNI_GROUP_LOADED)
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
			Message.obtain(mLocalHandlerThreadHandler, JNI_OFFLINE_LOADED, true)
					.sendToTarget();
		}
	}

	private class GroupRequestCB extends GroupRequestCallbackAdapter {
		// private LocalHandlerThreadHandler mLocalHandlerThreadHandler;
		private ContactsGroupRequestCBHandler contactsGroupHandler;

		public GroupRequestCB() {
			// this.mLocalHandlerThreadHandler = mCallbackHandler;
			contactsGroupHandler = new ContactsGroupRequestCBHandler();
		}

		@Override
		public void OnGetGroupInfoCallback(int groupType, List<V2Group> list) {
			Message.obtain(mLocalHandlerThreadHandler, JNI_GROUP_NOTIFY,
					groupType, 0, list).sendToTarget();
		}

		@Override
		public void OnGetGroupUserInfoCallback(int groupType, long nGroupID,
				String sXml) {
			if (isDebug) {
				V2Log.d("group type:" + groupType + " " + nGroupID + " " + sXml);
			}

			Message.obtain(mLocalHandlerThreadHandler,
					JNI_GROUP_USER_INFO_NOTIFICATION,
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
				// 姝ゅ涓嶅鐞嗭紝澶勭悊鍦–onferenceService
			} else if (group.type == GroupType.CHATING.intValue()) {
				CrowdGroup cg = (CrowdGroup) GlobalHolder.getInstance()
						.getGroupById(group.id);
				cg.setAnnouncement(group.getAnnounce());
				cg.setBrief(group.getBrief());
				cg.setAuthType(CrowdGroup.AuthType.fromInt(group.authType));
				cg.setName(group.getName());
			} else if (group.type == GroupType.DISCUSSION.intValue()) {
				DiscussionGroup cg = (DiscussionGroup) GlobalHolder
						.getInstance().getGroupById(group.id);
				cg.setName(group.getName());
			} else if (group.type == GroupType.CONTACT.intValue()) {
				/**
				 * @see ContactsService#OnModifyGroupInfoCallback
				 */
				return;
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
				V2Log.d(TAG,
						"OnApplyJoinGroup --> Receive failed! Because V2Group or V2User is null");
				return;
			}

			VMessageQualification qualication = VerificationProvider
					.queryCrowdQualMessageByCrowdId(user.uid, group.id);
			if (qualication != null) {
				V2Log.d(TAG, "OnApplyJoinGroup --> qualication : "
						+ qualication.getReadState().name()
						+ " offline state : "
						+ GlobalHolder.getInstance().isOfflineLoaded());
				if (qualication.getReadState() == ReadState.READ
						&& !GlobalHolder.getInstance().isOfflineLoaded()) {
					return;
				}
			}

			CrowdGroup crowd = (CrowdGroup) GlobalHolder.getInstance()
					.getGroupById(group.id);
			if (crowd == null) {
				V2Log.d(TAG,
						"OnApplyJoinGroup --> Parse failed! Because get CrowdGroup is null from GlobleHolder!"
								+ "ID is : " + group.id);
				return;
			}

			User remoteUser = GlobalHolder.getInstance().getUser(user.uid);
			if (remoteUser.isDirty()) {
				remoteUser = v2UserToUser(user);
				GlobalHolder.getInstance().putUser(user.uid, remoteUser);
			}

			checkMessageAndSendBroadcast(
					VMessageQualification.Type.CROWD_APPLICATION, crowd,
					remoteUser, reason);

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
				User chairMan = GlobalHolder.getInstance().getUser(
						group.chairMan.uid);
				ConferenceGroup g = new ConferenceGroup(group.id,
						group.getName(), owner, group.createTime, chairMan);
				Message.obtain(mLocalHandlerThreadHandler,
						JNI_CONFERENCE_INVITATION, g).sendToTarget();
			} else if (gType == GroupType.CHATING) {
				VMessageQualification qualication = VerificationProvider
						.queryCrowdQualMessageByCrowdId(group.creator.uid,
								group.id);
				if (qualication != null) {
					V2Log.d(TAG, "OnInviteJoinGroupCallback --> qualication : "
							+ qualication.getReadState().name()
							+ " offline state : "
							+ GlobalHolder.getInstance().isOfflineLoaded());
					if (qualication.getReadState() == ReadState.READ
							&& !GlobalHolder.getInstance().isOfflineLoaded()
							&& qualication.getQualState() == QualificationState.WAITING) {
						return;
					}
				} else {
					V2Log.d(TAG, "OnInviteJoinGroupCallback --> group id : "
							+ group.id + " group name : " + group.getName()
							+ " group user id : " + group.creator.uid);
				}
				User owner = GlobalHolder.getInstance().getUser(
						group.creator.uid);
				if (owner.isDirty()) {
					if (!TextUtils.isEmpty(owner.getName())
							&& TextUtils.isEmpty(group.creator.getName())) {
						V2Log.e(TAG,
								"OnInviteJoinGroupCallback --> Get Create User Name is empty and older user"
										+ "name not empty , dirty is mistake");
					} else
						owner.setName(group.creator.getName());
				}

				CrowdGroup cg = new CrowdGroup(group.id, group.getName(), owner);
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
				DiscussionGroup cg = new DiscussionGroup(group.id,
						group.getName(), owner);
				GlobalHolder.getInstance().addGroupToList(
						GroupType.DISCUSSION.intValue(), cg);
				Intent i = new Intent();
				i.setAction(JNIService.JNI_BROADCAST_NEW_DISCUSSION_NOTIFICATION);
				i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
				i.putExtra("gid", cg.getmGId());
				sendBroadcast(i);
			}
		}

		@Override
		public void OnRequestCreateRelationCallback(V2User user,
				String additInfo) {
			if (user == null) {
				V2Log.e(TAG,
						"OnRequestCreateRelationCallback ---> Create relation failed...get user is null");
				return;
			}

			AddFriendHistorieNode node = VerificationProvider
					.queryFriendQualMessageByUserId(user.uid);
			if (node != null && node.addState == 0
					&& node.readState == ReadState.READ.intValue()) {
				V2Log.d(TAG,
						"OnRequestCreateRelationCallback --> Node readState : "
								+ node.readState + " offlineLoad" + ": "
								+ GlobalHolder.getInstance().isOfflineLoaded());
				if (node.readState == ReadState.READ.intValue()
						&& !GlobalHolder.getInstance().isOfflineLoaded()) {
					return;
				}
			} else {
				V2Log.d(TAG,
						"OnRequestCreateRelationCallback --> user id is : "
								+ user.uid);
			}

			boolean isOutORG = false;
			User vUser = GlobalHolder.getInstance().getUser(user.uid);
			if (!vUser.isDirty()) {
				AddFriendHistroysHandler.addMeNeedAuthentication(
						getApplicationContext(), vUser, additInfo);
			} else {
				isOutORG = true;
				User newUser = v2UserToUser(user);
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

					if (((MainApplication) mContext.getApplicationContext())
							.isRunningBackgound()) {
						if (!GlobalHolder.getInstance().isInAudioCall()
								|| !GlobalHolder.getInstance().isInVideoCall()) {
							Intent intent = new Intent(mContext,
									MainActivity.class);
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
				}
			} else if (groupType == GroupType.CHATING.intValue()) {
				GlobalHolder.getInstance().removeGroup(
						GroupType.fromInt(V2GlobalConstants.GROUP_TYPE_CROWD),
						nGroupID);
				Intent i = new Intent();
				i.setAction(PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				i.putExtra("group", new GroupUserObject(
						V2GlobalConstants.GROUP_TYPE_CROWD, nGroupID, -1));
				sendBroadcast(i, null);

			} else if (groupType == GroupType.DISCUSSION.intValue()) {
				GlobalHolder
						.getInstance()
						.removeGroup(
								GroupType
										.fromInt(V2GlobalConstants.GROUP_TYPE_DISCUSSION),
								nGroupID);
				Intent i = new Intent();
				i.setAction(PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				i.putExtra("group", new GroupUserObject(
						V2GlobalConstants.GROUP_TYPE_DISCUSSION, nGroupID, -1));
				sendBroadcast(i);

			}
		}

		// 20141123 1
		@Override
		public void OnDelGroupUserCallback(int groupType, long nGroupID,
				long nUserID) {

			if (groupType == V2GlobalConstants.GROUP_TYPE_DEPARTMENT
					&& nUserID == GlobalHolder.getInstance().getCurrentUserId()) {
				/**
				 * TODO If the deleted user is yourself, you will need to exit
				 * the program and give prompt, finally return to the login
				 * Activity
				 */
			}

			// 好友删除：被删除的时候，nGroupID 为 0
			if (groupType == GroupType.CONTACT.intValue()) {

				Set<Group> groupSet = GlobalHolder.getInstance()
						.getUser(nUserID).getBelongsGroup();

				for (Group gg : groupSet) {
					if (gg.getGroupType() == GroupType.CONTACT) {
						nGroupID = gg.getmGId();
						V2Log.d(TAG,
								"OnDelGroupUserCallback --> delete group id is : "
										+ nGroupID);
						break;
					}
				}

			} else if (groupType == GroupType.CHATING.intValue()
					&& GlobalHolder.getInstance().getCurrentUserId() == nUserID) {
				GlobalHolder.getInstance().removeGroup(
						GroupType.fromInt(V2GlobalConstants.GROUP_TYPE_CROWD),
						nGroupID);
				Intent i = new Intent();
				i.setAction(PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				i.putExtra("group", new GroupUserObject(
						V2GlobalConstants.GROUP_TYPE_CROWD, nGroupID, -1));
				sendBroadcast(i);
			} else if (groupType == GroupType.DISCUSSION.intValue()
					&& GlobalHolder.getInstance().getCurrentUserId() == nUserID) {
				GlobalHolder
						.getInstance()
						.removeGroup(
								GroupType
										.fromInt(V2GlobalConstants.GROUP_TYPE_DISCUSSION),
								nGroupID);
				Intent i = new Intent();
				i.setAction(PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				i.putExtra("group", new GroupUserObject(
						V2GlobalConstants.GROUP_TYPE_DISCUSSION, nGroupID, -1));
				sendBroadcast(i);
			}

			if (groupType == GroupType.DISCUSSION.intValue()) {
				DiscussionGroup dis = (DiscussionGroup) GlobalHolder
						.getInstance().getGroupById(groupType, nGroupID);
				if (dis != null) {
					if (dis.getOwnerUser().getmUserId() == nUserID) {
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
			// group.getName() + "(浜烘暟):"
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

			User newUser = v2UserToUser(user);
			GroupType gType = GroupType.fromInt(groupType);
			newUser = GlobalHolder.getInstance().putUser(newUser.getmUserId(),
					newUser);
			GlobalHolder.getInstance().addUserToGroup(newUser, nGroupID);

			if (gType == GroupType.CONTACT) {
				contactsGroupHandler.OnAddContactsGroupUserInfoCallback(
						nGroupID, newUser);
			} else if (gType == GroupType.CHATING) {
				long msgID = -1;
				if (user.uid != GlobalHolder.getInstance().getCurrentUserId()) {
					long waitMessageExist = VerificationProvider
							.queryCrowdInviteWaitingQualMessageById(user.uid,
									nGroupID);
					if (waitMessageExist != -1) {
						boolean isTrue = VerificationProvider
								.deleteCrowdInviteWattingQualMessage(waitMessageExist);
						if (!isTrue) {
							V2Log.e(TAG,
									"delete local invite waitting qualication message failed... cols id is :"
											+ waitMessageExist);
						}

						GroupQualicationState state = new GroupQualicationState(
								Type.CROWD_APPLICATION,
								QualificationState.BE_ACCEPTED, null,
								ReadState.UNREAD, false);
						state.isUpdateTime = false;
						msgID = VerificationProvider
								.updateCrowdQualicationMessageState(nGroupID,
										user.uid, state);
					} else {
						CrowdGroup group = (CrowdGroup) GlobalHolder
								.getInstance()
								.getGroupById(groupType, nGroupID);
						if (group == null) {
							V2Log.e(TAG,
									"OnAddGroupUserInfoCallback --> update crowd qualication message failed..group is null");
						} else {
							if (group.getOwnerUser().getmUserId() == GlobalHolder
									.getInstance().getCurrentUserId()
									&& group.getAuthType() == AuthType.QULIFICATION) {
								GroupQualicationState state = new GroupQualicationState(
										Type.CROWD_APPLICATION,
										QualificationState.ACCEPTED, null,
										ReadState.UNREAD, false);
								state.isUpdateTime = false;
								msgID = VerificationProvider
										.updateCrowdQualicationMessageState(
												nGroupID, user.uid, state);
							}
						}
					}
				}

				if (msgID == -1) {
					V2Log.e(TAG,
							"OnAddGroupUserInfoCallback --> update crowd qualication message failed..");
				} else {
					sendQualicationBroad(msgID);
				}
			} else if (gType == GroupType.DISCUSSION) {
				DiscussionGroup dis = (DiscussionGroup) GlobalHolder
						.getInstance().getGroupById(groupType, nGroupID);
				if (dis != null) {
					if (dis.getOwnerUser().getmUserId() == user.uid) {
						if (!dis.isCreatorExist()) {
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
					.getGroupById(V2GlobalConstants.GROUP_TYPE_CROWD, group.id);
			if (g == null) {
				User user = GlobalHolder.getInstance().getUser(
						group.creator.uid);
				g = new CrowdGroup(group.id, group.getName(), user, null);
				g.setBrief(group.getBrief());
				g.setAnnouncement(group.getAnnounce());
				GlobalHolder.getInstance().addGroupToList(
						GroupType.CHATING.intValue(), g);
			}

			long msgID = VerificationProvider
					.updateCrowdQualicationMessageState(group,
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
					V2GlobalConstants.GROUP_TYPE_CROWD, group.id, -1));
			sendBroadcast(i);

			sendQualicationBroad(msgID);
		}

		@Override
		public void OnRefuseApplyJoinGroup(V2Group parseSingleCrowd,
				String reason) {
			if (parseSingleCrowd == null) {
				V2Log.e(TAG,
						"OnRefuseApplyJoinGroup : May receive refuse apply join message failed.. get null V2Group Object");
				return;
			}

			long msgID = VerificationProvider
					.updateCrowdQualicationMessageState(parseSingleCrowd,
							new GroupQualicationState(Type.CROWD_INVITATION,
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

			sendQualicationBroad(msgID);
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

				Group isExist = GlobalHolder.getInstance().getGroupById(
						V2GlobalConstants.GROUP_TYPE_CROWD, obj.groupID);
				if (isExist == null) {
					V2Log.e(TAG,
							"The Crowd Group already no exist! group id is : "
									+ obj.groupID);
					return;
				}
				long waitMessageExist = VerificationProvider
						.queryCrowdInviteWaitingQualMessageById(obj.userID,
								obj.groupID);
				if (waitMessageExist != -1) {
					boolean isTrue = VerificationProvider
							.deleteCrowdInviteWattingQualMessage(waitMessageExist);
					if (!isTrue) {
						V2Log.e(TAG,
								"delete local invite waitting qualication message failed... cols id is :"
										+ waitMessageExist);
					}
				}
				long msgID = VerificationProvider
						.updateCrowdQualicationMessageState(
								obj.groupID,
								obj.userID,
								new GroupQualicationState(
										com.bizcom.vo.VMessageQualification.Type
												.fromInt(obj.qualicationType),
										com.bizcom.vo.VMessageQualification.QualificationState
												.fromInt(obj.state),
										obj.reason, ReadState.UNREAD, false));
				if (msgID == -1) {
					V2Log.e(TAG,
							"OnRefuseInviteJoinGroup --> update refuse Invite join group failed... !");
					return;
				}

				sendQualicationBroad(msgID);
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
				CrowdGroup g = new CrowdGroup(crowd.id, crowd.getName(), user,
						null);
				g.setBrief(crowd.getBrief());
				g.setAnnouncement(crowd.getAnnounce());
				g.setAuthType(CrowdGroup.AuthType.fromInt(crowd.authType));
				GlobalHolder.getInstance().addGroupToList(
						GroupType.CHATING.intValue(), g);

				GroupQualicationState state = new GroupQualicationState(
						Type.CROWD_INVITATION, QualificationState.ACCEPTED,
						null, ReadState.UNREAD, false);
				state.isUpdateTime = false;
				VerificationProvider.updateCrowdQualicationMessageState(crowd,
						state);

				Intent i = new Intent();
				i.setAction(PublicIntent.BROADCAST_NEW_CROWD_NOTIFICATION);
				i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
				i.putExtra("group", new GroupUserObject(
						V2GlobalConstants.GROUP_TYPE_CROWD, crowd.id, -1));
				sendBroadcast(i);
			} else if (crowd.type == V2Group.TYPE_DISCUSSION_BOARD
					&& GlobalHolder.getInstance().getCurrentUserId() != crowd.owner.uid) {
				if (GlobalHolder.getInstance().getCurrentUserId() == crowd.creator.uid) {
					Group existGroup = GlobalHolder.getInstance().getGroupById(
							V2GlobalConstants.GROUP_TYPE_DISCUSSION, crowd.id);
					if (existGroup != null)
						return;
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
				DiscussionGroup g = new DiscussionGroup(crowd.id,
						crowd.getName(), user, null);
				GlobalHolder.getInstance().addGroupToList(
						GroupType.DISCUSSION.intValue(), g);
				Intent i = new Intent();
				i.setAction(PublicIntent.BROADCAST_NEW_CROWD_NOTIFICATION);
				i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
				i.putExtra("group", new GroupUserObject(
						V2GlobalConstants.GROUP_TYPE_DISCUSSION, crowd.id, -1));
				sendBroadcast(i);
			}
		}

		@Override
		public void OnAddGroupFile(V2Group group, List<FileJNIObject> list) {
			if (list == null || list.size() <= 0 || group == null) {
				V2Log.e("OnAddGroupFile : May receive new group files failed.. get empty collection");
				return;
			}

			if (group.type == V2GlobalConstants.GROUP_TYPE_CROWD) {
				CrowdGroup cg = (CrowdGroup) GlobalHolder.getInstance()
						.getGroupById(group.id);
				if (cg != null) {
					cg.addNewFileNum(list.size());
				}
			} else if (group.type == V2GlobalConstants.GROUP_TYPE_CONFERENCE) {
				// TODO 浼氳鍏变韩鏂囦欢
				return;
			}

			for (FileJNIObject fileJNIObject : list) {
				long uploadUserID = fileJNIObject.user.uid;
				// 鑷繁涓婁紶涓嶆彁绀�
				if (GlobalHolder.getInstance().getCurrentUserId() == uploadUserID)
					continue;
				User user = GlobalHolder.getInstance().getUser(uploadUserID);
				VMessage vm = new VMessage(V2GlobalConstants.GROUP_TYPE_CROWD,
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
			VMessageQualification crowdMsg = null;
			if (type == Type.CROWD_APPLICATION) {
				crowdMsg = VerificationProvider
						.queryCrowdApplyQualMessageByUserId(g.getmGId(),
								user.getmUserId());
			} else {
				crowdMsg = VerificationProvider.queryCrowdQualMessageByCrowdId(
						user, g);
			}

			if (crowdMsg != null) {
				if (crowdMsg.getQualState() != VMessageQualification.QualificationState.WAITING) {
					crowdMsg.setQualState(VMessageQualification.QualificationState.WAITING);
				}

				CrowdGroup olderGroup = crowdMsg.getmCrowdGroup();
				crowdMsg.setReadState(VMessageQualification.ReadState.UNREAD);
				crowdMsg.setmCrowdGroup(g);
				if (type == VMessageQualification.Type.CROWD_APPLICATION) {
					((VMessageQualificationApplicationCrowd) crowdMsg)
							.setApplyReason(reason);
				} else if (type == VMessageQualification.Type.CROWD_INVITATION) {
					crowdMsg.setRejectReason(reason);
				} else {
					throw new RuntimeException(
							"checkMessageAndSendBroadcast --> Unkown type");
				}

				if (olderGroup.getmGId() == g.getmGId())
					VerificationProvider
							.updateCrowdQualicationMessage(crowdMsg);
				else
					VerificationProvider.updateCrowdQualicationMessage(
							olderGroup, crowdMsg);
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
				sendQualicationBroad(crowdMsg.getId());
			}
			return crowdMsg;
		}

		private void sendQualicationBroad(long msgID) {
			Intent i = new Intent(JNI_BROADCAST_NEW_QUALIFICATION_MESSAGE);
			i.addCategory(JNI_BROADCAST_CATEGROY);
			i.putExtra("msgId", msgID);
			mContext.sendBroadcast(i);

		}

	}

	private class AudioRequestCB extends AudioRequestCallbackAdapter {

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

			// 濡傛灉鍦╬2p鐣岄潰鍒欐嫆缁�
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
			Log.i("20150130 1", "OnAudioChatInvite set true "
					+ GlobalHolder.getInstance().isP2pAVNeedStickyBraodcast());

		}

		private void updateAudioRecord(AudioJNIObjectInd ind) {
			if ((System.currentTimeMillis() / 1000) - lastNotificatorTime > 2) {
				Uri notification = RingtoneManager
						.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
				Ringtone r = RingtoneManager
						.getRingtone(mContext, notification);
				if (r != null) {
					r.play();
				}
				lastNotificatorTime = System.currentTimeMillis() / 1000;
			}
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

		@Override
		public void OnAudioChatClosed(AudioJNIObjectInd ind) {
			super.OnAudioChatClosed(ind);
			if (GlobalHolder.getInstance().isP2pAVNeedStickyBraodcast()) {
				Intent i = new Intent(JNI_BROADCAST_VIDEO_CALL_CLOSED);
				i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
				i.putExtra("fromUserId", ind.getFromUserId());
				i.putExtra("groupId", ind.getGroupId());
				// Send sticky broadcast, make sure activity receive
				mContext.sendStickyBroadcast(i);
			}
			Log.i("20150130 1", "OnAudioChatClosed: "
					+ GlobalHolder.getInstance().isP2pAVNeedStickyBraodcast());
		}
	}

	private class VideoRequestCB extends VideoRequestCallbackAdapter {

		// private LocalHandlerThreadHandler mLocalHandlerThreadHandler;
		//
		// public VideoRequestCB(LocalHandlerThreadHandler mCallbackHandler) {
		// this.mLocalHandlerThreadHandler = mCallbackHandler;
		// }

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

			if (((MainApplication) mContext.getApplicationContext())
					.isRunningBackgound()) {
				updateVideoRecord(ind);
				VideoRequest.getInstance().refuseVideoChat(
						ind.getSzSessionID(), ind.getFromUserId(),
						ind.getDeviceId());
				return;
			}

			Message.obtain(mLocalHandlerThreadHandler,
					JNI_RECEIVED_VIDEO_INVITION, ind).sendToTarget();
			Log.i("20150130 1", "OnVideoChatInviteCallback set true "
					+ GlobalHolder.getInstance().isP2pAVNeedStickyBraodcast());
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

		// @Override
		// /*
		// * Use to user quickly pressed video call button more than one time
		// * Because chat close event doesn't notify to activity.
		// P2PConversation
		// * doesn't start up yet.
		// *
		// * @see
		// * com.V2.jni.VideoRequestCallbackAdapter#OnVideoChatClosed(com.V2.jni
		// * .ind.VideoJNIObjectInd)
		// */
		// public void OnVideoChatClosed(VideoJNIObjectInd ind) {
		// super.OnVideoChatClosed(ind);
		// // if (GlobalHolder.getInstance().isInMeeting()
		// // || GlobalHolder.getInstance().isInAudioCall()
		// // || GlobalHolder.getInstance().isInVideoCall()) {
		// // Log.i("20150128 1","OnVideoChatClosed return");
		// // return;
		// // }
		// if (GlobalHolder.getInstance().isP2pAVNeedStickyBraodcast()) {
		// Intent i = new Intent(JNI_BROADCAST_VIDEO_CALL_CLOSED);
		// i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		// i.putExtra("fromUserId", ind.getFromUserId());
		// i.putExtra("groupId", ind.getGroupId());
		// // Send sticky broadcast, make sure activity receive
		// mContext.sendStickyBroadcast(i);
		// }
		// Log.i("20150130 1", "OnVideoChatClosed:"
		// + GlobalHolder.getInstance().isP2pAVNeedStickyBraodcast());
		// }

		private void updateVideoRecord(VideoJNIObjectInd ind) {
			if ((System.currentTimeMillis() / 1000) - lastNotificatorTime > 2) {
				Uri notification = RingtoneManager
						.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
				Ringtone r = RingtoneManager
						.getRingtone(mContext, notification);
				if (r != null) {
					r.play();
				}
				lastNotificatorTime = System.currentTimeMillis() / 1000;
			}
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

	private class ConfRequestCB extends ConfRequestCallbackAdapter {

		// public ConfRequestCB(LocalHandlerThreadHandler mCallbackHandler) {
		// }

		@Override
		public void OnConfNotify(V2Conference v2conf, V2User user) {
			if (v2conf == null || user == null) {
				V2Log.e(" v2conf is " + v2conf + " or user is null" + user);
				return;
			}

			User owner = GlobalHolder.getInstance().getUser(user.uid);
			Group g = new ConferenceGroup(v2conf.cid, v2conf.name, owner,
					v2conf.startTime, owner);
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

			// 骞挎挱缁欑晫闈�

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

			// 骞挎挱缁欑晫闈�
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

			// 骞挎挱缁欑晫闈�
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

			// 骞挎挱缁欑晫闈�
			Intent i = new Intent();
			i.setAction(JNIService.JNI_BROADCAST_CONFERENCE_CONF_SYNC_CLOSE_VIDEO);
			i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
			i.putExtra("gid", gid);
			i.putExtra("dstDeviceID", str);
			sendBroadcast(i);

		}

	}

	private class ChatRequestCB extends ChatRequestCallbackAdapter {

		// public ChatRequestCB(LocalHandlerThreadHandler mCallbackHandler) {
		// }

		@Override
		public void OnRecvChatTextCallback(int eGroupType, long nGroupID,
				long nFromUserID, long nToUserID, long nTime, String szSeqID,
				String szXmlText) {
			User toUser = GlobalHolder.getInstance().getUser(nToUserID);
			User fromUser = GlobalHolder.getInstance().getUser(nFromUserID);

			String uuid = XmlParser.parseForMessageUUID(szXmlText);

			// Record audio data meta
			VMessage vm = new VMessage(eGroupType, nGroupID, fromUser, toUser,
					uuid, new Date(nTime * 1000));
			vm.setmXmlDatas(szXmlText);
			XmlParser.extraAudioMetaFrom(vm, szXmlText);
			if (vm.getAudioItems().size() > 0) {
				synchronized (cacheAudioMeta) {
					cacheAudioMeta.add(vm);
					// messageQueue.add(vm);
					MessageBuilder.saveMessage(mContext, vm);
				}
			} else {
				// Record image data meta
				vm = new VMessage(eGroupType, nGroupID, fromUser, toUser, uuid,
						new Date(nTime * 1000));
				vm.setmXmlDatas(szXmlText);
				XmlParser.extraImageMetaFrom(vm, szXmlText);
				if (vm.getImageItems().size() > 0) {
					synchronized (cacheImageMeta) {
						cacheImageMeta.add(vm);
						// messageQueue.add(vm);
						MessageBuilder.saveMessage(mContext, vm);
					}
				} else {
					vm = new VMessage(eGroupType, nGroupID, fromUser, toUser,
							uuid, new Date(nTime * 1000));
					vm.setmXmlDatas(szXmlText);
					// messageQueue.add(vm);
					MessageBuilder.saveMessage(mContext, vm);
				}
			}

			Message.obtain(mLocalHandlerThreadHandler, JNI_RECEIVED_MESSAGE, vm)
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

			List<String> cacheNames = GlobalHolder.getInstance()
					.getDataBaseTableCacheName();
			if (!cacheNames.contains(ContentDescriptor.HistoriesMessage.NAME)) {
				V2Log.d(TAG,
						"OnSendChatResult --> update database failed...beacuse table isn't exist! name is : "
								+ ContentDescriptor.HistoriesMessage.NAME);
				return;
			}

			List<VMessage> messages = MessageLoader.queryMessage(
					ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_ID
							+ "= ? ", new String[] { ind.getUuid() }, null);
			if (messages != null && messages.size() > 0) {
				VMessage vm = messages.get(0);
				int state;
				int fileState;
				if (ind.getRet() == SendingResultJNIObjectInd.Result.FAILED) {
					state = VMessageAbstractItem.STATE_SENT_FALIED;
					fileState = VMessageAbstractItem.STATE_FILE_SENT_FALIED;
				} else {
					state = VMessageAbstractItem.STATE_SENT_SUCCESS;
					fileState = VMessageAbstractItem.STATE_FILE_SENT;
				}

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
				Intent i = new Intent();
				i.setAction(JNIService.JNI_BROADCAST_MESSAGE_SENT_RESULT);
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

			Message.obtain(mLocalHandlerThreadHandler,
					JNI_RECEIVED_MESSAGE_BINARY_DATA, vm).sendToTarget();
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
							item.setState(VMessageAbstractItem.STATE_SENT_SUCCESS);
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
				Message.obtain(mLocalHandlerThreadHandler,
						JNI_RECEIVED_MESSAGE_BINARY_DATA, vm).sendToTarget();
			} else {
				V2Log.e(" Didn't find audio item : " + messageId);
			}
		}

	}

	private class FileRequestCB extends FileRequestCallbackAdapter {

		// private LocalHandlerThreadHandler mLocalHandlerThreadHandler;
		private Map<String, FileDownLoadBean> mark = new HashMap<String, FileDownLoadBean>();

		// public FileRequestCB(LocalHandlerThreadHandler mCallbackHandler) {
		// this.mLocalHandlerThreadHandler = mCallbackHandler;
		// }

		@Override
		public void OnFileTransBegin(String szFileID, int nTransType,
				long nFileSize) {
		}

		@Override
		public void OnFileTransProgress(String szFileID, long nBytesTransed,
				int nTransType) {
			if (!szFileID.contains("AVATAR")) {
				super.OnFileTransProgress(szFileID, nBytesTransed, nTransType);
				FileDownLoadBean lastBean = mark.get(szFileID);
				if (lastBean == null) {
					lastBean = new FileDownLoadBean();
					lastBean.lastLoadTime = System.currentTimeMillis();
					lastBean.lastLoadSize = 0;
					mark.put(szFileID, lastBean);
				} else {
					FileDownLoadBean bean = GlobalHolder.getInstance().globleFileProgress
							.get(szFileID);
					if (bean == null)
						bean = new FileDownLoadBean();

					bean.lastLoadTime = lastBean.lastLoadTime;
					bean.lastLoadSize = lastBean.lastLoadSize;
					long time = System.currentTimeMillis();
					bean.currentLoadTime = time;
					bean.currentLoadSize = nBytesTransed;
					GlobalHolder.getInstance().globleFileProgress.put(szFileID,
							bean);
				}
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
			new VMessageFileItem(vm, file.fileId, file.fileSize,
					VMessageFileItem.STATE_FILE_UNDOWNLOAD, file.fileName,
					FileType.fromInt(file.fileType));
			vm.setmXmlDatas(vm.toXml());
			MessageBuilder.saveMessage(mContext, vm);
			Message.obtain(mLocalHandlerThreadHandler, JNI_RECEIVED_MESSAGE, vm)
					.sendToTarget();
		}

		@Override
		public void OnFileTransEnd(String szFileID, String szFileName,
				long nFileSize, int nTransType) {
			if (!szFileID.contains("AVATAR")) {
				for (int i = 0; i < cacheImageMeta.size(); i++) {
					VMessage vMessage = cacheImageMeta.get(i);
					for (int j = 0; j < vMessage.getImageItems().size(); j++) {
						VMessageImageItem vMessageImageItem = vMessage
								.getImageItems().get(j);
						if (vMessageImageItem.getUuid().equals(szFileID)) {
							vMessage.notReceiveImageSize -= 1;
							break;
						}
					}
				}

				mark.remove(szFileID);
				GlobalHolder.getInstance().globleFileProgress.remove(szFileID);
				VMessageFileItem fileItem = MessageLoader
						.queryFileItemByID(szFileID);
				if (fileItem == null) {
					V2Log.e(TAG, "File Trans End Record Failed! ID is : "
							+ szFileID);
					return;
				}

				int transType;
				if (nTransType == FileDownLoadErrorIndication.TYPE_SEND) {
					fileItem.setState(VMessageAbstractItem.STATE_FILE_SENT);
					transType = V2GlobalConstants.FILE_TRANS_SENDING;
				} else {
					fileItem.setState(VMessageAbstractItem.STATE_FILE_DOWNLOADED);
					transType = V2GlobalConstants.FILE_TRANS_DOWNLOADING;
				}
				updateFileState(transType, fileItem,
						"JNIService OnFileTransEnd", false);
				int updates = MessageBuilder.updateVMessageItem(mContext,
						fileItem);
				V2Log.d(TAG, "OnFileTransEnd updates success : " + updates);
				fileItem = null;
			}
		}

		@Override
		public void OnFileTransError(String szFileID, int errorCode,
				int nTransType) {
			boolean isTrueLocation = false;
			if (!szFileID.contains("AVATAR")) {
				for (int i = 0; i < cacheImageMeta.size(); i++) {
					VMessage vm = cacheImageMeta.get(i);
					for (int j = 0; j < vm.getImageItems().size(); j++) {
						VMessageImageItem image = vm.getImageItems().get(j);
						if (image.getUuid().equals(szFileID)) {
							isTrueLocation = true;
							image.setFilePath("error");
							V2Log.e(TAG, "the image -" + szFileID
									+ "- trans error!");
							break;
						}
					}

					if (isTrueLocation) {
						vm.setmXmlDatas(vm.getmXmlDatas());
						if (vm.notReceiveImageSize - 1 <= 0)
							cacheImageMeta.remove(i);
						Message.obtain(mLocalHandlerThreadHandler,
								JNI_RECEIVED_MESSAGE_BINARY_DATA, vm)
								.sendToTarget();
						break;
					}
				}

				for (int i = 0; i < cacheAudioMeta.size(); i++) {
					VMessage vm = cacheAudioMeta.get(i);
					for (int j = 0; j < vm.getAudioItems().size(); j++) {
						VMessageAudioItem audio = vm.getAudioItems().get(j);
						if (audio.getUuid().equals(szFileID)) {
							V2Log.e(TAG, "the audio -" + szFileID
									+ "- trans error!");
							audio.setState(VMessageAbstractItem.STATE_SENT_FALIED);
							isTrueLocation = true;
							break;
						}
					}

					if (isTrueLocation) {
						Message msg = Message.obtain(
								mLocalHandlerThreadHandler,
								JNI_RECEIVED_MESSAGE_BINARY_DATA, vm);
						msg.arg1 = 1;
						msg.sendToTarget();
						break;
					}
				}

				mark.remove(szFileID);
				GlobalHolder.getInstance().globleFileProgress.remove(szFileID);
				VMessageFileItem fileItem = MessageLoader
						.queryFileItemByID(szFileID);
				if (fileItem != null) {
					int transType = -1;

					if (fileItem.getState() == VMessageAbstractItem.STATE_FILE_SENDING
							|| fileItem.getState() == VMessageAbstractItem.STATE_FILE_PAUSED_SENDING) {
						transType = V2GlobalConstants.FILE_TRANS_SENDING;
						fileItem.setState(VMessageAbstractItem.STATE_FILE_SENT_FALIED);
					} else if (fileItem.getState() == VMessageAbstractItem.STATE_FILE_DOWNLOADING
							|| fileItem.getState() == VMessageAbstractItem.STATE_FILE_PAUSED_DOWNLOADING) {
						fileItem.setState(VMessageAbstractItem.STATE_FILE_DOWNLOADED_FALIED);
						transType = V2GlobalConstants.FILE_TRANS_DOWNLOADING;
						;
					}
					int updates = MessageBuilder.updateVMessageItem(mContext,
							fileItem);
					V2Log.e(TAG, "OnFileTransEnd updates success : " + updates);
					updateFileState(transType, fileItem,
							"JNIService OnFileTransError", false);

					Intent intent = new Intent();
					intent.setAction(JNI_BROADCAST_FILE_STATUS_ERROR_NOTIFICATION);
					intent.addCategory(JNI_BROADCAST_CATEGROY);
					intent.putExtra("fileID", szFileID);
					intent.putExtra("transType", transType);
					sendBroadcast(intent);
				} else {
					V2Log.w(TAG, "OnFileTransError updates miss , id : "
							+ szFileID);
				}
				fileItem = null;
			}
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
