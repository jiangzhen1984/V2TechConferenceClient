package com.v2tech.view.contacts;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
	private RadioGroup mGroupListLy;
	private ContactsService contactService = new ContactsService();

	private STATE state = STATE.NONE;
	private boolean changed;
	private long originGroupId;
	private long userId;
	private Toast mToast;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		originGroupId = getIntent().getLongExtra("gid", 0);
		userId = getIntent().getLongExtra("uid", 0);
		mContext = this;
		setContentView(R.layout.activity_contacts_update_group);
		mGroupListLy = (RadioGroup) findViewById(R.id.contact_update_group_list);
		// build radio button first
		buildList();
		mGroupListLy.setOnCheckedChangeListener(mGroupChangedListener);
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
				GroupType.CONTACT.intValue());
		for (int i = 0; i < friendGroup.size(); i++) {
			Group g = friendGroup.get(i);

			RadioButton rb = (RadioButton) LayoutInflater.from(mContext)
					.inflate(R.layout.common_radio_right, null);
			rb.setText(g.getName());
			rb.setTag(g);
			rb.setTextColor(mContext.getResources().getColor(
					R.color.activiy_contact_detail_item_color));
			int margin = (int) mContext.getResources().getDimension(
					R.dimen.contact_detail_2_item_margin_horizontal);
			rb.setPadding(margin, 0, margin, 0);
			LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			mGroupListLy.addView(rb, ll);

			LinearLayout line = new LinearLayout(mContext);
			line.setBackgroundColor(Color.rgb(206, 206, 206));
			mGroupListLy.addView(line, new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, 1));
			if (g.getmGId() == originGroupId) {
				rb.toggle();
			}

		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	private RadioGroup.OnCheckedChangeListener mGroupChangedListener = new RadioGroup.OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(RadioGroup rg, int id) {
			synchronized (state) {
				if (state == STATE.UPDATING) {
					if (mToast == null) {
						mToast = Toast
								.makeText(
										mContext,
										R.string.activiy_contact_update_group_error_msg_in_progess,
										Toast.LENGTH_SHORT);
					}
					mToast.cancel();
					mToast.show();
					return;
				}
				state = STATE.UPDATING;
			}
			Group srcGroup = GlobalHolder.getInstance().getGroupById(
					Group.GroupType.CONTACT.intValue(), originGroupId);
			// update group id to new group
			originGroupId = ((Group) rg.findViewById(id).getTag()).getmGId();
			Group desGroup = GlobalHolder.getInstance().getGroupById(
					Group.GroupType.CONTACT.intValue(), originGroupId);

			contactService.updateUserGroup((ContactGroup) desGroup,
					(ContactGroup) srcGroup, GlobalHolder.getInstance()
							.getUser(userId), new Registrant(mLocalHandler,
							UPDATE_USER_GROUP_DONE, null));

		}

	};

	private Handler mLocalHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPDATE_USER_GROUP_DONE:
				synchronized (state) {
					state = STATE.NONE;
				}
				finish();
				break;
			}
		}

	};

	enum STATE {
		NONE, UPDATING
	}

}
