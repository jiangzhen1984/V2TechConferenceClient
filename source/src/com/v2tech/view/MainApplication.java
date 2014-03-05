package com.v2tech.view;

import java.io.File;

import android.app.Application;
import android.content.Intent;
import android.content.pm.ApplicationInfo;

import com.V2.jni.AudioRequest;
import com.V2.jni.ChatRequest;
import com.V2.jni.ConfRequest;
import com.V2.jni.GroupRequest;
import com.V2.jni.ImRequest;
import com.V2.jni.VideoRequest;
import com.V2.jni.WBRequest;
import com.v2tech.util.StorageUtil;
import com.v2tech.util.V2Log;

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
		String path = StorageUtil.getAbsoluteSdcardPath();
//		new ConfigRequest().setExtStoragePath(path);
//		File pa = new File(path +"/Users");
//		if (!pa.exists()) {
//			boolean res = pa.mkdirs();
//			V2Log.i(" create avatar dir " +pa.getAbsolutePath() +"  "+ res);
//		}
//		pa.setWritable(true);
//		pa.setReadable(true);
//		
//		
		File image = new File(path +"/v2tech/pics");
		if (!image.exists()) {
			boolean res = image.mkdirs();
			V2Log.i(" create avatar dir " +image.getAbsolutePath() +"  "+ res);
		}
		
		
		this.getApplicationContext().startService(new Intent(this.getApplicationContext(), JNIService.class));
		
		V2Log.isDebuggable =  (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
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
