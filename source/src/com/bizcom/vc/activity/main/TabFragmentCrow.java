package com.bizcom.vc.activity.main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
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
import com.V2.jni.util.V2Log;
import com.bizcom.bo.ConversationNotificationObject;
import com.bizcom.bo.GroupUserObject;
import com.bizcom.db.provider.VerificationProvider;
import com.bizcom.request.ConferencMessageSyncService;
import com.bizcom.request.ConferenceService;
import com.bizcom.request.CrowdGroupService;
import com.bizcom.request.MessageListener;
import com.bizcom.request.jni.JNIResponse;
import com.bizcom.request.jni.RequestEnterConfResponse;
import com.bizcom.util.DateUtil;
import com.bizcom.util.MessageUtil;
import com.bizcom.util.Notificator;
import com.bizcom.util.SearchUtils;
import com.bizcom.util.SearchUtils.ScrollItem;
import com.bizcom.vc.activity.conference.ConferenceActivity;
import com.bizcom.vc.activity.conference.GroupLayout;
import com.bizcom.vc.activity.contacts.AddFriendHistroysHandler;
import com.bizcom.vc.activity.conversation.MessageLoader;
import com.bizcom.vc.application.GlobalConfig;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.application.PublicIntent;
import com.bizcom.vc.application.V2GlobalConstants;
import com.bizcom.vc.listener.ConferenceListener;
import com.bizcom.vc.service.JNIService;
import com.bizcom.vo.Conference;
import com.bizcom.vo.ConferenceConversation;
import com.bizcom.vo.ContactConversation;
import com.bizcom.vo.Conversation;
import com.bizcom.vo.ConversationFirendAuthenticationData.VerificationMessageType;
import com.bizcom.vo.CrowdConversation;
import com.bizcom.vo.CrowdGroup;
import com.bizcom.vo.DepartmentConversation;
import com.bizcom.vo.DiscussionConversation;
import com.bizcom.vo.DiscussionGroup;
import com.bizcom.vo.Group;
import com.bizcom.vo.Group.GroupType;
import com.bizcom.vo.User;
import com.bizcom.vo.VMessage;
import com.bizcom.vo.VMessageQualification.ReadState;
import com.v2tech.R;

public class TabFragmentCrow extends Fragment implements TextWatcher,
		ConferenceListener {
	private static final String TAG = "ConversationsTabFragment";
	private static final int FILL_CONFS_LIST = 2;
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

	private BroadcastReceiver receiver;
	private IntentFilter intentFilter;

	private ConferenceService cb;
	private CrowdGroupService chatService;

	private List<ScrollItem> mItemList = new ArrayList<ScrollItem>();
	private List<ScrollItem> searchList = new ArrayList<ScrollItem>();
	private List<CovCache> covCacheList = new ArrayList<TabFragmentCrow.CovCache>();

	private LocalHandler mHandler = new LocalHandler();

	/**
	 * This tag is used to limit the database load times
	 */
	private boolean isLoadedCov;

	private boolean mIsStartedSearch;
	private boolean isUpdateGroup;
	private boolean isUpdateDeparment;

	private ListView mConversationsListView;
	private ConversationsAdapter adapter = new ConversationsAdapter();

	private int mCurrentTabFlag;

	/**
	 * This tag is used to save current click the location of item.
	 */
	private Conversation currentClickConversation;

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
		V2Log.d(TAG, "TabFragmentCrow onCreate...");

		mContext = getActivity();
		service = Executors.newCachedThreadPool();
		res = getResources();
		initReceiver();

		chatService = new CrowdGroupService();
		Message.obtain(mHandler, FILL_CONFS_LIST).sendToTarget();
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
										.setTextColor(getResources().getColor(
												R.color.button_text_color_blue));
								populateConversation(
										GroupType.DISCUSSION,
										GlobalHolder.getInstance()
												.getGroup(
														GroupType.DISCUSSION
																.intValue()));
							} else if (checkedId == R.id.rb_crowd) {
								mCurrentSubTab = SUB_TAB_CROWD;
								populateConversation(
										GroupType.CHATING,
										GlobalHolder.getInstance().getGroup(
												GroupType.CHATING.intValue()));
								((RadioButton) group
										.findViewById(R.id.rb_discussion))
										.setTextColor(getResources().getColor(
												R.color.button_text_color_blue));
								((RadioButton) group
										.findViewById(R.id.rb_crowd))
										.setTextColor(Color.WHITE);
							}
						}
					});

			mConversationsListView.setOnItemClickListener(mItemClickListener);
			mConversationsListView
					.setOnItemLongClickListener(mItemLongClickListener);
		}
		return rootView;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getActivity().unregisterReceiver(receiver);
		mItemList = null;
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
		receiver = new GroupReceiver();
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
					.addAction(PublicIntent.BROADCAST_NEW_CROWD_NOTIFICATION);
		}
		getActivity().registerReceiver(receiver, intentFilter);
	}

	@Override
	public void onStart() {
		super.onStart();
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

	private void sortAndUpdate() {
		Collections.sort(mItemList);
		adapter.notifyDataSetChanged();
	}

	/**
	 * According populateType to fill the List Data. The data from server!
	 * 
	 * @param populateType
	 * @param list
	 */
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

			Conversation cov;
			if (g.getGroupType() == GroupType.CHATING) {
				cov = new CrowdConversation(g);
			} else if (g.getGroupType() == GroupType.DISCUSSION) {
				cov = new DiscussionConversation(g);
			} else {
				continue;
			}

			// Update all initial conversation to read
			long time = GlobalConfig.getGlobalServerTime();
			cov.setDate(DateUtil.getStandardDate(new Date(time)));
			tempList.add(cov);
		}
		sendMessageToFillAdapter(populateType, tempList);
	}

	/**
	 * Add a new conversation to current list.
	 * 
	 * @param g
	 * @param readState
	 *            Indicate that whether the new conversaion should display the
	 *            prompt!
	 */
	private void addConversation(Group g, boolean readState) {

		if (g == null || g.getGroupType() != GroupType.CHATING
				|| g.getGroupType() != GroupType.DISCUSSION) {
			V2Log.e(TAG,
					"addConversation --> Add new conversation failed ! Given Group is null");
			return;
		}

		Conversation cov;
		ScrollItem currentItem = null;
		if (g.getGroupType() == GroupType.CHATING) {
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
			gp.updateGroupContent(g);
			ScrollItem newItem = new ScrollItem(cov, gp);
			currentItem = newItem;
			mItemList.add(0, newItem);
		} else {
			V2Log.d(TAG,
					"addConversation -- The Group Conversation already exist, type is : "
							+ cov.getType() + " and id is : " + cov.getExtId()
							+ " and name is : " + cov.getName());
		}

		if (readState) {
			currentItem.cov.setReadFlag(Conversation.READ_FLAG_UNREAD);
		} else {
			currentItem.cov.setReadFlag(Conversation.READ_FLAG_READ);
		}
		adapter.notifyDataSetChanged();
	}

	private void sendMessageToFillAdapter(GroupType type,
			List<Conversation> conversations) {
		Message message = Message.obtain(mHandler, UPDATE_CONVERSATION_MESSAGE,
				new ConversationList(conversations));
		message.arg1 = type.intValue();
		message.sendToTarget();
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
			// 需要调用updateGroupContent
			Group fillGroup = null;
			if (cov.getType() == V2GlobalConstants.GROUP_TYPE_CROWD) {
				fillGroup = ((CrowdConversation) cov).getGroup();
			}

			if (fillGroup != null)
				layout.updateGroupContent(fillGroup);

			ScrollItem newItem = new ScrollItem(cov, layout);
			mItemList.add(newItem);
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

	public void updateSearchState() {

		mIsStartedSearch = false;
		searchList.clear();
		adapter.notifyDataSetChanged();
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
		for (int i = 0; i < mItemList.size(); i++) {
			Conversation temp = mItemList.get(i).cov;
			if (temp.getExtId() == conversationID) {
				// remove item
				ScrollItem removed = mItemList.remove(i);
				if (removed == null)
					V2Log.e(TAG, "Delete Conversation Failed...id is : "
							+ conversationID);
				// clear all system notification
				Notificator.cancelSystemNotification(getActivity(),
						PublicIntent.MESSAGE_NOTIFICATION_ID);
				break;
			}
		}
		sortAndUpdate();
	}

	private boolean isOutOrgShow;

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

		boolean flag = VerificationProvider.getUNReandMessage(false);
		intent.putExtra("isCrowdShowNotification", flag);
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
									Log.i("20150203 1", "8");
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
				if (isConference) {
					currentGroupLayout.updateConversationNotificator(true);
				} else {
					currentGroupLayout.update();
				}
				currentGroupLayout.updateGroupContent(newGroup);
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
			startConversationView(cov);
		}
	};

	private OnItemLongClickListener mItemLongClickListener = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> adapter, View v, int pos,
				long id) {
			showPopupWindow(v);
			return true;
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
				if (mCurrentTabFlag == V2GlobalConstants.GROUP_TYPE_USER) {
					Conversation cov = scrollItem.cov;
					if (cov.getType() == Conversation.TYPE_CONTACT) {
						Bitmap bp = ((ContactConversation) cov).getU()
								.getAvatarBitmap();
						GroupLayout gl = (GroupLayout) scrollItem.gp;
						gl.updateIcon(bp);
					}
				}
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
				if (groupType != V2GlobalConstants.GROUP_TYPE_CROWD
						|| groupType != V2GlobalConstants.GROUP_TYPE_DISCUSSION) {
					V2Log.w(TAG,
							"the GROUP_USER_UPDATEED Comming over , type is : "
									+ groupType + " id is : " + groupID);
					if (isLoadedCov) {
						initUpdateCovGroupList(groupType, groupID);
					} else {
						covCacheList.add(new CovCache(groupType, groupID));
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
				if (mCurrentTabFlag == V2GlobalConstants.GROUP_TYPE_CROWD
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

	class LocalHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case FILL_CONFS_LIST:
				if (crowdDiscussion.getCheckedRadioButtonId() == R.id.rb_crowd) {
					List<Group> chatGroup = GlobalHolder.getInstance()
							.getGroup(Group.GroupType.CHATING.intValue());
					if (chatGroup.size() > 0 && !isLoadedCov)
						populateConversation(GroupType.CHATING, chatGroup);
				} else {
					List<Group> discussionGroup = GlobalHolder.getInstance()
							.getGroup(Group.GroupType.DISCUSSION.intValue());
					if (discussionGroup.size() > 0 && !isLoadedCov)
						populateConversation(GroupType.DISCUSSION,
								discussionGroup);
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
	public boolean requestJoinConf(Conference conf) {
		// TODO Auto-generated method stub
		return false;
	}
}
