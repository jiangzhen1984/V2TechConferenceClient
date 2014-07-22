package com.v2tech.view.contacts;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.Registrant;
import com.v2tech.service.UserService;
import com.v2tech.util.SPUtil;
import com.v2tech.view.PublicIntent;
import com.v2tech.view.bo.ConversationNotificationObject;
import com.v2tech.vo.Conversation;
import com.v2tech.vo.User;
import com.v2tech.vo.UserDeviceConfig;

public class ContactDetail extends Activity implements OnTouchListener {

	private static final int UPDATE_USER_INFO = 2;
	private static final int UPDATE_USER_INFO_DONE = 3;

	private static final int FILE_SELECT_CODE = 100;

	private Context mContext;

	private long mUid;
	private User u;
	private LocalHandler lh = new LocalHandler();
	private UserService us = new UserService();

	private View mReturnButtonTV;
	private TextView mNameTitleIV;
	private ImageView mHeadIconIV;

	// view definition for non-self
	private TextView mCellphoneTV;
	private TextView mTelephoneTV;
	private TextView mTitleTV;
	private TextView mAddressTV;
	private TextView mFaxTV;
	private TextView mEmailTV;
	private TextView mCompanyTitleTV;
	private TextView mSignTV;
	private View[] mTVArr;
	private View mItemsContainer;

	private View mContactButtonContainer;
	private View mSendMsgBottomButton;
	private View mCallButtomButton;
	private View mCreateConfButton;
	private View mMoreDetailButton;
	private View mVideoCallButton;
	private View mSendSmsButton;
	private View mSendFilesButton;

	// view for self
	private EditText mSignature;
	private TextView mAccountTV;
	private RadioGroup mGenderRG;
	private EditText mBirthdayET;
	private EditText mCellphoneET;
	private EditText mTelephoneET;
	private EditText mTitleET;
	private EditText mAddressET;
	private TextView mDepartmentSelfTV;
	private TextView mCompanySelfTV;
	private View mSelfItemsContainer;
	private EditText[] mETArr;

	private boolean isUpdating;

	private Date bir;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.activity_contact_detail);
		mUid = this.getIntent().getLongExtra("uid", 0);
		initView();
		mContext = this;
		View v = findViewById(R.id.contact_detail_main_layout);
		v.setOnTouchListener(this);
		v = findViewById(R.id.contact_detail_scroll_view);
		if (v != null) {
			v.setOnTouchListener(this);
		}

		this.overridePendingTransition(R.animator.alpha_from_0_to_1,
				R.animator.alpha_from_1_to_0);
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
	protected void onDestroy() {
		super.onDestroy();
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
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (intent != null) {
			mUid = intent.getLongExtra("uid", 0);
			u = GlobalHolder.getInstance().getUser(mUid);
			if (u != null) {
				showUserInfo();
			}
		}
	}

	@Override
	public boolean onTouch(View view, MotionEvent mv) {
		// contact_detail_main_layout
		for (View v : mETArr) {
			if (v == null) {
				continue;
			}
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
		}
		return super.onTouchEvent(mv);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case FILE_SELECT_CODE:
			if (resultCode == RESULT_OK) {
				// Get the Uri of the selected file
				Uri uri = data.getData();
				String path = SPUtil.getPath(this, uri);
				if (path == null) {
					Toast.makeText(
							mContext,
							R.string.contacts_user_detail_file_selection_not_found_path,
							Toast.LENGTH_SHORT).show();
				}
				else{
					//return current conversationView with selected file
					returnConversationView(path);
				}
			}
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void initView() {
		mHeadIconIV = (ImageView) findViewById(R.id.contact_user_detail_head_icon);
		mNameTitleIV = (TextView) findViewById(R.id.contact_user_detail_title);

		mReturnButtonTV = (TextView) findViewById(R.id.contact_detail_return_button);
		mReturnButtonTV.setOnClickListener(mReturnButtonListener);

		mMoreDetailButton = findViewById(R.id.contact_user_detail_2);
		mMoreDetailButton.setOnClickListener(mMoreDetailListener);

		// view definition for non-self
		mItemsContainer = findViewById(R.id.contact_detail_items_ly);
		mSendMsgBottomButton = findViewById(R.id.contact_user_detail_send_bottom_button);
		mCallButtomButton = findViewById(R.id.contact_user_detail_call_bottom_button);
		mContactButtonContainer = findViewById(R.id.contact_button_ly);
		mCreateConfButton = findViewById(R.id.contact_user_detail_create_conf_bottom_button);
		mVideoCallButton = findViewById(R.id.contact_user_detail_video_call_bottom_button);
		mSendSmsButton = findViewById(R.id.contact_user_detail_send_sms_bottom_button);
		mSendFilesButton = findViewById(R.id.contact_user_detail_send_files_bottom_button);

		mTitleTV = (TextView) findViewById(R.id.contact_user_n_detail_title_tv);
		mAddressTV = (TextView) findViewById(R.id.contact_user_n_detail_address_tv);
		mEmailTV = (TextView) findViewById(R.id.contact_user_n_detail_email_tv);
		mCellphoneTV = (TextView) findViewById(R.id.contact_user_n_detail_cell_phone_tv);
		mTelephoneTV = (TextView) findViewById(R.id.contact_user_n_detail_telephone_tv);
		mFaxTV = (TextView) findViewById(R.id.contact_user_n_detail_fax_tv);
		mCompanyTitleTV = (TextView) findViewById(R.id.contact_user_company);
		mSignTV = (TextView) findViewById(R.id.contact_user_detail_user_signature_tv);

		mTVArr = new View[] { mItemsContainer, mCompanyTitleTV,
				mContactButtonContainer, mMoreDetailButton, mSignTV };

		// view for self
		mSelfItemsContainer = findViewById(R.id.contact_detail_self_items_ly);
		mSignature = (EditText) findViewById(R.id.contact_user_detail_signature_et);
		mAccountTV = (TextView) findViewById(R.id.contact_user_detail_account_tv);
		mGenderRG = (RadioGroup) findViewById(R.id.contact_user_detail_gender_rg);
		mBirthdayET = (EditText) findViewById(R.id.contact_user_detail_birthday_et);
		mCellphoneET = (EditText) findViewById(R.id.contact_user_detail_cell_phone_et);
		mTelephoneET = (EditText) findViewById(R.id.contact_user_detail_telephone_et);
		mTitleET = (EditText) findViewById(R.id.contact_user_detail_title_et);
		mAddressET = (EditText) findViewById(R.id.contact_user_detail_address_et);
		mCompanySelfTV = (TextView) findViewById(R.id.contact_user_detail_company_tv);
		mDepartmentSelfTV = (TextView) findViewById(R.id.contact_user_detail_department_tv);

		mETArr = new EditText[] { mSignature, mCellphoneET, mTelephoneET,
				mTitleET, mAddressET };
	}

	private void showUserInfo() {
		if (u.getAvatarBitmap() != null) {
			mHeadIconIV.setImageBitmap(u.getAvatarBitmap());
		}

		mNameTitleIV.setText(u.getName());

		// for self
		if (u.getmUserId() == GlobalHolder.getInstance().getCurrentUserId()) {
			for (View tv : mTVArr) {
				tv.setVisibility(View.GONE);
			}
			mSelfItemsContainer.setVisibility(View.VISIBLE);

			mGenderRG.setVisibility(View.VISIBLE);
			selectedRG(u.getGender());
			mGenderRG.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(RadioGroup rg, int id) {
					isUpdating = true;
					Message m = Message.obtain(lh, UPDATE_USER_INFO);
					lh.dispatchMessage(m);
				}

			});
			mAccountTV.setText(u.getAccount());
			mSignature.setText(u.getSignature());
			mBirthdayET.setText(u.getBirthdayStr());
			mCellphoneET.setText(u.getCellPhone());
			mTelephoneET.setText(u.getTelephone());
			mTitleET.setText(u.getTitle());
			mAddressET.setText(u.getAddress());
			mDepartmentSelfTV.setText(u.getDepartment());
			mCompanySelfTV.setText(u.getCompany());

			for (EditText et : mETArr) {
				et.setVisibility(View.VISIBLE);
				et.addTextChangedListener(tw);
				et.setOnFocusChangeListener(hidenKeyboardListener);
			}
			mBirthdayET.setVisibility(View.VISIBLE);

			mBirthdayET.setOnClickListener(datePickerListener);
			bir = u.getBirthday();

		} else {
			for (EditText et : mETArr) {
				et.setVisibility(View.GONE);
			}
			for (View tv : mTVArr) {
				tv.setVisibility(View.VISIBLE);
			}
			mSelfItemsContainer.setVisibility(View.GONE);
			mGenderRG.setVisibility(View.GONE);

			mCellphoneTV.setText(u.getCellPhone());
			mTelephoneTV.setText(u.getTelephone());
			mTitleTV.setText(u.getTitle());
			mAddressTV.setText(u.getAddress());
			mCompanyTitleTV.setText(u.getCompany());
			mFaxTV.setText(u.getFax());
			mEmailTV.setText(u.getmEmail());
			mSignTV.setText(u.getSignature());

			mSendMsgBottomButton.setOnClickListener(mSendMsgListener);
			if (mCallButtomButton != null) {
				mCallButtomButton.setOnClickListener(mCallButtonListener);
			}
			mCreateConfButton.setOnClickListener(mCreateConfMsgListener);

			mVideoCallButton.setOnClickListener(mVideoCallButtonListener);

			if (mSendSmsButton != null) {
				mSendSmsButton.setOnClickListener(mSendSmsMsgListener);
			}
			mSendFilesButton.setOnClickListener(mfileSelectionButtonListener);

		}

	}

	private void startVoiceCall() {
		Intent iv = new Intent();
		iv.addCategory(PublicIntent.DEFAULT_CATEGORY);
		iv.setAction(PublicIntent.START_P2P_CONVERSACTION_ACTIVITY);
		iv.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		iv.putExtra("uid", mUid);
		iv.putExtra("is_coming_call", false);
		iv.putExtra("voice", true);
		mContext.startActivity(iv);
	}

	private void startVideoCall() {
		Intent iv = new Intent();
		iv.addCategory(PublicIntent.DEFAULT_CATEGORY);
		iv.setAction(PublicIntent.START_P2P_CONVERSACTION_ACTIVITY);
		iv.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		iv.putExtra("uid", mUid);
		iv.putExtra("is_coming_call", false);
		iv.putExtra("voice", false);
		List<UserDeviceConfig> list = GlobalHolder.getInstance()
				.getAttendeeDevice(mUid);
		if (list != null && list.size() > 0) {
			iv.putExtra("device", list.get(0).getDeviceID());
		} else {
			iv.putExtra("device", "");
		}
		mContext.startActivity(iv);
	}

	Dialog d = null;

	private void showCallDialog(String tel, String phone, boolean voice) {

		d = new Dialog(mContext, R.style.ContactUserDetailVoiceCallDialog);

		d.setContentView(R.layout.contacts_user_detail_call_dialog_window);
		TextView tv = (TextView) d
				.findViewById(R.id.contact_user_detail_call_dialog_1);
		tv.setOnClickListener(itemClickListener);
		TextView tv1 = (TextView) d
				.findViewById(R.id.contact_user_detail_call_dialog_2);
		tv1.setOnClickListener(itemClickListener);

		if (tel != null && phone != null) {
			tv.setTag("call:" + tel + "|" + phone);
		} else if (tel != null || phone != null) {
			tv.setTag(tel == null ? phone : tel);
		} else {
			tv.setTag("");
			// tv.setVisibility(View.GONE);
		}

		if (voice) {
			tv1.setTag("voice");
		} else {
			tv1.setVisibility(View.GONE);
		}

		d.show();

	}

	private OnClickListener itemClickListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			String tag = (String) view.getTag();
			if (tag != null && tag.startsWith("call:")) {
				String[] nums = tag.substring(5).split("\\|");

				TextView tv = (TextView) d
						.findViewById(R.id.contact_user_detail_call_dialog_1);
				tv.setOnClickListener(itemClickListener);
				tv.setText(nums[0]);
				tv.setTag(nums[0]);
				
				if (nums.length > 1) {
					TextView tv1 = (TextView) d
							.findViewById(R.id.contact_user_detail_call_dialog_2);
					tv1.setOnClickListener(itemClickListener);
					tv1.setText(nums[1]);
					tv1.setTag(nums[1]);
				}
				return;
			}

			if (d != null) {
				d.dismiss();
				d = null;
			}
			if (view.getTag() != null && view.getTag().equals("voice")) {
				startVoiceCall();
			} else {
				if (view.getTag() == null
						|| view.getTag().toString().equals("")) {
					Intent intent = new Intent();
					intent.setAction(Intent.ACTION_DIAL);
					startActivity(intent);

				} else {
					Intent intent = new Intent();
					intent.setAction(Intent.ACTION_CALL);
					intent.setData(Uri.parse("tel:" + (String) view.getTag()));
					startActivity(intent);
				}
			}
		}

	};

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

	private View.OnClickListener mCallButtonListener = new View.OnClickListener() {

		@Override
		public void onClick(View arg0) {
			boolean phoneEmpty = u.getTelephone() == null
					|| u.getTelephone().isEmpty();
			boolean mobileEmpty = u.getCellPhone() == null
					|| u.getCellPhone().isEmpty();
			boolean offline = (u.getmStatus() == User.Status.OFFLINE || u
					.getmStatus() == User.Status.HIDDEN);

			if (offline) {
				if (phoneEmpty && mobileEmpty) {
					Intent intent = new Intent();
					intent.setAction(Intent.ACTION_DIAL); // android.intent.action.DIAL
					startActivity(intent);
				} else if (!phoneEmpty && !mobileEmpty) {
					showCallDialog(u.getTelephone(), u.getCellPhone(), false);
				} else {
					Intent intent = new Intent();
					intent.setAction(Intent.ACTION_CALL);
					if (!phoneEmpty) {
						intent.setData(Uri.parse("tel:" + u.getTelephone()));
					} else {
						intent.setData(Uri.parse("tel:" + u.getCellPhone()));
					}
					startActivity(intent);
				}
			} else {
				if (phoneEmpty && mobileEmpty) {
					showCallDialog(null, null, true);
				} else if (!phoneEmpty || !mobileEmpty) {
					showCallDialog(u.getTelephone(), u.getCellPhone(), true);
				} else {
					showCallDialog(u.getTelephone(), u.getCellPhone(), true);
				}
			}
		}

	};

	private View.OnClickListener mSendMsgListener = new View.OnClickListener() {

		@Override
		public void onClick(View arg0) {

			returnConversationView(null);
		}
	};
	
	/**
	 * return ConversationView with infos
	 */
	private void returnConversationView(String selectedFile) {
		Intent i = new Intent(PublicIntent.START_CONVERSACTION_ACTIVITY);
		i.putExtra("obj", new ConversationNotificationObject(
				Conversation.TYPE_CONTACT, u.getmUserId()));
		i.addCategory(PublicIntent.DEFAULT_CATEGORY);
		i.putExtra("selectedFile", selectedFile);
		mContext.startActivity(i);
		finish();
	}

	private View.OnClickListener mVideoCallButtonListener = new View.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			startVideoCall();
		}
	};

	private View.OnClickListener mSendSmsMsgListener = new View.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			Intent smsIntent = new Intent(Intent.ACTION_VIEW);
			if (u.getCellPhone() != null && !u.getCellPhone().isEmpty()) {
				smsIntent.putExtra("address", u.getCellPhone());
			}else {
				smsIntent.putExtra("address", "");
			}

			smsIntent.setType("vnd.android-dir/mms-sms");
			startActivity(smsIntent);
		}
	};

	private View.OnClickListener mfileSelectionButtonListener = new View.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			if(SPUtil.checkCurrentAviNetwork(mContext)){
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("*/*");
				intent.addCategory(Intent.CATEGORY_OPENABLE);

				try {
					startActivityForResult(
							Intent.createChooser(intent, "Select a File to Upload"),
							FILE_SELECT_CODE);
				} catch (android.content.ActivityNotFoundException ex) {
					Toast.makeText(mContext, "Please install a File Manager.",
							Toast.LENGTH_SHORT).show();
				}
			}
			else{
				
				Toast.makeText(mContext, "当前网络不可用，请稍候再试。", Toast.LENGTH_SHORT).show();
			}
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
			for (View v : mETArr) {
				if (v == null) {
					continue;
				}
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
			}
			onBackPressed();
		}

	};

	private OnClickListener mMoreDetailListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			Intent i = new Intent();
			i.setClass(mContext, ContactDetail2.class);
			i.putExtra("uid", mUid);
			mContext.startActivity(i);
		}

	};

	private View.OnClickListener datePickerListener = new View.OnClickListener() {

		@Override
		public void onClick(final View view) {
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
			Calendar c = Calendar.getInstance();
			c.setTime(bir);
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
							bir = cl.getTime();
							isUpdating = true;
							Message m = Message.obtain(lh, UPDATE_USER_INFO);
							lh.dispatchMessage(m);

						}
					}, c.get(Calendar.YEAR), c.get(Calendar.MONTH),
					c.get(Calendar.DAY_OF_MONTH)).show();
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
			u.setSignature(mSignature.getText().toString());
			u.setGender(getRadioValue());
			u.setBirthday(bir);
			u.setCellPhone(mCellphoneET.getText().toString());
			u.setTelephone(mTelephoneET.getText().toString());
			u.setTitle(mTitleET.getText().toString());
			u.setAddress(mAddressET.getText().toString());
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
