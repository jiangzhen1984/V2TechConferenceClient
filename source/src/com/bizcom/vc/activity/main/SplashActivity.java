package com.bizcom.vc.activity.main;

import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.TextView;

import com.V2.jni.util.V2Log;
import com.bizcom.db.DataBaseContext;
import com.bizcom.db.V2TechDBHelper;
import com.bizcom.db.provider.SearchContentProvider;
import com.bizcom.util.LocalSharedPreferencesStorage;
import com.bizcom.util.StorageUtil;
import com.bizcom.vc.application.GlobalConfig;
import com.bizcom.vc.application.GlobalHolder;
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
		((TextView)findViewById(R.id.versionNumber)).setText(GlobalConfig.GLOBAL_VERSION_NAME);
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

			initDataBaseTableCacheNames();

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

	/**
	 * 初始化获取数据库中所有表
	 */
	private void initDataBaseTableCacheNames() {
		DataBaseContext mContext = new DataBaseContext(getApplicationContext());
		V2TechDBHelper mOpenHelper = V2TechDBHelper.getInstance(mContext);
		SQLiteDatabase dataBase = mOpenHelper.getReadableDatabase();
		Cursor cursor = null;
		try {
			cursor = dataBase.rawQuery(
					"select name as c from sqlite_master where type ='table'",
					null);
			if (cursor != null) {
				List<String> dataBaseTableCacheName = GlobalHolder
						.getInstance().getDataBaseTableCacheName();
				while (cursor.moveToNext()) {
					// 遍历出表名
					String name = cursor.getString(0);
					V2Log.d("iteration DataBase table name : " + name);
					dataBaseTableCacheName.add(name);
				}
			} else
				throw new RuntimeException(
						"init DataBase table names failed.. get null , please check");
		} catch (Exception e) {
			throw new RuntimeException(
					"init DataBase table names failed.. get null , please check"
							+ e.getStackTrace());
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}

}
