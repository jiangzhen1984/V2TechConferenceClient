package com.v2tech.db.provider;

import android.database.Cursor;

import com.v2tech.db.ContentDescriptor;
import com.v2tech.util.CrashHandler;

public class VoiceProvider extends DatabaseProvider{

	/**
	 * 查询数据库中是否存在音视频消息记录
	 * @return
	 */
	public static boolean queryIsHaveVoiceMessages(){
		return queryIsHaveVoiceMessages(null , null);
	}
	
	/**
	 * 根据用户id , 查询该用户是否有音视频消息记录
	 * @param userID
	 * @return
	 * 		true 代表有记录 , false 代表没有记录
	 */
	public static boolean queryIsHaveVoiceMessages(long userID){
		String selection = ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_REMOTE_USER_ID + " = ? ";
		String[] args = new String[]{String.valueOf(userID)};
		return queryIsHaveVoiceMessages(selection , args);
	}
	
	/**
	 * 根据用户id , 查询该用户是否有音视频消息记录
	 * @param userID
	 * @return
	 * 		true 代表有记录 , false 代表没有记录
	 */
	public static boolean queryIsHaveVoiceMessages(String selection , String[] args){
		
		Cursor cursor = null;
		try {
			cursor = mContext.getContentResolver().query(ContentDescriptor.HistoriesMedia.CONTENT_URI, 
				null, 
				selection,
				args, 
				null);
			if (cursor == null || cursor.getCount() <= 0) {
				return false;
			}
			
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			CrashHandler.getInstance().saveCrashInfo2File(e);
			return false;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}
}
