package com.v2tech.db.provider;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;

import com.V2.jni.util.V2Log;
import com.v2tech.db.ContentDescriptor;
import com.v2tech.db.ContentDescriptor.HistoriesCrowd;
import com.v2tech.util.CrashHandler;
import com.v2tech.util.GlobalConfig;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.VMessageQualification;
import com.v2tech.vo.VMessageQualification.Type;
import com.v2tech.vo.VMessageQualificationApplicationCrowd;
import com.v2tech.vo.VMessageQualificationInvitationCrowd;

public class VerificationProvider extends DatabaseProvider{

	public static List<Long> getFriendWaittingVerifyMessage(){
		List<Long> waitingUsers = new ArrayList<Long>();
		Cursor cursor = null;
		try {
			cursor = mContext.getContentResolver().query(ContentDescriptor.HistoriesAddFriends.CONTENT_URI , 
					null , 
					ContentDescriptor.HistoriesAddFriends.Cols.HISTORY_FRIEND_STATE + " = ? ", 
					new String[]{"0"},
					null);
			
			if (cursor == null || cursor.getCount() < 0) {
				return null;
			}

			while (cursor.moveToNext()) {
				long remoteUserID = cursor.getLong(cursor.getColumnIndex(ContentDescriptor.
						HistoriesAddFriends.Cols.HISTORY_FRIEND_REMOTE_USER_ID));
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
	 * @param msg
	 * @return
	 */
	public static int updateQualicationMessage(VMessageQualification msg) {
		return updateQualicationMessage(null , msg);
	}
	
	/**
	 * Update a qualification message to database
	 * @param oldCrowd
	 * @param msg
	 * @return
	 */
	public static int updateQualicationMessage(CrowdGroup oldCrowd , VMessageQualification msg) {
		if (msg == null) {
			V2Log.e("To store failed...please check the given VMessageQualification Object in the databases");
			return -1;
		}
	
	    if(msg.getType() == Type.CROWD_APPLICATION){
	        if(((VMessageQualificationApplicationCrowd)msg).getApplicant() == null) {
	            V2Log.e("To store failed...please check the given VMessageQualification Object , Because applicant user is null!");
	            return -1;
	        }
	        else if(((VMessageQualificationApplicationCrowd)msg).getCrowdGroup() == null){
	            V2Log.e("To store failed...please check the given VMessageQualification Object , Because crowd group is null!");
	            return -1;
	        }
	    }
	
	    if(msg.getType() == Type.CROWD_INVITATION){
	        if(((VMessageQualificationInvitationCrowd)msg).getInvitationUser() == null) {
	            V2Log.e("To store failed...please check the given VMessageQualification Object , Because invitationUser user is null!");
	            return -1;
	        }
	        else if(((VMessageQualificationInvitationCrowd)msg).getCrowdGroup() == null){
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
					oldCrowd == null ? String.valueOf(crowdInviteMsg.getCrowdGroup().getmGId()) : String.valueOf(oldCrowd.getmGId()), 
					String.valueOf(crowdInviteMsg.getInvitationUser().getmUserId())};
			break;
		case CROWD_APPLICATION:
			VMessageQualificationApplicationCrowd crowdApplyMsg = (VMessageQualificationApplicationCrowd) msg;
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_APPLY_REASON,
					crowdApplyMsg.getApplyReason());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_ID,
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
					oldCrowd == null ? String.valueOf(crowdApplyMsg.getCrowdGroup().getmGId()) : String.valueOf(oldCrowd.getmGId()), 
					String.valueOf(crowdApplyMsg.getApplicant().getmUserId()) };
			break;
		case CONTACT:
			break;
		default:
			throw new RuntimeException(
					"invalid VMessageQualification enum type.. please check the type");
		}
		String where = ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_ID
				+ " = ? and " + HistoriesCrowd.Cols.HISTORY_CROWD_REMOTE_USER_ID + " = ?";
		values.put(
				ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_SAVEDATE,
				GlobalConfig.getGlobalServerTime());
		int updates = mContext.getContentResolver().update(
				ContentDescriptor.HistoriesCrowd.CONTENT_URI, values, where,
				selectionArgs);
		return updates;
	}
}
