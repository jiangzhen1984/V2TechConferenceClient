package com.v2tech.db;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class V2TechDBHelper extends SQLiteOpenHelper {

	private static final String DB_NAME = "v2tech.db";

	private static final int DATABASE_VERSION = 1;

	private static final String MESSAGES_TABLE_CREATE_SQL = " create table  "
			+ ContentDescriptor.Messages.NAME + " ( "
			+ ContentDescriptor.Messages.Cols.ID
			+ "  integer primary key AUTOINCREMENT,"
			+ ContentDescriptor.Messages.Cols.FROM_USER_ID + " numeric,"
			+ ContentDescriptor.Messages.Cols.FROM_USER_NAME + " text,"
			+ ContentDescriptor.Messages.Cols.TO_USER_ID + " numeric,"
			+ ContentDescriptor.Messages.Cols.TO_USER_NAME + " text,"
			+ ContentDescriptor.Messages.Cols.MSG_TYPE + " numeric,"
			+ ContentDescriptor.Messages.Cols.SEND_TIME
			+ "  TIMESTAMP  DEFAULT CURRENT_TIMESTAMP, "
			+ ContentDescriptor.Messages.Cols.STATE + " integer,"
			+ContentDescriptor.Messages.Cols.GROUP_ID+ " numeric,"
			+ContentDescriptor.Messages.Cols.UUID+ " text) ";
	
	
	
	private static final String MESSAGE_ITEM_TABLE_CREATE_SQL = " create table  "
			+ ContentDescriptor.MessageItems.NAME + " ( "
			+ ContentDescriptor.MessageItems.Cols.ID
			+ "  integer primary key AUTOINCREMENT,"
			+ ContentDescriptor.MessageItems.Cols.MSG_ID + " integer,"
			+ ContentDescriptor.MessageItems.Cols.CONTENT + " text,"
			+ ContentDescriptor.MessageItems.Cols.LINE + " numeric,"
			+ ContentDescriptor.MessageItems.Cols.TYPE+ " numeric, "
			+ ContentDescriptor.MessageItems.Cols.UUID + " text, "
			+ ContentDescriptor.MessageItems.Cols.STATE + " numeric )";
	
	
	

	private static final String CONVERSATION_TABLE_CREATE_SQL = " create table "
			+ ContentDescriptor.Conversation.NAME
			+ " ( "
			+ ContentDescriptor.Conversation.Cols.ID
			+ "  integer primary key AUTOINCREMENT,"
			+ ContentDescriptor.Conversation.Cols.TYPE
			+ " text not null,"
			+ ContentDescriptor.Conversation.Cols.EXT_ID
			+ " numeric not null,"
			+ ContentDescriptor.Conversation.Cols.EXT_NAME
			+ " text not null,"
			+ ContentDescriptor.Conversation.Cols.NOTI_FLAG
			+ " integer not null,"
			+ ContentDescriptor.Conversation.Cols.OWNER
			+ " numeric not null" + ")";

	public V2TechDBHelper(Context context, String name, CursorFactory factory,
			int version, DatabaseErrorHandler errorHandler) {
		super(context, name, factory, version, errorHandler);
	}

	public V2TechDBHelper(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
	}

	public V2TechDBHelper(Context context) {
		super(context, DB_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(MESSAGES_TABLE_CREATE_SQL);
		db.execSQL(CONVERSATION_TABLE_CREATE_SQL);
		db.execSQL(MESSAGE_ITEM_TABLE_CREATE_SQL);
	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {

	}

}
