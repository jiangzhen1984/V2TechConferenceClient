package com.v2tech.db.provider;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import com.V2.jni.util.V2Log;
import com.v2tech.db.ContentDescriptor;
import com.v2tech.service.GlobalHolder;
import com.v2tech.util.CrashHandler;
import com.v2tech.util.MessageUtil;
import com.v2tech.view.conversation.MessageLoader;
import com.v2tech.vo.ContactConversation;
import com.v2tech.vo.Conversation;
import com.v2tech.vo.CrowdConversation;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.DepartmentConversation;
import com.v2tech.vo.DiscussionConversation;
import com.v2tech.vo.DiscussionGroup;
import com.v2tech.vo.Group;
import com.v2tech.vo.OrgGroup;
import com.v2tech.vo.User;
import com.v2tech.vo.V2GlobalConstants;
import com.v2tech.vo.VMessage;

public class ConversationProvider extends DatabaseProvider{

	private static boolean isNoEmpty;

	/**
	 * 向数据库插入新的消息对象
	 * 
	 * @param mContext
	 * @param vm
	 */
	public static void saveConversation(VMessage vm) {

		if (vm == null){
			V2Log.e("ConversationsTabFragment" , "Save Conversation Failed... Because given VMessage Object is null!");
			return;
		}

		long remoteID = 0;
		int readState = 0;
		switch (vm.getMsgCode()) {
		case V2GlobalConstants.GROUP_TYPE_USER:
			if (vm.getFromUser() == null || vm.getToUser() == null){
				V2Log.e("ConversationsTabFragment" , "Save Conversation Failed... Because getFromeUser or getToUser is null"
						+ "for given VMessage Object ... id is : " + vm.getId());
				return;
			}

			if (vm.getFromUser().getmUserId() == GlobalHolder.getInstance()
					.getCurrentUserId()) {
				remoteID = vm.getToUser().getmUserId();
				readState = Conversation.READ_FLAG_READ;
			} else {
				remoteID = vm.getFromUser().getmUserId();
				readState = Conversation.READ_FLAG_UNREAD;
			}
			
			boolean userConversation = queryUserConversation(remoteID);
			if(userConversation){
				V2Log.e("ConversationsTabFragment" , "Save Conversation Failed... Because the Conversation is already exist!"
						+ "remoteUserID is : " + remoteID);
				return ;
			}
			break;
		default:
			boolean groupConversation = queryGroupConversation(vm.getMsgCode(), vm.getGroupId());
			if(groupConversation){
				V2Log.e("ConversationsTabFragment" , "Save Conversation Failed... Because the Conversation is already exist!"
						+ "groupType is : " + vm.getMsgCode() + " groupID is : " + vm.getGroupId());
				return ;
			}
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
	 * 
	 * @param mContext
	 * @return
	 */
	public static List<DepartmentConversation> loadDepartConversation() {

		List<DepartmentConversation> lists = new ArrayList<DepartmentConversation>();
		Cursor cursor = null;
		try {
			
			String where = ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_GROUP_TYPE
					+ "= ?";
			String[] args = new String[] { String
					.valueOf(V2GlobalConstants.GROUP_TYPE_DEPARTMENT) };
			cursor = mContext
					.getContentResolver()
					.query(ContentDescriptor.RecentHistoriesMessage.CONTENT_URI,
							ContentDescriptor.RecentHistoriesMessage.Cols.ALL_CLOS,
							where,
							args,
							ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_SAVEDATE
									+ " desc");
			
			if (cursor == null || cursor.getCount() < 0) {
				V2Log.e("ConversationsTabFragment",
						"loading department conversation get zero");
				return lists;
			}

			DepartmentConversation depart;
			while (cursor.moveToNext()) {
				long groupID = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_USER_TYPE_ID));
				Group department = GlobalHolder.getInstance()
						.findGroupById(groupID);
				Group group;
				if (department != null) {
					group = department;
				} else {
					group = new OrgGroup(groupID, "");
				}
				depart = new DepartmentConversation(group);
				depart.setReadFlag(Conversation.READ_FLAG_READ);
				lists.add(depart);
			}
			return lists;
		} catch (Exception e) {
			e.printStackTrace();
			CrashHandler.getInstance().saveCrashInfo2File(e);
			return lists;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * Depending on the type of organization, query the database fill the
	 * specified collection
	 * 
	 * @param mContext
	 * @param mConvList
	 * @param mCurrentTabFlag
	 * @param verificationMessageItemData
	 * @param voiceMessageItem
	 * @return
	 */
	public static List<Conversation> loadUserConversation(List<Conversation> mConvList,
			Conversation verificationMessageItemData,
			Conversation voiceMessageItem) {

		long verificationDate = 0;
		long voiceMessageDate = 0;
		
		if (verificationMessageItemData != null
				&& verificationMessageItemData.getDateLong() != null)
			verificationDate = Long.valueOf(verificationMessageItemData
					.getDateLong());
		if (voiceMessageItem != null && voiceMessageItem.getDateLong() != null)
			voiceMessageDate = Long.valueOf(voiceMessageItem.getDateLong());

		if (verificationMessageItemData != null && voiceMessageItem != null) {
			isNoEmpty = true;
			if (verificationDate > voiceMessageDate)
				verificationMessageItemData.setFirst(true);
			else
				voiceMessageItem.setFirst(true);
		}
		
		Cursor mCur = null;
		try{
			mCur = mContext
					.getContentResolver()
					.query(ContentDescriptor.RecentHistoriesMessage.CONTENT_URI,
							ContentDescriptor.RecentHistoriesMessage.Cols.ALL_CLOS,
							ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_GROUP_TYPE
									+ "= ? or "
									+ ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_GROUP_TYPE
									+ "= ? or "
									+ ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_GROUP_TYPE 
									+ "= ? or "
									+ ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_GROUP_TYPE + "= ? ",
							new String[] {
									String.valueOf(V2GlobalConstants.GROUP_TYPE_USER),
									String.valueOf(V2GlobalConstants.GROUP_TYPE_CROWD),
									String.valueOf(V2GlobalConstants.GROUP_TYPE_DEPARTMENT),
									String.valueOf(V2GlobalConstants.GROUP_TYPE_DISCUSSION)},
							ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_SAVEDATE
									+ " desc");
	
			while (mCur.moveToNext()) {
				long date = 0;
				Conversation cov = extractConversation(mContext, mCur);
				if (!TextUtils.isEmpty(cov.getDateLong()))
					date = Long.valueOf(cov.getDateLong());
				// 只有会话界面需要添加这两个特殊item
				// if (mCurrentTabFlag == Conversation.TYPE_CONTACT) {
				// 如果两个特殊item都不为空，走if语句
				if (verificationMessageItemData != null && voiceMessageItem != null) {
					if (verificationMessageItemData.isFirst()) {
						if (verificationDate > date
								&& !verificationMessageItemData.isAddedItem()) {
							mConvList.add(verificationMessageItemData);
							verificationMessageItemData.setAddedItem(true);
						}
	
						if (voiceMessageDate > date
								&& !voiceMessageItem.isAddedItem()) {
							mConvList.add(voiceMessageItem);
							voiceMessageItem.setAddedItem(true);
						}
					} else {
						if (voiceMessageDate > date
								&& !voiceMessageItem.isAddedItem()) {
							mConvList.add(voiceMessageItem);
							voiceMessageItem.setAddedItem(true);
						}
	
						if (verificationDate > date
								&& !verificationMessageItemData.isAddedItem()) {
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
				mConvList.add(cov);
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		finally{
			if(mCur != null)
				mCur.close();
		}
		
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

			if (verificationMessageItemData != null) {
				if (verificationMessageItemData.isAddedItem() == false)
					mConvList.add(verificationMessageItemData);
			}
		}
		return mConvList;
	}

	/**
	 * remove user or department conversation from databases
	 * 
	 * @param mContext
	 * @param cov
	 */
	public static void deleteConversation(Context mContext, Conversation cov) {

		if (cov == null)
			return;

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
		case V2GlobalConstants.GROUP_TYPE_DISCUSSION:
			where = ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_USER_TYPE_ID
					+ "=? and "
					+ ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_GROUP_TYPE
					+ "=?";
			break;
		}
		mContext.getContentResolver().delete(
				ContentDescriptor.RecentHistoriesMessage.CONTENT_URI,
				where,
				new String[] { String.valueOf(cov.getExtId()),
						String.valueOf(cov.getType()) });
	}
	
	public static boolean queryGroupConversation(int groupType , long groupID){
		Cursor mCur = null;
		try{
			mCur = mContext
					.getContentResolver()
					.query(ContentDescriptor.RecentHistoriesMessage.CONTENT_URI,
							ContentDescriptor.RecentHistoriesMessage.Cols.ALL_CLOS,
							ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_GROUP_TYPE
									+ "= ? and "
									+ ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_USER_TYPE_ID
									+ " = ?" ,
							new String[] {
									String.valueOf(groupType) , 
									String.valueOf(groupID)},
							ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_SAVEDATE
									+ " desc");
			if(mCur == null || mCur.getCount() < 0)
				return false;
			
			if(mCur.moveToFirst())
				return true;
			return false;
		}
		catch(Exception e){
			e.printStackTrace();
			return false;
		}
		finally{
			if(mCur != null)
				mCur.close();
		}
	}
	
	public static boolean queryUserConversation(long remoteUserID){
		Cursor mCur = null;
		try{
			mCur = mContext
					.getContentResolver()
					.query(ContentDescriptor.RecentHistoriesMessage.CONTENT_URI,
							ContentDescriptor.RecentHistoriesMessage.Cols.ALL_CLOS,
							ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_GROUP_TYPE
									+ "= ? and "
									+ ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_REMOTE_USER_ID
									+ " = ? " ,
							new String[] {
									String.valueOf(V2GlobalConstants.GROUP_TYPE_USER) , 
									String.valueOf(remoteUserID)},
							ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_SAVEDATE
									+ " desc");
			if(mCur == null || mCur.getCount() < 0)
				return false;
			
			if(mCur.moveToFirst())
				return true;
			return false;
		}
		catch(Exception e){
			e.printStackTrace();
			return false;
		}
		finally{
			if(mCur != null)
				mCur.close();
		}
	}

	/**
	 * update conversation state
	 * 
	 * @param context
	 * @param cov
	 * @param ret
	 * @return
	 */
	public static int updateConversationToDatabase(Conversation cov, int ret) {

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
					+ "= ? and "
					+ ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_USER_TYPE_ID
					+ "= ?";
			selectionArgs = new String[] { String.valueOf(cov.getType()),
					String.valueOf(cov.getExtId()) };
			break;
		}

		ContentValues values = new ContentValues();
		if (!TextUtils.isEmpty(cov.getDateLong())) {
			values.put(
					ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_SAVEDATE,
					cov.getDateLong());
		}
		values.put(
				ContentDescriptor.RecentHistoriesMessage.Cols.HISTORY_RECENT_MESSAGE_READ_STATE,
				ret);
		return mContext.getContentResolver().update(
				ContentDescriptor.RecentHistoriesMessage.CONTENT_URI, values,
				where, selectionArgs);
	}

	/**
	 * Depending on the Cursor Object to extract the Conversation Object.
	 * 
	 * @param mContext
	 * @param cur
	 * @return
	 */
	private static Conversation extractConversation(Context mContext, Cursor cur) {

		Conversation cov = null;
		VMessage vm = null;
		int groupType = cur.getInt(cur.getColumnIndex("GroupType"));
		switch (groupType) {
		case V2GlobalConstants.GROUP_TYPE_CROWD:
		case V2GlobalConstants.GROUP_TYPE_DISCUSSION:
		case V2GlobalConstants.GROUP_TYPE_DEPARTMENT:
			long groupID = cur.getLong(cur.getColumnIndex("GroupID"));
			Group group = GlobalHolder.getInstance().getGroupById(groupType,
					groupID);
			if (group == null) {
				V2Log.e("ConversationProvider:extractConversation ---> get Group is null , id is :"
						+ groupID);
			}

			switch (groupType) {
			case V2GlobalConstants.GROUP_TYPE_CROWD:
				group = new CrowdGroup(groupID, null, null);
				cov = new CrowdConversation(group);
				break;
			case V2GlobalConstants.GROUP_TYPE_DEPARTMENT:
				group = new OrgGroup(groupID, null);
				cov = new DepartmentConversation(group);
				break;
			case V2GlobalConstants.GROUP_TYPE_DISCUSSION:
				group = new DiscussionGroup(groupID, null , null);
				cov = new DiscussionConversation(group);
				break;
			default:
				throw new RuntimeException(
						"ConversationProvider:extractConversation ---> invalid groupType : "
								+ groupType);
			}
			vm = MessageLoader.getNewestGroupMessage(mContext, groupType,
					groupID);
			if (vm == null)
				V2Log.e("ConversationProvider:extractConversation ---> get Newest VMessage is null , update failed , id is :"
						+ groupID);
			break;
		case V2GlobalConstants.GROUP_TYPE_USER:
			long extId = cur.getLong(cur.getColumnIndex("RemoteUserID"));
			User u = GlobalHolder.getInstance().getUser(extId);
			if (u == null) {
				V2Log.e("ConversationProvider:extractConversation ---> get user is null , id is :"
						+ extId);
				u = new User(extId);
			}
			cov = new ContactConversation(u);
			vm = MessageLoader.getNewestMessage(mContext, GlobalHolder
					.getInstance().getCurrentUserId(), extId);
			if (vm == null)
				V2Log.e("ConversationProvider:extractConversation ---> get Newest VMessage is null , update failed , id is :"
						+ extId);
			break;
		default:
			throw new RuntimeException(
					"ConversationProvider:extractConversation ---> invalid groupType : "
							+ groupType);
		}
		int readState = cur.getInt(cur.getColumnIndex("ReadState"));
		if (vm != null) {
			cov.setDate(vm.getStringDate());
			cov.setDateLong(String.valueOf(vm.getmDateLong()));
			CharSequence newMessage = MessageUtil.getMixedConversationContent(
					mContext, vm);
			cov.setMsg(newMessage);
			cov.setReadFlag(readState);
		}
		return cov;
	}
}
