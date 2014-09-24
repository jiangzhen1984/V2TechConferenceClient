package com.v2tech.view.contacts.add;

import java.util.List;

import com.v2tech.R;
import com.v2tech.service.ContactsService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.view.MainActivity;
import com.v2tech.view.contacts.ContactDetail2;
import com.v2tech.view.contacts.UpdateContactGroupActivity;
import com.v2tech.vo.FriendGroup;
import com.v2tech.vo.Group;
import com.v2tech.vo.User;
import com.v2tech.vo.Group.GroupType;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FriendManagementActivity extends Activity {
	private static final int SELECT_GROUP_REQUEST_CODE = 0;

	// for control
	// R.id.right_text_view
	private TextView tvRightTextView;
	// R.id.contact_detail_return_button
	private TextView tvBack;
	// R.id.select_group
	private RelativeLayout rlSelectGroup;
	// R.id.comment_name_et
	private EditText commentNameET;
	// R.id.tv_group_name
	private TextView tvGroupName;

	private long mUid;
	private User detailUser;
	private String verificationInfo;
	private String startedCause;
	private ContactsService contactService = new ContactsService();
	private String selectGroupName;
	private long selectGroupID;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		List<Group> listFriendGroup = GlobalHolder.getInstance().getGroup(
				GroupType.CONTACT.intValue());
		selectGroupName = listFriendGroup.get(0).getName();
		selectGroupID = listFriendGroup.get(0).getmGId();

		setContentView(R.layout.activity_contact_add_friend_management);
		startedCause = this.getIntent().getStringExtra("cause");
		connectView();
		bindViewEvent();
		if ((startedCause != null)
				&& startedCause.equals("access_friend_authentication")) {
			mUid = this.getIntent().getLongExtra("remoteUserID", 0);
		} else {
			mUid = this.getIntent().getLongExtra("uid", 0);
			verificationInfo = this.getIntent()
					.getCharSequenceExtra("verificationInfo").toString();
		}
		detailUser = GlobalHolder.getInstance().getUser(mUid);
		commentNameET.setText(detailUser.getName());
		tvGroupName.setText(selectGroupName);

	}

	private void connectView() {
		tvRightTextView = (TextView) findViewById(R.id.right_text_view);
		tvBack = (TextView) findViewById(R.id.contact_detail_return_button);
		rlSelectGroup = (RelativeLayout) findViewById(R.id.select_group);
		commentNameET = (EditText) findViewById(R.id.comment_name_et);
		if ((startedCause != null)
				&& startedCause.equals("access_friend_authentication")) {
			tvRightTextView.setText("完成");
		} else {
			// 发送
			tvRightTextView.setText("发送");
		}

		tvGroupName = (TextView) findViewById(R.id.tv_group_name);
	}

	private void bindViewEvent() {
		// 返回
		tvBack.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				onBackPressed();
			}
		});

		// 选择分组
		rlSelectGroup.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent i = new Intent();
				i.setClass(FriendManagementActivity.this,
						UpdateContactGroupActivity.class);
				i.putExtra("from", "addFriend");
				i.putExtra("groupID", selectGroupID);
				startActivityForResult(i, 0);
			}
		});

		if ((startedCause != null)
				&& startedCause.equals("access_friend_authentication")) {
			
			tvRightTextView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					// 同意加为好友,默认组为30
					contactService.acceptAddedAsContact(selectGroupID, mUid);
					// 实现越级跳
					Intent i = new Intent(FriendManagementActivity.this,
							MainActivity.class);
					i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(i);
				}
			});
		} else {
			// 发送
			tvRightTextView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {

					if (detailUser.getAuthtype() == 0) {
						AddFriendHistroysHandler.addOtherNoNeedAuthentication(
								getApplicationContext(), detailUser);
						contactService.addContact(new FriendGroup(selectGroupID, ""),
								detailUser, verificationInfo, commentNameET
										.getText().toString());
					} else if (detailUser.getAuthtype() == 1) {
						AddFriendHistroysHandler.addOtherNeedAuthentication(
								getApplicationContext(), detailUser,
								commentNameET.getText().toString());
						contactService.addContact(new FriendGroup(selectGroupID, ""),
								detailUser, verificationInfo, commentNameET
										.getText().toString());
					} else if (detailUser.getAuthtype() == 2) {
						// 不让任何人加为好
						Toast.makeText(FriendManagementActivity.this, "对方不允许加为好友", Toast.LENGTH_SHORT).show();
					}

					// 实现越级跳
					Intent i = new Intent(FriendManagementActivity.this,
							ContactDetail2.class);
					i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(i);
				}
			});
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == SELECT_GROUP_REQUEST_CODE) {
			if (resultCode == UpdateContactGroupActivity.SELECT_GROUP_RESPONSE_CODE_DONE) {
				if (data != null) {
					selectGroupName = data.getStringExtra("groupName");
					selectGroupID = data.getLongExtra("groupID", 0);
					tvGroupName.setText(selectGroupName);
				}
			}else if(resultCode == UpdateContactGroupActivity.SELECT_GROUP_RESPONSE_CODE_CANCEL){	
			}
		}

	}

}
