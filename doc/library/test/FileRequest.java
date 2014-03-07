package com.V2.jni;


import java.io.File;

import com.xinlan.im.bean.msgtype.FileTransAccepted_MsgType;
import com.xinlan.im.bean.msgtype.InvitedMsgType;
import com.xinlan.im.bean.msgtype.MsgType;
import com.xinlan.im.ui.SplashActivity;
import com.xinlan.im.ui.chat.FileDownloadActivity;
import com.xinlan.im.utils.Constant;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


public class FileRequest
{
	private Context context;
	private static FileRequest mFileRequest;
	private String TAG="ImRequest UI";
	
	private FileRequest(Context context){
		this.context=context;
	}

	public static synchronized  FileRequest getInstance(Context context){
		
		if(mFileRequest==null){
			mFileRequest=new FileRequest(context);
		}
		return mFileRequest;
	}
	
	public native boolean initialize(FileRequest request);
	public native void unInitialize();
	
	//发送文件
	//邀请他人开始文件传输
	
	public native void inviteFileTrans(long  nGroupID, String szToUserXml, String szFilesXml,int linetype,int businesstype);
	//接受对方的文件传输邀请
	public native void acceptFileTrans(String szFileID, String szSavePath,int businesstype);
	//拒绝接收文件
	public native void refuseFileTrans(String szFileID,int businesstype);
	//取消文件传输
	public native void cancelFileTrans(String szFileID,int businesstype);
	//下载文件
	public native void downLoadFile(long  nGroupID, String szFileID, String szPathName,int businesstype);
	//删除文件
	public native void delFile(String szFileID,int businesstype);
	//取消群组文件传输
	public native void cancelGroupFile(String szFileID,int type,int businesstype);
	//群组文件续传
	public native void resumeGroupFile(String szFileID,int type,int businesstype);
	//群组文件暂停传输
	public native void pauseGroupFile(String szFileID,int type,int businesstype);

	//收到他人的文件传输邀请的回调
	/*
	 *OnFileTransInvite--->
	 *
	 *0:2:userid{AB2C7E63-1AA3-4688-BAD2-97920B155F43}:C:\Users\qiang\Desktop\old7\libv2ve.so:359180:0
	 */
	private void OnFileTransInvite(long  nGroupID, int nBusinessType,long userid, String szFileID, String szFileName, long  nFileBytes,int linetype)
	{
		Log.e(TAG,"OnFileTransInvite--->"+ nGroupID+":"+nBusinessType+":"+userid+":"+szFileID+":"+szFileName+":"+nFileBytes+":"+linetype);
		
		//拼装信息
				InvitedMsgType inviteMsgType=new InvitedMsgType();
				inviteMsgType.setnFileBytes(nFileBytes);
				inviteMsgType.setSzFileID(szFileID);
				inviteMsgType.setSzFileName(szFileName);
				inviteMsgType.setUserid(userid);
				inviteMsgType.setLinetype(linetype);
				
				Intent intent=new Intent(SplashActivity.IM);
				intent.putExtra("MsgType", MsgType.INVITED_FILE);
				intent.putExtra("MSG", inviteMsgType);
				context.sendBroadcast(intent);
				
		File path=new File(Constant.getInstance(context).getFilesDir(userid));
		if(!path.exists()){
			path.mkdirs();
		}
	}

	//收到我的文件传输邀请被对方接受的回调
	private void OnFileTransAccepted(int nBusinessType, String szFileID)
	{
		Log.e(TAG, "OnFileTransAccepted--->"+nBusinessType+":"+szFileID);
		
		//拼装信息
		FileTransAccepted_MsgType acceptMsgType=new FileTransAccepted_MsgType();
		acceptMsgType.setSzFileID(szFileID);
		
		Intent intent=new Intent(SplashActivity.IM);
		intent.putExtra("MsgType", MsgType.ACCEPT_FILE);
		intent.putExtra("MSG", acceptMsgType);
		context.sendBroadcast(intent);
	}

	//对方拒绝接收文件回调
	private void OnFileTransRefuse(String szFileID)
	{
		Log.e(TAG, "OnFileTransRefuse--->"+szFileID);
	}

	//收到他人给我上传的文件的通知的回调
	private void OnFileTransNotify(long  nGroupID, int nBusinessType, long  nFromUserID, String szFileID, String szFileName, long  nFileBytes)
	{
		Log.e(TAG, "OnFileTransNotify--->"+nGroupID+":"+nBusinessType+":"+nFromUserID+":"+szFileID+":"+szFileName+":"+nFileBytes);
	}

	//通知删除上传的文件
	private void OnFileTransNotifyDel(long  nGroupID, int nBusinessType, String szFileID)
	{
		Log.e(TAG, "OnFileTransNotifyDel--->"+nGroupID+":"+nBusinessType+":"+szFileID);
	}

	//收到文件传输开始的回调
	private void OnFileTransBegin(String szFileID, int nTransType,long  nFileSize)
	{
		Log.e(TAG, "OnFileTransBegin--->"+szFileID+":"+nTransType+":"+nFileSize);
	}

	//收到文件传输进度的回调OnFileTransProgress--->{D27BDEE0-7C5B-40B2-8DF2-7DA3EECCD831}:368640:368640

	private void OnFileTransProgress(String szFileID, long  nBytesTransed, int nTransType)
	{
		Log.e(TAG, "OnFileTransProgress--->"+szFileID+":"+nBytesTransed+":"+nTransType);
		
		if(FileDownloadActivity.mFileActivity!=null){
			Bundle bundle=new Bundle();
			bundle.putLong("progress",nBytesTransed);
			bundle.putString("fileid",szFileID);
			FileDownloadActivity.mFileActivity.SendMessage(Constant.FILE_PROGRESS, bundle);
		}
	}

	//收到文件传输完成的回调
	//{D27BDEE0-7C5B-40B2-8DF2-7DA3EECCD831}:/mnt/sdcard/XinLan_IM/files/ConfSession:1611176:2

	private void OnFileTransEnd(String szFileID, String szFileName, long  nFileSize, int nTransType)
	{
		Log.e(TAG, "OnFileTransEnd--->"+szFileID+":"+szFileName+":"+nFileSize+":"+nTransType);
		if(FileDownloadActivity.mFileActivity!=null){
			Bundle bundle=new Bundle();
			bundle.putString("path",szFileName);
			bundle.putString("fileid",szFileID);
			FileDownloadActivity.mFileActivity.SendMessage(Constant.FILE_DOWN_SUCCESS, bundle);
		}
	}

	//收到对方取消文件传输回调
	private void OnFileTransCancel(String szFileID)
	{
		Log.e(TAG, "OnFileTransCancel--->"+szFileID);
	}

	//文件传输失败
	private void OnFileDownloadError(String sFileID)
	{
		Log.e(TAG, "OnFileDownloadError--->"+sFileID);
		if(FileDownloadActivity.mFileActivity!=null){
			Bundle bundle=new Bundle();
			bundle.putString("fileid",sFileID);
			FileDownloadActivity.mFileActivity.SendMessage(Constant.FILE_DOWN_FAILD, bundle);
		}
	}
	
}
