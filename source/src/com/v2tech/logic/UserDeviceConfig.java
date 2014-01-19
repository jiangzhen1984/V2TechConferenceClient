package com.v2tech.logic;

import v2av.VideoPlayer;

/**
 * User video device configuration object.<br>
 * Local user video device unnecessary  VideoPlayer and device id information.<br>
 * If we want to open remote user's video, we need VidePlayer and device id information.
 * To get remote user's device id, we can listen {@link com.V2.jni.VideoRequestCallback#OnRemoteUserVideoDevice(String)}. 
 * <br>
 * @see v2av.VideoPlayer com.V2.jni.VideoRequestCallback
 * @author jiangzhen
 *
 */
public class UserDeviceConfig {

	private long mUerID;
	private String mDeviceID;
	private VideoPlayer mVP;
	private int mBusinessType;
	
	

	public UserDeviceConfig(long mUerID, String mDeviceID, VideoPlayer mVP) {
		this(mUerID, mDeviceID, mVP, 1);
		this.mUerID = mUerID;
		this.mDeviceID = mDeviceID;
		this.mVP = mVP;
	}

	public UserDeviceConfig(long mUerID, String mDeviceID, VideoPlayer mVP,
			int mBusinessType) {
		this.mUerID = mUerID;
		this.mDeviceID = mDeviceID;
		this.mVP = mVP;
		this.mBusinessType = mBusinessType;
	}

	public long getUserID() {
		return mUerID;
	}

	public void setUserID(long userID) {
		this.mUerID = userID;
	}

	public String getDeviceID() {
		return mDeviceID;
	}

	public void setDeviceID(String deviceID) {
		this.mDeviceID = deviceID;
	}

	public VideoPlayer getVp() {
		return mVP;
	}

	public void setVp(VideoPlayer vp) {
		this.mVP = vp;
	}

	public int getBusinessType() {
		return mBusinessType;
	}

	public void setBusinessType(int businessType) {
		this.mBusinessType = businessType;
	}

}
