package com.v2tech.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.V2.jni.AudioRequest;
import com.V2.jni.ConfRequest;
import com.V2.jni.GroupRequest;
import com.V2.jni.ImRequest;
import com.V2.jni.VideoRequest;
import com.V2.jni.WBRequest;
import com.v2tech.util.V2Log;

public class LogoutReceiver extends BroadcastReceiver {

	public static final String LOG_OUT = "com.v2tech.log_out";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(LOG_OUT)) {
			V2Log.i("log out ");
			ImRequest.getInstance(context).unInitialize();
			GroupRequest.getInstance(context).unInitialize();
			VideoRequest.getInstance(context).unInitialize();
			ConfRequest.getInstance(context).unInitialize();
			AudioRequest.getInstance(context).unInitialize();
			WBRequest.getInstance(context).unInitialize();
		}
	}

}
