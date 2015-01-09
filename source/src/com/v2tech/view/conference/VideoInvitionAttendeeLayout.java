package com.v2tech.view.conference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.TextUtils.TruncateAt;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;

import com.V2.jni.V2GlobalEnum;
import com.v2tech.R;
import com.v2tech.service.GlobalHolder;
import com.v2tech.view.adapter.CreateConfOrCrowdAdapter;
import com.v2tech.view.widget.GroupListView;
import com.v2tech.view.widget.GroupListView.ItemData;
import com.v2tech.vo.Conference;
import com.v2tech.vo.ConferenceGroup;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.User;

public class VideoInvitionAttendeeLayout extends LinearLayout {

	private static final int UPDATE_ATTENDEES = 2;
	private static final int START_GROUP_SELECT = 6;
	private static final int DOING_SELECT_GROUP = 7;
	private static final int END_GROUP_SELECT = 8;

	private static final int PAD_LAYOUT = 1;
	private static final int PHONE_LAYOUT = 0;

	private Context mContext;
	private LocalHandler mLocalHandler = new LocalHandler();

	private EditText searchedTextET;
	// private ListView mContactsContainer;
	private GroupListView mGroupListView;
	private EditText mConfTitleET;
	private EditText mConfStartTimeET;
	private View mInvitionButton;

	private LinearLayout mErrorNotificationLayout;

	private AdapterView<ListAdapter> mAttendeeContainer;
	private CreateConfOrCrowdAdapter mAdapter;

	private List<Group> mGroupList;

	// Used to save current selected user
	private Set<User> mAttendeeList = new HashSet<User>();
	private List<User> mUserListArray = new ArrayList<User>();

	private Conference conf;

	private int landLayout = PAD_LAYOUT;

	private Listener listener;

	public interface Listener {
		public void requestInvitation(Conference conf, List<User> l);
	}

	public VideoInvitionAttendeeLayout(Context context, Conference conf) {
		super(context);
		this.conf = conf;
		mGroupList = new ArrayList<Group>();
		initLayout();
	}

	private void initLayout() {
		mContext = getContext();
		View view = LayoutInflater.from(getContext()).inflate(
				R.layout.video_invition_attendee_layout, null, false);

		mGroupListView = (GroupListView) view
				.findViewById(R.id.conf_create_contacts_list);
		mGroupListView.setShowedCheckedBox(true);
		mGroupListView.setTextFilterEnabled(true);
		mGroupListView.setListener(mListener);
		mGroupListView.setIgnoreCurrentUser(true);

		mAttendeeContainer = (AdapterView<ListAdapter>) view.findViewById(R.id.conference_attendee_container);
		mAttendeeContainer.setOnItemClickListener(mItemClickedListener);
		landLayout = mAttendeeContainer.getTag().equals("vertical") ? PAD_LAYOUT
				: PHONE_LAYOUT;
		mAdapter = new CreateConfOrCrowdAdapter(mContext, mUserListArray,
				landLayout);
		mAttendeeContainer.setAdapter(mAdapter);

		mConfTitleET = (EditText) view
				.findViewById(R.id.conference_create_conf_name);
		mConfTitleET.setEnabled(false);
		mConfStartTimeET = (EditText) view
				.findViewById(R.id.conference_create_conf_start_time);
		mConfStartTimeET.setEnabled(false);
		mConfStartTimeET.setEllipsize(TruncateAt.END);

		searchedTextET = (EditText) view.findViewById(R.id.contacts_search);
		searchedTextET.addTextChangedListener(textChangedListener);

		mErrorNotificationLayout = (LinearLayout) view
				.findViewById(R.id.conference_create_error_notification);
		mInvitionButton = view
				.findViewById(R.id.video_invition_attendee_ly_invition_button);
		mInvitionButton.setOnClickListener(confirmButtonListener);

		this.addView(view, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT));
		initData();

		new LoadContactsAT().execute();
	}

	public void setListener(Listener l) {
		this.listener = l;
	}

	private void initData() {
		mConfTitleET.setText(conf.getName());
		mConfStartTimeET.setText(conf.getStartTimeStr());
	}

	public boolean isScreenLarge() {
		final int screenSize = getResources().getConfiguration().screenLayout
				& Configuration.SCREENLAYOUT_SIZE_MASK;
		return screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	private void updateUserToAttendList(final User u) {
		if (u == null) {
			return;
		}
		boolean remove = false;
		for (User tu : mAttendeeList) {
			if (tu.getmUserId() == u.getmUserId()) {
				mAttendeeList.remove(tu);
				remove = true;
				break;
			}
		}

		if (remove) {
			removeAttendee(u);
		} else {
			addAttendee(u);
		}

	}

	private void removeAttendee(User u) {
		mAttendeeList.remove(u);
		mUserListArray.remove(u);
		mAdapter.notifyDataSetChanged();
	}

	private void addAttendee(User u) {
		if (u.isCurrentLoggedInUser()) {
			return;
		}
		boolean ret = mAttendeeList.add(u);
		if (!ret) {
			return;
		}

		mUserListArray.add(u);
		mAdapter.notifyDataSetChanged();
	}

	private TextWatcher textChangedListener = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable s) {
			if (TextUtils.isEmpty(s.toString())) {
//				if (!TextUtils.isEmpty(mGroupListView.getTextFilter())) {
					mGroupListView.clearTextFilter();
//				}
			} else {
				mGroupListView.setFilterText(s.toString());
			}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {

		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {

		}

	};

	private GroupListView.GroupListViewListener mListener = new GroupListView.GroupListViewListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id, ItemData item) {
			return false;
		}

		@Override
		public void onItemClicked(AdapterView<?> parent, View view,
				int position, long id, ItemData item) {
			Object obj = item.getObject();
			if (obj instanceof User) {
				Message.obtain(mLocalHandler, UPDATE_ATTENDEES, (User) obj)
						.sendToTarget();
				mGroupListView.updateCheckItem((User) obj, !item.isChecked());
			}

		}

		public void onCheckboxClicked(View view, ItemData item) {
			CheckBox cb = (CheckBox) view;
			Object obj = item.getObject();
			if (obj instanceof User) {
				User user = (User) obj;
				Message.obtain(mLocalHandler, UPDATE_ATTENDEES, user)
						.sendToTarget();
				mGroupListView.updateCheckItem(user, !item.isChecked());

				Set<Group> belongsGroup = user.getBelongsGroup();
				for (Group group : belongsGroup) {
					List<User> users = group.getUsers();
					mGroupListView.checkBelongGroupAllChecked(group, users);
				}
			} else {
				Message.obtain(mLocalHandler, START_GROUP_SELECT,
						cb.isChecked() ? 1 : 2, 0, (Group) obj).sendToTarget();
				mGroupListView.updateCheckItem((Group) obj, !item.isChecked());
			}
		}
	};

	private OnClickListener confirmButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			List<User> removeUsers = new ArrayList<User>();
			ConferenceGroup confGroup = (ConferenceGroup) GlobalHolder.getInstance().
					getGroupById(V2GlobalEnum.GROUP_TYPE_CONFERENCE, conf.getId());
			List<User> users = confGroup.getUsers();
			Iterator<User> iterator = mAttendeeList.iterator();
			while (iterator.hasNext()) {
				User checkUser = iterator.next();
				for (User user : users) {
					if (user.getmUserId() == checkUser.getmUserId()) {
						removeUsers.add(checkUser);
						break;
					}
				}
			}

			for (int i = 0; i < removeUsers.size(); i++) {
				mAttendeeList.remove(removeUsers.get(i));
			}
			
			List<User> l = new ArrayList<User>(mAttendeeList);
			
			if (listener != null) {
				listener.requestInvitation(conf, l);
			}

			// Clean
			mGroupListView.updateUserItemcCheck(l , false);
			mAttendeeList.clear();
			mUserListArray.clear();
			if(mAttendeeContainer.getChildCount() > 0){
				mAttendeeContainer.removeAllViewsInLayout();
				mAdapter.notifyDataSetChanged();
			}
		}

	};
	
	private OnItemClickListener mItemClickedListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			User user = mUserListArray.get(position);
			mGroupListView.updateCheckItem(user, false);
			Message.obtain(mLocalHandler, UPDATE_ATTENDEES, user)
					.sendToTarget();
		}

	};

	class LoadContactsAT extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			mGroupList.clear();
			mGroupList.addAll(GlobalHolder.getInstance().getGroup(
					GroupType.CONTACT.intValue()));
			mGroupList.addAll(GlobalHolder.getInstance().getGroup(
					GroupType.ORG.intValue()));

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			mGroupListView.setGroupList(mGroupList);
		}

	};

	private ProgressDialog mWaitingDialog;

	private void selectGroup(Group selectGroup, boolean addOrRemove) {
		List<Group> subGroups = selectGroup.getChildGroup();
		for (int i = 0; i < subGroups.size(); i++) {
			selectGroup(subGroups.get(i), addOrRemove);
		}
		List<User> list = selectGroup.getUsers();
		for (int i = 0; i < list.size(); i++) {
			if (addOrRemove) {
				addAttendee(list.get(i));
			} else {
				removeAttendee(list.get(i));
			}
		}
	}

	class LocalHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPDATE_ATTENDEES:
				updateUserToAttendList((User) msg.obj);
				break;
			case START_GROUP_SELECT: {
				mWaitingDialog = ProgressDialog.show(
						mContext,
						"",
						mContext.getResources().getString(
								R.string.notification_watiing_process), true);
				Message.obtain(this, DOING_SELECT_GROUP, msg.arg1, msg.arg2,
						msg.obj).sendToTarget();
				break;
			}
			case DOING_SELECT_GROUP:
				selectGroup((Group) msg.obj, msg.arg1 == 1 ? true : false);
				Message.obtain(this, END_GROUP_SELECT).sendToTarget();
				break;
			case END_GROUP_SELECT:
				mWaitingDialog.dismiss();
				mWaitingDialog = null;
				break;
			}
		}

	}

}
