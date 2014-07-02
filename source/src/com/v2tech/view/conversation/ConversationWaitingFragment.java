package com.v2tech.view.conversation;

import android.app.Activity;
import android.app.Fragment;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.service.ChatService;
import com.v2tech.service.Registrant;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.service.jni.RequestChatServiceResponse;
import com.v2tech.util.V2Log;
import com.v2tech.vo.UserChattingObject;

public class ConversationWaitingFragment extends Fragment {

	private static final int CALL_RESPONSE = 1;
	private static final int CANCELLED_NOTIFICATION = 2;

	private TextView mInvitationNameTV;
	private TextView mTitleTV;
	private ImageView mAvatar;

	private View mRejectButton;
	private View mAcceptButton;
	private View mCancelButton;
	private View mAcceptVocieOnlyButton;

	private View mInvitationButtonLayout;
	private View mHostInvitationButtonLayout;

	private ChatService chat = new ChatService();

	private Handler mLocalHandler = new LocalHandler();

	private TurnListener mIndicator;

	private UserChattingObject uad;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// If is outing call, send invitation request
		if (!uad.isIncoming()) {
			chat.inviteUserChat(uad, new Registrant(mLocalHandler,
					CALL_RESPONSE, null));
		} else {
			playRingToneIncoming();
		}
		//Start timer
		mLocalHandler.postDelayed(timeOutMonitor, 1000*60);
		
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View v = inflater.inflate(R.layout.fragment_conversation_waiting,
				container, false);

		this.mInvitationNameTV = (TextView) v
				.findViewById(R.id.conversation_fragment_voice_invitation_name);
		this.mTitleTV = (TextView) v
				.findViewById(R.id.fragment_conversation_title);
		this.mAvatar = (ImageView) v
				.findViewById(R.id.conversation_fragment_voice_avatar);

		this.mInvitationButtonLayout = v
				.findViewById(R.id.conversation_fragment_voice_invitation_button_container);
		this.mHostInvitationButtonLayout = v
				.findViewById(R.id.conversation_fragment_voice_host_invitation_button_container);

		this.mRejectButton = v
				.findViewById(R.id.conversation_fragment_voice_reject_button);
		this.mAcceptButton = v
				.findViewById(R.id.conversation_fragment_voice_accept_button);
		this.mCancelButton = v
				.findViewById(R.id.conversation_fragment_voice_host_cancel_button);
		this.mAcceptVocieOnlyButton = v
				.findViewById(R.id.conversation_fragment_voice_accept_only_button);

		mRejectButton.setOnClickListener(rejectListener);
		mAcceptButton.setOnClickListener(acceptListener);
		mCancelButton.setOnClickListener(cancelListener);
		mAcceptVocieOnlyButton.setOnClickListener(acceptVoicOnlyListener);

		if (uad.isIncoming()) {
			mInvitationButtonLayout.setVisibility(View.VISIBLE);
			mHostInvitationButtonLayout.setVisibility(View.GONE);
		} else {
			mInvitationButtonLayout.setVisibility(View.GONE);
			mHostInvitationButtonLayout.setVisibility(View.VISIBLE);
		}
		
		if (uad.isVideoType()) {
			mAcceptVocieOnlyButton.setVisibility(View.VISIBLE);
		} else {
			mAcceptVocieOnlyButton.setVisibility(View.GONE);
		}

		// FIXME handle when user changed avatar
		if (uad.getUser().getAvatarBitmap() != null) {
			mAvatar.setImageBitmap(uad.getUser().getAvatarBitmap());
		}
		mInvitationNameTV.setText(uad.getUser().getName());
		return v;

	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mIndicator = (TurnListener) activity;
		chat.registerCancelledListener(mLocalHandler, CANCELLED_NOTIFICATION,
				null);
		uad = mIndicator.getObject();
	}
	
	

	@Override
	public void onStop() {
		super.onStop();
		stopRingTone();
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mIndicator = null;
		chat.removeRegisterCancelledListener(mLocalHandler,
				CANCELLED_NOTIFICATION, null);
		//Remove timer
		this.mLocalHandler.removeCallbacks(timeOutMonitor);
	}

	public void quit() {
		if (getActivity() != null) {
			getActivity().finish();
		}
	}

	private MediaPlayer thePlayer;
	private void playRingToneIncoming() {
		thePlayer = MediaPlayer.create(getActivity(), RingtoneManager
				.getDefaultUri(RingtoneManager.TYPE_RINGTONE));
		thePlayer.start();
	}
	
	private void stopRingTone() {
		if (thePlayer != null) {
			thePlayer.release();
		}
	}

	private OnClickListener rejectListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			chat.refuseChatting(uad, null);
			quit();
		}

	};

	private OnClickListener cancelListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			chat.cancelChattingCall(uad, null);
			quit();
		}

	};
	
	
	private OnClickListener acceptVoicOnlyListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			stopRingTone();
		//	uad.updateAudioType();
			chat.acceptChatting(uad, null);
			((TurnListener) getActivity()).turnToVideoUI();
		}

	};
	
	

	private OnClickListener acceptListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			stopRingTone();
			chat.acceptChatting(uad, null);
			((TurnListener) getActivity()).turnToVideoUI();
		}

	};
	
	private Runnable timeOutMonitor = new Runnable() {

		@Override
		public void run() {
			Message.obtain(mLocalHandler, CANCELLED_NOTIFICATION).sendToTarget();
		}
		
	};

	class LocalHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case CALL_RESPONSE: {
				JNIResponse resp = (JNIResponse) msg.obj;
				if (resp.getResult() == JNIResponse.Result.SUCCESS) {
					RequestChatServiceResponse rcsr = (RequestChatServiceResponse) resp;
					if (rcsr.getCode() == RequestChatServiceResponse.REJCTED) {
						chat.cancelChattingCall(uad, null);
						getActivity().finish();
					} else if (rcsr.getCode() == RequestChatServiceResponse.ACCEPTED
							&& mIndicator != null) {
						mIndicator.turnToVideoUI();
					} else {
						V2Log.e(" indicator is null can not open audio UI ");
					}
				}
			}
				break;
			case CANCELLED_NOTIFICATION:
				quit();
				break;
			}
		}

	}
}
