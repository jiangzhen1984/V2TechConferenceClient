package com.v2tech.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.v2tech.R;
import com.v2tech.logic.GlobalHolder;
import com.v2tech.logic.Group;
import com.v2tech.logic.Group.GroupType;
import com.v2tech.logic.User;
import com.v2tech.util.V2Log;
import com.v2tech.view.contacts.ContactGroupView;
import com.v2tech.view.contacts.ContactUserView;

public class ContactsTabFragment extends Fragment {

	private static final int FILL_CONTACTS_GROUP = 2;
	private static final int UPDATE_LIST_VIEW = 3;
	private static final int UPDATE_GROUP_STATUS = 4;
	private static final int UPDATE_USER_STATUS = 5;
	private static final int UPDATE_SEARCHED_USER_LIST = 6;
	private static final int UPDATE_USER_AVATAR = 7;
	private static final int UPDATE_USER_SIGN = 8;

	private Tab1BroadcastReceiver receiver = new Tab1BroadcastReceiver();
	private IntentFilter intentFilter;

	private ListView mContactsContainer;

	private ContactsAdapter adapter = new ContactsAdapter();

	private boolean mLoaded;

	private ContactsHandler mHandler = new ContactsHandler();

	private EditText searchedTextET;

	private boolean mIsStartedSearch = false;

	private List<ListItem> mItemList = new ArrayList<ListItem>();
	private List<ListItem> mCacheItemList;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActivity().registerReceiver(receiver, getIntentFilter());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.tab_fragment_contacts, container,
				false);
		mContactsContainer = (ListView) v.findViewById(R.id.contacts_container);
		mContactsContainer.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int pos,
					long id) {
				if (mItemList.get(pos).g != null) {
					((ContactGroupView) mItemList.get(pos).v)
							.doExpandedOrCollapse();
					Message.obtain(mHandler, UPDATE_LIST_VIEW, pos, 0)
							.sendToTarget();
				}
			}

		});
		mContactsContainer.setDivider(null);

		searchedTextET = (EditText) v.findViewById(R.id.contacts_search);
		searchedTextET.addTextChangedListener(textChangedListener);
		return v;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mLoaded = false;
		getActivity().unregisterReceiver(receiver);
		V2Log.d("====destroy contact tab fragment");
	}

	@Override
	public void onStart() {
		super.onStart();
		if (!mLoaded) {
			Message.obtain(mHandler, FILL_CONTACTS_GROUP).sendToTarget();
		}
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	private IntentFilter getIntentFilter() {
		if (intentFilter == null) {
			intentFilter = new IntentFilter();
			intentFilter.addAction(JNIService.JNI_BROADCAST_GROUP_NOTIFICATION);
			intentFilter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_USER_STATUS_NOTIFICATION);
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION);
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_USER_AVATAR_CHANGED_NOTIFICATION);
			intentFilter.addAction(JNIService.JNI_BROADCAST_USER_UPDATE_NAME_OR_SIGNATURE);
		}
		return intentFilter;
	}

	List<Group> l;

	private void fillContactsGroup() {
		if (mLoaded) {
			return;
		}
		l = GlobalHolder.getInstance().getGroup(GroupType.CONTACT);
		if (l != null) {
			mLoaded = true;
			for (Group g : l) {
				mItemList.add(new ListItem(g));
			}
			mContactsContainer.setAdapter(adapter);
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
				V2Log.d(" update  status  JNI_BROADCAST_USER_STATUS_NOTIFICATION");
				Message.obtain(
						mHandler,
						UPDATE_GROUP_STATUS,
						GlobalHolder.getInstance().getUser(
								intent.getExtras().getLong("uid")))
						.sendToTarget();
				long uid = intent.getExtras().getLong("uid");
				int status = intent.getExtras().getInt("status");
				Message.obtain(mHandler, UPDATE_USER_STATUS, (int) uid, status)
						.sendToTarget();
			} else if (JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION
					.equals(intent.getAction())) {
				V2Log.d(" update  status  JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION");
				Message.obtain(mHandler, UPDATE_GROUP_STATUS).sendToTarget();
			} else if (JNIService.JNI_BROADCAST_USER_AVATAR_CHANGED_NOTIFICATION
					.equals(intent.getAction())) {
				Object[] ar = new Object[] { intent.getExtras().get("uid"),
						intent.getExtras().get("avatar") };
				Message.obtain(mHandler, UPDATE_USER_AVATAR, ar).sendToTarget();
			} else if (JNIService.JNI_BROADCAST_USER_UPDATE_NAME_OR_SIGNATURE.equals(intent.getAction())) {
				Message.obtain(mHandler, UPDATE_USER_SIGN, intent.getExtras().get("uid")).sendToTarget();
			}
		}

	}

	private TextWatcher textChangedListener = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable s) {

		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {

		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			if (s.length() > 0) {
				if (!mIsStartedSearch) {
					mCacheItemList = mItemList;
					mIsStartedSearch = true;
				}
			} else {
				mItemList = mCacheItemList;
				adapter.notifyDataSetChanged();
				mIsStartedSearch = false;
				return;
			}
			List<User> searchedUserList = new ArrayList<User>();
			for (Group g : l) {
				Group.searchUser(s.toString(), searchedUserList, g);
			}
			Message.obtain(mHandler, UPDATE_SEARCHED_USER_LIST,
					searchedUserList).sendToTarget();
		}

	};

	private void updateView(int pos) {
		ListItem item = mItemList.get(pos);
		if (item.g == null) {
			return;
		}
		if (item.isExpanded == false) {
			for (Group g : item.g.getChildGroup()) {
				mItemList.add(++pos, new ListItem(g));
			}
			List<User> sortList = new ArrayList<User>();
			sortList.addAll(item.g.getUsers());
			Collections.sort(sortList);
			for (User u : sortList) {
				mItemList.add(++pos, new ListItem(u));
			}

		} else {
			if (item.g.getChildGroup().size() <= 0 && item.g.getUsers().size() <= 0) {
				return;
			}
			
			int startRemovePos = pos + 1;
			int endRemovePos = pos;
			for (int index = pos + 1; index < mItemList.size(); index ++) {
				ListItem li = mItemList.get(index);
				if (li.g != null && li.g.getLevel() <= item.g.getLevel()) {
					break;
				}
				//FIXME if find user how to check?
				endRemovePos++;
			}
			
			while (startRemovePos <= endRemovePos && endRemovePos < mItemList.size()) {
				mItemList.remove(startRemovePos);
				endRemovePos--;
			}
		}

		item.isExpanded = !item.isExpanded;
		adapter.notifyDataSetChanged();
	}
	
	
	private void updateUserStatus(int userId, int status) {
		User.Status st = User.Status.fromInt(status);
		for (ListItem li : mItemList) {
			if (li.u != null && li.u.getmUserId() == userId) {
				((ContactUserView) li.v).updateStatus(st);
			}
		}
	}

	private synchronized void updateUserViewPostion(int userId, int status) {
		User.Status st = User.Status.fromInt(status);
		int index = 0;
		ListItem it = null;
		boolean foundUserView = false;
		for (ListItem li : mItemList) {
			if (li.u != null && li.u.getmUserId() == userId) {
				it = li;
				//((ContactUserView) li.v).updateStatus(st);
				foundUserView = true;
				break;
			}
			index++;
		}
		if (!foundUserView) {
			return;
		}

		boolean updatePosFlag = false;
		do {
			if (st == User.Status.HIDDEN || st == User.Status.OFFLINE) {
				++index;
			} else {
				--index;
			}
			if (index < 0) {
				index = 0;
				updatePosFlag = true;
				break;
			} else if (index >= mItemList.size()) {
				index = mItemList.size() -1;
				updatePosFlag = true;
				break;
			}

			ListItem item = mItemList.get(index);
			if (item.u != null && item.u.getmStatus() == st) {
				if (item.u.compareTo(it.u) >= 0) {
					updatePosFlag = true;
					index -=1;
					break;
				} else {
					continue;
				}
			} else if (item.g != null) {
				updatePosFlag = true;
				break;
			} else if (index ==  mItemList.size() -1) {
				updatePosFlag = true;
				break;
			}
		} while (index >= 0 && index < mItemList.size());

		if (updatePosFlag) {
			if (index == mItemList.size() -1) {
				mItemList.remove(it);
				mItemList.add(it);
			} else {
				mItemList.remove(it);
				mItemList.add(index, it);
			}
			adapter.notifyDataSetChanged();
		}
		

	}

	private void updateSearchedUserList(List<User> lu) {
		mItemList = new ArrayList<ListItem>();
		for (User u : lu) {
			ListItem item = new ListItem(u);
			((ContactUserView)item.v).removePadding();
			mItemList.add(item);
		}
		adapter.notifyDataSetChanged();
	}

	class ListItem {
		long id;
		Group g;
		User u;
		View v;
		boolean isExpanded;
		int level;

		public ListItem(Group g) {
			super();
			this.g = g;
			this.id = 0x02000000 | g.getmGId();
			this.v = new ContactGroupView(getActivity(), g, null);
			isExpanded = false;
		}

		public ListItem(User u) {
			super();
			this.u = u;
			this.id = 0x03000000 | u.getmUserId();
			this.v = new ContactUserView(getActivity(), u);
			isExpanded = false;
		}

	}

	class ContactsAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mItemList.size();
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
				updateUserStatus(msg.arg1, msg.arg2);
				//FIXME update all users in all groups
				updateUserViewPostion(msg.arg1, msg.arg2);
				break;
			case UPDATE_SEARCHED_USER_LIST:
				updateSearchedUserList((List<User>) msg.obj);
				break;
			case UPDATE_USER_AVATAR:
				Object[] ar = (Object[]) msg.obj;
				for (ListItem li : mItemList) {
					if (li.u != null && li.u.getmUserId() == (Long) ar[0]) {
						if (li.u.getAvatarPath() == null) {
							li.u.setAvatarPath(ar[1] == null? null:ar[1].toString());
						}
						((ContactUserView) li.v).updateAvatar(li.u
								.getAvatarBitmap());
					}
				}
				break;
			case UPDATE_USER_SIGN:
				Long uid = (Long)msg.obj;
				for (ListItem li : mItemList) {
					if (li.u != null && li.u.getmUserId() == uid) {
						((ContactUserView) li.v).updateSign();
					}
				}
				
			}
		}

	}

}
