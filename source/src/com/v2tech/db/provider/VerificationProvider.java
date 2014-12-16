package com.v2tech.db.provider;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;

import com.V2.jni.util.V2Log;
import com.v2tech.db.ContentDescriptor;
import com.v2tech.db.ContentDescriptor.HistoriesCrowd;
import com.v2tech.db.vo.FriendMAData;
import com.v2tech.service.GlobalHolder;
import com.v2tech.util.CrashHandler;
import com.v2tech.util.GlobalConfig;
import com.v2tech.vo.AddFriendHistorieNode;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.User;
import com.v2tech.vo.VMessageQualification;
import com.v2tech.vo.VMessageQualification.Type;
import com.v2tech.vo.VMessageQualificationApplicationCrowd;
import com.v2tech.vo.VMessageQualificationInvitationCrowd;

public class VerificationProvider extends DatabaseProvider {

	/**
	 * // 别人加我：允许任何人：0已添加您为好友，需要验证：1未处理，2已同意，3已拒绝 //
	 * 我加别人：允许认识人：4你们已成为了好友，需要验证：5等待对方验证，4被同意（你们已成为了好友），6拒绝了你为好友
	 * 
	 * @return
	 */
	public static List<FriendMAData> loadFriendsVerifyMessages(int limit) {
		List<FriendMAData> tempList = new ArrayList<FriendMAData>();
		// // 把所有的改为已读
		// String sql = "update " + tableName
		// + " set ReadState=1 where ReadState=0";
		// AddFriendHistroysHandler.update(getApplicationContext(), sql);

		Cursor cursor = null;
		try {
			String order = ContentDescriptor.HistoriesAddFriends.Cols.ID+ " desc limit " + limit;
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

				tempNode.ownerUserID = cursor.getLong(cursor.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.OWNER_USER_ID));
				tempNode.saveDate = cursor.getLong(cursor.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_SAVEDATE));
				tempNode.fromUserID = cursor.getLong(cursor.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_FROM_USER_ID));
				tempNode.ownerAuthType = cursor.getLong(cursor.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_AUTHTYPE));
				tempNode.toUserID = cursor.getLong(cursor.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_TO_USER_ID));
				tempNode.remoteUserID = cursor.getLong(cursor.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_REMOTE_USER_ID));
				tempNode.applyReason = cursor.getString(cursor.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_APPLY_REASON));
				tempNode.refuseReason = cursor.getString(cursor.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_REFUSE_REASON));
				tempNode.addState = cursor.getLong(cursor.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_STATE));
				tempNode.readState = cursor.getLong(cursor.getColumnIndex(ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_MEDIA_READ_STATE));

				tempData.remoteUserID = tempNode.remoteUserID;
				User user = GlobalHolder.getInstance().getUser(
						tempData.remoteUserID);
				tempData.dheadImage = user.getAvatarBitmap();
				tempData.name = user.getName();

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
	 * Update a qualification message to database
	 * 
	 * @param msg
	 * @return
	 */
	public static int updateQualicationMessage(VMessageQualification msg) {
		return updateQualicationMessage(null, msg);
	}

	/**
	 * Update a qualification message to database
	 * 
	 * @param oldCrowd
	 * @param msg
	 * @return
	 */
	public static int updateQualicationMessage(CrowdGroup oldCrowd,
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
}
