package com.v2tech.view.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.V2.jni.util.V2Log;
import com.v2tech.service.GlobalHolder;
import com.v2tech.view.PublicIntent;
import com.v2tech.vo.User;

public class CrowdQuitReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(PublicIntent.BROADCAST_CROWD_QUIT_NOTIFICATION)) {
			long uid = intent.getLongExtra("userId", 0);
			long groupId = intent.getLongExtra("groupId", 0);
			boolean kicked = intent.getBooleanExtra("kicked", false);
			User u = GlobalHolder.getInstance().getUser(uid);
			if (u == null) {
				V2Log.e("Receiver Crowd Quit : can not find user" + uid);
				return;
			}
			//TODO delete all related message
			
		}
		

	}

}
