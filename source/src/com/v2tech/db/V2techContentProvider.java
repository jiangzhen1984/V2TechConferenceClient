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
	public int delete(Uri arg0, String arg1, String[] arg2) {
		return 0;
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
	public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
		return 0;
	}

}
