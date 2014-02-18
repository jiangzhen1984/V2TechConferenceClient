package com.v2tech.view;

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
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;

import com.v2tech.R;
import com.v2tech.logic.Group;
import com.v2tech.logic.Group.GroupType;
import com.v2tech.view.contacts.ContactGroupView;
import com.v2tech.view.contacts.ContactUserView;

public class ContactsTabFragment extends Fragment {

	private static final int REQUEST_SERVICE_BOUND = 1;
	private static final int FILL_CONTACTS_GROUP = 2;

	private Tab1BroadcastReceiver receiver = new Tab1BroadcastReceiver();
	private IntentFilter intentFilter;

	private JNIService mService;

	private ExpandableListView mContactsContainer;
	
	private boolean mLoaded;

	private ContactsHandler mHandler = new ContactsHandler();
	
	private Map<Long, View> adapterView = new HashMap<Long, View>();

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
		mContactsContainer = (ExpandableListView) v.findViewById(R.id.contacts_container);
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
		}
		return intentFilter;
	}

	List<Group> l;
	private void fillContactsGroup() {
		mLoaded = true;
		l = mService.getGroup(GroupType.CONTACT);
		//TODO
		if (l == null) {
			
		} else {
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
			} else if (intent.getAction().equals(
					MainActivity.SERVICE_BOUNDED_EVENT)) {
				mService = ((MainActivity) getActivity()).getService();
				
			}
		}

	}

	private ContactsAdapter adapter = new ContactsAdapter();



	
	
	class ContactsAdapter extends BaseExpandableListAdapter {

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return l.get(groupPosition).getUsers().get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return l.get(groupPosition).getUsers().get(childPosition).getmUserId();
		}

		@Override
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			Long key = Long.valueOf(l.get(groupPosition).getUsers().get(childPosition).getmUserId());
			View v = adapterView.get(key);
			if (v == null) {
				v = new ContactUserView(getActivity(), l.get(groupPosition).getUsers().get(childPosition));
				adapterView.put(key, v);
			}
			return v;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return l.get(groupPosition).getUsers().size();
		}

		@Override
		public Object getGroup(int groupPosition) {
			return l.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return l.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			return l.get(groupPosition).getmGId();
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			Long key = Long.valueOf(l.get(groupPosition).getmGId());
			View v = adapterView.get(key);
			if (v == null) {
				v = new ContactGroupView(getActivity(), l.get(groupPosition), null);
				adapterView.put(key, v);
			}
			
			return v;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return false;
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
			}
		}

	}

}
