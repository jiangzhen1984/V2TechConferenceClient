package com.v2tech.view;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import com.V2.jni.V2GlobalEnum;
import com.V2.jni.util.V2Log;
import com.v2tech.db.ContentDescriptor;
import com.v2tech.db.DataBaseContext;
import com.v2tech.service.GlobalHolder;
import com.v2tech.util.MessageUtil;
import com.v2tech.util.Notificator;
import com.v2tech.view.conversation.MessageLoader;
import com.v2tech.vo.ContactConversation;
import com.v2tech.vo.Conversation;
import com.v2tech.vo.DepartmentConversation;
import com.v2tech.vo.Group;
import com.v2tech.vo.OrgGroup;
import com.v2tech.vo.User;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.Group.GroupType;

public class ConversationProvider {

	private static boolean isNoEmpty;
	private static boolean isVoiceSpecificAdd;
	private static boolean isVerificationSpecificAdd;

	
	/**
	 * 向数据库插入新的消息对象
	 * 
	 * @param vm
	 * @param extId
	 */
	public static void saveConversation(Context mContext , VMessage vm) {

		if (vm == null)
			return;

		long remoteID = 0;
		int readState = 0;
		switch (vm.getMsgCode()) {
		case V2GlobalEnum.GROUP_TYPE_USER:
			if (vm.getFromUser() == null || vm.getToUser() == null)
				return;

			if (vm.getFromUser().getmUserId() == GlobalHolder.getInstance()
					.getCurrentUserId()) {
				remoteID = vm.getToUser().getmUserId();
				readState = Conversation.READ_FLAG_READ;
			} else {
				remoteID = vm.getFromUser().getmUserId();
				readState = Conversation.READ_FLAG_UNREAD;
			}
			break;
		default:
			break;
		}
		ContentValues conCv = new ContentValues();
		conCv.put(
				ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_FROM_USER_ID,
				vm.getFromUser().getmUserId());
		conCv.put(
				ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_REMOTE_USER_ID,
				remoteID);
		conCv.put(
				ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_GROUP_TYPE,
				vm.getMsgCode());
		conCv.put(
				ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_READ_STATE,
				readState);
		conCv.put(ContentDescriptor.RecentHistoriesMessage.Cols.OWNER_USER_ID,
				GlobalHolder.getInstance().getCurrentUserId());
		conCv.put(
				ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_TO_USER_ID,
				vm.getToUser().getmUserId());
		conCv.put(
				ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_USER_TYPE_ID,
				vm.getGroupId());
		conCv.put(
				ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_CONTENT,
				vm.getmXmlDatas());
		conCv.put(
				ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_ID,
				vm.getUUID());
		conCv.put(
				ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_SAVEDATE,
				vm.getmDateLong());
		mContext.getContentResolver().insert(
				ContentDescriptor.RecentHistoriesMessage.CONTENT_URI, conCv);
	}
	
	/**
	 * loading the department conversation
	 * @param mContext
	 * @return
	 */
	public static List<DepartmentConversation> loadDepartConversation(Context mContext) {

		List<DepartmentConversation> lists = new ArrayList<DepartmentConversation>();
		String where = ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_GROUP_TYPE
				+ "= ?";
		String[] args = new String[] { String
				.valueOf(V2GlobalEnum.GROUP_TYPE_DEPARTMENT) };
		Cursor mCur = mContext
				.getContentResolver()
				.query(ContentDescriptor.RecentHistoriesMessage.CONTENT_URI,
						ContentDescriptor.RecentHistoriesMessage.Cols.ALL_CLOS,
						where,
						args,
						ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_SAVEDATE
								+ " desc");
		if(mCur == null || mCur.getCount() <= 0){
			V2Log.e("ConversationsTabFragment", "loading department conversation get zero");
			return lists;
		}
		
		DepartmentConversation depart = null;
		while(mCur.moveToNext()){
			
			long groupID = mCur.getLong(mCur.getColumnIndex(ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_USER_TYPE_ID));
			Group department = GlobalHolder.getInstance().findGroupById(
					groupID);
			Group group = null;
			if(department != null){
				group = department;
			}
			else{
				group = new OrgGroup(groupID , "");
			}
			depart = new DepartmentConversation(group);
			depart.setReadFlag(Conversation.READ_FLAG_READ);
			lists.add(depart);
		}
		return lists;
	}

	/**
	 * Depending on the type of organization, query the database fill the specified collection
	 * @param mContext
	 * @param mConvList
	 * @param mCurrentTabFlag
	 * @param verificationMessageItemData
	 * @param voiceMessageItem
	 * @return
	 */
	public static List<Conversation> loadUserConversation(Context mContext,
			List<Conversation> mConvList, final int mCurrentTabFlag,
			Conversation verificationMessageItemData,
			Conversation voiceMessageItem) {

		Conversation firstAdd = null;
		Conversation secondAdd = null;
		long verificationDate = 0;
		long voiceMessageDate = 0;
		if (mCurrentTabFlag == Conversation.TYPE_CONTACT) {
			if (verificationMessageItemData != null
					&& verificationMessageItemData.getDateLong() != null)
				verificationDate = Long.valueOf(verificationMessageItemData
						.getDateLong());
			if (voiceMessageItem != null
					&& voiceMessageItem.getDateLong() != null)
				voiceMessageDate = Long.valueOf(voiceMessageItem.getDateLong());

			if (verificationMessageItemData != null && voiceMessageItem != null) {
				isNoEmpty = true;
				if (verificationDate > voiceMessageDate) {
					verificationMessageItemData.setFirst(true);
					voiceMessageItem.setFirst(false);
					firstAdd = verificationMessageItemData;
					secondAdd = voiceMessageItem;
				} else {
					verificationMessageItemData.setFirst(false);
					voiceMessageItem.setFirst(true);
					firstAdd = voiceMessageItem;
					secondAdd = verificationMessageItemData;
				}
			}
		}
		Cursor mCur = mContext
				.getContentResolver()
				.query(ContentDescriptor.RecentHistoriesMessage.CONTENT_URI,
						ContentDescriptor.RecentHistoriesMessage.Cols.ALL_CLOS,
						ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_GROUP_TYPE
								+ "=?",
						new String[] { String.valueOf(mCurrentTabFlag) },
						ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_SAVEDATE
								+ " desc");

		while (mCur.moveToNext()) {
			long date = 0;
			ContactConversation cov = extractContactConversation(mContext, mCur);
			if (!TextUtils.isEmpty(cov.getDateLong()))
				date = Long.valueOf(cov.getDateLong());
			// 只有会话界面需要添加这两个特殊item
			if (mCurrentTabFlag == Conversation.TYPE_CONTACT) {
				// 如果两个特殊item都不为空，走if语句
				if (verificationMessageItemData != null
						&& voiceMessageItem != null) {
					if (firstAdd != null && isVoiceSpecificAdd == false
							&& firstAdd.getDate() != null
							&& Long.valueOf(firstAdd.getDateLong()) > date) {
						mConvList.add(firstAdd);
						isVoiceSpecificAdd = true;
					}

					if (secondAdd != null && isVerificationSpecificAdd == false
							&& secondAdd.getDate() != null
							&& Long.valueOf(secondAdd.getDateLong()) > date) {
						mConvList.add(secondAdd);
						isVerificationSpecificAdd = true;
					}
				} else { // 如果两个特殊item都为空，或其中一个可能为空，则走else语句
							// 如果voiceMessageItem不为null，则进行比较
					if (voiceMessageItem != null
							&& isVoiceSpecificAdd == false
							&& voiceMessageItem.getDateLong() != null
							&& Long.valueOf(voiceMessageItem.getDateLong()) > date) {
						mConvList.add(voiceMessageItem);
						isVoiceSpecificAdd = true;
					}
					// 同理如果verificationMessageItem不为null，则进行比较
					if (verificationMessageItemData != null
							&& isVerificationSpecificAdd == false
							&& verificationMessageItemData.getDateLong() != null
							&& Long.valueOf(verificationMessageItemData
									.getDateLong()) > date) {
						mConvList.add(verificationMessageItemData);
						isVerificationSpecificAdd = true;
					}
				}
			}
			mConvList.add(cov);
		}
		mCur.close();
		if (mCurrentTabFlag == Conversation.TYPE_CONTACT) {
			if (isNoEmpty) {
				// 两种添加顺序
				if (voiceMessageItem.isFirst()) {
					if (isVoiceSpecificAdd == false)
						mConvList.add(voiceMessageItem);

					if (isVerificationSpecificAdd == false)
						mConvList.add(verificationMessageItemData);
				} else {
					if (isVerificationSpecificAdd == false)
						mConvList.add(verificationMessageItemData);
					if (isVoiceSpecificAdd == false)
						mConvList.add(voiceMessageItem);
				}
			} else {
				if (voiceMessageItem != null) {
					if (isVoiceSpecificAdd == false)
						mConvList.add(voiceMessageItem);

					if (verificationMessageItemData != null)
						mConvList.add(verificationMessageItemData);
				}
			}
		}
		return mConvList;
	}
	
	/**
	 * remove user or department conversation from databases
	 * @param mContext
	 * @param cov
	 */
	public static void deleteConversation(Context mContext , Conversation cov){
		
		if(cov == null)
			return ;
			
		String where = "";
		switch (cov.getType()) {
		case Conversation.TYPE_CONTACT:
			where = ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_REMOTE_USER_ID
					+ "=? and "
					+ ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_GROUP_TYPE
					+ "=?";
			break;
		case Conversation.TYPE_DEPARTMENT:
			where = ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_USER_TYPE_ID
			+ "=? and "
			+ ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_GROUP_TYPE
			+ "=?";
			break;
		}
		mContext.getContentResolver()
		.delete(ContentDescriptor.RecentHistoriesMessage.CONTENT_URI,
				where, new String[] { String.valueOf(cov.getExtId()), String.valueOf(cov.getType()) });
	}
	
	/**
	 * update conversation state
	 * @param context
	 * @param msgID
	 * @return
	 */
	public static int updateConversationToDatabase(Context context, Conversation cov , int ret) {

		if (cov == null)
			return -1;
		
		String where = "";
		String[] selectionArgs = null;
		switch (cov.getType()) {
		case Conversation.TYPE_CONTACT:
			where = ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_REMOTE_USER_ID
			+ "= ?";
			selectionArgs = new String[] { String.valueOf(cov.getExtId()) };
			break;
		default:
			where = ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_GROUP_TYPE
					+ "= ? and " + ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_USER_TYPE_ID + "= ?";
			selectionArgs = new String[] { String.valueOf(cov.getType()) , String.valueOf(cov.getExtId()) };
			break;
		}
		
		DataBaseContext mContext = new DataBaseContext(context);
		ContentValues values = new ContentValues();
		values.put(ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_READ_STATE,
				ret);
		return mContext.getContentResolver().update(
				ContentDescriptor.RecentHistoriesMessage.CONTENT_URI, values, where,
				selectionArgs);
	}

	/**
	 * Depending on the Cursor Object to extract the Conversation Object.
	 * @param mContext
	 * @param cur
	 * @return
	 */
	private static ContactConversation extractContactConversation(
			Context mContext, Cursor cur) {
		long extId = cur.getLong(cur.getColumnIndex("RemoteUserID"));
		int readState = cur.getInt(cur.getColumnIndex("ReadState"));
		User u = GlobalHolder.getInstance().getUser(extId);
		if (u == null) {
			V2Log.e("extractConversation , get user is null , id is :" + extId);
			u = new User(extId);
		}
		ContactConversation cov = new ContactConversation(u);
		VMessage vm = MessageLoader.getNewestMessage(mContext, GlobalHolder
				.getInstance().getCurrentUserId(), extId);
		if (vm != null) {
			cov.setDate(vm.getDateTimeStr());
			cov.setDateLong(String.valueOf(vm.getmDateLong()));
			CharSequence newMessage = MessageUtil.getMixedConversationContent(
					mContext, vm);
			cov.setMsg(newMessage);
		}
		cov.setReadFlag(readState);
		return cov;
	}
}
