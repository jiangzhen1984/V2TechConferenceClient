package com.v2tech.logic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.v2tech.util.V2Log;

/**
 * User information
 * 
 * @author 28851274
 * 
 */
public class User {

	public enum Status {
		ONLINE(1), OFFLINE(0), UNKNOWN(-1);
		private int code;

		private Status(int code) {
			this.code = code;
		}
		
		public int toIntValue() {
			return code;
		}

		public static Status fromInt(int status) {
			switch (status) {
			case 0:
				return OFFLINE;
			case 1:
				return ONLINE;
			default:
				return UNKNOWN;
			}
		}
	}

	private long mUserId;

	private NetworkStateCode mResult;
	
	private Status mStatus;


	private String mName;

	private String mEmail;

	private String mSignature;

	private String mAddress;

	private String mCellPhone;

	private String mCompany;

	private String mDepartment;

	private String mGender;

	private Date mBirthday;

	private String mTelephone;

	private String mTitle;

	private Set<Group> mBelongsGroup;

	private boolean isCurrentLoggedInUser;

	public User(long mUserId) {
		this(mUserId, null, null, null);
	}

	public User(long mUserId, String name) {
		this(mUserId, name, null, null);
	}

	public User(long mUserId, String name, NetworkStateCode mResult) {
		this(mUserId, name, null, null);
		this.mResult = mResult;
	}

	public User(long mUserId, String name, String email, String signature) {
		this.mUserId = mUserId;
		this.mName = name;
		this.mEmail = email;
		this.mSignature = signature;
		mBelongsGroup = new HashSet<Group>();
		isCurrentLoggedInUser = false;
	}

	public boolean isCurrentLoggedInUser() {
		return isCurrentLoggedInUser;
	}

	public void setCurrentLoggedInUser(boolean isCurrentLoggedInUser) {
		this.isCurrentLoggedInUser = isCurrentLoggedInUser;
	}

	public long getmUserId() {
		return mUserId;
	}

	public void setmUserId(long mUserId) {
		this.mUserId = mUserId;
	}

	public NetworkStateCode getmResult() {
		return mResult;
	}

	public void setmResult(NetworkStateCode mResult) {
		this.mResult = mResult;
	}

	public String getName() {
		return mName;
	}

	public void setName(String mName) {
		this.mName = mName;
	}

	public String getmEmail() {
		return mEmail;
	}

	public void setEmail(String mail) {
		this.mEmail = mail;
	}

	public String getSignature() {
		return mSignature;
	}

	public void setSignature(String signature) {
		this.mSignature = signature;
	}

	public Set<Group> getBelongsGroup() {
		return mBelongsGroup;
	}

	public void setmBelongsGroup(Set<Group> belongsGroup) {
		this.mBelongsGroup = belongsGroup;
	}

	public String getAddress() {
		return mAddress;
	}

	public void setAddress(String mAddress) {
		this.mAddress = mAddress;
	}

	public String getCellPhone() {
		return mCellPhone;
	}

	public void setCellPhone(String mCellPhone) {
		this.mCellPhone = mCellPhone;
	}

	public String getCompany() {
		return mCompany;
	}

	public void setCompany(String mCompany) {
		this.mCompany = mCompany;
	}

	public String getDepartment() {
		return mDepartment;
	}

	public void setDepartment(String mDepartment) {
		this.mDepartment = mDepartment;
	}

	public String getGender() {
		return mGender;
	}

	public void setGender(String mGender) {
		this.mGender = mGender;
	}

	public Date getBirthday() {
		return mBirthday;
	}

	public String getBirthdayStr() {
		if (mBirthday != null) {
			DateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
			return sd.format(mBirthday);
		} else {
			return "";
		}
	}

	public void setBirthday(Date mBirthday) {
		this.mBirthday = mBirthday;
	}

	public String getTelephone() {
		return mTelephone;
	}

	public void setTelephone(String mTelephone) {
		this.mTelephone = mTelephone;
	}

	public String getTitle() {
		return mTitle;
	}

	public void setTitle(String mTitle) {
		this.mTitle = mTitle;
	}
	

	public Status getmStatus() {
		return mStatus;
	}

	public void updateStatus(Status mStatus) {
		this.mStatus = mStatus;
	}

	public void addUserToGroup(Group g) {
		if (g == null) {
			V2Log.e(" group is null can't add user to this group");
			return;
		}
		this.mBelongsGroup.add(g);
	}
	
	public Group getFirstBelongsGroup() {
		if (this.mBelongsGroup.size() > 0) {
			return this.mBelongsGroup.iterator().next();
		} else {
			return null;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		User other = (User) obj;
		if (mUserId != other.mUserId)
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (mUserId ^ (mUserId >>> 32));
		return result;
	}

	/**
	 * 
	 * @param xml
	 * @return
	 */
	public static List<User> fromXml(String xml) {
		List<User> l = new ArrayList<User>();

		InputStream is = null;

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
			Document doc = dBuilder.parse(is);

			doc.getDocumentElement().normalize();

			NodeList gList = doc.getElementsByTagName("user");
			Element element;
			for (int i = 0; i < gList.getLength(); i++) {
				element = (Element) gList.item(i);
				l.add(new User(Long.parseLong(element.getAttribute("id")),
						element.getAttribute("nickname"), element
								.getAttribute("email"), element
								.getAttribute("sign")));
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

		return l;
	}

	/**
	 * 
	 * @param uID
	 * @param xml
	 * @return
	 */
	public static User fromXml(int uID, String xml) {
		String nickName = null;
		int pos = xml.indexOf("nickname='");
		if (pos == -1) {
			V2Log.w(" no nickname");
		} else {
			int end = xml.indexOf("'", pos + 10);
			if (end != -1) {
				nickName = xml.subSequence(pos + 10, end).toString();
			}
		}
		return new User(uID, nickName);
	}

}
