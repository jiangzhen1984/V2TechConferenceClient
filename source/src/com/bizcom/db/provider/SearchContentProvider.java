package com.bizcom.db.provider;

import java.io.File;
import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class SearchContentProvider {

	private final static String DATABASE_FILENAME = "hzpy.db";
	private static final String table = "HZToPY";
	private static final String[] columns = new String[] { "HZ", "PY" };
	private static SQLiteDatabase dateBase;

	private static void init(Context mContext) {
		File dbFile = mContext.getDatabasePath(DATABASE_FILENAME);
		dateBase = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
	}

	public static String queryChineseToEnglish(Context mContext,
			String selection, String[] selectionArgs) {
		Cursor cursor = null;
		try {
			if (dateBase == null || !dateBase.isOpen()) {
				init(mContext);
			}

			cursor = dateBase.query(table, columns, selection, selectionArgs,
					null, null, null);
			if (cursor != null && cursor.getCount() > 0 && cursor.moveToNext()) {
				return cursor.getString(cursor.getColumnIndex("PY"));
			}
			return null;
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}

	public static HashMap<String, String> queryAll(Context mContext) {
		Cursor cursor = null;
		try {
			if (dateBase == null || !dateBase.isOpen()) {
				init(mContext);
			}
			HashMap<String, String> tempMap = new HashMap<String, String>();
			cursor = dateBase.query(table, columns, null, null, null, null,
					null);
			if (cursor != null && cursor.getCount() > 0) {
				while (cursor.moveToNext()) {

					String chinese = cursor.getString(cursor
							.getColumnIndex("HZ"));
					String english = cursor.getString(cursor
							.getColumnIndex("PY"));
					tempMap.put(chinese, english);
				}
				return tempMap;
			}
			return null;
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}

	public static void closedDataBase() {
		if (dateBase != null && dateBase.isOpen())
			dateBase.close();
	}
}
