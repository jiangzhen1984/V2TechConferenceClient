package com.v2tech.view.contacts;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.v2tech.R;
import com.v2tech.service.ContactsService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.Registrant;
import com.v2tech.service.jni.GroupServiceJNIResponse;
import com.v2tech.view.PublicIntent;
import com.v2tech.vo.ContactGroup;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.User;

public class UpdateContactGroupActivity extends Activity {

	private static final int UPDATE_USER_GROUP_DONE = 1;
	public static final int SELECT_GROUP_RESPONSE_CODE_DONE = 0;
	public static final int SELECT_GROUP_RESPONSE_CODE_CANCEL = 1;

	private Context mContext;
	private RadioGroup mGroupListLy;
	private ContactsService contactService = new ContactsService();

	private STATE state = STATE.NONE;
	private boolean changed;
	private long originGroupId;
	private long userId;
	private Toast mToast;
	// 值为"addFriend"时是从加好友跳转而来，其他值为更改分组跳转而来。
	private String from;
	private View mReturnButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		originGroupId = getIntent().getLongExtra("gid", 0);
		userId = getIntent().getLongExtra("uid", 0);
		from = getIntent().getStringExtra("from");
		mContext = this;
		setContentView(R.layout.activity_contacts_update_group);
		mGroupListLy = (RadioGroup) findViewById(R.id.contact_update_group_list);
		mReturnButton = findViewById(R.id.contact_update_return_button);
		mReturnButton.setOnClickListener(mReturnButtonListener);
		// build radio button first
		buildList();
		mGroupListLy.setOnCheckedChangeListener(mGroupChangedListener);
		overridePendingTransition(R.animator.left_in, R.animator.left_out);
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

			rb.setId((int) g.getmGId());

			long id = rb.getId();

			Log.i("wzl", "id:" + id);
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

		if ((from != null) && from.equals("addFriend")) {
			int i1 = (int) getIntent().getLongExtra("groupID", -1);
			if (i1 != -1) {
				mGroupListLy.check(i1);
			}
		}
	}

	@Override
	public void finish() {
		if ((from != null) && from.equals("addFriend")) {
		} else {
			if (changed) {
				Intent i = new Intent(
						PublicIntent.BROADCAST_REQUEST_UPDATE_CONTACTS_GROUP);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				mContext.sendBroadcast(i);
			}
		}
		super.finish();
		overridePendingTransition(R.animator.right_in, R.animator.right_out);
	}
	
	

	@Override
	protected void onDestroy() {
		super.onDestroy();
		contactService.clearCalledBack();
	}

	@Override
	public void onBackPressed() {
		if ((from != null) && from.equals("addFriend")) {
			setResult(SELECT_GROUP_RESPONSE_CODE_CANCEL, null);
		}
		super.onBackPressed();
	}
	
	private OnClickListener mReturnButtonListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			onBackPressed();
		}
		
	};

	private RadioGroup.OnCheckedChangeListener mGroupChangedListener = new RadioGroup.OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(RadioGroup rg, int id) {
			if ((from != null) && from.equals("addFriend")) {

				RadioButton rb = (RadioButton) rg.findViewById(id);
				Group group = (Group) rb.getTag();
				Intent intent = new Intent();
				intent.putExtra("groupName", group.getName());
				intent.putExtra("groupID", group.getmGId());
				setResult(SELECT_GROUP_RESPONSE_CODE_DONE, intent);
				finish();
			} else {
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
				originGroupId = ((Group) rg.findViewById(id).getTag())
						.getmGId();
				Group desGroup = GlobalHolder.getInstance().getGroupById(
						Group.GroupType.CONTACT.intValue(), originGroupId);

				User user = GlobalHolder.getInstance()
						.getUser(userId);
				contactService.updateUserGroup((ContactGroup) desGroup,
						(ContactGroup) srcGroup, user, new Registrant(mLocalHandler,
								UPDATE_USER_GROUP_DONE, new LocalObject(user, srcGroup, desGroup)));

			}
		}

	};

	private Handler mLocalHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if ((from != null) && from.equals("addFriend")) {
			} else {
				switch (msg.what) {
				case UPDATE_USER_GROUP_DONE:
					synchronized (state) {
						state = STATE.NONE;
					}
					LocalObject lo = (LocalObject) ((GroupServiceJNIResponse)msg.obj).callerObject;
					//Send broadcast for indicate contact group update
					Intent i = new Intent(PublicIntent.BROADCAST_CONTACT_GROUP_UPDATED_NOTIFICATION);
					i.addCategory(PublicIntent.DEFAULT_CATEGORY);
					i.putExtra("userId", lo.user.getmUserId());
					i.putExtra("srcGroupId", lo.src.getmGId());
					i.putExtra("destGroupId", lo.dest.getmGId());
					
					mContext.sendBroadcast(i);
					
					//Set result to parent
					Intent intent = new Intent();
					Group g = GlobalHolder.getInstance().getGroupById(GroupType.CONTACT.intValue(), lo.dest.getmGId());
					intent.putExtra("groupName", g.getName());
					intent.putExtra("groupID", lo.dest.getmGId());
					setResult(SELECT_GROUP_RESPONSE_CODE_DONE, intent);
					
					finish();
					break;
				}
			}
		}

	};
	
	class LocalObject {
		User user;
		Group src;
		Group dest;
		
		public LocalObject(User user, Group src, Group dest) {
			super();
			this.user = user;
			this.src = src;
			this.dest = dest;
		}
		
	}

	enum STATE {
		NONE, UPDATING
	}

}
