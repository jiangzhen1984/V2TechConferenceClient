package com.v2tech.vo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.graphics.Bitmap;

import com.V2.jni.util.V2Log;
import com.v2tech.service.GlobalHolder;

/**
 * User information
 * 
 * @author 28851274
 * 
 */
public class User implements Comparable<User> {
	private long mUserId;
	private int authtype = 0;// 取值0允许任何人，1需要验证，2不允许任何人
	private NetworkStateCode mResult;
	private DeviceType mType;
	private Status mStatus;
	private String mAccount;
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
	private String mAvatarPath;
	private String abbra;
	private String mFax;
	private String mNickName;
	private static HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
	// This value indicate this object is dirty, construct locally without any
	// user information
	private boolean isDirty;

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
		mBelongsGroup = new CopyOnWriteArraySet<Group>();
		isCurrentLoggedInUser = false;
		this.mStatus = Status.OFFLINE;
		initAbbr();
		isDirty = true;
	}

	private void initAbbr() {
		abbra = "";
		if (this.mName != null) {
			format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
			char[] cs = this.mName.toCharArray();
			for (char c : cs) {
				try {
					String[] ars = PinyinHelper.toHanyuPinyinStringArray(c,
							format);
					if (ars != null && ars.length > 0) {
						abbra += ars[0].charAt(0);
					}
				} catch (BadHanyuPinyinOutputFormatCombination e) {
					e.printStackTrace();
				}
			}
			if (abbra.equals("")) {
				abbra = this.mName.toLowerCase(Locale.getDefault());
			}
		}
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

	public DeviceType getDeviceType() {
		return mType;
	}

	public void setDeviceType(DeviceType type) {
		this.mType = type;
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
		if (mCompany == null) {
			mCompany = loadCompany(this.getFirstBelongsGroup());
		}
		return mCompany;
	}

	private String loadCompany(Group g) {
		if (g == null) {
			return "";
		}
		if (g.getParent() != null) {
			return loadCompany(g.getParent());
		} else {
			return g.getName();
		}
	}

	public void setCompany(String mCompany) {
		this.mCompany = mCompany;
	}

	public String getDepartment() {
		// FIXM me
		if (this.getFirstBelongsGroup() != null) {
			mDepartment = this.getFirstBelongsGroup().getName();
		}
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
			DateFormat sd = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
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

	public String getArra() {
		return this.abbra;
	}

	public void updateStatus(Status mStatus) {
		this.mStatus = mStatus;
	}

	public String getFax() {
		return this.mFax;
	}

	public void setFax(String fax) {
		this.mFax = fax;
	}

	public int getAuthtype() {
		return this.authtype;
	}

	public void setAuthtype(int authtype) {
		this.authtype = authtype;
	}

	public String getAccount() {
		return this.mAccount;
	}

	public void setAccount(String acc) {
		this.mAccount = acc;
	}

	public void addUserToGroup(Group g) {
		if (g == null) {
			V2Log.e(" group is null can't add user to this group");
			return;
		}
		this.mBelongsGroup.add(g);
	}

	public String getNickName() {
		return mNickName;
	}

	public void setNickName(String nickName) {
		this.mNickName = nickName;
	}

	public boolean isDirty() {
		return isDirty;
	}
	
	public void updateUser(boolean dirty) {
		this.isDirty = dirty;
	}

	public Group getFirstBelongsGroup() {
		if (this.mBelongsGroup.size() > 0) {
			for (Group g : mBelongsGroup) {
				if (g.getGroupType() != Group.GroupType.CONFERENCE) {
					return g;
				}
			}
		}
		return null;
	}

	public String getAvatarPath() {
		return mAvatarPath;
	}

	public void setAvatarPath(String avatarPath) {
		this.mAvatarPath = avatarPath;
	}

	private Bitmap avatar;

	public Bitmap getAvatarBitmap() {
		if (avatar == null || avatar.isRecycled()) {
			avatar = GlobalHolder.getInstance().getUserAvatar(this.mUserId);
		}
		return avatar;
	}

	public void setAvatarBitmap(Bitmap bm) {
		this.avatar = bm;
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

	@Override
	public int compareTo(User another) {
		// make sure current user align first position
		if (this.mUserId == GlobalHolder.getInstance().getCurrentUserId()) {
			return -1;
		}
		if (another.getmUserId() == GlobalHolder.getInstance()
				.getCurrentUserId()) {
			return 1;
		}
		if (another.getmStatus() == this.mStatus) {
			return this.abbra.compareTo(another.abbra);
		}
		if (this.mStatus == Status.ONLINE) {
			return -1;
		} else if (another.getmStatus() == Status.ONLINE) {
			return 1;
		}
		if (this.mStatus == Status.LEAVE
				|| this.mStatus == Status.DO_NOT_DISTURB
				|| this.mStatus == Status.BUSY) {
			if (another.getmStatus() == Status.LEAVE
					|| another.getmStatus() == Status.DO_NOT_DISTURB
					|| another.getmStatus() == Status.BUSY) {
				return this.abbra.compareTo(another.abbra);
			} else {
				return -1;
			}
		} else if (another.getmStatus() == Status.LEAVE
				|| another.getmStatus() == Status.DO_NOT_DISTURB
				|| another.getmStatus() == Status.BUSY) {
			return 1;
		}

		return this.abbra.compareTo(another.abbra);
	}

	public String toXml() {
		DateFormat dp = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
		String xml = "<user " + " address='"
				+ (this.getAddress() == null ? "" : this.getAddress()) + "' "
				+ "authtype='" + this.getAuthtype() + "' " + "birthday='"
				+ (this.mBirthday == null ? "" : dp.format(this.mBirthday))
				+ "' " + "job='"
				+ (this.getTitle() == null ? "" : this.getTitle()) + "' "
				+ "mobile='"
				+ (this.getCellPhone() == null ? "" : this.getCellPhone())
				+ "' " + "nickname='"
				+ (this.getName() == null ? "" : this.getName()) + "'  "
				+ "sex='" + (this.getGender() == null ? "" : this.getGender())
				+ "'  " + "sign='"
				+ (this.getSignature() == null ? "" : this.getSignature())
				+ "' " + "telephone='"
				+ (this.getTelephone() == null ? "" : this.getTelephone())
				+ "'> " + "<videolist/> </user> ";
		return xml;
	}

	/**
	 * 
	 * @param xml
	 * @return
	 */
	public static List<User> fromXml(String xml) {
		List<User> l = new ArrayList<User>();

		InputStream is = null;

		DateFormat dp = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
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
				String strId = element.getAttribute("id");
				if (strId == null || strId.isEmpty()) {
					continue;
				}
				User u = new User(Long.parseLong(strId), getAttribute(element,
						"nickname"), getAttribute(element, "email"),
						getAttribute(element, "sign"));
				u.setTelephone(getAttribute(element, "telephone"));
				u.setGender(getAttribute(element, "sex"));
				u.setAddress(getAttribute(element, "address"));
				u.setCellPhone(getAttribute(element, "mobile"));
				u.setTitle(getAttribute(element, "job"));
				u.setAccount(getAttribute(element, "account"));
				u.setNickName(getAttribute(element, "commentname"));
				try {
					String bir = element.getAttribute("birthday");
					if (bir != null && !bir.equals("")) {
						u.setBirthday(dp.parse(bir));
					}
				} catch (ParseException e) {
					e.printStackTrace();
				}
				l.add(u);
			}

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (Exception e) {
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

	private static String getAttribute(Element el, String name) {
		Attr atr = el.getAttributeNode(name);
		if (atr != null) {
			return atr.getValue();
		}
		return null;
	}

	/**
	 * 
	 * @param uID
	 * @param xml
	 * @return
	 */
	public static User fromXmlToUser(String xml) {
		int uID;
		String strID = extraAttri("id='", "'", xml);
		if (strID == null) {
			return null;
		}
		try {
			uID = Integer.parseInt(strID);
		} catch (NumberFormatException e) {
			return null;
		}

		return fromXml(uID, xml);
	}

	/**
	 * 
	 * @param uID
	 * @param xml
	 * @return
	 */
	public static User fromXml(int uID, String xml) {
		String nickName = extraAttri("nickname='", "'", xml);
		String signature = extraAttri("sign='", "'", xml);
		String job = extraAttri("job='", "'", xml);
		String telephone = extraAttri("telephone='", "'", xml);
		String mobile = extraAttri("mobile='", "'", xml);
		String address = extraAttri("address='", "'", xml);
		String gender = extraAttri("sex='", "'", xml);
		String email = extraAttri("email='", "'", xml);
		String bir = extraAttri("birthday='", "'", xml);
		String account = extraAttri("account='", "'", xml);
		String authtype = extraAttri("authtype='", "'", xml);

		DateFormat dp = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

		User u = new User(uID, nickName);
		u.setSignature(signature);
		u.setTitle(job);
		u.setTelephone(telephone);
		u.setCellPhone(mobile);
		u.setAddress(address);
		u.setGender(gender);
		u.setEmail(email);
		u.setAccount(account);
		if (authtype != null && authtype != "") {
			u.setAuthtype(Integer.parseInt(authtype));
		} else {
			u.setAuthtype(0);
		}

		if (bir != null && bir.length() > 0) {
			try {
				u.setBirthday(dp.parse(bir));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return u;
	}

	private static String extraAttri(String startStr, String endStr, String xml) {
		int pos = xml.indexOf(startStr);
		if (pos == -1) {
			return null;
		}
		int end = xml.indexOf(endStr, pos + startStr.length());
		if (end == -1) {
			return null;
		}
		return xml.substring(pos + startStr.length(), end);
	}

	public enum DeviceType {
		CELL_PHONE(2), PC(1), UNKNOWN(-1);
		private int code;

		private DeviceType(int code) {
			this.code = code;
		}

		public int toIntValue() {
			return code;
		}

		public static DeviceType fromInt(int type) {
			switch (type) {
			case 2:
				return CELL_PHONE;
			case 1:
				return PC;
			default:
				return UNKNOWN;
			}
		}
	}

	public enum Status {
		LEAVE(2), BUSY(3), DO_NOT_DISTURB(4), HIDDEN(5), ONLINE(1), OFFLINE(0), UNKNOWN(
				-1);
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
			case 2:
				return LEAVE;
			case 3:
				return BUSY;
			case 4:
				return DO_NOT_DISTURB;
			case 5:
				return HIDDEN;
			default:
				return UNKNOWN;
			}
		}
	}

}
