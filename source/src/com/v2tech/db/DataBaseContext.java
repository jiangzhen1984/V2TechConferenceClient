package com.v2tech.db;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

import com.V2.jni.util.V2Log;
import com.v2tech.util.GlobalConfig;

/**
 * 用于支持对存储在SD卡上的数据库的访问
 **/
public class DataBaseContext extends ContextWrapper {

	private static final String TAG = "DataBaseContext";

	/**
	 * 构造函数
	 * 
	 * @param base
	 *            上下文环境
	 */
	public DataBaseContext(Context base) {
		super(base);
	}

	/**
	 * 获得数据库路径，如果不存在，则创建对象对象
	 * 
	 * @param name
	 * @param mode
	 * @param factory
	 */
	@Override
	public File getDatabasePath(String name) {
		// 判断是否存在sd卡
		boolean sdExist = android.os.Environment.MEDIA_MOUNTED
				.equals(android.os.Environment.getExternalStorageState());
		if (!sdExist) {// 如果不存在,
			V2Log.e(TAG, "创建数据库失败，SD卡不存在，请加载SD卡");
			return null;
		} else {// 如果存在
				// 获取指定的用户数据库路径
				// 判断目录是否存在，不存在则创建该目录
			String dbDir = GlobalConfig.DATABASE_PATH;
			File dirFile = new File(dbDir);
			if (!dirFile.exists())
				dirFile.mkdirs();
			String dbPath = dbDir + name;// 数据库路径
			// 数据库文件是否创建成功
			// 判断文件是否存在，不存在则创建该文件
			File dbFile = new File(dbPath);
			if (!dbFile.exists()) {
				try {
					dbFile.createNewFile();// 创建文件
					dbFile.canRead();
					dbFile.canWrite();
				} catch (IOException e) {
					V2Log.e(TAG, "the createNewFile was failed.....");
					e.printStackTrace();
				}
			}
			return dbFile;
		}
	}

	/**
	 * 重载这个方法，是用来打开SD卡上的数据库的，android 2.3及以下会调用这个方法。
	 * 
	 * @param name
	 * @param mode
	 * @param factory
	 */
	@Override
	public SQLiteDatabase openOrCreateDatabase(String name, int mode,
			SQLiteDatabase.CursorFactory factory) {
		synchronized (DataBaseContext.class) {
			File file = getDatabasePath(name);
			if (file == null)
				throw new RuntimeException("创建数据库失败");
			SQLiteDatabase result = SQLiteDatabase.openOrCreateDatabase(file,
					null);
			return result;
		}
	}

	/**
	 * Android 4.0会调用此方法获取数据库。
	 * 
	 * @see android.content.ContextWrapper#openOrCreateDatabase(java.lang.String,
	 *      int, android.database.sqlite.SQLiteDatabase.CursorFactory,
	 *      android.database.DatabaseErrorHandler)
	 * @param name
	 * @param mode
	 * @param factory
	 * @param errorHandler
	 */
	@Override
	public SQLiteDatabase openOrCreateDatabase(String name, int mode,
			CursorFactory factory, DatabaseErrorHandler errorHandler) {
		synchronized (DataBaseContext.class) {
			File file = getDatabasePath(name);
			if (file == null)
				throw new RuntimeException("创建数据库失败");
			SQLiteDatabase result = SQLiteDatabase.openOrCreateDatabase(file,
					null);
			return result;
		}
	}
}
