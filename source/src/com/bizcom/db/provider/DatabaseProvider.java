package com.bizcom.db.provider;

import android.content.Context;

import com.bizcom.db.DataBaseContext;

public class DatabaseProvider {
	
	public static DataBaseContext mContext;
	public static void init(Context context){
		mContext = new DataBaseContext(context);
	}
}
