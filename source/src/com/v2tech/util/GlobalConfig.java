package com.v2tech.util;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.SparseArray;

import com.v2tech.R;

public class GlobalConfig {

	public static final String KEY_LOGGED_IN = "LoggedIn";

	public static int GLOBAL_DPI = DisplayMetrics.DENSITY_XHIGH;

	public static int GLOBAL_VERSION_CODE = 1;

	public static String GLOBAL_VERSION_NAME = "1.3.0.1";

	public static double SCREEN_INCHES = 0;
	
	public static boolean isConversationOpen = false;

	public static int[] GLOBAL_FACE_ARRAY = new int[] { 0,R.drawable.face_1,
			R.drawable.face_2, R.drawable.face_3, R.drawable.face_4,
			R.drawable.face_5, R.drawable.face_6, R.drawable.face_7,
			R.drawable.face_8, R.drawable.face_9, R.drawable.face_10,
			R.drawable.face_11, R.drawable.face_12, R.drawable.face_13,
			R.drawable.face_14, R.drawable.face_15, R.drawable.face_16,
			R.drawable.face_17, R.drawable.face_18, R.drawable.face_19,
			R.drawable.face_20, R.drawable.face_21, R.drawable.face_22,
			R.drawable.face_23, R.drawable.face_24, R.drawable.face_25,
			R.drawable.face_26, R.drawable.face_27, R.drawable.face_28,
			R.drawable.face_29, R.drawable.face_30, R.drawable.face_31,
			R.drawable.face_32, R.drawable.face_33, R.drawable.face_34,
			R.drawable.face_35, R.drawable.face_36, R.drawable.face_37,
			R.drawable.face_38, R.drawable.face_39, R.drawable.face_40,
			R.drawable.face_41, R.drawable.face_42, R.drawable.face_43,
			R.drawable.face_44, R.drawable.face_45};
	
	private static SparseArray<EmojiWraper> EMOJI_ARRAY = new SparseArray<EmojiWraper>();
	
	static {
		String preFix = "/:";
		String suffFix = ":/";
		for (int i = 1; i < GLOBAL_FACE_ARRAY.length; i++) {
			char c = (char)i;
			if (c == '\n') {
				c += 100;
			}
			c += 100;
			EMOJI_ARRAY.put(GLOBAL_FACE_ARRAY[i], new EmojiWraper(preFix + c+ suffFix,GLOBAL_FACE_ARRAY[i]));
		}
	}

	public static void saveLogoutFlag(Context context) {
		SPUtil.putConfigIntValue(context, KEY_LOGGED_IN, 0);
	}
	
	
	public static String getEmojiStrByIndex(int index) {
		if (index <=0 || index >= GLOBAL_FACE_ARRAY.length) {
			return null;
		}
		EmojiWraper wrapper = EMOJI_ARRAY.get(GLOBAL_FACE_ARRAY[index]);
		if (wrapper != null) {
			return wrapper.emojiStr;
		}
		return null;
	}
	
	
	public static int getDrawableIdByEmoji(String str) {
		for (int i = 1; i < GLOBAL_FACE_ARRAY.length; i++) {
			EmojiWraper wrapper = EMOJI_ARRAY.get(GLOBAL_FACE_ARRAY[i]);
			if (wrapper == null) {
				continue;
			}
			if (wrapper.emojiStr.equals(str)) {
				return wrapper.id;
			}
		}
		return -1;
	}
	
	
	public static int getDrawableIndexByEmoji(String str) {
		for (int i = 1; i < GLOBAL_FACE_ARRAY.length; i++) {
			EmojiWraper wrapper = EMOJI_ARRAY.get(GLOBAL_FACE_ARRAY[i]);
			if (wrapper == null) {
				continue;
			}
			if (wrapper.emojiStr.equals(str)) {
				return i;
			}
		}
		return -1;
	}
	
	
	public static String getEmojiStr(int id) {
		EmojiWraper wrapper = EMOJI_ARRAY.get(id);
		if (wrapper != null) {
			return wrapper.emojiStr;
		}
		return null;
	}
	
	
	
	public static String getGlobalPath() {
		return StorageUtil.getAbsoluteSdcardPath()+"/.v2tech/";
	}
	
	public static String getGlobalUserAvatarPath() {
		return StorageUtil.getAbsoluteSdcardPath()+"/.v2tech/Users";
	}
	
	
	public static String getGlobalPicsPath() {
		return StorageUtil.getAbsoluteSdcardPath()+"/.v2tech/pics";
	}
	
	
	public static String getGlobalAudioPath() {
		return StorageUtil.getAbsoluteSdcardPath()+"/.v2tech/audio";
	}
	
	public static String getGlobalFilePath() {
		return StorageUtil.getAbsoluteSdcardPath()+"/v2tech/file";
	}
	
	
	
	static class EmojiWraper {
		String emojiStr;
		int id;
		
		public EmojiWraper(String emojiStr, int id) {
			super();
			this.emojiStr = emojiStr;
			this.id = id;
		}
		
	}
	
	
	
	public static class Resource {
		
		public static String ConversationCrator="";
		
		public static String ConferenceInvitation="";
		
		public static String ConferenceVideo="";
		
		public static String MixedVideoName="";
	}
}
