package com.bizcom.util;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import com.V2.jni.util.V2Log;
import com.bizcom.vc.application.GlobalConfig;
import com.v2tech.R;

public class BitmapUtil {

	public static Context context;

	public static Bitmap loadAvatarFromPath(String path) {
		boolean isOwnerAvatar = true;
		if (path == null) {
			isOwnerAvatar = false;
		} else {
			File f = new File(path);
			if (!f.exists()) {
				isOwnerAvatar = false;
				V2Log.e("BitmapUtil loadAvatarFromPath --> FAILED! Because parse Path , file isn't exist! path is : "
						+ path);
				File parent = new File(f.getParent());
				File[] listFiles = parent.listFiles();
				for (File file : listFiles) {
					V2Log.e("BitmapUtil loadAvatarFromPath --> current directory file list is : "
							+ file.getName());
				}
			}

			if (f.isDirectory()) {
				isOwnerAvatar = false;
				V2Log.e("BitmapUtil loadAvatarFromPath --> FAILED! Because parse Path , get a Directory! path is : "
						+ path);
			}
		}

		Bitmap tmep = null;
		if (!TextUtils.isEmpty(path)) {
			BitmapFactory.Options opt = new BitmapFactory.Options();
			tmep = BitmapFactory.decodeFile(path, opt);
			if (tmep == null) {
				isOwnerAvatar = false;
				V2Log.i(" bitmap object is null");
			}
		}

		Bitmap avatar;
		if (isOwnerAvatar) {
			if (GlobalConfig.GLOBAL_DPI == DisplayMetrics.DENSITY_HIGH) {
				avatar = Bitmap.createScaledBitmap(tmep, 70, 70, true);
			} else if (GlobalConfig.GLOBAL_DPI == DisplayMetrics.DENSITY_MEDIUM) {
				avatar = Bitmap.createScaledBitmap(tmep,
						(int) (70 / 1.5 + 0.5), (int) (70 / 1.5 + 0.5), true);
			} else if (GlobalConfig.GLOBAL_DPI == DisplayMetrics.DENSITY_LOW) {
				avatar = Bitmap.createScaledBitmap(tmep,
						(int) (70 / 1.5 * 0.7 + 0.5),
						(int) (70 / 1.5 * 0.7 + 0.5), true);
			} else if (GlobalConfig.GLOBAL_DPI == DisplayMetrics.DENSITY_XHIGH) {
				avatar = Bitmap.createScaledBitmap(tmep,
						(int) (70 / 1.5 * 2 + 0.5), (int) (70 / 1.5 * 2 + 0.5),
						true);
			} else if (GlobalConfig.GLOBAL_DPI == DisplayMetrics.DENSITY_XXHIGH) {
				avatar = Bitmap.createScaledBitmap(tmep, (int) (70 * 2 + 0.5),
						(int) (70 * 2 + 0.5), true);
			} else {
				avatar = Bitmap.createScaledBitmap(tmep, 70, 70, true);
			}
			tmep.recycle();
			V2Log.d("decode result: width " + avatar.getWidth() + "  height:"
					+ avatar.getHeight());
		} else {
			if (GlobalConfig.GLOBAL_DPI == DisplayMetrics.DENSITY_XHIGH
					| GlobalConfig.GLOBAL_DPI == DisplayMetrics.DENSITY_XXHIGH) {
				avatar = BitmapFactory.decodeResource(context.getResources(),
						R.drawable.avatar_big);
			} else {
				avatar = BitmapFactory.decodeResource(context.getResources(),
						R.drawable.avatar);
			}
		}
		return avatar;
	}

	public static Bitmap getCompressedBitmap(String filePath) {
		if (TextUtils.isEmpty(filePath)) {
			throw new NullPointerException(" file is null");
		}

		File f = new File(filePath);
		if (filePath.equals("wait")) {
			return BitmapFactory.decodeResource(context.getResources(),
					R.drawable.ws_receive_image_wait);
		}

		if (filePath.equals("error") || !f.exists()) {
			return BitmapFactory.decodeResource(context.getResources(),
					R.drawable.ws_download_error_icon);
		}

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		options.inPreferredConfig = Bitmap.Config.ALPHA_8;
		BitmapFactory.decodeFile(filePath, options);
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
		return BitmapFactory.decodeFile(filePath, options);
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

	public static Bitmap getSizeBitmap(Context context, String file) {
		if (TextUtils.isEmpty(file)) {
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
		} else if (options.outWidth > 200 || options.outHeight > 200) {
			options.inSampleSize = 2;
		} else {
			options.inSampleSize = 1;
		}
		options.inJustDecodeBounds = false;
		options.inPurgeable = true;
		options.inInputShareable = true;// 。当系统内存不够时候图片自动被回收
		DensityUtils.dip2px(context, 100);
		options.outHeight = DensityUtils.dip2px(context, 100);
		options.outWidth = DensityUtils.dip2px(context, 100);
		return BitmapFactory.decodeFile(file, options);
	}

	public static int getBitmapRotation(String imgpath) {
		int digree = 0;
		ExifInterface exif = null;
		try {
			exif = new ExifInterface(imgpath);
		} catch (IOException e) {
			e.printStackTrace();
			exif = null;
		}
		if (exif != null) {
			// 读取图片中相机方向信息
			int ori = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_UNDEFINED);
			// 计算旋转角度
			switch (ori) {
			case ExifInterface.ORIENTATION_ROTATE_90:
				digree = 90;
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				digree = 180;
				break;
			case ExifInterface.ORIENTATION_ROTATE_270:
				digree = 270;
				break;
			default:
				digree = 0;
				break;
			}
		}
		return digree;
	}

	public static Bitmap createScaledBitmap(Bitmap unscaledBitmap,
			int dstWidth, int dstHeight) {
		Rect srcRect = calculateSrcRect(unscaledBitmap.getWidth(),
				unscaledBitmap.getHeight(), dstWidth, dstHeight);
		Rect dstRect = calculateDstRect(unscaledBitmap.getWidth(),
				unscaledBitmap.getHeight(), dstWidth, dstHeight);
		Bitmap scaledBitmap = Bitmap.createBitmap(dstRect.width(),
				dstRect.height(), Config.ARGB_8888);
		Canvas canvas = new Canvas(scaledBitmap);
		canvas.drawBitmap(unscaledBitmap, srcRect, dstRect, new Paint(
				Paint.FILTER_BITMAP_FLAG));
		return scaledBitmap;
	}

	public static Rect calculateSrcRect(int srcWidth, int srcHeight,
			int dstWidth, int dstHeight) {
		// float srcAspect = (float) srcWidth / (float) srcHeight;
		// float dstAspect = (float) dstWidth / (float) dstHeight;
		// if (srcAspect > dstAspect) {
		// final int srcRectWidth = (int) (srcHeight * dstAspect);
		// final int srcRectLeft = (srcWidth - srcRectWidth) / 2;
		// return new Rect(srcRectLeft, 0, srcRectLeft + srcRectWidth,
		// srcHeight);
		// } else {
		// final int srcRectHeight = (int) (srcWidth / dstAspect);
		// final int scrRectTop = (int) (srcHeight - srcRectHeight) / 2;
		// return new Rect(0, scrRectTop, srcWidth, scrRectTop + srcRectHeight);
		// }
		return new Rect(0, 0, srcWidth, srcHeight);
	}

	public static Rect calculateDstRect(int srcWidth, int srcHeight,
			int dstWidth, int dstHeight) {
		float srcAspect = (float) srcWidth / (float) srcHeight;
		float dstAspect = (float) dstWidth / (float) dstHeight;
		if (srcAspect > dstAspect) {
			return new Rect(0, 0, dstWidth, (int) (dstWidth / srcAspect));
		} else {
			return new Rect(0, 0, (int) (dstHeight * srcAspect), dstHeight);
		}
		// return new Rect(0, 0, dstWidth, dstHeight);
	}

	public static int calculateSampleSize(int srcWidth, int srcHeight,
			int dstWidth, int dstHeight) {
		// final float srcAspect = (float) srcWidth / (float) srcHeight;
		// final float dstAspect = (float) dstWidth / (float) dstHeight;
		// if (srcAspect > dstAspect) {
		// return srcWidth / dstWidth;
		// } else {
		// return srcHeight / dstHeight;
		// }
		int scaleFactor = 1;
		if ((dstWidth > 0) || (dstHeight > 0)) {
			return scaleFactor = Math.min(srcWidth / dstWidth, srcHeight
					/ dstHeight);
		}
		return scaleFactor;
	}
}
