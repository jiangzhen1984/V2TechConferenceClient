package com.v2tech.view.conversation;

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

public class ConversationWaitingFragment extends Fragment {

	private TextView mUserNameTV;
	private TextView mTextTV;
	private TextView mRejectButtonTV;
	private TextView mAcceptButtonTV;
	private TextView mCancelButtonTV;

	private boolean mIsInComingCall;

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
		mIsInComingCall = getActivity().getIntent().getBooleanExtra("is_coming_call", false);
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
		
		mUserNameTV.setText( getActivity().getIntent().getStringExtra("name"));
		mRejectButtonTV.setOnClickListener(rejectListener);
		mAcceptButtonTV.setOnClickListener(acceptListener);
		mCancelButtonTV.setOnClickListener(cancelListener);
		LocalHandler lh = new LocalHandler();
		Message m = Message.obtain();
		lh.sendMessageDelayed(m, 2000);
		return v;
		
	}
	
	
	private OnClickListener rejectListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			//TODO reject call
			getActivity().finish();
		}
		
	};
	

	
	private OnClickListener cancelListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			//TODO cancel call
			getActivity().finish();
		}
		
	};
	
	private OnClickListener acceptListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			//TODO accept call
			((TurnListener)getActivity()).turnToVideoUI();
		}
		
	};
	
	
	
	//FIXME test code
	class LocalHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			((TurnListener)getActivity()).turnToVideoUI();
		}
		
	}

}
