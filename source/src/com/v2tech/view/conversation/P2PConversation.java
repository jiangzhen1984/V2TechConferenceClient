package com.v2tech.view.conversation;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;

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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_p2p_conversation);

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

		Fragment fragment1 = new ConversationWaitingFragment();
		FragmentTransaction transaction = getFragmentManager()
				.beginTransaction();
		transaction.replace(R.id.p2p_conversation_main, fragment1, "waiting");
		transaction.addToBackStack(null);
		transaction.commit();

	}

	@Override
	public void turnToVideoUI() {
		Fragment fragment1 = new ConversationConnectedFragment();
		FragmentTransaction transaction = getFragmentManager()
				.beginTransaction();
		transaction.replace(R.id.p2p_conversation_main, fragment1, "video");
		transaction.addToBackStack(null);
		transaction.commit();
	}

	@Override
	public UserChattingObject getObject() {
		return uad;
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
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
		finish();
	}

}
