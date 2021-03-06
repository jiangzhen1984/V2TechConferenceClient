package com.V2.jni;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;

import com.V2.jni.callbackInterface.WBRequestCallback;
import com.V2.jni.util.V2Log;

public class WBRequest {
	private static WBRequest mWBRequest;

	private List<WeakReference<WBRequestCallback>> mCallbacks = new ArrayList<WeakReference<WBRequestCallback>>();

	private WBRequest(Context context) {
	}

	public static synchronized WBRequest getInstance(Context context) {

		if (mWBRequest == null) {
			mWBRequest = new WBRequest(context);
			mWBRequest.initialize(mWBRequest);
		}
		return mWBRequest;
	}

	public static synchronized WBRequest getInstance() {
		if (mWBRequest == null) {
			mWBRequest = new WBRequest(null);
			mWBRequest.initialize(mWBRequest);
		}
		return mWBRequest;
	}

	public void addCallbacks(WBRequestCallback callback) {
		mCallbacks.add(new WeakReference<WBRequestCallback>(callback));
	}

	public void removeCallback(WBRequestCallback callback) {
		for (int i = 0; i < mCallbacks.size(); i++) {
			if (mCallbacks.get(i).get() == callback) {
				mCallbacks.remove(i);
				break;
			}
		}
	}

	public native boolean initialize(WBRequest request);

	public native void unInitialize();

	public native void downLoadPageDoc(String bowardid, int pageid);

	/**
	 * 取消文档打印
	 * 
	 * @param szWBoardID
	 *            ID白板ID
	 */
	public native void CanclePrint(String szWBoardID);

	/**
	 * 白板添加一页
	 * 
	 * @param szWBoardID
	 *            ID白板ID
	 * @param nActive
	 *            是否激活所添中的页
	 */
	public native void AddPage(String szWBoardID, boolean nActive);

	/**
	 * 白板激活一页
	 * 
	 * @param nUserId
	 * @param szWBoardID
	 *            白板ID
	 * @param nPageID
	 *            激页ID
	 * @param nIndex
	 * @param bNotify
	 *            激活页是否通知其它成员
	 */
	public native void ActivePage(long nUserId, String szWBoardID, int nPageId,
			int nIndex, boolean bNotify);

	private long lastgoupid = 0L;

	private void OnWBoardChatInvite(long nGroupID, int nBusinessType,
			long nFromUserID, String szWBoardID, int nWhiteIndex,
			String szFileName, int type) {
		Log.e("WBRequest UI", "OnWBoardChatInvite " + nGroupID + " "
				+ nBusinessType + " " + nFromUserID + " " + szWBoardID + " :"
				+ nWhiteIndex + " :" + szFileName);
		for (WeakReference<WBRequestCallback> wr : mCallbacks) {
			if (wr != null && wr.get() != null) {
				wr.get().OnWBoardChatInviteCallback(nGroupID, nBusinessType,
						nFromUserID, szWBoardID, nWhiteIndex, szFileName, type);
			}
		}

	}

	/**
	 * 3d4805676-0f02-4fd6-bf98-b8166832e51a :
	 * <pagelist><pageid='1'/></pagelist> : 1
	 * 
	 * @param szWBoardID
	 * @param szPageData
	 * @param nPageID
	 */
	private void OnWBoardPageList(String szWBoardID, String szPageData,
			int nPageID) {

		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = WBRequest METHOD = OnWBoardPageList()"
						+ " szWBoardID = " + szWBoardID + " szPageData = "
						+ szPageData + " nPageID = " + nPageID);

		Log.i("20141229 1", "ThreadID = " + Thread.currentThread().getId()
				+ " CLASS = WBRequest METHOD = OnWBoardPageList()"
				+ " szWBoardID = " + szWBoardID + " szPageData = " + szPageData
				+ " nPageID = " + nPageID);

		for (WeakReference<WBRequestCallback> wr : mCallbacks) {
			if (wr != null && wr.get() != null) {
				wr.get().OnWBoardPageListCallback(szWBoardID, szPageData,
						nPageID);
			}
		}
	}

	/**
	 * 
	 * @param nUserID
	 * @param szWBoardID
	 * @param nPageID
	 * @param index
	 */
	private void OnWBoardActivePage(long nUserID, String szWBoardID,
			int nPageID, int index) {

		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = WBRequest METHOD = OnWBoardActivePage()"
						+ " nUserID = " + nUserID + " szWBoardID = "
						+ szWBoardID + " nPageID = " + nPageID + " index = "
						+ index);

		Log.i("20141229 1", "ThreadID = " + Thread.currentThread().getId()
				+ " CLASS = WBRequest METHOD = OnWBoardActivePage()"
				+ " nUserID = " + nUserID + " szWBoardID = " + szWBoardID
				+ " nPageID = " + nPageID + " index = " + index);

		for (WeakReference<WBRequestCallback> wr : mCallbacks) {
			if (wr != null && wr.get() != null) {
				wr.get().OnWBoardActivePageCallback(nUserID, szWBoardID,
						nPageID);
			}
		}
	}

	private void OnWBoardAddPage(String szWBoardID, int nPageID) {

		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = WBRequest METHOD = OnWBoardAddPage()"
						+ " szWBoardID = " + szWBoardID + " nPageID = "
						+ nPageID);

		Log.i("20141229 1", "CLASS = WBRequest METHOD = OnWBoardAddPage()"
				+ " szWBoardID = " + szWBoardID + " nPageID = " + nPageID);

		for (WeakReference<WBRequestCallback> wr : mCallbacks) {
			if (wr != null && wr.get() != null) {
				wr.get().OnWBoardAddPageCallback(szWBoardID, nPageID);
			}
		}

	}

	/**
	 * 
	 * @param szWBoardID
	 * @param nPageID
	 * @param szFileName
	 * @param result
	 */
	private void OnWBoardDocDisplay(String szWBoardID, int nPageID,
			String szFileName, int result) {

		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = WBRequest METHOD = OnWBoardDocDisplay()"
						+ " szWBoardID = " + szWBoardID + " nPageID = "
						+ nPageID + " szFileName = " + szFileName
						+ " result = " + result);

		Log.i("20141229 1", "ThreadID = " + Thread.currentThread().getId()
				+ " CLASS = WBRequest METHOD = OnWBoardDocDisplay()"
				+ " szWBoardID = " + szWBoardID + " nPageID = " + nPageID
				+ " szFileName = " + szFileName + " result = " + result);

		szFileName = szFileName.replace("//", "/");
		for (WeakReference<WBRequestCallback> wr : mCallbacks) {
			if (wr != null && wr.get() != null) {
				wr.get().OnWBoardDocDisplayCallback(szWBoardID, nPageID,
						szFileName, result);
			}
		}

	}

	private void OnRecvAddWBoardData(String szWBoardID, int nPageID,
			String szDataID, String szData) {
		V2Log.d("OnRecvAddWBoardData " + szWBoardID + " " + nPageID + " "
				+ szDataID + " " + szData + " " + szData.length());

		for (WeakReference<WBRequestCallback> wr : mCallbacks) {
			if (wr != null && wr.get() != null) {
				wr.get().OnRecvAddWBoardDataCallback(szWBoardID, nPageID,
						szDataID, szData);
			}
		}
	}

	private void OnRecvAppendWBoardData(String szWBoardID, int nPageID,
			String szDataID, String szData) {
		Log.e("WBRequest UI",
				"OnRecvAppendWBoardData " + szWBoardID + " " + nPageID + " "
						+ szDataID + " " + szData + " " + szData.length());

		for (WeakReference<WBRequestCallback> wr : mCallbacks) {
			if (wr != null && wr.get() != null) {
				wr.get().OnRecvAppendWBoardDataCallback(szWBoardID, nPageID,
						szDataID, szData);
			}
		}

	}

	// / �յ��Է��������ҵİװ�Ự����Ļص�
	private void OnWBoardChatAccepted(long nGroupID,
			int nBuVideoActivityV2sinessType, long nFromUserID,
			String szWBoardID, int nWhiteIndex, String szFileName, int type) {
		V2Log.d("OnWBoardChatAccepted " + nGroupID + " " + nFromUserID + " "
				+ szWBoardID + " " + szFileName + " " + type);
	}

	private void OnWBoardChating(long nGroupID, int nBusinessType,
			long nFromUserID, String szWBoardID, String szFileName) {
		Log.e("WBRequest UI", "OnWBoardChating " + nGroupID + " "
				+ nBusinessType + " " + nFromUserID + " " + szWBoardID + " "
				+ szFileName);
	}

	private void OnWBoardClosed(long nGroupID, int nBusinessType, long nUserID,
			String szWBoardID) {
		V2Log.d(V2Log.JNI_CALLBACK,
				"CLASS = WBRequest METHOD = OnWBoardClosed()" + " nGroupID = "
						+ nGroupID + " nBusinessType = " + nBusinessType
						+ " nUserID = " + nUserID + " szWBoardID = "
						+ szWBoardID);

		Log.i("20141219 1", "CLASS = WBRequest METHOD = OnWBoardClosed()"
				+ " nGroupID = " + nGroupID + " nBusinessType = "
				+ nBusinessType + " nUserID = " + nUserID + " szWBoardID = "
				+ szWBoardID);

		for (WeakReference<WBRequestCallback> wr : mCallbacks) {
			if (wr != null && wr.get() != null) {
				wr.get().OnWBoardClosedCallback(nGroupID, nBusinessType,
						nUserID, szWBoardID);
			}
		}
	}

	private void OnWBoardDetroy(int groupType, long nGroupID, String szWBoardID) {
		Log.e("WBRequest UI", "OnWBoardDetroy " + nGroupID + " " + groupType
				+ " " + szWBoardID);
		for (WeakReference<WBRequestCallback> wr : mCallbacks) {
			if (wr != null && wr.get() != null) {
				wr.get().OnWBoardClosedCallback(nGroupID, 0, 0, szWBoardID);
			}
		}
	}

	private void OnRecvChangeWBoardData(String szWBoardID, int nPageID,
			String szDataID, String szData) {
		Log.e("WBRequest UI",
				"OnRecvChangeWBoardData " + szWBoardID + " " + nPageID + " "
						+ szDataID + " " + szData + " " + szData.length());
		// TODO
	}

	private void OnWBoardDataRemoved(String szWBoardID, int nPageID,
			String szDataID) {
		Log.e("WBRequest UI", "OnWBoardDataRemoved " + szWBoardID + " "
				+ nPageID + " " + szDataID);

	}

	//
	private void OnWBoardDeletePage(String szWBoardID, int nPageID) {
		Log.e("WBRequest UI", "OnWBoardDeletePage " + szWBoardID + " "
				+ nPageID);
		// TODO
	}

	private void OnDataBegin(String szWBoardID) {
		Log.e("WBRequest UI", "OnDataBegin " + szWBoardID);
	}

	private void OnDataEnd(String szWBoardID) {
		Log.e("WBRequest UI", "OnDataEnd " + szWBoardID);
	}

	private void OnGetPersonalSpaceDocDesc(long id, String xml) {
		Log.e("WBRequest UI", "OnGetPersonalSpaceDocDesc " + id + " " + xml);
	}
}
