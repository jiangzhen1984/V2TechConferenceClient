package com.V2.jni;

import com.v2tech.util.V2Log;

public class VideoMixerRequest {

	private static VideoMixerRequest mVideoMixerRequest= null;
	
	
	private VideoMixerRequest() {
		
	}
	
	public static synchronized VideoMixerRequest getInstance() {
		if (mVideoMixerRequest == null) {
			mVideoMixerRequest = new VideoMixerRequest();
		}
		return mVideoMixerRequest;
	}
	
	
	
	public native boolean initialize(GroupRequest  instance);
	
	public native void unInitialize();
	
	
	
	  public native void createVideoMixer( String szMediaId, int layout, int width, int height);
	  public native  void destroyVideoMixer( String szMediaId);
	  public native  void addVideoMixerDevID( String szMediaId, long dstUserId, String dstDevId, int pos);

	public native void delVideoMixerDevID(String szMediaId, long dstUserId, String dstDevId);
	
	
	
	private void OnCreateVideoMixer(String sMediaId, int layout, int width,
			int height) {
		V2Log.d("OnCreateVideoMixer-->"+sMediaId + "   " + layout + "  " + width + "  " + height);
	}

	private void OnDestroyVideoMixer(String sMediaId) {
		V2Log.d("OnDestroyVideoMixer-->"+sMediaId);
	}

	private void OnAddVideoMixer(String sMediaId, long nDstUserId,
			String sDstDevId, int pos) {
		V2Log.d("OnAddVideoMixer-->"+sMediaId+  "  "+nDstUserId +"  "+ sDstDevId+"  "+ pos);
	}

	private void OnDelVideoMixer(String sMediaId, long nDstUserId,
			String sDstDevId) {
		V2Log.d("OnDelVideoMixer-->"+sMediaId+"   "+nDstUserId+"   "+sDstDevId);
	}

}
