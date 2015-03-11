package com.V2.jni;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import com.V2.jni.callbackInterface.ImRequestCallback;
import com.V2.jni.ind.BoUserInfoBase;
import com.V2.jni.util.V2Log;
import com.V2.jni.util.XmlAttributeExtractor;

public class ImRequest {
	private List<WeakReference<ImRequestCallback>> mCallbacks;

	public boolean loginResult;
	private static ImRequest mImRequest;
	public Proxy proxy = new Proxy();

	private boolean haslogin = false;

	private ImRequest(Context context) {
		mCallbacks = new ArrayList<WeakReference<ImRequestCallback>>();
	};

	public void addCallback(ImRequestCallback callback) {
		this.mCallbacks.add(new WeakReference<ImRequestCallback>(callback));
	}

	public void removeCallback(ImRequestCallback callback) {
		for (int i = 0; i < mCallbacks.size(); i++) {
			if (mCallbacks.get(i).get() == callback) {
				this.mCallbacks.remove(i);
				break;
			}
		}
	}

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

	// 尽量使用代理，方便在请求前做一些通用的操作
	public class Proxy {

		public void login(String szName, String szPassword, int status,
				int nDeviceType, boolean isAnonymous) {
			V2Log.d(V2Log.JNI_REQUEST, "CLASS = ImRequest METHOD = login()"
					+ " szName = " + szName + " szPassword = " + szPassword
					+ " status = " + status + " nDeviceType = " + nDeviceType
					+ " isAnonymous = " + isAnonymous);
			ImRequest.this.login(szName, szPassword, status, nDeviceType,
					isAnonymous);

		}

		public void getUserBaseInfo(long nUserID) {
			V2Log.d(V2Log.JNI_REQUEST,
					"CLASS = ImRequest METHOD = getUserBaseInfo()"
							+ " nUserID = " + nUserID);
			ImRequest.this.getUserBaseInfo(nUserID);
		}
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
	 * @param accounttype
	 *            TODO add comment
	 * @param isAnonymous
	 * 
	 */
	private native void login(String szName, String szPassword, int status,
			int nDeviceType, boolean isAnonymous);

	/**
	 * <ul>
	 * Log in call back function. This function only is called by JNI.
	 * </ul>
	 * 
	 * @param nUserID
	 *            logged in user ID
	 * @param nStatus
	 * @param serverTime
	 * @param sDBID
	 *            server id
	 * @param nResult
	 *            0: logged in successfully
	 * 
	 * @see #login(String, String, int, int, boolean)
	 */
	private void OnLogin(long nUserID, int nStatus, long serverTime,
			String sDBID, int nResult) {

		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = ImRequest METHOD = OnLogin() nUserID= " + nUserID
						+ " nStatus = " + nStatus + " serverTime = "
						+ serverTime + " sDBID = " + sDBID + " nResult = "
						+ nResult);

		for (WeakReference<ImRequestCallback> wf : this.mCallbacks) {
			Object obj = wf.get();
			if (obj != null) {
				ImRequestCallback callback = (ImRequestCallback) obj;
				callback.OnLoginCallback(nUserID, nStatus, nResult, serverTime,
						sDBID.trim());
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
		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = ImRequest METHOD = OnLogout() nType= " + nType);

		for (WeakReference<ImRequestCallback> wf : this.mCallbacks) {
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
	private native void getUserBaseInfo(long nUserID);

	/**
	 * <ul>
	 * call back function. This function only is called by JNI.
	 * 自己和和自己有关系的人的个人信息更变时都会回调
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
		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = ImRequest METHOD = OnUpdateBaseInfo() nUserID= "
						+ nUserID + " updatexml = " + updatexml);

		
		
		// BoUserBaseInfo boUserBaseInfo =
		// XmlAttributeExtractor.fromXml(nUserID, updatexml);
		BoUserInfoBase boUserBaseInfo = null;
		try {
			boUserBaseInfo = BoUserInfoBase.parserXml(updatexml);
		} catch (Exception e) {
			V2Log.e("ImRequest OnUpdateBaseInfo --> Parsed the xml convert to a V2User Object failed... userID is : "
					+ "" + nUserID + " and xml is : " + updatexml);
			return;
		}

		if (boUserBaseInfo == null) {
			V2Log.e("ImRequest OnUpdateBaseInfo --> Parsed the xml convert to a V2User Object failed... userID is : "
					+ "" + nUserID + " and xml is : " + updatexml);
			return;
		}
		
		boUserBaseInfo.mId=nUserID;

		boUserBaseInfo.mId = nUserID;
		for (int i = 0; i < mCallbacks.size(); i++) {
			Object obj = mCallbacks.get(i).get();

			if (obj != null) {
				ImRequestCallback callback = (ImRequestCallback) obj;
				callback.OnUpdateBaseInfoCallback(boUserBaseInfo);
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
	 * @param nType
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
	 * @see com.bizcom.vo.User.Status
	 * @see ImRequestCallback#OnUserStatusUpdatedCallback(long, int, int,
	 *      String)
	 */
	private void OnUserStatusUpdated(long nUserID, int nDeviceType,
			int nStatus, String szStatusDesc) {
		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = ImRequest METHOD = OnUserStatusUpdated()"
						+ " nUserID = " + nUserID + " nDeviceType = "
						+ nDeviceType + " nStatus = " + nStatus
						+ " szStatusDesc = " + szStatusDesc);

		for (int i = 0; i < mCallbacks.size(); i++) {
			WeakReference<ImRequestCallback> wf = this.mCallbacks.get(i);
			Object obj = wf.get();
			if (obj != null) {
				ImRequestCallback callback = (ImRequestCallback) obj;
				callback.OnUserStatusUpdatedCallback(nUserID, nDeviceType,
						nStatus, szStatusDesc);
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
		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = ImRequest METHOD = OnChangeAvatar()"
						+ " nAvatarType = " + nAvatarType + " nUserID = "
						+ nUserID + " AvatarName = " + AvatarName);
		for (int i = 0; i < mCallbacks.size(); i++) {
			WeakReference<ImRequestCallback> wf = this.mCallbacks.get(i);
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
		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = ImRequest METHOD = OnModifyCommentName()"
						+ " nUserId = " + nUserId + " sCommmentName = "
						+ sCommmentName);
		for (WeakReference<ImRequestCallback> wf : this.mCallbacks) {
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
	 * 
	 * @param nResult
	 */
	private void OnConnectResponse(int nResult) {
		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = ImRequest METHOD = OnConnectResponse()"
						+ " nResult = " + nResult);
		for (WeakReference<ImRequestCallback> wf : this.mCallbacks) {
			Object obj = wf.get();
			if (obj != null) {
				ImRequestCallback callback = (ImRequestCallback) obj;
				callback.OnConnectResponseCallback(nResult);
			}
		}

	}

	// 鏇存敼绯荤粺澶村儚
	public native void changeSystemAvatar(String szAvatarName);

	public native void changeCustomAvatar(byte[] b, int len,
			String szExtensionName);

	public native void onStartUpdate();

	public native void onStopUpdate();

	/**
	 * Search member
	 * 
	 * @param szUnsharpName
	 * @param nStartNum
	 * @param nSearchNum
	 */
	public native void searchMember(String szUnsharpName, int nStartNum,
			int nSearchNum);

	/**
	 * Search crowd
	 * 
	 * @param szUnsharpName
	 * @param nStartNum
	 * @param nSearchNum
	 */
	public native void searchCrowd(String szUnsharpName, int nStartNum,
			int nSearchNum);

	public native void delCrowdFile(long nCrowdId, String sFileID);

	public native void getCrowdFileInfo(long nCrowdId);

	/**
	 * 10-10 16:14:00.197: E/ImRequest UI(24208):
	 * OnGetSearchMember:<userlist><user account='test1095' authtype='0'
	 * birthday='2000-01-01' bsystemavatar='1' id='130' nickname='test1095'
	 * privacy='0'/><user account='test5' authtype='0' birthday='2000-01-01'
	 * bsystemavatar='1' id='1286' nickname='test5' privacy='0'/></userlist>
	 * 
	 * @param xmlinfo
	 */
	private void OnGetSearchMember(String xmlinfo) {
		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = ImRequest METHOD = OnGetSearchMember()"
						+ " xmlinfo = " + xmlinfo);
		List<BoUserInfoBase> list = XmlAttributeExtractor.parseUserList(
				xmlinfo, "user");
		for (int i = 0; i < this.mCallbacks.size(); i++) {
			WeakReference<ImRequestCallback> wf = this.mCallbacks.get(i);
			Object obj = wf.get();
			if (obj != null) {
				ImRequestCallback callback = (ImRequestCallback) obj;
				callback.OnSearchUserCallback(list);
			}
		}
	}

	private void OnOfflineStart() {
		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = ImRequest METHOD = OnOfflineStart()");
		for (int i = 0; i < this.mCallbacks.size(); i++) {
			WeakReference<ImRequestCallback> wf = this.mCallbacks.get(i);
			Object obj = wf.get();
			if (obj != null) {
				ImRequestCallback callback = (ImRequestCallback) obj;
				callback.OnOfflineStart();
			}
		}
	}

	private void OnOfflineEnd() {
		V2Log.d(V2Log.JNI_CALLBACK, "CLASS = ImRequest METHOD = OnOfflineEnd()");
		for (int i = 0; i < this.mCallbacks.size(); i++) {
			WeakReference<ImRequestCallback> wf = this.mCallbacks.get(i);
			Object obj = wf.get();
			if (obj != null) {
				ImRequestCallback callback = (ImRequestCallback) obj;
				callback.OnOfflineEnd();
			}
		}
	}

	private void OnUserPrivacyUpdated(long nUserID, int nPrivacy) {
		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = ImRequest METHOD = OnUserPrivacyUpdated()"
						+ " nUserID = " + nUserID + " nPrivacy = " + nPrivacy);
	}

	private void OnCreateFriendGroup(long nGroupID, String szGroupName) {
		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = ImRequest METHOD = OnCreateFriendGroup()--"
						+ " nGroupID = " + nGroupID + " szGroupName = "
						+ szGroupName);
	}

	private void OnModifyFriendGroup(long nGroupID, String szGroupName) {
		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = ImRequest METHOD = OnModifyFriendGroup()--"
						+ " nGroupID = " + nGroupID + " szGroupName = "
						+ szGroupName);

	}

	private void OnMoveFriendsToGroup(long nDstUserID, long nDstGroupID) {
		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = ImRequest METHOD = OnMoveFriendsToGroup()--"
						+ " nDstUserID = " + nDstUserID + " nDstGroupID = "
						+ nDstGroupID);
	}

	private void OnHaveUpdateNotify(String updatefilepath, String updatetext) {
		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = ImRequest METHOD = OnHaveUpdateNotify()--"
						+ " updatefilepath = " + updatefilepath
						+ " updatetext = " + updatetext);
	}

	private void OnServerFaild(String sModuleName) {
		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = ImRequest METHOD = OnServerFaild()--"
						+ " sModuleName = " + sModuleName);
	}

	private void OnUpdateDownloadBegin(long filesize) {
		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = ImRequest METHOD = OnUpdateDownloadBegin()--"
						+ " filesize = " + filesize);
	}

	private void OnUpdateDownloading(long size) {
		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = ImRequest METHOD = OnUpdateDownloading()--"
						+ " size = " + size);
	}

	private void OnUpdateDownloadEnd(boolean error) {
		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = ImRequest METHOD = OnUpdateDownloadEnd()--"
						+ " error = " + error);
	}

	private void Oncrowdfile(long nCrowdId, String InfoXml) {
		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = ImRequest METHOD = Oncrowdfile()--" + " nCrowdId = "
						+ nCrowdId + " InfoXml = " + InfoXml);
	}

	private void OnGetCrowdFileInfo(long nCrowdId, String InfoXml) {
		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = ImRequest METHOD = OnDelCrowdFile()--"
						+ " nCrowdId = " + nCrowdId + " InfoXml = " + InfoXml);
	}

	private void OnDelCrowdFile(long nCrowdId, String sFileID) {
		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = ImRequest METHOD = OnDelCrowdFile()--"
						+ " nCrowdId = " + nCrowdId + " sFileID = " + sFileID);
	}

	private void OnSignalDisconnected() {
		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = ImRequest METHOD = OnSignalDisconnected()--");
	}

	private void OnDelGroupInfo(int type, long groupid, boolean isdel) {
		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = ImRequest METHOD = OnDelGroupInfo()--" + " type = "
						+ type + " groupid = " + groupid + " isdel = " + isdel);
	}

	private void OnGetGroupsInfoBegin() {
		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = ImRequest METHOD = OnGetGroupsInfoBegin()--");
	}

	private void OnGetGroupsInfoEnd() {
		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = ImRequest METHOD = OnGetGroupsInfoEnd()");

		for (int i = 0; i < this.mCallbacks.size(); i++) {
			WeakReference<ImRequestCallback> wf = this.mCallbacks.get(i);
			Object obj = wf.get();
			if (obj != null) {
				ImRequestCallback callback = (ImRequestCallback) obj;
				callback.OnGroupsLoaded();
			}
		}
	}

}
