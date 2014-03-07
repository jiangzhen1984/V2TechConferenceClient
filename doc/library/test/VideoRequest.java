package com.V2.jni;

import java.io.ByteArrayInputStream;

import v2av.VideoPlayer;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.xinlan.im.adapter.XiuLiuApplication;
import com.xinlan.im.bean.msgtype.MsgType;
import com.xinlan.im.bean.msgtype.VideoAcceptMsgType;
import com.xinlan.im.bean.msgtype.VideoInviteMsgType;
import com.xinlan.im.bean.msgtype.VideoRefuseMsgType;
import com.xinlan.im.ui.SplashActivity;
import com.xinlan.im.ui.chat.HandleVideoInviteActivity;
import com.xinlan.im.ui.chat.VideoChatActivity;
import com.xinlan.im.utils.Constant;
import com.xinlan.im.utils.XmlParserUtils;


public class VideoRequest
{
	private static VideoRequest mVideoRequest;
	private XiuLiuApplication app;
	private Activity context;
	private VideoRequest(Activity context){
		this.context=context;
		app=(XiuLiuApplication) context.getApplication();
	};
	
	public static synchronized VideoRequest getInstance(Activity context){
		if(mVideoRequest==null){
			mVideoRequest=new VideoRequest(context);
		}
		
		return mVideoRequest;
	}
	
	
	
	public native boolean initialize(VideoRequest request);
	public native void unInitialize();
	  
	//枚举摄像头	
	public native void enumMyVideos(int p); 
	//设置本地摄像头
	public native void setDefaultVideoDev(String szDeviceID);
	//邀请他人开始视频会话
	public native void inviteVideoChat(long nGroupID, long nToUserID, String szDeviceID, int businessType);
	//接受对方的视频会话邀请
	public native void acceptVideoChat(long nGroupID, long nToUserID, String szDeviceID, int businessType);
	//拒绝对方的视频会话邀请
	public native void refuseVideoChat(long nGroupID, long nToUserID, String szDeviceID, int businessType);
	//取消视频会话
	public native void cancelVideoChat(long nGroupID, long nToUserID, String szDeviceID, int businessType);
	//关闭视频会话
	public native void closeVideoChat(long nGroupID, long nToUserID, String szDeviceID, int businessType);
	
	//设置远端视频显示窗口
	public native void openVideoDevice(long nGroupID, long nUserID, String szDeviceID, VideoPlayer vp, int businessType);
	public native void closeVideoDevice(long nGroupID, long nUserID, String szDeviceID, VideoPlayer vp, int businessType);
	
	
	// 设置采集参数
	public native void setCapParam(String szDevID, int nSizeIndex, int nFrameRate, int nBitRate);
	
	
	private void OnRemoteUserVideoDevice(String szXmlData)
	{
		Log.e("ImRequest UI", "OnRemoteUserVideoDevice:---"+szXmlData);
		
		//解析一个用户的设备，放入到容器中
		XmlParserUtils.parserVideodevice(app.getVideodevices_map(),new ByteArrayInputStream(szXmlData.getBytes()));
			
	}
	
	//收到对方的视频通话请求                   参数（0，2，1112628， 1112628：Integrated Camera__2889200338）
	private void OnVideoChatInvite(long nGroupID, int nBusinessType, long nFromUserID, String szDeviceID)
	{
		Log.e("ImRequest UI", "OnVideoChatInvite " + nGroupID + " " + nBusinessType + " " + nFromUserID + " " + szDeviceID);
		//拼装信息
		VideoInviteMsgType videoinviteMsgType=new VideoInviteMsgType();
		videoinviteMsgType.setnFromuserID(nFromUserID);
		videoinviteMsgType.setSzDeviceID(szDeviceID);
		
		Intent videoinvite_intent=new Intent(SplashActivity.IM);
		videoinvite_intent.putExtra("MsgType", MsgType.VIDEO_INVITE);
		videoinvite_intent.putExtra("MSG", videoinviteMsgType);
		context.sendBroadcast(videoinvite_intent);
	}
	
	//邀请别人后得到应答: OnVideoChatAccepted 0 2 1112627 1112627:Integrated Camera____2889200338

	private void OnVideoChatAccepted(long nGroupID, int nBusinessType, long nFromuserID, String szDeviceID)
	{
		Log.e("ImRequest UI", "OnVideoChatAccepted " + nGroupID + " " + nBusinessType + " " + nFromuserID + " " + szDeviceID);

		//拼装信息
		VideoAcceptMsgType videoMsgType=new VideoAcceptMsgType();
		videoMsgType.setnFromuserID(nFromuserID);
		videoMsgType.setSzDeviceID(szDeviceID);
		
		Intent intent=new Intent(VideoChatActivity.VIDEOCHAT);
		intent.putExtra("MsgType", MsgType.VIDEOACCEPT_CHAT);
		intent.putExtra("MSG", videoMsgType);
		intent.putExtra("videochat", Constant.VIDEOCHATACCEPTED);
		context.sendBroadcast(intent);
	}
	
	
	
	
	//邀请别人后遭到拒绝的 回掉       OnVideoChatRefused 0 2 1112627 
	private void OnVideoChatRefused(long nGroupID, int nBusinessType, long nFromUserID, String szDeviceID)
	{
		Log.e("ImRequest UI", "OnVideoChatRefused " + nGroupID + " " + nBusinessType + " " + nFromUserID + " " + szDeviceID);
		
		
		//拼装信息
		VideoRefuseMsgType videoMsgType=new VideoRefuseMsgType();
		videoMsgType.setnFromuserID(nFromUserID);
		videoMsgType.setSzDeviceID(szDeviceID);
		
		Intent intent=new Intent(VideoChatActivity.VIDEOCHAT);
		intent.putExtra("MsgType", MsgType.VIDEOREFUSE_CHAT);
		intent.putExtra("MSG", videoMsgType);
		intent.putExtra("videochat", Constant.VIDEOCHATREFUSED);
		context.sendBroadcast(intent);
	}
	
	//收到视频会话已经建立的回调
	private void OnVideoChating(long nGroupID, int nBusinessType, long nFromUserID, String szDeviceID)
	{
		Log.e("ImRequest UI", "OnVideoChating " + nGroupID + " " + nBusinessType + " " + nFromUserID + " " + szDeviceID);
	}
	
	//收到视频会话被关闭的回调
	private void OnVideoChatClosed(long nGroupID, int nBusinessType, long nFromUserID, String szDeviceID)
	{
		Log.e("ImRequest UI", "OnVideoChatClosed " + nGroupID + " " + nBusinessType + " " + nFromUserID + " " + szDeviceID);
		
		//发送广播，通知视频通话已经结束  2代表关闭
		Intent intent=new Intent(VideoChatActivity.VIDEOCHAT);
		intent.putExtra("videochat", Constant.VIDEOCHATCLOSED);
		context.sendBroadcast(intent);
		
		Intent intent1=new Intent(HandleVideoInviteActivity.CLOSE_VIDEOCHAT1);
		intent1.putExtra("videochat", "2");
		context.sendBroadcast(intent1);
	}
	
	private void OnVideoWindowSet(String sDevId, Object hwnd)
	{
		Log.e("ImRequest UI", "OnVideoWindowSet " + sDevId +" "+hwnd.toString());
	}
	
	private void OnVideoWindowClosed(String sDevId, Object hwnd)
	{
		Log.e("ImRequest UI", "OnVideoWindowClosed " + sDevId +" "+hwnd.toString());
	}
	
//	private void OnGetDevSizeFormats(String szXml);
	
	private void OnSetCapParamDone(String szDevID, int nSizeIndex, int nFrameRate, int nBitRate)
	{
		Log.e("ImRequest UI", "OnSetCapParamDone " + szDevID +" "+nSizeIndex+" "+nFrameRate+" "+nBitRate);
	}
	
	// // 通知窗口视频比特率，单位Kbps
	private void OnVideoBitRate(Object hwnd, int bps)
	{
//		Log.e("ImRequest UI", "OnVideoBitRate " + hwnd +" "+bps);
	}
	
	//摄像头采集出错
	private void OnVideoCaptureError(String szDevID, int nErr)
	{
		Log.e("ImRequest UI", "OnVideoCaptureError " + szDevID +" "+nErr);
	}
	
	private void OnVideoPlayerClosed(String szDeviceID)
	{
		Log.e("ImRequest UI", "OnVideoPlayerClosed " + szDeviceID );
	}
}
