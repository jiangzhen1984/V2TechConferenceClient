package com.v2tech.view.conference;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
import android.text.InputType;
import android.text.TextUtils.TruncateAt;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.logic.AsynResult;
import com.v2tech.logic.Conference;
import com.v2tech.logic.GlobalHolder;
import com.v2tech.logic.Group;
import com.v2tech.logic.Group.GroupType;
import com.v2tech.logic.User;
import com.v2tech.logic.jni.RequestConfCreateResponse;
import com.v2tech.service.ConferenceService;
import com.v2tech.view.cus.DateTimePicker;

public class ConferenceCreateActivity extends Activity {

	private static final int UPDATE_LIST_VIEW = 1;
	private static final int UPDATE_ATTENDEES = 2;
	private static final int UPDATE_SEARCHED_USER_LIST = 3;
	private static final int CREATE_CONFERENC_RESP = 4;

	private static final int PAD_LAYOUT = 1;
	private static final int PHONE_LAYOUT = 0;

	private Context mContext;
	private LocalHandler mLocalHandler = new LocalHandler();

	private EditText searchedTextET;
	private ListView mContactsContainer;
	private ContactsAdapter adapter = new ContactsAdapter();
	private TextView mConfirmButton;
	private EditText mConfTitleET;
	private EditText mConfStartTimeET;

	private LinearLayout mErrorNotificationLayout;

	private LinearLayout mAttendeeContainer;

	private View mScroller;

	private boolean mIsStartedSearch;

	private List<ListItem> mItemList = new ArrayList<ListItem>();
	private List<ListItem> mCacheItemList;
	private List<Group> mGroupList;

	private Set<User> mAttendeeList = new HashSet<User>();

	private ConferenceService cs = new ConferenceService();

	private Conference conf;

	private int landLayout = PAD_LAYOUT;

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
		mContactsContainer.setAdapter(adapter);

		mAttendeeContainer = (LinearLayout) findViewById(R.id.conference_attendee_container);
		mAttendeeContainer.setGravity(Gravity.CENTER);
		landLayout = mAttendeeContainer.getTag().equals("vertical") ? PAD_LAYOUT
				: PHONE_LAYOUT;

		mConfirmButton = (TextView) findViewById(R.id.conference_create_confirm_button);
		mConfirmButton.setOnClickListener(confirmButtonListener);

		mConfTitleET = (EditText) findViewById(R.id.conference_create_conf_name);
		mConfStartTimeET = (EditText) findViewById(R.id.conference_create_conf_start_time);
		//mConfStartTimeET.setOnFocusChangeListener(mDateTimePickerListener);
		//mConfStartTimeET.setInputType(InputType.TYPE_NULL);
		
		new LoadContactsAT().execute();

		searchedTextET = (EditText) findViewById(R.id.contacts_search);

		mErrorNotificationLayout = (LinearLayout) findViewById(R.id.conference_create_error_notification);
		mScroller = findViewById(R.id.conf_create_scroll_view);
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
		return screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE
				|| screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE;
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
				mItemList.add(++pos, new ListItem(g, g.getLevel()));
			}
			List<User> sortList = new ArrayList<User>();
			sortList.addAll(item.g.getUsers());
			Collections.sort(sortList);
			for (User u : sortList) {
				mItemList.add(++pos, new ListItem(u, item.g.getLevel()));
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
				// FIXME if find user how to check?
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
		mAttendeeList.add(u);

		int cot = mAttendeeContainer.getChildCount();
		LinearLayout lineLayout = null;
		if (cot <= 0) {
			lineLayout = new LinearLayout(mContext);
			mAttendeeContainer.addView(lineLayout,
					new LinearLayout.LayoutParams(
							LinearLayout.LayoutParams.MATCH_PARENT,
							LinearLayout.LayoutParams.WRAP_CONTENT));
		} else {
			lineLayout = (LinearLayout) mAttendeeContainer.getChildAt(cot - 1);
			if (landLayout == PAD_LAYOUT && lineLayout.getChildCount() == 4) {
				lineLayout = new LinearLayout(mContext);
				mAttendeeContainer.addView(lineLayout,
						new LinearLayout.LayoutParams(
								LinearLayout.LayoutParams.MATCH_PARENT,
								LinearLayout.LayoutParams.WRAP_CONTENT));
			}
		}

		final View v = getAttendeeView(u);
		if (landLayout == PAD_LAYOUT) {
			LinearLayout.LayoutParams par = new LinearLayout.LayoutParams(0,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			par.weight = 0.25F;
			lineLayout.addView(v, par);
		} else {
			lineLayout.addView(v);
		}

		if (mAttendeeContainer.getChildCount() > 0) {
			if (this.landLayout == PAD_LAYOUT) {
				((ScrollView) mScroller).postDelayed(new Runnable() {
					@Override
					public void run() {
						((ScrollView) mScroller).scrollTo(0, v.getBottom());
					}

				}, 100L);
			} else {
				((HorizontalScrollView) mScroller).postDelayed(new Runnable() {
					@Override
					public void run() {
						((HorizontalScrollView) mScroller).scrollTo(
								v.getLeft(), 0);
					}

				}, 100L);
			}
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
		ll.addView(iv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

		TextView tv = new TextView(mContext);
		tv.setText(u.getName());
		tv.setEllipsize(TruncateAt.END);
		tv.setSingleLine(true);
		ll.setTag(u.getmUserId() + "");
		ll.addView(tv, new LinearLayout.LayoutParams(60,
				LinearLayout.LayoutParams.WRAP_CONTENT));
		ll.setPadding(5, 5, 5, 5);
		ll.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				Message.obtain(mLocalHandler, UPDATE_ATTENDEES, u)
						.sendToTarget();
				for (int index = 0; index < mItemList.size(); index++) {
					ListItem li = mItemList.get(index);
					if (li.u != null && u.getmUserId() == li.u.getmUserId()) {
						((ContactUserView) li.v).updateChecked();
					}
				}
			}

		});

		return ll;
	}

	private void updateSearchedUserList(List<User> lu) {
		mItemList = new ArrayList<ListItem>();
		for (User u : lu) {
			ListItem item = new ListItem(u, -1);
			((ContactUserView) item.v).removePadding();
			mItemList.add(item);
		}
		adapter.notifyDataSetChanged();
	}

	private TextWatcher textChangedListener = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable s) {
			if (s.length() > 0) {
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
			List<User> searchedUserList = new ArrayList<User>();
			for (Group g : mGroupList) {
				Group.searchUser(s.toString(), searchedUserList, g);
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
				cuv.updateChecked();
				Message.obtain(mLocalHandler, UPDATE_ATTENDEES, item.u)
						.sendToTarget();
			}
		}

	};

	private OnClickListener confirmButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			String title = mConfTitleET.getText().toString();
			if (title == null || title.length() == 0) {
				mConfTitleET
						.setError(getString(R.string.error_conf_title_required));
				mConfTitleET.requestFocus();
				return;
			}
			String startTimeStr = mConfStartTimeET.getText().toString();
//			if (startTimeStr == null || startTimeStr.length() == 0) {
//				mConfStartTimeET
//						.setError(getString(R.string.error_conf_start_time_required));
//				mConfStartTimeET.requestFocus();
//				return;
//			}
//
//			DateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm",
//					Locale.CHINESE);
//			
//			try {
//				sd.parse(startTimeStr);
//			} catch (ParseException e) {
//				e.printStackTrace();
//				mConfStartTimeET
//						.setError(getString(R.string.error_conf_start_time_format_failed));
//				mConfStartTimeET.requestFocus();
//				return;
//			}

			List<User> l = new ArrayList<User>(mAttendeeList);
			conf = new Conference(title, startTimeStr, null, l);
			cs.createConference(conf,
					Message.obtain(mLocalHandler, CREATE_CONFERENC_RESP));

		}

	};
	
	
	private OnFocusChangeListener mDateTimePickerListener = new OnFocusChangeListener() {

		@Override
		public void onFocusChange(View view, boolean focus) {
			if (focus) {
				DateTimePicker dtp = new DateTimePicker(mContext, 300, 150);
				dtp.showAsDropDown(view);
			}
			
		}
		
	};
	
	class LoadContactsAT extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			mGroupList = GlobalHolder.getInstance().getGroup(GroupType.CONTACT);
			if (mGroupList != null) {
				for (Group g : mGroupList) {
					mItemList.add(new ListItem(g, g.getLevel()));
				}
				adapter.notifyDataSetChanged();
			}
			return null;
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
			case CREATE_CONFERENC_RESP:
				AsynResult ar = (AsynResult) msg.obj;
				if (ar.getState() != AsynResult.AsynState.SUCCESS) {
					mErrorNotificationLayout.setVisibility(View.VISIBLE);
					break;
				}
				RequestConfCreateResponse rccr = (RequestConfCreateResponse) ar
						.getObject();
				User currU = GlobalHolder.getInstance().getCurrentUser();
				Group g = new Group(rccr.getConfId(), GroupType.CONFERENCE,
						conf.getName(), currU.getmUserId() + "", conf.getDate().getTime()/1000+"");
				g.setOwnerUser(currU);
				GlobalHolder.getInstance().addGroupToList(GroupType.CONFERENCE,
						g);
				Intent i = new Intent();
				i.putExtra("newGid", g.getmGId());
				setResult(Activity.RESULT_OK, i);
				finish();
				break;
			}
		}

	}

}
