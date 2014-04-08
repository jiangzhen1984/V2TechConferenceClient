package com.v2tech.view.contacts;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.app.DatePickerDialog;
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
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.v2tech.R;
import com.v2tech.logic.GlobalHolder;
import com.v2tech.logic.User;
import com.v2tech.service.UserService;
import com.v2tech.util.V2Log;
import com.v2tech.view.PublicIntent;

public class ContactDetail extends Activity implements OnTouchListener {

	private static final int SHOW_USER_INFO = 1;
	private static final int UPDATE_USER_INFO = 2;
	private static final int UPDATE_USER_INFO_DONE = 3;

	private Context mContext;

	private long mUid;
	private User u;
	private LocalHandler lh = new LocalHandler();
	private UserService us = new UserService();

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

	private RadioGroup mGenderRG;
	private EditText mBirthdayET;
	private EditText mCellphoneET;
	private EditText mTelephoneET;
	private EditText mTitleET;
	private EditText mAddressET;

	private EditText[] mETArr;

	private boolean isUpdating;

	private boolean isChanged;

	private Date bir;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_contact_detail);
		mUid = this.getIntent().getExtras().getLong("uid");
		initView();
		mContext = this;
	    View v = findViewById(R.id.contact_detail_main_layout);
	    v.setOnTouchListener(this);
	    v = findViewById(R.id.contact_detail_scroll_view);
	    if (v != null) {
	    	 v.setOnTouchListener(this);
	    }
	}

	@Override
	protected void onStart() {
		super.onStart();
		u = GlobalHolder.getInstance().getUser(mUid);
		if (u != null) {
			showUserInfo();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
	}
	
	
	
	

	@Override
	public boolean onTouch(View view, MotionEvent mv) {
		//contact_detail_main_layout
		for (View v : mETArr) {
			if (v == null) {
				continue;
			}
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
		}
		return super.onTouchEvent(mv);
	}

	private void initView() {
		mHeadIconIV = (ImageView) findViewById(R.id.contact_user_detail_head_icon);
		mNameTitleIV = (TextView) findViewById(R.id.contact_user_detail_title);
		mNickNameET = (EditText) findViewById(R.id.contact_user_detail_nick_name_et);
		mAccountTV = (TextView) findViewById(R.id.contact_user_detail_account_tv);

		mReturnButtonTV = (TextView) findViewById(R.id.contact_detail_return_button);
		mReturnButtonTV.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				for (View v : mETArr) {
					if (v == null) {
						continue;
					}
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
				}
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
				mTelephoneTV, mTitleTV, mAddressTV };

		// view for self
		mGenderRG = (RadioGroup) findViewById(R.id.contact_user_detail_gender_rg);
		mBirthdayET = (EditText) findViewById(R.id.contact_user_detail_birthday_et);
		mCellphoneET = (EditText) findViewById(R.id.contact_user_detail_cell_phone_et);
		mTelephoneET = (EditText) findViewById(R.id.contact_user_detail_telephone_et);
		mTitleET = (EditText) findViewById(R.id.contact_user_detail_title_et);
		mAddressET = (EditText) findViewById(R.id.contact_user_detail_address_et);

		mETArr = new EditText[] { mCellphoneET, mTelephoneET,
				mTitleET, mAddressET, mBirthdayET };
	}

	private void showUserInfo() {
		if (u.getAvatarBitmap() != null) {
			mHeadIconIV.setImageBitmap(u.getAvatarBitmap());
		}

		if (u.getmUserId() == GlobalHolder.getInstance().getCurrentUserId()) {
			for (TextView tv : mTVArr) {
				tv.setVisibility(View.GONE);
			}

			mGenderRG.setVisibility(View.VISIBLE);
			mGenderRG.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(RadioGroup rg, int id) {
					isUpdating = true;
					Message m = Message.obtain(lh, UPDATE_USER_INFO);
					lh.dispatchMessage(m);
				}

			});

			selectedRG(u.getGender());
			mBirthdayET.setText(u.getBirthdayStr());
			mCellphoneET.setText(u.getCellPhone());
			mTelephoneET.setText(u.getTelephone());
			mTitleET.setText(u.getTitle());
			mAddressET.setText(u.getAddress());

			for (EditText et : mETArr) {
				et.setVisibility(View.VISIBLE);
				et.addTextChangedListener(tw);
				et.setOnFocusChangeListener(hidenKeyboardListener);
			}
			mBirthdayET.setVisibility(View.VISIBLE);

		} else {
			for (EditText et : mETArr) {
				et.setVisibility(View.GONE);
			}
			for (TextView tv : mTVArr) {
				tv.setVisibility(View.VISIBLE);
			}
			mGenderRG.setVisibility(View.GONE);

			mUserSignatureTV.setText(u.getSignature());
			if (u.getGender() != null && u.getGender().equals("1")) {
				mGenderTV.setText(mContext.getResources().getText(
						R.string.contacts_user_detail_gender_male));
			} else if (u.getGender() != null && u.getGender().equals("2")) {
				mGenderTV.setText(mContext.getResources().getText(
						R.string.contacts_user_detail_gender_female));
			} else {
				mGenderTV.setText("");
			}
			mBirthdayTV.setText(u.getBirthdayStr());
			mCellphoneTV.setText(u.getCellPhone());
			mTelephoneTV.setText(u.getTelephone());
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
					Intent i = new Intent(
							PublicIntent.START_CONVERSACTION_ACTIVITY);
					i.putExtra("user1id", GlobalHolder.getInstance()
							.getCurrentUserId());
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
		mComapnyTV.setText(u.getCompany());
		mDepartmentTV.setText(u.getDepartment());

		// TODO hidden button of invite video conversation
		mButtonInviteVideoTV.setVisibility(View.INVISIBLE);

		mNickNameET.addTextChangedListener(tw);
		mBirthdayET.setOnClickListener(datePickerListener);
		bir = u.getBirthday();

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

	private View.OnClickListener datePickerListener = new View.OnClickListener() {
		
		@Override
		public void onClick(final View view) {
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
			Calendar c = Calendar.getInstance();

			new DatePickerDialog(mContext,
					new DatePickerDialog.OnDateSetListener() {
						@Override
						public void onDateSet(DatePicker dp, int year,
								int monthOfYear, int dayOfMonth) {
							((EditText) view).setText(year + "/"
									+ (monthOfYear + 1) + "/" + dayOfMonth);
							Calendar cl = Calendar.getInstance();
							cl.set(Calendar.YEAR, year);
							cl.set(Calendar.MONTH, monthOfYear);
							cl.set(Calendar.DAY_OF_MONTH, dayOfMonth);
							
							isUpdating = true;
							Message m = Message.obtain(lh, UPDATE_USER_INFO);
							lh.dispatchMessage(m);
							bir = cl.getTime();
							
						}
					}, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c
							.get(Calendar.DAY_OF_MONTH)).show();
		}

	};
	
	
	private OnFocusChangeListener hidenKeyboardListener = new OnFocusChangeListener() {

		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			if (!hasFocus) {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
			}
		}
		
	};

	private void gatherUserData() {
		if (u.getmUserId() == GlobalHolder.getInstance().getCurrentUserId()) {
			u.setName(mNickNameET.getText().toString());
			u.setGender(getRadioValue());
			u.setBirthday(bir);
			u.setCellPhone(mCellphoneET.getText().toString());
			u.setTelephone(mTelephoneET.getText().toString());
			u.setTitle(mTitleET.getText().toString());
			u.setAddress(mAddressET.getText().toString());
		} else {
			u.setName(mNickNameET.getText().toString());
		}
	}

	private String getRadioValue() {
		for (int i = 0; i < mGenderRG.getChildCount(); i++) {
			RadioButton rg = (RadioButton) mGenderRG.getChildAt(i);
			if (rg.isChecked()) {
				return rg.getTag().toString();
			}
		}
		return "1";
	}

	private void selectedRG(String genderVal) {
		for (int i = 0; i < mGenderRG.getChildCount(); i++) {
			RadioButton rg = (RadioButton) mGenderRG.getChildAt(i);
			if (rg.getTag().equals(genderVal)) {
				rg.setChecked(true);
			}
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
					us.updateUser(u,
							Message.obtain(this, UPDATE_USER_INFO_DONE));
				isUpdating = false;
				break;
			case UPDATE_USER_INFO_DONE:
				break;
			}
		}

	}

}
