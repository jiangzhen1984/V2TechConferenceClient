package com.v2tech.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.v2tech.util.V2Log;

public class GlobalHolder {

	private static GlobalHolder holder;

	private User mCurrentUser;

	private Map<Long, User.Status> onlineUsers = new HashMap<Long, User.Status>();

	private List<Group> mContactsGroup = null;

	private List<Group> mConfGroup = null;

	private Map<Long, User> mUserHolder = new HashMap<Long, User>();
	private Map<Long, Group> mGroupHolder = new HashMap<Long, Group>();
	private Map<Long, String> mAvatarHolder = new HashMap<Long, String>();

	private List<Conversation> mConversationHolder = new ArrayList<Conversation>();
	

	private Set<UserDeviceConfig> mUserDeviceList = new HashSet<UserDeviceConfig>();

	public static synchronized GlobalHolder getInstance() {
		if (holder == null) {
			holder = new GlobalHolder();
		}
		return holder;
	}

	private GlobalHolder() {

	}

	public User getCurrentUser() {
		return mCurrentUser;
	}

	public long getCurrentUserId() {
		if (mCurrentUser == null) {
			return 0;
		} else {
			return mCurrentUser.getmUserId();
		}
	}

	public void setCurrentUser(User u) {
		this.mCurrentUser = u;
		this.mCurrentUser.setCurrentLoggedInUser(true);
		this.mCurrentUser.updateStatus(User.Status.ONLINE);
		User mU = getUser(u.getmUserId());
		if (mU != null) {
			mU.updateStatus(User.Status.ONLINE);
		} else {
			putUser(u.getmUserId(), u);
		}
	}

	private Object mUserLock = new Object();
	public void putUser(long id, User u) {
		if (u == null) {
			return;
		}
		synchronized(mUserLock) {
			Long key = Long.valueOf(id);
			User cu = mUserHolder.get(key);
			if (cu != null) {
				if (u.getSignature() != null) {
					cu.setSignature(u.getSignature());
				}
				if (u.getName() != null) {
					cu.setName(u.getName());
				}
				//FIXME update new data
				V2Log.e(" merge user information ");
				return;
			}
			mUserHolder.put(key, u);
		}
	}

	public User getUser(long id) {
			Long key = Long.valueOf(id);
			return mUserHolder.get(key);
	}

	public void updateUserStatus(User u) {
		Long key = Long.valueOf(u.getmUserId());
		if (u.getmStatus() == User.Status.OFFLINE) {
			onlineUsers.remove(key);
		} else {
			onlineUsers.put(key, u.getmStatus());
		}

	}

	public void updateUserStatus(long uid, User.Status us) {
		Long key = Long.valueOf(uid);
		if (us == User.Status.OFFLINE) {
			onlineUsers.remove(key);
		} else {
			onlineUsers.put(key, us);
		}
	}

	public User.Status getOnlineUserStatus(long uid) {
		Long key = Long.valueOf(uid);
		return onlineUsers.get(key);
	}

	/**
	 * Update group information according server's side push data
	 * 
	 * @param gType
	 * @param list
	 */
	public void updateGroupList(Group.GroupType gType, List<Group> list) {
		if (gType == Group.GroupType.CONTACT) {
			mContactsGroup = list;
		} else if (gType == Group.GroupType.CONFERENCE) {
			mConfGroup = list;
		}
		for (Group g : list) {
			g.setOwnerUser(this.getUser(g.getOwner()));
			populateGroup(g);
		}
	}

	private void populateGroup(Group g) {
		mGroupHolder.put(Long.valueOf(g.getmGId()), g);
		for (Group subG : g.getChildGroup()) {
			populateGroup(subG);
		}
	}

	/**
	 * Group information is server active call, we can't request from server
	 * directly.<br>
	 * Only way to get group information is waiting for server call.<br>
	 * So if this function return null, means service doesn't receive any call
	 * from server. otherwise server already sent group information to service.<br>
	 * If you want to know indication, please register receiver:<br>
	 * category: {@link #JNI_BROADCAST_CATEGROY} <br>
	 * action : {@link #JNI_BROADCAST_GROUP_NOTIFICATION}<br>
	 * Notice: maybe you didn't receive broadcast forever, because this
	 * broadcast is sent before you register
	 * 
	 * @param gType
	 * @return return null means server didn't send group information to
	 *         service.
	 */
	public List<Group> getGroup(Group.GroupType gType) {
		switch (gType) {
		case CONTACT:
			return this.mContactsGroup;
		case CONFERENCE:
			return mConfGroup;
		default:
			throw new RuntimeException("Unkonw type");
		}

	}

	/**
	 * Find all types of group information according to group ID
	 * 
	 * @param gid
	 * @return null if doesn't find group, otherwise return Group information
	 * 
	 * @see Group
	 */
	public Group findGroupById(long gid) {
		return mGroupHolder.get(Long.valueOf(gid));
	}

	/**
	 * Add user collections to group collections
	 * 
	 * @param gList
	 * @param uList
	 * @param belongGID
	 */
	public void addUserToGroup(List<Group> gList, List<User> uList,
			long belongGID) {
		for (Group g : gList) {
			if (belongGID == g.getmGId()) {
				g.addUserToGroup(uList);
				return;
			}
			addUserToGroup(g.getChildGroup(), uList, belongGID);
		}
	}

	/**
	 * Add user collections to group collections
	 * 
	 * @param gList
	 * @param uList
	 * @param belongGID
	 */
	public void addUserToGroup(List<User> uList, long belongGID) {
		Group g = findGroupById(belongGID);
		if (g == null) {
			V2Log.e("Doesn't receive group<" + belongGID + "> information yet!");
			return;
		}
		g.addUserToGroup(uList);
	}

	public String getAvatarPath(long uid) {
		Long key = Long.valueOf(uid);
		return this.mAvatarHolder.get(key);
	}

	public void putAvatar(long uid, String path) {
		Long key = Long.valueOf(uid);
		this.mAvatarHolder.put(key, path);
	}

	public void updateConversation(List<Conversation> cache) {
		this.mConversationHolder.addAll(cache);
	}

	public void addConversation(Conversation con) {
		this.mConversationHolder.add(con);
	}

	public boolean findConversation(Conversation cov) {
		for (Conversation c : this.mConversationHolder) {
			if (cov.equals(c)) {
				return true;
			}
		}
		return false;
	}

	public Conversation findConversationByType(String type, long extId) {
		for (Conversation c : this.mConversationHolder) {
			if (c.getExtId() == extId
					&& ((type == null && c.getType() == type) || (type != null && type
							.equals(c.getType())))) {
				return c;
			}
		}
		return null;
	}
	
	

	/**
	 * Get user's video device according to user id.<br>
	 * This function never return null, even through we don't receive video
	 * device data from server.
	 * 
	 * @param uid
	 *            user's id
	 * @return list of user device
	 */
	public List<UserDeviceConfig> getAttendeeDevice(long uid) {
		List<UserDeviceConfig> l = new ArrayList<UserDeviceConfig>();
		for (UserDeviceConfig udl : mUserDeviceList) {
			if (udl.getUserID() == uid) {
				l.add(udl);
			}
		}
		return l;
	}
	
	public void addAttendeeDevice(UserDeviceConfig udc) {
		mUserDeviceList.add(udc);
	}
	
	public void addAttendeeDevice(List<UserDeviceConfig> udcList) {
		mUserDeviceList.addAll(udcList);
	}
}
