package com.v2tech.view;

import java.io.File;

import net.sourceforge.pinyin4j.PinyinHelper;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import com.V2.jni.AudioRequest;
import com.V2.jni.ChatRequest;
import com.V2.jni.ConfRequest;
import com.V2.jni.ConfigRequest;
import com.V2.jni.GroupRequest;
import com.V2.jni.ImRequest;
import com.V2.jni.VideoRequest;
import com.V2.jni.WBRequest;
import com.v2tech.util.CrashHandler;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.StorageUtil;
import com.v2tech.util.V2Log;

public class MainApplication extends Application {

	static {
		V2Log.d("=====load=====");
		System.loadLibrary("event");
		System.loadLibrary("udt");
		System.loadLibrary("v2vi");
		System.loadLibrary("v2ve");
		System.loadLibrary("v2client");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		CrashHandler crashHandler = CrashHandler.getInstance();  
        crashHandler.init(getApplicationContext()); 
		V2Log.d("=====create=====");
		ImRequest.getInstance(this);
		GroupRequest.getInstance(this);
		VideoRequest.getInstance(this);
		ConfRequest.getInstance(this);
		AudioRequest.getInstance(this);
		WBRequest.getInstance(this);
		ChatRequest.getInstance(this);
		String path = StorageUtil.getAbsoluteSdcardPath();
		new ConfigRequest().setExtStoragePath(path);
		File pa = new File(path + "/Users");
		if (!pa.exists()) {
			boolean res = pa.mkdirs();
			V2Log.i(" create avatar dir " + pa.getAbsolutePath() + "  " + res);
		}
		pa.setWritable(true);
		pa.setReadable(true);
		//
		//
		File image = new File(path + "/v2tech/pics");
		if (!image.exists()) {
			boolean res = image.mkdirs();
			V2Log.i(" create avatar dir " + image.getAbsolutePath() + "  "
					+ res);
		}

		this.getApplicationContext().startService(
				new Intent(this.getApplicationContext(), JNIService.class));

		V2Log.isDebuggable = (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));

		SharedPreferences sf = getSharedPreferences("config",
				Context.MODE_PRIVATE);
		Editor ed = sf.edit();
		ed.putInt("LoggedIn", 0);
		ed.commit();

		try {
			String app_ver = this.getPackageManager().getPackageInfo(
					this.getPackageName(), 0).versionName;
			GlobalConfig.GLOBAL_VERSION_NAME = app_ver;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		new Thread() {

			@Override
			public void run() {
				PinyinHelper.toHanyuPinyinStringArray('c');
			}
			
		}.start();
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
		this.getApplicationContext().stopService(
				new Intent(this.getApplicationContext(), JNIService.class));

	}

}
