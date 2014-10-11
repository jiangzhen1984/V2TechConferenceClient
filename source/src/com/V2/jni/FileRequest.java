package com.V2.jni;

//import com.xinlan.im.bean.msgtype.FileTransAccepted_MsgType;
//import com.xinlan.im.bean.msgtype.InvitedMsgType;
//import com.xinlan.im.bean.msgtype.MsgType;
//import com.xinlan.im.ui.SplashActivity;
//import com.xinlan.im.ui.chat.FileDownloadActivity;
//import com.xinlan.im.utils.Constant;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;

import com.V2.jni.ind.FileJNIObject;
import com.V2.jni.ind.GroupFileJNIObject;
import com.V2.jni.ind.V2Group;
import com.V2.jni.ind.V2User;
import com.V2.jni.util.V2Log;

public class FileRequest {

	public static final int BT_CONF = 1;
	public static final int BT_IM = 2;

	private Context context;
	private static FileRequest mFileRequest;
	private String TAG = "FileRequest UI";

	private List<WeakReference<FileRequestCallback>> mCallbacks;

	private FileRequest(Context context) {
		this.context = context;
		mCallbacks = new ArrayList<WeakReference<FileRequestCallback>>();
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

	public void addCallback(FileRequestCallback callback) {
		this.mCallbacks.add(new WeakReference<FileRequestCallback>(callback));
	}
	
	
	public void removeCallback(FileRequestCallback callback) {
		for (int i = 0; i < mCallbacks.size(); i++) {
			if (mCallbacks.get(i).get() == callback) {
				mCallbacks.remove(i);
				break;
			}
		}
	}

	public native boolean initialize(FileRequest request);

	public native void unInitialize();

	/**
	 * Send file to user
	 * 
	 * @param nUserId
	 *            user Id
	 * @param filePath
	 *            <file id="" name="{FILE PATH}" encrypttype="0"/>
	 * @param linetype
	 *            2: OFFLINE 1:ONLINE
	 */
	public native void inviteFileTrans(long nUserId, String filePath,
			int linetype);

	/**
	 * TODO add comments
	 * 
	 * @param szFileID
	 * @param szSavePath
	 */
	public native void acceptFileTrans(String szFileID, String szSavePath);

	/**
	 * TODO add comments
	 * 
	 * @param szFileID
	 */
	public native void refuseFileTrans(String szFileID);

	/**
	 * 
	 * @param szFileID
	 */
	public native void cancelFileTrans(String szFileID);

	/**
	 * 
	 * @param szFileID
	 */
	public native void cancelRecvFile(String szFileID, int type);


	// 鍒犻櫎鏂囦欢
	public native void delFile(String szFileID, int businesstype);


	public native void cancelSendFile(String szFileID, int type);

	public native void cancelP2PRecvFile(String szFileID, int type);

	public native void cancelHttpRecvFile(String szFileID, int type);

	public native void resumeSendFile(String szFileID, int type);

	public native void pauseSendFile(String szFileID, int type);

	public native void resumeHttpRecvFile(String szFileID, int type);

	public native void pauseHttpRecvFile(String szFileID, int type);

	/**
	 * 下载失败，重新下载时调用
	 * @param url
	 * @param sfileid
	 * @param filePath
	 * @param encrypttype
	 * @param businessType 填1即可
	 */
	public native void httpDownloadFile(String url, String sfileid,
			String filePath, int encrypttype, int businessType);

	/**
	 * Receive the Files from the others , but not contain group's files..
	 * 
	 * @param userid
	 * @param szFileID
	 * @param szFileName
	 * @param nFileBytes
	 * @param linetype
	 *            Whether it is online transfer
	 */
	private void OnFileTransInvite(long userid, String szFileID,
			String szFileName, long nFileBytes, int linetype) {
		V2Log.e("FileTrans UI", "OnFileTransInvite ---> userid :" + userid
				+ " | szFileID: " + szFileID + " | szFileName: " + szFileName
				+ " | nFileBytes: " + nFileBytes);
		for (int i = 0; i < mCallbacks.size(); i++) {
			WeakReference<FileRequestCallback> wrf = mCallbacks.get(i);
			if (wrf != null && wrf.get() != null) {
				((FileRequestCallback) wrf.get())
						.OnFileTransInvite(new FileJNIObject(new V2User(userid), szFileID,
								szFileName, nFileBytes, linetype));
			}
		}
	}

	// 鏀跺埌鎴戠殑鏂囦欢浼犺緭閭�琚鏂规帴鍙楃殑鍥炶皟
	private void OnFileTransAccepted(String szFileID) {
		Log.e(TAG, "OnFileTransAccepted--->" + szFileID);

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

	/**
	 * 
	 * @param nGroupID
	 * @param nBusinessType
	 * @param szFileID
	 */
	private void OnFileTransNotifyDel(long nGroupID, int nBusinessType,
			String szFileID) {
		Log.e(TAG, "OnFileTransNotifyDel--->" + nGroupID + ":" + nBusinessType
				+ ":" + szFileID);
		
		FileJNIObject file = null;
		if (nGroupID > 0) {
			//Use default type 
			V2Group group = new V2Group(nGroupID, V2Group.TYPE_CROWD);
			file = new GroupFileJNIObject(group,  szFileID); 
		} else {
			file = new FileJNIObject(null, szFileID, "", 0, 0); 
		}
		
		
		for (int i = 0; i < mCallbacks.size(); i++) {
			WeakReference<FileRequestCallback> wrf = mCallbacks.get(i);
			if (wrf != null && wrf.get() != null) {
				((FileRequestCallback) wrf.get()).OnFileDeleted(file);
			}
		}
	}

	// 鏀跺埌鏂囦欢浼犺緭寮�鐨勫洖璋�
	private void OnFileTransBegin(String szFileID, int nTransType,
			long nFileSize) {
		Log.e(TAG, "OnFileTransBegin--->" + szFileID + ":" + nTransType + ":"
				+ nFileSize);
	}

	/**
	 * 
	 * @param szFileID
	 * @param nBytesTransed
	 * @param nTransType
	 */
	private void OnFileTransProgress(String szFileID, long nBytesTransed,
			int nTransType) {
		V2Log.e(TAG, "OnFileTransProgress ---> szFileID :" + szFileID
				+ " | nBytesTransed: " + nBytesTransed + " | nTransType: "
				+ nTransType);

		for (int i = 0; i < mCallbacks.size(); i++) {
			WeakReference<FileRequestCallback> wrf = mCallbacks.get(i);
			if (wrf != null && wrf.get() != null) {
				((FileRequestCallback) wrf.get()).OnFileTransProgress(szFileID,
						nBytesTransed, nTransType);
			}
		}
	}

	/**
	 * 
	 * @param szFileID
	 * @param szFileName
	 * @param nFileSize
	 * @param nTransType
	 */
	private void OnFileTransEnd(String szFileID, String szFileName,
			long nFileSize, int nTransType, String tr) {
		V2Log.e(TAG, "OnFileTransEnd ---> szFileID :" + szFileID
				+ " | szFileName: " + szFileName + " | nFileSize: " + nFileSize
				+ " | nTransType: " + nTransType + " | tr: " + tr);
		for (int i = 0; i < mCallbacks.size(); i++) {
			WeakReference<FileRequestCallback> wrf = mCallbacks.get(i);
			if (wrf != null && wrf.get() != null) {
				((FileRequestCallback) wrf.get()).OnFileTransEnd(szFileID,
						szFileName, nFileSize, nTransType);
			}
		}
	}

	/**
	 * 当发送文件或下载文件出错时，会回调该函数
	 * @param szFileID
	 * @param errorCode
	 * @param nTransType
	 */
	private void OnFileTransError(String szFileID, int errorCode, int nTransType) {
		V2Log.e(TAG, "OnFileTransError ---> szFileID :" + szFileID
				+ " | errorCode: " + errorCode + " | nTransType: " + nTransType);
		for (int i = 0; i < mCallbacks.size(); i++) {
			WeakReference<FileRequestCallback> wrf = mCallbacks.get(i);
			if (wrf != null && wrf.get() != null) {
				((FileRequestCallback) wrf.get()).OnFileTransError(szFileID,
						errorCode, nTransType);
			}
		}
	}

	// 鏀跺埌瀵规柟鍙栨秷鏂囦欢浼犺緭鍥炶皟
	private void OnFileTransCancel(String szFileID) {
		V2Log.e(TAG, "OnFileTransCancel ---> szFileID :" + szFileID);
		for (int i = 0; i < mCallbacks.size(); i++) {
			WeakReference<FileRequestCallback> wrf = mCallbacks.get(i);
			if (wrf != null && wrf.get() != null) {
				((FileRequestCallback) wrf.get()).OnFileTransCancel(szFileID);
			}
		}
	}

	/**
	 * 弃用
	 * @param sFileID
	 * @param errorCode
	 * @param nTransType 1 send 2 download
	 */
	private void OnFileDownloadError(String sFileID, int errorCode, int nTransType) {
		V2Log.e(TAG, "OnFileDownloadError ---> szFileID :" + sFileID
				+ " | errorCode: " + errorCode + " | nTransType: " + nTransType);
		for (int i = 0; i < mCallbacks.size(); i++) {
			WeakReference<FileRequestCallback> wrf = mCallbacks.get(i);
			if (wrf != null && wrf.get() != null) {
				((FileRequestCallback) wrf.get()).OnFileDownloadError(sFileID,
						errorCode , nTransType );
			}
		}
	}

}
