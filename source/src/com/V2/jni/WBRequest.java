package com.V2.jni;


import android.app.Activity;
import android.util.Log;


public class WBRequest {
	private Activity context;
	private static WBRequest mWBRequest;
	
	private WBRequest(Activity context){
		this.context=context;
	}

	public static  synchronized  WBRequest getInstance(Activity context){
		
		if(mWBRequest==null){
			mWBRequest=new WBRequest(context);
		}
		return mWBRequest;
	}
	
	public native boolean initialize(WBRequest request);

	public native void unInitialize();
	 	
	public native void downLoadPageDoc(String bowardid,int pageid);

 	//4514714000   1    196    3d4805676-0f02-4fd6-bf98-b8166832e51a
	
	private long lastgoupid=0L;
	
	private void OnWBoardChatInvite(long nGroupID, int nBusinessType, long  nFromUserID, String szWBoardID, 
			int nWhiteIndex,String szFileName, int type) {
		Log.e("ImRequest UI", "OnWBoardChatInvite " + nGroupID + " "
				+ nBusinessType + " " + nFromUserID + " " + szWBoardID+"序列号是:"+nWhiteIndex+"文件名:"+szFileName);

		
	}

	// 3d4805676-0f02-4fd6-bf98-b8166832e51a <  pagelist><page id='1'/></pagelist>   1
	
	
	private void OnWBoardPageList(String szWBoardID, String szPageData,
			int nPageID) {
		Log.e("ImRequest UI", "OnWBoardPageList " + szWBoardID + " "
				+ szPageData + " " + nPageID);
	}

	//3d4805676-0f02-4fd6-bf98-b8166832e51a   1
	private void OnWBoardActivePage(long nUserID, String szWBoardID, int nPageID)
	{
		Log.e("ImRequest UI", "OnWBoardActivePage " + szWBoardID + " "
				+ nPageID);
		
	}
	
	private void OnRecvAddWBoardData(String szWBoardID, int nPageID,
			String szDataID, String szData) {
		Log.e("ImRequest UI",
				"OnRecvAddWBoardData " + szWBoardID + " " + nPageID + " "
						+ szDataID + " " + szData + " " + szData.length());
		//添加数据
		
	}

	// 收到追加白板数据的回调
	private void OnRecvAppendWBoardData(String szWBoardID, int nPageID,
			String szDataID, String szData) 
	{
		Log.e("ImRequest UI",
				"OnRecvAppendWBoardData " + szWBoardID + " " + nPageID + " "
						+ szDataID + " " + szData + " " + szData.length());
		//判断是否为文档共享的数据

	}
	
	/// 收到对方授受了我的白板会话邀请的回调
	private void OnWBoardChatAccepted(long  nGroupID, int nBusinessType, long  nFromUserID, String szWBoardID, 
			int nWhiteIndex,String szFileName, int type) {
		Log.e("ImRequest UI", "OnWBoardChatAccepted " + nGroupID + " "
				+ nBusinessType + " " + nFromUserID + " " + szWBoardID + " "
				+ szFileName + " " + type);
	}

	private void OnWBoardChating(long nGroupID, int nBusinessType,
			long nFromUserID, String szWBoardID, String szFileName) {
		Log.e("ImRequest UI", "OnWBoardChating " + nGroupID + " "
				+ nBusinessType + " " + nFromUserID + " " + szWBoardID + " "
				+ szFileName);
	}

	
	 private void OnWBoardClosed(long  nGroupID, int nBusinessType, long nUserID, String szWBoardID) {
		Log.e("ImRequest UI", "OnWBoardClosed " + nGroupID + " "
				+ nBusinessType + " " + szWBoardID);
	}

	private void OnRecvChangeWBoardData(String szWBoardID, int nPageID,
			String szDataID, String szData) {
		Log.e("ImRequest UI",
				"OnRecvChangeWBoardData " + szWBoardID + " " + nPageID + " "
						+ szDataID + " " + szData + " " + szData.length());
		// TODO
	}

	private void OnWBoardDataRemoved(String szWBoardID, int nPageID,
			String szDataID) {
		Log.e("ImRequest UI", "OnWBoardDataRemoved " + szWBoardID + " "
				+ nPageID + " " + szDataID);
		
	}

	// 收到文档增加的回调
	private void OnWBoardAddPage(String szWBoardID, int nPageID) {
		Log.e("ImRequest UI", "OnWBoardAddPage " + szWBoardID + " " + nPageID);
		
	}

	// 
	private void OnWBoardDeletePage(String szWBoardID, int nPageID) {
		Log.e("ImRequest UI", "OnWBoardDeletePage " + szWBoardID + " "
				+ nPageID);
		// TODO
	}


	
	//文档共享   应该是下载完一页显示一页   
	private void OnWBoardDocDisplay(String szWBoardID, int nPageID,
			String szFileName,int result) {
		
//		return ;
		Log.e("ImRequest UI", "文档显示---->OnWBoardDocDisplay " + szWBoardID + " "
				+ nPageID + " " + szFileName);
		
		
	}
	
	private void OnDataBegin(String szWBoardID)
	{
		Log.e("ImRequest UI", "OnDataBegin " + szWBoardID);
	}
	
	private void OnDataEnd(String szWBoardID)
	{
		Log.e("ImRequest UI", "OnDataEnd " + szWBoardID);
	} 
	
	private void OnGetPersonalSpaceDocDesc(long id,String xml){
		Log.e("ImRequest UI", "OnGetPersonalSpaceDocDesc " + id+" "+xml);
	}
}
