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
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupCollapseListener;
import android.widget.ExpandableListView.OnGroupExpandListener;

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

	private ExpandableListView mContactsContainer;

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
		mContactsContainer = (ExpandableListView) v
				.findViewById(R.id.contacts_container);
		mContactsContainer.setOnChildClickListener(new OnChildClickListener() {

			@Override
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {
				if (v instanceof ExpandableListView) {
					((ExpandableListView) v).expandGroup(0);
				}
				return true;
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
		}
		return intentFilter;
	}

	List<Group> l;

	private void fillContactsGroup() {
		mLoaded = true;
		l = mService.getGroup(GroupType.CONTACT);
		// TODO
		if (l == null) {

		} else {
			mContactsContainer.setAdapter(new ContactsAdapter(l));
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

	class ContactsAdapter extends BaseExpandableListAdapter {

		private List<Group> mDatas;

		private Map<Long, View> adapterView;

		private Map<Long, View> adapterGroupView;

		public ContactsAdapter(List<Group> mDatas) {
			super();
			this.mDatas = mDatas;
			this.adapterView = new HashMap<Long, View>();
			adapterGroupView = new HashMap<Long, View>();
		}

		public ContactsAdapter(Group g) {
			super();
			this.mDatas = new ArrayList<Group>();
			this.mDatas.add(g);
			this.adapterView = new HashMap<Long, View>();
			adapterGroupView = new HashMap<Long, View>();
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			// return mDatas.get(groupPosition).getUsers().get(childPosition);
			return null;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			Group g = mDatas.get(groupPosition);
			if (childPosition >= g.getChildGroup().size()) {
				return g.getUsers()
						.get(childPosition - g.getChildGroup().size())
						.getmUserId();
			} else {
				return g.getChildGroup().get(childPosition).getmGId();
			}
		}

		@Override
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			Group g = mDatas.get(groupPosition);
			if (childPosition >= g.getChildGroup().size()) {
				int rPos = childPosition - g.getChildGroup().size();
				User u = g.getUsers().get(rPos);
				Long key = Long.valueOf(u.getmUserId());
				View v = adapterView.get(key);
				if (v == null) {
					v = new ContactUserView(getActivity(), u);
					adapterView.put(key, v);
				}
				return v;
			} else {
				Group subGroup = g.getChildGroup().get(childPosition);
				Long key = Long.valueOf(subGroup.getmGId());
				View v = adapterGroupView.get(key);
				if (v == null) {
					final ExpandableListView lv = new ExpandableListView(
							getActivity());
					lv.setDivider(null);
					final ContactsAdapter ca = new ContactsAdapter(subGroup);
					lv.setAdapter(ca);
					lv.setGroupIndicator(getActivity().getResources()
							.getDrawable(
									R.drawable.selector_contact_group_arrow));
					lv.setPadding(50, 0, 0, 0);
					lv.setOnGroupExpandListener(new OnGroupExpandListener() {
						@Override
						public void onGroupExpand(int groupPosition) {

							AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
									ViewGroup.LayoutParams.MATCH_PARENT, (ca
											.getChildrenCount(0) + 1)
											* adapterGroupView.values()
													.iterator().next()
													.getMeasuredHeight());
							lv.setLayoutParams(lp);
						}
					});
					lv.setOnGroupCollapseListener(new OnGroupCollapseListener() {

						@Override
						public void onGroupCollapse(int arg0) {
							AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
									ViewGroup.LayoutParams.MATCH_PARENT,
									ViewGroup.LayoutParams.WRAP_CONTENT);
							lv.setLayoutParams(lp);
						}

					});
					adapterGroupView.put(key, lv);
					v = lv;

				}
				return v;
			}
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			Group g = mDatas.get(groupPosition);
			return g.getChildGroup().size() + g.getUsers().size();
		}

		@Override
		public Object getGroup(int groupPosition) {
			return mDatas.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return mDatas.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			return mDatas.get(groupPosition).getmGId();
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			Long key = null;

			key = Long.valueOf(mDatas.get(groupPosition).getmGId());
			View v = adapterGroupView.get(key);
			if (v == null) {
				v = new ContactGroupView(getActivity(),
						mDatas.get(groupPosition), null);
				adapterGroupView.put(key, v);
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
