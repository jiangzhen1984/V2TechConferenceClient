package com.v2tech.vo;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.text.TextUtils;

import com.V2.jni.util.V2Log;

/**
 * As conference's attendee object. <br>
 * 
 * @author 28851274
 * 
 */
public class Attendee implements Comparable<Attendee> {

	public static final int LECTURE_STATE_NOT = 0;
	public static final int LECTURE_STATE_APPLYING = 1;
	public static final int LECTURE_STATE_GRANTED = 2;

	public final static int TYPE_ATTENDEE = 1;
	public final static int TYPE_MIXED_VIDEO = 2;

	private User user;
	protected List<UserDeviceConfig> mDevices;
	private boolean isSelf;
	protected boolean isChairMan;
	protected boolean isJoined;
	private boolean isSpeaking;
	private int lectureState=LECTURE_STATE_NOT;
	public boolean isRmovedFromList; //快速入会用户 ，退出直接从列表删除
	
	protected Attendee() {

	}

	public Attendee(User user) {
		this(user, null, false, false);
	}

	public Attendee(User user, boolean isSelf, boolean isChairMan) {
		this(user, null, isSelf, isChairMan);
	}

	public Attendee(User user, List<UserDeviceConfig> mDevices) {
		this(user, mDevices, false, false);
	}

	public Attendee(User user, List<UserDeviceConfig> mDevices, boolean isSelf,
			boolean isChairMan) {
		super();
		this.user = user;
		this.mDevices = mDevices;
		this.isSelf = isSelf;
		this.isChairMan = isChairMan;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((user == null) ? 0 : user.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Attendee other = (Attendee) obj;
		if (user == null) {
			if (other.user != null)
				return false;
		} else if (!user.equals(other.user))
			return false;
		return true;
	}

	public UserDeviceConfig getDefaultDevice() {
		if (this.mDevices != null && this.mDevices.size() > 0) {
			return this.mDevices.get(0);
		} else {
			return null;
		}
	}

	public void addDevice(UserDeviceConfig udc) {
		if (udc == null) {
			V2Log.w(" null device");
			return;
		}
		if (mDevices == null) {
			mDevices = new ArrayList<UserDeviceConfig>();
		}
		for (int i = 0; i < mDevices.size(); i++) {
			if (mDevices.get(i).equals(udc)) {
				V2Log.w("device " + udc.getDeviceID() + "  exist");
				return;
			}
		}
		mDevices.add(udc);
		udc.setBelongsAttendee(this);
	}

	public void addDevice(List<UserDeviceConfig> udcs) {
		if (udcs == null) {
			V2Log.w(" null device");
			return;
		}
		for (UserDeviceConfig ud : udcs) {
			this.addDevice(ud);
		}
	}

	// public User getUser() {
	// return user;
	// }

	public long getAttId() {
		if (user != null) {
			return user.getmUserId();
		}
		return 0;
	}

	public String getAttName() {
		if (user != null) {
			if (!TextUtils.isEmpty(user.getNickName()))
				return user.getNickName();
			else
				return user.getName();

		}
		return null;
	}

	public String getAbbraName() {
		if (user != null) {
			return user.getArra();
		}
		return null;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public List<UserDeviceConfig> getmDevices() {
		return mDevices;
	}

	public void setmDevices(List<UserDeviceConfig> mDevices) {
		this.mDevices = mDevices;
		if (this.mDevices == null) {
			return;
		}
		for (UserDeviceConfig udc : mDevices) {
			udc.setBelongsAttendee(this);
		}
	}

	public boolean isSelf() {
		return isSelf;
	}

	public void setSelf(boolean isSelf) {
		this.isSelf = isSelf;
		this.isJoined = true;
	}

	public boolean isChairMan() {
		return isChairMan;
	}

	public void setChairMan(boolean isChairMan) {
		this.isChairMan = isChairMan;
	}

	public boolean isJoined() {
		return isJoined;
	}

	public void setJoined(boolean isJoined) {
		this.isJoined = isJoined;
	}

	public Bitmap getAvatar() {
		if (user == null) {
			return null;
		}
		Bitmap map = this.user.getAvatarBitmap();
		return map;
	}

	public int getType() {
		return TYPE_ATTENDEE;
	}

	public boolean isSpeaking() {
		return isSpeaking;
	}

	public void setSpeakingState(boolean isSpeaking) {
		this.isSpeaking = isSpeaking;
	}

	public int getLectureState() {
		return lectureState;
	}

	public void setLectureState(int lectureState) {
		this.lectureState = lectureState;
	}

	@Override
	public int compareTo(Attendee attendee) {
		if (this.user == null) {
			return 1;
		}
		if (attendee.user == null) {
			return -1;
		}

		if (this.user.getmUserId() == attendee.user.getmUserId()) {
			return 0;
		else
			return 1;
	}
	

}
