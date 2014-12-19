package com.v2tech.util;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import com.v2tech.R;

/**
 * 用于打开文件的Utils
 * 
 * @author
 * 
 */
public class FileUitls {

	public static int adapterFileIcon(String fileNames) {
		String fileName = fileNames.toLowerCase();
		if (fileName.endsWith(".jpg") || fileName.endsWith(".png")
				|| fileName.endsWith(".jpeg") || fileName.endsWith(".bmp")
				|| fileName.endsWith("gif")) {
			return 1; // PICTURE = 1
		} else if (fileName.endsWith(".doc")) {
			return 2; // WORD = 2
		} else if (fileName.endsWith(".xls")) {
			return 3; // EXCEL = 3
		} else if (fileName.endsWith(".pdf")) {
			return 4; // PDF = 4
		} else if (fileName.endsWith(".ppt") || fileName.endsWith(".pptx")) {
			return 5; // PPT = 5
		} else if (fileName.endsWith(".zip") || fileName.endsWith(".rar")) {
			return 6; // ZIP = 6
		} else if (fileName.endsWith(".vsd") || fileName.endsWith(".vss")
				|| fileName.endsWith(".vst") || fileName.endsWith(".vdx")) {
			return 7; // VIS = 7
		} else if (fileName.endsWith(".mp4") || fileName.endsWith(".rmvb")
				|| fileName.endsWith(".avi") || fileName.endsWith(".3gp")) {
			return 8; // VIDEO = 8
		} else if (fileName.endsWith(".mp3") || fileName.endsWith(".wav")
				|| fileName.endsWith(".ape") || fileName.endsWith(".wmv")) {
			return 9; // SOUND = 9
		} else {
			return 10; // OTHER = 10
		}
	}

	public static void openFile(Context mContext, String filePath) {

		if (!TextUtils.isEmpty(filePath))
			openFile(mContext, new File(filePath));
		else
			Toast.makeText(mContext, "对不起，这不是文件！", Toast.LENGTH_SHORT).show();

	}

	public static void openFile(Context mContext, File file) {

		// 4、通过调用OpenFileUitls类返回的Intent，打开相应的文件
		if (file != null && file.isFile()) {
			String filePath = file.getAbsolutePath();
			int dot = filePath.lastIndexOf(".");
			String postfixName = filePath.substring(dot);
			Intent intent;
			if (checkEndsWithInStringArray(postfixName, mContext.getResources()
					.getStringArray(R.array.fileEndingImage))) {
				intent = FileUitls.getImageFileIntent(file);
				mContext.startActivity(intent);
			} else if (checkEndsWithInStringArray(postfixName, mContext
					.getResources().getStringArray(R.array.fileEndingWebText))) {
				intent = FileUitls.getHtmlFileIntent(file);
				mContext.startActivity(intent);
			} else if (checkEndsWithInStringArray(postfixName, mContext
					.getResources().getStringArray(R.array.fileEndingPackage))) {
				intent = FileUitls.getApkFileIntent(file);
				mContext.startActivity(intent);

			} else if (checkEndsWithInStringArray(postfixName, mContext
					.getResources().getStringArray(R.array.fileEndingAudio))) {
				intent = FileUitls.getAudioFileIntent(file);
				mContext.startActivity(intent);
			} else if (checkEndsWithInStringArray(postfixName, mContext
					.getResources().getStringArray(R.array.fileEndingVideo))) {
				intent = FileUitls.getVideoFileIntent(file);
				mContext.startActivity(intent);
			} else if (checkEndsWithInStringArray(postfixName, mContext
					.getResources().getStringArray(R.array.fileEndingText))) {
				intent = FileUitls.getTextFileIntent(file);
				mContext.startActivity(intent);
			} else if (checkEndsWithInStringArray(postfixName, mContext
					.getResources().getStringArray(R.array.fileEndingPdf))) {
				intent = FileUitls.getPdfFileIntent(file);
				mContext.startActivity(intent);
			} else if (checkEndsWithInStringArray(postfixName, mContext
					.getResources().getStringArray(R.array.fileEndingWord))) {
				intent = FileUitls.getWordFileIntent(file);
				mContext.startActivity(intent);
			} else if (checkEndsWithInStringArray(postfixName, mContext
					.getResources().getStringArray(R.array.fileEndingExcel))) {
				intent = FileUitls.getExcelFileIntent(file);
				mContext.startActivity(intent);
			} else if (checkEndsWithInStringArray(postfixName, mContext
					.getResources().getStringArray(R.array.fileEndingPPT))) {
				intent = FileUitls.getPPTFileIntent(file);
				mContext.startActivity(intent);
			} else
				Toast.makeText(mContext, "无法打开，请安装相应的软件！", Toast.LENGTH_SHORT)
						.show();
		} else
			Toast.makeText(mContext, "对不起，这不是文件！", Toast.LENGTH_SHORT).show();

	}

	// 3、定义用于检查要打开的文件的后缀是否在遍历后缀数组中
	private static boolean checkEndsWithInStringArray(String checkItsEnd,
			String[] fileEndings) {
		for (String aEnd : fileEndings) {
			if (checkItsEnd.endsWith(aEnd))
				return true;
		}
		return false;
	}

	// android获取一个用于打开HTML文件的intent
	public static Intent getHtmlFileIntent(File file) {
		Uri uri = Uri.parse(file.toString()).buildUpon()
				.encodedAuthority("com.android.htmlfileprovider")
				.scheme("content").encodedPath(file.toString()).build();
		Intent intent = new Intent("android.intent.action.VIEW");
		intent.setDataAndType(uri, "text/html");
		return intent;
	}

	// android获取一个用于打开图片文件的intent
	public static Intent getImageFileIntent(File file) {
		Intent intent = new Intent("android.intent.action.VIEW");
		intent.addCategory("android.intent.category.DEFAULT");
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Uri uri = Uri.fromFile(file);
		intent.setDataAndType(uri, "image/*");
		return intent;
	}

	// android获取一个用于打开PDF文件的intent
	public static Intent getPdfFileIntent(File file) {
		Intent intent = new Intent("android.intent.action.VIEW");
		intent.addCategory("android.intent.category.DEFAULT");
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Uri uri = Uri.fromFile(file);
		intent.setDataAndType(uri, "application/pdf");
		return intent;
	}

	// android获取一个用于打开文本文件的intent
	public static Intent getTextFileIntent(File file) {
		Intent intent = new Intent("android.intent.action.VIEW");
		intent.addCategory("android.intent.category.DEFAULT");
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Uri uri = Uri.fromFile(file);
		intent.setDataAndType(uri, "text/plain");
		return intent;
	}

	// android获取一个用于打开音频文件的intent
	public static Intent getAudioFileIntent(File file) {
		Intent intent = new Intent("android.intent.action.VIEW");
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra("oneshot", 0);
		intent.putExtra("configchange", 0);
		Uri uri = Uri.fromFile(file);
		intent.setDataAndType(uri, "audio/*");
		return intent;
	}

	// android获取一个用于打开视频文件的intent
	public static Intent getVideoFileIntent(File file) {
		Intent intent = new Intent("android.intent.action.VIEW");
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra("oneshot", 0);
		intent.putExtra("configchange", 0);
		Uri uri = Uri.fromFile(file);
		intent.setDataAndType(uri, "video/*");
		return intent;
	}

	// android获取一个用于打开CHM文件的intent
	public static Intent getChmFileIntent(File file) {
		Intent intent = new Intent("android.intent.action.VIEW");
		intent.addCategory("android.intent.category.DEFAULT");
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Uri uri = Uri.fromFile(file);
		intent.setDataAndType(uri, "application/x-chm");
		return intent;
	}

	// android获取一个用于打开Word文件的intent
	public static Intent getWordFileIntent(File file) {
		Intent intent = new Intent("android.intent.action.VIEW");
		intent.addCategory("android.intent.category.DEFAULT");
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Uri uri = Uri.fromFile(file);
		intent.setDataAndType(uri, "application/msword");
		return intent;
	}

	// android获取一个用于打开Excel文件的intent
	public static Intent getExcelFileIntent(File file) {
		Intent intent = new Intent("android.intent.action.VIEW");
		intent.addCategory("android.intent.category.DEFAULT");
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Uri uri = Uri.fromFile(file);
		intent.setDataAndType(uri, "application/vnd.ms-excel");
		return intent;
	}

	// android获取一个用于打开PPT文件的intent
	public static Intent getPPTFileIntent(File file) {
		Intent intent = new Intent("android.intent.action.VIEW");
		intent.addCategory("android.intent.category.DEFAULT");
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Uri uri = Uri.fromFile(file);
		intent.setDataAndType(uri, "application/vnd.ms-powerpoint");
		return intent;
	}

	// android获取一个用于打开apk文件的intent
	public static Intent getApkFileIntent(File file) {
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setAction(android.content.Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.fromFile(file),
				"application/vnd.android.package-archive");
		return intent;
	}
}
