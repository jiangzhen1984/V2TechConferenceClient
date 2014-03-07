package com.V2.jni;

import com.xinlan.im.bean.msgtype.AudioAcceptMsgType;
import com.xinlan.im.bean.msgtype.AudioRefuseMsgType;
import com.xinlan.im.bean.msgtype.MsgType;
import com.xinlan.im.ui.SplashActivity;
import com.xinlan.im.ui.chat.VideoChatActivity;
import com.xinlan.im.utils.Constant;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

public class AudioRequest
{
	private Activity context;
	private static AudioRequest mAudioRequest;
	private AudioRequest(Activity context){
		this.context=context;
	};
	
	public static synchronized AudioRequest getInstance(Activity context){
		if(mAudioRequest==null)
		{
			mAudioRequest=new AudioRequest(context);
		}
		return mAudioRequest;
	}
	
	public native boolean initialize(AudioRequest request);
	public native void unInitialize();
	
	
	//邀请对方开始音频通话
	public native void InviteAudioChat(long  nGroupID, long  nToUserID,int businesstype);
	//接受对方的音频通话邀请
	public native void AcceptAudioChat(long  nGroupID, long  nToUserID,int businesstype);
	//拒绝对方的音频通话邀请
	public native void RefuseAudioChat(long  nGroupID, long  nToUserID,int businesstype);
	//取消音频通话
	public native void CloseAudioChat(long nGroupID, long nToUserID,int businesstype);
	
	public native void MuteMic(long nGroupID, long nUserID, boolean bMute,int businesstype);
	
	
	
	
	
	
	
	
	
	//收到音频通话的邀请的回调
	private void OnAudioChatInvite(long  nGroupID, long nBusinessType, long  nFromUserID)
	{
		Log.e("ImRequest UI", "OnAudioChaInvite " + nGroupID + ":" + nBusinessType+":"+nFromUserID);
		//收到对方的音频邀请
		Intent intent=new Intent(VideoChatActivity.VIDEOCHAT);
		intent.putExtra("videochat", Constant.AUDIOCHATINVITE);
		context.sendBroadcast(intent);
		
		AcceptAudioChat(nGroupID, nFromUserID, 2);
	}

	//音频通话邀请被对方接受的回调
	private void OnAudioChatAccepted(long  nGroupID, long nBusinessType, long  nFromUserID)
	{
		Log.e("ImRequest UI", "OnAudioChatAccepted " + nGroupID + ":" + nBusinessType+":"+nFromUserID);
		
		//拼装信息
		AudioAcceptMsgType videoMsgType=new AudioAcceptMsgType();
		videoMsgType.setnFromuserID(nFromUserID);
		
		Intent intent=new Intent(SplashActivity.IM);
		intent.putExtra("MsgType", MsgType.AUIDOACCEPT_CHAT);
		intent.putExtra("MSG", videoMsgType);
		context.sendBroadcast(intent);
	}

	//音频通话邀请被对方拒绝的回调
	private void OnAudioChatRefused(long  nGroupID, long nBusinessType, long  nFromUserID)
	{
		Log.e("ImRequest UI", "OnAudioChatRefused " + nGroupID + ":" + nBusinessType+":"+nFromUserID);
		
		//拼装信息
		AudioRefuseMsgType videoMsgType=new AudioRefuseMsgType();
		videoMsgType.setnFromuserID(nFromUserID);
		
		Intent intent=new Intent(SplashActivity.IM);
		intent.putExtra("MsgType", MsgType.AUIDOACCEPT_CHAT);
		intent.putExtra("MSG", videoMsgType);
		context.sendBroadcast(intent);
	}

	//音频通话被关闭物回调
	private void OnAudioChatClosed(long  nGroupID, long nBusinessType, long  nFromUserID)
	{
		Log.e("ImRequest UI", "OnAudioChatClosed " + nGroupID + ":" + nBusinessType+":"+nFromUserID);
	}

	//音频通话进行中
	private void OnAudioChating(long  nGroupID, long nBusinessType, long  nFromUserID)
	{
		Log.e("ImRequest UI", "OnAudioChating " + nGroupID + ":" + nBusinessType+":"+nFromUserID);
	}
}
