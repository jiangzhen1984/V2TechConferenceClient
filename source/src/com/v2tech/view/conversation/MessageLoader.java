package com.v2tech.view.conversation;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.V2.jni.util.V2Log;
import com.v2tech.db.ContentDescriptor;
import com.v2tech.db.ContentDescriptor.HistoriesMessage;
import com.v2tech.db.DataBaseContext;
import com.v2tech.db.V2TechDBHelper;
import com.v2tech.service.GlobalHolder;
import com.v2tech.util.XmlParser;
import com.v2tech.vo.AudioVideoMessageBean;
import com.v2tech.vo.User;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.VMessageAudioItem;
import com.v2tech.vo.VMessageFileItem;
import com.v2tech.vo.VMessageImageItem;
import com.v2tech.vo.VideoBean;

public class MessageLoader {

	public static final int CONTACT_TYPE = -5;
	public static final int CROWD_TYPE = -6;

	/**
	 * 查询前要判断该用户的数据库是否存在，不存在则创建
	 * 
	 * @param context
	 * @param uid2
	 * @return
	 */
	public static boolean init(Context context, long groupType, long groupID,
			long uid2, int type) {

		String name = "";
		switch (type) {
		case CROWD_TYPE:
			name = "Histories_" + groupType + "_" + groupID + "_" + uid2;
			break;
		case CONTACT_TYPE:
			name = "Histories_0_0_" + uid2;
			break;
		default:
			throw new RuntimeException("create database fialed...");
		}
		boolean result = isExistTable(context, name);
		ContentDescriptor.HistoriesMessage.PATH = name;
		ContentDescriptor.HistoriesMessage.NAME = name;
		ContentDescriptor.URI_MATCHER.addURI(ContentDescriptor.AUTHORITY,
				HistoriesMessage.PATH, HistoriesMessage.TOKEN);
		ContentDescriptor.URI_MATCHER.addURI(ContentDescriptor.AUTHORITY,
				HistoriesMessage.PATH + "/#", HistoriesMessage.TOKEN_WITH_ID);
		ContentDescriptor.URI_MATCHER
				.addURI(ContentDescriptor.AUTHORITY, HistoriesMessage.PATH
						+ "/page", HistoriesMessage.TOKEN_BY_PAGE);
		ContentDescriptor.HistoriesMessage.CONTENT_URI = ContentDescriptor.BASE_URI
				.buildUpon()
				.appendPath(ContentDescriptor.HistoriesMessage.PATH).build();
		if (!result) {
			// if(type == CONTACT_TYPE){
			//
			// User user = GlobalHolder.getInstance().getUser(uid2);
			// if(user == null){
			// V2Log.e("the remote user is null , the uid2 is --" + uid2);
			// return false;
			// }
			// }
			ContentDescriptor.execSQLCreate(context, name);
		}
		return true;
	}

	/**
	 * 加载指定用户之间所有通信数据
	 * 
	 * @param context
	 * @param uid1
	 * @param uid2
	 * @return
	 */
	public static List<VMessage> loadMessage(Context context, long uid1,
			long uid2) {
		if (!init(context, 0, 0, uid2, CONTACT_TYPE))
			return null;
		String selection = "(("
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_FROM_USER_ID
				+ "=? and "
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_TO_USER_ID
				+ "=? ) or "
				+ "("
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_FROM_USER_ID
				+ "=? and "
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_TO_USER_ID
				+ "=? )) and "
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_GROUP_TYPE
				+ "= 0 ";
		String order = ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_SAVEDATE
				+ " desc ";

		String[] args = new String[] { uid1 + "", uid2 + "", uid2 + "",
				uid1 + "" };
		return queryMessage(context, selection, args, order);
	}

	/**
	 * 分页加载聊天消息数据
	 * @param context
	 * @param uid1
	 * @param uid2
	 * @param limit
	 * @param offset
	 * @param groupType
	 * @return
	 */
	public static List<VMessage> loadMessageByPage(Context context, long uid1,
			long uid2, int limit, int offset, int groupType) {
		if (!init(context, 0, 0, uid2, CONTACT_TYPE))
			return null;

		String selection = "(("
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_FROM_USER_ID
				+ "=? and "
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_TO_USER_ID
				+ "=? ) or "
				+ "("
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_FROM_USER_ID
				+ "=? and "
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_TO_USER_ID
				+ "=? ))  and "
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_GROUP_TYPE
				+ "= ?";

		String[] args = new String[] { String.valueOf(uid1),
				String.valueOf(uid2), String.valueOf(uid2),
				String.valueOf(uid1) , String.valueOf(groupType)};

		String order = ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_SAVEDATE
				+ " desc , "
				+ ContentDescriptor.HistoriesMessage.Cols.ID
				+ " desc limit " + limit + " offset  " + offset;
		List<VMessage> queryMessage = queryMessage(context, selection, args,
				order);
		return queryMessage;
	}

	public static VMessage loadMessageById(Context context, long msgId) {

		String selection = ContentDescriptor.HistoriesMessage.Cols.ID + "=? ";
		String[] args = new String[] { msgId + "" };
		String order = ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_SAVEDATE
				+ " desc limit 1 offset 0 ";
		List<VMessage> list = queryMessage(context, selection, args, order);
		if (list != null && list.size() > 0) {
			return list.get(0);
		} else {
			return null;
		}
	}

	/**
	 * 根据指定用户id，加载出所有图片
	 * @param context
	 * @param uid1
	 * @param uid2
	 * @return
	 */
	public static List<VMessage> loadImageMessage(Context context, long uid1,
			long uid2) {
		List<VMessage> imageItems = new ArrayList<VMessage>();
		String sortOrder = ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_SAVEDATE
				+ " desc";
		Uri uri = ContentDescriptor.HistoriesGraphic.CONTENT_URI;
		String[] projection = ContentDescriptor.HistoriesGraphic.Cols.ALL_CLOS;
		String selection = "(("
				+ ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_FROM_USER_ID
				+ "=? and "
				+ ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_TO_USER_ID
				+ "=? ) or "
				+ "("
				+ ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_FROM_USER_ID
				+ "=? and "
				+ ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_TO_USER_ID
				+ "=? ))  and "
				+ ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_GROUP_TYPE
				+ "= 0";
		String[] args = new String[] { String.valueOf(uid1),
				String.valueOf(uid2), String.valueOf(uid2),
				String.valueOf(uid1) };
		DataBaseContext mContext = new DataBaseContext(context);
		Cursor mCur = mContext.getContentResolver().query(uri, projection,
				selection, args, sortOrder);
		if (mCur.getCount() == 0)
			return imageItems;

		VMessageImageItem item = null;
		VMessage current = null;
		while (mCur.moveToNext()) {
			int groupType = mCur
					.getInt(mCur
							.getColumnIndex(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_GROUP_TYPE));
			long groupID = mCur
					.getLong(mCur
							.getColumnIndex(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_GROUP_ID));
			long fromUserID = mCur
					.getLong(mCur
							.getColumnIndex(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_FROM_USER_ID));
			long date = mCur
					.getLong(mCur
							.getColumnIndex(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_SAVEDATE));
			String imageID = mCur
					.getString(mCur
							.getColumnIndex(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_ID));
			String imagePath = mCur
					.getString(mCur
							.getColumnIndex(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_PATH));
			User fromUser = GlobalHolder.getInstance().getUser(fromUserID);
			if (fromUser == null) {
				V2Log.e("get null when loadImageMessage get fromUser :"
						+ fromUserID);
				continue;
			}
			current = new VMessage(groupType, groupID, fromUser, new Date(date));
			current.setUUID(imageID);
			item = new VMessageImageItem(current);
			item.setUuid(imageID);
			item.setFilePath(imagePath);
			imageItems.add(current);
		}

		if (mCur != null)
			mCur.close();
		return imageItems;
	}

	/**
	 * 根据用户id，加载音频或视频通信记录
	 * @param context
	 * @param uid
	 * @param meidaType
	 * @return
	 */
	public static List<AudioVideoMessageBean> loadAudioOrVideoHistoriesMessage(
			Context context, long uid, int meidaType) {

		HashMap<Long, AudioVideoMessageBean> tempList = null;
		List<AudioVideoMessageBean> targetList = null;
		try {
			String selection = "";
			switch (meidaType) {
			case AudioVideoMessageBean.TYPE_AUDIO:
				selection = (ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_FROM_USER_ID
						+ "= ? and "
						+ ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_TYPE + "= 0")
						+ "or"
						+ (ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_TO_USER_ID
								+ "= ? and "
								+ ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_TYPE + "= 0");
				break;
			case AudioVideoMessageBean.TYPE_VIDEO:
				selection = (ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_FROM_USER_ID
						+ "= ? and "
						+ ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_TYPE + "= 1")
						+ "or"
						+ (ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_TO_USER_ID
								+ "= ? and "
								+ ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_TYPE + "= 1");
				break;
			default:
				selection = ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_FROM_USER_ID
						+ "= ? or "
						+ ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_TO_USER_ID
						+ "= ?";
				break;
			}

			String[] args = new String[] { String.valueOf(uid),
					String.valueOf(uid) };
			String sortOrder = ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_START_DATE
					+ " desc";
			Cursor mCur = context.getContentResolver().query(
					ContentDescriptor.HistoriesMedia.CONTENT_URI,
					ContentDescriptor.HistoriesMedia.Cols.ALL_CLOS, selection,
					args, sortOrder);
			if (mCur != null) {
				long currentID = GlobalHolder.getInstance().getCurrentUserId();
				tempList = new HashMap<Long, AudioVideoMessageBean>();
				targetList = new ArrayList<AudioVideoMessageBean>();
				AudioVideoMessageBean currentMedia = null;
				AudioVideoMessageBean.ChildMessageBean currentChildMedia = null;
				int isCallOut = -1; // 主动呼叫还是被动 0 主动 1 被动
				while (mCur.moveToNext()) {
					currentChildMedia = new AudioVideoMessageBean.ChildMessageBean();
					int types = mCur
							.getInt(mCur
									.getColumnIndex(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_TYPE));
					int readState = mCur
							.getInt(mCur
									.getColumnIndex(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_READ_STATE));
					long startDate = mCur
							.getLong(mCur
									.getColumnIndex(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_START_DATE));
					long endDate = mCur
							.getLong(mCur
									.getColumnIndex(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_END_DATE));
					long formID = mCur
							.getLong(mCur
									.getColumnIndex(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_FROM_USER_ID));
					long toID = mCur
							.getLong(mCur
									.getColumnIndex(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_TO_USER_ID));
					long remoteID = mCur
							.getLong(mCur
									.getColumnIndex(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_REMOTE_USER_ID));
					int mediaState = mCur
							.getInt(mCur
									.getColumnIndex(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_STATE));
					if (currentID == formID)
						isCallOut = AudioVideoMessageBean.STATE_CALL_OUT;
					else
						isCallOut = AudioVideoMessageBean.STATE_CALL_IN;

					User remoteUser = GlobalHolder.getInstance().getUser(
							remoteID);
					if (remoteUser == null) {
						V2Log.e("get null when get remote user :" + remoteID);
						continue;
					}

					currentMedia = tempList.get(remoteID);
					if (currentMedia == null) {
						AudioVideoMessageBean tempMedia = new AudioVideoMessageBean();
						tempMedia.name = remoteUser.getName();
						tempMedia.isCallOut = isCallOut;
						tempMedia.fromUserID = formID;
						tempMedia.toUserID = toID;
						tempMedia.remoteUserID = remoteID;
						tempMedia.readState = readState;
						tempList.put(remoteID, tempMedia);
						currentMedia = tempMedia;
					}

					currentChildMedia.childMediaType = types;
					currentChildMedia.childISCallOut = isCallOut;
					currentChildMedia.childHoldingTime = endDate - startDate;
					currentChildMedia.childSaveDate = startDate;
					currentChildMedia.childReadState = readState;
					currentChildMedia.childMediaState = mediaState;
					currentMedia.mChildBeans.add(currentChildMedia);
					// 判断该条消息是否未读，更改未读消息数量
					if (readState == AudioVideoMessageBean.STATE_UNREAD) {
						currentMedia.callNumbers += 1;
					}
				}
			}
			mCur.close();
			Set<Entry<Long, AudioVideoMessageBean>> entrySet = tempList
					.entrySet();
			Iterator<Entry<Long, AudioVideoMessageBean>> iterator = entrySet
					.iterator();
			while (iterator.hasNext()) {
				Entry<Long, AudioVideoMessageBean> next = iterator.next();
				AudioVideoMessageBean value = next.getValue();
				// 获取最新的通话数据
				String selections = ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_REMOTE_USER_ID
						+ "= ? ";
				String[] selectionArgs = new String[] { String
						.valueOf(value.remoteUserID) };
				VideoBean newestMediaMessage = getNewestMediaMessage(context,
						selections, selectionArgs);
				// ChildMessageBean childMessageBean =
				// newestMediaMessage.mChildBeans.get(value.mChildBeans.size() -
				// 1);
				// 更新
				// value.holdingTime = childMessageBean.childHoldingTime;
				// value.mediaType = childMessageBean.childMediaType;
				// value.readState = childMessageBean.childReadState;
				value.holdingTime = newestMediaMessage.endDate
						- newestMediaMessage.startDate;
				value.mediaType = newestMediaMessage.mediaType;
				value.meidaState = newestMediaMessage.mediaState;
				value.readState = newestMediaMessage.readSatate;
				targetList.add(value);
			}
		} catch (Exception e) {
			V2Log.e("loading AudioOrVideoHistories Messages happen a mistake....");
			e.getStackTrace();
		}
		return targetList;
	}

	// ****************************群组操作**************************************************

	public static List<VMessage> loadGroupMessage(Context context,
			long groupType, long gid) {
		if (!init(context, groupType, gid, 0, CROWD_TYPE))
			return null;
		String selection = ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_GROUP_ID
				+ "=? ";
		String order = ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_SAVEDATE
				+ " desc ";
		String[] args = new String[] { gid + "" };
		return queryMessage(context, selection, args, order);
	}

	public static List<VMessage> loadGroupImageMessage(Context context,
			int type, long gid) {
		List<VMessage> imageItems = new ArrayList<VMessage>();
		String sortOrder = ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_SAVEDATE
				+ " desc";
		String where = ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_GROUP_TYPE
				+ "=? and "
				+ ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_GROUP_ID
				+ "= ?";
		String[] args = new String[] { String.valueOf(type),
				String.valueOf(gid) };
		Uri uri = ContentDescriptor.HistoriesGraphic.CONTENT_URI;
		String[] projection = ContentDescriptor.HistoriesGraphic.Cols.ALL_CLOS;
		DataBaseContext mContext = new DataBaseContext(context);
		Cursor mCur = mContext.getContentResolver().query(uri, projection,
				where, args, sortOrder);
		if (mCur.getCount() == 0)
			return imageItems;

		VMessageImageItem item = null;
		VMessage current = null;
		while (mCur.moveToNext()) {
			int groupType = mCur
					.getInt(mCur
							.getColumnIndex(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_GROUP_TYPE));
			long groupID = mCur
					.getLong(mCur
							.getColumnIndex(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_GROUP_ID));
			long fromUserID = mCur
					.getLong(mCur
							.getColumnIndex(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_FROM_USER_ID));
			long date = mCur
					.getLong(mCur
							.getColumnIndex(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_SAVEDATE));
			String imageID = mCur
					.getString(mCur
							.getColumnIndex(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_ID));
			String imagePath = mCur
					.getString(mCur
							.getColumnIndex(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_PATH));
			User fromUser = GlobalHolder.getInstance().getUser(fromUserID);
			if (fromUser == null) {
				V2Log.e("get null when loadImageMessage get fromUser :"
						+ fromUserID);
				continue;
			}
			current = new VMessage(groupType, groupID, fromUser, new Date(date));
			item = new VMessageImageItem(current);
			item.setUuid(imageID);
			item.setFilePath(imagePath);
			imageItems.add(current);
		}

		if (mCur != null)
			mCur.close();
		return imageItems;
	}

	/**
	 * 
	 * @param context
	 * @param groupType
	 * @param groupId
	 * @param limit
	 * @param offset
	 * @return
	 */
	public static List<VMessage> loadGroupMessageByPage(Context context,
			long groupType, long groupId, int limit, int offset) {

		if (!init(context, groupType, groupId, 0, CROWD_TYPE))
			return null;

		String selection = ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_GROUP_TYPE
				+ "=? and "
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_GROUP_ID
				+ "= ?";
		String[] args = new String[] { String.valueOf(groupType),
				String.valueOf(groupId) };
		String order = ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_SAVEDATE
				+ " desc limit " + limit + " offset  " + offset;
		return queryMessage(context, selection, args, order);
	}

	public static Long queryVMessageID(Context context, String selection,
			String[] args, String sortOrder, long remoteID) {

		if (!init(context, 0, 0, remoteID, CONTACT_TYPE))
			return null;

		Cursor cursor = context.getContentResolver().query(
				ContentDescriptor.HistoriesMessage.CONTENT_URI,
				new String[] { "_id" }, selection, args, sortOrder);
		if (cursor != null && cursor.getCount() > 0 && cursor.moveToNext()) {

			return cursor.getLong(cursor.getColumnIndex("_id"));
		}
		return null;
	}

	public static List<VMessage> queryMessage(Context context,
			String selection, String[] args, String sortOrder) {
		DataBaseContext mContext = new DataBaseContext(context);
		Cursor mCur = mContext.getContentResolver().query(
				ContentDescriptor.HistoriesMessage.CONTENT_URI,
				ContentDescriptor.HistoriesMessage.Cols.ALL_CLOS, selection,
				args, sortOrder);

		List<VMessage> vimList = new ArrayList<VMessage>();
		if (mCur.getCount() == 0) {
			mCur.close();
			return vimList;
		}

		while (mCur.moveToNext()) {

			VMessage vm = XmlParser.parseForMessage(extractMsg(mCur));
			if (vm == null) {
				V2Log.e("the parse VMessage get null........");
				return vimList;
			}

			loadImageMessageById(vm, mContext);
			loadAudioMessageById(vm, mContext);
			loadFileMessageById(vm, mContext);
			vimList.add(vm);
		}
		mCur.close();
		return vimList;

	}

	/**
	 * delete the VMessage 
	 * @param context
	 * @param vm
	 * @return
	 */
	public static int deleteMessage(Context context, VMessage vm) {
		if (vm == null)
			return -1;

		DataBaseContext mContext = new DataBaseContext(context);
		int ret = mContext.getContentResolver().delete(
				ContentDescriptor.HistoriesMessage.CONTENT_URI,
				ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_ID
						+ "=?", new String[] { String.valueOf(vm.getUUID()) });

		List<VMessageAudioItem> audioItems = vm.getAudioItems();
		for (int i = 0; i < audioItems.size(); i++) {
			mContext.getContentResolver().delete(
					ContentDescriptor.HistoriesAudios.CONTENT_URI,
					ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_ID
							+ "=?",
					new String[] { String.valueOf(vm.getUUID()) });
		}

		List<VMessageFileItem> fileItems = vm.getFileItems();
		for (int i = 0; i < fileItems.size(); i++) {
			mContext.getContentResolver().delete(
					ContentDescriptor.HistoriesFiles.CONTENT_URI,
					ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_ID
							+ "=?",
					new String[] { String.valueOf(vm.getUUID()) });
		}

		List<VMessageImageItem> imageItems = vm.getImageItems();
		for (int i = 0; i < imageItems.size(); i++) {
			mContext.getContentResolver().delete(
					ContentDescriptor.HistoriesGraphic.CONTENT_URI,
					ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_ID
							+ "=?",
					new String[] { String.valueOf(vm.getUUID()) });
		}
		return ret;
	}

	/**
	 * update the given audio message read state...
	 * @param context
	 * @param vm
	 * @return
	 */
	public static int updateChatMessageState(Context context, VMessage vm) {

		if (vm == null)
			return -1;

		DataBaseContext mContext = new DataBaseContext(context);
		ContentValues values = new ContentValues();
		values.put(
				ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_TRANSTATE,
				vm.getState());
		String where = ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_ID
				+ "= ?";
		String[] selectionArgs = new String[] { vm.getUUID() };
		int ret = mContext.getContentResolver().update(
				ContentDescriptor.HistoriesMessage.CONTENT_URI, values, where,
				selectionArgs);
		return ret;
	}

	/**
	 * update the given audio message read state...
	 * @param context
	 * @param vm
	 * @param audioItem
	 * @return
	 */
	public static int updateBinaryAudioState(Context context, VMessage vm,
			VMessageAudioItem audioItem) {

		if (audioItem == null)
			return -1;

		DataBaseContext mContext = new DataBaseContext(context);
		ContentValues values = new ContentValues();
		values.put(
				ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_READ_STATE,
				audioItem.getState());
		String where = ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_ID
				+ "= ?";
		String[] selectionArgs = new String[] { audioItem.getUuid() };
		int ret = mContext.getContentResolver().update(
				ContentDescriptor.HistoriesAudios.CONTENT_URI, values, where,
				selectionArgs);
		return ret;
	}

	/**
	 * update the given audio message read state...
	 * @param context
	 * @param vm
	 * @param fileItem
	 * @return
	 */
	public static int updateFileItemState(Context context, VMessage vm,
			VMessageFileItem fileItem) {

		if (fileItem == null)
			return -1;

		DataBaseContext mContext = new DataBaseContext(context);
		ContentValues values = new ContentValues();
		values.put(
				ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SEND_STATE,
				fileItem.getState());
		String where = ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_ID
				+ "= ?";
		String[] selectionArgs = new String[] { vm.getUUID() };
		int ret = mContext.getContentResolver().update(
				ContentDescriptor.HistoriesFiles.CONTENT_URI, values, where,
				selectionArgs);
		return ret;
	}

	// private static void loadVMessageItem(Context context, VMessage vm,
	// int msgType) {
	// String selection = ContentDescriptor.MessageItems.Cols.MSG_ID + "=? ";
	// String[] args = null;
	// if (msgType != 0) {
	// selection += "and " + ContentDescriptor.MessageItems.Cols.TYPE
	// + "=? ";
	// args = new String[] { vm.getId() + "", msgType + "" };
	// } else {
	// args = new String[] { vm.getId() + "" };
	// }
	//
	// Cursor cur = context.getContentResolver().query(
	// ContentDescriptor.MessageItems.CONTENT_URI,
	// ContentDescriptor.MessageItems.Cols.ALL_CLOS, selection, args,
	// ContentDescriptor.MessageItems.Cols.ID);
	//
	// while (cur.moveToNext()) {
	// int id = cur.getInt(0);
	// // content
	// String content = cur.getString(2);
	// // Item type
	// int type = cur.getInt(3);
	// // new line flag
	// int newLineFlag = cur.getInt(4);
	//
	// String uuid = cur.getString(5);
	//
	// int state = cur.getInt(6);
	//
	// VMessageAbstractItem vai = null;
	// switch (type) {
	// case VMessageAbstractItem.ITEM_TYPE_TEXT:
	// vai = new VMessageTextItem(vm, content);
	// break;
	// case VMessageAbstractItem.ITEM_TYPE_FACE:
	// vai = new VMessageFaceItem(vm, Integer.parseInt(content));
	// break;
	// case VMessageAbstractItem.ITEM_TYPE_IMAGE:
	// vai = new VMessageImageItem(vm, content);
	// case VMessageAbstractItem.ITEM_TYPE_AUDIO:
	// if (content != null && !content.isEmpty()) {
	// String[] str = content.split("\\|");
	// if (str.length > 1) {
	// vai = new VMessageAudioItem(vm, str[0],
	// Integer.parseInt(str[1]));
	// }
	// }
	// break;
	// case VMessageAbstractItem.ITEM_TYPE_FILE: {
	// String fileName = null;
	// String filePath = null;
	// long fileSize = 0;
	// if (content != null && !content.isEmpty()) {
	// String[] str = content.split("\\|");
	// if (str.length > 2) {
	// fileName = str[0];
	// filePath = str[1];
	// fileSize = Long.parseLong(str[2]);
	// }
	// }
	//
	// vai = new VMessageFileItem(vm, filePath);
	// ((VMessageFileItem) vai).setFileSize(fileSize);
	// ((VMessageFileItem) vai).setFileName(fileName);
	//
	// }
	// break;
	// case VMessageAbstractItem.ITEM_TYPE_LINK_TEXT:
	// String[] str = content.split("\\|");
	// vai = new VMessageLinkTextItem(vm, str[0], str[1]);
	// break;
	//
	// }
	// if (vai != null
	// && newLineFlag == VMessageAbstractItem.NEW_LINE_FLAG_VALUE) {
	// vai.setNewLine(true);
	// }
	//
	// vai.setId(id);
	// vai.setUuid(uuid);
	// vai.setState(state);
	// }
	//
	// cur.close();
	// }

	public static VideoBean getNewestMediaMessage(Context context) {

		return getNewestMediaMessage(context, null, null);
	}

	/**
	 * 根据传入的查询条件，获取最新的通信消息对象(音频或视频通信)
	 * @param context
	 * @param selection
	 * @param selectionArgs
	 * @return
	 */
	public static VideoBean getNewestMediaMessage(Context context,
			String selection, String[] selectionArgs) {

		String order = ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_SAVEDATE
				+ " desc, "
				+ ContentDescriptor.HistoriesMedia.Cols.ID
				+ " desc limit 1 offset 0 ";

		DataBaseContext mContext = new DataBaseContext(context);
		Cursor mCur = mContext.getContentResolver().query(
				ContentDescriptor.HistoriesMedia.CONTENT_URI,
				ContentDescriptor.HistoriesMedia.Cols.ALL_CLOS, selection,
				selectionArgs, order);

		if (mCur == null || mCur.getCount() <= 0) {
			mCur.close();
			return null;
		}

		mCur.moveToNext();
		VideoBean bean = new VideoBean();
		bean.ownerID = mCur.getLong(mCur.getColumnIndex("OwnerUserID"));
		bean.formUserID = mCur.getLong(mCur.getColumnIndex("FromUserID"));
		bean.toUserID = mCur.getLong(mCur.getColumnIndex("ToUserID"));
		bean.startDate = mCur.getLong(mCur.getColumnIndex("StartDate"));
		bean.endDate = mCur.getLong(mCur.getColumnIndex("EndDate"));
		bean.readSatate = mCur.getInt(mCur.getColumnIndex("ReadState"));
		bean.mediaState = mCur.getInt(mCur.getColumnIndex("MediaState"));
		bean.mediaType = mCur.getInt(mCur.getColumnIndex("MediaType"));
		bean.mediaChatID = mCur.getString(mCur.getColumnIndex("MediaChatID"));
		bean.remoteUserID = mCur.getLong(mCur.getColumnIndex("RemoteUserID"));
		mCur.close();
		return bean;
	}

	/**
	 * 根据传入的group的type和id，查询数据库，获取最新的VMessage对象，群组
	 * @param context
	 * @param groupId
	 * @return
	 */
	public static VMessage getNewestGroupMessage(Context context,
			long groupType, long groupId) {

		if (!init(context, groupType, groupId, 0, CROWD_TYPE))
			return null;
		String selection = ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_GROUP_ID
				+ "=? ";
		String[] args = new String[] { groupId + "" };
		String order = ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_SAVEDATE
				+ " desc limit 1 offset 0 ";
		List<VMessage> list = queryMessage(context, selection, args, order);
		if (list != null && list.size() > 0) {
			return list.get(0);
		} else {
			return null;
		}
	}

	/**
	 * 根据传入的id，查询数据库，获取最新的VMessage对象
	 * @param cur
	 * @return
	 */
	public static VMessage getNewestMessage(Context context, long uid1,
			long uid2) {
		if (!init(context, 0, 0, uid2, CONTACT_TYPE))
			return null;
		// String selection = "((" +
		// ContentDescriptor.Messages.Cols.FROM_USER_ID
		// + "=? and " + ContentDescriptor.Messages.Cols.TO_USER_ID
		// + "=? ) or " + "("
		// + ContentDescriptor.Messages.Cols.FROM_USER_ID + "=? and "
		// + ContentDescriptor.Messages.Cols.TO_USER_ID + "=? ))  and "
		// + ContentDescriptor.Messages.Cols.GROUP_ID + "= 0 ";
		String selection = "(("
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_FROM_USER_ID
				+ "=? and "
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_TO_USER_ID
				+ "=? ) or "
				+ "("
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_FROM_USER_ID
				+ "=? and "
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_TO_USER_ID
				+ "=? ))  and "
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_GROUP_TYPE
				+ "= 0 ";

		String[] args = new String[] { uid1 + "", uid2 + "", uid2 + "",
				uid1 + "" };

		String order = ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_SAVEDATE
				+ " desc, "
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_ID
				+ " desc limit 1 offset 0 ";
		// String order = ContentDescriptor.Messages.Cols.SEND_TIME + " desc, "
		// + ContentDescriptor.Messages.Cols.ID
		// + " desc limit 1 offset 0 ";

		List<VMessage> list = queryMessage(context, selection, args, order);
		if (list != null && list.size() > 0) {
			return list.get(0);
		} else {
			return null;
		}
	}

	/**
	 * 根据传入的Cursor对象，构造一个VMessage对象
	 * @param cur
	 * @return
	 */
	private static VMessage extractMsg(Cursor cur) {
		if (cur.isClosed()) {
			throw new RuntimeException(" cursor is closed");
		}
		long user1Id = cur
				.getLong(cur
						.getColumnIndex(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_FROM_USER_ID));
		long user2Id = cur
				.getLong(cur
						.getColumnIndex(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_TO_USER_ID));
		User fromUser = GlobalHolder.getInstance().getUser(user1Id);
		if (fromUser == null) {
			fromUser = new User(user1Id);
		}
		User toUser = GlobalHolder.getInstance().getUser(user2Id);
		if (toUser == null) {
			toUser = new User(user2Id);
		}

		int id = cur.getInt(0);
		// message type
		int type = cur
				.getInt(cur
						.getColumnIndex(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_GROUP_TYPE));
		// date time
		long dateString = cur
				.getLong(cur
						.getColumnIndex(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_SAVEDATE));
		// group id
		long groupId = cur
				.getLong(cur
						.getColumnIndex(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_GROUP_ID));
		// message state
		int state = cur
				.getInt(cur
						.getColumnIndex(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_TRANSTATE));
		// message id
		String uuid = cur
				.getString(cur
						.getColumnIndex(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_ID));
		String xml = cur
				.getString(cur
						.getColumnIndex(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_CONTENT));
		VMessage vm = new VMessage(type, groupId, fromUser, toUser, uuid,
				new Date(dateString));
		vm.setId(id);
		vm.setState(state);
		vm.setmXmlDatas(xml);
		return vm;
	}

	/**
	 * 根据传入的表名，判断当前数据库中是否存在该表
	 * @param context
	 * @param tabName 表名
	 * @return
	 */
	private static boolean isExistTable(Context context, String tabName) {
		DataBaseContext mContext = new DataBaseContext(context);
		SQLiteDatabase base = mContext.openOrCreateDatabase(
				V2TechDBHelper.DB_NAME, 0, null);
		try {
			String sql = "select count(*) as c from sqlite_master where type ='table' "
					+ "and name ='" + tabName.trim() + "' ";
			Cursor cursor = base.rawQuery(sql, null);
			if (cursor != null && cursor.getCount() > 0 && cursor.moveToNext()) {
				int count = cursor.getInt(0);
				if (count > 0) {
					return true;
				}
			}
		} catch (Exception e) {
			V2Log.e("detection table " + tabName + " is failed..."); // 检测失败
			e.getStackTrace();
		} finally {
			if (base != null) {
				base.close();
			}
		}
		return false;
	}

	/**
	 * 根据传入的VMessage对象，查询数据库，向其填充VMessageImageItem对象。
	 * @param vm
	 * @param context
	 * @return
	 */
	private static VMessage loadImageMessageById(VMessage vm, Context context) {

		List<VMessageImageItem> imageItems = vm.getImageItems();
		if (imageItems.size() <= 0)
			return vm;

		String selection = ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_ID
				+ "=? ";
		String sortOrder = ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_SAVEDATE
				+ " desc limit 1 offset 0 ";
		Uri uri = ContentDescriptor.HistoriesGraphic.CONTENT_URI;
		String[] projection = ContentDescriptor.HistoriesGraphic.Cols.ALL_CLOS;
		DataBaseContext mContext = new DataBaseContext(context);
		Cursor mCur = null;
		for (VMessageImageItem item : imageItems) {
			String[] selectionArgs = new String[] { item.getUuid() };
			mCur = mContext.getContentResolver().query(uri, projection,
					selection, selectionArgs, sortOrder);
			if (mCur.getCount() == 0) {
				V2Log.e("the loading VMessageImageItem --" + item.getUuid()
						+ "-- get null........");
				return vm;
			}

			while (mCur.moveToNext()) {
				int transState = mCur
						.getInt(mCur
								.getColumnIndex(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_TRANSTATE));
				String filePath = mCur
						.getString(mCur
								.getColumnIndex(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_PATH));
				item.setState(transState);
				item.setFilePath(filePath);
			}
		}

		if (mCur != null)
			mCur.close();
		return vm;
	}

	/**
	 * 根据传入的VMessage对象，查询数据库，向其填充VMessageAudioItem对象。
	 * @param vm
	 * @param context
	 * @return
	 */
	private static VMessage loadAudioMessageById(VMessage vm, Context context) {

		List<VMessageAudioItem> audioItems = vm.getAudioItems();
		if (audioItems.size() <= 0)
			return vm;

		String selection = ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_ID
				+ "=? ";
		String sortOrder = ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_SAVEDATE
				+ " desc limit 1 offset 0 ";
		Uri uri = ContentDescriptor.HistoriesAudios.CONTENT_URI;
		String[] projection = ContentDescriptor.HistoriesAudios.Cols.ALL_CLOS;
		DataBaseContext mContext = new DataBaseContext(context);
		Cursor mCur = null;
		for (VMessageAudioItem item : audioItems) {
			String[] selectionArgs = new String[] { item.getUuid() };
			mCur = mContext.getContentResolver().query(uri, projection,
					selection, selectionArgs, sortOrder);
			if (mCur.getCount() == 0) {
				V2Log.e("the loading VMessageAudioItem --" + item.getUuid()
						+ "-- get null........");
				return vm;
			}

			while (mCur.moveToNext()) {
				int readState = mCur
						.getInt(mCur
								.getColumnIndex(ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_READ_STATE));
				String filePath = mCur
						.getString(mCur
								.getColumnIndex(ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_PATH));
				item.setState(readState);
				item.setAudioFilePath(filePath);
			}
		}

		if (mCur != null)
			mCur.close();
		return vm;
	}

	/**
	 * 根据传入的VMessage对象，查询数据库，向其填充VMessageFileItem对象。
	 * @param vm
	 * @param context
	 * @return
	 */
	private static VMessage loadFileMessageById(VMessage vm, Context context) {

		List<VMessageFileItem> fileItems = vm.getFileItems();
		if (fileItems.size() <= 0)
			return vm;

		String selection = ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_ID
				+ "=? ";
		String sortOrder = ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SAVEDATE
				+ " desc limit 1 offset 0 ";
		Uri uri = ContentDescriptor.HistoriesFiles.CONTENT_URI;
		String[] projection = ContentDescriptor.HistoriesFiles.Cols.ALL_CLOS;
		DataBaseContext mContext = new DataBaseContext(context);
		Cursor mCur = null;
		for (VMessageFileItem item : fileItems) {
			String[] selectionArgs = new String[] { item.getUuid() };
			mCur = mContext.getContentResolver().query(uri, projection,
					selection, selectionArgs, sortOrder);
			if (mCur.getCount() == 0) {
				V2Log.e("the loading VMessageFileItem --" + item.getUuid()
						+ "-- get null........");
				return vm;
			}

			while (mCur.moveToNext()) {
				int fileTransState = mCur
						.getInt(mCur
								.getColumnIndex(ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SEND_STATE));
				item.setState(fileTransState);
			}
		}

		if (mCur != null)
			mCur.close();
		return vm;
	}
}
