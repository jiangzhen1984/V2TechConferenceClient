package com.v2tech.view.receiver;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.v2tech.db.ContentDescriptor;
import com.v2tech.logic.ContactConversation;
import com.v2tech.logic.Conversation;
import com.v2tech.logic.GlobalHolder;
import com.v2tech.logic.User;
import com.v2tech.logic.VMessage;
import com.v2tech.util.V2Log;
import com.v2tech.view.JNIService;
import com.v2tech.view.PublicIntent;

public class ConversationConvertReceiver extends BroadcastReceiver {

	private Context mContext;

	@Override
	public void onReceive(Context context, Intent intent) {
		this.mContext = context;
		String action = intent.getAction();
		if (JNIService.JNI_BROADCAST_NEW_MESSAGE.equals(action)) {
			String msgId = intent.getExtras().getString("mid");
			Long fromUid = intent.getExtras().getLong("fromuid");
			if (msgId == null || msgId.equals("")) {
				V2Log.e("Invalid msgId: " + msgId);
			} else {
				int mid = Integer.parseInt(msgId);
				updateContactsConversation(fromUid, mid);
			}
		}
	}

	
	/**
	 * Convert new message to update conversation broadcast
	 * @param fromUid
	 * @param msgId
	 */
	private void updateContactsConversation(long fromUid, int msgId) {
		int type = -1;
		String content = null;
		String dateString = null;
		boolean notif = true;
		Uri uri = ContentUris.withAppendedId(
				ContentDescriptor.Messages.CONTENT_URI, msgId);

		Cursor cur = mContext.getContentResolver().query(uri,
				ContentDescriptor.Messages.Cols.ALL_CLOS, null, null, null);

		if (cur.moveToNext()) {
			content = cur.getString(5);
			// message type
			type = cur.getInt(6);
			// date time
			dateString = cur.getString(7);
			
		}
		cur.close();
		
		if (type == VMessage.MessageType.IMAGE.getIntValue()) {
			return;
		}
		
		
		
		notif= (fromUid == GlobalHolder.getInstance().CURRENT_CONVERSATION_USER ? true: false);
		
		
		Conversation c = GlobalHolder.getInstance().findConversationByType(
				Conversation.TYPE_CONTACT, fromUid);
		if (c == null) {
			User fromUser = GlobalHolder.getInstance().getUser(fromUid);
			c = new ContactConversation(fromUser, Conversation.NOTIFICATION);
			GlobalHolder.getInstance().addConversation(c);

			ContentValues conCv = new ContentValues();
			conCv.put(ContentDescriptor.Conversation.Cols.EXT_ID, fromUid);
			conCv.put(ContentDescriptor.Conversation.Cols.TYPE,
					Conversation.TYPE_CONTACT);
			conCv.put(ContentDescriptor.Conversation.Cols.EXT_NAME,
					fromUser.getName());
			conCv.put(ContentDescriptor.Conversation.Cols.NOTI_FLAG, notif? 
					Conversation.NOTIFICATION:Conversation.NONE);
			conCv.put(ContentDescriptor.Conversation.Cols.OWNER, GlobalHolder
					.getInstance().getCurrentUserId());
			mContext.getContentResolver().insert(
					ContentDescriptor.Conversation.CONTENT_URI, conCv);

		} else {

			c.setNotiFlag(notif? 
					Conversation.NOTIFICATION:Conversation.NONE);

			ContentValues ct = new ContentValues();
			ct.put(ContentDescriptor.Conversation.Cols.NOTI_FLAG,notif? 
					Conversation.NOTIFICATION:Conversation.NONE);
			mContext.getContentResolver().update(
					ContentDescriptor.Conversation.CONTENT_URI,
					ct,
					ContentDescriptor.Conversation.Cols.EXT_ID + "=? and "
							+ ContentDescriptor.Conversation.Cols.TYPE + "=?",
					new String[] { fromUid + "", Conversation.TYPE_CONTACT });
		} 
		
		
		Intent i = new Intent(PublicIntent.UPDATE_CONVERSATION);
		i.addCategory(PublicIntent.DEFAULT_CATEGORY);
		i.putExtra("extId", fromUid);
		i.putExtra("type", Conversation.TYPE_CONTACT);
		i.putExtra("date", dateString);
		i.putExtra("content", content);
		i.putExtra("noti", notif);
		mContext.sendBroadcast(i);
	}
	
	
	
}
