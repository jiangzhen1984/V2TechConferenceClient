package com.bizcom.vc.application;

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
	
	/**
	 * Use to mark current phone which  wired headset pluged
	 */
	public static final int STATE_WIRED_HEADSET_PLUG = 0x00010;
	
	/**
	 * Use to mark current phone which matched bluetooth headset
	 */
	public static final int STATE_BLUETOOTH_HEADSET_PLUG = 0x00020;
	
	/**
	 * Use to mark current server connection state
	 */
	public static final int STATE_SERVER_CONNECTED = 0x01000;
	
	
	/**
	 * Use to mark current server groups load state
	 */
	public static final int STATE_SERVER_GROUPS_LOADED = 0x02000;
	

	/**
	 * Use to mark all offline message loaded from server
	 */
	public static final int STATE_SERVER_OFFLINE_MESSAGE_LOADED = 0x04000;
	
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
	
	/**
	 * Set bluetooth headset matched or not
	 * @param flag
	 */
	public void setBluetoothHeadset(boolean flag) {
		setStatePri(STATE_BLUETOOTH_HEADSET_PLUG, flag);
	}
	
	/**
	 * Set wired headset state
	 * @param flag
	 */
	public void setWiredHeadsetState(boolean flag) {
		setStatePri(STATE_WIRED_HEADSET_PLUG, flag);
	}
	
	private void setStatePri(int marker, boolean flag) {
		if (flag) {
			state |= marker;
		} else {
			state &= ~marker;
		}
	}
	
	/**
	 * To get current application state : P2P audio call
	 * @return true in, false not
	 */
	public boolean isInAudioCall() {
		return (state & STATE_IN_AUDIO_CONVERSATION) == STATE_IN_AUDIO_CONVERSATION;
	}

	/**
	 * To get current application state : P2P video call
	 * @return true in, false not
	 */
	public boolean isInVideoCall() {
		return (state & STATE_IN_VIDEO_CONVERSATION) == STATE_IN_VIDEO_CONVERSATION;
	}
	
	/**
	 * To get current application state: P2P voice is connected.<br>
	 * @return true connected, false not
	 */
	public boolean isVoiceConnected() {
		return (state & STATE_IN_VOICE_CONNECTED) == STATE_IN_VOICE_CONNECTED;
	}

	/**
	 * To get current application state: in conference.
	 * @return true in, false not
	 */
	public boolean isInMeeting() {
		return (state & STATE_IN_MEETING_CONVERSATION) == STATE_IN_MEETING_CONVERSATION;
	}
	
	/**
	 * To get current phone state which wired headset pluged or not
	 * @return true pluged, false not
	 */
	public boolean isWiredHeadsetPluged() {
		return (state & STATE_WIRED_HEADSET_PLUG) == STATE_WIRED_HEADSET_PLUG;
	}
	
	/**
	 * To get current phone state which bluetooth headset matched or not
	 * @return true matched, false not
	 */
	public boolean isBluetoothHeadsetPluged() {
		return (state & STATE_BLUETOOTH_HEADSET_PLUG) == STATE_BLUETOOTH_HEADSET_PLUG;
	}
	
	
	public boolean isConnectedServer() {
		return (state & STATE_SERVER_CONNECTED) == STATE_SERVER_CONNECTED;
	}
	
	public boolean isGroupLoaded() {
		return (state & STATE_SERVER_GROUPS_LOADED) == STATE_SERVER_GROUPS_LOADED;
	}
	
	public boolean isOfflineLoaded() {
		return (state & STATE_SERVER_OFFLINE_MESSAGE_LOADED) == STATE_SERVER_OFFLINE_MESSAGE_LOADED;
	}
}
