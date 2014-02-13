package com.v2tech.view.conversation;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import com.v2tech.R;
import com.v2tech.logic.UserDeviceConfig;
import com.v2tech.view.JNIService;
import com.v2tech.view.JNIService.LocalBinder;

public class VideoConversation extends Activity implements TurnListener, VideoConversationListener {

	
	private JNIService mService;
	private boolean isBound;
	
	
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
	
	

	
	
	@Override
	protected void onStart() {
		super.onStart();
		isBound = bindService(new Intent(this.getApplicationContext(),
				JNIService.class), mConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}

	@Override
	public void openLocalCamera() {
		if (mService != null) {
			mService.requestOpenVideoDevice(0, new UserDeviceConfig(1, "", null), null);
		}
	}

	@Override
	public void reverseLocalCamera() {
		
	}

	@Override
	public void closeLocalCamera() {
		if (mService != null) {
			mService.requestCloseVideoDevice(0, new UserDeviceConfig(1, "", null), null);
		}
	}





	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
	}





	/** Defines callback for service binding, passed to bindService() */
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			isBound = true;
//			// Send server bounded message
//			Message.obtain(mVideoHandler, SERVICE_BUNDED).sendToTarget();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			isBound = false;
		}
	};

}
