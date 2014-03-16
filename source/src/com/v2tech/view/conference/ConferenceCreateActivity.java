package com.v2tech.view.conference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils.TruncateAt;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.logic.Conference;
import com.v2tech.logic.GlobalHolder;
import com.v2tech.logic.Group;
import com.v2tech.logic.Group.GroupType;
import com.v2tech.logic.User;
import com.v2tech.service.ConferenceService;

public class ConferenceCreateActivity extends Activity {

	private static final int UPDATE_LIST_VIEW = 1;
	private static final int UPDATE_ATTENDEES = 2;

	private Context mContext;
	private LocalHandler mLocalHandler = new LocalHandler();

	private EditText searchedTextET;
	private ListView mContactsContainer;
	private ContactsAdapter adapter = new ContactsAdapter();
	private TextView mConfirmButton;
	private EditText mConfTitleET;
	private EditText mConfStartTimeET;
	private EditText mConfEndTimeET;

	private TableLayout mAttendeeContainer;

	private List<ListItem> mItemList = new ArrayList<ListItem>();
	private List<ListItem> mCacheItemList;
	private List<Group> mGroupList;

	private Set<User> mAttendeeList = new HashSet<User>();

	private ConferenceService cs = new ConferenceService();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_conference_create);
		mContext = this;
		mContactsContainer = (ListView) findViewById(R.id.conf_create_contacts_list);
		mContactsContainer.setOnItemClickListener(itemListener);
		mContactsContainer.setAdapter(adapter);

		mAttendeeContainer = (TableLayout) findViewById(R.id.conference_attendee_container);
		mAttendeeContainer.setGravity(Gravity.CENTER);

		mConfirmButton = (TextView) findViewById(R.id.conference_create_confirm_button);
		mConfirmButton.setOnClickListener(confirmButtonListener);

		mConfTitleET = (EditText) findViewById(R.id.conference_create_conf_name);
		mConfStartTimeET = (EditText) findViewById(R.id.conference_create_conf_start_time);
		mConfEndTimeET = (EditText) findViewById(R.id.conference_create_conf_end_time);

		new LoadContactsAT().execute();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	private void updateView(int pos) {
		ListItem item = mItemList.get(pos);
		if (item.g == null) {
			return;
		}
		if (item.isExpanded == false) {
			for (Group g : item.g.getChildGroup()) {
				mItemList.add(++pos, new ListItem(g));
			}
			List<User> sortList = new ArrayList<User>();
			sortList.addAll(item.g.getUsers());
			Collections.sort(sortList);
			for (User u : sortList) {
				ListItem li = new ListItem(u);
				mItemList.add(++pos, li);
				
				boolean found = false;
				for (User invitedUser : mAttendeeList) {
					if (invitedUser.getmUserId() == u.getmUserId()) {
						found = true;
						break;
					}
				}
				if (found) {
					((ContactUserView)li.v).updateChecked();
				}
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

	private void updateUserToAttendList(User u) {
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
			int cot = mAttendeeContainer.getChildCount();
			for (int i = 0; i < cot; i++) {
				TableRow tr = (TableRow) mAttendeeContainer.getChildAt(i);
				int jcot = tr.getChildCount();
				for (int j = 0; j < jcot; j++) {
					LinearLayout ll = (LinearLayout) tr.getChildAt(j);
					if (u.getmUserId() == Long
							.parseLong(ll.getTag().toString())) {
						tr.removeView(ll);
						return;
					}
				}
			}
			return;
		}

		mAttendeeList.add(u);


		int cot = mAttendeeContainer.getChildCount();
		TableRow tr = null;
		TableLayout.LayoutParams tbl = new TableLayout.LayoutParams(
				TableLayout.LayoutParams.MATCH_PARENT,
				TableLayout.LayoutParams.WRAP_CONTENT);
		tbl.topMargin = 10;
		if (cot <= 0) {
			tr = new TableRow(mContext);
			mAttendeeContainer.addView(tr, tbl);
		} else {
			tr = (TableRow) mAttendeeContainer.getChildAt(cot - 1);
			if (tr.getChildCount() == 4) {
				tr = new TableRow(mContext);
				mAttendeeContainer.addView(tr, tbl);
			}
		}
		LinearLayout ll = new LinearLayout(mContext);
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setGravity(Gravity.CENTER);
		TableRow.LayoutParams tl = new TableRow.LayoutParams();
		tl.column = tr.getChildCount() ;
		tr.addView(ll, tl);
		

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
		ll.setTag(u.getmUserId() + "");
		ll.addView(tv, new LinearLayout.LayoutParams(
				60,
				LinearLayout.LayoutParams.WRAP_CONTENT));
	}

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
			if (startTimeStr == null || startTimeStr.length() == 0) {
				mConfStartTimeET
						.setError(getString(R.string.error_conf_start_time_required));
				mConfStartTimeET.requestFocus();
				return;
			}
			String endTimeStr = mConfEndTimeET.getText().toString();
			if (endTimeStr == null || endTimeStr.length() == 0) {
				mConfEndTimeET
						.setError(getString(R.string.error_conf_end_time_required));
				mConfEndTimeET.requestFocus();
				return;
			}

			List<User> l = new ArrayList<User>(mAttendeeList);
			Conference conf = new Conference(title, startTimeStr, endTimeStr, l);
			cs.createConference(conf, null);

		}

	};

	class LoadContactsAT extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			mGroupList = GlobalHolder.getInstance().getGroup(GroupType.CONTACT);
			if (mGroupList != null) {
				for (Group g : mGroupList) {
					mItemList.add(new ListItem(g));
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

		public ListItem(Group g) {
			super();
			this.g = g;
			this.id = 0x02000000 | g.getmGId();
			this.v = new ContactGroupView(mContext, g, null);
			isExpanded = false;
		}

		public ListItem(User u) {
			super();
			this.u = u;
			this.id = 0x03000000 | u.getmUserId();
			this.v = new ContactUserView(mContext, u);
			isExpanded = false;
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
			}
		}

	}

}
