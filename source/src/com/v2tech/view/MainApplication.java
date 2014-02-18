package com.v2tech.view;

import android.app.Application;
import android.content.Intent;

import com.V2.jni.AudioRequest;
import com.V2.jni.ChatRequest;
import com.V2.jni.ConfRequest;
import com.V2.jni.GroupRequest;
import com.V2.jni.ImRequest;
import com.V2.jni.VideoRequest;
import com.V2.jni.WBRequest;

public class MainApplication extends Application {

	


	static {
		System.loadLibrary("event");
		System.loadLibrary("udt");
		System.loadLibrary("v2vi");
		System.loadLibrary("v2ve");	
		System.loadLibrary("v2client");
	}

	
	
	
	@Override
	public void onCreate() {
		super.onCreate();
		ImRequest.getInstance(this);
		GroupRequest.getInstance(this);
		VideoRequest.getInstance(this);
		ConfRequest.getInstance(this);
		AudioRequest.getInstance(this);
		WBRequest.getInstance(this);
		ChatRequest.getInstance(this);
		this.getApplicationContext().startService(new Intent(this.getApplicationContext(), JNIService.class));
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		ImRequest.getInstance(this).unInitialize();
		GroupRequest.getInstance(this).unInitialize();
		VideoRequest.getInstance(this).unInitialize();
		ConfRequest.getInstance(this).unInitialize();
		AudioRequest.getInstance(this).unInitialize();
		WBRequest.getInstance(this).unInitialize();
		ChatRequest.getInstance(this).unInitialize();
		this.getApplicationContext().stopService(new Intent(this.getApplicationContext(), JNIService.class));
	}

	
	
}
