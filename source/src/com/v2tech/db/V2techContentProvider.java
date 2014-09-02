package com.v2tech.db;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class V2techContentProvider extends ContentProvider {

	public static final String AUTHORITY = "com.v2tech.bizcom";

	/*
	 * Defines a handle to the database helper object. The MainDatabaseHelper
	 * class is defined in a following snippet.
	 */
	private V2TechDBHelper mOpenHelper;

	@Override
	public int delete(Uri uri, String selection, String[] selectionsArgs) {
		int ret = 0;
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int token = ContentDescriptor.URI_MATCHER.match(uri);
		String table = null;
		switch (token) {
		case ContentDescriptor.Messages.TOKEN:
			table = ContentDescriptor.Messages.NAME;
			break;
		case ContentDescriptor.Conversation.TOKEN:
			table = ContentDescriptor.Conversation.NAME;
			break;
		case ContentDescriptor.HistoriesMessage.TOKEN:
			table = ContentDescriptor.HistoriesMessage.NAME;
			break;
		case ContentDescriptor.HistoriesGraphic.TOKEN:
			table = ContentDescriptor.HistoriesGraphic.NAME;
			break;
		case ContentDescriptor.RecentHistoriesMessage.TOKEN:
			table = ContentDescriptor.RecentHistoriesMessage.NAME;
			break;
		case ContentDescriptor.HistoriesAudios.TOKEN:
			table = ContentDescriptor.HistoriesAudios.NAME;
			break;
		case ContentDescriptor.HistoriesMedia.TOKEN:
			table = ContentDescriptor.HistoriesMedia.NAME;
			break;
		case ContentDescriptor.HistoriesFiles.TOKEN:
			table = ContentDescriptor.HistoriesFiles.NAME;
			break;
		case ContentDescriptor.HistoriesAddFriends.TOKEN:
			table = ContentDescriptor.HistoriesAddFriends.NAME;
			break;
		case ContentDescriptor.HistoriesCrowd.TOKEN:
			table = ContentDescriptor.HistoriesCrowd.NAME;
			break;
		default:
			throw new RuntimeException("Does not support operation ：" + token);
		}
		if (table != null) {
			ret = db.delete(table, selection, selectionsArgs);
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return ret;
	}

	@Override
	public String getType(Uri arg0) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		Uri newUri = null;
		long id;
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int token = ContentDescriptor.URI_MATCHER.match(uri);
		switch (token) {
		case ContentDescriptor.Messages.TOKEN:
			id = db.insert(ContentDescriptor.Messages.NAME, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			newUri = ContentDescriptor.Messages.CONTENT_URI.buildUpon()
					.appendPath(String.valueOf(id)).build();
			break;
		case ContentDescriptor.Conversation.TOKEN:
			id = db.insert(ContentDescriptor.Conversation.NAME, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			newUri = ContentDescriptor.Conversation.CONTENT_URI.buildUpon()
					.appendPath(String.valueOf(id)).build();
			break;
		case ContentDescriptor.MessageItems.TOKEN:
			id = db.insert(ContentDescriptor.MessageItems.NAME, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			newUri = ContentDescriptor.MessageItems.CONTENT_URI.buildUpon()
					.appendPath(String.valueOf(id)).build();
			break;
		case ContentDescriptor.HistoriesMessage.TOKEN:
			id = db.insert(ContentDescriptor.HistoriesMessage.NAME, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			newUri = ContentDescriptor.HistoriesMessage.CONTENT_URI.buildUpon()
					.appendPath(String.valueOf(id)).build();
			break;
		case ContentDescriptor.HistoriesGraphic.TOKEN:
			id = db.insert(ContentDescriptor.HistoriesGraphic.NAME, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			newUri = ContentDescriptor.HistoriesGraphic.CONTENT_URI.buildUpon()
					.appendPath(String.valueOf(id)).build();
			break;
		case ContentDescriptor.RecentHistoriesMessage.TOKEN:
			id = db.insert(ContentDescriptor.RecentHistoriesMessage.NAME, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			newUri = ContentDescriptor.RecentHistoriesMessage.CONTENT_URI.buildUpon()
					.appendPath(String.valueOf(id)).build();
			break;
		case ContentDescriptor.HistoriesAudios.TOKEN:
			id = db.insert(ContentDescriptor.HistoriesAudios.NAME, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			newUri = ContentDescriptor.HistoriesAudios.CONTENT_URI.buildUpon()
					.appendPath(String.valueOf(id)).build();
			break;
		case ContentDescriptor.HistoriesMedia.TOKEN:
			id = db.insert(ContentDescriptor.HistoriesMedia.NAME, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			newUri = ContentDescriptor.HistoriesMedia.CONTENT_URI.buildUpon()
					.appendPath(String.valueOf(id)).build();
			break;
		case ContentDescriptor.HistoriesFiles.TOKEN:
			id = db.insert(ContentDescriptor.HistoriesFiles.NAME, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			newUri = ContentDescriptor.HistoriesFiles.CONTENT_URI.buildUpon()
					.appendPath(String.valueOf(id)).build();
			break;
		case ContentDescriptor.HistoriesAddFriends.TOKEN:
			id = db.insert(ContentDescriptor.HistoriesAddFriends.NAME, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			newUri = ContentDescriptor.HistoriesAddFriends.CONTENT_URI.buildUpon()
					.appendPath(String.valueOf(id)).build();
			break;
		case ContentDescriptor.HistoriesCrowd.TOKEN:
			id = db.insert(ContentDescriptor.HistoriesCrowd.NAME, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			newUri = ContentDescriptor.HistoriesCrowd.CONTENT_URI.buildUpon()
					.appendPath(String.valueOf(id)).build();
			break;
		default:
			throw new RuntimeException("Does not support operation ：" + token);
		}
		getContext().getContentResolver().notifyChange(newUri, null);
		return newUri;
	}

	@Override
	public boolean onCreate() {
		mOpenHelper = new V2TechDBHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
//		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		int token = ContentDescriptor.URI_MATCHER.match(uri);
		String tableName = null;
		switch (token) {
		case ContentDescriptor.MessageItems.TOKEN:
//			qb.setTables(ContentDescriptor.MessageItems.NAME);
			tableName = ContentDescriptor.MessageItems.NAME;
			break;
		case ContentDescriptor.Conversation.TOKEN:
//			qb.setTables(ContentDescriptor.Conversation.NAME);
			tableName = ContentDescriptor.Conversation.NAME;
			break;
		case ContentDescriptor.HistoriesMessage.TOKEN:
//			qb.setTables(ContentDescriptor.HistoriesMessage.NAME);
			tableName = ContentDescriptor.HistoriesMessage.NAME;
			break;
		case ContentDescriptor.HistoriesMessage.TOKEN_WITH_ID:
//			qb.setTables(ContentDescriptor.HistoriesMessage.NAME);
			tableName = ContentDescriptor.HistoriesMessage.NAME;
			selection = ContentDescriptor.Messages.Cols.ID + "=? ";
			selectionArgs = new String[] { uri.getLastPathSegment() };
			break;
		case ContentDescriptor.HistoriesMessage.TOKEN_BY_PAGE:
			break;
		case ContentDescriptor.HistoriesGraphic.TOKEN:
			tableName = ContentDescriptor.HistoriesGraphic.NAME;
			break;
		case ContentDescriptor.HistoriesGraphic.TOKEN_WITH_ID:
			tableName = ContentDescriptor.HistoriesGraphic.NAME;
			selection = ContentDescriptor.HistoriesGraphic.Cols.ID + "=? ";
			selectionArgs = new String[] { uri.getLastPathSegment() };
			break;
		case ContentDescriptor.HistoriesGraphic.TOKEN_BY_PAGE:
			break;
		case ContentDescriptor.HistoriesAudios.TOKEN:
			tableName = ContentDescriptor.HistoriesAudios.NAME;
			break;
		case ContentDescriptor.HistoriesFiles.TOKEN:
			tableName = ContentDescriptor.HistoriesFiles.NAME;
			break;
		case ContentDescriptor.RecentHistoriesMessage.TOKEN:
			tableName = ContentDescriptor.RecentHistoriesMessage.NAME;
			break;
		case ContentDescriptor.HistoriesMedia.TOKEN:
			tableName = ContentDescriptor.HistoriesMedia.NAME;
			break;
		case ContentDescriptor.HistoriesAddFriends.TOKEN:
			tableName = ContentDescriptor.HistoriesAddFriends.NAME;
			break;
		case ContentDescriptor.HistoriesCrowd.TOKEN:
			tableName = ContentDescriptor.HistoriesCrowd.NAME;
			break;
		default:
			throw new RuntimeException("Does not support operation ：" + token);
		}
//		Cursor c = qb.query(db, projection, selection, selectionArgs, null,
//				null, sortOrder);
		Cursor c = db.query(tableName, 
				null, selection, selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] args) {
		int ret = 0;
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int token = ContentDescriptor.URI_MATCHER.match(uri);
		String table = null;
		switch (token) {
		case ContentDescriptor.MessageItems.TOKEN:
			table = ContentDescriptor.MessageItems.NAME;
			break;
		case ContentDescriptor.Conversation.TOKEN:
			table = ContentDescriptor.Conversation.NAME;
			break;
		case ContentDescriptor.HistoriesMessage.TOKEN:
			table = ContentDescriptor.HistoriesMessage.NAME;
			break;
		case ContentDescriptor.HistoriesGraphic.TOKEN:
			table = ContentDescriptor.HistoriesGraphic.NAME;
			break;
		case ContentDescriptor.HistoriesAudios.TOKEN:
			table = ContentDescriptor.HistoriesAudios.NAME;
			break;
		case ContentDescriptor.HistoriesFiles.TOKEN:
			table = ContentDescriptor.HistoriesFiles.NAME;
			break;
		case ContentDescriptor.RecentHistoriesMessage.TOKEN:
			table = ContentDescriptor.RecentHistoriesMessage.NAME;
			break;
		case ContentDescriptor.HistoriesMedia.TOKEN:
			table = ContentDescriptor.HistoriesMedia.NAME;
			break;
		case ContentDescriptor.HistoriesAddFriends.TOKEN:
			table = ContentDescriptor.HistoriesAddFriends.NAME;
			break;
		case ContentDescriptor.HistoriesCrowd.TOKEN:
			table = ContentDescriptor.HistoriesCrowd.NAME;
			break;
		default:
			throw new RuntimeException("Does not support operation ：" + token);
		}
		if (table != null) {
			ret = db.update(table, values, selection, args);
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return ret;
	}

}
