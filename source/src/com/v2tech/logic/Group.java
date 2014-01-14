package com.v2tech.logic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Group {

	private long mGId;

	private int mGroupType;

	private String mName;

	private String mOwner;

	private User mOwnerUser;

	private String mCreateDate;

	public Group(long mGId, int mGroupType, String mName, String mOwner,
			String createDate) {
		super();
		this.mGId = mGId;
		this.mGroupType = mGroupType;
		this.mName = mName;
		this.mOwner = mOwner;
		this.mCreateDate = createDate;
		Date d = new Date(Long.parseLong(createDate));
		DateFormat sd = new  SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
		this.mCreateDate = sd.format(d);
	}

	public long getmGId() {
		return mGId;
	}

	public void setGId(long mGId) {
		this.mGId = mGId;
	}

	public int getGroupType() {
		return mGroupType;
	}

	public void setGroupType(int mGroupType) {
		this.mGroupType = mGroupType;
	}

	public String getName() {
		return mName;
	}

	public void setName(String mName) {
		this.mName = mName;
	}

	public String getOwner() {
		return mOwner;
	}

	public void setOwner(String mOwner) {
		this.mOwner = mOwner;
	}

	public String getCreateDate() {
		return mCreateDate;
	}

	public void setCreateDate(String mCreateDate) {
		this.mCreateDate = mCreateDate;
	}
	
	

	public User getOwnerUser() {
		return mOwnerUser;
	}

	public void setOwnerUser(User mOwnerUser) {
		this.mOwnerUser = mOwnerUser;
	}

	/**
	 * <xml><conf createuserid='1124' id='513891897880' start time='1389189927'
	 * subject='est'/><conf createuserid='1124' id='513891899176'
	 * starttime='1389190062' subject='eee'/></xml>
	 * 
	 * @param xml
	 * @return
	 */
	public static List<Group> parserFromXml(int type, String xml) {
		List<Group> list = new ArrayList<Group>();

		InputStream is = null;

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
			Document doc = dBuilder.parse(is);

			doc.getDocumentElement().normalize();

			NodeList conferenceList = doc.getElementsByTagName("conf");
			Element conferenceElement;

			for (int i = 0; i < conferenceList.getLength(); i++) {
				conferenceElement = (Element) conferenceList.item(i);
				list.add(new Group(Long.parseLong(conferenceElement.getAttribute("id")), 4, conferenceElement
						.getAttribute("subject"), conferenceElement
						.getAttribute("createuserid"), conferenceElement
						.getAttribute("starttime")));
			}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return list;
	}

}
