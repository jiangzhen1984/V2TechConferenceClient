package com.v2tech.view;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.V2.jni.AudioRequest;
import com.V2.jni.ChatRequest;
import com.V2.jni.ConfRequest;
import com.V2.jni.ConfigRequest;
import com.V2.jni.GroupRequest;
import com.V2.jni.ImRequest;
import com.V2.jni.VideoRequest;
import com.V2.jni.WBRequest;
import com.v2tech.R;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.SPUtil;
import com.v2tech.util.StorageUtil;
import com.v2tech.util.V2Log;

public class StartupActivity extends Activity {

	private Context mCtx;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mCtx = this;
		setContentView(R.layout.load);
		new LoaderThread().start();
	}

	
	
	private void forward() {
		int flag = SPUtil
				.getConfigIntValue(this, GlobalConfig.KEY_LOGGED_IN, 0);
		if (flag == 1) {
			startActivity(new Intent(this, MainActivity.class));
		} else {
			startActivity(new Intent(this, LoginActivity.class));
		}
		finish();
	}
	
	class LoaderThread extends Thread {

		@Override
		public void run() {
			System.loadLibrary("event");
			System.loadLibrary("udt");
			System.loadLibrary("v2vi");
			System.loadLibrary("v2ve");
			System.loadLibrary("v2client");

			ImRequest.getInstance(mCtx);
			GroupRequest.getInstance(mCtx);
			VideoRequest.getInstance(mCtx);
			ConfRequest.getInstance(mCtx);
			AudioRequest.getInstance(mCtx);
			WBRequest.getInstance(mCtx);
			ChatRequest.getInstance(mCtx);

			getApplicationContext().startService(
					new Intent(mCtx, JNIService.class));

			String path = StorageUtil.getAbsoluteSdcardPath();
			new ConfigRequest().setExtStoragePath(path);
			File pa = new File(path + "/Users");
			if (!pa.exists()) {
				boolean res = pa.mkdirs();
				V2Log.i(" create avatar dir " + pa.getAbsolutePath() + "  "
						+ res);
			}
			pa.setWritable(true);
			pa.setReadable(true);
			
			File image = new File(path + "/v2tech/pics");
			if (!image.exists()) {
				boolean res = image.mkdirs();
				V2Log.i(" create avatar dir " + image.getAbsolutePath() + "  "
						+ res);
			}
			
			forward();
		}

	}

}
