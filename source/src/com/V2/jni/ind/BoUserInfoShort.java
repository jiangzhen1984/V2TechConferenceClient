package com.V2.jni.ind;

import java.io.ByteArrayInputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

//不同函数来的xml的user的字段
//CLASS = ConfRequest METHOD = OnConfMemberEnter()
//<user accounttype='1' id='11122' nickname='wenzl1' uetype='1'/>

//CLASS = GroupRequest METHOD = OnApplyJoinGroup()
//<user account='wenzl1' accounttype='1' bsystemavatar='1' id='11122' nickname='wenzl1' uetype='1'/> reason = 666

//CLASS = GroupRequest METHOD = OnInviteJoinGroup() 
//<user id='11122'/>

//CLASS = GroupRequest METHOD = OnAddGroupUserInfo()
//<user account='wenzl2' accounttype='1' bsystemavatar='1' id='11123' nickname='wenzl2' uetype='2'/>

public class BoUserInfoShort {
	public long mId;
	public String mAccount;
	public String mNickName;// 显示的姓名
	public String mUeType;// 登录使用的设备类型
	public String mAccountType;// 参会类型1普通，2快速入会

	// 从xml解析出来
	public static BoUserInfoShort parserXml(String xml) throws Exception {
		if (xml == null) {
			return null;
		}

		final BoUserInfoShort boUserInfoShort = new BoUserInfoShort();
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
				xml.getBytes());

		SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
		// 是阻塞的，可以正常返回数据
		saxParser.parse(byteArrayInputStream, new DefaultHandler() {
			@Override
			public void startDocument() throws SAXException {
				super.startDocument();
			}

			@Override
			public void startElement(String uri, String localName,
					String qName, Attributes attributes) throws SAXException {
				super.startElement(uri, localName, qName, attributes);
				if (localName.equals("user")) {

					// public String mUeType;// 登录使用的设备类型
					// public String mAccountType;// 参会类型1普通，2快速入会

					String id = attributes.getValue("id");
					if (id == null) {
						boUserInfoShort.mId = -1;
						return;
					}

					try {
						boUserInfoShort.mId = Integer.valueOf(id);
					} catch (NumberFormatException e) {
						boUserInfoShort.mId = -1;
					}

					boUserInfoShort.mAccount = attributes
							.getValue("account");
					boUserInfoShort.mNickName = attributes
							.getValue("nickname");
					boUserInfoShort.mUeType = attributes.getValue("uetype");
					boUserInfoShort.mAccountType = attributes
							.getValue("accounttype");
				}
			}

			@Override
			public void characters(char[] ch, int start, int length)
					throws SAXException {
				super.characters(ch, start, length);

			}

			@Override
			public void endElement(String uri, String localName, String qName)
					throws SAXException {
				super.endElement(uri, localName, qName);
			}

			@Override
			public void endDocument() throws SAXException {
				super.endDocument();
			}
		});

		if (boUserInfoShort.mId == -1) {
			return null;
		} else {
			return boUserInfoShort;
		}

	}

}
