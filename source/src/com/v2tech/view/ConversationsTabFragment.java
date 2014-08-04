package com.v2tech.view;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

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
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.v2tech.R;
import com.v2tech.db.ContentDescriptor;
import com.v2tech.service.BitmapManager;
import com.v2tech.service.ConferencMessageSyncService;
import com.v2tech.service.ConferenceService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.Registrant;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.service.jni.RequestEnterConfResponse;
import com.v2tech.util.MessageUtil;
import com.v2tech.util.Notificator;
import com.v2tech.util.V2Log;
import com.v2tech.view.bo.ConversationNotificationObject;
import com.v2tech.view.conference.GroupLayout;
import com.v2tech.view.conference.VideoActivityV2;
import com.v2tech.view.conversation.MessageLoader;
import com.v2tech.vo.Conference;
import com.v2tech.vo.ConferenceConversation;
import com.v2tech.vo.ContactConversation;
import com.v2tech.vo.Conversation;
import com.v2tech.vo.CrowdConversation;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.User;
import com.v2tech.vo.VMessage;

public class ConversationsTabFragment extends Fragment implements TextWatcher, ConferenceListener {
	private static final int FILL_CONFS_LIST = 2;
	private static final int UPDATE_USER_SIGN = 8;
	private static final int UPDATE_CONVERSATION = 9;
	private static final int UPDATE_SEARCHED_LIST = 11;
	private static final int REMOVE_CONVERSATION = 12;
	private static final int REQUEST_ENTER_CONF = 14;
	private static final int REQUEST_ENTER_CONF_RESPONSE = 15;

	private static final int CONFERENCE_ENTER_CODE = 100;

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

	private String mCurrentTabFlag;
	
	private int currentPosition;
	private List<ScrollItem> mItemListCache = null;

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

		

		notificationListener = (NotificationListener) getActivity();
		BitmapManager.getInstance().registerBitmapChangedListener(
				this.bitmapChangedListener);

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
	private void initReceiver(String tag) {
		if (mCurrentTabFlag.equals(Conversation.TYPE_CONFERNECE)) {
			receiver = new ConferenceReceiver();
		} else if (mCurrentTabFlag.equals(Conversation.TYPE_GROUP)) {
			receiver = new GroupReceiver();
		} else if (mCurrentTabFlag.equals(Conversation.TYPE_CONTACT)) {
			receiver = new ConversationReceiver();
		}
		if (intentFilter == null) {
			intentFilter = new IntentFilter();
			intentFilter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
			intentFilter.addCategory(PublicIntent.DEFAULT_CATEGORY);
			intentFilter.addAction(JNIService.JNI_BROADCAST_GROUP_NOTIFICATION);
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_USER_UPDATE_NAME_OR_SIGNATURE);
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION);

			if (mCurrentTabFlag.equals(Conversation.TYPE_CONFERNECE)) {
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

			if (mCurrentTabFlag.equals(Conversation.TYPE_GROUP)) {
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
		super.onStop();
	}

	@Override
	public void afterTextChanged(Editable s) {
		if (s != null && s.length() > 0) {
			if (!mIsStartedSearch) {
				mCacheItemList = this.mConvList;
				mIsStartedSearch = true;
			}
		} else {
			
			mItemList.clear();
			mItemList.addAll(mItemListCache);
			mItemListCache = null;
			if (mIsStartedSearch) {
				mConvList = mCacheItemList;
				adapter.notifyDataSetChanged();
				mIsStartedSearch = false;
			}
			return;
		}
		List<Conversation> newItemList = new ArrayList<Conversation>();
		String searchKey = s == null ? "" : s.toString();
		for (int i = 0; mCacheItemList != null && i < mCacheItemList.size(); i++) {
			Conversation cov = mCacheItemList.get(i);
			if (cov.getName() != null
					&& cov.getName().contains(searchKey)) {
				newItemList.add(cov);
			} else if (cov.getMsg() != null
					&& cov.getMsg().toString().contains(searchKey)) {
				newItemList.add(cov);
			}
		}
		mConvList = newItemList;
		fillAdapter(mConvList);
//		adapter.notifyDataSetChanged();
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {

		if(mItemListCache == null){
			mItemListCache = new CopyOnWriteArrayList<ScrollItem>(); 
			mItemListCache.addAll(mItemList);
			mItemList.clear();
		}
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {

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
	}

	private void populateConversation(List<Group> list) {
		isLoadedCov = true;
		if (list == null || list.size() <= 0) {
			V2Log.w(" group list is null");
			return;
		}

		for (final Group g : list) {
			if (g.getOwnerUser() == null) {
				g.setOwnerUser(GlobalHolder.getInstance().getUser(g.getOwner()));
			}
			Conversation cov = new ConferenceConversation(g);
			//Update all initial conversation to read
			cov.setReadFlag(Conversation.READ_FLAG_READ);
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
		Conversation cov = new ConferenceConversation(g);
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
		
		//Update unread conversation list
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
		Cursor mCur = getActivity().getContentResolver().query(
				ContentDescriptor.Conversation.CONTENT_URI,
				ContentDescriptor.Conversation.Cols.ALL_CLOS,
				ContentDescriptor.Conversation.Cols.TYPE + "=? and "
						+ ContentDescriptor.Conversation.Cols.OWNER + "=?",
				new String[] { Conversation.TYPE_CONTACT,
						GlobalHolder.getInstance().getCurrentUserId() + "" },
				null);

		while (mCur.moveToNext()) {
			Conversation cov = extractConversation(mCur);
			mConvList.add(cov);
		}
		mCur.close();
		isLoadedCov = true;

		fillAdapter(mConvList);
	}

	private Conversation extractConversation(Cursor cur) {
		long extId = cur.getLong(2);
		String name = cur.getString(3);
		int flag = cur.getInt(4);
		User u = GlobalHolder.getInstance().getUser(extId);
		if (u == null) {
			u = new User(extId);
			u.setName(name);
		}
		Conversation cov = new ContactConversation(u);
		cov.setReadFlag(flag);
		return cov;
	}

	private void fillAdapter(List<Conversation> list) {
		for (int i = 0; i < list.size(); i++) {
			Conversation cov = list.get(i);
			GroupLayout gp = new GroupLayout(mContext, cov);
			if (cov.getReadFlag() == Conversation.READ_FLAG_UNREAD) {
				gp.updateNotificator(true);
				updateUnreadConversation(cov);
			}
			mItemList.add(new ScrollItem(cov, gp));
		}

		
		adapter.notifyDataSetChanged();
	}

	private void updateConversationToDB(long extId, String name) {
		ContentValues ct = new ContentValues();
		ct.put(ContentDescriptor.Conversation.Cols.EXT_NAME, name);
		getActivity().getContentResolver().update(
				ContentDescriptor.Conversation.CONTENT_URI,
				ct,
				ContentDescriptor.Conversation.Cols.EXT_ID + "=? and "
						+ ContentDescriptor.Conversation.Cols.TYPE + "=?",
				new String[] { extId + "", Conversation.TYPE_CONTACT });
	}

	

	private void updateConversation(long extId, String type) {
		if (!isLoadedCov) {
			this.loadConversation();
		}
		Conversation existedCov = null;
		GroupLayout viewLayout = null;
		boolean foundFlag = false;
		for (int i = 0; i < this.mConvList.size(); i++) {
			Conversation cov = this.mConvList.get(i);
			if (cov.getExtId() == extId) {
				foundFlag = true;
				existedCov = cov;
				viewLayout = (GroupLayout)this.mItemList.get(i).gp;
				break;
			}
		}
		
		
		if (foundFlag) {
			VMessage vm = MessageLoader.getNewestMessage(mContext, GlobalHolder
					.getInstance().getCurrentUserId(), existedCov.getExtId());
			existedCov.setMsg(MessageUtil.getMixedConversationContent(mContext, vm));
			existedCov.setReadFlag(Conversation.READ_FLAG_READ);
			//Update view
			viewLayout.update();
			viewLayout.updateNotificator(false);
			//Update unread list
			updateUnreadConversation(existedCov);
		} else {
			
			if (type.equals(Conversation.TYPE_CONTACT)) {
				existedCov = new ContactConversation(GlobalHolder.getInstance()
						.getUser(extId));
				User fromUser = GlobalHolder.getInstance().getUser(extId);
				ContentValues conCv = new ContentValues();
				conCv.put(ContentDescriptor.Conversation.Cols.EXT_ID, extId);
				conCv.put(ContentDescriptor.Conversation.Cols.TYPE,
						Conversation.TYPE_CONTACT);
				conCv.put(ContentDescriptor.Conversation.Cols.EXT_NAME,
						fromUser == null ? "" : fromUser.getName());
				conCv.put(ContentDescriptor.Conversation.Cols.OWNER,
						GlobalHolder.getInstance().getCurrentUserId());
				conCv.put(ContentDescriptor.Conversation.Cols.NOTI_FLAG, 0);
				mContext.getContentResolver().insert(
						ContentDescriptor.Conversation.CONTENT_URI, conCv);
				
				this.mConvList.add(existedCov);
				viewLayout = new GroupLayout(mContext, existedCov);
				mItemList.add(new ScrollItem(existedCov, viewLayout));
				adapter.notifyDataSetChanged();
			}

		}
	}

	/**
	 * Update conversation according to message id This request call only from
	 * new message broadcast
	 * 
	 * @param msgId
	 */
	private void updateConversation(long msgId) {
		if (!isLoadedCov) {
			this.loadConversation();
		}
		VMessage vm = MessageLoader.loadMessageById(mContext, msgId);
		if (vm == null) {
			V2Log.e("Didn't find message " + msgId);
			return;
		}
		// Update status bar
		updateStatusBar(vm);

		long extId = 0;
		if (vm.getGroupId() != 0) {
			extId = vm.getGroupId();
		} else {
			extId = vm.getFromUser().getmUserId();
		}

		Conversation existedCov = null;
		GroupLayout viewLayout = null;
		boolean foundFlag = false;
		for (int i = 0; i < this.mConvList.size(); i++) {
			Conversation cov = this.mConvList.get(i);
			if (cov.getExtId() == extId) {
				foundFlag = true;
				existedCov = cov;
				viewLayout = (GroupLayout)this.mItemList.get(i).gp;
				break;
			}
		}
		

		if (!foundFlag) {
			if (mCurrentTabFlag.equals(Conversation.TYPE_CONTACT)) {
				existedCov = new ContactConversation(GlobalHolder.getInstance()
						.getUser(extId));
			} else if (mCurrentTabFlag.equals(Conversation.TYPE_GROUP)) {
				existedCov = new CrowdConversation(GlobalHolder.getInstance()
						.getGroupById(GroupType.CHATING, extId));
			}
			if (mCurrentTabFlag.equals(Conversation.TYPE_CONTACT)) {
				User fromUser = GlobalHolder.getInstance().getUser(extId);
				ContentValues conCv = new ContentValues();
				conCv.put(ContentDescriptor.Conversation.Cols.EXT_ID, extId);
				conCv.put(ContentDescriptor.Conversation.Cols.TYPE,
						Conversation.TYPE_CONTACT);
				conCv.put(ContentDescriptor.Conversation.Cols.EXT_NAME,
						fromUser == null ? "" : fromUser.getName());
				conCv.put(ContentDescriptor.Conversation.Cols.OWNER,
						GlobalHolder.getInstance().getCurrentUserId());
				conCv.put(ContentDescriptor.Conversation.Cols.NOTI_FLAG, 0);
				mContext.getContentResolver().insert(
						ContentDescriptor.Conversation.CONTENT_URI, conCv);
			}

			this.mConvList.add(existedCov);
			viewLayout = new GroupLayout(mContext, existedCov);
			mItemList.add(new ScrollItem(existedCov, viewLayout));
			adapter.notifyDataSetChanged();
		}

		existedCov.setMsg(MessageUtil.getMixedConversationContent(mContext, vm));
		existedCov.setReadFlag(Conversation.READ_FLAG_UNREAD);
		
		//Update view
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
		ct.put(ContentDescriptor.Conversation.Cols.NOTI_FLAG, ret);
		
		//If flag is true, means we updated unread list, then we need to update database
		if (flag) {
			getActivity().getContentResolver().update(
					ContentDescriptor.Conversation.CONTENT_URI,
					ct,
					ContentDescriptor.Conversation.Cols.EXT_ID + "=? and "
							+ ContentDescriptor.Conversation.Cols.TYPE + "=?",
					new String[] { cov.getExtId() + "", Conversation.TYPE_CONTACT });
		}

	}

	/**
	 * Remove conversation to list
	 * 
	 * @param id
	 * @param type
	 * @param owner
	 */
	private void removeConversation(long id, String type) {

		if (Conversation.TYPE_CONTACT.equals(type)) {

			mContext.getContentResolver().delete(
					ContentDescriptor.Conversation.CONTENT_URI,
					ContentDescriptor.Conversation.Cols.EXT_ID + "=? and "
							+ ContentDescriptor.Conversation.Cols.TYPE + "=?",
					new String[] { id + "", type });
		} else if (this.mCurrentTabFlag.equals(Conversation.TYPE_CONFERNECE)) {
			Group g = GlobalHolder.getInstance().getGroupById(GroupType.CONFERENCE, id);
			//If group is null, means we have removed this conversaion
			if (g != null) {
				cb.quitConference(new Conference(id, g.getOwner()), null);
			}
		}

		Conversation cache = null;
		// TODO if current tag is group, should send request to quit group
		for (int i = 0; i < mConvList.size(); i++) {
			if (mConvList.get(i).getExtId() == id) {
				mItemList.remove(i);
				cache = mConvList.remove(i);
				break;
			}
		}

		adapter.notifyDataSetChanged();
		if (cache != null) {
			//Set removed conversation state to readed
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
					Conversation.TYPE_GROUP, vm.getFromUser().getmUserId()));
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
		//Set current state to in meeting state
		GlobalHolder.getInstance().setMeetingState(true);
		Intent enterConference = new Intent(mContext, VideoActivityV2.class);
		enterConference.putExtra("conf", conf);
		currentEntered = conf;
		this.startActivityForResult(enterConference, CONFERENCE_ENTER_CODE);
	}
	
	
	
	
	/*
	 * This request from main activity
	 * @see com.v2tech.view.ConferenceListener#requestJoinConf(com.v2tech.vo.Conference)
	 */
	@Override
	public boolean requestJoinConf(Conference conf) {
		if (conf == null) {
			return false;
		}
		requestJoinConf(conf.getId());
		
		// This request from main activity
		// We need to update notificator for conversation
		for (int i =0; i< mItemList.size(); i++) {
			// hiden notificator
			((GroupLayout) mItemList.get(i).gp).updateNotificator(false);
			Conversation cov = mConvList.get(i);
			//update main activity notificator
			cov.setReadFlag(Conversation.READ_FLAG_READ);
			updateUnreadConversation(cov);
		}
		
		
		
		return true;
	}

	private void updateConferenceNotification(Conference conf) {
		Intent enterConference = new Intent(mContext,
				MainActivity.class);
		
		User creator = GlobalHolder.getInstance().getUser(conf.getCreator());
		enterConference.putExtra("conf", conf);
		Notificator.updateSystemNotification(mContext,conf.getName()
				+ " 会议邀请:", creator == null?"":creator.getName(), 1, enterConference,
				PublicIntent.VIDEO_NOTIFICATION_ID);

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

		@Override
		public void onItemClick(AdapterView<?> adapter, View v, int pos, long id) {
			// hiden notificator
			((GroupLayout) mItemList.get(pos).gp).updateNotificator(false);
			Conversation cov = mConvList.get(pos);
			if (mCurrentTabFlag == Conversation.TYPE_CONFERNECE) {
				requestJoinConf(cov.getExtId());
			} else if (mCurrentTabFlag == Conversation.TYPE_GROUP
					|| mCurrentTabFlag == Conversation.TYPE_CONTACT) {
				Intent i = new Intent(PublicIntent.START_CONVERSACTION_ACTIVITY);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				i.putExtra("obj",
						new ConversationNotificationObject(cov));
				startActivity(i);
			}
			
			//update main activity notificator
			cov.setReadFlag(Conversation.READ_FLAG_READ);
			updateUnreadConversation(cov);

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
			} else {
				item = new String[] { mContext.getResources().getString(
						R.string.conversations_delete_conversaion) };
			}

			final Conversation cov = mConvList.get(pos);
			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setTitle(cov.getName()).setItems(item,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							removeConversation(cov.getExtId(), cov.getType());
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
				if (Conversation.TYPE_CONTACT.equals(item.cov.getType())
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
			return mConvList == null ? 0 : mConvList.size();
		}

		@Override
		public Object getItem(int position) {
			return mConvList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return mConvList.get(position).getExtId();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			currentPosition = position;
			return mItemList.get(position).gp;
//			return mConvList.get(position).gp;
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
			//From this broadcast, user has already read conversation
			} else if (PublicIntent.REQUEST_UPDATE_CONVERSATION.equals(intent
					.getAction())) {
				ConversationNotificationObject uao = (ConversationNotificationObject)intent.getExtras().get("obj");
				if (mCurrentTabFlag.equals(uao.getType())) {
					Message.obtain(mHandler, UPDATE_CONVERSATION,
							uao).sendToTarget();
				}

			} else if (JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION
					.equals(intent.getAction())) {
				// FIXME crash
				for (ScrollItem item : mItemList) {
					if (item.cov.getType().equals(Conversation.TYPE_CONFERNECE)) {
						Group g = ((ConferenceConversation) item.cov)
								.getGroup();
						User u = GlobalHolder.getInstance().getUser(
								g.getOwner());
						if (u == null) {
							continue;
						}
						((GroupLayout) item.gp).updateContent(u.getName());
						g.setOwnerUser(u);
					} else if (item.cov.getType().equals(
							Conversation.TYPE_CONTACT)) {
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
						GroupType.CHATING, gid);
				if (g != null) {
					addConversation(g, false);
				} else {
					V2Log.e("Can not get crowd :" + gid);
				}
			} else if (JNIService.JNI_BROADCAST_NEW_MESSAGE.equals(intent
					.getAction())) {
				boolean groupMessage = intent.getBooleanExtra("gm", false);
				if (groupMessage) {
					updateConversation(intent.getExtras().getLong("mid"));
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
				Group g = GlobalHolder.getInstance().getGroupById(
						GroupType.CONFERENCE, gid);
				if (g != null) {
					g.setOwnerUser(GlobalHolder.getInstance().getUser(
							g.getOwner()));
					addConversation(g, true);

					Conference c = new Conference(gid);
					c.setCreator(g.getOwner());
					c.setChairman(g.getOwner());
					c.setName(g.getName());
					
					//Notify status bar
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
						GroupType.CONFERENCE, intent.getLongExtra("newGid", 0));
				// Add conference to conversation list
				addConversation(conf, false);
				Conference c = new Conference(conf.getmGId());
				c.setCreator(conf.getOwner());
				c.setChairman(conf.getOwner());
				c.setName(conf.getName());
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
				if (mCurrentTabFlag.equals(Conversation.TYPE_CONFERNECE)) {
					List<Group> gl = GlobalHolder.getInstance().getGroup(
							Group.GroupType.CONFERENCE);
					if (gl != null && gl.size() > 0 && !isLoadedCov) {
						populateConversation(gl);
					}
				} else if (mCurrentTabFlag.equals(Conversation.TYPE_GROUP)) {
					List<Group> gl = GlobalHolder.getInstance().getGroup(
							Group.GroupType.CHATING);
					if (gl != null && gl.size() > 0 && !isLoadedCov) {
						populateConversation(gl);
					}
				}
				if (mCurrentTabFlag.equals(Conversation.TYPE_CONTACT)) {
					if (!isLoadedCov) {
						loadConversation();
					}
				}

				break;

			case UPDATE_USER_SIGN:
				long fromuidS = (Long) msg.obj;
				for (ScrollItem item : mItemList) {
					((GroupLayout) item.gp).update();
					if (item.cov.getExtId() == fromuidS
							&& Conversation.TYPE_CONTACT.equals(item.cov
									.getType())) {
						User u = GlobalHolder.getInstance().getUser(fromuidS);
						updateConversationToDB(fromuidS,
								u == null ? "" : u.getName());
						break;
					}
				}
				break;
			case UPDATE_CONVERSATION:
				ConversationNotificationObject uno = (ConversationNotificationObject) msg.obj;
				updateConversation(uno.getExtId(), uno.getType());
				break;
			case UPDATE_SEARCHED_LIST:
				break;
			case REMOVE_CONVERSATION:
				break;
			case REQUEST_ENTER_CONF:
				mContext.startService(new Intent(mContext, ConferencMessageSyncService.class));
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
					mContext.stopService(new Intent(mContext, ConferencMessageSyncService.class));
				} else {
					mContext.stopService(new Intent(mContext, ConferencMessageSyncService.class));
					Toast.makeText(mContext,
							R.string.error_request_enter_conference,
							Toast.LENGTH_SHORT).show();
				}

				break;
			}
		}
	}

}
