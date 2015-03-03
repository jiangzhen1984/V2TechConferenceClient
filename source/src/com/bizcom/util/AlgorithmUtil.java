package com.bizcom.util;

import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

public class AlgorithmUtil {

	private static long lastClickTime;

	/**
	 * 获取单个文件的MD5值！
	 * 
	 * @param file
	 * @return
	 */
	public static String getFileMD5(InputStream ips) {
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

	public synchronized static boolean isFastClick() {
		long time = System.currentTimeMillis();
		if (time - lastClickTime < 500) {
			return true;
		}
		lastClickTime = time;
		return false;
	}
}
