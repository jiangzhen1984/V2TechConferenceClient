package com.v2tech.view.contacts;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.v2tech.R;
import com.v2tech.service.ContactsService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.Registrant;
import com.v2tech.view.PublicIntent;
import com.v2tech.vo.ContactGroup;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;

public class UpdateContactGroupActivity extends Activity {

	private static final int UPDATE_USER_GROUP_DONE = 1;

	private Context mContext;
	private LinearLayout mGroupListLy;
	private ContactsService contactService = new ContactsService();

	private STATE state = STATE.NONE;
	private boolean changed;
	private long originGroupId;
	private long userId;
	private RadioButton[] rbs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		originGroupId = getIntent().getLongExtra("gid", 0);
		userId = getIntent().getLongExtra("uid", 0);
		mContext = this;
		setContentView(R.layout.activity_contacts_update_group);
		mGroupListLy = (LinearLayout) findViewById(R.id.contact_update_group_list);
		buildList();
		overridePendingTransition(R.animator.left_in, R.animator.left_out);
	}

	@Override
	public void finish() {
		if (changed) {
			Intent i = new Intent(
					PublicIntent.BROADCAST_REQUEST_UPDATE_CONTACTS_GROUP);
			i.addCategory(PublicIntent.DEFAULT_CATEGORY);
			mContext.sendBroadcast(i);
		}
		super.finish();
		overridePendingTransition(R.animator.right_in, R.animator.right_out);
	}

	private void buildList() {
		List<Group> friendGroup = GlobalHolder.getInstance().getGroup(
				GroupType.CONTACT);
		rbs = new RadioButton[friendGroup.size()];
		for (int i = 0; i < friendGroup.size(); i++) {
			Group g = friendGroup.get(i);
			RelativeLayout rl = new RelativeLayout(mContext);
			mGroupListLy.addView(rl, new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT));

			LinearLayout line = new LinearLayout(mContext);
			line.setBackgroundColor(Color.rgb(206, 206, 206));
			mGroupListLy.addView(line, new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, 1));

			TextView gName = new TextView(mContext);
			gName.setTextColor(mContext.getResources().getColor(
					R.color.activiy_contact_detail_item_color));
			gName.setTextSize(14);
			gName.setText(g.getName());
			gName.setPadding(10, 10, 10, 10);

			RelativeLayout.LayoutParams lefll = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			lefll.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			lefll.addRule(RelativeLayout.CENTER_VERTICAL);
			lefll.leftMargin = (int) mContext.getResources().getDimension(
					R.dimen.contact_detail_2_item_margin_horizontal);
			rl.addView(gName, lefll);

			RadioButton rb = new RadioButton(mContext);
			rb.setText(null);
			rb.setTag(g);
			rb.setOnClickListener(updateGroupClickListener);
			rb.setSelected(g.getmGId() == originGroupId);
			RelativeLayout.LayoutParams rightll = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			rightll.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			rightll.addRule(RelativeLayout.CENTER_VERTICAL);
			rightll.rightMargin = (int) mContext.getResources().getDimension(
					R.dimen.contact_detail_2_item_margin_horizontal);
			rl.addView(rb, rightll);
			rbs[i] = rb;
		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	private OnClickListener updateGroupClickListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			synchronized (state) {
				if (state == STATE.UPDATING) {
					Toast.makeText(
							mContext,
							R.string.activiy_contact_update_group_error_msg_in_progess,
							Toast.LENGTH_SHORT).show();
					return;
				}
				state = STATE.UPDATING;
				Group srcGroup = GlobalHolder.getInstance().getGroupById(
						Group.GroupType.CONTACT, originGroupId);
				Group desGroup = null;
				for (int i = 0; i < rbs.length; i++) {
					if (view == rbs[i] && !rbs[i].isSelected()) {
						rbs[i].setSelected(true);
						rbs[i].setChecked(true);
						originGroupId = ((Group) rbs[i].getTag()).getmGId();
						desGroup = GlobalHolder.getInstance().getGroupById(
								Group.GroupType.CONTACT, originGroupId);
					} else {
						rbs[i].setSelected(false);
						rbs[i].setChecked(false);
					}
				}

				contactService.updateUserGroup((ContactGroup) desGroup,
						(ContactGroup) srcGroup, GlobalHolder.getInstance()
								.getUser(userId), new Registrant(mLocalHandler,
								UPDATE_USER_GROUP_DONE, null));
			}
		}

	};

	private Handler mLocalHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPDATE_USER_GROUP_DONE:
				state = STATE.NONE;
				break;
			}
		}

	};

	enum STATE {
		NONE, UPDATING
	}

}
