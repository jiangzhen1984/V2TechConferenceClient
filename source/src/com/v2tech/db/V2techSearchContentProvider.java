package com.v2tech.db;

import java.io.File;
import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class V2techSearchContentProvider {

	private final static String DATABASE_FILENAME = "HZPY.db";
	private static final String table = "HZToPY";
	private static final String[] columns = new String[]{"HZ" , "PY"};
	private static SQLiteDatabase dateBase; 
	
	private static void init(Context mContext) {
		File dbFile = mContext.getDatabasePath(DATABASE_FILENAME);
		dateBase = SQLiteDatabase.openOrCreateDatabase(dbFile , null);
	}

	public static String queryChineseToEnglish(Context mContext , String selection , String[] selectionArgs){
		if(dateBase == null || !dateBase.isOpen()){
			init(mContext);
		}
		Cursor cursor = dateBase.query(table, columns, selection, selectionArgs, null, null, null);
		if(cursor != null && cursor.getCount() > 0 && cursor.moveToNext()) {
			String string = cursor.getString(cursor.getColumnIndex("PY"));
			cursor.close();
			return string;
		}
		else
			cursor.close();
			return null;
	}
	
	public static HashMap<String , String> queryAll(Context mContext){
		
		if(dateBase == null || !dateBase.isOpen()){
			init(mContext);
		}
		HashMap<String , String> tempMap = new HashMap<String , String>();
		Cursor cursor = dateBase.query(table, columns, null, null, null, null, null);
		if(cursor != null && cursor.getCount() > 0) {
			while(cursor.moveToNext()){
				
				String chinese = cursor.getString(cursor.getColumnIndex("HZ"));
				String english = cursor.getString(cursor.getColumnIndex("PY"));
				tempMap.put(chinese, english);
			}
			cursor.close();
			return tempMap;
		}
		else
			cursor.close();
		return null;
	}
	
	public static void closedDataBase(){
		if(dateBase != null && dateBase.isOpen())
			dateBase.close();
	}
}
