package com.v2tech.view.group;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.TextView;

import com.V2.jni.V2GlobalEnum;
import com.v2tech.R;
import com.v2tech.service.CrowdGroupService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.MessageListener;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.util.ProgressUtils;
import com.v2tech.view.PublicIntent;
import com.v2tech.view.bo.ConversationNotificationObject;
import com.v2tech.view.conversation.MessageAuthenticationActivity;
import com.v2tech.view.conversation.MessageBuilder;
import com.v2tech.vo.Crowd;
import com.v2tech.vo.CrowdConversation;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.CrowdGroup.AuthType;
import com.v2tech.vo.Group;
import com.v2tech.vo.GroupQualicationState;
import com.v2tech.vo.VMessageQualification;
import com.v2tech.vo.VMessageQualification.QualificationState;
import com.v2tech.vo.VMessageQualification.ReadState;
import com.v2tech.vo.VMessageQualification.Type;

public class CrowdInvitationActivity extends Activity {

	private final static int ACCEPT_INVITATION_DONE = 1;
	private final static int REFUSE_INVITATION_DONE = 2;

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
	private VMessageQualification vq;
	private State mState = State.DONE;
	private boolean isInRejectReasonMode = false;
	private boolean isReturnData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;

		setContentView(R.layout.crowd_invitation_activity);
		mTitleName = (TextView) findViewById(R.id.crowd_invitation_title_text);
		mNameTV = (TextView) findViewById(R.id.crowd_invitation_name);
		mNoTV = (TextView) findViewById(R.id.crowd_invitation_crowd_no);
		mCreatorTV = (TextView) findViewById(R.id.crowd_invitation_creator_tv);
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
		mAcceptedLy =  findViewById(R.id.crowd_invitation_accepted_ly);
		

		mRejectResasonLayout = findViewById(R.id.crowd_content_reject_reason_ly);
		mBoxLy = findViewById(R.id.crowd_invitation_box_ly);
		mReasonET = (EditText)findViewById(R.id.crowd_content_reject_reason_et);

		crowd = (Crowd) getIntent().getExtras().get("crowd");
		String cid  = String.valueOf(crowd.getId());
		mNoTV.setText(cid.length() > 4 ? cid.substring(4) : cid.substring(1));
		mNameTV.setText(crowd.getName());
		mBriefTV.setText(crowd.getBrief());
		mCreatorTV.setText(crowd.getCreator().getName());
		mAnnounceTV.setText(crowd.getAnnounce());
		
		Group group = GlobalHolder.getInstance().getGroupById(V2GlobalEnum.GROUP_TYPE_CROWD , crowd.getId());
		if(group != null && group.getUsers() != null)
			mMembersTV.setText(String.valueOf(group.getUsers().size()));

		CrowdGroup g = new CrowdGroup(crowd.getId(), crowd.getName(),
				crowd.getCreator(), null);
		g.setAuthType(AuthType.fromInt(crowd.getAuth()));
		vq = MessageBuilder.queryQualMessageByCrowdId(mContext, crowd.getCreator().getmUserId(), g.getmGId());
		updateView(false);

		mRejectResasonLayout.setVisibility(View.GONE);

	}

	private void handleAcceptDone() {
//		CrowdGroup g = new CrowdGroup(crowd.getId(), crowd.getName(),
//				crowd.getCreator(), null);
//		g.setBrief(crowd.getBrief());
//		g.setAnnouncement(crowd.getAnnounce());
//		GlobalHolder.getInstance().addGroupToList(GroupType.CHATING.intValue(),
//				g);
//
//		Intent i = new Intent();
//		i.setAction(PublicIntent.BROADCAST_NEW_CROWD_NOTIFICATION);
//		i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
//		i.putExtra("crowd", crowd.getId());
//		sendBroadcast(i);
        Group group = GlobalHolder.getInstance().getGroupById(V2GlobalEnum.GROUP_TYPE_CROWD , crowd.getId());
        if(group != null && group.getUsers() != null)
            mMembersTV.setText(String.valueOf(group.getUsers().size()));
		
		VMessageQualification message = MessageBuilder.queryQualMessageById(mContext, vq.getId());
		vq.setQualState(message.getQualState());
		vq.setReadState(message.getReadState());
		updateView(false);
	}

	private void handleDeclineDone() {
		CrowdGroup g = new CrowdGroup(crowd.getId(), crowd.getName(),
				crowd.getCreator(), null);
		vq.setReadState(VMessageQualification.ReadState.READ);
		vq.setQualState(VMessageQualification.QualificationState.REJECT);
		updateView(false);
		MessageBuilder.updateQualicationMessageState(crowd.getId(), crowd.getCreator().getmUserId(),
				new GroupQualicationState(Type.CROWD_INVITATION , QualificationState.REJECT , null , ReadState.UNREAD , false));
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
            }  else if (vq.getQualState() == QualificationState.WAITING_FOR_APPLY) {
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
                VMessageQualification message = MessageBuilder.queryQualMessageById(mContext, vq.getId());
                if (message.getQualState().intValue() != vq.getQualState().intValue())
                    handleAcceptDone();
                else {
                    service.acceptInvitation(crowd, new MessageListener(mLocalHandler,
                            ACCEPT_INVITATION_DONE, null));
                    ProgressUtils.showNormalWithHintProgress(mContext, true).initTimeOut();
                }
			}
		}

	};

	private OnClickListener mDeclineButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			if (!isInRejectReasonMode) {
				updateView(!isInRejectReasonMode);
			}
		}

	};

	private OnClickListener mReturnButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			if (isInRejectReasonMode) {
				updateView(!isInRejectReasonMode);
				return;
			}
			
			if(isReturnData){
				Intent intent = new Intent(CrowdInvitationActivity.this,
						MessageAuthenticationActivity.class);
				intent.putExtra("qualificationID", vq.getId());
				intent.putExtra("qualState", vq.getQualState());
				setResult(4 , intent);
			}
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
					VMessageQualification message = MessageBuilder.queryQualMessageById(mContext, vq.getId());
					if(message.getQualState().intValue() != vq.getQualState().intValue())
						handleDeclineDone();
					else{
						service.refuseInvitation(crowd, mReasonET.getEditableText().toString(), new MessageListener(
								mLocalHandler, REFUSE_INVITATION_DONE, null));
						handleDeclineDone();
//						ProgressUtils.showNormalWithHintProgress(mContext, true).initTimeOut();
					}
				}
				return;
			} else {
				Group group = GlobalHolder.getInstance().getGroupById(
						crowd.getId());
				Intent i = new Intent(PublicIntent.START_CONVERSACTION_ACTIVITY);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				i.putExtra("obj", new ConversationNotificationObject(
						new CrowdConversation(group)));
				startActivity(i);
			}
		}

	};

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
                if(jni.getResult().ordinal() == JNIResponse.Result.SUCCESS.ordinal()){
                    handleAcceptDone();
                }
				break;
			case REFUSE_INVITATION_DONE:
//				ProgressUtils.showNormalWithHintProgress(mContext, false);
//				handleDeclineDone();
				break;
			}
            ProgressUtils.showNormalWithHintProgress(mContext, false);
		}

	};

	enum State {
		DONE, UPDATING
	}

}
