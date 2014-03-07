package com.V2.jni;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.xinlan.im.adapter.XiuLiuApplication;
import com.xinlan.im.bean.msgtype.AddFriMsgType;
import com.xinlan.im.bean.msgtype.CreateGroupMsgType;
import com.xinlan.im.bean.msgtype.DelFriMsgType;
import com.xinlan.im.bean.msgtype.DestoryGroupMsgType;
import com.xinlan.im.bean.msgtype.FriendMsgType;
import com.xinlan.im.bean.msgtype.GroupMsgType;
import com.xinlan.im.bean.msgtype.LoginMsgType;
import com.xinlan.im.bean.msgtype.ModifyGroupMsgType;
import com.xinlan.im.bean.msgtype.MoveGroupMsgType;
import com.xinlan.im.bean.msgtype.MsgType;
import com.xinlan.im.bean.msgtype.RefuseMsgType;
import com.xinlan.im.ui.SplashActivity;
import com.xinlan.im.ui.chat.bean.Group;
import com.xinlan.im.ui.chat.bean.NYXUser;
import com.xinlan.im.ui.chat.db.DbHelper;
import com.xinlan.im.utils.XmlParserUtils;

public class GroupRequest {

	private Activity context;
	public boolean loginResult;
	private static GroupRequest mGroupRequest;
	private XiuLiuApplication application;
	private DbHelper dbHelper;
	private LoginMsgType loginMsgType;

	private GroupRequest(Activity context) {
		this.context = context;
		application = (XiuLiuApplication) context.getApplication();
		dbHelper = DbHelper.getInstance(context);
	};

	public static synchronized GroupRequest getInstance(Activity context) {
		if (mGroupRequest == null) {
			mGroupRequest = new GroupRequest(context);
		}
		return mGroupRequest;
	}

	public native boolean initialize(GroupRequest request);

	public native void unInitialize();

	// 删除一个好友分组
	public native void delGroup(int groupType, long nGroupID);

	// 离开一个好友分组
	public native void leaveGroup(int groupType, long nGroupID);

	// 删除一个好友分组
	public native void delGroupUser(int groupType, long nGroupID,
			long nUserID);

	// 修改好友分组
	public native void modifyGroupInfo(int groupType, long nGroupID,
			String sXml);

	// 创建一个分组
	public native void createGroup(int groupType, String groupInfo,
			String userInfo);

	// 邀请别人加入组
	/*
	 */
	public native void inviteJoinGroup(int groupType, String groupInfo,
			String userInfo, String additInfo);

	// 移动好友到别的组
	public native void moveUserToGroup(int groupType, long srcGroupID,
			long dstGroupID, long nUserID);
	
	public native void getGroupInfo(int type,long userid);
	
	/**********************************************/

	// 拒绝好友邀请加如群
	public native void refuseInviteJoinGroup(int groupType, long nGroupID,
			long nUserID, String reason);

	// 申请加入群
	public native void applyJoinGroup(int groupType, String sGroupInfo,
			String sAdditInfo);

	// 接受加入群
	public native void acceptApplyJoinGroup(int groupType,
			String sGroupInfo, long nUserID);

	// 拒绝加入群
	public native void refuseApplyJoinGroup(int groupType,
			String sGroupInfo, long nUserID, String sReason);

	private void OnGetGroupInfo(int groupType, String sXml) {
		Log.e("ImRequest UI", "OnGetGroupInfo:: 得到好友组信息" + groupType + ":"
				+ sXml);
		System.out.println("得到好友组");
		// 拼装信息
		GroupMsgType friendMsgType = new GroupMsgType();
		friendMsgType.setGroupxml(sXml);

		Intent addIntent = new Intent(SplashActivity.IM);
		addIntent.putExtra("MsgType", MsgType.FRIENDGROUP);
		addIntent.putExtra("MSG", friendMsgType);
		context.sendOrderedBroadcast(addIntent,null);
	}

	private void OnGetGroupUserInfo(int groupType, long nGroupID, String sXml) {
		Log.e("ImRequest UI", "OnGetGroupUserInfo:: 得到组好友的详细信息" + groupType
				+ ":" + nGroupID + ":" + sXml);
		System.out.println("得到好友信息");

		// 拼装信息
		FriendMsgType friendMsgType = new FriendMsgType();
		friendMsgType.setFriendxml(sXml);
		friendMsgType.setGroupid(nGroupID);

		Intent addIntent = new Intent(SplashActivity.IM);
		addIntent.putExtra("MsgType", MsgType.FRIENDLIST);
		addIntent.putExtra("MSG", friendMsgType);
		context.sendOrderedBroadcast(addIntent,null);

	}

	private void OnAddGroupUserInfo(int groupType, long nGroupID, String sXml) {
		Log.e("ImRequest UI", "OnAddGroupUserInfo::添加好友到组的信息" + groupType + ":"
				+ nGroupID + ":" + sXml);
		
		List<NYXUser> addUsers = XmlParserUtils
				.parserNYXUser(new ByteArrayInputStream(sXml.getBytes()),application.getLocalUser().getId());

		// 拼装信息
		AddFriMsgType addFriMsgType = new AddFriMsgType();

		addFriMsgType.setAddUsers(addUsers);
		addFriMsgType.setGroupid(nGroupID);

		Intent addIntent = new Intent(SplashActivity.IM);
		addIntent.putExtra("MsgType", MsgType.ADDFRI);
		addIntent.putExtra("MSG", addFriMsgType);
		context.sendOrderedBroadcast(addIntent,null);
	}

	private void OnDelGroupUser(int groupType, long nGroupID, long nUserID) {
		Log.e("ImRequest UI", "OnDelGroupUser:: 从组中删除好友" + groupType + ":"
				+ nGroupID + ":" + nUserID);
		// 拼装消息
		DelFriMsgType delFri = new DelFriMsgType();
		delFri.setUserid(nUserID);

		// 通过广播发送消息来通知,更新最近连续人画面
		Intent delIntent = new Intent(SplashActivity.IM);
		delIntent.putExtra("MsgType", MsgType.DELFRI);
		delIntent.putExtra("MSG", delFri);
		context.sendOrderedBroadcast(delIntent,null);
	}

	private void OnAddGroupInfo(int groupType, long nParentID, long nGroupID,
			String sXml) {
		Log.e("ImRequest UI", "OnAddGroupInfo:: 增加一个组" + groupType + ":"
				+ nParentID + ":" + nGroupID + ":" + sXml);
		
		// <friendgroup id='312' name='byhy'/>
		InputStream in=new ByteArrayInputStream(sXml.getBytes());
		Group group=XmlParserUtils.parserAddGroup(in);
		
		// 拼装信息
		CreateGroupMsgType createMsgType = new CreateGroupMsgType();
		createMsgType.setmGroup(group);

		Intent createIntent = new Intent(SplashActivity.IM);
		createIntent.putExtra("MsgType", MsgType.CREATE_GROUP);
		createIntent.putExtra("MSG", createMsgType);
		context.sendOrderedBroadcast(createIntent,null);
	}

	private void OnModifyGroupInfo(int groupType, long nGroupID, String sXml) {
		Log.e("ImRequest UI", "OnModifyGroupInfo::修改一个组信息" + groupType + ":"
				+ nGroupID + ":" + sXml);

		InputStream in=new ByteArrayInputStream(sXml.getBytes());
		Group group=XmlParserUtils.parserAddGroup(in);
		
		// 拼装信息
		ModifyGroupMsgType modifyMsgType = new ModifyGroupMsgType();
		modifyMsgType.setMgGroup(group);
		
		Intent modifyIntent = new Intent(SplashActivity.IM);
		modifyIntent.putExtra("MsgType", MsgType.MODIFY_GROUP);
		modifyIntent.putExtra("MSG", modifyMsgType);
		context.sendOrderedBroadcast(modifyIntent,null);
	}

	private void OnDelGroup(int groupType, long nGroupID, boolean bMovetoRoot) {
		Log.e("ImRequest UI", "OnDelGroup::删除一个组" + groupType + ":" + nGroupID
				+ ":" + bMovetoRoot);

		// 拼装信息
		DestoryGroupMsgType destoryMsgType = new DestoryGroupMsgType();
		destoryMsgType.setnGroupID(nGroupID);

		Intent destoryIntent = new Intent(SplashActivity.IM);
		destoryIntent.putExtra("MsgType", MsgType.DESTORY_GROUP);
		destoryIntent.putExtra("MSG", destoryMsgType);
		context.sendOrderedBroadcast(destoryIntent,null);
	}

	private void OnInviteJoinGroup(int groupType, String groupInfo,
			String userInfo, String additInfo) {
		Log.e("ImRequest UI", "OnInviteJoinGroup::邀请好友加入组" + groupType + ":"
				+ groupInfo + ":" + userInfo + ":" + additInfo);
	}

	private void OnMoveUserToGroup(int groupType, long srcGroupID,
			long dstGroupID, long nUserID) {
		Log.e("ImRequest UI", "OnMoveUserToGroup:: 移动好友到什么组" + groupType + ":"
				+ srcGroupID + ":" + dstGroupID + ":" + nUserID);

		// 拼装消息
		MoveGroupMsgType moveMsgtype = new MoveGroupMsgType();
		moveMsgtype.setnDstGroupID(dstGroupID);
		moveMsgtype.setnDstUserID(dstGroupID);

		// 通过广播发送消息来通知,更新最近连续人画面
		Intent moveIntent = new Intent(SplashActivity.IM);
		moveIntent.putExtra("MsgType", MsgType.MOVE_GROUP);
		moveIntent.putExtra("MSG", moveMsgtype);
		context.sendOrderedBroadcast(moveIntent,null);
	}

	private void OnRefuseInviteJoinGroup(int groupType, long nGroupID,
			long nUserID, String sxml) {
		Log.e("ImRequest UI", "OnRefuseInviteJoinGroup:: 拒绝别人的邀请加入什么组"
				+ groupType + ":" + nGroupID + ":" + nUserID + ":" + sxml);
		
		// 拼装个人信息
				RefuseMsgType refuseMsgType = new RefuseMsgType();
				refuseMsgType.setReason(sxml);
				refuseMsgType.setUserBaseInfo(sxml);

				Intent addIntent = new Intent(SplashActivity.IM);
				addIntent.putExtra("MsgType", MsgType.REFUSE_ADD);
				addIntent.putExtra("MSG", refuseMsgType);
				context.sendOrderedBroadcast(addIntent,null);
	}

	private void OnApplyJoinGroup(int groupType, long nGroupID,
			String userInfo, String reason) {
		Log.e("ImRequest UI", "OnApplyJoinGroup:: 申请加入组" + groupType + ":"
				+ nGroupID + ":" + userInfo + ":" + reason);
	}

	private void OnAcceptApplyJoinGroup(int groupType, String sXml) {
		Log.e("ImRequest UI", "OnAcceptApplyJoinGroup:: 接受申请加入组" + groupType
				+ ":" + sXml);
	}

	private void OnRefuseApplyJoinGroup(int groupType, String sGroupInfo,
			String reason) {
		Log.e("ImRequest UI", "OnRefuseApplyJoinGroup:: 拒绝申请加入组" + groupType
				+ ":" + sGroupInfo + ":" + reason);
	}

}
