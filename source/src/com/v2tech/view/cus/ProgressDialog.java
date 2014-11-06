package com.v2tech.view.cus;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Dialog;
import android.content.Context;
import android.os.Looper;
import android.view.animation.RotateAnimation;
import android.widget.Toast;

public class ProgressDialog extends Dialog {

	public static final int TIME_OUT = 10000;
	private Timer timer = new Timer();
	private RotateAnimation animation;

	public ProgressDialog(Context context, int theme, RotateAnimation animation) {
		super(context, theme);
		this.animation = animation;
	}

	public void initTimeOut() {
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				animation.cancel();
				dismiss();
				Looper.prepare();
				Toast.makeText(getContext(), "网络不佳 , 连接超时", Toast.LENGTH_SHORT).show();
				Looper.loop();
			}
		}, TIME_OUT);
	}
	
	public void cannelTimeOut(){
		timer.cancel();
	}
}
