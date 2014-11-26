package com.v2tech.view.group;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.service.CrowdGroupService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.MessageListener;
import com.v2tech.service.jni.CreateDiscussionBoardResponse;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.view.JNIService;
import com.v2tech.view.PublicIntent;
import com.v2tech.view.adapter.CreateConfOrCrowdAdapter;
import com.v2tech.view.widget.GroupListView;
import com.v2tech.view.widget.GroupListView.Item;
import com.v2tech.vo.DiscussionGroup;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.User;

/**
 * Intent parameters:<br>
 * mode : true means in invitation mode, otherwise in create mode
 * 
 * @see PublicIntent#START_DISCUSSION_BOARD_CREATE_ACTIVITY
 * @author 28851274
 * 
 */
public class DiscussionBoardCreateActivity extends Activity {

	private static final int UPDATE_ATTENDEES = 2;
	private static final int CREATE_GROUP_MESSAGE = 4;
	private static final int UPDATE_CROWD_RESPONSE = 5;
	private static final int START_GROUP_SELECT = 6;
	private static final int DOING_SELECT_GROUP = 7;
	private static final int END_GROUP_SELECT = 8;

	private static final int OP_ADD_ALL_GROUP_USER = 1;
	private static final int OP_DEL_ALL_GROUP_USER = 0;

	private static final int PAD_LAYOUT = 1;
	private static final int PHONE_LAYOUT = 0;

	private Context mContext;
	private LocalHandler mLocalHandler = new LocalHandler();
	private CreateConfOrCrowdAdapter mAdapter;

	private EditText searchedTextET;
	private GroupListView mContactsContainer;
	private AdapterView<ListAdapter> mAttendeeContainer;

	private TextView mGroupConfirmButton;
	private TextView mReturnButton;

	private List<Group> mGroupList;
	private DiscussionGroup crowd;
	private CrowdGroupService cg = new CrowdGroupService();

	// Used to save current selected user
	private Set<User> mUserList = new HashSet<User>();
	private List<User> mUserListArray = new ArrayList<User>();

	private boolean isInInvitationMode = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isScreenLarge()) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		setContentView(R.layout.discussion_board_create_activity);
		
		TextView mTitle = (TextView) findViewById(R.id.ws_common_activity_title_content);
		mTitle.setText(R.string.discussion_create_activity_title);

		mGroupList = new ArrayList<Group>();
		mContext = this;
		mContactsContainer = (GroupListView) findViewById(R.id.discussion_board_create_contacts_list);
		mContactsContainer.setListener(listViewListener);
		mContactsContainer.setShowedCheckedBox(true);
		mContactsContainer.setTextFilterEnabled(true);
		mContactsContainer.setIgnoreCurrentUser(true);

		mAttendeeContainer = (AdapterView<ListAdapter>) findViewById(R.id.discussion_board_create_list_view);
		mAttendeeContainer.setOnItemClickListener(mItemClickedListener);
		mAdapter = new CreateConfOrCrowdAdapter(mContext, mUserListArray,
				mAttendeeContainer.getTag().equals("vertical") ? PAD_LAYOUT
						: PHONE_LAYOUT);
		mAttendeeContainer.setAdapter(mAdapter);

		mGroupConfirmButton = (TextView) findViewById(R.id.ws_common_activity_title_right_button);
		mGroupConfirmButton.setText(R.string.common_confirm_name);
		mGroupConfirmButton.setOnClickListener(confirmButtonListener);

		new LoadContactsAT().execute();

		searchedTextET = (EditText) findViewById(R.id.contacts_search);

		mReturnButton = (TextView) findViewById(R.id.ws_common_activity_title_left_button);
		mReturnButton.setText(R.string.common_return_name);
		mReturnButton.setOnClickListener(mReturnListener);
		loadCache();

		isInInvitationMode = getIntent().getBooleanExtra("mode", false);
		if (isInInvitationMode) {
			mTitle.setText(R.string.discussion_create_invitation_title);
		}

	}



	@Override
	protected void onDestroy() {
		super.onDestroy();
		cg.clearCalledBack();
	}

	@Override
	protected void onStart() {
		super.onStart();
		searchedTextET.addTextChangedListener(textChangedListener);
	}

	@Override
	protected void onStop() {
		super.onStop();
		searchedTextET.removeTextChangedListener(textChangedListener);
	}

	public boolean isScreenLarge() {
		final int screenSize = getResources().getConfiguration().screenLayout
				& Configuration.SCREENLAYOUT_SIZE_MASK;
		return screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE
				|| screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		setResult(Activity.RESULT_CANCELED, null);
	}

	/**
	 * For invite new member action
	 */
	private void loadCache() {
		crowd = (DiscussionGroup) GlobalHolder.getInstance().getGroupById(
				GroupType.DISCUSSION.intValue(),
				getIntent().getLongExtra("cid", 0));
	}

	private void updateUserToAttendList(final User u) {
		if (u == null) {
			return;
		}
		boolean remove = false;
		for (User tu : mUserList) {
			if (tu.getmUserId() == u.getmUserId()) {
				mUserList.remove(tu);
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
		mUserList.remove(u);
		mUserListArray.remove(u);
		mAdapter.notifyDataSetChanged();
	}

	private void addAttendee(User u) {
		if (u.isCurrentLoggedInUser()) {
			return;
		}
		boolean ret = mUserList.add(u);
		if (!ret) {
			return;
		}

		mUserListArray.add(u);
		mAdapter.notifyDataSetChanged();
	}

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

	private OnItemClickListener mItemClickedListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			User user = mUserListArray.get(position);
			mContactsContainer.updateCheckItem(user, false);
			Message.obtain(mLocalHandler, UPDATE_ATTENDEES, user)
					.sendToTarget();
		}

	};

	private TextWatcher textChangedListener = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable s) {
			if (s.toString().isEmpty()) {
				mContactsContainer.clearTextFilter();
			} else {
				mContactsContainer.setFilterText(s.toString());
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

	private GroupListView.GroupListViewListener listViewListener = new GroupListView.GroupListViewListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id, Item item) {
			return false;
		}

		@Override
		public void onItemClicked(AdapterView<?> parent, View view,
				int position, long id, Item item) {

		}

		public void onCheckboxClicked(View view, Item item) {
			CheckBox cb = (CheckBox) view;
			Object obj = item.getObject();
			if (obj instanceof User) {
				User user = (User) obj;
				mContactsContainer.updateCheckItem((User) obj, cb.isChecked());
				Message.obtain(mLocalHandler, UPDATE_ATTENDEES, (User) obj)
						.sendToTarget();

				Set<Group> belongsGroup = user.getBelongsGroup();
				for (Group group : belongsGroup) {
					List<User> users = group.getUsers();
					mContactsContainer.checkBelongGroupAllChecked(group, users);
				}
			} else if (obj instanceof Group) {
				Message.obtain(
						mLocalHandler,
						START_GROUP_SELECT,
						cb.isChecked() ? OP_ADD_ALL_GROUP_USER
								: OP_DEL_ALL_GROUP_USER, 0, (Group) obj)
						.sendToTarget();
				mContactsContainer.updateCheckItem((Group) obj, cb.isChecked());
			}
		}

	};

	private ProgressDialog mCreateWaitingDialog;

	private OnClickListener confirmButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			if (!GlobalHolder.getInstance().isServerConnected()) {
				Toast.makeText(DiscussionBoardCreateActivity.this,
						R.string.error_discussion_no_network,
						Toast.LENGTH_SHORT).show();
				return;
			}
			if (isInInvitationMode) {
				List<User> newMembers = new ArrayList<User>(mUserList);
				cg.inviteMember(crowd, newMembers, new MessageListener(
						mLocalHandler, UPDATE_CROWD_RESPONSE, crowd));
			} else {
				List<User> userList = new ArrayList<User>(mUserList);

				DiscussionGroup crowd = new DiscussionGroup(0, "",
						GlobalHolder.getInstance().getCurrentUser(), new Date());

				if (mCreateWaitingDialog != null
						&& mCreateWaitingDialog.isShowing()) {
					mCreateWaitingDialog.dismiss();
				}
				mCreateWaitingDialog = ProgressDialog.show(
						mContext,
						"",
						mContext.getResources().getString(
								R.string.notification_watiing_process), true);

				// Do not add userList to crowd, because this just invitation.
				cg.createDiscussionBoard(crowd, userList, new MessageListener(
						mLocalHandler, CREATE_GROUP_MESSAGE, crowd));
			}
		}

	};

	private OnClickListener mReturnListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			onBackPressed();
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
			mContactsContainer.setGroupList(mGroupList);
		}

	};

	class LocalHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPDATE_ATTENDEES:
				updateUserToAttendList((User) msg.obj);
				break;
			case CREATE_GROUP_MESSAGE: {
				mCreateWaitingDialog.dismiss();
				JNIResponse recr = (JNIResponse) msg.obj;
				if (recr.getResult() == JNIResponse.Result.SUCCESS) {
					DiscussionGroup cg = (DiscussionGroup) recr.callerObject;
					long id = ((CreateDiscussionBoardResponse) recr)
							.getGroupId();
					cg.setGId(id);
					cg.setCreateDate(new Date());

					// add group to global cache
					GlobalHolder.getInstance().addGroupToList(
							GroupType.DISCUSSION.intValue(), cg);
					// Add self to list
					cg.addUserToGroup(GlobalHolder.getInstance()
							.getCurrentUser());
					cg.addUserToGroup(mUserList);

					
					Intent i = new Intent();
					i.setAction(JNIService.JNI_BROADCAST_NEW_DISCUSSION_NOTIFICATION);
					i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
					i.putExtra("gid", cg.getmGId());
					sendBroadcast(i);
					

					Intent crowdIntent = new Intent(
							PublicIntent.SHOW_DISCUSSION_BOARD_DETAIL_ACTIVITY);
					crowdIntent.addCategory(PublicIntent.DEFAULT_CATEGORY);
					crowdIntent.putExtra("cid", id);
					mContext.startActivity(crowdIntent);

					// finish current activity
					finish();
				} else if (recr.getResult() == JNIResponse.Result.TIME_OUT) {
					cg.setmPendingCrowdId(0);
					V2Log.e("CrowdCreateActivity CREATE_GROUP_MESSAGE --> create discussion group failed.. time out!!");
				} else {
					V2Log.e("CrowdCreateActivity CREATE_GROUP_MESSAGE --> create discussion group failed.. ERROR CODE IS : "
							+ recr.getResult().name());
				}
			}
				break;
			case UPDATE_CROWD_RESPONSE:
				crowd.addUserToGroup(mUserList);
				setResult(Activity.RESULT_OK);
				finish();
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
				selectGroup((Group) msg.obj,
						msg.arg1 == OP_ADD_ALL_GROUP_USER ? true : false);
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
