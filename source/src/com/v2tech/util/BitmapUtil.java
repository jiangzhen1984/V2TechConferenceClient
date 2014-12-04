package com.v2tech.util;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import com.V2.jni.util.V2Log;
import com.v2tech.R;

public class BitmapUtil {

    public static Context context;
	public static Bitmap loadAvatarFromPath(String path) {
        boolean isOwnerAvatar = true;
		if (path == null) {
            isOwnerAvatar = false;
        }
		else {
            File f = new File(path);
            if (!f.exists()) {
                isOwnerAvatar = false;
                V2Log.e("BitmapUtil loadAvatarFromPath --> FAILED! Because parse Path , file isn't exist! path is : " + path);
                File parent = new File(f.getParent());
                File[] listFiles = parent.listFiles();
                for (File file : listFiles) {
                    V2Log.e("BitmapUtil loadAvatarFromPath --> current directory file list is : " + file.getName());
                }
            }

            if (f.isDirectory()) {
                isOwnerAvatar = false;
                V2Log.e("BitmapUtil loadAvatarFromPath --> FAILED! Because parse Path , get a Directory! path is : " + path);
            }
        }
		
		Bitmap tmep = null;
		if(!TextUtils.isEmpty(path)){
			BitmapFactory.Options opt = new BitmapFactory.Options();
			tmep = BitmapFactory.decodeFile(path, opt);
			if (tmep == null) {
	            isOwnerAvatar = false;
				V2Log.i(" bitmap object is null");
			}
		}

        Bitmap avatar;
        if(isOwnerAvatar) {
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
        }
        else{
            if (GlobalConfig.GLOBAL_DPI == DisplayMetrics.DENSITY_XHIGH |
                    GlobalConfig.GLOBAL_DPI == DisplayMetrics.DENSITY_XXHIGH) {
                avatar = BitmapFactory.decodeResource(context.getResources() , R.drawable.avatar_big);
            } else {
                avatar = BitmapFactory.decodeResource(context.getResources() , R.drawable.avatar);
            }
        }
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
	
	public static Bitmap getSizeBitmap(Context context , String file) {
		if (file == null) {
			throw new NullPointerException(" file is null");
		}
		File f = new File(file);
		if (!f.exists()) {
			throw new RuntimeException(" file is no exists :" + file);
		}

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inInputShareable = true;// 。当系统内存不够时候图片自动被回收
		options.inPurgeable = true;
		DensityUtils.dip2px(context, 100);
		options.outHeight = DensityUtils.dip2px(context, 100);
		options.outWidth = DensityUtils.dip2px(context, 100);
		Bitmap bp = BitmapFactory.decodeFile(file, options);
		return bp;
	}

}
