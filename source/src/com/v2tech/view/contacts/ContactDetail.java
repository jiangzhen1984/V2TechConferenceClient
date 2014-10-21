package com.v2tech.view.contacts;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.v2tech.R;
import com.v2tech.service.BitmapManager;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.Registrant;
import com.v2tech.service.UserService;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.SPUtil;
import com.v2tech.view.JNIService;
import com.v2tech.view.PublicIntent;
import com.v2tech.view.bo.ConversationNotificationObject;
import com.v2tech.view.contacts.add.AuthenticationActivity;
import com.v2tech.view.contacts.add.FriendManagementActivity;
import com.v2tech.view.conversation.ConversationSelectFileEntry;
import com.v2tech.vo.Conversation;
import com.v2tech.vo.User;
import com.v2tech.vo.UserDeviceConfig;

public class ContactDetail extends Activity implements OnTouchListener {
	// R.layout.activity_contact_detail
	private static final int UPDATE_USER_INFO = 2;
	private static final int UPDATE_USER_INFO_DONE = 3;
	private static final int FILE_SELECT_CODE = 100;

	private View mReturnButtonTV;
	private TextView mNameTitleIV;
	private ImageView mHeadIconIV;

	// view definition for non-self
	// R.id.authentication_message_layout
	private LinearLayout llAuthenticationMessageLayout;
	// R.id.authentication_message
	private TextView tvAuthenticationMessage;
	// R.id.access
	private Button bAccess;
	// R.id.refuse
	private Button bRefuse;
	// R.id.authentication_state
	private TextView tvAuthenticationState;
	private TextView mCellphoneTV;
	private TextView mTelephoneTV;
	private TextView mTitleTV;
	private TextView mAddressTV;
	private TextView mFaxTV;
	private TextView mEmailTV;
	// R.id.tv_title
	private TextView tvTitle;
	// R.id.contact_user_company
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
	// end view definition for non-self

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
	// end view for self

	private Context mContext;
	private long mUid;
	private User u;
	private LocalHandler lh = new LocalHandler();
	private UserService us = new UserService();
	private int state = -1;
	private String fromActivity;
	private boolean isUpdating;
	private boolean isPad;
	private Date bir;
	private String fromPlace;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_contact_detail);
		initView();
		connectView();
		bindViewEnvent();

		Configuration configuration = getResources().getConfiguration();
		if (configuration.smallestScreenWidthDp > 600) {
			isPad = true;
		}
		fromActivity = this.getIntent().getStringExtra("fromActivity");
		if ((fromActivity != null)
				&& (fromActivity.equals("MessageAuthenticationActivity"))) {
			mUid = this.getIntent().getLongExtra("remoteUserID", 0);
			state = this.getIntent().getIntExtra("state", -1);
			if (state == 4) {
				tvTitle.setText("个人资料");
			} else if (state == 6) {
				tvTitle.setText("验证通知");
			} else {
				tvTitle.setText("好友申请");
			}
		} else {
			mUid = this.getIntent().getLongExtra("uid", 0);
			tvTitle.setText("个人资料");
		}

		fromPlace = this.getIntent().getStringExtra("fromPlace");
		mContext = this;
		View v = findViewById(R.id.contact_detail_main_layout);
		v.setOnTouchListener(this);
		v = findViewById(R.id.contact_detail_scroll_view);
		if (v != null) {
			v.setOnTouchListener(this);
		}

		this.overridePendingTransition(R.animator.alpha_from_0_to_1,
				R.animator.alpha_from_1_to_0);
		initViewShow();

		initBroadcastReceiver();
		
		BitmapManager.getInstance().registerBitmapChangedListener(mAvatarChangedListener);
	}

	@Override
	protected void onDestroy() {
		uninitBroadcastReceiver();
		super.onDestroy();
		BitmapManager.getInstance().unRegisterLastBitmapChangedListener(mAvatarChangedListener);
	}

	private void connectView() {
		tvAuthenticationMessage = (TextView) findViewById(R.id.authentication_message);
		bAccess = (Button) findViewById(R.id.access);
		bRefuse = (Button) findViewById(R.id.refuse);
		tvAuthenticationState = (TextView) findViewById(R.id.authentication_state);
		llAuthenticationMessageLayout = (LinearLayout) findViewById(R.id.authentication_message_layout);
		tvTitle = (TextView) findViewById(R.id.tv_title);
	}

	private void bindViewEnvent() {
		bAccess.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(ContactDetail.this,
						FriendManagementActivity.class);
				intent.putExtra("remoteUserID", ContactDetail.this.getIntent()
						.getLongExtra("remoteUserID", 0));
				intent.putExtra("cause", "access_friend_authentication");
				ContactDetail.this.startActivity(intent);
			}
		});

		bRefuse.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(ContactDetail.this,
						AuthenticationActivity.class);
				intent.putExtra("remoteUserID", ContactDetail.this.getIntent()
						.getLongExtra("remoteUserID", 0));
				intent.putExtra("cause", "refuse_friend_authentication");
				ContactDetail.this.startActivity(intent);

			}
		});
	}

	private void initViewShow() {
		if ((fromActivity != null)
				&& (fromActivity.equals("MessageAuthenticationActivity"))) {
			mMoreDetailButton.setVisibility(View.INVISIBLE);
			llAuthenticationMessageLayout.setVisibility(View.VISIBLE);
			tvAuthenticationMessage.setText(this.getIntent().getStringExtra(
					"authenticationMessage"));
			mCompanyTitleTV.setVisibility(View.INVISIBLE);
			int state = this.getIntent().getIntExtra("state", -1);
			switch (state) {
			// 别人加我：允许任何人：0已添加您为好友，需要验证：1未处理，2已同意，3已拒绝
			// 我加别人：允许认识人：4你们已成为了好友，需要验证：5等待对方验证，4被同意（你们已成为了好友），6拒绝了你为好友
			case 0:// 0成为好友
				bAccess.setVisibility(View.GONE);
				bRefuse.setVisibility(View.GONE);
				tvAuthenticationState.setVisibility(View.GONE);
				break;
			case 1:// 未处理
				bAccess.setVisibility(View.VISIBLE);
				bRefuse.setVisibility(View.VISIBLE);
				tvAuthenticationState.setVisibility(View.GONE);
				break;
			case 2:// 已同意
				bAccess.setVisibility(View.GONE);
				bRefuse.setVisibility(View.GONE);
				tvAuthenticationState.setVisibility(View.VISIBLE);
				tvAuthenticationState.setText("已同意该申请");

				break;
			case 3:// 3已拒绝
				bAccess.setVisibility(View.GONE);
				bRefuse.setVisibility(View.GONE);
				tvAuthenticationState.setVisibility(View.VISIBLE);
				tvAuthenticationState.setText("已拒绝该申请");
				break;
			case 4:// 5被同意
				bAccess.setVisibility(View.GONE);
				bRefuse.setVisibility(View.GONE);
				tvAuthenticationState.setVisibility(View.VISIBLE);
				tvAuthenticationState.setVisibility(View.GONE);
				break;
			case 6:// 6被拒绝
				bAccess.setVisibility(View.GONE);
				bRefuse.setVisibility(View.GONE);
				tvAuthenticationState.setVisibility(View.VISIBLE);
				tvAuthenticationState.setText("拒绝你的好友申请");
				break;
			}

		} else {
			bAccess.setVisibility(View.GONE);
			bRefuse.setVisibility(View.GONE);
			mCompanyTitleTV.setVisibility(View.VISIBLE);
			mMoreDetailButton.setVisibility(View.VISIBLE);
			llAuthenticationMessageLayout.setVisibility(View.GONE);
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
		// case FILE_SELECT_CODE:
		// if (resultCode == RESULT_OK) {
		// // Get the Uri of the selected file
		// Uri uri = data.getData();
		// String path = SPUtil.getPath(this, uri);
		// if (path == null) {
		// Toast.makeText(
		// mContext,
		// R.string.contacts_user_detail_file_selection_not_found_path,
		// Toast.LENGTH_SHORT).show();
		// }
		// else{
		// //return current conversationView with selected file
		// returnConversationView(path);
		// }
		// }
		// break;
		case FILE_SELECT_CODE:
			if (data != null) {

				ArrayList<Parcelable> mCheckedList = data
						.getParcelableArrayListExtra("checkedFiles");

				if (mCheckedList != null && mCheckedList.size() > 0) {
					if (GlobalConfig.isConversationOpen == true) {
						Intent intent = new Intent();
						intent.putParcelableArrayListExtra("checkedFiles",
								mCheckedList);
						setResult(1000, intent);
					} else {
						returnConversationView(mCheckedList);
					}
					finish();
				}
			}
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
			selectedRG(u.getSex());
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
			mCellphoneET.setText(u.getMobile());
			mTelephoneET.setText(u.getTelephone());
			mTitleET.setText(u.getJob());
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
				if ((tv == mMoreDetailButton) || (tv == mCompanyTitleTV)) {
					if ((fromActivity != null)
							&& fromActivity
									.equals("MessageAuthenticationActivity")) {

						int state = this.getIntent().getIntExtra("state", -1);
						switch (state) {
						// 允许时候人时，0成为好友。需要验证时，被邀请，1未处理，2已同意，3已拒绝。邀请人，4等待验证，5被同意，6被拒绝。
						case 0:// 0成为好友
						case 5:// 5被同意
							tv.setVisibility(View.VISIBLE);
							break;
						case 1:// 未处理
						case 2:// 已同意
						case 3:// 3已拒绝
						case 6:// 6被拒绝
							tv.setVisibility(View.GONE);
							break;
						}
						continue;
					}
				}
				tv.setVisibility(View.VISIBLE);
			}
			mSelfItemsContainer.setVisibility(View.GONE);
			mGenderRG.setVisibility(View.GONE);

			mCellphoneTV.setText(u.getMobile());
			mTelephoneTV.setText(u.getTelephone());
			mTitleTV.setText(u.getJob());
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
		// Update global audio state
		GlobalHolder.getInstance().setAudioState(true, mUid);
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
		// Update global video state
		GlobalHolder.getInstance().setVideoState(true, mUid);
		Intent iv = new Intent();
		iv.addCategory(PublicIntent.DEFAULT_CATEGORY);
		iv.setAction(PublicIntent.START_P2P_CONVERSACTION_ACTIVITY);
		iv.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		iv.putExtra("uid", mUid);
		iv.putExtra("is_coming_call", false);
		iv.putExtra("voice", false);
		UserDeviceConfig udc = GlobalHolder.getInstance().getUserDefaultDevice(
				mUid);
		if (udc != null) {
			iv.putExtra("device", udc.getDeviceID());
		} else {
			iv.putExtra("device", "");
		}

		mContext.startActivity(iv);
	}

	Dialog d = null;

	private void showCallDialog(String tel, String phone, boolean voice) {

		d = new Dialog(mContext, R.style.ContactUserDetailVoiceCallDialog);
		d.setCancelable(true);
		d.setCanceledOnTouchOutside(true);
		d.setContentView(R.layout.contacts_user_detail_call_dialog_window);
		TextView tv = (TextView) d
				.findViewById(R.id.contact_user_detail_call_dialog_1);
		tv.setOnClickListener(itemClickListener);
		TextView tv1 = (TextView) d
				.findViewById(R.id.contact_user_detail_call_dialog_2);
		tv1.setOnClickListener(itemClickListener);

		if (!TextUtils.isEmpty(tel) && !TextUtils.isEmpty(phone)) {
			tv.setTag("call:" + tel + "|" + phone);
		} else if (!TextUtils.isEmpty(tel) || !TextUtils.isEmpty(phone)) {
			tv.setTag(TextUtils.isEmpty(tel) == true ? phone : tel);
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

					if (tv1.getVisibility() == View.GONE) {
						tv1.setVisibility(View.VISIBLE);
					}
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
	
	
	private BitmapManager.BitmapChangedListener mAvatarChangedListener = new BitmapManager.BitmapChangedListener() {

		@Override
		public void notifyAvatarChanged(User user, Bitmap bm) {
			if (user.getmUserId() == u.getmUserId()) {
				mHeadIconIV.setImageBitmap(bm);
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
			boolean mobileEmpty = u.getMobile() == null
					|| u.getMobile().isEmpty();
			boolean offline = (u.getmStatus() == User.Status.OFFLINE || u
					.getmStatus() == User.Status.HIDDEN);
			boolean localOffLine = !SPUtil.checkCurrentAviNetwork(mContext);

			if (offline || localOffLine) {
				if (isPad) {
					Toast.makeText(getApplicationContext(), "对方目前不在线，无法语音通话",
							Toast.LENGTH_SHORT).show();
				} else {
					if (phoneEmpty && mobileEmpty) {
						Intent intent = new Intent();
						intent.setAction(Intent.ACTION_DIAL); // android.intent.action.DIAL
						startActivity(intent);
					} else if (!phoneEmpty && !mobileEmpty) {
						// showCallDialog(u.getTelephone(), u.getCellPhone(),
						// false);
						// get telephone numbers and mobile numbers
						String telephone = u.getTelephone();
						String cellPhone = u.getMobile();
						d = new Dialog(mContext,
								R.style.ContactUserDetailVoiceCallDialog);
						d.setCancelable(true);
						d.setCanceledOnTouchOutside(true);
						d.setContentView(R.layout.contacts_user_detail_call_dialog_window);
						// create two TextView View to show them
						// telephone view
						TextView tv = (TextView) d
								.findViewById(R.id.contact_user_detail_call_dialog_1);
						tv.setOnClickListener(itemClickListener);
						tv.setText(telephone);
						tv.setTag(telephone);
						// mobile phone view
						TextView tv1 = (TextView) d
								.findViewById(R.id.contact_user_detail_call_dialog_2);
						tv1.setOnClickListener(itemClickListener);
						tv1.setText(cellPhone);
						tv1.setTag(cellPhone);

						if (tv1.getVisibility() == View.GONE) {
							tv1.setVisibility(View.VISIBLE);
						}
						d.show();
					} else {
						Intent intent = new Intent();
						intent.setAction(Intent.ACTION_CALL);
						if (!phoneEmpty) {
							intent.setData(Uri.parse("tel:" + u.getTelephone()));
						} else {
							intent.setData(Uri.parse("tel:" + u.getMobile()));
						}
						startActivity(intent);
					}
				}
			} else {
				if (isPad) {
					startVoiceCall();
				} else {
					if (phoneEmpty && mobileEmpty) {
						showCallDialog(null, null, true);
					} else if (!phoneEmpty || !mobileEmpty) {
						showCallDialog(u.getTelephone(), u.getMobile(), true);
					} else {
						showCallDialog(u.getTelephone(), u.getMobile(), true);
					}
				}
			}
		}

	};

	private View.OnClickListener mSendMsgListener = new View.OnClickListener() {

		@Override
		public void onClick(View arg0) {

			if ("ConversationView".equals(fromPlace)) {
				setResult(0);
				finish();
			} else
				returnConversationView(null);
		}
	};

	/**
	 * return ConversationView with infos
	 */
	private void returnConversationView(ArrayList<Parcelable> mCheckedList) {
		Intent i = new Intent(PublicIntent.START_CONVERSACTION_ACTIVITY);
		i.putExtra("obj", new ConversationNotificationObject(
				Conversation.TYPE_CONTACT, u.getmUserId()));
		i.addCategory(PublicIntent.DEFAULT_CATEGORY);
		i.putParcelableArrayListExtra("checkedFiles", mCheckedList);
		mContext.startActivity(i);
	}

	private View.OnClickListener mVideoCallButtonListener = new View.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			if (!SPUtil.checkCurrentAviNetwork(mContext)) {
				Toast.makeText(mContext,
						R.string.conversation_no_network_notification,
						Toast.LENGTH_SHORT).show();
				return;
			}
			startVideoCall();
		}
	};

	private View.OnClickListener mSendSmsMsgListener = new View.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			Intent smsIntent = new Intent(Intent.ACTION_VIEW);
			if (u.getMobile() != null && !u.getMobile().isEmpty()) {
				smsIntent.putExtra("address", u.getMobile());
			} else {
				smsIntent.putExtra("address", "");
			}

			smsIntent.setType("vnd.android-dir/mms-sms");
			startActivity(smsIntent);
		}
	};

	private View.OnClickListener mfileSelectionButtonListener = new View.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			if (SPUtil.checkCurrentAviNetwork(mContext)) {
				Intent intent = new Intent(ContactDetail.this,
						ConversationSelectFileEntry.class);
				startActivityForResult(intent, FILE_SELECT_CODE);
			} else {

				Toast.makeText(mContext, "当前网络不可用，请稍候再试。", Toast.LENGTH_SHORT)
						.show();
			}
		}
	};

	private View.OnClickListener mCreateConfMsgListener = new View.OnClickListener() {

		@Override
		public void onClick(View arg0) {
			// FIXME fix bug for enter conference and refresh group list
			Intent i = new Intent(PublicIntent.START_CONFERENCE_CREATE_ACTIVITY);
			i.addCategory(PublicIntent.DEFAULT_CATEGORY);
			i.putExtra("uid", u.getmUserId());
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
			u.setSex(getRadioValue());
			u.setBirthday(bir);
			u.setMobile(mCellphoneET.getText().toString());
			u.setTelephone(mTelephoneET.getText().toString());
			u.setJob(mTitleET.getText().toString());
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

	private boolean isNeedDefault;

	private void selectedRG(String genderVal) {
		for (int i = 0; i < mGenderRG.getChildCount(); i++) {
			RadioButton rg = (RadioButton) mGenderRG.getChildAt(i);
			if (rg.getTag().equals(genderVal)) {
				rg.setChecked(true);
				isNeedDefault = true;
			}
		}

		if (!isNeedDefault) {
			RadioButton radioButton = (RadioButton) findViewById(R.id.radio2);
			radioButton.setChecked(true);
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

	private void initBroadcastReceiver() {
		if (myBroadcastReceiver == null) {
			myBroadcastReceiver = new MyBroadcastReceiver();
			IntentFilter intentFilter = new IntentFilter();
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_USER_UPDATE_NAME_OR_SIGNATURE);
			intentFilter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
			registerReceiver(myBroadcastReceiver, intentFilter);
		}

	}

	private void uninitBroadcastReceiver() {
		if (myBroadcastReceiver != null) {
			unregisterReceiver(myBroadcastReceiver);
			myBroadcastReceiver = null;
		}
	}

	private MyBroadcastReceiver myBroadcastReceiver;

	class MyBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context arg0, Intent arg1) {

			if (arg1.getAction().equals(
					JNIService.JNI_BROADCAST_USER_UPDATE_NAME_OR_SIGNATURE)) {
				long uid = arg1.getLongExtra("uid", -1);
				if (uid == -1) {
					return;
				}
				if (uid == u.getmUserId()) {
					showUserInfo();
				}

			}

		}

	}
}
