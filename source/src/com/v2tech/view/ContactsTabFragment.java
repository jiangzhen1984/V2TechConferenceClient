package com.v2tech.view;

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
import android.widget.LinearLayout;

import com.v2tech.R;
import com.v2tech.logic.Group;
import com.v2tech.logic.Group.GroupType;
import com.v2tech.util.V2Log;
import com.v2tech.view.contacts.ContactGroupView;

public class ContactsTabFragment extends Fragment {

	private static final int REQUEST_SERVICE_BOUND = 1;
	private static final int FILL_CONTACTS_GROUP = 2;

	private Tab1BroadcastReceiver receiver = new Tab1BroadcastReceiver();
	private IntentFilter intentFilter;

	private JNIService mService;

	private LinearLayout mContactsContainer;

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
		mContactsContainer = (LinearLayout) v
				.findViewById(R.id.contacts_container);
		return v;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
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
		long l1 = System.currentTimeMillis();
		for (Group g : l) {
			mContactsContainer.addView(new ContactGroupView(getActivity(), g));
		}
		V2Log.e(System.currentTimeMillis() - l1 +"");
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
