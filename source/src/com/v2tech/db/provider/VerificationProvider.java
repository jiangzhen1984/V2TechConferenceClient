package com.v2tech.db.provider;

import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;

import com.v2tech.db.ContentDescriptor;
import com.v2tech.util.CrashHandler;

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
}
