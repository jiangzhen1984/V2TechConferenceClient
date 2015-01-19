package com.V2.jni;

public class ServerRecordRequest {

	public native boolean initialize(ServerRecordRequest record);

	public native void unInitialize();

	public native void startServerRecord(String sRecordID, String sRecordName);

	public native void stopServerRecord();

	public native void delServerRecord(long nGroupID, String sRecordID);

	public native void downConfVodSnapshot(long nGroupID, String sVodID,
			String sVodSnapshotUrl);

	public native void startLive(String sRecordID, String sRecordName);

	public native void stopLive();

	private void OnAddRecordResponse(long nGroupID, String sVodXml) {
	};

	/**
	 * 删除点播
	 */
	private void OnDelRecordResponse(long nGroupID, String sRecordID) {
	};

	private void OnNotifyVodShapshotResponse(String sRecordID, String sLocalDir) {
	};

	private void OnStartLive(long nGroupID, String sLiveBroadcastXml) {
	};

	private void OnStopLive(long nGroupID, long nUserID, String sRecordID) {
	};
}
