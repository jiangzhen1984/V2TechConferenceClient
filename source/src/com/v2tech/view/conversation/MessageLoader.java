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

import com.V2.jni.V2GlobalEnum;
import com.V2.jni.util.V2Log;
import com.v2tech.db.ContentDescriptor;
import com.v2tech.db.ContentDescriptor.HistoriesMessage;
import com.v2tech.db.DataBaseContext;
import com.v2tech.db.V2TechDBHelper;
import com.v2tech.service.GlobalHolder;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.XmlParser;
import com.v2tech.vo.AudioVideoMessageBean;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.User;
import com.v2tech.vo.VCrowdFile;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.VMessageAbstractItem;
import com.v2tech.vo.VMessageAudioItem;
import com.v2tech.vo.VMessageFileItem;
import com.v2tech.vo.VMessageImageItem;
import com.v2tech.vo.VMessageQualification;
import com.v2tech.vo.VideoBean;

public class MessageLoader {

	public static final int CONTACT_TYPE = -5;
	public static final int CROWD_TYPE = -6;
	private static final String TAG = "MessageLoader";

	/**
	 * 查询前要判断该用户的数据库是否存在，不存在则创建
	 * 
	 * @param context
	 * @param uid2
	 * @return
	 */
	public static boolean isTableExist(Context context, long groupType, long groupID,
			long uid2, int type) {

		String name;
		switch (type) {
		case CROWD_TYPE:
			name = "Histories_" + groupType + "_" + groupID + "_0";
			break;
		case CONTACT_TYPE:
			name = "Histories_0_0_" + uid2;
			break;
		default:
			throw new RuntimeException("create database fialed... type is : " + type);
		}
		//init dataBase path
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
		List<String> cacheNames = GlobalHolder.getInstance().getDataBaseTableCacheName();
		if(!cacheNames.contains(name)){
			//创建表
			boolean isCreate = ContentDescriptor.execSQLCreate(context, name);
			if(isCreate){
				GlobalHolder.getInstance().getDataBaseTableCacheName().add(name);
				return true;
			}
			else{
				V2Log.d(TAG, "create database fialed... name is : " + name);
				return false;
			}
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
	 * 
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
				+ "= ?";

		String[] args = new String[] { String.valueOf(uid1),
				String.valueOf(uid2), String.valueOf(uid2),
				String.valueOf(uid1), String.valueOf(groupType) };

		String order = ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_SAVEDATE
				+ " desc , "
				+ ContentDescriptor.HistoriesMessage.Cols.ID
				+ " desc limit " + limit + " offset  " + offset;
		return queryMessage(context, selection, args, order);
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
		List<VMessage> list = queryMessage(context, selection, args, order);
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

		if (mCur == null)
			return imageItems;

		if (mCur.getCount() <= 0) {
			mCur.close();
			return imageItems;
		}

		VMessageImageItem item;
		VMessage current;
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

		mCur.close();
		return imageItems;
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
			Cursor mCur = context.getContentResolver().query(
					ContentDescriptor.HistoriesMedia.CONTENT_URI,
					ContentDescriptor.HistoriesMedia.Cols.ALL_CLOS, selection,
					args, sortOrder);

			if (mCur == null)
				return targetList;

			if (mCur.getCount() <= 0) {
				mCur.close();
				return targetList;
			}

			long currentID = GlobalHolder.getInstance().getCurrentUserId();
			AudioVideoMessageBean currentMedia;
			AudioVideoMessageBean.ChildMessageBean currentChildMedia;
			int isCallOut; // 主动呼叫还是被动 0 主动 1 被动
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
		return queryMessage(context, selection, args, order);
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

		if (mCur == null) {
			return imageItems;
		}

		if (mCur.getCount() <= 0) {
			mCur.close();
			return imageItems;
		}

		VMessageImageItem item;
		VMessage current;
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
		mCur.close();
		return imageItems;
	}

	/**
	 * 查询指定群组中聊天中，正在上传的群文件集合
	 * 
	 * @param context
	 * @param type
	 * @param gid
	 * @return
	 */
	public static List<VCrowdFile> loadGroupUpLoadingFileMessage(
			Context context, int type, long gid, CrowdGroup crowd) {
		
		DataBaseContext mContext = new DataBaseContext(context);
		List<VCrowdFile> fileItems = new ArrayList<VCrowdFile>();
		if (!isTableExist(context, type, gid, 0, CROWD_TYPE)){
			V2Log.d(TAG, "create database fialed...groupType : " + type + "  groupID : "
					+ gid);
			return fileItems;
		}
		
		String sortOrder = ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_SAVEDATE
				+ " desc";
		String where = ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_GROUP_TYPE
				+ "=? and "
				+ ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_GROUP_ID
				+ "= ?";
		String[] args = new String[] { String.valueOf(type),
				String.valueOf(gid) };
		Cursor mCur = mContext.getContentResolver().query(
				ContentDescriptor.HistoriesMessage.CONTENT_URI,
				ContentDescriptor.HistoriesMessage.Cols.ALL_CLOS, where, args,
				sortOrder);

		if (mCur == null || mCur.getCount() == 0) {
			mCur.close();
			return fileItems;
		}

		VCrowdFile crowdFile;
		while (mCur.moveToNext()) {

			VMessage extract = extractMsg(mCur);
			if (extract == null) {
				V2Log.d("The extract VMessage from Cursor failed...get null , id is : "
						+ mCur.getInt(0));
				continue;
			}

			VMessage vm = XmlParser.parseForMessage(extract);
			if (vm == null) {
				V2Log.d("The parse VMessage from failed...get null , id is : "
						+ mCur.getInt(0));
				continue;
			}

			// protected String id;
			// protected String path;
			// protected long size;
			// protected State state;
			// protected long proceedSize;
			// protected String name;
			// protected User uploader;
			// protected Date startTime;
			// protected int flag;
			boolean flag = loadFileMessageById(vm, mContext);
			if (flag) {
				for (VMessageFileItem vMessageFileItem : vm.getFileItems()) {
					if (vMessageFileItem.getState() == VMessageFileItem.STATE_FILE_SENDING) {
						crowdFile = new VCrowdFile();
						crowdFile.setId(vMessageFileItem.getUuid());
						crowdFile.setPath(vMessageFileItem.getFilePath());
						crowdFile.setSize(vMessageFileItem.getFileSize());
						crowdFile.setName(vMessageFileItem.getFileName());
						crowdFile.setState(VCrowdFile.State.UPLOADING);
						crowdFile.setProceedSize((long) vMessageFileItem
								.getProgress());
						crowdFile.setUploader(vm.getFromUser());
						crowdFile.setStartTime(vm.getDate());
						crowdFile.setCrowd(crowd);
						fileItems.add(crowdFile);
					}
				}
			}
		}
		mCur.close();
		return fileItems;
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
		String order = ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_SAVEDATE
				+ " desc limit " + limit + " offset  " + offset;
		return queryMessage(context, selection, args, order);
	}
	
	/**
	 * query the given VMessageAbstractItem Object is exist.
	 * @param context
	 * @param vm
	 * @return ture is exist , false no exist
	 */
	public static boolean queryVMessageItemByID(Context context, VMessageAbstractItem vm) {
		if(vm == null)
			throw new RuntimeException("MessageLoader queryVMessageItemByID ---> the VMessageAbstractItem Object is null");
		
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
			throw new RuntimeException("MessageLoader queryVMessageItemByID ---> invalid VMessageAbstractItem Type ... current type is : " + vm.getType());	
		}
		
		try{
			String[] args = new String[] { vm.getUuid() };
			cursor = mContext.getContentResolver().query(
					uri,
					null, selection, args, null);
			if (cursor != null && cursor.getCount() > 0) 
				return true;
			else
				return false;
		}finally{
			if(cursor != null)
				cursor.close();
		}
	}

	/**
	 * 根据传入VMessage消息对象，查询其数据库的ID值
	 * 
	 * @param context
	 * @param vm
	 * @return
	 */
	public static Long queryVMessageID(Context context, VMessage vm) {

		if (vm == null)
			return -1l;

		if (!isTableExist(
				context,
				vm.getMsgCode(),
				vm.getGroupId(),
				vm.getFromUser().getmUserId(),
				vm.getMsgCode() == V2GlobalEnum.GROUP_TYPE_USER ? MessageLoader.CONTACT_TYPE
						: MessageLoader.CROWD_TYPE))
			return null;

		String selection = ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_ID
				+ "=?";
		String[] args = new String[] { vm.getUUID() };
		Cursor cursor = context.getContentResolver().query(
				ContentDescriptor.HistoriesMessage.CONTENT_URI,
				new String[] { "_id" }, selection, args, null);
		if (cursor != null && cursor.getCount() > 0 && cursor.moveToNext()) {

			return cursor.getLong(cursor.getColumnIndex("_id"));
		}
		return null;
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

			VMessage extract = extractMsg(mCur);
			if (extract == null) {
				V2Log.d("The extract VMessage from Cursor failed...get null , id is : "
						+ mCur.getInt(0));
				continue;
			}

			VMessage vm = XmlParser.parseForMessage(extract);
			if (vm == null) {
				V2Log.d("The parse VMessage from failed...get null , id is : "
						+ mCur.getInt(0));
				continue;
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
	 * 
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
			mContext.getContentResolver()
					.delete(ContentDescriptor.HistoriesAudios.CONTENT_URI,
							ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_ID
									+ "=?",
							new String[] { String.valueOf(audioItems.get(i)
									.getUuid()) });
		}

		List<VMessageFileItem> fileItems = vm.getFileItems();
		for (int i = 0; i < fileItems.size(); i++) {
			mContext.getContentResolver()
					.delete(ContentDescriptor.HistoriesFiles.CONTENT_URI,
							ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_ID
									+ "=?",
							new String[] { String.valueOf(fileItems.get(i)
									.getUuid()) });
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
		return ret;
	}

	/**
	 * delete all messages , according given args..
	 * 
	 * @param context
	 * @param groupType
	 * @param groupID
	 * @param userID
	 * @return
	 */
	public static void deleteMessageByID(Context context, int groupType,
			long groupID, long userID) {

		DataBaseContext mContext = new DataBaseContext(context);
		List<String> tableNames = GlobalHolder.getInstance().getDataBaseTableCacheName();
		String sql = "";
		try{
			if(tableNames.contains(ContentDescriptor.HistoriesMessage.NAME))
				sql = "drop table " + ContentDescriptor.HistoriesMessage.NAME;
			else{
				V2Log.e(TAG, "drop table failed...table no exists , name is : " + ContentDescriptor.HistoriesMessage.NAME);
				return ;
			}
			V2TechDBHelper dbHelper = new V2TechDBHelper(mContext);
			SQLiteDatabase db = dbHelper.getReadableDatabase();
			if (db != null && db.isOpen()) 
				db.execSQL(sql);
			else
				V2Log.d(TAG, "May delete HistoriesMessage failed...DataBase state not normal...groupType : " + groupType + "  groupID : "
						+ groupID + "  userID : " + userID);
			}
		catch(Exception e){
			V2Log.d(TAG, e.getStackTrace() + "");
			V2Log.d(TAG, "May delete HistoriesMessage failed...have exception...groupType : " + groupType + "  groupID : "
					+ groupID + "  userID : " + userID);
			return ;
		}
		//删除其他信息
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
		String[] tables = new String[]{ContentDescriptor.HistoriesAudios.CONTENT_URI.toString()
				,ContentDescriptor.HistoriesGraphic.CONTENT_URI.toString()};
		String[] conditions = new String[] {audioCondition,
				imageCondition };
		String[] names = new String[] {"audioCondition",
				"imageCondition" };
		String[] args = new String[] { String.valueOf(groupType),
				String.valueOf(groupID), String.valueOf(userID) };

		for (int i = 0; i < conditions.length; i++) {
			int ret = mContext.getContentResolver().delete(
					Uri.parse(tables[i]),
					conditions[i], args);
			if (ret <= 0)
				V2Log.d(TAG, "May delete " + names[i]
						+ " failed...groupType : " + groupType + "  groupID : "
						+ groupID + "  userID : " + userID);
		}

		//删除文件
		String[] fileArgs;
		if (groupType == V2GlobalEnum.GROUP_TYPE_USER)
			fileArgs = new String[] { String.valueOf(userID) };
		else
			fileArgs = new String[] { String.valueOf(groupID) };

		int ret = mContext.getContentResolver().delete(
				ContentDescriptor.HistoriesFiles.CONTENT_URI, fileCondition,
				fileArgs);
		if (ret <= 0)
			V2Log.d(TAG, "May delete fileConditions failed...groupType : "
					+ groupType + "  groupID : " + groupID + "  userID : " + userID);
	}
	
	/**
	 * according crowd group id , delete all verification message..
	 * @param context
	 * @param groupID
	 */
	public static void deleteCrowdVerificationMessage(Context context ,
			long groupID) {
		
		DataBaseContext mContext = new DataBaseContext(context);
		int ret = mContext.getContentResolver().delete(
				ContentDescriptor.HistoriesCrowd.CONTENT_URI,
				ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_ID
						+ "=?", new String[] { String.valueOf(groupID) });
		if(ret <= 0)
			V2Log.d(TAG, "May delete CrowdVerificationMessage failed...groupID : " + groupID);
	}

	/**
	 * update the given audio message read state...
	 * 
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
		return mContext.getContentResolver().update(
				ContentDescriptor.HistoriesMessage.CONTENT_URI, values, where,
				selectionArgs);
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
	 * update the given audio message read state...
	 * 
	 * @param context
	 * @param vm
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

	public static VMessageQualification getNewestCrowdVerificationMessage(
			Context context, User user) {

		DataBaseContext mContext = new DataBaseContext(context);
		if (user == null) {
			V2Log.e("To query failed...please check the given User Object");
			return null;
		}

		VMessageQualification message = null;
		String selection = ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_FROM_USER_ID
				+ "= ? or "
				+ ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_TO_USER_ID
				+ "= ?";
		String[] selectionArgs = new String[] {
				String.valueOf(user.getmUserId()),
				String.valueOf(user.getmUserId()) };
		String sortOrder = ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_SAVEDATE
				+ " desc limit 1 offset 0 ";
		Cursor cursor = mContext.getContentResolver().query(
				ContentDescriptor.HistoriesCrowd.CONTENT_URI,
				ContentDescriptor.HistoriesCrowd.Cols.ALL_CLOS, selection,
				selectionArgs, sortOrder);

		if (cursor == null || cursor.getCount() <= 0)
			return null;

		while (cursor.moveToNext()) {
			message = MessageBuilder.extraMsgFromCursor(cursor);
		}
		cursor.close();
		return message;
	}

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

		String order = ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_SAVEDATE
				+ " desc, "
				+ ContentDescriptor.HistoriesMedia.Cols.ID
				+ " desc limit 1 offset 0 ";

		DataBaseContext mContext = new DataBaseContext(context);
		Cursor mCur = mContext.getContentResolver().query(
				ContentDescriptor.HistoriesMedia.CONTENT_URI,
				ContentDescriptor.HistoriesMedia.Cols.ALL_CLOS, selection,
				selectionArgs, order);

		if (mCur == null)
			return null;

		if (mCur.getCount() <= 0) {
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
		List<VMessage> list = queryMessage(context, selection, args, order);
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

		List<VMessage> list = queryMessage(context, selection, args, order);
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
	 * 
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
	 * 
	 * @param vm
	 * @param context
	 * @return
	 */
	private static boolean loadFileMessageById(VMessage vm, Context context) {

		List<VMessageFileItem> fileItems = vm.getFileItems();
		if (fileItems.size() <= 0)
			return false;

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
				return false;
			}

			while (mCur.moveToNext()) {
				int fileTransState = mCur
						.getInt(mCur
								.getColumnIndex(ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SEND_STATE));
				int fileSize = mCur
						.getInt(mCur
								.getColumnIndex(ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SIZE));
				int state = mCur
						.getInt(mCur
								.getColumnIndex(ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SEND_STATE));
				String path = mCur
						.getString(mCur
								.getColumnIndex(ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_PATH));
				item.setState(fileTransState);
				item.setFileSize(fileSize);
				item.setState(state);
				item.setFilePath(path);
			}
		}

		if (mCur != null)
			mCur.close();
		return true;
	}
	
	/**
	 * 根据传入的表名，判断当前数据库中是否存在该表
	 * 
	 * @param context
	 * @param tabName
	 *            表名
	 * @return
	 */
	private static synchronized boolean isExistTable(Context context,
			String tabName) {
		DataBaseContext mContext = new DataBaseContext(context);
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

}
