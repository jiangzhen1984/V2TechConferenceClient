package com.V2.jni;

import v2av.VideoPlayer;
import android.app.Activity;
import android.content.Context;
import android.util.Log;

//import com.xinlan.im.adapter.XiuLiuApplication;
//import com.xinlan.im.bean.msgtype.MsgType;
//import com.xinlan.im.bean.msgtype.VideoAcceptMsgType;
//import com.xinlan.im.bean.msgtype.VideoInviteMsgType;
//import com.xinlan.im.bean.msgtype.VideoRefuseMsgType;
//import com.xinlan.im.ui.SplashActivity;
//import com.xinlan.im.ui.chat.HandleVideoInviteActivity;
//import com.xinlan.im.ui.chat.VideoChatActivity;
//import com.xinlan.im.utils.Constant;
//import com.xinlan.im.utils.XmlParserUtils;


public class VideoRequest
{
	private static VideoRequest mVideoRequest;
//	private XiuLiuApplication app;
	private Context context;
	private VideoRequest(Context context){
		this.context=context;
//		app=(XiuLiuApplication) context.getApplication();
	};
	
	public static synchronized VideoRequest getInstance(Context context){
		if(mVideoRequest==null){
			mVideoRequest=new VideoRequest(context);
			if (!mVideoRequest.initialize(mVideoRequest)) {
				Log.e("mVideoRequest", "can't initialize mVideoRequest ");
			}
		}
		
		return mVideoRequest;
	}
	
	
	
	public native boolean initialize(VideoRequest request);
	public native void unInitialize();
	  
	//鏋氫妇鎽勫儚澶�
	public native void enumMyVideos(int p); 
	//璁剧疆鏈湴鎽勫儚澶�
	public native void setDefaultVideoDev(String szDeviceID);
	//閭�浠栦汉寮�瑙嗛浼氳瘽
	public native void inviteVideoChat(long nGroupID, long nToUserID, String szDeviceID, int businessType);
	//鎺ュ彈瀵规柟鐨勮棰戜細璇濋個璇�
	public native void acceptVideoChat(long nGroupID, long nToUserID, String szDeviceID, int businessType);
	//鎷掔粷瀵规柟鐨勮棰戜細璇濋個璇�
	public native void refuseVideoChat(long nGroupID, long nToUserID, String szDeviceID, int businessType);
	//鍙栨秷瑙嗛浼氳瘽
	public native void cancelVideoChat(long nGroupID, long nToUserID, String szDeviceID, int businessType);
	//鍏抽棴瑙嗛浼氳瘽
	public native void closeVideoChat(long nGroupID, long nToUserID, String szDeviceID, int businessType);
	
	//璁剧疆杩滅瑙嗛鏄剧ず绐楀彛
	public native void openVideoDevice(long nGroupID, long nUserID, String szDeviceID, VideoPlayer vp, int businessType);
	public native void closeVideoDevice(long nGroupID, long nUserID, String szDeviceID, VideoPlayer vp, int businessType);
	
	
	// 璁剧疆閲囬泦鍙傛暟
	public native void setCapParam(String szDevID, int nSizeIndex, int nFrameRate, int nBitRate);
	
	
	private void OnRemoteUserVideoDevice(String szXmlData)
	{
		Log.e("ImRequest UI", "OnRemoteUserVideoDevice:---"+szXmlData);
		
		//瑙ｆ瀽涓�釜鐢ㄦ埛鐨勮澶囷紝鏀惧叆鍒板鍣ㄤ腑
//		XmlParserUtils.parserVideodevice(app.getVideodevices_map(),new ByteArrayInputStream(szXmlData.getBytes()));
			
	}
	
	//鏀跺埌瀵规柟鐨勮棰戦�璇濊姹�                  鍙傛暟锛�锛�锛�112628锛�1112628锛欼ntegrated Camera__2889200338锛�
	private void OnVideoChatInvite(long nGroupID, int nBusinessType, long nFromUserID, String szDeviceID)
	{
		Log.e("ImRequest UI", "OnVideoChatInvite " + nGroupID + " " + nBusinessType + " " + nFromUserID + " " + szDeviceID);
		//鎷艰淇℃伅
//		VideoInviteMsgType videoinviteMsgType=new VideoInviteMsgType();
//		videoinviteMsgType.setnFromuserID(nFromUserID);
//		videoinviteMsgType.setSzDeviceID(szDeviceID);
//		
//		Intent videoinvite_intent=new Intent(SplashActivity.IM);
//		videoinvite_intent.putExtra("MsgType", MsgType.VIDEO_INVITE);
//		videoinvite_intent.putExtra("MSG", videoinviteMsgType);
//		context.sendBroadcast(videoinvite_intent);
	}
	
	//閭�鍒汉鍚庡緱鍒板簲绛� OnVideoChatAccepted 0 2 1112627 1112627:Integrated Camera____2889200338

	private void OnVideoChatAccepted(long nGroupID, int nBusinessType, long nFromuserID, String szDeviceID)
	{
		Log.e("ImRequest UI", "OnVideoChatAccepted " + nGroupID + " " + nBusinessType + " " + nFromuserID + " " + szDeviceID);

		//鎷艰淇℃伅
//		VideoAcceptMsgType videoMsgType=new VideoAcceptMsgType();
//		videoMsgType.setnFromuserID(nFromuserID);
//		videoMsgType.setSzDeviceID(szDeviceID);
//		
//		Intent intent=new Intent(VideoChatActivity.VIDEOCHAT);
//		intent.putExtra("MsgType", MsgType.VIDEOACCEPT_CHAT);
//		intent.putExtra("MSG", videoMsgType);
//		intent.putExtra("videochat", Constant.VIDEOCHATACCEPTED);
//		context.sendBroadcast(intent);
	}
	
	
	
	
	//閭�鍒汉鍚庨伃鍒版嫆缁濈殑 鍥炴帀       OnVideoChatRefused 0 2 1112627 
	private void OnVideoChatRefused(long nGroupID, int nBusinessType, long nFromUserID, String szDeviceID)
	{
		Log.e("ImRequest UI", "OnVideoChatRefused " + nGroupID + " " + nBusinessType + " " + nFromUserID + " " + szDeviceID);
		
		
		//鎷艰淇℃伅
//		VideoRefuseMsgType videoMsgType=new VideoRefuseMsgType();
//		videoMsgType.setnFromuserID(nFromUserID);
//		videoMsgType.setSzDeviceID(szDeviceID);
//		
//		Intent intent=new Intent(VideoChatActivity.VIDEOCHAT);
//		intent.putExtra("MsgType", MsgType.VIDEOREFUSE_CHAT);
//		intent.putExtra("MSG", videoMsgType);
//		intent.putExtra("videochat", Constant.VIDEOCHATREFUSED);
//		context.sendBroadcast(intent);
	}
	
	//鏀跺埌瑙嗛浼氳瘽宸茬粡寤虹珛鐨勫洖璋�
	private void OnVideoChating(long nGroupID, int nBusinessType, long nFromUserID, String szDeviceID)
	{
		Log.e("ImRequest UI", "OnVideoChating " + nGroupID + " " + nBusinessType + " " + nFromUserID + " " + szDeviceID);
	}
	
	//鏀跺埌瑙嗛浼氳瘽琚叧闂殑鍥炶皟
	private void OnVideoChatClosed(long nGroupID, int nBusinessType, long nFromUserID, String szDeviceID)
	{
		Log.e("ImRequest UI", "OnVideoChatClosed " + nGroupID + " " + nBusinessType + " " + nFromUserID + " " + szDeviceID);
		
		//鍙戦�骞挎挱锛岄�鐭ヨ棰戦�璇濆凡缁忕粨鏉� 2浠ｈ〃鍏抽棴
//		Intent intent=new Intent(VideoChatActivity.VIDEOCHAT);
//		intent.putExtra("videochat", Constant.VIDEOCHATCLOSED);
//		context.sendBroadcast(intent);
//		
//		Intent intent1=new Intent(HandleVideoInviteActivity.CLOSE_VIDEOCHAT1);
//		intent1.putExtra("videochat", "2");
//		context.sendBroadcast(intent1);
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
	
	// // 閫氱煡绐楀彛瑙嗛姣旂壒鐜囷紝鍗曚綅Kbps
	private void OnVideoBitRate(Object hwnd, int bps)
	{
//		Log.e("ImRequest UI", "OnVideoBitRate " + hwnd +" "+bps);
	}
	
	//鎽勫儚澶撮噰闆嗗嚭閿�
	private void OnVideoCaptureError(String szDevID, int nErr)
	{
		Log.e("ImRequest UI", "OnVideoCaptureError " + szDevID +" "+nErr);
	}
	
	private void OnVideoPlayerClosed(String szDeviceID)
	{
		Log.e("ImRequest UI", "OnVideoPlayerClosed " + szDeviceID );
	}
	
	private void OnGetVideoDevice(String xml, long l) {
		Log.e("VideoRequest UI", "OnGetVideoDevice " + xml );
	}
	
	private void OnGetVideoDevice(long l, String xml) {
		Log.e("VideoRequest UI", "OnGetVideoDevice " + xml );
	}
}
