package com.v2tech.view.group;

import android.app.Activity;
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
import com.v2tech.vo.Crowd;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.User;

public class CrowdApplicantDetailActivity extends Activity {

	private final static int ACCEPT_INVITATION_DONE = 1;
	private final static int REFUSE_INVITATION_DONE = 2;

	private TextView mNameTV;
	private TextView mCreatorTV;
	private TextView mBriefTV;
	
	private View mReturnButton;
	private View mAcceptButton;
	private View mDeclineButton;
	private View mButtonLayout;
	private View mNotesLayout;
	private TextView mNotesTV;

	private Crowd crowd;
	private CrowdGroupService service = new CrowdGroupService();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.crowd_applicant_detail);
		mNameTV = (TextView) findViewById(R.id.crowd_invitation_name);
		mCreatorTV = (TextView) findViewById(R.id.crowd_invitation_creator_tv);
		mBriefTV = (TextView) findViewById(R.id.crowd_invitation_brief);
		
		mAcceptButton = findViewById(R.id.crowd_invitation_accept_button);
		mAcceptButton.setOnClickListener(mAcceptButtonListener);
		mDeclineButton = findViewById(R.id.crowd_invitation_decline_button);
		mDeclineButton.setOnClickListener(mDeclineButtonListener);
		mReturnButton = findViewById(R.id.crowd_invitation_return_button);
		mReturnButton.setOnClickListener(mReturnButtonListener);

		mButtonLayout = findViewById(R.id.crowd_invitation_button_ly);
		mNotesLayout = findViewById(R.id.crowd_invitation_notes_ly);
		mNotesTV = (TextView) findViewById(R.id.crowd_invitation_notes);

		crowd = (Crowd) getIntent().getExtras().get("crowd");
		mNameTV.setText(crowd.getName());
		mBriefTV.setText(crowd.getBrief());
		mCreatorTV.setText(crowd.getCreator().getName());

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
	}

	private OnClickListener mAcceptButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			// TODO get user
			service.acceptApplication((CrowdGroup)
					GlobalHolder.getInstance().getGroupById(
							GroupType.CHATING.intValue(), crowd.getId()),
					new User(1), new Registrant(mLocalHandler,
							ACCEPT_INVITATION_DONE, null));
			mButtonLayout.setVisibility(View.GONE);
			mNotesLayout.setVisibility(View.VISIBLE);
			mNotesTV.setText(R.string.crowd_invitation_accept_notes);
		}

	};

	private OnClickListener mDeclineButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			// TODO get user and reason
			service.refuseApplication((CrowdGroup)
					GlobalHolder.getInstance().getGroupById(
							GroupType.CHATING.intValue(), crowd.getId()),
					new User(1), "", new Registrant(mLocalHandler,
							REFUSE_INVITATION_DONE, null));
			mButtonLayout.setVisibility(View.GONE);
			mNotesLayout.setVisibility(View.VISIBLE);
			mNotesTV.setText(R.string.crowd_invitation_reject_notes);
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
