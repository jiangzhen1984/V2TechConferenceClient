package com.v2tech.view;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Vector;

import net.sourceforge.pinyin4j.PinyinHelper;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import com.V2.jni.AudioRequest;
import com.V2.jni.ChatRequest;
import com.V2.jni.ConfRequest;
import com.V2.jni.FileRequest;
import com.V2.jni.GroupRequest;
import com.V2.jni.ImRequest;
import com.V2.jni.NativeInitializer;
import com.V2.jni.VideoMixerRequest;
import com.V2.jni.VideoRequest;
import com.V2.jni.WBRequest;
import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.util.CrashHandler;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.LogcatThread;
import com.v2tech.util.Notificator;
import com.v2tech.view.conference.VideoActivityV2;

public class MainApplication extends Application {

	private Vector<WeakReference<Activity>> list = new Vector<WeakReference<Activity>>();
	private final String DATABASE_FILENAME = "hzpy.db";
	private boolean needCopy;
	@Override
	public void onCreate() {
		super.onCreate();

		V2Log.isDebuggable = (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
		if (!V2Log.isDebuggable) {
			CrashHandler crashHandler = CrashHandler.getInstance();
			crashHandler.init(getApplicationContext());
		}
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
		if (!V2Log.isDebuggable) {
			new Thread() {

				@Override
				public void run() {
					PinyinHelper.toHanyuPinyinStringArray('c');
				}

			}.start();
		}

		String path = GlobalConfig.getGlobalPath();
		File pa = new File(GlobalConfig.getGlobalUserAvatarPath());
		if (!pa.exists()) {
			boolean res = pa.mkdirs();
			V2Log.i(" create avatar dir " + pa.getAbsolutePath() + "  " + res);
		}
		pa.setWritable(true);
		pa.setReadable(true);

		File image = new File(GlobalConfig.getGlobalPicsPath());
		if (!image.exists()) {
			boolean res = image.mkdirs();
			V2Log.i(" create image dir " + image.getAbsolutePath() + "  " + res);
		}
		File audioPath = new File(GlobalConfig.getGlobalAudioPath());
		if (!audioPath.exists()) {
			boolean res = audioPath.mkdirs();
			V2Log.i(" create audio dir " + audioPath.getAbsolutePath() + "  "
					+ res);
		}
		File filePath = new File(GlobalConfig.getGlobalFilePath());
		if (!filePath.exists()) {
			boolean res = filePath.mkdirs();
			V2Log.i(" create file dir " + filePath.getAbsolutePath() + "  "
					+ res);
		}

		initConfFile();

		// Load native library
		System.loadLibrary("event");
		System.loadLibrary("v2vi");
		System.loadLibrary("v2ve");
		// System.loadLibrary("NetEvent");
		System.loadLibrary("v2client");

		// Initialize native library
		NativeInitializer.getIntance()
				.initialize(getApplicationContext(), path);
		ImRequest.getInstance(getApplicationContext());
		GroupRequest.getInstance(getApplicationContext());
		VideoRequest.getInstance(getApplicationContext());
		ConfRequest.getInstance(getApplicationContext());
		AudioRequest.getInstance(getApplicationContext());
		WBRequest.getInstance(getApplicationContext());
		ChatRequest.getInstance(getApplicationContext());
		VideoMixerRequest.getInstance();
		FileRequest.getInstance(getApplicationContext());

		// Start deamon service
		getApplicationContext().startService(
				new Intent(getApplicationContext(), JNIService.class));

		if (!V2Log.isDebuggable) {
			new LogcatThread().start();
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			this.registerActivityLifecycleCallbacks(new LocalActivityLifecycleCallBack());
		}

		initGlobalConfiguration();
		initSQLiteFile();
	}

	private void initSQLiteFile() {

		try {
			// 获得.db文件的绝对路径
			String parent = getDatabasePath(DATABASE_FILENAME).getParent();
			File dir = new File(parent);
			// 如果目录不存在，创建这个目录
			if (!dir.exists())
				dir.mkdir();
			String databaseFilename = getDatabasePath(DATABASE_FILENAME)
					.getPath();
			File file = new File(databaseFilename);
			// 目录中不存在 .db文件，则从res\raw目录中复制这个文件到该目录
			if(file.exists()){
				InputStream is = getResources().openRawResource(R.raw.hzpy);
				if (is == null) {
					V2Log.e("readed sqlite file failed... inputStream is null");
					return;
				}
				String md5 = getFileMD5(is);
				String currentMD5 = getFileMD5(new FileInputStream(file));
				if(md5.equals(currentMD5))
					needCopy = false; 
				else
					needCopy = true;
			}
			
			if (!(file.exists()) || needCopy == true) {
				// 获得封装.db文件的InputStream对象
				InputStream is = getResources().openRawResource(R.raw.hzpy);
				if (is == null) {
					V2Log.e("readed sqlite file failed... inputStream is null");
					return;
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
		finally{
			needCopy = false;
		}
	}

	private void initConfFile() {
		// Initialize global configuration file

		File optionsFile = new File(GlobalConfig.getGlobalPath()
				+ "/log_options.xml");
		{
			String content = "<xml><path>log</path><v2platform><outputdebugstring>0</outputdebugstring><level>5</level><basename>v2platform</basename><path>log</path><size>1024</size></v2platform></xml>";
			OutputStream os = null;
			try {
				os = new FileOutputStream(optionsFile);
				os.write(content.getBytes());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (os != null) {
					try {
						os.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		{
			File cfgFile = new File(GlobalConfig.getGlobalPath()
					+ "/v2platform.cfg");
			String contentCFG = "<v2platform><C2SProxy><ipv4 value=''/><tcpport value=''/></C2SProxy></v2platform>";

			OutputStream os = null;
			try {
				os = new FileOutputStream(cfgFile);
				os.write(contentCFG.getBytes());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (os != null) {
					try {
						os.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		V2Log.d(" terminating....");
		ImRequest.getInstance(this).unInitialize();
		GroupRequest.getInstance(this).unInitialize();
		VideoRequest.getInstance(this).unInitialize();
		ConfRequest.getInstance(this).unInitialize();
		AudioRequest.getInstance(this).unInitialize();
		WBRequest.getInstance(this).unInitialize();
		ChatRequest.getInstance(this).unInitialize();
		this.getApplicationContext().stopService(
				new Intent(this.getApplicationContext(), JNIService.class));
		V2Log.d(" terminated");

	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		V2Log.e("=================== low memeory :");
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	public void onTrimMemory(int level) {
		super.onTrimMemory(level);
		V2Log.e("=================== trim memeory :" + level);
	}

	private void initGlobalConfiguration() {
		Configuration conf = getResources().getConfiguration();
		if (conf.smallestScreenWidthDp >= 600) {
			conf.orientation = Configuration.ORIENTATION_LANDSCAPE;
		} else {
			conf.orientation = Configuration.ORIENTATION_PORTRAIT;
		}
	}

	public void requestQuit() {
		for (int i = 0; i < list.size(); i++) {
			WeakReference<Activity> w = list.get(i);
			Object obj = w.get();
			if (obj != null) {
				((Activity) obj).finish();
			}
		}

		Handler h = new Handler();
		h.postDelayed(new Runnable() {

			@Override
			public void run() {
				GlobalConfig.saveLogoutFlag(getApplicationContext());
				Notificator
						.cancelAllSystemNotification(getApplicationContext());
				System.exit(0);
			}

		}, 1000);
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	class LocalActivityLifecycleCallBack implements ActivityLifecycleCallbacks {

		private Object mLock = new Object();
		private int refCount = 0;

		@Override
		public void onActivityCreated(Activity activity,
				Bundle savedInstanceState) {
			Configuration conf = getResources().getConfiguration();
			if (conf.smallestScreenWidthDp >= 600
					|| activity instanceof VideoActivityV2) {
				activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			} else {
				activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			}

			list.add(new WeakReference<Activity>(activity));
		}

		@Override
		public void onActivityDestroyed(Activity activity) {
			for (int i = 0; i < list.size(); i++) {
				WeakReference<Activity> w = list.get(i);
				Object obj = w.get();
				if (obj != null && ((Activity) obj) == activity) {
					list.remove(i--);
				} else {
					list.remove(i--);
				}
			}
		}

		@Override
		public void onActivityPaused(Activity activity) {

		}

		@Override
		public void onActivityResumed(Activity activity) {
		}

		@Override
		public void onActivitySaveInstanceState(Activity activity,
				Bundle outState) {

		}

		@Override
		public void onActivityStarted(Activity activity) {
			if (activity instanceof LoginActivity
					|| activity instanceof StartupActivity) {
				return;
			}
			synchronized (mLock) {

				refCount++;
				if (refCount == 1) {
					Notificator.udpateApplicationNotification(
							getApplicationContext(), false, null);
				}
			}
		}

		@Override
		public void onActivityStopped(Activity activity) {
			if (activity instanceof LoginActivity
					|| activity instanceof StartupActivity) {
				return;
			}
			synchronized (mLock) {
				refCount--;
				if (refCount == 0) {
					Intent i = activity.getIntent();
					i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
							| Intent.FLAG_ACTIVITY_SINGLE_TOP);
					Notificator.udpateApplicationNotification(
							getApplicationContext(), true, i);
				}
			}
		}
	}

	/**
	 * 获取单个文件的MD5值！
	 * 
	 * @param file
	 * @return
	 */
	private String getFileMD5(InputStream ips) {
		if (ips == null) {
			return null;
		}
		MessageDigest digest = null;
		byte buffer[] = new byte[1024];
		int len;
		try {
			digest = MessageDigest.getInstance("MD5");
			while ((len = ips.read(buffer, 0, 1024)) != -1) {
				digest.update(buffer, 0, len);
			}
			ips.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		BigInteger bigInt = new BigInteger(1, digest.digest());
		return bigInt.toString(16);
	}
}
