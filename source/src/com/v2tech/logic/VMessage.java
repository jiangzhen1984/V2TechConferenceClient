package com.v2tech.logic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class VMessage {

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

	private static DateFormat sfL = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	private static DateFormat sfT = new SimpleDateFormat("HH:mm");

	
	private long id;
	
	private User mUser;

	private User mToUser;

	protected MessageType mType;

	private String mText;

	private Date mDate;

	private boolean isLocal;

	private String mStrDateTime;
	
	private VMessage() {
		
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

		if (System.currentTimeMillis() / (24 * 36000) == this.mDate.getTime()
				/ (24 * 36000)) {
			mStrDateTime = sfL.format(this.mDate);
		} else {
			mStrDateTime = sfT.format(this.mDate);
		}

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
		if (System.currentTimeMillis() / (24 * 36000) == this.mDate.getTime()
				/ (24 * 36000)) {
			mStrDateTime = sfL.format(this.mDate);
		} else {
			mStrDateTime = sfT.format(this.mDate);
		}
	}
	
	public String getNormalDateStr() {
		if (this.mDate != null) {
			return  sfL.format(this.mDate);
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
		//List<VMessage> msgList = new ArrayList<VMessage>();
		VMessage vm = new VMessage(from, to,"", true);
		InputStream is = null;

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
			try {
				dBuilder = dbFactory.newDocumentBuilder();
				is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
				Document doc = dBuilder.parse(is);

				doc.getDocumentElement().normalize();
				NodeList textMsgItemNL = doc.getElementsByTagName("TTextChatItem");
				
				vm.setDate(date);
				vm.setType(MessageType.TEXT);
				//msgList.add(vm);
				for (int i=0;i<textMsgItemNL.getLength(); i++) {
					Element msgEl = (Element)textMsgItemNL.item(i);
					 vm.setText(vm.getText()+msgEl.getAttribute("Text")+"\n");
				}
				
				
//				NodeList imgMsgItemNL = doc.getElementsByTagName("TPictureChatItem");
//				for (int i=0;i<imgMsgItemNL.getLength(); i++) {
//					Element msgEl = (Element)imgMsgItemNL.item(i);
//					VMessage vmImage = new VImageMessage(from, to, msgEl.getAttribute("GUID"), msgEl.getAttribute("FileExt"));
//					vmImage.setDate(date);
//					vmImage.setType(MessageType.IMAGE);
//					msgList.add(vmImage);
//				}
				
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		
			return vm;
	}

	public String toXml() {
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
				.append("<TChatData IsAutoReply=\"False\">\n")
				.append("<FontList>\n")
				.append("<TChatFont Color=\"255\" Name=\"SimSun\" Size=\"9\" Style=\"\"/>")
				.append("</FontList>\n")
				.append("<ItemList>\n")
				.append("<TTextChatItem NewLine=\"True\" FontIndex=\"0\" Text=\""
						+ this.mText + "\"/>").append("    </ItemList>")
				.append("</TChatData>");
		return sb.toString();
	}
}
