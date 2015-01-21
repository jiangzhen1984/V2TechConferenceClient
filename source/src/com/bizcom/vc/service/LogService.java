package com.bizcom.vc.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.text.TextUtils;

import com.V2.jni.util.V2Log;
import com.bizcom.util.StorageUtil;

/**
 * 日志服务：输出位置默认存储在SDcard中 , 如果没有SDcard会存储在内存中的安装目录下面。 <br/>
 * 1.本服务默认在SDcard中每天生成一个日志文件 ; 如果是手机内存中则会限制log文件的大小 , 如果单个文件超过10M会自动生成一个新的文件<br/>
 * 2.如果有SDCard的话会将之前存储在手机内存中的文件拷贝到SDCard中 <br/>
 * 3.如果没有SDCard，在安装目录下只保存当前在写日志 <br/>
 * 4.SDcard的装载卸载动作会在步骤2 , 3中切换<br/>
 * 5.SDcard中的日志文件只保存7天 <br/>
 * 6.监视App运行的LogCat日志（sdcard mounted）:sdcard/v2tech/crash/yyyy-MM-dd
 * HHmmss.log(默认)<br/>
 * 7.监视App运行的LogCat日志（sdcardunmounted）:/data/data/包名/crash/yyyy-MM-dd HHmmss.log<br/>
 * 8.本service的运行日志：与App的日志同级目录下 , 名称为log.log
 * 
 * @author Administrator
 */
public class LogService extends Service {
	private static final String TAG = "LogService";

	private static final int MEMORY_LOG_FILE_MAX_SIZE = 10 * 1024 * 1024; // 日志文件最大值，10M
	private static final int MEMORY_LOG_FILE_MONITOR_INTERVAL = 10 * 60 * 1000; // 日志文件大小监控时间间隔，10分钟
	private static final int SDCARD_LOG_FILE_SAVE_DAYS = 1; // sd卡中日志文件的最多保存天数
	private static String MONITOR_LOG_SIZE_ACTION = "MONITOR_LOG_SIZE"; // 日志文件监测action
	private static String SWITCH_LOG_FILE_ACTION = "SWITCH_LOG_FILE_ACTION"; // 切换日志文件action
	/*
	 * 是否正在监测日志文件大小； 如果当前日志记录在SDcard中则为false 如果当前日志记录在内存中则为true
	 */
	private boolean logSizeMoniting = false;

	/**
	 * 日志文件在手机自带内存中的路径(日志文件在安装目录中的路径); example : /data/data/包名/crash
	 */
	private String LOG_PATH_MEMORY_DIR;

	/**
	 * 日志文件在sdcard中的路径
	 */
	private String LOG_PATH_SDCARD_DIR;
	private String LOG_SERVICE_LOG_DIR; // 本服务产生的日志，记录日志服务开启失败信息
	private String LOG_SERVICE_LOG_PATH; // 本服务产生的日志，记录日志服务开启失败信息

	private final int SDCARD_TYPE = 0; // 当前的日志记录类型为存储在SD卡下面
	private final int MEMORY_TYPE = 1; // 当前的日志记录类型为存储在内存中
	private int CURR_LOG_TYPE = SDCARD_TYPE; // 当前的日志记录类型

	private String CURR_INSTALL_LOG_NAME; // 如果当前的日志写在内存中，记录当前的日志文件名称

	/**
	 * 本服务输出的日志文件名称
	 */
	private String logServiceLogName = "log.log";

	/**
	 * 用于格式化日期,用作在log.log日志文件中
	 */
	private SimpleDateFormat myLogSdf = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	/**
	 * 用于格式化日期,作为抓取正常日志的文件的名称的一部分
	 */
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

	private Process process;
	private WakeLock wakeLock;
	private LogTaskReceiver logTaskReceiver;
	private OutputStreamWriter writer;
	private StringBuilder sb = new StringBuilder();

	/**
	 * SDcard状态监测
	 */
	private SDStateMonitorReceiver sdStateReceiver;

	private String currentCatchLogFileName;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		init();
		register();
		deploySwitchLogFileTask();
		new LogCollectorThread().start();
	}

	// This is the new method that instead of the old onStart method on the
	// pre-2.0 platform.
	@Override
	// 开始服务，执行更新widget组件的操作
	public int onStartCommand(Intent intent, int flags, int startId) {

		V2Log.d(TAG,
				"===================onStartCommand===========================");

		// Notification notification = new Notification(R.drawable.logo,
		// "wf log service is running", System.currentTimeMillis());
		// PendingIntent pi = PendingIntent.getService(this, 0, intent, 0);
		// notification.setLatestEventInfo(this, "WF Log Service",
		// "wf log service is running！", pi);
		Notification notification = new Notification();
		// 让该service前台运行，避免手机休眠时系统自动杀掉该服务
		// 如果 id 为 0 ，那么状态栏的 notification 将不会显示。
		startForeground(0, notification);
		V2Log.d(TAG, "Log Service is started successfully !");
		return Service.START_REDELIVER_INTENT;
	}

	private void init() {
		CURR_LOG_TYPE = getCurrLogType();
		// --/data/data/包名/crash/xxx.log
		LOG_PATH_MEMORY_DIR = getFilesDir().getParent() + File.separator
				+ "crash";
		LOG_PATH_SDCARD_DIR = StorageUtil.getSdcardPath() + File.separator
				+ "v2tech" + File.separator + "crash";

		if (CURR_LOG_TYPE == SDCARD_TYPE) {
			LOG_SERVICE_LOG_DIR = LOG_PATH_SDCARD_DIR;
			LOG_SERVICE_LOG_PATH = LOG_SERVICE_LOG_DIR + File.separator
					+ logServiceLogName;
		} else {
			LOG_SERVICE_LOG_DIR = LOG_PATH_MEMORY_DIR;
			LOG_SERVICE_LOG_PATH = LOG_SERVICE_LOG_DIR + File.separator
					+ logServiceLogName;
		}

		createLogDir();
		currentCatchLogFileName = createLogFilePath();
		recordLogServiceLog("Create Cache Log File Name is : "
				+ currentCatchLogFileName);
		new Thread(new Runnable() {

			@Override
			public void run() {

				while (!checkDirExist()) {
					SystemClock.sleep(1000);
					V2Log.e(TAG, "check crash dir exist again !");
				}

				try {
					writer = new OutputStreamWriter(new FileOutputStream(
							LOG_SERVICE_LOG_PATH, true));
				} catch (Exception e) {
					e.printStackTrace();
				}
				recordLogServiceLog("");
				recordLogServiceLog("========================Halving Line========================");
				recordLogServiceLog("Create service log Successfully !  --> Start recording ! mean LogService run normal !");
				recordLogServiceLog("Project already startup now!");
				recordLogServiceLog(sb.toString());
				V2Log.e(TAG, "Successfully start record service log !");
			}

		}).start();

		PowerManager pm = (PowerManager) getApplicationContext()
				.getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		V2Log.d(TAG, "LogService onCreate");
	}

	private String createLogFilePath() {
		return sdf.format(new Date()) + ".log";
	}

	public boolean checkDirExist() {
		File file = new File(LOG_SERVICE_LOG_DIR);
		return file.exists();
	}

	private void register() {
		IntentFilter sdCarMonitorFilter = new IntentFilter();
		sdCarMonitorFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		sdCarMonitorFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		sdCarMonitorFilter.addDataScheme("file");
		sdStateReceiver = new SDStateMonitorReceiver();
		registerReceiver(sdStateReceiver, sdCarMonitorFilter);

		IntentFilter logTaskFilter = new IntentFilter();
		logTaskFilter.addAction(MONITOR_LOG_SIZE_ACTION);
		logTaskFilter.addAction(SWITCH_LOG_FILE_ACTION);
		logTaskReceiver = new LogTaskReceiver();
		registerReceiver(logTaskReceiver, logTaskFilter);
	}

	/**
	 * 部署日志切换任务，每天凌晨切换日志文件
	 */
	private void deploySwitchLogFileTask() {
		Intent intent = new Intent(SWITCH_LOG_FILE_ACTION);
		PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, 0);
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);

		// 部署任务
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
				AlarmManager.INTERVAL_DAY, sender);
		recordLogServiceLog("deployNextTask successfully , next task time is:"
				+ myLogSdf.format(calendar.getTime()));
	}

	/**
	 * 每次记录日志之前先清除日志的缓存, 不然会在两个日志文件中记录重复的日志
	 */
	private void clearLogCache() {
		Process proc = null;
		List<String> commandList = new ArrayList<String>();
		commandList.add("logcat");
		commandList.add("-c");
		try {
			proc = Runtime.getRuntime().exec(
					commandList.toArray(new String[commandList.size()]));
			// StreamConsumer errorGobbler = new StreamConsumer(
			// proc.getErrorStream());
			// errorGobbler.start();
			processWaitFor(proc);
		} catch (Exception e) {
			V2Log.e(TAG, "clearLogCache failed", e);
			recordLogServiceLog("clearLogCache failed");
		} finally {
			try {
				if (proc != null)
					proc.destroy();
				else
					recordLogServiceLog("clearLogCache failed , because process is null..");
			} catch (Exception e) {
				V2Log.e(TAG, "clearLogCache failed", e);
				recordLogServiceLog("clearLogCache failed");
			}
		}
	}

	/**
	 * 关闭由本程序开启的logcat进程： 根据用户名称杀死进程(如果是本程序进程开启的Logcat收集进程那么两者的USER一致)
	 * 如果不关闭会有多个进程读取logcat日志缓存信息写入日志文件
	 * 
	 * @param allProcList
	 * @return
	 */
	private void killLogcatProc(List<ProcessInfo> allProcList) {
		if (process != null) {
			process.destroy();
		}
		String packName = this.getPackageName();
		String myUser = getAppUser(packName, allProcList);
		if (TextUtils.isEmpty(myUser)) {
			recordLogServiceLog("kill another logcat process failed.. , get app user name is null");
			return;
		}
		recordLogServiceLog("app user is:" + myUser);
		recordLogServiceLog("============= START PRINT PROCESS LIST INFO ==============");
		// 只打印本App和logcat的进程信息，其他进程信息不打印
		for (ProcessInfo processInfo : allProcList) {
			if (myUser.equals(processInfo.user)
					|| "logcat".equals(processInfo.name)) {
				recordLogServiceLog(processInfo.toString());
			}
		}
		recordLogServiceLog("============= END PRINT PROCESS LIST INFO ==============");
		for (ProcessInfo processInfo : allProcList) {
			if (processInfo.name.toLowerCase().equals("logcat")
					&& processInfo.user.equals(myUser)) {
				android.os.Process.killProcess(Integer
						.parseInt(processInfo.pid));
				recordLogServiceLog("kill another logcat process success,the process info is:"
						+ processInfo);
			}
		}
	}

	/**
	 * 获取本程序的用户名称
	 * 
	 * @param packName
	 * @param allProcList
	 * @return
	 */
	private String getAppUser(String packName, List<ProcessInfo> allProcList) {
		for (ProcessInfo processInfo : allProcList) {
			if (processInfo.name.equals(packName)) {
				return processInfo.user;
			}
		}
		return null;
	}

	/**
	 * 根据ps命令得到的内容获取PID，User，name等信息
	 * 
	 * @param orgProcessList
	 * @return
	 */
	private List<ProcessInfo> getProcessInfoList(List<String> orgProcessList) {
		List<ProcessInfo> procInfoList = new ArrayList<ProcessInfo>();
		for (int i = 1; i < orgProcessList.size(); i++) {
			String processInfo = orgProcessList.get(i);
			String[] proStr = processInfo.split(" ");
			// USER PID PPID VSIZE RSS WCHAN PC NAME
			// root 1 0 416 300 c00d4b28 0000cd5c S /init
			List<String> orgInfo = new ArrayList<String>();
			for (String str : proStr) {
				if (!"".equals(str)) {
					orgInfo.add(str);
				}
			}
			if (orgInfo.size() == 9) {
				ProcessInfo pInfo = new ProcessInfo();
				pInfo.user = orgInfo.get(0);
				pInfo.pid = orgInfo.get(1);
				pInfo.ppid = orgInfo.get(2);
				pInfo.name = orgInfo.get(8);
				procInfoList.add(pInfo);
			}
		}
		return procInfoList;
	}

	/**
	 * 运行PS命令得到进程信息
	 * 
	 * @return USER PID PPID VSIZE RSS WCHAN PC NAME root 1 0 416 300 c00d4b28
	 *         0000cd5c S /init
	 */
	private List<String> getAllProcess() {
		List<String> orgProcList = new ArrayList<String>();
		Process proc = null;
		try {
			proc = Runtime.getRuntime().exec("ps");
			processWaitFor(proc);
		} catch (Exception e) {
			V2Log.e(TAG, "getAllProcess failed", e);
			recordLogServiceLog("getAllProcess failed");
		} finally {
			try {
				if (proc != null)
					proc.destroy();
				else
					recordLogServiceLog("clearLogCache failed , because process is null..");
			} catch (Exception e) {
				V2Log.e(TAG, "getAllProcess failed", e);
				recordLogServiceLog("getAllProcess failed");
			}
		}
		return orgProcList;
	}

	/**
	 * 获取当前应存储在内存中还是存储在SDCard中
	 * 
	 * @return
	 */
	public int getCurrLogType() {
		if (!Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			return MEMORY_TYPE;
		} else {
			return SDCARD_TYPE;
		}
	}

	/**
	 * 开始收集日志信息
	 * 
	 * @throws IOException
	 */
	public void createLogCollector() throws IOException {
		List<String> commandList = new ArrayList<String>();
		commandList.add("logcat");
		commandList.add("-v");
		commandList.add("time");
		commandList.add("-f");
		commandList.add(getLogFilePath());

		// 过滤所有的错误信息
		// commandList.add("*:I");
		// commandList.add("*:E");
		// 过滤指定TAG的信息
//		commandList.add("MyAPP:V");
//		commandList.add("*:S");

		// String cmd ="logcat -v time -f " + getLogFilePath();

		process = Runtime.getRuntime().exec(
				commandList.toArray(new String[commandList.size()]));
		// process = Runtime.getRuntime().exec(cmd);
		processWaitFor(process);
		recordLogServiceLog("start collecting the log,and log name is:"
				+ getLogFilePath());
	}

	private void processWaitFor(Process process) {
		InputStream stderr = process.getErrorStream();
		InputStreamReader isr = new InputStreamReader(stderr);
		BufferedReader br = new BufferedReader(isr);
		String line;
		try {
			while ((line = br.readLine()) != null)
				System.out.println(line);
			process.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
			recordLogServiceLog("processWaitFor == >" + e.getMessage());
		} catch (InterruptedException e) {
			e.printStackTrace();
			recordLogServiceLog("processWaitFor == >" + e.getMessage());
		}
	}

	/**
	 * 根据当前的存储位置得到日志的绝对存储路径
	 * 
	 * @return
	 */
	public String getLogFilePath() {
		if (CURR_LOG_TYPE == MEMORY_TYPE) {
			CURR_INSTALL_LOG_NAME = currentCatchLogFileName;
			recordLogServiceLog("Log stored in memory, the path is:"
					+ LOG_PATH_MEMORY_DIR + File.separator
					+ currentCatchLogFileName);
			return LOG_PATH_MEMORY_DIR + File.separator
					+ currentCatchLogFileName;
		} else {
			CURR_INSTALL_LOG_NAME = null;
			recordLogServiceLog("Log stored in SDcard, the path is:"
					+ LOG_PATH_SDCARD_DIR + File.separator
					+ currentCatchLogFileName);
			return LOG_PATH_SDCARD_DIR + File.separator
					+ currentCatchLogFileName;
		}
	}

	/**
	 * 获取log日志的文件名
	 * 
	 * @return
	 */
	// public String getLogFileName() {
	// return sdf.format(new Date()) + ".log";
	// }

	/**
	 * 处理日志文件 1.如果日志文件存储位置切换到内存中，删除除了正在写的日志文件 并且部署日志大小监控任务，控制日志大小不超过规定值
	 * 2.如果日志文件存储位置切换到SDCard中，删除7天之前的日志，移 动所有存储在内存中的日志到SDCard中，并将之前部署的日志大小 监控取消
	 */
	public void handleLog() {
		if (CURR_LOG_TYPE == MEMORY_TYPE) {
			deployLogSizeMonitorTask();
			deleteMemoryExpiredLog();
		} else {
			moveLogfileToSDCard();
			cancelLogSizeMonitorTask();
			deleteSDcardExpiredLog();
		}
	}

	/**
	 * 部署日志大小监控任务
	 */
	private void deployLogSizeMonitorTask() {
		if (logSizeMoniting) { // 如果当前正在监控着，则不需要继续部署
			return;
		}
		logSizeMoniting = true;
		Intent intent = new Intent(MONITOR_LOG_SIZE_ACTION);
		PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, 0);
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),
				MEMORY_LOG_FILE_MONITOR_INTERVAL, sender);
		V2Log.d(TAG, "deployLogSizeMonitorTask() succ !");
		Calendar calendar = Calendar.getInstance();
		recordLogServiceLog("deployLogSizeMonitorTask() succ ,start time is "
				+ calendar.getTime().toLocaleString());
	}

	/**
	 * 取消部署日志大小监控任务
	 */
	private void cancelLogSizeMonitorTask() {
		logSizeMoniting = false;
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		Intent intent = new Intent(MONITOR_LOG_SIZE_ACTION);
		PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, 0);
		am.cancel(sender);

		V2Log.d(TAG, "canelLogSizeMonitorTask() succ");
	}

	/**
	 * 检查日志文件大小是否超过了规定大小 如果超过了重新开启一个日志收集进程
	 */
	private void checkLogSize() {
		if (CURR_INSTALL_LOG_NAME != null && !"".equals(CURR_INSTALL_LOG_NAME)) {
			String path = LOG_PATH_MEMORY_DIR + File.separator
					+ CURR_INSTALL_LOG_NAME;
			File file = new File(path);
			if (!file.exists()) {
				return;
			}
			V2Log.d(TAG, "checkLog() ==> The size of the log is too big?");
			if (file.length() >= MEMORY_LOG_FILE_MAX_SIZE) {
				V2Log.d(TAG, "The log's size is too big!");
				new LogCollectorThread().start();
			}
		}
	}

	/**
	 * 将日志文件从机身自带内存中转移到SD卡下面
	 */
	private void moveLogfileToSDCard() {
		if (!Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			recordLogServiceLog("move file failed, sd card does not mount");
			return;
		}
		File file = new File(LOG_PATH_SDCARD_DIR);
		if (!file.isDirectory()) {
			boolean mkOk = file.mkdirs();
			if (!mkOk) {
				recordLogServiceLog("move file failed,dir is not created succ");
				return;
			}
		}

		file = new File(LOG_PATH_MEMORY_DIR);
		if (file.isDirectory()) {
			File[] allFiles = file.listFiles();
			for (File logFile : allFiles) {
				String fileName = logFile.getName();
				// 只copy App的LocCat日志文件【yyyy-MM-dd
				// HHmmss.log】到sdcard,本LogService的运行日志【V2Log.log】不做copy对象
				if (logServiceLogName.equals(fileName)) {
					continue;
				}
				// String createDateInfo =
				// getFileNameWithoutExtension(fileName);
				boolean isSucc = copy(logFile, new File(LOG_PATH_SDCARD_DIR
						+ File.separator + fileName));
				if (isSucc) {
					boolean result = logFile.delete();
					if (result)
						recordLogServiceLog("move file success , log name is:"
								+ fileName);
					else
						recordLogServiceLog("move file failed ,log name is:"
								+ fileName);
				}
			}
		}
	}

	/**
	 * 删除SDCard下过期的日志
	 */
	private void deleteSDcardExpiredLog() {
		File file = new File(LOG_PATH_SDCARD_DIR);
		if (file.isDirectory()) {
			File[] allFiles = file.listFiles();
			for (File logFile : allFiles) {
				String fileName = logFile.getName();
				if (logServiceLogName.equals(fileName)) {
					continue;
				}
				String createDateInfo = getFileNameWithoutExtension(fileName);
				if (canDeleteSDLog(createDateInfo)) {
					logFile.delete();
					V2Log.d(TAG, "delete expired log success,the log path is:"
							+ logFile.getAbsolutePath());

				}
			}
		}
	}

	/**
	 * 判断sdcard上的日志文件是否可以删除
	 * 
	 * @param createDateStr
	 * @return
	 */
	public boolean canDeleteSDLog(String createDateStr) {
		boolean canDel;
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_MONTH, -1 * SDCARD_LOG_FILE_SAVE_DAYS);// 删除7天之前日志
		Date expiredDate = calendar.getTime();
		try {
			Date createDate = sdf.parse(createDateStr);
			canDel = createDate.before(expiredDate);
		} catch (ParseException e) {
			V2Log.e(TAG, e.getMessage(), e);
			canDel = false;
		}
		return canDel;
	}

	/**
	 * 删除内存中的过期日志，删除规则： 除了当前的日志和离当前时间最近的日志保存其他的都删除
	 */
	private void deleteMemoryExpiredLog() {
		File file = new File(LOG_PATH_MEMORY_DIR);
		if (file.isDirectory()) {
			File[] allFiles = file.listFiles();
			Arrays.sort(allFiles, new FileComparator());
			for (int i = 0; i < allFiles.length - 2; i++) { // "-2"保存最近的两个日志文件
				File _file = allFiles[i];
				if (logServiceLogName.equals(_file.getName())
						|| _file.getName().equals(CURR_INSTALL_LOG_NAME)) {
					continue;
				}
				_file.delete();
				V2Log.d(TAG, "delete expired log success,the log path is:"
						+ _file.getAbsolutePath());
			}
		}
	}

	/**
	 * 拷贝文件
	 * 
	 * @param source
	 * @param target
	 * @return
	 */
	private boolean copy(File source, File target) {
		FileInputStream in = null;
		FileOutputStream out = null;
		try {
			if (!target.exists()) {
				boolean createSucc = target.createNewFile();
				if (!createSucc) {
					return false;
				}
			}
			in = new FileInputStream(source);
			out = new FileOutputStream(target);
			byte[] buffer = new byte[8 * 1024];
			int count;
			while ((count = in.read(buffer)) != -1) {
				out.write(buffer, 0, count);
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			V2Log.e(TAG, e.getMessage(), e);
			recordLogServiceLog("copy file fail");
			return false;
		} finally {
			try {
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
				V2Log.e(TAG, e.getMessage(), e);
				recordLogServiceLog("copy file fail");
			}
		}

	}

	/**
	 * 记录日志服务的基本信息 防止日志服务有错，在LogCat日志中无法查找 此日志名称为Log.log
	 * 
	 * @param msg
	 */
	private void recordLogServiceLog(String msg) {
		if (writer != null) {
			try {
				Date time = new Date();
				writer.write(myLogSdf.format(time) + " : " + msg);
				writer.write("\n");
				writer.flush();
			} catch (IOException e) {
				e.printStackTrace();
				V2Log.e(TAG, e.getMessage(), e);
			}
		} else {
			sb.append(msg).append("\n");
		}
	}

	/**
	 * 去除文件的扩展类型（.log）
	 * 
	 * @param fileName
	 * @return
	 */
	private String getFileNameWithoutExtension(String fileName) {
		return fileName.substring(0, fileName.indexOf("."));
	}

	/**
	 * 日志收集 1.清除日志缓存 2.杀死应用程序已开启的Logcat进程防止多个进程写入一个日志文件 3.开启日志收集进程 4.处理日志文件 移动
	 * OR 删除
	 */
	class LogCollectorThread extends Thread {

		public LogCollectorThread() {
			super("LogCollectorThread");
			V2Log.d(TAG, "LogCollectorThread is create");
		}

		@Override
		public void run() {
			// 唤醒手机
			wakeLock.acquire();

			clearLogCache();

			List<String> orgProcessList = getAllProcess();
			List<ProcessInfo> processInfoList = getProcessInfoList(orgProcessList);
			killLogcatProc(processInfoList);

			try {
				createLogCollector();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("createLogCollector : "
						+ e.getMessage());
			}
			// 休眠，创建文件，然后处理文件，不然该文件还没创建，会影响文件删除
			SystemClock.sleep(1000);

			handleLog();

			wakeLock.release(); // 释放
		}
	}

	class ProcessInfo {
		public String user;
		public String pid;
		public String ppid;
		public String name;

		@Override
		public String toString() {
			String str = "user=" + user + " pid=" + pid + " ppid=" + ppid
					+ " name=" + name;
			return str;
		}
	}

	/**
	 * 监控SD卡状态
	 * 
	 * @author Administrator
	 * 
	 */
	class SDStateMonitorReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {

			if (Intent.ACTION_MEDIA_UNMOUNTED.equals(intent.getAction())) { // 存储卡被卸载
				if (CURR_LOG_TYPE == SDCARD_TYPE) {
					V2Log.d(TAG,
							"SDcard is UNMOUNTED , failed stored to SDcard !");
					CURR_LOG_TYPE = MEMORY_TYPE;
					new LogCollectorThread().start();
				}
			} else { // 存储卡被挂载
				if (CURR_LOG_TYPE == MEMORY_TYPE) {
					V2Log.d(TAG,
							"SDcard is MOUNTED , stored to SDcard successfully !");
					CURR_LOG_TYPE = SDCARD_TYPE;
					new LogCollectorThread().start();

				}
			}
		}
	}

	/**
	 * 日志任务接收 切换日志，监控日志大小
	 * 
	 * @author Administrator
	 * 
	 */
	class LogTaskReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (SWITCH_LOG_FILE_ACTION.equals(action)) {
				new LogCollectorThread().start();
			} else if (MONITOR_LOG_SIZE_ACTION.equals(action)) {
				checkLogSize();
			}
		}
	}

	class FileComparator implements Comparator<File> {
		public int compare(File file1, File file2) {
			if (logServiceLogName.equals(file1.getName())) {
				return -1;
			} else if (logServiceLogName.equals(file2.getName())) {
				return 1;
			}

			String createInfo1 = getFileNameWithoutExtension(file1.getName());
			String createInfo2 = getFileNameWithoutExtension(file2.getName());

			try {
				Date create1 = sdf.parse(createInfo1);
				Date create2 = sdf.parse(createInfo2);
				if (create1.before(create2)) {
					return -1;
				} else {
					return 1;
				}
			} catch (ParseException e) {
				return 0;
			}
		}
	}

	@Override
	public void onDestroy() {
		// 对于通过startForeground启动的service，需要通过stopForeground来取消前台运行状态
		stopForeground(true);
		// super.onDestroy();
		recordLogServiceLog("LogService onDestroy");
		if (writer != null) {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (process != null) {
			process.destroy();
		}

		unregisterReceiver(sdStateReceiver);
		unregisterReceiver(logTaskReceiver);
	}

	/**
	 * 创建日志目录
	 */
	private void createLogDir() {

		File file;
		if (CURR_LOG_TYPE == SDCARD_TYPE) {
			try {
				file = new File(LOG_PATH_SDCARD_DIR);
				if (!file.exists()) {
					boolean isCreated = file.mkdir();
					if (!isCreated) {
						V2Log.e(TAG, "create sdcard crash dir failed!");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				V2Log.e(TAG, e.getMessage(), e);
				recordLogServiceLog("create log file failed , dir is not created successful");
			}
		} else {
			file = new File(LOG_PATH_MEMORY_DIR);
			if (!file.exists()) {
				try {
					if (!file.exists()) {
						boolean isCreated = file.mkdir();
						if (!isCreated) {
							V2Log.e(TAG, "create memory crash dir failed!");
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					V2Log.e(TAG, e.getMessage(), e);
					recordLogServiceLog("create log file failed , dir is not created successful");
				}
			}
		}
	}
}
