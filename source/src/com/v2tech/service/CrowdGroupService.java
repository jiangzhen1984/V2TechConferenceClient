package com.v2tech.service;

import java.util.List;

import android.os.Handler;
import android.os.Message;

import com.V2.jni.GroupRequest;
import com.V2.jni.GroupRequestCallbackAdapter;
import com.V2.jni.ImRequest;
import com.V2.jni.ImRequestCallbackAdapter;
import com.V2.jni.ind.V2Group;
import com.V2.jni.util.V2Log;
import com.v2tech.service.jni.CreateCrowdResponse;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.vo.Crowd;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.User;

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

	private ImRequestCB imCB;
	private GroupRequestCB grCB;
	private long mPendingCrowdId;

	public CrowdGroupService() {
		imCB = new ImRequestCB(this);
		ImRequest.getInstance().addCallback(imCB);
		grCB = new GroupRequestCB(this);
		GroupRequest.getInstance().addCallback(grCB);
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
	 * Accept join crowd invitation
	 * 
	 * @param crowd
	 * @param caller
	 *            if input is null, ignore response Message. Response Message
	 *            object is {@link com.v2tech.service.jni.JNIResponse}
	 */
	public void acceptJoinCrowd(Crowd crowd, Registrant caller) {
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
	public void refuseJoinCrowd(Crowd crowd, Registrant caller) {
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
	
	
	

	@Override
	public void clearCalledBack() {
		ImRequest.getInstance().removeCallback(imCB);
		GroupRequest.getInstance().removeCallback(grCB);		
	}

	/**
	 * FIXME add comment 
	 * @comment-user:wenzl 2014年9月15日
	 * @overview:
	 *
	 * @param group
	 * @param caller
	 * @return:
	 */
	public void createGroup(CrowdGroup group, Registrant caller) {
		//对CREATE_GROUP_MESSAGE做超时处理，如果超时此消息不再通知上层。
		this.initTimeoutMessage(CREATE_GROUP_MESSAGE, DEFAULT_TIME_OUT_SECS,
				caller);
		GroupRequest.getInstance().createGroup(
				Group.GroupType.CHATING.intValue(), group.toXml(),
				group.toGroupUserListXml());
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


	}

	class ImRequestCB extends ImRequestCallbackAdapter {

		private Handler mCallbackHandler;

		public ImRequestCB(Handler mCallbackHandler) {
			
			this.mCallbackHandler = mCallbackHandler;
		}

		@Override
		public void OnCreateCrowdCallback(String sCrowdXml, int nResult) {
			if (sCrowdXml == null || sCrowdXml.isEmpty()) {
				return;
			}
			int start = sCrowdXml.indexOf("id='");
			int end = sCrowdXml.indexOf("'", start + 4);
			long id = 0;
			if (start != -1 && end != -1) {
				id = Long.parseLong(sCrowdXml.substring(start + 4, end));
			} else {
				V2Log.e("unmalformed crow response " + sCrowdXml);
				return;
			}
			JNIResponse jniRes = new CreateCrowdResponse(id,
					CreateCrowdResponse.Result.SUCCESS);
			Message.obtain(mCallbackHandler, CREATE_GROUP_MESSAGE, jniRes)
					.sendToTarget();
		}

	}
}
