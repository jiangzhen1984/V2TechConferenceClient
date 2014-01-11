package com.V2.jni;

//import com.xinlan.im.bean.msgtype.AudioAcceptMsgType;
//import com.xinlan.im.bean.msgtype.AudioRefuseMsgType;
//import com.xinlan.im.bean.msgtype.MsgType;
//import com.xinlan.im.ui.SplashActivity;
//import com.xinlan.im.ui.chat.VideoChatActivity;
//import com.xinlan.im.utils.Constant;

import android.content.Context;
import android.util.Log;

public class AudioRequest
{
	private Context context;
	private static AudioRequest mAudioRequest;
	private AudioRequest(Context context){
		this.context=context;
	};
	
	public static synchronized AudioRequest getInstance(Context context){
		if(mAudioRequest==null)
		{
			mAudioRequest=new AudioRequest(context);
			if (!mAudioRequest.initialize(mAudioRequest)){
				Log.e("AudioRequest", "can't initialize AudioRequest ");
			}
		}
		return mAudioRequest;
	}
	
	public native boolean initialize(AudioRequest request);
	public native void unInitialize();
	
	
	//����Է���ʼ��Ƶͨ��
	public native void InviteAudioChat(long  nGroupID, long  nToUserID,int businesstype);
	//���ܶԷ�����Ƶͨ������
	public native void AcceptAudioChat(long  nGroupID, long  nToUserID,int businesstype);
	//�ܾ�Է�����Ƶͨ������
	public native void RefuseAudioChat(long  nGroupID, long  nToUserID,int businesstype);
	//ȡ����Ƶͨ��
	public native void CloseAudioChat(long nGroupID, long nToUserID,int businesstype);
	
	public native void MuteMic(long nGroupID, long nUserID, boolean bMute,int businesstype);
	
	
	
	
	
	
	
	
	
	//�յ���Ƶͨ��������Ļص�
	private void OnAudioChatInvite(long  nGroupID, long nBusinessType, long  nFromUserID)
	{
		Log.e("ImRequest UI", "OnAudioChaInvite " + nGroupID + ":" + nBusinessType+":"+nFromUserID);
		//�յ��Է�����Ƶ����
//		Intent intent=new Intent(VideoChatActivity.VIDEOCHAT);
//		intent.putExtra("videochat", Constant.AUDIOCHATINVITE);
//		context.sendBroadcast(intent);
		
		AcceptAudioChat(nGroupID, nFromUserID, 2);
	}

	//��Ƶͨ�����뱻�Է����ܵĻص�
	private void OnAudioChatAccepted(long  nGroupID, long nBusinessType, long  nFromUserID)
	{
		Log.e("ImRequest UI", "OnAudioChatAccepted " + nGroupID + ":" + nBusinessType+":"+nFromUserID);
		
		//ƴװ��Ϣ
//		AudioAcceptMsgType videoMsgType=new AudioAcceptMsgType();
//		videoMsgType.setnFromuserID(nFromUserID);
//		
//		Intent intent=new Intent(SplashActivity.IM);
//		intent.putExtra("MsgType", MsgType.AUIDOACCEPT_CHAT);
//		intent.putExtra("MSG", videoMsgType);
//		context.sendBroadcast(intent);
	}

	//��Ƶͨ�����뱻�Է��ܾ�Ļص�
	private void OnAudioChatRefused(long  nGroupID, long nBusinessType, long  nFromUserID)
	{
		Log.e("ImRequest UI", "OnAudioChatRefused " + nGroupID + ":" + nBusinessType+":"+nFromUserID);
		
		//ƴװ��Ϣ
//		AudioRefuseMsgType videoMsgType=new AudioRefuseMsgType();
//		videoMsgType.setnFromuserID(nFromUserID);
//		
//		Intent intent=new Intent(SplashActivity.IM);
//		intent.putExtra("MsgType", MsgType.AUIDOACCEPT_CHAT);
//		intent.putExtra("MSG", videoMsgType);
//		context.sendBroadcast(intent);
	}

	//��Ƶͨ�����ر���ص�
	private void OnAudioChatClosed(long  nGroupID, long nBusinessType, long  nFromUserID)
	{
		Log.e("ImRequest UI", "OnAudioChatClosed " + nGroupID + ":" + nBusinessType+":"+nFromUserID);
	}

	//��Ƶͨ��������
	private void OnAudioChating(long  nGroupID, long nBusinessType, long  nFromUserID)
	{
		Log.e("ImRequest UI", "OnAudioChating " + nGroupID + ":" + nBusinessType+":"+nFromUserID);
	}
}
