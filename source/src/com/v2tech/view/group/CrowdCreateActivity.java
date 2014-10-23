package com.v2tech.view.group;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
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
import android.text.TextUtils.TruncateAt;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.service.CrowdGroupService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.Registrant;
import com.v2tech.service.jni.CreateCrowdResponse;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.view.JNIService;
import com.v2tech.view.PublicIntent;
import com.v2tech.view.widget.GroupListView;
import com.v2tech.view.widget.GroupListView.Item;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.CrowdGroup.AuthType;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.User;

/**
 * Intent parameters:<br>
 * mode :  true means in invitation mode, otherwise in create mode
 * @author 28851274
 *
 */
public class CrowdCreateActivity extends Activity {

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
	private LocalAdapter mAdapter = new LocalAdapter();

	private EditText searchedTextET;
	private GroupListView mContactsContainer;
	private AdapterView mAttendeeContainer;

	private View mGroupConfirmButton;
	private EditText mGroupTitleET;
	private View mReturnButton;
	private Spinner mRuleSpinner;
	private LinearLayout mErrorNotificationLayout;

	private List<Group> mGroupList;
	private CrowdGroup crowd;
	private CrowdGroupService cg = new CrowdGroupService();

	// Used to save current selected user
	private Set<User> mUserList = new HashSet<User>();
	private List<User> mUserListArray = new ArrayList<User>();

	private int landLayout = PAD_LAYOUT;
	
	private boolean isInInvitationMode = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isScreenLarge()) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		setContentView(R.layout.crowd_create_activity);
				
		mGroupList = new ArrayList<Group>();
		mContext = this;
		mContactsContainer = (GroupListView) findViewById(R.id.group_create_contacts_list);
		mContactsContainer.setListener(listViewListener);
		mContactsContainer.setShowedCheckedBox(true);
		mContactsContainer.setTextFilterEnabled(true);
		mContactsContainer.setIgnoreCurrentUser(true);

		

		mAttendeeContainer = (AdapterView) findViewById(R.id.crowd_create_list_view);
		mAttendeeContainer.setOnItemClickListener(mItemClickedListener);
		mAttendeeContainer.setAdapter(mAdapter);
		landLayout = mAttendeeContainer.getTag().equals("vertical") ? PAD_LAYOUT
				: PHONE_LAYOUT;

		mGroupConfirmButton = (TextView) findViewById(R.id.group_create_confirm_button);
		mGroupConfirmButton.setOnClickListener(confirmButtonListener);

		mGroupTitleET = (EditText) findViewById(R.id.group_create_group_name);
		mRuleSpinner = (Spinner) findViewById(R.id.group_create_group_rule);
		mRuleSpinner.setSelection(0);

		new LoadContactsAT().execute();

		searchedTextET = (EditText) findViewById(R.id.contacts_search);

		mErrorNotificationLayout = (LinearLayout) findViewById(R.id.group_create_error_notification);
		mReturnButton = findViewById(R.id.group_create_return_button);
		mReturnButton.setOnClickListener(mReturnListener);
		loadCache();
		
		isInInvitationMode = getIntent().getBooleanExtra("mode", false);
		if (isInInvitationMode) {
			findViewById(R.id.group_create_input_box).setVisibility(View.GONE);;
			((TextView)findViewById(R.id.crowd_title)).setText(R.string.group_invitation_title);
		}
		
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// call clear to remove callback from JNI.
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
		crowd = (CrowdGroup) GlobalHolder.getInstance().getGroupById(
				GroupType.CHATING.intValue(),
				getIntent().getLongExtra("cid", 0));
		if (crowd != null) {
			mRuleSpinner.setEnabled(false);
			if (crowd.getAuthType() == AuthType.ALLOW_ALL) {
				mRuleSpinner.setSelection(0);
			} else if (crowd.getAuthType() == AuthType.QULIFICATION) {
				mRuleSpinner.setSelection(1);
			} else if (crowd.getAuthType() == AuthType.NEVER) {
				mRuleSpinner.setSelection(2);
			}
			mGroupTitleET.setEnabled(false);
			mGroupTitleET.setText(crowd.getName());
		}
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
		this.mUserListArray.remove(u);
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
		
		this.mUserListArray.add(u);
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
			Message.obtain(mLocalHandler, UPDATE_ATTENDEES, user).sendToTarget();
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
			CheckBox cb = (CheckBox)view;
			Object obj = item.getObject();
			if (obj instanceof User) {
				mContactsContainer.updateCheckItem((User) obj,
						cb.isChecked());
				Message.obtain(mLocalHandler, UPDATE_ATTENDEES, (User) obj)
						.sendToTarget();
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

	private OnClickListener confirmButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			String title = mGroupTitleET.getText().toString();
			if (title == null || title.trim().isEmpty()) {
				mGroupTitleET
						.setError(getString(R.string.error_crowd_title_required));
				mGroupTitleET.requestFocus();
				return;
			}

			if (crowd != null) {
				List<User> users = crowd.getUsers();
				Iterator<User> iterator = mUserList.iterator();
				while (iterator.hasNext()) {
					User checkUser = iterator.next();
					for (User user : users) {
						if(user.getmUserId() == checkUser.getmUserId()){
							mUserList.remove(checkUser);
							break;
						}
					}
				}
				
				List<User> newMembers = new ArrayList<User>(mUserList);
				cg.inviteMember(crowd, newMembers, new Registrant(
						mLocalHandler, UPDATE_CROWD_RESPONSE, crowd));
			} else {
				CrowdGroup crowd = new CrowdGroup(0, mGroupTitleET.getText()
						.toString(), GlobalHolder.getInstance()
						.getCurrentUser(), new Date());
				int pos = mRuleSpinner.getSelectedItemPosition();
				// Position match with R.array.crowd_rules
				if (pos == 0) {
					crowd.setAuthType(CrowdGroup.AuthType.ALLOW_ALL);
				} else if (pos == 1) {
					crowd.setAuthType(CrowdGroup.AuthType.QULIFICATION);
				} else if (pos == 2) {
					crowd.setAuthType(CrowdGroup.AuthType.NEVER);
				}

				List<User> userList = new ArrayList<User>(mUserList);
				//Do not add userList to crowd, because this just invitation.
				cg.createCrowdGroup(crowd, userList, new Registrant(mLocalHandler,
						CREATE_GROUP_MESSAGE, crowd));
				view.setEnabled(false);
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
	
	
	
	class LocalAdapter extends BaseAdapter {
		
		class ViewTag {
			ImageView headIcon;
			TextView name;
		}

		@Override
		public int getCount() {
			return mUserListArray.size();
		}

		@Override
		public Object getItem(int position) {
			return mUserListArray.get(position);
		}

		@Override
		public long getItemId(int position) {
			return mUserListArray.get(position).getmUserId();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewTag tag = null;
			User user = mUserListArray.get(position);
			if (convertView == null) {
				tag = new ViewTag();
				
				if (landLayout == PAD_LAYOUT) {
					convertView = new ContactUserView(mContext, user, false);
					tag.headIcon = ((ContactUserView)convertView).getmPhotoIV();
					tag.name = ((ContactUserView)convertView).getmUserNameTV();
				} else {
					convertView = getAttendeeView(tag, user);
				}
				convertView.setTag(tag);
				
			} else {
				tag = (ViewTag)convertView.getTag();
			}
			
			updateView(tag, user);
			return convertView;
		}
		
		
		private void updateView(ViewTag tag, User user) {
			 
			if (user.getAvatarBitmap() != null) {
				tag.headIcon.setImageBitmap(user.getAvatarBitmap());
			} else {
				tag.headIcon.setImageResource(R.drawable.avatar);
			}
			
			tag.name.setText(user.getName());
		}
		
		
		
		private View getAttendeeView(ViewTag tag, final User u) {
			final LinearLayout ll = new LinearLayout(mContext);
			ll.setOrientation(LinearLayout.VERTICAL);
			ll.setGravity(Gravity.CENTER);

			ImageView iv = new ImageView(mContext);
			tag.headIcon = iv;
			if (u.getAvatarBitmap() != null) {
				iv.setImageBitmap(u.getAvatarBitmap());
			} else {
				iv.setImageResource(R.drawable.avatar);
			}
			ll.addView(iv, new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT));

			TextView tv = new TextView(mContext);
			tag.name = tv;
			
			tv.setText(u.getName());
			tv.setEllipsize(TruncateAt.END);
			tv.setSingleLine(true);
			tv.setTextSize(8);
			tv.setMaxWidth(80);
			ll.addView(tv, new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT));
			ll.setPadding(5, 5, 5, 5);
			return ll;
		}
	}

	class LocalHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPDATE_ATTENDEES:
				updateUserToAttendList((User) msg.obj);
				break;
			case CREATE_GROUP_MESSAGE: {
				JNIResponse recr = (JNIResponse) msg.obj;
				if (recr.getResult() == JNIResponse.Result.SUCCESS) {
					CrowdGroup cg = (CrowdGroup) recr.callerObject;
					long id = ((CreateCrowdResponse) recr).getGroupId();
					cg.setGId(id);
					cg.setCreateDate(new Date());

					// add group to global cache
					GlobalHolder.getInstance().addGroupToList(
							GroupType.CHATING.intValue(), cg);
					//Add self to list
					cg.addUserToGroup(GlobalHolder.getInstance().getCurrentUser());
					// send broadcast to inform new crowd notification
					Intent i = new Intent();
					i.setAction(PublicIntent.BROADCAST_NEW_CROWD_NOTIFICATION);
					i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
					i.putExtra("crowd", id);
					mContext.sendBroadcast(i);

					Intent crowdIntent = new Intent(
							PublicIntent.SHOW_CROWD_DETAIL_ACTIVITY);
					crowdIntent.addCategory(PublicIntent.DEFAULT_CATEGORY);
					crowdIntent.putExtra("cid", id);
					mContext.startActivity(crowdIntent);

					// finish current activity
					finish();
				} else {
					mErrorNotificationLayout.setVisibility(View.VISIBLE);
				}
			}
				break;
			case UPDATE_CROWD_RESPONSE:
				crowd.addUserToGroup(mUserList);
				// finish current activity
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
