package com.v2tech.view.conversation;

import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import com.v2tech.db.ContentDescriptor;
import com.v2tech.vo.User;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.VMessageAbstractItem;
import com.v2tech.vo.VMessageFaceItem;
import com.v2tech.vo.VMessageImageItem;
import com.v2tech.vo.VMessageTextItem;

public class MessageBuilder {
	
	
	
	
	public static VMessage buildGroupTextMessage(long gid, User fromUser,
			String text) {
		VMessage vm = new VMessage(gid, fromUser, null);
		VMessageTextItem vti = new VMessageTextItem(vm, text);
		vti.setNewLine(true);
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
		new VMessageImageItem(vm, imagePath);
		return vm;
	}

	public static Uri saveMessage(Context context, VMessage vm) {
		if (vm == null) {
			return null;
		}
		ContentValues values = new ContentValues();
		values.put(ContentDescriptor.Messages.Cols.FROM_USER_ID, vm
				.getFromUser().getmUserId());
		if (vm.getToUser() != null) {
			values.put(ContentDescriptor.Messages.Cols.TO_USER_ID, vm
					.getToUser().getmUserId());
		}

		values.put(ContentDescriptor.Messages.Cols.GROUP_ID, vm.getGroupId());
		values.put(ContentDescriptor.Messages.Cols.SEND_TIME,
				vm.getFullDateStr());

		Uri uri = context.getContentResolver().insert(
				ContentDescriptor.Messages.CONTENT_URI, values);
		vm.setId(Long.parseLong(uri.getLastPathSegment()));
		
		List<VMessageAbstractItem> items = vm.getItems();
		for (VMessageAbstractItem item : items) {
			ContentValues itemVal = new ContentValues();
			itemVal.put(ContentDescriptor.MessageItems.Cols.MSG_ID,
					uri.getLastPathSegment());
			itemVal.put(ContentDescriptor.MessageItems.Cols.TYPE,
					item.getType());
			itemVal.put(ContentDescriptor.MessageItems.Cols.LINE,
					item.isNewLine());
			
			String content = null;

			if (item.getType() == VMessageAbstractItem.ITEM_TYPE_TEXT) {
				content = ((VMessageTextItem)item).getText();
			} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_IMAGE) {
				content = ((VMessageImageItem)item).getFilePath();
			} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_FACE) {
				content = ((VMessageFaceItem)item).getIndex()+"";
			}
			
			itemVal.put(ContentDescriptor.MessageItems.Cols.CONTENT, content);
			context.getContentResolver().insert(
					ContentDescriptor.MessageItems.CONTENT_URI, itemVal);

		}
		return uri;
	}
}
