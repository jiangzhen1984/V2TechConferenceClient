package com.v2tech.view.group;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
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
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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

public class CrowdCreateActivity extends Activity {

	private static final int UPDATE_ATTENDEES = 2;
	private static final int CREATE_CROWD_RESPONSE = 4;
	private static final int UPDATE_CROWD_RESPONSE = 5;

	private static final int PAD_LAYOUT = 1;
	private static final int PHONE_LAYOUT = 0;

	private Context mContext;
	private LocalHandler mLocalHandler = new LocalHandler();

	private EditText searchedTextET;
	private GroupListView mContactsContainer;

	private View mGroupConfirmButton;
	private EditText mGroupTitleET;
	private View mReturnButton;
	private Spinner mRuleSpinner;
	private LinearLayout mErrorNotificationLayout;
	private LinearLayout mAttendeeContainer;

	private View mScroller;

	private List<Group> mGroupList;
	private CrowdGroup crowd;
	private CrowdGroupService cg = new CrowdGroupService();

	// Used to save current selected user
	private Set<User> mUserList = new HashSet<User>();

	private int landLayout = PAD_LAYOUT;

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

		mAttendeeContainer = (LinearLayout) findViewById(R.id.group_member_container);
		mAttendeeContainer.setGravity(Gravity.CENTER);
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
		mScroller = findViewById(R.id.group_create_scroll_view);
		mReturnButton = findViewById(R.id.group_create_return_button);
		mReturnButton.setOnClickListener(mReturnListener);
		loadCache();
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
		mAttendeeContainer.removeAllViews();
		for (User tmpU : mUserList) {
			addAttendee(tmpU);
		}
	}

	private void addAttendee(User u) {
		if (u.isCurrentLoggedInUser()) {
			return;
		}
		mUserList.add(u);

		View v = null;
		if (landLayout == PAD_LAYOUT) {
			v = new ContactUserView(this, u, false);
			v.setTag(u);
			v.setOnClickListener(removeAttendeeListener);
		} else {
			v = getAttendeeView(u);
		}
		mAttendeeContainer.addView(v);

		if (mAttendeeContainer.getChildCount() > 0) {
			mScroller.postDelayed(new Runnable() {
				@Override
				public void run() {
					View child = mAttendeeContainer
							.getChildAt(mAttendeeContainer.getChildCount() - 1);
					if (landLayout == PAD_LAYOUT) {
						((ScrollView) mScroller).scrollTo(child.getRight(),
								child.getBottom());
					} else {
						((HorizontalScrollView) mScroller).scrollTo(
								child.getRight(), child.getBottom());
					}
				}

			}, 100L);
		}
	}

	/**
	 * Use to add scroll view
	 * 
	 * @param u
	 * @return
	 */
	private View getAttendeeView(final User u) {
		final LinearLayout ll = new LinearLayout(mContext);
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setGravity(Gravity.CENTER);

		ImageView iv = new ImageView(mContext);
		if (u.getAvatarBitmap() != null) {
			iv.setImageBitmap(u.getAvatarBitmap());
		} else {
			iv.setImageResource(R.drawable.avatar);
		}
		ll.addView(iv, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));

		TextView tv = new TextView(mContext);
		tv.setText(u.getName());
		tv.setEllipsize(TruncateAt.END);
		tv.setSingleLine(true);
		tv.setTextSize(8);
		ll.setTag(u.getmUserId() + "");
		ll.addView(tv, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));
		ll.setPadding(5, 5, 5, 5);
		if (u.isCurrentLoggedInUser()) {
			return ll;
		}
		ll.setTag(u);
		ll.setOnClickListener(removeAttendeeListener);

		return ll;
	}

	private OnClickListener removeAttendeeListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			User u = (User) view.getTag();
			Message.obtain(mLocalHandler, UPDATE_ATTENDEES, u).sendToTarget();
			mContactsContainer.updateCheckItem(u, false);
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
				List<User> newMembers = new ArrayList<User>(mUserList);
				cg.inviteMember(crowd, newMembers, new Registrant(
						mLocalHandler, UPDATE_CROWD_RESPONSE, crowd));
			} else {
				CrowdGroup crowd = new CrowdGroup(0, mGroupTitleET.getText()
						.toString(), GlobalHolder.getInstance()
						.getCurrentUser(), new Date());
				crowd.addUserToGroup(mUserList);
				int pos = mRuleSpinner.getSelectedItemPosition();
				// Position match with R.array.crowd_rules
				if (pos == 0) {
					crowd.setAuthType(CrowdGroup.AuthType.ALLOW_ALL);
				} else if (pos == 1) {
					crowd.setAuthType(CrowdGroup.AuthType.QULIFICATION);
				} else if (pos == 2) {
					crowd.setAuthType(CrowdGroup.AuthType.NEVER);
				}

				cg.createCrowdGroup(crowd, new Registrant(mLocalHandler,
						CREATE_CROWD_RESPONSE, crowd));
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

	class LocalHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPDATE_ATTENDEES:
				updateUserToAttendList((User) msg.obj);
				break;
			case CREATE_CROWD_RESPONSE: {
				JNIResponse recr = (JNIResponse) msg.obj;
				if (recr.getResult() == JNIResponse.Result.SUCCESS) {
					CrowdGroup cg = (CrowdGroup) recr.callerObject;
					long id = ((CreateCrowdResponse) recr).getGroupId();
					cg.setGId(id);
					cg.setCreateDate(new Date());

					// add group to global cache
					GlobalHolder.getInstance().addGroupToList(
							GroupType.CHATING.intValue(), cg);
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
			}
		}

	}

}
