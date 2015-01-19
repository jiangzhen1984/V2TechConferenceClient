package com.v2tech.view.conference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.service.GlobalHolder;
import com.v2tech.view.JNIService;
import com.v2tech.view.PublicIntent;
import com.v2tech.view.adapter.CommonCreateAdapter;
import com.v2tech.view.cus.SearchEditText;
import com.v2tech.view.widget.GroupListView;
import com.v2tech.view.widget.GroupListView.ItemData;
import com.v2tech.vo.Group;
import com.v2tech.vo.NetworkStateCode;
import com.v2tech.vo.User;

/**
 * 需要调用 initTitle 函数，初始化标题栏
 * 
 * @author Administrator
 * 
 */
public abstract class BaseCreateActivity extends Activity {

	protected static final int CREATE_LAYOUT_TYPE_CONFERENCE = 0x001;
	protected static final int CREATE_LAYOUT_TYPE_CROWD = 0x002;
	protected static final int CREATE_LAYOUT_TYPE_DISCUSSION = 0x004;

	protected static final int PAD_LAYOUT = 1;
	protected static final int PHONE_LAYOUT = 0;

	protected Context mContext;

	protected int createType;

	protected int landLayout = PAD_LAYOUT;

	protected TextView titleContentTV;
	protected TextView leftButtonTV;
	protected TextView rightButtonTV;
	protected SearchEditText searchedTextET;
	protected TextView mErrorNotification;
	protected View customLayout;
	
	protected AdapterView<ListAdapter> mAttendeeContainer;
	protected GroupListView mGroupListView;
	protected CommonCreateAdapter mAdapter;

	protected LocalBaseReceiver localBroadcast;

	// Used to save current selected user
	protected Set<User> mAttendeeList = new HashSet<User>();
	protected List<User> mAttendeeArrayList = new ArrayList<User>();

	protected void initCreateType(int createType) {
		this.createType = createType;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.common_create_group_layout);
		mContext = this;
		initBase();
		init();
		setListener();
	}

	private void initBase() {
		mGroupListView = (GroupListView) findViewById(R.id.ws_common_create_group_list_view);
		mGroupListView.initCreateMode();
		mGroupListView.setListener(listViewListener);

		mAttendeeContainer = (AdapterView<ListAdapter>) findViewById(R.id.ws_common_create_select_layout);
		mAttendeeContainer.setOnItemClickListener(mItemClickedListener);
		landLayout = mAttendeeContainer.getTag().equals("vertical") ? PAD_LAYOUT
				: PHONE_LAYOUT;
		mAdapter = new CommonCreateAdapter(this, mAttendeeArrayList, landLayout);
		mAttendeeContainer.setAdapter(mAdapter);

		mErrorNotification = (TextView) findViewById(R.id.ws_common_error_connect);
		boolean connect = GlobalHolder.getInstance().isServerConnected();
		if (connect) {
			mErrorNotification.setVisibility(View.GONE);
		} else {
			mErrorNotification.setVisibility(View.VISIBLE);
			mErrorNotification
					.setText(R.string.error_create_conference_failed_no_network);
		}
		
		searchedTextET = (SearchEditText) findViewById(R.id.ws_common_create_search);
		searchedTextET.addTextListener(mGroupListView);

		customLayout = findViewById(R.id.ws_common_create_custom_content_ly);
		TextView editNameHint = (TextView) findViewById(R.id.ws_common_create_edit_name_hint);
		TextView editContentHint = (TextView) findViewById(R.id.ws_common_create_edit_content_hint);
		View confStartTime = findViewById(R.id.conference_create_conf_start_time);
		View crowdSpiner = findViewById(R.id.group_create_group_rule);
		switch (createType) {
		case CREATE_LAYOUT_TYPE_CONFERENCE:
			initTitle(R.string.conference_create_title,
					R.string.conference_create_cancel,
					R.string.conference_create_confirm);
			editNameHint.setText(R.string.conference_create_conf_name);
			editContentHint.setText(R.string.conference_create_conf_start_time);
			customLayout.setVisibility(View.VISIBLE);
			confStartTime.setVisibility(View.VISIBLE);
			crowdSpiner.setVisibility(View.GONE);
			break;
		case CREATE_LAYOUT_TYPE_CROWD:
			initTitle(R.string.crowd_create_activity_title,
					R.string.common_return_name,
					R.string.common_confirm_name);
			editNameHint.setText(R.string.group_create_group_name);
			editContentHint.setText(R.string.group_create_group_qualification);
			customLayout.setVisibility(View.VISIBLE);
			confStartTime.setVisibility(View.GONE);
			crowdSpiner.setVisibility(View.VISIBLE);
			break;
		case CREATE_LAYOUT_TYPE_DISCUSSION:
			initTitle(R.string.discussion_create_activity_title,
					R.string.common_return_name,
					R.string.common_confirm_name);
			customLayout.setVisibility(View.GONE);
			break;
		default:
			break;
		}
		initBroadCast();
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(localBroadcast);
		searchedTextET.removeTextListener();
		super.onDestroy();
	}

	private void initBroadCast() {
		localBroadcast = new LocalBaseReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		intentFilter.addCategory(PublicIntent.DEFAULT_CATEGORY);
		intentFilter
				.addAction(JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION);
		this.registerReceiver(localBroadcast, intentFilter);
	}
	
	protected void addAttendee(User u) {
		if(!mAttendeeList.contains(u))
			mAttendeeList.add(u);
		if(!mAttendeeArrayList.contains(u))
			mAttendeeArrayList.add(u);
		mAdapter.notifyDataSetChanged();
	}

	protected void removeAttendee(User u) {
		mAttendeeList.remove(u);
		mAttendeeArrayList.remove(u);
		mAdapter.notifyDataSetChanged();
	}
	
	protected void selectGroup(Group selectGroup, boolean addOrRemove) {
		List<Group> subGroups = selectGroup.getChildGroup();
		for (int i = 0; i < subGroups.size(); i++) {
			selectGroup(subGroups.get(i), addOrRemove);
		}
		List<User> list = selectGroup.getUsers();
		for (int i = 0; i < list.size(); i++) {
			User u = list.get(i);
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

	protected abstract void init();

	protected abstract void setListener();

	protected abstract void leftButtonClickListener(View v);

	protected abstract void rightButtonClickListener(View v);

	protected abstract void mAttendeeContainerItemClick(AdapterView<?> parent,
			View view, int position, long id);

	protected abstract void mGroupListViewItemClick(AdapterView<?> parent,
			View view, int position, long id, ItemData item);

	protected abstract void mGroupListViewlongItemClick(AdapterView<?> parent,
			View view, int position, long id, ItemData item);

	protected abstract void mGroupListViewCheckBoxChecked(View view,
			ItemData item);

	private void initTitle(int titleContent, int leftButton, int rightButton) {
		titleContentTV = (TextView) findViewById(R.id.ws_common_activity_title_content);
		titleContentTV.setText(titleContent);

		leftButtonTV = (TextView) findViewById(R.id.ws_common_activity_title_left_button);
		leftButtonTV.setText(leftButton);
		leftButtonTV.setOnClickListener(leftButtonClickListener);

		rightButtonTV = (TextView) findViewById(R.id.ws_common_activity_title_right_button);
		rightButtonTV.setText(rightButton);
		rightButtonTV.setOnClickListener(rightButtonClickListener);
	}

	private OnClickListener leftButtonClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			leftButtonClickListener(v);
		}

	};

	private OnClickListener rightButtonClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			mErrorNotification.setVisibility(View.GONE);
			rightButtonClickListener(v);
		}

	};

	private OnItemClickListener mItemClickedListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			mAttendeeContainerItemClick(parent, view, position, id);
		}
	};

	private GroupListView.GroupListViewListener listViewListener = new GroupListView.GroupListViewListener() {

		@Override
		public void onItemClicked(AdapterView<?> parent, View view,
				int position, long id, ItemData item) {
			mGroupListViewItemClick(parent, view, position, id, item);
		}

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id, ItemData item) {
			mGroupListViewlongItemClick(parent, view, position, id, item);
			return true;
		}

		public void onCheckboxClicked(View view, ItemData item) {
			mGroupListViewCheckBoxChecked(view, item);
		}

	};

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		View v = getCurrentFocus();
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
		}
		return super.dispatchTouchEvent(ev);
	}

	class LocalBaseReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION
					.equals(intent.getAction())) {
				NetworkStateCode code = (NetworkStateCode) intent.getExtras()
						.get("state");
				if (code != NetworkStateCode.CONNECTED) {
					mErrorNotification.setVisibility(View.VISIBLE);
					mErrorNotification
							.setText(R.string.error_create_conference_failed_no_network);
				} else {
					mErrorNotification.setVisibility(View.GONE);
				}
			}
		}
	}
}
