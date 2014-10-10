package com.v2tech.view.group;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.service.CrowdGroupService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.Registrant;
import com.v2tech.view.conversation.MessageBuilder;
import com.v2tech.vo.Crowd;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.VMessageQualification;

public class CrowdApplicationActivity extends Activity {

	private final static int APPLY_DONE = 1;

	private TextView mNameTV;
	private TextView mCreatorTV;
	private TextView mBriefTV;
	private TextView mNoTV;
	private View mReturnButton;
	private View mApplicationButton;
	private View mButtonLy;
	private View mNotesLy;
	private View mSendButton;
	private View mMessageLy;
	private View mItemLy;
	private EditText mApplicationMessage;

	private Context mContext;

	private Crowd crowd;
	private CrowdGroupService service = new CrowdGroupService();
	private VMessageQualification vq;
	private State mState = State.DONE;
	private boolean isInApplicationMode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;

		setContentView(R.layout.crowd_application_activity);
		mNameTV = (TextView) findViewById(R.id.crowd_application_name);
		mCreatorTV = (TextView) findViewById(R.id.crowd_application_item_creator);
		mBriefTV = (TextView) findViewById(R.id.crowd_application_brief);
		mNoTV = (TextView) findViewById(R.id.crowd_application_no);
		mButtonLy = findViewById(R.id.crowd_application_button_ly);
		mNotesLy = findViewById(R.id.crowd_application_notes_ly);
		mSendButton = findViewById(R.id.crowd_application_send_button);
		mSendButton.setOnClickListener(mSendButtonListener);
		mItemLy = findViewById(R.id.crowd_application_item_ly);
		mMessageLy = findViewById(R.id.crowd_application_message_ly);
		mApplicationMessage = (EditText) findViewById(R.id.crowd_application_message_et);

		mApplicationButton = findViewById(R.id.crowd_application_button);
		mApplicationButton.setOnClickListener(mApplicationButtonListener);
		mReturnButton = findViewById(R.id.crowd_application_return_button);
		mReturnButton.setOnClickListener(mReturnButtonListener);

		crowd = (Crowd) getIntent().getExtras().get("crowd");
		mNameTV.setText(crowd.getName());
		mBriefTV.setText(crowd.getBrief());
		mCreatorTV.setText(crowd.getCreator().getName());
		mNoTV.setText(crowd.getId() + "");

		CrowdGroup g = new CrowdGroup(crowd.getId(), crowd.getName(),
				crowd.getCreator(), null);
		vq = MessageBuilder.queryQualMessageByCrowdId(mContext, GlobalHolder
				.getInstance().getCurrentUser(), g);
		updateView();

	}

	@Override
	public void onBackPressed() {
		if (isInApplicationMode) {
			isInApplicationMode = false;
			updateView();
			return;
		}
		super.onBackPressed();
	}

	private void handleApplyDone() {
		mButtonLy.setVisibility(View.GONE);
		mNotesLy.setVisibility(View.VISIBLE);
		
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mApplicationMessage.getWindowToken(), 0);
	}

	private void updateView() {
		if (isInApplicationMode) {
			mSendButton.setVisibility(View.VISIBLE);
			mMessageLy.setVisibility(View.VISIBLE);
			mItemLy.setVisibility(View.GONE);
			mApplicationButton.setVisibility(View.GONE);
		} else {
			mSendButton.setVisibility(View.GONE);
			mMessageLy.setVisibility(View.GONE);
			mItemLy.setVisibility(View.VISIBLE);
			mApplicationButton.setVisibility(View.VISIBLE);
		}
	}

	private OnClickListener mApplicationButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			synchronized (mState) {
				if (mState == State.APPLYING) {
					return;
				}
				if (crowd.getAuth() == CrowdGroup.AuthType.ALLOW_ALL.intValue()) {
					mState = State.APPLYING;
					service.applyCrowd(crowd, "", new Registrant(mLocalHandler,
							APPLY_DONE, null));
				} else if (crowd.getAuth() == CrowdGroup.AuthType.QULIFICATION
						.intValue()) {
					isInApplicationMode = true;
					updateView();
				}
			}
		}

	};

	private OnClickListener mSendButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			synchronized (mState) {

				if (mState == State.APPLYING) {
					return;
				}
				mState = State.APPLYING;
				service.applyCrowd(crowd, mApplicationMessage.getText()
						.toString(), new Registrant(mLocalHandler, APPLY_DONE,
						null));
				isInApplicationMode = false;
				updateView();
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
