package com.v2tech.view.contacts.add;

import com.v2tech.R;
import com.v2tech.service.FriendGroupService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.view.MainActivity;
import com.v2tech.view.contacts.ContactDetail2;
import com.v2tech.vo.FriendGroup;
import com.v2tech.vo.User;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class FriendManagementActivity extends Activity {

	long mUid;
	User detailUser;
	String verificationInfo;
	EditText commentNameET;
	String startedCause;
	// R.id.right_text_view
	private TextView tvRightTextView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_contact_add_friend_management);
		tvRightTextView = (TextView) findViewById(R.id.right_text_view);

		startedCause = this.getIntent().getStringExtra("cause");
		if ((startedCause != null)
				&& startedCause.equals("access_friend_authentication")) {
			mUid = this.getIntent().getLongExtra("remoteUserID", 0);
			tvRightTextView.setText("完成");
			tvRightTextView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					// 同意加为好友,默认组为30
					new FriendGroupService().acceptInviteJoinFriendGroup(30,
							mUid);
					// 实现越级跳
					Intent i = new Intent(FriendManagementActivity.this,
							MainActivity.class);
					i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(i);
				}
			});

		} else {
			mUid = this.getIntent().getLongExtra("uid", 0);
			verificationInfo = this.getIntent()
					.getCharSequenceExtra("verificationInfo").toString();
			// 发送
			tvRightTextView.setText("发送");
			tvRightTextView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {

					if (detailUser.getAuthtype() == 0) {
						AddFriendHistroysHandler
								.addOtherNoNeedAuthentication(detailUser);
						new FriendGroupService().AddFriendGroupUser(
								new FriendGroup(30, ""), detailUser,
								verificationInfo, commentNameET.getText()
										.toString());
					} else if (detailUser.getAuthtype() == 1) {
						AddFriendHistroysHandler.addOtherNeedAuthentication(
								detailUser, commentNameET.getText().toString());
						new FriendGroupService().AddFriendGroupUser(
								new FriendGroup(30, ""), detailUser,
								verificationInfo, commentNameET.getText()
										.toString());
					} else if (detailUser.getAuthtype() == 2) {
						//不让任何人加为好友
					}

					// 实现越级跳
					Intent i = new Intent(FriendManagementActivity.this,
							ContactDetail2.class);
					i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(i);
				}
			});
		}

		// 返回
		findViewById(R.id.contact_detail_return_button).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						onBackPressed();
					}
				});

		// 选择分组
		findViewById(R.id.select_group).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						Intent i = new Intent(FriendManagementActivity.this,
								SelectGroupActivity.class);
						FriendManagementActivity.this.startActivity(i);
					}
				});

		detailUser = GlobalHolder.getInstance().getUser(mUid);
		commentNameET = (EditText) findViewById(R.id.comment_name_et);
		commentNameET.setText(detailUser.getName());

	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

}
