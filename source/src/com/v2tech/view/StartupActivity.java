package com.v2tech.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.v2tech.R;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.SPUtil;

public class StartupActivity extends Activity {


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
			
			forward();
		}

	}

}
