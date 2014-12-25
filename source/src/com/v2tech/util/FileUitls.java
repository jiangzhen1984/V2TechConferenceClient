package com.v2tech.util;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.vo.VMessageFileItem.FileType;

/**
 * 用于打开文件的Utils
 * 
 * @author
 * 
 */
public class FileUitls {

	public static Context context;
	
	public static int adapterFileIcon(String fileName){
		if(fileName.indexOf(".") == -1)
			return FileType.UNKNOW.intValue();
		String postfixName = fileName.substring(fileName.indexOf("."));
		V2Log.e("FileUtils --> get postfix name is : " + postfixName);
		FileType fileType  = getFileType(postfixName);
		return adapterFileIcon(fileType);
	}
	
	public static int adapterFileIcon(FileType fileType) {
		switch (fileType) {
		case IMAGE:
			return R.drawable.selectfile_type_picture;
		case WORD:
			return R.drawable.selectfile_type_word;
		case EXCEL:
			return R.drawable.selectfile_type_excel;
		case PDF:
			return R.drawable.selectfile_type_pdf;
		case PPT:
			return R.drawable.selectfile_type_ppt;
		case ZIP:
			return R.drawable.selectfile_type_zip;
		case VIS:
			return R.drawable.selectfile_type_viso;
		case VIDEO:
			return R.drawable.selectfile_type_video;
		case AUDIO:
			return R.drawable.selectfile_type_sound;
		case UNKNOW:
			return R.drawable.selectfile_type_ohter;
		default:
			return R.drawable.selectfile_type_ohter;
		}
	}

	public static FileType getFileType(String postfixName) {
		postfixName = postfixName.toLowerCase();
		if (checkEndsWithInStringArray(postfixName, context.getResources()
				.getStringArray(R.array.fileEndingImage))) {
			return FileType.IMAGE;
		} else if (checkEndsWithInStringArray(postfixName, context
				.getResources().getStringArray(R.array.fileEndingWebText))) {
			return FileType.HTML;
		} else if (checkEndsWithInStringArray(postfixName, context
				.getResources().getStringArray(R.array.fileEndingAPK))) {
			return FileType.PACKAGE;

		} else if (checkEndsWithInStringArray(postfixName, context
				.getResources().getStringArray(R.array.fileEndingPackage))) {
			return FileType.ZIP;

		} else if (checkEndsWithInStringArray(postfixName, context
				.getResources().getStringArray(R.array.fileEndingAudio))) {
			return FileType.AUDIO;
		} else if (checkEndsWithInStringArray(postfixName, context
				.getResources().getStringArray(R.array.fileEndingVideo))) {
			return FileType.VIDEO;
		} else if (checkEndsWithInStringArray(postfixName, context
				.getResources().getStringArray(R.array.fileEndingText))) {
			return FileType.TEXT;
		} else if (checkEndsWithInStringArray(postfixName, context
				.getResources().getStringArray(R.array.fileEndingPdf))) {
			return FileType.PDF;
		} else if (checkEndsWithInStringArray(postfixName, context
				.getResources().getStringArray(R.array.fileEndingWord))) {
			return FileType.WORD;
		} else if (checkEndsWithInStringArray(postfixName, context
				.getResources().getStringArray(R.array.fileEndingExcel))) {
			return FileType.EXCEL;
		} else if (checkEndsWithInStringArray(postfixName, context
				.getResources().getStringArray(R.array.fileEndingPPT))) {
			return FileType.PPT;
		} else if (checkEndsWithInStringArray(postfixName, context
				.getResources().getStringArray(R.array.fileEndingVis))) {
			return FileType.VIS;
		} {
			return FileType.UNKNOW;
		}
	}

	public static void openFile(String filePath) {
		filePath = filePath.toLowerCase();
		if (!TextUtils.isEmpty(filePath))
			openFile(new File(filePath));
		else
			Toast.makeText(context, "没有应用程序可执行此操作", Toast.LENGTH_SHORT).show();

	}

	public static void openFile(File file) {

		// 4、通过调用OpenFileUitls类返回的Intent，打开相应的文件
		if (file != null && file.isFile()) {
			String filePath = file.getAbsolutePath();
			int dot = filePath.lastIndexOf(".");
			String postfixName = filePath.substring(dot);
			Intent intent;
			try{
				if (checkEndsWithInStringArray(postfixName, context.getResources()
						.getStringArray(R.array.fileEndingImage))) {
					intent = FileUitls.getImageFileIntent(file);
					context.startActivity(intent);
				} else if (checkEndsWithInStringArray(postfixName, context
						.getResources().getStringArray(R.array.fileEndingWebText))) {
					intent = FileUitls.getHtmlFileIntent(file);
					context.startActivity(intent);
				} else if (checkEndsWithInStringArray(postfixName, context
						.getResources().getStringArray(R.array.fileEndingAPK))) {
					intent = FileUitls.getApkFileIntent(file);
					context.startActivity(intent);
	
				} else if (checkEndsWithInStringArray(postfixName, context
						.getResources().getStringArray(R.array.fileEndingPackage))) {
					intent = FileUitls.getApplicationFileIntent(file);
					context.startActivity(intent);
	
				} else if (checkEndsWithInStringArray(postfixName, context
						.getResources().getStringArray(R.array.fileEndingAudio))) {
					intent = FileUitls.getAudioFileIntent(file);
					context.startActivity(intent);
				} else if (checkEndsWithInStringArray(postfixName, context
						.getResources().getStringArray(R.array.fileEndingVideo))) {
					intent = FileUitls.getVideoFileIntent(file);
					context.startActivity(intent);
				} else if (checkEndsWithInStringArray(postfixName, context
						.getResources().getStringArray(R.array.fileEndingText))) {
					intent = FileUitls.getTextFileIntent(file);
					context.startActivity(intent);
				} else if (checkEndsWithInStringArray(postfixName, context
						.getResources().getStringArray(R.array.fileEndingPdf))) {
					intent = FileUitls.getPdfFileIntent(file);
					context.startActivity(intent);
				} else if (checkEndsWithInStringArray(postfixName, context
						.getResources().getStringArray(R.array.fileEndingWord))) {
					intent = FileUitls.getWordFileIntent(file);
					context.startActivity(intent);
				} else if (checkEndsWithInStringArray(postfixName, context
						.getResources().getStringArray(R.array.fileEndingExcel))) {
					intent = FileUitls.getExcelFileIntent(file);
					context.startActivity(intent);
				} else if (checkEndsWithInStringArray(postfixName, context
						.getResources().getStringArray(R.array.fileEndingPPT))) {
					intent = FileUitls.getPPTFileIntent(file);
					context.startActivity(intent);
				} else if (checkEndsWithInStringArray(postfixName, context
						.getResources().getStringArray(R.array.fileEndingVis))) {
					intent = FileUitls.getApplicationFileIntent(file);
					context.startActivity(intent);
				} else
					Toast.makeText(context, "没有应用程序可执行此操作", Toast.LENGTH_SHORT)
							.show();
			}
			catch(Exception e){
				Toast.makeText(context, "没有应用程序可执行此操作", Toast.LENGTH_SHORT).show();
			}
		}
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
		intent.addCategory("android.intent.category.DEFAULT");
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Uri uri = Uri.fromFile(file);
		intent.setDataAndType(uri, "audio/*");
		return intent;
	}

	// android获取一个用于打开视频文件的intent
	public static Intent getVideoFileIntent(File file) {
		Intent intent = new Intent("android.intent.action.VIEW");
		intent.addCategory("android.intent.category.DEFAULT");
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//		intent.putExtra("oneshot", 0);
//		intent.putExtra("configchange", 0);
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

	// android获取一个用于打开apk文件的intent
	public static Intent getApplicationFileIntent(File file) {
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setAction(android.content.Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.fromFile(file),
				"application/*");
		return intent;
	}
}
