package com.bizcom.vc.activity.contacts;

import java.util.List;

import com.bizcom.request.ContactsService;
import com.bizcom.vc.activity.ConversationsTabFragment;
import com.bizcom.vc.activity.main.MainActivity;
import com.bizcom.vc.activity.message.MessageAuthenticationActivity;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.application.MainApplication;
import com.bizcom.vc.application.PublicIntent;
import com.bizcom.vo.FriendGroup;
import com.bizcom.vo.Group;
import com.bizcom.vo.User;
import com.bizcom.vo.Group.GroupType;
import com.v2tech.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class InputRemarkActivity extends Activity {
	private static final int SELECT_GROUP_REQUEST_CODE = 0;

	// for control
	// R.id.right_text_view
	private TextView tvRightTextView;
	// R.id.tv_back
	private TextView tvBack;
	// R.id.select_group
	private RelativeLayout rlSelectGroup;
	// R.id.comment_name_et
	private EditText commentNameET;
	// R.id.tv_group_name
	private TextView tvGroupName;

	private long mUid;
	private User detailUser;
	private String verificationInfo;
	private String startedCause;
	private ContactsService contactService = new ContactsService();
	private String selectGroupName;
	private long selectGroupID;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		List<Group> listFriendGroup = GlobalHolder.getInstance().getGroup(
				GroupType.CONTACT.intValue());
		selectGroupName = listFriendGroup.get(0).getName();
		selectGroupID = listFriendGroup.get(0).getmGId();

		setContentView(R.layout.activity_contact_add_friend_management);
		findViewById(R.id.layout).setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				hindSoftInput(v);
				return false;
			}
		});

		startedCause = this.getIntent().getStringExtra("cause");
		connectView();
		bindViewEvent();
		if ((startedCause != null)
				&& startedCause.equals("access_friend_authentication")) {
			mUid = this.getIntent().getLongExtra("remoteUserID", 0);
			tvRightTextView
					.setText(R.string.friendManagementActivity_titlebar_right_text);
			tvBack.setText(R.string.friendManagementActivity_titlebar_left_text);
		} else if ((startedCause != null)
				&& startedCause.equals("ContactDetail2")) {
			mUid = this.getIntent().getLongExtra("uid", 0);
			verificationInfo = this.getIntent().getStringExtra(
					"verificationInfo");
			tvRightTextView
					.setText(R.string.friendManagementActivity_titlebar_right_text1);
			tvBack.setText(R.string.friendManagementActivity_titlebar_left_text1);
		} else {
			mUid = this.getIntent().getLongExtra("uid", 0);
			verificationInfo = this.getIntent().getStringExtra(
					"verificationInfo");
			tvRightTextView
					.setText(R.string.friendManagementActivity_titlebar_right_text1);
			tvBack.setText(R.string.friendManagementActivity_titlebar_left_text2);
		}

		detailUser = GlobalHolder.getInstance().getUser(mUid);
		commentNameET.setHint(detailUser.getName());
		tvGroupName.setText(selectGroupName);

	}

	private void hindSoftInput(View v) {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null && v != null) {
			imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		contactService.clearCalledBack();
	}

	private void connectView() {
		tvRightTextView = (TextView) findViewById(R.id.right_text_view);
		tvBack = (TextView) findViewById(R.id.tv_back);
		rlSelectGroup = (RelativeLayout) findViewById(R.id.select_group);
		commentNameET = (EditText) findViewById(R.id.comment_name_et);
		tvGroupName = (TextView) findViewById(R.id.tv_group_name);
	}

	private void bindViewEvent() {
		// 返回
		tvBack.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				onBackPressed();
			}
		});

		// 选择分组
		rlSelectGroup.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent i = new Intent();
				i.setClass(InputRemarkActivity.this,
						SelectJionGroupActivity.class);
				i.putExtra("from", "addFriend");
				i.putExtra("groupID", selectGroupID);
				startActivityForResult(i, 0);
			}
		});

		if ((startedCause != null)
				&& startedCause.equals("access_friend_authentication")) {
			// 完成
			tvRightTextView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					contactService.acceptAddedAsContact(selectGroupID, mUid);
					// 实现越级跳
					Intent i = new Intent(InputRemarkActivity.this,
							MessageAuthenticationActivity.class);
					i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(i);
				}
			});
		} else {
			// 发送
			tvRightTextView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {

					if (GlobalHolder.getInstance().isServerConnected()) {
						if (detailUser.getAuthtype() == 0) {
							AddFriendHistroysHandler
									.addOtherNoNeedAuthentication(
											getApplicationContext(), detailUser);
							contactService.addContact(new FriendGroup(
									selectGroupID, ""), detailUser, "",
									commentNameET.getText().toString());
						} else if (detailUser.getAuthtype() == 1) {
							AddFriendHistroysHandler
									.addOtherNeedAuthentication(
											getApplicationContext(),
											detailUser, verificationInfo, true);
							contactService.addContact(new FriendGroup(
									selectGroupID, ""), detailUser,
									verificationInfo, commentNameET.getText()
											.toString());
							Toast.makeText(
									InputRemarkActivity.this,
									R.string.friendManagementActivity_toast_text_applySentSuccess,
									Toast.LENGTH_SHORT).show();

							Intent i = new Intent();
							i.setAction(PublicIntent.BROADCAST_ADD_OTHER_FRIEND_WAITING_NOTIFICATION);
							i.addCategory(PublicIntent.DEFAULT_CATEGORY);
							sendBroadcast(i);

						} else if (detailUser.getAuthtype() == 2) {
							// 不让任何人加为好
							Toast.makeText(
									InputRemarkActivity.this,
									R.string.friendManagementActivity_toast_text_reduseAddAsFriend,
									Toast.LENGTH_SHORT).show();
						}
					} else {
						Toast.makeText(
								InputRemarkActivity.this,
								R.string.friendManagementActivity_toast_text_applySentFail,
								Toast.LENGTH_SHORT).show();
					}

					// 实现越级跳
					Intent i = new Intent(InputRemarkActivity.this,
							ContactDetail2.class);
					i.putExtra("nickName", commentNameET.getText().toString());
					i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(i);
				}
			});
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == SELECT_GROUP_REQUEST_CODE) {
			if (resultCode == SelectJionGroupActivity.SELECT_GROUP_RESPONSE_CODE_DONE) {
				if (data != null) {
					selectGroupName = data.getStringExtra("groupName");
					selectGroupID = data.getLongExtra("groupID", 0);
					tvGroupName.setText(selectGroupName);
				}
			} else if (resultCode == SelectJionGroupActivity.SELECT_GROUP_RESPONSE_CODE_CANCEL) {
			}
		}

	}

}
