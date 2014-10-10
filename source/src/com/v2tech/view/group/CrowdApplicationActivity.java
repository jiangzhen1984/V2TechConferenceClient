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
import com.v2tech.view.JNIService;
import com.v2tech.view.PublicIntent;
import com.v2tech.view.conversation.MessageBuilder;
import com.v2tech.vo.Crowd;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.VMessageQualification;

public class CrowdApplicationActivity extends Activity {

	private final static int APPLY_DONE = 1;

	private TextView mNameTV;
	private TextView mCreatorTV;
	private TextView mBriefTV;
	private View mReturnButton;
	private View mApplicationButton;

	private Context mContext;

	private Crowd crowd;
	private CrowdGroupService service = new CrowdGroupService();
	private VMessageQualification vq;
	private State mState = State.DONE;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;

		setContentView(R.layout.crowd_application_activity);
		mNameTV = (TextView) findViewById(R.id.crowd_invitation_name);
		mCreatorTV = (TextView) findViewById(R.id.crowd_invitation_creator_tv);
		mBriefTV = (TextView) findViewById(R.id.crowd_invitation_brief);

		mApplicationButton = findViewById(R.id.crowd_application_button);
		mApplicationButton.setOnClickListener(mApplicationButtonListener);
		mReturnButton = findViewById(R.id.crowd_invitation_return_button);
		mReturnButton.setOnClickListener(mReturnButtonListener);

		crowd = (Crowd) getIntent().getExtras().get("crowd");
		mNameTV.setText(crowd.getName());
		mBriefTV.setText(crowd.getBrief());
		mCreatorTV.setText(crowd.getCreator().getName());

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

	private void handleApplyDone() {
		
	}

	private void updateView() {

	}

	private OnClickListener mApplicationButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			synchronized (mState) {
				if (mState == State.APPLYING) {
					return;
				}
				
				mState = State.APPLYING;
				//TODO send application request
			}
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
			synchronized (mState) {
				mState = State.DONE;
			}
			switch (msg.what) {
			case APPLY_DONE:
				handleApplyDone();
				break;
			}
		}

	};

	enum State {
		DONE, APPLYING
	}

}
