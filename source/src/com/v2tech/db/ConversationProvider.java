package com.v2tech.db;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import com.V2.jni.V2GlobalEnum;
import com.V2.jni.util.V2Log;
import com.v2tech.service.GlobalHolder;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.MessageUtil;
import com.v2tech.view.conversation.MessageLoader;
import com.v2tech.vo.ContactConversation;
import com.v2tech.vo.Conversation;
import com.v2tech.vo.CrowdConversation;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.DepartmentConversation;
import com.v2tech.vo.Group;
import com.v2tech.vo.OrgGroup;
import com.v2tech.vo.User;
import com.v2tech.vo.VMessage;

public class ConversationProvider {

	private static boolean isNoEmpty;

    /**
     * 向数据库插入新的消息对象
     * @param mContext
     * @param vm
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
		
		DepartmentConversation depart;
		while(mCur.moveToNext()){
			
			long groupID = mCur.getLong(mCur.getColumnIndex(ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_USER_TYPE_ID));
			Group department = GlobalHolder.getInstance().findGroupById(
					groupID);
			Group group;
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
			List<Conversation> mConvList,
			Conversation verificationMessageItemData,
			Conversation voiceMessageItem) {

		long verificationDate = 0;
		long voiceMessageDate = 0;
//		if (mCurrentTabFlag == Conversation.TYPE_CONTACT) {
			if (verificationMessageItemData != null
					&& verificationMessageItemData.getDateLong() != null)
				verificationDate = Long.valueOf(verificationMessageItemData
						.getDateLong());
			if (voiceMessageItem != null
					&& voiceMessageItem.getDateLong() != null)
				voiceMessageDate = Long.valueOf(voiceMessageItem.getDateLong());

			if (verificationMessageItemData != null && voiceMessageItem != null) {
				isNoEmpty = true;
				if (verificationDate > voiceMessageDate) 
					verificationMessageItemData.setFirst(true);
				else 
					voiceMessageItem.setFirst(true);
			}
//		}
		Cursor mCur = mContext
				.getContentResolver()
				.query(ContentDescriptor.RecentHistoriesMessage.CONTENT_URI,
						ContentDescriptor.RecentHistoriesMessage.Cols.ALL_CLOS,
						ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_GROUP_TYPE
								+ "= ? or " + ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_GROUP_TYPE + "= ?",
						new String[] { String.valueOf(V2GlobalEnum.GROUP_TYPE_USER) , String.valueOf(V2GlobalEnum.GROUP_TYPE_CROWD)},
						ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_SAVEDATE
								+ " desc");

		while (mCur.moveToNext()) {
			long date = 0;
			Conversation cov = extractConversation(mContext, mCur);
			if (!TextUtils.isEmpty(cov.getDateLong()))
				date = Long.valueOf(cov.getDateLong());
			// 只有会话界面需要添加这两个特殊item
//			if (mCurrentTabFlag == Conversation.TYPE_CONTACT) {
				// 如果两个特殊item都不为空，走if语句
				if (verificationMessageItemData != null
						&& voiceMessageItem != null) {
					if(verificationMessageItemData.isFirst()){
						if(verificationDate > date && !verificationMessageItemData.isAddedItem()){
							mConvList.add(verificationMessageItemData);
							verificationMessageItemData.setAddedItem(true);
						}
						
						if(voiceMessageDate > date && !voiceMessageItem.isAddedItem()){
							mConvList.add(voiceMessageItem);
							voiceMessageItem.setAddedItem(true);
						}
					}
					else{
						if(voiceMessageDate > date && !voiceMessageItem.isAddedItem()){
							mConvList.add(voiceMessageItem);
							voiceMessageItem.setAddedItem(true);
						}
						
						if(verificationDate > date && !verificationMessageItemData.isAddedItem()){
							mConvList.add(verificationMessageItemData);
							verificationMessageItemData.setAddedItem(true);
						}
					}
				} else { // 如果两个特殊item都为空，或其中一个可能为空，则走else语句
							// 如果voiceMessageItem不为null，则进行比较
					if (voiceMessageItem != null
							&& voiceMessageItem.isAddedItem() == false
							&& voiceMessageDate > date) {
						mConvList.add(voiceMessageItem);
						voiceMessageItem.setAddedItem(true);
					}
					// 同理如果verificationMessageItem不为null，则进行比较
					if (verificationMessageItemData != null
							&& verificationMessageItemData.isAddedItem() == false
							&& verificationDate > date) {
						mConvList.add(verificationMessageItemData);
						verificationMessageItemData.setAddedItem(true);
					}
				}
//			}
			mConvList.add(cov);
		}
		mCur.close();
//		if (mCurrentTabFlag == Conversation.TYPE_CONTACT) {
			if (isNoEmpty) {
				// 两种添加顺序
				if (voiceMessageItem.isFirst()) {
					if (voiceMessageItem.isAddedItem() == false)
						mConvList.add(voiceMessageItem);

					if (verificationMessageItemData.isAddedItem() == false)
						mConvList.add(verificationMessageItemData);
				} else {
					if (verificationMessageItemData.isAddedItem() == false)
						mConvList.add(verificationMessageItemData);
					if (voiceMessageItem.isAddedItem() == false)
						mConvList.add(voiceMessageItem);
				}
			} else {
				if (voiceMessageItem != null) {
					if (voiceMessageItem.isAddedItem() == false)
						mConvList.add(voiceMessageItem);
				}

				if (verificationMessageItemData != null){
					if (verificationMessageItemData.isAddedItem() == false)
						mConvList.add(verificationMessageItemData);
				}
			}
//		}
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
		case Conversation.TYPE_GROUP:
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
     * @param cov
     * @param ret
     * @return
     */
	public static int updateConversationToDatabase(Context context, Conversation cov , int ret) {

		if (cov == null)
			return -1;
		
		String where;
		String[] selectionArgs;
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
		if(!TextUtils.isEmpty(cov.getDateLong())){
			values.put(ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_SAVEDATE,
					cov.getDateLong());
		}
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
	private static Conversation extractConversation(
			Context mContext, Cursor cur) {
		
		Conversation cov = null;
		VMessage vm = null;
		int groupType = cur.getInt(cur.getColumnIndex("GroupType"));
		switch (groupType) {
		case V2GlobalEnum.GROUP_TYPE_CROWD:
			long groupID = cur.getLong(cur.getColumnIndex("GroupID"));
			Group crowdGroup = GlobalHolder.getInstance().getGroupById(groupType, groupID);
			if(crowdGroup == null){
				V2Log.e("ConversationProvider:extractConversation ---> get crowdGroup is null , id is :" + groupID);
				crowdGroup = new CrowdGroup(groupID, null, null);
			}
			cov = new CrowdConversation(crowdGroup);
			vm = MessageLoader.getNewestGroupMessage(mContext, groupType, groupID);
			if(vm == null)
				V2Log.e("ConversationProvider:extractConversation ---> get Newest VMessage is null , update failed , id is :" + groupID);
			break;
		case V2GlobalEnum.GROUP_TYPE_USER:
			long extId = cur.getLong(cur.getColumnIndex("RemoteUserID"));
			User u = GlobalHolder.getInstance().getUser(extId);
			if (u == null) {
				V2Log.e("ConversationProvider:extractConversation ---> get user is null , id is :" + extId);
				u = new User(extId);
			}
			cov = new ContactConversation(u);
			vm = MessageLoader.getNewestMessage(mContext, GlobalHolder
					.getInstance().getCurrentUserId(), extId);
			if(vm == null)
				V2Log.e("ConversationProvider:extractConversation ---> get Newest VMessage is null , update failed , id is :" + extId);
			break;
		default:
			throw new RuntimeException("ConversationProvider:extractConversation ---> invalid groupType : " + groupType);
		}
		int readState = cur.getInt(cur.getColumnIndex("ReadState"));
		if (vm != null) {
			cov.setDate(vm.getDateTimeStr());
			cov.setDateLong(String.valueOf(vm.getmDateLong()));
			CharSequence newMessage = MessageUtil.getMixedConversationContent(
					mContext, vm);
			cov.setMsg(newMessage);
			cov.setReadFlag(readState);
		}
		return cov;
	}
}
