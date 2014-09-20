package com.v2tech.view.contacts.add;

import com.v2tech.R;
import com.v2tech.service.ContactsService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.view.MainActivity;
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

	long mUid;
	User detailUser;
	private ContactsService contactService = new ContactsService();

	// 两个标题
	private ImageView mHeadIconIV;
	// contact_user_detail_head_icon
	private TextView mNameTitleIV;
	// contact_user_detail_title
	private TextView mSignTV;

	String startedCause;
	// R.id.right_text_view
	private TextView tvRightTextView;

	// contact_user_detail_user_signature_tv

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_contact_add_authentication);

		// 返回
		findViewById(R.id.contact_detail_return_button).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						onBackPressed();
					}
				});

		// 原因分界面
		// 界面处理
		tvRightTextView = (TextView) findViewById(R.id.right_text_view);
		startedCause = this.getIntent().getStringExtra("cause");
		if ((startedCause != null)
				&& startedCause.equals("refuse_friend_authentication")) {
			mUid = this.getIntent().getLongExtra("remoteUserID", 0);
			tvRightTextView.setText("完成");
			tvRightTextView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					// 拒绝加为好友
					contactService.refuseAddedAsContact(30,
							mUid, ((EditText) findViewById(R.id.editText1))
									.getText().toString());
					AddFriendHistroysHandler.addMeRefuse(getApplicationContext(),mUid,
							((EditText) findViewById(R.id.editText1)).getText()
									.toString());
					// 实现越级跳
					Intent i = new Intent(AuthenticationActivity.this,
							MainActivity.class);
					i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(i);
				}
			});

		} else {
			mUid = this.getIntent().getLongExtra("uid", 0);

			// 下一步
			tvRightTextView.setText("下一步");
			tvRightTextView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					Intent i = new Intent(AuthenticationActivity.this,
							FriendManagementActivity.class);
					i.putExtra("uid", mUid);
					i.putExtra("verificationInfo",
							((EditText) findViewById(R.id.editText1)).getText()
									.toString());
					AuthenticationActivity.this.startActivity(i);
				}
			});
		}

		detailUser = GlobalHolder.getInstance().getUser(mUid);

		mHeadIconIV = (ImageView) findViewById(R.id.contact_user_detail_head_icon);
		mNameTitleIV = (TextView) findViewById(R.id.contact_user_detail_title);
		mSignTV = (TextView) findViewById(R.id.contact_user_detail_user_signature_tv);

		if (detailUser.getAvatarBitmap() != null) {
			mHeadIconIV.setImageBitmap(detailUser.getAvatarBitmap());
		}

		mNameTitleIV.setText(detailUser.getName());

		mSignTV.setText(detailUser.getSignature());

	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

}
