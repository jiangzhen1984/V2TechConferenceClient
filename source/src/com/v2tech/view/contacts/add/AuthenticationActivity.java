package com.v2tech.view.contacts.add;

import com.v2tech.R;
import com.v2tech.service.ContactsService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.view.MainActivity;
import com.v2tech.view.conversation.MessageAuthenticationActivity;
import com.v2tech.vo.User;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class AuthenticationActivity extends Activity {
	// 两个标题
	private ImageView mHeadIconIV;
	// contact_user_detail_head_icon
	private TextView mNameTitleIV;
	// contact_user_detail_title
	private TextView mSignTV;
	// R.id.tv_left
	private TextView tvLeft;
	// R.id.right_text_view
	private TextView tvRightTextView;
	// R.id.tv_title
	private TextView tvTitle;
	// R.id.tv_input
	private EditText etInput;

	String startedCause;
	long mUid;
	User detailUser;
	private ContactsService contactService = new ContactsService();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_contact_add_authentication);
		startedCause = this.getIntent().getStringExtra("cause");
		connectView();
		bindViewEnvent();
		if ((startedCause != null)
				&& startedCause.equals("refuse_friend_authentication")) {
			mUid = this.getIntent().getLongExtra("remoteUserID", 0);
			tvLeft.setText("返回");
			tvTitle.setText("拒绝理由");
			etInput.setHint("请输入拒绝理由");
		} else {
			mUid = this.getIntent().getLongExtra("uid", 0);
			tvLeft.setText("个人资料");
			tvTitle.setText("身份验证");
			etInput.setHint("请输入验证信息");

		}
		detailUser = GlobalHolder.getInstance().getUser(mUid);
		if (detailUser.getAvatarBitmap() != null) {
			mHeadIconIV.setImageBitmap(detailUser.getAvatarBitmap());
		}
		mNameTitleIV.setText(detailUser.getName());
		mSignTV.setText(detailUser.getSignature());
	}
	
	
	

	@Override
	protected void onDestroy() {
		super.onDestroy();
		contactService.clearCalledBack();
	}




	private void connectView() {
		tvLeft = (TextView) findViewById(R.id.tv_left);
		tvRightTextView = (TextView) findViewById(R.id.right_text_view);
		mHeadIconIV = (ImageView) findViewById(R.id.contact_user_detail_head_icon);
		mNameTitleIV = (TextView) findViewById(R.id.contact_user_detail_title);
		mSignTV = (TextView) findViewById(R.id.contact_user_detail_user_signature_tv);
		if ((startedCause != null)
				&& startedCause.equals("refuse_friend_authentication")) {
			tvRightTextView.setText("完成");
		} else {
			// 下一步
			tvRightTextView.setText("下一步");
		}

		tvTitle = (TextView) findViewById(R.id.tv_title);
		etInput = (EditText) findViewById(R.id.et_input);

	}

	private void bindViewEnvent() {
		// 返回
		tvLeft.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				onBackPressed();
			}
		});

		if ((startedCause != null)
				&& startedCause.equals("refuse_friend_authentication")) {
			tvRightTextView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					// 拒绝加为好友
					contactService.refuseAddedAsContact(30, mUid, etInput
							.getText().toString());
					AddFriendHistroysHandler.addMeRefuse(
							getApplicationContext(), mUid, etInput.getText()
									.toString());
					// 实现越级跳
					Intent i = new Intent(AuthenticationActivity.this,
							MessageAuthenticationActivity.class);
					i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(i);
				}
			});
		} else {
			tvRightTextView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					Intent i = new Intent(AuthenticationActivity.this,
							FriendManagementActivity.class);
					i.putExtra("uid", mUid);
					i.putExtra("verificationInfo", etInput.getText().toString());
					AuthenticationActivity.this.startActivity(i);
				}
			});
		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

}
