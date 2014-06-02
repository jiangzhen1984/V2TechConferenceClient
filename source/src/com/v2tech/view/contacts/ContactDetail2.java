package com.v2tech.view.contacts;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.Registrant;
import com.v2tech.service.UserService;
import com.v2tech.view.PublicIntent;
import com.v2tech.vo.User;

public class ContactDetail2 extends Activity implements OnTouchListener {

	private static final int UPDATE_USER_INFO = 2;
	private static final int UPDATE_USER_INFO_DONE = 3;

	private Context mContext;

	private long mUid;
	private User u;
	private LocalHandler lh = new LocalHandler();
	private UserService us = new UserService();

	private View mReturnButtonTV;
	private TextView mNameTitleIV;
	private ImageView mHeadIconIV;

	// view definition for non-self
	private TextView mAccountTV;
	private TextView mGendarTV;
	private TextView mBirthdayTV;
	private TextView mCellphoneTV;
	private TextView mTelephoneTV;
	private TextView mTitleTV;
	private TextView mAddressTV;
	private TextView mSignTV;
	private TextView mDeptTV;
	private TextView mCompanyTV;

	private View mAddContactButton;
	private View mSendMsgButton;
	private View mCreateConfButton;

	private boolean isUpdating;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.activity_contact_detail_2);
		mUid = this.getIntent().getLongExtra("uid", 0);
		initView();
		mContext = this;
		u = GlobalHolder.getInstance().getUser(mUid);

		this.overridePendingTransition(R.animator.alpha_from_0_to_1,
				R.animator.alpha_from_1_to_0);
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (u != null) {
			showUserInfo();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	@Override
	public void finish() {
		super.finish();
		this.overridePendingTransition(R.animator.alpha_from_0_to_1,
				R.animator.alpha_from_1_to_0);
	}

	@Override
	public boolean onTouch(View view, MotionEvent mv) {
		return super.onTouchEvent(mv);
	}

	private void initView() {
		mHeadIconIV = (ImageView) findViewById(R.id.contact_user_detail_head_icon);
		mNameTitleIV = (TextView) findViewById(R.id.contact_user_detail_title);

		mReturnButtonTV = (TextView) findViewById(R.id.contact_detail_2_return_button);
		mReturnButtonTV.setOnClickListener(mReturnButtonListener);

		mSendMsgButton = findViewById(R.id.contact_user_detail_2_send_msg);
		mSendMsgButton.setOnClickListener(mSendMsgListener);
		mAddContactButton = findViewById(R.id.contact_user_detail_add_friend);
		mCreateConfButton = findViewById(R.id.contact_user_detail_invite_video);

		mAccountTV = (TextView) findViewById(R.id.contact_user_detail_account_tv);
		mGendarTV = (TextView) findViewById(R.id.contact_user_detail_gender_tv);
		mBirthdayTV = (TextView) findViewById(R.id.contact_user_detail_birthday_tv);

		mTitleTV = (TextView) findViewById(R.id.contact_user_detail_title_tv);
		mAddressTV = (TextView) findViewById(R.id.contact_user_detail_address_tv);
		mCellphoneTV = (TextView) findViewById(R.id.contact_user_detail_cell_phone_tv);
		mTelephoneTV = (TextView) findViewById(R.id.contact_user_detail_telephone_tv);
		mSignTV = (TextView) findViewById(R.id.contact_user_detail_user_signature_tv);
		mDeptTV = (TextView) findViewById(R.id.contact_user_detail_department_tv);
		mCompanyTV = (TextView) findViewById(R.id.contact_user_detail_company_tv);

	}

	private void showUserInfo() {
		if (u.getAvatarBitmap() != null) {
			mHeadIconIV.setImageBitmap(u.getAvatarBitmap());
		}

		mNameTitleIV.setText(u.getName());
		mAccountTV.setText(u.getAccount());
		if (u.getGender() != null) {
			if (u.getGender().equals("0")) {
				mGendarTV.setText("保密");
			} else if (u.getGender().equals("1")) {
				mGendarTV.setText("男");
			} else if (u.getGender().equals("2")) {
				mGendarTV.setText("女");
			}

		} else {
			mGendarTV.setText("");
		}
		mBirthdayTV.setText(u.getBirthdayStr());
		mCellphoneTV.setText(u.getCellPhone());
		mTelephoneTV.setText(u.getTelephone());
		mTitleTV.setText(u.getTitle());
		mAddressTV.setText(u.getAddress());
		mSignTV.setText(u.getSignature());
		mDeptTV.setText(u.getDepartment());
		mCompanyTV.setText(u.getCompany());

	}

	private TextWatcher tw = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable ed) {
			if (isUpdating) {
				return;
			}
			isUpdating = true;
			Message m = Message.obtain(lh, UPDATE_USER_INFO);
			lh.sendMessageDelayed(m, 2000);
		}

		@Override
		public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {

		}

		@Override
		public void onTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {

		}

	};

	private View.OnClickListener mSendMsgListener = new View.OnClickListener() {

		@Override
		public void onClick(View arg0) {

			Intent i = new Intent(PublicIntent.START_CONVERSACTION_ACTIVITY);
			i.putExtra("user1id", GlobalHolder.getInstance().getCurrentUserId());
			i.putExtra("user2id", u.getmUserId());
			i.putExtra("user2Name", u.getName());
			i.addCategory(PublicIntent.DEFAULT_CATEGORY);
			mContext.startActivity(i);
		}

	};

	private View.OnClickListener mCreateConfMsgListener = new View.OnClickListener() {

		@Override
		public void onClick(View arg0) {
			// FIXME fix bug for enter conference and refresh group list
			Intent i = new Intent(PublicIntent.START_CONFERENCE_CREATE_ACTIVITY);
			i.addCategory(PublicIntent.DEFAULT_CATEGORY);
			startActivityForResult(i, 0);
		}

	};

	private View.OnClickListener mReturnButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			onBackPressed();
		}

	};

	private void gatherUserData() {
		// TODO add implement
	}

	class LocalHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPDATE_USER_INFO:
				gatherUserData();
				us.updateUser(u, new Registrant(this, UPDATE_USER_INFO_DONE,
						null));
				isUpdating = false;
				break;
			case UPDATE_USER_INFO_DONE:
				break;
			}
		}

	}

}
