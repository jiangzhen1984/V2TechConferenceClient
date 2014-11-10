package com.v2tech.view.group;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.v2tech.R;
import com.v2tech.service.CrowdGroupService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.MessageListener;
import com.v2tech.util.ProgressUtils;
import com.v2tech.util.V2Toast;
import com.v2tech.view.conversation.MessageAuthenticationActivity;
import com.v2tech.view.conversation.MessageBuilder;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.GroupQualicationState;
import com.v2tech.vo.User;
import com.v2tech.vo.VMessageQualification;
import com.v2tech.vo.VMessageQualification.Type;
import com.v2tech.vo.VMessageQualificationApplicationCrowd;
import com.v2tech.vo.VMessageQualification.QualificationState;

/**
 * Intent key:<br>
 * cid: user applyed crowd Id<br>
 * aid: applicant user id<br>
 * mid: applicantion message id<br>
 * @author jiangzhen
 *
 */
public class CrowdApplicantDetailActivity extends Activity {

	private final static int ACCEPT_INVITATION_DONE = 1;
	private final static int REFUSE_INVITATION_DONE = 2;

	private Context mContext;
	
	private TextView mTitleText;
	private View mChildButtonLy;
	
	private View mInviteButton;
	private View mReturnButton;
	private View mAcceptButton;
	private View mDeclineButton;

	private View mButtonLayout;
	private View mNotesLayout;

	private TextView mNotesTV;

	private CrowdGroup crowd;
	private User applicant;
	private VMessageQualificationApplicationCrowd msg;
	private CrowdGroupService service = new CrowdGroupService();
	private Handler handler = new Handler();
	private CrowdGroupService cg = new CrowdGroupService();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.crowd_applicant_detail);
		mContext = this;
		
		mTitleText = (TextView) findViewById(R.id.crowd_applicant_title_text);
		mChildButtonLy = findViewById(R.id.crowd_application_ly);
		
		
		mInviteButton = findViewById(R.id.crowd_application_invite_button);
		mInviteButton.setOnClickListener(mInviteButtonListener);
		mAcceptButton = findViewById(R.id.crowd_application_accept_button);
		mAcceptButton.setOnClickListener(mAcceptButtonListener);
		mDeclineButton = findViewById(R.id.crowd_application_decline_button);
		mDeclineButton.setOnClickListener(mDeclineButtonListener);
		mReturnButton = findViewById(R.id.crowd_applicant_return_button);
		mReturnButton.setOnClickListener(mReturnButtonListener);

		mButtonLayout = findViewById(R.id.crowd_application_button_ly);
		mNotesLayout = findViewById(R.id.crowd_application_notes_ly);
		mNotesTV = (TextView) findViewById(R.id.crowd_application_notes);

		long crowdId = getIntent().getLongExtra("cid", 0);
		long applicationId = getIntent().getLongExtra("aid", 0);
		long mid = getIntent().getLongExtra("mid", 0);
		
		crowd = (CrowdGroup) GlobalHolder.getInstance().getGroupById(crowdId);
		applicant = GlobalHolder.getInstance().getUser(applicationId);
		msg = (VMessageQualificationApplicationCrowd)MessageBuilder.queryQualMessageById(this, mid);
		if (applicant != null) {
			updateView();
		} else {
			throw new RuntimeException("Can not get applicant information");
		}
		
		msg.setReadState(VMessageQualification.ReadState.READ);
		MessageBuilder.updateQualicationMessage(this, msg);
	}

	private void updateView() {
		((TextView)findViewById(R.id.crowd_applicant_name)).setText(applicant.getName());
		((TextView)findViewById(R.id.crowd_applicant_signature)).setText(applicant.getSignature());		
		((TextView)findViewById(R.id.contact_user_detail_title_tv)).setText(applicant.getJob());
		((TextView)findViewById(R.id.contact_user_detail_address_tv)).setText(applicant.getAddress());
		((TextView)findViewById(R.id.contact_user_detail_email_tv)).setText(applicant.getmEmail());
		((TextView)findViewById(R.id.contact_user_detail_cell_phone_tv)).setText(applicant.getMobile());
		((TextView)findViewById(R.id.contact_user_detail_telephone_tv)).setText(applicant.getTelephone());
		((TextView)findViewById(R.id.contact_user_detail_fax_tv)).setText(applicant.getFax());
		
		if(TextUtils.isEmpty(msg.getApplyReason()))
			((TextView)findViewById(R.id.crowd_application_additional_msg)).setText("附加消息: 无");
		else
			((TextView)findViewById(R.id.crowd_application_additional_msg)).setText("附加消息: " + msg.getApplyReason());
		
		if (msg.getQualState() == VMessageQualification.QualificationState.ACCEPTED) {
			mButtonLayout.setVisibility(View.GONE);
			mChildButtonLy.setVisibility(View.VISIBLE);
			mInviteButton.setVisibility(View.GONE);
			
			mNotesLayout.setVisibility(View.VISIBLE);
			mNotesTV.setText(R.string.crowd_invitation_accept_notes);
		} else if (msg.getQualState() == VMessageQualification.QualificationState.REJECT) {
			mButtonLayout.setVisibility(View.GONE);
			mChildButtonLy.setVisibility(View.VISIBLE);
			mInviteButton.setVisibility(View.GONE);
			
			mNotesLayout.setVisibility(View.VISIBLE);
			mNotesTV.setText(R.string.crowd_invitation_reject_notes);
		} else if((msg.getQualState() == VMessageQualification.QualificationState.BE_REJECT)
				|| (msg.getQualState() == VMessageQualification.QualificationState.WAITING)){
			mButtonLayout.setVisibility(View.VISIBLE);
			mNotesLayout.setVisibility(View.GONE);
			mChildButtonLy.setVisibility(View.GONE);
			mInviteButton.setVisibility(View.VISIBLE);
			mTitleText.setText(R.string.crowd_applicant_invite_title);
		} else if(msg.getQualState() == VMessageQualification.QualificationState.BE_ACCEPTED){
			mButtonLayout.setVisibility(View.GONE);
			mChildButtonLy.setVisibility(View.VISIBLE);
			mInviteButton.setVisibility(View.GONE);
			
			mNotesLayout.setVisibility(View.VISIBLE);
			mNotesTV.setText(R.string.crowd_invitation_joined);
			mTitleText.setText(R.string.crowd_applicant_invite_title);
		} 
	}

	private void handleAcceptDone() {
		mButtonLayout.setVisibility(View.GONE);
		mNotesLayout.setVisibility(View.VISIBLE);
		mNotesTV.setText(R.string.crowd_application_accepted);
		crowd.addUserToGroup(applicant);
	}
	
	private void handleDeclineDone() {
		mButtonLayout.setVisibility(View.GONE);
		mNotesLayout.setVisibility(View.VISIBLE);
		mNotesTV.setText(R.string.crowd_application_rejected);
	}
	
	private OnClickListener mAcceptButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			VMessageQualification message = MessageBuilder.queryQualMessageById(mContext, msg.getId());
			if(message.getQualState().intValue() != msg.getQualState().intValue())
				handleAcceptDone();
			else{
				service.acceptApplication(crowd, applicant, new MessageListener(
						mLocalHandler, ACCEPT_INVITATION_DONE, null));
				ProgressUtils.showNormalWithHintProgress(mContext, true).initTimeOut();
			}
		}

	};

	private OnClickListener mDeclineButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			VMessageQualification message = MessageBuilder.queryQualMessageById(mContext, msg.getId());
			if(message.getQualState().intValue() != msg.getQualState().intValue())
				handleAcceptDone();
			else{
				service.refuseApplication(crowd, applicant, "", new MessageListener(
						mLocalHandler, REFUSE_INVITATION_DONE, null));
				ProgressUtils.showNormalWithHintProgress(mContext, true).initTimeOut();
			}
		}

	};
	
	private OnClickListener mInviteButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			
			if(msg.getQualState() == VMessageQualification.QualificationState.WAITING){
				V2Toast.makeText(mContext, R.string.crowd_applicant_invite_hint, Toast.LENGTH_SHORT).show();
				return ;
			}
			
			List<User> newMembers = new ArrayList<User>();
			newMembers.add(applicant);
			cg.inviteMember(crowd, newMembers, null);
			
//			MessageBuilder.updateQualicationMessageState(crowd.getmGId() , new GroupQualicationState(Type.CROWD_APPLICATION,
//					QualificationState.WAITING, null));
			onBackPressed();
		}

	};

	private OnClickListener mReturnButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			Intent intent = new Intent(mContext,
					MessageAuthenticationActivity.class);
			intent.putExtra("qualificationID", msg.getId());
			intent.putExtra("qualState", msg.getQualState());
			setResult(4 , intent);
			
			onBackPressed();
		}

	};

	private Handler mLocalHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case ACCEPT_INVITATION_DONE:
				ProgressUtils.showNormalWithHintProgress(mContext, false);
				handleAcceptDone();
				break;
			case REFUSE_INVITATION_DONE:
				handleDeclineDone();
				ProgressUtils.showNormalWithHintProgress(mContext, false);
				break;

			}
		}
	};

}
