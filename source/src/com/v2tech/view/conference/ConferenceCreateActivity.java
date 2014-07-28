package com.v2tech.view.conference;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.service.ConferenceService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.Registrant;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.service.jni.RequestConfCreateResponse;
import com.v2tech.util.SPUtil;
import com.v2tech.util.V2Log;
import com.v2tech.view.PublicIntent;
import com.v2tech.view.cus.DateTimePicker;
import com.v2tech.view.cus.DateTimePicker.OnDateSetListener;
import com.v2tech.vo.Conference;
import com.v2tech.vo.ConferenceGroup;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.User;

public class ConferenceCreateActivity extends Activity {

	private static final int UPDATE_LIST_VIEW = 1;
	private static final int UPDATE_ATTENDEES = 2;
	private static final int UPDATE_SEARCHED_USER_LIST = 3;
	private static final int CREATE_CONFERENC_RESP = 4;
	private static final int DO_PRE_SELECT = 5;
	private static final int START_GROUP_SELECT = 6;
	private static final int DOING_SELECT_GROUP = 7;
	private static final int END_GROUP_SELECT = 8;

	private static final int PAD_LAYOUT = 1;
	private static final int PHONE_LAYOUT = 0;

	private static final int OP_ADD_ALL_GROUP_USER = 1;
	private static final int OP_DEL_ALL_GROUP_USER = 0;

	private Context mContext;
	private LocalHandler mLocalHandler = new LocalHandler();

	private EditText searchedTextET;
	private ListView mContactsContainer;
	private ContactsAdapter adapter = new ContactsAdapter();
	private TextView mConfirmButton;
	private EditText mConfTitleET;
	private EditText mConfStartTimeET;
	private View mReturnButton;
	private TextView mErrorMessageTV;

	private LinearLayout mErrorNotificationLayout;

	private LinearLayout mAttendeeContainer;

	private View mScroller;

	private boolean mIsStartedSearch;

	private List<ListItem> mItemList = new ArrayList<ListItem>();
	private List<ListItem> mCacheItemList;
	private List<Group> mGroupList;

	// Used to save current selected user
	private Set<User> mAttendeeList = new HashSet<User>();

	private ConferenceService cs = new ConferenceService();

	private Conference conf;

	private int landLayout = PAD_LAYOUT;

	private long preSelectedUID;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isScreenLarge()) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}

		setContentView(R.layout.activity_conference_create);
		mContext = this;
		mContactsContainer = (ListView) findViewById(R.id.conf_create_contacts_list);
		mContactsContainer.setOnItemClickListener(itemListener);
		mContactsContainer.setOnItemLongClickListener(itemLongClickListener);

		mAttendeeContainer = (LinearLayout) findViewById(R.id.conference_attendee_container);
		mAttendeeContainer.setGravity(Gravity.CENTER);
		landLayout = mAttendeeContainer.getTag().equals("vertical") ? PAD_LAYOUT
				: PHONE_LAYOUT;

		mConfirmButton = (TextView) findViewById(R.id.conference_create_confirm_button);
		mConfirmButton.setOnClickListener(confirmButtonListener);

		mConfTitleET = (EditText) findViewById(R.id.conference_create_conf_name);
		mConfStartTimeET = (EditText) findViewById(R.id.conference_create_conf_start_time);
		mConfStartTimeET.setOnTouchListener(mDateTimePickerListener);
		// mConfStartTimeET.setOnClickListener(mDateTimePickerListener);
		// mConfStartTimeET.setInputType(InputType.TYPE_NULL);

		new LoadContactsAT().execute();

		searchedTextET = (EditText) findViewById(R.id.contacts_search);

		mErrorNotificationLayout = (LinearLayout) findViewById(R.id.conference_create_error_notification);
		mScroller = findViewById(R.id.conf_create_scroll_view);
		mReturnButton = findViewById(R.id.conference_create_return_button);
		mReturnButton.setOnClickListener(mReturnListener);

		mErrorMessageTV = (TextView) findViewById(R.id.conference_create_error_notification_tv);
		preSelectedUID = getIntent().getLongExtra("uid", 0);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
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
		return screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		setResult(Activity.RESULT_CANCELED, null);
		// finish();
	}

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

	private void updateUserToAttendList(User u, int op) {
		if (u == null) {
			return;
		}
		if (op == OP_DEL_ALL_GROUP_USER) {
			removeAttendee(u);
		} else if (op == OP_ADD_ALL_GROUP_USER) {
			addAttendee(u);
		}

	}

	private void removeAttendee(User u) {
		boolean ret = mAttendeeList.remove(u);
		if (ret) {
			for (int i = 0; i < mAttendeeContainer.getChildCount(); i++) {
				User tagU = (User) mAttendeeContainer.getChildAt(i).getTag();
				if (tagU.getmUserId() == u.getmUserId()) {
					mAttendeeContainer.removeViewAt(i);
					break;
				}
			}
		}
		// mAttendeeContainer.removeAllViews();
		// for (User tmpU : mAttendeeList) {
		// addAttendee(tmpU);
		// }
	}

	private void updateItem(ListItem it) {
		if (it == null || it.u == null) {
			return;
		}

		for (User u : mAttendeeList) {
			if (it.u.getmUserId() == u.getmUserId()) {
				((ContactUserView) it.v).updateChecked();
			}
		}
	}

	private void addAttendee(User u) {
		if (u.isCurrentLoggedInUser()) {
			return;
		}
		boolean ret = mAttendeeList.add(u);
		if (!ret) {
			return;
		}

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
					if (mAttendeeContainer.getChildCount() <= 0) {
						return;
					}
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
		tv.setTextSize(12);
		tv.setMaxWidth(80);
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

	private void doPreSelect() {
		User preUser = GlobalHolder.getInstance().getUser(preSelectedUID);
		if (preUser == null) {
			V2Log.e("Dose not find pre-selected user");
			return;
		}

		List<Group> li = new ArrayList<Group>();
		Group userGroup = null;
		if (preUser.getBelongsGroup().size() > 0) {
			userGroup = preUser.getBelongsGroup().iterator().next();
		}
		while (true) {
			if (userGroup == null) {
				break;
			}
			li.add(0, userGroup);
			userGroup = userGroup.getParent();
		}

		for (int i = 0; i < li.size(); i++) {
			Group g = li.get(i);
			for (int j = 0; j < mItemList.size(); j++) {
				ListItem item = mItemList.get(j);
				if (item.g != null && g.getmGId() == item.g.getmGId()) {
					item.isExpanded = true;
					for (Group subGroup : item.g.getChildGroup()) {
						ListItem cache = new ListItem(subGroup,
								subGroup.getLevel());
						mItemList.add(++j, cache);
					}
					List<User> sortList = new ArrayList<User>();
					sortList.addAll(item.g.getUsers());
					Collections.sort(sortList);
					for (User u : sortList) {
						ListItem cache = new ListItem(u, item.g.getLevel() + 1);
						mItemList.add(++j, cache);
					}

					break;
				}
			}
		}

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
		
		boolean startFlag = false;
		for (int i =0; i < mItemList.size(); i++) {
			ListItem item = mItemList.get(i);
			if (item.g != null && item.g.getmGId() == selectGroup.getmGId()) {
				startFlag = true;
				continue;
			}
			
			if (startFlag ) {
				if (item.u != null) {
					((ContactUserView)item.v).updateChecked();
				} else if (item.g != null && item.g.getParent() == selectGroup.getParent()) {
					startFlag = false;
				}
			}
		}

	}

	private OnClickListener removeAttendeeListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			User u = (User) view.getTag();
			int flag = 0;
			for (int index = 0; index < mItemList.size(); index++) {
				ListItem li = mItemList.get(index);
				if (li.u != null && u.getmUserId() == li.u.getmUserId()) {
					if (((ContactUserView) li.v).isChecked()) {
						flag = OP_ADD_ALL_GROUP_USER;
					} else {
						flag = OP_DEL_ALL_GROUP_USER;
					}
					((ContactUserView) li.v).updateChecked();
				}
			}
			Message.obtain(mLocalHandler, UPDATE_ATTENDEES, flag, 0, u)
					.sendToTarget();
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

	private OnItemLongClickListener itemLongClickListener = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int pos, long id) {
			ListItem item = mItemList.get(pos);
			if (item.g != null) {
				ContactGroupView cgv = ((ContactGroupView) mItemList.get(pos).v);
				cgv.updateChecked();
				Message.obtain(
						mLocalHandler,
						START_GROUP_SELECT,
						cgv.isChecked() ? OP_ADD_ALL_GROUP_USER
								: OP_DEL_ALL_GROUP_USER, 0, item.g)
						.sendToTarget();
				return true;
			}
			return false;
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
				int flag = 0;
				for (ListItem li : mItemList) {
					if (li.u != null
							&& li.u.getmUserId() == cuv.getUser().getmUserId()) {
						if (((ContactUserView) li.v).isChecked()) {
							flag = OP_DEL_ALL_GROUP_USER;
						} else {
							flag = OP_ADD_ALL_GROUP_USER;
						}

						((ContactUserView) li.v).updateChecked();
					}
				}
				Message.obtain(mLocalHandler, UPDATE_ATTENDEES, flag, 0, item.u)
						.sendToTarget();
			}
		}

	};

	private OnClickListener confirmButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			if (!SPUtil.checkCurrentAviNetwork(mContext)) {
				mErrorNotificationLayout.setVisibility(View.VISIBLE);
				mErrorMessageTV
						.setText(R.string.error_create_conference_failed_no_network);
				return;
			}
			String title = mConfTitleET.getText().toString().trim();
			if (title == null || title.length() == 0) {
				mConfTitleET
						.setError(getString(R.string.error_conf_title_required));
				mConfTitleET.requestFocus();
				return;
			}
			String startTimeStr = mConfStartTimeET.getText().toString();
			if (startTimeStr == null || startTimeStr.length() == 0) {
				mConfStartTimeET
						.setError(getString(R.string.error_conf_start_time_required));
				mConfStartTimeET.requestFocus();
				return;
			}

			DateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm",
					Locale.CHINESE);

			Date st = null;
			try {
				st = sd.parse(startTimeStr);
			} catch (ParseException e) {
				e.printStackTrace();
				mConfStartTimeET
						.setError(getString(R.string.error_conf_start_time_format_failed));
				mConfStartTimeET.requestFocus();
				return;
			}
			if ((new Date().getTime() / 60000) - (st.getTime() / 60000) > 10) {
				mConfStartTimeET
						.setError(getString(R.string.error_conf_do_not_permit_create_piror_conf));
				mConfStartTimeET.requestFocus();
				return;
			}

			List<User> l = new ArrayList<User>(mAttendeeList);
			conf = new Conference(title, startTimeStr, null, l);
			cs.createConference(conf, new Registrant(mLocalHandler,
					CREATE_CONFERENC_RESP, null));
			view.setEnabled(false);
		}

	};

	private DateTimePicker dtp;
	private OnTouchListener mDateTimePickerListener = new OnTouchListener() {

		@Override
		public boolean onTouch(final View view, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_UP) {

				if (dtp == null) {
					dtp = new DateTimePicker(mContext,
							ViewGroup.LayoutParams.WRAP_CONTENT,
							ViewGroup.LayoutParams.WRAP_CONTENT);
					dtp.setOnDateSetListener(new OnDateSetListener() {

						@Override
						public void onDateTimeSet(int year, int monthOfYear,
								int dayOfMonth, int hour, int minute) {
							((EditText) view).setText(year
									+ "-"
									+ monthOfYear
									+ "-"
									+ dayOfMonth
									+ " "
									+ (hour < 10 ? ("0" + hour) : (hour + ""))
									+ ":"
									+ (minute < 10 ? ("0" + minute)
											: (minute + "")));
						}

					});
				}

				dtp.showAsDropDown(view);
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
				mConfStartTimeET.setError(null);

			}
			return true;
		}

	};

	private OnClickListener mReturnListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			onBackPressed();
		}

	};

	/**
	 * TODO add support for horizontal
	 */
	private OnClickListener mGroupCheckBoxListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
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

			doPreSelect();
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (preSelectedUID > 0) {
				ListItem item = null;
				User preUser = GlobalHolder.getInstance().getUser(
						preSelectedUID);
				for (int j = 0; j < mItemList.size(); j++) {
					item = mItemList.get(j);
					if (item.u != null
							&& preUser.getmUserId() == item.u.getmUserId()) {
						mContactsContainer.performItemClick(item.v, j,
								item.u.getmUserId());
						break;
					}
				}
			}

			adapter.notifyDataSetChanged();
			mContactsContainer.setAdapter(adapter);
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
			this.v = new ContactGroupView(mContext, g, null,
					mGroupCheckBoxListener);
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
				updateUserToAttendList((User) msg.obj, msg.arg1);
				break;
			case UPDATE_SEARCHED_USER_LIST:
				updateSearchedUserList((List<User>) msg.obj);
				break;
			case CREATE_CONFERENC_RESP:
				JNIResponse rccr = (JNIResponse) msg.obj;
				if (rccr.getResult() != JNIResponse.Result.SUCCESS) {
					mErrorNotificationLayout.setVisibility(View.VISIBLE);
					mErrorMessageTV
							.setText(R.string.error_create_conference_failed_from_server_side);
					break;
				}
				User currU = GlobalHolder.getInstance().getCurrentUser();
				ConferenceGroup g = new ConferenceGroup(
						((RequestConfCreateResponse) rccr).getConfId(),
						GroupType.CONFERENCE, conf.getName(),
						currU.getmUserId() + "", conf.getDate().getTime()
								/ 1000 + "", currU.getmUserId());
				g.setOwnerUser(currU);
				g.setChairManUId(currU.getmUserId());
				g.addUserToGroup(new ArrayList<User>(mAttendeeList));
				GlobalHolder.getInstance().addGroupToList(GroupType.CONFERENCE,
						g);
				Intent i = new Intent();
				i.putExtra("newGid", g.getmGId());
				i.setAction(PublicIntent.BROADCAST_NEW_CONFERENCE_NOTIFICATION);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				mContext.sendBroadcast(i);
				finish();
				break;
			case DO_PRE_SELECT:
				doPreSelect();
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
