package com.bizcom.vc.activity.crow;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.V2.jni.util.V2Log;
import com.bizcom.db.provider.VerificationProvider;
import com.bizcom.request.BitmapManager;
import com.bizcom.request.CrowdGroupService;
import com.bizcom.request.MessageListener;
import com.bizcom.request.BitmapManager.BitmapChangedListener;
import com.bizcom.request.jni.JNIResponse;
import com.bizcom.util.ProgressUtils;
import com.bizcom.util.V2Toast;
import com.bizcom.vc.activity.message.MessageAuthenticationActivity;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vo.CrowdGroup;
import com.bizcom.vo.GroupQualicationState;
import com.bizcom.vo.User;
import com.bizcom.vo.VMessageQualification;
import com.bizcom.vo.VMessageQualificationApplicationCrowd;
import com.bizcom.vo.VMessageQualification.QualificationState;
import com.bizcom.vo.VMessageQualification.ReadState;
import com.bizcom.vo.VMessageQualification.Type;
import com.v2tech.R;

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
	
	private ImageView headIcon;
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
	private CrowdGroupService cg = new CrowdGroupService();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.crowd_applicant_detail);
		mContext = this;
		BitmapManager.getInstance().registerBitmapChangedListener(listener);
		headIcon = (ImageView) findViewById(R.id.contact_user_detail_head_icon);
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
		msg = (VMessageQualificationApplicationCrowd)VerificationProvider.queryCrowdQualMessageById(mid);
		if (applicant != null) {
			updateView();
		} else {
			throw new RuntimeException("Can not get applicant information");
		}
		
		msg.setReadState(VMessageQualification.ReadState.READ);
		VerificationProvider.updateCrowdQualicationMessage(msg);
	}
	
	@Override
	protected void onDestroy() {
		BitmapManager.getInstance().unRegisterBitmapChangedListener(listener);
		super.onDestroy();
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
		
		if (applicant.getAvatarBitmap() != null) {
			headIcon.setImageBitmap(applicant.getAvatarBitmap());
		} else {
			headIcon.setImageResource(R.drawable.avatar);
		}
		
		if(TextUtils.isEmpty(msg.getApplyReason()))
			((TextView)findViewById(R.id.crowd_application_additional_msg)).setText("附加消息: 无");
		else
			((TextView)findViewById(R.id.crowd_application_additional_msg)).setText("附加消息: " + msg.getApplyReason());
		
		if (msg.getQualState() == VMessageQualification.QualificationState.ACCEPTED) {
			mButtonLayout.setVisibility(View.GONE);
			mChildButtonLy.setVisibility(View.VISIBLE);
			mInviteButton.setVisibility(View.GONE);
			
			mNotesLayout.setVisibility(View.VISIBLE);
			mNotesTV.setText(R.string.crowd_application_accepted);
			mTitleText.setText(R.string.crowd_applicant_title);
		} else if (msg.getQualState() == VMessageQualification.QualificationState.REJECT) {
			mButtonLayout.setVisibility(View.GONE);
			mChildButtonLy.setVisibility(View.VISIBLE);
			mInviteButton.setVisibility(View.GONE);
			
			mNotesLayout.setVisibility(View.VISIBLE);
			mNotesTV.setText(R.string.crowd_application_rejected);
			mTitleText.setText(R.string.crowd_applicant_title);
		} else if(msg.getQualState() == VMessageQualification.QualificationState.BE_REJECT){
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
		} else if(msg.getQualState() == VMessageQualification.QualificationState.WAITING){
			mButtonLayout.setVisibility(View.VISIBLE);
			mChildButtonLy.setVisibility(View.VISIBLE);
			mInviteButton.setVisibility(View.GONE);
			mNotesLayout.setVisibility(View.GONE);
			
			mTitleText.setText(R.string.crowd_applicant_title);
		}
	}
	
	private BitmapChangedListener listener = new BitmapChangedListener() {
		
		@Override
		public void notifyAvatarChanged(User user, Bitmap bm) {
			if(bm != null)
				headIcon.setImageBitmap(bm);
			else
				headIcon.setImageResource(R.drawable.avatar);
		}
	};

	private void handleAcceptDone() {
		mButtonLayout.setVisibility(View.GONE);
		mNotesLayout.setVisibility(View.VISIBLE);
		mNotesTV.setText(R.string.crowd_application_accepted);
		crowd.addUserToGroup(applicant);
		
		msg.setQualState(QualificationState.ACCEPTED);
		msg.setReadState(ReadState.READ);
	}
	
	private void handleDeclineDone() {
		mButtonLayout.setVisibility(View.GONE);
		mNotesLayout.setVisibility(View.VISIBLE);
		mNotesTV.setText(R.string.crowd_application_rejected);
		
		msg.setQualState(QualificationState.REJECT);
		msg.setReadState(ReadState.READ);
		GroupQualicationState state = new GroupQualicationState(Type.CROWD_APPLICATION,
				QualificationState.REJECT, null , ReadState.READ  , true);
		state.isUpdateTime = false;
		VerificationProvider.updateCrowdQualicationMessageState(msg.getId() , state);
	}
	
	private OnClickListener mAcceptButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			VMessageQualification message = VerificationProvider.queryCrowdQualMessageById(msg.getId());
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
//			VMessageQualification message = MessageBuilder.queryQualMessageById(mContext, msg.getId());
//			if(message.getQualState().intValue() != msg.getQualState().intValue())
//				handleAcceptDone();
//			else{
//				ProgressUtils.showNormalWithHintProgress(mContext, true).initTimeOut();
//			}
            service.refuseApplication(crowd, applicant, "", new MessageListener(
                    mLocalHandler, REFUSE_INVITATION_DONE, null));
            handleDeclineDone();
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
			saveQualication(applicant);
			cg.inviteMember(crowd, newMembers, null);
			
			onBackPressed();
		}

	};
	
	private void saveQualication(User user){

		if(user == null){
			V2Log.d("CrowdApplicationDetailActivity  --> Save VMessageQualification Cache Object failed , "
					+ "Because given user object is null");
			return ;
		}
		
		if(crowd == null)
			crowd = new CrowdGroup(0, "", null);
		
		long waitMessageExist = VerificationProvider.queryCrowdInviteWaitingQualMessageById(user.getmUserId());
		if (waitMessageExist != -1) {
			V2Log.d("CrowdApplicationDetailActivity  --> Save VMessageQualification Cache Object failed , "
					+ "Because already exist in database...groupID is : " + crowd.getmGId() + " userID is : " + user.getmUserId());
			return ;
		}
		
		if(crowd == null)
			crowd = new CrowdGroup(0, "", null);
		VMessageQualificationApplicationCrowd crowdQuion = new VMessageQualificationApplicationCrowd(
				crowd, user);
		crowdQuion.setmTimestamp(new Date());
		Uri uri = VerificationProvider.saveQualicationMessage(crowdQuion , true);
		if (uri != null){
			V2Log.d("CrowdCreateActivity  --> Save VMessageQualification Cache Object Successfully , "
					+ "groupID is : " + crowd.getmGId() + " userID is : " + user.getmUserId() + ""
							+ " database id is : " + Long.parseLong(uri.getLastPathSegment()) 
							+ " URI is : " + uri.toString());
		}
		else{
			Toast.makeText(mContext, "邀请群成员失败", 0).show();
			V2Log.d("CrowdCreateActivity  --> Save VMessageQualification Cache Object failed , "
					+ "the Uri is null...groupID is : " + crowd.getmGId() + " userID is : " + user.getmUserId());
		}
	}

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
                JNIResponse jni = (JNIResponse) msg.obj;
                if(jni.getResult().ordinal() == JNIResponse.Result.SUCCESS.ordinal()){
                    handleAcceptDone();
                }
				break;
			case REFUSE_INVITATION_DONE:
//				handleDeclineDone();
//				ProgressUtils.showNormalWithHintProgress(mContext, false);
				break;

			}
            ProgressUtils.showNormalWithHintProgress(mContext, false);
		}
	};

}
