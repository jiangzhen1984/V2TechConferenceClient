package com.v2tech.view.contacts;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.v2tech.R;
import com.v2tech.logic.GlobalHolder;
import com.v2tech.logic.User;
import com.v2tech.view.PublicIntent;

public class ContactDetail extends Activity {

	private static final int SHOW_USER_INFO = 1;
	private static final int UPDATE_USER_INFO = 2;
	private static final int UPDATE_USER_INFO_DONE = 3;

	private Context mContext;

	private long mUid;
	private User u;
	private LocalHandler lh = new LocalHandler();
	
	
	private TextView mReturnButtonTV;
	private TextView mNameTitleIV;
	private ImageView mHeadIconIV;
	private EditText mNickNameET;
	private TextView mAccountTV;

	// view definition for non-self
	private TextView mUserSignatureTV;
	private TextView mButtonInviteVideoTV;
	private TextView mButtonSendMsgTV;
	private TextView mGenderTV;
	private TextView mBirthdayTV;
	private TextView mCellphoneTV;
	private TextView mTelephoneTV;
	private TextView mComapnyTV;
	private TextView mDepartmentTV;
	private TextView mTitleTV;
	private TextView mAddressTV;
	private TextView[] mTVArr;

	// view for self

	private EditText mGenderET;
	private EditText mBirthdayET;
	private EditText mCellphoneET;
	private EditText mTelephoneET;
	private EditText mComapnyET;
	private EditText mDepartmentET;
	private EditText mTitleET;
	private EditText mAddressET;

	private EditText[] mETArr;
	
	private boolean isUpdating;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_contact_detail);
		mUid = this.getIntent().getExtras().getLong("uid");
		initView();
		mContext = this;
	}

	@Override
	protected void onStart() {
		super.onStart();
		Message.obtain(lh, SHOW_USER_INFO).sendToTarget();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	private void initView() {
		mHeadIconIV = (ImageView) findViewById(R.id.contact_user_detail_head_icon);
		mNameTitleIV = (TextView) findViewById(R.id.contact_user_detail_title);
		mNickNameET = (EditText) findViewById(R.id.contact_user_detail_nick_name_et);
		mAccountTV = (TextView) findViewById(R.id.contact_user_detail_account_tv);

		mReturnButtonTV = (TextView) findViewById(R.id.contact_detail_return_button);
		mReturnButtonTV.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				finish();
			}
			
		});
		// view definition for non-self
		mUserSignatureTV = (TextView) findViewById(R.id.contact_user_detail_user_signature_tv);
		mButtonInviteVideoTV = (TextView) findViewById(R.id.contact_user_detail_invite_video);
		mButtonSendMsgTV = (TextView) findViewById(R.id.contact_user_detail_send_msg);
		mGenderTV = (TextView) findViewById(R.id.contact_user_detail_gender_tv);
		mBirthdayTV = (TextView) findViewById(R.id.contact_user_detail_birthday_tv);
		mCellphoneTV = (TextView) findViewById(R.id.contact_user_detail_cell_phone_tv);
		mTelephoneTV = (TextView) findViewById(R.id.contact_user_detail_telephone_tv);
		mComapnyTV = (TextView) findViewById(R.id.contact_user_detail_company_tv);
		mDepartmentTV = (TextView) findViewById(R.id.contact_user_detail_department_tv);
		mTitleTV = (TextView) findViewById(R.id.contact_user_detail_title_tv);
		mAddressTV = (TextView) findViewById(R.id.contact_user_detail_address_tv);
		mTVArr = new TextView[] { mUserSignatureTV, mButtonInviteVideoTV,
				mButtonSendMsgTV, mGenderTV, mBirthdayTV, mCellphoneTV,
				mTelephoneTV, mComapnyTV, mDepartmentTV, mTitleTV, mAddressTV };

		// view for self
		mGenderET = (EditText) findViewById(R.id.contact_user_detail_gender_et);
		mBirthdayET = (EditText) findViewById(R.id.contact_user_detail_birthday_et);
		mCellphoneET = (EditText) findViewById(R.id.contact_user_detail_cell_phone_et);
		mTelephoneET = (EditText) findViewById(R.id.contact_user_detail_telephone_et);
		mComapnyET = (EditText) findViewById(R.id.contact_user_detail_company_et);
		mDepartmentET = (EditText) findViewById(R.id.contact_user_detail_department_et);
		mTitleET = (EditText) findViewById(R.id.contact_user_detail_title_et);
		mAddressET = (EditText) findViewById(R.id.contact_user_detail_address_et);

		mETArr = new EditText[] { mGenderET, mBirthdayET, mCellphoneET,
				mTelephoneET, mComapnyET, mDepartmentET, mTitleET, mAddressET };
	}

	private void showUserInfo() {
		if (u.getAvatarBitmap() != null) {
			mHeadIconIV.setImageBitmap(u.getAvatarBitmap());
		}
		if (u.getmUserId() == GlobalHolder.getInstance().getCurrentUserId()) {
			for (TextView tv : mTVArr) {
				tv.setVisibility(View.GONE);
			}
			for (EditText et : mETArr) {
				et.setVisibility(View.VISIBLE);
				et.addTextChangedListener(tw);
			}

			if (u.getGender() != null && u.getGender().equals("1")) {
				mGenderET.setText(mContext.getResources().getText(R.string.contacts_user_detail_gender_male));
			} else if (u.getGender()!= null && u.getGender().equals("0")) {
				mGenderET.setText(mContext.getResources().getText(R.string.contacts_user_detail_gender_female));
			} else {
				mGenderET.setText("");
			}
			mNickNameET.addTextChangedListener(tw);
			mBirthdayET.setText(u.getBirthdayStr());
			mCellphoneET.setText(u.getCellPhone());
			mTelephoneET.setText(u.getTelephone());
			mComapnyET.setText(u.getCompany());
			mDepartmentET.setText(u.getDepartment());
			mTitleET.setText(u.getTitle());
			mAddressET.setText(u.getAddress());

		} else {
			for (EditText et : mETArr) {
				et.setVisibility(View.GONE);
			}
			for (TextView tv : mTVArr) {
				tv.setVisibility(View.VISIBLE);
			}

			mUserSignatureTV.setText(u.getSignature());
			if (u.getGender() != null && u.getGender().equals("1")) {
				mGenderTV.setText(mContext.getResources().getText(R.string.contacts_user_detail_gender_male));
			} else if (u.getGender()!= null && u.getGender().equals("0")) {
				mGenderTV.setText(mContext.getResources().getText(R.string.contacts_user_detail_gender_female));
			} else {
				mGenderTV.setText("");
			}
			mBirthdayTV.setText(u.getBirthdayStr());
			mCellphoneTV.setText(u.getCellPhone());
			mTelephoneTV.setText(u.getTelephone());
			mComapnyTV.setText(u.getCompany());
			mDepartmentTV.setText(u.getDepartment());
			mTitleTV.setText(u.getTitle());
			mAddressTV.setText(u.getAddress());

			mButtonInviteVideoTV.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					Intent i = new Intent();
					i.setAction(PublicIntent.START_VIDEO_CONVERSACTION_ACTIVITY);
					i.addCategory(PublicIntent.DEFAULT_CATEGORY);
					i.putExtra("is_coming_call", false);
					i.putExtra("name", u.getName());
					i.putExtra("uid", u.getmUserId());
					mContext.startActivity(i);
				}

			});
			
			mButtonSendMsgTV.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					Intent i = new Intent(PublicIntent.START_CONVERSACTION_ACTIVITY);
					i.putExtra("user1id", GlobalHolder.getInstance().getCurrentUserId());
					i.putExtra("user2id", u.getmUserId());
					i.putExtra("user2Name", u.getName());
					i.addCategory(PublicIntent.DEFAULT_CATEGORY);
					mContext.startActivity(i);
				}

			});

		}
		
		mNameTitleIV.setText(u.getName());
		mNickNameET.setText(u.getName());
		mAccountTV.setText(u.getmEmail());
		
		
		mNickNameET.addTextChangedListener(tw);
		
		//TODO hidden button of invite video conversation
				mButtonInviteVideoTV.setVisibility(View.GONE);

	}

	private TextWatcher tw = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable arg0) {
			if (isUpdating) {
				return;
			}
			isUpdating = true;
			Message m = Message.obtain(lh, UPDATE_USER_INFO);
			lh.sendMessageDelayed(m, 800);
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
	
	
	private void gatherUserData() {
		if (u.isCurrentLoggedInUser()) {
			u.setGender(mGenderET.getText().toString());
			//mBirthdayET.setText(u.getBirthdayStr());
			u.setCellPhone(mCellphoneET.getText().toString());
			u.setTelephone(mTelephoneET.getText().toString());
			u.setCompany(mComapnyET.getText().toString());
			u.setDepartment(mDepartmentET.getText().toString());
			u.setTitle(mTitleET.getText().toString());
			u.setAddress(mAddressET.getText().toString());
		} else {
			u.setName(mNickNameET.getText().toString());
		}
	}
	
	

	class LocalHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case SHOW_USER_INFO:
				u = GlobalHolder.getInstance().getUser(mUid);
				if (u == null) {
					Toast.makeText(mContext,
							R.string.error_contacts_user_detail_no_avai_user,
							Toast.LENGTH_SHORT).show();
				} else {
					showUserInfo();
				}
				break;
			case UPDATE_USER_INFO:
				gatherUserData();
				isUpdating = false;
				break;
			case UPDATE_USER_INFO_DONE:
				break;
			}
		}

	}

}
