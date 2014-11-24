package com.v2tech.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.V2.jni.V2GlobalEnum;
import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.db.ContentDescriptor;
import com.v2tech.db.ConversationProvider;
import com.v2tech.db.DataBaseContext;
import com.v2tech.service.BitmapManager;
import com.v2tech.service.ConferencMessageSyncService;
import com.v2tech.service.ConferenceService;
import com.v2tech.service.CrowdGroupService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.MessageListener;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.service.jni.RequestEnterConfResponse;
import com.v2tech.util.DateUtil;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.MessageUtil;
import com.v2tech.util.Notificator;
import com.v2tech.util.SearchUtils;
import com.v2tech.view.bo.ConversationNotificationObject;
import com.v2tech.view.bo.GroupUserObject;
import com.v2tech.view.conference.GroupLayout;
import com.v2tech.view.conference.VideoActivityV2;
import com.v2tech.view.contacts.VoiceMessageActivity;
import com.v2tech.view.contacts.add.AddFriendHistroysHandler;
import com.v2tech.view.conversation.CommonCallBack;
import com.v2tech.view.conversation.ConversationP2PAVActivity;
import com.v2tech.view.conversation.MessageAuthenticationActivity;
import com.v2tech.view.conversation.MessageBuilder;
import com.v2tech.view.conversation.MessageLoader;
import com.v2tech.vo.AddFriendHistorieNode;
import com.v2tech.vo.AudioVideoMessageBean;
import com.v2tech.vo.Conference;
import com.v2tech.vo.ConferenceConversation;
import com.v2tech.vo.ConferenceGroup;
import com.v2tech.vo.ContactConversation;
import com.v2tech.vo.Conversation;
import com.v2tech.vo.ConversationFirendAuthenticationData;
import com.v2tech.vo.ConversationFirendAuthenticationData.VerificationMessageType;
import com.v2tech.vo.CrowdConversation;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.DepartmentConversation;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.OrgGroup;
import com.v2tech.vo.User;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.VMessageQualification;
import com.v2tech.vo.VMessageQualification.QualificationState;
import com.v2tech.vo.VMessageQualificationApplicationCrowd;
import com.v2tech.vo.VMessageQualificationInvitationCrowd;
import com.v2tech.vo.VideoBean;

public class ConversationsTabFragment extends Fragment implements TextWatcher,
		ConferenceListener {
	private static final int FILL_CONFS_LIST = 2;
	private static final int UPDATE_USER_SIGN = 8;
	private static final int UPDATE_CONVERSATION = 9;
	private static final int UPDATE_SEARCHED_LIST = 11;
	private static final int REMOVE_CONVERSATION = 12;
	private static final int REQUEST_ENTER_CONF = 14;
	private static final int REQUEST_ENTER_CONF_RESPONSE = 15;

	private static final int CONFERENCE_ENTER_CODE = 100;
	private static final String TAG = "ConversationsTabFragment";
	private static final int UPDATE_CONVERSATION_MESSAGE = 16;
	private static final int UPDATE_VERIFICATION_MESSAGE = 17;

	private static final int VERIFICATION_TYPE_FRIEND = 5;
	private static final int VERIFICATION_TYPE_CROWD = 6;

	private View rootView;

	private Context mContext;

	private NotificationListener notificationListener;

	private BroadcastReceiver receiver;
	private IntentFilter intentFilter;

	private ConferenceService cb;
	private CrowdGroupService chatService;

	private List<Conversation> mConvList;
	private Set<Conversation> mUnreadConvList;

	private List<ScrollItem> mItemList = new ArrayList<ScrollItem>();
	private List<ScrollItem> searchList = new ArrayList<ScrollItem>();

	private LocalHandler mHandler = new LocalHandler();

	/**
	 * This tag is used to limit the database load times
	 */
	private boolean isLoadedCov;
	private boolean mIsStartedSearch;
	private boolean showAuthenticationNotification;
	private boolean isUpdateGroup;
	private boolean isUpdateDeparment;
	private boolean isCallBack;
	private boolean isCreate;

	private ListView mConversationsListView;
	private ConversationsAdapter adapter = new ConversationsAdapter();

	private int mCurrentTabFlag;

	/**
	 * This tag is used to save current click the location of item.
	 */
	private int currentPosition;

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

	/**
	 * The PopupWindow was showen when onItemlongClick was call..
	 */
	private PopupWindow mPopup;
	private TextView mPouupView = null;

	private Object mLock = new Object();

	private Resources res;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		V2Log.d(TAG, "ConversationsTabFragment onCreate...");
		String tag = this.getArguments().getString("tag");
		if (PublicIntent.TAG_CONF.equals(tag)) {
			mCurrentTabFlag = Conversation.TYPE_CONFERNECE;
			cb = new ConferenceService();
		} else if (PublicIntent.TAG_COV.equals(tag)) {
			mCurrentTabFlag = Conversation.TYPE_CONTACT;
		} else if (PublicIntent.TAG_GROUP.equals(tag)) {
			mCurrentTabFlag = Conversation.TYPE_GROUP;
			chatService = new CrowdGroupService();
		}
		mContext = getActivity();
		service = Executors.newCachedThreadPool();
		initReceiver();
		notificationListener = (NotificationListener) getActivity();
		BitmapManager.getInstance().registerBitmapChangedListener(
				this.bitmapChangedListener);

		Message.obtain(mHandler, FILL_CONFS_LIST).sendToTarget();

		searchList = new ArrayList<ScrollItem>();
		isCreate = true;
		res = getResources();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.i("wzl", "ConversationsTabFragment onCreateView");
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
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mConvList = new ArrayList<Conversation>();
		mUnreadConvList = new HashSet<Conversation>();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getActivity().unregisterReceiver(receiver);
		mItemList = null;
		BitmapManager.getInstance().unRegisterBitmapChangedListener(
				this.bitmapChangedListener);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		((ViewGroup) rootView.getParent()).removeView(rootView);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mCurrentTabFlag == V2GlobalEnum.GROUP_TYPE_USER && !isCallBack) {
			isCallBack = true;
			CommonCallBack.getInstance().executeUpdateConversationState();
		}
	}

	/**
	 * According to mCurrentTabFlag, initialize different intent filter
	 */
	private void initReceiver() {
		if (mCurrentTabFlag == Conversation.TYPE_CONFERNECE) {
			receiver = new ConferenceReceiver();
		} else if (mCurrentTabFlag == Conversation.TYPE_GROUP) {
			receiver = new GroupReceiver();
		} else if (mCurrentTabFlag == Conversation.TYPE_CONTACT) {
			receiver = new ConversationReceiver();
		}
		if (intentFilter == null) {
			intentFilter = new IntentFilter();
			intentFilter.setPriority(IntentFilter.SYSTEM_LOW_PRIORITY);
			intentFilter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
			intentFilter.addCategory(PublicIntent.DEFAULT_CATEGORY);
			intentFilter
					.addAction(ConversationP2PAVActivity.P2P_BROADCAST_MEDIA_UPDATE);
			intentFilter.addAction(JNIService.JNI_BROADCAST_GROUP_NOTIFICATION);
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_USER_UPDATE_NAME_OR_SIGNATURE);
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION);

			if (mCurrentTabFlag == Conversation.TYPE_CONFERNECE) {
				intentFilter
						.addAction(JNIService.JNI_BROADCAST_CONFERENCE_INVATITION);
				intentFilter
						.addAction(JNIService.JNI_BROADCAST_CONFERENCE_REMOVED);
				intentFilter
						.addAction(PublicIntent.BROADCAST_NEW_CONFERENCE_NOTIFICATION);
			} else {
				// intentFilter.addAction(JNIService.JNI_BROADCAST_NEW_MESSAGE);
				intentFilter.addAction(JNIService.JNI_BROADCAST_KICED_CROWD);
				intentFilter
						.addAction(PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION);
				intentFilter.addAction(JNIService.JNI_BROADCAST_GROUP_UPDATED);
			}

			if (mCurrentTabFlag == Conversation.TYPE_CONTACT) {
				intentFilter
						.addAction(JNIService.JNI_BROADCAST_GROUP_USER_ADDED);
				intentFilter.addAction(JNIService.JNI_BROADCAST_NEW_MESSAGE);
				intentFilter
						.addAction(JNIService.JNI_BROADCAST_FRIEND_AUTHENTICATION);
				intentFilter
						.addAction(JNIService.JNI_BROADCAST_NEW_QUALIFICATION_MESSAGE);
				intentFilter
						.addAction(JNIService.BROADCAST_CROWD_NEW_UPLOAD_FILE_NOTIFICATION);
				intentFilter
						.addAction(PublicIntent.REQUEST_UPDATE_CONVERSATION);
				intentFilter
						.addAction(PublicIntent.BROADCAST_USER_COMMENT_NAME_NOTIFICATION);
				intentFilter
						.addAction(JNIService.JNI_BROADCAST_GROUP_USER_REMOVED);
				intentFilter
						.addAction(JNIService.JNI_BROADCAST_GROUP_USER_REMOVED);

			}

			if (mCurrentTabFlag == Conversation.TYPE_GROUP) {
				intentFilter
						.addAction(PublicIntent.BROADCAST_NEW_CROWD_NOTIFICATION);
			}
		}

		getActivity().registerReceiver(receiver, intentFilter);
	}

	@Override
	public void onStart() {
		super.onStart();
		if (mCurrentTabFlag == V2GlobalEnum.GROUP_TYPE_USER && !isCreate) {
			boolean isBreak = false;
			for (Conversation conversation : mConvList) {
				switch (conversation.getType()) {
				case Conversation.TYPE_VERIFICATION_MESSAGE:
					long crowdTime = 0;
					long friendTime = 0;
					User user = GlobalHolder.getInstance().getCurrentUser();
					VMessageQualification nestQualification = MessageLoader
							.getNewestCrowdVerificationMessage(mContext, user);
					AddFriendHistorieNode friendNode = MessageLoader
							.getNewestFriendVerificationMessage(mContext, user);

					if (nestQualification == null && friendNode == null) {
						isBreak = true;
						break;
					}

					if (nestQualification != null)
						crowdTime = nestQualification.getmTimestamp().getTime();

					if (friendNode != null)
						friendTime = friendNode.saveDate * 1000;

					if (crowdTime > friendTime)
						updateCrowdVerificationConversation(nestQualification,
								false);
					else
						updateFriendVerificationConversation(friendNode);
					isBreak = true;
					break;
				// case Conversation.TYPE_CONTACT:
				// updateUserConversation(conversation.getExtId());
				// break;
				// case Conversation.TYPE_GROUP:
				// updateGroupConversation(Conversation.TYPE_GROUP,
				// conversation.getExtId());
				// break;
				default:
					break;
				}

				if (isBreak) {
					break;
				}
			}
			Collections.sort(mConvList);
			Collections.sort(mItemList);
		}
		isCreate = false;
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void afterTextChanged(Editable s) {

		List<ScrollItem> searchTempList = new ArrayList<ScrollItem>();
		searchTempList.addAll(mItemList);
		searchList = SearchUtils.startConversationSearch(searchTempList, s);
		mIsStartedSearch = SearchUtils.mIsStartedSearch;
		adapter.notifyDataSetChanged();

		// String englishChar =
		// V2techSearchContentProvider.queryChineseToEnglish(mContext, "HZ = ?",
		// new String[]{String.valueOf(mChar)});
		// List<Conversation> newItemList = new ArrayList<Conversation>();
		// String searchKey = s == null ? "" : s.toString();
		// for (int i = 0; mCacheItemList != null && i < mCacheItemList.size();
		// i++) {
		// Conversation cov = mCacheItemList.get(i);
		// if (cov.getName() != null
		// && cov.getName().contains(searchKey)) {
		// newItemList.add(cov);
		// } else if (cov.getMsg() != null
		// && cov.getMsg().toString().contains(searchKey)) {
		// newItemList.add(cov);
		// }
		// }
		// mConvList = newItemList;
		// fillAdapter(mConvList , true);
		// adapter.notifyDataSetChanged();
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {

	}

	// @Override
	// public void setUserVisibleHint(boolean isVisibleToUser) {
	// super.setUserVisibleHint(isVisibleToUser);
	// // recover search result
	// if (!isVisibleToUser && mIsStartedSearch) {
	// mConvList = mCacheItemList;
	// adapter.notifyDataSetChanged();
	// mIsStartedSearch = false;
	// }
	// }

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		cb.requestExitConference(currentEntered, null);
		currentEntered = null;
		mContext.stopService(new Intent(mContext,
				ConferencMessageSyncService.class));
	}

	private void populateConversation(List<Group> list) {
		isLoadedCov = true;

		if (list == null || list.size() <= 0) {
			V2Log.w("ConversationsTabFragment populateConversation --> get group list is null");
			return;
		}

		for (int i = list.size() - 1; i >= 0; i--) {
			boolean isReturn = false;
			Group g = list.get(i);
			Conversation cov;
			if (g.getName() == null)
				V2Log.e(TAG,
						"the group name is null , group id is :" + g.getmGId());

			for (Conversation con : mConvList) {
				if (con.getExtId() == g.getmGId()) {
					isReturn = true;
				}
			}

			if (isReturn)
				continue;

			if (g.getGroupType() == GroupType.CONFERENCE) {
				cov = new ConferenceConversation(g);
			} else if (g.getGroupType() == GroupType.CHATING) {
				cov = new CrowdConversation(g);
			} else if (g.getGroupType() == GroupType.ORG) {
				cov = new DepartmentConversation(g);
			} else {
				continue;
			}

			// Update all initial conversation to read
			cov.setReadFlag(Conversation.READ_FLAG_READ);
			long time = GlobalConfig.getGlobalServerTime();
			cov.setDate(DateUtil.getStandardDate(new Date(time)));
			this.mConvList.add(cov);
		}

		fillAdapter(this.mConvList);
	}

	/**
	 * Add conversation to list
	 * 
	 * @param g
	 * @param flag
	 */
	private void addConversation(Group g, boolean flag) {
		Conversation cov;
		if (g.getGroupType() == GroupType.CONFERENCE) {
			cov = new ConferenceConversation(g);
		} else if (g.getGroupType() == GroupType.CHATING) {
			cov = new CrowdConversation(g);
		} else {
			return;
		}

		boolean isAdd = true;
		for (ScrollItem item : mItemList) {
			Conversation conversation = item.cov;
			if (conversation.getExtId() == cov.getExtId()) {
				isAdd = false;
				break;
			}
		}

		if (isAdd) {
			V2Log.d(TAG,
					"addConversation -- Successfully add a new conversation , type is : "
							+ cov.getType() + " and id is : " + cov.getExtId()
							+ " and name is : " + cov.getName());
			this.mConvList.add(0, cov);
			GroupLayout gp = new GroupLayout(this.getActivity(), cov);
			if (mCurrentTabFlag == V2GlobalEnum.GROUP_TYPE_CROWD)
				gp.updateCrowdLayout();
			mItemList.add(0, new ScrollItem(cov, gp));
			gp.updateNotificator(flag);
		}
		this.adapter.notifyDataSetChanged();

		if (flag) {
			cov.setReadFlag(Conversation.READ_FLAG_UNREAD);
		} else {
			cov.setReadFlag(Conversation.READ_FLAG_READ);
		}

		// Update unread conversation list
		if (cov.getReadFlag() == Conversation.READ_FLAG_UNREAD) {
			updateUnreadConversation(cov);
		}
	}

	private List<Conversation> tempList;

	/**
	 * Load local conversation list
	 */
	private void loadUserConversation() {
		if (isLoadedCov) {
			return;
		}

		// 判断只有消息界面，才添加这两个特殊item
		if (mCurrentTabFlag == Conversation.TYPE_CONTACT) {
			// init voice or video item
			VideoBean newestMediaMessage = MessageLoader
					.getNewestMediaMessage(mContext);
			if (newestMediaMessage != null) {
				initVoiceItem();
				updateVoiceSpecificItemState(true, newestMediaMessage);
			}
			// init add friend verification item
			switch (isHaveVerificationMessage()) {
			case VERIFICATION_TYPE_FRIEND:
				initVerificationItem();
				((ConversationFirendAuthenticationData) verificationMessageItemData)
						.setMessageType(ConversationFirendAuthenticationData.VerificationMessageType.CONTACT_TYPE);
				showUnreadFriendAuthentication();
				break;
			case VERIFICATION_TYPE_CROWD:
				initVerificationItem();
				((ConversationFirendAuthenticationData) verificationMessageItemData)
						.setMessageType(ConversationFirendAuthenticationData.VerificationMessageType.CROWD_TYPE);
				VMessageQualification nestQualification = MessageLoader
						.getNewestCrowdVerificationMessage(mContext,
								GlobalHolder.getInstance().getCurrentUser());
				updateCrowdVerificationConversation(nestQualification, false);
				break;
			default:
				break;
			}
		}
		service.execute(new Runnable() {

			@Override
			public void run() {
				synchronized (ConversationsTabFragment.class) {
					tempList = new ArrayList<Conversation>();
					tempList = ConversationProvider.loadUserConversation(
							mContext, tempList, verificationMessageItemData,
							voiceMessageItem);
				}
				Message.obtain(mHandler, UPDATE_CONVERSATION_MESSAGE)
						.sendToTarget();
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

		User user = GlobalHolder.getInstance().getCurrentUser();
		long crowdTime = 0;
		long friendTime = 0;
		VMessageQualification nestQualification = MessageLoader
				.getNewestCrowdVerificationMessage(mContext, user);
		AddFriendHistorieNode friendNode = MessageLoader
				.getNewestFriendVerificationMessage(mContext, user);

		if (nestQualification == null && friendNode == null)
			return result;

		if (nestQualification != null)
			crowdTime = nestQualification.getmTimestamp().getTime();

		if (friendNode != null)
			friendTime = friendNode.saveDate * 1000;

		if (crowdTime > friendTime)
			return VERIFICATION_TYPE_CROWD;
		else
			return VERIFICATION_TYPE_FRIEND;
	}

	private void fillAdapter(List<Conversation> list) {
		mItemList.clear();

		for (int i = 0; i < list.size(); i++) {
			Conversation cov = list.get(i);
			if (cov == null) {
				V2Log.e(TAG,
						"when fillAdapter , get null Conversation , index :"
								+ i);
				continue;
			}

			switch (cov.getType()) {
			case Conversation.TYPE_VERIFICATION_MESSAGE:
				mItemList.add(verificationItem);
				break;
			case Conversation.TYPE_VOICE_MESSAGE:
				mItemList.add(voiceItem);
				break;
			default:
				if (mCurrentTabFlag == V2GlobalEnum.GROUP_TYPE_USER) {

					switch (cov.getType()) {
					case Conversation.TYPE_DEPARTMENT:
						((DepartmentConversation) cov).setShowContact(true);
						break;
					case Conversation.TYPE_GROUP:
						((CrowdConversation) cov).setShowContact(true);
						break;
					default:
					}
				}

				GroupLayout gp = new GroupLayout(mContext, cov);
				// use message layout that show newest chat content and date ,
				// so need hide them
				if (mCurrentTabFlag == V2GlobalEnum.GROUP_TYPE_CROWD)
					gp.updateCrowdLayout();

				if (cov.getReadFlag() == Conversation.READ_FLAG_UNREAD) {
					gp.updateNotificator(true);
					updateUnreadConversation(cov);
				}

				mItemList.add(new ScrollItem(cov, gp));
				break;
			}
		}
		isLoadedCov = true;
		adapter.notifyDataSetChanged();
	}

	/**
	 * 初始化通话消息item对象
	 */
	private void initVoiceItem() {
		voiceMessageItem = new Conversation(Conversation.TYPE_VOICE_MESSAGE, 0);
		voiceMessageItem.setExtId(-1);
		voiceMessageItem.setName("通话消息");
		voiceLayout = new GroupLayout(mContext, voiceMessageItem);
		voiceMessageItem.setReadFlag(Conversation.READ_FLAG_READ);
		voiceItem = new ScrollItem(voiceMessageItem, voiceLayout);
	}

	/**
	 * 初始化验证消息item对象
	 */
	private void initVerificationItem() {
		verificationMessageItemData = new ConversationFirendAuthenticationData(
				Conversation.TYPE_VERIFICATION_MESSAGE, 0);
		verificationMessageItemData.setExtId(-2);
		verificationMessageItemData.setName("验证消息");
		verificationMessageItemLayout = new GroupLayout(mContext,
				verificationMessageItemData);
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
				// if (isHasUnreadMediaMessage())
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
				} else{
					voiceLayout.update(null, startDate, false);
					voiceMessageItem.setReadFlag(Conversation.READ_FLAG_READ);
				}
			}
		}
	}

	/**
	 * 判断通话中是否有未读的
	 * 
	 * @return
	 */
	private boolean isHasUnreadMediaMessage() {

		DataBaseContext context = new DataBaseContext(mContext);
		String selection = ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_READ_STATE
				+ "= ?";
		String[] selectionArgs = new String[] { "0" };
		Cursor mCur = context
				.getContentResolver()
				.query(ContentDescriptor.HistoriesMedia.CONTENT_URI,
						new String[] { ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_READ_STATE },
						selection, selectionArgs, null);

		if (mCur == null || mCur.getCount() == 0)
			return false;
		return true;
	}

	public void updateSearchState() {

		mIsStartedSearch = false;
		searchList.clear();
		adapter.notifyDataSetChanged();
	}

	private boolean crowdNotFresh;

	/**
	 * update group type conversation according groupType and groupID
	 * 
	 * @param groupType
	 * @param groupID
	 */
	private void updateGroupConversation(int groupType, long groupID) {
		Log.d(TAG, "update Group Conversation called , type is : " + groupType
				+ "id is : " + groupID);
		if (!isLoadedCov && mCurrentTabFlag == V2GlobalEnum.GROUP_TYPE_USER) {
			this.loadUserConversation();
		}

		if (groupType == V2GlobalEnum.GROUP_TYPE_DISCUSSION) {
			V2Log.e(TAG, "讨论组暂不处理");
			return;
		}

		VMessage vm = MessageLoader.getNewestGroupMessage(mContext, groupType,
				groupID);
		if (vm == null) {
			V2Log.e(TAG,
					"update group conversation failed.. Didn't find message "
							+ groupID);
			return;
		}

		Conversation existedCov = null;
		GroupLayout viewLayout = null;
		boolean foundFlag = false;
		for (int i = 0; i < mItemList.size(); i++) {
			Conversation cov = mItemList.get(i).cov;
			if (cov.getExtId() == groupID) {
				foundFlag = true;
				existedCov = cov;
				V2Log.d(TAG, "find the given conversation");
				viewLayout = (GroupLayout) this.mItemList.get(i).gp;
				break;
			}
		}

		if (foundFlag) {

			Group group = GlobalHolder.getInstance().getGroupById(groupType,
					groupID);
			switch (mCurrentTabFlag) {
			case V2GlobalEnum.GROUP_TYPE_USER:
				// if (groupType != Conversation.TYPE_DEPARTMENT) {
				if (group != null)
					existedCov.setName(group.getName());
				existedCov.setDate(vm.getDateTimeStr());
				existedCov.setDateLong(String.valueOf(vm.getmDateLong()));
				CharSequence newMessage = MessageUtil
						.getMixedConversationContent(mContext, vm);
				existedCov.setMsg(newMessage);

				Collections.sort(mConvList);
				Collections.sort(mItemList);
				// }
				break;
			// case V2GlobalEnum.GROUP_TYPE_CROWD:
			// if (groupType == Conversation.TYPE_DEPARTMENT) {
			// if (group != null) {
			// crowdNotFresh = false;
			// existedCov.setName(group.getName());
			// existedCov.setReadFlag(Conversation.READ_FLAG_READ);
			// }
			// } else
			// crowdNotFresh = true;
			// break;
			// default:
			// throw new RuntimeException(
			// "updateGroupConversation ---> invalid mCurrentTabFlag : "
			// + mCurrentTabFlag);
			}
		} else {

			switch (mCurrentTabFlag) {
			case V2GlobalEnum.GROUP_TYPE_USER:
				Group group = GlobalHolder.getInstance().getGroupById(
						groupType, groupID);

				switch (groupType) {
				case V2GlobalEnum.GROUP_TYPE_DEPARTMENT:
					if (group == null) {
						V2Log.e(TAG,
								"updateGroupConversation ---> get crowdGroup is null , id is :"
										+ groupID);
						group = new OrgGroup(groupID, null);
					}
					existedCov = new DepartmentConversation(group);
					((DepartmentConversation) existedCov).setShowContact(true);
					break;
				case V2GlobalEnum.GROUP_TYPE_CROWD:
					if (group == null) {
						V2Log.e(TAG,
								"updateGroupConversation ---> get crowdGroup is null , id is :"
										+ groupID);
						group = new CrowdGroup(groupID, null, null);
					}
					existedCov = new CrowdConversation(group);
					((CrowdConversation) existedCov).setShowContact(true);
					break;
				default:
					throw new RuntimeException(
							"updateGroupConversation ---> invalid groupType : "
									+ groupType);
				}
				vm = MessageLoader.getNewestGroupMessage(mContext, groupType,
						groupID);
				if (vm != null) {
					existedCov.setDate(vm.getDateTimeStr());
					existedCov.setDateLong(String.valueOf(vm.getmDateLong()));
					CharSequence newMessage = MessageUtil
							.getMixedConversationContent(mContext, vm);
					existedCov.setMsg(newMessage);
					existedCov.setReadFlag(Conversation.READ_FLAG_UNREAD);
					ConversationProvider.saveConversation(mContext, vm);
				} else
					V2Log.e(TAG,
							"updateGroupConversation ---> get newest VMessage is null , update failed");
				viewLayout = new GroupLayout(mContext, existedCov);

				// 添加到ListView中
				V2Log.d(TAG,
						"updateGroupConversation --> Successfully add a new conversation , type is : "
								+ existedCov.getType() + " and id is : "
								+ existedCov.getExtId() + " and name is : "
								+ existedCov.getName());
				mConvList.add(0, existedCov);
				mItemList.add(0, new ScrollItem(existedCov, viewLayout));
				break;
			// case V2GlobalEnum.GROUP_TYPE_CROWD:
			// if (groupType == Conversation.TYPE_DEPARTMENT) {
			// Group department = GlobalHolder.getInstance()
			// .findGroupById(groupID);
			// if (department == null) {
			// return;
			// }
			// existedCov = new DepartmentConversation(department);
			// ConversationProvider.saveConversation(mContext, vm);
			// viewLayout = new GroupLayout(mContext, existedCov);
			// viewLayout.updateCrowdLayout();
			// }
			// break;
			default:
				throw new RuntimeException(
						"updateGroupConversation ---> invalid mCurrentTabFlag : "
								+ mCurrentTabFlag);
			}
		}

		if (!crowdNotFresh) {
			if (vm.getFromUser() == null) {
				V2Log.e(TAG,
						"updateGroupConversation --> update group conversation state failed..."
								+ "becauser VMessage fromUser is null...please checked , group type is : "
								+ groupType + " groupID is :" + groupID);
				return;
			}

			if (GlobalHolder.getInstance().getCurrentUserId() != vm
					.getFromUser().getmUserId()) {
				// Update status bar
				if (GlobalConfig.isApplicationBackground(mContext))
					updateStatusBar(vm);
				viewLayout.updateNotificator(true);
				existedCov.setReadFlag(Conversation.READ_FLAG_UNREAD);
			} else {
				viewLayout.updateNotificator(false);
				existedCov.setReadFlag(Conversation.READ_FLAG_READ);
			}

			// Update view
			viewLayout.update();
			// Update unread list
			updateUnreadConversation(existedCov);
			adapter.notifyDataSetChanged();
		}
	}

	private void updateUserConversation(long remoteUserID) {

		if (!isLoadedCov) {
			this.loadUserConversation();
		}

		VMessage vm = MessageLoader.getNewestMessage(mContext, GlobalHolder
				.getInstance().getCurrentUserId(), remoteUserID);

		updateUserConversation(remoteUserID, vm);
	}

	private void updateUserConversation(long remoteUserID, long msgId) {

		if (!isLoadedCov) {
			this.loadUserConversation();
		}

		VMessage vm = MessageLoader.loadUserMessageById(mContext, remoteUserID,
				msgId);
		if (vm == null) {
			V2Log.e("Didn't find message " + msgId);
			return;
		}

		updateUserConversation(remoteUserID, vm);
	}

	/**
	 * Update conversation according to message id and remote user id, This
	 * request call only from new message broadcast
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
		Conversation existedCov = null;
		GroupLayout viewLayout = null;
		boolean foundFlag = false;
		for (int i = 0; i < this.mConvList.size(); i++) {
			Conversation cov = this.mConvList.get(i);
			if (cov.getExtId() == extId) {
				foundFlag = true;
				existedCov = cov;
				viewLayout = (GroupLayout) this.mItemList.get(i).gp;
				break;
			}
		}

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
			existedCov.setDate(vm.getFullDateStr());
			existedCov.setDateLong(String.valueOf(vm.getmDateLong()));

			// V2Log.e(TAG, "VMessage :" + vm.getDate().getTime());
			// V2Log.e(TAG, "lastDateTime : " + lastDateTime);
			// if (vm.getMsgCode() != V2GlobalEnum.GROUP_TYPE_CROWDR
			// && vm.getDate().getTime() != lastDateTime) {
			// lastDateTime = vm.getDate().getTime();
			// ScrollItem scrollItem = mItemList.get(currentMoveViewPosition);
			// mItemList.remove(currentMoveViewPosition);
			// mConvList.remove(currentMoveViewPosition);
			// mItemList.add(0, scrollItem);
			// mConvList.add(0, scrollItem.cov);
			// }
			Collections.sort(mConvList);
			Collections.sort(mItemList);
		} else {
			// 展示到界面
			existedCov = new ContactConversation(GlobalHolder.getInstance()
					.getUser(extId));
			existedCov.setMsg(MessageUtil.getMixedConversationContent(mContext,
					vm));
			existedCov.setDateLong(String.valueOf(vm.getmDateLong()));
			ConversationProvider.saveConversation(mContext, vm);
			// 添加到ListView中
			viewLayout = new GroupLayout(mContext, existedCov);
			mConvList.add(0, existedCov);
			mItemList.add(0, new ScrollItem(existedCov, viewLayout));
		}

		if (GlobalHolder.getInstance().getCurrentUserId() != vm.getFromUser()
				.getmUserId()) {
			// Update status bar
			if (GlobalConfig.isApplicationBackground(mContext))
				updateStatusBar(vm);
			viewLayout.updateNotificator(true);
			existedCov.setReadFlag(Conversation.READ_FLAG_UNREAD);
		} else {
			viewLayout.updateNotificator(false);
			existedCov.setReadFlag(Conversation.READ_FLAG_READ);
		}
		// Update view
		viewLayout.update();
		updateUnreadConversation(existedCov);
		adapter.notifyDataSetChanged();
	}

	/**
	 * update verification conversation content
	 * 
	 * @param tempNode
	 */
	private void updateFriendVerificationConversation(
			AddFriendHistorieNode tempNode) {

		if (tempNode == null) {
			V2Log.e(TAG,
					"update Friend verification conversation failed ... given AddFriendHistorieNode is null");
			return;
		}
		// ((ConversationFirendAuthenticationData)
		// verificationMessageItemData).setMessageType(ConversationFirendAuthenticationData.VerificationMessageType.CONTACT_TYPE);
		boolean hasUnread = false;
		if (tempNode.readState == 0)
			hasUnread = true;

		String msg = "";
		String date = "";
		String dateLong = "";
		User user = null;
		user = GlobalHolder.getInstance().getUser(tempNode.remoteUserID);
		String name = user.getName();

		// 别人加我：允许任何人：0已添加您为好友，需要验证：1未处理，2已同意，3已拒绝
		// 我加别人：允许认识人：4你们已成为了好友，需要验证：5等待对方验证，4被同意（你们已成为了好友），6拒绝了你为好友
		if ((tempNode.fromUserID == tempNode.remoteUserID)
				&& (tempNode.ownerAuthType == 0)) {// 别人加我允许任何人
			msg = name + res.getString(R.string.friend_has_added);
		} else if ((tempNode.fromUserID == tempNode.remoteUserID)
				&& (tempNode.ownerAuthType == 1)) {// 别人加我不管我有没有处理
			msg = name + res.getString(R.string.friend_apply_add_you_friend);
		} else if ((tempNode.fromUserID == tempNode.ownerUserID)
				&& (tempNode.addState == 0)) {// 我加别人等待验证
			msg = res.getString(R.string.friend_apply_add) + name
					+ res.getString(R.string.friend_add_you_waiting_verify);
		} else if ((tempNode.fromUserID == tempNode.ownerUserID)
				&& (tempNode.addState == 1)) {// 我加别人已被同意或我加别人不需验证
			msg = res.getString(R.string.friend_relation) + name
					+ res.getString(R.string.friend_becomed);
		} else if ((tempNode.fromUserID == tempNode.ownerUserID)
				&& (tempNode.addState == 2)) {// 我加别人已被拒绝
			msg = name + res.getString(R.string.friend_was_reject_apply);
		}
		date = DateUtil.getStringDate(tempNode.saveDate * 1000);
		dateLong = String.valueOf(tempNode.saveDate * 1000);

		verificationMessageItemLayout.updateNotificator(hasUnread);
		if (verificationMessageItemData != null) {
			verificationMessageItemData.setMsg(msg);
			verificationMessageItemData.setDate(date);
			verificationMessageItemData.setDateLong(dateLong);
			ConversationFirendAuthenticationData friend = (ConversationFirendAuthenticationData) verificationMessageItemData;
			friend.setUser(user);
			friend.setFriendNode(tempNode);
			friend.setMessageType(ConversationFirendAuthenticationData.VerificationMessageType.CONTACT_TYPE);
			verificationMessageItemLayout.update();
		}

		if (hasUnread)
			verificationMessageItemData
					.setReadFlag(Conversation.READ_FLAG_UNREAD);
		else
			verificationMessageItemData
					.setReadFlag(Conversation.READ_FLAG_READ);
		updateUnreadConversation(verificationMessageItemData);
		updateVerificationMessage(msg);
	}

	private void updateCrowdVerificationConversation(VMessageQualification msg) {
		updateCrowdVerificationConversation(msg, true);
	}

	/**
	 * update verification conversation content
	 * 
	 * @param msg
	 * @param isFresh
	 *            load from database , no fresh
	 */
	private void updateCrowdVerificationConversation(VMessageQualification msg,
			boolean isFresh) {

		if (msg == null) {
			V2Log.e(TAG,
					"update Friend verification conversation failed ... given VMessageQualification is null");
			return;
		}

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
				if (TextUtils.isEmpty(invitation.getInvitationUser().getName())) {
					User user = GlobalHolder.getInstance().getUser(
							invitation.getInvitationUser().getmUserId());
					if (user != null)
						invitationName = user.getName();
				} else
					invitationName = invitation.getInvitationUser().getName();

				if (invitation.getQualState() == QualificationState.BE_ACCEPTED) {
					content = crowdGroup.getName() + "同意了你的申请";
				} else if ((invitation.getQualState() == QualificationState.BE_REJECT)
						|| (invitation.getQualState() == QualificationState.WAITING_FOR_APPLY)) {
					content = crowdGroup.getName() + "拒绝了你的申请";
				} else {
					content = invitationName + "邀请你加入" + crowdGroup.getName()
							+ "群";
				}
			}
			verification.setUser(invitation.getInvitationUser());
			verification.setGroup(invitation.getCrowdGroup());
			verification.setQualification(msg);
			break;
		case CROWD_APPLICATION:
			VMessageQualificationApplicationCrowd apply = (VMessageQualificationApplicationCrowd) msg;
			String applyName = null;
			User applyUser = apply.getApplicant();
			CrowdGroup applyGroup = apply.getCrowdGroup();
			if (applyUser == null || applyGroup == null)
				content = null;
			else {
				if (TextUtils.isEmpty(applyUser.getName())) {
					User user = GlobalHolder.getInstance().getUser(
							apply.getApplicant().getmUserId());
					if (user != null)
						applyName = user.getName();
				} else
					applyName = apply.getApplicant().getName();

				if (apply.getQualState() == QualificationState.BE_REJECT)
					content = applyName + "拒绝加入" + applyGroup.getName() + "群";
				else if (apply.getQualState() == QualificationState.BE_ACCEPTED)
					content = applyName + "同意加入" + applyGroup.getName() + "群";
				else
					content = applyName + "申请加入" + applyGroup.getName() + "群";
			}
			verification.setUser(apply.getApplicant());
			verification.setGroup(apply.getCrowdGroup());
			verification.setQualification(msg);
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

		updateVerificationMessage(content);

		if (isFresh) {
			if (msg.getReadState() == VMessageQualification.ReadState.UNREAD) {
				verificationMessageItemData
						.setReadFlag(Conversation.READ_FLAG_UNREAD);
				verificationMessageItemLayout.updateNotificator(true);
				showAuthenticationNotification = true;
			} else {
				verificationMessageItemData
						.setReadFlag(Conversation.READ_FLAG_READ);
				verificationMessageItemLayout.updateNotificator(false);
			}
		}
		// else {
		// notificationListener.updateNotificator(Conversation.TYPE_CONTACT,
		// false);
		// verificationMessageItemLayout.updateNotificator(false);
		// }
		updateUnreadConversation(verificationMessageItemData);
		adapter.notifyDataSetChanged();
		Collections.sort(mConvList);
		Collections.sort(mItemList);
	}

	/**
	 * Update main activity to show or hide notificator , and update
	 * conversation read state in database
	 * 
	 * @param cov
	 */
	private void updateUnreadConversation(Conversation cov) {
		int ret;
		if (cov.getReadFlag() == Conversation.READ_FLAG_READ) {
			mUnreadConvList.remove(cov);
			ret = Conversation.READ_FLAG_READ;
		} else {
			mUnreadConvList.add(cov);
			ret = Conversation.READ_FLAG_UNREAD;
		}

		boolean isUnread = false;
		for (ScrollItem item : mItemList) {
			if(item.cov.getReadFlag() == Conversation.READ_FLAG_UNREAD){
				isUnread = true;
				break;
			}
		}
		
		if (mUnreadConvList.size() > 0 && isUnread) {
			notificationListener.updateNotificator(mCurrentTabFlag, true);
		} else {
			notificationListener.updateNotificator(mCurrentTabFlag, false);
		}
		// update conversation date and flag to database
		ConversationProvider.updateConversationToDatabase(mContext, cov, ret);
	}

	/**
	 * update crowd group name in Message Interface
	 */
	private void updateMessageGroupName() {
		service.execute(new Runnable() {

			@Override
			public void run() {
				while (!isLoadedCov) {
					SystemClock.sleep(1000);
					V2Log.e(TAG, "waiting for crowd fill adapter ......");
				}

				for (ScrollItem item : mItemList) {
					if (mCurrentTabFlag == V2GlobalEnum.GROUP_TYPE_CROWD) {
						final Group g = ((CrowdConversation) item.cov).getGroup();
						if (g == null || g.getOwnerUser() == null)
							continue;

						final GroupLayout currentGroupLayout = (GroupLayout) item.gp;

						final User u = GlobalHolder.getInstance().getUser(
								g.getOwnerUser().getmUserId());
						if (u == null) {
							continue;
						}

						getActivity().runOnUiThread(new Runnable() {

							@Override
							public void run() {
								currentGroupLayout.updateContent(u.getName());
								currentGroupLayout.updateGroupOwner(g.getName());
								g.setOwnerUser(u);
							}
						});
					} else {
						V2Log.e(TAG,
								"current item type is : " + item.cov.getType());
						if (item.cov.getType() != Conversation.TYPE_GROUP)
							continue;

						final GroupLayout currentGroupLayout = ((GroupLayout) item.gp);
						final CrowdConversation crowd = (CrowdConversation) item.cov;

						Group newGroup = GlobalHolder.getInstance()
								.getGroupById(V2GlobalEnum.GROUP_TYPE_CROWD,
										crowd.getExtId());
						if (newGroup != null) {
							crowd.setG(newGroup);
							crowd.setReadFlag(Conversation.READ_FLAG_READ);
							final VMessage vm = MessageLoader
									.getNewestGroupMessage(mContext,
											V2GlobalEnum.GROUP_TYPE_CROWD,
											crowd.getExtId());
							crowd.setName(newGroup.getName());
							getActivity().runOnUiThread(new Runnable() {

								@Override
								public void run() {
									currentGroupLayout.updateGroupOwner(crowd
											.getName());
									if (vm != null) {
										crowd.setDate(vm.getDateTimeStr());
										crowd.setDateLong(String.valueOf(vm
												.getmDateLong()));
										CharSequence newMessage = MessageUtil
												.getMixedConversationContent(
														mContext, vm);
										crowd.setMsg(newMessage);
										// currentGroupLayout.update(newMessage,
										// DateUtil.getStringDate(vm.getDate().getTime()),
										// crowd.getReadFlag() ==
										// Conversation.READ_FLAG_READ ? false :
										// true);
										currentGroupLayout.update();
									}
									adapter.notifyDataSetChanged();
									V2Log.e(TAG,
											"Successfully updated the CROWD_GROUP infos , "
													+ "group name is :"
													+ crowd.getName());
								}
							});
						}
					}
				}
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

				for (ScrollItem item : mItemList) {
					if (item.cov.getType() != Conversation.TYPE_DEPARTMENT)
						continue;

					final GroupLayout currentGroupLayout = ((GroupLayout) item.gp);
					final DepartmentConversation department = (DepartmentConversation) item.cov;

					Group newGroup = GlobalHolder.getInstance().getGroupById(
							V2GlobalEnum.GROUP_TYPE_DEPARTMENT,
							department.getExtId());
					if (newGroup != null) {
						department.setDepartmentGroup(newGroup);
						department.setReadFlag(Conversation.READ_FLAG_READ);
						final VMessage vm = MessageLoader
								.getNewestGroupMessage(mContext,
										V2GlobalEnum.GROUP_TYPE_DEPARTMENT,
										department.getExtId());
						department.setName(newGroup.getName());
						getActivity().runOnUiThread(new Runnable() {

							@Override
							public void run() {
								currentGroupLayout.updateGroupOwner(department
										.getName());
								if (vm != null) {
									department.setName(department.getName());
									department.setDate(vm.getDateTimeStr());
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
	 * @param id
	 */
	private void removeConversation(long id) {

		Conversation cache = null;
		for (int i = 0; i < mConvList.size(); i++) {
			if (mConvList.get(i).getExtId() == id) {
				mItemList.remove(i);
				Conversation conversation = mConvList.get(i);
				if (mCurrentTabFlag == V2GlobalEnum.GROUP_TYPE_USER) {
					if (conversation.getType() != Conversation.TYPE_VOICE_MESSAGE
							&& conversation.getType() != Conversation.TYPE_VERIFICATION_MESSAGE) {
						ConversationProvider.deleteConversation(mContext,
								conversation);
						V2Log.d(TAG,
								" Successfully remove contact conversation , id is : "
										+ id);
					}
				}
				// Now , Department conversation is not show in crowd interface.
				// else if (mCurrentTabFlag == V2GlobalEnum.GROUP_TYPE_CROWD) {
				// For delete Department Conversation in Crowd Interface
				// ConversationProvider.deleteConversation(mContext,
				// conversation);
				// V2Log.d(TAG,
				// " Successfully remove crowd conversation , id is : " + id);
				// }
				cache = mConvList.remove(i);
				break;
			}
		}
		if (cache != null) {
			adapter.notifyDataSetChanged();
			// Set removed conversation state to readed
			cache.setReadFlag(Conversation.READ_FLAG_READ);
			Notificator.cancelSystemNotification(getActivity(),
					PublicIntent.MESSAGE_NOTIFICATION_ID);
			updateUnreadConversation(cache);
		}

	}

	private void updateStatusBar(VMessage vm) {
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
			content = vm.getAllTextContent();
		}
		Intent resultIntent = new Intent(
				PublicIntent.START_CONVERSACTION_ACTIVITY);

		resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		switch (vm.getMsgCode()) {
		case V2GlobalEnum.GROUP_TYPE_USER:
			resultIntent.putExtra("obj", new ConversationNotificationObject(
					Conversation.TYPE_CONTACT, vm.getFromUser().getmUserId()));
			break;
		case V2GlobalEnum.GROUP_TYPE_CROWD:
			resultIntent.putExtra("obj", new ConversationNotificationObject(
					Conversation.TYPE_GROUP, vm.getGroupId()));
			break;
		case V2GlobalEnum.GROUP_TYPE_DEPARTMENT:
			resultIntent.putExtra("obj", new ConversationNotificationObject(
					V2GlobalEnum.GROUP_TYPE_DEPARTMENT, vm.getGroupId()));
			break;
		default:
			break;
		}
		resultIntent.addCategory(PublicIntent.DEFAULT_CATEGORY);

		Notificator.updateSystemNotification(mContext, vm.getFromUser()
				.getName(), content, 1, resultIntent,
				PublicIntent.MESSAGE_NOTIFICATION_ID);

	}

	private void initPopupWindow() {
		LayoutInflater inflater = (LayoutInflater) this.getActivity()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		RelativeLayout popWindow = (RelativeLayout) inflater.inflate(
				R.layout.pop_up_window_group_list_view, null);
		mPopup = new PopupWindow(popWindow, LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT, true);
		mPopup.setFocusable(true);
		mPopup.setTouchable(true);
		mPopup.setOutsideTouchable(true);

		TextView tvItem = (TextView) popWindow
				.findViewById(R.id.pop_up_window_item);
		tvItem.setVisibility(View.GONE);

		mPouupView = (TextView) popWindow
				.findViewById(R.id.pop_up_window_quit_crowd_item);

		if (!mPouupView.isShown())
			mPouupView.setVisibility(View.VISIBLE);
		mPouupView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Conversation cov = mConvList.get(currentPosition);
				CrowdGroup crowd = (CrowdGroup) GlobalHolder.getInstance()
						.getGroupById(GroupType.CHATING.intValue(),
								cov.getExtId());
				// If group is null, means we have
				// removed
				// this conversaion
				if (crowd != null) {
					chatService.quitCrowd(crowd, null);
					Intent kick = new Intent();
					kick.setAction(JNIService.JNI_BROADCAST_KICED_CROWD);
					kick.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
					kick.putExtra("crowd", cov.getExtId());
					mContext.sendBroadcast(kick);
				} else
					V2Log.e(TAG,
							"quit crowd group failed .. id is :"
									+ cov.getExtId());
				mPopup.dismiss();
			}

		});

	}

	private void showPopupWindow(View anchor) {

		if (!anchor.isShown()) {
			return;
		}

		if (this.mPopup == null) {
			initPopupWindow();
		}

		Conversation cov = mConvList.get(currentPosition);
		CrowdGroup cg = (CrowdGroup) GlobalHolder.getInstance().getGroupById(
				GroupType.CHATING.intValue(), cov.getExtId());
		if (cg.getOwnerUser().getmUserId() == GlobalHolder.getInstance()
				.getCurrentUserId())
			mPouupView
					.setText(R.string.crowd_detail_qulification_dismiss_button);
		else
			mPouupView.setText(R.string.crowd_detail_qulification_quit_button);

		if (mPopup.getContentView().getWidth() <= 0
				&& mPopup.getContentView() != null) {
			mPopup.getContentView().measure(View.MeasureSpec.EXACTLY,
					View.MeasureSpec.EXACTLY);
		}
		int popupWindowWidth = mPopup.getContentView().getMeasuredWidth();
		int popupWindowHeight = mPopup.getContentView().getMeasuredHeight();

		mPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

		int viewWidth = anchor.getMeasuredWidth();
		int viewHeight = anchor.getMeasuredHeight();
		int offsetX = (viewWidth - popupWindowWidth) / 2;
		int offsetY = (viewHeight + popupWindowHeight);

		int[] location = new int[2];
		anchor.getLocationInWindow(location);
		// if (location[1] <= 0) {
		Rect r = new Rect();
		anchor.getDrawingRect(r);
		Rect r1 = new Rect();
		anchor.getGlobalVisibleRect(r1);
		int offsetXLocation = r1.left + offsetX;
		int offsetYLocation = r1.top - (offsetY / 2);
		mPopup.showAtLocation((View) anchor.getParent(), Gravity.NO_GRAVITY,
				offsetXLocation, offsetYLocation);
	}

	private ProgressDialog mWaitingDialog;

	private void requestJoinConf(long gid) {
		if (currentEntered != null) {
			V2Log.e("Already in meeting " + currentEntered.getId());
			return;
		}
		synchronized (mLock) {
			// When request join conference response, mWaitingDialog will be
			// null
			if (mWaitingDialog != null) {
				return;
			}

			mWaitingDialog = ProgressDialog.show(
					mContext,
					"",
					mContext.getResources().getString(
							R.string.requesting_enter_conference), true);
			Message.obtain(this.mHandler, REQUEST_ENTER_CONF, gid)
					.sendToTarget();
		}

	}

	private Conference currentEntered;

	private void startConferenceActivity(Conference conf) {
		// Set current state to in meeting state
		GlobalHolder.getInstance().setMeetingState(true, conf.getId());
		Intent enterConference = new Intent(mContext, VideoActivityV2.class);
		enterConference.putExtra("conf", conf);
		enterConference.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
		currentEntered = conf;
		this.startActivityForResult(enterConference, CONFERENCE_ENTER_CODE);
	}

	/*
	 * This request from main activity
	 * 
	 * @see
	 * com.v2tech.view.ConferenceListener#requestJoinConf(com.v2tech.vo.Conference
	 * )
	 */
	@Override
	public boolean requestJoinConf(Conference conf) {
		if (conf == null) {
			return false;
		}

		requestJoinConf(conf.getId());

		// This request from main activity
		// We need to update notificator for conversation
		for (int i = 0; i < mItemList.size(); i++) {
			// hiden notificator
			((GroupLayout) mItemList.get(i).gp).updateNotificator(false);
			Conversation cov = mConvList.get(i);
			// update main activity notificator
			cov.setReadFlag(Conversation.READ_FLAG_READ);
			updateUnreadConversation(cov);
		}

		return true;
	}

	private void updateConferenceNotification(Conference conf) {
		Intent enterConference = new Intent(mContext, MainActivity.class);

		User creator = GlobalHolder.getInstance().getUser(conf.getCreator());
		enterConference.putExtra("conf", conf);
		enterConference.putExtra("initFragment", 3);
		Notificator.updateSystemNotification(mContext, conf.getName()
				+ " 会议邀请:", creator == null ? "" : creator.getName(), 1,
				enterConference, PublicIntent.VIDEO_NOTIFICATION_ID);
	}

	public class ScrollItem implements Comparable<ScrollItem> {
		public Conversation cov;
		public View gp;

		public ScrollItem(Conversation g, View gp) {
			super();
			this.cov = g;
			this.gp = gp;
			this.gp.setTag(cov);
		}

		@Override
		public int compareTo(ScrollItem item) {
			if (item == null)
				return 1;
			String localTime = cov.getDateLong();
			String remoteTime = item.cov.getDateLong();
			if (TextUtils.isEmpty(localTime) || TextUtils.isEmpty(remoteTime))
				return -1;

			if (Long.valueOf(localTime) < Long.valueOf(remoteTime))
				return 1;
			else
				return -1;
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
			currentPosition = pos;
			// hide notificator
			((GroupLayout) mItemList.get(pos).gp).updateNotificator(false);
			Conversation cov = mConvList.get(pos);
			if (mCurrentTabFlag == Conversation.TYPE_CONFERNECE)
				requestJoinConf(cov.getExtId());
			else if (mCurrentTabFlag == Conversation.TYPE_GROUP)
				startConversationView(cov);
			else if (mCurrentTabFlag == Conversation.TYPE_CONTACT) {

				if (cov.getType() == Conversation.TYPE_VOICE_MESSAGE) {
					Intent intent = new Intent(mContext,
							VoiceMessageActivity.class);
					startActivity(intent);
				} else if (cov.getType() == Conversation.TYPE_VERIFICATION_MESSAGE) {

					Intent intent = new Intent(mContext,
							MessageAuthenticationActivity.class);

					VerificationMessageType messageType = ((ConversationFirendAuthenticationData) verificationMessageItemData)
							.getMessageType();
					if (messageType == VerificationMessageType.CONTACT_TYPE)
						intent.putExtra("isFriendActivity", true);
					else
						intent.putExtra("isFriendActivity", false);

					// 查出未读的第一条按时间顺序
					String order = ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_SAVEDATE
							+ " desc limit 1";
					Cursor cursor = mContext.getContentResolver().query(
							ContentDescriptor.HistoriesAddFriends.CONTENT_URI,
							null, "ReadState = ?",
							new String[] { String.valueOf(0) }, order);
					if ((cursor != null) && (cursor.getCount() == 0))
						intent.putExtra("isFriendShowNotification", false);
					else
						intent.putExtra("isFriendShowNotification", true);

					if (cursor != null)
						cursor.close();

					intent.putExtra("isCrowdShowNotification",
							showAuthenticationNotification);
					startActivity(intent);
					showAuthenticationNotification = false;
				} else {
					startConversationView(cov);
				}
			}

			// update main activity notificator
			cov.setReadFlag(Conversation.READ_FLAG_READ);
			updateUnreadConversation(cov);

			// update voice phone state
			notificationListener.updateNotificator(mCurrentTabFlag, false);
		}
	};

	private OnItemLongClickListener mItemLongClickListener = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> adapter, View v, int pos,
				long id) {
			final Conversation cov = mConvList.get(pos);
			String[] item;
			currentPosition = pos;
			if (mCurrentTabFlag == Conversation.TYPE_GROUP) {
				showPopupWindow(v);
			} else {
				if (mCurrentTabFlag == Conversation.TYPE_CONFERNECE)
					item = new String[] { mContext.getResources().getString(
							R.string.conversations_delete_conf) };
				else
					// Conversation.TYPE_CONTACT
					item = new String[] { mContext.getResources().getString(
							R.string.conversations_delete_conversaion) };

				AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
				builder.setTitle(cov.getName()).setItems(item,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								if (which == 0) {
									switch (mCurrentTabFlag) {
									case Conversation.TYPE_CONFERNECE:
										Group g = GlobalHolder.getInstance()
												.getGroupById(
														GroupType.CONFERENCE
																.intValue(),
														cov.getExtId());
										// If group is null, means we have
										// removed
										// this conversaion
										if (g != null) {
											cb.quitConference(new Conference(
													cov.getExtId(), g
															.getOwnerUser()
															.getmUserId()),
													null);
										}
									case Conversation.TYPE_CONTACT:
										removeConversation(cov.getExtId());
										break;
									}
								}
								dialog.dismiss();
							}
						});
				AlertDialog ad = builder.create();
				ad.show();
			}
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
			if (mIsStartedSearch) {
				return searchList == null ? 0 : searchList.size();
			} else {
				if (mConvList == null) {
					mConvList = new ArrayList<Conversation>();
					for (int i = 0; i < mItemList.size(); i++) {
						mConvList.add(mItemList.get(i).cov);
					}
				} else {
					if (mConvList.size() != mItemList.size()) {
						fillAdapter(mConvList);
					}
				}
				return mConvList.size();
			}
		}

		@Override
		public Object getItem(int position) {
			if (mIsStartedSearch)
				return searchList.get(position);
			else
				return mConvList.get(position);
		}

		@Override
		public long getItemId(int position) {
			if (mIsStartedSearch)
				return searchList.get(position).cov.getExtId();
			else
				return mConvList.get(position).getExtId();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (mIsStartedSearch) {
				return searchList.get(position).gp;
			} else {
				if (currentPosition < mItemList.size()) {

					return mItemList.get(position).gp;
				} else {
					return convertView;
				}
			}
		}

	}

	class CommonReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (JNIService.JNI_BROADCAST_GROUP_NOTIFICATION.equals(intent
					.getAction())) {
				int type = intent.getExtras().getInt("gtype");
				if ((type == GroupType.CONFERENCE.intValue() && mCurrentTabFlag == Conversation.TYPE_CONFERNECE)
						|| (type == GroupType.CHATING.intValue() && mCurrentTabFlag == Conversation.TYPE_GROUP)) {
					Message.obtain(mHandler, FILL_CONFS_LIST).sendToTarget();
				}

				V2Log.e(TAG, "update Conversaton type is : " + type);
				if (type == GroupType.CHATING.intValue()
						&& mCurrentTabFlag == V2GlobalEnum.GROUP_TYPE_USER
						&& !isUpdateGroup) {
					isUpdateGroup = true;
					updateMessageGroupName();
				} else if (type == GroupType.ORG.intValue()
						&& mCurrentTabFlag == V2GlobalEnum.GROUP_TYPE_USER
						&& !isUpdateDeparment) {
					isUpdateDeparment = true;
					updateDepartmentGroupName();
				}

				if (type == GroupType.CHATING.intValue()
						&& mCurrentTabFlag == V2GlobalEnum.GROUP_TYPE_CROWD
						&& !isUpdateGroup) {
					isUpdateGroup = true;
					updateMessageGroupName();
				}
				// From this broadcast, user has already read conversation
			} else if (PublicIntent.REQUEST_UPDATE_CONVERSATION.equals(intent
					.getAction())) {
				ConversationNotificationObject uao = (ConversationNotificationObject) intent
						.getExtras().get("obj");
				boolean isFresh = intent.getBooleanExtra("isFresh", true);
				boolean isNeedDelete = intent
						.getBooleanExtra("isDelete", false);
				// delete Empty message conversation
				if (isNeedDelete) {
					removeConversation(uao.getExtId());
					return;
				}

				if (!isFresh) {
					for (ScrollItem scroll : mItemList) {
						if (scroll.cov.getExtId() == uao.getExtId()) {
							GroupLayout layout = (GroupLayout) scroll.gp;
							layout.updateNotificator(false);
							scroll.cov.setReadFlag(Conversation.READ_FLAG_READ);
							updateUnreadConversation(scroll.cov);
						}
					}
					return;
				}
				Message.obtain(mHandler, UPDATE_CONVERSATION, uao)
						.sendToTarget();
				// Update name of creator of conversation
			} else if (JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION
					.equals(intent.getAction())) {

				for (ScrollItem item : mItemList) {
					GroupLayout currentGroupLayout = ((GroupLayout) item.gp);
					Conversation currentConversation = item.cov;
					Group g = null;
					String groupType = null;
					switch (item.cov.getType()) {
					case Conversation.TYPE_VERIFICATION_MESSAGE:
						ConversationFirendAuthenticationData verification = ((ConversationFirendAuthenticationData) item.cov);
						if (verification.getQualification() == null
								|| verification.getUser() == null) {
							V2Log.e(TAG,
									"update crowd verification message failed .. user or group is null");
							return;
						}

						User verificationUser = GlobalHolder.getInstance()
								.getUser(verification.getUser().getmUserId());
						if (verificationUser == null)
							return;
						else
							verification.setUser(verificationUser);

						if (verification.getMessageType() == VerificationMessageType.CROWD_TYPE) {
							if (verification.getGroup() == null) {
								V2Log.e(TAG,
										"update crowd verification message failed .. user or group is null");
								return;
							}

							Group group = GlobalHolder.getInstance()
									.getGroupById(
											V2GlobalEnum.GROUP_TYPE_CROWD,
											verification.getGroup().getmGId());
							if (group == null)
								return;
							else
								verification.setGroup(group);

							Message msg = Message.obtain(mHandler,
									UPDATE_VERIFICATION_MESSAGE,
									verification.getQualification());
							msg.arg1 = verification.getMessageType().intValue();
							msg.sendToTarget();
						} else {

							if (verification.getFriendNode() == null) {
								V2Log.e(TAG,
										"update friend verification message failed .. user or group is null");
								return;
							}

							Message msg = Message.obtain(mHandler,
									UPDATE_VERIFICATION_MESSAGE,
									verification.getFriendNode());
							msg.arg1 = verification.getMessageType().intValue();
							msg.sendToTarget();
						}
						break;
					case Conversation.TYPE_CONTACT:
						if (mCurrentTabFlag == Conversation.TYPE_CONTACT) {
							ContactConversation contact = ((ContactConversation) item.cov);
							String oldName = contact.getName();
							User user = GlobalHolder.getInstance().getUser(
									contact.getExtId());
							if (user != null) {
								contact.updateUser(user);
								currentGroupLayout.update();
								adapter.notifyDataSetChanged();
								groupType = "CONTACT";
							}

							if (TextUtils.isEmpty(oldName) && user != null) {
								V2Log.d(TAG,
										"Successfully updated the user infos , user name is :"
												+ user.getName());
							}
						}
						break;
					case Conversation.TYPE_CONFERNECE:
						g = ((ConferenceConversation) currentConversation)
								.getGroup();
						groupType = "CONFERENCE";
						break;
					case Conversation.TYPE_GROUP:
						if (mCurrentTabFlag != Conversation.TYPE_CONTACT) {
							g = ((CrowdConversation) currentConversation)
									.getGroup();
							groupType = "CROWD";
						}
						break;
					case V2GlobalEnum.GROUP_TYPE_DEPARTMENT:
						if (mCurrentTabFlag != Conversation.TYPE_CONTACT) {
							g = ((DepartmentConversation) currentConversation)
									.getDepartmentGroup();
							groupType = "DEPARTMENT";
						}
						break;
					}

					if (currentConversation.getType() != Conversation.TYPE_CONTACT) {
						if (g == null || g.getOwnerUser() == null)
							continue;

						User u = GlobalHolder.getInstance().getUser(
								g.getOwnerUser().getmUserId());
						if (u == null) {
							continue;
						}
						V2Log.d(TAG,
								"Group User Update Notify : groupType --> "
										+ groupType + " | groupID --> "
										+ g.getmGId() + " | user name is --> "
										+ u.getName());
						currentGroupLayout.updateContent(u.getName());
						currentGroupLayout.updateGroupOwner(g.getName());
						g.setOwnerUser(u);
					}
				}
			} else if (JNIService.JNI_BROADCAST_USER_UPDATE_NAME_OR_SIGNATURE
					.equals(intent.getAction())) {
				if (mItemList != null && mItemList.size() > 0) {
					Message.obtain(mHandler, UPDATE_USER_SIGN,
							intent.getExtras().get("uid")).sendToTarget();
				}
			} else if (ConversationP2PAVActivity.P2P_BROADCAST_MEDIA_UPDATE
					.equals(intent.getAction())) {
				if (Conversation.TYPE_CONTACT == mCurrentTabFlag) {
					long remoteID = intent.getLongExtra("remoteID", -1l);
					if (remoteID == -1l) {
						Log.e(TAG, "get remoteID is -1 ... update failed!!");
						return;
					}

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

					if (voiceLayout == null || voiceMessageItem == null)
						initVoiceItem();

					updateVoiceSpecificItemState(false, newestMediaMessage);
					updateUnreadConversation(voiceMessageItem);
					mItemList.remove(voiceItem);
					mConvList.remove(voiceMessageItem);
					mItemList.add(0, voiceItem);
					mConvList.add(0, voiceItem.cov);
					adapter.notifyDataSetChanged();
				}
			}

		}

	}

	class GroupReceiver extends CommonReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			super.onReceive(context, intent);
			if (PublicIntent.BROADCAST_NEW_CROWD_NOTIFICATION.equals(intent
					.getAction())) {
				long gid = intent.getLongExtra("crowd", 0);
				Group g = GlobalHolder.getInstance().getGroupById(
						GroupType.CHATING.intValue(), gid);
				if (g != null)
					addConversation(g, false);
				else
					V2Log.e("Can not get crowd :" + gid);

			} else if (PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION
					.equals(intent.getAction())
					|| intent.getAction().equals(
							JNIService.JNI_BROADCAST_KICED_CROWD)) {
				long cid = intent.getLongExtra("crowd", -1l);
				if (cid == -1l) {
					V2Log.e(TAG,
							"Received the broadcast to quit the crowd group , but crowd id is wroing... ");
					return;
				}
				removeConversation(cid);
				GlobalHolder.getInstance().removeGroup(GroupType.CHATING, cid);
				// clear the crowd group all chat database messges
				MessageLoader.deleteMessageByID(context, mCurrentTabFlag, cid,
						-1);
				// clear the crowd group all verification database messges
				MessageLoader.deleteCrowdVerificationMessage(context, cid);
			} else if (JNIService.JNI_BROADCAST_GROUP_UPDATED.equals(intent
					.getAction())) {
				long gid = intent.getLongExtra("gid", 0);
				Group g = GlobalHolder.getInstance().getGroupById(gid);
				if(g == null)
					return ;
				
				if (g.getGroupType() == GroupType.CHATING) {
					for (int i = 0; i < mItemList.size(); i++) {
						ScrollItem item = mItemList.get(i);
						if (item.cov.getExtId() == g.getmGId()) {
							((GroupLayout) item.gp).update();
						}
					}
				}
			}
			// else if (JNIService.JNI_BROADCAST_NEW_MESSAGE.equals(intent
			// .getAction())) {
			// boolean groupMessage = intent.getBooleanExtra("gm", false);
			// if (groupMessage) {
			// V2Log.d(TAG,
			// "JNI_BROADCAST_NEW_MESSAGE group message update..");
			// updateGroupConversation(
			// intent.getIntExtra("groupType", -1),
			// intent.getLongExtra("groupID", -1));
			// }
			// }
		}
	}

	class ConferenceReceiver extends CommonReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			super.onReceive(context, intent);
			if (JNIService.JNI_BROADCAST_CONFERENCE_INVATITION.equals(intent
					.getAction())) {
				long gid = intent.getLongExtra("gid", 0);
				Group g = GlobalHolder.getInstance().getGroupById(
						GroupType.CONFERENCE.intValue(), gid);
				if (g != null) {
					addConversation(g, true);

					Conference c = new Conference((ConferenceGroup) g);

					// Notify status bar
					updateConferenceNotification(c);
				} else {
					V2Log.e("Can not get group information of invatition :"
							+ gid);
				}
			} else if (JNIService.JNI_BROADCAST_CONFERENCE_REMOVED
					.equals(intent.getAction())) {
				long confId = intent.getLongExtra("gid", 0);
				// Remove conference conversation from list
				removeConversation(confId);

				// This broadcast is sent after create conference successfully
			} else if (PublicIntent.BROADCAST_NEW_CONFERENCE_NOTIFICATION
					.equals(intent.getAction())) {
				Group conf = GlobalHolder.getInstance().getGroupById(
						GroupType.CONFERENCE.intValue(),
						intent.getLongExtra("newGid", 0));
				// Add conference to conversation list
				addConversation(conf, false);
				Conference c = new Conference((ConferenceGroup) conf);
				startConferenceActivity(c);
			}
		}
	}

	class ConversationReceiver extends CommonReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			super.onReceive(context, intent);
			String action = intent.getAction();
			if (action.equals(JNIService.JNI_BROADCAST_NEW_MESSAGE)) {
				boolean groupMessage = intent.getBooleanExtra("gm", false);
				if (isLoadedCov) {
					if (groupMessage)
						updateGroupConversation(
								intent.getIntExtra("groupType", -1),
								intent.getLongExtra("groupID", -1));
					else
						updateUserConversation(
								intent.getExtras().getLong("remoteUserID"),
								intent.getExtras().getLong("mid"));

				}
			} else if (action
					.equals(JNIService.JNI_BROADCAST_FRIEND_AUTHENTICATION)) {
				V2Log.d(TAG,
						"having new friend verification message coming ... update..");
				long uid = intent.getLongExtra("uid", -1);
				if (uid == -1)
					return;

				if (verificationMessageItemLayout == null
						|| verificationMessageItemData == null)
					initVerificationItem();

				AddFriendHistorieNode node = MessageLoader
						.getNewestFriendVerificationMessage(mContext, new User(
								uid));
				if (node == null) {
					V2Log.d(TAG,
							"update friend verification message content failed... get null");
					return;
				} else
					node.readState = 0;

				updateFriendVerificationConversation(node);
			} else if (JNIService.JNI_BROADCAST_NEW_QUALIFICATION_MESSAGE
					.equals(intent.getAction())) {
				// If this can receive this broadcast, means
				// MessageAuthenticationActivity doesn't show, we need to show
				// red icon
				V2Log.d(TAG,
						"having new crowd verification message coming ... update..");
				if (verificationMessageItemLayout == null
						|| verificationMessageItemData == null)
					initVerificationItem();

				long msgId = intent.getLongExtra("msgId", 0);
				if (msgId == 0l) {
					V2Log.d(TAG,
							"update crowd verification message content failed... get 0 message id");
					return;
				}

				VMessageQualification msg = MessageBuilder
						.queryQualMessageById(mContext, msgId);
				if (msg == null) {
					V2Log.d(TAG,
							"update crowd verification message content failed... get null");
					return;
				}

				msg.setReadState(VMessageQualification.ReadState.UNREAD);
				updateCrowdVerificationConversation(msg);
			} else if (PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION
					.equals(intent.getAction())
					|| intent.getAction().equals(
							JNIService.JNI_BROADCAST_KICED_CROWD)) {
				long cid = intent.getLongExtra("crowd", -1l);
				if (cid == -1l) {
					V2Log.e(TAG,
							"Received the broadcast to quit the crowd group , but crowd id is wroing... ");
					return;
				}

				removeConversation(cid);
				// clear the crowd group all chat database messges
				MessageLoader.deleteMessageByID(context, mCurrentTabFlag, cid,
						-1);
				// clear the crowd group all verification database messges
				MessageLoader.deleteCrowdVerificationMessage(context, cid);
			} else if ((JNIService.BROADCAST_CROWD_NEW_UPLOAD_FILE_NOTIFICATION
					.equals(intent.getAction()))) {
				long groupID = intent.getLongExtra("groupID", -1l);
				if (groupID == -1l) {
					V2Log.e(TAG,
							"May receive new group upload files failed.. get empty collection");
					return;
				}

				updateGroupConversation(V2GlobalEnum.GROUP_TYPE_CROWD, groupID);
			} else if (PublicIntent.BROADCAST_USER_COMMENT_NAME_NOTIFICATION
					.equals(intent.getAction())) {
				Long uid = intent.getLongExtra("modifiedUser", -1);
				if (uid == -1l) {
					V2Log.e("ConversationsTabFragment BROADCAST_USER_COMMENT_NAME_NOTIFICATION ---> update user comment name failed , get id is -1");
					return;
				}

				for (int i = 0; i < mConvList.size(); i++) {
					Conversation conversation = mConvList.get(i);
					if (conversation.getType() == V2GlobalEnum.GROUP_TYPE_CONTACT) {
						ContactConversation con = (ContactConversation) conversation;
						if (con.getUserID() == uid) {
							((GroupLayout) mItemList.get(i).gp).update();
							adapter.notifyDataSetChanged();
						}
					}
				}
			} else if (JNIService.JNI_BROADCAST_GROUP_USER_ADDED.equals(intent
					.getAction())
					|| JNIService.JNI_BROADCAST_GROUP_USER_REMOVED
							.equals(intent.getAction())) {
				GroupUserObject guo = (GroupUserObject) intent.getExtras().get(
						"obj");

				for (ScrollItem item : mItemList) {
					Conversation con = item.cov;
					if (con.getExtId() == guo.getmUserId()) {
						GroupLayout currentGroupLayout = ((GroupLayout) item.gp);
						currentGroupLayout.update();
						adapter.notifyDataSetChanged();
						break;
					}
				}
			}
		}
	}

	private void updateVerificationMessage(String msg) {

		if (((MainApplication) getActivity().getApplication())
				.theAppIsRunningBackground()) {
			// 发通知
			Intent i = new Intent(getActivity(),
					MessageAuthenticationActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_CLEAR_TOP);

			PendingIntent pendingIntent = PendingIntent.getActivity(
					getActivity(), 0, i, 0);

			Notification notification = new Notification.Builder(getActivity())
					.setSmallIcon(R.drawable.ic_launcher)
					.setContentTitle(
							getResources().getString(
									R.string.status_bar_notification))
					.setContentText(msg).setAutoCancel(true).setTicker(msg)
					.setWhen(System.currentTimeMillis())
					.setContentIntent(pendingIntent).getNotification();
			((NotificationManager) getActivity().getSystemService(
					Activity.NOTIFICATION_SERVICE)).notify(0, notification);
		}

		if (mItemList.contains(verificationItem)) {
			mItemList.remove(verificationItem);
			mConvList.remove(verificationMessageItemData);
			mItemList.add(0, verificationItem);
			mConvList.add(0, verificationItem.cov);
		} else {
			mItemList.add(0, verificationItem);
			mConvList.add(0, verificationItem.cov);
		}
		adapter.notifyDataSetChanged();
	}

	private String showUnreadFriendAuthentication() {

		// ActivityManager activityManager=(ActivityManager)
		// getActivity().getSystemService(Activity.ACTIVITY_SERVICE);
		//
		// if(activityManager.getRunningTasks(1).get(0).topActivity.equals());

		boolean hasUnread;
		// 查出未读的第一条按时间顺序
		String sql = "select * from " + AddFriendHistroysHandler.tableName
				+ " where ReadState=0 order by SaveDate desc limit 1";
		Cursor cr = AddFriendHistroysHandler.select(getActivity(), sql,
				new String[] {});
		if ((cr != null) && (cr.getCount() == 0)) {
			hasUnread = false;
		} else {
			hasUnread = true;
		}
		cr.close();

		sql = "select * from " + AddFriendHistroysHandler.tableName
				+ " order by SaveDate desc limit 1";
		cr = AddFriendHistroysHandler.select(getActivity(), sql,
				new String[] {});

		if (cr == null)
			return null;

		if (cr.getCount() <= 0) {
			cr.close();
			return null;
		}

		String msg = "";
		String date = "";
		String dateLong = "";
		User user = null;
		if (cr.moveToFirst()) {
			AddFriendHistorieNode tempNode = new AddFriendHistorieNode();
			// _id integer primary key AUTOINCREMENT,0
			// OwnerUserID bigint,1
			// SaveDate bigint,2
			// FromUserID bigint,3
			// OwnerAuthType bigint,4
			// ToUserID bigint, 5
			// RemoteUserID bigint, 6
			// ApplyReason nvarchar(4000),7
			// RefuseReason nvarchar(4000), 8
			// AddState bigint ,9
			// ReadState bigint);10
			tempNode.ownerUserID = cr.getLong(1);
			tempNode.saveDate = cr.getLong(2);
			tempNode.fromUserID = cr.getLong(3);
			tempNode.ownerAuthType = cr.getLong(4);
			tempNode.toUserID = cr.getLong(5);
			tempNode.remoteUserID = cr.getLong(6);
			tempNode.applyReason = cr.getString(7);
			tempNode.refuseReason = cr.getString(8);
			tempNode.addState = cr.getLong(9);
			tempNode.readState = cr.getLong(10);

			user = GlobalHolder.getInstance().getUser(tempNode.remoteUserID);
			String name = user.getName();

			// 别人加我：允许任何人：0已添加您为好友，需要验证：1未处理，2已同意，3已拒绝
			// 我加别人：允许认识人：4你们已成为了好友，需要验证：5等待对方验证，4被同意（你们已成为了好友），6拒绝了你为好友
			if ((tempNode.fromUserID == tempNode.remoteUserID)
					&& (tempNode.ownerAuthType == 0)) {// 别人加我允许任何人
				msg = name + res.getString(R.string.friend_has_added);
			} else if ((tempNode.fromUserID == tempNode.remoteUserID)
					&& (tempNode.ownerAuthType == 1)) {// 别人加我不管我有没有处理
				msg = name
						+ res.getString(R.string.friend_apply_add_you_friend);
			} else if ((tempNode.fromUserID == tempNode.ownerUserID)
					&& (tempNode.addState == 0)) {// 我加别人等待验证
				msg = res.getString(R.string.friend_apply_add) + name
						+ res.getString(R.string.friend_add_you_waiting_verify);
			} else if ((tempNode.fromUserID == tempNode.ownerUserID)
					&& (tempNode.addState == 1)) {// 我加别人已被同意或我加别人不需验证
				msg = res.getString(R.string.friend_relation) + name
						+ res.getString(R.string.friend_becomed);
			} else if ((tempNode.fromUserID == tempNode.ownerUserID)
					&& (tempNode.addState == 2)) {// 我加别人已被拒绝
				msg = name + res.getString(R.string.friend_was_reject_apply);
			}
			date = DateUtil.getStringDate(tempNode.saveDate * 1000);
			dateLong = String.valueOf(tempNode.saveDate * 1000);
		}
		cr.close();

		if (verificationMessageItemLayout != null) {
			verificationMessageItemLayout.updateNotificator(hasUnread);
			if (verificationMessageItemData != null) {
				verificationMessageItemData.setMsg(msg);
				verificationMessageItemData.setDate(date);
				verificationMessageItemData.setDateLong(dateLong);
				ConversationFirendAuthenticationData friend = (ConversationFirendAuthenticationData) verificationMessageItemData;
				friend.setUser(user);
				friend.setMessageType(ConversationFirendAuthenticationData.VerificationMessageType.CONTACT_TYPE);
				verificationMessageItemLayout.update();
			}
			notificationListener.updateNotificator(Conversation.TYPE_CONTACT,
					hasUnread);
		}
		return msg;
	}

	class LocalHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case FILL_CONFS_LIST:
				if (mCurrentTabFlag == Conversation.TYPE_CONFERNECE) {
					List<Group> gl = GlobalHolder.getInstance().getGroup(
							Group.GroupType.CONFERENCE.intValue());
					if (gl != null && gl.size() > 0 && !isLoadedCov)
						populateConversation(gl);
				} else if (mCurrentTabFlag == Conversation.TYPE_GROUP) {
					List<Group> groupList = new ArrayList<Group>();
					// List<Group> organizationGroup =
					// GlobalHolder.getInstance()
					// .getGroup(Group.GroupType.ORG.intValue());
					// if (organizationGroup.size() > 0)
					// groupList.addAll(organizationGroup);
					List<Group> chatGroup = GlobalHolder.getInstance()
							.getGroup(Group.GroupType.CHATING.intValue());
					if (chatGroup.size() > 0)
						groupList.addAll(chatGroup);

					if (groupList.size() > 0 && !isLoadedCov)
						populateConversation(groupList);
				}
				if (mCurrentTabFlag == Conversation.TYPE_CONTACT) {
					if (!isLoadedCov) {
						loadUserConversation();
					}
				}

				break;

			case UPDATE_USER_SIGN:
				// long fromuidS = (Long) msg.obj;
				// for (ScrollItem item : mItemList) {
				// ((GroupLayout) item.gp).update();
				// if (item.cov.getExtId() == fromuidS
				// && Conversation.TYPE_CONTACT.equals(item.cov
				// .getType())) {
				// User u = GlobalHolder.getInstance().getUser(fromuidS);
				// updateConversationToDB(fromuidS,
				// u == null ? "" : u.getName());
				// break;
				// }
				// }
				break;
			case UPDATE_CONVERSATION:
				ConversationNotificationObject uno = (ConversationNotificationObject) msg.obj;
				if (uno.getType() == Conversation.TYPE_CONTACT)
					updateUserConversation(uno.getExtId(), uno.getMsgID());
				else
					updateGroupConversation(uno.getType(), uno.getExtId());
				break;
			case UPDATE_SEARCHED_LIST:
				break;
			case REMOVE_CONVERSATION:
				break;
			case REQUEST_ENTER_CONF:
				mContext.startService(new Intent(mContext,
						ConferencMessageSyncService.class));
				cb.requestEnterConference(new Conference((Long) msg.obj),
						new MessageListener(this, REQUEST_ENTER_CONF_RESPONSE,
								null));
				break;
			case REQUEST_ENTER_CONF_RESPONSE:
				if (mWaitingDialog != null && mWaitingDialog.isShowing()) {
					mWaitingDialog.dismiss();
					mWaitingDialog = null;

				}
				JNIResponse recr = (JNIResponse) msg.obj;
				if (recr.getResult() == JNIResponse.Result.SUCCESS) {
					Conference c = ((RequestEnterConfResponse) recr).getConf();
					c.setStartTime(mConvList.get(currentPosition).getDate());
					// conf.setChairman(c.getChairman());
					// // set enter flag to true
					// // Request grant speaking permission
					// // If chair man is current user, then automatically apply
					// // speaking
					// if (c.getChairman() == GlobalHolder.getInstance()
					// .getCurrentUserId()) {
					// doApplyOrReleaseSpeak();
					// }
					startConferenceActivity(c);
				} else if (recr.getResult() == RequestEnterConfResponse.Result.TIME_OUT) {
					Toast.makeText(mContext,
							R.string.error_request_enter_conference_time_out,
							Toast.LENGTH_SHORT).show();
					mContext.stopService(new Intent(mContext,
							ConferencMessageSyncService.class));
				} else {
					mContext.stopService(new Intent(mContext,
							ConferencMessageSyncService.class));
					Toast.makeText(mContext,
							R.string.error_request_enter_conference,
							Toast.LENGTH_SHORT).show();
				}
				break;
			case UPDATE_CONVERSATION_MESSAGE:
				mConvList = tempList;
				fillAdapter(tempList);
				break;
			case UPDATE_VERIFICATION_MESSAGE:
				if (msg.arg1 == VerificationMessageType.CROWD_TYPE.intValue()) {
					VMessageQualification message = (VMessageQualification) msg.obj;
					updateCrowdVerificationConversation(message);
				} else {
					AddFriendHistorieNode node = (AddFriendHistorieNode) msg.obj;
					updateFriendVerificationConversation(node);
				}
				break;
			}
		}
	}

	private void startConversationView(Conversation cov) {
		Intent i = new Intent(PublicIntent.START_CONVERSACTION_ACTIVITY);
		i.addCategory(PublicIntent.DEFAULT_CATEGORY);
		i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		i.putExtra("obj", new ConversationNotificationObject(cov));
		startActivity(i);
	}
}
