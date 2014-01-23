package com.v2tech.logic;

import java.util.ArrayList;
import java.util.List;

import com.v2tech.util.V2Log;

/**
 * As conference's attendee object. <br>
 * 
 * @author 28851274
 *
 */
public class Attendee {

	private User user;
	private List<UserDeviceConfig> mDevices;
	private boolean isSelf;
	private boolean isChairMan;

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
		mDevices.add(udc);
		udc.setBelongsAttendee(this);
	}
	

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public List<UserDeviceConfig> getmDevices() {
		return mDevices;
	}

	public void setmDevices(List<UserDeviceConfig> mDevices) {
		this.mDevices = mDevices;
	}

	public boolean isSelf() {
		return isSelf;
	}

	public void setSelf(boolean isSelf) {
		this.isSelf = isSelf;
	}

	public boolean isChairMan() {
		return isChairMan;
	}

	public void setChairMan(boolean isChairMan) {
		this.isChairMan = isChairMan;
	}

}
