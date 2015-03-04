package com.bizcom.vc.activity.main;

import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.TextView;

import com.V2.jni.util.V2Log;
import com.bizcom.db.provider.SearchContentProvider;
import com.bizcom.util.LocalSharedPreferencesStorage;
import com.bizcom.vc.application.GlobalConfig;
import com.bizcom.vc.application.MainApplication;
import com.v2tech.R;

public class SplashActivity extends Activity {
	private long loadStartTime = 0;
	private long loadStopTime = 0;
	private long sleepTime = 2000;
	private boolean isFward = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
			finish();
			return;
		}

		setContentView(R.layout.load);
		((TextView) findViewById(R.id.versionNumber))
				.setText(GlobalConfig.GLOBAL_VERSION_NAME);
		new LoaderThread().start();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (!isFward) {
			((MainApplication) getApplication()).uninitForExitProcess();
		}

	}

	class LoaderThread extends Thread {

		@Override
		public void run() {
			loadStartTime = System.currentTimeMillis();

			DisplayMetrics dm = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(dm);
			GlobalConfig.SCREEN_WIDTH = dm.widthPixels;
			GlobalConfig.SCREEN_HEIGHT = dm.heightPixels;
			initSearchMap();
			forward();
		}

	}

	private void initSearchMap() {
		HashMap<String, String> allChinese = SearchContentProvider
				.queryAll(this);
		if (allChinese == null) {
			V2Log.e("loading dataBase data is fialed...");
			return;
		}
		GlobalConfig.allChinese.putAll(allChinese);
	}

	private void forward() {

		int flag = LocalSharedPreferencesStorage.getConfigIntValue(this,
				GlobalConfig.KEY_LOGGED_IN, 0);
		loadStopTime = System.currentTimeMillis();
		try {
			Log.i("20150211 4", ""
					+ (sleepTime - (loadStopTime - loadStartTime)));
			Thread.sleep(sleepTime - (loadStopTime - loadStartTime));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (flag == 1) {
			startActivity(new Intent(this, MainActivity.class));
			isFward = true;
		} else {
			startActivity(new Intent(this, LoginActivity.class));
			isFward = true;
		}

		try {
			Thread.sleep(1000);

		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		finish();
	}


}
