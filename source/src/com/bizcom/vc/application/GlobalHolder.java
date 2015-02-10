package com.bizcom.vc.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.V2.jni.ImRequest;
import com.V2.jni.ind.V2Group;
import com.V2.jni.util.V2Log;
import com.bizcom.request.BitmapManager;
import com.bizcom.vo.AddFriendHistorieNode;
import com.bizcom.vo.ConferenceGroup;
import com.bizcom.vo.ContactGroup;
import com.bizcom.vo.CrowdGroup;
import com.bizcom.vo.CrowdGroup.AuthType;
import com.bizcom.vo.DiscussionGroup;
import com.bizcom.vo.FileDownLoadBean;
import com.bizcom.vo.Group;
import com.bizcom.vo.Group.GroupType;
import com.bizcom.vo.OrgGroup;
import com.bizcom.vo.User;
import com.bizcom.vo.UserDeviceConfig;
import com.v2tech.R;

public class GlobalHolder {

	private static GlobalHolder holder;

	private User mCurrentUser;

	private List<Group> mOrgGroup = new ArrayList<Group>();

	private List<Group> mConfGroup = new ArrayList<Group>();

	private List<Group> mContactsGroup = new ArrayList<Group>();

	private List<Group> mCrowdGroup = new ArrayList<Group>();

	private List<Group> mDiscussionBoardGroup = new ArrayList<Group>();

	private Map<Long, User> mUserHolder = new HashMap<Long, User>();
	private Map<Long, Group> mGroupHolder = new HashMap<Long, Group>();
	private Map<Long, String> mAvatarHolder = new HashMap<Long, String>();

	public List<AddFriendHistorieNode> addFriendHistorieList = new ArrayList<AddFriendHistorieNode>();

	private Map<Long, List<UserDeviceConfig>> mUserDeviceList = new HashMap<Long, List<UserDeviceConfig>>();

	private Map<Long, Bitmap> mAvatarBmHolder = new HashMap<Long, Bitmap>();

	private GlobalState mState = new GlobalState();

	private List<String> dataBaseTableCacheName = new ArrayList<String>();

	public Map<String, FileDownLoadBean> globleFileProgress = new HashMap<String, FileDownLoadBean>();

	public Map<String, String> mTransingLockFiles = new HashMap<String, String>();

	public List<String> mFailedFiles = new ArrayList<String>();

	private volatile boolean p2pAVNeedStickyBraodcast = false;

	public static synchronized GlobalHolder getInstance() {
		if (holder == null) {
			holder = new GlobalHolder();
		}
		return holder;
	}

	private GlobalHolder() {
		BitmapManager.getInstance().registerLastBitmapChangedListener(
				bitmapChangedListener);
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
		if (id <= 0 || u == null) {
			return null;
		}
		synchronized (mUserLock) {
			Long key = Long.valueOf(id);
			User cu = mUserHolder.get(key);
			if (cu != null) {
				// Update user property for received user information
				cu.updateUser(false);

				if (u.getAddress() != null) {
					cu.setAddress(u.getAddress());
				}
				if (u.getAccount() != null) {
					cu.setAccount(u.getAccount());
				}
				cu.setAuthtype(u.getAuthtype());
				if (u.getBirthday() != null) {
					cu.setBirthday(u.getBirthday());
				}
				if (u.getmStringBirthday() != null) {
					cu.setmStringBirthday(u.getmStringBirthday());
				}
				if (u.getmEmail() != null) {
					cu.setEmail(u.getmEmail());
				}
				if (u.getFax() != null) {
					cu.setFax(u.getFax());
				}
				if (u.getJob() != null) {
					cu.setJob(u.getJob());
				}
				if (u.getMobile() != null) {
					cu.setMobile(u.getMobile());
				}
				if (u.getNickName() != null) {
					cu.setNickName(u.getNickName());
				}
				if (u.getSex() != null) {
					cu.setSex(u.getSex());
				}
				if (u.getSignature() != null) {
					cu.setSignature(u.getSignature());
				}
				if (u.getTelephone() != null) {
					cu.setTelephone(u.getTelephone());
				}
				if (u.getName() != null) {
					cu.setName(u.getName());
				}
				return cu;
			}
			mUserHolder.put(key, u);
			Bitmap avatar = mAvatarBmHolder.get(key);
			if (avatar != null) {
				u.setAvatarBitmap(avatar);
			}
		}
		return u;
	}

	/**
	 * Get user object according user ID<br>
	 * If id is negative, will return null.<br>
	 * Otherwise user never return null. If application doesn't receive user
	 * information from server.<br>
	 * User property is dirty {@link User#isDirty()}
	 * 
	 * @param userID
	 * @param isGetUserInfo
	 *            if true , invoke getUserBaseInfo();
	 * @return
	 */
	public User getUser(long userID) {
		if (userID <= 0) {
			return null;
		}
		Long key = Long.valueOf(userID);
		synchronized (key) {
			User tmp = mUserHolder.get(key);
			if (tmp == null) {
				tmp = new User(userID);
				mUserHolder.put(key, tmp);
				if (GlobalHolder.getInstance().getGlobalState().isGroupLoaded()) {
					// if receive this callback , the dirty change false;
					Log.i("20150203 1", "1");
					ImRequest.getInstance().getUserBaseInfo(userID);
				}
			} else if (TextUtils.isEmpty(tmp.getName())) {
				if (GlobalHolder.getInstance().getGlobalState().isGroupLoaded()) {
					// if receive this callback , the dirty change false;
					Log.i("20150203 1", "1");
					ImRequest.getInstance().getUserBaseInfo(userID);
				}
			}

			return tmp;
		}
	}

	/**
	 * Update group information according server's side push data
	 * 
	 * @param gType
	 * @param list
	 * 
	 */
	public void updateGroupList(int gType, List<V2Group> list) {

		for (V2Group vg : list) {
			Group cache = mGroupHolder.get(vg.id);
			if (cache != null) {
				continue;
			}

			if (vg.getName() == null)
				V2Log.e("parse the group name is wroing...the group is :"
						+ vg.id);

			Group g = null;
			if (gType == V2GlobalConstants.GROUP_TYPE_CROWD) {
				boolean flag = true;
				for (Group group : mCrowdGroup) {
					if (group.getmGId() == vg.id) {
						flag = false;
					}
				}

				if (flag) {
					User owner = GlobalHolder.getInstance().getUser(
							vg.owner.uid);
					g = new CrowdGroup(vg.id, vg.getName(), owner);
					((CrowdGroup) g).setBrief(vg.getBrief());
					((CrowdGroup) g).setAnnouncement(vg.getAnnounce());
					((CrowdGroup) g).setAuthType(AuthType.fromInt(vg.authType));
					mCrowdGroup.add(g);
				}
			} else if (gType == V2GlobalConstants.GROUP_TYPE_CONFERENCE) {
				User owner = GlobalHolder.getInstance().getUser(vg.owner.uid);
				User chairMan = GlobalHolder.getInstance().getUser(
						vg.chairMan.uid);
				g = new ConferenceGroup(vg.id, vg.getName(), owner,
						vg.createTime, chairMan);
				mConfGroup.add(g);
			} else if (gType == V2GlobalConstants.GROUP_TYPE_DEPARTMENT) {
				g = new OrgGroup(vg.id, vg.getName());
				mOrgGroup.add(g);
			} else if (gType == V2GlobalConstants.GROUP_TYPE_CONTACT) {
				g = new ContactGroup(vg.id, vg.getName());
				if (vg.isDefault) {
					((ContactGroup) g).setDefault(true);
					g.setName(GlobalConfig.Resource.CONTACT_DEFAULT_GROUP_NAME);
				}

				mContactsGroup.add(g);
			} else if (gType == V2GlobalConstants.GROUP_TYPE_DISCUSSION) {
				User owner = GlobalHolder.getInstance().getUser(vg.owner.uid);
				g = new DiscussionGroup(vg.id, vg.getName(), owner, null);
				mDiscussionBoardGroup.add(g);
			} else {
				throw new RuntimeException(" Can not support this type");
			}

			mGroupHolder.put(g.getmGId(), g);

			populateGroup(gType, g, vg.childs);
		}

	}

	public void addGroupToList(int groupType, Group g) {
		if (groupType == V2GlobalConstants.GROUP_TYPE_DEPARTMENT) {
		} else if (groupType == V2GlobalConstants.GROUP_TYPE_CONFERENCE) {
			mConfGroup.add(g);
		} else if (groupType == V2GlobalConstants.GROUP_TYPE_CROWD) {
			this.mCrowdGroup.add(g);
		} else if (groupType == V2GlobalConstants.GROUP_TYPE_CONTACT) {
			this.mContactsGroup.add(g);
		} else if (groupType == V2GlobalConstants.GROUP_TYPE_DISCUSSION) {
			this.mDiscussionBoardGroup.add(g);
		}
		mGroupHolder.put(Long.valueOf(g.getmGId()), g);
	}

	/**
	 * 
	 * @param groupType
	 * @param gId
	 * @return
	 * 
	 *         {@see com.V2.jni.V2GlobalEnum}
	 */
	public Group getGroupById(int groupType, long gId) {
		return mGroupHolder.get(Long.valueOf(gId));
	}

	/**
	 * @param gId
	 * @return
	 * 
	 *         {@see com.V2.jni.V2GlobalEnum}
	 */
	public Group getGroupById(long gId) {
		return mGroupHolder.get(Long.valueOf(gId));
	}

	private void populateGroup(int groupType, Group parent, Set<V2Group> list) {
		for (V2Group vg : list) {
			Group cache = mGroupHolder.get(Long.valueOf(vg.id));

			Group g = null;
			if (cache != null) {
				g = cache;
				// Update new name
				cache.setName(vg.getName());
			} else {
				if (groupType == V2GlobalConstants.GROUP_TYPE_CROWD) {
					User owner = GlobalHolder.getInstance().getUser(
							vg.owner.uid);
					g = new CrowdGroup(vg.id, vg.getName(), owner);
				} else if (groupType == V2GlobalConstants.GROUP_TYPE_CONFERENCE) {
					User owner = GlobalHolder.getInstance().getUser(
							vg.owner.uid);
					User chairMan = GlobalHolder.getInstance().getUser(
							vg.chairMan.uid);
					g = new ConferenceGroup(vg.id, vg.getName(), owner,
							vg.createTime, chairMan);
				} else if (groupType == V2GlobalConstants.GROUP_TYPE_DEPARTMENT) {
					g = new OrgGroup(vg.id, vg.getName());
				} else if (groupType == V2GlobalConstants.GROUP_TYPE_CONTACT) {
					g = new ContactGroup(vg.id, vg.getName());
				} else {
					throw new RuntimeException(" Can not support this type");
				}
			}

			parent.addGroupToGroup(g);
			mGroupHolder.put(Long.valueOf(g.getmGId()), g);

			populateGroup(groupType, g, vg.childs);
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
	public List<Group> getGroup(int groupType) {
		switch (groupType) {
		case V2GlobalConstants.GROUP_TYPE_DEPARTMENT:
			return this.mOrgGroup;
		case V2GlobalConstants.GROUP_TYPE_CONTACT:
			return mContactsGroup;
		case V2GlobalConstants.GROUP_TYPE_CROWD:
			List<Group> ct = new CopyOnWriteArrayList<Group>();
			ct.addAll(this.mCrowdGroup);
			return ct;
		case V2GlobalConstants.GROUP_TYPE_CONFERENCE:
			List<Group> confL = new ArrayList<Group>();
			confL.addAll(this.mConfGroup);
			Collections.sort(confL);
			List<Group> sortConfL = new CopyOnWriteArrayList<Group>(confL);
			return sortConfL;
		case V2GlobalConstants.GROUP_TYPE_DISCUSSION:
			return mDiscussionBoardGroup;
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
		} else {
			V2Log.e("GlobalHolder removeGroupUser", " Remove user failed ! get group is null "
					+ " group id is : " + gid + " user id is : " + uid);
		}
	}

	/**
	 * Add user collections to group collections
	 * 
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

	public void addUserToGroup(User u, long belongGID) {
		Group g = findGroupById(belongGID);
		if (g == null) {
			V2Log.e("Doesn't receive group<" + belongGID + "> information yet!");
			return;
		}
		g.addUserToGroup(u);
	}

	public boolean removeGroup(GroupType gType, long gid) {
		List<Group> list = null;
		if (gType == GroupType.CONFERENCE) {
			list = mConfGroup;
		} else if (gType == GroupType.CONTACT) {
			list = mContactsGroup;
		} else if (gType == GroupType.CHATING) {
			list = mCrowdGroup;
		} else if (gType == GroupType.ORG) {
			list = mOrgGroup;
		} else if (gType == GroupType.DISCUSSION) {
			list = mDiscussionBoardGroup;
		}
		mGroupHolder.remove(Long.valueOf(gid));
		if (list != null) {
			for (int i = 0; i < list.size(); i++) {
				Group g = list.get(i);
				if (g.getmGId() == gid) {
					list.remove(g);
					return true;
				}
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
		List<UserDeviceConfig> list = mUserDeviceList.get(Long.valueOf(uid));
		if (list == null) {
			return null;
		}

		return new ArrayList<UserDeviceConfig>(list);
	}

	public UserDeviceConfig getUserDefaultDevice(long uid) {
		List<UserDeviceConfig> list = mUserDeviceList.get(Long.valueOf(uid));
		if (list == null) {
			return null;
		}
		for (UserDeviceConfig udc : list) {
			if (udc.isDefault()) {
				return udc;
			}
		}

		if (list.size() > 0) {
			V2Log.e("Not found default device, use first device !");
			return list.iterator().next();
		}
		return null;
	}

	/**
	 * Update user video device and clear existed user device first
	 * 
	 * @param id
	 * @param udcList
	 */
	public void updateUserDevice(long id, List<UserDeviceConfig> udcList) {
		Long key = Long.valueOf(id);
		List<UserDeviceConfig> list = mUserDeviceList.get(key);
		if (list != null) {
			list.clear();
		} else {
			list = new ArrayList<UserDeviceConfig>();
			mUserDeviceList.put(key, list);
		}

		list.addAll(udcList);
	}

	/**
	 * Set current app audio state, also set voice connected state
	 * 
	 * @param flag
	 * @param uid
	 */
	public void setAudioState(boolean flag, long uid) {
		synchronized (mState) {
			int st = this.mState.getState();
			if (flag) {
				st |= GlobalState.STATE_IN_AUDIO_CONVERSATION;
			} else {
				st &= (~GlobalState.STATE_IN_AUDIO_CONVERSATION);
			}
			this.mState.setState(st);
			this.mState.setUid(uid);
			setVoiceConnectedState(flag);
		}
	}

	/**
	 * set cuurent app video state
	 * 
	 * @param flag
	 * @param uid
	 */
	public void setVideoState(boolean flag, long uid) {
		synchronized (mState) {
			int st = this.mState.getState();
			if (flag) {
				st |= GlobalState.STATE_IN_VIDEO_CONVERSATION;
			} else {
				st &= (~GlobalState.STATE_IN_VIDEO_CONVERSATION);
			}
			this.mState.setState(st);
			this.mState.setUid(uid);
		}
	}

	public void setMeetingState(boolean flag, long gid) {
		synchronized (mState) {
			int st = this.mState.getState();
			if (flag) {
				st |= GlobalState.STATE_IN_MEETING_CONVERSATION;
			} else {
				st &= (~GlobalState.STATE_IN_MEETING_CONVERSATION);
			}
			this.mState.setState(st);
			this.mState.setGid(gid);
		}
	}

	public void setVoiceConnectedState(boolean flag) {
		synchronized (mState) {
			int st = this.mState.getState();
			if (flag) {
				st |= GlobalState.STATE_IN_VOICE_CONNECTED;
			} else {
				st &= (~GlobalState.STATE_IN_VOICE_CONNECTED);
			}
			this.mState.setState(st);
		}
	}

	public void setServerConnection(boolean connected) {
		synchronized (mState) {
			int st = this.mState.getState();
			if (connected) {
				st |= GlobalState.STATE_SERVER_CONNECTED;
			} else {
				st &= (~GlobalState.STATE_SERVER_CONNECTED);
			}
			this.mState.setState(st);
		}
	}

	public void setOfflineLoaded(boolean isLoad) {
		synchronized (mState) {
			int st = this.mState.getState();
			if (isLoad) {
				st |= GlobalState.STATE_SERVER_OFFLINE_MESSAGE_LOADED;
			} else {
				st &= (~GlobalState.STATE_SERVER_OFFLINE_MESSAGE_LOADED);
			}
			this.mState.setState(st);
		}
	}

	public void setP2pAVNeedStickyBraodcast(boolean p2pAVNeedStickyBraodcast) {
		this.p2pAVNeedStickyBraodcast = p2pAVNeedStickyBraodcast;
	}

	public boolean isVoiceConnected() {
		synchronized (mState) {
			return this.mState.isVoiceConnected();
		}
	}

	public boolean isInAudioCall() {
		synchronized (mState) {
			return this.mState.isInAudioCall();
		}
	}

	public boolean isInVideoCall() {
		synchronized (mState) {
			return mState.isInVideoCall();
		}
	}

	public boolean isInMeeting() {
		synchronized (mState) {
			return mState.isInMeeting();
		}
	}

	public boolean isServerConnected() {
		synchronized (mState) {
			return mState.isConnectedServer();
		}
	}

	public boolean isFriend(User user) {

		synchronized (mState) {
			if (user == null) {
				V2Log.e("GlobalHolder isFriend ---> get user is null , please check conversation user is exist");
				return false;
			}

			long currentUserID = user.getmUserId();
			List<Group> friendGroup = GlobalHolder.getInstance().getGroup(
					V2GlobalConstants.GROUP_TYPE_CONTACT);
			if (friendGroup.size() >= 0) {
				for (Group friend : friendGroup) {
					List<User> users = friend.getUsers();
					for (User friendUser : users) {
						if (currentUserID == friendUser.getmUserId()) {
							return true;
						}
					}
				}
			}
			return false;
		}
	}

	public boolean isOfflineLoaded() {
		synchronized (mState) {
			return mState.isOfflineLoaded();
		}
	}

	public boolean isP2pAVNeedStickyBraodcast() {
		return p2pAVNeedStickyBraodcast;
	}

	/**
	 * Get current application state copy
	 * 
	 * @return
	 */
	public GlobalState getGlobalState() {
		return new GlobalState(this.mState);
	}

	/**
	 * Set bluetooth headset matched or not
	 * 
	 * @param flag
	 */
	public void setBluetoothHeadset(boolean flag) {
		synchronized (mState) {
			mState.setBluetoothHeadset(flag);
		}
	}

	public void setGroupLoaded() {
		synchronized (mState) {
			synchronized (mState) {
				int st = mState.getState();
				mState.setState(st | GlobalState.STATE_SERVER_GROUPS_LOADED);
			}
		}
	}

	/**
	 * Set wired headset state
	 * 
	 * @param flag
	 */
	public void setWiredHeadsetState(boolean flag) {
		synchronized (mState) {
			mState.setWiredHeadsetState(flag);
		}
	}

	public Bitmap getUserAvatar(long id) {
		Long key = Long.valueOf(id);
		return mAvatarBmHolder.get(key);
	}

	public List<String> getDataBaseTableCacheName() {
		return dataBaseTableCacheName;
	}

	public void setDataBaseTableCacheName(List<String> dataBaseTableCacheName) {
		this.dataBaseTableCacheName = dataBaseTableCacheName;
	}

	public boolean changeGlobleTransFileMember(final int transType,
			final Context mContext, boolean isAdd, Long key, String tag) {
		key = GlobalHolder.getInstance().getCurrentUserId();
		Map<Long, Integer> transingCollection = getFileTypeColl(transType);
		Integer transing = transingCollection.get(key);
		String typeString = null;
		if (transType == V2GlobalConstants.FILE_TRANS_SENDING)
			typeString = mContext.getResources().getString(
					R.string.application_global_holder_send_or_upload);
		else
			typeString = mContext.getResources().getString(
					R.string.application_global_holder_download);
		if (transing == null) {
			if (isAdd) {
				V2Log.d("TRANSING_FILE_SIZE", tag + " --> ID为- " + key
						+ " -的用户或群 , " + "传输类型 : " + typeString
						+ " 正在传输文件加1 , 当前数量为1");
				transing = 1;
				transingCollection.put(key, transing);
			}
			return true;
		} else {
			if (isAdd) {
				if (transing >= GlobalConfig.MAX_TRANS_FILE_SIZE) {
					new Thread(new Runnable() {

						@Override
						public void run() {
							Looper.prepare();
							// if(transType == V2GlobalEnum.FILE_TRANS_SENDING)
							// Toast.makeText(mContext,
							// "发送文件个数已达上限，当前正在传输的文件数量已达5个",
							// Toast.LENGTH_LONG).show();
							// else
							// Toast.makeText(mContext,
							// "下载文件个数已达上限，当前正在下载的文件数量已达5个",
							// Toast.LENGTH_LONG).show();
							Toast.makeText(
									mContext,
									R.string.application_global_holder_limit_number,
									Toast.LENGTH_LONG).show();
							Looper.loop();
						}
					}).start();
					return false;
				} else {
					transing = transing + 1;
					V2Log.d("TRANSING_FILE_SIZE", tag + " --> ID为- " + key
							+ " -的用户或群 , " + "传输类型 : " + typeString
							+ " 正在传输文件加1 , 当前数量为: " + transing);
					transingCollection.put(key, transing);
					return true;
				}
			} else {
				if (transing == 0)
					return false;
				transing = transing - 1;
				V2Log.d("TRANSING_FILE_SIZE", tag + " --> ID为- " + key
						+ " -的用户或群 , " + "传输类型 : " + typeString
						+ " 正在传输文件减1 , 当前数量为: " + transing);
				transingCollection.put(key, transing);
				return true;
			}
		}

	}

	private Map<Long, Integer> getFileTypeColl(int transType) {
		return GlobalConfig.mTransingFiles;
		// if(transType == V2GlobalEnum.FILE_TRANS_SENDING)
		// return GlobalConfig.mTransingFiles;
		// else
		// return GlobalConfig.mDownLoadingFiles;
	}

	/**
	 * Use to update cache avatar
	 */
	private BitmapManager.BitmapChangedListener bitmapChangedListener = new BitmapManager.BitmapChangedListener() {

		@Override
		public void notifyAvatarChanged(User user, Bitmap newAvatar) {
			Long key = Long.valueOf(user.getmUserId());
			Bitmap cache = mAvatarBmHolder.get(key);
			if (cache != null) {
				cache.recycle();
			}
			mAvatarBmHolder.put(key, newAvatar);
		}

	};

}
