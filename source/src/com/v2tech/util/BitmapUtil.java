package com.v2tech.util;

import java.io.File;

import com.V2.jni.util.V2Log;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;

public class BitmapUtil {

	public static Bitmap loadAvatarFromPath(String path) {
		if (path == null) {
			return null;
		}
		File f = new File(path);
		if (!f.exists() || f.isDirectory()) {
			return null;
		}

		BitmapFactory.Options opt = new BitmapFactory.Options();
		Bitmap tmep = BitmapFactory.decodeFile(path, opt);
		if (tmep == null) {
			V2Log.i(" bitmap object is null");
			return null;
		}

		Bitmap avatar = null;

		if (GlobalConfig.GLOBAL_DPI == DisplayMetrics.DENSITY_HIGH) {
			avatar = Bitmap.createScaledBitmap(tmep, 70, 70, true);
		} else if (GlobalConfig.GLOBAL_DPI == DisplayMetrics.DENSITY_XHIGH) {
			avatar = Bitmap.createScaledBitmap(tmep, 115, 115, true);
		} else if (GlobalConfig.GLOBAL_DPI == DisplayMetrics.DENSITY_XXHIGH) {
			avatar = Bitmap.createScaledBitmap(tmep, 115, 115, true);
		} else {
			avatar = Bitmap.createScaledBitmap(tmep, 70, 70, true);
		}
		tmep.recycle();
		V2Log.d("decode result: width " + avatar.getWidth() + "  height:"
				+ avatar.getHeight());

		return avatar;
	}

	public static Bitmap getCompressedBitmap(String file) {
		if (file == null) {
			throw new NullPointerException(" file is null");
		}
		File f = new File(file);
		if (!f.exists()) {
			throw new RuntimeException(" file is no exists :" + file);
		}

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		options.inPreferredConfig = Bitmap.Config.ALPHA_8;
		BitmapFactory.decodeFile(file, options);
		if (options.outWidth >= 1080 || options.outHeight >= 1080) {
			options.inSampleSize = 8;
		} else if (options.outWidth > 500 || options.outHeight > 500) {
			options.inSampleSize = 4;
		} else {
			options.inSampleSize = 2;
		}
		options.inJustDecodeBounds = false;
		options.inInputShareable = true;// 。当系统内存不够时候图片自动被回收
		options.inPurgeable = true;
		Bitmap bp = BitmapFactory.decodeFile(file, options);
		return bp;
	}

	public static void getCompressedBitmapBounds(String file, int[] r) {

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		options.inPreferredConfig = Bitmap.Config.ALPHA_8;
		BitmapFactory.decodeFile(file, options);
		if (options.outWidth >= 1080 || options.outHeight >= 1080) {
			options.inSampleSize = 8;
		} else if (options.outWidth > 500 || options.outHeight > 500) {
			options.inSampleSize = 4;
		} else if (options.outWidth > 200 || options.outHeight > 200) {
			options.inSampleSize = 2;
		} else {
			options.inSampleSize = 1;
		}
		BitmapFactory.decodeFile(file, options);
		r[0] = options.outWidth;
		r[1] = options.outHeight;
	}

	public static void getFullBitmapBounds(String file, int[] r) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		options.inPreferredConfig = Bitmap.Config.ALPHA_8;
		BitmapFactory.decodeFile(file, options);
		r[0] = options.outWidth;
		r[1] = options.outHeight;
	}

}
