package com.v2tech.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;

import com.v2tech.R;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.SPUtil;
import com.v2tech.util.V2Log;

public class StartupActivity extends Activity {


	private static final String TAG = "StartupActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.load);
		initDPI();
		Log.d(TAG, "执行了StartupActivity 的onCreate方法");
		new LoaderThread().start();
	}

	
	
	private void forward() {
		Log.d(TAG, "执行了StartupActivity 的forwrad方法");
		int flag = SPUtil
				.getConfigIntValue(this, GlobalConfig.KEY_LOGGED_IN, 0);
		if (flag == 1) {
			startActivity(new Intent(this, MainActivity.class));
		} else {
			startActivity(new Intent(this, LoginActivity.class));
		}
		finish();
	}
	
	
	private void initDPI() {
		DisplayMetrics metrics = new DisplayMetrics();

		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		GlobalConfig.GLOBAL_DPI = metrics.densityDpi;
		V2Log.i("Init user device DPI: " + GlobalConfig.GLOBAL_DPI);
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		double x = Math.pow(dm.widthPixels / dm.xdpi, 2);
		double y = Math.pow(dm.heightPixels / dm.ydpi, 2);
		GlobalConfig.SCREEN_INCHES = Math.sqrt(x + y);
	}
	
	class LoaderThread extends Thread {

		@Override
		public void run() {
			
			forward();
		}

	}

}
