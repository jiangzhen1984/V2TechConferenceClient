package com.V2.jni;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import v2av.VideoPlayer;
import android.content.Context;
import android.util.Log;

import com.V2.jni.callbackInterface.VideoRequestCallback;
import com.V2.jni.ind.VideoJNIObjectInd;
import com.V2.jni.util.V2Log;
import com.bizcom.vc.application.V2GlobalConstants;
import com.bizcom.vc.application.V2GlobalEnum;

public class VideoRequest {

	private static VideoRequest mVideoRequest;
	private List<WeakReference<VideoRequestCallback>> mCallbacks;

	private VideoRequest(Context context) {
		mCallbacks = new CopyOnWriteArrayList<WeakReference<VideoRequestCallback>>();
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
		if (mVideoRequest == null) {
			mVideoRequest = new VideoRequest(null);
			if (!mVideoRequest.initialize(mVideoRequest)) {
				Log.e("mVideoRequest", "can't initialize mVideoRequest ");
			}
		}
		return mVideoRequest;
	}

	public void addCallback(VideoRequestCallback callback) {
		this.mCallbacks.add(new WeakReference<VideoRequestCallback>(callback));
	}

	public void removeCallback(VideoRequestCallback callback) {
		for (int i = 0; i < mCallbacks.size(); i++) {
			if (mCallbacks.get(i).get() == callback) {
				mCallbacks.remove(i);
				break;
			}
		}
	}

	public native boolean initialize(VideoRequest request);

	public native void unInitialize();



	/**
	 * Request to open video device include remote user and self.
	 * 
	 * @param groupType
	 * 
	 *            {@link V2GlobalConstants#GROUP_TYPE_USER} <br/>
	 *            {@link V2GlobalConstants# GROUP_TYPE_DEPARTMENT} <br/>
	 *            {@link V2GlobalConstants#GROUP_TYPE_CONTACT} <br/>
	 *            {@link V2GlobalConstants#GROUP_TYPE_CROWD } <br/>
	 *            {@link V2GlobalConstants#GROUP_TYPE_CONFERENCE} conference <br/>
	 *            {@link V2GlobalConstants#GROUP_TYPE_DISCUSSION} <br/>
	 * 
	 * @param nGroupID   0 for IM
	 * @param type
	 *            type of request<br> 
	 *            {@link V2GlobalEnum#REQUEST_TYPE_CONF} for conference<br>
	 *            {@link V2GlobalEnum#REQUEST_TYPE_IM}  for P2P<br>
	 * 
	 * @param nUserID
	 * @param szDeviceID
	 * @param vp
	 *            if open local device, input null. Otherwise
	 *            {@link VideoPlayer}
	 */
	public native void openVideoDevice(int groupType, long nGroupID, int type,
			long nUserID, String szDeviceID, VideoPlayer vp);

	/**
	 * Request to close video device.
	 * 
	 * 
	 * @param groupType
	 * 
	 *            {@link V2GlobalConstants#GROUP_TYPE_USER} <br/>
	 *            {@link V2GlobalConstants# GROUP_TYPE_DEPARTMENT} <br/>
	 *            {@link V2GlobalConstants#GROUP_TYPE_CONTACT} <br/>
	 *            {@link V2GlobalConstants#GROUP_TYPE_CROWD } <br/>
	 *            {@link V2GlobalConstants#GROUP_TYPE_CONFERENCE} conference <br/>
	 *            {@link V2GlobalConstants#GROUP_TYPE_DISCUSSION} <br/>
	 * 
	 * @param nGroupID   0 for IM
	 * @param type
	 *            type of request<br> 
	 *            {@link V2GlobalEnum#REQUEST_TYPE_CONF} for conference<br>
	 *            {@link V2GlobalEnum#REQUEST_TYPE_IM}  for P2P<br>
	 * 
	 * @param nUserID
	 * @param szDeviceID
	 * @param vp
	 *            if open local device, input null. Otherwise
	 *            {@link VideoPlayer}
	 */
	public native void closeVideoDevice(int groupType, long nGroupID,int type, long nUserID,
			String szDeviceID, VideoPlayer vp);

	/**
	 * <ul>
	 * Update local camera configuration. JNI call
	 * {@link #OnSetCapParamDone(String, int, int, int)} to indicate response.
	 * </ul>
	 * 
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
	 * <ul>
	 * Indicate response that update camera configuration.
	 * </ul>
	 * 
	 * @param szDevID
	 * @param nSizeIndex
	 * @param nFrameRate
	 * @param nBitRate
	 */
	private void OnSetCapParamDone(String szDevID, int nSizeIndex,
			int nFrameRate, int nBitRate) {
		V2Log.d("OnSetCapParamDone " + szDevID + " " + nSizeIndex + " "
				+ nFrameRate + " " + nBitRate);
		for (WeakReference<VideoRequestCallback> wrCB : this.mCallbacks) {
			Object obj = wrCB.get();
			if (obj != null) {
				VideoRequestCallback cb = (VideoRequestCallback) obj;
				cb.OnSetCapParamDone(szDevID, nSizeIndex, nFrameRate, nBitRate);
			}
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
	private void OnRemoteUserVideoDevice(long uid, String szXmlData) {
		V2Log.d("OnRemoteUserVideoDevice:---" + uid + " " + szXmlData);
		for (WeakReference<VideoRequestCallback> wrCB : this.mCallbacks) {
			Object obj = wrCB.get();
			if (obj != null) {
				VideoRequestCallback cb = (VideoRequestCallback) obj;
				cb.OnRemoteUserVideoDevice(uid, szXmlData);
			}
		}
	}

	/**
	 * 
	 * @param nGroupID
	 * @param nBusinessType
	 * @param nFromUserID
	 * @param szDeviceID
	 */
	private void OnVideoChatInvite(String szSessionID, long nFromUserID,
			String szDeviceID) {
		for (WeakReference<VideoRequestCallback> wrCB : this.mCallbacks) {
			Object obj = wrCB.get();
			if (obj != null) {
				VideoRequestCallback cb = (VideoRequestCallback) obj;
				cb.OnVideoChatInviteCallback(new VideoJNIObjectInd(szSessionID,
						nFromUserID, szDeviceID, 0));
			}
		}
		V2Log.d("OnVideoChatInvite: nGroupID:" + szSessionID + " nFromUserID:"
				+ nFromUserID + "  szDeviceID:" + szDeviceID);

	}

	/**
	 * 
	 * @param nGroupID
	 * @param nBusinessType
	 * @param nFromuserID
	 * @param szDeviceID
	 */
	private void OnVideoChatAccepted(String szSessionID, long nFromUserID,
			String szDeviceID) {
		V2Log.d("OnVideoChatAccepted " + szSessionID + " " + nFromUserID + " "
				+ szDeviceID);
		Log.i("OnVideoChatAccepted ",szSessionID + " " + nFromUserID + " "
				+ szDeviceID);
		for (WeakReference<VideoRequestCallback> wrCB : this.mCallbacks) {
			Object obj = wrCB.get();
			if (obj != null) {
				VideoRequestCallback cb = (VideoRequestCallback) obj;
				cb.OnVideoChatAccepted(new VideoJNIObjectInd(szSessionID,
						nFromUserID, szDeviceID, 0));
			}
		}
	}

	/**
	 * 
	 * @param nGroupID
	 * @param nBusinessType
	 * @param nFromUserID
	 * @param szDeviceID
	 */
	private void OnVideoChatRefused(String szSessionID, long nFromUserID,
			String szDeviceID) {
		V2Log.d("OnVideoChatRefused " + szSessionID + " " + nFromUserID + " "
				+ szDeviceID);
		for (WeakReference<VideoRequestCallback> wrCB : this.mCallbacks) {
			Object obj = wrCB.get();
			if (obj != null) {
				VideoRequestCallback cb = (VideoRequestCallback) obj;
				cb.OnVideoChatRefused(new VideoJNIObjectInd(szSessionID,
						nFromUserID, szDeviceID, 0));
			}
		}
	}

	private void OnVideoChatClosed(String szSessionID, long nFromUserID,
			String szDeviceID) {
		V2Log.d("OnVideoChatClosed " + szSessionID + " " + nFromUserID + " "
				+ szDeviceID);
		for (WeakReference<VideoRequestCallback> wrCB : this.mCallbacks) {
			Object obj = wrCB.get();
			if (obj != null) {
				VideoRequestCallback cb = (VideoRequestCallback) obj;
				cb.OnVideoChatClosed(new VideoJNIObjectInd(szSessionID,
						nFromUserID, szDeviceID, 0));
			}
		}
	}

	private void OnVideoChating(String szSessionID, long nFromUserID,
			String szDeviceID) {
		V2Log.d("OnVideoChating " + szSessionID + " " + nFromUserID + " "
				+ szDeviceID);
		for (WeakReference<VideoRequestCallback> wrCB : this.mCallbacks) {
			Object obj = wrCB.get();
			if (obj != null) {
				VideoRequestCallback cb = (VideoRequestCallback) obj;
				cb.OnVideoChating(new VideoJNIObjectInd(szSessionID,
						nFromUserID, szDeviceID, 0));

			}
		}
	}

	// 鏋氫妇鎽勫儚澶�
	public native void enumMyVideos(int p);

	// 璁剧疆鏈湴鎽勫儚澶�
	public native void setDefaultVideoDev(String szDeviceID);

	/**
	 * Invite user to join video call
	 * 
	 * @param szSessionID
	 * @param nToUserID
	 * @param szDeviceID
	 */
	public native void inviteVideoChat(String szSessionID, long nToUserID,
			String szDeviceID);

	/**
	 * Accept video invitation
	 * 
	 * @param szSessionID
	 * @param nToUserID
	 * @param szDeviceID
	 */
	public native void acceptVideoChat(String szSessionID, long nToUserID,
			String szDeviceID);

	/**
	 * Reject video invitation call
	 * 
	 * @param szSessionID
	 * @param nToUserID
	 * @param szDeviceID
	 */
	public native void refuseVideoChat(String szSessionID, long nToUserID,
			String szDeviceID);

	public native void cancelVideoChat(String szSessionID, long nToUserID,
			String szDeviceID);

	/**
	 * @param szSessionID
	 * @param nToUserID
	 * @param szDeviceID
	 */
	public native void closeVideoChat(String szSessionID, long nToUserID,
			String szDeviceID);

	
	/**
	 * 启用摄像头设备。
	 * @param szDeviceID 设备ID
	 * @param bInuse 启用
	 */
	public native void enableVideoDev(String szDeviceID , int bInuse);
	
	/**
	 * 设置默认渲染器类型
	 * @param nType  0 --> VMR9 , 1--> GDI
	 */
	public native void setDefaultRenderType(int nType);


	/**
	 * 频抓图
	 * @param hwnd 窗口标识
	 * @param szFile 保存文件的全路径
	 */
	public native void windowSnapshot(int hwnd, String szFile);
	
	
	/**
	 * 允许别人控制摄像头
	 * @param szDevID 设备ID
	 * @param bRemotePtz  0,禁止; 1,允许
	 */
	public native void enableRemotePtz(String szDevID, int bRemotePtz);
	
	/**
	 * 设置摄像头预置位
	 * @param szDevID 设备ID
	 * @param nPresetNum 预置位 (1 - 6)
	 * @param szPreset 预置位名称
	 */
	public native void setCamPreset(String szDevID, int nPresetNum, String szPreset);
	
	private void OnVideoWindowSet(String sDevId, Object hwnd) {
		Log.e("ImRequest UI",
				"OnVideoWindowSet " + sDevId + " " + hwnd.toString());
	}

	private void OnVideoWindowClosed(String sDevId, Object hwnd) {
		Log.e("ImRequest UI",
				"OnVideoWindowClosed " + sDevId + " " + hwnd.toString());
	}

	// private void OnGetDevSizeFormats(String szXml);

	private void OnVideoBitRate(Object hwnd, int bps) {
		// Log.e("ImRequest UI", "OnVideoBitRate " + hwnd +" "+bps);
	}

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
	
	public native void pausePlayout(String szDeviceID);
	public native void resumePlayout(String szDeviceID);
}
