package com.V2.jni;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;

public class WBRequest {
	private Context context;
	private static WBRequest mWBRequest;

	private List<WeakReference<WBRequestCallback>> mCallbacks = new ArrayList<WeakReference<WBRequestCallback>>();

	private WBRequest(Context context) {
		this.context = context;
	}

	public static synchronized WBRequest getInstance(Context context) {

		if (mWBRequest == null) {
			mWBRequest = new WBRequest(context);
			mWBRequest.initialize(mWBRequest);
		}
		return mWBRequest;
	}
	
	public static synchronized WBRequest getInstance() {
		return mWBRequest;
	}

	public void addCallbacks(WBRequestCallback callback) {
		mCallbacks.add(new WeakReference<WBRequestCallback>(callback));
	}

	public native boolean initialize(WBRequest request);

	public native void unInitialize();

	public native void downLoadPageDoc(String bowardid, int pageid);

	// 4514714000 1 196 3d4805676-0f02-4fd6-bf98-b8166832e51a

	private long lastgoupid = 0L;

	private void OnWBoardChatInvite(long nGroupID, int nBusinessType,
			long nFromUserID, String szWBoardID, int nWhiteIndex,
			String szFileName, int type) {
		Log.e("WBRequest UI", "OnWBoardChatInvite " + nGroupID + " "
				+ nBusinessType + " " + nFromUserID + " " + szWBoardID
				+ " :" + nWhiteIndex + " :" + szFileName);
		for (WeakReference<WBRequestCallback> wr : mCallbacks) {
			if (wr != null && wr.get() != null) {
				wr.get().OnWBoardChatInvite(nGroupID, nBusinessType,
						nFromUserID, szWBoardID, nWhiteIndex, szFileName, type);
			}
		}

	}

	// 3d4805676-0f02-4fd6-bf98-b8166832e51a < pagelist><page
	// id='1'/></pagelist> 1

	private void OnWBoardPageList(String szWBoardID, String szPageData,
			int nPageID) {
		Log.e("WBRequest UI", "OnWBoardPageList " + szWBoardID + " "
				+ szPageData + " " + nPageID);
		for (WeakReference<WBRequestCallback> wr : mCallbacks) {
			if (wr != null && wr.get() != null) {
				wr.get().OnWBoardPageList(szWBoardID, szPageData, nPageID);
			}
		}
	}

	// 3d4805676-0f02-4fd6-bf98-b8166832e51a 1
	private void OnWBoardActivePage(long nUserID, String szWBoardID, int nPageID) {
		Log.e("WBRequest UI", "OnWBoardActivePage " + szWBoardID + " "
				+ nPageID);
		for (WeakReference<WBRequestCallback> wr : mCallbacks) {
			if (wr != null && wr.get() != null) {
				wr.get().OnWBoardActivePage(nUserID, szWBoardID, nPageID);
			}
		}

	}

	private void OnWBoardDocDisplay(String szWBoardID, int nPageID,
			String szFileName, int result) {
		// return ;
		Log.e("WBRequest UI", "�ĵ���ʾ---->OnWBoardDocDisplay " + szWBoardID
				+ " " + nPageID + " " + szFileName);
		for (WeakReference<WBRequestCallback> wr : mCallbacks) {
			if (wr != null && wr.get() != null) {
				wr.get().OnWBoardDocDisplay(szWBoardID, nPageID, szFileName,
						result);
			}
		}

	}

	private void OnRecvAddWBoardData(String szWBoardID, int nPageID,
			String szDataID, String szData) {
		Log.e("WBRequest UI",
				"OnRecvAddWBoardData " + szWBoardID + " " + nPageID + " "
						+ szDataID + " " + szData + " " + szData.length());
		// ������

	}

	// �յ�׷�Ӱװ���ݵĻص�
	private void OnRecvAppendWBoardData(String szWBoardID, int nPageID,
			String szDataID, String szData) {
		Log.e("WBRequest UI",
				"OnRecvAppendWBoardData " + szWBoardID + " " + nPageID + " "
						+ szDataID + " " + szData + " " + szData.length());
		// �ж��Ƿ�Ϊ�ĵ���������

	}

	// / �յ��Է��������ҵİװ�Ự����Ļص�
	private void OnWBoardChatAccepted(long nGroupID, int nBusinessType,
			long nFromUserID, String szWBoardID, int nWhiteIndex,
			String szFileName, int type) {
		Log.e("WBRequest UI", "OnWBoardChatAccepted " + nGroupID + " "
				+ nBusinessType + " " + nFromUserID + " " + szWBoardID + " "
				+ szFileName + " " + type);
	}

	private void OnWBoardChating(long nGroupID, int nBusinessType,
			long nFromUserID, String szWBoardID, String szFileName) {
		Log.e("WBRequest UI", "OnWBoardChating " + nGroupID + " "
				+ nBusinessType + " " + nFromUserID + " " + szWBoardID + " "
				+ szFileName);
	}

	private void OnWBoardClosed(long nGroupID, int nBusinessType, long nUserID,
			String szWBoardID) {
		Log.e("WBRequest UI", "OnWBoardClosed " + nGroupID + " "
				+ nBusinessType + " " + szWBoardID);
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

	// �յ��ĵ����ӵĻص�
	private void OnWBoardAddPage(String szWBoardID, int nPageID) {
		Log.e("WBRequest UI", "OnWBoardAddPage " + szWBoardID + " " + nPageID);

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
