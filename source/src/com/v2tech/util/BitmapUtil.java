package com.v2tech.util;

import java.io.File;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;

public class BitmapUtil {

	
	public static Bitmap  loadAvatarFromPath(String path) {
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
			avatar = Bitmap.createScaledBitmap(tmep, 60, 60, true);
		} else if (GlobalConfig.GLOBAL_DPI == DisplayMetrics.DENSITY_XHIGH) {
			avatar = Bitmap.createScaledBitmap(tmep, 100, 100, true);
		} else if (GlobalConfig.GLOBAL_DPI == DisplayMetrics.DENSITY_XXHIGH) {
			avatar = Bitmap.createScaledBitmap(tmep, 100, 100, true);
		} else {
			avatar = Bitmap.createScaledBitmap(tmep, 60, 60, true);
		}
		tmep.recycle();
		V2Log.d("decode result: width " + avatar.getWidth() + "  height:"
				+ avatar.getHeight());
		
		return avatar;
	}
	
}
