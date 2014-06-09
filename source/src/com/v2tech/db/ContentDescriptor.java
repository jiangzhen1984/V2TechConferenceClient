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
		matcher.addURI(augura, Messages.PATH + "/#", Messages.TOKEN_WITH_ID);
		matcher.addURI(augura, Messages.PATH + "/page", Messages.TOKEN_BY_PAGE);
		matcher.addURI(augura, MessageItems.PATH, MessageItems.TOKEN);
		matcher.addURI(augura, Conversation.PATH, Conversation.TOKEN);

		return matcher;

	}

	public static class Messages {

		public static final String PATH = "messages";

		public static final String NAME = PATH;

		public static final int TOKEN = 1;

		public static final int TOKEN_WITH_ID = 2;
		
		public static final int TOKEN_BY_PAGE = 3;

		public static final Uri CONTENT_URI = BASE_URI.buildUpon()
				.appendPath(PATH).build();

		public static class Cols {

			public static final String ID = BaseColumns._ID;

			public static final String FROM_USER_ID = "from_user_id";
			public static final String FROM_USER_NAME = "from_user_name";
			public static final String TO_USER_ID = "to_user_id";
			public static final String TO_USER_NAME = "to_user_name";
			public static final String MSG_TYPE = "msg_type";
			public static final String SEND_TIME = "send_time";
			public static final String STATE = "state";
			public static final String GROUP_ID = "group_id";

			public static final String[] ALL_CLOS = { ID, FROM_USER_ID,
					FROM_USER_NAME, TO_USER_ID, TO_USER_NAME, MSG_TYPE,
					SEND_TIME, STATE, GROUP_ID };
		}
	}

	public static class MessageItems {

		public static final String PATH = "messageitems";

		public static final String NAME = PATH;

		public static final int TOKEN = 5;

		public static final int TOKEN_WITH_ID = 6;

		public static final Uri CONTENT_URI = BASE_URI.buildUpon()
				.appendPath(PATH).build();

		public static class Cols {

			public static final String ID = BaseColumns._ID;

			public static final String MSG_ID = "msg_id";
			public static final String CONTENT = "content";
			public static final String TYPE = "item_type";
			public static final String LINE = "is_new_line";
			public static final String RESERVE = "rt_1";

			public static final String[] ALL_CLOS = { ID, MSG_ID, CONTENT,
					TYPE, LINE,  RESERVE };
		}
	}

	public static class Conversation {

		public static final String PATH = "conversation";

		public static final String NAME = PATH;

		public static final int TOKEN = 7;

		public static final int TOKEN_WITH_ID = 8;

		public static final Uri CONTENT_URI = BASE_URI.buildUpon()
				.appendPath(PATH).build();

		public static class Cols {

			public static final String ID = BaseColumns._ID;
			public static final String TYPE = "cov_type";
			public static final String EXT_ID = "ext_id";
			public static final String EXT_NAME = "ext_name";
			public static final String NOTI_FLAG = "noti_flag";
			public static final String OWNER = "t_owner";

			public static final String[] ALL_CLOS = { ID, TYPE, EXT_ID,
					EXT_NAME, NOTI_FLAG, OWNER };
		}
	}
}
