package com.v2tech.view.conversation;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.logic.GlobalHolder;
import com.v2tech.logic.User;
import com.v2tech.logic.UserAudioDevice;
import com.v2tech.service.ChatService;
import com.v2tech.util.V2Log;

public class ConversationWaitingFragment extends Fragment {

	private static final int CALL_RESPONSE = 1;

	private TextView mUserNameTV;
	private TextView mTextTV;
	private TextView mRejectButtonTV;
	private TextView mAcceptButtonTV;
	private TextView mCancelButtonTV;

	private boolean mIsInComingCall;

	private ChatService chat = new ChatService();
	
	private Handler mLocalHandler = new LocalHandler();
	
	private TurnListener mIndicator;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		long uid = getActivity().getIntent().getExtras().getLong("uid");
		User u = GlobalHolder.getInstance().getUser(uid);
		if (u == null) {

		} else {
			UserAudioDevice uad = new UserAudioDevice(u);
			chat.inviteUserAudioChat(uad, Message.obtain(mLocalHandler, CALL_RESPONSE));
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.activity_conversation_waiting,
				container, false);

		this.mUserNameTV = (TextView) v
				.findViewById(R.id.conversation_user_title);
		this.mTextTV = (TextView) v.findViewById(R.id.conversation_text);
		this.mRejectButtonTV = (TextView) v
				.findViewById(R.id.conversation_reject_button);
		this.mAcceptButtonTV = (TextView) v
				.findViewById(R.id.conversation_accept_button);
		this.mCancelButtonTV = (TextView) v
				.findViewById(R.id.conversation_cancel_button);
		mIsInComingCall = getActivity().getIntent().getBooleanExtra(
				"is_coming_call", false);
		if (mIsInComingCall) {
			mCancelButtonTV.setVisibility(View.GONE);
			mRejectButtonTV.setVisibility(View.VISIBLE);
			mAcceptButtonTV.setVisibility(View.VISIBLE);
			mTextTV.setText(R.string.conversation_notification);
		} else {
			mRejectButtonTV.setVisibility(View.GONE);
			mAcceptButtonTV.setVisibility(View.GONE);
			mCancelButtonTV.setVisibility(View.VISIBLE);
			mTextTV.setText(R.string.conversation_waiting);
		}

		mUserNameTV.setText(getActivity().getIntent().getStringExtra("name"));
		mRejectButtonTV.setOnClickListener(rejectListener);
		mAcceptButtonTV.setOnClickListener(acceptListener);
		mCancelButtonTV.setOnClickListener(cancelListener);
		return v;

	}
	
	

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mIndicator = (TurnListener) activity;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mIndicator = null;
	}



	private OnClickListener rejectListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			// TODO reject call
			getActivity().finish();
		}

	};

	private OnClickListener cancelListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			// TODO cancel call
			getActivity().finish();
		}

	};

	private OnClickListener acceptListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			// TODO accept call
			((TurnListener) getActivity()).turnToVideoUI();
		}

	};

	class LocalHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case CALL_RESPONSE: {
					if (mIndicator != null) {
						mIndicator.turnToVideoUI();
					} else {
						V2Log.e(" indicator is null can not open audio UI ");
					}
				}
				break;
			}
		}

	}
}
