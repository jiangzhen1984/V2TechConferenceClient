package com.v2tech.view.conversation;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.database.Cursor;

import com.v2tech.db.ContentDescriptor;
import com.v2tech.service.GlobalHolder;
import com.v2tech.vo.User;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.VMessageAbstractItem;
import com.v2tech.vo.VMessageAudioItem;
import com.v2tech.vo.VMessageFaceItem;
import com.v2tech.vo.VMessageImageItem;
import com.v2tech.vo.VMessageTextItem;

public class MessageLoader {

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
		String selection = ContentDescriptor.Messages.Cols.ID + "=? ";
		String[] args = new String[] { msgId + "" };
		String order = ContentDescriptor.Messages.Cols.SEND_TIME
				+ " desc limit 1 offset 0 ";
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
		String selection = ContentDescriptor.Messages.Cols.GROUP_ID + "=? ";
		String[] args = new String[] { groupId + "" };
		String order = ContentDescriptor.Messages.Cols.SEND_TIME
				+ " desc limit " + limit + " offset  " + offset;
		return queryMessage(context, selection, args, order,
				VMessageAbstractItem.ITEM_TYPE_ALL);
	}

	/**
	 * 
	 * @param context
	 * @param groupId
	 * @return
	 */
	public static List<VMessage> loadMessageByPage(Context context, long uid1,
			long uid2, int limit, int offset) {
		String selection = "((" + ContentDescriptor.Messages.Cols.FROM_USER_ID
				+ "=? and " + ContentDescriptor.Messages.Cols.TO_USER_ID
				+ "=? ) or " + "("
				+ ContentDescriptor.Messages.Cols.FROM_USER_ID + "=? and "
				+ ContentDescriptor.Messages.Cols.TO_USER_ID + "=? ))  and "
				+ ContentDescriptor.Messages.Cols.GROUP_ID + "= 0 ";

		String[] args = new String[] { uid1 + "", uid2 + "", uid2 + "",
				uid1 + "" };

		String order = ContentDescriptor.Messages.Cols.SEND_TIME
				+ " desc limit " + limit + " offset  " + offset;
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
		String selection = ContentDescriptor.Messages.Cols.GROUP_ID + "=? ";
		String[] args = new String[] { groupId + "" };
		String order = ContentDescriptor.Messages.Cols.SEND_TIME
				+ " desc limit 1 offset 0 ";
		List<VMessage> list = queryMessage(context, selection, args, order,
				VMessageAbstractItem.ITEM_TYPE_ALL);
		if (list != null && list.size() > 0) {
			return list.get(0);
		} else {
			return null;
		}
	}

	public static VMessage getNewestMessage(Context context, long uid1,
			long uid2) {
		String selection = "((" + ContentDescriptor.Messages.Cols.FROM_USER_ID
				+ "=? and " + ContentDescriptor.Messages.Cols.TO_USER_ID
				+ "=? ) or " + "("
				+ ContentDescriptor.Messages.Cols.FROM_USER_ID + "=? and "
				+ ContentDescriptor.Messages.Cols.TO_USER_ID + "=? ))  and "
				+ ContentDescriptor.Messages.Cols.GROUP_ID + "= 0 ";

		String[] args = new String[] { uid1 + "", uid2 + "", uid2 + "",
				uid1 + "" };

		String order = ContentDescriptor.Messages.Cols.SEND_TIME
				+ " desc limit 1 offset 0 ";
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
		String selection = ContentDescriptor.Messages.Cols.GROUP_ID + "=? ";
		String[] args = new String[] { gid + "" };
		String order = ContentDescriptor.Messages.Cols.SEND_TIME + " desc ";
		return queryMessage(context, selection, args, order, type);

	}

	public static List<VMessage> queryMessage(Context context,
			String selection, String[] args, String sortOrder, int itemType) {

		Cursor mCur = context.getContentResolver().query(
				ContentDescriptor.Messages.CONTENT_URI,
				ContentDescriptor.Messages.Cols.ALL_CLOS, selection, args,
				sortOrder);

		List<VMessage> vimList = new ArrayList<VMessage>();
		if (mCur.getCount() == 0) {
			mCur.close();
			return vimList;
		}

		while (mCur.moveToNext()) {
			VMessage vm = extractMsg(mCur);
			loadVMessageItem(context, vm, itemType);
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

	private static VMessage extractMsg(Cursor cur) {
		if (cur.isClosed()) {
			throw new RuntimeException(" cursor is closed");
		}
		DateFormat dp = new SimpleDateFormat("yyyy-MM-dd HH:mm",
				Locale.getDefault());
		long user1Id = cur.getLong(1);
		long user2Id = cur.getLong(3);
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
		int type = cur.getInt(6);
		// date time
		String dateString = cur.getString(6);
		// group id
		long groupId = cur.getLong(8);

		VMessage vm = new VMessage(groupId, fromUser, toUser, type);
		vm.setId(id);
		try {
			vm.setDate(dp.parse(dateString));
		} catch (ParseException e) {
			e.printStackTrace();
		}

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

}
