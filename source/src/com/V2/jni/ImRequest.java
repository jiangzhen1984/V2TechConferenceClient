package com.V2.jni;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.util.Log;

import com.v2tech.util.V2Log;



public class ImRequest {
	private Context context;
	public boolean loginResult;
	private static ImRequest mImRequest;
	
	private List<ImRequestCallback> callbacks;




	private ImRequest(Context context) {
		this.context = context;
		callbacks = new ArrayList<ImRequestCallback>();
	};

	public static synchronized ImRequest getInstance(Context context) {
		if (mImRequest == null) {
			mImRequest = new ImRequest(context);
			if (!mImRequest.initialize(mImRequest)) {
				V2Log.e(" can't  initialize imrequest ");
				throw new RuntimeException("can't initilaize imrequest");
			}
		}

		return mImRequest;
	}

	public static synchronized ImRequest getInstance() {
		if (mImRequest == null) {
			throw new RuntimeException(
					"doesn't initliaze ImRequest yet, please getInstance(Context) first");
		}

		return mImRequest;
	}
	
	public void setCallback(ImRequestCallback callback) {
		this.callbacks.add(callback);
	}


	public native boolean initialize(ImRequest request);

	public native void unInitialize();
	
	/**
	 * <ul> Log in to server. server will call {@link #OnLogin(long, int, int)} to indicate response</ul>
	 * @param szName   user name 
	 * @param szPassword  password
	 * @param status  TODO add comment
	 * @param type  TODO add  comment
	 * 
	 * @see #OnLogin(long, int, int)
	 */
	public native void login(String szName, String szPassword, int status,
			int type);
	
	/**
	 * <ul> Log in call back function. This function only is called by JNI.</ul>
	 * @param nUserID logged in user ID
	 * @param nStatus 
	 * @param nResult  0: logged in successfully
	 * 
	 * @see #login(String, String, int, int)
	 */
	private void OnLogin(long nUserID, int nStatus, int nResult) {
		V2Log.d( "OnLogin --> " + nUserID + ": " + "-:"
				+ nStatus + ":" + nResult);
		for (ImRequestCallback callback : this.callbacks) {
			callback.OnLoginCallback(nUserID, nStatus, nResult);
		}
	}
	

	/**
	 * <ul>Get user information from server.<br>
	 * when call this API, JNI will call {@link #OnUpdateBaseInfo(long, String)} to indicate response.<br>
	 * </ul>
	 * 
	 * @param nUserID user ID which want to get user information
	 */
	public native void getUserBaseInfo(long nUserID);
	
	/**
	 * <ul> call back function. This function only is called by JNI.</ul>
	 * <ul>
	 * {@link #getUserBaseInfo(long)} callback.
	 * </ul>
	 * @param nUserID
	 * @param updatexml
	 * 
	 */
	private void OnUpdateBaseInfo(long nUserID, String updatexml) {
		for (ImRequestCallback callback : this.callbacks) {
			callback.OnUpdateBaseInfoCallback(nUserID, updatexml);
		}
	}
	
	
	/**
	 * <ul>Indicate user's status changed.</ul>
	 * @param nUserID user ID
	 * @param eUEType device type of user logged in
	 * @param nStatus <ul> new status of user</ul>
	 * <ul>
	 * <li>0 : off line</li>
	 * <li>1 : on line</li>
	 * <li>2 : leave</li>
	 * <li>3 : busy</li>
	 * <li>4 : do not disturb</li>
	 * <li>5 : hidden</li>
	 * </ul> 
	 * @param szStatusDesc
	 * 
	 * @see com.v2tech.logic.User.Status
	 * @see ImRequestCallback#OnUserStatusUpdatedCallback(long, int, int, String)
	 */
	private void OnUserStatusUpdated(long nUserID, int eUEType,  int nStatus, String szStatusDesc) {
		V2Log.d(" OnUserStatusUpdated--> nUserID:"+nUserID+" eUEType: "+ eUEType+"  nStatus:"+nStatus+"  szStatusDesc:"+szStatusDesc+"  "+new Date());
		for (ImRequestCallback callback : this.callbacks) {
			callback.OnUserStatusUpdatedCallback(nUserID, eUEType, nStatus, szStatusDesc);
		}
	}
	
	/**
	 *  <ul>Indicate user avatar changed.</ul>
	 * @param nAvatarType
	 * @param nUserID  User ID which user's changed avatar
	 * @param AvatarName  patch of avatar
	 * 
	 * @see ImRequestCallback#OnChangeAvatarCallback(int, long, String)
	 */
	private void OnChangeAvatar(int nAvatarType, long nUserID, String AvatarName) {
		V2Log.d("OnChangeAvatar--> nAvatarType:"+nAvatarType+"    nUserID:"+nUserID+" AvatarName:"+ AvatarName);
		for (ImRequestCallback callback : this.callbacks) {
			callback.OnChangeAvatarCallback(nAvatarType, nUserID, AvatarName);
		}
	}
	

	public native void updateMyStatus(int nStatus, String szStatusDesc);

	public native void modifyCommentName(long nUserId, String sCommentName);


	// 淇敼涓汉淇℃伅
	/*
 * 
 */
	public native void modifyBaseInfo(String InfoXml);

	// 鏇存敼绯荤粺澶村儚
	public native void changeSystemAvatar(String szAvatarName);

	// 鏇存敼鑷畾涔夊ご鍍�
	public native void changeCustomAvatar(String szAvatar, int len,
			String szExtensionName);

	// 鍚姩鑷姩鏇存柊
	public native void onStartUpdate();

	// 鍋滄鑷姩鏇存柊
	public native void onStopUpdate();

	// 鎼滅储鐢ㄦ埛淇℃伅
	public native void searchMember(String szUnsharpName, int nStartNum,
			int nSearchNum);

	// 鎼滅储缇�
	public native void searchCrowd(String szUnsharpName, int nStartNum,
			int nSearchNum);

	// 鍒犻櫎缇ゅ叡浜枃浠�
	public native void delCrowdFile(long nCrowdId, String sFileID);

	public native void getCrowdFileInfo(long nCrowdId);

	

	// 娉ㄩ攢鐨勬柟娉�
	private void OnLogout(int nUserID) {
		Log.e("ImRequest UI", "OnLogout::" + nUserID);
	}



	// 杈撳叆鍏抽敭璇嶈繘琛屾悳绱�
	private void OnGetSearchMember(String xmlinfo) {
		Log.e("ImRequest UI", "OnGetSearchMember:" + xmlinfo);
		// List<NYXUser> searchUsers = XmlParserUtils
		// .parserSearchUsers(new ByteArrayInputStream(xmlinfo.getBytes()));
		// // 鎷艰娑堟伅
		// SearchMsgType searchMsgType = new SearchMsgType();
		// searchMsgType.setSearchUsers(searchUsers);
		//
		// Intent addIntent = new Intent(SplashActivity.IM);
		// addIntent.putExtra("MsgType", MsgType.SEARCH);
		// addIntent.putExtra("MSG", searchMsgType);
		// context.sendOrderedBroadcast(addIntent,null);
	}


	

	private void OnUserPrivacyUpdated(long nUserID, int nPrivacy) {
		Log.e("ImRequest UI", "OnUserPrivacyUpdated");
	}

	// 鍒涘缓缁勬垚鍔熷悗鐨勫洖璋�
	private void OnCreateFriendGroup(long nGroupID, String szGroupName) {
		Log.e("ImRequest UI", "OnCreateFriendGroup锛氾細" + nGroupID + ":"
				+ szGroupName);
	}

	// 淇敼缁勬垚鍔熷悗鐨勫洖璋�
	private void OnModifyFriendGroup(long nGroupID, String szGroupName) {
		Log.e("ImRequest UI", "OnModifyFriendGroup::" + nGroupID + ":"
				+ szGroupName);

	}

	// 鍙楀埌濂藉弸鎴愬姛绉诲姩缁勭殑鍥炶皟
	private void OnMoveFriendsToGroup(long nDstUserID, long nDstGroupID) {
		Log.e("ImRequest UI", "OnMoveFriendsToGroup" + nDstUserID + ":"
				+ nDstGroupID);

		// // 鎷艰娑堟伅
		// MoveGroupMsgType moveMsgtype = new MoveGroupMsgType();
		// moveMsgtype.setnDstGroupID(nDstGroupID);
		// moveMsgtype.setnDstUserID(nDstUserID);
		//
		// // 閫氳繃骞挎挱鍙戦�娑堟伅鏉ラ�鐭�鏇存柊鏈�繎杩炵画浜虹敾闈�
		// Intent moveIntent = new Intent(SplashActivity.IM);
		// moveIntent.putExtra("MsgType", MsgType.MOVE_GROUP);
		// moveIntent.putExtra("MSG", moveMsgtype);
		// context.sendOrderedBroadcast(moveIntent,null);
	}

	

	private void OnHaveUpdateNotify(String updatefilepath, String updatetext) {
		Log.e("ImRequest UI", "OnHaveUpdateNotify");
	}

	private void OnServerFaild(String sModuleName) {
		Log.e("ImRequest UI", "OnServerFaild");
	}

	private void OnConnectResponse(int nResult) {
		V2Log.d("OnConnectResponse::" + nResult);
		for (ImRequestCallback callback : this.callbacks) {
			callback.OnConnectResponseCallback(nResult);
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

	// 琚Щ鍑虹兢
	private void OnKickCrowd(long nCrowdId, long nAdminId) {
		Log.e("ImRequest UI", "OnKickCrowd:" + nCrowdId);
	}

	// 鎼滅储缇ゅ洖璋�
	private void OnSearchCrowd(String InfoXml) {
		Log.e("ImRequest UI", "OnSearchCrowd::" + InfoXml);
	}

	// 缇ゅ叡浜洖璋�
	private void Oncrowdfile(long nCrowdId, String InfoXml) {
		Log.e("ImRequest UI", "Oncrowdfile:" + nCrowdId);
	}

	// 鑾峰彇缇ゅ叡浜俊鎭�
	private void OnGetCrowdFileInfo(long nCrowdId, String InfoXml) {
		Log.e("ImRequest UI", "OnGetCrowdFileInfo:" + nCrowdId);
	}

	// 鍒犻櫎缇ゅ叡浜枃浠跺洖璋�
	private void OnDelCrowdFile(long nCrowdId, String sFileID) {
		Log.e("ImRequest UI", "OnDelCrowdFile:" + nCrowdId);
	}

	// 淇敼澶囨敞濮撳悕
	private void OnModifyCommentName(long nUserId, String sCommmentName) {
		Log.e("ImRequest UI", "OnModifyCommentName::" + "nUserId:" + nUserId
				+ "  sCommmentName" + sCommmentName);
	}

	private void OnSignalDisconnected() {
		Log.e("ImRequest UI", "OnSignalDisconnected");
	}

	private void OnDelGroupInfo(int type, long groupid, boolean isdel) {
		Log.e("ImRequest UI", "OnDelGroupInfo" + type + ":" + groupid + ":"
				+ isdel);
	}

	// 鏍囧織鐫�幏鍙栫敤鎴峰紑濮�
	private void OnGetGroupsInfoBegin() {
	}

	// 鏍囧織鐫�幏鍙栫敤鎴风粨鏉�

	private boolean haslogin = false; // 鏍囧織宸茬粡鍙戦�浜嗙櫥闄嗙姸鎬�

	private void OnGetGroupsInfoEnd() {

	}
	



}
