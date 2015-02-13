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
import com.bizcom.bo.GroupUserObject;
import com.bizcom.db.provider.VerificationProvider;
import com.bizcom.request.CrowdGroupService;
import com.bizcom.request.MessageListener;
import com.bizcom.request.jni.JNIResponse;
import com.bizcom.request.jni.JNIResponse.Result;
import com.bizcom.vc.activity.message.MessageAuthenticationActivity;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.application.PublicIntent;
import com.bizcom.vc.service.JNIService;
import com.bizcom.vo.Crowd;
import com.bizcom.vo.CrowdGroup;
import com.bizcom.vo.User;
import com.bizcom.vo.VMessageQualification;
import com.bizcom.vo.VMessageQualificationInvitationCrowd;
import com.bizcom.vo.VMessageQualification.QualificationState;
import com.v2tech.R;

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
	private TextView mNotes;
	private TextView mRefuseTV;
	private View mReturnButton;
	private View mApplicationButton;
	private View mNotesLy;
	private View mSendButton;
	private View mMessageLy;
	private View mRefuseLy;
	private View mItemLy;
	private EditText mApplicationMessage;

	private Context mContext;

	private Crowd crowd;
	private CrowdGroupService service = new CrowdGroupService();
	private CrowdApplyBroadcast applyReceiver;
	private VMessageQualification vq;
	private State mState = State.DONE;
	private boolean isInApplicationMode;

	private boolean disableAuth;
	private boolean isReturnData;
	private boolean shieldScreen;
	private boolean isFailed;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;

		setContentView(R.layout.crowd_application_activity);
		initReceiver();

		mTitleTV = (TextView) findViewById(R.id.crowd_application_title);
		mNameTV = (TextView) findViewById(R.id.crowd_application_name);
		mCreatorTV = (TextView) findViewById(R.id.crowd_application_item_creator);
		mBriefTV = (TextView) findViewById(R.id.crowd_application_brief);
		mNoTV = (TextView) findViewById(R.id.crowd_application_no);
		mNotesLy = findViewById(R.id.crowd_application_notes_ly);
		mNotes = (TextView) findViewById(R.id.crowd_application_notes);
		mItemLy = findViewById(R.id.crowd_application_item_ly);
		mMessageLy = findViewById(R.id.crowd_application_message_ly);
		mRefuseLy = findViewById(R.id.crowd_application_refuse_ly);
		mRefuseTV = (TextView) findViewById(R.id.crowd_refuse_message_et);
		mApplicationMessage = (EditText) findViewById(R.id.crowd_application_message_et);

		mSendButton = findViewById(R.id.crowd_application_send_button);
		mSendButton.setOnClickListener(mSendButtonListener);

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
		// mNoTV.setText(cid.length() > 4 ? cid.substring(5) :
		// cid.substring(1));
		mNoTV.setText(cid);
		CrowdGroup g = new CrowdGroup(crowd.getId(), crowd.getName(),
				crowd.getCreator(), null);
		if (g.getOwnerUser() != null) {
			User user = g.getOwnerUser();
			vq = VerificationProvider.queryCrowdQualMessageByCrowdId(user, g);
			// if(vq == null){
			// Toast.makeText(getApplicationContext(),
			// getResources().getString(R.string.crowd_Authentication_hit),
			// Toast.LENGTH_LONG).show();
			// super.onBackPressed();
			// return ;
			// }
		}
		updateView();
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(applyReceiver);
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		if (shieldScreen)
			return;

		if (isInApplicationMode) {
			isInApplicationMode = false;
			updateView();
			return;
		}
		super.onBackPressed();
	}

	private void initReceiver() {

		applyReceiver = new CrowdApplyBroadcast();
		IntentFilter filter = new IntentFilter();
		filter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		filter.addCategory(PublicIntent.DEFAULT_CATEGORY);
		filter.addAction(JNIService.JNI_BROADCAST_GROUP_JOIN_FAILED);
		filter.addAction(JNIService.JNI_BROADCAST_NEW_QUALIFICATION_MESSAGE);
		filter.addAction(JNIService.JNI_BROADCAST_KICED_CROWD);
		filter.addAction(PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION);
		registerReceiver(applyReceiver, filter);
	}

	private void updateView() {
		if (isInApplicationMode) {
			mSendButton.setVisibility(View.VISIBLE);
			mMessageLy.setVisibility(View.VISIBLE);
			mApplicationMessage.setText("");
			mItemLy.setVisibility(View.GONE);
			mApplicationButton.setVisibility(View.GONE);
			mTitleTV.setText(R.string.crowd_application_qualification);

			Animation out = AnimationUtils.loadAnimation(mContext,
					R.anim.left_in);
			mMessageLy.startAnimation(out);
			Animation in = AnimationUtils.loadAnimation(mContext,
					R.anim.left_out);
			mItemLy.startAnimation(in);

		} else {
			mSendButton.setVisibility(View.GONE);
			mMessageLy.setVisibility(View.GONE);
			mItemLy.setVisibility(View.VISIBLE);
			mTitleTV.setText(R.string.crowd_application_title);
			if (vq != null) {
				VMessageQualification.QualificationState state = vq
						.getQualState();
				if (state == VMessageQualification.QualificationState.BE_REJECT) {
					mRefuseLy.setVisibility(View.VISIBLE);
					mRefuseTV.setText(vq.getRejectReason());
					mApplicationButton.setVisibility(View.VISIBLE);
					mNotesLy.setVisibility(View.GONE);
					mTitleTV.setText(R.string.crowd_applicant_invite_title);
				} else {
					mRefuseLy.setVisibility(View.GONE);
					mApplicationButton.setVisibility(View.VISIBLE);
					mNotesLy.setVisibility(View.GONE);
				}
			} else {
				mRefuseLy.setVisibility(View.GONE);
				mNotesLy.setVisibility(View.GONE);
				mApplicationButton.setVisibility(View.VISIBLE);
			}

			Animation out = AnimationUtils.loadAnimation(mContext,
					R.anim.right_in);
			mItemLy.startAnimation(out);
			Animation in = AnimationUtils.loadAnimation(mContext,
					R.anim.right_out);
			mMessageLy.startAnimation(in);
		}
	}

	private void handleApplyDone() {
		Intent intent = new Intent(mContext,
				MessageAuthenticationActivity.class);
		setResult(MessageAuthenticationActivity.AUTHENTICATION_RESULT, intent);

		Intent i = new Intent();
		i.setAction(JNIService.JNI_BROADCAST_CROWD_INVATITION);
		i.addCategory(JNIService.JNI_ACTIVITY_CATEGROY);
		i.putExtra("crowd", crowd);
		i.putExtra("isFromApplication", true);
		startActivity(i);
		super.onBackPressed();
	}

	private void handleNeverApply() {
		Toast.makeText(mContext,
				R.string.crowd_application_sent_result_successful,
				Toast.LENGTH_SHORT).show();
		mLocalHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(mContext, R.string.crowd_applicant_invite_never,
						Toast.LENGTH_SHORT).show();
			}
		}, 1000);

		if (vq != null) {
			vq.setReadState(VMessageQualification.ReadState.READ);
			vq.setQualState(VMessageQualification.QualificationState.BE_REJECT);
			VerificationProvider.updateCrowdQualicationMessage(null, vq, false);
		} else {
			VMessageQualification quaion = VerificationProvider
					.queryCrowdQualMessageByCrowdId(crowd.getCreator()
							.getmUserId(), crowd.getId());
			if (quaion == null) {
				CrowdGroup g = new CrowdGroup(crowd.getId(), crowd.getName(),
						crowd.getCreator(), null);
				g.setBrief(crowd.getBrief());
				vq = new VMessageQualificationInvitationCrowd(g, GlobalHolder
						.getInstance().getCurrentUser());
				vq.setReadState(VMessageQualification.ReadState.READ);
				vq.setQualState(VMessageQualification.QualificationState.BE_REJECT);
				VerificationProvider.saveQualicationMessage(vq);
			}
		}
	}

	private OnClickListener mApplicationButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			synchronized (mState) {

				if (mState == State.APPLYING) {
					return;
				}

				if (!GlobalHolder.getInstance().isServerConnected()) {
					Toast.makeText(mContext,
							R.string.error_local_connect_to_server,
							Toast.LENGTH_SHORT).show();
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
						shieldScreen = true;
					} else if (crowd.getAuth() == CrowdGroup.AuthType.QULIFICATION
							.intValue()) {
						isInApplicationMode = true;
						updateView();
					} else if (crowd.getAuth() == CrowdGroup.AuthType.NEVER
							.intValue()) {
						handleNeverApply();
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
			}
		}

	};

	private OnClickListener mReturnButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			if (isReturnData) {
				Intent intent = new Intent(mContext,
						MessageAuthenticationActivity.class);
				intent.putExtra("qualificationID", vq.getId());
				intent.putExtra("qualState", vq.getQualState());
				setResult(4, intent);
			}
			onBackPressed();
		}

	};

	private class CrowdApplyBroadcast extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (JNIService.JNI_BROADCAST_GROUP_JOIN_FAILED.equals(intent
					.getAction())) {
				isFailed = true;
				mNotes.setText(R.string.crowd_invitation_invalid_notes);
				Toast.makeText(
						getApplicationContext(),
						getResources().getString(
								R.string.crowd_Authentication_hit),
						Toast.LENGTH_SHORT).show();
				if(vq != null){
					vq.setQualState(VMessageQualification.QualificationState.INVALID);
					VerificationProvider.updateCrowdQualicationMessage(vq);
					isReturnData = true;
				}
				mReturnButton.performClick();
			} else if (JNIService.JNI_BROADCAST_NEW_QUALIFICATION_MESSAGE
					.equals(intent.getAction())) {
				VMessageQualification msg = VerificationProvider
						.getNewestCrowdVerificationMessage();
				if (msg == null) {
					V2Log.e("CrowdApplicationActivity",
							"update Friend verification conversation failed ... given VMessageQualification is null");
					return;
				}

				if (crowd.getAuth() == CrowdGroup.AuthType.QULIFICATION
						.intValue()) {
					if (msg.getQualState() == QualificationState.BE_ACCEPTED)
						handleApplyDone();
					else if (msg.getQualState() == QualificationState.BE_REJECT) {
						mRefuseTV.setText(msg.getRejectReason());
					}
				}
			} else if (PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION
					.equals(intent.getAction())
					|| intent.getAction().equals(
							JNIService.JNI_BROADCAST_KICED_CROWD)) {
				GroupUserObject obj = intent.getParcelableExtra("group");
				if (obj == null) {
					V2Log.e("CrowdDetailActivity",
							"Received the broadcast to quit the crowd group , but crowd id is wroing... ");
					return;
				}
				if (obj.getmGroupId() == crowd.getId()) {
					onBackPressed();
				}
			}
		}
	}

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
						if (crowd.getAuth() == CrowdGroup.AuthType.ALLOW_ALL
								.intValue()) {
							mLocalHandler.postDelayed(new Runnable() {
								@Override
								public void run() {
									if(!isFailed){
										Toast.makeText(
												mContext,
												R.string.crowd_applicant_invite_finish,
												Toast.LENGTH_SHORT).show();
										shieldScreen = false;
										onBackPressed();
									}
								}
							}, 1000);
						}
					}
				} else {
					Toast.makeText(
							getApplicationContext(),
							mContext.getResources().getString(
									R.string.crowd_applicant_done),
							Toast.LENGTH_SHORT).show();
				}
				break;
			}
		}

	};

	enum State {
		DONE, APPLYING
	}

}
