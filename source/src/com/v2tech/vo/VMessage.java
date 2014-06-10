package com.v2tech.vo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.v2tech.service.GlobalHolder;

public class VMessage {

	public static final int VMESSAGE_CODE_CONF = 1;
	public static final int VMESSAGE_CODE_IM = 2;

	private static DateFormat sfF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
			Locale.getDefault());

	private static DateFormat sfL = new SimpleDateFormat("yyyy-MM-dd HH:mm",
			Locale.getDefault());

	private static DateFormat sfT = new SimpleDateFormat("HH:mm",
			Locale.getDefault());

	protected long id;

	protected User mFromUser;

	protected User mToUser;

	protected Date mDate;

	protected String mStrDateTime;

	protected int mMsgCode;

	protected String mUUID;

	protected long mGroupId;
	
	protected boolean isLocal;

	protected List<VMessageAbstractItem> itemList;

	public VMessage(User fromUser, User toUser) {
		this(0, fromUser, toUser, VMESSAGE_CODE_IM);
	}
	
	public VMessage(User fromUser, User toUser, Date date) {
		this(0, fromUser, toUser, UUID.randomUUID().toString(), new Date(),VMESSAGE_CODE_IM);
	}


	public VMessage(User fromUser, User toUser, int type) {
		this(0, fromUser, toUser, type);
	}

	public VMessage(long groupId, User fromUser, User toUser, int type) {
		this(0, fromUser, toUser, UUID.randomUUID().toString(), new Date(),type);
	}

	public VMessage(long groupId, User fromUser, User toUser) {
		this(0, fromUser, toUser, UUID.randomUUID().toString(),new Date(),
				VMESSAGE_CODE_IM);
	}

	public VMessage(long groupId, User fromUser, User toUser, String uuid, Date date,
			int type) {
		this.mGroupId = groupId;
		this.mFromUser = fromUser;
		this.mToUser = toUser;
		this.mDate = date ;
		this.mUUID = uuid;
		mMsgCode = type;

		itemList = new ArrayList<VMessageAbstractItem>();
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
	
	
	

	public boolean isLocal() {
		return GlobalHolder.getInstance().getCurrentUserId() == this.mFromUser.getmUserId()? true:false;
	}

	public void setLocal(boolean isLocal) {
		this.isLocal = isLocal;
	}
	
	
	public List<VMessageImageItem>  getImageItems() {
		List<VMessageImageItem> imageItems = new ArrayList<VMessageImageItem>();
		for (VMessageAbstractItem item : itemList) {
			if (item.getType() == VMessageAbstractItem.ITEM_TYPE_IMAGE) {
				imageItems.add((VMessageImageItem)item);
			}
		}
		return imageItems;
	}
	

	public String getUUID() {
		return mUUID;
	}

	public String getNormalDateStr() {
		if (this.mDate != null) {
			return sfF.format(this.mDate);
		} else {
			return null;
		}
	}

	public String getFullDateStr() {
		if (this.mDate != null) {
			return sfF.format(this.mDate);
		} else {
			return null;
		}
	}

	public String getDateTimeStr() {
		if (this.mStrDateTime == null && this.mDate != null) {
			if (System.currentTimeMillis() / (24 * 3600000) == this.mDate
					.getTime() / (24 * 3600000)) {
				mStrDateTime = sfT.format(this.mDate);
			} else {
				mStrDateTime = sfL.format(this.mDate);
			}
		}
		return this.mStrDateTime;
	}

	public User getToUser() {
		return this.mToUser;
	}

	public void addItem(VMessageAbstractItem item) {
		this.itemList.add(item);
	}


	public List<VMessageAbstractItem> getItems() {
		return this.itemList;
	}
	
	
	
	public String getAllTextContent() {
		StringBuilder sb = new StringBuilder();
		for (VMessageAbstractItem item : itemList) {
			if (item.getType() == VMessageAbstractItem.ITEM_TYPE_TEXT) {
				if (item.isNewLine() && sb.length() != 0) {
					sb.append("\n");
				}
				sb.append(((VMessageTextItem)item).getText());
			}
		}
		return sb.toString();
	}
	
	
	
	public void recycleAllImageMessage() {
		for (VMessageAbstractItem item : itemList) {
			if (item.getType() == VMessageAbstractItem.ITEM_TYPE_IMAGE) {
				((VMessageImageItem)item).recycleAll();
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
		sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
				.append("<TChatData IsAutoReply=\"False\" MessageID=\"{"
						+ this.mUUID + "}\">\n")
				.append("<FontList>\n")
				.append("<TChatFont Color=\"0\" Name=\"Tahoma\" Size=\"9\" Style=\"\"/>")
				.append("</FontList>\n").append("<ItemList>\n");
		for (VMessageAbstractItem item : itemList) {
			sb.append(item.toXmlItem());
		}
		sb.append("    </ItemList>");
		sb.append("</TChatData>");
		return sb.toString();
	}
}
