package com.v2tech.view.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class InvitionReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

}
