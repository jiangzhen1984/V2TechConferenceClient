package com.v2tech.service;

import java.util.Iterator;

import com.V2.jni.GroupRequest;
import com.v2tech.vo.Group;
import com.v2tech.vo.User;

/**
 * @deprecated should use ContactsService
 * @author jiangzhen
 *
 */
public class FriendGroupService extends AbstractHandler {

	public FriendGroupService() {
	}

	// 主调接口
	// 添加联系人

	public void AddFriendGroupUser(Group friendGroup, User user,
			String additInfo, String commentName) {

		String groupInfo = "<friendgroup" + " id='" + friendGroup.getmGId()
				+ "'/>";

		String userInfo = "<userlist>" + "<user id='" + user.getmUserId() + "'"
				+ " commentname='" + commentName + "'></user>" + "</userlist>";

		GroupRequest.getInstance().inviteJoinGroup(
				Group.GroupType.CONTACT.intValue(), groupInfo, userInfo,
				additInfo);
	}

	// 删除联系人
	public void delFriendGroupUser(User user) {
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

	public void acceptInviteJoinFriendGroup(long groupId, long nUserID) {
		GroupRequest.getInstance().acceptInviteJoinGroup(
				Group.GroupType.CONTACT.intValue(), groupId, nUserID);
	}

	public void refuseInviteJoinFriendGroup(long nGroupID, long nUserID,
			String reason) {
		GroupRequest.getInstance().refuseInviteJoinGroup(
				Group.GroupType.CONTACT.intValue(), nGroupID, nUserID, reason);
	}

	@Override
	public void clear() {
		//FIMXE remove callback from JNI
	}
	
	

	// 不用回调接口，在JNIService已经处理。

}
