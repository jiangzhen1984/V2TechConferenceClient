package com.V2.jni;

import java.util.ArrayList;
import java.util.List;

import com.v2tech.util.V2Log;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

public class ConfRequest {

	private static ConfRequest mConfRequest;

	private List<ConfRequestCallback> callbacks;

	private ConfRequest(Context context) {
		this.callbacks = new ArrayList<ConfRequestCallback>();
	};

	public static synchronized ConfRequest getInstance(Context context) {
		if (mConfRequest == null) {
			mConfRequest = new ConfRequest(context);
			mConfRequest.initialize(mConfRequest);
		}

		return mConfRequest;
	}

	public static synchronized ConfRequest getInstance() {
		if (mConfRequest == null) {
			mConfRequest = new ConfRequest(null);
			mConfRequest.initialize(mConfRequest);
		}

		return mConfRequest;
	}

	public void addCallback(ConfRequestCallback callback) {
		this.callbacks.add(callback);
	}

	private boolean isInConf = false;

	public native boolean initialize(ConfRequest request);

	public native void unInitialize();

	// 鍒犻櫎浼氳
	public native void destroyConf(long nConfID);

	/**
	 * <ul>
	 * User request to enter conference. will call {@link OnEnterConf} function
	 * to indicate response.
	 * </ul>
	 * 
	 * @param nConfID
	 *            conference ID which user request to enter
	 * 
	 * @see #OnEnterConf(long, long, String, int)
	 * @see ConfRequestCallback
	 */
	public native void enterConf(long nConfID);

	/**
	 * <ul>
	 * Callback of {@link #enterConf(long)}, Indicate response of result.
	 * </ul>
	 * 
	 * @param nConfID
	 *            conference ID which user request to enter
	 * @param nTime
	 *            entered time
	 * @param szConfData
	 * @param nJoinResult
	 *            result of join. 0 success 1:falied
	 */
	private void OnEnterConf(long nConfID, long nTime, String szConfData,
			int nJoinResult) {
		for (ConfRequestCallback cb : this.callbacks) {
			cb.OnEnterConfCallback(nConfID, nTime, szConfData, nJoinResult);
		}
		V2Log.d("OnEnterConf called  nConfID:" + nConfID + "  nTime:" + nTime
				+ " szConfData:" + szConfData + " nJoinResult:" + nJoinResult);
	}

	/**
	 * <ul>
	 * User request exit conference, this request no callback call.
	 * </ul>
	 * 
	 * @param nConfID
	 *            conference ID which user request to exit
	 * 
	 * @see ConfRequestCallback
	 */
	public native void exitConf(long nConfID);

	/**
	 * 
	 * 
	 * @param nConfID
	 * @param nTime
	 * @param szUserInfos
	 * 
	 * @see ConfRequestCallback
	 */
	private void OnConfMemberEnter(long nConfID, long nTime, String szUserInfos) {
		V2Log.d("-->OnConfMemberEnter " + nConfID + " " + nTime + " "
				+ szUserInfos);
		for (ConfRequestCallback cb : this.callbacks) {
			cb.OnConfMemberEnterCallback(nConfID, nTime, szUserInfos);
		}
	}

	/**
	 * 
	 * @param nConfID
	 * @param nTime
	 * @param nUserID
	 */
	private void OnConfMemberExit(long nConfID, long nTime, long nUserID) {
		V2Log.d("-->OnConfMemberExit " + nConfID + " " + nTime + " " + nUserID);

		for (ConfRequestCallback cb : this.callbacks) {
			cb.OnConfMemberExitCallback(nConfID, nTime, nUserID);
		}

	}

	private void OnKickConf(int nReason) {
		V2Log.d("-->OnKickConf " + nReason);
		for (ConfRequestCallback cb : this.callbacks) {
			cb.OnKickConfCallback(nReason);
		}

	}

	public native void openVideoMixer(String szMediaID, int nLayout,
			int videoSizeFormat, int nID);

	public native void closeVideoMixer(String szMediaID);

	public native void syncOpenVideoMixer(String szMediaID, int nLayout,
			int videoSizeFormat, int nPos, String szVideoMixerListXml);

	public native void syncCloseVideoMixer(String szMediaID);

	public native void addVideoMixerDevID(String szMediaID, long nDstUserID,
			String szDstDevID, int nPos);

	public native void delVideoMixerDevID(String szMediaID, long nDstUserID,
			String szDstDevID);

	public native void startMixerVideoToSip(long nSipUserID, String szMediaID);

	public native void stopMixerVideoToSip(long nSipUserID, String szMediaID);

	// 灏嗘煇浜鸿鍑轰細璁�
	public native void kickConf(long nUserID);

	// 閭�鍔犲叆浼氳
	public native void inviteJoinConf(long nDstUserID);

	// 鑾峰彇浼氳鐢ㄦ埛鍒楄〃(鏈繘鍏ヤ細璁級
	public native void getConfUserList(long nConfID);

	// 閲嶆柊寮�涓�釜浼氳
	public native void resumeConf(String sXmlConfData, String sXmlInviteUsers);

	// 浼氬満闈欓煶
	public native void muteConf();

	// 淇敼浼氳鎻忚堪
	public native void modifyConfDesc(String sXml);

	// 淇敼浼氳鍚屾
	public native void setConfSync(int syncMode);

	// 鍙栧緱浼氳鍒楄〃
	public native void getConfList();

	// 鐢宠鎺у埗鏉�
	public native void applyForControlPermission(int type);

	// 閲婃斁鎺у埗鏉�
	public native void releaseControlPermission(int type);

	// 涓诲腑鎺堜簣鏌愪汉鏉冮檺
	public native void grantPermission(long userid, int type, int status);

	// 璁剧疆鍏朵粬鐢ㄦ埛瑙嗛閲囬泦鍙傛暟
	public native void setCapParam(String szDevID, int nSizeIndex,
			int nFrameRate, int nBitRate);

	// 鍚屾鎵撳紑鏌愪汉瑙嗛
	public native void syncConfOpenVideo(long nGroupID, long nToUserID,
			String szDeviceID, int nPos);

	// 鍚屾鍙栨秷鏌愪汉瑙嗛
	public native void cancelSyncConfOpenVideo(long nGroupID, long nToUserID,
			String szDeviceID, boolean bCloseVideo);

	// 杞鎵撳紑鏌愪汉瑙嗛
	public native void pollingConfOpenVideo(long nGroupID, long nToUserID,
			String sToDevID, long nFromUserID, String sFromDevID);

	// 缃崲浼氳涓诲腑
	public native void changeConfChair(long nGroupID, long nUserID);

	// public List<Conf> confs = new ArrayList<Conf>();

	public boolean enterConf = false;

	private void OnAddUser(long userid, String xml) {
		Log.e("ImRequest UI", "OnAddUser " + userid + "  " + xml);
	}

	private void OnConfMute() {
		Log.e("ImRequest UI", "OnConfMute ");
	}

	// 浼氳琚垹闄ょ殑鍥炶皟
	private void OnDestroyConf(long nConfID) {
		Log.e("ImRequest UI", "OnDestroyConf " + nConfID);
	}

	// 鎴戞敹鍒板姞鍏ヤ細璁殑閭�鐨勫洖璋� private void OnInviteJoinConf(String confXml, String
	// userXml)
	{
		// System.out.println(confXml+";"+userXml);
		// ByteArrayInputStream in=new
		// ByteArrayInputStream(pUserBaseMsg.getBytes());
		// User user=XmlParserUtils.parserInviteConf(in);
		// Bundle bundle=new Bundle();
		// bundle.putSerializable("OnInviteJoinConf", user);
		// bundle.putString("invite_subject", szSubject);
		// bundle.putString("invite_password", Password);
		// bundle.putLong("invite_conf", nConfID);

		// if(isInConf){
		// MeetingRoom.mMeetingRoom.SendMessage(Constant.INVITE_JOIN_CONF,
		// bundle);
		// }else{
		// //
		// ConfMainActivity.mConfMainActivity.SendMessage(Constant.INVITE_JOIN_CONF,
		// bundle);
		// }

	}

	// strUserList :
	// <xml>
	// <user id='' nickname=''/>
	// <user id='' nickname=''/>
	// </xml>
	private void OnConfUserListReport(long nConfID, String strUserList) {
		Log.e("ImRequest UI", "浼氳鍐呭ソ鍙嬪垪琛�->OnConfUserListReport "
				+ strUserList);
	}

	// 浼氳鏈夌敤鎴风寮�殑鍥炶皟
	private void OnConfMemberLeave(long nConfID, long nUserID) {
		Log.e("ImRequest UI", "浼氳鏈変汉閫�嚭-->OnConfMemberLeave " + nConfID + " "
				+ nUserID);
		Bundle bundle = new Bundle();
		bundle.putLong("OnConfMemberLeave", nUserID);
		// MeetingRoom.mMeetingRoom.SendMessage(Constant.USER_LEAVE_CONF,
		// bundle);
	}

	private void OnConfNotify(String str, String str2) {

	}

	public void OnConfSyncOpenVideo(String str) {

	}

	public void OnConfSyncCloseVideo(long gid, String str) {

	}

	private void OnConfNotify(long nSrcUserID, String srcNickName,
			long nConfID, String subject, long nTime) {
		// TODO
		Log.e("ImRequest UI", "OnConfNotify " + nSrcUserID + " " + srcNickName
				+ " " + nConfID + " " + subject + " " + nTime);
	}

	private void OnConfNotifyEnd(long nConfID) {

	}

	// 寰楀埌浼氳瀹ゅ垪琛�
	private void OnGetConfList(String szConfListXml) {
		// Logger.i(null,"寰楀埌浼氳鍒楄〃:"+szConfListXml);

	}

	// 浼氳鎻忚堪淇℃伅琚慨鏀圭殑鍥炶皟
	private void OnModifyConfDesc(long nConfID, String szConfDescXml) {
		// TODO
		Log.e("ImRequest UI", "OnModifyConfDesc " + nConfID + " "
				+ szConfDescXml);
	}

	// 閫氱煡涓诲腑鏌愪汉鐢宠鎺у埗鏉冨洖璋�
	private void OnNotifyChair(long userid, int type) {
		Log.e("ImRequest UI", "OnNotifyChair " + userid + " " + type);

	}

	// 閫氱煡浼氳鎴愬憳鏌愪汉鑾峰緱鏌愮鏉冮檺
	private void OnGrantPermission(long userid, int type, int status) {
		// type 1 3 鍙戣█ status 1 鐢宠 2 鍙栨秷 3 鍙栧緱
		Log.e("ImRequest UI", "OnGrantPermission " + userid + " " + type + " "
				+ status);
	}

	// 鍚屾鎵撳紑鏌愪汉瑙嗛
	private void OnConfSyncOpenVideo(long nDstUserID, String sDstMediaID,
			int nPos) {
		Log.e("ImRequest UI", "OnConfSyncOpenVideo " + sDstMediaID);
	}

	private void OnConfPollingOpenVideo(long nToUserID, String sToDevID,
			long nFromUserID, String sFromDevID) {
		// TODO
		Log.e("ImRequest UI", "OnConfPollingOpenVideo " + nToUserID + " "
				+ sToDevID + " " + nFromUserID + " " + sFromDevID);
	}

	// 鍚屾鍙栨秷鏌愪汉瑙嗛
	private void OnConfSyncCloseVideo(long nDstUserID, String sDstMediaID,
			boolean bClose) {
		Log.e("ImRequest UI", "OnConfSyncCloseVideo " + nDstUserID + " "
				+ sDstMediaID + " " + bClose);
	}

	private void OnSetConfMode(long confid, int mode) {
		Log.e("ImRequest UI", "OnConfSyncCloseVideo " + confid + " " + mode);
	}

	private void OnConfChairChanged(long nConfID, long nChairID) {
		Log.e("ImRequest UI", "OnConfChairChanged " + nConfID + " " + nChairID);
	}

	private void OnSetCanOper(long nConfID, boolean bCanOper) {
		Log.e("ImRequest UI", "OnSetCanOper " + nConfID + " " + bCanOper);
	}

	private void OnSetCanInviteUser(long nConfID, boolean bInviteUser) {
		Log.e("ImRequest UI", "OnSetCanInviteUser " + nConfID + " "
				+ bInviteUser);
	}

	private void OnSyncOpenVideoMixer(String sMediaID, int nLayout,
			int videoSizeFormat, int nPos, String sSyncVideoMixerXml) {
		V2Log.d("OnSyncOpenVideoMixer");
	}

	private void OnSyncCloseVideoMixer(String sMediaID) {
		V2Log.d("OnSyncCloseVideoMixer");
	}

	private void OnAddVideoMixer(String sMediaID, long nDstUserID,
			String sDstDevID, int nPos) {
		V2Log.d("OnAddVideoMixer");
	}

	private void OnDelVideoMixer(String sMediaID, long nDstUserID,
			String sDstDevID) {
		V2Log.d("OnDelVideoMixer");
	}
}