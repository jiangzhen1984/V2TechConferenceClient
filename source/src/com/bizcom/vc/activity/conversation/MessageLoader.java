package com.bizcom.vc.activity.conversation;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;

import com.V2.jni.util.V2Log;
import com.bizcom.db.ContentDescriptor;
import com.bizcom.db.DataBaseContext;
import com.bizcom.db.V2TechDBHelper;
import com.bizcom.db.ContentDescriptor.HistoriesMessage;
import com.bizcom.util.CrashHandler;
import com.bizcom.util.XmlParser;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.application.V2GlobalConstants;
import com.bizcom.vo.AudioVideoMessageBean;
import com.bizcom.vo.CrowdGroup;
import com.bizcom.vo.Group;
import com.bizcom.vo.User;
import com.bizcom.vo.VCrowdFile;
import com.bizcom.vo.VMessage;
import com.bizcom.vo.VMessageAbstractItem;
import com.bizcom.vo.VMessageAudioItem;
import com.bizcom.vo.VMessageFileItem;
import com.bizcom.vo.VMessageImageItem;
import com.bizcom.vo.VideoBean;
import com.bizcom.vo.VMessageFileItem.FileType;

public class MessageLoader {

	public static final int CONTACT_TYPE = -5;
	public static final int CROWD_TYPE = -6;
	public static final String TAG = "MessageLoader";
	public static DataBaseContext mContext;

	public static void init(Context context) {
		mContext = new DataBaseContext(context);
	}

	/**
	 * 查询前要判断该用户的数据库是否存在，不存在则创建
	 * 
	 * @param context
	 * @param groupType
	 * @param groupID
	 * @param remoteUserID
	 * @param type
	 * <br>
	 *            &nbsp;&nbsp;&nbsp;&nbsp; MessageLoader.CONTACT_TYPE or
	 *            MessageLoader.CROWD_TYPE
	 * @return
	 */
	public static boolean isTableExist(Context context, long groupType,
			long groupID, long remoteUserID, int type) {

		String name;
		switch (type) {
		case CROWD_TYPE:
			name = "Histories_" + groupType + "_" + groupID + "_0";
			break;
		case CONTACT_TYPE:
			name = "Histories_0_0_" + remoteUserID;
			break;
		default:
			throw new RuntimeException("create database fialed... type is : "
					+ type);
		}

		List<String> cacheNames = GlobalHolder.getInstance()
				.getDataBaseTableCacheName();
		boolean flag = true;
		if (!cacheNames.contains(name)) {
			// 创建表
			boolean isCreate = ContentDescriptor.execSQLCreate(context, name);
			if (isCreate) {
				GlobalHolder.getInstance().getDataBaseTableCacheName()
						.add(name);
				flag = true;
			} else {
				V2Log.d(TAG, "create database fialed... name is : " + name);
				flag = false;
			}
		}

		// init dataBase path
		if (flag) {
			ContentDescriptor.HistoriesMessage.PATH = name;
			ContentDescriptor.HistoriesMessage.NAME = name;
			ContentDescriptor.URI_MATCHER.addURI(ContentDescriptor.AUTHORITY,
					HistoriesMessage.PATH, HistoriesMessage.TOKEN);
			ContentDescriptor.URI_MATCHER.addURI(ContentDescriptor.AUTHORITY,
					HistoriesMessage.PATH + "/#",
					HistoriesMessage.TOKEN_WITH_ID);
			ContentDescriptor.URI_MATCHER.addURI(ContentDescriptor.AUTHORITY,
					HistoriesMessage.PATH + "/page",
					HistoriesMessage.TOKEN_BY_PAGE);
			ContentDescriptor.HistoriesMessage.CONTENT_URI = ContentDescriptor.BASE_URI
					.buildUpon()
					.appendPath(ContentDescriptor.HistoriesMessage.PATH)
					.build();
		}

		return flag;
	}

	/**
	 * 分页加载聊天消息数据
	 * 
	 * @param context
	 * @param groupType
	 * @param fromUserID
	 * @param remoteUserID
	 * @param limit
	 * @param offset
	 * @return
	 */
	public static List<VMessage> loadMessageByPage(Context context,
			int groupType, long fromUserID, long remoteUserID, int limit,
			int offset) {
		if (!isTableExist(context, 0, 0, remoteUserID, CONTACT_TYPE))
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

		String[] args = new String[] { String.valueOf(fromUserID),
				String.valueOf(remoteUserID), String.valueOf(remoteUserID),
				String.valueOf(fromUserID), String.valueOf(groupType) };

		String order = ContentDescriptor.HistoriesMessage.Cols.ID + " desc , "
				+ ContentDescriptor.HistoriesMessage.Cols.ID + " desc limit "
				+ limit + " offset  " + offset;
		return queryMessage(selection, args, order);
	}

	/**
	 * according given VMessage ID , get VMessage Object
	 * 
	 * @param context
	 * @param msgId
	 * @return
	 */
	public static VMessage loadMessageById(Context context, int groupType,
			long groupID, long remoteID, long msgId) {

		int type;
		if (groupType == 0)
			type = CONTACT_TYPE;
		else
			type = CROWD_TYPE;

		if (!isTableExist(context, groupType, groupID, remoteID, type))
			return null;

		String selection = ContentDescriptor.HistoriesMessage.Cols.ID + "=? ";
		String[] args = new String[] { msgId + "" };
		String order = ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_SAVEDATE
				+ " desc limit 1 offset 0 ";
		List<VMessage> list = queryMessage(selection, args, order);
		if (list != null && list.size() > 0) {
			return list.get(0);
		} else {
			return null;
		}
	}

	/**
	 * loading user to user chating messages
	 * 
	 * @param context
	 * @param remoteID
	 * @param msgId
	 * @return
	 */
	public static VMessage loadUserMessageById(Context context, long remoteID,
			long msgId) {

		return loadMessageById(context, 0, 0, remoteID, msgId);
	}

	/**
	 * loading crowd chating messages
	 * 
	 * @param context
	 * @param groupType
	 * @param groupID
	 * @param msgId
	 * @return
	 */
	public static VMessage loadGroupMessageById(Context context, int groupType,
			long groupID, long msgId) {

		return loadMessageById(context, groupType, groupID, 0, msgId);
	}

	/**
	 * 根据指定用户id，加载出所有图片
	 * 
	 * @param context
	 * @param uid1
	 * @param uid2
	 * @return
	 */
	public static List<VMessage> loadImageMessage(Context context, long uid1,
			long uid2) {

		DataBaseContext mContext = new DataBaseContext(context);
		Cursor cursor = null;
		List<VMessage> imageItems = new ArrayList<VMessage>();
		try {
			Uri uri = ContentDescriptor.HistoriesGraphic.CONTENT_URI;
			String sortOrder = ContentDescriptor.HistoriesGraphic.Cols.ID
					+ " desc";
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
			cursor = mContext.getContentResolver().query(uri, projection,
					selection, args, sortOrder);

			if (cursor == null) {
				return imageItems;
			}

			if (cursor.getCount() < 0) {
				return imageItems;
			}

			VMessageImageItem item;
			VMessage current;
			while (cursor.moveToNext()) {
				int groupType = cursor
						.getInt(cursor
								.getColumnIndex(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_GROUP_TYPE));
				long groupID = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_GROUP_ID));
				long fromUserID = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_FROM_USER_ID));
				long date = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_SAVEDATE));
				String imageID = cursor
						.getString(cursor
								.getColumnIndex(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_ID));
				String imagePath = cursor
						.getString(cursor
								.getColumnIndex(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_PATH));
				User fromUser = GlobalHolder.getInstance().getUser(fromUserID);
				if (fromUser == null) {
					V2Log.e("get null when loadImageMessage get fromUser :"
							+ fromUserID);
					continue;
				}
				current = new VMessage(groupType, groupID, fromUser, new Date(
						date));
				current.setUUID(imageID);
				item = new VMessageImageItem(current , imageID , imagePath , 0);
				imageItems.add(current);
			}
			return imageItems;
		} catch (Exception e) {
			e.printStackTrace();
			CrashHandler.getInstance().saveCrashInfo2File(e);
			return imageItems;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * 根据用户id，加载音频或视频通信记录
	 * 
	 * @param context
	 * @param uid
	 * @param meidaType
	 * @return
	 */
	public static List<AudioVideoMessageBean> loadAudioOrVideoHistoriesMessage(
			Context context, long uid, int meidaType) {

		DataBaseContext mContext = new DataBaseContext(context);
		Cursor cursor = null;
		HashMap<Long, AudioVideoMessageBean> tempList = new HashMap<Long, AudioVideoMessageBean>();
		List<AudioVideoMessageBean> targetList = new ArrayList<AudioVideoMessageBean>();
		try {
			String selection;
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
			String sortOrder = ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_SAVEDATE
					+ " desc";
			cursor = mContext.getContentResolver().query(
					ContentDescriptor.HistoriesMedia.CONTENT_URI,
					ContentDescriptor.HistoriesMedia.Cols.ALL_CLOS, selection,
					args, sortOrder);

			if (cursor == null)
				return targetList;

			if (cursor.getCount() <= 0) {
				return targetList;
			}

			long currentID = GlobalHolder.getInstance().getCurrentUserId();
			AudioVideoMessageBean currentMedia;
			AudioVideoMessageBean.ChildMessageBean currentChildMedia;
			int isCallOut; // 主动呼叫还是被动 0 主动 1 被动
			while (cursor.moveToNext()) {
				currentChildMedia = new AudioVideoMessageBean.ChildMessageBean();
				int types = cursor
						.getInt(cursor
								.getColumnIndex(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_TYPE));
				int readState = cursor
						.getInt(cursor
								.getColumnIndex(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_READ_STATE));
				long startDate = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_START_DATE));
				long endDate = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_END_DATE));
				long formID = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_FROM_USER_ID));
				long toID = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_TO_USER_ID));
				long remoteID = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_REMOTE_USER_ID));
				int mediaState = cursor
						.getInt(cursor
								.getColumnIndex(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_STATE));
				if (currentID == formID)
					isCallOut = AudioVideoMessageBean.STATE_CALL_OUT;
				else
					isCallOut = AudioVideoMessageBean.STATE_CALL_IN;

				User remoteUser = GlobalHolder.getInstance().getUser(remoteID);
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
			return targetList;
		} catch (Exception e) {
			e.printStackTrace();
			CrashHandler.getInstance().saveCrashInfo2File(e);
			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	// ****************************群组操作**************************************************

	/**
	 * 查询指定群组的所有聊天消息记录
	 * 
	 * @param context
	 * @param groupType
	 * @param gid
	 * @return
	 */
	public static List<VMessage> loadGroupMessage(Context context,
			long groupType, long gid) {
		if (!isTableExist(context, groupType, gid, 0, CROWD_TYPE))
			return null;
		String selection = ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_GROUP_ID
				+ "=? ";
		String order = ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_SAVEDATE
				+ " desc ";
		String[] args = new String[] { gid + "" };
		return queryMessage(selection, args, order);
	}

	/**
	 * 查询指定群组中聊天收发的所有图片
	 * 
	 * @param context
	 * @param type
	 * @param gid
	 * @return
	 */
	public static List<VMessage> loadGroupImageMessage(Context context,
			int type, long gid) {
		if (!isTableExist(context, type, gid, 0, CROWD_TYPE))
			return null;

		List<VMessage> imageItems = new ArrayList<VMessage>();
		DataBaseContext mContext = new DataBaseContext(context);
		Cursor cursor = null;
		try {

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
			cursor = mContext.getContentResolver().query(uri, projection,
					where, args, sortOrder);

			if (cursor == null) {
				return imageItems;
			}

			if (cursor.getCount() < 0) {
				return imageItems;
			}

			VMessageImageItem item;
			VMessage current;
			while (cursor.moveToNext()) {
				int groupType = cursor
						.getInt(cursor
								.getColumnIndex(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_GROUP_TYPE));
				long groupID = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_GROUP_ID));
				long fromUserID = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_FROM_USER_ID));
				long date = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_SAVEDATE));
				String imageID = cursor
						.getString(cursor
								.getColumnIndex(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_ID));
				String imagePath = cursor
						.getString(cursor
								.getColumnIndex(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_PATH));
				User fromUser = GlobalHolder.getInstance().getUser(fromUserID);
				if (fromUser == null) {
					V2Log.e("get null when loadImageMessage get fromUser :"
							+ fromUserID);
					continue;
				}
				current = new VMessage(groupType, groupID, fromUser, new Date(
						date));
				item = new VMessageImageItem(current , imageID , imagePath , 0);
				imageItems.add(current);
			}
			return imageItems;
		} catch (Exception e) {
			e.printStackTrace();
			CrashHandler.getInstance().saveCrashInfo2File(e);
			return imageItems;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * 查询指定群组中聊天收发的所有文件
	 * 
	 * @param type
	 * @param remoteID
	 * @return
	 */
	public static List<VMessageFileItem> loadFileMessages(int type,
			long remoteID) {
		// 传两个-1是在MainActivity中需要查出文件表中所有文件而传递的
		if (type != -1 && remoteID != -1) {
			if (type == V2GlobalConstants.GROUP_TYPE_CROWD) {
				if (!isTableExist(mContext, type, remoteID, 0, CROWD_TYPE))
					return null;
			} else if (type == V2GlobalConstants.GROUP_TYPE_USER) {
				if (!isTableExist(mContext, 0, 0, remoteID, CONTACT_TYPE))
					return null;
			}
		}

		List<VMessageFileItem> fileItems = new ArrayList<VMessageFileItem>();
		Uri uri = ContentDescriptor.HistoriesFiles.CONTENT_URI;
		String[] args = null;
		String where = null;
		String sortOrder = null;
		Cursor cursor = null;
		try {
			if (type == -1 && remoteID == -1) {
				sortOrder = ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SAVEDATE
						+ " desc";
				where = ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SEND_STATE
						+ " = ? or "
						+ ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SEND_STATE
						+ " = ? or "
						+ ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SEND_STATE
						+ " = ? or "
						+ ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SEND_STATE
						+ " = ? or "
						+ ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SEND_STATE
						+ " = ?";
				args = new String[] {
						String.valueOf(VMessageAbstractItem.STATE_FILE_DOWNLOADING),
						String.valueOf(VMessageAbstractItem.STATE_FILE_UNDOWNLOAD),
						String.valueOf(VMessageAbstractItem.STATE_FILE_SENDING),
						String.valueOf(VMessageAbstractItem.STATE_FILE_PAUSED_SENDING),
						String.valueOf(VMessageAbstractItem.STATE_FILE_PAUSED_DOWNLOADING) };
				cursor = mContext.getContentResolver().query(uri, null, where,
						args, sortOrder);
			} else {
				sortOrder = ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SAVEDATE
						+ " desc";
				where = ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_REMOTE_USER_ID
						+ " = ? and "
						+ ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_FROM_USER_ID
						+ " = ? ";
				args = new String[] {
						String.valueOf(remoteID),
						String.valueOf(GlobalHolder.getInstance()
								.getCurrentUserId()) };
				cursor = mContext.getContentResolver().query(uri, null, where,
						args, sortOrder);
			}
			if (cursor == null) {
				return fileItems;
			}

			if (cursor.getCount() < 0) {
				return fileItems;
			}

			while (cursor.moveToNext()) {
				VMessageFileItem fileItem = extractFileItem(cursor, type,
						remoteID);
				if (fileItem != null)
					fileItems.add(fileItem);
			}
			return fileItems;
		} catch (Exception e) {
			e.printStackTrace();
			CrashHandler.getInstance().saveCrashInfo2File(e);
			return fileItems;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * 查询指定群组中聊天中收发的文件，并转换为VCrowdFile对象集合
	 * 
	 * @param mContext
	 * @param type
	 * @param gid
	 * @return
	 */
	public static List<VMessageFileItem> loadGroupFileItemConvertToVCrowdFile(
			long gid, CrowdGroup crowd) {

		if (crowd == null) {
			V2Log.e(TAG,
					"loadGroupFileItemConvertToVCrowdFile --> Given CrowdGroup is null!");
			return null;
		}

		Cursor cursor = null;
		try {

			List<VMessageFileItem> fileItems = new ArrayList<VMessageFileItem>();
			Uri uri = ContentDescriptor.HistoriesFiles.CONTENT_URI;
			String where = ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_REMOTE_USER_ID
					+ " = ?";
			String[] args = new String[] { String.valueOf(gid) };
			String sortOrder = ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SAVEDATE
					+ " desc";
			cursor = mContext.getContentResolver().query(uri, null, where,
					args, sortOrder);

			if (cursor == null || cursor.getCount() < 0) {
				return null;
			}

			while (cursor.moveToNext()) {
				VMessageFileItem fileItem = extractFileItem(cursor,
						V2GlobalConstants.GROUP_TYPE_CROWD, gid);
				if (fileItem != null) {
					fileItems.add(fileItem);
				}
			}
			return fileItems;
		} catch (Exception e) {
			e.printStackTrace();
			CrashHandler.getInstance().saveCrashInfo2File(e);
			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * 分页查询指定群组的聊天消息
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

		if (!isTableExist(context, groupType, groupId, 0, CROWD_TYPE))
			return null;

		String selection = ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_GROUP_TYPE
				+ "=? and "
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_GROUP_ID
				+ "= ?";
		String[] args = new String[] { String.valueOf(groupType),
				String.valueOf(groupId) };
		String order = ContentDescriptor.HistoriesMessage.Cols.ID
				+ " desc limit " + limit + " offset  " + offset;
		return queryMessage(selection, args, order);
	}

	/**
	 * query VMessageFileItme Object by uuid and groupType..
	 * 
	 * @param groupType
	 * @param uuid
	 * @return
	 */
	public static VMessageFileItem queryFileItemByID(String uuid) {

		if (TextUtils.isEmpty(uuid))
			throw new RuntimeException(
					"MessageLoader queryFileItemByID ---> the given VMessageFileItem fileID is null");

		String selection = ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_ID
				+ "=?";
		Cursor cursor = null;
		try {
			String[] args = new String[] { uuid };
			cursor = mContext.getContentResolver().query(
					ContentDescriptor.HistoriesFiles.CONTENT_URI, null,
					selection, args, null);

			if (cursor == null)
				return null;

			if (cursor.getCount() <= 0)
				return null;

			while (cursor.moveToFirst()) {
				long fromUserID = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_FROM_USER_ID));
				long remoteUserID = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_REMOTE_USER_ID));
				long saveDate = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SAVEDATE));
				String filePath = cursor
						.getString(cursor
								.getColumnIndex(ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_PATH));
				int fileState = cursor
						.getInt(cursor
								.getColumnIndex(ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SEND_STATE));

				int groupType;
				VMessage vm;
				if (remoteUserID != -1) {
					Group tempGroup = GlobalHolder.getInstance().getGroupById(
							remoteUserID);
					if (tempGroup == null)
						groupType = V2GlobalConstants.GROUP_TYPE_USER;
					else
						groupType = V2GlobalConstants.GROUP_TYPE_CROWD;
				} else {
					V2Log.e(TAG,
							"queryFileItemByID -- > Get remoteID is -1 , uuid is : "
									+ uuid);
					return null;
				}

				if (groupType == V2GlobalConstants.GROUP_TYPE_USER)
					vm = new VMessage(groupType, -1, GlobalHolder.getInstance()
							.getUser(fromUserID), GlobalHolder.getInstance()
							.getUser(remoteUserID), new Date(saveDate));
				else
					vm = new VMessage(groupType, remoteUserID, GlobalHolder
							.getInstance().getUser(fromUserID), null, new Date(
							saveDate));
				VMessageFileItem fileItem = new VMessageFileItem(vm, filePath,
						fileState);
				fileItem.setUuid(uuid);
				return fileItem;
			}
			return null;
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}

	/**
	 * query the given VMessageAbstractItem Object is exist.
	 * 
	 * @param context
	 * @param vm
	 * @return ture is exist , false no exist
	 */
	public static boolean queryVMessageItemByID(Context context,
			VMessageAbstractItem vm) {
		if (vm == null)
			throw new RuntimeException(
					"MessageLoader queryVMessageItemByID ---> the VMessageAbstractItem Object is null");

		DataBaseContext mContext = new DataBaseContext(context);
		String selection = "";
		Uri uri;
		Cursor cursor = null;
		switch (vm.getType()) {
		case VMessageAbstractItem.ITEM_TYPE_FILE:
			selection = ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_ID
					+ "=?";
			uri = ContentDescriptor.HistoriesFiles.CONTENT_URI;
			break;
		default:
			throw new RuntimeException(
					"MessageLoader queryVMessageItemByID ---> invalid VMessageAbstractItem Type ... current type is : "
							+ vm.getType());
		}

		try {
			String[] args = new String[] { vm.getUuid() };
			cursor = mContext.getContentResolver().query(uri, null, selection,
					args, null);
			if (cursor != null && cursor.getCount() > 0)
				return true;
			else
				return false;
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}

	/**
	 * 根据传入条件，查询聊天消息记录
	 * 
	 * @param context
	 * @param selection
	 * @param args
	 * @param sortOrder
	 * @return
	 */
	public static List<VMessage> queryMessage(String selection, String[] args,
			String sortOrder) {
		List<VMessage> vimList = new ArrayList<VMessage>();
		Cursor cursor = null;
		try {
			synchronized (MessageLoader.class) {
				boolean existTable = isExistTable(ContentDescriptor.HistoriesMessage.NAME);
				if (!existTable)
					return vimList;
				cursor = mContext.getContentResolver().query(
						ContentDescriptor.HistoriesMessage.CONTENT_URI,
						ContentDescriptor.HistoriesMessage.Cols.ALL_CLOS,
						selection, args, sortOrder);
			}

			if (cursor == null) {
				return vimList;
			}

			if (cursor.getCount() < 0) {
				return vimList;
			}

			while (cursor.moveToNext()) {
				VMessage extract = extractMsg(cursor);
				if (extract == null) {
					V2Log.d("The extract VMessage from Cursor failed...get null , id is : "
							+ cursor.getInt(0));
					continue;
				}

				VMessage vm = XmlParser.parseForMessage(extract);
				if (vm == null) {
					V2Log.d("The parse VMessage from failed...get null , id is : "
							+ cursor.getInt(0));
					continue;
				}

				loadImageMessageById(vm, mContext);
				loadAudioMessageById(vm, mContext);
				loadFileMessageById(vm, mContext);
				// boolean flag = loadFileMessageById(vm, mContext);
				// if (flag && vm.getMsgCode() == V2GlobalEnum.GROUP_TYPE_CROWD)
				// {
				// VMessageFileItem fileItem = vm.getFileItems().get(0);
				// if (fileItem.getState() ==
				// VMessageAbstractItem.STATE_FILE_SENT) {
				// if (vm.getFromUser() != null
				// && vm.getFromUser().getmUserId() == GlobalHolder
				// .getInstance().getCurrentUserId()) {
				// continue;
				// }
				// }
				// }
				vimList.add(vm);
			}
			return vimList;
		} catch (Exception e) {
			e.printStackTrace();
			CrashHandler.getInstance().saveCrashInfo2File(e);
			return vimList;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * According to given VMessage Object , delete it and ohter message (file ,
	 * audio , image)
	 * 
	 * @param context
	 * @param vm
	 * @param isDeleteOhter
	 *            true mean delete other messages(file . audio . image)
	 * @return
	 */
	public static int deleteMessage(Context context, VMessage vm,
			boolean isDeleteOhter) {
		if (vm == null)
			return -1;

		long remoteID = 0;
		if (vm.getMsgCode() == V2GlobalConstants.GROUP_TYPE_USER) {

			if (vm.getFromUser() == null || vm.getToUser() == null) {
				V2Log.e(TAG,
						"delete user to user chat message failed.. beacuse fromUser or toUser is null");
				return -1;
			}

			if (vm.getFromUser().getmUserId() == GlobalHolder.getInstance()
					.getCurrentUserId())
				remoteID = vm.getToUser().getmUserId();
			else
				remoteID = vm.getFromUser().getmUserId();
		}

		if (!isTableExist(
				context,
				vm.getMsgCode(),
				vm.getGroupId(),
				remoteID,
				vm.getMsgCode() == V2GlobalConstants.GROUP_TYPE_USER ? MessageLoader.CONTACT_TYPE
						: MessageLoader.CROWD_TYPE))
			return -1;

		DataBaseContext mContext = new DataBaseContext(context);
		int ret = mContext.getContentResolver().delete(
				ContentDescriptor.HistoriesMessage.CONTENT_URI,
				ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_ID
						+ "=?", new String[] { String.valueOf(vm.getUUID()) });

		if (isDeleteOhter) {
			List<VMessageAudioItem> audioItems = vm.getAudioItems();
			for (int i = 0; i < audioItems.size(); i++) {
				mContext.getContentResolver().delete(
						ContentDescriptor.HistoriesAudios.CONTENT_URI,
						ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_ID
								+ "=?",
						new String[] { String.valueOf(audioItems.get(i)
								.getUuid()) });
			}

			List<VMessageFileItem> fileItems = vm.getFileItems();
			for (int i = 0; i < fileItems.size(); i++) {
				deleteFileItem(fileItems.get(i).getUuid());
			}

			List<VMessageImageItem> imageItems = vm.getImageItems();
			for (int i = 0; i < imageItems.size(); i++) {
				mContext.getContentResolver()
						.delete(ContentDescriptor.HistoriesGraphic.CONTENT_URI,
								ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_ID
										+ "=?",
								new String[] { String.valueOf(imageItems.get(i)
										.getUuid()) });
			}
		}
		return ret;
	}

	public static int deleteFileItem(String fileID) {
		return mContext.getContentResolver().delete(
				ContentDescriptor.HistoriesFiles.CONTENT_URI,
				ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_ID + "=?",
				new String[] { fileID });
	}

	/**
	 * delete all messages , according given args..
	 * 
	 * @param context
	 * @param groupType
	 * @param groupID
	 * @param userID
	 * @param isDeleteFile
	 *            For crowd group , Don't delete file record
	 * @return
	 */
	public static boolean deleteMessageByID(Context context, int groupType,
			long groupID, long userID, boolean isDeleteFile) {

		DataBaseContext mContext = new DataBaseContext(context);
		List<String> tableNames = GlobalHolder.getInstance()
				.getDataBaseTableCacheName();
		String sql = "";
		String tableName;
		if (groupType != V2GlobalConstants.GROUP_TYPE_USER)
			tableName = "Histories_" + groupType + "_" + groupID + "_0";
		else
			tableName = "Histories_0_0_" + userID;

		if (tableNames.contains(tableName)) {
			tableNames.remove(tableName);
			sql = "drop table " + tableName;
		} else {
			V2Log.e(TAG, "drop table failed...table no exists , name is : "
					+ tableName);
			return false;
		}

		V2TechDBHelper dbHelper = new V2TechDBHelper(mContext);
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		try {
			if (db != null && db.isOpen())
				db.execSQL(sql);
			else {
				V2Log.d(TAG,
						"May delete HistoriesMessage failed...DataBase state not normal...groupType : "
								+ groupType + "  groupID : " + groupID
								+ "  userID : " + userID);
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			V2Log.d(TAG,
					"May delete HistoriesMessage failed...have exception...groupType : "
							+ groupType + "  groupID : " + groupID
							+ "  userID : " + userID);
			return false;
		} finally {
			if (db != null)
				db.close();
		}

		// 删除其他信息
		String audioCondition = ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_GROUP_TYPE
				+ "= ? and "
				+ ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_GROUP_ID
				+ "= ? and "
				+ ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_REMOTE_USER_ID
				+ "= ?";
		String imageCondition = ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_GROUP_TYPE
				+ "= ? and "
				+ ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_GROUP_ID
				+ "= ? and "
				+ ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_REMOTE_USER_ID
				+ "= ?";
		String fileCondition = ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_REMOTE_USER_ID
				+ "= ?";
		String[] tables = new String[] {
				ContentDescriptor.HistoriesAudios.CONTENT_URI.toString(),
				ContentDescriptor.HistoriesGraphic.CONTENT_URI.toString() };
		String[] conditions = new String[] { audioCondition, imageCondition };
		String[] names = new String[] { "audioCondition", "imageCondition" };
		String[] args = new String[] { String.valueOf(groupType),
				String.valueOf(groupID), String.valueOf(userID) };

		for (int i = 0; i < conditions.length; i++) {
			int ret = mContext.getContentResolver().delete(
					Uri.parse(tables[i]), conditions[i], args);
			if (ret <= 0)
				V2Log.d(TAG, "May delete " + names[i]
						+ " failed...groupType : " + groupType + "  groupID : "
						+ groupID + "  userID : " + userID);
		}

		if (isDeleteFile) {
			// 删除文件
			String[] fileArgs;
			if (groupType == V2GlobalConstants.GROUP_TYPE_USER)
				fileArgs = new String[] { String.valueOf(userID) };
			else
				fileArgs = new String[] { String.valueOf(groupID) };

			int ret = mContext.getContentResolver().delete(
					ContentDescriptor.HistoriesFiles.CONTENT_URI,
					fileCondition, fileArgs);
			if (ret <= 0)
				V2Log.d(TAG, "May delete fileConditions failed...groupType : "
						+ groupType + "  groupID : " + groupID + "  userID : "
						+ userID);
		}
		return true;
	}

	/**
	 * according user id , delete all voice message..
	 * 
	 * @param groupID
	 */
	public static int deleteVoiceMessage(long userID) {

		int ret;
		if (userID == -1)
			ret = mContext.getContentResolver().delete(
					ContentDescriptor.HistoriesMedia.CONTENT_URI, null, null);
		else
			ret = mContext
					.getContentResolver()
					.delete(ContentDescriptor.HistoriesMedia.CONTENT_URI,
							ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_REMOTE_USER_ID
									+ "= ?",
							new String[] { String.valueOf(userID) });
		if (ret <= 0)
			V2Log.d(TAG, "May delete voice Message failed...groupID : "
					+ userID);
		return ret;
	}

	/**
	 * update the given audio message read state...
	 * 
	 * @param context
	 * @param vm
	 * @return
	 */
	public static int updateChatMessageState(Context context, VMessage vm) {

		if (vm == null) {
			V2Log.e(TAG,
					"updateChatMessageState --> get VMessage Object is null...please check it");
			return -1;
		}

		DataBaseContext mContext = new DataBaseContext(context);
		ContentValues values = new ContentValues();
		ContentResolver contentResolver = mContext.getContentResolver();
		values.put(
				ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_TRANSTATE,
				vm.getState());
		String where = ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_ID
				+ "= ?";
		String[] selectionArgs = new String[] { vm.getUUID() };
		int update = contentResolver.update(
				ContentDescriptor.HistoriesMessage.CONTENT_URI, values, where,
				selectionArgs);
		if (update <= 0) {
			V2Log.e(TAG,
					"updateChatMessageState --> update chat message failed...message id is :"
							+ "" + vm.getUUID() + " and table name is : "
							+ ContentDescriptor.HistoriesMessage.CONTENT_URI);
			return -1;
		}

		List<VMessageAbstractItem> items = vm.getItems();
		if (items == null || items.size() <= 0) {
			V2Log.e(TAG,
					"updateChatMessageState --> get VMessageAbstractItem collection failed...is null");
			return -1;
		}

		for (int j = 0; j < items.size(); j++) {
			VMessageAbstractItem item = items.get(j);
			values.clear();
			selectionArgs = new String[] { item.getUuid() };
			switch (item.getType()) {
			case VMessageAbstractItem.ITEM_TYPE_AUDIO:
				values.put(
						ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_SEND_STATE,
						item.getState());
				where = ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_ID
						+ "= ?";
				int audioUpdate = contentResolver.update(
						ContentDescriptor.HistoriesAudios.CONTENT_URI, values,
						where, selectionArgs);
				if (audioUpdate <= 0) {
					V2Log.e(TAG,
							"updateChatMessageState --> update audio chat message failed...message id is :"
									+ "" + vm.getUUID()
									+ " and audio message id is : "
									+ item.getUuid());
				}
				break;
			case VMessageAbstractItem.ITEM_TYPE_FILE:
				values.put(
						ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SEND_STATE,
						item.getState());
				where = ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_ID
						+ "= ?";
				int fileUpdate = contentResolver.update(
						ContentDescriptor.HistoriesFiles.CONTENT_URI, values,
						where, selectionArgs);
				if (fileUpdate <= 0) {
					V2Log.e(TAG,
							"updateChatMessageState --> update file chat message failed...message id is :"
									+ "" + vm.getUUID()
									+ " and file message id is : "
									+ item.getUuid());
				}
				break;
			case VMessageAbstractItem.ITEM_TYPE_IMAGE:
				values.put(
						ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_TRANSTATE,
						item.getState());
				where = ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_ID
						+ "= ?";
				int imageUpdate = contentResolver.update(
						ContentDescriptor.HistoriesGraphic.CONTENT_URI, values,
						where, selectionArgs);
				if (imageUpdate <= 0) {
					V2Log.e(TAG,
							"updateChatMessageState --> update image chat message failed...message id is :"
									+ "" + vm.getUUID()
									+ " and image message id is : "
									+ item.getUuid());
				}
				break;
			default:
				// throw new
				// RuntimeException("updateChatMessageState --> invalid VMessageAbstractItem type , type is : "
				// + item.getType());
			}
		}
		return 1;
	}

	/**
	 * update the given audio message read state...
	 * 
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
		return mContext.getContentResolver().update(
				ContentDescriptor.HistoriesAudios.CONTENT_URI, values, where,
				selectionArgs);
	}

	/**
	 * update VMessageFileItem Object state to failed by fileID..
	 * 
	 * @param fileID
	 *            If fileID is null , mean change all fileItem to failed...
	 * @return
	 */
	public static int updateFileItemStateToFailed(String fileID) {

		ContentValues values = new ContentValues();
		if (TextUtils.isEmpty(fileID))
			return -1;

		VMessageFileItem fileItem = MessageLoader.queryFileItemByID(fileID);
		if (fileItem == null) {
			V2Log.e(TAG,
					"updateFileItemStateToFailed --> get VMessageFileItem Object failed...fileID is "
							+ fileID);
			return -1;
		}

		if (fileItem.getState() == VMessageAbstractItem.STATE_FILE_DOWNLOADING)
			values.put(
					ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SEND_STATE,
					VMessageAbstractItem.STATE_FILE_DOWNLOADED_FALIED);
		else if (fileItem.getState() == VMessageAbstractItem.STATE_FILE_SENDING)
			values.put(
					ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SEND_STATE,
					VMessageAbstractItem.STATE_FILE_SENT_FALIED);
		else
			return -1;
		String where = ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_ID
				+ "= ?";
		String[] selectionArgs = new String[] { fileItem.getUuid() };
		return mContext.getContentResolver().update(
				ContentDescriptor.HistoriesFiles.CONTENT_URI, values, where,
				selectionArgs);
	}

	/**
	 * According to Given the VMessageFileItem Object , update Transing State!
	 * 
	 * @param context
	 * @param fileItem
	 * @return
	 */
	public static int updateFileItemState(Context context,
			VMessageFileItem fileItem) {

		if (fileItem == null)
			return -1;

		DataBaseContext mContext = new DataBaseContext(context);
		ContentValues values = new ContentValues();
		values.put(
				ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SEND_STATE,
				fileItem.getState());
		values.put(ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_ID,
				fileItem.getUuid());
		String where = ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_ID
				+ "= ?";
		String[] selectionArgs = new String[] { fileItem.getUuid() };
		return mContext.getContentResolver().update(
				ContentDescriptor.HistoriesFiles.CONTENT_URI, values, where,
				selectionArgs);
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

	/**
	 * 获取最新的音视频记录消息
	 * 
	 * @param context
	 * @return
	 */
	public static VideoBean getNewestMediaMessage(Context context) {

		return getNewestMediaMessage(context, null, null);
	}

	/**
	 * 根据传入的查询条件，获取最新的通信消息对象(音频或视频通信)
	 * 
	 * @param context
	 * @param selection
	 * @param selectionArgs
	 * @return
	 */
	public static VideoBean getNewestMediaMessage(Context context,
			String selection, String[] selectionArgs) {

		DataBaseContext mContext = new DataBaseContext(context);
		Cursor cursor = null;
		try {

			String order = ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_SAVEDATE
					+ " desc, "
					+ ContentDescriptor.HistoriesMedia.Cols.ID
					+ " desc limit 1 offset 0 ";

			cursor = mContext.getContentResolver().query(
					ContentDescriptor.HistoriesMedia.CONTENT_URI,
					ContentDescriptor.HistoriesMedia.Cols.ALL_CLOS, selection,
					selectionArgs, order);

			if (cursor == null) {
				return null;
			}

			if (cursor.getCount() < 0) {
				return null;
			}

			if (cursor.moveToFirst()) {
				VideoBean bean = new VideoBean();
				bean.ownerID = cursor.getLong(cursor
						.getColumnIndex("OwnerUserID"));
				bean.formUserID = cursor.getLong(cursor
						.getColumnIndex("FromUserID"));
				bean.toUserID = cursor.getLong(cursor
						.getColumnIndex("ToUserID"));
				bean.startDate = cursor.getLong(cursor
						.getColumnIndex("StartDate"));
				bean.endDate = cursor.getLong(cursor.getColumnIndex("EndDate"));
				bean.readSatate = cursor.getInt(cursor
						.getColumnIndex("ReadState"));
				bean.mediaState = cursor.getInt(cursor
						.getColumnIndex("MediaState"));
				bean.mediaType = cursor.getInt(cursor
						.getColumnIndex("MediaType"));
				bean.mediaChatID = cursor.getString(cursor
						.getColumnIndex("MediaChatID"));
				bean.remoteUserID = cursor.getLong(cursor
						.getColumnIndex("RemoteUserID"));
				return bean;
			}
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			CrashHandler.getInstance().saveCrashInfo2File(e);
			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * 根据传入的group的type和id，查询数据库，获取最新的VMessage对象，群组
	 * 
	 * @param context
	 * @param groupId
	 * @return
	 */
	public static VMessage getNewestGroupMessage(Context context,
			long groupType, long groupId) {

		if (!isTableExist(context, groupType, groupId, 0, CROWD_TYPE))
			return null;

		String selection = ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_GROUP_ID
				+ "=? ";
		String[] args = new String[] { groupId + "" };
		String order = ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_SAVEDATE
				+ " desc limit 1 offset 0 ";
		List<VMessage> list = queryMessage(selection, args, order);
		if (list != null && list.size() > 0) {
			return list.get(0);
		} else {
			return null;
		}
	}

	/**
	 * 根据传入的id，查询数据库，获取最新的VMessage对象
	 * 
	 * @param context
	 * @param uid1
	 * @param uid2
	 * @return
	 */
	public static VMessage getNewestMessage(Context context, long uid1,
			long uid2) {
		if (!isTableExist(context, 0, 0, uid2, CONTACT_TYPE))
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
				+ "= 0 ";

		String[] args = new String[] { uid1 + "", uid2 + "", uid2 + "",
				uid1 + "" };

		String order = ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_SAVEDATE
				+ " desc, "
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_ID
				+ " desc limit 1 offset 0 ";

		List<VMessage> list = queryMessage(selection, args, order);
		if (list != null && list.size() > 0) {
			return list.get(0);
		} else {
			return null;
		}
	}

	// public static VMessageFileItem getFileMessageById(Context context,
	// VMessageFileItem item) {
	//
	// String selection = ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_ID
	// + "= ?";
	// String[] selectionArgs = new String[] { item.getUuid() };
	// Cursor cursor = context.getContentResolver().query(
	// ContentDescriptor.HistoriesFiles.CONTENT_URI, null, selection,
	// selectionArgs, null);
	// if (cursor != null && cursor.moveToFirst()) {
	//
	// String filePath = cursor
	// .getString(cursor
	// .getColumnIndex(ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_PATH));
	// item.setFilePath(filePath);
	// }
	// return item;
	// }

	/**
	 * 根据传入的Cursor对象，构造一个VMessage对象
	 * 
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

	private static VMessageFileItem extractFileItem(Cursor cursor,
			int groupType, long groupID) {
		int fromUserID = cursor
				.getInt(cursor
						.getColumnIndex(ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_FROM_USER_ID));
		long date = cursor
				.getLong(cursor
						.getColumnIndex(ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SAVEDATE));
		int fileState = cursor
				.getInt(cursor
						.getColumnIndex(ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SEND_STATE));
		String uuid = cursor
				.getString(cursor
						.getColumnIndex(ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_ID));
		String filePath = cursor
				.getString(cursor
						.getColumnIndex(ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_PATH));
		long fileSize = cursor
				.getLong(cursor
						.getColumnIndex(ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SIZE));
		User fromUser = GlobalHolder.getInstance().getUser(fromUserID);
		if (fromUser == null) {
			V2Log.e("get null when loadImageMessage get fromUser :"
					+ fromUserID);
			return null;
		}

		VMessage current = null;
		if (groupType == V2GlobalConstants.GROUP_TYPE_CROWD) {
			current = new VMessage(groupType, groupID, fromUser, new Date(date));
		} else if (groupType == V2GlobalConstants.GROUP_TYPE_USER) {
			current = new VMessage(groupType, 0, fromUser, new Date(date));
		} else {
			current = new VMessage(-1, -1, fromUser, new Date(date));
		}
		return new VMessageFileItem(current, uuid, filePath, null, fileSize,
				fileState, 0f, 0l, 0f, FileType.UNKNOW, 2);
	}

	public static List<VCrowdFile> convertToVCrowdFile(
			List<VMessageFileItem> fileItems, CrowdGroup crowd) {
		List<VCrowdFile> crowdFiles = new ArrayList<VCrowdFile>();
		for (int i = 0; i < fileItems.size(); i++) {
			VMessageFileItem vMessageFileItem = fileItems.get(i);
			VCrowdFile file = convertToVCrowdFile(vMessageFileItem, crowd);
			crowdFiles.add(file);
		}
		return crowdFiles;
	}

	public static VCrowdFile convertToVCrowdFile(
			VMessageFileItem vMessageFileItem, CrowdGroup crowd) {
		VCrowdFile crowdFile = new VCrowdFile();
		crowdFile.setId(vMessageFileItem.getUuid());
		crowdFile.setPath(vMessageFileItem.getFilePath());
		crowdFile.setSize(vMessageFileItem.getFileSize());
		crowdFile.setName(vMessageFileItem.getFileName());
		crowdFile.setState(com.bizcom.vo.VFile.State.fromInt(vMessageFileItem
				.getState()));
		crowdFile.setProceedSize((long) vMessageFileItem.getProgress());
		crowdFile.setUploader(vMessageFileItem.getVm().getFromUser());
		crowdFile.setStartTime(vMessageFileItem.getVm().getDate());
		crowdFile.setCrowd(crowd);
		return crowdFile;
	}

	/**
	 * 根据传入的VMessage对象，查询数据库，向其填充VMessageImageItem对象。
	 * 
	 * @param vm
	 * @param context
	 * @return
	 */
	private static VMessage loadImageMessageById(VMessage vm, Context context) {

		List<VMessageImageItem> imageItems = vm.getImageItems();
		if (imageItems.size() <= 0)
			return vm;

		DataBaseContext mContext = new DataBaseContext(context);
		Cursor mCur = null;
		try {
			String selection = ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_ID
					+ "=? ";
			String sortOrder = ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_SAVEDATE
					+ " desc limit 1 offset 0 ";
			Uri uri = ContentDescriptor.HistoriesGraphic.CONTENT_URI;
			String[] projection = ContentDescriptor.HistoriesGraphic.Cols.ALL_CLOS;
			for (VMessageImageItem item : imageItems) {
				String[] selectionArgs = new String[] { item.getUuid() };
				mCur = mContext.getContentResolver().query(uri, projection,
						selection, selectionArgs, sortOrder);
				if (mCur == null || mCur.getCount() <= 0) {
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
		} catch (Exception e) {
			e.printStackTrace();
			CrashHandler.getInstance().saveCrashInfo2File(e);
		} finally {
			if (mCur != null)
				mCur.close();
		}
		return vm;
	}

	/**
	 * 根据传入的VMessage对象，查询数据库，向其填充VMessageAudioItem对象。
	 * 
	 * @param vm
	 * @param context
	 * @return
	 */
	private static VMessage loadAudioMessageById(VMessage vm, Context context) {

		List<VMessageAudioItem> audioItems = vm.getAudioItems();
		if (audioItems.size() <= 0)
			return vm;

		DataBaseContext mContext = new DataBaseContext(context);
		Cursor mCur = null;
		try {
			String selection = ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_ID
					+ "=? ";
			String sortOrder = ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_SAVEDATE
					+ " desc limit 1 offset 0 ";
			Uri uri = ContentDescriptor.HistoriesAudios.CONTENT_URI;
			String[] projection = ContentDescriptor.HistoriesAudios.Cols.ALL_CLOS;
			for (VMessageAudioItem item : audioItems) {
				String[] selectionArgs = new String[] { item.getUuid() };
				mCur = mContext.getContentResolver().query(uri, projection,
						selection, selectionArgs, sortOrder);
				if (mCur == null || mCur.getCount() <= 0) {
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
					int transState = mCur
							.getInt(mCur
									.getColumnIndex(ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_SEND_STATE));
					item.setReadState(readState);
					item.setState(transState);
					item.setAudioFilePath(filePath);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			CrashHandler.getInstance().saveCrashInfo2File(e);
		} finally {
			if (mCur != null)
				mCur.close();
		}
		return vm;
	}

	/**
	 * 根据传入的VMessage对象，查询数据库，向其填充VMessageFileItem对象。
	 * 
	 * @param vm
	 * @param context
	 * @return
	 */
	private static boolean loadFileMessageById(VMessage vm, Context context) {

		List<VMessageFileItem> fileItems = vm.getFileItems();
		if (fileItems.size() <= 0)
			return false;

		DataBaseContext mContext = new DataBaseContext(context);
		Cursor mCur = null;
		try {
			String selection = ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_ID
					+ "=? ";
			String sortOrder = ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SAVEDATE
					+ " desc limit 1 offset 0 ";
			Uri uri = ContentDescriptor.HistoriesFiles.CONTENT_URI;
			String[] projection = ContentDescriptor.HistoriesFiles.Cols.ALL_CLOS;
			for (VMessageFileItem item : fileItems) {
				String[] selectionArgs = new String[] { item.getUuid() };
				mCur = mContext.getContentResolver().query(uri, projection,
						selection, selectionArgs, sortOrder);
				if (mCur == null || mCur.getCount() <= 0) {
					V2Log.e("the loading VMessageFileItem --" + item.getUuid()
							+ "-- get null........");
					return false;
				}

				while (mCur.moveToNext()) {
					int fileTransState = mCur
							.getInt(mCur
									.getColumnIndex(ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SEND_STATE));
					int fileSize = mCur
							.getInt(mCur
									.getColumnIndex(ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SIZE));
					String filePath = mCur
							.getString(mCur
									.getColumnIndex(ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_PATH));
					item.setState(fileTransState);
					item.setFileSize(fileSize);
					//为了兼容群文件中重名文件更改
					item.setFilePath(filePath);
					String name = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.length());
					item.setFileName(name);
				}
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			CrashHandler.getInstance().saveCrashInfo2File(e);
			return false;
		} finally {
			if (mCur != null)
				mCur.close();
		}
	}

	/**
	 * 根据传入的表名，判断当前数据库中是否存在该表
	 * 
	 * @param context
	 * @param tabName
	 *            表名
	 * @return
	 */
	private static boolean isExistTable(String tabName) {
		SQLiteDatabase base;
		base = mContext.openOrCreateDatabase(V2TechDBHelper.DB_NAME, 0, null);
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
			return false;
		} catch (Exception e) {
			V2Log.e("detection table " + tabName + " is failed..."); // 检测失败
			e.getStackTrace();
			return false;
		} finally {
			if (base != null) {
				base.close();
			}
		}
	}

}
