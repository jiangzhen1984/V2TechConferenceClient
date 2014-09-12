package com.v2tech.view.group;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.service.CrowdGroupService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.Registrant;
import com.v2tech.view.PublicIntent;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.Group.GroupType;

public class CrowdDetailActivity extends Activity {

	private final static String RULE_ALLOW_ALL = "0";
	private final static String RULE_QUALIFICATION = "1";
	private final static String RULE_NEVER = "2";

	private final static int TYPE_BRIEF = 1;
	private final static int TYPE_ANNOUNCE = 2;
	private final static int TYPE_UPDATE_MEMBERS = 3;
	
	
	private final static int REQUEST_UPDATE_CROWD_DONE = 1;
	private final static int REQUEST_QUIT_CROWD_DONE = 2;

	private TextView mNameTV;
	private TextView mCreatorTV;
	private TextView mBriefTV;
	private TextView mAnouncementTV;
	private TextView mMembersCountsTV;
	private TextView mDialogTitleTV;

	private View mAdminBox;
	private View mQuitButton;
	private View mShowBriefButton;
	private View mShowAnnounceButton;
	private View mShowMembersButton;
	private View mReturnButton;
	private TextView mButtonText;
	private RadioGroup mRulesRD;

	private CrowdGroup crowd;
	private CrowdGroupService service = new CrowdGroupService();
	private State mState = State.NONE;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.crowd_detail_activity);
		mNameTV = (TextView) findViewById(R.id.crowd_detail_name);
		mCreatorTV = (TextView) findViewById(R.id.crowd_detail_creator);
		mBriefTV = (TextView) findViewById(R.id.crowd_detail_brief);
		mAnouncementTV = (TextView) findViewById(R.id.crowd_detail_announcement);
		mMembersCountsTV = (TextView) findViewById(R.id.crowd_detail_members);
		mAdminBox = findViewById(R.id.crowd_detail_admistrator_box);
		mButtonText = (TextView) findViewById(R.id.crowd_detail_button_text);
		mRulesRD = (RadioGroup) findViewById(R.id.crowd_detail_radio_group);
		mQuitButton = findViewById(R.id.crowd_detail_button);
		mQuitButton.setOnClickListener(mQuitButtonListener);
		mReturnButton = findViewById(R.id.crowd_detail_return_button);
		mReturnButton.setOnClickListener(mReturnButtonListener);
		
		mShowBriefButton = findViewById(R.id.crowd_detail_brief_button);
		mShowBriefButton.setOnClickListener(mContentButtonListener);
		mShowAnnounceButton = findViewById(R.id.crowd_detail_announcement_button);
		mShowAnnounceButton.setOnClickListener(mContentButtonListener);
		mShowMembersButton= findViewById(R.id.crowd_detail_invitation_members_button);
		mShowMembersButton.setOnClickListener(mShowMembersButtonListener);

		crowd = (CrowdGroup) GlobalHolder.getInstance().getGroupById(
				GroupType.CHATING, getIntent().getLongExtra("cid", 0));

		mNameTV.setText(crowd.getName());
		mBriefTV.setText(crowd.getBrief());
		mCreatorTV.setText(crowd.getOwnerUser().getName());
		mAnouncementTV.setText(crowd.getAnnouncement());
		if (crowd.getOwnerUser().getmUserId() == GlobalHolder.getInstance()
				.getCurrentUserId()) {
			mAdminBox.setVisibility(View.VISIBLE);
			mButtonText
					.setText(R.string.crowd_detail_qulification_dismiss_button);
		} else {
			mAdminBox.setVisibility(View.GONE);
			mButtonText.setText(R.string.crowd_detail_qulification_quit_button);
		}
		
		mMembersCountsTV.setText(crowd.getUsers().size()+"");
		initRules();
		mRulesRD.setOnCheckedChangeListener(mRulesChangedListener);

	}
	
	
	
	

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == TYPE_BRIEF) {
			mBriefTV.setText(crowd.getBrief());
		} else if (requestCode == TYPE_ANNOUNCE) {
			mAnouncementTV.setText(crowd.getAnnouncement());
		} else if (requestCode == TYPE_UPDATE_MEMBERS) {
			mMembersCountsTV.setText(crowd.getUsers().size()+"");
		}
	}





	private void initRules() {
		for (int i = 0; i < mRulesRD.getChildCount(); i++) {
			if (mRulesRD.getChildAt(i) instanceof RadioButton) {
				RadioButton rb = (RadioButton) mRulesRD.getChildAt(i);
				if (rb.getTag().equals(RULE_ALLOW_ALL)
						&& crowd.getAuthType() == CrowdGroup.AuthType.ALLOW_ALL) {
					rb.setChecked(true);
				} else if (rb.getTag().equals(RULE_QUALIFICATION)
						&& crowd.getAuthType() == CrowdGroup.AuthType.QULIFICATION) {
					rb.setChecked(true);
				} else if (rb.getTag().equals(RULE_NEVER)
						&& crowd.getAuthType() == CrowdGroup.AuthType.NEVER) {
					rb.setChecked(true);
				}
			}
		}
	}

	private Dialog mDialog;

	private void showDialog() {
		if (mDialog == null) {

			mDialog = new Dialog(this, R.style.ContactUserActionDialog);

			mDialog.setContentView(R.layout.crowd_quit_confirmation_dialog);
			final Button cancelB = (Button) mDialog
					.findViewById(R.id.contacts_group_cancel_button);
			cancelB.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mDialog.dismiss();
				}

			});
			final Button confirmButton = (Button) mDialog
					.findViewById(R.id.contacts_group_confirm_button);
			confirmButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					service.quitCrowd(crowd, new Registrant(mLocalHandler, REQUEST_QUIT_CROWD_DONE, null));
				}

			});

			mDialogTitleTV = (TextView) mDialog
					.findViewById(R.id.crowd_quit_dialog_title);
		}

		if (crowd.getOwnerUser().getmUserId() == GlobalHolder.getInstance()
				.getCurrentUserId()) {
			mDialogTitleTV.setText(R.string.crowd_detail_dismiss_confirm_title);
		} else {
			mDialogTitleTV.setText(R.string.crowd_detail_quit_confirm_title);
		}

		mDialog.show();
	}
	
	
	private void handleQuitDone() {
		//Remove cache crowd
		GlobalHolder.getInstance().removeGroup(GroupType.CHATING, crowd.getmGId());
		//send broadcast to notify conversationtabfragment refresh list
		Intent i = new Intent(PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION);
		i.addCategory(PublicIntent.DEFAULT_CATEGORY);
		i.putExtra("crowd", crowd.getmGId());
		this.sendBroadcast(i);
		finish();
	}

	private OnClickListener mQuitButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			showDialog();
		}

	};
	
	private OnClickListener mReturnButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			onBackPressed();
		}

	};
	

	private OnCheckedChangeListener mRulesChangedListener = new OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(RadioGroup rg, int id) {
			synchronized (mState) {
				if (mState == State.PENDING) {
					return;
				}
				mState = State.PENDING;
			}
			RadioButton rb = (RadioButton) mRulesRD.findViewById(id);
			CrowdGroup.AuthType at = null;
			if (RULE_ALLOW_ALL.equals(rb.getTag())) {
				at = CrowdGroup.AuthType.ALLOW_ALL;
			} else if (RULE_QUALIFICATION.equals(rb.getTag())) {
				at = CrowdGroup.AuthType.QULIFICATION;
			} else if (RULE_NEVER.equals(rb.getTag())) {
				at = CrowdGroup.AuthType.NEVER;
			} else {
				V2Log.e(" unkonw type");
				mState = State.NONE;
			}

			if (at != crowd.getAuthType()) {
				crowd.setAuthType(at);
				service.updateCrowd(crowd, new Registrant(mLocalHandler,
						REQUEST_UPDATE_CROWD_DONE, null));
			}
		}

	};
	
	private OnClickListener mContentButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			int type = 0;
			//type value must match with CrowdContentUpdateActivity.UPDATE_TYPE_BRIEF or
			//or CrowdContentUpdateActivity.UPDATE_TYPE_ANNOUNCEMENT
			if (view == mShowBriefButton) {
				type = TYPE_BRIEF;
			} else if (view == mShowAnnounceButton) {
				type = TYPE_ANNOUNCE;
			}
			
			Intent i = new Intent(PublicIntent.SHOW_CROWD_CONTENT_ACTIVITY);
			i.addCategory(PublicIntent.DEFAULT_CATEGORY);
			i.putExtra("type", type);
			i.putExtra("cid", crowd.getmGId());
			startActivityForResult(i, type);
		}

	};
	
	private OnClickListener mShowMembersButtonListener = new  OnClickListener() {

		@Override
		public void onClick(View view) {
			Intent i = new Intent(PublicIntent.START_CROWD_MEMBERS_ACTIVITY);
			i.addCategory(PublicIntent.DEFAULT_CATEGORY);
			i.putExtra("cid", crowd.getmGId());
			startActivity(i);
		}

	};
	
	
	

	private Handler mLocalHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case REQUEST_UPDATE_CROWD_DONE:
				synchronized (mState) {
					mState = State.NONE;
				}
				break;
			case REQUEST_QUIT_CROWD_DONE:
				handleQuitDone();
				if (mDialog != null) {
					mDialog.dismiss();
				}
				break;

			}
		}

	};

	enum State {
		NONE, PENDING;
	}

}