package com.v2tech.view.conference;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.service.ConferenceService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.MessageListener;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.service.jni.RequestConfCreateResponse;
import com.v2tech.util.SPUtil;
import com.v2tech.view.JNIService;
import com.v2tech.view.PublicIntent;
import com.v2tech.view.bo.UserStatusObject;
import com.v2tech.view.cus.DateTimePicker;
import com.v2tech.view.cus.DateTimePicker.OnDateSetListener;
import com.v2tech.view.widget.GroupListView;
import com.v2tech.view.widget.GroupListView.Item;
import com.v2tech.vo.Conference;
import com.v2tech.vo.ConferenceGroup;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.User;

public class ConferenceCreateActivity extends Activity {

	private static final int UPDATE_ATTENDEES = 2;
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

	private com.v2tech.view.widget.GroupListView mGroupListView;

	private TextView mConfirmButton;
	private TextView mReturnButton;
	private EditText mConfTitleET;
	private EditText mConfStartTimeET;
	private TextView mErrorMessageTV;

	private LinearLayout mErrorNotificationLayout;

	private LinearLayout mAttendeeContainer;

	private View mScroller;

	// Used to save current selected user
	private Set<User> mAttendeeList = new HashSet<User>();

	private ConferenceService cs = new ConferenceService();

	private Conference conf;

	private int landLayout = PAD_LAYOUT;

	private long preSelectedUID;
	private long preSelectedGroupId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isScreenLarge()) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}

		setContentView(R.layout.activity_conference_create);

		initReceiver();

		mContext = this;

		mGroupListView = (GroupListView) findViewById(R.id.conf_create_contacts_list);
		mGroupListView.setShowedCheckedBox(true);
		mGroupListView.setTextFilterEnabled(true);
		mGroupListView.setListener(listViewListener);
		mGroupListView.setIgnoreCurrentUser(true);

		mAttendeeContainer = (LinearLayout) findViewById(R.id.conference_attendee_container);
		mAttendeeContainer.setGravity(Gravity.CENTER);
		landLayout = mAttendeeContainer.getTag().equals("vertical") ? PAD_LAYOUT
				: PHONE_LAYOUT;

		
		TextView titleContent = (TextView) findViewById(R.id.ws_common_activity_title_content);
		titleContent.setText(R.string.conference_create_title);
		
		mConfirmButton = (TextView) findViewById(R.id.ws_common_activity_title_right_button);
		mConfirmButton.setText(R.string.conference_create_confirm);
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
		mReturnButton = (TextView) findViewById(R.id.ws_common_activity_title_left_button);
		mReturnButton.setText(R.string.conference_create_cancel);
		mReturnButton.setOnClickListener(mReturnListener);

		mErrorMessageTV = (TextView) findViewById(R.id.conference_create_error_notification_tv);
		preSelectedUID = getIntent().getLongExtra("uid", 0);

		preSelectedUID = getIntent().getLongExtra("uid", 0);
		preSelectedGroupId = getIntent().getLongExtra("gid", 0);
		if (preSelectedUID > 0 || preSelectedGroupId > 0) {
			Message msg = Message.obtain(mLocalHandler, DO_PRE_SELECT);
			//TODO optimze code, if doesn't load group list yet, need to wait
			mLocalHandler.sendMessageDelayed(msg, 300);
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		cs.clearCalledBack();
		this.unregisterReceiver(receiver);
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

	private void doPreSelect() {
		User preUser = GlobalHolder.getInstance().getUser(preSelectedUID);
		if (preUser != null) {
			Message.obtain(mLocalHandler, UPDATE_ATTENDEES,
					OP_ADD_ALL_GROUP_USER, 0, preUser).sendToTarget();
			mGroupListView.selectUser(preUser);
		}

		Group preGroup = GlobalHolder.getInstance().getGroupById(
				preSelectedGroupId);
		if (preGroup != null) {
			mGroupListView.selectUser(preGroup.getUsers());
			Message.obtain(mLocalHandler, START_GROUP_SELECT,
					OP_ADD_ALL_GROUP_USER, 0, preGroup).sendToTarget();
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
			User  u = list.get(i);
			if (u.getmUserId() == GlobalHolder.getInstance().getCurrentUserId()) {
				continue;
			}
			if (addOrRemove) {
				addAttendee(u);
			} else {
				removeAttendee(u);
			}
		}
	}

	private OnClickListener removeAttendeeListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			User u = (User) view.getTag();
			mGroupListView.updateCheckItem(u, false);
			Message.obtain(mLocalHandler, UPDATE_ATTENDEES,
					OP_DEL_ALL_GROUP_USER, 0, u).sendToTarget();
		}

	};

	private TextWatcher textChangedListener = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable s) {

			if (s.toString().isEmpty()) {
				mGroupListView.clearTextFilter();
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

	private GroupListView.GroupListViewListener listViewListener = new GroupListView.GroupListViewListener() {

		@Override
		public void onItemClicked(AdapterView<?> parent, View view,
				int position, long id, Item item) {

		}

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id, Item item) {
			Object obj = item.getObject();

			return true;
		}

		public void onCheckboxClicked(View view, Item item) {
			CheckBox cb = (CheckBox) view;
			int flag = -1;
			Object obj = item.getObject();
			if (obj instanceof User) {
				if (!cb.isChecked()) {
					flag = OP_DEL_ALL_GROUP_USER;
				} else {
					flag = OP_ADD_ALL_GROUP_USER;
				}
				
				User user = (User) obj;
				Message.obtain(mLocalHandler, UPDATE_ATTENDEES, flag, 0,
						user).sendToTarget();
				mGroupListView.updateCheckItem(user, cb.isChecked());
				
				Set<Group> belongsGroup = user.getBelongsGroup();
				for (Group group : belongsGroup) {
					List<User> users = group.getUsers();
					mGroupListView.checkBelongGroupAllChecked(group , users);
				}
			} else if (obj instanceof Group) {
				Message.obtain(
						mLocalHandler,
						START_GROUP_SELECT,
						cb.isChecked() ? OP_ADD_ALL_GROUP_USER
								: OP_DEL_ALL_GROUP_USER, 0, (Group) obj)
						.sendToTarget();
				mGroupListView.updateCheckItem((Group) obj, cb.isChecked());
			}
		}

	};

	IntentFilter intentFilter;
	LocalBroadcastReceiver receiver = new LocalBroadcastReceiver();

	private void initReceiver() {
		intentFilter = new IntentFilter();
		intentFilter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		intentFilter.addCategory(PublicIntent.DEFAULT_CATEGORY);
		intentFilter
				.addAction(JNIService.JNI_BROADCAST_USER_STATUS_NOTIFICATION);
		this.registerReceiver(receiver, intentFilter);
	}

	class LocalBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (JNIService.JNI_BROADCAST_USER_STATUS_NOTIFICATION.equals(intent
					.getAction())) {
				UserStatusObject uso = (UserStatusObject) intent.getExtras()
						.get("status");
				User.Status us = User.Status.fromInt(uso.getStatus());
				User user = GlobalHolder.getInstance().getUser(uso.getUid());
				mGroupListView.updateUserStatus(user, us);
			}
		}
	}

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
			cs.createConference(conf, new MessageListener(mLocalHandler,
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

	private List<Group> mList = new ArrayList<Group>();

	class LoadContactsAT extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {

			mList.addAll(GlobalHolder.getInstance().getGroup(
					GroupType.CONTACT.intValue()));
			mList.addAll(GlobalHolder.getInstance().getGroup(
					GroupType.ORG.intValue()));

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			mGroupListView.setGroupList(mList);
		}

	};

	class LocalHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPDATE_ATTENDEES:
				updateUserToAttendList((User) msg.obj, msg.arg1);
				break;
			case CREATE_CONFERENC_RESP:
				JNIResponse rccr = (JNIResponse) msg.obj;
				if (rccr.getResult() != JNIResponse.Result.SUCCESS) {
					mErrorNotificationLayout.setVisibility(View.VISIBLE);
					mErrorMessageTV
							.setText(R.string.error_create_conference_failed_from_server_side);
					break;
				}

				RequestConfCreateResponse rc = (RequestConfCreateResponse) rccr;
				ConferenceGroup g = new ConferenceGroup(rc.getConfId(),
						conf.getName(), GlobalHolder.getInstance()
								.getCurrentUser(), new Date(), GlobalHolder
								.getInstance().getCurrentUser());

				g.addUserToGroup(new ArrayList<User>(mAttendeeList));
				GlobalHolder.getInstance().addGroupToList(
						GroupType.CONFERENCE.intValue(), g);
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

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		View v = getCurrentFocus();
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
		}
		return super.dispatchTouchEvent(ev);
	}
}
