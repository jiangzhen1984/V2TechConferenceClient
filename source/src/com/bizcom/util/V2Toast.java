package com.bizcom.util;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

public class V2Toast {
	public static final int LENGTH_SHORT = Toast.LENGTH_SHORT;
	public static final int LENGTH_LONG = Toast.LENGTH_LONG;

	private static Toast toast;
	private static Handler handler = new Handler();

	private static Runnable run = new Runnable() {
		public void run() {
			toast.cancel();
		}
	};

	public static V2Toast makeText(Context ctx, int stringID, int duration) {
		handler.removeCallbacks(run);
		
		V2Toast v2Toast = new V2Toast();
		CharSequence msg = ctx.getResources().getString(stringID);
		// handler的duration不能直接对应Toast的常量时长，在此针对Toast的常量相应定义时长
		switch (duration) {
		case LENGTH_SHORT:// Toast.LENGTH_SHORT值为0，对应的持续时间大概为1s
			duration = 1000;
			break;
		case LENGTH_LONG:// Toast.LENGTH_LONG值为1，对应的持续时间大概为3s
			duration = 3000;
			break;
		default:
			break;
		}
		if (null != toast)
			toast.setText(msg);
		else
			toast = Toast.makeText(ctx, msg, duration);
		
		handler.postDelayed(run, duration);
		toast.show();
		return v2Toast;
	}
	
	public void show(){};
}