package com.v2tech.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.AsyncTask;
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

import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.service.BitmapManager;
import com.v2tech.service.GlobalHolder;
import com.v2tech.view.bo.GroupUserObject;
import com.v2tech.view.bo.UserStatusObject;
import com.v2tech.view.contacts.ContactGroupView;
import com.v2tech.view.contacts.ContactUserView;
import com.v2tech.view.contacts.ContactsGroupActivity;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.User;

public class ContactsTabFragment extends Fragment implements TextWatcher {

	private static final int FILL_CONTACTS_GROUP = 2;
	private static final int UPDATE_LIST_VIEW = 3;
	private static final int UPDATE_GROUP_STATUS = 4;
	private static final int UPDATE_USER_STATUS = 5;
	private static final int UPDATE_SEARCHED_USER_LIST = 6;
	private static final int UPDATE_USER_SIGN = 8;

	private Context mContext;

	private Tab1BroadcastReceiver receiver = new Tab1BroadcastReceiver();
	private IntentFilter intentFilter;

	private ListView mContactsContainer;

	private ContactsAdapter adapter = new ContactsAdapter();

	private boolean mLoaded;

	private ContactsHandler mHandler = new ContactsHandler();

	private boolean mIsStartedSearch = false;

	private List<ListItem> mItemList = new ArrayList<ListItem>();
	private List<ListItem> mCacheItemList;

	private View rootView;

	private static final int TAG_ORG = 1;
	private static final int TAG_CONTACT = 2;
	public static final String TAG = "ContactsTabFragment";

	private int flag;
	private int currentPos = -1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String tag = this.getArguments().getString("tag");
		if (PublicIntent.TAG_ORG.equals(tag)) {
			flag = TAG_ORG;
		} else if (PublicIntent.TAG_CONTACT.equals(tag)) {
			flag = TAG_CONTACT;
		}

		getActivity().registerReceiver(receiver, getIntentFilter());
		mContext = getActivity();

		BitmapManager.getInstance().registerBitmapChangedListener(
				this.bitmapChangedListener);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (rootView != null) {
			return rootView;
		}
		rootView = inflater.inflate(R.layout.tab_fragment_contacts, container,
				false);
		mContactsContainer = (ListView) rootView
				.findViewById(R.id.contacts_container);
		mContactsContainer.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int pos,
					long id) {
				currentPos = pos;
				if (mItemList.get(pos).g != null) {
					((ContactGroupView) mItemList.get(pos).v)
							.doExpandedOrCollapse();
					Message.obtain(mHandler, UPDATE_LIST_VIEW, pos, 0)
							.sendToTarget();
				}
			}

		});
		if (flag == TAG_CONTACT) {
			mContactsContainer.setOnItemLongClickListener(mContactGroupManagementListener);
		}
		mContactsContainer.setDivider(null);

		// TextView tv = (TextView) rootView.findViewById(R.id.fragment_title);
		// if (flag == TAG_ORG) {
		// tv.setText(R.string.tab_org_name);
		// } else if (flag == TAG_CONTACT) {
		// tv.setText(R.string.tab_contact_name);
		// }

		return rootView;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		((ViewGroup) rootView.getParent()).removeView(rootView);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mLoaded = false;
		getActivity().unregisterReceiver(receiver);
		BitmapManager.getInstance().unRegisterBitmapChangedListener(
				this.bitmapChangedListener);
	}

	@Override
	public void onStart() {
		super.onStart();
		if (!mLoaded) {
			Message.obtain(mHandler, FILL_CONTACTS_GROUP).sendToTarget();
		}
		
		if (currentPos != -1) {
			mItemList.get(currentPos).isExpanded = lastExpanded;
			ContactGroupView contact = ((ContactGroupView) mItemList.get(currentPos).v);
			contact.getmGroupIndicatorIV().setTag(contact.getLastExpanded());
			contact.doExpandedOrCollapse();
		}
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);
		// recover search result
		if (!isVisibleToUser && mIsStartedSearch) {
			mItemList = mCacheItemList;
			adapter.notifyDataSetChanged();
			mIsStartedSearch = false;
			return;
		}
	}

	private IntentFilter getIntentFilter() {
		if (intentFilter == null) {
			intentFilter = new IntentFilter();
			intentFilter.addAction(JNIService.JNI_BROADCAST_GROUP_NOTIFICATION);
			intentFilter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
			intentFilter.addCategory(PublicIntent.DEFAULT_CATEGORY);
			intentFilter.addAction(JNIService.JNI_BROADCAST_GROUP_USER_ADDED);
			intentFilter.addAction(JNIService.JNI_BROADCAST_GROUP_USER_REMOVED);
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_USER_STATUS_NOTIFICATION);
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION);

			intentFilter
					.addAction(JNIService.JNI_BROADCAST_USER_UPDATE_NAME_OR_SIGNATURE);
			
			if (flag == TAG_CONTACT) {
				intentFilter.addAction(PublicIntent.BROADCAST_REQUEST_UPDATE_CONTACTS_GROUP);
			}
		}
		return intentFilter;
	}

	List<Group> l;

	private synchronized void fillContactsGroup() {
		if (mLoaded) {
			return;
		}
		if (flag == TAG_CONTACT) {
			l = GlobalHolder.getInstance().getGroup(GroupType.CONTACT);
		} else if (flag == TAG_ORG) {
			l = GlobalHolder.getInstance().getGroup(GroupType.ORG);
		}
		if (l != null) {
			new AsyncTaskLoader().execute();
		}
	}

	private Object mLock = new Object();
	private boolean lastExpanded;
	class AsyncTaskLoader extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			synchronized (mLock) {
				if (mLoaded == true || l.size() <= 0) {
					return null;
				}
				mItemList.clear();
				mLoaded = true;
				for (int i = 0; i < l.size(); i++) {
					Group g = l.get(i);
					ListItem li = new ListItem(g, g.getLevel());
					mItemList.add(li);
					iterateGroup(g);
				}
			}
			return null;
		}

		private void iterateGroup(Group g) {
			// for (User u : g.getUsers()) {
			// ListItem liu = new ListItem(u, g.getLevel() + 1);
			// }
			// for (Group subG : g.getChildGroup()) {
			// ListItem lisubg = new ListItem(subG, g.getLevel());
			// }
		}

		@Override
		protected void onPostExecute(Void result) {
			mContactsContainer.setAdapter(adapter);
			adapter.notifyDataSetChanged();
		}

	}

	class Tab1BroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(
					JNIService.JNI_BROADCAST_GROUP_NOTIFICATION)) {
				Message.obtain(mHandler, FILL_CONTACTS_GROUP).sendToTarget();
			} else if (JNIService.JNI_BROADCAST_USER_STATUS_NOTIFICATION
					.equals(intent.getAction())) {
				Message.obtain(mHandler, UPDATE_USER_STATUS,
						intent.getExtras().get("status")).sendToTarget();
			} else if (JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION
					.equals(intent.getAction())) {
				Message.obtain(mHandler, UPDATE_GROUP_STATUS).sendToTarget();
			} else if (JNIService.JNI_BROADCAST_USER_UPDATE_NAME_OR_SIGNATURE
					.equals(intent.getAction())) {
				Message.obtain(mHandler, UPDATE_USER_SIGN,
						intent.getExtras().get("uid")).sendToTarget();
			} else if (JNIService.JNI_BROADCAST_GROUP_USER_REMOVED
					.equals(intent.getAction())) {
				GroupUserObject guo = (GroupUserObject) intent.getExtras().get(
						"obj");
				// FIXME now just support contacts remove do not support
				if (flag == TAG_CONTACT
						&& guo.getmType() == Group.GroupType.CONTACT.intValue()) {
					// Remove from user
					for (int i = 0; i < l.size(); i++) {
						Group gr = l.get(i);
						gr.removeUserFromGroup(guo.getmUserId());
					}
					//
					for (int i = 0; i < mItemList.size(); i++) {
						ListItem item = mItemList.get(i);
						if (item.u != null
								&& item.u.getmUserId() == guo.getmUserId()) {
							mItemList.remove(i);
							// Do not break, because use exist in more than one
							// groups
							i--;
						}
					}
					adapter.notifyDataSetChanged();

				}
				// Update all group staticist information
				for (ListItem item : mItemList) {
					if (item.g != null) {
						((ContactGroupView) item.v).updateUserStatus();
					}
				}
			} else if (JNIService.JNI_BROADCAST_GROUP_USER_ADDED.equals(intent
					.getAction())) {
				GroupUserObject guo = (GroupUserObject) intent.getExtras().get(
						"obj");
				if (flag == TAG_CONTACT
						&& guo.getmType() == Group.GroupType.CONTACT.intValue()) {
					
					// Update all group staticist information
					for (int i = 0; i < mItemList.size(); i++) {
						ListItem item = mItemList.get(i);
						if(item.u != null){
							
							V2Log.e(TAG, "已存在的好友：" + GlobalHolder.getInstance().getUser(item.u.getmUserId()).getArra());
						}
						if (item.g != null) {
							if (item.isExpanded
									&& item.g.getmGId() == guo.getmGroupId()) {
								// ADD user
								User u = GlobalHolder.getInstance().getUser(
										guo.getmUserId());
								V2Log.e(TAG, "获取刚添加的好友 " + u.getArra());
								mItemList.add(++i,
										new ListItem(u, item.g.getLevel() + 1));
								continue;
							}
						}
					}
					Message.obtain(mHandler, UPDATE_GROUP_STATUS).sendToTarget();
					adapter.notifyDataSetChanged();
				}
			// Contacts group is updated
			} else if (PublicIntent.BROADCAST_REQUEST_UPDATE_CONTACTS_GROUP.equals(intent
					.getAction())) {
				mLoaded = false;
				fillContactsGroup();
			}
		}

	}

	@Override
	public void afterTextChanged(Editable s) {
		if (s != null && s.length() > 0) {
			if (!mIsStartedSearch) {
				mCacheItemList = mItemList;
				mIsStartedSearch = true;
			}
		} else {
			if (mIsStartedSearch) {
				mItemList = mCacheItemList;
				adapter.notifyDataSetChanged();
				mIsStartedSearch = false;
			}
			return;

		}
		String str = s == null ? "" : s.toString();
		List<User> searchedUserList = new ArrayList<User>();
		for (Group g : l) {
			Group.searchUser(str, searchedUserList, g);
		}

		Message.obtain(mHandler, UPDATE_SEARCHED_USER_LIST, searchedUserList)
				.sendToTarget();
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {

	}

	private void updateView(int pos) {
		ListItem item = mItemList.get(pos);
		if (item.g == null) {
			return;
		}
		if (item.isExpanded == false) {
			updateListState(pos, item);

		} else {
			updateListStateRemove(pos, item);
		}

		item.isExpanded = !item.isExpanded;
		lastExpanded = item.isExpanded;
		adapter.notifyDataSetChanged();
	}

	private void updateListStateRemove(int pos, ListItem item) {
		if ((item.g.getChildGroup() == null || item.g.getChildGroup().size() <= 0)
				&& (item.g.getUsers() == null || item.g.getUsers().size() <= 0)) {
			return;
		}

		int startRemovePos = pos + 1;
		int endRemovePos = pos;
		for (int index = pos + 1; index < mItemList.size(); index++) {
			ListItem li = mItemList.get(index);
			if (li.g != null && li.g.getLevel() <= item.g.getLevel()) {
				break;
			}
			if (li.u != null && li.level == item.g.getLevel()) {
				break;
			}
			endRemovePos++;
		}
		V2Log.i("Contacts collpse " + startRemovePos + "  " + endRemovePos);
		while (startRemovePos <= endRemovePos
				&& endRemovePos < mItemList.size()) {
			mItemList.remove(startRemovePos);
			endRemovePos--;
		}
	}

	private void updateListState(int pos, ListItem item) {
		
		for (Group g : item.g.getChildGroup()) {
			ListItem cache = new ListItem(g, g.getLevel());
			mItemList.add(++pos, cache);
		}
		List<User> sortList = new ArrayList<User>();
		sortList.addAll(item.g.getUsers());
		Collections.sort(sortList);
		for (User u : sortList) {
			ListItem cache = new ListItem(u, item.g.getLevel() + 1);
			mItemList.add(++pos, cache);
		}
	}

	private void updateUserStatus(long userId, User.DeviceType deiType,
			User.Status newState) {
		User u = GlobalHolder.getInstance().getUser(userId);
		if (u == null) {
			V2Log.w("No user found, returned");
			return;
		}
		// User.Status oldState = u.getmStatus();

		Set<Group> groupList = new HashSet<Group>();
		// // Match user state---Notice we can't match, because JNI update user
		// state before this.
		// // if new state is online or leave or busy or do not disturb and old
		// // state is hidden or off-line
		// // we need to re-calculate group online staticist
		// if (((oldState == User.Status.HIDDEN || oldState ==
		// User.Status.OFFLINE) && (newState == User.Status.ONLINE
		// || newState == User.Status.BUSY
		// || newState == User.Status.LEAVE || newState ==
		// User.Status.DO_NOT_DISTURB))
		// || ((newState == User.Status.HIDDEN || newState ==
		// User.Status.OFFLINE) && (oldState == User.Status.ONLINE
		// || oldState == User.Status.BUSY
		// || oldState == User.Status.LEAVE || oldState ==
		// User.Status.DO_NOT_DISTURB))) {
		Set<Group> gSet = u.getBelongsGroup();
		for (Group tg : gSet) {
			while (tg != null) {
				groupList.add(tg);
				tg = tg.getParent();
			}
		}

		// }
		for (ListItem li : mItemList) {
			if (li.u != null && li.u.getmUserId() == userId) {
				((ContactUserView) li.v).updateStatus(deiType, newState);
			}
			if (li.g != null) {
				for (Group g : groupList) {
					if (g.getmGId() == li.g.getmGId()) {
						((ContactGroupView) li.v).updateUserStatus();
					}
				}
			}
		}
	}

	private synchronized void updateUserViewPostionV2(long userId,
			User.Status newSt) {
		V2Log.i(" Contacts update user status : " + userId + "  : " + newSt);
		int startSortIndex = 0;
		boolean foundUserView = false;
		ListItem self = null;
		for (int i = 0; i < mItemList.size(); i++) {
			ListItem li = mItemList.get(i);
			if (li.u != null && li.u.getmUserId() == userId) {
				self = li;
				foundUserView = true;
				break;
			}
			startSortIndex++;
		}
		if (!foundUserView) {
			return;
		}

		// Try to looking for position which start sort index.
		//
		while (startSortIndex >= 0) {
			ListItem item = mItemList.get(startSortIndex);
			if (item.g != null) {
				break;
			} else if (item.u != null && item.level != self.level) {
				break;
			}
			if (startSortIndex == 0) {
				V2Log.e(" Didn't find compatable position for sort");
				return;
			}
			--startSortIndex;
		}
		mItemList.remove(self);

		int pos = startSortIndex + 1;
		for (startSortIndex += 1; startSortIndex < mItemList.size(); startSortIndex++, pos++) {
			ListItem item = mItemList.get(startSortIndex);
			if (item.g != null) {
				break;
			}
			// if item is current user, always sort after current user
			if (item.u.getmUserId() == GlobalHolder.getInstance()
					.getCurrentUserId()) {
				continue;
			}
			if (newSt == User.Status.ONLINE) {
				if (item.u.getmStatus() == User.Status.ONLINE
						&& item.u.compareTo(self.u) < 0) {
					continue;
				} else {
					break;
				}
			} else if (newSt == User.Status.OFFLINE
					|| newSt == User.Status.HIDDEN) {
				if ((item.u.getmStatus() == User.Status.OFFLINE || item.u
						.getmStatus() == User.Status.HIDDEN)
						&& self.u.compareTo(item.u) > 0) {
					continue;
				} else if (item.u.getmStatus() != User.Status.OFFLINE
						&& item.u.getmStatus() != User.Status.HIDDEN) {
					continue;
				} else {
					break;
				}
			} else {
				if (item.u.getmStatus() == User.Status.ONLINE) {
					continue;
				} else if (item.u.getmStatus() == User.Status.OFFLINE
						|| item.u.getmStatus() == User.Status.HIDDEN) {
					break;
				} else if (item.u.compareTo(self.u) < 0) {
					continue;
				} else {
					break;
				}
			}
		}

		V2Log.i(" Contacts update pos " + pos);
		if (pos == mItemList.size()) {
			mItemList.add(self);
		} else {
			mItemList.add(pos, self);
		}

		adapter.notifyDataSetChanged();
	}

	private void updateSearchedUserList(List<User> lu) {
		Collections.sort(lu);
		mItemList = new ArrayList<ListItem>();
		for (User u : lu) {
			ListItem item = new ListItem(u, -1);
			((ContactUserView) item.v).removePadding();
			mItemList.add(item);
		}
		adapter.notifyDataSetChanged();
	}

	private BitmapManager.BitmapChangedListener bitmapChangedListener = new BitmapManager.BitmapChangedListener() {

		@Override
		public void notifyAvatarChanged(User user, Bitmap bm) {
			for (ListItem li : mItemList) {
				if (li.u != null && li.u.getmUserId() == user.getmUserId()) {
					((ContactUserView) li.v).updateAvatar(bm);
				}
			}
		}
	};
	
	private OnItemLongClickListener mContactGroupManagementListener = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
				int arg2, long arg3) {
			Intent i = new Intent();
			i.setClass(mContext, ContactsGroupActivity.class);
			i.addCategory(PublicIntent.DEFAULT_CATEGORY);
			startActivity(i);
			return true;
		}
		
	};

	class ListItem {
		long id;
		Group g;
		User u;
		View v;
		boolean isExpanded;
		int level;

		public ListItem(Group g, int level) {
			super();
			this.g = g;
			this.id = 0x02000000 | g.getmGId();
			this.v = new ContactGroupView(getActivity(), g, null);
			isExpanded = false;
			this.level = level;
		}

		public ListItem(User u, int level) {
			super();
			this.u = u;
			this.id = 0x03000000 | u.getmUserId();
			this.v = new ContactUserView(getActivity(), u);
			((ContactUserView) this.v).setPaddingT((level - 1) * 35,
					this.v.getTop(), this.v.getRight(), this.v.getBottom());
			isExpanded = false;
			this.level = level;
		}

	}

	class ContactsAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mItemList == null ? 0 : mItemList.size();
		}

		@Override
		public Object getItem(int position) {
			ListItem item = mItemList.get(position);
			return item.g == null ? item.u : item.g;
		}

		@Override
		public long getItemId(int position) {
			return mItemList.get(position).id;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return mItemList.get(position).v;
		}

	}

	class ContactsHandler extends Handler {


		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case FILL_CONTACTS_GROUP:
				fillContactsGroup();
				break;
			case UPDATE_LIST_VIEW:
				updateView(msg.arg1);
				break;
			case UPDATE_GROUP_STATUS:
				if (mItemList == null) {
					V2Log.d(" handle message UPDATE_GROUP_STATUS");
				}
				for (ListItem li : mItemList) {
					if (li.g != null) {
						((ContactGroupView) li.v).updateUserStatus();
					}
				}
				break;
			case UPDATE_USER_STATUS:
				UserStatusObject uso = (UserStatusObject) msg.obj;
				updateUserStatus(uso.getUid(),
						User.DeviceType.fromInt(uso.getDeviceType()),
						User.Status.fromInt(uso.getStatus()));

				// FIXME update all users in all groups
				updateUserViewPostionV2(uso.getUid(),
						User.Status.fromInt(uso.getStatus()));
				break;
			case UPDATE_SEARCHED_USER_LIST:
				updateSearchedUserList((List<User>) msg.obj);
				break;
			case UPDATE_USER_SIGN:
				Long uid = (Long) msg.obj;
				for (ListItem li : mItemList) {
					if (li.u != null && li.u.getmUserId() == uid) {
						((ContactUserView) li.v).updateSign();
					}
				}
				break;
			}
			
		}

	}

}
