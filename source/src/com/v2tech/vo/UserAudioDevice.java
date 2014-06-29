package com.v2tech.vo;

public class UserAudioDevice {
	
	public static final int VOICE_CALL = 0x01;
	public static final int VIDEO_CALL = 0x02;
	
	private int flag;
	private User mUser;
	private long groupdId;
	private String mDeviceId;

	public UserAudioDevice(User user, int flag) {
		this(0, user, flag, "");
	}
	
	public UserAudioDevice(User user, int flag, String deviceId) {
		this(0, user, flag, deviceId);
	}
	
	public UserAudioDevice(long groupId, User user, int flag, String deviceId) {
		this.groupdId = groupId;
		this.flag = flag;
		this.mUser = user;
		this.mDeviceId =deviceId;
	}
	
	public User getUser() {
		return this.mUser;
	}

	public long getGroupdId() {
		return groupdId;
	}
	
	
	public String getDeviceId() {
		return this.mDeviceId;
	}
	
	
	
	
	public boolean isAudioType() {
		return (flag & VOICE_CALL) == VOICE_CALL;
	}
	
	public boolean isVideoType() {
		return (flag & VIDEO_CALL) == VIDEO_CALL;
	}
}
