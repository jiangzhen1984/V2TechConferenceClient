package com.V2.jni;

import java.util.ArrayList;
import java.util.List;

import v2av.VideoPlayer;
import android.content.Context;
import android.util.Log;

import com.v2tech.util.V2Log;

public class VideoRequest {
	private static VideoRequest mVideoRequest;
	private List<VideoRequestCallback> callback;
	private Context context;

	private VideoRequest(Context context) {
		this.context = context;
		callback = new ArrayList<VideoRequestCallback>();
	};

	public static synchronized VideoRequest getInstance(Context context) {
		if (mVideoRequest == null) {
			mVideoRequest = new VideoRequest(context);
			if (!mVideoRequest.initialize(mVideoRequest)) {
				Log.e("mVideoRequest", "can't initialize mVideoRequest ");
			}
		}

		return mVideoRequest;
	}

	public static synchronized VideoRequest getInstance() {
		return mVideoRequest;
	}

	public void addCallback(VideoRequestCallback callback) {
		this.callback.add(callback);
	}

	public native boolean initialize(VideoRequest request);

	public native void unInitialize();

	/**
	 * Request to open video device include remote user and self.
	 * 
	 * @param nGroupID
	 *            conference id. If open P2P user device, this parameter should
	 *            be 0
	 * @param nUserID
	 *            user id
	 * @param szDeviceID
	 *            user device ID. If open local user camera device, user ""
	 * 
	 * @param vp
	 *            if open local device, input null. Otherwise
	 *            {@link VideoPlayer}
	 * @param businessType
	 *            1 : conference 2: IM type
	 * 
	 * 
	 * @see {@link #OnRemoteUserVideoDevice(String)}
	 * @see VideoPlayer
	 * @see V2ClientType#CONF
	 * @see V2ClientType#IM
	 */
	public native void openVideoDevice(long nGroupID, long nUserID,
			String szDeviceID, VideoPlayer vp, int businessType);

	/**
	 * Request to close video device. This function no callback call<br>
	 * 
	 * @param nGroupID
	 *            conference id. If open P2P user device, this parameter should
	 *            be 0
	 * @param nUserID
	 *            user id
	 * @param szDeviceID
	 *            user device ID. If open local user camera device, user ""
	 * 
	 * @param vp
	 *            if open local device, input null. Otherwise input
	 *            {@link VideoPlayer}
	 * @param businessType
	 *            1: conference 2: IM type
	 * 
	 * @see {@link #OnRemoteUserVideoDevice(String)}
	 * @see VideoPlayer
	 * @see V2ClientType#CONF
	 * @see V2ClientType#IM
	 */
	public native void closeVideoDevice(long nGroupID, long nUserID,
			String szDeviceID, VideoPlayer vp, int businessType);

	/**
	 * <ul>Update local camera configuration. JNI call {@link #OnSetCapParamDone(String, int, int, int)} to indicate response.</ul>
	 * @param szDevID
	 *            ""
	 * @param nSizeIndex
	 *            it's camera index
	 * @param nFrameRate
	 *            15
	 * @param nBitRate
	 *            256000
	 */
	public native void setCapParam(String szDevID, int nSizeIndex,
			int nFrameRate, int nBitRate);
	
	
	/**
	 * <ul>Indicate response that update camera configuration.</ul>
	 * @param szDevID
	 * @param nSizeIndex
	 * @param nFrameRate
	 * @param nBitRate
	 */
	private void OnSetCapParamDone(String szDevID, int nSizeIndex,
			int nFrameRate, int nBitRate) {
		V2Log.d("OnSetCapParamDone " + szDevID + " " + nSizeIndex + " "
				+ nFrameRate + " " + nBitRate);
		for (VideoRequestCallback cb : this.callback) {
			cb.OnSetCapParamDone(szDevID, nSizeIndex, nFrameRate, nBitRate);
		}
	}

	/**
	 * <ul>
	 * Indicate user device list. This function only called by JNI. and xml
	 * formate as below:
	 * </ul>
	 * <ul>
	 * {@code 
	 * <xml><user id='136'><videolist defaultid='136:CyberLink Webcam Sharing
	 * Manager____2056417056'><video bps='256' camtype='0' comm='0'
	 * desc='CyberLink Webcam Sharing Manager____2056417056' fps='15'
	 * id='136:CyberLink Webcam Sharing Manager____2056417056' selectedindex='0'
	 * videotype='vid'><sizelist><size h='240' w='320'/><size h='360'
	 * w='640'/><size h='480' w='640'/><size h='600' w='800'/><size h='720'
	 * w='1280'/><size h='960' w='1280'/><size h='900' w='1600'/><size h='1200'
	 * w='1600'/></sizelist></video><video bps='256' camtype='0' comm='0'
	 * desc='HP HD Webcam [Fixed]____1388682949' fps='15' id='136:HP HD Webcam
	 * [Fixed]____1388682949' selectedindex='3' videotype='vid'><sizelist><size
	 * h='480' w='640'/><size h='400' w='640'/><size h='288' w='352'/><size
	 * h='240' w='320'/><size h='720'
	 * w='1280'/></sizelist></video></videolist></user></xml>
	 * }<br>
	 * </ul>
	 * 
	 * @param szXmlData
	 *            user devices list as XML format
	 */
	private void OnRemoteUserVideoDevice(String szXmlData) {
		for (VideoRequestCallback cb : this.callback) {
			cb.OnRemoteUserVideoDevice(szXmlData);
		}
		V2Log.d("OnRemoteUserVideoDevice:---" + szXmlData);
	}

	/**
	 * 
	 * @param nGroupID
	 * @param nBusinessType
	 * @param nFromUserID
	 * @param szDeviceID
	 */
	private void OnVideoChatInvite(long nGroupID, int nBusinessType,
			long nFromUserID, String szDeviceID) {
		for (VideoRequestCallback cb : this.callback) {
			cb.OnVideoChatInviteCallback(nGroupID, nBusinessType, nFromUserID,
					szDeviceID);
		}
		V2Log.d("OnVideoChatInvite: nGroupID:" + nGroupID + "  nBusinessType:"
				+ nBusinessType + " nFromUserID:" + nFromUserID
				+ "  szDeviceID:" + szDeviceID);
	}

	// 鏋氫妇鎽勫儚澶�
	public native void enumMyVideos(int p);

	// 璁剧疆鏈湴鎽勫儚澶�
	public native void setDefaultVideoDev(String szDeviceID);

	// 閭�浠栦汉寮�瑙嗛浼氳瘽
	public native void inviteVideoChat(long nGroupID, long nToUserID,
			String szDeviceID, int businessType);

	// 鎺ュ彈瀵规柟鐨勮棰戜細璇濋個璇�
	public native void acceptVideoChat(long nGroupID, long nToUserID,
			String szDeviceID, int businessType);

	// 鎷掔粷瀵规柟鐨勮棰戜細璇濋個璇�
	public native void refuseVideoChat(long nGroupID, long nToUserID,
			String szDeviceID, int businessType);

	// 鍙栨秷瑙嗛浼氳瘽
	public native void cancelVideoChat(long nGroupID, long nToUserID,
			String szDeviceID, int businessType);

	// 鍏抽棴瑙嗛浼氳瘽
	public native void closeVideoChat(long nGroupID, long nToUserID,
			String szDeviceID, int businessType);

	// 閭�鍒汉鍚庡緱鍒板簲绛� OnVideoChatAccepted 0 2 1112627 1112627:Integrated
	// Camera____2889200338

	private void OnVideoChatAccepted(long nGroupID, int nBusinessType,
			long nFromuserID, String szDeviceID) {
		Log.e("ImRequest UI", "OnVideoChatAccepted " + nGroupID + " "
				+ nBusinessType + " " + nFromuserID + " " + szDeviceID);

		// 鎷艰淇℃伅
		// VideoAcceptMsgType videoMsgType=new VideoAcceptMsgType();
		// videoMsgType.setnFromuserID(nFromuserID);
		// videoMsgType.setSzDeviceID(szDeviceID);
		//
		// Intent intent=new Intent(VideoChatActivity.VIDEOCHAT);
		// intent.putExtra("MsgType", MsgType.VIDEOACCEPT_CHAT);
		// intent.putExtra("MSG", videoMsgType);
		// intent.putExtra("videochat", Constant.VIDEOCHATACCEPTED);
		// context.sendBroadcast(intent);
	}

	// 閭�鍒汉鍚庨伃鍒版嫆缁濈殑 鍥炴帀 OnVideoChatRefused 0 2 1112627
	private void OnVideoChatRefused(long nGroupID, int nBusinessType,
			long nFromUserID, String szDeviceID) {
		Log.e("ImRequest UI", "OnVideoChatRefused " + nGroupID + " "
				+ nBusinessType + " " + nFromUserID + " " + szDeviceID);

		// 鎷艰淇℃伅
		// VideoRefuseMsgType videoMsgType=new VideoRefuseMsgType();
		// videoMsgType.setnFromuserID(nFromUserID);
		// videoMsgType.setSzDeviceID(szDeviceID);
		//
		// Intent intent=new Intent(VideoChatActivity.VIDEOCHAT);
		// intent.putExtra("MsgType", MsgType.VIDEOREFUSE_CHAT);
		// intent.putExtra("MSG", videoMsgType);
		// intent.putExtra("videochat", Constant.VIDEOCHATREFUSED);
		// context.sendBroadcast(intent);
	}

	// 鏀跺埌瑙嗛浼氳瘽宸茬粡寤虹珛鐨勫洖璋�
	private void OnVideoChating(long nGroupID, int nBusinessType,
			long nFromUserID, String szDeviceID) {
		Log.e("ImRequest UI", "OnVideoChating " + nGroupID + " "
				+ nBusinessType + " " + nFromUserID + " " + szDeviceID);
	}

	// 鏀跺埌瑙嗛浼氳瘽琚叧闂殑鍥炶皟
	private void OnVideoChatClosed(long nGroupID, int nBusinessType,
			long nFromUserID, String szDeviceID) {
		Log.e("ImRequest UI", "OnVideoChatClosed " + nGroupID + " "
				+ nBusinessType + " " + nFromUserID + " " + szDeviceID);

		// 鍙戦�骞挎挱锛岄�鐭ヨ棰戦�璇濆凡缁忕粨鏉� 2浠ｈ〃鍏抽棴
		// Intent intent=new Intent(VideoChatActivity.VIDEOCHAT);
		// intent.putExtra("videochat", Constant.VIDEOCHATCLOSED);
		// context.sendBroadcast(intent);
		//
		// Intent intent1=new
		// Intent(HandleVideoInviteActivity.CLOSE_VIDEOCHAT1);
		// intent1.putExtra("videochat", "2");
		// context.sendBroadcast(intent1);
	}

	private void OnVideoWindowSet(String sDevId, Object hwnd) {
		Log.e("ImRequest UI",
				"OnVideoWindowSet " + sDevId + " " + hwnd.toString());
	}

	private void OnVideoWindowClosed(String sDevId, Object hwnd) {
		Log.e("ImRequest UI",
				"OnVideoWindowClosed " + sDevId + " " + hwnd.toString());
	}

	// private void OnGetDevSizeFormats(String szXml);



	// // 閫氱煡绐楀彛瑙嗛姣旂壒鐜囷紝鍗曚綅Kbps
	private void OnVideoBitRate(Object hwnd, int bps) {
		// Log.e("ImRequest UI", "OnVideoBitRate " + hwnd +" "+bps);
	}

	// 鎽勫儚澶撮噰闆嗗嚭閿�
	private void OnVideoCaptureError(String szDevID, int nErr) {

	}

	private void OnVideoPlayerClosed(String szDeviceID) {
		Log.e("ImRequest UI", "OnVideoPlayerClosed " + szDeviceID);
	}

	private void OnGetVideoDevice(String xml, long l) {
		Log.e("VideoRequest UI", "OnGetVideoDevice " + xml);
	}

	private void OnGetVideoDevice(long l, String xml) {
		Log.e("VideoRequest UI", "OnGetVideoDevice " + xml);
	}
}
