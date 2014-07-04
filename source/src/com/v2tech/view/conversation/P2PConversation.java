package com.v2tech.view.conversation;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.service.ConferenceService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.vo.Conference;
import com.v2tech.vo.User;
import com.v2tech.vo.UserChattingObject;
import com.v2tech.vo.UserDeviceConfig;

public class P2PConversation extends Activity implements TurnListener,
		VideoConversationListener {

	private ConferenceService cb = new ConferenceService();

	private UserChattingObject uad;
	
	private boolean isStoped;
	
	private boolean isPending;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_p2p_conversation);
		buildObject();
		Fragment fragment1 = new ConversationWaitingFragment();
		FragmentTransaction transaction = getFragmentManager()
				.beginTransaction();
		transaction.replace(R.id.p2p_conversation_main, fragment1, "waiting");
		transaction.addToBackStack(null);
		transaction.commit();

	}

	@Override
	public void turnToVideoUI() {
		if (isStoped) {
			isPending = true;
			return;
		}
		Fragment fragment1 = new ConversationConnectedFragment();
		FragmentTransaction transaction = getFragmentManager()
				.beginTransaction();
		transaction.replace(R.id.p2p_conversation_main, fragment1, "video");
		transaction.addToBackStack(null);
		transaction.commit();
	}
	
	
	
	
	

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	public UserChattingObject getObject() {
		if (uad == null) {
			buildObject();
		}
		return uad;
	}

	@Override
	protected void onStart() {
		super.onStart();
		isStoped = false;
		if (isPending) {
			turnToVideoUI();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		isStoped = true;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
	}

	@Override
	public void openLocalCamera() {
		cb.requestOpenVideoDevice(new Conference(0), new UserDeviceConfig(
				GlobalHolder.getInstance().getCurrentUserId(), "", null), null);
	}

	@Override
	public void reverseLocalCamera() {

	}

	@Override
	public void closeLocalCamera() {
		cb.requestCloseVideoDevice(new Conference(0), new UserDeviceConfig(
				GlobalHolder.getInstance().getCurrentUserId(), "", null), null);
	}

	@Override
	public void onBackPressed() {
		showConfirmDialog();
	}

	private void showConfirmDialog() {
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.fragment_conversation_quit_dialog);
		dialog.getWindow().setBackgroundDrawable(
				new ColorDrawable(android.graphics.Color.TRANSPARENT));
		
		if (getObject().isAudioType()) {
			((TextView) dialog
					.findViewById(R.id.fragment_conversation_quit_dialog_content))
					.setText(R.string.conversation_quit_dialog_audio_text);
		}
		dialog.findViewById(R.id.fragment_conversation_IMWCancelButton)
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View view) {
						dialog.dismiss();
					}

				});

		dialog.findViewById(R.id.fragment_conversation_IMWQuitButton)
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View view) {
						finish();
					}

				});
		dialog.show();
	}

	private UserChattingObject buildObject() {
		long uid = getIntent().getExtras().getLong("uid");
		boolean mIsInComingCall = getIntent().getExtras().getBoolean(
				"is_coming_call");
		boolean mIsVoiceCall = getIntent().getExtras().getBoolean("voice");
		String deviceId = getIntent().getExtras().getString("device");
		User u = GlobalHolder.getInstance().getUser(uid);

		int flag = mIsVoiceCall ? UserChattingObject.VOICE_CALL
				: UserChattingObject.VIDEO_CALL;
		if (mIsInComingCall) {
			flag |= UserChattingObject.INCOMING_CALL;
		} else {
			flag |= UserChattingObject.OUTING_CALL;
		}
		uad = new UserChattingObject(u, flag, deviceId);
		return uad;
	}

}
