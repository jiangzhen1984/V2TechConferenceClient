package com.v2tech.view;

import java.util.ArrayList;
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
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.V2.jni.V2GlobalEnum;
import com.v2tech.R;
import com.v2tech.service.BitmapManager;
import com.v2tech.service.GlobalHolder;
import com.v2tech.view.bo.GroupUserObject;
import com.v2tech.view.bo.UserStatusObject;
import com.v2tech.view.contacts.ContactsGroupActivity;
import com.v2tech.view.widget.GroupListView;
import com.v2tech.view.widget.GroupListView.Item;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.User;

public class ContactsTabFragment extends Fragment implements TextWatcher {

	private static final int FILL_CONTACTS_GROUP = 2;
	private static final int UPDATE_GROUP_STATUS = 4;
	private static final int UPDATE_USER_STATUS = 5;
	private static final int UPDATE_USER_SIGN = 8;

	private Context mContext;

	private Tab1BroadcastReceiver receiver = new Tab1BroadcastReceiver();
	private IntentFilter intentFilter;

	private GroupListView mContactsContainer;
	private View rootView;
	private List<Group> mGroupList;

	private boolean mLoaded;

	private ContactsHandler mHandler = new ContactsHandler();

	private static final int TAG_ORG = V2GlobalEnum.GROUP_TYPE_DEPARTMENT;
	private static final int TAG_CONTACT = V2GlobalEnum.GROUP_TYPE_CONTACT;
	public static final String TAG = "ContactsTabFragment";

	private int flag;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String tag = this.getArguments().getString("tag");
		if (PublicIntent.TAG_ORG.equals(tag)) {
			flag = TAG_ORG;
		} else if (PublicIntent.TAG_CONTACT.equals(tag)) {
			flag = TAG_CONTACT;
		}

		mGroupList = new ArrayList<Group>();

		getActivity().registerReceiver(receiver, getIntentFilter());
		mContext = getActivity();

		BitmapManager.getInstance().registerBitmapChangedListener(
				this.bitmapChangedListener);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.i("wzl", "ContactsTabFragment onCreateView");
		if (rootView != null) {
			return rootView;
		}
		rootView = inflater.inflate(R.layout.tab_fragment_contacts, container,
				false);
		mContactsContainer = (GroupListView) rootView
				.findViewById(R.id.contacts_container);

		mContactsContainer.setListener(mListener);
		mContactsContainer.setTextFilterEnabled(true);

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
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);
		if (mContactsContainer != null) {
			mContactsContainer.clearTextFilter();
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
				intentFilter
						.addAction(PublicIntent.BROADCAST_REQUEST_UPDATE_CONTACTS_GROUP);
				intentFilter
						.addAction(PublicIntent.BROADCAST_CONTACT_GROUP_UPDATED_NOTIFICATION);
			}
		}
		return intentFilter;
	}

	private synchronized void fillContactsGroup() {
		if (mLoaded) {
			return;
		}
		new AsyncTaskLoader().execute();
	}

	private Object mLock = new Object();

	class AsyncTaskLoader extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			synchronized (mLock) {
				if (mLoaded) {
					return null;
				}
				if (flag == TAG_CONTACT) {
					mGroupList = GlobalHolder.getInstance().getGroup(
							GroupType.CONTACT.intValue());
				} else if (flag == TAG_ORG) {
					mGroupList = GlobalHolder.getInstance().getGroup(
							GroupType.ORG.intValue());
				}
				if (mGroupList != null && mGroupList.size() > 0) {
					mLoaded = true;
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			mContactsContainer.setGroupList(mGroupList);
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
					User user = GlobalHolder.getInstance().getUser(
							guo.getmUserId());

					if (user == null) {
						return;
					}
					// Remove user from contact group
					Set<Group> groups = user.getBelongsGroup();
					for (Group gg : groups) {
						if (gg.getGroupType() == GroupType.CONTACT) {
							gg.removeUserFromGroup(user);
						}
					}

					mContactsContainer.removeItem(user);
				}

			} else if (JNIService.JNI_BROADCAST_GROUP_USER_ADDED.equals(intent
					.getAction())) {
				GroupUserObject guo = (GroupUserObject) intent.getExtras().get(
						"obj");
				if (flag == TAG_CONTACT
						&& guo.getmType() == Group.GroupType.CONTACT.intValue()) {
					mContactsContainer.addUser(
							GlobalHolder.getInstance().getGroupById(
									GroupType.CONTACT.intValue(),
									guo.getmGroupId()), GlobalHolder
									.getInstance().getUser(guo.getmUserId()));
				}
				// Contacts group is updated
			} else if (PublicIntent.BROADCAST_REQUEST_UPDATE_CONTACTS_GROUP
					.equals(intent.getAction())) {
				mLoaded = false;
				fillContactsGroup();
			} else if (PublicIntent.BROADCAST_CONTACT_GROUP_UPDATED_NOTIFICATION
					.equals(intent.getAction())) {

				long uid = intent.getLongExtra("userId", 0);
				long srcGroupId = intent.getLongExtra("srcGroupId", 0);
				long destGroupId = intent.getLongExtra("destGroupId", 0);
				User u = GlobalHolder.getInstance().getUser(uid);
				Group src = GlobalHolder.getInstance().getGroupById(
						GroupType.CONTACT.intValue(), srcGroupId);
				Group dest = GlobalHolder.getInstance().getGroupById(
						GroupType.CONTACT.intValue(), destGroupId);
				mContactsContainer.updateUserGroup(u, src, dest);
			}
		}

	}

	@Override
	public void afterTextChanged(Editable s) {
		if (TextUtils.isEmpty(s.toString())) {
			if (!TextUtils.isEmpty(mContactsContainer.getTextFilter())) {
				mContactsContainer.clearTextFilter();
			}
		} else {
			mContactsContainer.setFilterText(s.toString());
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {

	}

	private BitmapManager.BitmapChangedListener bitmapChangedListener = new BitmapManager.BitmapChangedListener() {

		@Override
		public void notifyAvatarChanged(User user, Bitmap bm) {
			mContactsContainer.updateItem(user);
		}
	};

	private GroupListView.GroupListViewListener mListener = new GroupListView.GroupListViewListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id, Item item) {
			if (flag == TAG_CONTACT && item.getObject() instanceof Group) {
				Intent i = new Intent();
				i.setClass(mContext, ContactsGroupActivity.class);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				startActivity(i);
				return true;
			}
			return false;
		}

		@Override
		public void onItemClicked(AdapterView<?> parent, View view,
				int position, long id, Item item) {
			if (item.getObject() instanceof User) {
				Intent i = new Intent(PublicIntent.SHOW_CONTACT_DETAIL_ACTIVITY);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				i.putExtra("uid", ((User) item.getObject()).getmUserId());
				startActivity(i);
			}

		}

		public void onCheckboxClicked(View view, Item item) {

		}
	};

	class ContactsHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case FILL_CONTACTS_GROUP:
				fillContactsGroup();
				break;
			case UPDATE_GROUP_STATUS:
				//Just notify group statist information
				mContactsContainer.notifiyDataSetChanged();
				break;
			case UPDATE_USER_STATUS:
				UserStatusObject uso = (UserStatusObject) msg.obj;
				User.Status us = User.Status.fromInt(uso.getStatus());
				User user = GlobalHolder.getInstance().getUser(uso.getUid());
				mContactsContainer.updateUserStatus(user, us);
				break;
			case UPDATE_USER_SIGN:
				Long uid = (Long) msg.obj;
				mContactsContainer.updateItem(GlobalHolder.getInstance()
						.getUser(uid));
				break;
			}

		}

	}

}
