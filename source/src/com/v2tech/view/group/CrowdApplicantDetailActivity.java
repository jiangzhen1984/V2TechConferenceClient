package com.v2tech.view.group;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.service.CrowdGroupService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.Registrant;
import com.v2tech.view.conversation.MessageBuilder;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.User;
import com.v2tech.vo.VMessageQualification;
import com.v2tech.vo.VMessageQualificationApplicationCrowd;

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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.crowd_applicant_detail);

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
	}

	private void handleAcceptDone() {
		crowd.addUserToGroup(applicant);
	}

	private OnClickListener mAcceptButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			service.acceptApplication(crowd, applicant, new Registrant(
					mLocalHandler, ACCEPT_INVITATION_DONE, null));
			mButtonLayout.setVisibility(View.GONE);
			mNotesLayout.setVisibility(View.VISIBLE);
			mNotesTV.setText(R.string.crowd_application_accepted);
		}

	};

	private OnClickListener mDeclineButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			service.refuseApplication(crowd, applicant, "", new Registrant(
					mLocalHandler, REFUSE_INVITATION_DONE, null));
			mButtonLayout.setVisibility(View.GONE);
			mNotesLayout.setVisibility(View.VISIBLE);
			mNotesTV.setText(R.string.crowd_application_accepted);
		}

	};

	private OnClickListener mReturnButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			onBackPressed();
		}

	};

	private Handler mLocalHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case ACCEPT_INVITATION_DONE:
				handleAcceptDone();
				break;
			case REFUSE_INVITATION_DONE:
				break;

			}
		}

	};

}
