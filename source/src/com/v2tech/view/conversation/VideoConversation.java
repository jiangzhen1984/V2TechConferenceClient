package com.v2tech.view.conversation;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;

import com.v2tech.R;
import com.v2tech.logic.Conference;
import com.v2tech.logic.UserDeviceConfig;
import com.v2tech.service.ConferenceService;

public class VideoConversation extends Activity implements TurnListener, VideoConversationListener {

	
	private ConferenceService cb = new ConferenceService();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_conversation);
		         
     	Fragment fragment1 = new ConversationWaitingFragment();  
		FragmentTransaction transaction = getFragmentManager().beginTransaction();
		transaction.replace(R.id.video_conversation_main, fragment1,"waiting");
        transaction.addToBackStack(null);
		transaction.commit();  

	}

	@Override
	public void turnToVideoUI() {
		Fragment fragment1 = new VideoConversationFragment();
		FragmentTransaction transaction = getFragmentManager().beginTransaction();
		transaction.replace(R.id.video_conversation_main, fragment1, "video");
        transaction.addToBackStack(null);
		transaction.commit();  
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
	public void openLocalCamera() {
		cb.requestOpenVideoDevice(new Conference(0), new UserDeviceConfig(1, "", null), null);
	}

	@Override
	public void reverseLocalCamera() {
		
	}

	@Override
	public void closeLocalCamera() {
		cb.requestCloseVideoDevice(new Conference(0), new UserDeviceConfig(1, "", null), null);
	}





	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
	}




}
