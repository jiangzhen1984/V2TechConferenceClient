package com.v2tech.view.conversation;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.ViewGroup;

import com.v2tech.R;

public class VideoConversation extends Activity implements TurnListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_conversation);
		         
     	Fragment fragment1 = new ConversationWaitingFragment();  
		FragmentTransaction transaction = getFragmentManager().beginTransaction();
		transaction.replace(R.id.video_conversation_main, fragment1);
        transaction.addToBackStack(null);
		transaction.commit();  

	}

	@Override
	public void turnToVideoUI() {
		Fragment fragment1 = new VideoConversationFragment();  
		FragmentTransaction transaction = getFragmentManager().beginTransaction();
		transaction.replace(R.id.video_conversation_main, fragment1);
        transaction.addToBackStack(null);
		transaction.commit();  
	}
	
	
	
	

}
