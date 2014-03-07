package com.V2.jni;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.xinlan.im.bean.Conf;
import com.xinlan.im.utils.Logger;
import com.xinlan.im.utils.XmlParserUtils;


public class ConfRequest
{
	
	private static ConfRequest mConfRequest;
	private Activity context;
	
	private ConfRequest(Activity context){
		this.context = context;
	};
	
	public static synchronized ConfRequest getInstance(Activity context){
		if(mConfRequest==null){
			mConfRequest=new ConfRequest(context);
		}
		
		return mConfRequest;
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
	//删除会议
	public native void destroyConf(long nConfID);
	//加入一个会议
//	public native void enterConf(long nConfID, String szPassword);
	public native void enterConf(long nConfID);
	//退出一个会议
	public native void exitConf(long nConfID);
	//离开一个会议
	public native void leaveConf(long nConfID);
	//将某人请出会议
	public native void kickConf(long nUserID);
	//邀请加入会议
	public native void inviteJoinConf(long nDstUserID);
	//获取会议用户列表(未进入会议）
	public native void getConfUserList(long nConfID);
	//重新开始一个会议
	public native void resumeConf(String sXmlConfData, String sXmlInviteUsers);
	//会场静音
	public native void muteConf();
	//修改会议描述
	public native void modifyConfDesc(String sXml);
	//修改会议同步
	public native void setConfSync(int syncMode);
	//取得会议列表
	public native void getConfList();
	//申请控制权
	public native void applyForControlPermission(int type);
	//释放控制权
	public native void releaseControlPermission(int type);
	//主席授予某人权限
	public native void grantPermission(long userid, int type, int status);
	//设置其他用户视频采集参数
	public native void setCapParam(String szDevID, int nSizeIndex, int nFrameRate, int nBitRate);
	//同步打开某人视频
	public native void syncConfOpenVideo(long nGroupID, long nToUserID, String szDeviceID, int nPos);
	//同步取消某人视频
	public native void cancelSyncConfOpenVideo(long nGroupID, long nToUserID, String szDeviceID, boolean bCloseVideo);
	//轮询打开某人视频
	public native void pollingConfOpenVideo(long nGroupID, long nToUserID, String sToDevID, long  nFromUserID, String sFromDevID);
	//置换会议主席
	public native void changeConfChair(long nGroupID, long nUserID);
	
	

//	public List<Conf> confs = new ArrayList<Conf>();
	
	public boolean enterConf = false;
	
	
	//我加入会议的回调
	private void OnEnterConf(long nConfID, long nTime, String szConfData, int nJoinResult)
	{
			
	}
	
	private void OnAddUser(long userid,String xml){
		Log.e("ImRequest UI", "OnAddUser " + userid+"  "+xml);
	}
	
	private void OnConfMute(){
		Log.e("ImRequest UI", "OnConfMute ");
	}

	//会议被删除的回调
		private void OnDestroyConf(long nConfID)
		{
			Log.e("ImRequest UI", "OnDestroyConf " + nConfID);
		}

		//我收到加入会议的邀请的回调
		private void OnInviteJoinConf(String confXml, String userXml)
		{
			System.out.println(confXml+";"+userXml);
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
			Log.e("ImRequest UI", "会议内好友列表-->OnConfUserListReport " + strUserList);
		}

		private void OnConfMemberEnter(long nConfID, long  nTime, String szUserInfos)
		{
			Log.e("ImRequest UI", "会议有人进入-->OnConfMemberEnter " + nConfID + " " + nTime + " " + szUserInfos);
		}
		
		private void OnConfMemberExit(long nConfID, long nTime, long nUserID)
		{
			Log.e("ImRequest UI", "会议有人退出-->OnConfMemberExit " + nConfID + " " + nTime + " " + nUserID);
			Logger.i(null, "会议有人退出-->OnConfMemberExit " + nConfID + " " + nTime + " " + nUserID);
		}

		//会议有用户离开的回调
		private void OnConfMemberLeave(long  nConfID, long  nUserID)
		{
			Log.e("ImRequest UI", "会议有人退出-->OnConfMemberLeave " + nConfID + " " + nUserID);
			Bundle bundle=new Bundle();
			bundle.putLong("OnConfMemberLeave", nUserID);
//			MeetingRoom.mMeetingRoom.SendMessage(Constant.USER_LEAVE_CONF, bundle);
		}

		//我被请出会议的回调
		private void OnKickConf(int nReason)
		{
			// TODO
			Log.e("ImRequest UI", "会议有人退出--->OnKickConf " + nReason);
		}
		
		private void OnConfNotify(long nSrcUserID, String srcNickName, long nConfID, String subject, long nTime)
		{
			// TODO
			Log.e("ImRequest UI", "OnConfNotify " + nSrcUserID + " " + srcNickName + " " + nConfID + " " + subject + " " + nTime);
		}

		private void OnConfNotifyEnd(long nConfID){
			
		}
		
		//得到会议室列表
		private void OnGetConfList(String szConfListXml)
		{
				Logger.i(null,"得到会议列表:"+szConfListXml);

		}
		
		//会议描述信息被修改的回调
		private void OnModifyConfDesc(long  nConfID, String szConfDescXml)
		{
			// TODO
			Log.e("ImRequest UI", "OnModifyConfDesc " + nConfID + " " + szConfDescXml);
		}

		//通知主席某人申请控制权回调
		private void OnNotifyChair(long  userid, int type)
		{
			Log.e("ImRequest UI", "OnNotifyChair " + userid + " " + type);
			
		}

		//通知会议成员某人获得某种权限
		private void OnGrantPermission(long  userid,int type,int status)
		{
			//type 1       3 发言                     status 1 申请      2 取消     3 取得 
			Log.e("ImRequest UI", "OnGrantPermission " + userid + " " + type + " " + status);
		}

		//同步打开某人视频	
		private void OnConfSyncOpenVideo(long  nDstUserID, String sDstMediaID, int nPos)
		{
			Log.e("ImRequest UI", "OnConfSyncOpenVideo " + sDstMediaID);
		}

		private void OnConfPollingOpenVideo(long  nToUserID, String sToDevID, long  nFromUserID, String sFromDevID)
		{
			// TODO
			Log.e("ImRequest UI", "OnConfPollingOpenVideo " + nToUserID + " " + sToDevID + " " + nFromUserID + " " + sFromDevID );
		}

		//同步取消某人视频
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
