package com.V2.jni;

import java.io.ByteArrayInputStream;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.xinlan.im.adapter.XiuLiuApplication;
import com.xinlan.im.bean.msgtype.LoginMsgType;
import com.xinlan.im.bean.msgtype.MoveGroupMsgType;
import com.xinlan.im.bean.msgtype.MsgType;
import com.xinlan.im.bean.msgtype.SearchMsgType;
import com.xinlan.im.bean.msgtype.UpdateMsgType;
import com.xinlan.im.bean.msgtype.UserStatusMsgType;
import com.xinlan.im.ui.SplashActivity;
import com.xinlan.im.ui.chat.bean.NYXUser;
import com.xinlan.im.ui.chat.db.DbHelper;
import com.xinlan.im.utils.Logger;
import com.xinlan.im.utils.XmlParserUtils;

public class ImRequest {
	private Activity context;
	public boolean loginResult;
	private static ImRequest mImRequest;
	private XiuLiuApplication application;
	private DbHelper dbHelper;
	private LoginMsgType loginMsgType;

	private ImRequest(Activity context) {
		this.context = context;
		application = (XiuLiuApplication) context.getApplication();
		dbHelper = DbHelper.getInstance(context);
	};

	public static synchronized ImRequest getInstance(Activity context) {
		if (mImRequest == null) {
			mImRequest = new ImRequest(context);
		}

		return mImRequest;
	}

	public native boolean initialize(ImRequest request);

	public native void unInitialize();

	public native void login(String szName, String szPassword, int status,
			int type);

	// 更新的我的在线状态
	public native void updateMyStatus(int nStatus, String szStatusDesc);

	// 修改好友备注名
	public native void modifyCommentName(long nUserId, String sCommentName);

	// 获得个人信息
	public native void getUserBaseInfo(long nUserID);

	// 修改个人信息
/*
 * 
 */
	public native void modifyBaseInfo(String InfoXml);

	// 更改系统头像
	public native void changeSystemAvatar(String szAvatarName);

	// 更改自定义头像
	public native void changeCustomAvatar(String szAvatar, int len,
			String szExtensionName);

	// 启动自动更新
	public native void onStartUpdate();

	// 停止自动更新
	public native void onStopUpdate();

	// 搜索用户信息
	public native void searchMember(String szUnsharpName, int nStartNum,
			int nSearchNum);

	// 搜索群
	public native void searchCrowd(String szUnsharpName, int nStartNum,
			int nSearchNum);

	// 删除群共享文件
	public native void delCrowdFile(long nCrowdId, String sFileID);

	public native void getCrowdFileInfo(long nCrowdId);

	/*
	 */
	private void OnLogin(long nUserID, int nStatus, int nResult) {
		Log.e("ImRequest UI", "登录第一次回调:" + nUserID + ":：：" + "；；；--:" + nStatus + ":"+ nResult);
		loginMsgType = new LoginMsgType();
		loginMsgType.setResult(nResult);
		loginMsgType.setStatus(nStatus);
		loginMsgType.setUserid(nUserID);
		
		//说明登陆失败
		if(nResult==1){
			Intent intent = new Intent(SplashActivity.IM);
			intent.putExtra("MsgType", MsgType.LOGIN);
			intent.putExtra("MSG", loginMsgType);
			context.sendOrderedBroadcast(intent,null);
		}
	}

	// 注销的方法
	private void OnLogout(int nUserID) {
		Log.e("ImRequest UI", "OnLogout::" + nUserID);
	}




	// 好友的个人信息修改后返回，修改什么，返回那个字段
	private void OnUpdateBaseInfo(long nUserID, String udatexml) {
		Log.e("ImRequest UI", "OnUpdateBaseInfo::" + nUserID + "  " + udatexml);
		// 拼装个人信息
		UpdateMsgType updateMsgType = new UpdateMsgType();
		updateMsgType.setUpdatexml(udatexml);
		updateMsgType.setUserid(nUserID);

		Intent addIntent = new Intent(SplashActivity.IM);
		addIntent.putExtra("MsgType", MsgType.UPDATEPERSON);
		addIntent.putExtra("MSG", updateMsgType);
		context.sendOrderedBroadcast(addIntent,null);
	}



	// 输入关键词进行搜索
	private void OnGetSearchMember(String xmlinfo) {
		Log.e("ImRequest UI", "OnGetSearchMember:" + xmlinfo);
		List<NYXUser> searchUsers = XmlParserUtils
				.parserSearchUsers(new ByteArrayInputStream(xmlinfo.getBytes()));
		// 拼装消息
		SearchMsgType searchMsgType = new SearchMsgType();
		searchMsgType.setSearchUsers(searchUsers);

		Intent addIntent = new Intent(SplashActivity.IM);
		addIntent.putExtra("MsgType", MsgType.SEARCH);
		addIntent.putExtra("MSG", searchMsgType);
		context.sendOrderedBroadcast(addIntent,null);
	}

	// nStatus只要不是0 就在线
	private void OnUserStatusUpdated(long nUserID, int nStatus,
			String szStatusDesc) {
		Log.e("ImRequest UI","用户状态更新" + nUserID + ":" + nStatus + ":" + szStatusDesc);

		// 拼装消息
		UserStatusMsgType statusMsgType = new UserStatusMsgType();
		statusMsgType.setUserid(nUserID);
		statusMsgType.setOnline(nStatus != 0 ? true : false);
		Intent addIntent = new Intent(SplashActivity.IM);
		addIntent.putExtra("MsgType", MsgType.USER_STATUS);
		addIntent.putExtra("MSG", statusMsgType);
		context.sendOrderedBroadcast(addIntent, null);
	}



	private void OnUserPrivacyUpdated(long nUserID, int nPrivacy) {
		Log.e("ImRequest UI", "OnUserPrivacyUpdated");
	}

	// 创建组成功后的回调
	private void OnCreateFriendGroup(long nGroupID, String szGroupName) {
		Log.e("ImRequest UI", "OnCreateFriendGroup：：" + nGroupID + ":"
				+ szGroupName);
	}


	// 修改组成功后的回调
	private void OnModifyFriendGroup(long nGroupID, String szGroupName) {
		Log.e("ImRequest UI", "OnModifyFriendGroup::" + nGroupID + ":"
				+ szGroupName);

	
	}


	// 受到好友成功移动组的回调
	private void OnMoveFriendsToGroup(long nDstUserID, long nDstGroupID) {
		Log.e("ImRequest UI", "OnMoveFriendsToGroup：" + nDstUserID + ":"
				+ nDstGroupID);

		// 拼装消息
		MoveGroupMsgType moveMsgtype = new MoveGroupMsgType();
		moveMsgtype.setnDstGroupID(nDstGroupID);
		moveMsgtype.setnDstUserID(nDstUserID);

		// 通过广播发送消息来通知,更新最近连续人画面
		Intent moveIntent = new Intent(SplashActivity.IM);
		moveIntent.putExtra("MsgType", MsgType.MOVE_GROUP);
		moveIntent.putExtra("MSG", moveMsgtype);
		context.sendOrderedBroadcast(moveIntent,null);
	}


	private void OnChangeAvatar(int nAvatarType, long nUserID, String AvatarName) {
		Log.e("ImRequest UI", "OnChangeAvatar");
	}

	private void OnHaveUpdateNotify(String updatefilepath, String updatetext) {
		Log.e("ImRequest UI", "OnHaveUpdateNotify");
	}

	private void OnServerFaild(String sModuleName) {
		Log.e("ImRequest UI", "OnServerFaild");
	}

	private void OnConnectResponse(int nResult) {
		Log.e("ImRequest UI", "OnConnectResponse::" + nResult);
		//连接服务器失败
		if(nResult==301){
			Intent intent = new Intent(SplashActivity.IM);
			intent.putExtra("MsgType", MsgType.SERVER);
			context.sendOrderedBroadcast(intent,null);
		}
		
		
	}

	private void OnUpdateDownloadBegin(long filesize) {
		Log.e("ImRequest UI", "OnUpdateDownloadBegin::" + filesize);
	}

	private void OnUpdateDownloading(long size) {
		Log.e("ImRequest UI", "OnUpdateDownloading::" + size);
	}

	private void OnUpdateDownloadEnd(boolean error)

	{
		Log.e("ImRequest UI", "OnUpdateDownloadEnd:" + error);
	}

	private void OnCreateCrowd(String sCrowdXml, int nResult) {
		Log.e("ImRequest UI", "OnCreateCrowd  " + "sCrowdXml:" + sCrowdXml
				+ "  nResult:" + nResult);
	}


	// 被移出群
	private void OnKickCrowd(long nCrowdId, long nAdminId) {
		Log.e("ImRequest UI", "OnKickCrowd:" + nCrowdId);
	}


	// 搜索群回调
	private void OnSearchCrowd(String InfoXml) {
		Log.e("ImRequest UI", "OnSearchCrowd::" + InfoXml);
	}


	// 群共享回调
	private void Oncrowdfile(long nCrowdId, String InfoXml) {
		Log.e("ImRequest UI", "Oncrowdfile:" + nCrowdId);
	}

	// 获取群共享信息
	private void OnGetCrowdFileInfo(long nCrowdId, String InfoXml) {
		Log.e("ImRequest UI", "OnGetCrowdFileInfo:" + nCrowdId);
	}

	// 删除群共享文件回调
	private void OnDelCrowdFile(long nCrowdId, String sFileID) {
		Log.e("ImRequest UI", "OnDelCrowdFile:" + nCrowdId);
	}


	// 修改备注姓名
	private void OnModifyCommentName(long nUserId, String sCommmentName) {
		Log.e("ImRequest UI", "OnModifyCommentName::" + "nUserId:" + nUserId
				+ "  sCommmentName" + sCommmentName);
	}

	private void OnSignalDisconnected() {
		Log.e("ImRequest UI", "OnSignalDisconnected");
	}

	private void OnDelGroupInfo(int type, long groupid, boolean isdel) {
		Log.e("ImRequest UI", "OnDelGroupInfo：" + type + ":" + groupid + ":"
				+ isdel);
	}

	// 标志着获取用户开始
	private void OnGetGroupsInfoBegin() {
		Log.e("ImRequest UI", "开始获取组OnGetGroupsInfoBegin");
	}

	// 标志着获取用户结束
	
	private boolean haslogin=false; //标志已经发送了登陆状态
	private void OnGetGroupsInfoEnd() {
		Log.e("ImRequest UI", "OnGetGroupsInfoEnd");

		// 登陆完成，并且获得用户列表后再开始进入主界面
		Intent intent = new Intent(SplashActivity.IM);
		intent.putExtra("MsgType", MsgType.LOGIN);
		intent.putExtra("MSG", loginMsgType);
		
		if(!haslogin){
			context.sendOrderedBroadcast(intent,null);
			haslogin=true;
		}
	}


}
