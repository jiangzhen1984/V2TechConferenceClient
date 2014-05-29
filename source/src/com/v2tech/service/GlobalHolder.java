package com.v2tech.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import android.graphics.Bitmap;

import com.v2tech.util.V2Log;
import com.v2tech.view.bo.UserStatusObject;
import com.v2tech.vo.Conversation;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.User;
import com.v2tech.vo.UserDeviceConfig;

public class GlobalHolder {

	private static GlobalHolder holder;

	private User mCurrentUser;

	private Map<Long, UserStatusObject> onlineUsers = new HashMap<Long, UserStatusObject>();

	private List<Group> mOrgGroup = null;

	private Set<Group> mConfGroup = new CopyOnWriteArraySet<Group>();

	private Set<Group> mContactsGroup = new HashSet<Group>();

	private Set<Group> mCrowdGroup = new HashSet<Group>();

	private Map<Long, User> mUserHolder = new HashMap<Long, User>();
	private Map<Long, Group> mGroupHolder = new HashMap<Long, Group>();
	private Map<Long, String> mAvatarHolder = new HashMap<Long, String>();

	private Set<Conversation> mConatactConversationHolder = new CopyOnWriteArraySet<Conversation>();

	private Set<Conversation> mGroupConversationHolder = new CopyOnWriteArraySet<Conversation>();

	private Set<Conversation> mConferenceConversationHolder = new CopyOnWriteArraySet<Conversation>();

	private Map<Long, Set<UserDeviceConfig>> mUserDeviceList = new HashMap<Long, Set<UserDeviceConfig>>();

	private Map<Long, Bitmap> mAvatarBmHolder = new HashMap<Long, Bitmap>();

	// Use to hold current opened conversation
	public Conversation CURRENT_CONVERSATION = null;

	public long CURRENT_ID = 0;

	public static synchronized GlobalHolder getInstance() {
		if (holder == null) {
			holder = new GlobalHolder();
		}
		return holder;
	}

	private GlobalHolder() {
		BitmapManager.getInstance().registerLastBitmapChangedListener(bitmapChangedListener);
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
			// putUser(u.getmUserId(), u);
		}
	}

	private Object mUserLock = new Object();

	public User putUser(long id, User u) {
		if (u == null) {
			return null;
		}
		synchronized (mUserLock) {
			Long key = Long.valueOf(id);
			User cu = mUserHolder.get(key);
			if (cu != null) {
				if (u.getSignature() != null) {
					cu.setSignature(u.getSignature());
				}
				if (u.getName() != null) {
					cu.setName(u.getName());
				}
				if (u.getGender() != null) {
					cu.setGender(u.getGender());
				}
				V2Log.e(" merge user information " + id);
				return cu;
			}
			mUserHolder.put(key, u);
			Bitmap avatar = mAvatarBmHolder.get(key);
			if (avatar != null) {
				u.setAvatarBitmap(avatar);
				mAvatarBmHolder.remove(avatar);
			}
		}
		
		return u;
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
			onlineUsers
					.put(key, new UserStatusObject(u.getmUserId(), u
							.getDeviceType().toIntValue(), u.getmStatus()
							.toIntValue()));
		}

	}

	public void updateUserStatus(long uid, UserStatusObject us) {
		Long key = Long.valueOf(uid);
		if (User.Status.fromInt(us.getStatus()) == User.Status.OFFLINE) {
			onlineUsers.remove(key);
		} else {
			onlineUsers.put(key, us);
		}
	}

	public UserStatusObject getOnlineUserStatus(long uid) {
		Long key = Long.valueOf(uid);
		return onlineUsers.get(key);
	}

	/**
	 * Update group information according server's side push data
	 * 
	 * @param gType
	 * @param list
	 * 
	 *            FIXME need to optimize code
	 */
	public void updateGroupList(Group.GroupType gType, List<Group> list) {

		if (gType == Group.GroupType.ORG) {
			mOrgGroup = list;
		} else if (gType == Group.GroupType.CONFERENCE) {
			synchronized (mConfGroup) {
				mConfGroup.addAll(list);
			}
		} else if (gType == Group.GroupType.CONTACT) {
			this.mContactsGroup.addAll(list);
		} else if (gType == GroupType.CHATING) {
			this.mCrowdGroup.addAll(list);
		}
		for (Group g : list) {
			g.setOwnerUser(this.getUser(g.getOwner()));
			populateGroup(g);
		}
	}

	public void addGroupToList(Group.GroupType gType, Group g) {
		if (gType == Group.GroupType.ORG) {
		} else if (gType == Group.GroupType.CONFERENCE) {
			mConfGroup.add(g);
		} else if (gType == Group.GroupType.CHATING) {
			this.mCrowdGroup.add(g);
		}
		mGroupHolder.put(Long.valueOf(g.getmGId()), g);
	}

	public Group getGroupById(Group.GroupType gType, long gId) {
		Set<Group> gSet = null;
		if (gType == Group.GroupType.CONFERENCE) {
			gSet = mConfGroup;
		} else if (gType == Group.GroupType.CHATING) {
			gSet = mCrowdGroup;
		}

		if (gSet == null) {
			V2Log.e(" doesn't initialize collection " + gType.intValue()
					+ "    gid:" + gId);
			return null;
		}

		for (Group g : gSet) {
			if (g.getmGId() == gId) {
				return g;
			}
		}
		return null;
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
		case ORG:
			return this.mOrgGroup;
		case CONTACT:
			List<Group> cl = new CopyOnWriteArrayList<Group>();
			cl.addAll(this.mContactsGroup);
			return cl;
		case CHATING:
			List<Group> ct = new CopyOnWriteArrayList<Group>();
			ct.addAll(this.mCrowdGroup);
			return ct;
		case CONFERENCE:
			List<Group> confL = new ArrayList<Group>();
			confL.addAll(this.mConfGroup);
			Collections.sort(confL);
			List<Group> sortConfL = new CopyOnWriteArrayList<Group>(confL);
			return sortConfL;
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

	public void removeGroupUser(long gid, long uid) {
		Group g = this.findGroupById(gid);
		if (g != null) {
			g.removeUserFromGroup(uid);
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

		// update reference for conference group
		for (Group confG : mConfGroup) {
			User u = getUser(confG.getOwner());
			if (u != null) {
				confG.setOwnerUser(u);
			}
		}
	}

	public void addUserToGroup(User u, long belongGID) {
		Group g = findGroupById(belongGID);
		if (g == null) {
			V2Log.e("Doesn't receive group<" + belongGID + "> information yet!");
			return;
		}
		g.addUserToGroup(u);
	}

	public boolean removeConferenceGroup(long gid) {
		for (Group g : mConfGroup) {
			if (g.getmGId() == gid) {
				mConfGroup.remove(g);
				return true;
			}
		}
		return false;
	}

	public String getAvatarPath(long uid) {
		Long key = Long.valueOf(uid);
		return this.mAvatarHolder.get(key);
	}

	public void putAvatar(long uid, String path) {
		Long key = Long.valueOf(uid);
		this.mAvatarHolder.put(key, path);
	}

	public void addConversation(Conversation cov) {
		if (cov == null) {
			return;
		}
		if (Conversation.TYPE_CONTACT.equals(cov.getType())) {
			this.mConatactConversationHolder.add(cov);
		} else if (Conversation.TYPE_GROUP.equals(cov.getType())) {
			this.mGroupConversationHolder.add(cov);
		} else if (Conversation.TYPE_CONFERNECE.equals(cov.getType())) {
			mConferenceConversationHolder.add(cov);
		}
	}

	public boolean findConversation(Conversation cov) {
		if (cov == null) {
			return false;
		}
		Set<Conversation> tmp = null;
		if (Conversation.TYPE_CONTACT.equals(cov.getType())) {
			tmp = mConatactConversationHolder;
		} else if (Conversation.TYPE_GROUP.equals(cov.getType())) {
			tmp = mGroupConversationHolder;
		} else if (Conversation.TYPE_CONFERNECE.equals(cov.getType())) {
			tmp = mConferenceConversationHolder;
		}
		for (Conversation c : tmp) {
			if (cov.equals(c)) {
				return true;
			}
		}
		return false;
	}

	public Conversation findConversationByType(String type, long extId) {
		Set<Conversation> tmp = null;
		if (Conversation.TYPE_CONTACT.equals(type)) {
			tmp = mConatactConversationHolder;
		} else if (Conversation.TYPE_GROUP.equals(type)) {
			tmp = mGroupConversationHolder;
		} else if (Conversation.TYPE_CONFERNECE.equals(type)) {
			tmp = mConferenceConversationHolder;
		}
		if (tmp == null) {
			V2Log.w("No avialiable holder : " + type);
			return null;
		}

		for (Conversation c : tmp) {
			if (c.getExtId() == extId) {
				return c;
			}
		}
		return null;
	}

	public void removeConversation(String type, long extId) {
		Set<Conversation> tmp = null;
		if (Conversation.TYPE_CONTACT.equals(type)) {
			tmp = mConatactConversationHolder;
		} else if (Conversation.TYPE_GROUP.equals(type)) {
			tmp = mGroupConversationHolder;
		} else if (Conversation.TYPE_CONFERNECE.equals(type)) {
			tmp = mConferenceConversationHolder;
		}

		for (Conversation c : tmp) {
			if (c.getExtId() == extId) {
				tmp.remove(c);
			}
		}
	}

	public int getNoticatorCount(String type) {
		int c = 0;
		Set<Conversation> tmp = null;
		if (Conversation.TYPE_CONTACT.equals(type)) {
			tmp = mConatactConversationHolder;
		} else if (Conversation.TYPE_GROUP.equals(type)) {
			tmp = mGroupConversationHolder;
		} else if (Conversation.TYPE_CONFERNECE.equals(type)) {
			tmp = mConferenceConversationHolder;
		}

		if (tmp == null) {
			return 0;
		}
		for (Conversation cov : tmp) {
			if (cov.getNotiFlag() == Conversation.NOTIFICATION) {
				c++;
			}
		}
		return c;
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
		Set<UserDeviceConfig> list = mUserDeviceList.get(Long.valueOf(uid));
		if (list == null) {
			return null;
		}
		
		return new ArrayList<UserDeviceConfig>(list);
	}
	
	
	public void removeAttendeeDeviceCache(long uid) {
		mUserDeviceList.remove(Long.valueOf(uid));
	}


	public void addAttendeeDevice(List<UserDeviceConfig> udcList) {
		for (UserDeviceConfig udc : udcList) {
			if (udc == null) {
				continue;
			}
			Set<UserDeviceConfig> list = mUserDeviceList.get(Long.valueOf(udc.getUserID()));
			if (list == null) {
				list = new HashSet<UserDeviceConfig>();
				mUserDeviceList.put(Long.valueOf(udc.getUserID()), list);
			}
			list.add(udc);
		}
	}

	
	/**
	 * Use to update cache avatar
	 */
	private BitmapManager.BitmapChangedListener bitmapChangedListener = new BitmapManager.BitmapChangedListener() {

		@Override
		public void notifyAvatarChanged(User user, Bitmap newAvatar) {
			User u = getUser(user.getmUserId());
			if (u != null) {
				Bitmap cache = u.getAvatarBitmap();
				if (cache != null) {
					cache.recycle();
				}
				u.setAvatarBitmap(newAvatar);
			//Doesn't receive user information from server
			} else {
				mAvatarBmHolder.put(Long.valueOf(user.getmUserId()), newAvatar);
			}
		}
		
	};

}
