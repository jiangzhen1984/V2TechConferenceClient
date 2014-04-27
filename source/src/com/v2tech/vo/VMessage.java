package com.v2tech.vo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.V2.jni.ChatRequest;

public class VMessage {

	// TODO add comments
	public static final int VMESSAGE_CODE_CONF = 1;
	public static final int VMESSAGE_CODE_IM = 2;

	public enum MessageType {
		TEXT(1), IMAGE(2), IMAGE_AND_TEXT(3);

		private int code;

		private MessageType(int code) {
			this.code = code;
		}

		public int getIntValue() {
			return code;
		}
	}

	private static DateFormat sfF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
			Locale.getDefault());

	private static DateFormat sfL = new SimpleDateFormat("yyyy-MM-dd HH:mm",
			Locale.getDefault());

	private static DateFormat sfT = new SimpleDateFormat("HH:mm",
			Locale.getDefault());

	private long id;

	private User mUser;

	private User mToUser;

	protected MessageType mType;

	private String mText;

	private Date mDate;

	private boolean isLocal;

	private String mStrDateTime;

	protected String mUUID;

	protected int mMsgCode;
	
	//FIXM optimize code
	public long mGroupId;

	protected VMessage() {
		this(null, null, null, false);
	}

	public VMessage(User u, User toUser) {
		this(u, toUser, null, false);
	}

	public VMessage(User u, User toUser, String text) {
		this(u, toUser, text, false);
	}

	public VMessage(User u, User toUser, boolean isRemote) {
		this(u, toUser, null, isRemote);
	}

	public VMessage(User u, User toUser, String text, boolean isRemote) {
		this.mUser = u;
		this.mToUser = toUser;
		this.mText = text;
		this.mDate = new Date();
		this.mType = MessageType.TEXT;
		this.isLocal = !isRemote;
		this.mUUID = UUID.randomUUID().toString();

		if (System.currentTimeMillis() / (24 * 3600000) == this.mDate.getTime()
				/ (24 * 3600000)) {
			mStrDateTime = sfT.format(this.mDate);
		} else {
			mStrDateTime = sfL.format(this.mDate);
		}
		mMsgCode = ChatRequest.BT_IM;

	}

	public void setMsgCode(int code) {
		this.mMsgCode = code;
	}

	public int getMsgCode() {
		return this.mMsgCode;
	}

	public String getUUID() {
		return this.mUUID;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public User getUser() {
		return mUser;
	}

	public void setUser(User mUser) {
		this.mUser = mUser;
	}

	public void setToUser(User toUser) {
		this.mToUser = toUser;
	}

	public MessageType getType() {
		return mType;
	}

	public void setType(MessageType mType) {
		this.mType = mType;
	}

	public String getText() {
		return mText;
	}

	public void setText(String mText) {
		this.mText = mText;
	}

	public Date getDate() {
		return mDate;
	}

	public void setDate(Date mDate) {
		if (mDate == null) {
			return;
		}
		this.mDate = mDate;
		if (System.currentTimeMillis() / (24 * 3600000) == this.mDate.getTime()
				/ (24 * 3600000)) {
			mStrDateTime = sfT.format(this.mDate);
		} else {
			mStrDateTime = sfL.format(this.mDate);
		}
	}

	public String getNormalDateStr() {
		if (this.mDate != null) {
			return sfL.format(this.mDate);
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

	public boolean isLocal() {
		return isLocal;
	}

	public void setLocal(boolean isLocal) {
		this.isLocal = isLocal;
	}

	public String getDateTimeStr() {
		return this.mStrDateTime;
	}

	public User getToUser() {
		return this.mToUser;
	}

	/**
	 * return null if parse failed
	 * 
	 * @param xml
	 * @return
	 */
	public static VMessage fromXml(String xml) {
		int posS = -1;
		int posE = -1;
		posS = xml.indexOf("Text=\"");
		if (posS != -1) {
			posE = xml.indexOf("\"", posS + 6);
			if (posE != -1) {
				String content = xml.substring(posS + 6, posE);
				VMessage vm = new VMessage();
				vm.setText(content);
				return vm;
			}
		}
		return null;
	}

	public static VMessage fromXml(User from, User to, Date date, String xml) {
		// List<VMessage> msgList = new ArrayList<VMessage>();
		VMessage vm = new VMessage(from, to, "", true);
		InputStream is = null;
		int dataCount = 0;
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
			Document doc = dBuilder.parse(is);

			doc.getDocumentElement().normalize();
			// NodeList textMsgItemNL =
			// doc.getElementsByTagName("TTextChatItem");
			NodeList textMsgItemNL = doc.getElementsByTagName("ItemList");
			if (textMsgItemNL.getLength() <= 0) {
				return null;
			}
			vm.setDate(date);
			vm.setType(MessageType.TEXT);
			// msgList.add(vm);
			Element msgEl = (Element) textMsgItemNL.item(0);
			NodeList itemList = msgEl.getChildNodes();
			//FIMXE optimze code
			for (int i = 0; i < itemList.getLength(); i++) {
				Node n = itemList.item(i);
				if (n instanceof Element) {
					msgEl = (Element) itemList.item(i);
					if (!msgEl.getTagName().equals("TPictureChatItem")) {
						dataCount++;
						if (i == textMsgItemNL.getLength() - 1) {
							vm.setText(vm.getText()
									+ msgEl.getAttribute("Text"));
						} else {
							vm.setText(vm.getText()
									+ msgEl.getAttribute("Text") + "\n");
						}
					}

				}
			}

			// NodeList imgMsgItemNL =
			// doc.getElementsByTagName("TPictureChatItem");
			// for (int i=0;i<imgMsgItemNL.getLength(); i++) {
			// Element msgEl = (Element)imgMsgItemNL.item(i);
			// VMessage vmImage = new VImageMessage(from, to,
			// msgEl.getAttribute("GUID"), msgEl.getAttribute("FileExt"));
			// vmImage.setDate(date);
			// vmImage.setType(MessageType.IMAGE);
			// msgList.add(vmImage);
			// }

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (dataCount > 0)
			return vm;
		else
			return null;
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
				.append("</FontList>\n")
				.append("<ItemList>\n")
				.append("<TTextChatItem NewLine=\"True\" FontIndex=\"0\" Text=\""
						+ this.mText + "\"/>").append("    </ItemList>")
				.append("</TChatData>");
		return sb.toString();
	}
}
