package com.v2tech.view.conference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.service.GlobalHolder;
import com.v2tech.view.widget.GroupListView;
import com.v2tech.view.widget.GroupListView.Item;
import com.v2tech.vo.Conference;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.User;

public class VideoInvitionAttendeeLayout extends LinearLayout {

	private static final int UPDATE_ATTENDEES = 2;

	private static final int PAD_LAYOUT = 1;
	private static final int PHONE_LAYOUT = 0;

	private Context mContext;
	private LocalHandler mLocalHandler = new LocalHandler();

	private EditText searchedTextET;
	//private ListView mContactsContainer;
	private GroupListView mGroupListView;
	private EditText mConfTitleET;
	private EditText mConfStartTimeET;
	private View mInvitionButton;

	private LinearLayout mErrorNotificationLayout;

	private LinearLayout mAttendeeContainer;

	private View mScroller;


	private List<Group> mGroupList;
	
	// Used to save current selected user
	private Set<User> mAttendeeList = new HashSet<User>();

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

		mAttendeeContainer = (LinearLayout) view
				.findViewById(R.id.conference_attendee_container);
		mAttendeeContainer.setGravity(Gravity.CENTER);
		landLayout = mAttendeeContainer.getTag().equals("vertical") ? PAD_LAYOUT
				: PHONE_LAYOUT;

		mConfTitleET = (EditText) view
				.findViewById(R.id.conference_create_conf_name);
		mConfTitleET.setEnabled(false);
		mConfStartTimeET = (EditText) view
				.findViewById(R.id.conference_create_conf_start_time);
		mConfStartTimeET.setEnabled(false);

		searchedTextET = (EditText) view.findViewById(R.id.contacts_search);
		searchedTextET.addTextChangedListener(textChangedListener);

		mErrorNotificationLayout = (LinearLayout) view
				.findViewById(R.id.conference_create_error_notification);
		mScroller = view.findViewById(R.id.conf_create_scroll_view);
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
		mAttendeeContainer.removeAllViews();
		for (User tmpU : mAttendeeList) {
			addAttendee(tmpU);
		}
	}



	private void addAttendee(User u) {
		if (u.isCurrentLoggedInUser()) {
			return;
		}
		mAttendeeList.add(u);

		View v = null;
		if (landLayout == PAD_LAYOUT) {
			v = new ContactUserView(mContext, u, false);
			((ContactUserView) v).removePadding();
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
		tv.setTextSize(8);
		tv.setMaxWidth(60);
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
			mGroupListView.updateCheckItem(u, false);
		}

	};

	private TextWatcher textChangedListener = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable s) {
			if (TextUtils.isEmpty(s.toString())) {
				if (!TextUtils.isEmpty(mGroupListView.getTextFilter())) {
					mGroupListView.clearTextFilter();
				}
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
				int position, long id, Item item) {
			return false;
		}

		@Override
		public void onItemClicked(AdapterView<?> parent, View view,
				int position, long id, Item item) {
			Object obj = item.getObject();
			if (obj instanceof User) {
				Message.obtain(mLocalHandler, UPDATE_ATTENDEES, (User)obj)
				.sendToTarget();
				mGroupListView.updateCheckItem((User)obj, !item.isChecked());
			}

		}

		public void onCheckboxClicked(View view, Item item) {
			Object obj = item.getObject();
			if (obj instanceof User) {
				Message.obtain(mLocalHandler, UPDATE_ATTENDEES, (User)obj)
				.sendToTarget();
				mGroupListView.updateCheckItem((User)obj, !item.isChecked());
			}
		}
	};
	



	private OnClickListener confirmButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			List<User> l = new ArrayList<User>(mAttendeeList);
			if (listener != null) {
				listener.requestInvitation(conf, l);
			}

			// Clean
			mAttendeeContainer.removeAllViews();
			mAttendeeList.clear();
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



	

	class LocalHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPDATE_ATTENDEES:
				updateUserToAttendList((User) msg.obj);
				break;
			}
		}

	}

}
