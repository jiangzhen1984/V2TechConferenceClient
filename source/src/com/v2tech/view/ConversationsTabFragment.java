package com.v2tech.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.V2.jni.V2GlobalEnum;
import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.db.ContentDescriptor;
import com.v2tech.db.ConversationProvider;
import com.v2tech.db.DataBaseContext;
import com.v2tech.db.V2techSearchContentProvider;
import com.v2tech.service.BitmapManager;
import com.v2tech.service.ConferencMessageSyncService;
import com.v2tech.service.ConferenceService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.Registrant;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.service.jni.RequestEnterConfResponse;
import com.v2tech.util.DateUtil;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.MessageUtil;
import com.v2tech.util.Notificator;
import com.v2tech.view.bo.ConversationNotificationObject;
import com.v2tech.view.bo.GroupUserObject;
import com.v2tech.view.conference.GroupLayout;
import com.v2tech.view.conference.VideoActivityV2;
import com.v2tech.view.contacts.VoiceMessageActivity;
import com.v2tech.view.contacts.add.AddFriendHistroysHandler;
import com.v2tech.view.conversation.MessageAuthenticationActivity;
import com.v2tech.view.conversation.MessageBuilder;
import com.v2tech.view.conversation.MessageLoader;
import com.v2tech.view.conversation.P2PConversation;
import com.v2tech.view.group.CrowdDetailActivity;
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
import com.v2tech.vo.VMessageAbstractItem;
import com.v2tech.vo.VMessageQualification;
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

	private static final int VERIFICATION_TYPE_FRIEND = 5;
	private static final int VERIFICATION_TYPE_CROWD = 6;

	private NotificationListener notificationListener;

	private BroadcastReceiver receiver;
	private IntentFilter intentFilter;
	private boolean mIsStartedSearch;

	private List<Conversation> mConvList;
	private Set<Conversation> mUnreadConvList;

	private LocalHandler mHandler = new LocalHandler();

	private boolean isLoadedCov = false;

	private View rootView;

	private List<ScrollItem> mItemList = new ArrayList<ScrollItem>();
	private List<Conversation> mCacheItemList;

	private Context mContext;

	private ListView mConversationsListView;

	private ConversationsAdapter adapter = new ConversationsAdapter();

	private ConferenceService cb;

	private int mCurrentTabFlag;

	private int currentPosition;

	private boolean showAuthenticationNotification;

	private ScrollItem verificationItem;
	private ScrollItem voiceItem;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i("wzl", "ConversationsTabFragment onCreate");
		String tag = this.getArguments().getString("tag");
		if (PublicIntent.TAG_CONF.equals(tag)) {
			mCurrentTabFlag = Conversation.TYPE_CONFERNECE;
			cb = new ConferenceService();
		} else if (PublicIntent.TAG_COV.equals(tag)) {
			mCurrentTabFlag = Conversation.TYPE_CONTACT;
		} else if (PublicIntent.TAG_GROUP.equals(tag)) {
			mCurrentTabFlag = Conversation.TYPE_GROUP;
		}
		mContext = getActivity();

		initReceiver(mCurrentTabFlag);

		notificationListener = (NotificationListener) getActivity();
		BitmapManager.getInstance().registerBitmapChangedListener(
				this.bitmapChangedListener);

		Message.obtain(mHandler, FILL_CONFS_LIST).sendToTarget();

		searchList = new ArrayList<ScrollItem>();
		searchCacheList = new ArrayList<ScrollItem>();
		firstSearchCacheList = new ArrayList<ScrollItem>();
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
		mCacheItemList = null;
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
	}

	/**
	 * According to tag, initialize different intent filter
	 * 
	 * @param tag
	 */
	private void initReceiver(int tag) {
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
			intentFilter.addAction(P2PConversation.P2P_BROADCAST_MEDIA_UPDATE);
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
	}

	@Override
	public void onStop() {
		firstSearchCacheList.clear();
		searchList.clear();
		mIsStartedSearch = false;
		startIndex = 0;
		// adapter.notifyDataSetChanged();
		super.onStop();
	}

	private List<ScrollItem> searchList = null;
	private List<ScrollItem> searchCacheList = null;
	private List<ScrollItem> firstSearchCacheList = null;
	private int lastSize;
	private boolean isShouldAdd;
	private boolean isShouldQP; // 是否需要启动全拼
	private int startIndex = 0;
	private boolean isBreak; // 用于跳出getSearchList函数中的二级循环

	@Override
	public void afterTextChanged(Editable s) {
		if (s != null && s.length() > 0) {

			if (!mIsStartedSearch) {
				searchList.clear();
				lastSize = 0;
				startIndex = 0;
				mIsStartedSearch = true;
				searchList.addAll(mItemList);
			}

			int length = s.length();
			if (length < lastSize) {
				searchList.clear();
				searchList.addAll(mItemList);
				startIndex = s.length() - 1;
			}
			lastSize = length;
			StringBuilder sb = new StringBuilder();
			V2Log.e(TAG, "Editable :" + s.toString());

			char[] charSimpleArray = s.toString()
					.toLowerCase(Locale.getDefault()).toCharArray();
			if (charSimpleArray.length < 5) { // 搜字母查询
				for (int i = startIndex; i < charSimpleArray.length; i++) {
					if (isChineseWord(charSimpleArray[i])) {
						V2Log.e(TAG, charSimpleArray[i] + " is Chinese");
						searchCacheList = getSearchList(searchList,
								String.valueOf(charSimpleArray[i]), i, true,
								true);
					} else {
						V2Log.e(TAG, charSimpleArray[i] + " not Chinese");
						searchCacheList = getSearchList(searchList,
								String.valueOf(charSimpleArray[i]), i, true,
								false);
					}

					if (i == 0 && s.toString().length() == 1
							&& firstSearchCacheList.size() == 0) {
						firstSearchCacheList.addAll(searchCacheList);
					}

					searchList.clear();
					if (searchCacheList.size() > 0) {
						isShouldQP = false;
						searchList.addAll(searchCacheList);
						V2Log.e(TAG, "简拼找到结果 展示");
					} else {
						isShouldQP = true;
						searchCacheList.addAll(firstSearchCacheList);
						V2Log.e(TAG, "简拼没有结果 开启全拼");
					}
				}
			} else {
				isShouldQP = true;
				searchCacheList.addAll(firstSearchCacheList);
				V2Log.e(TAG, "简拼没有结果 开启全拼");
			}

			// if(s.toString().length() >= 5 && searchCacheList.size() > 0){
			// //如果长度大于5则不按首字母查询
			if (isShouldQP) { // 如果长度大于5则不按首字母查询
				searchList.clear();
				V2Log.e(TAG, "searchCacheList size :" + searchCacheList.size());
				for (int i = 0; i < searchCacheList.size(); i++) {
					V2Log.e(TAG,
							"searchList : "
									+ searchCacheList.get(i).cov.getName()
									+ "--StringBuilder : " + sb.toString());
					// 获取名字，将名字变成拼音串起来
					char[] charArray = searchCacheList.get(i).cov.getName()
							.toCharArray();
					for (char c : charArray) {
						String charStr = GlobalConfig.allChinese.get(String
								.valueOf(c));
						// V2techSearchContentProvider
						// .queryChineseToEnglish(mContext, "HZ = ?",
						// new String[] { String.valueOf(c) });
						sb.append(charStr);
					}
					V2Log.e(TAG, "StringBuilder : " + sb.toString());
					String material = sb.toString();
					// 判断该昵称第一个字母，与输入的第一字母是否匹配
					Character first = material.toCharArray()[0];
					char[] targetChars = s.toString().toCharArray();
					if (!first.equals(targetChars[0])) {
						isShouldAdd = true;
					} else {
						for (char c : targetChars) {
							if (!material.contains(String.valueOf(c))
									&& first.equals(c)) {
								isShouldAdd = true;
								V2Log.e(TAG, "material not contains " + c);
								break;
							}
							isShouldAdd = false;
						}
					}

					if (!isShouldAdd) {
						V2Log.e(TAG, "added ---------"
								+ searchCacheList.get(i).cov.getName());
						searchList.add(searchCacheList.get(i));
					}
					sb.delete(0, sb.length());
				}
			}

			V2Log.e(TAG, "get searchList size :" + searchList.size());
			adapter.notifyDataSetChanged();
			startIndex++;
		} else {
			V2techSearchContentProvider.closedDataBase();
			if (mIsStartedSearch) {
				firstSearchCacheList.clear();
				searchList.clear();
				mIsStartedSearch = false;
				startIndex = 0;
				adapter.notifyDataSetChanged();
			}
		}

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

	/**
	 * 判断给定的字符是否为汉字
	 * 
	 * @param mChar
	 * @return
	 */
	public boolean isChineseWord(char mChar) {
		Pattern pattern = Pattern.compile("[\\u4E00-\\u9FA5]"); // 判断是否为汉字
		Matcher matcher = pattern.matcher(String.valueOf(mChar));
		return matcher.find();
	}

	/**
	 * 根据 searchKey 获得搜索后的集合
	 * 
	 * @param list
	 * @param searchKey
	 * @param index
	 * @param isFirstSearch
	 *            判断是否是首字母搜索
	 * @param isChinese
	 *            判断searchKey是否为中文
	 * @return
	 */
	public List<ScrollItem> getSearchList(List<ScrollItem> list,
			String searchKey, int index, boolean isFirstSearch,
			boolean isChinese) {
		V2Log.e(TAG, "searchList :" + list.size() + "--searchKey :" + searchKey
				+ "--index :" + index);
		List<ScrollItem> tempList = new ArrayList<ScrollItem>();
		if (searchKey == null || searchKey.length() < 0) {
			return tempList;
		}

		String searchTarget;
		for (int i = 0; i < list.size(); i++) { // 一级循环，循环所有消息
			ScrollItem scrollItem = list.get(i);
			Conversation cov = list.get(i).cov;
			// 判断是否能获取到消息item的名字
			if (cov.getName() != null) {
				// 将名字分割为字符数组遍历
				char[] charArray = cov.getName().toCharArray();
				for (int j = 0; j < charArray.length; j++) { // 二级循环，循环消息名称
					searchTarget = String.valueOf(charArray[j]);
					if (isFirstSearch && isChinese) {
						if (searchKey.contains(searchTarget)) {
							tempList.add(scrollItem);
							break;
						}
					} else if (isFirstSearch && !isChinese) {
						// if (isChineseWord(cov.getName().charAt(index))) {
						if (isChineseWord(charArray[j])) {
							String englishChar = GlobalConfig.allChinese
									.get(searchTarget);
							// V2techSearchContentProvider
							// .queryChineseToEnglish(mContext, "HZ = ?",
							// new String[] { searchTarget });
							V2Log.e(TAG, "englishChar :" + englishChar);
							if (englishChar == null)
								continue;
							// if(englishChar.contains(searchKey)){
							// tempList.add(scrollItem);
							// }
							String[] split = englishChar.split(";");
							for (String string : split) { // 三级循环，循环多音字

								int indexOf = string.indexOf(searchKey);
								if (indexOf == 0) {
									tempList.add(scrollItem);
									isBreak = true;
									break;
								}
							}
							// tempList添加元素后就直接跳出二级循环。
							if (isBreak) {
								isBreak = false;
								break;
							}
							// if(searchTarget.contains(searchKey)){
						} else {
							searchTarget = searchTarget.toLowerCase(Locale
									.getDefault());
							V2Log.e(TAG, "searchTarget :" + searchTarget);
							int indexOf = searchTarget.indexOf(searchKey);
							// if(searchTarget.contains(searchKey)){
							if (indexOf != -1) {
								tempList.add(scrollItem);
								break;
							}
						}
					}
					// else if (!isFirstSearch && isChinese) {
					// String englishChar =
					// GlobalConfig.allChinese.get(searchTarget);
					// if (cov.getName().contains(searchKey) &&
					// first.equals(searchKey)){
					// tempList.add(scrollItem);
					// break;
					// }
					// }
					// else if (!isFirstSearch && !isChinese) {
					//
					// }
				}
				// 判断该消息人的名字，在index位置是否能取到字符
				// if (index >= cov.getName().length()) {
				// continue;
				// } else {
				//
				// searchTarget = String.valueOf(cov.getName().charAt(index));
				// if (searchTarget == null) {
				// continue;
				// }
				// }
			}

			// 暂不要求消息内容
			// else if (cov.getMsg() != null &&
			// cov.getMsg().toString().contains(searchKey)) {
			// newItemList.add(cov);
			// }
		}
		return tempList;
	}

	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);
		// recover search result
		if (!isVisibleToUser && mIsStartedSearch) {
			mConvList = mCacheItemList;
			adapter.notifyDataSetChanged();
			mIsStartedSearch = false;
		}
	}

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
			Group g = list.get(i);
			Conversation cov;
			if (g.getName() == null)
				V2Log.e(TAG,
						"the group name is null , group id is :" + g.getmGId());
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
		for (Conversation conversation : mConvList) {
			if (conversation.getExtId() == cov.getExtId()) {
				isAdd = false;
				break;
			}
		}

		if (isAdd) {
			V2Log.d(TAG, "Successfully add a new conversation , type is : "
					+ cov.getType() + " and id is : " + cov.getExtId()
					+ " and name is : " + cov.getName());
			this.mConvList.add(0, cov);
			GroupLayout gp = new GroupLayout(this.getActivity(), cov);
			if (mCurrentTabFlag == V2GlobalEnum.GROUP_TYPE_CROWD) {
				gp.updateCrowdLayout();
			}
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
				showCrowdAuthentication();
				break;
			default:
				break;
			}
		}
		new Thread(new Runnable() {

			@Override
			public void run() {
				synchronized (ConversationsTabFragment.class) {
					tempList = new ArrayList<Conversation>();
					tempList = ConversationProvider.loadUserConversation(
							mContext, tempList, verificationMessageItemData,
							voiceMessageItem);
					isLoadedCov = true;
				}
				Message.obtain(mHandler, UPDATE_CONVERSATION_MESSAGE)
						.sendToTarget();
			}
		}).start();

	}

	/**
	 * 判断数据库是否有验证消息
	 * 
	 * @return
	 */
	private int isHaveVerificationMessage() {
		int result = -1;
		Cursor cursor = null;
		try {
			VMessageQualification nestQualification = MessageLoader
					.getNewestCrowdVerificationMessage(mContext, GlobalHolder
							.getInstance().getCurrentUser());
			String sql = "select * from " + AddFriendHistroysHandler.tableName
					+ " order by SaveDate desc limit 1";
			cursor = AddFriendHistroysHandler.select(getActivity(), sql,
					new String[] {});
			if ((cursor != null && cursor.moveToNext())
					&& nestQualification != null) {
				long addFriendTime = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_SAVEDATE));
				if (nestQualification.getmTimestamp().getTime() > addFriendTime * 1000)
					return VERIFICATION_TYPE_CROWD;
				else
					return VERIFICATION_TYPE_FRIEND;

			} else {
				if (cursor != null && cursor.getCount() > 0)
					return VERIFICATION_TYPE_FRIEND;

				if (nestQualification != null)
					return VERIFICATION_TYPE_CROWD;
			}
			return result;
		} finally {
			if (cursor != null)
				cursor.close();
		}
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

		adapter.notifyDataSetChanged();
	}

	private Conversation voiceMessageItem;
	private GroupLayout voiceLayout;
	private GroupLayout verificationMessageItemLayout;
	private Conversation verificationMessageItemData;

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
					updateUnreadVoiceConversation(true);
				} else
					voiceLayout.update(null, startDate, false);
			}
		}
	}

	/**
	 * 判断通话中是否有未读的
	 * 
	 * @return
	 */
	public boolean isHasUnreadMediaMessage() {

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
		for (int i = 0; i < this.mConvList.size(); i++) {
			Conversation cov = this.mConvList.get(i);
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
				// }
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
			// default:
			// throw new RuntimeException(
			// "updateGroupConversation ---> invalid mCurrentTabFlag : "
			// + mCurrentTabFlag);
			}

			if (existedCov != null) {
				// 添加到ListView中
				V2Log.d(TAG,
						"updateGroupConversation --> Successfully add a new conversation , type is : "
								+ existedCov.getType() + " and id is : "
								+ existedCov.getExtId() + " and name is : "
								+ existedCov.getName());
				mConvList.add(0, existedCov);
				mItemList.add(0, new ScrollItem(existedCov, viewLayout));
			}
		}

		if (existedCov == null) {
			V2Log.e(TAG,
					"updateGroupConversation ---> The existedConversation that updated is null , update failed , foundFlag : "
							+ foundFlag);
			return;
		}

		if (!crowdNotFresh) {
			if (vm.getFromUser().getmUserId() != GlobalHolder.getInstance()
					.getCurrentUserId()) {
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

	/**
	 * Update conversation according to message id and remote user id, This
	 * request call only from new message broadcast
	 * 
	 * @param msgId
	 * @param remoteUserID
	 */
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
			// if (vm.getMsgCode() != V2GlobalEnum.GROUP_TYPE_CROWD
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

		if ((vm.getFromUser().getmUserId() != GlobalHolder.getInstance()
				.getCurrentUserId())
				&& vm.getState() == VMessageAbstractItem.STATE_UNREAD) {
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

	private void updateUnreadConversation(Conversation cov) {
		// boolean flag;
		int ret;
		if (cov.getReadFlag() == Conversation.READ_FLAG_READ) {
			mUnreadConvList.remove(cov);
			ret = Conversation.READ_FLAG_READ;
		} else {
			mUnreadConvList.add(cov);
			ret = Conversation.READ_FLAG_UNREAD;
		}
		// Update main activity to show or hide notificator
		if (mUnreadConvList.size() > 0) {
			this.notificationListener.updateNotificator(mCurrentTabFlag, true);
		} else {
			this.notificationListener.updateNotificator(mCurrentTabFlag, false);
		}
		// If flag is true, means we updated unread list, then we need to update
		// database
		// update conversation date and flag to database
		ConversationProvider.updateConversationToDatabase(mContext, cov, ret);
	}

	/**
	 * Remove conversation from mConvList by id.
	 * 
	 * @param id
	 */
	protected void removeConversation(long id) {

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
						V2Log.e(TAG, " GROUP_TYPE_USER mCurrentTabFlag :"
								+ mCurrentTabFlag);
					}
				} else if (mCurrentTabFlag == V2GlobalEnum.GROUP_TYPE_CROWD) {
					ConversationProvider.deleteConversation(mContext,
							conversation);
					V2Log.e(TAG, "GROUP_TYPE_CROWD mCurrentTabFlag :"
							+ mCurrentTabFlag);
				}
				cache = mConvList.remove(i);
				break;
			}
		}
		if (cache != null) {
			V2Log.e(TAG, "cache mCurrentTabFlag :" + mCurrentTabFlag);
			adapter.notifyDataSetChanged();
			// Set removed conversation state to readed
			cache.setReadFlag(Conversation.READ_FLAG_READ);
			updateUnreadConversation(cache);
			Notificator.cancelSystemNotification(getActivity(),
					PublicIntent.MESSAGE_NOTIFICATION_ID);
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

	private ProgressDialog mWaitingDialog;

	private void requestJoinConf(long gid) {
		if (currentEntered != null) {
			V2Log.e("Already in meeting " + currentEntered.getId());
			return;
		}
		mWaitingDialog = ProgressDialog.show(
				mContext,
				"",
				mContext.getResources().getString(
						R.string.requesting_enter_conference), true);
		Message.obtain(this.mHandler, REQUEST_ENTER_CONF, gid).sendToTarget();

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
		Notificator.updateSystemNotification(mContext, conf.getName()
				+ " 会议邀请:", creator == null ? "" : creator.getName(), 1,
				enterConference, PublicIntent.VIDEO_NOTIFICATION_ID);

	}

	class ScrollItem implements Comparable<ScrollItem> {
		Conversation cov;
		View gp;

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
					
					VerificationMessageType messageType = ((ConversationFirendAuthenticationData) verificationMessageItemData).getMessageType();
					if(messageType == VerificationMessageType.CONTACT_TYPE)
						intent.putExtra("isFriendActivity", true);
					else
						intent.putExtra("isFriendActivity", false);
					
					// 查出未读的第一条按时间顺序
					String order = ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_SAVEDATE
							+ " desc limit 1";
					Cursor cursor = mContext.getContentResolver().query(ContentDescriptor.HistoriesAddFriends.CONTENT_URI,
							null, "ReadState = ?", new String[]{String.valueOf(0)}, order);
					if ((cursor != null) && (cursor.getCount() == 0)) 
						intent.putExtra("isFriendShowNotification", false);
					else 
						intent.putExtra("isFriendShowNotification", true);
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
			updateUnreadVoiceConversation(false);
		}
	};

	private OnItemLongClickListener mItemLongClickListener = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> adapter, View v, int pos,
				long id) {

			String[] item;
			if (mCurrentTabFlag == Conversation.TYPE_CONFERNECE) {
				item = new String[] { mContext.getResources().getString(
						R.string.conversations_delete_conf) };
			} else if (mCurrentTabFlag == Conversation.TYPE_CONTACT) {
				item = new String[] { mContext.getResources().getString(
						R.string.conversations_delete_conversaion) };
				// Conversation.TYPE_GROUP
			} else {
				item = new String[] {
				// mContext.getResources().getString(
				// R.string.conversations_delete_conversaion),
				mContext.getResources()
						.getString(R.string.conversations_detail) };
			}

			final Conversation cov = mConvList.get(pos);
			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setTitle(cov.getName()).setItems(item,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							if (which == 0) {
								switch (mCurrentTabFlag) {
								case Conversation.TYPE_CONFERNECE:
									Group g = GlobalHolder.getInstance()
											.getGroupById(
													GroupType.CONFERENCE
															.intValue(),
													cov.getExtId());
									// If group is null, means we have removed
									// this conversaion
									if (g != null) {
										cb.quitConference(
												new Conference(cov.getExtId(),
														g.getOwnerUser()
																.getmUserId()),
												null);
									}
								case Conversation.TYPE_CONTACT:
									removeConversation(cov.getExtId());
									break;
								case Conversation.TYPE_GROUP:
									Intent crowdIntent = new Intent();
									crowdIntent.setClass(mContext,
											CrowdDetailActivity.class);
									crowdIntent.putExtra("cid", cov.getExtId());
									mContext.startActivity(crowdIntent);
								}
							}
							// else {
							// Intent crowdIntent = new Intent();
							// crowdIntent.setClass(mContext,
							// CrowdDetailActivity.class);
							// crowdIntent.putExtra("cid", cov.getExtId());
							// mContext.startActivity(crowdIntent);
							// }
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
			if (mIsStartedSearch) {
				return searchList == null ? 0 : searchList.size();
			} else {
				if (mConvList == null && mItemList != null) {
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
			currentPosition = position;
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
			if (intent.getAction().equals(
					JNIService.JNI_BROADCAST_GROUP_NOTIFICATION)) {
				int type = intent.getExtras().getInt("gtype");
				if ((type == GroupType.CONFERENCE.intValue() && mCurrentTabFlag == Conversation.TYPE_CONFERNECE)
						|| mCurrentTabFlag == Conversation.TYPE_GROUP) {
					Message.obtain(mHandler, FILL_CONFS_LIST).sendToTarget();
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
							notificationListener.updateNotificator(
									mCurrentTabFlag, false);
							ConversationProvider.updateConversationToDatabase(
									mContext, scroll.cov,
									Conversation.READ_FLAG_READ);
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
						if (verification.getUser() != null) {
							if (TextUtils.isEmpty(verification.getUser()
									.getName())) {
								User inviUser = GlobalHolder.getInstance()
										.getUser(
												verification.getUser()
														.getmUserId());
								if (inviUser != null)
									verification.setUser(inviUser);
							}

							verification
									.setMsg(verification.getUser().getName()
											+ mContext
													.getText(R.string.crowd_invitation_content));
							verificationMessageItemLayout.update();
							adapter.notifyDataSetChanged();
							V2Log.d(TAG,
									"Successfully updated verification the user infos , user name is :"
											+ verification.getUser().getName());
						}
						break;
					case Conversation.TYPE_CONTACT:
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
						break;
					case Conversation.TYPE_CONFERNECE:
						g = ((ConferenceConversation) currentConversation)
								.getGroup();
						groupType = "CONFERENCE";
						break;
					case Conversation.TYPE_GROUP:
						if (mCurrentTabFlag == V2GlobalEnum.GROUP_TYPE_USER) {
							CrowdConversation crowd = ((CrowdConversation) item.cov);
							if (crowd.getGroup().getName() == null) {
								Group newGroup = GlobalHolder.getInstance()
										.getGroupById(
												V2GlobalEnum.GROUP_TYPE_CROWD,
												crowd.getExtId());
								if (newGroup != null) {
									crowd.setG(newGroup);
									crowd.setReadFlag(Conversation.READ_FLAG_READ);
									VMessage vm = MessageLoader
											.getNewestGroupMessage(
													mContext,
													V2GlobalEnum.GROUP_TYPE_CROWD,
													crowd.getExtId());
									if (vm != null) {
										crowd.setName(newGroup.getName());
										crowd.setDate(vm.getDateTimeStr());
										crowd.setDateLong(String.valueOf(vm
												.getmDateLong()));
										CharSequence newMessage = MessageUtil
												.getMixedConversationContent(
														mContext, vm);
										crowd.setMsg(newMessage);
										currentGroupLayout.update();
										adapter.notifyDataSetChanged();
										V2Log.d(TAG,
												"Successfully updated the CROWD_GROUP infos , "
														+ "group name is :"
														+ crowd.getName());
									}
								}
							}
						} else {
							g = ((CrowdConversation) currentConversation)
									.getGroup();
						}
						groupType = "CROWD";
						break;
					case V2GlobalEnum.GROUP_TYPE_DEPARTMENT:
						if (mCurrentTabFlag == V2GlobalEnum.GROUP_TYPE_USER) {
							DepartmentConversation departCon = ((DepartmentConversation) item.cov);
							if (TextUtils.isEmpty(departCon.getName())) {
								Group department = GlobalHolder.getInstance()
										.getGroupById(departCon.getType(),
												departCon.getExtId());
								if (department != null) {
									departCon.setDepartmentGroup(department);
									departCon
											.setReadFlag(Conversation.READ_FLAG_READ);
									VMessage vm = MessageLoader
											.getNewestGroupMessage(
													mContext,
													V2GlobalEnum.GROUP_TYPE_DEPARTMENT,
													departCon.getExtId());
									if (vm != null) {
										departCon.setName(department.getName());
										departCon.setDate(vm.getDateTimeStr());
										departCon.setDateLong(String.valueOf(vm
												.getmDateLong()));
										CharSequence newMessage = MessageUtil
												.getMixedConversationContent(
														mContext, vm);
										departCon.setMsg(newMessage);
										currentGroupLayout.update();
										adapter.notifyDataSetChanged();
										V2Log.d(TAG,
												"Successfully updated the DEPARTMENT_GROUP infos , "
														+ "group name is :"
														+ department.getName());
									}
								}
							}
							g = departCon.getDepartmentGroup();
						} else {
							g = ((DepartmentConversation) currentConversation)
									.getDepartmentGroup();
						}
						groupType = "DEPARTMENT";
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
			} else if (P2PConversation.P2P_BROADCAST_MEDIA_UPDATE.equals(intent
					.getAction())) {
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
				long cid = intent.getLongExtra("crowd", 0);
				removeConversation(cid);
				// clear the crowd group all chat database messges
				MessageLoader.deleteMessageByID(context, mCurrentTabFlag, cid,
						-1);
				// clear the crowd group all verification database messges
				MessageLoader.deleteCrowdVerificationMessage(context, cid);
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

				if (verificationMessageItemLayout == null
						|| verificationMessageItemData == null)
					initVerificationItem();
				String msg = showUnreadFriendAuthentication();
				if (msg == null) {
					V2Log.d(TAG,
							"update friend verification message content failed... get null");
					return;
				}

				updateVerificationMessage(msg);
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

				if (verificationMessageItemLayout != null) {
					String content = "";
					verificationMessageItemLayout.updateNotificator(true);
					if (verificationMessageItemData != null) {
						switch (msg.getType()) {
						case CROWD_INVITATION:
							String invitationName = null;
							VMessageQualificationInvitationCrowd invitation = (VMessageQualificationInvitationCrowd) msg;
							if (TextUtils.isEmpty(invitation
									.getInvitationUser().getName())) {
								User user = GlobalHolder.getInstance().getUser(
										invitation.getInvitationUser()
												.getmUserId());
								if (user != null)
									invitationName = user.getName();
							} else
								invitationName = invitation.getInvitationUser()
										.getName();
							content = invitationName
									+ mContext
											.getText(R.string.crowd_invitation_content);
							break;
						case CROWD_APPLICATION:
							content = mContext.getText(
									R.string.crowd_applicant_content)
									.toString();
							break;
						default:
							break;
						}

						if (msg.getmTimestamp() == null)
							msg.setmTimestamp(new Date(GlobalConfig
									.getGlobalServerTime()));
						verificationMessageItemData.setMsg(content);
						verificationMessageItemData.setDate(DateUtil
								.getStandardDate(msg.getmTimestamp()));
						verificationMessageItemData.setDateLong(String
								.valueOf(msg.getmTimestamp().getTime()));
						verificationMessageItemLayout.update();
						((ConversationFirendAuthenticationData) verificationMessageItemData)
								.setMessageType(ConversationFirendAuthenticationData.VerificationMessageType.CROWD_TYPE);
					}
					updateVerificationMessage(content);
					if (msg.getReadState() == VMessageQualification.ReadState.UNREAD) {
						notificationListener.updateNotificator(
								Conversation.TYPE_CONTACT, true);
						showAuthenticationNotification = true;
						notificationListener.updateNotificator(mCurrentTabFlag,
								true);
					} else {
						notificationListener.updateNotificator(
								Conversation.TYPE_CONTACT, false);
						notificationListener.updateNotificator(mCurrentTabFlag,
								false);
					}
				}
			} else if (PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION
					.equals(intent.getAction())
					|| intent.getAction().equals(
							JNIService.JNI_BROADCAST_KICED_CROWD)) {
				long cid = intent.getLongExtra("crowd", 0);
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
					.setSmallIcon(R.drawable.ic_launcher).setContentTitle("通知")
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
				msg = name + "已添加你为好友";
			} else if ((tempNode.fromUserID == tempNode.remoteUserID)
					&& (tempNode.ownerAuthType == 1)) {// 别人加我不管我有没有处理
				msg = name + "申请加你为好友";
			} else if ((tempNode.fromUserID == tempNode.ownerUserID)
					&& (tempNode.addState == 0)) {// 我加别人等待验证
				msg = "申请加" + name + "为好友等待对方验证";
			} else if ((tempNode.fromUserID == tempNode.ownerUserID)
					&& (tempNode.addState == 1)) {// 我加别人已被同意或我加别人不需验证
				msg = "你和" + name + "成为了好友";
			} else if ((tempNode.fromUserID == tempNode.ownerUserID)
					&& (tempNode.addState == 2)) {// 我加别人已被拒绝
				msg = name + "拒绝你的好友请求";
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

	private void showCrowdAuthentication() {

		VMessageQualification nestQualification = MessageLoader
				.getNewestCrowdVerificationMessage(mContext, GlobalHolder
						.getInstance().getCurrentUser());
		if (verificationMessageItemLayout != null) {
			verificationMessageItemLayout.updateNotificator(nestQualification
					.getReadState().intValue() == 0 ? true : false);
			if (verificationMessageItemData != null) {
				String content = "";
				switch (nestQualification.getType()) {
				case CROWD_INVITATION:
					VMessageQualificationInvitationCrowd invitation = (VMessageQualificationInvitationCrowd) nestQualification;
					content = invitation.getInvitationUser().getName()
							+ mContext
									.getText(R.string.crowd_invitation_content);
					((ConversationFirendAuthenticationData) verificationMessageItemData)
							.setUser(invitation.getInvitationUser());
					break;
				case CROWD_APPLICATION:
					VMessageQualificationApplicationCrowd apply = (VMessageQualificationApplicationCrowd) nestQualification;
					User applicant = apply.getApplicant();
					content = applicant.getName()
							+ mContext
									.getText(R.string.crowd_applicant_content)
									.toString();
					((ConversationFirendAuthenticationData) verificationMessageItemData)
							.setUser(applicant);
					break;
				default:
					break;
				}
				verificationMessageItemData.setMsg(content);
				if (nestQualification.getmTimestamp() != null) {
					verificationMessageItemData
							.setDate(DateUtil.getStandardDate(nestQualification
									.getmTimestamp()));
					verificationMessageItemData.setDateLong(String
							.valueOf(nestQualification.getmTimestamp()
									.getTime()));
				}
				verificationMessageItemLayout.update();
			}

			if (nestQualification.getReadState().intValue() == Conversation.READ_FLAG_UNREAD) {
				notificationListener.updateNotificator(
						Conversation.TYPE_CONTACT, true);
				showAuthenticationNotification = true;
			} else {
				notificationListener.updateNotificator(
						Conversation.TYPE_CONTACT, false);
				showAuthenticationNotification = false;
			}
		}
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

					if (groupList != null && groupList.size() > 0
							&& !isLoadedCov)
						populateConversation(groupList);
					else
						groupList = null;
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
						new Registrant(this, REQUEST_ENTER_CONF_RESPONSE, null));
				break;
			case REQUEST_ENTER_CONF_RESPONSE:
				if (mWaitingDialog != null && mWaitingDialog.isShowing()) {
					mWaitingDialog.dismiss();

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

	/**
	 * Update main activity to show or hide notificator
	 * 
	 * @param b
	 */
	private void updateUnreadVoiceConversation(boolean b) {

		if (voiceLayout == null) {
			Log.e(TAG,
					"update unread voice conversationing , the voiceLayout is null");
			return;
		}
		// Update main activity to show or hide notificator
		if (b)
			this.notificationListener.updateNotificator(mCurrentTabFlag, true);
		else
			this.notificationListener.updateNotificator(mCurrentTabFlag, false);
	}
}
