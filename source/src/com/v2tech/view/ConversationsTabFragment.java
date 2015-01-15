package com.v2tech.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.V2.jni.ImRequest;
import com.V2.jni.ind.V2User;
import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.db.ContentDescriptor;
import com.v2tech.db.provider.ConversationProvider;
import com.v2tech.db.provider.VerificationProvider;
import com.v2tech.db.provider.VoiceProvider;
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
import com.v2tech.util.ProgressUtils;
import com.v2tech.util.SearchUtils;
import com.v2tech.view.bo.ConversationNotificationObject;
import com.v2tech.view.bo.GroupUserObject;
import com.v2tech.view.conference.ConferenceActivity;
import com.v2tech.view.conference.GroupLayout;
import com.v2tech.view.contacts.VoiceMessageActivity;
import com.v2tech.view.contacts.add.AddFriendHistroysHandler;
import com.v2tech.view.conversation.CommonCallBack;
import com.v2tech.view.conversation.CommonCallBack.CommonUpdateConversationToCreate;
import com.v2tech.view.conversation.ConversationP2PAVActivity;
import com.v2tech.view.conversation.MessageLoader;
import com.v2tech.view.message.MessageAuthenticationActivity;
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
import com.v2tech.vo.DiscussionConversation;
import com.v2tech.vo.DiscussionGroup;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.OrgGroup;
import com.v2tech.vo.User;
import com.v2tech.vo.V2GlobalConstants;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.VMessageQualification;
import com.v2tech.vo.VMessageQualification.QualificationState;
import com.v2tech.vo.VMessageQualification.ReadState;
import com.v2tech.vo.VMessageQualification.Type;
import com.v2tech.vo.VMessageQualificationApplicationCrowd;
import com.v2tech.vo.VMessageQualificationInvitationCrowd;
import com.v2tech.vo.VideoBean;

public class ConversationsTabFragment extends Fragment implements TextWatcher,
		ConferenceListener, CommonUpdateConversationToCreate {
	private static final String TAG = "ConversationsTabFragment";
	private static final int FILL_CONFS_LIST = 2;
	private static final int VERIFICATION_TYPE_FRIEND = 5;
	private static final int VERIFICATION_TYPE_CROWD = 6;
	private static final int UPDATE_CONVERSATION = 9;
	private static final int UPDATE_SEARCHED_LIST = 11;
	private static final int REMOVE_CONVERSATION = 12;
	private static final int REQUEST_ENTER_CONF = 14;
	private static final int REQUEST_ENTER_CONF_RESPONSE = 15;
	public static final int REQUEST_UPDATE_CHAT_CONVERSATION = 17;

	private static final int UPDATE_CONVERSATION_MESSAGE = 16;
	private static final int UPDATE_VERIFICATION_MESSAGE = 17;
	private static final int QUIT_DISCUSSION_BOARD_DONE = 18;

	public static final int CONFERENCE_ENTER_CODE = 100;

	private View rootView;

	private Context mContext;

	private NotificationListener notificationListener;

	private BroadcastReceiver receiver;
	private IntentFilter intentFilter;

	private ConferenceService cb;
	private CrowdGroupService chatService;

	private Set<Conversation> mUnreadConvList;
	private List<ScrollItem> mItemList = new ArrayList<ScrollItem>();
	private List<ScrollItem> searchList = new ArrayList<ScrollItem>();
	private List<Long> offlineDissCov = new ArrayList<Long>();
	private List<Long> offLineConf = new ArrayList<Long>();
	private List<CovCache> covCacheList = new ArrayList<ConversationsTabFragment.CovCache>();

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

	private boolean isAddVerificationItem;
	private boolean isAddVoiceItem;

	private boolean hasUnreadVoice;

	private boolean isShowVerificationNotify;

	private ListView mConversationsListView;
	private ConversationsAdapter adapter = new ConversationsAdapter();

	private int mCurrentTabFlag;

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

	/**
	 * The PopupWindow was showen when onItemlongClick was call..
	 */
	private PopupWindow mPopup;
	private TextView mPouupView = null;

	/**
	 * Use to crowd tab for crowd selected or discussion selected
	 */
	private static final int SUB_TAB_CROWD = 0;
	private static final int SUB_TAB_DISCUSSION = 1;
	private int mCurrentSubTab = SUB_TAB_CROWD;
	private RadioGroup crowdDiscussion;
	private View subTabLayout;

	/**
	 * Use to mark which conference user entered..
	 */
	private Conference currentEntered;

	private ExecutorService service;
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
		CommonCallBack.getInstance().setConversationCreate(this);
		Message.obtain(mHandler, FILL_CONFS_LIST).sendToTarget();

		searchList = new ArrayList<ScrollItem>();
		mUnreadConvList = new HashSet<Conversation>();
		isCreate = true;
		res = getResources();
		initSpecificationItem();
	}

	private void initSpecificationItem() {
		// 判断只有消息界面，才添加这两个特殊item
		if (mCurrentTabFlag == Conversation.TYPE_CONTACT) {
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
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (rootView == null) {
			rootView = inflater.inflate(R.layout.tab_fragment_conversations,
					container, false);
			mConversationsListView = (ListView) rootView
					.findViewById(R.id.conversations_list_container);
			mConversationsListView.setAdapter(adapter);

			mConversationsListView.setOnItemClickListener(mItemClickListener);
			mConversationsListView
					.setOnItemLongClickListener(mItemLongClickListener);

			if (mCurrentTabFlag == Conversation.TYPE_GROUP) {
				subTabLayout = rootView
						.findViewById(R.id.crowd_discussion_switcher_ly);
				subTabLayout.setVisibility(View.VISIBLE);
				crowdDiscussion = (RadioGroup) rootView
						.findViewById(R.id.crowd_discussion_switcher);
				crowdDiscussion
						.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

							@Override
							public void onCheckedChanged(RadioGroup group,
									int checkedId) {
								if (checkedId == R.id.rb_discussion) {
									mCurrentSubTab = SUB_TAB_DISCUSSION;
									((RadioButton) group
											.findViewById(R.id.rb_discussion))
											.setTextColor(Color.WHITE);
									((RadioButton) group
											.findViewById(R.id.rb_crowd))
											.setTextColor(getResources()
													.getColor(
															R.color.button_text_color_blue));
									populateConversation(
											GroupType.DISCUSSION,
											GlobalHolder
													.getInstance()
													.getGroup(
															GroupType.DISCUSSION
																	.intValue()));
								} else if (checkedId == R.id.rb_crowd) {
									mCurrentSubTab = SUB_TAB_CROWD;
									populateConversation(
											GroupType.CHATING,
											GlobalHolder
													.getInstance()
													.getGroup(
															GroupType.CHATING
																	.intValue()));
									((RadioButton) group
											.findViewById(R.id.rb_discussion))
											.setTextColor(getResources()
													.getColor(
															R.color.button_text_color_blue));
									((RadioButton) group
											.findViewById(R.id.rb_crowd))
											.setTextColor(Color.WHITE);
								}
							}
						});
			}

		}
		return rootView;
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
			intentFilter.addAction(JNIService.JNI_BROADCAST_GROUP_NOTIFICATION);
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION);
			intentFilter
					.addAction(PublicIntent.BROADCAST_USER_COMMENT_NAME_NOTIFICATION);
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_OFFLINE_MESSAGE_END);
			intentFilter.addAction(JNIService.JNI_BROADCAST_GROUPS_LOADED);
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_USER_UPDATE_BASE_INFO);

			if (mCurrentTabFlag == Conversation.TYPE_CONFERNECE) {
				intentFilter
						.addAction(JNIService.JNI_BROADCAST_CONFERENCE_INVATITION);
				intentFilter
						.addAction(JNIService.JNI_BROADCAST_CONFERENCE_REMOVED);
				intentFilter
						.addAction(PublicIntent.BROADCAST_NEW_CONFERENCE_NOTIFICATION);
			} else {
				intentFilter.addAction(JNIService.JNI_BROADCAST_KICED_CROWD);
				intentFilter
						.addAction(PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION);
				intentFilter.addAction(JNIService.JNI_BROADCAST_GROUP_UPDATED);
				intentFilter
						.addAction(JNIService.JNI_BROADCAST_NEW_DISCUSSION_NOTIFICATION);
				intentFilter
						.addAction(PublicIntent.BROADCAST_DISCUSSION_QUIT_NOTIFICATION);
				intentFilter
						.addAction(JNIService.JNI_BROADCAST_GROUP_USER_ADDED);
				intentFilter
						.addAction(JNIService.JNI_BROADCAST_GROUP_USER_REMOVED);
			}

			if (mCurrentTabFlag == Conversation.TYPE_CONTACT) {
				intentFilter
						.addAction(ConversationP2PAVActivity.P2P_BROADCAST_MEDIA_UPDATE);
				intentFilter
						.addAction(JNIService.JNI_BROADCAST_GROUP_USER_ADDED);
				intentFilter.addAction(JNIService.JNI_BROADCAST_NEW_MESSAGE);
				intentFilter
						.addAction(JNIService.JNI_BROADCAST_CONTACTS_AUTHENTICATION);
				intentFilter
						.addAction(JNIService.JNI_BROADCAST_NEW_QUALIFICATION_MESSAGE);
				intentFilter
						.addAction(JNIService.BROADCAST_CROWD_NEW_UPLOAD_FILE_NOTIFICATION);
				intentFilter
						.addAction(PublicIntent.REQUEST_UPDATE_CONVERSATION);
				intentFilter
						.addAction(JNIService.JNI_BROADCAST_GROUP_USER_REMOVED);
				intentFilter
						.addAction(PublicIntent.BROADCAST_ADD_OTHER_FRIEND_WAITING_NOTIFICATION);
				intentFilter
						.addAction(PublicIntent.BROADCAST_AUTHENTIC_TO_CONVERSATIONS_TAB_FRAGMENT_NOTIFICATION);
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
		if (mCurrentTabFlag == V2GlobalConstants.GROUP_TYPE_USER && !isCreate)
			sortAndUpdate();
		isCreate = false;
	}

	@Override
	public void onStop() {
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

		if (subTabLayout != null) {
			if (mIsStartedSearch)
				subTabLayout.setVisibility(View.GONE);
			else
				subTabLayout.setVisibility(View.VISIBLE);
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
		if (resultCode == REQUEST_UPDATE_CHAT_CONVERSATION) {
			if (mCurrentTabFlag != V2GlobalConstants.GROUP_TYPE_USER)
				return;
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
			if (isDelete
					&& mCurrentTabFlag == V2GlobalConstants.GROUP_TYPE_USER) {
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
		} else if (resultCode == CONFERENCE_ENTER_CODE) {
			if (currentEntered == null)
				return;
			V2Log.d(TAG, "The Conf was exit ! current enter conf is : "
					+ currentEntered.getName());
			cb.requestExitConference(currentEntered, null);
			currentEntered = null;
			mContext.stopService(new Intent(mContext,
					ConferencMessageSyncService.class));
		}
	}

	private void sortAndUpdate() {
		Collections.sort(mItemList);
		adapter.notifyDataSetChanged();
	}

	public boolean updateVerificationConversation() {
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

	private void populateConversation(GroupType populateType, List<Group> list) {
		mItemList.clear();
		adapter.notifyDataSetChanged();

		if (list == null || list.size() <= 0) {
			V2Log.e("ConversationsTabFragment populateConversation --> Given Group List is null ... please checked!");
			return;
		}

		List<Conversation> tempList = new ArrayList<Conversation>();
		for (int i = list.size() - 1; i >= 0; i--) {
			Group g = list.get(i);
			if (populateType != g.getGroupType()) {
				V2Log.e(TAG, "填充数据时, 发生数据类型不匹配！ group id is : " + g.getmGId()
						+ " and name is : " + g.getName() + " and type is : "
						+ g.getGroupType().name());
				continue;
			}

			boolean isReturn = false;
			for (ScrollItem item : mItemList) {
				if (item.cov.getExtId() == g.getmGId()) {
					if (offLineConf.contains(g.getmGId())) {
						item.cov.setReadFlag(Conversation.READ_FLAG_UNREAD);
						updateUnreadConversation(item);
						offLineConf.remove(g.getmGId());
					}
					isReturn = true;
				}
			}

			if (isReturn)
				continue;

			Conversation cov;
			if (g.getGroupType() == GroupType.CONFERENCE) {
				cov = new ConferenceConversation(g);
			} else if (g.getGroupType() == GroupType.CHATING) {
				cov = new CrowdConversation(g);
			} else if (g.getGroupType() == GroupType.DISCUSSION) {
				cov = new DiscussionConversation(g);
			} else if (g.getGroupType() == GroupType.ORG) {
				cov = new DepartmentConversation(g);
			} else {
				continue;
			}

			// Update all initial conversation to read
			cov.setReadFlag(Conversation.READ_FLAG_READ);
			long time = GlobalConfig.getGlobalServerTime();
			cov.setDate(DateUtil.getStandardDate(new Date(time)));
			tempList.add(cov);
		}
		sendMessageToFillAdapter(populateType, tempList);
	}

	/**
	 * Add conversation to list
	 * 
	 * @param g
	 * @param flag
	 */
	private void addConversation(Group g, boolean flag) {

		if (g == null) {
			V2Log.e(TAG,
					"addConversation --> Add new conversation failed ! Given Group is null");
			return;
		}

		Conversation cov;
		ScrollItem currentItem = null;
		if (g.getGroupType() == GroupType.CONFERENCE) {
			cov = new ConferenceConversation(g);
		} else if (g.getGroupType() == GroupType.CHATING) {
			if (mCurrentSubTab == SUB_TAB_DISCUSSION)
				return;
			cov = new CrowdConversation(g);
		} else if (g.getGroupType() == GroupType.DISCUSSION) {
			if (mCurrentSubTab == SUB_TAB_CROWD)
				return;
			cov = new DiscussionConversation(g);
		} else {
			V2Log.e(TAG,
					"addConversation --> Add new group conversation failed ... "
							+ "the group type is : " + g.getGroupType().name());
			return;
		}

		boolean isAdd = true;
		for (ScrollItem item : mItemList) {
			Conversation conversation = item.cov;
			if (conversation.getExtId() == cov.getExtId()) {
				currentItem = item;
				isAdd = false;
				break;
			}
		}

		if (isAdd) {
			V2Log.d(TAG,
					"addConversation -- Successfully add a new conversation , type is : "
							+ cov.getType() + " and id is : " + cov.getExtId()
							+ " and name is : " + cov.getName());
			GroupLayout gp = new GroupLayout(mContext, cov);
			/**
			 * 除了个人Conversation布局，其他组类Conversation布局默认可能不会显示时间，或者内容
			 * 所以这里需要将布局改变为个人的Conversation布局
			 */
			if (cov.getType() == V2GlobalConstants.GROUP_TYPE_DISCUSSION) {
				if (mCurrentTabFlag == V2GlobalConstants.GROUP_TYPE_USER) {
					gp.updateDiscussionLayout(true);
					gp.update();
				} else {
					gp.updateDiscussionLayout(false);
				}
				gp.update();
			}

			ScrollItem newItem = new ScrollItem(cov, gp);
			currentItem = newItem;
			mItemList.add(0, newItem);
		} else {
			V2Log.d(TAG,
					"addConversation -- The Group Conversation already exist, type is : "
							+ cov.getType() + " and id is : " + cov.getExtId()
							+ " and name is : " + cov.getName());
		}

		if (flag) {
			currentItem.cov.setReadFlag(Conversation.READ_FLAG_UNREAD);
		} else {
			currentItem.cov.setReadFlag(Conversation.READ_FLAG_READ);
		}

		// Update unread conversation list
		if (cov.getReadFlag() == Conversation.READ_FLAG_UNREAD) {
			updateUnreadConversation(currentItem);
		}
		adapter.notifyDataSetChanged();
	}

	/**
	 * Load local conversation list
	 */
	private void loadUserConversation() {
		if (isLoadedCov) {
			return;
		}
		V2Log.e(TAG, "LoadUserConversation was called!");
		service.execute(new Runnable() {

			@Override
			public void run() {
				synchronized (ConversationsTabFragment.class) {
					List<Conversation> tempList = new ArrayList<Conversation>();
					tempList = ConversationProvider.loadUserConversation(
							tempList, verificationMessageItemData,
							voiceMessageItem);
					fillUserAdapter(tempList);
				}
			}
		});

	}

	private void sendMessageToFillAdapter(GroupType type,
			List<Conversation> conversations) {
		Message message = Message.obtain(mHandler, UPDATE_CONVERSATION_MESSAGE,
				new ConversationList(conversations));
		message.arg1 = type.intValue();
		message.sendToTarget();
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
								"when fillUserAdapter , get null Conversation , index :"
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
					case Conversation.TYPE_DISCUSSION:
						if (offlineDissCov.contains(cov.getExtId()))
							continue;
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

				if (mCurrentTabFlag == V2GlobalConstants.GROUP_TYPE_USER
						&& !isCallBack) {
					isCallBack = true;
					CommonCallBack.getInstance()
							.executeUpdateConversationState();
				}
			}
		});
	}

	private void fillAdapter(GroupType fillType, List<Conversation> list) {
		for (int i = 0; i < list.size(); i++) {
			Conversation cov = list.get(i);
			if (cov == null) {
				V2Log.e(TAG,
						"when fillAdapter , get null Conversation , index :"
								+ i);
				continue;
			}

			if (cov.getType() != fillType.intValue()) {
				V2Log.e(TAG,
						"填充数据时, 发生数据类型不匹配！ Conversation id is : "
								+ cov.getExtId() + " and name is : "
								+ cov.getName() + " and type is : "
								+ cov.getType());
				continue;
			}

			GroupLayout layout = new GroupLayout(mContext, cov);
			layout.update();
			// 需要调用updateGroupContent
			Group fillGroup = null;
			if (cov.getType() == V2GlobalConstants.GROUP_TYPE_CROWD) {
				fillGroup = ((CrowdConversation) cov).getGroup();
			} else if (cov.getType() == V2GlobalConstants.GROUP_TYPE_CONFERENCE) {
				fillGroup = ((ConferenceConversation) cov).getGroup();
			}

			if (fillGroup != null)
				layout.updateGroupContent(fillGroup);

			ScrollItem newItem = new ScrollItem(cov, layout);
			mItemList.add(newItem);
			if (cov.getReadFlag() == Conversation.READ_FLAG_UNREAD) {
				updateUnreadConversation(newItem);
			}
		}
		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				adapter.notifyDataSetChanged();
				isLoadedCov = true;
				V2Log.w(TAG, "The ListView already fill over !  , type is : "
						+ mCurrentTabFlag);
			}
		});
	}

	/**
	 * 初始化通话消息item对象
	 */
	private void initVoiceItem() {
		voiceMessageItem = new Conversation(Conversation.TYPE_VOICE_MESSAGE,
				Conversation.SPECIFIC_VOICE_ID);
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
				Conversation.TYPE_VERIFICATION_MESSAGE,
				Conversation.SPECIFIC_VERIFICATION_ID);
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
					sendVoiceNotify();
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

	private ScrollItem makeNewGroupItem(int groupType, long groupID) {

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
		VMessage vm = MessageLoader.getNewestGroupMessage(mContext, groupType,
				groupID);
		if (vm != null) {
			cov.setDate(vm.getStringDate());
			cov.setDateLong(String.valueOf(vm.getmDateLong()));
			CharSequence newMessage = MessageUtil.getMixedConversationContent(
					mContext, vm);
			cov.setMsg(newMessage);
			cov.setReadFlag(Conversation.READ_FLAG_UNREAD);
			ConversationProvider.saveConversation(vm);
		} else
			V2Log.e(TAG,
					"makeNewGroupItem ---> get newest VMessage is null , update failed");
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
		if (mCurrentTabFlag == V2GlobalConstants.GROUP_TYPE_USER) {
			if (!isLoadedCov) {
				V2Log.e(TAG,
						"fill adapter isn't finish when update group conversation in USER INTERFACE!");
				return;
			}
		} else {
			V2Log.e(TAG,
					"fill adapter isn't finish when update group conversation!");
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

		if (foundFlag) {
			Group group = GlobalHolder.getInstance().getGroupById(groupType,
					groupID);
			switch (mCurrentTabFlag) {
			case V2GlobalConstants.GROUP_TYPE_USER:
				if (group != null)
					existedCov.setName(group.getName());
				existedCov.setDate(vm.getStringDate());
				existedCov.setDateLong(String.valueOf(vm.getmDateLong()));
				CharSequence newMessage = MessageUtil
						.getMixedConversationContent(mContext, vm);
				existedCov.setMsg(newMessage);
				break;
			}
		} else {
			if (mCurrentTabFlag == V2GlobalConstants.GROUP_TYPE_USER) {
				ScrollItem newItem = makeNewGroupItem(groupType, groupID);
				currentItem = newItem;
				existedCov = newItem.cov;
				viewLayout = (GroupLayout) newItem.gp;
				mItemList.add(0, newItem);
			}
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
			if (!TextUtils.isEmpty(user.getName()))
				name = user.getName();
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
				showAuthenticationNotification = true;
				if (verificationMessageItemData.getMsg() != null) {
					updateVerificationStateBar(verificationMessageItemData
							.getMsg().toString(),
							VerificationMessageType.CROWD_TYPE);
				}
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
				if (TextUtils.isEmpty(invitation.getInvitationUser().getName())) {
					User user = GlobalHolder.getInstance().getUser(
							invitation.getInvitationUser().getmUserId());
					if (!user.isDirty()) {
						boolean isFriend = GlobalHolder.getInstance().isFriend(
								user);
						if (isFriend && !TextUtils.isEmpty(user.getNickName()))
							invitationName = user.getNickName();
						else
							invitationName = user.getName();
					}
				} else {
					User user = invitation.getInvitationUser();
					boolean isFriend = GlobalHolder.getInstance()
							.isFriend(user);
					if (isFriend && !TextUtils.isEmpty(user.getNickName()))
						invitationName = user.getNickName();
					else
						invitationName = user.getName();
				}

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
					if (!user.isDirty()) {
						boolean isFriend = GlobalHolder.getInstance().isFriend(
								user);
						if (isFriend && !TextUtils.isEmpty(user.getNickName()))
							applyName = user.getNickName();
						else
							applyName = user.getName();
					}
				} else {
					User user = apply.getApplicant();
					boolean isFriend = GlobalHolder.getInstance()
							.isFriend(user);
					if (isFriend && !TextUtils.isEmpty(user.getNickName()))
						applyName = user.getNickName();
					else
						applyName = user.getName();
				}

				if (apply.getQualState() == QualificationState.BE_REJECT)
					content = applyName + "拒绝加入" + applyGroup.getName() + "群";
				else if (apply.getQualState() == QualificationState.BE_ACCEPTED)
					content = applyName + "同意加入" + applyGroup.getName() + "群";
				else
					content = applyName + "申请加入" + applyGroup.getName() + "群";
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
		if (mCurrentTabFlag != V2GlobalConstants.GROUP_TYPE_USER)
			return;

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
			notificationListener.updateNotificator(mCurrentTabFlag, true);
		} else {
			notificationListener.updateNotificator(mCurrentTabFlag, false);
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

					if (mCurrentTabFlag == V2GlobalConstants.GROUP_TYPE_CROWD) {
						final Group g = ((CrowdConversation) item.cov)
								.getGroup();
						if (g == null || g.getOwnerUser() == null)
							continue;

						final GroupLayout currentGroupLayout = (GroupLayout) item.gp;

						final User u = GlobalHolder.getInstance().getUser(
								g.getOwnerUser().getmUserId());

						getActivity().runOnUiThread(new Runnable() {

							@Override
							public void run() {
								g.setOwnerUser(u);
								currentGroupLayout.updateGroupContent(g);
								currentGroupLayout.updateGroupName(g.getName());
							}
						});
					} else if (mCurrentTabFlag == V2GlobalConstants.GROUP_TYPE_USER) {

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
								VMessage vm = MessageLoader
										.getNewestGroupMessage(
												mContext,
												V2GlobalConstants.GROUP_TYPE_CROWD,
												crowd.getExtId());
								crowd.setName(newGroup.getName());
								updateGroupInfo(currentGroupLayout, crowd, vm);
							}
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
		boolean flag = false;
		for (int i = 0; i < mItemList.size(); i++) {
			Conversation temp = mItemList.get(i).cov;
			if (temp.getExtId() == conversationID) {
				ScrollItem removeItem = mItemList.get(i);
				removeItem.cov.setReadFlag(Conversation.READ_FLAG_READ);
				updateUnreadConversation(removeItem);

				flag = mItemList.remove(removeItem);
				if (mCurrentTabFlag == V2GlobalConstants.GROUP_TYPE_USER) {
					if (temp.getType() != Conversation.TYPE_VOICE_MESSAGE
							&& temp.getType() != Conversation.TYPE_VERIFICATION_MESSAGE) {
						// delete conversation
						ConversationProvider.deleteConversation(mContext, temp);
						// delete messages
						if (temp.getType() == Conversation.TYPE_CONTACT) {
							MessageLoader.deleteMessageByID(mContext,
									Conversation.TYPE_CONTACT, -1,
									temp.getExtId(), false);
						} else {
							// clear the crowd group all chat database messges
							MessageLoader.deleteMessageByID(mContext,
									temp.getType(), temp.getExtId(), -1, false);
						}
						V2Log.d(TAG,
								" Successfully remove contact conversation , id is : "
										+ conversationID);
					}

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
					}

					if (temp.getType() == Conversation.TYPE_VOICE_MESSAGE) {
						MessageLoader.deleteVoiceMessage(-1);
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
				Notificator.cancelSystemNotification(getActivity(),
						PublicIntent.MESSAGE_NOTIFICATION_ID);
				break;
			}
		}

		if (!flag)
			V2Log.e(TAG, "Delete Conversation Failed...id is : "
					+ conversationID);

		if (mCurrentTabFlag == V2GlobalConstants.GROUP_TYPE_USER
				&& isDeleteVerification) {
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

		if (!GlobalConfig.isApplicationBackground(mContext)) {
			sendVoiceNotify();
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
			content = vm.getAllTextContent();
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
					.getName(), content, 1, resultIntent,
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
					content, 1, resultIntent,
					PublicIntent.MESSAGE_NOTIFICATION_ID);
		}
	}

	private void updateConferenceNotification(Conference conf) {
		if (checkSendingState()) {
			return;
		}

		if (!GlobalConfig.isApplicationBackground(mContext)) {
			sendVoiceNotify();
			return;
		}

		Intent enterConference = new Intent(mContext, MainActivity.class);
		User creator = GlobalHolder.getInstance().getUser(conf.getCreator());
		enterConference.putExtra("conf", conf);
		enterConference.putExtra("initFragment", 3);
		Notificator.updateSystemNotification(mContext, creator == null ? ""
				: creator.getName(), "邀请你参加会议 " + conf.getName(), 1,
				enterConference, PublicIntent.VIDEO_NOTIFICATION_ID);
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

		if (!GlobalConfig.isApplicationBackground(mContext)) {
			sendVoiceNotify();
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
			long uid = -1;
			if (crowdVerificationMessage.getType() == Type.CROWD_APPLICATION)
				uid = ((VMessageQualificationApplicationCrowd) crowdVerificationMessage)
						.getApplicant().getmUserId();
			else
				uid = ((VMessageQualificationInvitationCrowd) crowdVerificationMessage)
						.getInvitationUser().getmUserId();
			User remote = GlobalHolder.getInstance().getUser(uid);
			if (remote.isDirty()) {
				// The user info need to get
				isOutOrgShow = true;
				V2Log.e(TAG,
						"The current show Verification info need to update!");
			}
		}
	}

	/**
	 * 判断是否有离线讨论组加进来
	 */
	private void checkNewGroup() {
		List<String> databases = GlobalHolder.getInstance()
				.getDataBaseTableCacheName();
		for (int i = 0; i < databases.size(); i++) {
			V2Log.e("NEW_GROUP",
					"iterator database name is : " + databases.get(i));
			Pattern pattern = Pattern.compile("Histories_5_");
			Matcher matcher = pattern.matcher(databases.get(i));
			if (matcher.find()) {
				V2Log.e("NEW_GROUP",
						"match database name is : " + databases.get(i));
				boolean alreadExist = false;
				Conversation cov = null;
				for (int j = 0; j < mItemList.size(); j++) {
					cov = mItemList.get(j).cov;
					if (cov.getType() == V2GlobalConstants.GROUP_TYPE_DISCUSSION) {
						String target = "Histories_5_" + cov.getExtId() + "_0";
						if (databases.get(i).equals(target)) {
							V2Log.e("NEW_GROUP",
									"match conversation name is : "
											+ databases.get(i)
											+ " already exist!");
							alreadExist = true;
							break;
						}
					}
				}

				if (!alreadExist) {
					V2Log.e("NEW_GROUP", "match conversation name is : "
							+ databases.get(i) + " not exist!");
					addConversation(
							GlobalHolder.getInstance().getGroupById(
									V2GlobalConstants.GROUP_TYPE_DISCUSSION,
									cov.getExtId()), true);
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
				if (user.isDirty()) {
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

	private void checkRepeatVerification() {
		service.execute(new Runnable() {

			@Override
			public void run() {
				while (!isLoadedCov) {
					SystemClock.sleep(1000);
					V2Log.e(TAG,
							"checkRepeatVerification --> waiting for message interface fill adapter ......");
				}

				V2Log.d(TAG,
						"CHECK VERIFICATION CONVERSATION EXIST! 开始检测到验证会话！");
				boolean isFound = false;
				for (int j = 0; j < mItemList.size(); j++) {
					Conversation cov = mItemList.get(j).cov;
					if (cov.getExtId() == verificationMessageItemData
							.getExtId()) {
						if (isFound == false) {
							isFound = true;
							V2Log.d(TAG,
									"CHECK VERIFICATION CONVERSATION EXIST! 检测到验证会话，已经存在！");
						} else {
							V2Log.d(TAG,
									"CHECK VERIFICATION CONVERSATION EXIST!  验证会话重复，删除一个！");
							mItemList.remove(j);
							getActivity().runOnUiThread(new Runnable() {

								@Override
								public void run() {
									sortAndUpdate();
								}
							});
						}
					}

					// getActivity().runOnUiThread(new Runnable() {
					//
					// @Override
					// public void run() {
					// updateVerificationConversation();
					// }
					// });
				}
			}
		});
	}

	private MediaPlayer mChatPlayer;

	public void sendVoiceNotify() {
		// if ((System.currentTimeMillis() / 1000) - lastNotificatorTime > 2) {
		// Uri notification = RingtoneManager
		// .getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		// Ringtone r = RingtoneManager.getRingtone(mContext, notification);
		// if (r != null) {
		// r.play();
		// }
		// lastNotificatorTime = System.currentTimeMillis() / 1000;
		// }
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
			if (GlobalConfig.isApplicationBackground(mContext)) {
				sendVoiceNotify();
			}
			return true;
		}
		return false;
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
				if (!GlobalHolder.getInstance().isServerConnected()) {
					Toast.makeText(mContext,
							R.string.error_discussion_no_network,
							Toast.LENGTH_SHORT).show();
					return;
				}

				Group crowd = (Group) GlobalHolder.getInstance().getGroupById(
						currentClickConversation.getExtId());
				// If group is null, means we have removed this conversaion
				if (crowd != null) {
					if (crowd.getGroupType() == GroupType.CHATING) {
						chatService.quitCrowd((CrowdGroup) crowd, null);
					} else if (crowd.getGroupType() == GroupType.DISCUSSION) {
						chatService.quitDiscussionBoard(
								(DiscussionGroup) crowd, new MessageListener(
										mHandler, QUIT_DISCUSSION_BOARD_DONE,
										crowd));
					}
				} else
					V2Log.e(TAG, "quit crowd group failed .. id is :"
							+ currentClickConversation.getExtId());
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

		Group cg = (Group) GlobalHolder.getInstance().getGroupById(
				currentClickConversation.getExtId());
		if (cg.getGroupType() == GroupType.CHATING) {
			if (cg.getOwnerUser().getmUserId() == GlobalHolder.getInstance()
					.getCurrentUserId())
				mPouupView
						.setText(R.string.crowd_detail_qulification_dismiss_button);
			else
				mPouupView
						.setText(R.string.crowd_detail_qulification_quit_button);
		} else if (cg.getGroupType() == GroupType.DISCUSSION) {
			mPouupView.setText(R.string.discussion_board_detail_quit_button);
		}

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
			// When request join conference response, mWaitingDialog will be null
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

	private void startConferenceActivity(Conference conf) {
		// Set current state to in meeting state
		GlobalHolder.getInstance().setMeetingState(true, conf.getId());
		Intent enterConference = new Intent(mContext, ConferenceActivity.class);
		enterConference.putExtra("conf", conf);
		enterConference.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
		currentEntered = conf;
		this.startActivityForResult(enterConference, CONFERENCE_ENTER_CODE);
	}

	private Intent startAuthenticationActivity(Intent intent,
			VerificationMessageType messageType) {

		if (messageType == VerificationMessageType.CONTACT_TYPE)
			intent.putExtra("isFriendActivity", true);
		else
			intent.putExtra("isFriendActivity", false);

		intent.putExtra("isCrowdShowNotification",
				showAuthenticationNotification);
		showAuthenticationNotification = false;
		return intent;
	}

	private void initUpdateCovGroupList(final int groupType, final long groupID) {
		if (groupType == V2GlobalConstants.GROUP_TYPE_CROWD
				|| groupType == V2GlobalConstants.GROUP_TYPE_DISCUSSION) {
			if (mCurrentTabFlag == V2GlobalConstants.GROUP_TYPE_CROWD) {
				service.execute(new Runnable() {

					@Override
					public void run() {
						int count = 5;
						while (!isLoadedCov) {
							SystemClock.sleep(1000);
							count--;
							if (count == 0)
								break;
							V2Log.e(TAG,
									"initUpdateCovGroupList --> waiting for group fill adapter ...."
											+ " type is : CROWD!");
						}

						for (int i = 0; i < mItemList.size(); i++) {
							ScrollItem item = mItemList.get(i);
							if (item.cov.getExtId() == groupID) {
								Conversation currentConversation = item.cov;
								GroupLayout currentGroupLayout = ((GroupLayout) item.gp);
								if (currentConversation.getType() == V2GlobalConstants.GROUP_TYPE_CROWD) {
									Group newGroup = GlobalHolder.getInstance()
											.getGroupById(
													Conversation.TYPE_GROUP,
													groupID);
									((CrowdConversation) currentConversation)
											.setGroup(newGroup);
									updateCovContent(currentGroupLayout,
											newGroup, false);
									V2Log.d(TAG,
											"update converstaion over , type is : "
													+ groupType + " and"
													+ " group id : " + groupID
													+ " and name is : "
													+ newGroup.getName());
								} else {
									Group newGroup = GlobalHolder
											.getInstance()
											.getGroupById(
													Conversation.TYPE_DISCUSSION,
													groupID);
									if (newGroup != null) {
										((DiscussionConversation) currentConversation)
												.setDiscussionGroup(newGroup);
										updateCovContent(currentGroupLayout,
												newGroup, false);
										V2Log.d(TAG,
												"update crowd or diss converstaion over , type is : "
														+ groupType + " and"
														+ " group id : "
														+ groupID
														+ " and name is : "
														+ newGroup.getName());
									}
								}
								break;
							}
						}
					}
				});
			}
		} else if (groupType == V2GlobalConstants.GROUP_TYPE_DEPARTMENT) {
			if (mCurrentTabFlag == V2GlobalConstants.GROUP_TYPE_CONFERENCE) {
				service.execute(new Runnable() {

					@Override
					public void run() {
						int count = 5;
						while (!isLoadedCov) {
							SystemClock.sleep(1000);
							count--;
							if (count == 0)
								break;
							V2Log.e(TAG,
									"initUpdateCovGroupList --> waiting for group fill adapter ...."
											+ " type is : CONFERENCE!");
						}

						for (int i = 0; i < mItemList.size(); i++) {
							ScrollItem item = mItemList.get(i);
							GroupLayout currentGroupLayout = ((GroupLayout) item.gp);
							ConferenceConversation currentConversation = (ConferenceConversation) item.cov;
							Group newGroup = GlobalHolder
									.getInstance()
									.getGroupById(
											V2GlobalConstants.GROUP_TYPE_CONFERENCE,
											currentConversation.getExtId());
							if (newGroup != null) {
								User owner = GlobalHolder.getInstance()
										.getUser(
												newGroup.getOwnerUser()
														.getmUserId());
								if (TextUtils.isEmpty(owner.getName())) {
									ImRequest.getInstance().getUserBaseInfo(
											owner.getmUserId());
								} else {
									newGroup.setOwnerUser(owner);
									currentConversation.setG(newGroup);
									updateCovContent(currentGroupLayout,
											newGroup, true);
									V2Log.d(TAG,
											"update conference converstaion over , cov name is : "
													+ currentConversation
															.getName()
													+ " and"
													+ " creator name is : "
													+ newGroup.getOwnerUser()
															.getName());
								}
							}
						}
					}
				});
			}
		}
	}

	private void updateCovContent(final GroupLayout currentGroupLayout,
			final Group newGroup, final boolean isConference) {
		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (isConference){
					if(offLineConf.contains(newGroup.getmGId())){
						currentGroupLayout.updateConversationNotificator(true);
					}
				} else {
					currentGroupLayout.update();
				}
				
				currentGroupLayout.updateGroupContent(newGroup);
			}
		});
	}

	/**
	 * This request from main activity
	 * 
	 * @see com.v2tech.view.ConferenceListener#requestJoinConf(com.v2tech.vo.Conference
	 *      )
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
			ScrollItem item = mItemList.get(i);
			// update main activity notificator
			item.cov.setReadFlag(Conversation.READ_FLAG_READ);
			updateUnreadConversation(item);
		}
		return true;
	}

	class ConversationList {

		public ConversationList(List<Conversation> conversationList) {
			this.conversationList = conversationList;
		}

		public List<Conversation> conversationList;
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
				return -1;
			String localTime = cov.getDateLong();
			String remoteTime = item.cov.getDateLong();
			if (TextUtils.isEmpty(localTime))
				return 1;

			if (TextUtils.isEmpty(remoteTime))
				return -1;

			if (Long.valueOf(localTime) < Long.valueOf(remoteTime))
				return 1;
			else
				return -1;
		}
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
			if (mCurrentTabFlag == Conversation.TYPE_CONFERNECE) {
				if (offLineConf.contains(cov.getExtId()))
					offLineConf.remove(cov.getExtId());
				requestJoinConf(cov.getExtId());
			} else if (mCurrentTabFlag == Conversation.TYPE_GROUP)
				startConversationView(cov);
			else if (mCurrentTabFlag == Conversation.TYPE_CONTACT) {
				if (cov.getType() == Conversation.TYPE_VOICE_MESSAGE) {
					Intent intent = new Intent(mContext,
							VoiceMessageActivity.class);
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
					startConversationView(cov);
				}
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
			if (mCurrentTabFlag == Conversation.TYPE_GROUP) {
				showPopupWindow(v);
			} else {
				if (mCurrentTabFlag == Conversation.TYPE_CONFERNECE) {
					item = new String[] { mContext.getResources().getString(
							R.string.conversations_delete_conf) };
				} else {
					// Conversation.TYPE_CONTACT
					item = new String[] { mContext.getResources().getString(
							R.string.conversations_delete_conversaion) };
				}

				AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
				builder.setTitle(mItemList.get(pos).cov.getName()).setItems(
						item, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {

								if (!((MainApplication) getActivity()
										.getApplication()).netWordIsConnected) {
									dialog.dismiss();
									return;
								}

								if (which == 0) {
									switch (mCurrentTabFlag) {
									case Conversation.TYPE_CONFERNECE:
										if (!GlobalHolder.getInstance()
												.isServerConnected()) {
											Toast.makeText(
													mContext,
													R.string.error_local_connect_to_server,
													Toast.LENGTH_SHORT).show();
											return;
										}
										Group g = GlobalHolder.getInstance()
												.getGroupById(
														GroupType.CONFERENCE
																.intValue(),
														currentClickConversation
																.getExtId());
										// If group is null, means we have
										// removed
										// this conversaion
										if (g != null) {
											cb.quitConference(
													new Conference(
															currentClickConversation
																	.getExtId(),
															g.getOwnerUser()
																	.getmUserId()),
													null);
										}
									case Conversation.TYPE_CONTACT:
										removeConversation(
												currentClickConversation
														.getExtId(), false);
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
				if ((type == GroupType.CONFERENCE.intValue() && mCurrentTabFlag == Conversation.TYPE_CONFERNECE)
						|| (type == GroupType.CHATING.intValue() && mCurrentTabFlag == Conversation.TYPE_GROUP)) {
					Message.obtain(mHandler, FILL_CONFS_LIST).sendToTarget();
				}

				if (((type == GroupType.CHATING.intValue()))
						&& mCurrentTabFlag == V2GlobalConstants.GROUP_TYPE_USER
						&& !isUpdateGroup) {
					isUpdateGroup = true;
					updateMessageGroupName();
				} else if (type == GroupType.ORG.intValue()
						&& mCurrentTabFlag == V2GlobalConstants.GROUP_TYPE_USER
						&& !isUpdateDeparment) {
					isUpdateDeparment = true;
					updateDepartmentGroupName();
				}
				// From this broadcast, user has already read conversation
			} else if (JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION
					.equals(intent.getAction())) {
				int groupType = intent.getIntExtra("gtype", -1);
				long groupID = intent.getLongExtra("gid", -1);
				if (mCurrentTabFlag != V2GlobalConstants.GROUP_TYPE_USER) {
					V2Log.w(TAG,
							"the GROUP_USER_UPDATEED Comming over , type is : "
									+ groupType + " id is : " + groupID);
					if (isLoadedCov) {
						initUpdateCovGroupList(groupType, groupID);
					} else {
						covCacheList.add(new CovCache(groupType, groupID));
					}
				} else if (mCurrentTabFlag == V2GlobalConstants.GROUP_TYPE_USER) {
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
								msg.arg1 = verification.getMessageType()
										.intValue();
								msg.sendToTarget();
							} else {
								Message msg = Message.obtain(mHandler,
										UPDATE_VERIFICATION_MESSAGE);
								msg.arg1 = verification.getMessageType()
										.intValue();
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
												+ user.getName());
							} else {
								V2Log.d(TAG,
										"update the user infos failed ... beacuse get user is null from globleHolder! id is : "
												+ contact.getExtId());
							}
							break;
						case Conversation.TYPE_DEPARTMENT:
							Group depart = GlobalHolder.getInstance()
									.getGroupById(Conversation.TYPE_DEPARTMENT,
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
								VMessage vm = MessageLoader
										.getNewestGroupMessage(
												mContext,
												V2GlobalConstants.GROUP_TYPE_DISCUSSION,
												discussion.getExtId());
								if (vm != null) {
									discussion.setDate(vm.getStringDate());
									discussion.setDateLong(String.valueOf(vm
											.getmDateLong()));
									CharSequence newMessage = MessageUtil
											.getMixedConversationContent(
													mContext, vm);
									discussion.setMsg(newMessage);
									currentGroupLayout.update();
								} else
									V2Log.w("DISCUSSION",
											"没有获取到最新VMessage对象! 更新内容失败");
								V2Log.e("DISCUSSION",
										"update discussion group successful , id is : "
												+ currentConversation
														.getExtId()
												+ " name is : "
												+ currentConversation.getName());
							} else {
								V2Log.w(TAG, "没有获取到讨论组对象! 更新失败！ id is : "
										+ discussion.getExtId());
							}
							break;
						case Conversation.TYPE_GROUP:
							Group crowd = GlobalHolder.getInstance()
									.getGroupById(Conversation.TYPE_GROUP,
											currentConversation.getExtId());
							((CrowdConversation) currentConversation)
									.setGroup(crowd);
							currentGroupLayout.update();
							V2Log.d(TAG,
									"update crowd group successful , id is : "
											+ currentConversation.getExtId()
											+ " name is : "
											+ currentConversation.getName());
							break;
						}
					}
				}
			} else if (JNIService.JNI_BROADCAST_OFFLINE_MESSAGE_END
					.equals(intent.getAction())) {
				GlobalHolder.getInstance().setOfflineLoaded(true);
				V2Log.d(TAG,
						"JNI_BROADCAST_OFFLINE_MESSAGE_END 到达 , 所有离线消息均接收完毕 , 全局变量置为TRUE!");
			} else if (JNIService.JNI_BROADCAST_GROUPS_LOADED.equals(intent
					.getAction())) {
				// Update group loaded state
				GlobalHolder.getInstance().setGroupLoaded();
				if (mCurrentTabFlag == V2GlobalConstants.GROUP_TYPE_USER) {
					// 检测是否有等待验证的好友
					checkWaittingFriendExist();
					// 检测群组是否存在
					checkGroupIsExist();
					// 当所有组织信息和组织内用户信息获取完毕后，检测当前验证消息显示的是否是组织外用户的群验证消息。
					checkEmptyVerificationMessage();
					// 检测讨论组
					// checkNewGroup();
					// 检测是否有重复验证会话
					checkRepeatVerification();
				} else if (mCurrentTabFlag == V2GlobalConstants.GROUP_TYPE_CROWD
						|| mCurrentTabFlag == V2GlobalConstants.GROUP_TYPE_CONFERENCE) {
					// 刷新群组列表
					for (int i = 0; i < covCacheList.size(); i++) {
						CovCache covCache = covCacheList.get(i);
						V2Log.e(TAG, "The Group Need Fresh over! type is : "
								+ covCache.groupType + " id is : "
								+ covCache.groupId);
						initUpdateCovGroupList(covCache.groupType,
								covCache.groupId);
					}
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
				GroupUserObject obj = intent.getParcelableExtra("group");
				if (obj == null) {
					V2Log.e(TAG,
							"Received the broadcast to quit the crowd group , but crowd id is wroing... ");
					return;
				}

				if (V2GlobalConstants.GROUP_TYPE_CROWD == obj.getmType()
						&& mCurrentSubTab == SUB_TAB_CROWD) {
					Group crowd = GlobalHolder.getInstance().getGroupById(
							GroupType.CHATING.intValue(), obj.getmGroupId());
					if (crowd != null)
						addConversation(crowd, false);
					else
						V2Log.e("Can not get crowd group :" + obj.getmGroupId());
				} else if (V2GlobalConstants.GROUP_TYPE_DISCUSSION == obj
						.getmType() && mCurrentSubTab == SUB_TAB_DISCUSSION) {
					Group discussion = GlobalHolder.getInstance().getGroupById(
							GroupType.DISCUSSION.intValue(), obj.getmGroupId());
					if (discussion != null)
						addConversation(discussion, false);
					else
						V2Log.e("Can not get discussion group :"
								+ obj.getmGroupId());
				}
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

				if ((V2GlobalConstants.GROUP_TYPE_DISCUSSION == obj.getmType() && mCurrentSubTab == SUB_TAB_DISCUSSION)
						|| (V2GlobalConstants.GROUP_TYPE_CROWD == obj
								.getmType() && mCurrentSubTab == SUB_TAB_CROWD)) {
					removeConversation(obj.getmGroupId(), true);
				}
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
			} else if (JNIService.JNI_BROADCAST_NEW_DISCUSSION_NOTIFICATION
					.equals(intent.getAction())) {
				if (mCurrentSubTab == SUB_TAB_DISCUSSION) {
					long gid = intent.getLongExtra("gid", 0);
					Group g = GlobalHolder.getInstance().getGroupById(gid);
					if (g != null)
						addConversation(g, false);
					else
						V2Log.e("Can not get discussion :" + gid);
				}
			} else if (PublicIntent.BROADCAST_DISCUSSION_QUIT_NOTIFICATION
					.equals(intent.getAction())) {
				long cid = intent.getLongExtra("groupId", -1l);
				if (mCurrentSubTab == SUB_TAB_DISCUSSION)
					removeConversation(cid, true);
				// Message.obtain(mHandler, QUIT_DISCUSSION_BOARD_DONE,
				// cid).sendToTarget();
			} else if (PublicIntent.BROADCAST_USER_COMMENT_NAME_NOTIFICATION
					.equals(intent.getAction())) {
				Long uid = intent.getLongExtra("modifiedUser", -1);
				if (uid == -1l) {
					V2Log.e("ConversationsTabFragment BROADCAST_USER_COMMENT_NAME_NOTIFICATION ---> update user comment name failed , get id is -1");
					return;
				}

				if (mCurrentSubTab == SUB_TAB_CROWD) {
					for (int i = 0; i < mItemList.size(); i++) {
						Conversation con = mItemList.get(i).cov;
						if (con instanceof CrowdConversation) {
							CrowdConversation crowd = (CrowdConversation) mItemList
									.get(i).cov;
							if (crowd.getGroup().getOwnerUser() != null
									&& crowd.getGroup().getOwnerUser()
											.getmUserId() == uid) {
								((GroupLayout) mItemList.get(i).gp)
										.updateGroupContent(crowd.getGroup());
							}
						}
					}
				}
				adapter.notifyDataSetChanged();
			} else if (JNIService.JNI_BROADCAST_GROUP_USER_ADDED.equals(intent
					.getAction())) {
				GroupUserObject obj = (GroupUserObject) intent.getExtras().get(
						"obj");
				if (obj == null) {
					V2Log.e(TAG,
							"JNI_BROADCAST_GROUP_USER_ADDED --> Received the broadcast to quit the crowd group , but crowd id is wroing... ");
					return;
				}

				if (mCurrentSubTab == SUB_TAB_DISCUSSION) {
					for (int i = 0; i < mItemList.size(); i++) {
						Conversation cov = mItemList.get(i).cov;
						if (cov.getExtId() == obj.getmGroupId()) {
							GroupLayout layout = (GroupLayout) mItemList.get(i).gp;
							layout.update();
							adapter.notifyDataSetChanged();
						}
					}
				}
			} else if (JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION
					.equals(intent.getAction())) {
				if (GlobalHolder.getInstance().getGlobalState().isGroupLoaded()) {
					int groupType = intent.getIntExtra("gtype", -1);
					if (groupType == V2GlobalConstants.GROUP_TYPE_DISCUSSION
							&& mCurrentSubTab == SUB_TAB_DISCUSSION) {
						long gid = intent.getLongExtra("gid", -1);
						for (int i = 0; i < mItemList.size(); i++) {
							Conversation cov = mItemList.get(i).cov;
							if (cov.getExtId() == gid) {
								GroupLayout layout = (GroupLayout) mItemList
										.get(i).gp;
								layout.update();
								adapter.notifyDataSetChanged();
							}
						}
					}
				}
			} else if (JNIService.JNI_BROADCAST_GROUP_USER_REMOVED
					.equals(intent.getAction())) {
				GroupUserObject obj = (GroupUserObject) intent.getExtras().get(
						"obj");
				if (obj == null) {
					V2Log.e(TAG,
							"JNI_BROADCAST_GROUP_USER_REMOVED --> Update Conversation failed that the user removed ... given GroupUserObject is null");
					return;
				}

				if (mCurrentSubTab == SUB_TAB_DISCUSSION) {
					for (int i = 0; i < mItemList.size(); i++) {
						DiscussionConversation disCon = (DiscussionConversation) mItemList
								.get(i).cov;
						GroupLayout layout = (GroupLayout) mItemList.get(i).gp;
						if (obj.getmGroupId() == disCon.getExtId()) {
							DiscussionGroup dis = (DiscussionGroup) GlobalHolder
									.getInstance()
									.getGroupById(
											V2GlobalConstants.GROUP_TYPE_DISCUSSION,
											obj.getmGroupId());
							disCon.setDiscussionGroup(dis);
							layout.update();
						}
					}
				}
			}
		}
	}

	class ConferenceReceiver extends CommonReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			super.onReceive(context, intent);
			if (JNIService.JNI_BROADCAST_CONFERENCE_INVATITION.equals(intent
					.getAction())) {
				long gid = intent.getLongExtra("gid", 0);
				int existPos = -1;
				Group g = GlobalHolder.getInstance().getGroupById(
						GroupType.CONFERENCE.intValue(), gid);
				for (int i = 0; i < mItemList.size(); i++) {
					ConferenceConversation cov = (ConferenceConversation) mItemList
							.get(i).cov;
					if (cov.getGroup().getmGId() == gid) {
						existPos = i;
						break;
					}
				}

				if (!GlobalHolder.getInstance().isOfflineLoaded()) {
					V2Log.e("test", "接收到离线会议，需要显示红点！");
					offLineConf.add(gid);
				}

				if (existPos != -1) {
					ScrollItem exist = mItemList.get(existPos);
					exist.cov.setReadFlag(Conversation.READ_FLAG_UNREAD);
					updateUnreadConversation(exist);
					sendVoiceNotify();
				} else {
					addConversation(g, true);
					Conference c = new Conference((ConferenceGroup) g);
					// Notify status bar
					updateConferenceNotification(c);
				}
			} else if (JNIService.JNI_BROADCAST_CONFERENCE_REMOVED
					.equals(intent.getAction())) {
				long confId = intent.getLongExtra("gid", 0);
				// Remove conference conversation from list
				removeConversation(confId, false);
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
			} else if (PublicIntent.BROADCAST_USER_COMMENT_NAME_NOTIFICATION
					.equals(intent.getAction())) {
				Long uid = intent.getLongExtra("modifiedUser", -1);
				if (uid == -1l) {
					V2Log.e("ConversationsTabFragment BROADCAST_USER_COMMENT_NAME_NOTIFICATION ---> update user comment name failed , get id is -1");
					return;
				}

				for (int i = 0; i < mItemList.size(); i++) {
					ConferenceConversation con = (ConferenceConversation) mItemList
							.get(i).cov;
					if (con.getGroup().getOwnerUser() != null
							&& con.getGroup().getOwnerUser().getmUserId() == uid) {
						((GroupLayout) mItemList.get(i).gp)
								.updateGroupContent(con.getGroup());
					}
				}
			} else if (JNIService.JNI_BROADCAST_USER_UPDATE_BASE_INFO
					.equals(intent.getAction())) {
				long uid = intent.getLongExtra("uid", -1);
				if (uid == -1)
					return;

				for (int i = 0; i < mItemList.size(); i++) {
					ConferenceConversation con = (ConferenceConversation) mItemList
							.get(i).cov;
					User owner = con.getGroup().getOwnerUser();
					if (owner != null && owner.getmUserId() == uid) {
						User newUser = GlobalHolder.getInstance().getUser(uid);
						con.getGroup().setOwnerUser(newUser);
						GroupLayout layout = (GroupLayout) mItemList.get(i).gp;
						layout.update();

						if (offLineConf.contains(con.getGroup().getmGId()))
							layout.updateConversationNotificator(true);
						break;
					}
				}
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

				} else {
					int groupType = intent.getIntExtra("groupType", -1);
					long groupID = intent.getLongExtra("groupID", -1);
					if (groupType == V2GlobalConstants.GROUP_TYPE_DISCUSSION) {
						offlineDissCov.add(intent
								.getLongExtra("groupID", -1));
						updateGroupConversation(groupType, groupID);
					}
				}
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
					V2User v2User = intent.getParcelableExtra("v2User");
					updateFriendVerificationConversation(v2User.getName(), node);
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

				// delete Empty message conversation
				if (uao.isDeleteConversation()) {
					removeConversation(uao.getExtId(), false);
					return;
				}

				Message.obtain(mHandler, UPDATE_CONVERSATION, uao)
						.sendToTarget();
			} else if (ConversationP2PAVActivity.P2P_BROADCAST_MEDIA_UPDATE
					.equals(intent.getAction())) {
				if (Conversation.TYPE_CONTACT == mCurrentTabFlag) {
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

					updateVoiceSpecificItemState(false, newestMediaMessage);
					updateUnreadConversation(voiceItem);
					mItemList.remove(voiceItem);
					mItemList.add(0, voiceItem);
					adapter.notifyDataSetChanged();
				}
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
					verificationItem.cov.setReadFlag(Conversation.READ_FLAG_UNREAD);
				} else {
					verificationItem.cov.setReadFlag(Conversation.READ_FLAG_READ);
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
				if (mCurrentTabFlag == Conversation.TYPE_CONFERNECE) {
					List<Group> gl = GlobalHolder.getInstance().getGroup(
							Group.GroupType.CONFERENCE.intValue());
					if (gl != null && gl.size() > 0 && !isLoadedCov)
						populateConversation(GroupType.CONFERENCE, gl);
				} else if (mCurrentTabFlag == Conversation.TYPE_GROUP) {
					if (crowdDiscussion.getCheckedRadioButtonId() == R.id.rb_crowd) {
						List<Group> chatGroup = GlobalHolder.getInstance()
								.getGroup(Group.GroupType.CHATING.intValue());
						if (chatGroup.size() > 0 && !isLoadedCov)
							populateConversation(GroupType.CHATING, chatGroup);
					} else {
						List<Group> discussionGroup = GlobalHolder
								.getInstance().getGroup(
										Group.GroupType.DISCUSSION.intValue());
						if (discussionGroup.size() > 0 && !isLoadedCov)
							populateConversation(GroupType.DISCUSSION,
									discussionGroup);
					}
				}
				if (mCurrentTabFlag == Conversation.TYPE_CONTACT) {
					if (!isLoadedCov) {
						loadUserConversation();
					}
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
			case UPDATE_SEARCHED_LIST:
				break;
			case REMOVE_CONVERSATION:
				long extId = (Long) msg.obj;
				removeConversation(extId, true);
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
				JNIResponse response = (JNIResponse) msg.obj;
				if (response.getResult() == JNIResponse.Result.SUCCESS) {
					RequestEnterConfResponse recr = (RequestEnterConfResponse) msg.obj;
					Conference c = recr.getConf();
					startConferenceActivity(c);
				} else {
					V2Log.d(TAG, "Request enter conf response , code is : "
							+ response.getResult().name());
					int errResId = R.string.error_request_enter_conference_time_out;
					if (response.getResult() == RequestEnterConfResponse.Result.ERR_CONF_LOCKDOG_NORESOURCE) {
						errResId = R.string.error_request_enter_conference_no_resource;
					} else if (response.getResult() == RequestEnterConfResponse.Result.ERR_CONF_NO_EXIST) {
						errResId = R.string.error_request_enter_conference_not_exist;
					} else if (response.getResult() == RequestEnterConfResponse.Result.TIME_OUT) {
						errResId = R.string.error_request_enter_conference_time_out;
					} else {
						errResId = R.string.error_request_enter_conference_time_out;
					}

					Toast.makeText(mContext, errResId, Toast.LENGTH_SHORT)
							.show();
					mContext.stopService(new Intent(mContext,
							ConferencMessageSyncService.class));
				}
				break;
			case UPDATE_CONVERSATION_MESSAGE:
				GroupType type = GroupType.fromInt(msg.arg1);
				ConversationList conversations = (ConversationList) msg.obj;
				mItemList.clear();
				fillAdapter(type, conversations.conversationList);
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

	private void startConversationView(Conversation cov) {
		if (cov == null)
			return;
		Intent i = new Intent(PublicIntent.START_CONVERSACTION_ACTIVITY);
		i.addCategory(PublicIntent.DEFAULT_CATEGORY);
		i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		i.putExtra(
				"obj",
				new ConversationNotificationObject(cov.getType(), cov
						.getExtId()));
		startActivityForResult(i, REQUEST_UPDATE_CHAT_CONVERSATION);
	}

	@Override
	public void updateConversationToCreate(int groupType, long groupID,
			long remoteUserID) {
		if (mCurrentTabFlag != V2GlobalConstants.GROUP_TYPE_USER) {
			return;
		}

		for (int i = 0; i < mItemList.size(); i++) {
			Conversation cov = mItemList.get(i).cov;
			if (cov.getType() == V2GlobalConstants.GROUP_TYPE_USER) {
				if (cov.getExtId() == remoteUserID) {
					V2Log.w(TAG, "该会话已经存在 ，不需要重复创建！remoteUser id : "
							+ remoteUserID);
					return;
				}
			} else {
				if (cov.getExtId() == groupID) {
					V2Log.w(TAG, "该会话已经存在 ，不需要重复创建！ group id : " + groupID);
					return;
				}
			}
		}

		ScrollItem newItem = null;
		if (V2GlobalConstants.GROUP_TYPE_USER == groupType) {
			VMessage vm = MessageLoader.getNewestMessage(mContext, GlobalHolder
					.getInstance().getCurrentUserId(), remoteUserID);
			ContactConversation contact = new ContactConversation(GlobalHolder
					.getInstance().getUser(remoteUserID));
			contact.setMsg(MessageUtil
					.getMixedConversationContent(mContext, vm));
			contact.setDateLong(String.valueOf(vm.getmDateLong()));
			ConversationProvider.saveConversation(vm);
			// 添加到ListView中
			GroupLayout viewLayout = new GroupLayout(mContext, contact);
			newItem = new ScrollItem(contact, viewLayout);
		} else {
			newItem = makeNewGroupItem(groupType, groupID);
			if (newItem == null) {
				V2Log.e(TAG,
						"updateConversationToCreate --> make new group item failed!");
				return;
			}
		}
		mItemList.add(0, newItem);
		adapter.notifyDataSetChanged();
	}
}
