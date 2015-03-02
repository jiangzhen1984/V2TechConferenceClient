package com.bizcom.vc.activity.conference;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.V2.jni.util.V2Log;
import com.bizcom.bo.UserStatusObject;
import com.bizcom.request.V2ConferenceRequest;
import com.bizcom.request.jni.JNIResponse;
import com.bizcom.request.jni.RequestConfCreateResponse;
import com.bizcom.request.util.MessageListener;
import com.bizcom.util.DateUtil;
import com.bizcom.vc.activity.BaseCreateActivity;
import com.bizcom.vc.application.GlobalConfig;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.application.PublicIntent;
import com.bizcom.vc.service.JNIService;
import com.bizcom.vc.widget.MultilevelListView.ItemData;
import com.bizcom.vc.widget.cus.DateTimePicker;
import com.bizcom.vc.widget.cus.DateTimePicker.OnDateSetListener;
import com.bizcom.vo.Conference;
import com.bizcom.vo.ConferenceGroup;
import com.bizcom.vo.Group;
import com.bizcom.vo.Group.GroupType;
import com.bizcom.vo.User;
import com.v2tech.R;

public class ConferenceCreateActivity extends BaseCreateActivity {

	private static final int CREATE_CONFERENC_RESP = 4;

	private static final int OP_ADD_ALL_GROUP_USER = 1;
	private static final int OP_DEL_ALL_GROUP_USER = 0;

	private LocalHandler mLocalHandler = new LocalHandler();

	private EditText mConfTitleET;
	private EditText mConfStartTimeET;

	private V2ConferenceRequest cs = new V2ConferenceRequest();
	private Conference conf;

	private long preSelectedUID;
	private long preSelectedGroupId;

	private ProgressDialog mWaitingDialog;

	private LocalBroadcastReceiver receiver;

	private DateTimePicker dtp;
	
	/**
	 * show all group org
	 */
	private List<Group> mList = new ArrayList<Group>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.initCreateType(BaseCreateActivity.CREATE_LAYOUT_TYPE_CONFERENCE);
		super.onCreate(savedInstanceState);
		initReceiver();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		cs.clearCalledBack();
		this.unregisterReceiver(receiver);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		setResult(Activity.RESULT_CANCELED, null);
	}

	@Override
	protected void init() {
		mConfTitleET = (EditText) findViewById(R.id.ws_common_create_edit_name_et);
		mConfStartTimeET = (EditText) findViewById(R.id.conference_create_conf_start_time);
		mConfStartTimeET.setOnTouchListener(mDateTimePickerListener);
		mConfStartTimeET.setText(DateUtil.getStandardDate(new Date(GlobalConfig
				.getGlobalServerTime())));
		new LoadContactsAT().execute();
	}

	@Override
	protected void setListener() {

	}

	@Override
	protected void leftButtonClickListener(View v) {
		onBackPressed();
	}

	@Override
	protected void rightButtonClickListener(View v) {
		if (!GlobalHolder.getInstance().isServerConnected()) {
			Toast.makeText(mContext, R.string.error_local_connect_to_server,
					Toast.LENGTH_SHORT).show();
			return;
		}
		
		if (mState == State.CREATEING) {
			return;
		}
		mState = State.CREATEING;

		mWaitingDialog = ProgressDialog.show(mContext, "", mContext
				.getResources()
				.getString(R.string.notification_watiing_process), true);
		final String title = mConfTitleET.getText().toString().trim();
		final String startTimeStr = mConfStartTimeET.getText().toString();
		boolean strongly = ConfStrongWatch(title, startTimeStr);
		if (strongly == false) {
			if (mWaitingDialog != null
					&& mWaitingDialog.isShowing()) {
				mWaitingDialog.dismiss();
			}
			mState = State.DONE;
			return;
		}

		new Thread(new Runnable() {

			@Override
			public void run() {
				List<User> userList = new ArrayList<User>(mAttendeeList);
				conf = new Conference(title, startTimeStr, null, userList);
				cs.createConference(conf, new MessageListener(mLocalHandler,
						CREATE_CONFERENC_RESP, conf));
			}
		}).start();
	}

	private boolean ConfStrongWatch(final String title,
			final String startTimeStr) {
		if (title == null || title.length() == 0) {
			mConfTitleET
					.setError(getString(R.string.error_conf_title_required));
			mConfTitleET.requestFocus();
			return false;
		}

		if (startTimeStr == null || startTimeStr.length() == 0) {
			mConfStartTimeET
					.setError(getString(R.string.error_conf_start_time_required));
			mConfStartTimeET.requestFocus();
			return false;
		}

		DateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINESE);
		Date st = null;
		try {
			st = sd.parse(startTimeStr);
		} catch (ParseException e) {
			e.printStackTrace();
			mConfStartTimeET
					.setError(getString(R.string.error_conf_start_time_format_failed));
			mConfStartTimeET.requestFocus();
			return false;
		}

		if ((new Date(GlobalConfig.getGlobalServerTime()).getTime() / 60000) - (st.getTime() / 60000) > 10) {
			mConfStartTimeET
					.setError(getString(R.string.error_conf_do_not_permit_create_piror_conf));
			mConfStartTimeET.requestFocus();
			return false;
		}
		return true;
	}

	@Override
	protected void mAttendeeContainerItemClick(AdapterView<?> parent,
			View view, int position, long id) {
		User user = mAttendeeArrayList.get(position);
		mGroupListView.updateCheckItem(user, false);
		for (Group group : user.getBelongsGroup()) {
			List<User> users = group.getUsers();
			mGroupListView.checkBelongGroupAllChecked(group, users);
		}
		updateUserToAttendList(user, OP_DEL_ALL_GROUP_USER);
	}

	@Override
	protected void mGroupListViewItemClick(AdapterView<?> parent, View view,
			int position, long id, ItemData item) {
	}

	@Override
	protected void mGroupListViewlongItemClick(AdapterView<?> parent,
			View view, int position, long id, ItemData item) {

	}

	@Override
	protected void mGroupListViewCheckBoxChecked(View view, ItemData item) {
		final CheckBox cb = (CheckBox) view;
		int flag = -1;
		Object obj = item.getObject();
		if (obj instanceof User) {
			if (!cb.isChecked()) {
				flag = OP_DEL_ALL_GROUP_USER;
			} else {
				flag = OP_ADD_ALL_GROUP_USER;
			}
			User user = (User) obj;
			updateUserToAttendList(user, flag);

			mGroupListView.updateCheckItem(user, cb.isChecked());
			Set<Group> belongsGroup = user.getBelongsGroup();
			for (Group group : belongsGroup) {
				List<User> users = group.getUsers();
				mGroupListView.checkBelongGroupAllChecked(group, users);
			}
		} else if (obj instanceof Group) {
			Group selectedGroup = (Group) obj;
			startSelectGroup(mLocalHandler,cb, selectedGroup);
		}
	}

	private void initReceiver() {
		receiver = new LocalBroadcastReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		intentFilter.addCategory(PublicIntent.DEFAULT_CATEGORY);
		intentFilter
				.addAction(JNIService.JNI_BROADCAST_USER_STATUS_NOTIFICATION);
		intentFilter
				.addAction(JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION);
		this.registerReceiver(receiver, intentFilter);
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
									+ (monthOfYear < 10 ? ("0" + monthOfYear)
											: monthOfYear)
									+ "-"
									+ dayOfMonth
									+ " "
									+ (hour < 10 ? ("0" + hour) : (hour + ""))
									+ ":"
									+ (minute < 10 ? ("0" + minute)
											: (minute + "")) + ":00");
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
			preSelectedUID = getIntent().getLongExtra("uid", 0);
			preSelectedUID = getIntent().getLongExtra("uid", 0);
			preSelectedGroupId = getIntent().getLongExtra("gid", 0);
			if (preSelectedUID > 0) {
				User preUser = GlobalHolder.getInstance().getUser(
						preSelectedUID);
				if (preUser != null) {
					updateUserToAttendList(preUser, OP_ADD_ALL_GROUP_USER);
					mGroupListView.selectUser(preUser);
				}
			}

			if (preSelectedGroupId > 0) {
				Group preGroup = GlobalHolder.getInstance().getGroupById(
						preSelectedGroupId);
				if (preGroup != null) {
					mGroupListView.selectUser(preGroup.getUsers());
					selectGroup(preGroup, true);
				}
			}		
		}
	};
	
	class LocalHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case CREATE_CONFERENC_RESP:
				JNIResponse rccr = (JNIResponse) msg.obj;
				Conference conference = (Conference) rccr.callerObject;
				if (rccr.getResult() != JNIResponse.Result.SUCCESS) {
					V2Log.e("ConferenceCreateActivity --> CREATE FAILED ... ERROR CODE IS : "
							+ rccr.getResult().name());
					mErrorNotification.setVisibility(View.VISIBLE);
					if (rccr.getResult() == JNIResponse.Result.ERR_CONF_LOCKDOG_NORESOURCE)
						mErrorNotification.setText(R.string.error_no_resource);
					else if (rccr.getResult() == JNIResponse.Result.TIME_OUT) {
						mErrorNotification
								.setText(R.string.error_time_out_create_conference_failed);
					} else {
						mErrorNotification
								.setText(R.string.error_create_conference_failed_from_server_side);
					}
					mState = State.DONE;
					if (mWaitingDialog != null
							&& mWaitingDialog.isShowing()) {
						mWaitingDialog.dismiss();
					}
					break;
				}
				// 防止界面在未销户之前，用户再次点击创建按钮
				rightButtonTV.setClickable(false);
				if (mWaitingDialog != null
						&& mWaitingDialog.isShowing()) {
					mWaitingDialog.dismiss();
				}

				Date startDate;
				if (conference != null)
					startDate = conference.getDate();
				else
					startDate = new Date(GlobalConfig.getGlobalServerTime());

				RequestConfCreateResponse rc = (RequestConfCreateResponse) rccr;
				ConferenceGroup g = new ConferenceGroup(rc.getConfId(),
						conf.getName(), GlobalHolder.getInstance()
								.getCurrentUser(), startDate, GlobalHolder
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
			}
		}
	}
}
