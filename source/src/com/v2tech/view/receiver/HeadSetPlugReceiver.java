package com.v2tech.view.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import com.v2tech.service.GlobalHolder;
import com.v2tech.util.GlobalState;

public class HeadSetPlugReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		
		if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
			int state = intent.getIntExtra("state", -1);
			GlobalState gs = GlobalHolder.getInstance().getGlobalState();
			
			AudioManager  am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
			//If wired headset unpluged
			if (state == 0) {
				GlobalHolder.getInstance().setWiredHeadsetState(false);
			//If wired headset pluged
			} else if (state == 1){
				GlobalHolder.getInstance().setWiredHeadsetState(true);
				
			}
		}
		
		//TODO also handle bluetooth headset
		
	}

	
	
}
