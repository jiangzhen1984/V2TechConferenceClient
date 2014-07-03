package com.v2tech.vo;

import v2av.VideoPlayer;

public class UserChattingObject {
	
	public static final int VOICE_CALL = 0x01;
	public static final int VIDEO_CALL = 0x02;
	public static final int INCOMING_CALL = 0x10;
	public static final int OUTING_CALL = 0x00;
	public static final int SPEAKING = 0x100;
	
	private int flag;
	private User mUser;
	private long groupdId;
	private String mDeviceId;
	private VideoPlayer vp;

	public UserChattingObject(User user, int flag) {
		this(0, user, flag, "", null);
	}
	
	public UserChattingObject(User user, int flag, String deviceId) {
		this(0, user, flag, deviceId, null);
	}
	
	public UserChattingObject(long groupId, User user, int flag, String deviceId, VideoPlayer vp) {
		this.groupdId = groupId;
		this.flag = flag;
		this.flag |= SPEAKING;
		this.mUser = user;
		this.mDeviceId =deviceId;
		this.vp = vp;
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
	
	
	
	
	public VideoPlayer getVp() {
		return vp;
	}

	public void setVp(VideoPlayer vp) {
		this.vp = vp;
	}

	public void setMute(boolean b) {
		if (b) {
			this.flag &= (~SPEAKING);
		} else {
			this.flag |= SPEAKING;
		}
	}
	
	public boolean isMute() {
		return !((this.flag & SPEAKING) == SPEAKING);
	}
	
	public boolean isAudioType() {
		return (flag & VOICE_CALL) == VOICE_CALL;
	}
	
	public boolean isVideoType() {
		return (flag & VIDEO_CALL) == VIDEO_CALL;
	}
	
	public boolean isIncoming() {
		return (flag & INCOMING_CALL) == INCOMING_CALL;
	}
	
	public void updateAudioType() {
		flag &= VOICE_CALL;
		flag |= VOICE_CALL;
	}
}
