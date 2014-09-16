package com.v2tech.db;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class V2TechDBHelper extends SQLiteOpenHelper {

	public static final String DB_NAME = "v2tech.db";

	private static final int DATABASE_VERSION = 1;

	private static final String HISTORIES_GRAPHIC_TABLE_CREATE_SQL = " create table  "
			+ ContentDescriptor.HistoriesGraphic.NAME + " ( "
			+ ContentDescriptor.HistoriesGraphic.Cols.ID
			+ "  integer primary key AUTOINCREMENT,"
			+ ContentDescriptor.HistoriesGraphic.Cols.OWNER_USER_ID + " bigint,"
			+ ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_SAVEDATE + " bigint,"
			+ ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_GROUP_TYPE + " bigint,"
			+ ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_GROUP_ID + " bigint,"
			+ ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_FROM_USER_ID + " bigint,"
			+ ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_TO_USER_ID+ " bigint, "
			+ ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_REMOTE_USER_ID+ " bigint, "
			+ ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_ID + " nvarchar(4000), "
			+ ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_TRANSTATE + " bigint, "
			+ ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_PATH + " nvarchar(4000))";
	
	private static final String HISTORIES_AUDIOS_TABLE_CREATE_SQL = " create table  "
			+ ContentDescriptor.HistoriesAudios.NAME + " ( "
			+ ContentDescriptor.HistoriesAudios.Cols.ID
			+ "  integer primary key AUTOINCREMENT,"
			+ ContentDescriptor.HistoriesAudios.Cols.OWNER_USER_ID + " bigint,"
			+ ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_SAVEDATE + " bigint,"
			+ ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_GROUP_TYPE + " bigint,"
			+ ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_GROUP_ID + " bigint,"
			+ ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_FROM_USER_ID + " bigint,"
			+ ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_TO_USER_ID+ " bigint, "
			+ ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_REMOTE_USER_ID+ " bigint, "
			+ ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_ID + " nvarchar(4000), "
			+ ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_SEND_STATE + " bigint, "
//			+ ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_READ_STATE + " bigint, "
			+ ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_SECOND + " bigint, "
			+ ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_PATH + " nvarchar(4000))";

	private static final String HISTORIES_FILES_TABLE_CREATE_SQL = " create table  "
			+ ContentDescriptor.HistoriesFiles.NAME + " ( "
			+ ContentDescriptor.HistoriesFiles.Cols.ID
			+ "  integer primary key AUTOINCREMENT,"
			+ ContentDescriptor.HistoriesFiles.Cols.OWNER_USER_ID + " bigint,"
			+ ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SAVEDATE + " bigint,"
			+ ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_FROM_USER_ID + " bigint,"
			+ ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_TO_USER_ID+ " bigint, "
			+ ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_ID + " nvarchar(4000), "
			+ ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_PATH + " nvarchar(4000), "
			+ ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SEND_STATE + " bigint, "
//			+ ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_READ_STATE + " bigint, "
			+ ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SIZE + " bigint)";
	
	private static final String HISTORIES_MEDIA_TABLE_CREATE_SQL = " create table  "
			+ ContentDescriptor.HistoriesMedia.NAME + " ( "
			+ ContentDescriptor.HistoriesMedia.Cols.ID
			+ "  integer primary key AUTOINCREMENT,"
			+ ContentDescriptor.HistoriesMedia.Cols.OWNER_USER_ID + " bigint,"
			+ ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_SAVEDATE + " bigint,"
			+ ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_CHAT_ID + " nvarchar(4000),"
			+ ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_FROM_USER_ID + " bigint,"
			+ ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_TO_USER_ID+ " bigint, "
			+ ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_REMOTE_USER_ID+ " bigint, "
			+ ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_TYPE + " bigint, "
			+ ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_STATE + " bigint, "
			+ ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_START_DATE + " bigint,"
			+ ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_END_DATE + " bigint,"
			+ ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_READ_STATE + " bigint)";
	
	
	private static final String HISTORIES_RECENT_MESSAGE_TABLE_CREATE_SQL = " create table  "
			+ ContentDescriptor.RecentHistoriesMessage.NAME + " ( "
			+ ContentDescriptor.RecentHistoriesMessage.Cols.ID
			+ "  integer primary key AUTOINCREMENT,"
			+ ContentDescriptor.RecentHistoriesMessage.Cols.OWNER_USER_ID + " bigint,"
			+ ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_SAVEDATE + " bigint,"
			+ ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_GROUP_TYPE + " bigint,"
			+ ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_USER_TYPE_ID + " bigint,"
			+ ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_FROM_USER_ID+ " bigint, "
			+ ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_TO_USER_ID+ " bigint, "
			+ ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_REMOTE_USER_ID+ " bigint, "
			+ ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_ID + " nvarchar(4000), "
			+ ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_CONTENT + " binary, "
			+ ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_READ_STATE + " bigint)";
	
	private static final String HISTORIES_ADD_FRIENT_TABLE_CREATE_SQL = " create table  "
			+ ContentDescriptor.HistoriesAddFriends.NAME + " ( "
			+ ContentDescriptor.HistoriesAddFriends.Cols.ID
			+ "  integer primary key AUTOINCREMENT,"
			+ ContentDescriptor.HistoriesAddFriends.Cols.OWNER_USER_ID + " bigint,"
			+ ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_SAVEDATE + " bigint,"
			+ ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_FROM_USER_ID + " bigint,"
			+ ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_AUTHTYPE + " bigint,"
			+ ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_TO_USER_ID+ " bigint, "
			+ ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_REMOTE_USER_ID+ " bigint, "
			+ ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_APPLY_REASON + " nvarchar(4000), "
			+ ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_REFUSE_REASON + " nvarchar(4000), "
			+ ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_STATE + " bigint ,"
			+ ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_MEDIA_READ_STATE + " bigint)";
	
	private static final String HISTORIES_CROWD_TABLE_CREATE_SQL = " create table  "
			+ ContentDescriptor.HistoriesCrowd.NAME + " ( "
			+ ContentDescriptor.HistoriesCrowd.Cols.ID
			+ "  integer primary key AUTOINCREMENT,"
			+ ContentDescriptor.HistoriesCrowd.Cols.OWNER_USER_ID + " bigint,"
			+ ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_SAVEDATE + " bigint,"
			+ ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_ID + " bigint,"
			+ ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_AUTHTYPE + " bigint,"
			+ ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_FROM_USER_ID+ " bigint, "
			+ ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_TO_USER_ID + " bigint, "
			+ ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_REMOTE_USER_ID + " bigint, "
			+ ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_APPLY_REASON + " nvarchar(4000), "
			+ ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_REFUSE_REASON + " nvarchar(4000) ,"
			+ ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_BASE_INFO + " nvarchar(4000) ,"
			+ ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_STATE + " bigint ,"
			+ ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_READ_STATE + " bigint)";
	
	public V2TechDBHelper(DataBaseContext context, String name, CursorFactory factory,
			int version, DatabaseErrorHandler errorHandler) {
		super(context, name, factory, version, errorHandler);
	}

	public V2TechDBHelper(DataBaseContext context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
	}

	public V2TechDBHelper(DataBaseContext context) {
		super(context, DB_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(HISTORIES_GRAPHIC_TABLE_CREATE_SQL);
		db.execSQL(HISTORIES_AUDIOS_TABLE_CREATE_SQL);
		db.execSQL(HISTORIES_FILES_TABLE_CREATE_SQL);
		db.execSQL(HISTORIES_MEDIA_TABLE_CREATE_SQL);
		db.execSQL(HISTORIES_RECENT_MESSAGE_TABLE_CREATE_SQL);
		db.execSQL(HISTORIES_ADD_FRIENT_TABLE_CREATE_SQL);
		db.execSQL(HISTORIES_CROWD_TABLE_CREATE_SQL);

	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {

	}

}
