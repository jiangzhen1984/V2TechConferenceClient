package com.bizcom.vc.activity.main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.V2.jni.ind.BoUserInfoBase;
import com.V2.jni.util.V2Log;
import com.bizcom.bo.ConversationNotificationObject;
import com.bizcom.bo.GroupUserObject;
import com.bizcom.bo.MessageObject;
import com.bizcom.db.ContentDescriptor;
import com.bizcom.db.provider.ConversationProvider;
import com.bizcom.db.provider.VerificationProvider;
import com.bizcom.db.provider.VoiceProvider;
import com.bizcom.request.util.BitmapManager;
import com.bizcom.util.DateUtil;
import com.bizcom.util.MessageUtil;
import com.bizcom.util.Notificator;
import com.bizcom.util.SearchUtils;
import com.bizcom.util.SearchUtils.ScrollItem;
import com.bizcom.vc.activity.conference.GroupLayout;
import com.bizcom.vc.activity.contacts.AddFriendHistroysHandler;
import com.bizcom.vc.activity.conversation.MessageLoader;
import com.bizcom.vc.activity.conversationav.ConversationP2PAVActivity;
import com.bizcom.vc.activity.message.MessageAuthenticationActivity;
import com.bizcom.vc.activity.message.VoiceMessageActivity;
import com.bizcom.vc.application.GlobalConfig;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.application.PublicIntent;
import com.bizcom.vc.application.V2GlobalConstants;
import com.bizcom.vc.listener.CommonCallBack;
import com.bizcom.vc.listener.CommonCallBack.CommonUpdateConversationToCreate;
import com.bizcom.vc.listener.NotificationListener;
import com.bizcom.vc.service.JNIService;
import com.bizcom.vo.AddFriendHistorieNode;
import com.bizcom.vo.AudioVideoMessageBean;
import com.bizcom.vo.ContactConversation;
import com.bizcom.vo.Conversation;
import com.bizcom.vo.ConversationFirendAuthenticationData;
import com.bizcom.vo.ConversationFirendAuthenticationData.VerificationMessageType;
import com.bizcom.vo.CrowdConversation;
import com.bizcom.vo.CrowdGroup;
import com.bizcom.vo.DepartmentConversation;
import com.bizcom.vo.DiscussionConversation;
import com.bizcom.vo.DiscussionGroup;
import com.bizcom.vo.Group;
import com.bizcom.vo.Group.GroupType;
import com.bizcom.vo.OrgGroup;
import com.bizcom.vo.User;
import com.bizcom.vo.VMessage;
import com.bizcom.vo.VMessageAbstractItem;
import com.bizcom.vo.VMessageQualification;
import com.bizcom.vo.VMessageQualification.QualificationState;
import com.bizcom.vo.VMessageQualification.ReadState;
import com.bizcom.vo.VMessageQualification.Type;
import com.bizcom.vo.VMessageQualificationApplicationCrowd;
import com.bizcom.vo.VMessageQualificationInvitationCrowd;
import com.bizcom.vo.VMessageTextItem;
import com.bizcom.vo.VideoBean;
import com.v2tech.R;

public class TabFragmentMessage extends Fragment implements TextWatcher,
		CommonUpdateConversationToCreate {
	private static final String TAG = "TabFragmentMessage";
	private static final int FILL_CONFS_LIST = 2;
	private static final int VERIFICATION_TYPE_FRIEND = 5;
	private static final int VERIFICATION_TYPE_CROWD = 6;
	private static final int UPDATE_CONVERSATION = 9;
	private static final int NEW_MESSAGE_UPDATE = 11;
	private static final int REMOVE_CONVERSATION = 12;
	public static final int REQUEST_UPDATE_CHAT_CONVERSATION = 17;

	private static final int UPDATE_VERIFICATION_MESSAGE = 17;

	private View rootView;

	private Context mContext;

	private NotificationListener notificationListener;

	private BroadcastReceiver receiver;
	private IntentFilter intentFilter;

	private Set<Conversation> mUnreadConvList = new HashSet<Conversation>();;
	private List<ScrollItem> mItemList = new ArrayList<ScrollItem>();
	private List<ScrollItem> searchList = new ArrayList<ScrollItem>();
	private SparseArray<Integer> offlineCov = new SparseArray<Integer>();

	private LocalHandler mHandler = new LocalHandler();

	/**
	 * This tag is used to limit the database load times
	 */
	private boolean isLoadedCov;

	private boolean mIsStartedSearch;
	private boolean isUpdateGroup;
	private boolean isUpdateDeparment;
	private boolean isCallBack;
	private boolean isCreate;

	private boolean isAddVerificationItem;
	private boolean isAddVoiceItem;

	private boolean hasUnreadVoice;

	private boolean isShowVerificationNotify;

	private MediaPlayer mChatPlayer;

	private ListView mConversationsListView;
	private ConversationsAdapter adapter = new ConversationsAdapter();

	/**
	 * This tag is used to save current click the location of item.
	 */
	private Conversation currentClickConversation;

	/**
	 * The two special Items that were showed in Message Interface , them don't
	 * saved in database. VerificationItem item used to display verification
	 * messages VoiceItem item used to display phone message
	 */
	private ScrollItem verificationItem;
	private ScrollItem voiceItem;

	private Conversation voiceMessageItem;
	private GroupLayout voiceLayout;
	private GroupLayout verificationMessageItemLayout;
	private Conversation verificationMessageItemData;

	private ExecutorService service;
	private Resources res;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i("20150303 1", "TabFragmentMessage onCreate()");
		mContext = getActivity();
		res = getResources();
		service = Executors.newCachedThreadPool();
		initReceiver();
		notificationListener = (NotificationListener) getActivity();
		BitmapManager.getInstance().registerBitmapChangedListener(
				this.bitmapChangedListener);
		CommonCallBack.getInstance().setConversationCreate(this);
		initSpecificationItem();
		Message.obtain(mHandler, FILL_CONFS_LIST).sendToTarget();
		isCreate = true;
	}

	private void initSpecificationItem() {
		// 判断只有消息界面，才添加这两个特殊item
		initVoiceItem();
		initVerificationItem();
		// init voice or video item
		VideoBean newestMediaMessage = MessageLoader
				.getNewestMediaMessage(mContext);
		if (newestMediaMessage != null) {
			isAddVoiceItem = true;
			updateVoiceSpecificItemState(true, newestMediaMessage);
		}

		// init add friend verification item
		switch (isHaveVerificationMessage()) {
		case VERIFICATION_TYPE_FRIEND:
			isAddVerificationItem = true;
			((ConversationFirendAuthenticationData) verificationMessageItemData)
					.setMessageType(ConversationFirendAuthenticationData.VerificationMessageType.CONTACT_TYPE);
			AddFriendHistorieNode tempNode = VerificationProvider
					.getNewestFriendVerificationMessage();
			updateFriendVerificationConversation(tempNode);
			break;
		case VERIFICATION_TYPE_CROWD:
			isAddVerificationItem = true;
			((ConversationFirendAuthenticationData) verificationMessageItemData)
					.setMessageType(ConversationFirendAuthenticationData.VerificationMessageType.CROWD_TYPE);
			updateCrowdVerificationConversation(false);
			break;
		default:
			break;
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.i("20150303 1", "TabFragmentMessage onCreateView()");
		if (rootView == null) {
			rootView = inflater.inflate(R.layout.tab_fragment_conversations,
					container, false);
			mConversationsListView = (ListView) rootView
					.findViewById(R.id.conversations_list_container);
			mConversationsListView.setAdapter(adapter);
			mConversationsListView.setOnItemClickListener(mItemClickListener);
			mConversationsListView
					.setOnItemLongClickListener(mItemLongClickListener);
		}
		return rootView;
	}

	@Override
	public void onDestroy() {
		Log.i("20150303 1", "TabFragmentMessage onDestroy()");
		super.onDestroy();
		if(mChatPlayer != null){
			mChatPlayer.release();
			mChatPlayer = null;
		}
		getActivity().unregisterReceiver(receiver);
		BitmapManager.getInstance().unRegisterBitmapChangedListener(
				this.bitmapChangedListener);
		super.onDestroy();
	}

	@Override
	public void onDestroyView() {
		Log.i("20150303 1", "TabFragmentMessage onDestroyView()");
		super.onDestroyView();
		((ViewGroup) rootView.getParent()).removeView(rootView);
	}

	/**
	 * According to mCurrentTabFlag, initialize different intent filter
	 */
	private void initReceiver() {
		receiver = new ConversationReceiver();
		if (intentFilter == null) {
			intentFilter = new IntentFilter();
			intentFilter.setPriority(IntentFilter.SYSTEM_LOW_PRIORITY);
			intentFilter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
			intentFilter.addCategory(PublicIntent.DEFAULT_CATEGORY);
			intentFilter.addAction(JNIService.JNI_BROADCAST_GROUP_NOTIFICATION);
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION);
			intentFilter
					.addAction(PublicIntent.BROADCAST_USER_COMMENT_NAME_NOTIFICATION);
			intentFilter.addAction(JNIService.JNI_BROADCAST_GROUPS_LOADED);
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_OFFLINE_MESSAGE_END);
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_USER_UPDATE_BASE_INFO);

			intentFilter.addAction(JNIService.JNI_BROADCAST_KICED_CROWD);
			intentFilter
					.addAction(PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION);
			intentFilter.addAction(JNIService.JNI_BROADCAST_GROUP_UPDATED);
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_NEW_DISCUSSION_NOTIFICATION);
			intentFilter
					.addAction(PublicIntent.BROADCAST_DISCUSSION_QUIT_NOTIFICATION);
			intentFilter.addAction(JNIService.JNI_BROADCAST_GROUP_USER_ADDED);
			intentFilter.addAction(JNIService.JNI_BROADCAST_GROUP_USER_REMOVED);

			intentFilter
					.addAction(ConversationP2PAVActivity.P2P_BROADCAST_MEDIA_UPDATE);
			intentFilter.addAction(JNIService.JNI_BROADCAST_NEW_MESSAGE);
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_CONTACTS_AUTHENTICATION);
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_NEW_QUALIFICATION_MESSAGE);
			intentFilter
					.addAction(JNIService.BROADCAST_CROWD_NEW_UPLOAD_FILE_NOTIFICATION);
			intentFilter.addAction(PublicIntent.REQUEST_UPDATE_CONVERSATION);
			intentFilter.addAction(JNIService.JNI_BROADCAST_GROUP_USER_REMOVED);
			intentFilter
					.addAction(PublicIntent.BROADCAST_ADD_OTHER_FRIEND_WAITING_NOTIFICATION);
			intentFilter
					.addAction(PublicIntent.BROADCAST_AUTHENTIC_TO_CONVERSATIONS_TAB_FRAGMENT_NOTIFICATION);
		}

		getActivity().registerReceiver(receiver, intentFilter);
	}

	@Override
	public void onStart() {
		Log.i("20150303 1","TabFragmentMessage onStart()");
		super.onStart();
		if (!isCreate)
			sortAndUpdate();
		isCreate = false;
	}

	@Override
	public void onStop() {
		Log.i("20150303 1","TabFragmentMessage onStop()");
		super.onStop();
	}

	private boolean isFrist = true;

	@Override
	public void afterTextChanged(Editable s) {

		if (TextUtils.isEmpty(s)) {
			SearchUtils.clearAll();
			mIsStartedSearch = SearchUtils.mIsStartedSearch;
			isFrist = true;
			searchList.clear();
			adapter.notifyDataSetChanged();
		} else {
			if (isFrist) {
				SearchUtils.clearAll();
				List<Object> conversations = new ArrayList<Object>();
				for (int i = 0; i < mItemList.size(); i++) {
					conversations.add(mItemList.get(i));
				}
				SearchUtils.receiveList = conversations;
				isFrist = false;
			}

			searchList.clear();
			searchList = SearchUtils.startConversationSearch(s);
			mIsStartedSearch = SearchUtils.mIsStartedSearch;
			adapter.notifyDataSetChanged();
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_UPDATE_CHAT_CONVERSATION && data != null) {
			int groupType = data.getIntExtra("groupType", -1);
			long groupID = data.getLongExtra("groupID", -1);
			long remoteUserID = data.getLongExtra("remoteUserID", -1);

			long conversationID = -1;
			if (V2GlobalConstants.GROUP_TYPE_USER == groupType) {
				conversationID = remoteUserID;
			} else {
				conversationID = groupID;
			}

			boolean isDelete = data.getBooleanExtra("isDelete", false);
			if (isDelete) {
				removeConversation(conversationID, false);
				return;
			}

			VMessage vm = null;
			for (int i = 0; i < mItemList.size(); i++) {
				Conversation cov = mItemList.get(i).cov;
				if (cov.getExtId() == conversationID) {
					cov.setReadFlag(Conversation.READ_FLAG_READ);
					if (V2GlobalConstants.GROUP_TYPE_USER == groupType) {
						vm = MessageLoader.getNewestMessage(mContext,
								GlobalHolder.getInstance().getCurrentUserId(),
								remoteUserID);
					} else {
						vm = MessageLoader.getNewestGroupMessage(mContext,
								groupType, groupID);
					}

					if (vm != null) {
						cov.setDate(vm.getStringDate());
						cov.setDateLong(String.valueOf(vm.getmDateLong()));
						CharSequence msg = MessageUtil
								.getMixedConversationContent(mContext, vm);
						cov.setMsg(msg);
					}

					GroupLayout layout = (GroupLayout) mItemList.get(i).gp;
					layout.update();
					updateUnreadConversation(mItemList.get(i));
					adapter.notifyDataSetChanged();
				}
			}
		}
	}

	private void sortAndUpdate() {
		Collections.sort(mItemList);
		adapter.notifyDataSetChanged();
	}

	private boolean updateVerificationConversation() {
		long crowdTime = 0;
		long friendTime = 0;
		VMessageQualification nestQualification = VerificationProvider
				.getNewestCrowdVerificationMessage();
		AddFriendHistorieNode friendNode = VerificationProvider
				.getNewestFriendVerificationMessage();

		if (nestQualification == null && friendNode == null) {
			V2Log.d(TAG, "没有获取到好友或群组最新的验证消息，删除验证消息会话！");
			removeConversation(verificationItem.cov.getExtId(), false);
			return false;
		}

		if (nestQualification != null) {
			crowdTime = nestQualification.getmTimestamp().getTime();
		}

		if (friendNode != null) {
			friendTime = friendNode.saveDate;
		}

		boolean isCrowd = false;
		if (crowdTime > friendTime) {
			updateCrowdVerificationConversation(true);
			isCrowd = true;
		} else
			updateFriendVerificationConversation(friendNode);
		return isCrowd;
	}

	/**
	 * Load local conversation list
	 */
	private void loadUserConversation() {
		if (isLoadedCov) {
			return;
		}
		service.execute(new Runnable() {

			@Override
			public void run() {
				synchronized (TabFragmentMessage.class) {
					List<Conversation> tempList = new ArrayList<Conversation>();
					tempList = ConversationProvider.loadUserConversation(
							tempList, verificationMessageItemData,
							voiceMessageItem);
					fillUserAdapter(tempList);
				}
			}
		});

	}

	/**
	 * 判断数据库是否有验证消息
	 * 
	 * @return
	 */
	private int isHaveVerificationMessage() {
		int result = -1;

		long crowdTime = 0;
		long friendTime = 0;
		VMessageQualification nestQualification = VerificationProvider
				.getNewestCrowdVerificationMessage();
		AddFriendHistorieNode friendNode = VerificationProvider
				.getNewestFriendVerificationMessage();

		if (nestQualification == null && friendNode == null)
			return result;

		if (nestQualification != null)
			crowdTime = nestQualification.getmTimestamp().getTime();

		if (friendNode != null)
			friendTime = friendNode.saveDate;

		if (crowdTime > friendTime)
			return VERIFICATION_TYPE_CROWD;
		else
			return VERIFICATION_TYPE_FRIEND;
	}

	private void fillUserAdapter(final List<Conversation> list) {
		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				for (int i = 0; i < list.size(); i++) {
					Conversation cov = list.get(i);
					if (cov == null) {
						V2Log.e(TAG,
								"when execute fillUserAdapter , get null Conversation , index :"
										+ i);
						continue;
					}

					switch (cov.getType()) {
					case Conversation.TYPE_VERIFICATION_MESSAGE:
						if (isAddVerificationItem) {
							isAddVerificationItem = false;
							mItemList.add(verificationItem);
						}
						continue;
					case Conversation.TYPE_VOICE_MESSAGE:
						if (isAddVoiceItem) {
							isAddVoiceItem = false;
							mItemList.add(voiceItem);
						}
						continue;
					case Conversation.TYPE_DEPARTMENT:
						((DepartmentConversation) cov).setShowContact(true);
						break;
					case Conversation.TYPE_GROUP:
						((CrowdConversation) cov).setShowContact(true);
						break;
					}

					GroupLayout layout = new GroupLayout(mContext, cov);
					/**
					 * 除了个人Conversation布局，其他组类Conversation布局默认可能不会显示时间，或者内容
					 * 所以这里需要将布局改变为个人的Conversation布局
					 */
					if (cov.getType() == V2GlobalConstants.GROUP_TYPE_DISCUSSION)
						layout.updateDiscussionLayout(true);
					else
						layout.updateCrowdLayout();
					layout.update();
					ScrollItem newItem = new ScrollItem(cov, layout);
					mItemList.add(newItem);
					if (cov.getReadFlag() == Conversation.READ_FLAG_UNREAD) {
						updateUnreadConversation(newItem);
					}
				}
				isLoadedCov = true;
				adapter.notifyDataSetChanged();

				if (!isCallBack) {
					isCallBack = true;
					CommonCallBack.getInstance()
							.executeUpdateConversationState();
				}
			}
		});
	}

	/**
	 * 初始化通话消息item对象
	 */
	private void initVoiceItem() {
		voiceMessageItem = new Conversation(Conversation.TYPE_VOICE_MESSAGE,
				Conversation.SPECIFIC_VOICE_ID);
		voiceMessageItem.setName(res
				.getString(R.string.specificItem_voice_title));
		voiceLayout = new GroupLayout(mContext, voiceMessageItem);
		voiceLayout.update();
		voiceMessageItem.setReadFlag(Conversation.READ_FLAG_READ);
		voiceItem = new ScrollItem(voiceMessageItem, voiceLayout);
	}

	/**
	 * 初始化验证消息item对象
	 */
	private void initVerificationItem() {
		verificationMessageItemData = new ConversationFirendAuthenticationData(
				Conversation.TYPE_VERIFICATION_MESSAGE,
				Conversation.SPECIFIC_VERIFICATION_ID);
		verificationMessageItemData.setName(res
				.getString(R.string.group_create_group_qualification));
		verificationMessageItemLayout = new GroupLayout(mContext,
				verificationMessageItemData);
		verificationMessageItemLayout.update();
		verificationMessageItemData.setReadFlag(Conversation.READ_FLAG_READ);
		verificationItem = new ScrollItem(verificationMessageItemData,
				verificationMessageItemLayout);
	}

	/**
	 * 加载数据库，不显示红点
	 * 
	 * @param isFromDatabase
	 */
	private void updateVoiceSpecificItemState(boolean isFromDatabase,
			VideoBean newestMediaMessage) {

		if (newestMediaMessage != null && newestMediaMessage.startDate != 0) {
			String startDate = DateUtil
					.getStringDate(newestMediaMessage.startDate);
			voiceMessageItem.setDate(startDate);
			voiceMessageItem.setDateLong(String
					.valueOf(newestMediaMessage.startDate));

			if (isFromDatabase) {
				// boolean isShowFlag;
				// if (VoiceProvider.queryIsHaveVoiceMessages())
				// isShowFlag = true;
				// else
				// isShowFlag = false;
				voiceLayout.update(null, startDate, false);
				// if (newestMediaMessage.readSatate ==
				// AudioVideoMessageBean.STATE_UNREAD)
				// updateUnreadVoiceConversation(true);
			} else {
				if (newestMediaMessage.readSatate == AudioVideoMessageBean.STATE_UNREAD) {
					voiceLayout.update(null, startDate, true);
					voiceMessageItem.setReadFlag(Conversation.READ_FLAG_UNREAD);
				} else {
					if (hasUnreadVoice == true) {
						voiceLayout.update(null, startDate, true);
						voiceMessageItem
								.setReadFlag(Conversation.READ_FLAG_UNREAD);
						hasUnreadVoice = false;
					} else {
						voiceLayout.update(null, startDate, false);
						voiceMessageItem
								.setReadFlag(Conversation.READ_FLAG_READ);
					}

				}
			}
		}
	}

	public void updateSearchState() {

		mIsStartedSearch = false;
		searchList.clear();
		adapter.notifyDataSetChanged();
	}

	private ScrollItem makeNewGroupItem(VMessage vm, int groupType, long groupID) {

		Group group = GlobalHolder.getInstance().getGroupById(groupType,
				groupID);
		Conversation cov;
		switch (groupType) {
		case V2GlobalConstants.GROUP_TYPE_DEPARTMENT:
			if (group == null) {
				V2Log.e(TAG,
						"makeNewGroupItem ---> get department is null , id is :"
								+ groupID);
				group = new OrgGroup(groupID, null);
			}
			cov = new DepartmentConversation(group);
			((DepartmentConversation) cov).setShowContact(true);
			break;
		case V2GlobalConstants.GROUP_TYPE_CROWD:
			if (group == null) {
				V2Log.e(TAG,
						"makeNewGroupItem ---> get crowdGroup is null , id is :"
								+ groupID);
				group = new CrowdGroup(groupID, null, null);
			}
			cov = new CrowdConversation(group);
			((CrowdConversation) cov).setShowContact(true);
			break;
		case V2GlobalConstants.GROUP_TYPE_DISCUSSION:
			if (group == null) {
				V2Log.e(TAG,
						"makeNewGroupItem ---> get discussionGroup is null , id is :"
								+ groupID);
				group = new DiscussionGroup(groupID, null, null);
			}
			cov = new DiscussionConversation(group);
			break;
		default:
			throw new RuntimeException(
					"makeNewGroupItem ---> invalid groupType : " + groupType);
		}

		cov.setDate(vm.getStringDate());
		cov.setDateLong(String.valueOf(vm.getmDateLong()));
		CharSequence newMessage = MessageUtil.getMixedConversationContent(
				mContext, vm);
		cov.setMsg(newMessage);
		cov.setReadFlag(Conversation.READ_FLAG_UNREAD);
		ConversationProvider.saveConversation(vm);
		GroupLayout viewLayout = new GroupLayout(mContext, cov);
		viewLayout.update();

		if (groupType == V2GlobalConstants.GROUP_TYPE_DISCUSSION)
			viewLayout.updateDiscussionLayout(true);
		else
			viewLayout.updateCrowdLayout();
		// 添加到ListView中
		V2Log.d(TAG,
				"makeNewGroupItem --> Successfully add a new conversation , type is : "
						+ cov.getType() + " and id is : " + cov.getExtId()
						+ " and name is : " + cov.getName());

		ScrollItem newItem = new ScrollItem(cov, viewLayout);
		return newItem;
	}

	/**
	 * update group type conversation according groupType and groupID
	 * 
	 * @param groupType
	 * @param groupID
	 */
	private void updateGroupConversation(int groupType, long groupID) {
		Log.d(TAG, "update Group Conversation called , type is : " + groupType
				+ "id is : " + groupID);
		if (!isLoadedCov) {
			V2Log.e(TAG,
					"fill adapter isn't finish when update group conversation in USER INTERFACE!");
			return;
		}
		VMessage vm = MessageLoader.getNewestGroupMessage(mContext, groupType,
				groupID);
		if (vm == null) {
			V2Log.e(TAG,
					"update group conversation failed.. Didn't find message "
							+ groupID);
			if (!GlobalHolder.getInstance().isOfflineLoaded()
					&& offlineCov.get((int) groupID) == null)
				offlineCov.put((int) groupID, groupType);
			return;
		}

		ScrollItem currentItem = null;
		Conversation existedCov = null;
		GroupLayout viewLayout = null;
		boolean foundFlag = false;
		for (int i = 0; i < mItemList.size(); i++) {
			Conversation cov = mItemList.get(i).cov;
			if (cov.getExtId() == groupID) {
				foundFlag = true;
				existedCov = cov;
				currentItem = mItemList.get(i);
				V2Log.d(TAG, "find the given conversation");
				viewLayout = (GroupLayout) this.mItemList.get(i).gp;
				break;
			}
		}

		boolean isAdd = false;
		if (foundFlag) {
			Group group = GlobalHolder.getInstance().getGroupById(groupType,
					groupID);
			if (group != null)
				existedCov.setName(group.getName());
			existedCov.setDate(vm.getStringDate());
			existedCov.setDateLong(String.valueOf(vm.getmDateLong()));
			CharSequence newMessage = MessageUtil.getMixedConversationContent(
					mContext, vm);
			existedCov.setMsg(newMessage);
		} else {
			ScrollItem newItem = makeNewGroupItem(vm, groupType, groupID);
			currentItem = newItem;
			existedCov = newItem.cov;
			viewLayout = (GroupLayout) newItem.gp;
			mItemList.add(0, newItem);
			isAdd = true;
		}

		if (vm.getFromUser() == null) {
			V2Log.e(TAG,
					"updateGroupConversation --> update group conversation state failed..."
							+ "becauser VMessage fromUser is null...please checked , group type is : "
							+ groupType + " groupID is :" + groupID);
			return;
		}

		if (GlobalHolder.getInstance().getCurrentUserId() != vm.getFromUser()
				.getmUserId()) {
			// Update status bar
			updateStatusBar(vm);
			existedCov.setReadFlag(Conversation.READ_FLAG_UNREAD);
		} else {
			existedCov.setReadFlag(Conversation.READ_FLAG_READ);
		}

		// Update view
		viewLayout.update();
		// Update unread list
		updateUnreadConversation(currentItem);
		sortAndUpdate();
		if (isAdd)
			scrollToTop();
	}

	/**
	 * Update conversation according to message id and remote user id, This
	 * request call only from new message broadcast or request update
	 * conversation broadcast
	 * 
	 * @param remoteUserID
	 * @param vm
	 */
	private void updateUserConversation(long remoteUserID, long msgId) {

		if (!isLoadedCov) {
			V2Log.e(TAG,
					"fill adapter isn't finish when update group conversation in USER INTERFACE!");
			return;
		}

		VMessage vm = MessageLoader.loadUserMessageById(mContext, remoteUserID,
				msgId);
		if (vm == null) {
			V2Log.e("Didn't find message " + msgId);
			if (!GlobalHolder.getInstance().isOfflineLoaded()
					&& offlineCov.get((int) remoteUserID) == null)
				offlineCov.put((int) remoteUserID,
						V2GlobalConstants.GROUP_TYPE_USER);
			return;
		}

		updateUserConversation(remoteUserID, vm);
	}

	/**
	 * Update conversation according to message id and remote user id, This
	 * request call only from new message broadcast or request update
	 * conversation broadcast
	 * 
	 * @param remoteUserID
	 * @param vm
	 */
	private void updateUserConversation(long remoteUserID, VMessage vm) {
		long extId;
		if (vm.getFromUser().getmUserId() == GlobalHolder.getInstance()
				.getCurrentUserId())
			extId = vm.getToUser().getmUserId();
		else
			extId = vm.getFromUser().getmUserId();

		ScrollItem currentItem = null;
		Conversation existedCov = null;
		GroupLayout viewLayout = null;
		boolean foundFlag = false;
		for (int i = 0; i < mItemList.size(); i++) {
			Conversation cov = mItemList.get(i).cov;
			if (cov.getExtId() == extId) {
				foundFlag = true;
				existedCov = cov;
				currentItem = mItemList.get(i);
				viewLayout = (GroupLayout) mItemList.get(i).gp;
				break;
			}
		}

		boolean isAdd = false;
		/**
		 * foundFlag : true 代表当前ListView中并未包含该Conversation，需要加入数据库。 false
		 * 代表已存在，仅需要展示
		 */
		if (foundFlag) {
			CharSequence mixedContent = MessageUtil
					.getMixedConversationContent(mContext, vm);
			if (TextUtils.isEmpty(mixedContent))
				V2Log.e(TAG, "get mixed content is null , VMessage id is :"
						+ vm.getId());
			existedCov.setMsg(mixedContent);
			existedCov.setDate(vm.getStandFormatDate());
			existedCov.setDateLong(String.valueOf(vm.getmDateLong()));

		} else {
			// 展示到界面
			existedCov = new ContactConversation(GlobalHolder.getInstance()
					.getUser(extId));
			existedCov.setMsg(MessageUtil.getMixedConversationContent(mContext,
					vm));
			existedCov.setDateLong(String.valueOf(vm.getmDateLong()));
			ConversationProvider.saveConversation(vm);
			// 添加到ListView中
			viewLayout = new GroupLayout(mContext, existedCov);

			ScrollItem newItem = new ScrollItem(existedCov, viewLayout);
			currentItem = newItem;
			mItemList.add(0, newItem);
			isAdd = true;
		}

		if (GlobalHolder.getInstance().getCurrentUserId() != vm.getFromUser()
				.getmUserId()) {
			// Update status bar
			updateStatusBar(vm);
			existedCov.setReadFlag(Conversation.READ_FLAG_UNREAD);
		} else {
			existedCov.setReadFlag(Conversation.READ_FLAG_READ);
		}
		// Update view
		viewLayout.update();
		updateUnreadConversation(currentItem);
		sortAndUpdate();
		if (isAdd)
			scrollToTop();
	}

	/**
	 * update verification conversation content
	 * 
	 * @param tempNode
	 */
	private void updateFriendVerificationConversation(
			AddFriendHistorieNode tempNode) {
		updateFriendVerificationConversation(null, tempNode);
	}

	/**
	 * update verification conversation content
	 * 
	 * @param remoteUserName
	 *            为了处理组织外的用户
	 * @param tempNode
	 */
	private void updateFriendVerificationConversation(String remoteUserName,
			AddFriendHistorieNode tempNode) {

		if (tempNode == null) {
			V2Log.e(TAG,
					"update Friend verification conversation failed ... given AddFriendHistorieNode is null");
			return;
		}

		boolean hasUnread = false;
		if (tempNode.readState == ReadState.UNREAD.intValue())
			hasUnread = true;

		String name = null;
		if (remoteUserName != null)
			name = remoteUserName;
		else {
			User user = GlobalHolder.getInstance().getUser(
					tempNode.remoteUserID);
			if (!TextUtils.isEmpty(user.getDisplayName()))
				name = user.getDisplayName();
			else
				name = tempNode.remoteUserNickname;
		}
		String msg = buildFriendVerificationMsg(tempNode, name);
		String date = DateUtil.getStringDate(tempNode.saveDate);
		String dateLong = String.valueOf(tempNode.saveDate);
		if (verificationMessageItemData != null) {
			verificationMessageItemData.setMsg(msg);
			verificationMessageItemData.setDate(date);
			verificationMessageItemData.setDateLong(dateLong);
			ConversationFirendAuthenticationData friend = (ConversationFirendAuthenticationData) verificationMessageItemData;
			friend.setMessageType(ConversationFirendAuthenticationData.VerificationMessageType.CONTACT_TYPE);
			verificationMessageItemLayout.update();
		}

		if (hasUnread) {
			updateVerificationStateBar(msg,
					VerificationMessageType.CONTACT_TYPE);
			verificationMessageItemData
					.setReadFlag(Conversation.READ_FLAG_UNREAD);
			sendVoiceNotify();
		} else {
			if (isShowVerificationNotify) {
				verificationMessageItemData
						.setReadFlag(Conversation.READ_FLAG_UNREAD);
			} else {
				verificationMessageItemData
						.setReadFlag(Conversation.READ_FLAG_READ);
			}
		}

		boolean isAdd = true;
		for (int i = 0; i < mItemList.size(); i++) {
			ScrollItem item = mItemList.get(i);
			if (item.cov.getExtId() == Conversation.SPECIFIC_VERIFICATION_ID) {
				isAdd = false;
				break;
			}
		}

		if (isAdd) {
			V2Log.e(TAG,
					"updateFriendVerificationConversation Successfully Add verificationItem to mItemList!");
			mItemList.add(0, verificationItem);
		}
		verificationItem.cov = verificationMessageItemData;
		updateUnreadConversation(verificationItem);
		sortAndUpdate();
		if (isAdd)
			scrollToTop();
	}

	/**
	 * update verification conversation content
	 * 
	 * @param isFresh
	 *            是否要刷新Conversation会话的状态(红点，声音)
	 */
	private void updateCrowdVerificationConversation(boolean isFresh) {

		VMessageQualification msg = updateCrowdVerificationConversation();
		if (isFresh) {
			if (msg.getReadState() == VMessageQualification.ReadState.UNREAD) {
				verificationMessageItemData
						.setReadFlag(Conversation.READ_FLAG_UNREAD);
			} else {
				verificationMessageItemData
						.setReadFlag(Conversation.READ_FLAG_READ);
			}
		} else {
			if (isShowVerificationNotify)
				verificationMessageItemData
						.setReadFlag(Conversation.READ_FLAG_UNREAD);
			else
				verificationMessageItemData
						.setReadFlag(Conversation.READ_FLAG_READ);
		}
		
		verificationItem.cov = verificationMessageItemData;
		updateUnreadConversation(verificationItem);
		sortAndUpdate();
	}

	private VMessageQualification updateCrowdVerificationConversation() {
		VMessageQualification msg = VerificationProvider
				.getNewestCrowdVerificationMessage();
		if (msg == null) {
			V2Log.e(TAG,
					"update Crowd verification conversation failed ... given VMessageQualification is null");
			return null;
		}

		V2Log.d(TAG,
				"updateCrowdVerificationConversation --> get newest msg cols id is : "
						+ msg.getId());
		String content = "";
		ConversationFirendAuthenticationData verification = (ConversationFirendAuthenticationData) verificationMessageItemData;
		switch (msg.getType()) {
		case CROWD_INVITATION:
			VMessageQualificationInvitationCrowd invitation = (VMessageQualificationInvitationCrowd) msg;
			String invitationName = null;
			User inviteUser = invitation.getInvitationUser();
			CrowdGroup crowdGroup = invitation.getCrowdGroup();
			if (inviteUser == null || crowdGroup == null)
				content = null;
			else {
				if (TextUtils.isEmpty(invitation.getInvitationUser().getDisplayName())) {
					User user = GlobalHolder.getInstance().getUser(
							invitation.getInvitationUser().getmUserId());
					if (!user.isFromService()) {
						invitationName = user.getDisplayName();
					}
				} else {
					User user = invitation.getInvitationUser();
					invitationName = user.getDisplayName();
				}

				String inviteGroupName = crowdGroup.getName();
				if (invitation.getQualState() == QualificationState.BE_ACCEPTED) {
					content = crowdGroup.getName()
							+ res.getString(R.string.conversation_agree_with_your_application);
				} else if ((invitation.getQualState() == QualificationState.BE_REJECT)
						|| (invitation.getQualState() == QualificationState.WAITING_FOR_APPLY)) {
					content = crowdGroup.getName()
							+ res.getString(R.string.conversation_deny_your_application);
				} else {
					content = invitationName
							+ String.format(
									res.getString(R.string.conversation_invite_to_join),
									inviteGroupName);
				}
			}
			break;
		case CROWD_APPLICATION:
			VMessageQualificationApplicationCrowd apply = (VMessageQualificationApplicationCrowd) msg;
			String applyName = null;
			User applyUser = apply.getApplicant();
			CrowdGroup applyGroup = apply.getCrowdGroup();
			if (applyUser == null || applyGroup == null)
				content = null;
			else {
				if (TextUtils.isEmpty(applyUser.getDisplayName())) {
					User user = GlobalHolder.getInstance().getUser(
							apply.getApplicant().getmUserId());
					if (!user.isFromService()) {
						applyName = user.getDisplayName();
					}
				} else {
					User user = apply.getApplicant();
					applyName = user.getDisplayName();
				}

				String applyGroupName = applyGroup.getName();
				if (apply.getQualState() == QualificationState.BE_REJECT)
					content = applyName
							+ String.format(
									res.getString(R.string.conversation_refused_to_join),
									applyGroupName);
				else if (apply.getQualState() == QualificationState.BE_ACCEPTED)
					content = applyName
							+ String.format(
									res.getString(R.string.conversation_agree_to_join),
									applyGroupName);
				else
					content = applyName
							+ String.format(
									res.getString(R.string.crowd_invitation_apply_join),
									applyGroupName);
			}
			break;
		default:
			break;
		}
		verification
				.setMessageType(ConversationFirendAuthenticationData.VerificationMessageType.CROWD_TYPE);
		if (msg.getmTimestamp() == null)
			msg.setmTimestamp(new Date(GlobalConfig.getGlobalServerTime()));
		verificationMessageItemData.setMsg(content);
		verificationMessageItemData.setDate(DateUtil.getStandardDate(msg
				.getmTimestamp()));
		verificationMessageItemData.setDateLong(String.valueOf(msg
				.getmTimestamp().getTime()));
		verificationMessageItemLayout.update();
		verification
				.setMessageType(ConversationFirendAuthenticationData.VerificationMessageType.CROWD_TYPE);
		return msg;
	}

	private void addVerificationConversation(boolean isFirstIndex) {

		if (isLoadedCov) {
			boolean isAdd = true;
			for (int i = 0; i < mItemList.size(); i++) {
				if (mItemList.get(i).cov.getExtId() == verificationMessageItemData
						.getExtId()) {
					isAdd = false;
					break;
				}
			}

			if (isAdd) {
				if (isFirstIndex)
					mItemList.add(0, verificationItem);
				else
					mItemList.add(verificationItem);
				updateVerificationConversation();
				sortAndUpdate();
				scrollToTop();
			}
		}
	}

	/**
	 * Update main activity to show or hide notificator , and update
	 * conversation read state in database
	 * 
	 * @param scrollItem
	 */
	private void updateUnreadConversation(ScrollItem scrollItem) {
		int ret;
		Conversation cov = scrollItem.cov;
		GroupLayout groupLayout = (GroupLayout) scrollItem.gp;
		if (cov.getReadFlag() == Conversation.READ_FLAG_READ) {
			boolean flag = mUnreadConvList.remove(cov);
			if (flag) {
				groupLayout.updateConversationNotificator(false);
				ret = Conversation.READ_FLAG_READ;
			} else {
				// V2Log.e(TAG,
				// "updateUnreadConversation --> REMOVE Conversation failed!");
				return;
			}
		} else {
			boolean flag = mUnreadConvList.add(cov);
			if (flag) {
				groupLayout.updateConversationNotificator(true);
				ret = Conversation.READ_FLAG_UNREAD;
			} else {
				// V2Log.e(TAG,
				// "updateUnreadConversation --> ADD Conversation failed!");
				return;
			}
		}

		if (mUnreadConvList.size() > 0) {
			notificationListener.updateNotificator(Conversation.TYPE_CONTACT,
					true);
		} else {
			notificationListener.updateNotificator(Conversation.TYPE_CONTACT,
					false);
		}
		// update conversation date and flag to database
		ConversationProvider.updateConversationToDatabase(cov, ret);
	}

	/**
	 * update crowd group name in Message Interface
	 */
	private boolean isVerify;

	private void updateMessageGroupName() {
		service.execute(new Runnable() {

			@Override
			public void run() {
				while (!isLoadedCov) {
					SystemClock.sleep(1000);
					V2Log.e(TAG, "waiting for crowd fill adapter ......");
				}

				for (int i = 0; i < mItemList.size(); i++) {
					ScrollItem item = mItemList.get(i);
					V2Log.e(TAG,
							"current iterator conversation item type is : "
									+ item.cov.getType());

					if (item.cov.getType() == Conversation.TYPE_VERIFICATION_MESSAGE) {
						if (isVerify) {
							V2Log.e(TAG, "成功移除重复验证会话！");
							mItemList.remove(item);
						} else
							isVerify = true;
					}

					if (item.cov.getType() == Conversation.TYPE_GROUP) {
						GroupLayout currentGroupLayout = ((GroupLayout) item.gp);
						CrowdConversation crowd = (CrowdConversation) item.cov;
						Group newGroup = GlobalHolder.getInstance()
								.getGroupById(
										V2GlobalConstants.GROUP_TYPE_CROWD,
										crowd.getExtId());
						if (newGroup != null) {
							crowd.setGroup(newGroup);
							crowd.setReadFlag(Conversation.READ_FLAG_READ);
							VMessage vm = MessageLoader.getNewestGroupMessage(
									mContext,
									V2GlobalConstants.GROUP_TYPE_CROWD,
									crowd.getExtId());
							updateGroupInfo(currentGroupLayout, crowd, vm);
						}
					}
				}
				getActivity().runOnUiThread(new Runnable() {

					@Override
					public void run() {
						for (int i = 0; i < mItemList.size(); i++) {
							V2Log.e(TAG,
									"Conversation Type : "
											+ mItemList.get(i).cov.getType());
						}
						isVerify = false;
						sortAndUpdate();
					}
				});
			}
		});
	}

	private void updateGroupInfo(final GroupLayout currentGroupLayout,
			final Conversation con, final VMessage vm) {
		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (vm != null) {
					con.setDate(vm.getStringDate());
					con.setDateLong(String.valueOf(vm.getmDateLong()));
					CharSequence newMessage = MessageUtil
							.getMixedConversationContent(mContext, vm);
					con.setMsg(newMessage);
					currentGroupLayout.update();
				} else
					V2Log.w(TAG, "没有获取到最新VMessage对象! 更新内容失败");
				adapter.notifyDataSetChanged();
				V2Log.e(TAG, "UPDATE GROUP ITEM INFOS SUCCESSFULLY, "
						+ "GROUP TYPE IS : " + con.getType() + " NAME IS :"
						+ con.getName());
			}
		});
	}

	/**
	 * update department group name in Message Interface
	 */
	private void updateDepartmentGroupName() {
		service.execute(new Runnable() {

			@Override
			public void run() {
				while (!isLoadedCov) {
					SystemClock.sleep(1000);
					V2Log.e(TAG, "waiting for department fill adapter ......");
				}

				for (int i = 0; i < mItemList.size(); i++) {
					ScrollItem item = mItemList.get(i);
					if (item.cov.getType() != Conversation.TYPE_DEPARTMENT)
						continue;

					final GroupLayout currentGroupLayout = ((GroupLayout) item.gp);
					final DepartmentConversation department = (DepartmentConversation) item.cov;

					Group newGroup = GlobalHolder.getInstance().getGroupById(
							V2GlobalConstants.GROUP_TYPE_DEPARTMENT,
							department.getExtId());
					if (newGroup != null) {
						department.setDepartmentGroup(newGroup);
						department.setReadFlag(Conversation.READ_FLAG_READ);
						final VMessage vm = MessageLoader
								.getNewestGroupMessage(
										mContext,
										V2GlobalConstants.GROUP_TYPE_DEPARTMENT,
										department.getExtId());
						department.setName(newGroup.getName());
						getActivity().runOnUiThread(new Runnable() {

							@Override
							public void run() {
								if (vm != null) {
									department.setName(department.getName());
									department.setDate(vm.getStringDate());
									department.setDateLong(String.valueOf(vm
											.getmDateLong()));
									CharSequence newMessage = MessageUtil
											.getMixedConversationContent(
													mContext, vm);
									department.setMsg(newMessage);
									currentGroupLayout.update();
								}
								adapter.notifyDataSetChanged();
								V2Log.d(TAG,
										"Successfully updated the DEPARTMENT_GROUP infos , "
												+ "group name is :"
												+ department.getName());
							}
						});
					}
				}
			}
		});
	}

	/**
	 * Remove conversation from mConvList by id.
	 * 
	 * @param conversationID
	 *            <ul>
	 *            <li>ContactConversation : conversationID mean User's id
	 *            <li>ConferenceConversation : conversationID mean
	 *            ConferenceGroup's id
	 *            <li>CrowdConversation : conversationID mean CrowdGroup's id
	 *            <li>DepartmentConversation : conversationID mean OrgGroup's's
	 *            id
	 *            <li>DiscussionConversation : conversationID mean
	 *            DiscussionGroup's's id
	 *            </ul>
	 * @param isDeleteVerification
	 *            在删除会话的时候，是否连带删除验证消息
	 */
	private void removeConversation(long conversationID,
			boolean isDeleteVerification) {
		for (int i = 0; i < mItemList.size(); i++) {
			Conversation temp = mItemList.get(i).cov;
			if (temp.getExtId() == conversationID) {
				ScrollItem removeItem = mItemList.get(i);
				// update readState
				removeItem.cov.setReadFlag(Conversation.READ_FLAG_READ);
				updateUnreadConversation(removeItem);
				// remove item
				ScrollItem removed = mItemList.remove(i);
				if (removed == null)
					V2Log.e(TAG, "Delete Conversation Failed...id is : "
							+ conversationID);

				if (temp.getType() == Conversation.TYPE_VERIFICATION_MESSAGE) {
					// -1 mean delete all messages
					int friend = VerificationProvider
							.deleteFriendVerificationMessage(-1);
					int group = VerificationProvider
							.deleteCrowdVerificationMessage(-1, -1);
					if (friend + group > 0) {
						V2Log.e(TAG,
								"Successfully delete verification , update conversaton!");
					}
				} else if (temp.getType() == Conversation.TYPE_VOICE_MESSAGE) {
					MessageLoader.deleteVoiceMessage(-1);
				} else {
					// delete conversation
					ConversationProvider.deleteConversation(mContext, temp);
					// delete messages
					if (temp.getType() == Conversation.TYPE_CONTACT) {
						MessageLoader.deleteMessageByID(mContext,
								Conversation.TYPE_CONTACT, 0, temp.getExtId(),
								false);
					} else {
						// clear the crowd group all chat database messges
						MessageLoader.deleteMessageByID(mContext,
								temp.getType(), temp.getExtId(), 0, false);
					}
					V2Log.d(TAG,
							" Successfully remove contact conversation , id is : "
									+ conversationID);
				}
				break;
			}
		}

		if (isDeleteVerification) {
			removeVerificationMessage(conversationID);
		}
		sortAndUpdate();
	}

	private void removeVerificationMessage(long id) {
		// clear the crowd group all verification database messges
		int friend = VerificationProvider.deleteFriendVerificationMessage(id);
		int group = VerificationProvider.deleteCrowdVerificationMessage(id, -1);
		if (friend + group > 0) {
			updateVerificationConversation();
			V2Log.e(TAG,
					"Successfully delete verification , update conversaton!");
		}

		// clear the voice messages
		int voices = MessageLoader.deleteVoiceMessage(id);
		boolean voiceflag = VoiceProvider.queryIsHaveVoiceMessages(id);
		if (voices > 0 && !voiceflag)
			mItemList.remove(voiceItem);
	}

	private void updateStatusBar(VMessage vm) {
		if (checkSendingState()) {
			return;
		}

		String content;
		if (vm.getAudioItems().size() > 0) {
			content = mContext.getResources().getString(
					R.string.receive_voice_notification);
		} else if (vm.getImageItems().size() > 0) {
			content = mContext.getResources().getString(
					R.string.receive_image_notification);
		} else if (vm.getFileItems().size() > 0) {
			content = mContext.getResources().getString(
					R.string.receive_file_notification);
		} else {
			StringBuilder sb = new StringBuilder();
			for (VMessageAbstractItem item : vm.getItems()) {
				if (item.getType() == VMessageAbstractItem.ITEM_TYPE_TEXT) {
					if (item.isNewLine() && sb.length() != 0) {
						sb.append("\n");
					}
					sb.append(((VMessageTextItem) item).getText());
				} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_FACE) {
					sb.append(mContext.getResources().getString(
							R.string.receive_face_notification));
				}
			}
			content = sb.toString();
		}
		Intent resultIntent = new Intent(
				PublicIntent.START_CONVERSACTION_ACTIVITY);

		resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		switch (vm.getMsgCode()) {
		case V2GlobalConstants.GROUP_TYPE_USER:
			resultIntent.putExtra("obj", new ConversationNotificationObject(
					Conversation.TYPE_CONTACT, vm.getFromUser().getmUserId()));
			break;
		case V2GlobalConstants.GROUP_TYPE_CROWD:
			resultIntent.putExtra("obj", new ConversationNotificationObject(
					Conversation.TYPE_GROUP, vm.getGroupId()));
			break;
		case V2GlobalConstants.GROUP_TYPE_DEPARTMENT:
			resultIntent.putExtra("obj", new ConversationNotificationObject(
					V2GlobalConstants.GROUP_TYPE_DEPARTMENT, vm.getGroupId()));
			break;
		case V2GlobalConstants.GROUP_TYPE_DISCUSSION:
			resultIntent.putExtra("obj", new ConversationNotificationObject(
					V2GlobalConstants.GROUP_TYPE_DISCUSSION, vm.getGroupId()));
			break;
		default:
			break;
		}
		resultIntent.addCategory(PublicIntent.DEFAULT_CATEGORY);
		if (vm.getMsgCode() == V2GlobalConstants.GROUP_TYPE_USER) {
			Notificator.updateSystemNotification(mContext, vm.getFromUser()
					.getDisplayName(), content, 0, resultIntent,
					PublicIntent.MESSAGE_NOTIFICATION_ID);
		} else {
			Group group = GlobalHolder.getInstance().getGroupById(
					vm.getMsgCode(), vm.getGroupId());
			if (group == null) {
				V2Log.e(TAG,
						"Update ChatMessage Notificator failed ... get Group Object from GlobleHolder is null"
								+ "groupType is : "
								+ vm.getMsgCode()
								+ " groupID is : " + vm.getGroupId());
				return;
			}
			Notificator.updateSystemNotification(mContext, group.getName(),
					content, 0, resultIntent,
					PublicIntent.MESSAGE_NOTIFICATION_ID);
		}
	}

	/**
	 * 更新验证要想系统通知栏
	 * 
	 * @param msg
	 */
	private void updateVerificationStateBar(String msg,
			VerificationMessageType type) {

		if (checkSendingState()) {
			return;
		}

		// 发通知
		Intent i = new Intent(getActivity(),
				MessageAuthenticationActivity.class);
		i = startAuthenticationActivity(i, type);
		Notificator.updateSystemNotification(mContext,
				res.getString(R.string.status_bar_notification), msg, 1, i,
				PublicIntent.MESSAGE_NOTIFICATION_ID);
	}

	private boolean isOutOrgShow;

	/**
	 * 当所有组织信息和组织内用户信息获取完毕后，检测当前验证消息显示的是否是组织外用户的群验证消息。
	 */
	private void checkEmptyVerificationMessage() {
		VerificationMessageType messageType = ((ConversationFirendAuthenticationData) verificationMessageItemData)
				.getMessageType();
		if (messageType == VerificationMessageType.CROWD_TYPE) {
			VMessageQualification crowdVerificationMessage = VerificationProvider
					.getNewestCrowdVerificationMessage();
			if (crowdVerificationMessage != null) {
				long uid = -1;
				if (crowdVerificationMessage.getType() == Type.CROWD_APPLICATION) {
					VMessageQualificationApplicationCrowd applyMsg = (VMessageQualificationApplicationCrowd) crowdVerificationMessage;
					if (applyMsg.getApplicant() != null)
						uid = applyMsg.getApplicant().getmUserId();
				} else {
					VMessageQualificationInvitationCrowd inviteMsg = (VMessageQualificationInvitationCrowd) crowdVerificationMessage;
					if (inviteMsg.getInvitationUser() != null)
						uid = inviteMsg.getInvitationUser().getmUserId();
				}

				if (uid != -1) {
					User remote = GlobalHolder.getInstance().getUser(uid);
					if (remote.isFromService()) {
						// The user info need to get
						isOutOrgShow = true;
						V2Log.e(TAG,
								"The current show Verification info need to update!");
					}
				}
			}
		}
	}

	/**
	 * 登陆后检测数据库里是否存有等待好友验证的消息，并且已经与他成为好友
	 */
	private void checkWaittingFriendExist() {
		List<Long> remoteUsers = VerificationProvider
				.getFriendWaittingVerifyMessage();
		if (remoteUsers != null && remoteUsers.size() > 0) {
			for (int i = 0; i < remoteUsers.size(); i++) {
				V2Log.e(TAG,
						"Waitting add friend id is : " + remoteUsers.get(i));
				boolean isFinish = false;
				User user = GlobalHolder.getInstance().getUser(
						remoteUsers.get(i));
				if (user.isFromService()) {
					break;
				}

				Set<Group> belongsGroup = user.getBelongsGroup();
				Iterator<Group> iterator = belongsGroup.iterator();
				while (iterator.hasNext()) {
					Group next = iterator.next();
					if (next.getGroupType() == GroupType.CONTACT) {
						isFinish = true;
						break;
					}
				}

				if (isFinish) {
					V2Log.e(TAG,
							"发现有等待的好友验证的消息已变为成为好友，用户id : " + user.getmUserId());
					AddFriendHistroysHandler.becomeFriendHanler(mContext, user);
					int update = VerificationProvider
							.updateFriendQualicationReadState(
									user.getmUserId(), ReadState.UNREAD);
					if (update <= 0) {
						V2Log.e(TAG, "更新等待的好友验证失败！");
					}
				}
			}
		}
	}

	/**
	 * 当所有信息都已接收完毕，检测消息界面中存在的讨论组和群会话，它们是否存在，如不删除点击就会报错
	 */
	private void checkGroupIsExist() {
		for (int i = 0; i < mItemList.size(); i++) {
			Conversation cov = mItemList.get(i).cov;
			if (cov.getType() == V2GlobalConstants.GROUP_TYPE_CROWD) {
				Group crowd = GlobalHolder.getInstance().getGroupById(
						V2GlobalConstants.GROUP_TYPE_CROWD, cov.getExtId());
				if (crowd == null)
					Message.obtain(mHandler, REMOVE_CONVERSATION,
							cov.getExtId()).sendToTarget();
			} else if (cov.getType() == V2GlobalConstants.GROUP_TYPE_DISCUSSION) {
				Group crowd = GlobalHolder.getInstance()
						.getGroupById(V2GlobalConstants.GROUP_TYPE_DISCUSSION,
								cov.getExtId());
				if (crowd == null)
					Message.obtain(mHandler, REMOVE_CONVERSATION,
							cov.getExtId()).sendToTarget();
			}
		}
	}

	/**
	 * The verification conversation may be repeated when just user login , So
	 * you need to check it and find out why
	 */
	private void checkRepeatVerification() {
		service.execute(new Runnable() {

			@Override
			public void run() {
				while (!isLoadedCov) {
					SystemClock.sleep(1000);
					V2Log.e(TAG,
							"checkRepeatVerification --> waiting for message interface fill adapter ......");
				}

				boolean isFound = false;
				for (int j = 0; j < mItemList.size(); j++) {
					Conversation cov = mItemList.get(j).cov;
					if (cov.getExtId() == verificationMessageItemData
							.getExtId()) {
						if (isFound == false) {
							isFound = true;
						} else {
							mItemList.remove(j);
							getActivity().runOnUiThread(new Runnable() {

								@Override
								public void run() {
									sortAndUpdate();
								}
							});
						}
					}
				}
			}
		});
	}

	public void sendVoiceNotify() {
		if (mChatPlayer == null)
			mChatPlayer = MediaPlayer.create(mContext, R.raw.chat_audio);
		if (!mChatPlayer.isPlaying())
			mChatPlayer.start();
	}

	/**
	 * 
	 * @return true mean don't sending , false sending
	 */
	private boolean checkSendingState() {
		if (GlobalHolder.getInstance().isInMeeting()
				|| GlobalHolder.getInstance().isInAudioCall()
				|| GlobalHolder.getInstance().isInVideoCall()) {
			return true;
		}
		return false;
	}

	private Intent startAuthenticationActivity(Intent intent,
			VerificationMessageType messageType) {

		if (messageType == VerificationMessageType.CONTACT_TYPE)
			intent.putExtra("isFriendActivity", true);
		else
			intent.putExtra("isFriendActivity", false);

		boolean flag = VerificationProvider.getUNReandMessage(false);
		intent.putExtra("isCrowdShowNotification", flag);
		return intent;
	}

	private void scrollToTop() {
		if (mConversationsListView == null)
			return;
		mConversationsListView.post(new Runnable() {

			@Override
			public void run() {
				mConversationsListView.setSelection(0);
			}
		});
	}

	class ConversationList {

		public ConversationList(List<Conversation> conversationList) {
			this.conversationList = conversationList;
		}

		public List<Conversation> conversationList;
	}

	class CovCache {

		public int groupType;
		public long groupId;

		public CovCache(int groupType, long groupId) {
			this.groupType = groupType;
			this.groupId = groupId;
		}
	}

	private OnItemClickListener mItemClickListener = new OnItemClickListener() {

		long lasttime;

		@Override
		public void onItemClick(AdapterView<?> adapters, View v, int pos,
				long id) {
			if (System.currentTimeMillis() - lasttime < 300) {
				V2Log.w("Too short pressed");
				return;
			}

			currentClickConversation = mItemList.get(pos).cov;
			ScrollItem item = null;
			if (mIsStartedSearch)
				item = searchList.get(pos);
			else
				item = mItemList.get(pos);
			Conversation cov = item.cov;
			if (cov.getType() == Conversation.TYPE_VOICE_MESSAGE) {
				Intent intent = new Intent(mContext, VoiceMessageActivity.class);
				startActivity(intent);
			} else if (cov.getType() == Conversation.TYPE_VERIFICATION_MESSAGE) {
				isShowVerificationNotify = false;
				Intent intent = new Intent(mContext,
						MessageAuthenticationActivity.class);
				VerificationMessageType messageType = ((ConversationFirendAuthenticationData) verificationMessageItemData)
						.getMessageType();
				intent = startAuthenticationActivity(intent, messageType);
				startActivity(intent);
			} else {
				Intent i = new Intent(PublicIntent.START_CONVERSACTION_ACTIVITY);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				i.putExtra(
						"obj",
						new ConversationNotificationObject(cov.getType(), cov
								.getExtId()));
				startActivityForResult(i, REQUEST_UPDATE_CHAT_CONVERSATION);
			}

			// update main activity notificator
			cov.setReadFlag(Conversation.READ_FLAG_READ);
			updateUnreadConversation(item);
		}
	};

	private OnItemLongClickListener mItemLongClickListener = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> adapter, View v, int pos,
				long id) {
			String[] item;
			currentClickConversation = mItemList.get(pos).cov;
			// Conversation.TYPE_CONTACT
			item = new String[] { mContext.getResources().getString(
					R.string.conversations_delete_conversaion) };

			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setTitle(mItemList.get(pos).cov.getName()).setItems(item,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {

							if (!GlobalHolder.getInstance().isServerConnected()) {
								dialog.dismiss();
								return;
							}

							if (which == 0) {
								removeConversation(
										currentClickConversation.getExtId(),
										false);
							}
							dialog.dismiss();
						}
					});
			AlertDialog ad = builder.create();
			ad.show();
			return true;
		}
	};

	private BitmapManager.BitmapChangedListener bitmapChangedListener = new BitmapManager.BitmapChangedListener() {

		@Override
		public void notifyAvatarChanged(User user, Bitmap bm) {
			for (ScrollItem item : mItemList) {
				if (Conversation.TYPE_CONTACT == item.cov.getType()
						&& item.cov.getExtId() == user.getmUserId()) {
					((GroupLayout) item.gp).updateIcon(bm);
				}
			}

		}
	};

	class RemoveConversationRequest {
		long id;
		String type;

		public RemoveConversationRequest(long id, String type) {
			super();
			this.id = id;
			this.type = type;
		}

	}

	class ConversationsAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			if (mIsStartedSearch)
				return searchList == null ? 0 : searchList.size();
			else
				return mItemList.size();
		}

		@Override
		public Object getItem(int position) {
			if (mIsStartedSearch)
				return searchList.get(position);
			else
				return mItemList.get(position);
		}

		@Override
		public long getItemId(int position) {
			if (mIsStartedSearch)
				return searchList.get(position).cov.getExtId();
			else
				return mItemList.get(position).cov.getExtId();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (mIsStartedSearch) {
				return searchList.get(position).gp;
			} else {
				ScrollItem scrollItem = mItemList.get(position);
				Conversation cov = scrollItem.cov;
				if (cov.getType() == Conversation.TYPE_CONTACT) {
					Bitmap bp = ((ContactConversation) cov).getU()
							.getAvatarBitmap();
					GroupLayout gl = (GroupLayout) scrollItem.gp;
					gl.updateIcon(bp);
				}
				updateUnreadConversation(scrollItem);
				return mItemList.get(position).gp;
			}
		}

	}

	class CommonReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (JNIService.JNI_BROADCAST_GROUP_NOTIFICATION.equals(intent
					.getAction())) {
				int type = intent.getExtras().getInt("gtype");

				if (((type == GroupType.CHATING.intValue())) && !isUpdateGroup) {
					isUpdateGroup = true;
					updateMessageGroupName();
				} else if (type == GroupType.ORG.intValue()
						&& !isUpdateDeparment) {
					isUpdateDeparment = true;
					updateDepartmentGroupName();
				}
				// From this broadcast, user has already read conversation
			} else if (JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION
					.equals(intent.getAction())) {
				for (int i = 0; i < mItemList.size(); i++) {
					ScrollItem item = mItemList.get(i);
					GroupLayout currentGroupLayout = ((GroupLayout) item.gp);
					Conversation currentConversation = item.cov;
					V2Log.d("DISCUSSION",
							"current iterator conversation id is : " + ""
									+ currentConversation.getExtId()
									+ " type is : "
									+ currentConversation.getType());
					switch (currentConversation.getType()) {
					case Conversation.TYPE_VERIFICATION_MESSAGE:
						ConversationFirendAuthenticationData verification = ((ConversationFirendAuthenticationData) currentConversation);
						if (verification.getMessageType() == VerificationMessageType.CROWD_TYPE) {
							Message msg = Message.obtain(mHandler,
									UPDATE_VERIFICATION_MESSAGE);
							msg.arg1 = verification.getMessageType().intValue();
							msg.sendToTarget();
						} else {
							Message msg = Message.obtain(mHandler,
									UPDATE_VERIFICATION_MESSAGE);
							msg.arg1 = verification.getMessageType().intValue();
							msg.sendToTarget();
						}
						break;
					case Conversation.TYPE_CONTACT:
						ContactConversation contact = ((ContactConversation) currentConversation);
						User user = GlobalHolder.getInstance().getUser(
								contact.getExtId());
						if (user != null) {
							contact.updateUser(user);
							currentGroupLayout.update();
							adapter.notifyDataSetChanged();
							V2Log.w(TAG,
									"Successfully updated the user infos , user name is :"
											+ user.getDisplayName());
						} else {
							V2Log.d(TAG,
									"update the user infos failed ... beacuse get user is null from globleHolder! id is : "
											+ contact.getExtId());
						}
						break;
					case Conversation.TYPE_DEPARTMENT:
						Group depart = GlobalHolder.getInstance().getGroupById(
								Conversation.TYPE_DEPARTMENT,
								currentConversation.getExtId());
						((DepartmentConversation) currentConversation)
								.setDepartmentGroup(depart);
						currentGroupLayout.update();
						V2Log.d(TAG,
								"update department group successful , id is : "
										+ currentConversation.getExtId()
										+ " name is : "
										+ currentConversation.getName());
						break;
					case Conversation.TYPE_DISCUSSION:
						DiscussionConversation discussion = (DiscussionConversation) currentConversation;
						Group newGroup = GlobalHolder
								.getInstance()
								.getGroupById(
										V2GlobalConstants.GROUP_TYPE_DISCUSSION,
										discussion.getExtId());
						if (newGroup != null) {
							discussion.setDiscussionGroup(newGroup);
							VMessage vm = MessageLoader.getNewestGroupMessage(
									mContext,
									V2GlobalConstants.GROUP_TYPE_DISCUSSION,
									discussion.getExtId());
							if (vm != null) {
								discussion.setDate(vm.getStringDate());
								discussion.setDateLong(String.valueOf(vm
										.getmDateLong()));
								CharSequence newMessage = MessageUtil
										.getMixedConversationContent(mContext,
												vm);
								discussion.setMsg(newMessage);
								currentGroupLayout.update();
							} else
								V2Log.w("DISCUSSION",
										"没有获取到最新VMessage对象! 更新内容失败");
							V2Log.e("DISCUSSION",
									"update discussion group successful , id is : "
											+ currentConversation.getExtId()
											+ " name is : "
											+ currentConversation.getName());
						} else {
							V2Log.w(TAG, "没有获取到讨论组对象! 更新失败！ id is : "
									+ discussion.getExtId());
						}
						break;
					case Conversation.TYPE_GROUP:
						Group crowd = GlobalHolder.getInstance().getGroupById(
								Conversation.TYPE_GROUP,
								currentConversation.getExtId());
						((CrowdConversation) currentConversation)
								.setGroup(crowd);
						currentGroupLayout.update();
						V2Log.d(TAG, "update crowd group successful , id is : "
								+ currentConversation.getExtId()
								+ " name is : " + currentConversation.getName());
						break;
					}
				}
			} else if (JNIService.JNI_BROADCAST_OFFLINE_MESSAGE_END
					.equals(intent.getAction())) {
				GlobalHolder.getInstance().setOfflineLoaded(true);
				V2Log.d(TAG,
						"All offline messages has received. Globle flag change to true!");
				for (int i = 0; i < offlineCov.size(); i++) {
					long key = offlineCov.keyAt(i);
					V2Log.e(TAG, "off line conversaion id is : " + key);
					int value = offlineCov.valueAt(i);
					if (value == V2GlobalConstants.GROUP_TYPE_USER) {
						VMessage vm = MessageLoader.getNewestMessage(mContext,
								GlobalHolder.getInstance().getCurrentUserId(),
								key);
						updateUserConversation(key, vm);
					} else {
						updateGroupConversation(value, key);
					}
				}
			} else if (JNIService.JNI_BROADCAST_GROUPS_LOADED.equals(intent
					.getAction())) {
				GlobalHolder.getInstance().setGroupLoaded();
				V2Log.d(TAG, "All group and group user info has received. Globle flag change to true!");
				checkWaittingFriendExist();
				checkGroupIsExist();
				checkEmptyVerificationMessage();
				checkRepeatVerification();
			}
		}
	}

	class ConversationReceiver extends CommonReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			super.onReceive(context, intent);
			String action = intent.getAction();
			if (action.equals(JNIService.JNI_BROADCAST_NEW_MESSAGE)) {
				sendVoiceNotify();
				MessageObject msgObj = intent.getParcelableExtra("msgObj");
				Message.obtain(mHandler, NEW_MESSAGE_UPDATE, msgObj)
						.sendToTarget();
			} else if (action
					.equals(JNIService.JNI_BROADCAST_CONTACTS_AUTHENTICATION)) {
				long uid = intent.getLongExtra("uid", -1);
				if (uid == -1)
					return;

				AddFriendHistorieNode node = VerificationProvider
						.getNewestFriendVerificationMessage();
				if (node == null) {
					V2Log.d(TAG,
							"update friend verification message content failed... get null");
					return;
				}

				V2Log.d(TAG,
						"having new friend verification message coming ... update..");

				isShowVerificationNotify = true;
				ConversationFirendAuthenticationData verification = ((ConversationFirendAuthenticationData) verificationMessageItemData);
				verification
						.setMessageType(VerificationMessageType.CONTACT_TYPE);

				boolean isOutORG = intent.getBooleanExtra("isOutORG", false);
				if (isOutORG) {
					BoUserInfoBase v2User = intent.getParcelableExtra("v2User");
					updateFriendVerificationConversation(v2User.getNickName(), node);
				} else
					updateFriendVerificationConversation(node);
			} else if (JNIService.JNI_BROADCAST_NEW_QUALIFICATION_MESSAGE
					.equals(intent.getAction())) {
				long msgId = intent.getLongExtra("msgId", 0);
				if (msgId == 0l) {
					V2Log.d(TAG,
							"update crowd verification message content failed... get 0 message id");
					return;
				}

				V2Log.d(TAG,
						"having new crowd verification message coming ... update..");
				updateCrowdVerificationConversation();
				sendVoiceNotify();
				updateVerificationStateBar(verificationMessageItemData
						.getMsg().toString(),
						VerificationMessageType.CROWD_TYPE);

				isShowVerificationNotify = true;
				ConversationFirendAuthenticationData verification = ((ConversationFirendAuthenticationData) verificationMessageItemData);
				verification
						.setMessageType(VerificationMessageType.CONTACT_TYPE);

				Message msg = Message.obtain(mHandler,
						UPDATE_VERIFICATION_MESSAGE);
				msg.arg1 = VerificationMessageType.CROWD_TYPE.intValue();
				msg.sendToTarget();
			} else if (PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION
					.equals(intent.getAction())
					|| intent.getAction().equals(
							JNIService.JNI_BROADCAST_KICED_CROWD)) {
				GroupUserObject obj = intent.getParcelableExtra("group");
				if (obj == null) {
					V2Log.e(TAG,
							"Received the broadcast to quit the crowd group , but crowd id is wroing... ");
					return;
				}

				GlobalHolder.getInstance().removeGroup(
						GroupType.fromInt(obj.getmType()), obj.getmGroupId());
				removeConversation(obj.getmGroupId(), true);
			} else if ((JNIService.BROADCAST_CROWD_NEW_UPLOAD_FILE_NOTIFICATION
					.equals(intent.getAction()))) {
				long groupID = intent.getLongExtra("groupID", -1l);
				if (groupID == -1l) {
					V2Log.e(TAG,
							"May receive new group upload files failed.. get empty collection");
					return;
				}

				updateGroupConversation(V2GlobalConstants.GROUP_TYPE_CROWD,
						groupID);
			} else if (PublicIntent.BROADCAST_USER_COMMENT_NAME_NOTIFICATION
					.equals(intent.getAction())) {
				Long uid = intent.getLongExtra("modifiedUser", -1);
				if (uid == -1l) {
					V2Log.e("ConversationsTabFragment BROADCAST_USER_COMMENT_NAME_NOTIFICATION ---> update user comment name failed , get id is -1");
					return;
				}

				for (int i = 0; i < mItemList.size(); i++) {
					Conversation conversation = mItemList.get(i).cov;
					if (conversation.getType() == V2GlobalConstants.GROUP_TYPE_USER) {
						GroupLayout layout = (GroupLayout) mItemList.get(i).gp;
						layout.update();
					} else if (conversation.getType() == Conversation.TYPE_VERIFICATION_MESSAGE) {
						updateVerificationConversation();
					}
				}
				adapter.notifyDataSetChanged();
			} else if (JNIService.JNI_BROADCAST_GROUP_USER_ADDED.equals(intent
					.getAction())) {
				GroupUserObject guo = (GroupUserObject) intent.getExtras().get(
						"obj");
				if (guo == null) {
					V2Log.e(TAG,
							"JNI_BROADCAST_GROUP_USER_ADDED --> Received the broadcast to quit the crowd group , but crowd id is wroing... ");
					return;
				}

				for (ScrollItem item : mItemList) {
					Conversation con = item.cov;
					if (con.getExtId() == guo.getmUserId()) {
						GroupLayout currentGroupLayout = ((GroupLayout) item.gp);
						currentGroupLayout.update();
						adapter.notifyDataSetChanged();
						break;
					}
				}
			} else if (JNIService.JNI_BROADCAST_GROUP_USER_REMOVED
					.equals(intent.getAction())) {
				GroupUserObject guo = (GroupUserObject) intent.getExtras().get(
						"obj");

				if (guo == null) {
					V2Log.e(TAG,
							"JNI_BROADCAST_GROUP_USER_REMOVED --> Update Conversation failed that the user removed ... given GroupUserObject is null");
					return;
				}

				if (guo.getmType() == V2GlobalConstants.GROUP_TYPE_CROWD) {
					VerificationProvider.deleteCrowdVerificationMessage(
							guo.getmGroupId(), guo.getmUserId());
					// clear the voice messages
					int voices = MessageLoader.deleteVoiceMessage(guo
							.getmUserId());
					boolean voiceflag = VoiceProvider
							.queryIsHaveVoiceMessages(guo.getmUserId());
					if (voices > 0 && !voiceflag)
						mItemList.remove(voiceItem);
				} else if (guo.getmType() == V2GlobalConstants.GROUP_TYPE_CONTACT) {
					VerificationProvider.deleteFriendVerificationMessage(guo
							.getmUserId());
				}
				updateVerificationConversation();
				removeConversation(guo.getmUserId(), false);
			} else if (PublicIntent.BROADCAST_ADD_OTHER_FRIEND_WAITING_NOTIFICATION
					.equals(intent.getAction())) {
				addVerificationConversation(true);
				Message msg = Message.obtain(mHandler,
						UPDATE_VERIFICATION_MESSAGE);
				msg.arg1 = VerificationMessageType.CONTACT_TYPE.intValue();
				msg.sendToTarget();
			} else if (JNIService.JNI_BROADCAST_GROUP_UPDATED.equals(intent
					.getAction())) {
				long gid = intent.getLongExtra("gid", 0);
				Group g = GlobalHolder.getInstance().getGroupById(gid);
				if (g == null) {
					V2Log.e(TAG,
							"Update Group Infos Failed... Because get null goup , id is : "
									+ gid);
					return;
				}

				if (g.getGroupType() == GroupType.CHATING
						|| g.getGroupType() == GroupType.DISCUSSION) {
					for (int i = 0; i < mItemList.size(); i++) {
						ScrollItem item = mItemList.get(i);
						if (item.cov.getExtId() == g.getmGId()) {
							((GroupLayout) item.gp).update();
						}
					}
				}
				updateVerificationConversation();
			} else if (PublicIntent.REQUEST_UPDATE_CONVERSATION.equals(intent
					.getAction())) {
				// 来自验证界面
				boolean isAuthen = intent.getBooleanExtra("isAuthen", false);
				if (isAuthen) {
					updateVerificationConversation();
					return;
				}

				ConversationNotificationObject uao = (ConversationNotificationObject) intent
						.getExtras().get("obj");
				if (uao == null) {
					return;
				}

				boolean fromCrowdTab = intent.getBooleanExtra("fromCrowdTab",
						false);
				if (fromCrowdTab) {
					for (ScrollItem scrollItem : mItemList) {
						Conversation cov = scrollItem.cov;
						if (uao.getConversationType() == cov.getType()
								&& uao.getExtId() == cov.getExtId()) {
							VMessage vm = MessageLoader.getNewestGroupMessage(
									mContext, cov.getType(), cov.getExtId());
							if (vm != null) {
								cov.setDate(vm.getStringDate());
								cov.setDateLong(String.valueOf(vm
										.getmDateLong()));
								CharSequence newMessage = MessageUtil
										.getMixedConversationContent(mContext,
												vm);
								cov.setMsg(newMessage);
								cov.setReadFlag(Conversation.READ_FLAG_READ);
								ConversationProvider
										.updateConversationToDatabase(cov,
												Conversation.READ_FLAG_READ);
								GroupLayout layout = (GroupLayout) scrollItem.gp;
								layout.update();
								updateUnreadConversation(scrollItem);
							}
							break;
						}
					}
					return;
				}

				// delete Empty message conversation
				if (uao.isDeleteConversation()) {
					removeConversation(uao.getExtId(), false);
					return;
				}

				Message.obtain(mHandler, UPDATE_CONVERSATION, uao)
						.sendToTarget();
			} else if (ConversationP2PAVActivity.P2P_BROADCAST_MEDIA_UPDATE
					.equals(intent.getAction())) {
				long remoteID = intent.getLongExtra("remoteID", -1l);
				if (remoteID == -1l) {
					Log.e(TAG, "get remoteID is -1 ... update failed!!");
					return;
				}

				boolean unread = intent.getBooleanExtra("hasUnread", false);
				if (unread)
					hasUnreadVoice = true;

				String selections = ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_REMOTE_USER_ID
						+ "= ? ";
				String[] selectionArgs = new String[] { String
						.valueOf(remoteID) };
				VideoBean newestMediaMessage = MessageLoader
						.getNewestMediaMessage(mContext, selections,
								selectionArgs);
				if (newestMediaMessage == null) {
					Log.e(TAG, "get newest remoteID " + remoteID
							+ " --> VideoBean is NULL ... update failed!!");
					return;
				}
				sendVoiceNotify();
				updateVoiceSpecificItemState(false, newestMediaMessage);
				updateUnreadConversation(voiceItem);
				mItemList.remove(voiceItem);
				mItemList.add(0, voiceItem);
				adapter.notifyDataSetChanged();
				scrollToTop();
			} else if (JNIService.JNI_BROADCAST_USER_UPDATE_BASE_INFO
					.equals(intent.getAction())) {
				if (isOutOrgShow) {
					isOutOrgShow = false;
					updateCrowdVerificationConversation(false);
				}
			} else if (PublicIntent.BROADCAST_AUTHENTIC_TO_CONVERSATIONS_TAB_FRAGMENT_NOTIFICATION
					.equals(intent.getAction())) {
				int tabType = intent.getIntExtra("tabType", -1);
				boolean isOtherShowPrompt = intent.getBooleanExtra(
						"isOtherShowPrompt", false);
				if (tabType != -1) {
					if (tabType == MessageAuthenticationActivity.PROMPT_TYPE_FRIEND) {
						VerificationProvider
								.updateCrowdAllQualicationMessageReadStateToRead(false);
						if (isOtherShowPrompt) {
							VerificationProvider
									.updateCrowdAllQualicationMessageReadStateToRead(true);
						}
					} else {
						VerificationProvider
								.updateCrowdAllQualicationMessageReadStateToRead(true);
						if (isOtherShowPrompt) {
							VerificationProvider
									.updateCrowdAllQualicationMessageReadStateToRead(false);
						}
					}
				}
				// 更新验证会话最新内容
				updateVerificationConversation();
				// 更新红点
				if (isOtherShowPrompt) {
					verificationItem.cov
							.setReadFlag(Conversation.READ_FLAG_UNREAD);
				} else {
					verificationItem.cov
							.setReadFlag(Conversation.READ_FLAG_READ);
				}
				isShowVerificationNotify = false;
				updateUnreadConversation(verificationItem);
			}
		}
	}

	/**
	 * 构建好友验证信息显示的内容
	 * 
	 * @param tempNode
	 * @param name
	 * @return
	 */
	private String buildFriendVerificationMsg(AddFriendHistorieNode tempNode,
			String name) {
		String content = null;
		// 别人加我：允许任何人：0已添加您为好友，需要验证：1未处理，2已同意，3已拒绝
		// 我加别人：允许认识人：4你们已成为了好友，需要验证：5等待对方验证，4被同意（你们已成为了好友），6拒绝了你为好友
		if ((tempNode.fromUserID == tempNode.remoteUserID)
				&& (tempNode.ownerAuthType == 0)) {// 别人加我允许任何人
			content = String.format(res.getString(R.string.friend_has_added),
					name);
		} else if ((tempNode.fromUserID == tempNode.remoteUserID)
				&& (tempNode.ownerAuthType == 1)) {// 别人加我不管我有没有处理
			content = String.format(
					res.getString(R.string.friend_apply_add_you_friend), name);
		} else if ((tempNode.fromUserID == tempNode.ownerUserID)
				&& (tempNode.addState == 0)) {// 我加别人等待验证
			content = String.format(
					res.getString(R.string.friend_apply_add_waiting_verify),
					name);
		} else if ((tempNode.fromUserID == tempNode.ownerUserID)
				&& (tempNode.addState == 1)) {// 我加别人已被同意或我加别人不需验证
			content = String.format(res.getString(R.string.friend_relation),
					name);
		} else if ((tempNode.fromUserID == tempNode.ownerUserID)
				&& (tempNode.addState == 2)) {// 我加别人已被拒绝
			content = String.format(
					res.getString(R.string.friend_was_reject_apply), name);
		}
		return content;
	}

	class LocalHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case FILL_CONFS_LIST:
				if (!isLoadedCov) {
					loadUserConversation();
				}
				break;
			case UPDATE_CONVERSATION:
				ConversationNotificationObject uno = (ConversationNotificationObject) msg.obj;
				if (uno == null)
					return;
				long target = uno.getExtId();
				for (int i = 0; i < mItemList.size(); i++) {
					Conversation cov = mItemList.get(i).cov;
					if (cov.getExtId() == target) {
						cov.setReadFlag(Conversation.READ_FLAG_READ);
						GroupLayout layout = (GroupLayout) mItemList.get(i).gp;
						layout.update();
						updateUnreadConversation(mItemList.get(i));
						adapter.notifyDataSetChanged();
						return;
					}
				}
				break;
			case NEW_MESSAGE_UPDATE:
				MessageObject msgObj = (MessageObject) msg.obj;
				int groupType = msgObj.groupType;
				long groupID = msgObj.remoteGroupID;
				long remoteID = msgObj.rempteUserID;
				long msgID = msgObj.messageColsID;
				if (groupType == V2GlobalConstants.GROUP_TYPE_USER) {
					updateUserConversation(remoteID, msgID);
				} else {
					updateGroupConversation(groupType, groupID);
				}
				break;
			case REMOVE_CONVERSATION:
				long extId = (Long) msg.obj;
				removeConversation(extId, true);
				break;
			case UPDATE_VERIFICATION_MESSAGE:
				addVerificationConversation(true);
				if (msg.arg1 == VerificationMessageType.CROWD_TYPE.intValue()) {
					updateCrowdVerificationConversation(true);
				} else {
					AddFriendHistorieNode node = VerificationProvider
							.getNewestFriendVerificationMessage();
					if (node != null)
						updateFriendVerificationConversation(node);
					else
						V2Log.e(TAG,
								"UPDATE_VERIFICATION_MESSAGE --> Update Friend Conversation Failed...Get"
										+ "Newest is null");
				}
				break;
			}
		}
	}

	@Override
	public void updateConversationToCreate(int groupType, long groupID,
			long remoteUserID) {
		for (int i = 0; i < mItemList.size(); i++) {
			Conversation cov = mItemList.get(i).cov;
			if (cov.getType() == V2GlobalConstants.GROUP_TYPE_USER) {
				if (cov.getExtId() == remoteUserID) {
					return;
				}
			} else {
				if (cov.getExtId() == groupID) {
					return;
				}
			}
		}

		ScrollItem newItem = null;
		if (V2GlobalConstants.GROUP_TYPE_USER == groupType) {
			VMessage vm = MessageLoader.getNewestMessage(mContext, GlobalHolder
					.getInstance().getCurrentUserId(), remoteUserID);
			if (vm == null)
				return;
			ContactConversation contact = new ContactConversation(GlobalHolder
					.getInstance().getUser(remoteUserID));
			contact.setMsg(MessageUtil
					.getMixedConversationContent(mContext, vm));
			contact.setDateLong(String.valueOf(vm.getmDateLong()));
			contact.setReadFlag(Conversation.READ_FLAG_READ);
			ConversationProvider.saveConversation(vm);
			// 添加到ListView中
			GroupLayout viewLayout = new GroupLayout(mContext, contact);
			newItem = new ScrollItem(contact, viewLayout);
		} else {
			VMessage vm = MessageLoader.getNewestGroupMessage(mContext,
					groupType, groupID);
			if (vm == null)
				return;
			newItem = makeNewGroupItem(vm, groupType, groupID);
			if (newItem == null) {
				V2Log.e(TAG,
						"updateConversationToCreate --> make new group item failed!");
				return;
			}
			newItem.cov.setReadFlag(Conversation.READ_FLAG_READ);
		}
		mItemList.add(0, newItem);
		adapter.notifyDataSetChanged();
	}
}
