package com.v2tech.service;

import java.util.ArrayList;
import java.util.List;

import android.os.Handler;
import android.os.Message;

import com.V2.jni.FileRequest;
import com.V2.jni.FileRequestCallbackAdapter;
import com.V2.jni.GroupRequest;
import com.V2.jni.GroupRequestCallbackAdapter;
import com.V2.jni.ind.FileJNIObject;
import com.V2.jni.ind.GroupFileJNIObject;
import com.V2.jni.ind.V2Group;
import com.V2.jni.ind.V2User;
import com.V2.jni.util.V2Log;
import com.v2tech.service.jni.CreateCrowdResponse;
import com.v2tech.service.jni.FileTransStatusIndication;
import com.v2tech.service.jni.FileTransStatusIndication.FileTransErrorIndication;
import com.v2tech.service.jni.FileTransStatusIndication.FileTransProgressStatusIndication;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.service.jni.RequestFetchGroupFilesResponse;
import com.v2tech.util.GlobalConfig;
import com.v2tech.vo.Crowd;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.Group;
import com.v2tech.vo.VMessageQualification;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.User;
import com.v2tech.vo.VCrowdFile;

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
public class CrowdGroupService extends AbstractHandler {

	private static final int CREATE_GROUP_MESSAGE = 0x0001;
	private static final int ACCEPT_JOIN_CROWD = 0x0002;
	private static final int UPDATE_CROWD = 0x0004;
	private static final int QUIT_CROWD = 0x0005;
	private static final int ACCEPT_APPLICATION_CROWD = 0x0006;
	private static final int REFUSE_APPLICATION_CROWD = 0x0007;
	private static final int FETCH_FILES_CROWD = 0x0008;
	private static final int REMOVE_FILES_CROWD = 0x0009;

	private static final int KEY_CANCELLED_LISTNER = 1;
	private static final int KEY_FILE_TRANS_STATUS_NOTIFICATION_LISTNER = 2;
	private static final int KEY_FILE_REMOVED_NOTIFICATION_LISTNER = 3;
	private static final int KEY_FILE_NEW_NOTIFICATION_LISTNER = 4;

	private GroupRequestCB grCB;
	private FileRequestCB frCB;
	private long mPendingCrowdId;

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
	 *            object is {@link com.v2tech.service.jni.CreateCrowdResponse}
	 */
	public void createCrowdGroup(CrowdGroup crowd, List<User> invationUserList,
			Registrant caller) {
		this.initTimeoutMessage(CREATE_GROUP_MESSAGE, DEFAULT_TIME_OUT_SECS,
				caller);
		StringBuffer sb = new StringBuffer();
		sb.append("<userlist>");
		for (User u : invationUserList) {
			sb.append(" <user id=\"" + u.getmUserId() + "\" />");
		}
		sb.append("</userlist>");

		GroupRequest.getInstance().createGroup(
				Group.GroupType.CHATING.intValue(), crowd.toXml(),
				sb.toString());

	}

	/**
	 * Accept invitation
	 * 
	 * @param crowd
	 * @param caller
	 *            if input is null, ignore response Message. Response Message
	 *            object is {@link com.v2tech.service.jni.JNIResponse}
	 */
	public void acceptApplication(CrowdGroup crowd, User applicant,
			Registrant caller) {
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
				GlobalHolder.getInstance().getCurrentUserId());
	}

	/**
	 * Decline applicant who want to join crowd
	 * 
	 * @param group
	 */
	public void refuseApplication(CrowdGroup crowd, User applicant,
			String reason, Registrant caller) {
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
	 *            object is {@link com.v2tech.service.jni.JNIResponse}
	 */
	public void acceptInvitation(Crowd crowd, Registrant caller) {
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
		initTimeoutMessage(ACCEPT_JOIN_CROWD, DEFAULT_TIME_OUT_SECS, caller);

		GroupRequest.getInstance().acceptInviteJoinGroup(
				Group.GroupType.CHATING.intValue(), crowd.getId(),
				GlobalHolder.getInstance().getCurrentUserId());
	}

	/**
	 * Decline join crowd invitation
	 * 
	 * @param reason
	 * @param group
	 */
	public void refuseInvitation(Crowd crowd, String reason, Registrant caller) {
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
				GlobalHolder.getInstance().getCurrentUserId(),
				reason == null ? "" : reason);

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
	public void applyCrowd(Crowd crowd, String additional, Registrant caller) {
		if (!checkParamNull(caller, new Object[] { crowd, additional })) {
			return;
		}

		if (mPendingCrowdId > 0) {
			super.sendResult(caller, new JNIResponse(JNIResponse.Result.FAILED));
			return;
		}
		mPendingCrowdId = crowd.getId();
		GroupRequest.getInstance().applyJoinGroup(
				Group.GroupType.CHATING.intValue(), crowd.toXml(),
				additional == null ? "" : additional);
		mPendingCrowdId = 0;
		sendResult(caller, new JNIResponse(JNIResponse.Result.SUCCESS));
	}

	/**
	 * Update crowd data, like brief, announcement or member joined rules
	 * 
	 * @param crowd
	 * @param caller
	 */
	public void updateCrowd(CrowdGroup crowd, Registrant caller) {
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
	 * Quit crowd. <br>
	 * If current user is administrator, then will dismiss crowd.<br>
	 * If current user is member, just quit this crowd.
	 * 
	 * @param crowd
	 * @param caller
	 */
	public void quitCrowd(CrowdGroup crowd, Registrant caller) {
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
	 * Invite new member to join crowd.<br>
	 * Notice: call this API after crowd is created.
	 * 
	 * @param crowd
	 * @param newMembers
	 * @param caller
	 */
	public void inviteMember(CrowdGroup crowd, List<User> newMembers,
			Registrant caller) {
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

		StringBuffer members = new StringBuffer();
		members.append("<userlist> ");
		for (User at : newMembers) {
			members.append(" <user id='" + at.getmUserId() + "' />");
		}
		members.append("</userlist>");

		GroupRequest.getInstance().inviteJoinGroup(
				crowd.getGroupType().intValue(), crowd.toXml(),
				members.toString(), "");

		if (caller != null) {
			JNIResponse jniRes = new JNIResponse(JNIResponse.Result.SUCCESS);
			sendResult(caller, jniRes);
		}
	}

	/**
	 * Remove member from crowd
	 * 
	 * @param crowd
	 * @param user
	 * @param caller
	 */
	public void removeMember(CrowdGroup crowd, User member, Registrant caller) {
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
	 * FIXME add comment
	 * 
	 * @comment-user:wenzl 2014年9月15日
	 * @overview:
	 * 
	 * @param group
	 * @param caller
	 * @return:
	 */
	public void createGroup(CrowdGroup group, Registrant caller) {
		// 对CREATE_GROUP_MESSAGE做超时处理，如果超时此消息不再通知上层。
		this.initTimeoutMessage(CREATE_GROUP_MESSAGE, DEFAULT_TIME_OUT_SECS,
				caller);
		GroupRequest.getInstance().createGroup(
				Group.GroupType.CHATING.intValue(), group.toXml(),
				group.toGroupUserListXml());
	}

	/**
	 * fetch files from server
	 * 
	 * @param crowd
	 * @param caller
	 *            return List<VFile>
	 */
	public void fetchGroupFiles(CrowdGroup crowd, Registrant caller) {
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
			Registrant caller) {
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
			GroupRequest.getInstance().delGroupFile(
					crowd.getGroupType().intValue(), crowd.getmGId(),
					f.getId());
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
			Registrant caller) {
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
	 * {@link #removeGroupFiles(CrowdGroup, List, Registrant)}, will not
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

		/**
		 * @deprecated
		 */
		@Override
		public void OnAcceptInviteJoinGroup(int groupType, long groupId,
				long nUserID) {
			JNIResponse jniRes = new JNIResponse(
					CreateCrowdResponse.Result.SUCCESS);
			Message.obtain(mCallbackHandler, ACCEPT_JOIN_CROWD, jniRes)
					.sendToTarget();
		}

		@Override
		public void OnAcceptApplyJoinGroup(V2Group group) {
			JNIResponse jniRes = new JNIResponse(
					CreateCrowdResponse.Result.SUCCESS);
			Message.obtain(mCallbackHandler, ACCEPT_APPLICATION_CROWD, jniRes)
					.sendToTarget();

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
			}
		}

		/*
		 * Used to as callback of accept join crowd group
		 * 
		 * @see
		 * com.V2.jni.GroupRequestCallbackAdapter#onAddGroupInfo(com.V2.jni.
		 * ind.V2Group)
		 */
		public void onAddGroupInfo(V2Group group) {
			if (group.type == V2Group.TYPE_CROWD) {
				if (mPendingCrowdId == group.id) {
					mPendingCrowdId = 0;
					JNIResponse jniRes = new JNIResponse(
							CreateCrowdResponse.Result.SUCCESS);
					Message.obtain(mCallbackHandler, ACCEPT_JOIN_CROWD, jniRes)
							.sendToTarget();
				} else {
					JNIResponse jniRes = new CreateCrowdResponse(group.id,
							CreateCrowdResponse.Result.SUCCESS);
					Message.obtain(mCallbackHandler, CREATE_GROUP_MESSAGE,
							jniRes).sendToTarget();
				}
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

		private List<VCrowdFile> convertList(V2Group group,
				List<FileJNIObject> list) {
			List<VCrowdFile> vfList = null;
			if (list == null) {
				vfList = new ArrayList<VCrowdFile>(0);
				return vfList;
			} else {
				vfList = new ArrayList<VCrowdFile>(list.size());
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
				vcf.setPath(GlobalConfig.getGlobalPath() + "/files/" + group.id
						+ "/" + f.fileName);
				vfList.add(vcf);
			}
			return vfList;
		}

		@Override
		public void OnAddGroupFile(V2Group group, List<FileJNIObject> list) {
			// Use fetch group file object as result
			RequestFetchGroupFilesResponse jniRes = new RequestFetchGroupFilesResponse(
					JNIResponse.Result.SUCCESS);
			jniRes.setList(convertList(group, list));
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
			notifyListener(
					KEY_FILE_TRANS_STATUS_NOTIFICATION_LISTNER,
					0,
					0,
					new FileTransProgressStatusIndication(
							nTransType,
							szFileID,
							nFileSize,
							FileTransStatusIndication.IND_TYPE_PROGRESS_TRANSING));

		}

		@Override
		public void OnFileTransError(String szFileID, int errorCode,
				int nTransType) {

			notifyListener(KEY_FILE_TRANS_STATUS_NOTIFICATION_LISTNER, 0, 0,
					new FileTransErrorIndication(szFileID, errorCode,
							nTransType));
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
