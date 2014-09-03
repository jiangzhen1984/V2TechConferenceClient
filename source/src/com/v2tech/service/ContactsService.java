package com.v2tech.service;

import java.util.List;

import android.os.Handler;
import android.os.Message;

import com.V2.jni.GroupRequest;
import com.V2.jni.GroupRequestCallbackAdapter;
import com.v2tech.service.jni.GroupServiceJNIResponse;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.service.jni.RequestConfCreateResponse;
import com.v2tech.vo.ContactGroup;
import com.v2tech.vo.Group;
import com.v2tech.vo.User;

public class ContactsService extends AbstractHandler {

	private static final int CREATE_CONTACTS_GROUP = 10;
	private static final int UPDATE_CONTACTS_GROUP = 11;
	private static final int DELETE_CONTACTS_GROUP = 12;
	
	
	private long mWatingGid = 0;

	public ContactsService() {
		super();
		GroupRequest.getInstance().addCallback(new GroupRequestCB(this));
	}

	/**
	 * Create contacts group
	 * 
	 * @param group
	 * @param caller
	 */
	public void createGroup(ContactGroup group, Registrant caller) {
		if (group == null) {
			if (caller != null) {
				JNIResponse jniRes = new GroupServiceJNIResponse(
						RequestConfCreateResponse.Result.INCORRECT_PAR);
				sendResult(caller, jniRes);
			}
			return;

		}

		GroupRequest.getInstance().createGroup(group.getGroupType().intValue(),
				group.toXml(), "");

//		if (caller != null) {
//			JNIResponse jniRes = new GroupServiceJNIResponse(
//					RequestConfCreateResponse.Result.SUCCESS);
//			sendResult(caller, jniRes);
//		}
	}

	/**
	 * Update contacts group
	 * 
	 * @param group
	 * @param caller
	 */
	public void updateGroup(ContactGroup group, Registrant caller) {
		if (group == null) {
			if (caller != null) {
				JNIResponse jniRes = new GroupServiceJNIResponse(
						RequestConfCreateResponse.Result.INCORRECT_PAR);
				sendResult(caller, jniRes);
			}
			return;

		}

		//Initialize time out message
		initTimeoutMessage(UPDATE_CONTACTS_GROUP, DEFAULT_TIME_OUT_SECS, caller);
		GroupRequest.getInstance()
				.modifyGroupInfo(group.getGroupType().intValue(),
						group.getmGId(), group.toXml());

		if (caller != null) {
			JNIResponse jniRes = new GroupServiceJNIResponse(
					RequestConfCreateResponse.Result.SUCCESS);
			sendResult(caller, jniRes);
		}
	}

	/**
	 * 
	 * @param group
	 * @param caller
	 */
	public void removeGroup(ContactGroup group, Registrant caller) {
		if (group == null) {
			if (caller != null) {
				JNIResponse jniRes = new GroupServiceJNIResponse(
						RequestConfCreateResponse.Result.INCORRECT_PAR);
				sendResult(caller, jniRes);
			}
			return;

		}

		List<Group> list = GlobalHolder.getInstance().getGroup(Group.GroupType.CONTACT);
		//Update all users which belongs this group to root group
		if (list.size() > 0) {
			ContactGroup defaultGroup = (ContactGroup)list.get(0); 
			List<User> userList = group.getUsers();
			for (int i = 0; i < userList.size(); i++) {
				GroupRequest.getInstance().moveUserToGroup(group.getGroupType().intValue(), group.getmGId(), defaultGroup.getmGId(), userList.get(i).getmUserId());
			}
		}
		//Initialize time out message
				initTimeoutMessage(DELETE_CONTACTS_GROUP, DEFAULT_TIME_OUT_SECS, caller);
		GroupRequest.getInstance().delGroup(group.getGroupType().intValue(),
				group.getmGId());
	}
	
	
	
	class GroupRequestCB extends GroupRequestCallbackAdapter {

		private Handler mCallbackHandler;
		public GroupRequestCB(Handler callbackHandler) {
			mCallbackHandler = callbackHandler;
		}
		
		@Override
		public void OnModifyGroupInfoCallback(int groupType, long nGroupID,
				String sXml) {
			if (groupType != Group.GroupType.CONTACT.intValue()) {
				return;
			}
			//If equals, means we are waiting for modified response
			if (nGroupID == mWatingGid) {
				JNIResponse jniRes = new GroupServiceJNIResponse(
						RequestConfCreateResponse.Result.SUCCESS);
				Message.obtain(mCallbackHandler, UPDATE_CONTACTS_GROUP, jniRes).sendToTarget();
				mWatingGid = 0;
			}
		}

		@Override
		public void OnDelGroupCallback(int groupType, long nGroupID,
				boolean bMovetoRoot) {
			if (groupType != Group.GroupType.CONTACT.intValue()) {
				return;
			}
			
			//If equals, means we are waiting for modified response
			if (nGroupID == mWatingGid) {
				JNIResponse jniRes = new GroupServiceJNIResponse(
						RequestConfCreateResponse.Result.SUCCESS);
				Message.obtain(mCallbackHandler, DELETE_CONTACTS_GROUP, jniRes).sendToTarget();
				mWatingGid = 0;
			}
		}

		@Override
		public void OnAddGroupUserInfoCallback(int groupType, long nGroupID,
				String sXml) {
			if (groupType != Group.GroupType.CONTACT.intValue()) {
				return;
			}
			super.OnAddGroupUserInfoCallback(groupType, nGroupID, sXml);
		}
		
	}

}
