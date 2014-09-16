package com.v2tech.db;

import android.content.Context;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

/**
 * ChatFiles： TransState --传输状态 -3:等待接收，-2:正在传输, -1: 传输暂停, 0:传输成功, 其它值为传输失败  
 * @author 
 *
 */
public final class ContentDescriptor {

	public static final String AUTHORITY = "com.v2tech.bizcom";

	public static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY);

	public static final UriMatcher URI_MATCHER = buildUriMatcher();
	public static final String BASE_OWNER_USER_ID = "OwnerUserID";
	public static final String BASE_SAVEDATE = "SaveDate";

	private ContentDescriptor() {
	};

	private static UriMatcher buildUriMatcher() {
		final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
		final String augura = AUTHORITY;

		matcher.addURI(augura, HistoriesMessage.PATH, HistoriesMessage.TOKEN);
		matcher.addURI(augura, HistoriesMessage.PATH + "/#", HistoriesMessage.TOKEN_WITH_ID);
		matcher.addURI(augura, HistoriesMessage.PATH + "/page", HistoriesMessage.TOKEN_BY_PAGE);
		
		matcher.addURI(augura, HistoriesGraphic.PATH, HistoriesGraphic.TOKEN);
		matcher.addURI(augura, HistoriesGraphic.PATH + "/#", HistoriesGraphic.TOKEN_WITH_ID);
		matcher.addURI(augura, HistoriesGraphic.PATH + "/page", HistoriesGraphic.TOKEN_BY_PAGE);
		
		matcher.addURI(augura, HistoriesAudios.PATH, HistoriesAudios.TOKEN);
		matcher.addURI(augura, HistoriesAudios.PATH + "/#", HistoriesAudios.TOKEN);
		
		matcher.addURI(augura, HistoriesFiles.PATH, HistoriesFiles.TOKEN);
		matcher.addURI(augura, HistoriesFiles.PATH + "/#", HistoriesFiles.TOKEN);
		
		matcher.addURI(augura, RecentHistoriesMessage.PATH, RecentHistoriesMessage.TOKEN);
		matcher.addURI(augura, RecentHistoriesMessage.PATH + "/#", RecentHistoriesMessage.TOKEN);
		
		matcher.addURI(augura, HistoriesMedia.PATH, HistoriesMedia.TOKEN);
		matcher.addURI(augura, HistoriesMedia.PATH + "/#", HistoriesMedia.TOKEN);
		
		matcher.addURI(augura, HistoriesAddFriends.PATH, HistoriesAddFriends.TOKEN);
		matcher.addURI(augura, HistoriesAddFriends.PATH + "/#", HistoriesAddFriends.TOKEN);
		
		matcher.addURI(augura, HistoriesCrowd.PATH, HistoriesCrowd.TOKEN);
		matcher.addURI(augura, HistoriesCrowd.PATH + "/#", HistoriesCrowd.TOKEN);


		return matcher;
	}

	/**
	 * 创建好友数据库表
	 * @param context
	 */
	public static boolean execSQLCreate(Context context , String tableName){
		if(TextUtils.isEmpty(tableName)){
			return false;
		}
		
		DataBaseContext dbContext = new DataBaseContext(context);
		SQLiteDatabase base = dbContext.openOrCreateDatabase(V2TechDBHelper.DB_NAME , Context.MODE_WORLD_WRITEABLE | Context.MODE_WORLD_READABLE, null);
		String sql = " create table "
				+ tableName + " ( "
				+ ContentDescriptor.HistoriesMessage.Cols.ID
				+ " integer primary key AUTOINCREMENT,"
				+ ContentDescriptor.HistoriesMessage.Cols.OWNER_USER_ID + " bigint," 
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_GROUP_TYPE + " bigint,"
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_GROUP_ID + " bigint,"
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_REMOTE_USER_ID + " bigint,"
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_FROM_USER_ID + " bigint,"
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_TO_USER_ID + " bigint,"
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_ID + " nvarchar(4000),"
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_CONTENT + " binary,"
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_SAVEDATE + " bignit,"
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_TRANSTATE + " bignit)";
		base.execSQL(sql);
		base.close();
		return true;
	}
	
	/**
	 * 聊天历史消息记录表
	 * @author 
	 *
	 */
	public static class HistoriesMessage {

		public static String PATH = "";

		public static String NAME = PATH;
		
		public static final int TOKEN = 1;

		public static final int TOKEN_WITH_ID = 2;
		
		public static final int TOKEN_BY_PAGE = 3;

		public static Uri CONTENT_URI = BASE_URI.buildUpon()
				.appendPath(PATH).build();

		public static class Cols {

			public static final String ID = BaseColumns._ID;

			public static String OWNER_USER_ID = BASE_OWNER_USER_ID;
			public static String HISTORY_MESSAGE_SAVEDATE = BASE_SAVEDATE;
			public static final String HISTORY_MESSAGE_GROUP_TYPE = "GroupType";
			public static final String HISTORY_MESSAGE_GROUP_ID = "GroupID";
			public static final String HISTORY_MESSAGE_REMOTE_USER_ID = "RemoteUserID";
			public static final String HISTORY_MESSAGE_FROM_USER_ID = "FromUserID";
			public static final String HISTORY_MESSAGE_TO_USER_ID = "ToUserID";
			public static final String HISTORY_MESSAGE_ID = "MsgID";
			public static final String HISTORY_MESSAGE_CONTENT = "MsgContent";
			public static final String HISTORY_MESSAGE_TRANSTATE = "TransState";
			
			public static final String[] ALL_CLOS = { ID, HISTORY_MESSAGE_GROUP_TYPE,
				HISTORY_MESSAGE_GROUP_ID, HISTORY_MESSAGE_REMOTE_USER_ID , HISTORY_MESSAGE_FROM_USER_ID, HISTORY_MESSAGE_TO_USER_ID ,HISTORY_MESSAGE_ID, HISTORY_MESSAGE_CONTENT,
				HISTORY_MESSAGE_SAVEDATE  , HISTORY_MESSAGE_TRANSTATE};
		}
	}

	/**
	 * 聊天收发图片记录表
	 * @author 
	 *
	 */
	public static class HistoriesGraphic {

		public static final String PATH = "ChatGraphics";

		public static final String NAME = PATH;

		public static final int TOKEN = 4;

		public static final int TOKEN_WITH_ID = 5;
		
		public static final int TOKEN_BY_PAGE = 6;

		public static final Uri CONTENT_URI = BASE_URI.buildUpon()
				.appendPath(PATH).build();

		public static class Cols {

			public static final String ID = BaseColumns._ID;

			public static String OWNER_USER_ID = BASE_OWNER_USER_ID;
			public static String HISTORY_GRAPHIC_SAVEDATE = BASE_SAVEDATE;
			public static final String HISTORY_GRAPHIC_GROUP_TYPE = "GroupType";
			public static final String HISTORY_GRAPHIC_GROUP_ID = "GroupID";
			public static final String HISTORY_GRAPHIC_FROM_USER_ID = "FromUserID";
			public static final String HISTORY_GRAPHIC_TO_USER_ID = "ToUserID";
			public static final String HISTORY_GRAPHIC_REMOTE_USER_ID = "RemoteUserID";
			public static final String HISTORY_GRAPHIC_ID = "GraphicID";
			public static final String HISTORY_GRAPHIC_PATH = "FileExt";
			public static final String HISTORY_GRAPHIC_TRANSTATE = "TransState";

			public static final String[] ALL_CLOS = { ID, OWNER_USER_ID, HISTORY_GRAPHIC_SAVEDATE,HISTORY_GRAPHIC_GROUP_TYPE ,
				HISTORY_GRAPHIC_GROUP_ID , HISTORY_GRAPHIC_FROM_USER_ID, HISTORY_GRAPHIC_TO_USER_ID,  HISTORY_GRAPHIC_REMOTE_USER_ID ,
				HISTORY_GRAPHIC_ID, HISTORY_GRAPHIC_PATH , HISTORY_GRAPHIC_TRANSTATE};
		}
	}

	/**
	 * 聊天收发的语音留言记录表
	 * @author 
	 *
	 */
	public static class HistoriesAudios {

		public static final String PATH = "ChatAudios";

		public static final String NAME = PATH;

		public static final int TOKEN = 7;

		public static final int TOKEN_WITH_ID = 8;

		public static final Uri CONTENT_URI = BASE_URI.buildUpon()
				.appendPath(PATH).build();

		public static class Cols {

			public static final String ID = BaseColumns._ID;
			
			public static String OWNER_USER_ID = BASE_OWNER_USER_ID;
			public static String HISTORY_AUDIO_SAVEDATE = BASE_SAVEDATE;
			public static final String HISTORY_AUDIO_GROUP_TYPE = "GroupType";
			public static final String HISTORY_AUDIO_GROUP_ID = "GroupID";
			public static final String HISTORY_AUDIO_FROM_USER_ID = "FromUserID";
			public static final String HISTORY_AUDIO_TO_USER_ID = "ToUserID";
			public static final String HISTORY_AUDIO_REMOTE_USER_ID = "RemoteUserID";
			public static final String HISTORY_AUDIO_ID = "AudioID";
			public static final String HISTORY_AUDIO_PATH = "FileExt";
			public static final String HISTORY_AUDIO_SEND_STATE = "TransState";
			public static final String HISTORY_AUDIO_READ_STATE = "ReadState";
			public static final String HISTORY_AUDIO_SECOND = "AudioSeconds";
			
			public static final String[] ALL_CLOS = { ID, OWNER_USER_ID, HISTORY_AUDIO_SAVEDATE,HISTORY_AUDIO_GROUP_TYPE ,
				HISTORY_AUDIO_GROUP_ID , HISTORY_AUDIO_FROM_USER_ID, HISTORY_AUDIO_TO_USER_ID, HISTORY_AUDIO_ID , HISTORY_AUDIO_PATH , HISTORY_AUDIO_SEND_STATE ,
			   HISTORY_AUDIO_SECOND , HISTORY_AUDIO_REMOTE_USER_ID};
		}
	}
	
	/**
	 * 聊天收发的文件记录表
	 * @author 
	 *
	 */
	public static class HistoriesFiles {

		public static final String PATH = "ChatFiles";

		public static final String NAME = PATH;

		public static final int TOKEN = 9;

		public static final int TOKEN_WITH_ID = 10;

		public static final Uri CONTENT_URI = BASE_URI.buildUpon()
				.appendPath(PATH).build();

		public static class Cols {

			public static final String ID = BaseColumns._ID;
			
			public static String OWNER_USER_ID = BASE_OWNER_USER_ID;
			public static String HISTORY_FILE_SAVEDATE = BASE_SAVEDATE;
			public static final String HISTORY_FILE_FROM_USER_ID = "FromUserID";
			public static final String HISTORY_FILE_TO_USER_ID = "ToUserID";
			public static final String HISTORY_FILE_REMOTE_USER_ID = "RemoteUserID";
			public static final String HISTORY_FILE_ID = "FileID";
			public static final String HISTORY_FILE_PATH = "FileName";
			public static final String HISTORY_FILE_SEND_STATE = "TransState";
//			public static final String HISTORY_FILE_READ_STATE = "ReadState";
			public static final String HISTORY_FILE_SIZE = "FileSize";
			
			public static final String[] ALL_CLOS = { ID, OWNER_USER_ID, HISTORY_FILE_SAVEDATE,HISTORY_FILE_REMOTE_USER_ID,
				HISTORY_FILE_FROM_USER_ID, HISTORY_FILE_TO_USER_ID, HISTORY_FILE_ID , HISTORY_FILE_PATH , 
				HISTORY_FILE_SEND_STATE  , HISTORY_FILE_SIZE};
		}
	}
	
	/**
	 * 最近联系人表
	 * @author 
	 *
	 */
	public static class RecentHistoriesMessage {

		public static final String PATH = "Recents";

		public static final String NAME = PATH;

		public static final int TOKEN = 11;

		public static final int TOKEN_WITH_ID = 12;

		public static final Uri CONTENT_URI = BASE_URI.buildUpon()
				.appendPath(PATH).build();

		public static class Cols {

			public static final String ID = BaseColumns._ID;
			
			public static String OWNER_USER_ID = BASE_OWNER_USER_ID;
			public static String HISTORY_RECENT_MESSAGE_SAVEDATE = BASE_SAVEDATE;
			public static final String HISTORY_RECENT_MESSAGE_GROUP_TYPE = "GroupType";
			public static final String HISTORY_RECENT_MESSAGE_USER_TYPE_ID = "GroupID";
			public static final String HISTORY_RECENT_MESSAGE_FROM_USER_ID = "FromUserID";
			public static final String HISTORY_RECENT_MESSAGE_TO_USER_ID = "ToUserID";
			public static final String HISTORY_RECENT_MESSAGE_REMOTE_USER_ID = "RemoteUserID";
			public static final String HISTORY_RECENT_MESSAGE_ID = "MsgID";
			public static final String HISTORY_RECENT_MESSAGE_CONTENT = "MsgContent";
			public static final String HISTORY_RECENT_MESSAGE_READ_STATE = "ReadState";
			
			public static final String[] ALL_CLOS = { ID, OWNER_USER_ID, HISTORY_RECENT_MESSAGE_SAVEDATE, HISTORY_RECENT_MESSAGE_TO_USER_ID,
				HISTORY_RECENT_MESSAGE_GROUP_TYPE, HISTORY_RECENT_MESSAGE_USER_TYPE_ID, HISTORY_RECENT_MESSAGE_FROM_USER_ID
				, HISTORY_RECENT_MESSAGE_ID , HISTORY_RECENT_MESSAGE_CONTENT ,HISTORY_RECENT_MESSAGE_READ_STATE , HISTORY_RECENT_MESSAGE_REMOTE_USER_ID};
		}
	}
	
	/**
	 * 音视频聊天记录
	 * @author 
	 *
	 */
	public static class HistoriesMedia {

		public static final String PATH = "ChatMedias";

		public static final String NAME = PATH;

		public static final int TOKEN = 13;

		public static final int TOKEN_WITH_ID = 14;

		public static final Uri CONTENT_URI = BASE_URI.buildUpon()
				.appendPath(PATH).build();

		public static class Cols {

			public static final String ID = BaseColumns._ID;
			
			public static String OWNER_USER_ID = BASE_OWNER_USER_ID;
			public static String HISTORY_MEDIA_SAVEDATE = BASE_SAVEDATE;
			public static final String HISTORY_MEDIA_CHAT_ID = "MediaChatID";
			public static final String HISTORY_MEDIA_FROM_USER_ID = "FromUserID";
			public static final String HISTORY_MEDIA_TO_USER_ID = "ToUserID";
			public static final String HISTORY_MEDIA_REMOTE_USER_ID = "RemoteUserID";
			public static final String HISTORY_MEDIA_TYPE = "MediaType";
			public static final String HISTORY_MEDIA_STATE = "MediaState";
			public static final String HISTORY_MEDIA_START_DATE = "StartDate";
			public static final String HISTORY_MEDIA_END_DATE = "EndDate";
			public static final String HISTORY_MEDIA_READ_STATE = "ReadState";
			
			public static final String[] ALL_CLOS = { ID, OWNER_USER_ID, HISTORY_MEDIA_SAVEDATE,HISTORY_MEDIA_CHAT_ID,
				HISTORY_MEDIA_FROM_USER_ID, HISTORY_MEDIA_TO_USER_ID, HISTORY_MEDIA_TYPE , HISTORY_MEDIA_STATE, HISTORY_MEDIA_REMOTE_USER_ID
				, HISTORY_MEDIA_START_DATE , HISTORY_MEDIA_END_DATE ,HISTORY_MEDIA_READ_STATE};
		}
	}
	
	/**
	 * 添加好友历史记录表
=======
	/**
	 * ��Ӻ�����ʷ��¼��
>>>>>>> 2670ca0... 1.增加好友
	 * @author 
	 *
	 */
	public static class HistoriesAddFriends {

		public static final String PATH = "AddFriendHistories";

		public static final String NAME = PATH;

		public static final int TOKEN = 15;

		public static final int TOKEN_WITH_ID = 16;

		public static final Uri CONTENT_URI = BASE_URI.buildUpon()
				.appendPath(PATH).build();

		public static class Cols {

			public static final String ID = BaseColumns._ID;
			
			public static String OWNER_USER_ID = BASE_OWNER_USER_ID;
			public static String HISTORY_FRIEND_SAVEDATE = BASE_SAVEDATE;
			public static String HISTORY_FRIEND_AUTHTYPE = "OwnerAuthType";
			public static final String HISTORY_FRIEND_FROM_USER_ID = "FromUserID";
			public static final String HISTORY_FRIEND_TO_USER_ID = "ToUserID";
			public static final String HISTORY_FRIEND_REMOTE_USER_ID = "RemoteUserID";
			public static final String HISTORY_FRIEND_APPLY_REASON = "ApplyReason";
			public static final String HISTORY_FRIEND_REFUSE_REASON = "RefuseReason";
			public static final String HISTORY_FRIEND_STATE = "AddState";
			public static final String HISTORY_MEDIA_READ_STATE = "ReadState";
			
			public static final String[] ALL_CLOS = { ID, OWNER_USER_ID, HISTORY_FRIEND_SAVEDATE,HISTORY_FRIEND_AUTHTYPE,
				HISTORY_FRIEND_FROM_USER_ID, HISTORY_FRIEND_TO_USER_ID, HISTORY_FRIEND_REMOTE_USER_ID , HISTORY_FRIEND_APPLY_REASON , HISTORY_FRIEND_REFUSE_REASON , 
				HISTORY_FRIEND_STATE , HISTORY_MEDIA_READ_STATE};
		}
	}
	
	/**
	 * 加群历史记录表
	 * @author 
	 *
	 */
	public static class HistoriesCrowd {

		public static final String PATH = "JoinCrowdHistories";

		public static final String NAME = PATH;

		public static final int TOKEN = 17;

		public static final int TOKEN_WITH_ID = 18;

		public static final Uri CONTENT_URI = BASE_URI.buildUpon()
				.appendPath(PATH).build();

		public static class Cols {

			public static final String ID = BaseColumns._ID;
			
			public static String OWNER_USER_ID = BASE_OWNER_USER_ID;
			public static String HISTORY_CROWD_SAVEDATE = BASE_SAVEDATE;
			public static final String HISTORY_CROWD_ID = "CrowdID";
			public static final String HISTORY_CROWD_AUTHTYPE = "CrowdAuthType";
			public static final String HISTORY_CROWD_FROM_USER_ID = "FromUserID";
			public static final String HISTORY_CROWD_REMOTE_USER_ID = "RemoteUserID";
			public static final String HISTORY_CROWD_TO_USER_ID = "ToUserID";
			public static final String HISTORY_CROWD_APPLY_REASON = "ApplyReason";
			public static final String HISTORY_CROWD_REFUSE_REASON = "RefuseReason";
			public static final String HISTORY_CROWD_STATE = "JoinState";
			public static final String HISTORY_CROWD_READ_STATE = "ReadState";
			public static final String HISTORY_CROWD_BASE_INFO = "CrowdXml";
			
			public static final String[] ALL_CLOS = { ID, OWNER_USER_ID, HISTORY_CROWD_SAVEDATE,HISTORY_CROWD_AUTHTYPE , 
				HISTORY_CROWD_ID, HISTORY_CROWD_FROM_USER_ID, HISTORY_CROWD_REMOTE_USER_ID , HISTORY_CROWD_APPLY_REASON
				, HISTORY_CROWD_REFUSE_REASON , HISTORY_CROWD_STATE ,HISTORY_CROWD_READ_STATE , HISTORY_CROWD_TO_USER_ID , HISTORY_CROWD_BASE_INFO};
		}
	}
	
	public static class Messages {

		public static final String PATH = "messages";

		public static final String NAME = PATH;

		public static final int TOKEN = 30;

		public static final int TOKEN_WITH_ID = 31;
		
		public static final int TOKEN_BY_PAGE = 32;

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
			public static final String UUID = "uuid";

			public static final String[] ALL_CLOS = { ID, FROM_USER_ID,
					FROM_USER_NAME, TO_USER_ID, TO_USER_NAME, MSG_TYPE,
					SEND_TIME, STATE, GROUP_ID, UUID };
		}
	}

	public static class MessageItems {

		public static final String PATH = "messageitems";

		public static final String NAME = PATH;

		public static final int TOKEN = 33;

		public static final int TOKEN_WITH_ID = 34;

		public static final Uri CONTENT_URI = BASE_URI.buildUpon()
				.appendPath(PATH).build();

		public static class Cols {

			public static final String ID = BaseColumns._ID;

			public static final String MSG_ID = "msg_id";
			public static final String CONTENT = "content";
			public static final String TYPE = "item_type";
			public static final String LINE = "is_new_line";
			public static final String UUID = "uuid";
			public static final String STATE = "item_state";

			public static final String[] ALL_CLOS = { ID, MSG_ID, CONTENT,
					TYPE, LINE,  UUID, STATE };
		}
	}

	public static class Conversation {

		public static final String PATH = "conversation";

		public static final String NAME = PATH;

		public static final int TOKEN = 35;

		public static final int TOKEN_WITH_ID = 36;

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
