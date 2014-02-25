package com.v2tech.view;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
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

	private static final int REQUEST_SERVICE_BOUND = 1;
	private static final int FILL_CONTACTS_GROUP = 2;
	private static final int UPDATE_LIST_VIEW = 3;
	private static final int UPDATE_GROUP_STATUS = 4;
	private static final int UPDATE_USER_STATUS = 5;

	private Tab1BroadcastReceiver receiver = new Tab1BroadcastReceiver();
	private IntentFilter intentFilter;

	private JNIService mService;

	private ListView mContactsContainer;

	private ContactsAdapter adapter = new ContactsAdapter();

	private boolean mLoaded;

	private ContactsHandler mHandler = new ContactsHandler();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActivity().registerReceiver(receiver, getIntentFilter());
		Message.obtain(mHandler, REQUEST_SERVICE_BOUND).sendToTarget();
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
					((ContactGroupView)mItemList.get(pos).v).doExpandedOrCollapse();
					Message.obtain(mHandler, UPDATE_LIST_VIEW, pos, 0)
							.sendToTarget();
				}
			}

		});
		mContactsContainer.setDivider(null);
		return v;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mLoaded = false;
		getActivity().unregisterReceiver(receiver);
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
			intentFilter.addAction(MainActivity.SERVICE_BOUNDED_EVENT);
			intentFilter.addAction(JNIService.JNI_BROADCAST_USER_STATUS_NOTIFICATION);
			intentFilter.addAction(JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION);
		}
		return intentFilter;
	}

	List<Group> l;

	private void fillContactsGroup() {
		if (mLoaded) {
			return;
		}
		l = mService.getGroup(GroupType.CONTACT);
		if (l == null) {
			Message m = Message.obtain(mHandler, FILL_CONTACTS_GROUP);
			mHandler.sendMessageDelayed(m, 300);
			V2Log.i(" try to re-get group list");
		} else {
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
			} else if (intent.getAction().equals(
					MainActivity.SERVICE_BOUNDED_EVENT)) {
				mService = ((MainActivity) getActivity()).getService();
				if (!mLoaded) {
					Message.obtain(mHandler, FILL_CONTACTS_GROUP).sendToTarget();
				}

			} else if (JNIService.JNI_BROADCAST_USER_STATUS_NOTIFICATION.equals(intent.getAction())) {
				Message.obtain(mHandler, UPDATE_GROUP_STATUS, GlobalHolder.getInstance().getUser(intent.getExtras().getLong("uid"))).sendToTarget();
				long uid = intent.getExtras().getLong("uid");
				int status = intent.getExtras().getInt("status");
				Message.obtain(mHandler, UPDATE_USER_STATUS, (int)uid, status).sendToTarget();
			} else if (JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION.equals(intent.getAction())) {
				Message.obtain(mHandler, UPDATE_GROUP_STATUS).sendToTarget();
			}
		}

	}

	private void updateView(int pos) {
		ListItem item = mItemList.get(pos);
		if (item.g == null) {
			return;
		}
		if (item.isExpanded == false) {
			for (Group g : item.g.getChildGroup()) {
				mItemList.add(++pos, new ListItem(g));
			}
			for (User u : item.g.getUsers()) {
				mItemList.add(++pos, new ListItem(u));
			}
			
		} else {
			int startRemovePos = -1;
			int endRemovePos = -1;
			Group parent = item.g.getParent();
			Group nextGroup = null;
			int nextGrougPos = -1;
			
			List<Group> lg = parent != null ? parent.getChildGroup() : l;
			for (int i = 0; i < lg.size(); i++) {
				if (lg.get(i) == item.g) {
					nextGrougPos = i + 1;
					break;
				}
			}
			//found self
			if (nextGrougPos > -1) {
				startRemovePos = pos +1;
				//clicked group is last group
				if (nextGrougPos >= lg.size() ) {
					if (item.g.getParent() == null) {
						endRemovePos = mItemList.size();
					} else {
						endRemovePos = startRemovePos+1;
						while (true && endRemovePos <mItemList.size() ) {
							if (mItemList.get(endRemovePos).g != null) {
								break;
							}
							endRemovePos++;
							
						}
					}
				} else {
					nextGroup = parent.getChildGroup().get(nextGrougPos);
					for(int i=pos + 1; i < mItemList.size(); i++) {
						if (mItemList.get(i).g == nextGroup) {
							endRemovePos = i;
							break;
						}
					}
				}
				
				while (startRemovePos < endRemovePos) {
					mItemList.remove(startRemovePos);
					endRemovePos--;
				}
				
			} else {
				
			}
		}

		item.isExpanded = !item.isExpanded;
		adapter.notifyDataSetChanged();
	}

	List<ListItem> mItemList = new ArrayList<ListItem>();

	class ListItem {
		long id;
		Group g;
		User u;
		View v;
		boolean isExpanded;

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
			case REQUEST_SERVICE_BOUND:
				mService = ((MainActivity) getActivity()).getService();
				if (mService == null) {
					this.sendMessageDelayed(
							Message.obtain(this, REQUEST_SERVICE_BOUND), 500);
					break;
				}
				break;
			case FILL_CONTACTS_GROUP:
				fillContactsGroup();
				break;
			case UPDATE_LIST_VIEW:
				updateView(msg.arg1);
				break;
			case UPDATE_GROUP_STATUS:
				for (ListItem li : mItemList) {
					if (li.g != null) {
						((ContactGroupView)li.v).updateUserStatus();
					} 
				}
				break;
			case UPDATE_USER_STATUS:
				for (ListItem li : mItemList) {
					if (li.u != null && li.u.getmUserId() == msg.arg1) {
						((ContactUserView)li.v).updateStatus(User.Status.fromInt(msg.arg2));
						break;
					}
				}
				break;
			}
		}

	}

}
