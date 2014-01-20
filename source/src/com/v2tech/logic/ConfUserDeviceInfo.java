package com.v2tech.logic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @deprecated
 * Wrap user's camera information which user entered conference <br>
 * 
 * @author jiangzhen
 * 
 */
public class ConfUserDeviceInfo implements Parcelable {

	private long mUserID;

	private String mDefaultDeviceId;

	private ConfUserDeviceInfo(Parcel in) {
		this.mUserID = in.readLong();
		this.mDefaultDeviceId = in.readString();
	}

	public ConfUserDeviceInfo(long mUserID, String mDefaultDeviceId) {
		super();
		this.mUserID = mUserID;
		this.mDefaultDeviceId = mDefaultDeviceId;
	}

	public long getUserID() {
		return mUserID;
	}

	public void setUserID(long mUserID) {
		this.mUserID = mUserID;
	}

	public String getDefaultDeviceId() {
		return mDefaultDeviceId;
	}

	public void setDefaultDeviceId(String mDefaultDeviceId) {
		this.mDefaultDeviceId = mDefaultDeviceId;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flag) {
		parcel.writeLong(mUserID);
		parcel.writeString(this.mDefaultDeviceId);
	}

	public static final Parcelable.Creator<ConfUserDeviceInfo> CREATOR = new Parcelable.Creator<ConfUserDeviceInfo>() {
		public ConfUserDeviceInfo createFromParcel(Parcel in) {
			return new ConfUserDeviceInfo(in);
		}

		public ConfUserDeviceInfo[] newArray(int size) {
			return new ConfUserDeviceInfo[size];
		}
	};

	/**
	 * <xml><user id='136'><videolist defaultid='136:CyberLink Webcam Sharing
	 * Manager____2056417056'><video bps='256' camtype='0' comm='0'
	 * desc='CyberLink Webcam Sharing Manager____2056417056' fps='15'
	 * id='136:CyberLink Webcam Sharing Manager____2056417056' selectedindex='0'
	 * videotype='vid'><sizelist><size h='240' w='320'/><size h='360'
	 * w='640'/><size h='480' w='640'/><size h='600' w='800'/><size h='720'
	 * w='1280'/><size h='960' w='1280'/><size h='900' w='1600'/><size h='1200'
	 * w='1600'/></sizelist></video><video bps='256' camtype='0' comm='0'
	 * desc='HP HD Webcam [Fixed]____1388682949' fps='15' id='136:HP HD Webcam
	 * [Fixed]____1388682949' selectedindex='3' videotype='vid'><sizelist><size
	 * h='480' w='640'/><size h='400' w='640'/><size h='288' w='352'/><size
	 * h='240' w='320'/><size h='720'
	 * w='1280'/></sizelist></video></videolist></user></xml>
	 * 
	 * @param xmlData
	 * @return
	 */
	public static List<ConfUserDeviceInfo> parseFromXml(String xmlData) {
		if (xmlData == null) {
			throw new RuntimeException(" user video data is null");
		}
		List<ConfUserDeviceInfo> l = new ArrayList<ConfUserDeviceInfo>();

		InputStream is = null;

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			is = new ByteArrayInputStream(xmlData.getBytes("UTF-8"));
			Document doc = dBuilder.parse(is);

			doc.getDocumentElement().normalize();

			NodeList userList = doc.getElementsByTagName("user");
			Element userElement;

			for (int i = 0; i < userList.getLength(); i++) {
				userElement = (Element) userList.item(i);
				long uid = Long.parseLong(userElement.getAttribute("id"));
				String deviceId = null;
				NodeList videoList = userElement
						.getElementsByTagName("videolist");
				if (videoList.getLength() > 0) {
					Element videoElement = (Element) videoList.item(0);
					deviceId = videoElement.getAttribute("defaultid");
					l.add(new ConfUserDeviceInfo(uid, deviceId));
					break;
					// TODO need to be implement parse all video device list
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

		return l;
	}
}
