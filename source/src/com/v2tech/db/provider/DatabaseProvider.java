package com.v2tech.db.provider;

import android.content.Context;

import com.v2tech.db.DataBaseContext;

public class DatabaseProvider {
	
	public static DataBaseContext mContext;
	public static void init(Context context){
		mContext = new DataBaseContext(context);
	}
}
