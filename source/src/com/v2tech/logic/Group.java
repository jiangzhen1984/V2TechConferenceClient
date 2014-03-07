package com.v2tech.logic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
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

import com.v2tech.util.V2Log;

/**
 * Group information
 * 
 * @author 28851274
 * 
 */
public class Group {

	private long mGId;

	private GroupType mGroupType;

	private String mName;

	private long mOwner;

	private User mOwnerUser;

	private String mCreateDate;

	private Group mParent;

	private List<Group> mChild;

	private List<User> users;

	int level;

	public enum GroupType {
		CONTACT(1), CONFERENCE(4), UNKNOWN(-1);

		private int type;

		private GroupType(int type) {
			this.type = type;
		}

		public static GroupType fromInt(int code) {
			switch (code) {
			case 1:
				return CONTACT;
			case 4:
				return CONFERENCE;
			default:
				return UNKNOWN;

			}
		}

		public int intValue() {
			return type;
		}
	}

	public Group(long mGId, GroupType mGroupType, String mName, String mOwner,
			String createDate) {
		super();
		this.mGId = mGId;
		this.mGroupType = mGroupType;
		this.mName = mName;
		if (mOwner != null) {
			this.mOwner = Long.parseLong(mOwner);
		}
		this.mCreateDate = createDate;
		if (createDate != null) {
			Date d = new Date(Long.parseLong(createDate) * 1000);
			DateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
			this.mCreateDate = sd.format(d);
		}

		users = new ArrayList<User>();
		mChild = new ArrayList<Group>();
		level = 1;
	}

	public long getmGId() {
		return mGId;
	}

	public void setGId(long mGId) {
		this.mGId = mGId;
	}

	public GroupType getGroupType() {
		return mGroupType;
	}

	public void setGroupType(GroupType mGroupType) {
		this.mGroupType = mGroupType;
	}

	public String getName() {
		return mName;
	}

	public void setName(String mName) {
		this.mName = mName;
	}

	public long getOwner() {
		return mOwner;
	}

	public void setOwner(long mOwner) {
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

	public Group getParent() {
		return mParent;
	}

	public void setmParent(Group parent) {
		this.mParent = parent;
		level = this.getParent().getLevel() + 1;
	}

	public void addUserToGroup(User u) {
		if (u == null) {
			V2Log.e(" Invalid user data");
			return;
		}
		this.users.add(u);
		synchronized (users) {
			Collections.sort(users);
		}
	}

	public List<User> getUsers() {
		synchronized (users) {
			Collections.sort(users);
		}
		return this.users;
	}

	public List<Group> getChildGroup() {
		return this.mChild;
	}

	public void addGroupToGroup(Group g) {
		if (g == null) {
			V2Log.e(" Invalid group data");
			return;
		}
		this.mChild.add(g);
		g.setmParent(this);
	}

	public boolean findUser(User u, Group g) {
		for (User tu : g.getUsers()) {
			if (tu.getmUserId() == u.getmUserId()) {
				return true;
			}
		}
		for (Group subG : g.getChildGroup()) {
			boolean flag = findUser(u, subG);
			if (flag == true) {
				return flag;
			}
		}
		return false;
	}

	public List<User> searchUser(String text) {
		List<User> l = new ArrayList<User>();
		Group.searchUser(text, l, this);
		return l;
	}

	public static void searchUser(String text, List<User> l, Group g) {
		if (l == null || g == null) {
			return;
		}
		for (User u : g.getUsers()) {
			if (u.getName().contains(text)) {
				l.add(u);
			}
		}
		for (Group subG : g.getChildGroup()) {
			searchUser(text, l, subG);
		}
	}

	// FIXME need to be optimize
	public int getOnlineUserCount() {
		return getUserOnlineCount(this);
	}

	private int getUserOnlineCount(Group g) {
		int c = 0;
		if (g == null) {
			return 0;
		}
		synchronized (g) {
			for (User u : g.getUsers()) {
				if (u.getmStatus() == User.Status.ONLINE) {
					c++;
				}
			}

			for (Group subG : g.getChildGroup()) {
				c += getUserOnlineCount(subG);
			}
		}
		return c;
	}

	public int getUserCount() {
		return getUserCount(this);
	}

	private int getUserCount(Group g) {
		int c = g.getUsers().size();
		for (Group subG : g.getChildGroup()) {
			c += getUserCount(subG);
		}
		return c;
	}

	public void addUserToGroup(List<User> l) {
		for (User u : l) {
			this.users.add(u);
			u.addUserToGroup(this);
		}
		synchronized (users) {
			Collections.sort(users);
		}
	}

	public int getLevel() {
		return level;
	}

	public void updatePosition() {
		Collections.sort(users);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (mGId ^ (mGId >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Group other = (Group) obj;
		if (mGId != other.mGId)
			return false;
		return true;
	}

	/**
	 * 
	 * type contact(1): <xml><pubgroup id='61' name='ronghuo的组织'><pubgroup
	 * id='21' name='1'/></pubgroup></xml>
	 * 
	 * type conference(4): <xml><conf createuserid='1124' id='513891897880'
	 * start time='1389189927' subject='est'/><conf createuserid='1124'
	 * id='513891899176' starttime='1389190062' subject='eee'/></xml>
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

			if (type == GroupType.CONTACT.intValue()) {
				NodeList gList = doc.getChildNodes().item(0).getChildNodes();
				Element element;
				for (int i = 0; i < gList.getLength(); i++) {
					element = (Element) gList.item(i);
					Group g = new Group(Long.parseLong(element
							.getAttribute("id")), GroupType.fromInt(type),
							element.getAttribute("name"), null, null);
					list.add(g);

					// TODO add sub Group
					NodeList subGroupNodeList = element.getChildNodes();
					for (int j = 0; j < subGroupNodeList.getLength(); j++) {
						Element subGroupEl = (Element) subGroupNodeList.item(j);
						Group subGroup = new Group(Long.parseLong(subGroupEl
								.getAttribute("id")), GroupType.fromInt(type),
								subGroupEl.getAttribute("name"), null, null);
						g.addGroupToGroup(subGroup);

						NodeList subSubGroupNodeList = subGroupEl
								.getChildNodes();

						for (int k = 0; k < subSubGroupNodeList.getLength(); k++) {
							Element subSubGroupEl = (Element) subSubGroupNodeList
									.item(k);
							subGroup.addGroupToGroup(new Group(
									Long.parseLong(subSubGroupEl
											.getAttribute("id")), GroupType
											.fromInt(type), subSubGroupEl
											.getAttribute("name"), null, null));
						}

					}
				}

			} else if (type == GroupType.CONFERENCE.intValue()) {
				NodeList conferenceList = doc.getElementsByTagName("conf");
				Element conferenceElement;

				for (int i = 0; i < conferenceList.getLength(); i++) {
					conferenceElement = (Element) conferenceList.item(i);
					list.add(new Group(Long.parseLong(conferenceElement
							.getAttribute("id")), GroupType.fromInt(type),
							conferenceElement.getAttribute("subject"),
							conferenceElement.getAttribute("createuserid"),
							conferenceElement.getAttribute("starttime")));
				}
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
