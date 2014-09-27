package com.v2tech.service;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.V2.jni.FileRequest;
import com.V2.jni.FileRequestCallbackAdapter;
import com.V2.jni.GroupRequest;
import com.V2.jni.GroupRequestCallbackAdapter;
import com.V2.jni.ImRequest;
import com.V2.jni.ImRequestCallbackAdapter;
import com.V2.jni.ind.FileJNIObject;
import com.V2.jni.ind.V2Group;
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
	private static final int REFUSE_JOIN_CROWD = 0x0003;
	private static final int UPDATE_CROWD = 0x0004;
	private static final int QUIT_CROWD = 0x0005;
	private static final int ACCEPT_APPLICATION_CROWD = 0x0006;
	private static final int REFUSE_APPLICATION_CROWD = 0x0007;
	private static final int FETCH_FILES_CROWD = 0x0008;

	private static final int KEY_CANCELLED_LISTNER = 1;
	private static final int KEY_FILE_TRANS_STATUS_NOTIFICATION_LISTNER = 2;

	private ImRequestCB imCB;
	private GroupRequestCB grCB;
	private FileRequestCB frCB;
	private long mPendingCrowdId;

	public CrowdGroupService() {
		imCB = new ImRequestCB(this);
		ImRequest.getInstance().addCallback(imCB);
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
	 * @param caller
	 *            if input is null, ignore response Message. Response Message
	 *            object is {@link com.v2tech.service.jni.CreateCrowdResponse}
	 */
	public void createCrowdGroup(CrowdGroup crowd, Registrant caller) {
		this.initTimeoutMessage(CREATE_GROUP_MESSAGE, DEFAULT_TIME_OUT_SECS,
				caller);
		GroupRequest.getInstance().createGroup(
				Group.GroupType.CHATING.intValue(), crowd.toXml(),
				crowd.toGroupUserListXml());

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
				Group.GroupType.CHATING.intValue(), crowd.toXml(),
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
				Group.GroupType.CHATING.intValue(), crowd.toXml(),
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
	 * @param group
	 */
	public void refuseInvitation(Crowd crowd, Registrant caller) {
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
		initTimeoutMessage(REFUSE_JOIN_CROWD, DEFAULT_TIME_OUT_SECS, caller);

		GroupRequest.getInstance().refuseInviteJoinGroup(
				Group.GroupType.CHATING.intValue(), crowd.getId(),
				GlobalHolder.getInstance().getCurrentUserId(), "");
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
		ImRequest.getInstance().removeCallback(imCB);
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
					vf.getPath(), 0, 1);
			break;
		case OPERATION_CANCEL_DOWNLOADING:
			FileRequest.getInstance().cancelRecvFile(vf.getId(), 1);
			break;
		case OPERATION_CANCEL_SENDING:
			FileRequest.getInstance().cancelSendFile(vf.getId(), 1);
			break;
		case OPERATION_PAUSE_DOWNLOADING:
			FileRequest.getInstance().pauseHttpRecvFile(vf.getId(), 1);
			break;
		case OPERATION_PAUSE_SENDING:
			FileRequest.getInstance().pauseSendFile(vf.getId(), 1);
			break;
		case OPERATION_RESUME_DOWNLOAD:
			FileRequest.getInstance().resumeHttpRecvFile(vf.getId(), 1);
			break;
		case OPERATION_RESUME_SEND:
			FileRequest.getInstance().resumeSendFile(vf.getId(), 1);
			break;
		case OPERATION_START_SEND:
			break;
		default:
			break;
		}
	}

	/**
	 * Register listener for file transport status
	 * 
	 * @param msg
	 */
	public void registerFileTransStatusListener(Handler h, int what, Object obj) {
		registerListener(KEY_FILE_TRANS_STATUS_NOTIFICATION_LISTNER, h, what,
				obj);
	}

	public void removeRegisterFileTransStatusListener(Handler h, int what,
			Object obj) {
		unRegisterListener(KEY_FILE_TRANS_STATUS_NOTIFICATION_LISTNER, h, what,
				obj);

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
		public void OnModifyGroupInfoCallback(int groupType, long nGroupID,
				String sXml) {
			if (groupType == GroupType.CHATING.intValue()
					&& nGroupID == mPendingCrowdId) {
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
			if (group.type == V2Group.TYPE_CROWD && mPendingCrowdId == group.id) {
				mPendingCrowdId = 0;
				JNIResponse jniRes = new JNIResponse(
						CreateCrowdResponse.Result.SUCCESS);
				Message.obtain(mCallbackHandler, ACCEPT_JOIN_CROWD, jniRes)
						.sendToTarget();
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
				List<VCrowdFile> vfList = null;
				if (list == null) {
					vfList = new ArrayList<VCrowdFile>(0);
				} else {
					vfList = new ArrayList<VCrowdFile>(list.size());
				}
				for (FileJNIObject f : list) {
					VCrowdFile vcf = new VCrowdFile();
					vcf.setCrowd((CrowdGroup) GlobalHolder.getInstance()
							.getGroupById(GroupType.CHATING.intValue(),
									group.id));

					vcf.setId(f.fileId);
					vcf.setName(f.fileName);
					vcf.setSize(f.fileSize);
					vcf.setUrl(f.url);
					vcf.setUploader(GlobalHolder.getInstance().getUser(
							f.user.uid));
					vcf.setPath(GlobalConfig.getGlobalPath() + "/files/"
							+ group.id + "/" + f.fileName);
					vfList.add(vcf);
				}
				RequestFetchGroupFilesResponse jniRes = new RequestFetchGroupFilesResponse(
						JNIResponse.Result.SUCCESS);
				jniRes.setList(vfList);
				Message.obtain(mCallbackHandler, FETCH_FILES_CROWD, jniRes)
						.sendToTarget();
			}

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
				long nFileSize, int nTransType, Context context) {
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

	}

	class ImRequestCB extends ImRequestCallbackAdapter {

		private Handler mCallbackHandler;

		public ImRequestCB(Handler mCallbackHandler) {

			this.mCallbackHandler = mCallbackHandler;
		}

		@Override
		public void OnCreateCrowdCallback(V2Group crowd, int nResult) {
			if (crowd == null) {
				return;
			}
			JNIResponse jniRes = new CreateCrowdResponse(crowd.id,
					CreateCrowdResponse.Result.SUCCESS);
			Message.obtain(mCallbackHandler, CREATE_GROUP_MESSAGE, jniRes)
					.sendToTarget();
		}

	}
}
