package com.bizcom.vo;

import java.util.Date;
import java.util.List;

import android.text.TextUtils;

import com.V2.jni.util.EscapedcharactersProcessing;
import com.bizcom.vc.application.GlobalHolder;

public class DiscussionGroup extends Group {

	private boolean isCreatorExist;

	public DiscussionGroup(long gId, String name, User owner, Date createDate) {
		super(gId, GroupType.DISCUSSION, name, owner, createDate);
		isCreatorExist = true;
	}

	public DiscussionGroup(long gId, String name, User owner) {
		super(gId, GroupType.DISCUSSION, name, owner);
		isCreatorExist = true;
	}

	@Override
	public String getName() {
		if (!TextUtils.isEmpty(mName)) {
			return mName;
		}

		boolean isAddOwner = true;
		StringBuilder sb = new StringBuilder();
		List<User> users = getUsers();
		if (mOwnerUser != null && isCreatorExist) {
			if (TextUtils.isEmpty(mOwnerUser.getDisplayName()))
				mOwnerUser = GlobalHolder.getInstance().getUser(
						mOwnerUser.getmUserId());
			sb.append(mOwnerUser.getDisplayName());
			for (User user : users) {
				if (user.getmUserId() == this.mOwnerUser.getmUserId())
					continue;

				sb.append(" ").append(user.getDisplayName());
				if (sb.toString().length() >= 30)
					break;
			}
		} else {
			if (users != null) {
				for (int i = 0; i < users.size(); i++) {

					if (users.get(i).getmUserId() == GlobalHolder.getInstance()
							.getCurrentUserId())
						isAddOwner = false;

					if (i == 0)
						sb.append(users.get(i).getDisplayName());
					else
						sb.append(" ").append(users.get(i).getDisplayName());
					if (sb.toString().length() >= 30)
						break;
				}
			}

			if (isAddOwner) {
				this.addUserToGroup(GlobalHolder.getInstance().getCurrentUser());
				sb.append(" ").append(
						GlobalHolder.getInstance().getCurrentUser().getDisplayName());
			}
		}
		return sb.toString();

	}

	@Override
	public String toXml() {
		StringBuilder xml = new StringBuilder();
		xml.append("<discussion "
				+ (this.mGId > 0 ? (" id='" + this.mGId + "' ") : "")
				+ " creatoruserid='" + this.getOwnerUser().getmUserId()
				+ "' name='" + EscapedcharactersProcessing.convert(this.mName)
				+ "'/>");
		return xml.toString();
	}

	public boolean isCreatorExist() {
		return isCreatorExist;
	}

	public void setCreatorExist(boolean isCreatorExist) {
		this.isCreatorExist = isCreatorExist;
	}
}
