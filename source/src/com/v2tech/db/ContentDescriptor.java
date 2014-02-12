package com.v2tech.db;

import android.content.UriMatcher;
import android.net.Uri;
import android.provider.BaseColumns;

public final class ContentDescriptor {
	
	

	public static final String AUTHORITY = "com.v2tech.bizcom";

	private static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY);

	public static final UriMatcher URI_MATCHER = buildUriMatcher();

	private ContentDescriptor() {
	};

	private static UriMatcher buildUriMatcher() {
		final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
		final String augura = AUTHORITY;

		matcher.addURI(augura, Messages.PATH, Messages.TOKEN);

		return matcher;

	}

	public static class Messages {

		public static final String PATH = "messages";

		public static final String NAME = PATH;

		public static final int TOKEN = 1;

		public static final Uri CONTENT_URI = BASE_URI.buildUpon()
				.appendPath(PATH).build();

		public static class Cols {

			public static final String ID = BaseColumns._ID;

			public static final String FROM_USER_ID = "from_user_id";
			public static final String FROM_USER_NAME = "from_user_name";
			public static final String TO_USER_ID = "to_user_id";
			public static final String TO_USER_NAME = "to_user_name";
			public static final String MSG_CONTENT = "msg_content";
			public static final String MSG_TYPE = "msg_type";
			public static final String SEND_TIME = "send_time";
			public static final String STATE = "state";

			public static final String[] ALL_CLOS = { ID, FROM_USER_ID,
					FROM_USER_NAME, TO_USER_ID, TO_USER_NAME, MSG_CONTENT,
					MSG_TYPE, SEND_TIME, STATE };
		}
	}
}
