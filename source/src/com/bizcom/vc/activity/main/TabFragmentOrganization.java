package com.bizcom.vc.activity.main;

import java.util.ArrayList;
import java.util.List;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.V2.jni.util.V2Log;
import com.bizcom.bo.GroupUserObject;
import com.bizcom.bo.UserStatusObject;
import com.bizcom.request.util.BitmapManager;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.application.PublicIntent;
import com.bizcom.vc.service.JNIService;
import com.bizcom.vc.widget.MultilevelListView;
import com.bizcom.vc.widget.MultilevelListView.ItemData;
import com.bizcom.vo.Group;
import com.bizcom.vo.User;
import com.bizcom.vo.Group.GroupType;
import com.v2tech.R;

public class TabFragmentOrganization extends Fragment implements TextWatcher{

	private static final int FILL_CONTACTS_GROUP = 2;
	private static final int UPDATE_GROUP_STATUS = 4;
	private static final int UPDATE_USER_STATUS = 5;
	private static final int UPDATE_USER_SIGN = 8;

	private LocalReceiver receiver = new LocalReceiver();
	private IntentFilter intentFilter;

	private MultilevelListView mContactsContainer;
	private View rootView;
	private List<Group> mGroupList;

	private boolean mLoaded;

	private LocalHandler mHandler = new LocalHandler();

	public static final String TAG = "OrganizationTabFragment";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mGroupList = new ArrayList<Group>();
		getActivity().registerReceiver(receiver, getIntentFilter());
		BitmapManager.getInstance().registerBitmapChangedListener(
				this.bitmapChangedListener);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (rootView != null) {
			return rootView;
		}
		rootView = inflater.inflate(R.layout.tab_fragment_organization,
				container, false);
		mContactsContainer = (MultilevelListView) rootView
				.findViewById(R.id.contacts_container);

		mContactsContainer.setListener(mListener);
		mContactsContainer.setTextFilterEnabled(true);

		mContactsContainer.setDivider(null);
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
		//
		// if (mContactsContainer != null) {
		// mContactsContainer.updateDisplay();
		// Log.i("20141223 1","mContactsContainer 刷新");
		// }

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
			intentFilter.addAction(PublicIntent.BROADCAST_USER_COMMENT_NAME_NOTIFICATION);
			intentFilter.addAction(JNIService.JNI_BROADCAST_GROUP_USER_ADDED);
			intentFilter.addAction(JNIService.JNI_BROADCAST_GROUP_USER_REMOVED);
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_USER_STATUS_NOTIFICATION);
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION);
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_USER_UPDATE_BASE_INFO);

		}
		return intentFilter;
	}

	private synchronized void fillContactsGroup() {
		if (mLoaded) {
			return;
		}
		new AsyncTaskLoader().execute();
	}
	
	@Override
	public void afterTextChanged(Editable s) {
		if (mContactsContainer == null) {
			return;
		}
		String str = s.toString();
		if (TextUtils.isEmpty(str)) {
			mContactsContainer.clearTextFilter();
		} else {
			mContactsContainer.setFilterText(str);
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {

	}
	

	private Object mLock = new Object();

	class AsyncTaskLoader extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			synchronized (mLock) {
				if (mLoaded) {
					return null;
				}

				mGroupList = GlobalHolder.getInstance().getGroup(
						GroupType.ORG.intValue());

				if (mGroupList != null && mGroupList.size() > 0) {
					mLoaded = true;
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			mContactsContainer.setContactsGroupList(mGroupList);
		}

	}

	class LocalReceiver extends BroadcastReceiver {

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
			} else if (JNIService.JNI_BROADCAST_GROUP_USER_REMOVED
					.equals(intent.getAction())) {
				GroupUserObject obj = (GroupUserObject) intent.getExtras().get(
						"obj");
				if (obj == null) {
					V2Log.e(TAG,
							"JNI_BROADCAST_GROUP_USER_REMOVED --> Update Conversation failed that the user removed ... given GroupUserObject is null");
					return;
				}
				V2Log.d(TAG, "JNI GROUP USER REMOVED COMMING! GroupType : "
						+ obj.getmType() + " GroupID : " + obj.getmGroupId()
						+ " UserID : " + obj.getmUserId());
				// FIXME now just support contacts remove do not support
				if (obj.getmType() == Group.GroupType.ORG.intValue()) {
					// update user name
					mContactsContainer.updateUser(GlobalHolder.getInstance()
							.getUser(obj.getmUserId()));
				}

			} else if (JNIService.JNI_BROADCAST_GROUP_USER_ADDED.equals(intent
					.getAction())) {
				V2Log.d(TAG,
						"JNI_BROADCAST_GROUP_USER_ADDED --> The New User Coming !");
				GroupUserObject guo = (GroupUserObject) intent.getExtras().get(
						"obj");
				if (guo == null) {
					V2Log.e(TAG,
							"JNI_BROADCAST_GROUP_USER_ADDED --> Add New User Failed ! Because"
									+ "Given GroupUserObject is null!");
					return;
				}

				if (guo.getmType() == Group.GroupType.ORG.intValue()) {
					mContactsContainer.addUser(
							GlobalHolder.getInstance()
									.getGroupById(GroupType.ORG.intValue(),
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
			} else if (JNIService.JNI_BROADCAST_USER_UPDATE_BASE_INFO
					.equals(intent.getAction())) {
				long uid = intent.getLongExtra("uid", -1);
				if (uid == -1)
					return;
				Message.obtain(mHandler, UPDATE_USER_SIGN, uid).sendToTarget();
			} else if (PublicIntent.BROADCAST_USER_COMMENT_NAME_NOTIFICATION
					.equals(intent.getAction())) {
				Long uid = intent.getLongExtra("modifiedUser", -1);
				if (uid == -1l) {
					return;
				}

				Message.obtain(mHandler, UPDATE_USER_SIGN, uid).sendToTarget();
			}
		}

	}



	private BitmapManager.BitmapChangedListener bitmapChangedListener = new BitmapManager.BitmapChangedListener() {

		@Override
		public void notifyAvatarChanged(User user, Bitmap bm) {
			mContactsContainer.updateUser(user);
		}
	};

	private MultilevelListView.MultilevelListViewListener mListener = new MultilevelListView.MultilevelListViewListener() {

		@Override
		public void onItemClicked(AdapterView<?> parent, View view,
				int position, long id, ItemData item) {
			if (item.getObject() instanceof User) {
				Intent i = new Intent(PublicIntent.SHOW_CONTACT_DETAIL_ACTIVITY);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				i.putExtra("uid", ((User) item.getObject()).getmUserId());
				startActivity(i);
			}
		}

		public void onCheckboxClicked(View view, ItemData item) {

		}

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id, ItemData item) {
			// TODO Auto-generated method stub
			return false;
		}
	};

	private class LocalHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case FILL_CONTACTS_GROUP:
				fillContactsGroup();
				break;
			case UPDATE_GROUP_STATUS:
				// Just notify group statist information
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
				mContactsContainer.updateUser(GlobalHolder.getInstance()
						.getUser(uid));
				break;
			}

		}

	}

}
