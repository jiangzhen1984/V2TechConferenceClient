package com.v2tech.view.conversation;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import com.v2tech.db.ContentDescriptor;
import com.v2tech.service.GlobalHolder;
import com.v2tech.vo.User;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.VMessageAbstractItem;
import com.v2tech.vo.VMessageAudioItem;
import com.v2tech.vo.VMessageFaceItem;
import com.v2tech.vo.VMessageFileItem;
import com.v2tech.vo.VMessageImageItem;
import com.v2tech.vo.VMessageTextItem;

public class MessageBuilder {

	public static VMessage buildGroupTextMessage(long gid, User fromUser,
			String text) {
		String[] array = text.split("\n");
		VMessage vm = new VMessage(gid, fromUser, null);
		for (int i = 0; i < array.length; i++) {
			String str = array[i];
			VMessageTextItem vti = new VMessageTextItem(vm, str);
			vti.setNewLine(true);
		}
		return vm;
	}

	public static VMessage buildTextMessage(User fromUser, User toUser,
			String text) {
		VMessage vm = new VMessage(0, fromUser, toUser);
		VMessageTextItem vti = new VMessageTextItem(vm, text);
		vti.setNewLine(true);
		return vm;
	}

	public static VMessage buildImageMessage(User fromUser, User toUser,
			String imagePath) {
		VMessage vm = new VMessage(0, fromUser, toUser);
		VMessageImageItem item = new VMessageImageItem(vm, imagePath);
		vm.addItem(item);
		return vm;
	}

	public static VMessage buildFileMessage(User fromUser, User toUser,
			String filePath , int fileType) {
		VMessage vm = new VMessage(0, fromUser, toUser);
		new VMessageFileItem(vm, filePath , fileType);
		return vm;
	}

	public static Uri saveMessage(Context context, VMessage vm) {
		if (vm == null || vm.getFromUser() == null || vm.getToUser() == null) {
			return null;
		}
		//确定远程用户
		long remote = -1;
		if(vm.getFromUser().getmUserId() == GlobalHolder.getInstance().getCurrentUserId())
			remote = vm.getToUser().getmUserId();
		else
			remote = vm.getFromUser().getmUserId();
		//判断数据库是否存在
		if(!MessageLoader.init(context , remote))
			return null;
		//直接将xml存入数据库中，方便以后扩展。
		ContentValues values = new ContentValues();
		values.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_GROUP_TYPE, vm
				.getGroupId());
		values.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_GROUP_ID, vm
				.getMsgCode());
		values.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_FROM_USER_ID, vm
				.getFromUser().getmUserId());
		if (vm.getToUser() != null) {
			values.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_TO_USER_ID, vm
					.getToUser().getmUserId());
			values.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_REMOTE_USER_ID, vm
					.getToUser().getmUserId());
		}
		values.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_ID, vm
				.getUUID());
		values.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_TRANSTATE, vm
				.getState());
		values.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_CONTENT, vm
				.getmXmlDatas());
		values.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_SAVEDATE, vm
				.getmDateLong());
		values.put(ContentDescriptor.HistoriesMessage.Cols.OWNER_USER_ID, GlobalHolder.getInstance().getCurrentUserId());
		Uri uri = context.getContentResolver().insert(
				ContentDescriptor.HistoriesMessage.CONTENT_URI, values);
		
//		ContentValues values = new ContentValues();
//		values.put(ContentDescriptor.Messages.Cols.GROUP_ID, 
//				+vm.getGroupId());
//		values.put(ContentDescriptor.Messages.Cols.SEND_TIME,
//				vm.getFullDateStr());
//		values.put(ContentDescriptor.Messages.Cols.UUID, vm.getUUID());
//		values.put(ContentDescriptor.Messages.Cols.STATE, vm.getState());
//
//		Uri uri = context.getContentResolver().insert(
//				ContentDescriptor.Messages.CONTENT_URI, values);
//		vm.setId(Long.parseLong(uri.getLastPathSegment()));
//
//		List<VMessageAbstractItem> items = vm.getItems();
//		for (VMessageAbstractItem item : items) {
//			ContentValues itemVal = new ContentValues();
//			itemVal.put(ContentDescriptor.MessageItems.Cols.MSG_ID,
//					uri.getLastPathSegment());
//			itemVal.put(ContentDescriptor.MessageItems.Cols.TYPE,
//					item.getType());
//			itemVal.put(ContentDescriptor.MessageItems.Cols.LINE,
//					item.isNewLine());
//			itemVal.put(ContentDescriptor.MessageItems.Cols.STATE,
//					item.getState());
//			String content = null;
//
//			if (item.getType() == VMessageAbstractItem.ITEM_TYPE_TEXT) {
//				content = ((VMessageTextItem) item).getText();
//			} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_IMAGE) {
//				content = ((VMessageImageItem) item).getFilePath();
//			} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_FACE) {
//				content = ((VMessageFaceItem) item).getIndex() + "";
//			} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_AUDIO) {
//				content = ((VMessageAudioItem) item).getAudioFilePath() + "|"
//						+ ((VMessageAudioItem) item).getSeconds();
//			} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_FILE) {
//				content = ((VMessageFileItem) item).getFileName() + "|"
//						+ ((VMessageFileItem) item).getFilePath() + "|"
//						+ ((VMessageFileItem) item).getFileSize();
//			}else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_LINK_TEXT) {
//				content = ((VMessageLinkTextItem) item).getText() + "|"
//						+ ((VMessageLinkTextItem) item).getUrl();
//			}
//
//			itemVal.put(ContentDescriptor.MessageItems.Cols.CONTENT, content);
//			itemVal.put(ContentDescriptor.MessageItems.Cols.UUID,
//					item.getUuid());
//			Uri itemUri = context.getContentResolver().insert(
//					ContentDescriptor.MessageItems.CONTENT_URI, itemVal);
//			item.setId(Integer.parseInt(itemUri.getLastPathSegment()));
//
//		}
		return uri;
	}

	public static int updateVMessageItem(Context context,
			VMessageAbstractItem item) {
		ContentValues itemVal = new ContentValues();
		itemVal.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_GROUP_TYPE, item.getType());
//		itemVal.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_LINE, item.isNewLine());
		itemVal.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_TRANSTATE, item.getState());
		String content = null;

//		if (item.getType() == VMessageAbstractItem.ITEM_TYPE_TEXT) {
//			content = ((VMessageTextItem) item).getText();
//		} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_IMAGE) {
//			content = ((VMessageImageItem) item).getFilePath();
//		} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_FACE) {
//			content = ((VMessageFaceItem) item).getIndex() + "";
//		} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_AUDIO) {
//			content = ((VMessageAudioItem) item).getAudioFilePath() + "|"
//					+ ((VMessageAudioItem) item).getSeconds();
//		} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_FILE) {
//			content = ((VMessageFileItem) item).getFileName() + "|"
//					+ ((VMessageFileItem) item).getFilePath() + "|"
//					+ ((VMessageFileItem) item).getFileSize();
//		}

		itemVal.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_CONTENT, content);
		itemVal.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_ID, item.getUuid());
		return context.getContentResolver().update(
				ContentDescriptor.HistoriesMessage.CONTENT_URI, itemVal,
				ContentDescriptor.HistoriesMessage.Cols.ID + "=?",
				new String[] { item.getId() + "" });
	}
	
	
	
	
	public static int updateVMessageItemToSentFalied(Context context, String uuid) {
		ContentValues itemVal = new ContentValues();
		itemVal.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_TRANSTATE , VMessageAbstractItem.STATE_SENT_FALIED);

		return context.getContentResolver().update(
				ContentDescriptor.HistoriesMessage.CONTENT_URI, itemVal,
				ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_ID + "=?",
				new String[] { uuid });
	}
}
