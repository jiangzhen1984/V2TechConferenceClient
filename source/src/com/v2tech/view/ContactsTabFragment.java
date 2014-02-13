package com.v2tech.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.v2tech.R;
import com.v2tech.logic.Group;
import com.v2tech.logic.Group.GroupType;
import com.v2tech.logic.User;
import com.v2tech.view.contacts.ContactGroupView;
import com.v2tech.view.contacts.ContactUserView;

public class ContactsTabFragment extends Fragment {

	private static final int REQUEST_SERVICE_BOUND = 1;
	private static final int FILL_CONTACTS_GROUP = 2;

	private Tab1BroadcastReceiver receiver = new Tab1BroadcastReceiver();
	private IntentFilter intentFilter;

	private JNIService mService;

	private ListView mContactsContainer;

	private ContactsHandler mHandler = new ContactsHandler();

	private Map<Long, ListItem> mItemHolder = new HashMap<Long, ListItem>();

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
		mContactsContainer.setAdapter(adapter);
		mContactsContainer.setDivider(null);
		return v;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mItemList.clear();
	}

	@Override
	public void onStart() {
		super.onStart();
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
		}
		return intentFilter;
	}

	private void fillContactsGroup() {
		List<Group> l = mService.getGroup(GroupType.CONTACT);
		for (Group g : l) {
			mItemList.add(new ListItem(g));
		}
		adapter.notifyDataSetChanged();
	}

	class Tab1BroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(
					JNIService.JNI_BROADCAST_GROUP_NOTIFICATION)) {
			} else if (intent.getAction().equals(
					MainActivity.SERVICE_BOUNDED_EVENT)) {
				mService = ((MainActivity) getActivity()).getService();
				Message.obtain(mHandler, FILL_CONTACTS_GROUP).sendToTarget();
			}
		}

	}

	List<ListItem> mItemList = new ArrayList<ListItem>();
	private ContactsAdapter adapter = new ContactsAdapter();

	class ListItem {
		private Group g;
		private User u;
		private int type;
		private View v;

		public ListItem(Group g) {
			super();
			this.g = g;
			type = 1;
			v = new ContactGroupView(getActivity(), g, groupClickListener);
		}

		public ListItem(User u) {
			super();
			this.u = u;
			type = 2;
			v = new ContactUserView(getActivity(), u);
		}

	}

	private OnClickListener groupClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			ContactGroupView gv = (ContactGroupView) v;
			Group g = gv.getGroup();
			int index = 0;
			for (ListItem i : mItemList) {
				index++;
				if (i.type == 1 && i.g == g) {
					break;
				}

			}
			// remove already showed child
			if (gv.isShowedChild()) {
				Long l = System.currentTimeMillis();
				for (int i = 0; i < mItemList.size(); i++) {
					ListItem item = mItemList.get(i);
					List<Group> lg = g.getChildGroup();
					for (Group gg : lg) {
						if (item.type == 1 && item.g.getmGId() == gg.getmGId()) {
							mItemList.remove(item);
							i--;
						}
					}
					List<User> lu = g.getUsers();
					for (User u : lu) {
						if (item.type == 2
								&& item.u.getmUserId() == u.getmUserId()) {
							mItemList.remove(item);
							i--;
						}
					}
				}
				System.out.println(System.currentTimeMillis() - l + " ");
				// add child to show list
			} else {
				List<Group> lg = g.getChildGroup();
				for (Group gg : lg) {
					mItemList.add(index++, getListItem(gg));
				}
				List<User> lu = g.getUsers();
				for (User u : lu) {
					mItemList.add(index++, getListItem(u));
				}
			}

			adapter.notifyDataSetChanged();
		}

		private ListItem getListItem(Group g) {
			Long key = Long.valueOf(g.getmGId());
			ListItem it = mItemHolder.get(key);
			if (it == null) {
				it = new ListItem(g);
				mItemHolder.put(key, it);
			}
			return it;
		}

		private ListItem getListItem(User u) {
			Long key = Long.valueOf(u.getmUserId());
			ListItem it = mItemHolder.get(key);
			if (it == null) {
				it = new ListItem(u);
				mItemHolder.put(key, it);
			}
			return it;
		}

	};

	class ContactsAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mItemList.size();
		}

		@Override
		public Object getItem(int pos) {
			ListItem item = mItemList.get(pos);
			return item.type == 1 ? item.g : item.u;
		}

		@Override
		public long getItemId(int arg0) {
			return 0;
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
			case FILL_CONTACTS_GROUP:
				fillContactsGroup();
				break;
			}
		}

	}

}
