package com.V2.jni.ind;

import java.io.ByteArrayInputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.V2.jni.util.EscapedcharactersProcessing;
import android.os.Parcel;
import android.os.Parcelable;

//登录后自动传来的用户信息
//<user account='wenzl2' authtype='1' birthday='2000-01-01' bsystemavatar='1' id='11123' nickname='wenzl2' orgboxid='1' privacy='0'userboxid='126'bconf='1'bemail='1' bfilebox='1' blive='1' bmix='1' bsip='1' bsms='1' bvod='1' />
//使用ImRequest 的方法 getUserBaseInfo(long nUserID)请求得到的用户信息
//<user account='wenzl2' authtype='1' birthday='2000-01-01' bsystemavatar='1' id='11123' nickname='wenzl2' orgboxid='1' privacy='0' userboxid='126'/>
//入会时来的用户信息
//<user accounttype='2' id='81003' nickname='1234' uetype='1'/>

public class BoUserInfoBase implements Parcelable {

	public long mId;
	public String mAccount;// 登录用的帐号字符串
	public String mNickName;// 显示的姓名
	public String mCommentName;// 好友的备注姓名
	public String mAddress;
	public Date mBirthday;
	public String mStringBirthday;
	public String mEmail;
	public String mFax;
	public String mJob;
	public String mMobile;
	public String mSex;
	public String mSign;
	public String mTelephone;
	public String mAuthtype;// 取值0允许任何人，1需要验证，2不允许任何人

	public BoUserInfoBase() {

	}

	public BoUserInfoBase(long uid) {
		this(uid, null);
	}

	public BoUserInfoBase(long uid, String nickName) {
		super();
		this.mId = uid;
		this.mNickName = EscapedcharactersProcessing.reverse(nickName);
	}

	public String getNickName() {
		return mNickName;
	}

	public void setNickName_name(String nickName) {
		this.mNickName = EscapedcharactersProcessing.reverse(nickName);
	}

	public String getCommentName() {
		return mCommentName;
	}

	public void setCommentName(String CommentName) {
		this.mCommentName = EscapedcharactersProcessing.reverse(CommentName);
	}

	public String getmSignature() {
		return mSign;
	}

	public void setmSignature(String mSignature) {
		this.mSign = EscapedcharactersProcessing.reverse(mSignature);
	}

	@Override
	public String toString() {
		return " [ID:" + this.mId + " name:" + this.mNickName + "] ";
	}

	// 从xml解析出来
	public static BoUserInfoBase parserXml(String xml) throws Exception {
		if (xml == null) {
			return null;
		}

		final BoUserInfoBase boUserBaseInfo = new BoUserInfoBase();
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

					if (id != null) {
						try {
							boUserBaseInfo.mId = Integer.valueOf(id);
						} catch (NumberFormatException e) {
							boUserBaseInfo.mId = -1;
						}
					}

					boUserBaseInfo.mNickName = attributes.getValue("nickname");
					boUserBaseInfo.mSign = attributes.getValue("sign");
					boUserBaseInfo.mJob = attributes.getValue("job");
					boUserBaseInfo.mTelephone = attributes
							.getValue("telephone");
					boUserBaseInfo.mMobile = attributes.getValue("mobile");
					boUserBaseInfo.mAddress = attributes.getValue("address");
					boUserBaseInfo.mSex = attributes.getValue("sex");
					boUserBaseInfo.mEmail = attributes.getValue("email");
					boUserBaseInfo.mStringBirthday = attributes
							.getValue("birthday");
					boUserBaseInfo.mAccount = attributes.getValue("account");
					boUserBaseInfo.mFax = attributes.getValue("fax");
					boUserBaseInfo.mCommentName = attributes
							.getValue("commentname");
					boUserBaseInfo.mAuthtype = attributes.getValue("authtype");

					if (boUserBaseInfo.mStringBirthday != null
							&& boUserBaseInfo.mStringBirthday.length() > 0) {
						try {
							DateFormat dp = new SimpleDateFormat("yyyy-MM-dd",
									Locale.getDefault());
							boUserBaseInfo.mBirthday = dp
									.parse(boUserBaseInfo.mStringBirthday);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}

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
		byteArrayInputStream.close();

		if (boUserBaseInfo.mId == -1) {
			return null;
		} else {
			return boUserBaseInfo;
		}

	}

	// 打包与拆包
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(mId);
		dest.writeString(mNickName);
	}

	public static final Parcelable.Creator<BoUserInfoBase> CREATOR = new Creator<BoUserInfoBase>() {

		@Override
		public BoUserInfoBase[] newArray(int i) {
			return new BoUserInfoBase[i];
		}

		@Override
		public BoUserInfoBase createFromParcel(Parcel parcel) {
			return new BoUserInfoBase(parcel.readLong(), parcel.readString());
		}
	};

}
