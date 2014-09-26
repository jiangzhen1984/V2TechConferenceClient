package com.v2tech.view.contacts;

import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.V2.jni.V2GlobalEnum;
import com.v2tech.R;
import com.v2tech.service.ContactsService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.Registrant;
import com.v2tech.service.UserService;
import com.v2tech.vo.ContactGroup;
import com.v2tech.view.JNIService;
import com.v2tech.view.MainActivity;
import com.v2tech.view.contacts.add.AuthenticationActivity;
import com.v2tech.view.contacts.add.FriendManagementActivity;
import com.v2tech.view.widget.MarqueeTextView;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.User;

public class ContactDetail2 extends Activity implements OnTouchListener {

	private static final int UPDATE_USER_INFO = 2;
	private static final int UPDATE_USER_INFO_DONE = 3;

	private static final int REQUEST_UPDATE_GROUP_CODE = 100;

	private Context mContext;

	private long mUid;
	private User u;
	private LocalHandler lh = new LocalHandler();
	private UserService us = new UserService();
	private ContactsService contactService = new ContactsService();

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
	private MarqueeTextView mSignTV;
	private TextView mDeptTV;
	private TextView mCompanyTV;

	private EditText mNickNameET;
	private TextView mGroupNameTV;
	private TextView mAddContactButton;
	private View mUpdateContactGroupButton;
	private View mDeleteContactButton;

	private boolean isUpdating;
	private User currentUser;
	private boolean isRelation;
	private Group belongs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.activity_contact_detail_2);
		// 不同页面跳转过来
		String fromActivity = this.getIntent().getStringExtra("fromActivity");
		if ((fromActivity != null)
				&& (fromActivity.equals("MessageAuthenticationActivity"))) {
			mUid = this.getIntent().getLongExtra("remoteUserID", 0);
		} else {
			mUid = this.getIntent().getLongExtra("uid", 0);
		}

		initView();
		connectView();
		bindViewEnvent();

		mContext = this;
		u = GlobalHolder.getInstance().getUser(mUid);

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
			if ((belongs = group.findUser(u, group)) != null) {
				isRelation = true;
				break;
			}
		}
		updateContactGroup();
		initBroadcastReceiver();
	}

	private void connectView() {

	}

	private void bindViewEnvent() {

	}

	@Override
	protected void onDestroy() {
		uninitBroadcastReceiver();
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
	protected void onStop() {
		super.onStop();
		mNickNameET.removeTextChangedListener(tw);
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
				showDeleteContactDialog();
			}
		});

		mAccountTV = (TextView) findViewById(R.id.contact_user_detail_account_tv);
		mGendarTV = (TextView) findViewById(R.id.contact_user_detail_gender_tv);
		mBirthdayTV = (TextView) findViewById(R.id.contact_user_detail_birthday_tv);

		mTitleTV = (TextView) findViewById(R.id.contact_user_detail_title_tv);
		mAddressTV = (TextView) findViewById(R.id.contact_user_detail_address_tv);
		mCellphoneTV = (TextView) findViewById(R.id.contact_user_detail_cell_phone_tv);
		mTelephoneTV = (TextView) findViewById(R.id.contact_user_detail_telephone_tv);
		mSignTV = (MarqueeTextView) findViewById(R.id.contact_user_detail_user_signature_tv);
		mDeptTV = (TextView) findViewById(R.id.contact_user_detail_department_tv);
		mCompanyTV = (TextView) findViewById(R.id.contact_user_detail_company_tv);

	}

	private void updateContactGroup() {
		if (isRelation == true) {
			mUpdateContactGroupButton.setVisibility(View.VISIBLE);
			mGroupNameTV = (TextView) findViewById(R.id.detail_detail_2_group_name);
			mGroupNameTV.setText(belongs.getName());
			mAddContactButton.setVisibility(View.GONE);
			mDeleteContactButton.setVisibility(View.VISIBLE);
		} else {
			mUpdateContactGroupButton.setVisibility(View.GONE);
			mAddContactButton.setVisibility(View.VISIBLE);
			mDeleteContactButton.setVisibility(View.GONE);
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
				// TODO Auto-generated method stub
				deleteContactDialog.dismiss();
			}
		});

		okbut.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// 删除好友
				deleteContactDialog.dismiss();
				contactService.delContact(u);
				Intent i = new Intent(ContactDetail2.this, MainActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				ContactDetail2.this.startActivity(i);
			}
		});

		deleteContactDialog.show();

		// ListView lv = (ListView)
		// fileTransportDialog.findViewById(R.id.file_down_list);
		// lv.setAdapter(fileDownAdapter);
	}

	private void showUserInfo() {
		if (u.getAvatarBitmap() != null) {
			mHeadIconIV.setImageBitmap(u.getAvatarBitmap());
		}

		mNickNameET.addTextChangedListener(tw);
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
			mGendarTV.setText("");
		}

		mBirthdayTV.setText(u.getBirthdayStr());
		mCellphoneTV.setText(u.getMobile());
		mTelephoneTV.setText(u.getTelephone());
		mTitleTV.setText(u.getJob());
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

	private TextWatcher tw = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable ed) {
			if (isUpdating) {
				return;
			}
			isUpdating = true;
			Message m = Message.obtain(lh, UPDATE_USER_INFO);
			lh.sendMessageDelayed(m, 1500);
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
					// selectGroupName = data.getStringExtra("groupName");
					// selectGroupID = data.getLongExtra("groupID", 0);
					// tvGroupName.setText(selectGroupName);
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
				us.updateUser(u, new Registrant(this, UPDATE_USER_INFO_DONE,
						null));
				break;
			case UPDATE_USER_INFO_DONE:
				// if (mContext != null) {
				// Toast.makeText(mContext,
				// R.string.contacts_user_detail_nick_name_updated,
				// Toast.LENGTH_SHORT).show();
				// }
				isUpdating = false;
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
			intentFilter.addAction(JNIService.JNI_BROADCAST_FRIEND_ADDED);
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

			} else if (action.equals(JNIService.JNI_BROADCAST_FRIEND_ADDED)) {
				long uid = arg1.getLongExtra("uid", -1);
				if (uid == -1) {
					return;
				}

				if (uid == u.getmUserId()) {
					isRelation = true;
					long gid = arg1.getLongExtra("gid", -1);
					if (gid == -1) {
						return;
					}
					if ((belongs = GlobalHolder.getInstance().getGroupById(
							V2GlobalEnum.GROUP_TYPE_CONTACT, gid)) == null) {
						return;
					}
					updateContactGroup();
					Toast.makeText(ContactDetail2.this, "添加成功",
							Toast.LENGTH_SHORT).show();
				}

			}

		}

	}

}
