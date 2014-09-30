package com.v2tech.view;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
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
import com.v2tech.db.DataBaseContext;
import com.v2tech.db.V2techSearchContentProvider;
import com.v2tech.service.BitmapManager;
import com.v2tech.service.ConferencMessageSyncService;
import com.v2tech.service.ConferenceService;
import com.v2tech.service.CrowdGroupService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.Registrant;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.service.jni.RequestEnterConfResponse;
import com.v2tech.util.DateUtil;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.MessageUtil;
import com.v2tech.util.Notificator;
import com.v2tech.view.bo.ConversationNotificationObject;
import com.v2tech.view.conference.GroupLayout;
import com.v2tech.view.conference.VideoActivityV2;
import com.v2tech.view.contacts.VoiceMessageActivity;
import com.v2tech.view.contacts.add.AddFriendHistroysHandler;
import com.v2tech.view.conversation.MessageAuthenticationActivity;
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
import com.v2tech.vo.CrowdConversation;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.DepartmentConversation;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.User;
import com.v2tech.vo.VMessage;
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

	private NotificationListener notificationListener;

	private BroadcastReceiver receiver;
	private IntentFilter intentFilter;
	private boolean mIsStartedSearch;

	private List<Conversation> mConvList;
	private Set<Conversation> mUnreadConvList;

	private LocalHandler mHandler = new LocalHandler();

	private boolean isLoadedCov = false;

	private View rootView;

	private List<ScrollItem> mItemList = new CopyOnWriteArrayList<ScrollItem>();
	private List<Conversation> mCacheItemList;

	private Context mContext;

	private ListView mConversationsListView;

	private ConversationsAdapter adapter = new ConversationsAdapter();

	private ConferenceService cb;

	private CrowdGroupService crowdService;

	private int mCurrentTabFlag;

	private int currentPosition;

	private int currentMoveViewPosition;
	private ScrollItem verificationItem;
	private ScrollItem voiceItem;

	private long lastDateTime = 0;

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
			crowdService = new CrowdGroupService();
		}
		mContext = getActivity();

		initReceiver(mCurrentTabFlag);
		Log.d(TAG, "current tag:" + tag);

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
	public void onDetach() {
		super.onDetach();
		mItemList.clear();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getActivity().unregisterReceiver(receiver);
		if (mItemList != null) {
			mItemList.clear();
		}
		if (mCacheItemList != null) {
			mCacheItemList.clear();
		}
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
				intentFilter.addAction(JNIService.JNI_BROADCAST_NEW_MESSAGE);
				intentFilter
						.addAction(PublicIntent.REQUEST_UPDATE_CONVERSATION);
			}

			if (mCurrentTabFlag == Conversation.TYPE_CONTACT) {
				intentFilter
						.addAction(JNIService.JNI_BROADCAST_FRIEND_AUTHENTICATION);
				intentFilter
						.addAction(JNIService.JNI_BROADCAST_NEW_QUALIFICATION_MESSAGE);
			}

			if (mCurrentTabFlag == Conversation.TYPE_GROUP) {
				intentFilter
						.addAction(PublicIntent.BROADCAST_NEW_CROWD_NOTIFICATION);
				intentFilter
						.addAction(PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION);
			}
		}

		getActivity().registerReceiver(receiver, intentFilter);
	}

	@Override
	public void onStart() {
		super.onStart();
		if (mCurrentTabFlag == Conversation.TYPE_CONTACT) {
			showUnreadFriendAuthentication();
		}
	}

	@Override
	public void onStop() {
		firstSearchCacheList.clear();
		searchList.clear();
		mIsStartedSearch = false;
		startIndex = 0;
		adapter.notifyDataSetChanged();
		super.onStop();
	}

	private List<ScrollItem> searchList = null;
	private List<ScrollItem> searchCacheList = null;
	private List<ScrollItem> firstSearchCacheList = null;
	private int lastSize;
	private boolean isShouldAdd;
	private boolean isShouldQP; // 是否需要启动全拼
	private int startIndex = 0;

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
			return;
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

		// if(mItemListCache == null){
		// mItemListCache = new CopyOnWriteArrayList<ScrollItem>();
		// mItemListCache.addAll(mItemList);
		// }
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {

	}

	public boolean isChineseWord(char mChar) {
		Pattern pattern = Pattern.compile("[\\u4E00-\\u9FA5]"); // 判断是否为汉字
		Matcher matcher = pattern.matcher(String.valueOf(mChar));
		return matcher.find();
	}

	private String searchTarget;
	private boolean isBreak;

	/**
	 * 根据 searchKey 获得搜索后的集合
	 * 
	 * @param mCacheItemList
	 * @param searchKey
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

		for (int i = 0; list != null && i < list.size(); i++) { // 一级循环，循环所有消息
			ScrollItem scrollItem = list.get(i);
			Conversation cov = list.get(i).cov;
			if (cov.getName() == null) {
				// 判断是否能获取到消息item的名字
				continue;
			} else {
				// 将名字分割为字符数组遍历
				char[] charArray = cov.getName().toCharArray();
				for (int j = 0; j < charArray.length; j++) { // 二级循环，循环消息名称
					searchTarget = String.valueOf(charArray[j]);
					if (searchTarget == null) {
						continue;
					}

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
			return;
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
			V2Log.w(" group list is null");
			return;
		}

		if (mCurrentTabFlag == Conversation.TYPE_GROUP) {
			// loading department conversation
			if (mCurrentTabFlag == V2GlobalEnum.GROUP_TYPE_CROWD) {
				List<DepartmentConversation> departs = ConversationProvider
						.loadDepartConversation(mContext);
				if (departs.size() == 0)
					V2Log.e(TAG,
							"populateConversation load departs list is null...");
				for (DepartmentConversation cons : departs) {
					this.mConvList.add(cons);
				}
			}
		}

		for (Group g : list) {
			Conversation cov = null;
			if (g.getName() == null)
				V2Log.e(TAG,
						"the group name is null , group id is :" + g.getmGId());
			if (g.getGroupType() == GroupType.CONFERENCE) {
				cov = new ConferenceConversation(g);
			} else if (g.getGroupType() == GroupType.CHATING) {
				cov = new CrowdConversation(g);
			}
			// Update all initial conversation to read
			cov.setReadFlag(Conversation.READ_FLAG_READ);
			long time = GlobalConfig.getGlobalServerTime();
			cov.setDate(DateUtil.getStandardDate(new Date(time)));
			this.mConvList.add(cov);
		}

		fillAdapter(this.mConvList, true);
	}

	/**
	 * Add conversation to list
	 * 
	 * @param g
	 * @param flag
	 */
	private void addConversation(Group g, boolean flag) {
		Conversation cov = null;
		if (g.getGroupType() == GroupType.CONFERENCE) {
			cov = new ConferenceConversation(g);
		} else if (g.getGroupType() == GroupType.CHATING) {
			cov = new CrowdConversation(g);
		}
		this.mConvList.add(0, cov);
		GroupLayout gp = new GroupLayout(this.getActivity(), cov);
		gp.updateNotificator(flag);
		mItemList.add(0, new ScrollItem(cov, gp));
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

	/**
	 * Load local conversation list
	 */
	private synchronized void loadUserConversation() {
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
				updateVoiceSpecificItemState(false);
			}
			// init add friend verification item
			String sql = "select * from " + AddFriendHistroysHandler.tableName
					+ " order by SaveDate desc limit 1";
			Cursor cursor = AddFriendHistroysHandler.select(getActivity(), sql,
					new String[] {});
			if (cursor != null && cursor.getCount() > 0) {
				initVerificationItem();
				showUnreadFriendAuthentication();
			}
		}
		new Thread(new Runnable() {

			@Override
			public void run() {
				// FIXME don't sleep Thread
				SystemClock.sleep(3000);
				mConvList = ConversationProvider.loadUserConversation(mContext,
						mConvList, mCurrentTabFlag,
						verificationMessageItemData, voiceMessageItem);
				isLoadedCov = true;
				Message.obtain(mHandler, UPDATE_CONVERSATION_MESSAGE)
						.sendToTarget();
			}
		}).start();

	}

	private void fillAdapter(List<Conversation> list, boolean isFresh) {
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
				GroupLayout gp = new GroupLayout(mContext, cov);
				if (cov.getReadFlag() == Conversation.READ_FLAG_UNREAD) {
					gp.updateNotificator(true);
					updateUnreadConversation(cov);
				}
				mItemList.add(new ScrollItem(cov, gp));
				break;
			}
		}

		showUnreadFriendAuthentication();
		if (isFresh) {
			adapter.notifyDataSetChanged();
		}
	}

	// private boolean isExist;
	private Conversation voiceMessageItem;
	private GroupLayout voiceLayout;
	private GroupLayout verificationMessageItemLayout;
	private Conversation verificationMessageItemData;

	private void initVoiceItem() {
		voiceMessageItem = new Conversation(Conversation.TYPE_VOICE_MESSAGE, 0);
		voiceMessageItem.setExtId(-1);
		voiceMessageItem.setName("通话消息");
		voiceLayout = new GroupLayout(mContext, voiceMessageItem);
		voiceMessageItem.setReadFlag(Conversation.READ_FLAG_READ);
		voiceItem = new ScrollItem(voiceMessageItem, voiceLayout);
	}

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
	 * @param isFromDatabase
	 */
	private void updateVoiceSpecificItemState(boolean isFromDatabase) {

		VideoBean newestMediaMessage = MessageLoader
				.getNewestMediaMessage(mContext);
		if (newestMediaMessage != null && newestMediaMessage.startDate != 0) {
			String startDate = DateUtil
					.getStringDate(newestMediaMessage.startDate);
			voiceMessageItem.setDate(startDate);
			voiceMessageItem.setDateLong(String
					.valueOf(newestMediaMessage.startDate));

			if (isFromDatabase) {
				boolean isShowFlag = false;
				if (isHasUnreadMediaMessage())
					isShowFlag = true;
				else
					isShowFlag = false;
				voiceLayout.update(null, startDate, isShowFlag);
				if (newestMediaMessage.readSatate == AudioVideoMessageBean.STATE_UNREAD)
					updateUnreadVoiceConversation(true);
			}
			else
				voiceLayout.update(null, startDate, false);
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

	/**
	 * update group type conversation according groupType and groupID
	 * 
	 * @param groupType
	 * @param groupID
	 */
	private void updateGroupConversation(int groupType, long groupID) {
		Log.d(TAG, "update Conversation two param calling...");
		if (!isLoadedCov) {
			this.loadUserConversation();
		}

		VMessage vm = MessageLoader.getNewestGroupMessage(mContext, groupType,
				groupID);
		if (vm == null) {
			V2Log.e("update group conversation failed.. Didn't find message "
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
				currentMoveViewPosition = i;
				existedCov = cov;
				viewLayout = (GroupLayout) this.mItemList.get(i).gp;
				break;
			}
		}

		if (foundFlag) {
			if (groupType == Conversation.TYPE_DEPARTMENT) {

				Group department = GlobalHolder.getInstance().getGroupById(
						groupType, groupID);
				if (department != null) {
					existedCov.setName(department.getName());
					existedCov.setReadFlag(Conversation.READ_FLAG_READ);
				}
			}
		} else {

			switch (groupType) {
			case V2GlobalEnum.GROUP_TYPE_DEPARTMENT:
				Group department = GlobalHolder.getInstance().findGroupById(
						groupID);
				if (department == null) {
					return;
				}
				existedCov = new DepartmentConversation(department);
				ConversationProvider.saveConversation(mContext, vm);
				break;
			default:
				break;
			}
			// 添加到ListView中
			viewLayout = new GroupLayout(mContext, existedCov);
			mConvList.add(0, existedCov);
			mItemList.add(0, new ScrollItem(existedCov, viewLayout));
		}

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

	/**
	 * Update conversation according to message id and remote user id, This
	 * request call only from new message broadcast
	 * 
	 * @param msgId
	 * @param remoteUserID
	 */
	private void updateUserConversation(long remoteUserID, long msgId) {
		Log.d(TAG, "update Conversation msgId calling...");
		if (!isLoadedCov) {
			this.loadUserConversation();
		}

		VMessage vm = MessageLoader.loadUserMessageById(mContext, remoteUserID,
				msgId);
		if (vm == null) {
			V2Log.e("Didn't find message " + msgId);
			return;
		}

		long extId = -1;
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
				currentMoveViewPosition = i;
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

			V2Log.e(TAG, "VMessage :" + vm.getDate().getTime());
			V2Log.e(TAG, "lastDateTime : " + lastDateTime);
			if (vm.getMsgCode() != V2GlobalEnum.GROUP_TYPE_CROWD
					&& vm.getDate().getTime() != lastDateTime) {
				lastDateTime = vm.getDate().getTime();
				ScrollItem scrollItem = mItemList.get(currentMoveViewPosition);
				mItemList.remove(currentMoveViewPosition);
				mConvList.remove(currentMoveViewPosition);
				mItemList.add(0, scrollItem);
				mConvList.add(0, scrollItem.cov);
			}
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
		updateUnreadConversation(existedCov);
		adapter.notifyDataSetChanged();
	}

	private void updateUnreadConversation(Conversation cov) {
		boolean flag = false;
		int ret = -1;
		if (cov.getReadFlag() == Conversation.READ_FLAG_READ) {
			flag = mUnreadConvList.remove(cov);
			ret = Conversation.READ_FLAG_READ;
		} else {
			flag = mUnreadConvList.add(cov);
			ret = Conversation.READ_FLAG_UNREAD;
		}
		// Update main activity to show or hide notificator
		if (mUnreadConvList.size() > 0) {
			this.notificationListener.updateNotificator(mCurrentTabFlag, true);
		} else {
			this.notificationListener.updateNotificator(mCurrentTabFlag, false);
		}

		ContentValues ct = new ContentValues();
		ct.put(ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_READ_STATE,
				ret);

		// If flag is true, means we updated unread list, then we need to update
		// database
		if (flag)
			ConversationProvider.updateConversationToDatabase(mContext, cov,
					ret);
	}

	/**
	 * Remove conversation to list
	 * 
	 * @param id
	 * @param type
	 * @param owner
	 */
	protected void removeConversation(long id, int type) {

		Conversation cache = null;
		for (int i = 0; i < mConvList.size(); i++) {
			if (mConvList.get(i).getExtId() == id) {
				mItemList.remove(i);
				Conversation conversation = mConvList.get(i);
				if (conversation.getType() != Conversation.TYPE_VOICE_MESSAGE
						&& conversation.getType() != Conversation.TYPE_VERIFICATION_MESSAGE) {
					ConversationProvider.deleteConversation(mContext,
							mConvList.get(i));
				}
				cache = mConvList.remove(i);
				break;
			}
		}
		if (cache != null) {
			adapter.notifyDataSetChanged();
			// Set removed conversation state to readed
			cache.setReadFlag(Conversation.READ_FLAG_READ);
			updateUnreadConversation(cache);
			Notificator.cancelSystemNotification(getActivity(),
					PublicIntent.MESSAGE_NOTIFICATION_ID);
		}

	}

	private void updateStatusBar(VMessage vm) {
		String content = "";
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
		Message.obtain(this.mHandler, REQUEST_ENTER_CONF, Long.valueOf(gid))
				.sendToTarget();

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

	class ScrollItem {
		Conversation cov;
		View gp;

		public ScrollItem(Conversation g, View gp) {
			super();
			this.cov = g;
			this.gp = gp;
			this.gp.setTag(cov);
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
			currentMoveViewPosition = pos;
			lasttime = System.currentTimeMillis();

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

					// ScrollItem scrollItem = mItemList
					// .get(currentMoveViewPosition);
					// mItemList.remove(currentMoveViewPosition);
					// mConvList.remove(currentMoveViewPosition);
					// mItemList.add(0, scrollItem);
					// mConvList.add(0, scrollItem.cov);
					// adapter.notifyDataSetChanged();
				} else if (cov.getType() == Conversation.TYPE_VERIFICATION_MESSAGE) {

					Intent intent = new Intent(mContext,
							MessageAuthenticationActivity.class);
					startActivity(intent);
					// ScrollItem scrollItem = mItemList
					// .get(currentMoveViewPosition);
					// mItemList.remove(currentMoveViewPosition);
					// mConvList.remove(currentMoveViewPosition);
					// mItemList.add(0, scrollItem);
					// mConvList.add(0, scrollItem.cov);
					// adapter.notifyDataSetChanged();
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
						mContext.getResources().getString(
								R.string.conversations_delete_conversaion),
						mContext.getResources().getString(
								R.string.conversations_detail) };
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
									break;
								case Conversation.TYPE_GROUP:
									if (cov.getType() == Conversation.TYPE_GROUP) {
										CrowdGroup crowd = (CrowdGroup) GlobalHolder
												.getInstance().getGroupById(
														GroupType.CHATING
																.intValue(),
														cov.getExtId());
										if (crowdService != null) {
											crowdService.quitCrowd(crowd, null);
										}
										break;
									}
								}
								removeConversation(cov.getExtId(),
										cov.getType());
							} else {
								Intent crowdIntent = new Intent();
								crowdIntent.setClass(mContext,
										CrowdDetailActivity.class);
								crowdIntent.putExtra("cid", cov.getExtId());
								mContext.startActivity(crowdIntent);
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
						mItemList.clear();
						fillAdapter(mConvList, false);
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
						|| (type == GroupType.CHATING.intValue() && mCurrentTabFlag == Conversation.TYPE_GROUP)) {
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

				if (isNeedDelete) {

					removeConversation(uao.getExtId(), uao.getType());
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
				if (mCurrentTabFlag == uao.getType()) {
					Message.obtain(mHandler, UPDATE_CONVERSATION, uao)
							.sendToTarget();
				}
				// Update name of creator of conversation
			} else if (JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION
					.equals(intent.getAction())) {
				V2Log.d(TAG,
						"the JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION was invoking...");
				for (ScrollItem item : mItemList) {
					V2Log.d(TAG,
							" the current item type is :" + item.cov.getType());
					if ((item.cov.getType() == Conversation.TYPE_CONFERNECE)
							|| item.cov.getType() == Conversation.TYPE_GROUP
							|| item.cov.getType() == V2GlobalEnum.GROUP_TYPE_DEPARTMENT) {
						GroupLayout layout = ((GroupLayout) item.gp);
						Conversation con = item.cov;
						Group g = null;
						String groupType = null;
						switch (item.cov.getType()) {
						case Conversation.TYPE_CONFERNECE:
							g = ((ConferenceConversation) item.cov).getGroup();
							groupType = "CONFERENCE";
							break;
						case Conversation.TYPE_GROUP:
							g = ((CrowdConversation) item.cov).getGroup();
							groupType = "CROWD";
							break;
						case V2GlobalEnum.GROUP_TYPE_DEPARTMENT:
							DepartmentConversation departCon = ((DepartmentConversation) item.cov);
							if (con.getName() == "" || con.getName() == null) {
								Group department = GlobalHolder.getInstance()
										.getGroupById(
												Integer.valueOf(con.getType()),
												con.getExtId());
								if (department != null) {
									departCon.setDepartmentGroup(department);
									V2Log.e(TAG,
											"获得部门名称：" + department.getName());
									con.setName(department.getName());
									layout.update();
									con.setReadFlag(Conversation.READ_FLAG_READ);
									adapter.notifyDataSetChanged();
								}
							}
							g = departCon.getDepartmentGroup();
							groupType = "DEPARTMENT";
							break;
						}

						if (g.getOwnerUser() == null)
							continue;

						User u = GlobalHolder.getInstance().getUser(
								g.getOwnerUser().getmUserId());
						if (u == null) {
							continue;
						}
						V2Log.e(TAG, "update " + groupType
								+ " group user name :" + u.getName()
								+ "group id is :" + g.getmGId());
						layout.updateContent(u.getName());
						layout.updateGroupOwner(g.getName());
						g.setOwnerUser(u);
					} else if (item.cov.getType() == Conversation.TYPE_CONTACT) {
						User u = GlobalHolder.getInstance().getUser(
								((ContactConversation) item.cov).getExtId());
						if (u == null) {
							continue;
						}
						((ContactConversation) item.cov).updateUser(u);
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

					updateVoiceSpecificItemState(true);

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
				if (g != null) {
					addConversation(g, false);
				} else {
					V2Log.e("Can not get crowd :" + gid);
				}
			} else if (JNIService.JNI_BROADCAST_NEW_MESSAGE.equals(intent
					.getAction())) {
				boolean groupMessage = intent.getBooleanExtra("gm", false);
				if (groupMessage) {
					V2Log.d(TAG,
							"JNI_BROADCAST_NEW_MESSAGE group message update..");
					updateGroupConversation(
							intent.getIntExtra("groupType", -1),
							intent.getLongExtra("groupID", -1));
				}
			} else if (PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION
					.equals(intent.getAction())) {
				long cid = intent.getLongExtra("crowd", 0);
				removeConversation(cid, Conversation.TYPE_GROUP);
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
				removeConversation(confId, Conversation.TYPE_CONFERNECE);

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
				if (!groupMessage) {
					V2Log.e(TAG,
							"JNI_BROADCAST_NEW_MESSAGE user message update..");
					updateUserConversation(
							intent.getExtras().getLong("remoteUserID"), intent
									.getExtras().getLong("mid"));
				}
			} else if (action
					.equals(JNIService.JNI_BROADCAST_FRIEND_AUTHENTICATION)) {
				V2Log.e(TAG, "JNI_BROADCAST_FRIEND_AUTHENTICATIONE update..");

				if (verificationMessageItemLayout == null
						|| verificationMessageItemData == null)
					initVerificationItem();
				String msg = showUnreadFriendAuthentication();
				if (msg == null) {
					return;
				}

				if (((MainApplication) getActivity().getApplication())
						.theAppIsRunningBackground()) {
					// 发通知
					Intent i = new Intent(getActivity(),
							MessageAuthenticationActivity.class);
					i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
							| Intent.FLAG_ACTIVITY_CLEAR_TOP);

					PendingIntent pendingIntent = PendingIntent.getActivity(
							getActivity(), 0, i, 0);

					Notification notification = new Notification.Builder(
							getActivity()).setSmallIcon(R.drawable.ic_launcher)
							.setContentTitle("通知").setContentText(msg)
							.setAutoCancel(true).setTicker(msg)
							.setWhen(System.currentTimeMillis())
							.setContentIntent(pendingIntent).getNotification();
					((NotificationManager) getActivity().getSystemService(
							Activity.NOTIFICATION_SERVICE)).notify(0,
							notification);
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
			} else if (JNIService.JNI_BROADCAST_NEW_QUALIFICATION_MESSAGE
					.equals(intent.getAction())) {
				// If this can receive this broadcast, means
				// MessageAuthenticationActivity doesn't show, we need to show
				// red icon
				updateUnreadVoiceConversation(true);
			}
		}
	}

	private String showUnreadFriendAuthentication() {

		// ActivityManager activityManager=(ActivityManager)
		// getActivity().getSystemService(Activity.ACTIVITY_SERVICE);
		//
		// if(activityManager.getRunningTasks(1).get(0).topActivity.equals());

		boolean hasUnread = false;
		// 查出未读的第一条按时间顺序
		String sql = "select * from " + AddFriendHistroysHandler.tableName
				+ " where ReadState=0 order by SaveDate desc limit 1";
		Cursor cr = AddFriendHistroysHandler.select(getActivity(), sql,
				new String[] {});
		if ((cr != null) && (cr.getCount() == 0)) {
			cr.close();
			hasUnread = false;
		} else {
			hasUnread = true;
		}

		sql = "select * from " + AddFriendHistroysHandler.tableName
				+ " order by SaveDate desc limit 1";
		cr = AddFriendHistroysHandler.select(getActivity(), sql,
				new String[] {});

		if ((cr != null) && (cr.getCount() == 0)) {
			cr.close();
			return null;
		}

		String msg = "";
		String date = "";
		String dateLong = "";

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

			String name = GlobalHolder.getInstance()
					.getUser(tempNode.remoteUserID).getName();

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
				V2Log.d(TAG, "the FILL_CONFS_LIST was invoking...");
				if (mCurrentTabFlag == Conversation.TYPE_CONFERNECE) {
					List<Group> gl = GlobalHolder.getInstance().getGroup(
							Group.GroupType.CONFERENCE.intValue());
					if (gl != null && gl.size() > 0 && !isLoadedCov) {
						populateConversation(gl);
					}
				} else if (mCurrentTabFlag == Conversation.TYPE_GROUP) {
					List<Group> gl = GlobalHolder.getInstance().getGroup(
							Group.GroupType.CHATING.intValue());
					if ((gl == null || gl.size() <= 0) && !isLoadedCov) {
						// loading department conversation
						List<DepartmentConversation> departs = ConversationProvider
								.loadDepartConversation(mContext);
						if (departs.size() == 0)
							V2Log.e(TAG,
									" FILL_CONFS_LIST load departs list is null...");
						for (DepartmentConversation cons : departs) {
							mConvList.add(cons);
						}
						fillAdapter(mConvList, true);
						return;
					}

					if (!isLoadedCov) {
						populateConversation(gl);
					}
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
				V2Log.e(TAG, "UPDATE_CONVERSATION_MESSAGE is starting");
				fillAdapter(mConvList, true);
				break;
			}
		}
	}

	protected void startConversationView(Conversation cov) {
		Intent i = new Intent(PublicIntent.START_CONVERSACTION_ACTIVITY);
		i.addCategory(PublicIntent.DEFAULT_CATEGORY);
		i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		i.putExtra("obj", new ConversationNotificationObject(cov));
		startActivity(i);
	}

	/**
	 * 
	 * @param b
	 */
	protected void updateUnreadVoiceConversation(boolean b) {

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
