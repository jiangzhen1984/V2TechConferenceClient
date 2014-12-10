package com.v2tech.view;

import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;

import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.db.provider.SearchContentProvider;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.SPUtil;

public class StartupActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
			finish();
			return;
		} 
		setContentView(R.layout.load);
		
		initSearchMap();
		new LoaderThread().start();
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

			forward();
		}

	}

}
