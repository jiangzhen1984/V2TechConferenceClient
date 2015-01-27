package com.bizcom.vc.activity.crow;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.V2.jni.util.V2Log;
import com.bizcom.bo.ConversationNotificationObject;
import com.bizcom.bo.GroupUserObject;
import com.bizcom.db.provider.VerificationProvider;
import com.bizcom.request.CrowdGroupService;
import com.bizcom.request.MessageListener;
import com.bizcom.request.jni.JNIResponse;
import com.bizcom.util.ProgressUtils;
import com.bizcom.vc.activity.message.MessageAuthenticationActivity;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.application.PublicIntent;
import com.bizcom.vc.application.V2GlobalConstants;
import com.bizcom.vc.service.JNIService;
import com.bizcom.vo.Conversation;
import com.bizcom.vo.Crowd;
import com.bizcom.vo.CrowdGroup;
import com.bizcom.vo.Group;
import com.bizcom.vo.GroupQualicationState;
import com.bizcom.vo.VMessageQualification;
import com.bizcom.vo.CrowdGroup.AuthType;
import com.bizcom.vo.VMessageQualification.QualificationState;
import com.bizcom.vo.VMessageQualification.ReadState;
import com.bizcom.vo.VMessageQualification.Type;
import com.v2tech.R;

public class CrowdInvitationActivity extends Activity {

	private final static int ACCEPT_INVITATION_DONE = 1;
	private final static int REFUSE_INVITATION_DONE = 2;
	private final static int UPDATE_CROWD_INFO = 3;

	private TextView mTitleName;
	private TextView mNameTV;
	private TextView mNoTV;
	private TextView mCreatorTV;
	private TextView mBriefTV;
	private TextView mAnnounceTV;
	private TextView mMembersTV;

	private View mReturnButton;
	private TextView mAcceptButton;
	private View mDeclineButton;
	private View mButtonLayout;
	private View mNotesLayout;
	private TextView mSendMsgButton;
	private TextView mNotesTV;
	private EditText mReasonET;
	private View mRejectResasonLayout;
	private View mBoxLy;
	private View mAcceptedLy;

	private Context mContext;

	private Crowd crowd;
	private CrowdGroupService service = new CrowdGroupService();
	private CrowdInviteBroadcast inviteReceiver;
	private VMessageQualification vq;
	private State mState = State.DONE;
	private boolean isInRejectReasonMode = false;
	private boolean isReturnData;

	private boolean isShowNow;
	private boolean isNeedFinish;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;

		setContentView(R.layout.crowd_invitation_activity);
		initReceiver();
		mTitleName = (TextView) findViewById(R.id.crowd_invitation_title_text);
		mNameTV = (TextView) findViewById(R.id.crowd_invitation_name);
		mNoTV = (TextView) findViewById(R.id.crowd_invitation_crowd_no);
		mCreatorTV = (TextView) findViewById(R.id.crowd_invitation_creator_tv);
		mCreatorTV.setSingleLine();
		mBriefTV = (TextView) findViewById(R.id.crowd_invitation_brief);
		mAnnounceTV = (TextView) findViewById(R.id.crowd_invitation_announcement);
		mMembersTV = (TextView) findViewById(R.id.crowd_invitation_members);

		mAcceptButton = (TextView) findViewById(R.id.crowd_invitation_accept_button);
		mAcceptButton.setOnClickListener(mAcceptButtonListener);
		mDeclineButton = findViewById(R.id.crowd_invitation_decline_button);
		mDeclineButton.setOnClickListener(mDeclineButtonListener);
		mReturnButton = findViewById(R.id.crowd_invitation_return_button);
		mReturnButton.setOnClickListener(mReturnButtonListener);
		mSendMsgButton = (TextView) findViewById(R.id.crowd_invitation_send_msg_button);
		mSendMsgButton.setOnClickListener(mSendMsgButtonListener);

		mButtonLayout = findViewById(R.id.crowd_invitation_button_ly);
		mNotesLayout = findViewById(R.id.crowd_invitation_notes_ly);
		mNotesTV = (TextView) findViewById(R.id.crowd_invitation_notes);
		mAcceptedLy = findViewById(R.id.crowd_invitation_accepted_ly);

		mRejectResasonLayout = findViewById(R.id.crowd_content_reject_reason_ly);
		mBoxLy = findViewById(R.id.crowd_invitation_box_ly);
		mReasonET = (EditText) findViewById(R.id.crowd_content_reject_reason_et);

		crowd = (Crowd) getIntent().getExtras().get("crowd");
		String cid = String.valueOf(crowd.getId());
		// mNoTV.setText(cid.length() > 4 ? cid.substring(5) :
		// cid.substring(1));
		mNoTV.setText(cid);

		mNameTV.setText(crowd.getName());
		mBriefTV.setText(crowd.getBrief());
		mCreatorTV.setText(crowd.getCreator().getName());
		mAnnounceTV.setText(crowd.getAnnounce());

		Group group = GlobalHolder.getInstance().getGroupById(
				V2GlobalConstants.GROUP_TYPE_CROWD, crowd.getId());
		if (group != null && group.getUsers() != null)
			mMembersTV.setText(String.valueOf(group.getUsers().size()) + "人");

		CrowdGroup g = new CrowdGroup(crowd.getId(), crowd.getName(),
				crowd.getCreator(), null);
		g.setAuthType(AuthType.fromInt(crowd.getAuth()));
		vq = VerificationProvider.queryCrowdQualMessageByCrowdId(crowd
				.getCreator().getmUserId(), g.getmGId());
		if (vq == null) {
			Toast.makeText(
					getApplicationContext(),
					getResources().getString(R.string.crowd_Authentication_hit),
					Toast.LENGTH_LONG).show();
			super.onBackPressed();
			return;
		}

		updateView(false);
		mRejectResasonLayout.setVisibility(View.GONE);
	}

	@Override
	protected void onRestart() {
		if (isNeedFinish) {
			isNeedFinish = false;
			onBackPressed();
		}
		super.onRestart();
	}

	@Override
	protected void onResume() {
		isShowNow = true;
		super.onResume();
	}

	@Override
	protected void onStop() {
		isShowNow = false;
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(inviteReceiver);
		super.onDestroy();
	}

	private void initReceiver() {
		inviteReceiver = new CrowdInviteBroadcast();
		IntentFilter filter = new IntentFilter();
		filter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		filter.addCategory(PublicIntent.DEFAULT_CATEGORY);
		filter.addAction(JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION);
		filter.addAction(JNIService.JNI_BROADCAST_GROUP_JOIN_FAILED);
		filter.addAction(JNIService.JNI_BROADCAST_KICED_CROWD);
		filter.addAction(PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION);
		registerReceiver(inviteReceiver, filter);
	}

	private void handleAcceptDone() {
		CrowdGroup group = (CrowdGroup) GlobalHolder.getInstance()
				.getGroupById(V2GlobalConstants.GROUP_TYPE_CROWD, crowd.getId());
		if (group != null && group.getUsers() != null) {
			mMembersTV.setText(String.valueOf(group.getUsers().size()) + "人");
			mAnnounceTV.setText(group.getAnnouncement());
			mBriefTV.setText(group.getBrief());
		}

		VMessageQualification message = VerificationProvider
				.queryCrowdQualMessageById(vq.getId());
		vq.setQualState(message.getQualState());
		vq.setReadState(message.getReadState());
		GroupQualicationState state = new GroupQualicationState(
				Type.CROWD_INVITATION, QualificationState.ACCEPTED, null,
				ReadState.READ, false);
		state.isUpdateTime = false;
		VerificationProvider.updateCrowdQualicationMessageState(crowd.getId(),
				crowd.getCreator().getmUserId(), state);
		updateView(false);
	}

	private void handleDeclineDone() {
		vq.setReadState(VMessageQualification.ReadState.READ);
		vq.setQualState(VMessageQualification.QualificationState.REJECT);
		GroupQualicationState state = new GroupQualicationState(
				Type.CROWD_INVITATION, QualificationState.REJECT, null,
				ReadState.READ, false);
		state.isUpdateTime = false;
		VerificationProvider.updateCrowdQualicationMessageState(crowd.getId(),
				crowd.getCreator().getmUserId(), state);
		updateView(false);
	}

	private void updateView(boolean isInReject) {
		// view screen changed for rejection
		if (isInReject) {
			mBoxLy.setVisibility(View.GONE);
			mRejectResasonLayout.setVisibility(View.VISIBLE);
			mSendMsgButton.setVisibility(View.VISIBLE);
			mSendMsgButton
					.setText(R.string.crowd_invitation_reject_button_done);
			mButtonLayout.setVisibility(View.GONE);
			mTitleName.setText(R.string.crowd_invitation_reject_titile);

		} else {
			boolean isFromApplication = this.getIntent().getBooleanExtra(
					"isFromApplication", false);
			if (isFromApplication) {
				CrowdGroup crowdGroup = (CrowdGroup) GlobalHolder.getInstance().
						getGroupById(V2GlobalConstants.GROUP_TYPE_CROWD, crowd.getId());
				if(crowdGroup != null){
					mMembersTV.setText(String.valueOf(crowdGroup.getUsers().size())
							+ "人");
					mAnnounceTV.setText(crowdGroup.getAnnouncement());
					mBriefTV.setText(crowdGroup.getBrief());
				} 
				mTitleName.setText(R.string.crowd_application_title);
				mAcceptedLy.setVisibility(View.VISIBLE);
				mButtonLayout.setVisibility(View.GONE);
				mSendMsgButton.setVisibility(View.VISIBLE);
				mSendMsgButton
						.setText(R.string.crowd_invitation_top_bar_right_button);

				Intent i = new Intent(PublicIntent.REQUEST_UPDATE_CONVERSATION);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				ConversationNotificationObject obj = new ConversationNotificationObject(
						Conversation.TYPE_VERIFICATION_MESSAGE,
						Conversation.SPECIFIC_VERIFICATION_ID, false);
				i.putExtra("obj", obj);
				mContext.sendBroadcast(i);
			} else {
				mTitleName.setText(R.string.crowd_invitation_titile);
				if (vq.getQualState() == VMessageQualification.QualificationState.ACCEPTED) {
					mButtonLayout.setVisibility(View.GONE);

					mAcceptedLy.setVisibility(View.VISIBLE);
					mNotesLayout.setVisibility(View.VISIBLE);
					mNotesTV.setText(R.string.crowd_invitation_accept_notes);
					mSendMsgButton.setVisibility(View.VISIBLE);
				} else if (vq.getQualState() == VMessageQualification.QualificationState.REJECT) {
					mButtonLayout.setVisibility(View.GONE);
					mSendMsgButton.setVisibility(View.GONE);
					mAcceptedLy.setVisibility(View.GONE);

					mNotesLayout.setVisibility(View.VISIBLE);
					mNotesTV.setText(R.string.crowd_invitation_reject_notes);
				} else if (vq.getQualState() == QualificationState.INVALID) {
					mButtonLayout.setVisibility(View.GONE);
					mSendMsgButton.setVisibility(View.GONE);
					mAcceptedLy.setVisibility(View.GONE);

					mNotesLayout.setVisibility(View.VISIBLE);
					mNotesTV.setText(R.string.crowd_invitation_invalid_notes);
				} else if (vq.getQualState() == QualificationState.WAITING_FOR_APPLY) {
					mButtonLayout.setVisibility(View.GONE);
					mSendMsgButton.setVisibility(View.GONE);
					mAcceptedLy.setVisibility(View.GONE);

					mNotesLayout.setVisibility(View.VISIBLE);
					mNotesTV.setText(R.string.crowd_application_applyed);
					mTitleName.setText(R.string.crowd_applicant_invite_title);
				} else {
					mSendMsgButton.setVisibility(View.GONE);
					mAcceptedLy.setVisibility(View.GONE);
					mButtonLayout.setVisibility(View.VISIBLE);
				}
				mSendMsgButton
						.setText(R.string.crowd_invitation_top_bar_right_button);
				mBoxLy.setVisibility(View.VISIBLE);
				mRejectResasonLayout.setVisibility(View.GONE);
			}
		}

		if (isInRejectReasonMode != isInReject) {
			if (isInReject) {
				Animation out = AnimationUtils.loadAnimation(mContext,
						R.animator.left_in);
				mRejectResasonLayout.startAnimation(out);
				Animation in = AnimationUtils.loadAnimation(mContext,
						R.animator.left_out);
				mBoxLy.startAnimation(in);
			} else {
				Animation out = AnimationUtils.loadAnimation(mContext,
						R.animator.right_in);
				mBoxLy.startAnimation(out);
				Animation in = AnimationUtils.loadAnimation(mContext,
						R.animator.right_out);
				mRejectResasonLayout.startAnimation(in);
			}

			isInRejectReasonMode = isInReject;
		}

	}

	private OnClickListener mAcceptButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			synchronized (mState) {
				if (mState == State.UPDATING) {
					return;
				}
				mState = State.UPDATING;
				VMessageQualification message = VerificationProvider
						.queryCrowdQualMessageById(vq.getId());
				if (message.getQualState().intValue() != vq.getQualState()
						.intValue())
					handleAcceptDone();
				else {
					service.acceptInvitation(crowd, new MessageListener(
							mLocalHandler, ACCEPT_INVITATION_DONE, null));
					ProgressUtils.showNormalWithHintProgress(mContext, true);
				}
			}
		}

	};

	private OnClickListener mDeclineButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			if (!GlobalHolder.getInstance().isServerConnected()) {
				Toast.makeText(mContext,
						R.string.error_local_connect_to_server,
						Toast.LENGTH_SHORT).show();
				return;
			}

			if (!isInRejectReasonMode) {
				updateView(!isInRejectReasonMode);
			}
		}

	};

	private OnClickListener mReturnButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			onBackPressed();
		}

	};

	private OnClickListener mSendMsgButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			if (isInRejectReasonMode) {
				synchronized (mState) {
					if (mState == State.UPDATING) {
						return;
					}
					mState = State.UPDATING;
					VMessageQualification message = VerificationProvider
							.queryCrowdQualMessageById(vq.getId());
					if (message.getQualState().intValue() != vq.getQualState()
							.intValue())
						handleDeclineDone();
					else {
						service.refuseInvitation(crowd, mReasonET
								.getEditableText().toString(),
								new MessageListener(mLocalHandler,
										REFUSE_INVITATION_DONE, null));
						handleDeclineDone();
					}
				}
				return;
			} else {
				Group group = GlobalHolder.getInstance().getGroupById(
						crowd.getId());
				Intent i = new Intent(PublicIntent.START_CONVERSACTION_ACTIVITY);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				i.putExtra("obj", new ConversationNotificationObject(group
						.getGroupType().intValue(), group.getmGId()));
				startActivity(i);
			}
		}

	};

	private class CrowdInviteBroadcast extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (JNIService.JNI_BROADCAST_GROUP_JOIN_FAILED.equals(intent
					.getAction())) {
				ProgressUtils.showNormalWithHintProgress(mContext, false);
				mButtonLayout.setVisibility(View.GONE);
				mSendMsgButton.setVisibility(View.GONE);
				mAcceptedLy.setVisibility(View.GONE);

				mNotesLayout.setVisibility(View.VISIBLE);
				mNotesTV.setText(R.string.crowd_invitation_invalid_notes);
				Toast.makeText(
						getApplicationContext(),
						getResources().getString(
								R.string.crowd_Authentication_hit),
						Toast.LENGTH_SHORT).show();

				vq.setQualState(VMessageQualification.QualificationState.INVALID);
				VerificationProvider.updateCrowdQualicationMessage(vq);
				isReturnData = true;
				mReturnButton.performClick();
			} else if (PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION
					.equals(intent.getAction())
					|| intent.getAction().equals(
							JNIService.JNI_BROADCAST_KICED_CROWD)) {
				GroupUserObject obj = intent.getParcelableExtra("group");
				if (obj == null) {
					V2Log.e("CrowdInvitationActivity",
							"Received the broadcast to quit the crowd group , but crowd id is wroing... ");
					return;
				}

				if (isShowNow)
					onBackPressed();
				else
					isNeedFinish = true;
			} else if (JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION
					.equals(intent.getAction())) {
				CrowdGroup crowdGroup = (CrowdGroup) GlobalHolder.getInstance()
						.getGroupById(V2GlobalConstants.GROUP_TYPE_CROWD,
								crowd.getId());
				if (crowdGroup != null) {
					mMembersTV.setText(String.valueOf(crowdGroup.getUsers()
							.size()) + R.string.crowd_invitation_person);
					mAnnounceTV.setText(crowdGroup.getAnnouncement());
					mBriefTV.setText(crowdGroup.getBrief());
				}
			}
		}
	}

	@Override
	public void onBackPressed() {
		if (isInRejectReasonMode) {
			updateView(!isInRejectReasonMode);
			return;
		}

		if (isReturnData) {
			Intent intent = new Intent(CrowdInvitationActivity.this,
					MessageAuthenticationActivity.class);
			intent.putExtra("qualificationID", vq.getId());
			intent.putExtra("qualState", vq.getQualState());
			setResult(MessageAuthenticationActivity.AUTHENTICATION_RESULT,
					intent);
		}
		super.onBackPressed();
	}

	private Handler mLocalHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			isReturnData = true;
			synchronized (mState) {
				mState = State.DONE;
			}
			switch (msg.what) {
			case ACCEPT_INVITATION_DONE:
				JNIResponse jni = (JNIResponse) msg.obj;
				if (jni.getResult().ordinal() == JNIResponse.Result.SUCCESS
						.ordinal()) {
					handleAcceptDone();
				}
				break;
			case REFUSE_INVITATION_DONE:
				// ProgressUtils.showNormalWithHintProgress(mContext, false);
				// handleDeclineDone();
				break;
			case UPDATE_CROWD_INFO:
				CrowdGroup crowdGroup = (CrowdGroup) msg.obj;
				mMembersTV.setText(String.valueOf(crowdGroup.getUsers().size())
						+ R.string.crowd_invitation_person);
				mAnnounceTV.setText(crowdGroup.getAnnouncement());
				mBriefTV.setText(crowdGroup.getBrief());
				break;
			}
			ProgressUtils.showNormalWithHintProgress(mContext, false);
		}

	};

	enum State {
		DONE, UPDATING
	}

}
