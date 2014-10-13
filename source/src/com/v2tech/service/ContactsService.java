package com.v2tech.service;

import java.util.Iterator;
import java.util.List;

import android.os.Handler;
import android.os.Message;

import com.V2.jni.GroupRequest;
import com.V2.jni.GroupRequestCallbackAdapter;
import com.V2.jni.ind.FileJNIObject;
import com.V2.jni.ind.V2Group;
import com.V2.jni.ind.V2User;
import com.v2tech.service.jni.GroupServiceJNIResponse;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.service.jni.RequestConfCreateResponse;
import com.v2tech.util.HeartCharacterProcessing;
import com.v2tech.vo.ContactGroup;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.User;

/**
 * 
 * @author jiangzhen
 * 
 */
public class ContactsService extends AbstractHandler {

	private static final int CREATE_CONTACTS_GROUP = 10;
	private static final int UPDATE_CONTACTS_GROUP = 11;
	private static final int DELETE_CONTACTS_GROUP = 12;
	private static final int UPDATE_CONTACT_BELONGS_GROUP = 13;

	private long mWatingGid = 0;

	private GroupRequestCB crCB;

	public ContactsService() {
		super();
		crCB = new GroupRequestCB(this);
		GroupRequest.getInstance().addCallback(crCB);
	}

	/**
	 * @comment-user:wenzl 2014年9月19日
	 * @overview:
	 *
	 * @param contactGroup
	 * @param user
	 * @param additInfo
	 * @param commentName
	 * @return:
	 */
	public void addContact(Group contactGroup, User user, String additInfo,
			String commentName) {

		additInfo=HeartCharacterProcessing.convert(additInfo);
		commentName=HeartCharacterProcessing.convert(commentName);
		
		String groupInfo = "<friendgroup" + " id='" + contactGroup.getmGId()
				+ "'/>";

		String userInfo = "<userlist>" + "<user id='" + user.getmUserId() + "'"
				+ " commentname='" + commentName + "'></user>" + "</userlist>";

		GroupRequest.getInstance().inviteJoinGroup(
				Group.GroupType.CONTACT.intValue(), groupInfo, userInfo,
				additInfo);
	}

	/**
	 * @comment-user:wenzl 2014年9月19日
	 * @overview:删除联系人
	 *
	 * @param user
	 * @return:
	 */
	public void delContact(User user) {
		long nGroupID = -1;
		Iterator<Group> iterator = user.getBelongsGroup().iterator();
		boolean ret = false;
		while (iterator.hasNext()) {
			Group temp = iterator.next();
			if (temp.getGroupType() == Group.GroupType.CONTACT) {
				nGroupID = temp.getmGId();
				ret = true;
			}
		}

		if (!ret) {
			return;
		}

		long nUserID = user.getmUserId();
		GroupRequest.getInstance().delGroupUser(
				Group.GroupType.CONTACT.intValue(), nGroupID, nUserID);
	}

	/**
	 * @comment-user:wenzl 2014年9月19日
	 * @overview:同意被加为联系人
	 *
	 * @param groupId
	 * @param nUserID
	 * @return:
	 */
	public void acceptAddedAsContact(long groupId, long nUserID) {
		GroupRequest.getInstance().acceptInviteJoinGroup(
				Group.GroupType.CONTACT.intValue(), groupId, nUserID);
	}

	/**
	 * @comment-user:wenzl 2014年9月19日
	 * @overview:拒绝被加为联系人
	 *
	 * @param nGroupID
	 * @param nUserID
	 * @param reason
	 * @return:
	 */
	public void refuseAddedAsContact(long nGroupID, long nUserID, String reason) {
		reason=HeartCharacterProcessing.convert(reason);
		GroupRequest.getInstance().refuseInviteJoinGroup(
				Group.GroupType.CONTACT.intValue(), nGroupID, nUserID, reason);
	}

	/**
	 * Create contacts group
	 * 
	 * @param group
	 * @param caller
	 */
	public void createGroup(ContactGroup group, Registrant caller) {
		if (!checkParamNull(caller, new Object[] { group })) {
			return;
		}

		// Initialize time out message
		initTimeoutMessage(CREATE_CONTACTS_GROUP, DEFAULT_TIME_OUT_SECS, caller);

		GroupRequest.getInstance().createGroup(group.getGroupType().intValue(),
				group.toXml(), "");
	}

	/**
	 * Update contacts group
	 * 
	 * @param group
	 * @param caller
	 */
	public void updateGroup(ContactGroup group, Registrant caller) {
		if (!checkParamNull(caller, new Object[] { group })) {
			return;
		}

		mWatingGid = group.getmGId();
		// Initialize time out message
		initTimeoutMessage(UPDATE_CONTACTS_GROUP, DEFAULT_TIME_OUT_SECS, caller);
		GroupRequest.getInstance()
				.modifyGroupInfo(group.getGroupType().intValue(),
						group.getmGId(), group.toXml());
	}

	/**
	 * 
	 * @param group
	 * @param caller
	 */
	public void removeGroup(ContactGroup group, Registrant caller) {
		if (!checkParamNull(caller, new Object[] { group })) {
			return;
		}

		mWatingGid = group.getmGId();
		List<Group> list = GlobalHolder.getInstance().getGroup(
				Group.GroupType.CONTACT.intValue());
		// Update all users which belongs this group to root group
		if (list.size() > 0) {
			ContactGroup defaultGroup = (ContactGroup) list.get(0);
			List<User> userList = group.getUsers();
			for (int i = 0; i < userList.size(); i++) {
				User user = userList.get(i);
				GroupRequest.getInstance().moveUserToGroup(
						group.getGroupType().intValue(), group.getmGId(),
						defaultGroup.getmGId(), user.getmUserId());
				group.removeUserFromGroup(user);
				defaultGroup.addUserToGroup(user);
				
			}
		}
		// Initialize time out message
		initTimeoutMessage(DELETE_CONTACTS_GROUP, DEFAULT_TIME_OUT_SECS, caller);
		GroupRequest.getInstance().delGroup(group.getGroupType().intValue(),
				group.getmGId());
	}

	/**
	 * Update User to specific group and remove user from origin group.<br>
	 * Add new contact if srcGroup is null.<br>
	 * Remove contact if desGroup is null.<br>
	 * 
	 * @param desGroup
	 *            if desGroup is null, remove contact from group
	 * @param srcGroup
	 *            if srcGroup is null, means add new contact to group
	 * @param user
	 * @param caller
	 */
	public void updateUserGroup(ContactGroup desGroup, ContactGroup srcGroup,
			User user, Registrant caller) {
		if (!checkParamNull(caller, new Object[] { user })) {
			return;
		}
		// If srcGroup is null, means add new contact
		if (srcGroup == null && desGroup != null) {
			StringBuffer attendees = new StringBuffer();
			attendees.append("<userlist> ");
			attendees.append(" <user id='" + user.getmUserId() + " ' />");
			attendees.append("</userlist>");
			GroupRequest.getInstance().inviteJoinGroup(
					GroupType.CONTACT.intValue(),
					"<friendgroup id =\"" + desGroup.getmGId() + "\" />",
					attendees.toString(), "");
			// remove contact
		} else if (desGroup == null && srcGroup != null) {
			GroupRequest.getInstance().delGroupUser(
					GroupType.CONTACT.intValue(), srcGroup.getmGId(),
					user.getmUserId());
			// move contact to other group
		} else {
			// Initialize time out message
			initTimeoutMessage(UPDATE_CONTACT_BELONGS_GROUP,
					DEFAULT_TIME_OUT_SECS, caller);
			GroupRequest.getInstance().moveUserToGroup(
					Group.GroupType.CONTACT.intValue(), srcGroup.getmGId(),
					desGroup.getmGId(), user.getmUserId());
			//Update cache
			srcGroup.removeUserFromGroup(user);
			desGroup.addUserToGroup(user);
		}
	}

	@Override
	public void clearCalledBack() {
		GroupRequest.getInstance().removeCallback(crCB);
	}

	class GroupRequestCB extends GroupRequestCallbackAdapter {

		private Handler mCallbackHandler;

		public GroupRequestCB(Handler callbackHandler) {
			mCallbackHandler = callbackHandler;
		}

		@Override
		public void OnModifyGroupInfoCallback(V2Group group) {
			if (group == null || group.type != Group.GroupType.CONTACT.intValue()) {
				return;
			}
			// If equals, means we are waiting for modified response
			if (group.id == mWatingGid) {
				JNIResponse jniRes = new GroupServiceJNIResponse(
						RequestConfCreateResponse.Result.SUCCESS);
				Message.obtain(mCallbackHandler, UPDATE_CONTACTS_GROUP, jniRes)
						.sendToTarget();
				mWatingGid = 0;
			}
		}

		@Override
		public void OnDelGroupCallback(int groupType, long nGroupID,
				boolean bMovetoRoot) {
			if (groupType != Group.GroupType.CONTACT.intValue()) {
				return;
			}

			// If equals, means we are waiting for modified response
			if (nGroupID == mWatingGid) {
				JNIResponse jniRes = new GroupServiceJNIResponse(
						RequestConfCreateResponse.Result.SUCCESS,
						new ContactGroup(nGroupID, null));
				Message.obtain(mCallbackHandler, DELETE_CONTACTS_GROUP, jniRes)
						.sendToTarget();
				mWatingGid = 0;
				GlobalHolder.getInstance().removeGroup(GroupType.CONTACT,
						nGroupID);
			}
			;
		}

		@Override
		public void OnAddGroupUserInfoCallback(int groupType, long nGroupID,
				String sXml) {
			if (groupType != Group.GroupType.CONTACT.intValue()) {
				return;
			}
			// 为什么要调，父类的不是什么都不执行的函数吗？
			super.OnAddGroupUserInfoCallback(groupType, nGroupID, sXml);
		}

		@Override
		public void onAddGroupInfo(V2Group group) {
			if (group.type == V2Group.TYPE_CONTACTS_GROUP) {
				Group g = new ContactGroup(group.id, group.name);
				GlobalHolder.getInstance().addGroupToList(
						g.getGroupType().intValue(), g);
				JNIResponse jniRes = new GroupServiceJNIResponse(
						GroupServiceJNIResponse.Result.SUCCESS, g);
				Message.obtain(mCallbackHandler, CREATE_CONTACTS_GROUP, jniRes)
						.sendToTarget();
			}

		}

		public void OnMoveUserToGroup(int groupType, V2Group srcGroup,
				V2Group desGroup, V2User u) {
			JNIResponse jniRes = new GroupServiceJNIResponse(
					GroupServiceJNIResponse.Result.SUCCESS);
			Message.obtain(mCallbackHandler, UPDATE_CONTACT_BELONGS_GROUP,
					jniRes).sendToTarget();
		}

		@Override
		public void OnAddGroupFile(V2Group group, List<FileJNIObject> list) {
			
		}
	}

}
