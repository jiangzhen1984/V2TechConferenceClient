package com.v2tech.view.conversation;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.V2.jni.util.V2Log;
import com.v2tech.db.ContentDescriptor;
import com.v2tech.db.V2TechDBHelper;
import com.v2tech.db.ContentDescriptor.HistoriesMessage;
import com.v2tech.service.GlobalHolder;
import com.v2tech.util.XmlParser;
import com.v2tech.vo.User;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.VMessageAbstractItem;
import com.v2tech.vo.VMessageAudioItem;
import com.v2tech.vo.VMessageFaceItem;
import com.v2tech.vo.VMessageFileItem;
import com.v2tech.vo.VMessageImageItem;
import com.v2tech.vo.VMessageLinkTextItem;
import com.v2tech.vo.VMessageTextItem;

public class MessageLoader {

	/**
	 * 查询前要判断该用户的数据库是否存在，不存在则创建
	 * @param context
	 * @param uid2
	 * @return
	 */
	public static boolean init(Context context , long uid2){
		boolean result = isExistTable(context , "Histories_0_0_" + uid2);
		ContentDescriptor.HistoriesMessage.PATH = "Histories_0_0_" + uid2;
		ContentDescriptor.HistoriesMessage.NAME = "Histories_0_0_" + uid2;
		ContentDescriptor.HistoriesMessage.CONTENT_URI = ContentDescriptor.BASE_URI.buildUpon()
				.appendPath(ContentDescriptor.HistoriesMessage.PATH).build();
		ContentDescriptor.URI_MATCHER.addURI(ContentDescriptor.AUTHORITY, HistoriesMessage.PATH, HistoriesMessage.TOKEN);
		ContentDescriptor.URI_MATCHER.addURI(ContentDescriptor.AUTHORITY, HistoriesMessage.PATH + "/#", HistoriesMessage.TOKEN_WITH_ID);
		ContentDescriptor.URI_MATCHER.addURI(ContentDescriptor.AUTHORITY, HistoriesMessage.PATH + "/page", HistoriesMessage.TOKEN_BY_PAGE);
		
		if(!result){
			User user = GlobalHolder.getInstance().getUser(uid2);
			if(user == null){
				V2Log.e("the remote user is null , the uid2 is --" + uid2);
				return false;
			}
			ContentDescriptor.execSQLCreate(context, "Histories_0_0_" + uid2);
		}	
		return true;
	}
	/**
	 * Load all P2P message and order by date desc
	 * 
	 * @param context
	 * @param uid1
	 * @param uid2
	 * @param type
	 * @return
	 */
	public static List<VMessage> loadMessageByType(Context context, long uid1,
			long uid2, int type) {

		if(!init(context , uid2))
			return null;
		
		String selection = "((" + ContentDescriptor.Messages.Cols.FROM_USER_ID
				+ "=? and " + ContentDescriptor.Messages.Cols.TO_USER_ID
				+ "=? ) or " + "("
				+ ContentDescriptor.Messages.Cols.FROM_USER_ID + "=? and "
				+ ContentDescriptor.Messages.Cols.TO_USER_ID + "=? )) and "
				+ ContentDescriptor.Messages.Cols.GROUP_ID + "= 0 ";

		String[] args = new String[] { uid1 + "", uid2 + "", uid2 + "",
				uid1 + "" };

		String order = ContentDescriptor.Messages.Cols.SEND_TIME + " desc ";
		return queryMessage(context, selection, args, order, type);

	}

	public static List<VMessage> loadImageMessage(Context context, long uid1,
			long uid2) {
		return loadMessageByType(context, uid1, uid2,
				VMessageAbstractItem.ITEM_TYPE_IMAGE);
	}

	public static List<VMessage> loadMessage(Context context, long uid1,
			long uid2) {
		return loadMessageByType(context, uid1, uid2,
				VMessageAbstractItem.ITEM_TYPE_ALL);
	}

	public static VMessage loadMessageById(Context context, long msgId) {
		
		String selection = ContentDescriptor.HistoriesMessage.Cols.ID + "=? ";
		String[] args = new String[] { msgId + ""};
		String order = ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_SAVEDATE
				+ " desc limit 1 offset 0 ";
//		String selection = ContentDescriptor.Messages.Cols.ID + "=? ";
//		String order = ContentDescriptor.Messages.Cols.SEND_TIME
//				+ " desc limit 1 offset 0 ";
		List<VMessage> list = queryMessage(context, selection, args, order,
				VMessageAbstractItem.ITEM_TYPE_ALL);
		if (list != null && list.size() > 0) {
			return list.get(0);
		} else {
			return null;
		}
	}

	/**
	 * 
	 * @param context
	 * @param groupId
	 * @return
	 */
	public static List<VMessage> loadGroupMessageByPage(Context context,
			long groupId, int limit, int offset) {
//		String selection = ContentDescriptor.Messages.Cols.GROUP_ID + "=? ";
//		String[] args = new String[] { groupId + "" };
//		String order = ContentDescriptor.Messages.Cols.SEND_TIME
//				+ " desc limit " + limit + " offset  " + offset;
//		return queryMessage(context, selection, args, order,
//				VMessageAbstractItem.ITEM_TYPE_ALL);
		return null;
	}

	/**
	 * 
	 * @param context
	 * @param groupId
	 * @return
	 */
	public static List<VMessage> loadMessageByPage(Context context, long uid1,
			long uid2, int limit, int offset) {
		if(!init(context , uid2))
			return null;
		
		String selection = "((" + ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_FROM_USER_ID
				+ "=? and " + ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_TO_USER_ID
				+ "=? ) or " + "("
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_FROM_USER_ID + "=? and "
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_TO_USER_ID + "=? ))  and "
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_GROUP_TYPE + "= 0 ";
		
//		String selection = "((" + ContentDescriptor.Messages.Cols.FROM_USER_ID
//				+ "=? and " + ContentDescriptor.Messages.Cols.TO_USER_ID
//				+ "=? ) or " + "("
//				+ ContentDescriptor.Messages.Cols.FROM_USER_ID + "=? and "
//				+ ContentDescriptor.Messages.Cols.TO_USER_ID + "=? ))  and "
//				+ ContentDescriptor.Messages.Cols.GROUP_ID + "= 0 ";
//
		String[] args = new String[] { uid1 + "", uid2 + "", uid2 + "",
				uid1 + "" };
//
		String order = ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_SAVEDATE + " desc , "
				+ ContentDescriptor.HistoriesMessage.Cols.ID + " desc limit " + limit
				+ " offset  " + offset;
		return queryMessage(context, selection, args, order,
				VMessageAbstractItem.ITEM_TYPE_ALL);
	}

	/**
	 * 
	 * @param context
	 * @param groupId
	 * @return
	 */
	public static VMessage getNewestGroupMessage(Context context, long groupId) {
		
//		String selection = ContentDescriptor.Messages.Cols.GROUP_ID + "=? ";
//		String[] args = new String[] { groupId + "" };
//		String order = ContentDescriptor.Messages.Cols.SEND_TIME
//				+ " desc limit 1 offset 0 ";
//		List<VMessage> list = queryMessage(context, selection, args, order,
//				VMessageAbstractItem.ITEM_TYPE_ALL);
//		if (list != null && list.size() > 0) {
//			return list.get(0);
//		} else {
//			return null;
//		}
		return null;
	}

	public static VMessage getNewestMessage(Context context, long uid1,
			long uid2) {
		if(!init(context , uid2))
			return null;
//		String selection = "((" + ContentDescriptor.Messages.Cols.FROM_USER_ID
//				+ "=? and " + ContentDescriptor.Messages.Cols.TO_USER_ID
//				+ "=? ) or " + "("
//				+ ContentDescriptor.Messages.Cols.FROM_USER_ID + "=? and "
//				+ ContentDescriptor.Messages.Cols.TO_USER_ID + "=? ))  and "
//				+ ContentDescriptor.Messages.Cols.GROUP_ID + "= 0 ";
		String selection = "((" + ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_FROM_USER_ID
				+ "=? and " + ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_TO_USER_ID
				+ "=? ) or " + "("
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_FROM_USER_ID + "=? and "
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_TO_USER_ID + "=? ))  and "
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_GROUP_TYPE + "= 0 ";

		String[] args = new String[] { uid1 + "", uid2 + "", uid2 + "",
				uid1 + "" };

		String order = ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_SAVEDATE + " desc, "
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_ID
				+ " desc limit 1 offset 0 ";
//		String order = ContentDescriptor.Messages.Cols.SEND_TIME + " desc, "
//				+ ContentDescriptor.Messages.Cols.ID
//				+ " desc limit 1 offset 0 ";
		
		List<VMessage> list = queryMessage(context, selection, args, order,
				VMessageAbstractItem.ITEM_TYPE_ALL);
		if (list != null && list.size() > 0) {
			return list.get(0);
		} else {
			return null;
		}
	}

	/**
	 * Load all group message and order by date desc
	 * 
	 * @param context
	 * @param gid
	 * @param type
	 * @return
	 */
	public static List<VMessage> loadGroupMessageByType(Context context,
			long gid, int type) {
//		String selection = ContentDescriptor.Messages.Cols.GROUP_ID + "=? ";
//		String[] args = new String[] { gid + "" };
//		String order = ContentDescriptor.Messages.Cols.SEND_TIME + " desc ";
//		return queryMessage(context, selection, args, order, type);
		return null;

	}
	
	public static Long queryVMessageID(Context context , String selection, String[] args, String sortOrder){
		
		Cursor cursor = context.getContentResolver().query(ContentDescriptor.HistoriesMessage.CONTENT_URI,
				new String[]{"_id"}, selection, args, sortOrder);
		if(cursor != null && cursor.getCount() > 0 && cursor.moveToNext()){
			
			return cursor.getLong(cursor.getColumnIndex("_id"));
		}
		return null;
	}

	public static List<VMessage> queryMessage(Context context,
			String selection, String[] args, String sortOrder, int itemType) {

		Cursor mCur = context.getContentResolver().query(
				ContentDescriptor.HistoriesMessage.CONTENT_URI,
				ContentDescriptor.HistoriesMessage.Cols.ALL_CLOS, selection, args,
				sortOrder);
		
		List<VMessage> vimList = new ArrayList<VMessage>();
		if (mCur.getCount() == 0) {
			mCur.close();
			return vimList;
		}

		while (mCur.moveToNext()) {
//			VMessage vm = extractMsg(mCur);
			VMessage vm = XmlParser.parseForMessage(extractMsg(mCur));
//			loadVMessageItem(context, vm, itemType);
			if(vm == null){
				V2Log.e("the parse VMessage get null........");
				return vimList;
			}
			
			if (vm.getItems().size() > 0) {
				vimList.add(vm);
			}
		}

		mCur.close();

		return vimList;

	}

	public static List<VMessage> loadGroupImageMessage(Context context, long gid) {
		return loadGroupMessageByType(context, gid,
				VMessageAbstractItem.ITEM_TYPE_IMAGE);
	}

	public static List<VMessage> loadGroupMessage(Context context, long gid) {
		return loadGroupMessageByType(context, gid,
				VMessageAbstractItem.ITEM_TYPE_ALL);
	}

	public static int deleteMessage(Context context, VMessage vm) {
		if (vm == null) {
			return 0;
		}

		int ret = context.getContentResolver().delete(
				ContentDescriptor.Messages.CONTENT_URI,
				ContentDescriptor.Messages.Cols.ID + "=?",
				new String[] { vm.getId() + "" });

		// Delete message items
		context.getContentResolver().delete(
				ContentDescriptor.MessageItems.CONTENT_URI,
				ContentDescriptor.MessageItems.Cols.MSG_ID + "=?",
				new String[] { vm.getId() + "" });

		return ret;
	}

	private static VMessage extractMsg(Cursor cur) {
		if (cur.isClosed()) {
			throw new RuntimeException(" cursor is closed");
		}
//		long user1Id = cur.getLong(1);
//		long user2Id = cur.getLong(3);
		long user1Id = cur.getLong(cur.getColumnIndex(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_FROM_USER_ID));
		long user2Id = cur.getLong(cur.getColumnIndex(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_TO_USER_ID));
		User fromUser = GlobalHolder.getInstance().getUser(user1Id);
		if (fromUser == null) {
			fromUser = new User(user1Id);
		}
		User toUser = GlobalHolder.getInstance().getUser(user2Id);
		if (toUser == null) {
			toUser = new User(user2Id);
		}

		int id = cur.getInt(0);
//		// message type
		int type = cur.getInt(cur.getColumnIndex(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_GROUP_TYPE));
//		int type = cur.getInt(5);
//		// date time
		long dateString = cur.getLong(cur.getColumnIndex(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_SAVEDATE));
//		String dateString = cur.getString(6);
//		// group id
		long groupId = cur.getLong(cur.getColumnIndex(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_GROUP_ID));
//		long groupId = cur.getLong(8);
		int state = cur.getInt(cur.getColumnIndex(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_TRANSTATE));
//		int state = cur.getInt(7);
		// message id
		String uuid = cur.getString(cur.getColumnIndex(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_ID));
//		String uuid = cur.getString(9);
		String xml = cur.getString(cur.getColumnIndex(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_CONTENT));
		VMessage vm = new VMessage(groupId, fromUser, toUser, type);
		vm.setId(id);
		vm.setUUID(uuid);
		vm.setState(state);
		vm.setmXmlDatas(xml);
		vm.setDate(new Date(dateString));
		return vm;
	}

	private static void loadVMessageItem(Context context, VMessage vm,
			int msgType) {
		String selection = ContentDescriptor.MessageItems.Cols.MSG_ID + "=? ";
		String[] args = null;
		if (msgType != 0) {
			selection += "and " + ContentDescriptor.MessageItems.Cols.TYPE
					+ "=? ";
			args = new String[] { vm.getId() + "", msgType + "" };
		} else {
			args = new String[] { vm.getId() + "" };
		}

		Cursor cur = context.getContentResolver().query(
				ContentDescriptor.MessageItems.CONTENT_URI,
				ContentDescriptor.MessageItems.Cols.ALL_CLOS, selection, args,
				ContentDescriptor.MessageItems.Cols.ID);

		while (cur.moveToNext()) {
			int id = cur.getInt(0);
			// content
			String content = cur.getString(2);
			// Item type
			int type = cur.getInt(3);
			// new line flag
			int newLineFlag = cur.getInt(4);

			String uuid = cur.getString(5);

			int state = cur.getInt(6);

			VMessageAbstractItem vai = null;
			switch (type) {
			case VMessageAbstractItem.ITEM_TYPE_TEXT:
				vai = new VMessageTextItem(vm, content);
				break;
			case VMessageAbstractItem.ITEM_TYPE_FACE:
				vai = new VMessageFaceItem(vm, Integer.parseInt(content));
				break;
			case VMessageAbstractItem.ITEM_TYPE_IMAGE:
				vai = new VMessageImageItem(vm, content);
			case VMessageAbstractItem.ITEM_TYPE_AUDIO:
				if (content != null && !content.isEmpty()) {
					String[] str = content.split("\\|");
					if (str.length > 1) {
						vai = new VMessageAudioItem(vm, str[0],
								Integer.parseInt(str[1]));
					}
				}
				break;
			case VMessageAbstractItem.ITEM_TYPE_FILE: {
				String fileName = null;
				String filePath = null;
				long fileSize = 0;
				if (content != null && !content.isEmpty()) {
					String[] str = content.split("\\|");
					if (str.length > 2) {
						fileName = str[0];
						filePath = str[1];
						fileSize = Long.parseLong(str[2]);
					}
				}

				vai = new VMessageFileItem(vm, filePath);
				((VMessageFileItem) vai).setFileSize(fileSize);
				((VMessageFileItem) vai).setFileName(fileName);

			}
				break;
			case VMessageAbstractItem.ITEM_TYPE_LINK_TEXT:
				String[] str = content.split("\\|");
				vai = new VMessageLinkTextItem(vm, str[0], str[1]);
				break;

			}
			if (vai != null
					&& newLineFlag == VMessageAbstractItem.NEW_LINE_FLAG_VALUE) {
				vai.setNewLine(true);
			}

			vai.setId(id);
			vai.setUuid(uuid);
			vai.setState(state);
		}

		cur.close();
	}

	private static boolean isExistTable(Context context , String tabName){
		SQLiteDatabase base = context.openOrCreateDatabase(V2TechDBHelper.DB_NAME , 0, null);
		try {
            String sql = "select count(*) as c from sqlite_master where type ='table' "
            		+ "and name ='" + tabName.trim() + "' ";
            Cursor cursor = base.rawQuery(sql, null);
            if(cursor != null && cursor.getCount() > 0 && cursor.moveToNext()){
                 int count = cursor.getInt(0);
                 if(count > 0){
                     return true;
                 }
             }
		} 
		catch (Exception e) {
			V2Log.e("detection table " + tabName + " is failed..."); //检测失败
			e.getStackTrace();
		}  
		finally{
			if(base != null){
				base.close();
			}
		}
		return false;
	}
}
