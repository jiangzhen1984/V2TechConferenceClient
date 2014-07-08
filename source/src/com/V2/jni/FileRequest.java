package com.V2.jni;

//import com.xinlan.im.bean.msgtype.FileTransAccepted_MsgType;
//import com.xinlan.im.bean.msgtype.InvitedMsgType;
//import com.xinlan.im.bean.msgtype.MsgType;
//import com.xinlan.im.ui.SplashActivity;
//import com.xinlan.im.ui.chat.FileDownloadActivity;
//import com.xinlan.im.utils.Constant;
import android.content.Context;

import android.util.Log;

public class FileRequest {
	private Context context;
	private static FileRequest mFileRequest;
	private String TAG = "FileRequest UI";

	private FileRequest(Context context) {
		this.context = context;
	}

	public static synchronized FileRequest getInstance(Context context) {

		if (mFileRequest == null) {
			mFileRequest = new FileRequest(context);
			mFileRequest.initialize(mFileRequest);
		}
		return mFileRequest;
	}

	public static synchronized FileRequest getInstance() {
		return mFileRequest;
	}

	public native boolean initialize(FileRequest request);

	public native void unInitialize();

	// 鍙戦�鏂囦欢
	// 閭�浠栦汉寮�鏂囦欢浼犺緭

	public native void inviteFileTrans(long nGroupID, String szToUserXml,
			 int businesstype);

	// 鎺ュ彈瀵规柟鐨勬枃浠朵紶杈撻個璇�
	public native void acceptFileTrans(String szFileID, String szSavePath,
			int businesstype);

	// 鎷掔粷鎺ユ敹鏂囦欢
	public native void refuseFileTrans(String szFileID, int businesstype);

	// 鍙栨秷鏂囦欢浼犺緭
	public native void cancelFileTrans(String szFileID, int businesstype);

	// 涓嬭浇鏂囦欢
	public native void downLoadFile(long nGroupID, String szFileID,
			String szPathName, int businesstype);

	// 鍒犻櫎鏂囦欢
	public native void delFile(String szFileID, int businesstype);

	// 鍙栨秷缇ょ粍鏂囦欢浼犺緭
	public native void cancelGroupFile(String szFileID, int type,
			int businesstype);

	// 缇ょ粍鏂囦欢缁紶
	public native void resumeGroupFile(String szFileID, int type,
			int businesstype);

	// 缇ょ粍鏂囦欢鏆傚仠浼犺緭
	public native void pauseGroupFile(String szFileID, int type,
			int businesstype);

	private native void cancelSendFile(String patch, int type);

	private native void cancelP2PRecvFile(String patch, int type);

	private native void cancelHttpRecvFile(String patch, int type);

	private native void resumeSendFile(String patch, int type);

	private native void pauseSendFile(String patch, int type);

	private native void resumeHttpRecvFile(String patch, int type);

	private native void pauseHttpRecvFile(String patch, int type);

	private native void httpDownloadFile(String patch, String patch1,
			String patch2, int i, int i1);

	// 鏀跺埌浠栦汉鐨勬枃浠朵紶杈撻個璇风殑鍥炶皟
	/*
	 * OnFileTransInvite--->
	 * 
	 * 0:2:userid{AB2C7E63-1AA3-4688-BAD2-97920B155F43}:C:\Users\qiang\Desktop\old7
	 * \libv2ve.so:359180:0
	 */
	private void OnFileTransInvite(long nGroupID, int nBusinessType,
			long userid, String szFileID, String szFileName, long nFileBytes,
			int linetype) {
		Log.e(TAG, "OnFileTransInvite--->" + nGroupID + ":" + nBusinessType
				+ ":" + userid + ":" + szFileID + ":" + szFileName + ":"
				+ nFileBytes + ":" + linetype);

		// //鎷艰淇℃伅
		// InvitedMsgType inviteMsgType=new InvitedMsgType();
		// inviteMsgType.setnFileBytes(nFileBytes);
		// inviteMsgType.setSzFileID(szFileID);
		// inviteMsgType.setSzFileName(szFileName);
		// inviteMsgType.setUserid(userid);
		// inviteMsgType.setLinetype(linetype);
		//
		// Intent intent=new Intent(SplashActivity.IM);
		// intent.putExtra("MsgType", MsgType.INVITED_FILE);
		// intent.putExtra("MSG", inviteMsgType);
		// context.sendBroadcast(intent);
		//
		// File path=new
		// File(Constant.getInstance(context).getFilesDir(userid));
		// if(!path.exists()){
		// path.mkdirs();
		// }
	}

	// 鏀跺埌鎴戠殑鏂囦欢浼犺緭閭�琚鏂规帴鍙楃殑鍥炶皟
	private void OnFileTransAccepted(int nBusinessType, String szFileID) {
		Log.e(TAG, "OnFileTransAccepted--->" + nBusinessType + ":" + szFileID);

		// //鎷艰淇℃伅
		// FileTransAccepted_MsgType acceptMsgType=new
		// FileTransAccepted_MsgType();
		// acceptMsgType.setSzFileID(szFileID);
		//
		// Intent intent=new Intent(SplashActivity.IM);
		// intent.putExtra("MsgType", MsgType.ACCEPT_FILE);
		// intent.putExtra("MSG", acceptMsgType);
		// context.sendBroadcast(intent);
	}

	// 瀵规柟鎷掔粷鎺ユ敹鏂囦欢鍥炶皟
	private void OnFileTransRefuse(String szFileID) {
		Log.e(TAG, "OnFileTransRefuse--->" + szFileID);
	}

	// 鏀跺埌浠栦汉缁欐垜涓婁紶鐨勬枃浠剁殑閫氱煡鐨勫洖璋�
	private void OnFileTransNotify(long nGroupID, int nBusinessType,
			long nFromUserID, String szFileID, String szFileName,
			long nFileBytes) {
		Log.e(TAG, "OnFileTransNotify--->" + nGroupID + ":" + nBusinessType
				+ ":" + nFromUserID + ":" + szFileID + ":" + szFileName + ":"
				+ nFileBytes);
	}

	// 閫氱煡鍒犻櫎涓婁紶鐨勬枃浠�
	private void OnFileTransNotifyDel(long nGroupID, int nBusinessType,
			String szFileID) {
		Log.e(TAG, "OnFileTransNotifyDel--->" + nGroupID + ":" + nBusinessType
				+ ":" + szFileID);
	}

	// 鏀跺埌鏂囦欢浼犺緭寮�鐨勫洖璋�
	private void OnFileTransBegin(String szFileID, int nTransType,
			long nFileSize) {
		Log.e(TAG, "OnFileTransBegin--->" + szFileID + ":" + nTransType + ":"
				+ nFileSize);
	}

	// 鏀跺埌鏂囦欢浼犺緭杩涘害鐨勫洖璋僌nFileTransProgress--->{D27BDEE0-7C5B-40B2-8DF2-7DA3EECCD831}:368640:368640

	private void OnFileTransProgress(String szFileID, long nBytesTransed,
			int nTransType) {
		Log.e(TAG, "OnFileTransProgress--->" + szFileID + ":" + nBytesTransed
				+ ":" + nTransType);

		// if(FileDownloadActivity.mFileActivity!=null){
		// Bundle bundle=new Bundle();
		// bundle.putLong("progress",nBytesTransed);
		// bundle.putString("fileid",szFileID);
		// FileDownloadActivity.mFileActivity.SendMessage(Constant.FILE_PROGRESS,
		// bundle);
		// }
	}

	// 鏀跺埌鏂囦欢浼犺緭瀹屾垚鐨勫洖璋�
	// {D27BDEE0-7C5B-40B2-8DF2-7DA3EECCD831}:/mnt/sdcard/XinLan_IM/files/ConfSession:1611176:2

	private void OnFileTransEnd(String szFileID, String szFileName,
			long nFileSize, int nTransType) {
		Log.e(TAG, "OnFileTransEnd--->" + szFileID + ":" + szFileName + ":"
				+ nFileSize + ":" + nTransType);
		// if(FileDownloadActivity.mFileActivity!=null){
		// Bundle bundle=new Bundle();
		// bundle.putString("path",szFileName);
		// bundle.putString("fileid",szFileID);
		// FileDownloadActivity.mFileActivity.SendMessage(Constant.FILE_DOWN_SUCCESS,
		// bundle);
		// }
	}

	// 鏀跺埌瀵规柟鍙栨秷鏂囦欢浼犺緭鍥炶皟
	private void OnFileTransCancel(String szFileID) {
		Log.e(TAG, "OnFileTransCancel--->" + szFileID);
	}

	// 鏂囦欢浼犺緭澶辫触
	private void OnFileDownloadError(String sFileID) {
		Log.e(TAG, "OnFileDownloadError--->" + sFileID);
		// if(FileDownloadActivity.mFileActivity!=null){
		// Bundle bundle=new Bundle();
		// bundle.putString("fileid",sFileID);
		// FileDownloadActivity.mFileActivity.SendMessage(Constant.FILE_DOWN_FAILD,
		// bundle);
		// }
	}

}
