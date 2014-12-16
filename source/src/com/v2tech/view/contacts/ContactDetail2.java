package com.v2tech.view.contacts;

import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.V2.jni.V2GlobalEnum;
import com.v2tech.R;
import com.v2tech.service.BitmapManager;
import com.v2tech.service.ContactsService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.MessageListener;
import com.v2tech.service.UserService;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.util.ProgressUtils;
import com.v2tech.view.JNIService;
import com.v2tech.view.MainActivity;
import com.v2tech.view.MainApplication;
import com.v2tech.view.PublicIntent;
import com.v2tech.view.bo.ConversationNotificationObject;
import com.v2tech.view.bo.GroupUserObject;
import com.v2tech.view.contacts.add.AuthenticationActivity;
import com.v2tech.view.contacts.add.FriendManagementActivity;
import com.v2tech.view.widget.MarqueeTextView;
import com.v2tech.vo.ContactGroup;
import com.v2tech.vo.Conversation;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.User;

public class ContactDetail2 extends Activity implements OnTouchListener {

	private static final int UPDATE_USER_INFO = 2;
	private static final int UPDATE_USER_INFO_DONE = 3;
	private static final int DELETE_CONTACT_USER = 4;

	private static final int ORG_SAME_CONTACT = 6;
	private static final int ORG_NO_SAME_CONTACT = 7;

	private static final int REQUEST_UPDATE_GROUP_CODE = 100;

	private Context mContext;

	private long mUid;
	private User u;
	private LocalHandler lh = new LocalHandler();
	private UserService us = new UserService();
	private ContactsService contactService = new ContactsService();

	// R.id.contact_detail_main_layout
	private View contactDetailMainLayout;

	private TextView mNameTitleIV;
	private ImageView mHeadIconIV;

	// view definition for non-self
	private TextView mAccountTV;
	private TextView mGendarTV;
	private TextView mBirthdayTV;
	private TextView mCellphoneTV;
	private TextView mTelephoneTV;
	private TextView mPostJobTV;
	private TextView mAddressTV;
	private MarqueeTextView mSignTV;
	private TextView mDeptTV;
	private TextView mCompanyTV;
	private LinearLayout mSignTVLayout;
	private LinearLayout mSignTVLine;

	private View mCompanyLayout;
	private View mDeptLayout;
	private View mPostJobLayout;
	private View mCompanyLayoutDevider;
	private View mDeptLayoutDevider;
	private View mPostJobLayoutDevider;

	private EditText mNickNameET;
	private TextView mGroupNameTV;
	private TextView mAddContactButton;
	private View mUpdateContactGroupButton;
	private View mDeleteContactButton;

	private boolean isUpdating;
	private User currentUser;
	private boolean isRelation;
	private Group belongs;

	private Resources res;
	private int currentContactType;
	private String fromActivity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_contact_detail_2);
		contactDetailMainLayout = findViewById(R.id.contact_detail_main_layout);
		contactDetailMainLayout.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				hindSoftInput(v);
				return false;
			}
		});

		// 不同页面跳转过来
		fromActivity = this.getIntent().getStringExtra("fromActivity");
		if ((fromActivity != null)
				&& (fromActivity.equals("MessageAuthenticationActivity"))) {
			mUid = this.getIntent().getLongExtra("remoteUserID", 0);
			currentContactType = ORG_SAME_CONTACT;
		} else if ((fromActivity != null)
				&& (fromActivity.equals("SearchedResultActivity"))) {
			mUid = this.getIntent().getLongExtra("uid", 0);
			currentContactType = ORG_NO_SAME_CONTACT;
		} else if ((fromActivity != null)
				&& fromActivity
						.equals("MessageAuthenticationActivity-ContactDetail")) {
			mUid = this.getIntent().getLongExtra("uid", 0);
			currentContactType = ORG_SAME_CONTACT;
		} else {
			mUid = this.getIntent().getLongExtra("uid", 0);
			currentContactType = ORG_SAME_CONTACT;
		}

		initView();
		connectView();
		bindViewEnvent();

		mContext = this;
		u = GlobalHolder.getInstance().getUser(mUid);
		res = getResources();

		mNickNameET = (EditText) findViewById(R.id.contact_user_detail_nick_name_et);

		mUpdateContactGroupButton = findViewById(R.id.contact_detail_contact_group_item_ly);
		mUpdateContactGroupButton
				.setOnClickListener(mUpdateContactGroupButtonListener);

		this.overridePendingTransition(R.animator.alpha_from_0_to_1,
				R.animator.alpha_from_1_to_0);

		currentUser = GlobalHolder.getInstance().getCurrentUser();
		List<Group> friendGroup = GlobalHolder.getInstance().getGroup(
				GroupType.CONTACT.intValue());
		for (Group group : friendGroup) {
			if ((belongs = group.findUser(u)) != null) {
				isRelation = true;
				break;
			}
		}

		updateContactGroup();

		BitmapManager.getInstance().registerBitmapChangedListener(
				mAvatarChangedListener);

		initBroadcastReceiver();
	}

	private void connectView() {

	}

	private void bindViewEnvent() {

	}

	@Override
	protected void onDestroy() {
		uninitBroadcastReceiver();
		contactService.clearCalledBack();
		super.onDestroy();
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (u != null) {
			showUserInfo();
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		String receiveNickName = intent.getStringExtra("nickName");
		if (u != null)
			u.setNickName(receiveNickName);
	}

	@Override
	protected void onStop() {
		super.onStop();
//		mNickNameET.removeTextChangedListener(tw);
	}

	@Override
	public void onBackPressed() {
		Message m = Message.obtain(lh, UPDATE_USER_INFO);
		lh.sendMessage(m);
		super.onBackPressed();
	}

	@Override
	public void finish() {
		super.finish();
		this.overridePendingTransition(R.animator.alpha_from_0_to_1,
				R.animator.alpha_from_1_to_0);
	}

	private void hindSoftInput(View v) {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null && v != null) {
			imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
		}
	}

	@Override
	public boolean onTouch(View view, MotionEvent mv) {
		return super.onTouchEvent(mv);
	}

	private BitmapManager.BitmapChangedListener mAvatarChangedListener = new BitmapManager.BitmapChangedListener() {

		@Override
		public void notifyAvatarChanged(User user, Bitmap bm) {
			if (user.getmUserId() == u.getmUserId()) {
				mHeadIconIV.setImageBitmap(bm);
			}
		}

	};

	private void initView() {

		TextView mTitleContent = (TextView) findViewById(R.id.ws_common_activity_title_content);
		TextView mTitleLeftTV = (TextView) findViewById(R.id.ws_common_activity_title_left_button);
		TextView mTitleRightTV = (TextView) findViewById(R.id.ws_common_activity_title_right_button);

		mTitleRightTV.setVisibility(View.INVISIBLE);
		mTitleContent.setText(R.string.contacts_detail_title);
		mTitleLeftTV.setText(R.string.contacts_user_detail_return_button);
		mTitleLeftTV.setOnClickListener(mReturnButtonListener);

		mHeadIconIV = (ImageView) findViewById(R.id.contact_user_detail_head_icon);
		mNameTitleIV = (TextView) findViewById(R.id.contact_user_detail_title);

		mAddContactButton = (TextView) findViewById(R.id.contact_user_detail_add_friend);

		mAddContactButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// 加为好友
				Intent i = new Intent();
				switch (u.getAuthtype()) {
				case 0:
					i.setClass(ContactDetail2.this,
							FriendManagementActivity.class);
					i.putExtra("uid", mUid);
					i.putExtra("cause", "ContactDetail2");
					ContactDetail2.this.startActivity(i);
					break;
				case 1:
					i.setClass(ContactDetail2.this,
							AuthenticationActivity.class);
					i.putExtra("uid", mUid);
					ContactDetail2.this.startActivity(i);
					break;
				case 2:
					Toast.makeText(ContactDetail2.this, "对方不允许加为好友",
							Toast.LENGTH_SHORT).show();
					break;
				default:
					Toast.makeText(ContactDetail2.this, "对方不允许加为好友",
							Toast.LENGTH_SHORT).show();
					break;
				}

			}
		});

		mDeleteContactButton = findViewById(R.id.contact_user_detail_delete_friend);

		mDeleteContactButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				if (((MainApplication) getApplication()).netWordIsConnected) {
					showDeleteContactDialog();
				} else {
					Toast.makeText(ContactDetail2.this, "无法连接到服务器，请确认网络连接",
							Toast.LENGTH_SHORT).show();
				}
			}
		});

		mSignTV = (MarqueeTextView) findViewById(R.id.contact_user_detail_user_signature_tv);
		mSignTVLayout = (LinearLayout) findViewById(R.id.contact_user_detail_nick_name_et_linearlayout);
		mSignTVLine = (LinearLayout) findViewById(R.id.contact_user_detail_nick_name_et_belowline);

		mAccountTV = (TextView) findViewById(R.id.contact_user_detail_account_tv);
		mGendarTV = (TextView) findViewById(R.id.contact_user_detail_gender_tv);
		mBirthdayTV = (TextView) findViewById(R.id.contact_user_detail_birthday_tv);
		mCellphoneTV = (TextView) findViewById(R.id.contact_user_detail_cell_phone_tv);
		mTelephoneTV = (TextView) findViewById(R.id.contact_user_detail_telephone_tv);
		mCompanyTV = (TextView) findViewById(R.id.contact_user_detail_company_tv);
		mDeptTV = (TextView) findViewById(R.id.contact_user_detail_department_tv);
		mPostJobTV = (TextView) findViewById(R.id.contact_user_detail_title_tv);
		mAddressTV = (TextView) findViewById(R.id.contact_user_detail_address_tv);

		mCompanyLayout = findViewById(R.id.contact_user_detail_company_tv_layout);
		mDeptLayout = findViewById(R.id.contact_user_detail_department_tv_layout);
		mPostJobLayout = findViewById(R.id.contact_user_detail_title_tv_layout);
		mCompanyLayoutDevider = findViewById(R.id.contact_user_detail_company_tv_layout_devider);
		mDeptLayoutDevider = findViewById(R.id.contact_user_detail_department_tv_layout_devider);
		mPostJobLayoutDevider = findViewById(R.id.contact_user_detail_title_tv_layout_devider);
	}

	private void updateContactGroup() {
		if (isRelation == true) {
			mUpdateContactGroupButton.setVisibility(View.VISIBLE);
			mGroupNameTV = (TextView) findViewById(R.id.detail_detail_2_group_name);
			mGroupNameTV.setText(belongs.getName());
			mAddContactButton.setVisibility(View.GONE);
			mDeleteContactButton.setVisibility(View.VISIBLE);
			mSignTVLayout.setVisibility(View.VISIBLE);
			mSignTVLine.setVisibility(View.VISIBLE);
		} else {
			mUpdateContactGroupButton.setVisibility(View.GONE);
			mAddContactButton.setVisibility(View.VISIBLE);
			mDeleteContactButton.setVisibility(View.GONE);
			mSignTVLayout.setVisibility(View.GONE);
			mSignTVLine.setVisibility(View.GONE);
		}

		if (currentContactType == ORG_NO_SAME_CONTACT) {
			mCompanyLayout.setVisibility(View.GONE);
			mDeptLayout.setVisibility(View.GONE);
			mPostJobLayout.setVisibility(View.GONE);
			mCompanyLayoutDevider.setVisibility(View.GONE);
			mDeptLayoutDevider.setVisibility(View.GONE);
			mPostJobLayoutDevider.setVisibility(View.GONE);
		} else {
			mCompanyLayout.setVisibility(View.VISIBLE);
			mDeptLayout.setVisibility(View.VISIBLE);
			mPostJobLayout.setVisibility(View.VISIBLE);
			mCompanyLayoutDevider.setVisibility(View.VISIBLE);
			mDeptLayoutDevider.setVisibility(View.VISIBLE);
			mPostJobLayoutDevider.setVisibility(View.VISIBLE);
		}
	}

	private void showDeleteContactDialog() {

		final Dialog deleteContactDialog = new Dialog(ContactDetail2.this,
				R.style.customDialog);
		deleteContactDialog
				.setContentView(R.layout.activity_contact_delete_contact_dialog);
		Button cancelbut = (Button) deleteContactDialog
				.findViewById(R.id.cancelbut);
		Button okbut = (Button) deleteContactDialog.findViewById(R.id.okbut);
		cancelbut.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				deleteContactDialog.dismiss();
			}
		});

		okbut.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// 删除好友
				deleteContactDialog.dismiss();
				contactService.delContact(u, new MessageListener(lh,
						DELETE_CONTACT_USER, null));
				ProgressUtils.showNormalWithHintProgress(mContext, true);
			}
		});

		deleteContactDialog.show();
	}

	private void showUserInfo() {
		if (u.getAvatarBitmap() != null) {
			mHeadIconIV.setImageBitmap(u.getAvatarBitmap());
		}

//		mNickNameET.addTextChangedListener(tw);
		if (!mNickNameET.getText().toString().equals(u.getNickName())) {
			mNickNameET.setText(u.getNickName());
		}

		mNameTitleIV.setText(u.getName());
		mAccountTV.setText(u.getAccount());
		if (u.getSex() != null) {
			if (u.getSex().equals("0")) {
				mGendarTV.setText(mContext
						.getText(R.string.contacts_user_detail_gender_priacy));
			} else if (u.getSex().equals("1")) {
				mGendarTV.setText(mContext
						.getText(R.string.contacts_user_detail_gender_male));
			} else if (u.getSex().equals("2")) {
				mGendarTV.setText(mContext
						.getText(R.string.contacts_user_detail_gender_female));
			}

		} else {
			mGendarTV.setText(mContext
					.getText(R.string.contacts_user_detail_gender_priacy));
		}

		mBirthdayTV.setText(u.getBirthdayStr());
		mCellphoneTV.setText(u.getMobile());
		mTelephoneTV.setText(u.getTelephone());
		mPostJobTV.setText(u.getJob());
		mAddressTV.setText(u.getAddress());
		mSignTV.setText(u.getSignature());
		mDeptTV.setText(u.getDepartment());
		mCompanyTV.setText(u.getCompany());

	}

	private Dialog mDialog = null;

	private void showConfirmDialog() {
		if (mDialog == null) {
			mDialog = new Dialog(mContext);
			mDialog.setContentView(R.layout.contacts_remove_confirmation_dialog);
			Button confirmButton = (Button) mDialog
					.findViewById(R.id.contacts_group_confirm_button);
			confirmButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View view) {
					// input null for remove contact
					contactService.updateUserGroup(null,
							(ContactGroup) belongs, u, null);
					isRelation = false;
					updateContactGroup();
					mDialog.dismiss();
				}

			});
			Button cancelbutton = (Button) mDialog
					.findViewById(R.id.contacts_group_cancel_button);
			cancelbutton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					mDialog.dismiss();
				}

			});
		}

		if (!mDialog.isShowing()) {
			mDialog.show();
		}

	}

//	private TextWatcher tw = new TextWatcher() {
//
//		@Override
//		public void afterTextChanged(Editable ed) {
//			if (isUpdating) {
//				return;
//			}
//			isUpdating = true;
//			Message m = Message.obtain(lh, UPDATE_USER_INFO);
//			lh.sendMessageDelayed(m, 1500);
//		}
//
//		@Override
//		public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
//				int arg3) {
//
//		}
//
//		@Override
//		public void onTextChanged(CharSequence arg0, int arg1, int arg2,
//				int arg3) {
//
//		}
//
//	};

	private View.OnClickListener mReturnButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			onBackPressed();
		}

	};

	private View.OnClickListener mAddOrRemoveContactButton = new OnClickListener() {

		@Override
		public void onClick(View view) {
			if (isRelation) {
				showConfirmDialog();
			} else {
				List<Group> list = GlobalHolder.getInstance().getGroup(
						GroupType.CONTACT.intValue());
				if (list != null && list.size() > 0) {
					contactService.updateUserGroup((ContactGroup) list.get(0),
							null, u, null);
					isRelation = true;
					belongs = (ContactGroup) list.get(0);
				}
			}
			updateContactGroup();
		}

	};

	private View.OnClickListener mUpdateContactGroupButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			Intent i = new Intent();
			i.setClass(mContext, UpdateContactGroupActivity.class);
			i.putExtra("uid", u.getmUserId());
			i.putExtra("gid", belongs.getmGId());
			startActivityForResult(i, REQUEST_UPDATE_GROUP_CODE);
		}

	};

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_UPDATE_GROUP_CODE) {
			if (resultCode == UpdateContactGroupActivity.SELECT_GROUP_RESPONSE_CODE_DONE) {
				if (data != null) {
					String selectGroupName = data.getStringExtra("groupName");
					long selectGroupID = data.getLongExtra("groupID", 0);
					belongs = GlobalHolder.getInstance().getGroupById(
							selectGroupID);
					mGroupNameTV.setText(selectGroupName);

					// 请求刷新联系人界面
					Intent intent = new Intent(
							PublicIntent.BROADCAST_REQUEST_UPDATE_CONTACTS_GROUP);
					ContactDetail2.this.sendBroadcast(intent);
				}
			} else if (resultCode == UpdateContactGroupActivity.SELECT_GROUP_RESPONSE_CODE_CANCEL) {
			}
		}

	};

	private void gatherUserData() {
		u.setNickName(mNickNameET.getText().toString());
	}

	class LocalHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPDATE_USER_INFO:
				gatherUserData();
				us.updateUser(u, new MessageListener(this,
						UPDATE_USER_INFO_DONE, null));
				break;
			case UPDATE_USER_INFO_DONE:
				// if (mContext != null) {
				// Toast.makeText(mContext,
				// R.string.contacts_user_detail_nick_name_updated,
				// Toast.LENGTH_SHORT).show();
				// }
				isUpdating = false;
				break;
			case DELETE_CONTACT_USER:
				ProgressUtils.showNormalWithHintProgress(mContext, false);
				JNIResponse response = (JNIResponse) msg.obj;
				if (response.getResult() == JNIResponse.Result.SUCCESS) {
					// belongs = null;
					// isRelation = false;
					// updateContactGroup();
					if ((fromActivity != null)
							&& (fromActivity
									.equals("MessageAuthenticationActivity"))) {
						belongs = null;
						isRelation = false;
						updateContactGroup();
					} else if ((fromActivity != null)
							&& (fromActivity
									.equals("MessageAuthenticationActivity-ContactDetail"))) {
						belongs = null;
						isRelation = false;
						updateContactGroup();
					} else if ((fromActivity != null)
							&& (fromActivity.equals("SearchedResultActivity"))) {
						belongs = null;
						isRelation = false;
						updateContactGroup();
					} else {
						Intent i = new Intent(ContactDetail2.this,
								MainActivity.class);
						i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						ContactDetail2.this.startActivity(i);
						
						Intent intent = new Intent(PublicIntent.REQUEST_UPDATE_CONVERSATION);
						intent.addCategory(PublicIntent.DEFAULT_CATEGORY);
						ConversationNotificationObject obj = new ConversationNotificationObject(Conversation.TYPE_VERIFICATION_MESSAGE,
									Conversation.SPECIFIC_VERIFICATION_ID , false , true , -1);
						intent.putExtra("obj", obj);
						mContext.sendBroadcast(intent);
					}

				} else if (response.getResult() == JNIResponse.Result.TIME_OUT) {
					Toast.makeText(mContext,
							res.getString(R.string.contacts_delete_net_failed),
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(mContext,
							res.getString(R.string.contacts_delete_failed),
							Toast.LENGTH_SHORT).show();
				}
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
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_CONTACTS_AUTHENTICATION);
			intentFilter.addAction(JNIService.JNI_BROADCAST_GROUP_USER_REMOVED);
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
			String action = arg1.getAction();
			if (action
					.equals(JNIService.JNI_BROADCAST_USER_UPDATE_NAME_OR_SIGNATURE)) {
				long uid = arg1.getLongExtra("uid", -1);
				if (uid == -1) {
					return;
				}
				if (uid == u.getmUserId()) {
					showUserInfo();
				}

			} else if (action
					.equals(JNIService.JNI_BROADCAST_CONTACTS_AUTHENTICATION)) {
				long uid = arg1.getLongExtra("uid", -1);
				if (uid == -1) {
					return;
				}
				if (uid == u.getmUserId()) {

					long gid = arg1.getLongExtra("gid", -1);
					if (gid == -1) {
						return;
					}
					if ((belongs = GlobalHolder.getInstance().getGroupById(
							V2GlobalEnum.GROUP_TYPE_CONTACT, gid)) == null) {
						return;
					}
					isRelation = true;
					updateContactGroup();
					Toast.makeText(ContactDetail2.this, "添加成功",
							Toast.LENGTH_SHORT).show();

					// update user info
					u = GlobalHolder.getInstance().getUser(uid);
				}
			} else if (action
					.equals(JNIService.JNI_BROADCAST_GROUP_USER_REMOVED)) {

				GroupUserObject obj = arg1.getParcelableExtra("obj");
				if (obj.getmUserId() != u.getmUserId()) {
					return;
				}

				if (obj.getmType() == GroupType.CONTACT.intValue()) {
					belongs = null;
					isRelation = false;
					updateContactGroup();
				}
			}
		}

	}

}
