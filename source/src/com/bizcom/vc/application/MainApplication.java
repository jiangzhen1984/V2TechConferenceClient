package com.bizcom.vc.application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Vector;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

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
import com.bizcom.db.provider.DatabaseProvider;
import com.bizcom.util.AlgorithmUtil;
import com.bizcom.util.BitmapUtil;
import com.bizcom.util.CrashHandler;
import com.bizcom.util.FileUitls;
import com.bizcom.util.Notificator;
import com.bizcom.util.StorageUtil;
import com.bizcom.vc.activity.conference.ConferenceActivity;
import com.bizcom.vc.activity.conversation.MessageBuilder;
import com.bizcom.vc.activity.conversation.MessageLoader;
import com.bizcom.vc.activity.main.LoginActivity;
import com.bizcom.vc.activity.main.SplashActivity;
import com.bizcom.vc.service.JNIService;
import com.bizcom.vc.service.LogService;
import com.v2tech.R;

public class MainApplication extends Application {

	private static final String TAG = "MainApplication";
	private Vector<WeakReference<Activity>> list = new Vector<WeakReference<Activity>>();
	private final String DATABASE_FILENAME = "hzpy.db";
	private boolean needCopy;
	public boolean isPad = false;
	private int startedActivityCount = 0;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i("20150211 1", "MainApplication onCreate()");

		if (getResources().getConfiguration().smallestScreenWidthDp >= 600) {
			isPad = true;
		} else {
			isPad = false;
		}

		V2Log.isDebuggable = (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
		V2Log.d(TAG, "isDebuggable : " + V2Log.isDebuggable);

		try {
			GlobalConfig.GLOBAL_VERSION_NAME = this.getPackageManager()
					.getPackageInfo(this.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		initGloblePath();
		initConfigSP();
		initConfigFile();

		DatabaseProvider.init(getApplicationContext());
		MessageBuilder.init(getApplicationContext());
		MessageLoader.init(getApplicationContext());
		BitmapUtil.init(getApplicationContext());
		FileUitls.init(getApplicationContext());

		// Load native library
		System.loadLibrary("event");
		System.loadLibrary("v2vi");
		System.loadLibrary("v2ve");
		System.loadLibrary("v2client");

		// Initialize native library
		NativeInitializer.getIntance().initialize(getApplicationContext(),
				GlobalConfig.getGlobalPath());
		ImRequest.getInstance(getApplicationContext());
		GroupRequest.getInstance();
		VideoRequest.getInstance(getApplicationContext());
		ConfRequest.getInstance(getApplicationContext());
		AudioRequest.getInstance(getApplicationContext());
		WBRequest.getInstance(getApplicationContext());
		ChatRequest.getInstance(getApplicationContext());
		VideoMixerRequest.getInstance();
		FileRequest.getInstance(getApplicationContext());

		// Start service
		getApplicationContext().startService(
				new Intent(getApplicationContext(), JNIService.class));

		if (!V2Log.isDebuggable) {
			// log1
			CrashHandler crashHandler = CrashHandler.getInstance();
			crashHandler.init(getApplicationContext());
			// log2
			getApplicationContext().startService(
					new Intent(getApplicationContext(), LogService.class));

		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			this.registerActivityLifecycleCallbacks(new LocalActivityLifecycleCallBack());
		}

		initHZPYDBFile();
		initDPI();
		initResource();

	}

	private void initConfigSP() {
		SharedPreferences sp = getSharedPreferences("config",
				Context.MODE_PRIVATE);

		Editor ed = sp.edit();
		ed.putInt("LoggedIn", 0);
		ed.commit();

		boolean isAppFirstLoad = sp.getBoolean("isAppFirstLoad", true);
		if (isAppFirstLoad) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					File file = new File(GlobalConfig.getGlobalPath());
					if (file.exists()) {
						recursionDeleteOlderFiles(file);
					}
				}

				private void recursionDeleteOlderFiles(File file) {
					File[] files = file.listFiles();
					if (files != null) {
						for (int i = 0; i < files.length; i++) {
							File temp = files[i];
							if (temp.exists()) {
								if (temp.isDirectory()) {
									recursionDeleteOlderFiles(temp);
								} else {
									V2Log.d(TAG,
											"成功删除文件：" + temp.getAbsolutePath());
									temp.delete();
								}
							}
						}
					}
				}
			}).start();

			Editor editor = sp.edit();
			editor.putBoolean("isAppFirstLoad", false);
			editor.commit();
		}
	}

	private void initResource() {
		GlobalConfig.Resource.CONTACT_DEFAULT_GROUP_NAME = this
				.getApplicationContext().getResources()
				.getText(R.string.contacts_default_group_name).toString();

	}

	/**
	 * 初始化程序数据存储目录
	 */
	private void initGloblePath() {
		// 程序启动时检测SD卡状态
		boolean sdExist = android.os.Environment.MEDIA_MOUNTED
				.equals(android.os.Environment.getExternalStorageState());
		if (!sdExist) {// 如果不存在,
			// --data/data/com.v2tech
			GlobalConfig.DEFAULT_GLOBLE_PATH = getApplicationContext()
					.getFilesDir().getParent();
			V2Log.d(TAG, "SD卡状态异常，数据存储到手机内存中 , 存储路径为："
					+ GlobalConfig.DEFAULT_GLOBLE_PATH);
		} else {
			GlobalConfig.SDCARD_GLOBLE_PATH = StorageUtil
					.getAbsoluteSdcardPath();
			V2Log.d(TAG, "SD卡状态正常，数据存储到SD卡内存中 , 存储路径为："
					+ GlobalConfig.SDCARD_GLOBLE_PATH);
		}
	}

	/**
	 * 初始化搜索用到的hzpy.db文件
	 */
	private void initHZPYDBFile() {

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
			if (file.exists()) {
				InputStream is = getResources().openRawResource(R.raw.hzpy);
				if (is == null) {
					V2Log.e("readed sqlite file failed... inputStream is null");
					return;
				}
				String md5 = AlgorithmUtil.getFileMD5(is);
				String currentMD5 = AlgorithmUtil
						.getFileMD5(new FileInputStream(file));
				if (md5.equals(currentMD5))
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
		} finally {
			needCopy = false;
		}
	}

	private void initConfigFile() {
		// Initialize global configuration file
		File path = new File(GlobalConfig.getGlobalPath());
		if (!path.exists()) {
			path.mkdir();
		}

		File optionsFile = new File(path, "log_options.xml");
		if (!optionsFile.exists()) {
			try {
				optionsFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

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

		File cfgFile = new File(GlobalConfig.getGlobalPath()
				+ "/v2platform.cfg");
		String contentCFG = "<v2platform><C2SProxy><ipv4 value=''/><tcpport value=''/></C2SProxy></v2platform>";

		OutputStream os1 = null;
		try {
			os1 = new FileOutputStream(cfgFile);
			os1.write(contentCFG.getBytes());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (os1 != null) {
				try {
					os1.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private void initDPI() {
		DisplayMetrics metrics = new DisplayMetrics();
		WindowManager manager = (WindowManager) this.getApplicationContext()
				.getSystemService(Context.WINDOW_SERVICE);
		manager.getDefaultDisplay().getMetrics(metrics);
		GlobalConfig.GLOBAL_DPI = metrics.densityDpi;
		V2Log.i("Init user device DPI: " + GlobalConfig.GLOBAL_DPI);
		DisplayMetrics dm = new DisplayMetrics();
		manager.getDefaultDisplay().getMetrics(dm);
		double x = Math.pow(dm.widthPixels / dm.xdpi, 2);
		double y = Math.pow(dm.heightPixels / dm.ydpi, 2);
		GlobalConfig.SCREEN_INCHES = Math.sqrt(x + y);
	}

	@Override
	public void onLowMemory() {
		// 后台进程已经被全部回收，但系统内存还是低
		V2Log.e(TAG, "MainApplication.onLowMemory()");
		super.onLowMemory();
	}

	@Override
	public void onTrimMemory(int level) {
		V2Log.e(TAG, "onTrimMemory called , level is : " + level);
		switch (level) {
		case Application.TRIM_MEMORY_RUNNING_MODERATE:
			break;
		case Application.TRIM_MEMORY_RUNNING_LOW:
			break;
		case Application.TRIM_MEMORY_RUNNING_CRITICAL:
			break;
		case Application.TRIM_MEMORY_UI_HIDDEN:
			break;
		case Application.TRIM_MEMORY_BACKGROUND:
			break;
		case Application.TRIM_MEMORY_MODERATE:
			break;
		case Application.TRIM_MEMORY_COMPLETE:// 下个被回收进程就是此进程
			break;
		}

		super.onTrimMemory(level);
	}

	public void requestQuit() {
		for (int i = 0; i < list.size(); i++) {
			WeakReference<Activity> w = list.get(i);
			Object obj = w.get();
			if (obj != null) {
				((Activity) obj).finish();
			}
		}
	}

	public void uninitForExitProcess() {
		super.onTerminate();
		V2Log.d("uninitForExit....");

		this.getApplicationContext().stopService(
				new Intent(this.getApplicationContext(), JNIService.class));
		this.getApplicationContext().stopService(
				new Intent(this.getApplicationContext(), LogService.class));
		GlobalConfig.saveLogoutFlag(getApplicationContext());
		Notificator.cancelAllSystemNotification(getApplicationContext());
		ImRequest.getInstance(this).unInitialize();
		GroupRequest.getInstance().unInitialize();
		VideoRequest.getInstance(this).unInitialize();
		ConfRequest.getInstance(this).unInitialize();
		AudioRequest.getInstance(this).unInitialize();
		WBRequest.getInstance(this).unInitialize();
		ChatRequest.getInstance(this).unInitialize();

		V2Log.d("uninitForExited");

		System.exit(0);

	}

	/**
	 * This function is used to determine whether the program runs in the background
	 * @return true mean running in the backgroup , otherwise false.
	 */
	public boolean isRunningBackgound() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			if (startedActivityCount == 0) {
				return true;
			} else {
				return false;
			}
		} else {
			ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
			List<RunningTaskInfo> tasks = am.getRunningTasks(1);
			if (!tasks.isEmpty()) {
				ComponentName topActivity = tasks.get(0).topActivity;
				if (!topActivity.getPackageName().equals(getPackageName())) {
					return true;
				}
			}
			return false;
		}

	}

	// public static boolean isApplicationBackground(Context context) {
	// ActivityManager am = (ActivityManager) context
	// .getSystemService(Context.ACTIVITY_SERVICE);
	// List<RunningTaskInfo> tasks = am.getRunningTasks(1);
	// if (!tasks.isEmpty()) {
	// ComponentName topActivity = tasks.get(0).topActivity;
	// if (!topActivity.getPackageName().equals(context.getPackageName())) {
	// return true;
	// }
	// }
	// return false;
	// }

	class LocalActivityLifecycleCallBack implements ActivityLifecycleCallbacks {

		@Override
		public void onActivityCreated(Activity activity,
				Bundle savedInstanceState) {
			// 为什么要该声音模式？暂时注释
//			 activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);

			if (isPad || activity instanceof ConferenceActivity) {
				activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			} else {
				activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			}
			list.add(0, new WeakReference<Activity>(activity));
			V2Log.d(TAG, "MainApplication 添加一个activity : "
					+ activity.getClass().getName());

		}

		@Override
		public void onActivityDestroyed(Activity activity) {
			for (int i = 0; i < list.size(); i++) {
				WeakReference<Activity> w = list.get(i);
				Object obj = w.get();
				if (obj != null && ((Activity) obj) == activity) {
					list.remove(i--);
					V2Log.d(TAG, "MainApplication 删除一个activity : "
							+ activity.getClass().getName());
				}
			}

			// activity.setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);

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
					|| activity instanceof SplashActivity) {
				return;
			}
			// 测试证明activity跳转时，先start后stop
			startedActivityCount++;
			if (startedActivityCount == 1) {

			}

		}

		@Override
		public void onActivityStopped(Activity activity) {
			if (activity instanceof LoginActivity
					|| activity instanceof SplashActivity) {
				return;
			}
			startedActivityCount--;
		}
	}
}
