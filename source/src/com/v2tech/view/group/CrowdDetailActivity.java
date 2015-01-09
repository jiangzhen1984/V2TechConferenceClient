package com.v2tech.view.group;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import com.V2.jni.V2GlobalEnum;
import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.service.CrowdGroupService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.MessageListener;
import com.v2tech.view.JNIService;
import com.v2tech.view.PublicIntent;
import com.v2tech.view.bo.GroupUserObject;
import com.v2tech.view.conversation.CommonCallBack;
import com.v2tech.view.conversation.CommonCallBack.CommonNotifyCrowdDetailNewMessage;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.NetworkStateCode;

public class CrowdDetailActivity extends Activity implements CommonNotifyCrowdDetailNewMessage{

	private final static String RULE_ALLOW_ALL = "0";
	private final static String RULE_QUALIFICATION = "1";
	private final static String RULE_NEVER = "2";

	private final static int TYPE_BRIEF = 1;
	private final static int TYPE_ANNOUNCE = 2;
	private final static int TYPE_UPDATE_MEMBERS = 3;
	
	
	private final static int REQUEST_UPDATE_CROWD_DONE = 1;
	private final static int REQUEST_QUIT_CROWD_DONE = 2;

	private TextView mNoTV;
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
	private View mSHowFilesButton;
	private View mReturnButton;
	private TextView mButtonText;
	private RadioGroup mRulesRD;
	private View mRulesLayout;
	private View mNewFileNotificator;

	private CrowdGroup crowd;
	private CrowdGroupService service = new CrowdGroupService();
	private State mState = State.NONE;
	private LocalReceiver localReceiver;
	private Context mContext;
	
	private int defaultRule;
	private long lastNotificatorTime;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.crowd_detail_activity);
		mContext = this;
		mNoTV = (TextView) findViewById(R.id.crowd_detail_no);
		mNameTV = (TextView) findViewById(R.id.crowd_detail_name);
		mCreatorTV = (TextView) findViewById(R.id.crowd_detail_creator);
		mBriefTV = (TextView) findViewById(R.id.crowd_detail_brief);
		mAnouncementTV = (TextView) findViewById(R.id.crowd_detail_announcement);
		mMembersCountsTV = (TextView) findViewById(R.id.crowd_detail_members);
		mAdminBox = findViewById(R.id.crowd_detail_admistrator_box);
		mButtonText = (TextView) findViewById(R.id.crowd_detail_button_text);
		mRulesRD = (RadioGroup) findViewById(R.id.crowd_detail_radio_group);
		mRulesLayout = findViewById(R.id.crowd_detail_radio_group_layout);
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
		mSHowFilesButton = findViewById(R.id.crowd_detail_files_button);
		mSHowFilesButton.setOnClickListener(mShowFilesButtonListener);
		
		mNewFileNotificator = findViewById(R.id.crowd_detail_new_file_notificator);
		
		crowd = (CrowdGroup) GlobalHolder.getInstance().getGroupById(
				GroupType.CHATING.intValue(), getIntent().getLongExtra("cid", 0));

		if(crowd == null){
			super.onDestroy();
			return ;
		}
		String cid  = String.valueOf(crowd.getmGId());
//		mNoTV.setText(cid.length() > 4 ? cid.substring(5) : cid.substring(1));
		mNoTV.setText(cid);
		mNameTV.setText(crowd.getName());
		mBriefTV.setText(crowd.getBrief());
		mCreatorTV.setText(crowd.getOwnerUser().getName());
		mCreatorTV.setSingleLine();
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
		
		mMembersCountsTV.setText(String.valueOf(crowd.getUsers().size()));
		CommonCallBack.getInstance().setNotifyCrowdDetailActivity(this);
		initRules();
		mRulesRD.setOnCheckedChangeListener(mRulesChangedListener);
		mRulesLayout.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (!GlobalHolder.getInstance().isServerConnected()) {
					Toast.makeText(mContext,
							R.string.error_discussion_no_network,
							Toast.LENGTH_SHORT).show();
				}
				return false;
			}
		});
		initReceiver();
		updateGroupFileNotificator();
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

	@Override
	protected void onDestroy() {
		super.onDestroy();
		this.unregisterReceiver(localReceiver);
		service.clearCalledBack();
	}


	
	private void updateGroupFileNotificator() {
		if (crowd.getNewFileCount() > 0) {
			mNewFileNotificator.setVisibility(View.VISIBLE);
		} else {
			mNewFileNotificator.setVisibility(View.GONE);
		}
	}


	private void initReceiver() {
		localReceiver = new LocalReceiver(); 
		IntentFilter filter = new IntentFilter();
		filter.addAction(JNIService.JNI_BROADCAST_KICED_CROWD);
		filter.addAction(PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION);
		filter.addAction(JNIService.JNI_BROADCAST_GROUP_UPDATED);
		filter.addAction(JNIService.JNI_BROADCAST_GROUP_USER_ADDED);
		filter.addAction(JNIService.JNI_BROADCAST_GROUP_USER_REMOVED);
		filter.addAction(JNIService.BROADCAST_CROWD_NEW_UPLOAD_FILE_NOTIFICATION);
		filter.addAction(JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION);
		filter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		filter.addCategory(PublicIntent.DEFAULT_CATEGORY);
		this.registerReceiver(localReceiver, filter);
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
				defaultRule = mRulesRD.getChildAt(i).getId();
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
					service.quitCrowd(crowd, new MessageListener(mLocalHandler, REQUEST_QUIT_CROWD_DONE, null));
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
		i.putExtra("group", new GroupUserObject(V2GlobalEnum.GROUP_TYPE_CROWD, crowd.getmGId(), -1));
		this.sendBroadcast(i);
		
		
		Intent quit = new Intent(PublicIntent.BROADCAST_CROWD_QUIT_NOTIFICATION);
		quit.addCategory(PublicIntent.DEFAULT_CATEGORY);
		quit.putExtra("userId", crowd.getmGId());
		quit.putExtra("groupId", GlobalHolder.getInstance().getCurrentUserId());
		quit.putExtra("kicked",false);
		sendBroadcast(quit);
		
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
			RadioButton rb = (RadioButton) mRulesRD.findViewById(id);
			synchronized (mState) {
				if (mState == State.PENDING) {
					return;
				}
				mState = State.PENDING;
			}
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
				service.updateCrowd(crowd, new MessageListener(mLocalHandler,
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
			startActivityForResult(i, TYPE_UPDATE_MEMBERS);
		}

	};
	
	
	private OnClickListener mShowFilesButtonListener = new  OnClickListener() {

		@Override
		public void onClick(View view) {
			
			Intent i = new Intent(PublicIntent.START_CROWD_FILES_ACTIVITY);
			i.addCategory(PublicIntent.DEFAULT_CATEGORY);
			i.putExtra("cid", crowd.getmGId());
			startActivity(i);
			
			crowd.resetNewFileCount();
			updateGroupFileNotificator();
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
	
	
	class LocalReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION
					.equals(intent.getAction())
					|| intent.getAction().equals(
							JNIService.JNI_BROADCAST_KICED_CROWD)) {
				GroupUserObject obj = intent.getParcelableExtra("group");
				if (obj == null) {
					V2Log.e("CrowdDetailActivity",
							"Received the broadcast to quit the crowd group , but crowd id is wroing... ");
					return;
				}
				if (obj.getmGroupId() == crowd.getmGId()) {
					finish();
				}
			} else if (intent.getAction().equals(JNIService.JNI_BROADCAST_GROUP_UPDATED)) {
				long crowdId = intent.getLongExtra("gid", 0);
				//Update content
				if (crowdId == crowd.getmGId()) {
					initRules();
					mBriefTV.setText(crowd.getBrief());
					mAnouncementTV.setText(crowd.getAnnouncement());
					mMembersCountsTV.setText(crowd.getUsers().size()+"");
				}
			} else if (intent.getAction().equals(JNIService.JNI_BROADCAST_GROUP_USER_ADDED)) {
				GroupUserObject guo = (GroupUserObject)intent.getExtras().get("obj");
				if (guo.getmGroupId() == crowd.getmGId()) {
					CrowdGroup newGroup = (CrowdGroup) GlobalHolder.getInstance().
							getGroupById(V2GlobalEnum.GROUP_TYPE_CROWD, crowd.getmGId());
					if(newGroup != null){
						crowd = newGroup;
						mMembersCountsTV.setText(String.valueOf(crowd.getUsers().size()));
					}
				}
			} else if (intent.getAction().equals(JNIService.JNI_BROADCAST_GROUP_USER_REMOVED)) {
				CrowdGroup newGroup = (CrowdGroup) GlobalHolder.getInstance().
						getGroupById(V2GlobalEnum.GROUP_TYPE_CROWD, crowd.getmGId());
				if(newGroup != null){
					crowd = newGroup;
					mMembersCountsTV.setText(String.valueOf(crowd.getUsers().size()));
				}
			} else if (intent.getAction().equals(JNIService.BROADCAST_CROWD_NEW_UPLOAD_FILE_NOTIFICATION)) {
				long crowdId = intent.getLongExtra("groupID", 0);
				if (crowdId == crowd.getmGId()) {
					updateGroupFileNotificator();
				}
			}
			else if (intent.getAction().equals(JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION)) {
				NetworkStateCode code = (NetworkStateCode) intent.getExtras()
						.get("state");
				if (code == NetworkStateCode.CONNECTED_ERROR) {
					for (int i = 0; i < mRulesRD.getChildCount(); i++) {
						if (mRulesRD.getChildAt(i) instanceof RadioButton) {
							mRulesRD.getChildAt(i).setClickable(false);
						} 
					}
				} else {
					for (int i = 0; i < mRulesRD.getChildCount(); i++) {
						if (mRulesRD.getChildAt(i) instanceof RadioButton) {
							mRulesRD.getChildAt(i).setClickable(true);
						} 
					}
				}
			}
		}
		
	}


	@Override
	public void notifyCrowdDetailNewMessage() {
		if ((System.currentTimeMillis() / 1000) - lastNotificatorTime > 2) {
			Uri notification = RingtoneManager
					.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			Ringtone r = RingtoneManager.getRingtone(mContext, notification);
			if (r != null) {
				r.play();
			}
			lastNotificatorTime = System.currentTimeMillis() / 1000;
		}
	}
}
