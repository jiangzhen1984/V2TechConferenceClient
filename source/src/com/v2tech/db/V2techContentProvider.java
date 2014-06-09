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
		int ret = 0 ;
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
		long id;
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int token = ContentDescriptor.URI_MATCHER.match(uri);
		switch (token) {
		case ContentDescriptor.Messages.TOKEN:
			id = db.insert(ContentDescriptor.Messages.NAME, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			return ContentDescriptor.Messages.CONTENT_URI.buildUpon()
					.appendPath(String.valueOf(id)).build();
		case ContentDescriptor.Conversation.TOKEN:
			id = db.insert(ContentDescriptor.Conversation.NAME, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			return ContentDescriptor.Conversation.CONTENT_URI.buildUpon()
					.appendPath(String.valueOf(id)).build();
		case ContentDescriptor.MessageItems.TOKEN:
			id = db.insert(ContentDescriptor.MessageItems.NAME, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			return ContentDescriptor.MessageItems.CONTENT_URI.buildUpon()
					.appendPath(String.valueOf(id)).build();
		}
		return null;
	}

	@Override
	public boolean onCreate() {
		mOpenHelper = new V2TechDBHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		int token = ContentDescriptor.URI_MATCHER.match(uri);
		switch (token) {
		case ContentDescriptor.Messages.TOKEN:
			qb.setTables(ContentDescriptor.Messages.NAME);
			break;
		case ContentDescriptor.Messages.TOKEN_WITH_ID:
			qb.setTables(ContentDescriptor.Messages.NAME);
			selection = ContentDescriptor.Messages.Cols.ID + "=?  ";
			selectionArgs = new String[] { uri.getLastPathSegment() };
			break;
		case ContentDescriptor.Messages.TOKEN_BY_PAGE:
			break;
		case ContentDescriptor.MessageItems.TOKEN:
			qb.setTables(ContentDescriptor.MessageItems.NAME);
			break;
		case ContentDescriptor.Conversation.TOKEN:
			qb.setTables(ContentDescriptor.Conversation.NAME);
		default:
			break;
		}
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null,
				null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] args) {
		int ret = 0 ;
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
		}
		if (table != null) {
			ret = db.update(table, values, selection, args);
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return ret;
	}

}
