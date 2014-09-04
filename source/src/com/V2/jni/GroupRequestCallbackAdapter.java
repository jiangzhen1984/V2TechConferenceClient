package com.V2.jni;

import java.util.List;

import com.V2.jni.ind.V2Group;
import com.V2.jni.ind.V2User;

public abstract class GroupRequestCallbackAdapter implements
		GroupRequestCallback {

	@Override
	public void OnGetGroupInfoCallback(int groupType, List<V2Group> list) {

	}

	@Override
	public void OnGetGroupUserInfoCallback(int groupType, long nGroupID,
			String sXml) {

	}

	@Override
	public void OnModifyGroupInfoCallback(int groupType, long nGroupID,
			String sXml) {

	}

	@Override
	public void OnInviteJoinGroupCallback(int groupType, String groupInfo,
			String userInfo, String additInfo) {

	}

	@Override
	public void OnDelGroupCallback(int groupType, long nGroupID,
			boolean bMovetoRoot) {

	}

	@Override
	public void OnDelGroupUserCallback(int groupType, long nGroupID,
			long nUserID) {

	}

	@Override
	public void OnAddGroupUserInfoCallback(int groupType, long nGroupID,
			String sXml) {

	}

	@Override
	public void onAddGroupInfo(V2Group group) {
		
	}

	@Override
	public void OnMoveUserToGroup(int groupType, V2Group srcGroup,
			V2Group desGroup, V2User u) {
		
	}
	
	
	
	

}
