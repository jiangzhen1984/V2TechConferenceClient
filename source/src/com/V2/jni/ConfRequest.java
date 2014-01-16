package com.V2.jni;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.v2tech.view.VideoActivity;


public class ConfRequest
{
	
	private static ConfRequest mConfRequest;
	private Context context;
	
	private ConfRequestCallback callback;
	
	private ConfRequest(Context context){
		this.context = context;
	};
	
	public static synchronized ConfRequest getInstance(Context context){
		if(mConfRequest==null){
			mConfRequest=new ConfRequest(context);
			mConfRequest.initialize(mConfRequest);
		}
		
		return mConfRequest;
	}
	
	
	public static synchronized ConfRequest getInstance() {
		if (mConfRequest == null) {
			throw new RuntimeException("doesn't initliaze ConfRequest yet, please getInstance(Context) first");
		}

		return mConfRequest;
	}
	
	
	
	
	public void setCallback(ConfRequestCallback callback) {
		this.callback = callback;
	}




	private boolean isInConf = false;
	
	public native boolean initialize(ConfRequest request);
	public native void unInitialize();
	
	//sXmlConfData : 
	//<conf canaudio="1" candataop="1" canvideo="1" conftype="0" haskey="0" id="0" key="" 
	//	layout="1" lockchat="0" lockconf="0" lockfiletrans="0" mode="2" pollingvideo="0" 
	//	subject="ss" syncdesktop="0" syncdocument="1" syncvideo="0" 
	//	chairuserid='0' chairnickname=''>  
	//</conf>  
	//szInviteUsers :
	//<xml>   
	//	<user id="11760" nickname=""/>  
	//	<user id="11762" nickname=""/>   
	//</xml>
	public native void createConf(String sXmlConfData, String sXmlInviteUsers);
	//鍒犻櫎浼氳
	public native void destroyConf(long nConfID);
	
	/**
	 * Let user enter conference.<br>
	 * Callback is {@link OnEnterConf}
	 * @param nConfID  conference ID
	 */
	public native void enterConf(long nConfID);
	
	//
	public native void exitConf(long nConfID);
	
	//灏嗘煇浜鸿鍑轰細璁�
	public native void kickConf(long nUserID);
	//閭�鍔犲叆浼氳
	public native void inviteJoinConf(long nDstUserID);
	//鑾峰彇浼氳鐢ㄦ埛鍒楄〃(鏈繘鍏ヤ細璁級
	public native void getConfUserList(long nConfID);
	//閲嶆柊寮�涓�釜浼氳
	public native void resumeConf(String sXmlConfData, String sXmlInviteUsers);
	//浼氬満闈欓煶
	public native void muteConf();
	//淇敼浼氳鎻忚堪
	public native void modifyConfDesc(String sXml);
	//淇敼浼氳鍚屾
	public native void setConfSync(int syncMode);
	//鍙栧緱浼氳鍒楄〃
	public native void getConfList();
	//鐢宠鎺у埗鏉�
	public native void applyForControlPermission(int type);
	//閲婃斁鎺у埗鏉�
	public native void releaseControlPermission(int type);
	//涓诲腑鎺堜簣鏌愪汉鏉冮檺
	public native void grantPermission(long userid, int type, int status);
	//璁剧疆鍏朵粬鐢ㄦ埛瑙嗛閲囬泦鍙傛暟
	public native void setCapParam(String szDevID, int nSizeIndex, int nFrameRate, int nBitRate);
	//鍚屾鎵撳紑鏌愪汉瑙嗛
	public native void syncConfOpenVideo(long nGroupID, long nToUserID, String szDeviceID, int nPos);
	//鍚屾鍙栨秷鏌愪汉瑙嗛
	public native void cancelSyncConfOpenVideo(long nGroupID, long nToUserID, String szDeviceID, boolean bCloseVideo);
	//杞鎵撳紑鏌愪汉瑙嗛
	public native void pollingConfOpenVideo(long nGroupID, long nToUserID, String sToDevID, long  nFromUserID, String sFromDevID);
	//缃崲浼氳涓诲腑
	public native void changeConfChair(long nGroupID, long nUserID);
	
	

//	public List<Conf> confs = new ArrayList<Conf>();
	
	public boolean enterConf = false;
	
	
	//鎴戝姞鍏ヤ細璁殑鍥炶皟
	private void OnEnterConf(long nConfID, long nTime, String szConfData, int nJoinResult)
	{
			if (callback != null) {
				callback.OnEnterConfCallback(nConfID, nTime, szConfData, nJoinResult);
			}
	}
	
	private void OnAddUser(long userid,String xml){
		Log.e("ImRequest UI", "OnAddUser " + userid+"  "+xml);
	}
	
	private void OnConfMute(){
		Log.e("ImRequest UI", "OnConfMute ");
	}

	//浼氳琚垹闄ょ殑鍥炶皟
		private void OnDestroyConf(long nConfID)
		{
			Log.e("ImRequest UI", "OnDestroyConf " + nConfID);
		}

		//鎴戞敹鍒板姞鍏ヤ細璁殑閭�鐨勫洖璋�		private void OnInviteJoinConf(String confXml, String userXml)
		{
//			System.out.println(confXml+";"+userXml);
//			ByteArrayInputStream in=new ByteArrayInputStream(pUserBaseMsg.getBytes());
//			User user=XmlParserUtils.parserInviteConf(in);
//			Bundle bundle=new Bundle();
//			bundle.putSerializable("OnInviteJoinConf", user);
//			bundle.putString("invite_subject", szSubject);
//			bundle.putString("invite_password", Password);
//			bundle.putLong("invite_conf", nConfID);
			
//			if(isInConf){
//				MeetingRoom.mMeetingRoom.SendMessage(Constant.INVITE_JOIN_CONF, bundle);
//			}else{
////				ConfMainActivity.mConfMainActivity.SendMessage(Constant.INVITE_JOIN_CONF, bundle);
//			}
			
			
			
		}
		

		
		//strUserList :
		//<xml>
		//	<user id='' nickname=''/>
		//	<user id='' nickname=''/>
		//</xml>
		private void OnConfUserListReport(long nConfID, String strUserList)
		{
			Log.e("ImRequest UI", "浼氳鍐呭ソ鍙嬪垪琛�->OnConfUserListReport " + strUserList);
		}

		private void OnConfMemberEnter(long nConfID, long  nTime, String szUserInfos)
		{
			if (this.callback != null) {
				this.callback.OnConfMemberEnterCallback(nConfID, nTime, szUserInfos);
			}
			Log.e("ConfRequest UI", "-->OnConfMemberEnter " + nConfID + " " + nTime + " " + szUserInfos);
			//<user id='146' uetype='1'/>
			//TODO query user name
			Intent i = new Intent(VideoActivity.JNI_EVENT_CONF_USER_CATEGORY_NEW_USER_ENTERED_ACTION);
			i.addCategory(VideoActivity.JNI_EVENT_CONF_USER_CATEGORY);
			//TODO parse uid
			int pos = szUserInfos.indexOf("'");
			int end = szUserInfos.indexOf("'", pos + 1);
			if (pos > 0 && end > 0) { 
				i.putExtra("uid", Long.parseLong(szUserInfos.subSequence(pos+1, end).toString()));
			}
			i.putExtra("name", szUserInfos);
			this.context.sendBroadcast(i);
		}
		
		private void OnConfMemberExit(long nConfID, long nTime, long nUserID)
		{
			Log.e("ImRequest UI", "浼氳鏈変汉閫�嚭-->OnConfMemberExit " + nConfID + " " + nTime + " " + nUserID);
//			Logger.i(null, "浼氳鏈変汉閫�嚭-->OnConfMemberExit " + nConfID + " " + nTime + " " + nUserID);
			//TODO query user name
			Intent i = new Intent(VideoActivity.JNI_EVENT_CONF_USER_CATEGORY_USER_EXITED_ACTION);
			i.addCategory(VideoActivity.JNI_EVENT_CONF_USER_CATEGORY);
			i.putExtra("uid", nUserID);
			i.putExtra("name", nUserID);
			this.context.sendBroadcast(i);
			
		}

		//浼氳鏈夌敤鎴风寮�殑鍥炶皟
		private void OnConfMemberLeave(long  nConfID, long  nUserID)
		{
			Log.e("ImRequest UI", "浼氳鏈変汉閫�嚭-->OnConfMemberLeave " + nConfID + " " + nUserID);
			Bundle bundle=new Bundle();
			bundle.putLong("OnConfMemberLeave", nUserID);
//			MeetingRoom.mMeetingRoom.SendMessage(Constant.USER_LEAVE_CONF, bundle);
		}

		//鎴戣璇峰嚭浼氳鐨勫洖璋�
		private void OnKickConf(int nReason)
		{
			// TODO
			Log.e("ImRequest UI", "浼氳鏈変汉閫�嚭--->OnKickConf " + nReason);
		}
		
		private void OnConfNotify(long nSrcUserID, String srcNickName, long nConfID, String subject, long nTime)
		{
			// TODO
			Log.e("ImRequest UI", "OnConfNotify " + nSrcUserID + " " + srcNickName + " " + nConfID + " " + subject + " " + nTime);
		}

		private void OnConfNotifyEnd(long nConfID){
			
		}
		
		//寰楀埌浼氳瀹ゅ垪琛�
		private void OnGetConfList(String szConfListXml)
		{
//				Logger.i(null,"寰楀埌浼氳鍒楄〃:"+szConfListXml);

		}
		
		//浼氳鎻忚堪淇℃伅琚慨鏀圭殑鍥炶皟
		private void OnModifyConfDesc(long  nConfID, String szConfDescXml)
		{
			// TODO
			Log.e("ImRequest UI", "OnModifyConfDesc " + nConfID + " " + szConfDescXml);
		}

		//閫氱煡涓诲腑鏌愪汉鐢宠鎺у埗鏉冨洖璋�
		private void OnNotifyChair(long  userid, int type)
		{
			Log.e("ImRequest UI", "OnNotifyChair " + userid + " " + type);
			
		}

		//閫氱煡浼氳鎴愬憳鏌愪汉鑾峰緱鏌愮鏉冮檺
		private void OnGrantPermission(long  userid,int type,int status)
		{
			//type 1       3 鍙戣█                     status 1 鐢宠      2 鍙栨秷     3 鍙栧緱 
			Log.e("ImRequest UI", "OnGrantPermission " + userid + " " + type + " " + status);
		}

		//鍚屾鎵撳紑鏌愪汉瑙嗛	
		private void OnConfSyncOpenVideo(long  nDstUserID, String sDstMediaID, int nPos)
		{
			Log.e("ImRequest UI", "OnConfSyncOpenVideo " + sDstMediaID);
		}

		private void OnConfPollingOpenVideo(long  nToUserID, String sToDevID, long  nFromUserID, String sFromDevID)
		{
			// TODO
			Log.e("ImRequest UI", "OnConfPollingOpenVideo " + nToUserID + " " + sToDevID + " " + nFromUserID + " " + sFromDevID );
		}

		//鍚屾鍙栨秷鏌愪汉瑙嗛
		private void OnConfSyncCloseVideo(long  nDstUserID, String sDstMediaID, boolean bClose)
		{
			Log.e("ImRequest UI", "OnConfSyncCloseVideo " + nDstUserID + " " + sDstMediaID + " " + bClose);
		}
		
		private void OnSetConfMode(long confid,int mode){
			Log.e("ImRequest UI", "OnConfSyncCloseVideo " + confid + " " + mode);
		}
		
		private void OnConfChairChanged(long nConfID, long nChairID)
		{
			Log.e("ImRequest UI", "OnConfChairChanged " + nConfID + " " + nChairID);
		}
		
		private void OnSetCanOper(long nConfID, boolean bCanOper)
		{
			Log.e("ImRequest UI", "OnSetCanOper " + nConfID + " " + bCanOper);
		}
		
		private void OnSetCanInviteUser(long nConfID, boolean bInviteUser)
		{
			Log.e("ImRequest UI", "OnSetCanInviteUser " + nConfID + " " + bInviteUser);
		}
}
