package com.V2.jni;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.V2.jni.ind.V2Group;
import com.V2.jni.ind.V2User;
import com.V2.jni.util.V2Log;
import com.V2.jni.util.XmlAttributeExtractor;

public class ImRequest {
	public boolean loginResult;
	private static ImRequest mImRequest;

	private List<WeakReference<ImRequestCallback>> mCallbacks;

	private ImRequest(Context context) {
		mCallbacks = new ArrayList<WeakReference<ImRequestCallback>>();
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
		// GlobalConfig.TIME_SERVER_TIME = serverTime;
		for (WeakReference<ImRequestCallback> wf : this.mCallbacks) {
			Object obj = wf.get();
			if (obj != null) {
				ImRequestCallback callback = (ImRequestCallback) obj;
				callback.OnLoginCallback(nUserID, nStatus, nResult, serverTime);
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
		V2Log.d("ImRequest.OnUpdateBaseInfo==>" + "nUserID:" + nUserID + ","
				+ "updatexml:" + updatexml);
		for (WeakReference<ImRequestCallback> wf : this.mCallbacks) {
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
		if (nUserID == 113 || nUserID == 112) {
			V2Log.d(" OnUserStatusUpdated--> nUserID:" + nUserID + "  nStatus:"
					+ nStatus + " nType:" + nType + " szStatusDesc:"
					+ szStatusDesc + "  " + new Date());
		}
		for (int i = 0; i < mCallbacks.size(); i++) {
			WeakReference<ImRequestCallback> wf = this.mCallbacks.get(i);
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
		for (WeakReference<ImRequestCallback> wf : this.mCallbacks) {
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
		V2Log.d("OnConnectResponse::" + nResult);
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
		Log.e("ImRequest UI", "OnGetSearchMember:" + xmlinfo);
		List<V2User> list = XmlAttributeExtractor.parseUserList(xmlinfo, "user");
		for (int i = 0; i <this.mCallbacks.size(); i++) {
			WeakReference<ImRequestCallback> wf = this.mCallbacks.get(i);
			Object obj = wf.get();
			if (obj != null) {
				ImRequestCallback callback = (ImRequestCallback) obj;
				callback.OnSearchUserCallback(list);
			}
		}
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
		for (int i = 0; i <this.mCallbacks.size(); i++) {
			WeakReference<ImRequestCallback> wf = this.mCallbacks.get(i);
			Object obj = wf.get();
			if (obj != null) {
				ImRequestCallback callback = (ImRequestCallback) obj;
				callback.OnKickCrowd(nCrowdId, nAdminId);
			}
		}
	}

	/**
	 * 10-10 16:12:06.997: E/ImRequest UI(24208):
	 * OnSearchCrowd::<crowdlist><crowd announcement='' authtype='1'
	 * creatornickname='zhao1' creatoruserid='14' id='44' name='13269997886'
	 * size='500' summary=''/><crowd announcement='' authtype='0'
	 * creatornickname='test3' creatoruserid='1290' id='481' name='1' size='0'
	 * summary=''/><crowd announcement='' authtype='0' creatornickname='test3'
	 * creatoruserid='1290' id='491' name='11' size='0' summary=''/><crowd
	 * announcement='' authtype='0' creatornickname='test3' creatoruserid='1290'
	 * id='492' name='12' size='0' summary=''/><crowd announcement=''
	 * authtype='0' creatornickname='long3' creatoruserid='11749' id='4123'
	 * name='long1' size='500' summary=''/><crowd announcement='' authtype='0'
	 * creatornickname='test1' creatoruserid='1246' id='4138' name='111'
	 * size='500' summary=''/><crowd
	 * announcement='idsfiposdaippafsjgjlkfdsjglfdkjKjjjjjjjj' authtype='1'
	 * creatornickname='zhao2' creatoruserid='15' id='4139' name='43214321432'
	 * size='500' summary=
	 * 'idsfiposdaippafsjgjlkfdsjglfdkjKjjjjjjjjafdsafsdafdsafdsafdsafds43214321443243212121'/><crow
	 * d announcement='' authtype='0' creatornickname='zhao2' creatoruserid='15'
	 * id='4141' name='432143214' size='500' summary=''/></crowdlist>
	 * 
	 * @param InfoXml
	 */
	private void OnSearchCrowd(String InfoXml) {
		Log.e("ImRequest UI", "OnSearchCrowd::" + InfoXml);
		List<V2Group> list = XmlAttributeExtractor.parseCrowd(InfoXml);
		for (int i = 0; i <this.mCallbacks.size(); i++) {
			WeakReference<ImRequestCallback> wf = this.mCallbacks.get(i);
			Object obj = wf.get();
			if (obj != null) {
				ImRequestCallback callback = (ImRequestCallback) obj;
				callback.OnSearchCrowdCallback(list);
			}
		}
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
