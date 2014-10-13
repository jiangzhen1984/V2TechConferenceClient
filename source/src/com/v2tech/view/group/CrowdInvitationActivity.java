package com.v2tech.view.group;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.service.CrowdGroupService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.Registrant;
import com.v2tech.view.JNIService;
import com.v2tech.view.PublicIntent;
import com.v2tech.view.bo.ConversationNotificationObject;
import com.v2tech.view.conversation.MessageBuilder;
import com.v2tech.vo.Crowd;
import com.v2tech.vo.CrowdConversation;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.VMessageQualification;

public class CrowdInvitationActivity extends Activity {

	private final static int ACCEPT_INVITATION_DONE = 1;
	private final static int REFUSE_INVITATION_DONE = 2;

	private TextView mNameTV;
	private TextView mNoTV;
	private TextView mCreatorTV;
	private TextView mBriefTV;
	private TextView mAnnounceTV;
	
	private View mReturnButton;
	private View mAcceptButton;
	private View mDeclineButton;
	private View mButtonLayout;
	private View mNotesLayout;
	private View mSendMsgButton;
	private TextView mNotesTV;

	private Context mContext;

	private Crowd crowd;
	private CrowdGroupService service = new CrowdGroupService();
	private VMessageQualification vq;
	private State mState = State.DONE;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		
		setContentView(R.layout.crowd_invitation_activity);
		mNameTV = (TextView) findViewById(R.id.crowd_invitation_name);
		mNoTV = (TextView) findViewById(R.id.crowd_invitation_crowd_no);
		mCreatorTV = (TextView) findViewById(R.id.crowd_invitation_creator_tv);
		mBriefTV = (TextView) findViewById(R.id.crowd_invitation_brief);
		mAnnounceTV = (TextView) findViewById(R.id.crowd_invitation_announcement);
		
		
		mAcceptButton = findViewById(R.id.crowd_invitation_accept_button);
		mAcceptButton.setOnClickListener(mAcceptButtonListener);
		mDeclineButton = findViewById(R.id.crowd_invitation_decline_button);
		mDeclineButton.setOnClickListener(mDeclineButtonListener);
		mReturnButton = findViewById(R.id.crowd_invitation_return_button);
		mReturnButton.setOnClickListener(mReturnButtonListener);
		mSendMsgButton= findViewById(R.id.crowd_invitation_send_msg_button);
		mSendMsgButton.setOnClickListener(mSendMsgButtonListener);
		
		mButtonLayout = findViewById(R.id.crowd_invitation_button_ly);
		mNotesLayout = findViewById(R.id.crowd_invitation_notes_ly);
		mNotesTV = (TextView) findViewById(R.id.crowd_invitation_notes);

		crowd = (Crowd) getIntent().getExtras().get("crowd");
		mNoTV.setText(crowd.getId()+"");
		mNameTV.setText(crowd.getName());
		mBriefTV.setText(crowd.getBrief());
		mCreatorTV.setText(crowd.getCreator().getName());
		mAnnounceTV.setText(crowd.getAnnounce());
		
		CrowdGroup g = new CrowdGroup(crowd.getId(), crowd.getName(),
				crowd.getCreator(), null);
		vq = MessageBuilder.queryQualMessageByCrowdId(mContext, GlobalHolder
				.getInstance().getCurrentUser(), g);
		updateView();


	}

	private void handleAcceptDone() {
		CrowdGroup g = new CrowdGroup(crowd.getId(), crowd.getName(),
				crowd.getCreator(), null);
		g.setBrief(crowd.getBrief());
		g.setAnnouncement(crowd.getAnnounce());
		GlobalHolder.getInstance().addGroupToList(GroupType.CHATING.intValue(),
				g);

		Intent i = new Intent();
		i.setAction(PublicIntent.BROADCAST_NEW_CROWD_NOTIFICATION);
		i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		i.putExtra("crowd", crowd.getId());
		sendBroadcast(i);

		vq.setReadState(VMessageQualification.ReadState.READ);
		vq.setQualState(VMessageQualification.QualificationState.ACCEPTED);
		MessageBuilder.updateQualicationMessage(mContext, vq);

		updateView();
	}

	private void handleDeclineDone() {
		CrowdGroup g = new CrowdGroup(crowd.getId(), crowd.getName(),
				crowd.getCreator(), null);
		vq.setReadState(VMessageQualification.ReadState.READ);
		vq.setQualState(VMessageQualification.QualificationState.REJECT);
		MessageBuilder.updateQualicationMessage(mContext, vq);
		updateView();
	}

	private void updateView() {
		if (vq.getQualState() == VMessageQualification.QualificationState.ACCEPTED) {
			mButtonLayout.setVisibility(View.GONE);
			mNotesLayout.setVisibility(View.VISIBLE);
			mSendMsgButton.setVisibility(View.VISIBLE);
			mNotesTV.setText(R.string.crowd_invitation_accept_notes);
		} else if (vq.getQualState() == VMessageQualification.QualificationState.REJECT) {
			mButtonLayout.setVisibility(View.GONE);
			mNotesLayout.setVisibility(View.VISIBLE);
			mSendMsgButton.setVisibility(View.VISIBLE);
			mNotesTV.setText(R.string.crowd_invitation_reject_notes);
		} else {
			mSendMsgButton.setVisibility(View.GONE);
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
				service.acceptInvitation(crowd, new Registrant(mLocalHandler,
						ACCEPT_INVITATION_DONE, null));
			}
		}

	};

	private OnClickListener mDeclineButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			synchronized (mState) {
				if (mState == State.UPDATING) {
					return;
				}
				mState = State.UPDATING;
				service.refuseInvitation(crowd, new Registrant(mLocalHandler,
						REFUSE_INVITATION_DONE, null));
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
			Group group = GlobalHolder.getInstance().getGroupById(crowd.getId());
			Intent i = new Intent(PublicIntent.START_CONVERSACTION_ACTIVITY);
			i.addCategory(PublicIntent.DEFAULT_CATEGORY);
			i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			i.putExtra("obj", new ConversationNotificationObject(new CrowdConversation(group)));
			startActivity(i);
		}

	};

	private Handler mLocalHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			synchronized (mState) {
				mState = State.DONE;
			}
			switch (msg.what) {
			case ACCEPT_INVITATION_DONE:
				handleAcceptDone();
				break;
			case REFUSE_INVITATION_DONE:
				handleDeclineDone();
				break;

			}
		}

	};

	enum State {
		DONE, UPDATING
	}

}
