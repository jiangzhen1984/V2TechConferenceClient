package com.V2.jni.ind;

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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

//由GroupRequest 的 OnGetGroupUserInfo()登陆后自动传来
//<xml>
//<user account='zhuangjf@v2tech.com' authtype='1' avatarlocation='http://182.92.231.197:8090/avatar/1112/AVATAR_1112/0e6daca9-c236-41f6-96ec-cf2a87e6547d.png' avatarname='0e6daca9-c236-41f6-96ec-cf2a87e6547d.png' birthday='2000-01-01' bsystemavatar='0' id='1112' nickname='继峰pc' privacy='0' sign='fdsfsdf'/>
//<user account='guozm' authtype='0' birthday='2000-01-01' bsystemavatar='1' id='1113' nickname='guozm' privacy='0'/>
//</xml>

public class BoUserInfoGroup {

	public long mId;
	public String mAccount;
	public String mNickName;
	public String mCommentName;
	public String mSign;
	public String mAuthtype;// 取值0允许任何人，1需要验证，2不允许任何人
	public String mSex;
	public String mStringBirthday;
	public String mMobile;
	public String mTelephone;
	public String mEmail;
	public String mFax;
	public String mJob;
	public String mAddress;

	public Date mBirthday;

	public static List<BoUserInfoGroup> paserXml(String xml) {
		List<BoUserInfoGroup> boGroupUserInfoList = new ArrayList<BoUserInfoGroup>();

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

				BoUserInfoGroup boGroupUserInfo = new BoUserInfoGroup();
				String strId = element.getAttribute("id");
				if (strId == null || strId.isEmpty()) {
					continue;
				}
				boGroupUserInfo.mId = Long.parseLong(strId);
				boGroupUserInfo.mAccount = element.getAttribute("account");
				boGroupUserInfo.mNickName = element.getAttribute("nickname");
				boGroupUserInfo.mCommentName = element
						.getAttribute("commentname");
				boGroupUserInfo.mSign = element.getAttribute("sign");
				boGroupUserInfo.mAuthtype = element.getAttribute("authtype");
				boGroupUserInfo.mSex = element.getAttribute("sex");
				boGroupUserInfo.mStringBirthday = element
						.getAttribute("birthday");
				boGroupUserInfo.mMobile = element.getAttribute("mobile");
				boGroupUserInfo.mTelephone = element.getAttribute("telephone");
				boGroupUserInfo.mEmail = element.getAttribute("email");
				boGroupUserInfo.mFax = element.getAttribute("fax");
				boGroupUserInfo.mJob = element.getAttribute("job");
				boGroupUserInfo.mAddress = element.getAttribute("address");

				if (boGroupUserInfo.mStringBirthday != null
						&& boGroupUserInfo.mStringBirthday.length() > 0) {
					try {
						DateFormat dp = new SimpleDateFormat("yyyy-MM-dd",
								Locale.getDefault());
						boGroupUserInfo.mBirthday = dp
								.parse(boGroupUserInfo.mStringBirthday);
					} catch (ParseException e) {
						e.printStackTrace();
					}
				}

				boGroupUserInfoList.add(boGroupUserInfo);
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
		return boGroupUserInfoList;
	}

}
