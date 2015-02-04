package com.bizcom.vo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.V2.jni.util.V2Log;
import com.bizcom.util.DateUtil;
import com.bizcom.vc.activity.conversation.MessageBodyView;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.application.V2GlobalEnum;

public class VMessage {

	protected long id;

	protected User mFromUser;

	protected User mToUser;

	protected Date mDate;

	protected long mDateLong;

	/**
	 * The flag Decides VMessage Object type
	 * 
	 * @see V2GlobalEnum --> GroupType
	 */
	protected int mMsgCode;

	protected String mUUID;

	protected long mGroupId;

	protected boolean isLocal;

	protected int mState;

	protected String mXmlDatas;

	/**
	 * The flag decide that Whether should to display the Time View
	 * 
	 * @see MessageBodyView --> timeTV variable
	 */
	protected boolean isShowTime;

	/**
	 * This flag indicates that this VMessage Object is from resend or not , so
	 * as to decide whether to display Sending Icon View
	 * 
	 * @see MessageBodyView --> sendingIcon variable
	 */
	protected boolean isResendMessage;

	/**
	 * This flag indicates that this VMessage Object is autio reply or not
	 */
	protected boolean isAutoReply;
	
	protected int readState;
	
	public int notReceiveImageSize = 0;
	
	public boolean isBeginSendingAnima;
	public boolean isShowFailed;
	public boolean isUpdateDate;
	public boolean isUpdateAvatar;

	protected List<VMessageAbstractItem> itemList;
	protected List<VMessageImageItem> imageItems;
	protected List<VMessageAudioItem> audioItems;
	protected List<VMessageFileItem> fileItems;

	public VMessage(int groupType, long groupId, User fromUser, Date date) {
		this(groupType, groupId, fromUser, null, UUID.randomUUID().toString(),
				date);
	}

	public VMessage(int groupType, long groupId, User fromUser, User toUser,
			Date date) {
		this(groupType, groupId, fromUser, toUser,
				UUID.randomUUID().toString(), date);
	}

	public VMessage(int groupType, long groupId, User fromUser, User toUser,
			String uuid, Date date) {
		this.mGroupId = groupId;
		this.mFromUser = fromUser;
		this.mToUser = toUser;
		this.mDate = date;
		this.mUUID = uuid;
		this.mMsgCode = groupType;
		this.readState = VMessageAbstractItem.STATE_READED;

		itemList = new ArrayList<VMessageAbstractItem>();
		imageItems = new ArrayList<VMessageImageItem>();
		audioItems = new ArrayList<VMessageAudioItem>();
		fileItems = new ArrayList<VMessageFileItem>();
	}

	public long getmDateLong() {
		return mDate.getTime();
	}

	public void setmDateLong(long mDateLong) {
		this.mDateLong = mDateLong;
	}

	public String getmXmlDatas() {
		return mXmlDatas;
	}

	public void setmXmlDatas(String mXmlDatas) {
		this.mXmlDatas = mXmlDatas;
	}

	public void setMsgCode(int code) {
		this.mMsgCode = code;
	}

	public int getMsgCode() {
		return this.mMsgCode;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public User getFromUser() {
		return mFromUser;
	}

	public void setFromUser(User fromUser) {
		this.mFromUser = fromUser;
	}

	public void setToUser(User toUser) {
		this.mToUser = toUser;
	}

	public Date getDate() {
		return mDate;
	}

	public void setDate(Date date) {
		this.mDate = date;
	}

	public long getGroupId() {
		return this.mGroupId;
	}

	public void setGroupId(long groupId) {
		this.mGroupId = groupId;
	}

	public int getState() {
		return mState;
	}

	public void setState(int state) {
		this.mState = state;
	}

	public void setUUID(String UUID) {
		this.mUUID = UUID;
	}

	public int isReadState() {
		return readState;
	}

	public void setReadState(int readState) {
		this.readState = readState;
	}

	public boolean isShowTime() {
		return isShowTime;
	}

	public void setShowTime(boolean isShowTime) {
		this.isShowTime = isShowTime;
	}

	public boolean isAutoReply() {
		return isAutoReply;
	}

	public void setAutoReply(boolean isAutoReply) {
		this.isAutoReply = isAutoReply;
	}

	public boolean isLocal() {
		if (this.mFromUser == null) {
			return false;
		}
		return GlobalHolder.getInstance().getCurrentUserId() == this.mFromUser
				.getmUserId() ? true : false;
	}

	public void setLocal(boolean isLocal) {
		this.isLocal = isLocal;
	}

	public boolean isResendMessage() {
		return isResendMessage;
	}

	public void setResendMessage(boolean isResendMessage) {
		this.isResendMessage = isResendMessage;
	}
	
	public void setImageItems(List<VMessageImageItem> imageItems) {
		for (int i = 0; i < this.imageItems.size(); i++) {
			this.imageItems.get(i).recycle();
			this.imageItems.get(i).setFilePath(imageItems.get(i).getFilePath());
		}
		this.imageItems = imageItems;
	}
	
	public void addItem(VMessageAbstractItem item) {
		this.itemList.add(item);
		if(item.getType() == VMessageAbstractItem.ITEM_TYPE_IMAGE){
			imageItems.add((VMessageImageItem) item);
		} else if(item.getType() == VMessageAbstractItem.ITEM_TYPE_AUDIO){
			audioItems.add((VMessageAudioItem) item);
		} else if(item.getType() == VMessageAbstractItem.ITEM_TYPE_FILE){
			fileItems.add((VMessageFileItem) item);
		}
	}
	
	public List<VMessageImageItem> getImageItems() {
		return imageItems;
	}

	public List<VMessageAudioItem> getAudioItems() {
		return audioItems;
	}

	public List<VMessageFileItem> getFileItems() {
		return fileItems;
	}

	public List<VMessageLinkTextItem> getLinkItems() {
		List<VMessageLinkTextItem> linkItems = new ArrayList<VMessageLinkTextItem>();
		for (VMessageAbstractItem item : itemList) {
			if (item.getType() == VMessageAbstractItem.ITEM_TYPE_LINK_TEXT) {
				linkItems.add((VMessageLinkTextItem) item);
			}
		}
		return linkItems;
	}

	public String getUUID() {
		return mUUID;
	}

	public String getStandFormatDate() {
		if (mDate != null) {
			return DateUtil.getStandardDate(mDate);
		}
		return null;
	}

	public String getStringDate() {
		if (mDate != null) {
			return DateUtil.getStringDate(mDate.getTime());
		}
		return null;
	}

	public User getToUser() {
		return this.mToUser;
	}

	public List<VMessageAbstractItem> getItems() {
		return this.itemList;
	}

	public String getTextContent() {

		StringBuilder sb = new StringBuilder();
		for (VMessageAbstractItem item : itemList) {
			if (item.getType() == VMessageAbstractItem.ITEM_TYPE_TEXT) {
				if (item.isNewLine() && sb.length() != 0) {
					sb.append("\n");
				}
				sb.append(((VMessageTextItem) item).getText());
			}
		}
		return sb.toString();
	}

	public void recycleAllImageMessage() {
		for (VMessageAbstractItem item : itemList) {
			if (item.getType() == VMessageAbstractItem.ITEM_TYPE_IMAGE) {
				((VMessageImageItem) item).recycleAll();
			}
		}
	}

	/**
	 * Color user BGR
	 * 
	 * @return
	 */
	public String toXml() {
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n").append(
				"<TChatData IsAutoReply=\"False\" MessageID=\"" + this.mUUID
						+ "\">\n");

		sb.append("<FontList>\n");
		sb.append("<TChatFont Color=\"0\" Name=\"Tahoma\" Size=\"9\" Style=\"\"/>");
		for (VMessageAbstractItem item : itemList) {
			if (item.getType() == VMessageAbstractItem.ITEM_TYPE_LINK_TEXT) {
				sb.append("<TChatFont Color=\"14127617\" Name=\"Tahoma\" Size=\"9\" Style=\"fsUnderline\"/>");
			}
		}
		sb.append("</FontList>\n").append("<ItemList>\n");

		for (VMessageAbstractItem item : itemList) {
			sb.append(item.toXmlItem());
		}
		sb.append("    </ItemList>");
		sb.append("</TChatData>");
		if (V2Log.isDebuggable) {
			V2Log.d(sb.toString());
		}
		return sb.toString();
	}
}
