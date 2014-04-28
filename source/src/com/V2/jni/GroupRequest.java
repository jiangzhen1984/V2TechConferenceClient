package com.V2.jni;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;
import android.util.Log;

import com.v2tech.util.V2Log;

public class GroupRequest {

	public boolean loginResult;
	private static GroupRequest mGroupRequest;

	private List<WeakReference<GroupRequestCallback>> callbacks;

	private GroupRequest(Context context) {
		callbacks = new CopyOnWriteArrayList<WeakReference<GroupRequestCallback>>();
	};

	public static synchronized GroupRequest getInstance(Context context) {
		if (mGroupRequest == null) {
			mGroupRequest = new GroupRequest(context);
			if (!mGroupRequest.initialize(mGroupRequest)) {
				throw new RuntimeException(
						" can't not inintialize group request");
			}
		}
		return mGroupRequest;
	}

	public static synchronized GroupRequest getInstance() {
		if (mGroupRequest == null) {
			mGroupRequest = new GroupRequest(null);
			if (!mGroupRequest.initialize(mGroupRequest)) {
				throw new RuntimeException(
						" can't not inintialize group request");
			}
		}
		return mGroupRequest;
	}

	public native boolean initialize(GroupRequest request);

	public native void unInitialize();

	// ɾ��һ�����ѷ���
	public native void delGroup(int groupType, long nGroupID);

	// �뿪һ�����ѷ���
	public native void leaveGroup(int groupType, long nGroupID);

	// ɾ��һ�����ѷ���
	public native void delGroupUser(int groupType, long nGroupID, long nUserID);

	// �޸ĺ��ѷ���
	public native void modifyGroupInfo(int groupType, long nGroupID, String sXml);

	/**
	 * // sXmlConfData : // <conf canaudio="1" candataop="1" canvideo="1"
	 * conftype="0" haskey="0" // id="0" key="" // layout="1" lockchat="0"
	 * lockconf="0" lockfiletrans="0" mode="2" // pollingvideo="0" //
	 * subject="ss" syncdesktop="0" syncdocument="1" syncvideo="0" //
	 * chairuserid='0' chairnickname=''> // </conf> // szInviteUsers : // <xml>
	 * // <user id="11760" nickname=""/> // <user id="11762" nickname=""/> //
	 * </xml>
	 * 
	 * @param groupType
	 * @param groupInfo
	 * @param userInfo
	 */
	public native void createGroup(int groupType, String groupInfo,
			String userInfo);

	// ������˼�����
	/*
	 */
	public native void inviteJoinGroup(int groupType, String groupInfo,
			String userInfo, String additInfo);

	// �ƶ����ѵ������
	public native void moveUserToGroup(int groupType, long srcGroupID,
			long dstGroupID, long nUserID);

	public native void getGroupInfo(int type, long groupId);

	/**********************************************/

	// �ܾ�����������Ⱥ
	public native void refuseInviteJoinGroup(int groupType, long nGroupID,
			long nUserID, String reason);

	// �������Ⱥ
	public native void applyJoinGroup(int groupType, String sGroupInfo,
			String sAdditInfo);

	// ���ܼ���Ⱥ
	public native void acceptApplyJoinGroup(int groupType, String sGroupInfo,
			long nUserID);

	public native void acceptInviteJoinGroup(int groupType, long t, long nUserID);

	public void OnAcceptInviteJoinGroup(int groupType, long t, long nUserID) {

	}

	public void OnConfSyncOpenVideo(String str) {

	}

	// �ܾ����Ⱥ
	public native void refuseApplyJoinGroup(int groupType, String sGroupInfo,
			long nUserID, String sReason);

	/**
	 * This is unsolicited callback. This function will be call after log in
	 * 
	 * @param groupType
	 *            4 : conference
	 * @param sXml
	 */
	private void OnGetGroupInfo(int groupType, String sXml) {
		for (WeakReference<GroupRequestCallback> wrcb : callbacks) {
			Object obj = wrcb.get();
			if (obj != null) {
				GroupRequestCallback callback = (GroupRequestCallback) obj;
				callback.OnGetGroupInfoCallback(groupType, sXml);
			}
		}
		V2Log.d("OnGetGroupInfo::" + groupType + ":" + sXml);

	}

	public void addCallback(GroupRequestCallback callback) {
		this.callbacks.add(new WeakReference<GroupRequestCallback>(callback));
	}

	private void OnGetGroupUserInfo(int groupType, long nGroupID, String sXml) {
		V2Log.d("OnGetGroupUserInfo -> " + groupType + ":" + nGroupID + ":"
				+ sXml);
		for (WeakReference<GroupRequestCallback> wrcb : callbacks) {
			Object obj = wrcb.get();
			if (obj != null) {
				GroupRequestCallback callback = (GroupRequestCallback) obj;
				callback.OnGetGroupUserInfoCallback(groupType, nGroupID, sXml);
			}
		}

	}

	private void OnAddGroupUserInfo(int groupType, long nGroupID, String sXml) {
		Log.e("ImRequest UI", "OnAddGroupUserInfo::��Ӻ��ѵ������Ϣ" + groupType
				+ ":" + nGroupID + ":" + sXml);

		// List<NYXUser> addUsers = XmlParserUtils
		// .parserNYXUser(new
		// ByteArrayInputStream(sXml.getBytes()),application.getLocalUser().getId());
		//
		// // ƴװ��Ϣ
		// AddFriMsgType addFriMsgType = new AddFriMsgType();
		//
		// addFriMsgType.setAddUsers(addUsers);
		// addFriMsgType.setGroupid(nGroupID);
		//
		// Intent addIntent = new Intent(SplashActivity.IM);
		// addIntent.putExtra("MsgType", MsgType.ADDFRI);
		// addIntent.putExtra("MSG", addFriMsgType);
		// context.sendOrderedBroadcast(addIntent,null);
	}

	private void OnDelGroupUser(int groupType, long nGroupID, long nUserID) {
		Log.e("ImRequest UI", "OnDelGroupUser:: ������ɾ�����" + groupType + ":"
				+ nGroupID + ":" + nUserID);
		// ƴװ��Ϣ
		// DelFriMsgType delFri = new DelFriMsgType();
		// delFri.setUserid(nUserID);
		//
		// // ͨ��㲥������Ϣ��֪ͨ,������������˻���
		// Intent delIntent = new Intent(SplashActivity.IM);
		// delIntent.putExtra("MsgType", MsgType.DELFRI);
		// delIntent.putExtra("MSG", delFri);
		// context.sendOrderedBroadcast(delIntent,null);
	}

	private void OnAddGroupInfo(int groupType, long nParentID, long nGroupID,
			String sXml) {
		Log.e("ImRequest UI", "OnAddGroupInfo:: ����һ����" + groupType + ":"
				+ nParentID + ":" + nGroupID + ":" + sXml);

		// <friendgroup id='312' name='byhy'/>
		// InputStream in=new ByteArrayInputStream(sXml.getBytes());
		// Group group=XmlParserUtils.parserAddGroup(in);
		//
		// // ƴװ��Ϣ
		// CreateGroupMsgType createMsgType = new CreateGroupMsgType();
		// createMsgType.setmGroup(group);
		//
		// Intent createIntent = new Intent(SplashActivity.IM);
		// createIntent.putExtra("MsgType", MsgType.CREATE_GROUP);
		// createIntent.putExtra("MSG", createMsgType);
		// context.sendOrderedBroadcast(createIntent,null);
	}

	/**
	 * TODO to be implement comment
	 * 
	 * @param groupType
	 * @param nGroupID
	 * @param sXml
	 */
	private void OnModifyGroupInfo(int groupType, long nGroupID, String sXml) {
		V2Log.d("OnModifyGroupInfo::-->" + groupType + ":" + nGroupID + ":"
				+ sXml);
		for (WeakReference<GroupRequestCallback> wrcb : callbacks) {
			Object obj = wrcb.get();
			if (obj != null) {
				GroupRequestCallback callback = (GroupRequestCallback) obj;
				callback.OnModifyGroupInfoCallback(groupType, nGroupID, sXml);
			}
		}

	}

	/**
	 * TODO add implement comment
	 * 
	 * @param groupType
	 * @param nGroupID
	 * @param bMovetoRoot
	 */
	private void OnDelGroup(int groupType, long nGroupID, boolean bMovetoRoot) {
		V2Log.d("OnDelGroup::==>" + groupType + ":" + nGroupID + ":"
				+ bMovetoRoot);
		for (WeakReference<GroupRequestCallback> wrcb : callbacks) {
			Object obj = wrcb.get();
			if (obj != null) {
				GroupRequestCallback callback = (GroupRequestCallback) obj;
				callback.OnDelGroupCallback(groupType, nGroupID, bMovetoRoot);
			}
		}
	}

	private void OnInviteJoinGroup(int groupType, String groupInfo,
			String userInfo, String additInfo) {
		V2Log.d("OnInviteJoinGroup::==>" + groupType + ":" + groupInfo + ":"
				+ userInfo + ":" + additInfo);
		for (WeakReference<GroupRequestCallback> wrcb : callbacks) {
			Object obj = wrcb.get();
			if (obj != null) {
				GroupRequestCallback callback = (GroupRequestCallback) obj;
				callback.OnInviteJoinGroupCallback(groupType, groupInfo,
						userInfo, additInfo);
			}
		}

	}

	private void OnMoveUserToGroup(int groupType, long srcGroupID,
			long dstGroupID, long nUserID) {
		Log.e("ImRequest UI", "OnMoveUserToGroup:: �ƶ����ѵ�ʲô��" + groupType
				+ ":" + srcGroupID + ":" + dstGroupID + ":" + nUserID);

		// ƴװ��Ϣ
		// MoveGroupMsgType moveMsgtype = new MoveGroupMsgType();
		// moveMsgtype.setnDstGroupID(dstGroupID);
		// moveMsgtype.setnDstUserID(dstGroupID);
		//
		// // ͨ��㲥������Ϣ��֪ͨ,������������˻���
		// Intent moveIntent = new Intent(SplashActivity.IM);
		// moveIntent.putExtra("MsgType", MsgType.MOVE_GROUP);
		// moveIntent.putExtra("MSG", moveMsgtype);
		// context.sendOrderedBroadcast(moveIntent,null);
	}

	private void OnRefuseInviteJoinGroup(int groupType, long nGroupID,
			long nUserID, String sxml) {
		Log.e("ImRequest UI", "OnRefuseInviteJoinGroup:: �ܾ���˵��������ʲô��"
				+ groupType + ":" + nGroupID + ":" + nUserID + ":" + sxml);

		// // ƴװ������Ϣ
		// RefuseMsgType refuseMsgType = new RefuseMsgType();
		// refuseMsgType.setReason(sxml);
		// refuseMsgType.setUserBaseInfo(sxml);
		//
		// Intent addIntent = new Intent(SplashActivity.IM);
		// addIntent.putExtra("MsgType", MsgType.REFUSE_ADD);
		// addIntent.putExtra("MSG", refuseMsgType);
		// context.sendOrderedBroadcast(addIntent,null);
	}

	private void OnApplyJoinGroup(int groupType, long nGroupID,
			String userInfo, String reason) {
		Log.e("ImRequest UI", "OnApplyJoinGroup:: ���������" + groupType + ":"
				+ nGroupID + ":" + userInfo + ":" + reason);
	}

	private void OnAcceptApplyJoinGroup(int groupType, String sXml) {
		Log.e("ImRequest UI", "OnAcceptApplyJoinGroup:: �������������"
				+ groupType + ":" + sXml);
	}

	private void OnRefuseApplyJoinGroup(int groupType, String sGroupInfo,
			String reason) {
		Log.e("ImRequest UI", "OnRefuseApplyJoinGroup:: �ܾ����������"
				+ groupType + ":" + sGroupInfo + ":" + reason);
	}

}
