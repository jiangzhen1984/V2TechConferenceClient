package com.v2tech.vo;

import android.text.TextUtils;

import com.v2tech.service.GlobalHolder;
import com.v2tech.util.DateUtil;
import com.v2tech.vo.Group.GroupType;

public class CrowdConversation extends Conversation {
	
	private Group group;
	private User lastSendUser;
	private boolean showContact;

	public CrowdConversation(Group g) {
		if (g == null) {
			throw new NullPointerException(" group is null");
		}
		if (g.getGroupType() != GroupType.CHATING && g.getGroupType() != GroupType.DISCUSSION) {
			throw new IllegalArgumentException(" group type is not GroupType.CHATING");
		}
		this.group = g;
		super.mExtId = g.getmGId();
		super.mType = TYPE_GROUP;
	}

	@Override
	public String getName() {
        if (group != null) {
            return group.getName();
        }
        return super.getName();
	}

	@Override
	public CharSequence getMsg() {
		if(showContact)
			return msg;
		else{
			if (group != null) {
				User u = group.getOwnerUser();
				if(u != null){
					if(TextUtils.isEmpty(u.getName())){
						u = GlobalHolder.getInstance().getUser(u.getmUserId());
						group.setOwnerUser(u);
					}
					boolean isFriend = GlobalHolder.getInstance().isFriend(u);
					if(isFriend)
						return "创建人:" + u.getNickName();
					else
						return "创建人:" + u.getName();
				}
			}
			return msg;
		}
	}

	@Override
	public String getDate() {
		if(showContact){
			if(dateLong != null){
				return DateUtil.getStringDate(Long.valueOf(dateLong));
			}
			return super.getDate();
		}
		else{
			if (group != null) {
				return group.getStrCreateDate();
			}
			return super.getDate();
		}
	}
	
	@Override
	public String getDateLong() {
		if (dateLong != null) {
			return dateLong;
		}
		return super.getDateLong();
	}
	
	public void setMsg(String msg) {
		this.msg = msg;
	}
	
	public void setDate(String date) {
		this.date = date;
	}



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
	
	public Group getGroup() {
		return group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

}
