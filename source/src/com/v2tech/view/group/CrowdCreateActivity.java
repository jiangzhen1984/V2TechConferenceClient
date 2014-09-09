package com.v2tech.view.group;

import java.util.ArrayList;
import java.util.Collections;
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
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.CrowdGroup.AuthType;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.User;

public class CrowdCreateActivity extends Activity {

	private static final int UPDATE_LIST_VIEW = 1;
	private static final int UPDATE_ATTENDEES = 2;
	private static final int UPDATE_SEARCHED_USER_LIST = 3;
	private static final int CREATE_CROWD_RESPONSE = 4;
	private static final int UPDATE_CROWD_RESPONSE = 5;

	private static final int PAD_LAYOUT = 1;
	private static final int PHONE_LAYOUT = 0;

	private Context mContext;
	private LocalHandler mLocalHandler = new LocalHandler();

	private EditText searchedTextET;
	private ListView mContactsContainer;
	private ContactsAdapter adapter = new ContactsAdapter();
	private View mGroupConfirmButton;
	private EditText mGroupTitleET;
	private View mReturnButton;
	private Spinner mRuleSpinner;
	private LinearLayout mErrorNotificationLayout;
	private LinearLayout mAttendeeContainer;

	private View mScroller;

	private boolean mIsStartedSearch;

	private List<ListItem> mItemList = new ArrayList<ListItem>();
	private List<ListItem> mCacheItemList;
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
		mContext = this;
		mContactsContainer = (ListView) findViewById(R.id.group_create_contacts_list);
		mContactsContainer.setOnItemClickListener(itemListener);
		mContactsContainer.setAdapter(adapter);

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
		//call clear to remove callback from JNI.
		cg.clear();
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
				GroupType.CHATING, getIntent().getLongExtra("cid", 0));
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

	// FIXME duplicate code should be merge with conference create and contacts
	// tab fragment
	private void updateView(int pos) {
		ListItem item = mItemList.get(pos);
		if (item.g == null) {
			return;
		}
		if (item.isExpanded == false) {
			for (Group g : item.g.getChildGroup()) {
				ListItem cache = new ListItem(g, g.getLevel());
				mItemList.add(++pos, cache);
			}
			List<User> sortList = new ArrayList<User>();
			sortList.addAll(item.g.getUsers());
			Collections.sort(sortList);
			for (User u : sortList) {
				ListItem cache = new ListItem(u, item.g.getLevel() + 1);
				mItemList.add(++pos, cache);
				updateItem(cache);
			}

		} else {
			if (item.g.getChildGroup().size() <= 0
					&& item.g.getUsers().size() <= 0) {
				return;
			}

			int startRemovePos = pos + 1;
			int endRemovePos = pos;
			for (int index = pos + 1; index < mItemList.size(); index++) {
				ListItem li = mItemList.get(index);
				if (li.g != null && li.g.getLevel() <= item.g.getLevel()) {
					break;
				}
				if (li.u != null && li.level == item.g.getLevel()) {
					break;
				}
				endRemovePos++;
			}

			while (startRemovePos <= endRemovePos
					&& endRemovePos < mItemList.size()) {
				mItemList.remove(startRemovePos);
				endRemovePos--;
			}
		}

		item.isExpanded = !item.isExpanded;
		adapter.notifyDataSetChanged();
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

	private void updateItem(ListItem it) {
		if (it == null || it.u == null) {
			return;
		}

		for (User u : mUserList) {
			if (it.u.getmUserId() == u.getmUserId()) {
				((ContactUserView) it.v).updateChecked();
			}
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

	private void updateSearchedUserList(List<User> lu) {
		mItemList = new ArrayList<ListItem>();
		for (User u : lu) {
			ListItem item = new ListItem(u, -1);
			((ContactUserView) item.v).removePadding();
			mItemList.add(item);
			updateItem(item);
		}
		adapter.notifyDataSetChanged();
	}

	private OnClickListener removeAttendeeListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			User u = (User) view.getTag();
			Message.obtain(mLocalHandler, UPDATE_ATTENDEES, u).sendToTarget();
			for (int index = 0; index < mItemList.size(); index++) {
				ListItem li = mItemList.get(index);
				if (li.u != null && u.getmUserId() == li.u.getmUserId()) {
					((ContactUserView) li.v).updateChecked();
				}
			}
		}

	};

	private TextWatcher textChangedListener = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable s) {
			if (s != null && s.length() > 0) {
				if (!mIsStartedSearch) {
					mCacheItemList = mItemList;
					mIsStartedSearch = true;
				}
			} else {
				if (mIsStartedSearch) {
					mItemList = mCacheItemList;
					adapter.notifyDataSetChanged();
					mIsStartedSearch = false;
				}
				return;
			}
			String str = s == null ? "" : s.toString();
			List<User> searchedUserList = new ArrayList<User>();
			for (Group g : mGroupList) {
				Group.searchUser(str, searchedUserList, g);
			}
			Message.obtain(mLocalHandler, UPDATE_SEARCHED_USER_LIST,
					searchedUserList).sendToTarget();
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

	private OnItemClickListener itemListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int pos,
				long id) {
			ListItem item = mItemList.get(pos);
			if (item.g != null) {
				((ContactGroupView) mItemList.get(pos).v)
						.doExpandedOrCollapse();
				Message.obtain(mLocalHandler, UPDATE_LIST_VIEW, pos, 0)
						.sendToTarget();
			} else {
				ContactUserView cuv = (ContactUserView) view;
				for (ListItem li : mItemList) {
					if (li.u != null
							&& li.u.getmUserId() == cuv.getUser().getmUserId()) {
						((ContactUserView) li.v).updateChecked();
					}
				}
				Message.obtain(mLocalHandler, UPDATE_ATTENDEES, item.u)
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
				cg.inviteMember(crowd,  newMembers, new Registrant(mLocalHandler,
						UPDATE_CROWD_RESPONSE, crowd));
			} else  {
				CrowdGroup crowd = new CrowdGroup(0, mGroupTitleET.getText()
						.toString(), GlobalHolder.getInstance().getCurrentUser(),
						new Date());
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
			mGroupList = GlobalHolder.getInstance().getGroup(GroupType.ORG);
			if (mGroupList != null) {
				for (Group g : mGroupList) {
					mItemList.add(new ListItem(g, g.getLevel()));
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			adapter.notifyDataSetChanged();
		}

	};

	class ListItem {
		long id;
		Group g;
		User u;
		View v;
		boolean isExpanded;
		int level;

		public ListItem(Group g, int level) {
			super();
			this.g = g;
			this.id = 0x02000000 | g.getmGId();
			this.v = new ContactGroupView(mContext, g, null);
			isExpanded = false;
			this.level = level;
		}

		public ListItem(User u, int level) {
			super();
			this.u = u;
			this.id = 0x03000000 | u.getmUserId();
			this.v = new ContactUserView(mContext, u);
			isExpanded = false;
			this.level = level;
		}

	}

	class ContactsAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mItemList.size();
		}

		@Override
		public Object getItem(int position) {
			ListItem item = mItemList.get(position);
			return item.g == null ? item.u : item.g;
		}

		@Override
		public long getItemId(int position) {
			return mItemList.get(position).id;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return mItemList.get(position).v;
		}

	}

	class LocalHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPDATE_LIST_VIEW:
				updateView(msg.arg1);
				break;
			case UPDATE_ATTENDEES:
				updateUserToAttendList((User) msg.obj);
				break;
			case UPDATE_SEARCHED_USER_LIST:
				updateSearchedUserList((List<User>) msg.obj);
				break;
			case CREATE_CROWD_RESPONSE: {
				JNIResponse recr = (JNIResponse) msg.obj;
				if (recr.getResult() == JNIResponse.Result.SUCCESS) {
					CrowdGroup cg = (CrowdGroup) recr.callerObject;
					long id = ((CreateCrowdResponse) recr).getGroupId();
					cg.setGId(id);
					// add group to global cache
					GlobalHolder.getInstance().addGroupToList(
							GroupType.CHATING, cg);
					// send broadcast to inform new crowd notification
					Intent i = new Intent();
					i.setAction(PublicIntent.BROADCAST_NEW_CROWD_NOTIFICATION);
					i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
					i.putExtra("crowd", id);
					mContext.sendBroadcast(i);
					
					
					Intent crowdIntent = new Intent(PublicIntent.SHOW_CROWD_DETAIL_ACTIVITY);
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
