package com.v2tech.view;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.DisplayMetrics;

import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.SPUtil;

public class StartupActivity extends Activity {

	private final String DATABASE_FILENAME = "HZPY.db";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
			finish();
			return;
		} 
		setContentView(R.layout.load);
		initDPI();
		initSQLiteFile();
		new LoaderThread().start();
	}

	private void initSQLiteFile() {
		
		try {
			// 获得.db文件的绝对路径
			String parent = getDatabasePath(DATABASE_FILENAME).getParent();
			File dir = new File(parent);
			// 如果目录不存在，创建这个目录
			if (!dir.exists())
				dir.mkdir();
			String databaseFilename = getDatabasePath(DATABASE_FILENAME).getPath();
			// 目录中不存在 .db文件，则从res\raw目录中复制这个文件到该目录
			if (!(new File(databaseFilename)).exists()) {
				// 获得封装.db文件的InputStream对象
				InputStream is = getResources()
						.openRawResource(R.raw.hzpy);
				if(is == null){
					V2Log.e("readed sqlite file failed... inputStream is null");
					return ; 
				}
				FileOutputStream fos = new FileOutputStream(databaseFilename);
				byte[] buffer = new byte[1024];
				int count = 0;
				// 开始复制.db文件
				while ((count = is.read(buffer)) != -1) {
					fos.write(buffer, 0, count);
				}
				fos.close();
				is.close();
			}
		} catch (Exception e) {
			e.getStackTrace();
			V2Log.e("loading HZPY.db SQListe");
		}
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
