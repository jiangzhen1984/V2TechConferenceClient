package com.bizcom.vc.activity.main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.V2.jni.util.V2Log;
import com.bizcom.bo.ConversationNotificationObject;
import com.bizcom.bo.GroupUserObject;
import com.bizcom.request.V2CrowdGroupRequest;
import com.bizcom.request.util.HandlerWrap;
import com.bizcom.util.DateUtil;
import com.bizcom.util.Notificator;
import com.bizcom.util.SearchUtils;
import com.bizcom.util.SearchUtils.ScrollItem;
import com.bizcom.vc.activity.conference.GroupLayout;
import com.bizcom.vc.application.GlobalConfig;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.application.PublicIntent;
import com.bizcom.vc.application.V2GlobalConstants;
import com.bizcom.vc.service.JNIService;
import com.bizcom.vo.Conversation;
import com.bizcom.vo.CrowdConversation;
import com.bizcom.vo.CrowdGroup;
import com.bizcom.vo.DiscussionConversation;
import com.bizcom.vo.DiscussionGroup;
import com.bizcom.vo.Group;
import com.bizcom.vo.Group.GroupType;
import com.v2tech.R;

public class TabFragmentCrowd extends Fragment implements TextWatcher {
	private static final String TAG = "TabFragmentCrow";

	private static final int FILL_CONFS_LIST = 0x001;
	private static final int QUIT_DISCUSSION_BOARD_DONE = 0x002;

	private View rootView;
	private Context mContext;

	private BroadcastReceiver receiver;
	private IntentFilter intentFilter;

	private V2CrowdGroupRequest chatService;

	private List<ScrollItem> mItemList = new ArrayList<ScrollItem>();
	private List<ScrollItem> searchList = new ArrayList<ScrollItem>();
	private List<CovCache> covCacheList = new ArrayList<TabFragmentCrowd.CovCache>();

	private LocalHandler mHandler = new LocalHandler();

	/**
	 * This tag is used to limit the database load times
	 */
	private boolean isLoadedCov;

	private boolean mIsStartedSearch;
	private ListView mConversationsListView;
	private ConversationsAdapter adapter = new ConversationsAdapter();

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

	private ExecutorService service;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i("20150303 1","TabFragmentCrow onCreate()");

		mContext = getActivity();
		service = Executors.newCachedThreadPool();
		initReceiver();

		chatService = new V2CrowdGroupRequest();
		Message.obtain(mHandler, FILL_CONFS_LIST).sendToTarget();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.i("20150303 1","TabFragmentCrow onCreateView()");
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
								populateConversation(GlobalHolder
										.getInstance()
										.getGroup(
												GroupType.DISCUSSION.intValue()));
							} else if (checkedId == R.id.rb_crowd) {
								mCurrentSubTab = SUB_TAB_CROWD;
								populateConversation(GlobalHolder.getInstance()
										.getGroup(GroupType.CHATING.intValue()));
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
		Log.i("20150303 1","TabFragmentCrow onDestroy()");
		super.onDestroy();
		getActivity().unregisterReceiver(receiver);
		mItemList = null;
	}

	@Override
	public void onDestroyView() {
		Log.i("20150303 1","TabFragmentCrow onDestroyView()");
		super.onDestroyView();
		((ViewGroup) rootView.getParent()).removeView(rootView);
	}
	
	@Override
	public void onStop() {
		Log.i("20150303 1","TabFragmentCrow onStop()");
		super.onStop();
	}
	
	@Override
	public void onStart() {
		Log.i("20150303 1","TabFragmentCrow onStart()");
		super.onStart();
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == V2GlobalConstants.REQUEST_CONVERSATION_TEXT_RETURN && data != null){
			int groupType = data.getIntExtra("groupType", -1);
			long groupID = data.getLongExtra("groupID", -1);
			Intent intent = new Intent(PublicIntent.REQUEST_UPDATE_CONVERSATION);
			intent.addCategory(PublicIntent.DEFAULT_CATEGORY);
			ConversationNotificationObject obj = new ConversationNotificationObject(groupType,
						groupID , false);
			intent.putExtra("fromCrowdTab", true);
			intent.putExtra("obj", obj);
			mContext.sendBroadcast(intent);
		}
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
			intentFilter.addAction(PublicIntent.BROADCAST_USER_COMMENT_NAME_NOTIFICATION);
			intentFilter.addAction(JNIService.JNI_BROADCAST_GROUPS_LOADED);
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

	public void updateSearchState() {

		mIsStartedSearch = false;
		searchList.clear();
		adapter.notifyDataSetChanged();
	}

	/**
	 * According populateType to fill the List Data. The data from server!
	 * 
	 * @param populateType
	 * @param list
	 */
	private void populateConversation(List<Group> list) {
		mItemList.clear();
		adapter.notifyDataSetChanged();

		if (list == null || list.size() <= 0) {
			V2Log.e("TAG",
					"populateConversation --> Given Group List is null ... please checked!");
			return;
		}

		List<Conversation> tempList = new ArrayList<Conversation>();
		for (int i = list.size() - 1; i >= 0; i--) {
			Group g = list.get(i);
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
		fillAdapter(tempList);
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
			// 需要调用updateGroupContent
			if (cov.getType() == V2GlobalConstants.GROUP_TYPE_CROWD) {
				Group fillGroup = ((CrowdConversation) cov).getGroup();
				if (fillGroup != null)
					layout.updateGroupContent(fillGroup);
			}

			ScrollItem newItem = new ScrollItem(cov, layout);
			mItemList.add(newItem);
		}
		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				adapter.notifyDataSetChanged();
				isLoadedCov = true;
				V2Log.w(TAG,
						"The ListView already fill over !  , type is CROWD");
			}
		});
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
		if(isAdd)
			scrollToTop();
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
		startActivityForResult(i, V2GlobalConstants.REQUEST_CONVERSATION_TEXT_RETURN);
	}

	/**
	 * Remove conversation from mConvList by id.
	 * 
	 * @param conversationID
	 */
	private void removeConversation(long conversationID) {
		for (int i = 0; i < mItemList.size(); i++) {
			Conversation temp = mItemList.get(i).cov;
			if (temp.getExtId() == conversationID) {
				// remove item
				mItemList.remove(i);
				// clear all system notification
				Notificator.cancelSystemNotification(getActivity(),
						PublicIntent.MESSAGE_NOTIFICATION_ID);
				break;
			}
		}
		sortAndUpdate();
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
								(DiscussionGroup) crowd, new HandlerWrap(
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

	private void initUpdateCovGroupList(final int groupType, final long groupID) {
		if (groupType == V2GlobalConstants.GROUP_TYPE_CROWD
				|| groupType == V2GlobalConstants.GROUP_TYPE_DISCUSSION) {
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
										.getGroupById(Conversation.TYPE_GROUP,
												groupID);
								((CrowdConversation) currentConversation)
										.setGroup(newGroup);
								updateCovContent(currentGroupLayout, newGroup,
										false);
								V2Log.d(TAG,
										"update converstaion over , type is : "
												+ groupType + " and"
												+ " group id : " + groupID
												+ " and name is : "
												+ newGroup.getName());
							} else {
								Group newGroup = GlobalHolder.getInstance()
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
													+ " group id : " + groupID
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
	
	private void scrollToTop(){
		mConversationsListView.post(new Runnable() {

			@Override
			public void run() {
				mConversationsListView.setSelection(0);
			}
		});
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
			currentClickConversation = mItemList.get(pos).cov;
			showPopupWindow(v);
			return true;
		}
	};

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

	class GroupReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (JNIService.JNI_BROADCAST_GROUP_NOTIFICATION.equals(intent
					.getAction())) {
				Message.obtain(mHandler, FILL_CONFS_LIST).sendToTarget();
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
			} else if (PublicIntent.BROADCAST_NEW_CROWD_NOTIFICATION
					.equals(intent.getAction())) {
				GroupUserObject obj = intent.getParcelableExtra("group");
				if (obj == null) {
					V2Log.e(TAG,
							"Received the broadcast to quit the crowd group , but crowd id is wroing... ");
					return;
				} else {
					V2Log.d(TAG, "Received the new group broadcast !");
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
					removeConversation(obj.getmGroupId());
				}
			} else if (PublicIntent.BROADCAST_DISCUSSION_QUIT_NOTIFICATION
					.equals(intent.getAction())) {
				long cid = intent.getLongExtra("groupId", -1l);
				if (mCurrentSubTab == SUB_TAB_DISCUSSION)
					removeConversation(cid);
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
						populateConversation(chatGroup);
				} else {
					List<Group> discussionGroup = GlobalHolder.getInstance()
							.getGroup(Group.GroupType.DISCUSSION.intValue());
					if (discussionGroup.size() > 0 && !isLoadedCov)
						populateConversation(discussionGroup);
				}
				break;
			}
		}
	}
}
