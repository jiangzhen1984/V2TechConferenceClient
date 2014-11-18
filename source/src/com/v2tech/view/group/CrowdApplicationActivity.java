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
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.v2tech.R;
import com.v2tech.service.CrowdGroupService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.MessageListener;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.service.jni.JNIResponse.Result;
import com.v2tech.view.JNIService;
import com.v2tech.view.PublicIntent;
import com.v2tech.view.conversation.MessageBuilder;
import com.v2tech.vo.Crowd;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.User;
import com.v2tech.vo.VMessageQualification;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.VMessageQualificationInvitationCrowd;

/**
 * 
 * @see PublicIntent#SHOW_CROWD_APPLICATION_ACTIVITY
 * 
 * @author jiangzhen
 * 
 */
public class CrowdApplicationActivity extends Activity {

	private final static int APPLY_DONE = 1;

	private TextView mTitleTV;
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

	private boolean disableAuth;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;

		setContentView(R.layout.crowd_application_activity);
		mTitleTV = (TextView) findViewById(R.id.crowd_application_title);
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

		disableAuth = getIntent().getBooleanExtra("authdisable", false);

		mNameTV.setText(crowd.getName());
		mBriefTV.setText(crowd.getBrief());
		mCreatorTV.setText(crowd.getCreator().getName());

		String cid = String.valueOf(crowd.getId());
		mNoTV.setText(cid.length() > 4 ? cid.substring(4) : cid.substring(1));

		CrowdGroup g = new CrowdGroup(crowd.getId(), crowd.getName(),
				crowd.getCreator(), null);
        if(g.getOwnerUser() != null) {
            User user = g.getOwnerUser();
            vq = MessageBuilder.queryQualMessageByCrowdId(mContext, user, g);
        }
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
			mTitleTV.setText(R.string.crowd_application_qualification);

			Animation out = AnimationUtils.loadAnimation(mContext,
					R.animator.left_in);
			mMessageLy.startAnimation(out);
			Animation in = AnimationUtils.loadAnimation(mContext,
					R.animator.left_out);
			mItemLy.startAnimation(in);

		} else {
			mSendButton.setVisibility(View.GONE);
			mMessageLy.setVisibility(View.GONE);
            mMessageLy.setVisibility(View.GONE);
			mItemLy.setVisibility(View.VISIBLE);
			mTitleTV.setText(R.string.crowd_application_title);
            if(vq != null){
                VMessageQualification.QualificationState state = vq.getQualState();
                if(state == VMessageQualification.QualificationState.WAITING_FOR_APPLY){
                    mApplicationButton.setVisibility(View.GONE);
                    mNotesLy.setVisibility(View.VISIBLE);
                    mTitleTV.setText(R.string.crowd_applicant_invite_title);
                } else if(state == VMessageQualification.QualificationState.BE_REJECT){
                	mApplicationButton.setVisibility(View.VISIBLE);
                    mNotesLy.setVisibility(View.GONE);
                	mTitleTV.setText(R.string.crowd_applicant_invite_title);
                }
                else {
                    mApplicationButton.setVisibility(View.VISIBLE);
                    mNotesLy.setVisibility(View.GONE);
                }
            }
            else {
                mNotesLy.setVisibility(View.GONE);
                mApplicationButton.setVisibility(View.VISIBLE);
            }

			Animation out = AnimationUtils.loadAnimation(mContext,
					R.animator.right_in);
			mItemLy.startAnimation(out);
			Animation in = AnimationUtils.loadAnimation(mContext,
					R.animator.right_out);
			mMessageLy.startAnimation(in);
		}
	}

	private OnClickListener mApplicationButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			synchronized (mState) {

				if (mState == State.APPLYING) {
					return;
				}
				if (disableAuth) {
					mState = State.APPLYING;
					service.applyCrowd(crowd, "", new MessageListener(
							mLocalHandler, APPLY_DONE, null));
				} else {
					if (crowd.getAuth() == CrowdGroup.AuthType.ALLOW_ALL
							.intValue()) {
						mState = State.APPLYING;
						service.applyCrowd(crowd, "", new MessageListener(
								mLocalHandler, APPLY_DONE, null));
					} else if (crowd.getAuth() == CrowdGroup.AuthType.QULIFICATION
							.intValue()) {
						isInApplicationMode = true;
						updateView();
					} else if (crowd.getAuth() == CrowdGroup.AuthType.NEVER
							.intValue()) {
                        Toast.makeText(
                                mContext,
                                R.string.crowd_application_sent_result_successful,
                                Toast.LENGTH_SHORT).show();
                        mLocalHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext,
                                        R.string.crowd_applicant_invite_never,
                                        Toast.LENGTH_SHORT).show();
                            }
                        } , 1000);

                        if (vq != null) {
                            vq.setReadState(VMessageQualification.ReadState.READ);
                            vq.setQualState(VMessageQualification.QualificationState.BE_REJECT);
                            MessageBuilder.updateQualicationMessage(mContext, vq);
                        }
                        else{
                            VMessageQualification quaion = MessageBuilder.queryQualMessageByCrowdId(mContext ,
                                   crowd.getCreator().getmUserId() , crowd.getId());
                            if(quaion == null) {
                                CrowdGroup g = new CrowdGroup(crowd.getId(),
                                        crowd.getName(), crowd.getCreator(), null);
                                g.setBrief(crowd.getBrief());
                                vq = new VMessageQualificationInvitationCrowd(g, GlobalHolder.getInstance().getCurrentUser());
                                vq.setReadState(VMessageQualification.ReadState.READ);
                                vq.setQualState(VMessageQualification.QualificationState.BE_REJECT);
                                MessageBuilder.saveQualicationMessage(mContext, vq);
                            }
                        }
					}
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
						.toString(), new MessageListener(mLocalHandler,
						APPLY_DONE, null));
				isInApplicationMode = false;
				updateView();
                if (vq != null) {
                    vq.setReadState(VMessageQualification.ReadState.READ);
                    vq.setQualState(VMessageQualification.QualificationState.WAITING_FOR_APPLY);
                    MessageBuilder.updateQualicationMessage(mContext, vq);
                }
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
				JNIResponse response = (JNIResponse) msg.obj;
				if (response.getResult() == Result.SUCCESS) {
                    if (mContext != null) {
                        Toast.makeText(
                                mContext,
                                R.string.crowd_application_sent_result_successful,
                                Toast.LENGTH_SHORT).show();
                        if(crowd.getAuth() == CrowdGroup.AuthType.ALLOW_ALL
                                .intValue()){
                            CrowdGroup g = new CrowdGroup(crowd.getId(),
                                    crowd.getName(), crowd.getCreator(), null);
                            g.setBrief(crowd.getBrief());
                            g.setAnnouncement(crowd.getAnnounce());
                            GlobalHolder.getInstance().addGroupToList(
                                    GroupType.CHATING.intValue(), g);

                            Intent i = new Intent();
                            i.setAction(PublicIntent.BROADCAST_NEW_CROWD_NOTIFICATION);
                            i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
                            i.putExtra("crowd", crowd.getId());
                            sendBroadcast(i);
                            mLocalHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(mContext,
                                            R.string.crowd_applicant_invite_finish,
                                            Toast.LENGTH_SHORT).show();
                                }
                            } , 1000);
                            if (vq != null) {
                                vq.setReadState(VMessageQualification.ReadState.READ);
                                vq.setQualState(VMessageQualification.QualificationState.BE_ACCEPTED);
//						        MessageBuilder.updateQualicationMessage(mContext, vq);
                            }
//                        else{
//                           vq = new VMessageQualificationInvitationCrowd(g , GlobalHolder.getInstance().getCurrentUser());
//                           vq.setReadState(VMessageQualification.ReadState.READ);
//                          vq.setQualState(VMessageQualification.QualificationState.BE_ACCEPTED);
//                        }
                        }
                    }
				} else if (response.getResult() == Result.FAILED) {

				}
				handleApplyDone();
				break;
			}
		}

	};

	enum State {
		DONE, APPLYING
	}

}
