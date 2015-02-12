package com.bizcom.vc.activity.main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.V2.jni.ImRequest;
import com.V2.jni.util.V2Log;
import com.bizcom.db.provider.ConversationProvider;
import com.bizcom.request.ConferencMessageSyncService;
import com.bizcom.request.ConferenceService;
import com.bizcom.request.MessageListener;
import com.bizcom.request.jni.JNIResponse;
import com.bizcom.request.jni.RequestEnterConfResponse;
import com.bizcom.util.DateUtil;
import com.bizcom.util.Notificator;
import com.bizcom.util.SearchUtils;
import com.bizcom.util.SearchUtils.ScrollItem;
import com.bizcom.vc.activity.conference.ConferenceActivity;
import com.bizcom.vc.activity.conference.GroupLayout;
import com.bizcom.vc.application.GlobalConfig;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.application.MainApplication;
import com.bizcom.vc.application.PublicIntent;
import com.bizcom.vc.application.V2GlobalConstants;
import com.bizcom.vc.listener.ConferenceListener;
import com.bizcom.vc.listener.NotificationListener;
import com.bizcom.vc.service.JNIService;
import com.bizcom.vo.Conference;
import com.bizcom.vo.ConferenceConversation;
import com.bizcom.vo.ConferenceGroup;
import com.bizcom.vo.Conversation;
import com.bizcom.vo.CrowdConversation;
import com.bizcom.vo.DepartmentConversation;
import com.bizcom.vo.DiscussionConversation;
import com.bizcom.vo.Group;
import com.bizcom.vo.Group.GroupType;
import com.bizcom.vo.User;
import com.v2tech.R;

public class TabFragmentConference extends Fragment implements TextWatcher,
		ConferenceListener {
	private static final String TAG = "TabFragmentConference";
	private static final int FILL_CONFS_LIST = 0x0001;
	private static final int REQUEST_ENTER_CONF = 0x0002;
	private static final int REQUEST_ENTER_CONF_RESPONSE = 0x0003;
	public static final int CONFERENCE_ENTER_CODE = 100;

	private View rootView;
	private View subTabLayout;
	private ListView mConferenceListView;
	private ProgressDialog mWaitingDialog;
	private OnItemClickListener mConferenceListViewOnItemClickListener = new ConferenceListViewOnItemClickListener();
	private OnItemLongClickListener mConferenceListViewOnItemLongClickListener = new ConferenceListViewOnItemLongClickListener();
	private LocalHandler mHandler = new LocalHandler();
	private BroadcastReceiver receiver;
	private ConferenceListViewAdapter adapter = new ConferenceListViewAdapter();

	private Context mContext;
	private NotificationListener notificationListener;
	private IntentFilter intentFilter;
	private ConferenceService mConferenceService;
	private Set<Conversation> mUnreadConvList = new HashSet<Conversation>();;
	private List<ScrollItem> mItemList = new ArrayList<ScrollItem>();
	private List<ScrollItem> searchList = new ArrayList<ScrollItem>();
	private List<Long> offLineConf = new ArrayList<Long>();
	private List<CovCache> covCacheList = new ArrayList<TabFragmentConference.CovCache>();
	private Conversation currentClickConversation;
	private Conference currentEntered;
	private ExecutorService service;
	private MediaPlayer mChatPlayer;
	private Object mLock = new Object();

	// This tag is used to limit the database load times
	private boolean isLoadedCov;
	private boolean mIsStartedSearch;
	// Use to mark which conference user entered..
	private boolean isFrist = true;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		V2Log.d(TAG, "ConversationsTabFragment onCreate...");
		mConferenceService = new ConferenceService();
		mContext = getActivity();
		service = Executors.newCachedThreadPool();
		initReceiver();
		notificationListener = (NotificationListener) getActivity();
		Message.obtain(mHandler, FILL_CONFS_LIST).sendToTarget();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (rootView == null) {
			rootView = inflater.inflate(R.layout.tab_fragment_conversations,
					container, false);
			mConferenceListView = (ListView) rootView
					.findViewById(R.id.conversations_list_container);
			mConferenceListView.setAdapter(adapter);

			mConferenceListView
					.setOnItemClickListener(mConferenceListViewOnItemClickListener);
			mConferenceListView
					.setOnItemLongClickListener(mConferenceListViewOnItemLongClickListener);
		}
		return rootView;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getActivity().unregisterReceiver(receiver);
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
		receiver = new ConferenceReceiver();
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
					.addAction(JNIService.JNI_BROADCAST_USER_UPDATE_BASE_INFO);

			intentFilter
					.addAction(JNIService.JNI_BROADCAST_CONFERENCE_INVATITION);
			intentFilter.addAction(JNIService.JNI_BROADCAST_CONFERENCE_REMOVED);
			intentFilter
					.addAction(PublicIntent.BROADCAST_NEW_CONFERENCE_NOTIFICATION);

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

		if (resultCode == CONFERENCE_ENTER_CODE) {
			if (currentEntered == null)
				return;
			V2Log.d(TAG, "The Conf was exit ! current enter conf is : "
					+ currentEntered.getName());
			mConferenceService.requestExitConference(currentEntered, null);
			currentEntered = null;
			mContext.stopService(new Intent(mContext,
					ConferencMessageSyncService.class));
		}

	}

	public void updateSearchState() {

		mIsStartedSearch = false;
		searchList.clear();
		adapter.notifyDataSetChanged();
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

		if (g == null) {
			V2Log.e(TAG,
					"addConversation --> Add new conversation failed ! Given Group is null");
			return;
		}

		Conversation cov;
		ScrollItem currentItem = null;
		if (g.getGroupType() == GroupType.CONFERENCE) {
			cov = new ConferenceConversation(g);
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

		// Update unread conversation list
		updateUnreadConversation(currentItem);
		adapter.notifyDataSetChanged();
		scrollToTop();
	}
	
	private void scrollToTop(){
		mConferenceListView.post(new Runnable() {

			@Override
			public void run() {
				mConferenceListView.setSelection(0);
			}
		});
	}

	private void fillAdapter(List<Conversation> list) {
		for (int i = 0; i < list.size(); i++) {
			Conversation cov = list.get(i);
			if (cov == null) {
				V2Log.e(TAG,
						"when fillAdapter , get null Conversation , index :"
								+ i);
				continue;
			}

			GroupLayout layout = new GroupLayout(mContext, cov);
			if (cov.getType() == V2GlobalConstants.GROUP_TYPE_CONFERENCE) {
				Group fillGroup = ((ConferenceConversation) cov).getGroup();
				if (fillGroup != null)
					layout.updateGroupContent(fillGroup);
			}

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
			}
		});
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
			notificationListener.updateNotificator(
					Conversation.TYPE_CONFERNECE, true);
		} else {
			notificationListener.updateNotificator(
					Conversation.TYPE_CONFERNECE, false);
		}
		// update conversation date and flag to database
		ConversationProvider.updateConversationToDatabase(cov, ret);
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
				// clear all system notification
				Notificator.cancelSystemNotification(getActivity(),
						PublicIntent.MESSAGE_NOTIFICATION_ID);
				break;
			}
		}
		sortAndUpdate();
	}

	private void updateConferenceNotification(Conference conf) {
		if (checkSendingState()) {
			return;
		}

		if (!((MainApplication) mContext.getApplicationContext())
				.isRunningBackgound()) {
			sendVoiceNotify();
			return;
		}

		Intent enterConference = new Intent(mContext, MainActivity.class);
		User creator = GlobalHolder.getInstance().getUser(conf.getCreator());
		enterConference.putExtra("conf", conf);
		enterConference.putExtra("initFragment", 3);
		Notificator.updateSystemNotification(mContext, creator == null ? ""
				: creator.getName(), mContext.getString(R.string.conversation_attend_the_meeting)
				+ conf.getName(), 1, enterConference,
				PublicIntent.VIDEO_NOTIFICATION_ID);
	}

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
			if (((MainApplication) mContext.getApplicationContext())
					.isRunningBackgound()) {
				sendVoiceNotify();
			}
			return true;
		}
		return false;
	}

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

	private void initUpdateCovGroupList(final int groupType, final long groupID) {
		if (groupType == V2GlobalConstants.GROUP_TYPE_DEPARTMENT) {
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
							User owner = GlobalHolder.getInstance().getUser(
									newGroup.getOwnerUser().getmUserId());
							if (TextUtils.isEmpty(owner.getName())) {
								Log.i("20150203 1", "7");
								ImRequest.getInstance().getUserBaseInfo(
										owner.getmUserId());
							} else {
								newGroup.setOwnerUser(owner);
								currentConversation.setG(newGroup);
								updateCovContent(currentGroupLayout, newGroup,
										true);
								V2Log.d(TAG,
										"update conference converstaion over , cov name is : "
												+ currentConversation.getName()
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

	private void updateCovContent(final GroupLayout currentGroupLayout,
			final Group newGroup, final boolean isConference) {
		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (isConference) {
					if (offLineConf.contains(newGroup.getmGId())) {
						currentGroupLayout.updateConversationNotificator(true);
					}
				} else {
					currentGroupLayout.update();
				}
				currentGroupLayout.updateGroupContent(newGroup);
			}
		});
	}

	private void sortAndUpdate() {
		Collections.sort(mItemList);
		adapter.notifyDataSetChanged();
	}

	/**
	 * This request from main activity
	 * 
	 * @see com.bizcom.vc.listener.ConferenceListener#requestJoinConf(com.bizcom.vo.Conference
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

	class CovCache {

		public int groupType;
		public long groupId;

		public CovCache(int groupType, long groupId) {
			this.groupType = groupType;
			this.groupId = groupId;
		}
	}

	private class ConferenceListViewOnItemClickListener implements
			OnItemClickListener {

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

			if (offLineConf.contains(cov.getExtId()))
				offLineConf.remove(cov.getExtId());
			requestJoinConf(cov.getExtId());

			// update main activity notificator
			cov.setReadFlag(Conversation.READ_FLAG_READ);
			updateUnreadConversation(item);
		}
	};

	private class ConferenceListViewOnItemLongClickListener implements
			OnItemLongClickListener {

		private DeleteConferenceOnClickListener mDeleteConferenceOnClickListener = new DeleteConferenceOnClickListener();

		@Override
		public boolean onItemLongClick(AdapterView<?> adapter, View v, int pos,
				long id) {
			String[] item;
			currentClickConversation = mItemList.get(pos).cov;

			item = new String[] { mContext.getResources().getString(
					R.string.conversations_delete_conf) };

			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setTitle(mItemList.get(pos).cov.getName()).setItems(item,
					mDeleteConferenceOnClickListener);
			AlertDialog ad = builder.create();
			ad.show();

			return true;
		}

		private class DeleteConferenceOnClickListener implements
				DialogInterface.OnClickListener {
			public void onClick(DialogInterface dialog, int which) {
				if (!GlobalHolder.getInstance().isServerConnected()) {
					Toast.makeText(mContext,
							R.string.error_local_connect_to_server,
							Toast.LENGTH_SHORT).show();
					dialog.dismiss();
					return;
				}
				Group g = GlobalHolder.getInstance().getGroupById(
						GroupType.CONFERENCE.intValue(),
						currentClickConversation.getExtId());
				// If group is null, means we have
				// removed
				// this conversaion
				if (g != null) {
					mConferenceService.quitConference(new Conference(
							currentClickConversation.getExtId(), g
									.getOwnerUser().getmUserId()), null);
				}
				dialog.dismiss();
			}
		}

	};

	class ConferenceListViewAdapter extends BaseAdapter {

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
				updateUnreadConversation(scrollItem);
				return mItemList.get(position).gp;
			}
		}

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

		fillAdapter(tempList);
	}

	class LocalHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case FILL_CONFS_LIST:
				List<Group> gl = GlobalHolder.getInstance().getGroup(
						Group.GroupType.CONFERENCE.intValue());
				if (gl != null && gl.size() > 0 && !isLoadedCov)
					populateConversation(GroupType.CONFERENCE, gl);

				break;
			case REQUEST_ENTER_CONF:
				mContext.startService(new Intent(mContext,
						ConferencMessageSyncService.class));
				mConferenceService.requestEnterConference(new Conference(
						(Long) msg.obj), new MessageListener(this,
						REQUEST_ENTER_CONF_RESPONSE, null));
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
			}
		}
	}

	class ConferenceReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (JNIService.JNI_BROADCAST_GROUP_NOTIFICATION.equals(intent
					.getAction())) {
				int type = intent.getExtras().getInt("gtype");
				if ((type == GroupType.CONFERENCE.intValue())) {
					Message.obtain(mHandler, FILL_CONFS_LIST).sendToTarget();
				}
				// From this broadcast, user has already read conversation
			} else if (JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION
					.equals(intent.getAction())) {
				int groupType = intent.getIntExtra("gtype", -1);
				long groupID = intent.getLongExtra("gid", -1);
				if (isLoadedCov) {
					initUpdateCovGroupList(groupType, groupID);
				} else {
					covCacheList.add(new CovCache(groupType, groupID));
				}
			} else if (JNIService.JNI_BROADCAST_GROUPS_LOADED.equals(intent
					.getAction())) {
				// 刷新群组列表
				for (int i = 0; i < covCacheList.size(); i++) {
					CovCache covCache = covCacheList.get(i);
					V2Log.e(TAG, "The Group Need Fresh over! type is : "
							+ covCache.groupType + " id is : "
							+ covCache.groupId);
					initUpdateCovGroupList(covCache.groupType, covCache.groupId);
				}
			} else if (JNIService.JNI_BROADCAST_CONFERENCE_INVATITION
					.equals(intent.getAction())) {
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
					offLineConf.add(gid);
				}

				if (existPos != -1) {
					ScrollItem exist = mItemList.get(existPos);
					exist.cov.setReadFlag(Conversation.READ_FLAG_UNREAD);
					updateUnreadConversation(exist);
				} else {
					addConversation(g, true);
				}
				Conference c = new Conference((ConferenceGroup) g);
				// Notify status bar
				updateConferenceNotification(c);
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
}
