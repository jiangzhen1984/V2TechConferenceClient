package com.bizcom.request;

import java.util.ArrayList;
import java.util.List;

import android.os.Handler;
import android.os.Message;

import com.V2.jni.FileRequest;
import com.V2.jni.GroupRequest;
import com.V2.jni.callbacAdapter.FileRequestCallbackAdapter;
import com.V2.jni.callbacAdapter.GroupRequestCallbackAdapter;
import com.V2.jni.ind.FileJNIObject;
import com.V2.jni.ind.GroupAddUserJNIObject;
import com.V2.jni.ind.GroupFileJNIObject;
import com.V2.jni.ind.V2Group;
import com.V2.jni.ind.V2User;
import com.V2.jni.util.V2Log;
import com.V2.jni.util.XmlAttributeExtractor;
import com.bizcom.request.jni.CreateCrowdResponse;
import com.bizcom.request.jni.CreateDiscussionBoardResponse;
import com.bizcom.request.jni.FileTransStatusIndication;
import com.bizcom.request.jni.JNIResponse;
import com.bizcom.request.jni.RequestFetchGroupFilesResponse;
import com.bizcom.request.jni.FileTransStatusIndication.FileTransErrorIndication;
import com.bizcom.request.jni.FileTransStatusIndication.FileTransProgressStatusIndication;
import com.bizcom.vc.application.GlobalConfig;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.application.V2GlobalConstants;
import com.bizcom.vo.Crowd;
import com.bizcom.vo.CrowdGroup;
import com.bizcom.vo.DiscussionGroup;
import com.bizcom.vo.Group;
import com.bizcom.vo.User;
import com.bizcom.vo.VCrowdFile;
import com.bizcom.vo.Group.GroupType;

//组别统一名称
//1:部门, organizationGroup
//2:好友组, contactGroup
//3:群, crowdGroup
//4:会议, conferenceGroup
//5:讨论组,discussionGroup
/**
 * Crowd group service, used to create crowd and remove crowd
 * 
 * @author 28851274
 * 
 */
public class CrowdGroupService extends V2AbstractHandler {

	private static final int ACCEPT_JOIN_CROWD = 0x0002;
	private static final int UPDATE_CROWD = 0x0004;
	private static final int QUIT_CROWD = 0x0005;
	private static final int ACCEPT_APPLICATION_CROWD = 0x0006;
	private static final int REFUSE_APPLICATION_CROWD = 0x0007;
	private static final int FETCH_FILES_CROWD = 0x0008;
	private static final int REMOVE_FILES_CROWD = 0x0009;
	
	
	private static final int QUIT_DISCUSSION_BOARD = 0x000A;
	private static final int CREATE_DISCUSSION_BOARD = 0x000B;
	private static final int UPDATE_DISCUSSION_BOARD = 0x000C;

	private static final int KEY_FILE_TRANS_STATUS_NOTIFICATION_LISTNER = 2;
	private static final int KEY_FILE_REMOVED_NOTIFICATION_LISTNER = 3;
	private static final int KEY_FILE_NEW_NOTIFICATION_LISTNER = 4;

	private GroupRequestCB grCB;
	private FileRequestCB frCB;

	public void setmPendingCrowdId(long mPendingCrowdId) {
		this.mPendingCrowdId = mPendingCrowdId;
	}

	private long mPendingCrowdId;
	private static boolean isInvoked;

	public CrowdGroupService() {
		grCB = new GroupRequestCB(this);
		GroupRequest.getInstance().addCallback(grCB);
		frCB = new FileRequestCB(this);
		FileRequest.getInstance().addCallback(frCB);
	}

	/**
	 * Create crowd function, it's asynchronization request. response will be
	 * send by caller.
	 * 
	 * @param crowd
	 * @param invationUserList
	 *            be invite user list
	 * @param caller
	 *            if input is null, ignore response Message. Response Message
	 *            object is {@link com.bizcom.request.jni.CreateCrowdResponse}
	 */
	public void createCrowdGroup(CrowdGroup crowd, List<User> invationUserList,
			MessageListener caller) {
		String sXml = XmlAttributeExtractor.buildAttendeeUsersXml(invationUserList);

		this.initTimeoutMessage(CREATE_DISCUSSION_BOARD, DEFAULT_TIME_OUT_SECS,
				caller);
		GroupRequest.getInstance().createGroup(
				Group.GroupType.CHATING.intValue(), crowd.toXml(),
				sXml);

	}
	
	
	/**
	 * 
	 * @param discussion
	 * @param invationUserList
	 * @param caller
	 */
	public void createDiscussionBoard(DiscussionGroup discussion, List<User> invationUserList,
			MessageListener caller) {
		String sXml = XmlAttributeExtractor.buildAttendeeUsersXml(invationUserList);

		this.initTimeoutMessage(CREATE_DISCUSSION_BOARD, DEFAULT_TIME_OUT_SECS,
				caller);
		GroupRequest.getInstance().createGroup(
				Group.GroupType.DISCUSSION.intValue(), discussion.toXml(),
				sXml);

	}
	
	

	/**
	 * Accept invitation
	 * 
	 * @param crowd
	 * @param caller
	 *            if input is null, ignore response Message. Response Message
	 *            object is {@link com.bizcom.request.jni.JNIResponse}
	 */
	public void acceptApplication(CrowdGroup crowd, User applicant,
			MessageListener caller) {
		if (!checkParamNull(caller, new Object[] { crowd, applicant })) {
			return;
		}

		// FIXME concurrency problem, if user use one crowdgroupservice instance
		// to
		// accept mulit-application, then maybe call back will notify incorrect
		initTimeoutMessage(ACCEPT_APPLICATION_CROWD, DEFAULT_TIME_OUT_SECS,
				caller);

		GroupRequest.getInstance().acceptApplyJoinGroup(
				Group.GroupType.CHATING.intValue(), crowd.getmGId(),
				applicant.getmUserId());
	}

	/**
	 * Decline applicant who want to join crowd
	 * 
	 * @param crowd
	 * @param applicant
	 * @param reason
	 * @param caller
	 */
	public void refuseApplication(CrowdGroup crowd, User applicant,
			String reason, MessageListener caller) {
		if (!checkParamNull(caller, new Object[] { crowd, applicant })) {
			return;
		}

		// FIXME concurrency problem, if user use one crowdgroupservice instance
		// to
		// accept mulit-invitation, then maybe call back will notify incorrect
		initTimeoutMessage(REFUSE_APPLICATION_CROWD, DEFAULT_TIME_OUT_SECS,
				caller);

		GroupRequest.getInstance().refuseApplyJoinGroup(
				Group.GroupType.CHATING.intValue(), crowd.getmGId(),
				applicant.getmUserId(), reason);
	}

	/**
	 * Accept invitation
	 * 
	 * @param crowd
	 * @param caller
	 *            if input is null, ignore response Message. Response Message
	 *            object is {@link com.bizcom.request.jni.JNIResponse}
	 */
	public void acceptInvitation(Crowd crowd, MessageListener caller) {
		if (!checkParamNull(caller, new Object[] { crowd })) {
			return;
		}
		
//		if (mPendingCrowdId > 0) {
//			super.sendResult(caller, new JNIResponse(JNIResponse.Result.FAILED));
//			return;
//		}
		mPendingCrowdId = crowd.getId();

		// FIXME concurrency problem, if user use one crowdgroupservice instance
		// to accept mulit-invitation, then maybe call back will notify incorrect
		initTimeoutMessage(ACCEPT_JOIN_CROWD, DEFAULT_TIME_OUT_SECS, caller);

		GroupRequest.getInstance().acceptInviteJoinGroup(
				Group.GroupType.CHATING.intValue(), crowd.getId(),
				crowd.getCreator().getmUserId());
	}

	/**
	 * Decline join crowd invitation
	 * 
	 * @param crowd
	 * @param reason
	 * @param caller
	 */
	public void refuseInvitation(Crowd crowd, String reason,
			MessageListener caller) {
		if (!checkParamNull(caller, new Object[] { crowd })) {
			return;
		}

		if (mPendingCrowdId > 0) {
			super.sendResult(caller, new JNIResponse(JNIResponse.Result.FAILED));
			return;
		}
		mPendingCrowdId = crowd.getId();

		// FIXME concurrency problem, if user use one crowdgroupservice instance
		// to
		// accept mulit-invitation, then maybe call back will notify incorrect

		GroupRequest.getInstance().refuseInviteJoinGroup(
				Group.GroupType.CHATING.intValue(), crowd.getId(),
				crowd.getCreator().getmUserId(), reason == null ? "" : reason);

		mPendingCrowdId = 0;
		sendResult(caller, new JNIResponse(JNIResponse.Result.SUCCESS));
	}

	/**
	 * Apply join crowd
	 * 
	 * @param crowd
	 * @param additional
	 * @param caller
	 */
	public void applyCrowd(Crowd crowd, String additional,
			MessageListener caller) {
		if (!checkParamNull(caller, new Object[] { crowd, additional })) {
			return;
		}

		mPendingCrowdId = crowd.getId();
		GroupRequest.getInstance().applyJoinGroup(
				Group.GroupType.CHATING.intValue(), crowd.getId(),
				additional == null ? "" : additional);
		sendResult(caller, new JNIResponse(JNIResponse.Result.SUCCESS));
	}

	/**
	 * Update crowd data, like brief, announcement or member joined rules
	 * 
	 * @param crowd
	 * @param caller
	 */
	public void updateCrowd(CrowdGroup crowd, MessageListener caller) {
		if (!checkParamNull(caller, crowd)) {
			return;
		}
		if (mPendingCrowdId > 0) {
			super.sendResult(caller, new JNIResponse(JNIResponse.Result.FAILED));
			return;
		}
		mPendingCrowdId = crowd.getmGId();
		initTimeoutMessage(UPDATE_CROWD, DEFAULT_TIME_OUT_SECS, caller);
		GroupRequest.getInstance()
				.modifyGroupInfo(crowd.getGroupType().intValue(),
						crowd.getmGId(), crowd.toXml());
	}
	
	
	/**
	 * Update crowd data, like brief, announcement or member joined rules
	 * 
	 * @param crowd
	 * @param caller
	 */
	public void updateDiscussion(DiscussionGroup discussion, MessageListener caller) {
		if (!checkParamNull(caller, discussion)) {
			return;
		}
		if (mPendingCrowdId > 0) {
			super.sendResult(caller, new JNIResponse(JNIResponse.Result.FAILED));
			return;
		}
		mPendingCrowdId = discussion.getmGId();
		initTimeoutMessage(UPDATE_DISCUSSION_BOARD, DEFAULT_TIME_OUT_SECS, caller);
		GroupRequest.getInstance()
				.modifyGroupInfo(discussion.getGroupType().intValue(),
						discussion.getmGId(), discussion.toXml());
	}

	/**
	 * Quit crowd. <br>
	 * If current user is administrator, then will dismiss crowd.<br>
	 * If current user is member, just quit this crowd.
	 * 
	 * @param crowd
	 * @param caller
	 */
	public void quitCrowd(CrowdGroup crowd, MessageListener caller) {
		if (!checkParamNull(caller, crowd)) {
			return;
		}

		if (mPendingCrowdId > 0) {
			super.sendResult(caller, new JNIResponse(JNIResponse.Result.FAILED));
			return;
		}
		mPendingCrowdId = crowd.getmGId();
		initTimeoutMessage(QUIT_CROWD, DEFAULT_TIME_OUT_SECS, caller);
		if (crowd.getOwnerUser().getmUserId() == GlobalHolder.getInstance()
				.getCurrentUserId()) {
			GroupRequest.getInstance().delGroup(
					crowd.getGroupType().intValue(), crowd.getmGId());
		} else {
			GroupRequest.getInstance().leaveGroup(
					crowd.getGroupType().intValue(), crowd.getmGId());
		}
	}
	
	
	
	/**
	 * Quit from discussion board. <br>
	 * 
	 * @param crowd
	 * @param caller
	 */
	public void quitDiscussionBoard(DiscussionGroup discussion, MessageListener caller) {
		if (!checkParamNull(caller, discussion)) {
			return;
		}

		if (mPendingCrowdId > 0) {
			super.sendResult(caller, new JNIResponse(JNIResponse.Result.FAILED));
			return;
		}
		mPendingCrowdId = discussion.getmGId();
		initTimeoutMessage(QUIT_DISCUSSION_BOARD, DEFAULT_TIME_OUT_SECS, caller);
		GroupRequest.getInstance().leaveGroup(
				discussion.getGroupType().intValue(), discussion.getmGId());
	}
	
	

	/**
	 * Invite new member to join crowd or discussion board.<br>
	 * Notice: call this API after group is created.
	 * 
	 * @param crowd
	 * @param newMembers
	 * @param caller
	 */
	public void inviteMember(Group crowd, List<User> newMembers,
			MessageListener caller) {
		if (!checkParamNull(caller, new Object[] { crowd, newMembers })) {
			return;
		}
		if (newMembers.size() <= 0) {
			if (caller != null) {
				JNIResponse jniRes = new JNIResponse(JNIResponse.Result.SUCCESS);
				sendResult(caller, jniRes);
			}
			return;
		}

        if(crowd == null) {
            V2Log.e("CrowdGroupService inviteMember --> INVITE MEMBER FAILED ... Because crowd Object is null!");
            return;
        }
        
        String sXml = XmlAttributeExtractor.buildAttendeeUsersXml(newMembers);
		GroupRequest.getInstance().inviteJoinGroup(
				crowd.getGroupType().intValue(), crowd.toXml(),
				sXml , "");
		if (caller != null) {
			JNIResponse jniRes = new JNIResponse(JNIResponse.Result.SUCCESS);
			sendResult(caller, jniRes);
		}
	}

	/**
	 * Remove member from crowd
	 * 
	 * @param crowd
	 * @param member
	 * @param caller
	 */
	public void removeMember(Group crowd, User member,
			MessageListener caller) {
		if (!checkParamNull(caller, new Object[] { crowd, member })) {
			return;
		}
		GroupRequest.getInstance().delGroupUser(
				crowd.getGroupType().intValue(), crowd.getmGId(),
				member.getmUserId());
	}

	@Override
	public void clearCalledBack() {
		GroupRequest.getInstance().removeCallback(grCB);
		FileRequest.getInstance().removeCallback(frCB);
	}

	/**
	 * fetch files from server
	 * 
	 * @param crowd
	 * @param caller
	 *            return List<VFile>
	 */
	public void fetchGroupFiles(CrowdGroup crowd, MessageListener caller) {
		if (!checkParamNull(caller, new Object[] { crowd })) {
			return;
		}

		if (mPendingCrowdId > 0) {
			super.sendResult(caller, new JNIResponse(JNIResponse.Result.FAILED));
			return;
		}
		mPendingCrowdId = crowd.getmGId();

		this.initTimeoutMessage(FETCH_FILES_CROWD, DEFAULT_TIME_OUT_SECS,
				caller);
		GroupRequest.getInstance().getGroupFileInfo(
				GroupType.CHATING.intValue(), crowd.getmGId());

	}

	/**
	 * Remove files from crowd.
	 * 
	 * @param crowd
	 * @param files
	 * @param caller
	 */
	public void removeGroupFiles(CrowdGroup crowd, List<VCrowdFile> files,
			MessageListener caller) {
		if (!checkParamNull(caller, new Object[] { crowd, files })) {
			return;
		}
		if (files.size() <= 0) {
			JNIResponse jniRes = new JNIResponse(JNIResponse.Result.SUCCESS);
			super.sendResult(caller, jniRes);
			return;
		}

		if (mPendingCrowdId > 0) {
			super.sendResult(caller, new JNIResponse(JNIResponse.Result.FAILED));
			return;
		}
		mPendingCrowdId = crowd.getmGId();

		this.initTimeoutMessage(REMOVE_FILES_CROWD, DEFAULT_TIME_OUT_SECS,
				caller);
		for (VCrowdFile f : files) {
			GroupRequest.getInstance()
					.delGroupFile(crowd.getGroupType().intValue(),
							crowd.getmGId(), f.getId());
		}

	}

	/**
	 * 
	 * @param vf
	 * @param opt
	 *            {@link FileOperationEnum}
	 * @param caller
	 */
	public void handleCrowdFile(VCrowdFile vf, FileOperationEnum opt,
			MessageListener caller) {
		if (!checkParamNull(caller, new Object[] { vf })) {
			return;
		}

		switch (opt) {
		case OPERATION_START_DOWNLOAD:
			FileRequest.getInstance().httpDownloadFile(vf.getUrl(), vf.getId(),
					vf.getPath(), 0);
			break;
		case OPERATION_CANCEL_DOWNLOADING:
			FileRequest.getInstance().cancelRecvFile(vf.getId());
			break;
		case OPERATION_CANCEL_SENDING:
			FileRequest.getInstance().cancelSendFile(vf.getId());
			break;
		case OPERATION_PAUSE_DOWNLOADING:
			FileRequest.getInstance().pauseHttpRecvFile(vf.getId());
			break;
		case OPERATION_PAUSE_SENDING:
			FileRequest.getInstance().pauseSendFile(vf.getId());
			break;
		case OPERATION_RESUME_DOWNLOAD:
			FileRequest.getInstance().resumeHttpRecvFile(vf.getId());
			break;
		case OPERATION_RESUME_SEND:
			FileRequest.getInstance().resumeSendFile(vf.getId());
			break;
		case OPERATION_START_SEND: {
			GroupRequest.getInstance().groupUploadFile(
					vf.getCrowd().getGroupType().intValue(),
					vf.getCrowd().getmGId(), vf.toXml());
		}
			break;
		default:
			break;
		}
	}

	/**
	 * Register listener for file transport status
	 * 
	 * @param h
	 * @param what
	 * @param obj
	 */
	public void registerFileTransStatusListener(Handler h, int what, Object obj) {
		registerListener(KEY_FILE_TRANS_STATUS_NOTIFICATION_LISTNER, h, what,
				obj);
	}

	public void unRegisterFileTransStatusListener(Handler h, int what,
			Object obj) {
		unRegisterListener(KEY_FILE_TRANS_STATUS_NOTIFICATION_LISTNER, h, what,
				obj);
	}

	/**
	 * Register listener for group file is removed.<br>
	 * Notice: If current user send remove file command by
	 * {@link #removeGroupFiles(CrowdGroup, List, MessageListener)}, will not
	 * notification
	 * 
	 * @param h
	 * @param what
	 * @param obj
	 */
	public void registerFileRemovedNotification(Handler h, int what, Object obj) {
		registerListener(KEY_FILE_REMOVED_NOTIFICATION_LISTNER, h, what, obj);
	}

	public void unRegisterFileRemovedNotification(Handler h, int what,
			Object obj) {
		unRegisterListener(KEY_FILE_REMOVED_NOTIFICATION_LISTNER, h, what, obj);
	}

	/**
	 * Register listener for group new file notification.<br>
	 * 
	 * @param h
	 * @param what
	 * @param obj
	 */
	public void registerNewFileNotification(Handler h, int what, Object obj) {
		registerListener(KEY_FILE_NEW_NOTIFICATION_LISTNER, h, what, obj);
	}

	public void unRegisterNewFileNotification(Handler h, int what, Object obj) {
		unRegisterListener(KEY_FILE_NEW_NOTIFICATION_LISTNER, h, what, obj);
	}

	class GroupRequestCB extends GroupRequestCallbackAdapter {
		private Handler mCallbackHandler;

		public GroupRequestCB(Handler mCallbackHandler) {
			this.mCallbackHandler = mCallbackHandler;
		}

		@Override
		public void OnModifyGroupInfoCallback(V2Group group) {
			if (group == null) {
				return;
			}
			if (group.type == GroupType.CHATING.intValue()
					&& group.id == mPendingCrowdId) {
				mPendingCrowdId = 0;
				JNIResponse jniRes = new JNIResponse(JNIResponse.Result.SUCCESS);
				Message.obtain(mCallbackHandler, UPDATE_CROWD, jniRes)
						.sendToTarget();
			} else if (group.type == GroupType.DISCUSSION.intValue() && group.id == mPendingCrowdId) {
				mPendingCrowdId = 0;
				JNIResponse jniRes = new JNIResponse(JNIResponse.Result.SUCCESS);
				Message.obtain(mCallbackHandler, UPDATE_DISCUSSION_BOARD, jniRes)
						.sendToTarget();
				
			}

		}

		@Override
		public void OnDelGroupCallback(int groupType, long nGroupID,
				boolean bMovetoRoot) {
			if (groupType == GroupType.CHATING.intValue()
					&& nGroupID == mPendingCrowdId) {
				mPendingCrowdId = 0;
				JNIResponse jniRes = new JNIResponse(JNIResponse.Result.SUCCESS);
				Message.obtain(mCallbackHandler, QUIT_CROWD, jniRes)
						.sendToTarget();
			} else if (groupType == GroupType.DISCUSSION.intValue() && nGroupID == mPendingCrowdId) {
				mPendingCrowdId = 0;
				JNIResponse jniRes = new JNIResponse(JNIResponse.Result.SUCCESS);
				Message.obtain(mCallbackHandler, QUIT_DISCUSSION_BOARD, jniRes)
						.sendToTarget();
			}
		}

		/**
		 * Used to as callback of accept join crowd group <br />
		 * ===OnAcceptInviteJoinGroup never called===
		 * 
		 * @see com.V2.jni.callbacAdapter.GroupRequestCallbackAdapter#onAddGroupInfo(V2Group)
		 */
		public void onAddGroupInfo(V2Group group) {
			if (group.type == V2Group.TYPE_CROWD) {
				if (GlobalHolder.getInstance().getCurrentUserId() == group.owner.uid) {
					// Create a new crowd group by current logined user
					V2Log.e("CrowdGroupService onAddGroupInfo--> successful create a new group , id is : "
							+ group.id);
					mPendingCrowdId = 0;
					JNIResponse jniRes = new CreateCrowdResponse(group.id,
							CreateCrowdResponse.Result.SUCCESS);
					Message.obtain(mCallbackHandler, CREATE_DISCUSSION_BOARD,
							jniRes).sendToTarget();
				} else {
					if (mPendingCrowdId == group.id) {
						V2Log.e("CrowdGroupService onAddGroupInfo--> add a new group , id is : "
								+ group.id);
						mPendingCrowdId = 0;
						JNIResponse jniRes = new JNIResponse(
								CreateCrowdResponse.Result.SUCCESS);
						Message.obtain(mCallbackHandler, ACCEPT_JOIN_CROWD,
								jniRes).sendToTarget();
					} else {
						V2Log.e("CrowdGroupService onAddGroupInfo--> mPendingCrowdId isn't equals groupID , MayBe this callback"
								+ "already time out , group id is : "
								+ group.id + " group name is : " + group.getName());
					}
				}
			} else if (group.type == V2Group.TYPE_DISCUSSION_BOARD) {
				
				if (GlobalHolder.getInstance().getCurrentUserId() == group.owner.uid) {
					// Create a new discussion board group by current logged user
					JNIResponse jniRes = new CreateDiscussionBoardResponse(group.id,
							JNIResponse.Result.SUCCESS);
					Message.obtain(mCallbackHandler, CREATE_DISCUSSION_BOARD,
							jniRes).sendToTarget();
				}
			}
		}

		
		@Override
		public void OnAddGroupUserInfoCallback(int groupType, long nGroupID,
				V2User user) {
			//JNIService中的 OnAddGroupUserInfoCallback 来处理数据库更新
			if (groupType == V2Group.TYPE_CROWD
					&& user.uid != GlobalHolder.getInstance()
							.getCurrentUserId() && !isInvoked) {
				JNIResponse jniRes = new JNIResponse(
						CreateCrowdResponse.Result.SUCCESS);
				jniRes.resObj = new GroupAddUserJNIObject(groupType, nGroupID,
						user.uid, "");
				Message.obtain(mCallbackHandler, ACCEPT_APPLICATION_CROWD,
						jniRes).sendToTarget();
			}
		}
	

		/*
		 * Used to as callback of leave crowd group
		 * 
		 * @see
		 * com.V2.jni.GroupRequestCallbackAdapter#OnDelGroupUserCallback(int,
		 * long, long)
		 */
		public void OnDelGroupUserCallback(int groupType, long nGroupID,
				long nUserID) {
			if (groupType == GroupType.CHATING.intValue()
					&& nGroupID == mPendingCrowdId) {
				mPendingCrowdId = 0;
				JNIResponse jniRes = new JNIResponse(JNIResponse.Result.SUCCESS);
				Message.obtain(mCallbackHandler, QUIT_CROWD, jniRes)
						.sendToTarget();
			}else if (groupType == GroupType.DISCUSSION.intValue()
					&& nGroupID == mPendingCrowdId) {
				mPendingCrowdId = 0;
				JNIResponse jniRes = new JNIResponse(JNIResponse.Result.SUCCESS);
				Message.obtain(mCallbackHandler, QUIT_DISCUSSION_BOARD, jniRes)
						.sendToTarget();
			}
		}

		@Override
		public void OnGetGroupFileInfo(V2Group group, List<FileJNIObject> list) {
			if (group.type == GroupType.CHATING.intValue()
					&& group.id == mPendingCrowdId) {
				mPendingCrowdId = 0;

				RequestFetchGroupFilesResponse jniRes = new RequestFetchGroupFilesResponse(
						JNIResponse.Result.SUCCESS);
				jniRes.setList(convertList(group, list));
				Message.obtain(mCallbackHandler, FETCH_FILES_CROWD, jniRes)
						.sendToTarget();
			}

		}

		@Override
		public void OnDelGroupFile(V2Group group, List<FileJNIObject> list) {
			if (group.type == GroupType.CHATING.intValue()) {
				// Use fetch group file object as result
				RequestFetchGroupFilesResponse jniRes = new RequestFetchGroupFilesResponse(
						JNIResponse.Result.SUCCESS);
				jniRes.setList(convertList(group, list));
				// If user requested, send message
				if (group.id == mPendingCrowdId) {
					mPendingCrowdId = 0;

					Message.obtain(mCallbackHandler, REMOVE_FILES_CROWD, jniRes)
							.sendToTarget();
					// If this request is not user requested, send notification
				} else if (mPendingCrowdId == 0) {
					notifyListener(KEY_FILE_REMOVED_NOTIFICATION_LISTNER, 0, 0,
							jniRes);
				}
			}
		}
		
		@Override
		public void OnJoinGroupError(int eGroupType, long nGroupID, int nErrorNo) {
			if (mPendingCrowdId == nGroupID) {
				mPendingCrowdId = 0;
				JNIResponse jniRes = new JNIResponse(
						CreateCrowdResponse.Result.SERVER_REJECT);
				Message.obtain(mCallbackHandler, ACCEPT_JOIN_CROWD,
						jniRes).sendToTarget();
			}
		}

		private List<VCrowdFile> convertList(V2Group group,
				List<FileJNIObject> list) {
			List<VCrowdFile> vfList = null;
			vfList = new ArrayList<VCrowdFile>();
			if (list == null) {
				return vfList;
			}
			for (FileJNIObject f : list) {
				VCrowdFile vcf = new VCrowdFile();
				vcf.setCrowd((CrowdGroup) GlobalHolder.getInstance()
						.getGroupById(GroupType.CHATING.intValue(), group.id));

				vcf.setId(f.fileId);
				vcf.setName(f.fileName);
				vcf.setSize(f.fileSize);
				vcf.setUrl(f.url);
				// If event is removed file, then user is null
				if (f.user != null) {
					vcf.setUploader(GlobalHolder.getInstance().getUser(
							f.user.uid));
				}
				vcf.setPath(GlobalConfig.getGlobalFilePath() + "/" + f.fileName);
				vfList.add(vcf);
			}
			return vfList;
		}

		@Override
		public void OnAddGroupFile(V2Group group, List<FileJNIObject> list) {
            if(group == null){
                V2Log.e("CrowdGroupService OnAddGroupFile--> add a new group file failed , V2Group is null");
                return ;
            }

            if(list == null){
                V2Log.e("CrowdGroupService OnAddGroupFile--> add a new group file failed , FileJNIObject List is null");
                return ;
            }
            
            if(group.type == V2GlobalConstants.GROUP_TYPE_CONFERENCE){
            	V2Log.e("CrowdGroupService OnAddGroupFile--> add a new group file failed , Group Type is Conference!");
            	return ;
            }

			// Use fetch group file object as result
			RequestFetchGroupFilesResponse jniRes = new RequestFetchGroupFilesResponse(
					JNIResponse.Result.SUCCESS);
			jniRes.setList(convertList(group, list));
            jniRes.setGroupID(group.id);
			notifyListener(KEY_FILE_NEW_NOTIFICATION_LISTNER, 0, 0, jniRes);
		}

	}

	class FileRequestCB extends FileRequestCallbackAdapter {

		public FileRequestCB(Handler mCallbackHandler) {
		}

		@Override
		public void OnFileTransProgress(String szFileID, long nBytesTransed,
				int nTransType) {
			notifyListener(
					KEY_FILE_TRANS_STATUS_NOTIFICATION_LISTNER,
					0,
					0,
					new FileTransProgressStatusIndication(
							nTransType,
							szFileID,
							nBytesTransed,
							FileTransStatusIndication.IND_TYPE_PROGRESS_TRANSING));
		}

		@Override
		public void OnFileTransEnd(String szFileID, String szFileName,
				long nFileSize, int nTransType) {
			notifyListener(KEY_FILE_TRANS_STATUS_NOTIFICATION_LISTNER, 0, 0,
					new FileTransProgressStatusIndication(nTransType, szFileID,
							nFileSize,
							FileTransStatusIndication.IND_TYPE_PROGRESS_END));

		}

		@Override
		public void OnFileDeleted(FileJNIObject file) {
			if (file instanceof GroupFileJNIObject) {
				GroupFileJNIObject gfile = (GroupFileJNIObject) file;

				RequestFetchGroupFilesResponse jniRes = new RequestFetchGroupFilesResponse(
						JNIResponse.Result.SUCCESS);
				List<VCrowdFile> list = new ArrayList<VCrowdFile>(1);

				VCrowdFile vcf = new VCrowdFile();
				vcf.setCrowd((CrowdGroup) GlobalHolder.getInstance()
						.getGroupById(GroupType.CHATING.intValue(),
								gfile.group.id));
				vcf.setId(gfile.fileId);
				list.add(vcf);

				jniRes.setList(list);

				notifyListener(KEY_FILE_REMOVED_NOTIFICATION_LISTNER, 0, 0,
						jniRes);
			}
		}

	}

}
