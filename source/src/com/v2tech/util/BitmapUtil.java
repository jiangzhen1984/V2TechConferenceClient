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
		BitmapFactory.Options opt = null;
		if (GlobalConfig.GLOBAL_DPI < DisplayMetrics.DENSITY_XHIGH) {
			opt = new BitmapFactory.Options();
			opt.inSampleSize = 2;
		}
		return BitmapFactory.decodeFile(path, opt);
	}
}
