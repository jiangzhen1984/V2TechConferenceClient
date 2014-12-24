package com.v2tech.db.provider;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.V2.jni.V2GlobalEnum;
import com.V2.jni.ind.V2Group;
import com.V2.jni.util.V2Log;
import com.V2.jni.util.XmlAttributeExtractor;
import com.v2tech.db.ContentDescriptor;
import com.v2tech.db.ContentDescriptor.HistoriesCrowd;
import com.v2tech.db.vo.FriendMAData;
import com.v2tech.service.GlobalHolder;
import com.v2tech.util.CrashHandler;
import com.v2tech.util.GlobalConfig;
import com.v2tech.view.conversation.MessageBuilder;
import com.v2tech.view.conversation.MessageLoader;
import com.v2tech.vo.AddFriendHistorieNode;
import com.v2tech.vo.ConversationFirendAuthenticationData.VerificationMessageType;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.CrowdGroup.ReceiveQualificationType;
import com.v2tech.vo.GroupQualicationState;
import com.v2tech.vo.User;
import com.v2tech.vo.VMessageQualification;
import com.v2tech.vo.VMessageQualification.ReadState;
import com.v2tech.vo.VMessageQualification.Type;
import com.v2tech.vo.VMessageQualificationApplicationCrowd;
import com.v2tech.vo.VMessageQualificationInvitationCrowd;

public class VerificationProvider extends DatabaseProvider {

	/**
	 * 当前用户邀请他人加入的记录，保存的函数
	 * 
	 * @param msg
	 * @param saveReceivState
	 * @return
	 */
	public static Uri saveCreateQualicationMessage(VMessageQualification msg,
			boolean saveReceivState) {
		return saveQualicationMessage(msg, saveReceivState);
	}

	/**
	 * 正常保存验证消息的函数
	 * 
	 * @param msg
	 * @return
	 */
	public static Uri saveQualicationMessage(VMessageQualification msg) {
		return saveQualicationMessage(msg, false);
	}

	/**
	 * Save new qualification message to database, and fill id to msg object
	 * 
	 * @param msg
	 * @param saveReceivState
	 * @return
	 */
	public static Uri saveQualicationMessage(VMessageQualification msg,
			boolean saveReceivState) {

		if (msg == null) {
			V2Log.e("To store failed...please check the given VMessageQualification Object in the databases");
			return null;
		}
		ContentValues values = new ContentValues();
		values.put(ContentDescriptor.HistoriesCrowd.Cols.OWNER_USER_ID,
				GlobalHolder.getInstance().getCurrentUserId());
		values.put(
				ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_SAVEDATE,
				GlobalConfig.getGlobalServerTime());
		Uri uri = null;
		switch (msg.getType()) {
		case CROWD_INVITATION:
			VMessageQualificationInvitationCrowd crowdInviteMsg = (VMessageQualificationInvitationCrowd) msg;
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_FROM_USER_ID,
					crowdInviteMsg.getInvitationUser().getmUserId());
			if (crowdInviteMsg.getBeInvitatonUser() != null) {
				values.put(
						ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_TO_USER_ID,
						crowdInviteMsg.getBeInvitatonUser().getmUserId());
			}
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_REMOTE_USER_ID,
					crowdInviteMsg.getInvitationUser().getmUserId());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_APPLY_REASON,
					"");
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_REFUSE_REASON,
					crowdInviteMsg.getRejectReason());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_READ_STATE,
					crowdInviteMsg.getReadState().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_STATE,
					crowdInviteMsg.getQualState().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_AUTHTYPE,
					crowdInviteMsg.getCrowdGroup().getAuthType().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_BASE_INFO,
					crowdInviteMsg.getCrowdGroup().toXml());
			values.put(ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_ID,
					crowdInviteMsg.getCrowdGroup().getmGId());
			values.put(HistoriesCrowd.Cols.HISTORY_CROWD_SAVEDATE,
					crowdInviteMsg.getmTimestamp().getTime());
			if (saveReceivState)
				values.put(
						ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_RECEIVER_STATE,
						ReceiveQualificationType.LOCAL_INVITE_TYPE.intValue());
			else
				values.put(
						ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_RECEIVER_STATE,
						ReceiveQualificationType.REMOTE_APPLY_TYPE.intValue());
			uri = MessageBuilder.mContext.getContentResolver().insert(
					ContentDescriptor.HistoriesCrowd.CONTENT_URI, values);
			crowdInviteMsg.setId(ContentUris.parseId(uri));
			return uri;
		case CROWD_APPLICATION:
			VMessageQualificationApplicationCrowd crowdApplyMsg = (VMessageQualificationApplicationCrowd) msg;
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_FROM_USER_ID,
					crowdApplyMsg.getApplicant().getmUserId());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_TO_USER_ID,
					GlobalHolder.getInstance().getCurrentUserId());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_REMOTE_USER_ID,
					crowdApplyMsg.getApplicant().getmUserId());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_APPLY_REASON,
					crowdApplyMsg.getApplyReason());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_REFUSE_REASON,
					crowdApplyMsg.getRejectReason());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_READ_STATE,
					crowdApplyMsg.getReadState().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_STATE,
					crowdApplyMsg.getQualState().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_AUTHTYPE,
					crowdApplyMsg.getCrowdGroup().getAuthType().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_BASE_INFO,
					crowdApplyMsg.getCrowdGroup().toXml());
			values.put(ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_ID,
					crowdApplyMsg.getCrowdGroup().getmGId());
			values.put(HistoriesCrowd.Cols.HISTORY_CROWD_SAVEDATE,
					crowdApplyMsg.getmTimestamp().getTime());
			if (saveReceivState)
				values.put(
						ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_RECEIVER_STATE,
						ReceiveQualificationType.LOCAL_INVITE_TYPE.intValue());
			else
				values.put(
						ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_RECEIVER_STATE,
						ReceiveQualificationType.REMOTE_APPLY_TYPE.intValue());
			uri = MessageBuilder.mContext.getContentResolver().insert(
					ContentDescriptor.HistoriesCrowd.CONTENT_URI, values);
			crowdApplyMsg.setId(ContentUris.parseId(uri));
			return uri;
		case CONTACT:
			break;
		default:
			throw new RuntimeException(
					"invalid VMessageQualification enum type.. please check the type");
		}
		return uri;
	}

	/**
	 * // 别人加我：允许任何人：0已添加您为好友，需要验证：1未处理，2已同意，3已拒绝 //
	 * 我加别人：允许认识人：4你们已成为了好友，需要验证：5等待对方验证，4被同意（你们已成为了好友），6拒绝了你为好友
	 * 
	 * @return
	 */
	public static List<FriendMAData> loadFriendsVerifyMessages() {
		List<FriendMAData> tempList = new ArrayList<FriendMAData>();
		// // 把所有的改为已读
		// String sql = "update " + tableName
		// + " set ReadState=1 where ReadState=0";
		// AddFriendHistroysHandler.update(getApplicationContext(), sql);

		Cursor cursor = null;
		try {
			String order = ContentDescriptor.HistoriesAddFriends.Cols.ID
					+ " desc";
			cursor = mContext.getContentResolver().query(
					ContentDescriptor.HistoriesAddFriends.CONTENT_URI, null,
					null, null, order);

			if (cursor == null || cursor.getCount() < 0) {
				return null;
			}

			AddFriendHistorieNode tempNode = null;
			FriendMAData tempData = null;
			while (cursor.moveToNext()) {
				tempNode = new AddFriendHistorieNode();
				tempData = new FriendMAData();

				tempNode.ownerUserID = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.OWNER_USER_ID));
				tempNode.saveDate = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_SAVEDATE));
				tempNode.fromUserID = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_FROM_USER_ID));
				tempNode.ownerAuthType = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_AUTHTYPE));
				tempNode.toUserID = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_TO_USER_ID));
				tempNode.remoteUserID = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_REMOTE_USER_ID));
				tempNode.applyReason = cursor
						.getString(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_APPLY_REASON));
				tempNode.refuseReason = cursor
						.getString(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_REFUSE_REASON));
				tempNode.addState = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_STATE));
				tempNode.readState = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_MEDIA_READ_STATE));

				tempData._id = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.ID));
				tempData.remoteUserID = tempNode.remoteUserID;
				User user = GlobalHolder.getInstance().getUser(
						tempData.remoteUserID);
				tempData.dheadImage = user.getAvatarBitmap();

				tempData.dbRecordIndex = cursor.getLong(0);
				if ((tempNode.fromUserID == tempNode.remoteUserID)
						&& (tempNode.ownerAuthType == 0)) {// 别人加我允许任何人
					tempData.state = 0;
				} else if ((tempNode.fromUserID == tempNode.remoteUserID)
						&& (tempNode.ownerAuthType == 1)
						&& (tempNode.addState == 0)) {// 别人加我未处理
					tempData.state = 1;
					tempData.authenticationMessage = tempNode.applyReason;
				} else if ((tempNode.fromUserID == tempNode.remoteUserID)
						&& (tempNode.ownerAuthType == 1)
						&& (tempNode.addState == 1)) {// 别人加我已同意
					tempData.state = 2;
					tempData.authenticationMessage = tempNode.applyReason;
				} else if ((tempNode.fromUserID == tempNode.remoteUserID)
						&& (tempNode.ownerAuthType == 1)
						&& (tempNode.addState == 2)) {// 别人加我已拒绝
					tempData.state = 3;
					tempData.authenticationMessage = tempNode.refuseReason;
				} else if ((tempNode.fromUserID == tempNode.ownerUserID)
						&& (tempNode.addState == 0)) {// 我加别人等待验证
					tempData.state = 5;
				} else if ((tempNode.fromUserID == tempNode.ownerUserID)
						&& (tempNode.addState == 1)) {// 我加别人已被同意或我加别人不需验证
					tempData.state = 4;
				} else if ((tempNode.fromUserID == tempNode.ownerUserID)
						&& (tempNode.addState == 2)) {// 我加别人已被拒绝
					tempData.state = 6;
					tempData.authenticationMessage = tempNode.refuseReason;
				}
				tempList.add(tempData);
			}
			return tempList;
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
	
	
	public static List<FriendMAData> loadFriendsVerifyMessages(int limit) {
		List<FriendMAData> tempList = new ArrayList<FriendMAData>();
		// // 把所有的改为已读
		// String sql = "update " + tableName
		// + " set ReadState=1 where ReadState=0";
		// AddFriendHistroysHandler.update(getApplicationContext(), sql);

		Cursor cursor = null;
		try {
			String order = ContentDescriptor.HistoriesAddFriends.Cols.ID
					+ " desc limit " + limit;
			cursor = mContext.getContentResolver().query(
					ContentDescriptor.HistoriesAddFriends.CONTENT_URI, null,
					null, null, order);

			if (cursor == null || cursor.getCount() < 0) {
				return null;
			}

			AddFriendHistorieNode tempNode = null;
			FriendMAData tempData = null;
			while (cursor.moveToNext()) {
				tempNode = new AddFriendHistorieNode();
				tempData = new FriendMAData();

				tempNode.ownerUserID = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.OWNER_USER_ID));
				tempNode.saveDate = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_SAVEDATE));
				tempNode.fromUserID = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_FROM_USER_ID));
				tempNode.ownerAuthType = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_AUTHTYPE));
				tempNode.toUserID = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_TO_USER_ID));
				tempNode.remoteUserID = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_REMOTE_USER_ID));
				tempNode.applyReason = cursor
						.getString(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_APPLY_REASON));
				tempNode.refuseReason = cursor
						.getString(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_REFUSE_REASON));
				tempNode.addState = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_STATE));
				tempNode.readState = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_MEDIA_READ_STATE));
				tempNode.remoteUserNickname = cursor
						.getString(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_CROWD_REMOTE_USER_NICK_NAME));
				tempData._id = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.ID));
				tempData.remoteUserID = tempNode.remoteUserID;
				User user = GlobalHolder.getInstance().getUser(
						tempData.remoteUserID);
				tempData.dheadImage = user.getAvatarBitmap();

				tempData.dbRecordIndex = cursor.getLong(0);
				if ((tempNode.fromUserID == tempNode.remoteUserID)
						&& (tempNode.ownerAuthType == 0)) {// 别人加我允许任何人
					tempData.state = 0;
				} else if ((tempNode.fromUserID == tempNode.remoteUserID)
						&& (tempNode.ownerAuthType == 1)
						&& (tempNode.addState == 0)) {// 别人加我未处理
					tempData.state = 1;
					tempData.authenticationMessage = tempNode.applyReason;
				} else if ((tempNode.fromUserID == tempNode.remoteUserID)
						&& (tempNode.ownerAuthType == 1)
						&& (tempNode.addState == 1)) {// 别人加我已同意
					tempData.state = 2;
					tempData.authenticationMessage = tempNode.applyReason;
				} else if ((tempNode.fromUserID == tempNode.remoteUserID)
						&& (tempNode.ownerAuthType == 1)
						&& (tempNode.addState == 2)) {// 别人加我已拒绝
					tempData.state = 3;
					tempData.authenticationMessage = tempNode.refuseReason;
				} else if ((tempNode.fromUserID == tempNode.ownerUserID)
						&& (tempNode.addState == 0)) {// 我加别人等待验证
					tempData.state = 5;
				} else if ((tempNode.fromUserID == tempNode.ownerUserID)
						&& (tempNode.addState == 1)) {// 我加别人已被同意或我加别人不需验证
					tempData.state = 4;
				} else if ((tempNode.fromUserID == tempNode.ownerUserID)
						&& (tempNode.addState == 2)) {// 我加别人已被拒绝
					tempData.state = 6;
					tempData.authenticationMessage = tempNode.refuseReason;
				}
				tempList.add(tempData);
			}
			return tempList;
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
	 * Delete a qualification message by VMessageQualification Object id
	 * 
	 * @param colsID
	 * 
	 * @return
	 */
	public static boolean deleteCrowdQualMessage(long colsID) {
		String where = ContentDescriptor.HistoriesCrowd.Cols.ID + " = ?";
		String[] selectionArgs = new String[] { String.valueOf(colsID) };
		int ret = mContext.getContentResolver().delete(
				ContentDescriptor.HistoriesCrowd.CONTENT_URI, where,
				selectionArgs);
		if (ret >= 0)
			return true;
		else
			return false;
	}

	/**
	 * 用于删除本地邀请他人入群的缓存记录
	 * 
	 * @param colsID
	 */
	public static boolean deleteCrowdInviteWattingQualMessage(long colsID) {
		String where = ContentDescriptor.HistoriesCrowd.Cols.ID + " = ?";
		String[] selectionArgs = new String[] { String.valueOf(colsID) };
		int ret = mContext.getContentResolver().delete(
				ContentDescriptor.HistoriesCrowd.CONTENT_URI, where,
				selectionArgs);
		if (ret >= 0)
			return true;
		else
			return false;
	}

	/**
	 * according user id , delete all verification message..
	 * 
	 * @param groupID
	 */
	public static int deleteFriendVerificationMessage(long userID) {
		int ret;
		if (userID != -1)
			ret = mContext
					.getContentResolver()
					.delete(ContentDescriptor.HistoriesAddFriends.CONTENT_URI,
							ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_REMOTE_USER_ID
									+ "=?",
							new String[] { String.valueOf(userID) });
		else
			ret = mContext.getContentResolver().delete(
					ContentDescriptor.HistoriesAddFriends.CONTENT_URI, null,
					null);
		if (ret <= 0)
			V2Log.d(MessageLoader.TAG,
					"May delete FriendVerificationMessage failed...groupID : "
							+ userID);
		return ret;
	}

	/**
	 * according crowd group id , delete all verification message..
	 * 
	 * @param groupID
	 */
	public static int deleteCrowdVerificationMessage(long groupID) {
		int ret;
		if (groupID == -1)
			ret = mContext.getContentResolver().delete(
					ContentDescriptor.HistoriesCrowd.CONTENT_URI, null, null);
		else
			ret = mContext.getContentResolver().delete(
					ContentDescriptor.HistoriesCrowd.CONTENT_URI,
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_ID
							+ "=?", new String[] { String.valueOf(groupID) });
		if (ret <= 0)
			V2Log.d(MessageLoader.TAG,
					"May delete CrowdVerificationMessage failed...groupID : "
							+ groupID);
		return ret;
	}

	/**
	 * Update a qualification message to database
	 * 
	 * @param msg
	 * @return
	 */
	public static int updateCrowdQualicationMessage(VMessageQualification msg) {
		return updateCrowdQualicationMessage(null, msg);
	}

	/**
	 * Update a qualification message to database
	 * 
	 * @param oldCrowd
	 * @param msg
	 * @return
	 */
	public static int updateCrowdQualicationMessage(CrowdGroup oldCrowd,
			VMessageQualification msg) {
		if (msg == null) {
			V2Log.e("To store failed...please check the given VMessageQualification Object in the databases");
			return -1;
		}

		if (msg.getType() == Type.CROWD_APPLICATION) {
			if (((VMessageQualificationApplicationCrowd) msg).getApplicant() == null) {
				V2Log.e("To store failed...please check the given VMessageQualification Object , Because applicant user is null!");
				return -1;
			} else if (((VMessageQualificationApplicationCrowd) msg)
					.getCrowdGroup() == null) {
				V2Log.e("To store failed...please check the given VMessageQualification Object , Because crowd group is null!");
				return -1;
			}
		}

		if (msg.getType() == Type.CROWD_INVITATION) {
			if (((VMessageQualificationInvitationCrowd) msg)
					.getInvitationUser() == null) {
				V2Log.e("To store failed...please check the given VMessageQualification Object , Because invitationUser user is null!");
				return -1;
			} else if (((VMessageQualificationInvitationCrowd) msg)
					.getCrowdGroup() == null) {
				V2Log.e("To store failed...please check the given VMessageQualification Object , Because crowd group is null!");
				return -1;
			}
		}

		ContentValues values = new ContentValues();
		String[] selectionArgs = null;
		switch (msg.getType()) {
		case CROWD_INVITATION:
			VMessageQualificationInvitationCrowd crowdInviteMsg = (VMessageQualificationInvitationCrowd) msg;
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_APPLY_REASON,
					"");
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_REFUSE_REASON,
					crowdInviteMsg.getRejectReason());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_READ_STATE,
					crowdInviteMsg.getReadState().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_STATE,
					crowdInviteMsg.getQualState().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_AUTHTYPE,
					crowdInviteMsg.getCrowdGroup().getAuthType().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_BASE_INFO,
					crowdInviteMsg.getCrowdGroup().toXml());
			selectionArgs = new String[] {
					oldCrowd == null ? String.valueOf(crowdInviteMsg
							.getCrowdGroup().getmGId()) : String
							.valueOf(oldCrowd.getmGId()),
					String.valueOf(crowdInviteMsg.getInvitationUser()
							.getmUserId()) };
			break;
		case CROWD_APPLICATION:
			VMessageQualificationApplicationCrowd crowdApplyMsg = (VMessageQualificationApplicationCrowd) msg;
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_APPLY_REASON,
					crowdApplyMsg.getApplyReason());
			values.put(ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_ID,
					crowdApplyMsg.getCrowdGroup().getmGId());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_REFUSE_REASON,
					crowdApplyMsg.getRejectReason());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_READ_STATE,
					crowdApplyMsg.getReadState().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_STATE,
					crowdApplyMsg.getQualState().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_AUTHTYPE,
					crowdApplyMsg.getCrowdGroup().getAuthType().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_BASE_INFO,
					crowdApplyMsg.getCrowdGroup().toXml());
			selectionArgs = new String[] {
					oldCrowd == null ? String.valueOf(crowdApplyMsg
							.getCrowdGroup().getmGId()) : String
							.valueOf(oldCrowd.getmGId()),
					String.valueOf(crowdApplyMsg.getApplicant().getmUserId()) };
			break;
		case CONTACT:
			break;
		default:
			throw new RuntimeException(
					"invalid VMessageQualification enum type.. please check the type");
		}
		String where = ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_ID
				+ " = ? and "
				+ HistoriesCrowd.Cols.HISTORY_CROWD_REMOTE_USER_ID + " = ?";
		values.put(
				ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_SAVEDATE,
				GlobalConfig.getGlobalServerTime());
		int updates = mContext.getContentResolver().update(
				ContentDescriptor.HistoriesCrowd.CONTENT_URI, values, where,
				selectionArgs);
		return updates;
	}

	/**
	 * Update a qualification message to database
	 * 
	 * @param crowdGroup
	 * @param obj
	 * @return
	 */
	public static long updateCrowdQualicationMessageState(
			CrowdGroup crowdGroup, long userId, GroupQualicationState obj) {

		if (obj == null) {
			V2Log.e("MessageBuilder updateQualicationMessageState --> update failed... beacuser given GroupQualicationState"
					+ "is null!");
			return -1;
		}

		if (crowdGroup == null) {
			V2Log.e("MessageBuilder updateQualicationMessageState --> update failed... beacuser get crowdGroup"
					+ "is null!");
			return -1;
		}

		if (crowdGroup.getOwnerUser() == null) {
			V2Log.e("MessageBuilder updateQualicationMessageState --> update failed... beacuser get Owner User"
					+ "is null!");
			return -1;
		}

		long uid = -1;
		long userID = crowdGroup.getOwnerUser().getmUserId();
		if (userID == GlobalHolder.getInstance().getCurrentUserId())
			uid = userId;
		else
			uid = GlobalHolder.getInstance().getCurrentUserId();
		long groupID = crowdGroup.getmGId();
		VMessageQualification crowdQuion = queryCrowdQualMessageByCrowdId(
				userId, groupID);
		if (crowdQuion == null) {
			if (obj.qualicationType == Type.CROWD_APPLICATION) {
				User applicant = GlobalHolder.getInstance().getUser(uid);
				if (applicant == null)
					applicant = new User(uid);
				crowdQuion = new VMessageQualificationApplicationCrowd(
						crowdGroup, applicant);
				((VMessageQualificationApplicationCrowd) crowdQuion)
						.setApplyReason(obj.applyReason);
			} else {
				crowdQuion = new VMessageQualificationInvitationCrowd(
						crowdGroup, GlobalHolder.getInstance().getCurrentUser());
				crowdQuion.setRejectReason(obj.refuseReason);
			}
			crowdQuion.setReadState(ReadState.UNREAD);
			crowdQuion.setQualState(obj.state);
			crowdQuion.setmTimestamp(new Date(GlobalConfig
					.getGlobalServerTime()));
			Uri uri = VerificationProvider.saveQualicationMessage(crowdQuion);
			if (uri != null) {
				long id = Long.parseLong(uri.getLastPathSegment());
				crowdQuion.setId(id);
				return id;
			} else {
				V2Log.e("MessageBuilder updateQualicationMessageState --> Save VMessageQualification Object failed , "
						+ "the Uri is null...groupID is : "
						+ groupID
						+ " userID is : " + userID);
				return -1;
			}
		}

		ContentValues values = new ContentValues();
		switch (obj.qualicationType) {
		case CROWD_INVITATION:
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_REFUSE_REASON,
					obj.refuseReason);
			break;
		case CROWD_APPLICATION:
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_APPLY_REASON,
					obj.applyReason);
			break;
		case CONTACT:
			break;
		default:
			throw new RuntimeException(
					"invalid VMessageQualification enum type.. please check the type");
		}
		values.put(ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_STATE,
				obj.state.intValue());
		values.put(
				ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_READ_STATE,
				obj.readState.intValue());
		values.put(HistoriesCrowd.Cols.HISTORY_CROWD_SAVEDATE,
				GlobalConfig.getGlobalServerTime());
		String where = ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_ID
				+ " = ? and "
				+ HistoriesCrowd.Cols.HISTORY_CROWD_REMOTE_USER_ID + " = ?";
		String[] args = new String[] { String.valueOf(groupID),
				String.valueOf(userId) };
		MessageBuilder.mContext.getContentResolver().update(
				ContentDescriptor.HistoriesCrowd.CONTENT_URI, values, where,
				args);
		return crowdQuion.getId();
	}

	/**
	 * Update a qualification message to database by groupID and userID
	 * 
	 * @param groupID
	 * @param userID
	 * @param obj
	 * @return
	 */
	public static long updateCrowdQualicationMessageState(long groupID,
			long userID, GroupQualicationState obj) {
		if (obj == null) {
			V2Log.e("MessageBuilder updateQualicationMessageState --> update failed... beacuser given GroupQualicationState"
					+ "is null!");
			return -1;
		}

		CrowdGroup crowdGroup = (CrowdGroup) GlobalHolder.getInstance()
				.getGroupById(V2GlobalEnum.GROUP_TYPE_CROWD, groupID);
		if (crowdGroup == null) {
			V2Log.e("MessageBuilder updateQualicationMessageState --> the VMessageQualification Object is null , Need to build"
					+ "groupID is : " + groupID + " userID is : " + userID);
			User user = GlobalHolder.getInstance().getUser(userID);
			if (user == null) {
				V2Log.e("MessageBuilder updateQualicationMessageState --> update failed... beacuser get Owner User"
						+ "from GlobalHolder is null!");
				return -1;
			}

			if (obj.isOwnerGroup)
				crowdGroup = new CrowdGroup(groupID, null, GlobalHolder
						.getInstance().getCurrentUser(), null);
			else
				crowdGroup = new CrowdGroup(groupID, null, user, null);
			return updateCrowdQualicationMessageState(crowdGroup, userID, obj);
		} else {
			return updateCrowdQualicationMessageState(crowdGroup, userID, obj);
		}
	}

	/**
	 * Update a qualification message to database by V2Group Object
	 * 
	 * @param crowd
	 * @param obj
	 * @return
	 */
	public static long updateCrowdQualicationMessageState(V2Group crowd,
			GroupQualicationState obj) {
		if (crowd == null) {
			V2Log.e("MessageBuilder updateQualicationMessageState --> update failed... beacuser given V2Group"
					+ "is null!");
			return -1;
		}

		if (obj == null) {
			V2Log.e("MessageBuilder updateQualicationMessageState --> update failed... beacuser get crowdGroup"
					+ "is null!");
			return -1;
		}

		if (crowd.owner == null) {
			V2Log.e("MessageBuilder updateQualicationMessageState --> update failed... beacuser get Owner User"
					+ "is null!");
			return -1;
		}

		long userID = crowd.owner.uid;
		long groupID = crowd.id;

		CrowdGroup crowdGroup = (CrowdGroup) GlobalHolder.getInstance()
				.getGroupById(V2GlobalEnum.GROUP_TYPE_CROWD, groupID);
		if (crowdGroup == null) {
			V2Log.e("MessageBuilder updateQualicationMessageState --> the VMessageQualification Object is null , Need to build"
					+ "groupID is : " + groupID + " userID is : " + userID);
			User user = GlobalHolder.getInstance().getUser(userID);
			if (user == null) {
				V2Log.e("MessageBuilder updateQualicationMessageState --> update failed... beacuser get Owner User"
						+ "from GlobalHolder is null!");
				return -1;
			}
			crowdGroup = new CrowdGroup(crowd.id, crowd.name, user, null);
			crowdGroup.setBrief(crowd.brief);
			crowdGroup.setAnnouncement(crowd.announce);
		}

		return updateCrowdQualicationMessageState(crowdGroup, userID, obj);
	}

	/**
	 * Update a qualification message to database by cols id
	 * 
	 * @param colsID
	 * @param obj
	 * @return
	 */
	public static int updateCrowdQualicationMessageState(long colsID,
			GroupQualicationState obj) {

		if (obj == null)
			return -1;

		ContentValues values = new ContentValues();
		switch (obj.qualicationType) {
		case CROWD_INVITATION:
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_REFUSE_REASON,
					obj.refuseReason);
			break;
		case CROWD_APPLICATION:
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_APPLY_REASON,
					obj.applyReason);
			break;
		case CONTACT:
			break;
		default:
			throw new RuntimeException(
					"invalid VMessageQualification enum type.. please check the type");
		}
		values.put(ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_STATE,
				obj.state.intValue());
		values.put(
				ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_READ_STATE,
				obj.readState.intValue());
		String where = ContentDescriptor.HistoriesCrowd.Cols.ID + " = ?";
		values.put(
				ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_SAVEDATE,
				GlobalConfig.getGlobalServerTime());
		int updates = MessageBuilder.mContext.getContentResolver().update(
				ContentDescriptor.HistoriesCrowd.CONTENT_URI, values, where,
				new String[] { String.valueOf(colsID) });
		return updates;
	}

	/**
	 * According to the specified ReadState , update group verification message
	 * or friend verification message
	 * 
	 * @param type
	 * <br>
	 *            &nbsp;&nbsp;&nbsp;&nbsp; Crowd Type OR Friend Type
	 * @param readState
	 * <br>
	 *            &nbsp;&nbsp;&nbsp;&nbsp; Read OR UNRead
	 * @param where
	 * <br>
	 *            &nbsp;&nbsp;&nbsp;&nbsp; if null , it would update all
	 *            Messages;
	 * @param args
	 * @return
	 * 
	 * @see VerificationMessageType
	 * @see ReadState
	 */
	public static int updateCrowdQualicationReadState(
			VerificationMessageType type, ReadState readState, String where,
			String[] args) {

		if (type == null | readState == null) {
			V2Log.e(MessageLoader.TAG,
					"updateGroupVerificationReadState --> Update Verification Message ReadState Failed !  Given "
							+ "VerificationMessageType or ReadState is null !");
			return -1;
		}

		ContentValues values = new ContentValues();
		if (type == VerificationMessageType.CROWD_TYPE) {
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_READ_STATE,
					readState.intValue());
			return mContext.getContentResolver().update(
					ContentDescriptor.HistoriesCrowd.CONTENT_URI, values,
					where, args);
		} else {
			values.put(
					ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_MEDIA_READ_STATE,
					readState.intValue());
			return mContext.getContentResolver().update(
					ContentDescriptor.HistoriesAddFriends.CONTENT_URI, values,
					where, args);
		}
	}

	/**
	 * 
	 * @param isCrowd
	 *            true is crowd group , false is friend
	 * @return
	 */
	public static int updateCrowdAllQualicationMessageReadStateToRead(
			boolean isCrowd) {

		ContentValues values = new ContentValues();
		if (isCrowd) {
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_READ_STATE,
					ReadState.READ.intValue());
			return MessageBuilder.mContext.getContentResolver().update(
					ContentDescriptor.HistoriesCrowd.CONTENT_URI, values, null,
					null);
		} else {
			values.put(
					ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_MEDIA_READ_STATE,
					ReadState.READ.intValue());
			return MessageBuilder.mContext.getContentResolver().update(
					ContentDescriptor.HistoriesAddFriends.CONTENT_URI, values,
					null, null);
		}
	}

	/**
	 * Update friend qualication message read state by remote user id
	 * 
	 * @param remoteUserID
	 * @return
	 */
	public static int updateFriendQualicationReadState(long remoteUserID,
			ReadState readState) {
		ContentValues values = new ContentValues();
		values.put(
				ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_MEDIA_READ_STATE,
				readState.intValue());
		return MessageBuilder.mContext
				.getContentResolver()
				.update(ContentDescriptor.HistoriesAddFriends.CONTENT_URI,
						values,
						ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_REMOTE_USER_ID
								+ " = ? ",
						new String[] { String.valueOf(remoteUserID) });
	}

	/**
	 * Query qualification message by Message's id
	 * 
	 * @param colsID
	 * 
	 * @return
	 */
	public static VMessageQualification queryCrowdQualMessageById(long colsID) {

		Cursor cursor = null;
		try {

			String selection = ContentDescriptor.HistoriesCrowd.Cols.ID
					+ " = ? and "
					+ ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_RECEIVER_STATE
					+ " = ? ";
			String[] selectionArgs = new String[] {
					String.valueOf(colsID),
					String.valueOf(ReceiveQualificationType.REMOTE_APPLY_TYPE
							.intValue()) };
			String sortOrder = ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_SAVEDATE
					+ " desc";
			cursor = mContext.getContentResolver().query(
					ContentDescriptor.HistoriesCrowd.CONTENT_URI,
					ContentDescriptor.HistoriesCrowd.Cols.ALL_CLOS, selection,
					selectionArgs, sortOrder);
			if (cursor == null || cursor.getCount() < 0) {
				return null;
			}

			VMessageQualification msg = null;
			if (cursor.moveToNext()) {
				msg = VerificationProvider.extraMsgFromCursor(cursor);
			}
			return msg;
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

	public static VMessageQualification queryCrowdQualMessageByCrowdId(
			User user, CrowdGroup cg) {

		if (user == null || cg == null) {
			V2Log.e("To query failed...please check the given User Object");
			return null;
		}

		return queryCrowdQualMessageByCrowdId(user.getmUserId(), cg.getmGId());
	}

	/**
	 * Query qualification message by crowd group id and user id
	 * 
	 * @param context
	 * @param userID
	 * @param groupID
	 * @return
	 */
	public static VMessageQualification queryCrowdQualMessageByCrowdId(
			long userID, long groupID) {

		Cursor cursor = null;
		try {

			String selection = ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_REMOTE_USER_ID
					+ "= ? and "
					+ ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_ID
					+ "= ? and "
					+ ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_RECEIVER_STATE
					+ "= ? ";
			String[] selectionArgs = new String[] {
					String.valueOf(userID),
					String.valueOf(groupID),
					String.valueOf(ReceiveQualificationType.REMOTE_APPLY_TYPE
							.intValue()) };
			String sortOrder = ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_SAVEDATE
					+ " desc";
			cursor = MessageBuilder.mContext.getContentResolver().query(
					ContentDescriptor.HistoriesCrowd.CONTENT_URI,
					ContentDescriptor.HistoriesCrowd.Cols.ALL_CLOS, selection,
					selectionArgs, sortOrder);

			if (cursor == null || cursor.getCount() < 0) {
				return null;
			}

			VMessageQualification msg = null;
			if (cursor.moveToNext()) {
				msg = VerificationProvider.extraMsgFromCursor(cursor);
			}
			return msg;
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
	 * Query qualification of apply type message by apply user id
	 * 
	 * @param userID
	 * @return
	 */
	public static VMessageQualification queryCrowdApplyQualMessageByUserId(
			long userID) {

		Cursor cursor = null;
		try {

			String selection = ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_REMOTE_USER_ID
					+ "= ? and "
					+ ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_RECEIVER_STATE
					+ "= ?";
			String[] selectionArgs = new String[] {
					String.valueOf(userID),
					String.valueOf(ReceiveQualificationType.REMOTE_APPLY_TYPE
							.intValue()) };
			String sortOrder = ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_SAVEDATE
					+ " desc";
			cursor = MessageBuilder.mContext.getContentResolver().query(
					ContentDescriptor.HistoriesCrowd.CONTENT_URI,
					ContentDescriptor.HistoriesCrowd.Cols.ALL_CLOS, selection,
					selectionArgs, sortOrder);
			if (cursor == null || cursor.getCount() < 0) {
				return null;
			}

			VMessageQualification msg = null;
			if (cursor.moveToFirst()) {
				msg = VerificationProvider.extraMsgFromCursor(cursor);
			}
			cursor.close();
			return msg;
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
	 * Get a List Collection for qualification message from database
	 * 
	 * @param user
	 * 
	 * @return
	 */
	public static List<VMessageQualification> queryCrowdQualMessageList(
			User user) {
		if (user == null) {
			V2Log.e("To query failed...please check the given User Object");
			return null;
		}

		Cursor cursor = null;
		try {
			List<VMessageQualification> list = new ArrayList<VMessageQualification>();
			String selection = "( "
					+ ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_FROM_USER_ID
					+ "= ? or "
					+ ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_TO_USER_ID
					+ "= ? ) and "
					+ ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_RECEIVER_STATE
					+ "= ?";
			String[] selectionArgs = new String[] {
					String.valueOf(user.getmUserId()),
					String.valueOf(user.getmUserId()),
					String.valueOf(ReceiveQualificationType.REMOTE_APPLY_TYPE
							.intValue()) };
			String sortOrder = ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_SAVEDATE
					+ " desc";
			cursor = MessageBuilder.mContext.getContentResolver().query(
					ContentDescriptor.HistoriesCrowd.CONTENT_URI,
					ContentDescriptor.HistoriesCrowd.Cols.ALL_CLOS, selection,
					selectionArgs, sortOrder);

			if (cursor == null || cursor.getCount() < 0) {
				return null;
			}

			while (cursor.moveToNext()) {
				VMessageQualification qualification = VerificationProvider
						.extraMsgFromCursor(cursor);
				if (qualification != null)
					list.add(qualification);
			}
			return list;
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
	 * 用于检测本地邀请他人加群，等待对方回复的记录
	 * 
	 * @param remoteUserID
	 * @return
	 */
	public static long queryCrowdInviteWaitingQualMessageById(long remoteUserID) {
		Cursor cursor = null;
		try {
			String selection = ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_REMOTE_USER_ID
					+ " = ? and "
					+ ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_RECEIVER_STATE
					+ " = ?";
			String[] selectionArgs = new String[] {
					String.valueOf(remoteUserID),
					String.valueOf(ReceiveQualificationType.LOCAL_INVITE_TYPE
							.intValue()) };
			cursor = MessageBuilder.mContext.getContentResolver().query(
					ContentDescriptor.HistoriesCrowd.CONTENT_URI,
					ContentDescriptor.HistoriesCrowd.Cols.ALL_CLOS, selection,
					selectionArgs, null);

			if (cursor == null || cursor.getCount() <= 0)
				return -1;

			if (cursor.moveToFirst())
				return cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesCrowd.Cols.ID));

			return -1;
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}

	public static AddFriendHistorieNode queryFriendQualMessageByUserId(
			long remoteUserID) {
		Cursor cursor = null;
		try {
			cursor = mContext
					.getContentResolver()
					.query(ContentDescriptor.HistoriesAddFriends.CONTENT_URI,
							ContentDescriptor.HistoriesAddFriends.Cols.ALL_CLOS,
							ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_FROM_USER_ID
									+ " = ?",
							new String[] { String.valueOf(remoteUserID) }, null);

			if (cursor == null) {
				return null;
			}

			if (cursor.getCount() < 0) {
				return null;
			}

			if (cursor.moveToNext()) {
				AddFriendHistorieNode tempNode = new AddFriendHistorieNode();
				tempNode.ownerUserID = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.OWNER_USER_ID));
				tempNode.saveDate = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_SAVEDATE));
				tempNode.fromUserID = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_FROM_USER_ID));
				tempNode.ownerAuthType = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_AUTHTYPE));
				tempNode.toUserID = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_TO_USER_ID));
				tempNode.remoteUserID = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_REMOTE_USER_ID));
				tempNode.applyReason = cursor
						.getString(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_APPLY_REASON));
				tempNode.refuseReason = cursor
						.getString(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_REFUSE_REASON));
				tempNode.addState = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_STATE));
				tempNode.readState = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_MEDIA_READ_STATE));
				return tempNode;
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

	public static List<Long> getFriendWaittingVerifyMessage() {
		List<Long> waitingUsers = new ArrayList<Long>();
		Cursor cursor = null;
		try {
			cursor = mContext
					.getContentResolver()
					.query(ContentDescriptor.HistoriesAddFriends.CONTENT_URI,
							null,
							ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_STATE
									+ " = ? ", new String[] { "0" }, null);

			if (cursor == null || cursor.getCount() < 0) {
				return null;
			}

			while (cursor.moveToNext()) {
				long remoteUserID = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_REMOTE_USER_ID));
				waitingUsers.add(remoteUserID);
			}
			return waitingUsers;
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
	 * according Cursor Object , extract VMessageQualification Object.
	 * 
	 * @param cursor
	 * @return
	 */
	public static VMessageQualification extraMsgFromCursor(Cursor cursor) {
		String xml = cursor.getString(cursor.getColumnIndex("CrowdXml"));
		if (TextUtils.isEmpty(xml)) {
			V2Log.e("MessageBuilder extraMsgFromCursor -->pase the CrowdXml failed.. XML is null");
			return null;
		}

		List<V2Group> parseCrowd = XmlAttributeExtractor.parseCrowd(xml);
		if (parseCrowd == null) {
			V2Log.e("MessageBuilder extraMsgFromCursor -->pase the CrowdXml failed.. XML is : "
					+ xml);
			return null;
		}

		V2Group v2Group = parseCrowd.get(0);
		if (v2Group == null || v2Group.creator == null) {
			V2Log.e("MessageBuilder extraMsgFromCursor --> pase the CrowdXml failed..v2Group or v2Group.createor is null");
			return null;
		}

		CrowdGroup group = null;

		long mid = cursor
				.getLong(cursor.getColumnIndex(HistoriesCrowd.Cols.ID));

		long crowdGroupID = cursor.getLong(cursor.getColumnIndex("CrowdID"));
		long saveDate = cursor.getLong(cursor.getColumnIndex("SaveDate"));
		int authType = cursor.getInt(cursor.getColumnIndex("CrowdAuthType"));
		int joinState = cursor.getInt(cursor.getColumnIndex("JoinState"));
		int readState = cursor.getInt(cursor.getColumnIndex("ReadState"));
		String applyReason = cursor.getString(cursor
				.getColumnIndex("ApplyReason"));
		String refuseReason = cursor.getString(cursor
				.getColumnIndex("RefuseReason"));
		group = (CrowdGroup) GlobalHolder.getInstance().getGroupById(
				crowdGroupID);
		if (group == null) {
			group = new CrowdGroup(crowdGroupID, v2Group.name, GlobalHolder
					.getInstance().getUser(v2Group.owner.uid), new Date());
			group.setBrief(v2Group.brief);
			group.setAnnouncement(v2Group.announce);
			group.setAuthType(CrowdGroup.AuthType.fromInt(authType));
		}

		long fromUserID = cursor.getLong(cursor.getColumnIndex("FromUserID"));
		long toUserID = cursor.getLong(cursor.getColumnIndex("ToUserID"));
		if (v2Group.creator.uid == fromUserID) {
			VMessageQualificationInvitationCrowd inviteCrowd = new VMessageQualificationInvitationCrowd(
					group, GlobalHolder.getInstance().getUser(toUserID));
			inviteCrowd.setRejectReason(refuseReason);
			inviteCrowd.setQualState(VMessageQualification.QualificationState
					.fromInt(joinState));
			inviteCrowd.setReadState(VMessageQualification.ReadState
					.fromInt(readState));
			inviteCrowd.setId(mid);
			inviteCrowd.setmTimestamp(new Date(saveDate));
			return inviteCrowd;
		} else {
			VMessageQualificationApplicationCrowd applyCrowd = new VMessageQualificationApplicationCrowd(
					group, GlobalHolder.getInstance().getUser(fromUserID));
			applyCrowd.setApplyReason(applyReason);
			applyCrowd.setRejectReason(refuseReason);
			applyCrowd.setQualState(VMessageQualification.QualificationState
					.fromInt(joinState));
			applyCrowd.setReadState(VMessageQualification.ReadState
					.fromInt(readState));
			applyCrowd.setId(mid);
			applyCrowd.setmTimestamp(new Date(saveDate));
			return applyCrowd;
		}
	}

	/**
	 * 获取最新的好友验证消息
	 * 
	 * @return
	 */
	public static AddFriendHistorieNode getNewestFriendVerificationMessage() {
		Cursor cursor = null;
		try {

			String sortOrder = ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_SAVEDATE
					+ " desc limit 1 offset 0 ";
			cursor = mContext.getContentResolver().query(
					ContentDescriptor.HistoriesAddFriends.CONTENT_URI,
					ContentDescriptor.HistoriesAddFriends.Cols.ALL_CLOS, null,
					null, sortOrder);

			if (cursor == null) {
				return null;
			}

			if (cursor.getCount() < 0) {
				return null;
			}

			if (cursor.moveToNext()) {
				AddFriendHistorieNode tempNode = new AddFriendHistorieNode();
				tempNode.ownerUserID = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.OWNER_USER_ID));
				tempNode.saveDate = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_SAVEDATE));
				tempNode.fromUserID = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_FROM_USER_ID));
				tempNode.ownerAuthType = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_AUTHTYPE));
				tempNode.toUserID = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_TO_USER_ID));
				tempNode.remoteUserID = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_REMOTE_USER_ID));
				tempNode.applyReason = cursor
						.getString(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_APPLY_REASON));
				tempNode.refuseReason = cursor
						.getString(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_REFUSE_REASON));
				tempNode.addState = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_STATE));
				tempNode.readState = cursor
						.getLong(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_MEDIA_READ_STATE));
				tempNode.remoteUserNickname = cursor
						.getString(cursor
								.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_CROWD_REMOTE_USER_NICK_NAME));
				return tempNode;
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
	 * 获取最新的群验证消息
	 * 
	 * @return
	 */
	public static VMessageQualification getNewestCrowdVerificationMessage() {

		Cursor cursor = null;
		try {
			VMessageQualification message = null;
			String selection = ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_RECEIVER_STATE
					+ "= ?";
			String[] selectionArgs = new String[] { String
					.valueOf(ReceiveQualificationType.REMOTE_APPLY_TYPE
							.intValue()) };
			String sortOrder = ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_SAVEDATE
					+ " desc limit 1 offset 0 ";
			cursor = mContext.getContentResolver().query(
					ContentDescriptor.HistoriesCrowd.CONTENT_URI,
					ContentDescriptor.HistoriesCrowd.Cols.ALL_CLOS, selection,
					selectionArgs, sortOrder);

			if (cursor == null || cursor.getCount() <= 0)
				return null;

			if (cursor.moveToFirst()) {
				message = extraMsgFromCursor(cursor);
			}
			return message;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}

	/**
	 * 判断数据库是否有未读的好友验证消息
	 * 
	 * @return
	 */
	public static boolean getUNReandFriendMessage() {
		Cursor cursor = null;
		try {
			String selection = ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_MEDIA_READ_STATE
					+ "= ?";
			String[] selectionArgs = new String[] { String
					.valueOf(ReadState.UNREAD.intValue()) };
			cursor = mContext.getContentResolver().query(
					ContentDescriptor.HistoriesAddFriends.CONTENT_URI,
					ContentDescriptor.HistoriesAddFriends.Cols.ALL_CLOS,
					selection, selectionArgs, null);
			if (cursor == null) {
				return false;
			}

			if (cursor.getCount() < 0) {
				return false;
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			CrashHandler.getInstance().saveCrashInfo2File(e);
			return false;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}
}
