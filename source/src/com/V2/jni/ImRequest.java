package com.V2.jni;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.util.Log;

import com.v2tech.util.V2Log;

public class ImRequest {
	public boolean loginResult;
	private static ImRequest mImRequest;

	private List<WeakReference<ImRequestCallback>> callbacks;

	private ImRequest(Context context) {
		callbacks = new ArrayList<WeakReference<ImRequestCallback>>();
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
			mImRequest = new ImRequest(null);
			if (!mImRequest.initialize(mImRequest)) {
				V2Log.e(" can't  initialize imrequest ");
				throw new RuntimeException("can't initilaize imrequest");
			}
		}

		return mImRequest;
	}

	/**
	 * 
	 * @param callback
	 */
	public void addCallback(ImRequestCallback callback) {
		this.callbacks.add(new WeakReference<ImRequestCallback>(callback));
	}

	public native boolean initialize(ImRequest request);

	public native void unInitialize();

	/**
	 * <ul>
	 * Log in to server. server will call {@link #OnLogin(long, int, long, int)}
	 * to indicate response
	 * </ul>
	 * 
	 * @param szName
	 *            user name
	 * @param szPassword
	 *            password
	 * @param status
	 *            TODO add comment
	 * @param type
	 *            TODO add comment
	 * @param isAnonymous
	 * 
	 */
	public native void login(String szName, String szPassword, int status,
			int type, boolean isAnonymous);

	/**
	 * <ul>
	 * Log in call back function. This function only is called by JNI.
	 * </ul>
	 * 
	 * @param nUserID
	 *            logged in user ID
	 * @param nStatus
	 * @param nResult
	 *            0: logged in successfully
	 * 
	 * @see #login(String, String, int, int, boolean)
	 */
	private void OnLogin(long nUserID, int nStatus, long serverTime, int nResult) {
		V2Log.d("OnLogin --> " + nUserID + ": " + "-:" + nStatus + ":"
				+ nResult);
		for (WeakReference<ImRequestCallback> wf : this.callbacks) {
			Object obj = wf.get();
			if (obj != null) {
				ImRequestCallback callback = (ImRequestCallback) obj;
				callback.OnLoginCallback(nUserID, nStatus, nResult);
			}
		}
	}

	/**
	 * <ul>
	 * When Same user log in with other device, then this function will be
	 * called
	 * </ul>
	 * 
	 * @param nType
	 *            device type of logged
	 */
	private void OnLogout(int nType) {
		V2Log.d("OnLogout::" + nType);
		for (WeakReference<ImRequestCallback> wf : this.callbacks) {
			Object obj = wf.get();
			if (obj != null) {
				ImRequestCallback callback = (ImRequestCallback) obj;
				callback.OnLogoutCallback(nType);
			}
		}

	}

	/**
	 * <ul>
	 * Get user information from server.<br>
	 * when call this API, JNI will call {@link #OnUpdateBaseInfo(long, String)}
	 * to indicate response.<br>
	 * </ul>
	 * 
	 * @param nUserID
	 *            user ID which want to get user information
	 */
	public native void getUserBaseInfo(long nUserID);

	/**
	 * <ul>
	 * call back function. This function only is called by JNI.
	 * </ul>
	 * <ul>
	 * {@link #getUserBaseInfo(long)} callback.
	 * </ul>
	 * 
	 * @param nUserID
	 * @param updatexml
	 * 
	 */
	private void OnUpdateBaseInfo(long nUserID, String updatexml) {
		for (WeakReference<ImRequestCallback> wf : this.callbacks) {
			Object obj = wf.get();
			if (obj != null) {
				ImRequestCallback callback = (ImRequestCallback) obj;
				callback.OnUpdateBaseInfoCallback(nUserID, updatexml);
			}
		}
	}

	/**
	 * <ul>
	 * Indicate user's status changed.
	 * </ul>
	 * 
	 * @param nUserID
	 *            user ID
	 * @param eUEType
	 *            device type of user logged in
	 * @param nStatus
	 *            <ul>
	 *            new status of user
	 *            </ul>
	 *            <ul>
	 *            <li>0 : off line</li>
	 *            <li>1 : on line</li>
	 *            <li>2 : leave</li>
	 *            <li>3 : busy</li>
	 *            <li>4 : do not disturb</li>
	 *            <li>5 : hidden</li>
	 *            </ul>
	 * @param szStatusDesc
	 * 
	 * @see com.v2tech.vo.User.Status
	 * @see ImRequestCallback#OnUserStatusUpdatedCallback(long, int, int,
	 *      String)
	 */
	private void OnUserStatusUpdated(long nUserID, int nType, int nStatus,
			String szStatusDesc) {
		V2Log.d(" OnUserStatusUpdated--> nUserID:" + nUserID + "  nStatus:"
				+ nStatus + " nType:" + nType + " szStatusDesc:" + szStatusDesc
				+ "  " + new Date());
		for (WeakReference<ImRequestCallback> wf : this.callbacks) {
			Object obj = wf.get();
			if (obj != null) {
				ImRequestCallback callback = (ImRequestCallback) obj;
				callback.OnUserStatusUpdatedCallback(nUserID, nType, nStatus,
						szStatusDesc);
			}
		}
	}

	/**
	 * <ul>
	 * Indicate user avatar changed.
	 * </ul>
	 * 
	 * @param nAvatarType
	 * @param nUserID
	 *            User ID which user's changed avatar
	 * @param AvatarName
	 *            patch of avatar
	 * 
	 * @see ImRequestCallback#OnChangeAvatarCallback(int, long, String)
	 */
	private void OnChangeAvatar(int nAvatarType, long nUserID, String AvatarName) {
		V2Log.d("OnChangeAvatar--> nAvatarType:" + nAvatarType + "    nUserID:"
				+ nUserID + " AvatarName:" + AvatarName);
		for (WeakReference<ImRequestCallback> wf : this.callbacks) {
			Object obj = wf.get();
			if (obj != null) {
				ImRequestCallback callback = (ImRequestCallback) obj;
				callback.OnChangeAvatarCallback(nAvatarType, nUserID,
						AvatarName);
			}
		}
	}

	/**
	 * <ul>
	 * Update user information
	 * </ul>
	 * 
	 * @param InfoXml
	 *            content as below:<br>
	 *            {@code <user address="" birthday="" fax="" homepage="" job="" mobile="" nickname="" sex="1" sign="" telephone=""><videolist/> </user> }
	 */
	public native void modifyBaseInfo(String InfoXml);

	/**
	 * <ul>
	 * Update contacts nick name</br>
	 * 
	 * </ul>
	 * 
	 * @param nUserId
	 * @param sCommentName
	 * 
	 * @see ImRequest#OnModifyCommentName(long, String)
	 */
	public native void modifyCommentName(long nUserId, String sCommentName);

	/**
	 * 
	 * @param nUserId
	 * @param sCommmentName
	 */
	private void OnModifyCommentName(long nUserId, String sCommmentName) {
		V2Log.d("ImRequest UI --> OnModifyCommentName:: " + "nUserId:"
				+ nUserId + "  sCommmentName" + sCommmentName);
		for (WeakReference<ImRequestCallback> wf : this.callbacks) {
			Object obj = wf.get();
			if (obj != null) {
				ImRequestCallback callback = (ImRequestCallback) obj;
				callback.OnModifyCommentNameCallback(nUserId, sCommmentName);
			}
		}
	}

	public native void updateMyStatus(int nStatus, String szStatusDesc);

	
	/**
	 * Connection state callback
	 * @param nResult
	 */
	private void OnConnectResponse(int nResult) {
		V2Log.d("OnConnectResponse::" + nResult);
		for (WeakReference<ImRequestCallback> wf : this.callbacks) {
			Object obj = wf.get();
			if (obj != null) {
				ImRequestCallback callback = (ImRequestCallback) obj;
				callback.OnConnectResponseCallback(nResult);
			}
		}

	}
	
	
	/**
	 * Crowd created request call back<br>
	 * 
	 * @see {@link ImRequestCallback#OnCreateCrowdCallback(String, int)}
	 * @param sCrowdXml
	 *            {@code <crowd authtype='0' id='44' name='hhh mjj ' size='100'/>}
	 * @param nResult
	 *            0: successfully
	 */
	private void OnCreateCrowd(String sCrowdXml, int nResult) {
		V2Log.d("ImRequest UI -- > OnCreateCrowd  " + "sCrowdXml:" + sCrowdXml
				+ "  nResult:" + nResult);
		for (WeakReference<ImRequestCallback> wf : this.callbacks) {
			Object obj = wf.get();
			if (obj != null) {
				ImRequestCallback callback = (ImRequestCallback) obj;
				callback.OnCreateCrowdCallback(sCrowdXml, nResult);
			}
		}
	}

	// 淇敼涓汉淇℃伅
	/*
 * 
 */

	// 鏇存敼绯荤粺澶村儚
	public native void changeSystemAvatar(String szAvatarName);

	public native void changeCustomAvatar(byte[] b, int len,
			String szExtensionName);

	public native void onStartUpdate();

	public native void onStopUpdate();

	public native void searchMember(String szUnsharpName, int nStartNum,
			int nSearchNum);

	public native void searchCrowd(String szUnsharpName, int nStartNum,
			int nSearchNum);

	public native void delCrowdFile(long nCrowdId, String sFileID);

	public native void getCrowdFileInfo(long nCrowdId);

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

	private void OnCreateFriendGroup(long nGroupID, String szGroupName) {
		Log.e("ImRequest UI", "OnCreateFriendGroup锛氾細" + nGroupID + ":"
				+ szGroupName);
	}

	private void OnModifyFriendGroup(long nGroupID, String szGroupName) {
		Log.e("ImRequest UI", "OnModifyFriendGroup::" + nGroupID + ":"
				+ szGroupName);

	}

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

	private void OnKickCrowd(long nCrowdId, long nAdminId) {
		Log.e("ImRequest UI", "OnKickCrowd:" + nCrowdId);
	}

	private void OnSearchCrowd(String InfoXml) {
		Log.e("ImRequest UI", "OnSearchCrowd::" + InfoXml);
	}

	private void Oncrowdfile(long nCrowdId, String InfoXml) {
		Log.e("ImRequest UI", "Oncrowdfile:" + nCrowdId);
	}

	private void OnGetCrowdFileInfo(long nCrowdId, String InfoXml) {
		Log.e("ImRequest UI", "OnGetCrowdFileInfo:" + nCrowdId);
	}

	private void OnDelCrowdFile(long nCrowdId, String sFileID) {
		Log.e("ImRequest UI", "OnDelCrowdFile:" + nCrowdId);
	}

	private void OnSignalDisconnected() {
		Log.e("ImRequest UI", "OnSignalDisconnected");
	}

	private void OnDelGroupInfo(int type, long groupid, boolean isdel) {
		Log.e("ImRequest UI", "OnDelGroupInfo" + type + ":" + groupid + ":"
				+ isdel);
	}

	private void OnGetGroupsInfoBegin() {
	}


	private boolean haslogin = false;

	private void OnGetGroupsInfoEnd() {

	}

}
