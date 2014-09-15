package com.v2tech.view;

import java.text.SimpleDateFormat;
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
import com.v2tech.view.conversation.MessageLoader;
import com.v2tech.view.group.CrowdDetailActivity;
import com.v2tech.view.conversation.P2PConversation;
import com.v2tech.vo.AudioVideoMessageBean;
import com.v2tech.vo.Conference;
import com.v2tech.vo.ConferenceConversation;
import com.v2tech.vo.ConferenceGroup;
import com.v2tech.vo.ContactConversation;
import com.v2tech.vo.Conversation;
import com.v2tech.vo.CrowdConversation;
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

	private int mCurrentTabFlag;

	private int currentPosition;

	private int currentMoveViewPosition;
	private boolean updateConversation;
	private long lastDateTime;
	private long lastExitId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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

			char[] charSimpleArray = s.toString().toLowerCase(Locale.getDefault()).toCharArray();
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
							searchTarget = searchTarget.toLowerCase(Locale.getDefault());
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

		for (Group g : list) {
			Conversation cov = null;
			if (g.getGroupType() == GroupType.CONFERENCE) {
				cov = new ConferenceConversation(g);
			} else if (g.getGroupType() == GroupType.CHATING) {
				cov = new CrowdConversation(g);
			}
			// Update all initial conversation to read
			cov.setReadFlag(Conversation.READ_FLAG_READ);
			long time = (((System.currentTimeMillis() - GlobalConfig.LOCAL_TIME) / 1000) + GlobalConfig.SERVER_TIME) * 1000;
			SimpleDateFormat format = new SimpleDateFormat(
					"yyyy-MM-dd hh:mm:ss");
			cov.setDate(format.format(new Date(time)));
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
	private synchronized void loadConversation() {
		if (isLoadedCov) {
			return;
		}

		new Thread(new Runnable() {

			@Override
			public void run() {
				SystemClock.sleep(1500);
				Cursor mCur = getActivity()
						.getContentResolver()
						.query(ContentDescriptor.RecentHistoriesMessage.CONTENT_URI,
								ContentDescriptor.RecentHistoriesMessage.Cols.ALL_CLOS,
								ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_GROUP_TYPE
										+ "=? and "
										+ ContentDescriptor.RecentHistoriesMessage.Cols.OWNER_USER_ID
										+ "=?",
								new String[] {
										mCurrentTabFlag + "",
										GlobalHolder.getInstance()
												.getCurrentUserId() + "" },
								ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_SAVEDATE
										+ " desc");

				while (mCur.moveToNext()) {
					Conversation cov = extractConversation(mCur);
					if (cov.getType() == mCurrentTabFlag) {
						VMessage vm = MessageLoader.getNewestMessage(mContext,
								GlobalHolder.getInstance().getCurrentUserId(),
								cov.getExtId());
						if (vm != null) {
							CharSequence newMessage = MessageUtil
									.getMixedConversationContent(mContext, vm);
							cov.setMsg(newMessage);
						}
						mConvList.add(cov);
					}
				}
				mCur.close();

				isLoadedCov = true;
				Message.obtain(mHandler, UPDATE_CONVERSATION_MESSAGE)
						.sendToTarget();
			}
		}).start();

	}

	private Conversation extractConversation(Cursor cur) {
		long extId = cur.getLong(cur.getColumnIndex("RemoteUserID"));
		int readState = cur.getInt(cur.getColumnIndex("ReadState"));
		User u = GlobalHolder.getInstance().getUser(extId);
		if (u == null) {
			u = new User(extId);
		}
		Conversation cov = new ContactConversation(u);
		VMessage vm = MessageLoader.getNewestMessage(mContext, GlobalHolder
				.getInstance().getCurrentUserId(), extId);
		if (vm != null) {
			cov.setDate(vm.getDateTimeStr());
			cov.setDateLong(String.valueOf(vm.getmDateLong()));
		}
		cov.setReadFlag(readState);
		return cov;
	}

	private void fillAdapter(List<Conversation> list, boolean isFresh) {
		mItemList.clear();
		for (int i = 0; i < list.size(); i++) {
			Conversation cov = list.get(i);
			GroupLayout gp = new GroupLayout(mContext, cov);
			if (cov.getReadFlag() == Conversation.READ_FLAG_UNREAD) {
				gp.updateNotificator(true);
				updateUnreadConversation(cov);
			}
			mItemList.add(new ScrollItem(cov, gp));
		}
		// 判断只有消息界面，才添加这两个特殊item
		if (mCurrentTabFlag == Conversation.TYPE_CONTACT)
			initSpecificItem();

		if (isFresh) {
			adapter.notifyDataSetChanged();
		}
	}


	private boolean isExist;
	private Conversation voiceMessageItem;
	private GroupLayout voiceLayout;

	private void initSpecificItem() {
		if (isExist) {
			return;
		}

		isExist = true;
		voiceMessageItem = new Conversation(
				Conversation.TYPE_VOICE_MESSAGE, 0);
		Conversation verificationMessageItem = new Conversation(
				Conversation.TYPE_VERIFICATION_MESSAGE, 0);
		voiceLayout = new GroupLayout(mContext, voiceMessageItem);
		GroupLayout verificationLayout = new GroupLayout(mContext,
				verificationMessageItem);
		mConvList.add(voiceMessageItem);
		mConvList.add(verificationMessageItem);
		mItemList.add(new ScrollItem(voiceMessageItem, voiceLayout));
		mItemList.add(new ScrollItem(verificationMessageItem,
				verificationLayout));

		VideoBean newestMediaMessage = MessageLoader
				.getNewestMediaMessage(mContext);
		if (newestMediaMessage != null && newestMediaMessage.startDate != 0) {

			voiceLayout
					.update(null,
							DateUtil.getStringDate(newestMediaMessage.startDate),
							false);
		}

		if (isHasUnreadMediaMessage())
			updateUnreadVoiceConversation(true);
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

	private void updateConversation(int groupType, long groupID) {
		Log.d(TAG, "updateConversation two param calling...");
		if (!isLoadedCov) {
			this.loadConversation();
		}
		Conversation existedCov = null;
		GroupLayout viewLayout = null;
		boolean foundFlag = false;
		for (int i = 0; i < this.mConvList.size(); i++) {
			Conversation cov = this.mConvList.get(i);
			if (cov.getExtId() == groupID) {
				foundFlag = true;
				existedCov = cov;
				viewLayout = (GroupLayout) this.mItemList.get(i).gp;
				break;
			}
		}

		if (foundFlag) {

			VMessage vm = null;
			if (groupType == Conversation.TYPE_GROUP) {
				vm = MessageLoader.getNewestGroupMessage(mContext,
						Long.valueOf(Conversation.TYPE_GROUP), groupID);
			} else if (groupType == Conversation.TYPE_CONTACT) {
				vm = MessageLoader.getNewestMessage(mContext, GlobalHolder
						.getInstance().getCurrentUserId(), existedCov
						.getExtId());
			}

			if (vm == null) {
				V2Log.e("Didn't find conversation " + existedCov.getExtId());
				return;
			}

			CharSequence newMessage = MessageUtil.getMixedConversationContent(
					mContext, vm);
			existedCov.setMsg(newMessage);

			if (groupType == Conversation.TYPE_CONTACT) {
				if (lastExitId != groupID
						|| vm.getDate().getTime() == lastDateTime) {
					updateConversation = false;
				}
				lastDateTime = vm.getDate().getTime();
				existedCov.setDate(vm.getFullDateStr());
				existedCov.setDateLong(String.valueOf(lastDateTime));
			}
			lastExitId = groupID;

			if (updateConversation) {
				ScrollItem scrollItem = mItemList.get(currentMoveViewPosition);
				mItemList.remove(currentMoveViewPosition);
				mConvList.remove(currentMoveViewPosition);
				mItemList.add(0, scrollItem);
				mConvList.add(0, scrollItem.cov);
				adapter.notifyDataSetChanged();
				updateConversation = false;
			}
			existedCov.setReadFlag(Conversation.READ_FLAG_READ);
			// Update view
			viewLayout.update();
			viewLayout.updateNotificator(false);
			// Update unread list
			updateUnreadConversation(existedCov);
		} else {

			if (groupType == Conversation.TYPE_CONTACT) {
				existedCov = new ContactConversation(GlobalHolder.getInstance()
						.getUser(groupID));
				VMessage vm = MessageLoader.getNewestMessage(mContext,
						GlobalHolder.getInstance().getCurrentUserId(),
						existedCov.getExtId());
				insertNewMessage(vm, groupID);
				this.mConvList.add(existedCov);
				viewLayout = new GroupLayout(mContext, existedCov);
				mItemList.add(new ScrollItem(existedCov, viewLayout));
				adapter.notifyDataSetChanged();
			}

		}
	}

	/**
	 * 向数据库插入新的消息对象
	 * 
	 * @param vm
	 * @param extId
	 */
	private void insertNewMessage(VMessage vm, long extId) {

		ContentValues conCv = new ContentValues();
		conCv.put(
				ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_FROM_USER_ID,
				extId);
		conCv.put(
				ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_REMOTE_USER_ID,
				extId);
		conCv.put(
				ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_GROUP_TYPE,
				Conversation.TYPE_CONTACT);
		conCv.put(
				ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_READ_STATE,
				0);
		conCv.put(ContentDescriptor.RecentHistoriesMessage.Cols.OWNER_USER_ID,
				GlobalHolder.getInstance().getCurrentUserId());
		conCv.put(
				ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_TO_USER_ID,
				GlobalHolder.getInstance().getCurrentUserId());
		// conCv.put(ContentDescriptor.RecentHistoriesMessage.Cols.,
		// fromUser == null ? "" : fromUser.getName());
		// conCv.put(ContentDescriptor.RecentHistoriesMessage.Cols.NOTI_FLAG,
		// 0);
		if (vm != null) {
			conCv.put(
					ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_USER_TYPE_ID,
					vm.getMsgCode());
			conCv.put(
					ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_CONTENT,
					vm.getmXmlDatas());
			conCv.put(
					ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_ID,
					vm.getUUID());
			conCv.put(
					ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_SAVEDATE,
					vm.getmDateLong());
		}
		mContext.getContentResolver().insert(
				ContentDescriptor.RecentHistoriesMessage.CONTENT_URI, conCv);
	}

	/**
	 * Update conversation according to message id This request call only from
	 * new message broadcast
	 * 
	 * @param msgId
	 */
	private void updateConversation(long msgId) {
		Log.d(TAG, "updateConversation calling...");
		if (!isLoadedCov) {
			this.loadConversation();
		}
		VMessage vm = MessageLoader.loadMessageById(mContext, msgId);
		if (vm == null) {
			V2Log.e("Didn't find message " + msgId);
			return;
		}

		// 查询出该消息的种类，是私聊还是群聊
		long extId = 0;
		switch (vm.getMsgCode()) {
		case V2GlobalEnum.GROUP_TYPE_USER:
			extId = vm.getFromUser().getmUserId();
			break;
		case V2GlobalEnum.GROUP_TYPE_CROWD:
			extId = vm.getGroupId();
			break;
		}

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
			existedCov.setMsg(MessageUtil.getMixedConversationContent(mContext,
					vm));
			existedCov.setReadFlag(Conversation.READ_FLAG_UNREAD);
			existedCov.setDateLong(String.valueOf(vm.getmDateLong()));
		} else {
			// 展示到界面
			switch (mCurrentTabFlag) {
			case Conversation.TYPE_CONTACT:
				existedCov = new ContactConversation(GlobalHolder.getInstance()
						.getUser(extId));
				break;
			case Conversation.TYPE_GROUP:
				Group g = GlobalHolder.getInstance().findGroupById(extId);
				// Handle for department chatting
				if (g == null) {
					return;
				}
				existedCov = new CrowdConversation(g);
				break;
			}

			existedCov.setMsg(MessageUtil.getMixedConversationContent(mContext,
					vm));
			existedCov.setReadFlag(Conversation.READ_FLAG_UNREAD);
			existedCov.setDateLong(String.valueOf(vm.getmDateLong()));

			// 添加到ListView中
			if (mCurrentTabFlag == Conversation.TYPE_CONTACT) {
				viewLayout = new GroupLayout(mContext, existedCov);
				insertNewMessage(vm, extId);
				this.mConvList.add(existedCov);
				mItemList.add(new ScrollItem(existedCov, viewLayout));
				adapter.notifyDataSetChanged();
			}
		}

		// Update status bar
		updateStatusBar(vm);
		// Update view
		viewLayout.update();
		viewLayout.updateNotificator(true);
		updateUnreadConversation(existedCov);
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
		if (flag) {
			getActivity()
					.getContentResolver()
					.update(ContentDescriptor.RecentHistoriesMessage.CONTENT_URI,
							ct,
							ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_FROM_USER_ID
									+ "=? and "
									+ ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_GROUP_TYPE
									+ "=?",
							new String[] { cov.getExtId() + "",
									Conversation.TYPE_CONTACT + "" });
		}

	}

	/**
	 * Remove conversation to list
	 * 
	 * @param id
	 * @param type
	 * @param owner
	 */
	protected void removeConversation(long id, int type) {

		if (Conversation.TYPE_CONTACT == type) {

			mContext.getContentResolver()
					.delete(ContentDescriptor.RecentHistoriesMessage.CONTENT_URI,
							ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_FROM_USER_ID
									+ "=? and "
									+ ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_GROUP_TYPE
									+ "=?", new String[] { id + "", type + "" });
		} else if (this.mCurrentTabFlag == Conversation.TYPE_CONFERNECE) {
			Group g = GlobalHolder.getInstance().getGroupById(
					GroupType.CONFERENCE.intValue(), id);
			// If group is null, means we have removed this conversaion
			if (g != null) {
				cb.quitConference(new Conference(id, g.getOwnerUser().getmUserId()), null);
			}
		}

		Conversation cache = null;
		for (int i = 0; i < mConvList.size(); i++) {
			if (mConvList.get(i).getExtId() == id) {
				mItemList.remove(i);
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
		} else {
			content = vm.getAllTextContent();
		}
		Intent resultIntent = new Intent(
				PublicIntent.START_CONVERSACTION_ACTIVITY);

		resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		if (vm.getGroupId() != 0) {
			resultIntent.putExtra("obj", new ConversationNotificationObject(
					Conversation.TYPE_GROUP, vm.getGroupId()));
		} else {
			resultIntent.putExtra("obj", new ConversationNotificationObject(
					Conversation.TYPE_CONTACT, vm.getFromUser().getmUserId()));
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
		public void onItemClick(AdapterView<?> adapter, View v, int pos, long id) {
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
				} else if (cov.getType() == Conversation.TYPE_VERIFICATION_MESSAGE) {

				} else {
					startConversationView(cov);
				}
			}

			// update main activity notificator
			cov.setReadFlag(Conversation.READ_FLAG_READ);
			updateUnreadConversation(cov);
			
			//update voice phone state
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
								//FIXME  ADD RESOURCE mContext.getResources().getString(
								//R.string.conversations_detail) 
						""};
			}

			final Conversation cov = mConvList.get(pos);
			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setTitle(cov.getName()).setItems(item,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							if (which == 0) {
								if (mCurrentTabFlag == Conversation.TYPE_CONFERNECE) {
									Group g = GlobalHolder.getInstance()
											.getGroupById(GroupType.CONFERENCE.intValue(),
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
				if (mCurrentTabFlag == uao.getType()) {
					Message.obtain(mHandler, UPDATE_CONVERSATION, uao)
							.sendToTarget();
				}
				// Update name of creator of conversation
			} else if (JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION
					.equals(intent.getAction())) {
				for (ScrollItem item : mItemList) {
					if ((item.cov.getType() == Conversation.TYPE_CONFERNECE)
							|| item.cov.getType() == Conversation.TYPE_GROUP) {
						if (item.cov instanceof ConferenceConversation) {
							Group g = ((ConferenceConversation) item.cov)
									.getGroup();
							User u = GlobalHolder.getInstance().getUser(
									g.getOwnerUser().getmUserId());
							if (u == null) {
								continue;
							}
							V2Log.e(TAG,
									"group user update :type:"
											+ item.cov.getType() + "-- name:"
											+ u.getName());
							((GroupLayout) item.gp).updateContent(u.getName());
							g.setOwnerUser(u);
						}
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
					if(remoteID == -1l){
						Log.e(TAG, "get remoteID is -1 ... update failed!!");
						return ;
					}
					
					String selections = ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_REMOTE_USER_ID
							+ "= ? ";
					String[] selectionArgs = new String[] { String.valueOf(remoteID) };
					VideoBean newestMediaMessage = MessageLoader.getNewestMediaMessage(
							mContext, selections, selectionArgs);
					if(newestMediaMessage == null){
						Log.e(TAG, "get newest remoteID "+ remoteID +" --> VideoBean is NULL ... update failed!!");
						return ;
					}
					
					if(voiceLayout == null || voiceMessageItem == null){
						initSpecificItem();
						return ;
					}
					
					if (newestMediaMessage.startDate != 0) {
	
						voiceLayout
								.update(null,
										DateUtil.getStringDate(newestMediaMessage.startDate),
										false);
					}
	
					if (newestMediaMessage.readSatate == AudioVideoMessageBean.STATE_UNREAD)
						updateUnreadVoiceConversation(true);
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
					V2Log.d(TAG, "JNI_BROADCAST_NEW_MESSAGE update..");
					updateConversation(intent.getExtras().getLong("mid"));
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
			if (JNIService.JNI_BROADCAST_NEW_MESSAGE.equals(intent.getAction())) {
				boolean groupMessage = intent.getBooleanExtra("gm", false);
				if (!groupMessage) {
					V2Log.e(TAG, "JNI_BROADCAST_NEW_MESSAGE update..");
					updateConversation(intent.getExtras().getLong("mid"));
				}
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
					if (gl != null && gl.size() > 0 && !isLoadedCov) {
						populateConversation(gl);
					}
				} else if (mCurrentTabFlag == Conversation.TYPE_GROUP) {
					List<Group> gl = GlobalHolder.getInstance().getGroup(
							Group.GroupType.CHATING.intValue());
					if (gl != null && gl.size() > 0 && !isLoadedCov) {
						populateConversation(gl);
					}
				}
				if (mCurrentTabFlag == Conversation.TYPE_CONTACT) {
					if (!isLoadedCov) {
						loadConversation();
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
				updateConversation = true;
				updateConversation(uno.getType(), uno.getExtId());
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

	protected void updateUnreadVoiceConversation(boolean b) {
		
		if(voiceLayout == null)
			Log.e(TAG, "update unread voice conversationing , the voiceLayout is null");
		
		voiceLayout.updateNotificator(b);
	}

}
