package com.v2tech.util;

public final class GlobalState {
	
	/**
	 * Use to mark current app state which during audio call
	 */
	public static final int STATE_IN_AUDIO_CONVERSATION = 0x00001;
	
	/**
	 * Use to mark current app state which during video call
	 */
	public static final int STATE_IN_VIDEO_CONVERSATION = 0x00002;
	
	/**
	 * Used to mark current app state which voice is connected from remote side
	 */
	public static final int STATE_IN_VOICE_CONNECTED = 0x00004;
	
	/**
	 * Use to mark current app state which joined meeting or not
	 */
	public static final int STATE_IN_MEETING_CONVERSATION = 0x00008;

	private int state;
	private long gid;
	private long uid;
	
	
	
	
	public GlobalState() {
		super();
	}
	
	
	public GlobalState(GlobalState gstate) {
		this.state = gstate.state;
		this.gid = gstate.gid;
		this.uid = gstate.uid;
	}
	public int getState() {
		return state;
	}
	public void setState(int state) {
		this.state = state;
	}
	public long getGid() {
		return gid;
	}
	public void setGid(long gid) {
		this.gid = gid;
	}
	public long getUid() {
		return uid;
	}
	public void setUid(long uid) {
		this.uid = uid;
	}
	
	
	
	public boolean isInAudioCall() {
		return (state & STATE_IN_AUDIO_CONVERSATION) == STATE_IN_AUDIO_CONVERSATION;
	}

	public boolean isInVideoCall() {
		return (state & STATE_IN_VIDEO_CONVERSATION) == STATE_IN_VIDEO_CONVERSATION;
	}
	
	public boolean isVoiceConnected() {
		return (state & STATE_IN_VOICE_CONNECTED) == STATE_IN_VOICE_CONNECTED;
	}

	public boolean isInMeeting() {
		return (state & STATE_IN_MEETING_CONVERSATION) == STATE_IN_MEETING_CONVERSATION;
	}
}
