package com.v2tech.view.contacts;

import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.v2tech.R;
import com.v2tech.service.ContactsService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.Registrant;
import com.v2tech.service.UserService;
import com.v2tech.vo.ContactGroup;
import com.v2tech.service.FriendGroupService;
import com.v2tech.view.MainActivity;
import com.v2tech.view.PublicIntent;
import com.v2tech.view.contacts.add.AuthenticationActivity;
import com.v2tech.vo.FriendGroup;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.User;

public class ContactDetail2 extends Activity implements OnTouchListener {

	private static final int UPDATE_USER_INFO = 2;
	private static final int UPDATE_USER_INFO_DONE = 3;
	
	private static final int REQUEST_UPDATE_GROUP_CODE = 100;

	private Context mContext;

	private long mUid;
	private User detailUser;
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
	private TextView mSignTV;
	private TextView mDeptTV;
	private TextView mCompanyTV;
	private TextView mGroupNameTV;
	

	private EditText mNickNameET;

	private TextView mAddContactButton;
	private View mDeleteContactButton;
	private View mUpdateContactGroupButton;

	private boolean isUpdating;
	private User currentUser;
	private boolean isRelation;
	private Group belongs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.activity_contact_detail_2);
		// 不同页面跳转过来
		String fromActivity=this.getIntent().getStringExtra("fromActivity");
		if((fromActivity!=null)&&(fromActivity.equals("MessageAuthenticationActivity"))){
			//166是wenzl测试用
			//mUid = this.getIntent().getLongExtra("remoteUserID", 0);
			mUid=166;
		}else{
			mUid = this.getIntent().getLongExtra("uid", 0);
		}

		initView();
		mContext = this;
		detailUser = GlobalHolder.getInstance().getUser(mUid);

		mNickNameET = (EditText) findViewById(R.id.contact_user_detail_nick_name_et);
		
		mUpdateContactGroupButton = findViewById(R.id.contact_detail_contact_group_item_ly);
		mUpdateContactGroupButton.setOnClickListener(mUpdateContactGroupButtonListener);
		
		this.overridePendingTransition(R.animator.alpha_from_0_to_1,
				R.animator.alpha_from_1_to_0);

		currentUser = GlobalHolder.getInstance().getCurrentUser();
		List<Group> friendGroup = GlobalHolder.getInstance().getGroup(
				GroupType.CONTACT.intValue());
		for (Group group : friendGroup) {
			if (group.findUser(detailUser, group) != null) {
				isRelation = true;
			}
		}

		if (isRelation == true) {
			mAddContactButton.setVisibility(View.GONE);
			mDeleteContactButton.setVisibility(View.VISIBLE);
		} else {
			mAddContactButton.setVisibility(View.VISIBLE);
			mDeleteContactButton.setVisibility(View.GONE);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (detailUser != null) {
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

		mAddContactButton = (TextView)findViewById(R.id.contact_user_detail_add_friend);

		mAddContactButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				// 加为好友
				Intent i = new Intent(ContactDetail2.this,
						AuthenticationActivity.class);
				i.putExtra("uid", mUid);
				ContactDetail2.this.startActivity(i);
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
		mSignTV = (TextView) findViewById(R.id.contact_user_detail_user_signature_tv);
		mDeptTV = (TextView) findViewById(R.id.contact_user_detail_department_tv);
		mCompanyTV = (TextView) findViewById(R.id.contact_user_detail_company_tv);

	}
	
	
	private void updateContactGroup() {
		if (isRelation == true) {
			mAddContactButton.setText(mContext.getText(R.string.contacts_user_detail_delete_friend));
			mUpdateContactGroupButton.setVisibility(View.VISIBLE);
			mGroupNameTV =(TextView)findViewById(R.id.detail_detail_2_group_name);
			mGroupNameTV.setText(belongs.getName());
		} else {
			mAddContactButton.setText(mContext.getText(R.string.contacts_user_detail_add_friend));
			mUpdateContactGroupButton.setVisibility(View.GONE);
		}
		
		mAddContactButton.setOnClickListener(mAddOrRemoveContactButton);
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
				new FriendGroupService().delFriendGroupUser(detailUser);
				// 返回用户列表
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
		if (detailUser.getAvatarBitmap() != null) {
			mHeadIconIV.setImageBitmap(detailUser.getAvatarBitmap());
		}

		mNickNameET.setText(detailUser.getNickName());
		mNickNameET.addTextChangedListener(tw);

		mNameTitleIV.setText(detailUser.getName());
		mAccountTV.setText(detailUser.getAccount());
		if (detailUser.getGender() != null) {
			if (detailUser.getGender().equals("0")) {
				mGendarTV.setText(mContext
						.getText(R.string.contacts_user_detail_gender_priacy));
			} else if (detailUser.getGender().equals("1")) {
				mGendarTV.setText(mContext
						.getText(R.string.contacts_user_detail_gender_male));
			} else if (detailUser.getGender().equals("2")) {
				mGendarTV.setText(mContext
						.getText(R.string.contacts_user_detail_gender_female));
			}

		} else {
			mGendarTV.setText("");
		}

		mBirthdayTV.setText(detailUser.getBirthdayStr());
		mCellphoneTV.setText(detailUser.getCellPhone());
		mTelephoneTV.setText(detailUser.getTelephone());
		mTitleTV.setText(detailUser.getTitle());
		mAddressTV.setText(detailUser.getAddress());
		mSignTV.setText(detailUser.getSignature());
		mDeptTV.setText(detailUser.getDepartment());
		mCompanyTV.setText(detailUser.getCompany());

	}
	
	
	private Dialog  mDialog = null;
	private void showConfirmDialog() {
		if (mDialog == null) {
			mDialog = new Dialog(mContext);
			mDialog.setContentView(R.layout.contacts_remove_confirmation_dialog);
			Button confirmButton = (Button)mDialog.findViewById(R.id.contacts_group_confirm_button);
			confirmButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View view) {
					//input null for remove contact
					contactService.updateUserGroup(null,(ContactGroup)belongs,  detailUser, null);
					isRelation = false;
					updateContactGroup();
					mDialog.dismiss();
				}
				
			});
			Button cancelbutton = (Button)mDialog.findViewById(R.id.contacts_group_cancel_button);
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
				List<Group> list = GlobalHolder.getInstance().getGroup(GroupType.CONTACT.intValue());
				if (list != null && list.size() > 0) {
					contactService.updateUserGroup((ContactGroup)list.get(0), null, detailUser, null);
					isRelation = true;
					belongs = (ContactGroup)list.get(0);
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
			i.putExtra("uid", currentUser.getmUserId());
			i.putExtra("gid", belongs.getmGId());
			startActivityForResult(i, REQUEST_UPDATE_GROUP_CODE);
		}

	};

	private void gatherUserData() {
		detailUser.setNickName(mNickNameET.getText().toString());
	}

	class LocalHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPDATE_USER_INFO:
				gatherUserData();
				us.updateUser(detailUser, new Registrant(this,
						UPDATE_USER_INFO_DONE, null));
				break;
			case UPDATE_USER_INFO_DONE:
				if (mContext != null) {
					Toast.makeText(mContext,
							R.string.contacts_user_detail_nick_name_updated,
							Toast.LENGTH_SHORT).show();
				}
				isUpdating = false;
				break;
			}
		}

	}

}
