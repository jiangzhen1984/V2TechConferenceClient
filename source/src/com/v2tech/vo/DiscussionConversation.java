package com.v2tech.vo;

import android.text.TextUtils;

import java.util.List;

import com.V2.jni.V2GlobalEnum;
import com.V2.jni.util.V2Log;
import com.v2tech.service.GlobalHolder;
import com.v2tech.vo.Group.GroupType;

public class DiscussionConversation extends Conversation {

	private Group discussionGroup;
	public DiscussionConversation(Group discussion) {
		if (discussion == null)
			throw new NullPointerException("Given Discussion Group is null ... please check!");

		if (discussion.getGroupType() != GroupType.DISCUSSION) {
			throw new IllegalArgumentException(
					" Given The type of Group Object is not GroupType.DISCUSSION ... please check!");
		}
		
		this.discussionGroup = discussion;
		super.mExtId = discussionGroup.getmGId();
		super.mType = V2GlobalEnum.GROUP_TYPE_DISCUSSION;
	}

	@Override
	public String getName() {
		if(discussionGroup != null){
			if(!TextUtils.isEmpty(discussionGroup.getName())){
				return discussionGroup.getName();
			}
			else{
				String name = getDiscussionNames();
				return name;
			}
		}
		else
			return super.getName();
	}

	private String getDiscussionNames() {
        StringBuilder sb = new StringBuilder();
        List<User> users = discussionGroup.getUsers();
        User ownerUser = discussionGroup.getOwnerUser();
        if (ownerUser != null) {
            sb.append(ownerUser.getName());
        }
        if (users != null) {
            for (User user : users) {
            	if(user.getmUserId() == ownerUser.getmUserId())
            		continue ;
            	
                sb.append(" ").append(user.getName());
                if(sb.toString().length() >= 30)
                    break;
            }
        }
        return sb.toString();
	}
	
	public Group getDiscussionGroup() {
		return discussionGroup;
	}

	public void setDiscussionGroup(Group discussionGroup) {
		this.discussionGroup = discussionGroup;
	}

}
