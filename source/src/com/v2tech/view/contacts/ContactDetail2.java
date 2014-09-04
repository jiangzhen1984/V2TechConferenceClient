package com.v2tech.view.contacts;

import java.util.List;

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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.v2tech.R;
import com.v2tech.service.ContactsService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.Registrant;
import com.v2tech.service.UserService;
import com.v2tech.vo.ContactGroup;
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
	private TextView mSignTV;
	private TextView mDeptTV;
	private TextView mCompanyTV;
	

	private EditText mNickNameET;
	private TextView mGroupNameTV;
	private TextView mAddContactButton;
	private View mUpdateContactGroupButton;

	private boolean isUpdating;
	private User currentUser;
	private boolean isRelation;
	private Group belongs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.activity_contact_detail_2);
		mUid = this.getIntent().getLongExtra("uid", 0);
		initView();
		mContext = this;
		u = GlobalHolder.getInstance().getUser(mUid);

		mNickNameET = (EditText) findViewById(R.id.contact_user_detail_nick_name_et);
		
		mUpdateContactGroupButton = findViewById(R.id.contact_detail_contact_group_item_ly);
		mUpdateContactGroupButton.setOnClickListener(mUpdateContactGroupButtonListener);
		
		this.overridePendingTransition(R.animator.alpha_from_0_to_1,
				R.animator.alpha_from_1_to_0);

		currentUser = GlobalHolder.getInstance().getCurrentUser();
		List<Group> friendGroup = GlobalHolder.getInstance().getGroup(
				GroupType.CONTACT);
		for (Group group : friendGroup) {
			if ((belongs = group.findUser(u, group)) != null) {
				isRelation = true;
				break;
			}
		}

		updateContactGroup();
		
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

		mAddContactButton = (TextView)findViewById(R.id.contact_user_detail_add_friend);

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

	private void showUserInfo() {
		if (u.getAvatarBitmap() != null) {
			mHeadIconIV.setImageBitmap(u.getAvatarBitmap());
		}

		mNickNameET.setText(u.getNickName());
		mNickNameET.addTextChangedListener(tw);

		mNameTitleIV.setText(u.getName());
		mAccountTV.setText(u.getAccount());
		if (u.getGender() != null) {
			if (u.getGender().equals("0")) {
				mGendarTV.setText(mContext
						.getText(R.string.contacts_user_detail_gender_priacy));
			} else if (u.getGender().equals("1")) {
				mGendarTV.setText(mContext
						.getText(R.string.contacts_user_detail_gender_male));
			} else if (u.getGender().equals("2")) {
				mGendarTV.setText(mContext
						.getText(R.string.contacts_user_detail_gender_female));
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
				contactService.updateUserGroup(null,(ContactGroup)belongs,  u, null);
				isRelation = false;
			} else {
				List<Group> list = GlobalHolder.getInstance().getGroup(GroupType.CONTACT);
				if (list != null && list.size() > 0) {
					contactService.updateUserGroup((ContactGroup)list.get(0), null, u, null);
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
