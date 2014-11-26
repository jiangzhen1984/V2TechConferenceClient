package com.v2tech.vo;

import android.text.TextUtils;

import com.V2.jni.V2GlobalEnum;
import com.v2tech.util.DateUtil;
import com.v2tech.vo.Group.GroupType;

public class DiscussionConversation extends Conversation {

	private Group discussionGroup;
	private User lastSendUser;
	private boolean showContact;

	public DiscussionConversation(Group discussion) {
		if (discussion == null)
			throw new NullPointerException("Given Discussion Group is null ... please check!");

		if (discussionGroup.getGroupType() != GroupType.DISCUSSION) {
			throw new IllegalArgumentException(
					" Given The type of Group Object is not GroupType.DISCUSSION ... please check!");
		}
		
		this.discussionGroup = discussion;
		super.mExtId = discussionGroup.getmGId();
		super.mType = V2GlobalEnum.GROUP_TYPE_DISCUSSION;
	}

	@Override
	public String getName() {
		String superName = super.getName();
		if(TextUtils.isEmpty(superName))
			return getDiscussionNames();
		else
			return superName;	
	}

	private String getDiscussionNames() {
		return date;
//		if(discussionGroup )
//		return null;
	}

//	@Override
//	public CharSequence getMsg() {
//		if(showContact)
//			return msg;
//		else{
//			if (departmentGroup != null) {
//				User u = departmentGroup.getOwnerUser();
//				// TODO need use localization
//				return u == null ? "" : "创建人:" + u.getName();
//			}
//			return msg;
//		}
//	}
	


	public User getLastSendUser() {
		return lastSendUser;
	}

	public void setLastSendUser(User lastSendUser) {
		this.lastSendUser = lastSendUser;
	}

	public boolean isShowContact() {
		return showContact;
	}

	public void setShowContact(boolean showContact) {
		this.showContact = showContact;
	}
}
