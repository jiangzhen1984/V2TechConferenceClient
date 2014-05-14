package com.v2tech.view.receiver;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.v2tech.db.ContentDescriptor;
import com.v2tech.service.GlobalHolder;
import com.v2tech.util.V2Log;
import com.v2tech.view.JNIService;
import com.v2tech.view.PublicIntent;
import com.v2tech.vo.ConferenceConversation;
import com.v2tech.vo.ContactConversation;
import com.v2tech.vo.Conversation;
import com.v2tech.vo.CrowdConversation;
import com.v2tech.vo.Group;
import com.v2tech.vo.User;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.Group.GroupType;

public class ConversationConvertReceiver extends BroadcastReceiver {

	private Context mContext;

	@Override
	public void onReceive(Context context, Intent intent) {
		this.mContext = context;
		String action = intent.getAction();
		if (JNIService.JNI_BROADCAST_NEW_MESSAGE.equals(action)) {
			String msgId = intent.getExtras().getString("mid");
			Long fromUid = intent.getExtras().getLong("fromuid");
			Long gid = intent.getExtras().getLong("gid");
			if (msgId == null || msgId.equals("")) {
				V2Log.e("Invalid msgId: " + msgId);
			} else {
				int mid = Integer.parseInt(msgId);
				if (gid != 0) {
					updateGroupConversation(gid, fromUid, mid);
				} else {
					updateContactsConversation(fromUid, mid);
				}
			}
		} else if (PublicIntent.REQUEST_UPDATE_CONVERSATION.equals(action)) {
			long extId = intent.getLongExtra("extId", 0);
			String type = intent.getExtras().getString("type");
			boolean noti = intent.getBooleanExtra("noti", false);
			saveCOnversationStatusToDB(extId, type, noti);

			Conversation cv = GlobalHolder.getInstance()
					.findConversationByType(type, extId);
			if (cv != null) {
				cv.setNotiFlag(noti ? Conversation.NOTIFICATION
						: Conversation.NONE);
			}

			Intent i = new Intent(PublicIntent.UPDATE_CONVERSATION);
			i.addCategory(PublicIntent.DEFAULT_CATEGORY);
			i.putExtras(intent.getExtras());
			mContext.sendBroadcast(i);

		} else if (JNIService.JNI_BROADCAST_CONFERENCE_INVATITION
				.equals(action)
				|| JNIService.JNI_BROADCAST_CONFERENCE_REMOVED.equals(action)) {
			long gid = intent.getLongExtra("gid", 0);

			boolean noti = false;
			Group g = GlobalHolder.getInstance().findGroupById(gid);
			Conversation cv = GlobalHolder.getInstance()
					.findConversationByType(Conversation.TYPE_CONFERNECE, gid);
			if (JNIService.JNI_BROADCAST_CONFERENCE_INVATITION.equals(action)) {

				if (cv == null) {
					cv = new ConferenceConversation(g);
					GlobalHolder.getInstance().addConversation(cv);
				}
				cv.setNotiFlag(Conversation.NOTIFICATION);
				noti = true;
			} else {
				if (cv != null) {
					cv.setNotiFlag(Conversation.NONE);
				}
				noti = false;
			}

			Intent i = new Intent(PublicIntent.UPDATE_CONVERSATION);
			i.addCategory(PublicIntent.DEFAULT_CATEGORY);
			i.putExtra("extId", gid);
			i.putExtra("type", Conversation.TYPE_CONFERNECE);
			i.putExtra("noti", noti);
			i.putExtra("action", action);
			mContext.sendBroadcast(i);
		}
	}

	private void saveCOnversationStatusToDB(long extId, String type,
			boolean noti) {

		ContentValues ct = new ContentValues();
		ct.put(ContentDescriptor.Conversation.Cols.NOTI_FLAG,
				noti ? Conversation.NOTIFICATION : Conversation.NONE);
		mContext.getContentResolver().update(
				ContentDescriptor.Conversation.CONTENT_URI,
				ct,
				ContentDescriptor.Conversation.Cols.EXT_ID + "=? and "
						+ ContentDescriptor.Conversation.Cols.TYPE + "=?",
				new String[] { extId + "", type });
	}

	private void updateGroupConversation(long gid, long fromUid, int msgId) {
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

		// FIXME if doesn't receive group information yet, how to do?
		Group g = GlobalHolder.getInstance().getGroupById(GroupType.CHATING,
				gid);
		Conversation cov = GlobalHolder.getInstance().findConversationByType(
				Conversation.TYPE_GROUP, gid);
		if (cov == null) {
			if (g != null) {
				// FIXME how to do notification if group information didn't
				// receive
				cov = new CrowdConversation(g);
				GlobalHolder.getInstance().addConversation(cov);
			} else {
				V2Log.e(" didn't receive group informaion");
				return;
			}
		}

		// FIXME optimize code
		Intent i = new Intent(PublicIntent.UPDATE_CONVERSATION);
		i.addCategory(PublicIntent.DEFAULT_CATEGORY);
		i.putExtra("extId", gid);
		i.putExtra("type", Conversation.TYPE_GROUP);

		if (type != VMessage.MessageType.IMAGE.getIntValue()) {
			i.putExtra("date", dateString);
			i.putExtra("content", content);
		}
		notif = GlobalHolder.getInstance().CURRENT_CONVERSATION == cov ? false
				: true;
		i.putExtra("noti", notif);
		mContext.sendBroadcast(i);

	}

	/**
	 * Convert new message to update conversation broadcast
	 * FIXME update code structure
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

		cur = mContext.getContentResolver().query(
				ContentDescriptor.Conversation.CONTENT_URI,
				ContentDescriptor.Conversation.Cols.ALL_CLOS,
				ContentDescriptor.Conversation.Cols.TYPE + "=? and "
						+ ContentDescriptor.Conversation.Cols.OWNER + "=? and "
						+ ContentDescriptor.Conversation.Cols.EXT_ID + " =?",
				new String[] { Conversation.TYPE_CONTACT,
						GlobalHolder.getInstance().getCurrentUserId() + "",
						fromUid + "" }, null);

		Conversation c = null;
		if (cur.moveToNext()) {
			long extId = cur.getLong(2);
			String name = cur.getString(3);
			int flag = cur.getInt(4);
			User u = GlobalHolder.getInstance().getUser(extId);
			if (u == null) {
				u = new User(extId);
				u.setName(name);
			}
			c = new ContactConversation(u, flag);
		}
		cur.close();

		// FIXME query from database
		// = GlobalHolder.getInstance().findConversationByType(
		// Conversation.TYPE_CONTACT, fromUid);
		if (c == null) {
			c = GlobalHolder.getInstance().findConversationByType(
					Conversation.TYPE_CONTACT, fromUid);
		}
		if (c == null) {
			User fromUser = GlobalHolder.getInstance().getUser(fromUid);
			c = new ContactConversation(fromUser, Conversation.NOTIFICATION);
			GlobalHolder.getInstance().addConversation(c);

			ContentValues conCv = new ContentValues();
			conCv.put(ContentDescriptor.Conversation.Cols.EXT_ID, fromUid);

			conCv.put(ContentDescriptor.Conversation.Cols.TYPE,
					Conversation.TYPE_CONTACT);
			conCv.put(ContentDescriptor.Conversation.Cols.EXT_NAME,
					fromUser == null ? "" : fromUser.getName());
			conCv.put(ContentDescriptor.Conversation.Cols.NOTI_FLAG,
					notif ? Conversation.NOTIFICATION : Conversation.NONE);
			conCv.put(ContentDescriptor.Conversation.Cols.OWNER, GlobalHolder
					.getInstance().getCurrentUserId());
			mContext.getContentResolver().insert(
					ContentDescriptor.Conversation.CONTENT_URI, conCv);

		} else {
			notif = (c.equals(GlobalHolder.getInstance().CURRENT_CONVERSATION) || (GlobalHolder
					.getInstance().CURRENT_ID == fromUid)) ? false : true;
			c.setNotiFlag(notif ? Conversation.NOTIFICATION : Conversation.NONE);

			ContentValues ct = new ContentValues();
			ct.put(ContentDescriptor.Conversation.Cols.NOTI_FLAG,
					notif ? Conversation.NOTIFICATION : Conversation.NONE);
			mContext.getContentResolver().update(
					ContentDescriptor.Conversation.CONTENT_URI,
					ct,
					ContentDescriptor.Conversation.Cols.EXT_ID + "=? and "
							+ ContentDescriptor.Conversation.Cols.TYPE + "=?",
					new String[] { fromUid + "", Conversation.TYPE_CONTACT });

		}

		notif = (c.equals(GlobalHolder.getInstance().CURRENT_CONVERSATION) || (GlobalHolder
				.getInstance().CURRENT_ID == fromUid)) ? false : true;
		c.setNotiFlag(notif ? Conversation.NOTIFICATION
				: Conversation.NONE);
		//FIXME sometimes c object is not same with cv object
		Conversation cv = GlobalHolder.getInstance()
				.findConversationByType(Conversation.TYPE_CONTACT, fromUid);
		if (cv != null) {
			cv.setNotiFlag(notif ? Conversation.NOTIFICATION
					: Conversation.NONE);
		}
		
		if (GlobalHolder.getInstance().CURRENT_CONVERSATION != null) {
			GlobalHolder.getInstance().CURRENT_CONVERSATION
					.setNotiFlag(notif ? Conversation.NOTIFICATION
							: Conversation.NONE);
		}
		Intent i = new Intent(PublicIntent.UPDATE_CONVERSATION);
		i.addCategory(PublicIntent.DEFAULT_CATEGORY);
		i.putExtra("extId", fromUid);
		i.putExtra("type", Conversation.TYPE_CONTACT);
		if (type != VMessage.MessageType.IMAGE.getIntValue()) {
			i.putExtra("date", dateString);
			i.putExtra("content", content);
		}
		i.putExtra("noti", notif);
		mContext.sendBroadcast(i);
	}

}
