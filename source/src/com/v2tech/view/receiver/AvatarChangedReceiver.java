package com.v2tech.view.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.v2tech.service.BitmapManager;
import com.v2tech.view.JNIService;
import com.v2tech.view.bo.UserAvatarObject;

public class AvatarChangedReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (JNIService.JNI_BROADCAST_USER_AVATAR_CHANGED_NOTIFICATION.equals(intent.getAction())) {
			UserAvatarObject  uao = (UserAvatarObject) intent.getExtras().get("avatar");
			if (uao != null) {
				BitmapManager.getInstance().loadUserAvatarAndNotify(uao);
			}
		}

	}

}
