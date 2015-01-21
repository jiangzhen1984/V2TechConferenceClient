package com.bizcom.util;

import java.io.IOException;

import com.V2.jni.util.V2Log;

public class LogcatThread extends Thread {

	@Override
	public void run() {
		String logName = StorageUtil.getSdcardPath()+"/v2tech/crash/"+System.currentTimeMillis()+"_adblog.log" ;
    	
    	
    	V2Log.i("Catching log to  "+ logName);;
    	String cmd ="logcat -v time  -f  " + logName;
    	try {
			Runtime.getRuntime().exec(cmd);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
